package com.pescacoleccion.pelea;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * La pelea con un pez, sin Minecraft (se puede probar sola y la usan igual el cliente y el servidor).
 * <p>
 * Todo sale de una semilla y de las teclas del jugador con su tiempo en milisegundos: el cliente la juega en vivo y al final manda
 * sus teclas; el servidor la vuelve a jugar con las mismas teclas y decide si el pez se pescó (así nadie puede decir que ganó sin
 * jugar).
 * <ul>
 *     <li><b>Tirón</b>: el pez jala a un lado; A y D empujan la aguja al otro. Fuera de la zona verde el hilo se gasta.</li>
 *     <li><b>Ritmo</b>: llegan flechas al ritmo del pez; acertarlas (← ↑ ↓ →) lo cansa. Las rojas son trampa: no se aprietan.</li>
 *     <li><b>Salto</b>: el pez brinca; Espacio cuando el anillo llega a la marca.</li>
 *     <li><b>Final</b>: sin energía, se saca alternando ↑ y ↓ (o W y S) rápido.</li>
 * </ul>
 */
public final class PeleaSim {
    // teclas
    public static final int LEFT = 0, UP = 1, DOWN = 2, RIGHT = 3, A = 4, D = 5, SPACE = 6, W = 7, S = 8;
    // resultado
    public static final int PELEANDO = 0, PESCADO = 1, HILO_ROTO = 2, ESCAPO = 3;
    // fases
    public static final int TIRON_RITMO = 0, SALTO = 1, FINAL = 2;

    public static final int STEP = 10;            // ms por paso de la aguja
    public static final int JUMP_LEN = 1400;      // ms que dura un salto (el anillo llega a la marca al final)

    /** Cómo pelea el pez (sale de la especie, el tamaño y la caña). */
    public static final class Params {
        public long seed;
        public int bpm = 90;
        /** Patrones de compás: 8 octavos, 'x' = flecha. Se elige uno por compás. */
        public String[] patterns = {"x...x...", "x.x.x...", "x...x.x."};
        public int keys = 2;               // 2 (← →) o 4 (← ↑ ↓ →)
        public float trapChance = 0;       // flechas trampa
        public int jumps = 0;              // saltos en la pelea
        public float pull = 0.5F;          // fuerza del tirón (0..1)
        public int switchMs = 2600;        // cada cuánto cambia de lado (promedio)
        public float stamina = 100;        // energía del pez
        public int approachMs = 1800;      // lo que tarda una flecha en llegar a la línea
        public int durationMs = 60000;     // si no lo sacas en este tiempo, se va
        public int ghostFromMs = -1;       // desde aquí las flechas se desvanecen antes de llegar (legendarios)
        public int burstFromMs = -1;       // desde aquí el pez se pone bravo (más flechas y más tirón)
        // caña y jugador
        public float zone = 0.42F;         // media zona verde de la aguja (0..1)
        public float control = 1.0F;       // fuerza de A/D (1 = igual al tirón más fuerte)
        public float lineHp = 100;         // vida del hilo
        public float damage = 10;          // energía que quita una flecha bien dada
        public int good = 100, perfect = 45;  // ventanas en ms (modo fácil las agranda)
        public float missHp = 4;           // hilo que se pierde por cada flecha fallada
        public float mashGain = 0.09F;     // lo que sube la barra del final con cada tecla alternada
        public boolean inflate;            // pez globo: la zona verde se encoge en la parte media de la pelea
        public boolean rests;              // deja de jalar de vez en cuando (respiro para acomodar la aguja)
        public int mashMs = 4000;          // tiempo para llenarla
    }

    /** Una flecha: cuándo llega a la línea, qué tecla, si es trampa. */
    public static final class Note {
        public final int time, key;
        public final boolean trap, ghost;
        /** 0 = sin juzgar, 1 = perfecta, 2 = bien, 3 = fallada, 4 = trampa evitada, 5 = trampa apretada. */
        public int judged;
        public int judgedAt;

        Note(int time, int key, boolean trap, boolean ghost) {
            this.time = time;
            this.key = key;
            this.trap = trap;
            this.ghost = ghost;
        }
    }

    public final Params p;
    public final List<Note> notes = new ArrayList<>();
    public final int[] jumpTimes;
    private final int[] switchTimes;
    private final float[] switchDir;

    // estado
    public int now;                 // ms simulados
    public float needle, vel;       // aguja (-1..1)
    public float hp, stamina;
    public int combo, maxCombo, perfects, goods, misses, traps;
    public int phase = TIRON_RITMO, result = PELEANDO;
    public int jumpIndex = -1, jumpsOk, jumpsFailed;
    public boolean jumpTried;
    public float reel;              // barra del final (0..1)
    public int finalStart, finalTries, lastMashKey = -1;
    public float hpLost;
    public int lastEventMs;
    private boolean holdA, holdD;
    private int noteCursor;

    public PeleaSim(Params p) {
        this.p = p;
        this.hp = p.lineHp;
        this.stamina = p.stamina;
        Random r = new Random(p.seed);
        // saltos repartidos a lo largo de lo que suele durar la pelea (con un hueco sin flechas alrededor)
        jumpTimes = new int[p.jumps];
        int expected = expectedMs();
        for (int i = 0; i < p.jumps; i++) {
            float f = (i + 1F) / (p.jumps + 1F);
            jumpTimes[i] = Math.max(3500 + i * 2500, Math.round(expected * f) + r.nextInt(800) - 400);
        }
        // flechas
        int beat = 60000 / Math.max(30, p.bpm), eighth = beat / 2;
        int t = 1600;  // la primera llega después de un momento
        int bar = 0;
        while (t < p.durationMs) {
            String pat = p.patterns[r.nextInt(p.patterns.length)];
            boolean burst = p.burstFromMs >= 0 && t >= p.burstFromMs;
            for (int i = 0; i < 8; i++) {
                int nt = t + i * eighth;
                boolean on = pat.charAt(i % pat.length()) == 'x';
                if (!on && burst && i % 2 == 1 && r.nextFloat() < 0.35F) {
                    on = true;   // bravo: notas extra a contratiempo
                }
                if (!on || nearJump(nt)) {
                    continue;
                }
                int key = p.keys >= 4 ? r.nextInt(4) : (r.nextBoolean() ? LEFT : RIGHT);
                boolean trap = bar > 0 && r.nextFloat() < p.trapChance;
                boolean ghost = p.ghostFromMs >= 0 && nt >= p.ghostFromMs && r.nextFloat() < 0.5F;
                notes.add(new Note(nt, key, trap, ghost));
            }
            t += 8 * eighth;
            bar++;
        }
        // cambios de lado del tirón
        List<Integer> st = new ArrayList<>();
        List<Float> sd = new ArrayList<>();
        int tt = 0;
        float dir = r.nextBoolean() ? 1 : -1;
        while (tt < p.durationMs + 5000) {
            st.add(tt);
            sd.add(dir * (0.65F + 0.35F * r.nextFloat()));
            tt += (int) (p.switchMs * (0.45F + r.nextFloat() * 1.1F));
            dir = -dir;
        }
        switchTimes = st.stream().mapToInt(Integer::intValue).toArray();
        switchDir = new float[sd.size()];
        for (int i = 0; i < sd.size(); i++) {
            switchDir[i] = sd.get(i);
        }
    }

    /** Lo que suele durar la pelea jugando bien (flechas por segundo según el ritmo y los patrones, y la energía del pez). */
    public int expectedMs() {
        float perBar = 0;
        for (String pat : p.patterns) {
            for (int i = 0; i < pat.length(); i++) {
                perBar += pat.charAt(i) == 'x' ? 1 : 0;
            }
        }
        perBar /= Math.max(1, p.patterns.length);
        float barMs = 240000F / Math.max(30, p.bpm);
        float rate = perBar / barMs * 1000F;
        float dps = Math.max(0.5F, rate * p.damage * 1.35F);
        return Math.round(1600 + p.stamina / dps * 1000F);
    }

    private boolean nearJump(int t) {
        for (int j : jumpTimes) {
            if (t > j - 900 && t < j + JUMP_LEN + 700) {
                return true;
            }
        }
        return false;
    }

    /** Fuerza del pez en este momento (-1..1 por la fuerza del tirón). */
    public float pullAt(int t) {
        int i = 0;
        while (i + 1 < switchTimes.length && switchTimes[i + 1] <= t) {
            i++;
        }
        float cur = switchDir[i];
        // el cambio de lado no es de golpe: 400 ms para voltearse
        float prev = i > 0 ? switchDir[i - 1] : cur;
        float k = Math.min(1, (t - switchTimes[i]) / 400F);
        float base = prev + (cur - prev) * k;
        float wob = (float) Math.sin(t / 310.0) * 0.18F + (float) Math.sin(t / 97.0) * 0.07F;
        float strength = p.pull * (p.burstFromMs >= 0 && t >= p.burstFromMs ? 1.25F : 1F);
        if (p.rests && t % 7000 > 5600) {
            strength *= 0.15F;   // respiro de 1.4 s cada 7 s
        }
        return (base + wob) * strength;
    }

    /** La zona verde ahora (el pez globo la encoge cuando se infla). */
    public float zone() {
        if (p.inflate) {
            int e = expectedMs();
            if (now > e * 0.3F && now < e * 0.7F) {
                return p.zone * 0.65F;
            }
        }
        return p.zone;
    }

    public boolean inZone() {
        return Math.abs(needle) <= zone();
    }

    /** Avanza la aguja (y todo lo que depende del tiempo) hasta {@code ms}. */
    public void advanceTo(int ms) {
        while (now + STEP <= ms && result == PELEANDO) {
            step();
        }
    }

    private void step() {
        now += STEP;
        float dt = STEP / 1000F;
        // salto en curso
        if (phase == TIRON_RITMO) {
            for (int j = 0; j < jumpTimes.length; j++) {
                if (j > jumpIndex && now >= jumpTimes[j]) {
                    phase = SALTO;
                    jumpIndex = j;
                    jumpTried = false;
                    break;
                }
            }
        }
        if (phase == SALTO && now >= jumpTimes[jumpIndex] + JUMP_LEN + p.good) {
            if (!jumpTried) {
                jumpFail();
            }
            phase = TIRON_RITMO;
        }
        // aguja: el pez jala (menos en el aire o al final), A/D empujan
        float pull = phase == TIRON_RITMO ? pullAt(now) : 0;
        float push = (holdD ? 1 : 0) - (holdA ? 1 : 0);
        float acc = pull * 2.6F + push * 2.6F * p.control - vel * 3.2F - needle * (phase == TIRON_RITMO ? 0.4F : 2.5F);
        vel += acc * dt;
        needle += vel * dt;
        if (needle > 1) {
            needle = 1;
            vel = Math.min(vel, 0);
        } else if (needle < -1) {
            needle = -1;
            vel = Math.max(vel, 0);
        }
        // fuera de la zona verde el hilo se gasta (más cerca del borde, más rápido)
        if (phase != FINAL) {
            float z = zone();
            float out = (Math.abs(needle) - z) / Math.max(0.05F, 1 - z);
            if (out > 0) {
                hurt(dt * (8 + 26 * out * out));
            }
        }
        // flechas que pasaron sin apretar
        for (int i = noteCursor; i < notes.size(); i++) {
            Note n = notes.get(i);
            if (n.time > now - p.good) {
                break;
            }
            if (n.judged == 0) {
                if (n.trap) {
                    n.judged = 4;
                } else {
                    n.judged = 3;
                    missed();
                }
                n.judgedAt = now;
            }
        }
        while (noteCursor < notes.size() && notes.get(noteCursor).judged != 0) {
            noteCursor++;
        }
        // final: la barra baja sola; si no se llena a tiempo, el pez recupera energía y sigue
        if (phase == FINAL) {
            reel = Math.max(0, reel - dt * 0.12F);
            if (now - finalStart > p.mashMs) {
                finalTries++;
                phase = TIRON_RITMO;
                stamina = p.stamina * 0.3F;
                reel = 0;
            }
        }
        if (now >= p.durationMs + 2000 && result == PELEANDO) {
            result = ESCAPO;
        }
    }

    private void hurt(float amount) {
        hp -= amount;
        hpLost += amount;
        if (hp <= 0) {
            hp = 0;
            result = HILO_ROTO;
        }
    }

    private void missed() {
        combo = 0;
        misses++;
        hurt(p.missHp);
        stamina = Math.min(p.stamina, stamina + 3);
    }

    private void jumpFail() {
        jumpsFailed++;
        hurt(14);
        stamina = Math.min(p.stamina, stamina + 10);
        combo = 0;
    }

    private void tire(float amount) {
        stamina -= amount;
        if (stamina <= 0 && phase == TIRON_RITMO) {
            stamina = 0;
            phase = FINAL;
            finalStart = now;
            reel = 0;
            lastMashKey = -1;
        }
    }

    /** Una tecla (apretada o soltada) en el milisegundo {@code ms} (en orden). */
    public void input(int ms, int key, boolean down) {
        ms = Math.max(ms, now);   // nunca hacia atrás (así el cliente y el servidor siempre coinciden)
        advanceTo(ms);
        if (result != PELEANDO) {
            return;
        }
        lastEventMs = ms;
        if (key == A) {
            holdA = down;
            return;
        }
        if (key == D) {
            holdD = down;
            return;
        }
        if (!down) {
            return;
        }
        if (phase == FINAL) {
            int k = key == UP || key == W ? 0 : key == DOWN || key == S ? 1 : -1;
            if (k >= 0 && k != lastMashKey) {
                lastMashKey = k;
                reel += p.mashGain;
                if (reel >= 1) {
                    reel = 1;
                    result = PESCADO;
                }
            }
            return;
        }
        if (key == SPACE) {
            if (phase == SALTO && !jumpTried) {
                jumpTried = true;
                int target = jumpTimes[jumpIndex] + JUMP_LEN;
                int err = Math.abs(ms - target);
                if (err <= p.perfect) {
                    jumpsOk++;
                    tire(p.damage * 5);
                    combo += 2;
                    maxCombo = Math.max(maxCombo, combo);
                } else if (err <= p.good) {
                    jumpsOk++;
                    tire(p.damage * 3.5F);
                    combo++;
                    maxCombo = Math.max(maxCombo, combo);
                } else {
                    jumpFail();
                }
            }
            return;
        }
        if (key > RIGHT) {
            return;
        }
        // la flecha más cercana sin juzgar dentro de la ventana
        Note best = null;
        int bestErr = Integer.MAX_VALUE;
        for (int i = noteCursor; i < notes.size(); i++) {
            Note n = notes.get(i);
            if (n.time > ms + p.good) {
                break;
            }
            if (n.judged != 0) {
                continue;
            }
            int err = Math.abs(n.time - ms);
            if (err <= p.good && err < bestErr && (n.key == key || !n.trap)) {
                best = n;
                bestErr = err;
            }
        }
        if (best == null) {
            combo = 0;   // apretar sin flecha: pierde la racha (para que no sirva apretar todo)
            return;
        }
        best.judgedAt = ms;
        if (best.trap) {
            best.judged = 5;
            traps++;
            combo = 0;
            hurt(10);
            return;
        }
        if (best.key != key) {
            best.judged = 3;
            missed();
            return;
        }
        boolean perf = bestErr <= p.perfect;
        best.judged = perf ? 1 : 2;
        if (perf) {
            perfects++;
        } else {
            goods++;
        }
        combo++;
        maxCombo = Math.max(maxCombo, combo);
        float bonus = 1 + Math.min(0.5F, combo * 0.02F);
        tire(p.damage * (perf ? 1.5F : 1F) * bonus * (inZone() ? 1F : 0.4F));
    }

    /** ¿Se pescó sin errores? (para la estrella de maestría) */
    public boolean flawless() {
        return result == PESCADO && misses == 0 && traps == 0 && jumpsFailed == 0 && hpLost < p.lineHp * 0.1F;
    }

    /** Progreso del salto en curso (0..1: el anillo llega a la marca en 1), o -1 si no hay salto. */
    public float jumpProgress() {
        if (phase != SALTO) {
            return -1;
        }
        return (now - jumpTimes[jumpIndex]) / (float) JUMP_LEN;
    }
}
