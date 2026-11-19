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
        Motion m = Motion.of(e);
        boolean charging = e.isCharging();
        float charge = m.smooth(0, charging ? 1.0F : 0.0F, 0.2F, partialTick);
        float shot = Motion.pulse(m.sinceEnd(0, charging, e, partialTick), 9.0F);
        float hurt = Anim.hurt(e, partialTick);
        float jig = m.jiggle(partialTick);
        float dth = Anim.death(e, partialTick);
        float pop = m.pop(e, partialTick);
        // vuela meciéndose; se inclina hacia donde va y se ladea en las curvas
        float bob = Mth.sin(age * 0.12F) * 0.08F + Mth.sin(age * 0.31F) * 0.02F;
        pose.pushPose();
        pose.translate(0.0, 0.45 + bob - dth * 0.35, 0.0);
        float body = Mth.rotLerp(partialTick, e.yBodyRotO, e.yBodyRot);
        pose.mulPose(Axis.YP.rotationDegrees(-body + 90.0F + Anim.easeIn(dth) * 720.0F));
        // (el libro mira hacia +X local: inclinarse "hacia delante" es girar sobre Z)
        pose.mulPose(Axis.ZP.rotation(-m.lean(partialTick) * 0.8F - shot * 0.35F + hurt * 0.4F));
        pose.mulPose(Axis.XP.rotation(m.bank(partialTick) * 0.8F + jig * 0.12F + Mth.sin(age * 1.9F) * 0.05F * charge));
        pose.mulPose(Axis.ZP.rotationDegrees(70.0F + Mth.sin(age * 0.07F) * 6.0F));
        float s = pop * (1.0F + shot * 0.12F + charge * 0.05F) * (1.0F - Anim.easeIn(dth) * 0.95F);
        pose.scale(s, s, s);
        // tapas: aletean como alas; al cargar se abren del todo y tiemblan; al disparar se cierran de golpe y rebotan
        float speed = 0.18F + 0.37F * charge;
        float flap = 0.75F + Mth.sin(age * speed) * 0.35F;
        float open = Mth.lerp(charge, flap, 1.15F + Mth.sin(age * 1.4F) * 0.04F) - shot * 0.6F + hurt * 0.3F + dth * 0.4F;
        open = m.spring(0, Math.max(0.05F, open), 0.35F, 0.3F, partialTick);
        // hojas: las pasa solo, muy rápido al cargar y a lo loco al recibir un golpe
        float flip = age * (0.04F + 0.21F * charge + 0.4F * hurt);
        float right = Mth.frac(flip + 0.25F) * 1.6F - 0.3F;
        float left = Mth.frac(flip + 0.75F) * 1.6F - 0.3F;
        this.book.setupAnim(age, Mth.clamp(right, 0.0F, 1.0F), Mth.clamp(left, 0.0F, 1.0F), open);
        VertexConsumer vc = buffers.getBuffer(this.book.renderType(TEXTURE));
        int overlay = LivingEntityRenderer.getOverlayCoords(e, 0.0F);
        float glow = 0.85F + 0.15F * charge + 0.1F * charge * Mth.sin(age * 0.8F);
        this.book.render(pose, vc, light, overlay, 0.78F * glow, 0.55F * glow, 1.0F * glow, 1.0F);
        pose.popPose();
        super.render(e, yaw, partialTick, pose, buffers, light);
    }

    @Override
    public ResourceLocation getTextureLocation(LibroEmbrujado entity) {
        return TEXTURE;
    }
}
