package com.casino.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Moneda arcade (para jugar en la sala recreativa del casino Divertido) o boleto (lo que sueltan sus máquinas). Se cambian en el
 * cajero por fichas o dinero, y al revés; los boletos también en el mostrador de premios.
 */
public class ArcadeItem extends Item {
    /** Clave del texto de abajo ("casino.arcade.coin_tip" o "casino.arcade.ticket_tip") y su valor en fichas. */
    private final String tipKey;
    private final int value;

    public ArcadeItem(Properties props, String tipKey, int value) {
        super(props);
        this.tipKey = tipKey;
        this.value = value;
    }

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
        tip.accept(Component.translatable(tipKey).withStyle(ChatFormatting.GRAY));
        tip.accept(Component.translatable("casino.arcade.value", value).withStyle(ChatFormatting.GOLD));
        if (stack.getCount() > 1) {
            tip.accept(Component.translatable("casino.chip.stack", value * stack.getCount()).withStyle(ChatFormatting.DARK_GRAY));
        }
    }
}
