package com.celular.client;

import com.celular.plat.client.G;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

/**
 * Notas y alarmas: pendientes (escribe y Enter; toca el círculo para tacharla), alarmas con la hora de Minecraft (toca la hora o los
 * minutos para cambiarlos) y un temporizador de minutos reales.
 */
final class NotesApp extends App {
    private static final int YELLOW = 0xFFFFD60A, ORANGE = 0xFFFF9F0A;
    private static int tab;
    private final Scroll scroll = new Scroll();
    private EditBox input;
    /** Alarma o nota esperando el segundo toque para borrarse. */
    private Object confirm;
    private long confirmAt;

    NotesApp(PhoneScreen s) {
        super(s, D.APP_NOTES, "notes");
    }

    @Override
    void enter() {
        Notes.load();
        scroll.reset();
        boxForTab();
    }

    @Override
    void leave() {
        s.removeBox();
        input = null;
    }

    private int contentTop() {
        return s.cy0 + 19 + 19;
    }

    private void boxForTab() {
        if (tab == 0) {
            input = s.box(s.dx0 + 22, contentTop() + 4, s.dw() - 30, 10, "celular.notes.hint", 120);
        } else {
            s.removeBox();
            input = null;
        }
    }

    private void addNote() {
        String t = s.boxValue().trim();
        if (t.isEmpty()) {
            return;
        }
        Notes.Note n = new Notes.Note();
        n.text = t;
        Notes.notes.add(0, n);
        Notes.save();
        if (input != null) {
            input.setValue("");
        }
    }

    private boolean confirming(Object o) {
        return confirm == o && System.currentTimeMillis() - confirmAt < 3000;
    }

    private void askDelete(Object o, Runnable del) {
        if (confirming(o)) {
            confirm = null;
            del.run();
            Notes.save();
        } else {
            confirm = o;
            confirmAt = System.currentTimeMillis();
        }
    }

    // ------------------------------------------------------------------ dibujo

    @Override
    void draw(G g, int mx, int my, float pt) {
        hits.clear();
        g.fill(s.dx0, s.dy0, s.dx1, s.dy1, s.bg());
        int top = header(g, "celular.app.notes", false, mx, my);
        // pestañas
        int x0 = s.dx0 + 6, x1 = s.dx1 - 6, y = top + 3;
        D.rr(g, x0, y, x1, y + 13, 6, Prefs.dark ? 0xFF2C2C2E : 0xFFE5E5EA);
        String[] keys = {"celular.notes.tab_notes", "celular.notes.tab_alarms", "celular.notes.tab_timer"};
        int w = (x1 - x0) / 3;
        for (int i = 0; i < 3; i++) {
            int sx = x0 + i * w, ex = i == 2 ? x1 : sx + w;
            boolean sel = tab == i, h = s.in(mx, my, sx, y, ex, y + 13);
            if (sel) {
                D.rr(g, sx + 1, y + 1, ex - 1, y + 12, 5, ORANGE);
            }
            String t = Component.translatable(keys[i]).getString();
            t = D.cut(font(), t, (int) ((ex - sx - 4) / 0.7F));
            int tw = (int) (font().width(t) * 0.7F);
            small(g, t, (sx + ex) / 2 - tw / 2, y + 4, sel ? 0xFFFFFFFF : h ? s.fg() : s.sub(), 0.7F);
            int ti = i;
            hit(sx, y, ex, y + 13, () -> {
                if (tab != ti) {
                    tab = ti;
                    scroll.reset();
                    boxForTab();
                }
            });
        }
        switch (tab) {
            case 1 -> drawAlarms(g, mx, my);
            case 2 -> drawTimer(g, mx, my);
            default -> drawNotes(g, mx, my);
        }
    }

    private void drawNotes(G g, int mx, int my) {
        Font f = font();
        int y = contentTop();
        // caja para escribir
        D.rr(g, s.dx0 + 5, y, s.dx1 - 5, y + 17, 6, s.card());
        D.icon(g, D.PLUS, s.dx0 + 9, y + 4, 9, ORANGE);
        hit(s.dx0 + 5, y, s.dx0 + 21, y + 17, this::addNote);
        int top = y + 21, bottom = s.cy1, rowH = 18;
        if (Notes.notes.isEmpty()) {
            centered(g, Component.translatable("celular.notes.empty"), top + 20, s.sub());
            return;
        }
        scroll.update(Notes.notes.size() * rowH, bottom - top);
        g.scissor(s.dx0, top, s.dx1, bottom);
        for (int i = 0; i < Notes.notes.size(); i++) {
            Notes.Note n = Notes.notes.get(i);
            int ry = top + i * rowH - scroll.off();
            if (ry + rowH < top || ry > bottom) {
                continue;
            }
            int cx = s.dx0 + 12, cy = ry + rowH / 2;
            if (n.done) {
                D.circle(g, cx, cy, 5, ORANGE);
                D.icon(g, D.CHECK, cx - 4, cy - 4, 8, 0xFFFFFFFF);
            } else {
                D.circle(g, cx, cy, 5, s.sub());
                D.circle(g, cx, cy, 4, s.bg());
            }
            int tw = s.dw() - 40;
            Component txt = Component.literal(D.cut(f, n.text, (int) (tw / 0.8F)));
            if (n.done) {
                txt = txt.copy().withStyle(ChatFormatting.STRIKETHROUGH);
            }
            g.push();
            g.translate(s.dx0 + 22, cy - 3, 0);
            g.scale(0.8F, 0.8F, 1);
            g.drawString(f, txt, 0, 0, n.done ? s.sub() : s.fg(), false);
            g.pop();
            boolean c = confirming(n);
            boolean hd = s.in(mx, my, s.dx1 - 15, Math.max(ry, top), s.dx1, Math.min(ry + rowH, bottom));
            D.icon(g, D.TRASH, s.dx1 - 13, cy - 4, 9, c ? 0xFFFF3B30 : hd ? 0xFFFF6961 : D.alpha(s.sub(), 0.7F));
            g.fill(s.dx0 + 22, ry + rowH - 1, s.dx1, ry + rowH, s.line());
            hitIn(s.dx0, ry, s.dx1 - 16, ry + rowH, top, bottom, () -> {
                n.done = !n.done;
                Notes.save();
            });
            hitIn(s.dx1 - 16, ry, s.dx1, ry + rowH, top, bottom, () -> askDelete(n, () -> Notes.notes.remove(n)));
        }
        g.noScissor();
    }

    private void drawAlarms(G g, int mx, int my) {
        Font f = font();
        int y = contentTop();
        int now = Notes.gameMinute();
        // hora del juego ahora y botón de agregar
        String nowTxt = Component.translatable("celular.notes.game_time", now < 0 ? "--:--" : Notes.clock(now)).getString();
        small(g, D.cut(f, nowTxt, (int) ((s.dw() - 30) / 0.7F)), s.dx0 + 6, y + 4, s.sub(), 0.7F);
        boolean ha = s.in(mx, my, s.dx1 - 20, y, s.dx1 - 4, y + 14);
        D.circle(g, s.dx1 - 12, y + 7, 7, ha ? 0xFFFFB340 : ORANGE);
        D.icon(g, D.PLUS, s.dx1 - 17, y + 2, 10, 0xFFFFFFFF);
        hit(s.dx1 - 20, y, s.dx1 - 4, y + 14, () -> {
            Notes.Alarm a = new Notes.Alarm();
            int m = now < 0 ? 6 * 60 : (now / 60 + 1) % 24 * 60;     // la próxima hora en punto
            a.hour = m / 60;
            a.minute = 0;
            Notes.alarms.add(a);
            Notes.save();
        });
        int top = y + 18, bottom = s.cy1 - 12, rowH = 30;
        small(g, D.cut(f, Component.translatable("celular.notes.alarm_tip").getString(), (int) ((s.dw() - 10) / 0.6F)), s.dx0 + 5, s.cy1 - 9,
                s.sub(), 0.6F);
        if (Notes.alarms.isEmpty()) {
            centered(g, Component.translatable("celular.notes.no_alarms"), top + 20, s.sub());
            return;
        }
        scroll.update(Notes.alarms.size() * rowH, bottom - top);
        g.scissor(s.dx0, top, s.dx1, bottom);
        for (int i = 0; i < Notes.alarms.size(); i++) {
            Notes.Alarm a = Notes.alarms.get(i);
            int ry = top + i * rowH - scroll.off();
            if (ry + rowH < top || ry > bottom) {
                continue;
            }
            // la hora grande: la parte de la hora y la de los minutos se tocan por separado
            String hh = Integer.toString(a.hour), mm = String.format(java.util.Locale.ROOT, "%02d", a.minute);
            float sc = 1.6F;
            int tx = s.dx0 + 8, ty = ry + 7;
            int hw = (int) (f.width(hh) * sc), cw = (int) (f.width(":") * sc), mw = (int) (f.width(mm) * sc);
            int col = a.on ? s.fg() : s.sub();
            g.push();
            g.translate(tx, ty, 0);
            g.scale(sc, sc, 1);
            g.drawString(f, hh + ":" + mm, 0, 0, col, false);
            g.pop();
            if (s.in(mx, my, tx - 2, ty - 2, tx + hw + 1, ty + 14)) {
                g.fill(tx, ty + 13, tx + hw, ty + 14, ORANGE);
            }
            if (s.in(mx, my, tx + hw + cw, ty - 2, tx + hw + cw + mw + 2, ty + 14)) {
                g.fill(tx + hw + cw, ty + 13, tx + hw + cw + mw, ty + 14, ORANGE);
            }
            hitIn(tx - 2, ty - 2, tx + hw + 1, ty + 14, top, bottom, () -> {
                a.hour = (a.hour + 1) % 24;
                Notes.save();
            });
            hitIn(tx + hw + cw, ty - 2, tx + hw + cw + mw + 2, ty + 14, top, bottom, () -> {
                a.minute = (a.minute + 15) % 60;
                Notes.save();
            });
            // interruptor y basura
            int sx = s.dx1 - 40, sy = ry + 9;
            D.rr(g, sx, sy, sx + 20, sy + 11, 5, a.on ? 0xFF34C759 : (Prefs.dark ? 0xFF39393D : 0xFFD1D1D6));
            D.circle(g, a.on ? sx + 14 : sx + 6, sy + 5, 4, 0xFFFFFFFF);
            hitIn(sx - 2, ry, sx + 22, ry + rowH, top, bottom, () -> {
                a.on = !a.on;
                Notes.save();
            });
            boolean c = confirming(a);
            boolean hd = s.in(mx, my, s.dx1 - 16, Math.max(ry, top), s.dx1, Math.min(ry + rowH, bottom));
            D.icon(g, D.TRASH, s.dx1 - 14, sy + 1, 9, c ? 0xFFFF3B30 : hd ? 0xFFFF6961 : D.alpha(s.sub(), 0.7F));
            hitIn(s.dx1 - 16, ry, s.dx1, ry + rowH, top, bottom, () -> askDelete(a, () -> Notes.alarms.remove(a)));
            g.fill(s.dx0 + 6, ry + rowH - 1, s.dx1, ry + rowH, s.line());
        }
        g.noScissor();
    }

    private void drawTimer(G g, int mx, int my) {
        Font f = font();
        long now = System.currentTimeMillis();
        boolean running = Notes.timerEnd > now;
        int cx = (s.dx0 + s.dx1) / 2;
        int y = contentTop() + 8;
        int r = Math.min(34, s.dw() / 2 - 10);
        int cy = y + r;
        // anillo que se vacía
        float k = running && Notes.timerLen > 0 ? (Notes.timerEnd - now) / (Notes.timerLen * 1000F) : 0;
        int steps = 60;
        for (int i = 0; i < steps; i++) {
            double a = -Math.PI / 2 + i * 2 * Math.PI / steps;
            int px = cx + (int) Math.round(Math.cos(a) * r), py = cy + (int) Math.round(Math.sin(a) * r);
            boolean lit = i < Math.ceil(k * steps);
            g.fill(px - 1, py - 1, px + 2, py + 2, lit ? ORANGE : s.line());
        }
        long left = running ? (Notes.timerEnd - now + 999) / 1000 : 0;
        String t = String.format(java.util.Locale.ROOT, "%d:%02d", left / 60, left % 60);
        float sc = 1.8F;
        g.push();
        g.translate(cx - f.width(t) * sc / 2, cy - 6, 0);
        g.scale(sc, sc, 1);
        g.drawString(f, t, 0, 0, s.fg(), false);
        g.pop();
        y = cy + r + 10;
        if (running) {
            button(g, s.dx0 + 14, y, s.dx1 - 14, y + 16, Component.translatable("celular.notes.stop"), 0xFFFF3B30, mx, my);
            hit(s.dx0 + 14, y, s.dx1 - 14, y + 16, () -> Notes.timerEnd = 0);
            return;
        }
        // minutos para empezar
        int[] mins = {1, 3, 5, 10, 15, 30};
        int cols = 3, gap = 4;
        int bw = (s.dw() - 12 - gap * (cols - 1)) / cols;
        for (int i = 0; i < mins.length; i++) {
            int bx = s.dx0 + 6 + (i % cols) * (bw + gap), by = y + (i / cols) * 20;
            String lbl = Component.translatable("celular.notes.minutes", mins[i]).getString();
            button(g, bx, by, bx + bw, by + 16, Component.literal(lbl), Prefs.dark ? 0xFF3A3A3C : 0xFF8E8E93, mx, my);
            int m = mins[i];
            hit(bx, by, bx + bw, by + 16, () -> {
                Notes.timerLen = m * 60;
                Notes.timerEnd = System.currentTimeMillis() + m * 60_000L;
            });
        }
        small(g, D.cut(f, Component.translatable("celular.notes.timer_tip").getString(), (int) ((s.dw() - 10) / 0.6F)), s.dx0 + 5, y + 44,
                s.sub(), 0.6F);
    }

    // ------------------------------------------------------------------ entrada

    @Override
    boolean click(double x, double y, int b) {
        clickHits(x, y);
        return true;
    }

    @Override
    boolean drag(double x, double y, double dx, double dy) {
        scroll.drag(dy);
        return true;
    }

    @Override
    boolean scroll(double x, double y, double d) {
        scroll.wheel(d);
        return true;
    }

    @Override
    boolean key(int key) {
        if (tab == 0 && (key == 257 || key == 335)) {
            addNote();
            return true;
        }
        return false;
    }
}
