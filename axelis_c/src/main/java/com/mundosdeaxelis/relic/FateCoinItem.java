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

import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Items;

/** Moneda del Destino: la lanzas y el azar decide. Cara, fortuna; cruz, tormenta sobre los enemigos. */
public class FateCoinItem extends RelicItem {
    private static final int COLOR = 0xFFD34D;

    public FateCoinItem() {
        super("fate_coin", COLOR, 20 * 25, Rarity.RARE);
    }

    @Override
    protected int interval() {
        return 100;
    }

    @Override
    protected void passive(ServerPlayer sp, ServerLevel sl, long time) {
        if (sl.random.nextFloat() < 0.15F) {
            ItemStack gift = sl.random.nextFloat() < 0.06F ? new ItemStack(Items.GOLD_INGOT) : new ItemStack(Items.GOLD_NUGGET, 1 + sl.random.nextInt(3));
            ItemEntity it = new ItemEntity(sl, sp.getX(), sp.getY() + 0.3, sp.getZ(), gift);
            it.setDeltaMovement((sl.random.nextDouble() - 0.5) * 0.2, 0.25, (sl.random.nextDouble() - 0.5) * 0.2);
            sl.addFreshEntity(it);
            sl.sendParticles(Fx.spark(COLOR, 0.6F), sp.getX(), sp.getY() + 0.5, sp.getZ(), 5, 0.3, 0.3, 0.3, 0.02);
            sl.playSound(null, sp.getX(), sp.getY(), sp.getZ(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.5F, 1.6F);
        }
    }

    @Override
    protected boolean active(ServerPlayer sp, ServerLevel sl, ItemStack stack) {
        boolean heads = sl.random.nextBoolean();
        float power = power(stack);
        Vec3 top = sp.position().add(0, 1.5, 0);
        Vfx.coin(sl, top, heads ? 0 : 1, 1.9, 56);
        RelicSounds.play(sl, top, relicId(), "flip", 0.9F, 1.0F);
        Scheduler.runLater(37, () -> {
            if (!sp.isAlive()) {
                return;
            }
            Vec3 at = new Vec3(top.x, RelicFx.groundY(sl, top.x, top.y - 2, top.z), top.z);
            if (heads) {
                heads(sl, sp, at, power);
            } else {
                tails(sl, sp, at, power);
            }
        });
        return true;
    }

    private void heads(ServerLevel sl, ServerPlayer sp, Vec3 at, float power) {
        RelicSounds.play(sl, at, relicId(), "heads", 1.0F, 1.0F);
        Vfx.column(sl, at, COLOR, 0.8, 8, 40);
        Vfx.rune(sl, at, COLOR, 6.5, 50);
        Vfx.ring(sl, at.add(0, 0.1, 0), 0xFFFFFF, 9.0, 22);
        Vfx.flash(sl, at.add(0, 1, 0), COLOR, 3.0, 14);
        for (LivingEntity a : RelicFx.allies(sl, at, 12, sp, true)) {
            a.addEffect(new MobEffectInstance(MobEffects.LUCK, 1200, 1, false, true, true));
            a.addEffect(new MobEffectInstance(MobEffects.REGENERATION, (int) (120 * power), 1, false, true, true));
            a.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 600, 1, false, true, true));
            sl.sendParticles(Fx.spark(COLOR, 0.7F), a.getX(), a.getY() + 1.0, a.getZ(), 10, 0.4, 0.6, 0.4, 0.05);
        }
        int n = 5 + sl.random.nextInt(4);
        for (int i = 0; i < n; i++) {
            ItemEntity it = new ItemEntity(sl, at.x, at.y + 1.2, at.z, new ItemStack(Items.GOLD_NUGGET, 2 + sl.random.nextInt(3)));
            it.setDeltaMovement((sl.random.nextDouble() - 0.5) * 0.5, 0.35 + sl.random.nextDouble() * 0.2, (sl.random.nextDouble() - 0.5) * 0.5);
            sl.addFreshEntity(it);
        }
        sl.addFreshEntity(new ExperienceOrb(sl, at.x, at.y + 1.0, at.z, 14));
        RelicFx.burst(sl, Fx.spark(COLOR, 0.9F), at.add(0, 1.2, 0), 40, 0.5);
        RelicFx.burst(sl, Fx.paper(0, 0.8F), at.add(0, 1.4, 0), 30, 0.4);
    }

    private void tails(ServerLevel sl, ServerPlayer sp, Vec3 at, float power) {
        RelicSounds.play(sl, at, relicId(), "tails", 1.0F, 1.0F);
        Vfx.rune(sl, at, 0x9FB4FF, 7.0, 40);
        Vfx.ring(sl, at.add(0, 0.1, 0), 0x9FB4FF, 12.0, 20);
        RelicFx.shakeNear(sl, at, 20, 1.4F, 14);
        List<LivingEntity> foes = RelicFx.foes(sl, at, 12, sp);
        for (int i = 0; i < foes.size() && i < 8; i++) {
            final LivingEntity e = foes.get(i);
            Scheduler.runLater(i * 3, () -> {
                if (!e.isAlive()) {
                    return;
                }
                Vec3 c = e.getBoundingBox().getCenter();
                Vfx.bolt(sl, c.add(0, 14, 0), c, 0xCFD8FF, 0.09, 8);
                Vfx.flash(sl, c, 0xFFFFFF, 2.4, 8);
                sl.playSound(null, c.x, c.y, c.z, SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 0.7F, 1.3F);
                RelicFx.damage(sl, sp, e, 9.0F * power);
                e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 2));
                RelicFx.burst(sl, ParticleTypes.ELECTRIC_SPARK, c, 14, 0.4);
            });
        }
        if (foes.isEmpty()) {
            Vfx.bolt(sl, at.add(2, 14, 1), at, 0xCFD8FF, 0.09, 8);
            sl.playSound(null, at.x, at.y, at.z, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 0.8F, 1.2F);
        }
    }
}
