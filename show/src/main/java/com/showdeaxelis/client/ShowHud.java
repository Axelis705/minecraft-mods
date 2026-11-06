package com.showdeaxelis.client;

import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraftforge.client.gui.overlay.ForgeGui;

/** Todo lo que se dibuja en pantalla durante el show: marcador, rotulos, preguntas, tablero, locutor, confeti. */
public final class ShowHud {
    private static final int[] OPT_COLORS = {0xE53935, 0x1E88E5, 0xFDD835, 0x43A047};
    private static final String[] KEYS = {"Z", "X", "C", "V"};

    private ShowHud() {
    }

    private static int argb(int rgb) {
        return 0xFF000000 | rgb;
    }

    private static int dark(int rgb, float k) {
        int r = (int) (((rgb >> 16) & 255) * k);
        int g = (int) (((rgb >> 8) & 255) * k);
        int b = (int) ((rgb & 255) * k);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    private static void text(GuiGraphics g, Font f, String s, float x, float y, int color, float scale, boolean center) {
        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        g.pose().scale(scale, scale, 1f);
        int w = f.width(s);
        g.drawString(f, s, center ? -w / 2 : 0, 0, color, true);
        g.pose().popPose();
    }

    private static void panel(GuiGraphics g, int x, int y, int w, int h, int border) {
        g.fill(x - 2, y - 2, x + w + 2, y + h + 2, border);
        g.fill(x, y, x + w, y + h, 0xE0101018);
    }

    private static List<String> strs(ListTag l) {
        java.util.ArrayList<String> out = new java.util.ArrayList<>();
        for (int i = 0; i < l.size(); i++) {
            out.add(l.getString(i));
        }
        return out;
    }

    private static void wrapped(GuiGraphics g, Font f, String s, int x, int y, int w, int color, boolean center) {
        List<FormattedCharSequence> lines = f.split(Component.literal(s), w);
        int yy = y;
        for (FormattedCharSequence l : lines) {
            int lw = f.width(l);
            g.drawString(f, l, center ? x + (w - lw) / 2 : x, yy, color, true);
            yy += 10;
        }
    }

    private static int wrappedHeight(Font f, String s, int w) {
        return Math.max(1, f.split(Component.literal(s), w).size()) * 10;
    }

    /** Donde empieza el contenido de cada juego (justo debajo del aviso de arriba). */
    private static int top = 8;
    private static boolean compact;

    private static boolean isCompact(Minecraft mc, CompoundTag st) {
        if (ShowClient.hudMode == 1) {
            return true;
        }
        if (ShowClient.hudMode == 2) {
            return false;
        }
        CompoundTag stage = st.getCompound("stage");
        if (!stage.contains("x") || mc.player == null) {
            return false;
        }
        double dx = mc.player.getX() - stage.getInt("x");
        double dz = mc.player.getZ() - stage.getInt("z");
        return dx * dx + dz * dz < 90.0 * 90.0 && Math.abs(mc.player.getY() - stage.getInt("y")) < 40;
    }

    public static void render(ForgeGui gui, GuiGraphics g, float pt, int W, int H) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui || !ShowClient.active()) {
            return;
        }
        CompoundTag st = ShowClient.state;
        Font f = mc.font;
        String phase = st.getString("phase");
        ListTag sides = st.getList("sides", 10);
        CompoundTag you = st.getCompound("you");
        // si el panel del presentador esta abierto, el HUD se corre a la izquierda para no taparse
        if (mc.screen instanceof HostScreen) {
            W -= HostScreen.PW + 14;
        }
        compact = isCompact(mc, st);

        top = drawBanner(g, f, st, phase, W) + 6;
        top = drawSay(g, f, W, top) + 4;
        if (phase.equals("DRAW") && st.contains("dr", 10)) {
            drawDraw(g, f, st, sides, W, H);
        } else if (compact) {
            drawCompact(g, f, st, phase, W, H);
        } else if (phase.equals("R100") && st.contains("r", 10)) {
            drawBoard(g, f, st, sides, W, H);
        } else if (phase.equals("TRIVIA") && st.contains("t", 10)) {
            drawTrivia(g, f, st, W, H);
        } else if (phase.equals("IMPOSTOR") && st.contains("imp", 10)) {
            drawImpostor(g, f, st, W, H);
        } else if (phase.equals("TABOO") && st.contains("tb", 10)) {
            drawTaboo(g, f, st, sides, W, H, you);
        } else if (phase.equals("FINAL")) {
            drawFinal(g, f, sides, W, H);
        }
        if (!phase.equals("LOBBY")) {
            drawScores(g, f, st, sides, W, H);
        }
        drawTitle(g, f, W, H);
        drawStrike(g, f, W, H);
        drawPopups(g, f, sides, W, H);
        drawFx(g, mc.getWindow().getGuiScaledWidth(), H);
    }

    // ------------------------------------------------------------ piezas

    private static String phaseLabel(CompoundTag st, String phase) {
        return switch (phase) {
            case "R100" -> "100 MEXICANOS DIJERON";
            case "TRIVIA" -> st.getCompound("t").getString("cat").toUpperCase(java.util.Locale.ROOT);
            case "IMPOSTOR" -> "EL IMPOSTOR";
            case "TABOO" -> "ADIVINA LA PALABRA";
            case "DRAW" -> "ADIVINA EL DIBUJO";
            case "FINAL" -> "FINAL";
            case "LOBBY" -> "PREPARACION";
            default -> "SHOW DE AXELIS";
        };
    }

    /** El aviso de arriba: en que juego estamos, que esta pasando y que tienes que hacer tu. Devuelve donde termina. */
    private static int drawBanner(GuiGraphics g, Font f, CompoundTag st, String phase, int W) {
        Guide.Tip tip = Guide.tip(st);
        int bw = Math.min(440, W - 16);
        int x = (W - bw) / 2;
        int y = 6;
        int inner = bw - 16;
        List<FormattedCharSequence> what = f.split(Component.literal(tip.what()), inner);
        List<FormattedCharSequence> todo = tip.todo().isEmpty() ? List.of() : f.split(Component.literal(tip.todo()), inner);
        int h = 14 + Math.min(2, what.size()) * 10 + 3 + Math.min(2, todo.size()) * 10 + 4;
        boolean pulse = tip.mine() && (ShowClient.clientTick / 10) % 2 == 0;
        int border = tip.mine() ? (pulse ? 0xFF69F0AE : 0xFF2E9E5B) : 0xFFFFC107;
        g.fill(x - 2, y - 2, x + bw + 2, y + h + 2, border);
        g.fill(x, y, x + bw, y + h, 0xE8101018);
        String label = phaseLabel(st, phase) + (st.getInt("round") > 0 && !phase.equals("LOBBY") ? "  -  Ronda " + st.getInt("round") : "");
        g.drawString(f, label, x + 8, y + 3, 0xFFB0B0D0, false);
        if (st.getBoolean("voice")) {
            String v = "MODO VOZ";
            g.drawString(f, v, x + bw - 8 - f.width(v), y + 3, 0xFF80DEEA, false);
        }
        int yy = y + 14;
        for (int i = 0; i < Math.min(2, what.size()); i++) {
            g.drawString(f, what.get(i), x + 8, yy, 0xFFFFFFFF, true);
            yy += 10;
        }
        yy += 3;
        for (int i = 0; i < Math.min(2, todo.size()); i++) {
            g.drawString(f, todo.get(i), x + 8, yy, tip.mine() ? 0xFFB9F6CA : 0xFFFFE082, true);
            yy += 10;
        }
        return y + h + 2;
    }

    /** Version chica del HUD para quien esta dentro del estudio: lo importante se ve en la pantalla gigante. */
    private static void drawCompact(GuiGraphics g, Font f, CompoundTag st, String phase, int W, int H) {
        CompoundTag you = st.getCompound("you");
        int bw = Math.min(440, W - 16);
        int x = (W - bw) / 2;
        int y = top;
        switch (phase) {
            case "TRIVIA" -> {
                CompoundTag t = st.getCompound("t");
                List<String> ops = strs(t.getList("op", 8));
                boolean rev = t.getBoolean("rev");
                int ok = t.contains("ok") ? t.getInt("ok") : -1;
                int mine = t.getInt("mine");
                int cw = (bw - 3 * 4) / Math.max(1, ops.size());
                for (int i = 0; i < ops.size(); i++) {
                    int c = ops.size() == 2 ? (i == 0 ? 0x43A047 : 0xE53935) : OPT_COLORS[i % 4];
                    int cx = x + i * (cw + 4);
                    boolean sel = !rev && mine == i || rev && i == ok;
                    if (sel) {
                        g.fill(cx - 2, y - 2, cx + cw + 2, y + 22, 0xFFFFFFFF);
                    }
                    g.fill(cx, y, cx + cw, y + 20, rev && ok >= 0 && i != ok ? dark(c, 0.3f) : argb(c));
                    int tc = c == 0xFDD835 ? 0xFF222222 : 0xFFFFFFFF;
                    String label = (char) ('A' + i) + (you.getBoolean("player") ? "  (" + KEYS[i] + ")" : "");
                    g.drawString(f, label, cx + (cw - f.width(label)) / 2, y + 6, tc, false);
                }
                y += 26;
                if (!rev) {
                    drawTimer(g, x, y, bw, t.getInt("left"), t.getInt("total"));
                }
            }
            case "IMPOSTOR" -> {
                CompoundTag im = st.getCompound("imp");
                String stage = im.getString("stage");
                String line = null;
                int color = 0xFFFFFFFF;
                if (you.getBoolean("player") || you.getBoolean("host")) {
                    if (im.getBoolean("isImp") && !stage.equals("DONE")) {
                        line = "ERES EL IMPOSTOR  -  categoria: " + im.getString("cat");
                        color = 0xFFFF5252;
                    } else if (im.contains("word")) {
                        line = "Palabra secreta: " + im.getString("word") + "  (" + im.getString("cat") + ")";
                        color = 0xFF69F0AE;
                    }
                }
                if (line != null) {
                    g.fill(x, y, x + bw, y + 16, 0xE0101018);
                    g.drawCenteredString(f, line, x + bw / 2, y + 4, color);
                    y += 20;
                }
                if (you.getBoolean("host") && im.contains("imp") && !stage.equals("DONE")) {
                    g.drawCenteredString(f, "Impostor: " + im.getString("imp"), x + bw / 2, y, 0xFFFFD54F);
                }
            }
            case "TABOO" -> {
                CompoundTag tb = st.getCompound("tb");
                if (tb.contains("word") && !tb.getBoolean("over")) {
                    g.fill(x, y, x + bw, y + 36, 0xE0101018);
                    text(g, f, tb.getString("word").toUpperCase(java.util.Locale.ROOT), x + bw / 2f, y + 4, 0xFFFFFFFF, 1.8f, true);
                    g.drawCenteredString(f, "Prohibido: " + String.join(", ", strs(tb.getList("no", 8))), x + bw / 2, y + 24, 0xFFFF8A80);
                    y += 40;
                }
                if (!tb.getBoolean("over")) {
                    drawTimer(g, x, y, bw, tb.getInt("left"), tb.getInt("total"));
                }
            }
            case "R100" -> {
                CompoundTag r = st.getCompound("r");
                if (you.getBoolean("host") && !r.getString("pending").isEmpty()) {
                    String s = "Escribio: \"" + r.getString("pending") + "\"";
                    g.drawCenteredString(f, s, x + bw / 2, y, 0xFFFFD54F);
                }
            }
            default -> {
            }
        }
    }

    private static int scoreChipW(int n, int W) {
        return Math.max(54, Math.min(92, (W - 20) / Math.max(1, n) - 6));
    }

    private static void drawScores(GuiGraphics g, Font f, CompoundTag st, ListTag sides, int W, int H) {
        int n = 0;
        for (int i = 0; i < sides.size(); i++) {
            if (sides.getCompound(i).getList("m", 8).size() > 0) {
                n++;
            }
        }
        if (n == 0) {
            return;
        }
        int bw = scoreChipW(n, W);
        int total = bw * n + 6 * (n - 1);
        int x = W - total - 8;
        int y = H - 22 - 6 - 30;
        CompoundTag r = st.getCompound("r");
        int control = st.contains("r", 10) ? r.getInt("control") : -1;
        for (int i = 0; i < sides.size(); i++) {
            CompoundTag s = sides.getCompound(i);
            if (s.getList("m", 8).isEmpty()) {
                continue;
            }
            int c = s.getInt("c");
            boolean hot = i == control;
            g.fill(x - 2, y - 2, x + bw + 2, y + 30, hot ? 0xFFFFFFFF : dark(c, 0.45f));
            g.fill(x, y, x + bw, y + 28, argb(c));
            g.fill(x, y + 16, x + bw, y + 28, dark(c, 0.6f));
            String name = s.getString("n");
            boolean light = c == 0xFDD835;
            int tc = light ? 0xFF222222 : 0xFFFFFFFF;
            String shown = f.width(name) > bw - 4 ? f.plainSubstrByWidth(name, bw - 4) : name;
            g.drawString(f, shown, x + (bw - f.width(shown)) / 2, y + 2, tc, false);
            text(g, f, String.valueOf(s.getInt("s")), x + bw / 2f, y + 8, tc, 1.5f, true);
            x += bw + 6;
        }
    }

    private static void drawBoard(GuiGraphics g, Font f, CompoundTag st, ListTag sides, int W, int H) {
        CompoundTag r = st.getCompound("r");
        boolean host = st.getCompound("you").getBoolean("host");
        ListTag as = r.getList("a", 10);
        int bw = Math.min(330, W - 120);
        int x = (W - bw) / 2;
        int y = top;
        int qh = wrappedHeight(f, r.getString("q"), bw - 12) + 8;
        panel(g, x, y, bw, qh, 0xFFFFC107);
        wrapped(g, f, r.getString("q"), x + 6, y + 4, bw - 12, 0xFFFFFFFF, true);
        y += qh + 8;
        int rows = (as.size() + 1) / 2;
        int bh = rows * 17 + 8;
        panel(g, x, y, bw, bh, 0xFF3F51B5);
        int cw = (bw - 12) / 2;
        for (int i = 0; i < as.size(); i++) {
            CompoundTag a = as.getCompound(i);
            int cx = x + 4 + (i / rows) * (cw + 4);
            int cy = y + 4 + (i % rows) * 17;
            boolean rev = a.getBoolean("rev");
            g.fill(cx, cy, cx + cw, cy + 15, rev ? 0xFF2E7D32 : 0xFF283593);
            g.drawString(f, String.valueOf(i + 1), cx + 4, cy + 4, 0xFFFFEB3B, true);
            if (rev) {
                String t = a.getString("t");
                g.drawString(f, t, cx + 16, cy + 4, 0xFFFFFFFF, true);
                String p = String.valueOf(a.getInt("p"));
                g.drawString(f, p, cx + cw - 4 - f.width(p), cy + 4, 0xFFFFEB3B, true);
            } else if (host && a.contains("t")) {
                g.drawString(f, a.getString("t") + " (" + a.getInt("p") + ")", cx + 16, cy + 4, 0xFF9FA8DA, false);
            } else {
                g.drawString(f, "- - - - -", cx + 16, cy + 4, 0xFF7986CB, false);
            }
        }
        y += bh + 6;
        String info = switch (r.getInt("stage")) {
            case 0 -> "CARA A CARA - toca el timbre quien da la cara de cada equipo";
            case 1 -> "Juega " + sideName(sides, r.getInt("control")) + (r.getString("ans").isEmpty() ? "" : "  -  responde " + r.getString("ans"));
            case 2 -> "ROBO: " + (r.getString("ans").isEmpty() ? "el equipo contrario" : r.getString("ans")) + " puede robar el banco";
            default -> "Ronda terminada";
        };
        String line = "BANCO: " + r.getInt("bank") + (r.getInt("mult") > 1 ? " x" + r.getInt("mult") : "") + "   " + info;
        int iw = Math.min(bw, f.width(line) + 12);
        g.fill(x + (bw - iw) / 2, y, x + (bw + iw) / 2, y + 14, 0xC0000000);
        g.drawString(f, line, x + (bw - f.width(line)) / 2, y + 3, 0xFFFFFFFF, true);
        y += 18;
        if (r.getInt("strikes") > 0 && r.getInt("stage") == 1) {
            String xs = "X ".repeat(r.getInt("strikes")).trim();
            text(g, f, xs, W / 2f, y, 0xFFFF3030, 1.6f, true);
            y += 16;
        }
        if (host && !r.getString("pending").isEmpty()) {
            String s = "Escribio: \"" + r.getString("pending") + "\" - aceptar o rechazar en el panel (Y)";
            g.drawString(f, s, (W - f.width(s)) / 2, y, 0xFFFFD54F, true);
        }
    }

    private static String sideName(ListTag sides, int i) {
        return i >= 0 && i < sides.size() ? sides.getCompound(i).getString("n") : "";
    }

    private static void drawTimer(GuiGraphics g, int x, int y, int w, int left, int total) {
        float k = total <= 0 ? 0 : Math.max(0f, Math.min(1f, (left - (ShowClient.clientTick - ShowClient.stateStamp)) / (float) total));
        g.fill(x - 1, y - 1, x + w + 1, y + 7, 0xFF000000);
        int col = k > 0.5f ? 0xFF43A047 : k > 0.25f ? 0xFFFDD835 : 0xFFE53935;
        g.fill(x, y, x + (int) (w * k), y + 6, col);
    }

    private static void drawTrivia(GuiGraphics g, Font f, CompoundTag st, int W, int H) {
        CompoundTag t = st.getCompound("t");
        boolean rev = t.getBoolean("rev");
        List<String> ops = strs(t.getList("op", 8));
        int bw = Math.min(380, W - 40);
        int x = (W - bw) / 2;
        int y = top;
        int qh = wrappedHeight(f, t.getString("q"), bw - 12) + 8;
        panel(g, x, y, bw, qh, 0xFFFFFFFF);
        wrapped(g, f, t.getString("q"), x + 6, y + 4, bw - 12, 0xFFFFFFFF, true);
        y += qh + 6;
        int ok = t.contains("ok") ? t.getInt("ok") : -1;
        int mine = t.getInt("mine");
        int cw = (bw - 4) / 2;
        for (int i = 0; i < ops.size(); i++) {
            int cx = x + (i % 2) * (cw + 4);
            int cy = y + (i / 2) * 24;
            int c = ops.size() == 2 ? (i == 0 ? 0x43A047 : 0xE53935) : OPT_COLORS[i % 4];
            int fill = argb(c);
            if (rev && ok >= 0 && i != ok) {
                fill = dark(c, 0.3f);
            }
            if (!rev && mine == i) {
                g.fill(cx - 2, cy - 2, cx + cw + 2, cy + 22, 0xFFFFFFFF);
            }
            if (rev && i == ok) {
                g.fill(cx - 2, cy - 2, cx + cw + 2, cy + 22, 0xFFFFFFFF);
            }
            g.fill(cx, cy, cx + cw, cy + 20, fill);
            int tc = c == 0xFDD835 ? 0xFF222222 : 0xFFFFFFFF;
            String label = (char) ('A' + i) + ": " + ops.get(i);
            if (f.width(label) > cw - 8) {
                label = f.plainSubstrByWidth(label, cw - 8);
            }
            g.drawString(f, label, cx + 6, cy + 6, tc, false);
        }
        y += ((ops.size() + 1) / 2) * 24 + 2;
        if (!rev) {
            drawTimer(g, x, y, bw, t.getInt("left"), t.getInt("total"));
            List<String> done = strs(t.getList("done", 8));
            String s = "Ya contestaron: " + done.size() + (done.isEmpty() ? "" : " (" + String.join(", ", done) + ")");
            g.drawString(f, s, x, y + 10, 0xFFCCCCCC, true);
        } else {
            String note = t.getString("note");
            if (!note.isEmpty()) {
                wrapped(g, f, note, x, y + 2, bw, 0xFFFFE082, false);
            }
            List<String> gained = strs(t.getList("gained", 8));
            if (!gained.isEmpty()) {
                StringBuilder sb = new StringBuilder("Puntos: ");
                for (String e : gained) {
                    String[] p = e.split("\\|");
                    sb.append(p[0]).append(" +").append(p.length > 1 ? p[1] : "?").append("   ");
                }
                g.drawString(f, sb.toString(), x, y + 14 + (note.isEmpty() ? 0 : 12), 0xFF81C784, true);
            }
        }
    }

    private static void drawImpostor(GuiGraphics g, Font f, CompoundTag st, int W, int H) {
        CompoundTag im = st.getCompound("imp");
        String stage = im.getString("stage");
        boolean host = st.getCompound("you").getBoolean("host");
        boolean player = st.getCompound("you").getBoolean("player");
        int bw = Math.min(360, W - 40);
        int x = (W - bw) / 2;
        int y = top;
        panel(g, x, y, bw, 40, 0xFFE53935);
        String top;
        int topColor = 0xFFFFFFFF;
        if (!player && !host) {
            top = "Categoria: " + im.getString("cat");
        } else if (im.getBoolean("isImp") && !stage.equals("DONE")) {
            top = "ERES EL IMPOSTOR - categoria: " + im.getString("cat");
            topColor = 0xFFFF5252;
        } else if (im.contains("word")) {
            top = "Palabra secreta: " + im.getString("word") + "  (" + im.getString("cat") + ")";
            topColor = 0xFF69F0AE;
        } else {
            top = "Categoria: " + im.getString("cat");
        }
        g.drawCenteredString(f, top, W / 2, y + 6, topColor);
        if (host && im.contains("imp")) {
            g.drawCenteredString(f, "Impostor: " + im.getString("imp"), W / 2, y + 20, 0xFFFFD54F);
        }
        y += 46;
        String phaseText = switch (stage) {
            case "CLUES" -> "Pista de " + im.getString("turn") + " (" + im.getInt("step") + "/" + im.getInt("total") + ") - escribe tu pista en el chat";
            case "DISCUSS" -> "A discutir: quien es el impostor?";
            case "VOTE" -> "VOTACION (" + im.getInt("votes") + "/" + im.getInt("n") + "): escribe en el chat el numero o nombre de a quien acusas";
            case "GUESS" -> "Descubierto! " + im.getString("acc") + " puede adivinar la palabra escribiendo en el chat";
            default -> im.getBoolean("caught") ? "Descubierto" : "Se salvo";
        };
        int ph = wrappedHeight(f, phaseText, bw - 12) + 8;
        panel(g, x, y, bw, ph, 0xFF3F51B5);
        wrapped(g, f, phaseText, x + 6, y + 4, bw - 12, 0xFFFFFFFF, true);
        y += ph + 6;
        List<String> clues = strs(im.getList("clues", 8));
        int from = Math.max(0, clues.size() - 8);
        for (int i = from; i < clues.size(); i++) {
            String[] p = clues.get(i).split("\\|", 2);
            g.fill(x, y, x + bw, y + 13, 0xB0000000);
            g.drawString(f, p[0] + ": " + (p.length > 1 ? p[1] : ""), x + 4, y + 3, 0xFFFFFFFF, false);
            y += 14;
        }
        if (stage.equals("VOTE")) {
            List<String> order = strs(st.getList("players", 10).isEmpty() ? new ListTag() : names(st));
            for (int i = 0; i < order.size(); i++) {
                g.drawString(f, (i + 1) + ". " + order.get(i), x + 4, y + 2 + i * 11, 0xFFFFEB3B, true);
            }
        }
        if (stage.equals("DONE") && im.contains("imp")) {
            String s = "El impostor era " + im.getString("imp") + " y la palabra: " + im.getString("word");
            g.drawCenteredString(f, s, W / 2, y + 4, 0xFFFFD54F);
        }
    }

    private static ListTag names(CompoundTag st) {
        ListTag out = new ListTag();
        ListTag pl = st.getList("players", 10);
        for (int i = 0; i < pl.size(); i++) {
            out.add(net.minecraft.nbt.StringTag.valueOf(pl.getCompound(i).getString("n")));
        }
        return out;
    }

    private static void drawTaboo(GuiGraphics g, Font f, CompoundTag st, ListTag sides, int W, int H, CompoundTag you) {
        CompoundTag tb = st.getCompound("tb");
        int bw = Math.min(360, W - 40);
        int x = (W - bw) / 2;
        int y = top;
        boolean sees = tb.contains("word");
        panel(g, x, y, bw, sees ? 52 : 30, 0xFF8E24AA);
        if (sees) {
            text(g, f, tb.getString("word").toUpperCase(java.util.Locale.ROOT), W / 2f, y + 6, 0xFFFFFFFF, 2.0f, true);
            String no = "Prohibido: " + String.join(", ", strs(tb.getList("no", 8)));
            g.drawCenteredString(f, no, W / 2, y + 36, 0xFFFF8A80);
        } else {
            g.drawCenteredString(f, tb.getString("desc") + " describe la palabra a " + sideName(sides, tb.getInt("side")) + "...", W / 2, y + 6, 0xFFFFFFFF);
            g.drawCenteredString(f, "Escribe en el chat lo que crees que es", W / 2, y + 18, 0xFFCCCCCC);
        }
        y += sees ? 58 : 36;
        if (!tb.getBoolean("over")) {
            drawTimer(g, x, y, bw, tb.getInt("left"), tb.getInt("total"));
        } else {
            String s = tb.getBoolean("solved") ? tb.getString("solver") + " acerto: " + tb.getString("word") + " (+" + tb.getInt("gained") + ")"
                    : "Se acabo. Era: " + tb.getString("word");
            g.drawCenteredString(f, s, W / 2, y, tb.getBoolean("solved") ? 0xFF69F0AE : 0xFFFF8A80);
        }
        y += 12;
        if (tb.getInt("strikes") > 0) {
            text(g, f, "X ".repeat(tb.getInt("strikes")).trim(), W / 2f, y, 0xFFFF3030, 1.5f, true);
            y += 14;
        }
        for (String l : strs(tb.getList("lines", 8))) {
            g.fill(x, y, x + bw, y + 13, 0xB0000000);
            g.drawString(f, l, x + 4, y + 3, 0xFFFFFFFF, false);
            y += 14;
        }
    }

    private static void drawDraw(GuiGraphics g, Font f, CompoundTag st, ListTag sides, int W, int H) {
        CompoundTag d = st.getCompound("dr");
        boolean over = d.getBoolean("over");
        int bw = Math.min(360, W - 40);
        int x = (W - bw) / 2;
        int y = top;
        panel(g, x, y, bw, 34, 0xFF1E88E5);
        boolean sees = d.contains("word") && !over;
        if (sees) {
            g.drawCenteredString(f, d.getString("drawer") + " dibuja:", W / 2, y + 4, 0xFFB3E5FC);
            text(g, f, d.getString("word").toUpperCase(java.util.Locale.ROOT), W / 2f, y + 16, 0xFFFFFFFF, 1.6f, true);
        } else if (over) {
            text(g, f, d.getString("word").toUpperCase(java.util.Locale.ROOT), W / 2f, y + 9, 0xFFFFE082, 2.0f, true);
        } else {
            g.drawCenteredString(f, d.getString("drawer") + " dibuja... escribe en el chat lo que ves", W / 2, y + 4, 0xFFB3E5FC);
            text(g, f, d.getString("mask"), W / 2f, y + 16, 0xFFFFFFFF, 1.6f, true);
        }
        int top = y + 40;
        int ch = Math.max(60, Math.min(H - top - 94, (int) (bw * 0.75f)));
        int cw = ch * 4 / 3;
        if (cw > W - 30) {
            cw = W - 30;
            ch = cw * 3 / 4;
        }
        DrawCanvas.render(g, (W - cw) / 2, top, cw, ch);
        int by = top + ch + 6;
        if (!over) {
            drawTimer(g, (W - cw) / 2, by, cw, d.getInt("left"), d.getInt("total"));
        }
        ListTag solved = d.getList("solved", 8);
        String sv = solved.isEmpty() ? "Todavia nadie acierta" : "Acertaron: " + String.join(", ", strs(solved));
        g.drawCenteredString(f, sv, W / 2, by + 10, over ? 0xFFFFE082 : 0xFF69F0AE);
        if (d.getBoolean("iDraw") && !over) {
            g.drawCenteredString(f, "J: abrir el lienzo para dibujar", W / 2, by + 22, 0xFFCCCCCC);
        }
    }

    private static void drawFinal(GuiGraphics g, Font f, ListTag sides, int W, int H) {
        int best = Integer.MIN_VALUE;
        for (int i = 0; i < sides.size(); i++) {
            if (!sides.getCompound(i).getList("m", 8).isEmpty()) {
                best = Math.max(best, sides.getCompound(i).getInt("s"));
            }
        }
        StringBuilder w = new StringBuilder();
        for (int i = 0; i < sides.size(); i++) {
            CompoundTag s = sides.getCompound(i);
            if (!s.getList("m", 8).isEmpty() && s.getInt("s") == best) {
                if (w.length() > 0) {
                    w.append(" y ");
                }
                w.append(s.getString("n"));
            }
        }
        text(g, f, "GANA", W / 2f, 60, 0xFFFFD54F, 3f, true);
        text(g, f, w.toString(), W / 2f, 96, 0xFFFFFFFF, 2.2f, true);
    }

    /** El locutor: una burbuja debajo del aviso. Devuelve donde termina (o {@code y} si no hay nada). */
    private static int drawSay(GuiGraphics g, Font f, int W, int y) {
        if (ShowClient.sayText.isEmpty()) {
            return y;
        }
        int shown = Math.min(ShowClient.sayText.length(), ShowClient.sayTick / 2);
        String s = ShowClient.sayText.substring(0, shown);
        int bw = Math.min(440, W - 16);
        int h = wrappedHeight(f, ShowClient.sayText, bw - 16) + 8;
        int x = (W - bw) / 2;
        g.fill(x - 2, y - 2, x + bw + 2, y + h + 2, 0xFF7E57C2);
        g.fill(x, y, x + bw, y + h, 0xF0201030);
        wrapped(g, f, s, x + 8, y + 4, bw - 16, 0xFFFFFFFF, false);
        return y + h + 2;
    }

    private static void drawTitle(GuiGraphics g, Font f, int W, int H) {
        if (ShowClient.title.isEmpty()) {
            return;
        }
        float t = ShowClient.titleTick;
        float grow = Math.min(1f, t / 8f);
        float fade = t > 70 ? Math.max(0f, 1f - (t - 70) / 20f) : 1f;
        int a = (int) (255 * fade);
        int cy = H / 2 - 20;
        int bandH = (int) (46 * grow);
        g.fill(0, cy - bandH / 2, W, cy + bandH / 2, (a * 3 / 4 << 24) | 0x2A0A5A);
        g.fill(0, cy - bandH / 2, W, cy - bandH / 2 + 3, (a << 24) | 0xFFC107);
        g.fill(0, cy + bandH / 2 - 3, W, cy + bandH / 2, (a << 24) | 0xFFC107);
        if (grow >= 1f) {
            float sc = 2.6f + (float) Math.sin(t * 0.3f) * 0.05f;
            text(g, f, ShowClient.title, W / 2f, cy - 9 * sc / 2f * 1.0f + 1, (a << 24) | 0xFFFFFF, sc, true);
            if (!ShowClient.titleSub.isEmpty()) {
                text(g, f, ShowClient.titleSub, W / 2f, cy + 14, (a << 24) | 0xFFD54F, 1.2f, true);
            }
        }
    }

    private static void drawStrike(GuiGraphics g, Font f, int W, int H) {
        if (ShowClient.strikeCount <= 0) {
            return;
        }
        float t = ShowClient.strikeTick;
        float sc = 7f - Math.min(t, 6f) * 0.4f;
        int a = (int) (255 * (t > 30 ? 1f - (t - 30) / 10f : 1f));
        for (int i = 0; i < ShowClient.strikeCount; i++) {
            float cx = W / 2f + (i - (ShowClient.strikeCount - 1) / 2f) * 60f;
            text(g, f, "X", cx, H / 2f - 40, (Math.max(0, a) << 24) | 0xFF2020, sc, true);
        }
    }

    private static void drawPopups(GuiGraphics g, Font f, ListTag sides, int W, int H) {
        int n = 0;
        for (int i = 0; i < sides.size(); i++) {
            if (!sides.getCompound(i).getList("m", 8).isEmpty()) {
                n++;
            }
        }
        if (n == 0) {
            return;
        }
        int bw = scoreChipW(n, W);
        int total = bw * n + 6 * (n - 1);
        for (ShowClient.Popup p : ShowClient.popups) {
            int idx = 0;
            for (int i = 0; i < Math.min(p.side(), sides.size()); i++) {
                if (!sides.getCompound(i).getList("m", 8).isEmpty()) {
                    idx++;
                }
            }
            int age = ShowClient.clientTick - p.born();
            float cx = W - total - 8 + idx * (bw + 6) + bw / 2f;
            int a = Math.max(0, 255 - age * 5);
            text(g, f, p.text(), cx, H - 66 - age * 1.2f, (a << 24) | 0xFFEB3B, 2f, true);
        }
    }

    private static void drawFx(GuiGraphics g, int W, int H) {
        for (ShowClient.Confetti c : ShowClient.confetti) {
            int s = 3 + (int) (Math.abs(Math.sin(c.rot)) * 3);
            g.fill((int) c.x, (int) c.y, (int) c.x + s, (int) c.y + s, c.color);
        }
        if (ShowClient.glitchTick > 0) {
            java.util.Random r = new java.util.Random(ShowClient.clientTick / 2);
            for (int i = 0; i < 10; i++) {
                int y = r.nextInt(H);
                int h = 2 + r.nextInt(12);
                int col = (0x70 << 24) | (r.nextInt(3) == 0 ? 0xFF0040 : r.nextInt(2) == 0 ? 0x00FFE0 : 0xFFFFFF);
                g.fill(r.nextInt(W / 3), y, W - r.nextInt(W / 3), y + h, col);
            }
        }
    }
}
