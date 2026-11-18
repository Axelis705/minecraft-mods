package com.mundosdeaxelis.fx;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import com.mundosdeaxelis.network.ModNetwork;
import com.mundosdeaxelis.network.ScreenFxPacket;
import com.mundosdeaxelis.particle.GlowOptions;
import com.mundosdeaxelis.particle.ModParticles;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

/** Efectos de partículas y sonido que se repiten en varios objetos. Todo corre en el servidor. */
public final class Fx {
    private Fx() {
    }

    /** Color del arcoíris: t entre 0 y 1 (da la vuelta sola). */
    public static int rainbow(float t) {
        return Mth.hsvToRgb(t - (float) Math.floor(t), 0.72F, 1.0F);
    }

    /** Orbe de luz suave con color (textura propia). */
    public static ParticleOptions dust(int rgb, float size) {
        return new GlowOptions(ModParticles.GLOW.get(), rgb, size);
    }

    /** Estrella de cuatro puntas que titila. */
    public static ParticleOptions spark(int rgb, float size) {
        return new GlowOptions(ModParticles.SPARKLE.get(), rgb, size);
    }

    /** Papelito de confeti (rgb = 0: color al azar). */
    public static ParticleOptions paper(int rgb, float size) {
        return new GlowOptions(ModParticles.CONFETTI.get(), rgb, size);
    }

    /** Pétalo que cae meciéndose (rgb = 0: color al azar). */
    public static ParticleOptions petal(int rgb, float size) {
        return new GlowOptions(ModParticles.PETAL.get(), rgb, size);
    }

    public static ParticleOptions ember(int rgb, float size) {
        return new GlowOptions(ModParticles.EMBER.get(), rgb, size);
    }

    public static ParticleOptions rune(int rgb, float size) {
        return new GlowOptions(ModParticles.RUNE.get(), rgb, size);
    }

    public static ParticleOptions bolt(int rgb, float size) {
        return new GlowOptions(ModParticles.BOLT.get(), rgb, size);
    }

    public static ParticleOptions feather(int rgb, float size) {
        return new GlowOptions(ModParticles.FEATHER.get(), rgb, size);
    }

    public static ParticleOptions wisp(int rgb, float size) {
        return new GlowOptions(ModParticles.WISP.get(), rgb, size);
    }

    public static ParticleOptions shard(int rgb, float size) {
        return new GlowOptions(ModParticles.SHARD.get(), rgb, size);
    }

    public static ParticleOptions note(int rgb, float size) {
        return new GlowOptions(ModParticles.NOTE.get(), rgb, size);
    }

    public static ParticleOptions slash(int rgb, float size) {
        return new GlowOptions(ModParticles.SLASH.get(), rgb, size);
    }

    public static ParticleOptions heart(int rgb, float size) {
        return new GlowOptions(ModParticles.HEART.get(), rgb, size);
    }

    public static ParticleOptions bubble(int rgb, float size) {
        return new GlowOptions(ModParticles.BUBBLE.get(), rgb, size);
    }

    // Efectos en la pantalla del jugador (con texturas). Ver ScreenFxPacket.
    public static void screen(ServerPlayer p, int kind, int rgb, int ticks) {
        ModNetwork.sendTo(p, new ScreenFxPacket(kind, rgb, ticks));
    }

    public static void dustAt(ServerLevel l, int rgb, float size, double x, double y, double z, int n, double spread) {
        l.sendParticles(dust(rgb, size), x, y, z, n, spread, spread, spread, 0.0);
    }

    /** Un anillo horizontal de partículas. */
    public static void ring(ServerLevel l, ParticleOptions p, double cx, double cy, double cz, double r, int n, double phase) {
        for (int i = 0; i < n; i++) {
            double a = phase + Math.PI * 2.0 * i / n;
            l.sendParticles(p, cx + Math.cos(a) * r, cy, cz + Math.sin(a) * r, 1, 0, 0, 0, 0);
        }
    }

    /** Un anillo de colores del arcoíris. */
    public static void rainbowRing(ServerLevel l, double cx, double cy, double cz, double r, int n, double phase) {
        for (int i = 0; i < n; i++) {
            double a = phase + Math.PI * 2.0 * i / n;
            l.sendParticles(dust(rainbow((float) i / n), 1.1F), cx + Math.cos(a) * r, cy, cz + Math.sin(a) * r,
                    1, 0, 0, 0, 0);
        }
    }

    /** Espiral que sube (hélice doble). */
    public static void helix(ServerLevel l, ParticleOptions p, double cx, double cy, double cz, double r, double h, int n, double phase) {
        for (int i = 0; i < n; i++) {
            double u = (double) i / n;
            double a = phase + u * Math.PI * 4.0;
            l.sendParticles(p, cx + Math.cos(a) * r, cy + u * h, cz + Math.sin(a) * r, 1, 0, 0, 0, 0);
            l.sendParticles(p, cx + Math.cos(a + Math.PI) * r, cy + u * h, cz + Math.sin(a + Math.PI) * r, 1, 0, 0, 0, 0);
        }
    }

    /** Estallido de confeti de colores: papelitos que salen disparados y caen dando vueltas. */
    public static void confetti(ServerLevel l, double x, double y, double z, int n, double spread) {
        RandomSource r = l.getRandom();
        for (int i = 0; i < n; i++) {
            l.sendParticles(paper(rainbow(r.nextFloat()), 0.9F + r.nextFloat() * 0.8F),
                    x + (r.nextDouble() - 0.5) * spread * 0.4, y + r.nextDouble() * spread * 0.3,
                    z + (r.nextDouble() - 0.5) * spread * 0.4, 0,
                    (r.nextDouble() - 0.5) * 0.45, 0.12 + r.nextDouble() * 0.28, (r.nextDouble() - 0.5) * 0.45, 1.0);
        }
        for (int i = 0; i < Math.max(3, n / 3); i++) {
            l.sendParticles(spark(0xFFF0A8, 0.8F + r.nextFloat() * 0.8F),
                    x + (r.nextDouble() - 0.5) * spread, y + r.nextDouble() * spread * 0.7,
                    z + (r.nextDouble() - 0.5) * spread, 1, 0.0, 0.0, 0.0, 0.0);
        }
        l.sendParticles(ParticleTypes.FIREWORK, x, y + spread * 0.3, z, n / 3, spread * 0.4, spread * 0.3, spread * 0.4, 0.08);
    }

    /** Línea de partículas de colores entre dos puntos. */
    public static void rainbowLine(ServerLevel l, Vec3 a, Vec3 b, double step, float phase) {
        Vec3 d = b.subtract(a);
        double len = d.length();
        if (len < 0.01) {
            return;
        }
        Vec3 dir = d.scale(1.0 / len);
        int i = 0;
        for (double s = 0; s < len; s += step, i++) {
            Vec3 p = a.add(dir.scale(s));
            l.sendParticles(dust(rainbow(phase + (float) (s * 0.12)), 1.0F), p.x, p.y, p.z, 1, 0.02, 0.02, 0.02, 0.0);
        }
    }

    public static void sound(ServerLevel l, double x, double y, double z, SoundEvent s, SoundSource src, float vol, float pitch) {
        l.playSound(null, x, y, z, s, src, vol, pitch);
    }
}
