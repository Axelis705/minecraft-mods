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
