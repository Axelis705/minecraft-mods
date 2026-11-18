package com.mundosdeaxelis.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mundosdeaxelis.MundosDeAxelis;
import com.mundosdeaxelis.entity.Espinoso;
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

/** Modelo del Espinoso: una flor carnívora clavada en el suelo; abre los pétalos justo antes de disparar una espina. */
public class EspinosoModel extends EntityModel<Espinoso> {
    public static final ModelLayerLocation LAYER =
            new ModelLayerLocation(new ResourceLocation(MundosDeAxelis.MODID, "espinoso"), "main");

    private final ModelPart root;
    private final ModelPart stem;
    private final ModelPart head;
    private final ModelPart petalN;
    private final ModelPart petalS;
    private final ModelPart petalE;
    private final ModelPart petalW;

    public EspinosoModel(ModelPart root) {
        this.root = root;
        this.stem = root.getChild("stem");
        this.head = this.stem.getChild("head");
        this.petalN = this.head.getChild("petal_n");
        this.petalS = this.head.getChild("petal_s");
        this.petalE = this.head.getChild("petal_e");
        this.petalW = this.head.getChild("petal_w");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition r = mesh.getRoot();
        PartDefinition moundPart = r.addOrReplaceChild("mound", CubeListBuilder.create().texOffs(0, 15).addBox(-5F, 0F, -5F, 10F, 3F, 10F), PartPose.offset(0F, 21F, 0F));
        PartDefinition stemPart = r.addOrReplaceChild("stem", CubeListBuilder.create().texOffs(40, 15).addBox(-1F, -10F, -1F, 2F, 10F, 2F), PartPose.offset(0F, 21F, 0F));
        PartDefinition leaf_lPart = stemPart.addOrReplaceChild("leaf_l", CubeListBuilder.create().texOffs(4, 42).addBox(-4F, 0F, -1F, 4F, 1F, 2F), PartPose.offsetAndRotation(-1F, 16F, 0F, 0F, 0F, 0.6F));
        PartDefinition leaf_rPart = stemPart.addOrReplaceChild("leaf_r", CubeListBuilder.create().texOffs(16, 42).addBox(0F, 0F, -1F, 4F, 1F, 2F), PartPose.offsetAndRotation(1F, 13F, 0F, 0F, 0F, -0.6F));
        PartDefinition headPart = stemPart.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-4F, -7F, -4F, 8F, 7F, 8F), PartPose.offset(0F, -10F, 0F));
        PartDefinition petal_nPart = headPart.addOrReplaceChild("petal_n", CubeListBuilder.create().texOffs(0, 36).addBox(-3.5F, 0F, -5F, 7F, 1F, 5F), PartPose.offset(0F, -1F, -3.5F));
        PartDefinition petal_sPart = headPart.addOrReplaceChild("petal_s", CubeListBuilder.create().texOffs(24, 36).addBox(-3.5F, 0F, 0F, 7F, 1F, 5F), PartPose.offset(0F, -1F, 3.5F));
        PartDefinition petal_ePart = headPart.addOrReplaceChild("petal_e", CubeListBuilder.create().texOffs(0, 28).addBox(0F, 0F, -3.5F, 5F, 1F, 7F), PartPose.offset(3.5F, -1F, 0F));
        PartDefinition petal_wPart = headPart.addOrReplaceChild("petal_w", CubeListBuilder.create().texOffs(24, 28).addBox(-5F, 0F, -3.5F, 5F, 1F, 7F), PartPose.offset(-3.5F, -1F, 0F));
        PartDefinition thorn_1Part = headPart.addOrReplaceChild("thorn_1", CubeListBuilder.create().texOffs(48, 36).addBox(-0.5F, -3F, -0.5F, 1F, 3F, 1F), PartPose.offset(-2.5F, -7F, -2.5F));
        PartDefinition thorn_2Part = headPart.addOrReplaceChild("thorn_2", CubeListBuilder.create().texOffs(52, 36).addBox(-0.5F, -3F, -0.5F, 1F, 3F, 1F), PartPose.offset(2.5F, -7F, -2.5F));
        PartDefinition thorn_3Part = headPart.addOrReplaceChild("thorn_3", CubeListBuilder.create().texOffs(56, 36).addBox(-0.5F, -3F, -0.5F, 1F, 3F, 1F), PartPose.offset(-2.5F, -7F, 2.5F));
        PartDefinition thorn_4Part = headPart.addOrReplaceChild("thorn_4", CubeListBuilder.create().texOffs(60, 36).addBox(-0.5F, -3F, -0.5F, 1F, 3F, 1F), PartPose.offset(2.5F, -7F, 2.5F));
        PartDefinition thorn_5Part = headPart.addOrReplaceChild("thorn_5", CubeListBuilder.create().texOffs(0, 42).addBox(-0.5F, -3F, -0.5F, 1F, 3F, 1F), PartPose.offset(0F, -7F, 0F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(Espinoso e, float limbSwing, float limbSwingAmount, float age, float netHeadYaw, float headPitch) {
        this.stem.yRot = netHeadYaw * ((float) Math.PI / 180.0F) * 0.5F;
        this.stem.xRot = Mth.sin(age * 0.07F) * 0.05F + headPitch * ((float) Math.PI / 180.0F) * 0.25F;
        this.head.yRot = netHeadYaw * ((float) Math.PI / 180.0F) * 0.5F;
        float open = e.isCharging() ? 1.0F : 0.12F + 0.05F * Mth.sin(age * 0.1F);
        this.petalN.xRot = -open * 0.95F;
        this.petalS.xRot = open * 0.95F;
        this.petalE.zRot = -open * 0.95F;
        this.petalW.zRot = open * 0.95F;
        if (e.isCharging()) {
            this.head.xRot = -0.18F + Mth.sin(age * 1.6F) * 0.03F;
        } else {
            this.head.xRot = 0.0F;
        }
    }

    @Override
    public void renderToBuffer(PoseStack pose, VertexConsumer buffer, int light, int overlay, float r, float g, float b, float a) {
        this.root.render(pose, buffer, light, overlay, r, g, b, a);
    }
}
