package com.kensei.client;

import com.kensei.KenseiConfig;
import com.kensei.registry.ModParticles;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/**
 * La estela brillante del filo. Cada tick se calcula por dónde pasa la hoja en ese tajo (un arco delante del jugador) y se dejan
 * destellos a lo largo de ese tramo. Funciona para todos los jugadores que se ven, porque usa los mismos avisos de movimiento que las
 * animaciones.
 */
public final class SlashTrail {
    private SlashTrail() {
    }

    /** Duración del tajo normal en ticks (la del swing del juego). */
    private static final float SWING_TICKS = 6.0F;

    public static void tick(Minecraft mc) {
        ClientLevel level = mc.level;
        if (level == null || mc.isPaused()) {
            return;
        }
        long now = level.getGameTime();
        ClientAnims.forEachActive((id, a) -> {
            Entity e = level.getEntity(id);
            if (e == null || !e.isAlive()) {
                return;
            }
            int age = (int) (now - a.start);
            if (age < 0) {
                return;
            }
            switch (a.move) {
                case 1, 2, 3, 4 -> {
                    if (age < SWING_TICKS) {
                        emitSlash(level, e, a.move, age / SWING_TICKS, (age + 1) / SWING_TICKS, 1.0F);
                    }
                }
                case ClientAnims.THRUST -> {
                    if (age < SWING_TICKS) {
                        emitThrust(level, e, age / SWING_TICKS, (age + 1) / SWING_TICKS);
                    }
                }
                case ClientAnims.HEAVY -> {
                    int w = KenseiConfig.HEAVY_WINDUP.get();
                    if (age >= w && age < w + 5) {
                        emitSlash(level, e, 5, (age - w) / 5.0F, (age - w + 1) / 5.0F, 1.5F);
                    }
                }
                case ClientAnims.DODGE -> {
                    if (age < 9) {
                        emitDash(level, e, a.arg);
                    }
                }
                default -> {
                }
            }
        });
    }

    private static Vec3 forward(Entity e) {
        double yaw = Math.toRadians(e.getYRot());
        return new Vec3(-Math.sin(yaw), 0, Math.cos(yaw));
    }

    private static Vec3 right(Entity e) {
        double yaw = Math.toRadians(e.getYRot());
        return new Vec3(-Math.cos(yaw), 0, -Math.sin(yaw));
    }

    private static float ease(float u) {
        return u * u * (3.0F - 2.0F * u);
    }

    /** Un punto del arco del tajo para el progreso u (0 a 1). */
    private static Vec3 arcPoint(Entity e, int move, float u, double radius) {
        Vec3 f = forward(e);
        Vec3 r = right(e);
        double eu = ease(u);
        double baseY = e.getY() + e.getBbHeight() * 0.62;
        Vec3 origin = new Vec3(e.getX(), baseY, e.getZ()).add(r.scale(0.2));
        double ang;
        switch (move) {
            case 1: { // horizontal, de derecha a izquierda
                ang = Math.toRadians(60.0 - 125.0 * eu);
                Vec3 d = f.scale(Math.cos(ang)).add(r.scale(Math.sin(ang)));
                return origin.add(d.scale(radius)).add(0, 0.1 - 0.1 * eu, 0);
            }
            case 2: { // de vuelta
                ang = Math.toRadians(-65.0 + 125.0 * eu);
                Vec3 d = f.scale(Math.cos(ang)).add(r.scale(Math.sin(ang)));
                return origin.add(d.scale(radius)).add(0, 0.1 - 0.1 * eu, 0);
            }
            case 3: { // diagonal de arriba hacia abajo (y de derecha a izquierda)
                ang = Math.toRadians(75.0 - 105.0 * eu);
                double side = 0.7 - 1.0 * eu;
                Vec3 d = f.scale(Math.cos(ang)).add(r.scale(side * 0.5));
                return origin.add(d.scale(radius)).add(0, Math.sin(ang) * radius, 0);
            }
            case 4: { // ascendente
                ang = Math.toRadians(-40.0 + 125.0 * eu);
                Vec3 d = f.scale(Math.cos(ang)).add(r.scale(-0.25 + 0.4 * eu));
                return origin.add(d.scale(radius)).add(0, Math.sin(ang) * radius, 0);
            }
            default: { // 5: crítico, un tajo vertical grande de arriba hacia abajo
                ang = Math.toRadians(85.0 - 125.0 * eu);
                Vec3 d = f.scale(Math.cos(ang));
                return origin.add(d.scale(radius)).add(0, Math.sin(ang) * radius, 0);
            }
        }
    }

    private static void emitSlash(ClientLevel level, Entity e, int move, float u0, float u1, float density) {
        int steps = Math.max(3, Math.round(5 * density));
        for (int i = 0; i < steps; i++) {
            float u = u0 + (u1 - u0) * (i / (float) steps);
            for (double radius : (density > 1.2F ? new double[] {1.1, 1.7, 2.3} : new double[] {1.2, 1.75})) {
                Vec3 p = arcPoint(e, move, u, radius);
                level.addParticle(ModParticles.SLASH.get(), p.x, p.y, p.z, 0, 0, 0);
            }
        }
    }

    /** Rayas de viento que se quedan atrás al esquivar (arg = hacia dónde va el impulso, en grados). */
    private static void emitDash(ClientLevel level, Entity e, float dirYaw) {
        double yaw = Math.toRadians(dirYaw);
        Vec3 dir = new Vec3(-Math.sin(yaw), 0, Math.cos(yaw));
        for (int i = 0; i < 5; i++) {
            double back = 0.2 + level.random.nextDouble() * 1.6;
            double side = (level.random.nextDouble() - 0.5) * 0.7;
            double up = 0.15 + level.random.nextDouble() * 1.5;
            Vec3 perp = new Vec3(-dir.z, 0, dir.x);
            Vec3 p = new Vec3(e.getX(), e.getY() + up, e.getZ()).subtract(dir.scale(back)).add(perp.scale(side));
            level.addParticle(ModParticles.SLASH.get(), p.x, p.y, p.z, 0, 0, 0);
        }
    }

    private static void emitThrust(ClientLevel level, Entity e, float u0, float u1) {
        Vec3 f = forward(e);
        Vec3 r = right(e);
        Vec3 origin = new Vec3(e.getX(), e.getY() + e.getBbHeight() * 0.62, e.getZ()).add(r.scale(0.2));
        for (int i = 0; i < 4; i++) {
            float u = u0 + (u1 - u0) * (i / 4.0F);
            double reach = 0.7 + 2.1 * Math.sin(u * Math.PI * 0.9);
            for (double dr = 0.0; dr <= 1.0; dr += 0.5) {
                Vec3 p = origin.add(f.scale(reach - dr * 0.5)).add(0, -0.05 * dr, 0);
                level.addParticle(ModParticles.SLASH.get(), p.x, p.y, p.z, 0, 0, 0);
            }
        }
    }
}
