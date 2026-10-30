package com.kensei.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;

/** Destello de la estela de la katana: nace quieto, brilla (siempre a máxima luz), se encoge y se desvanece en unos ticks. */
public class SlashParticle extends TextureSheetParticle {
    private final float startSize;

    protected SlashParticle(ClientLevel level, double x, double y, double z, SpriteSet sprites) {
        super(level, x, y, z);
        this.lifetime = 6;
        this.hasPhysics = false;
        this.gravity = 0.0F;
        this.xd = 0.0;
        this.yd = 0.0;
        this.zd = 0.0;
        this.startSize = 0.11F + this.random.nextFloat() * 0.07F;
        this.quadSize = startSize;
        float mix = this.random.nextFloat() * 0.35F;
        this.setColor(0.55F + mix, 0.88F + mix * 0.3F, 1.0F);
        this.pickSprite(sprites);
    }

    @Override
    public void tick() {
        super.tick();
        float life = this.age / (float) this.lifetime;
        this.quadSize = startSize * (1.0F - life * 0.7F);
        this.setAlpha(1.0F - life * life);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Override
    protected int getLightColor(float partial) {
        return 0xF000F0;
    }

    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double xs, double ys, double zs) {
            return new SlashParticle(level, x, y, z, sprites);
        }
    }
}
