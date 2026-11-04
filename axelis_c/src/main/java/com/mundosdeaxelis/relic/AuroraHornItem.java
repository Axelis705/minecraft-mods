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

/** Cuerno de la Aurora: su canto da fuerza y velocidad a los aliados y aparta a los enemigos. */
public class AuroraHornItem extends RelicItem {
    public AuroraHornItem() {
        super("aurora_horn", 0x7CFFC8, 20 * 50, Rarity.EPIC);
    }

    @Override
    protected void passive(ServerPlayer sp, ServerLevel sl, long time) {
        sp.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 60, 0, true, false, true));
    }

    @Override
    protected boolean active(ServerPlayer sp, ServerLevel sl, ItemStack stack) {
        double x = sp.getX();
        double y = sp.getY();
        double z = sp.getZ();
        Fx.sound(sl, x, y, z, SoundEvents.GOAT_HORN_SOUND_VARIANTS.get(2).get(), SoundSource.PLAYERS, 3.0F, 1.0F);
        for (int i = 1; i <= 10; i++) {
            final int r = i * 2;
            Scheduler.runLater(i * 2, () -> {
                Fx.ring(sl, Fx.dust(Fx.rainbow(r / 20.0F), 1.5F), x, y + 0.5, z, r, 14 + r * 3, 0);
                Fx.ring(sl, Fx.spark(0xFFFFFF, 0.9F), x, y + 2.0, z, r, 8 + r * 2, r * 0.3);
            });
        }
        int n = 0;
        for (ServerPlayer p : sl.getEntitiesOfClass(ServerPlayer.class, new AABB(x - 16, y - 8, z - 16, x + 16, y + 8, z + 16))) {
            p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 20 * 25, 1));
            p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 20 * 25, 1));
            p.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 20 * 8, 1));
            Fx.screen(p, ScreenFxPacket.STARS, 0x7CFFC8, 100);
            Fx.screen(p, ScreenFxPacket.VIGNETTE, 0x7CFFC8, 100);
            n++;
        }
        for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class, new AABB(x - 12, y - 6, z - 12, x + 12, y + 6, z + 12), e -> e instanceof Enemy && e.isAlive())) {
            Vec3 d = e.position().subtract(sp.position());
            Vec3 nrm = d.length() < 0.01 ? new Vec3(1, 0, 0) : d.normalize();
            e.setDeltaMovement(nrm.x * 1.2, 0.35, nrm.z * 1.2);
            e.hurtMarked = true;
        }
        sp.displayClientMessage(Component.translatable("message.mundos_de_axelis.horn_used", n), true);
        return true;
    }
}
