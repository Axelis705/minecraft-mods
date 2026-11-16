package com.casino.west;

import com.casino.plat.ItemTag;
import com.casino.plat.Plat;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;

/** Un juego que presta el revólver de feria (galería de tiro, duelo): el revólver le avisa a su juego cada vez que disparas. */
public interface GunGame {
    boolean isShooter(UUID id);

    void shoot(ServerPlayer p);

    /** Presta un revólver ligado a este juego (en la mano si está vacía; si no, al inventario). */
    static void lend(ServerPlayer p, BlockPos game) {
        ItemStack gun = new ItemStack(WestReg.REVOLVER.get());
        ItemTag.edit(gun, t -> {
            t.putInt("GalleryX", game.getX());
            t.putInt("GalleryY", game.getY());
            t.putInt("GalleryZ", game.getZ());
        });
        if (p.getMainHandItem().isEmpty()) {
            p.setItemInHand(InteractionHand.MAIN_HAND, gun);
        } else {
            net.minecraftforge.items.ItemHandlerHelper.giveItemToPlayer(p, gun);
            Plat.msg(p, Component.translatable("casino.gallery.gun_inventory").withStyle(ChatFormatting.YELLOW), false);
        }
    }

    /** Recoge los revólveres prestados. */
    static void takeBack(ServerPlayer p) {
        for (ItemStack s : Plat.mainAndOffhand(p.getInventory())) {
            if (s.getItem() instanceof RevolverItem) {
                s.setCount(0);
            }
        }
    }
}
