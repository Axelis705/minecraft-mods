package com.mundosdeaxelis.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mundosdeaxelis.MundosDeAxelis;
import com.mundosdeaxelis.entity.Ascua;
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

/** Modelo de Ascua (Etapa 2, la Forja). */
public class AscuaModel extends EntityModel<Ascua> {
    public static final ModelLayerLocation LAYER =
            new ModelLayerLocation(new ResourceLocation(MundosDeAxelis.MODID, "ascua"), "main");

    private final ModelPart root;
    private final ModelPart core;
    private final ModelPart shardA;
    private final ModelPart shardB;
    private final ModelPart shardC;
    private final ModelPart shardD;
    private final ModelPart flameA;
    private final ModelPart flameB;
    private final ModelPart flameC;
    private final ModelPart wingL;
    private final ModelPart wingR;
    private final ModelPart tipL;
    private final ModelPart tipR;
    private final ModelPart tailA;
    private final ModelPart tailB;
    private final ModelPart tailC;

    public AscuaModel(ModelPart root) {
        this.root = root;
        this.core = root.getChild("core");
        this.shardA = root.getChild("core").getChild("shard_a");
        this.shardB = root.getChild("core").getChild("shard_b");
        this.shardC = root.getChild("core").getChild("shard_c");
        this.shardD = root.getChild("core").getChild("shard_d");
        this.flameA = root.getChild("core").getChild("flame_a");
        this.flameB = root.getChild("core").getChild("flame_b");
        this.flameC = root.getChild("core").getChild("flame_c");
        this.wingL = root.getChild("core").getChild("wing_l");
        this.wingR = root.getChild("core").getChild("wing_r");
        this.tipL = root.getChild("core").getChild("wing_l").getChild("tip_l");
        this.tipR = root.getChild("core").getChild("wing_r").getChild("tip_r");
        this.tailA = root.getChild("core").getChild("tail_a");
        this.tailB = root.getChild("core").getChild("tail_a").getChild("tail_b");
        this.tailC = root.getChild("core").getChild("tail_a").getChild("tail_b").getChild("tail_c");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition r = mesh.getRoot();
        PartDefinition corePart = r.addOrReplaceChild("core", CubeListBuilder.create().texOffs(0, 0).addBox(-3F, -3F, -3F, 6F, 6F, 6F), PartPose.offset(0F, 18F, 0F));
        PartDefinition shard_aPart = corePart.addOrReplaceChild("shard_a", CubeListBuilder.create().texOffs(26, 19).addBox(-1F, -1F, -1F, 2F, 2F, 2F), PartPose.offset(5F, 0F, 0F));
        PartDefinition shard_bPart = corePart.addOrReplaceChild("shard_b", CubeListBuilder.create().texOffs(34, 19).addBox(-1F, -1F, -1F, 2F, 2F, 2F), PartPose.offset(-5F, 0F, 0F));
        PartDefinition shard_cPart = corePart.addOrReplaceChild("shard_c", CubeListBuilder.create().texOffs(42, 19).addBox(-1F, -1F, -1F, 2F, 2F, 2F), PartPose.offset(0F, 0F, 5F));
        PartDefinition shard_dPart = corePart.addOrReplaceChild("shard_d", CubeListBuilder.create().texOffs(50, 19).addBox(-1F, -1F, -1F, 2F, 2F, 2F), PartPose.offset(0F, 0F, -5F));
        PartDefinition flame_aPart = corePart.addOrReplaceChild("flame_a", CubeListBuilder.create().texOffs(26, 12).addBox(-1F, -4F, -1F, 2F, 4F, 2F), PartPose.offset(0F, -3F, 0F));
        PartDefinition flame_bPart = corePart.addOrReplaceChild("flame_b", CubeListBuilder.create().texOffs(10, 19).addBox(-1F, -3F, -1F, 2F, 3F, 2F), PartPose.offsetAndRotation(1.5F, -3F, 1F, 0F, 0F, 0.35F));
        PartDefinition flame_cPart = corePart.addOrReplaceChild("flame_c", CubeListBuilder.create().texOffs(18, 19).addBox(-1F, -3F, -1F, 2F, 3F, 2F), PartPose.offsetAndRotation(-1.5F, -3F, -1F, 0F, 0F, -0.35F));
        PartDefinition wing_lPart = corePart.addOrReplaceChild("wing_l", CubeListBuilder.create().texOffs(24, 0).addBox(0F, -0.5F, -2F, 4F, 1F, 6F), PartPose.offsetAndRotation(3F, -1F, 1F, 0F, 0F, -0.45F));
        PartDefinition wing_rPart = corePart.addOrReplaceChild("wing_r", CubeListBuilder.create().texOffs(44, 0).addBox(-4F, -0.5F, -2F, 4F, 1F, 6F), PartPose.offsetAndRotation(-3F, -1F, 1F, 0F, 0F, 0.45F));
        PartDefinition tip_lPart = wing_lPart.addOrReplaceChild("tip_l", CubeListBuilder.create().texOffs(34, 12).addBox(0F, -0.5F, -1F, 3F, 1F, 4F), PartPose.offsetAndRotation(4F, 0F, 0F, 0F, 0F, -0.3F));
        PartDefinition tip_rPart = wing_rPart.addOrReplaceChild("tip_r", CubeListBuilder.create().texOffs(48, 12).addBox(-3F, -0.5F, -1F, 3F, 1F, 4F), PartPose.offsetAndRotation(-4F, 0F, 0F, 0F, 0F, 0.3F));
        PartDefinition tail_aPart = corePart.addOrReplaceChild("tail_a", CubeListBuilder.create().texOffs(0, 12).addBox(-2F, -2F, 0F, 4F, 4F, 3F), PartPose.offset(0F, 0F, 3F));
        PartDefinition tail_bPart = tail_aPart.addOrReplaceChild("tail_b", CubeListBuilder.create().texOffs(14, 12).addBox(-1.5F, -1.5F, 0F, 3F, 3F, 3F), PartPose.offset(0F, 0F, 3F));
        PartDefinition tail_cPart = tail_bPart.addOrReplaceChild("tail_c", CubeListBuilder.create().texOffs(0, 19).addBox(-1F, -1F, 0F, 2F, 2F, 3F), PartPose.offset(0F, 0F, 3F));
        return LayerDefinition.create(mesh, 64, 32);
    }

    @Override
    public void setupAnim(Ascua e, float limbSwing, float limbSwingAmount, float age, float netHeadYaw, float headPitch) {
        float pt = age - e.tickCount;
        float dth = ForgeDeath.progress(e, pt);
        boolean ch = e.isCharging();
        this.core.y = 18.0F + Mth.sin(age * 0.2F) * 1.5F;
        this.core.xRot = ch ? 0.5F : 0.0F;
        this.core.yRot = dth * dth * 14.0F;
        float sp = age * (ch ? 0.45F : 0.12F);
        this.shardA.setPos(Mth.cos(sp) * 5.0F, Mth.sin(sp * 1.3F) * 1.5F, Mth.sin(sp) * 5.0F);
        this.shardB.setPos(Mth.cos(sp + (float) Math.PI) * 5.0F, Mth.sin(sp * 1.3F + 2.0F) * 1.5F, Mth.sin(sp + (float) Math.PI) * 5.0F);
        this.shardC.setPos(Mth.cos(sp + 1.57F) * 5.0F, Mth.sin(sp * 1.3F + 4.0F) * 1.5F, Mth.sin(sp + 1.57F) * 5.0F);
        this.shardD.setPos(Mth.cos(sp + 4.71F) * 5.0F, Mth.sin(sp * 1.3F + 1.0F) * 1.5F, Mth.sin(sp + 4.71F) * 5.0F);
        float fl = Mth.sin(age * 0.5F) * 0.15F;
        this.flameB.zRot = 0.35F + fl;
        this.flameC.zRot = -0.35F - fl;
        this.flameA.xRot = Mth.sin(age * 0.4F) * 0.1F;
        this.flameA.yScale = 1.0F + Mth.sin(age * 0.7F) * 0.2F;
        float fs = age * (ch ? 1.1F : 0.55F);
        float amp = ch ? 0.75F : 0.45F;
        this.wingL.zRot = -0.45F - Mth.sin(fs) * amp;
        this.wingR.zRot = 0.45F + Mth.sin(fs) * amp;
        this.tipL.zRot = -0.3F - Mth.sin(fs - 1.0F) * amp * 0.8F;
        this.tipR.zRot = 0.3F + Mth.sin(fs - 1.0F) * amp * 0.8F;
        this.tailA.yRot = Mth.sin(age * 0.2F) * 0.25F;
        this.tailB.yRot = Mth.sin(age * 0.2F - 0.8F) * 0.3F;
        this.tailC.yRot = Mth.sin(age * 0.2F - 1.6F) * 0.35F;
    }

    @Override
    public void renderToBuffer(PoseStack pose, VertexConsumer buffer, int light, int overlay, float r, float g, float b, float a) {
        this.root.render(pose, buffer, light, overlay, r, g, b, a);
    }
}
