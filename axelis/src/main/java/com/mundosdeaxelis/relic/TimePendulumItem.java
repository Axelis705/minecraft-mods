package com.mundosdeaxelis.relic;

import com.mundosdeaxelis.fx.Fx;
import com.mundosdeaxelis.network.ScreenFxPacket;
import com.mundosdeaxelis.registry.ModSounds;
import com.mundosdeaxelis.world.Scheduler;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.Set;

/** Péndulo del Tiempo: durante cinco segundos todo a tu alrededor queda congelado en el instante. */
public class TimePendulumItem extends RelicItem {
    private static final int COLOR = 0x6FE0FF;
    private static final int DURATION = 100;
    private static final double RADIUS = 12.0;

    public TimePendulumItem() {
        super("time_pendulum", COLOR, 20 * 55, Rarity.EPIC);
    }

    @Override
    protected void passive(ServerPlayer sp, ServerLevel sl, long time) {
        sp.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 40, 0, true, false, true));
        if (time % 40 == 0) {
            Vec3 h = RelicFx.hand(sp);
            sl.sendParticles(Fx.spark(COLOR, 0.5F), h.x, h.y, h.z, 1, 0.1, 0.1, 0.1, 0.0);
        }
    }

    @Override
    protected boolean active(ServerPlayer sp, ServerLevel sl, ItemStack stack) {
        Vec3 c = sp.position();
        Fx.sound(sl, c.x, c.y, c.z, SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 0.6F, 0.6F);
        Fx.screen(sp, ScreenFxPacket.VIGNETTE, COLOR, DURATION);
        Fx.screen(sp, ScreenFxPacket.DARK, 0x305878, DURATION);
        RelicFx.shake(sp, 1.5F, 14);
        for (int r = 1; r <= 8; r++) {
            final int rr = r;
            Scheduler.runLater(r, () -> Fx.ring(sl, Fx.dust(COLOR, 1.2F), c.x, c.y + 0.2, c.z, rr * 1.5, 14 + rr * 6, rr));
        }
        tick(sp, sl, c, 0, new HashSet<>());
        return true;
    }

    private void tick(ServerPlayer sp, ServerLevel sl, Vec3 c, int t, Set<Entity> frozen) {
        if (!sp.isAlive() || sp.isRemoved() || t >= DURATION) {
            end(sl, c, frozen);
            return;
        }
        Vec3 o = sp.position();
        AABB box = new AABB(o.x - RADIUS, o.y - RADIUS, o.z - RADIUS, o.x + RADIUS, o.y + RADIUS, o.z + RADIUS);
        for (Entity e : sl.getEntitiesOfClass(Entity.class, box, e -> e != sp && e.isAlive() && e.position().distanceTo(o) <= RADIUS)) {
            if (e instanceof Projectile) {
                e.setDeltaMovement(Vec3.ZERO);
                e.setNoGravity(true);
                e.hurtMarked = true;
                frozen.add(e);
            } else if (e instanceof LivingEntity le && RelicFx.isFoe(le, sp)) {
                le.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 8, 9, false, false));
                le.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 8, 3, false, false));
                le.setDeltaMovement(0, Math.min(0, le.getDeltaMovement().y), 0);
                le.hurtMarked = true;
                if (t % 10 == 0) {
                    sl.sendParticles(Fx.spark(COLOR, 0.7F), le.getX(), le.getY() + le.getBbHeight() + 0.4, le.getZ(), 2, 0.2, 0.1, 0.2, 0.0);
                    sl.sendParticles(ParticleTypes_ENCHANT(), le.getX(), le.getY() + 1.0, le.getZ(), 6, 0.4, 0.6, 0.4, 0.3);
                }
            }
        }
        // Reloj gigante en el suelo
        double R = 4.5;
        if (t % 2 == 0) {
            for (int h = 0; h < 12; h++) {
                double a = Math.PI * 2.0 * h / 12;
                sl.sendParticles(Fx.spark(h % 3 == 0 ? 0xFFFFFF : COLOR, h % 3 == 0 ? 0.8F : 0.5F), o.x + Math.cos(a) * R, o.y + 0.12, o.z + Math.sin(a) * R, 1, 0, 0, 0, 0);
            }
            Fx.ring(sl, Fx.dust(COLOR, 0.55F), o.x, o.y + 0.1, o.z, R, 30, 0);
            Fx.ring(sl, Fx.dust(COLOR, 0.4F), o.x, o.y + 0.1, o.z, R * 0.92, 24, 0);
            double hourAng = -t * 0.05;
            double minAng = -t * 0.31;
            RelicFx.line(sl, Fx.dust(0xFFFFFF, 0.7F), new Vec3(o.x, o.y + 0.13, o.z), new Vec3(o.x + Math.cos(minAng) * R * 0.9, o.y + 0.13, o.z + Math.sin(minAng) * R * 0.9), 0.45);
            RelicFx.line(sl, Fx.dust(COLOR, 0.8F), new Vec3(o.x, o.y + 0.13, o.z), new Vec3(o.x + Math.cos(hourAng) * R * 0.55, o.y + 0.13, o.z + Math.sin(hourAng) * R * 0.55), 0.4);
            sl.sendParticles(Fx.rune(COLOR, 1.0F), o.x + Math.cos(minAng + 0.4) * R * 0.75, o.y + 0.2, o.z + Math.sin(minAng + 0.4) * R * 0.75, 1, 0, 0, 0, 0);
        }
        // Engranajes verticales que giran a su alrededor
        RelicFx.ringFacing(sl, Fx.dust(0xB8F0FF, 0.5F), o.add(0, 1.0, 0), new Vec3(Math.cos(t * 0.1), 0.4, Math.sin(t * 0.1)), 2.3, 22, t * 0.2);
        // Burbuja temporal
        if (t % 5 == 0) {
            RelicFx.sphere(sl, Fx.spark(COLOR, 0.5F), o.add(0, 1, 0), RADIUS, 22, t * 0.05);
        }
        if (t % 10 == 0) {
            Fx.sound(sl, o.x, o.y, o.z, SoundEvents.NOTE_BLOCK_HAT.value(), SoundSource.PLAYERS, 0.7F, (t / 10) % 2 == 0 ? 1.4F : 1.0F);
            Fx.sound(sl, o.x, o.y, o.z, SoundEvents.NOTE_BLOCK_BASEDRUM.value(), SoundSource.PLAYERS, 0.25F, 1.6F);
        }
        Scheduler.runLater(1, () -> tick(sp, sl, c, t + 1, frozen));
    }

    private static net.minecraft.core.particles.ParticleOptions ParticleTypes_ENCHANT() {
        return net.minecraft.core.particles.ParticleTypes.ENCHANT;
    }

    private void end(ServerLevel sl, Vec3 c, Set<Entity> frozen) {
        for (Entity e : frozen) {
            e.setNoGravity(false);
        }
        RelicSounds.play(sl, c, "time_pendulum", "end", 1.2F, 1.0F);
        Fx.sound(sl, c.x, c.y, c.z, ModSounds.RELIC_BOOM.get(), SoundSource.PLAYERS, 0.5F, 1.4F);
        for (int r = 1; r <= 6; r++) {
            final int rr = r;
            Scheduler.runLater(r, () -> Fx.ring(sl, Fx.spark(0xFFFFFF, 0.9F), c.x, c.y + 0.3, c.z, rr * 2.0, 14 + rr * 6, rr));
        }
    }
}
