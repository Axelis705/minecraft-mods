package com.mundosdeaxelis.lock;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Random;

/**
 * Las reglas del minijuego de la cerradura (sin nada de Minecraft, así se prueba sola). Una aguja gira alrededor de la cerradura y
 * hay clavijas rojas en 12 posiciones; hay que pulsar justo cuando la aguja pasa por una clavija para abrirla. Si fallas pierdes un
 * corazón; si se acaba el tiempo o los corazones, pierdes. Ángulos en grados: 0 = arriba, crece hacia la derecha.
 *
 * <p>Modificadores según el nivel (1 a 5): <b>Carrusel</b> (cada 5 s el campo gira 90° al azar), <b>Clavija falsa</b> (una clavija
 * que al pulsarla vuelve a cerrar todas las abiertas, y cambia de sitio con cada pulsación) y <b>Cambios de humor</b> (la aguja
 * cambia de sentido tras cada acierto).
 */
public final class LockGame {
    public enum Mod {MOOD, CAROUSEL, FAKE}

    public enum State {PLAYING, WON, LOST}

    public enum Event {PIN, MISS, FAKE, WIN, LOSE, CAROUSEL}

    public static final int SLOTS = 12;
    public static final double SLOT_DEG = 360.0 / SLOTS;
    private static final int[] PINS = {5, 6, 7, 8, 9};
    private static final int[] LIVES = {5, 5, 5, 4, 4};
    private static final double[] SPEED = {105, 120, 135, 150, 165};
    private static final double[] TOL = {10.0, 9.0, 8.5, 8.0, 7.5};
    public static final double CAROUSEL_EVERY = 5.0;
    private static final double CAROUSEL_DEG_PER_SEC = 260.0;

    public final int level;
    public final EnumSet<Mod> mods;
    public final boolean[] hasPin = new boolean[SLOTS];
    public final boolean[] opened = new boolean[SLOTS];
    public int fakeSlot = -1;
    public final int totalPins;
    public final double tolerance;
    public final double baseSpeed;
    public double speed;
    public double needle = 0.0;
    public int dir = 1;
    /** Giro actual del campo (se anima) y al que va. */
    public double fieldRot = 0.0;
    public double fieldTarget = 0.0;
    private double carouselIn = CAROUSEL_EVERY;
    public int lives;
    public final int maxLives;
    public final double timeLimit;
    public double timeLeft;
    public State state = State.PLAYING;
    public final List<Event> events = new ArrayList<>();
    private final Random rnd;

    public static int clampLevel(int level) {
        return Math.max(1, Math.min(5, level));
    }

    public static EnumSet<Mod> modsFor(int level) {
        return switch (clampLevel(level)) {
            case 1 -> EnumSet.noneOf(Mod.class);
            case 2 -> EnumSet.of(Mod.MOOD);
            case 3 -> EnumSet.of(Mod.CAROUSEL);
            case 4 -> EnumSet.of(Mod.MOOD, Mod.FAKE);
            default -> EnumSet.of(Mod.MOOD, Mod.CAROUSEL, Mod.FAKE);
        };
    }

    /** Velocidad relativa de la aguja respecto al nivel 1 (porcentaje, para mostrar). */
    public static int speedPct(int level) {
        return (int) Math.round(SPEED[clampLevel(level) - 1] / SPEED[0] * 100.0);
    }

    public LockGame(int levelIn, long seed) {
        this.level = clampLevel(levelIn);
        this.mods = modsFor(level);
        this.rnd = new Random(seed);
        this.totalPins = PINS[level - 1];
        this.tolerance = TOL[level - 1];
        this.baseSpeed = SPEED[level - 1];
        this.speed = baseSpeed;
        this.maxLives = LIVES[level - 1];
        this.lives = maxLives;
        this.timeLimit = Math.round(8.0 + 2.6 * totalPins);
        this.timeLeft = timeLimit;
        List<Integer> slots = new ArrayList<>();
        for (int i = 0; i < SLOTS; i++) {
            slots.add(i);
        }
        java.util.Collections.shuffle(slots, rnd);
        for (int i = 0; i < totalPins; i++) {
            hasPin[slots.get(i)] = true;
        }
        this.needle = rnd.nextInt(360);
        this.dir = rnd.nextBoolean() ? 1 : -1;
        if (mods.contains(Mod.FAKE)) {
            moveFake();
        }
    }

    public int openedCount() {
        int n = 0;
        for (int i = 0; i < SLOTS; i++) {
            if (opened[i]) {
                n++;
            }
        }
        return n;
    }

    private void moveFake() {
        List<Integer> free = new ArrayList<>();
        for (int i = 0; i < SLOTS; i++) {
            if (!hasPin[i] && i != fakeSlot) {
                free.add(i);
            }
        }
        fakeSlot = free.isEmpty() ? -1 : free.get(rnd.nextInt(free.size()));
    }

    public static double wrap(double a) {
        a %= 360.0;
        if (a < 0) {
            a += 360.0;
        }
        return a;
    }

    /** Diferencia angular más corta (0..180). */
    public static double diff(double a, double b) {
        double d = Math.abs(wrap(a) - wrap(b));
        return d > 180.0 ? 360.0 - d : d;
    }

    /** Ángulo (en el campo) al que apunta la aguja ahora. */
    public double tip() {
        return wrap(needle - fieldRot);
    }

    public void update(double dt) {
        if (state != State.PLAYING) {
            return;
        }
        dt = Math.min(dt, 0.1);
        timeLeft -= dt;
        if (timeLeft <= 0) {
            timeLeft = 0;
            lose();
            return;
        }
        needle = wrap(needle + dir * speed * dt);
        if (mods.contains(Mod.CAROUSEL)) {
            carouselIn -= dt;
            if (carouselIn <= 0) {
                carouselIn += CAROUSEL_EVERY;
                fieldTarget += rnd.nextBoolean() ? 90.0 : -90.0;
                events.add(Event.CAROUSEL);
            }
        }
        double d = fieldTarget - fieldRot;
        if (Math.abs(d) > 1e-6) {
            double step = CAROUSEL_DEG_PER_SEC * dt;
            fieldRot += Math.abs(d) <= step ? d : Math.signum(d) * step;
        }
    }

    /** Tiempo hasta el siguiente giro del campo (para mostrar), o -1 si no hay carrusel. */
    public double carouselIn() {
        return mods.contains(Mod.CAROUSEL) ? carouselIn : -1;
    }

    private void lose() {
        state = State.LOST;
        events.add(Event.LOSE);
    }

    /** El jugador pulsa. Devuelve lo que pasó. */
    public Event click() {
        if (state != State.PLAYING) {
            return null;
        }
        double t = tip();
        Event result = Event.MISS;
        int hit = -1;
        for (int i = 0; i < SLOTS; i++) {
            if ((hasPin[i] || i == fakeSlot) && diff(t, i * SLOT_DEG) <= tolerance) {
                hit = i;
                break;
            }
        }
        if (hit >= 0 && hasPin[hit] && !opened[hit]) {
            opened[hit] = true;
            result = Event.PIN;
            if (mods.contains(Mod.MOOD)) {
                dir = -dir;
            }
            speed *= 1.03;
            if (openedCount() >= totalPins) {
                state = State.WON;
                events.add(result);
                events.add(Event.WIN);
                if (mods.contains(Mod.FAKE)) {
                    moveFake();
                }
                return result;
            }
        } else if (hit >= 0 && hit == fakeSlot) {
            for (int i = 0; i < SLOTS; i++) {
                opened[i] = false;
            }
            result = Event.FAKE;
        } else {
            lives--;
            if (lives <= 0) {
                lives = 0;
                events.add(Event.MISS);
                lose();
                if (mods.contains(Mod.FAKE)) {
                    moveFake();
                }
                return Event.MISS;
            }
        }
        events.add(result);
        if (mods.contains(Mod.FAKE)) {
            moveFake();
        }
        return result;
    }
}
