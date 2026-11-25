package com.coleccionista.client;

import com.coleccionista.Coleccionista;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/**
 * (Cliente) Lo que ve una pieza al apretar E: no el inventario (ni su cuerpo), solo su bolsillo — la fila de abajo — en un marco oscuro,
 * con el logo del evento arriba: "EL COLECCIONISTA · por Axelis". Con clic se elige la casilla; con E o Esc se cierra.
 */
public class BolsilloScreen extends Screen {
    private static final String MOD = Coleccionista.MODID;
    private static final int CASILLA = 26, SEP = 4;
    private long abierto;

    public BolsilloScreen() {
        super(Component.translatable(MOD + ".bolsillo.titulo"));
    }

    @Override
    protected void init() {
        abierto = System.currentTimeMillis();
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.BUNDLE_DROP_CONTENTS, 0.6F, 0.6F));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private int x0() {
        return width / 2 - (9 * CASILLA + 8 * SEP) / 2;
    }

    private int y0() {
        return height / 2 - CASILLA / 2 + 10;
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        Minecraft mc = Minecraft.getInstance();
        Font f = font;
        float t = (System.currentTimeMillis() - abierto) / 1000F;
        float a = Math.min(1F, t / 0.25F);
        // fondo: negro con viñeta roja que late despacio
        g.fillGradient(0, 0, width, height, al(0xE0080406, a), al(0xF0000000, a));
        float late = 0.5F + 0.5F * (float) Math.sin(t * 2.2F);
        int borde = (int) (Math.min(width, height) * 0.18F);
        for (int i = 0; i < 8; i++) {
            int o = borde * (i + 1) / 8;
            int c = al(((int) (10 + 8 * late) << 24) | 0x5A0008, a);
            g.fill(0, 0, width, o / 3, c);
            g.fill(0, height - o / 3, width, height, c);
            g.fill(0, 0, o / 3, height, c);
            g.fill(width - o / 3, 0, width, height, c);
        }
        // el logo: EL COLECCIONISTA, grande y rojo, con un parpadeo de foco viejo
        String logo = Component.translatable(MOD + ".bolsillo.logo").getString();
        float esc = Math.min(3.2F, (width - 40) / (float) Math.max(1, f.width(logo)));
        boolean falla = ((int) (t * 7) % 23) == 0;
        int rojo = falla ? 0xFF6A0A10 : 0xFFB3121A;
        int ly = y0() - 80;
        g.pose().pushPose();
        g.pose().translate(width / 2F, ly, 0);
        g.pose().scale(esc, esc, 1F);
        int lw = f.width(logo);
        g.drawString(f, logo, -lw / 2 + 1, 1, al(0xFF000000, a), false);
        g.drawString(f, logo, -lw / 2, 0, al(rojo, a), false);
        g.pose().popPose();
        // "por Axelis" debajo, con dos rayas a los lados
        String por = Component.translatable(MOD + ".bolsillo.por").getString();
        int py = ly + (int) (10 * esc) + 4;
        g.pose().pushPose();
        g.pose().translate(width / 2F, py, 0);
        g.pose().scale(1.3F, 1.3F, 1F);
        int pw = f.width(por);
        g.drawString(f, por, -pw / 2, 0, al(0xFFD8CCB8, a), true);
        g.pose().popPose();
        int rx = (int) (pw * 1.3F / 2) + 8;
        g.fill(width / 2 - rx - 40, py + 6, width / 2 - rx, py + 7, al(0xFF6A2A30, a));
        g.fill(width / 2 + rx, py + 6, width / 2 + rx + 40, py + 7, al(0xFF6A2A30, a));
        // la fila de abajo: nueve casillas con marco de latón viejo; la elegida, en rojo
        Inventory inv = mc.player == null ? null : mc.player.getInventory();
        int x0 = x0(), y0 = y0();
        ItemStack sobre = ItemStack.EMPTY;
        for (int i = 0; i < 9; i++) {
            int x = x0 + i * (CASILLA + SEP);
            boolean sel = inv != null && inv.selected == i;
            boolean hover = mx >= x && mx < x + CASILLA && my >= y0 && my < y0 + CASILLA;
            int marco = sel ? 0xFFB3121A : hover ? 0xFFC8A060 : 0xFF6E5A3A;
            g.fill(x - 1, y0 - 1, x + CASILLA + 1, y0 + CASILLA + 1, al(0xFF000000, a));
            g.fill(x, y0, x + CASILLA, y0 + CASILLA, al(marco, a));
            g.fillGradient(x + 2, y0 + 2, x + CASILLA - 2, y0 + CASILLA - 2, al(0xFF1A1012, a), al(0xFF0A0606, a));
            // tornillos en las esquinas
            for (int[] c : new int[][] {{x + 1, y0 + 1}, {x + CASILLA - 2, y0 + 1}, {x + 1, y0 + CASILLA - 2}, {x + CASILLA - 2, y0 + CASILLA - 2}}) {
                g.fill(c[0], c[1], c[0] + 1, c[1] + 1, al(0xFF2A2018, a));
            }
            if (inv != null) {
                ItemStack st = inv.getItem(i);
                if (!st.isEmpty()) {
                    g.pose().pushPose();
                    g.pose().translate(x + CASILLA / 2F - 10, y0 + CASILLA / 2F - 10, 0);
                    g.pose().scale(1.25F, 1.25F, 1F);
                    g.renderItem(st, 0, 0);
                    g.renderItemDecorations(f, st, 0, 0);
                    g.pose().popPose();
                    if (hover) {
                        sobre = st;
                    }
                }
            }
            // el número de la casilla, chiquito abajo
            g.drawString(f, String.valueOf(i + 1), x + CASILLA / 2 - 2, y0 + CASILLA + 4, al(sel ? 0xFFE04040 : 0xFF6E6258, a), false);
        }
        // abajo: cómo cerrar
        String cerrar = Component.translatable(MOD + ".bolsillo.cerrar").getString();
        g.drawString(f, cerrar, width / 2 - f.width(cerrar) / 2, y0 + CASILLA + 26, al(0xFF8A7E74, a), false);
        if (!sobre.isEmpty()) {
            g.renderTooltip(f, sobre, mx, my);
        }
    }

    private static int al(int c, float a) {
        int alfa = (int) (((c >>> 24) & 255) * a);
        return alfa << 24 | (c & 0xFFFFFF);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int b) {
        Minecraft mc = Minecraft.getInstance();
        int x0 = x0(), y0 = y0();
        for (int i = 0; i < 9; i++) {
            int x = x0 + i * (CASILLA + SEP);
            if (mx >= x && mx < x + CASILLA && my >= y0 && my < y0 + CASILLA && mc.player != null) {
                mc.player.getInventory().selected = i;
                mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK.value(), 1.2F, 0.4F));
                return true;
            }
        }
        return super.mouseClicked(mx, my, b);
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.keyInventory.matches(key, scan)) {
            onClose();
            return true;
        }
        return super.keyPressed(key, scan, mods);
    }
}
