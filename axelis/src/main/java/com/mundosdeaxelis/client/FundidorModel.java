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
        PartDefinition bodyPart = r.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-6F, -16F, -4F, 12F, 16F, 8F), PartPose.offset(0F, 12F, 0F));
        PartDefinition beltPart = bodyPart.addOrReplaceChild("belt", CubeListBuilder.create().texOffs(56, 39).addBox(-6.5F, 8F, -4.5F, 13F, 3F, 9F), PartPose.offset(0F, -12F, 0F));
        PartDefinition tong_aPart = bodyPart.addOrReplaceChild("tong_a", CubeListBuilder.create().texOffs(96, 52).addBox(-0.5F, 0F, -0.5F, 1F, 9F, 1F), PartPose.offsetAndRotation(-4F, -1F, -4.8F, 0F, 0F, 0.08F));
        PartDefinition tong_bPart = bodyPart.addOrReplaceChild("tong_b", CubeListBuilder.create().texOffs(100, 52).addBox(-0.5F, 0F, -0.5F, 1F, 9F, 1F), PartPose.offsetAndRotation(-3F, -1F, -4.8F, 0F, 0F, -0.08F));
        PartDefinition cruciblePart = bodyPart.addOrReplaceChild("crucible", CubeListBuilder.create().texOffs(64, 24).addBox(-4.5F, -3F, 0F, 9F, 8F, 6F), PartPose.offset(0F, -12F, 4F));
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
        PartDefinition pipePart = bodyPart.addOrReplaceChild("pipe", CubeListBuilder.create().texOffs(100, 39).addBox(-1.5F, -9F, -1.5F, 3F, 9F, 3F), PartPose.offset(3F, -14F, 4.5F));
        PartDefinition pipe2Part = bodyPart.addOrReplaceChild("pipe2", CubeListBuilder.create().texOffs(104, 52).addBox(-1.5F, -6F, -1.5F, 3F, 6F, 3F), PartPose.offset(-3F, -14F, 4.5F));
        return LayerDefinition.create(mesh, 128, 96);
    }

    @Override
    public void setupAnim(Fundidor e, float limbSwing, float limbSwingAmount, float age, float netHeadYaw, float headPitch) {
        float pt = Anim.pt(e, age);
        Motion m = Motion.of(e);
        float dth = Anim.death(e, pt);
        float walk = Math.min(1.0F, limbSwingAmount);
        float ph = limbSwing * 0.55F;
        boolean root = e.isRooting();
        float rooting = m.smooth(0, root ? 1.0F : 0.0F, 0.18F, pt);
        float slam = Motion.pulse(m.sinceEnd(0, root, e, pt), 14.0F);
        float agg = m.smooth(1, e.isAggressive() ? 1.0F : 0.0F, 0.1F, pt);
        float u = Anim.swing(m, e, pt, 18.0F);
        float sm = Anim.smash(u);
        float raise = -sm / 2.7F;
        float hit = Anim.impact(u);
        float hurt = Anim.hurt(e, pt);
        float jig = m.jiggle(pt);
        float breath = Anim.breath(age, 0.08F);
        float sway = m.sway(pt), drag = m.drag(pt);
        // piernas pesadas con las rodillas que suben
        float swing = Mth.cos(ph) * 1.0F * walk;
        this.legR.xRot = swing;
        this.legL.xRot = -swing;
        this.legR.y = 12.0F - Math.max(0.0F, Mth.sin(ph)) * 1.3F * walk;
        this.legL.y = 12.0F - Math.max(0.0F, -Mth.sin(ph)) * 1.3F * walk;
        // cuerpo (desde la cadera): se balancea con cada paso, se arquea al cargar el crisol y se vuelca al golpear
        float bob = -Anim.bob(limbSwing, walk, 0.55F) * 1.3F + breath * 0.35F * (1.0F - walk);
        float bodyX = 0.04F + 0.1F * walk + agg * 0.08F - rooting * 0.25F + slam * 0.42F - raise * 0.18F + hit * 0.35F - hurt * 0.18F;
        float roll = Mth.sin(ph) * 0.06F * walk + jig * 0.05F;
        this.body.y = 12.0F + bob;
        this.body.xRot = bodyX;
        this.body.zRot = roll;
        this.body.yRot = Mth.sin(ph) * 0.08F * walk;
        Anim.follow(this.head, 12.0F + bob, 0.0F, 0.0F, -4.0F + bob, -1.0F, bodyX);
        this.head.yRot = netHeadYaw * Anim.DEG * 0.6F + Anim.idleLook(e, age) * (1.0F - walk) * (1.0F - agg);
        this.head.xRot = headPitch * Anim.DEG * 0.6F - bodyX * 0.5F - hurt * 0.35F + hit * 0.2F + dth * 0.5F;
        this.head.zRot = roll * 0.8F;
        this.brow.y = agg * 0.6F;
        this.hat.y = -jig * 0.6F - hit * 0.8F;
        this.hat.zRot = jig * 0.12F;
        // hombreras y brazos: van sobre el cuerpo
        Anim.follow(this.pauldR, 12.0F + bob, 0.0F, -8.0F, -3.0F + bob, 0.0F, bodyX);
        Anim.follow(this.pauldL, 12.0F + bob, 0.0F, 8.0F, -3.0F + bob, 0.0F, bodyX);
        Anim.follow(this.armR, 12.0F + bob, 0.0F, -9.5F, -1.0F + bob, 0.0F, bodyX);
        Anim.follow(this.armL, 12.0F + bob, 0.0F, 9.5F, -1.0F + bob, 0.0F, bodyX);
        float armSwing = Mth.cos(ph) * 0.6F * walk * (1.0F - raise) * (1.0F - rooting);
        float lift = -2.6F * rooting + Mth.sin(age * 0.9F) * 0.12F * rooting - 0.4F * agg * (1.0F - rooting) * (1.0F - raise);
        this.armR.xRot = lift + sm - armSwing + slam * 2.2F + dth * 0.7F;
        this.armL.xRot = lift + sm + armSwing + slam * 2.2F + dth * 0.7F;
        this.armR.zRot = 0.08F + roll + hurt * 0.3F + rooting * 0.2F + dth * 0.4F;
        this.armL.zRot = -0.08F + roll - hurt * 0.3F - rooting * 0.2F - dth * 0.4F;
        this.pauldR.xRot = bodyX + this.armR.xRot * 0.25F;
        this.pauldL.xRot = bodyX + this.armL.xRot * 0.25F;
        this.pauldR.zRot = roll;
        this.pauldL.zRot = roll;
        // el crisol de la espalda: chapotea con la inercia y se inclina al cargar
        this.crucible.xRot = drag * 0.22F + Mth.sin(age * 0.09F) * 0.02F + rooting * 0.25F - hit * 0.2F + jig * 0.15F;
        this.crucible.zRot = Mth.sin(age * 0.11F) * 0.025F + sway * 0.3F + roll * 1.5F;
        // tubos: soplan humo y vibran al cargar
        float puff = 1.0F + Mth.sin(age * 0.4F) * 0.06F + rooting * (0.12F + Mth.sin(age * 1.3F) * 0.05F) + slam * 0.2F;
        this.pipe.yScale = puff;
        this.pipe2.yScale = 2.0F - puff;
        this.pipe.xRot = Mth.sin(age * 0.15F) * 0.05F + drag * 0.15F + jig * 0.15F;
        this.pipe2.xRot = Mth.sin(age * 0.15F + 1.0F) * 0.05F + drag * 0.15F - jig * 0.15F;
        // tenazas colgando del cinturón: péndulo
        this.tongA.xRot = swing * 0.3F + drag * 0.5F + Mth.sin(age * 0.2F) * 0.03F + jig * 0.3F - bodyX;
        this.tongB.xRot = swing * 0.3F + drag * 0.5F + Mth.sin(age * 0.2F + 0.8F) * 0.03F - jig * 0.3F - bodyX;
        this.tongA.zRot = 0.08F + sway * 0.3F;
        this.tongB.zRot = -0.08F + sway * 0.3F;
    }

    @Override
    public void renderToBuffer(PoseStack pose, VertexConsumer buffer, int light, int overlay, float r, float g, float b, float a) {
        this.root.render(pose, buffer, light, overlay, r, g, b, a);
    }
}
