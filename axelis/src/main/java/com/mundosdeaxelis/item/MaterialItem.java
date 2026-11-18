package com.mundosdeaxelis.item;

import com.mundosdeaxelis.table.Mats;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/** Material de un mob: sirve para refinar Esencia y para subir de nivel la Mesa del Artífice. */
public class MaterialItem extends Item {
    public final Mats mat;

    public MaterialItem(Mats mat) {
        super(new Item.Properties().rarity(mat.rarity));
        this.mat = mat;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.mundos_de_axelis.mat." + mat.id).withStyle(ChatFormatting.GRAY));
        if (mat.isBoss()) {
            tooltip.add(Component.translatable("tooltip.mundos_de_axelis.mat.boss", mat.bossStage + 1).withStyle(ChatFormatting.GOLD));
            tooltip.add(Component.translatable("tooltip.mundos_de_axelis.mat.boss_use").withStyle(ChatFormatting.DARK_GRAY));
            return;
        }
        tooltip.add(Component.translatable("tooltip.mundos_de_axelis.mat.value", mat.essence).withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("tooltip.mundos_de_axelis.mat.use").withStyle(ChatFormatting.DARK_GRAY));
    }
}
