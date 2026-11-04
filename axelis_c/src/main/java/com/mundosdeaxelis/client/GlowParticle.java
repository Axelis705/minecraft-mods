package com.mundosdeaxelis.client;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mundosdeaxelis.particle.GlowOptions;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.util.Mth;

/** Partícula con textura propia. Cada "tipo" se mueve y se ve distinto. */
public class GlowParticle extends TextureSheetParticle {
    public enum Kind { GLOW, SPARKLE, CONFETTI, HEART, BUBBLE, EMBER, RUNE, BOLT, FEATHER, WISP, SHARD, NOTE, SLASH }

    private final SpriteSet sprites;
    private final Kind kind;
    private final float baseSize;
    private final float spin;
    private final float phase;

    public GlowParticle(ClientLevel level, double x, double y, double z, double vx, double vy, double vz,
                        GlowOptions options, SpriteSet sprites, Kind kind) {
        super(level, x, y, z, vx, vy, vz);
        this.sprites = sprites;
        this.kind = kind;
        this.xd = vx;
        this.yd = vy;
        this.zd = vz;
        this.hasPhysics = false;
        this.phase = this.random.nextFloat() * 6.2831F;
        this.spin = (this.random.nextFloat() - 0.5F) * 0.5F;

        int rgb = options.rgb;
        if (rgb == 0) {
            rgb = Mth.hsvToRgb(this.random.nextFloat(), 0.7F, 1.0F);
        }
        this.rCol = ((rgb >> 16) & 255) / 255.0F;
        this.gCol = ((rgb >> 8) & 255) / 255.0F;
        this.bCol = (rgb & 255) / 255.0F;
        float s = Math.max(0.2F, options.size);
        switch (kind) {
            case GLOW -> {
                this.baseSize = 0.16F * s;
                this.lifetime = 14 + this.random.nextInt(10);
                this.friction = 0.92F;
            }
            case SPARKLE -> {
                this.baseSize = 0.2F * s;
                this.lifetime = 16 + this.random.nextInt(12);
                this.friction = 0.9F;
            }
            case CONFETTI -> {
                this.baseSize = 0.07F * s;
                this.lifetime = 45 + this.random.nextInt(30);
                this.friction = 0.94F;
                this.roll = this.random.nextFloat() * 6.28F;
            }
            case HEART -> {
                this.baseSize = 0.13F * s;
                this.lifetime = 34 + this.random.nextInt(14);
                this.friction = 0.96F;
            }
            case EMBER -> {
                this.baseSize = 0.12F * s;
                this.lifetime = 24 + this.random.nextInt(18);
                this.friction = 0.95F;
            }
            case RUNE -> {
                this.baseSize = 0.28F * s;
                this.lifetime = 30 + this.random.nextInt(16);
                this.friction = 0.9F;
                this.roll = this.random.nextFloat() * 6.28F;
            }
            case BOLT -> {
                this.baseSize = 0.55F * s;
                this.lifetime = 5 + this.random.nextInt(3);
                this.friction = 1.0F;
                this.roll = this.random.nextFloat() * 6.28F;
            }
            case FEATHER -> {
                this.baseSize = 0.14F * s;
                this.lifetime = 40 + this.random.nextInt(24);
                this.friction = 0.95F;
                this.roll = this.random.nextFloat() * 6.28F;
            }
            case WISP -> {
                this.baseSize = 0.3F * s;
                this.lifetime = 26 + this.random.nextInt(16);
                this.friction = 0.94F;
                this.roll = this.random.nextFloat() * 6.28F;
            }
            case SHARD -> {
                this.baseSize = 0.12F * s;
                this.lifetime = 24 + this.random.nextInt(14);
                this.friction = 0.97F;
                this.roll = this.random.nextFloat() * 6.28F;
            }
            case NOTE -> {
                this.baseSize = 0.2F * s;
                this.lifetime = 32 + this.random.nextInt(12);
                this.friction = 0.96F;
            }
            case SLASH -> {
                this.baseSize = 0.9F * s;
                this.lifetime = 7;
                this.friction = 1.0F;
                this.roll = this.random.nextFloat() * 6.28F;
            }
            default -> {
                this.baseSize = 0.11F * s;
                this.lifetime = 40 + this.random.nextInt(20);
                this.friction = 0.97F;
            }
        }
        this.quadSize = this.baseSize;
        this.oRoll = this.roll;
        this.setSpriteFromAge(sprites);
    }

    /**
     * Las partículas muy cerca de la cámara se encogen y se desvanecen: corazones, tajos y runas a medio bloque de tu cara tapaban
     * media pantalla en las peleas. Los tajos y rayos (que son grandes a propósito) empiezan a atenuarse desde más lejos.
     */
    @Override
    public void render(VertexConsumer buffer, Camera camera, float partialTicks) {
        var cam = camera.getPosition();
        double dx = Mth.lerp(partialTicks, this.xo, this.x) - cam.x;
        double dy = Mth.lerp(partialTicks, this.yo, this.y) - cam.y;
        double dz = Mth.lerp(partialTicks, this.zo, this.z) - cam.z;
        double d = Math.sqrt(dx * dx + dy * dy + dz * dz);
        boolean big = kind == Kind.SLASH || kind == Kind.BOLT || kind == Kind.WISP;
        double far = big ? 3.2D : 2.2D;
        if (d >= far) {
            super.render(buffer, camera, partialTicks);
            return;
        }
        float k = (float) Mth.clamp((d - 0.6D) / (far - 0.6D), 0.0D, 1.0D);
        if (k <= 0.02F) {
            return;
        }
        float oldSize = this.quadSize;
        float oldAlpha = this.alpha;
        this.quadSize = oldSize * (0.3F + 0.7F * k);
        this.alpha = oldAlpha * k;
        super.render(buffer, camera, partialTicks);
        this.quadSize = oldSize;
        this.alpha = oldAlpha;
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        if (this.age++ >= this.lifetime) {
            this.remove();
            return;
        }
        float life = this.age / (float) this.lifetime;
        this.oRoll = this.roll;
        switch (kind) {
            case GLOW -> {
                this.yd += 0.002;
                this.alpha = Mth.clamp(1.0F - life * life, 0.0F, 1.0F);
                this.quadSize = this.baseSize * (1.0F - 0.65F * life);
            }
            case SPARKLE -> {
                this.alpha = Mth.clamp(Mth.sin(life * 3.1416F) * 1.4F, 0.0F, 1.0F);
                float pulse = 0.75F + 0.25F * Mth.sin(this.age * 0.9F + phase);
                this.quadSize = this.baseSize * pulse * (0.4F + 0.9F * Mth.sin(life * 3.1416F));
                this.roll += spin * 0.4F;
                this.setSpriteFromAge(sprites);
            }
            case CONFETTI -> {
                this.yd = Math.max(this.yd - 0.012, -0.07);
                this.xd += Mth.sin(this.age * 0.33F + phase) * 0.006;
                this.zd += Mth.cos(this.age * 0.29F + phase) * 0.006;
                this.roll += spin;
                this.alpha = life > 0.75F ? Mth.clamp((1.0F - life) * 4.0F, 0.0F, 1.0F) : 1.0F;
                int frames = 4;
                this.setSprite(sprites.get((this.age / 2 + (int) (phase * 3)) % frames, frames - 1));
            }
            case HEART -> {
                this.yd = Math.max(this.yd, 0.0) + 0.0015;
                this.xd += Mth.sin(this.age * 0.3F + phase) * 0.003;
                float pop = Mth.clamp(this.age / 6.0F, 0.0F, 1.0F);
                float pp = pop - 1.0F;
                float back = 1.0F + 2.70158F * pp * pp * pp + 1.70158F * pp * pp;
                this.quadSize = this.baseSize * Mth.clamp(back, 0.0F, 1.3F) * (1.0F + 0.08F * Mth.sin(this.age * 0.8F));
                this.alpha = life > 0.7F ? Mth.clamp((1.0F - life) / 0.3F, 0.0F, 1.0F) : 1.0F;
            }
            case EMBER -> {
                this.yd += 0.004;
                this.xd += Mth.sin(this.age * 0.5F + phase) * 0.004;
                this.zd += Mth.cos(this.age * 0.45F + phase) * 0.004;
                this.alpha = Mth.clamp((1.0F - life) * 1.6F, 0.0F, 1.0F);
                this.quadSize = this.baseSize * (1.0F - 0.7F * life) * (0.85F + 0.3F * Mth.sin(this.age * 1.3F + phase));
                this.setSprite(sprites.get((this.age / 2 + (int) (phase * 4)) % 4, 3));
            }
            case RUNE -> {
                this.yd += 0.0015;
                this.roll += spin * 0.25F;
                float fade = Mth.clamp(Mth.sin(life * 3.1416F) * 1.8F, 0.0F, 1.0F);
                this.alpha = fade;
                this.quadSize = this.baseSize * (0.7F + 0.3F * fade);
                this.setSprite(sprites.get((int) (phase * 1.3F) % 6, 5));
            }
            case BOLT -> {
                this.alpha = (this.age % 2 == 0) ? 1.0F : 0.55F;
                this.quadSize = this.baseSize * (1.0F - 0.25F * life);
                this.setSprite(sprites.get((this.age + (int) (phase * 2)) % 4, 3));
            }
            case FEATHER -> {
                this.yd = Math.max(this.yd - 0.006, -0.045);
                this.xd += Mth.sin(this.age * 0.28F + phase) * 0.006;
                this.zd += Mth.cos(this.age * 0.24F + phase) * 0.006;
                this.roll += Mth.sin(this.age * 0.2F + phase) * 0.05F;
                this.alpha = life > 0.7F ? Mth.clamp((1.0F - life) / 0.3F, 0.0F, 1.0F) : 1.0F;
                this.setSprite(sprites.get((int) (phase * 2) % 2, 1));
            }
            case WISP -> {
                this.yd += 0.003;
                this.xd += Mth.sin(this.age * 0.2F + phase) * 0.004;
                this.zd += Mth.cos(this.age * 0.18F + phase) * 0.004;
                this.roll += spin * 0.3F;
                this.alpha = Mth.clamp(Mth.sin(life * 3.1416F) * 1.3F, 0.0F, 0.85F);
                this.quadSize = this.baseSize * (0.5F + 1.2F * life);
                this.setSprite(sprites.get(Math.min(3, (int) (life * 4.0F)), 3));
            }
            case SHARD -> {
                this.yd -= 0.018;
                this.roll += spin * 1.6F;
                this.alpha = life > 0.7F ? Mth.clamp((1.0F - life) / 0.3F, 0.0F, 1.0F) : 1.0F;
                this.setSprite(sprites.get((int) (phase * 1.2F) % 3, 2));
            }
            case NOTE -> {
                this.yd = Math.max(this.yd, 0.0) + 0.002;
                this.xd += Mth.sin(this.age * 0.4F + phase) * 0.004;
                float pop = Mth.clamp(this.age / 5.0F, 0.0F, 1.0F);
                this.quadSize = this.baseSize * pop * (1.0F + 0.1F * Mth.sin(this.age * 0.7F));
                this.alpha = life > 0.7F ? Mth.clamp((1.0F - life) / 0.3F, 0.0F, 1.0F) : 1.0F;
                this.setSprite(sprites.get((int) (phase * 1.0F) % 2, 1));
            }
            case SLASH -> {
                float k = this.age / (float) this.lifetime;
                this.alpha = Mth.clamp(1.0F - k * k, 0.0F, 1.0F);
                this.quadSize = this.baseSize * (0.7F + 0.5F * k);
                this.roll += 0.12F;
            }
            default -> {
                this.yd = Math.max(this.yd, 0.0) + 0.0012;
                this.xd += Mth.sin(this.age * 0.35F + phase) * 0.004;
                this.zd += Mth.cos(this.age * 0.31F + phase) * 0.004;
                this.quadSize = this.baseSize * (1.0F + 0.25F * life);
                if (life > 0.88F) {
                    // Pop: se infla y desaparece.
                    this.quadSize *= 1.0F + (life - 0.88F) * 5.0F;
                    this.alpha = Mth.clamp((1.0F - life) / 0.12F, 0.0F, 1.0F);
                } else {
                    this.alpha = 0.9F;
                }
            }
        }
        this.move(this.xd, this.yd, this.zd);
        this.xd *= this.friction;
        this.yd *= this.friction;
        this.zd *= this.friction;
    }

    @Override
    protected int getLightColor(float partialTick) {
        return 15728880;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }
}
