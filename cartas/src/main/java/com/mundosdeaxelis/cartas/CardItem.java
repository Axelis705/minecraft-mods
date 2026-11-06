package com.mundosdeaxelis.cartas;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/** Una carta. La version holografica es la misma carta con la marca "holo" y brillo. */
public class CardItem extends Item {
    public static final String HOLO = "holo";
    public final CardDef def;

    public CardItem(CardDef def) {
        super(new Item.Properties().stacksTo(64));
        this.def = def;
    }

    public static boolean isHolo(ItemStack s) {
        return s.hasTag() && s.getTag().getBoolean(HOLO);
    }

    public static ItemStack make(net.minecraft.world.item.Item item, boolean holo, int n) {
        ItemStack s = new ItemStack(item, n);
        if (holo) {
            s.getOrCreateTag().putBoolean(HOLO, true);
        }
        return s;
    }

    @Override
    public Component getName(ItemStack stack) {
        MutableComponent c = Component.literal(def.name).withStyle(Style.EMPTY.withColor(def.rarity.color));
        if (isHolo(stack)) {
            return Component.literal("✦ Holográfica: ").withStyle(Style.EMPTY.withColor(0x7FE8FF)).append(c);
        }
        return Component.literal("Carta: ").withStyle(ChatFormatting.GRAY).append(c);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return isHolo(stack) || def.rarity.atLeast(CardRarity.LEGENDARIA);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.literal(def.rarity.label).withStyle(Style.EMPTY.withColor(def.rarity.color)));
        CardCategory cat = Cards.category(def.category);
        if (cat != null) {
            tip.add(Component.literal(cat.name).withStyle(Style.EMPTY.withColor(cat.color)));
        }
        tip.add(Component.literal(def.desc).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        if (isHolo(stack)) {
            tip.add(Component.literal("Edición holográfica").withStyle(Style.EMPTY.withColor(0x7FE8FF)));
        }
        tip.add(Component.literal("Guárdala en tu álbum").withStyle(ChatFormatting.DARK_GRAY));
    }
}
