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

/** Cáliz Vampírico: robas vida con tus golpes; activo: nova de sangre que daña y te cura. */
public class VampireChaliceItem extends RelicItem {
    public VampireChaliceItem() {
        super("vampire_chalice", 0xC81E3C, 20 * 25, Rarity.EPIC);
    }

    @Override
    protected void passive(ServerPlayer sp, ServerLevel sl, long time) {
    }

    public static void lifesteal(ServerPlayer sp, float damage) {
        float heal = Math.min(4.0F, damage * 0.2F);
        if (heal > 0.0F) {
            sp.heal(heal);
            sp.serverLevel().sendParticles(Fx.heart(0xC81E3C, 0.8F), sp.getX(), sp.getY() + 1.6, sp.getZ(), 2, 0.3, 0.2, 0.3, 0.02);
        }
    }

    @Override
    protected boolean active(ServerPlayer sp, ServerLevel sl, ItemStack stack) {
        double x = sp.getX();
        double y = sp.getY();
        double z = sp.getZ();
        Fx.sound(sl, x, y, z, SoundEvents.WITHER_SPAWN, SoundSource.PLAYERS, 0.5F, 1.8F);
        Fx.sound(sl, x, y, z, SoundEvents.BREWING_STAND_BREW, SoundSource.PLAYERS, 1.0F, 0.6F);
        Fx.screen(sp, ScreenFxPacket.VIGNETTE, 0xC81E3C, 50);
        for (int i = 1; i <= 6; i++) {
            final int r = i;
            Scheduler.runLater(i, () -> Fx.ring(sl, Fx.dust(0xC81E3C, 1.5F), x, y + 0.3, z, r, 12 + r * 4, r * 0.3));
        }
        float healed = 0.0F;
        for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class, new AABB(x - 6, y - 3, z - 6, x + 6, y + 3, z + 6), e -> e != sp && e instanceof Enemy && e.isAlive())) {
            e.hurt(sl.damageSources().magic(), 6.0F);
            healed += 3.0F;
            for (int k = 1; k <= 8; k++) {
                final int step = k;
                final LivingEntity target = e;
                Scheduler.runLater(k + 2, () -> {
                    Vec3 a = target.position().add(0.0, 1.0, 0.0);
                    Vec3 b = sp.position().add(0.0, 1.0, 0.0);
                    Vec3 p = a.lerp(b, step / 8.0);
                    sp.serverLevel().sendParticles(Fx.heart(0xC81E3C, 0.7F), p.x, p.y, p.z, 1, 0.05, 0.05, 0.05, 0.0);
                });
            }
        }
        sp.heal(Math.min(healed, 14.0F));
        return true;
    }
}
