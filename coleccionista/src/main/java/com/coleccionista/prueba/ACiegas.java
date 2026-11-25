package com.coleccionista.prueba;

import com.axeliscore.AxelisCore;
import com.axeliscore.data.PlayerStore;
import com.axeliscore.plat.Plat;
import com.coleccionista.Coleccionista;
import com.coleccionista.evento.Anuncio;
import com.coleccionista.evento.Complejo;
import com.coleccionista.evento.Evento;
import com.coleccionista.evento.Pantallas;
import com.coleccionista.evento.Pieza;
import com.coleccionista.mapa.Laberintos;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

/**
 * Prueba 4: A ciegas. Equipos al azar, cada uno en su laberinto. Uno es el guía: camina arriba, sobre el vidrio, y ve todo (y ve las
 * trampas marcadas en rojo). Los demás caminan adentro con los ojos vendados (todo negro; solo un pulso blanco de vez en cuando les deja
 * ver un instante lo que tienen cerca) y solo tienen la voz del guía. Pisar una trampa te regresa al vestíbulo de entrada.
 * <p>
 * Los desafíos: las trampas de fuera del camino cambian de lugar cada cierto tiempo (el guía tiene que estar atento), y en el camino bueno
 * hay baldosas intermitentes que se encienden y se apagan: hay que esperar a que se apaguen para cruzar ("¡espera… ahora!"). Cuando todos
 * los caminantes de un equipo salen, el equipo termina. El último equipo en terminar (o los que no terminen a tiempo) pierde a su guía.
 */
public final class ACiegas {
    private static final String MOD = Coleccionista.MODID;
    public static final String KEY = MOD + ":ciegas";
    public static final String PRUEBA = "A ciegas";
    public static int segundos = 300, tamano = 3, trampas = 12, pierden = 1;
    /** Cada cuántos segundos ven los vendados el pulso blanco; cada cuántos cambian de lugar las trampas (0 = nunca); baldosas intermitentes. */
    public static int pulso = 4, cambio = 45, intermitentes = 3;
    /** Las baldosas intermitentes: ciclo y cuánto de él están encendidas (ticks). */
    static final int CICLO = 120, ENCENDIDA = 50;

    public enum Fase {
        NADA, LISTA, HABLANDO, JUEGO, FIN
    }

    static final class Equipo {
        final int k;
        final List<UUID> ids = new ArrayList<>();
        UUID guia;
        final Set<UUID> salieron = new HashSet<>();
        long termino = -1;

        Equipo(int k) {
            this.k = k;
        }
    }

    private static Fase fase = Fase.NADA;
    private static final List<Equipo> equipos = new ArrayList<>();
    /** Las trampas (en bloques del laberinto: dx, dz), las mismas para todos los equipos. */
    private static final List<int[]> trampasPos = new ArrayList<>();
    /** Las baldosas intermitentes del camino bueno: {dx, dz, desfase en ticks}. */
    private static final List<int[]> interPos = new ArrayList<>();
    private static final List<int[]> libres = new ArrayList<>();
    private static final Map<UUID, Long> botSale = new HashMap<>();
    private static long t, tpEn = -1, presentarEn = -1;
    private static int eliminados;
    private static boolean sucio;

    private ACiegas() {
    }

    public static void init() {
        AxelisCore.onServerTick(ACiegas::tick);
        AxelisCore.onLogin(p -> {
            if (fase != Fase.NADA) {
                PlayerStore.Sync.toPlayer(p, KEY, estado(Plat.server(p)));
            }
        });
    }

    public static Fase fase() {
        return fase;
    }

    // ------------------------------------------------------------------ preparar

    /** ¿La prueba está abierta (preparada, en juego o terminada sin limpiar)? */
    public static boolean abierta() {
        return fase != Fase.NADA;
    }

    public static Component preparar(MinecraftServer s) {
        if (!Complejo.construido(s, "laberintos")) {
            return Component.translatable(MOD + ".cie.sin_mapa");
        }
        if (fase != Fase.NADA && fase != Fase.LISTA && fase != Fase.FIN) {
            return Component.translatable(MOD + ".cie.ya_empezo");
        }
        Evento ev = Evento.get(s);
        List<Pieza> v = ev.vivos();
        if (v.size() < 4) {
            return Component.translatable(MOD + ".cie.pocos");
        }
        Pruebas.cerrarOtras(s, "ciegas");
        limpiar(s);
        Random r = new Random(s.overworld().getRandom().nextLong());
        int n = Math.max(2, Math.min(Laberintos.LABERINTOS, v.size() / Math.max(2, tamano)));
        List<List<Pieza>> dados = Pruebas.dados(ev);
        if (dados != null) {
            // los equipos que ellos formaron; si hay más que laberintos, los que sobran se reparten
            n = Math.min(Laberintos.LABERINTOS, dados.size());
            for (int i = 0; i < n; i++) {
                equipos.add(new Equipo(i + 1));
            }
            for (int i = 0; i < dados.size(); i++) {
                Equipo e = equipos.get(i % n);
                for (Pieza p : dados.get(i)) {
                    e.ids.add(p.id);
                    p.equipo = e.k - 1;
                }
            }
        } else {
            Collections.shuffle(v, r);
            for (int i = 0; i < n; i++) {
                equipos.add(new Equipo(i + 1));
            }
            for (int i = 0; i < v.size(); i++) {
                Equipo e = equipos.get(i % n);
                e.ids.add(v.get(i).id);
                v.get(i).equipo = e.k - 1;
            }
        }
        for (Equipo e : equipos) {
            e.guia = e.ids.get(r.nextInt(e.ids.size()));
        }
        // trampas: celdas libres que no están en el camino bueno (castigan las vueltas equivocadas)
        boolean[][] a = Laberintos.celdas();
        Set<Long> enCamino = new HashSet<>();
        for (int[] c : Laberintos.camino()) {
            enCamino.add((long) c[0] * 1000 + c[1]);
        }
        for (int dx = 1; dx < Laberintos.LADO - 1; dx++) {
            for (int dz = 1; dz < Laberintos.LADO - 1; dz++) {
                if (a[dx][dz] && !enCamino.contains((long) dx * 1000 + dz)) {
                    libres.add(new int[] {dx, dz});
                }
            }
        }
        ponerTrampas(r, java.util.Set.of());
        // las baldosas intermitentes, repartidas por el camino bueno (ni al principio ni al final), cada una con su ritmo
        List<int[]> cam = Laberintos.camino();
        int desde = 4, hasta = cam.size() - 5;
        for (int i = 0; i < intermitentes && hasta > desde; i++) {
            int[] c = cam.get(desde + (hasta - desde) * (i + 1) / (intermitentes + 1));
            interPos.add(new int[] {c[0], c[1], (i * 47 + r.nextInt(40)) % CICLO});
        }
        ev.cambio(s);
        ev.prueba(s, PRUEBA);
        ev.reloj(s, 0);
        Pantallas.modo(s, "estatica", "");
        fase = Fase.LISTA;
        long now = s.overworld().getGameTime();
        Escena.fundido(s, MOD + ".cie.fundido", 80);
        tpEn = now + 22;
        presentarEn = now + 90;
        sucio = true;
        Coleccionista.LOGGER.info("COLECCIONISTA_CIEGAS preparada equipos={} trampas={} intermitentes={}", n, trampasPos.size(), interPos.size());
        return null;
    }

    /** Pone las trampas de fuera del camino (sin caer donde está parado alguien). */
    private static void ponerTrampas(Random r, Set<Long> ocupadas) {
        trampasPos.clear();
        List<int[]> l = new ArrayList<>(libres);
        Collections.shuffle(l, r);
        for (int[] c : l) {
            if (trampasPos.size() >= trampas) {
                break;
            }
            if (!ocupadas.contains((long) c[0] * 1000 + c[1])) {
                trampasPos.add(c);
            }
        }
    }

    /** ¿Esa baldosa intermitente está encendida ahora? */
    private static boolean encendida(int[] b, long now) {
        return Math.floorMod(now + b[2], CICLO) < ENCENDIDA;
    }

    private static void aSusLaberintos(MinecraftServer s) {
        Evento ev = Evento.get(s);
        for (Equipo e : equipos) {
            int i = 0;
            for (UUID id : e.ids) {
                if (id.equals(e.guia)) {
                    mover(s, ev.pieza(id), "cie.lab." + e.k + ".guia");
                } else {
                    // cada caminante en su lugar del vestíbulo (nadie aparece encima de otro)
                    mover(s, ev.pieza(id), "cie.lab." + e.k + ".inicio." + (i % Laberintos.LUGARES + 1));
                    i++;
                }
            }
        }
    }

    /** De regreso al vestíbulo, a un lugar donde no haya nadie. */
    private static void alInicio(MinecraftServer s, Equipo e, Pieza p) {
        Evento ev = Evento.get(s);
        for (int i = 1; i <= Laberintos.LUGARES; i++) {
            BlockPos b = Complejo.pos(s, "cie.lab." + e.k + ".inicio." + i);
            if (b == null) {
                break;
            }
            boolean libre = true;
            for (UUID id : e.ids) {
                Pieza q = ev.pieza(id);
                Entity en = q == null || q == p ? null : Evento.entidad(s, q);
                if (en != null && en.distanceToSqr(b.getX() + 0.5, b.getY(), b.getZ() + 0.5) < 0.8) {
                    libre = false;
                    break;
                }
            }
            if (libre) {
                mover(s, p, "cie.lab." + e.k + ".inicio." + i);
                return;
            }
        }
        mover(s, p, "cie.lab." + e.k + ".inicio");
    }

    private static void mover(MinecraftServer s, Pieza p, String marca) {
        BlockPos b = Complejo.pos(s, marca);
        Entity e = p == null ? null : Evento.entidad(s, p);
        if (b == null || e == null) {
            return;
        }
        float yaw = Complejo.yaw(marca);
        e.fallDistance = 0;
        if (e instanceof ServerPlayer sp) {
            if (sp.isSleeping()) {
                sp.stopSleeping();
            }
            sp.teleportTo(s.overworld(), b.getX() + 0.5, b.getY(), b.getZ() + 0.5, yaw, 0F);
            sp.setGameMode(GameType.ADVENTURE);
        } else {
            e.moveTo(b.getX() + 0.5, b.getY(), b.getZ() + 0.5, yaw, 0F);
            e.setYHeadRot(yaw);
            e.setDeltaMovement(Vec3.ZERO);
        }
    }

    // ------------------------------------------------------------------ juego

    public static boolean empezar(MinecraftServer s, boolean conDiscurso) {
        if (fase != Fase.LISTA) {
            return false;
        }
        if (conDiscurso) {
            fase = Fase.HABLANDO;
            sucio = true;
            Pantallas.hablar(s, "ciegas", () -> {
                if (fase == Fase.HABLANDO) {
                    arrancar(s);
                }
            });
        } else {
            Pantallas.callar(s);
            arrancar(s);
        }
        return true;
    }

    private static void arrancar(MinecraftServer s) {
        fase = Fase.JUEGO;
        java.util.Map<UUID, Integer> voz = new java.util.HashMap<>();
        for (var g : equipos) {
            for (UUID id : g.ids) {
                voz.put(id, g.k);
            }
        }
        com.coleccionista.voz.Voz.separar(voz);
        t = 0;
        Evento ev = Evento.get(s);
        ev.reloj(s, segundos);
        Pantallas.modo(s, "texto", Component.translatable(MOD + ".cie.pantalla").getString());
        Anuncio.todos(s, Anuncio.coleccionista(Component.translatable(MOD + ".cie.chat.empieza", Math.max(1, (segundos + 30) / 60))));
        Anuncio.sonido(s, SoundEvents.BELL_BLOCK, 1F, 0.6F);
        Random r = new Random(s.overworld().getRandom().nextLong());
        long now = s.overworld().getGameTime();
        for (Equipo e : equipos) {
            for (UUID id : e.ids) {
                Pieza p = ev.pieza(id);
                if (p != null && p.bot && !id.equals(e.guia)) {
                    botSale.put(id, now + 200 + r.nextInt(Math.max(1, segundos * 20 / 2)));
                }
            }
        }
        sucio = true;
        Coleccionista.LOGGER.info("COLECCIONISTA_CIEGAS empieza");
    }

    /** ¿En qué celda del laberinto k está? (null si está fuera) */
    private static int[] celda(MinecraftServer s, int k, Entity e) {
        BlockPos o = Complejo.pos(s, "cie.lab." + k + ".origen");
        if (o == null) {
            return null;
        }
        int dx = (int) Math.floor(e.getX()) - o.getX(), dz = (int) Math.floor(e.getZ()) - o.getZ();
        if (dx < 0 || dz < 0 || dx >= Laberintos.LADO || dz >= Laberintos.LADO || e.getY() > o.getY() + 6) {
            return null;
        }
        return new int[] {dx, dz};
    }

    private static boolean salio(MinecraftServer s, int k, Entity e) {
        BlockPos o = Complejo.pos(s, "cie.lab." + k + ".origen");
        return o != null && e.getZ() < o.getZ() - 0.2 && e.getY() < o.getY() + 6 && e.getX() >= o.getX() && e.getX() <= o.getX() + Laberintos.LADO;
    }

    private static void tick(MinecraftServer s) {
        if (fase == Fase.NADA) {
            return;
        }
        long now = s.overworld().getGameTime();
        if (tpEn > 0 && now >= tpEn) {
            tpEn = -1;
            aSusLaberintos(s);
        }
        if (presentarEn > 0 && now >= presentarEn) {
            presentarEn = -1;
            Escena.presentacion(s, "IX", MOD + ".cie.titulo", MOD + ".cie.lema");
            Anuncio.sonido(s, SoundEvents.BELL_BLOCK, 1F, 0.45F);
        }
        Evento ev = Evento.get(s);
        if (ev.fase() == Evento.Fase.PAUSA || fase != Fase.JUEGO) {
            if (sucio) {
                enviar(s);
            }
            return;
        }
        t++;
        // las trampas de fuera del camino cambian de lugar (el guía tiene que estar atento)
        if (cambio > 0 && t % (cambio * 20L) == 0) {
            moverTrampas(s, ev);
        }
        boolean quedan = false;
        for (Equipo e : equipos) {
            if (e.termino >= 0) {
                continue;
            }
            int caminantes = 0;
            for (UUID id : e.ids) {
                Pieza p = ev.pieza(id);
                if (p == null || !p.vivo() || id.equals(e.guia) || e.salieron.contains(id)) {
                    continue;
                }
                caminantes++;
                Entity en = Evento.entidad(s, p);
                if (en == null) {
                    continue;
                }
                Long b = botSale.get(id);
                if (b != null && now >= b) {
                    botSale.remove(id);
                    mover(s, p, "cie.lab." + e.k + ".salida");
                }
                if (salio(s, e.k, en)) {
                    e.salieron.add(id);
                    caminantes--;
                    s.overworld().playSound(null, en.getX(), en.getY(), en.getZ(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.6F, 1.2F);
                    Anuncio.todos(s, Component.literal("✔ ").withStyle(ChatFormatting.GREEN).append(Anuncio.nombre(p))
                            .append(Component.translatable(MOD + ".cie.chat.salio", e.k).withStyle(ChatFormatting.GRAY)));
                    Coleccionista.LOGGER.info("COLECCIONISTA_CIEGAS sale {} lab={}", p.etiqueta(), e.k);
                    sucio = true;
                    continue;
                }
                int[] c = celda(s, e.k, en);
                if (c != null) {
                    boolean cae = false;
                    for (int[] tr : trampasPos) {
                        if (tr[0] == c[0] && tr[1] == c[1]) {
                            cae = true;
                            break;
                        }
                    }
                    for (int[] b2 : interPos) {
                        if (b2[0] == c[0] && b2[1] == c[1] && encendida(b2, now)) {
                            cae = true;
                            break;
                        }
                    }
                    if (cae) {
                        // ¡trampa! de regreso al vestíbulo
                        s.overworld().playSound(null, en.getX(), en.getY(), en.getZ(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1F, 0.6F);
                        alInicio(s, e, p);
                        if (en instanceof ServerPlayer sp) {
                            Plat.msg(sp, Component.translatable(MOD + ".cie.trampa").withStyle(ChatFormatting.RED), true);
                        }
                        Coleccionista.LOGGER.info("COLECCIONISTA_CIEGAS trampa {}", p.etiqueta());
                    }
                }
            }
            if (caminantes == 0) {
                e.termino = t;
                Anuncio.todos(s, Anuncio.coleccionista(Component.translatable(MOD + ".cie.chat.termino", e.k)));
                Anuncio.sonido(s, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 0.6F, 1F);
                Coleccionista.LOGGER.info("COLECCIONISTA_CIEGAS termino lab={}", e.k);
                sucio = true;
            } else {
                quedan = true;
            }
        }
        // cuando solo falta(n) el (los) que pierde(n), se acaba
        int sinTerminar = 0;
        for (Equipo e : equipos) {
            if (e.termino < 0) {
                sinTerminar++;
            }
        }
        if (!quedan || sinTerminar <= Math.min(pierden, equipos.size() - 1) && sinTerminar > 0) {
            fin(s, false);
        }
        if (sucio || now % 20 == 0) {
            enviar(s);
        }
    }

    private static void moverTrampas(MinecraftServer s, Evento ev) {
        Set<Long> ocupadas = new HashSet<>();
        for (Equipo e : equipos) {
            for (UUID id : e.ids) {
                Pieza p = ev.pieza(id);
                Entity en = p == null ? null : Evento.entidad(s, p);
                int[] c = en == null ? null : celda(s, e.k, en);
                if (c != null) {
                    for (int ddx = -1; ddx <= 1; ddx++) {
                        for (int ddz = -1; ddz <= 1; ddz++) {
                            ocupadas.add((long) (c[0] + ddx) * 1000 + c[1] + ddz);
                        }
                    }
                }
            }
        }
        ponerTrampas(new Random(s.overworld().getRandom().nextLong()), ocupadas);
        for (Equipo e : equipos) {
            Pieza g = ev.pieza(e.guia);
            if (g != null && Evento.entidad(s, g) instanceof ServerPlayer sp) {
                Plat.msg(sp, Component.translatable(MOD + ".cie.cambian").withStyle(ChatFormatting.GOLD), true);
                sp.playNotifySound(SoundEvents.PISTON_EXTEND, SoundSource.BLOCKS, 0.8F, 0.6F);
            }
        }
        sucio = true;
        Coleccionista.LOGGER.info("COLECCIONISTA_CIEGAS trampas cambian");
    }

    public static boolean alAcabarReloj(MinecraftServer s) {
        if (fase != Fase.JUEGO) {
            return false;
        }
        fin(s, true);
        return true;
    }

    /** Termina: los equipos que no terminaron (o los últimos) pierden a su guía. */
    public static void fin(MinecraftServer s, boolean porTiempo) {
        if (fase != Fase.JUEGO) {
            return;
        }
        Evento ev = Evento.get(s);
        fase = Fase.FIN;
        com.coleccionista.voz.Voz.juntar();
        ev.reloj(s, 0);
        // orden: primero los que no terminaron (menos salidos primero), luego los que terminaron más tarde
        List<Equipo> orden = new ArrayList<>(equipos);
        orden.sort((x, y) -> {
            if (x.termino < 0 && y.termino < 0) {
                return Integer.compare(x.salieron.size(), y.salieron.size());
            }
            if (x.termino < 0) {
                return -1;
            }
            if (y.termino < 0) {
                return 1;
            }
            return Long.compare(y.termino, x.termino);
        });
        int caen = 0;
        for (Equipo e : orden) {
            if (caen >= pierden) {
                break;
            }
            Pieza g = ev.pieza(e.guia);
            if (g != null && g.vivo() && ev.vivos().size() > 1) {
                Anuncio.todos(s, Anuncio.coleccionista(Component.translatable(MOD + ".cie.chat.guia", e.k, Anuncio.nombre(g))));
                if (ev.eliminarEnPrueba(s, g, PRUEBA)) {
                    eliminados++;
                }
                caen++;
            }
        }
        Pantallas.modo(s, "coleccionista", "");
        sucio = true;
        Coleccionista.LOGGER.info("COLECCIONISTA_CIEGAS fin eliminados={} tiempo={}", eliminados, porTiempo);
        enviar(s);
    }

    /** Para la prueba automática: lleva a un caminante a la salida. */
    public static boolean sacar(MinecraftServer s, Pieza p) {
        for (Equipo e : equipos) {
            if (e.ids.contains(p.id) && !p.id.equals(e.guia) && fase == Fase.JUEGO) {
                mover(s, p, "cie.lab." + e.k + ".salida");
                return true;
            }
        }
        return false;
    }

    /** El organizador elige quién guía en su equipo (antes de empezar). Si ya están en sus laberintos, los cambia de lugar. */
    public static boolean elegirGuia(MinecraftServer s, Pieza p) {
        if (fase != Fase.LISTA && fase != Fase.HABLANDO) {
            return false;
        }
        for (Equipo e : equipos) {
            if (e.ids.contains(p.id)) {
                UUID antes = e.guia;
                e.guia = p.id;
                if (tpEn < 0 && antes != null && !antes.equals(p.id)) {
                    Evento ev = Evento.get(s);
                    mover(s, p, "cie.lab." + e.k + ".guia");
                    Pieza a2 = ev.pieza(antes);
                    if (a2 != null) {
                        alInicio(s, e, a2);
                    }
                }
                sucio = true;
                Coleccionista.LOGGER.info("COLECCIONISTA_CIEGAS guia {} lab={}", p.etiqueta(), e.k);
                return true;
            }
        }
        return false;
    }

    public static void terminar(MinecraftServer s) {
        if (fase == Fase.HABLANDO) {
            Pantallas.callar(s);
        }
        limpiar(s);
        enviar(s);
        Coleccionista.LOGGER.info("COLECCIONISTA_CIEGAS terminada");
    }

    public static void reiniciar(MinecraftServer s) {
        if (fase != Fase.NADA) {
            terminar(s);
        }
    }

    private static void limpiar(MinecraftServer s) {
        com.coleccionista.voz.Voz.juntar();
        fase = Fase.NADA;
        equipos.clear();
        trampasPos.clear();
        interPos.clear();
        libres.clear();
        botSale.clear();
        eliminados = 0;
        tpEn = -1;
        presentarEn = -1;
        t = 0;
    }

    // ------------------------------------------------------------------ a los clientes

    public static CompoundTag estado(MinecraftServer s) {
        CompoundTag c0 = new CompoundTag();
        c0.putString("fase", fase.name());
        ListTag l = new ListTag();
        for (Equipo e : equipos) {
            CompoundTag x = new CompoundTag();
            x.putInt("k", e.k);
            if (e.guia != null) {
                x.putUUID("guia", e.guia);
            }
            ListTag ms = new ListTag();
            for (UUID id : e.ids) {
                CompoundTag m = new CompoundTag();
                m.putUUID("id", id);
                m.putBoolean("salio", e.salieron.contains(id));
                ms.add(m);
            }
            x.put("miembros", ms);
            x.putBoolean("termino", e.termino >= 0);
            BlockPos o = Complejo.pos(s, "cie.lab." + e.k + ".origen");
            if (o != null) {
                x.putInt("ox", o.getX());
                x.putInt("oy", o.getY());
                x.putInt("oz", o.getZ());
            }
            l.add(x);
        }
        c0.put("equipos", l);
        // las trampas (el cliente solo las dibuja para los guías y el organizador)
        ListTag tr = new ListTag();
        for (int[] p : trampasPos) {
            CompoundTag x = new CompoundTag();
            x.putInt("dx", p[0]);
            x.putInt("dz", p[1]);
            tr.add(x);
        }
        c0.put("trampas", tr);
        ListTag in = new ListTag();
        for (int[] p : interPos) {
            CompoundTag x = new CompoundTag();
            x.putInt("dx", p[0]);
            x.putInt("dz", p[1]);
            x.putInt("des", p[2]);
            in.add(x);
        }
        c0.put("inter", in);
        c0.putInt("ciclo", CICLO);
        c0.putInt("encendida", ENCENDIDA);
        c0.putInt("pulso", pulso);
        c0.putInt("cambio", cambio);
        c0.putLong("t", t);
        return c0;
    }

    private static void enviar(MinecraftServer s) {
        sucio = false;
        PlayerStore.Sync.toAll(s, KEY, estado(s));
    }
}
