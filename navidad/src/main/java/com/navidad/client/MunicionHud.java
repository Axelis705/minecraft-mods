package com.navidad.client;

import com.axeliscore.fx.client.Hud;
import com.axeliscore.plat.client.G;
import com.mojang.math.Axis;
import com.navidad.nieve.Lanzanieves;
import com.navidad.nieve.Municion;
import com.navidad.nieve.TanqueNieve;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * (Cliente) Con el lanzanieves en la mano: junto a la mira, cuántos tiros te quedan (con una barrita del tanque), el aro que se llena
 * al cargar la bola gigante y un aviso cuando estás aspirando nieve. Si no traes el tanque, te recuerda que te lo pongas.
 */
public final class MunicionHud {
    private static final ItemStack BOLA = new ItemStack(Items.SNOWBALL);

    private MunicionHud() {
    }

    public static void init() {
        Hud.add(MunicionHud::render);
    }

    static void render(G gg, int w, int h) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui || !(mc.player.getMainHandItem().getItem() instanceof Lanzanieves)) {
            return;
        }
        GuiGraphics g = gg.raw;
        var p = mc.player;
        var f = mc.font;
        int cx = w / 2, cy = h / 2;
        ItemStack t = Municion.tanque(p);
        int total = Municion.total(p);
        // tiros que quedan
        int x = cx + 14, y = cy + 8;
        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        g.pose().scale(0.6F, 0.6F, 1);
        g.renderItem(BOLA, 0, 0);
        g.pose().popPose();
        String n = p.getAbilities().instabuild ? "∞" : String.valueOf(total);
        g.drawString(f, n, x + 11, y + 1, total > 0 || p.getAbilities().instabuild ? 0xFFE8F6FF : 0xFFFF7070, true);
        if (!t.isEmpty()) {
            float k = Mth.clamp(TanqueNieve.carga(t) / (float) TanqueNieve.LLENO, 0, 1);
            g.fill(x, y + 11, x + 34, y + 14, 0x80203040);
            g.fill(x, y + 11, x + Math.round(34 * k), y + 14, 0xFF9CD8FF);
        } else {
            g.pose().pushPose();
            g.pose().translate(x, y + 11, 0);
            g.pose().scale(0.6F, 0.6F, 1);
            g.drawString(f, Component.translatable("navidad.lanzanieves.hud_sin_tanque"), 0, 0, 0xFFB0C8D8, true);
            g.pose().popPose();
        }
        if (!p.isUsingItem() || !(p.getUseItem().getItem() instanceof Lanzanieves)) {
            return;
        }
        int usado = p.getTicksUsingItem();
        if (p.isShiftKeyDown()) {
            // el aro de la bola gigante: 16 puntitos que se van llenando
            float k = Mth.clamp((usado + mc.getFrameTime()) / Lanzanieves.CARGA, 0, 1);
            int puntos = 16;
            for (int i = 0; i < puntos; i++) {
                boolean lleno = i < Math.round(k * puntos);
                float a = i / (float) puntos * Mth.TWO_PI;
                g.pose().pushPose();
                g.pose().translate(cx + Mth.sin(a) * 11, cy - Mth.cos(a) * 11, 0);
                g.pose().mulPose(Axis.ZP.rotation(a));
                int col = lleno ? (k >= 1 ? 0xFFFFFFFF : 0xFF9CD8FF) : 0x60FFFFFF;
                g.fill(-1, -1, 1, 1, col);
                g.pose().popPose();
            }
            if (k >= 1) {
                Component c = Component.translatable("navidad.lanzanieves.hud_lista");
                g.drawString(f, c, cx - f.width(c) / 2, cy + 18, 0xFFFFFFFF, true);
            }
        } else if (Lanzanieves.aspirando(p)) {
            Component c = Component.translatable("navidad.lanzanieves.hud_aspira");
            int a = (int) (170 + 85 * Mth.sin((p.tickCount + mc.getFrameTime()) * 0.5F));
            g.drawString(f, c, cx - f.width(c) / 2, cy + 18, a << 24 | 0xBFE8FF, true);
        }
    }
}
