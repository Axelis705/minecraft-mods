package com.mundosdeaxelis.block;

import com.mundosdeaxelis.crate.Tier;

import java.util.List;

/**
 * Tabla de la decoración (generada con gen12/decor_install.py): una sola lista para registrar, sortear en las cajas y dar forma a los
 * bloques. Cada decoración sale solo en las cajas de su campo {@code crates} (ids separados por comas): no hay decoración que salga en todas.
 */
public final class DecorDefs {
    private DecorDefs() {
    }

    /** Un objeto decorativo. {@code box} es la forma (null = cubo completo); {@code sound}, el nombre del sonido. */
    public record Def(String id, Tier tier, int light, String sound, int min, int max, double[] box, float strength, String crates) {
        public boolean shaped() {
            return box != null;
        }

        /** Si esta decoración puede salir en la caja con ese id (forest, forge, ice, magma, magic, ruby, sky, abyss). */
        public boolean dropsIn(String crateId) {
            for (String c : crates.split(",")) {
                if (c.equals(crateId)) {
                    return true;
                }
            }
            return false;
        }
    }

    public static final List<Def> ALL = List.of(
            // trofeo del jefe de la etapa 1: no sale en ninguna caja (campo crates vacío), solo al derrotar al Rey Brote
            new Def("decor_trophy_forest", Tier.EPIC, 8, "STONE", 1, 1, new double[]{3.0, 0.0, 3.0, 13.0, 16.0, 13.0}, 1.5F, ""),
            // trofeo del jefe de la etapa 2 (Forja): solo al derrotar al Gran Herrero de Brasas
            new Def("decor_trophy_forge", Tier.EPIC, 8, "STONE", 1, 1, new double[]{3.0, 0.0, 3.0, 14.8, 16.0, 13.0}, 1.5F, ""),
            new Def("decor_rose_bricks", Tier.LEGENDARY, 0, "STONE", 8, 16, null, 1.0F, "ruby"),
            new Def("decor_checker_tiles", Tier.COMMON, 0, "STONE", 8, 16, null, 1.0F, "forge"),
            new Def("decor_ornate_planks", Tier.COMMON, 0, "WOOD", 8, 16, null, 1.0F, "forest"),
            new Def("decor_gift_box", Tier.MYTHIC, 0, "WOOL", 1, 2, new double[]{0.9, 0.0, 0.9, 15.1, 14.2, 15.1}, 1.0F, "sky"),
            new Def("decor_flower_vase", Tier.COMMON, 0, "GLASS", 1, 2, new double[]{4.0, 0.0, 4.0, 12.0, 15.2, 12.0}, 1.0F, "forest"),
            new Def("decor_cloud_block", Tier.UNCOMMON, 0, "WOOL", 6, 12, null, 1.0F, "ice"),
            new Def("decor_star_lantern", Tier.MYTHIC, 15, "LANTERN", 4, 8, null, 1.0F, "sky"),
            new Def("decor_candelabra", Tier.COMMON, 13, "METAL", 1, 2, new double[]{1.6, 0.0, 4.0, 14.4, 16.0, 12.0}, 1.0F, "forge"),
            new Def("decor_moon_stone", Tier.EPIC, 9, "AMETHYST", 4, 8, null, 1.5F, "magic"),
            new Def("decor_crystal_block", Tier.STELLAR, 10, "AMETHYST", 3, 6, null, 1.5F, "abyss"),
            new Def("decor_lava_lamp", Tier.RARE, 15, "GLASS", 3, 6, null, 1.0F, "magma"),
            new Def("decor_gold_tiles", Tier.LEGENDARY, 0, "METAL", 8, 16, null, 2.0F, "ruby"),
            new Def("decor_glow_mushroom", Tier.COMMON, 11, "WOOD", 1, 2, new double[]{2.0, 0.0, 2.0, 14.0, 13.0, 14.0}, 1.0F, "forest"),
            new Def("decor_brazier", Tier.COMMON, 15, "METAL", 1, 2, new double[]{2.0, 0.0, 2.0, 14.0, 16.0, 14.0}, 1.0F, "forge"),
            new Def("decor_knight_statue", Tier.RARE, 0, "STONE", 1, 1, new double[]{1.0, 0.0, 2.0, 14.0, 16.0, 13.0}, 1.0F, "magma"),
            new Def("decor_snowman", Tier.UNCOMMON, 0, "WOOL", 1, 1, new double[]{3.0, 0.0, 3.0, 13.0, 16.0, 13.0}, 1.0F, "ice"),
            new Def("decor_ice_spikes", Tier.UNCOMMON, 8, "GLASS", 1, 2, new double[]{1.5, 0.0, 1.5, 14.5, 16.0, 14.5}, 1.0F, "ice"),
            new Def("decor_candle_skull", Tier.RARE, 12, "STONE", 1, 1, new double[]{2.0, 0.0, 2.0, 14.0, 13.0, 14.0}, 1.0F, "magma"),
            new Def("decor_crystal_ball", Tier.EPIC, 13, "GLASS", 1, 1, new double[]{3.0, 0.0, 3.0, 13.0, 14.0, 13.0}, 1.0F, "magic"),
            new Def("decor_book_stack", Tier.EPIC, 6, "WOOD", 1, 2, new double[]{2.0, 0.0, 3.0, 14.0, 16.0, 13.0}, 1.0F, "magic"),
            new Def("decor_dragon_egg", Tier.LEGENDARY, 10, "STONE", 1, 1, new double[]{2.5, 0.0, 2.5, 13.5, 16.0, 13.5}, 1.0F, "ruby"),
            new Def("decor_balloon", Tier.MYTHIC, 0, "WOOL", 1, 1, new double[]{1.5, 0.0, 1.5, 14.5, 16.0, 14.5}, 1.0F, "sky"),
            new Def("decor_void_eye", Tier.STELLAR, 12, "AMETHYST", 1, 1, new double[]{2.0, 0.0, 2.0, 14.0, 16.0, 14.0}, 1.0F, "abyss"),
            new Def("decor_petal_lantern", Tier.COMMON, 12, "LANTERN", 1, 2, new double[]{4.0, 0.0, 4.0, 12.0, 16.0, 12.0}, 1.0F, "forest"),
            new Def("decor_garden_bench", Tier.COMMON, 0, "WOOD", 1, 1, new double[]{0.0, 0.0, 3.6, 16.0, 13.0, 12.0}, 1.0F, "forest"),
            new Def("decor_petal_pond", Tier.COMMON, 0, "STONE", 1, 1, new double[]{1.0, 0.0, 1.0, 15.0, 6.0, 15.0}, 1.0F, "forest"),
            new Def("decor_gear_lantern", Tier.UNCOMMON, 14, "METAL", 1, 2, new double[]{3.0, 0.0, 3.0, 13.0, 16.0, 13.0}, 1.0F, "forge"),
            new Def("decor_cog_clock", Tier.UNCOMMON, 0, "METAL", 1, 1, new double[]{3.0, 0.0, 4.0, 13.0, 16.0, 12.0}, 1.0F, "forge"),
            new Def("decor_steam_boiler", Tier.UNCOMMON, 8, "METAL", 1, 1, new double[]{2.0, 0.0, 3.0, 14.0, 16.0, 13.0}, 1.0F, "forge"),
            new Def("decor_aurora_lamp", Tier.RARE, 13, "LANTERN", 1, 2, new double[]{4.0, 0.0, 4.0, 12.0, 16.0, 12.0}, 1.0F, "ice"),
            new Def("decor_aurora_arch", Tier.RARE, 11, "METAL", 1, 1, new double[]{1.5, 0.0, 5.0, 14.5, 16.0, 11.0}, 1.0F, "ice"),
            new Def("decor_aurora_fir", Tier.RARE, 9, "WOOD", 1, 1, new double[]{3.0, 0.0, 3.0, 13.0, 16.0, 13.0}, 1.0F, "ice"),
            new Def("decor_ember_bowl", Tier.EPIC, 15, "LANTERN", 1, 2, new double[]{2.0, 0.0, 2.0, 14.0, 10.0, 14.0}, 1.0F, "magma"),
            new Def("decor_obsidian_spire", Tier.EPIC, 9, "STONE", 1, 1, new double[]{2.0, 0.0, 2.0, 14.0, 16.0, 14.0}, 1.0F, "magma"),
            new Def("decor_obsidian_gate", Tier.EPIC, 13, "STONE", 1, 1, new double[]{1.5, 0.0, 4.5, 14.5, 16.0, 11.5}, 1.0F, "magma"),
            new Def("decor_moon_lantern", Tier.LEGENDARY, 14, "LANTERN", 1, 2, new double[]{4.0, 0.0, 4.0, 12.0, 16.0, 12.0}, 1.0F, "magic"),
            new Def("decor_moon_telescope", Tier.LEGENDARY, 0, "METAL", 1, 1, new double[]{2.0, 0.0, 1.0, 14.0, 16.0, 15.0}, 1.0F, "magic"),
            new Def("decor_moon_fountain", Tier.LEGENDARY, 9, "STONE", 1, 1, new double[]{1.0, 0.0, 1.0, 15.0, 15.0, 15.0}, 1.0F, "magic"),
            new Def("decor_golden_lantern", Tier.MYTHIC, 15, "LANTERN", 1, 2, new double[]{4.0, 0.0, 4.0, 12.0, 16.0, 12.0}, 1.0F, "ruby"),
            new Def("decor_golden_pearl", Tier.MYTHIC, 12, "METAL", 1, 1, new double[]{1.0, 0.0, 1.0, 15.0, 15.0, 15.0}, 1.0F, "ruby"),
            new Def("decor_golden_dragon", Tier.MYTHIC, 0, "METAL", 1, 1, new double[]{1.0, 0.0, 1.0, 15.0, 16.0, 15.0}, 1.0F, "ruby"),
            new Def("decor_pegasus_lantern", Tier.STELLAR, 15, "LANTERN", 1, 2, new double[]{4.0, 0.0, 4.0, 12.0, 16.0, 12.0}, 1.0F, "sky"),
            new Def("decor_pegasus_fountain", Tier.STELLAR, 12, "STONE", 1, 1, new double[]{1.0, 0.0, 1.0, 15.0, 15.0, 15.0}, 1.0F, "sky"),
            new Def("decor_pegasus_statue", Tier.STELLAR, 0, "STONE", 1, 1, new double[]{2.0, 0.0, 3.0, 14.0, 16.0, 14.0}, 1.0F, "sky"),
            new Def("decor_cosmic_lantern", Tier.CELESTIAL, 15, "LANTERN", 1, 2, new double[]{4.0, 0.0, 4.0, 12.0, 16.0, 12.0}, 1.0F, "abyss"),
            new Def("decor_cosmic_gate", Tier.CELESTIAL, 14, "STONE", 1, 1, new double[]{1.0, 0.0, 5.5, 15.0, 16.0, 10.5}, 1.0F, "abyss"),
            new Def("decor_cosmic_orrery", Tier.CELESTIAL, 13, "METAL", 1, 1, new double[]{1.0, 0.0, 1.0, 15.0, 16.0, 15.0}, 1.0F, "abyss"),
            new Def("decor_shadow_obelisk", Tier.STELLAR, 12, "STONE", 1, 1, new double[]{2.5, 0.0, 2.5, 13.5, 16.0, 13.5}, 1.0F, "abyss")
    );

    public static Def get(String id) {
        for (Def d : ALL) {
            if (d.id().equals(id)) {
                return d;
            }
        }
        return null;
    }
}
