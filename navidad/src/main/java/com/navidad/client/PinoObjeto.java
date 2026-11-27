package com.navidad.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.navidad.arbol.PuntosArbol;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/** (Cliente) El Pino de Navidad en el inventario y en la mano: el mismo dibujo del árbol, con los adornos que guarda el objeto. */
public final class PinoObjeto extends BlockEntityWithoutLevelRenderer {
    private PinoObjeto() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    public static BlockEntityWithoutLevelRenderer crear() {
        return new PinoObjeto();
    }

    @Override
    public void renderByItem(ItemStack st, ItemDisplayContext ctx, PoseStack ps, MultiBufferSource buf, int luz, int overlay) {
        String[] lugares = new String[PuntosArbol.LUGARES.length];
        java.util.Arrays.fill(lugares, "");
        int luces = 0, guirnaldas = 0;
        String punta = "";
        CompoundTag be = BlockItem.getBlockEntityData(st);
        if (be != null) {
            ListTag l = be.getList("Adornos", Tag.TAG_STRING);
            for (int i = 0; i < Math.min(l.size(), lugares.length); i++) {
                lugares[i] = l.getString(i);
            }
            luces = be.getInt("Luces");
            guirnaldas = be.getInt("Guirnaldas");
            punta = be.getString("Punta");
        }
        var mc = Minecraft.getInstance();
        float t = mc.level == null ? 0 : mc.level.getGameTime() + mc.getFrameTime();
        PinoRenderer.dibujar(ps, buf, luz, overlay, lugares, luces, guirnaldas, punta, t, null, mc.level, 7, false);
    }
}
