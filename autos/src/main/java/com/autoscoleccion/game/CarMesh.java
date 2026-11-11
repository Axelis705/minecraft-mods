package com.autoscoleccion.game;

import com.autoscoleccion.game.CarStyle.Shape;

import static com.autoscoleccion.game.CarStyle.*;

/**
 * Geometría de un cochecito a partir de su {@link CarStyle} (Java puro: la usan el cliente para dibujar y la herramienta de vista
 * previa). Carrocería con morro y cola inclinados, cabina de vidrio con techo y pilares, ruedas de ocho lados con rin, faros que
 * brillan y los extras. Medidas en píxeles (1/16 de bloque); el auto queda apoyado en y = 0, centrado y mirando a +X.
 */
public final class CarMesh {
    public static final int MODE_NORMAL = 0;
    /** Silueta negra para los autos que todavía no tienes. */
    public static final int MODE_SILHOUETTE = 1;

    /** Quien recibe las caras (cuatro esquinas en orden, color y si brilla). */
    public interface Sink {
        void quad(float[] a, float[] b, float[] c, float[] d, int rgb, boolean emissive);
    }

    private CarMesh() {
    }

    private static final class Painter {
        final Sink sink;
        final int mode;

        Painter(Sink sink, int mode) {
            this.sink = sink;
            this.mode = mode;
        }

        void quad(float[] a, float[] b, float[] c, float[] d, int rgb, boolean emissive) {
            sink.quad(a, b, c, d, mode == MODE_SILHOUETTE ? 0x0A0A10 : rgb, emissive && mode == MODE_NORMAL);
        }

        /**
         * Prisma de 8 esquinas: abajo (y0) el rectángulo x0..x1 / z0..z1 y arriba (y1) el rectángulo tx0..tx1 / tz0..tz1. Sirve para
         * cajas normales y para cuñas, parabrisas inclinados y cabinas que se estrechan.
         */
        void hexa(float x0, float x1, float z0, float z1, float y0, float tx0, float tx1, float tz0, float tz1, float y1, int rgb, boolean em) {
            float[] b00 = {x0, y0, z0};
            float[] b10 = {x1, y0, z0};
            float[] b11 = {x1, y0, z1};
            float[] b01 = {x0, y0, z1};
            float[] t00 = {tx0, y1, tz0};
            float[] t10 = {tx1, y1, tz0};
            float[] t11 = {tx1, y1, tz1};
            float[] t01 = {tx0, y1, tz1};
            quad(t00, t01, t11, t10, rgb, em);   // arriba
            quad(b00, b10, b11, b01, rgb, em);   // abajo
            quad(b10, t10, t11, b11, rgb, em);   // frente (+x)
            quad(b01, t01, t00, b00, rgb, em);   // atrás (-x)
            quad(b00, t00, t10, b10, rgb, em);   // lado -z
            quad(b11, t11, t01, b01, rgb, em);   // lado +z
        }

        void box(float x0, float x1, float y0, float y1, float z0, float z1, int rgb, boolean em) {
            hexa(x0, x1, z0, z1, y0, x0, x1, z0, z1, y1, rgb, em);
        }

        /** Prisma de 8 lados acostado sobre el eje Z (para ruedas y escapes). */
        void cylZ(float cx, float cy, float z0, float z1, float r, int rgb, boolean em) {
            int n = 8;
            float off = (float) (Math.PI / n);
            for (int i = 0; i < n; i++) {
                double a0 = off + i * 2 * Math.PI / n;
                double a1 = off + (i + 1) * 2 * Math.PI / n;
                float x0 = cx + (float) Math.cos(a0) * r;
                float y0 = cy + (float) Math.sin(a0) * r;
                float x1 = cx + (float) Math.cos(a1) * r;
                float y1 = cy + (float) Math.sin(a1) * r;
                quad(new float[] {x0, y0, z0}, new float[] {x1, y1, z0}, new float[] {x1, y1, z1}, new float[] {x0, y0, z1}, rgb, em);
                quad(new float[] {cx, cy, z1}, new float[] {x0, y0, z1}, new float[] {x1, y1, z1}, new float[] {cx, cy, z1}, rgb, em);
                quad(new float[] {cx, cy, z0}, new float[] {x1, y1, z0}, new float[] {x0, y0, z0}, new float[] {cx, cy, z0}, rgb, em);
            }
        }

        /** Prisma de 8 lados acostado sobre el eje X (escapes). */
        void cylX(float x0, float x1, float cy, float cz, float r, int rgb, boolean em) {
            int n = 8;
            for (int i = 0; i < n; i++) {
                double a0 = i * 2 * Math.PI / n;
                double a1 = (i + 1) * 2 * Math.PI / n;
                float y0 = cy + (float) Math.cos(a0) * r;
                float z0 = cz + (float) Math.sin(a0) * r;
                float y1 = cy + (float) Math.cos(a1) * r;
                float z1 = cz + (float) Math.sin(a1) * r;
                quad(new float[] {x0, y0, z0}, new float[] {x0, y1, z1}, new float[] {x1, y1, z1}, new float[] {x1, y0, z0}, rgb, em);
            }
        }
    }

    private static int shade(int rgb, float k) {
        int r = Math.min(255, Math.max(0, Math.round(((rgb >> 16) & 255) * k)));
        int g = Math.min(255, Math.max(0, Math.round(((rgb >> 8) & 255) * k)));
        int b = Math.min(255, Math.max(0, Math.round((rgb & 255) * k)));
        return (r << 16) | (g << 8) | b;
    }

    private static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    /**
     * Genera el auto. {@code time} (en ticks, con decimales) solo anima lo que parpadea (torreta, destellos del oro).
     */
    public static void build(CarStyle s, Sink sink, float time, int mode) {
        Painter p = new Painter(sink, mode);
        boolean formula = s.shape == Shape.FORMULA;
        float hx = s.len / 2.0F;
        float hz = s.wid / 2.0F;
        float y0 = s.ride;
        float y1 = s.ride + s.bodyH;
        boolean chrome = s.has(CHROME);
        int trim = chrome ? 0xD8DEE6 : s.accent;
        int dark = 0x18191D;
        int upper = s.has(TWO_TONE) ? s.accent : s.body;
        boolean gold = s.has(GOLD_SPARKLE);

        // ---------------------------------------------------------------- carrocería baja
        float topX0 = -hx + s.tailSlope;
        float topX1 = hx - s.noseSlope;
        float in = 0.15F;
        if (s.shape == Shape.VAN && s.has(TWO_TONE)) {
            // la combi: mitad de abajo de un color y de arriba de otro
            p.hexa(-hx, hx, -hz, hz, y0, -hx, hx, -hz, hz, y0 + s.bodyH * 0.55F, s.body, false);
            p.hexa(-hx, hx, -hz, hz, y0 + s.bodyH * 0.55F, topX0, topX1, -hz + in, hz - in, y1, upper, false);
        } else {
            p.hexa(-hx, hx, -hz, hz, y0, topX0, topX1, -hz + in, hz - in, y1, s.body, gold);
        }
        // bajos oscuros (para que no se vea "flotando") y defensas
        p.box(-hx + 0.3F, hx - 0.3F, y0 - 0.25F, y0 + 0.02F, -hz + 0.35F, hz - 0.35F, dark, false);
        if (!formula) {
            p.box(hx - 0.25F, hx + 0.15F, y0 - 0.05F, y0 + 0.55F, -hz + 0.2F, hz - 0.2F, trim, false);
            p.box(-hx - 0.15F, -hx + 0.25F, y0 - 0.05F, y0 + 0.55F, -hz + 0.2F, hz - 0.2F, trim, false);
            // parrilla
            float gy = y0 + s.bodyH * 0.25F;
            p.box(hx - 0.1F, hx + 0.08F, gy, gy + s.bodyH * 0.22F, -hz * 0.45F, hz * 0.45F, shade(trim, 0.55F), false);
        }

        // ---------------------------------------------------------------- caja de la pickup
        if (s.shape == Shape.PICKUP) {
            float bx1 = s.cabRear - 0.3F;
            p.box(-hx + 0.5F, bx1, y1 - 0.02F, y1 + 0.03F, -hz + 0.5F, hz - 0.5F, shade(s.body, 0.35F), false);
            p.box(-hx + 0.1F, bx1, y1, y1 + 0.9F, -hz + 0.12F, -hz + 0.5F, s.body, false);
            p.box(-hx + 0.1F, bx1, y1, y1 + 0.9F, hz - 0.5F, hz - 0.12F, s.body, false);
            p.box(-hx + 0.05F, -hx + 0.5F, y1, y1 + 0.9F, -hz + 0.12F, hz - 0.12F, s.body, false);
        }

        // ---------------------------------------------------------------- cabina
        if (s.roofH > 0.01F && !formula) {
            float cz0 = -hz + 0.45F;
            float cz1 = hz - 0.45F;
            float rx0 = s.cabRear + s.rearInset;
            float rx1 = s.cabFront - s.wsInset;
            float rz0 = cz0 + s.sideInset;
            float rz1 = cz1 - s.sideInset;
            float yt = y1 + s.roofH;
            // la cabina se interpola entre la base y el techo: cada "rebanada" toma su rectángulo a esa altura
            float belt = 0.22F;
            float cap = 0.82F;
            float[] r0 = slice(s.cabRear, s.cabFront, cz0, cz1, rx0, rx1, rz0, rz1, 0.0F);
            float[] rb = slice(s.cabRear, s.cabFront, cz0, cz1, rx0, rx1, rz0, rz1, belt);
            float[] rc = slice(s.cabRear, s.cabFront, cz0, cz1, rx0, rx1, rz0, rz1, cap);
            float[] rt = slice(s.cabRear, s.cabFront, cz0, cz1, rx0, rx1, rz0, rz1, 1.0F);
            float yb = lerp(y1, yt, belt);
            float yc = lerp(y1, yt, cap);
            p.hexa(r0[0], r0[1], r0[2], r0[3], y1 - 0.02F, rb[0], rb[1], rb[2], rb[3], yb, upper, false);
            p.hexa(rb[0] + 0.05F, rb[1] - 0.05F, rb[2] + 0.05F, rb[3] - 0.05F, yb, rc[0] + 0.05F, rc[1] - 0.05F, rc[2] + 0.05F, rc[3] - 0.05F, yc,
                    s.glass, false);
            p.hexa(rc[0], rc[1], rc[2], rc[3], yc, rt[0], rt[1], rt[2], rt[3], yt, s.has(CANOPY) ? s.glass : upper, false);
            if (!s.has(CANOPY)) {
                // pilar central (se ve en los costados)
                float mx = (rb[0] + rb[1]) / 2.0F;
                float half = s.shape == Shape.VAN ? 0.35F : 0.25F;
                p.hexa(mx - half, mx + half, rb[2] - 0.02F, rb[3] + 0.02F, yb, mx - half, mx + half, rc[2] - 0.02F, rc[3] + 0.02F, yc, upper, false);
                if (s.shape == Shape.VAN) {
                    float mx2 = (mx + rb[0]) / 2.0F;
                    p.hexa(mx2 - half, mx2 + half, rb[2] - 0.02F, rb[3] + 0.02F, yb, mx2 - half, mx2 + half, rc[2] - 0.02F, rc[3] + 0.02F, yc, upper, false);
                }
            }
            roofExtras(p, s, rt, yt, time);
        }

        // ---------------------------------------------------------------- monoplaza: alerones y piloto
        if (formula) {
            float ww = hz + s.track + s.wheelW * 0.7F;
            p.box(hx - 1.0F, hx + 0.2F, y0 - 0.25F, y0 + 0.05F, -ww, ww, s.accent, false);
            p.box(hx - 0.9F, hx + 0.1F, y0 - 0.25F, y0 + 0.3F, -ww - 0.1F, -ww, s.accent, false);
            p.box(hx - 0.9F, hx + 0.1F, y0 - 0.25F, y0 + 0.3F, ww, ww + 0.1F, s.accent, false);
            // pontones a los lados
            p.hexa(-2.8F, 1.2F, -hz - 1.1F, hz + 1.1F, y0, -2.4F, 0.4F, -hz - 0.9F, hz + 0.9F, y0 + 1.1F, s.body, false);
            // cabina abierta y piloto
            p.box(s.cabRear, s.cabFront, y1 - 0.05F, y1 + 0.02F, -0.9F, 0.9F, dark, false);
            p.box(-0.6F, 0.6F, y1, y1 + 1.15F, -0.6F, 0.6F, s.accent, false);
            p.box(0.35F, 0.65F, y1 + 0.35F, y1 + 0.8F, -0.45F, 0.45F, 0x101418, false);
            // toma de aire sobre el piloto y alerón trasero
            p.hexa(-2.6F, -0.8F, -0.55F, 0.55F, y1, -2.6F, -1.4F, -0.45F, 0.45F, y1 + 1.4F, s.body, false);
            float wy = y1 + 1.6F;
            p.box(-hx - 0.3F, -hx + 1.3F, wy, wy + 0.3F, -hz - 1.2F, hz + 1.2F, s.accent, false);
            p.box(-hx - 0.3F, -hx + 1.3F, wy - 1.8F, wy + 0.4F, -hz - 1.35F, -hz - 1.2F, s.accent, false);
            p.box(-hx - 0.3F, -hx + 1.3F, wy - 1.8F, wy + 0.4F, hz + 1.2F, hz + 1.35F, s.accent, false);
        }

        // ---------------------------------------------------------------- luces
        if (!formula) {
            float ly = y0 + s.bodyH * 0.5F;
            float lx = hx - s.noseSlope * 0.5F;
            int head = s.shape == Shape.CLASSIC || s.shape == Shape.LOWRIDER ? 0xFFF0C0 : 0xF4FAFF;
            p.box(lx - 0.25F, lx + 0.12F, ly, ly + 0.45F, -hz + 0.35F, -hz + 1.5F, head, true);
            p.box(lx - 0.25F, lx + 0.12F, ly, ly + 0.45F, hz - 1.5F, hz - 0.35F, head, true);
            float tx = -hx + s.tailSlope * 0.5F;
            p.box(tx - 0.12F, tx + 0.2F, ly, ly + 0.4F, -hz + 0.35F, -hz + 1.5F, 0xFF2A2A, true);
            p.box(tx - 0.12F, tx + 0.2F, ly, ly + 0.4F, hz - 1.5F, hz - 0.35F, 0xFF2A2A, true);
            // retrovisores
            if (s.roofH > 0.01F) {
                float mx = s.cabFront - 0.3F;
                p.box(mx - 0.4F, mx, y1 + 0.2F, y1 + 0.6F, -hz - 0.35F, -hz + 0.1F, upper, false);
                p.box(mx - 0.4F, mx, y1 + 0.2F, y1 + 0.6F, hz - 0.1F, hz + 0.35F, upper, false);
            }
        }

        // ---------------------------------------------------------------- ruedas
        float wz = hz - s.wheelW / 2.0F + s.track + 0.05F;
        for (float wx : new float[] {s.wheelFront, s.wheelRear}) {
            for (int side = -1; side <= 1; side += 2) {
                float zc = side * wz;
                float za = zc - s.wheelW / 2.0F;
                float zb = zc + s.wheelW / 2.0F;
                p.cylZ(wx, s.wheelR, za, zb, s.wheelR, 0x16171A, false);
                float out = side > 0 ? zb : za;
                float rz0 = side > 0 ? out - 0.05F : out - 0.12F;
                float rz1 = side > 0 ? out + 0.12F : out + 0.05F;
                p.cylZ(wx, s.wheelR, rz0, rz1, s.wheelR * 0.62F, s.rim, gold);
                p.cylZ(wx, s.wheelR, side > 0 ? rz1 - 0.02F : rz0 - 0.08F, side > 0 ? rz1 + 0.08F : rz0 + 0.02F, s.wheelR * 0.22F, shade(s.rim, 0.6F), false);
                if (s.has(MUDFLAPS)) {
                    p.box(wx - s.wheelR - 0.35F, wx - s.wheelR - 0.2F, y0 - 0.9F, y0 + 0.4F, za, zb, dark, false);
                }
            }
        }

        // ---------------------------------------------------------------- extras de la carrocería
        if (s.has(STRIPES)) {
            for (int side = -1; side <= 1; side += 2) {
                float z0 = side * 0.25F - (side < 0 ? 0.55F : 0.0F);
                p.hexa(-hx + 0.05F, hx - 0.05F, z0, z0 + 0.55F, y0 + 0.02F, topX0 - 0.01F, topX1 + 0.01F, z0, z0 + 0.55F, y1 + 0.03F, s.accent, false);
            }
        }
        if (s.has(SIDE_STRIPE)) {
            float sy = y0 + s.bodyH * 0.38F;
            float sh = s.bodyH * 0.16F;
            float zIn = in * (sy - y0) / s.bodyH;
            p.box(-hx + 0.8F, hx - 0.9F, sy, sy + sh, -hz + zIn - 0.04F, -hz + zIn + 0.05F, s.accent, s.has(NEON));
            p.box(-hx + 0.8F, hx - 0.9F, sy, sy + sh, hz - zIn - 0.05F, hz - zIn + 0.04F, s.accent, s.has(NEON));
        }
        if (s.has(NUMBER_DISC)) {
            float dx = (s.cabFront + s.cabRear) / 2.0F;
            float dy = y0 + s.bodyH * 0.55F;
            for (int side = -1; side <= 1; side += 2) {
                float zf = side * (hz - in * 0.5F);
                p.box(dx - 0.9F, dx + 0.9F, dy - 0.75F, dy + 0.75F, zf - 0.06F, zf + 0.06F, 0xF8F8F8, false);
                p.box(dx - 0.3F, dx + 0.3F, dy - 0.5F, dy + 0.5F, zf - (side < 0 ? 0.1F : 0.0F), zf + (side > 0 ? 0.1F : 0.0F), s.accent, false);
            }
        }
        if (s.has(HOOD_SCOOP)) {
            float hx0 = s.cabFront + 0.4F;
            float hx1 = Math.min(topX1 - 0.2F, hx0 + 2.4F);
            p.hexa(hx0, hx1, -0.9F, 0.9F, y1 - 0.02F, hx0, hx1 - 0.6F, -0.75F, 0.75F, y1 + 0.55F, s.accent, false);
            p.box(hx1 - 0.7F, hx1 - 0.55F, y1 + 0.05F, y1 + 0.45F, -0.6F, 0.6F, 0x111111, false);
        }
        if (s.has(SPOILER) || s.has(BIG_WING)) {
            boolean big = s.has(BIG_WING) && !formula;
            if (!formula) {
                float h = big ? 1.5F : 0.6F;
                float wx0 = -hx + 0.2F;
                float wx1 = wx0 + (big ? 1.6F : 1.2F);
                float wy = y1 + h;
                p.box(wx0, wx1, wy, wy + 0.25F, -hz + 0.2F, hz - 0.2F, s.accent, false);
                p.box(wx0 + 0.3F, wx0 + 0.6F, y1 - 0.05F, wy, -hz + 1.2F, -hz + 1.45F, s.accent, false);
                p.box(wx0 + 0.3F, wx0 + 0.6F, y1 - 0.05F, wy, hz - 1.45F, hz - 1.2F, s.accent, false);
                if (big) {
                    p.box(wx0 - 0.05F, wx1 + 0.05F, wy - 0.5F, wy + 0.5F, -hz + 0.05F, -hz + 0.2F, s.accent, false);
                    p.box(wx0 - 0.05F, wx1 + 0.05F, wy - 0.5F, wy + 0.5F, hz - 0.2F, hz - 0.05F, s.accent, false);
                }
            }
        }
        if (s.has(FIN)) {
            float fx1 = Math.min(s.cabRear + s.rearInset, -0.5F);
            p.hexa(-hx + 0.4F, fx1, -0.12F, 0.12F, y1 - 0.05F, -hx + 0.4F, -hx + 1.3F, -0.1F, 0.1F, y1 + 1.4F, s.has(TWO_TONE) ? s.accent : s.body, false);
        }
        if (s.has(EXHAUST)) {
            for (int side = -1; side <= 1; side += 2) {
                p.cylX(-hx - 0.5F, -hx + 0.4F, y0 + 0.35F, side * 1.2F, 0.32F, 0xBFC6CE, false);
                p.cylX(-hx - 0.55F, -hx - 0.45F, y0 + 0.35F, side * 1.2F, 0.18F, 0xFF7A2A, true);
            }
        }
        if (s.has(BULLBAR)) {
            p.box(hx + 0.15F, hx + 0.45F, y0 + 0.1F, y0 + s.bodyH * 0.8F, -hz + 0.6F, -hz + 0.9F, 0x3B3E44, false);
            p.box(hx + 0.15F, hx + 0.45F, y0 + 0.1F, y0 + s.bodyH * 0.8F, hz - 0.9F, hz - 0.6F, 0x3B3E44, false);
            p.box(hx + 0.15F, hx + 0.45F, y0 + s.bodyH * 0.55F, y0 + s.bodyH * 0.8F, -hz + 0.6F, hz - 0.6F, 0x3B3E44, false);
        }
        if (chrome) {
            float cy = y0 + s.bodyH * 0.62F;
            float zIn = in * (cy - y0) / s.bodyH;
            p.box(-hx + 0.6F, hx - 0.6F, cy, cy + 0.12F, -hz + zIn - 0.05F, -hz + zIn + 0.03F, 0xEEF2F6, false);
            p.box(-hx + 0.6F, hx - 0.6F, cy, cy + 0.12F, hz - zIn - 0.03F, hz - zIn + 0.05F, 0xEEF2F6, false);
        }
        if (s.has(NEON) && mode == MODE_NORMAL) {
            float pulse = 0.85F + 0.15F * (float) Math.sin(time * 0.25F);
            p.box(-hx + 1.0F, hx - 1.0F, 0.02F, 0.08F, -hz + 0.3F, hz - 0.3F, shade(s.neon, pulse), true);
            p.box(-hx + 1.5F, hx - 1.5F, y0 - 0.3F, y0 - 0.22F, -hz + 0.4F, hz - 0.4F, s.neon, true);
        }
        if (gold && mode == MODE_NORMAL) {
            // destellos que aparecen y desaparecen sobre la carrocería
            for (int i = 0; i < 6; i++) {
                float ph = (time * 0.09F + i * 0.37F) % 1.0F;
                if (ph > 0.35F) {
                    continue;
                }
                float k = 1.0F - Math.abs(ph - 0.17F) / 0.17F;
                float sx = lerp(-hx + 1, hx - 1, (i * 0.618F) % 1.0F);
                float sz = (i % 2 == 0 ? -1 : 1) * (hz - 0.1F);
                float sy = y0 + s.bodyH * (0.3F + 0.5F * ((i * 0.381F) % 1.0F));
                float sz2 = 0.12F + 0.25F * k;
                p.box(sx - sz2, sx + sz2, sy - 0.05F, sy + 0.05F, sz - 0.05F, sz + 0.05F, 0xFFFFF0, true);
                p.box(sx - 0.05F, sx + 0.05F, sy - sz2, sy + sz2, sz - 0.05F, sz + 0.05F, 0xFFFFF0, true);
            }
        }
    }

    /** Rectángulo de la cabina a una altura relativa {@code t} (0 = base, 1 = techo): {x0, x1, z0, z1}. */
    private static float[] slice(float bx0, float bx1, float bz0, float bz1, float tx0, float tx1, float tz0, float tz1, float t) {
        return new float[] {lerp(bx0, tx0, t), lerp(bx1, tx1, t), lerp(bz0, tz0, t), lerp(bz1, tz1, t)};
    }

    /** Lo que va sobre el techo: torreta, letrero, faros extra, parrilla, tabla de surf, cono de helado. */
    private static void roofExtras(Painter p, CarStyle s, float[] rt, float yt, float time) {
        float cx = (rt[0] + rt[1]) / 2.0F;
        if (s.has(SIREN)) {
            boolean on = ((int) (time / 4.0F)) % 2 == 0;
            p.box(cx - 0.6F, cx + 0.6F, yt, yt + 0.2F, -2.0F, 2.0F, 0x222428, false);
            p.box(cx - 0.45F, cx + 0.45F, yt + 0.2F, yt + 0.6F, -1.9F, -0.1F, on ? 0xFF2030 : 0x5A1015, on);
            p.box(cx - 0.45F, cx + 0.45F, yt + 0.2F, yt + 0.6F, 0.1F, 1.9F, on ? 0x1B2E5A : 0x2A6BFF, !on);
        }
        if (s.has(TAXI_SIGN)) {
            p.hexa(cx - 0.8F, cx + 0.8F, -1.1F, 1.1F, yt, cx - 0.6F, cx + 0.6F, -1.0F, 1.0F, yt + 0.8F, 0xFFF4B0, true);
            p.box(cx - 0.82F, cx + 0.82F, yt, yt + 0.12F, -1.12F, 1.12F, 0x1E1E1E, false);
        }
        if (s.has(ROOF_LIGHTS)) {
            float fx = rt[1] - 0.3F;
            p.box(fx - 0.25F, fx + 0.25F, yt, yt + 0.2F, rt[2] + 0.2F, rt[3] - 0.2F, 0x1A1A1A, false);
            for (int i = 0; i < 4; i++) {
                float z = lerp(rt[2] + 0.5F, rt[3] - 0.5F, i / 3.0F);
                p.box(fx + 0.1F, fx + 0.35F, yt + 0.15F, yt + 0.65F, z - 0.3F, z + 0.3F, 0xFFF8DC, true);
            }
        }
        if (s.has(ROOF_RACK)) {
            p.box(rt[0] + 0.2F, rt[1] - 0.2F, yt, yt + 0.35F, rt[2] + 0.2F, rt[2] + 0.4F, 0x2A2C30, false);
            p.box(rt[0] + 0.2F, rt[1] - 0.2F, yt, yt + 0.35F, rt[3] - 0.4F, rt[3] - 0.2F, 0x2A2C30, false);
        }
        if (s.has(SURFBOARD)) {
            p.hexa(rt[0] - 1.2F, rt[1] + 1.0F, -0.8F, 0.8F, yt + 0.35F, rt[0] - 1.0F, rt[1] + 0.8F, -0.7F, 0.7F, yt + 0.6F, 0x3FC1C9, false);
            p.box(rt[0] - 1.0F, rt[1] + 0.8F, yt + 0.6F, yt + 0.62F, -0.12F, 0.12F, 0xFFFFFF, false);
        }
        if (s.has(ICE_CREAM)) {
            // cono y bola de helado gigantes sobre el techo
            p.hexa(cx - 0.3F, cx + 0.3F, -0.3F, 0.3F, yt, cx - 1.1F, cx + 1.1F, -1.1F, 1.1F, yt + 2.2F, 0xD9A066, false);
            p.box(cx - 1.3F, cx + 1.3F, yt + 2.1F, yt + 3.5F, -1.3F, 1.3F, 0xFF8FC0, false);
            p.box(cx - 1.0F, cx + 1.0F, yt + 3.5F, yt + 4.0F, -1.0F, 1.0F, 0xFF8FC0, false);
            p.box(cx - 0.2F, cx + 0.2F, yt + 4.0F, yt + 4.4F, -0.2F, 0.2F, 0xE0212F, false);
        }
        if (s.has(HELMET)) {
            p.box(cx - 0.5F, cx + 0.5F, yt, yt + 0.9F, -0.5F, 0.5F, s.accent, false);
        }
    }
}
