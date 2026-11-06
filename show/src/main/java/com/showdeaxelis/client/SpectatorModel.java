package com.showdeaxelis.client;

import com.showdeaxelis.entity.SpectatorEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;

/** El cuerpo del publico: un humanoide normal que mueve brazos y cabeza segun su humor. */
public class SpectatorModel extends HumanoidModel<SpectatorEntity> {
    public SpectatorModel(ModelPart root) {
        super(root);
    }

    @Override
    public void setupAnim(SpectatorEntity e, float limbSwing, float limbSwingAmount, float age, float headYaw, float headPitch) {
        super.setupAnim(e, 0f, 0f, age, headYaw, headPitch);
        float s = (float) Math.sin(age * 1.3f);
        float slow = (float) Math.sin(age * 0.15f + e.getId());
        switch (e.mood()) {
            case SpectatorEntity.CLAP -> {
                float k = s * 0.5f + 0.5f;
                rightArm.xRot = -1.25f;
                leftArm.xRot = -1.25f;
                rightArm.yRot = -0.55f + k * 0.45f;
                leftArm.yRot = 0.55f - k * 0.45f;
            }
            case SpectatorEntity.CHEER -> {
                rightArm.xRot = -3.0f + s * 0.35f;
                leftArm.xRot = -3.0f - s * 0.35f;
                rightArm.zRot = 0.35f;
                leftArm.zRot = -0.35f;
                head.xRot = -0.25f;
            }
            case SpectatorEntity.BOO -> {
                rightArm.xRot = -1.3f;
                leftArm.xRot = -1.3f;
                rightArm.zRot = 0.4f;
                leftArm.zRot = -0.4f;
                head.yRot = (float) Math.sin(age * 0.5f) * 0.45f;
                head.xRot = 0.25f;
            }
            case SpectatorEntity.LAUGH -> {
                head.xRot = -0.35f + s * 0.15f;
                rightArm.xRot = -0.7f;
                leftArm.xRot = -0.7f;
                rightArm.yRot = -0.5f;
                leftArm.yRot = 0.5f;
                body.xRot = s * 0.12f;
            }
            default -> {
                rightArm.xRot = slow * 0.05f;
                leftArm.xRot = -slow * 0.05f;
                rightArm.zRot = 0.06f;
                leftArm.zRot = -0.06f;
            }
        }
        hat.copyFrom(head);
    }
}
