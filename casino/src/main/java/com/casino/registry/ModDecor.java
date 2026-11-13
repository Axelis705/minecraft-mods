package com.casino.registry;

import com.casino.decor.CounterBlock;
import com.casino.decor.DecorBlock;
import com.casino.decor.DrinksBlock;
import com.casino.decor.SeatBlock;
import com.casino.decor.SeatEntity;
import com.casino.decor.SofaBlock;
import com.casino.plat.Reg;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.registries.RegistryObject;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/** Decoración del casino (ronda 1: asientos y bar). Se registra en los mismos registros del mod. */
public final class ModDecor {
    public static final String[] COLORS = {"red", "black", "purple", "blue"};
    private static final MapColor[] MAP = {MapColor.COLOR_RED, MapColor.COLOR_BLACK, MapColor.COLOR_PURPLE, MapColor.COLOR_BLUE};

    /** Todo lo de decoración, en el orden de la pestaña del modo creativo. */
    public static final List<RegistryObject<Item>> TAB_ITEMS = new ArrayList<>();

    public static final RegistryObject<EntityType<SeatEntity>> SEAT = ModEntities.ENTITIES.register("seat",
            () -> Reg.entity("seat", EntityType.Builder.<SeatEntity>of(SeatEntity::new, MobCategory.MISC).sized(0.001F, 0.001F)
                    .clientTrackingRange(10)));

    public static final List<RegistryObject<Block>> CHAIRS = new ArrayList<>();
    public static final List<RegistryObject<Block>> STOOLS = new ArrayList<>();
    public static final List<RegistryObject<Block>> SOFAS = new ArrayList<>();

    static {
        for (int i = 0; i < COLORS.length; i++) {
            MapColor mc = MAP[i];
            CHAIRS.add(block("casino_chair_" + COLORS[i], p -> new SeatBlock(p.mapColor(mc).strength(1.5F).sound(SoundType.WOOL).noOcclusion(), 0.6,
                    new double[]{2, 0, 2, 14, 9.6, 14}, new double[]{2, 9.6, 12.4, 14, 16, 15})));
        }
        for (int i = 0; i < COLORS.length; i++) {
            MapColor mc = MAP[i];
            STOOLS.add(block("bar_stool_" + COLORS[i], p -> new SeatBlock(p.mapColor(mc).strength(1.5F).sound(SoundType.METAL).noOcclusion(), 0.85,
                    new double[]{3.6, 0, 3.6, 12.4, 13.6, 12.4})));
        }
        for (int i = 0; i < COLORS.length; i++) {
            MapColor mc = MAP[i];
            SOFAS.add(block("sofa_" + COLORS[i], p -> new SofaBlock(p.mapColor(mc).strength(1.5F).sound(SoundType.WOOL).noOcclusion())));
        }
    }

    public static final RegistryObject<Block> COCKTAIL_TABLE = block("cocktail_table", p -> new DecorBlock(
            p.mapColor(MapColor.QUARTZ).strength(2.0F).sound(SoundType.STONE).noOcclusion(), false,
            new double[]{3, 0, 3, 13, 1.6, 13}, new double[]{6.5, 1.6, 6.5, 9.5, 13, 9.5}, new double[]{0.8, 13, 0.8, 15.2, 14.6, 15.2}));
    public static final RegistryObject<Block> COCKTAIL_TABLE_BLACK = block("cocktail_table_black", p -> new DecorBlock(
            p.mapColor(MapColor.COLOR_BLACK).strength(2.0F).sound(SoundType.STONE).noOcclusion(), false,
            new double[]{3, 0, 3, 13, 1.6, 13}, new double[]{6.5, 1.6, 6.5, 9.5, 13, 9.5}, new double[]{0.8, 13, 0.8, 15.2, 14.6, 15.2}));
    public static final RegistryObject<Block> BAR_COUNTER = block("bar_counter", p -> new CounterBlock(
            p.mapColor(MapColor.TERRACOTTA_BROWN).strength(2.5F).sound(SoundType.WOOD).noOcclusion()));
    public static final RegistryObject<Block> BOTTLE_SHELF = block("bottle_shelf", p -> new DecorBlock(
            p.mapColor(MapColor.TERRACOTTA_BROWN).strength(1.5F).sound(SoundType.WOOD).noOcclusion(), true,
            new double[]{0, 0, 8.5, 16, 16, 16}));
    public static final RegistryObject<Block> DRINKS = block("drinks", p -> new DrinksBlock(
            p.mapColor(MapColor.NONE).strength(0.3F).sound(SoundType.GLASS).noOcclusion()));

    // ronda 2: luces y señales
    public static final RegistryObject<Block> NEON_LETTER = block("neon_letter", p -> new com.casino.decor.NeonLetterBlock(
            p.mapColor(MapColor.COLOR_BLACK).strength(1.0F).sound(SoundType.GLASS).noOcclusion().lightLevel(s -> 10)));
    public static final RegistryObject<Block> NEON_SIGN = block("neon_sign", p -> new com.casino.decor.WallDecorBlock.Sign(
            p.mapColor(MapColor.COLOR_BLACK).strength(1.0F).sound(SoundType.GLASS).noOcclusion().lightLevel(s -> 10)));
    public static final RegistryObject<Block> WALL_SCREEN = block("wall_screen", p -> new com.casino.decor.WallDecorBlock.Screen(
            p.mapColor(MapColor.COLOR_BLACK).strength(1.5F).sound(SoundType.METAL).noOcclusion().lightLevel(s -> 8)));
    public static final RegistryObject<Block> CHANDELIER = block("chandelier", p -> new DecorBlock(
            p.mapColor(MapColor.GOLD).strength(1.0F).sound(SoundType.GLASS).noOcclusion().lightLevel(s -> 15), false,
            new double[]{1, 3.6, 1, 15, 16, 15}));
    public static final RegistryObject<Block> GRAND_CHANDELIER = block("grand_chandelier", p -> new com.casino.decor.GrandChandelierBlock(
            p.mapColor(MapColor.GOLD).strength(1.0F).sound(SoundType.GLASS).noOcclusion().lightLevel(s -> 15)));
    public static final List<RegistryObject<Block>> ROPE_POSTS = new ArrayList<>();

    static {
        for (int i = 0; i < COLORS.length; i++) {
            ROPE_POSTS.add(block("rope_post_" + COLORS[i], p -> new com.casino.decor.RopePostBlock(
                    p.mapColor(MapColor.GOLD).strength(1.5F).sound(SoundType.METAL).noOcclusion())));
        }
    }

    // ronda 3: pisos y paredes
    public static final String[] PATTERNS = {"royal", "galaxy", "deco", "lounge"};

    static {
        for (String pat : PATTERNS) {
            block("casino_floor_" + pat, p -> new Block(p.mapColor(MapColor.COLOR_RED).strength(0.8F).sound(SoundType.WOOL)));
            block("casino_carpet_" + pat, p -> new net.minecraft.world.level.block.CarpetBlock(p.mapColor(MapColor.COLOR_RED).strength(0.1F)
                    .sound(SoundType.WOOL)));
        }
    }

    public static final RegistryObject<Block> MARBLE = block("marble", p -> new Block(p.mapColor(MapColor.QUARTZ).strength(1.5F, 6.0F)
            .sound(SoundType.STONE).requiresCorrectToolForDrops()));
    public static final RegistryObject<Block> MARBLE_BLACK = block("marble_black", p -> new Block(p.mapColor(MapColor.COLOR_BLACK)
            .strength(1.5F, 6.0F).sound(SoundType.STONE).requiresCorrectToolForDrops()));
    public static final RegistryObject<Block> MARBLE_STAIRS = block("marble_stairs", p -> new net.minecraft.world.level.block.StairBlock(
            MARBLE.get().defaultBlockState(), p.mapColor(MapColor.QUARTZ).strength(1.5F, 6.0F).sound(SoundType.STONE).requiresCorrectToolForDrops()));
    public static final RegistryObject<Block> MARBLE_BLACK_STAIRS = block("marble_black_stairs", p -> new net.minecraft.world.level.block.StairBlock(
            MARBLE_BLACK.get().defaultBlockState(), p.mapColor(MapColor.COLOR_BLACK).strength(1.5F, 6.0F).sound(SoundType.STONE)
            .requiresCorrectToolForDrops()));
    public static final RegistryObject<Block> MARBLE_SLAB = block("marble_slab", p -> new net.minecraft.world.level.block.SlabBlock(
            p.mapColor(MapColor.QUARTZ).strength(1.5F, 6.0F).sound(SoundType.STONE).requiresCorrectToolForDrops()));
    public static final RegistryObject<Block> MARBLE_BLACK_SLAB = block("marble_black_slab", p -> new net.minecraft.world.level.block.SlabBlock(
            p.mapColor(MapColor.COLOR_BLACK).strength(1.5F, 6.0F).sound(SoundType.STONE).requiresCorrectToolForDrops()));
    public static final RegistryObject<Block> MAHOGANY_PANEL = block("mahogany_panel", p -> new Block(p.mapColor(MapColor.TERRACOTTA_BROWN)
            .strength(2.0F, 3.0F).sound(SoundType.WOOD)));
    public static final RegistryObject<Block> GOLD_TRIM = block("gold_trim", p -> new Block(p.mapColor(MapColor.GOLD).strength(3.0F, 6.0F)
            .sound(SoundType.METAL)));

    static {
        for (int i = 0; i < COLORS.length; i++) {
            MapColor mc = MAP[i];
            block("velvet_wall_" + COLORS[i], p -> new Block(p.mapColor(mc).strength(0.8F).sound(SoundType.WOOL)));
        }
    }

    // bocina con música
    public static final RegistryObject<Block> SPEAKER = block("speaker", p -> new com.casino.decor.SpeakerBlock(
            p.mapColor(MapColor.TERRACOTTA_BROWN).strength(1.5F).sound(SoundType.WOOD).noOcclusion()));
    public static final RegistryObject<net.minecraft.world.level.block.entity.BlockEntityType<com.casino.decor.SpeakerBlock.Be>> SPEAKER_BE =
            ModBlockEntities.TYPES.register("speaker", () -> Reg.blockEntity(com.casino.decor.SpeakerBlock.Be::new, SPEAKER.get()));

    public static final RegistryObject<CreativeModeTab> TAB = ModCreativeTabs.TABS.register("decor", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.casino.decor"))
            .icon(() -> new ItemStack(CHAIRS.get(0).get()))
            .displayItems((params, out) -> {
                for (RegistryObject<Item> it : TAB_ITEMS) {
                    out.accept(it.get());
                }
            })
            .build());

    private ModDecor() {
    }

    /** Llamar en el constructor del mod, antes de registrar: así esta clase se carga y agrega sus cosas a tiempo. */
    public static void init() {
    }

    private static RegistryObject<Block> block(String name, Function<BlockBehaviour.Properties, Block> make) {
        RegistryObject<Block> b = ModBlocks.BLOCKS.register(name, () -> make.apply(Reg.block(name)));
        TAB_ITEMS.add(ModItems.ITEMS.register(name, () -> new BlockItem(b.get(), Reg.blockItem(name))));
        return b;
    }
}
