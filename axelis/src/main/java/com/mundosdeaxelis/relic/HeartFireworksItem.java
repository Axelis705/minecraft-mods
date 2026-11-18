package com.mundosdeaxelis.relic;

import com.mundosdeaxelis.fx.Fx;
import com.mundosdeaxelis.fx.Vfx;
import com.mundosdeaxelis.world.Scheduler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Fuegos del Corazón: tres cohetes que estallan en corazones y estrellas, hieren a los enemigos y cuidan a los aliados. */
public class HeartFireworksItem extends RelicItem {
    private static final int COLOR = 0xFF5C8A;
    private static final int[] COLORS = {0xFF5C8A, 0xFFD34D, 0xE8A6FF};

    public HeartFireworksItem() {
        super("heart_fireworks", COLOR, 20 * 30, Rarity.EPIC);
    }

    @Override
    protected int interval() {
        return 20;
    }

    @Override
    protected int channelTicks() {
        return 30;
    }

    @Override
    protected UseAnim anim() {
        return UseAnim.BOW;
    }

    @Override
    protected void passive(ServerPlayer sp, ServerLevel sl, long time) {
        for (LivingEntity a : RelicFx.allies(sl, sp.position(), 12, sp, false)) {
            if (a instanceof net.minecraft.world.entity.player.Player) {
                sp.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 100, 0, true, false, true));
                a.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 100, 0, true, false, true));
                if (time % 80 == 0) {
                    sl.sendParticles(Fx.heart(COLOR, 0.6F), sp.getX(), sp.getY() + 2.2, sp.getZ(), 1, 0.3, 0.1, 0.3, 0.0);
                    sl.sendParticles(Fx.heart(COLOR, 0.6F), a.getX(), a.getY() + 2.2, a.getZ(), 1, 0.3, 0.1, 0.3, 0.0);
                }
                break;
            }
        }
    }

    @Override
    protected boolean channelStart(ServerPlayer sp, ServerLevel sl, ItemStack stack) {
        RelicSounds.play(sl, sp, relicId(), "charge", 0.7F, 1.0F);
        return true;
    }

    @Override
    protected void channelTick(ServerPlayer sp, ServerLevel sl, ItemStack stack, int used) {
        float k = used / 30.0F;
        Vec3 h = RelicFx.hand(sp);
        for (int i = 0; i < 2 + used / 8; i++) {
            double a = sl.random.nextDouble() * Math.PI * 2.0;
            double r = 1.4 * (1.0 - k) + 0.3;
            Vec3 from = h.add(Math.cos(a) * r, (sl.random.nextDouble() - 0.5) * r, Math.sin(a) * r);
            Vec3 v = h.subtract(from).scale(0.15);
            sl.sendParticles(i % 2 == 0 ? Fx.heart(COLOR, 0.55F) : Fx.spark(0xFFD34D, 0.5F), from.x, from.y, from.z, 0, v.x, v.y, v.z, 1.0);
        }
        if (used % 7 == 0) {
            Vec3 look = sp.getLookAngle();
            Vfx.ringFacing(sl, h.add(look.scale(0.5)), look, COLOR, 0.35 + 0.5 * k, 9);
        }
    }

    @Override
    protected void channelEnd(ServerPlayer sp, ServerLevel sl, ItemStack stack, int used, boolean completed) {
        if (used < 8) {
            return;
        }
        float charge = Math.min(1.0F, used / 30.0F);
        float power = power(stack);
        Vec3 look = sp.getLookAngle();
        Vec3 flat = new Vec3(look.x, 0, look.z);
        flat = flat.lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : flat.normalize();
        Vec3 side = flat.cross(new Vec3(0, 1, 0)).normalize();
        final Vec3 flatDir = flat;
        Vec3 h = RelicFx.hand(sp);
        RelicSounds.play(sl, h, relicId(), "launch", 0.95F, 1.0F);
        sl.playSound(null, h.x, h.y, h.z, SoundEvents.FIREWORK_ROCKET_LAUNCH, SoundSource.PLAYERS, 0.7F, 1.1F);
        RelicFx.shake(sp, 0.6F, 8);
        for (int i = 0; i < 3; i++) {
            final int idx = i;
            final int color = COLORS[i];
            double off = i - 1;
            Vec3 dir = new Vec3(side.x * off * 0.28 + flat.x * 0.12, 1.0, side.z * off * 0.28 + flat.z * 0.12).normalize();
            Scheduler.runLater(1 + i * 4, () -> {
                Missiles.Spec m = new Missiles.Spec(sp);
                m.pos = h;
                m.dir = dir;
                m.speed = 0.95;
                m.life = 14 + idx * 2;
                m.rgb = color;
                m.size = 0.24;
                m.particles = ParticleTypes.FIREWORK;
                final int[] tt = {0};
                m.onTick = spec -> {
                    tt[0]++;
                    spec.dir = spec.dir.add(Math.sin(tt[0] * 0.7 + idx) * 0.05, 0, Math.cos(tt[0] * 0.7 + idx) * 0.05).normalize();
                };
                m.hitRadius = 0.0;
                m.onHit = (pos, target) -> burst(sl, sp, pos, color, flatDir, side, charge, power);
                Missiles.launch(m);
            });
        }
        spend(sp, stack, cooldownTicks(), true);
    }

    private static void burst(ServerLevel sl, ServerPlayer sp, Vec3 c, int color, Vec3 flat, Vec3 side, float charge, float power) {
        RelicSounds.play(sl, c, "heart_fireworks", "burst", 1.1F, 0.95F + sl.random.nextFloat() * 0.1F);
        Scheduler.runLater(4, () -> RelicSounds.play(sl, c, "heart_fireworks", "crackle", 0.7F, 1.0F));
        Vfx.flash(sl, c, color, 3.6, 16);
        Vfx.ringFacing(sl, c, flat, color, 4.5, 20);
        Vfx.ringFacing(sl, c, new Vec3(0, 1, 0), 0xFFFFFF, 3.5, 16);
        // corazón grande que se abre
        for (int i = 0; i < 46; i++) {
            double t = Math.PI * 2.0 * i / 46;
            double hx = 16 * Math.pow(Math.sin(t), 3) / 16.0;
            double hy = (13 * Math.cos(t) - 5 * Math.cos(2 * t) - 2 * Math.cos(3 * t) - Math.cos(4 * t)) / 16.0;
            double sp2 = 0.11;
            sl.sendParticles(i % 3 == 0 ? Fx.heart(color, 1.0F) : Fx.spark(color, 0.9F), c.x, c.y, c.z, 0,
                    side.x * hx * sp2, hy * sp2 + 0.01, side.z * hx * sp2, 1.0);
        }
        RelicFx.burst(sl, Fx.spark(0xFFFFFF, 0.8F), c, 26, 0.45);
        RelicFx.burst(sl, ParticleTypes.FIREWORK, c, 28, 0.3);
        RelicFx.burst(sl, Fx.paper(0, 0.8F), c, 18, 0.25);
        for (LivingEntity e : RelicFx.foes(sl, c, 5.5, sp)) {
            RelicFx.damage(sl, sp, e, 6.0F * power * (0.7F + 0.3F * charge));
            RelicFx.knock(e, c, 0.5, 0.3);
        }
        for (LivingEntity a : RelicFx.allies(sl, c, 9, sp, true)) {
            a.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 80, 1, false, true, true));
            sl.sendParticles(Fx.heart(color, 0.6F), a.getX(), a.getY() + 1.4, a.getZ(), 3, 0.3, 0.3, 0.3, 0.02);
        }
        RelicFx.shakeNear(sl, c, 14, 0.6F, 8);
    }
}
