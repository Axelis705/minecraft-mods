package com.mundosdeaxelis.world;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * El corazón del volcán (jefe de la etapa 4, el Corazón de Magma): una gran caverna en forma de cúpula (radio 30) sobre un mar
 * de lava. En el centro, una arena redonda de piedra negra (radio 21) con tres anillos: el centro (nunca se cae), el anillo medio
 * y el de fuera, cortados en 8 trozos que se pueden derrumbar a la lava y luego vuelven. Cuatro pedestales de obsidiana para las
 * anclas de las cadenas, seis respiraderos de gas y, arriba, el techo con la grieta por donde baja el corazón. Por el sur, un
 * puente sobre la lava lleva a la antesala (una cueva redonda), donde aparecen los jugadores; un muro de obsidiana sella el
 * túnel cuando empieza la pelea. Se construye una sola vez, poco a poco.
 */
public final class FireArena {
    public static final int Y = 64;
    public static final int R = 21;
    /** Radio interior de la cúpula y su centro (por debajo del suelo). */
    public static final int DOME = 30, DOME_DY = -6;
    /** Antesala al sur. */
    public static final int AZ = 50, AR = 9;
    /** Anillos del suelo que se pueden caer: del medio (6,5 a 13,5) y de fuera (13,5 a R). */
    public static final double RING_IN = 6.5, RING_MID = 13.5;
    public static final int SECTORS = 8;
    private static final int ROWS_PER_TICK = 3;
    private static final int MIN_X = -DOME - 4, MAX_X = DOME + 4, MIN_Z = -DOME - 4, MAX_Z = AZ + AR + 4;
    private static final int LOW = -7, HIGH = DOME + DOME_DY + 4;
    /** Marca «ya construida» (versión 1): bajo el suelo, en el centro. */
    private static final BlockPos MARKER = new BlockPos(0, Y - 6, 0);

    private FireArena() {
    }

    public static boolean isBuilt(ServerLevel level) {
        return level.getBlockState(MARKER).is(Blocks.CRYING_OBSIDIAN);
    }

    /** Centro de la arena (a pie de suelo). */
    public static Vec3 center() {
        return new Vec3(0.5, Y + 1.0, 0.5);
    }

    /** Donde flota el corazón mientras está encadenado. */
    public static Vec3 heartHome() {
        return new Vec3(0.5, Y + 8.5, 0.5);
    }

    /** Donde espera escondido, encima del techo, antes de la intro. */
    public static Vec3 heartHidden() {
        return new Vec3(0.5, Y + DOME + DOME_DY + 5.0, 0.5);
    }

    /** Las 4 anclas (sobre sus pedestales), en diagonal a 15,5 bloques del centro. */
    public static Vec3 anchor(int i) {
        double a = Math.PI / 4 + i * Math.PI / 2;
        return new Vec3(0.5 + Math.round(Math.cos(a) * 11.0), Y + 1.0, 0.5 + Math.round(Math.sin(a) * 11.0));
    }

    /** Los 6 respiraderos de gas (a 9,5 bloques del centro). */
    public static Vec3 vent(int i) {
        double a = Math.PI / 6 + i * Math.PI / 3;
        return new Vec3(0.5 + Math.round(Math.cos(a) * 9.5), Y + 1.0, 0.5 + Math.round(Math.sin(a) * 9.5));
    }

    public static Vec3 entryPoint(int i) {
        double dx = ((i % 4) - 1.5) * 1.6;
        double dz = (i / 4) * 1.6;
        return new Vec3(0.5 + dx, Y + 1.0, AZ + 3.5 + dz);
    }

    /** ¿Está este punto dentro de la arena (no en el puente ni en la antesala)? */
    public static boolean inArena(double x, double z, double margin) {
        return Math.hypot(x - 0.5, z - 0.5) <= R - margin;
    }

    /** ¿Ha caído al mar de lava (bajo el suelo de la arena o del puente)? */
    public static boolean fellIn(double x, double y, double z) {
        return y < Y - 0.6 && Math.hypot(x - 0.5, z - 0.5) < DOME;
    }

    public static void ensureBuilt(ServerLevel level, Runnable done) {
        if (isBuilt(level)) {
            done.run();
            return;
        }
        buildRows(level, MIN_X, done);
    }

    public static void rebuild(ServerLevel level, Runnable done) {
        buildRows(level, MIN_X, done);
    }

    private static long hash(int x, int y, int z) {
        long h = x * 374761393L + y * 1274126177L + z * 668265263L;
        h = (h ^ (h >> 13)) * 1274126177L;
        return h ^ (h >> 16);
    }

    private static void buildRows(ServerLevel level, int fromX, Runnable done) {
        int toX = Math.min(MAX_X, fromX + ROWS_PER_TICK - 1);
        BlockState air = Blocks.AIR.defaultBlockState();
        for (int x = fromX; x <= toX; x++) {
            for (int z = MIN_Z; z <= MAX_Z; z++) {
                for (int dy = LOW; dy <= HIGH; dy++) {
                    BlockState st = at(x, dy, z);
                    BlockPos p = new BlockPos(x, Y + dy, z);
                    if (st != null) {
                        level.setBlock(p, st, 2);
                    } else if (!level.getBlockState(p).isAir()) {
                        level.setBlock(p, air, 2);
                    }
                }
            }
        }
        if (toX >= MAX_X) {
            Scheduler.runLater(1, () -> {
                level.setBlock(MARKER, Blocks.CRYING_OBSIDIAN.defaultBlockState(), 2);
                done.run();
            });
        } else {
            Scheduler.runLater(1, () -> buildRows(level, toX + 1, done));
        }
    }

    // ------------------------------------------------------------------ forma

    private static double ang(double x, double z) {
        double a = Math.atan2(z, x);
        return a < 0 ? a + Math.PI * 2.0 : a;
    }

    /** Trozo (0..7) al que pertenece un punto del suelo. */
    public static int sectorOf(double x, double z) {
        return (int) (ang(x, z) / (Math.PI * 2.0 / SECTORS)) % SECTORS;
    }

    /** Anillo del suelo: 0 = centro, 1 = medio, 2 = fuera, -1 = no es suelo de la arena. */
    public static int ringOf(double x, double z) {
        double d = Math.hypot(x, z);
        if (d > R + 0.5) {
            return -1;
        }
        return d <= RING_IN ? 0 : d <= RING_MID ? 1 : 2;
    }

    /** ¿Es parte de un pedestal de ancla (no se cae nunca)? */
    private static boolean plinth(int x, int z) {
        for (int i = 0; i < 4; i++) {
            Vec3 a = anchor(i);
            if (Math.abs(x - Math.floor(a.x)) <= 1 && Math.abs(z - Math.floor(a.z)) <= 1) {
                return true;
            }
        }
        return false;
    }

    private static boolean ventAt(int x, int z, int r) {
        for (int i = 0; i < 6; i++) {
            Vec3 v = vent(i);
            if (Math.abs(x - Math.floor(v.x)) <= r && Math.abs(z - Math.floor(v.z)) <= r) {
                return true;
            }
        }
        return false;
    }

    private static boolean tunnel(int x, int dy, int z) {
        return Math.abs(x) <= 2 && z > R && z < AZ - AR + 2 && dy >= 1 && dy <= 5;
    }

    /** El muro que sella el túnel durante la pelea (dentro de la pared de la cúpula). */
    private static boolean gate(int x, int dy, int z) {
        return Math.abs(x) <= 2 && dy >= 1 && dy <= 5 && z == DOME - 1;
    }

    /** Bloque del suelo de la arena en (x, z) (capa de arriba). */
    private static BlockState floorTop(int x, int z) {
        double d = Math.hypot(x, z);
        long h = hash(x, 0, z);
        if (plinth(x, z)) {
            return (x + z) % 2 == 0 ? Blocks.OBSIDIAN.defaultBlockState() : Blocks.CRYING_OBSIDIAN.defaultBlockState();
        }
        if (ventAt(x, z, 0)) {
            return Blocks.MAGMA_BLOCK.defaultBlockState();
        }
        if (ventAt(x, z, 1)) {
            return Blocks.POLISHED_BASALT.defaultBlockState();
        }
        // bordes de los anillos: oro negro (se ve dónde se puede caer el suelo)
        if (Math.abs(d - RING_IN) < 0.55 || Math.abs(d - RING_MID) < 0.55 || d > R - 0.5) {
            return (h & 3) == 0 ? Blocks.GILDED_BLACKSTONE.defaultBlockState() : Blocks.POLISHED_BLACKSTONE.defaultBlockState();
        }
        // centro: dibujo de rayos que salen del corazón
        if (d <= RING_IN) {
            int sp = (int) Math.round(ang(x, z) / (Math.PI * 2.0 / 12));
            double k = ang(x, z) / (Math.PI * 2.0 / 12) - sp;
            if (Math.abs(k) * d < 0.6 && d > 1.5) {
                return Blocks.SHROOMLIGHT.defaultBlockState();
            }
            return d < 2.2 ? Blocks.CRYING_OBSIDIAN.defaultBlockState() : Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState();
        }
        // líneas entre trozos (se nota por dónde se parte)
        double sec = ang(x, z) / (Math.PI * 2.0 / SECTORS);
        if (Math.abs(sec - Math.round(sec)) * d * (Math.PI * 2.0 / SECTORS) < 0.5) {
            return Blocks.CRACKED_POLISHED_BLACKSTONE_BRICKS.defaultBlockState();
        }
        int r = (int) (h >>> 8 & 15);
        return r < 3 ? Blocks.CRACKED_POLISHED_BLACKSTONE_BRICKS.defaultBlockState()
                : r < 5 ? Blocks.BLACKSTONE.defaultBlockState()
                : r < 6 ? Blocks.SMOOTH_BASALT.defaultBlockState() : Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState();
    }

    /** Bloque en (x, Y+dy, z), o null si es aire. */
    static BlockState at(int x, int dy, int z) {
        double d = Math.hypot(x, z);
        double ds = Math.sqrt(x * x + z * z + (double) (dy - DOME_DY) * (dy - DOME_DY));
        double da = Math.hypot(x, z - AZ);
        long h = hash(x, dy, z);
        // ---- antesala: cueva redonda al sur
        if (da <= AR + 3 && dy >= -2 && dy <= 9) {
            double dsa = Math.sqrt(x * x + (z - AZ) * (z - AZ) + (double) dy * dy * 1.4);
            if (dy <= 0 && da <= AR + 3) {
                if (dy == 0) {
                    if (da <= 2.5) {
                        return Blocks.CRYING_OBSIDIAN.defaultBlockState();
                    }
                    return (h & 7) == 0 ? Blocks.SHROOMLIGHT.defaultBlockState() : Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState();
                }
                return Blocks.BASALT.defaultBlockState();
            }
            if (dsa >= AR && dsa <= AR + 2.5 && !tunnel(x, dy, z)) {
                if ((h & 15) == 0) {
                    return Blocks.MAGMA_BLOCK.defaultBlockState();
                }
                return (h & 3) == 0 ? Blocks.BLACKSTONE.defaultBlockState() : Blocks.BASALT.defaultBlockState();
            }
            if (dsa < AR) {
                // braseros en la pared de la antesala
                if (dy == 1 && Math.abs(da - (AR - 1)) < 0.6 && (int) Math.round(ang(x, z - AZ) * 4 / Math.PI) % 2 == 0) {
                    return Blocks.CAMPFIRE.defaultBlockState();
                }
                return null;
            }
        }
        // ---- túnel cerrado entre la cúpula y la antesala (paredes y techo de basalto)
        if (Math.abs(x) <= 4 && z >= DOME + 1 && z < AZ - AR + 2 && dy >= -2 && dy <= 6 && !(Math.abs(x) <= 2 && dy >= 0 && dy <= 5)) {
            if ((h & 15) == 0 && dy > 0 && dy < 6) {
                return Blocks.SHROOMLIGHT.defaultBlockState();
            }
            return (h & 3) == 0 ? Blocks.BLACKSTONE.defaultBlockState() : Blocks.BASALT.defaultBlockState();
        }
        // ---- túnel y puente sobre la lava (sur)
        if (Math.abs(x) <= 3 && z > R - 1 && z < AZ - AR + 2) {
            if (dy == 0 && Math.abs(x) <= 2) {
                return Math.abs(x) == 2 ? Blocks.POLISHED_BLACKSTONE.defaultBlockState() : Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState();
            }
            if (dy == -1 && Math.abs(x) <= 2) {
                return Blocks.BLACKSTONE.defaultBlockState();
            }
            if (dy == 1 && Math.abs(x) == 3 && z < DOME - 2) {
                return Blocks.BLACKSTONE_WALL.defaultBlockState();
            }
            if (gate(x, dy, z)) {
                return null; // abierto hasta que empieza la pelea
            }
            if (tunnel(x, dy, z)) {
                return null;
            }
        }
        // ---- cúpula (pared y techo), con la grieta en el centro del techo
        if (ds >= DOME && ds <= DOME + 3) {
            if (d < 3.6 && dy > 0) {
                // tapón de la grieta del techo: piedra agrietada que se abre en la intro
                return (h & 1) == 0 ? Blocks.CRACKED_POLISHED_BLACKSTONE_BRICKS.defaultBlockState() : Blocks.BLACKSTONE.defaultBlockState();
            }
            if (ds < DOME + 1.0) {
                int r = (int) (h >>> 12 & 31);
                if (r == 0) {
                    return Blocks.SHROOMLIGHT.defaultBlockState();
                }
                if (r < 3) {
                    return Blocks.MAGMA_BLOCK.defaultBlockState();
                }
                if (r < 5) {
                    return Blocks.CRYING_OBSIDIAN.defaultBlockState();
                }
            }
            // vetas de magma que bajan por la pared
            if (dy > 2 && Math.abs(Math.sin(ang(x, z) * 9) ) < 0.05) {
                return Blocks.MAGMA_BLOCK.defaultBlockState();
            }
            return (h & 3) == 0 ? Blocks.BLACKSTONE.defaultBlockState() : Blocks.BASALT.defaultBlockState();
        }
        if (ds > DOME + 3) {
            return null;
        }
        // ---- dentro de la cúpula
        if (dy <= -5) {
            return Blocks.BASALT.defaultBlockState();
        }
        if (dy == -4) {
            return Blocks.LAVA.defaultBlockState();
        }
        if (d <= R + 0.5) {
            if (dy == 0) {
                return floorTop(x, z);
            }
            if (dy == -1) {
                return plinth(x, z) ? Blocks.OBSIDIAN.defaultBlockState() : Blocks.BLACKSTONE.defaultBlockState();
            }
            // columnas de basalto que sostienen el centro (se ven al caer el suelo)
            if (dy < -1 && d <= RING_IN - 0.5) {
                return Blocks.BASALT.defaultBlockState();
            }
            if (dy < -1 && plinth(x, z)) {
                return Blocks.OBSIDIAN.defaultBlockState();
            }
            // borde con murete bajo
            if (dy == 1 && d > R - 0.5 && !(Math.abs(x) <= 2 && z > 0)) {
                return Blocks.POLISHED_BLACKSTONE_WALL.defaultBlockState();
            }
            return null;
        }
        // islotes de basalto en la lava y estalactitas
        if (dy >= -3 && dy <= -1 && (h & 63) == 0 && d > R + 3) {
            return Blocks.BASALT.defaultBlockState();
        }
        double ceil = Math.sqrt(Math.max(0, (double) DOME * DOME - d * d)) + DOME_DY;
        if (dy > ceil - 4 && dy < ceil && (hash(x, 7, z) & 31) == 0 && d > 6) {
            return (dy > ceil - 2) ? Blocks.BASALT.defaultBlockState() : Blocks.BLACKSTONE_WALL.defaultBlockState();
        }
        return null;
    }

    // ------------------------------------------------------------------ durante la pelea

    /** Cierra (o abre) el túnel hacia la antesala con un muro de obsidiana. */
    public static void sealGateway(ServerLevel level, boolean sealed) {
        for (int x = -2; x <= 2; x++) {
            for (int dy = 1; dy <= 5; dy++) {
                BlockPos p = new BlockPos(x, Y + dy, DOME - 1);
                level.setBlock(p, sealed ? ((x + dy) % 2 == 0 ? Blocks.CRYING_OBSIDIAN : Blocks.OBSIDIAN).defaultBlockState()
                        : Blocks.AIR.defaultBlockState(), 2);
            }
        }
        if (sealed) {
            level.playSound(null, 0.5, Y + 3, DOME - 1, SoundEvents.RESPAWN_ANCHOR_SET_SPAWN, SoundSource.BLOCKS, 3.0F, 0.6F);
        }
    }

    /** Abre la grieta del techo (la intro): quita el tapón poco a poco, de fuera hacia dentro no, del centro hacia fuera. */
    public static void openCeiling(ServerLevel level, int step) {
        int top = DOME + DOME_DY + 4;
        double r = Math.min(3.6, 0.8 + step * 0.7);
        for (int x = -4; x <= 4; x++) {
            for (int z = -4; z <= 4; z++) {
                if (Math.hypot(x, z) > r) {
                    continue;
                }
                for (int dy = DOME + DOME_DY - 4; dy <= top; dy++) {
                    BlockPos p = new BlockPos(x, Y + dy, z);
                    BlockState st = level.getBlockState(p);
                    if (!st.isAir()) {
                        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, st), x + 0.5, Y + dy + 0.5, z + 0.5, 6, 0.3, 0.3, 0.3, 0.1);
                        level.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
                    }
                }
            }
        }
        level.sendParticles(ParticleTypes.LAVA, 0.5, Y + DOME + DOME_DY - 2, 0.5, 20, 1.5, 0.5, 1.5, 0.1);
    }

    /** Vuelve a poner el tapón del techo (al reiniciar la pelea). */
    public static void closeCeiling(ServerLevel level) {
        for (int x = -4; x <= 4; x++) {
            for (int z = -4; z <= 4; z++) {
                for (int dy = DOME + DOME_DY - 4; dy <= DOME + DOME_DY + 4; dy++) {
                    BlockState st = at(x, dy, z);
                    BlockPos p = new BlockPos(x, Y + dy, z);
                    level.setBlock(p, st == null ? Blocks.AIR.defaultBlockState() : st, 2);
                }
            }
        }
    }

    /** Los bloques (x, z) de un trozo de un anillo (sin los pedestales de las anclas). */
    public static java.util.List<int[]> sectorCells(int ring, int sector) {
        java.util.List<int[]> out = new java.util.ArrayList<>();
        for (int x = -R; x <= R; x++) {
            for (int z = -R; z <= R; z++) {
                if (ringOf(x, z) == ring && sectorOf(x, z) == sector && !plinth(x, z) && !(Math.abs(x) <= 2 && z > R - 2)) {
                    out.add(new int[]{x, z});
                }
            }
        }
        return out;
    }

    /** Derrumba un trozo del suelo (las dos capas) a la lava. */
    public static void drop(ServerLevel level, java.util.List<int[]> cells) {
        for (int[] c : cells) {
            for (int dy = -1; dy <= 0; dy++) {
                BlockPos p = new BlockPos(c[0], Y + dy, c[1]);
                BlockState st = level.getBlockState(p);
                if (!st.isAir()) {
                    if (dy == 0 && (hash(c[0], 3, c[1]) & 3) == 0) {
                        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, st), c[0] + 0.5, Y + 0.6, c[1] + 0.5, 4, 0.3, 0.2, 0.3, 0.05);
                    }
                    level.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
                }
            }
        }
    }

    /** Vuelve a poner un trozo del suelo. */
    public static void restore(ServerLevel level, java.util.List<int[]> cells) {
        for (int[] c : cells) {
            for (int dy = -1; dy <= 0; dy++) {
                BlockState st = at(c[0], dy, c[1]);
                if (st != null) {
                    level.setBlock(new BlockPos(c[0], Y + dy, c[1]), st, 2);
                }
            }
        }
    }

    /** Pone todo el suelo de la arena como nuevo (al reiniciar). */
    public static void restoreFloor(ServerLevel level) {
        for (int ring = 1; ring <= 2; ring++) {
            for (int s = 0; s < SECTORS; s++) {
                restore(level, sectorCells(ring, s));
            }
        }
    }
}
