package com.mundosdeaxelis.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mundosdeaxelis.MundosDeAxelis;
import com.mundosdeaxelis.entity.LoboArtico;
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

/**
 * Modelo del Lobo Ártico: un hombre lobo enorme con capucha y capa negras, cristales de hielo y dos guadañas de escarcha.
 * Las partes de abajo (cuerpo, capa, guadañas...) las genera el script de la textura; aquí está la animación: cada estado del
 * jefe (guardia, carga, embestida, aullido, tajos, ventisca, luna, aturdido, patinaje) tiene su pose, y entre ellas se camina,
 * respira y se mece la capa. Las guadañas conservan su inclinación pase lo que pase con los brazos ({@code grip_*} la compensa).
 */
public class LoboArticoModel extends EntityModel<LoboArtico> {
    public static final ModelLayerLocation LAYER =
            new ModelLayerLocation(new ResourceLocation(MundosDeAxelis.MODID, "lobo_artico"), "main");

    private static final float Y_PELVIS = -1.0F;
    private static final float Y_TORSO = -2.0F;
    private static final float Y_LEG = -2.0F;
    private static final float Y_TAIL = 1.0F;

    private final ModelPart root;
    private final ModelPart pelvis;
    private final ModelPart legL;
    private final ModelPart legR;
    private final ModelPart shinL;
    private final ModelPart shinR;
    private final ModelPart footL;
    private final ModelPart footR;
    private final ModelPart tailA;
    private final ModelPart tailB;
    private final ModelPart tailC;
    private final ModelPart torso;
    private final ModelPart neck;
    private final ModelPart head;
    private final ModelPart jaw;
    private final ModelPart cloakA;
    private final ModelPart cloakB;
    private final ModelPart cloakCl;
    private final ModelPart cloakCm;
    private final ModelPart cloakCr;
    private final ModelPart flapL;
    private final ModelPart flapR;
    private final ModelPart hoodDrape;
    private final ModelPart armL;
    private final ModelPart armR;
    private final ModelPart foreL;
    private final ModelPart foreR;
    private final ModelPart gripL;
    private final ModelPart gripR;
    private final ModelPart scyL;
    private final ModelPart scyR;
    private final ModelPart chainL;
    private final ModelPart chainR;
    private final ModelPart beard;
    private final float chainLx;
    private final float chainRx;
    private final float beardX;
    private final float tailBx;
    private final float tailCx;

    public LoboArticoModel(ModelPart root) {
        this.root = root;
        this.pelvis = root.getChild("pelvis");
        this.legL = root.getChild("leg_l");
        this.legR = root.getChild("leg_r");
        this.shinL = root.getChild("leg_l").getChild("shin_l");
        this.shinR = root.getChild("leg_r").getChild("shin_r");
        this.footL = root.getChild("leg_l").getChild("shin_l").getChild("foot_l");
        this.footR = root.getChild("leg_r").getChild("shin_r").getChild("foot_r");
        this.tailA = root.getChild("tail_a");
        this.tailB = root.getChild("tail_a").getChild("tail_b");
        this.tailC = root.getChild("tail_a").getChild("tail_b").getChild("tail_c");
        this.torso = root.getChild("torso");
        this.neck = root.getChild("torso").getChild("neck");
        this.head = root.getChild("torso").getChild("neck").getChild("head");
        this.jaw = root.getChild("torso").getChild("neck").getChild("head").getChild("jaw");
        this.cloakA = root.getChild("torso").getChild("cloak_a");
        this.cloakB = root.getChild("torso").getChild("cloak_a").getChild("cloak_b");
        this.cloakCl = root.getChild("torso").getChild("cloak_a").getChild("cloak_b").getChild("cloak_cl");
        this.cloakCm = root.getChild("torso").getChild("cloak_a").getChild("cloak_b").getChild("cloak_cm");
        this.cloakCr = root.getChild("torso").getChild("cloak_a").getChild("cloak_b").getChild("cloak_cr");
        this.flapL = root.getChild("torso").getChild("flap_l");
        this.flapR = root.getChild("torso").getChild("flap_r");
        this.hoodDrape = root.getChild("torso").getChild("neck").getChild("head").getChild("hood_drape");
        this.armL = root.getChild("torso").getChild("arm_l");
        this.armR = root.getChild("torso").getChild("arm_r");
        this.foreL = root.getChild("torso").getChild("arm_l").getChild("fore_l");
        this.foreR = root.getChild("torso").getChild("arm_r").getChild("fore_r");
        this.gripL = root.getChild("torso").getChild("arm_l").getChild("fore_l").getChild("hand_l").getChild("grip_l");
        this.gripR = root.getChild("torso").getChild("arm_r").getChild("fore_r").getChild("hand_r").getChild("grip_r");
        this.scyL = root.getChild("torso").getChild("arm_l").getChild("fore_l").getChild("hand_l").getChild("grip_l").getChild("scy_l");
        this.scyR = root.getChild("torso").getChild("arm_r").getChild("fore_r").getChild("hand_r").getChild("grip_r").getChild("scy_r");
        this.chainL = root.getChild("torso").getChild("chain_l");
        this.chainR = root.getChild("torso").getChild("chain_r");
        this.beard = root.getChild("torso").getChild("neck").getChild("head").getChild("beard");
        this.chainLx = this.chainL.xRot;
        this.chainRx = this.chainR.xRot;
        this.beardX = this.beard.xRot;
        this.tailBx = this.tailB.xRot;
        this.tailCx = this.tailC.xRot;
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition r = mesh.getRoot();
        PartDefinition pelvisPart = r.addOrReplaceChild("pelvis", CubeListBuilder.create().texOffs(170, 142).addBox(-7F, -3.5F, -5F, 14F, 7F, 10F), PartPose.offset(0F, -1F, 1F));
        PartDefinition leg_lPart = r.addOrReplaceChild("leg_l", CubeListBuilder.create().texOffs(262, 55).addBox(-4.5F, 0F, -5F, 9F, 13F, 10F), PartPose.offsetAndRotation(6F, -2F, 1F, -0.35F, 0F, 0F));
        PartDefinition knee_lPart = leg_lPart.addOrReplaceChild("knee_l", CubeListBuilder.create().texOffs(52, 123).addBox(-5F, -1F, -5.5F, 10F, 7F, 11F), PartPose.offset(0F, 0F, 0F));
        PartDefinition shin_lPart = leg_lPart.addOrReplaceChild("shin_l", CubeListBuilder.create().texOffs(260, 103).addBox(-3F, 0F, -3F, 6F, 13F, 6F), PartPose.offsetAndRotation(0F, 12.5F, 0.5F, 0.9F, 0F, 0F));
        PartDefinition foot_lPart = shin_lPart.addOrReplaceChild("foot_l", CubeListBuilder.create().texOffs(116, 159).addBox(-3.5F, 0F, -8.5F, 7F, 3.5F, 12F), PartPose.offsetAndRotation(0F, 12.5F, 0F, -0.55F, 0F, 0F));
        PartDefinition toe_lPart = foot_lPart.addOrReplaceChild("toe_l", CubeListBuilder.create().texOffs(104, 211).addBox(-2.5F, 1F, -12.5F, 5F, 2F, 4F), PartPose.offset(0F, 0F, 0F));
        PartDefinition leg_rPart = r.addOrReplaceChild("leg_r", CubeListBuilder.create().texOffs(0, 80).addBox(-4.5F, 0F, -5F, 9F, 13F, 10F), PartPose.offsetAndRotation(-6F, -2F, 1F, -0.35F, 0F, 0F));
        PartDefinition knee_rPart = leg_rPart.addOrReplaceChild("knee_r", CubeListBuilder.create().texOffs(94, 123).addBox(-5F, -1F, -5.5F, 10F, 7F, 11F), PartPose.offset(0F, 0F, 0F));
        PartDefinition shin_rPart = leg_rPart.addOrReplaceChild("shin_r", CubeListBuilder.create().texOffs(284, 103).addBox(-3F, 0F, -3F, 6F, 13F, 6F), PartPose.offsetAndRotation(0F, 12.5F, 0.5F, 0.9F, 0F, 0F));
        PartDefinition foot_rPart = shin_rPart.addOrReplaceChild("foot_r", CubeListBuilder.create().texOffs(154, 159).addBox(-3.5F, 0F, -8.5F, 7F, 3.5F, 12F), PartPose.offsetAndRotation(0F, 12.5F, 0F, -0.55F, 0F, 0F));
        PartDefinition toe_rPart = foot_rPart.addOrReplaceChild("toe_r", CubeListBuilder.create().texOffs(122, 211).addBox(-2.5F, 1F, -12.5F, 5F, 2F, 4F), PartPose.offset(0F, 0F, 0F));
        PartDefinition tail_aPart = r.addOrReplaceChild("tail_a", CubeListBuilder.create().texOffs(192, 159).addBox(-3F, -3F, 0F, 6F, 6F, 10F), PartPose.offsetAndRotation(0F, 1F, 8F, 0.2F, 0F, 0F));
        PartDefinition tail_bPart = tail_aPart.addOrReplaceChild("tail_b", CubeListBuilder.create().texOffs(0, 103).addBox(-4.5F, -4.5F, 0F, 9F, 9F, 11F), PartPose.offsetAndRotation(0F, 0F, 9.5F, 0.25F, 0F, 0F));
        PartDefinition tail_cPart = tail_bPart.addOrReplaceChild("tail_c", CubeListBuilder.create().texOffs(258, 142).addBox(-4F, -4F, 0F, 8F, 8F, 9F), PartPose.offsetAndRotation(0F, 0F, 10.5F, 0.3F, 0F, 0F));
        PartDefinition tail_tipPart = tail_cPart.addOrReplaceChild("tail_tip", CubeListBuilder.create().texOffs(72, 191).addBox(-2F, -2F, 8.5F, 4F, 4F, 6F), PartPose.offset(0F, 0F, 0F));
        PartDefinition torsoPart = r.addOrReplaceChild("torso", CubeListBuilder.create().texOffs(96, 0).addBox(-9F, -18F, -5.5F, 18F, 18F, 11F), PartPose.offsetAndRotation(0F, -2F, 0F, 0.25F, 0F, 0F));
        PartDefinition chestPart = torsoPart.addOrReplaceChild("chest", CubeListBuilder.create().texOffs(98, 55).addBox(-10F, -18F, -6.5F, 20F, 13F, 12F), PartPose.offset(0F, 0F, 0F));
        PartDefinition ruffPart = torsoPart.addOrReplaceChild("ruff", CubeListBuilder.create().texOffs(154, 0).addBox(-13F, -27F, -8.5F, 26F, 11F, 17F), PartPose.offset(0F, 0F, 0F));
        PartDefinition beltPart = torsoPart.addOrReplaceChild("belt", CubeListBuilder.create().texOffs(0, 142).addBox(-10F, -3F, -6.5F, 20F, 4F, 13F), PartPose.offset(0F, 0F, 0F));
        PartDefinition yokePart = torsoPart.addOrReplaceChild("yoke", CubeListBuilder.create().texOffs(0, 55).addBox(-15F, -24F, -9.5F, 30F, 6F, 19F), PartPose.offset(0F, 0F, 0F));
        PartDefinition gemPart = torsoPart.addOrReplaceChild("gem", CubeListBuilder.create().texOffs(40, 211).addBox(-2.5F, -14F, -9.5F, 5F, 5F, 2F), PartPose.offset(0F, 0F, 0F));
        PartDefinition cloak_aPart = torsoPart.addOrReplaceChild("cloak_a", CubeListBuilder.create().texOffs(120, 142).addBox(-11F, 0F, -1.5F, 22F, 14F, 3F), PartPose.offset(0F, -23F, 6.5F));
        PartDefinition cloak_bPart = cloak_aPart.addOrReplaceChild("cloak_b", CubeListBuilder.create().texOffs(66, 142).addBox(-12F, 0F, -1.5F, 24F, 14F, 3F), PartPose.offset(0F, 14F, 0F));
        PartDefinition cloak_clPart = cloak_bPart.addOrReplaceChild("cloak_cl", CubeListBuilder.create().texOffs(148, 103).addBox(-4.5F, 0F, -1.5F, 9F, 17F, 3F), PartPose.offset(-8F, 13.5F, 0F));
        PartDefinition cloak_cmPart = cloak_bPart.addOrReplaceChild("cloak_cm", CubeListBuilder.create().texOffs(38, 80).addBox(-4F, 0F, -1.5F, 8F, 20F, 3F), PartPose.offset(0F, 13.5F, 0F));
        PartDefinition cloak_crPart = cloak_bPart.addOrReplaceChild("cloak_cr", CubeListBuilder.create().texOffs(0, 123).addBox(-4.5F, 0F, -1.5F, 9F, 16F, 3F), PartPose.offset(8F, 13.5F, 0F));
        PartDefinition flap_lPart = torsoPart.addOrReplaceChild("flap_l", CubeListBuilder.create().texOffs(240, 0).addBox(-3.5F, 0F, -1.5F, 7F, 24F, 3F), PartPose.offset(7.5F, -22F, -8.5F));
        PartDefinition flap_rPart = torsoPart.addOrReplaceChild("flap_r", CubeListBuilder.create().texOffs(260, 0).addBox(-3.5F, 0F, -1.5F, 7F, 24F, 3F), PartPose.offset(-7.5F, -22F, -8.5F));
        PartDefinition sh_lPart = torsoPart.addOrReplaceChild("sh_l", CubeListBuilder.create().texOffs(264, 123).addBox(-2F, -14F, -2F, 4F, 14F, 4F), PartPose.offsetAndRotation(14F, -24F, 0F, -0.1F, 0F, 0.55F));
        PartDefinition sh_rPart = torsoPart.addOrReplaceChild("sh_r", CubeListBuilder.create().texOffs(280, 123).addBox(-2F, -14F, -2F, 4F, 14F, 4F), PartPose.offsetAndRotation(-14F, -24F, 0F, -0.1F, 0F, -0.55F));
        PartDefinition sh2_lPart = torsoPart.addOrReplaceChild("sh2_l", CubeListBuilder.create().texOffs(0, 191).addBox(-1.5F, -9F, -1.5F, 3F, 9F, 3F), PartPose.offsetAndRotation(12F, -25F, 5F, 0.4F, 0F, 0.3F));
        PartDefinition sh2_rPart = torsoPart.addOrReplaceChild("sh2_r", CubeListBuilder.create().texOffs(12, 191).addBox(-1.5F, -9F, -1.5F, 3F, 9F, 3F), PartPose.offsetAndRotation(-12F, -25F, 5F, 0.4F, 0F, -0.3F));
        PartDefinition cry_lPart = torsoPart.addOrReplaceChild("cry_l", CubeListBuilder.create().texOffs(224, 159).addBox(-2F, -12F, -2F, 4F, 12F, 4F), PartPose.offsetAndRotation(4.5F, -24F, 7F, 0.35F, 0F, 0.15F));
        PartDefinition cry_rPart = torsoPart.addOrReplaceChild("cry_r", CubeListBuilder.create().texOffs(240, 159).addBox(-2F, -12F, -2F, 4F, 12F, 4F), PartPose.offsetAndRotation(-4.5F, -24F, 7F, 0.35F, 0F, -0.15F));
        PartDefinition chain_lPart = torsoPart.addOrReplaceChild("chain_l", CubeListBuilder.create().texOffs(208, 191).addBox(-1F, 0F, -1F, 2F, 8F, 2F), PartPose.offset(9F, -2F, -6F));
        PartDefinition skull_lPart = chain_lPart.addOrReplaceChild("skull_l", CubeListBuilder.create().texOffs(224, 176).addBox(-3F, 7F, -3F, 6F, 6F, 6F), PartPose.offset(0F, 0F, 0F));
        PartDefinition chain_rPart = torsoPart.addOrReplaceChild("chain_r", CubeListBuilder.create().texOffs(216, 191).addBox(-1F, 0F, -1F, 2F, 8F, 2F), PartPose.offset(-9F, -2F, -6F));
        PartDefinition skull_rPart = chain_rPart.addOrReplaceChild("skull_r", CubeListBuilder.create().texOffs(248, 176).addBox(-3F, 7F, -3F, 6F, 6F, 6F), PartPose.offset(0F, 0F, 0F));
        PartDefinition neckPart = torsoPart.addOrReplaceChild("neck", CubeListBuilder.create().texOffs(218, 142).addBox(-5.5F, -6F, -4.5F, 11F, 8F, 9F), PartPose.offsetAndRotation(0F, -24F, -2F, 0.35F, 0F, 0F));
        PartDefinition headPart = neckPart.addOrReplaceChild("head", CubeListBuilder.create().texOffs(162, 55).addBox(-7F, -13F, -11F, 14F, 13F, 12F), PartPose.offsetAndRotation(0F, -5F, 1F, -0.3F, 0F, 0F));
        PartDefinition snoutPart = headPart.addOrReplaceChild("snout", CubeListBuilder.create().texOffs(178, 123).addBox(-3.5F, -6F, -23F, 7F, 6F, 12F), PartPose.offset(0F, 0F, 0F));
        PartDefinition nosePart = headPart.addOrReplaceChild("nose", CubeListBuilder.create().texOffs(140, 211).addBox(-2.5F, -6F, -24.4F, 5F, 3.5F, 1.5F), PartPose.offset(0F, 0F, 0F));
        PartDefinition jawPart = headPart.addOrReplaceChild("jaw", CubeListBuilder.create().texOffs(256, 159).addBox(-3F, 0F, -12F, 6F, 2.5F, 12F), PartPose.offsetAndRotation(0F, 0F, 0F, 0.12F, 0F, 0F));
        PartDefinition tu_l0Part = headPart.addOrReplaceChild("tu_l0", CubeListBuilder.create().texOffs(170, 211).addBox(3.0F, 0F, -21.5F, 1F, 2.5F, 1F), PartPose.offset(0F, 0F, 0F));
        PartDefinition tu_r0Part = headPart.addOrReplaceChild("tu_r0", CubeListBuilder.create().texOffs(174, 211).addBox(-4.0F, 0F, -21.5F, 1F, 2.5F, 1F), PartPose.offset(0F, 0F, 0F));
        PartDefinition tu_l1Part = headPart.addOrReplaceChild("tu_l1", CubeListBuilder.create().texOffs(178, 211).addBox(3.0F, 0F, -18.5F, 1F, 2.5F, 1F), PartPose.offset(0F, 0F, 0F));
        PartDefinition tu_r1Part = headPart.addOrReplaceChild("tu_r1", CubeListBuilder.create().texOffs(182, 211).addBox(-4.0F, 0F, -18.5F, 1F, 2.5F, 1F), PartPose.offset(0F, 0F, 0F));
        PartDefinition tu_l2Part = headPart.addOrReplaceChild("tu_l2", CubeListBuilder.create().texOffs(186, 211).addBox(3.0F, 0F, -15.5F, 1F, 2.5F, 1F), PartPose.offset(0F, 0F, 0F));
        PartDefinition tu_r2Part = headPart.addOrReplaceChild("tu_r2", CubeListBuilder.create().texOffs(190, 211).addBox(-4.0F, 0F, -15.5F, 1F, 2.5F, 1F), PartPose.offset(0F, 0F, 0F));
        PartDefinition tu_l3Part = headPart.addOrReplaceChild("tu_l3", CubeListBuilder.create().texOffs(194, 211).addBox(3.0F, 0F, -12.5F, 1F, 2.5F, 1F), PartPose.offset(0F, 0F, 0F));
        PartDefinition tu_r3Part = headPart.addOrReplaceChild("tu_r3", CubeListBuilder.create().texOffs(198, 211).addBox(-4.0F, 0F, -12.5F, 1F, 2.5F, 1F), PartPose.offset(0F, 0F, 0F));
        PartDefinition td_l0Part = jawPart.addOrReplaceChild("td_l0", CubeListBuilder.create().texOffs(202, 211).addBox(2.2F, -2.2F, -10.5F, 1F, 2.2F, 1F), PartPose.offset(0F, 0F, 0F));
        PartDefinition td_r0Part = jawPart.addOrReplaceChild("td_r0", CubeListBuilder.create().texOffs(206, 211).addBox(-3.2F, -2.2F, -10.5F, 1F, 2.2F, 1F), PartPose.offset(0F, 0F, 0F));
        PartDefinition td_l1Part = jawPart.addOrReplaceChild("td_l1", CubeListBuilder.create().texOffs(210, 211).addBox(2.2F, -2.2F, -7.5F, 1F, 2.2F, 1F), PartPose.offset(0F, 0F, 0F));
        PartDefinition td_r1Part = jawPart.addOrReplaceChild("td_r1", CubeListBuilder.create().texOffs(214, 211).addBox(-3.2F, -2.2F, -7.5F, 1F, 2.2F, 1F), PartPose.offset(0F, 0F, 0F));
        PartDefinition td_l2Part = jawPart.addOrReplaceChild("td_l2", CubeListBuilder.create().texOffs(218, 211).addBox(2.2F, -2.2F, -4.5F, 1F, 2.2F, 1F), PartPose.offset(0F, 0F, 0F));
        PartDefinition td_r2Part = jawPart.addOrReplaceChild("td_r2", CubeListBuilder.create().texOffs(222, 211).addBox(-3.2F, -2.2F, -4.5F, 1F, 2.2F, 1F), PartPose.offset(0F, 0F, 0F));
        PartDefinition fang_ulPart = headPart.addOrReplaceChild("fang_ul", CubeListBuilder.create().texOffs(132, 203).addBox(1.6F, 0F, -23F, 1.6F, 5.5F, 1.6F), PartPose.offset(0F, 0F, 0F));
        PartDefinition fang_urPart = headPart.addOrReplaceChild("fang_ur", CubeListBuilder.create().texOffs(140, 203).addBox(-3.2F, 0F, -23F, 1.6F, 5.5F, 1.6F), PartPose.offset(0F, 0F, 0F));
        PartDefinition fang_llPart = jawPart.addOrReplaceChild("fang_ll", CubeListBuilder.create().texOffs(154, 211).addBox(1.4F, -4F, -11.5F, 1.4F, 4F, 1.4F), PartPose.offset(0F, 0F, 0F));
        PartDefinition fang_lrPart = jawPart.addOrReplaceChild("fang_lr", CubeListBuilder.create().texOffs(162, 211).addBox(-2.8F, -4F, -11.5F, 1.4F, 4F, 1.4F), PartPose.offset(0F, 0F, 0F));
        PartDefinition beardPart = headPart.addOrReplaceChild("beard", CubeListBuilder.create().texOffs(22, 176).addBox(-4F, 0F, -9F, 8F, 7F, 7F), PartPose.offset(0F, 0F, 0F));
        PartDefinition browPart = headPart.addOrReplaceChild("brow", CubeListBuilder.create().texOffs(70, 211).addBox(-7F, -10.5F, -12.5F, 14F, 2.5F, 3F), PartPose.offset(0F, 0F, 0F));
        PartDefinition hood_topPart = headPart.addOrReplaceChild("hood_top", CubeListBuilder.create().texOffs(232, 80).addBox(-9.5F, -16F, -13F, 19F, 3F, 17F), PartPose.offset(0F, 0F, 0F));
        PartDefinition hood_capPart = headPart.addOrReplaceChild("hood_cap", CubeListBuilder.create().texOffs(24, 159).addBox(-6.5F, -19F, -11F, 13F, 3F, 13F), PartPose.offset(0F, 0F, 0F));
        PartDefinition hood_peakPart = headPart.addOrReplaceChild("hood_peak", CubeListBuilder.create().texOffs(136, 123).addBox(-4F, -3F, 0F, 8F, 5F, 13F), PartPose.offsetAndRotation(0F, -19F, 2F, -0.65F, 0F, 0F));
        PartDefinition hood_backPart = headPart.addOrReplaceChild("hood_back", CubeListBuilder.create().texOffs(214, 55).addBox(-9.5F, -16F, 1F, 19F, 18F, 4.5F), PartPose.offset(0F, 0F, 0F));
        PartDefinition hood_lPart = headPart.addOrReplaceChild("hood_l", CubeListBuilder.create().texOffs(24, 0).addBox(7F, -16F, -13F, 3F, 16F, 15F), PartPose.offset(0F, 0F, 0F));
        PartDefinition hood_rPart = headPart.addOrReplaceChild("hood_r", CubeListBuilder.create().texOffs(60, 0).addBox(-10F, -16F, -13F, 3F, 16F, 15F), PartPose.offset(0F, 0F, 0F));
        PartDefinition hood_brimPart = headPart.addOrReplaceChild("hood_brim", CubeListBuilder.create().texOffs(148, 203).addBox(-9.5F, -17F, -16F, 19F, 3F, 3.5F), PartPose.offset(0F, 0F, 0F));
        PartDefinition hood_drapePart = headPart.addOrReplaceChild("hood_drape", CubeListBuilder.create().texOffs(76, 159).addBox(-8F, 0F, -1.5F, 16F, 12F, 3.5F), PartPose.offsetAndRotation(0F, 3F, 5.5F, 0.3F, 0F, 0F));
        PartDefinition arm_lPart = torsoPart.addOrReplaceChild("arm_l", CubeListBuilder.create().texOffs(116, 80).addBox(-4F, -2F, -4F, 8F, 13F, 8F), PartPose.offset(14F, -21F, 0F));
        PartDefinition pauld_lPart = arm_lPart.addOrReplaceChild("pauld_l", CubeListBuilder.create().texOffs(172, 103).addBox(-5.5F, -4F, -5.5F, 11F, 8F, 11F), PartPose.offset(0F, 0F, 0F));
        PartDefinition fore_lPart = arm_lPart.addOrReplaceChild("fore_l", CubeListBuilder.create().texOffs(40, 103).addBox(-3.5F, 0F, -3.5F, 7F, 13F, 7F), PartPose.offset(0F, 11F, 0F));
        PartDefinition bracer_lPart = fore_lPart.addOrReplaceChild("bracer_l", CubeListBuilder.create().texOffs(76, 176).addBox(-4F, 6F, -4F, 8F, 5F, 8F), PartPose.offset(0F, 0F, 0F));
        PartDefinition hand_lPart = fore_lPart.addOrReplaceChild("hand_l", CubeListBuilder.create().texOffs(140, 176).addBox(-3.5F, 0F, -4F, 7F, 5F, 8F), PartPose.offset(0F, 12.5F, 0F));
        PartDefinition claw0_lPart = hand_lPart.addOrReplaceChild("claw0_l", CubeListBuilder.create().texOffs(36, 203).addBox(-3.2F, 3F, -9.5F, 1.8F, 2F, 6F), PartPose.offset(0F, 0F, 0F));
        PartDefinition claw1_lPart = hand_lPart.addOrReplaceChild("claw1_l", CubeListBuilder.create().texOffs(52, 203).addBox(-0.9F, 3F, -10.5F, 1.8F, 2F, 6F), PartPose.offset(0F, 0F, 0F));
        PartDefinition claw2_lPart = hand_lPart.addOrReplaceChild("claw2_l", CubeListBuilder.create().texOffs(68, 203).addBox(1.4F, 3F, -9.5F, 1.8F, 2F, 6F), PartPose.offset(0F, 0F, 0F));
        PartDefinition grip_lPart = hand_lPart.addOrReplaceChild("grip_l", CubeListBuilder.create(), PartPose.offset(0F, 2.5F, -1F));
        PartDefinition scy_lPart = grip_lPart.addOrReplaceChild("scy_l", CubeListBuilder.create().texOffs(0, 0).addBox(-1.5F, -38F, -1.5F, 3F, 52F, 3F), PartPose.offset(0F, 0F, 0F));
        PartDefinition ring0_lPart = scy_lPart.addOrReplaceChild("ring0_l", CubeListBuilder.create().texOffs(194, 203).addBox(-2.25F, -31F, -2.25F, 4.5F, 2F, 4.5F), PartPose.offset(0F, 0F, 0F));
        PartDefinition ring1_lPart = scy_lPart.addOrReplaceChild("ring1_l", CubeListBuilder.create().texOffs(214, 203).addBox(-2.25F, -19F, -2.25F, 4.5F, 2F, 4.5F), PartPose.offset(0F, 0F, 0F));
        PartDefinition ring2_lPart = scy_lPart.addOrReplaceChild("ring2_l", CubeListBuilder.create().texOffs(234, 203).addBox(-2.25F, -7F, -2.25F, 4.5F, 2F, 4.5F), PartPose.offset(0F, 0F, 0F));
        PartDefinition ring3_lPart = scy_lPart.addOrReplaceChild("ring3_l", CubeListBuilder.create().texOffs(254, 203).addBox(-2.25F, 5F, -2.25F, 4.5F, 2F, 4.5F), PartPose.offset(0F, 0F, 0F));
        PartDefinition cap_lPart = scy_lPart.addOrReplaceChild("cap_l", CubeListBuilder.create().texOffs(92, 191).addBox(-2.5F, -43F, -2.5F, 5F, 5F, 5F), PartPose.offset(0F, 0F, 0F));
        PartDefinition topskull_lPart = scy_lPart.addOrReplaceChild("topskull_l", CubeListBuilder.create().texOffs(112, 191).addBox(-2.5F, -48F, -2.5F, 5F, 5F, 5F), PartPose.offset(0F, 0F, 0F));
        PartDefinition spike_lPart = scy_lPart.addOrReplaceChild("spike_l", CubeListBuilder.create().texOffs(200, 176).addBox(-1.25F, -58F, -1.25F, 2.5F, 10F, 2.5F), PartPose.offset(0F, 0F, 0F));
        PartDefinition butt_lPart = scy_lPart.addOrReplaceChild("butt_l", CubeListBuilder.create().texOffs(24, 191).addBox(-1.75F, 14F, -1.75F, 3.5F, 7F, 3.5F), PartPose.offset(0F, 0F, 0F));
        PartDefinition bchain_lPart = scy_lPart.addOrReplaceChild("bchain_l", CubeListBuilder.create().texOffs(56, 191).addBox(-0.75F, 0F, -0.75F, 1.5F, 9F, 1.5F), PartPose.offset(0F, 21F, 0F));
        PartDefinition bcharm_lPart = bchain_lPart.addOrReplaceChild("bcharm_l", CubeListBuilder.create().texOffs(224, 191).addBox(-1.75F, 8.5F, -1.75F, 3.5F, 5F, 3.5F), PartPose.offset(0F, 0F, 0F));
        PartDefinition rag_lPart = scy_lPart.addOrReplaceChild("rag_l", CubeListBuilder.create().texOffs(24, 123).addBox(-3F, 0F, -0.5F, 6F, 18F, 1F), PartPose.offset(0F, -35F, 2.8F));
        PartDefinition rag2_lPart = scy_lPart.addOrReplaceChild("rag2_l", CubeListBuilder.create().texOffs(52, 176).addBox(-2.5F, 0F, -0.5F, 5F, 13F, 1F), PartPose.offset(0F, -30F, -2.8F));
        PartDefinition bl0_lPart = scy_lPart.addOrReplaceChild("bl0_l", CubeListBuilder.create().texOffs(60, 80).addBox(-1.5F, -5.5F, -11F, 3F, 11F, 11F), PartPose.offset(0F, -40F, 0F));
        PartDefinition bl1_lPart = bl0_lPart.addOrReplaceChild("bl1_l", CubeListBuilder.create().texOffs(180, 80).addBox(-1.5F, -5.5F, -10F, 3F, 11F, 10F), PartPose.offsetAndRotation(0F, 0F, -10.5F, 0.28F, 0F, 0F));
        PartDefinition bl2_lPart = bl1_lPart.addOrReplaceChild("bl2_l", CubeListBuilder.create().texOffs(96, 103).addBox(-1.5F, -5.0F, -10F, 3F, 10F, 10F), PartPose.offsetAndRotation(0F, 0F, -9.5F, 0.3F, 0F, 0F));
        PartDefinition bl3_lPart = bl2_lPart.addOrReplaceChild("bl3_l", CubeListBuilder.create().texOffs(216, 123).addBox(-1.5F, -4.5F, -9F, 3F, 9F, 9F), PartPose.offsetAndRotation(0F, 0F, -9.5F, 0.32F, 0F, 0F));
        PartDefinition bl4_lPart = bl3_lPart.addOrReplaceChild("bl4_l", CubeListBuilder.create().texOffs(292, 142).addBox(-1.5F, -4.0F, -9F, 3F, 8F, 9F), PartPose.offsetAndRotation(0F, 0F, -8.5F, 0.36F, 0F, 0F));
        PartDefinition bl5_lPart = bl4_lPart.addOrReplaceChild("bl5_l", CubeListBuilder.create().texOffs(292, 159).addBox(-1.5F, -3.25F, -8F, 3F, 6.5F, 8F), PartPose.offsetAndRotation(0F, 0F, -8.5F, 0.4F, 0F, 0F));
        PartDefinition bl6_lPart = bl5_lPart.addOrReplaceChild("bl6_l", CubeListBuilder.create().texOffs(272, 176).addBox(-1.5F, -2.5F, -7F, 3F, 5F, 7F), PartPose.offsetAndRotation(0F, 0F, -7.5F, 0.44F, 0F, 0F));
        PartDefinition bl7_lPart = bl6_lPart.addOrReplaceChild("bl7_l", CubeListBuilder.create().texOffs(172, 191).addBox(-1.5F, -1.75F, -6F, 3F, 3.5F, 6F), PartPose.offsetAndRotation(0F, 0F, -6.5F, 0.5F, 0F, 0F));
        PartDefinition bl8_lPart = bl7_lPart.addOrReplaceChild("bl8_l", CubeListBuilder.create().texOffs(0, 203).addBox(-1.5F, -1.0F, -6F, 3F, 2F, 6F), PartPose.offsetAndRotation(0F, 0F, -5.5F, 0.55F, 0F, 0F));
        PartDefinition barb1_lPart = bl1_lPart.addOrReplaceChild("barb1_l", CubeListBuilder.create().texOffs(256, 191).addBox(-1F, -7F, -1F, 2F, 7F, 2F), PartPose.offsetAndRotation(0F, -5.5F, -5.5F, -0.55F, 0F, 0F));
        PartDefinition barb3_lPart = bl3_lPart.addOrReplaceChild("barb3_l", CubeListBuilder.create().texOffs(264, 191).addBox(-1F, -7F, -1F, 2F, 7F, 2F), PartPose.offsetAndRotation(0F, -4.5F, -4.95F, -0.55F, 0F, 0F));
        PartDefinition barb5_lPart = bl5_lPart.addOrReplaceChild("barb5_l", CubeListBuilder.create().texOffs(272, 191).addBox(-1F, -7F, -1F, 2F, 7F, 2F), PartPose.offsetAndRotation(0F, -3.25F, -4.4F, -0.55F, 0F, 0F));
        PartDefinition barb7_lPart = bl7_lPart.addOrReplaceChild("barb7_l", CubeListBuilder.create().texOffs(54, 211).addBox(-1F, -5F, -1F, 2F, 5F, 2F), PartPose.offsetAndRotation(0F, -1.75F, -3.3000000000000003F, -0.55F, 0F, 0F));
        PartDefinition arm_rPart = torsoPart.addOrReplaceChild("arm_r", CubeListBuilder.create().texOffs(148, 80).addBox(-4F, -2F, -4F, 8F, 13F, 8F), PartPose.offset(-14F, -21F, 0F));
        PartDefinition pauld_rPart = arm_rPart.addOrReplaceChild("pauld_r", CubeListBuilder.create().texOffs(216, 103).addBox(-5.5F, -4F, -5.5F, 11F, 8F, 11F), PartPose.offset(0F, 0F, 0F));
        PartDefinition fore_rPart = arm_rPart.addOrReplaceChild("fore_r", CubeListBuilder.create().texOffs(68, 103).addBox(-3.5F, 0F, -3.5F, 7F, 13F, 7F), PartPose.offset(0F, 11F, 0F));
        PartDefinition bracer_rPart = fore_rPart.addOrReplaceChild("bracer_r", CubeListBuilder.create().texOffs(108, 176).addBox(-4F, 6F, -4F, 8F, 5F, 8F), PartPose.offset(0F, 0F, 0F));
        PartDefinition hand_rPart = fore_rPart.addOrReplaceChild("hand_r", CubeListBuilder.create().texOffs(170, 176).addBox(-3.5F, 0F, -4F, 7F, 5F, 8F), PartPose.offset(0F, 12.5F, 0F));
        PartDefinition claw0_rPart = hand_rPart.addOrReplaceChild("claw0_r", CubeListBuilder.create().texOffs(84, 203).addBox(-3.2F, 3F, -9.5F, 1.8F, 2F, 6F), PartPose.offset(0F, 0F, 0F));
        PartDefinition claw1_rPart = hand_rPart.addOrReplaceChild("claw1_r", CubeListBuilder.create().texOffs(100, 203).addBox(-0.9F, 3F, -10.5F, 1.8F, 2F, 6F), PartPose.offset(0F, 0F, 0F));
        PartDefinition claw2_rPart = hand_rPart.addOrReplaceChild("claw2_r", CubeListBuilder.create().texOffs(116, 203).addBox(1.4F, 3F, -9.5F, 1.8F, 2F, 6F), PartPose.offset(0F, 0F, 0F));
        PartDefinition grip_rPart = hand_rPart.addOrReplaceChild("grip_r", CubeListBuilder.create(), PartPose.offset(0F, 2.5F, -1F));
        PartDefinition scy_rPart = grip_rPart.addOrReplaceChild("scy_r", CubeListBuilder.create().texOffs(12, 0).addBox(-1.5F, -38F, -1.5F, 3F, 52F, 3F), PartPose.offset(0F, 0F, 0F));
        PartDefinition ring0_rPart = scy_rPart.addOrReplaceChild("ring0_r", CubeListBuilder.create().texOffs(274, 203).addBox(-2.25F, -31F, -2.25F, 4.5F, 2F, 4.5F), PartPose.offset(0F, 0F, 0F));
        PartDefinition ring1_rPart = scy_rPart.addOrReplaceChild("ring1_r", CubeListBuilder.create().texOffs(294, 203).addBox(-2.25F, -19F, -2.25F, 4.5F, 2F, 4.5F), PartPose.offset(0F, 0F, 0F));
        PartDefinition ring2_rPart = scy_rPart.addOrReplaceChild("ring2_r", CubeListBuilder.create().texOffs(0, 211).addBox(-2.25F, -7F, -2.25F, 4.5F, 2F, 4.5F), PartPose.offset(0F, 0F, 0F));
        PartDefinition ring3_rPart = scy_rPart.addOrReplaceChild("ring3_r", CubeListBuilder.create().texOffs(20, 211).addBox(-2.25F, 5F, -2.25F, 4.5F, 2F, 4.5F), PartPose.offset(0F, 0F, 0F));
        PartDefinition cap_rPart = scy_rPart.addOrReplaceChild("cap_r", CubeListBuilder.create().texOffs(132, 191).addBox(-2.5F, -43F, -2.5F, 5F, 5F, 5F), PartPose.offset(0F, 0F, 0F));
        PartDefinition topskull_rPart = scy_rPart.addOrReplaceChild("topskull_r", CubeListBuilder.create().texOffs(152, 191).addBox(-2.5F, -48F, -2.5F, 5F, 5F, 5F), PartPose.offset(0F, 0F, 0F));
        PartDefinition spike_rPart = scy_rPart.addOrReplaceChild("spike_r", CubeListBuilder.create().texOffs(212, 176).addBox(-1.25F, -58F, -1.25F, 2.5F, 10F, 2.5F), PartPose.offset(0F, 0F, 0F));
        PartDefinition butt_rPart = scy_rPart.addOrReplaceChild("butt_r", CubeListBuilder.create().texOffs(40, 191).addBox(-1.75F, 14F, -1.75F, 3.5F, 7F, 3.5F), PartPose.offset(0F, 0F, 0F));
        PartDefinition bchain_rPart = scy_rPart.addOrReplaceChild("bchain_r", CubeListBuilder.create().texOffs(64, 191).addBox(-0.75F, 0F, -0.75F, 1.5F, 9F, 1.5F), PartPose.offset(0F, 21F, 0F));
        PartDefinition bcharm_rPart = bchain_rPart.addOrReplaceChild("bcharm_r", CubeListBuilder.create().texOffs(240, 191).addBox(-1.75F, 8.5F, -1.75F, 3.5F, 5F, 3.5F), PartPose.offset(0F, 0F, 0F));
        PartDefinition rag_rPart = scy_rPart.addOrReplaceChild("rag_r", CubeListBuilder.create().texOffs(38, 123).addBox(-3F, 0F, -0.5F, 6F, 18F, 1F), PartPose.offset(0F, -35F, 2.8F));
        PartDefinition rag2_rPart = scy_rPart.addOrReplaceChild("rag2_r", CubeListBuilder.create().texOffs(64, 176).addBox(-2.5F, 0F, -0.5F, 5F, 13F, 1F), PartPose.offset(0F, -30F, -2.8F));
        PartDefinition bl0_rPart = scy_rPart.addOrReplaceChild("bl0_r", CubeListBuilder.create().texOffs(88, 80).addBox(-1.5F, -5.5F, -11F, 3F, 11F, 11F), PartPose.offset(0F, -40F, 0F));
        PartDefinition bl1_rPart = bl0_rPart.addOrReplaceChild("bl1_r", CubeListBuilder.create().texOffs(206, 80).addBox(-1.5F, -5.5F, -10F, 3F, 11F, 10F), PartPose.offsetAndRotation(0F, 0F, -10.5F, 0.28F, 0F, 0F));
        PartDefinition bl2_rPart = bl1_rPart.addOrReplaceChild("bl2_r", CubeListBuilder.create().texOffs(122, 103).addBox(-1.5F, -5.0F, -10F, 3F, 10F, 10F), PartPose.offsetAndRotation(0F, 0F, -9.5F, 0.3F, 0F, 0F));
        PartDefinition bl3_rPart = bl2_rPart.addOrReplaceChild("bl3_r", CubeListBuilder.create().texOffs(240, 123).addBox(-1.5F, -4.5F, -9F, 3F, 9F, 9F), PartPose.offsetAndRotation(0F, 0F, -9.5F, 0.32F, 0F, 0F));
        PartDefinition bl4_rPart = bl3_rPart.addOrReplaceChild("bl4_r", CubeListBuilder.create().texOffs(0, 159).addBox(-1.5F, -4.0F, -9F, 3F, 8F, 9F), PartPose.offsetAndRotation(0F, 0F, -8.5F, 0.36F, 0F, 0F));
        PartDefinition bl5_rPart = bl4_rPart.addOrReplaceChild("bl5_r", CubeListBuilder.create().texOffs(0, 176).addBox(-1.5F, -3.25F, -8F, 3F, 6.5F, 8F), PartPose.offsetAndRotation(0F, 0F, -8.5F, 0.4F, 0F, 0F));
        PartDefinition bl6_rPart = bl5_rPart.addOrReplaceChild("bl6_r", CubeListBuilder.create().texOffs(292, 176).addBox(-1.5F, -2.5F, -7F, 3F, 5F, 7F), PartPose.offsetAndRotation(0F, 0F, -7.5F, 0.44F, 0F, 0F));
        PartDefinition bl7_rPart = bl6_rPart.addOrReplaceChild("bl7_r", CubeListBuilder.create().texOffs(190, 191).addBox(-1.5F, -1.75F, -6F, 3F, 3.5F, 6F), PartPose.offsetAndRotation(0F, 0F, -6.5F, 0.5F, 0F, 0F));
        PartDefinition bl8_rPart = bl7_rPart.addOrReplaceChild("bl8_r", CubeListBuilder.create().texOffs(18, 203).addBox(-1.5F, -1.0F, -6F, 3F, 2F, 6F), PartPose.offsetAndRotation(0F, 0F, -5.5F, 0.55F, 0F, 0F));
        PartDefinition barb1_rPart = bl1_rPart.addOrReplaceChild("barb1_r", CubeListBuilder.create().texOffs(280, 191).addBox(-1F, -7F, -1F, 2F, 7F, 2F), PartPose.offsetAndRotation(0F, -5.5F, -5.5F, -0.55F, 0F, 0F));
        PartDefinition barb3_rPart = bl3_rPart.addOrReplaceChild("barb3_r", CubeListBuilder.create().texOffs(288, 191).addBox(-1F, -7F, -1F, 2F, 7F, 2F), PartPose.offsetAndRotation(0F, -4.5F, -4.95F, -0.55F, 0F, 0F));
        PartDefinition barb5_rPart = bl5_rPart.addOrReplaceChild("barb5_r", CubeListBuilder.create().texOffs(296, 191).addBox(-1F, -7F, -1F, 2F, 7F, 2F), PartPose.offsetAndRotation(0F, -3.25F, -4.4F, -0.55F, 0F, 0F));
        PartDefinition barb7_rPart = bl7_rPart.addOrReplaceChild("barb7_r", CubeListBuilder.create().texOffs(62, 211).addBox(-1F, -5F, -1F, 2F, 5F, 2F), PartPose.offsetAndRotation(0F, -1.75F, -3.3000000000000003F, -0.55F, 0F, 0F));
        return LayerDefinition.create(mesh, 320, 320);
    }

    // ------------------------------------------------------------------ poses

    /** Todos los números de una pose (ángulos en radianes). */
    static final class Pz {
        float spin;
        float torsoX, torsoY, torsoZ, neckX, headPitch, headYaw, jaw;
        float lx, rx, lsx, rsx;
        float alx, alz, flx, arx, arz, frx;
        float tlx, tlz, trx, trz, rolll, rollr;
        float tail, cloak, strip, flap, drape;
    }

    private static float clamp01(float x) {
        return Mth.clamp(x, 0.0F, 1.0F);
    }

    private static float sm(float x) {
        x = clamp01(x);
        return x * x * (3.0F - 2.0F * x);
    }

    private static float lerp(float a, float b, float x) {
        return a + (b - a) * x;
    }

    /** Interpolación suave entre claves: {tick, valor, tick, valor...}. */
    private static float kf(float t, float... f) {
        if (t <= f[0]) {
            return f[1];
        }
        for (int i = 2; i < f.length; i += 2) {
            if (t <= f[i]) {
                float t0 = f[i - 2];
                float t1 = f[i];
                return lerp(f[i - 1], f[i + 1], sm((t - t0) / Math.max(1.0E-6F, t1 - t0)));
            }
        }
        return f[f.length - 1];
    }

    static Pz idle(float age) {
        Pz p = new Pz();
        p.torsoX = 0.28F + Mth.sin(age * 0.08F) * 0.02F;
        p.neckX = 0.35F;
        p.headPitch = 0.3F;
        p.jaw = 0.14F + Math.max(0.0F, Mth.sin(age * 0.05F)) * 0.05F;
        p.lx = -0.35F;
        p.rx = -0.35F;
        p.lsx = 0.9F;
        p.rsx = 0.9F;
        p.alx = -0.40F;
        p.alz = -0.20F;
        p.flx = -0.65F;
        p.arx = -0.40F;
        p.arz = 0.20F;
        p.frx = -0.65F;
        p.tlx = 0.45F;
        p.tlz = 0.60F;
        p.trx = 0.45F;
        p.trz = 0.60F;
        p.rolll = 0.9F;
        p.rollr = 0.9F;
        return p;
    }

    static Pz pose(int state, float t, float age, int variant, float walk, float ph) {
        Pz p = idle(age);
        switch (state) {
            case LoboArtico.S_IDLE -> {
                float a = walk;
                p.lx = -0.35F + Mth.cos(ph) * 0.75F * a;
                p.rx = -0.35F - Mth.cos(ph) * 0.75F * a;
                p.lsx = 0.9F + Math.max(0.0F, Mth.sin(ph)) * 0.8F * a;
                p.rsx = 0.9F + Math.max(0.0F, -Mth.sin(ph)) * 0.8F * a;
                p.alx += -Mth.cos(ph) * 0.25F * a;
                p.arx += Mth.cos(ph) * 0.25F * a;
                p.torsoX += 0.06F * a;
                p.cloak += 0.25F * a;
                p.strip += 0.2F * a;
                p.flap += 0.2F * a;
            }
            case LoboArtico.S_WINDUP -> {
                float k = sm(t / 10.0F);
                p.torsoX = lerp(0.28F, 0.85F, k) + Mth.sin(t * 2.6F) * 0.025F * k;
                p.headPitch = lerp(0.3F, -0.55F, k);
                p.jaw = lerp(0.14F, 0.75F, k);
                p.lx = lerp(-0.35F, -0.95F, k) + Mth.sin(t * 0.9F) * 0.35F * k;
                p.rx = lerp(-0.35F, -0.7F, k);
                p.lsx = lerp(0.9F, 1.7F, k);
                p.rsx = lerp(0.9F, 1.55F, k);
                p.alx = lerp(-0.4F, 0.95F, k);
                p.arx = lerp(-0.4F, 0.95F, k);
                p.alz = lerp(-0.2F, -0.35F, k);
                p.arz = lerp(0.2F, 0.35F, k);
                p.flx = lerp(-0.65F, -0.15F, k);
                p.frx = lerp(-0.65F, -0.15F, k);
                p.tlx = lerp(0.45F, -1.35F, k);
                p.trx = lerp(0.45F, -1.35F, k);
                p.tlz = lerp(0.6F, 0.25F, k);
                p.trz = lerp(0.6F, 0.25F, k);
                p.tail = 0.35F * k;
                p.cloak = 0.45F * k;
                p.strip = 0.3F * k;
                p.flap = 0.4F * k;
                p.drape = 0.3F * k;
                p.rolll = lerp(0.9F, 1.4F, k);
                p.rollr = p.rolll;
            }
            case LoboArtico.S_DASH -> {
                float g = t * 0.95F;
                p.torsoX = 0.95F;
                p.headPitch = -0.7F;
                p.jaw = 0.7F;
                p.lx = -0.35F + Mth.sin(g) * 1.0F;
                p.rx = -0.35F - Mth.sin(g) * 1.0F;
                p.lsx = 0.9F + Math.max(0.0F, Mth.cos(g)) * 1.0F;
                p.rsx = 0.9F + Math.max(0.0F, -Mth.cos(g)) * 1.0F;
                p.alx = 1.1F;
                p.arx = 1.1F;
                p.alz = -0.5F;
                p.arz = 0.5F;
                p.flx = 0.05F;
                p.frx = 0.05F;
                p.tlx = -1.55F;
                p.trx = -1.55F;
                p.tlz = 0.3F;
                p.trz = 0.3F;
                p.tail = 0.2F;
                p.cloak = 0.95F;
                p.strip = 0.55F;
                p.flap = 0.95F;
                p.drape = 0.6F;
                p.rolll = 1.5F;
                p.rollr = 1.5F;
            }
            case LoboArtico.S_HOWL -> {
                float k = sm(t / 12.0F);
                float sway = Mth.sin(t * 0.35F) * 0.04F;
                p.torsoX = lerp(0.28F, -0.45F, k) + sway;
                p.headPitch = lerp(0.3F, -1.15F, k);
                p.jaw = lerp(0.14F, 0.95F, k) + Mth.sin(t * 0.6F) * 0.05F * k;
                p.lx = lerp(-0.35F, -0.15F, k);
                p.rx = p.lx;
                p.lsx = lerp(0.9F, 0.55F, k);
                p.rsx = p.lsx;
                p.alx = lerp(-0.4F, -2.3F, k);
                p.arx = p.alx;
                p.alz = lerp(-0.2F, -0.85F, k);
                p.arz = lerp(0.2F, 0.85F, k);
                p.flx = lerp(-0.65F, -0.25F, k);
                p.frx = p.flx;
                p.tlx = lerp(0.45F, 0.05F, k);
                p.trx = p.tlx;
                p.tlz = lerp(0.6F, 0.7F, k);
                p.trz = p.tlz;
                p.tail = 0.5F * k;
                p.cloak = -0.2F * k;
                p.strip = -0.1F * k;
                p.flap = -0.25F * k;
                p.rolll = lerp(0.9F, 0.4F, k);
                p.rollr = p.rolll;
            }
            case LoboArtico.S_SLASH -> {
                boolean leadL = variant != 0;
                float up = kf(t, 0, 0, 10, 1, 14, 0, 22, 0);
                float hit = kf(t, 0, 0, 9, 0, 13, 1, 15, 1, 22, 0);
                float twist = kf(t, 0, 0, 10, -0.7F, 13, 0.7F, 16, 0.55F, 22, 0) * (variant == 0 ? 1.0F : -1.0F);
                p.torsoY = twist;
                p.torsoX = lerp(0.28F, 0.55F, hit) + up * -0.2F;
                p.headPitch = 0.3F - 0.2F * hit;
                p.jaw = 0.14F + 0.7F * hit;
                p.headYaw = -twist * 0.5F;
                p.lx = -0.35F - 0.5F * hit * (leadL ? 0.0F : 1.0F);
                p.rx = -0.35F - 0.5F * hit * (leadL ? 1.0F : 0.0F);
                p.lsx = 0.9F + 0.4F * hit;
                p.rsx = 0.9F + 0.4F * hit;
                float ax = kf(t, 0, -0.4F, 10, -2.55F, 13, -0.3F, 16, -0.2F, 22, -0.4F);
                float az = kf(t, 0, -0.2F, 10, -0.35F, 13, 0.35F, 16, 0.3F, 22, -0.2F);
                float fx = kf(t, 0, -0.65F, 10, -0.4F, 13, -0.25F, 22, -0.65F);
                float tilt = kf(t, 0, 0.45F, 10, -0.9F, 13, 1.75F, 16, 1.6F, 22, 0.45F);
                float lr = kf(t, 0, 0.9F, 10, 0.2F, 13, 0.0F, 22, 0.9F);
                if (leadL) {
                    p.alx = ax;
                    p.alz = az;
                    p.flx = fx;
                    p.tlx = tilt;
                    p.rolll = lr;
                } else {
                    p.arx = ax;
                    p.arz = -az;
                    p.frx = fx;
                    p.trx = tilt;
                    p.rollr = lr;
                }
                p.cloak = 0.3F * hit;
                p.flap = 0.4F * hit;
            }
            case LoboArtico.S_CROSS -> {
                float up = kf(t, 0, 0, 16, 1, 19, 0, 30, 0);
                float hit = kf(t, 0, 0, 16, 0, 20, 1, 23, 1, 30, 0);
                p.torsoX = lerp(0.28F, -0.1F, up) + 0.55F * hit;
                p.headPitch = lerp(0.3F, -0.5F, up) + 0.3F * hit;
                p.jaw = 0.14F + 0.8F * (up + hit) * 0.6F;
                float ax = kf(t, 0, -0.4F, 16, -2.45F, 20, -0.8F, 23, -0.7F, 30, -0.4F);
                float az = kf(t, 0, 0.2F, 16, 0.95F, 20, -0.65F, 23, -0.55F, 30, 0.2F);
                float fx = kf(t, 0, -0.65F, 16, -0.3F, 20, -0.35F, 30, -0.65F);
                float tx = kf(t, 0, 0.45F, 16, -0.2F, 20, 1.7F, 23, 1.55F, 30, 0.45F);
                float tz = kf(t, 0, 0.6F, 16, 0.8F, 20, -0.5F, 23, -0.4F, 30, 0.6F);
                p.alx = ax;
                p.arx = ax;
                p.alz = -az;
                p.arz = az;
                p.flx = fx;
                p.frx = fx;
                p.tlx = tx;
                p.trx = tx;
                p.tlz = tz;
                p.trz = tz;
                p.lx = -0.35F - 0.3F * hit;
                p.rx = p.lx;
                p.cloak = 0.5F * hit - 0.2F * up;
                p.flap = 0.6F * hit;
                p.rolll = kf(t, 0, 0.9F, 16, 0.3F, 20, 0.0F, 30, 0.9F);
                p.rollr = p.rolll;
            }
            case LoboArtico.S_BLIZZARD -> {
                float k = sm(t / 14.0F) * (1.0F - sm((t - 68.0F) / 12.0F));
                p.torsoX = lerp(0.28F, 0.45F, k);
                p.headPitch = lerp(0.3F, -0.35F, k);
                p.jaw = lerp(0.14F, 0.9F, k);
                p.lx = lerp(-0.35F, -0.6F, k);
                p.rx = p.lx;
                p.lsx = lerp(0.9F, 1.35F, k);
                p.rsx = p.lsx;
                p.alx = lerp(-0.4F, -0.25F, k);
                p.arx = p.alx;
                p.alz = lerp(-0.2F, -1.45F, k);
                p.arz = lerp(0.2F, 1.45F, k);
                p.flx = lerp(-0.65F, -0.1F, k);
                p.frx = p.flx;
                p.tlx = lerp(0.45F, 0.1F, k);
                p.trx = p.tlx;
                p.tlz = lerp(0.6F, 1.4F, k);
                p.trz = p.tlz;
                p.rolll = lerp(0.9F, 0.0F, k);
                p.rollr = p.rolll;
                p.cloak = lerp(0.0F, 0.15F, k);
                p.strip = lerp(0.0F, 0.7F, k);
                p.flap = lerp(0.0F, -0.9F, k);
                p.drape = lerp(0.0F, 0.5F, k);
                float v = 0.55F;
                float ang;
                if (t < 14.0F) {
                    ang = v * (t * t) / 28.0F;
                } else if (t < 68.0F) {
                    ang = v * 7.0F + v * (t - 14.0F);
                } else {
                    float tt = Math.min(12.0F, t - 68.0F);
                    ang = v * 7.0F + v * 54.0F + v * (tt - (tt * tt) / 24.0F);
                }
                p.spin = ang;
            }
            case LoboArtico.S_MOON -> {
                float k = sm(t / 14.0F);
                p.torsoX = lerp(0.28F, -0.55F, k);
                p.headPitch = lerp(0.3F, -1.3F, k);
                p.jaw = lerp(0.14F, 1.0F, k);
                p.lx = lerp(-0.35F, -0.2F, k);
                p.rx = p.lx;
                p.lsx = lerp(0.9F, 0.6F, k);
                p.rsx = p.lsx;
                p.alx = lerp(-0.4F, -2.75F, k);
                p.arx = p.alx;
                p.alz = lerp(-0.2F, -0.3F, k);
                p.arz = lerp(0.2F, 0.3F, k);
                p.flx = lerp(-0.65F, -0.15F, k);
                p.frx = p.flx;
                p.tlx = lerp(0.45F, 0.0F, k);
                p.trx = p.tlx;
                p.tlz = lerp(0.6F, -0.25F, k);
                p.trz = p.tlz;
                p.tail = 0.55F * k;
                p.cloak = -0.3F * k;
                p.strip = -0.2F * k;
                p.flap = -0.35F * k;
                p.rolll = lerp(0.9F, 0.5F, k);
                p.rollr = p.rolll;
            }
            case LoboArtico.S_STUN -> {
                float k = sm(t / 8.0F);
                p.torsoX = lerp(0.28F, 0.55F, k) + Mth.sin(age * 0.12F) * 0.03F;
                p.headPitch = lerp(0.3F, 0.75F, k);
                p.headYaw = Mth.sin(t * 1.1F) * 0.6F * (1.0F - clamp01(t / 34.0F));
                p.jaw = lerp(0.14F, 0.35F, k);
                p.lx = lerp(-0.35F, -1.15F, k);
                p.rx = lerp(-0.35F, -1.0F, k);
                p.lsx = lerp(0.9F, 2.0F, k);
                p.rsx = lerp(0.9F, 1.9F, k);
                p.alx = lerp(-0.4F, -0.1F, k);
                p.arx = p.alx;
                p.flx = lerp(-0.65F, -0.3F, k);
                p.frx = p.flx;
                p.tlx = lerp(0.45F, 1.35F, k);
                p.trx = p.tlx;
                p.tlz = lerp(0.6F, 0.5F, k);
                p.trz = p.tlz;
                p.cloak = 0.1F * k;
                p.rolll = lerp(0.9F, 0.0F, k);
                p.rollr = p.rolll;
            }
            case LoboArtico.S_SKATE -> {
                float sway = Mth.sin(t * 0.12F) * 0.25F;
                p.torsoX = 0.6F;
                p.torsoZ = sway * 0.5F;
                p.headPitch = -0.2F;
                p.jaw = 0.6F;
                p.lx = -1.0F;
                p.rx = 0.25F;
                p.lsx = 1.5F;
                p.rsx = 0.4F;
                p.alx = 0.7F;
                p.arx = 0.7F;
                p.alz = -0.75F;
                p.arz = 0.75F;
                p.flx = -0.2F;
                p.frx = -0.2F;
                p.tlx = -1.3F;
                p.trx = -1.3F;
                p.tlz = 0.35F;
                p.trz = 0.35F;
                p.cloak = 0.9F;
                p.strip = 0.5F;
                p.flap = 0.8F;
                p.tail = 0.1F;
                p.rolll = 1.2F;
                p.rollr = 1.2F;
            }
            default -> {
            }
        }
        return p;
    }

    /** Se arrodilla y se desploma hacia delante al morir (k de 0 a 1). */
    private static void kneel(Pz p, float k) {
        p.spin = lerp(p.spin, 0.0F, k);
        p.torsoX = lerp(p.torsoX, 1.1F, k);
        p.torsoY = lerp(p.torsoY, 0.0F, k);
        p.torsoZ = lerp(p.torsoZ, 0.0F, k);
        p.headPitch = lerp(p.headPitch, 0.9F, k);
        p.headYaw = lerp(p.headYaw, 0.0F, k);
        p.jaw = lerp(p.jaw, 0.55F, k);
        p.lx = lerp(p.lx, -1.4F, k);
        p.rx = lerp(p.rx, -1.15F, k);
        p.lsx = lerp(p.lsx, 2.3F, k);
        p.rsx = lerp(p.rsx, 2.1F, k);
        p.alx = lerp(p.alx, 0.25F, k);
        p.arx = lerp(p.arx, 0.15F, k);
        p.alz = lerp(p.alz, -0.55F, k);
        p.arz = lerp(p.arz, 0.55F, k);
        p.flx = lerp(p.flx, -0.2F, k);
        p.frx = lerp(p.frx, -0.2F, k);
        p.tlx = lerp(p.tlx, 1.25F, k);
        p.trx = lerp(p.trx, 1.35F, k);
        p.tlz = lerp(p.tlz, 0.9F, k);
        p.trz = lerp(p.trz, 0.9F, k);
        p.rolll = lerp(p.rolll, 0.0F, k);
        p.rollr = lerp(p.rollr, 0.0F, k);
        p.cloak = lerp(p.cloak, 0.35F, k);
        p.strip = lerp(p.strip, 0.2F, k);
        p.flap = lerp(p.flap, 0.3F, k);
        p.tail = lerp(p.tail, -0.2F, k);
    }

    private static float legHeight(float lx, float sx) {
        return 12.5F * Mth.cos(lx) + 12.5F * Mth.cos(lx + sx) + 3.5F;
    }

    // ------------------------------------------------------------------ animación

    @Override
    public void setupAnim(LoboArtico e, float limbSwing, float limbSwingAmount, float age, float netHeadYaw, float headPitch) {
        float pt = age - e.tickCount;
        int state = e.state();
        float t = e.stateAge(pt);
        float walk = Mth.clamp(limbSwingAmount * 1.5F, 0.0F, 1.0F);
        float ph = limbSwing * 0.6F;
        Pz p = pose(state, t, state == LoboArtico.S_IDLE ? age : t, e.variant(), walk, ph);
        float dth = e.deathProgress(pt);
        if (dth > 0.0F) {
            kneel(p, sm(dth * 2.2F));
        }

        // hundirse sobre las piernas dobladas (el cuerpo baja lo que suben los pies respecto a la guardia)
        float h0 = legHeight(-0.35F, 0.9F);
        float h = (legHeight(p.lx, p.lsx) + legHeight(p.rx, p.rsx)) * 0.5F;
        float drop = h0 - h;
        this.pelvis.y = Y_PELVIS + drop;
        this.torso.y = Y_TORSO + drop;
        this.legL.y = Y_LEG + drop;
        this.legR.y = Y_LEG + drop;
        this.tailA.y = Y_TAIL + drop;

        this.root.yRot = p.spin;
        this.torso.xRot = p.torsoX;
        this.torso.yRot = p.torsoY;
        this.torso.zRot = p.torsoZ;
        this.neck.xRot = p.neckX;
        float look = state == LoboArtico.S_BLIZZARD || dth > 0.0F ? 0.0F : 1.0F;
        this.head.xRot = p.headPitch - p.torsoX - p.neckX + headPitch * ((float) Math.PI / 180.0F) * 0.25F * look;
        this.head.yRot = p.headYaw + Mth.clamp(netHeadYaw, -50.0F, 50.0F) * ((float) Math.PI / 180.0F) * 0.5F * look;
        this.jaw.xRot = p.jaw;

        this.legL.xRot = p.lx;
        this.shinL.xRot = p.lsx;
        this.footL.xRot = -(p.lx + p.lsx);
        this.legR.xRot = p.rx;
        this.shinR.xRot = p.rsx;
        this.footR.xRot = -(p.rx + p.rsx);

        this.armL.xRot = p.alx;
        this.armL.yRot = 0.0F;
        this.armL.zRot = p.alz;
        this.foreL.xRot = p.flx;
        this.gripL.xRot = p.tlx - (p.alx + p.flx);
        this.gripL.yRot = 0.0F;
        this.gripL.zRot = p.tlz - p.alz;
        this.scyL.xRot = 0.0F;
        this.scyL.yRot = -p.rolll;
        this.scyL.zRot = 0.0F;

        this.armR.xRot = p.arx;
        this.armR.yRot = 0.0F;
        this.armR.zRot = p.arz;
        this.foreR.xRot = p.frx;
        this.gripR.xRot = p.trx - (p.arx + p.frx);
        this.gripR.yRot = 0.0F;
        this.gripR.zRot = -p.trz - p.arz;
        this.scyR.xRot = 0.0F;
        this.scyR.yRot = p.rollr;
        this.scyR.zRot = 0.0F;

        // capa, capucha y cola se mecen solas
        float sw = Mth.sin(age * 0.09F);
        float sw2 = Mth.sin(age * 0.13F + 1.0F);
        this.cloakA.xRot = p.cloak + sw * 0.035F;
        this.cloakB.xRot = p.cloak * 0.7F + p.strip * 0.3F + sw2 * 0.05F;
        this.cloakCl.xRot = p.strip + Mth.sin(age * 0.11F + 0.4F) * 0.07F;
        this.cloakCm.xRot = p.strip + Mth.sin(age * 0.11F + 1.7F) * 0.07F;
        this.cloakCr.xRot = p.strip + Mth.sin(age * 0.11F + 2.9F) * 0.07F;
        this.flapL.xRot = p.flap + Mth.sin(age * 0.1F) * 0.04F;
        this.flapR.xRot = p.flap + Mth.sin(age * 0.1F + 0.8F) * 0.04F;
        this.hoodDrape.xRot = 0.3F + p.drape + sw * 0.03F;
        this.tailA.xRot = 0.2F + p.tail;
        this.tailA.yRot = Mth.sin(age * 0.07F) * 0.1F;
        this.tailB.xRot = this.tailBx + Mth.sin(age * 0.09F + 0.7F) * 0.08F;
        this.tailB.yRot = Mth.sin(age * 0.07F + 0.9F) * 0.14F;
        this.tailC.xRot = this.tailCx + Mth.sin(age * 0.1F + 1.4F) * 0.1F;
        this.tailC.yRot = Mth.sin(age * 0.07F + 1.8F) * 0.2F;
        this.chainL.xRot = this.chainLx + Mth.sin(age * 0.1F) * 0.08F + p.cloak * 0.4F;
        this.chainR.xRot = this.chainRx + Mth.sin(age * 0.1F + 1.2F) * 0.08F + p.cloak * 0.4F;
        this.beard.xRot = this.beardX + Mth.sin(age * 0.08F) * 0.04F - p.torsoX * 0.1F;
    }

    @Override
    public void renderToBuffer(PoseStack pose, VertexConsumer buffer, int light, int overlay, float r, float g, float b, float a) {
        this.root.render(pose, buffer, light, overlay, r, g, b, a);
    }
}
