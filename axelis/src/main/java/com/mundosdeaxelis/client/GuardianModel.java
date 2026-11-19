package com.mundosdeaxelis.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mundosdeaxelis.MundosDeAxelis;
import com.mundosdeaxelis.entity.GuardianMusgo;
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

/** Modelo del Guardián de Musgo: un gólem de piedra cubierto de musgo con un arbolito que le crece en el hombro. */
public class GuardianModel extends EntityModel<GuardianMusgo> {
    private ModelPart mossCape;
    private ModelPart trunk;
    private ModelPart crownA;
    private ModelPart crownB;
    private ModelPart flower;

    public static final ModelLayerLocation LAYER =
            new ModelLayerLocation(new ResourceLocation(MundosDeAxelis.MODID, "guardian_musgo"), "main");

    private final ModelPart root;
    private final ModelPart head;
    private final ModelPart body;
    private final ModelPart armR;
    private final ModelPart armL;
    private final ModelPart legR;
    private final ModelPart legL;

    public GuardianModel(ModelPart root) {
        this.root = root;
        this.head = root.getChild("head");
        this.body = root.getChild("body");
        this.armR = root.getChild("arm_r");
        this.armL = root.getChild("arm_l");
        this.legR = root.getChild("leg_r");
        this.legL = root.getChild("leg_l");
        this.mossCape = root.getChild("moss_cape");
        this.trunk = root.getChild("trunk");
        this.crownA = root.getChild("crown_a");
        this.crownB = root.getChild("crown_b");
        this.flower = root.getChild("flower");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition r = mesh.getRoot();
        PartDefinition headPart = r.addOrReplaceChild("head", CubeListBuilder.create().texOffs(28, 24).addBox(-4.5F, -9F, -4.5F, 9F, 9F, 9F), PartPose.offset(0F, -2F, -1F));
        PartDefinition bodyPart = r.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-8F, -14F, -5F, 16F, 14F, 10F), PartPose.offset(0F, 12F, 0F));
        PartDefinition moss_capePart = r.addOrReplaceChild("moss_cape", CubeListBuilder.create().texOffs(48, 43).addBox(-7F, -15F, -4.5F, 14F, 3F, 9F), PartPose.offset(0F, 12F, 0F));
        PartDefinition leg_rPart = r.addOrReplaceChild("leg_r", CubeListBuilder.create().texOffs(100, 0).addBox(-3.5F, 0F, -3.5F, 7F, 12F, 7F), PartPose.offset(-4.5F, 12F, 0F));
        PartDefinition leg_lPart = r.addOrReplaceChild("leg_l", CubeListBuilder.create().texOffs(0, 24).addBox(-3.5F, 0F, -3.5F, 7F, 12F, 7F), PartPose.offset(4.5F, 12F, 0F));
        PartDefinition arm_rPart = r.addOrReplaceChild("arm_r", CubeListBuilder.create().texOffs(52, 0).addBox(-3F, 0F, -3F, 6F, 14F, 6F), PartPose.offset(-11F, 0F, 0F));
        PartDefinition arm_lPart = r.addOrReplaceChild("arm_l", CubeListBuilder.create().texOffs(76, 0).addBox(-3F, 0F, -3F, 6F, 14F, 6F), PartPose.offset(11F, 0F, 0F));
        PartDefinition fist_rPart = arm_rPart.addOrReplaceChild("fist_r", CubeListBuilder.create().texOffs(64, 24).addBox(-4F, 12F, -4F, 8F, 8F, 8F), PartPose.offset(0F, 0F, 0F));
        PartDefinition fist_lPart = arm_lPart.addOrReplaceChild("fist_l", CubeListBuilder.create().texOffs(96, 24).addBox(-4F, 12F, -4F, 8F, 8F, 8F), PartPose.offset(0F, 0F, 0F));
        PartDefinition trunkPart = r.addOrReplaceChild("trunk", CubeListBuilder.create().texOffs(36, 43).addBox(-1.5F, -10F, -1.5F, 3F, 10F, 3F), PartPose.offset(7F, -2F, 0F));
        PartDefinition crown_aPart = r.addOrReplaceChild("crown_a", CubeListBuilder.create().texOffs(0, 43).addBox(-4.5F, -4F, -4.5F, 9F, 4F, 9F), PartPose.offset(7F, -12F, 0F));
        PartDefinition crown_bPart = r.addOrReplaceChild("crown_b", CubeListBuilder.create().texOffs(94, 43).addBox(-2.5F, -3F, -2.5F, 5F, 3F, 5F), PartPose.offset(7F, -16F, 0F));
        PartDefinition flowerPart = r.addOrReplaceChild("flower", CubeListBuilder.create().texOffs(114, 43).addBox(-1F, -2F, -1F, 2F, 2F, 2F), PartPose.offset(7F, -19F, 0F));
        return LayerDefinition.create(mesh, 128, 128);
    }

    @Override
    public void setupAnim(GuardianMusgo e, float limbSwing, float limbSwingAmount, float age, float netHeadYaw, float headPitch) {
        float pt = Anim.pt(e, age);
        Motion m = Motion.of(e);
        float walk = Math.min(1.0F, limbSwingAmount);
        float ph = limbSwing * 0.5F;
        float charge = e.slamProgress();
        boolean slamming = charge > 0.0F;
        float ch = m.smooth(0, charge, 0.5F, pt);
        float slam = Motion.pulse(m.sinceEnd(0, slamming, e, pt), 14.0F);
        float u = Anim.swing(m, e, pt, 20.0F);
        float hitArm = Anim.smash(u);
        float atk = Anim.impact(u);
        float hurt = Anim.hurt(e, pt);
        float jig = m.jiggle(pt);
        float breath = Anim.breath(age, 0.07F);
        float sway = m.sway(pt), drag = m.drag(pt);
        // piernas: pasos lentos y pesados, levanta la rodilla
        float swing = Mth.cos(ph) * 0.9F * walk;
        this.legR.xRot = swing;
        this.legL.xRot = -swing;
        this.legR.y = 12.0F - Math.max(0.0F, Mth.sin(ph)) * 1.5F * walk;
        this.legL.y = 12.0F - Math.max(0.0F, -Mth.sin(ph)) * 1.5F * walk;
        // cuerpo: se inclina hacia atrás al cargar el golpe de suelo y se echa adelante con todo al soltarlo
        float bodyX = -ch * 0.22F + slam * 0.42F + atk * 0.3F + hitArm * -0.05F - hurt * 0.12F + breath * 0.02F;
        float roll = Mth.sin(ph) * 0.06F * walk + jig * 0.04F;
        this.body.xRot = bodyX;
        this.body.zRot = roll;
        this.body.y = 12.0F - breath * 0.3F;
        this.mossCape.xRot = bodyX;
        this.mossCape.zRot = roll;
        this.mossCape.y = 12.0F - breath * 0.3F - Math.abs(Mth.sin(ph)) * 0.6F * walk + jig * 0.5F;
        // cabeza sobre el cuerpo
        Anim.follow(this.head, 12.0F, 0.0F, 0.0F, -2.0F, -1.0F, bodyX);
        this.head.yRot = netHeadYaw * Anim.DEG * 0.6F + Anim.idleLook(e, age) * 0.6F * (1.0F - walk);
        this.head.xRot = headPitch * Anim.DEG * 0.6F - ch * 0.25F + slam * 0.3F - hurt * 0.3F;
        this.head.zRot = roll + jig * 0.08F;
        // brazos: alza los puños por encima de la cabeza (temblando de fuerza) y los estrella contra el suelo
        float lift = -2.75F * ch + Mth.sin(age * 1.8F) * 0.06F * ch;
        float hit = hitArm;
        float idle = Mth.sin(age * 0.06F) * 0.04F;
        float armSwing = Mth.cos(ph) * 0.55F * walk;
        this.armR.xRot = lift + hit - armSwing + idle + slam * 0.9F;
        this.armL.xRot = lift + hit + armSwing - idle + slam * 0.9F;
        this.armR.zRot = 0.08F + roll + hurt * 0.25F + ch * 0.2F;
        this.armL.zRot = -0.08F + roll - hurt * 0.25F - ch * 0.2F;
        Anim.follow(this.armR, 12.0F, 0.0F, -11.0F, -breath * 0.3F, 0.0F, bodyX);
        Anim.follow(this.armL, 12.0F, 0.0F, 11.0F, -breath * 0.3F, 0.0F, bodyX);
        // el arbolito del hombro: se mece, se queda atrás al andar y se sacude con los golpes (la copa va pegada al tronco)
        float tx = Mth.sin(age * 0.08F) * 0.05F + drag * 0.12F + jig * 0.15F + (slam + atk) * Mth.sin(age * 2.5F) * 0.15F - Math.abs(Mth.sin(ph)) * 0.05F * walk;
        float tz = Mth.cos(age * 0.07F) * 0.05F + sway * 0.3F + roll;
        this.trunk.xRot = tx;
        this.trunk.zRot = tz;
        // la base del tronco va sobre el hombro, que se mueve con el cuerpo
        Anim.follow(this.trunk, 12.0F, 0.0F, 7.0F, -2.0F, 0.0F, bodyX);
        this.trunk.xRot = tx + bodyX;
        place(this.crownA, -10.0F, tx + bodyX, tz);
        place(this.crownB, -14.0F, (tx + bodyX) * 1.0F + tx * 0.15F, tz * 1.15F);
        place(this.flower, -17.0F, (tx + bodyX) + tx * 0.3F, tz * 1.3F);
        this.crownA.xRot = tx + bodyX;
        this.crownA.zRot = tz;
        this.crownB.xRot = tx * 1.15F + bodyX;
        this.crownB.zRot = tz * 1.15F;
        this.flower.xRot = tx * 1.3F + bodyX;
        this.flower.zRot = tz * 1.3F + Mth.sin(age * 0.2F) * 0.1F;
        float bloom = 1.0F + Mth.sin(age * 0.1F) * 0.06F;
        this.flower.xScale = bloom;
        this.flower.yScale = bloom;
        this.flower.zScale = bloom;
    }

    /** Coloca una pieza de la copa a {@code up} píxeles por encima de la base del tronco, inclinada con él. */
    private void place(ModelPart p, float up, float xr, float zr) {
        p.x = this.trunk.x - Mth.sin(zr) * up;
        p.y = this.trunk.y + up * Mth.cos(xr) * Mth.cos(zr);
        p.z = this.trunk.z + up * Mth.sin(xr);
    }

    @Override
    public void renderToBuffer(PoseStack pose, VertexConsumer buffer, int light, int overlay, float r, float g, float b, float a) {
        this.root.render(pose, buffer, light, overlay, r, g, b, a);
    }
}
