package com.mundosdeaxelis.relic;

import com.mundosdeaxelis.fx.Fx;
import com.mundosdeaxelis.registry.ModSounds;
import com.mundosdeaxelis.world.Scheduler;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.phys.Vec3;

/** Nube en Botella: un empujón al cielo, incluso en el aire, y caídas más suaves. */
public class CloudBottleItem extends RelicItem {
    private static final int COLOR = 0xE6F4FF;

    public CloudBottleItem() {
        super("cloud_bottle", COLOR, 20 * 4, Rarity.UNCOMMON);
    }

    @Override
    protected int interval() {
        return 6;
    }

    @Override
    protected void passive(ServerPlayer sp, ServerLevel sl, long time) {
        if (time % 30 == 0 || (!sp.onGround() && sp.getDeltaMovement().y < -0.3)) {
            sl.sendParticles(Fx.wisp(COLOR, 0.6F), sp.getX(), sp.getY() + 0.7, sp.getZ(), 1, 0.3, 0.1, 0.3, 0.0);
        }
    }

    @Override
    protected boolean active(ServerPlayer sp, ServerLevel sl, ItemStack stack) {
        Vec3 dm = sp.getDeltaMovement();
        sp.setDeltaMovement(dm.x * 1.1, 0.95, dm.z * 1.1);
        sp.hurtMarked = true;
        sp.fallDistance = 0.0F;
        sp.getPersistentData().putLong("mdc_nofall", sl.getGameTime() + 20 * 5);
        double x = sp.getX();
        double y = sp.getY();
        double z = sp.getZ();
        Fx.sound(sl, x, y, z, ModSounds.RELIC_WHOOSH.get(), SoundSource.PLAYERS, 0.8F, 1.6F);
        Fx.sound(sl, x, y, z, SoundEvents.WOOL_PLACE, SoundSource.PLAYERS, 1.0F, 0.6F);
        for (int i = 0; i < 28; i++) {
            double a = Math.PI * 2.0 * i / 28;
            sl.sendParticles(ParticleTypes.CLOUD, x, y + 0.1, z, 0, Math.cos(a) * 0.22, 0.03, Math.sin(a) * 0.22, 1.0);
        }
        sl.sendParticles(Fx.wisp(0xFFFFFF, 1.4F), x, y + 0.2, z, 8, 0.5, 0.1, 0.5, 0.0);
        puffs(sp, sl, 0);
        return true;
    }

    private void puffs(ServerPlayer sp, ServerLevel sl, int tick) {
        if (tick > 12 || !sp.isAlive() || sp.isRemoved()) {
            return;
        }
        sl.sendParticles(ParticleTypes.CLOUD, sp.getX(), sp.getY() + 0.1, sp.getZ(), 3, 0.25, 0.05, 0.25, 0.01);
        sl.sendParticles(Fx.wisp(0xFFFFFF, 0.9F), sp.getX(), sp.getY() + 0.2, sp.getZ(), 1, 0.2, 0.05, 0.2, 0.0);
        sl.sendParticles(Fx.spark(0xCFEFFF, 0.6F), sp.getX(), sp.getY(), sp.getZ(), 1, 0.3, 0.1, 0.3, 0.0);
        Scheduler.runLater(1, () -> puffs(sp, sl, tick + 1));
    }
}
