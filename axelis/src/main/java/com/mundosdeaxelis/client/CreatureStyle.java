package com.mundosdeaxelis.client;

import net.minecraft.core.particles.ParticleOptions;

import java.util.function.Supplier;

/**
 * Cómo se mueve y qué efectos suelta cada criatura: cuánto se inclina y se ladea, cuánto rebota al andar y se aplasta al caer,
 * cómo muere y qué partículas deja al pisar, al volar, al recibir golpes y al morir. Lo usan {@link Juice} (dibujo) y
 * {@link MotionFx} (partículas).
 */
public final class CreatureStyle {
    /** Cómo muere. */
    public enum Death {
        /** Cae de lado como las criaturas del juego (con rebote). */
        FLOP,
        /** Se va de bruces hacia delante y rebota contra el suelo. */
        TOPPLE,
        /** Se derrite: cada vez más ancho y bajo hasta quedar en un charco. */
        MELT,
        /** Se infla un poco y desaparece girando (pequeños y voladores). */
        POOF,
        /** Tiembla y se desmorona hacia abajo (piedra, metal). */
        CRUMBLE,
        /** Lo hace el modelo o el dibujo propio. */
        NONE
    }

    float lean = 1.0F;
    float bank = 1.0F;
    float bob = 0.03F;
    float squash = 1.0F;
    float recoil = 10.0F;
    float stepFreq = 0.6662F;
    float footSpread = 0.3F;
    boolean dust = true;
    boolean heavy;
    boolean flier;
    Death death = Death.FLOP;
    Supplier<ParticleOptions> hurtFx;
    Supplier<ParticleOptions> trailFx;
    int trailEvery = 3;
    boolean trailWhenIdle;
    Supplier<ParticleOptions> deathFx;
    int deathCount = 18;
    Supplier<ParticleOptions> ambientFx;
    int ambientEvery = 12;

    public static CreatureStyle of() {
        return new CreatureStyle();
    }

    public CreatureStyle lean(float v) {
        lean = v;
        return this;
    }

    public CreatureStyle bank(float v) {
        bank = v;
        return this;
    }

    public CreatureStyle bob(float v) {
        bob = v;
        return this;
    }

    public CreatureStyle squash(float v) {
        squash = v;
        return this;
    }

    public CreatureStyle recoil(float deg) {
        recoil = deg;
        return this;
    }

    /** La frecuencia de paso del modelo (la que multiplica a limbSwing en sus cosenos). */
    public CreatureStyle steps(float freq, float spread) {
        stepFreq = freq;
        footSpread = spread;
        return this;
    }

    public CreatureStyle noDust() {
        dust = false;
        return this;
    }

    public CreatureStyle heavy() {
        heavy = true;
        return this;
    }

    public CreatureStyle flier() {
        flier = true;
        dust = false;
        bob = 0.0F;
        return this;
    }

    public CreatureStyle death(Death d) {
        death = d;
        return this;
    }

    public CreatureStyle hurtFx(Supplier<ParticleOptions> p) {
        hurtFx = p;
        return this;
    }

    public CreatureStyle trail(Supplier<ParticleOptions> p, int every, boolean idleToo) {
        trailFx = p;
        trailEvery = every;
        trailWhenIdle = idleToo;
        return this;
    }

    public CreatureStyle deathFx(Supplier<ParticleOptions> p, int count) {
        deathFx = p;
        deathCount = count;
        return this;
    }

    public CreatureStyle ambient(Supplier<ParticleOptions> p, int every) {
        ambientFx = p;
        ambientEvery = every;
        return this;
    }
}
