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

/** Objeto de decoración (bloque para adornar): solo añade una línea que lo identifica. */
public class DecorItem extends BlockItem {
    public DecorItem(Block block, Properties props) {
        super(block, props);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.mundos_de_axelis.decor").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        if (getBlock() instanceof com.mundosdeaxelis.block.BenchBlock) {
            tooltip.add(Component.translatable("tooltip.mundos_de_axelis.decor_sit").withStyle(ChatFormatting.GRAY));
        }
        var key = net.minecraftforge.registries.ForgeRegistries.BLOCKS.getKey(getBlock());
        com.mundosdeaxelis.block.DecorDefs.Def def = key == null ? null : com.mundosdeaxelis.block.DecorDefs.get(key.getPath());
        if (def != null && def.crates().isEmpty()) {
            tooltip.add(Component.translatable("tooltip.mundos_de_axelis.decor_trophy").withStyle(ChatFormatting.GOLD));
            return;
        }
        if (def != null) {
            net.minecraft.network.chat.MutableComponent crates = Component.empty();
            boolean first = true;
            for (com.mundosdeaxelis.crate.CrateKind k : com.mundosdeaxelis.crate.CrateKind.values()) {
                if (def.dropsIn(k.id)) {
                    if (!first) {
                        crates.append(Component.literal(", ").withStyle(ChatFormatting.DARK_GRAY));
                    }
                    crates.append(Component.translatable(k.titleKey()).withStyle(net.minecraft.network.chat.Style.EMPTY.withColor(k.color)));
                    first = false;
                }
            }
            tooltip.add(Component.translatable("tooltip.mundos_de_axelis.decor_in", crates).withStyle(ChatFormatting.GRAY));
        }
    }
}
