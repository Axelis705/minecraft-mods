package com.mundosdeaxelis.relic;

import com.mundosdeaxelis.fx.Fx;
import com.mundosdeaxelis.network.ScreenFxPacket;
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

/** Cetro Meteórico: marca un lugar y hace caer una lluvia de meteoros del cielo. */
public class MeteorScepterItem extends RelicItem {
    private static final int COLOR = 0xFF8A30;
    private static final int METEORS = 9;
    private static final int FALL = 11;

    public MeteorScepterItem() {
        super("meteor_scepter", COLOR, 20 * 35, Rarity.EPIC);
    }

    @Override
    protected void passive(ServerPlayer sp, ServerLevel sl, long time) {
        if (!sl.isDay() && sl.canSeeSky(sp.blockPosition())) {
            sp.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 60, 0, true, false, true));
            if (time % 30 == 0) {
                sl.sendParticles(Fx.ember(COLOR, 0.8F), sp.getX(), sp.getY() + 1.6, sp.getZ(), 2, 0.4, 0.3, 0.4, 0.02);
            }
        }
    }

    @Override
    protected boolean active(ServerPlayer sp, ServerLevel sl, ItemStack stack) {
        Vec3 target = RelicFx.target(sp, 40);
        Fx.sound(sl, sp.getX(), sp.getY(), sp.getZ(), SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 0.5F, 0.5F);
        Fx.screen(sp, ScreenFxPacket.VIGNETTE, COLOR, 24);
        // Señal en el suelo: círculo de runas que gira
        for (int i = 0; i < 18; i++) {
            final int k = i;
            Scheduler.runLater(i, () -> RelicFx.sigil(sl, target, 5.0, COLOR, k * 0.2));
        }
        for (int i = 0; i < METEORS; i++) {
            double a = sl.random.nextDouble() * Math.PI * 2.0;
            double r = Math.sqrt(sl.random.nextDouble()) * 4.5;
            Vec3 impact = new Vec3(target.x + Math.cos(a) * r, target.y, target.z + Math.sin(a) * r);
            Scheduler.runLater(16 + i * 4, () -> meteor(sp, sl, impact, 0));
        }
        return true;
    }

    private void meteor(ServerPlayer sp, ServerLevel sl, Vec3 impact, int step) {
        Vec3 start = impact.add(-7, 26, 5);
        Vec3 p = start.add(impact.subtract(start).scale(step / (double) FALL));
        Vec3 prev = start.add(impact.subtract(start).scale(Math.max(0, step - 1) / (double) FALL));
        if (step == 0) {
            RelicSounds.play(sl, impact.add(0, 8, 0), relicId(), "fall", 0.55F, 0.9F + sl.random.nextFloat() * 0.25F);
        }
        if (step < FALL) {
            sl.sendParticles(Fx.dust(0xFFB060, 2.4F), p.x, p.y, p.z, 2, 0.1, 0.1, 0.1, 0.0);
            sl.sendParticles(Fx.dust(COLOR, 1.6F), p.x, p.y, p.z, 3, 0.25, 0.25, 0.25, 0.0);
            sl.sendParticles(Fx.ember(0xFFD060, 1.2F), p.x, p.y, p.z, 4, 0.3, 0.3, 0.3, 0.03);
            sl.sendParticles(ParticleTypes.FLAME, p.x, p.y, p.z, 3, 0.2, 0.2, 0.2, 0.02);
            sl.sendParticles(Fx.wisp(0x30180A, 1.1F), p.x, p.y, p.z, 2, 0.2, 0.2, 0.2, 0.0);
            RelicFx.line(sl, Fx.dust(COLOR, 1.0F), prev, p, 0.5);
            Scheduler.runLater(1, () -> meteor(sp, sl, impact, step + 1));
            return;
        }
        // Impacto
        Fx.sound(sl, impact.x, impact.y, impact.z, SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 0.6F, 0.7F + sl.random.nextFloat() * 0.4F);
        RelicSounds.play(sl, impact, "meteor_scepter", "impact", 0.8F, 0.85F + sl.random.nextFloat() * 0.3F);
        RelicFx.shakeNear(sl, impact, 16, 1.4F, 10);
        sl.sendParticles(ParticleTypes.EXPLOSION, impact.x, impact.y + 0.4, impact.z, 1, 0, 0, 0, 0);
        sl.sendParticles(ParticleTypes.LAVA, impact.x, impact.y + 0.3, impact.z, 8, 0.4, 0.1, 0.4, 0.0);
        sl.sendParticles(RelicFx.blockDust(RelicFx.groundState(sl, impact.x, impact.y, impact.z)), impact.x, impact.y + 0.3, impact.z, 24, 0.4, 0.2, 0.4, 0.2);
        for (int i = 0; i < 14; i++) {
            sl.sendParticles(Fx.ember(COLOR, 1.2F), impact.x, impact.y + 0.2, impact.z, 0, (sl.random.nextDouble() - 0.5) * 0.4, 0.2 + sl.random.nextDouble() * 0.4, (sl.random.nextDouble() - 0.5) * 0.4, 1.0);
        }
        for (int r = 1; r <= 3; r++) {
            final int rr = r;
            Scheduler.runLater(r, () -> Fx.ring(sl, Fx.dust(COLOR, 1.2F), impact.x, impact.y + 0.15, impact.z, rr, 12 + rr * 4, rr));
        }
        for (LivingEntity e : RelicFx.foes(sl, impact, 3.5, sp)) {
            RelicFx.hurt(sl, sp, e, 6.0F);
            e.setSecondsOnFire(4);
            e.invulnerableTime = 0;
        }
    }
}
