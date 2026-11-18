package com.mundosdeaxelis.relic;

import com.mundosdeaxelis.fx.Fx;
import com.mundosdeaxelis.fx.Vfx;
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

/** Estandarte de la Victoria: lo clavas en el suelo y todos los aliados cercanos se vuelven más fuertes. */
public class VictoryBannerItem extends RelicItem {
    private static final int COLOR = 0xFFB02E;
    private static final double RADIUS = 11.0;

    public VictoryBannerItem() {
        super("victory_banner", COLOR, 20 * 70, Rarity.EPIC);
    }

    @Override
    protected int interval() {
        return 20;
    }

    @Override
    protected void passive(ServerPlayer sp, ServerLevel sl, long time) {
        for (LivingEntity a : RelicFx.allies(sl, sp.position(), 12, sp, false)) {
            sp.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 60, 0, true, false, true));
            if (time % 60 == 0) {
                Vec3 h = RelicFx.hand(sp);
                sl.sendParticles(Fx.spark(COLOR, 0.5F), h.x, h.y + 0.2, h.z, 3, 0.2, 0.2, 0.2, 0.0);
            }
            break;
        }
    }

    @Override
    protected boolean active(ServerPlayer sp, ServerLevel sl, ItemStack stack) {
        float power = power(stack);
        Vec3 look = RelicFx.look(sp);
        Vec3 flat = new Vec3(look.x, 0, look.z);
        flat = flat.lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : flat.normalize();
        final Vec3 flatDir = flat;
        Vec3 at0 = sp.position().add(flat.scale(3.0));
        final Vec3 at = new Vec3(at0.x, RelicFx.groundY(sl, at0.x, sp.getY() + 2, at0.z), at0.z);
        final int dur = (int) (20 * 18 * power);
        RelicSounds.play(sl, at, relicId(), "plant", 1.0F, 1.0F);
        // Golpe: el asta se clava
        Vfx.column(sl, at, COLOR, 0.7, 7.0, 20);
        Vfx.flash(sl, at.add(0, 0.3, 0), 0xFFFFFF, 3.0, 10);
        Vfx.ring(sl, at.add(0, 0.1, 0), COLOR, 8.0, 18);
        Vfx.ring(sl, at.add(0, 0.1, 0), 0xFFFFFF, 5.0, 12);
        RelicFx.shakeNear(sl, at, 18, 1.0F, 10);
        RelicFx.burst(sl, RelicFx.blockDust(RelicFx.groundState(sl, at.x, at.y - 0.5, at.z)), at.add(0, 0.2, 0), 40, 0.35);
        RelicFx.burst(sl, Fx.ember(COLOR, 0.8F), at.add(0, 0.3, 0), 24, 0.3);
        for (LivingEntity e : RelicFx.foes(sl, at, 4.5, sp)) {
            RelicFx.damage(sl, sp, e, 5.0F * power);
            RelicFx.knock(e, at, 0.7, 0.35);
        }
        Scheduler.runLater(4, () -> {
            Vfx.rune(sl, at.add(0, 0.05, 0), COLOR, RADIUS, dur);
            Vfx.cross(sl, at, VfxTex.BANNER, 4.2, 16, 1, flatDir, 0xFFEBC4, dur);
        });
        tickBanner(sl, sp, at, dur, 0, power);
        return true;
    }

    private void tickBanner(ServerLevel sl, ServerPlayer sp, Vec3 at, int dur, int age, float power) {
        if (age >= dur) {
            RelicSounds.play(sl, at, relicId(), "end", 0.9F, 1.0F);
            Vfx.flash(sl, at.add(0, 2, 0), COLOR, 3.0, 12);
            Vfx.ring(sl, at.add(0, 0.1, 0), COLOR, 6.0, 16);
            RelicFx.burst(sl, Fx.ember(COLOR, 0.8F), at.add(0, 2.0, 0), 30, 0.2);
            RelicFx.burst(sl, Fx.paper(0, 0.8F), at.add(0, 3.0, 0), 20, 0.25);
            return;
        }
        if (age % 20 == 0) {
            for (LivingEntity a : RelicFx.allies(sl, at, RADIUS, sp, true)) {
                a.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 60, power > 1.2F ? 1 : 0, false, true, true));
                a.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 60, 0, false, true, true));
                a.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 60, 0, false, true, true));
                sl.sendParticles(Fx.spark(COLOR, 0.6F), a.getX(), a.getY() + 1.0, a.getZ(), 4, 0.35, 0.5, 0.35, 0.02);
            }
            for (LivingEntity e : RelicFx.foes(sl, at, RADIUS, sp)) {
                e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 0, false, true, true));
                e.addEffect(new MobEffectInstance(MobEffects.GLOWING, 40, 0, false, false));
            }
        }
        if (age % 2 == 0) {
            double a = sl.random.nextDouble() * Math.PI * 2.0;
            double r = RADIUS * Math.sqrt(sl.random.nextDouble());
            sl.sendParticles(Fx.ember(COLOR, 0.6F), at.x + Math.cos(a) * r, at.y + 0.2, at.z + Math.sin(a) * r, 0, 0, 0.12, 0, 1.0);
            if (age % 6 == 0) {
                sl.sendParticles(Fx.paper(0, 0.7F), at.x + (sl.random.nextDouble() - 0.5), at.y + 4.2, at.z + (sl.random.nextDouble() - 0.5), 1, 0.4, 0.1, 0.4, 0.02);
            }
        }
        Scheduler.runLater(1, () -> tickBanner(sl, sp, at, dur, age + 1, power));
    }
}
