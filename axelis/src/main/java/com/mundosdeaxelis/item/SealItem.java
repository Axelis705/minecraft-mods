package com.mundosdeaxelis.item;

import com.mundosdeaxelis.crate.CrateForge;
import com.mundosdeaxelis.crate.CrateKind;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/** Sello que suelta el jefe de cada etapa. Se gasta al forjar el bloque de la caja siguiente. */
public class SealItem extends Item {
    public final CrateKind kind;

    public SealItem(CrateKind kind) {
        super(new Item.Properties().rarity(Rarity.EPIC));
        this.kind = kind;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.mundos_de_axelis.seal.1", Component.translatable(kind.titleKey())).withStyle(ChatFormatting.GRAY));
        CrateKind next = CrateForge.unlocks(kind);
        if (next != null) {
            tooltip.add(Component.translatable("tooltip.mundos_de_axelis.seal.2", Component.translatable(next.titleKey())).withStyle(ChatFormatting.LIGHT_PURPLE));
        } else {
            tooltip.add(Component.translatable("tooltip.mundos_de_axelis.seal.last").withStyle(ChatFormatting.LIGHT_PURPLE));
        }
    }
}
