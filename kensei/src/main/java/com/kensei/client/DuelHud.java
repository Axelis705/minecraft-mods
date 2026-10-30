package com.kensei.client;

import com.kensei.KenseiConfig;
import com.kensei.combat.KatanaUtil;
import com.kensei.net.DuelStatePacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraftforge.client.gui.overlay.ForgeGui;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Los marcadores del duelo, como en un juego de pelea: tu equipo a la izquierda y el rival a la derecha (vida con estela de daño,
 * dashes, espera del crítico, guardia y estadísticas), el temporizador y las rondas ganadas arriba al centro, y los carteles grandes
 * (RONDA, 3-2-1, ¡PELEA!, K.O., VICTORIA). Los espectadores ven los dos lados completos. Tu propio panel lo dibuja {@link KatanaHud}.
 */
public final class DuelHud {
    private static final int FACE = 28;
    private static final int PANEL_H = 46;
    private static final int SKEW = 5;
    private static final Map<UUID, float[]> GHOST = new HashMap<>();

    private DuelHud() {
    }

    public static void clear() {
        GHOST.clear();
    }

    // ------------------------------------------------------------------ utilidades

    private static void slant(GuiGraphics g, int x, int y, int fillW, int h, int skew, int color, boolean mirror) {
        if (fillW <= 0) {
            return;
        }
        for (int r = 0; r < h; r++) {
            int off = h <= 1 ? 0 : Math.round(skew * (h - 1 - r) / (float) (h - 1));
            if (mirror) {
                off = skew - off;
            }
            g.fill(x + off, y + r, x + off + fillW, y + r + 1, color);
        }
    }

    private static int hpColor(float f) {
        int r;
        int gr;
        if (f > 0.5F) {
            float k = (f - 0.5F) / 0.5F;
            r = Math.round(Mth.lerp(k, 235, 70));
            gr = Math.round(Mth.lerp(k, 200, 215));
        } else {
            float k = f / 0.5F;
            r = Math.round(Mth.lerp(k, 225, 235));
            gr = Math.round(Mth.lerp(k, 50, 200));
        }
        return 0xFF000000 | (r << 16) | (gr << 8) | 0x3C;
    }

    private static int argb(int rgb, float alpha) {
        int a = Mth.clamp((int) (alpha * 255.0F), 6, 255);
        return (a << 24) | (rgb & 0xFFFFFF);
    }

    private static void bigText(GuiGraphics g, Font font, String text, float cx, float cy, float scale, int rgb, float alpha) {
        g.pose().pushPose();
        g.pose().translate(cx, cy, 0);
        g.pose().scale(scale, scale, 1.0F);
        int tw = font.width(text);
        int dark = argb(0x08080F, alpha);
        for (int[] o : new int[][] {{-1, 0}, {1, 0}, {0, -1}, {0, 1}, {-1, -1}, {1, 1}}) {
            g.drawString(font, text, -tw / 2 + o[0], -4 + o[1], dark, false);
        }
        g.drawString(font, text, -tw / 2, -4, argb(rgb, alpha), false);
        g.pose().popPose();
    }

    private static ResourceLocation skin(Minecraft mc, UUID id) {
        PlayerInfo info = mc.getConnection() == null ? null : mc.getConnection().getPlayerInfo(id);
        return info != null ? info.getSkinLocation() : DefaultPlayerSkin.getDefaultSkin(id);
    }

    private static String names(DuelStatePacket s, int team) {
        StringBuilder sb = new StringBuilder();
        for (DuelStatePacket.F f : s.fighters) {
            if (f.team == team) {
                if (sb.length() > 0) {
                    sb.append(" + ");
                }
                sb.append(f.name);
            }
        }
        return sb.toString();
    }

    // ------------------------------------------------------------------ marcador

    public static void render(ForgeGui gui, GuiGraphics g, float partial, int w, int h) {
        Minecraft mc = Minecraft.getInstance();
        DuelStatePacket s = ClientDuel.state();
        if (s == null || mc.player == null || mc.options.hideGui) {
            return;
        }
        DuelStatePacket.F self = ClientDuel.self();
        int leftTeam = self != null ? self.team : 0;
        boolean ownPanel = self != null && KatanaHud.drawsPanel(mc.player);
        int barW = Mth.clamp(w / 2 - 8 - 46 - FACE - 12, 60, 150);

        int ly = 8;
        int ry = 8;
        if (ownPanel) {
            ly = 76; // debajo de tu panel
        }
        for (DuelStatePacket.F f : s.fighters) {
            boolean left = f.team == leftTeam;
            if (left) {
                if (ownPanel && f == self) {
                    continue;
                }
                fighter(g, mc, f, 8, ly, false, barW, f.team);
                ly += PANEL_H + 4;
            } else {
                fighter(g, mc, f, w - 8, ry, true, barW, f.team);
                ry += PANEL_H + 4;
            }
        }
        timer(g, mc, s, w, leftTeam);
        banner(g, mc, s, w, h, self);
    }

    private static void fighter(GuiGraphics g, Minecraft mc, DuelStatePacket.F f, int x0, int y, boolean mirror, int barW, int team) {
        Font font = mc.font;
        long ms = System.currentTimeMillis();
        int accent = team == 0 ? 0xFF2E7F9C : 0xFFA83A44;
        boolean out = f.state != 0;
        // cara con marco
        int fx = mirror ? x0 - FACE : x0;
        g.fill(fx - 2, y - 2, fx + FACE + 2, y + FACE + 2, 0xFF0B0D12);
        g.fill(fx - 1, y - 1, fx + FACE + 1, y + FACE + 1, accent);
        g.fill(fx, y, fx + FACE, y + FACE, 0xFF161A22);
        PlayerFaceRenderer.draw(g, skin(mc, f.id), fx + 2, y + 2, FACE - 4);
        if (out) {
            g.fill(fx, y, fx + FACE, y + FACE, 0xAA120808);
            String t = f.state == 2 ? "X" : "K.O.";
            g.drawString(font, t, fx + FACE / 2 - font.width(t) / 2, y + FACE / 2 - 4, 0xFFFF5A5A, true);
        }
        // a partir de aquí, las posiciones se miden desde el borde de afuera de la pantalla
        int off = FACE + 8;
        int bx = mirror ? x0 - off - barW : x0 + off;
        // nombre
        String name = f.name;
        int nx = mirror ? bx + barW - font.width(name) : bx;
        g.drawString(font, name, nx, y - 1, out ? 0xFF8A8A8A : 0xFFFFFFFF, true);
        // vida con estela
        float frac = f.maxHp <= 0 ? 0 : Mth.clamp(f.hp / f.maxHp, 0, 1);
        float[] gh = GHOST.computeIfAbsent(f.id, k -> new float[] {frac, ms});
        float dt = Math.min(0.1F, (ms - gh[1]) / 1000.0F);
        gh[1] = ms;
        if (frac >= gh[0]) {
            gh[0] = frac;
        } else {
            gh[0] = Math.max(frac, gh[0] - dt * 0.4F);
        }
        int hy = y + 10;
        int hh = 9;
        slant(g, mirror ? bx - 1 : bx - 1, hy - 1, barW + 2, hh + 2, SKEW, 0xFF0B0D12, mirror);
        slant(g, bx, hy, barW, hh, SKEW, 0xFF2A2024, mirror);
        int gw = Math.round(barW * gh[0]);
        int fw = Math.round(barW * frac);
        slant(g, mirror ? bx + barW - gw : bx, hy, gw, hh, SKEW, 0xCCFFFFFF, mirror);
        slant(g, mirror ? bx + barW - fw : bx, hy, fw, hh, SKEW, hpColor(frac), mirror);
        slant(g, mirror ? bx + barW - fw : bx, hy, fw, 1, SKEW, 0x66FFFFFF, mirror);
        String hp = (int) Math.ceil(f.hp) + "";
        g.drawString(font, hp, mirror ? bx - font.width(hp) - 7 : bx + barW + 9, hy, 0xFFE0E0E0, true);
        // estamina
        if (KenseiConfig.STAMINA_ENABLED.get() && !out) {
            int sy = hy + hh + 2;
            slant(g, bx - 1, sy - 1, barW + 2, 5, 3, 0xFF0B0D12, mirror);
            slant(g, bx, sy, barW, 3, 3, 0xFF1B2218, mirror);
            int sw = Math.round(barW * Mth.clamp(f.stamina, 0, 1));
            int scol = f.exhausted ? ((ms / 110) % 2 == 0 ? 0xFFE03A30 : 0xFF7A2420) : f.stamina > 0.5F ? 0xFF4CC36A : f.stamina > 0.25F ? 0xFFE0C040 : 0xFFE0703A;
            slant(g, mirror ? bx + barW - sw : bx, sy, sw, 3, 3, scol, mirror);
        }
        // dashes
        int pips = Math.max(1, Math.round(KenseiConfig.maxEnergy() / 100.0F));
        int pw = 19;
        int py = y + 25;
        for (int i = 0; i < pips; i++) {
            int px = mirror ? bx + barW - (i + 1) * (pw + 3) + 3 : bx + i * (pw + 3);
            slant(g, px - 1, py - 1, pw + 2, 7, 3, 0xFF0B0D12, mirror);
            slant(g, px, py, pw, 5, 3, 0xFF22303A, mirror);
            float fill = Mth.clamp(f.energy / 100.0F - i, 0, 1);
            if (fill > 0) {
                slant(g, mirror ? px + pw - Math.round(pw * fill) : px, py, Math.round(pw * fill), 5, 3, fill >= 0.999F ? 0xFF2CC8FF : 0xFF1B7FA6, mirror);
            }
        }
        // espera del crítico
        float ready = f.heavyCd <= 0 ? 1.0F
                : 1.0F - Math.min(1.0F, f.heavyCd / (float) (KenseiConfig.HEAVY_COOLDOWN.get() + KenseiConfig.HEAVY_WINDUP.get()));
        int cw = 30;
        int cx = mirror ? bx + barW - pips * (pw + 3) - cw - 6 : bx + pips * (pw + 3) + 4;
        slant(g, cx, py + 1, cw, 3, 2, 0xFF2B2B33, mirror);
        slant(g, mirror ? cx + cw - Math.round(cw * ready) : cx, py + 1, Math.round(cw * ready), 3, 2, f.heavyCd <= 0 ? 0xFFE8B030 : 0xFF8A6A20, mirror);
        // guardia
        if (f.guard && !out) {
            String t = Component.translatable("kensei.hud.guard").getString();
            int tx = mirror ? cx - font.width(t) - 5 : cx + cw + 5;
            g.drawString(font, t, tx, py - 1, 0xFF8FD8FF, true);
        }
        // estadísticas pequeñas
        g.pose().pushPose();
        float sc = 0.72F;
        String st = Component.translatable("kensei.duel.stat_parry").getString() + " " + f.parries + "   "
                + Component.translatable("kensei.duel.stat_dodge").getString() + " " + f.dodges + "   "
                + Component.translatable("kensei.duel.stat_hits").getString() + " " + f.hits;
        float sx = mirror ? (bx + barW - font.width(st) * sc) : bx;
        g.pose().translate(sx, y + 35, 0);
        g.pose().scale(sc, sc, 1.0F);
        g.drawString(font, st, 0, 0, 0xFFB4BCC8, true);
        g.pose().popPose();
    }

    private static void timer(GuiGraphics g, Minecraft mc, DuelStatePacket s, int w, int leftTeam) {
        Font font = mc.font;
        int cx = w / 2;
        int secs = (int) Math.ceil(ClientDuel.timeLeftTicks() / 20.0F);
        boolean low = s.phase == DuelStatePacket.PHASE_FIGHT && secs <= 10;
        long ms = System.currentTimeMillis();
        g.fill(cx - 24, 5, cx + 24, 31, 0xFF0B0D12);
        g.fill(cx - 23, 6, cx + 23, 30, 0xFF1A1E27);
        g.fill(cx - 23, 6, cx + 23, 7, 0xFF2E7F9C);
        String t = Integer.toString(secs);
        int col = low ? (((ms / 250) % 2 == 0) ? 0xFFFF5A5A : 0xFFFFB0B0) : 0xFFFFFFFF;
        g.pose().pushPose();
        g.pose().translate(cx, 10, 0);
        g.pose().scale(2.0F, 2.0F, 1.0F);
        g.drawString(font, t, -font.width(t) / 2, 0, col, true);
        g.pose().popPose();
        // ronda
        String r = Component.translatable("kensei.duel.round", s.round).getString();
        g.pose().pushPose();
        g.pose().translate(cx - font.width(r) * 0.75F / 2.0F, 34, 0);
        g.pose().scale(0.75F, 0.75F, 1.0F);
        g.drawString(font, r, 0, 0, 0xFFC8D0DC, true);
        g.pose().popPose();
        // rondas ganadas: un rombo por cada una que hace falta
        int winsLeft = leftTeam == 0 ? s.winsA : s.winsB;
        int winsRight = leftTeam == 0 ? s.winsB : s.winsA;
        for (int i = 0; i < s.roundsToWin; i++) {
            pip(g, cx - 32 - i * 12, 12, i < winsLeft, 0xFF2E9CCF);
            pip(g, cx + 32 + i * 12, 12, i < winsRight, 0xFFCF4A56);
        }
    }

    private static void pip(GuiGraphics g, int x, int y, boolean on, int color) {
        g.fill(x - 5, y - 1, x + 5, y + 9, 0xFF0B0D12);
        g.fill(x - 4, y, x + 4, y + 8, on ? color : 0xFF262B36);
        if (on) {
            g.fill(x - 4, y, x + 4, y + 2, 0x66FFFFFF);
        }
    }

    // ------------------------------------------------------------------ carteles grandes

    private static void banner(GuiGraphics g, Minecraft mc, DuelStatePacket s, int w, int h, DuelStatePacket.F self) {
        Font font = mc.font;
        float cx = w / 2.0F;
        float cy = h * 0.30F;
        int left = ClientDuel.phaseTicksLeft();
        int el = s.phaseTotal - left;
        long ms = System.currentTimeMillis();
        String text = null;
        String sub = null;
        int rgb = 0xFFFFFF;
        float scale = 4.0F;
        float t = 0; // ticks desde que apareció el texto
        switch (s.phase) {
            case DuelStatePacket.PHASE_INTRO -> {
                if (el < 24) {
                    boolean last = s.winsA == s.roundsToWin - 1 && s.winsB == s.roundsToWin - 1 && s.roundsToWin > 1;
                    text = Component.translatable(last ? "kensei.duel.final_round" : "kensei.duel.round", s.round).getString();
                    rgb = last ? 0xFF5A5A : 0xFFE8B0;
                    t = el;
                } else {
                    int n = 3 - Math.min(2, (el - 24) / 18);
                    text = Integer.toString(n);
                    rgb = n == 1 ? 0xFFE060 : 0xFFFFFF;
                    scale = 6.0F;
                    t = (el - 24) % 18;
                }
            }
            case DuelStatePacket.PHASE_FIGHT -> {
                int fightEl = s.roundTicks - ClientDuel.timeLeftTicks();
                if (fightEl < 34) {
                    text = Component.translatable("kensei.duel.fight").getString();
                    rgb = fightEl < 6 ? 0xFFFFFF : 0xFF4A3A;
                    scale = 5.0F;
                    t = fightEl;
                }
            }
            case DuelStatePacket.PHASE_ROUND_END -> {
                text = Component.translatable(s.reason == DuelStatePacket.REASON_KO ? "kensei.duel.ko"
                        : s.reason == DuelStatePacket.REASON_TIME ? "kensei.duel.time" : "kensei.duel.tie").getString();
                rgb = s.reason == DuelStatePacket.REASON_KO ? 0xFF4A4A : 0xFFFFFF;
                scale = 5.5F;
                t = el;
                if (s.winner >= 0 && el > 10) {
                    sub = Component.translatable("kensei.duel.round_winner", names(s, s.winner)).getString();
                }
            }
            case DuelStatePacket.PHASE_MATCH_END -> {
                if (s.winner < 0) {
                    text = Component.translatable("kensei.duel.tie").getString();
                } else if (self != null) {
                    boolean won = self.team == s.winner;
                    text = Component.translatable(won ? "kensei.duel.victory" : "kensei.duel.defeat").getString();
                    rgb = won ? 0xFFD040 : 0xC86A6A;
                } else {
                    text = Component.translatable("kensei.duel.wins", names(s, s.winner)).getString().toUpperCase(Locale.ROOT);
                    rgb = 0xFFD040;
                    scale = 3.0F;
                }
                if (s.winner >= 0) {
                    int a = s.winner == 0 ? s.winsA : s.winsB;
                    int b = s.winner == 0 ? s.winsB : s.winsA;
                    sub = a + " - " + b;
                }
                t = el;
            }
            default -> {
            }
        }
        if (text == null) {
            return;
        }
        float pop = 1.0F + 0.8F * (float) Math.exp(-t * 0.6);
        float alpha = 1.0F;
        if (s.phase == DuelStatePacket.PHASE_FIGHT) {
            alpha = Mth.clamp(1.0F - (t - 22) / 12.0F, 0, 1);
            pop += t * 0.012F;
        } else if (s.phase == DuelStatePacket.PHASE_INTRO) {
            alpha = Mth.clamp(1.0F - (t - 14) / 4.0F, 0, 1) * (el < 24 ? Mth.clamp((24 - el) / 5.0F, 0, 1) : 1.0F);
        }
        bigText(g, font, text, cx, cy, scale * pop, rgb, alpha);
        if (sub != null) {
            bigText(g, font, sub, cx, cy + 12 * scale * 0.55F + 14, 1.8F, 0xFFFFFF, 1.0F);
        }
    }
}
