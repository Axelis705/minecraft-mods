package com.navidad.krampus;

import com.axeliscore.data.WorldStore;
import com.navidad.Navidad;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Lo que hay en el saco del Krampus: los regalos que se llevaron los krampusillos que no atraparon (se guarda con el mundo). Cuando le
 * rompen el saco suelta algunos y, cuando lo vencen, revienta y suelta todos.
 */
public final class SacoDelKrampus {
    private static final String KEY = "sacoKrampus";
    /** Más no caben (los más viejos se pierden). */
    public static final int MAX = 30;

    private SacoDelKrampus() {
    }

    private static ListTag lista(MinecraftServer s) {
        CompoundTag g = WorldStore.get(s, Navidad.MODID).global();
        if (!g.contains(KEY)) {
            g.put(KEY, new ListTag());
        }
        return g.getList(KEY, Tag.TAG_COMPOUND);
    }

    /** Un regalo robado entra al saco. */
    public static void meter(MinecraftServer s, ItemStack st) {
        if (st.isEmpty()) {
            return;
        }
        ListTag l = lista(s);
        while (l.size() >= MAX) {
            l.remove(0);
        }
        l.add(st.save(new CompoundTag()));
        WorldStore.get(s, Navidad.MODID).dirty();
        Navidad.LOGGER.info("NAVIDAD_SACO_KRAMPUS entra {} (hay {})", st.getHoverName().getString(), l.size());
    }

    public static int cuantos(MinecraftServer s) {
        return lista(s).size();
    }

    /** Saca hasta {@code n} regalos (los más recientes primero). */
    public static List<ItemStack> sacar(MinecraftServer s, int n) {
        ListTag l = lista(s);
        List<ItemStack> out = new ArrayList<>();
        while (n-- > 0 && !l.isEmpty()) {
            ItemStack st = ItemStack.of(l.getCompound(l.size() - 1));
            l.remove(l.size() - 1);
            if (!st.isEmpty()) {
                out.add(st);
            }
        }
        if (!out.isEmpty()) {
            WorldStore.get(s, Navidad.MODID).dirty();
        }
        return out;
    }

    public static List<ItemStack> vaciar(MinecraftServer s) {
        return sacar(s, MAX);
    }
}
