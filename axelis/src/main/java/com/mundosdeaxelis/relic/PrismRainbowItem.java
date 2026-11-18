package com.mundosdeaxelis.relic;

import com.mundosdeaxelis.fx.Fx;
import com.mundosdeaxelis.network.ScreenFxPacket;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

/** Prisma del Arcoíris: mantén el clic y un rayo de todos los colores abrasa lo que apuntes. */
public class PrismRainbowItem extends RelicItem {
    private static final int COLOR = 0xFF9AF0;

    public PrismRainbowItem() {
        super("prism_rainbow", COLOR, 20 * 20, Rarity.EPIC);
    }

    @Override
    protected void passive(ServerPlayer sp, ServerLevel sl, long time) {
        if (sl.isDay() && sl.canSeeSky(sp.blockPosition())) {
            sp.addEffect(new MobEffectInstance(MobEffects.LUCK, 60, 0, true, false, true));
            if (time % 20 == 0) {
                sl.sendParticles(Fx.dust(Fx.rainbow(time * 0.01F), 0.6F), sp.getX(), sp.getY() + 1.8, sp.getZ(), 2, 0.4, 0.2, 0.4, 0.0);
            }
        }
    }

    @Override
    protected int channelTicks() {
        return 70;
    }

    @Override
    protected UseAnim anim() {
        return UseAnim.SPYGLASS;
    }

    @Override
    protected boolean channelStart(ServerPlayer sp, ServerLevel sl, ItemStack stack) {
        RelicSounds.play(sl, sp, relicId(), "cast", 0.8F, 1.0F);
        Fx.screen(sp, ScreenFxPacket.SPEED, 0, 16);
        return true;
    }

    @Override
    protected void channelTick(ServerPlayer sp, ServerLevel sl, ItemStack stack, int used) {
        Vec3 from = RelicFx.hand(sp);
        Vec3 look = sp.getLookAngle();
        Vec3 end = RelicFx.target(sp, 20);
        Vec3 d = end.subtract(from);
        double len = d.length();
        if (len < 0.5) {
            return;
        }
        Vec3 dir = d.scale(1.0 / len);
        Vec3 ref = Math.abs(dir.y) > 0.95 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
        Vec3 u = dir.cross(ref).normalize();
        Vec3 v = dir.cross(u).normalize();
        float width = 0.1F + Math.min(0.2F, used * 0.004F);
        int i = 0;
        for (double s = 0.4; s <= len; s += 0.4, i++) {
            Vec3 c = from.add(dir.scale(s));
            int col = Fx.rainbow((float) (s * 0.07 + used * 0.04));
            sl.sendParticles(Fx.dust(col, 0.75F), c.x, c.y, c.z, 1, 0.01, 0.01, 0.01, 0.0);
            double ph = s * 1.7 - used * 0.5;
            for (int k = 0; k < 2; k++) {
                double a = ph + k * Math.PI;
                Vec3 q = c.add(u.scale(Math.cos(a) * width)).add(v.scale(Math.sin(a) * width));
                sl.sendParticles(Fx.dust(Fx.rainbow((float) (s * 0.07 + used * 0.04 + 0.5 * k)), 0.5F), q.x, q.y, q.z, 1, 0, 0, 0, 0);
            }
            if (i % 5 == 0) {
                sl.sendParticles(Fx.spark(0xFFFFFF, 0.4F), c.x, c.y, c.z, 1, 0.03, 0.03, 0.03, 0.0);
            }
        }
        sl.sendParticles(Fx.dust(0xFFFFFF, 1.1F), from.x, from.y, from.z, 1, 0.02, 0.02, 0.02, 0.0);
        // Impacto
        sl.sendParticles(ParticleTypes.END_ROD, end.x, end.y, end.z, 3, 0.2, 0.2, 0.2, 0.1);
        RelicFx.burst(sl, Fx.spark(Fx.rainbow(used * 0.07F), 0.7F), end, 3, 0.2);
        if (used % 4 == 0) {
            RelicFx.ringFacing(sl, Fx.dust(Fx.rainbow(used * 0.05F), 0.8F), end, dir, 0.5, 12, used);
            RelicFx.ringFacing(sl, Fx.dust(0xFFFFFF, 0.5F), from.add(look.scale(0.2)), dir, 0.25, 8, used);
        }
        if (used % 20 == 10) {
            RelicSounds.play(sl, sp, relicId(), "hum", 0.7F, 1.0F);
        }
        if (used % 5 == 0) {
            float pitch = 0.7F + (used % 25) / 25.0F * 1.3F;
            Fx.sound(sl, sp.getX(), sp.getY(), sp.getZ(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.35F, pitch);
            for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class, new net.minecraft.world.phys.AABB(from, end).inflate(1.5), e -> RelicFx.isFoe(e, sp))) {
                Optional<Vec3> hit = e.getBoundingBox().inflate(0.35).clip(from, end);
                if (hit.isPresent()) {
                    RelicFx.hurt(sl, sp, e, 2.0F + used / 18.0F);
                    e.invulnerableTime = 0;
                    Vec3 c = e.getBoundingBox().getCenter();
                    RelicFx.burst(sl, Fx.spark(Fx.rainbow(used * 0.09F), 0.6F), c, 5, 0.2);
                }
            }
        }
        if (used == 35) {
            RelicFx.shake(sp, 0.5F, 40);
        }
    }

    @Override
    protected void channelEnd(ServerPlayer sp, ServerLevel sl, ItemStack stack, int used, boolean completed) {
        RelicSounds.stop(sp, relicId(), "cast");
        if (used < 10) {
            return;
        }
        Fx.sound(sl, sp.getX(), sp.getY(), sp.getZ(), SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.PLAYERS, 1.0F, 1.3F);
        Vec3 h = RelicFx.hand(sp);
        for (int i = 0; i < 12; i++) {
            sl.sendParticles(Fx.dust(Fx.rainbow(i / 12.0F), 0.9F), h.x, h.y, h.z, 0, Math.cos(i * 0.52) * 0.2, 0.05, Math.sin(i * 0.52) * 0.2, 1.0);
        }
        spend(sp, stack, cooldownTicks(), false);
    }
}
