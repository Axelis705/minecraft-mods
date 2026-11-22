package com.mundosdeaxelis.client;

import com.mundosdeaxelis.encargo.Encargos;
import com.mundosdeaxelis.encargo.TablonMenu;
import com.mundosdeaxelis.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;

/**
 * Pantalla del Tablón de Encargos (etapa 280): tres notas clavadas con su encargo, su avance, lo que dan y un botón para
 * recogerlo; abajo, la gran recompensa por recoger las tres.
 */
public class TablonScreen extends AbstractContainerScreen<TablonMenu> {
    private static final int W = 236, H = 192;
    private static final int ACCENT = 0xE0A040, PAPER = 0xFF2A2016, PAPER_DONE = 0xFF2E3A1C;
    private static final int ROW_Y = 22, ROW_H = 40, BONUS_Y = 146;
    private static final int[] BTN = {170, 12, 58, 16};
    private int hover = -1;

    public TablonScreen(TablonMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        imageWidth = W;
        imageHeight = H;
    }

    @Override
    protected void init() {
        super.init();
        UiFx.open();
    }

    private static ItemStack icon(Encargos.Tipo t) {
        return switch (t) {
            case ENEMIGOS -> new ItemStack(Items.IRON_SWORD);
            case ELITES -> new ItemStack(Items.NETHER_STAR);
            case COFRES -> new ItemStack(ModItems.VAULT_CHEST.get());
            case MIMICO -> new ItemStack(ModItems.MIMICO_SPAWN_EGG.get());
            case ALTAR -> new ItemStack(ModItems.ALTAR_MALDITO.get());
            case ARROJADIZOS -> new ItemStack(ModItems.ARROJADIZOS.get(com.mundosdeaxelis.arrojadizo.Arrojadizos.Tipo.ARCANO).get());
            case GEMAS -> new ItemStack(ModItems.GEMA_VACIA.get());
            case FORJA -> new ItemStack(ModItems.GRAN_YUNQUE.get());
        };
    }

    private boolean canClaim(int k) {
        return k == Encargos.COUNT ? allClaimed() && !menu.bonusClaimed() : menu.progress(k) >= menu.need(k) && menu.need(k) > 0 && !menu.claimed(k);
    }

    private boolean allClaimed() {
        for (int k = 0; k < Encargos.COUNT; k++) {
            if (!menu.claimed(k)) {
                return false;
            }
        }
        return true;
    }

    private int btnY(int k) {
        return (k == Encargos.COUNT ? BONUS_Y : ROW_Y + k * ROW_H) + BTN[1];
    }

    private int buttonAt(double mx, double my) {
        for (int k = 0; k <= Encargos.COUNT; k++) {
            int x = leftPos + BTN[0], y = topPos + btnY(k);
            if (mx >= x && mx < x + BTN[2] && my >= y && my < y + BTN[3]) {
                return k;
            }
        }
        return -1;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int b) {
        int k = buttonAt(mx, my);
        if (b == 0 && k >= 0) {
            if (canClaim(k)) {
                UiFx.click();
                claim(k);
            } else {
                UiFx.error();
            }
            return true;
        }
        return super.mouseClicked(mx, my, b);
    }

    /** Recoge el encargo k (o la gran recompensa con k = 3); también lo usa la prueba con el cliente. */
    public void claim(int k) {
        if (minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, k);
        }
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        renderBackground(g);
        int now = buttonAt(mx, my);
        if (now != hover) {
            hover = now;
            if (now >= 0) {
                UiFx.hover();
            }
        }
        super.render(g, mx, my, pt);
        renderTooltip(g, mx, my);
    }

    @Override
    protected void renderBg(GuiGraphics g, float pt, int mx, int my) {
        int x = leftPos, y = topPos;
        PanelUi.frame(g, x, y, W, H, ACCENT);
        for (int k = 0; k < Encargos.COUNT; k++) {
            int ry = y + ROW_Y + k * ROW_H;
            boolean done = menu.progress(k) >= menu.need(k) && menu.need(k) > 0;
            g.fill(x + 6, ry, x + W - 6, ry + ROW_H - 4, done ? PAPER_DONE : PAPER);
            g.fill(x + 6, ry, x + W - 6, ry + 1, 0x55FFE0A0);
            // la chincheta
            g.fill(x + 10, ry + 2, x + 13, ry + 5, 0xFFC03030);
        }
        int by = y + BONUS_Y;
        g.fill(x + 6, by, x + W - 6, by + 28, allClaimed() ? 0xFF3A2A10 : 0xFF1E1810);
        g.fill(x + 6, by, x + W - 6, by + 1, 0x88FFC93C);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int amx, int amy) {
        Font f = font;
        int mx = amx - leftPos, my = amy - topPos;
        PanelUi.title(g, f, title, 8, 6, ACCENT);

        RandomSource r = RandomSource.create(0L);
        for (int k = 0; k < Encargos.COUNT; k++) {
            int ry = ROW_Y + k * ROW_H;
            Encargos.Tipo t = menu.tipo(k);
            int need = Math.max(1, menu.need(k)), prog = Math.min(need, menu.progress(k));
            boolean done = prog >= need, claimed = menu.claimed(k);
            g.renderItem(icon(t), 14, ry + 10);
            Component desc = Encargos.describe(t, need);
            float sc = Math.min(1.0F, 130.0F / Math.max(1, f.width(desc)));
            g.pose().pushPose();
            g.pose().translate(36, ry + 4, 0);
            g.pose().scale(sc, sc, 1.0F);
            g.drawString(f, done ? desc.copy().withStyle(ChatFormatting.GREEN) : desc, 0, 0, 0xFFF2E6C8, false);
            g.pose().popPose();
            // barra de avance
            int bw = 100;
            g.fill(36, ry + 16, 36 + bw, ry + 21, 0xFF120C08);
            g.fill(37, ry + 17, 37 + (bw - 2) * prog / need, ry + 20, done ? 0xFF6BE08A : 0xFFE0A040);
            g.drawString(f, prog + "/" + need, 36 + bw + 4, ry + 15, 0xFFE8DCC0, false);
            // lo que da
            int ix = 36;
            for (ItemStack it : Encargos.reward(t, menu.tier(), r)) {
                g.pose().pushPose();
                g.pose().translate(ix, ry + 23, 0);
                g.pose().scale(0.6F, 0.6F, 1.0F);
                g.renderItem(it, 0, 0);
                g.renderItemDecorations(f, it, 0, 0);
                g.pose().popPose();
                ix += 12;
            }
            g.drawString(f, "+" + Encargos.essence(menu.tier()) + " ◆", ix + 2, ry + 25, 0xFF7FE8FF, false);
            button(g, f, k, mx, my, claimed ? Component.translatable("gui.mundos_de_axelis.tablon.recogido")
                    : Component.translatable(done ? "gui.mundos_de_axelis.tablon.recoger" : "gui.mundos_de_axelis.tablon.en_curso"));
        }
        // gran recompensa
        g.drawString(f, Component.translatable("gui.mundos_de_axelis.tablon.gran"), 12, BONUS_Y + 4, 0xFFFFC93C, false);
        int ix = 12;
        for (ItemStack it : new ItemStack[]{new ItemStack(ModItems.HEART_FRAGMENT.get()), new ItemStack(ModItems.GEMA_VACIA.get()),
                new ItemStack(ModItems.ESSENCE_RUNES.get(Math.max(1, Math.min(5, menu.tier()))).get(), 2)}) {
            g.pose().pushPose();
            g.pose().translate(ix, BONUS_Y + 14, 0);
            g.pose().scale(0.6F, 0.6F, 1.0F);
            g.renderItem(it, 0, 0);
            g.renderItemDecorations(f, it, 0, 0);
            g.pose().popPose();
            ix += 12;
        }
        g.drawString(f, Component.translatable("gui.mundos_de_axelis.tablon.gran_detalle", 100 * menu.tier()), ix + 2, BONUS_Y + 16, 0xFFD8C8A0, false);
        button(g, f, Encargos.COUNT, mx, my, Component.translatable(menu.bonusClaimed() ? "gui.mundos_de_axelis.tablon.recogido"
                : "gui.mundos_de_axelis.tablon.recoger"));
        Component foot = Component.translatable("gui.mundos_de_axelis.tablon.tier", menu.tier()).append(" · ")
                .append(Component.translatable("gui.mundos_de_axelis.tablon.manana"));
        float fs = Math.min(1.0F, (W - 16.0F) / Math.max(1, f.width(foot)));
        g.pose().pushPose();
        g.pose().translate(8, H - 12, 0);
        g.pose().scale(fs, fs, 1.0F);
        g.drawString(f, foot, 0, 0, 0xFF8C7C60, false);
        g.pose().popPose();
    }

    private void button(GuiGraphics g, Font f, int k, int mx, int my, Component label) {
        int x = BTN[0], y = btnY(k);
        boolean over = mx >= x && mx < x + BTN[2] && my >= y && my < y + BTN[3];
        PanelUi.button(g, f, x, y, BTN[2], BTN[3], label, canClaim(k), over, k == Encargos.COUNT ? 0xFFC93C : 0x6BE08A);
    }

    public static List<String> debugRows(TablonMenu m) {
        java.util.ArrayList<String> out = new java.util.ArrayList<>();
        for (int k = 0; k < Encargos.COUNT; k++) {
            out.add(m.tipo(k).id + " " + m.progress(k) + "/" + m.need(k) + (m.claimed(k) ? " ✔" : ""));
        }
        return out;
    }
}
