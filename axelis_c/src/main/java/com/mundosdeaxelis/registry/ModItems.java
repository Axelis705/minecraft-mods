package com.mundosdeaxelis.registry;

import com.mundosdeaxelis.MundosDeAxelis;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, MundosDeAxelis.MODID);

    public static final RegistryObject<Item> GRAVE = ITEMS.register("grave",
            () -> new BlockItem(ModBlocks.GRAVE.get(), new Item.Properties().rarity(Rarity.UNCOMMON)));
    public static final RegistryObject<Item> PORTAL_STONE_FOREST = ITEMS.register("portal_stone_forest",
            () -> new com.mundosdeaxelis.item.PortalStoneItem(ModBlocks.PORTAL_STONE_FOREST.get(), new Item.Properties().rarity(Rarity.UNCOMMON), "portal_stone_forest"));

    /** Piedra de Portal de la etapa 2 (Forja). */
    public static final RegistryObject<Item> PORTAL_STONE_FORGE = ITEMS.register("portal_stone_forge",
            () -> new com.mundosdeaxelis.item.PortalStoneItem(ModBlocks.PORTAL_STONE_FORGE.get(), new Item.Properties().rarity(Rarity.UNCOMMON), "portal_stone_forge"));
    public static final RegistryObject<Item> PORTAL_STONE_ICE = ITEMS.register("portal_stone_ice",
            () -> new com.mundosdeaxelis.item.PortalStoneItem(ModBlocks.PORTAL_STONE_ICE.get(), new Item.Properties().rarity(Rarity.UNCOMMON), "portal_stone_ice"));

    /** Brasero Eterno (bloque de calor para el frío). */
    public static final RegistryObject<Item> ETERNAL_BRAZIER = ITEMS.register("eternal_brazier",
            () -> new com.mundosdeaxelis.item.PortalStoneItem(ModBlocks.ETERNAL_BRAZIER.get(), new Item.Properties().rarity(Rarity.RARE), "eternal_brazier"));

    public static final RegistryObject<Item> REY_BROTE_SPAWN_EGG = ITEMS.register("rey_brote_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.REY_BROTE, 0x78D86A, 0xFFE35A, new Item.Properties()));

    public static final RegistryObject<Item> LOBO_ARTICO_SPAWN_EGG = ITEMS.register("lobo_artico_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.LOBO_ARTICO, 0x1A2030, 0x9FE3FF, new Item.Properties()));
    public static final RegistryObject<Item> LOBO_ESCARCHA_SPAWN_EGG = ITEMS.register("lobo_escarcha_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.LOBO_ESCARCHA, 0xD6E4F2, 0x5FB8F0, new Item.Properties()));

    public static final RegistryObject<Item> BROTE_SPAWN_EGG = ITEMS.register("brote_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.BROTE, 0x6B8F3A, 0xE88CB0, new Item.Properties()));
    public static final RegistryObject<Item> ESPINOSO_SPAWN_EGG = ITEMS.register("espinoso_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.ESPINOSO, 0x4E8444, 0xCE4C74, new Item.Properties()));
    public static final RegistryObject<Item> GUARDIAN_MUSGO_SPAWN_EGG = ITEMS.register("guardian_musgo_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.GUARDIAN_MUSGO, 0x707868, 0x96FF78, new Item.Properties()));

    public static final RegistryObject<Item> RAIZAL_SPAWN_EGG = ITEMS.register("raizal_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.RAIZAL, 0x5A3E26, 0xEEB040, new Item.Properties()));
    public static final RegistryObject<Item> POLINIZADOR_SPAWN_EGG = ITEMS.register("polinizador_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.POLINIZADOR, 0x8CCB6A, 0xF4D44C, new Item.Properties()));

    public static final RegistryObject<Item> ESCORIA_SPAWN_EGG = ITEMS.register("escoria_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.ESCORIA, 0x3A2F2C, 0xFF8A1E, new Item.Properties()));
    public static final RegistryObject<Item> CHISPERO_SPAWN_EGG = ITEMS.register("chispero_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.CHISPERO, 0x4A4F58, 0xFFB030, new Item.Properties()));
    public static final RegistryObject<Item> ASCUA_SPAWN_EGG = ITEMS.register("ascua_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.ASCUA, 0x7A2410, 0xFFD060, new Item.Properties()));
    public static final RegistryObject<Item> FUNDIDOR_SPAWN_EGG = ITEMS.register("fundidor_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.FUNDIDOR, 0x5A3A30, 0xFF5A18, new Item.Properties()));
    public static final RegistryObject<Item> CAPATAZ_SPAWN_EGG = ITEMS.register("capataz_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.CAPATAZ, 0x5C6470, 0xE26A22, new Item.Properties()));
    public static final RegistryObject<Item> GRAN_HERRERO_SPAWN_EGG = ITEMS.register("gran_herrero_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.GRAN_HERRERO, 0x2E2A32, 0xFF7A18, new Item.Properties()));

    public static final RegistryObject<Item> VAULT_CHEST = ITEMS.register("vault_chest",
            () -> new BlockItem(ModBlocks.VAULT_CHEST.get(), new Item.Properties().rarity(Rarity.UNCOMMON)));
    public static final RegistryObject<Item> VAULT_CHEST_SEALED = ITEMS.register("vault_chest_sealed",
            () -> new BlockItem(ModBlocks.VAULT_CHEST_SEALED.get(), new Item.Properties().rarity(Rarity.RARE)));
    public static final RegistryObject<Item> VAULT_CHEST_LOCKED = ITEMS.register("vault_chest_locked",
            () -> new BlockItem(ModBlocks.VAULT_CHEST_LOCKED.get(), new Item.Properties().rarity(Rarity.RARE)));
    public static final RegistryObject<Item> VAULT_CHEST_FORGE = ITEMS.register("vault_chest_forge",
            () -> new BlockItem(ModBlocks.VAULT_CHEST_FORGE.get(), new Item.Properties().rarity(Rarity.UNCOMMON)));
    public static final RegistryObject<Item> VAULT_CHEST_SEALED_FORGE = ITEMS.register("vault_chest_sealed_forge",
            () -> new BlockItem(ModBlocks.VAULT_CHEST_SEALED_FORGE.get(), new Item.Properties().rarity(Rarity.RARE)));
    public static final RegistryObject<Item> VAULT_CHEST_LOCKED_FORGE = ITEMS.register("vault_chest_locked_forge",
            () -> new BlockItem(ModBlocks.VAULT_CHEST_LOCKED_FORGE.get(), new Item.Properties().rarity(Rarity.RARE)));
    /** Comida de la dungeon de la Forja: cura, regeneración I (6 s) y absorción I (30 s), con 5 s de espera entre un asado y otro. */
    public static final RegistryObject<Item> FORGE_ROAST = ITEMS.register("forge_roast",
            () -> new com.mundosdeaxelis.item.ForgeRoastItem(new Item.Properties().food(new net.minecraft.world.food.FoodProperties.Builder()
                    .nutrition(6).saturationMod(0.9F).fast().alwaysEat()
                    .effect(() -> new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.REGENERATION, 120, 0), 1.0F)
                    .effect(() -> new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.ABSORPTION, 600, 0), 1.0F)
                    .build()).stacksTo(32).rarity(Rarity.RARE)));
    /** Comida especial de la dungeon del Rey Brote: cura, da regeneración y corazones de absorción. */
    public static final RegistryObject<Item> SANCTUARY_BERRY = ITEMS.register("sanctuary_berry",
            () -> new Item(new Item.Properties().food(new net.minecraft.world.food.FoodProperties.Builder()
                    .nutrition(6).saturationMod(0.9F).fast().alwaysEat()
                    .effect(() -> new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.REGENERATION, 160, 1), 1.0F)
                    .effect(() -> new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.ABSORPTION, 1200, 1), 1.0F)
                    .build()).stacksTo(32).rarity(Rarity.RARE)));
    /** Repara un poco las cuatro piezas de armadura puestas. */
    public static final RegistryObject<Item> ACORN = ITEMS.register("acorn", com.mundosdeaxelis.item.AcornItem::new);
    /** Versión rara: repara mucho más (60 %). Solo sale de cofres difíciles y de la cripta. */
    public static final RegistryObject<Item> GOLDEN_ACORN = ITEMS.register("golden_acorn",
            () -> new com.mundosdeaxelis.item.AcornItem(com.mundosdeaxelis.item.AcornItem.REPAIR_GOLDEN, "golden_acorn", Rarity.RARE));
    /** Dungeon de la Forja: igual que la bellota pero repara el equipo de la Forja (sets Forja y Bronce). */
    public static final RegistryObject<Item> EMBER_RIVET = ITEMS.register("ember_rivet",
            () -> new com.mundosdeaxelis.item.AcornItem(com.mundosdeaxelis.item.AcornItem.REPAIR, "ember_rivet", Rarity.UNCOMMON,
                    "rivet", com.mundosdeaxelis.crate.CrateKind.FORGE));
    public static final RegistryObject<Item> GOLDEN_RIVET = ITEMS.register("golden_rivet",
            () -> new com.mundosdeaxelis.item.AcornItem(com.mundosdeaxelis.item.AcornItem.REPAIR_GOLDEN, "golden_rivet", Rarity.RARE,
                    "rivet", com.mundosdeaxelis.crate.CrateKind.FORGE));
    /** Llave de la dungeon de la Forja: abre los cofres sellados de allí. */
    public static final RegistryObject<Item> FORGE_KEY = ITEMS.register("forge_key",
            () -> new com.mundosdeaxelis.item.DungeonKeyItem("forge_key"));
    /** Acero Ígneo: material de la Forja para crear cosas en la Mesa del Artífice. */
    public static final RegistryObject<Item> EMBER_STEEL = ITEMS.register("ember_steel", com.mundosdeaxelis.item.EmberSteelItem::new);
    public static final RegistryObject<Item> DUNGEON_KEY = ITEMS.register("dungeon_key", com.mundosdeaxelis.item.DungeonKeyItem::new);

    public static final RegistryObject<Item> ARTIFICER_TABLE = ITEMS.register("artificer_table",
            () -> new BlockItem(ModBlocks.ARTIFICER_TABLE.get(), new Item.Properties().rarity(Rarity.RARE)));

    /** Materiales de mobs (17): se refinan en la mesa para obtener Esencia. */
    public static final java.util.Map<com.mundosdeaxelis.table.Mats, RegistryObject<Item>> MATS = registerMats();

    private static java.util.Map<com.mundosdeaxelis.table.Mats, RegistryObject<Item>> registerMats() {
        java.util.Map<com.mundosdeaxelis.table.Mats, RegistryObject<Item>> m = new java.util.EnumMap<>(com.mundosdeaxelis.table.Mats.class);
        for (com.mundosdeaxelis.table.Mats k : com.mundosdeaxelis.table.Mats.values()) {
            m.put(k, ITEMS.register(k.itemId(), () -> new com.mundosdeaxelis.item.MaterialItem(k)));
        }
        return m;
    }

    /** Una taza de café: se bebe y da un poco de velocidad. Aparece en los barriles de R.E.P.O. */
    public static final RegistryObject<Item> COFFEE = ITEMS.register("coffee",
            () -> new com.mundosdeaxelis.item.CoffeeItem(new Item.Properties().stacksTo(16)));

    /** Un dólar: lo da cada mascota rescatada; se cambia por una brújula con el Tendero. */
    public static final RegistryObject<Item> DOLLAR = ITEMS.register("dollar",
            () -> new Item(new Item.Properties().stacksTo(64)));

    // ---------------------------------------------------------------- Caja misteriosa y sus premios

    public static final RegistryObject<Item> FORTUNE_COOKIE = ITEMS.register("fortune_cookie",
            () -> new com.mundosdeaxelis.item.FortuneCookieItem(new Item.Properties().stacksTo(32)));

    public static final RegistryObject<Item> STAR_FEATHER = ITEMS.register("star_feather",
            () -> new com.mundosdeaxelis.item.StarFeatherItem(new Item.Properties().durability(24).rarity(Rarity.UNCOMMON)));

    public static final RegistryObject<Item> STAR_MAGNET = ITEMS.register("star_magnet",
            () -> new com.mundosdeaxelis.item.StarMagnetItem(new Item.Properties().durability(60).rarity(Rarity.RARE)));

    public static final RegistryObject<Item> CONFETTI_CANNON = ITEMS.register("confetti_cannon",
            () -> new com.mundosdeaxelis.item.ConfettiCannonItem(new Item.Properties().durability(40).rarity(Rarity.RARE)));

    public static final RegistryObject<Item> RAINBOW_GRAPPLE = ITEMS.register("rainbow_grapple",
            () -> new com.mundosdeaxelis.item.RainbowGrappleItem(new Item.Properties().durability(120).rarity(Rarity.EPIC)));

    public static final RegistryObject<Item> BUBBLE_PEARL = ITEMS.register("bubble_pearl",
            () -> new com.mundosdeaxelis.item.BubblePearlItem(new Item.Properties().stacksTo(16).rarity(Rarity.UNCOMMON)));

    // ---------------------------------------------------------------- Segundo lote de objetos
    public static final RegistryObject<Item> FIREFLY_LANTERN = ITEMS.register("firefly_lantern",
            () -> new com.mundosdeaxelis.item.FireflyLanternItem(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)));

    public static final RegistryObject<Item> STAR_CLOCK = ITEMS.register("star_clock",
            () -> new com.mundosdeaxelis.item.StarClockItem(new Item.Properties().durability(12).rarity(Rarity.RARE)));

    public static final RegistryObject<Item> FLOWER_WAND = ITEMS.register("flower_wand",
            () -> new com.mundosdeaxelis.item.FlowerWandItem(new Item.Properties().durability(48).rarity(Rarity.UNCOMMON)));

    public static final RegistryObject<Item> UMBRELLA = ITEMS.register("umbrella",
            () -> new com.mundosdeaxelis.item.UmbrellaItem(new Item.Properties().stacksTo(1).durability(400).rarity(Rarity.UNCOMMON)));

    public static final RegistryObject<Item> EXPLORER_COMPASS = ITEMS.register("explorer_compass",
            () -> new com.mundosdeaxelis.item.ExplorerCompassItem(new Item.Properties().durability(40).rarity(Rarity.RARE)));

    public static final RegistryObject<Item> LUCKY_DIE = ITEMS.register("lucky_die",
            () -> new com.mundosdeaxelis.item.LuckyDieItem(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)));

    // ---------------------------------------------------------------- Tercer lote de objetos
    public static final RegistryObject<Item> WATCHER_EYE = ITEMS.register("watcher_eye",
            () -> new com.mundosdeaxelis.item.WatcherEyeItem(new Item.Properties().durability(30).rarity(Rarity.RARE)));

    public static final RegistryObject<Item> COCOA_FLASK = ITEMS.register("cocoa_flask",
            () -> new com.mundosdeaxelis.item.CocoaFlaskItem(new Item.Properties().durability(5).rarity(Rarity.UNCOMMON)));

    public static final RegistryObject<Item> CALM_BELL = ITEMS.register("calm_bell",
            () -> new com.mundosdeaxelis.item.CalmBellItem(new Item.Properties().durability(25).rarity(Rarity.RARE)));

    public static final RegistryObject<Item> RAINBOW_BRUSH = ITEMS.register("rainbow_brush",
            () -> new com.mundosdeaxelis.item.RainbowBrushItem(new Item.Properties().durability(96).rarity(Rarity.UNCOMMON)));

    public static final RegistryObject<Item> BLINK_STONE = ITEMS.register("blink_stone",
            () -> new com.mundosdeaxelis.item.BlinkStoneItem(new Item.Properties().durability(50).rarity(Rarity.EPIC)));

    public static final RegistryObject<Item> RAINBOW_LOLLIPOP = ITEMS.register("rainbow_lollipop",
            () -> new com.mundosdeaxelis.item.LollipopItem(new Item.Properties().food(new net.minecraft.world.food.FoodProperties.Builder().nutrition(3).saturationMod(0.4F).fast().alwaysEat().build()).stacksTo(16).rarity(Rarity.UNCOMMON)));

    // ---------------------------------------------------------------- Reliquias
    public static final RegistryObject<Item> PHOENIX_RING = ITEMS.register("phoenix_ring", com.mundosdeaxelis.relic.PhoenixRingItem::new);
    public static final RegistryObject<Item> FEATHER_TALISMAN = ITEMS.register("feather_talisman", com.mundosdeaxelis.relic.FeatherTalismanItem::new);
    public static final RegistryObject<Item> OAK_HEART = ITEMS.register("oak_heart", com.mundosdeaxelis.relic.OakHeartItem::new);
    public static final RegistryObject<Item> MINER_MEDALLION = ITEMS.register("miner_medallion", com.mundosdeaxelis.relic.MinerMedallionItem::new);
    public static final RegistryObject<Item> TIDE_SHELL = ITEMS.register("tide_shell", com.mundosdeaxelis.relic.TideShellItem::new);
    public static final RegistryObject<Item> WOLF_FANG = ITEMS.register("wolf_fang", com.mundosdeaxelis.relic.WolfFangItem::new);
    public static final RegistryObject<Item> STAR_CHARM = ITEMS.register("star_charm", com.mundosdeaxelis.relic.StarCharmItem::new);
    public static final RegistryObject<Item> FROST_BRACELET = ITEMS.register("frost_bracelet", com.mundosdeaxelis.relic.FrostBraceletItem::new);

    public static final RegistryObject<Item> SUN_CROWN = ITEMS.register("sun_crown", com.mundosdeaxelis.relic.SunCrownItem::new);
    public static final RegistryObject<Item> STORM_EYE = ITEMS.register("storm_eye", com.mundosdeaxelis.relic.StormEyeItem::new);
    public static final RegistryObject<Item> WIND_RING = ITEMS.register("wind_ring", com.mundosdeaxelis.relic.WindRingItem::new);
    /** Anillo del Herrero: solo se consigue creándolo en la Mesa del Artífice. */
    public static final RegistryObject<Item> SMITH_RING = ITEMS.register("smith_ring", com.mundosdeaxelis.relic.SmithRingItem::new);
    public static final RegistryObject<Item> DRAGON_SCALE = ITEMS.register("dragon_scale", com.mundosdeaxelis.relic.DragonScaleItem::new);
    public static final RegistryObject<Item> MOON_CRYSTAL = ITEMS.register("moon_crystal", com.mundosdeaxelis.relic.MoonCrystalItem::new);
    public static final RegistryObject<Item> ETERNAL_ROSE = ITEMS.register("eternal_rose", com.mundosdeaxelis.relic.EternalRoseItem::new);
    public static final RegistryObject<Item> HOURGLASS = ITEMS.register("hourglass", com.mundosdeaxelis.relic.HourglassItem::new);
    public static final RegistryObject<Item> FOX_CHARM = ITEMS.register("fox_charm", com.mundosdeaxelis.relic.FoxCharmItem::new);

    public static final RegistryObject<Item> CRYSTAL_HEART = ITEMS.register("crystal_heart", com.mundosdeaxelis.relic.CrystalHeartItem::new);
    public static final RegistryObject<Item> SPECTER_MASK = ITEMS.register("specter_mask", com.mundosdeaxelis.relic.SpecterMaskItem::new);
    public static final RegistryObject<Item> AURORA_HORN = ITEMS.register("aurora_horn", com.mundosdeaxelis.relic.AuroraHornItem::new);
    public static final RegistryObject<Item> ARCANE_TOME = ITEMS.register("arcane_tome", com.mundosdeaxelis.relic.ArcaneTomeItem::new);
    public static final RegistryObject<Item> WARRIOR_MEDAL = ITEMS.register("warrior_medal", com.mundosdeaxelis.relic.WarriorMedalItem::new);
    public static final RegistryObject<Item> VAMPIRE_CHALICE = ITEMS.register("vampire_chalice", com.mundosdeaxelis.relic.VampireChaliceItem::new);
    public static final RegistryObject<Item> ENDER_ORB = ITEMS.register("ender_orb", com.mundosdeaxelis.relic.EnderOrbItem::new);
    public static final RegistryObject<Item> TITAN_BELT = ITEMS.register("titan_belt", com.mundosdeaxelis.relic.TitanBeltItem::new);
    public static final RegistryObject<Item> VOLTAIC_SCEPTER = ITEMS.register("voltaic_scepter", com.mundosdeaxelis.relic.VoltaicScepterItem::new);
    public static final RegistryObject<Item> WAR_HORN = ITEMS.register("war_horn", com.mundosdeaxelis.relic.WarHornItem::new);
    public static final RegistryObject<Item> HOME_MIRROR = ITEMS.register("home_mirror", com.mundosdeaxelis.relic.HomeMirrorItem::new);
    public static final RegistryObject<Item> MEDUSA_EYE = ITEMS.register("medusa_eye", com.mundosdeaxelis.relic.MedusaEyeItem::new);
    public static final RegistryObject<Item> RAGE_GAUNTLET = ITEMS.register("rage_gauntlet", com.mundosdeaxelis.relic.RageGauntletItem::new);
    public static final RegistryObject<Item> UMBRAL_GLAIVE = ITEMS.register("umbral_glaive", com.mundosdeaxelis.relic.UmbralGlaiveItem::new);
    public static final RegistryObject<Item> MAGMA_BOOTS = ITEMS.register("magma_boots", com.mundosdeaxelis.relic.MagmaBootsItem::new);
    public static final RegistryObject<Item> CLOUD_BOTTLE = ITEMS.register("cloud_bottle", com.mundosdeaxelis.relic.CloudBottleItem::new);
    public static final RegistryObject<Item> VOID_SINGULARITY = ITEMS.register("void_singularity", com.mundosdeaxelis.relic.VoidSingularityItem::new);
    public static final RegistryObject<Item> CELESTIAL_AEGIS = ITEMS.register("celestial_aegis", com.mundosdeaxelis.relic.CelestialAegisItem::new);
    public static final RegistryObject<Item> HEALING_LYRE = ITEMS.register("healing_lyre", com.mundosdeaxelis.relic.HealingLyreItem::new);
    public static final RegistryObject<Item> GENIE_LAMP = ITEMS.register("genie_lamp", com.mundosdeaxelis.relic.GenieLampItem::new);
    public static final RegistryObject<Item> METEOR_SCEPTER = ITEMS.register("meteor_scepter", com.mundosdeaxelis.relic.MeteorScepterItem::new);
    public static final RegistryObject<Item> ECLIPSE_DAGGER = ITEMS.register("eclipse_dagger", com.mundosdeaxelis.relic.EclipseDaggerItem::new);
    public static final RegistryObject<Item> QUAKE_SEAL = ITEMS.register("quake_seal", com.mundosdeaxelis.relic.QuakeSealItem::new);
    public static final RegistryObject<Item> LIGHT_WINGS = ITEMS.register("light_wings", com.mundosdeaxelis.relic.LightWingsItem::new);
    public static final RegistryObject<Item> SOUL_LANTERN = ITEMS.register("soul_lantern", com.mundosdeaxelis.relic.SoulLanternItem::new);
    public static final RegistryObject<Item> PRISM_RAINBOW = ITEMS.register("prism_rainbow", com.mundosdeaxelis.relic.PrismRainbowItem::new);
    public static final RegistryObject<Item> SOLAR_CHAKRAM = ITEMS.register("solar_chakram", com.mundosdeaxelis.relic.SolarChakramItem::new);
    public static final RegistryObject<Item> TIME_PENDULUM = ITEMS.register("time_pendulum", com.mundosdeaxelis.relic.TimePendulumItem::new);
    public static final RegistryObject<Item> STAR_BOW = ITEMS.register("star_bow", com.mundosdeaxelis.relic.StarBowItem::new);
    public static final RegistryObject<Item> COMET_LANCE = ITEMS.register("comet_lance", com.mundosdeaxelis.relic.CometLanceItem::new);
    public static final RegistryObject<Item> MIRROR_SHIELD = ITEMS.register("mirror_shield", com.mundosdeaxelis.relic.MirrorShieldItem::new);
    public static final RegistryObject<Item> ASTRAL_SPYGLASS = ITEMS.register("astral_spyglass", com.mundosdeaxelis.relic.AstralSpyglassItem::new);
    public static final RegistryObject<Item> FANG_STAFF = ITEMS.register("fang_staff", com.mundosdeaxelis.relic.FangStaffItem::new);
    public static final RegistryObject<Item> REWIND_ANCHOR = ITEMS.register("rewind_anchor", com.mundosdeaxelis.relic.RewindAnchorItem::new);
    public static final RegistryObject<Item> FATE_COIN = ITEMS.register("fate_coin", com.mundosdeaxelis.relic.FateCoinItem::new);
    public static final RegistryObject<Item> HEART_FIREWORKS = ITEMS.register("heart_fireworks", com.mundosdeaxelis.relic.HeartFireworksItem::new);
    public static final RegistryObject<Item> RED_THREAD = ITEMS.register("red_thread", com.mundosdeaxelis.relic.RedThreadItem::new);
    public static final RegistryObject<Item> VICTORY_BANNER = ITEMS.register("victory_banner", com.mundosdeaxelis.relic.VictoryBannerItem::new);
    public static final RegistryObject<Item> WHIRLWIND_STAFF = ITEMS.register("whirlwind_staff", com.mundosdeaxelis.relic.WhirlwindStaffItem::new);
    public static final RegistryObject<Item> WORLD_TREE_SEED = ITEMS.register("world_tree_seed", com.mundosdeaxelis.relic.WorldTreeSeedItem::new);
    public static final RegistryObject<Item> HATCHLING_EGG = ITEMS.register("hatchling_egg", com.mundosdeaxelis.relic.HatchlingEggItem::new);
    public static final RegistryObject<Item> FAIRY_JAR = ITEMS.register("fairy_jar", com.mundosdeaxelis.relic.FairyJarItem::new);
    public static final RegistryObject<Item> MOON_BLADE = ITEMS.register("moon_blade", com.mundosdeaxelis.relic.MoonBladeItem::new);
    public static final RegistryObject<Item> ROCKET_PACK = ITEMS.register("rocket_pack", com.mundosdeaxelis.relic.RocketPackItem::new);
    public static final RegistryObject<Item> MUSIC_BOX = ITEMS.register("music_box", com.mundosdeaxelis.relic.MusicBoxItem::new);
    public static final RegistryObject<Item> STAR_HEART = ITEMS.register("star_heart", com.mundosdeaxelis.relic.StarHeartItem::new);
    public static final RegistryObject<Item> DRAGON_WHIP = ITEMS.register("dragon_whip", com.mundosdeaxelis.relic.DragonWhipItem::new);
    public static final RegistryObject<Item> ABYSS_SCEPTER = ITEMS.register("abyss_scepter", com.mundosdeaxelis.relic.AbyssScepterItem::new);

    // Figuritas de compañeros 3D
    public static final RegistryObject<Item> COMP_FOX = ITEMS.register("comp_fox", () -> new com.mundosdeaxelis.relic.CompanionItem(com.mundosdeaxelis.relic.Companions.FOX));
    public static final RegistryObject<Item> COMP_CAT = ITEMS.register("comp_cat", () -> new com.mundosdeaxelis.relic.CompanionItem(com.mundosdeaxelis.relic.Companions.CAT));
    public static final RegistryObject<Item> COMP_BUNNY = ITEMS.register("comp_bunny", () -> new com.mundosdeaxelis.relic.CompanionItem(com.mundosdeaxelis.relic.Companions.BUNNY));
    public static final RegistryObject<Item> COMP_JELLY = ITEMS.register("comp_jelly", () -> new com.mundosdeaxelis.relic.CompanionItem(com.mundosdeaxelis.relic.Companions.JELLY));
    public static final RegistryObject<Item> COMP_PHOENIX = ITEMS.register("comp_phoenix", () -> new com.mundosdeaxelis.relic.CompanionItem(com.mundosdeaxelis.relic.Companions.PHOENIX));
    public static final RegistryObject<Item> COMP_TURTLE = ITEMS.register("comp_turtle", () -> new com.mundosdeaxelis.relic.CompanionItem(com.mundosdeaxelis.relic.Companions.TURTLE));
    public static final RegistryObject<Item> COMP_PENGUIN = ITEMS.register("comp_penguin", () -> new com.mundosdeaxelis.relic.CompanionItem(com.mundosdeaxelis.relic.Companions.PENGUIN));
    public static final RegistryObject<Item> COMP_PANDA = ITEMS.register("comp_panda", () -> new com.mundosdeaxelis.relic.CompanionItem(com.mundosdeaxelis.relic.Companions.PANDA));

    public static final RegistryObject<Item> COMP_CHICK = ITEMS.register("comp_chick", () -> new com.mundosdeaxelis.relic.CompanionItem(com.mundosdeaxelis.relic.Companions.CHICK));
    public static final RegistryObject<Item> COMP_FROG = ITEMS.register("comp_frog", () -> new com.mundosdeaxelis.relic.CompanionItem(com.mundosdeaxelis.relic.Companions.FROG));
    public static final RegistryObject<Item> COMP_HAMSTER = ITEMS.register("comp_hamster", () -> new com.mundosdeaxelis.relic.CompanionItem(com.mundosdeaxelis.relic.Companions.HAMSTER));
    public static final RegistryObject<Item> COMP_SNAIL = ITEMS.register("comp_snail", () -> new com.mundosdeaxelis.relic.CompanionItem(com.mundosdeaxelis.relic.Companions.SNAIL));
    public static final RegistryObject<Item> COMP_BEE = ITEMS.register("comp_bee", () -> new com.mundosdeaxelis.relic.CompanionItem(com.mundosdeaxelis.relic.Companions.BEE));
    public static final RegistryObject<Item> COMP_SHEEP = ITEMS.register("comp_sheep", () -> new com.mundosdeaxelis.relic.CompanionItem(com.mundosdeaxelis.relic.Companions.SHEEP));
    public static final RegistryObject<Item> COMP_HEDGEHOG = ITEMS.register("comp_hedgehog", () -> new com.mundosdeaxelis.relic.CompanionItem(com.mundosdeaxelis.relic.Companions.HEDGEHOG));
    public static final RegistryObject<Item> COMP_AXOLOTL = ITEMS.register("comp_axolotl", () -> new com.mundosdeaxelis.relic.CompanionItem(com.mundosdeaxelis.relic.Companions.AXOLOTL));
    public static final RegistryObject<Item> COMP_RACCOON = ITEMS.register("comp_raccoon", () -> new com.mundosdeaxelis.relic.CompanionItem(com.mundosdeaxelis.relic.Companions.RACCOON));
    public static final RegistryObject<Item> COMP_WHALE = ITEMS.register("comp_whale", () -> new com.mundosdeaxelis.relic.CompanionItem(com.mundosdeaxelis.relic.Companions.WHALE));
    public static final RegistryObject<Item> COMP_KITSUNE = ITEMS.register("comp_kitsune", () -> new com.mundosdeaxelis.relic.CompanionItem(com.mundosdeaxelis.relic.Companions.KITSUNE));
    public static final RegistryObject<Item> COMP_CAPYBARA = ITEMS.register("comp_capybara", () -> new com.mundosdeaxelis.relic.CompanionItem(com.mundosdeaxelis.relic.Companions.CAPYBARA));
    public static final RegistryObject<Item> COMP_DUCK = ITEMS.register("comp_duck", () -> new com.mundosdeaxelis.relic.CompanionItem(com.mundosdeaxelis.relic.Companions.DUCK));
    public static final RegistryObject<Item> COMP_SLIME = ITEMS.register("comp_slime", () -> new com.mundosdeaxelis.relic.CompanionItem(com.mundosdeaxelis.relic.Companions.SLIME));
    public static final RegistryObject<Item> COMP_CORGI = ITEMS.register("comp_corgi", () -> new com.mundosdeaxelis.relic.CompanionItem(com.mundosdeaxelis.relic.Companions.CORGI));
    public static final RegistryObject<Item> COMP_CREEPER = ITEMS.register("comp_creeper", () -> new com.mundosdeaxelis.relic.CompanionItem(com.mundosdeaxelis.relic.Companions.CREEPER));
    public static final RegistryObject<Item> COMP_SNOWMAN = ITEMS.register("comp_snowman", () -> new com.mundosdeaxelis.relic.CompanionItem(com.mundosdeaxelis.relic.Companions.SNOWMAN));
    public static final RegistryObject<Item> COMP_GHOST = ITEMS.register("comp_ghost", () -> new com.mundosdeaxelis.relic.CompanionItem(com.mundosdeaxelis.relic.Companions.GHOST));
    public static final RegistryObject<Item> COMP_GOLEM = ITEMS.register("comp_golem", () -> new com.mundosdeaxelis.relic.CompanionItem(com.mundosdeaxelis.relic.Companions.GOLEM));
    public static final RegistryObject<Item> COMP_SPARKMOUSE = ITEMS.register("comp_sparkmouse", () -> new com.mundosdeaxelis.relic.CompanionItem(com.mundosdeaxelis.relic.Companions.SPARKMOUSE));
    public static final RegistryObject<Item> COMP_DROID = ITEMS.register("comp_droid", () -> new com.mundosdeaxelis.relic.CompanionItem(com.mundosdeaxelis.relic.Companions.DROID));
    public static final RegistryObject<Item> COMP_FOREST_SPIRIT = ITEMS.register("comp_forest_spirit", () -> new com.mundosdeaxelis.relic.CompanionItem(com.mundosdeaxelis.relic.Companions.FOREST_SPIRIT));
    public static final RegistryObject<Item> COMP_MOON_WOLF = ITEMS.register("comp_moon_wolf", () -> new com.mundosdeaxelis.relic.CompanionItem(com.mundosdeaxelis.relic.Companions.MOON_WOLF));
    public static final RegistryObject<Item> COMP_NIGHT_DRAGON = ITEMS.register("comp_night_dragon", () -> new com.mundosdeaxelis.relic.CompanionItem(com.mundosdeaxelis.relic.Companions.NIGHT_DRAGON));
    public static final RegistryObject<Item> COMP_OCTOPUS = ITEMS.register("comp_octopus", () -> new com.mundosdeaxelis.relic.CompanionItem(com.mundosdeaxelis.relic.Companions.OCTOPUS));
    public static final RegistryObject<Item> COMP_BUTTERFLY = ITEMS.register("comp_butterfly", () -> new com.mundosdeaxelis.relic.CompanionItem(com.mundosdeaxelis.relic.Companions.BUTTERFLY));
    public static final RegistryObject<Item> COMP_CRAB = ITEMS.register("comp_crab", () -> new com.mundosdeaxelis.relic.CompanionItem(com.mundosdeaxelis.relic.Companions.CRAB));
    public static final RegistryObject<Item> COMP_FIREFLY = ITEMS.register("comp_firefly", () -> new com.mundosdeaxelis.relic.CompanionItem(com.mundosdeaxelis.relic.Companions.FIREFLY));
    public static final RegistryObject<Item> COMP_ELEPHANT = ITEMS.register("comp_elephant", () -> new com.mundosdeaxelis.relic.CompanionItem(com.mundosdeaxelis.relic.Companions.ELEPHANT));
    public static final RegistryObject<Item> COMP_LION = ITEMS.register("comp_lion", () -> new com.mundosdeaxelis.relic.CompanionItem(com.mundosdeaxelis.relic.Companions.LION));
    public static final RegistryObject<Item> COMP_PEACOCK = ITEMS.register("comp_peacock", () -> new com.mundosdeaxelis.relic.CompanionItem(com.mundosdeaxelis.relic.Companions.PEACOCK));
    public static final RegistryObject<Item> COMP_DOLPHIN = ITEMS.register("comp_dolphin", () -> new com.mundosdeaxelis.relic.CompanionItem(com.mundosdeaxelis.relic.Companions.DOLPHIN));
    public static final RegistryObject<Item> COMP_PEGASUS = ITEMS.register("comp_pegasus", () -> new com.mundosdeaxelis.relic.CompanionItem(com.mundosdeaxelis.relic.Companions.PEGASUS));
    public static final RegistryObject<Item> COMP_CELESTIAL_DRAGON = ITEMS.register("comp_celestial_dragon", () -> new com.mundosdeaxelis.relic.CompanionItem(com.mundosdeaxelis.relic.Companions.CELESTIAL_DRAGON));
    public static final RegistryObject<Item> COMP_MIMIC = ITEMS.register("comp_mimic", () -> new com.mundosdeaxelis.relic.CompanionItem(com.mundosdeaxelis.relic.Companions.MIMIC));
    public static final RegistryObject<Item> COMP_GRIFFON = ITEMS.register("comp_griffon", () -> new com.mundosdeaxelis.relic.CompanionItem(com.mundosdeaxelis.relic.Companions.GRIFFON));
    public static final RegistryObject<Item> COMP_PIGGY = ITEMS.register("comp_piggy", () -> new com.mundosdeaxelis.relic.CompanionItem(com.mundosdeaxelis.relic.Companions.PIGGY));
    public static final RegistryObject<Item> COMP_SKUNK = ITEMS.register("comp_skunk", () -> new com.mundosdeaxelis.relic.CompanionItem(com.mundosdeaxelis.relic.Companions.SKUNK));
    public static final RegistryObject<Item> COMP_SEAL = ITEMS.register("comp_seal", () -> new com.mundosdeaxelis.relic.CompanionItem(com.mundosdeaxelis.relic.Companions.SEAL));
    public static final RegistryObject<Item> COMP_MOONIMP = ITEMS.register("comp_moonimp", () -> new com.mundosdeaxelis.relic.CompanionItem(com.mundosdeaxelis.relic.Companions.MOONIMP));
    public static final RegistryObject<Item> COMP_PATCH = ITEMS.register("comp_patch", () -> new com.mundosdeaxelis.relic.CompanionItem(com.mundosdeaxelis.relic.Companions.PATCH));
    public static final RegistryObject<Item> COMP_TRIKE = ITEMS.register("comp_trike", () -> new com.mundosdeaxelis.relic.CompanionItem(com.mundosdeaxelis.relic.Companions.TRIKE));
    public static final RegistryObject<Item> COMP_YETI = ITEMS.register("comp_yeti", () -> new com.mundosdeaxelis.relic.CompanionItem(com.mundosdeaxelis.relic.Companions.YETI));
    public static final RegistryObject<Item> COMP_SPROUT_KING = ITEMS.register("comp_sprout_king", () -> new com.mundosdeaxelis.relic.CompanionItem(com.mundosdeaxelis.relic.Companions.SPROUT_KING));
    public static final RegistryObject<Item> COMP_EMBER_ANVIL = ITEMS.register("comp_ember_anvil", () -> new com.mundosdeaxelis.relic.CompanionItem(com.mundosdeaxelis.relic.Companions.EMBER_ANVIL));
    public static final RegistryObject<Item> COMP_FERRET = ITEMS.register("comp_ferret", () -> new com.mundosdeaxelis.relic.CompanionItem(com.mundosdeaxelis.relic.Companions.FERRET));

    // ---------------------------------------------------------------- Equipo especial (armas y herramientas)
    public static final RegistryObject<Item> GEAR_DAWN_BLADE = ITEMS.register("gear_dawn_blade",
            () -> new com.mundosdeaxelis.gear.GearSword(com.mundosdeaxelis.gear.Ability.DAWN, new com.mundosdeaxelis.gear.SpecialTier(com.mundosdeaxelis.gear.GearStats.specialSpeed(2), 1, 18, com.mundosdeaxelis.gear.GearStats.specialBonus(com.mundosdeaxelis.gear.Family.SWORD, 3F, 2, 1F)), 3, -2.2F, new Item.Properties().durability(com.mundosdeaxelis.gear.GearStats.specialUses(2, 0))));
    public static final RegistryObject<Item> GEAR_FROST_BLADE = ITEMS.register("gear_frost_blade",
            () -> new com.mundosdeaxelis.gear.GearSword(com.mundosdeaxelis.gear.Ability.FROST, new com.mundosdeaxelis.gear.SpecialTier(com.mundosdeaxelis.gear.GearStats.specialSpeed(6), 2, 21, com.mundosdeaxelis.gear.GearStats.specialBonus(com.mundosdeaxelis.gear.Family.SWORD, 3F, 6, 1F)), 3, -2.4F, new Item.Properties().durability(com.mundosdeaxelis.gear.GearStats.specialUses(6, 1))));
    public static final RegistryObject<Item> GEAR_STORM_BLADE = ITEMS.register("gear_storm_blade",
            () -> new com.mundosdeaxelis.gear.GearSword(com.mundosdeaxelis.gear.Ability.STORM, new com.mundosdeaxelis.gear.SpecialTier(com.mundosdeaxelis.gear.GearStats.specialSpeed(14), 4, 24, com.mundosdeaxelis.gear.GearStats.specialBonus(com.mundosdeaxelis.gear.Family.SWORD, 3F, 14, 1F)), 3, -2.4F, new Item.Properties().durability(com.mundosdeaxelis.gear.GearStats.specialUses(14, 5))));
    public static final RegistryObject<Item> GEAR_VAMPIRE_SABER = ITEMS.register("gear_vampire_saber",
            () -> new com.mundosdeaxelis.gear.GearSword(com.mundosdeaxelis.gear.Ability.VAMPIRE, new com.mundosdeaxelis.gear.SpecialTier(com.mundosdeaxelis.gear.GearStats.specialSpeed(16), 4, 27, com.mundosdeaxelis.gear.GearStats.specialBonus(com.mundosdeaxelis.gear.Family.SWORD, 3F, 16, 1F)), 3, -2.4F, new Item.Properties().durability(com.mundosdeaxelis.gear.GearStats.specialUses(16, 6))));
    public static final RegistryObject<Item> GEAR_DRAGON_SWORD = ITEMS.register("gear_dragon_sword",
            () -> new com.mundosdeaxelis.gear.GearSword(com.mundosdeaxelis.gear.Ability.DRAGON, new com.mundosdeaxelis.gear.SpecialTier(com.mundosdeaxelis.gear.GearStats.specialSpeed(12), 3, 30, com.mundosdeaxelis.gear.GearStats.specialBonus(com.mundosdeaxelis.gear.Family.SWORD, 3F, 12, 1F)), 3, -2.4F, new Item.Properties().durability(com.mundosdeaxelis.gear.GearStats.specialUses(12, 4)).fireResistant()));
    public static final RegistryObject<Item> GEAR_CHEER_PICK = ITEMS.register("gear_cheer_pick",
            () -> new com.mundosdeaxelis.gear.GearPick(com.mundosdeaxelis.gear.Ability.CHEER, new com.mundosdeaxelis.gear.SpecialTier(com.mundosdeaxelis.gear.GearStats.specialSpeed(2), 1, 18, com.mundosdeaxelis.gear.GearStats.specialBonus(com.mundosdeaxelis.gear.Family.PICK, 1F, 2, 1F)), 1, -2.8F, new Item.Properties().durability(com.mundosdeaxelis.gear.GearStats.specialUses(2, 0))));
    public static final RegistryObject<Item> GEAR_PROSPECTOR_PICK = ITEMS.register("gear_prospector_pick",
            () -> new com.mundosdeaxelis.gear.GearPick(com.mundosdeaxelis.gear.Ability.PROSPECTOR, new com.mundosdeaxelis.gear.SpecialTier(com.mundosdeaxelis.gear.GearStats.specialSpeed(4), 2, 21, com.mundosdeaxelis.gear.GearStats.specialBonus(com.mundosdeaxelis.gear.Family.PICK, 1F, 4, 1F)), 1, -2.8F, new Item.Properties().durability(com.mundosdeaxelis.gear.GearStats.specialUses(4, 1))));
    public static final RegistryObject<Item> GEAR_VEIN_PICK = ITEMS.register("gear_vein_pick",
            () -> new com.mundosdeaxelis.gear.GearPick(com.mundosdeaxelis.gear.Ability.VEIN, new com.mundosdeaxelis.gear.SpecialTier(com.mundosdeaxelis.gear.GearStats.specialSpeed(10), 3, 27, com.mundosdeaxelis.gear.GearStats.specialBonus(com.mundosdeaxelis.gear.Family.PICK, 1F, 10, 1F)), 1, -2.8F, new Item.Properties().durability(com.mundosdeaxelis.gear.GearStats.specialUses(10, 3))));
    public static final RegistryObject<Item> GEAR_CORE_PICK = ITEMS.register("gear_core_pick",
            () -> new com.mundosdeaxelis.gear.GearPick(com.mundosdeaxelis.gear.Ability.CORE, new com.mundosdeaxelis.gear.SpecialTier(com.mundosdeaxelis.gear.GearStats.specialSpeed(16), 4, 33, com.mundosdeaxelis.gear.GearStats.specialBonus(com.mundosdeaxelis.gear.Family.PICK, 1F, 16, 2F)), 1, -2.8F, new Item.Properties().durability(com.mundosdeaxelis.gear.GearStats.specialUses(16, 7)).fireResistant()));
    public static final RegistryObject<Item> GEAR_EXCAVATOR = ITEMS.register("gear_excavator",
            () -> new com.mundosdeaxelis.gear.GearShovel(com.mundosdeaxelis.gear.Ability.EXCAVATOR, new com.mundosdeaxelis.gear.SpecialTier(com.mundosdeaxelis.gear.GearStats.specialSpeed(8), 3, 24, com.mundosdeaxelis.gear.GearStats.specialBonus(com.mundosdeaxelis.gear.Family.SHOVEL, 1.5F, 8, 1F)), 1.5F, -3.0F, new Item.Properties().durability(com.mundosdeaxelis.gear.GearStats.specialUses(8, 2))));
    public static final RegistryObject<Item> GEAR_LUMBER_AXE = ITEMS.register("gear_lumber_axe",
            () -> new com.mundosdeaxelis.gear.GearAxe(com.mundosdeaxelis.gear.Ability.LUMBER, new com.mundosdeaxelis.gear.SpecialTier(com.mundosdeaxelis.gear.GearStats.specialSpeed(4), 2, 18, com.mundosdeaxelis.gear.GearStats.specialBonus(com.mundosdeaxelis.gear.Family.AXE, 4.0F, 4, 1F)), 4.0F, -3.1F, new Item.Properties().durability(com.mundosdeaxelis.gear.GearStats.specialUses(4, 0))));
    public static final RegistryObject<Item> GEAR_THUNDER_AXE = ITEMS.register("gear_thunder_axe",
            () -> new com.mundosdeaxelis.gear.GearAxe(com.mundosdeaxelis.gear.Ability.THUNDER, new com.mundosdeaxelis.gear.SpecialTier(com.mundosdeaxelis.gear.GearStats.specialSpeed(14), 4, 30, com.mundosdeaxelis.gear.GearStats.specialBonus(com.mundosdeaxelis.gear.Family.AXE, 4.0F, 14, 1F)), 4.0F, -3.0F, new Item.Properties().durability(com.mundosdeaxelis.gear.GearStats.specialUses(14, 6)).fireResistant()));

    // ---------------------------------------------------------------- Escaleras de equipo por set (espadas, picos, hachas, palas)
    public static final java.util.Map<String, RegistryObject<Item>> SET_GEAR = new java.util.LinkedHashMap<>();

    static {
        for (com.mundosdeaxelis.gear.GearDef d : com.mundosdeaxelis.gear.GearDefs.ALL) {
            com.mundosdeaxelis.crate.Tiers.register(d.id(), d.tier());
            SET_GEAR.put(d.id(), ITEMS.register(d.id(), () -> switch (d.family()) {
                case SWORD -> new com.mundosdeaxelis.gear.LadderSword(d);
                case PICK -> new com.mundosdeaxelis.gear.LadderPick(d);
                case AXE -> new com.mundosdeaxelis.gear.LadderAxe(d);
                case SHOVEL -> new com.mundosdeaxelis.gear.LadderShovel(d);
            }));
        }
    }

    // ---------------------------------------------------------------- Armaduras de los ocho sets (4 piezas cada uno)
    public static final java.util.Map<String, RegistryObject<Item>> SET_ARMOR = new java.util.LinkedHashMap<>();

    static {
        for (com.mundosdeaxelis.gear.GearSet set : com.mundosdeaxelis.gear.GearSet.values()) {
            for (com.mundosdeaxelis.gear.ArmorDefs.Piece piece : com.mundosdeaxelis.gear.ArmorDefs.Piece.values()) {
                String id = com.mundosdeaxelis.gear.ArmorDefs.id(set, piece);
                com.mundosdeaxelis.crate.Tiers.register(id, com.mundosdeaxelis.gear.ArmorDefs.tier(set));
                SET_ARMOR.put(id, ITEMS.register(id, () -> new com.mundosdeaxelis.gear.SetArmor(set, piece)));
            }
        }
    }

    // ---------------------------------------------------------------- Decoración (sale en las cajas)
    public static final java.util.Map<String, RegistryObject<Item>> DECOR = new java.util.LinkedHashMap<>();

    static {
        for (com.mundosdeaxelis.block.DecorDefs.Def d : com.mundosdeaxelis.block.DecorDefs.ALL) {
            DECOR.put(d.id(), ITEMS.register(d.id(),
                    () -> new com.mundosdeaxelis.item.DecorItem(ModBlocks.DECOR.get(d.id()).get(), new Item.Properties().rarity(d.tier().rarity))));
        }
    }

    // ---------------------------------------------------------------- Capas (salen de las cajas; ranura "Capa" del menú de equipo)
    public static final java.util.Map<String, RegistryObject<Item>> CAPES = new java.util.LinkedHashMap<>();

    static {
        for (com.mundosdeaxelis.cape.CapeDefs.Def d : com.mundosdeaxelis.cape.CapeDefs.ALL) {
            CAPES.put(d.id(), ITEMS.register(d.id(), () -> new com.mundosdeaxelis.item.CapeItem(d)));
        }
    }

    // ---------------------------------------------------------------- Crates y llaves
    public static final java.util.Map<com.mundosdeaxelis.crate.CrateKind, RegistryObject<Item>> CRATE_ITEMS = new java.util.EnumMap<>(com.mundosdeaxelis.crate.CrateKind.class);
    public static final java.util.Map<com.mundosdeaxelis.crate.CrateKind, RegistryObject<Item>> KEYS = new java.util.EnumMap<>(com.mundosdeaxelis.crate.CrateKind.class);

    static {
        for (com.mundosdeaxelis.crate.CrateKind k : com.mundosdeaxelis.crate.CrateKind.values()) {
            CRATE_ITEMS.put(k, ITEMS.register(k.blockId(),
                    () -> new BlockItem(ModBlocks.CRATES.get(k).get(), new Item.Properties().rarity(Rarity.EPIC))));
            KEYS.put(k, ITEMS.register(k.keyId(), () -> new com.mundosdeaxelis.item.CrateKeyItem(k)));
        }
    }

    /** Sellos de los jefes (uno por caja de colección): se gastan al forjar la caja siguiente. */
    public static final java.util.Map<com.mundosdeaxelis.crate.CrateKind, RegistryObject<Item>> SEALS = new java.util.EnumMap<>(com.mundosdeaxelis.crate.CrateKind.class);

    static {
        for (com.mundosdeaxelis.crate.CrateKind k : com.mundosdeaxelis.crate.CrateKind.values()) {
            if (k != com.mundosdeaxelis.crate.CrateKind.PETS) {
                SEALS.put(k, ITEMS.register("seal_" + k.id, () -> new com.mundosdeaxelis.item.SealItem(k)));
            }
        }
    }

    public static final RegistryObject<Item> POCKET_BACKPACK = ITEMS.register("pocket_backpack",
            () -> new com.mundosdeaxelis.item.PocketBackpackItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE)));

    public static final RegistryObject<Item> HEART_LOCKET = ITEMS.register("heart_locket",
            () -> new com.mundosdeaxelis.item.HeartLocketItem(new Item.Properties().durability(12).rarity(Rarity.EPIC)));

    private ModItems() {
    }
}
