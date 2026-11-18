package com.mundosdeaxelis.relic;

import com.mundosdeaxelis.fx.Fx;
import com.mundosdeaxelis.fx.Vfx;
import com.mundosdeaxelis.network.ScreenFxPacket;
import com.mundosdeaxelis.vfx.VfxType;
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

/** Lanza Cometa: cargas y sales disparado en línea recta como un cometa, arrollando a quien se cruce. */
public class CometLanceItem extends RelicItem {
    private static final int COLOR = 0x66E4FF;

    public CometLanceItem() {
        super("comet_lance", COLOR, 20 * 16, Rarity.EPIC);
    }

    @Override
    protected int interval() {
        return 4;
    }

    @Override
    protected int channelTicks() {
        return 30;
    }

    @Override
    protected UseAnim anim() {
        return UseAnim.SPEAR;
    }

    @Override
    protected void passive(ServerPlayer sp, ServerLevel sl, long time) {
        if (sp.isSprinting()) {
            sp.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 20, 1, true, false, true));
            Vec3 back = sp.getLookAngle().scale(-0.5);
            sl.sendParticles(Fx.dust(COLOR, 0.8F), sp.getX() + back.x, sp.getY() + 0.9, sp.getZ() + back.z, 2, 0.15, 0.3, 0.15, 0.0);
            if (time % 8 == 0) {
                sl.sendParticles(Fx.spark(0xFFFFFF, 0.6F), sp.getX() + back.x, sp.getY() + 0.8, sp.getZ() + back.z, 1, 0.1, 0.2, 0.1, 0.0);
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
        Vec3 look = sp.getLookAngle();
        Vec3 h = RelicFx.hand(sp);
        for (int i = 0; i < 2 + used / 8; i++) {
            Vec3 from = h.add(look.scale(1.6 + sl.random.nextDouble() * 1.4)).add((sl.random.nextDouble() - 0.5) * 1.2,
                    (sl.random.nextDouble() - 0.5) * 1.0, (sl.random.nextDouble() - 0.5) * 1.2);
            Vec3 v = h.subtract(from).scale(0.16);
            sl.sendParticles(Fx.dust(COLOR, 0.7F + k * 0.5F), from.x, from.y, from.z, 0, v.x, v.y, v.z, 1.0);
        }
        if (used % 8 == 0) {
            Vfx.ringFacing(sl, h.add(look.scale(0.7)), look, COLOR, 0.5 + 0.7 * k, 9);
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
        Vec3 dir = new Vec3(look.x, Mth.clamp(look.y, -0.45, 0.6), look.z).normalize();
        double dist = 9.0 + charge * 9.0;
        int ticks = 9;
        double speed = dist / ticks;
        Vec3 start = sp.position().add(0, 1.0, 0);
        sp.getPersistentData().putLong("mdc_nofall", sl.getGameTime() + 100);
        RelicSounds.play(sl, sp, relicId(), "dash", 0.95F, 1.0F);
        Vfx.beam(sl, start, start.add(dir.scale(dist)), COLOR, 0.2, 16, VfxType.F_CAPS);
        Vfx.beam(sl, start, start.add(dir.scale(dist)), 0xFFFFFF, 0.07, 16, 0);
        Fx.screen(sp, ScreenFxPacket.SPEED, COLOR, ticks + 8);
        RelicFx.shake(sp, 0.8F, 10);
        Set<LivingEntity> hit = new HashSet<>();
        dash(sp, sl, dir, speed, 0, ticks, hit, (7.0F + 6.0F * charge) * power);
        spend(sp, stack, cooldownTicks(), true);
    }

    private void dash(ServerPlayer sp, ServerLevel sl, Vec3 dir, double speed, int t, int ticks, Set<LivingEntity> hit, float dmg) {
        if (!sp.isAlive() || sp.isRemoved()) {
            return;
        }
        if (t >= ticks) {
            sp.setDeltaMovement(dir.scale(0.3));
            sp.hurtMarked = true;
            Vec3 p = sp.position();
            Vfx.ring(sl, p.add(0, 0.1, 0), COLOR, 3.4, 16);
            Vfx.flash(sl, p.add(0, 1.0, 0), COLOR, 2.4, 10);
            RelicFx.burst(sl, Fx.spark(COLOR, 0.8F), p.add(0, 1, 0), 16, 0.4);
            return;
        }
        sp.setDeltaMovement(dir.scale(speed));
        sp.hurtMarked = true;
        sp.fallDistance = 0;
        Vec3 p = sp.position().add(0, 1.0, 0);
        sl.sendParticles(Fx.dust(COLOR, 1.1F), p.x, p.y, p.z, 3, 0.25, 0.3, 0.25, 0.02);
        sl.sendParticles(Fx.spark(0xFFFFFF, 0.7F), p.x, p.y, p.z, 1, 0.2, 0.2, 0.2, 0.0);
        Vfx.flash(sl, p, COLOR, 1.1, 5);
        for (LivingEntity e : RelicFx.foes(sl, p, 2.2, sp)) {
            if (hit.add(e)) {
                RelicFx.damage(sl, sp, e, dmg);
                RelicFx.knock(e, p.subtract(dir.scale(1.0)), 0.5, 0.45);
                e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 2));
                RelicSounds.play(sl, e, relicId(), "hit", 0.9F, 1.0F);
                Vfx.flash(sl, e.getBoundingBox().getCenter(), 0xFFFFFF, 2.0, 8);
                RelicFx.burst(sl, Fx.spark(COLOR, 0.8F), e.getBoundingBox().getCenter(), 12, 0.35);
            }
        }
        Scheduler.runLater(1, () -> dash(sp, sl, dir, speed, t + 1, ticks, hit, dmg));
    }
}
