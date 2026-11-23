package com.pescacoleccion.nilo;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * El muelle de Nilo: busca una orilla con agua enfrente (cerca de donde se pescó) y arma la lista de bloques. Solo se ponen bloques
 * donde hay agua, aire o plantas: nunca rompe construcciones de nadie.
 * <p>
 * Forma (vista desde arriba, la orilla abajo): tablones de 3 de ancho y {@link #LARGO} de largo, postes que bajan al agua, dos
 * faroles en la punta, un barril y un banquito. Nilo pesca parado en el centro, cerca de la punta.
 */
public final class Muelle {
    public static final int LARGO = 5;

    /** Un bloque del muelle. */
    public record Pieza(BlockPos pos, BlockState state) {
    }

    /**
     * Un buen sitio: {@code orilla} = el bloque de tierra donde empieza, {@code hacia} = dirección del agua, {@code piso} = altura de
     * los tablones, {@code parado} = donde se para Nilo a pescar, {@code agua} = donde cae su anzuelo.
     */
    public record Sitio(BlockPos orilla, Direction hacia, int piso, BlockPos parado, BlockPos agua, List<Pieza> piezas) {
    }

    private Muelle() {
    }

    /** El mejor sitio cerca del agua {@code agua} (el más cercano al jugador); null si no hay ninguno. */
    public static Sitio buscar(ServerLevel l, BlockPos agua, BlockPos jugador) {
        Sitio best = null;
        double bestD = Double.MAX_VALUE;
        for (int dy = -2; dy <= 2; dy++) {
            for (int dx = -10; dx <= 10; dx++) {
                for (int dz = -10; dz <= 10; dz++) {
                    BlockPos s = agua.offset(dx, dy, dz);
                    for (Direction d : Direction.Plane.HORIZONTAL) {
                        Sitio st = probar(l, s, d);
                        if (st != null) {
                            double dist = s.distSqr(jugador) + s.distSqr(agua) * 0.25;
                            if (dist < bestD) {
                                best = st;
                                bestD = dist;
                            }
                        }
                    }
                }
            }
        }
        return best;
    }

    private static boolean agua(BlockState st) {
        return st.is(Blocks.WATER) || st.is(Blocks.SEAGRASS) || st.is(Blocks.TALL_SEAGRASS) || st.is(Blocks.KELP) || st.is(Blocks.KELP_PLANT);
    }

    /** Aire o plantas que se pueden quitar (pasto, flores). */
    private static boolean libre(BlockState st) {
        return st.isAir() || st.canBeReplaced() && st.getFluidState().isEmpty();
    }

    /** ¿Cabe el muelle empezando en la tierra {@code s} hacia {@code d}? */
    public static Sitio probar(ServerLevel l, BlockPos s, Direction d) {
        BlockState suelo = l.getBlockState(s);
        if (agua(suelo) || !suelo.getFluidState().isEmpty() || !suelo.isFaceSturdy(l, s, Direction.UP)) {
            return null;
        }
        if (!libre(l.getBlockState(s.above())) || !libre(l.getBlockState(s.above(2)))) {
            return null;
        }
        BlockPos frente = s.relative(d);
        // al ras del agua (la tierra está a la altura del agua) o elevado (la tierra está un bloque arriba del agua)
        boolean alRas = agua(l.getBlockState(frente));
        if (!alRas && !(libre(l.getBlockState(frente)) && agua(l.getBlockState(frente.below())))) {
            return null;
        }
        Direction p = d.getClockWise();
        int piso = s.getY();
        for (int i = 1; i <= LARGO; i++) {
            for (int j = -1; j <= 1; j++) {
                BlockPos c = s.relative(d, i).relative(p, j);
                BlockState st = l.getBlockState(c);
                if (alRas ? !agua(st) : !(libre(st) && agua(l.getBlockState(c.below())))) {
                    return null;
                }
                if (!libre(l.getBlockState(c.above())) || !libre(l.getBlockState(c.above(2)))) {
                    return null;
                }
            }
        }
        // agua abierta delante de la punta para pescar
        BlockPos lanza = s.relative(d, LARGO + 3);
        BlockPos aguaLanza = alRas ? lanza : lanza.below();
        if (!agua(l.getBlockState(aguaLanza))) {
            return null;
        }
        return new Sitio(s, d, piso, s.relative(d, LARGO - 1).above(), aguaLanza, piezas(l, s, d, piso));
    }

    /** Los bloques del muelle, en el orden en que Nilo los pone (de la orilla hacia el agua). */
    private static List<Pieza> piezas(ServerLevel l, BlockPos s, Direction d, int piso) {
        List<Pieza> out = new ArrayList<>();
        Direction p = d.getClockWise();
        BlockState tabla = Blocks.SPRUCE_PLANKS.defaultBlockState();
        for (int i = 1; i <= LARGO; i++) {
            for (int j : new int[] {0, -1, 1}) {
                BlockPos c = new BlockPos(s.relative(d, i).relative(p, j).getX(), piso, s.relative(d, i).relative(p, j).getZ());
                out.add(new Pieza(c, tabla));
                // postes en las esquinas que bajan al agua
                if (j != 0 && (i == 1 || i == 3 || i == LARGO)) {
                    for (int k = 1; k <= 4; k++) {
                        BlockPos b = c.below(k);
                        BlockState st = l.getBlockState(b);
                        if (!agua(st) && !st.isAir()) {
                            break;
                        }
                        out.add(new Pieza(b, Blocks.SPRUCE_FENCE.defaultBlockState().setValue(BlockStateProperties.WATERLOGGED, agua(st))));
                    }
                }
            }
        }
        // faroles en la punta
        for (int j : new int[] {-1, 1}) {
            BlockPos c = s.relative(d, LARGO).relative(p, j);
            BlockPos poste = new BlockPos(c.getX(), piso + 1, c.getZ());
            out.add(new Pieza(poste, Blocks.SPRUCE_FENCE.defaultBlockState()));
            out.add(new Pieza(poste.above(), Blocks.LANTERN.defaultBlockState()));
        }
        // barril y banquito cerca de la orilla
        BlockPos barril = s.relative(d, 1).relative(p, 1);
        out.add(new Pieza(new BlockPos(barril.getX(), piso + 1, barril.getZ()), Blocks.BARREL.defaultBlockState().setValue(BarrelBlock.FACING,
                Direction.UP)));
        BlockPos banco = s.relative(d, 2).relative(p, -1);
        out.add(new Pieza(new BlockPos(banco.getX(), piso + 1, banco.getZ()), Blocks.SPRUCE_STAIRS.defaultBlockState().setValue(
                StairBlock.FACING, p.getOpposite())));
        return out;
    }

    /** ¿Se puede poner esta pieza todavía? (sigue habiendo agua, aire o una planta) */
    public static boolean cabe(ServerLevel l, Pieza pz) {
        BlockState st = l.getBlockState(pz.pos());
        return agua(st) || libre(st);
    }
}
