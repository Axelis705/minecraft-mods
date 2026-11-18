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

/** Corazón de Cristal: se regenera un escudo de cristal; activo: barrera que repele y protege. */
public class CrystalHeartItem extends RelicItem {
    public CrystalHeartItem() {
        super("crystal_heart", 0x8FE8FF, 20 * 35, Rarity.EPIC);
    }

    @Override
    protected int interval() {
        return 20;
    }

    @Override
    protected void passive(ServerPlayer sp, ServerLevel sl, long time) {
        if (sp.getAbsorptionAmount() < 4.0F && time % 400 == 0) {
            sp.setAbsorptionAmount(4.0F);
            sl.sendParticles(Fx.spark(0x8FE8FF, 1.2F), sp.getX(), sp.getY() + 1.2, sp.getZ(), 6, 0.5, 0.6, 0.5, 0.0);
        }
    }

    @Override
    protected boolean active(ServerPlayer sp, ServerLevel sl, ItemStack stack) {
        double x = sp.getX();
        double y = sp.getY();
        double z = sp.getZ();
        sp.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 20 * 6, 3));
        sp.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 20 * 12, 2));
        Fx.sound(sl, x, y, z, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.4F, 1.6F);
        Fx.sound(sl, x, y, z, SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 0.6F, 1.8F);
        Fx.screen(sp, ScreenFxPacket.VIGNETTE, 0x8FE8FF, 60);
        for (int i = 0; i < 12; i++) {
            final int k = i;
            Scheduler.runLater(i, () -> {
                double h = k * 0.2;
                double r = 2.4 * Math.sin(Math.min(1.0, (k + 1) / 12.0) * Math.PI * 0.5 + 0.2);
                Fx.ring(sl, Fx.dust(0x8FE8FF, 1.2F), x, y + h, z, Math.min(2.6, r + 0.6), 18, k * 0.4);
                Fx.ring(sl, Fx.spark(0xFFFFFF, 0.8F), x, y + h, z, Math.min(2.6, r + 0.6), 6, -k * 0.5);
            });
        }
        for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class, new AABB(x - 5, y - 3, z - 5, x + 5, y + 3, z + 5), e -> e != sp && e instanceof Enemy && e.isAlive())) {
            Vec3 d = e.position().subtract(sp.position());
            Vec3 n = d.length() < 0.01 ? new Vec3(1, 0, 0) : d.normalize();
            e.setDeltaMovement(n.x * 1.4, 0.4, n.z * 1.4);
            e.hurtMarked = true;
        }
        return true;
    }
}
