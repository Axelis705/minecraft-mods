package com.pescacoleccion.pelea;

/**
 * Cómo pelea un pez según su rareza (la base; cada especie la ajusta con su carácter). Probado con jugadores de mentira en
 * {@code tests/PeleaBots.java}: con la caña inicial un jugador normal saca siempre los comunes y los raros, la cuarta parte de los
 * épicos y casi ningún legendario; con la mejor caña, la cuarta parte de los legendarios.
 */
public final class Niveles {
    private Niveles() {
    }

    /** Base de la pelea para una rareza (0 común ... 4 legendario, 5 mítico). */
    public static PeleaSim.Params base(int rareza, long seed) {
        PeleaSim.Params p = new PeleaSim.Params();
        p.seed = seed;
        switch (Math.max(0, Math.min(5, rareza))) {
            case 0 -> {
                p.bpm = 80; p.keys = 2; p.pull = 0.35F; p.switchMs = 2800; p.stamina = 70; p.approachMs = 1900; p.missHp = 4;
                p.patterns = new String[] {"x...x...", "x.......", "x...x..."};
                p.durationMs = 40000;
            }
            case 1 -> {
                p.bpm = 92; p.keys = 2; p.pull = 0.45F; p.switchMs = 2400; p.stamina = 120; p.approachMs = 1700; p.jumps = 1; p.missHp = 5;
                p.patterns = new String[] {"x...x...", "x.x.x...", "x...x.x."};
                p.durationMs = 45000;
            }
            case 2 -> {
                p.bpm = 104; p.keys = 4; p.pull = 0.55F; p.switchMs = 2100; p.stamina = 230; p.approachMs = 1500; p.jumps = 1; p.missHp = 5;
                p.trapChance = 0.07F;
                p.patterns = new String[] {"x...x...", "x.x.x...", "x...x.x.", "x.x...x."};
                p.durationMs = 55000;
            }
            case 3 -> {
                p.bpm = 116; p.keys = 4; p.pull = 0.70F; p.switchMs = 1800; p.stamina = 400; p.approachMs = 1300; p.jumps = 2; p.missHp = 7;
                p.trapChance = 0.12F; p.burstFromMs = 18000;
                p.patterns = new String[] {"x.x.x...", "x...x.x.", "x.x.x.x.", "xx..x..."};
                p.durationMs = 70000;
            }
            case 4 -> {
                p.bpm = 132; p.keys = 4; p.pull = 0.82F; p.switchMs = 1500; p.stamina = 820; p.approachMs = 1100; p.jumps = 3; p.missHp = 9;
                p.trapChance = 0.15F; p.burstFromMs = 20000; p.ghostFromMs = 15000; p.mashGain = 0.075F;
                p.patterns = new String[] {"x.x.x.x.", "x.xx..x.", "x.x.x.x.", "xx.x.x.x"};
                p.durationMs = 90000;
            }
            default -> {
                p.bpm = 140; p.keys = 4; p.pull = 0.9F; p.switchMs = 1300; p.stamina = 1000; p.approachMs = 1000; p.jumps = 4; p.missHp = 10;
                p.trapChance = 0.18F; p.burstFromMs = 15000; p.ghostFromMs = 10000; p.mashGain = 0.07F;
                p.patterns = new String[] {"x.xx.xx.", "xx.x.xx.", "x.x.x.x.", "xxx.x.x."};
                p.durationMs = 100000;
            }
        }
        return p;
    }

    /** La caña y las mejoras del jugador sobre la base. */
    public static void cana(PeleaSim.Params p, float zone, float control, float lineHp, float damage) {
        p.zone = zone;
        p.control = control;
        p.lineHp = lineHp;
        p.damage = damage;
    }

    /** Modo fácil (opción de la configuración): ventanas más grandes, el pez jala menos y quita menos hilo. */
    public static void facil(PeleaSim.Params p) {
        p.good = Math.round(p.good * 1.5F);
        p.perfect = Math.round(p.perfect * 1.4F);
        p.pull *= 0.6F;
        p.missHp *= 0.6F;
        p.trapChance *= 0.5F;
        p.mashGain *= 1.25F;
    }
}
