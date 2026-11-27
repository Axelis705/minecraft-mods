package com.navidad.client;

import com.navidad.Navidad;
import com.navidad.nieve.TanqueMenu;
import com.navidad.nieve.TanqueNieve;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/**
 * (Cliente) La ventana del tanque de nieve: el tanque dibujado con su ventanita que se llena de nieve (con copitos que se mueven) según
 * los tiros que tiene, la aguja del manómetro, cuántos tiros son, y las 6 ranuras para bolas o bloques de nieve.
 */
public class TanquePantalla extends AbstractContainerScreen<TanqueMenu> {
    private static final ResourceLocation TEX = new ResourceLocation(Navidad.MODID, "textures/gui/tanque.png");

    public TanquePantalla(TanqueMenu m, Inventory inv, Component titulo) {
        super(m, inv, titulo);
        imageWidth = 176;
        imageHeight = 166;
        inventoryLabelY = imageHeight - 94;
    }

    private int tiros() {
        ItemStack t = menu.tanque();
        int suelta = t.getItem() instanceof TanqueNieve ? TanqueNieve.suelta(t) : 0;
        return menu.tirosEnRanuras() + suelta;
    }

    @Override
    protected void renderBg(GuiGraphics g, float pt, int mx, int my) {
        int x = leftPos, y = topPos;
        g.blit(TEX, x, y, 0, 0, imageWidth, imageHeight, 256, 256);
        // la nieve de la ventanita (de abajo hacia arriba) con copitos que suben y bajan
        int tiros = tiros();
        float k = Mth.clamp(tiros / (float) TanqueNieve.LLENO, 0, 1);
        int alto = Math.round(42 * k);
        int wx = x + 32, wy = y + 26, ww = 18, wh = 42;
        if (alto > 0) {
            g.fill(wx, wy + wh - alto, wx + ww, wy + wh, 0xFFEAF4FF);
            g.fill(wx, wy + wh - alto, wx + ww, wy + wh - alto + 1, 0xFFFFFFFF);
            long ms = System.currentTimeMillis();
            for (int i = 0; i < 10; i++) {
                float fx = (i * 37 % ww);
                float fy = (float) ((ms / (60.0 + i * 7) + i * 13) % Math.max(1, alto));
                g.fill(wx + (int) fx, wy + wh - 1 - (int) fy, wx + (int) fx + 1, wy + wh - (int) fy, 0xFF9CC8E8);
            }
        }
        // brillo del vidrio
        g.fill(wx + 2, wy + 2, wx + 3, wy + wh - 4, 0x50FFFFFF);
        // la aguja del manómetro
        float a = Mth.lerp(k, -2.4F, 2.4F) - Mth.HALF_PI;
        int gx = x + 67, gy = y + 35;
        for (int i = 0; i < 5; i++) {
            int px = gx + Math.round(Mth.cos(a) * i), py = gy + Math.round(Mth.sin(a) * i);
            g.fill(px, py, px + 1, py + 1, 0xFFD01E28);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mx, int my) {
        super.renderLabels(g, mx, my);
        g.drawString(font, Component.translatable("navidad.tanque.tiros", tiros()), 97, 64, 0xFF3A5A7A, false);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        renderBackground(g);
        super.render(g, mx, my, pt);
        renderTooltip(g, mx, my);
    }
}
