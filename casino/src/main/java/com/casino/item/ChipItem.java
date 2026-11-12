package com.casino.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/** Una ficha de casino con un valor fijo. */
public class ChipItem extends Item {
    public final int value;

    public ChipItem(Properties props, int value) {
        super(props);
        this.value = value;
    }

    //#if MC < 1.20.5
    @Override
    public Rarity getRarity(ItemStack stack) {
        return value >= 500 ? Rarity.EPIC : value >= 100 ? Rarity.RARE : value >= 25 ? Rarity.UNCOMMON : Rarity.COMMON;
    }
    //#endif

    //#if MC >= 1.21.5
    //$$ @Override
    //$$ public void appendHoverText(ItemStack stack, Item.TooltipContext ctx, net.minecraft.world.item.component.TooltipDisplay display,
    //$$         java.util.function.Consumer<Component> add, net.minecraft.world.item.TooltipFlag flag) {
    //$$     tips(stack, add);
    //$$ }
    //#elif MC >= 1.20.5
    //$$ @Override
    //$$ public void appendHoverText(ItemStack stack, Item.TooltipContext ctx, List<Component> tip, net.minecraft.world.item.TooltipFlag flag) {
    //$$     tips(stack, tip::add);
    //$$ }
    //#else
    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tip, net.minecraft.world.item.TooltipFlag flag) {
        tips(stack, tip::add);
    }
    //#endif

    private void tips(ItemStack stack, java.util.function.Consumer<Component> tip) {
        tip.accept(Component.translatable("casino.chip.value", value).withStyle(ChatFormatting.GOLD));
        if (stack.getCount() > 1) {
            tip.accept(Component.translatable("casino.chip.stack", value * stack.getCount()).withStyle(ChatFormatting.GRAY));
        }
    }
}
