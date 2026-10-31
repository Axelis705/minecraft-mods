package com.pooltable.item;

import com.pooltable.block.BowlingLaneBE;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

/** La bola de boliche: mantén el clic derecho para cargar la fuerza y suelta para lanzar por la pista en la dirección en que miras. */
public class BowlingBallItem extends Item {
    public static final int FULL_CHARGE = 22;

    public BowlingBallItem(Properties props) {
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
        BowlingLaneBE lane = BowlingLaneBE.findUnder(level, sp.position());
        if (lane == null) {
            sp.displayClientMessage(Component.translatable("pooltable.bowling.not_on_lane"), true);
            return;
        }
        lane.tryThrow(sp, Math.min(1.0, charge / (double) FULL_CHARGE));
    }
}
