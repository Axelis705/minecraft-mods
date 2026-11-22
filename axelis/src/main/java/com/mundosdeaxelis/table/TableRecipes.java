package com.mundosdeaxelis.table;

import com.mundosdeaxelis.registry.ModItems;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.List;
import java.util.function.Supplier;

/**
 * Recetas de la pestaña «Crear» de la Mesa del Artífice. Cada una cuesta Esencia y objetos del inventario, y pide un nivel de mesa.
 * Etapa 286: por pestañas (reparar, útiles, para tu base) y los reparadores de todos los tiers, que se aprenden al tenerlos.
 * Para añadir una creación nueva basta con sumar una línea a {@link #all()}: la pantalla y el servidor la leen de aquí.
 */
public final class TableRecipes {
    private TableRecipes() {
    }

    public record Ing(Supplier<Item> item, int n) {
    }

    /**
     * {@code cat}: pestaña («reparar», «mesa» o «utiles»). {@code descubrir}: id del objeto que hay que haber tenido alguna vez (el
     * Archivo lo apunta) para poder crearlo; null = siempre.
     */
    public record Recipe(String id, Supplier<Item> result, int count, int essence, int minLevel, List<Ing> ings, String cat, String descubrir) {
        public Recipe(String id, Supplier<Item> result, int count, int essence, int minLevel, List<Ing> ings) {
            this(id, result, count, essence, minLevel, ings, "utiles", null);
        }

        /** ¿Lo conoce ya? (lo ha tenido alguna vez) */
        public boolean conocida(java.util.Set<String> archivo) {
            return descubrir == null || archivo.contains(descubrir);
        }
    }

    public static final String[] CATS = {"reparar", "utiles", "mesa"};

    /** ¿Tener este objeto enseña alguna receta? */
    public static boolean ensenia(String id) {
        for (Recipe r : all()) {
            if (id.equals(r.descubrir())) {
                return true;
            }
        }
        return false;
    }

    private static List<Recipe> all;

    private static Ing mat(Mats m, int n) {
        return new Ing(() -> ModItems.MATS.get(m).get(), n);
    }

    private static Ing of(Supplier<Item> it, int n) {
        return new Ing(it, n);
    }

    /** Reparador: se puede crear una vez que lo hayas tenido (el Archivo lo apunta). */
    private static Recipe rep(String id, Supplier<Item> it, int n, int essence, int lvl, List<Ing> ings) {
        return new Recipe(id, it, n, essence, lvl, ings, "reparar", id);
    }

    /** Todas las recetas, en el orden en que salen en pantalla (el número de botón es la posición). */
    public static List<Recipe> all() {
        if (all == null) {
            all = List.of(
                    // ---- reparar: el remache ardiente y el dorado siempre; los demás, cuando ya los hayas tenido (etapa 286)
                    new Recipe("ember_rivet", ModItems.EMBER_RIVET::get, 2, 20, 1,
                            List.of(of(ModItems.EMBER_STEEL::get, 1)), "reparar", null),
                    new Recipe("golden_rivet", ModItems.GOLDEN_RIVET::get, 1, 80, 1,
                            List.of(of(ModItems.EMBER_STEEL::get, 2), of(() -> Items.GOLD_INGOT, 3)), "reparar", null),
                    rep("acorn", ModItems.ACORN::get, 2, 25, 1, List.of(mat(Mats.TORN_CLOTH, 2), of(() -> Items.OAK_SAPLING, 1))),
                    rep("golden_acorn", ModItems.GOLDEN_ACORN::get, 1, 90, 1, List.of(of(ModItems.ACORN::get, 3), of(() -> Items.GOLD_INGOT, 4))),
                    rep("frost_scale", ModItems.FROST_SCALE::get, 2, 35, 2, List.of(mat(Mats.PEARL_KELP, 2), of(() -> Items.BLUE_ICE, 1))),
                    rep("pure_frost_scale", ModItems.PURE_FROST_SCALE::get, 1, 120, 2, List.of(of(ModItems.FROST_SCALE::get, 3), of(() -> Items.DIAMOND, 1))),
                    rep("tempered_cinder", ModItems.TEMPERED_CINDER::get, 2, 45, 3, List.of(mat(Mats.LIVE_EMBER, 2), of(() -> Items.MAGMA_CREAM, 2))),
                    rep("cinder_heart", ModItems.CINDER_HEART::get, 1, 150, 3, List.of(of(ModItems.TEMPERED_CINDER::get, 3), of(() -> Items.BLAZE_ROD, 2))),
                    rep("mending_rune", ModItems.MENDING_RUNE::get, 2, 55, 4, List.of(mat(Mats.PURPLE_ESSENCE, 2), of(() -> Items.LAPIS_LAZULI, 4))),
                    rep("greater_mending_rune", ModItems.GREATER_MENDING_RUNE::get, 1, 180, 4,
                            List.of(of(ModItems.MENDING_RUNE::get, 3), of(() -> Items.AMETHYST_SHARD, 4))),
                    rep("dragon_mend_scale", ModItems.DRAGON_MEND_SCALE::get, 2, 70, 5, List.of(mat(Mats.DARK_ASH, 1), mat(Mats.SEA_SCALE, 1))),
                    rep("ancient_dragon_scale", ModItems.ANCIENT_DRAGON_SCALE::get, 1, 220, 5,
                            List.of(of(ModItems.DRAGON_MEND_SCALE::get, 3), of(() -> Items.EMERALD, 4))),
                    rep("woven_feather", ModItems.WOVEN_FEATHER::get, 2, 85, 6, List.of(mat(Mats.LUNAR_VEIL, 2), of(() -> Items.FEATHER, 4))),
                    rep("storm_woven_feather", ModItems.STORM_WOVEN_FEATHER::get, 1, 260, 6,
                            List.of(of(ModItems.WOVEN_FEATHER::get, 3), mat(Mats.LAMENT_ECHO, 1))),
                    rep("void_splinter", ModItems.VOID_SPLINTER::get, 2, 100, 7, List.of(mat(Mats.SHELL_SHARD, 1), of(() -> Items.ENDER_PEARL, 2))),
                    rep("pure_void_splinter", ModItems.PURE_VOID_SPLINTER::get, 1, 320, 7,
                            List.of(of(ModItems.VOID_SPLINTER::get, 3), of(() -> Items.ECHO_SHARD, 1))),
                    // ---- útiles
                    new Recipe("smith_ring", ModItems.SMITH_RING::get, 1, 520, 2,
                            List.of(of(ModItems.EMBER_STEEL::get, 8), mat(Mats.LIVE_EMBER, 4), mat(Mats.OLD_COIN, 6)), "utiles", null),
                    new Recipe("sanctuary_berry", ModItems.SANCTUARY_BERRY::get, 3, 15, 1,
                            List.of(of(() -> Items.SWEET_BERRIES, 6), mat(Mats.ELIXIR_DROP, 1)), "utiles", "sanctuary_berry"),
                    new Recipe("forge_roast", ModItems.FORGE_ROAST::get, 2, 20, 1,
                            List.of(of(() -> Items.COOKED_BEEF, 2), of(() -> Items.COAL, 2)), "utiles", "forge_roast"),
                    new Recipe("gema_vacia", ModItems.GEMA_VACIA::get, 1, 40, 2,
                            List.of(of(() -> Items.AMETHYST_SHARD, 4), mat(Mats.ELIXIR_DROP, 2)), "utiles", "gema_vacia"),
                    // ---- para tu base
                    // etapa 270: las llaves ya no se forjan aquí sino en el Gran Yunque de Fundición, que se crea en esta Mesa
                    new Recipe("gran_yunque", ModItems.GRAN_YUNQUE::get, 1, 130, 1,
                            List.of(of(() -> Items.ANVIL, 1), of(() -> Items.BLAST_FURNACE, 1), of(() -> Items.IRON_BLOCK, 2)), "mesa", null),
                    new Recipe("mesa_gemas", ModItems.MESA_GEMAS::get, 1, 100, 1,
                            List.of(of(() -> Items.AMETHYST_SHARD, 8), of(() -> Items.SMITHING_TABLE, 1), of(() -> Items.LAPIS_BLOCK, 1)), "mesa", null),
                    new Recipe("guia_gemas", ModItems.GUIA_GEMAS::get, 1, 10, 1,
                            List.of(of(() -> Items.BOOK, 1), of(() -> Items.AMETHYST_SHARD, 2)), "mesa", null),
                    new Recipe("tablon_encargos", ModItems.TABLON_ENCARGOS::get, 1, 40, 1,
                            List.of(of(() -> Items.SPRUCE_PLANKS, 6), of(() -> Items.PAPER, 4), of(() -> Items.INK_SAC, 1)), "mesa", null)
            );
        }
        return all;
    }
}
