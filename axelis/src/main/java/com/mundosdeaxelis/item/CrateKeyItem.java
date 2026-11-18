package com.mundosdeaxelis.item;

import com.mundosdeaxelis.crate.CrateKind;
import com.mundosdeaxelis.crate.Tier;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/** Llave de un crate (mascotas, equipo o reliquias). Se gasta al abrir. */
public class CrateKeyItem extends Item {
    public final CrateKind kind;

    public CrateKeyItem(CrateKind kind) {
        super(new Item.Properties().stacksTo(16).rarity(Rarity.RARE));
        this.kind = kind;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public Component getName(ItemStack stack) {
        return super.getName(stack).copy().withStyle(Style.EMPTY.withColor(TextColor.fromRgb(kind.color)));
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.mundos_de_axelis.key." + kind.id).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.mundos_de_axelis.key.use", Component.translatable(kind.titleKey())).withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("tooltip.mundos_de_axelis.key.peek").withStyle(ChatFormatting.DARK_GRAY));
        Tier lo = null;
        Tier hi = null;
        for (Tier t : Tier.values()) {
            if (kind.weight(t) > 0) {
                lo = lo == null ? t : lo;
                hi = t;
            }
        }
        if (lo != null) {
            tooltip.add(Component.translatable("tooltip.mundos_de_axelis.key.range", lo.label(), hi.label()).withStyle(ChatFormatting.DARK_GRAY));
        }
    }
}
