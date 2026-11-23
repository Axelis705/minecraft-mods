package com.coleccionista.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * (Cliente) La cámara del organizador para transmitir. Mueve al propio organizador (en modo espectador) cuadro por cuadro:
 * <ul>
 *     <li>Viaje: va suave de donde está a una toma.</li>
 *     <li>Recorrido: pasa por todas las tomas en orden con una curva suave (Catmull-Rom), velocidad pareja.</li>
 *     <li>Seguir: va detrás de una pieza, a unos bloques y un poco arriba, mirándola, con un leve giro alrededor.</li>
 * </ul>
 * Moverse (W A S D, espacio, Shift) suelta la cámara.
 */
public final class Camara {
    public enum Modo {
        NADA, VIAJE, RECORRIDO, SEGUIR
    }

    private static Modo modo = Modo.NADA;
    /** x, y, z, yaw, pitch. */
    private static final List<double[]> PUNTOS = new ArrayList<>();
    public static float segundosPorTramo = 5F;
    private static long inicio;
    private static UUID objetivo;
    private static Vec3 suave;
    private static float giro;
    private static long ultimo;
    /** Si el organizador quiere ver a los fantasmas (para una toma limpia se apagan). */
    public static boolean verFantasmas = true;

    private Camara() {
    }

    public static void init() {
        MinecraftForge.EVENT_BUS.addListener(Camara::cuadro);
    }

    public static Modo modo() {
        return modo;
    }

    public static void parar() {
        modo = Modo.NADA;
        PUNTOS.clear();
        objetivo = null;
    }

    /** Viaja suave desde donde estoy hasta (x, y, z) mirando con (yaw, pitch). */
    public static void viajar(double x, double y, double z, float yaw, float pitch) {
        LocalPlayer p = Minecraft.getInstance().player;
        if (p == null) {
            return;
        }
        PUNTOS.clear();
        PUNTOS.add(new double[] {p.getX(), p.getY(), p.getZ(), p.getYRot(), p.getXRot()});
        PUNTOS.add(new double[] {x, y, z, yaw, pitch});
        modo = Modo.VIAJE;
        inicio = System.currentTimeMillis();
    }

    /** Recorre las tomas en orden (cada tramo dura {@link #segundosPorTramo}). */
    public static void recorrido(List<double[]> tomas) {
        if (tomas.size() < 2) {
            return;
        }
        PUNTOS.clear();
        PUNTOS.addAll(tomas);
        modo = Modo.RECORRIDO;
        inicio = System.currentTimeMillis();
    }

    public static void seguir(UUID id) {
        objetivo = id;
        suave = null;
        giro = 0;
        modo = Modo.SEGUIR;
        ultimo = System.currentTimeMillis();
    }

    private static boolean moviendose(Minecraft mc) {
        var o = mc.options;
        return o.keyUp.isDown() || o.keyDown.isDown() || o.keyLeft.isDown() || o.keyRight.isDown() || o.keyJump.isDown()
                || o.keyShift.isDown();
    }

    private static void cuadro(TickEvent.RenderTickEvent e) {
        if (e.phase != TickEvent.Phase.START || modo == Modo.NADA) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null || mc.level == null) {
            parar();
            return;
        }
        if (mc.screen == null && moviendose(mc)) {
            parar();
            return;
        }
        long now = System.currentTimeMillis();
        switch (modo) {
            case VIAJE -> {
                float t = (now - inicio) / 1000F / Math.max(0.5F, segundosPorTramo * 0.6F);
                double[] a = PUNTOS.get(0), b = PUNTOS.get(1);
                float k = suave(Math.min(1, t));
                poner(p, Mth.lerp(k, a[0], b[0]), Mth.lerp(k, a[1], b[1]), Mth.lerp(k, a[2], b[2]),
                        Mth.rotLerp(k, (float) a[3], (float) b[3]), Mth.lerp(k, (float) a[4], (float) b[4]));
                if (t >= 1) {
                    parar();
                }
            }
            case RECORRIDO -> {
                int n = PUNTOS.size();
                float total = (n - 1) * segundosPorTramo;
                float s = (now - inicio) / 1000F;
                if (s >= total) {
                    double[] u = PUNTOS.get(n - 1);
                    poner(p, u[0], u[1], u[2], (float) u[3], (float) u[4]);
                    parar();
                    return;
                }
                // acelera el primer 10 %, va pareja y frena el último 10 % (sin tirones: la velocidad no salta)
                float g = s / total, a = 0.1F, v = 1 / (1 - a);
                float k = g < a ? v * g * g / (2 * a) : g > 1 - a ? 1 - v * (1 - g) * (1 - g) / (2 * a) : v * (g - a / 2);
                float pos = Mth.clamp(k, 0, 1) * (n - 1);
                int i = Math.min(n - 2, (int) pos);
                float f = pos - i;
                double[] p0 = PUNTOS.get(Math.max(0, i - 1)), p1 = PUNTOS.get(i), p2 = PUNTOS.get(i + 1), p3 = PUNTOS.get(Math.min(n - 1, i + 2));
                double x = cr(p0[0], p1[0], p2[0], p3[0], f), y = cr(p0[1], p1[1], p2[1], p3[1], f), z = cr(p0[2], p1[2], p2[2], p3[2], f);
                float fy = suave(f);
                poner(p, x, y, z, Mth.rotLerp(fy, (float) p1[3], (float) p2[3]), Mth.lerp(fy, (float) p1[4], (float) p2[4]));
            }
            case SEGUIR -> {
                Entity t = buscar(mc, objetivo);
                if (t == null) {
                    parar();
                    return;
                }
                float dt = Math.min(0.1F, (now - ultimo) / 1000F);
                ultimo = now;
                float pt = mc.getFrameTime();
                Vec3 o = new Vec3(Mth.lerp(pt, t.xo, t.getX()), Mth.lerp(pt, t.yo, t.getY()), Mth.lerp(pt, t.zo, t.getZ()));
                float cuerpo = t instanceof LivingEntity le ? Mth.rotLerp(pt, le.yBodyRotO, le.yBodyRot) : t.getYRot();
                giro += dt * 6F;
                float ang = (float) Math.toRadians(cuerpo + 180F + Mth.sin(giro * 0.05F) * 25F);
                Vec3 quiero = o.add(-Math.sin(ang) * 4.5, 2.2, Math.cos(ang) * 4.5);
                suave = suave == null ? quiero : suave.lerp(quiero, 1 - Math.exp(-dt * 3.0));
                Vec3 ojo = o.add(0, t.getBbHeight() * 0.85, 0);
                Vec3 d = ojo.subtract(suave.add(0, p.getEyeHeight(), 0));
                float yaw = (float) Math.toDegrees(Math.atan2(-d.x, d.z));
                float pitch = (float) Math.toDegrees(-Math.atan2(d.y, Math.hypot(d.x, d.z)));
                poner(p, suave.x, suave.y, suave.z, yaw, pitch);
            }
            default -> {
            }
        }
    }

    private static Entity buscar(Minecraft mc, UUID id) {
        if (id == null) {
            return null;
        }
        for (Entity e : mc.level.entitiesForRendering()) {
            if (id.equals(e.getUUID())) {
                return e;
            }
        }
        return null;
    }

    private static float suave(float x) {
        x = Mth.clamp(x, 0, 1);
        return x * x * (3 - 2 * x);
    }

    /** Catmull-Rom entre p1 y p2. */
    private static double cr(double p0, double p1, double p2, double p3, float t) {
        double t2 = t * t, t3 = t2 * t;
        return 0.5 * (2 * p1 + (-p0 + p2) * t + (2 * p0 - 5 * p1 + 4 * p2 - p3) * t2 + (-p0 + 3 * p1 - 3 * p2 + p3) * t3);
    }

    private static void poner(LocalPlayer p, double x, double y, double z, float yaw, float pitch) {
        p.setPos(x, y, z);
        p.xo = x;
        p.yo = y;
        p.zo = z;
        p.xOld = x;
        p.yOld = y;
        p.zOld = z;
        p.setDeltaMovement(0, 0, 0);
        p.setYRot(yaw);
        p.yRotO = yaw;
        p.setXRot(pitch);
        p.xRotO = pitch;
        p.yHeadRot = yaw;
        p.yHeadRotO = yaw;
    }
}
