package com.axeliscore.anim;

/**
 * Movimiento hecho con código encima de las animaciones (se usa en {@code axPose}, después de muestrear): resortes para lo que cuelga
 * (colas, orejas, pelo, capas, antenas), mirar hacia un punto con límites y suavizado, y ruido suave para que nada se vea congelado.
 * Todo guarda su estado en el {@link AnimHost} del personaje, así que se puede usar en cualquier entidad.
 * <pre>
 *     public void axPose(AnimHost h, Pose pose, float pt) {
 *         Procedural.resorte(h, pose, "tail", this, 0.18F, 0.82F, 1.0F);
 *         Procedural.ruido(h, pose, "head", 2F, 0.6F);   // 2 grados, 0.6 veces por segundo
 *     }
 * </pre>
 */
public final class Procedural {
    private Procedural() {
    }

    // ------------------------------------------------------------------ resortes

    /** Estado de un resorte (ángulos y velocidades en x y z). */
    static final class Resorte {
        float ax, az, vx, vz;
        double lx = Double.NaN, lz, lvx, lvz;
        float lyaw = Float.NaN;
        float lastT = -1;
    }

    /**
     * Un hueso que cuelga y se balancea solo: se va hacia atrás al acelerar, hacia los lados al girar y vuelve con rebote.
     *
     * @param rigidez qué tan rápido vuelve (0.05 muy flojo, 0.3 duro)
     * @param freno   cuánto se frena cada cuadro (0.7 rebota mucho, 0.95 casi nada)
     * @param fuerza  cuánto le afecta el movimiento (1 normal)
     */
    public static void resorte(AnimHost h, Pose pose, String hueso, net.minecraft.world.entity.Entity e, float rigidez, float freno, float fuerza) {
        int b = h.geo().bone(hueso);
        if (b < 0) {
            return;
        }
        Resorte r = (Resorte) h.data.computeIfAbsent("ax_resorte_" + hueso, k -> new Resorte());
        float t = e.tickCount;
        if (r.lastT != t) {
            r.lastT = t;
            double x = e.getX(), z = e.getZ();
            float yaw = e instanceof net.minecraft.world.entity.LivingEntity le ? le.yBodyRot : e.getYRot();
            if (Double.isNaN(r.lx)) {
                r.lx = x;
                r.lz = z;
                r.lyaw = yaw;
            }
            double vx = x - r.lx, vz = z - r.lz;
            double accX = vx - r.lvx, accZ = vz - r.lvz;
            r.lx = x;
            r.lz = z;
            r.lvx = vx;
            r.lvz = vz;
            // aceleración en el espacio del cuerpo: adelante/atrás y a los lados
            double rad = Math.toRadians(yaw);
            double fwd = -accX * Math.sin(rad) + accZ * Math.cos(rad);
            double side = accX * Math.cos(rad) + accZ * Math.sin(rad);
            float giro = net.minecraft.util.Mth.wrapDegrees(yaw - r.lyaw);
            r.lyaw = yaw;
            // empuje: al acelerar se va hacia atrás; al girar, hacia afuera
            r.vx += (float) (fwd * 9.0 * fuerza);
            r.vz += (float) (side * 9.0 * fuerza) + giro * 0.012F * fuerza;
            // resorte hacia 0 y freno
            r.vx += -r.ax * rigidez;
            r.vz += -r.az * rigidez;
            r.vx *= freno;
            r.vz *= freno;
            r.ax = clamp(r.ax + r.vx, -1.2F, 1.2F);
            r.az = clamp(r.az + r.vz, -1.2F, 1.2F);
        }
        pose.addRot(b, r.ax, 0, r.az);
    }

    /** Lo mismo para una cadena (cola de varios huesos): cada hueso sigue al anterior con un poco de retraso, más suelto al final. */
    public static void cadena(AnimHost h, Pose pose, net.minecraft.world.entity.Entity e, float rigidez, float freno, float fuerza, String... huesos) {
        for (int i = 0; i < huesos.length; i++) {
            float k = 1 + i * 0.35F;
            resorte(h, pose, huesos[i], e, rigidez / k, Math.min(0.97F, freno + i * 0.02F), fuerza * k * 0.6F);
        }
    }

    // ------------------------------------------------------------------ mirar

    /**
     * Gira uno o varios huesos para mirar hacia un ángulo (grados, relativos al cuerpo), repartiendo el giro ({@code partes} suman 1,
     * p. ej. cuello 0.4 y cabeza 0.6), con un límite y suavizado.
     */
    public static void mirar(AnimHost h, Pose pose, float yaw, float pitch, float limite, float suave, String[] huesos, float[] partes) {
        yaw = clamp(yaw, -limite, limite);
        pitch = clamp(pitch, -limite, limite);
        Object prev = h.data.get("ax_mirar");
        float[] p = prev instanceof float[] f ? f : new float[] {yaw, pitch};
        p[0] += (yaw - p[0]) * suave;
        p[1] += (pitch - p[1]) * suave;
        h.data.put("ax_mirar", p);
        float r = (float) Math.PI / 180F;
        for (int i = 0; i < huesos.length; i++) {
            pose.addRot(h.geo().bone(huesos[i]), p[1] * partes[i] * r, p[0] * partes[i] * r, 0);
        }
    }

    // ------------------------------------------------------------------ ruido

    /** Movimiento suave al azar (como respirar o tambalearse un poco): {@code grados} de amplitud, {@code veces} por segundo. */
    public static void ruido(AnimHost h, Pose pose, String hueso, float grados, float veces) {
        int b = h.geo().bone(hueso);
        if (b < 0) {
            return;
        }
        float t = (System.nanoTime() / 1_000_000_000F) * veces;
        int seed = hueso.hashCode();
        float r = (float) Math.PI / 180F * grados;
        pose.addRot(b, suave(t, seed) * r, suave(t, seed + 17) * r * 0.6F, suave(t, seed + 41) * r * 0.8F);
    }

    /** Ruido suave de -1 a 1 (valor interpolado entre números al azar fijos). */
    public static float suave(float t, int seed) {
        int i = (int) Math.floor(t);
        float f = t - i;
        float a = hash(i, seed), b = hash(i + 1, seed);
        float s = f * f * (3 - 2 * f);
        return a + (b - a) * s;
    }

    private static float hash(int i, int seed) {
        int x = i * 374761393 + seed * 668265263;
        x = (x ^ (x >>> 13)) * 1274126177;
        x ^= x >>> 16;
        return (x & 0xFFFF) / 32767.5F - 1F;
    }

    private static float clamp(float v, float a, float b) {
        return v < a ? a : v > b ? b : v;
    }
}
