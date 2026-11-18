package com.mundosdeaxelis.vfx;

import static com.mundosdeaxelis.vfx.Creature3D.*;

/**
 * Compañeros del lote 4: zorro de nueve colas (rediseñado) y las especies nuevas. Mismo motor que {@link Creature3D}.
 */
final class Creatures4 {
    private Creatures4() {
    }

    /** Membrana triangular entre una raíz y una fila de puntas (alas de dragón, murciélago). */
    static void membrane(Ctx c, V3 root, V3[] tips, Paint paint) {
        for (int i = 0; i < tips.length - 1; i++) {
            V3 a = tips[i], b = tips[i + 1];
            V3 n = a.sub(root).cross(b.sub(root)).norm();
            V3[] p = {root, a, b, root.lerp(b, 0.02)};
            V3[] nn = {n, n, n, n};
            float[][] cols = new float[4][];
            for (int k = 0; k < 4; k++) {
                cols[k] = paint.at(n, p[k]);
            }
            c.quad(c.blend, c.alpha, T_WHITE, p, nn, cols, null);
        }
    }

    static void flame(Ctx c, V3 at, double size, float[] col, double t, double ph) {
        boolean em = c.emissive;
        c.emissive = true;
        double k = 0.8 + 0.2 * Math.sin(t * 0.4 + ph);
        ell(c, at.add(0, size * 0.9, 0), new V3(size * 0.45, size * 1.1 * k, size * 0.45), 6, 4, flat(mix(col, WHITE, 0.55)));
        glow(c, at.add(0, size * 0.6, 0), size * 4.0, col, 0.55);
        c.emissive = em;
    }

    // ------------------------------------------------------------------ 22. ZORRO DE NUEVE COLAS (dorado)

    // Homenaje al gran zorro de las nueve colas de los cómics: pelaje naranja ardiente, vientre crema,
    // trazos negros alrededor de los ojos, ojos rojos y colas que terminan en llamas de chakra.
    private static final float[] KG = {0.98F, 0.50F, 0.12F}, KG2 = {1.00F, 0.70F, 0.26F}, KCR = {1F, 0.93F, 0.80F},
            KDK = {0.13F, 0.07F, 0.07F}, KTL = {1F, 0.45F, 0.12F}, KFL = {1F, 0.55F, 0.18F}, KRED = {0.95F, 0.12F, 0.10F};

    private static final double YY = -0.05;

    static void kitsune(Ctx c) {
        double t = c.t, ph = c.ph, w = c.walk;
        double bob = Math.abs(Math.sin(c.gait)) * 0.014 * w;
        c.pivot = new V3(0, 0.35, 0);
        glow(c, new V3(0, 0.45, -0.05), 2.0, new float[]{1F, 0.72F, 0.30F}, 0.10 + 0.03 * Math.sin(t * 0.07));
        // nueve colas en abanico alto (cada una con su propio vaivén y llama al final)
        V3 base = new V3(0, 0.40 + bob + YY, -0.22);
        for (int k = 0; k < 9; k++) {
            double f = (k - 4) / 4.0;
            double yaw = f * 1.05;
            double lift = 0.62 + (1 - Math.abs(f)) * 0.55;
            double sw = Math.sin(t * 0.06 + ph + k * 0.9) * 0.07;
            double len = 0.66 + (1 - Math.abs(f)) * 0.10;
            V3[] cp = new V3[6];
            double[] rd = {0.065, 0.105, 0.125, 0.115, 0.075, 0.018};
            for (int i = 0; i < 6; i++) {
                double u = i / 5.0;
                double a = yaw + sw * u;
                double hx = Math.sin(a) * (0.10 + len * u * 0.85);
                double hz = -Math.cos(a) * (0.05 + len * u * 0.55);
                double hy = Math.sin(lift * u * 1.35) * len * 0.78 - u * u * 0.06;
                cp[i] = base.add(hx, hy, hz);
            }
            final double fk = f;
            final double by = base.y;
            curve(c, cp, rd, 3, 9, (n, p) -> {
                double u = cl((p.y - by) / 0.55, 0, 1);
                float[] col = mix(KG, KG2, ss(0.0, 0.7, u));
                return mix(col, KCR, ss(0.62, 0.98, u));
            });
            final double fby = by;
            Detail.furTube(c, cp, rd, 12, 251 + k, 0.05, 0.022, 0.8, (n, p) -> {
                double u = cl((p.y - fby) / 0.55, 0, 1);
                return mix(mix(KG, KG2, ss(0.0, 0.7, u)), KCR, ss(0.62, 0.98, u));
            }, KG2);
            flame(c, cp[5], 0.032, KFL, t, ph + k);
        }
        // patas robustas con calcetines oscuros
        legs4(c, 0.082, 0.14, -0.15, 0.27 + bob, 0.042, 0.03, 0.065, (n, p) -> mix(KG, KDK, ss(0.14, 0.05, p.y)), flat(KDK));
        // cuerpo
        V3 bc = new V3(0, 0.38 + bob + YY, -0.03), br = new V3(0.15, 0.135, 0.27);
        Paint bp = (n, p) -> mix(KG, KCR, ss(0.0, -0.95, n.y) * 0.85);
        ell(c, bc, br, 16, 11, bp);
        Detail.fur(c, bc, br, 150, 261, 0.04, 0.017, new V3(0, -0.4, -1), 0.9, Detail.ALL, bp, KG2);
        // marcas rojas de fuego a lo largo del lomo que laten
        for (int k = 0; k < 4; k++) {
            Detail.twinkle(c, new V3(0, 0.515 + bob + YY - k * 0.004, 0.10 - k * 0.1), 0.026, KTL, t, ph + k * 0.9, 0.07);
        }
        // pecho esponjoso
        ell(c, new V3(0, 0.41 + bob + YY, 0.17), new V3(0.115, 0.115, 0.11), 12, 8, (n, p) -> mix(KCR, KG2, ss(0.2, 0.9, n.y) * 0.35));
        Detail.fur(c, new V3(0, 0.41 + bob + YY, 0.17), new V3(0.115, 0.115, 0.11), 55, 262, 0.05, 0.02, new V3(0, -0.6, 0.2), 0.7, d -> ss(0.0, 0.6, d.z), flat(KCR), WHITE);
        // cuello
        V3[] nk = {new V3(0, 0.43 + bob + YY, 0.14), new V3(0, 0.50 + bob + YY, 0.20), new V3(0, 0.56 + bob + YY, 0.24)};
        tube(c, nk, new double[]{0.105, 0.090, 0.080}, 10, (n, p) -> mix(KG, KCR, ss(0.1, -0.9, n.y) * 0.8));
        // collar dorado con joya
        V3[] ring = new V3[13];
        for (int i = 0; i < 13; i++) {
            double a = Math.PI * 2 * i / 12.0;
            ring[i] = new V3(Math.cos(a) * 0.098, 0.485 + bob + YY + Math.sin(a) * 0.0, Math.sin(a) * 0.098 + 0.19);
        }
        double[] rr = new double[13];
        java.util.Arrays.fill(rr, 0.013);
        tube(c, ring, rr, 6, flat(new float[]{0.62F, 0.10F, 0.10F}));
        c.emissive = true;
        ell(c, new V3(0, 0.455 + bob + YY, 0.285), new V3(0.026, 0.032, 0.02), 8, 6, flat(KTL));
        glow(c, new V3(0, 0.455 + bob + YY, 0.29), 0.12, KTL, 0.5);
        c.emissive = false;
        // cabeza
        headLook(c, 0.13, 0);
        c.hpv = new V3(0, 0.55, 0.22);
        V3 hc = new V3(0, 0.62 + bob + YY, 0.28), hr = new V3(0.13, 0.108, 0.115);
        Paint hp = (n, p) -> mix(KG, KG2, ss(-0.2, 0.9, n.y) * 0.5);
        ell(c, hc, hr, 16, 11, hp);
        Detail.fur(c, hc, hr, 70, 263, 0.03, 0.013, new V3(0, -0.4, -1), 0.8, d -> d.z < 0.5 ? 1 : 0, hp, KG2);
        // mejillas peludas
        for (int sd = -1; sd <= 1; sd += 2) {
            V3 a = new V3(sd * 0.10, 0.60 + bob + YY, 0.31);
            V3 b = new V3(sd * 0.185, 0.565 + bob + YY, 0.26);
            cone(c, a, b, 0.045, 0.004, 7, flat(KCR));
            for (int k = -1; k <= 1; k++) {
                Detail.whisker(c, new V3(sd * 0.05, 0.575 + bob + YY + k * 0.012, 0.43), new V3(sd * 0.18, 0.58 + bob + YY + k * 0.035, 0.40), -0.01, 0.003, new float[]{1F, 0.95F, 0.85F});
            }
        }
        // hocico alargado
        ell(c, new V3(0, 0.585 + bob + YY, 0.385), new V3(0.052, 0.044, 0.085), 12, 8, (n, p) -> mix(KCR, KG2, ss(0.4, 1.0, n.y) * 0.5));
        double nt = Math.sin(t * 0.5 + ph) * 0.003;
        ell(c, new V3(0, 0.598 + bob + YY + nt, 0.465), new V3(0.019, 0.015, 0.013), 8, 5, flat(BLACK));
        Detail.glint(c, new V3(0, 0.605 + bob + YY + nt, 0.472), new V3(0.019, 0.015, 0.013), new V3(0, 0.7, 1), 0.014, WHITE, 0.9);
        // colmillitos
        for (int sd = -1; sd <= 1; sd += 2) {
            cone(c, new V3(sd * 0.026, 0.566 + bob + YY, 0.445), new V3(sd * 0.026, 0.536 + bob + YY, 0.452), 0.0085, 0.0008, 5, flat(WHITE));
        }
        // orejas altas con punta oscura
        for (int sd = -1; sd <= 1; sd += 2) {
            V3 b0 = new V3(sd * 0.075, 0.69 + bob + YY, 0.22);
            V3 b1 = new V3(sd * 0.105, 0.78 + bob + YY, 0.20);
            V3 b2 = new V3(sd * 0.125, 0.87 + bob + YY, 0.185);
            double ef = Detail.flick(c, sd > 0 ? 31 : 32);
            b1 = b1.add(sd * 0.03 * ef, 0, 0); b2 = b2.add(sd * 0.09 * ef, -0.07 * ef, 0);
            curve(c, new V3[]{b0, b1, b2}, new double[]{0.048, 0.036, 0.005}, 3, 7, (n, p) -> mix(KG, KDK, ss(0.80, 0.87, p.y - bob)));
            curve(c, new V3[]{b0.add(0, 0.005, 0.022), b1.add(0, 0.005, 0.022), b2.add(-sd * 0.004, -0.02, 0.02)}, new double[]{0.028, 0.02, 0.003}, 3, 6, flat(KCR));
        }
        // trazos negros de los ojos hacia las mejillas y marca en la frente
        c.emissive = true;
        glow(c, new V3(0, 0.71 + bob + YY, 0.34), 0.10, KTL, 0.35);
        c.emissive = false;
        for (int sd = -1; sd <= 1; sd += 2) {
            // tres trazos negros que salen del rabillo del ojo hacia la mejilla
            for (int k = 0; k < 3; k++) {
                V3 d0 = new V3(sd * 0.74, 0.14 - k * 0.07, 0.62);
                V3 d1 = new V3(sd * (1.0 + 0.05 * k), -0.10 - k * 0.20, 0.30 - k * 0.04);
                V3 a = surf(hc, hr, d0), b = surf(hc, hr, d1);
                V3 na = new V3((a.x - hc.x) / (hr.x * hr.x), (a.y - hc.y) / (hr.y * hr.y), (a.z - hc.z) / (hr.z * hr.z)).norm().mul(0.003);
                V3 nb = new V3((b.x - hc.x) / (hr.x * hr.x), (b.y - hc.y) / (hr.y * hr.y), (b.z - hc.z) / (hr.z * hr.z)).norm().mul(0.003);
                cone(c, a.add(na), b.add(nb), 0.0065, 0.0018, 5, flat(KDK));
            }
            // ceja inclinada hacia dentro: el ceño firme del zorro grande
            V3 e0 = surf(hc, hr, new V3(sd * 0.72, 0.52, 0.62)), e1 = surf(hc, hr, new V3(sd * 0.22, 0.34, 0.92));
            cone(c, e0, e1, 0.0085, 0.0035, 5, flat(KDK));
        }
        eyes(c, hc, hr, 0.50, 0.16, 0.82, 0.105, T_EYE, KRED);
        c.head = false;
        // fuegos fatuos orbitando
        c.emissive = true;
        for (int k = 0; k < 3; k++) {
            double a = t * 0.05 + k * 2.09;
            V3 wp = new V3(Math.cos(a) * 0.50, 0.36 + 0.12 * Math.sin(a * 2 + k), Math.sin(a) * 0.50);
            glow(c, wp, 0.18, KFL, 0.7);
            ell(c, wp, new V3(0.02, 0.034, 0.02), 6, 4, flat(mix(KFL, WHITE, 0.6)));
        }
        c.emissive = false;
    }
}
