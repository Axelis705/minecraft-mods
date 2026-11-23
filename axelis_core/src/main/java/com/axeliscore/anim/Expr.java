package com.axeliscore.anim;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Expresiones de las animaciones (un Molang pequeño, el lenguaje que usan Blockbench y Bedrock en los cuadros clave). Ejemplos:
 * {@code "math.sin(query.anim_time * 360) * 10"}, {@code "variable.speed > 0.5 ? 30 : 10"}, {@code "-45 + q.life_time * 2"}.
 * <ul>
 *     <li>Números, + - * /, paréntesis, comparaciones (&lt; &lt;= &gt; &gt;= == !=), && ||, !, y "a ? b : c".</li>
 *     <li>Variables: query.X / q.X (lo que da la animación: anim_time, life_time, ground_speed...) y variable.X / v.X (las que pone el
 *     personaje). Si una no existe vale 0.</li>
 *     <li>math.sin/cos/tan/asin/acos/atan/atan2 (en GRADOS, como en Molang), abs, clamp, lerp, min, max, floor, ceil, round, trunc,
 *     sqrt, pow, exp, ln, mod, sign, pi, random(a,b), hermite_blend(t).</li>
 * </ul>
 * Las expresiones constantes se calculan una sola vez al cargar.
 */
public abstract class Expr {
    /** De dónde saca una expresión el valor de sus variables. */
    public interface Ctx {
        /** Valor de "query.x" o "variable.x" (nombre completo, en minúsculas); 0 si no existe. */
        double get(String name);
    }

    public static final Ctx EMPTY = n -> 0;

    public abstract double eval(Ctx c);

    public boolean isConst() {
        return false;
    }

    public static Expr num(double v) {
        return new Const(v);
    }

    /** Lee una expresión. Si está mal escrita lanza IllegalArgumentException con el motivo. */
    public static Expr parse(String s) {
        Parser p = new Parser(s);
        Expr e = p.ternary();
        p.skip();
        if (p.i < p.s.length()) {
            throw new IllegalArgumentException("sobra texto en '" + s + "' desde la posición " + p.i);
        }
        return e;
    }

    // ------------------------------------------------------------------ nodos

    static final class Const extends Expr {
        final double v;

        Const(double v) {
            this.v = v;
        }

        @Override
        public double eval(Ctx c) {
            return v;
        }

        @Override
        public boolean isConst() {
            return true;
        }
    }

    static final class Var extends Expr {
        final String name;

        Var(String name) {
            this.name = name;
        }

        @Override
        public double eval(Ctx c) {
            return c.get(name);
        }
    }

    interface Op2 {
        double f(double a, double b);
    }

    static final class Bin extends Expr {
        final Expr a, b;
        final Op2 op;

        Bin(Expr a, Expr b, Op2 op) {
            this.a = a;
            this.b = b;
            this.op = op;
        }

        @Override
        public double eval(Ctx c) {
            return op.f(a.eval(c), b.eval(c));
        }
    }

    static final class And extends Expr {
        final Expr a, b;
        final boolean or;

        And(Expr a, Expr b, boolean or) {
            this.a = a;
            this.b = b;
            this.or = or;
        }

        @Override
        public double eval(Ctx c) {
            boolean x = a.eval(c) != 0;
            if (or) {
                return x || b.eval(c) != 0 ? 1 : 0;
            }
            return x && b.eval(c) != 0 ? 1 : 0;
        }
    }

    static final class Cond extends Expr {
        final Expr q, y, n;

        Cond(Expr q, Expr y, Expr n) {
            this.q = q;
            this.y = y;
            this.n = n;
        }

        @Override
        public double eval(Ctx c) {
            return q.eval(c) != 0 ? y.eval(c) : n.eval(c);
        }
    }

    interface Fn {
        double f(double[] a);
    }

    static final class Call extends Expr {
        final Expr[] args;
        final Fn fn;
        final boolean pure;

        Call(Expr[] args, Fn fn, boolean pure) {
            this.args = args;
            this.fn = fn;
            this.pure = pure;
        }

        @Override
        public double eval(Ctx c) {
            double[] v = new double[args.length];
            for (int i = 0; i < v.length; i++) {
                v[i] = args[i].eval(c);
            }
            return fn.f(v);
        }
    }

    /** Si todo lo de dentro es constante, se calcula ya. */
    static Expr fold(Expr e) {
        boolean k;
        if (e instanceof Bin b) {
            k = b.a.isConst() && b.b.isConst();
        } else if (e instanceof And a) {
            k = a.a.isConst() && a.b.isConst();
        } else if (e instanceof Cond c) {
            k = c.q.isConst() && c.y.isConst() && c.n.isConst();
        } else if (e instanceof Call c) {
            k = c.pure;
            for (Expr x : c.args) {
                k &= x.isConst();
            }
        } else {
            k = false;
        }
        return k ? new Const(e.eval(EMPTY)) : e;
    }

    // ------------------------------------------------------------------ lector

    private static final class Parser {
        final String s;
        int i;

        Parser(String s) {
            this.s = s;
        }

        void skip() {
            while (i < s.length() && Character.isWhitespace(s.charAt(i))) {
                i++;
            }
        }

        boolean eat(String t) {
            skip();
            if (s.startsWith(t, i)) {
                i += t.length();
                return true;
            }
            return false;
        }

        IllegalArgumentException err(String m) {
            return new IllegalArgumentException(m + " en '" + s + "' (posición " + i + ")");
        }

        Expr ternary() {
            Expr q = or();
            if (eat("?")) {
                Expr y = ternary();
                if (!eat(":")) {
                    throw err("falta ':'");
                }
                Expr n = ternary();
                return fold(new Cond(q, y, n));
            }
            return q;
        }

        Expr or() {
            Expr a = and();
            while (eat("||")) {
                a = fold(new And(a, and(), true));
            }
            return a;
        }

        Expr and() {
            Expr a = cmp();
            while (eat("&&")) {
                a = fold(new And(a, cmp(), false));
            }
            return a;
        }

        Expr cmp() {
            Expr a = add();
            while (true) {
                if (eat("<=")) {
                    a = fold(new Bin(a, add(), (x, y) -> x <= y ? 1 : 0));
                } else if (eat(">=")) {
                    a = fold(new Bin(a, add(), (x, y) -> x >= y ? 1 : 0));
                } else if (eat("==")) {
                    a = fold(new Bin(a, add(), (x, y) -> x == y ? 1 : 0));
                } else if (eat("!=")) {
                    a = fold(new Bin(a, add(), (x, y) -> x != y ? 1 : 0));
                } else if (eat("<")) {
                    a = fold(new Bin(a, add(), (x, y) -> x < y ? 1 : 0));
                } else if (eat(">")) {
                    a = fold(new Bin(a, add(), (x, y) -> x > y ? 1 : 0));
                } else {
                    return a;
                }
            }
        }

        Expr add() {
            Expr a = mul();
            while (true) {
                if (eat("+")) {
                    a = fold(new Bin(a, mul(), Double::sum));
                } else if (peekMinus()) {
                    i++;
                    a = fold(new Bin(a, mul(), (x, y) -> x - y));
                } else {
                    return a;
                }
            }
        }

        boolean peekMinus() {
            skip();
            return i < s.length() && s.charAt(i) == '-';
        }

        Expr mul() {
            Expr a = unary();
            while (true) {
                if (eat("*")) {
                    a = fold(new Bin(a, unary(), (x, y) -> x * y));
                } else if (eat("/")) {
                    a = fold(new Bin(a, unary(), (x, y) -> y == 0 ? 0 : x / y));
                } else {
                    return a;
                }
            }
        }

        Expr unary() {
            if (eat("-")) {
                return fold(new Bin(num(0), unary(), (x, y) -> x - y));
            }
            if (eat("+")) {
                return unary();
            }
            if (eat("!")) {
                return fold(new Bin(unary(), num(0), (x, y) -> x == 0 ? 1 : 0));
            }
            return primary();
        }

        Expr primary() {
            skip();
            if (i >= s.length()) {
                throw err("falta un valor");
            }
            char ch = s.charAt(i);
            if (ch == '(') {
                i++;
                Expr e = ternary();
                if (!eat(")")) {
                    throw err("falta ')'");
                }
                return e;
            }
            if (Character.isDigit(ch) || ch == '.') {
                int st = i;
                while (i < s.length() && (Character.isDigit(s.charAt(i)) || s.charAt(i) == '.')) {
                    i++;
                }
                if (i < s.length() && (s.charAt(i) == 'f' || s.charAt(i) == 'F')) {
                    i++; // 1.5f
                }
                return num(Double.parseDouble(s.substring(st, i).replace("f", "").replace("F", "")));
            }
            if (Character.isLetter(ch) || ch == '_') {
                int st = i;
                while (i < s.length() && (Character.isLetterOrDigit(s.charAt(i)) || s.charAt(i) == '_' || s.charAt(i) == '.')) {
                    i++;
                }
                String id = s.substring(st, i).toLowerCase(Locale.ROOT);
                if (eat("(")) {
                    List<Expr> args = new ArrayList<>();
                    if (!eat(")")) {
                        do {
                            args.add(ternary());
                        } while (eat(","));
                        if (!eat(")")) {
                            throw err("falta ')' en " + id);
                        }
                    }
                    return call(id, args.toArray(new Expr[0]));
                }
                return name(id);
            }
            throw err("no entiendo '" + ch + "'");
        }

        Expr name(String id) {
            switch (id) {
                case "math.pi":
                    return num(Math.PI);
                case "true":
                    return num(1);
                case "false":
                    return num(0);
                default:
                    break;
            }
            if (id.startsWith("q.")) {
                id = "query." + id.substring(2);
            } else if (id.startsWith("v.")) {
                id = "variable." + id.substring(2);
            } else if (id.startsWith("c.")) {
                id = "context." + id.substring(2);
            }
            return new Var(id);
        }

        Expr call(String id, Expr[] a) {
            String n = id.startsWith("math.") ? id.substring(5) : id;
            final double r = Math.PI / 180;
            Fn f;
            boolean pure = true;
            switch (n) {
                case "sin" -> f = v -> Math.sin(v[0] * r);
                case "cos" -> f = v -> Math.cos(v[0] * r);
                case "tan" -> f = v -> Math.tan(v[0] * r);
                case "asin" -> f = v -> Math.asin(v[0]) / r;
                case "acos" -> f = v -> Math.acos(v[0]) / r;
                case "atan" -> f = v -> Math.atan(v[0]) / r;
                case "atan2" -> f = v -> Math.atan2(v[0], v[1]) / r;
                case "abs" -> f = v -> Math.abs(v[0]);
                case "sign" -> f = v -> Math.signum(v[0]);
                case "floor" -> f = v -> Math.floor(v[0]);
                case "ceil" -> f = v -> Math.ceil(v[0]);
                case "round" -> f = v -> Math.round(v[0]);
                case "trunc" -> f = v -> v[0] < 0 ? Math.ceil(v[0]) : Math.floor(v[0]);
                case "sqrt" -> f = v -> Math.sqrt(Math.max(0, v[0]));
                case "exp" -> f = v -> Math.exp(v[0]);
                case "ln" -> f = v -> v[0] <= 0 ? 0 : Math.log(v[0]);
                case "pow" -> f = v -> Math.pow(v[0], v[1]);
                case "mod" -> f = v -> v[1] == 0 ? 0 : v[0] % v[1];
                case "min" -> f = v -> Math.min(v[0], v[1]);
                case "max" -> f = v -> Math.max(v[0], v[1]);
                case "clamp" -> f = v -> Math.max(v[1], Math.min(v[2], v[0]));
                case "lerp" -> f = v -> v[0] + (v[1] - v[0]) * v[2];
                case "hermite_blend" -> f = v -> 3 * v[0] * v[0] - 2 * v[0] * v[0] * v[0];
                case "random" -> {
                    f = v -> v[0] + Math.random() * (v[1] - v[0]);
                    pure = false;
                }
                default -> throw err("función desconocida " + id);
            }
            int need = switch (n) {
                case "atan2", "pow", "mod", "min", "max", "random" -> 2;
                case "clamp", "lerp" -> 3;
                default -> 1;
            };
            if (a.length != need) {
                throw err(id + " pide " + need + " valores");
            }
            return fold(new Call(a, f, pure));
        }
    }
}
