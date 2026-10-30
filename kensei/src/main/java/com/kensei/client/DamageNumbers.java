package com.kensei.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * Los números de daño que suben desde el golpe (con un "pop" al aparecer y se desvanecen) y el nombre del movimiento que aparece
 * bajo la mira (CRÍTICO, PARRY PERFECTO...). Se dibujan en pantalla proyectando el punto del mundo, así que siempre se ven nítidos.
 */
public final class DamageNumbers {
    private static final long NUM_MS = 950;
    private static final long BANNER_MS = 1100;
    private static final Random RNG = new Random();

    private record Num(Vec3 pos, String text, int rgb, float scale, long start, float drift) {
    }

    private static final List<Num> NUMS = new ArrayList<>();
    private static String bannerKey = null;
    private static int bannerRgb = 0xFFFFFF;
    private static long bannerStart = 0;

    // contador de golpes seguidos (arriba a la derecha de la mira)
    private static final long COMBO_KEEP_MS = 2300;
    private static final long COMBO_FADE_MS = 350;
    private static int comboCount = 0;
    private static float comboDamage = 0;
    private static long comboLast = 0;
    private static long comboPop = 0;

    private DamageNumbers() {
    }

    /** Un golpe tuyo conectó: sube el contador de golpes seguidos (varios enemigos en el mismo tajo cuentan como un solo golpe). */
    public static void comboHit(float damage) {
        long now = System.currentTimeMillis();
        if (now - comboLast > COMBO_KEEP_MS) {
            comboCount = 0;
            comboDamage = 0;
        }
        if (comboCount == 0 || now - comboLast > 80) {
            comboCount++;
            comboPop = now;
        }
        comboDamage += damage;
        comboLast = now;
    }

    public static void clear() {
        comboCount = 0;
        NUMS.clear();
        bannerKey = null;
    }

    public static void add(double x, double y, double z, float value, int rgb, float scale) {
        float v = Math.round(value * 10.0F) / 10.0F;
        String text = v == Math.floor(v) ? Integer.toString((int) v) : Float.toString(v);
        if (NUMS.size() > 30) {
            NUMS.remove(0);
        }
        NUMS.add(new Num(new Vec3(x + (RNG.nextDouble() - 0.5) * 0.5, y + 0.2, z + (RNG.nextDouble() - 0.5) * 0.5), text, rgb, scale,
                System.currentTimeMillis(), (RNG.nextFloat() - 0.5F) * 30.0F));
    }

    public static void banner(String langKey, int rgb) {
        bannerKey = langKey;
        bannerRgb = rgb;
        bannerStart = System.currentTimeMillis();
    }

    private static int argb(int rgb, float alpha) {
        int a = Mth.clamp((int) (alpha * 255.0F), 8, 255);
        return (a << 24) | (rgb & 0xFFFFFF);
    }

    public static void render(GuiGraphics g, int w, int h) {
        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;
        long now = System.currentTimeMillis();

        Iterator<Num> it = NUMS.iterator();
        while (it.hasNext()) {
            Num n = it.next();
            long age = now - n.start;
            if (age > NUM_MS) {
                it.remove();
                continue;
            }
            float t = age / (float) NUM_MS;
            float rise = 1.0F - (1.0F - t) * (1.0F - t) * (1.0F - t);
            Vec3 world = n.pos.add(0, 0.9 * rise, 0);
            float[] sp = ClientLock.haveMatrices ? ClientLock.project(world, w, h) : null;
            if (sp == null) {
                continue;
            }
            float pop = 1.0F + 0.9F * (float) Math.exp(-age / 55.0);
            float scale = n.scale * 1.25F * pop * (t > 0.7F ? 1.0F - (t - 0.7F) * 0.8F : 1.0F);
            float alpha = t < 0.6F ? 1.0F : 1.0F - (t - 0.6F) / 0.4F;
            g.pose().pushPose();
            g.pose().translate(sp[0] + n.drift * rise, sp[1], 0);
            g.pose().scale(scale, scale, 1.0F);
            int tw = font.width(n.text);
            // contorno oscuro para que se lea sobre cualquier fondo
            int dark = argb(0x101018, alpha);
            for (int[] o : new int[][] {{-1, 0}, {1, 0}, {0, -1}, {0, 1}}) {
                g.drawString(font, n.text, -tw / 2 + o[0], -4 + o[1], dark, false);
            }
            g.drawString(font, n.text, -tw / 2, -4, argb(n.rgb, alpha), false);
            g.pose().popPose();
        }

        renderCombo(g, font, now, w, h);

        if (bannerKey != null) {
            long age = now - bannerStart;
            if (age > BANNER_MS) {
                bannerKey = null;
                return;
            }
            float t = age / (float) BANNER_MS;
            float pop = 1.0F + 0.55F * (float) Math.exp(-age / 70.0);
            float alpha = t < 0.65F ? 1.0F : 1.0F - (t - 0.65F) / 0.35F;
            Component c = Component.translatable(bannerKey);
            String text = c.getString().toUpperCase(java.util.Locale.ROOT);
            int tw = font.width(text);
            float scale = 2.2F * pop;
            float cx = w / 2.0F;
            float cy = h / 2.0F + 46.0F;
            g.pose().pushPose();
            g.pose().translate(cx, cy, 0);
            g.pose().scale(scale, scale, 1.0F);
            int dark = argb(0x0A0A12, alpha);
            for (int[] o : new int[][] {{-1, 0}, {1, 0}, {0, -1}, {0, 1}}) {
                g.drawString(font, text, -tw / 2 + o[0], o[1], dark, false);
            }
            g.drawString(font, text, -tw / 2, 0, argb(bannerRgb, alpha), false);
            g.pose().popPose();
            // dos líneas que se abren a los lados
            float open = Mth.clamp(age / 140.0F, 0.0F, 1.0F);
            int half = (int) (tw * scale / 2.0F + 10 + 40 * open);
            int len = (int) (50 * open);
            int ly = (int) (cy + 5 * scale);
            g.fill((int) cx - half - len, ly, (int) cx - half, ly + 1, argb(bannerRgb, alpha * 0.8F));
            g.fill((int) cx + half, ly, (int) cx + half + len, ly + 1, argb(bannerRgb, alpha * 0.8F));
        }
    }

    private static void renderCombo(GuiGraphics g, Font font, long now, int w, int h) {
        if (comboCount < 2) {
            return;
        }
        long idle = now - comboLast;
        if (idle > COMBO_KEEP_MS + COMBO_FADE_MS) {
            comboCount = 0;
            return;
        }
        float alpha = idle <= COMBO_KEEP_MS ? 1.0F : 1.0F - (idle - COMBO_KEEP_MS) / (float) COMBO_FADE_MS;
        int rgb = comboCount >= 10 ? 0xFF4A4A : comboCount >= 5 ? 0xFFD040 : 0xFFFFFF;
        float pop = 1.0F + 0.7F * (float) Math.exp(-(now - comboPop) / 90.0);
        String num = Integer.toString(comboCount);
        String label = Component.translatable("kensei.fx.hits").getString().toUpperCase(java.util.Locale.ROOT);
        int x = w - 26;
        int y = (int) (h * 0.36F);
        int dark = argb(0x0A0A12, alpha);
        // número grande, pegado a la derecha
        float big = 3.4F * pop;
        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        g.pose().scale(big, big, 1.0F);
        int nw = font.width(num);
        for (int[] o : new int[][] {{-1, 0}, {1, 0}, {0, -1}, {0, 1}}) {
            g.drawString(font, num, -nw + o[0], o[1], dark, false);
        }
        g.drawString(font, num, -nw, 0, argb(rgb, alpha), false);
        g.pose().popPose();
        // etiqueta y daño total debajo
        g.pose().pushPose();
        g.pose().translate(x, y + 32, 0);
        g.pose().scale(1.4F, 1.4F, 1.0F);
        int lw = font.width(label);
        g.drawString(font, label, -lw, 0, argb(rgb, alpha), true);
        g.pose().popPose();
        String dmg = Math.round(comboDamage) + " dmg";
        g.drawString(font, dmg, x - font.width(dmg), y + 48, argb(0xC8D0DC, alpha * 0.85F), true);
        // barrita que se vacía con el tiempo que queda para seguir el combo
        float left = Mth.clamp(1.0F - idle / (float) COMBO_KEEP_MS, 0.0F, 1.0F);
        int bw = 70;
        g.fill(x - bw, y + 60, x, y + 62, argb(0x202630, alpha * 0.8F));
        g.fill(x - (int) (bw * left), y + 60, x, y + 62, argb(rgb, alpha));
    }
}
