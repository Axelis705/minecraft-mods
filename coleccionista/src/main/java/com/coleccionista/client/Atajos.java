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

    /**
     * Tecla R: la cámara se acerca a la pantalla más cercana que mira hacia mí (para ver bien al Coleccionista); otra vez R o moverse la
     * regresa.
     */
    public static void acercarse() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            return;
        }
        net.minecraft.core.BlockPos yo = mc.player.blockPosition();
        net.minecraft.world.phys.Vec3 ojo = mc.player.getEyePosition();
        net.minecraft.world.phys.Vec3 mejor = null, frente = null;
        double dist = 1e9;
        for (net.minecraft.core.BlockPos p : net.minecraft.core.BlockPos.betweenClosed(yo.offset(-7, -3, -7), yo.offset(7, 5, 7))) {
            var st = mc.level.getBlockState(p);
            if (!(st.getBlock() instanceof com.coleccionista.bloque.PantallaBlock)) {
                continue;
            }
            net.minecraft.core.Direction f = st.getValue(com.coleccionista.bloque.PantallaBlock.FACING);
            boolean grande = st.getValue(com.coleccionista.bloque.PantallaBlock.GRANDE);
            net.minecraft.world.phys.Vec3 n = net.minecraft.world.phys.Vec3.atLowerCornerOf(f.getNormal());
            net.minecraft.world.phys.Vec3 centro = net.minecraft.world.phys.Vec3.atCenterOf(p).add(n.scale(-0.5 + 2.05 / 16));
            if (ojo.subtract(centro).dot(n) <= 0) {
                continue; // la pantalla mira hacia el otro lado
            }
            double dd = ojo.distanceToSqr(centro);
            if (dd < dist) {
                dist = dd;
                mejor = centro;
                frente = centro.add(n.scale(grande ? 7.5 : 1.7));
            }
        }
        if (mejor != null) {
            Cine.acercarse(frente, mejor);
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
