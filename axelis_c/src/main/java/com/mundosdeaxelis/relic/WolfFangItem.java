package com.mundosdeaxelis.relic;

import com.mundosdeaxelis.fx.Fx;
import com.mundosdeaxelis.network.ScreenFxPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.phys.AABB;

/** Colmillo del Lobo: veloz de noche, fuerza al estar herido, y curas al derrotar enemigos; activo: aullido. */
public class WolfFangItem extends RelicItem {
    public WolfFangItem() {
        super("wolf_fang", 0xC8D0E0, 20 * 45, Rarity.RARE);
    }

    @Override
    protected void passive(ServerPlayer sp, ServerLevel sl, long time) {
        if (sl.isNight()) {
            sp.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 60, 0, true, false, true));
        }
        if (sp.getHealth() <= sp.getMaxHealth() * 0.5F) {
            sp.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 60, 0, true, false, true));
        }
    }

    public static void onKill(ServerPlayer sp, LivingEntity victim) {
        sp.heal(2.0F);
        ServerLevel sl = sp.serverLevel();
        sl.sendParticles(Fx.heart(0xFF9CB8, 0.8F), sp.getX(), sp.getY() + 1.6, sp.getZ(), 2, 0.3, 0.2, 0.3, 0.02);
        sl.sendParticles(Fx.spark(0xC8D0E0, 0.9F), victim.getX(), victim.getY() + 0.8, victim.getZ(), 5, 0.3, 0.3, 0.3, 0.0);
    }

    @Override
    protected boolean active(ServerPlayer sp, ServerLevel sl, ItemStack stack) {
        Fx.sound(sl, sp.getX(), sp.getY(), sp.getZ(), SoundEvents.WOLF_HOWL, SoundSource.PLAYERS, 1.6F, 1.0F);
        Fx.screen(sp, ScreenFxPacket.VIGNETTE, 0xC8D0E0, 50);
        Fx.screen(sp, ScreenFxPacket.STARS, 0xE8EEFF, 50);
        for (int i = 1; i <= 8; i++) {
            final int r = i * 2;
            com.mundosdeaxelis.world.Scheduler.runLater(i * 2, () -> Fx.ring(sl, Fx.dust(0xC8D0E0, 1.3F), sp.getX(), sp.getY() + 0.3, sp.getZ(), r, 10 + r * 3, 0));
        }
        sp.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 20 * 12, 1));
        sp.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 20 * 12, 1));
        for (TamableAnimal t : sl.getEntitiesOfClass(TamableAnimal.class, new AABB(sp.blockPosition()).inflate(20), t -> t.isTame() && t.isOwnedBy(sp))) {
            t.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 20 * 12, 1));
            t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 20 * 12, 1));
            sl.sendParticles(Fx.spark(0xC8D0E0, 1.0F), t.getX(), t.getY() + 0.8, t.getZ(), 6, 0.3, 0.3, 0.3, 0.0);
        }
        return true;
    }
}
