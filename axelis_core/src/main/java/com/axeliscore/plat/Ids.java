package com.axeliscore.plat;

import net.minecraft.resources.ResourceLocation;

/** Identificadores (namespace:ruta). En 1.21+ el constructor de ResourceLocation es privado. */
public final class Ids {
    private Ids() {
    }

    public static ResourceLocation rl(String ns, String path) {
        //#if MC >= 1.21
        //$$ return ResourceLocation.fromNamespaceAndPath(ns, path);
        //#else
        return new ResourceLocation(ns, path);
        //#endif
    }

    /** Un id del propio Core (axelis_core:ruta). */
    public static ResourceLocation core(String path) {
        return rl("axelis_core", path);
    }
}
