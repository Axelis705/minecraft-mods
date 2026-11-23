package com.axeliscore.data;

import com.axeliscore.net.CoreNet;
import com.axeliscore.plat.Plat;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Datos de cada jugador para un mod (se guardan con el mundo en {@link WorldStore}) y su copia en el cliente para mostrarlos en las
 * pantallas ({@link com.axeliscore.data.client.ClientData}).
 * <pre>
 *     CompoundTag d = PlayerStore.of(player, "pesca");
 *     d.putInt("peces", d.getInt("peces") + 1);
 *     PlayerStore.save(player, "pesca");   // guarda y manda la copia al cliente
 * </pre>
 * En el cliente: {@code ClientData.player("pesca")}.
 */
public final class PlayerStore {
    private static final Set<String> SYNC_ON_LOGIN = new CopyOnWriteArraySet<>();

    private PlayerStore() {
    }

    /** Los datos de este jugador para el mod (se pueden cambiar; luego llamar a {@link #save}). */
    public static CompoundTag of(ServerPlayer p, String modid) {
        return WorldStore.get(Plat.server(p), modid).player(p.getUUID());
    }

    /** Guarda los cambios y manda la copia al cliente del jugador. */
    public static void save(ServerPlayer p, String modid) {
        MinecraftServer s = Plat.server(p);
        WorldStore.get(s, modid).dirty();
        sync(p, modid);
    }

    /** Solo manda la copia al cliente (con la clave "{@code modid}:jugador"). */
    public static void sync(ServerPlayer p, String modid) {
        Sync.toPlayer(p, key(modid), of(p, modid));
    }

    /** Que la copia se mande sola cada vez que el jugador entra al mundo. */
    public static void syncOnLogin(String modid) {
        SYNC_ON_LOGIN.add(modid);
    }

    public static String key(String modid) {
        return modid + ":jugador";
    }

    /** (Core) Al entrar un jugador. */
    public static void onLogin(ServerPlayer p) {
        for (String m : SYNC_ON_LOGIN) {
            sync(p, m);
        }
    }

    /** Mensajes de datos para el cliente. */
    public static final class Sync {
        private Sync() {
        }

        public static void toPlayer(ServerPlayer p, String key, CompoundTag tag) {
            CoreNet.NET.toPlayer(p, new CoreNet.DataMsg(key, tag.copy()));
        }

        public static void toAll(MinecraftServer s, String key, CompoundTag tag) {
            CoreNet.NET.toAll(s, new CoreNet.DataMsg(key, tag.copy()));
        }
    }
}
