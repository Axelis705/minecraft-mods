package com.navidad.mundo;

import com.mojang.serialization.Codec;
import com.navidad.Navidad;
import com.navidad.bloque.FarolNochebuena;
import com.navidad.registry.ModRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.ChainBlock;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * La guarida del Krampus (rara, en los biomas nevados; o con {@code /navidad guarida}): un claro redondo (radio 11) en el bosque, con
 * árboles secos alrededor de los que cuelgan cadenas, cuatro faroles de Nochebuena en postes y, en el centro, el altar del Krampus sobre
 * piedra negra. Al norte, una cabaña oscura de roble oscuro con techo de escalones, ventanas con rejas, una fogata de almas, carbón,
 * cadenas colgando, telarañas y un cofre (siempre trae una campanita de Santa). Todo cabe en ±16 bloques del centro (la cabaña va de z -15 a -9).
 */
public class Guarida extends Feature<NoneFeatureConfiguration> {
    public static final ResourceLocation BOTIN = new ResourceLocation(Navidad.MODID, "chests/guarida_krampus");
    static final int R = 11;
    /** Para el registro y las pruebas. */
    public static int construidas;

    public Guarida(Codec<NoneFeatureConfiguration> c) {
        super(c);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        return construir(ctx.level(), ctx.origin(), ctx.random(), false);
    }

    /**
     * Construye la guarida con el centro (el altar) en {@code o} (a la altura del piso). Si {@code siempre} es false (al generar el
     * mundo), no la pone en agua, ni en pendientes fuertes.
     */
    public static boolean construir(WorldGenLevel l, BlockPos o, RandomSource r, boolean siempre) {
        int y0 = o.getY();
        if (!siempre) {
            // el piso de verdad (sin troncos, hojas ni nieve suelta) en una rejilla: nada de agua ni pendientes fuertes
            int min = 999, max = -999;
            for (int x = -R; x <= R; x += 4) {
                for (int z = -R - 4; z <= R; z += 4) {
                    int y = piso(l, o.offset(x, 0, z));
                    if (y == Integer.MIN_VALUE) {
                        Navidad.LOGGER.debug("NAVIDAD_GUARIDA no cabe en {}: agua o sin piso", o.toShortString());
                        return false;
                    }
                    min = Math.min(min, y);
                    max = Math.max(max, y);
                }
            }
            if (max - min > 5) {
                Navidad.LOGGER.debug("NAVIDAD_GUARIDA no cabe en {}: pendiente {}", o.toShortString(), max - min);
                return false;
            }
            y0 = piso(l, o);
        }
        BlockPos c = new BlockPos(o.getX(), y0, o.getZ());
        claro(l, c, r);
        arboles(l, c, r);
        faroles(l, c);
        altar(l, c);
        cabana(l, c.offset(0, 0, -R + 2), r);
        construidas++;
        Navidad.LOGGER.info("NAVIDAD_GUARIDA en {}", c.toShortString());
        return true;
    }

    /** La altura del piso firme en esa columna (buscando 12 bloques arriba y abajo); MIN_VALUE si hay agua o no hay piso. */
    private static int piso(WorldGenLevel l, BlockPos p) {
        for (int y = 12; y >= -12; y--) {
            BlockPos q = p.above(y);
            BlockState b = l.getBlockState(q);
            if (!b.getFluidState().isEmpty()) {
                return Integer.MIN_VALUE;
            }
            if (b.is(net.minecraft.tags.BlockTags.LOGS) || b.is(net.minecraft.tags.BlockTags.LEAVES) || !b.blocksMotion()) {
                continue;
            }
            return q.getY() + 1;
        }
        return Integer.MIN_VALUE;
    }

    private static void set(WorldGenLevel l, BlockPos p, BlockState s) {
        l.setBlock(p, s, 2);
    }

    /** Aplana el claro (radio R): quita lo de arriba, rellena lo de abajo y lo cubre de nieve y tierra gruesa. */
    private static void claro(WorldGenLevel l, BlockPos c, RandomSource r) {
        for (int x = -R - 2; x <= R + 2; x++) {
            for (int z = -16; z <= R + 2; z++) {
                double d = Math.sqrt(x * x + z * z);
                boolean cabana = z <= -R + 3 && Math.abs(x) <= 5;
                if (d > R + 1.5 && !cabana) {
                    continue;
                }
                BlockPos col = c.offset(x, 0, z);
                for (int y = 0; y <= 20; y++) {
                    if (!l.getBlockState(col.above(y)).isAir()) {
                        set(l, col.above(y), Blocks.AIR.defaultBlockState());
                    }
                }
                for (int y = -1; y >= -5; y--) {
                    BlockState b = l.getBlockState(col.above(y));
                    if (b.isSolid() && y < -1) {
                        break;
                    }
                    set(l, col.above(y), y == -1 ? suelo(r) : Blocks.DIRT.defaultBlockState());
                }
                if (r.nextInt(3) != 0) {
                    set(l, col, Blocks.SNOW.defaultBlockState().setValue(SnowLayerBlock.LAYERS, 1 + r.nextInt(2)));
                }
            }
        }
    }

    private static BlockState suelo(RandomSource r) {
        int k = r.nextInt(10);
        return k < 5 ? Blocks.SNOW_BLOCK.defaultBlockState() : k < 8 ? Blocks.COARSE_DIRT.defaultBlockState() : Blocks.PODZOL.defaultBlockState();
    }

    /** Árboles secos alrededor del claro, con cadenas colgando de sus ramas. */
    private static void arboles(WorldGenLevel l, BlockPos c, RandomSource r) {
        BlockState tronco = Blocks.STRIPPED_DARK_OAK_LOG.defaultBlockState();
        BlockState rama = Blocks.DARK_OAK_FENCE.defaultBlockState();
        int[] angulos = {325, 0, 35, 70, 110, 145, 180, 215};      // dejan libre el lado de la cabaña (norte)
        for (int ang : angulos) {
            double a = Math.toRadians(ang + r.nextInt(12) - 6);
            int x = (int) Math.round(Math.cos(a) * (R + 0.5)), z = (int) Math.round(Math.sin(a) * (R + 0.5));
            BlockPos b = c.offset(x, 0, z);
            int alto = 4 + r.nextInt(3);
            for (int y = 0; y < alto; y++) {
                set(l, b.above(y), tronco);
            }
            // una rama hacia el claro con su cadena
            Direction hacia = Math.abs(x) > Math.abs(z) ? (x > 0 ? Direction.WEST : Direction.EAST) : (z > 0 ? Direction.NORTH : Direction.SOUTH);
            BlockPos rm = b.above(alto - 1).relative(hacia);
            set(l, rm, rama);
            set(l, rm.relative(hacia), rama);
            int largo = 1 + r.nextInt(3);
            for (int k = 1; k <= largo; k++) {
                set(l, rm.relative(hacia).below(k), Blocks.CHAIN.defaultBlockState().setValue(ChainBlock.AXIS, Direction.Axis.Y));
            }
            set(l, rm.relative(hacia).below(largo + 1), Blocks.SOUL_LANTERN.defaultBlockState()
                    .setValue(net.minecraft.world.level.block.LanternBlock.HANGING, true));
            // una rama seca del otro lado
            set(l, b.above(alto - 2).relative(hacia.getOpposite()), rama);
            if (r.nextBoolean()) {
                set(l, b.relative(hacia.getClockWise()), Blocks.DEAD_BUSH.defaultBlockState());
            }
        }
    }

    /** Los cuatro faroles de Nochebuena (los de la pelea en el apagón), cada uno en su poste. */
    private static void faroles(WorldGenLevel l, BlockPos c) {
        for (int i = 0; i < 4; i++) {
            double a = Math.toRadians(45 + 90 * i);
            BlockPos b = c.offset((int) Math.round(Math.cos(a) * 7), 0, (int) Math.round(Math.sin(a) * 7));
            set(l, b, Blocks.DARK_OAK_FENCE.defaultBlockState());
            set(l, b.above(), ModRegistry.FAROL_NOCHEBUENA.get().defaultBlockState().setValue(FarolNochebuena.LIT, true));
        }
    }

    /** El altar en el centro, sobre piedra negra pulida con velas. */
    private static void altar(WorldGenLevel l, BlockPos c) {
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                boolean esq = Math.abs(x) == 2 && Math.abs(z) == 2;
                set(l, c.offset(x, -1, z), esq ? Blocks.CHISELED_POLISHED_BLACKSTONE.defaultBlockState()
                        : Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState());
                set(l, c.offset(x, 0, z), Blocks.AIR.defaultBlockState());
            }
        }
        set(l, c, ModRegistry.ALTAR_KRAMPUS.get().defaultBlockState());
        for (int[] v : new int[][] {{-2, -2}, {2, -2}, {-2, 2}, {2, 2}}) {
            set(l, c.offset(v[0], 0, v[1]), Blocks.RED_CANDLE.defaultBlockState().setValue(net.minecraft.world.level.block.CandleBlock.CANDLES, 3)
                    .setValue(net.minecraft.world.level.block.CandleBlock.LIT, true));
        }
    }

    /** La cabaña: 9 x 7, con la puerta hacia el claro (al sur). {@code o}: el centro de su pared de enfrente. */
    private static void cabana(WorldGenLevel l, BlockPos o, RandomSource r) {
        BlockState pared = Blocks.DARK_OAK_PLANKS.defaultBlockState();
        BlockState esquina = Blocks.SPRUCE_LOG.defaultBlockState();
        BlockState piso = Blocks.DEEPSLATE_TILES.defaultBlockState();
        int x0 = -4, x1 = 4, z0 = -6, z1 = 0;
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                set(l, o.offset(x, -1, z), piso);
                for (int y = -2; y >= -4; y--) {
                    if (l.getBlockState(o.offset(x, y, z)).isSolid()) {
                        break;
                    }
                    set(l, o.offset(x, y, z), Blocks.COBBLED_DEEPSLATE.defaultBlockState());
                }
                for (int y = 0; y <= 6; y++) {
                    set(l, o.offset(x, y, z), Blocks.AIR.defaultBlockState());
                }
                boolean borde = x == x0 || x == x1 || z == z0 || z == z1;
                boolean esq = (x == x0 || x == x1) && (z == z0 || z == z1);
                if (borde) {
                    for (int y = 0; y <= 3; y++) {
                        set(l, o.offset(x, y, z), esq ? esquina : pared);
                    }
                }
            }
        }
        // techo de dos aguas (escalones de roble oscuro)
        for (int k = 0; k <= 4; k++) {
            for (int z = z0 - 1; z <= z1 + 1; z++) {
                BlockState izq = Blocks.DARK_OAK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.EAST);
                BlockState der = Blocks.DARK_OAK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.WEST);
                if (k < 4) {
                    set(l, o.offset(x0 - 1 + k, 4 + k, z), izq);
                    set(l, o.offset(x1 + 1 - k, 4 + k, z), der);
                } else {
                    set(l, o.offset(0, 4 + k, z), Blocks.DARK_OAK_SLAB.defaultBlockState());
                }
            }
            if (k < 4) {
                for (int x = x0 + k; x <= x1 - k; x++) {
                    set(l, o.offset(x, 4 + k, z0), pared);
                    set(l, o.offset(x, 4 + k, z1), pared);
                }
            }
        }
        // puerta hacia el claro, ventanas con rejas
        BlockState puerta = Blocks.DARK_OAK_DOOR.defaultBlockState().setValue(DoorBlock.FACING, Direction.SOUTH).setValue(DoorBlock.HINGE,
                DoorHingeSide.LEFT);
        set(l, o.offset(0, 0, z1), puerta.setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER));
        set(l, o.offset(0, 1, z1), puerta.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
        for (int[] v : new int[][] {{-2, z1}, {2, z1}, {x0, -3}, {x1, -3}}) {
            set(l, o.offset(v[0], 1, v[1]), Blocks.IRON_BARS.defaultBlockState());
        }
        // adentro: fogata de almas, carbón, barriles, cadenas, telarañas y el cofre
        set(l, o.offset(0, 0, -4), Blocks.SOUL_CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, true).setValue(CampfireBlock.SIGNAL_FIRE,
                false));
        set(l, o.offset(-3, 0, -5), Blocks.COAL_BLOCK.defaultBlockState());
        set(l, o.offset(-2, 0, -5), Blocks.COAL_BLOCK.defaultBlockState());
        set(l, o.offset(-3, 1, -5), Blocks.COAL_BLOCK.defaultBlockState());
        set(l, o.offset(3, 0, -5), Blocks.BARREL.defaultBlockState());
        set(l, o.offset(3, 1, -5), Blocks.BIRCH_FENCE.defaultBlockState());     // un manojo de varas
        set(l, o.offset(3, 0, -1), Blocks.BIRCH_FENCE.defaultBlockState());
        set(l, o.offset(-3, 0, -1), Blocks.CAULDRON.defaultBlockState());
        for (int[] v : new int[][] {{-2, -2}, {2, -3}, {0, -2}}) {
            for (int y = 3; y >= 2; y--) {
                set(l, o.offset(v[0], y, v[1]), Blocks.CHAIN.defaultBlockState().setValue(ChainBlock.AXIS, Direction.Axis.Y));
            }
        }
        set(l, o.offset(-3, 3, -5), Blocks.COBWEB.defaultBlockState());
        set(l, o.offset(3, 3, -1), Blocks.COBWEB.defaultBlockState());
        set(l, o.offset(2, 2, -5), Blocks.SKELETON_SKULL.defaultBlockState());
        BlockPos cofre = o.offset(-1, 0, -5);
        set(l, cofre, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.SOUTH));
        RandomizableContainerBlockEntity.setLootTable(l, r, cofre, BOTIN);
        set(l, o.offset(1, 0, -5), ModRegistry.FAROL_NOCHEBUENA.get().defaultBlockState().setValue(FarolNochebuena.LIT, false));
    }
}
