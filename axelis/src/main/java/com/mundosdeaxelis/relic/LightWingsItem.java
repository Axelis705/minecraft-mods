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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.phys.Vec3;

/** Alas de Luz: unas alas doradas te impulsan en un vuelo corto; además amortiguan tus caídas. */
public class LightWingsItem extends RelicItem {
    private static final int COLOR = 0xFFF1B0;
    private static final int FLIGHT = 44;

    public LightWingsItem() {
        super("light_wings", COLOR, 20 * 14, Rarity.EPIC);
    }

    @Override
    protected int interval() {
        return 4;
    }

    @Override
    protected void passive(ServerPlayer sp, ServerLevel sl, long time) {
        if (!sp.onGround() && sp.getDeltaMovement().y < -0.5 && !sp.isCrouching()) {
            sp.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 30, 0, true, false, true));
            sp.getPersistentData().putLong("mdc_nofall", sl.getGameTime() + 40);
            wings(sl, sp, (int) time, 0.5F);
        }
    }

    @Override
    protected boolean active(ServerPlayer sp, ServerLevel sl, ItemStack stack) {
        Fx.sound(sl, sp.getX(), sp.getY(), sp.getZ(), ModSounds.RELIC_WHOOSH.get(), SoundSource.PLAYERS, 0.6F, 1.4F);
        RelicSounds.play(sl, sp, relicId(), "flap", 1.0F, 0.9F);
        Fx.screen(sp, ScreenFxPacket.SPEED, 0xFFE9A0, FLIGHT);
        Vec3 look = sp.getLookAngle();
        sp.setDeltaMovement(look.x * 0.7, 0.9, look.z * 0.7);
        sp.hurtMarked = true;
        RelicFx.burst(sl, Fx.spark(COLOR, 1.0F), sp.position().add(0, 1, 0), 20, 0.3);
        for (int i = 0; i < 12; i++) {
            sl.sendParticles(Fx.feather(0xFFFFFF, 1.2F), sp.getX(), sp.getY() + 1.2, sp.getZ(), 0, (sl.random.nextDouble() - 0.5) * 0.5, 0.15 + sl.random.nextDouble() * 0.2, (sl.random.nextDouble() - 0.5) * 0.5, 1.0);
        }
        fly(sp, sl, 0);
        return true;
    }

    private void fly(ServerPlayer sp, ServerLevel sl, int tick) {
        if (!sp.isAlive() || sp.isRemoved()) {
            return;
        }
        sp.getPersistentData().putLong("mdc_nofall", sl.getGameTime() + 100);
        if (tick >= FLIGHT || (tick > 6 && sp.onGround())) {
            Fx.sound(sl, sp.getX(), sp.getY(), sp.getZ(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.8F, 1.6F);
            RelicFx.burst(sl, Fx.spark(COLOR, 0.9F), sp.position().add(0, 1.2, 0), 18, 0.25);
            for (int i = 0; i < 10; i++) {
                sl.sendParticles(Fx.feather(0xFFFFFF, 1.2F), sp.getX(), sp.getY() + 1.2, sp.getZ(), 0, (sl.random.nextDouble() - 0.5) * 0.3, 0.05 + sl.random.nextDouble() * 0.1, (sl.random.nextDouble() - 0.5) * 0.3, 1.0);
            }
            return;
        }
        Vec3 look = sp.getLookAngle();
        Vec3 dm = sp.getDeltaMovement();
        double lift = tick < 10 ? 0.12 : 0.05;
        Vec3 want = new Vec3(look.x * 1.0, look.y * 0.85 + lift, look.z * 1.0);
        sp.setDeltaMovement(dm.scale(0.35).add(want.scale(0.65)));
        sp.hurtMarked = true;
        sp.fallDistance = 0.0F;
        wings(sl, sp, tick, 1.0F);
        if (tick % 14 == 7) {
            RelicSounds.play(sl, sp, relicId(), "flap", 0.7F, 0.95F + sl.random.nextFloat() * 0.2F);
        }
        Scheduler.runLater(1, () -> fly(sp, sl, tick + 1));
    }

    /** Dibuja dos alas de luz a la espalda del jugador. */
    static void wings(ServerLevel sl, ServerPlayer sp, int tick, float density) {
        Vec3 look = sp.getLookAngle();
        Vec3 fwd = new Vec3(look.x, 0, look.z);
        fwd = fwd.lengthSqr() < 0.01 ? new Vec3(0, 0, 1) : fwd.normalize();
        Vec3 right = fwd.cross(new Vec3(0, 1, 0)).normalize();
        Vec3 base = sp.position().add(0, 1.25, 0).subtract(fwd.scale(0.25));
        double flap = Math.sin(tick * 0.55);
        int pts = Math.max(5, (int) (11 * density));
        for (int side = -1; side <= 1; side += 2) {
            for (int i = 0; i <= pts; i++) {
                double s = (double) i / pts;
                double span = 0.2 + 2.1 * s;
                double up = 0.9 * Math.sin(s * Math.PI * 0.85) + flap * 0.5 * s;
                double back = 0.15 + 0.8 * s * s;
                Vec3 p = base.add(right.scale(side * span)).add(0, up, 0).subtract(fwd.scale(back));
                sl.sendParticles(Fx.spark(i % 3 == 0 ? 0xFFFFFF : COLOR, 0.55F), p.x, p.y, p.z, 1, 0, 0, 0, 0);
                // plumas colgando
                double len = 0.35 + 0.75 * Math.sin(s * Math.PI * 0.9);
                for (int f = 1; f <= 3; f++) {
                    Vec3 q = p.add(0, -len * f / 3.0, 0).subtract(fwd.scale(0.1 * f)).add(right.scale(side * 0.05 * f));
                    sl.sendParticles(Fx.dust(f == 3 ? COLOR : 0xFFFFFF, 0.5F), q.x, q.y, q.z, 1, 0, 0, 0, 0);
                }
            }
        }
        sl.sendParticles(Fx.dust(COLOR, 1.2F), base.x, base.y, base.z, 1, 0.1, 0.1, 0.1, 0.0);
        if (tick % 3 == 0) {
            sl.sendParticles(Fx.feather(0xFFFFFF, 1.0F), base.x - fwd.x, base.y, base.z - fwd.z, 1, 0.5, 0.3, 0.5, 0.0);
        }
    }
}
