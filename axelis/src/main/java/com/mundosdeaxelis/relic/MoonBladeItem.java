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

import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.state.BlockState;

/** Filo Lunar: dos cuchilladas de luna cruzan el aire y cortan todo lo que encuentran. */
public class MoonBladeItem extends RelicItem {
    private static final int COLOR = 0xCFE0FF;
    private static final int LIFE = 9;
    private static final double REACH = 15.0;

    public MoonBladeItem() {
        super("moon_blade", COLOR, 20 * 14, Rarity.EPIC);
    }

    @Override
    protected int interval() {
        return 20;
    }

    @Override
    protected void passive(ServerPlayer sp, ServerLevel sl, long time) {
        if (RelicFx.isNight(sl)) {
            sp.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 60, 0, true, false, true));
            if (sl.getMoonPhase() == 0) {
                sp.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 60, 0, true, false, true));
            }
            if (time % 60 == 0) {
                Vec3 h = RelicFx.hand(sp);
                sl.sendParticles(Fx.spark(COLOR, 0.5F), h.x, h.y, h.z, 3, 0.15, 0.15, 0.15, 0.0);
            }
        }
    }

    @Override
    protected boolean active(ServerPlayer sp, ServerLevel sl, ItemStack stack) {
        float power = power(stack);
        boolean full = RelicFx.isNight(sl) && sl.getMoonPhase() == 0;
        float damage = (7.0F + (full ? 3.0F : 0.0F)) * power;
        Vec3 look = sp.getLookAngle();
        Vec3 from = sp.getEyePosition().add(look.scale(1.0)).add(0, -0.25, 0);
        RelicSounds.play(sl, from, relicId(), "wave", 1.0F, 1.0F);
        RelicFx.shake(sp, 0.6F, 6);
        for (int i = 0; i < 2; i++) {
            final int k = i;
            final double roll = i == 0 ? 0.55 : -0.55;
            Scheduler.runLater(i * 3, () -> launch(sl, sp, from, look, roll, damage, k));
        }
        return true;
    }

    private void launch(ServerLevel sl, ServerPlayer sp, Vec3 from, Vec3 dir, double roll, float damage, int k) {
        Vec3 travel = dir.scale(REACH);
        Vfx.slash(sl, from, travel, k == 0 ? COLOR : 0xFFFFFF, 3.6, roll, LIFE);
        Vfx.make(sl, VfxType.SLASH).at(from).dir(travel).rgb(0x8FB0FF).size(4.4).a(roll).b(0.35).life(LIFE + 2).send();
        Set<LivingEntity> hit = new HashSet<>();
        for (int t = 1; t <= LIFE; t++) {
            final int tt = t;
            Scheduler.runLater(t, () -> {
                double e0 = ease((tt - 1) / (double) LIFE);
                double e1 = ease(tt / (double) LIFE);
                Vec3 a = from.add(travel.scale(e0));
                Vec3 b = from.add(travel.scale(e1));
                RelicFx.line(sl, Fx.spark(COLOR, 0.5F), a, b, 0.9);
                sl.sendParticles(Fx.slash(COLOR, 0.8F), b.x, b.y, b.z, 1, 0.5, 0.5, 0.5, 0.0);
                for (LivingEntity e : RelicFx.foesAlong(sl, a, b, 1.7, sp)) {
                    if (hit.add(e)) {
                        RelicFx.damage(sl, sp, e, damage);
                        e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1));
                        Vec3 c = e.getBoundingBox().getCenter();
                        Vfx.flash(sl, c, COLOR, 1.8, 8);
                        RelicSounds.playVaried(sl, c, relicId(), "hit", 0.8F);
                        RelicFx.burst(sl, Fx.spark(0xFFFFFF, 0.7F), c, 10, 0.25);
                        RelicFx.knock(e, from, 0.3, 0.12);
                    }
                }
                cut(sl, b);
            });
        }
    }

    private static double ease(double t) {
        double u = 1.0 - Math.max(0.0, Math.min(1.0, t));
        return 1.0 - u * u * u;
    }

    /** Corta hierba y flores que cruza. */
    private void cut(ServerLevel sl, Vec3 p) {
        net.minecraft.core.BlockPos c = net.minecraft.core.BlockPos.containing(p);
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    net.minecraft.core.BlockPos bp = c.offset(dx, dy, dz);
                    BlockState st = sl.getBlockState(bp);
                    if (st.is(BlockTags.FLOWERS) || st.is(BlockTags.REPLACEABLE_BY_TREES) && st.getDestroySpeed(sl, bp) == 0.0F && !st.is(BlockTags.SAPLINGS)) {
                        sl.destroyBlock(bp, true);
                    }
                }
            }
        }
    }
}
