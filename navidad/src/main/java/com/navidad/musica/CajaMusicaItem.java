package com.navidad.musica;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import java.util.List;
import java.util.function.Consumer;

/** La caja de música como objeto (se ve en 3D, cerradita). */
public class CajaMusicaItem extends BlockItem {
    public CajaMusicaItem(Block b, Properties p) {
        super(b, p);
    }

    @Override
    public void appendHoverText(ItemStack st, Level l, List<Component> t, TooltipFlag f) {
        t.add(Component.translatable("navidad.caja_musica.ayuda").withStyle(ChatFormatting.GRAY));
    }

    @Override
    public void initializeClient(Consumer<net.minecraftforge.client.extensions.common.IClientItemExtensions> c) {
        c.accept(new net.minecraftforge.client.extensions.common.IClientItemExtensions() {
            private net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer r;

            @Override
            public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (r == null) {
                    r = com.navidad.client.CajaMusicaObjeto.crear();
                }
                return r;
            }
        });
    }
}
