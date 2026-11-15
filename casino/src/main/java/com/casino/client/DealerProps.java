package com.casino.client;

import com.casino.Casino;
import com.casino.plat.Ids;
import com.casino.plat.client.Draw;
import com.casino.plat.client.VB;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.joml.Quaternionf;

/**
 * Lo que los crupieres tienen en la mano mientras trabajan: carta, bola de ruleta, botella, vaso (vacío o servido), trapo,
 * coctelera y fichas. Se arman con cajitas de colores pegadas a la palma; siempre quedan derechos (como si la muñeca los acomodara),
 * salvo la botella al servir, que se inclina.
 */
public final class DealerProps {
    public static final int NONE = 0;
    public static final int CARD = 1;
    public static final int BALL = 2;
    public static final int BOTTLE = 3;
    public static final int GLASS_EMPTY = 4;
    public static final int GLASS_FULL = 5;
    public static final int RAG = 6;
    public static final int SHAKER = 7;
    public static final int CHIPS = 8;
    public static final int BATON = 9;
    public static final int TASER = 10;
    public static final int PISTOL = 11;

    private static final ResourceLocation WHITE = Ids.rl(Casino.MODID, "textures/entity/bubble.png");

    private DealerProps() {
    }

    /** Dibuja lo que hay en una mano. arm ya trae su giro de este cuadro; tilt (0..1) inclina la botella para servir. */
    public static void draw(Draw d, PoseStack ps, ModelPart arm, boolean right, int prop, float tilt, int light) {
        if (prop == NONE) {
            return;
        }
        ps.pushPose();
        arm.translateAndRotate(ps);
        // a la palma (el brazo baja por +y; la mano termina en 10/16)
        ps.translate((right ? -1.0 : 1.0) / 16.0, 9.5 / 16.0, -1.5 / 16.0);
        // derecho otra vez: se deshace el giro del brazo
        ps.mulPose(new Quaternionf().rotationZYX(arm.zRot, arm.yRot, arm.xRot).conjugate());
        if (tilt > 0) {
            ps.mulPose(new Quaternionf().rotationZ((right ? 1 : -1) * tilt * 1.9F));
        }
        B b = new B(d.cutout(WHITE), ps.last(), light);
        switch (prop) {
            case CARD -> {
                b.box(-0.055, 0.02, -0.003, 0.055, -0.14, 0.003, 0xF4F1EA);
                b.box(-0.045, 0.01, -0.005, 0.045, -0.13, -0.003, 0xB2232C);
                b.box(-0.045, 0.01, 0.003, 0.045, -0.13, 0.005, 0xF4F1EA);
            }
            case BALL -> b.box(-0.032, 0.0, -0.06, 0.032, -0.064, 0.004, 0xF6F1E4);
            case BOTTLE -> {
                b.box(-0.045, 0.09, -0.045, 0.045, -0.17, 0.045, 0x1F4A2A);
                b.box(-0.047, 0.0, -0.047, 0.047, -0.09, 0.047, 0xEFE3C2);
                b.box(-0.032, -0.17, -0.032, 0.032, -0.2, 0.032, 0x1F4A2A);
                b.box(-0.018, -0.2, -0.018, 0.018, -0.3, 0.018, 0x24552F);
                b.box(-0.021, -0.3, -0.021, 0.021, -0.325, 0.021, 0xD9A93A);
                if (tilt > 0.55F) {
                    // el chorro: sale de la boca y cae derecho al vaso (se deshace la inclinación para que baje vertical)
                    ps.pushPose();
                    ps.translate(0, -0.325, 0);
                    ps.mulPose(new Quaternionf().rotationZ(-(right ? 1 : -1) * tilt * 1.9F));
                    B s = new B(d.cutout(WHITE), ps.last(), light);
                    float len = 0.19F * Math.min(1F, (tilt - 0.55F) / 0.3F);
                    s.box(-0.011, 0.0, -0.011, 0.011, len, 0.011, 0xC8102E);
                    s.box(-0.006, 0.0, -0.016, 0.006, len * 0.9, -0.011, 0xE0457A);
                    ps.popPose();
                }
            }
            case GLASS_EMPTY, GLASS_FULL -> {
                // piezas separadas por un pelito: si dos caras quedan en el mismo plano, parpadean
                b.box(-0.04, 0.07, -0.04, 0.04, 0.061, 0.04, 0xD8E8EE);
                b.box(-0.008, 0.06, -0.008, 0.008, -0.029, 0.008, 0xD8E8EE);
                b.box(-0.025, -0.031, -0.025, 0.025, -0.059, 0.025, 0xCFE4EC);
                b.box(-0.05, -0.061, -0.05, 0.05, -0.1, 0.05, 0xCFE4EC);
                if (prop == GLASS_FULL) {
                    b.box(-0.044, -0.065, -0.044, 0.044, -0.112, 0.044, 0xE0457A);
                    b.box(-0.012, -0.102, 0.015, 0.012, -0.126, 0.039, 0xC8102E);
                    b.box(-0.002, -0.126, 0.025, 0.002, -0.15, 0.029, 0x2E6B2A);
                }
            }
            case RAG -> {
                b.box(-0.05, 0.0, -0.05, 0.05, 0.12, 0.05, 0xF2F2F2);
                b.box(-0.052, 0.04, -0.052, 0.052, 0.05, 0.052, 0x3A5CA8);
            }
            case SHAKER -> {
                b.box(-0.04, 0.06, -0.04, 0.04, -0.16, 0.04, 0xC9CFD6);
                b.box(-0.042, -0.04, -0.042, 0.042, -0.06, 0.042, 0x9AA2AC);
                b.box(-0.03, -0.16, -0.03, 0.03, -0.22, 0.03, 0xD9DEE4);
                b.box(-0.015, -0.22, -0.015, 0.015, -0.25, 0.015, 0xC9CFD6);
            }
            case CHIPS -> {
                int[] c = {0xAC242C, 0x208444, 0x1C1C21, 0xAC242C};
                for (int i = 0; i < c.length; i++) {
                    double y0 = 0.02 - i * 0.018;
                    b.box(-0.05, y0, -0.05, 0.05, y0 - 0.016, 0.05, c[i]);
                    b.box(-0.052, y0 - 0.006, -0.02, 0.052, y0 - 0.01, 0.02, 0xF0F0F0);
                }
            }
            case BATON -> {
                b.box(-0.025, 0.06, -0.025, 0.025, -0.5, 0.025, 0x16161A);
                b.box(-0.032, 0.06, -0.032, 0.032, -0.06, 0.032, 0x2E2E34);
            }
            case TASER -> {
                b.box(-0.035, -0.03, -0.22, 0.035, 0.05, 0.02, 0xE8C21E);
                b.box(-0.036, -0.032, -0.23, 0.036, 0.0, -0.19, 0x18181C);
                b.box(-0.03, 0.0, -0.02, 0.03, 0.13, 0.04, 0x18181C);
            }
            case PISTOL -> {
                b.box(-0.03, -0.04, -0.26, 0.03, 0.03, 0.03, 0x26282C);
                b.box(-0.02, -0.03, -0.27, 0.02, 0.0, -0.26, 0x101012);
                b.box(-0.028, 0.0, -0.02, 0.028, 0.14, 0.05, 0x1A1B1E);
            }
            default -> {
            }
        }
        ps.popPose();
    }

    // ------------------------------------------------------------------ sombreros del Viejo Oeste

    public static final int HAT_NONE = 0, HAT_COWBOY = 1, HAT_SHERIFF = 2, HAT_VISOR = 3, HAT_BOWLER = 4, HAT_STRAW = 5;
    /** Sombrero de cada vaquero del saloon (variante WEST + n): tipo y color. */
    private static final int[][] WEST_PATRON_HATS = {
            {HAT_COWBOY, 0x6B4423}, {HAT_COWBOY, 0xC8A878}, {HAT_COWBOY, 0x24201C}, {HAT_COWBOY, 0x7A7468},
            {HAT_COWBOY, 0x9E2A2A}, {HAT_BOWLER, 0x1E1C1A}, {HAT_NONE, 0}, {HAT_STRAW, 0xD8C080}};

    /** Qué sombrero lleva este personaje: {tipo, color} ({0, 0} = ninguno). */
    public static int[] hat(int game, int variant) {
        if (variant < com.casino.entity.DealerEntity.WEST) {
            return new int[]{HAT_NONE, 0};
        }
        return switch (game) {
            case com.casino.entity.DealerEntity.GAME_GUARD -> new int[]{HAT_SHERIFF, 0x3A2A1E};
            case com.casino.entity.DealerEntity.GAME_BARTENDER -> new int[]{HAT_NONE, 0};
            case com.casino.entity.DealerEntity.GAME_WHEEL -> new int[]{HAT_BOWLER, 0x5A1414};
            case com.casino.entity.DealerEntity.GAME_PATRON -> WEST_PATRON_HATS[Math.floorMod(variant - com.casino.entity.DealerEntity.WEST,
                    WEST_PATRON_HATS.length)];
            default -> new int[]{HAT_VISOR, 0x2E8B57};
        };
    }

    private static int shade(int c, float k) {
        int r = Math.min(255, (int) (((c >> 16) & 255) * k)), g = Math.min(255, (int) (((c >> 8) & 255) * k)), b = Math.min(255, (int) ((c & 255) * k));
        return r << 16 | g << 8 | b;
    }

    /** Dibuja el sombrero sobre la cabeza (head ya trae su giro de este cuadro). En el modelo, "arriba" es -y y el frente es -z. */
    public static void drawHat(Draw d, PoseStack ps, ModelPart head, int[] hat, int light) {
        if (hat[0] == HAT_NONE) {
            return;
        }
        ps.pushPose();
        head.translateAndRotate(ps);
        B b = new B(d.cutout(WHITE), ps.last(), light);
        int c = hat[1], band = shade(c, 0.55F), light2 = shade(c, 1.18F);
        switch (hat[0]) {
            case HAT_COWBOY, HAT_SHERIFF, HAT_STRAW -> {
                boolean sheriff = hat[0] == HAT_SHERIFF;
                double bw = sheriff ? 0.47 : 0.44, bd = 0.41, y = -0.44;
                // ala ancha con las orillas de los lados levantadas
                b.box(-bw + 0.07, y, -bd, bw - 0.07, y - 0.025, bd, c);
                b.box(-bw, y - 0.02, -bd + 0.05, -bw + 0.07, y - 0.075, bd - 0.05, light2);
                b.box(bw - 0.07, y - 0.02, -bd + 0.05, bw, y - 0.075, bd - 0.05, light2);
                // copa con el pellizco arriba
                b.box(-0.27, y - 0.02, -0.27, 0.27, y - 0.2, 0.27, c);
                b.box(-0.27, y - 0.2, -0.27, -0.06, y - 0.27, 0.27, c);
                b.box(0.06, y - 0.2, -0.27, 0.27, y - 0.27, 0.27, c);
                b.box(-0.06, y - 0.2, -0.2, 0.06, y - 0.23, 0.2, shade(c, 0.85F));
                // cinta
                b.box(-0.278, y - 0.025, -0.278, 0.278, y - 0.075, 0.278, hat[0] == HAT_STRAW ? 0x7A3A1E : sheriff ? 0xB89A60 : band);
                if (sheriff) {
                    b.box(-0.05, y - 0.03, -0.285, 0.05, y - 0.07, -0.279, 0xE8C34A);   // estrellita en la cinta
                }
            }
            case HAT_BOWLER -> {
                double y = -0.45;
                b.box(-0.33, y, -0.33, 0.33, y - 0.025, 0.33, c);
                b.box(-0.255, y - 0.02, -0.255, 0.255, y - 0.17, 0.255, c);
                b.box(-0.2, y - 0.17, -0.2, 0.2, y - 0.22, 0.2, c);
                b.box(-0.262, y - 0.025, -0.262, 0.262, y - 0.06, 0.262, band);
            }
            case HAT_VISOR -> {
                // visera verde de crupier: banda alrededor y la visera al frente
                double y = -0.36;
                b.box(-0.262, y, -0.262, 0.262, y - 0.045, 0.262, 0x1E1E1E);
                b.box(-0.26, y - 0.01, -0.46, 0.26, y - 0.035, -0.262, c);
            }
            default -> {
            }
        }
        ps.popPose();
    }

    /** Cajitas de color con su luz (las caras miran hacia afuera). */
    private static final class B {
        private final VB vb;
        private final PoseStack.Pose p;
        private final int light;

        B(VB vb, PoseStack.Pose p, int light) {
            this.vb = vb;
            this.p = p;
            this.light = light;
        }

        void box(double ax, double ay, double az, double bx, double by, double bz, int rgb) {
            float x0 = (float) Math.min(ax, bx), x1 = (float) Math.max(ax, bx);
            float y0 = (float) Math.min(ay, by), y1 = (float) Math.max(ay, by);
            float z0 = (float) Math.min(az, bz), z1 = (float) Math.max(az, bz);
            int r = (rgb >> 16) & 255, g = (rgb >> 8) & 255, bl = rgb & 255;
            // modelo con y hacia abajo: "arriba" es -y
            quad(x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1, 0, -1, 0, r, g, bl, 1.0F);
            quad(x0, y1, z1, x1, y1, z1, x1, y1, z0, x0, y1, z0, 0, 1, 0, r, g, bl, 0.55F);
            quad(x0, y1, z0, x1, y1, z0, x1, y0, z0, x0, y0, z0, 0, 0, -1, r, g, bl, 0.85F);
            quad(x1, y1, z1, x0, y1, z1, x0, y0, z1, x1, y0, z1, 0, 0, 1, r, g, bl, 0.85F);
            quad(x0, y1, z1, x0, y1, z0, x0, y0, z0, x0, y0, z1, -1, 0, 0, r, g, bl, 0.7F);
            quad(x1, y1, z0, x1, y1, z1, x1, y0, z1, x1, y0, z0, 1, 0, 0, r, g, bl, 0.7F);
        }

        private void quad(float ax, float ay, float az, float bx, float by, float bz, float cx, float cy, float cz, float dx, float dy, float dz,
                float nx, float ny, float nz, int r, int g, int b, float k) {
            int rr = (int) (r * k), gg = (int) (g * k), bb = (int) (b * k);
            vb.v(p, ax, ay, az, rr, gg, bb, 255, 0.4F, 0.4F, OverlayTexture.NO_OVERLAY, light, nx, ny, nz);
            vb.v(p, bx, by, bz, rr, gg, bb, 255, 0.6F, 0.4F, OverlayTexture.NO_OVERLAY, light, nx, ny, nz);
            vb.v(p, cx, cy, cz, rr, gg, bb, 255, 0.6F, 0.6F, OverlayTexture.NO_OVERLAY, light, nx, ny, nz);
            vb.v(p, dx, dy, dz, rr, gg, bb, 255, 0.4F, 0.6F, OverlayTexture.NO_OVERLAY, light, nx, ny, nz);
            //#if MC >= 26.1
            //$$ // y del otro lado (en 26.x el pincel oculta las caras de atrás; antes se dibujan las dos y repetirla parpadea)
            //$$ vb.v(p, dx, dy, dz, rr, gg, bb, 255, 0.4F, 0.6F, OverlayTexture.NO_OVERLAY, light, -nx, -ny, -nz);
            //$$ vb.v(p, cx, cy, cz, rr, gg, bb, 255, 0.6F, 0.6F, OverlayTexture.NO_OVERLAY, light, -nx, -ny, -nz);
            //$$ vb.v(p, bx, by, bz, rr, gg, bb, 255, 0.6F, 0.4F, OverlayTexture.NO_OVERLAY, light, -nx, -ny, -nz);
            //$$ vb.v(p, ax, ay, az, rr, gg, bb, 255, 0.4F, 0.4F, OverlayTexture.NO_OVERLAY, light, -nx, -ny, -nz);
            //#endif
        }
    }
}
