package com.mundosdeaxelis.crate;

import com.mundosdeaxelis.registry.ModItems;
import com.mundosdeaxelis.table.Mats;
import net.minecraft.world.item.Item;

import java.util.List;

/**
 * Receta para forjar el bloque de cada caja de colección en la Mesa del Artífice: Esencia + materiales de mobs + el sello del
 * jefe de la etapa anterior (la primera caja, Naturaleza, no pide sello). Las llaves se siguen comprando solo con Esencia.
 */
public final class CrateForge {
    private CrateForge() {
    }

    /** Un material de mob y cuántos pide la receta. */
    public record Need(Mats mat, int n) {
    }

    /** Esencia que cuesta forjar el bloque (cuatro veces el precio de su llave). */
    public static int essence(CrateKind k) {
        return k.keyCost() * 4;
    }

    /** Materiales de mobs que pide la receta, de temática parecida a la de la caja. */
    public static List<Need> mats(CrateKind k) {
        return switch (k) {
            case FOREST -> List.of(new Need(Mats.TORN_CLOTH, 6), new Need(Mats.WOVEN_SILK, 6));
            case FORGE -> List.of(new Need(Mats.RUNIC_BONE, 8), new Need(Mats.SHINY_JELLY, 8), new Need(Mats.EXPLOSIVE_HEART, 3));
            case ICE -> List.of(new Need(Mats.PEARL_KELP, 8), new Need(Mats.LUNAR_VEIL, 6), new Need(Mats.SEA_SCALE, 2));
            case MAGMA -> List.of(new Need(Mats.LIVE_EMBER, 8), new Need(Mats.OLD_COIN, 8), new Need(Mats.DARK_ASH, 3));
            case MAGIC -> List.of(new Need(Mats.ELIXIR_DROP, 8), new Need(Mats.PURPLE_ESSENCE, 8), new Need(Mats.SHELL_SHARD, 4));
            case RUBY -> List.of(new Need(Mats.LIVE_EMBER, 12), new Need(Mats.DARK_ASH, 8), new Need(Mats.LAMENT_ECHO, 4));
            case SKY -> List.of(new Need(Mats.LUNAR_VEIL, 12), new Need(Mats.LAMENT_ECHO, 8), new Need(Mats.SHELL_SHARD, 6));
            case ABYSS -> List.of(new Need(Mats.SEA_SCALE, 8), new Need(Mats.SHELL_SHARD, 10), new Need(Mats.ABYSS_CORE, 2));
            default -> List.of();
        };
    }

    /** Caja cuyo jefe suelta el sello que pide esta receta (null = no pide sello). */
    public static CrateKind sealOf(CrateKind k) {
        int r = k.rank();
        return r <= 1 ? null : CrateKind.values()[r - 1];
    }

    /** Caja que se desbloquea con el sello de {@code k} (null si es la última). */
    public static CrateKind unlocks(CrateKind k) {
        int r = k.rank();
        return r <= 0 || r >= CrateKind.values().length - 1 ? null : CrateKind.values()[r + 1];
    }

    /**
     * Etapa del jugador: el puesto más alto cuyo sello ya ha tenido (el Archivo lo apunta al llevarlo). Puede forjar las
     * cajas hasta la etapa + 1 y ve (como «?») una más por encima.
     */
    public static int stage(java.util.Set<String> archive) {
        int best = 0;
        for (CrateKind k : CrateKind.values()) {
            if (k != CrateKind.PETS && archive.contains("seal_" + k.id)) {
                best = Math.max(best, k.rank());
            }
        }
        return best;
    }

    public static Item sealItem(CrateKind k) {
        return ModItems.SEALS.get(k).get();
    }

    /** Lo máximo que pide cualquier receta de ese material (la Mesa no lo refina para que no se pierda). */
    public static int reservedFor(Mats m) {
        int best = 0;
        for (CrateKind k : CrateKind.values()) {
            for (Need n : mats(k)) {
                if (n.mat() == m) {
                    best = Math.max(best, n.n());
                }
            }
        }
        return best;
    }
}
