package com.mundosdeaxelis.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mundosdeaxelis.MundosDeAxelis;
import com.mundosdeaxelis.entity.LoboEscarcha;
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

/** Modelo de los lobeznos de escarcha que invoca el Lobo Ártico: cuatro patas, lomo con cristales y ojos que brillan. */
public class LoboEscarchaModel extends EntityModel<LoboEscarcha> {
    private ModelPart mane;
    private ModelPart rump;
    private ModelPart earL;
    private ModelPart earR;
    private ModelPart tailTip;
    private final ModelPart[] spikes = new ModelPart[3];

    public static final ModelLayerLocation LAYER =
            new ModelLayerLocation(new ResourceLocation(MundosDeAxelis.MODID, "lobo_escarcha"), "main");

    private final ModelPart root;
    private final ModelPart body;
    private final ModelPart head;
    private final ModelPart jaw;
    private final ModelPart tail;
    private final ModelPart legFl;
    private final ModelPart legFr;
    private final ModelPart legBl;
    private final ModelPart legBr;
    private final float headY;
    private final float tailX;

    public LoboEscarchaModel(ModelPart root) {
        this.root = root;
        this.body = root.getChild("body");
        this.head = root.getChild("head");
        this.jaw = root.getChild("head").getChild("jaw");
        this.tail = root.getChild("tail");
        this.legFl = root.getChild("leg_fl");
        this.legFr = root.getChild("leg_fr");
        this.legBl = root.getChild("leg_bl");
        this.legBr = root.getChild("leg_br");
        this.headY = this.head.y;
        this.tailX = this.tail.xRot;
        this.mane = this.body.getChild("mane");
        this.rump = this.body.getChild("rump");
        this.earL = this.head.getChild("ear_l");
        this.earR = this.head.getChild("ear_r");
        this.tailTip = this.tail.getChild("tail_tip");
        for (int i = 0; i < 3; i++) {
            this.spikes[i] = this.body.getChild("spike" + i);
        }
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition r = mesh.getRoot();
        PartDefinition bodyPart = r.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-4.5F, -4F, -7F, 9F, 8F, 14F), PartPose.offset(0F, 13F, 1F));
        PartDefinition manePart = bodyPart.addOrReplaceChild("mane", CubeListBuilder.create().texOffs(46, 0).addBox(-5.5F, -5F, -3F, 11F, 10F, 6F), PartPose.offset(0F, 0.5F, -3.5F));
        PartDefinition rumpPart = bodyPart.addOrReplaceChild("rump", CubeListBuilder.create().texOffs(0, 22).addBox(-4F, -3.5F, 0F, 8F, 7F, 5F), PartPose.offset(0F, 0.5F, 5F));
        PartDefinition spike0Part = bodyPart.addOrReplaceChild("spike0", CubeListBuilder.create().texOffs(66, 34).addBox(-0.8F, -4.5F, -0.8F, 1.6F, 4.5F, 1.6F), PartPose.offsetAndRotation(0F, -3.6F, -4.5F, 0.25F, 0F, 0F));
        PartDefinition spike1Part = bodyPart.addOrReplaceChild("spike1", CubeListBuilder.create().texOffs(90, 34).addBox(-0.8F, -3.9F, -0.8F, 1.6F, 3.9F, 1.6F), PartPose.offsetAndRotation(0F, -3.6F, -0.5F, 0.25F, 0F, 0F));
        PartDefinition spike2Part = bodyPart.addOrReplaceChild("spike2", CubeListBuilder.create().texOffs(98, 34).addBox(-0.8F, -3.3F, -0.8F, 1.6F, 3.3F, 1.6F), PartPose.offsetAndRotation(0F, -3.6F, 3.5F, 0.25F, 0F, 0F));
        PartDefinition headPart = r.addOrReplaceChild("head", CubeListBuilder.create().texOffs(80, 0).addBox(-3.5F, -3.5F, -5F, 7F, 7F, 6F), PartPose.offset(0F, 11F, -7.5F));
        PartDefinition snoutPart = headPart.addOrReplaceChild("snout", CubeListBuilder.create().texOffs(94, 22).addBox(-2F, -0.5F, -4F, 4F, 3.5F, 4F), PartPose.offset(0F, 0.5F, -4.5F));
        PartDefinition nosePart = headPart.addOrReplaceChild("nose", CubeListBuilder.create().texOffs(0, 41).addBox(-1F, -0.6F, -1.2F, 2F, 1.4F, 1.4F), PartPose.offset(0F, 0.2F, -8.2F));
        PartDefinition jawPart = headPart.addOrReplaceChild("jaw", CubeListBuilder.create().texOffs(74, 34).addBox(-1.8F, 0F, -3.4F, 3.6F, 1.4F, 3.6F), PartPose.offsetAndRotation(0F, 3.3F, -4.3F, 0.1F, 0F, 0F));
        PartDefinition ear_lPart = headPart.addOrReplaceChild("ear_l", CubeListBuilder.create().texOffs(106, 34).addBox(-1F, -3F, -0.75F, 2F, 3F, 1.5F), PartPose.offsetAndRotation(2.4F, -3.4F, -1F, 0F, 0F, 0.15F));
        PartDefinition ear_rPart = headPart.addOrReplaceChild("ear_r", CubeListBuilder.create().texOffs(114, 34).mirror().addBox(-1F, -3F, -0.75F, 2F, 3F, 1.5F), PartPose.offsetAndRotation(-2.4F, -3.4F, -1F, 0F, 0F, -0.15F));
        PartDefinition fang_lPart = headPart.addOrReplaceChild("fang_l", CubeListBuilder.create().texOffs(8, 41).addBox(-0.4F, 0F, -0.4F, 0.8F, 1.6F, 0.8F), PartPose.offset(1.3F, 3.2F, -8.2F));
        PartDefinition fang_rPart = headPart.addOrReplaceChild("fang_r", CubeListBuilder.create().texOffs(12, 41).addBox(-0.4F, 0F, -0.4F, 0.8F, 1.6F, 0.8F), PartPose.offset(-1.3F, 3.2F, -8.2F));
        PartDefinition tailPart = r.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(58, 22).addBox(-1.5F, 0F, -1.5F, 3F, 9F, 3F), PartPose.offsetAndRotation(0F, 10.5F, 8.5F, 1.15F, 0F, 0F));
        PartDefinition tail_tipPart = tailPart.addOrReplaceChild("tail_tip", CubeListBuilder.create().texOffs(54, 34).addBox(-1.1F, 8.5F, -1.1F, 2.2F, 3.5F, 2.2F), PartPose.offset(0F, 0F, 0F));
        PartDefinition leg_flPart = r.addOrReplaceChild("leg_fl", CubeListBuilder.create().texOffs(70, 22).addBox(-1.5F, 0F, -1.5F, 3F, 8F, 3F), PartPose.offset(3.2F, 16F, -4.5F));
        PartDefinition leg_fl_pawPart = leg_flPart.addOrReplaceChild("leg_fl_paw", CubeListBuilder.create().texOffs(110, 22).addBox(-1.8F, 6.3F, -2.5F, 3.6F, 2F, 4.6F), PartPose.offset(0F, 0F, 0F));
        PartDefinition leg_frPart = r.addOrReplaceChild("leg_fr", CubeListBuilder.create().texOffs(82, 22).addBox(-1.5F, 0F, -1.5F, 3F, 8F, 3F), PartPose.offset(-3.2F, 16F, -4.5F));
        PartDefinition leg_fr_pawPart = leg_frPart.addOrReplaceChild("leg_fr_paw", CubeListBuilder.create().texOffs(0, 34).addBox(-1.8F, 6.3F, -2.5F, 3.6F, 2F, 4.6F), PartPose.offset(0F, 0F, 0F));
        PartDefinition leg_blPart = r.addOrReplaceChild("leg_bl", CubeListBuilder.create().texOffs(26, 22).addBox(-1.7F, 0F, -1.7F, 3.4F, 8F, 3.4F), PartPose.offset(3.2F, 16F, 6.5F));
        PartDefinition leg_bl_pawPart = leg_blPart.addOrReplaceChild("leg_bl_paw", CubeListBuilder.create().texOffs(18, 34).addBox(-2.0F, 6.3F, -2.7F, 4.0F, 2F, 5.0F), PartPose.offset(0F, 0F, 0F));
        PartDefinition leg_brPart = r.addOrReplaceChild("leg_br", CubeListBuilder.create().texOffs(42, 22).addBox(-1.7F, 0F, -1.7F, 3.4F, 8F, 3.4F), PartPose.offset(-3.2F, 16F, 6.5F));
        PartDefinition leg_br_pawPart = leg_brPart.addOrReplaceChild("leg_br_paw", CubeListBuilder.create().texOffs(36, 34).addBox(-2.0F, 6.3F, -2.7F, 4.0F, 2F, 5.0F), PartPose.offset(0F, 0F, 0F));
        return LayerDefinition.create(mesh, 128, 96);
    }

    @Override
    public void setupAnim(LoboEscarcha e, float limbSwing, float limbSwingAmount, float age, float netHeadYaw, float headPitch) {
        float pt = Anim.pt(e, age);
        Motion m = Motion.of(e);
        float walk = Math.min(1.0F, limbSwingAmount);
        // al correr pasa del trote (patas en diagonal) al galope (las de delante juntas, luego las de atrás)
        float gallop = m.smooth(0, walk > 0.7F ? 1.0F : 0.0F, 0.08F, pt);
        float ph = limbSwing * 0.7F;
        float agg = m.smooth(1, e.isAggressive() ? 1.0F : 0.0F, 0.12F, pt);
        float u = Anim.swing(m, e, pt, 11.0F);
        float bite = Anim.jab(u);
        float hurt = Anim.hurt(e, pt);
        float jig = m.jiggle(pt);
        float breath = Anim.breath(age, 0.14F);
        float sway = m.sway(pt), drag = m.drag(pt);
        // patas: trote en diagonal que se convierte en galope; el pie se levanta al adelantar
        float a = 1.1F * walk;
        float fl = Mth.cos(ph) * a;
        float fr = Mth.cos(ph + Anim.PI * (1.0F - gallop * 0.85F)) * a;
        float bl = Mth.cos(ph + Anim.PI * (1.0F - gallop * 0.6F)) * a;
        float br = Mth.cos(ph + gallop * Anim.PI * 0.25F) * a;
        this.legFl.xRot = fl - agg * 0.15F * (1.0F - walk);
        this.legFr.xRot = fr - agg * 0.15F * (1.0F - walk);
        this.legBl.xRot = bl + agg * 0.2F * (1.0F - walk);
        this.legBr.xRot = br + agg * 0.2F * (1.0F - walk);
        this.legFl.y = 16.0F - Math.max(0.0F, Mth.sin(ph)) * 1.2F * walk;
        this.legFr.y = 16.0F - Math.max(0.0F, Mth.sin(ph + Anim.PI * (1.0F - gallop * 0.85F))) * 1.2F * walk;
        this.legBl.y = 16.0F - Math.max(0.0F, Mth.sin(ph + Anim.PI * (1.0F - gallop * 0.6F))) * 1.2F * walk;
        this.legBr.y = 16.0F - Math.max(0.0F, Mth.sin(ph + gallop * Anim.PI * 0.25F)) * 1.2F * walk;
        // lomo: se estira y se encoge con el galope, se agacha al acechar y se lanza al morder
        float flex = Mth.sin(ph) * 0.08F * walk * (0.4F + gallop);
        float crouch = agg * (1.0F - walk) * 1.2F;
        this.body.xRot = flex - bite * 0.12F + hurt * 0.15F;
        this.body.y = 13.0F + crouch + Mth.sin(ph * 2.0F) * 0.5F * walk + breath * 0.15F;
        this.body.z = 1.0F - bite * 1.5F;
        this.body.zRot = jig * 0.06F + sway * 0.12F;
        this.mane.xScale = 1.0F + breath * 0.03F + agg * 0.06F;
        this.mane.yScale = 1.0F + breath * 0.03F + agg * 0.08F;
        this.rump.xRot = -flex * 0.8F;
        // las púas de hielo del lomo se erizan al enfadarse y vibran con los golpes
        for (int i = 0; i < 3; i++) {
            this.spikes[i].yScale = 1.0F + agg * 0.3F + jig * 0.15F;
            this.spikes[i].zRot = Mth.sin(age * 0.9F + i) * 0.04F * agg + jig * 0.1F;
        }
        // cabeza: va baja al acechar, cabecea al trotar, se lanza al morder y se echa atrás al recibir un golpe
        this.head.y = this.headY + crouch * 0.8F + Mth.sin(age * 0.1F) * 0.2F + Mth.sin(ph * 2.0F) * 0.6F * walk;
        this.head.z = -7.5F - bite * 3.0F;
        this.head.xRot = headPitch * Anim.DEG + agg * 0.15F * (1.0F - walk) - bite * 0.35F + hurt * -0.4F + Mth.sin(ph * 2.0F) * 0.05F * walk;
        this.head.yRot = Mth.clamp(netHeadYaw, -45.0F, 45.0F) * Anim.DEG + Anim.idleLook(e, age) * (1.0F - walk) * (1.0F - agg);
        this.head.zRot = jig * 0.12F + Mth.sin(age * 0.05F) * 0.04F * (1.0F - walk);
        // mandíbula: jadea al correr, gruñe enfadado y se cierra de golpe al morder
        float pant = walk * (0.12F + Math.max(0.0F, Mth.sin(age * 0.5F)) * 0.15F);
        float snarl = agg * (0.18F + Mth.sin(age * 0.7F) * 0.05F) * (1.0F - walk);
        float snap = u < 0.0F ? 0.0F : (u < 0.12F ? u / 0.12F * 0.9F : 0.9F * (1.0F - Anim.smooth(Math.min(1.0F, (u - 0.12F) / 0.12F))));
        this.jaw.xRot = 0.1F + pant + snarl + snap + hurt * 0.5F + Math.max(0.0F, Mth.sin(age * 0.05F)) * 0.05F;
        // orejas: atentas al enfadarse, hacia atrás al correr y al recibir un golpe; de vez en cuando se mueven solas
        float flick = Anim.blink(e, age * 0.7F) ? 0.4F : 0.0F;
        this.earL.xRot = -drag * 0.6F - hurt * 0.8F + agg * 0.1F;
        this.earR.xRot = -drag * 0.6F - hurt * 0.8F + agg * 0.1F;
        this.earL.zRot = 0.1F + flick + jig * 0.3F;
        this.earR.zRot = -0.1F - jig * 0.3F;
        // cola: se menea con el trote, se queda atrás en las curvas, se levanta al galopar y se esconde al recibir golpes
        this.tail.xRot = this.tailX - 0.5F * walk - drag * 0.3F + hurt * 0.4F - agg * 0.2F * (1.0F - walk);
        this.tail.yRot = Mth.sin(age * 0.2F) * (0.2F + 0.3F * walk) * (1.0F - agg * 0.7F) + sway * 0.9F;
        this.tail.y = 10.5F + crouch + Mth.sin(ph * 2.0F) * 0.5F * walk;
        this.tailTip.xRot = drag * 0.2F + jig * 0.3F;
    }

    @Override
    public void renderToBuffer(PoseStack pose, VertexConsumer buffer, int light, int overlay, float r, float g, float b, float a) {
        this.root.render(pose, buffer, light, overlay, r, g, b, a);
    }
}
