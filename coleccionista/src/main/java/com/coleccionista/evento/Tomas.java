package com.coleccionista.evento;

import com.axeliscore.AxelisCore;
import com.axeliscore.data.PlayerStore;
import com.axeliscore.data.WorldStore;
import com.axeliscore.plat.Plat;
import com.coleccionista.Coleccionista;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * Las tomas de cámara del organizador para transmitir: lugares con su ángulo guardados en el mapa (se quedan con el mundo). El
 * organizador puede ir a una, viajar suave a otra o hacer un recorrido por todas. Solo se les mandan a los organizadores.
 */
public final class Tomas {
    public static final String KEY = Coleccionista.MODID + ":tomas";
    public static final int MAX = 40;

    private Tomas() {
    }

    public static void init() {
        AxelisCore.onLogin(p -> {
            if (puede(p)) {
                PlayerStore.Sync.toPlayer(p, KEY, datos(Plat.server(p)));
            }
        });
    }

    /** ¿Es operador, organizador o el dueño del mundo (en un jugador)? */
    public static boolean puede(ServerPlayer p) {
        return Plat.isOp(p) || Plat.isHost(p) || Evento.get(Plat.server(p)).esOrganizador(p.getUUID());
    }

    static CompoundTag datos(MinecraftServer s) {
        CompoundTag g = WorldStore.get(s, Coleccionista.MODID).global();
        if (!g.contains("tomas")) {
            g.put("tomas", new CompoundTag());
        }
        return g.getCompound("tomas");
    }

    private static ListTag lista(MinecraftServer s) {
        CompoundTag d = datos(s);
        if (!d.contains("lista")) {
            d.put("lista", new ListTag());
        }
        return d.getList("lista", Tag.TAG_COMPOUND);
    }

    /** Guarda dónde está y hacia dónde mira el organizador. */
    public static boolean guardar(ServerPlayer p, String nombre) {
        MinecraftServer s = Plat.server(p);
        ListTag l = lista(s);
        if (l.size() >= MAX) {
            return false;
        }
        CompoundTag t = new CompoundTag();
        t.putString("nombre", nombre == null || nombre.isBlank() ? "Toma " + (l.size() + 1) : nombre);
        t.putString("dim", p.level().dimension().location().toString());
        t.putDouble("x", p.getX());
        t.putDouble("y", p.getY());
        t.putDouble("z", p.getZ());
        t.putFloat("yaw", p.getYRot());
        t.putFloat("pitch", p.getXRot());
        l.add(t);
        cambio(s);
        return true;
    }

    public static void borrar(MinecraftServer s, int i) {
        ListTag l = lista(s);
        if (i >= 0 && i < l.size()) {
            l.remove(i);
            cambio(s);
        }
    }

    /** Sube (-1) o baja (+1) una toma en la lista (el orden es el del recorrido). */
    public static void mover(MinecraftServer s, int i, int d) {
        ListTag l = lista(s);
        int j = i + d;
        if (i >= 0 && i < l.size() && j >= 0 && j < l.size()) {
            Tag a = l.get(i);
            l.set(i, l.get(j));
            l.set(j, a);
            cambio(s);
        }
    }

    private static void cambio(MinecraftServer s) {
        WorldStore.get(s, Coleccionista.MODID).dirty();
        CompoundTag d = datos(s);
        for (ServerPlayer p : s.getPlayerList().getPlayers()) {
            if (puede(p)) {
                PlayerStore.Sync.toPlayer(p, KEY, d);
            }
        }
    }
}
