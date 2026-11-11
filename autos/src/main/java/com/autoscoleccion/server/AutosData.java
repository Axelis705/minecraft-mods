package com.autoscoleccion.server;

import com.autoscoleccion.game.Cars;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;

/**
 * Lo que se guarda de cada jugador (dentro de "PlayerPersisted", así no se pierde al morir): cuántas veces consiguió cada auto, el
 * auto que está abriendo (se entrega al terminar la animación o al volver a entrar) y la última prueba de habilidad que jugó.
 */
public final class AutosData {
    private static final String ROOT = "autos_coleccion";

    private AutosData() {
    }

    private static CompoundTag root(Player p) {
        CompoundTag persisted = p.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        if (!p.getPersistentData().contains(Player.PERSISTED_NBT_TAG)) {
            p.getPersistentData().put(Player.PERSISTED_NBT_TAG, persisted);
        }
        CompoundTag r = persisted.getCompound(ROOT);
        if (!persisted.contains(ROOT)) {
            persisted.put(ROOT, r);
        }
        return r;
    }

    /** Cuántas veces consiguió cada auto (30 números). */
    public static int[] counts(Player p) {
        int[] c = root(p).getIntArray("counts");
        int[] out = new int[Cars.COUNT];
        System.arraycopy(c, 0, out, 0, Math.min(c.length, out.length));
        return out;
    }

    /** Suma un auto a la colección; devuelve true si es la primera vez que lo consigue. */
    public static boolean addCar(Player p, int car) {
        int[] c = counts(p);
        boolean first = c[car] == 0;
        c[car]++;
        root(p).putIntArray("counts", c);
        return first;
    }

    public static boolean has(Player p, int car) {
        return counts(p)[car] > 0;
    }

    /** Auto pendiente de entregar (-1 = ninguno). */
    public static int pendingCar(Player p) {
        CompoundTag r = root(p);
        return r.contains("pending") ? r.getInt("pending") : -1;
    }

    public static int pendingBox(Player p) {
        return root(p).getInt("pendingBox");
    }

    public static void setPending(Player p, int car, int box) {
        CompoundTag r = root(p);
        if (car < 0) {
            r.remove("pending");
            r.remove("pendingBox");
        } else {
            r.putInt("pending", car);
            r.putInt("pendingBox", box);
        }
    }

    public static int lastGame(Player p) {
        CompoundTag r = root(p);
        return r.contains("lastGame") ? r.getInt("lastGame") : -1;
    }

    public static void setLastGame(Player p, int type) {
        root(p).putInt("lastGame", type);
    }

    public static int boxesOpened(Player p) {
        return root(p).getInt("opened");
    }

    public static void countOpened(Player p) {
        root(p).putInt("opened", boxesOpened(p) + 1);
    }
}
