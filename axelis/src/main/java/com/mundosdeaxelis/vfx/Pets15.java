package com.mundosdeaxelis.vfx;

import static com.mundosdeaxelis.vfx.Creature3D.*;

/**
 * Mascotas originales del lote 6: Bellota Brotada, Salchicha Elástica, Nutria Agente, Tejón Caballero y Pilluelo Lunar.
 */
final class Pets15 {
    private Pets15() {
    }

    private static float[] hex(int c) {
        return rgb(c);
    }

    // ------------------------------------------------------------------ 55. PILLUELO LUNAR

    private static final float[] PL_V = hex(0x8E6CD8), PL_V2 = hex(0xB89CF4), PL_BE = hex(0xEADFFF), PL_EY = hex(0xFFE04A), PL_AN = hex(0x62E8C8), PL_DK = hex(0x4A3A8A);

    static void moonImp(Ctx c) {
        double t = c.t, ph = c.ph, w = c.walk;
        double bob = Math.abs(Math.sin(c.gait * 1.4)) * 0.03 * w + 0.01 * c.breath;
        c.pivot = new V3(0, 0.22, 0);
        // cola con punta de flecha
        double wag = Math.sin(t * 0.18 + ph) * 0.05 + Math.sin(c.gait) * 0.03 * w;
        V3[] tcp = {new V3(0, 0.15 + bob, -0.1), new V3(wag * 0.5, 0.12 + bob, -0.2), new V3(wag, 0.17 + bob, -0.29), new V3(wag * 1.4, 0.25 + bob, -0.32)};
        curve(c, tcp, new double[]{0.028, 0.022, 0.016, 0.01}, 4, 6, flat(PL_V));
        cone(c, tcp[3], tcp[3].add(wag * 0.2, 0.06, -0.01), 0.026, 0.0, 4, flat(PL_AN));
        // piernas cortas con garritas
        for (int sd = -1; sd <= 1; sd += 2) {
            double p = c.gait * 1.4 + (sd > 0 ? Math.PI : 0);
            double sw = Math.sin(p) * 0.06 * w, lift = Math.max(0, Math.cos(p)) * 0.045 * w;
            V3 a = new V3(sd * 0.055, 0.12 + bob, 0), b = new V3(sd * 0.065, 0.025 + lift, sw);
            tube(c, new V3[]{a, a.lerp(b, 0.5), b}, new double[]{0.03, 0.026, 0.024}, 6, flat(PL_V));
            ell(c, b.add(0, -0.008, 0.025), new V3(0.036, 0.016, 0.05), 8, 5, flat(PL_DK));
            for (int k = -1; k <= 1; k++) {
                cone(c, b.add(k * 0.016, -0.006, 0.065), b.add(k * 0.018, -0.012, 0.09), 0.007, 0.0, 4, flat(PL_BE));
            }
        }
        // cuerpo redondo con barriguita clara
        V3 bc = new V3(0, 0.225 + bob, 0), br = new V3(0.115, 0.12, 0.1);
        Paint bp = (n, p) -> mix(mix(PL_V, PL_V2, ss(0.0, 0.9, n.y) * 0.5), PL_BE, ss(0.1, 0.7, n.z) * ss(0.3, -0.3, n.y) * 0.9);
        ell(c, bc, br, 14, 10, bp);
        // cuatro bracitos: dos arriba, dos abajo
        for (int sd = -1; sd <= 1; sd += 2) {
            for (int r = 0; r < 2; r++) {
                double wv = Math.sin(t * 0.14 + ph + sd * 1.3 + r * 2) * 0.02;
                V3 a = new V3(sd * 0.1, 0.27 - r * 0.07 + bob, 0.03), b = new V3(sd * (0.17 + r * 0.01), 0.3 - r * 0.1 + bob + wv, 0.07);
                tube(c, new V3[]{a, a.lerp(b, 0.5), b}, new double[]{0.017, 0.015, 0.013}, 5, flat(PL_V));
                ell(c, b, new V3(0.02, 0.02, 0.02), 6, 5, flat(PL_BE));
            }
        }
        // cabeza grande con ojos enormes brillantes y sonrisa pícara
        headLook(c, 0.12, 0.0);
        c.hpv = new V3(0, 0.33, 0.0);
        V3 hc = new V3(0, 0.43 + bob, 0.02), hr = new V3(0.15, 0.125, 0.13);
        Paint hp = (n, p) -> mix(PL_V, PL_V2, ss(0.0, 0.9, n.y) * 0.5);
        ell(c, hc, hr, 16, 11, hp);
        boolean em = c.emissive;
        for (int sd = -1; sd <= 1; sd += 2) {
            V3 ec = hc.add(sd * 0.06, 0.02, 0.105);
            ell(c, ec, new V3(0.05, 0.062, 0.03), 0, 0, sd * -0.18, 12, 8, flat(PL_DK));
            c.emissive = true;
            ell(c, ec.add(0, 0, 0.012), new V3(0.04, 0.052, 0.022), 0, 0, sd * -0.18, 12, 8, flat(c.shut ? PL_V : PL_EY));
            c.emissive = em;
            if (!c.shut) {
                double lx = Math.sin(t * 0.05 + ph) * 0.008;
                ell(c, ec.add(lx, 0, 0.026), new V3(0.016, 0.03, 0.01), 6, 5, flat(BLACK));
                Detail.glint(c, ec.add(lx - 0.006, 0.012, 0.03), new V3(0.016, 0.03, 0.01), new V3(-0.3, 0.5, 1), 0.012, WHITE, 0.95);
            }
        }
        decal(c, hc, hr, new V3(0, -0.34, 1), 0.12, 0, T_MOUTH, new float[]{0.35F, 0.12F, 0.2F}, 1, 0.004);
        for (int sd = -1; sd <= 1; sd += 2) {
            V3 fb = hc.add(sd * 0.026, -0.058, 0.118);
            cone(c, fb, fb.add(0, -0.016, 0.004), 0.007, 0.0, 4, flat(WHITE));
        }
        cheeks(c, hc, hr, 0.86, -0.1, 0.5, 0.07);
        // orejas largas de punta
        for (int sd = -1; sd <= 1; sd += 2) {
            double ef = Detail.flick(c, sd > 0 ? 39 : 40) * 0.03;
            V3 eb = hc.add(sd * 0.1, 0.05, -0.02);
            V3[] ep = {eb, eb.add(sd * (0.06 + ef), 0.04, -0.02), eb.add(sd * (0.13 + ef), 0.02, -0.04)};
            tube(c, ep, new double[]{0.04, 0.03, 0.006}, 5, flat(PL_V2));
            // antenas con bolita brillante
            double aw = Math.sin(t * 0.1 + sd + ph) * 0.02;
            V3 ab = hc.add(sd * 0.03, 0.115, -0.02);
            V3[] an = {ab, ab.add(sd * 0.03 + aw, 0.07, 0.0), ab.add(sd * 0.05 + aw * 2, 0.135, 0.03)};
            curve(c, an, new double[]{0.009, 0.008, 0.007}, 3, 5, flat(PL_V));
            c.emissive = true;
            ell(c, an[2], new V3(0.017, 0.017, 0.017), 7, 5, flat(PL_AN));
            c.emissive = em;
            glow(c, an[2], 0.09, new float[]{0.4F, 1F, 0.8F}, 0.45);
        }
        c.head = false;
        for (int k = 0; k < 4; k++) {
            double u = ((t * 0.014 + ph * 0.2 + k * 0.25) % 1.0);
            Detail.twinkle(c, new V3(Math.sin(k * 2.3 + u * 3) * 0.22, 0.3 + u * 0.4 + bob, Math.cos(k * 1.7) * 0.18), 0.02 * (1 - u * 0.4), new float[]{0.7F, 1F, 0.85F}, t, ph + k, 0.3);
        }
    }
}
