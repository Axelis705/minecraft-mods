package com.casino.item;

import com.casino.net.MemberPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/** Tarjeta de socio: con clic derecho muestra tu nivel, tus números, tus logros y el bono diario. La da el casino en tu primera apuesta. */
public class MemberCardItem extends Item {
    public MemberCardItem(Properties props) {
        super(props);
    }

    private static void open(Level level, Player player) {
        if (!level.isClientSide() && player instanceof ServerPlayer sp) {
            MemberPacket.open(sp);
        }
    }

    //#if MC >= 1.21.2
    //$$ @Override
    //$$ public InteractionResult use(Level level, Player player, InteractionHand hand) {
    //$$     open(level, player);
    //$$     return InteractionResult.SUCCESS;
    //$$ }
    //#else
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        open(level, player);
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide());
    }
    //#endif

    //#if MC >= 1.21.5
    //$$ @Override
    //$$ public void appendHoverText(ItemStack stack, Item.TooltipContext ctx, net.minecraft.world.item.component.TooltipDisplay display,
    //$$         java.util.function.Consumer<Component> add, TooltipFlag flag) {
    //$$     add.accept(Component.translatable("casino.member.tooltip").withStyle(ChatFormatting.GRAY));
    //$$ }
    //#elif MC >= 1.20.5
    //$$ @Override
    //$$ public void appendHoverText(ItemStack stack, Item.TooltipContext ctx, List<Component> tip, TooltipFlag flag) {
    //$$     tip.add(Component.translatable("casino.member.tooltip").withStyle(ChatFormatting.GRAY));
    //$$ }
    //#else
    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.translatable("casino.member.tooltip").withStyle(ChatFormatting.GRAY));
    }
    //#endif

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}
