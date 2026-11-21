package com.mundosdeaxelis.item;

import com.mundosdeaxelis.block.VaultLoot;
import com.mundosdeaxelis.crate.CrateKind;
import com.mundosdeaxelis.registry.ModItems;
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

/**
 * Fragmento de llave de una caja de colección (etapa 268). Sale en los cofres y barriles de las estructuras del tier de esa caja;
 * con {@link VaultLoot#FRAGMENTS_PER_KEY} se forja la llave en la Mesa del Artífice (pestaña Crear).
 */
public class KeyFragmentItem extends Item {
    public final CrateKind kind;

    public KeyFragmentItem(CrateKind kind) {
        super(new Item.Properties().stacksTo(64).rarity(Rarity.UNCOMMON));
        this.kind = kind;
    }

    @Override
    public Component getName(ItemStack stack) {
        return super.getName(stack).copy().withStyle(Style.EMPTY.withColor(TextColor.fromRgb(kind.color)));
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.mundos_de_axelis.key_fragment").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.mundos_de_axelis.key_fragment.use", VaultLoot.FRAGMENTS_PER_KEY,
                Component.translatable(ModItems.KEYS.get(kind).get().getDescriptionId())).withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("tooltip.mundos_de_axelis.key_fragment.where").withStyle(ChatFormatting.DARK_GRAY));
    }
}
