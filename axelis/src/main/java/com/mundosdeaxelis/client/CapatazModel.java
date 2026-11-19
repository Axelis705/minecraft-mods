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
        PartDefinition bodyPart = r.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-7F, -16F, -4.5F, 14F, 16F, 9F), PartPose.offset(0F, 10F, 0F));
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
        float pt = Anim.pt(e, age);
        Motion m = Motion.of(e);
        float dth = Anim.death(e, pt);
        float walk = Math.min(1.0F, limbSwingAmount);
        float ph = limbSwing * 0.5F;
        float charge = e.slamProgress();
        boolean slamming = charge > 0.0F;
        float ch = m.smooth(0, charge, 0.5F, pt);
        float slam = Motion.pulse(m.sinceEnd(0, slamming, e, pt), 14.0F);
        float agg = m.smooth(1, e.isAggressive() ? 1.0F : 0.0F, 0.1F, pt);
        float u = Anim.swing(m, e, pt, 18.0F);
        float sm = Anim.smash(u);
        float raise = -sm / 2.7F;
        float hit = Anim.impact(u);
        float hurt = Anim.hurt(e, pt);
        float jig = m.jiggle(pt);
        float breath = Anim.breath(age, 0.07F);
        float sway = m.sway(pt), drag = m.drag(pt);
        // marcha de capataz: pasos firmes
        float swing = Mth.cos(ph) * 0.9F * walk;
        this.legR.xRot = swing;
        this.legL.xRot = -swing;
        this.legR.y = 10.0F - Math.max(0.0F, Mth.sin(ph)) * 1.4F * walk;
        this.legL.y = 10.0F - Math.max(0.0F, -Mth.sin(ph)) * 1.4F * walk;
        // cuerpo desde la cadera
        float bob = -Anim.bob(limbSwing, walk, 0.5F) * 1.3F + breath * 0.3F * (1.0F - walk);
        float bodyX = 0.03F + 0.08F * walk + agg * 0.06F - ch * 0.2F + slam * 0.45F - raise * 0.2F + hit * 0.35F - hurt * 0.15F;
        float roll = Mth.sin(ph) * 0.05F * walk + jig * 0.04F + raise * 0.06F;
        this.body.y = 10.0F + bob;
        this.body.xRot = bodyX;
        this.body.zRot = roll;
        this.body.yRot = Mth.sin(ph) * 0.07F * walk - raise * 0.25F + hit * 0.2F;
        Anim.follow(this.belt, 10.0F + bob, 0.0F, 0.0F, 8.0F + bob, 0.0F, bodyX);
        Anim.follow(this.buckle, 10.0F + bob, 0.0F, 0.0F, 8.0F + bob, -4.9F, bodyX);
        this.belt.xRot = bodyX;
        this.buckle.xRot = bodyX;
        this.belt.zRot = roll;
        this.buckle.zRot = roll;
        Anim.follow(this.head, 10.0F + bob, 0.0F, 0.0F, -6.0F + bob, -1.0F, bodyX);
        this.head.yRot = netHeadYaw * Anim.DEG * 0.5F + Anim.idleLook(e, age) * (1.0F - walk) * (1.0F - agg);
        this.head.xRot = headPitch * Anim.DEG * 0.5F - bodyX * 0.5F + ch * 0.1F - hurt * 0.3F + hit * 0.2F + dth * 0.5F;
        this.head.zRot = roll;
        this.crest.zRot = jig * 0.2F + Mth.sin(age * 0.1F) * 0.03F;
        this.crest.xRot = -drag * 0.15F;
        // brazos: el derecho lleva el martillo (mazazo largo y pesado), el izquierdo acompaña; con la carga los alza los dos
        Anim.follow(this.pauldR, 10.0F + bob, 0.0F, -8.0F, -5.0F + bob, 0.0F, bodyX);
        Anim.follow(this.pauldL, 10.0F + bob, 0.0F, 8.0F, -5.0F + bob, 0.0F, bodyX);
        Anim.follow(this.armR, 10.0F + bob, 0.0F, -10.5F, -4.0F + bob, 0.0F, bodyX);
        Anim.follow(this.armL, 10.0F + bob, 0.0F, 10.5F, -4.0F + bob, 0.0F, bodyX);
        float lift = -2.7F * ch + Mth.sin(age * 1.7F) * 0.06F * ch;
        float idle = Mth.sin(age * 0.06F) * 0.04F;
        float armSwing = Mth.cos(ph) * 0.5F * walk * (1.0F - raise);
        this.armR.xRot = lift + sm - armSwing + idle + slam * 1.0F + dth * 0.8F - agg * 0.25F * (1.0F - ch) * (1.0F - raise);
        this.armL.xRot = lift * 0.9F + sm * 0.45F + armSwing - idle + slam * 0.8F + dth * 0.8F;
        this.armR.zRot = 0.06F + roll + hurt * 0.25F + ch * 0.15F + dth * 0.3F;
        this.armL.zRot = -0.06F + roll - hurt * 0.25F - ch * 0.15F - dth * 0.3F;
        this.pauldR.xRot = bodyX + this.armR.xRot * 0.3F;
        this.pauldL.xRot = bodyX + this.armL.xRot * 0.3F;
        this.pauldR.zRot = roll;
        this.pauldL.zRot = roll;
        // cadenas del cinturón
        this.chainA.xRot = Mth.sin(age * 0.2F) * 0.12F + swing * 0.3F + drag * 0.4F + jig * 0.3F - bodyX;
        this.chainB.xRot = -Mth.sin(age * 0.2F) * 0.12F - swing * 0.3F + drag * 0.4F - jig * 0.3F - bodyX;
        // capa: va sobre la espalda, se levanta al andar, ondea y se queda atrás en las curvas; el ribete va a destiempo
        Anim.follow(this.cape, 10.0F + bob, 0.0F, 0.0F, -5.0F + bob, 5.0F, bodyX);
        float wave = Mth.sin(age * 0.08F) * 0.04F + Mth.sin(limbSwing * 1.0F) * 0.06F * walk;
        this.cape.xRot = Math.max(bodyX + 0.04F, 0.08F + drag * 0.7F + wave - ch * 0.25F + slam * 0.3F + jig * 0.1F);
        this.cape.zRot = sway * 0.25F + roll;
        this.cape.yRot = -sway * 0.15F;
        this.capeTrim.xRot = Mth.sin(age * 0.15F - 0.8F) * 0.06F + drag * 0.25F + jig * 0.15F;
    }

    @Override
    public void renderToBuffer(PoseStack pose, VertexConsumer buffer, int light, int overlay, float r, float g, float b, float a) {
        this.root.render(pose, buffer, light, overlay, r, g, b, a);
    }
}
