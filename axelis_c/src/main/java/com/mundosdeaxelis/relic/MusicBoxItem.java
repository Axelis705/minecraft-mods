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
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

/** Caja de Música: le das cuerda y su nana duerme a los enemigos cercanos y calma a los aliados. */
public class MusicBoxItem extends RelicItem {
    private static final int COLOR = 0xE6C8FF;
    private static final int SONG_TICKS = 196;
    private static final String SLEEP = "mdc_sleep_until";

    public MusicBoxItem() {
        super("music_box", COLOR, 20 * 50, Rarity.RARE);
    }

    @Override
    protected int interval() {
        return 20;
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
        CompoundTag d = sp.getPersistentData();
        double dx = sp.getX() - d.getDouble("mdc_mb_x");
        double dz = sp.getZ() - d.getDouble("mdc_mb_z");
        int still = d.getInt("mdc_mb_still");
        if (dx * dx + dz * dz < 0.25 && Math.abs(sp.getY() - d.getDouble("mdc_mb_y")) < 0.5) {
            still += 20;
        } else {
            still = 0;
        }
        d.putDouble("mdc_mb_x", sp.getX());
        d.putDouble("mdc_mb_y", sp.getY());
        d.putDouble("mdc_mb_z", sp.getZ());
        d.putInt("mdc_mb_still", still);
        if (still >= 60) {
            sp.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 60, 0, true, false, true));
            sl.sendParticles(Fx.note(COLOR, 0.7F), sp.getX(), sp.getY() + 1.9, sp.getZ(), 1, 0.3, 0.1, 0.3, 0.0);
        }
    }

    @Override
    protected boolean channelStart(ServerPlayer sp, ServerLevel sl, ItemStack stack) {
        sl.playSound(null, sp.getX(), sp.getY(), sp.getZ(), SoundEvents.NOTE_BLOCK_HAT.value(), SoundSource.PLAYERS, 0.6F, 0.9F);
        return true;
    }

    @Override
    protected void channelTick(ServerPlayer sp, ServerLevel sl, ItemStack stack, int used) {
        if (used % 5 == 0) {
            sl.playSound(null, sp.getX(), sp.getY(), sp.getZ(), SoundEvents.NOTE_BLOCK_HAT.value(), SoundSource.PLAYERS, 0.5F, 0.9F + used * 0.016F);
            Vec3 h = RelicFx.hand(sp);
            sl.sendParticles(Fx.note(COLOR, 0.6F), h.x, h.y + 0.2, h.z, 1, 0.15, 0.1, 0.15, 0.0);
            Vfx.ring(sl, sp.position().add(0, 0.1, 0), COLOR, 0.8 + used * 0.03, 10);
        }
    }

    @Override
    protected void channelEnd(ServerPlayer sp, ServerLevel sl, ItemStack stack, int used, boolean completed) {
        if (!completed && used < 36) {
            return;
        }
        float power = power(stack);
        double radius = 13.0 + 1.2 * (level(stack) - 1);
        Vec3 c = sp.position();
        RelicSounds.play(sl, c.add(0, 1, 0), relicId(), "song", 1.0F, 1.0F);
        Vfx.rune(sl, c, COLOR, radius * 0.55, SONG_TICKS);
        Vfx.dome(sl, c.add(0, 1.0, 0), 0xC9A0FF, 2.4, 40, false);
        Vfx.ring(sl, c.add(0, 0.1, 0), COLOR, radius, 30);
        Vfx.ring(sl, c.add(0, 0.1, 0), 0xFFFFFF, radius * 0.7, 24);
        Fx.screen(sp, ScreenFxPacket.STARS, COLOR, 50);
        spend(sp, stack, cooldownTicks(), true);
        final int dur = SONG_TICKS + 4;
        lullaby(sl, sp, c, radius, 0, dur, power, new HashSet<>());
    }

    private void lullaby(ServerLevel sl, ServerPlayer sp, Vec3 c, double radius, int age, int dur, float power, Set<Integer> marked) {
        if (age >= dur || !sp.isAlive()) {
            RelicSounds.play(sl, c, relicId(), "wake", 0.5F, 1.2F);
            return;
        }
        if (age % 10 == 0) {
            double base = age * 0.2;
            for (int i = 0; i < 6; i++) {
                double a = base + i * Math.PI / 3.0;
                double r = 1.0 + (age % 40) * 0.05;
                sl.sendParticles(Fx.note(COLOR, 0.8F), c.x + Math.cos(a) * r, c.y + 0.6, c.z + Math.sin(a) * r, 0, 0, 0.12, 0, 1.0);
            }
            sl.sendParticles(Fx.note(0xFFFFFF, 0.7F), sp.getX(), sp.getY() + 2.2, sp.getZ(), 1, 0.4, 0.1, 0.4, 0.0);
            // el centro canta
            Vfx.ring(sl, c.add(0, 0.1, 0), COLOR, 3.0, 16);
        }
        if (age % 10 == 2) {
            for (LivingEntity e : RelicFx.foes(sl, c, radius, sp)) {
                if (e.getMaxHealth() > 120.0F) {
                    continue;
                }
                e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 6, false, false, false));
                e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 30, 4, false, false, false));
                e.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 30, 3, false, false, false));
                if (e instanceof Mob m) {
                    m.setTarget(null);
                }
                e.getPersistentData().putLong(SLEEP, sl.getGameTime() + 30);
                if (marked.add(e.getId())) {
                    int id = Vfx.spriteFollow(sl, e, 0, e.getBbHeight() + 0.5, 0, VfxTex.ZZZ, 0.8, 0xE6C8FF, true, dur - age + 6);
                    e.getPersistentData().putInt("mdc_sleep_vfx", id);
                    e.getPersistentData().putInt("mdc_sleep_dim", System.identityHashCode(sl));
                }
                sl.sendParticles(Fx.note(COLOR, 0.5F), e.getX(), e.getY() + e.getBbHeight() + 0.1, e.getZ(), 1, 0.2, 0.1, 0.2, 0.0);
            }
        }
        if (age % 20 == 0) {
            for (LivingEntity a : RelicFx.allies(sl, c, radius, sp, true)) {
                a.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 60, 1, false, true, true));
                a.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 60, 0, false, true, true));
                a.removeEffect(MobEffects.POISON);
                a.removeEffect(MobEffects.WITHER);
                sl.sendParticles(Fx.heart(COLOR, 0.5F), a.getX(), a.getY() + a.getBbHeight() + 0.2, a.getZ(), 1, 0.3, 0.1, 0.3, 0.0);
            }
        }
        Scheduler.runLater(1, () -> lullaby(sl, sp, c, radius, age + 1, dur, power, marked));
    }

    /** Un enemigo dormido que recibe un golpe se despierta, y ese primer golpe duele un 50 % más. */
    public static void onHurt(LivingHurtEvent event) {
        LivingEntity e = event.getEntity();
        CompoundTag d = e.getPersistentData();
        if (!d.contains(SLEEP) || e.level().isClientSide) {
            return;
        }
        long until = d.getLong(SLEEP);
        d.remove(SLEEP);
        if (e.level().getGameTime() > until) {
            return;
        }
        event.setAmount(event.getAmount() * 1.5F);
        e.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
        e.removeEffect(MobEffects.WEAKNESS);
        e.removeEffect(MobEffects.DIG_SLOWDOWN);
        if (e.level() instanceof ServerLevel sl) {
            if (d.contains("mdc_sleep_vfx")) {
                Vfx.remove(sl, d.getInt("mdc_sleep_vfx"));
                d.remove("mdc_sleep_vfx");
                d.remove("mdc_sleep_dim");
            }
            Vec3 p = e.getBoundingBox().getCenter();
            RelicSounds.play(sl, p, "music_box", "wake", 0.7F, 1.0F);
            Vfx.flash(sl, p, COLOR, 1.8, 8);
            sl.sendParticles(Fx.note(0xFFFFFF, 0.8F), p.x, p.y + 0.6, p.z, 6, 0.4, 0.3, 0.4, 0.05);
        }
    }
}
