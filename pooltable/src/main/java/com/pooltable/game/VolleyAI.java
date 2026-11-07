package com.pooltable.game;

import com.pooltable.game.VolleyMatch.P;

/**
 * Los bots del voleibol. Tres niveles (0 fácil, 1 normal, 2 difícil) que cambian su velocidad, su tiempo de reacción, cuánto fallan al apuntar,
 * qué tan seguido rematan y bloquean. Juegan igual que una persona: reciben (toque 1), colocan (toque 2) y rematan o mandan la pelota por encima (toque 3).
 */
public final class VolleyAI {
    public static final double[] SPEED = {3.0, 3.8, 4.8};
    public static final double[] REACT = {0.3, 0.18, 0.1};
    public static final double[] ERR = {0.6, 0.45, 0.3};
    public static final double[] PRED = {2.0, 1.5, 0.8};
    public static final double[] REACH_SCALE = {0.8, 0.95, 1.05};
    public static final double[] SERVE_ERR = {0.8, 0.5, 0.28};
    public static final double[] SPIKE_P = {0.35, 0.7, 0.95};
    public static final double[] BLOCK_P = {0.1, 0.35, 0.6};

    private VolleyAI() {
    }

    // ------------------------------------------------------------------ movimiento

    static void moveToward(VolleyMatch m, P p, double tx, double tz, double speed, double dt) {
        double own = -VolleyMatch.dir(p.team);
        double hl = m.hl();
        double hw = m.hw();
        double minX = own > 0 ? 0.5 : -(hl + 1.8);
        double maxX = own > 0 ? hl + 1.8 : -0.5;
        tx = VolleyMatch.clamp(tx, minX, maxX);
        tz = VolleyMatch.clamp(tz, -hw - 1.2, hw + 1.2);
        double dx = tx - p.x;
        double dz = tz - p.z;
        double d = Math.hypot(dx, dz);
        if (d < 0.02) {
            p.vx = 0;
            p.vz = 0;
            return;
        }
        double step = Math.min(d, speed * dt);
        p.vx = dx / d * (step / dt);
        p.vz = dz / d * (step / dt);
        p.x += dx / d * step;
        p.z += dz / d * step;
    }

    public static void goHome(VolleyMatch m, P p, double dt) {
        moveToward(m, p, p.homeX, p.homeZ, SPEED[m.diff], dt);
    }

    // ------------------------------------------------------------------ saque

    public static void serve(VolleyMatch m, P s) {
        double fwd = -0.2 + m.rnd.nextDouble() * 1.2;
        double right = m.rnd.nextDouble() * 2 - 1;
        m.doServe(s, fwd, right);
    }

    // ------------------------------------------------------------------ jugada

    private static boolean eligible(VolleyMatch m, int i) {
        P q = m.players.get(i);
        if (m.touchTeam == q.team) {
            if (m.touches >= 3) {
                return false;
            }
            return !(m.n > 1 && m.touches > 0 && m.lastToucher == i);
        }
        return true;
    }

    private static int nearest(VolleyMatch m, int team, double x, double z, boolean needEligible) {
        int best = -1;
        double bd = 1e9;
        for (int i = 0; i < m.players.size(); i++) {
            P q = m.players.get(i);
            if (q.team != team || needEligible && !eligible(m, i)) {
                continue;
            }
            double d = Math.hypot(q.x - x, q.z - z) - (q.human ? 0.5 : 0.0);
            if (d < bd) {
                bd = d;
                best = i;
            }
        }
        return best;
    }

    public static void update(VolleyMatch m, P p, double dt) {
        int idx = m.players.indexOf(p);
        int d = m.diff;
        double side = -VolleyMatch.dir(p.team);
        double speed = SPEED[d];
        double[] b = m.ball;
        double[] v = m.vel;
        double tl = m.timeToHeight(VolleyMatch.R);
        if (tl < 0) {
            tl = 0;
        }
        double lx = b[0] + v[0] * tl;
        double lz = b[2] + v[2] * tl;
        if (p.aiSeenHit != m.lastHitTime) {
            // cada golpe del rival: este bot calcula mal dónde caerá (según el nivel) y corrige al acercarse la pelota
            p.aiSeenHit = m.lastHitTime;
            p.aiOffX = m.rnd.nextGaussian() * PRED[d];
            p.aiOffZ = m.rnd.nextGaussian() * PRED[d];
        }
        double fade = Math.max(0.0, Math.min(1.0, tl / 0.9));
        lx += p.aiOffX * fade;
        lz += p.aiOffZ * fade;
        boolean landsMine = lx * side > 0 && Math.abs(lx) <= m.hl() + 0.5 && Math.abs(lz) <= m.hw() + 0.5;
        boolean receive = m.touchTeam != p.team || m.touches == 0;
        p.face = Math.atan2(b[2] - p.z, b[0] - p.x);
        boolean reacted = m.time - m.lastHitTime >= REACT[d] * 1.5;
        if (!reacted) {
            speed = 0;
        }

        if (receive && landsMine && (m.touchTeam != p.team || b[0] * side > 0)) {
            // la pelota viene a mi lado: la recibe el más cercano
            int recv = nearest(m, p.team, lx, lz, true);
            if (recv == idx) {
                moveToward(m, p, lx, lz, speed, dt);
                strike(m, p, VolleyMatch.HIT, 0.0, 0.0, 2.0, d);
            } else {
                double sz = lz > 0 ? -m.hw() * 0.4 : m.hw() * 0.4;
                moveToward(m, p, side * 1.7, sz, speed, dt);
                p.aiWait = -1;
            }
            return;
        }
        if (m.touchTeam == p.team && m.touches >= 1 && b[0] * side > 0 && m.nextHitter == idx) {
            if (m.touches == 1) {
                // soy quien coloca
                moveToward(m, p, lx, lz, speed, dt);
                strike(m, p, VolleyMatch.SET, 0.0, 0.0, 2.35, d);
            } else {
                attack(m, p, idx, dt);
            }
            return;
        }
        if (m.touchTeam != p.team && b[0] * side < 0 && m.touches >= 2 && Math.abs(b[0]) < 3.0) {
            // el rival va a rematar: bloqueo si soy el más cercano a la red en esa zona
            int blk = nearest(m, p.team, side * 0.8, b[2], false);
            if (blk == idx) {
                moveToward(m, p, side * 0.8, VolleyMatch.clamp(b[2], -m.hw() + 0.4, m.hw() - 0.4), speed, dt);
                if (b[1] > VolleyMatch.NET_TOP - 0.3 && p.y <= 0 && p.jumpIn < 0 && m.rnd.nextDouble() < BLOCK_P[d]) {
                    p.jumpIn = REACT[d] * (0.5 + m.rnd.nextDouble());
                }
                if (p.jumpIn >= 0) {
                    p.jumpIn -= dt;
                    if (p.jumpIn < 0 && p.y <= 0) {
                        p.wantJump = true;
                        p.jumpIn = -1;
                    }
                }
                if (p.y > 0.25 && m.reachable(p, false)) {
                    m.act(p, VolleyMatch.HIT, 0.0, 0.0, false);
                }
                return;
            }
        }
        // en cualquier otro caso, me acomodo en mi lugar (con un poco de atención hacia donde va la pelota)
        double tz = p.homeZ + (b[2] - p.homeZ) * 0.25;
        moveToward(m, p, p.homeX, tz, speed, dt);
        p.aiWait = -1;
        p.jumpIn = -1;
    }

    /** Espera un instante de reacción y golpea cuando la pelota está al alcance. */
    private static void strike(VolleyMatch m, P p, int kind, double fwd, double right, double maxY, int d) {
        boolean ok = m.reachable(p, false) && m.ball[1] <= maxY && m.vel[1] <= 1.5;
        if (!ok) {
            p.aiWait = -1;
            return;
        }
        if (p.aiWait < 0) {
            p.aiWait = REACT[d] * (0.5 + m.rnd.nextDouble());
        }
        p.aiWait -= 0.05;
        if (p.aiWait <= 0 || m.ball[1] < 1.0) {
            p.aiWait = -1;
            m.act(p, kind, fwd, right, false);
        }
    }

    /** Tercer toque: remate saltando (según el nivel) o pelota suave por encima de la red, buscando el lado del rival donde hay más hueco. */
    private static void attack(VolleyMatch m, P p, int idx, double dt) {
        int d = m.diff;
        double[] b = m.ball;
        double side = -VolleyMatch.dir(p.team);
        double s = VolleyMatch.dir(p.team);
        double hitY = 2.55;
        double th = m.timeToHeight(hitY);
        double px = b[0];
        double pz = b[2];
        if (th > 0) {
            px = b[0] + m.vel[0] * th;
            pz = b[2] + m.vel[2] * th;
        }
        moveToward(m, p, px, pz, SPEED[d], dt);
        // hueco del rival: el punto lateral más lejos de todos los defensores
        double wantZ = 0;
        double bestGap = -1;
        for (int k = -4; k <= 4; k++) {
            double cz = k / 4.0 * m.hw() * 0.8;
            double gap = 1e9;
            for (P q : m.players) {
                if (q.team != p.team) {
                    gap = Math.min(gap, Math.abs(q.z - cz));
                }
            }
            gap += m.rnd.nextDouble() * 0.4;
            if (gap > bestGap) {
                bestGap = gap;
                wantZ = cz;
            }
        }
        double right = VolleyMatch.clamp(wantZ / (s * m.hw() * 0.75), -1, 1);
        double fwd = 0.2 + m.rnd.nextDouble() * 0.8;
        boolean spike = p.jumpIn == -2 || p.jumpIn == -1 && m.rnd.nextDouble() < SPIKE_P[d];
        if (p.jumpIn == -1) {
            p.jumpIn = spike ? -2 : -3; // decisión tomada una vez por jugada
        }
        if (p.jumpIn == -2) {
            if (p.y <= 0 && th > 0 && th < 0.3 && Math.hypot(p.x - px, p.z - pz) < 1.3) {
                p.wantJump = true;
            }
            if (p.y > 0.3 && m.reachable(p, false) && b[1] >= 2.2) {
                m.act(p, VolleyMatch.HIT, fwd, right, false);
            } else if (p.y <= 0 && m.reachable(p, false) && b[1] < 1.9) {
                m.act(p, VolleyMatch.HIT, fwd, right, false);
            }
        } else {
            strike(m, p, VolleyMatch.HIT, fwd, right, 2.0, d);
        }
    }

    /** Se llama al empezar cada jugada para que la decisión de rematar se tome de nuevo. */
    public static void newRally(P p) {
        p.jumpIn = -1;
    }
}
