package com.mundosdeaxelis.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;

/** Ayudas para las animaciones de muerte de los seres de la Forja (se desploman en vez de caer de lado). */
final class ForgeDeath {
    private ForgeDeath() {
    }

    /** 0 mientras vive; sube de 0 a 1 durante los 20 ticks de la muerte. */
    static float progress(LivingEntity e, float partialTick) {
        return e.deathTime > 0 ? Mth.clamp((e.deathTime + partialTick) / 20.0F, 0.0F, 1.0F) : 0.0F;
    }

    /** Se va de bruces hacia delante: despacio al principio y con el golpe final contra el suelo. */
    static void topple(PoseStack pose, LivingEntity e, float partialTick) {
        float p = progress(e, partialTick);
        if (p <= 0.0F) {
            return;
        }
        float f = p * p * (3.0F - 2.0F * p);
        pose.mulPose(Axis.XP.rotationDegrees(f * 86.0F));
    }
}
