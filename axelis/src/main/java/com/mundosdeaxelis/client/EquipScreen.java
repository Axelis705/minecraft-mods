package com.mundosdeaxelis.client;

import com.mundosdeaxelis.relic.EquipMenu;
import com.mundosdeaxelis.relic.Equipment;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

import java.util.List;

/** Pantalla del menú de equipo (se abre con G): mascotas arriba, reliquias en medio y el inventario debajo. */
public class EquipScreen extends AbstractContainerScreen<EquipMenu> {
    private static final int PET = 0xFFD27F, RELIC = 0x7FD2FF, CAPE = 0xFF8FD0;

    public EquipScreen(EquipMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 176;
        this.imageHeight = 238;
        this.titleLabelY = 6;
        this.inventoryLabelY = EquipMenu.INV_Y - 11;
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (EquipClient.MENU.matches(key, scan)) {
            onClose();
            return true;
        }
        return super.keyPressed(key, scan, mods);
    }

    @Override
    protected void init() {
        super.init();
        UiFx.open();
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        renderBackground(g);
        super.render(g, mx, my, pt);
        // ranuras bloqueadas: explican cómo se desbloquean
        for (int i = menu.petN; i < Equipment.MAX_PETS; i++) {
            lockedTip(g, mx, my, leftPos + EquipMenu.PET_X + i * EquipMenu.SLOT_STEP, topPos + EquipMenu.PET_Y);
        }
        for (int i = menu.relicN; i < Equipment.MAX_RELICS; i++) {
            lockedTip(g, mx, my, leftPos + EquipMenu.RELIC_X + i * EquipMenu.SLOT_STEP, topPos + EquipMenu.RELIC_Y);
        }
    }

    private void lockedTip(GuiGraphics g, int mx, int my, int x, int y) {
        if (mx >= x - 1 && mx < x + 17 && my >= y - 1 && my < y + 17) {
            g.renderTooltip(font, Component.translatable("gui.mundos_de_axelis.equip.locked"), mx, my);
        }
    }

    @Override
    protected void renderBg(GuiGraphics g, float pt, int mx, int my) {
        int x = leftPos, y = topPos, w = imageWidth, h = imageHeight;
        // panel con marco adornado
        PanelUi.frame(g, x, y, w, h, 0x8A6AD0);
        PanelUi.divider(g, x + 6, y + 19, w - 12, 0x8A6AD0);
        // rótulos de sección (placa de color detrás del texto)
        PanelUi.sectionBar(g, x + 5, y + EquipMenu.PET_Y - 13, w - 10, PET);
        PanelUi.sectionBar(g, x + 5, y + EquipMenu.RELIC_Y - 13, w - 10, RELIC);
        PanelUi.sectionBar(g, x + EquipMenu.CAPE_X - 3, y + EquipMenu.CAPE_Y - 13, w - EquipMenu.CAPE_X - 2, CAPE);
        PanelUi.sectionBar(g, x + 5, y + EquipMenu.INV_Y - 12, w - 10, 0xB8AAD8);
        // ranuras de mascotas y reliquias
        for (int i = 0; i < Equipment.MAX_PETS; i++) {
            cell(g, x + EquipMenu.PET_X + i * EquipMenu.SLOT_STEP, y + EquipMenu.PET_Y, PET, i >= menu.petN);
        }
        for (int i = 0; i < Equipment.MAX_RELICS; i++) {
            cell(g, x + EquipMenu.RELIC_X + i * EquipMenu.SLOT_STEP, y + EquipMenu.RELIC_Y, RELIC, i >= menu.relicN);
        }
        cell(g, x + EquipMenu.CAPE_X, y + EquipMenu.CAPE_Y, CAPE, false);
        // inventario
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 9; c++) {
                plain(g, x + 8 + c * 18, y + EquipMenu.INV_Y + r * 18);
            }
        }
        for (int c = 0; c < 9; c++) {
            plain(g, x + 8 + c * 18, y + EquipMenu.INV_Y + 58);
        }
    }

    private static void plain(GuiGraphics g, int x, int y) {
        PanelUi.plain(g, x, y);
    }

    private void cell(GuiGraphics g, int x, int y, int accent, boolean locked) {
        PanelUi.slot(g, x, y, accent, locked);
        if (locked) {
            return;
        }
        // si hay un objeto, un halo del color de su rareza que late despacio
        for (net.minecraft.world.inventory.Slot sl : menu.slots) {
            if (sl.x == x - leftPos && sl.y == y - topPos) {
                net.minecraft.world.item.ItemStack st = sl.getItem();
                if (!st.isEmpty() && com.mundosdeaxelis.crate.Tiers.of(st) != null) {
                    int col = com.mundosdeaxelis.crate.Tiers.shown(st).color;
                    double t = UiFx.time();
                    int a1 = (int) (0x50 + 0x38 * Math.sin(t * 2.2D + x * 0.1D));
                    g.fill(x - 2, y - 2, x + 18, y - 1, (a1 << 24) | col);
                    g.fill(x - 2, y + 17, x + 18, y + 18, (a1 << 24) | col);
                    g.fill(x - 2, y - 1, x - 1, y + 17, (a1 << 24) | col);
                    g.fill(x + 17, y - 1, x + 18, y + 17, (a1 << 24) | col);
                    g.fillGradient(x, y, x + 16, y + 16, ((a1 / 3) << 24) | col, (0x00 << 24) | col);
                }
                break;
            }
        }
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mx, int my) {
        Font f = font;
        g.drawString(f, title, (imageWidth - f.width(title)) / 2, titleLabelY, 0xFFFFFF, true);
        String relicKey = EquipClient.RELIC.getTranslatedKeyMessage().getString();
        g.drawString(f, Component.translatable("gui.mundos_de_axelis.equip.pets"), 9, EquipMenu.PET_Y - 12, 0xFF000000 | PET, true);
        g.drawString(f, Component.translatable("gui.mundos_de_axelis.equip.cape"), EquipMenu.CAPE_X, EquipMenu.CAPE_Y - 12, 0xFF000000 | CAPE, true);
        g.drawString(f, Component.translatable("gui.mundos_de_axelis.equip.relics", relicKey), 9, EquipMenu.RELIC_Y - 12, 0xFF000000 | RELIC, true);
        List<FormattedCharSequence> hint = f.split(FormattedText.of(Component.translatable("gui.mundos_de_axelis.equip.hint").getString()), imageWidth - 16);
        int y = EquipMenu.RELIC_Y + 22;
        for (FormattedCharSequence line : hint) {
            g.drawString(f, line, 8, y, 0xFF9A8CC0, false);
            y += 9;
        }
        g.drawString(f, Component.translatable("gui.mundos_de_axelis.equip.cape_hint"), EquipMenu.CAPE_X + 26, EquipMenu.CAPE_Y + 4, 0xFF9A8CC0, false);
        g.drawString(f, playerInventoryTitle, 9, inventoryLabelY, 0xFFB8AAD8, false);
    }
}
