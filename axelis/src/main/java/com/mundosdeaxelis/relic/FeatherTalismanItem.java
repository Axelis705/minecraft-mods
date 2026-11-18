package com.mundosdeaxelis.relic;

import com.mundosdeaxelis.fx.Fx;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.Rarity;

/** Talismán de Pluma: sin daño de caída, y si te agachas en el aire caes como una pluma. */
public class FeatherTalismanItem extends RelicItem {
    public FeatherTalismanItem() {
        super("feather_talisman", 0xBFF1FF, 0, Rarity.UNCOMMON);
    }

    @Override
    protected int interval() {
        return 4;
    }

    @Override
    protected void passive(ServerPlayer sp, ServerLevel sl, long time) {
        if (!sp.onGround() && !sp.isInWater() && sp.isShiftKeyDown() && sp.getDeltaMovement().y < 0.0) {
            sp.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 10, 0, true, false, false));
            sl.sendParticles(Fx.dust(0xFFFFFF, 0.6F), sp.getX(), sp.getY() + 0.2, sp.getZ(), 2, 0.3, 0.05, 0.3, 0.0);
        }
    }
}
