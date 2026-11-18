package com.mundosdeaxelis.client;

import com.mundosdeaxelis.MundosDeAxelis;
import com.mundosdeaxelis.crate.Tier;
import com.mundosdeaxelis.crate.Tiered;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderTooltipEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Piel nueva para la descripción de los objetos del mod: fondo oscuro teñido con el color de la rareza, borde con degradado,
 * gemas en las esquinas, una línea decorativa bajo el título y, en lo legendario y lo mítico, un destello que recorre el borde.
 * Solo afecta a objetos de este mod; los de Minecraft siguen con su descripción normal.
 */
@Mod.EventBusSubscriber(modid = MundosDeAxelis.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class ClientTooltips {
    private ClientTooltips() {
    }

    /** Color de los objetos sin rareza (decoración, comida, bloques...): rosa dorado. */
    private static final int DEFAULT = 0xE07AAE;

    /** Rectángulo y color del último tooltip del cuadro actual (null si no hay). */
    private static boolean has;
    private static int tx, ty, tw, th, tcolor, trank;
    private static boolean tTitle;

    private static boolean ours(ItemStack st) {
        return !st.isEmpty() && MundosDeAxelis.MODID.equals(BuiltInRegistries.ITEM.getKey(st.getItem()).getNamespace());
    }

    private static int mix(int a, int b, float t) {
        int r = Math.round(((a >> 16) & 255) * (1 - t) + ((b >> 16) & 255) * t);
        int g = Math.round(((a >> 8) & 255) * (1 - t) + ((b >> 8) & 255) * t);
        int bl = Math.round((a & 255) * (1 - t) + (b & 255) * t);
        return (r << 16) | (g << 8) | bl;
    }

    @SubscribeEvent
    public static void onColor(RenderTooltipEvent.Color e) {
        ItemStack st = e.getItemStack();
        if (!ours(st)) {
            return;
        }
        int rank = -1;
        int c = DEFAULT;
        if (st.getItem() instanceof Tiered t) {
            Tier tier = com.mundosdeaxelis.crate.Tiers.shown(st.getItem());
            c = tier.color;
            rank = tier.ordinal();
        }
        float time = (System.currentTimeMillis() % 4000L) / 4000.0F;
        int bright = mix(c, 0xFFFFFF, 0.35F);
        int dark = mix(c, 0x1A0E2A, 0.60F);
        if (rank >= Tier.LEGENDARY.ordinal()) {
            // el borde respira despacio
            float k = 0.5F + 0.5F * (float) Math.sin(time * Math.PI * 2);
            bright = mix(bright, 0xFFFFFF, 0.25F * k);
            dark = mix(dark, c, 0.35F * k);
        }
        e.setBackgroundStart(0xF0000000 | mix(0x130C20, c, 0.10F));
        e.setBackgroundEnd(0xF0000000 | mix(0x0A0614, c, 0.20F));
        e.setBorderStart(0xFF000000 | bright);
        e.setBorderEnd(0xE0000000 | dark);

        int w = 0;
        int h = e.getComponents().size() == 1 ? -2 : 0;
        for (ClientTooltipComponent comp : e.getComponents()) {
            w = Math.max(w, comp.getWidth(e.getFont()));
            h += comp.getHeight();
        }
        has = true;
        tx = e.getX();
        ty = e.getY();
        tw = w;
        th = h;
        tcolor = c;
        trank = rank;
        tTitle = e.getComponents().size() > 1;
    }

    /** Línea de "vinculado a ..." en los objetos atados a un alma. */
    @SubscribeEvent
    public static void onTooltip(net.minecraftforge.event.entity.player.ItemTooltipEvent e) {
        ItemStack st = e.getItemStack();
        if (com.mundosdeaxelis.table.Bound.isBound(st) && !e.getToolTip().isEmpty()) {
            e.getToolTip().add(1, net.minecraft.network.chat.Component.translatable("tooltip.mundos_de_axelis.bound",
                    com.mundosdeaxelis.table.Bound.ownerName(st)).withStyle(net.minecraft.ChatFormatting.AQUA));
        }
    }

    @SubscribeEvent
    public static void onScreenPre(ScreenEvent.Render.Pre e) {
        has = false;
    }

    /** Los adornos se dibujan al final del cuadro, por encima del tooltip (que ya se pintó dentro de la pantalla). */
    @SubscribeEvent
    public static void onScreenPost(ScreenEvent.Render.Post e) {
        if (!has) {
            return;
        }
        has = false;
        GuiGraphics g = e.getGuiGraphics();
        g.pose().pushPose();
        g.pose().translate(0.0F, 0.0F, 450.0F);
        int bright = 0xFF000000 | mix(tcolor, 0xFFFFFF, 0.45F);
        int x0 = tx - 3, y0 = ty - 3, x1 = tx + tw + 3, y1 = ty + th + 3;

        // línea decorativa bajo el título (se desvanece hacia los lados)
        if (tTitle && tw > 12) {
            int ly = ty + 10;
            for (int px = 0; px < tw; px += 2) {
                float u = tw <= 1 ? 0 : (float) px / (tw - 1);
                float f = 1.0F - (float) Math.pow(Math.abs(2 * u - 1), 2);
                int a = Math.round(0xA0 * f);
                if (a > 4) {
                    g.fill(tx + px, ly, Math.min(tx + px + 2, tx + tw), ly + 1, (a << 24) | mix(tcolor, 0xFFFFFF, 0.25F));
                }
            }
        }

        // gemas en las cuatro esquinas
        gem(g, x0, y0, bright);
        gem(g, x1 - 1, y0, bright);
        gem(g, x0, y1 - 1, bright);
        gem(g, x1 - 1, y1 - 1, bright);

        // destello que recorre el borde de arriba (legendario y mítico)
        if (trank >= Tier.LEGENDARY.ordinal()) {
            long ms = System.currentTimeMillis();
            int span = Math.max(8, x1 - x0 - 10);
            int sx = x0 + 5 + (int) ((ms / (trank >= Tier.MYTHIC.ordinal() ? 18 : 28)) % span);
            for (int i = -5; i <= 5; i++) {
                int a = Math.round(0xE0 * (1.0F - Math.abs(i) / 6.0F));
                int px = sx + i;
                if (px > x0 + 1 && px < x1 - 2) {
                    g.fill(px, y0, px + 1, y0 + 1, (a << 24) | 0xFFFFFF);
                }
            }
        }
        g.pose().popPose();
    }

    /** Un rombito de 3x3 con el centro más claro. */
    private static void gem(GuiGraphics g, int cx, int cy, int color) {
        g.fill(cx, cy - 1, cx + 1, cy + 2, color);
        g.fill(cx - 1, cy, cx + 2, cy + 1, color);
        g.fill(cx, cy, cx + 1, cy + 1, 0xFFFFFFFF);
    }
}
