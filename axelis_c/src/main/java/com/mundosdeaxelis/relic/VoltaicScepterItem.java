package com.mundosdeaxelis.relic;

import com.mundosdeaxelis.fx.Fx;
import com.mundosdeaxelis.network.ScreenFxPacket;
import com.mundosdeaxelis.registry.ModSounds;
import com.mundosdeaxelis.world.Scheduler;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/** Cetro Voltaico: se carga manteniendo el clic y suelta un rayo que salta entre los enemigos. */
public class VoltaicScepterItem extends RelicItem {
    private static final int COLOR = 0x5CC8FF;

    public VoltaicScepterItem() {
        super("voltaic_scepter", COLOR, 20 * 14, Rarity.EPIC);
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
        if (sl.isRaining() || sl.isThundering()) {
            sp.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 40, 0, true, false, true));
            sp.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 40, 0, true, false, true));
        }
        if (time % 40 == 0) {
            Vec3 h = RelicFx.hand(sp);
            sl.sendParticles(ParticleTypes.ELECTRIC_SPARK, h.x, h.y, h.z, 3, 0.2, 0.2, 0.2, 0.3);
        }
    }

    @Override
    protected boolean channelStart(ServerPlayer sp, ServerLevel sl, ItemStack stack) {
        RelicSounds.play(sl, sp, relicId(), "charge", 0.85F, 1.0F);
        return true;
    }

    @Override
    protected void channelTick(ServerPlayer sp, ServerLevel sl, ItemStack stack, int used) {
        Vec3 h = RelicFx.hand(sp);
        float k = used / 40.0F;
        double r = 1.9 * (1.0 - k) + 0.35;
        for (int i = 0; i < 2 + used / 10; i++) {
            double a = sl.random.nextDouble() * Math.PI * 2.0;
            double e = (sl.random.nextDouble() - 0.5) * 2.0;
            Vec3 from = h.add(Math.cos(a) * r, e * r * 0.7, Math.sin(a) * r);
            Vec3 v = h.subtract(from).scale(0.13);
            sl.sendParticles(Fx.spark(COLOR, 0.6F + k * 0.6F), from.x, from.y, from.z, 0, v.x, v.y, v.z, 1.0);
        }
        if (used % 3 == 0) {
            Vec3 from = h.add((sl.random.nextDouble() - 0.5) * 1.4, (sl.random.nextDouble() - 0.5) * 1.4, (sl.random.nextDouble() - 0.5) * 1.4);
            RelicFx.bolt(sl, from, h, COLOR);
        }
        if (used % 5 == 0) {
            Fx.ring(sl, Fx.dust(COLOR, 0.6F), sp.getX(), sp.getY() + 0.1, sp.getZ(), 0.8 + k * 1.2, 14, used * 0.3);
        }
        if (used % 6 == 0) {
            Fx.sound(sl, sp.getX(), sp.getY(), sp.getZ(), ModSounds.RELIC_MAGIC.get(), SoundSource.PLAYERS, 0.35F, 0.6F + k * 1.0F);
        }
        if (used == 20) {
            RelicFx.shake(sp, 0.4F, 20);
        }
    }

    @Override
    protected void channelEnd(ServerPlayer sp, ServerLevel sl, ItemStack stack, int used, boolean completed) {
        RelicSounds.stop(sp, relicId(), "charge");
        if (used < 8) {
            return;
        }
        float charge = Math.min(1.0F, used / 40.0F);
        Vec3 h = RelicFx.hand(sp);
        LivingEntity first = RelicFx.aimedFoe(sl, sp, 30, 0.9);
        List<LivingEntity> chain = new ArrayList<>();
        if (first != null) {
            chain.add(first);
            LivingEntity cur = first;
            int max = 2 + (int) (charge * 5);
            while (chain.size() < max) {
                LivingEntity next = null;
                for (LivingEntity e : RelicFx.foes(sl, cur.position(), 8, sp)) {
                    if (!chain.contains(e)) {
                        next = e;
                        break;
                    }
                }
                if (next == null) {
                    break;
                }
                chain.add(next);
                cur = next;
            }
        }
        Fx.sound(sl, sp.getX(), sp.getY(), sp.getZ(), SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 0.45F, 1.3F);
        RelicSounds.play(sl, h, relicId(), "zap", 1.0F, 1.0F);
        Fx.screen(sp, ScreenFxPacket.VIGNETTE, COLOR, 12);
        RelicFx.shake(sp, 1.6F * charge + 0.4F, 10);
        if (chain.isEmpty()) {
            Vec3 end = RelicFx.target(sp, 22);
            RelicFx.bolt(sl, h, end, COLOR);
            RelicFx.bolt(sl, end.add(0, 14, 0), end, 0xFFFFFF);
            RelicFx.burst(sl, Fx.spark(COLOR, 0.9F), end, 14, 0.35);
            Fx.sound(sl, end.x, end.y, end.z, SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 1.0F, 1.1F);
        } else {
            Vec3 prev = h;
            float dmg = 6.0F + charge * 8.0F;
            for (int i = 0; i < chain.size(); i++) {
                final LivingEntity target = chain.get(i);
                final Vec3 from = prev;
                final float d = dmg;
                final boolean firstHit = i == 0;
                Scheduler.runLater(1 + i * 2, () -> {
                    if (!target.isAlive()) {
                        return;
                    }
                    Vec3 to = target.getBoundingBox().getCenter();
                    RelicFx.bolt(sl, from, to, COLOR);
                    if (firstHit) {
                        RelicFx.bolt(sl, to.add(0, 14, 0), to, 0xFFFFFF);
                    }
                    RelicFx.burst(sl, Fx.spark(COLOR, 0.8F), to, 12, 0.3);
                    sl.sendParticles(ParticleTypes.ELECTRIC_SPARK, to.x, to.y, to.z, 14, 0.4, 0.5, 0.4, 0.5);
                    Fx.sound(sl, to.x, to.y, to.z, SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 0.35F, 1.4F);
                    RelicSounds.playVaried(sl, to, "voltaic_scepter", "zap", 0.75F);
                    RelicFx.hurt(sl, sp, target, d);
                    target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 2));
                });
                prev = target.getBoundingBox().getCenter();
                dmg *= 0.85F;
            }
        }
        spend(sp, stack, cooldownTicks(), true);
    }
}
