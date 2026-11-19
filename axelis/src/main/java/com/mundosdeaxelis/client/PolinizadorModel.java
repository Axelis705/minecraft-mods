package com.mundosdeaxelis.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mundosdeaxelis.MundosDeAxelis;
import com.mundosdeaxelis.entity.Polinizador;
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

/** Modelo del Polinizador: un insecto-flor con cuatro alas, collar de pétalos alrededor de la cara y un aguijón de espora. */
public class PolinizadorModel extends EntityModel<Polinizador> {
    private ModelPart legA;
    private ModelPart legB;
    private ModelPart pollen;
    private ModelPart sting;
    private ModelPart petalUp;
    private ModelPart petalDn;
    private ModelPart petalL;
    private ModelPart petalR;

    public static final ModelLayerLocation LAYER =
            new ModelLayerLocation(new ResourceLocation(MundosDeAxelis.MODID, "polinizador"), "main");

    private final ModelPart root;
    private final ModelPart body;
    private final ModelPart head;
    private final ModelPart tail;
    private final ModelPart wingR1;
    private final ModelPart wingR2;
    private final ModelPart wingL1;
    private final ModelPart wingL2;
    private final ModelPart antennaL;
    private final ModelPart antennaR;

    public PolinizadorModel(ModelPart root) {
        this.root = root;
        this.body = root.getChild("body");
        this.head = root.getChild("body").getChild("head");
        this.tail = root.getChild("body").getChild("tail");
        this.wingR1 = root.getChild("body").getChild("wing_r1");
        this.wingR2 = root.getChild("body").getChild("wing_r2");
        this.wingL1 = root.getChild("body").getChild("wing_l1");
        this.wingL2 = root.getChild("body").getChild("wing_l2");
        this.antennaL = root.getChild("body").getChild("head").getChild("antenna_l");
        this.antennaR = root.getChild("body").getChild("head").getChild("antenna_r");
        this.legA = this.body.getChild("leg_a");
        this.legB = this.body.getChild("leg_b");
        this.pollen = this.body.getChild("pollen");
        this.sting = this.tail.getChild("sting");
        this.petalUp = this.head.getChild("petal_up");
        this.petalDn = this.head.getChild("petal_dn");
        this.petalL = this.head.getChild("petal_l");
        this.petalR = this.head.getChild("petal_r");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition r = mesh.getRoot();
        PartDefinition bodyPart = r.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-3F, -3F, -4.5F, 6F, 6F, 9F), PartPose.offset(0F, 16F, 0F));
        PartDefinition headPart = bodyPart.addOrReplaceChild("head", CubeListBuilder.create().texOffs(30, 0).addBox(-2.5F, -2.5F, -5F, 5F, 5F, 5F), PartPose.offset(0F, 0F, -4.5F));
        PartDefinition antenna_lPart = headPart.addOrReplaceChild("antenna_l", CubeListBuilder.create().texOffs(40, 24).addBox(-0.5F, -4F, -0.5F, 1F, 4F, 1F), PartPose.offsetAndRotation(1.5F, -2.5F, -3F, -0.5F, 0F, 0.3F));
        PartDefinition antenna_rPart = headPart.addOrReplaceChild("antenna_r", CubeListBuilder.create().texOffs(44, 24).addBox(-0.5F, -4F, -0.5F, 1F, 4F, 1F), PartPose.offsetAndRotation(-1.5F, -2.5F, -3F, -0.5F, 0F, -0.3F));
        PartDefinition petal_upPart = headPart.addOrReplaceChild("petal_up", CubeListBuilder.create().texOffs(36, 29).addBox(-2F, -5.5F, -5.5F, 4F, 3F, 1F), PartPose.offset(0F, 0F, 0F));
        PartDefinition petal_dnPart = headPart.addOrReplaceChild("petal_dn", CubeListBuilder.create().texOffs(46, 29).addBox(-2F, 2.5F, -5.5F, 4F, 3F, 1F), PartPose.offset(0F, 0F, 0F));
        PartDefinition petal_lPart = headPart.addOrReplaceChild("petal_l", CubeListBuilder.create().texOffs(24, 24).addBox(2.5F, -2F, -5.5F, 3F, 4F, 1F), PartPose.offset(0F, 0F, 0F));
        PartDefinition petal_rPart = headPart.addOrReplaceChild("petal_r", CubeListBuilder.create().texOffs(32, 24).addBox(-5.5F, -2F, -5.5F, 3F, 4F, 1F), PartPose.offset(0F, 0F, 0F));
        PartDefinition tailPart = bodyPart.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(0, 15).addBox(-2F, -2F, 0F, 4F, 4F, 5F), PartPose.offset(0F, 0F, 4.5F));
        PartDefinition stingPart = tailPart.addOrReplaceChild("sting", CubeListBuilder.create().texOffs(56, 29).addBox(-0.5F, -0.5F, 5F, 1F, 1F, 3F), PartPose.offset(0F, 0F, 0F));
        PartDefinition pollenPart = bodyPart.addOrReplaceChild("pollen", CubeListBuilder.create().texOffs(18, 15).addBox(-1.5F, 2.5F, -1.5F, 3F, 3F, 3F), PartPose.offset(0F, 0F, 0F));
        PartDefinition wing_r1Part = bodyPart.addOrReplaceChild("wing_r1", CubeListBuilder.create().texOffs(30, 15).addBox(0F, -0.5F, -2F, 8F, 1F, 4F), PartPose.offsetAndRotation(2.5F, -3F, -1.5F, 0F, 0F, -0.3F));
        PartDefinition wing_r2Part = bodyPart.addOrReplaceChild("wing_r2", CubeListBuilder.create().texOffs(0, 29).addBox(0F, -0.5F, -1.5F, 6F, 1F, 3F), PartPose.offsetAndRotation(2.5F, -3F, 2F, 0F, 0F, -0.2F));
        PartDefinition wing_l1Part = bodyPart.addOrReplaceChild("wing_l1", CubeListBuilder.create().texOffs(0, 24).addBox(-8F, -0.5F, -2F, 8F, 1F, 4F), PartPose.offsetAndRotation(-2.5F, -3F, -1.5F, 0F, 0F, 0.3F));
        PartDefinition wing_l2Part = bodyPart.addOrReplaceChild("wing_l2", CubeListBuilder.create().texOffs(18, 29).addBox(-6F, -0.5F, -1.5F, 6F, 1F, 3F), PartPose.offsetAndRotation(-2.5F, -3F, 2F, 0F, 0F, 0.2F));
        PartDefinition leg_aPart = bodyPart.addOrReplaceChild("leg_a", CubeListBuilder.create().texOffs(0, 33).addBox(-0.5F, 0F, -0.5F, 1F, 3F, 1F), PartPose.offset(-1.5F, 3F, -1.5F));
        PartDefinition leg_bPart = bodyPart.addOrReplaceChild("leg_b", CubeListBuilder.create().texOffs(4, 33).addBox(-0.5F, 0F, -0.5F, 1F, 3F, 1F), PartPose.offset(1.5F, 3F, -1.5F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(Polinizador e, float limbSwing, float limbSwingAmount, float age, float netHeadYaw, float headPitch) {
        float pt = Anim.pt(e, age);
        Motion m = Motion.of(e);
        boolean ch = e.isCharging();
        float charge = m.smooth(0, ch ? 1.0F : 0.0F, 0.22F, pt);
        float windup = Mth.clamp(m.sinceStart(1, ch, e, pt) / 8.0F, 0.0F, 1.0F);
        float after = Motion.pulse(m.sinceEnd(1, ch, e, pt), 10.0F);
        float hurt = Anim.hurt(e, pt);
        float jig = m.jiggle(pt);
        float dth = Anim.death(e, pt);
        float speed = Math.min(1.0F, m.speed(pt) * 6.0F);
        float sway = m.sway(pt), drag = m.drag(pt);
        // alas: aleteo rapidísimo (más al cargar y al ir deprisa); el par de atrás va un poco a destiempo; al morir se paran
        float beat = 1.9F + 0.9F * charge + 0.5F * speed;
        float amp = (0.65F + 0.25F * charge) * (1.0F - dth);
        float f1 = Mth.sin(age * beat) * amp, f2 = Mth.sin(age * beat - 0.9F) * amp * 0.85F;
        this.wingR1.zRot = -0.3F - f1 - dth * 0.6F;
        this.wingR2.zRot = -0.2F - f2 - dth * 0.5F;
        this.wingL1.zRot = 0.3F + f1 + dth * 0.6F;
        this.wingL2.zRot = 0.2F + f2 + dth * 0.5F;
        this.wingR1.yRot = -0.15F * speed;
        this.wingL1.yRot = 0.15F * speed;
        this.wingR2.yRot = -0.2F * speed;
        this.wingL2.yRot = 0.2F * speed;
        // cuerpo: flota en ocho (sube y baja y se mece), apunta al cargar y retrocede tras la embestida
        float hover = Mth.sin(age * 0.2F) * 1.2F + Mth.sin(age * 0.47F) * 0.35F;
        this.body.y = 16.0F + hover * (1.0F - charge * 0.6F) - after * 1.5F;
        this.body.x = Mth.sin(age * 0.1F) * 0.5F * (1.0F - charge);
        this.body.xRot = 0.1F + Mth.sin(age * 0.15F) * 0.05F - charge * 0.6F + windup * Mth.sin(age * 2.6F) * 0.06F
                + after * 0.35F - hurt * 0.4F + speed * 0.2F;
        this.body.zRot = Mth.sin(age * 0.1F + 1.0F) * 0.06F + jig * 0.2F + sway * 0.4F;
        // cabeza: mira al objetivo; los pétalos de la cara se abren al atacar
        this.head.xRot = headPitch * Anim.DEG * 0.5F + charge * 0.3F;
        this.head.yRot = netHeadYaw * Anim.DEG * 0.7F;
        this.head.zRot = -jig * 0.15F;
        float bloom = 1.0F + charge * 0.35F + Mth.sin(age * 0.3F) * 0.04F;
        this.petalUp.yScale = bloom;
        this.petalDn.yScale = bloom;
        this.petalL.xScale = bloom;
        this.petalR.xScale = bloom;
        // cola y aguijón: se curva para picar y late
        this.tail.xRot = 0.35F + Mth.sin(age * 0.25F) * 0.1F + charge * 0.75F - after * 0.3F + drag * -0.15F;
        this.tail.yRot = -sway * 0.5F;
        float st = 1.0F + charge * (0.35F + 0.15F * Mth.sin(age * 1.2F));
        this.sting.zScale = st;
        // antenas: tiemblan, se quedan atrás al volar rápido y rebotan con los golpes
        this.antennaL.xRot = -0.5F + Mth.sin(age * 0.3F) * 0.1F + drag * 0.5F + jig * 0.3F;
        this.antennaR.xRot = -0.5F - Mth.sin(age * 0.3F) * 0.1F + drag * 0.5F - jig * 0.3F;
        this.antennaL.zRot = 0.3F + sway * 0.4F + charge * 0.25F;
        this.antennaR.zRot = -0.3F + sway * 0.4F - charge * 0.25F;
        // patitas colgando y la bola de polen que se balancea como un péndulo
        this.legA.xRot = 0.25F + drag * 0.7F + Mth.sin(age * 0.33F) * 0.15F;
        this.legB.xRot = 0.25F + drag * 0.7F + Mth.sin(age * 0.33F + 1.4F) * 0.15F;
        this.pollen.xRot = drag * 0.35F + Mth.sin(age * 0.2F + 0.8F) * 0.12F;
        this.pollen.zRot = sway * 0.6F + jig * 0.2F;
    }

    @Override
    public void renderToBuffer(PoseStack pose, VertexConsumer buffer, int light, int overlay, float r, float g, float b, float a) {
        this.root.render(pose, buffer, light, overlay, r, g, b, a);
    }
}
