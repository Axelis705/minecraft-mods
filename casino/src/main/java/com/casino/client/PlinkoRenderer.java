package com.casino.client;

import com.casino.plat.client.CasinoBER;
import com.casino.plat.client.Draw;
import com.casino.plat.client.VB;
import com.casino.plat.Ids;
import com.casino.Casino;
import com.casino.block.PlinkoBE;
import com.casino.game.PlinkoLayout;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import org.joml.Matrix4f;

/**
 * Dibuja la mesa de Plinko: tablero con clavijas, casilleros con su multiplicador, marco con luces, cartel y las bolas cayendo
 * (con estela). Las posiciones salen de {@link PlinkoLayout} y del reloj del juego, igual para todos los jugadores.
 */
public class PlinkoRenderer extends CasinoBER<PlinkoBE> {
    private static final ResourceLocation BOARD = tex("plinko_board");
    private static final ResourceLocation HEADER = tex("plinko_header");
    private static final ResourceLocation WHITE = tex("plinko_white");
    private static final ResourceLocation BALL = tex("plinko_ball");
    private static final ResourceLocation GOLD = tex("wheel_gold");
    private static final ResourceLocation GLOW = tex("glow");

    private static final int FULL = 0xF000F0;
    private static final float HALF_W = 1.50F;
    private static final float POST_W = 0.12F;
    private static final float BOARD_BOTTOM = 0.45F;
    private static final float HEADER_BOTTOM = 3.42F;
    private static final float TOP = 4.0F;
    private static final float BUCKET_TOP = 1.12F;

    private final Font font;

    public PlinkoRenderer(BlockEntityRendererProvider.Context ctx) {
        this.font = Draw.font();
    }

    private static ResourceLocation tex(String n) {
        return Ids.rl(Casino.MODID, "textures/entity/" + n + ".png");
    }

    private static void v(VB vc, PoseStack.Pose p, float x, float y, float z, float u, float w, int light, float nx, float ny, float nz,
                          int r, int g, int b, int a) {
        vc.v(p, x, y, z, r, g, b, a, u, w, OverlayTexture.NO_OVERLAY, light, nx, ny, nz);
    }

    /** Rectángulo de cara al frente; la textura entra entera, con el arriba arriba. */
    private static void quad(VB vc, PoseStack.Pose p, float x0, float y0, float x1, float y1, float z, int light, int rgb, int a) {
        int r = (rgb >> 16) & 255;
        int g = (rgb >> 8) & 255;
        int b = rgb & 255;
        v(vc, p, x0, y0, z, 0, 1, light, 0, 0, 1, r, g, b, a);
        v(vc, p, x1, y0, z, 1, 1, light, 0, 0, 1, r, g, b, a);
        v(vc, p, x1, y1, z, 1, 0, light, 0, 0, 1, r, g, b, a);
        v(vc, p, x0, y1, z, 0, 0, light, 0, 0, 1, r, g, b, a);
    }

    /** Pared lateral (mira hacia los lados) entre z0 y z1. */
    private static void sideQuad(VB vc, PoseStack.Pose p, float x, float y0, float y1, float z0, float z1, float nx, int light) {
        v(vc, p, x, y0, z0, 0, 1, light, nx, 0, 0, 255, 255, 255, 255);
        v(vc, p, x, y1, z0, 0, 0, light, nx, 0, 0, 255, 255, 255, 255);
        v(vc, p, x, y1, z1, 1, 0, light, nx, 0, 0, 255, 255, 255, 255);
        v(vc, p, x, y0, z1, 1, 1, light, nx, 0, 0, 255, 255, 255, 255);
    }

    private static void disc(VB vc, PoseStack.Pose p, float cx, float cy, float z, float r, int seg, int light, int rgb) {
        int cr = (rgb >> 16) & 255;
        int cg = (rgb >> 8) & 255;
        int cb = rgb & 255;
        for (int i = 0; i < seg; i++) {
            double a0 = Math.PI * 2 * i / seg;
            double a1 = Math.PI * 2 * (i + 1) / seg;
            v(vc, p, cx, cy, z, 0.5F, 0.5F, light, 0, 0, 1, cr, cg, cb, 255);
            v(vc, p, cx + r * (float) Math.cos(a0), cy + r * (float) Math.sin(a0), z, 0.5F, 0.5F, light, 0, 0, 1, cr, cg, cb, 255);
            v(vc, p, cx + r * (float) Math.cos(a1), cy + r * (float) Math.sin(a1), z, 0.5F, 0.5F, light, 0, 0, 1, cr, cg, cb, 255);
            v(vc, p, cx, cy, z, 0.5F, 0.5F, light, 0, 0, 1, cr, cg, cb, 255);
        }
    }

    private static int scale(int rgb, float k) {
        int r = Math.min(255, (int) (((rgb >> 16) & 255) * k));
        int g = Math.min(255, (int) (((rgb >> 8) & 255) * k));
        int b = Math.min(255, (int) ((rgb & 255) * k));
        return (r << 16) | (g << 8) | b;
    }

    private static float bucketX(int bucket) {
        return (float) ((bucket - PlinkoLayout.ROWS / 2.0) * PlinkoLayout.DX);
    }

    @Override
    protected void draw(PlinkoBE be, float partial, PoseStack ps, Draw d, int packedLight, int overlay) {
        Level level = be.getLevel();
        if (level == null) {
            return;
        }
        long gt = level.getGameTime();
        float time = gt + partial;
        int l0 = LevelRenderer.getLightColor(level, be.getBlockPos().above(2));
        int lit = LightTexture.pack(Math.max(LightTexture.block(l0), 13), Math.max(LightTexture.sky(l0), 9));
        int risk = be.shownRisk;

        ps.pushPose();
        ps.translate(0.5, 0, 0.5);
        ps.mulPose(Axis.YP.rotationDegrees(-be.facing().toYRot()));
        PoseStack.Pose pose = ps.last();

        // OJO: cada pasada pide su "pincel" justo antes de usarlo y no lo guarda. Al pedir otro tipo de dibujo, Minecraft termina el anterior
        // y comparte el mismo almacén; si se reutilizara un pincel viejo, los dibujos saldrían con la textura equivocada.
        // ---- fondo del tablero
        VB vc = d.cutout(BOARD);
        quad(vc, pose, -HALF_W, BOARD_BOTTOM, HALF_W, HEADER_BOTTOM + 0.02F, 0.0F, lit, 0xFFFFFF, 255);

        // ---- casilleros: fondo de color según lo que pagan
        vc = d.cutout(WHITE);
        for (int b = 0; b < PlinkoLayout.BUCKETS; b++) {
            int col = PlinkoLayout.colorFor(PlinkoLayout.mult10(risk, b));
            float cx = bucketX(b);
            float hw = (float) PlinkoLayout.DX / 2 - 0.012F;
            quad(vc, pose, cx - hw, BOARD_BOTTOM + 0.04F, cx + hw, BUCKET_TOP, 0.006F, FULL, scale(col, 0.42F), 255);
            quad(vc, pose, cx - hw, BOARD_BOTTOM + 0.04F, cx + hw, BOARD_BOTTOM + 0.10F, 0.009F, FULL, col, 255);
        }
        // ---- clavijas
        for (int k = 0; k < PlinkoLayout.ROWS; k++) {
            float y = (float) (PlinkoLayout.PEG_Y0 - k * PlinkoLayout.DY);
            for (int j = 0; j <= k; j++) {
                float x = (float) ((j - k / 2.0) * PlinkoLayout.DX);
                disc(vc, pose, x, y, 0.025F, 0.034F, 10, FULL, 0xFFE9A0);
                disc(vc, pose, x, y, 0.03F, 0.02F, 10, FULL, 0xFFFFFF);
            }
        }

        // ---- divisiones doradas, postes y base
        vc = d.cutout(GOLD);
        for (int i = 0; i <= PlinkoLayout.BUCKETS; i++) {
            float x = (float) ((i - PlinkoLayout.BUCKETS / 2.0) * PlinkoLayout.DX);
            quad(vc, pose, x - 0.011F, BOARD_BOTTOM, x + 0.011F, BUCKET_TOP + 0.02F, 0.02F, lit, 0xFFFFFF, 255);
        }
        for (int side = -1; side <= 1; side += 2) {
            float x0 = side < 0 ? -HALF_W - POST_W : HALF_W;
            float x1 = x0 + POST_W;
            quad(vc, pose, x0, BOARD_BOTTOM - 0.1F, x1, TOP, 0.07F, lit, 0xFFFFFF, 255);
            sideQuad(vc, pose, side < 0 ? x1 : x0, BOARD_BOTTOM - 0.1F, TOP, 0.0F, 0.07F, side < 0 ? 1 : -1, lit);
        }
        quad(vc, pose, -HALF_W - POST_W, BOARD_BOTTOM - 0.1F, HALF_W + POST_W, BOARD_BOTTOM, 0.07F, lit, 0xFFFFFF, 255);

        // ---- cartel
        vc = d.cutout(HEADER);
        quad(vc, pose, -HALF_W - POST_W, HEADER_BOTTOM, HALF_W + POST_W, TOP, 0.075F, FULL, 0xFFFFFF, 255);

        // ---- luces que corren por los postes (brillan solas)
        boolean busy = !be.balls.isEmpty();
        vc = d.glow(GLOW);
        for (int side = -1; side <= 1; side += 2) {
            float bx = side * (HALF_W + POST_W / 2);
            for (int i = 0; i < 15; i++) {
                float by = BOARD_BOTTOM + 0.12F + i * 0.2F;
                float ph = ((i - time * (busy ? 0.55F : 0.25F) * side) % 6 + 6) % 6;
                float bright = ph < 1.4F ? 1.0F : 0.22F;
                int rgb = busy ? 0xFF60D0 : 0xFFC030;
                quad(vc, pose, bx - 0.09F, by - 0.09F, bx + 0.09F, by + 0.09F, 0.085F, FULL, scale(rgb, bright), 255);
            }
        }

        // ---- brillo de las bolas (estela + halo) y de los casilleros que acaban de ganar
        vc = d.glow(GLOW);
        for (PlinkoBE.Ball ball : be.balls) {
            double t = time - ball.start;
            if (t < 0) {
                continue;
            }
            int col = PlinkoLayout.ballColor(ball.risk);
            for (int i = 4; i >= 1; i--) {
                double[] tp = PlinkoLayout.position(ball.path, Math.max(0, t - i * 0.9));
                float k = 0.55F - i * 0.1F;
                float rs = 0.085F - i * 0.012F;
                quad(vc, pose, (float) tp[0] - rs, (float) tp[1] - rs, (float) tp[0] + rs, (float) tp[1] + rs, 0.05F, FULL, scale(col, k), 255);
            }
            double[] p = PlinkoLayout.position(ball.path, t);
            quad(vc, pose, (float) p[0] - 0.19F, (float) p[1] - 0.19F, (float) p[0] + 0.19F, (float) p[1] + 0.19F, 0.052F, FULL, scale(col, 0.62F), 255);
        }
        for (PlinkoBE.Landing l : be.landings) {
            float age = (float) (gt - l.time) + partial;
            if (age < 0 || age > PlinkoBE.LANDING_SHOW) {
                continue;
            }
            int col = PlinkoLayout.colorFor(l.m10);
            float cx = bucketX(l.bucket);
            float pulse = (1 - age / PlinkoBE.LANDING_SHOW) * (0.65F + 0.35F * (float) Math.sin(age * 0.7F));
            float hw = (float) PlinkoLayout.DX / 2 + 0.05F;
            quad(vc, pose, cx - hw, BOARD_BOTTOM, cx + hw, BUCKET_TOP + 0.12F, 0.045F, FULL, scale(col, Math.max(0, pulse)), 255);
        }

        // ---- cuerpo de las bolas (las que caen y las que descansan en su casillero)
        vc = d.cutout(BALL);
        for (PlinkoBE.Ball ball : be.balls) {
            double t = time - ball.start;
            if (t < 0) {
                continue;
            }
            double[] p = PlinkoLayout.position(ball.path, t);
            quad(vc, pose, (float) p[0] - 0.066F, (float) p[1] - 0.066F, (float) p[0] + 0.066F, (float) p[1] + 0.066F, 0.06F, FULL, PlinkoLayout.ballColor(ball.risk), 255);
        }
        for (PlinkoBE.Landing l : be.landings) {
            float age = (float) (gt - l.time) + partial;
            if (age >= 0 && age < 28) {
                float cx = bucketX(l.bucket);
                float cy = 0.78F - age * 0.003F;
                quad(vc, pose, cx - 0.066F, cy - 0.066F, cx + 0.066F, cy + 0.066F, 0.06F, FULL, PlinkoLayout.ballColor(l.risk), 255);
            }
        }

        // ---- multiplicadores en los casilleros
        Matrix4f mat;
        for (int b = 0; b < PlinkoLayout.BUCKETS; b++) {
            int m10 = PlinkoLayout.mult10(risk, b);
            ps.pushPose();
            ps.translate(bucketX(b), 0.69F, 0.034F);
            ps.scale(0.0118F, -0.0118F, 0.0118F);
            mat = ps.last().pose();
            Component c = Component.literal(PlinkoLayout.format(m10));
            d.text(ps, c, -font.width(c) / 2.0F, -4, 0xFF000000 | PlinkoLayout.colorFor(m10), 0, FULL);
            ps.popPose();
        }

        // ---- cartel del multiplicador que sube desde el casillero
        for (PlinkoBE.Landing l : be.landings) {
            float age = (float) (gt - l.time) + partial;
            if (age < 0 || age >= 60) {
                continue;
            }
            int col = PlinkoLayout.colorFor(l.m10);
            float cx = bucketX(l.bucket);
            float rise = Math.min(1, age / 12.0F);
            float pop = 0.9F + 0.35F * (float) Math.sin(Math.min(1, age / 8.0F) * Math.PI);
            int alpha = (int) (255 * Math.min(1, (60 - age) / 14.0F));
            if (alpha > 8) {
                ps.pushPose();
                ps.translate(cx, 1.28F + 0.55F * rise, 0.09F);
                float s = (l.m10 >= 100 ? 0.026F : 0.02F) * pop;
                ps.scale(s, -s, s);
                mat = ps.last().pose();
                Component c = Component.literal("x" + PlinkoLayout.format(l.m10));
                d.text(ps, c, -font.width(c) / 2.0F, -4, (alpha << 24) | col, 0, FULL);
                ps.popPose();
            }
        }
        ps.popPose();
    }


    @Override
    public int getViewDistance() {
        return 80;
    }
}
