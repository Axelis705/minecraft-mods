package com.mundosdeaxelis.client;

import com.mundosdeaxelis.MundosDeAxelis;
import com.mundosdeaxelis.cape.CapeDefs;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Lado del cliente: qué capa lleva cada jugador que se ve, y sus texturas. */
public final class ClientCapes {
    private ClientCapes() {
    }

    private static final Map<UUID, String> WORN = new HashMap<>();
    private static final Map<String, ResourceLocation> TEX = new HashMap<>();
    private static final Map<String, ResourceLocation> GLOW = new HashMap<>();

    public static void set(UUID player, String cape) {
        if (cape == null || cape.isEmpty() || CapeDefs.get(cape) == null) {
            WORN.remove(player);
        } else {
            WORN.put(player, cape);
        }
    }

    /** Id de la capa que lleva ese jugador, o null. */
    public static String of(UUID player) {
        return WORN.get(player);
    }

    public static ResourceLocation texture(String id) {
        return TEX.computeIfAbsent(id, k -> new ResourceLocation(MundosDeAxelis.MODID, "textures/cape/" + k + ".png"));
    }

    public static ResourceLocation glow(String id) {
        return GLOW.computeIfAbsent(id, k -> new ResourceLocation(MundosDeAxelis.MODID, "textures/cape/" + k + "_glow.png"));
    }

    /** Al salir del mundo se olvida todo. */
    public static void clear() {
        WORN.clear();
    }
}
