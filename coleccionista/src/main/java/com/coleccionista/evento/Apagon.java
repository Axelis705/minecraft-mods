package com.coleccionista.evento;

import com.axeliscore.AxelisCore;
import com.axeliscore.data.PlayerStore;
import com.coleccionista.Coleccionista;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Un apagón corto (las luces parpadean y se va la corriente un par de segundos): para mover a la gente sin que se vea el cambio (por
 * ejemplo, de un pasillo de servicio a su copia en otro lado del mapa). A la mitad del apagón se hace lo que se pidió.
 */
public final class Apagon {
    public static final String KEY = Coleccionista.MODID + ":apagon";
    private static final List<Object[]> PENDIENTES = new ArrayList<>();
    private static int n;

    private Apagon() {
    }

    public static void init() {
        AxelisCore.onServerTick(Apagon::tick);
    }

    /** Apaga la luz a esos jugadores {@code ticks} y, a la mitad, corre {@code enMedio}. */
    public static void hacer(MinecraftServer s, Collection<ServerPlayer> quienes, int ticks, Runnable enMedio) {
        CompoundTag d = new CompoundTag();
        d.putInt("n", ++n);
        d.putInt("dura", ticks);
        for (ServerPlayer p : quienes) {
            PlayerStore.Sync.toPlayer(p, KEY, d);
        }
        synchronized (PENDIENTES) {
            PENDIENTES.add(new Object[] {s.getTickCount() + Math.max(1, ticks / 2), enMedio});
        }
        Coleccionista.LOGGER.info("COLECCIONISTA_APAGON jugadores={} ticks={}", quienes.size(), ticks);
    }

    /** A todos los jugadores conectados. */
    public static void todos(MinecraftServer s, int ticks, Runnable enMedio) {
        hacer(s, s.getPlayerList().getPlayers(), ticks, enMedio);
    }

    private static void tick(MinecraftServer s) {
        List<Runnable> toca = new ArrayList<>();
        synchronized (PENDIENTES) {
            PENDIENTES.removeIf(o -> {
                if (s.getTickCount() >= (int) o[0]) {
                    toca.add((Runnable) o[1]);
                    return true;
                }
                return false;
            });
        }
        for (Runnable r : toca) {
            if (r != null) {
                r.run();
            }
        }
    }
}
