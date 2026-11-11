package com.autoscoleccion.game;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Una prueba de habilidad de la máquina expendedora. Toda la lógica es determinista: con la misma semilla y las mismas teclas (con su
 * tiempo en milisegundos desde que empieza) da siempre el mismo puntaje. El cliente la usa para jugar y dibujar; el servidor repite
 * las teclas que le manda el cliente para calcular el puntaje de verdad.
 */
public abstract class SkillGame {
    public static final int SHIFT = 0;
    public static final int RHYTHM = 1;
    public static final int COMBO = 2;
    public static final int SEQUENCE = 3;
    public static final int TYPES = 4;
    public static final String[] NAME = {"Cambios perfectos", "Ritmo de teclas", "Control de motor", "Secuencia relámpago"};
    public static final String[] HOWTO = {
            "Pulsa la tecla que aparece justo cuando la aguja esté en la franja verde. ¡Seis marchas, cada vez más rápido!",
            "Pulsa D F J K cuando cada nota cruce la línea. Al final llegan dobles.",
            "Mantén W para subir la aguja y suéltala para bajarla. Tiene inercia y el motor da tirones: tenla en la franja verde, que se mueve y se achica.",
            "Repite la secuencia de teclas antes de que se acabe el tiempo. Cada ronda es más larga y más corta de tiempo."};

    // teclas (códigos de GLFW, iguales en el cliente)
    public static final int K_SPACE = 32;
    public static final int K_RIGHT = 262;
    public static final int K_LEFT = 263;
    public static final int K_DOWN = 264;
    public static final int K_UP = 265;
    public static final int K_W = 'W';

    /** Un evento de teclado: tiempo (ms desde el inicio), tecla y si se pulsó (true) o se soltó (false). */
    public record Input(int t, int key, boolean down) {
    }

    /** Un aviso para la pantalla (¡PERFECTO!, Fallo...) con su tiempo y su valor (0 a 1). */
    public record Pop(int t, String text, double value) {
    }

    public final long seed;
    protected final Random rnd;
    protected int now;
    protected final List<Pop> pops = new ArrayList<>();

    protected SkillGame(long seed) {
        this.seed = seed;
        this.rnd = new Random(seed);
    }

    public static SkillGame create(int type, long seed) {
        return switch (type) {
            case RHYTHM -> new RhythmGame(seed);
            case COMBO -> new ComboGame(seed);
            case SEQUENCE -> new SequenceGame(seed);
            default -> new ShiftGame(seed);
        };
    }

    public abstract int type();

    /** Avanza el juego hasta el tiempo {@code t} (ms). Nunca hacia atrás. */
    public abstract void update(int t);

    /** Una tecla en el tiempo {@code t} (antes se llama a update(t)). */
    public abstract void input(int t, int key, boolean down);

    public abstract boolean done();

    /** Puntaje de 0 a 1. */
    public abstract double score();

    /** Duración máxima (ms): pasado esto el servidor da el juego por terminado. */
    public abstract int maxDuration();

    public int now() {
        return now;
    }

    public List<Pop> pops() {
        return pops;
    }

    protected void pop(String text, double value) {
        pops.add(new Pop(now, text, value));
    }

    /** Texto según lo bien que salió un intento (0 a 1). */
    protected static String judge(double v) {
        if (v >= 0.92) {
            return "¡PERFECTO!";
        }
        if (v >= 0.7) {
            return "¡Genial!";
        }
        if (v >= 0.4) {
            return "Bien";
        }
        if (v > 0.0) {
            return "Justo";
        }
        return "Fallo";
    }

    /**
     * Repite una partida con las teclas que mandó el cliente y devuelve el puntaje. Ignora eventos desordenados o fuera de tiempo.
     */
    public static double replay(int type, long seed, List<Input> inputs, int endT) {
        SkillGame g = create(type, seed);
        int last = 0;
        int max = g.maxDuration();
        for (Input in : inputs) {
            if (in.t() < last || in.t() > max) {
                continue;
            }
            last = in.t();
            g.update(in.t());
            if (g.done()) {
                break;
            }
            g.input(in.t(), in.key(), in.down());
        }
        g.update(Math.max(last, Math.min(max, endT)));
        return g.score();
    }

    /** Nombre corto de una tecla para mostrarla. */
    public static String keyName(int k) {
        return switch (k) {
            case K_LEFT -> "←";
            case K_RIGHT -> "→";
            case K_UP -> "↑";
            case K_DOWN -> "↓";
            case K_SPACE -> "ESPACIO";
            default -> k >= 'A' && k <= 'Z' ? String.valueOf((char) k) : "?";
        };
    }
}
