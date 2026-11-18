package com.mundosdeaxelis.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mundosdeaxelis.MundosDeAxelis;
import com.mundosdeaxelis.entity.Fundidor;
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

/** Modelo de Fundidor (Etapa 2, la Forja). */
public class FundidorModel extends EntityModel<Fundidor> {
    public static final ModelLayerLocation LAYER =
            new ModelLayerLocation(new ResourceLocation(MundosDeAxelis.MODID, "fundidor"), "main");

    private final ModelPart root;
    private final ModelPart head;
    private final ModelPart brow;
    private final ModelPart hat;
    private final ModelPart body;
    private final ModelPart belt;
    private final ModelPart tongA;
    private final ModelPart tongB;
    private final ModelPart crucible;
    private final ModelPart pauldR;
    private final ModelPart pauldL;
    private final ModelPart legR;
    private final ModelPart legL;
    private final ModelPart shinR;
    private final ModelPart shinL;
    private final ModelPart armR;
    private final ModelPart armL;
    private final ModelPart bracerR;
    private final ModelPart bracerL;
    private final ModelPart fistR;
    private final ModelPart fistL;
    private final ModelPart pipe;
    private final ModelPart pipe2;

    public FundidorModel(ModelPart root) {
        this.root = root;
        this.head = root.getChild("head");
        this.brow = root.getChild("head").getChild("brow");
        this.hat = root.getChild("head").getChild("hat");
        this.body = root.getChild("body");
        this.belt = root.getChild("body").getChild("belt");
        this.tongA = root.getChild("body").getChild("tong_a");
        this.tongB = root.getChild("body").getChild("tong_b");
        this.crucible = root.getChild("body").getChild("crucible");
        this.pauldR = root.getChild("pauld_r");
        this.pauldL = root.getChild("pauld_l");
        this.legR = root.getChild("leg_r");
        this.legL = root.getChild("leg_l");
        this.shinR = root.getChild("leg_r").getChild("shin_r");
        this.shinL = root.getChild("leg_l").getChild("shin_l");
        this.armR = root.getChild("arm_r");
        this.armL = root.getChild("arm_l");
        this.bracerR = root.getChild("arm_r").getChild("bracer_r");
        this.bracerL = root.getChild("arm_l").getChild("bracer_l");
        this.fistR = root.getChild("arm_r").getChild("fist_r");
        this.fistL = root.getChild("arm_l").getChild("fist_l");
        this.pipe = root.getChild("body").getChild("pipe");
        this.pipe2 = root.getChild("body").getChild("pipe2");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition r = mesh.getRoot();
        PartDefinition headPart = r.addOrReplaceChild("head", CubeListBuilder.create().texOffs(94, 24).addBox(-3.5F, -7F, -3.5F, 7F, 7F, 7F), PartPose.offset(0F, -4F, -1F));
        PartDefinition browPart = headPart.addOrReplaceChild("brow", CubeListBuilder.create().texOffs(20, 62).addBox(-4F, -6F, -4.5F, 8F, 1F, 2F), PartPose.offset(0F, 0F, 0F));
        PartDefinition hatPart = headPart.addOrReplaceChild("hat", CubeListBuilder.create().texOffs(0, 62).addBox(-2.5F, -10F, -2.5F, 5F, 3F, 5F), PartPose.offset(0F, 0F, 0F));
        PartDefinition bodyPart = r.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-6F, -4F, -4F, 12F, 16F, 8F), PartPose.offset(0F, 0F, 0F));
        PartDefinition beltPart = bodyPart.addOrReplaceChild("belt", CubeListBuilder.create().texOffs(56, 39).addBox(-6.5F, 8F, -4.5F, 13F, 3F, 9F), PartPose.offset(0F, 0F, 0F));
        PartDefinition tong_aPart = bodyPart.addOrReplaceChild("tong_a", CubeListBuilder.create().texOffs(96, 52).addBox(-0.5F, 0F, -0.5F, 1F, 9F, 1F), PartPose.offsetAndRotation(-4F, 11F, -4.8F, 0F, 0F, 0.08F));
        PartDefinition tong_bPart = bodyPart.addOrReplaceChild("tong_b", CubeListBuilder.create().texOffs(100, 52).addBox(-0.5F, 0F, -0.5F, 1F, 9F, 1F), PartPose.offsetAndRotation(-3F, 11F, -4.8F, 0F, 0F, -0.08F));
        PartDefinition cruciblePart = bodyPart.addOrReplaceChild("crucible", CubeListBuilder.create().texOffs(64, 24).addBox(-4.5F, -3F, 0F, 9F, 8F, 6F), PartPose.offset(0F, 0F, 4F));
        PartDefinition pauld_rPart = r.addOrReplaceChild("pauld_r", CubeListBuilder.create().texOffs(0, 24).addBox(-6F, -3F, -5F, 6F, 5F, 10F), PartPose.offset(-8F, -3F, 0F));
        PartDefinition pauld_lPart = r.addOrReplaceChild("pauld_l", CubeListBuilder.create().texOffs(32, 24).addBox(0F, -3F, -5F, 6F, 5F, 10F), PartPose.offset(8F, -3F, 0F));
        PartDefinition leg_rPart = r.addOrReplaceChild("leg_r", CubeListBuilder.create().texOffs(80, 0).addBox(-2.5F, 0F, -2.5F, 5F, 12F, 5F), PartPose.offset(-3.5F, 12F, 0F));
        PartDefinition leg_lPart = r.addOrReplaceChild("leg_l", CubeListBuilder.create().texOffs(100, 0).addBox(-2.5F, 0F, -2.5F, 5F, 12F, 5F), PartPose.offset(3.5F, 12F, 0F));
        PartDefinition shin_rPart = leg_rPart.addOrReplaceChild("shin_r", CubeListBuilder.create().texOffs(0, 52).addBox(-3F, 6F, -3F, 6F, 4F, 6F), PartPose.offset(0F, 0F, 0F));
        PartDefinition shin_lPart = leg_lPart.addOrReplaceChild("shin_l", CubeListBuilder.create().texOffs(24, 52).addBox(-3F, 6F, -3F, 6F, 4F, 6F), PartPose.offset(0F, 0F, 0F));
        PartDefinition arm_rPart = r.addOrReplaceChild("arm_r", CubeListBuilder.create().texOffs(40, 0).addBox(-2.5F, 0F, -2.5F, 5F, 13F, 5F), PartPose.offset(-9.5F, -1F, 0F));
        PartDefinition arm_lPart = r.addOrReplaceChild("arm_l", CubeListBuilder.create().texOffs(60, 0).addBox(-2.5F, 0F, -2.5F, 5F, 13F, 5F), PartPose.offset(9.5F, -1F, 0F));
        PartDefinition bracer_rPart = arm_rPart.addOrReplaceChild("bracer_r", CubeListBuilder.create().texOffs(48, 52).addBox(-3F, 7F, -3F, 6F, 4F, 6F), PartPose.offset(0F, 0F, 0F));
        PartDefinition bracer_lPart = arm_lPart.addOrReplaceChild("bracer_l", CubeListBuilder.create().texOffs(72, 52).addBox(-3F, 7F, -3F, 6F, 4F, 6F), PartPose.offset(0F, 0F, 0F));
        PartDefinition fist_rPart = arm_rPart.addOrReplaceChild("fist_r", CubeListBuilder.create().texOffs(0, 39).addBox(-3.5F, 12F, -3.5F, 7F, 6F, 7F), PartPose.offset(0F, 0F, 0F));
        PartDefinition fist_lPart = arm_lPart.addOrReplaceChild("fist_l", CubeListBuilder.create().texOffs(28, 39).addBox(-3.5F, 12F, -3.5F, 7F, 6F, 7F), PartPose.offset(0F, 0F, 0F));
        PartDefinition pipePart = bodyPart.addOrReplaceChild("pipe", CubeListBuilder.create().texOffs(100, 39).addBox(-1.5F, -9F, -1.5F, 3F, 9F, 3F), PartPose.offset(3F, -2F, 4.5F));
        PartDefinition pipe2Part = bodyPart.addOrReplaceChild("pipe2", CubeListBuilder.create().texOffs(104, 52).addBox(-1.5F, -6F, -1.5F, 3F, 6F, 3F), PartPose.offset(-3F, -2F, 4.5F));
        return LayerDefinition.create(mesh, 128, 96);
    }

    @Override
    public void setupAnim(Fundidor e, float limbSwing, float limbSwingAmount, float age, float netHeadYaw, float headPitch) {
        float pt = age - e.tickCount;
        float dth = ForgeDeath.progress(e, pt);
        this.head.yRot = netHeadYaw * ((float) Math.PI / 180.0F) * 0.6F;
        this.head.xRot = headPitch * ((float) Math.PI / 180.0F) * 0.6F;
        float swing = Mth.cos(limbSwing * 0.55F) * 1.0F * limbSwingAmount;
        this.legR.xRot = swing;
        this.legL.xRot = -swing;
        float hit = Mth.sin(e.getAttackAnim(pt) * (float) Math.PI) * -1.5F;
        float raise = e.isRooting() ? -2.6F + Mth.sin(age * 0.9F) * 0.12F : (e.isAggressive() ? -0.4F : 0.0F);
        this.armR.xRot = raise + hit - swing * 0.6F + dth * 0.7F;
        this.armL.xRot = raise + hit + swing * 0.6F + dth * 0.7F;
        this.armR.zRot = 0.08F + dth * 0.4F;
        this.armL.zRot = -0.08F - dth * 0.4F;
        this.body.xRot = e.isRooting() ? -0.22F : 0.04F + limbSwingAmount * 0.1F;
        this.head.xRot += dth * 0.5F;
        this.pipe.xRot = Mth.sin(age * 0.15F) * 0.05F;
        this.pipe2.xRot = Mth.sin(age * 0.15F + 1.0F) * 0.05F;
        this.crucible.zRot = Mth.sin(age * 0.11F) * 0.025F + swing * 0.04F;
        this.crucible.xRot = e.isRooting() ? 0.25F : Mth.sin(age * 0.09F) * 0.02F;
        this.tongA.xRot = swing * 0.3F + Mth.sin(age * 0.2F) * 0.03F;
        this.tongB.xRot = swing * 0.3F + Mth.sin(age * 0.2F + 0.8F) * 0.03F;
    }

    @Override
    public void renderToBuffer(PoseStack pose, VertexConsumer buffer, int light, int overlay, float r, float g, float b, float a) {
        this.root.render(pose, buffer, light, overlay, r, g, b, a);
    }
}
