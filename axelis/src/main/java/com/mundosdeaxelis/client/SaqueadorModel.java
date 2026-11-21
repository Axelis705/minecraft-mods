package com.mundosdeaxelis.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mundosdeaxelis.MundosDeAxelis;
import com.mundosdeaxelis.entity.Saqueador;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Modelo del Saqueador del Tesoro (etapa 276): un bicho bajito de piel verde grisácea, orejas largas y nariz ganchuda, con un
 * saco enorme a la espalda lleno de monedas. Corre inclinado hacia delante, con el saco dando botes y las orejas al viento.
 */
public class SaqueadorModel extends EntityModel<Saqueador> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(new ResourceLocation(MundosDeAxelis.MODID, "saqueador"), "main");

    private final ModelPart root, body, head, earL, earR, sack, legL, legR, armL, armR;

    public SaqueadorModel(ModelPart root) {
        this.root = root;
        this.body = root.getChild("body");
        this.head = body.getChild("head");
        this.earL = head.getChild("ear_l");
        this.earR = head.getChild("ear_r");
        this.sack = body.getChild("sack");
        this.armL = body.getChild("arm_l");
        this.armR = body.getChild("arm_r");
        this.legL = root.getChild("leg_l");
        this.legR = root.getChild("leg_r");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition r = mesh.getRoot();
        PartDefinition body = r.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 10).addBox(-3F, -6F, -2F, 6F, 6F, 4F),
                PartPose.offset(0F, 20F, 0F));
        PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-3F, -5F, -2.5F, 6F, 5F, 5F)
                .texOffs(22, 0).addBox(-0.5F, -2.8F, -4F, 1F, 2F, 1.5F), PartPose.offset(0F, -6F, -0.5F));
        head.addOrReplaceChild("ear_l", CubeListBuilder.create().texOffs(28, 0).addBox(0F, -1F, -0.5F, 4F, 2F, 1F),
                PartPose.offsetAndRotation(3F, -3.5F, 0F, 0F, -0.3F, -0.35F));
        head.addOrReplaceChild("ear_r", CubeListBuilder.create().texOffs(28, 4).addBox(-4F, -1F, -0.5F, 4F, 2F, 1F),
                PartPose.offsetAndRotation(-3F, -3.5F, 0F, 0F, 0.3F, 0.35F));
        PartDefinition sack = body.addOrReplaceChild("sack", CubeListBuilder.create().texOffs(0, 20).addBox(-3.5F, -7F, 0F, 7F, 7F, 5F)
                .texOffs(24, 20).addBox(-1F, -9F, 1.5F, 2F, 2F, 2F)
                .texOffs(32, 20).addBox(-2.5F, -7.8F, 0.8F, 5F, 1F, 3F), PartPose.offset(0F, -1F, 2F));
        body.addOrReplaceChild("arm_l", CubeListBuilder.create().texOffs(28, 10).addBox(-1F, -1F, -1F, 2F, 5F, 2F), PartPose.offset(4F, -5F, 0F));
        body.addOrReplaceChild("arm_r", CubeListBuilder.create().texOffs(28, 10).addBox(-1F, -1F, -1F, 2F, 5F, 2F), PartPose.offset(-4F, -5F, 0F));
        r.addOrReplaceChild("leg_l", CubeListBuilder.create().texOffs(20, 10).addBox(-1F, 0F, -1F, 2F, 4F, 2F), PartPose.offset(1.6F, 20F, 0F));
        r.addOrReplaceChild("leg_r", CubeListBuilder.create().texOffs(20, 10).addBox(-1F, 0F, -1F, 2F, 4F, 2F), PartPose.offset(-1.6F, 20F, 0F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(Saqueador e, float limbSwing, float amount, float age, float yawHead, float pitch) {
        float run = Math.min(1.0F, amount * 1.6F);
        float sp = limbSwing * 1.3F;
        head.yRot = yawHead * Mth.DEG_TO_RAD * 0.6F;
        head.xRot = pitch * Mth.DEG_TO_RAD * 0.5F;
        body.xRot = 0.35F * run;
        body.y = 20.0F - Math.abs(Mth.sin(sp)) * 1.2F * run;
        legL.xRot = Mth.cos(sp) * 1.3F * amount;
        legR.xRot = Mth.cos(sp + Mth.PI) * 1.3F * amount;
        armL.xRot = Mth.cos(sp + Mth.PI) * 1.1F * amount - 0.3F * run;
        armR.xRot = Mth.cos(sp) * 1.1F * amount - 0.3F * run;
        sack.xRot = 0.15F + Mth.sin(sp * 2.0F) * 0.18F * run + Mth.sin(age * 0.08F) * 0.03F;
        float flap = Mth.sin(age * 0.6F) * 0.25F * run;
        earL.zRot = -0.35F + flap + Mth.sin(age * 0.1F) * 0.05F;
        earR.zRot = 0.35F - flap - Mth.sin(age * 0.1F) * 0.05F;
        earL.yRot = -0.3F - 0.5F * run;
        earR.yRot = 0.3F + 0.5F * run;
    }

    @Override
    public void renderToBuffer(PoseStack pose, VertexConsumer vc, int light, int overlay, float r, float g, float b, float a) {
        root.render(pose, vc, light, overlay, r, g, b, a);
    }
}
