package com.mundosdeaxelis.relic;

import com.mundosdeaxelis.fx.Fx;
import com.mundosdeaxelis.registry.ModSounds;
import com.mundosdeaxelis.world.Scheduler;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
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

/** Guante de Furia: cada golpe que das acumula furia; con ella puedes soltar un puñetazo contra el suelo. */
public class RageGauntletItem extends RelicItem {
    private static final int COLOR = 0xFF4A2A;
    private static final int MAX = 10;

    public RageGauntletItem() {
        super("rage_gauntlet", COLOR, 20 * 20, Rarity.EPIC);
    }

    public static int rage(ServerPlayer sp) {
        return sp.getPersistentData().getInt("mdc_rage");
    }

    /** Llamado desde RelicEvents cuando el portador golpea a algo. */
    public static void onHit(ServerPlayer sp, LivingEntity victim) {
        CompoundTag d = sp.getPersistentData();
        long now = sp.level().getGameTime();
        if (now == d.getLong("mdc_rage_hit")) {
            return;
        }
        d.putLong("mdc_rage_hit", now);
        int r = Math.min(MAX, d.getInt("mdc_rage") + 1);
        d.putInt("mdc_rage", r);
        ServerLevel sl = sp.serverLevel();
        Vec3 c = victim.getBoundingBox().getCenter();
        sl.sendParticles(Fx.ember(COLOR, 1.0F), c.x, c.y, c.z, 6, 0.3, 0.3, 0.3, 0.05);
        sl.sendParticles(Fx.slash(0xFF8A5A, 1.0F), c.x, c.y, c.z, 1, 0, 0, 0, 0);
        RelicSounds.play(sl, c, "rage_gauntlet", "hit", 0.75F, 0.85F + r * 0.06F);
        sp.displayClientMessage(Component.translatable("msg.mundos_de_axelis.rage", r, MAX), true);
        if (r == MAX) {
            Fx.sound(sl, sp.getX(), sp.getY(), sp.getZ(), SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 0.8F, 0.6F);
            RelicFx.burst(sl, Fx.ember(COLOR, 1.2F), sp.position().add(0, 1, 0), 24, 0.3);
        }
    }

    @Override
    protected int interval() {
        return 5;
    }

    @Override
    protected void passive(ServerPlayer sp, ServerLevel sl, long time) {
        CompoundTag d = sp.getPersistentData();
        int r = d.getInt("mdc_rage");
        if (r <= 0) {
            return;
        }
        if (time - d.getLong("mdc_rage_hit") > 100 && time % 40 == 0) {
            d.putInt("mdc_rage", --r);
            sp.displayClientMessage(Component.translatable("msg.mundos_de_axelis.rage", r, MAX), true);
        }
        if (r >= 5) {
            sp.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 30, r >= 8 ? 1 : 0, true, false, true));
        }
        if (r >= 8) {
            sp.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 30, 0, true, false, true));
        }
        Vec3 h = RelicFx.hand(sp);
        sl.sendParticles(Fx.ember(COLOR, 0.7F), h.x, h.y, h.z, 1 + r / 3, 0.15, 0.15, 0.15, 0.02);
        if (r >= 6) {
            sl.sendParticles(ParticleTypes.FLAME, sp.getX(), sp.getY() + 1.0, sp.getZ(), 1, 0.3, 0.6, 0.3, 0.01);
        }
    }

    @Override
    protected boolean active(ServerPlayer sp, ServerLevel sl, ItemStack stack) {
        int r = rage(sp);
        if (r < 3) {
            sp.displayClientMessage(Component.translatable("msg.mundos_de_axelis.rage_low"), true);
            return false;
        }
        sp.getPersistentData().putInt("mdc_rage", 0);
        double x = sp.getX();
        double y = sp.getY();
        double z = sp.getZ();
        double radius = 3.0 + r * 0.55;
        float dmg = 4.0F + r * 1.6F;
        Fx.sound(sl, x, y, z, ModSounds.RELIC_BOOM.get(), SoundSource.PLAYERS, 1.5F, 0.6F);
        Fx.sound(sl, x, y, z, SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 1.0F, 0.8F);
        RelicFx.shake(sp, 1.5F + r * 0.3F, 20);
        RelicFx.shakeNear(sl, sp.position(), 14, 1.5F + r * 0.2F, 16);
        sp.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 20 * 10, 1));
        // Cráter: pilar de fuego, escombros y ondas
        sl.sendParticles(RelicFx.blockDust(RelicFx.groundState(sl, x, y, z)), x, y + 0.2, z, 60, 0.6, 0.2, 0.6, 0.3);
        for (int i = 0; i < 20; i++) {
            sl.sendParticles(Fx.ember(COLOR, 1.2F), x, y + 0.2, z, 0, (sl.random.nextDouble() - 0.5) * 0.3, 0.3 + sl.random.nextDouble() * 0.5, (sl.random.nextDouble() - 0.5) * 0.3, 1.0);
        }
        sl.sendParticles(ParticleTypes.LAVA, x, y + 0.3, z, 14, 0.5, 0.1, 0.5, 0.0);
        sl.sendParticles(ParticleTypes.EXPLOSION, x, y + 0.3, z, 1, 0, 0, 0, 0);
        final int steps = (int) Math.ceil(radius);
        for (int i = 1; i <= steps; i++) {
            final int rr = i;
            Scheduler.runLater(i, () -> {
                Fx.ring(sl, Fx.dust(COLOR, 1.3F), x, y + 0.2, z, rr, 14 + rr * 5, rr * 0.2);
                Fx.ring(sl, Fx.ember(0xFFB040, 1.0F), x, y + 0.3, z, rr, 6 + rr * 2, -rr * 0.2);
                for (int k = 0; k < 6; k++) {
                    double a = sl.random.nextDouble() * Math.PI * 2.0;
                    sl.sendParticles(RelicFx.blockDust(RelicFx.groundState(sl, x + Math.cos(a) * rr, y, z + Math.sin(a) * rr)),
                            x + Math.cos(a) * rr, y + 0.3, z + Math.sin(a) * rr, 4, 0.2, 0.2, 0.2, 0.15);
                }
            });
        }
        for (LivingEntity e : RelicFx.foes(sl, sp.position(), radius, sp)) {
            RelicFx.hurt(sl, sp, e, dmg);
            e.setSecondsOnFire(4);
            double dx = e.getX() - x;
            double dz = e.getZ() - z;
            double len = Math.max(0.5, Math.sqrt(dx * dx + dz * dz));
            e.push(dx / len * 0.9, 0.7, dz / len * 0.9);
            e.hurtMarked = true;
        }
        return true;
    }
}
