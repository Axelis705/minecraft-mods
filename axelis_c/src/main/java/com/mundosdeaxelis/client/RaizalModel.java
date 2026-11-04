package com.mundosdeaxelis.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mundosdeaxelis.MundosDeAxelis;
import com.mundosdeaxelis.entity.Raizal;
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

/** Modelo del Raizal: un gólem de raíces y corteza con hombros de musgo, garras de espina y ramas con hojas en la espalda. */
public class RaizalModel extends EntityModel<Raizal> {
    public static final ModelLayerLocation LAYER =
            new ModelLayerLocation(new ResourceLocation(MundosDeAxelis.MODID, "raizal"), "main");

    private final ModelPart root;
    private final ModelPart head;
    private final ModelPart torso;
    private final ModelPart armR;
    private final ModelPart armL;
    private final ModelPart legR;
    private final ModelPart legL;
    private final ModelPart branchA;
    private final ModelPart branchB;
    private final ModelPart branchC;

    public RaizalModel(ModelPart root) {
        this.root = root;
        this.head = root.getChild("head");
        this.torso = root.getChild("torso");
        this.armR = root.getChild("arm_r");
        this.armL = root.getChild("arm_l");
        this.legR = root.getChild("leg_r");
        this.legL = root.getChild("leg_l");
        this.branchA = root.getChild("torso").getChild("branch_a");
        this.branchB = root.getChild("torso").getChild("branch_b");
        this.branchC = root.getChild("torso").getChild("branch_c");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition r = mesh.getRoot();
        PartDefinition headPart = r.addOrReplaceChild("head", CubeListBuilder.create().texOffs(78, 0).addBox(-4F, -8F, -4F, 8F, 8F, 8F), PartPose.offset(0F, 3F, 0F));
        PartDefinition torsoPart = r.addOrReplaceChild("torso", CubeListBuilder.create().texOffs(40, 0).addBox(-6F, -12F, -3.5F, 12F, 12F, 7F), PartPose.offset(0F, 14F, 0F));
        PartDefinition arm_rPart = r.addOrReplaceChild("arm_r", CubeListBuilder.create().texOffs(0, 0).addBox(-2.5F, -1F, -2.5F, 5F, 15F, 5F), PartPose.offset(-8.5F, 4.5F, 0F));
        PartDefinition arm_lPart = r.addOrReplaceChild("arm_l", CubeListBuilder.create().texOffs(20, 0).addBox(-2.5F, -1F, -2.5F, 5F, 15F, 5F), PartPose.offset(8.5F, 4.5F, 0F));
        PartDefinition fist_rPart = arm_rPart.addOrReplaceChild("fist_r", CubeListBuilder.create().texOffs(64, 36).addBox(-3F, 13F, -3F, 6F, 4F, 6F), PartPose.offset(0F, 0F, 0F));
        PartDefinition fist_lPart = arm_lPart.addOrReplaceChild("fist_l", CubeListBuilder.create().texOffs(88, 36).addBox(-3F, 13F, -3F, 6F, 4F, 6F), PartPose.offset(0F, 0F, 0F));
        PartDefinition claw_r1Part = arm_rPart.addOrReplaceChild("claw_r1", CubeListBuilder.create().texOffs(84, 47).addBox(-2F, 17F, -2.5F, 1F, 4F, 1F), PartPose.offset(0F, 0F, 0F));
        PartDefinition claw_r2Part = arm_rPart.addOrReplaceChild("claw_r2", CubeListBuilder.create().texOffs(88, 47).addBox(1F, 17F, -2.5F, 1F, 4F, 1F), PartPose.offset(0F, 0F, 0F));
        PartDefinition claw_l1Part = arm_lPart.addOrReplaceChild("claw_l1", CubeListBuilder.create().texOffs(92, 47).addBox(-2F, 17F, -2.5F, 1F, 4F, 1F), PartPose.offset(0F, 0F, 0F));
        PartDefinition claw_l2Part = arm_lPart.addOrReplaceChild("claw_l2", CubeListBuilder.create().texOffs(96, 47).addBox(1F, 17F, -2.5F, 1F, 4F, 1F), PartPose.offset(0F, 0F, 0F));
        PartDefinition shoulder_rPart = arm_rPart.addOrReplaceChild("shoulder_r", CubeListBuilder.create().texOffs(8, 36).addBox(-3.5F, -3F, -3.5F, 7F, 3F, 7F), PartPose.offset(0F, 0F, 0F));
        PartDefinition shoulder_lPart = arm_lPart.addOrReplaceChild("shoulder_l", CubeListBuilder.create().texOffs(36, 36).addBox(-3.5F, -3F, -3.5F, 7F, 3F, 7F), PartPose.offset(0F, 0F, 0F));
        PartDefinition leg_rPart = r.addOrReplaceChild("leg_r", CubeListBuilder.create().texOffs(0, 20).addBox(-3F, 0F, -3F, 6F, 10F, 6F), PartPose.offset(-3.2F, 14F, 0F));
        PartDefinition leg_lPart = r.addOrReplaceChild("leg_l", CubeListBuilder.create().texOffs(24, 20).addBox(-3F, 0F, -3F, 6F, 10F, 6F), PartPose.offset(3.2F, 14F, 0F));
        PartDefinition foot_rPart = leg_rPart.addOrReplaceChild("foot_r", CubeListBuilder.create().texOffs(48, 20).addBox(-4F, 8F, -5F, 8F, 2F, 9F), PartPose.offset(0F, 0F, 0F));
        PartDefinition foot_lPart = leg_lPart.addOrReplaceChild("foot_l", CubeListBuilder.create().texOffs(82, 20).addBox(-4F, 8F, -5F, 8F, 2F, 9F), PartPose.offset(0F, 0F, 0F));
        PartDefinition branch_aPart = torsoPart.addOrReplaceChild("branch_a", CubeListBuilder.create().texOffs(116, 20).addBox(-1F, -9F, -1F, 2F, 9F, 2F), PartPose.offsetAndRotation(-4F, -10F, 3.5F, 0.35F, 0F, -0.45F));
        PartDefinition branch_bPart = torsoPart.addOrReplaceChild("branch_b", CubeListBuilder.create().texOffs(0, 36).addBox(-1F, -9F, -1F, 2F, 9F, 2F), PartPose.offsetAndRotation(4F, -10F, 3.5F, 0.35F, 0F, 0.45F));
        PartDefinition branch_cPart = torsoPart.addOrReplaceChild("branch_c", CubeListBuilder.create().texOffs(112, 36).addBox(-1F, -7F, -1F, 2F, 7F, 2F), PartPose.offsetAndRotation(0F, -11F, 3.5F, 0.5F, 0F, 0F));
        PartDefinition leaf_aPart = branch_aPart.addOrReplaceChild("leaf_a", CubeListBuilder.create().texOffs(0, 47).addBox(-3F, -11F, -3F, 6F, 2F, 6F), PartPose.offset(0F, 0F, 0F));
        PartDefinition leaf_bPart = branch_bPart.addOrReplaceChild("leaf_b", CubeListBuilder.create().texOffs(24, 47).addBox(-3F, -11F, -3F, 6F, 2F, 6F), PartPose.offset(0F, 0F, 0F));
        PartDefinition leaf_cPart = branch_cPart.addOrReplaceChild("leaf_c", CubeListBuilder.create().texOffs(48, 47).addBox(-2.5F, -9F, -2.5F, 5F, 2F, 5F), PartPose.offset(0F, 0F, 0F));
        PartDefinition beardPart = headPart.addOrReplaceChild("beard", CubeListBuilder.create().texOffs(68, 47).addBox(-3F, -1F, -4.5F, 6F, 3F, 2F), PartPose.offset(0F, 3F, 0F));
        return LayerDefinition.create(mesh, 128, 64);
    }

    @Override
    public void setupAnim(Raizal e, float limbSwing, float limbSwingAmount, float age, float netHeadYaw, float headPitch) {
        this.head.yRot = netHeadYaw * ((float) Math.PI / 180.0F) * 0.8F;
        this.head.xRot = headPitch * ((float) Math.PI / 180.0F) * 0.8F;
        float swing = Mth.cos(limbSwing * 0.6F) * 1.1F * limbSwingAmount;
        this.legR.xRot = swing;
        this.legL.xRot = -swing;
        float hit = Mth.sin(e.getAttackAnim(0.0F) * (float) Math.PI) * -1.4F;
        float raise = e.isRooting() ? -2.6F + Mth.sin(age * 0.9F) * 0.12F : (e.isAggressive() ? -0.5F : 0.0F);
        this.armR.xRot = raise + hit - swing * 0.6F;
        this.armL.xRot = raise + hit + swing * 0.6F;
        this.armR.zRot = 0.08F + Mth.cos(age * 0.07F) * 0.02F;
        this.armL.zRot = -0.08F - Mth.cos(age * 0.07F) * 0.02F;
        this.torso.xRot = e.isRooting() ? -0.25F : 0.05F;
        float sway = Mth.sin(age * 0.1F) * 0.06F;
        this.branchA.zRot = -0.45F + sway;
        this.branchB.zRot = 0.45F - sway;
        this.branchC.xRot = 0.5F + sway;
    }

    @Override
    public void renderToBuffer(PoseStack pose, VertexConsumer buffer, int light, int overlay, float r, float g, float b, float a) {
        this.root.render(pose, buffer, light, overlay, r, g, b, a);
    }
}
