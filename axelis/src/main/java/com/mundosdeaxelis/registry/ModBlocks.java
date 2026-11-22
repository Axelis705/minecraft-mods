package com.mundosdeaxelis.registry;

import com.mundosdeaxelis.MundosDeAxelis;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, MundosDeAxelis.MODID);

    /** Piedra de Portal de la etapa 1 (Naturaleza): lleva a la arena del Rey Brote. */
    public static final RegistryObject<Block> PORTAL_STONE_FOREST = BLOCKS.register("portal_stone_forest",
            () -> new com.mundosdeaxelis.block.BossPortalBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_GREEN)
                    .strength(3.0F, 1200.0F)
                    .sound(SoundType.MOSS)
                    .lightLevel(state -> 9), 1));

    /** Piedra de Portal de la etapa 2 (Forja): lleva a la arena del Gran Herrero. */
    public static final RegistryObject<Block> PORTAL_STONE_FORGE = BLOCKS.register("portal_stone_forge",
            () -> new com.mundosdeaxelis.block.BossPortalBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_ORANGE)
                    .strength(3.0F, 1200.0F)
                    .sound(SoundType.DEEPSLATE_BRICKS)
                    .lightLevel(state -> 11), 2));

    /** Piedra de Portal de la etapa 3 (Hielo): lleva a la sala del Lobo Ártico. */
    public static final RegistryObject<Block> PORTAL_STONE_ICE = BLOCKS.register("portal_stone_ice",
            () -> new com.mundosdeaxelis.block.BossPortalBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.ICE)
                    .strength(3.0F, 1200.0F)
                    .sound(SoundType.GLASS)
                    .lightLevel(state -> 10), 3));

    /** Piedra de Portal de la etapa 4 (Volcán): lleva al corazón del volcán, con el Corazón de Magma. */
    public static final RegistryObject<Block> PORTAL_STONE_VOLCANO = BLOCKS.register("portal_stone_volcano",
            () -> new com.mundosdeaxelis.block.BossPortalBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_ORANGE)
                    .strength(3.0F, 1200.0F)
                    .sound(SoundType.NETHER_BRICKS)
                    .lightLevel(state -> 12), 4));

    /** Piedra de Portal de la etapa 5 (Torre): lleva a la cima de la torre del Archimago. */
    public static final RegistryObject<Block> PORTAL_STONE_TOWER = BLOCKS.register("portal_stone_tower",
            () -> new com.mundosdeaxelis.block.BossPortalBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE)
                    .strength(3.0F, 1200.0F)
                    .sound(SoundType.AMETHYST)
                    .lightLevel(state -> 11), 5));

    /** Piedra de regreso (etapa 263): junto al altar del volcán, lleva a la entrada del volcán. */
    public static final RegistryObject<Block> PIEDRA_REGRESO = BLOCKS.register("piedra_regreso",
            () -> new com.mundosdeaxelis.block.PiedraRegresoBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BLACK)
                    .strength(-1.0F, 3600000.0F)
                    .sound(SoundType.GILDED_BLACKSTONE)
                    .noLootTable()
                    .noOcclusion()
                    .lightLevel(state -> 10)
                    .pushReaction(PushReaction.BLOCK)));

    /** Lava falsa del cráter del volcán: se ve como lava pero no quema ni tiene suelo (caes despacio a la dungeon). */
    public static final RegistryObject<Block> LAVA_FALSA = BLOCKS.register("lava_falsa",
            () -> new com.mundosdeaxelis.block.LavaFalsaBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.FIRE)
                    .noCollission()
                    .noOcclusion()
                    .strength(-1.0F, 3600000.0F)
                    .noLootTable()
                    .lightLevel(state -> 15)
                    .isSuffocating((st, l, p) -> false)
                    .isViewBlocking((st, l, p) -> false)
                    .isValidSpawn((st, l, p, t) -> false)
                    .pushReaction(PushReaction.BLOCK)));

    /** Corazón del volcán: parece lava falsa, pero de vez en cuando hace erupciones de adorno si hay alguien cerca. */
    public static final RegistryObject<Block> NUCLEO_VOLCAN = BLOCKS.register("nucleo_volcan",
            () -> new com.mundosdeaxelis.block.NucleoVolcanBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.FIRE)
                    .noCollission()
                    .noOcclusion()
                    .strength(-1.0F, 3600000.0F)
                    .noLootTable()
                    .lightLevel(state -> 15)
                    .randomTicks()
                    .isSuffocating((st, l, p) -> false)
                    .isViewBlocking((st, l, p) -> false)
                    .isValidSpawn((st, l, p, t) -> false)
                    .pushReaction(PushReaction.BLOCK)));

    /** Basalto agrietado de la dungeon del volcán: al pisarlo cruje, cae y a los 10 s se vuelve a formar. */
    public static final RegistryObject<Block> BASALTO_AGRIETADO = BLOCKS.register("basalto_agrietado",
            () -> new com.mundosdeaxelis.block.BasaltoAgrietadoBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BLACK)
                    .strength(-1.0F, 3600000.0F)
                    .noLootTable()
                    .noOcclusion()
                    .sound(SoundType.BASALT)
                    .isValidSpawn((st, l, p, t) -> false)
                    .pushReaction(PushReaction.BLOCK)
                    .lightLevel(state -> switch (state.getValue(com.mundosdeaxelis.block.BasaltoAgrietadoBlock.STAGE)) {
                        case 1 -> 5;
                        case 2 -> 9;
                        default -> 2;
                    })));

    /** Respiradero volcánico de la dungeon del volcán: erupciones de fuego con aviso. */
    public static final RegistryObject<Block> RESPIRADERO_VOLCAN = BLOCKS.register("respiradero_volcan",
            () -> new com.mundosdeaxelis.block.RespiraderoBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.NETHER)
                    .strength(-1.0F, 3600000.0F)
                    .noLootTable()
                    .sound(SoundType.BASALT)
                    .isValidSpawn((st, l, p, t) -> false)
                    .pushReaction(PushReaction.BLOCK)
                    .lightLevel(state -> {
                        int ph = state.getValue(com.mundosdeaxelis.block.RespiraderoBlock.PHASE);
                        return ph >= 2 ? 14 : ph == 1 ? 9 : 5;
                    })));

    /**
     * Semilla de casa puesta en el suelo (etapa 260): marca dónde crecerá la casa de un habitante y muestra un holograma de la casa.
     * Se rompe al momento y vuelve a la mano; ni las explosiones, ni los pistones, ni el agua se la llevan.
     */
    public static final RegistryObject<Block> SEMILLA_CASA = BLOCKS.register("semilla_casa",
            () -> new com.mundosdeaxelis.npc.SemillaCasaBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_GREEN)
                    .strength(0.2F, 3600000.0F)
                    .sound(SoundType.ROOTED_DIRT)
                    .noOcclusion()
                    .forceSolidOn()
                    .noLootTable()
                    .pushReaction(PushReaction.BLOCK)
                    .lightLevel(state -> 6)));

    /** Fumarola del volcán (etapa 255): echa vapor sin parar; adorno de la ladera, sin daño. */
    public static final RegistryObject<Block> FUMAROLA = BLOCKS.register("fumarola",
            () -> new com.mundosdeaxelis.block.FumarolaBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_YELLOW)
                    .strength(1.5F, 6.0F)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.BASALT)
                    .lightLevel(state -> 5)));

    /** Roca de ceniza del volcán (etapa 255): en los techos de la dungeon, deja caer ceniza y a veces retumba. */
    public static final RegistryObject<Block> CENIZA_VOLCAN = BLOCKS.register("ceniza_volcan",
            () -> new com.mundosdeaxelis.block.CenizaVolcanBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_GRAY)
                    .strength(1.25F, 4.2F)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.BASALT)));

    /** Veta de magma (etapa 255): roca con grietas que brillan en las paredes del volcán. */
    public static final RegistryObject<Block> VETA_MAGMA = BLOCKS.register("veta_magma",
            () -> new com.mundosdeaxelis.block.VetaMagmaBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.NETHER)
                    .strength(1.5F, 6.0F)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.BASALT)
                    .lightLevel(state -> 6)));

    /** Brasero del volcán (etapa 255): el acertijo de fuego de la sala de erupciones. */
    public static final RegistryObject<Block> BRASERO_VOLCAN = BLOCKS.register("brasero_volcan",
            () -> new com.mundosdeaxelis.block.BraseroVolcanBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BLACK)
                    .strength(-1.0F, 3600000.0F)
                    .noLootTable()
                    .noOcclusion()
                    .sound(SoundType.METAL)
                    .pushReaction(PushReaction.BLOCK)
                    .lightLevel(state -> state.getValue(com.mundosdeaxelis.block.BraseroVolcanBlock.LIT) ? 15 : 3)));

    /** Reja del volcán (etapa 255): se abre al vencer al guardián o al resolver el acertijo de los braseros. */
    public static final RegistryObject<Block> REJA_VOLCAN = BLOCKS.register("reja_volcan",
            () -> new com.mundosdeaxelis.block.RejaVolcanBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BLACK)
                    .strength(-1.0F, 3600000.0F)
                    .noLootTable()
                    .noOcclusion()
                    .sound(SoundType.CHAIN)
                    .isValidSpawn((st, l, p, t) -> false)
                    .pushReaction(PushReaction.BLOCK)));

    /** Brasero Eterno: fuente de calor de las dungeons de frío (luz 15 encendido). */
    public static final RegistryObject<Block> ETERNAL_BRAZIER = BLOCKS.register("eternal_brazier",
            () -> new com.mundosdeaxelis.block.EternalBrazierBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_ORANGE)
                    .strength(3.5F, 6.0F)
                    .sound(SoundType.METAL)
                    .noOcclusion()
                    .lightLevel(state -> state.getValue(com.mundosdeaxelis.block.EternalBrazierBlock.LIT) ? 15 : 0)));

    /** Cofres de la dungeon: botín propio para cada jugador. El sellado pide una Llave del Santuario. */
    public static final RegistryObject<Block> VAULT_CHEST = BLOCKS.register("vault_chest",
            () -> new com.mundosdeaxelis.block.VaultChestBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.WOOD)
                    .strength(-1.0F, 3600000.0F)
                    .sound(SoundType.WOOD)
                    .noOcclusion()
                    .noLootTable()
                    .lightLevel(state -> 5), false));
    public static final RegistryObject<Block> VAULT_CHEST_SEALED = BLOCKS.register("vault_chest_sealed",
            () -> new com.mundosdeaxelis.block.VaultChestBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BLACK)
                    .strength(-1.0F, 3600000.0F)
                    .sound(SoundType.CHAIN)
                    .noOcclusion()
                    .noLootTable()
                    .lightLevel(state -> 6), true));

    public static final RegistryObject<Block> VAULT_CHEST_LOCKED = BLOCKS.register("vault_chest_locked",
            () -> new com.mundosdeaxelis.block.VaultChestBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.WOOD)
                    .strength(-1.0F, 3600000.0F)
                    .sound(SoundType.WOOD)
                    .noOcclusion()
                    .noLootTable()
                    .lightLevel(state -> 4), false, true));

    /** Versiones de la dungeon de la Forja (mismos cofres, aspecto de hierro y brasas). El sellado pide una Llave de Hierro. */
    public static final RegistryObject<Block> VAULT_CHEST_FORGE = BLOCKS.register("vault_chest_forge",
            () -> new com.mundosdeaxelis.block.VaultChestBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_GRAY)
                    .strength(-1.0F, 3600000.0F)
                    .sound(SoundType.NETHERITE_BLOCK)
                    .noOcclusion()
                    .noLootTable()
                    .lightLevel(state -> 6), false));
    public static final RegistryObject<Block> VAULT_CHEST_SEALED_FORGE = BLOCKS.register("vault_chest_sealed_forge",
            () -> new com.mundosdeaxelis.block.VaultChestBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BLACK)
                    .strength(-1.0F, 3600000.0F)
                    .sound(SoundType.CHAIN)
                    .noOcclusion()
                    .noLootTable()
                    .lightLevel(state -> 7), true));
    public static final RegistryObject<Block> VAULT_CHEST_LOCKED_FORGE = BLOCKS.register("vault_chest_locked_forge",
            () -> new com.mundosdeaxelis.block.VaultChestBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_GRAY)
                    .strength(-1.0F, 3600000.0F)
                    .sound(SoundType.NETHERITE_BLOCK)
                    .noOcclusion()
                    .noLootTable()
                    .lightLevel(state -> 5), false, true));

    /** Versiones del volcán (etapa 255): basalto con grietas de magma y oro ennegrecido. */
    public static final RegistryObject<Block> VAULT_CHEST_VOLCANO = BLOCKS.register("vault_chest_volcano",
            () -> new com.mundosdeaxelis.block.VaultChestBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BLACK)
                    .strength(-1.0F, 3600000.0F)
                    .sound(SoundType.NETHER_BRICKS)
                    .noOcclusion()
                    .noLootTable()
                    .lightLevel(state -> 7), false));
    public static final RegistryObject<Block> VAULT_CHEST_LOCKED_VOLCANO = BLOCKS.register("vault_chest_locked_volcano",
            () -> new com.mundosdeaxelis.block.VaultChestBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BLACK)
                    .strength(-1.0F, 3600000.0F)
                    .sound(SoundType.NETHER_BRICKS)
                    .noOcclusion()
                    .noLootTable()
                    .lightLevel(state -> 6), false, true));

    /** Versiones del hielo (etapa 268): cuarzo escarchado con ribetes de plata y una gema celeste. */
    public static final RegistryObject<Block> VAULT_CHEST_ICE = BLOCKS.register("vault_chest_ice",
            () -> new com.mundosdeaxelis.block.VaultChestBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.ICE)
                    .strength(-1.0F, 3600000.0F)
                    .sound(SoundType.GLASS)
                    .noOcclusion()
                    .noLootTable()
                    .lightLevel(state -> 6), false));
    public static final RegistryObject<Block> VAULT_CHEST_LOCKED_ICE = BLOCKS.register("vault_chest_locked_ice",
            () -> new com.mundosdeaxelis.block.VaultChestBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.ICE)
                    .strength(-1.0F, 3600000.0F)
                    .sound(SoundType.GLASS)
                    .noOcclusion()
                    .noLootTable()
                    .lightLevel(state -> 5), false, true));

    /** Versiones de la magia (etapa 268): madera morada oscura con oro y una gema de amatista. */
    public static final RegistryObject<Block> VAULT_CHEST_MAGIC = BLOCKS.register("vault_chest_magic",
            () -> new com.mundosdeaxelis.block.VaultChestBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE)
                    .strength(-1.0F, 3600000.0F)
                    .sound(SoundType.AMETHYST)
                    .noOcclusion()
                    .noLootTable()
                    .lightLevel(state -> 7), false));
    public static final RegistryObject<Block> VAULT_CHEST_LOCKED_MAGIC = BLOCKS.register("vault_chest_locked_magic",
            () -> new com.mundosdeaxelis.block.VaultChestBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE)
                    .strength(-1.0F, 3600000.0F)
                    .sound(SoundType.AMETHYST)
                    .noOcclusion()
                    .noLootTable()
                    .lightLevel(state -> 6), false, true));

    /** Gran Yunque de Fundición (etapa 270): dos partes (yunque y crisol con lava); forja llaves con fragmentos y un juego de habilidad. */
    public static final RegistryObject<Block> GRAN_YUNQUE = BLOCKS.register("gran_yunque",
            () -> new com.mundosdeaxelis.block.GranYunqueBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(5.0F, 1200.0F)
                    .sound(SoundType.ANVIL)
                    .noOcclusion()
                    .lightLevel(st -> st.getValue(com.mundosdeaxelis.block.GranYunqueBlock.PART) == com.mundosdeaxelis.block.GranYunqueBlock.Part.FORGE ? 13 : 3)));

    /** Etapa 278: el Altar Maldito (oleadas de enemigos con recompensa; no se puede romper en supervivencia). */
    public static final RegistryObject<Block> ALTAR_MALDITO = BLOCKS.register("altar_maldito",
            () -> new com.mundosdeaxelis.block.AltarMalditoBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BLACK)
                    .strength(-1.0F, 3600000.0F)
                    .sound(SoundType.POLISHED_DEEPSLATE)
                    .noOcclusion()
                    .noLootTable()
                    .lightLevel(st -> st.getValue(com.mundosdeaxelis.block.AltarMalditoBlock.CARGADO) ? 10 : 3)));

    /** Etapa 280: el Tablón de Encargos. */
    public static final RegistryObject<Block> TABLON_ENCARGOS = BLOCKS.register("tablon_encargos",
            () -> new com.mundosdeaxelis.encargo.TablonBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.WOOD)
                    .strength(2.0F, 3.0F)
                    .sound(SoundType.WOOD)
                    .noOcclusion()
                    .ignitedByLava()));

    /** Mesa de Gemas (etapa 271): infunde gemas vacías y las engarza en las armas. */
    public static final RegistryObject<Block> MESA_GEMAS = BLOCKS.register("mesa_gemas",
            () -> new com.mundosdeaxelis.gema.MesaGemasBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE)
                    .strength(3.5F, 1200.0F)
                    .sound(SoundType.AMETHYST)
                    .noOcclusion()
                    .lightLevel(st -> 7)));

    /** Mesa del Artífice: mejoras, tienda de llaves y Archivo. */
    public static final RegistryObject<Block> ARTIFICER_TABLE = BLOCKS.register("artificer_table",
            () -> new com.mundosdeaxelis.block.ArtificerTableBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE)
                    .strength(2.5F, 6.0F)
                    .sound(SoundType.AMETHYST)
                    .noOcclusion()
                    .lightLevel(state -> 9)));

    /** Lápida: guarda 15 minutos lo que llevabas al morir. Solo la abre su dueño. */
    public static final RegistryObject<Block> GRAVE = BLOCKS.register("grave",
            () -> new com.mundosdeaxelis.block.GraveBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE)
                    .strength(-1.0F, 3600000.0F)
                    .sound(SoundType.DEEPSLATE)
                    .noOcclusion()
                    .noLootTable()
                    .lightLevel(state -> 5)
                    .pushReaction(PushReaction.BLOCK)));

    /** Decoración (bloques y objetos con forma) que sale en las cajas; todo sale de la tabla {@link com.mundosdeaxelis.block.DecorDefs}. */
    public static final java.util.Map<String, RegistryObject<Block>> DECOR = new java.util.LinkedHashMap<>();

    private static SoundType decorSound(String n) {
        return switch (n) {
            case "WOOL" -> SoundType.WOOL;
            case "WOOD" -> SoundType.WOOD;
            case "GLASS" -> SoundType.GLASS;
            case "METAL" -> SoundType.METAL;
            case "LANTERN" -> SoundType.LANTERN;
            case "AMETHYST" -> SoundType.AMETHYST;
            default -> SoundType.STONE;
        };
    }

    static {
        for (com.mundosdeaxelis.block.DecorDefs.Def d : com.mundosdeaxelis.block.DecorDefs.ALL) {
            DECOR.put(d.id(), BLOCKS.register(d.id(), () -> {
                BlockBehaviour.Properties p = BlockBehaviour.Properties.of().strength(d.strength()).sound(decorSound(d.sound()))
                        .lightLevel(state -> d.light());
                if (d.id().equals("decor_garden_bench")) {
                    return new com.mundosdeaxelis.block.BenchBlock(p.noOcclusion(), d.box());
                }
                if (d.shaped()) {
                    return new com.mundosdeaxelis.block.DecorBlock(p.noOcclusion(), d.box());
                }
                return new Block(p);
            }));
        }
    }

    /** Los tres crates permanentes (no se rompen en supervivencia). */
    public static final java.util.Map<com.mundosdeaxelis.crate.CrateKind, RegistryObject<Block>> CRATES = registerCrates();

    private static java.util.Map<com.mundosdeaxelis.crate.CrateKind, RegistryObject<Block>> registerCrates() {
        java.util.Map<com.mundosdeaxelis.crate.CrateKind, RegistryObject<Block>> m = new java.util.EnumMap<>(com.mundosdeaxelis.crate.CrateKind.class);
        for (com.mundosdeaxelis.crate.CrateKind k : com.mundosdeaxelis.crate.CrateKind.values()) {
            m.put(k, BLOCKS.register(k.blockId(),
                    () -> new com.mundosdeaxelis.block.CrateBlock(k, BlockBehaviour.Properties.of()
                            .mapColor(MapColor.COLOR_PURPLE)
                            .strength(-1.0F, 3600000.0F)
                            .sound(SoundType.AMETHYST)
                            .noOcclusion()
                            .noLootTable()
                            .lightLevel(state -> 10))));
        }
        return m;
    }

    private ModBlocks() {
    }
}
