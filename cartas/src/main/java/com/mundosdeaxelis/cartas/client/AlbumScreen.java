package com.mundosdeaxelis.cartas.client;

import com.mojang.math.Axis;
import com.mundosdeaxelis.cartas.AlbumLogic;
import com.mundosdeaxelis.cartas.CardCategory;
import com.mundosdeaxelis.cartas.CardDef;
import com.mundosdeaxelis.cartas.CardRarity;
import com.mundosdeaxelis.cartas.Cards;
import com.mundosdeaxelis.cartas.CartasNet;
import com.mundosdeaxelis.cartas.PackDef;
import com.mundosdeaxelis.cartas.PackRoller;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.ChatFormatting;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;

/**
 * El album: un libro abierto con pestanas. Las paginas se arrastran con el raton (con fisica de resorte),
 * y al guardar cartas vuelan una a una hasta su hueco con sonido y chispas.
 * Todo el dibujo usa una pantalla virtual de 360 x 250 que se escala a la ventana.
 */
public final class AlbumScreen extends Screen {
    private static final int VW = 360;
    private static final int VH = 250;
    private static final int SPINE = 180;
    private static final int PW = 172;
    private static final int PAGE_Y = 22;
    private static final int PH = 208;
    private static final int PER_PAGE = 6;
    private static final int CARD_W = 44;
    private static final int CARD_H = 66;
    private static final float K = 190f;
    private static final float D = 21f;

    private enum Mode { IDLE, DRAG, SETTLE }

    private static final class Fly {
        final String id;
        final boolean holo;
        float t;
        float dur;
        boolean started;

        Fly(String id, boolean holo) {
            this.id = id;
            this.holo = holo;
        }
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
    private int tab;
    private int cur;
    private int target;
    private Mode mode = Mode.IDLE;
    private int leafL;
    private int flipDir;
    private float theta;
    private float vel;
    private float dest;
    private boolean crossed;
    private float dragVx;
    private float dragStartVx;
    private boolean moved;
    private float tabFade = 1f;

    private final ArrayDeque<Fly> queue = new ArrayDeque<>();
    private final Set<String> pending = new HashSet<>();
    private Fly flying;
    private int flyTotal;
    private float bonusBefore;
    private float bonusAfter;
    private float bonusShow;
    private float shake;
    private final List<Spark> sparks = new ArrayList<>();
    private final List<Ring> rings = new ArrayList<>();

    private long lastNs = System.nanoTime();
    private float time;
    private float sc = 1f;
    private float ox;
    private float oy;

    public AlbumScreen() {
        super(Component.translatable("item.mundos_de_axelis.album_cartas"));
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

    private int pagesIn(int t) {
        return Math.max(1, (tabCards(t).size() + PER_PAGE - 1) / PER_PAGE);
    }

    private int spreadsIn(int t) {
        return (pagesIn(t) + 1) / 2;
    }

    private boolean owned(int t, CardDef d) {
        AlbumLogic a = CartasClient.ALBUM;
        boolean h = holoTab(t);
        return (h ? a.holo : a.normal).contains(d.id) && !pending.contains(key(d.id, h));
    }

    private static String key(String id, boolean holo) {
        return id + (holo ? "|h" : "|n");
    }

    // ------------------------------------------------------------------ insercion

    public void onInserted(CartasNet.InsertResult m) {
        if (m.ids().isEmpty()) {
            CartasClient.play(SoundEvents.VILLAGER_NO, 1.2f, 0.5f);
            bonusShow = 0f;
            return;
        }
        if (queue.isEmpty() && flying == null) {
            bonusBefore = m.before();
        }
        bonusAfter = m.after();
        for (int i = 0; i < m.ids().size(); i++) {
            pending.add(key(m.ids().get(i), m.holo().get(i)));
            queue.add(new Fly(m.ids().get(i), m.holo().get(i)));
        }
        flyTotal = queue.size() + (flying != null ? 1 : 0);
        CartasClient.play(SoundEvents.BOOK_PUT, 0.9f, 0.8f);
    }

    private void tickInsertion(float dt) {
        if (flying == null && !queue.isEmpty()) {
            flying = queue.poll();
            flying.dur = flyTotal <= 6 ? 0.85f : flyTotal <= 16 ? 0.5f : 0.3f;
        }
        if (flying == null) {
            return;
        }
        int wantTab = flying.holo ? Cards.CATEGORIES.size() : categoryIndex(flying.id);
        int idx = tabCards(wantTab).indexOf(Cards.get(flying.id));
        int wantSpread = Math.max(0, (idx / PER_PAGE) / 2);
        if (tab != wantTab) {
            if (mode == Mode.IDLE) {
                selectTab(wantTab, false);
            }
            return;
        }
        if (cur != wantSpread || mode != Mode.IDLE) {
            target = wantSpread;
            return;
        }
        if (!flying.started) {
            flying.started = true;
            CartasClient.play(SoundEvents.BOOK_PAGE_TURN, 1.5f, 0.35f);
        }
        flying.t += dt / flying.dur;
        if (flying.t >= 1f) {
            land(flying, wantTab, idx);
            flying = null;
            if (queue.isEmpty()) {
                bonusShow = 4.5f;
            }
        }
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

    private void land(Fly f, int t, int idx) {
        pending.remove(key(f.id, f.holo));
        CardDef d = Cards.get(f.id);
        float[] c = slotCenter(idx);
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
    }

    // ------------------------------------------------------------------ paginas

    private void selectTab(int t, boolean sound) {
        if (t < 0 || t >= tabCount() || (t == tab && mode == Mode.IDLE)) {
            return;
        }
        if (mode != Mode.IDLE) {
            return;
        }
        tab = t;
        cur = 0;
        target = 0;
        tabFade = 0f;
        if (sound) {
            CartasClient.play(SoundEvents.BOOK_PAGE_TURN, 1.3f, 0.5f);
        }
    }

    private void startLeaf(int dir, Mode m) {
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
        for (int i = 0; i < sub; i++) {
            float acc = K * (goal - theta) - D * vel;
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
        target = cur;
        mode = Mode.IDLE;
        crossed = false;
        CartasClient.play(SoundEvents.BOOK_PUT, 1.6f, 0.18f);
    }

    private void flip(int dir) {
        if (mode != Mode.IDLE || flying != null) {
            return;
        }
        int t = Mth.clamp(target + dir, 0, spreadsIn(tab) - 1);
        target = t;
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
        tabFade = Math.min(1f, tabFade + dt * 4f);
        shake = Math.max(0f, shake - dt);
        bonusShow = Math.max(0f, bonusShow - dt);
        tickInsertion(dt);
        updateLeaf(dt);
        tickFx(dt);

        renderBackground(g);
        sc = Math.min(width / (float) VW, height / (float) VH) * 0.96f;
        ox = (width - VW * sc) / 2f;
        oy = (height - VH * sc) / 2f;
        float vx = (mx - ox) / sc;
        float vy = (my - oy) / sc;

        g.pose().pushPose();
        g.pose().translate(ox, oy, 0f);
        g.pose().scale(sc, sc, 1f);
        if (shake > 0f) {
            g.pose().translate(Mth.sin(time * 90f) * shake * 3f, Mth.cos(time * 77f) * shake * 2f, 0f);
        }

        drawBook(g);
        List<Component> tip = null;
        if (mode == Mode.IDLE && flying == null) {
            tip = hoverTooltip(vx, vy);
        }
        drawTabs(g, vx, vy);
        drawFooter(g);
        drawFx(g);
        drawFlying(g);
        g.pose().popPose();

        if (tip != null) {
            g.renderComponentTooltip(font, tip, mx, my);
        }
    }

    private void drawBook(GuiGraphics g) {
        // tapa
        g.fill(2, PAGE_Y - 4, VW - 2, PAGE_Y + PH + 4, 0xFF4A2E1A);
        g.fill(4, PAGE_Y - 2, VW - 4, PAGE_Y + PH + 2, 0xFF6B4528);
        CardRender.border(g, 2, PAGE_Y - 4, VW - 4, PH + 8, 0xFF2E1C0F);

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
        if (tabFade < 1f) {
            g.fill(6, PAGE_Y, VW - 6, PAGE_Y + PH, ((int) ((1f - tabFade) * 200) << 24));
        }
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
        List<CardDef> cards = tabCards(tab);
        int pages = pagesIn(tab);
        if (page < 0 || page >= pages) {
            // pagina vacia con un adorno
            g.drawCenteredString(font, "✦", px + PW / 2, PAGE_Y + PH / 2 - 4, 0x30A88A5A);
            return;
        }
        boolean holo = holoTab(tab);
        int tabColor = holo ? 0x4FC3E8 : Cards.CATEGORIES.get(tab).color;
        // titulo
        int have = 0;
        for (CardDef d : cards) {
            if (owned(tab, d)) {
                have++;
            }
        }
        String title = holo ? "✦ Holográficas" : Cards.CATEGORIES.get(tab).name;
        g.drawString(font, title, px + 12, PAGE_Y + 10, 0xFF000000 | darker(tabColor), false);
        String cnt = have + "/" + cards.size();
        g.drawString(font, cnt, px + PW - 12 - font.width(cnt), PAGE_Y + 10, 0xFF6B4A2A, false);
        g.fill(px + 12, PAGE_Y + 22, px + PW - 12, PAGE_Y + 23, 0xFF000000 | tabColor);
        // cartas
        for (int i = page * PER_PAGE; i < Math.min(cards.size(), (page + 1) * PER_PAGE); i++) {
            drawSlot(g, tab, cards.get(i), i, px, false);
        }
        g.drawCenteredString(font, String.valueOf(page + 1), px + PW / 2, PAGE_Y + PH - 14, 0xFF8A6A42);
    }

    private static int darker(int rgb) {
        int r = (rgb >> 16 & 255) * 3 / 5;
        int gg = (rgb >> 8 & 255) * 3 / 5;
        int b = (rgb & 255) * 3 / 5;
        return r << 16 | gg << 8 | b;
    }

    private float[] slotPos(int idx) {
        int p = idx / PER_PAGE;
        boolean right = p % 2 == 1;
        int px = right ? SPINE : SPINE - PW;
        int k = idx % PER_PAGE;
        float x = px + 11 + (k % 3) * (CARD_W + 10);
        float y = PAGE_Y + 34 + (k / 3) * (CARD_H + 14);
        return new float[] {x, y};
    }

    private float[] slotCenter(int idx) {
        float[] p = slotPos(idx);
        return new float[] {p[0] + CARD_W / 2f, p[1] + CARD_H / 2f};
    }

    private int hoverIdx = -1;

    private void drawSlot(GuiGraphics g, int t, CardDef d, int idx, int px, boolean unused) {
        int k = idx % PER_PAGE;
        float x = px + 11 + (k % 3) * (CARD_W + 10);
        float y = PAGE_Y + 34 + (k / 3) * (CARD_H + 14);
        boolean own = owned(t, d);
        boolean hov = idx == hoverIdx && mode == Mode.IDLE && flying == null;
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
        }
    }

    private void drawTabs(GuiGraphics g, float vx, float vy) {
        int n = tabCount();
        int tw = Math.min(52, (VW - 8) / n);
        int x0 = (VW - tw * n) / 2;
        for (int i = 0; i < n; i++) {
            boolean sel = i == tab;
            boolean hov = vy >= 0 && vy < 20 && vx >= x0 + i * tw && vx < x0 + (i + 1) * tw;
            int col = holoTab(i) ? 0x4FC3E8 : Cards.CATEGORIES.get(i).color;
            int top = sel ? 0 : (hov ? 3 : 6);
            g.fill(x0 + i * tw + 1, top, x0 + (i + 1) * tw - 1, PAGE_Y, 0xFF000000 | (sel ? col : darker(col)));
            CardRender.border(g, x0 + i * tw + 1, top, tw - 2, PAGE_Y - top, 0xFF2E1C0F);
            String name = holoTab(i) ? "✦ Holo" : Cards.CATEGORIES.get(i).name;
            g.pose().pushPose();
            float s = font.width(name) > tw - 6 ? (tw - 6f) / font.width(name) : 1f;
            g.pose().translate(x0 + i * tw + tw / 2f, top + (PAGE_Y - top) / 2f - 4f * s + 1f, 0f);
            g.pose().scale(s, s, 1f);
            g.drawString(font, name, -font.width(name) / 2, 0, sel ? 0xFFFFFFFF : 0xFFE8DCC8, true);
            g.pose().popPose();
        }
    }

    private void drawFooter(GuiGraphics g) {
        AlbumLogic a = CartasClient.ALBUM;
        int shown = a.filled() - pendingCount();
        double pct = 100.0 * shown / Math.max(1, AlbumLogic.totalSlots());
        String left = String.format("Álbum %.0f %%  (%d/%d)   Vida extra: +%.1f corazones", pct, shown, AlbumLogic.totalSlots(), a.bonusHealth() / 2.0);
        if (flying != null || !queue.isEmpty()) {
            left = String.format("Álbum %.0f %%  (%d/%d)", pct, shown, AlbumLogic.totalSlots());
        }
        g.drawString(font, left, 8, PAGE_Y + PH + 8, 0xFFE8DCC8, true);
        String key = CartasKeys.INSERT.getTranslatedKeyMessage().getString();
        String hint = "Arrastra las páginas · [" + key + "] guardar cartas";
        g.drawString(font, hint, VW - 8 - font.width(hint), PAGE_Y + PH + 8, 0xFFB8A888, true);
        if (bonusShow > 0f && bonusAfter > bonusBefore) {
            float al = Math.min(1f, bonusShow);
            float rise = (4.5f - bonusShow) * 2f;
            String txt = String.format("+%.1f corazones de vida máxima", (bonusAfter - bonusBefore) / 2f);
            int a2 = (int) (al * 255);
            g.drawCenteredString(font, txt, VW / 2, (int) (PAGE_Y + PH - 26 - rise), (a2 << 24) | 0xFF6A8A);
        }
    }

    private int pendingCount() {
        return pending.size();
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

    private void drawFlying(GuiGraphics g) {
        if (flying == null || !flying.started) {
            return;
        }
        CardDef d = Cards.get(flying.id);
        int wantTab = flying.holo ? Cards.CATEGORIES.size() : categoryIndex(flying.id);
        int idx = tabCards(wantTab).indexOf(d);
        float[] end = slotCenter(idx);
        float e = 1f - (float) Math.pow(1f - Mth.clamp(flying.t, 0f, 1f), 3);
        float sx = VW / 2f + (end[0] - VW / 2f) * 0.2f;
        float sy = VH + 70f;
        float x = Mth.lerp(e, sx, end[0]);
        float y = Mth.lerp(e, sy, end[1]) - Mth.sin(e * Mth.PI) * 36f;
        float scale = Mth.lerp(e, 1.8f, 1.0f);
        float rot = Mth.lerp(e, (d.index % 2 == 0 ? -16f : 16f), 0f);
        g.pose().pushPose();
        g.pose().translate(x, y, 0f);
        g.pose().mulPose(Axis.ZP.rotationDegrees(rot));
        g.pose().scale(scale, scale, 1f);
        float w = CARD_W;
        float h = CARD_H;
        CardRender.glow(g, -w / 2, -h / 2, w, h, flying.holo ? 0x7FE8FF : d.rarity.color, 0.7f + e * 0.4f);
        CardRender.face(g, d, -w / 2, -h / 2, w, h);
        if (flying.holo) {
            CardRender.holo(g, -w / 2, -h / 2, w, h, time, 0.3f);
        }
        g.pose().popPose();
    }

    // ------------------------------------------------------------------ tooltip

    private List<Component> hoverTooltip(float vx, float vy) {
        hoverIdx = -1;
        List<CardDef> cards = tabCards(tab);
        int pages = pagesIn(tab);
        for (int i = 0; i < cards.size(); i++) {
            int p = i / PER_PAGE;
            if (p / 2 != cur || p >= pages) {
                continue;
            }
            float[] pos = slotPos(i);
            if (vx >= pos[0] && vx < pos[0] + CARD_W && vy >= pos[1] && vy < pos[1] + CARD_H) {
                hoverIdx = i;
                return tooltip(cards.get(i));
            }
        }
        return null;
    }

    private List<Component> tooltip(CardDef d) {
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
        if (button != 0 || flying != null || !queue.isEmpty()) {
            return super.mouseClicked(mx, my, button);
        }
        if (vy >= 0 && vy < PAGE_Y) {
            int n = tabCount();
            int tw = Math.min(52, (VW - 8) / n);
            int x0 = (VW - tw * n) / 2;
            int i = (int) Math.floor((vx - x0) / tw);
            if (i >= 0 && i < n) {
                selectTab(i, true);
                return true;
            }
        }
        if (mode == Mode.IDLE && vy >= PAGE_Y && vy < PAGE_Y + PH && vx >= 6 && vx < VW - 6) {
            int dir = vx >= SPINE ? 1 : -1;
            if ((dir > 0 && cur + 1 < spreadsIn(tab)) || (dir < 0 && cur > 0)) {
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
        if (delta != 0) {
            flip(delta < 0 ? 1 : -1);
            return true;
        }
        return false;
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (CartasKeys.OPEN.matches(key, scan)) {
            onClose();
            return true;
        }
        if (CartasKeys.INSERT.matches(key, scan)) {
            if (flying == null && queue.isEmpty()) {
                CartasNet.CHANNEL.sendToServer(new CartasNet.InsertRequest());
            }
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
        if (key >= GLFW.GLFW_KEY_1 && key < GLFW.GLFW_KEY_1 + tabCount() && flying == null) {
            selectTab(key - GLFW.GLFW_KEY_1, true);
            return true;
        }
        return super.keyPressed(key, scan, mods);
    }
}
