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
            com.axeliscore.plat.client.Screens.open(new LineaScreen());
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

    /** ¿Se ve la pantalla desde mis ojos? (sin muros en medio: los barrotes y el vidrio no tapan) */
    private static boolean seVe(Minecraft mc, net.minecraft.world.phys.Vec3 ojo, net.minecraft.world.phys.Vec3 a, net.minecraft.core.BlockPos pantalla) {
        net.minecraft.world.phys.Vec3 d = a.subtract(ojo);
        double largo = d.length();
        d = d.normalize();
        for (double t = 0.2; t < largo; t += 0.2) {
            net.minecraft.core.BlockPos b = net.minecraft.core.BlockPos.containing(ojo.add(d.scale(t)));
            if (b.equals(pantalla)) {
                return true;
            }
            if (mc.level.getBlockState(b).isSolidRender(mc.level, b)) {
                return false;
            }
        }
        return true;
    }

    /** ¿Tengo una pantalla enfrente (que se ve y mira hacia mí)? Si no, el Coleccionista sale en la mini tele del HUD. */
    public static boolean pantallaEnfrente() {
        Minecraft mc = Minecraft.getInstance();
        net.minecraft.world.phys.Vec3[] p = pantallaCerca();
        if (p == null || mc.player == null) {
            return false;
        }
        net.minecraft.world.phys.Vec3 ojo = mc.player.getEyePosition();
        return mc.player.getViewVector(1F).dot(p[1].subtract(ojo).normalize()) > 0.55;
    }

    /** Las pantallas que se dibujaron hace poco (las cargadas cerca): posición → última vez. Así R funciona desde lejos. */
    private static final java.util.Map<net.minecraft.core.BlockPos, Long> VISTAS = new java.util.HashMap<>();
    /** Hasta dónde funciona R: las pantallas grandes desde muy lejos (en el Salón, desde cualquier mesa), las chicas menos. */
    private static final double LEJOS_GRANDE = 64, LEJOS_CHICA = 28;

    /** La pantalla se dibujó en este cuadro (lo llama PantallaRenderer). */
    static void vista(net.minecraft.core.BlockPos p) {
        VISTAS.put(p.immutable(), System.currentTimeMillis());
    }

    private static net.minecraft.world.phys.Vec3[] buscar(Minecraft mc) {
        net.minecraft.world.phys.Vec3 ojo = mc.player.getEyePosition();
        net.minecraft.world.phys.Vec3 mira = mc.player.getViewVector(1F);
        net.minecraft.world.phys.Vec3 mejor = null, frente = null;
        double puntos = 1e9;
        long now = System.currentTimeMillis();
        VISTAS.values().removeIf(t -> now - t > 3000);
        for (net.minecraft.core.BlockPos p : VISTAS.keySet()) {
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
            double d = ojo.distanceTo(centro);
            if (d > (grande ? LEJOS_GRANDE : LEJOS_CHICA)) {
                continue;
            }
            // la que tengo enfrente gana aunque esté un poco más lejos
            double hacia = mira.dot(centro.subtract(ojo).normalize());
            double pts = d * (hacia > 0.5 ? 0.5 : hacia > 0 ? 1 : 2.5);
            if (pts < puntos && seVe(mc, ojo, centro.add(n.scale(0.3)), p)) {
                puntos = pts;
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
