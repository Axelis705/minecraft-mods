package com.mundosdeaxelis.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mundosdeaxelis.MundosDeAxelis;
import com.mundosdeaxelis.entity.Chispero;
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

/** Modelo de Chispero (Etapa 2, la Forja). */
public class ChisperoModel extends EntityModel<Chispero> {
    public static final ModelLayerLocation LAYER =
            new ModelLayerLocation(new ResourceLocation(MundosDeAxelis.MODID, "chispero"), "main");

    private final ModelPart root;
    private final ModelPart pad;
    private final ModelPart body;
    private final ModelPart doorL;
    private final ModelPart doorR;
    private final ModelPart nozzle;
    private final ModelPart chimney;
    private final ModelPart rim;
    private final ModelPart stack;
    private final ModelPart bellowL;
    private final ModelPart bellowR;
    private final ModelPart gauge;
    private final ModelPart brow;

    public ChisperoModel(ModelPart root) {
        this.root = root;
        this.pad = root.getChild("pad");
        this.body = root.getChild("body");
        this.doorL = root.getChild("body").getChild("door_l");
        this.doorR = root.getChild("body").getChild("door_r");
        this.nozzle = root.getChild("body").getChild("nozzle");
        this.chimney = root.getChild("body").getChild("chimney");
        this.rim = root.getChild("body").getChild("rim");
        this.stack = root.getChild("body").getChild("stack");
        this.bellowL = root.getChild("body").getChild("bellow_l");
        this.bellowR = root.getChild("body").getChild("bellow_r");
        this.gauge = root.getChild("body").getChild("gauge");
        this.brow = root.getChild("body").getChild("brow");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition r = mesh.getRoot();
        PartDefinition padPart = r.addOrReplaceChild("pad", CubeListBuilder.create().texOffs(40, 0).addBox(-6F, -3F, -6F, 12F, 3F, 12F), PartPose.offset(0F, 24F, 0F));
        PartDefinition bodyPart = r.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-5F, -12F, -5F, 10F, 12F, 10F), PartPose.offset(0F, 21F, 0F));
        PartDefinition door_lPart = bodyPart.addOrReplaceChild("door_l", CubeListBuilder.create().texOffs(72, 22).addBox(-5F, -3.5F, -0.5F, 5F, 7F, 1F), PartPose.offset(4.7F, -4.5F, -5.6F));
        PartDefinition door_rPart = bodyPart.addOrReplaceChild("door_r", CubeListBuilder.create().texOffs(84, 22).addBox(0F, -3.5F, -0.5F, 5F, 7F, 1F), PartPose.offset(-4.7F, -4.5F, -5.6F));
        PartDefinition nozzlePart = bodyPart.addOrReplaceChild("nozzle", CubeListBuilder.create().texOffs(52, 22).addBox(-2F, -2F, -6F, 4F, 4F, 6F), PartPose.offset(0F, -10F, -5F));
        PartDefinition chimneyPart = bodyPart.addOrReplaceChild("chimney", CubeListBuilder.create().texOffs(36, 22).addBox(-2F, -8F, -2F, 4F, 8F, 4F), PartPose.offset(2F, -12F, 2F));
        PartDefinition rimPart = bodyPart.addOrReplaceChild("rim", CubeListBuilder.create().texOffs(22, 36).addBox(-3F, -1F, -3F, 6F, 1F, 6F), PartPose.offset(2F, -20F, 2F));
        PartDefinition stackPart = bodyPart.addOrReplaceChild("stack", CubeListBuilder.create().texOffs(0, 36).addBox(-1.5F, -5F, -1.5F, 3F, 5F, 3F), PartPose.offset(-3F, -12F, 3F));
        PartDefinition bellow_lPart = bodyPart.addOrReplaceChild("bellow_l", CubeListBuilder.create().texOffs(0, 22).addBox(0F, -4F, -3F, 3F, 8F, 6F), PartPose.offset(5F, -6F, 0F));
        PartDefinition bellow_rPart = bodyPart.addOrReplaceChild("bellow_r", CubeListBuilder.create().texOffs(18, 22).addBox(-3F, -4F, -3F, 3F, 8F, 6F), PartPose.offset(-5F, -6F, 0F));
        PartDefinition gaugePart = bodyPart.addOrReplaceChild("gauge", CubeListBuilder.create().texOffs(12, 36).addBox(0F, -2F, -2F, 1F, 4F, 4F), PartPose.offset(5.4F, -3F, 3F));
        PartDefinition browPart = bodyPart.addOrReplaceChild("brow", CubeListBuilder.create().texOffs(46, 36).addBox(-4F, -1F, -1F, 8F, 2F, 2F), PartPose.offset(0F, -8F, -5F));
        return LayerDefinition.create(mesh, 96, 64);
    }

    @Override
    public void setupAnim(Chispero e, float limbSwing, float limbSwingAmount, float age, float netHeadYaw, float headPitch) {
        float pt = age - e.tickCount;
        float dth = ForgeDeath.progress(e, pt);
        boolean ch = e.isCharging();
        float op = Mth.lerp(pt, e.openO, e.open);
        this.doorL.yRot = -op * 1.45F;
        this.doorR.yRot = op * 1.45F;
        this.nozzle.z = -5.0F - op * 1.0F + e.recoil * 2.5F;
        this.nozzle.xRot = ch ? Mth.sin(age * 1.6F) * 0.03F : 0.0F;
        this.bellowL.zRot = Mth.sin(age * (ch ? 0.9F : 0.25F)) * 0.18F;
        this.bellowR.zRot = -this.bellowL.zRot;
        this.chimney.xRot = ch ? Mth.sin(age * 1.4F) * 0.04F : 0.0F;
        this.stack.xRot = Mth.sin(age * 0.5F) * 0.02F;
        this.body.yRot = netHeadYaw * ((float) Math.PI / 180.0F) * 0.35F;
        this.body.zRot = ch ? Mth.sin(age * 2.1F) * 0.012F : 0.0F;
    }

    @Override
    public void renderToBuffer(PoseStack pose, VertexConsumer buffer, int light, int overlay, float r, float g, float b, float a) {
        this.root.render(pose, buffer, light, overlay, r, g, b, a);
    }
}
