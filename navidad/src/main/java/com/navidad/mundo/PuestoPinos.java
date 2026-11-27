package com.navidad.mundo;

import com.mojang.serialization.Codec;
import com.navidad.Navidad;
import com.navidad.bloque.LetreroPinos;
import com.navidad.bloque.LetreroPinosBE;
import com.navidad.bloque.deco.Deco;
import com.navidad.bloque.deco.DecoBlock;
import com.navidad.registry.ModRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * El puesto de árboles de Navidad que sale solo en el mundo (praderas, bosques, taigas y nieve): un terrenito con cerca de abeto y
 * faroles, piso de hojas y tierra, dos filas de pinos recién cortados y arbolitos adornados a la venta, y al fondo el puestito con techo
 * de lona roja y blanca, luces colgando, mostrador, barriles y una guirnalda. En la entrada, el letrero con el pinito pintado, que trae a
 * Don Abeto, el vendedor (detrás del mostrador).
 */
public class PuestoPinos extends Feature<NoneFeatureConfiguration> {
    public static final ResourceLocation BOTIN = new ResourceLocation(Navidad.MODID, "chests/puesto_pinos");

    public PuestoPinos(Codec<NoneFeatureConfiguration> c) {
        super(c);
    }

    private static void deco(WorldGenLevel l, BlockPos p, String id, Direction f) {
        Block b = Deco.block(id);
        if (b == null) {
            return;
        }
        BlockState s = b.defaultBlockState();
        if (s.hasProperty(DecoBlock.FACING)) {
            s = s.setValue(DecoBlock.FACING, f);
        }
        if (s.hasProperty(DecoBlock.HALF)) {
            l.setBlock(p, s.setValue(DecoBlock.HALF, DoubleBlockHalf.LOWER), 2);
            l.setBlock(p.above(), s.setValue(DecoBlock.HALF, DoubleBlockHalf.UPPER), 2);
            return;
        }
        l.setBlock(p, s, 2);
    }

    /** (x, y, z) del puesto mirando al sur (+z, la entrada), girado para que mire hacia {@code f}. */
    static BlockPos rot(BlockPos o, int x, int y, int z, Direction f) {
        return CasitaDuende.rot(o, x, y, z, f);
    }

    /** Hacia dónde queda {@code d} (dado mirando al sur) cuando el puesto mira hacia {@code f}. */
    static Direction girar(Direction d, Direction f) {
        return switch (f) {
            case NORTH -> d.getOpposite();
            case EAST -> d.getCounterClockWise();
            case WEST -> d.getClockWise();
            default -> d;
        };
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        WorldGenLevel l = ctx.level();
        RandomSource r = ctx.random();
        BlockPos o0 = ctx.origin();
        // terreno casi plano (a lo más 2 de diferencia) y sin agua
        int min = Integer.MAX_VALUE, max = Integer.MIN_VALUE;
        for (int x = -7; x <= 7; x += 2) {
            for (int z = -6; z <= 7; z += 2) {
                int h = l.getHeight(Heightmap.Types.WORLD_SURFACE_WG, o0.getX() + x, o0.getZ() + z);
                min = Math.min(min, h);
                max = Math.max(max, h);
                BlockPos s = new BlockPos(o0.getX() + x, h - 1, o0.getZ() + z);
                if (!l.getFluidState(s).isEmpty() || !l.getFluidState(s.above()).isEmpty()) {
                    return false;
                }
            }
        }
        if (max - min > 2) {
            return false;
        }
        BlockPos o = new BlockPos(o0.getX(), min, o0.getZ());
        Direction f = Direction.Plane.HORIZONTAL.getRandomDirection(r);
        // limpiar y emparejar: piso de tierra gruesa con hojarasca (podzol) y relleno abajo
        for (int x = -7; x <= 7; x++) {
            for (int z = -6; z <= 7; z++) {
                for (int y = 0; y <= 7; y++) {
                    l.setBlock(rot(o, x, y, z, f), Blocks.AIR.defaultBlockState(), 2);
                }
                BlockState piso = (x * 7 + z * 13 + r.nextInt(3)) % 4 == 0 ? Blocks.PODZOL.defaultBlockState()
                        : Math.abs(x) <= 1 && z > -3 ? Blocks.DIRT_PATH.defaultBlockState() : Blocks.COARSE_DIRT.defaultBlockState();
                l.setBlock(rot(o, x, -1, z, f), piso, 2);
                for (int y = -2; y >= -5; y--) {
                    BlockPos p = rot(o, x, y, z, f);
                    if (l.getBlockState(p).isSolid()) {
                        break;
                    }
                    l.setBlock(p, Blocks.DIRT.defaultBlockState(), 2);
                }
            }
        }
        // la cerca con faroles en las esquinas y la entrada al frente
        BlockState cerca = Blocks.SPRUCE_FENCE.defaultBlockState();
        for (int x = -6; x <= 6; x++) {
            for (int z = -5; z <= 6; z++) {
                boolean borde = Math.abs(x) == 6 || z == -5 || z == 6;
                if (!borde || z == 6 && Math.abs(x) <= 1) {
                    continue;
                }
                l.setBlock(rot(o, x, 0, z, f), cerca, 3);
            }
        }
        for (int[] e : new int[][]{{-6, -5}, {6, -5}, {-6, 6}, {6, 6}, {-2, 6}, {2, 6}}) {
            l.setBlock(rot(o, e[0], 1, e[1], f), Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, false), 2);
        }
        // el puestito al fondo: postes, techo de lona a rayas, pared con guirnalda, mostrador con barriles
        for (int[] e : new int[][]{{-3, -4}, {3, -4}, {-3, -2}, {3, -2}}) {
            for (int y = 0; y <= 3; y++) {
                l.setBlock(rot(o, e[0], y, e[1], f), Blocks.STRIPPED_SPRUCE_LOG.defaultBlockState(), 2);
            }
        }
        for (int x = -4; x <= 4; x++) {
            for (int z = -5; z <= -1; z++) {
                l.setBlock(rot(o, x, 4, z, f), ((x + 4) % 2 == 0 ? Blocks.RED_WOOL : Blocks.WHITE_WOOL).defaultBlockState(), 2);
            }
            l.setBlock(rot(o, x, 4, 0, f), ((x + 4) % 2 == 0 ? Blocks.RED_CARPET : Blocks.WHITE_CARPET).defaultBlockState(), 2);
        }
        for (int x = -2; x <= 2; x++) {
            for (int y = 0; y <= 3; y++) {
                l.setBlock(rot(o, x, y, -4, f), Blocks.SPRUCE_PLANKS.defaultBlockState(), 2);
            }
            // luces colgando del techo, al frente
            deco(l, rot(o, x, 3, -1, f), "luces_colgantes", girar(Direction.SOUTH, f));
        }
        deco(l, rot(o, -3, 3, -1, f), "luces_colgantes", girar(Direction.SOUTH, f));
        deco(l, rot(o, 3, 3, -1, f), "luces_colgantes", girar(Direction.SOUTH, f));
        deco(l, rot(o, 0, 2, -3, f), "guirnalda_luces", girar(Direction.SOUTH, f));
        deco(l, rot(o, -2, 2, -3, f), "corona_navidad", girar(Direction.SOUTH, f));
        deco(l, rot(o, 2, 2, -3, f), "corona_navidad", girar(Direction.SOUTH, f));
        // mostrador (losas de arriba) con chocolate y una caja de adornos encima, y barriles a los lados
        for (int x = -1; x <= 1; x++) {
            l.setBlock(rot(o, x, 0, -2, f), Blocks.SPRUCE_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.DOUBLE), 2);
        }
        deco(l, rot(o, -1, 1, -2, f), "taza_chocolate", girar(Direction.SOUTH, f));
        deco(l, rot(o, 1, 1, -2, f), "regalo_rojo", girar(Direction.SOUTH, f));
        BlockPos barril = rot(o, -2, 0, -2, f);
        l.setBlock(barril, Blocks.BARREL.defaultBlockState().setValue(BarrelBlock.FACING, Direction.UP), 2);
        RandomizableContainerBlockEntity.setLootTable(l, r, barril, BOTIN);
        l.setBlock(rot(o, 2, 0, -2, f), Blocks.BARREL.defaultBlockState().setValue(BarrelBlock.FACING, Direction.UP), 2);
        // los árboles a la venta: pinos recién cortados en las orillas y arbolitos ya adornados para que se antojen
        String[][] filas = {{"pino_atado", "pino_atado", "arbol_navidad"}, {"pino_atado", "arbol_nevado", "pino_atado"}};
        int[] zs = {0, 3};
        for (int k = 0; k < 2; k++) {
            int z = zs[k];
            deco(l, rot(o, -5, 0, z, f), filas[k][0], girar(Direction.EAST, f));
            deco(l, rot(o, -3, 0, z, f), filas[k][1], girar(Direction.EAST, f));
            deco(l, rot(o, 3, 0, z, f), filas[k][2], girar(Direction.WEST, f));
            deco(l, rot(o, 5, 0, z, f), filas[k == 0 ? 1 : 0][0], girar(Direction.WEST, f));
        }
        deco(l, rot(o, -4, 0, 5, f), "pino_maceta", girar(Direction.SOUTH, f));
        deco(l, rot(o, 4, 0, 5, f), "pino_maceta", girar(Direction.SOUTH, f));
        deco(l, rot(o, -5, 0, -3, f), "pino_maceta", girar(Direction.SOUTH, f));
        l.setBlock(rot(o, 5, 0, -3, f), Blocks.HAY_BLOCK.defaultBlockState(), 2);
        l.setBlock(rot(o, 5, 0, -4, f), Blocks.HAY_BLOCK.defaultBlockState(), 2);
        l.setBlock(rot(o, 4, 0, -4, f), Blocks.SPRUCE_SAPLING.defaultBlockState(), 2);
        l.setBlock(rot(o, 4, -1, -4, f), Blocks.PODZOL.defaultBlockState(), 2);
        // el letrero, junto a la entrada, mirando hacia afuera; trae a Don Abeto (detrás del mostrador)
        BlockPos letrero = rot(o, 2, 0, 7, f);
        l.setBlock(letrero, ModRegistry.LETRERO_PINOS.get().defaultBlockState().setValue(LetreroPinos.FACING, girar(Direction.SOUTH, f)), 2);
        if (l.getBlockEntity(letrero) instanceof LetreroPinosBE be) {
            be.vendedor = rot(o, 0, 0, -3, f).subtract(letrero);
            be.setChanged();
        }
        Navidad.LOGGER.debug("NAVIDAD_PUESTO en {}", o.toShortString());
        return true;
    }
}
