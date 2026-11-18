package com.mundosdeaxelis.relic;

import com.mundosdeaxelis.fx.Fx;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;

/** Corazón de Roble: te rodea de corteza (absorción) cada tanto; activo: renuevas vida y hambre con savia. */
public class OakHeartItem extends RelicItem {
    public OakHeartItem() {
        super("oak_heart", 0x7BE08A, 20 * 60, Rarity.RARE);
    }

    @Override
    protected int interval() {
        return 20;
    }

    @Override
    protected void passive(ServerPlayer sp, ServerLevel sl, long time) {
        if (sp.getAbsorptionAmount() <= 0.0F && time % 600 == 0) {
            sp.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 20 * 30, 0, true, false, true));
            sl.sendParticles(Fx.petal(0x7BE08A, 1.0F), sp.getX(), sp.getY() + 1.2, sp.getZ(), 6, 0.5, 0.5, 0.5, 0.02);
        }
    }

    @Override
    protected boolean active(ServerPlayer sp, ServerLevel sl, ItemStack stack) {
        sp.heal(10.0F);
        sp.getFoodData().eat(8, 0.8F);
        sp.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 20 * 10, 1));
        Fx.sound(sl, sp.getX(), sp.getY(), sp.getZ(), SoundEvents.FLOWERING_AZALEA_PLACE, SoundSource.PLAYERS, 1.0F, 0.8F);
        Fx.sound(sl, sp.getX(), sp.getY(), sp.getZ(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0F, 1.2F);
        Fx.helix(sl, Fx.petal(0x7BE08A, 1.2F), sp.getX(), sp.getY(), sp.getZ(), 0.9, 2.2, 14, 0);
        Fx.ring(sl, Fx.heart(0xFF9CB8, 1.0F), sp.getX(), sp.getY() + 0.3, sp.getZ(), 1.0, 8, 0);
        Fx.screen(sp, com.mundosdeaxelis.network.ScreenFxPacket.PETALS, 0xA8FFB0, 60);
        return true;
    }
}
