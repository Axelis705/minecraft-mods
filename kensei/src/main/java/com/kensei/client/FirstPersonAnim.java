package com.kensei.client;

import com.kensei.KenseiConfig;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;

/**
 * Los movimientos de la katana vistos en primera persona. Reemplaza el balanceo normal del juego mientras hay un tajo, estocada, crítico
 * o esquive en curso; con la guardia (clic derecho) y en reposo deja el comportamiento normal. Los ángulos están pensados con la hoja
 * apuntando hacia arriba en reposo: un giro positivo sobre X la inclina hacia atrás (hacia la cámara), uno negativo hacia delante.
 */
public final class FirstPersonAnim {
    private FirstPersonAnim() {
    }

    private static float ease(float u) {
        u = Mth.clamp(u, 0.0F, 1.0F);
        return u * u * (3.0F - 2.0F * u);
    }

    /** @return true si se dibujó el movimiento propio (el juego no debe aplicar el suyo). */
    public static boolean apply(PoseStack ps, LocalPlayer p, HumanoidArm arm, float partial, float equip, float swing) {
        if (p.isUsingItem()) {
            return false;
        }
        ClientAnims.Anim a = ClientAnims.get(p, 40);
        if (a == null) {
            return false;
        }
        float side = arm == HumanoidArm.RIGHT ? 1.0F : -1.0F;
        float t = ClientAnims.age(a, partial);

        float u = swing;
        boolean swingMove = a.move >= 1 && a.move <= 4 || a.move == ClientAnims.THRUST;
        if (swingMove && !(p.swinging && u > 0.0F)) {
            return false;
        }
        if (a.move == ClientAnims.DODGE && t >= 12.0F) {
            return false;
        }
        if (a.move == ClientAnims.HEAVY && t >= KenseiConfig.HEAVY_WINDUP.get() + 5.0F + 12.0F) {
            return false;
        }

        ps.translate(side * 0.56F, -0.52F + equip * -0.6F, -0.72F);

        float e = ease(u);
        float s = Mth.sin(u * (float) Math.PI);
        switch (a.move) {
            case 1: // tajo horizontal de derecha a izquierda
                ps.translate(side * (0.18F - 0.5F * e), 0.05F * s, -0.25F * s);
                ps.mulPose(Axis.YP.rotationDegrees(side * (-55.0F + 125.0F * e)));
                ps.mulPose(Axis.XP.rotationDegrees(-55.0F * s));
                ps.mulPose(Axis.ZP.rotationDegrees(side * (-15.0F + 30.0F * e)));
                break;
            case 2: // de vuelta, de izquierda a derecha
                ps.translate(side * (-0.32F + 0.55F * e), 0.05F * s, -0.25F * s);
                ps.mulPose(Axis.YP.rotationDegrees(side * (70.0F - 125.0F * e)));
                ps.mulPose(Axis.XP.rotationDegrees(-55.0F * s));
                ps.mulPose(Axis.ZP.rotationDegrees(side * (15.0F - 30.0F * e)));
                break;
            case 3: // tajo en diagonal de arriba hacia abajo
                ps.translate(side * (0.1F - 0.2F * e), 0.3F - 0.45F * e, -0.15F - 0.2F * s);
                ps.mulPose(Axis.XP.rotationDegrees(55.0F - 135.0F * e));
                ps.mulPose(Axis.YP.rotationDegrees(side * (-25.0F + 55.0F * e)));
                ps.mulPose(Axis.ZP.rotationDegrees(side * (30.0F - 60.0F * e)));
                break;
            case 4: // golpe final: sube de abajo hacia arriba y se extiende
                ps.translate(side * (0.05F - 0.1F * e), -0.25F + 0.5F * e, -0.3F * s);
                ps.mulPose(Axis.XP.rotationDegrees(-85.0F + 125.0F * e));
                ps.mulPose(Axis.YP.rotationDegrees(side * 25.0F * s));
                ps.mulPose(Axis.ZP.rotationDegrees(side * -20.0F * (1.0F - e)));
                break;
            case 7: // estocada: la hoja apunta recto hacia delante y se clava
                ps.translate(side * -0.1F * s, 0.1F * s, -0.15F - 0.65F * s);
                ps.mulPose(Axis.XP.rotationDegrees(-80.0F * Math.min(1.0F, s * 2.2F)));
                ps.mulPose(Axis.YP.rotationDegrees(side * 12.0F * s));
                break;
            case 5: { // crítico: sube con las dos manos, cae
                float w = KenseiConfig.HEAVY_WINDUP.get();
                float rot;
                float lift;
                float push;
                if (t < w) {
                    float k = ease(t / w);
                    rot = 70.0F * k;
                    lift = 0.35F * k;
                    push = 0.1F * k;
                } else if (t < w + 5.0F) {
                    float k = ((t - w) / 5.0F);
                    k = k * k;
                    rot = Mth.lerp(k, 70.0F, -80.0F);
                    lift = Mth.lerp(k, 0.35F, -0.1F);
                    push = Mth.lerp(k, 0.1F, -0.45F);
                } else {
                    float k = ease((t - w - 5.0F) / 12.0F);
                    rot = Mth.lerp(k, -80.0F, 0.0F);
                    lift = Mth.lerp(k, -0.1F, 0.0F);
                    push = Mth.lerp(k, -0.45F, 0.0F);
                }
                ps.translate(side * -0.12F, lift, push);
                ps.mulPose(Axis.XP.rotationDegrees(rot));
                break;
            }
            case 6: { // esquive: la katana se echa un poco atrás y abajo
                float k = Mth.sin(Mth.clamp(t / 12.0F, 0.0F, 1.0F) * (float) Math.PI);
                ps.translate(side * 0.05F * k, -0.12F * k, 0.12F * k);
                ps.mulPose(Axis.XP.rotationDegrees(25.0F * k));
                ps.mulPose(Axis.ZP.rotationDegrees(side * -12.0F * k));
                break;
            }
            default:
                break;
        }
        return true;
    }
}
