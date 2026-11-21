package com.mundosdeaxelis.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mundosdeaxelis.MundosDeAxelis;
import com.mundosdeaxelis.entity.Mimico;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Modelo del Mímico (etapa 272): el cuerpo de un cofre del santuario con la tapa como mandíbula. Dentro esconde dientes arriba
 * y abajo, una lengua y dos ojos en el techo de la tapa; al despertar saca cuatro patitas y se levanta. Textura de 64×64.
 */
public class MimicoModel extends EntityModel<Mimico> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(new ResourceLocation(MundosDeAxelis.MODID, "mimico"), "main");

    private final ModelPart root;
    private final ModelPart base;
    private final ModelPart lid;
    private final ModelPart tongue;
    private final ModelPart[] legs = new ModelPart[4];

    public MimicoModel(ModelPart root) {
        this.root = root;
        this.base = root.getChild("base");
        this.lid = base.getChild("lid");
        this.tongue = base.getChild("tongue");
        for (int i = 0; i < 4; i++) {
            legs[i] = root.getChild("leg" + i);
        }
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition r = mesh.getRoot();
        PartDefinition base = r.addOrReplaceChild("base", CubeListBuilder.create().texOffs(0, 0).addBox(-7F, -9F, -6F, 14F, 9F, 12F),
                PartPose.offset(0F, 24F, 0F));
        // dientes de abajo (dentro del cofre cerrado: solo se ven al abrir)
        CubeListBuilder lower = CubeListBuilder.create().texOffs(52, 6);
        for (float x : new float[]{-5.5F, -2.75F, 0.0F, 2.75F, 5.5F}) {
            lower.addBox(x - 0.75F, -11F, -5.7F, 1.5F, 2F, 1F);
        }
        lower.addBox(-6.6F, -10.5F, -2.5F, 1F, 1.5F, 1.5F).addBox(5.6F, -10.5F, -2.5F, 1F, 1.5F, 1.5F);
        base.addOrReplaceChild("teeth_low", lower, PartPose.ZERO);
        base.addOrReplaceChild("tongue", CubeListBuilder.create().texOffs(0, 37).addBox(-2.5F, -0.5F, -8F, 5F, 1F, 8F),
                PartPose.offset(0F, -9.6F, 3.5F));
        // la tapa: gira en la bisagra de atrás
        PartDefinition lid = base.addOrReplaceChild("lid", CubeListBuilder.create().texOffs(0, 21).addBox(-7F, -4F, -12F, 14F, 4F, 12F),
                PartPose.offset(0F, -9F, 6F));
        lid.addOrReplaceChild("lock", CubeListBuilder.create().texOffs(52, 0).addBox(-1.5F, -2F, -12.8F, 3F, 4F, 1F), PartPose.ZERO);
        CubeListBuilder upper = CubeListBuilder.create().texOffs(52, 6);
        for (float x : new float[]{-4.25F, -1.4F, 1.4F, 4.25F}) {
            upper.addBox(x - 0.75F, 0F, -11.7F, 1.5F, 2F, 1F);
        }
        upper.addBox(-6.6F, 0F, -9F, 1F, 1.5F, 1.5F).addBox(5.6F, 0F, -9F, 1F, 1.5F, 1.5F)
                .addBox(-6.6F, 0F, -5.5F, 1F, 1.5F, 1.5F).addBox(5.6F, 0F, -5.5F, 1F, 1.5F, 1.5F);
        lid.addOrReplaceChild("teeth_up", upper, PartPose.ZERO);
        lid.addOrReplaceChild("eyes", CubeListBuilder.create().texOffs(26, 37)
                .addBox(-4.75F, 0.02F, -8.5F, 2.5F, 0.5F, 2F).addBox(2.25F, 0.02F, -8.5F, 2.5F, 0.5F, 2F), PartPose.ZERO);
        float[][] lp = {{-5F, -4F}, {5F, -4F}, {-5F, 4F}, {5F, 4F}};
        for (int i = 0; i < 4; i++) {
            r.addOrReplaceChild("leg" + i, CubeListBuilder.create().texOffs(36, 37).addBox(-1F, 0F, -1F, 2F, 3F, 2F, new CubeDeformation(0.0F)),
                    PartPose.offset(lp[i][0], 21F, lp[i][1]));
        }
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(Mimico e, float limbSwing, float limbSwingAmount, float age, float yawHead, float pitch) {
        float pt = age - e.tickCount;
        float w = Mth.lerp(pt, e.wakeAnimO, e.wakeAnim);
        // despierto: se levanta sobre las patas y da saltitos al andar
        float lift = 3.0F * w;
        float hop = w * Math.abs(Mth.sin(limbSwing * 0.66F)) * Math.min(1.0F, limbSwingAmount * 2.0F) * 1.5F;
        base.y = 24.0F - lift - hop;
        base.xRot = w * Mth.sin(limbSwing * 0.66F) * 0.08F * Math.min(1.0F, limbSwingAmount * 2.0F);
        base.zRot = w * Mth.cos(limbSwing * 0.33F) * 0.05F * Math.min(1.0F, limbSwingAmount * 2.0F);
        for (int i = 0; i < 4; i++) {
            ModelPart l = legs[i];
            l.visible = w > 0.03F;
            l.y = 24.0F - lift - hop;
            l.yScale = Math.max(0.01F, w + hop / 3.0F);
            float ph = (i == 0 || i == 3) ? 0.0F : Mth.PI;
            l.xRot = Mth.cos(limbSwing * 1.2F + ph) * 0.9F * limbSwingAmount;
        }
        // la tapa: respira dormido, mastica despierto y se abre del todo al morder
        float open = 0.0F;
        if (e.breatheTicks > 0) {
            float b = (14 - e.breatheTicks + pt) / 14.0F;
            open = 0.09F * Mth.sin(Mth.clamp(b, 0.0F, 1.0F) * Mth.PI);
        }
        if (w > 0.0F) {
            open = Math.max(open, w * (0.16F + 0.2F * Math.abs(Mth.sin(age * 0.28F)) + 0.25F * limbSwingAmount));
        }
        if (e.biteTicks > 0) {
            float b = (10 - e.biteTicks + pt) / 10.0F;
            open = Math.max(open, 1.15F * Mth.sin(Mth.clamp(b, 0.0F, 1.0F) * Mth.PI));
        }
        lid.xRot = -open;
        tongue.visible = open > 0.02F;
        tongue.xRot = -0.25F * open + Mth.sin(age * 0.45F) * 0.06F * w;
        tongue.yRot = Mth.sin(age * 0.37F) * 0.25F * w;
        tongue.z = 3.5F - 2.0F * Math.max(0.0F, open - 0.4F);
    }

    @Override
    public void renderToBuffer(PoseStack pose, VertexConsumer vc, int light, int overlay, float r, float g, float b, float a) {
        root.render(pose, vc, light, overlay, r, g, b, a);
    }
}
