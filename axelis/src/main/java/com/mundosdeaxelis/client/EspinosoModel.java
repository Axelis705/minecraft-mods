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
    private ModelPart leafL;
    private ModelPart leafR;
    private final ModelPart[] thorns = new ModelPart[5];

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
        this.leafL = this.stem.getChild("leaf_l");
        this.leafR = this.stem.getChild("leaf_r");
        for (int i = 0; i < 5; i++) {
            this.thorns[i] = this.head.getChild("thorn_" + (i + 1));
        }
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
        float pt = Anim.pt(e, age);
        Motion m = Motion.of(e);
        boolean ch = e.isCharging();
        // carga suave; los pétalos se abren con resorte (se pasan un poco y rebotan)
        float charge = m.smooth(0, ch ? 1.0F : 0.0F, 0.2F, pt);
        float open = m.spring(0, ch ? 1.0F : 0.12F, 0.2F, 0.22F, pt);
        // el disparo: justo al soltar la carga
        float shot = Motion.pulse(m.sinceEnd(0, ch, e, pt), 9.0F);
        float hurt = Anim.hurt(e, pt);
        float jig = m.jiggle(pt);
        float dth = Anim.smooth(Anim.death(e, pt));
        float breath = Anim.breath(age, 0.09F);
        float wind = Mth.sin(age * 0.07F + e.getId()) * 0.06F + Mth.sin(age * 0.19F + e.getId() * 0.3F) * 0.025F;
        float tremble = Mth.sin(age * 2.1F) * 0.035F * charge;
        // tallo: se mece con el viento, apunta, se echa atrás al cargar, latigazo al disparar, se dobla con los golpes y se marchita
        this.stem.yRot = netHeadYaw * Anim.DEG * 0.5F + wind * 0.5F;
        this.stem.xRot = wind + headPitch * Anim.DEG * 0.25F - charge * 0.16F + shot * 0.38F - hurt * 0.32F + dth * 1.15F;
        this.stem.zRot = Mth.cos(age * 0.06F + e.getId()) * 0.04F + jig * 0.12F + tremble;
        // cabeza: tiembla al cargar, cabecea al disparar y se infla un poco con cada carga
        this.head.yRot = netHeadYaw * Anim.DEG * 0.5F;
        this.head.xRot = -0.2F * charge + tremble * 1.5F + shot * 0.28F + dth * 0.5F;
        this.head.zRot = jig * 0.18F;
        float puff = 1.0F + breath * 0.025F + charge * 0.07F * (0.6F + 0.4F * Mth.sin(age * 0.9F)) - shot * 0.08F;
        this.head.xScale = puff;
        this.head.yScale = 1.0F + breath * 0.02F + charge * 0.05F + shot * 0.06F;
        this.head.zScale = puff;
        // pétalos: respiran, se abren de golpe al cargar (cada uno con su ritmo), se abren más al disparar y caen al morir
        float o = open + shot * 0.45F + dth * 0.6F;
        this.petalN.xRot = -(o + Mth.sin(age * 0.13F) * 0.04F) * 0.95F;
        this.petalS.xRot = (o + Mth.sin(age * 0.13F + 1.6F) * 0.04F) * 0.95F;
        this.petalE.zRot = -(o + Mth.sin(age * 0.13F + 3.1F) * 0.04F) * 0.95F;
        this.petalW.zRot = (o + Mth.sin(age * 0.13F + 4.7F) * 0.04F) * 0.95F;
        // hojas del tallo: aletean con el viento y los golpes; se caen al morir
        float flutter = wind * 2.2F + jig * 0.45F + hurt * 0.3F;
        this.leafL.zRot = 0.6F + flutter - dth * 0.9F;
        this.leafR.zRot = -0.6F + flutter + dth * 0.9F;
        this.leafL.yRot = Mth.sin(age * 0.11F) * 0.12F;
        this.leafR.yRot = -Mth.sin(age * 0.11F + 1.0F) * 0.12F;
        // espinas: se erizan al cargar, salen disparadas (se encogen) y vuelven a crecer
        for (int i = 0; i < 5; i++) {
            ModelPart t = this.thorns[i];
            float grow = Mth.clamp(1.0F + charge * 0.55F - shot * 0.85F, 0.15F, 1.6F);
            t.yScale = grow;
            t.xScale = 1.0F + charge * 0.15F;
            t.zScale = 1.0F + charge * 0.15F;
            t.xRot = Mth.sin(age * 1.7F + i * 1.3F) * 0.08F * charge;
            t.zRot = Mth.cos(age * 1.5F + i * 0.9F) * 0.08F * charge;
        }
    }

    @Override
    public void renderToBuffer(PoseStack pose, VertexConsumer buffer, int light, int overlay, float r, float g, float b, float a) {
        this.root.render(pose, buffer, light, overlay, r, g, b, a);
    }
}
