import com.pooltable.game.*;
import java.util.*;

/** Prueba en Java puro del voleibol: bots contra bots en las 3 medidas de cancha y 3 niveles, y un humano de mentira que usa la misma interfaz que el jugador. */
public class VolleyTest {
    static int fails = 0;
    static void check(boolean ok, String msg) { if (!ok) { fails++; System.out.println("FALLO: " + msg); } }

    static class Stats { int rotations, faults, setWins; int points, touchesTotal, spikes, blocks, bumps, sets, frees, serves, outs, nets, rejects; double time; }

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
        if (m.evRotate != 0) st.rotations++;
        if (!m.evFault.isEmpty()) st.faults++;
        if (m.evSetWon != 0) {
            st.setWins++;
            int hi = Math.max(m.scoreA, m.scoreB), lo = Math.min(m.scoreA, m.scoreB);
            check(hi >= m.target() && (hi - lo >= 2 || hi >= m.target() + VolleyMatch.CAP_EXTRA), "marcador de set valido " + m.scoreA + "-" + m.scoreB + " a " + m.target());
        }
        if (!m.evReject.isEmpty()) st.rejects++;
    }

    static boolean finite(VolleyMatch m) {
        for (double d : m.ball) if (Double.isNaN(d) || Double.isInfinite(d)) return false;
        for (double d : m.vel) if (Double.isNaN(d) || Double.isInfinite(d)) return false;
        for (VolleyMatch.P p : m.players) if (Double.isNaN(p.x) || Double.isNaN(p.z) || Double.isNaN(p.y)) return false;
        return true;
    }

    /** Partido entre bots: diffA para el equipo 0 y diffB para el 1 (la dificultad de la partida es una sola, asi que se usa la de A para los dos). */
    static int botMatch(long seed, int n, int diff, Stats st) { return botMatch(seed, n, diff, 1, st); }

    static int botMatch(long seed, int n, int diff, int sets, Stats st) {
        VolleyMatch m = new VolleyMatch(new Random(seed));
        for (int t = 0; t < 2; t++) for (int i = 0; i < n; i++) m.addPlayer(t, false, "bot" + t + i);
        m.start(n, diff, sets);
        double time = 0;
        while (m.state != VolleyMatch.State.IDLE && time < 7200) {
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
        int need = sets / 2 + 1;
        check(m.setsA == need || m.setsB == need, "alguien gano los sets necesarios " + m.setsA + "-" + m.setsB + " de " + sets);
        check(m.setsA + m.setsB <= sets && m.setsA + m.setsB >= need, "cantidad de sets coherente " + m.setsA + "-" + m.setsB);
        return m.winner;
    }

    /** Humano de mentira (equipo 0) con la misma API que el cliente: se mueve hacia donde cae la pelota, salta para rematar y manda HIT/SET. */
    static int humanMatch(long seed, int n, int diff, Stats st) {
        VolleyMatch m = new VolleyMatch(new Random(seed));
        for (int i = 0; i < n; i++) m.addPlayer(0, true, "humano" + i);
        for (int i = 0; i < n; i++) m.addPlayer(1, false, "bot" + i);
        m.start(n, diff, 1);
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
        check(m.state == VolleyMatch.State.IDLE || time >= 3600, "partido con humano termina n=" + n + " tiempo=" + time);
        return m.winner;
    }

    /** Reglas sueltas: falta de saque, calidad de golpe, sets y rotacion. */
    static void rules() {
        // falta de saque: el humano saca desde dentro de la cancha
        VolleyMatch m = new VolleyMatch(new Random(5));
        m.addPlayer(0, true, "h"); m.addPlayer(1, false, "b");
        m.start(1, 1, 3);
        VolleyMatch.P h = m.players.get(0);
        h.x = -2.0; h.z = 0;
        m.humanAct(0, VolleyMatch.HIT, 0, 0, false, 0.7);
        m.tick(0.05);
        check(m.evFault.equals("saque") && m.scoreB == 1 && m.state == VolleyMatch.State.POINT, "falta de saque desde dentro de la cancha: " + m.evFault + " " + m.scoreB);
        m.clearEvents();
        // saque legal desde detras de la linea de fondo
        m = new VolleyMatch(new Random(5));
        m.addPlayer(0, true, "h"); m.addPlayer(1, false, "b");
        m.start(1, 1, 3);
        h = m.players.get(0);
        h.x = -(VolleyMatch.HL[0] + 0.9); h.z = 0;
        m.humanAct(0, VolleyMatch.HIT, 0, 0, false, 0.7);
        m.tick(0.05);
        check(m.state == VolleyMatch.State.RALLY && m.evFault.isEmpty(), "saque legal sale");
        // falta por tiempo
        m = new VolleyMatch(new Random(5));
        m.addPlayer(0, true, "h"); m.addPlayer(1, false, "b");
        m.start(1, 1, 3);
        h = m.players.get(0); h.x = -(VolleyMatch.HL[0] + 0.9);
        for (int i = 0; i < 20 * 21 && m.evFault.isEmpty(); i++) m.tick(0.05);
        check(m.evFault.equals("tiempo"), "falta por tiempo de saque");
        // calidad: pase bien hecho vs mal hecho
        double good = quality(0.3, 0.9, 0), bad = quality(1.45, 2.6, 6.0);
        check(good > 0.8 && bad < 0.5 && good > bad, "calidad buena " + good + " mala " + bad);
        // la fuerza del saque: muy flojo no pasa la red o cae corto; en la zona verde casi siempre entra
        int in = 0, tot = 200;
        for (int i = 0; i < tot; i++) in += serveIn(0.7, i) ? 1 : 0;
        int inWeak = 0, inStrong = 0;
        for (int i = 0; i < tot; i++) { inWeak += serveIn(0.1, i) ? 1 : 0; inStrong += serveIn(1.0, i) ? 1 : 0; }
        check(in > tot * 0.7, "saque en zona verde entra " + in + "/" + tot);
        check(inWeak < tot * 0.5 && inStrong < tot * 0.6, "saque flojo o muy fuerte falla: flojo " + inWeak + " fuerte " + inStrong);
        System.out.printf("reglas: calidad buena=%.2f mala=%.2f, saque entra: verde %d/%d flojo %d fuerte %d%n", good, bad, in, tot, inWeak, inStrong);
    }

    static double quality(double dist, double ballY, double vy) {
        VolleyMatch m = new VolleyMatch(new Random(1));
        m.addPlayer(0, false, "a"); m.addPlayer(1, false, "b");
        m.start(1, 1, 3);
        VolleyMatch.P p = m.players.get(0);
        p.x = -3; p.z = 0; p.y = 0; p.vy = vy; p.vx = 0; p.vz = 0;
        m.ball[0] = -3 - dist; m.ball[1] = ballY; m.ball[2] = 0;
        m.vel[0] = 1; m.vel[1] = -3; m.vel[2] = 0;
        // pasar al estado de juego sin saque
        m.humanAct(0, 0, 0, 0, false);
        try {
            java.lang.reflect.Field f = VolleyMatch.class.getDeclaredField("state");
            f.set(m, VolleyMatch.State.RALLY);
        } catch (Exception e) { throw new RuntimeException(e); }
        m.touches = 0; m.touchTeam = -1;
        m.act(p, VolleyMatch.HIT, 0, 0, false);
        return m.evQuality;
    }

    static boolean serveIn(double power, int seed) {
        VolleyMatch m = new VolleyMatch(new Random(900 + seed));
        m.addPlayer(0, true, "h"); m.addPlayer(1, false, "b");
        m.start(1, 1, 3);
        VolleyMatch.P h = m.players.get(0);
        h.x = -(VolleyMatch.HL[0] + 0.9); h.z = 0;
        m.doServe(h, 0.0, 0.0, power);
        // dónde pasa por la red y dónde cae (sin que nadie la toque)
        double tc = -m.ball[0] / m.vel[0];
        double yc = m.ball[1] + m.vel[1] * tc - 0.5 * VolleyMatch.G * tc * tc;
        double t = m.timeToHeight(VolleyMatch.R);
        double lx = m.ball[0] + m.vel[0] * t, lz = m.ball[2] + m.vel[2] * t;
        return yc > VolleyMatch.NET_TOP + VolleyMatch.R * 0.4 && lx > 0 && lx < VolleyMatch.HL[0] && Math.abs(lz) < VolleyMatch.HW[0];
    }

    public static void main(String[] a) {
        rules();
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
        // partidos al mejor de 3 sets con rotacion
        for (int n = 1; n <= 3; n++) {
            Stats st = new Stats();
            for (int g = 0; g < 2; g++) botMatch(31 + g * 11 + n, n, 1, 3, st);
            System.out.printf("3 sets n=%d: sets jugados=%d rotaciones=%d duracion=%.0fs%n", n, st.setWins, st.rotations, st.time / 2);
            check(st.setWins >= 4, "se juegan sets n=" + n);
            if (n > 1) check(st.rotations > 0, "hay rotaciones n=" + n);
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
