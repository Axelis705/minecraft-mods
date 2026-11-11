package com.autoscoleccion.client;

import com.autoscoleccion.entity.RcCarEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/** Dibuja el auto a control remoto con las llantas girando y las de adelante doblando. */
public class RcCarRenderer extends EntityRenderer<RcCarEntity> {
    public static final float SCALE = 0.9F / 16.0F;

    public RcCarRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        this.shadowRadius = 0.4F;
    }

    @Override
    public void render(RcCarEntity e, float yaw, float pt, PoseStack ps, MultiBufferSource buf, int light) {
        ps.pushPose();
        float y = Mth.rotLerp(pt, e.yRotO, e.getYRot());
        ps.mulPose(Axis.YP.rotationDegrees(-y - 90.0F));
        float steer = Mth.lerp(pt, e.steerO, e.steer);
        // inclinación de la carrocería en las curvas
        ps.mulPose(Axis.XP.rotationDegrees(-steer * Math.min(1, Math.abs(e.speed) * 3) * 4.0F));
        ps.scale(SCALE, SCALE, SCALE);
        float spin = Mth.lerp(pt, e.spinO, e.spin);
        float time = e.tickCount + pt;
        CarModel.renderDriving(e.def().style, ps, buf, light, time, spin, steer * 28.0F);
        ps.popPose();
        super.render(e, yaw, pt, ps, buf, light);
    }

    @Override
    public ResourceLocation getTextureLocation(RcCarEntity e) {
        return CarModel.WHITE;
    }
}
