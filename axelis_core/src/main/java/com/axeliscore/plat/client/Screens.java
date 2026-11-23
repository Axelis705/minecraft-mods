package com.axeliscore.plat.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

/** Abrir, cerrar y ver la pantalla actual en todas las versiones (en 26.2+ la pantalla vive en {@code Minecraft.gui}). */
public final class Screens {
    private Screens() {
    }

    /** Abre una pantalla (null = cerrar la que haya). */
    public static void open(Screen s) {
        //#if MC >= 26.2
        //$$ Minecraft.getInstance().gui.setScreen(s);
        //#else
        Minecraft.getInstance().setScreen(s);
        //#endif
    }

    public static void close() {
        open(null);
    }

    /** La pantalla abierta (o null). */
    public static Screen current() {
        //#if MC >= 26.2
        //$$ return Minecraft.getInstance().gui.screen();
        //#else
        return Minecraft.getInstance().screen;
        //#endif
    }
}
