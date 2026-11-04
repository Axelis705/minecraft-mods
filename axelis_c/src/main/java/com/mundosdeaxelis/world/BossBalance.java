package com.mundosdeaxelis.world;

import com.mundosdeaxelis.gear.GearStats;
import net.minecraft.world.damagesource.CombatRules;

/**
 * Dificultad de los jefes, guiada SIEMPRE por lo mejor que da la caja de su etapa (ver {@link LoadoutRef}: set B, espada B,
 * mascotas, reliquias y bonus de set). La etapa N tiene los sets A = 2N-1 y B = 2N. La vida sale del daño por golpe del
 * jugador de referencia (unos 72 golpes) y el daño del jefe se calcula para que su golpe fuerte quite el 25 % de la vida
 * máxima del jugador de referencia tras toda su protección. Quien llegue con el set A o con menos (sin mascotas ni reliquias
 * buenas) lo tiene más difícil; quien tenga el B de la caja o mejor lo tiene como se diseñó. Los ataques de los jefes están
 * escritos para la etapa 1 y se multiplican por {@link #dmgMul}.
 */
public final class BossBalance {
    /** Golpe fuerte de referencia de la etapa 1, antes de armadura. */
    private static final float REF_RAW = 9.0F;
    /** Después de armadura el golpe fuerte quita esta parte de los 20 de vida (25 %). */
    private static final float TARGET_AFTER_ARMOR = 5.0F;

    /**
     * Desde la etapa 2: parte de la vida máxima del jugador de referencia que quita el golpe fuerte del jefe (la etapa 1 conserva lo
     * que se probó: ~13 %). 17 % = unos 6 golpes fuertes para caer sin comida ni esquivar.
     */
    private static final float STRONG_FRACTION = 0.17F;

    private BossBalance() {
    }

    /** Golpes de espada de referencia que hay que dar de más (jefe y mobs) respecto a la etapa 1: +15 % por etapa hasta la 4. */
    public static float hitsRamp(int stage) {
        return 1.0F + 0.15F * Math.min(3, Math.max(0, stage - 1));
    }

    /** Puesto del set B de la caja de la etapa (2, 4, ... 16). */
    private static int bRank(int stage) {
        return Math.max(2, Math.min(16, 2 * stage));
    }

    /** Daño de la espada del set B de la caja de esta etapa. */
    public static float swordAvg(int stage) {
        return GearStats.sword(bRank(stage));
    }

    /** Vida del jefe de la etapa (1..8). */
    public static float hp(int stage) {
        return Math.round(LoadoutRef.hit(stage) * 72.0F * hitsRamp(stage) / 10.0F) * 10.0F;
    }

    /** Daño que recibe el jugador de referencia de la etapa (set B, mascotas, reliquias...) por un golpe de {@code raw}. */
    private static float afterArmor(int stage, float raw) {
        return LoadoutRef.taken(stage, bRank(stage), raw);
    }

    /** Lo mismo con un set de otro puesto (para comparar el A con el B), conservando mascotas y reliquias de la etapa. */
    private static float afterArmorRank(int stage, int rankInt, float raw) {
        return LoadoutRef.taken(stage, rankInt, raw);
    }

    /**
     * Golpe sin armadura que hace falta para quitar el 25 % de la vida máxima al jugador de referencia de la etapa.
     * {@code full = false} usa solo el set (sin mascotas, reliquias, esquiva ni vida extra): la base con la que se probó el jefe 1.
     */
    private static float rawFor(int stage, boolean full) {
        float target = TARGET_AFTER_ARMOR / 20.0F * (full ? LoadoutRef.maxHp(bRank(stage)) : 20.0F);
        float lo = 1.0F;
        float hi = 5000.0F;
        for (int i = 0; i < 50; i++) {
            float mid = (lo + hi) / 2.0F;
            if (LoadoutRef.taken(stage, bRank(stage), mid, full) < target) {
                lo = mid;
            } else {
                hi = mid;
            }
        }
        return (lo + hi) / 2.0F;
    }

    /**
     * Multiplicador de todos los ataques del jefe de la etapa. La base (1,0) es la etapa 1 con el set B solo, tal como se probó;
     * con las mascotas, reliquias y bonus del jugador de referencia de la etapa 1 sale un poco más de 1.
     */
    public static float dmgMul(int stage) {
        if (stage <= 1) {
            return rawFor(1, true) / rawFor(1, false);
        }
        // etapas 2+: el golpe fuerte (base 9) quita STRONG_FRACTION de la vida del jugador de referencia (armadura, mascotas, capa,
        // reliquias y encantamientos esperados incluidos)
        float target = STRONG_FRACTION * LoadoutRef.maxHp(bRank(stage));
        float lo = 1.0F;
        float hi = 5000.0F;
        for (int i = 0; i < 50; i++) {
            float mid = (lo + hi) / 2.0F;
            if (LoadoutRef.taken(stage, bRank(stage), mid, true) < target) {
                lo = mid;
            } else {
                hi = mid;
            }
        }
        return (lo + hi) / 2.0F / REF_RAW;
    }

    /**
     * Minecraft no deja que la vida máxima de una criatura pase de 1024 (el atributo se recorta solo). Los jefes con más vida (el
     * Gran Herrero tiene 1440 con un jugador; el Rey Brote con 2+ jugadores) usan esta vida real como tope y reciben menos daño en la
     * proporción {@link #hpScale}, así aguantan lo mismo que si la vida fuera la de diseño y NO se «curan» al intentar subir el atributo.
     */
    public static final float ATTR_CAP = 1000.0F;

    /** Vida máxima real (atributo) para una vida de diseño {@code target}. */
    public static float realMax(float target) {
        return Math.min(target, ATTR_CAP);
    }

    /** Fracción del daño que se aplica al jefe cuando su vida de diseño pasa del tope del atributo (1 = sin recorte). */
    public static float hpScale(float target) {
        return target <= ATTR_CAP ? 1.0F : ATTR_CAP / target;
    }

    /** Vida del jefe según los jugadores que hay en la arena: cada jugador extra suma el 75 % de la vida base. */
    public static float hpPlayers(int n) {
        return 1.0F + 0.75F * (Math.max(1, n) - 1);
    }

    /** Daño del jefe según los jugadores en la arena: cada jugador extra suma un 12 % (el reparto de golpes ya lo hace más duro). */
    public static float dmgPlayers(int n) {
        return 1.0F + 0.12F * (Math.max(1, n) - 1);
    }

    /** Golpe fuerte del jefe de la etapa (1 jugador), antes de armadura. */
    public static float strongHit(int stage) {
        return REF_RAW * dmgMul(stage);
    }

    /** Daño (en puntos de vida) que recibe del golpe fuerte quien lleva el set B de la caja de la etapa. */
    public static float strongHitTakenB(int stage) {
        return afterArmorRank(stage, bRank(stage), strongHit(stage));
    }

    /** Lo mismo con el set A de la misma caja. */
    public static float strongHitTakenA(int stage) {
        return afterArmorRank(stage, bRank(stage) - 1, strongHit(stage));
    }

    /** Puesto del set B de la etapa (para los textos del portal). */
    public static int setBRank(int stage) {
        return bRank(stage);
    }

    /** Resumen para el comando de pruebas. */
    public static String describe(int stage) {
        float raw = REF_RAW * dmgMul(stage);
        return "etapa " + stage + ": vida " + (int) hp(stage) + ", daño x" + String.format("%.2f", dmgMul(stage))
                + " (golpe fuerte " + String.format("%.1f", raw) + "; con armadura B recibes "
                + String.format("%.1f", afterArmorRank(stage, bRank(stage), raw)) + ", con A "
                + String.format("%.1f", afterArmorRank(stage, bRank(stage) - 1, raw)) + "; golpe de referencia " + String.format("%.1f", LoadoutRef.hit(stage)) + ") | "
                + LoadoutRef.describe(stage);
    }
}
