package com.mundosdeaxelis.relic;

import com.mundosdeaxelis.fx.Fx;
import com.mundosdeaxelis.network.ScreenFxPacket;
import com.mundosdeaxelis.world.Scheduler;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.phys.Vec3;

/** Amuleto de la Estrella Fugaz: buena suerte constante; activo: lluvia de estrellas que deja experiencia. */
public class StarCharmItem extends RelicItem {
    public StarCharmItem() {
        super("star_charm", 0xFFE680, 20 * 90, Rarity.EPIC);
    }

    @Override
    protected int interval() {
        return 40;
    }

    @Override
    protected void passive(ServerPlayer sp, ServerLevel sl, long time) {
        sp.addEffect(new MobEffectInstance(MobEffects.LUCK, 100, 1, true, false, true));
    }

    @Override
    protected boolean active(ServerPlayer sp, ServerLevel sl, ItemStack stack) {
        Fx.sound(sl, sp.getX(), sp.getY(), sp.getZ(), SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.PLAYERS, 1.0F, 1.4F);
        Fx.screen(sp, ScreenFxPacket.STARS, 0xFFE680, 120);
        Fx.screen(sp, ScreenFxPacket.VIGNETTE, 0xFFE680, 120);
        for (int i = 0; i < 24; i++) {
            final int k = i;
            Scheduler.runLater(5 + i * 5, () -> {
                if (!sp.isAlive() || sp.hasDisconnected()) {
                    return;
                }
                ServerLevel w = sp.serverLevel();
                double a = w.getRandom().nextDouble() * Math.PI * 2.0;
                double r = 2.0 + w.getRandom().nextDouble() * 5.0;
                double x = sp.getX() + Math.cos(a) * r;
                double z = sp.getZ() + Math.sin(a) * r;
                double y = sp.getY() + 9.0;
                for (int s = 0; s < 9; s++) {
                    w.sendParticles(Fx.spark(0xFFE680, 1.2F - s * 0.08F), x, y - s * 1.0, z, 1, 0.05, 0.05, 0.05, 0.0);
                    w.sendParticles(Fx.dust(0xFFF3B0, 0.8F), x, y - s * 1.0 + 0.4, z, 1, 0.08, 0.08, 0.08, 0.0);
                }
                w.sendParticles(Fx.spark(0xFFFFFF, 1.6F), x, sp.getY() + 0.3, z, 6, 0.3, 0.2, 0.3, 0.0);
                ExperienceOrb.award(w, new Vec3(x, sp.getY() + 0.5, z), 3 + k % 4);
                w.playSound(null, x, sp.getY(), z, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.6F, 1.0F + (k % 5) * 0.12F);
            });
        }
        return true;
    }
}
