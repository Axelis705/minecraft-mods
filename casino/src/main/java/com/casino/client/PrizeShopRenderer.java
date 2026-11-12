package com.casino.client;

import com.casino.diamond.PrizeShopBE;
import com.casino.plat.client.CasinoBER;
import com.casino.plat.client.Draw;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/** El premio de la vitrina gira flotando sobre el cojín (cambia cada 3 segundos si hay varios). */
public class PrizeShopRenderer extends CasinoBER<PrizeShopBE> {
    private static final int FULL = 0xF000F0;

    public PrizeShopRenderer(BlockEntityRendererProvider.Context ctx) {
    }

    @Override
    protected void draw(PrizeShopBE be, float partial, PoseStack ps, Draw d, int light, int overlay) {
        if (be.offers.isEmpty() || be.getLevel() == null) {
            return;
        }
        long t = be.getLevel().getGameTime();
        int idx = (int) ((t / 60) % be.offers.size());
        ItemStack st = be.offers.get(idx).item();
        float time = t + partial;
        ps.pushPose();
        ps.translate(0.5, 1.12 + Math.sin(time / 12.0) * 0.04, 0.5);
        ps.mulPose(Axis.YP.rotationDegrees((time * 2.0F) % 360.0F));
        ps.scale(0.6F, 0.6F, 0.6F);
        d.item(st, ItemDisplayContext.GROUND, ps, FULL, be.getLevel(), (int) be.getBlockPos().asLong());
        ps.popPose();
    }
}
