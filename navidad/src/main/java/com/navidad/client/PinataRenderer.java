package com.navidad.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.navidad.bloque.deco.Deco;
import com.navidad.entidad.Pinata;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/** La piñata: el modelo del adorno de estrella, en grande, colgando de una cuerda; se columpia al recibir un golpe. */
public class PinataRenderer extends EntityRenderer<Pinata> {
    public PinataRenderer(EntityRendererProvider.Context c) {
        super(c);
        this.shadowRadius = 0.4F;
    }

    @Override
    public void render(Pinata e, float yaw, float pt, PoseStack ps, MultiBufferSource buf, int luz) {
        var br = Minecraft.getInstance().getBlockRenderer();
        float t = e.tickCount + pt - e.golpe();
        // columpio que se va apagando (y un vaivén suave siempre)
        float swing = t >= 0 && t < 80 ? 28F * (float) Math.exp(-t / 22F) * Mth.sin(t * 0.32F) : 0F;
        swing += 2.5F * Mth.sin((e.tickCount + pt) * 0.05F);
        float giro = (e.tickCount + pt) * 0.6F + e.golpes() * 23F;
        ps.pushPose();
        // pivote arriba de la cuerda
        ps.translate(0, 3.2, 0);
        ps.mulPose(Axis.YP.rotationDegrees(e.lado()));
        ps.mulPose(Axis.XP.rotationDegrees(swing));
        ps.mulPose(Axis.YP.rotationDegrees(-e.lado()));
        // la cuerda (lana blanca muy delgada)
        ps.pushPose();
        ps.translate(-0.03, -1.5, -0.03);
        ps.scale(0.06F, 1.5F, 0.06F);
        br.renderSingleBlock(Blocks.WHITE_WOOL.defaultBlockState(), ps, buf, luz, OverlayTexture.NO_OVERLAY);
        ps.popPose();
        // la estrella (adorno de 16 px escalado a 1.7 bloques, de y 0 a 1.7), girando despacio
        Block b = Deco.block("pinata_adorno");
        if (b != null) {
            ps.translate(0, -1.5, 0);
            ps.mulPose(Axis.YP.rotationDegrees(giro));
            ps.scale(1.7F, 1.7F, 1.7F);
            ps.translate(-0.5, -1.0, -0.5);
            br.renderSingleBlock(b.defaultBlockState(), ps, buf, luz, OverlayTexture.NO_OVERLAY);
        }
        ps.popPose();
        super.render(e, yaw, pt, ps, buf, luz);
    }

    @Override
    public ResourceLocation getTextureLocation(Pinata e) {
        return InventoryMenu.BLOCK_ATLAS;
    }
}
