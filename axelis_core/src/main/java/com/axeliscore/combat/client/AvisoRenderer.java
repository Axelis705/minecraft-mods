package com.axeliscore.combat.client;

import com.axeliscore.combat.Aviso;
import com.axeliscore.plat.client.CoreEntityRenderer;
import com.axeliscore.plat.client.Draw;
import com.axeliscore.plat.client.VB;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;

/**
 * Dibuja un {@link Aviso}: la figura entera clarita, lo que se va llenando más fuerte (de adentro hacia afuera; en las líneas, de la
 * punta de salida hacia el final), un borde brillante y un parpadeo al final. Siempre con luz completa (se ve de noche).
 */
public class AvisoRenderer extends CoreEntityRenderer<Aviso, AvisoRenderer.Snap> {
    public static final class Snap {
        int forma, color;
        float a, b, dir, p, edad;
    }

    private static final int LUZ = 0xF000F0;

    public AvisoRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @Override
    protected Snap snap(Aviso e, float pt) {
        Snap s = new Snap();
        s.forma = e.forma();
        s.color = e.colorArgb();
        s.a = e.a();
        s.b = e.b();
        s.dir = e.dir();
        s.edad = e.tickCount + pt;
        s.p = Math.min(1F, s.edad / Math.max(1, e.duracion()));
        return s;
    }

    @Override
    protected void draw(Snap s, PoseStack ps, Draw d, int light) {
        VB vb = d.translucent(Draw.BLANCO);
        PoseStack.Pose pose = ps.last();
        int r = (s.color >> 16) & 255, g = (s.color >> 8) & 255, bl = s.color & 255;
        // al final parpadea más rápido
        float blink = s.p > 0.75F ? 0.75F + 0.25F * (float) Math.sin(s.edad * (1.2F + 2.5F * (s.p - 0.75F) * 4)) : 1F;
        int aBase = (int) (60 * blink), aLleno = (int) (110 * blink), aBorde = (int) (215 * blink);
        float y = 0.03F;
        double dirR = Math.toRadians(s.dir);
        switch (s.forma) {
            case Aviso.ANILLO -> {
                float in = s.b, out = s.a;
                anillo(vb, pose, y, in, out, 0, Math.PI * 2, r, g, bl, aBase);
                anillo(vb, pose, y + 0.002F, in, in + (out - in) * s.p, 0, Math.PI * 2, r, g, bl, aLleno);
                anillo(vb, pose, y + 0.004F, out - 0.09F, out, 0, Math.PI * 2, r, g, bl, aBorde);
                anillo(vb, pose, y + 0.004F, in, in + 0.09F, 0, Math.PI * 2, r, g, bl, aBorde);
            }
            case Aviso.CONO -> {
                double half = Math.toRadians(s.b);
                double a0 = dirR - half, a1 = dirR + half;
                anillo(vb, pose, y, 0, s.a, a0, a1, r, g, bl, aBase);
                anillo(vb, pose, y + 0.002F, 0, s.a * s.p, a0, a1, r, g, bl, aLleno);
                anillo(vb, pose, y + 0.004F, s.a - 0.09F, s.a, a0, a1, r, g, bl, aBorde);
                rayo(vb, pose, y + 0.004F, a0, s.a, r, g, bl, aBorde);
                rayo(vb, pose, y + 0.004F, a1, s.a, r, g, bl, aBorde);
            }
            case Aviso.LINEA -> {
                float w = s.b / 2;
                rect(vb, pose, y, dirR, 0, s.a, -w, w, r, g, bl, aBase);
                rect(vb, pose, y + 0.002F, dirR, 0, s.a * s.p, -w, w, r, g, bl, aLleno);
                marco(vb, pose, y + 0.004F, dirR, 0, s.a, -w, w, r, g, bl, aBorde);
            }
            case Aviso.CUADRO -> {
                rect(vb, pose, y, dirR, -s.b, s.b, -s.a, s.a, r, g, bl, aBase);
                rect(vb, pose, y + 0.002F, dirR, -s.b * s.p, s.b * s.p, -s.a * s.p, s.a * s.p, r, g, bl, aLleno);
                marco(vb, pose, y + 0.004F, dirR, -s.b, s.b, -s.a, s.a, r, g, bl, aBorde);
            }
            default -> {
                anillo(vb, pose, y, 0, s.a, 0, Math.PI * 2, r, g, bl, aBase);
                anillo(vb, pose, y + 0.002F, 0, s.a * s.p, 0, Math.PI * 2, r, g, bl, aLleno);
                anillo(vb, pose, y + 0.004F, s.a - 0.09F, s.a, 0, Math.PI * 2, r, g, bl, aBorde);
                if (s.p > 0.02F) {
                    float e = s.a * s.p;
                    anillo(vb, pose, y + 0.004F, Math.max(0, e - 0.05F), e, 0, Math.PI * 2, r, g, bl, Math.min(255, aBorde));
                }
            }
        }
    }

    /** Punto del suelo a distancia {@code rad} con ángulo {@code ang} (0 = sur, como el giro de Minecraft). */
    private static float px(double ang, double rad) {
        return (float) (-Math.sin(ang) * rad);
    }

    private static float pz(double ang, double rad) {
        return (float) (Math.cos(ang) * rad);
    }

    private static void v(VB vb, PoseStack.Pose p, float x, float y, float z, int r, int g, int b, int a) {
        vb.v(p, x, y, z, r, g, b, a, 0.5F, 0.5F, OverlayTexture.NO_OVERLAY, LUZ, 0, 1, 0);
    }

    /** Parte de un anillo entre dos radios y dos ángulos (en pedacitos). */
    private static void anillo(VB vb, PoseStack.Pose p, float y, double r0, double r1, double a0, double a1, int r, int g, int b, int a) {
        if (r1 <= r0 + 0.001 || a <= 0) {
            return;
        }
        int n = (int) Math.max(6, Math.min(96, Math.abs(a1 - a0) / (Math.PI * 2) * (24 + r1 * 8)));
        for (int i = 0; i < n; i++) {
            double t0 = a0 + (a1 - a0) * i / n, t1 = a0 + (a1 - a0) * (i + 1) / n;
            v(vb, p, px(t0, r0), y, pz(t0, r0), r, g, b, a);
            v(vb, p, px(t0, r1), y, pz(t0, r1), r, g, b, a);
            v(vb, p, px(t1, r1), y, pz(t1, r1), r, g, b, a);
            v(vb, p, px(t1, r0), y, pz(t1, r0), r, g, b, a);
        }
    }

    /** Borde recto de un cono. */
    private static void rayo(VB vb, PoseStack.Pose p, float y, double ang, double len, int r, int g, int b, int a) {
        double w = 0.045;
        double sx = Math.cos(ang) * w, sz = Math.sin(ang) * w;
        float ex = px(ang, len), ez = pz(ang, len);
        v(vb, p, (float) -sx, y, (float) -sz, r, g, b, a);
        v(vb, p, (float) sx, y, (float) sz, r, g, b, a);
        v(vb, p, (float) (ex + sx), y, (float) (ez + sz), r, g, b, a);
        v(vb, p, (float) (ex - sx), y, (float) (ez - sz), r, g, b, a);
    }

    /** Rectángulo en las coordenadas de la figura: {@code f} hacia adelante (el giro) y {@code s} de lado. */
    private static void rect(VB vb, PoseStack.Pose p, float y, double dir, double f0, double f1, double s0, double s1, int r, int g, int b, int a) {
        if (f1 <= f0 + 0.001 || a <= 0) {
            return;
        }
        double fx = -Math.sin(dir), fz = Math.cos(dir), sx = Math.cos(dir), sz = Math.sin(dir);
        v(vb, p, (float) (fx * f0 + sx * s0), y, (float) (fz * f0 + sz * s0), r, g, b, a);
        v(vb, p, (float) (fx * f0 + sx * s1), y, (float) (fz * f0 + sz * s1), r, g, b, a);
        v(vb, p, (float) (fx * f1 + sx * s1), y, (float) (fz * f1 + sz * s1), r, g, b, a);
        v(vb, p, (float) (fx * f1 + sx * s0), y, (float) (fz * f1 + sz * s0), r, g, b, a);
    }

    private static void marco(VB vb, PoseStack.Pose p, float y, double dir, double f0, double f1, double s0, double s1, int r, int g, int b, int a) {
        double t = 0.09;
        rect(vb, p, y, dir, f0, f1, s0, s0 + t, r, g, b, a);
        rect(vb, p, y, dir, f0, f1, s1 - t, s1, r, g, b, a);
        rect(vb, p, y, dir, f0, f0 + t, s0, s1, r, g, b, a);
        rect(vb, p, y, dir, f1 - t, f1, s0, s1, r, g, b, a);
    }
}
