package com.kensei.client;

import com.kensei.entity.RivalEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;

/** El cuerpo del rival (el mismo molde que el del jugador) con sus posturas: en guardia, tajo levantado y crítico levantado. */
public class RivalModel extends PlayerModel<RivalEntity> {
    public RivalModel(ModelPart root) {
        super(root, false);
    }

    @Override
    public void setupAnim(RivalEntity e, float limbSwing, float limbSwingAmount, float age, float yaw, float pitch) {
        super.setupAnim(e, limbSwing, limbSwingAmount, age, yaw, pitch);
        int vis = e.visState();
        // las mismas posturas que el jugador con katana (hoja y manos calculadas), para que se vea igual de bien
        double[] pose = KatanaStance.IDLE;
        float walkW = 0.22F;
        if (vis == RivalEntity.VIS_GUARD) {
            pose = KatanaStance.GUARD;
            walkW = 0.06F;
        } else if (vis == RivalEntity.VIS_HEAVY) {
            pose = KatanaStance.sample(KatanaStance.HEAVY, 0.5);
            walkW = 0.0F;
        } else if (this.attackTime > 0.0F) {
            double[][] keys = ((e.tickCount - e.swingTime) / 3 & 1) == 0 ? KatanaStance.SLASH1 : KatanaStance.SLASH2;
            float u = Math.min(1.0F, this.attackTime);
            float uu = 1.0F - (float) Math.pow(1.0F - u, 1.7F);
            float f = u / 0.16F;
            f = Math.min(1.0F, f * f * (3.0F - 2.0F * f));
            float o = Math.min(1.0F, Math.max(0.0F, (u - 0.8F) / 0.2F));
            o = 1.0F - o * o * (3.0F - 2.0F * o);
            pose = KatanaStance.lerp(KatanaStance.IDLE, KatanaStance.sample(keys, uu), f * o);
            walkW = 0.0F;
        } else if (vis == RivalEntity.VIS_LIGHT) {
            pose = KatanaStance.sample(KatanaStance.SLASH1, 0.0);
            walkW = 0.0F;
        }
        float walkMain = this.rightArm.xRot * walkW;
        float walkOff = this.leftArm.xRot * walkW;
        this.attackTime = 0.0F;
        KatanaPoses.put(this, this.rightArm, this.leftArm, 1.0F, pose, walkMain, walkOff);
        KatanaPoses.stance(this, e, 1.0F, 1.0F, Minecraft.getInstance().getFrameTime(), vis == RivalEntity.VIS_GUARD ? 1.45F : 1.0F);
        if (vis == RivalEntity.VIS_HEAVY) {
            this.body.xRot = -0.18F;
        }
        this.leftSleeve.copyFrom(this.leftArm);
        this.rightSleeve.copyFrom(this.rightArm);
        this.jacket.copyFrom(this.body);
        this.hat.copyFrom(this.head);
    }
}
