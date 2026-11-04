package com.mundosdeaxelis.client;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Axis;
import com.mundosdeaxelis.MundosDeAxelis;
import com.mundosdeaxelis.network.ScreenFxPacket;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Efectos con texturas dibujados sobre la pantalla del jugador. Solo cliente. */
public final class ScreenFx {
    private static final ResourceLocation VIGNETTE = tex("vignette");
    private static final ResourceLocation BUBBLE = tex("bubble");
    private static final ResourceLocation CONFETTI = tex("confetti");
    private static final ResourceLocation HEART = tex("heart");
    private static final ResourceLocation SPARKLE = tex("sparkle");
    private static final ResourceLocation SPEED = tex("speed");
    private static final ResourceLocation PETAL = tex("petal");
    private static final ResourceLocation BURST = new ResourceLocation(MundosDeAxelis.MODID, "textures/effect/starburst.png");

    private record Active(int kind, int rgb, double start, int dur, long seed) {
    }

    private static final List<Active> ACTIVE = new ArrayList<>();

    private ScreenFx() {
    }

    private static ResourceLocation tex(String name) {
        return new ResourceLocation(MundosDeAxelis.MODID, "textures/gui/fx/" + name + ".png");
    }

    private static double now() {
        return Util.getMillis() / 50.0;
    }

    /** Fuerza actual de la sacudida de cámara, en grados. */
    public static float shake() {
        if (ACTIVE.isEmpty()) {
            return 0.0F;
        }
        double now = now();
        float best = 0.0F;
        for (Active a : ACTIVE) {
            if (a.kind() == ScreenFxPacket.SHAKE) {
                float t = (float) (now - a.start());
                if (t >= 0 && t <= a.dur()) {
                    float k = 1.0F - t / a.dur();
                    best = Math.max(best, a.rgb() / 10.0F * k * k);
                }
            }
        }
        return best;
    }

    public static void start(int kind, int rgb, int ticks) {
        if (ACTIVE.size() > 10) {
            ACTIVE.remove(0);
        }
        ACTIVE.add(new Active(kind, rgb, now(), Math.max(4, ticks), System.nanoTime()));
    }

    public static void render(GuiGraphics g, float partialTick, int w, int h) {
        if (ACTIVE.isEmpty()) {
            return;
        }
        double now = now();
        RenderSystem.enableBlend();
        for (int i = ACTIVE.size() - 1; i >= 0; i--) {
            Active a = ACTIVE.get(i);
            float t = (float) (now - a.start());
            if (t > a.dur() || t < 0) {
                ACTIVE.remove(i);
                continue;
            }
            float env = Mth.sin(Mth.clamp(t / a.dur(), 0.0F, 1.0F) * 3.14159F);
            env = (float) Math.pow(env, 0.6);
            float r = ((a.rgb() >> 16) & 255) / 255.0F;
            float gr = ((a.rgb() >> 8) & 255) / 255.0F;
            float b = (a.rgb() & 255) / 255.0F;
            Random rnd = new Random(a.seed());
            switch (a.kind()) {
                case ScreenFxPacket.FLASH -> flash(g, w, h, t, a.dur(), r, gr, b);
                case ScreenFxPacket.VIGNETTE -> vignette(g, w, h, t, env, r, gr, b);
                case ScreenFxPacket.BUBBLES -> {
                    vignette(g, w, h, t, env * 0.55F, 0.45F, 0.85F, 1.0F);
                    rising(g, BUBBLE, rnd, 28, w, h, t, env, 0.75F, 0.95F, 1.0F, 10, 28, 0.9F, false);
                }
                case ScreenFxPacket.HEARTS -> {
                    vignette(g, w, h, t, env * 0.5F, r, gr, b);
                    rising(g, HEART, rnd, 22, w, h, t, env, r, gr, b, 12, 30, 1.0F, true);
                }
                case ScreenFxPacket.CONFETTI -> confetti(g, rnd, 64, w, h, t, a.dur(), env);
                case ScreenFxPacket.STARS -> stars(g, rnd, 36, w, h, t, a.dur(), r, gr, b);
                case ScreenFxPacket.SPEED -> speed(g, w, h, t, env, a.seed());
                case ScreenFxPacket.PETALS -> {
                    vignette(g, w, h, t, env * 0.35F, 1.0F, 0.6F, 0.8F);
                    rising(g, PETAL, rnd, 30, w, h, t, env, r, gr, b, 12, 26, 0.95F, false);
                }
                case ScreenFxPacket.DARK -> {
                    normal();
                    g.setColor(r * 0.25F, gr * 0.25F, b * 0.25F, Mth.clamp(env * 0.95F, 0.0F, 1.0F));
                    g.blit(VIGNETTE, 0, 0, w, h, 0.0F, 0.0F, 256, 256, 256, 256);
                    g.setColor(1.0F, 1.0F, 1.0F, 1.0F);
                }
                case ScreenFxPacket.WHITEOUT -> whiteout(g, rnd, w, h, t, a.dur(), r, gr, b);
                case ScreenFxPacket.SUNRISE -> sunrise(g, rnd, w, h, t, a.dur(), env, r, gr, b);
                default -> {
                }
            }
        }
        RenderSystem.defaultBlendFunc();
        g.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.disableBlend();
    }

    private static void additive() {
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
    }

    private static void normal() {
        RenderSystem.defaultBlendFunc();
    }

    private static void quad(GuiGraphics g, ResourceLocation tex, float cx, float cy, float sw, float sh, float rotDeg, int texSize) {
        g.pose().pushPose();
        g.pose().translate(cx, cy, 0.0F);
        if (rotDeg != 0.0F) {
            g.pose().mulPose(Axis.ZP.rotationDegrees(rotDeg));
        }
        g.blit(tex, (int) (-sw / 2.0F), (int) (-sh / 2.0F), (int) sw, (int) sh, 0.0F, 0.0F, texSize, texSize, texSize, texSize);
        g.pose().popPose();
    }

    /** Ventisca: bordes blancos, un velo que tapa algo de vista y rayas de nieve que cruzan la pantalla de lado. */
    private static void whiteout(GuiGraphics g, Random rnd, int w, int h, float t, int dur, float r, float gr, float b) {
        float in = Mth.clamp(t / 10.0F, 0.0F, 1.0F);
        float out = Mth.clamp((dur - t) / 14.0F, 0.0F, 1.0F);
        float k = in * out;
        normal();
        g.setColor(r, gr, b, Mth.clamp(k * 0.95F, 0.0F, 1.0F));
        g.blit(VIGNETTE, 0, 0, w, h, 0.0F, 0.0F, 256, 256, 256, 256);
        g.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        int veil = (int) (k * 70.0F);
        g.fill(0, 0, w, h, (veil << 24) | (((int) (r * 255) & 255) << 16) | (((int) (gr * 255) & 255) << 8) | ((int) (b * 255) & 255));
        for (int i = 0; i < 46; i++) {
            float y = rnd.nextFloat() * h;
            float len = 30.0F + rnd.nextFloat() * 120.0F;
            float speed = 14.0F + rnd.nextFloat() * 22.0F;
            float x = ((t * speed + rnd.nextFloat() * (w + 240)) % (w + 240)) - 120.0F;
            int al = (int) (k * (40 + rnd.nextInt(90)));
            int col = (al << 24) | 0xEAF6FF;
            g.fill((int) x, (int) y, (int) (x + len), (int) y + 1 + (i % 3 == 0 ? 1 : 0), col);
        }
    }

    /** Destello suave: solo un estallido de rayos centrado, sin cubrir la pantalla. */
    private static void flash(GuiGraphics g, int w, int h, float t, int dur, float r, float gr, float b) {
        float k = 1.0F - t / dur;
        float a = k * k;
        additive();
        g.setColor(r, gr, b, Math.min(1.0F, a * 0.55F));
        float size = Math.min(w, h) * (0.5F + 0.5F * (1.0F - k));
        quad(g, BURST, w / 2.0F, h / 2.0F, size, size, t * 4.0F, 128);
        normal();
        g.setColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private static void vignette(GuiGraphics g, int w, int h, float t, float env, float r, float gr, float b) {
        float pulse = 0.82F + 0.18F * Mth.sin(t * 0.45F);
        additive();
        g.setColor(r, gr, b, Mth.clamp(env * pulse, 0.0F, 1.0F));
        g.blit(VIGNETTE, 0, 0, w, h, 0.0F, 0.0F, 256, 256, 256, 256);
        normal();
        g.setColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private static void rising(GuiGraphics g, ResourceLocation tex, Random rnd, int n, int w, int h, float t, float env,
                               float r, float gr, float b, int minSize, int maxSize, float baseAlpha, boolean add) {
        if (add) {
            additive();
        } else {
            normal();
        }
        for (int i = 0; i < n; i++) {
            float x0 = rnd.nextFloat() * w;
            float size = minSize + rnd.nextFloat() * (maxSize - minSize);
            float speed = 0.9F + rnd.nextFloat() * 1.8F;
            float off = rnd.nextFloat() * (h + size * 2);
            float phase = rnd.nextFloat() * 6.28F;
            float y = h + size - ((t * speed * 2.2F + off) % (h + size * 2));
            float x = x0 + Mth.sin(t * 0.09F + phase) * 16.0F;
            g.setColor(r, gr, b, Mth.clamp(env * baseAlpha, 0.0F, 1.0F));
            quad(g, tex, x, y, size, size, Mth.sin(t * 0.07F + phase) * 14.0F, 32);
        }
        normal();
        g.setColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private static void confetti(GuiGraphics g, Random rnd, int n, int w, int h, float t, int dur, float env) {
        normal();
        for (int i = 0; i < n; i++) {
            float x0 = rnd.nextFloat() * w;
            float delay = rnd.nextFloat() * dur * 0.35F;
            float speed = 2.6F + rnd.nextFloat() * 3.2F;
            float phase = rnd.nextFloat() * 6.28F;
            float spin = (rnd.nextFloat() - 0.5F) * 14.0F;
            float hue = rnd.nextFloat();
            float tt = t - delay;
            if (tt < 0) {
                continue;
            }
            float y = -16.0F + tt * speed;
            if (y > h + 16) {
                continue;
            }
            float x = x0 + Mth.sin(tt * 0.11F + phase) * 34.0F;
            int rgb = Mth.hsvToRgb(hue, 0.7F, 1.0F);
            float flip = Math.abs(Mth.cos(tt * 0.22F + phase));
            float sw = 14.0F * (0.25F + 0.75F * flip);
            float fade = Mth.clamp((dur - t) / 12.0F, 0.0F, 1.0F);
            g.setColor(((rgb >> 16) & 255) / 255.0F, ((rgb >> 8) & 255) / 255.0F, (rgb & 255) / 255.0F, fade);
            g.pose().pushPose();
            g.pose().translate(x, y, 0.0F);
            g.pose().mulPose(Axis.ZP.rotationDegrees(tt * spin));
            g.blit(CONFETTI, (int) (-sw / 2), -7, (int) Math.max(2, sw), 14, 0.0F, 0.0F, 16, 16, 16, 16);
            g.pose().popPose();
        }
        g.setColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private static void stars(GuiGraphics g, Random rnd, int n, int w, int h, float t, int dur, float r, float gr, float b) {
        additive();
        for (int i = 0; i < n; i++) {
            float x = rnd.nextFloat() * w;
            float y = rnd.nextFloat() * h;
            float begin = rnd.nextFloat() * dur * 0.8F;
            float life = 12.0F + rnd.nextFloat() * 14.0F;
            float size = 12.0F + rnd.nextFloat() * 34.0F;
            float local = t - begin;
            if (local < 0 || local > life) {
                continue;
            }
            float a = Mth.sin(local / life * 3.14159F);
            g.setColor(r, gr, b, Mth.clamp(a, 0.0F, 1.0F));
            float s = size * (0.5F + 0.6F * a);
            quad(g, SPARKLE, x, y, s, s, local * 3.0F, 32);
        }
        normal();
        g.setColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private static void speed(GuiGraphics g, int w, int h, float t, float env, long seed) {
        int rgb = Mth.hsvToRgb((t * 0.03F) % 1.0F, 0.55F, 1.0F);
        additive();
        g.setColor(((rgb >> 16) & 255) / 255.0F, ((rgb >> 8) & 255) / 255.0F, (rgb & 255) / 255.0F, Mth.clamp(env * 0.85F, 0.0F, 1.0F));
        float size = Math.max(w, h) * (1.5F + 0.12F * Mth.sin(t * 0.8F));
        quad(g, SPEED, w / 2.0F, h / 2.0F, size, size, (seed % 360) + t * 9.0F, 256);
        normal();
        g.setColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    /** Sol que sale por abajo con rayos giratorios y destellos. */
    private static void sunrise(GuiGraphics g, Random rnd, int w, int h, float t, int dur, float env, float r, float gr, float b) {
        float u = Mth.clamp(t / dur, 0.0F, 1.0F);
        float rise = 1.0F - (1.0F - u) * (1.0F - u);
        additive();
        g.setColor(r, gr, b, Mth.clamp(env * 0.9F, 0.0F, 1.0F));
        float size = Math.max(w, h) * (1.2F + 0.5F * rise);
        quad(g, BURST, w / 2.0F, h * (1.15F - 0.45F * rise), size, size, t * 1.2F, 256);
        g.setColor(1.0F, 0.95F, 0.8F, Mth.clamp(env * 0.5F, 0.0F, 1.0F));
        quad(g, BURST, w / 2.0F, h * (1.15F - 0.45F * rise), size * 0.6F, size * 0.6F, -t * 1.9F, 256);
        normal();
        vignette(g, w, h, t, env * 0.5F, r, gr, b);
        for (int i = 0; i < 18; i++) {
            float x = rnd.nextFloat() * w;
            float y = h * (0.15F + rnd.nextFloat() * 0.7F);
            float ph = rnd.nextFloat() * 40.0F;
            float local = t - ph;
            if (local < 0 || local > 28) {
                continue;
            }
            float a = Mth.sin(local / 28.0F * 3.14159F);
            additive();
            g.setColor(1.0F, 0.93F, 0.7F, Mth.clamp(a * env * 1.2F, 0.0F, 1.0F));
            float sz = 10 + 16 * a;
            quad(g, SPARKLE, x, y, sz, sz, local * 3.0F, 32);
        }
        normal();
        g.setColor(1.0F, 1.0F, 1.0F, 1.0F);
    }
}
