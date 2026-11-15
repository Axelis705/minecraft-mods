package com.casino.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/** Ficha de colección de un nivel de socio: un recuerdo de cuando llegaste a ese nivel (no vale para apostar). */
public class MemberChipItem extends Item {
    private final String level;

    public MemberChipItem(Properties props, String level) {
        super(props);
        this.level = level;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return level.equals("diamond") || level.equals("platinum");
    }

    private void tips(java.util.function.Consumer<Component> add) {
        add.accept(Component.translatable("casino.member.chip_tooltip", Component.translatable("casino.member.level_" + level))
                .withStyle(ChatFormatting.GRAY));
    }

    //#if MC >= 1.21.5
    //$$ @Override
    //$$ public void appendHoverText(ItemStack stack, Item.TooltipContext ctx, net.minecraft.world.item.component.TooltipDisplay display,
    //$$         java.util.function.Consumer<Component> add, TooltipFlag flag) {
    //$$     tips(add);
    //$$ }
    //#elif MC >= 1.20.5
    //$$ @Override
    //$$ public void appendHoverText(ItemStack stack, Item.TooltipContext ctx, List<Component> tip, TooltipFlag flag) {
    //$$     tips(tip::add);
    //$$ }
    //#else
    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level lvl, List<Component> tip, TooltipFlag flag) {
        tips(tip::add);
    }
    //#endif
}
