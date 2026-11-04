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

/** Báculo de Colmillos: mandíbulas espectrales salen del suelo y muerden; agachado, forman un círculo. */
public class FangStaffItem extends RelicItem {
    private static final int COLOR = 0xC9A8FF;

    public FangStaffItem() {
        super("fang_staff", COLOR, 20 * 12, Rarity.RARE);
    }

    @Override
    protected int interval() {
        return 20;
    }

    @Override
    protected void passive(ServerPlayer sp, ServerLevel sl, long time) {
        if (!RelicFx.foes(sl, sp.position(), 8, sp).isEmpty()) {
            sp.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 60, 0, true, false, true));
            Vec3 h = RelicFx.hand(sp);
            sl.sendParticles(Fx.wisp(COLOR, 0.5F), h.x, h.y, h.z, 1, 0.15, 0.15, 0.15, 0.0);
        }
    }

    @Override
    protected boolean active(ServerPlayer sp, ServerLevel sl, ItemStack stack) {
        float dmg = 7.0F * power(stack);
        Vec3 base = sp.position();
        Set<LivingEntity> bitten = new HashSet<>();
        if (sp.isShiftKeyDown()) {
            double[] radii = {1.8, 3.4, 5.0};
            for (int k = 0; k < radii.length; k++) {
                final double r = radii[k];
                Scheduler.runLater(1 + k * 3, () -> {
                    Vec3 c = new Vec3(base.x, RelicFx.groundY(sl, base.x, base.y, base.z), base.z);
                    Vfx.maw(sl, c, COLOR, r, 1.3, 24);
                    RelicSounds.play(sl, c, relicId(), "bite", 0.85F, 0.9F + (float) r * 0.05F);
                    Scheduler.runLater(4, () -> {
                        for (LivingEntity e : RelicFx.foes(sl, c, r + 1.6, sp)) {
                            double d = Math.hypot(e.getX() - c.x, e.getZ() - c.z);
                            if (Math.abs(d - r) <= 1.5 && bitten.add(e)) {
                                bite(sl, sp, e, dmg);
                            }
                        }
                    });
                });
            }
        } else {
            Vec3 look = sp.getLookAngle();
            Vec3 dir = new Vec3(look.x, 0, look.z);
            dir = dir.lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : dir.normalize();
            for (int i = 0; i < 9; i++) {
                final int idx = i;
                final Vec3 d = dir;
                Scheduler.runLater(1 + i * 2, () -> {
                    double dist = 1.8 + idx * 1.35;
                    double x = base.x + d.x * dist, z = base.z + d.z * dist;
                    Vec3 c = new Vec3(x, RelicFx.groundY(sl, x, base.y, z), z);
                    Vfx.maw(sl, c, COLOR, 0.9 + 0.04 * idx, 1.15 + 0.05 * idx, 22);
                    if (idx % 2 == 0) {
                        RelicSounds.playVaried(sl, c, relicId(), "bite", 0.8F);
                    }
                    Scheduler.runLater(3, () -> {
                        for (LivingEntity e : RelicFx.foes(sl, c, 2.2, sp)) {
                            if (Math.hypot(e.getX() - c.x, e.getZ() - c.z) <= 1.7 && bitten.add(e)) {
                                bite(sl, sp, e, dmg);
                            }
                        }
                    });
                });
            }
        }
        return true;
    }

    private static void bite(ServerLevel sl, ServerPlayer sp, LivingEntity e, float dmg) {
        RelicFx.damage(sl, sp, e, dmg);
        e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 2));
        e.setDeltaMovement(e.getDeltaMovement().add(0, 0.35, 0));
        e.hurtMarked = true;
        Vec3 c = e.getBoundingBox().getCenter();
        Vfx.flash(sl, c, COLOR, 1.4, 8);
        RelicFx.burst(sl, Fx.wisp(COLOR, 0.7F), c, 8, 0.2);
    }
}
