package com.mundosdeaxelis.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mundosdeaxelis.MundosDeAxelis;
import com.mundosdeaxelis.entity.Capataz;
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

/** Modelo de Capataz (Etapa 2, la Forja). */
public class CapatazModel extends EntityModel<Capataz> {
    public static final ModelLayerLocation LAYER =
            new ModelLayerLocation(new ResourceLocation(MundosDeAxelis.MODID, "capataz"), "main");

    private final ModelPart root;
    private final ModelPart head;
    private final ModelPart brow;
    private final ModelPart cheekR;
    private final ModelPart cheekL;
    private final ModelPart crest;
    private final ModelPart body;
    private final ModelPart belt;
    private final ModelPart buckle;
    private final ModelPart cape;
    private final ModelPart capeTrim;
    private final ModelPart pauldR;
    private final ModelPart pauldL;
    private final ModelPart spikeR;
    private final ModelPart spikeL;
    private final ModelPart legR;
    private final ModelPart legL;
    private final ModelPart kneeR;
    private final ModelPart kneeL;
    private final ModelPart bootR;
    private final ModelPart bootL;
    private final ModelPart armR;
    private final ModelPart armL;
    private final ModelPart bracerR;
    private final ModelPart bracerL;
    private final ModelPart fistR;
    private final ModelPart fistL;
    private final ModelPart handle;
    private final ModelPart hhead;
    private final ModelPart hband;
    private final ModelPart hspikeA;
    private final ModelPart hspikeB;
    private final ModelPart strike;
    private final ModelPart chainA;
    private final ModelPart chainB;

    public CapatazModel(ModelPart root) {
        this.root = root;
        this.head = root.getChild("head");
        this.brow = root.getChild("head").getChild("brow");
        this.cheekR = root.getChild("head").getChild("cheek_r");
        this.cheekL = root.getChild("head").getChild("cheek_l");
        this.crest = root.getChild("head").getChild("crest");
        this.body = root.getChild("body");
        this.belt = root.getChild("belt");
        this.buckle = root.getChild("buckle");
        this.cape = root.getChild("cape");
        this.capeTrim = root.getChild("cape").getChild("cape_trim");
        this.pauldR = root.getChild("pauld_r");
        this.pauldL = root.getChild("pauld_l");
        this.spikeR = root.getChild("pauld_r").getChild("spike_r");
        this.spikeL = root.getChild("pauld_l").getChild("spike_l");
        this.legR = root.getChild("leg_r");
        this.legL = root.getChild("leg_l");
        this.kneeR = root.getChild("leg_r").getChild("knee_r");
        this.kneeL = root.getChild("leg_l").getChild("knee_l");
        this.bootR = root.getChild("leg_r").getChild("boot_r");
        this.bootL = root.getChild("leg_l").getChild("boot_l");
        this.armR = root.getChild("arm_r");
        this.armL = root.getChild("arm_l");
        this.bracerR = root.getChild("arm_r").getChild("bracer_r");
        this.bracerL = root.getChild("arm_l").getChild("bracer_l");
        this.fistR = root.getChild("arm_r").getChild("fist_r");
        this.fistL = root.getChild("arm_l").getChild("fist_l");
        this.handle = root.getChild("arm_r").getChild("handle");
        this.hhead = root.getChild("arm_r").getChild("hhead");
        this.hband = root.getChild("arm_r").getChild("hband");
        this.hspikeA = root.getChild("arm_r").getChild("hspike_a");
        this.hspikeB = root.getChild("arm_r").getChild("hspike_b");
        this.strike = root.getChild("arm_r").getChild("strike");
        this.chainA = root.getChild("belt").getChild("chain_a");
        this.chainB = root.getChild("belt").getChild("chain_b");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition r = mesh.getRoot();
        PartDefinition headPart = r.addOrReplaceChild("head", CubeListBuilder.create().texOffs(40, 25).addBox(-4.5F, -9F, -4.5F, 9F, 9F, 9F), PartPose.offset(0F, -6F, -1F));
        PartDefinition browPart = headPart.addOrReplaceChild("brow", CubeListBuilder.create().texOffs(10, 94).addBox(-5F, -8F, -6.5F, 10F, 2F, 2F), PartPose.offset(0F, 0F, 0F));
        PartDefinition cheek_rPart = headPart.addOrReplaceChild("cheek_r", CubeListBuilder.create().texOffs(0, 84).addBox(-5.5F, -5F, -4.5F, 1F, 5F, 5F), PartPose.offset(0F, 0F, 0F));
        PartDefinition cheek_lPart = headPart.addOrReplaceChild("cheek_l", CubeListBuilder.create().texOffs(12, 84).addBox(4.5F, -5F, -4.5F, 1F, 5F, 5F), PartPose.offset(0F, 0F, 0F));
        PartDefinition crestPart = headPart.addOrReplaceChild("crest", CubeListBuilder.create().texOffs(36, 44).addBox(-1F, -5F, -5F, 2F, 5F, 10F), PartPose.offset(0F, -9F, 0F));
        PartDefinition bodyPart = r.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-7F, -6F, -4.5F, 14F, 16F, 9F), PartPose.offset(0F, 0F, 0F));
        PartDefinition beltPart = r.addOrReplaceChild("belt", CubeListBuilder.create().texOffs(0, 60).addBox(-7F, 0F, -4.5F, 14F, 3F, 9F), PartPose.offset(0F, 8F, 0F));
        PartDefinition bucklePart = r.addOrReplaceChild("buckle", CubeListBuilder.create().texOffs(34, 94).addBox(-2F, 0F, -1F, 4F, 3F, 1F), PartPose.offset(0F, 8F, -4.9F));
        PartDefinition capePart = r.addOrReplaceChild("cape", CubeListBuilder.create().texOffs(54, 0).addBox(-6F, 0F, 0F, 12F, 22F, 1F), PartPose.offset(0F, -5F, 5F));
        PartDefinition cape_trimPart = capePart.addOrReplaceChild("cape_trim", CubeListBuilder.create().texOffs(44, 94).addBox(-6F, 22F, 0F, 12F, 2F, 1F), PartPose.offset(0F, 0F, 0F));
        PartDefinition pauld_rPart = r.addOrReplaceChild("pauld_r", CubeListBuilder.create().texOffs(76, 25).addBox(-7F, -3F, -5.5F, 7F, 5F, 11F), PartPose.offset(-8F, -5F, 0F));
        PartDefinition pauld_lPart = r.addOrReplaceChild("pauld_l", CubeListBuilder.create().texOffs(0, 44).addBox(0F, -3F, -5.5F, 7F, 5F, 11F), PartPose.offset(8F, -5F, 0F));
        PartDefinition spike_rPart = pauld_rPart.addOrReplaceChild("spike_r", CubeListBuilder.create().texOffs(66, 84).addBox(-1F, -5F, -1F, 2F, 5F, 2F), PartPose.offset(-3.5F, -3F, 0F));
        PartDefinition spike_lPart = pauld_lPart.addOrReplaceChild("spike_l", CubeListBuilder.create().texOffs(74, 84).addBox(-1F, -5F, -1F, 2F, 5F, 2F), PartPose.offset(3.5F, -3F, 0F));
        PartDefinition leg_rPart = r.addOrReplaceChild("leg_r", CubeListBuilder.create().texOffs(80, 0).addBox(-3F, 0F, -3F, 6F, 14F, 6F), PartPose.offset(-4F, 10F, 0F));
        PartDefinition leg_lPart = r.addOrReplaceChild("leg_l", CubeListBuilder.create().texOffs(104, 0).addBox(-3F, 0F, -3F, 6F, 14F, 6F), PartPose.offset(4F, 10F, 0F));
        PartDefinition knee_rPart = leg_rPart.addOrReplaceChild("knee_r", CubeListBuilder.create().texOffs(82, 84).addBox(-3.5F, 6F, -4F, 7F, 4F, 2F), PartPose.offset(0F, 0F, 0F));
        PartDefinition knee_lPart = leg_lPart.addOrReplaceChild("knee_l", CubeListBuilder.create().texOffs(100, 84).addBox(-3.5F, 6F, -4F, 7F, 4F, 2F), PartPose.offset(0F, 0F, 0F));
        PartDefinition boot_rPart = leg_rPart.addOrReplaceChild("boot_r", CubeListBuilder.create().texOffs(46, 60).addBox(-3.5F, 10F, -4.5F, 7F, 4F, 8F), PartPose.offset(0F, 0F, 0F));
        PartDefinition boot_lPart = leg_lPart.addOrReplaceChild("boot_l", CubeListBuilder.create().texOffs(76, 60).addBox(-3.5F, 10F, -4.5F, 7F, 4F, 8F), PartPose.offset(0F, 0F, 0F));
        PartDefinition arm_rPart = r.addOrReplaceChild("arm_r", CubeListBuilder.create().texOffs(0, 25).addBox(-2.5F, 0F, -2.5F, 5F, 14F, 5F), PartPose.offset(-10.5F, -4F, 0F));
        PartDefinition arm_lPart = r.addOrReplaceChild("arm_l", CubeListBuilder.create().texOffs(20, 25).addBox(-2.5F, 0F, -2.5F, 5F, 14F, 5F), PartPose.offset(10.5F, -4F, 0F));
        PartDefinition bracer_rPart = arm_rPart.addOrReplaceChild("bracer_r", CubeListBuilder.create().texOffs(74, 72).addBox(-3F, 8F, -3F, 6F, 4F, 6F), PartPose.offset(0F, 0F, 0F));
        PartDefinition bracer_lPart = arm_lPart.addOrReplaceChild("bracer_l", CubeListBuilder.create().texOffs(98, 72).addBox(-3F, 8F, -3F, 6F, 4F, 6F), PartPose.offset(0F, 0F, 0F));
        PartDefinition fist_rPart = arm_rPart.addOrReplaceChild("fist_r", CubeListBuilder.create().texOffs(0, 72).addBox(-3F, 13F, -3F, 6F, 6F, 6F), PartPose.offset(0F, 0F, 0F));
        PartDefinition fist_lPart = arm_lPart.addOrReplaceChild("fist_l", CubeListBuilder.create().texOffs(24, 72).addBox(-3F, 13F, -3F, 6F, 6F, 6F), PartPose.offset(0F, 0F, 0F));
        PartDefinition handlePart = arm_rPart.addOrReplaceChild("handle", CubeListBuilder.create().texOffs(46, 0).addBox(-1F, 10F, -1F, 2F, 22F, 2F), PartPose.offset(0F, 0F, 0F));
        PartDefinition hheadPart = arm_rPart.addOrReplaceChild("hhead", CubeListBuilder.create().texOffs(60, 44).addBox(-5F, 19F, -3.5F, 10F, 7F, 7F), PartPose.offset(0F, 0F, 0F));
        PartDefinition hbandPart = arm_rPart.addOrReplaceChild("hband", CubeListBuilder.create().texOffs(48, 72).addBox(-5.5F, 18.5F, -1F, 11F, 8F, 2F), PartPose.offset(0F, 0F, 0F));
        PartDefinition hspike_aPart = arm_rPart.addOrReplaceChild("hspike_a", CubeListBuilder.create().texOffs(118, 84).addBox(-1F, 21F, -6.5F, 2F, 2F, 3F), PartPose.offset(0F, 0F, 0F));
        PartDefinition hspike_bPart = arm_rPart.addOrReplaceChild("hspike_b", CubeListBuilder.create().texOffs(0, 94).addBox(-1F, 21F, 3.5F, 2F, 2F, 3F), PartPose.offset(0F, 0F, 0F));
        PartDefinition strikePart = arm_rPart.addOrReplaceChild("strike", CubeListBuilder.create().texOffs(28, 84).addBox(-5F, 26F, -3.5F, 10F, 1F, 7F), PartPose.offset(0F, 0F, 0F));
        PartDefinition chain_aPart = beltPart.addOrReplaceChild("chain_a", CubeListBuilder.create().texOffs(24, 84).addBox(-0.5F, 0F, -0.5F, 1F, 9F, 1F), PartPose.offset(-5F, 3F, -4F));
        PartDefinition chain_bPart = beltPart.addOrReplaceChild("chain_b", CubeListBuilder.create().texOffs(62, 84).addBox(-0.5F, 0F, -0.5F, 1F, 7F, 1F), PartPose.offset(5F, 3F, -4F));
        return LayerDefinition.create(mesh, 128, 128);
    }

    @Override
    public void setupAnim(Capataz e, float limbSwing, float limbSwingAmount, float age, float netHeadYaw, float headPitch) {
        float pt = age - e.tickCount;
        float dth = ForgeDeath.progress(e, pt);
        this.head.yRot = netHeadYaw * ((float) Math.PI / 180.0F) * 0.5F;
        this.head.xRot = headPitch * ((float) Math.PI / 180.0F) * 0.5F;
        float swing = Mth.cos(limbSwing * 0.5F) * 0.9F * limbSwingAmount;
        this.legR.xRot = swing;
        this.legL.xRot = -swing;
        float charge = e.slamProgress();
        float lift = -2.7F * charge;
        float hit = Mth.sin(e.getAttackAnim(pt) * (float) Math.PI) * -1.7F;
        float idle = Mth.sin(age * 0.06F) * 0.04F;
        this.armR.xRot = lift + hit - swing * 0.5F + idle + dth * 0.8F;
        this.armL.xRot = lift * 0.9F + hit * 0.6F + swing * 0.5F - idle + dth * 0.8F;
        this.armR.zRot = 0.06F + dth * 0.3F;
        this.armL.zRot = -0.06F - dth * 0.3F;
        this.body.xRot = charge * -0.16F;
        this.head.xRot += charge * 0.1F;
        this.pauldR.xRot = this.armR.xRot * 0.3F;
        this.pauldL.xRot = this.armL.xRot * 0.3F;
        this.chainA.xRot = Mth.sin(age * 0.2F) * 0.12F + swing * 0.3F;
        this.chainB.xRot = -Mth.sin(age * 0.2F) * 0.12F - swing * 0.3F;
        this.cape.xRot = 0.08F + limbSwingAmount * 0.45F + Mth.sin(age * 0.08F) * 0.04F - charge * 0.25F;
        this.capeTrim.xRot = Mth.sin(age * 0.15F) * 0.05F + limbSwingAmount * 0.15F;
    }

    @Override
    public void renderToBuffer(PoseStack pose, VertexConsumer buffer, int light, int overlay, float r, float g, float b, float a) {
        this.root.render(pose, buffer, light, overlay, r, g, b, a);
    }
}
