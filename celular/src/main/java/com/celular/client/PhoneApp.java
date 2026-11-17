package com.celular.client;

import com.celular.net.CelularNet;
import com.celular.net.CelularNet.CallState;
import com.celular.plat.Net;
import com.celular.plat.client.G;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;

import java.util.UUID;

/**
 * Teléfono: los contactos (los jugadores conectados), las llamadas recientes y la pantalla de la llamada (llamando, entrante y en
 * curso, con el reloj y si hay voz).
 */
final class PhoneApp extends App {
    private static final int GREEN = 0xFF30D158, RED = 0xFFFF3B30;
    private int tab;
    private final Scroll scroll = new Scroll();
    private long requestedAt;

    PhoneApp(PhoneScreen s) {
        super(s, D.APP_PHONE, "phone");
    }

    @Override
    void enter() {
        ClientHooks.badgeCalls = 0;
        requestContacts();
        scroll.reset();
    }

    private void requestContacts() {
        requestedAt = System.currentTimeMillis();
        Net.toServer(new CelularNet.Simple(CelularNet.Simple.CONTACTS));
    }

    private boolean inCall() {
        return ClientHooks.call.state != CallState.IDLE || (ClientHooks.lastEnd != null && System.currentTimeMillis() - ClientHooks.lastEndAt < 2200
                && ClientHooks.lastEnd.reason != CallState.R_NONE);
    }

    @Override
    int bg() {
        return inCall() ? 0xFF101418 : s.bg();
    }

    @Override
    boolean lightStatus() {
        return !inCall() && !Prefs.dark;
    }

    @Override
    void draw(G g, int mx, int my, float pt) {
        if (inCall()) {
            drawCall(g, mx, my);
            return;
        }
        // cada 5 segundos se actualiza quién está conectado
        if (System.currentTimeMillis() - requestedAt > 5000) {
            requestContacts();
        }
        g.fill(s.dx0, s.dy0, s.dx1, s.dy1, s.bg());
        int top = header(g, tab == 0 ? "celular.phone.contacts" : "celular.phone.recents", false, mx, my);
        int bottom = s.cy1 - 16;
        g.scissor(s.dx0, top, s.dx1, bottom);
        if (tab == 0) {
            drawContacts(g, top, bottom, mx, my);
        } else {
            drawRecents(g, top, bottom, mx, my);
        }
        g.noScissor();
        // pestañas
        g.fill(s.dx0, bottom, s.dx1, s.dy1, s.bg());
        g.fill(s.dx0, bottom, s.dx1, bottom + 1, s.line());
        int w = s.dw() / 2;
        int[] icons = {D.PEOPLE, D.CLOCK};
        for (int i = 0; i < 2; i++) {
            boolean on = tab == i, h = s.in(mx, my, s.dx0 + i * w, bottom, s.dx0 + (i + 1) * w, bottom + 16);
            D.icon(g, icons[i], s.dx0 + i * w + w / 2 - 6, bottom + 2, 12, on ? s.accent() : h ? s.fg() : s.sub());
        }
    }

    private void drawContacts(G g, int top, int bottom, int mx, int my) {
        Font f = font();
        CelularNet.Contacts c = ClientHooks.contacts;
        if (c == null) {
            spinner(g, (s.dx0 + s.dx1) / 2, top + 30, s.fg());
            return;
        }
        if (c.ids.isEmpty()) {
            D.icon(g, D.PEOPLE, (s.dx0 + s.dx1) / 2 - 12, top + 24, 24, s.sub());
            centered(g, Component.translatable("celular.phone.alone"), top + 54, s.sub());
            return;
        }
        int rowH = 22;
        scroll.update(c.ids.size() * rowH + 4, bottom - top);
        int y = top - scroll.off();
        for (int i = 0; i < c.ids.size(); i++, y += rowH) {
            boolean phone = c.phone.get(i);
            boolean h = s.in(mx, my, s.dx0, y, s.dx1, y + rowH) && my >= top && my < bottom;
            if (h && phone) {
                g.fill(s.dx0, y, s.dx1, y + rowH, s.card());
            }
            D.faceRound(g, c.ids.get(i), s.dx0 + 6, y + 4, 14, s.line());
            g.drawString(f, D.cut(f, c.names.get(i), s.dw() - 50), s.dx0 + 25, y + (phone ? 7 : 3), phone ? s.fg() : s.sub(), false);
            if (!phone) {
                g.push();
                g.translate(s.dx0 + 25, y + 13, 0);
                g.scale(0.7F, 0.7F, 1);
                g.drawString(f, Component.translatable("celular.phone.no_phone"), 0, 0, s.sub(), false);
                g.pop();
            } else {
                D.circle(g, s.dx1 - 13, y + 11, 7, h ? GREEN : D.alpha(GREEN, 0.85F));
                D.icon(g, D.CALL, s.dx1 - 18, y + 6, 10, 0xFFFFFFFF);
            }
            g.fill(s.dx0 + 25, y + rowH - 1, s.dx1, y + rowH, s.line());
        }
    }

    private void drawRecents(G g, int top, int bottom, int mx, int my) {
        Font f = font();
        if (ClientHooks.recents.isEmpty()) {
            D.icon(g, D.CLOCK, (s.dx0 + s.dx1) / 2 - 12, top + 24, 24, s.sub());
            centered(g, Component.translatable("celular.phone.no_recents"), top + 54, s.sub());
            return;
        }
        int rowH = 22;
        scroll.update(ClientHooks.recents.size() * rowH + 4, bottom - top);
        int y = top - scroll.off();
        for (ClientHooks.Recent r : ClientHooks.recents) {
            boolean missed = r.type == 2;
            D.faceRound(g, r.id, s.dx0 + 6, y + 4, 14, s.line());
            g.drawString(f, D.cut(f, r.name, s.dw() - 60), s.dx0 + 25, y + 3, missed ? RED : s.fg(), false);
            Component kind = Component.translatable("celular.phone.recent." + r.type);
            if (r.seconds > 0) {
                kind = kind.copy().append(" · " + D.clock(r.seconds));
            }
            g.push();
            g.translate(s.dx0 + 25, y + 13, 0);
            g.scale(0.7F, 0.7F, 1);
            g.drawString(f, kind, 0, 0, s.sub(), false);
            g.pop();
            Component ago = D.ago(r.time);
            g.push();
            g.translate(s.dx1 - 4 - f.width(ago) * 0.7F, y + 4, 0);
            g.scale(0.7F, 0.7F, 1);
            g.drawString(f, ago, 0, 0, s.sub(), false);
            g.pop();
            g.fill(s.dx0 + 25, y + rowH - 1, s.dx1, y + rowH, s.line());
            y += rowH;
        }
    }

    // ------------------------------------------------------------------ la llamada

    private int[] bigButton(int i, int n) {
        int cy = s.cy1 - 30;
        int cx = n == 1 ? (s.dx0 + s.dx1) / 2 : (i == 0 ? s.dx0 + s.dw() / 4 : s.dx1 - s.dw() / 4);
        return new int[]{cx, cy};
    }

    private void drawCall(G g, int mx, int my) {
        Font f = font();
        long now = System.currentTimeMillis();
        CallState st = ClientHooks.call;
        boolean ended = st.state == CallState.IDLE;
        CallState shown = ended ? ClientHooks.lastEnd : st;
        // fondo con degradado
        g.fillGradient(s.dx0, s.dy0, s.dx1, s.dy1, 0xFF1B2A38, 0xFF07090C);
        int cx = (s.dx0 + s.dx1) / 2;
        int fy = s.cy0 + 30;
        int fs = 40;
        // anillos que laten mientras suena
        if (st.state == CallState.DIALING || st.state == CallState.INCOMING) {
            for (int i = 0; i < 2; i++) {
                float ph = ((now + i * 700) % 1400) / 1400F;
                int r = (int) (fs / 2 + 4 + ph * 18);
                D.circle(g, cx, fy + fs / 2, r, D.alpha(0xFF30D158, (1 - ph) * 0.25F));
            }
        }
        UUID who = shown != null ? shown.other : null;
        D.faceRound(g, who, cx - fs / 2, fy, fs, 0xFF2C3E50);
        String name = shown != null ? shown.name : "";
        Component nm = Component.literal(D.cut(f, name, s.dw() - 12)).withStyle(ChatFormatting.BOLD);
        g.drawString(f, nm, cx - f.width(nm) / 2, fy + fs + 8, 0xFFFFFFFF, false);
        Component status;
        if (ended) {
            status = Component.translatable("celular.call.end." + shown.reason, shown.seconds > 0 ? D.clock(shown.seconds) : "");
        } else if (st.state == CallState.DIALING) {
            status = Component.translatable("celular.call.dialing");
        } else if (st.state == CallState.INCOMING) {
            status = Component.translatable("celular.call.incoming");
        } else {
            status = Component.literal(D.clock(ClientHooks.callSeconds()));
        }
        g.drawString(f, status, cx - f.width(status) / 2, fy + fs + 20, ended ? 0xFFFF6B60 : 0xFFB8C4CE, false);
        // estado de la voz
        if (!ended && st.state == CallState.ACTIVE) {
            Component v = Component.translatable(st.voice ? "celular.call.voice_on" : "celular.call.voice_off");
            int vy = fy + fs + 36;
            for (net.minecraft.util.FormattedCharSequence l : f.split(v, s.dw() - 16)) {
                g.push();
                g.translate(cx - f.width(l) * 0.375F, vy, 0);
                g.scale(0.75F, 0.75F, 1);
                g.drawString(f, l, 0, 0, st.voice ? 0xFF7CF0A6 : 0xFFFFC46B, false);
                g.pop();
                vy += 8;
            }
            if (st.voice) {
                // ondas
                for (int i = 0; i < 7; i++) {
                    int bh = 2 + (int) (8 * Math.abs(Math.sin(now / 140.0 + i * 0.9)));
                    int bx = cx - 14 + i * 4;
                    g.fill(bx, vy + 10 - bh / 2, bx + 2, vy + 10 + bh / 2, 0xC07CF0A6);
                }
            }
        }
        if (ended) {
            return;
        }
        if (st.state == CallState.INCOMING) {
            int[] a = bigButton(0, 2), b = bigButton(1, 2);
            round(g, b[0], b[1], GREEN, D.CALL, mx, my, (int) (Math.sin(now / 90.0) * 1.5));
            round(g, a[0], a[1], RED, D.HANGUP, mx, my, 0);
            label(g, b[0], b[1] + 15, "celular.call.answer");
            label(g, a[0], a[1] + 15, "celular.call.decline");
        } else {
            int[] a = bigButton(0, 1);
            round(g, a[0], a[1], RED, D.HANGUP, mx, my, 0);
            label(g, a[0], a[1] + 15, "celular.call.hangup");
        }
    }

    private void round(G g, int cx, int cy, int color, int icon, int mx, int my, int shake) {
        boolean h = Math.hypot(mx - cx, my - cy) < 12;
        D.circle(g, cx + shake, cy, h ? 13 : 12, color);
        D.icon(g, icon, cx - 7 + shake, cy - 7, 14, 0xFFFFFFFF);
    }

    private void label(G g, int cx, int y, String key) {
        Component t = Component.translatable(key);
        g.push();
        g.translate(cx - font().width(t) * 0.375F, y, 0);
        g.scale(0.75F, 0.75F, 1);
        g.drawString(font(), t, 0, 0, 0xFFD0D0D0, false);
        g.pop();
    }

    // ------------------------------------------------------------------ entrada

    @Override
    boolean click(double x, double y, int b) {
        CallState st = ClientHooks.call;
        if (inCall()) {
            if (st.state == CallState.INCOMING) {
                int[] a = bigButton(0, 2), bb = bigButton(1, 2);
                if (Math.hypot(x - bb[0], y - bb[1]) < 13) {
                    Net.toServer(new CelularNet.CallAct(CelularNet.CallAct.ACCEPT, null));
                    ClientHooks.tap();
                } else if (Math.hypot(x - a[0], y - a[1]) < 13) {
                    Net.toServer(new CelularNet.CallAct(CelularNet.CallAct.DECLINE, null));
                    ClientHooks.tap();
                }
            } else if (st.state != CallState.IDLE) {
                int[] a = bigButton(0, 1);
                if (Math.hypot(x - a[0], y - a[1]) < 13) {
                    Net.toServer(new CelularNet.CallAct(CelularNet.CallAct.HANGUP, null));
                    ClientHooks.tap();
                }
            } else {
                ClientHooks.lastEnd = null;
            }
            return true;
        }
        int bottom = s.cy1 - 16;
        if (y >= bottom) {
            int t = x < (s.dx0 + s.dx1) / 2.0 ? 0 : 1;
            if (t != tab) {
                tab = t;
                scroll.reset();
                ClientHooks.tap();
            }
            return true;
        }
        int top = s.cy0 + 19;
        if (y < top) {
            return true;
        }
        int rowH = 22;
        int row = (int) ((y - top + scroll.off()) / rowH);
        if (tab == 0) {
            CelularNet.Contacts c = ClientHooks.contacts;
            if (c != null && row >= 0 && row < c.ids.size() && c.phone.get(row)) {
                dial(c.ids.get(row), c.names.get(row));
            }
        } else if (row >= 0 && row < ClientHooks.recents.size()) {
            ClientHooks.Recent r = ClientHooks.recents.get(row);
            dial(r.id, r.name);
        }
        return true;
    }

    void dial(UUID id, String name) {
        ClientHooks.tap();
        ClientHooks.lastEnd = null;
        // se ve "llamando" de una vez; el servidor confirma (o dice que está ocupado)
        ClientHooks.call = new CallState(CallState.DIALING, 0, id, name, 0, ClientHooks.call.voice);
        Net.toServer(new CelularNet.CallAct(CelularNet.CallAct.DIAL, id));
    }

    @Override
    boolean drag(double x, double y, double dx, double dy) {
        if (!inCall()) {
            scroll.drag(dy);
        }
        return true;
    }

    @Override
    boolean scroll(double x, double y, double d) {
        if (!inCall()) {
            scroll.wheel(d);
        }
        return true;
    }
}
