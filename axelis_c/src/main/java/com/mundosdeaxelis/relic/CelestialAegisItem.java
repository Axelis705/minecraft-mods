package com.mundosdeaxelis.relic;

import com.mundosdeaxelis.fx.Fx;
import com.mundosdeaxelis.registry.ModSounds;
import com.mundosdeaxelis.world.Scheduler;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.phys.Vec3;

/** Égida Celestial: una cúpula de luz que absorbe los cinco próximos golpes. */
public class CelestialAegisItem extends RelicItem {
    private static final int COLOR = 0xFFE9A8;
    private static final int DURATION = 20 * 9;
    private static final int HITS = 5;
    private static final double RADIUS = 2.5;

    public CelestialAegisItem() {
        super("celestial_aegis", COLOR, 20 * 35, Rarity.EPIC);
    }

    public static boolean isActive(ServerPlayer sp) {
        CompoundTag d = sp.getPersistentData();
        return d.getLong("mdc_aegis") > sp.level().getGameTime() && d.getInt("mdc_aegis_hits") > 0;
    }

    /** Llamado cuando la cúpula absorbe un golpe. */
    public static void absorb(ServerPlayer sp) {
        CompoundTag d = sp.getPersistentData();
        int left = d.getInt("mdc_aegis_hits") - 1;
        d.putInt("mdc_aegis_hits", left);
        ServerLevel sl = sp.serverLevel();
        Vec3 c = sp.position().add(0, 1.0, 0);
        Fx.sound(sl, c.x, c.y, c.z, SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 0.5F, 1.3F);
        RelicSounds.play(sl, c, "celestial_aegis", "absorb", 1.0F, 0.85F + Math.max(0, left) * 0.07F);
        RelicFx.sphere(sl, Fx.spark(0xFFFFFF, 1.0F), c, RADIUS, 40, sl.random.nextDouble() * 6);
        RelicFx.burst(sl, Fx.spark(COLOR, 0.9F), c.add(sp.getLookAngle().scale(-0.5)), 10, 0.3);
        RelicFx.shake(sp, 1.0F, 8);
        if (left <= 0) {
            d.putLong("mdc_aegis", 0);
            Fx.sound(sl, c.x, c.y, c.z, SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 1.2F, 0.7F);
            RelicFx.burst(sl, Fx.shard(0xFFF2C0, 1.2F), c, 40, 0.5);
            RelicFx.burst(sl, Fx.spark(COLOR, 1.0F), c, 24, 0.4);
        }
    }

    @Override
    protected int interval() {
        return 20;
    }

    @Override
    protected void passive(ServerPlayer sp, ServerLevel sl, long time) {
        if (time % 200 == 0 && sp.getAbsorptionAmount() < 2.0F) {
            sp.setAbsorptionAmount(2.0F);
            Fx.ring(sl, Fx.spark(COLOR, 0.7F), sp.getX(), sp.getY() + 1.0, sp.getZ(), 0.9, 10, 0);
        }
    }

    @Override
    protected boolean active(ServerPlayer sp, ServerLevel sl, ItemStack stack) {
        CompoundTag d = sp.getPersistentData();
        d.putLong("mdc_aegis", sl.getGameTime() + DURATION);
        d.putInt("mdc_aegis_hits", HITS);
        Fx.sound(sl, sp.getX(), sp.getY(), sp.getZ(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.0F, 1.4F);
        Fx.sound(sl, sp.getX(), sp.getY(), sp.getZ(), ModSounds.RELIC_MAGIC.get(), SoundSource.PLAYERS, 0.8F, 1.2F);
        for (int i = 0; i < 6; i++) {
            final int k = i;
            Scheduler.runLater(i, () -> Fx.ring(sl, Fx.dust(COLOR, 1.1F), sp.getX(), sp.getY() + 0.1 + k * 0.45, sp.getZ(), RADIUS * Math.sin((k + 0.5) / 6.0 * Math.PI * 0.5 + 0.6), 22, k));
        }
        dome(sp, sl, 0);
        return true;
    }

    private void dome(ServerPlayer sp, ServerLevel sl, int tick) {
        if (!sp.isAlive() || sp.isRemoved()) {
            return;
        }
        if (!isActive(sp)) {
            if (sp.getPersistentData().getInt("mdc_aegis_hits") > 0 && sp.getPersistentData().getLong("mdc_aegis") > 0) {
                sp.getPersistentData().putLong("mdc_aegis", 0);
                Vec3 e = sp.position().add(0, 1.0, 0);
                Fx.sound(sl, e.x, e.y, e.z, SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 0.5F, 1.5F);
                RelicFx.sphere(sl, Fx.spark(COLOR, 0.7F), e, RADIUS, 40, 0);
                RelicFx.burst(sl, Fx.feather(0xFFFFFF, 1.0F), e, 12, 0.2);
            }
            return;
        }
        Vec3 c = sp.position().add(0, 0.9, 0);
        // Cúpula hexagonal: tres anillos inclinados que giran + malla de puntos
        for (int i = 0; i < 3; i++) {
            RelicFx.ringFacing(sl, Fx.dust(i == 1 ? 0xFFFFFF : COLOR, 0.55F), c,
                    new Vec3(Math.cos(tick * 0.05 + i * 1.05), 0.6, Math.sin(tick * 0.05 + i * 1.05)), RADIUS, 22, tick * 0.1);
        }
        RelicFx.sphere(sl, Fx.spark(COLOR, 0.45F), c, RADIUS, 14, tick * 0.07);
        // Ola de brillo que sube por la cúpula
        double wave = ((tick * 0.04) % 1.0) * 2.0 - 1.0;
        double rr = Math.sqrt(Math.max(0, 1 - wave * wave)) * RADIUS;
        Fx.ring(sl, Fx.spark(0xFFFFFF, 0.6F), c.x, c.y + wave * RADIUS, c.z, rr, 14, tick);
        for (LivingEntity e : RelicFx.foes(sl, c, RADIUS + 0.6, sp)) {
            Vec3 away = e.position().subtract(sp.position());
            Vec3 n = away.lengthSqr() < 0.01 ? new Vec3(1, 0, 0) : away.normalize();
            e.push(n.x * 0.25, 0.05, n.z * 0.25);
            e.hurtMarked = true;
        }
        if (tick % 40 == 0) {
            Fx.sound(sl, c.x, c.y, c.z, SoundEvents.BEACON_AMBIENT, SoundSource.PLAYERS, 0.5F, 1.6F);
        }
        Scheduler.runLater(4, () -> dome(sp, sl, tick + 4));
    }
}
