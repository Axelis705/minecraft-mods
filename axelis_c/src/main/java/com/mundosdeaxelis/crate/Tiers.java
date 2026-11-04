package com.mundosdeaxelis.crate;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;

/**
 * Tabla única de rarezas de TODO lo que tiene rareza propia. Para reequilibrar el mod basta con cambiar una línea de aquí.
 * Criterio: Común = utilidad sencilla; Poco común = útil pero limitada; Rara = habilidad clara; Épica = habilidad fuerte;
 * Legendaria = cambia una pelea; Mítica = poder máximo.
 */
public final class Tiers {
    private Tiers() {
    }

    private static final Map<String, Tier> MAP = new HashMap<>();

    private static void put(Tier t, String... ids) {
        for (String id : ids) {
            MAP.put(id, t);
        }
    }

    static {
        // ---------------- reliquias (64)
        put(Tier.COMMON, "feather_talisman", "tide_shell", "wind_ring", "warrior_medal", "cloud_bottle", "astral_spyglass", "fate_coin");
        put(Tier.UNCOMMON, "oak_heart", "miner_medallion", "wolf_fang", "frost_bracelet", "moon_crystal", "fox_charm", "specter_mask",
                "titan_belt", "healing_lyre", "quake_seal", "solar_chakram", "music_box", "whirlwind_staff", "fang_staff");
        put(Tier.RARE, "eternal_rose", "crystal_heart", "aurora_horn", "ender_orb", "war_horn", "rage_gauntlet", "eclipse_dagger",
                "soul_lantern", "comet_lance", "heart_fireworks", "red_thread", "fairy_jar", "home_mirror", "magma_boots");
        put(Tier.EPIC, "sun_crown", "storm_eye", "dragon_scale", "arcane_tome", "vampire_chalice", "voltaic_scepter", "medusa_eye",
                "umbral_glaive", "genie_lamp", "light_wings", "prism_rainbow", "star_bow", "mirror_shield", "victory_banner",
                "hatchling_egg", "moon_blade", "dragon_whip", "rocket_pack", "smith_ring");
        put(Tier.LEGENDARY, "meteor_scepter", "time_pendulum", "rewind_anchor", "world_tree_seed", "star_charm",
                "star_heart");
        put(Tier.MYTHIC, "phoenix_ring");
        // las dos rarezas nuevas: Estelar (Cielo y Abismo) y Celestial, la más alta de todas (Abismo)
        put(Tier.STELLAR, "hourglass", "abyss_scepter");
        put(Tier.CELESTIAL, "void_singularity", "celestial_aegis");
        // ---------------- compañeros (48)
        put(Tier.COMMON, "comp_chick", "comp_frog", "comp_hamster", "comp_snail", "comp_capybara", "comp_duck", "comp_slime");
        put(Tier.UNCOMMON, "comp_bee", "comp_sheep", "comp_hedgehog", "comp_bunny", "comp_cat", "comp_corgi", "comp_snowman");
        put(Tier.RARE, "comp_fox", "comp_turtle", "comp_penguin", "comp_raccoon", "comp_ghost", "comp_sparkmouse");
        put(Tier.EPIC, "comp_jelly", "comp_panda", "comp_forest_spirit");
        // favoritas de Axelis subidas de rareza: Creepercito, Golemcito y Ajolote a Legendaria; Droide, Cerbero y Dragón de la Noche a Mítica
        put(Tier.LEGENDARY, "comp_phoenix", "comp_whale", "comp_creeper", "comp_golem", "comp_axolotl");
        put(Tier.MYTHIC, "comp_kitsune", "comp_droid", "comp_moon_wolf", "comp_night_dragon");
        put(Tier.COMMON, "comp_crab");
        put(Tier.UNCOMMON, "comp_octopus", "comp_firefly");
        put(Tier.RARE, "comp_elephant", "comp_lion");
        put(Tier.EPIC, "comp_peacock", "comp_dolphin", "comp_mimic");
        put(Tier.LEGENDARY, "comp_pegasus", "comp_griffon");
        // Fosu y Cookie (las mascotas de Axelis y Odette) son Celestiales, como el Dragón Celestial
        put(Tier.CELESTIAL, "comp_celestial_dragon", "comp_butterfly", "comp_ferret");
        put(Tier.COMMON, "comp_piggy");
        put(Tier.UNCOMMON, "comp_skunk");
        put(Tier.RARE, "comp_seal");
        put(Tier.EPIC, "comp_moonimp");
        put(Tier.RARE, "comp_trike");
        put(Tier.COMMON, "comp_patch");
        put(Tier.EPIC, "comp_yeti");
        // mascotas exclusivas de los jefes (Rey Brote y Gran Herrero): solo caen de ellos, no salen en cajas
        put(Tier.EPIC, "comp_sprout_king");
        put(Tier.LEGENDARY, "comp_ember_anvil");
        // ---------------- equipo (12)
        put(Tier.COMMON, "gear_dawn_blade", "gear_cheer_pick", "gear_lumber_axe");
        put(Tier.UNCOMMON, "gear_frost_blade", "gear_prospector_pick");
        // la rareza de cada arma especial es la de su caja (cada caja sube de menos a más, sin saltos hacia atrás)
        put(Tier.RARE, "gear_excavator");
        put(Tier.EPIC, "gear_vein_pick");
        put(Tier.LEGENDARY, "gear_dragon_sword");
        put(Tier.MYTHIC, "gear_storm_blade");
        put(Tier.STELLAR, "gear_thunder_axe", "gear_vampire_saber");
        put(Tier.CELESTIAL, "gear_core_pick");
    }

    /** Objetos que NO salen en cajas ni cofres: solo se consiguen creándolos en la Mesa del Artífice. */
    private static final java.util.Set<String> CRAFT_ONLY = java.util.Set.of("smith_ring", "comp_sprout_king", "comp_ember_anvil");

    public static boolean craftOnly(String id) {
        return CRAFT_ONLY.contains(id);
    }

    /** Todas las rarezas registradas (id -> rareza). */
    public static Map<String, Tier> all() {
        return MAP;
    }

    /** Registra la rareza de un objeto nuevo (equipo, llaves...). */
    public static void register(String id, Tier tier) {
        MAP.put(id, tier);
    }

    public static Tier of(String id) {
        return MAP.getOrDefault(id, Tier.COMMON);
    }

    public static Tier of(Item item) {
        if (item instanceof Tiered t) {
            return t.tier();
        }
        return Tier.COMMON;
    }

    public static Tier of(ItemStack stack) {
        return of(stack.getItem());
    }

    /**
     * Rareza que se VE (nombre, color y texto): la que tiene el objeto en su caja. Solo es etiqueta; el poder, el enfriamiento y los
     * costes siguen usando {@link #of}. Lo que no sale en ninguna caja se ve con su rareza interna.
     */
    public static Tier shown(Item item) {
        net.minecraft.resources.ResourceLocation rl = net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(item);
        if (rl != null) {
            Tier h = CratePools.home(rl.getPath());
            if (h != null) {
                return h;
            }
        }
        return of(item);
    }

    public static Tier shown(ItemStack stack) {
        return shown(stack.getItem());
    }

    /** Igual, por id del objeto ("forest_sword_common"). */
    public static Tier shown(String id) {
        Tier h = CratePools.home(id);
        return h != null ? h : of(id);
    }

    /** Estilo del círculo rúnico que se dibuja al activar una reliquia. */
    public enum Circle {
        NONE, PULSE, RUNE
    }

    private static final Map<String, Circle> CIRCLES = new HashMap<>();

    static {
        for (String id : new String[]{"arcane_tome", "void_singularity", "celestial_aegis", "hourglass", "time_pendulum", "red_thread",
                "world_tree_seed", "fairy_jar", "hatchling_egg", "genie_lamp", "abyss_scepter", "rewind_anchor", "aurora_horn",
                "victory_banner", "meteor_scepter", "soul_lantern", "eternal_rose", "star_charm", "moon_crystal", "heart_fireworks",
                "music_box", "healing_lyre", "home_mirror", "crystal_heart", "oak_heart"}) {
            CIRCLES.put(id, Circle.RUNE);
        }
        for (String id : new String[]{"sun_crown", "storm_eye", "vampire_chalice", "war_horn", "rage_gauntlet", "quake_seal",
                "titan_belt", "frost_bracelet", "specter_mask", "medusa_eye", "warrior_medal", "miner_medallion", "fox_charm",
                "wolf_fang", "fate_coin"}) {
            CIRCLES.put(id, Circle.PULSE);
        }
    }

    public static Circle circle(String id) {
        return CIRCLES.getOrDefault(id, Circle.NONE);
    }

}
