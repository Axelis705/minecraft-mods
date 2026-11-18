package com.mundosdeaxelis.relic;

import com.mundosdeaxelis.fx.Fx;
import com.mundosdeaxelis.fx.Vfx;
import com.mundosdeaxelis.network.ScreenFxPacket;
import com.mundosdeaxelis.vfx.VfxTex;
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

import com.mundosdeaxelis.vfx.VfxTex;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** Semilla del Árbol Mundo: la plantas y crece un árbol de luz que cura a los aliados y llena el suelo de flores. */
public class WorldTreeSeedItem extends RelicItem {
    private static final int COLOR = 0x7CFF8A;
    private static final Block[] FLOWERS = {Blocks.DANDELION, Blocks.POPPY, Blocks.AZURE_BLUET, Blocks.OXEYE_DAISY, Blocks.CORNFLOWER,
        Blocks.LILY_OF_THE_VALLEY, Blocks.ALLIUM, Blocks.PINK_TULIP, Blocks.RED_TULIP, Blocks.ORANGE_TULIP, Blocks.WHITE_TULIP, Blocks.BLUE_ORCHID};

    public WorldTreeSeedItem() {
        super("world_tree_seed", COLOR, 20 * 120, Rarity.EPIC);
    }

    @Override
    protected int interval() {
        return 40;
    }

    @Override
    protected int channelTicks() {
        return 40;
    }

    @Override
    protected UseAnim anim() {
        return UseAnim.BRUSH;
    }

    @Override
    protected void passive(ServerPlayer sp, ServerLevel sl, long time) {
        if (!RelicFx.isNight(sl) && sl.canSeeSky(sp.blockPosition())) {
            sp.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 90, 0, true, false, true));
            if (time % 120 == 0) {
                sl.sendParticles(Fx.petal(COLOR, 0.6F), sp.getX(), sp.getY() + 1.6, sp.getZ(), 3, 0.4, 0.3, 0.4, 0.01);
            }
        }
    }

    private static Vec3 plantSpot(ServerPlayer sp, ServerLevel sl) {
        Vec3 look = sp.getLookAngle();
        Vec3 flat = new Vec3(look.x, 0, look.z);
        flat = flat.lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : flat.normalize();
        Vec3 p = sp.position().add(flat.scale(2.6));
        return new Vec3(p.x, RelicFx.groundY(sl, p.x, sp.getY() + 1, p.z), p.z);
    }

    @Override
    protected boolean channelStart(ServerPlayer sp, ServerLevel sl, ItemStack stack) {
        RelicSounds.play(sl, plantSpot(sp, sl), relicId(), "plant", 0.9F, 1.0F);
        return true;
    }

    @Override
    protected void channelTick(ServerPlayer sp, ServerLevel sl, ItemStack stack, int used) {
        Vec3 at = plantSpot(sp, sl);
        float k = used / 40.0F;
        if (used % 4 == 0) {
            sl.sendParticles(RelicFx.blockDust(RelicFx.groundState(sl, at.x, at.y - 0.5, at.z)), at.x, at.y + 0.1, at.z, 6, 0.35, 0.05, 0.35, 0.1);
        }
        for (int i = 0; i < 2 + used / 10; i++) {
            double a = sl.random.nextDouble() * Math.PI * 2.0;
            double r = 2.2 * (1.0 - k) + 0.3;
            Vec3 from = at.add(Math.cos(a) * r, 0.1 + sl.random.nextDouble() * 1.8, Math.sin(a) * r);
            Vec3 v = at.add(0, 0.2, 0).subtract(from).scale(0.12);
            sl.sendParticles(Fx.spark(COLOR, 0.5F), from.x, from.y, from.z, 0, v.x, v.y, v.z, 1.0);
        }
        if (used % 10 == 0) {
            Vfx.ring(sl, at.add(0, 0.1, 0), COLOR, 0.6 + 1.6 * k, 12);
        }
    }

    @Override
    protected void channelEnd(ServerPlayer sp, ServerLevel sl, ItemStack stack, int used, boolean completed) {
        if (!completed && used < 38) {
            return;
        }
        float power = power(stack);
        Vec3 at = plantSpot(sp, sl);
        int dur = (int) (20 * 24 * power);
        RelicSounds.play(sl, at, relicId(), "grow", 1.0F, 1.0F);
        Vfx.flash(sl, at.add(0, 1, 0), 0xFFFFFF, 3.5, 14);
        Vfx.column(sl, at, COLOR, 0.9, 10.0, 28);
        Vfx.rune(sl, at.add(0, 0.05, 0), COLOR, 12.0, dur);
        Vfx.ring(sl, at.add(0, 0.1, 0), COLOR, 12.0, 24);
        Vfx.cross(sl, at, VfxTex.TREE, 11.0, 60, 0, null, 0xE0FFE0, dur);
        RelicFx.shakeNear(sl, at, 20, 0.9F, 14);
        Fx.screen(sp, ScreenFxPacket.PETALS, COLOR, 60);
        spend(sp, stack, cooldownTicks(), true);
        grow(sl, sp, at, dur, 0, power, new int[]{0});
    }

    private void grow(ServerLevel sl, ServerPlayer sp, Vec3 at, int dur, int age, float power, int[] flowers) {
        if (age >= dur) {
            RelicSounds.play(sl, at.add(0, 3, 0), relicId(), "end", 0.9F, 1.0F);
            Vfx.flash(sl, at.add(0, 5, 0), COLOR, 5.0, 16);
            RelicFx.burst(sl, Fx.petal(COLOR, 0.8F), at.add(0, 6, 0), 50, 0.25);
            RelicFx.burst(sl, Fx.spark(COLOR, 0.8F), at.add(0, 6, 0), 30, 0.4);
            return;
        }
        // hojas y polen
        if (age % 2 == 0) {
            double a = sl.random.nextDouble() * Math.PI * 2.0;
            double r = 1.5 + sl.random.nextDouble() * 4.0;
            double top = Math.min(1.0, age / 50.0);
            sl.sendParticles(Fx.petal(0xFFB0D8, 0.55F), at.x + Math.cos(a) * r, at.y + 7.0 * top + 2.0, at.z + Math.sin(a) * r, 0, 0, -0.04, 0, 1.0);
            sl.sendParticles(Fx.spark(COLOR, 0.35F), at.x + Math.cos(a) * r * 0.6, at.y + 0.3, at.z + Math.sin(a) * r * 0.6, 0, 0, 0.1, 0, 1.0);
        }
        if (age > 40 && age % 20 == 0) {
            for (LivingEntity a : RelicFx.allies(sl, at, 12.0, sp, true)) {
                a.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 60, 1, false, true, true));
                a.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 100, 0, false, true, true));
                sl.sendParticles(Fx.heart(COLOR, 0.6F), a.getX(), a.getY() + a.getBbHeight() + 0.2, a.getZ(), 2, 0.3, 0.2, 0.3, 0.01);
            }
            for (LivingEntity e : RelicFx.foes(sl, at, 9.0, sp)) {
                e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 50, 2, false, true, true));
                RelicFx.damage(sl, sp, e, 2.0F * power);
                sl.sendParticles(Fx.shard(0x4DBF5A, 0.6F), e.getX(), e.getY() + 0.2, e.getZ(), 6, 0.3, 0.1, 0.3, 0.05);
            }
        }
        if (age > 30 && age % 8 == 0 && flowers[0] < 70) {
            plantFlower(sl, at, flowers);
        }
        Scheduler.runLater(1, () -> grow(sl, sp, at, dur, age + 1, power, flowers));
    }

    private void plantFlower(ServerLevel sl, Vec3 at, int[] flowers) {
        for (int tries = 0; tries < 4; tries++) {
            double a = sl.random.nextDouble() * Math.PI * 2.0;
            double r = 2.0 + sl.random.nextDouble() * 8.0;
            int x = (int) Math.floor(at.x + Math.cos(a) * r);
            int z = (int) Math.floor(at.z + Math.sin(a) * r);
            int y = (int) Math.floor(RelicFx.groundY(sl, x + 0.5, at.y + 1, z + 0.5) + 0.01);
            net.minecraft.core.BlockPos pos = new net.minecraft.core.BlockPos(x, y, z);
            if (!sl.getBlockState(pos).isAir() || !sl.getBlockState(pos.below()).is(Blocks.GRASS_BLOCK)) {
                continue;
            }
            BlockState st = FLOWERS[sl.random.nextInt(FLOWERS.length)].defaultBlockState();
            if (!st.canSurvive(sl, pos)) {
                continue;
            }
            sl.setBlock(pos, st, 3);
            flowers[0]++;
            sl.sendParticles(Fx.petal(COLOR, 0.5F), x + 0.5, y + 0.4, z + 0.5, 4, 0.2, 0.2, 0.2, 0.02);
            sl.sendParticles(ParticleTypes.HAPPY_VILLAGER, x + 0.5, y + 0.4, z + 0.5, 3, 0.2, 0.2, 0.2, 0.0);
            return;
        }
    }
}
