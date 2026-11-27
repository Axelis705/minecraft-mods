package com.navidad.mundo;

import com.mojang.serialization.Codec;
import com.navidad.Navidad;
import com.navidad.bloque.deco.Deco;
import com.navidad.bloque.deco.DecoBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * La casita de duende que sale sola (muy rara) en los biomas nevados: paredes de ladrillo de jengibre con esquinas de caramelo, techo de
 * glaseado en escalones, chimenea que echa humo, puerta y ventanas. Adentro: chimenea navideña, alfombra, pinito, mesa de trabajo y un
 * cofre con cosas de duende (galletas, dulces, adornos, regalos y, con suerte, patines, un trineo o un disco). Afuera, bastones gigantes,
 * un farolito, un muñeco de nieve y un regalo.
 */
public class CasitaDuende extends Feature<NoneFeatureConfiguration> {
    public static final ResourceLocation BOTIN = new ResourceLocation(Navidad.MODID, "chests/casita_duende");

    public CasitaDuende(Codec<NoneFeatureConfiguration> c) {
        super(c);
    }

    private static BlockState nav(String id, BlockState otro) {
        Block b = Deco.block(id);
        return b == null ? otro : b.defaultBlockState();
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

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        WorldGenLevel l = ctx.level();
        RandomSource r = ctx.random();
        BlockPos o = ctx.origin();
        // el suelo: firme bajo casi toda la casa y sin mucha pendiente
        int firmes = 0;
        for (int x = -3; x <= 3; x++) {
            for (int z = -3; z <= 3; z++) {
                BlockState b = l.getBlockState(o.offset(x, -1, z));
                if (b.isSolid() && !b.is(Blocks.ICE) && b.getFluidState().isEmpty()) {
                    firmes++;
                }
                if (!l.getBlockState(o.offset(x, 2, z)).isAir() && !l.getBlockState(o.offset(x, 2, z)).canBeReplaced()) {
                    return false;
                }
            }
        }
        if (firmes < 40) {
            return false;
        }
        Direction frente = Direction.Plane.HORIZONTAL.getRandomDirection(r);
        BlockState pared = nav("ladrillo_jengibre", Blocks.BRICKS.defaultBlockState());
        BlockState esquina = nav("bloque_caramelo", Blocks.RED_WOOL.defaultBlockState());
        BlockState techo = nav("glaseado", Blocks.SNOW_BLOCK.defaultBlockState());
        BlockState punta = nav("glaseado_chispas", Blocks.SNOW_BLOCK.defaultBlockState());
        // limpiar y cimientos
        for (int x = -4; x <= 4; x++) {
            for (int z = -4; z <= 5; z++) {
                for (int y = 0; y <= 7; y++) {
                    l.setBlock(rot(o, x, y, z, frente), Blocks.AIR.defaultBlockState(), 2);
                }
            }
        }
        for (int x = -3; x <= 3; x++) {
            for (int z = -3; z <= 3; z++) {
                l.setBlock(rot(o, x, -1, z, frente), Blocks.SPRUCE_PLANKS.defaultBlockState(), 2);
                for (int y = -2; y >= -5; y--) {
                    BlockPos p = rot(o, x, y, z, frente);
                    if (l.getBlockState(p).isSolid()) {
                        break;
                    }
                    l.setBlock(p, Blocks.STONE_BRICKS.defaultBlockState(), 2);
                }
            }
        }
        // paredes, esquinas y techo en escalones
        for (int y = 0; y <= 3; y++) {
            for (int x = -3; x <= 3; x++) {
                for (int z = -3; z <= 3; z++) {
                    boolean borde = Math.abs(x) == 3 || Math.abs(z) == 3;
                    if (!borde) {
                        continue;
                    }
                    boolean esq = Math.abs(x) == 3 && Math.abs(z) == 3;
                    l.setBlock(rot(o, x, y, z, frente), esq ? esquina : pared, 2);
                }
            }
        }
        for (int k = 0; k <= 2; k++) {
            int rr = 3 - k;
            for (int x = -rr; x <= rr; x++) {
                for (int z = -rr; z <= rr; z++) {
                    l.setBlock(rot(o, x, 4 + k, z, frente), k == 2 ? punta : techo, 2);
                }
            }
        }
        // la chimenea con humo, atrás a la izquierda
        for (int y = 4; y <= 7; y++) {
            l.setBlock(rot(o, -2, y, -2, frente), Blocks.BRICKS.defaultBlockState(), 2);
        }
        l.setBlock(rot(o, -2, 8, -2, frente), Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, true)
                .setValue(CampfireBlock.SIGNAL_FIRE, false), 2);
        // puerta (al frente, +z antes de girar) y ventanas
        BlockState puerta = Blocks.SPRUCE_DOOR.defaultBlockState().setValue(DoorBlock.FACING, frente).setValue(DoorBlock.HINGE, DoorHingeSide.LEFT);
        l.setBlock(rot(o, 0, 0, 3, frente), puerta.setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER), 2);
        l.setBlock(rot(o, 0, 1, 3, frente), puerta.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER), 2);
        l.setBlock(rot(o, 0, 2, 3, frente), nav("bloque_caramelo_verde", Blocks.GREEN_WOOL.defaultBlockState()), 2);
        for (int[] v : new int[][]{{-2, 3}, {2, 3}, {-3, 0}, {3, 0}, {0, -3}}) {
            l.setBlock(rot(o, v[0], 1, v[1], frente), Blocks.GLASS.defaultBlockState(), 2);
        }
        // adentro
        Direction atras = frente.getOpposite();
        deco(l, rot(o, 0, 0, -2, frente), "chimenea", frente);
        deco(l, rot(o, 2, 0, 2, frente), "pino_maceta", atras);
        l.setBlock(rot(o, 2, 0, -2, frente), Blocks.CRAFTING_TABLE.defaultBlockState(), 2);
        deco(l, rot(o, 1, 0, -2, frente), "pila_regalos", frente);
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                deco(l, rot(o, x, 0, z, frente), "alfombra_navidad", frente);
            }
        }
        l.setBlock(rot(o, 0, 3, 0, frente), Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true), 2);
        deco(l, rot(o, -1, 2, -2, frente), "calcetin_rojo", frente);
        deco(l, rot(o, 1, 2, -2, frente), "calcetin_verde", frente);
        BlockPos cofre = rot(o, -2, 0, -2, frente);
        l.setBlock(cofre, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, frente), 2);
        RandomizableContainerBlockEntity.setLootTable(l, r, cofre, BOTIN);
        // afuera
        deco(l, rot(o, -1, 0, 4, frente), "baston_gigante", frente);
        deco(l, rot(o, 1, 0, 4, frente), "baston_gigante", frente);
        deco(l, rot(o, 3, 0, 4, frente), "farolito", frente);
        deco(l, rot(o, -3, 0, 5, frente), "muneco_nieve", frente);
        deco(l, rot(o, 2, 0, 5, frente), "regalo_rojo", frente);
        deco(l, rot(o, 0, 3, 4, frente), "corona_navidad", frente);
        return true;
    }

    /** (x, y, z) de la casa mirando al sur (+z), girada para que mire hacia {@code f}. */
    static BlockPos rot(BlockPos o, int x, int y, int z, Direction f) {
        return switch (f) {
            case NORTH -> o.offset(-x, y, -z);
            case EAST -> o.offset(z, y, -x);
            case WEST -> o.offset(-z, y, x);
            default -> o.offset(x, y, z);
        };
    }
}
