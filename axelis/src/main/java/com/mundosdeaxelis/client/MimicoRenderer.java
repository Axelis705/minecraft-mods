package com.mundosdeaxelis.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mundosdeaxelis.MundosDeAxelis;
import com.mundosdeaxelis.entity.Mimico;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BeaconRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;

/**
 * Dibuja al Mímico con la madera y los adornos del cofre del santuario de su tier (bosque, forja, hielo, volcán y magia; los
 * tiers 6 a 8 usan el de magia por ahora). Dormido lleva el mismo haz de luz de rareza que un cofre de verdad (es la trampa);
 * despierto, sus ojos brillan en la oscuridad.
 */
public class MimicoRenderer extends MobRenderer<Mimico, MimicoModel> {
    private static final ResourceLocation[] TEX = new ResourceLocation[5];
    private static final ResourceLocation EYES = new ResourceLocation(MundosDeAxelis.MODID, "textures/entity/mimico_ojos.png");

    static {
        for (int i = 0; i < 5; i++) {
            TEX[i] = new ResourceLocation(MundosDeAxelis.MODID, "textures/entity/mimico_" + (i + 1) + ".png");
        }
    }

    public MimicoRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new MimicoModel(ctx.bakeLayer(MimicoModel.LAYER)), 0.5F);
        addLayer(new Eyes(this));
    }

    @Override
    public ResourceLocation getTextureLocation(Mimico e) {
        return TEX[Math.max(0, Math.min(4, e.tier() - 1))];
    }

    @Override
    public void render(Mimico e, float yaw, float pt, PoseStack pose, MultiBufferSource buf, int light) {
        super.render(e, yaw, pt, pose, buf, light);
        if (e.isAwake() || e.isDeadOrDying()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }
        int st = e.tier();
        int ps = com.mundosdeaxelis.crate.CrateForge.stage(TableClient.archive);
        if (st > 1 && st > ps + 1 && !mc.player.isCreative()) {
            return; // como los cofres sellados: sin luz
        }
        int c = com.mundosdeaxelis.block.VaultChestBlockEntity.rarity(st).color;
        float[] col = {((c >> 16) & 255) / 255.0F, ((c >> 8) & 255) / 255.0F, (c & 255) / 255.0F};
        pose.pushPose();
        pose.translate(-0.5D, 0.0D, -0.5D);
        BeaconRenderer.renderBeaconBeam(pose, buf, BeaconRenderer.BEAM_LOCATION, pt, 1.0F, e.level().getGameTime(), 1, 4 + st, col, 0.07F, 0.13F);
        pose.popPose();
    }

    @Override
    public boolean shouldRender(Mimico e, net.minecraft.client.renderer.culling.Frustum f, double x, double y, double z) {
        // el haz sube por encima de la caja de la entidad: que no desaparezca al mirar hacia arriba
        return super.shouldRender(e, f, x, y, z) || (!e.isAwake() && f.isVisible(e.getBoundingBox().expandTowards(0.0D, 4.0D + e.tier(), 0.0D)));
    }

    private static final class Eyes extends RenderLayer<Mimico, MimicoModel> {
        Eyes(MimicoRenderer parent) {
            super(parent);
        }

        @Override
        public void render(PoseStack pose, MultiBufferSource buf, int light, Mimico e, float limbSwing, float limbSwingAmount, float pt,
                           float age, float yawHead, float pitch) {
            if (!e.isAwake() || e.isInvisible()) {
                return;
            }
            getParentModel().renderToBuffer(pose, buf.getBuffer(RenderType.eyes(EYES)), LightTexture.FULL_BRIGHT,
                    LivingEntityRenderer.getOverlayCoords(e, 0.0F), 1.0F, 1.0F, 1.0F, 1.0F);
        }
    }
}
