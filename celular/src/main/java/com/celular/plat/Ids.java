package com.celular.plat;

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

    public static ResourceLocation mod(String path) {
        return rl("celular", path);
    }
}
