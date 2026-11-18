package com.mundosdeaxelis.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mundosdeaxelis.MundosDeAxelis;
import com.mundosdeaxelis.entity.GranHerrero;
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

/** Modelo de GranHerrero (Etapa 2, la Forja). */
public class GranHerreroModel extends EntityModel<GranHerrero> {
    public static final ModelLayerLocation LAYER =
            new ModelLayerLocation(new ResourceLocation(MundosDeAxelis.MODID, "gran_herrero"), "main");

    private final ModelPart root;
    private final ModelPart body;
    private final ModelPart belt;
    private final ModelPart buckle;
    private final ModelPart apron;
    private final ModelPart chestL;
    private final ModelPart chestR;
    private final ModelPart collar;
    private final ModelPart head;
    private final ModelPart brow;
    private final ModelPart beard;
    private final ModelPart hornR;
    private final ModelPart hornL;
    private final ModelPart pauldR;
    private final ModelPart pauldL;
    private final ModelPart spikeR;
    private final ModelPart spikeL;
    private final ModelPart flameR;
    private final ModelPart flameL;
    private final ModelPart legR;
    private final ModelPart legL;
    private final ModelPart tassetR;
    private final ModelPart tassetL;
    private final ModelPart kneeR;
    private final ModelPart kneeL;
    private final ModelPart bootR;
    private final ModelPart bootL;
    private final ModelPart armR;
    private final ModelPart cuffR;
    private final ModelPart elbowR;
    private final ModelPart handle;
    private final ModelPart collarH;
    private final ModelPart hhead;
    private final ModelPart bandA;
    private final ModelPart bandB;
    private final ModelPart hspike;
    private final ModelPart strikeA;
    private final ModelPart strikeB;
    private final ModelPart armL;
    private final ModelPart fistL;
    private final ModelPart cuffL;
    private final ModelPart elbowL;
    private final ModelPart anvilBase;
    private final ModelPart anvilWaist;
    private final ModelPart anvilTop;
    private final ModelPart anvilHorn;
    private final ModelPart pipeA;
    private final ModelPart pipeB;
    private final ModelPart chainA;
    private final ModelPart chainB;
    private final ModelPart chainC;
    private final ModelPart lumpA;
    private final ModelPart lumpB;

    public GranHerreroModel(ModelPart root) {
        this.root = root;
        this.body = root.getChild("body");
        this.belt = root.getChild("body").getChild("belt");
        this.buckle = root.getChild("body").getChild("buckle");
        this.apron = root.getChild("body").getChild("apron");
        this.chestL = root.getChild("body").getChild("chest_l");
        this.chestR = root.getChild("body").getChild("chest_r");
        this.collar = root.getChild("body").getChild("collar");
        this.head = root.getChild("head");
        this.brow = root.getChild("head").getChild("brow");
        this.beard = root.getChild("head").getChild("beard");
        this.hornR = root.getChild("head").getChild("horn_r");
        this.hornL = root.getChild("head").getChild("horn_l");
        this.pauldR = root.getChild("pauld_r");
        this.pauldL = root.getChild("pauld_l");
        this.spikeR = root.getChild("pauld_r").getChild("spike_r");
        this.spikeL = root.getChild("pauld_l").getChild("spike_l");
        this.flameR = root.getChild("pauld_r").getChild("spike_r").getChild("flame_r");
        this.flameL = root.getChild("pauld_l").getChild("spike_l").getChild("flame_l");
        this.legR = root.getChild("leg_r");
        this.legL = root.getChild("leg_l");
        this.tassetR = root.getChild("leg_r").getChild("tasset_r");
        this.tassetL = root.getChild("leg_l").getChild("tasset_l");
        this.kneeR = root.getChild("leg_r").getChild("knee_r");
        this.kneeL = root.getChild("leg_l").getChild("knee_l");
        this.bootR = root.getChild("leg_r").getChild("boot_r");
        this.bootL = root.getChild("leg_l").getChild("boot_l");
        this.armR = root.getChild("arm_r");
        this.cuffR = root.getChild("arm_r").getChild("cuff_r");
        this.elbowR = root.getChild("arm_r").getChild("elbow_r");
        this.handle = root.getChild("arm_r").getChild("handle");
        this.collarH = root.getChild("arm_r").getChild("collar_h");
        this.hhead = root.getChild("arm_r").getChild("hhead");
        this.bandA = root.getChild("arm_r").getChild("band_a");
        this.bandB = root.getChild("arm_r").getChild("band_b");
        this.hspike = root.getChild("arm_r").getChild("hspike");
        this.strikeA = root.getChild("arm_r").getChild("strike_a");
        this.strikeB = root.getChild("arm_r").getChild("strike_b");
        this.armL = root.getChild("arm_l");
        this.fistL = root.getChild("arm_l").getChild("fist_l");
        this.cuffL = root.getChild("arm_l").getChild("cuff_l");
        this.elbowL = root.getChild("arm_l").getChild("elbow_l");
        this.anvilBase = root.getChild("body").getChild("anvil_base");
        this.anvilWaist = root.getChild("body").getChild("anvil_waist");
        this.anvilTop = root.getChild("body").getChild("anvil_top");
        this.anvilHorn = root.getChild("body").getChild("anvil_horn");
        this.pipeA = root.getChild("body").getChild("pipe_a");
        this.pipeB = root.getChild("body").getChild("pipe_b");
        this.chainA = root.getChild("body").getChild("chain_a");
        this.chainB = root.getChild("body").getChild("chain_b");
        this.chainC = root.getChild("body").getChild("chain_c");
        this.lumpA = root.getChild("body").getChild("chain_a").getChild("lump_a");
        this.lumpB = root.getChild("body").getChild("chain_b").getChild("lump_b");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition r = mesh.getRoot();
        PartDefinition bodyPart = r.addOrReplaceChild("body", CubeListBuilder.create().texOffs(40, 0).addBox(-12F, 0F, -7F, 24F, 22F, 14F), PartPose.offset(0F, -16F, 0F));
        PartDefinition beltPart = bodyPart.addOrReplaceChild("belt", CubeListBuilder.create().texOffs(48, 67).addBox(-13F, 17F, -8F, 26F, 4F, 16F), PartPose.offset(0F, 0F, 0F));
        PartDefinition bucklePart = bodyPart.addOrReplaceChild("buckle", CubeListBuilder.create().texOffs(166, 136).addBox(-3F, 17F, -8.9F, 6F, 4F, 1F), PartPose.offset(0F, 0F, 0F));
        PartDefinition apronPart = bodyPart.addOrReplaceChild("apron", CubeListBuilder.create().texOffs(96, 89).addBox(-8F, 22F, -9F, 16F, 15F, 2F), PartPose.offset(0F, 0F, 0F));
        PartDefinition chest_lPart = bodyPart.addOrReplaceChild("chest_l", CubeListBuilder.create().texOffs(36, 123).addBox(6.5F, 2F, -8.5F, 6F, 10F, 2F), PartPose.offset(0F, 0F, 0F));
        PartDefinition chest_rPart = bodyPart.addOrReplaceChild("chest_r", CubeListBuilder.create().texOffs(52, 123).addBox(-12.5F, 2F, -8.5F, 6F, 10F, 2F), PartPose.offset(0F, 0F, 0F));
        PartDefinition collarPart = bodyPart.addOrReplaceChild("collar", CubeListBuilder.create().texOffs(132, 107).addBox(-10F, -2F, -5F, 20F, 3F, 10F), PartPose.offset(0F, 0F, 0F));
        PartDefinition headPart = r.addOrReplaceChild("head", CubeListBuilder.create().texOffs(76, 38).addBox(-6F, -11F, -6F, 12F, 11F, 12F), PartPose.offset(0F, -17F, -1F));
        PartDefinition browPart = headPart.addOrReplaceChild("brow", CubeListBuilder.create().texOffs(180, 136).addBox(-6.5F, -9F, -7F, 13F, 2F, 2F), PartPose.offset(0F, 0F, 0F));
        PartDefinition beardPart = headPart.addOrReplaceChild("beard", CubeListBuilder.create().texOffs(204, 123).addBox(-4F, 0F, -7.5F, 8F, 6F, 3F), PartPose.offset(0F, 0F, 0F));
        PartDefinition horn_rPart = headPart.addOrReplaceChild("horn_r", CubeListBuilder.create().texOffs(68, 123).addBox(-1.5F, -9F, -1.5F, 3F, 9F, 3F), PartPose.offsetAndRotation(-5.5F, -9F, 0F, 0F, 0F, -0.45F));
        PartDefinition horn_lPart = headPart.addOrReplaceChild("horn_l", CubeListBuilder.create().texOffs(80, 123).addBox(-1.5F, -9F, -1.5F, 3F, 9F, 3F), PartPose.offsetAndRotation(5.5F, -9F, 0F, 0F, 0F, 0.45F));
        PartDefinition pauld_rPart = r.addOrReplaceChild("pauld_r", CubeListBuilder.create().texOffs(124, 38).addBox(-6F, -4F, -7F, 12F, 8F, 14F), PartPose.offset(-17F, -14F, 0F));
        PartDefinition pauld_lPart = r.addOrReplaceChild("pauld_l", CubeListBuilder.create().texOffs(176, 38).addBox(-6F, -4F, -7F, 12F, 8F, 14F), PartPose.offset(17F, -14F, 0F));
        PartDefinition spike_rPart = pauld_rPart.addOrReplaceChild("spike_r", CubeListBuilder.create().texOffs(172, 123).addBox(-1.5F, -7F, -1.5F, 3F, 7F, 3F), PartPose.offset(-2F, -4F, 0F));
        PartDefinition spike_lPart = pauld_lPart.addOrReplaceChild("spike_l", CubeListBuilder.create().texOffs(184, 123).addBox(-1.5F, -7F, -1.5F, 3F, 7F, 3F), PartPose.offset(2F, -4F, 0F));
        PartDefinition flame_rPart = spike_rPart.addOrReplaceChild("flame_r", CubeListBuilder.create().texOffs(98, 136).addBox(-1.5F, -5F, -1.5F, 3F, 5F, 3F), PartPose.offset(0F, -7F, 0F));
        PartDefinition flame_lPart = spike_lPart.addOrReplaceChild("flame_l", CubeListBuilder.create().texOffs(110, 136).addBox(-1.5F, -5F, -1.5F, 3F, 5F, 3F), PartPose.offset(0F, -7F, 0F));
        PartDefinition leg_rPart = r.addOrReplaceChild("leg_r", CubeListBuilder.create().texOffs(188, 0).addBox(-4.5F, 0F, -4.5F, 9F, 20F, 9F), PartPose.offset(-7F, 4F, 0F));
        PartDefinition leg_lPart = r.addOrReplaceChild("leg_l", CubeListBuilder.create().texOffs(0, 38).addBox(-4.5F, 0F, -4.5F, 9F, 20F, 9F), PartPose.offset(7F, 4F, 0F));
        PartDefinition tasset_rPart = leg_rPart.addOrReplaceChild("tasset_r", CubeListBuilder.create().texOffs(92, 123).addBox(-5F, -3F, -6.5F, 10F, 9F, 2F), PartPose.offset(0F, 0F, 0F));
        PartDefinition tasset_lPart = leg_lPart.addOrReplaceChild("tasset_l", CubeListBuilder.create().texOffs(116, 123).addBox(-5F, -3F, -6.5F, 10F, 9F, 2F), PartPose.offset(0F, 0F, 0F));
        PartDefinition knee_rPart = leg_rPart.addOrReplaceChild("knee_r", CubeListBuilder.create().texOffs(18, 136).addBox(-5.5F, 6F, -7F, 11F, 5F, 3F), PartPose.offset(0F, 0F, 0F));
        PartDefinition knee_lPart = leg_lPart.addOrReplaceChild("knee_l", CubeListBuilder.create().texOffs(46, 136).addBox(-5.5F, 6F, -7F, 11F, 5F, 3F), PartPose.offset(0F, 0F, 0F));
        PartDefinition boot_rPart = leg_rPart.addOrReplaceChild("boot_r", CubeListBuilder.create().texOffs(0, 89).addBox(-5.5F, 15F, -7.5F, 11F, 5F, 13F), PartPose.offset(0F, 0F, 0F));
        PartDefinition boot_lPart = leg_lPart.addOrReplaceChild("boot_l", CubeListBuilder.create().texOffs(48, 89).addBox(-5.5F, 15F, -7.5F, 11F, 5F, 13F), PartPose.offset(0F, 0F, 0F));
        PartDefinition arm_rPart = r.addOrReplaceChild("arm_r", CubeListBuilder.create().texOffs(36, 38).addBox(-5F, 0F, -5F, 10F, 18F, 10F), PartPose.offset(-17F, -16F, 0F));
        PartDefinition cuff_rPart = arm_rPart.addOrReplaceChild("cuff_r", CubeListBuilder.create().texOffs(204, 89).addBox(-6F, 13F, -6F, 12F, 4F, 12F), PartPose.offset(0F, 0F, 0F));
        PartDefinition elbow_rPart = arm_rPart.addOrReplaceChild("elbow_r", CubeListBuilder.create().texOffs(226, 123).addBox(-8F, 5F, -3F, 3F, 3F, 6F), PartPose.offset(0F, 0F, 0F));
        PartDefinition handlePart = arm_rPart.addOrReplaceChild("handle", CubeListBuilder.create().texOffs(116, 107).addBox(-2F, 18F, -2F, 4F, 10F, 4F), PartPose.offset(0F, 0F, 0F));
        PartDefinition collar_hPart = arm_rPart.addOrReplaceChild("collar_h", CubeListBuilder.create().texOffs(74, 136).addBox(-3F, 24F, -3F, 6F, 2F, 6F), PartPose.offset(0F, 0F, 0F));
        PartDefinition hheadPart = arm_rPart.addOrReplaceChild("hhead", CubeListBuilder.create().texOffs(116, 0).addBox(-7F, 26F, -11F, 14F, 13F, 22F), PartPose.offset(0F, 0F, 0F));
        PartDefinition band_aPart = arm_rPart.addOrReplaceChild("band_a", CubeListBuilder.create().texOffs(132, 89).addBox(-7.5F, 25.5F, -6F, 15F, 14F, 3F), PartPose.offset(0F, 0F, 0F));
        PartDefinition band_bPart = arm_rPart.addOrReplaceChild("band_b", CubeListBuilder.create().texOffs(168, 89).addBox(-7.5F, 25.5F, 3F, 15F, 14F, 3F), PartPose.offset(0F, 0F, 0F));
        PartDefinition hspikePart = arm_rPart.addOrReplaceChild("hspike", CubeListBuilder.create().texOffs(154, 136).addBox(-1.5F, 22F, -1.5F, 3F, 4F, 3F), PartPose.offset(0F, 0F, 0F));
        PartDefinition strike_aPart = arm_rPart.addOrReplaceChild("strike_a", CubeListBuilder.create().texOffs(192, 107).addBox(-6F, 27F, -13F, 12F, 11F, 2F), PartPose.offset(0F, 0F, 0F));
        PartDefinition strike_bPart = arm_rPart.addOrReplaceChild("strike_b", CubeListBuilder.create().texOffs(220, 107).addBox(-6F, 27F, 11F, 12F, 11F, 2F), PartPose.offset(0F, 0F, 0F));
        PartDefinition arm_lPart = r.addOrReplaceChild("arm_l", CubeListBuilder.create().texOffs(0, 0).addBox(-5F, 0F, -5F, 10F, 28F, 10F), PartPose.offset(17F, -16F, 0F));
        PartDefinition fist_lPart = arm_lPart.addOrReplaceChild("fist_l", CubeListBuilder.create().texOffs(0, 67).addBox(-6F, 26F, -6F, 12F, 10F, 12F), PartPose.offset(0F, 0F, 0F));
        PartDefinition cuff_lPart = arm_lPart.addOrReplaceChild("cuff_l", CubeListBuilder.create().texOffs(0, 107).addBox(-6F, 20F, -6F, 12F, 4F, 12F), PartPose.offset(0F, 0F, 0F));
        PartDefinition elbow_lPart = arm_lPart.addOrReplaceChild("elbow_l", CubeListBuilder.create().texOffs(0, 136).addBox(5F, 8F, -3F, 3F, 3F, 6F), PartPose.offset(0F, 0F, 0F));
        PartDefinition anvil_basePart = bodyPart.addOrReplaceChild("anvil_base", CubeListBuilder.create().texOffs(68, 107).addBox(-7F, 8F, 7F, 14F, 4F, 10F), PartPose.offset(0F, 0F, 0F));
        PartDefinition anvil_waistPart = bodyPart.addOrReplaceChild("anvil_waist", CubeListBuilder.create().texOffs(0, 123).addBox(-3.5F, 2F, 8.5F, 7F, 6F, 7F), PartPose.offset(0F, 0F, 0F));
        PartDefinition anvil_topPart = bodyPart.addOrReplaceChild("anvil_top", CubeListBuilder.create().texOffs(132, 67).addBox(-12F, -6F, 6F, 24F, 8F, 12F), PartPose.offset(0F, 0F, 0F));
        PartDefinition anvil_hornPart = bodyPart.addOrReplaceChild("anvil_horn", CubeListBuilder.create().texOffs(140, 123).addBox(12F, -5F, 9F, 6F, 5F, 6F), PartPose.offset(0F, 0F, 0F));
        PartDefinition pipe_aPart = bodyPart.addOrReplaceChild("pipe_a", CubeListBuilder.create().texOffs(204, 67).addBox(-2.5F, -14F, -2.5F, 5F, 14F, 5F), PartPose.offset(-8F, -8F, 10F));
        PartDefinition pipe_bPart = bodyPart.addOrReplaceChild("pipe_b", CubeListBuilder.create().texOffs(48, 107).addBox(-2.5F, -10F, -2.5F, 5F, 10F, 5F), PartPose.offset(8F, -8F, 10F));
        PartDefinition chain_aPart = bodyPart.addOrReplaceChild("chain_a", CubeListBuilder.create().texOffs(28, 123).addBox(-1F, 0F, -1F, 2F, 11F, 2F), PartPose.offset(-10F, 21F, -8.5F));
        PartDefinition chain_bPart = bodyPart.addOrReplaceChild("chain_b", CubeListBuilder.create().texOffs(164, 123).addBox(-1F, 0F, -1F, 2F, 9F, 2F), PartPose.offset(10F, 21F, -8.5F));
        PartDefinition chain_cPart = bodyPart.addOrReplaceChild("chain_c", CubeListBuilder.create().texOffs(196, 123).addBox(-1F, 0F, -1F, 2F, 8F, 2F), PartPose.offset(0F, 21F, 9F));
        PartDefinition lump_aPart = chain_aPart.addOrReplaceChild("lump_a", CubeListBuilder.create().texOffs(122, 136).addBox(-2F, 0F, -2F, 4F, 3F, 4F), PartPose.offset(0F, 11F, 0F));
        PartDefinition lump_bPart = chain_bPart.addOrReplaceChild("lump_b", CubeListBuilder.create().texOffs(138, 136).addBox(-2F, 0F, -2F, 4F, 3F, 4F), PartPose.offset(0F, 9F, 0F));
        return LayerDefinition.create(mesh, 256, 192);
    }

    @Override
    public void setupAnim(GranHerrero e, float limbSwing, float limbSwingAmount, float age, float netHeadYaw, float headPitch) {
        float pt = age - e.tickCount;
        float dth = ForgeDeath.progress(e, pt);
        float swing = Mth.cos(limbSwing * 0.45F) * 0.7F * limbSwingAmount;
        this.legR.xRot = swing;
        this.legL.xRot = -swing;
        this.head.yRot = netHeadYaw * ((float) Math.PI / 180.0F) * 0.5F;
        this.head.xRot = headPitch * ((float) Math.PI / 180.0F) * 0.4F;
        int t = e.slamValue();
        float armRx;
        float armLx;
        float bodyX = 0.0F;
        float idle = Mth.sin(age * 0.06F) * 0.04F;
        if (e.pose() == 1) {
            armRx = -2.5F + Mth.sin(age * 0.8F) * 0.06F;
            armLx = -2.5F - Mth.sin(age * 0.8F) * 0.06F;
            bodyX = -0.18F;
            this.head.xRot = -0.35F;
        } else if (t > 0) {
            float p = Mth.clamp(t / (float) GranHerrero.WINDUP, 0.0F, 1.0F);
            float k = p * p * (3.0F - 2.0F * p);
            armRx = -2.95F * k;
            armLx = -2.4F * k;
            bodyX = -0.22F * k;
        } else if (t < 0) {
            float q = Mth.clamp(-t / (float) GranHerrero.RECOVER, 0.0F, 1.0F);
            armRx = 0.95F * q * q;
            armLx = 0.7F * q * q;
            bodyX = 0.3F * q;
        } else {
            float hit = Mth.sin(e.getAttackAnim(pt) * (float) Math.PI) * -1.6F;
            armRx = -swing * 0.5F + hit + idle;
            armLx = swing * 0.5F - idle;
        }
        armRx += dth * 0.9F;
        armLx += dth * 0.9F;
        this.armR.xRot = armRx;
        this.armL.xRot = armLx;
        this.armR.zRot = 0.05F + dth * 0.5F;
        this.armL.zRot = -0.05F - dth * 0.5F;
        float br = Mth.sin(age * 0.08F) * 0.45F * (1.0F - dth);
        this.body.y = -16.0F + br;
        this.head.y = -17.0F + br;
        this.armR.y = -16.0F + br;
        this.armL.y = -16.0F + br;
        this.pauldR.y = -14.0F + br;
        this.pauldL.y = -14.0F + br;
        this.body.xRot = bodyX;
        this.head.xRot += bodyX * -0.6F + dth * 0.6F;
        this.pauldR.xRot = armRx * 0.25F;
        this.pauldL.xRot = armLx * 0.25F;
        this.pipeA.xRot = Mth.sin(age * 0.13F) * 0.05F;
        this.pipeB.xRot = Mth.sin(age * 0.13F + 1.0F) * 0.05F;
        float fk = 1.0F + Mth.sin(age * 0.75F) * 0.22F;
        float fk2 = 1.0F + Mth.sin(age * 0.75F + 1.7F) * 0.22F;
        this.flameR.yScale = fk * (e.isHot() ? 1.0F : 0.35F);
        this.flameL.yScale = fk2 * (e.isHot() ? 1.0F : 0.35F);
        this.flameR.xRot = Mth.sin(age * 0.4F) * 0.12F;
        this.flameL.zRot = Mth.sin(age * 0.45F) * 0.12F;
        this.chainA.xRot = Mth.sin(age * 0.12F) * 0.06F - swing * 0.35F + bodyX * 0.5F;
        this.chainB.xRot = Mth.sin(age * 0.12F + 1.3F) * 0.06F + swing * 0.35F + bodyX * 0.5F;
        this.chainC.xRot = Mth.sin(age * 0.1F + 0.7F) * 0.05F + bodyX * 0.3F;
        this.beard.xRot = Mth.sin(age * 0.09F) * 0.04F - bodyX * 0.5F;
    }

    @Override
    public void renderToBuffer(PoseStack pose, VertexConsumer buffer, int light, int overlay, float r, float g, float b, float a) {
        this.root.render(pose, buffer, light, overlay, r, g, b, a);
    }
}
