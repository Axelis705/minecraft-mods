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
 * trampas marcadas en rojo). Los demás caminan adentro con los ojos vendados (casi no ven nada) y solo tienen la voz del guía. Pisar una
 * trampa te regresa al inicio. Cuando todos los caminantes de un equipo salen, el equipo termina. El último equipo en terminar (o los que
 * no terminen a tiempo) pierde a su guía: "guió mal".
 */
public final class ACiegas {
    private static final String MOD = Coleccionista.MODID;
    public static final String KEY = MOD + ":ciegas";
    public static final String PRUEBA = "A ciegas";
    public static int segundos = 300, tamano = 3, trampas = 8, pierden = 1;

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
    private static final Map<UUID, Long> botSale = new HashMap<>();
    private static long t, tpEn = -1, presentarEn = -1;
    private static int eliminados;
    private static boolean cambio;

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
        limpiar(s);
        Random r = new Random(s.overworld().getRandom().nextLong());
        int n = Math.max(2, Math.min(Laberintos.LABERINTOS, v.size() / Math.max(2, tamano)));
        Collections.shuffle(v, r);
        for (int i = 0; i < n; i++) {
            equipos.add(new Equipo(i + 1));
        }
        for (int i = 0; i < v.size(); i++) {
            Equipo e = equipos.get(i % n);
            e.ids.add(v.get(i).id);
            v.get(i).equipo = e.k - 1;
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
        List<int[]> libres = new ArrayList<>();
        for (int dx = 1; dx < Laberintos.LADO - 1; dx++) {
            for (int dz = 1; dz < Laberintos.LADO - 1; dz++) {
                if (a[dx][dz] && !enCamino.contains((long) dx * 1000 + dz)) {
                    libres.add(new int[] {dx, dz});
                }
            }
        }
        Collections.shuffle(libres, r);
        for (int i = 0; i < Math.min(trampas, libres.size()); i++) {
            trampasPos.add(libres.get(i));
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
        cambio = true;
        Coleccionista.LOGGER.info("COLECCIONISTA_CIEGAS preparada equipos={} trampas={}", n, trampasPos.size());
        return null;
    }

    private static void aSusLaberintos(MinecraftServer s) {
        Evento ev = Evento.get(s);
        for (Equipo e : equipos) {
            for (UUID id : e.ids) {
                mover(s, ev.pieza(id), "cie.lab." + e.k + (id.equals(e.guia) ? ".guia" : ".inicio"));
            }
        }
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
            cambio = true;
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
        cambio = true;
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
            Escena.presentacion(s, "IV", MOD + ".cie.titulo", MOD + ".cie.lema");
            Anuncio.sonido(s, SoundEvents.BELL_BLOCK, 1F, 0.45F);
        }
        Evento ev = Evento.get(s);
        if (ev.fase() == Evento.Fase.PAUSA || fase != Fase.JUEGO) {
            if (cambio) {
                enviar(s);
            }
            return;
        }
        t++;
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
                    cambio = true;
                    continue;
                }
                int[] c = celda(s, e.k, en);
                if (c != null) {
                    for (int[] tr : trampasPos) {
                        if (tr[0] == c[0] && tr[1] == c[1]) {
                            // ¡trampa! de regreso al inicio
                            s.overworld().playSound(null, en.getX(), en.getY(), en.getZ(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1F, 0.6F);
                            mover(s, p, "cie.lab." + e.k + ".inicio");
                            if (en instanceof ServerPlayer sp) {
                                Plat.msg(sp, Component.translatable(MOD + ".cie.trampa").withStyle(ChatFormatting.RED), true);
                            }
                            Coleccionista.LOGGER.info("COLECCIONISTA_CIEGAS trampa {}", p.etiqueta());
                            break;
                        }
                    }
                }
            }
            if (caminantes == 0) {
                e.termino = t;
                Anuncio.todos(s, Anuncio.coleccionista(Component.translatable(MOD + ".cie.chat.termino", e.k)));
                Anuncio.sonido(s, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 0.6F, 1F);
                Coleccionista.LOGGER.info("COLECCIONISTA_CIEGAS termino lab={}", e.k);
                cambio = true;
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
        if (cambio || now % 20 == 0) {
            enviar(s);
        }
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
                ev.eliminar(s, g, PRUEBA);
                eliminados++;
                caen++;
            }
        }
        Pantallas.modo(s, "coleccionista", "");
        cambio = true;
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
                    mover(s, ev.pieza(antes), "cie.lab." + e.k + ".inicio");
                    mover(s, p, "cie.lab." + e.k + ".guia");
                }
                cambio = true;
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
        return c0;
    }

    private static void enviar(MinecraftServer s) {
        cambio = false;
        PlayerStore.Sync.toAll(s, KEY, estado(s));
    }
}
