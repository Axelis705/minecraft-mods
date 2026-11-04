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
import net.minecraft.world.phys.Vec3;

/** Botas de Magma: inmune al fuego, caminas sobre la lava y puedes dar un salto volcánico. */
public class MagmaBootsItem extends RelicItem {
    private static final int COLOR = 0xFF7A1A;

    public MagmaBootsItem() {
        super("magma_boots", COLOR, 20 * 18, Rarity.RARE);
    }

    @Override
    protected int interval() {
        return 4;
    }

    @Override
    protected void passive(ServerPlayer sp, ServerLevel sl, long time) {
        sp.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 80, 0, true, false, true));
        if (sp.isInLava() && !sp.isCrouching()) {
            Vec3 dm = sp.getDeltaMovement();
            sp.setDeltaMovement(dm.x * 1.1, 0.14, dm.z * 1.1);
            sp.hurtMarked = true;
            sp.fallDistance = 0.0F;
        }
        boolean moving = sp.getDeltaMovement().horizontalDistanceSqr() > 0.004;
        if ((sp.onGround() || sp.isInLava()) && moving) {
            sl.sendParticles(Fx.ember(COLOR, 0.9F), sp.getX(), sp.getY() + 0.1, sp.getZ(), 2, 0.2, 0.0, 0.2, 0.02);
            sl.sendParticles(ParticleTypes.FLAME, sp.getX(), sp.getY() + 0.05, sp.getZ(), 1, 0.15, 0.0, 0.15, 0.0);
            if (time % 12 == 0) {
                sl.sendParticles(ParticleTypes.LAVA, sp.getX(), sp.getY() + 0.1, sp.getZ(), 1, 0.2, 0.0, 0.2, 0.0);
            }
        }
    }

    @Override
    protected boolean active(ServerPlayer sp, ServerLevel sl, ItemStack stack) {
        Vec3 look = sp.getLookAngle();
        sp.setDeltaMovement(look.x * 0.9, 1.1, look.z * 0.9);
        sp.hurtMarked = true;
        sp.getPersistentData().putLong("mdc_nofall", sl.getGameTime() + 20 * 8);
        double x = sp.getX();
        double y = sp.getY();
        double z = sp.getZ();
        Fx.sound(sl, x, y, z, SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 1.0F, 0.6F);
        Fx.sound(sl, x, y, z, ModSounds.RELIC_FIRE.get(), SoundSource.PLAYERS, 0.8F, 1.0F);
        Fx.screen(sp, ScreenFxPacket.SPEED, 0xFF8A2A, 14);
        for (int i = 0; i < 16; i++) {
            sl.sendParticles(Fx.ember(COLOR, 1.2F), x, y + 0.1, z, 0, (sl.random.nextDouble() - 0.5) * 0.4, 0.15 + sl.random.nextDouble() * 0.3, (sl.random.nextDouble() - 0.5) * 0.4, 1.0);
        }
        sl.sendParticles(ParticleTypes.LAVA, x, y + 0.2, z, 10, 0.4, 0.1, 0.4, 0.0);
        Fx.ring(sl, Fx.dust(COLOR, 1.2F), x, y + 0.1, z, 1.2, 18, 0);
        flight(sp, sl, 0);
        return true;
    }

    private void flight(ServerPlayer sp, ServerLevel sl, int tick) {
        if (!sp.isAlive() || sp.isRemoved()) {
            return;
        }
        double x = sp.getX();
        double y = sp.getY();
        double z = sp.getZ();
        if (tick > 5 && (sp.onGround() || sp.isInWater() || tick > 90)) {
            slam(sp, sl, x, y, z);
            return;
        }
        sl.sendParticles(ParticleTypes.FLAME, x, y + 0.1, z, 4, 0.15, 0.05, 0.15, 0.02);
        sl.sendParticles(Fx.ember(COLOR, 1.0F), x, y + 0.2, z, 3, 0.2, 0.1, 0.2, 0.02);
        sl.sendParticles(Fx.wisp(0x40200A, 0.8F), x, y + 0.3, z, 1, 0.1, 0.1, 0.1, 0.0);
        if (tick % 3 == 0) {
            Fx.ring(sl, Fx.dust(COLOR, 0.8F), x, y + 0.1, z, 0.5, 8, tick);
        }
        sp.fallDistance = 0.0F;
        Scheduler.runLater(1, () -> flight(sp, sl, tick + 1));
    }

    private void slam(ServerPlayer sp, ServerLevel sl, double x, double y, double z) {
        Fx.sound(sl, x, y, z, ModSounds.RELIC_BOOM.get(), SoundSource.PLAYERS, 0.8F, 0.8F);
        RelicSounds.play(sl, x, y, z, "magma_boots", "land", 1.0F, 1.0F);
        RelicFx.shakeNear(sl, new Vec3(x, y, z), 14, 2.2F, 16);
        sl.sendParticles(ParticleTypes.EXPLOSION, x, y + 0.4, z, 1, 0, 0, 0, 0);
        sl.sendParticles(ParticleTypes.LAVA, x, y + 0.3, z, 16, 0.6, 0.1, 0.6, 0.0);
        sl.sendParticles(RelicFx.blockDust(RelicFx.groundState(sl, x, y, z)), x, y + 0.3, z, 40, 0.6, 0.2, 0.6, 0.25);
        for (int i = 0; i < 24; i++) {
            sl.sendParticles(Fx.ember(COLOR, 1.3F), x, y + 0.2, z, 0, (sl.random.nextDouble() - 0.5) * 0.5, 0.2 + sl.random.nextDouble() * 0.4, (sl.random.nextDouble() - 0.5) * 0.5, 1.0);
        }
        for (int r = 1; r <= 5; r++) {
            final int rr = r;
            Scheduler.runLater(r, () -> {
                Fx.ring(sl, Fx.dust(COLOR, 1.3F), x, y + 0.15, z, rr, 14 + rr * 5, rr * 0.3);
                Fx.ring(sl, Fx.ember(0xFFD060, 1.0F), x, y + 0.3, z, rr, 6 + rr * 2, -rr);
                Fx.ring(sl, ParticleTypes.FLAME, x, y + 0.1, z, rr, 10 + rr * 3, 0);
            });
        }
        for (LivingEntity e : RelicFx.foes(sl, new Vec3(x, y, z), 5, sp)) {
            RelicFx.hurt(sl, sp, e, 8.0F);
            e.setSecondsOnFire(5);
            double dx = e.getX() - x;
            double dz = e.getZ() - z;
            double len = Math.max(0.5, Math.sqrt(dx * dx + dz * dz));
            e.push(dx / len * 0.6, 0.7, dz / len * 0.6);
            e.hurtMarked = true;
        }
    }
}
