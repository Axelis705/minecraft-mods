package com.mundosdeaxelis.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mundosdeaxelis.MundosDeAxelis;
import com.mundosdeaxelis.crate.CrateKind;
import com.mundosdeaxelis.crate.Tier;
import com.mundosdeaxelis.crate.Tiers;
import com.mundosdeaxelis.registry.ModItems;
import com.mundosdeaxelis.relic.RelicItem;
import com.mundosdeaxelis.table.Bound;
import com.mundosdeaxelis.table.Essence;
import com.mundosdeaxelis.table.Mats;
import com.mundosdeaxelis.table.TableLevels;
import com.mundosdeaxelis.table.TableMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Pantalla de la Mesa del Artífice: Taller, Tienda, Archivo y Mesa (nivel). Todo se dibuja a mano, con el mismo estilo que el menú de equipo. */
public class TableScreen extends AbstractContainerScreen<TableMenu> {
    private static final int W = 256, H = 254;
    private static final int GOLD = 0xFFE9A8, VIOLET = 0xFFB890FF, GRAY = 0xFF9A8CC0;
    private static final String[] TABS = {"workshop", "shop", "archive", "table", "craft"};
    private static final int SHOP_PER_PAGE = 4;

    private int tab;
    private int shopPage;
    private int craftPage;
    private int archCat;
    private int archPage;

    /** Zona en la que se puede hacer clic (en coordenadas de la pantalla ya sin el margen de la ventana). */
    private record Hot(int x, int y, int w, int h, Runnable action, Component tip) {
    }

    private final List<Hot> hots = new ArrayList<>();
    private Component hoverTip;
    /** Objeto del Archivo que ya tienes (su descripción lleva la línea «Ya lo tienes»). */
    private ItemStack ownedTip = ItemStack.EMPTY;
    private ItemStack hoverStack = ItemStack.EMPTY;

    public TableScreen(TableMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = W;
        this.imageHeight = H;
        this.titleLabelY = 6;
        this.inventoryLabelY = TableMenu.INV_Y - 11;
    }

    @Override
    protected void init() {
        super.init();
        menu.tallerOpen = tab == 0;
        UiFx.open();
        lastToast = TableClient.toastTime;
    }

    private long lastToast;
    private int hoverId = -1;
    private int hoverNow = -1;

    private void press(int id) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.gameMode != null) {
            mc.gameMode.handleInventoryButtonClick(menu.containerId, id);
        }
    }

    private void setTab(int t) {
        tab = t;
        menu.tallerOpen = t == 0;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0) {
            int rx = (int) mx - leftPos, ry = (int) my - topPos;
            for (Hot h : hots) {
                if (rx >= h.x && rx < h.x + h.w && ry >= h.y && ry < h.y + h.h) {
                    h.action.run();
                    if (h.tip == null) {
                        UiFx.tab();
                    } else {
                        UiFx.click();
                    }
                    return true;
                }
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        renderBackground(g);
        hoverTip = null;
        hoverStack = ItemStack.EMPTY;
        ownedTip = ItemStack.EMPTY;
        if (TableClient.toast != null && TableClient.toastTime != lastToast) {
            lastToast = TableClient.toastTime;
            if (!TableClient.toastGood) {
                UiFx.error();
            } else if (TableClient.toast.getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents tc && (tc.getKey().endsWith(".forged") || tc.getKey().endsWith(".crafted"))) {
                UiFx.forge();
            } else {
                UiFx.success();
            }
        }
        hoverNow = -1;
        super.render(g, mx, my, pt);
        if (hoverNow != hoverId) {
            hoverId = hoverNow;
            if (hoverNow >= 0) {
                UiFx.hover();
            }
        }
        if (!ownedTip.isEmpty()) {
            Minecraft mcx = Minecraft.getInstance();
            List<Component> lines = new java.util.ArrayList<>(ownedTip.getTooltipLines(mcx.player, mcx.options.advancedItemTooltips ? net.minecraft.world.item.TooltipFlag.Default.ADVANCED : net.minecraft.world.item.TooltipFlag.Default.NORMAL));
            lines.add(Component.translatable("gui.mundos_de_axelis.table.owned").withStyle(net.minecraft.ChatFormatting.WHITE, net.minecraft.ChatFormatting.BOLD));
            g.renderTooltip(font, lines, ownedTip.getTooltipImage(), ownedTip, mx, my);
        } else if (!hoverStack.isEmpty()) {
            g.renderTooltip(font, hoverStack, mx, my);
        } else if (hoverTip != null) {
            g.renderTooltip(font, font.split(hoverTip, 200), mx, my);
        } else {
            renderTooltip(g, mx, my);
        }
    }

    @Override
    protected void renderBg(GuiGraphics g, float pt, int mx, int my) {
        int x = leftPos, y = topPos;
        PanelUi.frame(g, x, y, W, H, 0xC8962E);
        PanelUi.divider(g, x + 6, y + 37, W - 12, 0xC8962E);
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 9; c++) {
                plain(g, x + TableMenu.INV_X + c * 18, y + TableMenu.INV_Y + r * 18);
            }
        }
        for (int c = 0; c < 9; c++) {
            plain(g, x + TableMenu.INV_X + c * 18, y + TableMenu.INV_Y + 58);
        }
        if (tab == 0) {
            int sx = x + TableMenu.SLOT_X, sy = y + TableMenu.SLOT_Y;
            g.fill(sx - 3, sy - 3, sx + 19, sy + 19, 0x40000000 | GOLD);
            g.fill(sx - 2, sy - 2, sx + 18, sy + 18, 0xFF000000 | GOLD);
            g.fill(sx - 1, sy - 1, sx + 17, sy + 17, 0xFF1A1030);
            PanelUi.slot(g, sx, sy, GOLD, false);
        }
    }

    private static void plain(GuiGraphics g, int x, int y) {
        PanelUi.plain(g, x, y);
    }

    // ------------------------------------------------------------------ contenido (coordenadas relativas a la ventana)

    @Override
    protected void renderLabels(GuiGraphics g, int amx, int amy) {
        hots.clear();
        int mx = amx - leftPos, my = amy - topPos;
        Font f = font;
        PanelUi.title(g, f, title, 8, titleLabelY, 0xC8962E);
        String es = "◆ " + TableClient.essence;
        g.drawString(f, es, W - 8 - f.width(es), titleLabelY, 0xFF000000 | 0x7FE8FF, true);
        if (mx >= W - 8 - f.width(es) && mx < W - 8 && my >= 4 && my < 16) {
            hoverTip = Component.translatable("gui.mundos_de_axelis.table.essence_tip");
        }
        // pestañas
        for (int i = 0; i < TABS.length; i++) {
            int tx = 8 + i * 48, ty = 19;
            boolean on = i == tab;
            boolean over = mx >= tx && mx < tx + 46 && my >= ty && my < ty + 16;
            Component lab = Component.translatable("gui.mundos_de_axelis.table.tab." + TABS[i]);
            PanelUi.tab(g, f, tx, ty, 46, 16, lab, on, over, GOLD & 0xFFFFFF);
            if (over && !on) {
                hoverNow = 1000 + i;
            }
            final int idx = i;
            hots.add(new Hot(tx, ty, 46, 16, () -> setTab(idx), null));
        }
        switch (tab) {
            case 0 -> workshop(g, mx, my);
            case 1 -> shop(g, mx, my);
            case 2 -> archive(g, mx, my);
            case 4 -> craftTab(g, mx, my);
            default -> tableTab(g, mx, my);
        }
        // aviso de la última acción
        if (TableClient.toast != null && System.currentTimeMillis() - TableClient.toastTime < 6000) {
            int col = TableClient.toastGood ? 0xFF8CFFA8 : 0xFFFF8C8C;
            List<FormattedCharSequence> ls = f.split(TableClient.toast, W - 16);
            g.drawString(f, ls.get(0), 8, 152, col, true);
        }
        g.drawString(f, playerInventoryTitle, TableMenu.INV_X, inventoryLabelY, 0xFFB8AAD8, false);
        if (menu.helped()) {
            Component h = Component.translatable("gui.mundos_de_axelis.table.helped", menu.effectiveLevel());
            g.drawString(f, h, W - 8 - f.width(h), inventoryLabelY, 0xFF000000 | GOLD, false);
        }
    }

    private void button(GuiGraphics g, int x, int y, int w, int h, Component label, boolean enabled, int mx, int my, Runnable act, Component tip) {
        boolean over = enabled && mx >= x && mx < x + w && my >= y && my < y + h;
        PanelUi.button(g, font, x, y, w, h, label, enabled, over, GOLD & 0xFFFFFF);
        if (over) {
            hoverNow = x * 1000 + y;
        }
        if (enabled && act != null) {
            hots.add(new Hot(x, y, w, h, act, tip));
        }
        if (mx >= x && mx < x + w && my >= y && my < y + h && tip != null) {
            hoverTip = tip;
        }
    }

    private static Item mat(Mats m) {
        return ForgeRegistries.ITEMS.getValue(new ResourceLocation(MundosDeAxelis.MODID, m.itemId()));
    }

    private void wrap(GuiGraphics g, Component c, int x, int y, int w, int color) {
        for (FormattedCharSequence l : font.split(FormattedText.of(c.getString()), w)) {
            g.drawString(font, l, x, y, color, false);
            y += 9;
        }
    }

    // ------------------------------------------------------------------ Taller

    private int refinable(boolean essence) {
        Minecraft mc = Minecraft.getInstance();
        int total = 0;
        for (Mats m : Mats.values()) {
            Item it = mat(m);
            if (it == null || mc.player == null || m.isBoss()) {
                continue;
            }
            int reserve = Math.max(TableClient.level >= TableLevels.MAX ? 0 : TableLevels.upgradeMats(TableClient.level + 1).getOrDefault(m, 0),
                    com.mundosdeaxelis.crate.CrateForge.reservedFor(m));
            int n = Math.max(0, TableMenu.count(mc.player, it) - reserve);
            total += essence ? n * m.essence : n;
        }
        return total;
    }

    private void workshop(GuiGraphics g, int mx, int my) {
        Font f = font;
        ItemStack s = menu.boxItem();
        int px = 62;
        boolean relic = s.getItem() instanceof RelicItem;
        boolean gear = s.getItem() instanceof com.mundosdeaxelis.gear.GearTool;
        if (!relic && !gear) {
            wrap(g, Component.translatable("gui.mundos_de_axelis.table.drop_hint"), 8, 80, 52, GRAY);
            wrap(g, Component.translatable("gui.mundos_de_axelis.table.workshop_info"), px, 44, W - px - 8, 0xFFB8AAD8);
        } else {
            Tier t = Tiers.of(s);
            Tier seen = Tiers.shown(s);
            int lv = relic ? RelicItem.level(s) : 1;
            int cap = TableLevels.maxItemLevel(menu.effectiveLevel());
            g.drawString(f, s.getHoverName(), px, 44, 0xFF000000 | seen.color, true);
            boolean mine = Minecraft.getInstance().player != null && Bound.isBoundTo(s, Minecraft.getInstance().player);
            boolean other = Bound.isBound(s) && !mine;
            if (relic) {
                g.drawString(f, Component.translatable("gui.mundos_de_axelis.table.rarity_level", seen.label(), lv, 5), px, 55, 0xFFB8AAD8, false);
                for (int i = 0; i < 5; i++) {
                    int bx = px + i * 14;
                    g.fill(bx, 67, bx + 12, 72, 0xFF000000);
                    g.fill(bx + 1, 68, bx + 11, 71, i < lv ? 0xFF000000 | seen.color : i < cap ? 0xFF2C2048 : 0xFF181020);
                }
                boolean canUp = lv < cap;
                int cost = TableLevels.levelCost(t, lv, menu.effectiveLevel());
                if (lv >= 5) {
                    g.drawString(f, Component.translatable("message.mundos_de_axelis.table.item_max"), px, 78, GRAY, false);
                } else if (!canUp) {
                    wrap(g, Component.translatable("message.mundos_de_axelis.table.item_cap"), px, 78, W - px - 8, 0xFFFFC27A);
                } else {
                    boolean ok = TableClient.essence >= cost;
                    g.drawString(f, Component.translatable("gui.mundos_de_axelis.table.level_cost", lv + 1, cost), px, 78, ok ? 0xFF8CFFA8 : 0xFFFF8C8C, false);
                }
                button(g, px, 90, 54, 14, Component.translatable("gui.mundos_de_axelis.table.level_up"), canUp && lv < 5, mx, my,
                        () -> press(TableMenu.B_LEVEL), Component.translatable("gui.mundos_de_axelis.table.level_up_tip"));
                boolean unique = s.hasTag() && s.getTag().getBoolean("mdc_unique");
                button(g, px + 57, 90, 56, 14, Component.translatable("gui.mundos_de_axelis.table.dismantle"), !unique, mx, my,
                        () -> press(TableMenu.B_DISMANTLE), Component.translatable("gui.mundos_de_axelis.table.dismantle_tip"));
            } else {
                g.drawString(f, Component.translatable("gui.mundos_de_axelis.table.rarity_only", seen.label()), px, 55, 0xFFB8AAD8, false);
                wrap(g, Component.translatable("gui.mundos_de_axelis.table.gear_info"), px, 67, W - px - 8, GRAY);
            }
            // vínculo de alma
            Component bl = Component.translatable(mine ? "gui.mundos_de_axelis.table.unbind" : "gui.mundos_de_axelis.table.bind");
            button(g, px + 116, 90, 70, 14, bl, !other, mx, my, () -> press(TableMenu.B_BIND),
                    Component.translatable(mine ? "gui.mundos_de_axelis.table.unbind_tip" : "gui.mundos_de_axelis.table.bind_tip"));
            Component info = relic
                    ? Component.translatable("gui.mundos_de_axelis.table.bind_info", TableLevels.dismantleValue(t, lv, menu.effectiveLevel()), Bound.cost(t))
                    : Component.translatable("gui.mundos_de_axelis.table.bind_info_gear", Bound.cost(t));
            g.drawString(f, info, px, 106, other ? 0xFFFF8C8C : 0xFFB8AAD8, false);
            wrap(g, Component.translatable("gui.mundos_de_axelis.table.bound_count", menu.boundCount(), menu.boundLimit()), 8, 80, 52,
                    mine ? 0xFF8CFFA8 : GRAY);
        }
        // refinar materiales
        PanelUi.divider(g, 8, 116, W - 16, 0xC8962E);
        int pieces = refinable(false), gain = refinable(true);
        g.drawString(f, Component.translatable("gui.mundos_de_axelis.table.refine_title"), 8, 121, 0xFF000000 | GOLD, false);
        Component info = pieces > 0 ? Component.translatable("gui.mundos_de_axelis.table.refine_info", pieces, gain)
                : Component.translatable("gui.mundos_de_axelis.table.refine_none");
        wrap(g, info, 8, 133, 150, pieces > 0 ? 0xFFB8AAD8 : GRAY);
        button(g, W - 8 - 84, 128, 84, 16, Component.translatable("gui.mundos_de_axelis.table.refine"), pieces > 0, mx, my,
                () -> press(TableMenu.B_REFINE), Component.translatable("gui.mundos_de_axelis.table.refine_tip"));
    }

    // ------------------------------------------------------------------ Tienda

    private static List<CrateKind> sold() {
        List<CrateKind> l = new ArrayList<>();
        for (CrateKind k : CrateKind.values()) {
            if (k.keyCost() > 0) {
                l.add(k);
            }
        }
        return l;
    }

    private static String roman(int n) {
        return switch (n) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            case 5 -> "V";
            case 6 -> "VI";
            case 7 -> "VII";
            default -> "VIII";
        };
    }

    /** Candadito dibujado a mano (8x10). */
    private static void lock(GuiGraphics g, int x, int y) {
        g.fill(x + 1, y, x + 7, y + 1, 0xFFC8B8E8);
        g.fill(x + 1, y + 1, x + 2, y + 5, 0xFFC8B8E8);
        g.fill(x + 6, y + 1, x + 7, y + 5, 0xFFC8B8E8);
        g.fill(x, y + 4, x + 8, y + 10, 0xFF000000 | GOLD);
        g.fill(x + 1, y + 5, x + 7, y + 9, 0xFFB0802A);
        g.fill(x + 3, y + 6, x + 5, y + 8, 0xFF2A1C4A);
    }

    private int heldCount(Item it) {
        Minecraft mc = Minecraft.getInstance();
        return mc.player == null ? 0 : TableMenu.count(mc.player, it);
    }

    /** Si el jugador tiene ahora mismo todo lo que pide la receta de esa caja. */
    private boolean canForge(CrateKind k) {
        if (TableClient.essence < com.mundosdeaxelis.crate.CrateForge.essence(k)) {
            return false;
        }
        for (com.mundosdeaxelis.crate.CrateForge.Need n : com.mundosdeaxelis.crate.CrateForge.mats(k)) {
            if (heldCount(ModItems.MATS.get(n.mat()).get()) < n.n()) {
                return false;
            }
        }
        CrateKind sk = com.mundosdeaxelis.crate.CrateForge.sealOf(k);
        return sk == null || heldCount(com.mundosdeaxelis.crate.CrateForge.sealItem(sk)) >= 1;
    }

    private void shop(GuiGraphics g, int mx, int my) {
        int stage = com.mundosdeaxelis.crate.CrateForge.stage(TableClient.archive);
        List<CrateKind> list = new ArrayList<>();
        for (CrateKind k : sold()) {
            if (k.rank() <= stage + 2) {
                list.add(k);
            }
        }
        int pages = Math.max(1, (list.size() + SHOP_PER_PAGE - 1) / SHOP_PER_PAGE);
        shopPage = Math.max(0, Math.min(pages - 1, shopPage));
        int from = shopPage * SHOP_PER_PAGE;
        double t = UiFx.time();
        for (int i = 0; i < SHOP_PER_PAGE && from + i < list.size(); i++) {
            CrateKind k = list.get(from + i);
            int ry = 42 + i * 27;
            int lx = 8, lw = 150, kx = 162, kw = 86;
            boolean mystery = k.rank() > stage + 1;
            int kc = TableLevels.keyPrice(k, menu.effectiveLevel()), cc = com.mundosdeaxelis.crate.CrateForge.essence(k);
            float kd = TableLevels.keyDiscount(menu.effectiveLevel(), k);
            int idx = k.ordinal();
            boolean ready = !mystery && canForge(k);
            CrateKind sealK = com.mundosdeaxelis.crate.CrateForge.sealOf(k);
            boolean locked = mystery || (sealK != null && heldCount(com.mundosdeaxelis.crate.CrateForge.sealItem(sealK)) < 1);
            int edge = ready ? (int) (0x90 + 0x6F * Math.sin(t * 4.0D + i)) : locked ? 0x50 : 0xFF;
            int tint = mystery ? 0x6C5E8C : k.color;
            // ---- celda de la caja (a la izquierda)
            g.fill(lx, ry, lx + lw, ry + 26, (edge << 24) | tint);
            g.fillGradient(lx + 1, ry + 1, lx + lw - 1, ry + 25, locked ? 0xFF1A1230 : 0xFF2A1C4A, locked ? 0xFF0E0A1C : 0xFF150D28);
            g.fillGradient(lx + 1, ry + 1, lx + lw - 1, ry + 10, (locked ? 0x18000000 : 0x38000000) | tint, tint);
            if (ready) {
                for (int j = 0; j < 4; j++) {
                    double ph = (t * 0.6D + j * 0.25D + i * 0.1D) % 1.0D;
                    int px = lx + 26 + j * 28 + (int) (Math.sin(t * 2 + j) * 3);
                    int py = ry + 24 - (int) (ph * 22);
                    g.fill(px, py, px + 1, py + 1, ((int) (Math.sin(ph * Math.PI) * 0xA0) << 24) | 0xFFFFFF);
                }
            }
            ItemStack crate = new ItemStack(ModItems.CRATE_ITEMS.get(k).get());
            if (mystery) {
                shadowItem(g, crate, lx + 3, ry + 5);
                g.drawString(font, "?", lx + 9, ry + 9, 0xFFB8AAD8, false);
            } else {
                g.renderItem(crate, lx + 3, ry + 5);
            }
            if (locked && !mystery) {
                g.fill(lx + 3, ry + 5, lx + 19, ry + 21, 0x90100A20);
                lock(g, lx + 7, ry + 8);
            }
            if (!mystery && mx >= lx + 3 && mx < lx + 19 && my >= ry + 5 && my < ry + 21) {
                hoverStack = crate;
            }
            // etapa en números romanos, pegada a la esquina del icono
            String stageTxt = roman(k.rank());
            g.pose().pushPose();
            g.pose().translate(lx + 3, ry + 19, 200);
            g.pose().scale(0.7F, 0.7F, 1.0F);
            g.drawString(font, stageTxt, 1, 1, 0xFF000000, false);
            g.drawString(font, stageTxt, 0, 0, 0xFF000000 | (locked ? 0x9A8CC0 : GOLD), false);
            g.pose().popPose();
            if (mystery) {
                g.drawString(font, "???", lx + 22, ry + 3, 0xFF807098, false);
                button(g, lx + lw - 38, ry + 2, 36, 11, Component.literal("?"), false, mx, my, null, null);
                g.drawString(font, "\u25C6 ?    ?    ?    ?", lx + 22, ry + 15, 0xFF6C5E8C, false);
                if (mx >= lx && mx < lx + lw && my >= ry && my < ry + 26) {
                    hoverTip = Component.translatable("gui.mundos_de_axelis.table.mystery_crate");
                }
            } else {
                fitText(g, Component.translatable(k.titleKey()), lx + 22, ry + 3, 86, (locked ? 0xFF807098 : 0xFF000000 | k.color));
                button(g, lx + lw - 38, ry + 2, 36, 11, Component.translatable("gui.mundos_de_axelis.table.forge_btn"), TableClient.essence >= cc, mx, my,
                        () -> press(TableMenu.B_CRATE + idx), forgeTip(k));
                // lo que pide: Esencia, materiales de mobs y sello (de izquierda a derecha)
                int ex = lx + 22, ey = ry + 14;
                String es = "\u25C6" + cc;
                g.drawString(font, es, ex, ey + 1, TableClient.essence >= cc ? 0xFF7FE8FF : 0xFFFF8C8C, false);
                ex += font.width(es) + 4;
                for (com.mundosdeaxelis.crate.CrateForge.Need n : com.mundosdeaxelis.crate.CrateForge.mats(k)) {
                    ex = need(g, mx, my, ex, ey, new ItemStack(ModItems.MATS.get(n.mat()).get()), n.n());
                }
                if (sealK != null) {
                    ex = need(g, mx, my, ex, ey, new ItemStack(com.mundosdeaxelis.crate.CrateForge.sealItem(sealK)), 1);
                }
            }
            // ---- celda de la llave (a la derecha)
            g.fill(kx, ry, kx + kw, ry + 26, ((locked ? 0x50 : 0xB0) << 24) | tint);
            g.fillGradient(kx + 1, ry + 1, kx + kw - 1, ry + 25, locked ? 0xFF1A1230 : 0xFF2A1C4A, locked ? 0xFF0E0A1C : 0xFF150D28);
            g.fillGradient(kx + 1, ry + 1, kx + kw - 1, ry + 10, (locked ? 0x18000000 : 0x30000000) | tint, tint);
            ItemStack key = new ItemStack(ModItems.KEYS.get(k).get());
            if (mystery) {
                shadowItem(g, key, kx + 3, ry + 5);
                g.drawString(font, "Llave ???", kx + 22, ry + 3, 0xFF807098, false);
                button(g, kx + 22, ry + 13, 48, 11, Component.literal("\u25C6 ?"), false, mx, my, null, null);
            } else {
                g.renderItem(key, kx + 3, ry + 5);
                if (mx >= kx + 3 && mx < kx + 19 && my >= ry + 5 && my < ry + 21) {
                    hoverStack = key;
                }
                fitText(g, key.getHoverName(), kx + 22, ry + 3, kw - 24, (locked ? 0xFF807098 : 0xFF000000 | k.color));
                button(g, kx + 22, ry + 13, 48, 11, Component.literal("\u25C6 " + kc), TableClient.essence >= kc, mx, my,
                        () -> press(TableMenu.B_KEY + idx), kd > 0
                                ? Component.translatable("gui.mundos_de_axelis.table.buy_key_tip_discount", Component.translatable(k.titleKey()), Math.round(kd * 100), k.keyCost())
                                : Component.translatable("gui.mundos_de_axelis.table.buy_key_tip", Component.translatable(k.titleKey())));
            }
        }
        if (pages > 1) {
            pager(g, mx, my, shopPage, pages, 8 + 5 * 0, 152 - 0, false);
        }
    }

    // ------------------------------------------------------------------ Crear

    private static final int CRAFT_PER_PAGE = 4;

    private boolean canCraft(com.mundosdeaxelis.table.TableRecipes.Recipe r) {
        if (menu.effectiveLevel() < r.minLevel() || TableClient.essence < r.essence()) {
            return false;
        }
        for (com.mundosdeaxelis.table.TableRecipes.Ing in : r.ings()) {
            if (heldCount(in.item().get()) < in.n()) {
                return false;
            }
        }
        return true;
    }

    private void craftTab(GuiGraphics g, int mx, int my) {
        List<com.mundosdeaxelis.table.TableRecipes.Recipe> list = com.mundosdeaxelis.table.TableRecipes.all();
        int pages = Math.max(1, (list.size() + CRAFT_PER_PAGE - 1) / CRAFT_PER_PAGE);
        craftPage = Math.max(0, Math.min(pages - 1, craftPage));
        int from = craftPage * CRAFT_PER_PAGE;
        double t = UiFx.time();
        int lx = 8, lw = W - 16;
        for (int i = 0; i < CRAFT_PER_PAGE && from + i < list.size(); i++) {
            com.mundosdeaxelis.table.TableRecipes.Recipe r = list.get(from + i);
            int idx = from + i;
            int ry = 42 + i * 27;
            boolean lockedLv = menu.effectiveLevel() < r.minLevel();
            boolean ready = canCraft(r);
            ItemStack out = new ItemStack(r.result().get(), r.count());
            int tint = com.mundosdeaxelis.crate.Tiers.shown(out).color;
            int edge = ready ? (int) (0x90 + 0x6F * Math.sin(t * 4.0D + i)) : lockedLv ? 0x50 : 0xB0;
            g.fill(lx, ry, lx + lw, ry + 26, (edge << 24) | tint);
            g.fillGradient(lx + 1, ry + 1, lx + lw - 1, ry + 25, lockedLv ? 0xFF1A1230 : 0xFF2A1C4A, lockedLv ? 0xFF0E0A1C : 0xFF150D28);
            g.fillGradient(lx + 1, ry + 1, lx + lw - 1, ry + 10, (lockedLv ? 0x18000000 : 0x38000000) | tint, tint);
            if (ready) {
                for (int j = 0; j < 4; j++) {
                    double ph = (t * 0.6D + j * 0.25D + i * 0.1D) % 1.0D;
                    int px = lx + 26 + j * 40 + (int) (Math.sin(t * 2 + j) * 3);
                    int py = ry + 24 - (int) (ph * 22);
                    g.fill(px, py, px + 1, py + 1, ((int) (Math.sin(ph * Math.PI) * 0xA0) << 24) | 0xFFFFFF);
                }
            }
            g.renderItem(out, lx + 3, ry + 5);
            if (r.count() > 1) {
                g.renderItemDecorations(font, out, lx + 3, ry + 5);
            }
            if (lockedLv) {
                g.fill(lx + 3, ry + 5, lx + 19, ry + 21, 0x90100A20);
                lock(g, lx + 7, ry + 8);
            }
            if (mx >= lx + 3 && mx < lx + 19 && my >= ry + 5 && my < ry + 21) {
                hoverStack = out;
            }
            fitText(g, out.getHoverName(), lx + 22, ry + 3, lw - 22 - 50, lockedLv ? 0xFF807098 : 0xFF000000 | tint);
            String cost = "\u25C6" + r.essence();
            int ex = lx + 22, ey = ry + 14;
            g.drawString(font, cost, ex, ey + 1, TableClient.essence >= r.essence() ? 0xFF7FE8FF : 0xFFFF8C8C, false);
            ex += font.width(cost) + 5;
            for (com.mundosdeaxelis.table.TableRecipes.Ing in : r.ings()) {
                ex = need(g, mx, my, ex, ey, new ItemStack(in.item().get()), in.n());
            }
            if (lockedLv) {
                Component need = Component.translatable("gui.mundos_de_axelis.table.craft_need_level", Component.translatable(TableLevels.nameKey(r.minLevel())));
                g.drawString(font, need, lx + lw - 4 - font.width(need), ry + 4, 0xFFFFC27A, false);
            } else {
                button(g, lx + lw - 46, ry + 2, 42, 11, Component.translatable("gui.mundos_de_axelis.table.craft_btn"), ready, mx, my,
                        () -> press(TableMenu.B_CRAFT + idx), Component.translatable("gui.mundos_de_axelis.table.craft_tip", out.getHoverName()));
            }
        }
        if (pages > 1) {
            final int pg = craftPage;
            button(g, W - 8 - 60, 151, 14, 12, Component.literal("<"), pg > 0, mx, my, () -> craftPage--, null);
            String tx = (pg + 1) + "/" + pages;
            g.drawString(font, tx, W - 8 - 42 + (28 - font.width(tx)) / 2, 153, 0xFFB8AAD8, false);
            button(g, W - 8 - 14, 151, 14, 12, Component.literal(">"), pg < pages - 1, mx, my, () -> craftPage++, null);
        }
    }

    /** Objeto en silueta oscura (para lo que aún no se ha descubierto). */
    private void shadowItem(GuiGraphics g, ItemStack st, int x, int y) {
        RenderSystem.setShaderColor(0.03F, 0.02F, 0.06F, 1.0F);
        g.renderItem(st, x, y);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    /** Un material de la receta: icono pequeño y la cantidad (verde si ya la tienes, roja si no). Devuelve dónde sigue el siguiente. */
    private int need(GuiGraphics g, int mx, int my, int x, int y, ItemStack st, int n) {
        int have = heldCount(st.getItem());
        String txt = String.valueOf(n);
        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        g.pose().scale(0.56F, 0.56F, 1.0F);
        g.renderItem(st, 0, 0);
        g.pose().popPose();
        g.drawString(font, txt, x + 10, y + 1, have >= n ? 0xFF8CFFA8 : 0xFFFF8C8C, false);
        int w = 10 + font.width(txt);
        if (mx >= x && mx < x + w && my >= y && my < y + 10) {
            hoverStack = st;
        }
        return x + w + 3;
    }

    /** Texto que se encoge un poco si no cabe en el ancho dado. */
    private void fitText(GuiGraphics g, Component text, int x, int y, int maxW, int color) {
        int w = font.width(text);
        if (w <= maxW) {
            g.drawString(font, text, x, y, color, false);
            return;
        }
        float sc = (float) maxW / w;
        g.pose().pushPose();
        g.pose().translate(x, y + (1.0F - sc) * 4.0F, 0);
        g.pose().scale(sc, sc, 1.0F);
        g.drawString(font, text, 0, 0, color, false);
        g.pose().popPose();
    }

    /** Receta para forjar la caja: Esencia, materiales de mobs y sello del jefe anterior (verde: lo tienes, rojo: te falta). */
    private Component forgeTip(CrateKind k) {
        Minecraft mc = Minecraft.getInstance();
        net.minecraft.network.chat.MutableComponent c = Component.translatable("gui.mundos_de_axelis.table.forge_head", Component.translatable(k.titleKey()))
                .withStyle(net.minecraft.ChatFormatting.GOLD);
        int ess = com.mundosdeaxelis.crate.CrateForge.essence(k);
        c.append(Component.literal("\n").append(line(TableClient.essence >= ess, Component.translatable("gui.mundos_de_axelis.table.forge_essence", ess, TableClient.essence))));
        for (com.mundosdeaxelis.crate.CrateForge.Need n : com.mundosdeaxelis.crate.CrateForge.mats(k)) {
            net.minecraft.world.item.Item it = ModItems.MATS.get(n.mat()).get();
            int have = mc.player == null ? 0 : TableMenu.count(mc.player, it);
            c.append(Component.literal("\n").append(line(have >= n.n(), Component.translatable("gui.mundos_de_axelis.table.forge_item",
                    Component.translatable(it.getDescriptionId()), n.n(), have))));
        }
        CrateKind sk = com.mundosdeaxelis.crate.CrateForge.sealOf(k);
        if (sk != null) {
            net.minecraft.world.item.Item seal = com.mundosdeaxelis.crate.CrateForge.sealItem(sk);
            int have = mc.player == null ? 0 : TableMenu.count(mc.player, seal);
            c.append(Component.literal("\n").append(line(have >= 1, Component.translatable("gui.mundos_de_axelis.table.forge_seal", Component.translatable(sk.titleKey()), have))));
        }
        return c;
    }

    private static Component line(boolean ok, Component text) {
        return Component.literal(ok ? "\u2022 " : "\u2022 ").withStyle(ok ? net.minecraft.ChatFormatting.GREEN : net.minecraft.ChatFormatting.RED)
                .append(text.copy().withStyle(net.minecraft.ChatFormatting.WHITE));
    }

    /** Flechas y número de página. */
    private void pager(GuiGraphics g, int mx, int my, int page, int pages, int x, int y, boolean archive) {
        final int p = page;
        button(g, W - 8 - 60, y - 1, 14, 12, Component.literal("<"), p > 0, mx, my, () -> {
            if (archive) archPage--; else shopPage--;
        }, null);
        String t = (p + 1) + "/" + pages;
        g.drawString(font, t, W - 8 - 42 + (28 - font.width(t)) / 2, y + 1, 0xFFB8AAD8, false);
        button(g, W - 8 - 14, y - 1, 14, 12, Component.literal(">"), p < pages - 1, mx, my, () -> {
            if (archive) archPage++; else shopPage++;
        }, null);
    }

    // ------------------------------------------------------------------ Archivo

    private void archive(GuiGraphics g, int mx, int my) {
        Font f = font;
        for (int i = 0; i < Essence.CATS.length; i++) {
            List<Essence.Entry> es = Essence.entries(Essence.CATS[i]);
            int found = 0;
            for (Essence.Entry e : es) {
                if (TableClient.archive.contains(e.id())) {
                    found++;
                }
            }
            int tx = 8 + i * 48;
            boolean on = i == archCat;
            g.fill(tx, 41, tx + 46, 55, on ? 0xFF000000 | VIOLET : 0xFF3B2B66);
            g.fill(tx + 1, 42, tx + 45, 55, on ? 0xFF3A2866 : 0xFF1A1030);
            g.drawString(f, Component.translatable("gui.mundos_de_axelis.table.cat." + Essence.CATS[i]), tx + 3, 42, on ? 0xFFFFFF : 0xFFB8AAD8, false);
            g.drawString(f, found + "/" + es.size(), tx + 3, 49, on ? 0xFFFFFFFF : GRAY, false);
            final int idx = i;
            hots.add(new Hot(tx, 41, 46, 14, () -> {
                archCat = idx;
                archPage = 0;
            }, null));
        }
        List<Essence.Entry> es = Essence.entries(Essence.CATS[archCat]);
        int perPage = 12 * 5;
        int pages = Math.max(1, (es.size() + perPage - 1) / perPage);
        archPage = Math.max(0, Math.min(pages - 1, archPage));
        for (int i = 0; i < perPage; i++) {
            int n = archPage * perPage + i;
            if (n >= es.size()) {
                break;
            }
            Essence.Entry e = es.get(n);
            Item it = e.item();
            if (it == null) {
                continue;
            }
            int cx = 20 + (i % 12) * 18, cy = 62 + (i / 12) * 18;
            boolean known = TableClient.archive.contains(e.id());
            ItemStack st = new ItemStack(it);
            g.fill(cx, cy, cx + 17, cy + 17, 0xFF000000 | (known ? e.tier().color : 0x3B2B66));
            g.fill(cx + 1, cy + 1, cx + 16, cy + 16, 0xFF0C0818);
            boolean avail = !known && available().contains(e.id());
            if (avail) {
                // sale en alguna caja que ya puedes forjar: se ve el objeto, apagado hasta que lo consigas
                g.fill(cx, cy, cx + 17, cy + 17, 0x90000000 | e.tier().color);
                g.fill(cx + 1, cy + 1, cx + 16, cy + 16, 0xFF0C0818);
                RenderSystem.setShaderColor(0.45F, 0.45F, 0.5F, 1.0F);
                g.renderItem(st, cx + 1, cy + 1);
                RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            } else if (known) {
                // ya lo tienes: fondo claro, objeto a todo color y una marca blanca en la esquina
                g.fill(cx + 1, cy + 1, cx + 16, cy + 16, 0xFF4A4468);
                g.renderItem(st, cx + 1, cy + 1);
                g.pose().pushPose();
                g.pose().translate(0, 0, 200);
                g.drawString(f, "✔", cx + 10, cy + 8, 0xFFFFFFFF, true);
                g.pose().popPose();
            } else {
                RenderSystem.setShaderColor(0.03F, 0.02F, 0.06F, 1.0F);
                g.renderItem(st, cx + 1, cy + 1);
                RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
                g.drawString(f, "?", cx + 6, cy + 5, 0xFF6C5E8C, false);
            }
            if (mx >= cx && mx < cx + 17 && my >= cy && my < cy + 17) {
                if (known) {
                    ownedTip = st;
                    hoverStack = ItemStack.EMPTY;
                } else if (avail) {
                    hoverStack = st;
                } else {
                    hoverTip = Component.translatable("gui.mundos_de_axelis.table.unknown", e.tier().label());
                }
            }
        }
        pager(g, mx, my, archPage, pages, 8, 152, true);
        int total = 0, got = 0;
        for (Essence.Entry e : Essence.entries()) {
            total++;
            if (TableClient.archive.contains(e.id())) {
                got++;
            }
        }
        if (TableClient.toast == null || System.currentTimeMillis() - TableClient.toastTime >= 6000) {
            g.drawString(f, Component.translatable("gui.mundos_de_axelis.table.archive_total", got, total), 8, 153, 0xFFB8AAD8, false);
        }
    }

    private int availStage = -1;
    private java.util.Set<String> avail = new java.util.HashSet<>();

    /** Ids de todo lo que puede salir en las cajas que el jugador ya puede forjar (se recalcula al subir de etapa). */
    private java.util.Set<String> available() {
        int stage = com.mundosdeaxelis.crate.CrateForge.stage(TableClient.archive);
        if (stage != availStage) {
            availStage = stage;
            java.util.Set<String> out = new java.util.HashSet<>();
            for (CrateKind k : CrateKind.values()) {
                if (k == CrateKind.PETS || k.rank() > stage + 1) {
                    continue;
                }
                for (com.mundosdeaxelis.crate.CratePools.Chance c : com.mundosdeaxelis.crate.CratePools.chances(k)) {
                    ResourceLocation rl = ForgeRegistries.ITEMS.getKey(c.item());
                    if (rl != null) {
                        out.add(rl.getPath());
                    }
                }
            }
            avail = out;
        }
        return avail;
    }

    // ------------------------------------------------------------------ Mesa (nivel)

    private void tableTab(GuiGraphics g, int mx, int my) {
        Font f = font;
        int lv = TableClient.level;
        g.drawString(f, Component.translatable("gui.mundos_de_axelis.table.your_level", lv, Component.translatable(TableLevels.nameKey(lv))), 8, 42, 0xFF000000 | GOLD, true);
        wrap(g, Component.translatable("table.mundos_de_axelis.perk." + lv), 8, 53, W - 16, 0xFFB8AAD8);
        if (lv >= TableLevels.MAX) {
            g.drawString(f, Component.translatable("gui.mundos_de_axelis.table.max_level"), 8, 90, 0xFF8CFFA8, false);
            wrap(g, Component.translatable("gui.mundos_de_axelis.table.later"), 8, 110, W - 16, GRAY);
            return;
        }
        int to = lv + 1;
        g.drawString(f, Component.translatable("gui.mundos_de_axelis.table.next_level", to, Component.translatable(TableLevels.nameKey(to))), 8, 82, 0xFF000000 | VIOLET, true);
        wrap(g, Component.translatable("table.mundos_de_axelis.perk." + to), 8, 93, W - 16, 0xFFB8AAD8);
        int cost = TableLevels.upgradeEssence(to);
        boolean ok = TableClient.essence >= cost;
        boolean all = ok;
        g.drawString(f, Component.translatable("gui.mundos_de_axelis.table.upgrade_cost", cost, TableClient.essence), 8, 117, ok ? 0xFF8CFFA8 : 0xFFFF8C8C, false);
        int i = 0;
        Minecraft mc = Minecraft.getInstance();
        for (Map.Entry<Mats, Integer> e : TableLevels.upgradeMats(to).entrySet()) {
            Item it = mat(e.getKey());
            if (it == null) {
                continue;
            }
            int have = mc.player == null ? 0 : TableMenu.count(mc.player, it);
            boolean enough = have >= e.getValue();
            all &= enough;
            // hasta 5 materiales en una fila; con más, dos filas con iconos más pequeños
            boolean compact = TableLevels.upgradeMats(to).size() > 5;
            int ix = 8 + (i % 5) * 48, iy = compact ? 126 + (i / 5) * 13 : 128;
            ItemStack st = new ItemStack(it);
            if (compact) {
                g.pose().pushPose();
                g.pose().translate(ix, iy, 0);
                g.pose().scale(0.75F, 0.75F, 1.0F);
                g.renderItem(st, 0, 0);
                g.pose().popPose();
            } else {
                g.renderItem(st, ix, iy);
            }
            int sz = compact ? 12 : 16;
            g.drawString(f, Math.min(have, 99) + "/" + e.getValue(), ix + sz + 2, iy + (compact ? 2 : 4), enough ? 0xFF8CFFA8 : 0xFFFF8C8C, false);
            if (mx >= ix && mx < ix + sz && my >= iy && my < iy + sz) {
                hoverStack = st;
            }
            i++;
        }
        final boolean enabled = all;
        button(g, W - 8 - 112, 114, 112, 14, Component.translatable("gui.mundos_de_axelis.table.upgrade"), enabled, mx, my,
                () -> press(TableMenu.B_UPGRADE), com.mundosdeaxelis.world.BossRewards.exists(to - 1) || to < 4
                        ? Component.translatable("gui.mundos_de_axelis.table.upgrade_tip")
                        : Component.translatable("gui.mundos_de_axelis.table.upgrade_boss_missing", to - 1));
    }
}
