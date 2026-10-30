package com.casino.client;

import com.casino.Casino;
import com.casino.block.RouletteBE;
import com.casino.game.RouletteBets;
import com.casino.game.RouletteBets.Spot;
import com.casino.game.RouletteLayout;
import com.casino.game.RouletteTable;
import com.casino.game.RouletteTable.Phase;
import com.casino.game.RouletteTable.Slot;
import com.casino.game.RouletteWheel;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import org.joml.Matrix4f;

/**
 * Dibuja toda la mesa de ruleta: cuerpo de madera, rueda con 37 casillas que gira, pista y bola (animadas con la misma función que usa el
 * servidor para decidir el número), paño de apuestas con sus números, las fichas de cada jugador (con un aro de su color), la marca del
 * número ganador y un marcador con los últimos números y el tiempo para apostar.
 * Marco local: origen en el centro del bloque del medio, +z hacia los jugadores, +x a su derecha.
 */
public class RouletteRenderer implements BlockEntityRenderer<RouletteBE> {
    private static final ResourceLocation WHITE = new ResourceLocation(Casino.MODID, "textures/entity/plinko_white.png");
    private static final ResourceLocation FELT = new ResourceLocation(Casino.MODID, "textures/block/bj_felt.png");
    private static final ResourceLocation WOOD = new ResourceLocation(Casino.MODID, "textures/block/bj_wood.png");
    private static final ResourceLocation LEATHER = new ResourceLocation(Casino.MODID, "textures/block/bj_leather.png");
    private static final ResourceLocation GOLD = new ResourceLocation(Casino.MODID, "textures/block/gold.png");

    private static final float Y = (float) RouletteLayout.Y_TOP;
    private static final float Y_FELT = Y + 0.004F;
    private static final float Y_RAIL = Y + 0.05F;
    private static final float FX0 = -0.40F, FX1 = 3.40F, FZ0 = -1.30F, FZ1 = 1.30F;
    private static final int FULL = 15728880;
    private static final int RED = 0xB32222, BLACK = 0x17171A, GREEN = 0x1E8A45;
    private static final int CREAM = 0xE8DFC4;
    /** Colores de los jugadores (aro bajo sus fichas). */
    public static final int[] PLAYER_COLORS = {0xFF5555, 0x55AAFF, 0xFFD23C, 0x66DD66, 0xFF8FE0, 0xFF9A3C, 0x55EEDD, 0xBB88FF};
    private static final int[] CHIP_BODY = {0xBCBCC0, 0xAC242C, 0x208444, 0x1C1C21, 0x7830A4};

    private final Font font;

    public RouletteRenderer(BlockEntityRendererProvider.Context ctx) {
        this.font = ctx.getFont();
    }

    // ------------------------------------------------------------------ vértices y formas

    private static void v(VertexConsumer vc, PoseStack.Pose p, float x, float y, float z, float u, float w, int light, float nx, float ny, float nz, int rgb) {
        vc.vertex(p.pose(), x, y, z).color((rgb >> 16) & 255, (rgb >> 8) & 255, rgb & 255, 255).uv(u, w).overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(light).normal(p.normal(), nx, ny, nz).endVertex();
    }

    private static int scale(int rgb, float k) {
        int r = Math.min(255, (int) (((rgb >> 16) & 255) * k));
        int g = Math.min(255, (int) (((rgb >> 8) & 255) * k));
        int b = Math.min(255, (int) ((rgb & 255) * k));
        return (r << 16) | (g << 8) | b;
    }

    /** Rectángulo horizontal (cara hacia arriba). */
    private static void rectTop(VertexConsumer vc, PoseStack.Pose p, int light, float x0, float z0, float x1, float z1, float y, int rgb, float uvScale) {
        v(vc, p, x0, y, z0, x0 * uvScale, z0 * uvScale, light, 0, 1, 0, rgb);
        v(vc, p, x0, y, z1, x0 * uvScale, z1 * uvScale, light, 0, 1, 0, rgb);
        v(vc, p, x1, y, z1, x1 * uvScale, z1 * uvScale, light, 0, 1, 0, rgb);
        v(vc, p, x1, y, z0, x1 * uvScale, z0 * uvScale, light, 0, 1, 0, rgb);
    }

    /** Pared vertical entre dos puntos del plano, de y0 a y1, con la normal dada. */
    private static void wall(VertexConsumer vc, PoseStack.Pose p, int light, float x0, float z0, float x1, float z1, float y0, float y1, float nx, float nz, int rgb, float uvScale) {
        float len = (float) Math.hypot(x1 - x0, z1 - z0);
        v(vc, p, x0, y0, z0, 0, (1 - y0) * uvScale, light, nx, 0, nz, rgb);
        v(vc, p, x1, y0, z1, len * uvScale, (1 - y0) * uvScale, light, nx, 0, nz, rgb);
        v(vc, p, x1, y1, z1, len * uvScale, (1 - y1) * uvScale, light, nx, 0, nz, rgb);
        v(vc, p, x0, y1, z0, 0, (1 - y1) * uvScale, light, nx, 0, nz, rgb);
    }

    /** Las 4 paredes de un rectángulo (hacia afuera o hacia adentro). */
    private static void wallsRect(VertexConsumer vc, PoseStack.Pose p, int light, float x0, float z0, float x1, float z1, float y0, float y1, boolean outward, int rgb, float uvScale) {
        float k = outward ? 1 : -1;
        wall(vc, p, light, x0, z1, x1, z1, y0, y1, 0, k, rgb, uvScale);
        wall(vc, p, light, x1, z0, x0, z0, y0, y1, 0, -k, rgb, uvScale);
        wall(vc, p, light, x1, z1, x1, z0, y0, y1, k, 0, rgb, uvScale);
        wall(vc, p, light, x0, z0, x0, z1, y0, y1, -k, 0, rgb, uvScale);
    }

    /** Anillo inclinado (de un radio y altura al otro), centrado en el origen, entre dos ángulos. */
    private static void ringSlope(VertexConsumer vc, PoseStack.Pose p, int light, float r0, float y0, float r1, float y1, double a0, double a1, int n, int rgb) {
        float m = (y1 - y0) / (r1 - r0);
        for (int i = 0; i < n; i++) {
            double s0 = a0 + (a1 - a0) * i / n;
            double s1 = a0 + (a1 - a0) * (i + 1) / n;
            float c0 = (float) Math.cos(s0), n0 = (float) Math.sin(s0), c1 = (float) Math.cos(s1), n1 = (float) Math.sin(s1);
            float cm = (float) Math.cos((s0 + s1) / 2), nm = (float) Math.sin((s0 + s1) / 2);
            float inv = 1.0F / (float) Math.sqrt(1 + m * m);
            float nx = -m * cm * inv, ny = inv, nz = -m * nm * inv;
            v(vc, p, c0 * r0, y0, n0 * r0, c0 * r0 * 2, n0 * r0 * 2, light, nx, ny, nz, rgb);
            v(vc, p, c0 * r1, y1, n0 * r1, c0 * r1 * 2, n0 * r1 * 2, light, nx, ny, nz, rgb);
            v(vc, p, c1 * r1, y1, n1 * r1, c1 * r1 * 2, n1 * r1 * 2, light, nx, ny, nz, rgb);
            v(vc, p, c1 * r0, y0, n1 * r0, c1 * r0 * 2, n1 * r0 * 2, light, nx, ny, nz, rgb);
        }
    }

    /** Pared cilíndrica a radio r entre dos alturas. */
    private static void wallCyl(VertexConsumer vc, PoseStack.Pose p, int light, float r, float y0, float y1, int n, boolean outward, int rgb, float vScale) {
        for (int i = 0; i < n; i++) {
            double s0 = Math.PI * 2 * i / n;
            double s1 = Math.PI * 2 * (i + 1) / n;
            double sm = (s0 + s1) / 2;
            float k = outward ? 1 : -1;
            float nx = (float) Math.cos(sm) * k, nz = (float) Math.sin(sm) * k;
            float u0 = (float) (r * s0) * 2, u1 = (float) (r * s1) * 2;
            v(vc, p, (float) Math.cos(s0) * r, y0, (float) Math.sin(s0) * r, u0, (1 - y0) * vScale, light, nx, 0, nz, rgb);
            v(vc, p, (float) Math.cos(s1) * r, y0, (float) Math.sin(s1) * r, u1, (1 - y0) * vScale, light, nx, 0, nz, rgb);
            v(vc, p, (float) Math.cos(s1) * r, y1, (float) Math.sin(s1) * r, u1, (1 - y1) * vScale, light, nx, 0, nz, rgb);
            v(vc, p, (float) Math.cos(s0) * r, y1, (float) Math.sin(s0) * r, u0, (1 - y1) * vScale, light, nx, 0, nz, rgb);
        }
    }

    /** Segmento plano fino sobre una superficie (líneas del paño, fretes). */
    private static void line(VertexConsumer vc, PoseStack.Pose p, int light, float x0, float z0, float x1, float z1, float w, float y, int rgb) {
        float dx = x1 - x0, dz = z1 - z0;
        float len = (float) Math.sqrt(dx * dx + dz * dz);
        if (len < 1e-5F) {
            return;
        }
        float nx = -dz / len * w / 2, nz = dx / len * w / 2;
        v(vc, p, x0 - nx, y, z0 - nz, 0, 0, light, 0, 1, 0, rgb);
        v(vc, p, x0 + nx, y, z0 + nz, 0, 1, light, 0, 1, 0, rgb);
        v(vc, p, x1 + nx, y, z1 + nz, 1, 1, light, 0, 1, 0, rgb);
        v(vc, p, x1 - nx, y, z1 - nz, 1, 0, light, 0, 1, 0, rgb);
    }

    private static void frame(VertexConsumer vc, PoseStack.Pose p, int light, float x0, float z0, float x1, float z1, float w, float y, int rgb) {
        line(vc, p, light, x0 - w / 2, z0, x1 + w / 2, z0, w, y, rgb);
        line(vc, p, light, x0 - w / 2, z1, x1 + w / 2, z1, w, y, rgb);
        line(vc, p, light, x0, z0, x0, z1, w, y, rgb);
        line(vc, p, light, x1, z0, x1, z1, w, y, rgb);
    }

    private static void sphere(VertexConsumer vc, PoseStack.Pose p, int light, float cx, float cy, float cz, float r, int rgb) {
        int lat = 6, lon = 10;
        for (int i = 0; i < lat; i++) {
            double a0 = Math.PI * i / lat - Math.PI / 2, a1 = Math.PI * (i + 1) / lat - Math.PI / 2;
            for (int j = 0; j < lon; j++) {
                double b0 = Math.PI * 2 * j / lon, b1 = Math.PI * 2 * (j + 1) / lon;
                for (int k = 0; k < 4; k++) {
                    double a = k < 2 ? a0 : a1;
                    double b = (k == 0 || k == 3) ? b0 : b1;
                    float nx = (float) (Math.cos(a) * Math.cos(b)), ny = (float) Math.sin(a), nz = (float) (Math.cos(a) * Math.sin(b));
                    v(vc, p, cx + nx * r, cy + ny * r, cz + nz * r, 0.5F, 0.5F, light, nx, ny, nz, rgb);
                }
            }
        }
    }

    /** Pirámide de 4 lados (marca ganadora, deflectores). */
    private static void pyramid(VertexConsumer vc, PoseStack.Pose p, int light, float cx, float cz, float y, float half, float h, int rgb) {
        float[][] c = {{-half, -half}, {half, -half}, {half, half}, {-half, half}};
        for (int i = 0; i < 4; i++) {
            float[] a = c[i], b = c[(i + 1) % 4];
            float mx = (a[0] + b[0]) / 2, mz = (a[1] + b[1]) / 2;
            float len = (float) Math.sqrt(mx * mx + mz * mz + h * h);
            float nx = mx / len, nz = mz / len, ny = h / len;
            v(vc, p, cx + a[0], y, cz + a[1], 0, 0, light, nx, ny, nz, rgb);
            v(vc, p, cx + b[0], y, cz + b[1], 1, 0, light, nx, ny, nz, rgb);
            v(vc, p, cx, y + h, cz, 0.5F, 1, light, nx, ny, nz, scale(rgb, 1.15F));
            v(vc, p, cx, y + h, cz, 0.5F, 1, light, nx, ny, nz, scale(rgb, 1.15F));
        }
    }

    /** Ficha: cilindro bajo con borde a rayas. */
    private static void chip(VertexConsumer vc, PoseStack.Pose p, int light, float cx, float y, float cz, float r, float h, int body, int stripe) {
        int n = 12;
        for (int i = 0; i < n; i++) {
            double s0 = Math.PI * 2 * i / n, s1 = Math.PI * 2 * (i + 1) / n;
            float x0 = cx + (float) Math.cos(s0) * r, z0 = cz + (float) Math.sin(s0) * r;
            float x1 = cx + (float) Math.cos(s1) * r, z1 = cz + (float) Math.sin(s1) * r;
            float nx = (float) Math.cos((s0 + s1) / 2), nz = (float) Math.sin((s0 + s1) / 2);
            int side = (i % 2 == 0) ? stripe : body;
            v(vc, p, x0, y, z0, 0, 0, light, nx, 0, nz, side);
            v(vc, p, x1, y, z1, 1, 0, light, nx, 0, nz, side);
            v(vc, p, x1, y + h, z1, 1, 1, light, nx, 0, nz, side);
            v(vc, p, x0, y + h, z0, 0, 1, light, nx, 0, nz, side);
            // tapa: anillo exterior del color del cuerpo con una marca clara, interior apenas más oscuro
            int topC = (i % 2 == 0) ? scale(stripe, 0.95F) : body;
            v(vc, p, cx, y + h, cz, 0, 0, light, 0, 1, 0, scale(body, 0.85F));
            v(vc, p, x0, y + h, z0, 0, 0, light, 0, 1, 0, topC);
            v(vc, p, x1, y + h, z1, 0, 0, light, 0, 1, 0, topC);
            v(vc, p, cx, y + h, cz, 0, 0, light, 0, 1, 0, scale(body, 0.85F));
        }
    }

    // ------------------------------------------------------------------ texto

    private void flatText(PoseStack ps, MultiBufferSource buf, String text, double x, double y, double z, float yaw, int color, float s, int light) {
        ps.pushPose();
        ps.translate(x, y, z);
        ps.mulPose(Axis.YP.rotationDegrees(yaw));
        ps.mulPose(Axis.XP.rotationDegrees(-90));
        ps.scale(s, -s, s);
        float w = font.width(text);
        Matrix4f m = ps.last().pose();
        font.drawInBatch(text, -w / 2.0F, -font.lineHeight / 2.0F + 1, color, false, m, buf, Font.DisplayMode.NORMAL, 0, light);
        ps.popPose();
    }

    private static String tr(String key) {
        return Component.translatable(key).getString();
    }

    // ------------------------------------------------------------------ la mesa

    private void drawTable(PoseStack ps, MultiBufferSource buf, int light) {
        PoseStack.Pose p = ps.last();
        float L = (float) RouletteLayout.HALF_LEN, D = (float) RouletteLayout.HALF_DEP;
        VertexConsumer vc = buf.getBuffer(RenderType.entityCutoutNoCull(GOLD));
        wallsRect(vc, p, light, -L + 0.05F, -D + 0.05F, L - 0.05F, D - 0.05F, 0, 0.09F, true, 0xFFFFFF, 1);
        rectTop(vc, p, light, -L + 0.05F, -D + 0.05F, L - 0.05F, D - 0.05F, 0.09F, 0xFFFFFF, 1);
        wallsRect(vc, p, light, -L + 0.045F, -D + 0.045F, L - 0.045F, D - 0.045F, 0.46F, 0.52F, true, 0xFFFFFF, 1);
        vc = buf.getBuffer(RenderType.entityCutoutNoCull(WOOD));
        wallsRect(vc, p, light, -L + 0.05F, -D + 0.05F, L - 0.05F, D - 0.05F, 0.09F, Y - 0.17F, true, 0xFFFFFF, 1);
        wallsRect(vc, p, light, -L, -D, L, D, Y - 0.17F, Y, true, 0xFFFFFF, 1);
        rectTop(vc, p, light, -L, -D, L, D, Y, 0xE0B080, 1);
        // riel acolchado alrededor del paño
        float rx0 = -0.5F, rx1 = L;
        vc = buf.getBuffer(RenderType.entityCutoutNoCull(LEATHER));
        wallsRect(vc, p, light, rx0, -D, rx1, D, Y, Y_RAIL, true, 0xFFFFFF, 1);
        wallsRect(vc, p, light, FX0, FZ0, FX1, FZ1, Y_FELT, Y_RAIL, false, 0xC8C8C8, 1);
        rectTop(vc, p, light, rx0, -D, rx1, FZ0, Y_RAIL, 0xFFFFFF, 1);
        rectTop(vc, p, light, rx0, FZ1, rx1, D, Y_RAIL, 0xFFFFFF, 1);
        rectTop(vc, p, light, rx0, FZ0, FX0, FZ1, Y_RAIL, 0xFFFFFF, 1);
        rectTop(vc, p, light, FX1, FZ0, rx1, FZ1, Y_RAIL, 0xFFFFFF, 1);
        // filo dorado sobre el riel
        vc = buf.getBuffer(RenderType.entityCutoutNoCull(GOLD));
        wallsRect(vc, p, light, rx0 + 0.01F, -D + 0.01F, rx1 - 0.01F, D - 0.01F, Y_RAIL - 0.01F, Y_RAIL + 0.004F, true, 0xFFFFFF, 1);
        // fieltro
        vc = buf.getBuffer(RenderType.entityCutoutNoCull(FELT));
        rectTop(vc, p, light, FX0, FZ0, FX1, FZ1, Y_FELT, 0xFFFFFF, 2);
    }

    private static final float CELL_Y = Y_FELT + 0.002F;

    /** El dibujo del paño: casillas de colores, líneas y textos. */
    private void drawFelt(RouletteBE be, PoseStack ps, MultiBufferSource buf, int light, double time) {
        PoseStack.Pose p = ps.last();
        VertexConsumer vc = buf.getBuffer(RenderType.entityCutoutNoCull(WHITE));
        float in = 0.006F;
        // casillas de los números
        for (int n = 0; n <= 36; n++) {
            double[] c = RouletteBets.numberCenter(n);
            float x0, x1, z0, z1;
            if (n == 0) {
                x0 = (float) RouletteLayout.feltX(0);
                x1 = (float) RouletteLayout.feltX(1);
                z0 = (float) RouletteLayout.feltZ(0);
                z1 = (float) RouletteLayout.feltZ(3);
            } else {
                x0 = (float) RouletteLayout.feltX(c[0] - 0.5);
                x1 = (float) RouletteLayout.feltX(c[0] + 0.5);
                z0 = (float) RouletteLayout.feltZ(c[1] - 0.5);
                z1 = (float) RouletteLayout.feltZ(c[1] + 0.5);
            }
            int col = switch (RouletteBets.colorOf(n)) {
                case 0 -> GREEN;
                case 1 -> RED;
                default -> BLACK;
            };
            rectTop(vc, p, light, x0 + in, z0 + in, x1 - in, z1 - in, CELL_Y, col, 1);
        }
        // chances simples rojo/negro como rombo grande
        for (int k = 0; k < 6; k++) {
            if (k == RouletteBets.RED || k == RouletteBets.BLACK) {
                Spot s = RouletteBets.SPOTS.get(RouletteBets.EVEN_BASE + k);
                float cx = (float) RouletteLayout.feltX(s.gx), cz = (float) RouletteLayout.feltZ(s.gy);
                float hx = (float) (RouletteLayout.feltX(0.8) - RouletteLayout.feltX(0)) , hz = (float) ((RouletteLayout.feltZ(5) - RouletteLayout.feltZ(4)) * 0.42);
                int col = k == RouletteBets.RED ? RED : BLACK;
                v(vc, p, cx - hx, CELL_Y, cz, 0, 0, light, 0, 1, 0, col);
                v(vc, p, cx, CELL_Y, cz + hz, 0, 0, light, 0, 1, 0, col);
                v(vc, p, cx + hx, CELL_Y, cz, 0, 0, light, 0, 1, 0, col);
                v(vc, p, cx, CELL_Y, cz - hz, 0, 0, light, 0, 1, 0, col);
            }
        }
        // líneas del paño
        float ly = CELL_Y + 0.002F;
        float w = 0.008F;
        for (int g = 0; g <= RouletteBets.COLS; g++) {
            float x = (float) RouletteLayout.feltX(g);
            float zTop = (float) RouletteLayout.feltZ(0);
            float zBot = g >= 1 && g <= 13 ? (float) RouletteLayout.feltZ(5) : (float) RouletteLayout.feltZ(3);
            if (g == 0 || g == 14) {
                zBot = (float) RouletteLayout.feltZ(3);
            }
            line(vc, p, light, x, zTop, x, zBot, w, ly, CREAM);
        }
        for (int r = 0; r <= 5; r++) {
            float z = (float) RouletteLayout.feltZ(r);
            float xa = (float) RouletteLayout.feltX(r >= 4 ? 1 : 0);
            float xb = (float) RouletteLayout.feltX(r >= 4 ? 13 : 14);
            if (r == 3) {
                xb = (float) RouletteLayout.feltX(14);
            }
            line(vc, p, light, xa - w / 2, z, xb + w / 2, z, w, ly, CREAM);
        }
        // divisiones de columnas y docenas (3 docenas de 4 columnas)
        for (int k = 1; k < 3; k++) {
            float x = (float) RouletteLayout.feltX(1 + 4 * k);
            line(vc, p, light, x, (float) RouletteLayout.feltZ(3), x, (float) RouletteLayout.feltZ(4), w, ly, CREAM);
        }
        for (int k = 1; k < 6; k++) {
            float x = (float) RouletteLayout.feltX(1 + 2 * k);
            line(vc, p, light, x, (float) RouletteLayout.feltZ(4), x, (float) RouletteLayout.feltZ(5), w, ly, CREAM);
        }

        // textos
        float ty = CELL_Y + 0.004F;
        for (int n = 0; n <= 36; n++) {
            double[] c = RouletteBets.numberCenter(n);
            flatText(ps, buf, String.valueOf(n), RouletteLayout.feltX(c[0]), ty, RouletteLayout.feltZ(c[1]), 0, 0xFFFFFF, 0.016F, light);
        }
        String[] eTxt = {tr("casino.roulette.felt_low"), tr("casino.roulette.felt_even"), "", "", tr("casino.roulette.felt_odd"), tr("casino.roulette.felt_high")};
        for (int k = 0; k < 6; k++) {
            if (!eTxt[k].isEmpty()) {
                Spot s = RouletteBets.SPOTS.get(RouletteBets.EVEN_BASE + k);
                flatText(ps, buf, eTxt[k], RouletteLayout.feltX(s.gx), ty, RouletteLayout.feltZ(s.gy), 0, CREAM, 0.0125F, light);
            }
        }
        for (int k = 0; k < 3; k++) {
            Spot s = RouletteBets.SPOTS.get(RouletteBets.DOZEN_BASE + k);
            flatText(ps, buf, tr("casino.roulette.felt_dozen_" + (k + 1)), RouletteLayout.feltX(s.gx), ty, RouletteLayout.feltZ(s.gy), 0, CREAM, 0.0135F, light);
            Spot c = RouletteBets.SPOTS.get(RouletteBets.COLUMN_BASE + k);
            flatText(ps, buf, "2:1", RouletteLayout.feltX(c.gx), ty, RouletteLayout.feltZ(c.gy), 0, CREAM, 0.0125F, light);
        }
        // letrero del paño, en el borde de los jugadores
        flatText(ps, buf, tr("casino.roulette.felt_text"), 1.5, ty, 1.19, 0, 0xE8C14A, 0.011F, light);

        // marca de las casillas ganadoras
        if (be.table.phase == Phase.RESULT && be.table.winning >= 0) {
            float age = (float) (time - be.resultShownAt);
            if (age > 4) {
                float pulse = 0.6F + 0.4F * Mth.sin((float) (time % 1000.0) * 0.3F);
                int gold = scale(0xFFD84A, pulse);
                int win = be.table.winning;
                VertexConsumer vc2 = buf.getBuffer(RenderType.entityCutoutNoCull(WHITE));
                double[] c = RouletteBets.numberCenter(win);
                float x0, x1, z0, z1;
                if (win == 0) {
                    x0 = (float) RouletteLayout.feltX(0);
                    x1 = (float) RouletteLayout.feltX(1);
                    z0 = (float) RouletteLayout.feltZ(0);
                    z1 = (float) RouletteLayout.feltZ(3);
                } else {
                    x0 = (float) RouletteLayout.feltX(c[0] - 0.5);
                    x1 = (float) RouletteLayout.feltX(c[0] + 0.5);
                    z0 = (float) RouletteLayout.feltZ(c[1] - 0.5);
                    z1 = (float) RouletteLayout.feltZ(c[1] + 0.5);
                }
                frame(vc2, ps.last(), FULL, x0 + 0.01F, z0 + 0.01F, x1 - 0.01F, z1 - 0.01F, 0.016F, CELL_Y + 0.006F, gold);
                for (int id = RouletteBets.COLUMN_BASE; id < RouletteBets.SPOTS.size(); id++) {
                    Spot s = RouletteBets.SPOTS.get(id);
                    if (s.covers(win)) {
                        float[] b = outsideBox(s);
                        frame(vc2, ps.last(), FULL, b[0] + 0.01F, b[1] + 0.01F, b[2] - 0.01F, b[3] - 0.01F, 0.01F, CELL_Y + 0.006F, scale(0xFFD84A, pulse * 0.9F));
                    }
                }
            }
        }
    }

    /** Rectángulo (x0, z0, x1, z1) de una casilla de afuera (columna, docena o chance simple). */
    private static float[] outsideBox(Spot s) {
        double gx0, gx1, gy0, gy1;
        switch (s.kind) {
            case COLUMN -> { gx0 = 13; gx1 = 14; gy0 = 2 - s.index; gy1 = 3 - s.index; }
            case DOZEN -> { gx0 = 1 + 4 * s.index; gx1 = 5 + 4 * s.index; gy0 = 3; gy1 = 4; }
            default -> { gx0 = 1 + 2 * s.index; gx1 = 3 + 2 * s.index; gy0 = 4; gy1 = 5; }
        }
        return new float[]{(float) RouletteLayout.feltX(gx0), (float) RouletteLayout.feltZ(gy0), (float) RouletteLayout.feltX(gx1), (float) RouletteLayout.feltZ(gy1)};
    }

    // ------------------------------------------------------------------ la rueda

    private static int pocketColor(int number) {
        return switch (RouletteBets.colorOf(number)) {
            case 0 -> GREEN;
            case 1 -> RED;
            default -> BLACK;
        };
    }

    private void drawBowl(RouletteBE be, PoseStack ps, MultiBufferSource buf, int light, double time) {
        double wa = RouletteWheel.wheelAngle(time, be.spinStart, be.spinSeq) % (2 * Math.PI);
        ps.pushPose();
        ps.translate(RouletteLayout.WHEEL_X, 0, RouletteLayout.WHEEL_Z);
        // ---- la cuba (quieta): pared de madera, aro dorado, pista de la bola y deflectores
        PoseStack.Pose p = ps.last();
        float ro = (float) RouletteLayout.R_OUTER, rto = (float) RouletteLayout.R_TRACK_OUT, rti = (float) RouletteLayout.R_TRACK_IN, rw = (float) RouletteLayout.R_WHEEL;
        int seg = 56;
        VertexConsumer vc = buf.getBuffer(RenderType.entityCutoutNoCull(WOOD));
        wallCyl(vc, p, light, ro, Y, Y + 0.075F, seg, true, 0xFFFFFF, 1);
        ringSlope(vc, p, light, ro, Y + 0.075F, rto + 0.03F, Y + 0.075F, 0, Math.PI * 2, seg, 0xC89058);
        vc = buf.getBuffer(RenderType.entityCutoutNoCull(GOLD));
        ringSlope(vc, p, light, rto + 0.03F, Y + 0.077F, rto, Y + 0.077F, 0, Math.PI * 2, seg, 0xFFFFFF);
        wallCyl(vc, p, light, rto + 0.03F, Y + 0.075F, Y + 0.077F, seg, true, 0xFFFFFF, 1);
        vc = buf.getBuffer(RenderType.entityCutoutNoCull(WOOD));
        ringSlope(vc, p, light, rto, Y + 0.075F, rti, Y + 0.025F, 0, Math.PI * 2, seg, 0xA66E3C);
        vc = buf.getBuffer(RenderType.entityCutoutNoCull(GOLD));
        ringSlope(vc, p, light, rti, Y + 0.025F, rw, Y + 0.025F, 0, Math.PI * 2, seg, 0xFFFFFF);
        // deflectores de la pista
        vc = buf.getBuffer(RenderType.entityCutoutNoCull(GOLD));
        for (int k = 0; k < 8; k++) {
            double a = Math.PI * 2 * k / 8 + 0.4;
            float rr = 1.16F;
            ps.pushPose();
            ps.translate(Math.cos(a) * rr, 0, Math.sin(a) * rr);
            ps.mulPose(Axis.YP.rotationDegrees((float) (-Math.toDegrees(a) + 45)));
            pyramid(vc, ps.last(), light, 0, 0, Y + 0.052F, 0.028F, 0.04F, 0xFFFFFF);
            ps.popPose();
            vc = buf.getBuffer(RenderType.entityCutoutNoCull(GOLD));
        }

        // ---- la rueda (gira)
        ps.pushPose();
        ps.mulPose(Axis.YP.rotationDegrees((float) -Math.toDegrees(wa)));
        p = ps.last();
        float yRim = Y + 0.028F;
        vc = buf.getBuffer(RenderType.entityCutoutNoCull(GOLD));
        ringSlope(vc, p, light, rw, yRim, 0.96F, yRim, 0, Math.PI * 2, seg, 0xFFFFFF);
        vc = buf.getBuffer(RenderType.entityCutoutNoCull(WHITE));
        for (int i = 0; i < RouletteWheel.N; i++) {
            double ai = i * RouletteWheel.POCKET;
            double a0 = ai - RouletteWheel.POCKET / 2, a1 = ai + RouletteWheel.POCKET / 2;
            int col = pocketColor(RouletteBets.WHEEL[i]);
            ringSlope(vc, p, light, 0.96F, yRim - 0.002F, 0.84F, yRim - 0.013F, a0, a1, 1, col);
            ringSlope(vc, p, light, 0.84F, yRim - 0.015F, 0.64F, yRim - 0.024F, a0, a1, 1, scale(col, 0.7F));
        }
        // fretes entre casillas (doradas)
        vc = buf.getBuffer(RenderType.entityCutoutNoCull(GOLD));
        for (int i = 0; i < RouletteWheel.N; i++) {
            double a = (i + 0.5) * RouletteWheel.POCKET;
            float c = (float) Math.cos(a), s = (float) Math.sin(a);
            line(vc, p, light, c * 0.96F, s * 0.96F, c * 0.84F, s * 0.84F, 0.011F, yRim - 0.001F, 0xFFFFFF);
            line(vc, p, light, c * 0.84F, s * 0.84F, c * 0.64F, s * 0.64F, 0.011F, yRim - 0.013F, 0xFFFFFF);
        }
        // cono central
        vc = buf.getBuffer(RenderType.entityCutoutNoCull(WOOD));
        ringSlope(vc, p, light, 0.64F, yRim - 0.024F, 0.46F, yRim + 0.05F, 0, Math.PI * 2, 40, 0xC08A58);
        vc = buf.getBuffer(RenderType.entityCutoutNoCull(GOLD));
        ringSlope(vc, p, light, 0.46F, yRim + 0.05F, 0.20F, yRim + 0.10F, 0, Math.PI * 2, 40, 0xFFFFFF);
        ringSlope(vc, p, light, 0.20F, yRim + 0.10F, 0.07F, yRim + 0.115F, 0, Math.PI * 2, 24, 0xFFE9A0);
        wallCyl(vc, p, light, 0.07F, yRim + 0.115F, yRim + 0.23F, 16, true, 0xFFFFFF, 1);
        // cruz del centro con bolitas
        for (int k = 0; k < 4; k++) {
            double a = Math.PI / 2 * k + Math.PI / 4;
            float c = (float) Math.cos(a), s = (float) Math.sin(a);
            vc = buf.getBuffer(RenderType.entityCutoutNoCull(GOLD));
            line(vc, p, light, c * 0.07F, s * 0.07F, c * 0.20F, s * 0.20F, 0.018F, yRim + 0.205F, 0xFFFFFF);
            vc = buf.getBuffer(RenderType.entityCutoutNoCull(GOLD));
            sphere(vc, p, light, c * 0.21F, yRim + 0.205F, s * 0.21F, 0.022F, 0xFFFFFF);
        }
        vc = buf.getBuffer(RenderType.entityCutoutNoCull(GOLD));
        sphere(vc, p, light, 0, yRim + 0.245F, 0, 0.04F, 0xFFFFFF);
        // números (en la rueda, giran con ella)
        for (int i = 0; i < RouletteWheel.N; i++) {
            double ai = i * RouletteWheel.POCKET;
            double deg = Math.toDegrees(ai);
            flatText(ps, buf, String.valueOf(RouletteBets.WHEEL[i]), Math.cos(ai) * 0.905, yRim - 0.0035, Math.sin(ai) * 0.905, (float) (90.0 - deg), 0xFFFFFF, 0.0075F, light);
        }
        ps.popPose();

        // ---- la bola
        if (!(be.table.phase == Phase.SPINNING && time < be.spinStart)) {
            RouletteWheel.Ball b = RouletteWheel.ball(time, be.spinStart, be.spinSeq, be.spinNumber, be.spinSeed);
            float rad = Mth.lerp((float) b.radius, (float) RouletteLayout.R_POCKET, (float) RouletteLayout.R_TRACK_MID);
            float surf;
            if (rad >= 1.03F) {
                surf = Y + 0.025F + (rad - 1.03F) * 0.208F;
            } else if (rad >= 0.96F) {
                surf = Y + 0.028F;
            } else {
                surf = Math.max(Y + 0.004F, Y + 0.026F - (0.96F - rad) * 0.1F);
            }
            float bx = rad * (float) Math.cos(b.angle), bz = rad * (float) Math.sin(b.angle);
            vc = buf.getBuffer(RenderType.entityCutoutNoCull(WHITE));
            sphere(vc, ps.last(), light, bx, surf + 0.032F + (float) b.height, bz, 0.032F, 0xF6F1E4);
        }
        ps.popPose();
    }

    // ------------------------------------------------------------------ fichas

    private void drawChips(RouletteBE be, PoseStack ps, MultiBufferSource buf, int light, double time) {
        RouletteTable t = be.table;
        boolean result = t.phase == Phase.RESULT;
        float age = result ? (float) (time - be.resultShownAt) : 0;
        for (int sp = 0; sp < RouletteBets.SPOTS.size(); sp++) {
            int players = 0;
            for (int i = 0; i < RouletteTable.MAX_PLAYERS; i++) {
                if (t.slots[i].stake[sp] > 0) {
                    players++;
                }
            }
            if (players == 0) {
                continue;
            }
            Spot spot = RouletteBets.SPOTS.get(sp);
            float cx = (float) RouletteLayout.feltX(spot.gx), cz = (float) RouletteLayout.feltZ(spot.gy);
            boolean wins = result && t.winning >= 0 && spot.covers(t.winning);
            boolean partage = result && t.winning == 0 && spot.kind == RouletteBets.Kind.EVEN_MONEY;
            int k = 0;
            for (int i = 0; i < RouletteTable.MAX_PLAYERS; i++) {
                int amount = t.slots[i].stake[sp];
                if (amount <= 0) {
                    continue;
                }
                float ox = (k - (players - 1) / 2.0F) * 0.075F;
                k++;
                float x = cx + ox, z = cz;
                float lift = 0;
                // los perdedores se los lleva el crupier (hacia la rueda) unos segundos después del resultado
                if (result && !wins && !partage && age > 50) {
                    float f = Mth.clamp((age - 50) / 26.0F, 0, 1);
                    if (f >= 1) {
                        continue;
                    }
                    float e = f * f;
                    x = Mth.lerp(e, x, (float) RouletteLayout.WHEEL_X + 1.6F);
                    z = Mth.lerp(e, z, -1.55F);
                    lift = (float) Math.sin(Math.PI * f) * 0.08F;
                }
                // ficha que cae al apostar
                float drop = 0;
                float since = (float) (time - be.spotBorn[i][sp]);
                if (since >= 0 && since < 5) {
                    drop = (1 - since / 5.0F) * 0.18F;
                }
                drawStack(ps, buf, light, x, Y_FELT + 0.003F + lift + drop, z, amount, PLAYER_COLORS[i], wins && age > 4, time);
            }
        }
    }

    private void drawStack(PoseStack ps, MultiBufferSource buf, int light, float x, float y, float z, int amount, int playerColor, boolean glow, double time) {
        int[] values = {500, 100, 25, 5, 1};
        int left = amount;
        int shown = 0;
        float h = 0.0115F;
        VertexConsumer vc = buf.getBuffer(RenderType.entityCutoutNoCull(WHITE));
        PoseStack.Pose p = ps.last();
        // aro del color del jugador debajo de la pila
        chip(vc, p, glow ? FULL : light, x, y, z, 0.056F, 0.0025F, scale(playerColor, 0.9F), playerColor);
        float yy = y + 0.0025F;
        outer:
        for (int vi = 0; vi < values.length; vi++) {
            int n = left / values[vi];
            left -= n * values[vi];
            for (int j = 0; j < n; j++) {
                if (shown >= 14) {
                    break outer;
                }
                chip(vc, p, glow ? FULL : light, x + ((shown * 7) % 3 - 1) * 0.0018F, yy + shown * h, z, 0.046F, h, CHIP_BODY[4 - vi], 0xF2F2F5);
                shown++;
            }
        }
        float top = yy + shown * h;
        flatText(ps, buf, String.valueOf(amount), x, top + 0.004F, z + 0.003F, 0, 0xFFFFFF, 0.0066F, light);
    }

    // ------------------------------------------------------------------ marca del ganador y marcador

    private void drawDolly(RouletteBE be, PoseStack ps, MultiBufferSource buf, int light, double time) {
        if (be.table.phase != Phase.RESULT || be.table.winning < 0) {
            return;
        }
        float age = (float) (time - be.resultShownAt);
        if (age < 0) {
            return;
        }
        double[] c = RouletteBets.numberCenter(be.table.winning);
        float x = (float) RouletteLayout.feltX(c[0]);
        float z = (float) RouletteLayout.feltZ(c[1]);
        float f = Mth.clamp(age / 10.0F, 0, 1);
        float drop = (1 - f * f) * 0.35F;
        float bounce = f >= 1 ? (float) Math.abs(Math.sin((age - 10) * 0.25)) * 0.012F * Math.max(0, 1 - (age - 10) / 40.0F) : 0;
        VertexConsumer vc = buf.getBuffer(RenderType.entityCutoutNoCull(GOLD));
        float y = CELL_Y + 0.004F + drop + bounce;
        cone(vc, ps.last(), FULL, x, z, 0.025F, y, y + 0.03F);
        vc = buf.getBuffer(RenderType.entityCutoutNoCull(GOLD));
        sphere(vc, ps.last(), FULL, x, y + 0.055F, z, 0.025F, 0xFFFFFF);
    }

    private static void cone(VertexConsumer vc, PoseStack.Pose p, int light, float cx, float cz, float r, float y0, float y1) {
        int n = 10;
        for (int i = 0; i < n; i++) {
            double s0 = Math.PI * 2 * i / n, s1 = Math.PI * 2 * (i + 1) / n;
            float x0 = cx + (float) Math.cos(s0) * r, z0 = cz + (float) Math.sin(s0) * r;
            float x1 = cx + (float) Math.cos(s1) * r, z1 = cz + (float) Math.sin(s1) * r;
            float nx = (float) Math.cos((s0 + s1) / 2), nz = (float) Math.sin((s0 + s1) / 2);
            v(vc, p, x0, y0, z0, 0, 0, light, nx, 0, nz, 0xFFFFFF);
            v(vc, p, x1, y0, z1, 1, 0, light, nx, 0, nz, 0xFFFFFF);
            v(vc, p, x1 * 0.5F + cx * 0.5F, y1, z1 * 0.5F + cz * 0.5F, 1, 1, light, nx, 0.5F, nz, 0xFFFFFF);
            v(vc, p, x0 * 0.5F + cx * 0.5F, y1, z0 * 0.5F + cz * 0.5F, 0, 1, light, nx, 0.5F, nz, 0xFFFFFF);
        }
    }

    private void panelText(PoseStack ps, MultiBufferSource buf, String text, float x, float y, float s, int color, boolean center) {
        ps.pushPose();
        float w = font.width(text);
        ps.translate(x, y, 0.0095F);
        ps.scale(s, -s, s);
        Matrix4f m = ps.last().pose();
        font.drawInBatch(text, center ? -w / 2.0F : 0, 0, color, false, m, buf, Font.DisplayMode.NORMAL, 0, FULL);
        ps.popPose();
    }

    private static void panelQuad(VertexConsumer vc, PoseStack.Pose p, float x0, float y0, float x1, float y1, float z, int rgb) {
        v(vc, p, x0, y0, z, 0, 0, FULL, 0, 0, 1, rgb);
        v(vc, p, x1, y0, z, 1, 0, FULL, 0, 0, 1, rgb);
        v(vc, p, x1, y1, z, 1, 1, FULL, 0, 0, 1, rgb);
        v(vc, p, x0, y1, z, 0, 1, FULL, 0, 0, 1, rgb);
    }

    /** Marcador al fondo del paño: estado de la ronda, tiempo para apostar y los últimos números. */
    private void drawBoard(RouletteBE be, PoseStack ps, MultiBufferSource buf, double time) {
        RouletteTable t = be.table;
        float pw = 2.7F, ph = 0.30F;
        ps.pushPose();
        ps.translate(1.5, Y_RAIL - 0.005F, -1.22);
        ps.mulPose(Axis.XP.rotationDegrees(-22));
        PoseStack.Pose p = ps.last();
        VertexConsumer vc = buf.getBuffer(RenderType.entityCutoutNoCull(WHITE));
        panelQuad(vc, p, -pw / 2 - 0.02F, -0.01F, pw / 2 + 0.02F, ph + 0.02F, 0.0F, 0xC9A23A);
        panelQuad(vc, p, -pw / 2, 0.0F, pw / 2, ph, 0.003F, 0x0C0C10);
        float s = 0.0062F;
        // estado
        String status;
        int sc;
        if (t.phase == Phase.BETTING) {
            int secs = Math.max(0, (int) Math.ceil((be.phaseEnd - time) / 20.0));
            status = tr("casino.roulette.board_bets") + " " + secs;
            sc = 0x55FF77;
        } else if (t.phase == Phase.SPINNING) {
            status = tr("casino.roulette.board_nomore");
            sc = 0xFF5555;
        } else if (t.phase == Phase.RESULT && time >= be.spinStart + RouletteWheel.SPIN_TICKS) {
            status = tr("casino.roulette.board_result");
            sc = 0xFFD23C;
        } else if (t.phase == Phase.RESULT) {
            status = tr("casino.roulette.board_nomore");
            sc = 0xFF5555;
        } else {
            status = tr("casino.roulette.board_idle");
            sc = 0xA0A0A0;
        }
        panelText(ps, buf, status, -pw / 2 + 0.05F, ph - 0.03F, s * 1.4F, sc, false);
        panelText(ps, buf, tr("casino.roulette.board_limits"), -pw / 2 + 0.05F, ph - 0.115F, s * 0.85F, 0x9A9A9A, false);
        // jugadores y colores
        float nx = -pw / 2 + 0.05F;
        for (int i = 0; i < RouletteTable.MAX_PLAYERS; i++) {
            if (be.players[i] != null && !be.names[i].isEmpty() && t.slots[i].total > 0) {
                String nm = be.names[i].length() > 9 ? be.names[i].substring(0, 9) : be.names[i];
                vc = buf.getBuffer(RenderType.entityCutoutNoCull(WHITE));
                panelQuad(vc, ps.last(), nx, 0.025F, nx + 0.03F, 0.055F, 0.006F, PLAYER_COLORS[i]);
                panelText(ps, buf, nm, nx + 0.04F, 0.058F, s * 0.8F, 0xDDDDDD, false);
                nx += 0.05F + font.width(nm) * s * 0.8F + 0.05F;
            }
        }
        // últimos números: el más reciente grande, a la izquierda; los viejos van quedando a la derecha
        boolean hideFirst = t.phase == Phase.RESULT && time < be.spinStart + RouletteWheel.SPIN_TICKS;
        int from = hideFirst ? 1 : 0;
        int cells = Math.min(t.past.size() - from, 7);
        float cw = 0.17F;
        float bigW = 0.30F;
        float curX = pw / 2 - 0.08F; // borde derecho disponible
        for (int k = cells - 1; k >= 0; k--) {
            int n = t.past.get(from + k);
            boolean newest = k == 0;
            float w = newest ? bigW : cw;
            float x1 = curX, x0 = curX - w;
            vc = buf.getBuffer(RenderType.entityCutoutNoCull(WHITE));
            panelQuad(vc, ps.last(), x0, 0.03F, x1, ph - 0.03F, 0.006F, pocketColor(n));
            panelText(ps, buf, String.valueOf(n), (x0 + x1) / 2, ph / 2 + (newest ? 0.045F : 0.03F), newest ? s * 2.6F : s * 1.5F, 0xFFFFFF, true);
            curX = x0 - 0.025F;
        }
        ps.popPose();
    }

    // ------------------------------------------------------------------ principal

    @Override
    public void render(RouletteBE be, float partial, PoseStack ps, MultiBufferSource buf, int packedLight, int overlay) {
        Level level = be.getLevel();
        if (level == null) {
            return;
        }
        double time = level.getGameTime() + (double) partial;
        int l0 = LevelRenderer.getLightColor(level, be.getBlockPos().above());
        int light = LightTexture.pack(Math.max(LightTexture.block(l0), 9), Math.max(LightTexture.sky(l0), 9));
        ps.pushPose();
        ps.translate(0.5, 0, 0.5);
        ps.mulPose(Axis.YP.rotationDegrees(-be.facing().toYRot()));
        drawTable(ps, buf, light);
        drawFelt(be, ps, buf, light, time);
        drawBowl(be, ps, buf, light, time);
        drawChips(be, ps, buf, light, time);
        drawDolly(be, ps, buf, light, time);
        drawBoard(be, ps, buf, time);
        ps.popPose();
    }
}
