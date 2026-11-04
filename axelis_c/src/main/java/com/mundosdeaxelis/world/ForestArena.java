package com.mundosdeaxelis.world;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * La arena del Rey Brote: un claro de bosque redondo (radio 32) con un árbol gigante cada pocos pasos en el borde, ruinas
 * con musgo y una plataforma en el centro. Se construye una sola vez, poco a poco (unas filas por tick) para no trabar el juego.
 * Un anillo de bloques barrera invisibles impide caer al vacío.
 */
public final class ForestArena {
    public static final int Y = 64;
    public static final int R = 32;
    private static final int ROWS_PER_TICK = 5;

    private ForestArena() {
    }

    private static final BlockPos MARKER = new BlockPos(0, Y, 0);

    public static boolean isBuilt(ServerLevel level) {
        return level.getBlockState(MARKER).is(Blocks.CHISELED_STONE_BRICKS);
    }

    /** Construye la arena (si no existe) y avisa al terminar. */
    public static void ensureBuilt(ServerLevel level, Runnable done) {
        if (isBuilt(level)) {
            ensureRim(level, done);
            return;
        }
        buildRows(level, -R - 2, () -> ensureRim(level, done));
    }

    // ------------------------------------------------------------------ muro de acantilado alrededor

    /** Radio interior y exterior del muro que rodea la arena (así desde dentro no se ve la dungeon ni el vacío). */
    private static final int RIM_IN = 30;
    private static final double RIM_OUT = 34.6;
    private static final BlockPos RIM_MARK = new BlockPos(0, Y - 3, 0);

    private static boolean rimBuilt(ServerLevel level) {
        return level.getBlockState(RIM_MARK).is(Blocks.MOSSY_COBBLESTONE);
    }

    /** Levanta el muro (si no existe), también en arenas ya construidas. Se hace por filas para no trabar el juego. */
    public static void ensureRim(ServerLevel level, Runnable done) {
        if (rimBuilt(level)) {
            done.run();
            return;
        }
        rimRows(level, -36, done);
    }

    private static void rimRows(ServerLevel level, int fromX, Runnable done) {
        int toX = Math.min(36, fromX + ROWS_PER_TICK - 1);
        for (int x = fromX; x <= toX; x++) {
            for (int z = -36; z <= 36; z++) {
                rimColumn(level, x, z);
            }
        }
        if (toX >= 36) {
            set(level, 0, Y - 3, 0, Blocks.MOSSY_COBBLESTONE.defaultBlockState());
            done.run();
        } else {
            Scheduler.runLater(1, () -> rimRows(level, toX + 1, done));
        }
    }

    private static boolean rimFree(BlockState st) {
        return st.isAir() || st.is(Blocks.BARRIER) || st.canBeReplaced();
    }

    private static void rimColumn(ServerLevel level, int x, int z) {
        double d = Math.hypot(x, z);
        if (d < RIM_IN || d > RIM_OUT) {
            return;
        }
        double ang = Math.atan2(z, x);
        int top = 27 + (int) Math.round(4.0 * Math.sin(ang * 5.0) + 3.0 * Math.sin(ang * 11.0 + 1.3) + (hash(x, z) & 3));
        boolean tunnel = Math.abs(x) <= 2 && z >= 28;
        long h = hash(x * 7, z * 13);
        for (int y = 1; y <= top; y++) {
            if (tunnel && y <= 5) {
                continue;
            }
            BlockPos p = new BlockPos(x, Y + y, z);
            if (!rimFree(level.getBlockState(p))) {
                continue;
            }
            BlockState b;
            if (y == top) {
                b = Blocks.GRASS_BLOCK.defaultBlockState();
            } else {
                int r = (int) ((h + y * 31L) & 15);
                b = r < 6 ? Blocks.MOSSY_STONE_BRICKS.defaultBlockState()
                        : r < 10 ? Blocks.MOSSY_COBBLESTONE.defaultBlockState()
                        : r < 12 ? Blocks.STONE_BRICKS.defaultBlockState()
                        : r < 14 ? Blocks.MOSS_BLOCK.defaultBlockState() : Blocks.STONE.defaultBlockState();
            }
            level.setBlock(p, b, 2);
        }
        // copas de hojas sobre el borde
        if ((h & 3) != 0) {
            int ly = Y + top + 1;
            BlockPos lp = new BlockPos(x, ly, z);
            if (level.getBlockState(lp).isAir()) {
                BlockState lv = (h & 12) == 0 ? Blocks.FLOWERING_AZALEA_LEAVES.defaultBlockState() : Blocks.OAK_LEAVES.defaultBlockState();
                if (lv.hasProperty(LeavesBlock.PERSISTENT)) {
                    lv = lv.setValue(LeavesBlock.PERSISTENT, true);
                }
                level.setBlock(lp, lv, 2);
                if ((h & 48) == 0 && level.getBlockState(lp.above()).isAir()) {
                    level.setBlock(lp.above(), lv, 2);
                }
            }
        }
    }

    /** Cierra (o abre) el paso del muro hacia la dungeon: un muro de piedra de 2 bloques que cierra el túnel. */
    public static void sealGateway(ServerLevel level, boolean sealed) {
        for (int x = -2; x <= 2; x++) {
            for (int z = RIM_IN; z <= RIM_IN + 1; z++) {
                for (int y = Y + 1; y <= Y + 5; y++) {
                    BlockPos p = new BlockPos(x, y, z);
                    BlockState st = level.getBlockState(p);
                    if (sealed && st.isAir()) {
                        level.setBlock(p, Blocks.MOSSY_STONE_BRICKS.defaultBlockState(), 2);
                    } else if (!sealed && st.is(Blocks.MOSSY_STONE_BRICKS)) {
                        level.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
                    }
                }
            }
        }
    }

    /** ¿Hay alguien dentro del túnel del muro? */
    public static boolean tunnelBusy(ServerLevel level) {
        return !level.getEntitiesOfClass(net.minecraft.world.entity.player.Player.class,
                new net.minecraft.world.phys.AABB(-3, Y, RIM_IN - 2, 4, Y + 8, 38)).isEmpty();
    }

    private static void buildRows(ServerLevel level, int fromX, Runnable done) {
        int toX = Math.min(R + 2, fromX + ROWS_PER_TICK - 1);
        for (int x = fromX; x <= toX; x++) {
            for (int z = -R - 2; z <= R + 2; z++) {
                column(level, x, z);
            }
        }
        if (toX >= R + 2) {
            Scheduler.runLater(1, () -> {
                decorate(level);
                set(level, 0, Y, 0, Blocks.CHISELED_STONE_BRICKS.defaultBlockState());
                done.run();
            });
        } else {
            Scheduler.runLater(1, () -> buildRows(level, toX + 1, done));
        }
    }

    /** Quita la barrera invisible del paso hacia la dungeon (z 32..34, x -2..2); también en arenas ya construidas. */
    public static void openGateway(ServerLevel level) {
        sealGateway(level, false);
        for (int x = -2; x <= 2; x++) {
            for (int z = 32; z <= 34; z++) {
                for (int y = Y + 1; y <= Y + 40; y++) {
                    BlockPos p = new BlockPos(x, y, z);
                    if (level.getBlockState(p).is(Blocks.BARRIER)) {
                        level.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
                    }
                }
            }
        }
    }

    private static long hash(int x, int z) {
        long h = x * 374761393L + z * 668265263L;
        h = (h ^ (h >> 13)) * 1274126177L;
        return h ^ (h >> 16);
    }

    private static void column(ServerLevel level, int x, int z) {
        double d = Math.hypot(x, z);
        if (d <= R) {
            long h = hash(x, z);
            double patch = Math.sin(x * 0.33) * Math.cos(z * 0.29) + Math.sin((x + z) * 0.16);
            BlockState top = Blocks.GRASS_BLOCK.defaultBlockState();
            if (patch > 0.95) {
                top = Blocks.MOSS_BLOCK.defaultBlockState();
            } else if ((h & 31) == 0) {
                top = Blocks.COARSE_DIRT.defaultBlockState();
            } else if ((h & 63) == 1) {
                top = Blocks.PODZOL.defaultBlockState();
            }
            if (d <= 5.5) {
                top = ((h & 3) == 0) ? Blocks.MOSSY_COBBLESTONE.defaultBlockState()
                        : (d <= 4.5 && (h & 3) == 1) ? Blocks.MOSS_BLOCK.defaultBlockState() : Blocks.MOSSY_STONE_BRICKS.defaultBlockState();
            }
            set(level, x, Y - 4, z, Blocks.STONE.defaultBlockState());
            for (int y = Y - 3; y < Y; y++) {
                set(level, x, y, z, Blocks.DIRT.defaultBlockState());
            }
            set(level, x, Y, z, top);
            // plantas sueltas (no cerca del centro)
            if (d > 7 && top.is(Blocks.GRASS_BLOCK) && (h % 11) < 2) {
                BlockState plant = switch ((int) ((h >> 8) & 7)) {
                    case 0 -> Blocks.POPPY.defaultBlockState();
                    case 1 -> Blocks.DANDELION.defaultBlockState();
                    case 2 -> Blocks.AZURE_BLUET.defaultBlockState();
                    case 3 -> Blocks.OXEYE_DAISY.defaultBlockState();
                    case 4 -> Blocks.FERN.defaultBlockState();
                    default -> Blocks.GRASS.defaultBlockState();
                };
                set(level, x, Y + 1, z, plant);
            }
        } else if (d <= R + 1.5 && !(z >= 32 && z <= 34 && Math.abs(x) <= 2)) {
            // pared invisible alrededor
            for (int y = Y + 1; y <= Y + 40; y++) {
                set(level, x, y, z, Blocks.BARRIER.defaultBlockState());
            }
            set(level, x, Y, z, Blocks.BARRIER.defaultBlockState());
        }
    }

    private static void decorate(ServerLevel level) {
        RandomSource rnd = RandomSource.create(1234L);
        // árboles gigantes en el borde
        int trees = 18;
        for (int i = 0; i < trees; i++) {
            double a = (Math.PI * 2 * i / trees) + rnd.nextDouble() * 0.12;
            double r = R - 3.5 - rnd.nextDouble() * 2.0;
            // el lado sur queda más despejado: ahí aparecen los jugadores
            if (Math.cos(a - Math.PI / 2) > 0.93) {
                continue;
            }
            tree(level, (int) Math.round(Math.cos(a) * r), (int) Math.round(Math.sin(a) * r), 11 + rnd.nextInt(5), rnd);
        }
        // ruinas con musgo
        int pillars = 7;
        for (int i = 0; i < pillars; i++) {
            double a = (Math.PI * 2 * i / pillars) + 0.4;
            double r = 14.0 + (i % 3) * 2.5;
            pillar(level, (int) Math.round(Math.cos(a) * r), (int) Math.round(Math.sin(a) * r), 3 + rnd.nextInt(4), rnd);
        }
        // pequeños pilares con linterna alrededor del centro
        for (int i = 0; i < 8; i++) {
            double a = Math.PI * 2 * i / 8;
            int px = (int) Math.round(Math.cos(a) * 7.0);
            int pz = (int) Math.round(Math.sin(a) * 7.0);
            set(level, px, Y + 1, pz, Blocks.MOSSY_STONE_BRICKS.defaultBlockState());
            set(level, px, Y + 2, pz, Blocks.MOSSY_STONE_BRICK_WALL.defaultBlockState());
            set(level, px, Y + 3, pz, Blocks.LANTERN.defaultBlockState());
        }
    }

    private static void tree(ServerLevel level, int bx, int bz, int height, RandomSource rnd) {
        BlockState log = Blocks.OAK_LOG.defaultBlockState();
        for (int dx = 0; dx <= 1; dx++) {
            for (int dz = 0; dz <= 1; dz++) {
                for (int y = 1; y <= height; y++) {
                    set(level, bx + dx, Y + y, bz + dz, log);
                }
            }
        }
        // raíces
        set(level, bx - 1, Y + 1, bz, log);
        set(level, bx + 2, Y + 1, bz + 1, log);
        set(level, bx, Y + 1, bz + 2, log);
        int cy = Y + height;
        int rh = 5;
        int rv = 4;
        for (int dx = -rh; dx <= rh + 1; dx++) {
            for (int dz = -rh; dz <= rh + 1; dz++) {
                for (int dy = -2; dy <= rv; dy++) {
                    double e = Math.pow((dx - 0.5) / rh, 2) + Math.pow((dz - 0.5) / rh, 2) + Math.pow((dy - 1.0) / rv, 2);
                    if (e <= 1.0 && rnd.nextFloat() < 0.93F) {
                        BlockPos p = new BlockPos(bx + dx, cy + dy, bz + dz);
                        if (level.getBlockState(p).isAir()) {
                            BlockState lv = rnd.nextInt(5) == 0 ? Blocks.FLOWERING_AZALEA_LEAVES.defaultBlockState()
                                    : Blocks.OAK_LEAVES.defaultBlockState();
                            if (lv.hasProperty(LeavesBlock.PERSISTENT)) {
                                lv = lv.setValue(LeavesBlock.PERSISTENT, true);
                            }
                            level.setBlock(p, lv, 2);
                        }
                    }
                }
            }
        }
    }

    private static void pillar(ServerLevel level, int px, int pz, int height, RandomSource rnd) {
        for (int y = 1; y <= height; y++) {
            if (y > 2 && rnd.nextInt(5) == 0) {
                continue; // piedra que falta
            }
            BlockState b = switch (rnd.nextInt(3)) {
                case 0 -> Blocks.MOSSY_COBBLESTONE.defaultBlockState();
                case 1 -> Blocks.MOSSY_STONE_BRICKS.defaultBlockState();
                default -> Blocks.STONE_BRICKS.defaultBlockState();
            };
            set(level, px, Y + y, pz, b);
        }
        set(level, px, Y + height + 1, pz, Blocks.MOSS_CARPET.defaultBlockState());
        if (rnd.nextBoolean()) {
            set(level, px + 1, Y + 1, pz, Blocks.MOSSY_COBBLESTONE_SLAB.defaultBlockState());
        }
    }

    private static void set(ServerLevel level, int x, int y, int z, BlockState state) {
        level.setBlock(new BlockPos(x, y, z), state, 2);
    }

    /** Dónde aparece el jefe y dónde llegan los jugadores. */
    public static net.minecraft.world.phys.Vec3 bossSpot() {
        return new net.minecraft.world.phys.Vec3(0.5, Y + 1, 0.5);
    }

    public static net.minecraft.world.phys.Vec3 arrival(int i) {
        return new net.minecraft.world.phys.Vec3(0.5 + (i % 2 == 0 ? -1.5 : 1.5), Y + 1, R - 5.5);
    }

    @SuppressWarnings("unused")
    private static void unused(Block b, float f) {
        Mth.clamp(f, 0.0F, 1.0F);
    }
}
