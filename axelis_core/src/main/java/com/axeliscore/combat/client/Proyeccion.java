package com.axeliscore.combat.client;

import com.axeliscore.plat.client.Draw;
import net.minecraft.world.phys.Vec3;

/**
 * (Cliente) Pasa un punto del mundo a la pantalla (para barras de vida, números de daño e íconos que se dibujan en el HUD, igual en
 * todas las versiones). Usa la posición de la cámara, sus ángulos (los guarda el evento de la cámara, ya con el temblor) y el campo de
 * visión de este cuadro.
 */
public final class Proyeccion {
    private static float yaw, pitch, partial = 1F;
    private static double fov = 70;

    private Proyeccion() {
    }

    /** Lo llama el evento de los ángulos de la cámara (cada cuadro). */
    public static void angulos(float y, float p) {
        yaw = y;
        pitch = p;
    }

    /** Lo llama el evento del campo de visión (cada cuadro, solo el del mundo). */
    public static void fov(double f, float pt) {
        if (f > 1 && f < 179) {
            fov = f;
        }
        partial = pt;
    }

    /** Fracción del tick de este cuadro (para que lo que se mueve no salte). */
    public static float partial() {
        return partial;
    }

    /**
     * El punto en la pantalla (coordenadas del HUD de tamaño {@code w}×{@code h}): {x, y, distancia}. Null si queda detrás de la cámara.
     */
    public static float[] aPantalla(double x, double y, double z, int w, int h) {
        Vec3 cam = Draw.camPos();
        double rx = x - cam.x, ry = y - cam.y, rz = z - cam.z;
        double f = Math.toRadians(pitch), g = Math.toRadians(-yaw);
        // adelante, arriba y derecha de la cámara (igual que la vista de Minecraft)
        double fx = Math.sin(g) * Math.cos(f), fy = -Math.sin(f), fz = Math.cos(g) * Math.cos(f);
        double f2 = f - Math.PI / 2;
        double ux = Math.sin(g) * Math.cos(f2), uy = -Math.sin(f2), uz = Math.cos(g) * Math.cos(f2);
        double dx = fy * uz - fz * uy, dy = fz * ux - fx * uz, dz = fx * uy - fy * ux;
        double zc = rx * fx + ry * fy + rz * fz;
        if (zc < 0.1) {
            return null;
        }
        double xc = rx * dx + ry * dy + rz * dz, yc = rx * ux + ry * uy + rz * uz;
        double t = Math.tan(Math.toRadians(fov) / 2);
        double aspect = w / (double) Math.max(1, h);
        double nx = xc / (zc * t * aspect), ny = yc / (zc * t);
        return new float[] {(float) (w / 2.0 * (1 + nx)), (float) (h / 2.0 * (1 - ny)), (float) Math.sqrt(rx * rx + ry * ry + rz * rz)};
    }
}
