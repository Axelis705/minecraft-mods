package com.mundosdeaxelis.gema;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/** Los objetos de las gemas (etapa 271): la gema vacía, las gemas de cada elemento y las esencias de infusión. */
public final class GemaItems {
    private GemaItems() {
    }

    /** Gema vacía: su pureza va en el dato «Pureza». Se infunde en la Mesa de Gemas con una esencia. */
    public static class Vacia extends Item {
        public Vacia() {
            super(new Item.Properties().stacksTo(16).rarity(Rarity.EPIC));
        }

        @Override
        public Component getName(ItemStack st) {
            return Component.translatable("item.mundos_de_axelis.gema_vacia").append(" (").append(Gemas.purezaName(Gemas.pureza(st))).append(")");
        }

        @Override
        public void appendHoverText(ItemStack st, @Nullable Level level, List<Component> tip, TooltipFlag flag) {
            tip.add(Component.translatable("tooltip.mundos_de_axelis.gema_vacia").withStyle(ChatFormatting.GRAY));
            tip.add(Component.translatable("tooltip.mundos_de_axelis.gema_vacia.use").withStyle(ChatFormatting.AQUA));
        }
    }

    /** Gema de un elemento: se engarza en un arma en la Mesa de Gemas. */
    public static class Gema extends Item {
        public final Gemas.Elemento el;

        public Gema(Gemas.Elemento el) {
            super(new Item.Properties().stacksTo(16).rarity(Rarity.EPIC));
            this.el = el;
        }

        @Override
        public boolean isFoil(ItemStack st) {
            return Gemas.pureza(st) >= 3;
        }

        @Override
        public Component getName(ItemStack st) {
            return Component.translatable("item.mundos_de_axelis.gema_" + el.id).append(" (").append(Gemas.purezaName(Gemas.pureza(st))).append(")")
                    .withStyle(el.style());
        }

        @Override
        public void appendHoverText(ItemStack st, @Nullable Level level, List<Component> tip, TooltipFlag flag) {
            tip.add(Component.translatable("gema.mundos_de_axelis.efecto." + el.id).withStyle(ChatFormatting.GRAY));
            tip.add(Component.translatable("gema.mundos_de_axelis.efecto_armadura." + el.id).withStyle(ChatFormatting.DARK_GRAY));
            tip.add(Component.translatable("tooltip.mundos_de_axelis.gema.use").withStyle(ChatFormatting.AQUA));
        }
    }

    /** Esencia de infusión de un elemento: convierte una gema vacía en una gema de ese elemento. */
    public static class Esencia extends Item {
        public final Gemas.Elemento el;

        public Esencia(Gemas.Elemento el) {
            super(new Item.Properties().stacksTo(64).rarity(Rarity.RARE));
            this.el = el;
        }

        @Override
        public Component getName(ItemStack st) {
            return super.getName(st).copy().withStyle(el.style());
        }

        @Override
        public void appendHoverText(ItemStack st, @Nullable Level level, List<Component> tip, TooltipFlag flag) {
            tip.add(Component.translatable("tooltip.mundos_de_axelis.esencia_infusion", Component.translatable("item.mundos_de_axelis.gema_" + el.id))
                    .withStyle(ChatFormatting.GRAY));
            tip.add(Component.translatable("gema.mundos_de_axelis.efecto." + el.id).withStyle(ChatFormatting.DARK_GRAY));
            tip.add(Component.translatable("gema.mundos_de_axelis.efecto_armadura." + el.id).withStyle(ChatFormatting.DARK_GRAY));
        }
    }
}
