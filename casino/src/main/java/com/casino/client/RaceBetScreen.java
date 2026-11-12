package com.casino.client;

import com.casino.chip.Chips;
import com.casino.diamond.RaceBE;
import com.casino.diamond.RaceBetPacket;
import com.casino.game.HorseRace;
import com.casino.net.CasinoNet;
import com.casino.plat.client.CasinoScreen;
import com.casino.plat.client.G;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;

/**
 * Menú de apuestas de las carreras: a la izquierda los 6 caballos (número, sedas, nombre, forma, cuota y probabilidad); a la
 * derecha el elegido galopando en grande, cuánto apostar (fichas rápidas y +/−) y lo que cobrarías si gana.
 */
public final class RaceBetScreen extends CasinoScreen {
    private static final int W = 404;
    private static final int H = 250;
    private static int lastAmount = 100;

    private final RaceBE be;
    private int sel;
    private int amount;
    private int left;
    private int top;
    private long opened;
    private final List<Btn> buttons = new ArrayList<>();

    /** Botón dibujado a mano (más limpio que los de Minecraft). */
    private final class Btn {
        final int x, y, w, h, color;
        final Component label;
        final Runnable action;
        final BooleanSupplier enabled;

        Btn(int x, int y, int w, int h, Component label, int color, Runnable action, BooleanSupplier enabled) {
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
            this.label = label;
            this.color = color;
            this.action = action;
            this.enabled = enabled;
        }

        boolean over(double mx, double my) {
            return mx >= left + x && mx < left + x + w && my >= top + y && my < top + y + h;
        }

        void draw(G g, int mx, int my) {
            boolean on = enabled.getAsBoolean();
            boolean hov = on && over(mx, my);
            int c = on ? color : 0xFF3A3644;
            if (hov) {
                c = lighten(c, 0.18F);
            }
            int x0 = left + x;
            int y0 = top + y;
            g.fill(x0, y0, x0 + w, y0 + h, 0xFF000000);
            g.fill(x0 + 1, y0 + 1, x0 + w - 1, y0 + h - 1, c);
            g.fill(x0 + 1, y0 + 1, x0 + w - 1, y0 + 2, lighten(c, 0.3F));
            g.fill(x0 + 1, y0 + h - 2, x0 + w - 1, y0 + h - 1, darken(c, 0.35F));
            int tc = on ? (RaceArt.light(c) ? 0xFF141018 : 0xFFFFFFFF) : 0xFF8A8494;
            g.drawString(font, label, x0 + (w - font.width(label)) / 2, y0 + (h - 8) / 2, tc, false);
        }
    }

    private RaceBetScreen(RaceBE be) {
        super(Component.translatable("casino.race.title"));
        this.be = be;
        HorseRace r = be.race();
        var pl = Minecraft.getInstance().player;
        RaceBE.Bet b = pl == null ? null : be.bets.get(pl.getUUID());
        if (b != null) {
            sel = b.horse();
            amount = b.amount();
        } else {
            sel = 0;
            for (int i = 1; i < HorseRace.N; i++) {
                if (r.p[i] > r.p[sel]) {
                    sel = i;
                }
            }
            amount = lastAmount;
        }
    }

    public static void open(RaceBE be) {
        ClientHooks.open(new RaceBetScreen(be));
    }

    private static int lighten(int c, float k) {
        int r = (c >> 16) & 255;
        int g = (c >> 8) & 255;
        int b = c & 255;
        return 0xFF000000 | (int) (r + (255 - r) * k) << 16 | (int) (g + (255 - g) * k) << 8 | (int) (b + (255 - b) * k);
    }

    private static int darken(int c, float k) {
        int r = (c >> 16) & 255;
        int g = (c >> 8) & 255;
        int b = c & 255;
        return 0xFF000000 | (int) (r * (1 - k)) << 16 | (int) (g * (1 - k)) << 8 | (int) (b * (1 - k));
    }

    private boolean open() {
        var l = Minecraft.getInstance().level;
        return l != null && be.phase(l.getGameTime()) == RaceBE.Phase.BET && be.betTicksLeft(l.getGameTime()) >= 10;
    }

    private int balance() {
        var p = Minecraft.getInstance().player;
        return p == null ? 0 : Chips.balance(p);
    }

    private RaceBE.Bet myBet() {
        var p = Minecraft.getInstance().player;
        return p == null ? null : be.bets.get(p.getUUID());
    }

    private int maxAmount() {
        RaceBE.Bet b = myBet();
        return Math.min(RaceBE.MAX_BET, balance() + (b == null ? 0 : b.amount()));
    }

    private void add(int d) {
        amount = Math.max(RaceBE.MIN_BET, Math.min(Math.max(RaceBE.MIN_BET, maxAmount()), amount + d));
    }

    @Override
    protected void init() {
        left = (width - W) / 2;
        top = Math.max(4, (height - H) / 2);
        opened = System.currentTimeMillis();
        buttons.clear();
        int rx = 240;
        int y = 158;
        buttons.add(new Btn(rx, y, 28, 16, Component.literal("-100"), 0xFF4A3E66, () -> add(-100), this::open));
        buttons.add(new Btn(rx + 30, y, 24, 16, Component.literal("-10"), 0xFF4A3E66, () -> add(-10), this::open));
        buttons.add(new Btn(rx + 102, y, 24, 16, Component.literal("+10"), 0xFF4A3E66, () -> add(10), this::open));
        buttons.add(new Btn(rx + 128, y, 28, 16, Component.literal("+100"), 0xFF4A3E66, () -> add(100), this::open));
        int[] quick = {50, 250, 1000, 5000};
        for (int k = 0; k < 4; k++) {
            int v = quick[k];
            buttons.add(new Btn(rx + k * 32, y + 20, 30, 14, Component.literal(v >= 1000 ? (v / 1000) + "K" : String.valueOf(v)), 0xFF2C5A8C,
                    () -> amount = Math.max(RaceBE.MIN_BET, Math.min(v, Math.max(RaceBE.MIN_BET, maxAmount()))), this::open));
        }
        buttons.add(new Btn(rx + 128, y + 20, 28, 14, Component.translatable("casino.race.max"), 0xFF8C5A2C,
                () -> amount = Math.max(RaceBE.MIN_BET, maxAmount()), this::open));
        buttons.add(new Btn(rx, 214, 156, 22, Component.translatable("casino.race.place").withStyle(ChatFormatting.BOLD), 0xFF2E9A52, this::place,
                () -> open() && maxAmount() >= RaceBE.MIN_BET));
        buttons.add(new Btn(W - 52, 6, 46, 14, Component.translatable("casino.race.cancel"), 0xFF8A3030,
                () -> CasinoNet.sendToServer(new RaceBetPacket(be.getBlockPos(), 0, 0)), () -> open() && myBet() != null));
    }

    private void place() {
        lastAmount = amount;
        CasinoNet.sendToServer(new RaceBetPacket(be.getBlockPos(), sel, amount));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void tick() {
        var p = Minecraft.getInstance().player;
        if (p == null || be.isRemoved() || p.distanceToSqr(be.center()) > RaceBE.RANGE * RaceBE.RANGE) {
            onClose();
        }
    }

    private void click() {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }

    @Override
    protected boolean onClick(double mx, double my, int button) {
        if (button != 0) {
            return superClick(mx, my, button);
        }
        for (Btn b : buttons) {
            if (b.over(mx, my) && b.enabled.getAsBoolean()) {
                click();
                b.action.run();
                return true;
            }
        }
        for (int i = 0; i < HorseRace.N; i++) {
            int cx = left + 8;
            int cy = top + 32 + i * 35;
            if (mx >= cx && mx < cx + 222 && my >= cy && my < cy + 32) {
                if (sel != i) {
                    sel = i;
                    click();
                }
                return true;
            }
        }
        return superClick(mx, my, button);
    }

    @Override
    protected boolean onScroll(double x, double y, double delta) {
        if (open()) {
            add(delta > 0 ? 10 : -10);
            return true;
        }
        return false;
    }

    @Override
    protected void draw(G g, int mx, int my, float pt) {
        drawBackground(g, mx, my, pt);
        var mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }
        long now = mc.level.getGameTime();
        HorseRace r = be.race();
        RaceBE.Bet mine = myBet();
        float anim = Math.min(1, (System.currentTimeMillis() - opened) / 220F);
        float ease = 1 - (1 - anim) * (1 - anim);
        int dy = (int) ((1 - ease) * 14);
        top += dy;

        // marco y cabecera
        g.fill(left - 2, top - 2, left + W + 2, top + H + 2, 0xFFC8A040);
        g.fill(left - 1, top - 1, left + W + 1, top + H + 1, 0xFF5A4418);
        g.fillGradient(left, top, left + W, top + H, 0xF6181424, 0xF60A0910);
        g.fillGradient(left, top, left + W, top + 26, 0xFF3A2A0C, 0xFF1E1606);
        g.drawString(font, Component.translatable("casino.race.title").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), left + 8, top + 5, 0xFFFFFF, true);
        g.drawString(font, Component.translatable("casino.cashier.balance", String.format("%,d", balance())).withStyle(ChatFormatting.YELLOW),
                left + 8, top + 15, 0xFFFFFF, false);
        // reloj
        Component clock;
        int cc;
        RaceBE.Phase ph = be.phase(now);
        if (ph == RaceBE.Phase.BET) {
            int tl = be.betTicksLeft(now);
            clock = Component.translatable("casino.race.closes_in", RaceArt.time(tl));
            cc = tl < 200 && (now / 5) % 2 == 0 ? 0xFFFF5050 : 0xFF2E9A52;
        } else {
            clock = Component.translatable(ph == RaceBE.Phase.RESULT ? "casino.race.hud_results_short" : "casino.race.closed_short");
            cc = 0xFF8A3030;
        }
        int pw = font.width(clock) + 12;
        int px = left + W - 58 - pw;
        g.fill(px, top + 6, px + pw, top + 20, cc);
        g.drawString(font, clock, px + 6, top + 9, 0xFFFFFFFF, true);

        // lista de caballos
        for (int i = 0; i < HorseRace.N; i++) {
            int cx = left + 8;
            int cy = top + 32 + i * 35;
            boolean s = i == sel;
            boolean hov = mx >= cx && mx < cx + 222 && my >= cy && my < cy + 32;
            if (s) {
                g.fill(cx - 1, cy - 1, cx + 223, cy + 33, 0xFFE8C050);
            }
            g.fill(cx, cy, cx + 222, cy + 32, s ? 0xFF2A2236 : hov ? 0xFF221E2E : 0xFF17151F);
            g.fill(cx, cy, cx + 3, cy + 32, RaceArt.silkA(r, i));
            RaceArt.badge(g, font, r, i, cx + 7, cy + 9, 14);
            RaceArt.gui(g, r, i, s ? (int) (now / 2) : 6, cx + 24, cy + 3, 38, 26);
            String odds = r.oddsText(i);
            // el nombre (en negrita) no puede pisar la cuota: se corta con "…" si no cabe
            int room = 214 - (int) (font.width(odds) * 1.5F) - 6 - 66;
            String name = RaceBE.horseName(r, i).getString();
            if (font.width(Component.literal(name).withStyle(ChatFormatting.BOLD)) > room) {
                while (name.length() > 1 && font.width(Component.literal(name + "…").withStyle(ChatFormatting.BOLD)) > room) {
                    name = name.substring(0, name.length() - 1);
                }
                name = name.trim() + "…";
            }
            g.drawString(font, Component.literal(name).withStyle(ChatFormatting.BOLD), cx + 66, cy + 6, 0xFFF4F0FF, true);
            g.drawString(font, RaceArt.stars(r.form[i]), cx + 66, cy + 18, 0xFFE8C050, false);
            g.push();
            g.translate(cx + 214 - font.width(odds) * 1.5F, cy + 5, 0);
            g.scale(1.5F, 1.5F, 1);
            g.drawString(font, odds, 0, 0, 0xFFFFD040, true);
            g.pop();
            String pct = Math.round(r.p[i] * 100) + "%";
            g.drawString(font, pct, cx + 214 - font.width(pct), cy + 21, 0xFF9890B0, false);
            if (mine != null && mine.horse() == i) {
                Component tag = Component.translatable("casino.race.your_bet_tag");
                int tw = font.width(tag) + 6;
                // a la derecha de las estrellas, sin taparlas
                int tx = cx + 116;
                g.fill(tx, cy + 18, tx + tw, cy + 28, 0xFF2E9A52);
                g.drawString(font, tag, tx + 3, cy + 19, 0xFFFFFFFF, false);
            }
        }

        // panel derecho: el caballo elegido
        int rx = left + 240;
        g.fill(rx - 4, top + 30, left + W - 6, top + H - 6, 0xFF120F1A);
        g.fill(rx - 4, top + 30, left + W - 6, top + 31, 0xFF5A4418);
        int frame = (int) (now / 1.5);
        g.fill(rx, top + 36, rx + 156, top + 100, 0xFF2C4E2A);
        g.fill(rx, top + 88, rx + 156, top + 100, 0xFF8A6440);
        RaceArt.gui(g, r, sel, frame, rx + 30, top + 36, 96, 66);
        g.drawCenteredString(font, RaceBE.horseName(r, sel).withStyle(ChatFormatting.BOLD), rx + 78, top + 104, 0xFFFFFFFF);
        Component oddsLine = Component.translatable("casino.race.odds_line", r.oddsText(sel), String.format("%.1f", 1 + (double) r.oddsNum[sel] / r.oddsDen[sel]));
        g.drawCenteredString(font, oddsLine, rx + 78, top + 116, 0xFFFFD040);
        // barra de probabilidad
        int bw = 120;
        int bx = rx + 18;
        int byy = top + 127;
        g.fill(bx, byy, bx + bw, byy + 6, 0xFF2A2634);
        g.fill(bx, byy, bx + (int) (bw * Math.min(1, r.p[sel] * 1.6)), byy + 6, 0xFF5AC870);
        g.drawCenteredString(font, Component.translatable("casino.race.chance", Math.round(r.p[sel] * 100)), rx + 78, byy + 8, 0xFF9890B0);
        // cantidad
        String amt = String.format("%,d", amount);
        g.fill(rx + 56, top + 158, rx + 100, top + 174, 0xFF000000);
        g.fill(rx + 57, top + 159, rx + 99, top + 173, 0xFF221E2E);
        g.drawCenteredString(font, amt, rx + 78, top + 162, 0xFFFFE070);
        long pay = r.payout(sel, amount);
        g.drawCenteredString(font, Component.translatable("casino.race.would_pay", String.format("%,d", pay)), rx + 78, top + 200, 0xFF70FF9A);
        if (!open()) {
            g.drawCenteredString(font, Component.translatable("casino.race.closed").withStyle(ChatFormatting.RED), rx + 78, top + 147, 0xFFFFFFFF);
        } else if (mine != null) {
            g.drawCenteredString(font, Component.translatable("casino.race.current_bet", mine.horse() + 1, String.format("%,d", mine.amount())), rx + 78,
                    top + 147, 0xFFB0E8C0);
        }
        for (Btn b : buttons) {
            b.draw(g, mx, my);
        }
        top -= dy;
    }
}
