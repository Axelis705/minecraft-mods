package com.celular.client;

import com.celular.net.AppNet;
import com.celular.net.CelularNet;
import com.celular.plat.Ids;
import com.celular.plat.Net;
import com.celular.plat.Plat;
import com.celular.plat.client.G;
import com.celular.wallet.Wallet;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.UUID;

/**
 * Billetera del casino: cuántas fichas del Casino llevas (y de qué valor), enviar fichas a otro jugador conectado y los últimos
 * movimientos. Si el mod Casino no está, lo dice.
 */
final class WalletApp extends App {
    private static final int GOLD = 0xFFD4AF37, GREEN = 0xFF30D158;
    private static AppNet.WalletHist hist;
    /** 0 = billetera; 1 = elegir a quién; 2 = elegir cuánto. */
    private int view;
    private UUID to;
    private String toName = "";
    private long amount;
    private final Scroll scroll = new Scroll();

    WalletApp(PhoneScreen s) {
        super(s, D.APP_WALLET, "wallet");
    }

    static void hist(AppNet.WalletHist m) {
        hist = m;
    }

    @Override
    void enter() {
        view = 0;
        scroll.reset();
        Net.toServer(new AppNet.Req(AppNet.Req.WALLET_HIST));
    }

    private static boolean casino() {
        Item it = ForgeRegistries.ITEMS.getValue(Ids.rl("casino", "chip_1"));
        return it != null && it != net.minecraft.world.item.Items.AIR;
    }

    private static int count(int value) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return 0;
        }
        int n = 0;
        for (ItemStack st : Plat.mainAndOffhand(mc.player.getInventory())) {
            if (Wallet.value(st) == value) {
                n += st.getCount();
            }
        }
        return n;
    }

    private static String num(long n) {
        return String.format(java.util.Locale.ROOT, "%,d", n).replace(',', '.');
    }

    // ------------------------------------------------------------------ dibujo

    @Override
    void draw(G g, int mx, int my, float pt) {
        hits.clear();
        g.fill(s.dx0, s.dy0, s.dx1, s.dy1, s.bg());
        switch (view) {
            case 1 -> drawPick(g, mx, my);
            case 2 -> drawAmount(g, mx, my);
            default -> drawMain(g, mx, my);
        }
    }

    private void drawMain(G g, int mx, int my) {
        Minecraft mc = Minecraft.getInstance();
        Font f = font();
        int top = header(g, "celular.app.wallet", false, mx, my);
        if (!casino()) {
            D.circle(g, (s.dx0 + s.dx1) / 2, top + 30, 14, GOLD);
            D.icon(g, D.WALLET, (s.dx0 + s.dx1) / 2 - 8, top + 22, 16, 0xFFFFFFFF);
            centered(g, Component.translatable("celular.wallet.no_casino"), top + 52, s.sub());
            return;
        }
        long bal = mc.player == null ? 0 : Wallet.balance(mc.player);
        // tarjeta con el saldo
        int x0 = s.dx0 + 6, x1 = s.dx1 - 6, y = top + 6;
        g.fillGradient(x0, y, x1, y + 44, 0xFF2B2416, 0xFF14110B);
        D.rrLine(g, x0, y, x1, y + 44, 6, GOLD);
        small(g, Component.translatable("celular.wallet.balance").getString().toUpperCase(java.util.Locale.ROOT), x0 + 6, y + 5, 0xFFBFA65A, 0.6F);
        String b = num(Math.round(Anim.ease("billetera:saldo", bal, 7F)));
        float sc = 1.7F;
        g.push();
        g.translate(x0 + 6, y + 14, 0);
        g.scale(sc, sc, 1);
        g.drawString(f, b, 0, 0, 0xFFFFE08A, false);
        g.pop();
        small(g, Component.translatable("celular.wallet.chips").getString(), x0 + 8 + (int) (f.width(b) * sc), y + 22, 0xFFBFA65A, 0.65F);
        // de qué fichas
        int cx = x0 + 6;
        for (int v : Wallet.VALUES) {
            int n = count(v);
            if (n == 0) {
                continue;
            }
            Item it = ForgeRegistries.ITEMS.getValue(Ids.rl("casino", "chip_" + v));
            String t = "×" + n;
            int w = 10 + (int) (f.width(t) * 0.6F) + 3;
            if (cx + w > x1 - 4) {
                break;
            }
            if (it != null) {
                g.push();
                g.translate(cx, y + 33, 0);
                g.scale(0.55F, 0.55F, 1);
                g.renderItem(new ItemStack(it), 0, 0);
                g.pop();
            }
            small(g, t, cx + 10, y + 35, 0xFFE5D3A0, 0.6F);
            cx += w;
        }
        y += 50;
        // enviar
        button(g, s.dx0 + 6, y, s.dx1 - 6, y + 16, Component.translatable("celular.wallet.send"), GREEN, mx, my);
        hit(s.dx0 + 6, y, s.dx1 - 6, y + 16, () -> {
            view = 1;
            scroll.reset();
            Net.toServer(new CelularNet.Simple(CelularNet.Simple.CONTACTS));
        });
        y += 22;
        small(g, Component.translatable("celular.wallet.history").getString().toUpperCase(java.util.Locale.ROOT), s.dx0 + 6, y, s.sub(), 0.6F);
        y += 9;
        if (hist == null || hist.who.isEmpty()) {
            small(g, D.cut(f, Component.translatable("celular.wallet.no_history").getString(), (int) ((s.dw() - 12) / 0.65F)), s.dx0 + 6, y + 2,
                    s.sub(), 0.65F);
            return;
        }
        int rowH = 15, bottom = s.cy1;
        scroll.update(hist.who.size() * rowH, bottom - y);
        g.scissor(s.dx0, y, s.dx1, bottom);
        for (int i = 0; i < hist.who.size(); i++) {
            int ry = y + i * rowH - scroll.off();
            if (ry + rowH < y || ry > bottom) {
                continue;
            }
            int a = hist.amount.get(i);
            String amt = (a > 0 ? "+" : "−") + num(Math.abs(a));
            int aw = (int) (f.width(amt) * 0.75F);
            small(g, amt, s.dx1 - 6 - aw, ry + 3, a > 0 ? GREEN : 0xFFFF6961, 0.75F);
            String who = Component.translatable(a > 0 ? "celular.wallet.from" : "celular.wallet.to", hist.who.get(i)).getString();
            small(g, D.cut(f, who, (int) ((s.dw() - 16 - aw) / 0.7F)), s.dx0 + 6, ry + 2, s.fg(), 0.7F);
            small(g, D.ago(hist.time.get(i)).getString(), s.dx0 + 6, ry + 9, s.sub(), 0.55F);
        }
        g.noScissor();
    }

    private void drawPick(G g, int mx, int my) {
        Font f = font();
        int top = header(g, "celular.wallet.pick", true, mx, my);
        hit(s.dx0, s.cy0, s.dx0 + 18, s.cy0 + 18, () -> view = 0);
        CelularNet.Contacts c = ClientHooks.contacts;
        if (c == null) {
            spinner(g, (s.dx0 + s.dx1) / 2, top + 30, s.sub());
            return;
        }
        String me = Minecraft.getInstance().player == null ? "" : Minecraft.getInstance().player.getScoreboardName();
        int rowH = 20, bottom = s.cy1, k = 0;
        int n = 0;
        for (int i = 0; i < c.ids.size(); i++) {
            if (!c.names.get(i).equals(me)) {
                n++;
            }
        }
        if (n == 0) {
            centered(g, Component.translatable("celular.wallet.nobody"), top + 20, s.sub());
            return;
        }
        scroll.update(n * rowH, bottom - top);
        g.scissor(s.dx0, top, s.dx1, bottom);
        for (int i = 0; i < c.ids.size(); i++) {
            if (c.names.get(i).equals(me)) {
                continue;
            }
            int ry = top + (k++) * rowH - scroll.off();
            if (ry + rowH < top || ry > bottom) {
                continue;
            }
            if (s.in(mx, my, s.dx0, Math.max(ry, top), s.dx1, Math.min(ry + rowH, bottom))) {
                g.fill(s.dx0, ry, s.dx1, ry + rowH, s.card());
            }
            UUID id = c.ids.get(i);
            String name = c.names.get(i);
            D.faceRound(g, id, s.dx0 + 6, ry + 3, 14, s.line());
            g.drawString(f, D.cut(f, name, s.dw() - 30), s.dx0 + 25, ry + 6, s.fg(), false);
            hitIn(s.dx0, ry, s.dx1, ry + rowH, top, bottom, () -> {
                to = id;
                toName = name;
                amount = 0;
                view = 2;
            });
        }
        g.noScissor();
    }

    private void drawAmount(G g, int mx, int my) {
        Minecraft mc = Minecraft.getInstance();
        Font f = font();
        int top = header(g, "celular.wallet.how_much", true, mx, my);
        hit(s.dx0, s.cy0, s.dx0 + 18, s.cy0 + 18, () -> view = 1);
        long bal = mc.player == null ? 0 : Wallet.balance(mc.player);
        int cx = (s.dx0 + s.dx1) / 2, y = top + 6;
        D.faceRound(g, to, cx - 9, y, 18, GOLD);
        y += 22;
        centered(g, Component.literal(toName).withStyle(ChatFormatting.BOLD), y, s.fg());
        y += 14;
        String a = num(amount);
        float sc = 2.0F;
        g.push();
        g.translate(cx - f.width(a) * sc / 2, y, 0);
        g.scale(sc, sc, 1);
        g.drawString(f, a, 0, 0, amount > bal ? 0xFFFF6961 : 0xFFFFE08A, false);
        g.pop();
        y += 20;
        String have = Component.translatable("celular.wallet.have", num(bal)).getString();
        small(g, have, cx - (int) (f.width(have) * 0.6F / 2), y, s.sub(), 0.6F);
        y += 10;
        // sumar fichas
        int[] adds = {1, 5, 25, 100, 500};
        int gap = 3, bw = (s.dw() - 12 - gap * 4) / 5;
        for (int i = 0; i < adds.length; i++) {
            int bx = s.dx0 + 6 + i * (bw + gap);
            boolean h = s.in(mx, my, bx, y, bx + bw, y + 14);
            D.rr(g, bx, y, bx + bw, y + 14, 6, h ? 0xFF4A4A4E : (Prefs.dark ? 0xFF2C2C2E : 0xFF8E8E93));
            String t = "+" + adds[i];
            small(g, t, bx + bw / 2 - (int) (f.width(t) * 0.65F / 2), y + 4, 0xFFFFFFFF, 0.65F);
            int v = adds[i];
            hit(bx, y, bx + bw, y + 14, () -> amount = Math.min(1_000_000, amount + v));
        }
        y += 18;
        int half = (s.dw() - 15) / 2;
        button(g, s.dx0 + 6, y, s.dx0 + 6 + half, y + 14, Component.translatable("celular.wallet.clear"), Prefs.dark ? 0xFF3A3A3C : 0xFF8E8E93, mx,
                my);
        hit(s.dx0 + 6, y, s.dx0 + 6 + half, y + 14, () -> amount = 0);
        button(g, s.dx1 - 6 - half, y, s.dx1 - 6, y + 14, Component.translatable("celular.wallet.all"), Prefs.dark ? 0xFF3A3A3C : 0xFF8E8E93, mx,
                my);
        hit(s.dx1 - 6 - half, y, s.dx1 - 6, y + 14, () -> amount = bal);
        y += 20;
        boolean ok = amount > 0 && amount <= bal;
        button(g, s.dx0 + 6, y, s.dx1 - 6, y + 17, Component.translatable("celular.wallet.confirm", num(amount)), ok ? GREEN : 0xFF48484A, mx, my);
        if (ok) {
            hit(s.dx0 + 6, y, s.dx1 - 6, y + 17, () -> {
                Net.toServer(new AppNet.Req(AppNet.Req.WALLET_SEND, to, amount, ""));
                view = 0;
            });
        }
    }

    @Override
    boolean click(double x, double y, int b) {
        clickHits(x, y);
        return true;
    }

    @Override
    boolean scroll(double x, double y, double d) {
        scroll.wheel(d);
        return true;
    }

    @Override
    boolean back() {
        if (view > 0) {
            view--;
            return true;
        }
        return false;
    }

    static void reset() {
        hist = null;
    }
}
