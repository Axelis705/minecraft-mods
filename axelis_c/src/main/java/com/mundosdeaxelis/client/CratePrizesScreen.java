package com.mundosdeaxelis.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mundosdeaxelis.crate.CrateKind;
import com.mundosdeaxelis.crate.CratePools;
import com.mundosdeaxelis.crate.Pity;
import com.mundosdeaxelis.crate.Tier;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Catálogo de una caja (etapa 138): los premios se agrupan en pestañas por tipo (Minecraft, armas y herramientas, armadura,
 * decoración, capas, mascotas). Arriba, una barra muestra cuánto de la caja ocupa cada tipo; cada fila muestra el porcentaje
 * real, "1 de N" y una barrita. Se abre al hacer clic en la caja sin llave (o agachado).
 */
public class CratePrizesScreen extends Screen {
    private static final int ROW = 24;
    /** Pestañas: 0 todo, 1 Minecraft, 2 armas y herramientas, 3 armadura, 4 decoración, 5 capas, 6 mascotas. */
    private static final String[] TAB_KEY = {"all", "vanilla", "gear", "armor", "decor", "capes", "pets"};
    private static final int[] TAB_COLOR = {0, 0x7FC46B, 0xE8A24A, 0x6FA8F0, 0xD98FE0, 0xF0657A, 0xFFD35C};

    private final CrateKind kind;
    private final List<CratePools.Chance> all;
    private final double[] tierPct;
    private final double[] tabPct = new double[TAB_KEY.length];
    private List<CratePools.Chance> rows;
    private int tab;
    private float scroll;
    private float scrollTarget;

    private int left, top, pw, ph, listTop, listBottom, tabTop, tabH;

    public CratePrizesScreen(CrateKind kind) {
        super(Component.translatable(kind.titleKey()));
        this.kind = kind;
        this.all = CratePools.chances(kind);
        this.tierPct = CratePools.tierChances(kind);
        for (CratePools.Chance c : all) {
            tabPct[tabOf(c.group())] += c.percent();
            tabPct[0] += c.percent();
        }
        this.rows = filtered();
    }

    private static int tabOf(String group) {
        if (group == null) {
            return 1;
        }
        if (group.startsWith("gear")) {
            return 2;
        }
        if (group.startsWith("armor")) {
            return 3;
        }
        if (group.startsWith("decor")) {
            return 4;
        }
        if (group.startsWith("cape")) {
            return 5;
        }
        if (group.startsWith("pet")) {
            return 6;
        }
        return 1;
    }

    private List<CratePools.Chance> filtered() {
        List<CratePools.Chance> out = new ArrayList<>();
        for (CratePools.Chance c : all) {
            if (tab == 0 || tabOf(c.group()) == tab) {
                out.add(c);
            }
        }
        // lo más probable arriba; lo más difícil, al final
        out.sort((a, b) -> Double.compare(b.percent(), a.percent()));
        return out;
    }

    private int tabColor(int i) {
        return i == 0 ? kind.color : TAB_COLOR[i];
    }

    @Override
    protected void init() {
        UiFx.open();
        pw = Math.min(this.width - 16, 380);
        ph = Math.min(this.height - 12, 280);
        left = (this.width - pw) / 2;
        top = (this.height - ph) / 2;
        tabTop = top + 46;
        tabH = 24;
        listTop = tabTop + tabH + 6;
        listBottom = top + ph - 32;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private float maxScroll() {
        return Math.max(0, rows.size() * ROW - (listBottom - listTop));
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        scrollTarget = Mth.clamp(scrollTarget - (float) delta * ROW * 1.5F, 0, maxScroll());
        return true;
    }

    private int tabX(int i) {
        return left + 8 + i * (pw - 16) / TAB_KEY.length;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0 && my >= tabTop && my < tabTop + tabH) {
            for (int i = 0; i < TAB_KEY.length; i++) {
                if (mx >= tabX(i) && mx < tabX(i + 1)
                        && (tabPct[i] > 0 || i == 0)) {
                    if (tab != i) {
                        tab = i;
                        rows = filtered();
                        scroll = 0;
                        scrollTarget = 0;
                        UiFx.tab();
                    }
                    return true;
                }
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        // flechas izquierda/derecha: cambiar de pestaña
        if (key == 262 || key == 263) {
            int d = key == 262 ? 1 : -1;
            int n = tab;
            do {
                n = (n + d + TAB_KEY.length) % TAB_KEY.length;
            } while (tabPct[n] <= 0 && n != 0);
            tab = n;
            rows = filtered();
            scroll = 0;
            scrollTarget = 0;
            return true;
        }
        return super.keyPressed(key, scan, mods);
    }

    private static String pct(double v) {
        if (v < 0.05) {
            return "<0.1%";
        }
        if (v >= 10) {
            return String.format(Locale.ROOT, "%.0f%%", v);
        }
        return String.format(Locale.ROOT, "%.2f%%", v).replaceAll("0%$", "%");
    }

    private static String odds(double v) {
        if (v <= 0) {
            return "";
        }
        long n = Math.round(100.0 / v);
        return n <= 1 ? "" : Component.translatable("crate.mundos_de_axelis.odds", n).getString();
    }

    private void small(GuiGraphics g, Component t, int x, int y, float s, int color) {
        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        g.pose().scale(s, s, 1);
        g.drawString(this.font, t, 0, 0, color, false);
        g.pose().popPose();
    }

    private void smallRight(GuiGraphics g, String t, int xr, int y, float s, int color) {
        small(g, Component.literal(t), xr - (int) (this.font.width(t) * s), y, s, color);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        scroll = Mth.lerp(0.35F, scroll, scrollTarget);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        g.fillGradient(0, 0, this.width, this.height, 0x90060612, 0xB0120A1E);

        // panel
        int col = kind.color;
        PanelUi.frame(g, left, top, pw, ph, col);
        g.fillGradient(left, top, left + pw, top + 40, 0x44000000 | col, 0x00000000 | col);
        // hueco oscuro donde va la lista, para que se lea mejor sobre el marco
        g.fill(left + 3, listTop - 2, left + pw - 3, listBottom + 2, 0x80060410);
        g.fill(left + 3, listTop - 2, left + pw - 3, listTop - 1, 0x40000000 | col);
        g.fill(left + 3, listBottom + 1, left + pw - 3, listBottom + 2, 0x40000000 | col);

        // título
        g.pose().pushPose();
        g.pose().translate(left + pw / 2.0F, top + 5, 0);
        g.pose().scale(1.4F, 1.4F, 1);
        g.drawCenteredString(this.font, Component.translatable(kind.titleKey()), 0, 0, col);
        g.pose().popPose();
        g.drawCenteredString(this.font, Component.translatable("crate.mundos_de_axelis.prizes_title"), left + pw / 2, top + 21, 0xB8B0D8);

        // barra con el reparto de la caja por tipo de premio
        int bx = left + 8, bw = pw - 16, by = top + 33;
        g.fill(bx - 1, by - 1, bx + bw + 1, by + 6, 0x80000000);
        int x = bx;
        int lastTab = 0;
        for (int i = 1; i < TAB_KEY.length; i++) {
            if (tabPct[i] > 0) {
                lastTab = i;
            }
        }
        for (int i = 1; i < TAB_KEY.length; i++) {
            if (tabPct[i] <= 0) {
                continue;
            }
            int w = i == lastTab ? bx + bw - x : (int) Math.round(bw * tabPct[i] / Math.max(0.0001, tabPct[0]));
            g.fill(x, by, x + Math.max(1, w), by + 5, (tab == 0 || tab == i ? 0xFF000000 : 0x66000000) | TAB_COLOR[i]);
            x += w;
        }

        // pestañas
        for (int i = 0; i < TAB_KEY.length; i++) {
            if (tabPct[i] <= 0 && i != 0) {
                continue;
            }
            int x0 = tabX(i), x1 = i + 1 < TAB_KEY.length ? tabX(i + 1) : left + pw - 8;
            boolean sel = tab == i;
            boolean over = mx >= x0 && mx < x1 && my >= tabTop && my < tabTop + tabH;
            int c = tabColor(i);
            g.fill(x0 + 1, tabTop, x1 - 1, tabTop + tabH, sel ? (0x55000000 | c) : over ? 0x30FFFFFF : 0x16FFFFFF);
            g.fill(x0 + 1, tabTop + tabH - 2, x1 - 1, tabTop + tabH, (sel ? 0xFF000000 : 0x55000000) | c);
            Component nm = Component.translatable("crate.mundos_de_axelis.tab_" + TAB_KEY[i]);
            int cx = (x0 + x1) / 2;
            float s = 0.7F;
            small(g, nm, cx - (int) (this.font.width(nm) * s / 2), tabTop + 4, s, sel ? 0xFFFFFFFF : 0xFFB8B8D0);
            String p = i == 0 ? "100%" : pct(tabPct[i]);
            small(g, Component.literal(p), cx - (int) (this.font.width(p) * s / 2), tabTop + 14, s, 0xFF000000 | c);
        }

        // lista con recorte
        g.enableScissor(left + 2, listTop, left + pw - 2, listBottom);
        ItemStack hover = ItemStack.EMPTY;
        int off = Math.round(scroll);
        double maxP = rows.isEmpty() ? 1 : rows.get(0).percent();
        for (int i = 0; i < rows.size(); i++) {
            CratePools.Chance c = rows.get(i);
            int ry = listTop + i * ROW - off;
            if (ry + ROW < listTop || ry > listBottom) {
                continue;
            }
            int tc = 0xFF000000 | c.tier().color;
            int rx0 = left + 6, rx1 = left + pw - 10;
            boolean over = mx >= rx0 && mx < rx1 && my >= Math.max(ry, listTop) && my < Math.min(ry + ROW, listBottom);
            g.fill(rx0, ry + 1, rx1, ry + ROW - 1, over ? 0x40FFFFFF : ((i & 1) == 0 ? 0x1CFFFFFF : 0x0AFFFFFF));
            g.fill(rx0, ry + 1, rx0 + 2, ry + ROW - 1, tc);
            ItemStack st = new ItemStack(c.item());
            g.renderItem(st, rx0 + 6, ry + 4);
            boolean star = "pet_star".equals(c.group());
            Component name = c.tier().paint(st.getHoverName());
            g.drawString(this.font, name, rx0 + 28, ry + 4, 0xFFFFFFFF, true);
            String qty = c.max() > 1 ? " ×" + c.min() + (c.max() > c.min() ? "-" + c.max() : "") : "";
            Component sub = Component.empty().append(c.tier().label()).append(qty + (star ? "  ★" : ""));
            small(g, sub, rx0 + 28, ry + 14, 0.65F, star ? 0xFFFFD35C : (0xFF000000 | (c.tier().color & 0xFFFFFF)));
            // porcentaje, "1 de N" y barrita
            smallRight(g, pct(c.percent()), rx1 - 4, ry + 3, 1.0F, 0xFFF0F0FF);
            String o = odds(c.percent());
            if (!o.isEmpty()) {
                smallRight(g, o, rx1 - 4, ry + 13, 0.65F, 0xFF9AA0C8);
            }
            int barW = 54;
            int fill = (int) Math.max(1, Math.round(barW * Math.sqrt(c.percent() / Math.max(0.0001, maxP))));
            g.fill(rx1 - 4 - barW - 70, ry + ROW - 5, rx1 - 4 - 70 + 0, ry + ROW - 3, 0x30FFFFFF);
            g.fill(rx1 - 4 - barW - 70, ry + ROW - 5, rx1 - 4 - barW - 70 + fill, ry + ROW - 3, tc);
            if (over) {
                hover = st;
            }
        }
        g.disableScissor();

        if (rows.isEmpty()) {
            g.drawCenteredString(this.font, Component.translatable("crate.mundos_de_axelis.tab_empty"), left + pw / 2, listTop + 20, 0xFF8890B8);
        }

        // barra de desplazamiento
        if (maxScroll() > 0) {
            int sx = left + pw - 7, sh = listBottom - listTop;
            g.fill(sx, listTop, sx + 3, listBottom, 0x30FFFFFF);
            int th = Math.max(14, (int) (sh * sh / (float) (rows.size() * ROW)));
            int ty = listTop + (int) ((sh - th) * (scroll / maxScroll()));
            g.fill(sx, ty, sx + 3, ty + th, 0xC0000000 | col);
        }

        // garantía: cuántas aperturas faltan para cada rareza segura
        int[] cnt = TableClient.pity.getOrDefault(kind.id, new int[3]);
        int gx = left + 10;
        boolean any = false;
        for (Tier t : kind.pityTiers()) {
            if (kind.pity(t) <= 0) {
                continue;
            }
            any = true;
            Component line = Component.translatable("crate.mundos_de_axelis.pity", t.label(), Pity.left(cnt, kind, t));
            small(g, line, gx, top + ph - 26, 0.75F, 0xFFD8D0F0);
            gx += (int) (this.font.width(line) * 0.75F) + 10;
        }
        if (!any) {
            small(g, Component.translatable("crate.mundos_de_axelis.pity_none"), gx, top + ph - 26, 0.75F, 0xFF8890B8);
        }
        small(g, Component.translatable("crate.mundos_de_axelis.prizes_hint"), left + 10, top + ph - 13, 0.7F, 0xFF8890B8);
        CrateUi.watermark(g, this.font, left + pw - 6, top + ph - 4, 0.45F);

        if (!hover.isEmpty()) {
            g.renderTooltip(this.font, hover, mx, my);
        }
    }
}
