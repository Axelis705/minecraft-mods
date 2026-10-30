package com.pooltable.client;

import com.pooltable.block.PoolTableBE;
import com.pooltable.item.PoolCueItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraftforge.client.gui.overlay.ForgeGui;

/** La barra de fuerza mientras se carga el tiro con el taco. */
public final class PoolHud {
    private PoolHud() {
    }

    public static void render(ForgeGui gui, GuiGraphics g, float partial, int w, int h) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null || mc.options.hideGui || !p.isUsingItem() || !(p.getUseItem().getItem() instanceof PoolCueItem)) {
            return;
        }
        PoolTableBE t = PoolTableBE.findNear(p.level(), p.position(), PoolCueItem.REACH);
        int cx = w / 2;
        int y = h - 62;
        if (t == null) {
            g.drawCenteredString(mc.font, Component.translatable("pooltable.hud.too_far"), cx, y, 0xFFFF6060);
            return;
        }
        float charge = Mth.clamp((p.getTicksUsingItem() + partial) / PoolCueItem.FULL_CHARGE, 0.0F, 1.0F);
        int bw = 100;
        int x0 = cx - bw / 2;
        g.fill(x0 - 2, y - 2, x0 + bw + 2, y + 8, 0xC0101418);
        g.fill(x0, y, x0 + bw, y + 6, 0xFF2A3038);
        int fill = Math.round(bw * charge);
        for (int i = 0; i < fill; i++) {
            float k = i / (float) bw;
            int r = (int) (60 + 195 * k);
            int gr = (int) (230 - 150 * k);
            g.fill(x0 + i, y, x0 + i + 1, y + 6, 0xFF000000 | (r << 16) | (gr << 8) | 70);
        }
        g.drawCenteredString(mc.font, Component.translatable("pooltable.hud.power"), cx, y - 12, 0xFFFFFFFF);
    }
}
