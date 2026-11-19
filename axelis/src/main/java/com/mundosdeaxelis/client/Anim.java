package com.mundosdeaxelis.client;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;

/** Curvas y ayudas de animación compartidas por todos los modelos (todo con tiempo parcial, para que no vaya a saltos). */
public final class Anim {
    public static final float PI = (float) Math.PI;
    public static final float DEG = PI / 180.0F;

    private Anim() {
    }

    /** El tiempo parcial a partir de la edad que recibe {@code setupAnim}. */
    public static float pt(LivingEntity e, float age) {
        return Mth.clamp(age - e.tickCount, 0.0F, 1.0F);
    }

    public static float smooth(float t) {
        t = Mth.clamp(t, 0.0F, 1.0F);
        return t * t * (3.0F - 2.0F * t);
    }

    public static float easeOut(float t) {
        t = Mth.clamp(t, 0.0F, 1.0F);
        return 1.0F - (1.0F - t) * (1.0F - t);
    }

    public static float easeIn(float t) {
        t = Mth.clamp(t, 0.0F, 1.0F);
        return t * t;
    }

    /** Sale con un pequeño rebote por encima de 1 y vuelve (para apariciones y golpes secos). */
    public static float backOut(float t) {
        t = Mth.clamp(t, 0.0F, 1.0F) - 1.0F;
        float c = 1.9F;
        return 1.0F + (c + 1.0F) * t * t * t + c * t * t;
    }

    /**
     * Golpe con peso: sale rápido (0..0.22), se pasa un poco y vuelve despacio. Recibe el {@code getAttackAnim} (0..1) y da
     * 0 → 1 (golpe) → 0. Úsese en vez de {@code sin(t·π)}, que es blando y simétrico.
     */
    public static float strike(float t) {
        if (t <= 0.0F || t >= 1.0F) {
            return 0.0F;
        }
        if (t < 0.22F) {
            float u = t / 0.22F;
            return 1.0F - (1.0F - u) * (1.0F - u) * (1.0F - u);
        }
        float u = (t - 0.22F) / 0.78F;
        return 1.0F - smooth(u);
    }

    /** El golpe de ataque de la criatura, ya con la curva {@link #strike}. */
    public static float attack(LivingEntity e, float pt) {
        return strike(e.getAttackAnim(pt));
    }

    /** Retroceso al recibir daño: sube enseguida y se va apagando (0..1). */
    public static float hurt(LivingEntity e, float pt) {
        if (e.hurtTime <= 0 || e.hurtDuration <= 0) {
            return 0.0F;
        }
        float h = Mth.clamp((e.hurtTime - pt) / e.hurtDuration, 0.0F, 1.0F);
        float rise = Math.min(1.0F, (1.0F - h) * 5.0F);
        return Mth.sin(rise * PI * 0.5F) * h;
    }

    /** 0 vivo; 0..1 durante los 20 ticks de la muerte. */
    public static float death(LivingEntity e, float pt) {
        return e.deathTime > 0 ? Mth.clamp((e.deathTime + pt) / 20.0F, 0.0F, 1.0F) : 0.0F;
    }

    /** Respiración: -1..1, lenta (rate ≈ 0.08–0.12). */
    public static float breath(float age, float rate) {
        return Mth.sin(age * rate);
    }

    /** Onda de paso para un miembro: {@code cos(fase·frecuencia + desfase)} por la amplitud de la marcha. */
    public static float gait(float limbSwing, float amount, float freq, float offset) {
        return Mth.cos(limbSwing * freq + offset) * amount;
    }

    /** Rebote vertical del cuerpo al caminar: dos golpecitos por ciclo (0..1). */
    public static float bob(float limbSwing, float amount, float freq) {
        return Math.abs(Mth.sin(limbSwing * freq)) * amount;
    }

    /** Hacia dónde está mirando de reojo: una vuelta lenta de cabeza "pensando", distinta para cada criatura. */
    public static float idleLook(LivingEntity e, float age) {
        float ph = (e.getId() % 97) * 0.37F;
        float t = (age + ph * 40.0F) % 220.0F;
        if (t > 60.0F) {
            return 0.0F;
        }
        return Mth.sin(t / 60.0F * PI) * Mth.sin(t / 60.0F * PI * 2.0F + ph) * 0.45F;
    }

    /** Parpadeo: 1 con los ojos cerrados (unos 2 ticks cada pocos segundos, distinto para cada criatura). */
    public static boolean blink(LivingEntity e, float age) {
        float t = (age + (e.getId() % 53) * 7.0F) % 70.0F;
        return t < 2.5F;
    }

    /**
     * Coloca una pieza "pegada" a otra que se inclina (p. ej. la cabeza sobre un torso que se dobla en la cadera): gira su
     * posición de reposo ({@code bx, by, bz}) alrededor del pivote ({@code py, pz}) lo mismo que {@code xRot}, con la
     * convención de las piezas del juego (xRot positivo = hacia delante).
     */
    public static void follow(ModelPart part, float py, float pz, float bx, float by, float bz, float xRot) {
        float dy = by - py, dz = bz - pz;
        float c = Mth.cos(xRot), s = Mth.sin(xRot);
        part.x = bx;
        part.y = py + dy * c - dz * s;
        part.z = pz + dy * s + dz * c;
    }

    /** Vuelve una pieza a su tamaño normal (las que se estiran o encogen). */
    public static void unscale(ModelPart p) {
        p.xScale = 1.0F;
        p.yScale = 1.0F;
        p.zScale = 1.0F;
    }

    /**
     * Progreso 0..1 de un ataque que dura {@code len} ticks desde que empezó el último golpe (el del juego dura solo 6 ticks:
     * así los golpes pesados tienen tiempo de subir, caer y recuperarse); -1 si no está atacando. Usa la casilla 9 de sucesos.
     */
    public static float swing(Motion m, LivingEntity e, float pt, float len) {
        float s = m.sinceLastStart(9, e.swinging, e, pt);
        return s < 0.0F || s > len ? -1.0F : s / len;
    }

    /** Mazazo por encima de la cabeza (para el xRot de los brazos): sube rápido, baja de golpe y se recupera despacio. */
    public static float smash(float u) {
        if (u < 0.0F || u >= 1.0F) {
            return 0.0F;
        }
        if (u < 0.14F) {
            return -2.7F * easeOut(u / 0.14F);
        }
        if (u < 0.26F) {
            return Mth.lerp(easeIn((u - 0.14F) / 0.12F), -2.7F, -0.35F);
        }
        float k = (u - 0.26F) / 0.74F;
        return -0.35F * (1.0F - smooth(k));
    }

    /** El instante del impacto de un {@link #smash} (pico corto 0..1): para sacudir el cuerpo, la cabeza y lo que cuelga. */
    public static float impact(float u) {
        if (u < 0.22F || u > 0.5F) {
            return 0.0F;
        }
        return u < 0.27F ? (u - 0.22F) / 0.05F : 1.0F - (u - 0.27F) / 0.23F;
    }

    /** Puñetazo o zarpazo: sale disparado (0 → 1) y vuelve despacio. */
    public static float jab(float u) {
        if (u < 0.0F || u >= 1.0F) {
            return 0.0F;
        }
        return u < 0.15F ? easeOut(u / 0.15F) : 1.0F - smooth((u - 0.15F) / 0.85F);
    }
}
