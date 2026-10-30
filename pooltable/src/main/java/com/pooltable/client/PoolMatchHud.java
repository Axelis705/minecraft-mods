package com.pooltable.client;

import com.pooltable.block.PoolTableBE;
import com.pooltable.game.PoolPhysics;
import com.pooltable.game.PoolRules;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraftforge.client.gui.overlay.ForgeGui;

import java.util.List;

/**
 * Panel de la partida (a la izquierda de la pantalla) mientras estás cerca de una mesa con jugadores apuntados:
 * modo, jugadores y marcador, bolas que quedan, turno y últimos sucesos. Sustituye al cartel flotante.
 */
public final class PoolMatchHud {
    private static final int PW = 178;
    private static final int X0 = 6;
    private static final int Y0 = 6;
    private static final double REACH = 14.0;
    private static final int[] BALL_RGB = {0xF5F5EB, 0xF0C814, 0x1E46BE, 0xDC2823, 0x6E2896, 0xF07814, 0x1E8C3C, 0x821E28, 0x141416};
    private static final int TEAM_A = 0x55D8FF;
    private static final int TEAM_B = 0xFFAA55;

    private static PoolTableBE cached;
    private static long lastSearch = -100;

    private PoolMatchHud() {
    }

    /** La mesa de partida cercana; se busca cada medio segundo para no recorrer el mundo en cada imagen. */
    private static PoolTableBE near(Minecraft mc, LocalPlayer p) {
        long t = mc.level.getGameTime();
        if (cached != null && (cached.isRemoved() || cached.getLevel() != mc.level)) {
            cached = null;
        }
        if (t - lastSearch >= 10 || t < lastSearch) {
            lastSearch = t;
            cached = PoolTableBE.findNear(mc.level, p.position(), REACH);
        } else if (cached != null && cached.distanceTo(p.position()) > REACH) {
            cached = null;
        }
        return cached;
    }

    public static void render(ForgeGui gui, GuiGraphics g, float partial, int sw, int sh) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null || mc.level == null || mc.options.hideGui || mc.options.renderDebug) {
            return;
        }
        PoolTableBE t = near(mc, p);
        if (t == null || t.pids.isEmpty()) {
            return;
        }
        Font font = mc.font;
        PoolRules r = t.rules;
        PoolRules.Mode mode = t.mode();
        boolean playing = t.playing;
        int n = playing ? Math.max(1, r.players) : t.pids.size();
        boolean teams = mode.teams(n);
        boolean mine = playing && t.currentPlayer() != null && t.currentPlayer().equals(p.getUUID());

        // ---- cuánto mide el panel
        List<FormattedCharSequence> logLines = new java.util.ArrayList<>();
        if (playing) {
            for (int i = Math.max(0, t.log.size() - PoolTableBE.LOG_MAX); i < t.log.size(); i++) {
                logLines.addAll(font.split(t.log.get(i), PW - 12));
            }
            while (logLines.size() > 5) {
                logLines.remove(0);
            }
        }
        int h = 6 + 12 + 2 + 12 + 4;           // título y subtítulo
        h += t.pids.size() * 14 + 4;           // jugadores
        if (playing) {
            h += 11 + 14 + 4;                  // bolas
        }
        h += 18 + 4;                           // turno / estado
        if (!playing) {
            h += 11;                           // pista del menú
        }
        if (!logLines.isEmpty()) {
            h += 10 + logLines.size() * 10 + 4;
        }

        g.fill(X0, Y0, X0 + PW, Y0 + h, 0xC0101418);
        g.fill(X0, Y0, X0 + PW, Y0 + 2, 0xFF3C8A5A);
        g.fill(X0, Y0 + h - 1, X0 + PW, Y0 + h, 0xFF2A5A3C);
        int y = Y0 + 6;

        // ---- título y subtítulo
        g.drawCenteredString(font, Component.translatable("pooltable.mode." + mode.id + ".name").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), X0 + PW / 2, y, 0xFFFFFF);
        y += 14;
        Component sub = switch (mode) {
            case SOLO -> Component.translatable("pooltable.panel.shots", r.shots, r.best > 0 ? Integer.toString(r.best) : "-");
            case EIGHT -> Component.translatable(teams ? "pooltable.panel.teams" : "pooltable.panel.groups");
            case NINE -> Component.translatable("pooltable.panel.target", PoolRules.targetBall(t.ph));
            case ROTATION -> Component.translatable("pooltable.panel.goal", PoolRules.ROTATION_TARGET);
        };
        if (!playing) {
            sub = Component.translatable("pooltable.panel.lobby");
        }
        g.drawCenteredString(font, sub.copy().withStyle(ChatFormatting.GRAY), X0 + PW / 2, y, 0xFFFFFF);
        y += 14;

        // ---- jugadores
        for (int i = 0; i < t.pids.size(); i++) {
            boolean turn = playing && i == r.turn;
            if (turn) {
                g.fill(X0 + 3, y - 1, X0 + PW - 3, y + 12, 0x50FFE040);
                g.fill(X0 + 3, y - 1, X0 + 5, y + 12, 0xFFFFE040);
            }
            int team = r.teamOf(i);
            int nameCol = turn ? 0xFFFF55 : 0xFFFFFF;
            int x = X0 + 9;
            if (teams) {
                g.fill(x, y + 2, x + 4, y + 9, 0xFF000000 | (team == 0 ? TEAM_A : TEAM_B));
                x += 7;
            }
            String nm = font.plainSubstrByWidth((i + 1) + ". " + t.pnames.get(i), teams ? 82 : 92);
            g.drawString(font, nm, x, y + 2, nameCol, true);
            String right = "";
            if (playing) {
                switch (mode) {
                    case ROTATION -> right = r.score[i] + "/" + PoolRules.ROTATION_TARGET;
                    case EIGHT -> {
                        int gr = r.group[team];
                        if (gr >= 0) {
                            right = Component.translatable(gr == 0 ? "pooltable.group.solids" : "pooltable.group.stripes").getString() + " " + t.ph.remaining(gr);
                        }
                    }
                    default -> {
                    }
                }
            } else if (teams) {
                right = team == 0 ? "A" : "B";
            }
            if (!right.isEmpty()) {
                g.drawString(font, right, X0 + PW - 8 - font.width(right), y + 2, 0xFFE0E0E0, true);
            }
            if (playing && mode == PoolRules.Mode.ROTATION) {
                int bw = PW - 18;
                g.fill(X0 + 9, y + 11, X0 + 9 + bw, y + 12, 0xFF2A3038);
                g.fill(X0 + 9, y + 11, X0 + 9 + Math.min(bw, bw * r.score[i] / PoolRules.ROTATION_TARGET), y + 12, 0xFF50D070);
            }
            y += 14;
        }
        y += 4;

        // ---- bolas que quedan en la mesa
        if (playing) {
            g.drawString(font, Component.translatable("pooltable.panel.on_table").withStyle(ChatFormatting.GRAY), X0 + 8, y, 0xFFFFFF, false);
            y += 11;
            int target = (mode == PoolRules.Mode.NINE || mode == PoolRules.Mode.ROTATION) ? PoolRules.targetBall(t.ph) : -1;
            int cx = X0 + 8;
            int last = 0;
            for (int b = 1; b < PoolPhysics.N; b++) {
                if ((t.offMask & (1 << b)) != 0) {
                    continue;
                }
                drawChip(g, font, cx, y, b, !t.ph.out[b], b == target);
                cx += 11;
                last = b;
            }
            // subrayado del grupo de quien tira (8 bolas)
            if (mode == PoolRules.Mode.EIGHT && r.group[r.teamOf(r.turn)] >= 0) {
                int gr = r.group[r.teamOf(r.turn)];
                int from = gr == 0 ? 1 : 9;
                int to = gr == 0 ? 7 : 15;
                g.fill(X0 + 8 + (from - 1) * 11, y + 12, X0 + 8 + (to - 1) * 11 + 10, y + 13, 0xFFFFE040);
            }
            y += 14 + 4;
        }

        // ---- turno / estado
        if (playing) {
            String who = r.turn < t.pnames.size() ? t.pnames.get(r.turn) : "?";
            g.fill(X0 + 4, y, X0 + PW - 4, y + 16, mine ? 0xD0206A34 : 0xD0303842);
            Component line = mine ? Component.translatable("pooltable.panel.your_turn").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD)
                    : Component.translatable("pooltable.status.turn", Component.literal(who).withStyle(ChatFormatting.YELLOW)).withStyle(ChatFormatting.WHITE);
            g.drawCenteredString(font, line, X0 + PW / 2, y + 4, 0xFFFFFF);
        } else {
            boolean ok = t.validCount();
            g.fill(X0 + 4, y, X0 + PW - 4, y + 16, ok ? 0xD0206A34 : 0xD05A4A18);
            g.drawCenteredString(font, Component.translatable(ok ? "pooltable.panel.ready" : "pooltable.panel.waiting", Component.translatable("pooltable.mode." + mode.id + ".count")),
                    X0 + PW / 2, y + 4, 0xFFFFFF);
        }
        y += 18 + 4;
        if (!playing) {
            g.drawCenteredString(font, Component.translatable("pooltable.panel.hint").withStyle(ChatFormatting.DARK_GRAY), X0 + PW / 2, y - 2, 0xFFFFFF);
            y += 11;
        }

        // ---- últimos sucesos
        if (!logLines.isEmpty()) {
            g.fill(X0 + 4, y - 2, X0 + PW - 4, y - 1, 0x40FFFFFF);
            g.drawString(font, Component.translatable("pooltable.panel.log").withStyle(ChatFormatting.GRAY), X0 + 8, y + 1, 0xFFFFFF, false);
            y += 10;
            for (FormattedCharSequence l : logLines) {
                g.drawString(font, l, X0 + 8, y + 1, 0xFFFFFF, false);
                y += 10;
            }
        }
    }

    /** Una bola en miniatura con su número; las lisas son de un color y las rayadas llevan una franja. */
    private static void drawChip(GuiGraphics g, Font font, int x, int y, int ball, boolean onTable, boolean target) {
        if (target) {
            g.fill(x - 1, y - 1, x + 11, y + 11, 0xFFFFE040);
        }
        if (!onTable) {
            g.fill(x, y, x + 10, y + 10, 0xFF262A30);
            g.fill(x + 1, y + 1, x + 9, y + 9, 0xFF14171B);
            return;
        }
        int rgb = BALL_RGB[ball <= 8 ? ball : ball - 8];
        int col = 0xFF000000 | rgb;
        g.fill(x + 1, y, x + 9, y + 10, col);
        g.fill(x, y + 1, x + 10, y + 9, col);
        if (ball > 8) {
            // rayada: blanca arriba y abajo, color en el centro
            g.fill(x + 1, y, x + 9, y + 3, 0xFFF5F5EB);
            g.fill(x + 1, y + 7, x + 9, y + 10, 0xFFF5F5EB);
            g.fill(x, y + 3, x + 10, y + 7, col);
        }
        // número pequeño
        String s = Integer.toString(ball);
        float sc = 0.6F;
        int light = (ball == 1 || ball == 9 || ball == 0) ? 0xFF101010 : 0xFFFFFFFF;
        int tw = font.width(s);
        boolean band = ball > 8;
        if (band) {
            g.fill(x + 3, y + 2, x + 7, y + 8, 0xFFF5F5EB);
            light = 0xFF101010;
        }
        g.pose().pushPose();
        g.pose().translate(x + 5 - tw * sc / 2.0F, y + 5 - 8 * sc / 2.0F + 0.5F, 0);
        g.pose().scale(sc, sc, 1);
        g.drawString(font, s, 0, 0, light, false);
        g.pose().popPose();
    }
}
