package com.mundosdeaxelis.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mundosdeaxelis.MundosDeAxelis;
import com.mundosdeaxelis.entity.DriadeRoble;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Etapa 288: la Dríade del Roble. Cuerpo humano con piel de corteza, vestido de hojas y corona de flores (textura propia), ojos de
 * savia que brillan en la oscuridad. Dormida está arrodillada con la cabeza gacha; al lanzar flores o raíces levanta los brazos y
 * al tirar la enredadera estira uno hacia su objetivo.
 */
public class DriadeRenderer extends HumanoidMobRenderer<DriadeRoble, DriadeRenderer.Modelo> {
    private static final ResourceLocation TEXTURE = new ResourceLocation(MundosDeAxelis.MODID, "textures/entity/driade_roble.png");
    private static final RenderType OJOS = RenderType.eyes(new ResourceLocation(MundosDeAxelis.MODID, "textures/entity/driade_roble_ojos.png"));

    public DriadeRenderer(EntityRendererProvider.Context context) {
        super(context, new Modelo(context.bakeLayer(ModelLayers.ZOMBIE)), 0.5F);
        addLayer(new Ojos(this));
    }

    @Override
    public ResourceLocation getTextureLocation(DriadeRoble e) {
        return TEXTURE;
    }

    @Override
    protected void scale(DriadeRoble e, PoseStack pose, float pt) {
        pose.scale(1.12F, 1.12F, 1.12F); // un poco más alta que una persona
    }

    @Override
    protected void setupRotations(DriadeRoble e, PoseStack pose, float age, float yaw, float pt) {
        super.setupRotations(e, pose, age, yaw, pt);
        if (e.dormida()) {
            pose.translate(0.0, -0.62, 0.0); // arrodillada
        } else {
            pose.translate(0.0, Mth.sin(age * 0.08F) * 0.04F, 0.0); // flota un poco, como un espíritu
        }
    }

    /** Las poses propias de la dríade sobre el cuerpo humano «vivo» del mod. */
    public static class Modelo extends LivelyHumanoidModel<DriadeRoble> {
        public Modelo(ModelPart root) {
            super(root, false, false);
        }

        @Override
        public void setupAnim(DriadeRoble e, float limbSwing, float limbSwingAmount, float age, float netHeadYaw, float headPitch) {
            super.setupAnim(e, limbSwing, limbSwingAmount, age, netHeadYaw, headPitch);
            if (e.dormida()) {
                this.head.xRot = 0.6F;
                this.head.yRot = 0.0F;
                this.rightArm.xRot = -0.3F;
                this.leftArm.xRot = -0.3F;
                this.rightArm.zRot = 0.35F;
                this.leftArm.zRot = -0.35F;
                this.rightLeg.xRot = -1.45F;
                this.leftLeg.xRot = -1.45F;
                this.rightLeg.yRot = 0.3F;
                this.leftLeg.yRot = -0.3F;
                this.hat.copyFrom(this.head);
                return;
            }
            float sway = Mth.sin(age * 0.12F) * 0.06F;
            if (e.gesto() == DriadeRoble.BRAZOS) {
                this.rightArm.xRot = -2.9F + sway;
                this.leftArm.xRot = -2.9F - sway;
                this.rightArm.zRot = 0.35F;
                this.leftArm.zRot = -0.35F;
                this.head.xRot = -0.35F;
            } else if (e.gesto() == DriadeRoble.ENREDADERA) {
                this.rightArm.xRot = -1.65F;
                this.rightArm.yRot = -0.15F;
                this.leftArm.xRot = 0.4F;
            } else {
                this.leftArm.zRot -= 0.08F + sway;
                this.rightArm.zRot += 0.08F + sway;
            }
            this.hat.copyFrom(this.head);
        }
    }

    /** Ojos de savia que brillan (y la flor del pecho). */
    static class Ojos extends EyesLayer<DriadeRoble, Modelo> {
        Ojos(RenderLayerParent<DriadeRoble, Modelo> parent) {
            super(parent);
        }

        @Override
        public RenderType renderType() {
            return OJOS;
        }
    }
}
