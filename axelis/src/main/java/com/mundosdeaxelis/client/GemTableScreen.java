package com.mundosdeaxelis.client;

import com.mundosdeaxelis.gema.GemTableMenu;
import com.mundosdeaxelis.gema.GemaItems;
import com.mundosdeaxelis.gema.Gemas;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/** Pantalla de la Mesa de Gemas (etapa 271): a la izquierda se infunde una gema vacía; a la derecha se engarza (o se quita) en un arma. */
public class GemTableScreen extends AbstractContainerScreen<GemTableMenu> {
    private static final int W = 214, H = 200;
    private static final int ACCENT = 0xC060FF, GOLD = 0xFFE9A8;
    private int hoverBtn = -1;
    /** Etapa 279: la guía de las 8 gemas (botón «?»). */
    private boolean guide;
    private static final int[] HELP = {W - 20, 4, 12, 11};

    public GemTableScreen(GemTableMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        imageWidth = W;
        imageHeight = H;
        titleLabelY = 6;
        inventoryLabelY = 107;
    }

    @Override
    protected void init() {
        super.init();
        UiFx.open();
    }

    private ItemStack slot(int i) {
        return menu.slots.get(i).getItem();
    }

    private boolean canInfuse() {
        return slot(GemTableMenu.S_VACIA).getItem() instanceof GemaItems.Vacia && slot(GemTableMenu.S_ESENCIA).getItem() instanceof GemaItems.Esencia
                && TableClient.essence >= GemTableMenu.infuseCost(Gemas.pureza(slot(GemTableMenu.S_VACIA)));
    }

    private boolean canCombine() {
        ItemStack v = slot(GemTableMenu.S_VACIA);
        return GemTableMenu.combinable(v) && TableClient.essence >= GemTableMenu.combineCost(Gemas.pureza(v));
    }

    private boolean canSocket() {
        return Gemas.admite(slot(GemTableMenu.S_ARMA)) && slot(GemTableMenu.S_GEMA).getItem() instanceof GemaItems.Gema;
    }

    private boolean canRemove() {
        return Gemas.gemaDe(slot(GemTableMenu.S_ARMA)) != null && slot(GemTableMenu.S_GEMA).isEmpty() && TableClient.essence >= GemTableMenu.REMOVE_COST;
    }

    // botones: x, y, w, h (relativos a la ventana)
    private static final int[][] BTN = {{8, 58, 47, 14}, {114, 58, 50, 14}, {168, 58, 38, 14}, {57, 58, 47, 14}};

    /** Abre o cierra la guía (lo usa la prueba con el cliente). */
    public void toggleGuide() {
        guide = !guide;
    }

    public boolean guideOpen() {
        return guide;
    }

    private boolean overHelp(double mx, double my) {
        return mx >= leftPos + HELP[0] && mx < leftPos + HELP[0] + HELP[2] && my >= topPos + HELP[1] && my < topPos + HELP[1] + HELP[3];
    }

    @Override
    public boolean mouseClicked(double mx, double my, int b) {
        if (b == 0 && overHelp(mx, my)) {
            guide = !guide;
            UiFx.click();
            return true;
        }
        if (guide) {
            guide = false; // cualquier clic cierra la guía
            return true;
        }
        if (b == 0) {
            for (int i = 0; i < BTN.length; i++) {
                int[] r = BTN[i];
                if (mx >= leftPos + r[0] && mx < leftPos + r[0] + r[2] && my >= topPos + r[1] && my < topPos + r[1] + r[3]) {
                    boolean ok = i == 0 ? canInfuse() : i == 1 ? canSocket() : i == 2 ? canRemove() : canCombine();
                    if (ok) {
                        UiFx.click();
                        if (minecraft != null && minecraft.gameMode != null) {
                            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, i);
                        }
                    } else {
                        UiFx.error();
                    }
                    return true;
                }
            }
        }
        return super.mouseClicked(mx, my, b);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        renderBackground(g);
        int now = -1;
        for (int i = 0; i < BTN.length; i++) {
            int[] r = BTN[i];
            if (mx >= leftPos + r[0] && mx < leftPos + r[0] + r[2] && my >= topPos + r[1] && my < topPos + r[1] + r[3]) {
                now = i;
            }
        }
        if (now != hoverBtn) {
            hoverBtn = now;
            if (now >= 0) {
                UiFx.hover();
            }
        }
        super.render(g, mx, my, pt);
        if (guide) {
            renderGuide(g);
        } else {
            renderTooltip(g, mx, my);
        }
    }

    /** La guía: cada gema con lo que hace en un arma y en la armadura. */
    private void renderGuide(GuiGraphics g) {
        Font f = font;
        int x = leftPos + 4, y = topPos + 18, w = W - 8, h = H - 22;
        g.pose().pushPose();
        g.pose().translate(0, 0, 400);
        g.fill(x, y, x + w, y + h, 0xF0120A22);
        PanelUi.frame(g, x, y, w, h, ACCENT);
        g.drawString(f, Component.translatable("gui.mundos_de_axelis.gemas.guia"), x + 6, y + 5, 0xFFFFE9A8, false);
        int row = 0;
        for (Gemas.Elemento el : Gemas.Elemento.values()) {
            int ry = y + 16 + row * 20;
            g.renderItem(Gemas.conPureza(new ItemStack(com.mundosdeaxelis.registry.ModItems.GEMAS.get(el).get()), 3), x + 5, ry);
            g.pose().pushPose();
            g.pose().translate(x + 24, ry - 1, 0);
            g.pose().scale(0.8F, 0.8F, 1.0F);
            g.drawString(f, Component.translatable("item.mundos_de_axelis.gema_" + el.id).withStyle(el.style()), 0, 0, 0xFFFFFFFF, false);
            g.drawString(f, Component.translatable("gema.mundos_de_axelis.corto." + el.id + ".arma"), 0, 8, 0xFFE8DCFF, false);
            g.drawString(f, Component.translatable("gema.mundos_de_axelis.corto." + el.id + ".armadura"), 0, 16, 0xFFB0A4D0, false);
            g.pose().popPose();
            row++;
        }
        g.pose().popPose();
    }

    @Override
    protected void renderBg(GuiGraphics g, float pt, int mx, int my) {
        int x = leftPos, y = topPos;
        PanelUi.frame(g, x, y, W, H, ACCENT);
        PanelUi.divider(g, x + 6, y + 103, W - 12, ACCENT);
        // separador vertical entre las dos zonas
        g.fillGradient(x + 106, y + 20, x + 107, y + 99, 0x00C060FF, 0xAAC060FF);
        Gemas.Elemento el = slot(GemTableMenu.S_ESENCIA).getItem() instanceof GemaItems.Esencia es ? es.el : null;
        int infAccent = el != null ? el.color : ACCENT;
        PanelUi.slot(g, x + 26, y + 36, infAccent, false);
        PanelUi.slot(g, x + 62, y + 36, infAccent, false);
        Gemas.Elemento wEl = Gemas.gemaDe(slot(GemTableMenu.S_ARMA));
        PanelUi.slot(g, x + 134, y + 36, wEl != null ? wEl.color : GOLD, false);
        PanelUi.slot(g, x + 170, y + 36, slot(GemTableMenu.S_GEMA).getItem() instanceof GemaItems.Gema gm ? gm.el.color : GOLD, false);
        // brillo que late alrededor de la gema vacía cuando se puede infundir
        if (canInfuse()) {
            int a = (int) (0x50 + 0x40 * Math.sin(UiFx.time() * 4.0D));
            g.fill(x + 24, y + 34, x + 44, y + 54, (a << 24) | (infAccent & 0xFFFFFF));
            PanelUi.slot(g, x + 26, y + 36, infAccent, false);
        }
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 9; c++) {
                PanelUi.plain(g, x + 26 + c * 18, y + 118 + r * 18);
            }
        }
        for (int c = 0; c < 9; c++) {
            PanelUi.plain(g, x + 26 + c * 18, y + 176);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics g, int amx, int amy) {
        Font f = font;
        int mx = amx - leftPos, my = amy - topPos;
        PanelUi.title(g, f, title, 8, titleLabelY, ACCENT);
        String es = "◆ " + TableClient.essence;
        g.drawString(f, es, W - 24 - f.width(es), titleLabelY, 0xFF7FE8FF, true);
        boolean oh = overHelp(amx, amy);
        g.fill(HELP[0], HELP[1], HELP[0] + HELP[2], HELP[1] + HELP[3], oh || guide ? 0xFFC060FF : 0xFF5A3A8A);
        g.fill(HELP[0] + 1, HELP[1] + 1, HELP[0] + HELP[2] - 1, HELP[1] + HELP[3] - 1, 0xFF1A1030);
        g.drawString(f, "?", HELP[0] + 4, HELP[1] + 2, 0xFFFFE9A8, false);
        PanelUi.sectionBar(g, 8, 20, 94, ACCENT);
        g.drawString(f, Component.translatable("gui.mundos_de_axelis.gemas.infundir_titulo"), 13, 21, 0xFFE8DCFF, false);
        PanelUi.sectionBar(g, 112, 20, 94, GOLD);
        g.drawString(f, Component.translatable("gui.mundos_de_axelis.gemas.engarzar_titulo"), 117, 21, 0xFFFFE9A8, false);
        g.drawString(f, "+", 50, 40, 0xFFE8DCFF, true);
        g.drawString(f, "◆", 156, 40, 0xFFFFE9A8, true);
        String[] keys = {"infundir", "engarzar", "quitar", "combinar"};
        boolean[] ok = {canInfuse(), canSocket(), canRemove(), canCombine()};
        for (int i = 0; i < BTN.length; i++) {
            int[] r = BTN[i];
            boolean over = mx >= r[0] && mx < r[0] + r[2] && my >= r[1] && my < r[1] + r[3];
            PanelUi.button(g, f, r[0], r[1], r[2], r[3], Component.translatable("gui.mundos_de_axelis.gemas." + keys[i]), ok[i], over, i == 0 || i == 3 ? ACCENT : GOLD);
        }
        // texto de ayuda de cada zona
        ItemStack v = slot(GemTableMenu.S_VACIA), e = slot(GemTableMenu.S_ESENCIA);
        Component left;
        if (v.getItem() instanceof GemaItems.Vacia && e.getItem() instanceof GemaItems.Esencia esn) {
            int p = Gemas.pureza(v);
            left = Component.translatable("gui.mundos_de_axelis.gemas.resultado", Component.translatable("item.mundos_de_axelis.gema_" + esn.el.id)
                    .append(" (").append(Gemas.purezaName(p)).append(")").withStyle(esn.el.style()), GemTableMenu.infuseCost(p));
        } else if (GemTableMenu.combinable(v)) {
            int p = Gemas.pureza(v);
            left = Component.translatable("gui.mundos_de_axelis.gemas.combinar_resultado", Gemas.conPureza(new ItemStack(v.getItem()), p + 1).getHoverName()
                    .copy().withStyle(v.getItem() instanceof GemaItems.Gema gm ? gm.el.style() : net.minecraft.network.chat.Style.EMPTY.withColor(0xD8CCEE)),
                    GemTableMenu.combineCost(p));
        } else {
            left = Component.translatable("gui.mundos_de_axelis.gemas.infundir_ayuda").withStyle(ChatFormatting.GRAY);
        }
        lines(g, f, left, 9, 76, 94);
        ItemStack w = slot(GemTableMenu.S_ARMA);
        Gemas.Elemento wEl = Gemas.gemaDe(w);
        Component right;
        if (wEl != null) {
            right = Component.translatable("gui.mundos_de_axelis.gemas.actual", Component.translatable("item.mundos_de_axelis.gema_" + wEl.id)
                    .append(" (").append(Gemas.purezaName(Gemas.purezaDe(w))).append(")").withStyle(wEl.style()), GemTableMenu.REMOVE_COST);
        } else if (!w.isEmpty()) {
            right = Component.translatable("gui.mundos_de_axelis.gemas.sin_gema").withStyle(ChatFormatting.GRAY);
        } else {
            right = Component.translatable("gui.mundos_de_axelis.gemas.engarzar_ayuda").withStyle(ChatFormatting.GRAY);
        }
        lines(g, f, right, 113, 76, 94);
        g.drawString(f, playerInventoryTitle, 26, inventoryLabelY, 0xFF9A8CC0, false);
    }

    private static void lines(GuiGraphics g, Font f, Component c, int x, int y, int w) {
        List<FormattedCharSequence> ls = f.split(c, (int) (w / 0.85F));
        for (int i = 0; i < Math.min(3, ls.size()); i++) {
            g.pose().pushPose();
            g.pose().translate(x, y + i * 8, 0);
            g.pose().scale(0.85F, 0.85F, 1.0F);
            g.drawString(f, ls.get(i), 0, 0, 0xFFE8DCFF, false);
            g.pose().popPose();
        }
    }

    public static boolean open() {
        return Minecraft.getInstance().screen instanceof GemTableScreen;
    }
}
