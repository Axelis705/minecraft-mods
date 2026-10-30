package com.kensei.client;

import net.minecraft.client.Minecraft;

/** Abre pantallas en el cliente. Va en una clase aparte para que el servidor nunca cargue clases de pantalla. */
public final class ClientScreens {
    private ClientScreens() {}

    public static void openDuelMenu(int targetId) {
        Minecraft.getInstance().setScreen(new DuelMenuScreen(targetId));
    }
}
