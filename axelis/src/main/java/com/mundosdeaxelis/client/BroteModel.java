package com.mundosdeaxelis.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mundosdeaxelis.MundosDeAxelis;
import com.mundosdeaxelis.entity.Brote;
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

/** Modelo del Brote: un retoño del bosque con brazos de enredadera, patas de raíz y un brote con hojas en la cabeza. */
public class BroteModel extends EntityModel<Brote> {
    public static final ModelLayerLocation LAYER =
            new ModelLayerLocation(new ResourceLocation(MundosDeAxelis.MODID, "brote"), "main");

    private final ModelPart root;
    private final ModelPart head;
    private final ModelPart armR;
    private final ModelPart armL;
    private final ModelPart legR;
    private final ModelPart legL;
    private final ModelPart leafA;
    private final ModelPart leafB;

    public BroteModel(ModelPart root) {
        this.root = root;
        this.head = root.getChild("head");
        this.armR = root.getChild("arm_r");
        this.armL = root.getChild("arm_l");
        this.legR = root.getChild("leg_r");
        this.legL = root.getChild("leg_l");
        this.leafA = this.head.getChild("leaf_a");
        this.leafB = this.head.getChild("leaf_b");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition r = mesh.getRoot();
        PartDefinition headPart = r.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-4F, -8F, -4F, 8F, 8F, 8F), PartPose.offset(0F, 11F, 0F));
        PartDefinition bodyPart = r.addOrReplaceChild("body", CubeListBuilder.create().texOffs(32, 0).addBox(-4F, 0F, -2.5F, 8F, 8F, 5F), PartPose.offset(0F, 11F, 0F));
        PartDefinition leg_rPart = r.addOrReplaceChild("leg_r", CubeListBuilder.create().texOffs(24, 16).addBox(-1.5F, 0F, -1.5F, 3F, 5F, 3F), PartPose.offset(-2.2F, 19F, 0F));
        PartDefinition leg_lPart = r.addOrReplaceChild("leg_l", CubeListBuilder.create().texOffs(36, 16).addBox(-1.5F, 0F, -1.5F, 3F, 5F, 3F), PartPose.offset(2.2F, 19F, 0F));
        PartDefinition arm_rPart = r.addOrReplaceChild("arm_r", CubeListBuilder.create().texOffs(0, 16).addBox(-1.5F, -1F, -1.5F, 3F, 9F, 3F), PartPose.offset(-5.25F, 12.5F, 0F));
        PartDefinition arm_lPart = r.addOrReplaceChild("arm_l", CubeListBuilder.create().texOffs(12, 16).addBox(-1.5F, -1F, -1.5F, 3F, 9F, 3F), PartPose.offset(5.25F, 12.5F, 0F));
        PartDefinition thorn_rPart = arm_rPart.addOrReplaceChild("thorn_r", CubeListBuilder.create().texOffs(32, 28).addBox(-0.5F, 8F, -0.5F, 1F, 3F, 1F), PartPose.offset(0F, 0F, 0F));
        PartDefinition thorn_lPart = arm_lPart.addOrReplaceChild("thorn_l", CubeListBuilder.create().texOffs(36, 28).addBox(-0.5F, 8F, -0.5F, 1F, 3F, 1F), PartPose.offset(0F, 0F, 0F));
        PartDefinition stemPart = headPart.addOrReplaceChild("stem", CubeListBuilder.create().texOffs(40, 28).addBox(-0.5F, -3F, -0.5F, 1F, 3F, 1F), PartPose.offset(0F, -8F, 0F));
        PartDefinition leaf_aPart = headPart.addOrReplaceChild("leaf_a", CubeListBuilder.create().texOffs(0, 28).addBox(0F, -1F, -1.5F, 5F, 1F, 3F), PartPose.offsetAndRotation(0.5F, -10.5F, 0F, 0F, 0F, -0.45F));
        PartDefinition leaf_bPart = headPart.addOrReplaceChild("leaf_b", CubeListBuilder.create().texOffs(16, 28).addBox(-5F, -1F, -1.5F, 5F, 1F, 3F), PartPose.offsetAndRotation(-0.5F, -10.5F, 0F, 0F, 0F, 0.45F));
        PartDefinition budPart = headPart.addOrReplaceChild("bud", CubeListBuilder.create().texOffs(48, 16).addBox(-1.5F, -2F, -1.5F, 3F, 2F, 3F), PartPose.offset(0F, -10.5F, 0F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(Brote e, float limbSwing, float limbSwingAmount, float age, float netHeadYaw, float headPitch) {
        this.head.yRot = netHeadYaw * ((float) Math.PI / 180.0F);
        this.head.xRot = headPitch * ((float) Math.PI / 180.0F);
        float swing = Mth.cos(limbSwing * 0.8F) * 1.3F * limbSwingAmount;
        this.legR.xRot = swing;
        this.legL.xRot = -swing;
        float raise = e.isAggressive() ? -1.25F : 0.0F;
        float hit = Mth.sin(e.getAttackAnim(0.0F) * (float) Math.PI) * -1.1F;
        this.armR.xRot = raise + hit - swing * 0.4F;
        this.armL.xRot = raise + hit + swing * 0.4F;
        this.armR.zRot = 0.1F + Mth.cos(age * 0.09F) * 0.03F;
        this.armL.zRot = -0.1F - Mth.cos(age * 0.09F) * 0.03F;
        float sway = Mth.sin(age * 0.12F) * 0.12F;
        this.leafA.zRot = -0.45F + sway;
        this.leafB.zRot = 0.45F + sway;
    }

    @Override
    public void renderToBuffer(PoseStack pose, VertexConsumer buffer, int light, int overlay, float r, float g, float b, float a) {
        this.root.render(pose, buffer, light, overlay, r, g, b, a);
    }
}
