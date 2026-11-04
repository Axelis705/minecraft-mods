package com.mundosdeaxelis.world;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * La arena del Gran Herrero: un taller volcánico redondo (radio 32) de piedra negra y basalto, con yunques de piedra, pilares
 * con braseros y aros de luz en el suelo. La rodea un muro de acantilado con vetas de magma (se ve la caverna roja, nunca la
 * dungeon). Misma forma que {@link ForestArena} (mismo túnel al sur) para que la dungeon encaje igual. Se construye una sola vez,
 * poco a poco, para no trabar el juego.
 */
public final class ForgeArena {
    public static final int Y = 64;
    public static final int R = 32;
    private static final int ROWS_PER_TICK = 5;
    private static final BlockPos MARKER = new BlockPos(0, Y, 0);
    private static final BlockPos RIM_MARK = new BlockPos(0, Y - 3, 0);
    private static final int RIM_IN = 30;
    private static final double RIM_OUT = 34.6;

    private ForgeArena() {
    }

    public static boolean isBuilt(ServerLevel level) {
        return level.getBlockState(MARKER).is(Blocks.CHISELED_POLISHED_BLACKSTONE);
    }

    /** Construye la arena (si no existe) y avisa al terminar. */
    public static void ensureBuilt(ServerLevel level, Runnable done) {
        if (isBuilt(level)) {
            ensureRim(level, done);
            return;
        }
        buildRows(level, -R - 2, () -> ensureRim(level, done));
    }

    private static long hash(int x, int z) {
        long h = x * 374761393L + z * 668265263L;
        h = (h ^ (h >> 13)) * 1274126177L;
        return h ^ (h >> 16);
    }

    private static void set(ServerLevel level, int x, int y, int z, BlockState state) {
        level.setBlock(new BlockPos(x, y, z), state, 2);
    }

    // ------------------------------------------------------------------ suelo

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
                set(level, 0, Y, 0, Blocks.CHISELED_POLISHED_BLACKSTONE.defaultBlockState());
                done.run();
            });
        } else {
            Scheduler.runLater(1, () -> buildRows(level, toX + 1, done));
        }
    }

    private static BlockState floorTop(int x, int z, double d) {
        long h = hash(x, z);
        int r = (int) (h & 15);
        BlockState b = r < 5 ? Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState()
                : r < 8 ? Blocks.BLACKSTONE.defaultBlockState()
                : r < 10 ? Blocks.CRACKED_POLISHED_BLACKSTONE_BRICKS.defaultBlockState()
                : r < 12 ? Blocks.POLISHED_BASALT.defaultBlockState()
                : r < 14 ? Blocks.SMOOTH_BASALT.defaultBlockState() : Blocks.DEEPSLATE_TILES.defaultBlockState();
        // aros de piedra pulida a 12 y 22 del centro
        if (Math.abs(d - 12.0) < 0.6 || Math.abs(d - 22.0) < 0.6) {
            b = Blocks.POLISHED_BLACKSTONE.defaultBlockState();
            // lucecitas empotradas cada pocos bloques del aro
            double ang = Math.atan2(z, x);
            double step = Math.abs(d - 12.0) < 0.6 ? Math.PI / 6.0 : Math.PI / 10.0;
            double k = ang / step;
            if (Math.abs(k - Math.round(k)) * step * d < 0.55) {
                b = Blocks.SHROOMLIGHT.defaultBlockState();
            }
        }
        if (d <= 5.5) {
            b = ((h & 3) == 0) ? Blocks.GILDED_BLACKSTONE.defaultBlockState()
                    : ((h & 3) == 1) ? Blocks.CRACKED_POLISHED_BLACKSTONE_BRICKS.defaultBlockState() : Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState();
            if (Math.abs(d - 4.4) < 0.55) {
                b = Blocks.CHISELED_POLISHED_BLACKSTONE.defaultBlockState();
            }
        }
        return b;
    }

    private static void column(ServerLevel level, int x, int z) {
        double d = Math.hypot(x, z);
        if (d > R) {
            return;
        }
        set(level, x, Y - 4, z, Blocks.DEEPSLATE.defaultBlockState());
        for (int y = Y - 3; y < Y; y++) {
            set(level, x, y, z, Blocks.BLACKSTONE.defaultBlockState());
        }
        set(level, x, Y, z, floorTop(x, z, d));
    }

    // ------------------------------------------------------------------ adornos

    private static void decorate(ServerLevel level) {
        RandomSource rnd = RandomSource.create(4321L);
        // pilares con brasero en el borde
        int pillars = 8;
        for (int i = 0; i < pillars; i++) {
            double a = (Math.PI * 2 * i / pillars) + 0.3;
            double r = 15.0 + (i % 3) * 2.2;
            // el lado sur queda despejado: ahí aparecen los jugadores
            if (Math.sin(a) > 0.93) {
                continue;
            }
            pillar(level, (int) Math.round(Math.cos(a) * r), (int) Math.round(Math.sin(a) * r), 4 + rnd.nextInt(4), rnd);
        }
        // pequeños pilares con luz alrededor del centro
        for (int i = 0; i < 8; i++) {
            double a = Math.PI * 2 * i / 8;
            int px = (int) Math.round(Math.cos(a) * 8.0);
            int pz = (int) Math.round(Math.sin(a) * 8.0);
            if (i == 2) {
                continue; // el del sur, para dejar paso
            }
            set(level, px, Y + 1, pz, Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState());
            set(level, px, Y + 2, pz, Blocks.POLISHED_BLACKSTONE_BRICK_WALL.defaultBlockState());
            set(level, px, Y + 3, pz, Blocks.LAVA_CAULDRON.defaultBlockState());
        }
        // yunques de piedra (adorno sin menú) en diagonal, a 11 del centro
        for (int i = 0; i < 4; i++) {
            double a = Math.PI / 4 + Math.PI / 2 * i;
            int ax = (int) Math.round(Math.cos(a) * 11.0);
            int az = (int) Math.round(Math.sin(a) * 11.0);
            if (az > 5) {
                continue; // no tapar la entrada del sur
            }
            anvil(level, ax, az, i % 2 == 0);
        }
        // escombros: pilas de basalto y carbón repartidas
        for (int k = 0; k < 26; k++) {
            double a = rnd.nextDouble() * Math.PI * 2;
            double r = 6.5 + rnd.nextDouble() * 21.0;
            int x = (int) Math.round(Math.cos(a) * r);
            int z = (int) Math.round(Math.sin(a) * r);
            if (z > 18 && Math.abs(x) < 6) {
                continue;
            }
            BlockPos p = new BlockPos(x, Y + 1, z);
            if (!level.getBlockState(p).isAir() || level.getBlockState(p.below()).is(Blocks.SHROOMLIGHT)) {
                continue;
            }
            int hgt = 1 + rnd.nextInt(2);
            for (int y = 0; y < hgt; y++) {
                set(level, x, Y + 1 + y, z, rnd.nextInt(3) == 0 ? Blocks.COAL_BLOCK.defaultBlockState() : Blocks.BASALT.defaultBlockState());
            }
            if (rnd.nextInt(3) == 0) {
                set(level, x, Y + 1 + hgt, z, Blocks.BLACKSTONE_SLAB.defaultBlockState());
            }
        }
    }

    private static void pillar(ServerLevel level, int px, int pz, int height, RandomSource rnd) {
        for (int y = 1; y <= height; y++) {
            BlockState b = y == 1 ? Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState()
                    : switch (rnd.nextInt(3)) {
                        case 0 -> Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState();
                        case 1 -> Blocks.CRACKED_POLISHED_BLACKSTONE_BRICKS.defaultBlockState();
                        default -> Blocks.BLACKSTONE.defaultBlockState();
                    };
            set(level, px, Y + y, pz, b);
        }
        set(level, px, Y + height + 1, pz, Blocks.CHISELED_POLISHED_BLACKSTONE.defaultBlockState());
        set(level, px, Y + height + 2, pz, Blocks.LAVA_CAULDRON.defaultBlockState());
        set(level, px + 1, Y + 1, pz, Blocks.POLISHED_BLACKSTONE_BRICK_SLAB.defaultBlockState());
        set(level, px, Y + 1, pz + 1, Blocks.BLACKSTONE_SLAB.defaultBlockState());
    }

    /** Un yunque de piedra: base ancha, cuello y mesa con los dos cuernos (largo en x o en z). */
    private static void anvil(ServerLevel level, int cx, int cz, boolean alongX) {
        int ax = alongX ? 1 : 0;
        int az = alongX ? 0 : 1;
        for (int t = -1; t <= 1; t++) {
            set(level, cx + ax * t, Y + 1, cz + az * t, Blocks.POLISHED_DEEPSLATE.defaultBlockState());
        }
        set(level, cx, Y + 2, cz, Blocks.DEEPSLATE_BRICKS.defaultBlockState());
        for (int t = -2; t <= 2; t++) {
            boolean horn = Math.abs(t) == 2;
            set(level, cx + ax * t, Y + 3, cz + az * t, horn ? Blocks.POLISHED_BLACKSTONE_SLAB.defaultBlockState() : Blocks.CHISELED_DEEPSLATE.defaultBlockState());
        }
    }

    // ------------------------------------------------------------------ muro de acantilado alrededor

    private static boolean rimBuilt(ServerLevel level) {
        return level.getBlockState(RIM_MARK).is(Blocks.GILDED_BLACKSTONE);
    }

    /** Levanta el muro (si no existe). Se hace por filas para no trabar el juego. */
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
            set(level, 0, Y - 3, 0, Blocks.GILDED_BLACKSTONE.defaultBlockState());
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
        for (int y = -3; y <= top; y++) {
            if (tunnel && y >= 1 && y <= 5) {
                continue;
            }
            BlockPos p = new BlockPos(x, Y + y, z);
            if (!rimFree(level.getBlockState(p))) {
                continue;
            }
            BlockState b;
            int r = (int) ((h + y * 31L) & 31);
            if (y <= 0) {
                r %= 23; // en el suelo del túnel no hay magma ni luces (quemaría al pisarlo)
            }
            if (y == top) {
                b = Blocks.BASALT.defaultBlockState();
            } else if (y >= 2 && y <= 3 && d < RIM_IN + 1.5 && (h % 11) == 0) {
                b = Blocks.SHROOMLIGHT.defaultBlockState();
            } else if (r < 11) {
                b = Blocks.BLACKSTONE.defaultBlockState();
            } else if (r < 17) {
                b = Blocks.BASALT.defaultBlockState();
            } else if (r < 23) {
                b = Blocks.DEEPSLATE.defaultBlockState();
            } else if (r < 27) {
                b = Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState();
            } else if (r < 30) {
                b = Blocks.MAGMA_BLOCK.defaultBlockState();
            } else {
                b = Blocks.CRACKED_POLISHED_BLACKSTONE_BRICKS.defaultBlockState();
            }
            level.setBlock(p, b, 2);
        }
    }

    // ------------------------------------------------------------------ túnel hacia la dungeon

    private static BlockState sealBlock() {
        return Blocks.NETHER_BRICKS.defaultBlockState();
    }

    /** Cierra (o abre) el paso del muro hacia la dungeon: un muro de ladrillo del Nether de 2 bloques que cierra el túnel. */
    public static void sealGateway(ServerLevel level, boolean sealed) {
        for (int x = -2; x <= 2; x++) {
            for (int z = RIM_IN; z <= RIM_IN + 1; z++) {
                for (int y = Y + 1; y <= Y + 5; y++) {
                    BlockPos p = new BlockPos(x, y, z);
                    BlockState st = level.getBlockState(p);
                    if (sealed && st.isAir()) {
                        level.setBlock(p, sealBlock(), 2);
                    } else if (!sealed && st.is(Blocks.NETHER_BRICKS)) {
                        level.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
                    }
                }
            }
        }
    }

    /** Abre el paso hacia la dungeon (también en arenas ya construidas). */
    public static void openGateway(ServerLevel level) {
        sealGateway(level, false);
    }

    /** ¿Hay alguien dentro del túnel del muro? */
    public static boolean tunnelBusy(ServerLevel level) {
        return !level.getEntitiesOfClass(net.minecraft.world.entity.player.Player.class,
                new net.minecraft.world.phys.AABB(-3, Y, RIM_IN - 2, 4, Y + 8, 38)).isEmpty();
    }

    /** Dónde aparece el jefe y dónde llegan los jugadores. */
    public static Vec3 bossSpot() {
        return new Vec3(0.5, Y + 1, 0.5);
    }

    public static Vec3 arrival(int i) {
        return new Vec3(0.5 + (i % 2 == 0 ? -1.5 : 1.5), Y + 1, R - 5.5);
    }
}
