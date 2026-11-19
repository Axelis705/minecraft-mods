package com.mundosdeaxelis.client;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;

/**
 * El modelo humano del juego con más vida: mira de reojo, respira, se encorva para acechar, da tajos largos con el arma
 * (sube, corta y se recupera), retrocede al recibir golpes y, si es una armadura dormida, se queda rígida y "despierta" con
 * una sacudida.
 */
public class LivelyHumanoidModel<T extends Mob> extends HumanoidModel<T> {
    /** Se agacha para acechar cuando persigue a alguien. */
    private final boolean stalker;
    /** Se mueve como una armadura: pasos más rígidos y metálicos. */
    private final boolean stiff;
    private java.util.function.Predicate<T> asleep = e -> false;

    public LivelyHumanoidModel(ModelPart root, boolean stalker, boolean stiff) {
        super(root);
        this.stalker = stalker;
        this.stiff = stiff;
    }

    /** Mientras se cumpla, se queda quieta como una estatua. */
    public LivelyHumanoidModel<T> asleepWhen(java.util.function.Predicate<T> p) {
        this.asleep = p;
        return this;
    }

    @Override
    public void setupAnim(T e, float limbSwing, float limbSwingAmount, float age, float netHeadYaw, float headPitch) {
        float pt = Anim.pt(e, age);
        Motion m = Motion.of(e);
        boolean sleeping = this.asleep.test(e);
        float wake = Mth.clamp(m.sinceEnd(5, sleeping, e, pt) / 14.0F, 0.0F, 1.0F);
        float agg = m.smooth(0, e.isAggressive() && !sleeping ? 1.0F : 0.0F, 0.12F, pt);
        float u = Anim.swing(m, e, pt, 12.0F);
        this.crouching = this.stalker && agg > 0.5F && u < 0.0F && limbSwingAmount < 0.9F;
        super.setupAnim(e, limbSwing, limbSwingAmount, age, netHeadYaw, headPitch);
        if (sleeping) {
            // estatua: todo quieto
            this.head.xRot = 0.0F;
            this.head.yRot = 0.0F;
            this.rightArm.xRot = 0.0F;
            this.leftArm.xRot = 0.0F;
            this.rightArm.zRot = 0.0F;
            this.leftArm.zRot = 0.0F;
            this.rightLeg.xRot = 0.0F;
            this.leftLeg.xRot = 0.0F;
            this.hat.copyFrom(this.head);
            return;
        }
        float walk = Math.min(1.0F, limbSwingAmount);
        float hurt = Anim.hurt(e, pt);
        float jig = m.jiggle(pt);
        float breath = Anim.breath(age, 0.1F);
        // despertar: una sacudida que se va calmando y la cabeza que se levanta de golpe
        float shake = wake < 1.0F ? Mth.sin(age * 2.6F) * 0.12F * (1.0F - wake) : 0.0F;
        // cabeza: mira de reojo al estar quieta, retrocede con los golpes
        this.head.yRot += Anim.idleLook(e, age) * (1.0F - walk) * (1.0F - agg) + shake;
        this.head.xRot += -hurt * 0.4F + (wake < 1.0F ? (1.0F - wake) * 0.5F : 0.0F);
        this.head.zRot = jig * 0.1F + shake * 0.5F;
        // brazos: respiran; con el arma, un tajo de arriba abajo cruzando el cuerpo
        float sl = Anim.smash(u);
        if (u >= 0.0F) {
            this.rightArm.xRot = sl * 0.9F - 0.2F;
            this.rightArm.yRot = -0.3F * (-sl / 2.7F) + 0.4F * Anim.impact(u);
            this.rightArm.zRot = 0.1F;
            this.body.yRot = 0.25F * (-sl / 2.7F) - 0.35F * Anim.impact(u);
            this.leftArm.xRot = -0.4F * (-sl / 2.7F);
        } else {
            this.body.yRot = 0.0F;
        }
        this.rightArm.zRot += breath * 0.03F + hurt * 0.3F + shake;
        this.leftArm.zRot += -breath * 0.03F - hurt * 0.3F - shake;
        this.body.zRot = jig * 0.05F;
        if (this.stiff) {
            // armadura: pasos cortos y secos, los brazos casi no se balancean
            this.rightArm.xRot *= 0.75F;
            this.leftArm.xRot *= 0.75F;
            float clank = Mth.abs(Mth.sin(limbSwing * 0.6662F)) * walk;
            this.head.y = -clank * 0.6F;
            this.hat.y = this.head.y;
        }
        this.hat.copyFrom(this.head);
    }
}
