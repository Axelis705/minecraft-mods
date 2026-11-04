package com.mundosdeaxelis.particle;

import com.mojang.serialization.Codec;
import com.mundosdeaxelis.MundosDeAxelis;
import net.minecraft.core.particles.ParticleType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** Partículas propias del mod (cada una con sus texturas en textures/particle). */
public final class ModParticles {
    public static final DeferredRegister<ParticleType<?>> PARTICLES =
            DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, MundosDeAxelis.MODID);

    /** Orbe de luz suave. */
    public static final RegistryObject<ParticleType<GlowOptions>> GLOW = reg("glow");
    /** Estrella de cuatro puntas que titila. */
    public static final RegistryObject<ParticleType<GlowOptions>> SPARKLE = reg("sparkle");
    /** Papelito de confeti que cae dando vueltas. */
    public static final RegistryObject<ParticleType<GlowOptions>> CONFETTI = reg("confetti");
    /** Pétalo de flor que cae. */
    public static final RegistryObject<ParticleType<GlowOptions>> PETAL = reg("petal");
    /** Corazón flotante. */
    public static final RegistryObject<ParticleType<GlowOptions>> HEART = reg("heart");
    /** Burbuja brillante. */
    public static final RegistryObject<ParticleType<GlowOptions>> BUBBLE = reg("bubble");

    /** Ascua que sube y titila. */
    public static final RegistryObject<ParticleType<GlowOptions>> EMBER = reg("ember");
    /** Runa que flota y gira. */
    public static final RegistryObject<ParticleType<GlowOptions>> RUNE = reg("rune");
    /** Relámpago corto. */
    public static final RegistryObject<ParticleType<GlowOptions>> BOLT = reg("bolt");
    /** Pluma que cae meciéndose. */
    public static final RegistryObject<ParticleType<GlowOptions>> FEATHER = reg("feather");
    /** Voluta de humo mágico. */
    public static final RegistryObject<ParticleType<GlowOptions>> WISP = reg("wisp");
    /** Esquirla de cristal. */
    public static final RegistryObject<ParticleType<GlowOptions>> SHARD = reg("shard");
    /** Nota musical. */
    public static final RegistryObject<ParticleType<GlowOptions>> NOTE = reg("note");
    /** Corte en arco. */
    public static final RegistryObject<ParticleType<GlowOptions>> SLASH = reg("slash");

    private ModParticles() {
    }

    private static RegistryObject<ParticleType<GlowOptions>> reg(String name) {
        return PARTICLES.register(name, () -> new ParticleType<GlowOptions>(false, GlowOptions.DESERIALIZER) {
            @Override
            public Codec<GlowOptions> codec() {
                return GlowOptions.codec(this);
            }
        });
    }
}
