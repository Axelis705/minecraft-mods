package com.casino.item;

import com.casino.client.ClientHooks;
import com.casino.plat.Net;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
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

/** La Guía del Casino: un libro con las reglas de cada juego y cómo usar la decoración. */
public class GuideItem extends Item {
    public GuideItem(Properties props) {
        super(props);
    }

    private static void open(Level level) {
        if (level.isClientSide()) {
            Net.onClient(ClientHooks::openGuide);
        }
    }

    //#if MC >= 1.21.2
    //$$ @Override
    //$$ public InteractionResult use(Level level, Player player, InteractionHand hand) {
    //$$     open(level);
    //$$     return InteractionResult.SUCCESS;
    //$$ }
    //#else
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        open(level);
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide());
    }
    //#endif

    //#if MC >= 1.21.5
    //$$ @Override
    //$$ public void appendHoverText(ItemStack stack, Item.TooltipContext ctx, net.minecraft.world.item.component.TooltipDisplay display,
    //$$         java.util.function.Consumer<Component> add, TooltipFlag flag) {
    //$$     add.accept(Component.translatable("casino.guide.tooltip").withStyle(ChatFormatting.GRAY));
    //$$ }
    //#elif MC >= 1.20.5
    //$$ @Override
    //$$ public void appendHoverText(ItemStack stack, Item.TooltipContext ctx, List<Component> tip, TooltipFlag flag) {
    //$$     tip.add(Component.translatable("casino.guide.tooltip").withStyle(ChatFormatting.GRAY));
    //$$ }
    //#else
    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.translatable("casino.guide.tooltip").withStyle(ChatFormatting.GRAY));
    }
    //#endif
}
