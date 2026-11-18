package com.mundosdeaxelis.cartas;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/** El album: clic derecho (o la tecla) para abrirlo. El progreso es del jugador, no del objeto. */
public class AlbumItem extends Item {
    public AlbumItem() {
        super(new Item.Properties().stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide && player instanceof ServerPlayer sp) {
            CartasNet.openAlbum(sp);
        }
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.literal("Guarda aquí tus cartas.").withStyle(ChatFormatting.GRAY));
        tip.add(Component.literal("Cada 5 % del álbum y cada categoría completa te dan vida extra.").withStyle(ChatFormatting.GRAY));
        tip.add(Component.literal("Clic derecho para abrir").withStyle(ChatFormatting.DARK_GRAY));
    }
}
