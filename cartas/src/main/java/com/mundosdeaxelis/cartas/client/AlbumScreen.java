package com.mundosdeaxelis.cartas.client;

import com.mojang.math.Axis;
import com.mundosdeaxelis.cartas.AlbumLogic;
import com.mundosdeaxelis.cartas.CardCategory;
import com.mundosdeaxelis.cartas.CardDef;
import com.mundosdeaxelis.cartas.CardItem;
import com.mundosdeaxelis.cartas.CardRarity;
import com.mundosdeaxelis.cartas.Cards;
import com.mundosdeaxelis.cartas.CartasNet;
import com.mundosdeaxelis.cartas.PackDef;
import com.mundosdeaxelis.cartas.PackRoller;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.ChatFormatting;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

/**
 * El album: un libro abierto SIN pestanas. Las paginas se arrastran con el raton (con fisica de resorte).
 * Las cartas nuevas que lleva el jugador aparecen en un estante abajo: se agarran y se sueltan en su hueco.
 * Todo el dibujo usa una pantalla virtual de 360 x 258 que se escala a la ventana.
 */
public final class AlbumScreen extends Screen {
    private static final int VW = 360;
    private static final int VH = 258;
    private static final int SPINE = 180;
    private static final int PW = 172;
    private static final int PAGE_Y = 4;
    private static final int PH = 200;
    private static final int PER_PAGE = 6;
    private static final int CARD_W = 44;
    private static final int CARD_H = 66;
    private static final int TRAY_Y = 222;
    private static final int TRAY_CELL = 26;
    private static final int TRAY_X = 18;
    private static final int TRAY_COUNT = 12;
    private static final float K = 190f;
    private static final float D = 21f;

    private enum Mode { IDLE, DRAG, SETTLE }

    /** una pagina del libro: tab -1 es el indice */
    private static final class PageDef {
        final int tab;
        final int first;
        final int count;

        PageDef(int tab, int first, int count) {
            this.tab = tab;
            this.first = first;
            this.count = count;
        }
    }

    /** una carta nueva en el estante */
    private static final class Entry {
        final String id;
        final boolean holo;
        int count;

        Entry(String id, boolean holo) {
            this.id = id;
            this.holo = holo;
        }
    }

    /** la carta que se esta guardando: vuela del raton a su hueco mientras el servidor confirma */
    private static final class Placing {
        String id;
        boolean holo;
        int page;
        int k;
        float fx;
        float fy;
        float t;
        boolean confirmed;
        boolean rejected;
    }

    private static final class Spark {
        float x, y, vx, vy, life, max;
        int rgb;
        boolean rainbow;
    }

    private static final class Ring {
        float x, y, t;
        int rgb;
    }

    private final Random rnd = new Random();
    private final List<PageDef> pages = new ArrayList<>();
    private int cur;
    private int target;
    private Mode mode = Mode.IDLE;
    private boolean fromDrag;
    private int leafL;
    private int flipDir;
    private float theta;
    private float vel;
    private float dest;
    private boolean crossed;
    private float dragVx;
    private float dragStartVx;
    private boolean moved;

    private final Set<String> pending = new HashSet<>();
    private Placing placing;
    private float bonusBefore;
    private float bonusAfter;
    private float bonusShow;
    private float shake;
    private final List<Spark> sparks = new ArrayList<>();
    private final List<Ring> rings = new ArrayList<>();

    private List<Entry> tray = new ArrayList<>();
    private int trayScroll;
    private Entry held;
    private boolean heldSticky;
    private float heldX;
    private float heldY;
    private float heldDownX;
    private float heldDownY;
    private float edgeTimer;
    private CardDef zoom;
    private boolean zoomHolo;
    private float zoomT;
    private int[] clickSlot;
    private int badPage = -1;
    private int badK;
    private float badT;

    private long lastNs = System.nanoTime();
    private float time;
    private float sc = 1f;
    private float ox;
    private float oy;
    private float mvx;
    private float mvy;

    public AlbumScreen() {
        super(Component.translatable("item.mundos_de_axelis.album_cartas"));
        pages.add(new PageDef(-1, 0, 0));
        for (int t = 0; t < tabCount(); t++) {
            int n = tabCards(t).size();
            for (int f = 0; f < n; f += PER_PAGE) {
                pages.add(new PageDef(t, f, Math.min(PER_PAGE, n - f)));
            }
        }
    }

    public void refresh() {
    }

    private int tabCount() {
        return Cards.CATEGORIES.size() + 1;
    }

    private boolean holoTab(int t) {
        return t == Cards.CATEGORIES.size();
    }

    private List<CardDef> tabCards(int t) {
        return holoTab(t) ? Cards.ALL : Cards.CATEGORIES.get(t).cards;
    }

    private int spreads() {
        return (pages.size() + 1) / 2;
    }

    private int firstPageOf(int t) {
        for (int i = 0; i < pages.size(); i++) {
            if (pages.get(i).tab == t) {
                return i;
            }
        }
        return 0;
    }

    private boolean owned(int t, CardDef d) {
        AlbumLogic a = CartasClient.ALBUM;
        boolean h = holoTab(t);
        return (h ? a.holo : a.normal).contains(d.id) && !pending.contains(key(d.id, h));
    }

    private static String key(String id, boolean holo) {
        return id + (holo ? "|h" : "|n");
    }

    private int categoryIndex(String id) {
        CardDef d = Cards.get(id);
        for (int i = 0; i < Cards.CATEGORIES.size(); i++) {
            if (Cards.CATEGORIES.get(i).id.equals(d.category)) {
                return i;
            }
        }
        return 0;
    }

    /** pagina del libro donde va esta carta */
    private int pageOf(String id, boolean holo) {
        int t = holo ? Cards.CATEGORIES.size() : categoryIndex(id);
        int idx = tabCards(t).indexOf(Cards.get(id));
        return firstPageOf(t) + idx / PER_PAGE;
    }

    // ------------------------------------------------------------------ estante y colocacion

    private void refreshTray() {
        Minecraft mc = Minecraft.getInstance();
        Map<String, Entry> m = new LinkedHashMap<>();
        if (mc.player != null) {
            List<ItemStack> all = new ArrayList<>(mc.player.getInventory().items);
            all.addAll(mc.player.getInventory().offhand);
            for (ItemStack s : all) {
                if (s.isEmpty() || !(s.getItem() instanceof CardItem ci)) {
                    continue;
                }
                boolean h = CardItem.isHolo(s);
                AlbumLogic a = CartasClient.ALBUM;
                if ((h ? a.holo : a.normal).contains(ci.def.id) || pending.contains(key(ci.def.id, h))) {
                    continue;
                }
                m.computeIfAbsent(key(ci.def.id, h), k -> new Entry(ci.def.id, h)).count += s.getCount();
            }
        }
        List<Entry> l = new ArrayList<>();
        for (int pass = 0; pass < 2; pass++) {
            for (CardDef d : Cards.ALL) {
                Entry e = m.get(key(d.id, pass == 1));
                if (e != null) {
                    l.add(e);
                }
            }
        }
        tray = l;
        trayScroll = Mth.clamp(trayScroll, 0, Math.max(0, tray.size() - TRAY_COUNT));
    }

    private Entry trayAt(float vx, float vy) {
        if (vy < TRAY_Y || vy >= TRAY_Y + 34) {
            return null;
        }
        int i = (int) Math.floor((vx - TRAY_X) / TRAY_CELL);
        if (vx < TRAY_X || i < 0 || i >= TRAY_COUNT || i + trayScroll >= tray.size()) {
            return null;
        }
        return tray.get(i + trayScroll);
    }

    /** el hueco que hay bajo el raton en las dos paginas visibles: {pagina, k} o null */
    private int[] slotAt(float vx, float vy) {
        if (mode != Mode.IDLE) {
            return null;
        }
        for (int pg = cur * 2; pg <= cur * 2 + 1; pg++) {
            if (pg >= pages.size() || pages.get(pg).tab < 0) {
                continue;
            }
            PageDef pd = pages.get(pg);
            for (int k = 0; k < pd.count; k++) {
                float[] pos = slotPos(pg, k);
                if (vx >= pos[0] - 3 && vx < pos[0] + CARD_W + 3 && vy >= pos[1] - 3 && vy < pos[1] + CARD_H + 3) {
                    return new int[] {pg, k};
                }
            }
        }
        return null;
    }

    private void drop(float vx, float vy) {
        Entry e = held;
        held = null;
        heldSticky = false;
        if (e == null) {
            return;
        }
        int[] s = slotAt(vx, vy);
        if (s != null) {
            PageDef pd = pages.get(s[0]);
            CardDef d = tabCards(pd.tab).get(pd.first + s[1]);
            if (d.id.equals(e.id) && holoTab(pd.tab) == e.holo) {
                Placing p = new Placing();
                p.id = e.id;
                p.holo = e.holo;
                p.page = s[0];
                p.k = s[1];
                p.fx = vx;
                p.fy = vy;
                placing = p;
                CartasNet.CHANNEL.sendToServer(new CartasNet.PlaceRequest(e.id, e.holo));
                CartasClient.play(SoundEvents.BOOK_PAGE_TURN, 1.6f, 0.3f);
                return;
            }
            badPage = s[0];
            badK = s[1];
            badT = 0f;
            CartasClient.play(SoundEvents.VILLAGER_NO, 1.3f, 0.45f);
        }
    }

    /** respuesta del servidor a la colocacion */
    public void onInserted(CartasNet.InsertResult m) {
        if (placing == null) {
            return;
        }
        if (m.ids().isEmpty()) {
            placing.rejected = true;
            return;
        }
        placing.confirmed = true;
        pending.add(key(placing.id, placing.holo));
        bonusBefore = m.before();
        bonusAfter = m.after();
    }

    private void tickPlacing(float dt) {
        if (placing == null) {
            return;
        }
        placing.t = Math.min(1f, placing.t + dt / 0.26f);
        if (placing.rejected) {
            CartasClient.play(SoundEvents.VILLAGER_NO, 1.2f, 0.5f);
            placing = null;
            return;
        }
        if (placing.confirmed && placing.t >= 1f) {
            land(placing);
            placing = null;
        }
    }

    private void land(Placing f) {
        pending.remove(key(f.id, f.holo));
        CardDef d = Cards.get(f.id);
        float[] c = slotCenter(f.page, f.k);
        shake = f.holo || d.rarity.atLeast(CardRarity.LEGENDARIA) ? 0.45f : 0.22f;
        CartasClient.play(SoundEvents.BOOK_PUT, 0.75f + rnd.nextFloat() * 0.2f, 0.9f);
        int n = 10 + d.rarity.ordinal() * 8 + (f.holo ? 30 : 0);
        for (int i = 0; i < n; i++) {
            Spark s = new Spark();
            float a = rnd.nextFloat() * Mth.TWO_PI;
            float v = 25f + rnd.nextFloat() * (50f + d.rarity.ordinal() * 15f);
            s.x = c[0];
            s.y = c[1];
            s.vx = Mth.cos(a) * v;
            s.vy = Mth.sin(a) * v - 15f;
            s.max = s.life = 0.5f + rnd.nextFloat() * 0.6f;
            s.rgb = d.rarity.color;
            s.rainbow = f.holo;
            sparks.add(s);
        }
        Ring r = new Ring();
        r.x = c[0];
        r.y = c[1];
        r.rgb = f.holo ? 0x7FE8FF : d.rarity.color;
        rings.add(r);
        if (d.rarity.atLeast(CardRarity.EPICA)) {
            CartasClient.play(SoundEvents.AMETHYST_BLOCK_CHIME, 1.1f + d.rarity.ordinal() * 0.12f, 0.8f);
        }
        if (d.rarity.atLeast(CardRarity.LEGENDARIA)) {
            CartasClient.play(SoundEvents.PLAYER_LEVELUP, 1.4f, 0.4f);
        }
        if (d.rarity == CardRarity.MITICA) {
            CartasClient.play(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 0.6f);
        }
        if (f.holo) {
            CartasClient.play(SoundEvents.AMETHYST_BLOCK_RESONATE, 1.3f, 0.9f);
        }
        bonusShow = 4.5f;
    }

    // ------------------------------------------------------------------ paginas

    private void startLeaf(int dir, Mode m) {
        fromDrag = m == Mode.DRAG;
        flipDir = dir;
        leafL = dir > 0 ? cur : cur - 1;
        theta = dir > 0 ? 0f : Mth.PI;
        vel = 0f;
        mode = m;
        crossed = false;
        if (m == Mode.SETTLE) {
            dest = dir > 0 ? Mth.PI : 0f;
            vel = dir * 5f;
        }
    }

    private void updateLeaf(float dt) {
        if (mode == Mode.IDLE) {
            if (target != cur) {
                int dir = target > cur ? 1 : -1;
                startLeaf(dir, Mode.SETTLE);
            }
            if (mode == Mode.IDLE) {
                return;
            }
        }
        float goal = dest;
        if (mode == Mode.DRAG) {
            float c = Mth.clamp((dragVx - SPINE) / PW, -1f, 1f);
            goal = (float) Math.acos(c);
        }
        int sub = 4;
        float h = dt / sub;
        // si hay que cruzar varias paginas, el resorte es mas rapido
        float kMul = mode == Mode.SETTLE && Math.abs(target - cur) > 1 ? 2.4f : 1f;
        float kk = K * kMul;
        float dd = D * (float) Math.sqrt(kMul);
        for (int i = 0; i < sub; i++) {
            float acc = kk * (goal - theta) - dd * vel;
            vel += acc * h;
            theta += vel * h;
            if (theta < 0f) {
                theta = 0f;
                vel = Math.max(vel, 0f) * -0.2f;
            } else if (theta > Mth.PI) {
                theta = Mth.PI;
                vel = Math.min(vel, 0f) * -0.2f;
            }
        }
        if (!crossed && (theta - Mth.HALF_PI) * (flipDir > 0 ? 1 : -1) > 0f && mode != Mode.IDLE) {
            crossed = true;
            CartasClient.play(SoundEvents.BOOK_PAGE_TURN, 0.9f + rnd.nextFloat() * 0.25f, Mth.clamp(Math.abs(vel) / 8f, 0.25f, 0.9f));
        }
        if (mode == Mode.SETTLE && Math.abs(theta - dest) < 0.012f && Math.abs(vel) < 0.35f) {
            theta = dest;
            finishLeaf();
        }
    }

    private void finishLeaf() {
        cur = dest > Mth.HALF_PI ? leafL + 1 : leafL;
        if (fromDrag) {
            target = cur;
        }
        fromDrag = false;
        mode = Mode.IDLE;
        crossed = false;
        CartasClient.play(SoundEvents.BOOK_PUT, 1.6f, 0.18f);
    }

    private void flip(int dir) {
        if (mode != Mode.IDLE) {
            return;
        }
        target = Mth.clamp(target + dir, 0, spreads() - 1);
    }

    // ------------------------------------------------------------------ dibujo

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        long now = System.nanoTime();
        float dt = Mth.clamp((now - lastNs) / 1e9f, 0.0001f, 0.05f);
        lastNs = now;
        time += dt;
        shake = Math.max(0f, shake - dt);
        bonusShow = Math.max(0f, bonusShow - dt);
        badT = Math.min(1f, badT + dt * 2.5f);
        zoomT = zoom != null ? Math.min(1f, zoomT + dt * 6f) : 0f;
        refreshTray();
        tickPlacing(dt);
        updateLeaf(dt);
        tickFx(dt);

        renderBackground(g);
        sc = Math.min(width / (float) VW, height / (float) VH) * 0.96f;
        ox = (width - VW * sc) / 2f;
        oy = (height - VH * sc) / 2f;
        float vx = (mx - ox) / sc;
        float vy = (my - oy) / sc;
        mvx = vx;
        mvy = vy;
        if (held != null) {
            heldX = vx;
            heldY = vy;
            // al acercar la carta al borde, el libro pasa de pagina solo
            if (mode == Mode.IDLE && (vx < 26f || vx > VW - 26f) && vy < TRAY_Y) {
                edgeTimer += dt;
                if (edgeTimer > 0.55f) {
                    edgeTimer = 0f;
                    flip(vx < 26f ? -1 : 1);
                }
            } else {
                edgeTimer = 0f;
            }
        }

        g.pose().pushPose();
        g.pose().translate(ox, oy, 0f);
        g.pose().scale(sc, sc, 1f);
        if (shake > 0f) {
            g.pose().translate(Mth.sin(time * 90f) * shake * 3f, Mth.cos(time * 77f) * shake * 2f, 0f);
        }

        drawBook(g);
        List<Component> tip = null;
        if (mode == Mode.IDLE && held == null && placing == null && zoom == null) {
            tip = hoverTooltip(vx, vy);
        }
        drawTray(g, vx, vy);
        if (tip == null && held == null && mode == Mode.IDLE && zoom == null) {
            Entry e = trayAt(vx, vy);
            if (e != null) {
                tip = trayTooltip(e);
            }
        }
        drawFooter(g);
        drawFx(g);
        drawPlacing(g);
        drawHeld(g);
        drawZoom(g);
        g.pose().popPose();

        if (tip != null) {
            g.renderComponentTooltip(font, tip, mx, my);
        }
    }

    private void drawBook(GuiGraphics g) {
        // tapa
        g.fill(2, PAGE_Y - 2, VW - 2, PAGE_Y + PH + 4, 0xFF4A2E1A);
        g.fill(4, PAGE_Y, VW - 4, PAGE_Y + PH + 2, 0xFF6B4528);
        CardRender.border(g, 2, PAGE_Y - 2, VW - 4, PH + 6, 0xFF2E1C0F);

        int leftPage;
        int rightPage;
        if (mode == Mode.IDLE) {
            leftPage = cur * 2;
            rightPage = cur * 2 + 1;
        } else {
            leftPage = leafL * 2;
            rightPage = (leafL + 1) * 2 + 1;
        }
        drawPage(g, leftPage, SPINE - PW, true);
        drawPage(g, rightPage, SPINE, false);
        if (mode != Mode.IDLE) {
            drawLeaf(g);
        }
        // lomo
        for (int i = 0; i < 10; i++) {
            int a = (int) (0x55 * (1f - i / 10f));
            g.fill(SPINE - 1 - i, PAGE_Y, SPINE - i, PAGE_Y + PH, a << 24);
            g.fill(SPINE + i, PAGE_Y, SPINE + i + 1, PAGE_Y + PH, a << 24);
        }
        g.fill(SPINE - 1, PAGE_Y, SPINE + 1, PAGE_Y + PH, 0x66000000);
    }

    private void drawLeaf(GuiGraphics g) {
        float c = Mth.cos(theta);
        float s = Mth.sin(theta);
        boolean front = c >= 0f;
        float sx = Math.max(Math.abs(c), 0.003f);
        int page = front ? (leafL * 2 + 1) : ((leafL + 1) * 2);
        int px = front ? SPINE : SPINE - PW;
        // sombra que la hoja proyecta sobre la pagina de abajo
        float edge = SPINE + c * PW;
        for (int i = 0; i < 14; i++) {
            int a = (int) (0x50 * s * Math.pow(1f - i / 14f, 2));
            int xa = (int) (front ? edge + i : edge - i - 1);
            if (xa >= 6 && xa < VW - 6) {
                g.fill(xa, PAGE_Y, xa + 1, PAGE_Y + PH, a << 24);
            }
        }
        g.pose().pushPose();
        g.pose().translate(SPINE, 0f, 0f);
        g.pose().scale(sx, 1f, 1f);
        g.pose().translate(-SPINE, 0f, 0f);
        drawPage(g, page, px, !front);
        g.fill(px, PAGE_Y, px + PW, PAGE_Y + PH, ((int) (s * 90) << 24));
        int ex = front ? px + PW - 3 : px;
        g.fill(ex, PAGE_Y, ex + 3, PAGE_Y + PH, 0xFFFFF4D8);
        g.pose().popPose();
    }

    private void drawPage(GuiGraphics g, int page, int px, boolean left) {
        g.fill(px, PAGE_Y, px + PW, PAGE_Y + PH, 0xFFEADFC4);
        g.fill(px + 3, PAGE_Y + 3, px + PW - 3, PAGE_Y + PH - 3, 0xFFF1E8D0);
        CardRender.border(g, px + 2, PAGE_Y + 2, PW - 4, PH - 4, 0x40A88A5A);
        // sombreado hacia el lomo
        for (int i = 0; i < 16; i++) {
            int a = (int) (0x30 * (1f - i / 16f));
            int xx = left ? px + PW - 1 - i : px + i;
            g.fill(xx, PAGE_Y, xx + 1, PAGE_Y + PH, a << 24);
        }
        if (page < 0 || page >= pages.size()) {
            g.drawCenteredString(font, "✦", px + PW / 2, PAGE_Y + PH / 2 - 4, 0x30A88A5A);
            return;
        }
        PageDef pd = pages.get(page);
        if (pd.tab < 0) {
            drawIndex(g, px);
            return;
        }
        int t = pd.tab;
        List<CardDef> cards = tabCards(t);
        boolean holo = holoTab(t);
        int tabColor = holo ? 0x4FC3E8 : Cards.CATEGORIES.get(t).color;
        int have = 0;
        for (CardDef d : cards) {
            if (owned(t, d)) {
                have++;
            }
        }
        String title = holo ? "✦ Holográficas" : Cards.CATEGORIES.get(t).name;
        if (pd.first > 0) {
            title += " (cont.)";
        }
        g.drawString(font, title, px + 12, PAGE_Y + 10, 0xFF000000 | darker(tabColor), false);
        String cnt = have + "/" + cards.size();
        g.drawString(font, cnt, px + PW - 12 - font.width(cnt), PAGE_Y + 10, 0xFF6B4A2A, false);
        g.fill(px + 12, PAGE_Y + 22, px + PW - 12, PAGE_Y + 23, 0xFF000000 | tabColor);
        for (int k = 0; k < pd.count; k++) {
            drawSlot(g, page, k);
        }
        g.drawCenteredString(font, String.valueOf(page), px + PW / 2, PAGE_Y + PH - 14, 0xFF8A6A42);
    }

    private void drawIndex(GuiGraphics g, int px) {
        g.drawString(font, "Álbum de Cartas", px + 12, PAGE_Y + 10, 0xFF5A3A1E, false);
        g.fill(px + 12, PAGE_Y + 22, px + PW - 12, PAGE_Y + 23, 0xFF8A6A42);
        int y = PAGE_Y + 32;
        for (int t = 0; t < tabCount(); t++) {
            boolean holo = holoTab(t);
            int col = holo ? 0x4FC3E8 : Cards.CATEGORIES.get(t).color;
            int have = 0;
            for (CardDef d : tabCards(t)) {
                if (owned(t, d)) {
                    have++;
                }
            }
            g.fill(px + 14, y + 1, px + 20, y + 7, 0xFF000000 | col);
            String name = holo ? "Holográficas" : Cards.CATEGORIES.get(t).name;
            g.drawString(font, name, px + 25, y, 0xFF4A2E1A, false);
            String right = have + "/" + tabCards(t).size() + "  p." + firstPageOf(t);
            g.drawString(font, right, px + PW - 14 - font.width(right), y, 0xFF8A6A42, false);
            y += 14;
        }
        g.fill(px + 12, y + 2, px + PW - 12, y + 3, 0x60A88A5A);
        y += 10;
        String[] help = {
            "Las cartas nuevas que llevas aparecen en el estante de abajo: agárralas y suéltalas en su hueco.",
            "Cada 5 % del álbum: +½ corazón.",
            "Categoría completa: +1 corazón.",
            "Clic en una carta guardada para verla en grande."
        };
        for (String h : help) {
            for (var line : font.split(Component.literal(h), PW - 28)) {
                g.drawString(font, line, px + 14, y, 0xFF6B4A2A, false);
                y += 10;
            }
            y += 3;
        }
    }

    private static int darker(int rgb) {
        int r = (rgb >> 16 & 255) * 3 / 5;
        int gg = (rgb >> 8 & 255) * 3 / 5;
        int b = (rgb & 255) * 3 / 5;
        return r << 16 | gg << 8 | b;
    }

    private float[] slotPos(int page, int k) {
        int px = page % 2 == 1 ? SPINE : SPINE - PW;
        float x = px + 11 + (k % 3) * (CARD_W + 10);
        float y = PAGE_Y + 34 + (k / 3) * (CARD_H + 14);
        return new float[] {x, y};
    }

    private float[] slotCenter(int page, int k) {
        float[] p = slotPos(page, k);
        return new float[] {p[0] + CARD_W / 2f, p[1] + CARD_H / 2f};
    }

    private int hoverPage = -1;
    private int hoverK = -1;

    private void drawSlot(GuiGraphics g, int page, int k) {
        PageDef pd = pages.get(page);
        int t = pd.tab;
        CardDef d = tabCards(t).get(pd.first + k);
        float[] pos = slotPos(page, k);
        float x = pos[0];
        float y = pos[1];
        if (page == badPage && k == badK && badT < 1f) {
            x += Mth.sin(badT * 40f) * 3f * (1f - badT);
        }
        boolean own = owned(t, d);
        boolean hov = page == hoverPage && k == hoverK && mode == Mode.IDLE && held == null;
        int rc = d.rarity.color;
        // hueco
        g.fill((int) x - 2, (int) y - 2, (int) x + CARD_W + 2, (int) y + CARD_H + 2, 0x30301808);
        CardRender.border(g, (int) x - 2, (int) y - 2, CARD_W + 4, CARD_H + 4, (own ? 0xC0 : 0x50) << 24 | rc);
        float lift = hov ? 1.07f : 1f;
        float w = CARD_W * lift;
        float h = CARD_H * lift;
        float cx = x - (w - CARD_W) / 2f;
        float cy = y - (h - CARD_H) / 2f - (hov ? 2f : 0f);
        if (own) {
            if (hov) {
                CardRender.glow(g, cx, cy, w, h, rc, 0.8f);
            }
            CardRender.face(g, d, cx, cy, w, h);
            if (holoTab(t)) {
                CardRender.holo(g, cx, cy, w, h, time, (d.index * 0.137f) % 1f);
            } else if (CartasClient.ALBUM.holo.contains(d.id)) {
                g.drawString(font, "✦", (int) x + CARD_W - 9, (int) y + 2, 0xFF7FE8FF, true);
            }
        } else {
            CardRender.back(g, cx, cy, w, h, 0.5f);
            g.fill((int) cx, (int) cy, (int) (cx + w), (int) (cy + h), 0x70101018);
            g.drawCenteredString(font, "?", (int) (cx + w / 2), (int) (cy + h / 2 - 4), 0xFFB8A888);
            g.fill((int) (cx + w / 2) - 3, (int) (cy + h) - 8, (int) (cx + w / 2) + 3, (int) (cy + h) - 5, 0xFF000000 | rc);
            // si llevas en la mano la carta de este hueco, el hueco late
            if (held != null && placing == null && d.id.equals(held.id) && holoTab(t) == held.holo) {
                int a = (int) (110 + 120 * (0.5f + 0.5f * Mth.sin(time * 7f)));
                CardRender.border(g, (int) x - 3, (int) y - 3, CARD_W + 6, CARD_H + 6, (a << 24) | 0xFFFFFF);
                CardRender.border(g, (int) x - 2, (int) y - 2, CARD_W + 4, CARD_H + 4, (a << 24) | rc);
            }
        }
    }

    private void drawTray(GuiGraphics g, float vx, float vy) {
        // estante de madera
        g.fill(2, TRAY_Y - 2, VW - 2, VH - 2, 0xFF3A2414);
        g.fill(4, TRAY_Y, VW - 4, VH - 4, 0xFF5A3A1E);
        g.fill(4, TRAY_Y, VW - 4, TRAY_Y + 1, 0xFF7A5230);
        CardRender.border(g, 2, TRAY_Y - 2, VW - 4, VH - TRAY_Y, 0xFF2E1C0F);
        if (tray.isEmpty()) {
            g.drawCenteredString(font, "No llevas cartas nuevas. Abre sobres o derrota jefes.", VW / 2, TRAY_Y + 14, 0xFFB8A888);
            return;
        }
        if (trayScroll > 0) {
            g.drawString(font, "◀", 7, TRAY_Y + 14, 0xFFE8DCC8, true);
        }
        if (trayScroll + TRAY_COUNT < tray.size()) {
            g.drawString(font, "▶", VW - 14, TRAY_Y + 14, 0xFFE8DCC8, true);
        }
        Entry hovE = held == null && mode == Mode.IDLE ? trayAt(vx, vy) : null;
        for (int i = 0; i < TRAY_COUNT && i + trayScroll < tray.size(); i++) {
            Entry e = tray.get(i + trayScroll);
            CardDef d = Cards.get(e.id);
            float x = TRAY_X + i * TRAY_CELL + 1;
            float y = TRAY_Y + 3 + (e == hovE ? -2f : 0f);
            g.fill((int) x - 1, TRAY_Y + 2, (int) x + 21, TRAY_Y + 34, 0x40000000);
            if (e == held) {
                g.fill((int) x, TRAY_Y + 3, (int) x + 20, TRAY_Y + 33, 0x60000000);
                continue;
            }
            if (e == hovE) {
                CardRender.glow(g, x, y, 20, 30, e.holo ? 0x7FE8FF : d.rarity.color, 0.6f);
            }
            CardRender.face(g, d, x, y, 20, 30);
            if (e.holo) {
                CardRender.holo(g, x, y, 20, 30, time, (d.index * 0.137f) % 1f);
            }
            if (e.count > 1) {
                String c = "x" + e.count;
                g.pose().pushPose();
                g.pose().translate(x + 20 - font.width(c) * 0.7f, y + 22, 200f);
                g.pose().scale(0.7f, 0.7f, 1f);
                g.drawString(font, c, 0, 0, 0xFFFFFFFF, true);
                g.pose().popPose();
            }
        }
    }

    private void drawFooter(GuiGraphics g) {
        AlbumLogic a = CartasClient.ALBUM;
        int shown = a.filled();
        for (String k : pending) {
            boolean h = k.endsWith("|h");
            if ((h ? a.holo : a.normal).contains(k.substring(0, k.length() - 2))) {
                shown--;
            }
        }
        double pct = 100.0 * shown / Math.max(1, AlbumLogic.totalSlots());
        String left = String.format("Álbum %.0f %% (%d/%d)", pct, shown, AlbumLogic.totalSlots());
        if (placing == null) {
            left += String.format(" · +%.1f corazones", a.bonusHealth() / 2.0);
        }
        String hint;
        int hintCol = 0xFFB8A888;
        if (held != null) {
            CardDef d = Cards.get(held.id);
            int pg = pageOf(held.id, held.holo);
            hint = "Va en " + (held.holo ? "Holográficas" : Cards.category(d.category).name) + ", pág. " + pg;
            hintCol = 0xFF000000 | d.rarity.color;
        } else {
            hint = "Arrastra las páginas";
        }
        // los dos textos comparten la linea: si no caben, se encogen juntos
        float need = font.width(left) + font.width(hint) + 14f;
        float fs = Math.min(1f, (VW - 16f) / need);
        int fy = PAGE_Y + PH + 7;
        g.pose().pushPose();
        g.pose().translate(8f, fy, 0f);
        g.pose().scale(fs, fs, 1f);
        g.drawString(font, left, 0, 0, 0xFFE8DCC8, true);
        g.pose().popPose();
        g.pose().pushPose();
        g.pose().translate(VW - 8f - font.width(hint) * fs, fy, 0f);
        g.pose().scale(fs, fs, 1f);
        g.drawString(font, hint, 0, 0, hintCol, true);
        g.pose().popPose();
        if (bonusShow > 0f && bonusAfter > bonusBefore) {
            float al = Math.min(1f, bonusShow);
            float rise = (4.5f - bonusShow) * 2f;
            String txt = String.format("+%.1f corazones de vida máxima", (bonusAfter - bonusBefore) / 2f);
            int a2 = (int) (al * 255);
            g.drawCenteredString(font, txt, VW / 2, (int) (PAGE_Y + PH - 26 - rise), (a2 << 24) | 0xFF6A8A);
        }
    }

    // ------------------------------------------------------------------ efectos

    private void tickFx(float dt) {
        for (int i = sparks.size() - 1; i >= 0; i--) {
            Spark s = sparks.get(i);
            s.life -= dt;
            if (s.life <= 0f) {
                sparks.remove(i);
                continue;
            }
            s.vy += 140f * dt;
            s.x += s.vx * dt;
            s.y += s.vy * dt;
            s.vx *= 1f - 1.2f * dt;
        }
        for (int i = rings.size() - 1; i >= 0; i--) {
            Ring r = rings.get(i);
            r.t += dt * 2.4f;
            if (r.t >= 1f) {
                rings.remove(i);
            }
        }
    }

    private void drawFx(GuiGraphics g) {
        for (Ring r : rings) {
            float rad = 6f + r.t * 34f;
            int a = (int) ((1f - r.t) * 200);
            int c = (a << 24) | r.rgb;
            int x = (int) r.x;
            int y = (int) r.y;
            int rr = (int) rad;
            g.fill(x - rr, y - rr, x + rr, y - rr + 1, c);
            g.fill(x - rr, y + rr, x + rr + 1, y + rr + 1, c);
            g.fill(x - rr, y - rr, x - rr + 1, y + rr, c);
            g.fill(x + rr, y - rr, x + rr + 1, y + rr + 1, c);
        }
        for (Spark s : sparks) {
            float f = s.life / s.max;
            int rgb = s.rainbow ? Mth.hsvToRgb((time * 0.8f + s.x * 0.01f) % 1f, 0.6f, 1f) : s.rgb;
            int a = (int) (Mth.clamp(f * 1.5f, 0f, 1f) * 255);
            int sz = f > 0.5f ? 2 : 1;
            g.fill((int) s.x, (int) s.y, (int) s.x + sz, (int) s.y + sz, (a << 24) | rgb);
        }
    }

    private void drawCardAt(GuiGraphics g, CardDef d, boolean holo, float x, float y, float scale, float rot, float glow) {
        g.pose().pushPose();
        g.pose().translate(x, y, 300f);
        g.pose().mulPose(Axis.ZP.rotationDegrees(rot));
        g.pose().scale(scale, scale, 1f);
        float w = CARD_W;
        float h = CARD_H;
        CardRender.glow(g, -w / 2, -h / 2, w, h, holo ? 0x7FE8FF : d.rarity.color, glow);
        CardRender.face(g, d, -w / 2, -h / 2, w, h);
        if (holo) {
            CardRender.holo(g, -w / 2, -h / 2, w, h, time, 0.3f);
        }
        g.pose().popPose();
    }

    private void openZoom(CardDef d, boolean holo) {
        zoom = d;
        zoomHolo = holo;
        zoomT = 0f;
        CartasClient.play(SoundEvents.BOOK_PAGE_TURN, 1.7f, 0.5f);
    }

    private void closeZoom() {
        zoom = null;
        CartasClient.play(SoundEvents.BOOK_PUT, 1.8f, 0.25f);
    }

    private void drawZoom(GuiGraphics g) {
        if (zoom == null) {
            return;
        }
        float e = 1f - (float) Math.pow(1f - zoomT, 3);
        g.pose().pushPose();
        g.pose().translate(0f, 0f, 400f);
        g.fill(0, 0, VW, VH, ((int) (e * 190) << 24));
        CardDef d = zoom;
        int rc = zoomHolo ? 0x7FE8FF : d.rarity.color;
        float cw = 124f;
        float ch = cw * CARD_H / CARD_W;
        float cx = 112f;
        float cy = VH / 2f - 4f;
        float s = Mth.lerp(e, 0.55f, 1f);
        float tilt = Mth.clamp((mvx - cx) / 160f, -1f, 1f) * 5f;
        g.pose().pushPose();
        g.pose().translate(cx, cy, 0f);
        g.pose().mulPose(Axis.ZP.rotationDegrees(tilt * 0.6f));
        g.pose().scale(s, s, 1f);
        CardRender.glow(g, -cw / 2, -ch / 2, cw, ch, rc, 0.9f + 0.2f * Mth.sin(time * 3f));
        CardRender.face(g, d, -cw / 2, -ch / 2, cw, ch);
        if (zoomHolo) {
            CardRender.holo(g, -cw / 2, -ch / 2, cw, ch, time, (d.index * 0.137f) % 1f);
        }
        g.pose().popPose();
        // textos
        float a = Mth.clamp((zoomT - 0.3f) / 0.7f, 0f, 1f);
        int alpha = (int) (a * 255) << 24;
        if (a > 0.05f) {
            int x = 196;
            int y = 70;
            CardCategory cat = Cards.category(d.category);
            g.pose().pushPose();
            g.pose().translate(x, y, 0f);
            float ns = Math.min(1.5f, (VW - x - 8f) / Math.max(1, font.width(d.name)));
            g.pose().scale(ns, ns, 1f);
            g.drawString(font, d.name, 0, 0, alpha | d.rarity.color, true);
            g.pose().popPose();
            y += 20;
            g.drawString(font, d.rarity.label + " · " + (zoomHolo ? "Holográfica" : cat.name), x, y, alpha | rc, true);
            y += 16;
            for (var line : font.split(Component.literal(d.desc), 140)) {
                g.drawString(font, line, x, y, alpha | 0xC8C0B0, true);
                y += 11;
            }
            y += 8;
            if (!zoomHolo && CartasClient.ALBUM.holo.contains(d.id)) {
                g.drawString(font, "✦ También tienes la holográfica", x, y, alpha | 0x7FE8FF, true);
                y += 12;
            }
            y += 4;
            g.drawString(font, "Probabilidad por sobre:", x, y, alpha | 0x8AA0A8, true);
            y += 11;
            for (PackDef p : Cards.PACKS) {
                g.drawString(font, p.name + ": " + String.format("%.1f %%", PackRoller.chanceInPack(p, d) * 100.0), x, y, alpha | 0xA8B8C0, true);
                y += 10;
            }
            g.drawCenteredString(font, "Clic para cerrar", VW / 2, VH - 14, ((int) (a * 160) << 24) | 0xE8DCC8);
        }
        g.pose().popPose();
    }

    private void drawPlacing(GuiGraphics g) {
        if (placing == null) {
            return;
        }
        CardDef d = Cards.get(placing.id);
        float[] end = slotCenter(placing.page, placing.k);
        float e = 1f - (float) Math.pow(1f - Mth.clamp(placing.t, 0f, 1f), 3);
        drawCardAt(g, d, placing.holo, Mth.lerp(e, placing.fx, end[0]), Mth.lerp(e, placing.fy, end[1]), Mth.lerp(e, 0.9f, 1.0f), 0f, 0.5f + e * 0.4f);
    }

    private void drawHeld(GuiGraphics g) {
        if (held == null) {
            return;
        }
        CardDef d = Cards.get(held.id);
        float sway = Mth.sin(time * 5f) * 3f;
        drawCardAt(g, d, held.holo, heldX, heldY, 0.9f, sway, 0.75f);
    }

    // ------------------------------------------------------------------ tooltip

    private List<Component> hoverTooltip(float vx, float vy) {
        hoverPage = -1;
        hoverK = -1;
        int[] s = slotAt(vx, vy);
        if (s == null) {
            return null;
        }
        PageDef pd = pages.get(s[0]);
        hoverPage = s[0];
        hoverK = s[1];
        return tooltip(pd.tab, tabCards(pd.tab).get(pd.first + s[1]));
    }

    private List<Component> trayTooltip(Entry e) {
        CardDef d = Cards.get(e.id);
        Style rs = Style.EMPTY.withColor(d.rarity.color);
        List<Component> l = new ArrayList<>();
        l.add(Component.literal((e.holo ? "✦ " : "") + d.name).withStyle(rs.withBold(true)));
        l.add(Component.literal(d.rarity.label + " · " + (e.holo ? "Holográfica" : Cards.category(d.category).name)).withStyle(rs));
        l.add(Component.literal("Agárrala y suéltala en su hueco").withStyle(ChatFormatting.GRAY));
        l.add(Component.literal("Va en la pág. " + pageOf(e.id, e.holo)).withStyle(ChatFormatting.DARK_AQUA));
        return l;
    }

    private List<Component> tooltip(int tab, CardDef d) {
        List<Component> l = new ArrayList<>();
        boolean own = owned(tab, d);
        CardCategory cat = Cards.category(d.category);
        Style rs = Style.EMPTY.withColor(d.rarity.color);
        if (own) {
            l.add(Component.literal(d.name).withStyle(rs.withBold(true)));
        } else {
            l.add(Component.literal("???").withStyle(ChatFormatting.GRAY));
        }
        l.add(Component.literal(d.rarity.label + " · " + cat.name).withStyle(rs));
        if (own) {
            l.add(Component.literal(d.desc).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        } else {
            l.add(Component.literal(holoTab(tab) ? "Todavía no tienes la versión holográfica." : "Todavía no la tienes.").withStyle(ChatFormatting.DARK_GRAY));
        }
        StringBuilder sb = new StringBuilder();
        for (PackDef p : Cards.PACKS) {
            if (sb.length() > 0) {
                sb.append("  ");
            }
            sb.append(p.name.replace("Sobre ", "").replace("de ", "")).append(' ').append(String.format("%.1f %%", PackRoller.chanceInPack(p, d) * 100.0));
        }
        l.add(Component.literal("Por sobre: " + sb).withStyle(ChatFormatting.DARK_AQUA));
        if (holoTab(tab)) {
            StringBuilder h = new StringBuilder();
            for (PackDef p : Cards.PACKS) {
                if (h.length() > 0) {
                    h.append(" / ");
                }
                h.append(String.format("%.0f %%", p.holo * 100.0));
            }
            l.add(Component.literal("Holográfica: " + h + " por carta").withStyle(Style.EMPTY.withColor(0x7FE8FF)));
        }
        return l;
    }

    // ------------------------------------------------------------------ entrada

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        float vx = (float) ((mx - ox) / sc);
        float vy = (float) ((my - oy) / sc);
        if (zoom != null) {
            closeZoom();
            return true;
        }
        if (button != 0 || placing != null) {
            return super.mouseClicked(mx, my, button);
        }
        if (held != null) {
            // carta "pegada" al raton: este clic la suelta
            drop(vx, vy);
            return true;
        }
        if (mode == Mode.IDLE) {
            Entry e = trayAt(vx, vy);
            if (e != null) {
                held = e;
                heldSticky = false;
                heldX = vx;
                heldY = vy;
                heldDownX = vx;
                heldDownY = vy;
                CartasClient.play(SoundEvents.BOOK_PAGE_TURN, 1.9f, 0.3f);
                return true;
            }
        }
        if (mode == Mode.IDLE && vy >= PAGE_Y && vy < PAGE_Y + PH && vx >= 6 && vx < VW - 6) {
            int dir = vx >= SPINE ? 1 : -1;
            clickSlot = slotAt(vx, vy);
            if ((dir > 0 && cur + 1 < spreads()) || (dir < 0 && cur > 0)) {
                startLeaf(dir, Mode.DRAG);
                dragVx = vx;
                dragStartVx = vx;
                moved = false;
                return true;
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (mode == Mode.DRAG) {
            dragVx = (float) ((mx - ox) / sc);
            if (Math.abs(dragVx - dragStartVx) > 4f) {
                moved = true;
            }
            return true;
        }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        float vx = (float) ((mx - ox) / sc);
        float vy = (float) ((my - oy) / sc);
        if (held != null && !heldSticky && button == 0) {
            float dist = Math.max(Math.abs(vx - heldDownX), Math.abs(vy - heldDownY));
            if (dist < 5f) {
                heldSticky = true;
            } else {
                drop(vx, vy);
            }
            return true;
        }
        if (mode == Mode.DRAG && button == 0 && !moved && clickSlot != null) {
            // un clic sin arrastrar sobre una carta guardada: se ve en grande en vez de pasar de pagina
            PageDef pd = pages.get(clickSlot[0]);
            CardDef d = tabCards(pd.tab).get(pd.first + clickSlot[1]);
            if (owned(pd.tab, d)) {
                mode = Mode.IDLE;
                fromDrag = false;
                target = cur;
                theta = 0f;
                vel = 0f;
                openZoom(d, holoTab(pd.tab));
                return true;
            }
        }
        if (mode == Mode.DRAG && button == 0) {
            mode = Mode.SETTLE;
            if (!moved) {
                dest = flipDir > 0 ? Mth.PI : 0f;
                vel = flipDir * 6f;
            } else if (vel > 5f) {
                dest = Mth.PI;
            } else if (vel < -5f) {
                dest = 0f;
            } else {
                dest = theta > Mth.HALF_PI ? Mth.PI : 0f;
            }
            return true;
        }
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        if (delta == 0 || zoom != null) {
            return false;
        }
        float vy = (float) ((my - oy) / sc);
        if (vy >= TRAY_Y && held == null) {
            trayScroll = Mth.clamp(trayScroll + (delta < 0 ? 1 : -1), 0, Math.max(0, tray.size() - TRAY_COUNT));
            return true;
        }
        flip(delta < 0 ? 1 : -1);
        return true;
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (zoom != null && (key == GLFW.GLFW_KEY_ESCAPE || CartasKeys.OPEN.matches(key, scan))) {
            closeZoom();
            return true;
        }
        if (key == GLFW.GLFW_KEY_ESCAPE && held != null) {
            held = null;
            heldSticky = false;
            return true;
        }
        if (CartasKeys.OPEN.matches(key, scan)) {
            onClose();
            return true;
        }
        if (key == GLFW.GLFW_KEY_RIGHT || key == GLFW.GLFW_KEY_D) {
            flip(1);
            return true;
        }
        if (key == GLFW.GLFW_KEY_LEFT || key == GLFW.GLFW_KEY_A) {
            flip(-1);
            return true;
        }
        return super.keyPressed(key, scan, mods);
    }
}
