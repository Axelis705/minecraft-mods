package com.mundosdeaxelis.relic;

import com.mundosdeaxelis.fx.Fx;
import com.mundosdeaxelis.fx.Vfx;
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

import net.minecraft.world.entity.projectile.AbstractArrow;

/** Báculo del Torbellino: desvía proyectiles y manda un torbellino que levanta a los enemigos y los lanza por los aires. */
public class WhirlwindStaffItem extends RelicItem {
    private static final int COLOR = 0xC8F4FF;

    public WhirlwindStaffItem() {
        super("whirlwind_staff", COLOR, 20 * 28, Rarity.RARE);
    }

    @Override
    protected int interval() {
        return 1;
    }

    @Override
    protected void passive(ServerPlayer sp, ServerLevel sl, long time) {
        AABB box = sp.getBoundingBox().inflate(5.5);
        for (Projectile pr : sl.getEntitiesOfClass(Projectile.class, box)) {
            if (pr.getPersistentData().getBoolean("mdc_wind") || pr.getOwner() == sp) {
                continue;
            }
            if (pr.getOwner() instanceof net.minecraft.world.entity.player.Player) {
                continue;
            }
            Vec3 vel = pr.getDeltaMovement();
            if (vel.lengthSqr() < 0.04) {
                continue;
            }
            Vec3 to = sp.getBoundingBox().getCenter().subtract(pr.position());
            if (to.dot(vel) <= 0) {
                continue;
            }
            // lo desvía hacia un lado y hacia arriba, con un remolino
            Vec3 side = vel.cross(new Vec3(0, 1, 0));
            side = side.lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : side.normalize();
            double sgn = sl.random.nextBoolean() ? 1.0 : -1.0;
            pr.setDeltaMovement(vel.scale(0.35).add(side.scale(sgn * 0.5)).add(0, 0.35, 0));
            pr.hurtMarked = true;
            pr.getPersistentData().putBoolean("mdc_wind", true);
            if (pr instanceof AbstractArrow ar) {
                ar.setCritArrow(false);
            }
            Vec3 p = pr.position();
            RelicSounds.playVaried(sl, p, relicId(), "fling", 0.45F);
            Vfx.ring(sl, p, COLOR, 1.6, 10);
            RelicFx.burst(sl, ParticleTypes.CLOUD, p, 8, 0.12);
            RelicFx.burst(sl, Fx.spark(COLOR, 0.5F), p, 6, 0.15);
        }
    }

    @Override
    protected boolean active(ServerPlayer sp, ServerLevel sl, ItemStack stack) {
        float power = power(stack);
        Vec3 look = RelicFx.look(sp);
        Vec3 dir = new Vec3(look.x, 0, look.z);
        dir = dir.lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : dir.normalize();
        Vec3 start0 = sp.position().add(dir.scale(2.8));
        Vec3 start = new Vec3(start0.x, RelicFx.groundY(sl, start0.x, sp.getY() + 1, start0.z), start0.z);
        int dur = (int) (20 * 5 * power);
        int id = Vfx.make(sl, VfxType.FUNNEL).at(start).rgb(COLOR).size(2.6).a(7.5).life(dur + 24).send();
        Vfx.ring(sl, start.add(0, 0.1, 0), 0xFFFFFF, 5.5, 14);
        RelicFx.burst(sl, ParticleTypes.CLOUD, start.add(0, 0.5, 0), 20, 0.3);
        spin(sl, sp, start, dir, dur, 0, id, power, new java.util.HashMap<>());
        return true;
    }

    private void spin(ServerLevel sl, ServerPlayer sp, Vec3 pos, Vec3 dir, int dur, int age, int id, float power,
                      java.util.Map<java.util.UUID, Integer> hurtAt) {
        if (age >= dur || !sp.isAlive()) {
            finish(sl, sp, pos, id, power);
            return;
        }
        // avanza por el suelo, se detiene ante un muro
        Vec3 next = pos.add(dir.scale(0.3));
        double g = RelicFx.groundY(sl, next.x, pos.y + 1, next.z);
        Vec3 np = pos;
        if (g - pos.y < 1.3) {
            np = new Vec3(next.x, g, next.z);
        }
        final Vec3 p = np;
        Vfx.make(sl, VfxType.FUNNEL).at(p).rgb(COLOR).size(2.6).a(7.5).life(0).update(id);
        if (age % 32 == 0) {
            RelicSounds.play(sl, p.add(0, 1.5, 0), relicId(), "loop", 0.85F, 1.0F);
        }
        if (age % 9 == 0) {
            Vfx.ring(sl, p.add(0, 0.1, 0), COLOR, 3.6, 10);
        }
        // partículas en espiral
        for (int h = 0; h < 7; h++) {
            double y = h * 1.05 + 0.2;
            double r = 0.7 + y * 0.28;
            double a = age * 0.55 + h * 1.1;
            for (int k = 0; k < 2; k++) {
                double aa = a + k * Math.PI;
                sl.sendParticles(h % 2 == 0 ? ParticleTypes.CLOUD : Fx.dust(COLOR, 0.9F), p.x + Math.cos(aa) * r, p.y + y, p.z + Math.sin(aa) * r, 0,
                        -Math.sin(aa) * 0.15, 0.04, Math.cos(aa) * 0.15, 1.0);
            }
        }
        if (age % 2 == 0) {
            sl.sendParticles(RelicFx.blockDust(RelicFx.groundState(sl, p.x, p.y - 0.5, p.z)), p.x, p.y + 0.15, p.z, 6, 1.0, 0.1, 1.0, 0.1);
        }
        // levanta a los enemigos
        for (LivingEntity e : RelicFx.foes(sl, p.add(0, 3, 0), 5.5, sp)) {
            Vec3 rel = e.position().subtract(p);
            double h = Math.sqrt(rel.x * rel.x + rel.z * rel.z);
            if (h > 3.4 || e.getY() < p.y - 1.0 || e.getY() > p.y + 8.0) {
                continue;
            }
            Vec3 tang = h < 0.05 ? new Vec3(1, 0, 0) : new Vec3(-rel.z, 0, rel.x).normalize();
            Vec3 in = h < 0.05 ? Vec3.ZERO : new Vec3(-rel.x, 0, -rel.z).normalize();
            double lift = e.getY() < p.y + 5.5 ? 0.2 : 0.02;
            e.setDeltaMovement(tang.scale(0.38).add(in.scale(0.1)).add(0, lift, 0));
            e.fallDistance = 0;
            e.hurtMarked = true;
            Integer last = hurtAt.get(e.getUUID());
            if (last == null || age - last >= 20) {
                hurtAt.put(e.getUUID(), age);
                RelicFx.damage(sl, sp, e, 1.5F * power);
                sl.sendParticles(Fx.slash(COLOR, 0.5F), e.getX(), e.getY() + e.getBbHeight() * 0.5, e.getZ(), 2, 0.2, 0.2, 0.2, 0.0);
            }
        }
        final Vec3 pp = p;
        Scheduler.runLater(1, () -> spin(sl, sp, pp, dir, dur, age + 1, id, power, hurtAt));
    }

    private void finish(ServerLevel sl, ServerPlayer sp, Vec3 p, int id, float power) {
        Vfx.remove(sl, id);
        RelicSounds.play(sl, p.add(0, 2, 0), relicId(), "fling", 1.0F, 1.0F);
        Vfx.flash(sl, p.add(0, 2, 0), COLOR, 4.0, 12);
        Vfx.ring(sl, p.add(0, 0.2, 0), COLOR, 7.0, 16);
        Vfx.ring(sl, p.add(0, 3.0, 0), 0xFFFFFF, 6.0, 14);
        RelicFx.burst(sl, ParticleTypes.CLOUD, p.add(0, 2, 0), 40, 0.7);
        RelicFx.burst(sl, Fx.spark(COLOR, 0.7F), p.add(0, 2, 0), 30, 0.6);
        RelicFx.shakeNear(sl, p, 14, 0.8F, 8);
        for (LivingEntity e : RelicFx.foes(sl, p.add(0, 3, 0), 6.0, sp)) {
            Vec3 d = e.position().subtract(p);
            Vec3 h = new Vec3(d.x, 0, d.z);
            h = h.lengthSqr() < 0.01 ? new Vec3(sl.random.nextDouble() - 0.5, 0, sl.random.nextDouble() - 0.5).normalize() : h.normalize();
            e.setDeltaMovement(h.x * 1.2, 1.05, h.z * 1.2);
            e.hurtMarked = true;
            RelicFx.damage(sl, sp, e, 5.0F * power);
            Vec3 c = e.getBoundingBox().getCenter();
            Vfx.flash(sl, c, COLOR, 1.6, 8);
        }
    }
}
