package com.celular.client;

import com.celular.msg.Messages;
import com.celular.net.AppNet;
import com.celular.plat.Net;
import com.celular.plat.client.G;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;
import java.util.UUID;

/**
 * Mensajes: chats con los demás jugadores (les llegan aunque no estén conectados) y un grupo "Todos" del mundo. Lista de chats
 * con su último mensaje y los sin leer; dentro, burbujas (las tuyas a la derecha) y una caja para escribir (Enter para enviar).
 */
final class MessagesApp extends App {
    private static final int GREEN = 0xFF30D158, BLUE = 0xFF0A84FF;
    private UUID chat;
    private String chatName = "";
    private final Scroll scroll = new Scroll();
    private boolean stick = true;
    private long listAt;
    private EditBox input;

    MessagesApp(PhoneScreen s) {
        super(s, D.APP_MESSAGES, "messages");
    }

    @Override
    void enter() {
        scroll.reset();
        if (chat != null) {
            makeInput();                                   // cambió el tamaño de la ventana: vuelve la caja de texto
        } else {
            askList();
        }
    }

    @Override
    void leave() {
        s.removeBox();
        input = null;
        chat = null;
        Chats.viewing = null;
    }

    private void askList() {
        listAt = System.currentTimeMillis();
        Net.toServer(new AppNet.Req(AppNet.Req.MSG_LIST));
    }

    private int inputTop() {
        return s.cy1 - 18;
    }

    private void makeInput() {
        input = s.box(s.dx0 + 10, inputTop() + 5, s.dw() - 34, 10, "celular.msg.hint", 256);
    }

    private void openChat(UUID id, String name) {
        chat = id;
        chatName = name;
        Chats.open = null;
        Chats.viewing = id;
        stick = true;
        scroll.reset();
        Net.toServer(new AppNet.Req(AppNet.Req.MSG_OPEN, id, 0, ""));
        makeInput();
    }

    private void closeChat() {
        s.removeBox();
        input = null;
        chat = null;
        Chats.viewing = null;
        scroll.reset();
        askList();
    }

    private void send() {
        String t = s.boxValue().trim();
        if (chat == null || t.isEmpty()) {
            return;
        }
        Net.toServer(new AppNet.Req(AppNet.Req.MSG_SEND, chat, 0, t));
        if (input != null) {
            input.setValue("");
        }
        stick = true;
    }

    private static boolean isAll(UUID id) {
        return Messages.ALL.equals(id);
    }

    // ------------------------------------------------------------------ dibujo

    @Override
    void draw(G g, int mx, int my, float pt) {
        hits.clear();
        g.fill(s.dx0, s.dy0, s.dx1, s.dy1, s.bg());
        if (chat == null) {
            drawList(g, mx, my);
        } else {
            drawChat(g, mx, my);
        }
    }

    private void avatar(G g, UUID id, int x, int y, int size, boolean online) {
        if (isAll(id)) {
            D.circle(g, x + size / 2, y + size / 2, size / 2, GREEN);
            D.icon(g, D.PEOPLE, x + size / 2 - size * 3 / 8, y + size / 2 - size * 3 / 8, size * 3 / 4, 0xFFFFFFFF);
        } else {
            D.faceRound(g, id, x, y, size, s.line());
            if (online) {
                D.circle(g, x + size, y + size, 3, s.bg());
                D.circle(g, x + size, y + size, 2, GREEN);
            }
        }
    }

    private void drawList(G g, int mx, int my) {
        Font f = font();
        if (System.currentTimeMillis() - listAt > (Chats.stale ? 800 : 6000)) {
            askList();
        }
        int top = header(g, "celular.app.messages", false, mx, my);
        AppNet.MsgList l = Chats.list;
        if (l == null) {
            spinner(g, (s.dx0 + s.dx1) / 2, top + 40, s.sub());
            return;
        }
        int rowH = 26, bottom = s.cy1;
        scroll.update(l.convs.size() * rowH, bottom - top);
        g.scissor(s.dx0, top, s.dx1, bottom);
        for (int i = 0; i < l.convs.size(); i++) {
            AppNet.Conv c = l.convs.get(i);
            int ry = top + i * rowH - scroll.off();
            if (ry + rowH < top || ry > bottom) {
                continue;
            }
            if (s.in(mx, my, s.dx0, Math.max(ry, top), s.dx1, Math.min(ry + rowH, bottom))) {
                g.fill(s.dx0, ry, s.dx1, ry + rowH, s.card());
            }
            avatar(g, c.id, s.dx0 + 6, ry + 5, 16, c.online);
            String name = isAll(c.id) ? Component.translatable("celular.msg.all").getString() : c.name;
            int right = 4;
            if (c.time > 0) {
                String ago = D.ago(c.time).getString();
                right = (int) (f.width(ago) * 0.6F) + 6;
                small(g, ago, s.dx1 - right + 2, ry + 5, s.sub(), 0.6F);
            }
            int tw = s.dw() - 30 - right;
            g.drawString(f, Component.literal(D.cut(f, name, tw)).withStyle(c.unread > 0 ? ChatFormatting.BOLD : ChatFormatting.RESET), s.dx0 + 27, ry + 4,
                    s.fg(), false);
            String prev;
            if (c.last.isEmpty()) {
                prev = Component.translatable("celular.msg.start").getString();
            } else if (c.lastMine) {
                prev = Component.translatable("celular.msg.you").getString() + ": " + c.last;
            } else {
                prev = isAll(c.id) ? c.name + ": " + c.last : c.last;
            }
            int pw = s.dw() - 30 - (c.unread > 0 ? 14 : 4);
            small(g, D.cut(f, prev, (int) (pw / 0.7F)), s.dx0 + 27, ry + 15, c.unread > 0 ? s.fg() : s.sub(), 0.7F);
            if (c.unread > 0) {
                String n = c.unread > 9 ? "9+" : Integer.toString(c.unread);
                int bw = Math.max(9, (int) (f.width(n) * 0.7F) + 5);
                D.rr(g, s.dx1 - 5 - bw, ry + 13, s.dx1 - 5, ry + 22, 4, GREEN);
                small(g, n, s.dx1 - 5 - bw / 2 - (int) (f.width(n) * 0.7F / 2), ry + 15, 0xFFFFFFFF, 0.7F);
            }
            if (i < l.convs.size() - 1) {
                g.fill(s.dx0 + 27, ry + rowH - 1, s.dx1, ry + rowH, s.line());
            }
            UUID id = c.id;
            String nm = name;
            hitIn(s.dx0, ry, s.dx1, ry + rowH, top, bottom, () -> openChat(id, nm));
        }
        g.noScissor();
        if (l.convs.size() <= 1) {
            centered(g, Component.translatable("celular.msg.nobody"), top + rowH + 14, s.sub());
        }
    }

    private void drawChat(G g, int mx, int my) {
        Font f = font();
        // encabezado: volver, cara y nombre
        int y0 = s.cy0;
        g.fill(s.dx0, y0, s.dx1, y0 + 18, s.bg());
        g.fill(s.dx0, y0 + 18, s.dx1, y0 + 19, s.line());
        boolean hb = s.in(mx, my, s.dx0 + 2, y0 + 2, s.dx0 + 16, y0 + 16);
        D.icon(g, D.BACK, s.dx0 + 3, y0 + 3, 12, hb ? s.accent() : s.fg());
        hit(s.dx0, y0, s.dx0 + 18, y0 + 18, this::closeChat);
        AppNet.MsgThread t = Chats.open != null && Chats.open.id.equals(chat) ? Chats.open : null;
        avatar(g, chat, s.dx0 + 18, y0 + 3, 12, t != null && t.online);
        String title = isAll(chat) ? Component.translatable("celular.msg.all").getString() : chatName;
        g.drawString(f, Component.literal(D.cut(f, title, s.dw() - 40)).withStyle(ChatFormatting.BOLD), s.dx0 + 34, y0 + 5, s.fg(), false);
        int top = y0 + 19, bottom = inputTop() - 2;
        // caja para escribir y botón de enviar
        int iy = inputTop();
        D.rr(g, s.dx0 + 5, iy + 2, s.dx1 - 21, iy + 16, 7, s.card());
        boolean hs = s.in(mx, my, s.dx1 - 19, iy + 1, s.dx1 - 3, iy + 17);
        D.circle(g, s.dx1 - 11, iy + 9, 7, hs ? 0xFF3AA0FF : BLUE);
        D.icon(g, D.SEND, s.dx1 - 16, iy + 4, 10, 0xFFFFFFFF);
        hit(s.dx1 - 20, iy, s.dx1, iy + 18, this::send);
        if (t == null) {
            spinner(g, (s.dx0 + s.dx1) / 2, (top + bottom) / 2, s.sub());
            return;
        }
        if (t.msgs.isEmpty()) {
            centered(g, Component.translatable(isAll(chat) ? "celular.msg.all_empty" : "celular.msg.empty", title), (top + bottom) / 2 - 10, s.sub());
            return;
        }
        // burbujas (de abajo hacia arriba se apilan; aquí se calcula todo y se dibuja con el desplazamiento)
        float sc = 0.75F;
        int maxW = (int) (s.dw() * 0.74F);
        int wrap = (int) ((maxW - 8) / sc);
        int lineH = 8;
        int total = 4;
        int[] heights = new int[t.msgs.size()];
        @SuppressWarnings("unchecked")
        List<FormattedCharSequence>[] lines = new List[t.msgs.size()];
        for (int i = 0; i < t.msgs.size(); i++) {
            AppNet.Msg m = t.msgs.get(i);
            lines[i] = f.split(Component.literal(m.text), wrap);
            boolean showName = isAll(chat) && !m.mine && (i == 0 || t.msgs.get(i - 1).mine || !t.msgs.get(i - 1).from.equals(m.from));
            heights[i] = lines[i].size() * lineH + 6 + (showName ? 7 : 0) + 3;
            total += heights[i];
        }
        scroll.update(total, bottom - top);
        if (stick) {
            scroll.target = scroll.max;
            scroll.pos = scroll.max;
        }
        g.scissor(s.dx0, top, s.dx1, bottom);
        int y = top + 4 - scroll.off();
        if (total < bottom - top) {
            y = bottom - total + 4;                        // pocos mensajes: quedan abajo, junto a la caja
        }
        for (int i = 0; i < t.msgs.size(); i++) {
            AppNet.Msg m = t.msgs.get(i);
            int h = heights[i];
            if (y + h >= top && y <= bottom) {
                int w = 0;
                for (FormattedCharSequence l : lines[i]) {
                    w = Math.max(w, (int) Math.ceil(f.width(l) * sc));
                }
                w += 8;
                int yy = y;
                boolean showName = isAll(chat) && !m.mine && (i == 0 || t.msgs.get(i - 1).mine || !t.msgs.get(i - 1).from.equals(m.from));
                if (showName) {
                    small(g, D.cut(f, m.from, (int) (maxW / 0.6F)), s.dx0 + 9, yy, D.hsv(Math.floorMod(m.from.hashCode(), 360), 0.6F, 0.95F), 0.6F);
                    yy += 7;
                }
                int bh = lines[i].size() * lineH + 6;
                int bx0 = m.mine ? s.dx1 - 5 - w : s.dx0 + 5;
                D.rr(g, bx0, yy, bx0 + w, yy + bh, 5, m.mine ? BLUE : Prefs.dark ? 0xFF2C2C2E : 0xFFE5E5EA);
                int ly = yy + 3;
                for (FormattedCharSequence l : lines[i]) {
                    g.push();
                    g.translate(bx0 + 4, ly, 0);
                    g.scale(sc, sc, 1);
                    g.drawString(f, l, 0, 0, m.mine ? 0xFFFFFFFF : s.fg(), false);
                    g.pop();
                    ly += lineH;
                }
            }
            y += h;
        }
        g.noScissor();
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
        stick = chat != null && scroll.target >= scroll.max - 2;
        return true;
    }

    @Override
    boolean scroll(double x, double y, double d) {
        scroll.wheel(d);
        stick = chat != null && scroll.target >= scroll.max - 2;
        return true;
    }

    @Override
    boolean key(int key) {
        if (chat != null && (key == 257 || key == 335)) {     // Enter (también el del teclado numérico)
            send();
            return true;
        }
        return false;
    }

    @Override
    boolean back() {
        if (chat != null) {
            closeChat();
            return true;
        }
        return false;
    }
}
