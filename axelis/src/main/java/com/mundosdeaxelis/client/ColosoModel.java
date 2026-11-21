package com.mundosdeaxelis.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mundosdeaxelis.MundosDeAxelis;
import com.mundosdeaxelis.entity.ColosoHierro;
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
 * Modelo del Coloso de Hierro (etapa 267): un gólem gigante de hierro oxidado. Torso ancho con un horno encendido en el pecho, cabeza
 * pequeña hundida entre los hombros (una rendija de ojos que brilla), hombreras remachadas, brazos larguísimos con puños enormes que
 * casi tocan el suelo, piernas cortas y gruesas y dos chimeneas en la espalda.
 * <p>Animaciones: anda pesado (balanceo de brazos y piernas), duerme con la cabeza gacha, levanta los dos puños por encima de la
 * cabeza al cargar el golpe de suelo y los deja caer de golpe, echa el brazo atrás para lanzar lingotes y, recalentado, se encorva
 * temblando.
 */
public class ColosoModel extends EntityModel<ColosoHierro> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(new ResourceLocation(MundosDeAxelis.MODID, "coloso_hierro"), "main");

    private final ModelPart root;
    private final ModelPart body;
    private final ModelPart head;
    private final ModelPart armR;
    private final ModelPart armL;
    private final ModelPart foreR;
    private final ModelPart foreL;
    private final ModelPart legR;
    private final ModelPart legL;

    public ColosoModel(ModelPart root) {
        this.root = root;
        this.body = root.getChild("body");
        this.head = body.getChild("head");
        this.armR = root.getChild("arm_r");
        this.armL = root.getChild("arm_l");
        this.foreR = armR.getChild("fore_r");
        this.foreL = armL.getChild("fore_l");
        this.legR = root.getChild("leg_r");
        this.legL = root.getChild("leg_l");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition r = mesh.getRoot();
        PartDefinition body = r.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-11F, -22F, -7F, 22F, 17F, 14F)
                .texOffs(0, 31).addBox(-7F, -5F, -5F, 14F, 5F, 10F)
                .texOffs(48, 31).addBox(-5F, -18F, -8F, 10F, 8F, 1F)
                .texOffs(104, 15).addBox(3F, -26F, 5F, 3F, 8F, 3F)
                .texOffs(116, 15).addBox(-6F, -25F, 5F, 3F, 7F, 3F), PartPose.offset(0F, 10F, 0F));
        body.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(72, 0).addBox(-4F, -7F, -4.5F, 8F, 7F, 8F)
                .texOffs(72, 15).addBox(-5F, -5F, -5.5F, 10F, 2F, 2F), PartPose.offset(0F, -21F, -2F));
        for (int side = 0; side < 2; side++) {
            boolean right = side == 0;
            float x = right ? -14.5F : 14.5F;
            PartDefinition arm = r.addOrReplaceChild(right ? "arm_r" : "arm_l", CubeListBuilder.create()
                    .texOffs(right ? 64 : 96, 46).addBox(-4F, -2F, -4F, 8F, 14F, 8F)
                    .texOffs(80, right ? 68 : 83).addBox(right ? -6F : -5F, -5F, -5F, 11F, 5F, 10F), PartPose.offset(x, -8F, 0F));
            arm.addOrReplaceChild(right ? "fore_r" : "fore_l", CubeListBuilder.create()
                    .texOffs(right ? 0 : 40, 68).addBox(-5F, 0F, -5F, 10F, 14F, 10F), PartPose.offset(0F, 12F, 0F));
            r.addOrReplaceChild(right ? "leg_r" : "leg_l", CubeListBuilder.create()
                    .texOffs(right ? 0 : 32, 46).addBox(-4F, 0F, -4F, 8F, 14F, 8F), PartPose.offset(right ? -5.5F : 5.5F, 10F, 0F));
        }
        return LayerDefinition.create(mesh, 128, 128);
    }

    @Override
    public void setupAnim(ColosoHierro e, float limbSwing, float limbSwingAmount, float age, float netHeadYaw, float headPitch) {
        float pt = Anim.pt(e, age);
        Motion m = Motion.of(e);
        int st = e.state();
        float walk = Math.min(1.0F, limbSwingAmount);
        float ph = limbSwing * 0.42F;
        float sleep = m.smooth(0, st == ColosoHierro.ASLEEP ? 1.0F : 0.0F, 0.12F, pt);
        float hot = m.smooth(1, st == ColosoHierro.HOT ? 1.0F : 0.0F, 0.15F, pt);
        float chargeT = m.sinceStart(2, st == ColosoHierro.CHARGE, e, pt);
        float charge = chargeT < 0 ? 0.0F : Anim.easeOut(Mth.clamp(chargeT / ColosoHierro.CHARGE_TICKS, 0.0F, 1.0F));
        float slam = Motion.pulse(m.sinceEnd(3, st == ColosoHierro.CHARGE, e, pt), 12.0F);
        float throwT = m.sinceStart(4, st == ColosoHierro.THROW, e, pt);
        float breath = Mth.sin(age * 0.08F) * 0.5F;

        // cuerpo: balanceo al andar, respiración, inclinarse atrás al cargar, caer adelante al golpear, encorvarse recalentado
        body.xRot = -0.18F * charge + 0.32F * slam + 0.3F * hot + 0.06F * sleep;
        body.zRot = Mth.cos(ph) * 0.05F * walk + (hot > 0.01F ? Mth.sin(age * 1.7F) * 0.025F * hot : 0.0F);
        body.y = 10F + Math.abs(Mth.cos(ph)) * 1.2F * walk + breath * (1.0F - sleep) + 1.5F * slam;

        // cabeza: mira al jugador; dormido o recalentado, gacha
        head.yRot = netHeadYaw * Anim.DEG * (1.0F - sleep) * 0.7F;
        head.xRot = headPitch * Anim.DEG * (1.0F - sleep) * 0.6F + 0.45F * sleep + 0.35F * hot;

        // piernas
        legR.xRot = Mth.cos(ph) * 0.55F * walk;
        legL.xRot = -Mth.cos(ph) * 0.55F * walk;
        legR.y = legL.y = 10F;

        // brazos: balanceo al andar; arriba al cargar; abajo de golpe al soltar
        float swingA = -Mth.cos(ph) * 0.45F * walk;
        float up = -2.75F * charge;
        float down = 0.55F * slam;
        armR.xRot = swingA * (1.0F - charge) + up + down + 0.25F * hot;
        armL.xRot = -swingA * (1.0F - charge) + up + down + 0.25F * hot;
        armR.zRot = 0.12F + 0.25F * charge - 0.05F * breath;
        armL.zRot = -0.12F - 0.25F * charge + 0.05F * breath;
        foreR.xRot = -0.25F * charge - 0.15F * walk - 0.1F * sleep;
        foreL.xRot = -0.25F * charge - 0.15F * walk - 0.1F * sleep;
        // lanzar lingotes: el brazo derecho atrás y luego adelante
        if (throwT >= 0.0F) {
            float back = Anim.easeOut(Mth.clamp(throwT / 10.0F, 0.0F, 1.0F));
            float fwd = Anim.easeOut(Mth.clamp((throwT - 10.0F) / 4.0F, 0.0F, 1.0F));
            armR.xRot = -2.3F * back * (1.0F - fwd) + 0.5F * fwd;
            armR.zRot = 0.12F + 0.3F * back * (1.0F - fwd);
            foreR.xRot = -0.6F * back * (1.0F - fwd);
        }
        // los hombros siguen al cuerpo
        float lift = 1.5F * slam + Math.abs(Mth.cos(ph)) * 1.2F * walk;
        armR.y = -8F + lift;
        armL.y = -8F + lift;
        armR.z = armL.z = -body.xRot * 6.0F;

        // muerte: se viene abajo hacia delante
        float dth = Anim.death(e, pt);
        if (dth > 0.0F) {
            body.xRot += 0.6F * dth;
            head.xRot += 0.5F * dth;
            armR.xRot += 0.8F * dth;
            armL.xRot += 0.8F * dth;
        }
    }

    @Override
    public void renderToBuffer(PoseStack pose, VertexConsumer buffer, int light, int overlay, float r, float g, float b, float a) {
        this.root.render(pose, buffer, light, overlay, r, g, b, a);
    }
}
