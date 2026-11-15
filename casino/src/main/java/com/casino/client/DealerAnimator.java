package com.casino.client;

import com.casino.entity.DealerAnim;
import com.casino.entity.DealerEntity;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

/**
 * Mueve brazos y cabeza del crupier: una postura de descanso según su oficio (manos sobre la mesa, brazos cruzados el guardia,
 * secando la barra el bartender...), gestos al hablar y las animaciones que manda el servidor ({@link DealerAnim}). Cada animación
 * entra y sale suave para no dar saltos. Se llama después de la animación normal del modelo de jugador.
 */
public final class DealerAnimator {
    private static final float PI = (float) Math.PI;

    private DealerAnimator() {
    }

    /**
     * @param t       ticks desde que empezó la animación
     * @param age     reloj del crupier (ticks, con fracción)
     * @param talkAge ticks desde que empezó a hablar (o -1 si no habla)
     */
    public static void apply(int game, int anim, float t, float age, float talkAge, ModelPart head, ModelPart rArm, ModelPart lArm) {
        float[] r = {rArm.xRot, rArm.yRot, rArm.zRot};
        float[] l = {lArm.xRot, lArm.yRot, lArm.zRot};
        float[] h = {0, 0, 0};
        idle(game, age, r, l);
        if (talkAge >= 0) {
            float k = smooth(Math.min(1F, talkAge / 6F)) * smooth(Math.min(1F, (DealerEntity.TIP_TICKS - talkAge) / 8F));
            float[] tr = r.clone();
            float[] tl = l.clone();
            talk(game, age, tr, tl, h, k);
            lerp(r, tr, k);
            if (game != DealerEntity.GAME_GUARD) {
                lerp(l, tl, k * 0.5F);
            }
        }
        int dur = DealerAnim.duration(anim);
        if (anim != DealerAnim.NONE && t >= 0 && t <= dur) {
            float k = smooth(Math.min(1F, t / 4F)) * smooth(Math.min(1F, (dur - t) / 5F));
            float[] ar = r.clone();
            float[] al = l.clone();
            float[] ah = h.clone();
            pose(anim, t, dur, age, ar, al, ah);
            lerp(r, ar, k);
            lerp(l, al, k);
            lerp(h, ah, k);
        }
        rArm.xRot = r[0];
        rArm.yRot = r[1];
        rArm.zRot = r[2];
        lArm.xRot = l[0];
        lArm.yRot = l[1];
        lArm.zRot = l[2];
        head.xRot += h[0];
        head.yRot += h[1];
        head.zRot += h[2];
    }

    /** Postura de descanso (se suma al vaivén normal de los brazos). */
    private static void idle(int game, float age, float[] r, float[] l) {
        float breathe = Mth.sin(age * 0.07F) * 0.02F;
        switch (game) {
            case DealerEntity.GAME_BLACKJACK, DealerEntity.GAME_POKER -> {
                // manos apoyadas en la orilla de la mesa
                set(r, -0.62F + breathe, -0.18F, 0.05F);
                set(l, -0.62F + breathe, 0.18F, -0.05F);
            }
            case DealerEntity.GAME_ROULETTE -> {
                // manos juntas al frente, esperando
                set(r, -0.42F + breathe, -0.5F, 0.08F);
                set(l, -0.42F + breathe, 0.5F, -0.08F);
            }
            case DealerEntity.GAME_GUARD -> {
                // brazos cruzados
                set(r, -1.05F + breathe, -0.72F, 0.12F);
                set(l, -0.95F + breathe, 0.72F, -0.12F);
            }
            case DealerEntity.GAME_BARTENDER -> {
                // secando la barra en círculos con la derecha; la izquierda apoyada
                set(r, -0.85F + Mth.sin(age * 0.22F) * 0.12F, -0.25F + Mth.cos(age * 0.22F) * 0.22F, 0.0F);
                set(l, -0.55F + breathe, 0.15F, -0.05F);
            }
            case DealerEntity.GAME_WHEEL -> {
                // presentador: mano izquierda en la cintura, derecha suelta
                set(r, breathe * 2, 0.0F, 0.1F + Mth.sin(age * 0.05F) * 0.03F);
                set(l, -0.35F, 0.25F, -0.75F);
            }
            default -> {
                r[0] += breathe;
                l[0] += breathe;
            }
        }
    }

    /** Gestos mientras habla: la mano derecha acompaña lo que dice y la cabeza se mueve un poco. */
    private static void talk(int game, float age, float[] r, float[] l, float[] h, float k) {
        float g1 = Mth.sin(age * 0.33F);
        float g2 = Mth.sin(age * 0.19F + 1.3F);
        if (game == DealerEntity.GAME_GUARD) {
            // el guardia no descruza los brazos: solo inclina la cabeza
            h[2] += 0.06F * g2 * k;
            return;
        }
        set(r, -1.0F + 0.22F * g1, -0.25F + 0.2F * g2, 0.12F + 0.06F * g1);
        set(l, -0.6F + 0.1F * g2, 0.2F, -0.1F);
        h[0] += 0.04F * g1 * k;
        h[2] += 0.05F * g2 * k;
    }

    private static void pose(int anim, float t, float dur, float age, float[] r, float[] l, float[] h) {
        float u = t / dur;
        switch (anim) {
            case DealerAnim.DEAL -> {
                // de su lado hacia el jugador, con un golpe de muñeca
                set(r, -1.25F - 0.3F * Mth.sin(PI * u), 0.4F - 0.85F * smooth(u), 0.08F);
                l[0] = -0.75F;
                l[1] = 0.25F;
                h[0] = 0.18F;
            }
            case DealerAnim.SHUFFLE -> {
                float s = Mth.sin(t * 1.3F);
                set(r, -0.95F, -0.42F, -0.1F + 0.13F * s);
                set(l, -0.95F, 0.42F, 0.1F + 0.13F * s);
                h[0] = 0.3F;
            }
            case DealerAnim.FLIP -> {
                float s = Mth.sin(PI * u);
                set(r, -1.15F - 0.45F * s, -0.25F, 0.35F * s);
                h[0] = 0.15F;
            }
            case DealerAnim.CHIP -> {
                float s = Mth.sin(PI * u);
                set(l, -0.95F - 0.3F * s, 0.25F - 0.45F * u, -0.05F);
                h[0] = 0.15F;
            }
            case DealerAnim.SPIN -> {
                // lanza la bola: brazo en arco de un lado a otro
                float a = smooth(Mth.clamp((t - 4) / 12F, 0, 1));
                set(r, -1.45F + 0.2F * Mth.sin(PI * a), 0.7F - 1.5F * a, 0.1F);
                h[1] = -0.25F + 0.5F * a;
            }
            case DealerAnim.COLLECT -> {
                float a = smooth(u);
                set(r, -1.15F, 0.7F - 0.95F * a, 0.0F);
                set(l, -1.15F, -0.7F + 0.95F * a, 0.0F);
                h[0] = 0.2F;
            }
            case DealerAnim.PAY -> {
                float s = Mth.sin(PI * u);
                set(r, -1.2F - 0.25F * s, -0.3F, 0.05F);
                h[0] = 0.12F;
            }
            case DealerAnim.CLAP -> {
                float s = Math.max(0, Mth.sin(t * 1.15F));
                set(r, -1.2F, -0.25F - 0.35F * s, 0.0F);
                set(l, -1.2F, 0.25F + 0.35F * s, 0.0F);
                h[0] = -0.1F;
            }
            case DealerAnim.WAVE -> {
                set(r, -2.65F, -0.1F, 0.25F + 0.35F * Mth.sin(t * 0.75F));
                h[2] = 0.08F;
            }
            case DealerAnim.SHAKE -> h[1] = 0.45F * Mth.sin(t * 0.95F);
            case DealerAnim.NOD -> h[0] = 0.32F * Math.abs(Mth.sin(t * 0.42F));
            case DealerAnim.POUR -> {
                float tilt = smooth(Mth.clamp((t - 8) / 10F, 0, 1)) * (1 - smooth(Mth.clamp((t - 30) / 8F, 0, 1)));
                set(r, -1.75F, -0.25F, -0.15F - 0.55F * tilt);
                set(l, -1.05F, 0.35F, 0.0F);
                h[0] = 0.2F;
            }
            case DealerAnim.STOP -> {
                set(r, -1.6F, -0.1F, 0.15F);
                h[0] = -0.05F;
            }
            case DealerAnim.PULL -> {
                // brazo arriba... y lo baja de golpe hacia la rueda
                float up = smooth(Mth.clamp(t / 10F, 0, 1));
                float down = smooth(Mth.clamp((t - 12) / 4F, 0, 1));
                set(r, -2.9F * up + 2.4F * down, 0.35F, 0.2F - 0.1F * down);
                h[1] = 0.35F;
                h[0] = -0.15F * up + 0.25F * down;
            }
            case DealerAnim.PRESENT -> {
                float a = smooth(Mth.clamp(t / 8F, 0, 1));
                set(r, -0.55F * a, 0.0F, 1.0F * a);
                set(l, -0.55F * a, 0.0F, -1.0F * a);
                h[0] = -0.1F;
            }
            case DealerAnim.CHEER -> {
                float b = Mth.sin(t * 0.9F) * 0.15F;
                set(r, -2.85F + b, 0.0F, 0.3F);
                set(l, -2.85F - b, 0.0F, -0.3F);
                h[0] = -0.25F;
            }
            default -> {
            }
        }
    }

    private static void set(float[] a, float x, float y, float z) {
        a[0] = x;
        a[1] = y;
        a[2] = z;
    }

    private static void lerp(float[] a, float[] b, float k) {
        for (int i = 0; i < a.length; i++) {
            a[i] += (b[i] - a[i]) * k;
        }
    }

    private static float smooth(float x) {
        x = Mth.clamp(x, 0F, 1F);
        return x * x * (3 - 2 * x);
    }
}
