package com.pooltable.game;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Una partida de voleibol (sin nada de Minecraft). La cancha mide 2*HL de largo y 2*HW de ancho y la red está en x = 0 (equipo 0 en x &lt; 0, equipo 1 en
 * x &gt; 0; z es el ancho; la altura es y sobre el piso). El tamaño de la cancha depende de cuántos juegan por equipo (1 a 3).
 *
 * Los jugadores humanos son los de verdad de Minecraft: el servidor copia su posición real a {@link P#x}, {@link P#z}, {@link P#y} en cada tick y manda
 * sus golpes con {@link #humanAct}. Los demás jugadores son bots ({@link VolleyAI}). La pelota es virtual: aquí se calcula su vuelo, la red, los toques
 * (máximo 3 por equipo, sin tocarla dos veces seguidas la misma persona en equipos de más de uno), el saque (desde detrás de la línea de fondo, con
 * barra de fuerza), la rotación de quien saca, los puntos por jugada y los sets (al mejor de 3: dos sets a 15 y el tercero a 11, con 2 de ventaja).
 *
 * Cada golpe tiene una calidad de 0 a 1 que depende de qué tan cerca, a qué altura, qué tan quieto y qué tan en el momento se golpeó la pelota: un golpe
 * mal hecho sale impreciso, un remate malo se va a la red y un buen pase deja a un compañero listo para atacar.
 */
public final class VolleyMatch {
    public static final double G = 13.0;
    public static final double R = 0.22;
    public static final double NET_TOP = 2.25;
    /** Puntos de un set normal, del set decisivo, y cuántos puntos más se aceptan para ganar por 2 antes de cortar. */
    public static final int SET_TO = 15;
    public static final int DECIDER_TO = 11;
    public static final int CAP_EXTRA = 3;
    /** Medio largo y medio ancho de la cancha (como las de verdad: 3 contra 3 mide 18 x 9) y distancia de la línea de ataque a la red. */
    public static final double[] HL = {6.5, 8.0, 9.0};
    public static final double[] HW = {3.2, 4.0, 4.5};
    public static final double[] ATK = {2.2, 2.7, 3.0};
    /** Zona de fuerza buena del saque (de 0 a 1). */
    public static final double SERVE_LO = 0.55;
    public static final double SERVE_HI = 0.85;
    public static final double REACH = 1.55;
    public static final double JUMP_V = 8.0;
    public static final double JUMP_G = 28.0;
    public static final double BOT_SPEED_BASE = 3.6;

    public static final int HIT = 0;
    public static final int SET = 1;

    public enum State { IDLE, SERVE, RALLY, POINT, OVER }

    public enum Act { NONE, SERVE, BUMP, SET, SPIKE, BLOCK, FREE }

    /** Un jugador (humano o bot). */
    public static final class P {
        public final int team;
        public int slot;
        public final boolean human;
        public String name = "";
        public double x;
        public double z;
        public double y;
        public double vx;
        public double vz;
        public double vy;
        public double homeX;
        public double homeZ;
        public double cool;
        public double swing;
        public Act lastAct = Act.NONE;
        public double face;
        // golpe pendiente de un humano (se guarda unos instantes por si la pelota aún no llega)
        public int pend = -1;
        public double pendT;
        public double pendFwd;
        public double pendRight;
        public boolean pendDive;
        public double pendPower;
        // IA
        public double aiWait = -1;
        public double jumpIn = -1;
        public boolean wantJump;
        public double reachScale = 1.0;
        public double aiSeenHit = -1;
        public double aiOffX;
        public double aiOffZ;

        public P(int team, int slot, boolean human) {
            this.team = team;
            this.slot = slot;
            this.human = human;
        }
    }

    public final List<P> players = new ArrayList<>();
    public final Random rnd;
    public int n = 1;
    public int diff = 1;
    public State state = State.IDLE;
    public int scoreA;
    public int scoreB;
    public int serving;
    public int serverIdx = -1;
    public double timer;
    public final double[] ball = new double[3];
    public final double[] vel = new double[3];
    public int touches;
    public int touchTeam = -1;
    public int lastToucher = -1;
    public int nextHitter = -1;
    /** Toques seguidos en la jugada actual (después de muchos, los golpes se vuelven menos precisos: así los intercambios no son infinitos). */
    public int rallyTouches;
    /** Segundos de partida y cuándo fue el último golpe (los bots tardan un poco en reaccionar a cada golpe). */
    public double time;
    public double lastHitTime;
    public int winner = -1;
    public int pointWinner = -1;
    /** Quién saca dentro de cada equipo (se pasa al siguiente cada vez que el equipo recupera el saque). */
    private final int[] serveIdx = new int[2];
    /** Sets: al mejor de bestOf (1 o 3), set actual (desde 1) y sets ganados. */
    public int bestOf = 3;
    public int setNo = 1;
    public int setsA;
    public int setsB;
    private boolean setPending;
    /** Calidad (0 a 1) del último golpe y quién lo dio (para dar ánimo o avisar en pantalla). */
    public double evQuality = -1;
    public int evQualityBy = -1;
    /** Falta ("saque", "tiempo") del último tick; el equipo contrario se lleva el punto. */
    public String evFault = "";
    public int evSetWon;
    public boolean evNewSet;
    public int evRotate;

    // eventos del último tick (los borra clearEvents)
    public Act evAct = Act.NONE;
    public int evBy = -1;
    public boolean evNet;
    public boolean evLand;
    public boolean evOut;
    public int evPoint; // 1 = punto del equipo 0, 2 = del equipo 1
    public boolean evServeStart;
    public String evReject = "";
    public int evRejectBy = -1;
    /** Todos los golpes desde la última vez que se borraron los eventos: {tipo (ordinal de Act), quién}. */
    public final List<int[]> hits = new ArrayList<>();

    public VolleyMatch(Random rnd) {
        this.rnd = rnd;
    }

    public double hl() {
        return HL[n - 1];
    }

    public double hw() {
        return HW[n - 1];
    }

    public static double dir(int team) {
        return team == 0 ? 1.0 : -1.0;
    }

    /** Puntos que hay que llegar en el set actual. */
    public int target() {
        return bestOf > 1 && setNo == bestOf ? DECIDER_TO : SET_TO;
    }

    public int score(int team) {
        return team == 0 ? scoreA : scoreB;
    }

    // ------------------------------------------------------------------ preparación

    public void clearPlayers() {
        players.clear();
    }

    public int addPlayer(int team, boolean human, String name) {
        P p = new P(team, 0, human);
        p.name = name;
        players.add(p);
        return players.size() - 1;
    }

    /** Reparte lugares de inicio y empieza el partido. Los jugadores ya deben estar agregados (n por equipo). */
    public void start(int teamSize, int difficulty) {
        start(teamSize, difficulty, 3);
    }

    public void start(int teamSize, int difficulty, int sets) {
        n = Math.max(1, Math.min(3, teamSize));
        diff = Math.max(0, Math.min(2, difficulty));
        bestOf = sets >= 3 ? 3 : 1;
        setNo = 1;
        setsA = 0;
        setsB = 0;
        setPending = false;
        scoreA = 0;
        scoreB = 0;
        winner = -1;
        pointWinner = -1;
        serveIdx[0] = 0;
        serveIdx[1] = 0;
        int[] slotOf = new int[2];
        for (P p : players) {
            // los lugares se asignan en orden de llegada dentro de cada equipo
            slotOf[p.team]++;
        }
        int[] k = new int[2];
        List<P> copy = new ArrayList<>(players);
        players.clear();
        for (P old : copy) {
            P p = new P(old.team, k[old.team]++, old.human);
            p.name = old.name;
            p.x = old.x;
            p.z = old.z;
            p.y = old.y;
            players.add(p);
        }
        for (P p : players) {
            setHome(p);
            if (!p.human) {
                p.x = p.homeX;
                p.z = p.homeZ;
                p.reachScale = VolleyAI.REACH_SCALE[diff];
            }
        }
        serving = 0;
        startServe();
    }

    public void reset() {
        state = State.IDLE;
        scoreA = 0;
        scoreB = 0;
        setsA = 0;
        setsB = 0;
        setNo = 1;
        setPending = false;
        winner = -1;
        touches = 0;
        touchTeam = -1;
        lastToucher = -1;
        nextHitter = -1;
        ball[0] = 0;
        ball[1] = R;
        ball[2] = 0;
        vel[0] = 0;
        vel[1] = 0;
        vel[2] = 0;
        players.clear();
        clearEvents();
    }

    public void clearEvents() {
        evAct = Act.NONE;
        evBy = -1;
        evNet = false;
        evLand = false;
        evOut = false;
        evPoint = 0;
        evServeStart = false;
        evReject = "";
        evRejectBy = -1;
        evQuality = -1;
        evQualityBy = -1;
        evFault = "";
        evSetWon = 0;
        evNewSet = false;
        evRotate = 0;
        hits.clear();
    }

    /** Lugar base de un jugador según su equipo y su puesto. */
    public void setHome(P p) {
        double s = dir(p.team);
        double hl = hl();
        double hw = hw();
        double own = -s;
        double hx;
        double hz;
        switch (n) {
            case 1 -> {
                hx = own * hl * 0.6;
                hz = 0;
            }
            case 2 -> {
                hx = own * hl * (p.slot == 0 ? 0.35 : 0.7);
                hz = (p.slot == 0 ? -1 : 1) * hw * 0.35 * s;
            }
            default -> {
                if (p.slot == 2) {
                    hx = own * hl * 0.72;
                    hz = 0;
                } else {
                    hx = own * hl * 0.32;
                    hz = (p.slot == 0 ? -1 : 1) * hw * 0.55 * s;
                }
            }
        }
        p.homeX = hx;
        p.homeZ = hz;
    }

    // ------------------------------------------------------------------ saque

    private P pickServer(int team) {
        List<Integer> idx = new ArrayList<>();
        for (int i = 0; i < players.size(); i++) {
            if (players.get(i).team == team) {
                idx.add(i);
            }
        }
        if (idx.isEmpty()) {
            return null;
        }
        int pick = idx.get(serveIdx[team] % idx.size());
        serverIdx = pick;
        return players.get(pick);
    }

    /** Dónde se para quien saca (detrás de la línea de fondo de su lado). */
    public double[] serveSpot(int team) {
        double own = -dir(team);
        return new double[] {own * (hl() + 0.9), 0.0};
    }

    private void startServe() {
        state = State.SERVE;
        touches = 0;
        rallyTouches = 0;
        touchTeam = -1;
        lastToucher = -1;
        nextHitter = -1;
        vel[0] = 0;
        vel[1] = 0;
        vel[2] = 0;
        P s = pickServer(serving);
        if (s == null) {
            state = State.IDLE;
            return;
        }
        for (P p : players) {
            p.pend = -1;
            p.aiWait = -1;
            p.jumpIn = -1;
            p.wantJump = false;
            p.cool = 0;
            if (!p.human) {
                setHome(p);
                p.x = p.homeX;
                p.z = p.homeZ;
                p.y = 0;
                p.vy = 0;
            }
        }
        if (!s.human) {
            double[] sp = serveSpot(s.team);
            s.x = sp[0];
            s.z = sp[1];
        }
        timer = s.human ? 20.0 : 1.1 + rnd.nextDouble() * 0.8;
        evServeStart = true;
        holdBall(s);
    }

    /** Rotación: el equipo que recupera el saque pasa al siguiente jugador y los bots se mueven un puesto. */
    private void rotate(int team) {
        serveIdx[team]++;
        evRotate = team + 1;
        if (n > 1) {
            for (P p : players) {
                if (p.team == team) {
                    p.slot = (p.slot + 1) % n;
                    setHome(p);
                }
            }
        }
    }

    /** Falta del que saca: la pelota no sale y el punto es del otro equipo. */
    private void fault(String why, int team) {
        evFault = why;
        holdBall(players.get(serverIdx));
        awardPoint(1 - team);
    }

    private void holdBall(P s) {
        double d = dir(s.team);
        ball[0] = s.x + d * 0.45;
        ball[1] = s.y + 1.35;
        ball[2] = s.z;
    }

    // ------------------------------------------------------------------ avance

    /** Avanza la partida dt segundos (el servidor llama con 0,05). Los eventos se van juntando hasta que quien los lee llama a {@link #clearEvents()}. */
    public void tick(double dt) {
        if (state == State.IDLE) {
            return;
        }
        time += dt;
        for (P p : players) {
            p.cool = Math.max(0, p.cool - dt);
            p.swing = Math.max(0, p.swing - dt);
            if (!p.human) {
                integrateBot(p, dt);
            }
        }
        switch (state) {
            case SERVE -> tickServe(dt);
            case RALLY -> tickRally(dt);
            case POINT -> {
                for (P p : players) {
                    if (!p.human) {
                        VolleyAI.goHome(this, p, dt);
                    }
                }
                timer -= dt;
                if (timer <= 0) {
                    if (setPending) {
                        // empieza el siguiente set: saca primero el equipo que no sacó al inicio del anterior
                        setPending = false;
                        setNo++;
                        scoreA = 0;
                        scoreB = 0;
                        serving = (setNo % 2 == 1) ? 0 : 1;
                        evNewSet = true;
                    } else {
                        boolean sideOut = pointWinner != serving;
                        serving = pointWinner;
                        if (sideOut) {
                            rotate(serving);
                        }
                    }
                    startServe();
                }
            }
            case OVER -> {
                timer -= dt;
                if (timer <= 0) {
                    state = State.IDLE;
                }
            }
            default -> {
            }
        }
    }

    private void tickServe(double dt) {
        P s = players.get(serverIdx);
        if (!s.human) {
            double[] sp = serveSpot(s.team);
            s.x = sp[0];
            s.z = sp[1];
        }
        holdBall(s);
        for (P p : players) {
            if (!p.human && p != s) {
                VolleyAI.goHome(this, p, dt);
            }
        }
        timer -= dt;
        if (s.human && s.pend >= 0) {
            int kind = s.pend;
            s.pend = -1;
            if (kind == HIT) {
                doServe(s, s.pendFwd, s.pendRight, s.pendPower);
                return;
            }
        }
        if (timer <= 0) {
            if (s.human) {
                fault("tiempo", s.team);
            } else {
                VolleyAI.serve(this, s);
            }
        }
    }

    private void tickRally(double dt) {
        // golpes pendientes de los humanos
        for (int i = 0; i < players.size(); i++) {
            P p = players.get(i);
            if (p.human && p.pend >= 0) {
                p.pendT -= dt;
                if (reachable(p, p.pendDive)) {
                    int kind = p.pend;
                    p.pend = -1;
                    perform(i, p, kind, p.pendFwd, p.pendRight, p.pendDive);
                } else if (p.pendT <= 0) {
                    p.pend = -1;
                }
            }
        }
        for (P p : players) {
            if (!p.human) {
                VolleyAI.update(this, p, dt);
            }
        }
        stepBall(dt);
    }

    private void integrateBot(P p, double dt) {
        if (p.wantJump && p.y <= 0.0 && p.vy == 0) {
            p.vy = JUMP_V;
            p.wantJump = false;
        }
        if (p.y > 0 || p.vy > 0) {
            p.y += p.vy * dt;
            p.vy -= JUMP_G * dt;
            if (p.y <= 0) {
                p.y = 0;
                p.vy = 0;
            }
        }
    }

    // ------------------------------------------------------------------ física de la pelota

    private void stepBall(double dt) {
        int sub = 4;
        double h = dt / sub;
        for (int i = 0; i < sub && state == State.RALLY; i++) {
            vel[1] -= G * h;
            double nx = ball[0] + vel[0] * h;
            double ny = ball[1] + vel[1] * h;
            double nz = ball[2] + vel[2] * h;
            // la red (en x = 0), de las bandas a las bandas
            if ((ball[0] <= 0 && nx > 0 || ball[0] >= 0 && nx < 0) && Math.abs(vel[0]) > 1e-9) {
                double t = -ball[0] / vel[0];
                double yc = ball[1] + vel[1] * t;
                double zc = ball[2] + vel[2] * t;
                if (Math.abs(zc) <= hw() + 0.3 && yc < NET_TOP + R * 0.4) {
                    double side = ball[0] < 0 || (ball[0] == 0 && vel[0] > 0) ? -1.0 : 1.0;
                    nx = side * (R + 0.02);
                    vel[0] = -vel[0] * 0.3;
                    vel[1] *= 0.7;
                    vel[2] *= 0.8;
                    evNet = true;
                }
            }
            ball[0] = nx;
            ball[1] = ny;
            ball[2] = nz;
            if (ball[1] <= R && vel[1] < 0) {
                ball[1] = R;
                landed();
            } else if (Math.abs(ball[0]) > 40 || Math.abs(ball[2]) > 40) {
                landed();
            }
        }
    }

    private void landed() {
        evLand = true;
        double x = ball[0];
        double z = ball[2];
        int side = x < 0 ? 0 : 1;
        boolean in = Math.abs(x) <= hl() && Math.abs(z) <= hw();
        int win;
        if (in) {
            win = 1 - side;
        } else {
            evOut = true;
            win = touchTeam >= 0 ? 1 - touchTeam : 1 - side;
        }
        vel[0] = 0;
        vel[1] = 0;
        vel[2] = 0;
        awardPoint(win);
    }

    private void awardPoint(int team) {
        pointWinner = team;
        evPoint = team + 1;
        if (team == 0) {
            scoreA++;
        } else {
            scoreB++;
        }
        int tgt = target();
        int a = Math.max(scoreA, scoreB);
        int b = Math.min(scoreA, scoreB);
        vel[0] = 0;
        vel[1] = 0;
        vel[2] = 0;
        if (a >= tgt && a - b >= 2 || a >= tgt + CAP_EXTRA) {
            int w = scoreA > scoreB ? 0 : 1;
            if (w == 0) {
                setsA++;
            } else {
                setsB++;
            }
            evSetWon = w + 1;
            int need = bestOf / 2 + 1;
            if (setsA >= need || setsB >= need) {
                winner = w;
                state = State.OVER;
                timer = 6.0;
            } else {
                setPending = true;
                state = State.POINT;
                timer = 4.5;
            }
        } else {
            state = State.POINT;
            timer = 2.2;
        }
    }

    // ------------------------------------------------------------------ golpes

    /** ¿La pelota está al alcance de las manos de este jugador? */
    public boolean reachable(P p, boolean dive) {
        double dx = ball[0] - p.x;
        double dz = ball[2] - p.z;
        double h = Math.hypot(dx, dz);
        double reach = REACH * p.reachScale + (dive ? 0.8 : 0.0);
        double top = p.y + 2.1 + (p.y > 0.25 ? 0.9 : 0.0);
        return h <= reach && ball[1] >= p.y - 0.15 && ball[1] <= top;
    }

    /** Un humano pide golpear: kind = HIT o SET; aimFwd (+1 = adelante/profundo, -1 = corto) y aimRight (+1 = a su derecha) vienen de WASD. */
    public void humanAct(int idx, int kind, double aimFwd, double aimRight, boolean dive) {
        humanAct(idx, kind, aimFwd, aimRight, dive, 0.7);
    }

    /** Igual, con la fuerza (0 a 1) que eligió con la barra al sacar. */
    public void humanAct(int idx, int kind, double aimFwd, double aimRight, boolean dive, double power) {
        if (idx < 0 || idx >= players.size()) {
            return;
        }
        P p = players.get(idx);
        if (!p.human || state == State.IDLE || state == State.OVER || state == State.POINT) {
            return;
        }
        if (p.cool > 0 && state == State.RALLY) {
            return;
        }
        p.pend = kind;
        // un instante de margen: hay que pulsar cuando la pelota ya está a la mano, no antes
        p.pendT = 0.12;
        p.pendFwd = clamp(aimFwd, -1, 1);
        p.pendRight = clamp(aimRight, -1, 1);
        p.pendDive = dive;
        p.pendPower = clamp(power, 0, 1);
        if (state == State.RALLY && reachable(p, dive)) {
            p.pend = -1;
            perform(idx, p, kind, p.pendFwd, p.pendRight, dive);
        }
    }

    /** Un bot o un humano ya decidió: aplica el golpe en el contexto actual. Devuelve lo que se hizo. */
    public Act act(P p, int kind, double aimFwd, double aimRight, boolean dive) {
        int idx = players.indexOf(p);
        if (state == State.SERVE) {
            return Act.NONE;
        }
        return perform(idx, p, kind, aimFwd, aimRight, dive);
    }

    /**
     * Qué tan bien se golpeó (0 a 1): cerca de la pelota, a la altura buena para ese golpe (pase abajo, colocación arriba de la cabeza, remate en lo alto),
     * quieto (para recibir hay que frenar), con una pelota no muy rápida y, en el remate, cerca de lo más alto del salto.
     */
    private double quality(P p, Act act, double d, boolean dive) {
        double reach = REACH * p.reachScale + (dive ? 0.8 : 0.0);
        double sweet = 0.6;
        double qd = d <= sweet ? 1.0 : 1.0 - (d - sweet) / Math.max(0.1, reach - sweet) * 0.8;
        qd = clamp(qd, 0.2, 1.0);
        double lo;
        double hi;
        switch (act) {
            case SET -> {
                lo = 1.6;
                hi = 2.4;
            }
            case SPIKE -> {
                lo = 2.6;
                hi = 3.6;
            }
            case BLOCK -> {
                lo = NET_TOP - 0.2;
                hi = 3.4;
            }
            default -> {
                lo = 0.35;
                hi = 1.3;
            }
        }
        double dh = ball[1] < lo ? lo - ball[1] : Math.max(0.0, ball[1] - hi);
        double qh = 1.0 - Math.min(1.0, dh / 0.8) * 0.9;
        double q;
        if (act == Act.SPIKE || act == Act.BLOCK) {
            double qt = 1.0 - clamp((Math.abs(p.vy) - 1.0) / 5.0, 0.0, 0.7);
            q = 0.4 * qd + 0.35 * qh + 0.25 * qt;
        } else {
            double speed = Math.hypot(p.vx, p.vz);
            double qm = 1.0 - clamp((speed - 1.2) / 6.0, 0.0, 0.45);
            double incoming = Math.sqrt(vel[0] * vel[0] + vel[1] * vel[1] + vel[2] * vel[2]);
            double qs = 1.0 - clamp((incoming - 11.0) / 14.0, 0.0, 0.4);
            q = 0.35 * qd + 0.3 * qh + 0.2 * qm + 0.15 * qs;
        }
        if (dive) {
            q *= 0.75;
        }
        return clamp(q, 0.05, 1.0);
    }

    private Act perform(int idx, P p, int kind, double aimFwd, double aimRight, boolean dive) {
        if (state != State.RALLY) {
            return Act.NONE;
        }
        int ballSide = ball[0] < 0 ? 0 : 1;
        boolean block = ballSide != p.team && Math.abs(ball[0]) < 1.7 && p.y > 0.25 && ball[1] > NET_TOP - 0.45;
        if (!block) {
            if (touchTeam == p.team && touches >= 3) {
                evReject = "toques";
                evRejectBy = idx;
                return Act.NONE;
            }
            if (n > 1 && touchTeam == p.team && touches > 0 && lastToucher == idx) {
                evReject = "doble";
                evRejectBy = idx;
                return Act.NONE;
            }
            if (ballSide != p.team) {
                evReject = "lado";
                evRejectBy = idx;
                return Act.NONE;
            }
        }
        double s = dir(p.team);
        double hw = hw();
        double hl = hl();
        double d = Math.hypot(ball[0] - p.x, ball[2] - p.z);
        double sigma;
        double tx;
        double tz;
        double flight;
        boolean cross = false;
        Act act;
        if (block) {
            act = Act.BLOCK;
        } else if (kind == SET) {
            act = Act.SET;
        } else if (p.y > 0.3 && ball[1] >= 2.2 && Math.abs(ball[0]) < ATK[n - 1] + 0.8) {
            act = Act.SPIKE;
        } else if (touchTeam != p.team || touches == 0) {
            act = aimFwd > 0.6 ? Act.FREE : Act.BUMP;
        } else {
            act = Act.FREE;
        }
        double q = quality(p, act, d, dive);
        double bad = 1.0 - q;
        switch (act) {
            case BLOCK -> {
                double bs = ball[0] < 0 ? -1 : 1;
                tx = bs * (1.4 + rnd.nextDouble() * 1.6);
                tz = clamp(ball[2] * 0.6, -hw + 0.4, hw - 0.4);
                flight = 0.5;
                sigma = 0.2 + bad * 0.8;
            }
            case SET -> {
                double[] mate = mateSpot(p, idx);
                tx = mate[0];
                tz = mate[1];
                flight = 1.15 + 0.2 * bad;
                sigma = 0.1 + bad * 1.0;
                nextHitter = (int) mate[2];
            }
            case SPIKE -> {
                tx = s * (hl * 0.35 + hl * 0.55 * (aimFwd + 1.0) / 2.0);
                tz = clamp(aimRight * s * hw * 0.8, -hw + 0.35, hw - 0.35);
                flight = 0.45 + bad * 0.4;
                sigma = 0.2 + bad * 1.6;
                nextHitter = -1;
                if (q < 0.35 && rnd.nextDouble() < (0.35 - q) * 2.0) {
                    // remate mal pegado: se queda en la red / en tu lado
                    tx = -s * (0.3 + rnd.nextDouble() * 0.6);
                    flight = 0.4;
                }
            }
            case BUMP -> {
                double[] mate = mateSpot(p, idx);
                tx = mate[0];
                tz = mate[1];
                flight = 1.3 + 0.15 * bad;
                sigma = 0.15 + bad * 1.5;
                nextHitter = (int) mate[2];
            }
            default -> {
                tx = s * (hl * 0.3 + hl * 0.6 * (aimFwd + 1.0) / 2.0);
                tz = clamp(aimRight * s * hw * 0.75, -hw + 0.4, hw - 0.4);
                flight = p.human ? 1.55 : 1.1;
                sigma = 0.25 + bad * 1.4;
                nextHitter = -1;
            }
        }
        if (!p.human) {
            sigma += VolleyAI.ERR[diff];
        }
        sigma += 0.08 * Math.max(0, rallyTouches - 6);
        rallyTouches++;
        tx += rnd.nextGaussian() * sigma;
        tz += rnd.nextGaussian() * sigma;
        if (q < 0.3 && act != Act.SPIKE && act != Act.BLOCK) {
            // pelota "chueca": se va lejos de donde se quería
            tx += rnd.nextGaussian() * 1.5;
            tz += rnd.nextGaussian() * 1.5;
        }
        cross = Math.signum(tx) != Math.signum(ball[0]);
        launch(tx, tz, flight, cross && act != Act.BLOCK);
        // anotar el toque
        if (act == Act.BLOCK) {
            touchTeam = p.team;
            touches = 0;
        } else if (touchTeam != p.team) {
            touchTeam = p.team;
            touches = 1;
        } else {
            touches++;
        }
        lastToucher = idx;
        p.cool = 0.35;
        p.swing = 0.35;
        p.lastAct = act;
        p.pend = -1;
        lastHitTime = time;
        evAct = act;
        evBy = idx;
        evQuality = q;
        evQualityBy = idx;
        hits.add(new int[] {act.ordinal(), idx});
        return act;
    }

    /** Saque con fuerza por defecto (para los bots: la que sacan según su nivel). */
    public void doServe(P s, double aimFwd, double aimRight) {
        double power = 0.7;
        if (!s.human) {
            power = clamp(0.7 + rnd.nextGaussian() * VolleyAI.SERVE_ERR[diff] * 0.25, 0.05, 1.0);
        }
        doServe(s, aimFwd, aimRight, power);
    }

    /**
     * Saque del jugador del turno (el humano lo pide con un clic mantenido y la barra de fuerza, el bot por IA). Hay que sacar desde detrás de la línea de
     * fondo; la fuerza buena es de {@link #SERVE_LO} a {@link #SERVE_HI}: más flojo se queda corto o en la red, más fuerte se pasa de la línea.
     */
    public void doServe(P s, double aimFwd, double aimRight, double power) {
        double d = dir(s.team);
        double hl = hl();
        double hw = hw();
        if (s.human && s.x * (-d) < hl - 0.1) {
            fault("saque", s.team);
            return;
        }
        double pw = clamp(power, 0.0, 1.0);
        double depth = 0.25 + 0.8 * pw + 0.16 * clamp(aimFwd, -1, 1);
        double tx = d * hl * depth;
        double tz = clamp(clamp(aimRight, -1, 1) * d * hw * 0.7, -hw + 0.3, hw - 0.3);
        double off = pw < SERVE_LO ? SERVE_LO - pw : Math.max(0.0, pw - SERVE_HI);
        double sigma = 0.15 + 1.8 * off;
        tx += rnd.nextGaussian() * sigma;
        tz += rnd.nextGaussian() * sigma * 0.6;
        double fl = 1.45;
        boolean cross = true;
        if (pw < 0.3 && rnd.nextDouble() < (0.3 - pw) * 3.3) {
            // saque muy flojo: se queda en la red
            tx = d * 0.5;
            fl = 0.8;
            cross = false;
        }
        state = State.RALLY;
        holdBall(s);
        launch(tx, tz, fl, cross);
        touchTeam = s.team;
        touches = 1;
        lastToucher = serverIdx;
        nextHitter = -1;
        s.swing = 0.4;
        s.lastAct = Act.SERVE;
        lastHitTime = time;
        evAct = Act.SERVE;
        evBy = serverIdx;
        evQuality = pw >= SERVE_LO && pw <= SERVE_HI ? 1.0 : (off < 0.15 ? 0.6 : 0.2);
        evQualityBy = serverIdx;
        hits.add(new int[] {Act.SERVE.ordinal(), serverIdx});
    }

    /** Dónde mandar un pase: cerca de la red, donde está el compañero más adelantado (o el propio jugador si juega solo). */
    private double[] mateSpot(P p, int idx) {
        double own = -dir(p.team);
        double nearX = own * Math.max(1.5, hl() * 0.2);
        double hw = hw();
        int best = -1;
        double bestD = 1e9;
        for (int i = 0; i < players.size(); i++) {
            P q = players.get(i);
            if (i == idx || q.team != p.team) {
                continue;
            }
            double dd = Math.abs(q.x - nearX) + (q.human ? -0.2 : 0);
            if (dd < bestD) {
                bestD = dd;
                best = i;
            }
        }
        if (best < 0) {
            return new double[] {own * 2.0, clamp(p.z, -hw + 0.6, hw - 0.6), idx};
        }
        P q = players.get(best);
        return new double[] {nearX, clamp(q.z + q.vz * 0.3, -hw + 0.6, hw - 0.6), best};
    }

    /** Pone en {@link #vel} lo necesario para que la pelota llegue a (tx, R, tz) en flight segundos; si tiene que pasar la red, la sube hasta librarla. */
    private void launch(double tx, double tz, double flight, boolean cross) {
        double t = flight;
        double vx = 0;
        double vy = 0;
        double vz = 0;
        for (int i = 0; i < 40; i++) {
            vx = (tx - ball[0]) / t;
            vz = (tz - ball[2]) / t;
            vy = (R - ball[1] + 0.5 * G * t * t) / t;
            if (!cross || Math.abs(vx) < 1e-6) {
                break;
            }
            double tc = -ball[0] / vx;
            if (tc <= 0) {
                break;
            }
            double yc = ball[1] + vy * tc - 0.5 * G * tc * tc;
            if (yc >= NET_TOP + R + 0.3 || t > 3.4) {
                break;
            }
            t += 0.08;
        }
        double hs = Math.hypot(vx, vz);
        double max = 22.0;
        if (hs > max) {
            vx *= max / hs;
            vz *= max / hs;
        }
        vel[0] = vx;
        vel[1] = vy;
        vel[2] = vz;
    }

    /** Segundos hasta que la pelota (bajando) llegue a la altura h, o -1 si no llega. */
    public double timeToHeight(double h) {
        double disc = vel[1] * vel[1] + 2 * G * (ball[1] - h);
        if (disc < 0) {
            return -1;
        }
        return (vel[1] + Math.sqrt(disc)) / G;
    }

    public static double clamp(double v, double lo, double hi) {
        return Math.max(lo, Math.min(hi, v));
    }
}
