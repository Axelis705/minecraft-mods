package com.mundosdeaxelis.block;

import com.mundosdeaxelis.registry.ModItems;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.item.enchantment.Enchantments;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

/** Lo que puede salir de una caja misteriosa, según su categoría. */
public final class MysteryLoot {
    private record Entry(int weight, Function<RandomSource, ItemStack> make) {
    }

    private static final Map<BoxTier, List<Entry>> TABLE = new EnumMap<>(BoxTier.class);

    private MysteryLoot() {
    }

    private static void add(BoxTier tier, int weight, Function<RandomSource, ItemStack> make) {
        TABLE.computeIfAbsent(tier, k -> new ArrayList<>()).add(new Entry(weight, make));
    }

    private static Function<RandomSource, ItemStack> of(Supplier<? extends Item> item, int min, int max) {
        return r -> new ItemStack(item.get(), min + (max > min ? r.nextInt(max - min + 1) : 0));
    }

    private static Function<RandomSource, ItemStack> vanilla(Item item, int min, int max) {
        return r -> new ItemStack(item, min + (max > min ? r.nextInt(max - min + 1) : 0));
    }

    private static Function<RandomSource, ItemStack> enchanted(Item item, int level) {
        return r -> EnchantmentHelper.enchantItem(r, new ItemStack(item), level, true);
    }

    private static Function<RandomSource, ItemStack> book(net.minecraft.world.item.enchantment.Enchantment e, int lvl) {
        return r -> EnchantedBookItem.createForEnchantment(new EnchantmentInstance(e, lvl));
    }

    static {
        // ---- Comunes ----
        add(BoxTier.COMMON, 20, of(ModItems.FORTUNE_COOKIE, 2, 4));
        add(BoxTier.COMMON, 10, of(ModItems.COFFEE, 1, 2));
        add(BoxTier.COMMON, 10, of(ModItems.DOLLAR, 1, 3));
        add(BoxTier.COMMON, 8, of(ModItems.STAR_FEATHER, 1, 1));
        add(BoxTier.COMMON, 8, of(ModItems.BUBBLE_PEARL, 1, 3));
        add(BoxTier.COMMON, 7, of(ModItems.LUCKY_DIE, 1, 1));
        add(BoxTier.COMMON, 6, of(ModItems.FLOWER_WAND, 1, 1));
        add(BoxTier.COMMON, 9, of(ModItems.RAINBOW_LOLLIPOP, 2, 5));
        add(BoxTier.COMMON, 6, of(ModItems.COCOA_FLASK, 1, 1));
        add(BoxTier.COMMON, 10, vanilla(Items.COOKED_BEEF, 4, 8));
        add(BoxTier.COMMON, 8, vanilla(Items.GOLDEN_CARROT, 3, 6));
        add(BoxTier.COMMON, 8, vanilla(Items.IRON_INGOT, 3, 7));
        add(BoxTier.COMMON, 6, vanilla(Items.EMERALD, 1, 3));
        add(BoxTier.COMMON, 6, vanilla(Items.EXPERIENCE_BOTTLE, 3, 6));
        add(BoxTier.COMMON, 4, vanilla(Items.CAKE, 1, 1));

        // Las llaves de caja ya no salen como botín: solo se compran con Esencia en la Mesa del Artífice.

        // ---- Raras ----
        add(BoxTier.RARE, 12, of(ModItems.CONFETTI_CANNON, 1, 1));
        add(BoxTier.RARE, 12, of(ModItems.STAR_MAGNET, 1, 1));
        add(BoxTier.RARE, 10, of(ModItems.STAR_CLOCK, 1, 1));
        add(BoxTier.RARE, 9, of(ModItems.WATCHER_EYE, 1, 1));
        add(BoxTier.RARE, 8, of(ModItems.FEATHER_TALISMAN, 1, 1));
        add(BoxTier.RARE, 8, of(ModItems.OAK_HEART, 1, 1));
        add(BoxTier.RARE, 8, of(ModItems.MINER_MEDALLION, 1, 1));
        add(BoxTier.RARE, 8, of(ModItems.TIDE_SHELL, 1, 1));
        add(BoxTier.RARE, 8, of(ModItems.WIND_RING, 1, 1));
        add(BoxTier.RARE, 8, of(ModItems.MOON_CRYSTAL, 1, 1));
        add(BoxTier.RARE, 6, of(ModItems.ASTRAL_SPYGLASS, 1, 1));
        add(BoxTier.RARE, 6, of(ModItems.FANG_STAFF, 1, 1));
        add(BoxTier.RARE, 6, of(ModItems.FATE_COIN, 1, 1));
        add(BoxTier.RARE, 6, of(ModItems.WHIRLWIND_STAFF, 1, 1));
        add(BoxTier.RARE, 6, of(ModItems.FAIRY_JAR, 1, 1));
        add(BoxTier.RARE, 6, of(ModItems.ROCKET_PACK, 1, 1));
        add(BoxTier.RARE, 6, of(ModItems.MUSIC_BOX, 1, 1));
        add(BoxTier.RARE, 8, of(ModItems.FOX_CHARM, 1, 1));
        add(BoxTier.RARE, 8, of(ModItems.SPECTER_MASK, 1, 1));
        add(BoxTier.RARE, 8, of(ModItems.WARRIOR_MEDAL, 1, 1));
        add(BoxTier.RARE, 8, of(ModItems.TITAN_BELT, 1, 1));
        add(BoxTier.RARE, 8, of(ModItems.CLOUD_BOTTLE, 1, 1));
        add(BoxTier.RARE, 8, of(ModItems.MAGMA_BOOTS, 1, 1));
        add(BoxTier.RARE, 8, of(ModItems.HEALING_LYRE, 1, 1));
        add(BoxTier.RARE, 8, of(ModItems.QUAKE_SEAL, 1, 1));
        add(BoxTier.RARE, 8, of(ModItems.SOLAR_CHAKRAM, 1, 1));
        add(BoxTier.RARE, 9, of(ModItems.CALM_BELL, 1, 1));
        add(BoxTier.RARE, 9, of(ModItems.RAINBOW_BRUSH, 1, 1));
        add(BoxTier.RARE, 10, of(ModItems.EXPLORER_COMPASS, 1, 1));
        add(BoxTier.RARE, 8, of(ModItems.FIREFLY_LANTERN, 1, 1));
        add(BoxTier.RARE, 8, of(ModItems.UMBRELLA, 1, 1));
        add(BoxTier.RARE, 10, of(ModItems.RAINBOW_GRAPPLE, 1, 1));
        add(BoxTier.RARE, 10, of(ModItems.POCKET_BACKPACK, 1, 1));
        add(BoxTier.RARE, 8, of(ModItems.FORTUNE_COOKIE, 6, 10));
        add(BoxTier.RARE, 10, vanilla(Items.GOLDEN_APPLE, 1, 2));
        add(BoxTier.RARE, 10, vanilla(Items.DIAMOND, 1, 3));
        add(BoxTier.RARE, 8, vanilla(Items.ENDER_PEARL, 3, 6));
        add(BoxTier.RARE, 8, vanilla(Items.EXPERIENCE_BOTTLE, 10, 16));
        add(BoxTier.RARE, 6, vanilla(Items.NAME_TAG, 1, 2));
        add(BoxTier.RARE, 6, book(Enchantments.UNBREAKING, 3));

        // ---- Épicas ----
        add(BoxTier.EPIC, 12, of(ModItems.HEART_LOCKET, 1, 1));
        add(BoxTier.EPIC, 12, of(ModItems.BLINK_STONE, 1, 1));
        add(BoxTier.EPIC, 10, of(ModItems.WOLF_FANG, 1, 1));
        add(BoxTier.EPIC, 10, of(ModItems.FROST_BRACELET, 1, 1));
        add(BoxTier.EPIC, 9, of(ModItems.SUN_CROWN, 1, 1));
        add(BoxTier.EPIC, 9, of(ModItems.STORM_EYE, 1, 1));
        add(BoxTier.EPIC, 9, of(ModItems.DRAGON_SCALE, 1, 1));
        add(BoxTier.EPIC, 9, of(ModItems.ETERNAL_ROSE, 1, 1));
        add(BoxTier.EPIC, 7, of(ModItems.HOURGLASS, 1, 1));
        add(BoxTier.EPIC, 8, of(ModItems.CRYSTAL_HEART, 1, 1));
        add(BoxTier.EPIC, 8, of(ModItems.AURORA_HORN, 1, 1));
        add(BoxTier.EPIC, 8, of(ModItems.ARCANE_TOME, 1, 1));
        add(BoxTier.EPIC, 8, of(ModItems.VAMPIRE_CHALICE, 1, 1));
        add(BoxTier.EPIC, 8, of(ModItems.ENDER_ORB, 1, 1));
        add(BoxTier.EPIC, 7, of(ModItems.VOLTAIC_SCEPTER, 1, 1));
        add(BoxTier.EPIC, 7, of(ModItems.WAR_HORN, 1, 1));
        add(BoxTier.EPIC, 7, of(ModItems.HOME_MIRROR, 1, 1));
        add(BoxTier.EPIC, 7, of(ModItems.MEDUSA_EYE, 1, 1));
        add(BoxTier.EPIC, 7, of(ModItems.RAGE_GAUNTLET, 1, 1));
        add(BoxTier.EPIC, 7, of(ModItems.UMBRAL_GLAIVE, 1, 1));
        add(BoxTier.EPIC, 7, of(ModItems.VOID_SINGULARITY, 1, 1));
        add(BoxTier.EPIC, 7, of(ModItems.CELESTIAL_AEGIS, 1, 1));
        add(BoxTier.EPIC, 7, of(ModItems.GENIE_LAMP, 1, 1));
        add(BoxTier.EPIC, 7, of(ModItems.METEOR_SCEPTER, 1, 1));
        add(BoxTier.EPIC, 7, of(ModItems.ECLIPSE_DAGGER, 1, 1));
        add(BoxTier.EPIC, 7, of(ModItems.LIGHT_WINGS, 1, 1));
        add(BoxTier.EPIC, 7, of(ModItems.SOUL_LANTERN, 1, 1));
        add(BoxTier.EPIC, 7, of(ModItems.PRISM_RAINBOW, 1, 1));
        add(BoxTier.EPIC, 7, of(ModItems.TIME_PENDULUM, 1, 1));
        add(BoxTier.EPIC, 7, of(ModItems.CLOUD_BOTTLE, 1, 1));
        add(BoxTier.EPIC, 7, of(ModItems.MAGMA_BOOTS, 1, 1));
        add(BoxTier.EPIC, 7, of(ModItems.HEALING_LYRE, 1, 1));
        add(BoxTier.EPIC, 7, of(ModItems.QUAKE_SEAL, 1, 1));
        add(BoxTier.EPIC, 7, of(ModItems.SOLAR_CHAKRAM, 1, 1));
        add(BoxTier.EPIC, 7, of(ModItems.STAR_BOW, 1, 1));
        add(BoxTier.EPIC, 7, of(ModItems.COMET_LANCE, 1, 1));
        add(BoxTier.EPIC, 7, of(ModItems.MIRROR_SHIELD, 1, 1));
        add(BoxTier.EPIC, 7, of(ModItems.REWIND_ANCHOR, 1, 1));
        add(BoxTier.EPIC, 7, of(ModItems.HEART_FIREWORKS, 1, 1));
        add(BoxTier.EPIC, 7, of(ModItems.RED_THREAD, 1, 1));
        add(BoxTier.EPIC, 7, of(ModItems.VICTORY_BANNER, 1, 1));
        add(BoxTier.EPIC, 7, of(ModItems.WORLD_TREE_SEED, 1, 1));
        add(BoxTier.EPIC, 7, of(ModItems.HATCHLING_EGG, 1, 1));
        add(BoxTier.EPIC, 7, of(ModItems.MOON_BLADE, 1, 1));
        add(BoxTier.EPIC, 7, of(ModItems.STAR_HEART, 1, 1));
        add(BoxTier.EPIC, 7, of(ModItems.DRAGON_WHIP, 1, 1));
        add(BoxTier.EPIC, 7, of(ModItems.ABYSS_SCEPTER, 1, 1));
        add(BoxTier.EPIC, 5, of(ModItems.FATE_COIN, 1, 1));
        add(BoxTier.EPIC, 5, of(ModItems.WHIRLWIND_STAFF, 1, 1));
        add(BoxTier.EPIC, 5, of(ModItems.FANG_STAFF, 1, 1));
        add(BoxTier.EPIC, 5, of(ModItems.MUSIC_BOX, 1, 1));
        add(BoxTier.EPIC, 5, of(ModItems.ROCKET_PACK, 1, 1));
        add(BoxTier.EPIC, 5, of(ModItems.FAIRY_JAR, 1, 1));
        add(BoxTier.EPIC, 5, of(ModItems.ASTRAL_SPYGLASS, 1, 1));
        add(BoxTier.EPIC, 9, of(ModItems.OAK_HEART, 1, 1));
        add(BoxTier.EPIC, 9, of(ModItems.TIDE_SHELL, 1, 1));
        add(BoxTier.EPIC, 10, of(ModItems.RAINBOW_GRAPPLE, 1, 1));
        add(BoxTier.EPIC, 8, of(ModItems.POCKET_BACKPACK, 1, 1));
        add(BoxTier.EPIC, 10, vanilla(Items.DIAMOND, 4, 7));
        add(BoxTier.EPIC, 8, vanilla(Items.ENCHANTED_GOLDEN_APPLE, 1, 1));
        add(BoxTier.EPIC, 8, vanilla(Items.NETHERITE_SCRAP, 2, 4));
        add(BoxTier.EPIC, 8, enchanted(Items.DIAMOND_PICKAXE, 30));
        add(BoxTier.EPIC, 8, enchanted(Items.DIAMOND_SWORD, 30));
        add(BoxTier.EPIC, 6, vanilla(Items.SHULKER_SHELL, 2, 3));
        add(BoxTier.EPIC, 6, book(Enchantments.MENDING, 1));

        // ---- Legendarias ----
        add(BoxTier.LEGENDARY, 10, of(ModItems.HEART_LOCKET, 1, 1));
        add(BoxTier.LEGENDARY, 14, of(ModItems.PHOENIX_RING, 1, 1));
        add(BoxTier.LEGENDARY, 14, of(ModItems.STAR_CHARM, 1, 1));
        add(BoxTier.LEGENDARY, 12, of(ModItems.SUN_CROWN, 1, 1));
        add(BoxTier.LEGENDARY, 12, of(ModItems.HOURGLASS, 1, 1));
        add(BoxTier.LEGENDARY, 12, of(ModItems.ETERNAL_ROSE, 1, 1));
        add(BoxTier.LEGENDARY, 10, of(ModItems.ARCANE_TOME, 1, 1));
        add(BoxTier.LEGENDARY, 10, of(ModItems.AURORA_HORN, 1, 1));
        add(BoxTier.LEGENDARY, 10, of(ModItems.CRYSTAL_HEART, 1, 1));
        add(BoxTier.LEGENDARY, 10, of(ModItems.VOID_SINGULARITY, 1, 1));
        add(BoxTier.LEGENDARY, 10, of(ModItems.CELESTIAL_AEGIS, 1, 1));
        add(BoxTier.LEGENDARY, 10, of(ModItems.TIME_PENDULUM, 1, 1));
        add(BoxTier.LEGENDARY, 10, of(ModItems.GENIE_LAMP, 1, 1));
        add(BoxTier.LEGENDARY, 10, of(ModItems.VOLTAIC_SCEPTER, 1, 1));
        add(BoxTier.LEGENDARY, 10, of(ModItems.METEOR_SCEPTER, 1, 1));
        add(BoxTier.LEGENDARY, 10, of(ModItems.STAR_BOW, 1, 1));
        add(BoxTier.LEGENDARY, 10, of(ModItems.STAR_HEART, 1, 1));
        add(BoxTier.LEGENDARY, 10, of(ModItems.ABYSS_SCEPTER, 1, 1));
        add(BoxTier.LEGENDARY, 10, of(ModItems.REWIND_ANCHOR, 1, 1));
        add(BoxTier.LEGENDARY, 10, of(ModItems.WORLD_TREE_SEED, 1, 1));
        add(BoxTier.LEGENDARY, 10, of(ModItems.HATCHLING_EGG, 1, 1));
        add(BoxTier.LEGENDARY, 10, of(ModItems.RED_THREAD, 1, 1));
        add(BoxTier.LEGENDARY, 10, of(ModItems.HEART_FIREWORKS, 1, 1));
        add(BoxTier.LEGENDARY, 10, of(ModItems.DRAGON_WHIP, 1, 1));
        add(BoxTier.LEGENDARY, 10, of(ModItems.MOON_BLADE, 1, 1));
        add(BoxTier.LEGENDARY, 10, of(ModItems.COMET_LANCE, 1, 1));
        add(BoxTier.LEGENDARY, 10, of(ModItems.MIRROR_SHIELD, 1, 1));
        add(BoxTier.LEGENDARY, 10, of(ModItems.VICTORY_BANNER, 1, 1));
        add(BoxTier.LEGENDARY, 10, vanilla(Items.ELYTRA, 1, 1));
        // Figuritas de compañeros
        add(BoxTier.RARE, 5, of(ModItems.COMP_FOX, 1, 1));
        add(BoxTier.RARE, 5, of(ModItems.COMP_CAT, 1, 1));
        add(BoxTier.RARE, 5, of(ModItems.COMP_BUNNY, 1, 1));
        add(BoxTier.RARE, 5, of(ModItems.COMP_TURTLE, 1, 1));
        add(BoxTier.RARE, 5, of(ModItems.COMP_PENGUIN, 1, 1));
        add(BoxTier.EPIC, 4, of(ModItems.COMP_FOX, 1, 1));
        add(BoxTier.EPIC, 4, of(ModItems.COMP_CAT, 1, 1));
        add(BoxTier.EPIC, 4, of(ModItems.COMP_BUNNY, 1, 1));
        add(BoxTier.EPIC, 4, of(ModItems.COMP_TURTLE, 1, 1));
        add(BoxTier.EPIC, 4, of(ModItems.COMP_PENGUIN, 1, 1));
        add(BoxTier.EPIC, 4, of(ModItems.COMP_JELLY, 1, 1));
        add(BoxTier.EPIC, 4, of(ModItems.COMP_PHOENIX, 1, 1));
        add(BoxTier.EPIC, 4, of(ModItems.COMP_PANDA, 1, 1));
        add(BoxTier.LEGENDARY, 8, of(ModItems.COMP_JELLY, 1, 1));
        add(BoxTier.LEGENDARY, 8, of(ModItems.COMP_PHOENIX, 1, 1));
        add(BoxTier.LEGENDARY, 8, of(ModItems.COMP_PANDA, 1, 1));
        add(BoxTier.LEGENDARY, 8, of(ModItems.COMP_FOX, 1, 1));
        add(BoxTier.LEGENDARY, 8, of(ModItems.COMP_CAT, 1, 1));

        add(BoxTier.LEGENDARY, 10, vanilla(Items.TOTEM_OF_UNDYING, 1, 2));
        add(BoxTier.LEGENDARY, 8, vanilla(Items.NETHERITE_INGOT, 1, 2));
        add(BoxTier.LEGENDARY, 8, vanilla(Items.NETHER_STAR, 1, 1));
        add(BoxTier.LEGENDARY, 6, vanilla(Items.BEACON, 1, 1));
        add(BoxTier.LEGENDARY, 6, vanilla(Items.DIAMOND_BLOCK, 1, 2));
        add(BoxTier.LEGENDARY, 6, enchanted(Items.NETHERITE_PICKAXE, 40));
    }

    /** Un objeto al azar de la categoría. */
    public static ItemStack roll(RandomSource r, BoxTier tier) {
        List<Entry> list = TABLE.get(tier);
        int total = 0;
        for (Entry e : list) {
            total += e.weight();
        }
        int n = r.nextInt(total);
        for (Entry e : list) {
            n -= e.weight();
            if (n < 0) {
                return e.make().apply(r);
            }
        }
        return list.get(0).make().apply(r);
    }

    /** Un objeto extra: se saca de una categoría igual o un escalón abajo, para que haya variedad. */
    public static ItemStack rollExtra(RandomSource r, BoxTier tier) {
        BoxTier t = tier.ordinal() > 0 && r.nextBoolean() ? BoxTier.byIndex(tier.ordinal() - 1) : tier;
        return roll(r, t);
    }
}
