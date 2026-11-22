package com.mundosdeaxelis.gema;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Etapa 286: la Guía del Gemista. Un libro que se abre con clic derecho y explica las gemas de principio a fin: de dónde salen las
 * gemas vacías y las esencias, la pureza, infundir, combinar, engarzar y quitar, y qué hace cada elemento en el arma y en la
 * armadura. Las páginas son textos del idioma del jugador (se leen en el cliente). La Mesa de Gemas te la da la primera vez.
 */
public class GuiaGemasItem extends Item {
    public GuiaGemasItem() {
        super(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide) {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> com.mundosdeaxelis.client.GuiaGemasLibro::abrir);
        }
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.translatable("tooltip.mundos_de_axelis.guia_gemas").withStyle(ChatFormatting.GRAY));
    }
}
