package com.navidad.libro;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

import java.util.List;

/** El Libro de Navidad: clic derecho para abrirlo (tareas, premios y cómo va tu árbol). */
public class LibroItem extends Item {
    public LibroItem(Properties p) {
        super(p);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level l, Player p, InteractionHand h) {
        ItemStack st = p.getItemInHand(h);
        if (l.isClientSide) {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> com.navidad.client.LibroPantalla::abrir);
        } else if (p instanceof ServerPlayer sp) {
            com.navidad.ArbolSanta.revisarDe(sp);
            Libro.sync(sp);
            l.playSound(null, p.blockPosition(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1F, 1F);
        }
        return InteractionResultHolder.sidedSuccess(st, l.isClientSide);
    }

    @Override
    public boolean isFoil(ItemStack st) {
        return false;
    }

    @Override
    public void appendHoverText(ItemStack st, Level l, List<Component> t, TooltipFlag f) {
        t.add(Component.translatable("navidad.libro.ayuda").withStyle(ChatFormatting.GRAY));
    }
}
