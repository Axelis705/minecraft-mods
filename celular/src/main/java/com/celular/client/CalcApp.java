package com.celular.client;

import com.celular.plat.client.G;
import net.minecraft.client.gui.Font;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

/** Calculadora (como la de los celulares): botones redondos, también con el teclado (números, + - * /, Enter, Retroceso, Esc = AC). */
final class CalcApp extends App {
    private static final String[][] KEYS = {
            {"AC", "±", "%", "÷"},
            {"7", "8", "9", "×"},
            {"4", "5", "6", "−"},
            {"1", "2", "3", "+"},
            {"0", "", ".", "="}};

    private final Calc calc = new Calc();
    private String pressed = "";
    private long pressedAt;

    CalcApp(PhoneScreen s) {
        super(s, D.APP_CALC, "calc");
    }

    @Override
    int bg() {
        return 0xFF000000;
    }

    @Override
    boolean lightStatus() {
        return false;
    }

    private int cell() {
        return Math.min((s.dw() - 10) / 4, (s.cy1 - s.cy0 - 40) / 5);
    }

    private int keysTop() {
        return s.cy1 - 4 - 5 * cell();
    }

    @Override
    void draw(G g, int mx, int my, float pt) {
        Font f = font();
        g.fill(s.dx0, s.dy0, s.dx1, s.dy1, 0xFF000000);
        int c = cell(), top = keysTop();
        int x0 = (s.dx0 + s.dx1) / 2 - c * 2;
        // pantalla: el número (se achica si no cabe)
        String shown = calc.display();
        float sc = 2.6F;
        int maxW = s.dw() - 12;
        while (sc > 1 && f.width(shown) * sc > maxW) {
            sc -= 0.1F;
        }
        g.push();
        g.translate(s.dx1 - 6 - f.width(shown) * sc, top - 10 - 8 * sc, 0);
        g.scale(sc, sc, 1);
        g.drawString(f, shown, 0, 0, 0xFFFFFFFF, false);
        g.pop();
        if (!calc.pending().isEmpty()) {
            g.push();
            g.translate(s.dx1 - 6 - f.width(calc.pending()) * 0.75F, top - 14 - 8 * sc - 10, 0);
            g.scale(0.75F, 0.75F, 1);
            g.drawString(f, calc.pending(), 0, 0, 0xFF8E8E93, false);
            g.pop();
        }
        long now = System.currentTimeMillis();
        for (int r = 0; r < 5; r++) {
            for (int k = 0; k < 4; k++) {
                String key = KEYS[r][k];
                if (key.isEmpty()) {
                    continue;
                }
                int bx = x0 + k * c, by = top + r * c, bw = c - 4;
                if (key.equals("0")) {
                    bw = 2 * c - 4;
                }
                boolean op = k == 3, fn = r == 0 && k < 3;
                int col = op ? 0xFFFF9F0A : fn ? 0xFFA5A5A5 : 0xFF333333;
                if (op && calc.isActiveOp(key)) {
                    col = 0xFFFFFFFF;
                }
                boolean h = s.in(mx, my, bx, by, bx + bw, by + c - 4);
                if (h) {
                    col = D.mix(col, 0xFFFFFFFF, 0.18F);
                }
                if (key.equals(pressed) && now - pressedAt < 120) {
                    col = D.mix(col, 0xFFFFFFFF, 0.45F);
                }
                D.rr(g, bx, by, bx + bw, by + c - 4, (c - 4) / 2, col);
                int tc = fn ? 0xFF000000 : (op && calc.isActiveOp(key)) ? 0xFFFF9F0A : 0xFFFFFFFF;
                String label = key.equals("AC") && calc.canClearEntry() ? "C" : key;
                int lx = key.equals("0") ? bx + (c - 4) / 2 - f.width(label) / 2 : bx + bw / 2 - f.width(label) / 2;
                g.drawString(f, label, lx, by + (c - 4) / 2 - 4, tc, false);
            }
        }
    }

    @Override
    boolean click(double x, double y, int b) {
        int c = cell(), top = keysTop();
        int x0 = (s.dx0 + s.dx1) / 2 - c * 2;
        for (int r = 0; r < 5; r++) {
            for (int k = 0; k < 4; k++) {
                String key = KEYS[r][k];
                if (key.isEmpty()) {
                    continue;
                }
                int bx = x0 + k * c, by = top + r * c, bw = key.equals("0") ? 2 * c - 4 : c - 4;
                if (s.in(x, y, bx, by, bx + bw, by + c - 4)) {
                    press(key);
                    return true;
                }
            }
        }
        return true;
    }

    private void press(String key) {
        pressed = key;
        pressedAt = System.currentTimeMillis();
        ClientHooks.tap();
        calc.press(key);
    }

    @Override
    boolean key(int key) {
        String k = switch (key) {
            case 48, 320 -> "0";
            case 49, 321 -> "1";
            case 50, 322 -> "2";
            case 51, 323 -> "3";
            case 52, 324 -> "4";
            case 53, 325 -> "5";
            case 54, 326 -> "6";
            case 55, 327 -> "7";
            case 56, 328 -> "8";
            case 57, 329 -> "9";
            case 46, 44, 330 -> ".";
            case 334, 61 -> "+";
            case 333, 45 -> "−";
            case 332 -> "×";
            case 331, 47 -> "÷";
            case 257, 335 -> "=";
            case 259 -> "⌫";
            default -> null;
        };
        if (k == null) {
            return false;
        }
        if (k.equals("⌫")) {
            calc.backspace();
            return true;
        }
        press(k);
        return true;
    }

    /** La lógica de la calculadora (sin pantalla). */
    static final class Calc {
        private String entry = "0";
        private BigDecimal acc;
        private String op = "";
        private boolean fresh = true;
        private boolean error;
        private String last = "";

        String display() {
            if (error) {
                return "Error";
            }
            return entry;
        }

        String pending() {
            return acc != null && !op.isEmpty() ? fmt(acc) + " " + op : last;
        }

        boolean isActiveOp(String k) {
            return fresh && k.equals(op);
        }

        boolean canClearEntry() {
            return !entry.equals("0") && !fresh;
        }

        void backspace() {
            if (fresh || error) {
                return;
            }
            entry = entry.length() <= 1 || (entry.length() == 2 && entry.startsWith("-")) ? "0" : entry.substring(0, entry.length() - 1);
        }

        void press(String k) {
            if (error && !k.equals("AC")) {
                return;
            }
            switch (k) {
                case "AC" -> {
                    if (canClearEntry()) {
                        entry = "0";
                        fresh = true;
                    } else {
                        entry = "0";
                        acc = null;
                        op = "";
                        last = "";
                        error = false;
                        fresh = true;
                    }
                }
                case "±" -> entry = entry.startsWith("-") ? entry.substring(1) : entry.equals("0") ? "0" : "-" + entry;
                case "%" -> {
                    entry = fmt(num(entry).divide(BigDecimal.valueOf(100), MathContext.DECIMAL64));
                    fresh = true;
                }
                case "." -> {
                    if (fresh) {
                        entry = "0.";
                        fresh = false;
                    } else if (!entry.contains(".") && entry.length() < 14) {
                        entry += ".";
                    }
                }
                case "+", "−", "×", "÷" -> {
                    if (acc != null && !op.isEmpty() && !fresh) {
                        compute();
                    } else if (acc == null || !fresh) {
                        acc = num(entry);
                    }
                    if (!error) {
                        acc = num(entry);
                        op = k;
                        fresh = true;
                    }
                }
                case "=" -> {
                    if (acc != null && !op.isEmpty()) {
                        last = fmt(acc) + " " + op + " " + entry + " =";
                        compute();
                        op = "";
                        acc = null;
                        fresh = true;
                    }
                }
                default -> {
                    if (fresh) {
                        entry = k;
                        fresh = false;
                    } else if (entry.replace("-", "").replace(".", "").length() < 14) {
                        entry = entry.equals("0") ? k : entry.equals("-0") ? "-" + k : entry + k;
                    }
                }
            }
        }

        private void compute() {
            BigDecimal a = acc, b = num(entry), r;
            try {
                r = switch (op) {
                    case "+" -> a.add(b);
                    case "−" -> a.subtract(b);
                    case "×" -> a.multiply(b);
                    case "÷" -> a.divide(b, MathContext.DECIMAL64);
                    default -> b;
                };
                entry = fmt(r);
                acc = r;
            } catch (ArithmeticException e) {
                error = true;
                entry = "0";
            }
        }

        private static BigDecimal num(String s) {
            try {
                return new BigDecimal(s);
            } catch (NumberFormatException e) {
                return BigDecimal.ZERO;
            }
        }

        static String fmt(BigDecimal v) {
            BigDecimal r = v.round(new MathContext(12, RoundingMode.HALF_UP)).stripTrailingZeros();
            if (r.abs().compareTo(new BigDecimal("1e12")) >= 0 || (r.signum() != 0 && r.abs().compareTo(new BigDecimal("1e-9")) < 0)) {
                return r.round(new MathContext(6)).toString().replace("E+", "e");
            }
            String p = r.toPlainString();
            return p.equals("-0") ? "0" : p;
        }
    }
}
