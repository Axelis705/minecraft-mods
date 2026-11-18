package com.mundosdeaxelis.client;

import com.mundosdeaxelis.MundosDeAxelis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Cuadrito con el icono del mod en el menú de pausa (arriba a la izquierda): abre las opciones de Mundos de Axelis. */
@Mod.EventBusSubscriber(modid = MundosDeAxelis.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class PauseMenuButton {
    private PauseMenuButton() {
    }

    @SubscribeEvent
    public static void onInit(ScreenEvent.Init.Post e) {
        if (!(e.getScreen() instanceof PauseScreen ps)) {
            return;
        }
        Button b = new IconButton(6, 6, () -> Minecraft.getInstance().setScreen(new MundosOptionsScreen(ps)));
        b.setTooltip(Tooltip.create(Component.translatable("gui.mundos_de_axelis.options")));
        e.addListener(b);
    }

    private static final class IconButton extends Button {
        IconButton(int x, int y, Runnable action) {
            super(x, y, 20, 20, Component.literal(""), b -> action.run(), DEFAULT_NARRATION);
        }

        @Override
        protected void renderWidget(GuiGraphics g, int mx, int my, float pt) {
            super.renderWidget(g, mx, my, pt);
            g.renderItem(new ItemStack(com.mundosdeaxelis.registry.ModItems.PORTAL_STONE_TOWER.get()), getX() + 2, getY() + 2);
        }
    }
}
