package com.mundosdeaxelis.table;

import com.mundosdeaxelis.registry.ModItems;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.List;
import java.util.function.Supplier;

/**
 * Recetas de la pestaña «Crear» de la Mesa del Artífice. Cada una cuesta Esencia y objetos del inventario, y pide un nivel de mesa.
 * Para añadir una creación nueva basta con sumar una línea a {@link #all()}: la pantalla y el servidor la leen de aquí.
 */
public final class TableRecipes {
    private TableRecipes() {
    }

    public record Ing(Supplier<Item> item, int n) {
    }

    public record Recipe(String id, Supplier<Item> result, int count, int essence, int minLevel, List<Ing> ings) {
    }

    private static List<Recipe> all;

    private static Ing mat(Mats m, int n) {
        return new Ing(() -> ModItems.MATS.get(m).get(), n);
    }

    private static Ing of(Supplier<Item> it, int n) {
        return new Ing(it, n);
    }

    /** Todas las recetas, en el orden en que salen en pantalla (el número de botón es la posición). */
    public static List<Recipe> all() {
        if (all == null) {
            all = List.of(
                    new Recipe("ember_rivet", ModItems.EMBER_RIVET::get, 2, 15, 1,
                            List.of(of(ModItems.EMBER_STEEL::get, 1))),
                    new Recipe("golden_rivet", ModItems.GOLDEN_RIVET::get, 1, 60, 1,
                            List.of(of(ModItems.EMBER_STEEL::get, 2), of(() -> Items.GOLD_INGOT, 3))),
                    new Recipe("smith_ring", ModItems.SMITH_RING::get, 1, 400, 2,
                            List.of(of(ModItems.EMBER_STEEL::get, 8), mat(Mats.LIVE_EMBER, 4), mat(Mats.OLD_COIN, 6))),
                    // etapa 270: las llaves ya no se forjan aquí sino en el Gran Yunque de Fundición, que se crea en esta Mesa
                    new Recipe("gran_yunque", ModItems.GRAN_YUNQUE::get, 1, 100, 1,
                            List.of(of(() -> Items.ANVIL, 1), of(() -> Items.BLAST_FURNACE, 1), of(() -> Items.IRON_BLOCK, 2))),
                    new Recipe("mesa_gemas", ModItems.MESA_GEMAS::get, 1, 80, 1,
                            List.of(of(() -> Items.AMETHYST_SHARD, 8), of(() -> Items.SMITHING_TABLE, 1), of(() -> Items.LAPIS_BLOCK, 1))),
                    new Recipe("tablon_encargos", ModItems.TABLON_ENCARGOS::get, 1, 30, 1,
                            List.of(of(() -> Items.SPRUCE_PLANKS, 6), of(() -> Items.PAPER, 4), of(() -> Items.INK_SAC, 1)))
            );
        }
        return all;
    }
}
