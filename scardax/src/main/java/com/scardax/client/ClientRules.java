package com.scardax.client;

import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

/** Las reglas que el cliente conoce (las manda el servidor; solo las distintas de 100). */
public final class ClientRules {
    private static Map<ResourceLocation, Integer> rules = new HashMap<>();
    public static boolean strict = false;

    private ClientRules() {
    }

    public static void replace(Map<ResourceLocation, Integer> all) {
        rules = new HashMap<>(all);
    }

    public static int get(ResourceLocation id) {
        return rules.getOrDefault(id, 100);
    }

    /** Cambio local inmediato (el servidor lo confirma después). */
    public static void set(ResourceLocation id, int pct) {
        if (pct == 100) {
            rules.remove(id);
        } else {
            rules.put(id, pct);
        }
    }
}
