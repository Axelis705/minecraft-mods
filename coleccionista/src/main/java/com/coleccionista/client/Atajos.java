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
        if (mc.player != null && (mc.player.hasPermissions(2) || EventoCliente.soyOrganizador() || mc.hasSingleplayerServer())) {
            com.axeliscore.plat.client.Screens.open(new PanelScreen());
        }
    }

    /**
     * Tecla R: la cámara se acerca a la pantalla más cercana que mira hacia mí (para ver bien al Coleccionista); otra vez R o moverse la
     * regresa.
     */
    public static void acercarse() {
        net.minecraft.world.phys.Vec3[] p = pantallaCerca();
        if (p != null) {
            Cine.acercarse(p[0], p[1]);
        }
    }

    private static long revisada;
    private static net.minecraft.world.phys.Vec3[] ultima;

    /** La pantalla más cercana que mira hacia mí: {frente, centro} (null si no hay ninguna cerca). Se revisa cada medio segundo. */
    public static net.minecraft.world.phys.Vec3[] pantallaCerca() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            return null;
        }
        long now = System.currentTimeMillis();
        if (now - revisada < 500 && ultima != null || now - revisada < 100) {
            return ultima;
        }
        revisada = now;
        ultima = buscar(mc);
        return ultima;
    }

    private static net.minecraft.world.phys.Vec3[] buscar(Minecraft mc) {
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
        return mejor == null ? null : new net.minecraft.world.phys.Vec3[] {frente, mejor};
    }

    /** Tecla K: esconder o mostrar el HUD del evento (para grabar). */
    public static void hud() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && (mc.player.hasPermissions(2) || EventoCliente.soyOrganizador() || mc.hasSingleplayerServer())) {
            EventoHud.oculto = !EventoHud.oculto;
        }
    }
}
