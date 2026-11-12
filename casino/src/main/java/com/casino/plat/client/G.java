package com.casino.plat.client;

import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
//#if MC >= 26.1
//$$ import net.minecraft.client.gui.GuiGraphicsExtractor;
//$$ import net.minecraft.client.renderer.RenderPipelines;
//#else
import net.minecraft.client.gui.GuiGraphics;
//#endif

import java.util.List;
import java.util.Optional;

/**
 * Lo que las pantallas del casino usan para dibujar, igual en todas las versiones (en 26.x Minecraft cambió GuiGraphics por
 * GuiGraphicsExtractor y la pila de transformaciones pasó a ser 2D).
 */
public final class G {
    //#if MC >= 26.1
    //$$ public final GuiGraphicsExtractor raw;
    //$$
    //$$ public G(GuiGraphicsExtractor raw) {
    //$$     this.raw = raw;
    //$$ }
    //#else
    public final GuiGraphics raw;

    public G(GuiGraphics raw) {
        this.raw = raw;
    }
    //#endif

    /** Si el color no trae opacidad, se pone opaco (en las versiones nuevas un alfa de 0 es invisible). */
    private static int argb(int c) {
        return (c & 0xFC000000) == 0 ? c | 0xFF000000 : c;
    }

    public int drawString(Font f, String s, int x, int y, int color) {
        return drawString(f, s, x, y, color, true);
    }

    public int drawString(Font f, String s, int x, int y, int color, boolean shadow) {
        //#if MC >= 26.1
        //$$ raw.text(f, s, x, y, argb(color), shadow);
        //$$ return x + f.width(s);
        //#else
        return raw.drawString(f, s, x, y, argb(color), shadow);
        //#endif
    }

    public int drawString(Font f, Component s, int x, int y, int color) {
        return drawString(f, s, x, y, color, true);
    }

    public int drawString(Font f, Component s, int x, int y, int color, boolean shadow) {
        //#if MC >= 26.1
        //$$ raw.text(f, s, x, y, argb(color), shadow);
        //$$ return x + f.width(s);
        //#else
        return raw.drawString(f, s, x, y, argb(color), shadow);
        //#endif
    }

    public int drawString(Font f, FormattedCharSequence s, int x, int y, int color) {
        return drawString(f, s, x, y, color, true);
    }

    public int drawString(Font f, FormattedCharSequence s, int x, int y, int color, boolean shadow) {
        //#if MC >= 26.1
        //$$ raw.text(f, s, x, y, argb(color), shadow);
        //$$ return x + f.width(s);
        //#else
        return raw.drawString(f, s, x, y, argb(color), shadow);
        //#endif
    }

    public void drawCenteredString(Font f, String s, int x, int y, int color) {
        drawString(f, s, x - f.width(s) / 2, y, color, true);
    }

    public void drawCenteredString(Font f, Component s, int x, int y, int color) {
        drawString(f, s, x - f.width(s) / 2, y, color, true);
    }

    public void fill(int x0, int y0, int x1, int y1, int color) {
        raw.fill(x0, y0, x1, y1, color);
    }

    public void fillGradient(int x0, int y0, int x1, int y1, int top, int bottom) {
        raw.fillGradient(x0, y0, x1, y1, top, bottom);
    }

    /** Un pedazo de textura: en pantalla (x, y, w, h); en la textura (u, v, uw, vh) de una imagen texW × texH. */
    public void blit(ResourceLocation tex, int x, int y, int w, int h, float u, float v, int uw, int vh, int texW, int texH) {
        //#if MC >= 26.1
        //$$ raw.blit(RenderPipelines.GUI_TEXTURED, tex, x, y, u, v, w, h, uw, vh, texW, texH);
        //#elif MC >= 1.21.2
        //$$ raw.blit(net.minecraft.client.renderer.RenderType::guiTextured, tex, x, y, u, v, w, h, uw, vh, texW, texH);
        //#else
        raw.blit(tex, x, y, w, h, u, v, uw, vh, texW, texH);
        //#endif
    }

    /** Igual que blit pero teñido con un color ARGB (para las sedas de los jinetes, el pelaje de los caballos, etc.). */
    public void blitTint(ResourceLocation tex, int x, int y, int w, int h, float u, float v, int uw, int vh, int texW, int texH, int argb) {
        //#if MC >= 26.1
        //$$ raw.blit(RenderPipelines.GUI_TEXTURED, tex, x, y, u, v, w, h, uw, vh, texW, texH, argb);
        //#elif MC >= 1.21.2
        //$$ raw.blit(net.minecraft.client.renderer.RenderType::guiTextured, tex, x, y, u, v, w, h, uw, vh, texW, texH, argb);
        //#else
        raw.setColor(((argb >> 16) & 255) / 255F, ((argb >> 8) & 255) / 255F, (argb & 255) / 255F, ((argb >>> 24) & 255) / 255F);
        raw.blit(tex, x, y, w, h, u, v, uw, vh, texW, texH);
        raw.setColor(1F, 1F, 1F, 1F);
        //#endif
    }

    public void renderItem(ItemStack st, int x, int y) {
        //#if MC >= 26.1
        //$$ raw.item(st, x, y);
        //#else
        raw.renderItem(st, x, y);
        //#endif
    }

    public void renderItemDecorations(Font f, ItemStack st, int x, int y) {
        //#if MC >= 26.1
        //$$ raw.itemDecorations(f, st, x, y);
        //#else
        raw.renderItemDecorations(f, st, x, y);
        //#endif
    }

    public void renderTooltip(Font f, List<Component> lines, int x, int y) {
        //#if MC >= 26.1
        //$$ raw.setTooltipForNextFrame(f, lines, Optional.empty(), x, y);
        //#else
        raw.renderTooltip(f, lines, Optional.empty(), x, y);
        //#endif
    }

    // ---- transformaciones (en 26.x son 2D: la z se ignora)

    public void push() {
        //#if MC >= 26.1
        //$$ raw.pose().pushMatrix();
        //#else
        raw.pose().pushPose();
        //#endif
    }

    public void pop() {
        //#if MC >= 26.1
        //$$ raw.pose().popMatrix();
        //#else
        raw.pose().popPose();
        //#endif
    }

    public void translate(double x, double y, double z) {
        //#if MC >= 26.1
        //$$ raw.pose().translate((float) x, (float) y);
        //#else
        raw.pose().translate(x, y, z);
        //#endif
    }

    public void scale(float x, float y, float z) {
        //#if MC >= 26.1
        //$$ raw.pose().scale(x, y);
        //#else
        raw.pose().scale(x, y, z);
        //#endif
    }
}
