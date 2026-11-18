package com.mundosdeaxelis.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mundosdeaxelis.gear.GearTool;
import com.mundosdeaxelis.gear.SoulUser;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;

/**
 * Contador de almas: un frasco vertical pegado al borde derecho, a media altura (lejos de la barra de objetos).
 * Siempre está a la vista: las almas se guardan en el jugador (no en el arma) y se ganan al matar con cualquier espada de set.
 */
public final class SoulsHud {
    private SoulsHud() {
    }

    private static int souls;
    private static int max = 50;
    private static float shown;
    private static long lastChange = -1000;
    private static float pulse;
    private static float flash;
    private static float alpha;

    public static void set(int value, int maxValue) {
        Minecraft mc = Minecraft.getInstance();
        long now = mc.level == null ? 0 : mc.level.getGameTime();
        if (value > souls) {
            pulse = 1.0F;
        } else if (value < souls) {
            flash = 1.0F;
        }
        if (value != souls) {
            lastChange = now;
        }
        souls = value;
        max = Math.max(1, maxValue);
    }

    private static boolean holding(Player p) {
        for (ItemStack s : new ItemStack[]{p.getMainHandItem(), p.getOffhandItem()}) {
            if (s.getItem() instanceof SoulUser || (s.getItem() instanceof SwordItem && s.getItem() instanceof GearTool)) {
                return true;
            }
        }
        return false;
    }

    public static void render(GuiGraphics g, float pt, int width, int height) {
        Minecraft mc = Minecraft.getInstance();
        Player p = mc.player;
        if (p == null || mc.options.hideGui) {
            return;
        }
        long now = mc.level == null ? 0 : mc.level.getGameTime();
        // siempre visible (las almas son del jugador, no del arma); solo se oculta en las escenas con cámara de cine
        boolean show = !CinematicClient.isActive();
        alpha = Mth.lerp(0.18F, alpha, show ? 1.0F : 0.0F);
        if (alpha < 0.02F) {
            return;
        }
        pulse = Math.max(0, pulse - 0.04F);
        flash = Math.max(0, flash - 0.06F);
        shown = Mth.lerp(0.12F, shown, souls);
        Font font = mc.font;
        int w = 26, h = 100;
        int x = width - w - 4;
        int y = height / 2 - h / 2 + 10;
        int a = (int) (alpha * 255);
        float time = (now + pt);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        // panel con esquinas recortadas: borde tenue y fondo azul muy oscuro
        int edge = lerpRgb(0x4FC8E8, 0xFFFFFF, pulse);
        soft(g, x - 1, y - 1, w + 2, h + 2, (int) (a * 0.55F) << 24 | edge);
        soft(g, x, y, w, h, (int) (a * 0.80F) << 24 | 0x0A1022);
        soft(g, x + 2, y + 2, w - 4, 22, (int) (a * 0.30F) << 24 | 0x1B2A55);

        // frasco de cristal
        int gx = x + 8, gy = y + 30, gw = 10, gh = 50;
        g.fill(gx - 1, gy - 1, gx + gw + 1, gy + gh + 1, (int) (a * 0.85F) << 24 | 0x2E3C66);
        g.fill(gx, gy, gx + gw, gy + gh, (int) (a * 0.95F) << 24 | 0x040610);
        float frac = Mth.clamp(shown / max, 0, 1);
        int fh = Math.round(gh * frac);
        if (souls > 0 && fh < 2) {
            fh = 2;
        }
        for (int i = 0; i < fh; i++) {
            float t = i / (float) gh;
            int col = lerpRgb(0x1A6CF0, 0x9CF0FF, t);
            if (flash > 0) {
                col = lerpRgb(col, 0xFF6A8A, flash * 0.7F);
            }
            g.fill(gx, gy + gh - 1 - i, gx + gw, gy + gh - i, a << 24 | col);
        }
        // burbujas que suben
        if (fh > 4) {
            for (int k = 0; k < 4; k++) {
                float ph = (time * (0.35F + 0.08F * k) + k * 13.0F) % Math.max(4, fh - 2);
                int bx = gx + 2 + ((k * 5 + 1) % (gw - 3));
                int by = gy + gh - 2 - (int) ph;
                g.fill(bx, by, bx + 1, by + 1, (int) (a * 0.65F) << 24 | 0xFFFFFF);
            }
        }
        // superficie con brillo
        if (fh > 1 && fh < gh) {
            int sy = gy + gh - fh;
            g.fill(gx, sy, gx + gw, sy + 1, (int) (a * 0.85F) << 24 | 0xE8FFFF);
        }
        // reflejo del cristal y marcas
        g.fill(gx + 1, gy + 1, gx + 2, gy + gh - 1, (int) (a * 0.20F) << 24 | 0xFFFFFF);
        for (int i = 1; i < 4; i++) {
            int my = gy + gh * i / 4;
            g.fill(gx + gw + 1, my, gx + gw + 3, my + 1, (int) (a * 0.45F) << 24 | 0x9AB8FF);
        }

        // alma flotante: núcleo blanco que se apaga hacia el azul, con halo suave
        float sc = 1.0F + 0.30F * pulse;
        float bob = (float) Math.sin(time * 0.15F) * 1.2F;
        int cx = x + w / 2, cy = y + 14 + Math.round(bob);
        g.pose().pushPose();
        g.pose().translate(cx, cy, 0);
        g.pose().scale(sc, sc, 1);
        // halo suave hecho con anillos concéntricos (sin texturas, para que no queden bordes)
        for (int r = 11; r >= 6; r--) {
            float k = (11 - r + 1) / 6.0F;
            int ha = (int) (a * 0.07F * k * (0.7F + 0.5F * pulse));
            int rr = r;
            for (int dy = -rr; dy <= rr; dy++) {
                int hw = (int) Math.round(Math.sqrt(rr * rr - dy * dy));
                g.fill(-hw, dy, hw + 1, dy + 1, (ha << 24) | 0x4FB8FF);
            }
        }
        for (int dy = -5; dy <= 5; dy++) {
            int hw = (int) Math.round(Math.sqrt(30 - dy * dy));
            for (int dx = -hw; dx <= hw; dx++) {
                float d = (float) Math.sqrt(dx * dx + dy * dy) / 5.5F;
                int col = lerpRgb(0xFFFFFF, 0x2C9CF0, Mth.clamp(d * d, 0, 1));
                g.fill(dx, dy, dx + 1, dy + 1, a << 24 | col);
            }
        }
        // dos ojitos de alma
        g.fill(-2, -1, -1, 1, (int) (a * 0.75F) << 24 | 0x0B2A5A);
        g.fill(1, -1, 2, 1, (int) (a * 0.75F) << 24 | 0x0B2A5A);
        // colita ondulante
        for (int i = 0; i < 4; i++) {
            int tx = Math.round((float) Math.sin(time * 0.3F + i) * (1 + i * 0.4F));
            g.fill(tx - 1 + (i == 3 ? 0 : 0), 5 + i, tx + 1, 6 + i, (int) (a * (0.55F - i * 0.12F)) << 24 | 0x9CE8FF);
        }
        g.pose().popPose();

        // número
        String num = String.valueOf(souls);
        g.drawString(font, num, x + (w - font.width(num)) / 2, y + h - 17, (a << 24) | 0xE6F8FF, true);
        g.pose().pushPose();
        g.pose().translate(x + w / 2.0F, y + h - 8, 0);
        g.pose().scale(0.6F, 0.6F, 1);
        String mx = "/" + max;
        g.drawString(font, mx, -font.width(mx) / 2, 0, ((int) (a * 0.7F) << 24) | 0x8AA8D8, false);
        g.pose().popPose();
        RenderSystem.disableBlend();
    }

    /** Rectángulo con las cuatro esquinas recortadas un píxel (se ve redondeado). */
    private static void soft(GuiGraphics g, int x, int y, int w, int h, int argb) {
        g.fill(x + 1, y, x + w - 1, y + h, argb);
        g.fill(x, y + 1, x + 1, y + h - 1, argb);
        g.fill(x + w - 1, y + 1, x + w, y + h - 1, argb);
    }

    private static int lerpRgb(int c1, int c2, float t) {
        t = Mth.clamp(t, 0, 1);
        int r = (int) (((c1 >> 16) & 255) * (1 - t) + ((c2 >> 16) & 255) * t);
        int gg = (int) (((c1 >> 8) & 255) * (1 - t) + ((c2 >> 8) & 255) * t);
        int b = (int) ((c1 & 255) * (1 - t) + (c2 & 255) * t);
        return (r << 16) | (gg << 8) | b;
    }
}
