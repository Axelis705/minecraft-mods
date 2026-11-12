package com.autoscoleccion.client;

import com.autoscoleccion.AutosMod;
import com.autoscoleccion.game.CarMesh;
import com.autoscoleccion.game.CarStyle;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * Dibuja un cochecito en el mundo, en la mano o en una pantalla: la forma sale de {@link CarMesh} y aquí se pasa a vértices de
 * Minecraft (color por vértice sobre una textura blanca; lo que brilla usa luz máxima). La parte fija se calcula una sola vez por
 * auto y se guarda; solo lo que parpadea se rehace en cada cuadro.
 */
public final class CarModel {
    public static final ResourceLocation WHITE = new ResourceLocation(AutosMod.MODID, "textures/misc/white.png");
    public static final int FULL = 0xF000F0;

    /** Caras ya calculadas: 12 floats de posición, 3 de normal, 4 colores y si brilla, por cara. */
    private static final class Baked {
        final float[] pos;
        final float[] nrm;
        final int[] col;
        final boolean[] em;
        final int n;

        Baked(List<float[]> p, List<int[]> c) {
            n = p.size();
            pos = new float[n * 12];
            nrm = new float[n * 3];
            col = new int[n * 4];
            em = new boolean[n];
            for (int i = 0; i < n; i++) {
                float[] q = p.get(i);
                System.arraycopy(q, 0, pos, i * 12, 12);
                System.arraycopy(q, 12, nrm, i * 3, 3);
                int[] cc = c.get(i);
                System.arraycopy(cc, 0, col, i * 4, 4);
                em[i] = cc[4] != 0;
            }
        }
    }

    private static final Map<CarStyle, Baked[]> CACHE = new IdentityHashMap<>();
    /** Carrocería sin llantas y las dos llantas sueltas (izquierda, derecha), para manejar. */
    private static final Map<CarStyle, Baked[]> DRIVE = new IdentityHashMap<>();

    private CarModel() {
    }

    private static float[] normalOf(float[] a, float[] b, float[] c, float[] d) {
        float ux = b[0] - a[0], uy = b[1] - a[1], uz = b[2] - a[2];
        float vx = d[0] - a[0], vy = d[1] - a[1], vz = d[2] - a[2];
        float nx = uy * vz - uz * vy, ny = uz * vx - ux * vz, nz = ux * vy - uy * vx;
        if (nx * nx + ny * ny + nz * nz < 1e-10F) {
            ux = c[0] - b[0];
            uy = c[1] - b[1];
            uz = c[2] - b[2];
            vx = a[0] - b[0];
            vy = a[1] - b[1];
            vz = a[2] - b[2];
            nx = uy * vz - uz * vy;
            ny = uz * vx - ux * vz;
            nz = ux * vy - uy * vx;
        }
        float l = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
        if (l < 1e-6F) {
            return null;
        }
        return new float[] {nx / l, ny / l, nz / l};
    }

    private interface Builder {
        void build(CarMesh.Sink sink);
    }

    private static Baked bake(Builder b) {
        List<float[]> p = new ArrayList<>();
        List<int[]> c = new ArrayList<>();
        b.build((a, bb, cc, d, c0, c1, c2, c3, em) -> {
            float[] n = normalOf(a, bb, cc, d);
            if (n == null) {
                return;
            }
            p.add(new float[] {a[0], a[1], a[2], bb[0], bb[1], bb[2], cc[0], cc[1], cc[2], d[0], d[1], d[2], n[0], n[1], n[2]});
            c.add(new int[] {c0, c1, c2, c3, em ? 1 : 0});
        });
        return new Baked(p, c);
    }

    private static void draw(VertexConsumer vc, PoseStack ps, Baked b, int light) {
        Matrix4f pose = ps.last().pose();
        Matrix3f normal = ps.last().normal();
        for (int i = 0; i < b.n; i++) {
            int lt = b.em[i] ? FULL : light;
            int o = i * 12;
            float nx = b.nrm[i * 3], ny = b.nrm[i * 3 + 1], nz = b.nrm[i * 3 + 2];
            for (int k = 0; k < 4; k++) {
                vertex(vc, pose, normal, b.pos[o + k * 3], b.pos[o + k * 3 + 1], b.pos[o + k * 3 + 2], b.col[i * 4 + k], lt, nx, ny, nz, 255);
            }
        }
    }

    /**
     * Dibuja el auto para manejarlo: la carrocería fija y cada llanta girando ({@code spin} en radianes) y las de adelante dobladas
     * {@code steerDeg} grados.
     */
    public static void renderDriving(CarStyle s, PoseStack ps, MultiBufferSource buf, int light, float time, float spin, float steerDeg) {
        float[][] mounts = CarMesh.wheelMounts(s);
        Baked[] arr = DRIVE.computeIfAbsent(s, k -> {
            Baked[] b = new Baked[1 + mounts.length];
            b[0] = bake(sink -> CarMesh.buildBody(s, sink, 0));
            for (int i = 0; i < mounts.length; i++) {
                float[] m = mounts[i];
                b[1 + i] = bake(sink -> CarMesh.buildWheel(s, sink, 0, (int) m[3], m[1], m[5]));
            }
            return b;
        });
        VertexConsumer vc = buf.getBuffer(RenderType.entityCutoutNoCull(WHITE));
        draw(vc, ps, arr[0], light);
        for (int i = 0; i < mounts.length; i++) {
            float[] m = mounts[i];
            ps.pushPose();
            ps.translate(m[0], m[1], m[2]);
            if (m[4] > 0) {
                ps.mulPose(Axis.YP.rotationDegrees(-steerDeg));
            }
            ps.mulPose(Axis.ZP.rotation(-spin * s.wheelR / m[1]));
            draw(vc, ps, arr[1 + i], light);
            ps.popPose();
        }
        CarMesh.buildAnimated(s, (a, bb, c, d, c0, c1, c2, c3, em) -> {
            float[] n = normalOf(a, bb, c, d);
            if (n == null) {
                return;
            }
            int lt = em ? FULL : light;
            Matrix4f pose = ps.last().pose();
            Matrix3f normal = ps.last().normal();
            vertex(vc, pose, normal, a[0], a[1], a[2], c0, lt, n[0], n[1], n[2], 255);
            vertex(vc, pose, normal, bb[0], bb[1], bb[2], c1, lt, n[0], n[1], n[2], 255);
            vertex(vc, pose, normal, c[0], c[1], c[2], c2, lt, n[0], n[1], n[2], 255);
            vertex(vc, pose, normal, d[0], d[1], d[2], c3, lt, n[0], n[1], n[2], 255);
        }, time, 0);
    }

    private static Baked baked(CarStyle s, int mode) {
        Baked[] arr = CACHE.computeIfAbsent(s, k -> new Baked[2]);
        if (arr[mode] == null) {
            List<float[]> p = new ArrayList<>();
            List<int[]> c = new ArrayList<>();
            CarMesh.buildStatic(s, (a, b, cc, d, c0, c1, c2, c3, em) -> {
                float[] n = normalOf(a, b, cc, d);
                if (n == null) {
                    return;
                }
                p.add(new float[] {a[0], a[1], a[2], b[0], b[1], b[2], cc[0], cc[1], cc[2], d[0], d[1], d[2], n[0], n[1], n[2]});
                c.add(new int[] {c0, c1, c2, c3, em ? 1 : 0});
            }, mode);
            arr[mode] = new Baked(p, c);
        }
        return arr[mode];
    }

    private static void vertex(VertexConsumer vc, Matrix4f pose, Matrix3f normal, float x, float y, float z, int rgb, int light, float nx, float ny,
            float nz, int alpha) {
        vc.vertex(pose, x, y, z).color((rgb >> 16) & 255, (rgb >> 8) & 255, rgb & 255, alpha).uv(0.5F, 0.5F).overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(light).normal(normal, nx, ny, nz).endVertex();
    }

    /** Dibuja el auto en píxeles (1/16 de bloque): el que llama escala como quiera. */
    public static void render(CarStyle s, PoseStack ps, MultiBufferSource buf, int light, float time, int mode) {
        Matrix4f pose = ps.last().pose();
        Matrix3f normal = ps.last().normal();
        VertexConsumer vc = buf.getBuffer(RenderType.entityCutoutNoCull(WHITE));
        Baked b = baked(s, mode);
        for (int i = 0; i < b.n; i++) {
            int lt = b.em[i] ? FULL : light;
            int o = i * 12;
            float nx = b.nrm[i * 3], ny = b.nrm[i * 3 + 1], nz = b.nrm[i * 3 + 2];
            for (int k = 0; k < 4; k++) {
                vertex(vc, pose, normal, b.pos[o + k * 3], b.pos[o + k * 3 + 1], b.pos[o + k * 3 + 2], b.col[i * 4 + k], lt, nx, ny, nz, 255);
            }
        }
        CarMesh.buildAnimated(s, (a, bb, c, d, c0, c1, c2, c3, em) -> {
            float[] n = normalOf(a, bb, c, d);
            if (n == null) {
                return;
            }
            int lt = em ? FULL : light;
            vertex(vc, pose, normal, a[0], a[1], a[2], c0, lt, n[0], n[1], n[2], 255);
            vertex(vc, pose, normal, bb[0], bb[1], bb[2], c1, lt, n[0], n[1], n[2], 255);
            vertex(vc, pose, normal, c[0], c[1], c[2], c2, lt, n[0], n[1], n[2], 255);
            vertex(vc, pose, normal, d[0], d[1], d[2], c3, lt, n[0], n[1], n[2], 255);
        }, time, mode);
    }

    /** Una cara suelta (para la base giratoria, la caja, etc.), con luz y color dados. */
    public static void quad(VertexConsumer vc, PoseStack ps, float[] a, float[] b, float[] c, float[] d, int rgb, int alpha, int light) {
        float[] n = normalOf(a, b, c, d);
        if (n == null) {
            return;
        }
        Matrix4f pose = ps.last().pose();
        Matrix3f normal = ps.last().normal();
        vertex(vc, pose, normal, a[0], a[1], a[2], rgb, light, n[0], n[1], n[2], alpha);
        vertex(vc, pose, normal, b[0], b[1], b[2], rgb, light, n[0], n[1], n[2], alpha);
        vertex(vc, pose, normal, c[0], c[1], c[2], rgb, light, n[0], n[1], n[2], alpha);
        vertex(vc, pose, normal, d[0], d[1], d[2], rgb, light, n[0], n[1], n[2], alpha);
    }

    /** Disco plano horizontal (base giratoria) de radio r a la altura y. */
    public static void disc(VertexConsumer vc, PoseStack ps, float y, float r0, float r1, int segs, int rgb, int alpha, int light) {
        for (int i = 0; i < segs; i++) {
            double a0 = i * 2 * Math.PI / segs, a1 = (i + 1) * 2 * Math.PI / segs;
            float c0 = (float) Math.cos(a0), s0 = (float) Math.sin(a0), c1 = (float) Math.cos(a1), s1 = (float) Math.sin(a1);
            quad(vc, ps, new float[] {c0 * r0, y, s0 * r0}, new float[] {c1 * r0, y, s1 * r0}, new float[] {c1 * r1, y, s1 * r1},
                    new float[] {c0 * r1, y, s0 * r1}, rgb, alpha, light);
        }
    }

    /** Costado de un cilindro vertical (canto de la base). */
    public static void band(VertexConsumer vc, PoseStack ps, float y0, float y1, float r, int segs, int rgb, int alpha, int light) {
        for (int i = 0; i < segs; i++) {
            double a0 = i * 2 * Math.PI / segs, a1 = (i + 1) * 2 * Math.PI / segs;
            float c0 = (float) Math.cos(a0) * r, s0 = (float) Math.sin(a0) * r, c1 = (float) Math.cos(a1) * r, s1 = (float) Math.sin(a1) * r;
            quad(vc, ps, new float[] {c0, y0, s0}, new float[] {c0, y1, s0}, new float[] {c1, y1, s1}, new float[] {c1, y0, s1}, rgb, alpha, light);
        }
    }

    /**
     * Dibuja un auto en una pantalla. (x, y) es el centro en píxeles de pantalla, {@code size} cuántos píxeles de pantalla mide un píxel
     * del auto, yaw/pitch en grados.
     */
    public static void renderInGui(GuiGraphics g, CarStyle s, float x, float y, float z, float size, float yaw, float pitch, int mode, float time) {
        g.flush();
        PoseStack ps = g.pose();
        ps.pushPose();
        ps.translate(x, y, z);
        ps.scale(size, size, -size);
        ps.mulPose(Axis.ZP.rotationDegrees(180));
        ps.mulPose(Axis.XP.rotationDegrees(-pitch));
        ps.mulPose(Axis.YP.rotationDegrees(yaw));
        ps.translate(0, -3.0F, 0);
        Lighting.setupForEntityInInventory();
        MultiBufferSource.BufferSource buf = g.bufferSource();
        render(s, ps, buf, FULL, time, mode);
        buf.endBatch();
        Lighting.setupFor3DItems();
        ps.popPose();
    }
}
