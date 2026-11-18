package com.mundosdeaxelis.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mundosdeaxelis.MundosDeAxelis;
import com.mundosdeaxelis.entity.Polinizador;
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

/** Modelo del Polinizador: un insecto-flor con cuatro alas, collar de pétalos alrededor de la cara y un aguijón de espora. */
public class PolinizadorModel extends EntityModel<Polinizador> {
    public static final ModelLayerLocation LAYER =
            new ModelLayerLocation(new ResourceLocation(MundosDeAxelis.MODID, "polinizador"), "main");

    private final ModelPart root;
    private final ModelPart body;
    private final ModelPart head;
    private final ModelPart tail;
    private final ModelPart wingR1;
    private final ModelPart wingR2;
    private final ModelPart wingL1;
    private final ModelPart wingL2;
    private final ModelPart antennaL;
    private final ModelPart antennaR;

    public PolinizadorModel(ModelPart root) {
        this.root = root;
        this.body = root.getChild("body");
        this.head = root.getChild("body").getChild("head");
        this.tail = root.getChild("body").getChild("tail");
        this.wingR1 = root.getChild("body").getChild("wing_r1");
        this.wingR2 = root.getChild("body").getChild("wing_r2");
        this.wingL1 = root.getChild("body").getChild("wing_l1");
        this.wingL2 = root.getChild("body").getChild("wing_l2");
        this.antennaL = root.getChild("body").getChild("head").getChild("antenna_l");
        this.antennaR = root.getChild("body").getChild("head").getChild("antenna_r");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition r = mesh.getRoot();
        PartDefinition bodyPart = r.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-3F, -3F, -4.5F, 6F, 6F, 9F), PartPose.offset(0F, 16F, 0F));
        PartDefinition headPart = bodyPart.addOrReplaceChild("head", CubeListBuilder.create().texOffs(30, 0).addBox(-2.5F, -2.5F, -5F, 5F, 5F, 5F), PartPose.offset(0F, 0F, -4.5F));
        PartDefinition antenna_lPart = headPart.addOrReplaceChild("antenna_l", CubeListBuilder.create().texOffs(40, 24).addBox(-0.5F, -4F, -0.5F, 1F, 4F, 1F), PartPose.offsetAndRotation(1.5F, -2.5F, -3F, -0.5F, 0F, 0.3F));
        PartDefinition antenna_rPart = headPart.addOrReplaceChild("antenna_r", CubeListBuilder.create().texOffs(44, 24).addBox(-0.5F, -4F, -0.5F, 1F, 4F, 1F), PartPose.offsetAndRotation(-1.5F, -2.5F, -3F, -0.5F, 0F, -0.3F));
        PartDefinition petal_upPart = headPart.addOrReplaceChild("petal_up", CubeListBuilder.create().texOffs(36, 29).addBox(-2F, -5.5F, -5.5F, 4F, 3F, 1F), PartPose.offset(0F, 0F, 0F));
        PartDefinition petal_dnPart = headPart.addOrReplaceChild("petal_dn", CubeListBuilder.create().texOffs(46, 29).addBox(-2F, 2.5F, -5.5F, 4F, 3F, 1F), PartPose.offset(0F, 0F, 0F));
        PartDefinition petal_lPart = headPart.addOrReplaceChild("petal_l", CubeListBuilder.create().texOffs(24, 24).addBox(2.5F, -2F, -5.5F, 3F, 4F, 1F), PartPose.offset(0F, 0F, 0F));
        PartDefinition petal_rPart = headPart.addOrReplaceChild("petal_r", CubeListBuilder.create().texOffs(32, 24).addBox(-5.5F, -2F, -5.5F, 3F, 4F, 1F), PartPose.offset(0F, 0F, 0F));
        PartDefinition tailPart = bodyPart.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(0, 15).addBox(-2F, -2F, 0F, 4F, 4F, 5F), PartPose.offset(0F, 0F, 4.5F));
        PartDefinition stingPart = tailPart.addOrReplaceChild("sting", CubeListBuilder.create().texOffs(56, 29).addBox(-0.5F, -0.5F, 5F, 1F, 1F, 3F), PartPose.offset(0F, 0F, 0F));
        PartDefinition pollenPart = bodyPart.addOrReplaceChild("pollen", CubeListBuilder.create().texOffs(18, 15).addBox(-1.5F, 2.5F, -1.5F, 3F, 3F, 3F), PartPose.offset(0F, 0F, 0F));
        PartDefinition wing_r1Part = bodyPart.addOrReplaceChild("wing_r1", CubeListBuilder.create().texOffs(30, 15).addBox(0F, -0.5F, -2F, 8F, 1F, 4F), PartPose.offsetAndRotation(2.5F, -3F, -1.5F, 0F, 0F, -0.3F));
        PartDefinition wing_r2Part = bodyPart.addOrReplaceChild("wing_r2", CubeListBuilder.create().texOffs(0, 29).addBox(0F, -0.5F, -1.5F, 6F, 1F, 3F), PartPose.offsetAndRotation(2.5F, -3F, 2F, 0F, 0F, -0.2F));
        PartDefinition wing_l1Part = bodyPart.addOrReplaceChild("wing_l1", CubeListBuilder.create().texOffs(0, 24).addBox(-8F, -0.5F, -2F, 8F, 1F, 4F), PartPose.offsetAndRotation(-2.5F, -3F, -1.5F, 0F, 0F, 0.3F));
        PartDefinition wing_l2Part = bodyPart.addOrReplaceChild("wing_l2", CubeListBuilder.create().texOffs(18, 29).addBox(-6F, -0.5F, -1.5F, 6F, 1F, 3F), PartPose.offsetAndRotation(-2.5F, -3F, 2F, 0F, 0F, 0.2F));
        PartDefinition leg_aPart = bodyPart.addOrReplaceChild("leg_a", CubeListBuilder.create().texOffs(0, 33).addBox(-0.5F, 0F, -0.5F, 1F, 3F, 1F), PartPose.offset(-1.5F, 3F, -1.5F));
        PartDefinition leg_bPart = bodyPart.addOrReplaceChild("leg_b", CubeListBuilder.create().texOffs(4, 33).addBox(-0.5F, 0F, -0.5F, 1F, 3F, 1F), PartPose.offset(1.5F, 3F, -1.5F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(Polinizador e, float limbSwing, float limbSwingAmount, float age, float netHeadYaw, float headPitch) {
        float flap = Mth.sin(age * 1.9F) * 0.7F;
        this.wingR1.zRot = -0.3F - flap;
        this.wingR2.zRot = -0.2F - flap * 0.8F;
        this.wingL1.zRot = 0.3F + flap;
        this.wingL2.zRot = 0.2F + flap * 0.8F;
        this.body.y = 16.0F + Mth.sin(age * 0.2F) * 1.2F;
        this.body.xRot = e.isCharging() ? -0.5F : 0.1F + Mth.sin(age * 0.15F) * 0.05F;
        this.head.xRot = headPitch * ((float) Math.PI / 180.0F) * 0.5F;
        this.head.yRot = netHeadYaw * ((float) Math.PI / 180.0F) * 0.7F;
        this.tail.xRot = 0.35F + Mth.sin(age * 0.25F) * 0.1F + (e.isCharging() ? 0.5F : 0.0F);
        this.antennaL.xRot = -0.5F + Mth.sin(age * 0.3F) * 0.1F;
        this.antennaR.xRot = -0.5F - Mth.sin(age * 0.3F) * 0.1F;
    }

    @Override
    public void renderToBuffer(PoseStack pose, VertexConsumer buffer, int light, int overlay, float r, float g, float b, float a) {
        this.root.render(pose, buffer, light, overlay, r, g, b, a);
    }
}
