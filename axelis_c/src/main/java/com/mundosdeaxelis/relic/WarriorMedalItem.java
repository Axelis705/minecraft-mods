package com.mundosdeaxelis.relic;

import com.mundosdeaxelis.fx.Fx;
import com.mundosdeaxelis.network.ScreenFxPacket;
import com.mundosdeaxelis.world.Scheduler;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Medalla del Guerrero: al recibir golpes te enfureces (fuerza y velocidad); activo: grito de guerra. */
public class WarriorMedalItem extends RelicItem {
    public WarriorMedalItem() {
        super("warrior_medal", 0xFF6A4A, 20 * 40, Rarity.RARE);
    }

    @Override
    protected void passive(ServerPlayer sp, ServerLevel sl, long time) {
    }

    public static void onHurt(ServerPlayer sp) {
        sp.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 20 * 6, 0));
        sp.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 20 * 6, 0));
        sp.serverLevel().sendParticles(Fx.spark(0xFF6A4A, 1.0F), sp.getX(), sp.getY() + 1.2, sp.getZ(), 4, 0.4, 0.5, 0.4, 0.0);
    }

    @Override
    protected boolean active(ServerPlayer sp, ServerLevel sl, ItemStack stack) {
        double x = sp.getX();
        double y = sp.getY();
        double z = sp.getZ();
        Fx.sound(sl, x, y, z, SoundEvents.RAVAGER_ROAR, SoundSource.PLAYERS, 1.5F, 0.9F);
        Fx.sound(sl, x, y, z, SoundEvents.GOAT_HORN_SOUND_VARIANTS.get(4).get(), SoundSource.PLAYERS, 1.5F, 1.0F);
        Fx.screen(sp, ScreenFxPacket.VIGNETTE, 0xFF6A4A, 60);
        for (int i = 1; i <= 6; i++) {
            final int r = i * 2;
            Scheduler.runLater(i * 2, () -> Fx.ring(sl, Fx.dust(0xFF6A4A, 1.5F), x, y + 0.4, z, r, 14 + r * 3, 0));
        }
        for (ServerPlayer p : sl.getEntitiesOfClass(ServerPlayer.class, new AABB(x - 14, y - 6, z - 14, x + 14, y + 6, z + 14))) {
            p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 20 * 15, 1));
            p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 20 * 15, 0));
        }
        return true;
    }
}
