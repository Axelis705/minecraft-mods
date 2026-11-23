package com.pescacoleccion.pelea;

import com.pescacoleccion.PescaColeccion;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * (Servidor) Las peleas en curso: una por jugador. Al terminar, el servidor vuelve a jugar la pelea con las teclas del cliente
 * ({@link Pelea#replay}) y decide; así nadie puede decir que sacó un pez sin pelearlo.
 */
public final class Peleas {
    /** Lo que se guarda de una pelea mientras dura. */
    public record Activa(int id, PeleaSim.Params params, String pez, int rareza, long inicio, Consumer<PeleaSim> alTerminar) {
    }

    private static final Map<UUID, Activa> ACTIVAS = new ConcurrentHashMap<>();
    private static int nextId = 1;

    private Peleas() {
    }

    public static boolean peleando(ServerPlayer p) {
        return ACTIVAS.containsKey(p.getUUID());
    }

    /** Empieza una pelea; {@code alTerminar} recibe la simulación ya jugada por el servidor (con su resultado). */
    public static synchronized void start(ServerPlayer p, PeleaSim.Params params, String pez, int rareza, Consumer<PeleaSim> alTerminar) {
        cleanup(p.getServer());
        int id = nextId++;
        ACTIVAS.put(p.getUUID(), new Activa(id, params, pez, rareza, System.currentTimeMillis(), alTerminar));
        PeleaNet.send(p, new PeleaNet.Abrir(id, pez, rareza, Pelea.params(params)));
    }

    /** Llegó el final desde el cliente. */
    public static void finish(ServerPlayer p, int id, boolean rendido, int endMs, int[] teclas) {
        Activa a = ACTIVAS.get(p.getUUID());
        if (a == null || a.id() != id) {
            return;
        }
        ACTIVAS.remove(p.getUUID());
        long elapsed = System.currentTimeMillis() - a.inicio();
        PeleaSim sim;
        if (rendido) {
            sim = new PeleaSim(a.params());
            sim.result = PeleaSim.ESCAPO;
        } else {
            sim = Pelea.replay(a.params(), teclas);
            // lo que pasó después de la última tecla (por ejemplo, que el hilo se rompiera solo), hasta donde terminó en el cliente
            int until = (int) Math.min(endMs, elapsed + 1500);
            sim.advanceTo(until);
            if (endMs > elapsed + 3000) {
                PescaColeccion.LOGGER.warn("pelea de {}: el cliente dice {} ms pero pasaron {} ms", p.getGameProfile().getName(), endMs, elapsed);
                sim.result = PeleaSim.ESCAPO;
            }
        }
        PescaColeccion.LOGGER.info("PESCA_PELEA rareza={} resultado={} perfecto={} teclas={} fallos={} hilo={}", a.rareza(), sim.result,
                sim.flawless(), teclas.length / 3, sim.misses, Math.round(sim.hp));
        a.alTerminar().accept(sim);
    }

    /** Si el jugador se fue o la pelea quedó colgada, el pez se escapa. */
    static void cleanup(net.minecraft.server.MinecraftServer server) {
        if (server == null) {
            return;
        }
        long now = System.currentTimeMillis();
        ACTIVAS.entrySet().removeIf(e -> {
            Activa a = e.getValue();
            boolean gone = server.getPlayerList().getPlayer(e.getKey()) == null;
            return gone || now - a.inicio() > a.params().durationMs + 20000L;
        });
    }
}
