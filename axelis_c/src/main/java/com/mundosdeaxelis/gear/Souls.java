package com.mundosdeaxelis.gear;

import com.mundosdeaxelis.network.ModNetwork;
import com.mundosdeaxelis.network.SoulsPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * Las almas del jugador: se ganan al matar con armas del set y se gastan en las habilidades. Se guardan con el jugador
 * y se avisa al cliente para dibujar el contador.
 */
public final class Souls {
    private Souls() {
    }

    /** Capacidad base de almas; cada pieza de armadura de set suma más (ver {@link #max}). */
    public static final int BASE = 50;
    private static final String KEY = "mdc_souls";

    /** Capacidad de almas del jugador: la base más 2 + puesto del set por cada pieza de armadura de set que lleva puesta. */
    public static int max(Player p) {
        int n = BASE;
        for (net.minecraft.world.entity.EquipmentSlot slot : new net.minecraft.world.entity.EquipmentSlot[]{
                net.minecraft.world.entity.EquipmentSlot.HEAD, net.minecraft.world.entity.EquipmentSlot.CHEST,
                net.minecraft.world.entity.EquipmentSlot.LEGS, net.minecraft.world.entity.EquipmentSlot.FEET}) {
            if (p.getItemBySlot(slot).getItem() instanceof SetArmor a && a.piece().slot == slot) {
                n += 2 + a.set().rank;
            }
        }
        return n;
    }

    public static int get(Player p) {
        return Math.max(0, Math.min(max(p), p.getPersistentData().getInt(KEY)));
    }

    public static void set(ServerPlayer sp, int v) {
        sp.getPersistentData().putInt(KEY, Math.max(0, Math.min(max(sp), v)));
        sync(sp);
    }

    /** Suma almas y devuelve cuántas entraron de verdad (el tope es {@link #max}). */
    public static int add(ServerPlayer sp, int n) {
        int cur = get(sp);
        int nv = Math.min(max(sp), cur + Math.max(0, n));
        if (nv != cur) {
            set(sp, nv);
        }
        return nv - cur;
    }

    public static boolean spend(ServerPlayer sp, int n) {
        int cur = get(sp);
        if (cur < n) {
            return false;
        }
        set(sp, cur - n);
        return true;
    }

    public static void sync(ServerPlayer sp) {
        ModNetwork.sendTo(sp, new SoulsPacket(get(sp), max(sp)));
    }
}
