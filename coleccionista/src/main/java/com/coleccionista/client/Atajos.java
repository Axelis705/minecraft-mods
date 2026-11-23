package com.coleccionista.client;

import net.minecraft.client.Minecraft;

/**
 * (Cliente) Lo que hacen las teclas del mod. Está aparte para que el servidor nunca cargue pantallas al leer la clase principal.
 */
public final class Atajos {
    private Atajos() {
    }

    /** Tecla O: el panel del organizador (solo operadores u organizadores). */
    public static void panel() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && (mc.player.hasPermissions(2) || EventoCliente.soyOrganizador())) {
            com.axeliscore.plat.client.Screens.open(new PanelScreen());
        }
    }

    /** Tecla K: esconder o mostrar el HUD del evento (para grabar). */
    public static void hud() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && (mc.player.hasPermissions(2) || EventoCliente.soyOrganizador())) {
            EventoHud.oculto = !EventoHud.oculto;
        }
    }
}
