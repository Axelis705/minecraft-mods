package com.navidad.client;

import net.minecraft.client.Minecraft;

import java.time.LocalDate;
import java.time.Month;

/** Lo del cliente para montar renos: si el jinete aprieta saltar y si es temporada (con la fecha de la compu, igual que el servidor). */
public final class Montar {
    private Montar() {
    }

    public static boolean saltando() {
        Minecraft mc = Minecraft.getInstance();
        return mc.player != null && mc.player.getVehicle() != null && mc.options.keyJump.isDown();
    }

    public static boolean temporada() {
        LocalDate h = LocalDate.now(com.navidad.Temporada.ZONA);
        return h.getMonth() == Month.DECEMBER || (h.getMonth() == Month.JANUARY && h.getDayOfMonth() <= 6) || com.navidad.Temporada.diaPrueba > 0;
    }
}
