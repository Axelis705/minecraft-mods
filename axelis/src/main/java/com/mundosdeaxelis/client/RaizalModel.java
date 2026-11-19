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
    private ModelPart beard;

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
        this.beard = this.head.getChild("beard");
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
        float pt = Anim.pt(e, age);
        Motion m = Motion.of(e);
        float walk = Math.min(1.0F, limbSwingAmount);
        float ph = limbSwing * 0.6F;
        boolean root = e.isRooting();
        float rooting = m.smooth(0, root ? 1.0F : 0.0F, 0.18F, pt);
        float slam = Motion.pulse(m.sinceEnd(0, root, e, pt), 12.0F);
        float agg = m.smooth(1, e.isAggressive() ? 1.0F : 0.0F, 0.1F, pt);
        float u = Anim.swing(m, e, pt, 18.0F);
        float atk = u < 0.0F ? 0.0F : Math.max(0.0F, -Anim.smash(u) / 2.7F);
        float hit = Anim.impact(u);
        float hurt = Anim.hurt(e, pt);
        float jig = m.jiggle(pt);
        float breath = Anim.breath(age, 0.08F);
        float sway = m.sway(pt), drag = m.drag(pt);
        // piernas: zancada pesada, la rodilla sube al adelantar y el pie golpea el suelo
        float swing = Mth.cos(ph) * 1.05F * walk;
        this.legR.xRot = swing;
        this.legL.xRot = -swing;
        this.legR.y = 14.0F - Math.max(0.0F, Mth.sin(ph)) * 1.2F * walk;
        this.legL.y = 14.0F - Math.max(0.0F, -Mth.sin(ph)) * 1.2F * walk;
        // tronco: se balancea de lado a lado con cada paso, respira, se encorva al enfadarse y se arquea al plantar raíces
        float bob = -Anim.bob(limbSwing, walk, 0.6F) * 1.1F + breath * 0.3F * (1.0F - walk);
        float roll = Mth.sin(ph) * 0.07F * walk;
        this.torso.y = 14.0F + bob;
        this.torso.zRot = roll + jig * 0.06F;
        this.torso.xRot = 0.05F + agg * 0.12F - rooting * 0.32F + slam * 0.45F - atk * 0.15F + hit * 0.35F - hurt * 0.2F;
        this.torso.yRot = Mth.sin(ph) * 0.1F * walk + atk * 0.15F;
        // la cabeza va encima del tronco (sigue su inclinación) y mira al objetivo
        Anim.follow(this.head, 14.0F + bob, 0.0F, 0.0F, 3.0F + bob, 0.0F, this.torso.xRot);
        this.head.yRot = netHeadYaw * Anim.DEG * 0.8F + Anim.idleLook(e, age) * (1.0F - walk) * (1.0F - agg);
        this.head.xRot = headPitch * Anim.DEG * 0.8F + agg * 0.15F - hurt * 0.4F + slam * 0.2F;
        this.head.zRot = roll * 0.6F + jig * 0.1F;
        this.beard.xRot = -drag * 0.35F + Mth.sin(age * 0.09F) * 0.06F + jig * 0.25F - Math.max(0.0F, this.torso.xRot) * 0.6F;
        // brazos: cuelgan pesados y se balancean; el golpe es un mazazo con las dos manos; al plantar raíces los alza y los clava
        float armSwing = Mth.cos(ph) * 0.65F * walk;
        float raise = -2.7F * rooting + Mth.sin(age * 0.9F) * 0.1F * rooting - 0.45F * agg * (1.0F - rooting);
        float smash = Anim.smash(u);
        this.armR.xRot = raise - armSwing * (1.0F - Math.abs(smash) * 0.4F) + smash + slam * 2.4F;
        this.armL.xRot = raise + armSwing * (1.0F - Math.abs(smash) * 0.4F) + smash + slam * 2.4F;
        Anim.follow(this.armR, 14.0F + bob, 0.0F, -8.5F, 4.5F + bob, 0.0F, this.torso.xRot);
        Anim.follow(this.armL, 14.0F + bob, 0.0F, 8.5F, 4.5F + bob, 0.0F, this.torso.xRot);
        this.armR.zRot = 0.08F + Mth.cos(age * 0.07F) * 0.02F + hurt * 0.3F + roll + rooting * 0.25F;
        this.armL.zRot = -0.08F - Mth.cos(age * 0.07F) * 0.02F - hurt * 0.3F + roll - rooting * 0.25F;
        // ramas de la espalda: se mecen, se quedan atrás en las curvas y tiemblan con cada golpe
        float wave = Mth.sin(age * 0.1F) * 0.06F;
        float shake = jig * 0.25F + (slam + hit) * Mth.sin(age * 3.0F) * 0.12F;
        this.branchA.zRot = -0.45F + wave + sway * 0.4F + shake;
        this.branchB.zRot = 0.45F - wave + sway * 0.4F - shake;
        this.branchC.xRot = 0.5F + wave + drag * 0.25F + shake;
        this.branchA.xRot = 0.35F + drag * 0.2F + Mth.sin(ph * 2.0F) * 0.05F * walk;
        this.branchB.xRot = 0.35F + drag * 0.2F + Mth.sin(ph * 2.0F + 1.0F) * 0.05F * walk;
    }

    @Override
    public void renderToBuffer(PoseStack pose, VertexConsumer buffer, int light, int overlay, float r, float g, float b, float a) {
        this.root.render(pose, buffer, light, overlay, r, g, b, a);
    }
}
