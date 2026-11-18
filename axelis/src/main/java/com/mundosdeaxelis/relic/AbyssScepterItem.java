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

/** Cetro del Abismo: tentáculos del abismo atrapan a los enemigos, los elevan y los aplastan contra el suelo. */
public class AbyssScepterItem extends RelicItem {
    private static final int COLOR = 0x7A5CFF;
    private static final int DARK = 0x3B1E8F;

    public AbyssScepterItem() {
        super("abyss_scepter", COLOR, 20 * 30, Rarity.EPIC);
    }

    @Override
    protected int interval() {
        return 10;
    }

    @Override
    protected void passive(ServerPlayer sp, ServerLevel sl, long time) {
        Vec3 look = sp.getLookAngle();
        Vec3 flat = new Vec3(look.x, 0, look.z);
        if (flat.lengthSqr() < 1.0E-4) {
            return;
        }
        flat = flat.normalize();
        for (LivingEntity e : RelicFx.foes(sl, sp.position(), 11, sp)) {
            Vec3 to = e.position().subtract(sp.position());
            Vec3 tf = new Vec3(to.x, 0, to.z);
            if (tf.lengthSqr() < 0.01) {
                continue;
            }
            if (tf.normalize().dot(flat) < -0.25) {
                e.addEffect(new MobEffectInstance(MobEffects.GLOWING, 25, 0, false, false));
                if (time % 20 == 0) {
                    sl.sendParticles(Fx.wisp(COLOR, 0.6F), e.getX(), e.getY() + e.getBbHeight() + 0.3, e.getZ(), 2, 0.2, 0.1, 0.2, 0.0);
                }
            }
        }
    }

    @Override
    protected boolean active(ServerPlayer sp, ServerLevel sl, ItemStack stack) {
        float power = power(stack);
        Vec3 look = sp.getLookAngle();
        LivingEntity aimed = RelicFx.aimedFoe(sl, sp, 16, 0.9);
        Vec3 center = aimed != null ? aimed.position() : new Vec3(sp.getX() + look.x * 7.0, sp.getY(), sp.getZ() + look.z * 7.0);
        center = new Vec3(center.x, RelicFx.groundY(sl, center.x, center.y + 1, center.z), center.z);
        final Vec3 c = center;
        double radius = 7.5;
        List<LivingEntity> foes = RelicFx.foes(sl, c, radius, sp);
        Vfx.rune(sl, c.add(0, 0.05, 0), COLOR, radius, 46);
        Vfx.ring(sl, c.add(0, 0.1, 0), DARK, radius, 24);
        Vfx.column(sl, c, DARK, 1.2, 4.5, 26);
        RelicFx.shakeNear(sl, c, 18, 0.8F, 12);
        RelicFx.burst(sl, ParticleTypes.SMOKE, c.add(0, 0.2, 0), 28, 0.25);
        // algunos tentáculos decorativos aunque no haya enemigos
        for (int i = 0; i < 6; i++) {
            double a = i * Math.PI / 3.0 + 0.3;
            Vec3 root = c.add(Math.cos(a) * 3.8, 0, Math.sin(a) * 3.8);
            Vec3 top = c.add(Math.cos(a + 0.5) * 2.4, 3.0 + sl.random.nextDouble() * 1.4, Math.sin(a + 0.5) * 2.4);
            Scheduler.runLater(i * 2, () -> Vfx.whip(sl, root, top, COLOR, 0.34, 34, true, 0.8, 2.0));
        }
        RelicSounds.play(sl, c, relicId(), "grab", 1.0F, 0.8F);
        for (int i = 0; i < foes.size() && i < 8; i++) {
            final LivingEntity e = foes.get(i);
            Scheduler.runLater(3 + i * 3, () -> grab(sl, sp, e, power));
        }
        return true;
    }

    private void grab(ServerLevel sl, ServerPlayer sp, LivingEntity e, float power) {
        if (!e.isAlive()) {
            return;
        }
        Vec3 base = new Vec3(e.getX(), e.getY(), e.getZ());
        RelicSounds.playVaried(sl, base, relicId(), "grab", 0.7F);
        RelicFx.damage(sl, sp, e, 3.0F * power);
        e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 6, false, false, false));
        e.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 50, 0, false, false, false));
        int n = 3;
        for (int k = 0; k < n; k++) {
            double a = k * Math.PI * 2.0 / n + sl.random.nextDouble();
            Vec3 root = base.add(Math.cos(a) * 1.9, 0, Math.sin(a) * 1.9);
            Vec3 tip = base.add(Math.cos(a + 0.7) * 0.5, e.getBbHeight() * 0.75, Math.sin(a + 0.7) * 0.5);
            Vfx.whip(sl, root, tip, COLOR, 0.3, 26, true, 0.7, 2.0);
        }
        Vfx.ring(sl, base.add(0, 0.1, 0), COLOR, 2.4, 12);
        holdLoop(sl, sp, e, base, power, 0);
    }

    private void holdLoop(ServerLevel sl, ServerPlayer sp, LivingEntity e, Vec3 base, float power, int t) {
        if (!e.isAlive()) {
            return;
        }
        if (t < 16) {
            // lo elevan y lo sujetan
            double lift = Math.min(2.6, t * 0.18);
            e.setDeltaMovement(0, t < 12 ? 0.11 : 0.0, 0);
            e.hurtMarked = true;
            e.fallDistance = 0;
            if (e instanceof Mob m) {
                m.setTarget(null);
            }
            if (t % 3 == 0) {
                sl.sendParticles(Fx.wisp(COLOR, 0.5F), e.getX(), e.getY() + 0.5, e.getZ(), 2, 0.3, 0.4, 0.3, 0.0);
            }
            Scheduler.runLater(1, () -> holdLoop(sl, sp, e, base, power, t + 1));
            return;
        }
        // aplastar
        e.setDeltaMovement(0, -2.4, 0);
        e.hurtMarked = true;
        Scheduler.runLater(3, () -> {
            if (!e.isAlive()) {
                return;
            }
            Vec3 g = new Vec3(e.getX(), RelicFx.groundY(sl, e.getX(), e.getY() + 1, e.getZ()), e.getZ());
            RelicSounds.playVaried(sl, g, relicId(), "slam", 0.9F);
            RelicFx.damage(sl, sp, e, 9.0F * power);
            Vfx.flash(sl, g.add(0, 0.3, 0), COLOR, 3.0, 10);
            Vfx.ring(sl, g.add(0, 0.1, 0), COLOR, 4.0, 14);
            Vfx.rune(sl, g.add(0, 0.05, 0), DARK, 1.6, 16);
            RelicFx.burst(sl, RelicFx.blockDust(RelicFx.groundState(sl, g.x, g.y - 0.5, g.z)), g.add(0, 0.2, 0), 26, 0.3);
            RelicFx.burst(sl, ParticleTypes.SMOKE, g.add(0, 0.3, 0), 10, 0.15);
            RelicFx.shakeNear(sl, g, 10, 0.6F, 8);
            for (LivingEntity o : RelicFx.foes(sl, g, 2.6, sp)) {
                if (o != e) {
                    RelicFx.damage(sl, sp, o, 4.0F * power);
                    RelicFx.knock(o, g, 0.5, 0.25);
                }
            }
        });
    }
}
