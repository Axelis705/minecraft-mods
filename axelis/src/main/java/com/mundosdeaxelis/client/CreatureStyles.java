package com.mundosdeaxelis.client;

import net.minecraft.world.entity.LivingEntity;

import java.util.HashMap;
import java.util.Map;

/** El {@link CreatureStyle} de cada criatura del mod (por clase). */
public final class CreatureStyles {
    private static final Map<Class<?>, CreatureStyle> BY_CLASS = new HashMap<>();
    private static final CreatureStyle DEFAULT = CreatureStyle.of();

    private CreatureStyles() {
    }

    static void put(Class<?> k, CreatureStyle s) {
        BY_CLASS.put(k, s);
    }

    /** El estilo de esa criatura, o null si no es del mod. */
    public static CreatureStyle find(LivingEntity e) {
        init();
        return BY_CLASS.get(e.getClass());
    }

    public static CreatureStyle of(LivingEntity e) {
        CreatureStyle s = find(e);
        return s == null ? DEFAULT : s;
    }

    private static boolean ready;

    private static synchronized void init() {
        if (ready) {
            return;
        }
        ready = true;
        CreatureStylesData.register();
    }
}
