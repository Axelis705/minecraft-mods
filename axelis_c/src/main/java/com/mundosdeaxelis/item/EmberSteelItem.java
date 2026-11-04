package com.mundosdeaxelis.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Acero Ígneo: lingote que solo se saca de la dungeon de la Forja (salas despejadas y Gran Herrero).
 * No se refina: es el ingrediente para crear cosas en la pestaña «Crear» de la Mesa del Artífice.
 */
public class EmberSteelItem extends Item {
    public EmberSteelItem() {
        super(new Item.Properties().rarity(Rarity.UNCOMMON));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return false;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.mundos_de_axelis.ember_steel.1").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.mundos_de_axelis.ember_steel.2").withStyle(ChatFormatting.DARK_GRAY));
    }
}
