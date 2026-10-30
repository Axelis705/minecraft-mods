package com.kensei.registry;

import com.kensei.Kensei;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** Partículas propias. La imagen de cada una está en assets/kensei/particles y textures/particle. */
public final class ModParticles {
    public static final DeferredRegister<ParticleType<?>> PARTICLES = DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, Kensei.MODID);

    /** Un destello brillante y corto que se deja a lo largo del filo al tajear (la estela). */
    public static final RegistryObject<SimpleParticleType> SLASH = PARTICLES.register("slash_trail", () -> new SimpleParticleType(false));

    /** La marca que flota sobre el enemigo fijado. */
    public static final RegistryObject<SimpleParticleType> LOCK = PARTICLES.register("lock_on", () -> new SimpleParticleType(false));

    private ModParticles() {
    }
}
