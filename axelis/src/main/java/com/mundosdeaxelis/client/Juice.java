package com.mundosdeaxelis.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;

/**
 * El "movimiento con vida" que se aplica a todo el cuerpo al dibujar: inclinarse al arrancar y frenar, ladearse en las curvas,
 * rebotar al andar, retroceder al recibir un golpe, estirarse y aplastarse, aparecer con un saltito y morir con estilo.
 * <p>{@link #body} va en {@code setupRotations} (después del giro de la criatura, con el pivote en los pies) y {@link #scale}
 * en {@code scale} (el modelo ya está volteado: escalar ahí aplasta hacia los pies).
 * <p>Marco local en {@code setupRotations}: delante = -Z, arriba = +Y, su derecha = +X.
 */
public final class Juice {
    private Juice() {
    }

    public static void body(PoseStack pose, LivingEntity e, CreatureStyle st, float pt) {
        Motion m = Motion.of(e);
        float dth = Anim.death(e, pt);
        float alive = 1.0F - Math.min(1.0F, dth * 4.0F);
        // rebote al andar: arriba a mitad de zancada, abajo al apoyar el pie
        if (st.bob > 0.0F) {
            float amt = e.walkAnimation.speed(pt);
            pose.translate(0.0F, Anim.bob(e.walkAnimation.position(pt), Math.min(1.0F, amt), st.stepFreq) * st.bob * alive, 0.0F);
        }
        float lean = m.lean(pt) * st.lean * alive;
        float bank = m.bank(pt) * st.bank * alive;
        float hurt = Anim.hurt(e, pt);
        float jig = m.jiggle(pt);
        // retroceso: se echa hacia atrás y tiembla de lado
        float back = hurt * st.recoil * Anim.DEG;
        pose.mulPose(Axis.XP.rotation(-lean + back));
        pose.mulPose(Axis.ZP.rotation(-bank + jig * 0.05F));
        death(pose, e, st, dth);
    }

    private static void death(PoseStack pose, LivingEntity e, CreatureStyle st, float p) {
        if (p <= 0.0F) {
            return;
        }
        switch (st.death) {
            case TOPPLE -> {
                // cae de bruces cada vez más rápido y rebota dos veces contra el suelo
                float a;
                if (p < 0.62F) {
                    a = Anim.easeIn(p / 0.62F) * 90.0F;
                } else {
                    float u = (p - 0.62F) / 0.38F;
                    a = 90.0F - Math.abs(Mth.sin(u * Anim.PI * 2.0F)) * 9.0F * (1.0F - u);
                }
                pose.mulPose(Axis.XP.rotationDegrees(-a));
                pose.mulPose(Axis.ZP.rotationDegrees(Mth.sin(p * 7.0F) * 6.0F * (1.0F - p)));
            }
            case FLOP -> {
                float a;
                if (p < 0.55F) {
                    a = Anim.easeIn(p / 0.55F) * 90.0F;
                } else {
                    float u = (p - 0.55F) / 0.45F;
                    a = 90.0F - Math.abs(Mth.sin(u * Anim.PI * 2.0F)) * 10.0F * (1.0F - u);
                }
                pose.mulPose(Axis.ZP.rotationDegrees(a));
            }
            case POOF -> pose.mulPose(Axis.YP.rotationDegrees(Anim.easeIn(p) * 540.0F));
            case CRUMBLE -> {
                float j = (1.0F - p) * 5.0F;
                pose.mulPose(Axis.XP.rotationDegrees(Mth.sin(e.tickCount * 2.7F) * j));
                pose.mulPose(Axis.ZP.rotationDegrees(Mth.cos(e.tickCount * 3.1F) * j));
            }
            default -> {
            }
        }
    }

    public static void scale(PoseStack pose, LivingEntity e, CreatureStyle st, float pt) {
        Motion m = Motion.of(e);
        float sq = m.squash(pt) * st.squash;
        float jig = m.jiggle(pt) * st.squash;
        float hurt = Anim.hurt(e, pt) * st.squash;
        float sy = 1.0F + sq * 0.22F - hurt * 0.07F + jig * 0.045F;
        float sxz = 1.0F - sq * 0.11F + hurt * 0.05F - jig * 0.03F;
        float pop = m.pop(e, pt);
        float p = Anim.death(e, pt);
        if (p > 0.0F) {
            switch (st.death) {
                case MELT -> {
                    float k = Anim.smooth(p);
                    sy *= 1.0F - 0.9F * k;
                    sxz *= 1.0F + 0.5F * k;
                }
                case POOF -> {
                    float s = p < 0.25F ? 1.0F + 0.3F * Anim.easeOut(p / 0.25F) : 1.3F * (1.0F - Anim.easeIn((p - 0.25F) / 0.75F));
                    sy *= s;
                    sxz *= s;
                }
                case CRUMBLE -> {
                    float k = Anim.easeIn(p);
                    sy *= 1.0F - 0.85F * k;
                    sxz *= 1.0F + 0.3F * k;
                }
                default -> {
                }
            }
        }
        pose.scale(Math.max(0.01F, sxz * pop), Math.max(0.01F, sy * pop), Math.max(0.01F, sxz * pop));
    }
}
