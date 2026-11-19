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
        PartDefinition bodyPart = r.addOrReplaceChild("body", CubeListBuilder.create().texOffs(32, 0).addBox(-4F, -8F, -2.5F, 8F, 8F, 5F), PartPose.offset(0F, 19F, 0F));
        PartDefinition ventPart = bodyPart.addOrReplaceChild("vent", CubeListBuilder.create().texOffs(8, 45).addBox(-2F, 0F, 0F, 4F, 3F, 2F), PartPose.offset(0F, -6F, 2.5F));
        PartDefinition spine_aPart = bodyPart.addOrReplaceChild("spine_a", CubeListBuilder.create().texOffs(30, 45).addBox(-1F, -3F, 0F, 2F, 3F, 2F), PartPose.offsetAndRotation(0F, -6.5F, 2.5F, 0.55F, 0F, 0F));
        PartDefinition spine_bPart = bodyPart.addOrReplaceChild("spine_b", CubeListBuilder.create().texOffs(38, 45).addBox(-1F, -3F, 0F, 2F, 3F, 2F), PartPose.offsetAndRotation(0F, -3.8F, 2.5F, 0.55F, 0F, 0F));
        PartDefinition spine_cPart = bodyPart.addOrReplaceChild("spine_c", CubeListBuilder.create().texOffs(46, 45).addBox(-1F, -3F, 0F, 2F, 3F, 2F), PartPose.offsetAndRotation(0F, -1.2F, 2.5F, 0.55F, 0F, 0F));
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
        float pt = Anim.pt(e, age);
        Motion m = Motion.of(e);
        float dth = Anim.death(e, pt);
        float walk = Math.min(1.0F, limbSwingAmount);
        float ph = limbSwing * 0.7F;
        float agg = m.smooth(0, e.isAggressive() ? 1.0F : 0.0F, 0.12F, pt);
        float u = Anim.swing(m, e, pt, 13.0F);
        float sm = Anim.smash(u);
        float raise = -sm / 2.7F;
        float hit = Anim.impact(u);
        float hurt = Anim.hurt(e, pt);
        float jig = m.jiggle(pt);
        float breath = Anim.breath(age, 0.12F);
        float sway = m.sway(pt), drag = m.drag(pt);
        // patas cortas: trote rápido, levanta las rodillas
        float swing = Mth.cos(ph) * 1.2F * walk;
        this.legR.xRot = swing;
        this.legL.xRot = -swing;
        this.legR.y = 19.0F - Math.max(0.0F, Mth.sin(ph)) * 1.0F * walk;
        this.legL.y = 19.0F - Math.max(0.0F, -Mth.sin(ph)) * 1.0F * walk;
        // cuerpo (gira desde la cadera): rebota, se encorva al enfadarse, se echa atrás al alzar los puños y adelante al golpear
        float bob = -Anim.bob(limbSwing, walk, 0.7F) * 1.0F + breath * 0.2F * (1.0F - walk);
        float bodyX = 0.12F * walk + agg * 0.22F - raise * 0.25F + hit * 0.45F - hurt * 0.35F + dth * 0.4F;
        float roll = Mth.sin(ph) * 0.08F * walk + jig * 0.08F;
        this.body.y = 19.0F + bob;
        this.body.xRot = bodyX;
        this.body.zRot = roll;
        this.body.yRot = Mth.sin(ph) * 0.12F * walk;
        Anim.follow(this.head, 19.0F + bob, 0.0F, 0.0F, 11.0F + bob, 0.0F, bodyX);
        this.head.yRot = netHeadYaw * Anim.DEG * 0.7F + Anim.idleLook(e, age) * (1.0F - walk) * (1.0F - agg);
        this.head.xRot = headPitch * Anim.DEG * 0.7F - bodyX * 0.6F - hurt * 0.35F + hit * 0.25F + dth * 0.6F;
        this.head.zRot = roll * 0.7F;
        // mandíbula: respira, gruñe enfadado, muerde al golpear y se abre al morir
        this.jaw.xRot = 0.1F + breath * 0.04F + agg * (0.2F + Mth.sin(age * 0.6F) * 0.05F) + hit * 0.7F + raise * 0.3F + hurt * 0.4F + dth * 0.5F;
        this.spikeR.zRot = -0.3F - agg * 0.2F - jig * 0.2F;
        this.spikeL.zRot = 0.3F + agg * 0.2F + jig * 0.2F;
        // brazos: se balancean al trotar; el golpe es un mazazo con los dos puños de escoria
        float armSwing = Mth.cos(ph) * 0.7F * walk * (1.0F - raise);
        Anim.follow(this.armR, 19.0F + bob, 0.0F, -5.5F, 12.5F + bob, 0.0F, bodyX);
        Anim.follow(this.armL, 19.0F + bob, 0.0F, 5.5F, 12.5F + bob, 0.0F, bodyX);
        this.armR.xRot = -0.4F * agg * (1.0F - raise) + sm - armSwing + dth * 0.8F;
        this.armL.xRot = -0.4F * agg * (1.0F - raise) + sm + armSwing + dth * 0.8F;
        this.armR.zRot = 0.06F + Mth.cos(age * 0.09F) * 0.03F + hurt * 0.3F + raise * 0.25F;
        this.armL.zRot = -0.06F - Mth.cos(age * 0.09F) * 0.03F - hurt * 0.3F - raise * 0.25F;
        // púas del lomo: una ola; se erizan con los golpes
        float flare = jig * 0.35F + hurt * 0.4F + agg * 0.1F;
        this.spineA.xRot = 0.55F + Mth.sin(age * 0.3F) * 0.08F - flare;
        this.spineB.xRot = 0.55F + Mth.sin(age * 0.3F - 0.7F) * 0.08F - flare;
        this.spineC.xRot = 0.55F + Mth.sin(age * 0.3F - 1.4F) * 0.08F - flare;
        // la rejilla del lomo late como un fuelle
        this.vent.yScale = 1.0F + breath * 0.12F + agg * 0.1F;
        // cola: sigue al cuerpo, se queda atrás en las curvas y se levanta al correr
        Anim.follow(this.tail, 19.0F + bob, 0.0F, 0.0F, 16.0F + bob, 2.5F, bodyX);
        this.tail.yRot = Mth.sin(ph) * 0.5F * walk + Mth.sin(age * 0.1F) * 0.1F + sway * 0.8F;
        this.tail.xRot = -0.25F + drag * 0.35F + bodyX * 0.5F - hit * 0.3F;
        this.tailTip.yRot = Mth.sin(age * 0.2F - 1.0F) * 0.3F + sway * 0.6F;
        this.tailTip.xRot = drag * 0.2F + jig * 0.3F;
        // gotas de escoria que caen
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
