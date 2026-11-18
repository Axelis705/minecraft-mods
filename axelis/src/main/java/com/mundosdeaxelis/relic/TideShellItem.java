package com.mundosdeaxelis.relic;

import com.mundosdeaxelis.fx.Fx;
import com.mundosdeaxelis.network.ScreenFxPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.phys.Vec3;

/** Concha de la Marea: respiras bajo el agua, nadas veloz y ves claro; activo: salto de delfín. */
public class TideShellItem extends RelicItem {
    public TideShellItem() {
        super("tide_shell", 0x6FD8FF, 20 * 6, Rarity.RARE);
    }

    @Override
    protected void passive(ServerPlayer sp, ServerLevel sl, long time) {
        if (sp.isInWaterOrBubble()) {
            sp.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, 80, 0, true, false, true));
            sp.addEffect(new MobEffectInstance(MobEffects.DOLPHINS_GRACE, 60, 0, true, false, true));
            sp.addEffect(new MobEffectInstance(MobEffects.CONDUIT_POWER, 60, 0, true, false, false));
        }
    }

    @Override
    protected boolean active(ServerPlayer sp, ServerLevel sl, ItemStack stack) {
        Vec3 look = sp.getLookAngle();
        boolean wet = sp.isInWaterOrBubble();
        sp.setDeltaMovement(look.x * 1.3, wet ? Math.max(look.y * 1.3, 0.6) : 0.9, look.z * 1.3);
        sp.hurtMarked = true;
        sp.fallDistance = 0.0F;
        Fx.sound(sl, sp.getX(), sp.getY(), sp.getZ(), SoundEvents.DOLPHIN_JUMP, SoundSource.PLAYERS, 1.0F, 1.2F);
        Fx.sound(sl, sp.getX(), sp.getY(), sp.getZ(), SoundEvents.BUBBLE_COLUMN_UPWARDS_AMBIENT, SoundSource.PLAYERS, 1.0F, 1.0F);
        Fx.screen(sp, ScreenFxPacket.BUBBLES, 0xFFFFFF, 40);
        for (int i = 0; i < 16; i++) {
            sl.sendParticles(Fx.bubble(0xFFFFFF, 1.0F), sp.getX(), sp.getY() + 0.5, sp.getZ(), 1, 0.4, 0.4, 0.4, 0.05);
        }
        Fx.ring(sl, Fx.dust(0x6FD8FF, 1.2F), sp.getX(), sp.getY() + 0.3, sp.getZ(), 1.0, 16, 0);
        return true;
    }
}
