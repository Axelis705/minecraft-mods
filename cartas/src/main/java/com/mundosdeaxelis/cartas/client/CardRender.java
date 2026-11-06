package com.mundosdeaxelis.cartas.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mundosdeaxelis.cartas.CardDef;
import com.mundosdeaxelis.cartas.Cartas;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/** Dibujo de una carta (cara, reverso, brillo holografico, halos). Coordenadas en pixeles de pantalla virtuales. */
final class CardRender {
    static final ResourceLocation BACK = new ResourceLocation(Cartas.MOD_ID, "textures/gui/cartas/reverso.png");
    static final int CW = 64;
    static final int CH = 96;
    /** tamano real de las texturas de las cartas (el doble, para que se vean nitidas) */
    static final int TW = 128;
    static final int TH = 192;

    private CardRender() {
    }

    static ResourceLocation art(String id) {
        return new ResourceLocation(Cartas.MOD_ID, "textures/gui/cartas/" + id + ".png");
    }

    static void face(GuiGraphics g, CardDef d, float x, float y, float w, float h) {
        tex(g, art(d.id), x, y, w, h, 1f, TW, TH);
    }

    /** la misma carta en alta calidad (256 x 384), para la vista grande */
    static void faceHd(GuiGraphics g, CardDef d, float x, float y, float w, float h) {
        tex(g, new ResourceLocation(Cartas.MOD_ID, "textures/gui/cartas/hd/" + d.id + ".png"), x, y, w, h, 1f, TW * 2, TH * 2);
    }

    static void back(GuiGraphics g, float x, float y, float w, float h, float alpha) {
        tex(g, BACK, x, y, w, h, alpha, TW, TH);
    }

    private static void tex(GuiGraphics g, ResourceLocation t, float x, float y, float w, float h, float alpha, int tw, int th) {
        g.pose().pushPose();
        g.pose().translate(x, y, 0f);
        g.pose().scale(w / CW, h / CH, 1f);
        RenderSystem.enableBlend();
        g.setColor(1f, 1f, 1f, alpha);
        g.blit(t, 0, 0, CW, CH, 0f, 0f, tw, th, tw, th);
        g.setColor(1f, 1f, 1f, 1f);
        g.pose().popPose();
    }

    /** Brillo holografico: franjas de arcoiris que se mueven, un destello diagonal y chispas. */
    static void holo(GuiGraphics g, float x, float y, float w, float h, float time, float seed) {
        g.pose().pushPose();
        g.pose().translate(x, y, 0f);
        g.pose().scale(w / CW, h / CH, 1f);
        float band = ((time * 0.45f + seed) % 1f) * (CH + 60) - 30;
        for (int k = 0; k < CH / 2; k++) {
            int yy = k * 2;
            float hue = (k * 0.025f + time * 0.22f + seed) % 1f;
            int rgb = Mth.hsvToRgb(hue, 0.55f, 1f);
            float near = Math.max(0f, 1f - Math.abs(yy - band) / 14f);
            int a = 0x2A + (int) (near * 0x60);
            g.fill(0, yy, CW, yy + 2, (a << 24) | rgb);
        }
        for (int i = 0; i < 5; i++) {
            float ph = time * 1.7f + i * 1.9f + seed * 9f;
            float tw = Math.max(0f, Mth.sin(ph));
            if (tw > 0.2f) {
                int sx = 6 + (int) (((i * 37 + seed * 100f) % 52));
                int sy = 8 + (int) (((i * 53 + seed * 200f) % 70));
                int a = (int) (tw * 230);
                int c = (a << 24) | 0xFFFFFF;
                g.fill(sx, sy - 2, sx + 1, sy + 3, c);
                g.fill(sx - 2, sy, sx + 3, sy + 1, c);
            }
        }
        g.pose().popPose();
    }

    /** Halo cuadrado suave alrededor de un rectangulo. */
    static void glow(GuiGraphics g, float x, float y, float w, float h, int rgb, float strength) {
        int a = Math.max(1, (int) (0x12 * strength));
        for (int i = 10; i >= 1; i -= 1) {
            g.fill((int) (x - i), (int) (y - i), (int) (x + w + i), (int) (y + h + i), (Math.min(255, a) << 24) | rgb);
        }
    }

    static void border(GuiGraphics g, int x, int y, int w, int h, int argb) {
        g.fill(x, y, x + w, y + 1, argb);
        g.fill(x, y + h - 1, x + w, y + h, argb);
        g.fill(x, y + 1, x + 1, y + h - 1, argb);
        g.fill(x + w - 1, y + 1, x + w, y + h - 1, argb);
    }
}
