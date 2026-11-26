package com.navidad.bloque;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import java.util.List;

/** La caja de regalo como objeto: si va envuelta dice de quién y para quién (lo de adentro es sorpresa). */
public class CajaRegaloItem extends BlockItem {
    public CajaRegaloItem(Block b, Properties p) {
        super(b, p);
    }

    @Override
    public void appendHoverText(ItemStack s, Level l, List<Component> t, TooltipFlag f) {
        CompoundTag be = s.getTagElement("BlockEntityTag");
        if (be != null && be.contains("Contenido")) {
            t.add(Component.translatable("navidad.caja.item_envuelta").withStyle(ChatFormatting.GOLD));
            t.add(Component.translatable("navidad.caja.item_de", be.getString("De")).withStyle(ChatFormatting.GRAY));
            if (!be.getString("Para").isEmpty()) {
                t.add(Component.translatable("navidad.caja.item_para", be.getString("Para")).withStyle(ChatFormatting.GRAY));
            }
        } else {
            t.add(Component.translatable("navidad.caja.ayuda").withStyle(ChatFormatting.GRAY));
        }
    }
}
