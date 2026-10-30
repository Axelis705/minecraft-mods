package com.kensei.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;

/** La mira que flota sobre el enemigo fijado: se vuelve a crear cada tick mientras siga fijado, gira despacio y siempre se ve (máxima luz). */
public class LockParticle extends TextureSheetParticle {
    protected LockParticle(ClientLevel level, double x, double y, double z, SpriteSet sprites) {
        super(level, x, y, z);
        this.lifetime = 1;
        this.hasPhysics = false;
        this.gravity = 0.0F;
        this.xd = 0.0;
        this.yd = 0.0;
        this.zd = 0.0;
        this.quadSize = 0.3F;
        this.roll = (level.getGameTime() % 60) * 0.1F;
        this.oRoll = this.roll - 0.1F;
        this.pickSprite(sprites);
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
            return new LockParticle(level, x, y, z, sprites);
        }
    }
}
