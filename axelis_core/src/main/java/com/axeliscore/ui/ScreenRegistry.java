package com.axeliscore.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Pantallas que el servidor puede abrir por nombre ({@code CoreScreens.open(jugador, "pesca:album")}): cada mod registra las suyas en el
 * arranque del cliente.
 */
public final class ScreenRegistry {
    private static final Map<String, Supplier<Screen>> SCREENS = new HashMap<>();

    private ScreenRegistry() {
    }

    public static void register(String id, Supplier<Screen> factory) {
        SCREENS.put(id, factory);
    }

    public static void open(String id) {
        Supplier<Screen> f = SCREENS.get(id);
        if (f != null) {
            Minecraft.getInstance().setScreen(f.get());
        }
    }
}
