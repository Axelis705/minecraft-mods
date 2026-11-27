package com.navidad.arbol;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import java.util.List;
import java.util.function.Consumer;

/** El Pino de Navidad como objeto: se dibuja en 3D con los adornos que trae puestos (si lo rompiste ya adornado). */
public class PinoItem extends BlockItem {
    public PinoItem(Block b, Properties p) {
        super(b, p);
    }

    @Override
    public void appendHoverText(ItemStack st, Level l, List<Component> t, TooltipFlag f) {
        CompoundTag be = getBlockEntityData(st);
        int n = 0;
        if (be != null) {
            ListTag ls = be.getList("Adornos", Tag.TAG_STRING);
            for (int i = 0; i < ls.size(); i++) {
                if (!ls.getString(i).isEmpty()) {
                    n++;
                }
            }
        }
        if (n > 0 || be != null && (be.getInt("Luces") > 0 || !be.getString("Punta").isEmpty())) {
            t.add(Component.translatable("navidad.pino.trae", n, be.getInt("Luces"), be.getInt("Guirnaldas")).withStyle(ChatFormatting.GOLD));
        }
        t.add(Component.translatable("navidad.pino.ayuda").withStyle(ChatFormatting.GRAY));
    }

    @Override
    public void initializeClient(Consumer<net.minecraftforge.client.extensions.common.IClientItemExtensions> c) {
        c.accept(new net.minecraftforge.client.extensions.common.IClientItemExtensions() {
            private net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer r;

            @Override
            public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (r == null) {
                    r = com.navidad.client.PinoObjeto.crear();
                }
                return r;
            }
        });
    }
}
