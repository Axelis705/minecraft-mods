package com.mundosdeaxelis.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import javax.annotation.Nullable;
import java.util.List;

/** La Piedra de Portal de un jefe de etapa, con su explicación. */
public class PortalStoneItem extends BlockItem {
    private final String key;

    public PortalStoneItem(Block block, Properties props, String key) {
        super(block, props);
        this.key = key;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.mundos_de_axelis." + key + ".1").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.mundos_de_axelis." + key + ".2").withStyle(ChatFormatting.LIGHT_PURPLE));
    }
}
