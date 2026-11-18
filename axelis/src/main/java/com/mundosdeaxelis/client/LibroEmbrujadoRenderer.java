package com.mundosdeaxelis.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.mundosdeaxelis.entity.LibroEmbrujado;
import net.minecraft.client.model.BookModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Libro embrujado: el libro de la mesa de encantamientos (modelo y textura del juego), teñido de morado, que vuela abriendo y
 * cerrando las tapas y pasando hojas; cuando va a disparar las pasa mucho más rápido.
 */
public class LibroEmbrujadoRenderer extends EntityRenderer<LibroEmbrujado> {
    private static final ResourceLocation TEXTURE = new ResourceLocation("textures/entity/enchanting_table_book.png");
    private final BookModel book;

    public LibroEmbrujadoRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.book = new BookModel(context.bakeLayer(ModelLayers.BOOK));
        this.shadowRadius = 0.3F;
    }

    @Override
    public void render(LibroEmbrujado e, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        float age = e.tickCount + partialTick;
        boolean charging = e.isCharging();
        float bob = Mth.sin(age * 0.12F) * 0.08F;
        pose.pushPose();
        pose.translate(0.0, 0.45 + bob, 0.0);
        float body = Mth.rotLerp(partialTick, e.yBodyRotO, e.yBodyRot);
        pose.mulPose(Axis.YP.rotationDegrees(-body + 90.0F));
        pose.mulPose(Axis.ZP.rotationDegrees(70.0F + Mth.sin(age * 0.07F) * 6.0F));
        float speed = charging ? 0.55F : 0.18F;
        float open = charging ? 1.15F : 0.75F + Mth.sin(age * speed) * 0.35F;
        float flip = age * (charging ? 0.25F : 0.04F);
        float right = Mth.frac(flip + 0.25F) * 1.6F - 0.3F;
        float left = Mth.frac(flip + 0.75F) * 1.6F - 0.3F;
        this.book.setupAnim(age, Mth.clamp(right, 0.0F, 1.0F), Mth.clamp(left, 0.0F, 1.0F), open);
        VertexConsumer vc = buffers.getBuffer(this.book.renderType(TEXTURE));
        int overlay = LivingEntityRenderer.getOverlayCoords(e, 0.0F);
        float glow = charging ? 1.0F : 0.85F;
        this.book.render(pose, vc, light, overlay, 0.78F * glow, 0.55F * glow, 1.0F * glow, 1.0F);
        pose.popPose();
        super.render(e, yaw, partialTick, pose, buffers, light);
    }

    @Override
    public ResourceLocation getTextureLocation(LibroEmbrujado entity) {
        return TEXTURE;
    }
}
