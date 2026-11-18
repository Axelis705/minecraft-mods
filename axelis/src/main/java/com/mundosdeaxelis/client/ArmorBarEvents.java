package com.mundosdeaxelis.client;

import com.mundosdeaxelis.MundosDeAxelis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Barra de armadura con capas: la barra de Minecraft solo dibuja 20 puntos (10 iconos). Cuando tienes más, aquí se dibuja
 * encima otra fila de iconos teñida con un color nuevo por cada 20 puntos extra (celeste, dorado, magenta, verde...), parecido
 * a lo que hace el mod Adaptive Bars.
 */
@Mod.EventBusSubscriber(modid = MundosDeAxelis.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class ArmorBarEvents {
    private static final ResourceLocation ICONS = new ResourceLocation("textures/gui/icons.png");
    /** Color de cada capa extra (RGB). */
    private static final int[] LAYERS = {0x4FE3FF, 0xFFC93C, 0xFF5AD2, 0x6BFF7A, 0xFF8A3C, 0xFFFFFF};

    private ArmorBarEvents() {
    }

    @SubscribeEvent
    public static void onOverlay(RenderGuiOverlayEvent.Post event) {
        if (event.getOverlay() != VanillaGuiOverlay.ARMOR_LEVEL.type()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.options.hideGui || !(mc.gui instanceof ForgeGui gui)) {
            return;
        }
        int level = player.getArmorValue();
        if (level <= 20) {
            return;
        }
        GuiGraphics g = event.getGuiGraphics();
        int width = event.getWindow().getGuiScaledWidth();
        int height = event.getWindow().getGuiScaledHeight();
        int left = width / 2 - 91;
        // Forge ya sumó 10 a leftHeight al terminar de dibujar la armadura
        int top = height - (gui.leftHeight - 10);
        int layers = (level - 1) / 20;
        for (int layer = 1; layer <= layers; layer++) {
            int pts = Math.min(20, level - 20 * layer);
            int rgb = LAYERS[(layer - 1) % LAYERS.length];
            g.setColor(((rgb >> 16) & 255) / 255.0F, ((rgb >> 8) & 255) / 255.0F, (rgb & 255) / 255.0F, 1.0F);
            for (int i = 0; i < 10; i++) {
                int point = i * 2 + 1;
                int u;
                if (point < pts) {
                    u = 34;
                } else if (point == pts) {
                    u = 25;
                } else {
                    break;
                }
                g.blit(ICONS, left + i * 8, top, u, 9, 9, 9);
            }
        }
        g.setColor(1.0F, 1.0F, 1.0F, 1.0F);
    }
}
