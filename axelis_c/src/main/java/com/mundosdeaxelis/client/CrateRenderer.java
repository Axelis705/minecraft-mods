package com.mundosdeaxelis.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.mundosdeaxelis.block.CrateBlockEntity;
import com.mundosdeaxelis.crate.CrateKind;
import com.mundosdeaxelis.crate.CratePools;
import com.mundosdeaxelis.crate.CrateRoll;
import com.mundosdeaxelis.crate.Tier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Dibuja el crate: gema flotante y nombre mientras espera, y mientras alguien lo abre, el holograma de la ruleta sobre el
 * bloque (lo ve todo el que esté cerca) con haz de luz, rayos y el premio al final.
 */
public class CrateRenderer implements BlockEntityRenderer<CrateBlockEntity> {
    private final ItemRenderer items;
    private final Font font;

    public CrateRenderer(BlockEntityRendererProvider.Context ctx) {
        this.items = ctx.getItemRenderer();
        this.font = ctx.getFont();
    }

    @Override
    public boolean shouldRenderOffScreen(CrateBlockEntity be) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 64;
    }

    // ------------------------------------------------------------------ utilidades de dibujo

    private static void v(VertexConsumer vc, PoseStack.Pose p, float x, float y, float z, float u, float vv,
                          float r, float g, float b, float a) {
        vc.vertex(p.pose(), x, y, z).color(r, g, b, a).uv(u, vv)
                .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(LightTexture.FULL_BRIGHT)
                .normal(p.normal(), 0.0F, 0.0F, 1.0F).endVertex();
    }

    /** Rectángulo del atlas (celda de 32x32) en el plano XY, centrado. */
    private static void cell(VertexConsumer vc, PoseStack ps, int[] c, float cx, float cy, float z, float w, float h, int rgb, float a) {
        float u0 = c[0] / (float) CrateUi.ATLAS_W, u1 = (c[0] + 32) / (float) CrateUi.ATLAS_W;
        float v0 = c[1] / (float) CrateUi.ATLAS_H, v1 = (c[1] + 32) / (float) CrateUi.ATLAS_H;
        float r = CrateUi.r(rgb), g = CrateUi.g(rgb), b = CrateUi.b(rgb);
        PoseStack.Pose p = ps.last();
        v(vc, p, cx - w / 2, cy - h / 2, z, u0, v1, r, g, b, a);
        v(vc, p, cx + w / 2, cy - h / 2, z, u1, v1, r, g, b, a);
        v(vc, p, cx + w / 2, cy + h / 2, z, u1, v0, r, g, b, a);
        v(vc, p, cx - w / 2, cy + h / 2, z, u0, v0, r, g, b, a);
    }

    /** Marco de la tira en 3 trozos (extremos fijos + centro estirado) para que no se vea borroso. */
    private static void panel(VertexConsumer vc, PoseStack ps, float cx, float cy, float z, float w, float h, int rgb, float a) {
        float r = CrateUi.r(rgb), g = CrateUi.g(rgb), b = CrateUi.b(rgb);
        PoseStack.Pose p = ps.last();
        float cap = h * (10.0F / 32.0F);
        float x0 = cx - w / 2, x1 = x0 + cap, x3 = cx + w / 2, x2 = x3 - cap;
        float v0 = CrateUi.PANEL[1] / (float) CrateUi.ATLAS_H, v1 = (CrateUi.PANEL[1] + 32) / (float) CrateUi.ATLAS_H;
        float bx = CrateUi.PANEL[0];
        float ua = bx / CrateUi.ATLAS_W, ub = (bx + 10) / (float) CrateUi.ATLAS_W,
                uc = (bx + 22) / (float) CrateUi.ATLAS_W, ud = (bx + 32) / (float) CrateUi.ATLAS_W;
        float[][] seg = {{x0, x1, ua, ub}, {x1, x2, ub, uc}, {x2, x3, uc, ud}};
        for (float[] q : seg) {
            v(vc, p, q[0], cy - h / 2, z, q[2], v1, r, g, b, a);
            v(vc, p, q[1], cy - h / 2, z, q[3], v1, r, g, b, a);
            v(vc, p, q[1], cy + h / 2, z, q[3], v0, r, g, b, a);
            v(vc, p, q[0], cy + h / 2, z, q[2], v0, r, g, b, a);
        }
    }

    /** Textura entera en el plano XY, centrada. */
    private static void full(VertexConsumer vc, PoseStack ps, float cx, float cy, float z, float w, float h, int rgb, float a) {
        float r = CrateUi.r(rgb), g = CrateUi.g(rgb), b = CrateUi.b(rgb);
        PoseStack.Pose p = ps.last();
        v(vc, p, cx - w / 2, cy - h / 2, z, 0, 1, r, g, b, a);
        v(vc, p, cx + w / 2, cy - h / 2, z, 1, 1, r, g, b, a);
        v(vc, p, cx + w / 2, cy + h / 2, z, 1, 0, r, g, b, a);
        v(vc, p, cx - w / 2, cy + h / 2, z, 0, 0, r, g, b, a);
    }

    /** Cuadrado con textura tumbado en el plano horizontal. */
    private static void flat(VertexConsumer vc, PoseStack ps, float half, int rgb, float a) {
        float r = CrateUi.r(rgb), g = CrateUi.g(rgb), b = CrateUi.b(rgb);
        PoseStack.Pose p = ps.last();
        v(vc, p, -half, 0, -half, 0, 0, r, g, b, a);
        v(vc, p, -half, 0, half, 0, 1, r, g, b, a);
        v(vc, p, half, 0, half, 1, 1, r, g, b, a);
        v(vc, p, half, 0, -half, 1, 0, r, g, b, a);
    }

    /** Pone el sistema de ejes mirando a la cámara: x a la derecha, y arriba, z hacia el jugador. */
    private static void billboard(PoseStack ps) {
        ps.mulPose(Minecraft.getInstance().gameRenderer.getMainCamera().rotation());
        ps.mulPose(Axis.YP.rotationDegrees(180.0F));
    }

    private void text(Component c, float y, float scale, int color, PoseStack ps, MultiBufferSource buf) {
        ps.pushPose();
        ps.translate(0.5, y, 0.5);
        ps.mulPose(Minecraft.getInstance().gameRenderer.getMainCamera().rotation());
        float s = 0.025F * scale;
        ps.scale(-s, -s, s);
        float w = font.width(c);
        font.drawInBatch(c, -w / 2.0F, 0, color, false, ps.last().pose(), buf, Font.DisplayMode.NORMAL, 0x38000000,
                LightTexture.FULL_BRIGHT);
        ps.popPose();
    }

    private void item(Item it, float x, float y, float z, float size, PoseStack ps, MultiBufferSource buf, Level level, float yaw) {
        ps.pushPose();
        ps.translate(x, y, z);
        if (yaw != 0) {
            ps.mulPose(Axis.YP.rotationDegrees(yaw));
        }
        ps.scale(size, size, size * 0.4F);
        items.renderStatic(new ItemStack(it), ItemDisplayContext.GUI, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY,
                ps, buf, level, 0);
        ps.popPose();
    }

    private static float easeOutBack(float x) {
        float c1 = 1.70158F, c3 = c1 + 1.0F, p = x - 1.0F;
        return 1.0F + c3 * p * p * p + c1 * p * p;
    }

    // ------------------------------------------------------------------ dibujo principal

    @Override
    public void render(CrateBlockEntity be, float pt, PoseStack ps, MultiBufferSource buf, int light, int overlay) {
        Level level = be.getLevel();
        if (level == null) {
            return;
        }
        CrateKind kind = be.kind();
        float time = level.getGameTime() + pt;
        if (!be.rolling()) {
            idle(kind, time, ps, buf);
            return;
        }
        float el = time - be.start;
        if (el < 0 || el > CrateRoll.TOTAL + 6) {
            idle(kind, time, ps, buf);
            return;
        }
        roll(be, kind, el, time, ps, buf, level);
    }

    private void idle(CrateKind kind, float time, PoseStack ps, MultiBufferSource buf) {
        ps.pushPose();
        ps.translate(0.5, 1.02, 0.5);
        // halo sobre el cofre
        float pulse = 0.6F + 0.4F * Mth.sin(time * 0.1F);
        flat(buf.getBuffer(RenderType.entityTranslucentEmissive(CrateUi.HALO)), ps, 0.5F + 0.05F * pulse, kind.color, 0.40F * pulse);
        ps.popPose();
        // cristal 3D que gira, con brillo detrás, esquirlas orbitando y destellos
        float bob = 0.07F * Mth.sin(time * 0.08F);
        ps.pushPose();
        ps.translate(0.5, 1.5 + bob, 0.5);
        ps.pushPose();
        billboard(ps);
        VertexConsumer vc = buf.getBuffer(RenderType.entityTranslucentEmissive(CrateUi.ATLAS));
        cell(vc, ps, CrateUi.GLOW, 0, 0, -0.30F, 1.25F, 1.25F, kind.color, 0.45F * pulse);
        for (int k = 0; k < 3; k++) {
            float ph = time * 0.045F + k * 2.1F;
            float tw = Math.max(0, Mth.sin(ph * 3.0F));
            cell(vc, ps, CrateUi.STAR, Mth.cos(ph) * 0.42F, Mth.sin(ph * 1.3F) * 0.36F, 0.25F, 0.22F * tw, 0.22F * tw, 0xFFFFFF, tw);
        }
        ps.popPose();
        Gem3D.Shape[] shapes = Gem3D.Shape.values();
        Gem3D.Shape shape = shapes[Math.min(shapes.length - 1, kind.gem)];
        VertexConsumer solid = buf.getBuffer(RenderType.entityCutoutNoCull(CrateUi.WHITE));
        Gem3D.draw(solid, ps, shape, kind.color, 0.62F, time * 2.6F, 1.0F);
        for (int k = 0; k < 3; k++) {
            float ang = time * 0.06F + k * Mth.TWO_PI / 3.0F;
            ps.pushPose();
            ps.translate(Mth.cos(ang) * 0.52F, 0.10F * Mth.sin(time * 0.1F + k * 2.0F), Mth.sin(ang) * 0.52F);
            Gem3D.draw(solid, ps, Gem3D.Shape.SPIKE, CrateUi.lerp(kind.color, 0xFFFFFF, 0.35F), 0.16F, -time * 5.0F, 1.0F);
            ps.popPose();
        }
        ps.popPose();
        // nombre (solo si estás cerca)
        text(Component.translatable(kind.titleKey()), 1.85F, 0.9F, kind.color | 0xFF000000, ps, buf);
    }

    private void roll(CrateBlockEntity be, CrateKind kind, float el, float time, PoseStack ps, MultiBufferSource buf, Level level) {
        float appear = Mth.clamp(el / 8.0F, 0, 1);
        float vanish = Mth.clamp((CrateRoll.TOTAL + 4 - el) / 10.0F, 0, 1);
        float vis = Math.min(appear, vanish);
        double pos = CrateRoll.pos(Math.min(el, CrateRoll.ROLL), be.seed);
        Item[] strip = be.strip();
        int slot = Mth.clamp((int) Math.round(pos), 0, strip.length - 1);
        Item prize = be.winnerItem();
        Tier prizeTier = CratePools.tierOf(be.kind(), prize);
        boolean revealed = el >= CrateRoll.ROLL;
        float rev = revealed ? el - CrateRoll.ROLL : 0;
        int col = revealed ? prizeTier.color : CratePools.tierOf(be.kind(), strip[slot]).color;

        // haz de luz del color de lo que cae bajo la flecha
        ps.pushPose();
        ps.translate(0.5, 0.9, 0.5);
        float fullH = 3.6F * (1.0F + 0.05F * prizeTier.ordinal());
        float grow = Mth.clamp(rev / 10.0F, 0, 1);
        float height = appear * (revealed ? 1.0F + (fullH - 1.0F) * grow : 1.0F);
        float width = 0.30F + 0.06F * Mth.sin(time * 0.3F) + (revealed ? 0.04F * prizeTier.ordinal() : 0);
        VertexConsumer beam = buf.getBuffer(RenderType.entityTranslucentEmissive(CrateUi.BEAM));
        for (int k = 0; k < 2; k++) {
            ps.pushPose();
            ps.mulPose(Axis.YP.rotationDegrees(time * 3.0F + k * 90.0F));
            PoseStack.Pose p = ps.last();
            float a = 0.55F * vis;
            float r = CrateUi.r(col), g = CrateUi.g(col), b = CrateUi.b(col);
            v(beam, p, -width, 0, 0, 0, 1, r, g, b, a);
            v(beam, p, width, 0, 0, 1, 1, r, g, b, a);
            v(beam, p, width, height, 0, 1, 0, r, g, b, a);
            v(beam, p, -width, height, 0, 0, 0, r, g, b, a);
            ps.popPose();
        }
        // anillos de luz que suben y se abren al revelar
        if (revealed) {
            VertexConsumer halo = buf.getBuffer(RenderType.entityTranslucentEmissive(CrateUi.HALO));
            for (int k = 0; k < 3; k++) {
                float local = (rev - k * 8.0F) / 24.0F;
                if (local < 0 || local > 1) {
                    continue;
                }
                ps.pushPose();
                ps.translate(0, local * 1.6F, 0);
                ps.mulPose(Axis.YP.rotationDegrees(time * 6.0F * (k % 2 == 0 ? 1 : -1)));
                flat(halo, ps, 0.5F + local * (1.2F + 0.2F * prizeTier.ordinal()), col, (1 - local) * 0.9F * vis);
                ps.popPose();
            }
        }
        ps.popPose();

        // el holograma de la ruleta
        ps.pushPose();
        ps.translate(0.5, 2.35, 0.5);
        billboard(ps);
        float sc = vis * (0.6F + 0.4F * easeOutBack(appear));
        ps.scale(sc, sc, sc);
        float cw = 0.46F, band = 0.56F;
        VertexConsumer atlas = buf.getBuffer(RenderType.entityTranslucentEmissive(CrateUi.ATLAS));
        float stripFade = 1.0F - Mth.clamp((rev - 2) / 8.0F, 0, 1);
        // fondo
        panel(atlas, ps, 0, 0, -0.01F, cw * 9.2F, band + 0.14F, col, 0.55F * stripFade);
        int first = (int) Math.floor(pos) - 4, last = (int) Math.ceil(pos) + 4;
        for (int i = Math.max(0, first); i <= Math.min(strip.length - 1, last); i++) {
            float d = (float) (i - pos);
            float x = d * cw;
            float edge = Mth.clamp((4.3F - Math.abs(d)) / 1.6F, 0, 1);
            float close = 1.0F - Math.min(1.0F, Math.abs(d));
            boolean win = revealed && i == CrateRoll.WIN;
            float fade = edge * (win ? 1.0F - Mth.clamp((rev - 4) / 6.0F, 0, 1) : stripFade);
            if (fade <= 0.02F) {
                continue;
            }
            Tier t = CratePools.tierOf(be.kind(), strip[i]);
            float size = band * (1.0F + 0.12F * close * close);
            cell(atlas, ps, CrateUi.SLOT, x, 0, 0.0F, size, size, CrateUi.lerp(0x8E98BE, t.color, 0.55F + 0.45F * close), fade);
        }
        // flecha y línea central
        float pa = vis * (1 - 0.5F * Mth.clamp(rev / 10.0F, 0, 1));
        cell(atlas, ps, CrateUi.POINTER, 0, band * 0.5F + 0.15F, 0.002F, 0.2F, 0.2F, 0xFFD966, pa);
        // objetos
        for (int i = Math.max(0, first); i <= Math.min(strip.length - 1, last); i++) {
            float d = (float) (i - pos);
            float edge = Mth.clamp((4.3F - Math.abs(d)) / 1.6F, 0, 1);
            float close = 1.0F - Math.min(1.0F, Math.abs(d));
            boolean win = revealed && i == CrateRoll.WIN;
            float fade = edge * (win ? 1.0F - Mth.clamp((rev - 4) / 6.0F, 0, 1) : stripFade);
            if (fade <= 0.25F) {
                continue;
            }
            item(strip[i], d * cw, 0, 0.03F, band * 0.62F * (1.0F + 0.12F * close * close) * Math.min(1, fade * 1.4F), ps, buf, level, 0);
        }
        ps.popPose();

        // al revelar: rayos y el premio grande subiendo
        if (revealed) {
            float u = Mth.clamp(rev / 16.0F, 0, 1);
            float rise = 1.0F - (1.0F - u) * (1.0F - u);
            float bigScale = 0.30F + 0.60F * Mth.clamp(easeOutBack(u), 0, 1.3F);
            ps.pushPose();
            ps.translate(0.5, 2.35 + 0.45F + 0.55F * rise + 0.04F * Mth.sin(time * 0.2F), 0.5);
            billboard(ps);
            ps.scale(vis, vis, vis);
            // rayos detrás
            ps.pushPose();
            ps.mulPose(Axis.ZP.rotationDegrees(time * 2.0F));
            full(buf.getBuffer(RenderType.entityTranslucentEmissive(CrateUi.BURST)), ps, 0, 0, -0.05F,
                    (1.4F + 0.3F * prizeTier.ordinal()) * rise, (1.4F + 0.3F * prizeTier.ordinal()) * rise, prizeTier.color, 0.85F);
            ps.popPose();
            // el objeto con un vaivén suave
            item(prize, 0, 0, 0.0F, bigScale, ps, buf, level, Mth.sin(time * 0.12F) * 25.0F);
            ps.popPose();
            // textos
            float ta = Mth.clamp((rev - 6) / 8.0F, 0, 1) * vis;
            if (ta > 0.05F) {
                int a = ((int) (255 * ta)) << 24;
                text(prizeTier.label(), 4.55F, 1.1F, a | 0xFFFFFF, ps, buf);
                text(prizeTier.paint(CratePools.label(CratePools.stack(be.kind(), prize, be.seed))), 4.25F, 0.8F, a | 0xFFFFFF, ps, buf);
            }
        } else {
            text(Component.translatable("crate.mundos_de_axelis.opens", be.openerName), 3.25F, 0.9F,
                    0xFF000000 | kind.color, ps, buf);
        }
    }
}
