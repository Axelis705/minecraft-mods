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

/** Corazón de Estrella: te protege con absorción y dispara un rayo de estrella que atraviesa todo, hiere a los enemigos y cura a los aliados. */
public class StarHeartItem extends RelicItem {
    private static final int COLOR = 0xFF9CF0;
    private static final double RANGE = 42.0;

    public StarHeartItem() {
        super("star_heart", COLOR, 20 * 40, Rarity.EPIC);
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
        return UseAnim.BOW;
    }

    @Override
    protected void passive(ServerPlayer sp, ServerLevel sl, long time) {
        long last = sp.getPersistentData().getLong("mdc_sh_last");
        if (time - last >= 600 && sp.getAbsorptionAmount() < 3.5F) {
            sp.getPersistentData().putLong("mdc_sh_last", time);
            sp.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 1200, 0, true, false, true));
            Vfx.ring(sl, sp.position().add(0, 0.1, 0), COLOR, 1.8, 14);
            RelicFx.sphere(sl, Fx.spark(COLOR, 0.6F), sp.position().add(0, 1.0, 0), 0.9, 18, 0);
            RelicFx.burst(sl, Fx.heart(COLOR, 0.6F), sp.position().add(0, 1.2, 0), 3, 0.1);
            sl.playSound(null, sp.getX(), sp.getY(), sp.getZ(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.5F, 1.5F);
        }
    }

    @Override
    protected boolean channelStart(ServerPlayer sp, ServerLevel sl, ItemStack stack) {
        RelicSounds.play(sl, sp, relicId(), "charge", 0.8F, 1.0F);
        Vec3 look = sp.getLookAngle();
        Vfx.runeFacing(sl, sp.getEyePosition().add(look.scale(1.8)), look, COLOR, 1.1, 42);
        sp.getPersistentData().putInt("mdc_sh_orb", Vfx.orb(sl, RelicFx.hand(sp).add(look.scale(0.7)), COLOR, 0.2, 60, false));
        return true;
    }

    @Override
    protected void channelTick(ServerPlayer sp, ServerLevel sl, ItemStack stack, int used) {
        float k = used / 40.0F;
        Vec3 look = sp.getLookAngle();
        Vec3 h = RelicFx.hand(sp).add(look.scale(0.7));
        Vfx.make(sl, VfxType.ORB).at(h).rgb(used > 30 ? 0xFFFFFF : COLOR).size(0.2 + 0.7 * k).life(0).update(sp.getPersistentData().getInt("mdc_sh_orb"));
        double r = 2.0 * (1.0 - k) + 0.3;
        for (int i = 0; i < 2 + used / 8; i++) {
            double a = sl.random.nextDouble() * Math.PI * 2.0;
            double e = (sl.random.nextDouble() - 0.5) * 2.0;
            Vec3 from = h.add(Math.cos(a) * r, e * r * 0.8, Math.sin(a) * r);
            Vec3 v = h.subtract(from).scale(0.15);
            sl.sendParticles(Fx.spark(i % 2 == 0 ? COLOR : 0xFFFFFF, 0.5F + k * 0.5F), from.x, from.y, from.z, 0, v.x, v.y, v.z, 1.0);
        }
        if (used % 6 == 0) {
            Vfx.ringFacing(sl, h.add(look.scale(0.5)), look, COLOR, 0.4 + 0.6 * k, 9);
        }
        if (used % 10 == 0 && used > 0) {
            sl.playSound(null, h.x, h.y, h.z, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.4F, 0.8F + k);
        }
    }

    @Override
    protected void channelEnd(ServerPlayer sp, ServerLevel sl, ItemStack stack, int used, boolean completed) {
        Vfx.remove(sl, sp.getPersistentData().getInt("mdc_sh_orb"));
        if (used < 12) {
            return;
        }
        float charge = Math.min(1.0F, used / 40.0F);
        float power = power(stack);
        Vec3 look = sp.getLookAngle();
        Vec3 from = RelicFx.hand(sp).add(look.scale(0.5));
        Vec3 to = sp.getEyePosition().add(look.scale(RANGE));
        RelicSounds.play(sl, from, relicId(), "beam", 1.0F, 1.0F);
        RelicFx.shake(sp, 1.4F, 10);
        Fx.screen(sp, ScreenFxPacket.FLASH, COLOR, 10);
        double width = 0.5 + 0.7 * charge;
        int life = 14;
        Vfx.beam(sl, from, to, COLOR, width * 1.7, life, VfxType.F_CAPS | VfxType.F_FLICKER);
        Vfx.beam(sl, from, to, 0xFFD2F8, width, life, VfxType.F_CAPS | VfxType.F_FLICKER);
        Vfx.beam(sl, from, to, 0xFFFFFF, width * 0.45, life, VfxType.F_CAPS);
        Vfx.flash(sl, from, COLOR, 3.2, 10);
        Vfx.ringFacing(sl, from.add(look.scale(0.4)), look, COLOR, 2.2, 14);
        for (int i = 1; i <= 6; i++) {
            final int k = i;
            Scheduler.runLater(i, () -> Vfx.ringFacing(sl, from.add(look.scale(k * 6.0)), look, 0xFFB8F2, 1.5 + k * 0.1, 10));
        }
        // la luz hiere a los enemigos y cura a los aliados que atraviesa
        float dmg = (10.0F + 8.0F * charge) * power;
        for (LivingEntity e : RelicFx.foesAlong(sl, from, to, 1.0 + width, sp)) {
            RelicFx.damage(sl, sp, e, dmg);
            Vec3 c = e.getBoundingBox().getCenter();
            Vfx.flash(sl, c, COLOR, 2.4, 10);
            RelicSounds.playVaried(sl, c, relicId(), "hit", 0.8F);
            RelicFx.burst(sl, Fx.spark(0xFFFFFF, 0.8F), c, 12, 0.35);
            RelicFx.knock(e, from, 0.5, 0.2);
        }
        Vec3 mid = from.add(to).scale(0.5);
        for (LivingEntity a : RelicFx.allies(sl, mid, RANGE * 0.5 + 3, sp, false)) {
            if (RelicFx.segDist(a.getBoundingBox().getCenter(), from, to) <= 1.8) {
                a.heal(6.0F * power);
                a.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 1, false, true, true));
                Vfx.flash(sl, a.getBoundingBox().getCenter(), 0xFFFFFF, 1.8, 8);
                RelicFx.burst(sl, Fx.heart(COLOR, 0.7F), a.getBoundingBox().getCenter(), 5, 0.15);
            }
        }
        // chispas a lo largo
        RelicFx.line(sl, Fx.spark(COLOR, 0.7F), from, to, 1.6);
        RelicFx.line(sl, Fx.dust(0xFFFFFF, 0.9F), from, to, 2.4);
        // impacto al final (donde choca con un bloque, si lo hay)
        net.minecraft.world.phys.BlockHitResult bh = sl.clip(new net.minecraft.world.level.ClipContext(sp.getEyePosition(), to,
                net.minecraft.world.level.ClipContext.Block.COLLIDER, net.minecraft.world.level.ClipContext.Fluid.NONE, sp));
        if (bh.getType() != net.minecraft.world.phys.HitResult.Type.MISS) {
            Vec3 e = bh.getLocation();
            Vfx.flash(sl, e, COLOR, 3.4, 12);
            RelicFx.burst(sl, Fx.spark(COLOR, 0.8F), e, 20, 0.35);
            RelicFx.burst(sl, RelicFx.blockDust(sl.getBlockState(bh.getBlockPos())), e, 14, 0.2);
        }
        spend(sp, stack, cooldownTicks(), true);
    }
}
