package com.coleccionista.evento;

import com.axeliscore.AxelisCore;
import com.axeliscore.data.PlayerStore;
import com.axeliscore.data.WorldStore;
import com.axeliscore.plat.Nbt;
import com.axeliscore.plat.Plat;
import com.coleccionista.Coleccionista;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerStoppedEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

/**
 * El estado del evento en el servidor: las piezas (participantes), los organizadores, la fase, el reloj y la colección de eliminados.
 * Se guarda con el mundo (si el servidor se reinicia a medio evento, sigue donde iba) y se manda a todos los clientes para el HUD.
 */
public final class Evento {
    public static final String MOD = Coleccionista.MODID;
    /** Lo que ven los clientes (HUD, panel, fantasmas). */
    public static final String KEY = MOD + ":evento";
    /** Aviso de una eliminación o de una ficha usada (para la escena en pantalla). */
    public static final String KEY_AVISO = MOD + ":aviso";

    public enum Fase {
        ESPERA, PRUEBA, PAUSA, FIN
    }

    public enum Resultado {
        ELIMINADO, YA_ESTABA
    }

    private static Evento actual;
    private static MinecraftServer de;

    final Map<UUID, Pieza> piezas = new LinkedHashMap<>();
    final Set<UUID> organizadores = new LinkedHashSet<>();
    Fase fase = Fase.ESPERA;
    /** Nombre de la prueba en curso (para el HUD). */
    String prueba = "";
    /** Tiempo de juego en el que se acaba el reloj (0: sin reloj). */
    long relojFin;
    /** Ticks que le quedaban al reloj al pausar. */
    long relojResta;
    int coleccionados;
    int siguienteNumero = 1;
    /** Sala de espera: todos en su celda, acostados y con la pantalla en negro. */
    boolean sala;
    /** Quién está disfrazado del Coleccionista (el organizador cuando aparece). */
    final Set<UUID> disfraz = new LinkedHashSet<>();

    private Evento() {
    }

    // ------------------------------------------------------------------ arranque

    public static void init() {
        AxelisCore.onServerTick(Evento::tick);
        AxelisCore.onLogin(p -> {
            Evento e = get(Plat.server(p));
            Fantasmas.alEntrar(e, p);
            PlayerStore.Sync.toPlayer(p, KEY, e.estado(Plat.server(p)));
        });
        MinecraftForge.EVENT_BUS.addListener((ServerStoppedEvent ev) -> {
            actual = null;
            de = null;
        });
    }

    /** El evento de este servidor (se carga del mundo la primera vez). */
    public static Evento get(MinecraftServer s) {
        if (actual == null || de != s) {
            de = s;
            actual = new Evento();
            actual.cargar(WorldStore.get(s, MOD).global().getCompound("evento"));
        }
        return actual;
    }

    // ------------------------------------------------------------------ consultas

    public Fase fase() {
        return fase;
    }

    public String prueba() {
        return prueba;
    }

    public List<Pieza> piezas() {
        return new ArrayList<>(piezas.values());
    }

    public List<Pieza> vivos() {
        List<Pieza> r = new ArrayList<>();
        for (Pieza p : piezas.values()) {
            if (p.vivo()) {
                r.add(p);
            }
        }
        return r;
    }

    public Pieza pieza(UUID id) {
        return id == null ? null : piezas.get(id);
    }

    public Pieza pieza(Entity e) {
        return e == null ? null : piezas.get(e.getUUID());
    }

    /** Busca por nombre (sin importar mayúsculas) o por número ("7" o "07"). */
    public Pieza buscar(String q) {
        for (Pieza p : piezas.values()) {
            if (p.nombre.equalsIgnoreCase(q)) {
                return p;
            }
        }
        try {
            int n = Integer.parseInt(q);
            for (Pieza p : piezas.values()) {
                if (p.numero == n) {
                    return p;
                }
            }
        } catch (NumberFormatException ignored) {
            // no era número
        }
        return null;
    }

    public boolean esOrganizador(UUID id) {
        return organizadores.contains(id);
    }

    /** Pone o quita el disfraz del Coleccionista. */
    public void disfraz(MinecraftServer s, UUID id, boolean si) {
        if (si) {
            disfraz.add(id);
        } else {
            disfraz.remove(id);
        }
        Coleccionista.LOGGER.info("COLECCIONISTA_DISFRAZ {} {}", id, si);
        cambio(s);
    }

    public boolean esFantasma(UUID id) {
        Pieza p = piezas.get(id);
        return p != null && !p.vivo();
    }

    /** La entidad de una pieza: el jugador conectado o el bot (null si no está en el mundo). */
    public static Entity entidad(MinecraftServer s, Pieza p) {
        if (!p.bot) {
            return s.getPlayerList().getPlayer(p.id);
        }
        for (ServerLevel l : s.getAllLevels()) {
            Entity e = l.getEntity(p.id);
            if (e != null) {
                return e;
            }
        }
        return null;
    }

    /** Los integrantes de un equipo (vivos). */
    public List<Pieza> equipo(int n) {
        List<Pieza> r = new ArrayList<>();
        for (Pieza p : vivos()) {
            if (p.equipo == n) {
                r.add(p);
            }
        }
        return r;
    }

    // ------------------------------------------------------------------ inscripción

    /** Agrega a alguien al evento (si no estaba). Devuelve su pieza. */
    public Pieza inscribir(MinecraftServer s, UUID id, String nombre, boolean bot) {
        Pieza p = piezas.get(id);
        if (p == null) {
            p = new Pieza(id, nombre, siguienteNumero++, bot);
            piezas.put(id, p);
            organizadores.remove(id);
            Coleccionista.LOGGER.info("COLECCIONISTA_INSCRITO {} {} bot={}", p.etiqueta(), nombre, bot);
            cambio(s);
        }
        return p;
    }

    /** Saca a alguien del evento por completo (y quita su bot del mundo). */
    public boolean quitar(MinecraftServer s, Pieza p) {
        if (piezas.remove(p.id) == null) {
            return false;
        }
        if (p.bot) {
            Entity e = entidad(s, p);
            if (e != null) {
                e.discard();
            }
        } else if (!p.vivo()) {
            Fantasmas.restaurar(s, p);
        }
        cambio(s);
        return true;
    }

    public void organizador(MinecraftServer s, UUID id, boolean si) {
        if (si) {
            organizadores.add(id);
        } else {
            organizadores.remove(id);
        }
        cambio(s);
    }

    // ------------------------------------------------------------------ eliminar y revivir

    /**
     * Elimina a una pieza (todos tienen una sola vida): pasa a la colección con anuncio en el chat y escena en pantalla, y se vuelve
     * fantasma. Si queda una sola, es la ganadora.
     */
    public Resultado eliminar(MinecraftServer s, Pieza p, String motivo) {
        if (!p.vivo()) {
            return Resultado.YA_ESTABA;
        }
        p.rol = Pieza.Rol.FANTASMA;
        p.orden = ++coleccionados;
        p.motivo = motivo == null ? "" : motivo;
        int quedan = vivos().size();
        Coleccionista.LOGGER.info("COLECCIONISTA_ELIMINADO {} {} quedan={}", p.etiqueta(), p.nombre, quedan);
        Anuncio.eliminado(s, this, p, quedan);
        Fantasmas.convertir(s, this, p);
        cambio(s);
        if (quedan == 1 && piezas.size() > 1 && fase != Fase.FIN) {
            Pieza g = vivos().get(0);
            fase = Fase.FIN;
            relojFin = 0;
            Coleccionista.LOGGER.info("COLECCIONISTA_GANADOR {} {}", g.etiqueta(), g.nombre);
            Anuncio.ganador(s, this, g);
            cambio(s);
        }
        return Resultado.ELIMINADO;
    }

    /** Devuelve a una pieza al juego (si el organizador se equivocó). */
    public boolean revivir(MinecraftServer s, Pieza p) {
        if (p.vivo()) {
            return false;
        }
        p.rol = Pieza.Rol.VIVO;
        p.orden = 0;
        p.motivo = "";
        if (fase == Fase.FIN) {
            fase = Fase.PRUEBA;
        }
        Coleccionista.LOGGER.info("COLECCIONISTA_REVIVIDO {} {}", p.etiqueta(), p.nombre);
        Fantasmas.restaurar(s, p);
        Anuncio.revivido(s, p);
        cambio(s);
        return true;
    }

    // ------------------------------------------------------------------ equipos

    /**
     * Arma equipos al azar con los vivos, del tamaño pedido (2 = parejas). Si no salen exactos, algunos equipos quedan con uno más.
     * Devuelve los equipos armados.
     */
    public List<List<Pieza>> equiposAlAzar(MinecraftServer s, int tam) {
        List<Pieza> v = vivos();
        Collections.shuffle(v, new Random(s.overworld().getRandom().nextLong()));
        int n = Math.max(1, v.size() / Math.max(1, tam));
        List<List<Pieza>> eq = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            eq.add(new ArrayList<>());
        }
        for (int i = 0; i < v.size(); i++) {
            eq.get(i % n).add(v.get(i));
        }
        for (int i = 0; i < n; i++) {
            for (Pieza p : eq.get(i)) {
                p.equipo = i;
            }
        }
        Coleccionista.LOGGER.info("COLECCIONISTA_EQUIPOS tam={} equipos={}", tam, n);
        Anuncio.equipos(s, eq);
        cambio(s);
        return eq;
    }

    public void sinEquipos(MinecraftServer s) {
        for (Pieza p : piezas.values()) {
            p.equipo = -1;
        }
        cambio(s);
    }

    // ------------------------------------------------------------------ fase y reloj

    public void prueba(MinecraftServer s, String nombre) {
        prueba = nombre == null ? "" : nombre;
        if (fase == Fase.ESPERA || fase == Fase.FIN) {
            fase = Fase.PRUEBA;
        }
        cambio(s);
    }

    /** Pone el reloj en {@code segundos} (0 lo quita). */
    public void reloj(MinecraftServer s, int segundos) {
        long now = s.overworld().getGameTime();
        if (fase == Fase.PAUSA) {
            relojResta = segundos * 20L;
        } else {
            relojFin = segundos <= 0 ? 0 : now + segundos * 20L;
        }
        cambio(s);
    }

    /** Segundos que le quedan al reloj (-1: sin reloj). */
    public int resta(MinecraftServer s) {
        if (fase == Fase.PAUSA) {
            return relojResta > 0 ? (int) (relojResta / 20) : -1;
        }
        if (relojFin <= 0) {
            return -1;
        }
        return (int) Math.max(0, (relojFin - s.overworld().getGameTime()) / 20);
    }

    public boolean pausar(MinecraftServer s) {
        if (fase == Fase.PAUSA) {
            return false;
        }
        relojResta = relojFin > 0 ? Math.max(1, relojFin - s.overworld().getGameTime()) : 0;
        relojFin = 0;
        fase = Fase.PAUSA;
        Pausa.congelar(s, this, true);
        Anuncio.pausa(s, true);
        cambio(s);
        return true;
    }

    public boolean reanudar(MinecraftServer s) {
        if (fase != Fase.PAUSA) {
            return false;
        }
        fase = Fase.PRUEBA;
        relojFin = relojResta > 0 ? s.overworld().getGameTime() + relojResta : 0;
        relojResta = 0;
        Pausa.congelar(s, this, false);
        Anuncio.pausa(s, false);
        cambio(s);
        return true;
    }

    /** Borra todo el evento (vuelve a todos al juego normal). */
    public void reiniciar(MinecraftServer s) {
        Pausa.congelar(s, this, false);
        for (Pieza p : new ArrayList<>(piezas.values())) {
            if (p.bot) {
                Entity e = entidad(s, p);
                if (e != null) {
                    e.discard();
                }
            } else if (!p.vivo()) {
                Fantasmas.restaurar(s, p);
            }
        }
        piezas.clear();
        sala = false;
        Despertar.reiniciar();
        com.coleccionista.prueba.Pruebas.reiniciar(s);
        Cuerpos.limpiar(s);
        Pantallas.modo(s, "apagada", "");
        Celdas.candados(s, false);
        fase = Fase.ESPERA;
        prueba = "";
        relojFin = 0;
        relojResta = 0;
        coleccionados = 0;
        siguienteNumero = 1;
        Coleccionista.LOGGER.info("COLECCIONISTA_REINICIADO");
        cambio(s);
    }

    private static void tick(MinecraftServer s) {
        if (actual == null || de != s) {
            return;
        }
        Evento e = actual;
        if (e.relojFin > 0 && s.overworld().getGameTime() >= e.relojFin) {
            e.relojFin = 0;
            Coleccionista.LOGGER.info("COLECCIONISTA_TIEMPO {}", e.prueba);
            if (!Despertar.alAcabarReloj(s) && !com.coleccionista.prueba.Pruebas.alAcabarReloj(s)) {
                Anuncio.tiempo(s);
            }
            e.cambio(s);
        }
        if (e.fase == Fase.PAUSA && s.getTickCount() % 10 == 0) {
            Pausa.mantener(s, e);
        }
    }

    // ------------------------------------------------------------------ guardar y mandar

    /** Guarda y avisa a todos los clientes. */
    public void cambio(MinecraftServer s) {
        guardar(s);
        PlayerStore.Sync.toAll(s, KEY, estado(s));
    }

    private void guardar(MinecraftServer s) {
        WorldStore ws = WorldStore.get(s, MOD);
        CompoundTag t = new CompoundTag();
        ListTag l = new ListTag();
        for (Pieza p : piezas.values()) {
            l.add(p.save());
        }
        t.put("piezas", l);
        ListTag o = new ListTag();
        for (UUID id : organizadores) {
            CompoundTag x = new CompoundTag();
            x.putUUID("id", id);
            o.add(x);
        }
        t.put("organizadores", o);
        t.putString("fase", fase.name());
        t.putString("prueba", prueba);
        t.putLong("relojFin", relojFin);
        t.putLong("relojResta", relojResta);
        t.putInt("coleccionados", coleccionados);
        t.putInt("siguiente", siguienteNumero);
        t.putBoolean("sala", sala);
        ListTag dz = new ListTag();
        for (UUID id : disfraz) {
            CompoundTag x = new CompoundTag();
            x.putUUID("id", id);
            dz.add(x);
        }
        t.put("disfraz", dz);
        ws.global().put("evento", t);
        ws.dirty();
    }

    private void cargar(CompoundTag t) {
        for (Tag x : t.getList("piezas", Tag.TAG_COMPOUND)) {
            Pieza p = Pieza.load((CompoundTag) x);
            piezas.put(p.id, p);
        }
        for (Tag x : t.getList("organizadores", Tag.TAG_COMPOUND)) {
            organizadores.add(((CompoundTag) x).getUUID("id"));
        }
        try {
            fase = Fase.valueOf(Nbt.s(t, "fase"));
        } catch (IllegalArgumentException e) {
            fase = Fase.ESPERA;
        }
        prueba = Nbt.s(t, "prueba");
        relojFin = Nbt.l(t, "relojFin");
        relojResta = Nbt.l(t, "relojResta");
        coleccionados = Nbt.i(t, "coleccionados");
        siguienteNumero = Math.max(1, Nbt.i(t, "siguiente"));
        sala = t.getBoolean("sala");
        for (Tag x : t.getList("disfraz", Tag.TAG_COMPOUND)) {
            disfraz.add(((CompoundTag) x).getUUID("id"));
        }
    }

    /** Lo que se manda a los clientes. */
    public CompoundTag estado(MinecraftServer s) {
        CompoundTag t = new CompoundTag();
        t.putString("fase", fase.name());
        t.putString("prueba", prueba);
        t.putLong("fin", relojFin);
        t.putLong("resta", relojResta);
        t.putBoolean("sala", sala);
        ListTag l = new ListTag();
        int vivos = 0;
        for (Pieza p : piezas.values()) {
            CompoundTag x = p.save();
            l.add(x);
            if (p.vivo()) {
                vivos++;
            }
        }
        t.put("piezas", l);
        t.putInt("vivos", vivos);
        t.putInt("total", piezas.size());
        ListTag o = new ListTag();
        for (UUID id : organizadores) {
            CompoundTag x = new CompoundTag();
            x.putUUID("id", id);
            o.add(x);
        }
        t.put("organizadores", o);
        ListTag dz = new ListTag();
        for (UUID id : disfraz) {
            CompoundTag x = new CompoundTag();
            x.putUUID("id", id);
            dz.add(x);
        }
        t.put("disfraz", dz);
        return t;
    }

    /** Para los mensajes a un jugador del evento (organizadores incluidos). */
    public static boolean conectado(MinecraftServer s, UUID id) {
        ServerPlayer p = s.getPlayerList().getPlayer(id);
        return p != null;
    }
}
