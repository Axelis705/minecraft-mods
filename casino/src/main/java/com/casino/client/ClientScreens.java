package com.casino.client;

import net.minecraft.client.Minecraft;

/** Saber si hay una pantalla abierta (en 26.2+ la pantalla vive en gui). */
public final class ClientScreens {
    private ClientScreens() {
    }

    public static boolean none() {
        //#if MC >= 26.2
        //$$ return Minecraft.getInstance().gui.screen() == null;
        //#else
        return Minecraft.getInstance().screen == null;
        //#endif
    }
}
