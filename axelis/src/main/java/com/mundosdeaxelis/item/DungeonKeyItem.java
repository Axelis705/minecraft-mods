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

/** Llave del Santuario (bosque) o Llave de Hierro (Forja): abre un Cofre Sellado de la dungeon (se gasta; cada jugador usa la suya). */
public class DungeonKeyItem extends Item {
    private final String key;

    public DungeonKeyItem() {
        this("dungeon_key");
    }

    /** {@code key}: dungeon_key (bosque) o forge_key (Forja): de ahí salen los textos del objeto. */
    public DungeonKeyItem(String key) {
        super(new Item.Properties().stacksTo(16).rarity(Rarity.UNCOMMON));
        this.key = key;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.mundos_de_axelis." + key).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.mundos_de_axelis." + key + ".use").withStyle(ChatFormatting.AQUA));
    }
}
