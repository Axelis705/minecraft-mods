package com.autoscoleccion.client;

import com.autoscoleccion.item.CarItem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/** Dibuja los autos como objeto (inventario, mano, suelo, marcos) con el modelo 3D. */
public final class CarItemRenderer extends BlockEntityWithoutLevelRenderer {
    private static CarItemRenderer instance;

    private CarItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    public static CarItemRenderer get() {
        if (instance == null) {
            instance = new CarItemRenderer();
        }
        return instance;
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext ctx, PoseStack ps, MultiBufferSource buf, int light, int overlay) {
        if (!(stack.getItem() instanceof CarItem ci)) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        float time = mc.level != null ? mc.level.getGameTime() + mc.getFrameTime() : 0;
        ps.pushPose();
        ps.translate(0.5F, 0.0F, 0.5F);
        float k = 0.85F / 16.0F;
        ps.scale(k, k, k);
        ps.mulPose(Axis.YP.rotationDegrees(-90));
        CarModel.render(ci.def().style, ps, buf, light, time, 0);
        ps.popPose();
    }
}
