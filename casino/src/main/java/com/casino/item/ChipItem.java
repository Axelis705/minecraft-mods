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

    @Override
    public Rarity getRarity(ItemStack stack) {
        return value >= 500 ? Rarity.EPIC : value >= 100 ? Rarity.RARE : value >= 25 ? Rarity.UNCOMMON : Rarity.COMMON;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tip, net.minecraft.world.item.TooltipFlag flag) {
        tip.add(Component.translatable("casino.chip.value", value).withStyle(ChatFormatting.GOLD));
        if (stack.getCount() > 1) {
            tip.add(Component.translatable("casino.chip.stack", value * stack.getCount()).withStyle(ChatFormatting.GRAY));
        }
    }
}
