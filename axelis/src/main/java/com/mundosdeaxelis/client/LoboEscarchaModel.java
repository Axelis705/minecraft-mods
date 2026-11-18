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
        float pt = age - e.tickCount;
        float sw = Mth.cos(limbSwing * 0.7F) * 1.1F * limbSwingAmount;
        this.legFl.xRot = sw;
        this.legBr.xRot = sw;
        this.legFr.xRot = -sw;
        this.legBl.xRot = -sw;
        float atk = Mth.sin(Mth.sqrt(e.getAttackAnim(pt)) * (float) Math.PI);
        this.head.xRot = headPitch * ((float) Math.PI / 180.0F) - atk * 0.5F;
        this.head.yRot = Mth.clamp(netHeadYaw, -45.0F, 45.0F) * ((float) Math.PI / 180.0F);
        this.head.y = this.headY + Mth.sin(age * 0.1F) * 0.2F + limbSwingAmount * Mth.sin(limbSwing * 1.4F) * 0.3F;
        this.jaw.xRot = 0.1F + atk * 0.9F + Math.max(0.0F, Mth.sin(age * 0.05F)) * 0.05F;
        this.tail.xRot = this.tailX - 0.5F * limbSwingAmount;
        this.tail.yRot = Mth.sin(age * 0.2F) * (0.2F + 0.3F * limbSwingAmount);
        this.body.xRot = atk * -0.12F;
    }

    @Override
    public void renderToBuffer(PoseStack pose, VertexConsumer buffer, int light, int overlay, float r, float g, float b, float a) {
        this.root.render(pose, buffer, light, overlay, r, g, b, a);
    }
}
