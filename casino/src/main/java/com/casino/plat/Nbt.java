package com.casino.plat;

import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.UUID;

/**
 * Leer y escribir datos guardados igual en todas las versiones. Desde 1.21.5 los getters de CompoundTag devuelven Optional y ya no
 * existen putUUID/getUUID; aquí todo devuelve un valor por defecto (0, false, "", vacío) como en 1.20.
 */
public final class Nbt {
    private Nbt() {
    }

    public static int i(CompoundTag t, String k) {
        //#if MC >= 1.21.5
        //$$ return t.getIntOr(k, 0);
        //#else
        return t.getInt(k);
        //#endif
    }

    public static long l(CompoundTag t, String k) {
        //#if MC >= 1.21.5
        //$$ return t.getLongOr(k, 0L);
        //#else
        return t.getLong(k);
        //#endif
    }

    public static boolean b(CompoundTag t, String k) {
        //#if MC >= 1.21.5
        //$$ return t.getBooleanOr(k, false);
        //#else
        return t.getBoolean(k);
        //#endif
    }

    public static float f(CompoundTag t, String k) {
        //#if MC >= 1.21.5
        //$$ return t.getFloatOr(k, 0F);
        //#else
        return t.getFloat(k);
        //#endif
    }

    public static String s(CompoundTag t, String k) {
        //#if MC >= 1.21.5
        //$$ return t.getStringOr(k, "");
        //#else
        return t.getString(k);
        //#endif
    }

    public static int[] ia(CompoundTag t, String k) {
        //#if MC >= 1.21.5
        //$$ return t.getIntArray(k).orElse(new int[0]);
        //#else
        return t.getIntArray(k);
        //#endif
    }

    public static CompoundTag c(CompoundTag t, String k) {
        //#if MC >= 1.21.5
        //$$ return t.getCompoundOrEmpty(k);
        //#else
        return t.getCompound(k);
        //#endif
    }

    /** Lista de compuestos guardada con esa clave (vacía si no hay). */
    public static ListTag list(CompoundTag t, String k) {
        //#if MC >= 1.21.5
        //$$ return t.getListOrEmpty(k);
        //#else
        return t.getList(k, Tag.TAG_COMPOUND);
        //#endif
    }

    public static CompoundTag at(ListTag l, int i) {
        //#if MC >= 1.21.5
        //$$ return l.getCompoundOrEmpty(i);
        //#else
        return l.getCompound(i);
        //#endif
    }

    public static long atLong(ListTag l, int i) {
        Tag t = l.get(i);
        //#if MC >= 26.1
        //$$ return t instanceof net.minecraft.nbt.LongTag lt ? lt.value() : 0L;
        //#else
        return t instanceof net.minecraft.nbt.LongTag lt ? lt.getAsLong() : 0L;
        //#endif
    }

    public static boolean has(CompoundTag t, String k) {
        return t.contains(k);
    }

    /** Los UUID se guardan como 4 enteros (igual que hacía putUUID). */
    public static void putUuid(CompoundTag t, String k, UUID u) {
        t.putIntArray(k, UUIDUtil.uuidToIntArray(u));
    }

    public static boolean hasUuid(CompoundTag t, String k) {
        return ia(t, k).length == 4;
    }

    public static UUID uuid(CompoundTag t, String k) {
        int[] a = ia(t, k);
        return a.length == 4 ? UUIDUtil.uuidFromIntArray(a) : null;
    }
}
