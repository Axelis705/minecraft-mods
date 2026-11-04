package com.mundosdeaxelis.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mundosdeaxelis.MundosDeAxelis;
import com.mundosdeaxelis.entity.GuardianMusgo;
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

/** Modelo del Guardián de Musgo: un gólem de piedra cubierto de musgo con un arbolito que le crece en el hombro. */
public class GuardianModel extends EntityModel<GuardianMusgo> {
    public static final ModelLayerLocation LAYER =
            new ModelLayerLocation(new ResourceLocation(MundosDeAxelis.MODID, "guardian_musgo"), "main");

    private final ModelPart root;
    private final ModelPart head;
    private final ModelPart body;
    private final ModelPart armR;
    private final ModelPart armL;
    private final ModelPart legR;
    private final ModelPart legL;

    public GuardianModel(ModelPart root) {
        this.root = root;
        this.head = root.getChild("head");
        this.body = root.getChild("body");
        this.armR = root.getChild("arm_r");
        this.armL = root.getChild("arm_l");
        this.legR = root.getChild("leg_r");
        this.legL = root.getChild("leg_l");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition r = mesh.getRoot();
        PartDefinition headPart = r.addOrReplaceChild("head", CubeListBuilder.create().texOffs(28, 24).addBox(-4.5F, -9F, -4.5F, 9F, 9F, 9F), PartPose.offset(0F, -2F, -1F));
        PartDefinition bodyPart = r.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-8F, -2F, -5F, 16F, 14F, 10F), PartPose.offset(0F, 0F, 0F));
        PartDefinition moss_capePart = r.addOrReplaceChild("moss_cape", CubeListBuilder.create().texOffs(48, 43).addBox(-7F, -3F, -4.5F, 14F, 3F, 9F), PartPose.offset(0F, 0F, 0F));
        PartDefinition leg_rPart = r.addOrReplaceChild("leg_r", CubeListBuilder.create().texOffs(100, 0).addBox(-3.5F, 0F, -3.5F, 7F, 12F, 7F), PartPose.offset(-4.5F, 12F, 0F));
        PartDefinition leg_lPart = r.addOrReplaceChild("leg_l", CubeListBuilder.create().texOffs(0, 24).addBox(-3.5F, 0F, -3.5F, 7F, 12F, 7F), PartPose.offset(4.5F, 12F, 0F));
        PartDefinition arm_rPart = r.addOrReplaceChild("arm_r", CubeListBuilder.create().texOffs(52, 0).addBox(-3F, 0F, -3F, 6F, 14F, 6F), PartPose.offset(-11F, 0F, 0F));
        PartDefinition arm_lPart = r.addOrReplaceChild("arm_l", CubeListBuilder.create().texOffs(76, 0).addBox(-3F, 0F, -3F, 6F, 14F, 6F), PartPose.offset(11F, 0F, 0F));
        PartDefinition fist_rPart = arm_rPart.addOrReplaceChild("fist_r", CubeListBuilder.create().texOffs(64, 24).addBox(-4F, 12F, -4F, 8F, 8F, 8F), PartPose.offset(0F, 0F, 0F));
        PartDefinition fist_lPart = arm_lPart.addOrReplaceChild("fist_l", CubeListBuilder.create().texOffs(96, 24).addBox(-4F, 12F, -4F, 8F, 8F, 8F), PartPose.offset(0F, 0F, 0F));
        PartDefinition trunkPart = r.addOrReplaceChild("trunk", CubeListBuilder.create().texOffs(36, 43).addBox(-1.5F, -10F, -1.5F, 3F, 10F, 3F), PartPose.offset(7F, -2F, 0F));
        PartDefinition crown_aPart = r.addOrReplaceChild("crown_a", CubeListBuilder.create().texOffs(0, 43).addBox(-4.5F, -4F, -4.5F, 9F, 4F, 9F), PartPose.offset(7F, -12F, 0F));
        PartDefinition crown_bPart = r.addOrReplaceChild("crown_b", CubeListBuilder.create().texOffs(94, 43).addBox(-2.5F, -3F, -2.5F, 5F, 3F, 5F), PartPose.offset(7F, -16F, 0F));
        PartDefinition flowerPart = r.addOrReplaceChild("flower", CubeListBuilder.create().texOffs(114, 43).addBox(-1F, -2F, -1F, 2F, 2F, 2F), PartPose.offset(7F, -19F, 0F));
        return LayerDefinition.create(mesh, 128, 128);
    }

    @Override
    public void setupAnim(GuardianMusgo e, float limbSwing, float limbSwingAmount, float age, float netHeadYaw, float headPitch) {
        this.head.yRot = netHeadYaw * ((float) Math.PI / 180.0F) * 0.6F;
        this.head.xRot = headPitch * ((float) Math.PI / 180.0F) * 0.6F;
        float swing = Mth.cos(limbSwing * 0.5F) * 1.0F * limbSwingAmount;
        this.legR.xRot = swing;
        this.legL.xRot = -swing;
        float charge = e.slamProgress();
        float lift = -2.7F * charge;
        float hit = Mth.sin(e.getAttackAnim(0.0F) * (float) Math.PI) * -1.6F;
        float idle = Mth.sin(age * 0.06F) * 0.04F;
        this.armR.xRot = lift + hit - swing * 0.6F + idle;
        this.armL.xRot = lift + hit + swing * 0.6F - idle;
        this.armR.zRot = 0.08F;
        this.armL.zRot = -0.08F;
        this.body.xRot = charge * -0.18F;
    }

    @Override
    public void renderToBuffer(PoseStack pose, VertexConsumer buffer, int light, int overlay, float r, float g, float b, float a) {
        this.root.render(pose, buffer, light, overlay, r, g, b, a);
    }
}
