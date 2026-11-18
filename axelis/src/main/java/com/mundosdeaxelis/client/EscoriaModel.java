package com.mundosdeaxelis.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mundosdeaxelis.MundosDeAxelis;
import com.mundosdeaxelis.entity.Escoria;
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

/** Modelo de Escoria (Etapa 2, la Forja). */
public class EscoriaModel extends EntityModel<Escoria> {
    public static final ModelLayerLocation LAYER =
            new ModelLayerLocation(new ResourceLocation(MundosDeAxelis.MODID, "escoria"), "main");

    private final ModelPart root;
    private final ModelPart head;
    private final ModelPart jaw;
    private final ModelPart spikeR;
    private final ModelPart spikeL;
    private final ModelPart body;
    private final ModelPart vent;
    private final ModelPart spineA;
    private final ModelPart spineB;
    private final ModelPart spineC;
    private final ModelPart tail;
    private final ModelPart tailTip;
    private final ModelPart legR;
    private final ModelPart legL;
    private final ModelPart armR;
    private final ModelPart armL;
    private final ModelPart crustR;
    private final ModelPart crustL;
    private final ModelPart fistR;
    private final ModelPart fistL;
    private final ModelPart dripA;
    private final ModelPart dripB;

    public EscoriaModel(ModelPart root) {
        this.root = root;
        this.head = root.getChild("head");
        this.jaw = root.getChild("head").getChild("jaw");
        this.spikeR = root.getChild("head").getChild("spike_r");
        this.spikeL = root.getChild("head").getChild("spike_l");
        this.body = root.getChild("body");
        this.vent = root.getChild("body").getChild("vent");
        this.spineA = root.getChild("body").getChild("spine_a");
        this.spineB = root.getChild("body").getChild("spine_b");
        this.spineC = root.getChild("body").getChild("spine_c");
        this.tail = root.getChild("tail");
        this.tailTip = root.getChild("tail").getChild("tail_tip");
        this.legR = root.getChild("leg_r");
        this.legL = root.getChild("leg_l");
        this.armR = root.getChild("arm_r");
        this.armL = root.getChild("arm_l");
        this.crustR = root.getChild("arm_r").getChild("crust_r");
        this.crustL = root.getChild("arm_l").getChild("crust_l");
        this.fistR = root.getChild("arm_r").getChild("fist_r");
        this.fistL = root.getChild("arm_l").getChild("fist_l");
        this.dripA = root.getChild("drip_a");
        this.dripB = root.getChild("drip_b");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition r = mesh.getRoot();
        PartDefinition headPart = r.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-4F, -8F, -4F, 8F, 8F, 8F), PartPose.offset(0F, 11F, 0F));
        PartDefinition jawPart = headPart.addOrReplaceChild("jaw", CubeListBuilder.create().texOffs(32, 37).addBox(-3F, -1F, -5F, 6F, 2F, 5F), PartPose.offset(0F, -1F, -1F));
        PartDefinition spike_rPart = headPart.addOrReplaceChild("spike_r", CubeListBuilder.create().texOffs(54, 37).addBox(-1F, -5F, -1F, 2F, 5F, 2F), PartPose.offsetAndRotation(-2.5F, -8F, 0F, 0F, 0F, -0.3F));
        PartDefinition spike_lPart = headPart.addOrReplaceChild("spike_l", CubeListBuilder.create().texOffs(0, 45).addBox(-1F, -5F, -1F, 2F, 5F, 2F), PartPose.offsetAndRotation(2.5F, -8F, 0F, 0F, 0F, 0.3F));
        PartDefinition bodyPart = r.addOrReplaceChild("body", CubeListBuilder.create().texOffs(32, 0).addBox(-4F, 0F, -2.5F, 8F, 8F, 5F), PartPose.offset(0F, 11F, 0F));
        PartDefinition ventPart = bodyPart.addOrReplaceChild("vent", CubeListBuilder.create().texOffs(8, 45).addBox(-2F, 0F, 0F, 4F, 3F, 2F), PartPose.offset(0F, 2F, 2.5F));
        PartDefinition spine_aPart = bodyPart.addOrReplaceChild("spine_a", CubeListBuilder.create().texOffs(30, 45).addBox(-1F, -3F, 0F, 2F, 3F, 2F), PartPose.offsetAndRotation(0F, 1.5F, 2.5F, 0.55F, 0F, 0F));
        PartDefinition spine_bPart = bodyPart.addOrReplaceChild("spine_b", CubeListBuilder.create().texOffs(38, 45).addBox(-1F, -3F, 0F, 2F, 3F, 2F), PartPose.offsetAndRotation(0F, 4.2F, 2.5F, 0.55F, 0F, 0F));
        PartDefinition spine_cPart = bodyPart.addOrReplaceChild("spine_c", CubeListBuilder.create().texOffs(46, 45).addBox(-1F, -3F, 0F, 2F, 3F, 2F), PartPose.offsetAndRotation(0F, 6.8F, 2.5F, 0.55F, 0F, 0F));
        PartDefinition tailPart = r.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(24, 16).addBox(-1.5F, -1.5F, 0F, 3F, 3F, 6F), PartPose.offset(0F, 16F, 2.5F));
        PartDefinition tail_tipPart = tailPart.addOrReplaceChild("tail_tip", CubeListBuilder.create().texOffs(20, 45).addBox(-1F, -1F, 0F, 2F, 2F, 3F), PartPose.offset(0F, 0F, 6F));
        PartDefinition leg_rPart = r.addOrReplaceChild("leg_r", CubeListBuilder.create().texOffs(42, 16).addBox(-2F, 0F, -2F, 4F, 5F, 4F), PartPose.offset(-2.4F, 19F, 0F));
        PartDefinition leg_lPart = r.addOrReplaceChild("leg_l", CubeListBuilder.create().texOffs(0, 28).addBox(-2F, 0F, -2F, 4F, 5F, 4F), PartPose.offset(2.4F, 19F, 0F));
        PartDefinition arm_rPart = r.addOrReplaceChild("arm_r", CubeListBuilder.create().texOffs(0, 16).addBox(-1.5F, -1F, -1.5F, 3F, 9F, 3F), PartPose.offset(-5.5F, 12.5F, 0F));
        PartDefinition arm_lPart = r.addOrReplaceChild("arm_l", CubeListBuilder.create().texOffs(12, 16).addBox(-1.5F, -1F, -1.5F, 3F, 9F, 3F), PartPose.offset(5.5F, 12.5F, 0F));
        PartDefinition crust_rPart = arm_rPart.addOrReplaceChild("crust_r", CubeListBuilder.create().texOffs(16, 28).addBox(-2.5F, -2.5F, -2.5F, 5F, 3F, 5F), PartPose.offset(0F, 0F, 0F));
        PartDefinition crust_lPart = arm_lPart.addOrReplaceChild("crust_l", CubeListBuilder.create().texOffs(36, 28).addBox(-2.5F, -2.5F, -2.5F, 5F, 3F, 5F), PartPose.offset(0F, 0F, 0F));
        PartDefinition fist_rPart = arm_rPart.addOrReplaceChild("fist_r", CubeListBuilder.create().texOffs(0, 37).addBox(-2F, 8F, -2F, 4F, 4F, 4F), PartPose.offset(0F, 0F, 0F));
        PartDefinition fist_lPart = arm_lPart.addOrReplaceChild("fist_l", CubeListBuilder.create().texOffs(16, 37).addBox(-2F, 8F, -2F, 4F, 4F, 4F), PartPose.offset(0F, 0F, 0F));
        PartDefinition drip_aPart = r.addOrReplaceChild("drip_a", CubeListBuilder.create().texOffs(54, 45).addBox(-0.5F, 0F, -0.5F, 1F, 2F, 1F), PartPose.offset(-2F, 19F, -2F));
        PartDefinition drip_bPart = r.addOrReplaceChild("drip_b", CubeListBuilder.create().texOffs(58, 45).addBox(-0.5F, 0F, -0.5F, 1F, 2F, 1F), PartPose.offset(2.5F, 19F, 1.5F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(Escoria e, float limbSwing, float limbSwingAmount, float age, float netHeadYaw, float headPitch) {
        float pt = age - e.tickCount;
        float dth = ForgeDeath.progress(e, pt);
        this.head.yRot = netHeadYaw * ((float) Math.PI / 180.0F) * 0.7F;
        this.head.xRot = headPitch * ((float) Math.PI / 180.0F) * 0.7F;
        float swing = Mth.cos(limbSwing * 0.7F) * 1.2F * limbSwingAmount;
        this.legR.xRot = swing;
        this.legL.xRot = -swing;
        float atk = Mth.sin(e.getAttackAnim(pt) * (float) Math.PI);
        float raise = e.isAggressive() ? -0.4F : 0.0F;
        this.armR.xRot = raise + atk * -1.5F - swing * 0.7F;
        this.armL.xRot = raise + atk * -1.5F + swing * 0.7F;
        this.armR.zRot = 0.06F + Mth.cos(age * 0.09F) * 0.03F;
        this.armL.zRot = -0.06F - Mth.cos(age * 0.09F) * 0.03F;
        this.body.xRot = limbSwingAmount * 0.3F + dth * 0.5F;
        this.head.xRot += atk * 0.3F - limbSwingAmount * 0.15F + dth * 0.6F;
        this.jaw.xRot = 0.1F + Mth.sin(age * 0.2F) * 0.04F + (e.isAggressive() ? 0.2F : 0.0F) + atk * 0.8F + dth * 0.5F;
        this.spineA.xRot = 0.55F + Mth.sin(age * 0.3F) * 0.08F;
        this.spineB.xRot = 0.55F + Mth.sin(age * 0.3F - 0.7F) * 0.08F;
        this.spineC.xRot = 0.55F + Mth.sin(age * 0.3F - 1.4F) * 0.08F;
        this.tail.yRot = Mth.sin(limbSwing * 0.7F) * 0.5F * limbSwingAmount + Mth.sin(age * 0.1F) * 0.1F;
        this.tail.xRot = -0.25F + limbSwingAmount * 0.2F;
        this.tailTip.yRot = Mth.sin(age * 0.2F - 1.0F) * 0.3F;
        float c1 = (age * 0.045F) % 1.0F;
        float c2 = (age * 0.045F + 0.5F) % 1.0F;
        this.dripA.y = 19.0F + c1 * 4.5F;
        this.dripA.yScale = 1.0F - c1 * 0.7F;
        this.dripB.y = 19.0F + c2 * 4.5F;
        this.dripB.yScale = 1.0F - c2 * 0.7F;
    }

    @Override
    public void renderToBuffer(PoseStack pose, VertexConsumer buffer, int light, int overlay, float r, float g, float b, float a) {
        this.root.render(pose, buffer, light, overlay, r, g, b, a);
    }
}
