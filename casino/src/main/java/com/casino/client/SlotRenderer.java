package com.casino.client;

import com.casino.Casino;
import com.casino.block.SlotBE;
import com.casino.game.SlotLayout;
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
import net.minecraft.world.level.Level;
import org.joml.Matrix4f;

/**
 * Dibuja lo que se mueve de la tragamonedas: los 3 carretes (con el giro y el frenado), las líneas ganadoras, las luces del marco,
 * la palanca y el marcador. El cuerpo de la máquina es un modelo normal (json). Todo sale de {@link SlotLayout} y del reloj del juego.
 */
public class SlotRenderer implements BlockEntityRenderer<SlotBE> {
    private static final ResourceLocation WHITE = tex("plinko_white");
    private static final ResourceLocation SYMBOLS = tex("slot_symbols");
    private static final ResourceLocation GLOW = tex("glow");

    private static final int FULL = 0xF000F0;
    // ventana de los carretes (en bloques, relativa al centro de la máquina; y desde el suelo del bloque de abajo)
    private static final float CELL = 0.25F;
    private static final float WIN_L = -0.375F;
    private static final float WIN_B = 1.125F;
    private static final float WIN_T = WIN_B + CELL * 3;
    private static final float Z = 0.385F;

    private final Font font;

    public SlotRenderer(BlockEntityRendererProvider.Context ctx) {
        this.font = ctx.getFont();
    }

    private static ResourceLocation tex(String n) {
        return new ResourceLocation(Casino.MODID, "textures/entity/" + n + ".png");
    }

    private static void v(VertexConsumer vc, PoseStack.Pose p, float x, float y, float z, float u, float w, int light, float nx, float ny, float nz,
                          int r, int g, int b, int a) {
        vc.vertex(p.pose(), x, y, z).color(r, g, b, a).uv(u, w).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(p.normal(), nx, ny, nz).endVertex();
    }

    private static void quadUV(VertexConsumer vc, PoseStack.Pose p, float x0, float y0, float x1, float y1, float z, float u0, float v0, float u1, float v1,
                               int light, int rgb, int a) {
        int r = (rgb >> 16) & 255;
        int g = (rgb >> 8) & 255;
        int b = rgb & 255;
        v(vc, p, x0, y0, z, u0, v1, light, 0, 0, 1, r, g, b, a);
        v(vc, p, x1, y0, z, u1, v1, light, 0, 0, 1, r, g, b, a);
        v(vc, p, x1, y1, z, u1, v0, light, 0, 0, 1, r, g, b, a);
        v(vc, p, x0, y1, z, u0, v0, light, 0, 0, 1, r, g, b, a);
    }

    private static void quad(VertexConsumer vc, PoseStack.Pose p, float x0, float y0, float x1, float y1, float z, int light, int rgb, int a) {
        quadUV(vc, p, x0, y0, x1, y1, z, 0, 0, 1, 1, light, rgb, a);
    }

    /** Barra delgada entre dos puntos (para las líneas de pago en diagonal). */
    private static void segment(VertexConsumer vc, PoseStack.Pose p, float x0, float y0, float x1, float y1, float w, float z, int light, int rgb, int a) {
        float dx = x1 - x0;
        float dy = y1 - y0;
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        float nx = -dy / len * w / 2;
        float ny = dx / len * w / 2;
        int r = (rgb >> 16) & 255;
        int g = (rgb >> 8) & 255;
        int b = rgb & 255;
        v(vc, p, x0 - nx, y0 - ny, z, 0, 1, light, 0, 0, 1, r, g, b, a);
        v(vc, p, x1 - nx, y1 - ny, z, 1, 1, light, 0, 0, 1, r, g, b, a);
        v(vc, p, x1 + nx, y1 + ny, z, 1, 0, light, 0, 0, 1, r, g, b, a);
        v(vc, p, x0 + nx, y0 + ny, z, 0, 0, light, 0, 0, 1, r, g, b, a);
    }

    private static int scale(int rgb, float k) {
        int r = Math.min(255, (int) (((rgb >> 16) & 255) * k));
        int g = Math.min(255, (int) (((rgb >> 8) & 255) * k));
        int b = Math.min(255, (int) ((rgb & 255) * k));
        return (r << 16) | (g << 8) | b;
    }

    /** Caja de color liso (la palanca). */
    private static void box(VertexConsumer vc, PoseStack.Pose p, float x0, float y0, float z0, float x1, float y1, float z1, int rgb, int light) {
        int fr = scale(rgb, 0.85F);
        int sd = scale(rgb, 0.65F);
        int tp = rgb;
        // frente (+z)
        quadUV(vc, p, x0, y0, x1, y1, z1, 0, 0, 1, 1, light, fr, 255);
        // atrás (-z)
        int r = (sd >> 16) & 255;
        int g = (sd >> 8) & 255;
        int b = sd & 255;
        v(vc, p, x1, y0, z0, 0, 1, light, 0, 0, -1, r, g, b, 255);
        v(vc, p, x0, y0, z0, 1, 1, light, 0, 0, -1, r, g, b, 255);
        v(vc, p, x0, y1, z0, 1, 0, light, 0, 0, -1, r, g, b, 255);
        v(vc, p, x1, y1, z0, 0, 0, light, 0, 0, -1, r, g, b, 255);
        // izquierda (-x) y derecha (+x)
        v(vc, p, x0, y0, z0, 0, 1, light, -1, 0, 0, r, g, b, 255);
        v(vc, p, x0, y0, z1, 1, 1, light, -1, 0, 0, r, g, b, 255);
        v(vc, p, x0, y1, z1, 1, 0, light, -1, 0, 0, r, g, b, 255);
        v(vc, p, x0, y1, z0, 0, 0, light, -1, 0, 0, r, g, b, 255);
        v(vc, p, x1, y0, z1, 0, 1, light, 1, 0, 0, r, g, b, 255);
        v(vc, p, x1, y0, z0, 1, 1, light, 1, 0, 0, r, g, b, 255);
        v(vc, p, x1, y1, z0, 1, 0, light, 1, 0, 0, r, g, b, 255);
        v(vc, p, x1, y1, z1, 0, 0, light, 1, 0, 0, r, g, b, 255);
        // arriba (+y)
        r = (tp >> 16) & 255;
        g = (tp >> 8) & 255;
        b = tp & 255;
        v(vc, p, x0, y1, z1, 0, 1, light, 0, 1, 0, r, g, b, 255);
        v(vc, p, x1, y1, z1, 1, 1, light, 0, 1, 0, r, g, b, 255);
        v(vc, p, x1, y1, z0, 1, 0, light, 0, 1, 0, r, g, b, 255);
        v(vc, p, x0, y1, z0, 0, 0, light, 0, 1, 0, r, g, b, 255);
    }

    private static float reelX(int reel) {
        return WIN_L + reel * CELL;
    }

    private static float rowTop(int row) {
        return WIN_T - row * CELL;
    }

    @Override
    public void render(SlotBE be, float partial, PoseStack ps, MultiBufferSource buf, int packedLight, int overlay) {
        Level level = be.getLevel();
        if (level == null) {
            return;
        }
        long gt = level.getGameTime();
        float time = gt + partial;
        int l0 = LevelRenderer.getLightColor(level, be.getBlockPos().above());
        int lit = LightTexture.pack(Math.max(LightTexture.block(l0), 12), Math.max(LightTexture.sky(l0), 9));
        boolean spinning = be.spinning;
        float spinT = time - be.spinStart;
        boolean flashing = !spinning && gt < be.flashEnd;
        SlotLayout.Result res = SlotLayout.evaluate(be.stops);

        ps.pushPose();
        ps.translate(0.5, 0, 0.5);
        ps.mulPose(Axis.YP.rotationDegrees(-be.facing().toYRot()));
        PoseStack.Pose pose = ps.last();

        // OJO: cada pasada pide su "pincel" justo antes de usarlo y no lo guarda (ver PlinkoRenderer).
        // ---- fondo de los carretes (crema) y separadores
        VertexConsumer vc = buf.getBuffer(RenderType.entityCutoutNoCull(WHITE));
        quad(vc, pose, WIN_L, WIN_B, WIN_L + CELL * 3, WIN_T, Z, FULL, 0xFFF1D2, 255);
        for (int i = 0; i <= SlotLayout.REELS; i++) {
            float x = reelX(i);
            quad(vc, pose, x - 0.006F, WIN_B, x + 0.006F, WIN_T, Z + 0.001F, FULL, 0x3A2410, 255);
        }
        // sombras arriba y abajo de la ventana (efecto de cilindro)
        quad(vc, pose, WIN_L, WIN_T - 0.05F, WIN_L + CELL * 3, WIN_T, Z + 0.0012F, FULL, 0xB8A27C, 255);
        quad(vc, pose, WIN_L, WIN_B, WIN_L + CELL * 3, WIN_B + 0.05F, Z + 0.0012F, FULL, 0xB8A27C, 255);

        // ---- símbolos de cada carrete
        vc = buf.getBuffer(RenderType.entityCutoutNoCull(SYMBOLS));
        for (int reel = 0; reel < SlotLayout.REELS; reel++) {
            double o = spinning || flashing || gt < be.spinStart + SlotLayout.END_TICK + 8
                    ? SlotLayout.offset(reel, be.stops[reel], spinning ? spinT : SlotLayout.END_TICK + 20) : be.stops[reel];
            if (!spinning) {
                o = be.stops[reel];
            }
            float fast = spinning && spinT < SlotLayout.STOP_TICK[reel] - 7 ? 0.82F : 1.0F;
            int shade = (int) (255 * fast);
            int rgb = (shade << 16) | (shade << 8) | shade;
            int base = (int) Math.floor(o);
            for (int j = base - 2; j <= base + 3; j++) {
                float slot = (float) (j - o + 1);
                float yTop = WIN_T - slot * CELL;
                float yBot = yTop - CELL;
                float cTop = Math.min(yTop, WIN_T);
                float cBot = Math.max(yBot, WIN_B);
                if (cTop <= cBot) {
                    continue;
                }
                int sym = SlotLayout.STRIPS[reel][SlotLayout.mod(j)];
                float vT = (yTop - cTop) / CELL;
                float vB = (yTop - cBot) / CELL;
                float u0 = sym / (float) SlotLayout.SYMBOLS;
                float u1 = (sym + 1) / (float) SlotLayout.SYMBOLS;
                float x = reelX(reel);
                quadUV(vc, pose, x + 0.006F, cBot, x + CELL - 0.006F, cTop, Z + 0.002F, u0, vT, u1, vB, FULL, rgb, 255);
            }
        }

        // ---- palanca (se baja al empezar el giro y vuelve)
        float pull = 0;
        if (spinning && spinT < 18) {
            pull = spinT < 6 ? spinT / 6.0F : 1.0F - (spinT - 6) / 12.0F;
            pull = Math.max(0, Math.min(1, pull));
        }
        vc = buf.getBuffer(RenderType.entityCutoutNoCull(WHITE));
        ps.pushPose();
        ps.translate(0.4375F, 0.72F, 0.0F);
        ps.mulPose(Axis.ZP.rotationDegrees(-30 - pull * 100));
        PoseStack.Pose lp = ps.last();
        box(vc, lp, -0.03F, 0.0F, -0.03F, 0.03F, 0.40F, 0.03F, 0xC9CBD6, lit);
        box(vc, lp, -0.07F, 0.38F, -0.07F, 0.07F, 0.52F, 0.07F, 0xE0202A, lit);
        ps.popPose();

        // ---- líneas ganadoras (parpadean) y casillas que ganaron
        if (flashing) {
            float blink = 0.55F + 0.45F * (float) Math.sin(time * 0.55F);
            vc = buf.getBuffer(RenderType.eyes(GLOW));
            for (int l = 0; l < SlotLayout.LINES; l++) {
                if ((be.flashMask & (1 << l)) == 0) {
                    continue;
                }
                int[] rows = SlotLayout.LINE_ROWS[l];
                int a = res.grid[0][rows[0]];
                int b = res.grid[1][rows[1]];
                int c = res.grid[2][rows[2]];
                int cells = a == b && b == c ? 3 : 2;
                int col = be.jackpotHit && a == SlotLayout.CROWN ? 0xD070FF : 0xFFD23C;
                for (int k = 0; k < cells; k++) {
                    float cx = reelX(k) + CELL / 2;
                    float cy = rowTop(rows[k]) - CELL / 2;
                    quad(vc, pose, cx - CELL * 0.52F, cy - CELL * 0.52F, cx + CELL * 0.52F, cy + CELL * 0.52F, Z + 0.01F, FULL, scale(col, 0.40F * blink), 255);
                }
                for (int k = 0; k + 1 < cells; k++) {
                    float cx0 = reelX(k) + CELL / 2;
                    float cy0 = rowTop(rows[k]) - CELL / 2;
                    float cx1 = reelX(k + 1) + CELL / 2;
                    float cy1 = rowTop(rows[k + 1]) - CELL / 2;
                    segment(vc, pose, cx0, cy0, cx1, cy1, 0.028F, Z + 0.012F, FULL, scale(col, blink), 255);
                }
            }
        }

        // ---- luces que corren por el marco (brillan solas)
        vc = buf.getBuffer(RenderType.eyes(GLOW));
        boolean fiesta = flashing;
        float speed = spinning ? 0.9F : fiesta ? 1.2F : 0.22F;
        int idx = 0;
        for (int i = 0; i < 9; i++) { // arriba
            float bx = -0.40F + i * 0.1F;
            idx = bulb(vc, pose, bx, 1.945F, idx, time, speed, spinning, fiesta, be.jackpotHit);
        }
        for (int i = 0; i < 6; i++) { // lados
            float by = 1.86F - i * 0.14F;
            idx = bulb(vc, pose, -0.415F, by, idx, time, speed, spinning, fiesta, be.jackpotHit);
            idx = bulb(vc, pose, 0.415F, by, idx, time, speed, spinning, fiesta, be.jackpotHit);
        }

        // ---- marcador (apuesta o premio)
        Component msg;
        int col;
        if (flashing && be.flashPayout > 0) {
            msg = Component.literal("+" + be.flashPayout);
            col = 0xFFFFD23C;
        } else if (spinning) {
            msg = Component.literal(String.valueOf(be.bet));
            col = 0xFF9AA0B8;
        } else {
            msg = Component.translatable("casino.slot.led");
            col = 0xFF60E0A0;
        }
        ps.pushPose();
        ps.translate(0, 1.055F, Z + 0.03F);
        float s = 0.0105F;
        ps.scale(s, -s, s);
        Matrix4f mat = ps.last().pose();
        font.drawInBatch(msg, -font.width(msg) / 2.0F, -4, col, false, mat, buf, Font.DisplayMode.NORMAL, 0, FULL);
        ps.popPose();

        ps.popPose();
    }

    private int bulb(VertexConsumer vc, PoseStack.Pose pose, float x, float y, int idx, float time, float speed, boolean spinning, boolean fiesta, boolean jackpot) {
        float ph = ((idx - time * speed) % 5 + 5) % 5;
        float bright = ph < 1.3F ? 1.0F : 0.2F;
        int rgb = jackpot && fiesta ? (idx % 2 == 0 ? 0xD070FF : 0xFFE040) : spinning ? 0xFF60D0 : fiesta ? 0xFFE040 : 0xFFB020;
        quad(vc, pose, x - 0.045F, y - 0.045F, x + 0.045F, y + 0.045F, Z + 0.06F, FULL, scale(rgb, bright), 255);
        return idx + 1;
    }

    @Override
    public boolean shouldRenderOffScreen(SlotBE be) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 64;
    }
}
