package com.navidad.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/** (Cliente) La caja de música como objeto: la misma caja, cerradita. */
public final class CajaMusicaObjeto extends BlockEntityWithoutLevelRenderer {
    private CajaMusicaObjeto() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    public static BlockEntityWithoutLevelRenderer crear() {
        return new CajaMusicaObjeto();
    }

    @Override
    public void renderByItem(ItemStack st, ItemDisplayContext ctx, PoseStack ps, MultiBufferSource buf, int luz, int overlay) {
        CajaMusicaRenderer.dibujar(ps, buf, luz, overlay, Direction.SOUTH, 0F, 0F);
    }
}
