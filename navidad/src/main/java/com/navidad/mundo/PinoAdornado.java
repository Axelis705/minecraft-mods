package com.navidad.mundo;

import com.mojang.serialization.Codec;
import com.navidad.bloque.CajaRegalo;
import com.navidad.bloque.CajaRegaloBE;
import com.navidad.bloque.deco.Deco;
import com.navidad.objeto.Premios;
import com.navidad.registry.ModRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * El pino adornado que sale solo en los biomas nevados (raro: uno cada tantos chunks): un pino de verdad con esferas de colores colgando
 * de las ramas, una luz en la punta y regalos al pie. Uno de los regalos es una caja de regalo de verdad, sin dueño, con algo adentro.
 * La nieve de encima la pone el mismo mundo (la capa de nieve se genera después).
 */
public class PinoAdornado extends Feature<NoneFeatureConfiguration> {
    private static final String[] ESFERAS = {"esfera_roja", "esfera_dorada", "esfera_azul", "esfera_plata", "esfera_verde"};
    private static final String[] REGALOS = {"regalo_rojo", "regalo_verde", "regalo_azul", "regalo_dorado"};

    public PinoAdornado(Codec<NoneFeatureConfiguration> c) {
        super(c);
    }

    private static boolean suelo(BlockState b) {
        return b.is(BlockTags.DIRT) || b.is(Blocks.SNOW_BLOCK) || b.is(Blocks.GRASS_BLOCK);
    }

    private static boolean libre(BlockState b) {
        return b.isAir() || b.canBeReplaced() || b.is(Blocks.SNOW);
    }

    /** El radio de la copa en cada altura: escalones que se van cerrando, como un pino de Navidad. */
    static int radio(int y, int alto) {
        if (y < 2 || y > alto) {
            return -1;
        }
        int d = alto - y;
        int r = Math.min(3, (d + 1) / 2);
        if (d > 0 && d % 3 == 0) {
            r--;
        }
        return Math.max(0, r);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        WorldGenLevel l = ctx.level();
        RandomSource r = ctx.random();
        BlockPos o = ctx.origin();
        while (o.getY() > l.getMinBuildHeight() + 2 && libre(l.getBlockState(o.below()))) {
            o = o.below();
        }
        if (!suelo(l.getBlockState(o.below()))) {
            return false;
        }
        int alto = 8 + r.nextInt(4);
        // que haya lugar: nada sólido en el tronco ni en la copa
        for (int y = 0; y <= alto + 2; y++) {
            int rad = Math.max(0, radio(y, alto));
            for (int dx = -rad; dx <= rad; dx++) {
                for (int dz = -rad; dz <= rad; dz++) {
                    BlockState b = l.getBlockState(o.offset(dx, y, dz));
                    if (!libre(b) && !b.is(BlockTags.LEAVES)) {
                        return false;
                    }
                }
            }
        }
        BlockState hoja = Blocks.SPRUCE_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true).setValue(LeavesBlock.DISTANCE, 1);
        for (int y = 0; y < alto; y++) {
            l.setBlock(o.above(y), Blocks.SPRUCE_LOG.defaultBlockState(), 2);
        }
        for (int y = 2; y <= alto + 1; y++) {
            int rad = y > alto ? 0 : radio(y, alto);
            for (int dx = -rad; dx <= rad; dx++) {
                for (int dz = -rad; dz <= rad; dz++) {
                    if (dx * dx + dz * dz > rad * rad + rad || (dx == 0 && dz == 0 && y < alto)) {
                        continue;
                    }
                    BlockPos p = o.offset(dx, y, dz);
                    if (libre(l.getBlockState(p))) {
                        l.setBlock(p, hoja, 2);
                    }
                }
            }
        }
        // la luz de la punta
        Block luces = Deco.block("bloque_luces");
        if (luces != null) {
            l.setBlock(o.above(alto + 2), luces.defaultBlockState(), 2);
        }
        // esferas colgando abajo de las ramas de afuera
        for (int y = 3; y <= alto; y++) {
            int rad = radio(y, alto);
            for (int dx = -rad; dx <= rad; dx++) {
                for (int dz = -rad; dz <= rad; dz++) {
                    int d2 = dx * dx + dz * dz;
                    if (d2 < Math.max(1, (rad - 1) * (rad - 1)) || d2 > rad * rad + rad) {
                        continue;
                    }
                    BlockPos p = o.offset(dx, y, dz);
                    if (!l.getBlockState(p).is(Blocks.SPRUCE_LEAVES) || !l.getBlockState(p.below()).isAir() || r.nextInt(3) != 0) {
                        continue;
                    }
                    Block e = Deco.block(ESFERAS[r.nextInt(ESFERAS.length)]);
                    if (e != null) {
                        l.setBlock(p.below(), girar(e.defaultBlockState(), r), 2);
                    }
                }
            }
        }
        // regalos al pie del árbol (y uno de verdad)
        int puestos = 0;
        boolean real = false;
        for (int k = 0; k < 14 && puestos < 4; k++) {
            int dx = r.nextInt(7) - 3, dz = r.nextInt(7) - 3;
            if (Math.abs(dx) + Math.abs(dz) < 2) {
                continue;
            }
            BlockPos p = o.offset(dx, 0, dz);
            while (p.getY() > o.getY() - 3 && libre(l.getBlockState(p.below()))) {
                p = p.below();
            }
            while (p.getY() < o.getY() + 3 && !libre(l.getBlockState(p))) {
                p = p.above();
            }
            if (!libre(l.getBlockState(p)) || !l.getBlockState(p.below()).isFaceSturdy(l, p.below(), Direction.UP)) {
                continue;
            }
            if (!real) {
                BlockState caja = ModRegistry.CAJA_REGALO.get().defaultBlockState().setValue(CajaRegalo.LLENA, true);
                l.setBlock(p, girar(caja, r), 2);
                BlockEntity be = l.getBlockEntity(p);
                if (be instanceof CajaRegaloBE c) {
                    c.contenido = Premios.sacar(r.nextInt(6) == 0 ? Premios.Rareza.EPICO : Premios.Rareza.RARO, r);
                    c.de = "Santa";
                    c.para = "";
                    c.setChanged();
                }
                real = true;
            } else {
                Block g = Deco.block(REGALOS[r.nextInt(REGALOS.length)]);
                if (g != null) {
                    l.setBlock(p, girar(g.defaultBlockState(), r), 2);
                }
            }
            puestos++;
        }
        return true;
    }

    private static BlockState girar(BlockState s, RandomSource r) {
        if (s.hasProperty(HorizontalDirectionalBlock.FACING)) {
            return s.setValue(HorizontalDirectionalBlock.FACING, Direction.Plane.HORIZONTAL.getRandomDirection(r));
        }
        return s;
    }
}
