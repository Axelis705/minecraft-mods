package com.axeliscore.data;

import com.axeliscore.AxelisCore;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Acciones que el cliente le pide al servidor por nombre (por ejemplo, el botón "Reclamar" de un libro de colección), sin que cada mod
 * tenga que hacer su propio mensaje de red. El servidor decide si se puede: nunca confiar en lo que manda el cliente.
 * <pre>
 *     // servidor (al arrancar)
 *     Actions.on("pesca:reclamar", (player, data) -&gt; { ... });
 *     // cliente (en una pantalla)
 *     ClientData.send("pesca:reclamar", tag);
 * </pre>
 */
public final class Actions {
    public interface Handler {
        void handle(ServerPlayer player, CompoundTag data);
    }

    private static final Map<String, Handler> HANDLERS = new ConcurrentHashMap<>();

    private Actions() {
    }

    public static void on(String key, Handler h) {
        HANDLERS.put(key, h);
    }

    /** (Core) Llegó una acción de un jugador. */
    public static void receive(ServerPlayer p, String key, CompoundTag data) {
        Handler h = HANDLERS.get(key);
        if (h == null) {
            AxelisCore.LOGGER.warn("acción desconocida '{}' de {}", key, p.getGameProfile().getName());
            return;
        }
        try {
            h.handle(p, data);
        } catch (RuntimeException e) {
            AxelisCore.LOGGER.error("falló la acción '{}' de {}", key, p.getGameProfile().getName(), e);
        }
    }
}
