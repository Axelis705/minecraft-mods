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

import net.minecraft.world.level.Level;

/** Látigo del Dragón: latigazo de fuego que daña, prende y arrastra; agachado, restalla en círculo a tu alrededor. */
public class DragonWhipItem extends RelicItem {
    private static final int COLOR = 0xE8283C;
    private static final int FIRE = 0xFF7A1A;

    public DragonWhipItem() {
        super("dragon_whip", COLOR, 20 * 7, Rarity.EPIC);
    }

    @Override
    protected int interval() {
        return 20;
    }

    @Override
    protected void passive(ServerPlayer sp, ServerLevel sl, long time) {
        if (sl.dimension() == Level.NETHER) {
            sp.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 60, 0, true, false, true));
            sp.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 60, 0, true, false, true));
            if (time % 60 == 0) {
                Vec3 h = RelicFx.hand(sp);
                sl.sendParticles(Fx.ember(FIRE, 0.5F), h.x, h.y, h.z, 3, 0.15, 0.15, 0.15, 0.01);
            }
        }
    }

    @Override
    protected boolean active(ServerPlayer sp, ServerLevel sl, ItemStack stack) {
        float power = power(stack);
        if (sp.isShiftKeyDown()) {
            circle(sl, sp, power);
        } else {
            lash(sl, sp, power);
        }
        return true;
    }

    private void lash(ServerLevel sl, ServerPlayer sp, float power) {
        Vec3 hand = RelicFx.hand(sp);
        LivingEntity aimed = RelicFx.aimedFoe(sl, sp, 15, 0.93);
        Vec3 look = sp.getLookAngle();
        Vec3 tip = aimed != null ? aimed.getBoundingBox().getCenter() : sp.getEyePosition().add(look.scale(12.0));
        // no atraviesa paredes: se queda en el primer bloque
        net.minecraft.world.phys.BlockHitResult bh = sl.clip(new net.minecraft.world.level.ClipContext(sp.getEyePosition(), tip,
                net.minecraft.world.level.ClipContext.Block.COLLIDER, net.minecraft.world.level.ClipContext.Fluid.NONE, sp));
        if (bh.getType() != net.minecraft.world.phys.HitResult.Type.MISS && aimed == null) {
            tip = bh.getLocation();
        }
        final Vec3 end = tip;
        Vfx.whip(sl, hand, end, FIRE, 0.3, 12, false, 0.9, 3.0);
        Vfx.whip(sl, hand, end, 0xFFD070, 0.12, 10, false, 0.5, 3.0);
        RelicFx.shake(sp, 0.5F, 6);
        Scheduler.runLater(4, () -> {
            RelicSounds.play(sl, end, relicId(), "crack", 1.0F, 1.0F);
            Vfx.flash(sl, end, FIRE, 3.0, 9);
            Vfx.ring(sl, end, COLOR, 2.6, 12);
            RelicFx.burst(sl, ParticleTypes.FLAME, end, 22, 0.28);
            RelicFx.burst(sl, Fx.ember(FIRE, 0.8F), end, 14, 0.3);
            RelicFx.burst(sl, ParticleTypes.LAVA, end, 4, 0.1);
            boolean first = true;
            for (LivingEntity e : RelicFx.foesAlong(sl, hand, end, 1.3, sp)) {
                boolean main = first && aimed != null ? e == aimed : first;
                first = false;
                RelicFx.damage(sl, sp, e, (main ? 9.0F : 5.0F) * power);
                e.setSecondsOnFire(5);
                RelicSounds.playVaried(sl, e.getBoundingBox().getCenter(), relicId(), "hit", 0.8F);
                Vfx.flash(sl, e.getBoundingBox().getCenter(), FIRE, 1.8, 8);
                if (main || e == aimed) {
                    Vec3 pull = sp.position().subtract(e.position());
                    Vec3 h = new Vec3(pull.x, 0, pull.z);
                    h = h.lengthSqr() < 0.04 ? Vec3.ZERO : h.normalize();
                    double dist = Math.min(2.2, Math.sqrt(pull.x * pull.x + pull.z * pull.z));
                    e.setDeltaMovement(h.x * (0.45 + dist * 0.25), 0.3, h.z * (0.45 + dist * 0.25));
                    e.hurtMarked = true;
                }
            }
        });
    }

    private void circle(ServerLevel sl, ServerPlayer sp, float power) {
        Vec3 c = sp.position().add(0, 0.9, 0);
        double reach = 7.5;
        RelicSounds.play(sl, c, relicId(), "crack", 1.0F, 0.85F);
        Vfx.ring(sl, sp.position().add(0, 0.1, 0), FIRE, reach + 1.0, 18);
        RelicFx.shake(sp, 0.9F, 8);
        Set<LivingEntity> hit = new HashSet<>();
        int steps = 8;
        for (int s = 0; s < steps; s++) {
            final int k = s;
            Scheduler.runLater(s, () -> {
                if (!sp.isAlive()) {
                    return;
                }
                double ang = k * Math.PI * 2.0 / steps * 1.0 + 0.4;
                for (int w = 0; w < 2; w++) {
                    double a = ang + w * Math.PI;
                    Vec3 end = c.add(Math.cos(a) * reach, 0.2, Math.sin(a) * reach);
                    Vfx.whip(sl, c, end, w == 0 ? FIRE : 0xFFB040, 0.26, 8, false, 0.6, 2.0);
                    RelicFx.burst(sl, ParticleTypes.FLAME, end, 6, 0.12);
                    Vfx.flash(sl, end, FIRE, 1.4, 6);
                }
                for (LivingEntity e : RelicFx.foes(sl, c, reach + 0.6, sp)) {
                    if (hit.contains(e)) {
                        continue;
                    }
                    Vec3 d = e.position().subtract(c);
                    double bearing = Math.atan2(d.z, d.x);
                    for (int w = 0; w < 2; w++) {
                        double a = ang + w * Math.PI;
                        double diff = Math.abs(Mth.wrapDegrees((float) Math.toDegrees(bearing - a)));
                        if (diff <= 32.0) {
                            hit.add(e);
                            RelicFx.damage(sl, sp, e, 7.0F * power);
                            e.setSecondsOnFire(4);
                            RelicFx.knock(e, c, 0.9, 0.3);
                            RelicSounds.playVaried(sl, e.getBoundingBox().getCenter(), relicId(), "hit", 0.7F);
                            Vfx.flash(sl, e.getBoundingBox().getCenter(), FIRE, 1.8, 8);
                            break;
                        }
                    }
                }
                if (k % 3 == 0) {
                    sl.playSound(null, c.x, c.y, c.z, SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 0.5F, 1.2F);
                }
            });
        }
    }
}
