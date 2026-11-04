package com.mundosdeaxelis.relic;

import com.mundosdeaxelis.fx.Fx;
import com.mundosdeaxelis.network.ScreenFxPacket;
import com.mundosdeaxelis.world.Scheduler;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.phys.AABB;

/** Anillo del Fénix: inmune al fuego y, si vas a morir, renaces entre llamas (una vez cada 3 minutos). */
public class PhoenixRingItem extends RelicItem {
    public static final int REVIVE_COOLDOWN = 20 * 60 * 3;

    public PhoenixRingItem() {
        super("phoenix_ring", 0xFF8A2B, 0, Rarity.EPIC);
    }

    @Override
    protected void passive(ServerPlayer sp, ServerLevel sl, long time) {
        sp.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 60, 0, true, false, true));
    }

    /** Llamado desde el evento de muerte. */
    public static void rebirth(ServerPlayer sp) {
        ServerLevel sl = sp.serverLevel();
        sp.setHealth(sp.getMaxHealth());
        sp.removeAllEffects();
        sp.clearFire();
        sp.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 20 * 20, 0));
        sp.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 20 * 8, 1));
        sp.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 20 * 6, 2));
        sp.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 20 * 30, 2));
        sp.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 20 * 10, 1));
        sp.getFoodData().setFoodLevel(Math.max(sp.getFoodData().getFoodLevel(), 12));
        sp.fallDistance = 0.0F;
        com.mundosdeaxelis.fx.Fx.sound(sl, sp.getX(), sp.getY(), sp.getZ(), com.mundosdeaxelis.registry.ModSounds.RELIC_REVIVE.get(), net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 1.0F);
        com.mundosdeaxelis.fx.Fx.sound(sl, sp.getX(), sp.getY(), sp.getZ(), com.mundosdeaxelis.registry.ModSounds.RELIC_FIRE.get(), net.minecraft.sounds.SoundSource.PLAYERS, 0.8F, 0.8F);
        sp.getCooldowns().addCooldown(com.mundosdeaxelis.registry.ModItems.PHOENIX_RING.get(), REVIVE_COOLDOWN);
        double x = sp.getX();
        double y = sp.getY();
        double z = sp.getZ();
        Fx.sound(sl, x, y, z, SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 1.0F, 1.2F);
        Fx.sound(sl, x, y, z, SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 1.0F, 0.6F);
        Fx.screen(sp, ScreenFxPacket.VIGNETTE, 0xFF8A2B, 80);
        Fx.screen(sp, ScreenFxPacket.STARS, 0xFFC36B, 70);
        sp.displayClientMessage(Component.translatable("message.mundos_de_axelis.phoenix_reborn"), true);
        for (int i = 0; i < 22; i++) {
            final int k = i;
            Scheduler.runLater(i, () -> {
                double h = k * 0.14;
                Fx.helix(sl, Fx.dust(0xFF8A2B, 1.3F), x, y + h * 0.3, z, 0.4 + k * 0.08, 1.6, 6, k * 0.5);
                Fx.ring(sl, Fx.spark(0xFFD36B, 1.0F), x, y + 0.2 + h, z, 0.5 + k * 0.18, 14, k * 0.3);
            });
        }
        for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class, new AABB(x - 5, y - 3, z - 5, x + 5, y + 3, z + 5), e -> e != sp && e.isAlive())) {
            e.setSecondsOnFire(5);
            e.push((e.getX() - x) * 0.25, 0.4, (e.getZ() - z) * 0.25);
            e.hurtMarked = true;
        }
    }
}
