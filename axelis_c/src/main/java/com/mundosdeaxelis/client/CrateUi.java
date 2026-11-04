package com.mundosdeaxelis.client;

import com.mundosdeaxelis.MundosDeAxelis;
import com.mundosdeaxelis.crate.Tier;
import net.minecraft.resources.ResourceLocation;

/** Piezas comunes de la interfaz de los crates (atlas de 128x64 con celdas de 32x32, todas en blanco para teñirlas). */
public final class CrateUi {
    private CrateUi() {
    }

    public static final ResourceLocation ATLAS = new ResourceLocation(MundosDeAxelis.MODID, "textures/gui/crate_ui.png");
    public static final ResourceLocation BURST = new ResourceLocation(MundosDeAxelis.MODID, "textures/effect/starburst.png");
    public static final ResourceLocation BEAM = new ResourceLocation(MundosDeAxelis.MODID, "textures/effect/light_beam.png");
    public static final ResourceLocation WHITE = new ResourceLocation(MundosDeAxelis.MODID, "textures/effect/white.png");
    public static final ResourceLocation HALO = new ResourceLocation(MundosDeAxelis.MODID, "textures/effect/halo.png");

    public static final int ATLAS_W = 128, ATLAS_H = 64;
    /** Esquina (u,v) de cada celda. */
    public static final int[] SLOT = {0, 0}, GLOW = {32, 0}, STAR = {64, 0}, POINTER = {96, 0},
            BAR = {0, 32}, PANEL = {32, 32}, GEM = {64, 32}, RING = {96, 32};

    public static float r(int rgb) {
        return ((rgb >> 16) & 255) / 255.0F;
    }

    public static float g(int rgb) {
        return ((rgb >> 8) & 255) / 255.0F;
    }

    public static float b(int rgb) {
        return (rgb & 255) / 255.0F;
    }

    public static int lerp(int a, int b, float t) {
        t = Math.max(0, Math.min(1, t));
        int rr = (int) (((a >> 16) & 255) * (1 - t) + ((b >> 16) & 255) * t);
        int gg = (int) (((a >> 8) & 255) * (1 - t) + ((b >> 8) & 255) * t);
        int bb = (int) ((a & 255) * (1 - t) + (b & 255) * t);
        return (rr << 16) | (gg << 8) | bb;
    }

    /** Cuánto "brilla" cada rareza (para el tamaño del halo y los destellos). */
    public static float intensity(Tier t) {
        return 0.35F + 0.13F * t.ordinal();
    }

    /** Marca de agua pequeñita (abajo a la derecha). {@code right}/{@code bottom} son el borde donde se alinea. */
    public static void watermark(net.minecraft.client.gui.GuiGraphics g, net.minecraft.client.gui.Font font, int right, int bottom, float alpha) {
        String t = "Axelis705";
        float sc = 0.5F;
        g.pose().pushPose();
        g.pose().translate(right - font.width(t) * sc, bottom - 8 * sc, 0);
        g.pose().scale(sc, sc, 1);
        g.drawString(font, t, 0, 0, ((int) (255 * alpha) << 24) | 0xE8E0FF, false);
        g.pose().popPose();
    }
}
