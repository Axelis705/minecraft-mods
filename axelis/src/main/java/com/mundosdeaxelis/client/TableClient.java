package com.mundosdeaxelis.client;

import com.mundosdeaxelis.network.EssenceSyncPacket;
import net.minecraft.network.chat.Component;

import java.util.HashSet;
import java.util.Set;

/** Lo que el cliente sabe de la Mesa del Artífice del jugador (Esencia, nivel y Archivo), enviado por el servidor. */
public final class TableClient {
    private TableClient() {
    }

    public static int essence;
    public static int level = 1;
    public static final Set<String> archive = new HashSet<>();
    /** Garantía por caja (id -> aperturas seguidas sin Épica, Legendaria y Mítica). */
    public static final java.util.Map<String, int[]> pity = new java.util.HashMap<>();
    public static Component toast;
    public static boolean toastGood;
    public static long toastTime;

    public static void set(EssenceSyncPacket p) {
        essence = p.essence;
        level = Math.max(1, p.level);
        archive.clear();
        archive.addAll(p.archive);
        pity.clear();
        pity.putAll(p.pity);
        if (p.toast != null) {
            toast = p.toast;
            toastGood = p.good;
            toastTime = System.currentTimeMillis();
        }
    }
}
