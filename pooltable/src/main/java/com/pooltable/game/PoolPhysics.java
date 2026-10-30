package com.pooltable.game;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * La física del billar, sin nada del juego (solo números): 16 bolas (0 = blanca, 1-7 lisas, 8 negra, 9-15 rayadas) sobre un tablero de
 * L x W bloques. Las coordenadas van de 0 a L (largo, u) y de 0 a W (ancho, v). Las bolas ruedan con fricción, chocan entre sí, rebotan en las
 * bandas (menos en las bocas de las troneras) y caen en las 6 troneras.
 */
public final class PoolPhysics {
    /** Medidas en bloques. */
    public static final double L = 5.0;
    public static final double W = 2.5;
    public static final double R = 0.09;
    public static final int N = 16;

    // troneras: 4 esquinas y 2 laterales
    public static final double CORNER_CAPTURE = 0.18;
    public static final double SIDE_CAPTURE = 0.155;
    /** Centro y radio de cada tronera: {u, v, radio}. El modelo del bloque tiene los hoyos exactamente en estos sitios. */
    public static final double[][] POCKETS = {
            {0.0, 0.0, CORNER_CAPTURE}, {L, 0.0, CORNER_CAPTURE}, {0.0, W, CORNER_CAPTURE}, {L, W, CORNER_CAPTURE},
            {L / 2, -0.03, SIDE_CAPTURE}, {L / 2, W + 0.03, SIDE_CAPTURE}};
    /** Hasta dónde llega la boca de una esquina a lo largo de la banda. */
    private static final double CORNER_MOUTH = 0.20;
    private static final double SIDE_MOUTH = 0.16;

    // ajustes de "sensación" (todos en bloques y segundos)
    public static final double MAX_SPEED = 12.0;
    private static final double FRICTION = 1.1;           // frenado constante (bloques/s^2)
    private static final double DRAG = 0.12;              // frenado proporcional a la velocidad (1/s)
    private static final double BALL_REST = 0.96;         // rebote entre bolas
    private static final double CUSHION_REST = 0.78;      // rebote en banda (se pierde energía)
    private static final double REST_SPEED = 0.025;       // por debajo se detiene
    private static final int SUBSTEPS = 12;               // por tick (1/20 s)

    public final double[] x = new double[N];
    public final double[] z = new double[N];
    public final double[] vx = new double[N];
    public final double[] vz = new double[N];
    public final boolean[] out = new boolean[N];

    /** Primera bola que tocó la blanca en el tiro actual (-1 = ninguna). */
    public int firstContact = -1;
    /** Bolas que cayeron en el tiro actual, en orden. */
    public final List<Integer> potted = new ArrayList<>();
    /** Un golpe de bola contra bola o contra banda (para el sonido): fuerza aproximada; se vacía al leerla. */
    public final List<double[]> impacts = new ArrayList<>();

    public PoolPhysics() {
        rack(new Random());
    }

    // ------------------------------------------------------------------ colocar bolas

    /** Pone todas las bolas en juego y quietas. */
    private void clearTable() {
        for (int i = 0; i < N; i++) {
            vx[i] = 0;
            vz[i] = 0;
            out[i] = false;
        }
        resetShot();
    }

    /** Coloca el triángulo (con la 8 en el centro y una lisa y una rayada en las esquinas de atrás) y la blanca en su sitio. */
    public void rack(Random rnd) {
        clearTable();
        List<Integer> pool = new ArrayList<>();
        for (int i = 1; i < N; i++) {
            if (i != 8) {
                pool.add(i);
            }
        }
        java.util.Collections.shuffle(pool, rnd);
        // esquinas de atrás: una lisa y una rayada
        int solid = -1;
        int stripe = -1;
        for (int b : pool) {
            if (b < 8 && solid < 0) {
                solid = b;
            }
            if (b > 8 && stripe < 0) {
                stripe = b;
            }
        }
        pool.remove((Integer) solid);
        pool.remove((Integer) stripe);
        boolean flip = rnd.nextBoolean();
        int idx = 0;
        int[] slots = new int[15];
        // posiciones por fila: 0 | 1 2 | 3 4 5 | 6 7 8 9 | 10 11 12 13 14 ; la 8 va en la posición 4 (centro de la fila 3)
        for (int s = 0; s < 15; s++) {
            if (s == 4) {
                slots[s] = 8;
            } else if (s == 10) {
                slots[s] = flip ? solid : stripe;
            } else if (s == 14) {
                slots[s] = flip ? stripe : solid;
            } else {
                slots[s] = pool.get(idx++);
            }
        }
        placeRows(slots, new int[] {1, 2, 3, 4, 5});
        placeCue();
    }

    /** Rotación (15 bolas): la 1 en la punta del triángulo, la 2 y la 3 en las esquinas de atrás y el resto al azar. */
    public void rackRotation(Random rnd) {
        clearTable();
        List<Integer> pool = new ArrayList<>();
        for (int i = 4; i < N; i++) {
            pool.add(i);
        }
        java.util.Collections.shuffle(pool, rnd);
        int[] slots = new int[15];
        int idx = 0;
        boolean flip = rnd.nextBoolean();
        for (int s = 0; s < 15; s++) {
            if (s == 0) {
                slots[s] = 1;
            } else if (s == 10) {
                slots[s] = flip ? 2 : 3;
            } else if (s == 14) {
                slots[s] = flip ? 3 : 2;
            } else {
                slots[s] = pool.get(idx++);
            }
        }
        placeRows(slots, new int[] {1, 2, 3, 4, 5});
        placeCue();
    }

    /** 9 bolas: rombo con la 1 delante, la 9 en el centro y las demás al azar; las bolas 10 a 15 quedan fuera de juego. */
    public void rackNine(Random rnd) {
        clearTable();
        List<Integer> pool = new ArrayList<>();
        for (int i = 2; i <= 8; i++) {
            pool.add(i);
        }
        java.util.Collections.shuffle(pool, rnd);
        // filas 1 | 2 | 3 | 2 | 1 -> posiciones 0 | 1 2 | 3 4 5 | 6 7 | 8 ; la 1 en la 0 y la 9 en la 4 (centro)
        int[] slots = new int[9];
        int idx = 0;
        for (int s = 0; s < 9; s++) {
            slots[s] = s == 0 ? 1 : s == 4 ? 9 : pool.get(idx++);
        }
        placeRows(slots, new int[] {1, 2, 3, 2, 1});
        for (int b = 10; b < N; b++) {
            out[b] = true;
        }
        placeCue();
    }

    /** Pone las bolas en filas centradas contra el lado del triángulo (la 1.ª fila es la punta). */
    private void placeRows(int[] slots, int[] rows) {
        double footU = L * 0.75;
        double dRow = Math.sqrt(3.0) * R + 0.002;
        int s = 0;
        for (int row = 0; row < rows.length; row++) {
            for (int j = 0; j < rows[row]; j++) {
                int b = slots[s++];
                x[b] = footU + row * dRow;
                z[b] = W / 2 + (j - (rows[row] - 1) / 2.0) * (2 * R + 0.003);
            }
        }
    }

    private void placeCue() {
        x[0] = L * 0.25;
        z[0] = W / 2;
    }

    /** Devuelve una bola a la mesa en el punto del triángulo (o lo más cerca posible hacia atrás si está ocupado). */
    public void respotBall(int ball) {
        double fu = L * 0.75;
        for (int k = 0; k < 60; k++) {
            double tx = fu + k * 0.05;
            if (free(tx, W / 2, ball)) {
                x[ball] = tx;
                z[ball] = W / 2;
                break;
            }
            double alt = fu + (k / 2) * 0.05;
            double tz = W / 2 + ((k % 2 == 0) ? 1 : -1) * (k / 2) * 0.05;
            if (free(alt, tz, ball)) {
                x[ball] = alt;
                z[ball] = tz;
                break;
            }
        }
        vx[ball] = 0;
        vz[ball] = 0;
        out[ball] = false;
    }

    public void resetShot() {
        firstContact = -1;
        potted.clear();
        impacts.clear();
    }

    /** Vuelve a poner la blanca en la salida (o lo más cerca posible si está ocupada). */
    public void respotCue() {
        double ux = L * 0.25;
        double uz = W / 2;
        for (int k = 0; k < 40; k++) {
            double tx = ux - (k % 2 == 0 ? 1 : 0) * (k / 2) * 0.01 + (k / 2) * 0.0;
            double tz = uz + ((k % 4 < 2) ? 1 : -1) * (k / 4) * 2 * R;
            if (free(tx, tz)) {
                x[0] = tx;
                z[0] = tz;
                break;
            }
            // si no cabe, se prueba más atrás y a los lados
            double bx = ux - (k / 4) * R;
            if (free(bx, uz)) {
                x[0] = bx;
                z[0] = uz;
                break;
            }
        }
        vx[0] = 0;
        vz[0] = 0;
        out[0] = false;
    }

    private boolean free(double px, double pz) {
        return free(px, pz, 0);
    }

    /** ¿Cabe una bola en (px, pz) sin tocar otra? Se ignora la bola {@code self} y se mira también la blanca si no es ella. */
    private boolean free(double px, double pz, int self) {
        if (px < R || px > L - R || pz < R || pz > W - R) {
            return false;
        }
        for (int i = self == 0 ? 1 : 0; i < N; i++) {
            if (i != self && !out[i] && Math.hypot(x[i] - px, z[i] - pz) < 2 * R + 0.002) {
                return false;
            }
        }
        return true;
    }

    // ------------------------------------------------------------------ tiros

    /** Golpea la blanca: ángulo en radianes (0 = hacia +u) y velocidad en bloques/s. */
    public void shoot(double angle, double speed) {
        resetShot();
        vx[0] = Math.cos(angle) * Math.min(speed, MAX_SPEED);
        vz[0] = Math.sin(angle) * Math.min(speed, MAX_SPEED);
    }

    public boolean moving() {
        for (int i = 0; i < N; i++) {
            if (!out[i] && (vx[i] != 0 || vz[i] != 0)) {
                return true;
            }
        }
        return false;
    }

    /** Avanza 1/20 de segundo. */
    public void tick() {
        double dt = 1.0 / 20.0 / SUBSTEPS;
        for (int s = 0; s < SUBSTEPS; s++) {
            step(dt);
        }
    }

    private void step(double dt) {
        for (int i = 0; i < N; i++) {
            if (out[i]) {
                continue;
            }
            double sp = Math.hypot(vx[i], vz[i]);
            if (sp == 0) {
                continue;
            }
            double ns = sp - (FRICTION + DRAG * sp) * dt;
            if (ns < REST_SPEED) {
                vx[i] = 0;
                vz[i] = 0;
                continue;
            }
            double k = ns / sp;
            vx[i] *= k;
            vz[i] *= k;
            x[i] += vx[i] * dt;
            z[i] += vz[i] * dt;
        }
        // choques entre bolas (dos pasadas para que ninguna quede encajada en otra o en una banda)
        for (int pass = 0; pass < 2; pass++) {
            for (int i = 0; i < N; i++) {
                if (out[i]) {
                    continue;
                }
                for (int j = i + 1; j < N; j++) {
                    if (out[j]) {
                        continue;
                    }
                    collide(i, j);
                }
            }
            for (int i = 0; i < N; i++) {
                if (!out[i]) {
                    cushions(i);
                    pockets(i);
                }
            }
        }
    }

    private void collide(int i, int j) {
        double dx = x[j] - x[i];
        double dz = z[j] - z[i];
        double d2 = dx * dx + dz * dz;
        double min = 2 * R;
        if (d2 >= min * min || d2 == 0) {
            return;
        }
        double d = Math.sqrt(d2);
        double nx = dx / d;
        double nz = dz / d;
        // separar
        double overlap = (min - d) / 2;
        x[i] -= nx * overlap;
        z[i] -= nz * overlap;
        x[j] += nx * overlap;
        z[j] += nz * overlap;
        double rv = (vx[i] - vx[j]) * nx + (vz[i] - vz[j]) * nz; // velocidad de acercamiento
        if (rv <= 0) {
            return;
        }
        double imp = rv * (1 + BALL_REST) / 2;
        vx[i] -= imp * nx;
        vz[i] -= imp * nz;
        vx[j] += imp * nx;
        vz[j] += imp * nz;
        if (firstContact < 0 && (i == 0 || j == 0)) {
            firstContact = i == 0 ? j : i;
        }
        impacts.add(new double[] {0, (x[i] + x[j]) / 2, (z[i] + z[j]) / 2, rv});
    }

    private static boolean inMouthU(double u) {
        return u < CORNER_MOUTH || u > L - CORNER_MOUTH || Math.abs(u - L / 2) < SIDE_MOUTH;
    }

    private static boolean inMouthV(double v) {
        return v < CORNER_MOUTH || v > W - CORNER_MOUTH;
    }

    private void cushions(int i) {
        // bandas largas (v = 0 y v = W): tienen las bocas de las esquinas y la del medio
        if (z[i] < R && !inMouthU(x[i])) {
            bounceV(i, R);
        } else if (z[i] > W - R && !inMouthU(x[i])) {
            bounceV(i, W - R);
        }
        // bandas cortas (u = 0 y u = L): bocas de las esquinas
        if (x[i] < R && !inMouthV(z[i])) {
            bounceU(i, R);
        } else if (x[i] > L - R && !inMouthV(z[i])) {
            bounceU(i, L - R);
        }
    }

    private void bounceV(int i, double edge) {
        z[i] = edge;
        double s = Math.abs(vz[i]);
        vz[i] = -vz[i] * CUSHION_REST;
        vx[i] *= 0.985;
        if (s > 0.15) {
            impacts.add(new double[] {1, x[i], z[i], s});
        }
    }

    private void bounceU(int i, double edge) {
        x[i] = edge;
        double s = Math.abs(vx[i]);
        vx[i] = -vx[i] * CUSHION_REST;
        vz[i] *= 0.985;
        if (s > 0.15) {
            impacts.add(new double[] {1, x[i], z[i], s});
        }
    }

    private void pockets(int i) {
        for (double[] p : POCKETS) {
            if (Math.hypot(x[i] - p[0], z[i] - p[1]) < p[2]) {
                out[i] = true;
                vx[i] = 0;
                vz[i] = 0;
                potted.add(i);
                impacts.add(new double[] {2, p[0], p[1], 1.0});
                return;
            }
        }
        // por si una bola sale disparada fuera del tablero (no debería): se cuenta como caída
        if (x[i] < -0.2 || x[i] > L + 0.2 || z[i] < -0.2 || z[i] > W + 0.2) {
            out[i] = true;
            vx[i] = 0;
            vz[i] = 0;
            potted.add(i);
        }
    }

    // ------------------------------------------------------------------ línea de puntería (la usa el cliente)

    /** Resultado de seguir la línea de la blanca. */
    public static final class Trace {
        public double hitX;
        public double hitZ;
        /** Bola tocada (-1 = banda). */
        public int ball = -1;
        /** Dirección en que saldría la bola tocada (si ball >= 0) o el rebote (si es banda). */
        public double outX;
        public double outZ;
        /** Dirección en que sigue la blanca tras el choque. */
        public double cueX;
        public double cueZ;
    }

    /** Sigue la blanca desde su sitio en la dirección (dx, dz) hasta tocar una bola o una banda. */
    public Trace trace(double dx, double dz) {
        double len = Math.hypot(dx, dz);
        dx /= len;
        dz /= len;
        Trace t = new Trace();
        double best = Double.MAX_VALUE;
        double ox = x[0];
        double oz = z[0];
        for (int j = 1; j < N; j++) {
            if (out[j]) {
                continue;
            }
            // distancia a lo largo del rayo hasta que los centros estén a 2R
            double fx = x[j] - ox;
            double fz = z[j] - oz;
            double proj = fx * dx + fz * dz;
            if (proj <= 0) {
                continue;
            }
            double perp2 = fx * fx + fz * fz - proj * proj;
            double min = 2 * R;
            if (perp2 >= min * min) {
                continue;
            }
            double t0 = proj - Math.sqrt(min * min - perp2);
            if (t0 >= 0 && t0 < best) {
                best = t0;
                t.ball = j;
            }
        }
        // bandas
        double tw = Double.MAX_VALUE;
        double wx = 0;
        double wz = 0;
        if (dx > 1e-9) {
            double tt = (L - R - ox) / dx;
            if (tt < tw) {
                tw = tt;
                wx = -1;
                wz = 1;
            }
        } else if (dx < -1e-9) {
            double tt = (R - ox) / dx;
            if (tt < tw) {
                tw = tt;
                wx = -1;
                wz = 1;
            }
        }
        if (dz > 1e-9) {
            double tt = (W - R - oz) / dz;
            if (tt < tw) {
                tw = tt;
                wx = 1;
                wz = -1;
            }
        } else if (dz < -1e-9) {
            double tt = (R - oz) / dz;
            if (tt < tw) {
                tw = tt;
                wx = 1;
                wz = -1;
            }
        }
        if (tw < best) {
            t.ball = -1;
            best = Math.max(0, tw);
            t.outX = dx * wx;
            t.outZ = dz * wz;
            t.cueX = t.outX;
            t.cueZ = t.outZ;
        } else if (t.ball >= 0) {
            best = Math.max(0, best);
        } else {
            best = 0;
        }
        t.hitX = ox + dx * best;
        t.hitZ = oz + dz * best;
        if (t.ball >= 0) {
            double nx = x[t.ball] - t.hitX;
            double nz = z[t.ball] - t.hitZ;
            double nl = Math.hypot(nx, nz);
            nx /= nl;
            nz /= nl;
            t.outX = nx;
            t.outZ = nz;
            // la blanca sigue por la tangente (el resto de su movimiento que no fue hacia la bola)
            double dot = dx * nx + dz * nz;
            double tx = dx - dot * nx;
            double tz = dz - dot * nz;
            double tl = Math.hypot(tx, tz);
            if (tl > 1e-6) {
                t.cueX = tx / tl;
                t.cueZ = tz / tl;
            } else {
                t.cueX = 0;
                t.cueZ = 0;
            }
        }
        return t;
    }

    /** Cuántas bolas de un grupo siguen en la mesa: 0 = lisas (1-7), 1 = rayadas (9-15). */
    public int remaining(int group) {
        int c = 0;
        for (int b = (group == 0 ? 1 : 9); b <= (group == 0 ? 7 : 15); b++) {
            if (!out[b]) {
                c++;
            }
        }
        return c;
    }

    public static int groupOf(int ball) {
        return ball >= 1 && ball <= 7 ? 0 : ball >= 9 ? 1 : -1;
    }
}
