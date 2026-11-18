package com.mundosdeaxelis.gear;

import com.mundosdeaxelis.crate.Tier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * La tabla de armaduras: ocho sets (uno por caja de colección) de cuatro piezas. Cada set tiene una sola rareza (todas sus
 * piezas la comparten) y la protección total sube con el puesto del set: la de Naturaleza protege como el hierro (15) y la del Vacío estelar
 * llega a 45 (el tope normal de Minecraft, 30, se quita en ArmorCap). Las cajas mejores dan armaduras mejores, igual que con las herramientas.
 */
public final class ArmorDefs {
    private ArmorDefs() {
    }

    public enum Piece {
        HELMET("helmet", ArmorItem.Type.HELMET, EquipmentSlot.HEAD),
        CHESTPLATE("chestplate", ArmorItem.Type.CHESTPLATE, EquipmentSlot.CHEST),
        LEGGINGS("leggings", ArmorItem.Type.LEGGINGS, EquipmentSlot.LEGS),
        BOOTS("boots", ArmorItem.Type.BOOTS, EquipmentSlot.FEET);

        public final String id;
        public final ArmorItem.Type type;
        public final EquipmentSlot slot;

        Piece(String id, ArmorItem.Type type, EquipmentSlot slot) {
            this.id = id;
            this.type = type;
            this.slot = slot;
        }
    }

    /**
     * Protección total del set por puesto 1..16 (A y B de cada caja): el primer set protege como el hierro (15) y CADA PIEZA
     * suma 1 punto en cada puesto, así que el set B es mejor que el A en las cuatro piezas (no solo en el combo del set
     * completo): de A a B el set suma +4 y el último llega a 75. Minecraft limita la armadura a 30, pero el mod quita ese tope
     * (ver {@link ArmorCap}); la barra de armadura dibuja encima, con otro color, los puntos que pasan de 20.
     */
    public static int setTotal(int rank) {
        return 15 + 4 * (rank - 1);
    }

    /** Reparto por pieza (casco, peto, pantalón, botas): como el hierro (2/6/5/2) en el puesto 1 y +1 en cada pieza por puesto. */
    private static final int[][] DEFENSE = new int[16][4];

    static {
        int[] base = {2, 6, 5, 2};
        for (int r = 0; r < 16; r++) {
            for (int i = 0; i < 4; i++) {
                DEFENSE[r][i] = base[i] + r;
            }
        }
    }
    /** Dureza (por pieza, +0,25 por puesto): el hierro no tiene, el diamante 2 y la netherita 3; aquí llega a 4,25. */
    private static final float[] TOUGHNESS = {0.5F, 0.75F, 1F, 1.25F, 1.5F, 1.75F, 2F, 2.25F, 2.5F, 2.75F, 3F, 3.25F, 3.5F, 3.75F, 4F, 4.25F};
    /** Resistencia al empuje (por pieza; la netherita da 0,1). */
    private static final float[] KNOCKBACK = {0F, 0F, 0.01F, 0.01F, 0.02F, 0.02F, 0.03F, 0.03F, 0.04F, 0.04F, 0.05F, 0.06F, 0.07F, 0.08F, 0.09F, 0.10F};
    /** Multiplicador de durabilidad (el de cuero es 5, el de hierro 15, el de diamante 33, el de netherita 37). */
    private static final int[] DURABILITY = {15, 17, 19, 21, 23, 25, 27, 29, 31, 33, 35, 37, 39, 41, 43, 45};
    private static final int[] BASE = {11, 16, 15, 13};

    /** Rareza del set de armadura (la de todo el set). */
    /**
     * Reducción de daño propia del mod con el set completo (fracción): 0 en el primer set y 66 % en el último, además de la
     * defensa. Baja un 7 % del daño restante por puesto (0,93 por puesto), así que el set B siempre protege algo más que el A,
     * incluso cuando la armadura de Minecraft ya está en su tope (80 %). Cada pieza aporta una cuarta parte
     * (ver {@code ArmorSetEvents.onHurt}).
     */
    public static float damageReduction(GearSet s) {
        return damageReduction(s.rank);
    }

    /** Lo mismo por puesto (1..16). */
    public static float damageReduction(int rank) {
        return 1.0F - (float) Math.pow(0.93D, rank - 1);
    }

    public static Tier tier(GearSet s) {
        return s.tier;
    }

    public static int defense(GearSet s, Piece p) {
        return DEFENSE[s.rank - 1][p.ordinal()];
    }

    public static int totalDefense(GearSet s) {
        int n = 0;
        for (int d : DEFENSE[s.rank - 1]) {
            n += d;
        }
        return n;
    }

    public static float toughness(GearSet s) {
        return TOUGHNESS[s.rank - 1];
    }

    public static float knockback(GearSet s) {
        return KNOCKBACK[s.rank - 1];
    }

    public static int durability(GearSet s, Piece p) {
        return BASE[p.ordinal()] * DURABILITY[s.rank - 1];
    }

    public static int enchantability(GearSet s) {
        return 12 + 3 * tier(s).ordinal();
    }

    public static String id(GearSet s, Piece p) {
        return s.id + "_armor_" + p.id;
    }

    private static final Map<String, Object[]> BY_ID = new HashMap<>();
    public static final List<String> IDS = new ArrayList<>();

    static {
        for (GearSet s : GearSet.values()) {
            for (Piece p : Piece.values()) {
                BY_ID.put(id(s, p), new Object[]{s, p});
                IDS.add(id(s, p));
            }
        }
    }

    public static boolean isArmorId(String id) {
        return BY_ID.containsKey(id);
    }
}
