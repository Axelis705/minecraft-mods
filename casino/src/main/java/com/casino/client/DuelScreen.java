package com.casino.client;

import com.casino.game.DuelRules;
import com.casino.net.CasinoNet;
import com.casino.plat.client.CasinoScreen;
import com.casino.plat.client.G;
import com.casino.west.DuelBE;
import com.casino.west.MatchBE;
import com.casino.west.WestPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;

import java.util.Locale;

/**
 * Pantalla del duelo (transparente: se ve la calle y el rival). Antes del grito no se toca nada; al gritar "¡DESENFUNDA!" sale una
 * tecla al azar en grande y gana quien la apriete primero. El tiempo se mide aquí, desde el primer cuadro que muestra la tecla, y se
 * manda al servidor. Una tecla equivocada traba el revólver medio segundo. Esc dos veces = rendirse.
 */
public final class DuelScreen extends CasinoScreen {
    private final BlockPos pos;
    private boolean aimed;
    private long keySeenNanos;
    private char keySeen;
    private int myMs = -1;
    private long firedAt;
    private boolean sentEarly;
    private long lockUntil;
    private long wrongAt;
    private long escUntil;
    private long tooSoonAt;
    private final boolean[] held = new boolean[400];

    public DuelScreen(BlockPos pos) {
        super(Component.translatable("block.casino.duel_bell"));
        this.pos = pos;
    }

    public BlockPos pos() {
        return pos;
    }

    private DuelBE be() {
        var level = Minecraft.getInstance().level;
        return level != null && level.getBlockEntity(pos) instanceof DuelBE d ? d : null;
    }

    private int side(DuelBE d) {
        var p = Minecraft.getInstance().player;
        return d == null || p == null ? -1 : d.side(p.getUUID());
    }

    @Override
    protected void init() {
        DuelBE d = be();
        var p = Minecraft.getInstance().player;
        int s = side(d);
        if (!aimed && p != null && s >= 0) {
            // mirar de frente al rival
            aimed = true;
            p.setYRot(d.yaw(s));
            p.setYHeadRot(d.yaw(s));
            p.setXRot(0);
        }
        // las teclas que ya venían apretadas no cuentan hasta que se suelten
        long w = KeyChars.window();
        for (int k = 0; k < held.length; k++) {
            held[k] = KeyChars.byCode(k) != 0 && KeyChars.down(w, k);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void tick() {
        DuelBE d = be();
        int s = side(d);
        if (d == null || s < 0 || d.phase() == MatchBE.IDLE || d.phase() == MatchBE.WAITING) {
            onClose();
            return;
        }
        long w = KeyChars.window();
        for (int k = 0; k < held.length; k++) {
            if (held[k] && !KeyChars.down(w, k)) {
                held[k] = false;
            }
        }
    }

    private static void ui(net.minecraft.sounds.SoundEvent e, float pitch) {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(e, pitch, 0.7F));
    }

    @Override
    protected boolean onKey(int key, int scan, int mods) {
        DuelBE d = be();
        int s = side(d);
        long now = System.nanoTime();
        if (key == 256) {
            if (d == null || s < 0 || d.phase() != MatchBE.RUNNING || now < escUntil) {
                if (d != null && s >= 0 && d.phase() == MatchBE.RUNNING) {
                    CasinoNet.sendToServer(new WestPacket(WestPacket.MATCH_GIVE_UP, pos, 0));
                }
                onClose();
            } else {
                escUntil = now + 2_000_000_000L;
            }
            return true;
        }
        if (!KeyChars.letterOrDigit(key, scan)) {
            return true;   // las demás teclas no hacen nada durante el duelo
        }
        // ya disparé (si el servidor no lo registró en 0.6 s, se puede volver a intentar)
        boolean fired = myMs >= 0 && d != null && s >= 0 && (d.ms(s) >= 0 || now - firedAt < 600_000_000L);
        if (d == null || s < 0 || d.phase() != MatchBE.RUNNING || fired || (key >= 0 && key < held.length && held[key])) {
            return true;
        }
        char want = d.key();
        if (want == 0) {
            if (sentEarly) {
                return true;
            }
            if (d.time(0) < DuelRules.TOLL_1) {
                tooSoonAt = now;   // todavía se están acomodando: no cuenta
                return true;
            }
            // ¡antes del grito! (el servidor decide: si de verdad no había salido la tecla, pierde)
            sentEarly = true;
            char c = KeyChars.byCode(key);
            CasinoNet.sendToServer(new WestPacket(WestPacket.DUEL_KEY, pos, c));
            return true;
        }
        if (now < lockUntil) {
            return true;
        }
        if (KeyChars.is(key, scan, want)) {
            if (keySeenNanos == 0 || keySeen != want) {
                keySeenNanos = now;
                keySeen = want;
            }
            myMs = (int) Math.max(1, Math.min(9999, (now - keySeenNanos) / 1_000_000L));
            firedAt = now;
            CasinoNet.sendToServer(new WestPacket(WestPacket.DUEL_KEY, pos, myMs * 256 + want));
        } else {
            lockUntil = now + DuelRules.WRONG_LOCK_MS * 1_000_000L;
            wrongAt = now;
            ui(SoundEvents.DISPENSER_FAIL, 1.4F);
        }
        return true;
    }

    private static String sec(int ms) {
        return ms < 0 ? "—" : String.format(Locale.ROOT, "%.3f", ms / 1000.0);
    }

    @Override
    protected void draw(G g, int mx, int my, float pt) {
        DuelBE d = be();
        var p = Minecraft.getInstance().player;
        int s = side(d);
        if (d == null || p == null || s < 0) {
            return;
        }
        long now = System.nanoTime();
        int cx = width / 2;
        Component me = Component.literal(d.name(s)), rival = s == 0 ? d.rivalName() : Component.literal(d.name(0));
        // franja de arriba: quién contra quién y cuánto
        g.fill(0, 0, width, 30, 0x90180C04);
        g.fill(0, 30, width, 31, 0xC0C08A3E);
        Component vs = Component.translatable("casino.match.vs", me, rival).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD);
        g.drawCenteredString(font, vs, cx, 5, 0xFFFFFFFF);
        Component bet = Component.translatable("casino.match.bet", d.bet()).withStyle(ChatFormatting.YELLOW);
        if (d.mode() == MatchBE.NPC && d.noon()) {
            bet = bet.copy().append(Component.literal("  ")).append(Component.translatable("casino.duel.scr_noon").withStyle(ChatFormatting.GOLD));
        }
        g.drawCenteredString(font, bet, cx, 17, 0xFFFFFFFF);

        if (d.phase() == MatchBE.RESULT) {
            result(g, d, s, rival, cx, now);
            return;
        }
        char key = d.key();
        double t = d.time(pt);
        if (key == 0) {
            int tolls = t >= DuelRules.TOLL_3 ? 3 : t >= DuelRules.TOLL_2 ? 2 : t >= DuelRules.TOLL_1 ? 1 : 0;
            // tres campanas
            for (int i = 0; i < 3; i++) {
                int bx = cx - 40 + i * 30, by = 48;
                boolean rung = i < tolls;
                g.fill(bx + 4, by, bx + 16, by + 3, rung ? 0xFFE0B040 : 0x80504030);
                g.fill(bx + 2, by + 3, bx + 18, by + 16, rung ? 0xFFD8A030 : 0x80403020);
                g.fill(bx, by + 16, bx + 20, by + 19, rung ? 0xFFB07818 : 0x80302010);
                g.fill(bx + 8, by + 19, bx + 12, by + 22, rung ? 0xFF8A5A10 : 0x80302010);
            }
            if (tolls > 0) {
                WestHud.big(g, font, Component.literal(String.valueOf(tolls)).withStyle(ChatFormatting.BOLD), cx, height / 2 - 30, 4F, 0xFFE8D8B0);
            }
            Component line = tolls >= 3 ? Component.translatable("casino.duel.scr_ready").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)
                    : Component.translatable("casino.duel.scr_stay").withStyle(ChatFormatting.WHITE);
            if (tolls >= 3 && ((now / 300_000_000L) % 2 == 0)) {
                line = line.copy().withStyle(ChatFormatting.YELLOW);
            }
            box(g, cx, height / 2 + 4, line);
            if (sentEarly) {
                box(g, cx, height / 2 + 22, Component.translatable("casino.duel.scr_early").withStyle(ChatFormatting.RED, ChatFormatting.BOLD));
            } else if (now - tooSoonAt < 1_500_000_000L) {
                box(g, cx, height / 2 + 22, Component.translatable("casino.duel.scr_too_soon").withStyle(ChatFormatting.GRAY));
            }
            rules(g, cx);
        } else {
            if (keySeenNanos == 0 || keySeen != key) {
                keySeenNanos = now;
                keySeen = key;
            }
            double secs = (now - keySeenNanos) / 1e9;
            if (secs < 0.25) {
                g.fill(0, 31, width, height, 0x28FF6020);
            }
            WestHud.big(g, font, Component.translatable("casino.duel.scr_draw").withStyle(ChatFormatting.BOLD), cx, 52, 3F, 0xFFFF4020);
            boolean locked = now < lockUntil;
            int cy = height / 2 - 6, h = 34;
            int border = myMs >= 0 ? 0xFF40C040 : locked ? 0xFFE03020 : 0xFFF0D080;
            g.fill(cx - h - 3, cy - h - 3, cx + h + 3, cy + h + 3, 0xFF1A0E06);
            g.fill(cx - h - 2, cy - h - 2, cx + h + 2, cy + h + 2, border);
            g.fill(cx - h, cy - h, cx + h, cy + h, locked ? 0xFF4A1A10 : 0xFFF4ECD8);
            WestHud.big(g, font, Component.literal(String.valueOf(key)).withStyle(ChatFormatting.BOLD), cx, cy + 2, 6F, locked ? 0xFFFF8060 : 0xFF2A1A0A);
            int y = cy + h + 10;
            if (myMs >= 0) {
                box(g, cx, y, Component.translatable("casino.duel.scr_fired", sec(myMs)).withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD));
                g.drawCenteredString(font, Component.translatable("casino.duel.scr_wait").withStyle(ChatFormatting.GRAY), cx, y + 16, 0xFFFFFFFF);
            } else if (locked || now - wrongAt < 700_000_000L) {
                box(g, cx, y, Component.translatable("casino.duel.scr_wrong").withStyle(ChatFormatting.RED, ChatFormatting.BOLD));
            } else {
                box(g, cx, y, Component.translatable("casino.duel.scr_press", String.valueOf(key)).withStyle(ChatFormatting.WHITE));
            }
            g.drawCenteredString(font, Component.literal(String.format(Locale.ROOT, "%.2f s", secs)), cx, y + 30, 0xFFE0D0B0);
            if (d.ms(1 - s) >= 0) {
                g.drawCenteredString(font, Component.translatable("casino.duel.scr_rival_fired", rival).withStyle(ChatFormatting.GOLD), cx, y + 42, 0xFFFFFFFF);
            }
        }
        Component esc = now < escUntil ? Component.translatable("casino.match.esc_again").withStyle(ChatFormatting.RED, ChatFormatting.BOLD)
                : Component.translatable("casino.match.esc").withStyle(ChatFormatting.DARK_GRAY);
        g.drawCenteredString(font, esc, cx, height - 14, 0xFFFFFFFF);
    }

    /** Texto con fondo oscuro, centrado. */
    private void box(G g, int cx, int y, Component c) {
        int w = font.width(c) + 12;
        g.fill(cx - w / 2, y - 3, cx + w / 2, y + 11, 0xA0180C04);
        g.drawCenteredString(font, c, cx, y, 0xFFFFFFFF);
    }

    private void rules(G g, int cx) {
        int y = height - 54;
        for (FormattedCharSequence line : font.split(Component.translatable("casino.duel.scr_rules"), Math.min(320, width - 20))) {
            int w = font.width(line);
            g.fill(cx - w / 2 - 4, y - 2, cx + w / 2 + 4, y + 9, 0x80180C04);
            g.drawString(font, line, cx - w / 2, y, 0xFFE8DCC0, false);
            y += 11;
        }
    }

    private void result(G g, DuelBE d, int s, Component rival, int cx, long now) {
        int w = d.winner();
        boolean tie = w == MatchBE.TIE, won = w == s;
        if (!tie && !won && d.reason() == MatchBE.R_BEAT) {
            g.fill(0, 31, width, height, 0x30FF0000);
        }
        Component head = tie ? Component.translatable("casino.match.tie").withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD)
                : won ? Component.translatable("casino.match.win").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)
                : Component.translatable("casino.match.lose").withStyle(ChatFormatting.RED, ChatFormatting.BOLD);
        WestHud.big(g, font, head, cx, height / 2 - 40, 3F, 0xFFFFFFFF);
        box(g, cx, height / 2 - 14, why(d, s, rival, "casino.duel."));
        Component times = Component.translatable("casino.duel.scr_times", sec(d.ms(s)), rival, sec(d.ms(1 - s)));
        g.drawCenteredString(font, times, cx, height / 2 + 6, 0xFFE8DCC0);
        Component money = tie ? Component.translatable("casino.match.refund").withStyle(ChatFormatting.YELLOW)
                : won ? Component.translatable("casino.match.paid", d.paid()).withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD)
                : Component.translatable("casino.match.lost", d.bet()).withStyle(ChatFormatting.RED);
        box(g, cx, height / 2 + 22, money);
        g.drawCenteredString(font, Component.translatable("casino.match.close").withStyle(ChatFormatting.DARK_GRAY), cx, height - 14, 0xFFFFFFFF);
    }

    /** Por qué ganó o perdió (sirve también para las vencidas: prefix = "casino.duel." o "casino.arm."). */
    static Component why(MatchBE d, int s, Component rival, String prefix) {
        int w = d.winner();
        if (w == MatchBE.TIE) {
            return Component.translatable("casino.match.why_tie").withStyle(ChatFormatting.YELLOW);
        }
        boolean won = w == s;
        String r = switch (d.reason()) {
            case MatchBE.R_EARLY -> "early";
            case MatchBE.R_LEFT -> "left";
            case MatchBE.R_GAVE_UP -> "gave_up";
            case MatchBE.R_TIMEOUT -> "timeout";
            default -> "beat";
        };
        String k = r.equals("beat") ? prefix + (won ? "why_win_beat" : "why_lose_beat") : "casino.match." + (won ? "why_win_" : "why_lose_") + r;
        return Component.translatable(k, rival).withStyle(won ? ChatFormatting.GREEN : ChatFormatting.RED);
    }
}
