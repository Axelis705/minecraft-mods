package com.autoscoleccion.client;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import org.joml.Matrix4f;

/**
 * Dibujo 2D de las pantallas: cuadriláteros con degradado, arcos, círculos, brillos (mezcla aditiva) y texto grande. Se usa entre
 * {@link #begin} y {@link #end}.
 */
public final class Gfx {
    private static BufferBuilder bb;
    private static Matrix4f m;
    private static boolean add;

    private Gfx() {
    }

    public static void begin(GuiGraphics g, boolean additive) {
        g.flush();
        RenderSystem.enableBlend();
        add = additive;
        if (additive) {
            RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        } else {
            RenderSystem.defaultBlendFunc();
        }
        RenderSystem.disableCull();
        RenderSystem.disableDepthTest();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        m = g.pose().last().pose();
        bb = Tesselator.getInstance().getBuilder();
        bb.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
    }

    public static void end() {
        BufferUploader.drawWithShader(bb.end());
        if (add) {
            RenderSystem.defaultBlendFunc();
        }
        RenderSystem.enableCull();
        RenderSystem.enableDepthTest();
    }

    private static void v(float x, float y, int argb) {
        bb.vertex(m, x, y, 0).color((argb >> 16) & 255, (argb >> 8) & 255, argb & 255, (argb >>> 24)).endVertex();
    }

    public static void quad(float x1, float y1, int c1, float x2, float y2, int c2, float x3, float y3, int c3, float x4, float y4, int c4) {
        v(x1, y1, c1);
        v(x2, y2, c2);
        v(x3, y3, c3);
        v(x4, y4, c4);
    }

    public static void rect(float x0, float y0, float x1, float y1, int argb) {
        quad(x0, y0, argb, x0, y1, argb, x1, y1, argb, x1, y0, argb);
    }

    public static void vgrad(float x0, float y0, float x1, float y1, int top, int bottom) {
        quad(x0, y0, top, x0, y1, bottom, x1, y1, bottom, x1, y0, top);
    }

    public static void hgrad(float x0, float y0, float x1, float y1, int left, int right) {
        quad(x0, y0, left, x0, y1, left, x1, y1, right, x1, y0, right);
    }

    /** Brillo suave alrededor de un rectángulo (del color al transparente en {@code spread} píxeles). */
    public static void glow(float x0, float y0, float x1, float y1, int argb, float spread) {
        int t = argb & 0xFFFFFF;
        quad(x0 - spread, y0 - spread, t, x0, y0, argb, x1, y0, argb, x1 + spread, y0 - spread, t);
        quad(x1, y0, argb, x1, y1, argb, x1 + spread, y1 + spread, t, x1 + spread, y0 - spread, t);
        quad(x0, y1, argb, x0 - spread, y1 + spread, t, x1 + spread, y1 + spread, t, x1, y1, argb);
        quad(x0 - spread, y0 - spread, t, x0 - spread, y1 + spread, t, x0, y1, argb, x0, y0, argb);
    }

    /** Arco (ángulos en radianes, 0 = derecha, positivo hacia arriba). */
    public static void arc(float cx, float cy, float r0, float r1, double a0, double a1, int inner, int outer, int segs) {
        for (int i = 0; i < segs; i++) {
            double b0 = a0 + (a1 - a0) * i / segs, b1 = a0 + (a1 - a0) * (i + 1) / segs;
            float c0 = (float) Math.cos(b0), s0 = (float) -Math.sin(b0), c1 = (float) Math.cos(b1), s1 = (float) -Math.sin(b1);
            quad(cx + c0 * r0, cy + s0 * r0, inner, cx + c0 * r1, cy + s0 * r1, outer, cx + c1 * r1, cy + s1 * r1, outer, cx + c1 * r0, cy + s1 * r0, inner);
        }
    }

    public static void circle(float cx, float cy, float r, int argb) {
        arc(cx, cy, 0, r, 0, Math.PI * 2, argb, argb, Math.max(12, (int) (r * 1.2F)));
    }

    /** Círculo que brilla: centro del color y borde transparente. */
    public static void blob(float cx, float cy, float r, int argb) {
        arc(cx, cy, 0, r, 0, Math.PI * 2, argb, argb & 0xFFFFFF, Math.max(16, (int) (r * 1.2F)));
    }

    public static void line(float x0, float y0, float x1, float y1, float w, int argb) {
        float dx = x1 - x0, dy = y1 - y0;
        float l = (float) Math.sqrt(dx * dx + dy * dy);
        if (l < 1e-4F) {
            return;
        }
        float nx = -dy / l * w / 2, ny = dx / l * w / 2;
        quad(x0 + nx, y0 + ny, argb, x0 - nx, y0 - ny, argb, x1 - nx, y1 - ny, argb, x1 + nx, y1 + ny, argb);
    }

    /** Rectángulo con esquinas redondeadas (aproximadas). */
    public static void round(float x0, float y0, float x1, float y1, float r, int argb) {
        r = Math.min(r, Math.min((x1 - x0) / 2, (y1 - y0) / 2));
        rect(x0 + r, y0, x1 - r, y1, argb);
        rect(x0, y0 + r, x0 + r, y1 - r, argb);
        rect(x1 - r, y0 + r, x1, y1 - r, argb);
        arc(x0 + r, y0 + r, 0, r, Math.PI / 2, Math.PI, argb, argb, 4);
        arc(x1 - r, y0 + r, 0, r, 0, Math.PI / 2, argb, argb, 4);
        arc(x0 + r, y1 - r, 0, r, Math.PI, Math.PI * 1.5, argb, argb, 4);
        arc(x1 - r, y1 - r, 0, r, Math.PI * 1.5, Math.PI * 2, argb, argb, 4);
    }

    public static int argb(int rgb, float alpha) {
        return ((int) (Math.max(0, Math.min(1, alpha)) * 255) << 24) | (rgb & 0xFFFFFF);
    }

    public static int mix(int a, int b, float t) {
        t = Math.max(0, Math.min(1, t));
        int r = Math.round(((a >> 16) & 255) * (1 - t) + ((b >> 16) & 255) * t);
        int gg = Math.round(((a >> 8) & 255) * (1 - t) + ((b >> 8) & 255) * t);
        int bl = Math.round((a & 255) * (1 - t) + (b & 255) * t);
        return (r << 16) | (gg << 8) | bl;
    }

    /** Texto centrado y escalado (con sombra). */
    public static void text(GuiGraphics g, Font f, String s, float cx, float y, float scale, int argb) {
        g.pose().pushPose();
        g.pose().translate(cx, y, 0);
        g.pose().scale(scale, scale, 1);
        g.drawString(f, s, -f.width(s) / 2, 0, argb, true);
        g.pose().popPose();
    }

    /** Texto alineado a la izquierda y escalado. */
    public static void textL(GuiGraphics g, Font f, String s, float x, float y, float scale, int argb) {
        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        g.pose().scale(scale, scale, 1);
        g.drawString(f, s, 0, 0, argb, true);
        g.pose().popPose();
    }

    public static float ease(float t) {
        t = Math.max(0, Math.min(1, t));
        return 1 - (1 - t) * (1 - t) * (1 - t);
    }

    /** Rebote al final (para cosas que aparecen). */
    public static float back(float t) {
        t = Math.max(0, Math.min(1, t));
        float c1 = 1.70158F, c3 = c1 + 1;
        return 1 + c3 * (float) Math.pow(t - 1, 3) + c1 * (float) Math.pow(t - 1, 2);
    }
}
