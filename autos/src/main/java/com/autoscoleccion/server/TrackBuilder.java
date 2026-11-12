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

    /** Los materiales de cada tema. lamp: cada cuántos bloques del muro va una luz (0 = ninguna). */
    private record Theme(BlockState road, BlockState lineA, BlockState lineB, BlockState curbA, BlockState curbB, BlockState wallA, BlockState wallB,
            BlockState wallTop, BlockState base, BlockState ground, BlockState lamp, int lampEvery, BlockState centerLine) {
    }

    private static BlockState b(net.minecraft.world.level.block.Block block) {
        return block.defaultBlockState();
    }

    private static Theme theme(int t) {
        return switch (t) {
            case 1 -> new Theme(b(Blocks.SMOOTH_SANDSTONE), b(Blocks.WHITE_CONCRETE), b(Blocks.BLACK_CONCRETE), b(Blocks.ORANGE_TERRACOTTA),
                    b(Blocks.WHITE_TERRACOTTA), b(Blocks.CUT_RED_SANDSTONE), b(Blocks.CHISELED_RED_SANDSTONE), b(Blocks.SMOOTH_RED_SANDSTONE_SLAB),
                    b(Blocks.SANDSTONE), b(Blocks.SAND), null, 0, null);
            case 2 -> new Theme(b(Blocks.BLACK_CONCRETE), b(Blocks.WHITE_CONCRETE), b(Blocks.GRAY_CONCRETE), b(Blocks.MAGENTA_CONCRETE),
                    b(Blocks.CYAN_CONCRETE), b(Blocks.GRAY_CONCRETE), b(Blocks.SEA_LANTERN), b(Blocks.POLISHED_BLACKSTONE_SLAB), b(Blocks.STONE),
                    b(Blocks.SMOOTH_STONE), b(Blocks.SEA_LANTERN), 9, b(Blocks.YELLOW_CONCRETE));
            case 3 -> new Theme(b(Blocks.PACKED_ICE), b(Blocks.WHITE_CONCRETE), b(Blocks.BLUE_CONCRETE), b(Blocks.BLUE_CONCRETE), b(Blocks.WHITE_CONCRETE),
                    b(Blocks.SNOW_BLOCK), b(Blocks.LIGHT_BLUE_CONCRETE), b(Blocks.SMOOTH_QUARTZ_SLAB), b(Blocks.STONE), b(Blocks.SNOW_BLOCK), null, 0, null);
            case 4 -> new Theme(b(Blocks.POLISHED_BLACKSTONE), b(Blocks.WHITE_CONCRETE), b(Blocks.BLACK_CONCRETE), b(Blocks.ORANGE_CONCRETE),
                    b(Blocks.BLACK_CONCRETE), b(Blocks.BLACKSTONE), b(Blocks.MAGMA_BLOCK), b(Blocks.POLISHED_BLACKSTONE_SLAB), b(Blocks.BLACKSTONE),
                    b(Blocks.BASALT), null, 0, null);
            default -> new Theme(b(Blocks.GRAY_CONCRETE), b(Blocks.WHITE_CONCRETE), b(Blocks.BLACK_CONCRETE), b(Blocks.RED_CONCRETE), b(Blocks.WHITE_CONCRETE),
                    b(Blocks.LIGHT_GRAY_CONCRETE), b(Blocks.WHITE_CONCRETE), b(Blocks.SMOOTH_STONE_SLAB), b(Blocks.STONE), null, null, 0, null);
        };
    }

    /** Un número "al azar" pero siempre el mismo para cada lugar (así la pista sale igual si se repite). */
    private static int hash(int x, int z) {
        int h = x * 73856093 ^ z * 19349663;
        h ^= h >>> 13;
        h *= 0x5bd1e995;
        return (h ^ (h >>> 15)) & 0x7FFFFFFF;
    }

    /** @return cuántos bloques se pusieron */
    public static int build(ServerLevel level, BlockPos pos, Direction facing, int themeIdx) {
        TrackLayout t = TrackLayout.ALL[Math.max(0, Math.min(TrackLayout.ALL.length - 1, themeIdx))];
        Theme th = theme(themeIdx);
        float hw = t.halfW;
        // alrededor del muro hay una franja decorada (arena con cactus, banquetas con faroles, nieve con pinos, roca con magma)
        float decoW = th.ground != null ? 5.0F : 0;
        float[] bb = t.bounds(hw + 2.2F + decoW);
        int floorY = pos.getY() - 1;
        int count = 0;
        BlockState grid = Blocks.LIGHT_GRAY_CONCRETE.defaultBlockState();
        for (int lx = (int) Math.floor(bb[0]); lx <= (int) Math.ceil(bb[2]); lx++) {
            for (int lz = (int) Math.floor(bb[1]); lz <= (int) Math.ceil(bb[3]); lz++) {
                float[] nr = t.nearest(lx, lz);
                float s = nr[0], d = nr[1];
                if (d > hw + 2.1F + decoW) {
                    continue;
                }
                double[] w = toWorld(pos, facing, lx, lz);
                int wx = (int) Math.floor(w[0]), wz = (int) Math.floor(w[1]);
                if (Math.abs(wx - pos.getX()) <= 1 && Math.abs(wz - pos.getZ()) <= 1) {
                    continue;
                }
                BlockPos floor = new BlockPos(wx, floorY, wz);
                // no se toca nada que tenga contenido (cofres, hornos, máquinas...)
                boolean busy = false;
                for (int dy = 0; dy <= 6 && !busy; dy++) {
                    busy = level.getBlockEntity(floor.above(dy)) != null || level.getBlockState(floor.above(dy)).is(Blocks.BEDROCK);
                }
                if (busy) {
                    continue;
                }
                float sw = Math.min(s, t.length - s);
                BlockState top;
                boolean isWall = false, isDeco = false;
                if (d <= hw) {
                    if (sw < 1.0F) {
                        top = ((lx + lz) & 1) == 0 ? th.lineA : th.lineB;
                    } else if (s > t.length - 9.5F && s < t.length - 1.5F && d > 0.7F && d < hw - 0.6F
                            && ((int) Math.floor((t.length - s) / 2.4F) % 2 == 0)) {
                        top = grid;
                    } else if (th.centerLine != null && d < 0.5F && ((int) Math.floor(s / 2.0F) & 1) == 0) {
                        top = th.centerLine;
                    } else {
                        top = th.road;
                    }
                } else if (d <= hw + 1.05F) {
                    top = ((int) Math.floor(s / 2.0F) & 1) == 0 ? th.curbA : th.curbB;
                } else if (d <= hw + 2.1F) {
                    top = th.base;
                    isWall = true;
                } else {
                    top = th.ground;
                    isDeco = true;
                }
                count += set(level, floor, top);
                for (int dy = 1; dy <= 6; dy++) {
                    BlockPos up = floor.above(dy);
                    BlockState want = Blocks.AIR.defaultBlockState();
                    if (isWall) {
                        boolean lamp = th.lampEvery > 0 && ((int) Math.floor(s) % th.lampEvery) == 0;
                        if (dy == 1) {
                            want = lamp ? th.wallB : ((int) Math.floor(s / 3.0F) & 1) == 0 ? th.wallA : th.wallB == th.lamp ? th.wallA : th.wallB;
                        } else if (dy == 2) {
                            want = th.wallTop;
                        }
                    } else if (isDeco) {
                        want = deco(themeIdx, dy, lx, lz, d - hw - 2.1F);
                    }
                    if (want.isAir() && level.getBlockState(up).isAir()) {
                        continue;
                    }
                    count += set(level, up, want);
                }
                for (int dy = 1; dy <= 3; dy++) {
                    BlockPos dn = floor.below(dy);
                    BlockState st = level.getBlockState(dn);
                    if (st.isAir() || st.canBeReplaced() || !st.getFluidState().isEmpty()) {
                        count += set(level, dn, th.base);
                    }
                }
            }
        }
        return count;
    }

    /** Qué va en la franja decorada a la altura dy (1 = justo encima del piso). Todo separado para que nada se toque. */
    private static BlockState deco(int theme, int dy, int lx, int lz, float out) {
        int h = hash(lx, lz);
        boolean spot = h % 23 == 0 && out > 1.2F && out < 4.0F;
        boolean small = h % 11 == 0 && out > 0.8F;
        switch (theme) {
            case 1 -> {
                if (spot && dy <= 1 + h % 3) {
                    return Blocks.CACTUS.defaultBlockState();
                }
                if (!spot && small && dy == 1) {
                    return Blocks.DEAD_BUSH.defaultBlockState();
                }
            }
            case 2 -> {
                // faroles: poste negro con una lámpara de mar arriba
                if (spot) {
                    return dy <= 3 ? Blocks.POLISHED_BLACKSTONE_WALL.defaultBlockState() : dy == 4 ? Blocks.SEA_LANTERN.defaultBlockState()
                            : Blocks.AIR.defaultBlockState();
                }
            }
            case 3 -> {
                // pinos: tronco y copa de hojas (que no se caen solas)
                BlockState leaves = Blocks.SPRUCE_LEAVES.defaultBlockState().setValue(net.minecraft.world.level.block.LeavesBlock.PERSISTENT, true);
                if (spot) {
                    return dy <= 4 ? Blocks.SPRUCE_LOG.defaultBlockState() : leaves;
                }
                boolean nextToTree = false;
                for (int[] o : new int[][] {{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
                    nextToTree |= hash(lx + o[0], lz + o[1]) % 23 == 0;
                }
                if (nextToTree) {
                    return dy >= 2 && dy <= 4 ? leaves : Blocks.AIR.defaultBlockState();
                }
                if (small && dy == 1) {
                    return Blocks.SNOW.defaultBlockState();
                }
            }
            case 4 -> {
                // picos de obsidiana con la punta brillando, y charcos de magma
                if (spot) {
                    int top = 2 + h % 3;
                    return dy < top ? Blocks.OBSIDIAN.defaultBlockState() : dy == top ? Blocks.CRYING_OBSIDIAN.defaultBlockState()
                            : Blocks.AIR.defaultBlockState();
                }
            }
            default -> {
            }
        }
        return Blocks.AIR.defaultBlockState();
    }

    private static int set(ServerLevel level, BlockPos p, BlockState st) {
        if (level.getBlockState(p) == st) {
            return 0;
        }
        level.setBlock(p, st, Block.UPDATE_CLIENTS);
        return 1;
    }
}
