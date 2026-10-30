package com.scardax.client;

import com.scardax.net.PurgePacket;
import com.scardax.net.ScardaxNet;
import com.scardax.net.SetRulePacket;
import com.scardax.net.StrictPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * La pantalla de /scardax. Lista todos los mobs del juego (de todos los mods) con su dibujo, su mod y su categoría. Cada uno tiene
 * una barra de spawneo (0 a 300 %) y un botón para quitarlo del todo. Arriba hay filtros (buscar, categoría, mod, estado) y abajo
 * acciones para todos los que se ven: quitar, restaurar, y limpiar los mobs que ya están cargados en el mundo.
 */
public class MobRulesScreen extends Screen {
    private static final int ROW_H = 36;
    private static final int ICON = 32;

    private final List<MobCatalog.Entry> all = MobCatalog.get();
    private List<MobCatalog.Entry> shown = new ArrayList<>();
    private final List<AbstractWidget> rowWidgets = new ArrayList<>();

    private final List<String> cats = new ArrayList<>();
    private final List<String> mods = new ArrayList<>();
    private int catIdx = 0;
    private int modIdx = 0;
    private int stateIdx = 0; // 0 todos, 1 quitados, 2 cambiados
    private String searchText = "";
    private int scroll = 0;

    private EditBox search;
    private Button strictBtn;
    private int listLeft;
    private int listRight;
    private int listTop;
    private int listBottom;

    public MobRulesScreen() {
        super(Component.translatable("scardax.screen.title"));
        Set<String> c = new LinkedHashSet<>();
        Set<String> m = new LinkedHashSet<>();
        for (MobCatalog.Entry e : all) {
            c.add(e.category.getName());
            m.add(e.modName);
        }
        cats.add("");
        cats.addAll(c);
        mods.add("");
        mods.addAll(m);
    }

    // ------------------------------------------------------------------ montaje

    @Override
    protected void init() {
        rowWidgets.clear();
        listLeft = 16;
        listRight = this.width - 16;
        listTop = 66;
        listBottom = this.height - 34;

        search = new EditBox(this.font, listLeft, 34, 150, 18, Component.translatable("scardax.screen.search"));
        search.setHint(Component.translatable("scardax.screen.search").withStyle(ChatFormatting.DARK_GRAY));
        search.setValue(searchText);
        search.setResponder(s -> {
            searchText = s;
            refilter(true);
        });
        addRenderableWidget(search);

        int x = listLeft + 156;
        addRenderableWidget(Button.builder(catLabel(), b -> {
            catIdx = (catIdx + 1) % cats.size();
            b.setMessage(catLabel());
            refilter(true);
        }).bounds(x, 33, 120, 20).build());
        x += 124;
        addRenderableWidget(Button.builder(modLabel(), b -> {
            modIdx = (modIdx + 1) % mods.size();
            b.setMessage(modLabel());
            refilter(true);
        }).bounds(x, 33, 150, 20).build());
        x += 154;
        addRenderableWidget(Button.builder(stateLabel(), b -> {
            stateIdx = (stateIdx + 1) % 3;
            b.setMessage(stateLabel());
            refilter(true);
        }).bounds(x, 33, 110, 20).build());

        int by = this.height - 28;
        int bw = Math.max(80, Math.min(140, (this.width - 32 - 12) / 5));
        int bx = listLeft;
        addRenderableWidget(Button.builder(Component.translatable("scardax.btn.remove_visible"), b -> setVisible(0))
                .bounds(bx, by, bw, 20).build());
        bx += bw + 3;
        addRenderableWidget(Button.builder(Component.translatable("scardax.btn.restore_visible"), b -> setVisible(100))
                .bounds(bx, by, bw, 20).build());
        bx += bw + 3;
        addRenderableWidget(Button.builder(Component.translatable("scardax.btn.purge"), b -> ScardaxNet.CHANNEL.sendToServer(new PurgePacket()))
                .bounds(bx, by, bw, 20).build());
        bx += bw + 3;
        strictBtn = addRenderableWidget(Button.builder(strictLabel(), b -> {
            ScardaxNet.CHANNEL.sendToServer(new StrictPacket(!ClientRules.strict));
        }).bounds(bx, by, bw, 20).build());
        bx += bw + 3;
        addRenderableWidget(Button.builder(Component.translatable("scardax.btn.close"), b -> onClose())
                .bounds(bx, by, bw, 20).build());

        refilter(false);
    }

    private Component catLabel() {
        String c = cats.get(catIdx);
        return Component.translatable("scardax.filter.category", c.isEmpty() ? Component.translatable("scardax.filter.all") : categoryName(c));
    }

    private Component modLabel() {
        String m = mods.get(modIdx);
        return Component.translatable("scardax.filter.mod", m.isEmpty() ? Component.translatable("scardax.filter.all") : Component.literal(m));
    }

    private Component stateLabel() {
        return Component.translatable("scardax.filter.state." + stateIdx);
    }

    private Component strictLabel() {
        return Component.translatable(ClientRules.strict ? "scardax.btn.strict_on" : "scardax.btn.strict_off");
    }

    private static Component categoryName(String key) {
        return Component.translatable("scardax.category." + key);
    }

    // ------------------------------------------------------------------ filtros y lista

    /** Las reglas cambiaron (las confirmó el servidor): se vuelve a dibujar todo. */
    public void refresh() {
        if (strictBtn != null) {
            strictBtn.setMessage(strictLabel());
        }
        refilter(false);
    }

    private void refilter(boolean resetScroll) {
        String q = searchText.trim().toLowerCase();
        String cat = cats.get(catIdx);
        String mod = mods.get(modIdx);
        shown = new ArrayList<>();
        for (MobCatalog.Entry e : all) {
            if (!cat.isEmpty() && !e.category.getName().equals(cat)) {
                continue;
            }
            if (!mod.isEmpty() && !e.modName.equals(mod)) {
                continue;
            }
            int p = ClientRules.get(e.id);
            if ((stateIdx == 1 && p != 0) || (stateIdx == 2 && p == 100)) {
                continue;
            }
            if (!q.isEmpty() && !e.search.contains(q)) {
                continue;
            }
            shown.add(e);
        }
        if (resetScroll) {
            scroll = 0;
        }
        clampScroll();
        rebuildRows();
    }

    private int visibleRows() {
        return Math.max(1, (listBottom - listTop) / ROW_H);
    }

    private void clampScroll() {
        int max = Math.max(0, shown.size() - visibleRows());
        scroll = Math.max(0, Math.min(max, scroll));
    }

    private void rebuildRows() {
        for (AbstractWidget w : rowWidgets) {
            removeWidget(w);
        }
        rowWidgets.clear();
        int end = Math.min(shown.size(), scroll + visibleRows());
        for (int i = scroll; i < end; i++) {
            MobCatalog.Entry e = shown.get(i);
            int y = listTop + (i - scroll) * ROW_H;
            int quitW = 72;
            int quitX = listRight - 8 - quitW;
            int sliderW = 130;
            int sliderX = quitX - 6 - sliderW;
            PercentSlider slider = new PercentSlider(sliderX, y + 8, sliderW, 20, e, pct -> {
                send(e.id, pct);
                Minecraft.getInstance().tell(this::rebuildRows);
            });
            int pct = ClientRules.get(e.id);
            Button quit = Button.builder(Component.translatable(pct == 0 ? "scardax.row.removed" : "scardax.row.remove"), b -> {
                send(e.id, ClientRules.get(e.id) == 0 ? 100 : 0);
                rebuildRows();
            }).bounds(quitX, y + 8, quitW, 20).build();
            rowWidgets.add(addRenderableWidget(slider));
            rowWidgets.add(addRenderableWidget(quit));
        }
    }

    /** Cambia la regla aquí al instante y se la manda al servidor. */
    private void send(ResourceLocation id, int pct) {
        ClientRules.set(id, pct);
        List<ResourceLocation> one = new ArrayList<>();
        one.add(id);
        ScardaxNet.CHANNEL.sendToServer(new SetRulePacket(one, pct));
    }

    private void setVisible(int pct) {
        if (shown.isEmpty()) {
            return;
        }
        List<ResourceLocation> ids = new ArrayList<>();
        for (MobCatalog.Entry e : shown) {
            ids.add(e.id);
            ClientRules.set(e.id, pct);
        }
        ScardaxNet.CHANNEL.sendToServer(new SetRulePacket(ids, pct));
        refilter(false);
    }

    // ------------------------------------------------------------------ dibujo

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        renderBackground(g);
        g.drawCenteredString(this.font, this.title, this.width / 2, 12, 0xFFFFFF);

        int removed = 0;
        int changed = 0;
        for (MobCatalog.Entry e : all) {
            int p = ClientRules.get(e.id);
            if (p == 0) {
                removed++;
            } else if (p != 100) {
                changed++;
            }
        }
        Component counts = Component.translatable("scardax.screen.counts", all.size(), shown.size(), removed, changed);
        g.drawString(this.font, counts, listLeft, 56, 0xAAAAAA, false);

        g.fill(listLeft, listTop, listRight, listBottom, 0x90000000);
        g.enableScissor(listLeft, listTop, listRight, listBottom);
        int end = Math.min(shown.size(), scroll + visibleRows());
        for (int i = scroll; i < end; i++) {
            drawRow(g, shown.get(i), listTop + (i - scroll) * ROW_H, i);
        }
        g.disableScissor();
        if (shown.isEmpty()) {
            g.drawCenteredString(this.font, Component.translatable("scardax.screen.empty"), this.width / 2, listTop + 20, 0xAAAAAA);
        }
        drawScrollbar(g);
        super.render(g, mx, my, pt);
    }

    private void drawRow(GuiGraphics g, MobCatalog.Entry e, int y, int index) {
        int pct = ClientRules.get(e.id);
        int bg = pct == 0 ? 0x66AA2222 : pct != 100 ? 0x55AA9922 : (index % 2 == 0 ? 0x33FFFFFF : 0x22FFFFFF);
        g.fill(listLeft + 2, y + 1, listRight - 8, y + ROW_H - 1, bg);
        // dibujo
        int ix = listLeft + 6;
        int iy = y + 2;
        g.fill(ix, iy, ix + ICON, iy + ICON, 0xFF1B1B1B);
        drawIcon(g, e, ix, iy);
        // textos
        int textX = ix + ICON + 6;
        int textMax = listRight - 8 - 72 - 6 - 130 - 6 - textX - 4;
        String name = this.font.plainSubstrByWidth(e.name.getString(), Math.max(20, textMax));
        g.drawString(this.font, name, textX, y + 7, pct == 0 ? 0xFF8888 : 0xFFFFFF, false);
        String sub = e.modName + " · " + categoryName(e.category.getName()).getString();
        g.drawString(this.font, this.font.plainSubstrByWidth(sub, Math.max(20, textMax)), textX, y + 19, 0xAAAAAA, false);
    }

    /** El mob dibujado dentro de su cuadro; si no se puede dibujar, el huevo de aparición o una marca. */
    private void drawIcon(GuiGraphics g, MobCatalog.Entry e, int ix, int iy) {
        LivingEntity le = e.living;
        if (le != null && !e.broken) {
            try {
                float size = Math.max(Math.max(le.getBbHeight(), le.getBbWidth() * 0.9F), 0.3F);
                int scale = Math.max(2, Math.min(60, (int) (24.0F / size)));
                g.enableScissor(ix, iy, ix + ICON, iy + ICON);
                InventoryScreen.renderEntityInInventoryFollowsAngle(g, ix + ICON / 2, iy + ICON - 4, scale, -12.0F, -4.0F, le);
                g.disableScissor();
                return;
            } catch (Throwable t) {
                e.broken = true;
            }
        }
        if (!e.egg.isEmpty()) {
            g.renderItem(e.egg, ix + 8, iy + 8);
        } else {
            g.drawCenteredString(this.font, "?", ix + ICON / 2, iy + ICON / 2 - 4, 0xAAAAAA);
        }
    }

    private void drawScrollbar(GuiGraphics g) {
        int rows = visibleRows();
        if (shown.size() <= rows) {
            return;
        }
        int x0 = listRight - 6;
        int h = listBottom - listTop;
        g.fill(x0, listTop, x0 + 5, listBottom, 0x55000000);
        int thumb = Math.max(14, h * rows / shown.size());
        int span = h - thumb;
        int max = shown.size() - rows;
        int ty = listTop + (max == 0 ? 0 : span * scroll / max);
        g.fill(x0, ty, x0 + 5, ty + thumb, 0xFFAAAAAA);
    }

    // ------------------------------------------------------------------ ratón

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        if (mx >= listLeft && mx <= listRight && my >= listTop && my <= listBottom) {
            int before = scroll;
            scroll -= (int) Math.signum(delta) * 2;
            clampScroll();
            if (scroll != before) {
                rebuildRows();
            }
            return true;
        }
        return super.mouseScrolled(mx, my, delta);
    }

    private boolean onScrollbar(double mx, double my) {
        return shown.size() > visibleRows() && mx >= listRight - 7 && mx <= listRight && my >= listTop && my <= listBottom;
    }

    private void dragScrollbar(double my) {
        int max = Math.max(0, shown.size() - visibleRows());
        double f = (my - listTop) / Math.max(1, listBottom - listTop);
        int before = scroll;
        scroll = (int) Math.round(f * max);
        clampScroll();
        if (scroll != before) {
            rebuildRows();
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0 && onScrollbar(mx, my)) {
            dragScrollbar(my);
            return true;
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (button == 0 && mx >= listRight - 12 && mx <= listRight + 6 && shown.size() > visibleRows() && !(getFocused() instanceof PercentSlider)) {
            dragScrollbar(my);
            return true;
        }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
