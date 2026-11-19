package com.mundosdeaxelis.client;

import com.mundosdeaxelis.MundosDeAxelis;
import com.mundosdeaxelis.entity.Archimago;
import net.minecraft.client.model.HierarchicalModel;
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
 * Modelo del Archimago: túnica, sombrero puntiagudo, barba, bastón en la mano derecha, un libro que flota a su izquierda y tres
 * cristales que giran a su alrededor. Las cajas salen de gen/gen_archimago.py (modelo provisional hecho por código; si se rehace en
 * Blockbench hay que conservar los nombres de las partes). Trae una pose para cada estado del jefe.
 */
public class ArchimagoModel extends HierarchicalModel<Archimago> {
    public static final ModelLayerLocation LAYER =
            new ModelLayerLocation(new ResourceLocation(MundosDeAxelis.MODID, "archimago"), "main");

    private final ModelPart root;
    private final ModelPart body;
    private final ModelPart head;
    private final ModelPart hat;
    private final ModelPart hatTip;
    private final ModelPart rightArm;
    private final ModelPart leftArm;
    private final ModelPart staff;
    private final ModelPart skirt;
    private final ModelPart book;
    private final ModelPart[] crystals = new ModelPart[3];

    public ArchimagoModel(ModelPart root) {
        this.root = root;
        this.body = root.getChild("body");
        this.head = body.getChild("head");
        this.hat = head.getChild("hat");
        this.hatTip = hat.getChild("hat_tip");
        this.rightArm = body.getChild("right_arm");
        this.leftArm = body.getChild("left_arm");
        this.staff = rightArm.getChild("staff");
        this.skirt = body.getChild("skirt");
        this.book = root.getChild("book");
        this.crystals[0] = root.getChild("crystal1");
        this.crystals[1] = root.getChild("crystal2");
        this.crystals[2] = root.getChild("crystal3");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(4, 0).addBox(-5.0F, 0.0F, -3.0F, 10.0F, 12.0F, 6.0F)
                .texOffs(0, 50).addBox(-6.0F, -1.0F, -4.0F, 12.0F, 4.0F, 8.0F)
                .texOffs(16, 73).addBox(-5.0F, 10.0F, -3.5F, 10.0F, 2.0F, 7.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(36, 0).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F)
                .texOffs(50, 73).addBox(-3.0F, -2.0F, -5.0F, 6.0F, 7.0F, 2.0F)
                .texOffs(32, 83).addBox(-2.0F, 5.0F, -4.6F, 4.0F, 4.0F, 1.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition hat = head.addOrReplaceChild("hat", CubeListBuilder.create()
                .texOffs(68, 0).addBox(-7.0F, -1.0F, -7.0F, 14.0F, 1.0F, 14.0F)
                .texOffs(40, 50).addBox(-4.0F, -5.0F, -4.0F, 8.0F, 4.0F, 8.0F)
                .texOffs(24, 62).addBox(-3.0F, -9.0F, -3.0F, 6.0F, 4.0F, 6.0F)
                .texOffs(48, 62).addBox(-4.5F, -2.0F, -4.5F, 9.0F, 1.0F, 9.0F),
                PartPose.offset(0.0F, -8.0F, 0.0F));
        PartDefinition hat_tip = hat.addOrReplaceChild("hat_tip", CubeListBuilder.create()
                .texOffs(102, 73).addBox(-2.0F, -1.0F, -2.0F, 4.0F, 4.0F, 4.0F)
                .texOffs(42, 83).addBox(-1.0F, -4.0F, 0.0F, 2.0F, 3.0F, 2.0F)
                .texOffs(62, 83).addBox(-1.0F, -6.0F, 1.0F, 2.0F, 2.0F, 2.0F),
                PartPose.offset(0.0F, -12.0F, 0.0F));
        PartDefinition right_arm = body.addOrReplaceChild("right_arm", CubeListBuilder.create()
                .texOffs(0, 35).addBox(-3.0F, -2.0F, -2.5F, 4.0F, 10.0F, 5.0F)
                .texOffs(84, 62).addBox(-3.5F, 6.0F, -3.0F, 5.0F, 4.0F, 6.0F)
                .texOffs(70, 83).addBox(-2.0F, 10.0F, -1.0F, 2.0F, 2.0F, 2.0F),
                PartPose.offset(-7.0F, 2.0F, 0.0F));
        PartDefinition left_arm = body.addOrReplaceChild("left_arm", CubeListBuilder.create()
                .texOffs(18, 35).addBox(-1.0F, -2.0F, -2.5F, 4.0F, 10.0F, 5.0F)
                .texOffs(106, 62).addBox(-1.5F, 6.0F, -3.0F, 5.0F, 4.0F, 6.0F)
                .texOffs(78, 83).addBox(0.0F, 10.0F, -1.0F, 2.0F, 2.0F, 2.0F),
                PartPose.offset(7.0F, 2.0F, 0.0F));
        PartDefinition staff = right_arm.addOrReplaceChild("staff", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-0.5F, -22.0F, -0.5F, 1.0F, 34.0F, 1.0F)
                .texOffs(50, 83).addBox(-1.5F, -24.0F, -1.5F, 3.0F, 2.0F, 3.0F)
                .texOffs(24, 83).addBox(-2.5F, -28.0F, -0.5F, 1.0F, 5.0F, 1.0F)
                .texOffs(28, 83).addBox(1.5F, -28.0F, -0.5F, 1.0F, 5.0F, 1.0F)
                .texOffs(0, 83).addBox(-1.5F, -30.0F, -1.5F, 3.0F, 5.0F, 3.0F),
                PartPose.offset(-1.0F, 10.0F, -2.0F));
        PartDefinition skirt = body.addOrReplaceChild("skirt", CubeListBuilder.create()
                .texOffs(84, 35).addBox(-6.0F, 0.0F, -4.0F, 12.0F, 6.0F, 8.0F)
                .texOffs(36, 35).addBox(-7.0F, 6.0F, -5.0F, 14.0F, 5.0F, 10.0F)
                .texOffs(72, 50).addBox(-7.5F, 10.0F, -5.5F, 15.0F, 1.0F, 11.0F),
                PartPose.offset(0.0F, 12.0F, 0.0F));
        PartDefinition book = root.addOrReplaceChild("book", CubeListBuilder.create()
                .texOffs(0, 73).addBox(-3.0F, -4.0F, -1.0F, 6.0F, 8.0F, 2.0F)
                .texOffs(12, 83).addBox(-2.5F, -3.5F, -1.4F, 5.0F, 7.0F, 1.0F),
                PartPose.offset(11.0F, 6.0F, -5.0F));
        PartDefinition crystal1 = root.addOrReplaceChild("crystal1", CubeListBuilder.create()
                .texOffs(66, 73).addBox(-1.5F, -3.0F, -1.5F, 3.0F, 6.0F, 3.0F)
                .texOffs(0, 62).addBox(-1.0F, -4.5F, -1.0F, 2.0F, 9.0F, 2.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition crystal2 = root.addOrReplaceChild("crystal2", CubeListBuilder.create()
                .texOffs(78, 73).addBox(-1.5F, -3.0F, -1.5F, 3.0F, 6.0F, 3.0F)
                .texOffs(8, 62).addBox(-1.0F, -4.5F, -1.0F, 2.0F, 9.0F, 2.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition crystal3 = root.addOrReplaceChild("crystal3", CubeListBuilder.create()
                .texOffs(90, 73).addBox(-1.5F, -3.0F, -1.5F, 3.0F, 6.0F, 3.0F)
                .texOffs(16, 62).addBox(-1.0F, -4.5F, -1.0F, 2.0F, 9.0F, 2.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        return LayerDefinition.create(mesh, 128, 128);
    }

    @Override
    public ModelPart root() {
        return this.root;
    }

    private static float ease(float t) {
        t = Mth.clamp(t, 0.0F, 1.0F);
        return t * t * (3.0F - 2.0F * t);
    }

    @Override
    public void setupAnim(Archimago e, float limbSwing, float limbSwingAmount, float age, float headYaw, float headPitch) {
        root().getAllParts().forEach(ModelPart::resetPose);
        float pt = age - e.tickCount;
        float t = e.stateAge(pt);
        int st = e.state();
        float death = e.deathProgress(pt);

        // flota: sube y baja despacio
        float bob = Mth.sin(age * 0.07F) * 1.2F;
        body.y += bob - 1.0F;
        head.yRot = headYaw * Mth.DEG_TO_RAD * 0.7F;
        head.xRot = headPitch * Mth.DEG_TO_RAD * 0.6F;
        hatTip.xRot = -0.25F + Mth.sin(age * 0.09F) * 0.08F;
        skirt.xRot = Mth.clamp(limbSwingAmount, 0.0F, 1.0F) * 0.25F + Mth.sin(age * 0.06F) * 0.03F;

        // guardia: bastón algo adelantado, la otra mano abierta
        float rx = -0.35F + Mth.sin(age * 0.05F) * 0.05F;
        float rz = 0.08F;
        float lx = -0.15F + Mth.sin(age * 0.05F + 1.0F) * 0.05F;
        float lz = -0.12F;
        float bodyX = 0.0F;
        float bodyY = 0.0F;
        float lift = 0.0F;
        switch (st) {
            case Archimago.S_RAYS -> {
                // apunta con el bastón y lo sacude con cada salva
                float k = ease(t / 5.0F);
                float pulse = Mth.sin((t % 26.0F) / 26.0F * Mth.PI);
                rx = Mth.lerp(k, rx, -1.45F - 0.25F * pulse);
                lx = Mth.lerp(k, lx, -0.6F);
                bodyX = 0.08F * k;
            }
            case Archimago.S_RUNES -> {
                // los dos brazos arriba y los baja de golpe
                float up = ease(t / 8.0F);
                float down = ease((t - 30.0F) / 5.0F);
                float a = -2.7F * up * (1.0F - 0.7F * down);
                rx = Mth.lerp(up, rx, a);
                lx = Mth.lerp(up, lx, a);
                rz = 0.35F * up;
                lz = -0.35F * up;
                lift = 2.0F * up * (1.0F - down);
            }
            case Archimago.S_FLASKS -> {
                // lanza con la izquierda
                float c = (t % 10.0F) / 10.0F;
                lx = -2.4F + 2.0F * ease(c * 2.0F);
                lz = -0.3F;
            }
            case Archimago.S_STAFF -> {
                // barrido con el bastón
                float w = ease(t / 10.0F);
                float s = ease((t - 10.0F) / 5.0F);
                rx = -1.2F;
                rz = 0.5F;
                bodyY = -0.9F * w + 2.4F * s;
            }
            case Archimago.S_BLINK -> {
                float k = ease(t / 6.0F);
                rx = -2.2F * k;
                lx = -2.2F * k;
                bodyX = 0.2F * k;
            }
            case Archimago.S_SEALS -> {
                // bastón en alto, canalizando
                float k = ease(t / 12.0F);
                rx = Mth.lerp(k, rx, -2.9F);
                lx = Mth.lerp(k, lx, -1.2F + Mth.sin(age * 0.2F) * 0.1F);
                lz = -0.5F * k;
                lift = 3.0F * k;
            }
            case Archimago.S_STUN -> {
                float k = ease(t / 8.0F);
                bodyX = 0.55F * k;
                head.xRot = 0.5F * k;
                rx = 0.3F * k;
                lx = 0.3F * k;
                lift = -3.0F * k;
            }
            case Archimago.S_METEORS -> {
                float k = ease(t / 10.0F);
                rx = Mth.lerp(k, rx, -3.0F + Mth.sin(age * 0.5F) * 0.12F);
                lx = Mth.lerp(k, lx, -2.6F + Mth.sin(age * 0.5F + 1.5F) * 0.12F);
                rz = 0.3F * k;
                lz = -0.3F * k;
                head.xRot = -0.5F * k;
                lift = 2.5F * k;
            }
            case Archimago.S_BEAMS -> {
                // brazos en cruz
                float k = ease(t / 14.0F);
                rx = Mth.lerp(k, rx, 0.0F);
                lx = Mth.lerp(k, lx, 0.0F);
                rz = 1.45F * k;
                lz = -1.45F * k;
                lift = 4.0F * k;
            }
            default -> {
            }
        }
        if (death > 0.0F) {
            float k = ease(death * 1.6F);
            bodyX = -0.5F * k;
            head.xRot = -0.6F * k;
            rx = Mth.lerp(k, rx, -0.2F);
            rz = 1.0F * k;
            lz = -1.0F * k;
            lift = -4.0F * ease((death - 0.5F) * 2.0F);
        }
        // al cambiar de hechizo, la pose anterior se funde con la nueva (nada salta de golpe)
        float[] pv = {rx, rz, lx, lz, bodyX, bodyY, lift, head.xRot};
        Inertia.blend(e, death > 0.0F ? 99 : st, pv, 8.0F, pt);
        rx = pv[0];
        rz = pv[1];
        lx = pv[2];
        lz = pv[3];
        bodyX = pv[4];
        bodyY = pv[5];
        lift = pv[6];
        head.xRot = pv[7];
        // inercia del vuelo y temblor al recibir golpes
        Motion mo = Motion.of(e);
        float jig = mo.jiggle(pt), hurt = Anim.hurt(e, pt);
        rightArm.xRot = rx;
        rightArm.zRot = rz + hurt * 0.25F;
        leftArm.xRot = lx;
        leftArm.zRot = lz - hurt * 0.25F;
        body.xRot = bodyX - hurt * 0.15F;
        body.yRot = bodyY;
        body.zRot = jig * 0.05F;
        body.y -= lift;
        skirt.xRot += mo.drag(pt) * 0.35F;
        skirt.zRot = mo.sway(pt) * 0.25F;
        // el bastón queda más o menos vertical aunque el brazo suba
        staff.xRot = st == Archimago.S_RAYS || st == Archimago.S_STAFF ? 1.35F : Mth.clamp(-rx * 0.75F, -0.4F, 1.6F);

        // el libro flota a su izquierda
        book.y += Mth.sin(age * 0.09F + 2.0F) * 1.5F + bob - lift;
        book.yRot = -0.5F + Mth.sin(age * 0.05F) * 0.2F;
        book.xRot = -0.2F;

        // los cristales giran alrededor; con el escudo puesto giran más cerca y rápido, y solo se ven los sellos que quedan
        int shield = e.shield();
        boolean fast = st == Archimago.S_SEALS || st == Archimago.S_BEAMS || st == Archimago.S_METEORS;
        float speed = fast ? 0.16F : 0.045F;
        float radius = st == Archimago.S_SEALS ? 10.0F : 14.0F;
        for (int i = 0; i < 3; i++) {
            ModelPart c = crystals[i];
            float a = age * speed + i * (Mth.TWO_PI / 3.0F);
            c.x = Mth.cos(a) * radius;
            c.z = Mth.sin(a) * radius;
            c.y = 2.0F + Mth.sin(age * 0.1F + i * 2.1F) * 3.0F + bob - lift + 30.0F * ease((death - 0.3F) * 2.0F);
            c.yRot = age * 0.1F + i;
            c.zRot = 0.25F * Mth.sin(age * 0.07F + i);
            c.visible = st != Archimago.S_SEALS || i < shield;
            if (st == Archimago.S_STUN) {
                c.y = 20.0F;
                c.zRot = 1.3F;
            }
        }
    }
}
