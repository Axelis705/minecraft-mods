package com.mundosdeaxelis.world;

/**
 * Números de los mobs de la dungeon, ligados a {@link BossBalance} para que sigan la misma escala que el jefe: la vida sale
 * del daño por golpe del jugador de referencia (espada B, bonus de set y reliquias, ver {@link LoadoutRef}) y el daño, del multiplicador de golpe de esa etapa. Cada jugador
 * extra en la dungeon suma un 50 % de vida a los mobs y un 12 % de daño (el jefe usa los suyos, ver {@code BossBalance}).
 */
public final class DungeonBalance {
    private DungeonBalance() {
    }

    /** Vida de un mob que aguanta {@code hits} golpes de espada B de la etapa (con 1 jugador). */
    public static float hp(int stage, float hits) {
        return Math.round(LoadoutRef.hit(stage) * hits * BossBalance.hitsRamp(stage));
    }

    /** Multiplicador de vida de los mobs con {@code players} jugadores. */
    public static float hpPlayers(int players) {
        return 1.0F + 0.5F * (Math.max(1, players) - 1);
    }

    /** Daño de un golpe de base {@code base} (pensado para la etapa 1) en la etapa dada con {@code players} jugadores. */
    public static float dmg(int stage, float base, int players) {
        return base * BossBalance.dmgMul(stage) * BossBalance.dmgPlayers(players);
    }
}
