package com.pooltable.item;

import com.pooltable.block.PoolTableBE;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

/** El taco: mantén el clic derecho para cargar la fuerza y suelta para golpear la blanca en la dirección en que miras. */
public class PoolCueItem extends Item {
    /** Ticks de carga hasta la fuerza máxima. */
    public static final int FULL_CHARGE = 26;
    /** Alcance (en bloques desde el borde de la mesa) para poder tirar. */
    public static final double REACH = 2.3;

    public PoolCueItem(Properties props) {
        super(props);
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 72000;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.NONE;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack s = player.getItemInHand(hand);
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(s);
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
        if (level.isClientSide || !(entity instanceof ServerPlayer sp)) {
            return;
        }
        int charge = getUseDuration(stack) - timeLeft;
        if (charge < 3) {
            return;
        }
        PoolTableBE t = PoolTableBE.findNear(level, sp.position(), REACH);
        if (t != null) {
            t.tryShoot(sp, Math.min(1.0, charge / (double) FULL_CHARGE));
        }
    }
}
