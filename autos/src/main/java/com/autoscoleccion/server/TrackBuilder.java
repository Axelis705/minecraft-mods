package com.autoscoleccion.server;

import com.autoscoleccion.game.TrackLayout;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Construye la mini pista con bloques normales de Minecraft delante del bloque de la pista: asfalto (concreto gris), meta a cuadros,
 * pianos rojo y blanco en las orillas, y un muro bajito para que los autos no se salgan. Limpia el aire encima y rellena debajo.
 */
public final class TrackBuilder {
    private TrackBuilder() {
    }

    /** Pasa de coordenadas de la pista (x a la derecha, z adelante) a coordenadas del mundo (centro del bloque). */
    public static double[] toWorld(BlockPos pos, Direction facing, double lx, double lz) {
        Direction right = facing.getClockWise();
        double wx = pos.getX() + 0.5 + right.getStepX() * lx + facing.getStepX() * lz;
        double wz = pos.getZ() + 0.5 + right.getStepZ() * lx + facing.getStepZ() * lz;
        return new double[] {wx, wz};
    }

    /** Lo contrario: del mundo a la pista. */
    public static float[] toLocal(BlockPos pos, Direction facing, double wx, double wz) {
        Direction right = facing.getClockWise();
        double dx = wx - (pos.getX() + 0.5), dz = wz - (pos.getZ() + 0.5);
        return new float[] {(float) (dx * right.getStepX() + dz * right.getStepZ()), (float) (dx * facing.getStepX() + dz * facing.getStepZ())};
    }

    /** Rumbo (yRot de Minecraft) de una dirección local. */
    public static float yaw(Direction facing, float ltx, float ltz) {
        Direction right = facing.getClockWise();
        double dx = right.getStepX() * ltx + facing.getStepX() * ltz;
        double dz = right.getStepZ() * ltx + facing.getStepZ() * ltz;
        return (float) Math.toDegrees(Math.atan2(-dx, dz));
    }

    /** @return cuántos bloques se pusieron */
    public static int build(ServerLevel level, BlockPos pos, Direction facing) {
        TrackLayout t = TrackLayout.MAIN;
        float hw = TrackLayout.HALF_W;
        float[] b = t.bounds(hw + 2.2F);
        int floorY = pos.getY() - 1;
        int count = 0;
        BlockState asphalt = Blocks.GRAY_CONCRETE.defaultBlockState();
        BlockState line = Blocks.WHITE_CONCRETE.defaultBlockState();
        BlockState lineDark = Blocks.BLACK_CONCRETE.defaultBlockState();
        BlockState red = Blocks.RED_CONCRETE.defaultBlockState();
        BlockState white = Blocks.WHITE_CONCRETE.defaultBlockState();
        BlockState wall = Blocks.LIGHT_GRAY_CONCRETE.defaultBlockState();
        BlockState wallTop = Blocks.SMOOTH_STONE_SLAB.defaultBlockState();
        BlockState base = Blocks.STONE.defaultBlockState();
        BlockState grid = Blocks.LIGHT_GRAY_CONCRETE.defaultBlockState();
        for (int lx = (int) Math.floor(b[0]); lx <= (int) Math.ceil(b[2]); lx++) {
            for (int lz = (int) Math.floor(b[1]); lz <= (int) Math.ceil(b[3]); lz++) {
                float[] nr = t.nearest(lx, lz);
                float s = nr[0], d = nr[1];
                if (d > hw + 2.1F) {
                    continue;
                }
                double[] w = toWorld(pos, facing, lx, lz);
                int wx = (int) Math.floor(w[0]), wz = (int) Math.floor(w[1]);
                if (wx == pos.getX() && wz == pos.getZ()) {
                    continue;
                }
                BlockPos floor = new BlockPos(wx, floorY, wz);
                // no se toca nada que tenga contenido (cofres, hornos, máquinas...)
                boolean busy = false;
                for (int dy = 0; dy <= 5 && !busy; dy++) {
                    busy = level.getBlockEntity(floor.above(dy)) != null || level.getBlockState(floor.above(dy)).is(Blocks.BEDROCK);
                }
                if (busy) {
                    continue;
                }
                float sw = Math.min(s, t.length - s);
                BlockState top;
                boolean isWall = false;
                if (d <= hw) {
                    if (sw < 1.0F) {
                        top = ((lx + lz) & 1) == 0 ? line : lineDark;
                    } else if (s > t.length - 9.5F && s < t.length - 1.5F && Math.abs(d) > 0.7F && ((int) Math.floor((t.length - s) / 2.2F) % 2 == 0)
                            && d < hw - 0.6F) {
                        // marcas de la parrilla de salida
                        top = grid;
                    } else {
                        top = asphalt;
                    }
                } else if (d <= hw + 1.05F) {
                    top = ((int) Math.floor(s / 2.0F) & 1) == 0 ? red : white;
                } else {
                    top = base;
                    isWall = true;
                }
                count += set(level, floor, top);
                for (int dy = 1; dy <= 5; dy++) {
                    BlockPos up = floor.above(dy);
                    if (dy == 1 && isWall) {
                        count += set(level, up, ((int) Math.floor(s / 3.0F) & 1) == 0 ? wall : white);
                    } else if (dy == 2 && isWall) {
                        count += set(level, up, wallTop);
                    } else if (!level.getBlockState(up).isAir()) {
                        count += set(level, up, Blocks.AIR.defaultBlockState());
                    }
                }
                for (int dy = 1; dy <= 3; dy++) {
                    BlockPos dn = floor.below(dy);
                    BlockState st = level.getBlockState(dn);
                    if (st.isAir() || st.canBeReplaced() || !st.getFluidState().isEmpty()) {
                        count += set(level, dn, base);
                    }
                }
            }
        }
        return count;
    }

    private static int set(ServerLevel level, BlockPos p, BlockState st) {
        if (level.getBlockState(p) == st) {
            return 0;
        }
        level.setBlock(p, st, Block.UPDATE_CLIENTS);
        return 1;
    }
}
