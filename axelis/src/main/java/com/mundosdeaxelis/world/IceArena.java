package com.mundosdeaxelis.world;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * La sala del Lobo Ártico: un gran círculo nevado (radio 26) rodeado de un muro de hielo con ventanas de luz y anillos de hielo
 * en el suelo (el centro queda despejado, sin columnas). Por el sur sale un pasillo cubierto hasta una antesala redonda donde
 * aparecen los jugadores; la salida a la arena se sella cuando empieza la pelea. Se construye una sola vez, poco a poco.
 *
 * <p>El suelo es de bloques sólidos y NO resbalosos (nieve y hielo compacto): la Luna de escarcha del lobo es la que los
 * cambia por hielo azul. El centro de la arena es (0, Y, 0).
 */
public final class IceArena {
    public static final int Y = 64;
    public static final int R = 26;
    /** Centro y radio interior de la antesala (al sur). */
    public static final int AZ = 58;
    public static final int AR = 10;
    private static final int ROWS_PER_TICK = 5;
    /** Marca «ya construida» (versión 3): un bloque escondido bajo el suelo. Si cambia la forma de la sala se cambia esta marca y se reconstruye. */
    private static final BlockPos MARKER = new BlockPos(0, Y - 2, 0);

    private IceArena() {
    }

    public static boolean isBuilt(ServerLevel level) {
        return level.getBlockState(MARKER).is(Blocks.GILDED_BLACKSTONE);
    }

    /** Centro de la arena (a pie de suelo). */
    public static Vec3 center() {
        return new Vec3(0.5, Y + 1.0, 0.5);
    }

    /** Dónde aparece el lobo: al fondo norte, lejos de la puerta. */
    public static Vec3 bossSpot() {
        return new Vec3(0.5, Y + 1.0, -13.5);
    }

    /** Sitio en la antesala para el jugador número i (mirando al norte, hacia la arena). */
    public static Vec3 entryPoint(int i) {
        double dx = ((i % 4) - 1.5) * 1.6;
        double dz = (i / 4) * 1.6;
        return new Vec3(0.5 + dx, Y + 1.0, AZ - 3.5 + dz);
    }

    /** ¿Está este punto dentro de la arena (no en el pasillo ni en la antesala)? */
    public static boolean inArena(double x, double z, double margin) {
        return Math.hypot(x, z) <= R - margin;
    }

    /** Construye la sala (si no existe) y avisa al terminar. */
    public static void ensureBuilt(ServerLevel level, Runnable done) {
        if (isBuilt(level)) {
            done.run();
            return;
        }
        buildRows(level, -R - 6, done);
    }

    /** Vuelve a construir la sala entera (borra hielo derretido, charcos de calor y cualquier resto de la pelea anterior). */
    public static void rebuild(ServerLevel level, Runnable done) {
        buildRows(level, -R - 6, done);
    }

    private static long hash(int x, int y, int z) {
        long h = x * 374761393L + y * 1274126177L + z * 668265263L;
        h = (h ^ (h >> 13)) * 1274126177L;
        return h ^ (h >> 16);
    }

    private static void buildRows(ServerLevel level, int fromX, Runnable done) {
        int maxX = R + 6;
        int toX = Math.min(maxX, fromX + ROWS_PER_TICK - 1);
        for (int x = fromX; x <= toX; x++) {
            for (int z = -R - 6; z <= AZ + AR + 4; z++) {
                for (int y = Y - 4; y <= Y + 26; y++) {
                    // lo que no es bloque se pone como aire: así una reconstrucción borra lo viejo (agua incluida)
                    BlockState st = at(x, y, z);
                    level.setBlock(new BlockPos(x, y, z), st != null ? st : Blocks.AIR.defaultBlockState(), 2);
                }
            }
        }
        if (toX >= maxX) {
            Scheduler.runLater(1, () -> {
                decorate(level);
                level.setBlock(MARKER, Blocks.GILDED_BLACKSTONE.defaultBlockState(), 2);
                done.run();
            });
        } else {
            Scheduler.runLater(1, () -> buildRows(level, toX + 1, done));
        }
    }

    // ------------------------------------------------------------------ forma

    private static boolean gate(int x, int dy, int z) {
        return Math.abs(x) <= 3 && z >= R - 1 && z <= AZ && dy >= 1 && dy <= 6;
    }

    /** Bloque que va en (x,y,z), o null si es aire. */
    private static BlockState at(int x, int y, int z) {
        int dy = y - Y;
        double d = Math.hypot(x, z);
        double da = Math.hypot(x, z - AZ);
        // aire: interior de la arena, del pasillo y de la antesala
        if (dy >= 1 && d <= R + 0.5) {
            return null;
        }
        if (gate(x, dy, z)) {
            return null;
        }
        if (dy >= 1 && dy <= 9 && da <= AR) {
            return null;
        }
        // ---- suelo
        boolean corridor = Math.abs(x) <= 5 && z > R - 2 && z <= AZ;
        boolean hall = da <= AR + 2.5;
        if (dy <= 0 && dy >= -4) {
            // el suelo llega hasta pegarse con el muro (sin rendijas) y el muro tiene su propia base
            if (d <= R + 0.5) {
                return arenaFloor(x, y, z, d, dy);
            }
            if (corridor || hall) {
                if (dy == -4) {
                    return Blocks.DEEPSLATE.defaultBlockState();
                }
                if (dy == 0) {
                    long h = hash(x, y, z);
                    return (h & 3) == 0 ? Blocks.PACKED_ICE.defaultBlockState()
                            : ((x + z) & 1) == 0 ? Blocks.POLISHED_DIORITE.defaultBlockState() : Blocks.QUARTZ_BRICKS.defaultBlockState();
                }
                return Blocks.PACKED_ICE.defaultBlockState();
            }
            if (d <= R + 4.5) {
                return dy == -4 ? Blocks.DEEPSLATE.defaultBlockState() : Blocks.PACKED_ICE.defaultBlockState();
            }
            return null;
        }
        if (dy < 1) {
            return null;
        }
        // ---- muro de hielo alrededor de la arena
        if (d > R + 0.5 && d <= R + 4.5) {
            long h = hash(x, 0, z);
            int top = 14 + (int) ((h >> 3) & 3) + (d > R + 2.5 ? 4 + (int) ((h >> 7) & 3) : 0);
            if (dy <= top) {
                return wallBlock(x, y, z, d, dy, top);
            }
            return null;
        }
        // ---- pasillo cubierto
        if (corridor && z > R + 4 && dy <= 8) {
            return palaceBlock(x, y, z, dy, 8);
        }
        // ---- antesala redonda
        if (da <= AR + 2.5 && dy <= 11) {
            return palaceBlock(x, y, z, dy, 11);
        }
        return null;
    }

    private static BlockState arenaFloor(int x, int y, int z, double d, int dy) {
        if (dy == -4) {
            return Blocks.DEEPSLATE.defaultBlockState();
        }
        if (dy < 0) {
            return Blocks.PACKED_ICE.defaultBlockState();
        }
        long h = hash(x, 0, z);
        int r = (int) (h & 15);
        BlockState b = r < 11 ? Blocks.SNOW_BLOCK.defaultBlockState() : Blocks.PACKED_ICE.defaultBlockState();
        // aros de hielo compacto a 8, 17 y 24 del centro, con luces empotradas
        for (double ring : new double[]{8.0, 17.0, 24.0}) {
            if (Math.abs(d - ring) < 0.6) {
                b = Blocks.PACKED_ICE.defaultBlockState();
                double step = ring < 10 ? Math.PI / 6.0 : Math.PI / 10.0;
                double k = Math.atan2(z, x) / step;
                if (Math.abs(k - Math.round(k)) * step * d < 0.55) {
                    b = Blocks.SEA_LANTERN.defaultBlockState();
                }
            }
        }
        // el corazón de la arena: cuarzo con un aro cincelado
        if (d <= 5.5) {
            b = ((h & 3) == 0) ? Blocks.SMOOTH_QUARTZ.defaultBlockState()
                    : ((h & 3) == 1) ? Blocks.QUARTZ_BRICKS.defaultBlockState() : Blocks.QUARTZ_BLOCK.defaultBlockState();
            if (Math.abs(d - 4.4) < 0.55) {
                b = Blocks.CHISELED_QUARTZ_BLOCK.defaultBlockState();
            }
        }
        return b;
    }

    private static BlockState wallBlock(int x, int y, int z, double d, int dy, int top) {
        long h = hash(x, y, z);
        // ventanas de luz en la cara interior
        if (d < R + 1.5 && dy % 4 == 1 && ((x + z) & 3) == 0 && dy <= 13) {
            return Blocks.SEA_LANTERN.defaultBlockState();
        }
        // OJO: el hielo normal se DERRITE junto a los faroles (se convierte en agua); aquí solo hielo azul y compacto
        if (dy > top - 2 && (h & 3) == 0) {
            return Blocks.BLUE_ICE.defaultBlockState();
        }
        int r = (int) (h & 15);
        return r < 10 ? Blocks.PACKED_ICE.defaultBlockState() : r < 13 ? Blocks.BLUE_ICE.defaultBlockState() : Blocks.SNOW_BLOCK.defaultBlockState();
    }

    private static BlockState palaceBlock(int x, int y, int z, int dy, int roofTop) {
        long h = hash(x, y, z);
        if (dy == roofTop - 1 && ((x & 3) == 0) && ((z & 3) == 0)) {
            return Blocks.SEA_LANTERN.defaultBlockState();
        }
        if (dy % 4 == 0 && (h & 1) == 0) {
            return Blocks.CHISELED_QUARTZ_BLOCK.defaultBlockState();
        }
        int r = (int) (h & 15);
        return r < 8 ? Blocks.QUARTZ_BRICKS.defaultBlockState() : r < 12 ? Blocks.PACKED_ICE.defaultBlockState() : Blocks.POLISHED_DIORITE.defaultBlockState();
    }

    // ------------------------------------------------------------------ adornos

    private static void set(ServerLevel level, int x, int dy, int z, BlockState st) {
        level.setBlock(new BlockPos(x, Y + dy, z), st, 2);
    }

    private static void decorate(ServerLevel level) {
        // candelabros de cristal junto a la puerta de la antesala y en el pasillo
        for (int zz = R + 6; zz < AZ - AR; zz += 6) {
            for (int sx : new int[]{-3, 3}) {
                set(level, sx, 1, zz, Blocks.QUARTZ_PILLAR.defaultBlockState());
                set(level, sx, 2, zz, Blocks.SEA_LANTERN.defaultBlockState());
            }
        }
        // en la antesala: aro de cuarzo cincelado en el suelo y cuatro faroles
        for (int dx = -AR; dx <= AR; dx++) {
            for (int dz = -AR; dz <= AR; dz++) {
                double da = Math.hypot(dx, dz);
                if (Math.abs(da - 6.0) < 0.55) {
                    level.setBlock(new BlockPos(dx, Y, AZ + dz), Blocks.CHISELED_QUARTZ_BLOCK.defaultBlockState(), 2);
                }
            }
        }
        for (int i = 0; i < 4; i++) {
            double a = Math.PI / 4 + i * Math.PI / 2;
            int px = (int) Math.round(Math.cos(a) * 8.0);
            int pz = AZ + (int) Math.round(Math.sin(a) * 8.0);
            set(level, px, 1, pz, Blocks.QUARTZ_PILLAR.defaultBlockState());
            set(level, px, 2, pz, Blocks.QUARTZ_PILLAR.defaultBlockState());
            set(level, px, 3, pz, Blocks.SEA_LANTERN.defaultBlockState());
        }
        sealGateway(level, false);
    }

    // ------------------------------------------------------------------ puerta de la arena

    /** Cierra (hielo) o abre la salida de la arena hacia el pasillo. */
    public static void sealGateway(ServerLevel level, boolean closed) {
        BlockState ice = Blocks.PACKED_ICE.defaultBlockState();
        BlockState blue = Blocks.BLUE_ICE.defaultBlockState();
        for (int x = -3; x <= 3; x++) {
            for (int dy = 1; dy <= 6; dy++) {
                for (int z = R + 1; z <= R + 2; z++) {
                    BlockState st = !closed ? Blocks.AIR.defaultBlockState()
                            : ((x + dy + z) % 3 == 0) ? blue : ice;
                    level.setBlock(new BlockPos(x, Y + dy, z), st, 2);
                }
            }
        }
        if (closed) {
            level.playSound(null, 0.5, Y + 3.0, R + 1.5, net.minecraft.sounds.SoundEvents.GLASS_BREAK, net.minecraft.sounds.SoundSource.BLOCKS, 2.0F, 0.5F);
        }
    }
}
