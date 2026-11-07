import com.pooltable.game.*;
import java.util.*;

/** Prueba en Java puro del voleibol: bots contra bots en las 3 medidas de cancha y 3 niveles, y un humano de mentira que usa la misma interfaz que el jugador. */
public class VolleyTest {
    static int fails = 0;
    static void check(boolean ok, String msg) { if (!ok) { fails++; System.out.println("FALLO: " + msg); } }

    static class Stats { int points, touchesTotal, spikes, blocks, bumps, sets, frees, serves, outs, nets, rejects; double time; }

    static void count(VolleyMatch m, Stats st) {
        switch (m.evAct) {
            case SPIKE -> st.spikes++;
            case BLOCK -> st.blocks++;
            case BUMP -> st.bumps++;
            case SET -> st.sets++;
            case FREE -> st.frees++;
            case SERVE -> st.serves++;
            default -> {}
        }
        if (m.evAct != VolleyMatch.Act.NONE) st.touchesTotal++;
        if (m.evOut) st.outs++;
        if (m.evNet) st.nets++;
        if (m.evPoint != 0) st.points++;
        if (!m.evReject.isEmpty()) st.rejects++;
    }

    static boolean finite(VolleyMatch m) {
        for (double d : m.ball) if (Double.isNaN(d) || Double.isInfinite(d)) return false;
        for (double d : m.vel) if (Double.isNaN(d) || Double.isInfinite(d)) return false;
        for (VolleyMatch.P p : m.players) if (Double.isNaN(p.x) || Double.isNaN(p.z) || Double.isNaN(p.y)) return false;
        return true;
    }

    /** Partido entre bots: diffA para el equipo 0 y diffB para el 1 (la dificultad de la partida es una sola, asi que se usa la de A para los dos). */
    static int botMatch(long seed, int n, int diff, Stats st) {
        VolleyMatch m = new VolleyMatch(new Random(seed));
        for (int t = 0; t < 2; t++) for (int i = 0; i < n; i++) m.addPlayer(t, false, "bot" + t + i);
        m.start(n, diff);
        double time = 0;
        while (m.state != VolleyMatch.State.IDLE && time < 3600) {
            m.tick(0.05);
            time += 0.05;
            count(m, st);
            m.clearEvents();
            if (!finite(m)) { check(false, "numero invalido n=" + n); return -1; }
            double lim = 60;
            check(Math.abs(m.ball[0]) < lim && Math.abs(m.ball[2]) < lim && m.ball[1] < 80, "pelota fuera de rango");
        }
        st.time += time;
        check(m.state == VolleyMatch.State.IDLE, "el partido termina n=" + n + " d=" + diff + " tiempo=" + time);
        int a = Math.max(m.scoreA, m.scoreB), b = Math.min(m.scoreA, m.scoreB);
        check(a >= VolleyMatch.WIN && (a - b >= 2 || a == VolleyMatch.CAP), "marcador valido " + m.scoreA + "-" + m.scoreB);
        return m.winner;
    }

    /** Humano de mentira (equipo 0) con la misma API que el cliente: se mueve hacia donde cae la pelota, salta para rematar y manda HIT/SET. */
    static int humanMatch(long seed, int n, int diff, Stats st) {
        VolleyMatch m = new VolleyMatch(new Random(seed));
        for (int i = 0; i < n; i++) m.addPlayer(0, true, "humano" + i);
        for (int i = 0; i < n; i++) m.addPlayer(1, false, "bot" + i);
        m.start(n, diff);
        double[] jump = new double[m.players.size()];
        double time = 0;
        while (m.state != VolleyMatch.State.IDLE && time < 3600) {
            // mover humanos (4,3 b/s) y simular su salto
            for (int i = 0; i < m.players.size(); i++) {
                VolleyMatch.P p = m.players.get(i);
                if (!p.human) continue;
                double tx = p.homeX, tz = p.homeZ;
                if (m.state == VolleyMatch.State.SERVE && m.serverIdx == i) {
                    double[] sp = m.serveSpot(0);
                    p.x = sp[0]; p.z = sp[1];
                    m.humanAct(i, VolleyMatch.HIT, 0.5, 0.0, false);
                }
                if (m.state == VolleyMatch.State.RALLY) {
                    double tl = Math.max(0, m.timeToHeight(1.2));
                    double lx = m.ball[0] + m.vel[0] * tl, lz = m.ball[2] + m.vel[2] * tl;
                    boolean mine = lx < 0;
                    boolean myTurn = m.touchTeam != 0 || m.touches == 0 || m.nextHitter == i;
                    // el companero con la pelota en camino va hacia ella; los demas a su lugar
                    int nearest = -1; double bd = 1e9;
                    for (int j = 0; j < m.players.size(); j++) { VolleyMatch.P q = m.players.get(j); if (q.team == 0 && q.human) { double dd = Math.hypot(q.x - lx, q.z - lz); if (dd < bd) { bd = dd; nearest = j; } } }
                    if (mine && nearest == i && myTurn) { tx = lx; tz = lz; }
                    double dx = tx - p.x, dz = tz - p.z, dd = Math.hypot(dx, dz);
                    double step = Math.min(dd, 4.3 * 0.05);
                    if (dd > 1e-6) { p.x += dx / dd * step; p.z += dz / dd * step; p.vz = dz / dd * 4.3; }
                    // salto
                    if (p.y > 0 || jump[i] > 0) { jump[i] -= 0.05; }
                    double t255 = m.timeToHeight(2.5);
                    boolean attacking = m.touchTeam == 0 && m.touches == 2 && m.nextHitter == i;
                    if (attacking && p.y == 0 && t255 > 0 && t255 < 0.3 && Math.hypot(p.x - (m.ball[0] + m.vel[0] * t255), p.z - (m.ball[2] + m.vel[2] * t255)) < 1.2) { p.vy = 5.0; p.y = 0.01; }
                    if (p.y > 0) { p.y += p.vy * 0.05; p.vy -= 28 * 0.05; if (p.y <= 0) { p.y = 0; p.vy = 0; } }
                    if (myTurn && mine && m.reachable(p, false)) {
                        int kind = (m.touchTeam == 0 && m.touches == 1) ? VolleyMatch.SET : VolleyMatch.HIT;
                        m.humanAct(i, kind, 0.4, 0.3, false);
                    }
                } else if (p.y > 0) { p.y = 0; p.vy = 0; }
            }
            m.tick(0.05);
            time += 0.05;
            count(m, st);
            m.clearEvents();
            if (!finite(m)) { check(false, "numero invalido (humano)"); return -1; }
        }
        st.time += time;
        check(m.state == VolleyMatch.State.IDLE, "partido con humano termina n=" + n + " tiempo=" + time);
        return m.winner;
    }

    public static void main(String[] a) {
        for (int n = 1; n <= 3; n++) {
            check(VolleyMatch.HL[n - 1] > 3 && VolleyMatch.HW[n - 1] > 2, "medidas n=" + n);
            if (n > 1) check(VolleyMatch.HL[n - 1] > VolleyMatch.HL[n - 2] && VolleyMatch.HW[n - 1] > VolleyMatch.HW[n - 2], "la cancha crece con los jugadores");
        }
        for (int n = 1; n <= 3; n++) {
            for (int d = 0; d < 3; d++) {
                Stats st = new Stats();
                int games = 6;
                for (int g = 0; g < games; g++) botMatch(1000 + g * 7 + n * 100 + d, n, d, st);
                double perPoint = st.touchesTotal / (double) Math.max(1, st.points);
                System.out.printf("bots n=%d d=%d: puntos/partido=%.1f toques/punto=%.2f duracion=%.0fs remates=%d bloqueos=%d fuera=%d red=%d rechazos=%d%n",
                        n, d, st.points / (double) games, perPoint, st.time / games, st.spikes, st.blocks, st.outs, st.nets, st.rejects);
                check(perPoint > 1.3, "hay jugadas con mas de un toque n=" + n + " d=" + d + " (" + perPoint + ")");
                check(st.time / games < 1500, "duracion razonable n=" + n + " d=" + d);
                if (n > 1) check(st.sets > 0, "hay colocaciones n=" + n + " d=" + d);
                if (d == 2 && n > 1) check(st.spikes > 0, "hay remates en dificil n=" + n);
            }
        }
        for (int n = 1; n <= 3; n++) {
            for (int d = 0; d < 3; d++) {
                Stats st = new Stats();
                int wins = 0, games = 4;
                for (int g = 0; g < games; g++) if (humanMatch(77 + g * 13 + n * 5 + d, n, d, st) == 0) wins++;
                System.out.printf("humano n=%d d=%d: gana %d/%d, toques/punto=%.2f remates=%d bloqueos=%d rechazos=%d duracion=%.0fs%n", n, d, wins, games,
                        st.touchesTotal / (double) Math.max(1, st.points), st.spikes, st.blocks, st.rejects, st.time / games);
                check(st.touchesTotal / (double) Math.max(1, st.points) > 1.2, "humano juega jugadas largas n=" + n + " d=" + d);
            }
        }
        if (fails > 0) { System.out.println("PRUEBAS FALLIDAS: " + fails); System.exit(1); }
        System.out.println("VolleyTest: todo bien");
    }
}
