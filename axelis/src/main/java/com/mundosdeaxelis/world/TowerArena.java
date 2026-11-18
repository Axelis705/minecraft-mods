package com.mundosdeaxelis.world;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.AmethystClusterBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.LecternBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.phys.Vec3;

/**
 * La cima de la Torre del Archimago (jefe de la etapa 4): un gran disco de piedra oscura (radio 21) que flota entre las estrellas,
 * con aros rúnicos de amatista en el suelo (a 6, 11 y 16 bloques del centro), ocho columnas con cristales en el borde y un pretil
 * con una barrera invisible para que nadie caiga al vacío. Debajo, el cuerpo de la torre se afila hasta una punta. Por el sur, un
 * puente lleva a la antesala: una biblioteca redonda con estanterías, atriles, velas y un ojo abierto al cielo, donde aparecen los
 * jugadores. La salida de la arena se sella con un vitral mágico cuando empieza la pelea. Se construye una sola vez, poco a poco.
 *
 * <p>El centro de la arena es (0, Y, 0); el Archimago la usa como su «casa» (radio útil 17).
 */
public final class TowerArena {
    public static final int Y = 100;
    public static final int R = 21;
    /** Centro y radio interior de la biblioteca (al sur). */
    public static final int AZ = 46;
    public static final int AR = 9;
    private static final int ROWS_PER_TICK = 3;
    private static final int MIN_X = -R - 6, MAX_X = R + 6, MIN_Z = -R - 6, MAX_Z = AZ + AR + 4;
    private static final int LOW = -48, HIGH = 16;
    /** Marca «ya construida» (versión 1): un bloque escondido dentro de la torre, bajo el suelo. */
    private static final BlockPos MARKER = new BlockPos(0, Y - 3, 0);

    private TowerArena() {
    }

    public static boolean isBuilt(ServerLevel level) {
        return level.getBlockState(MARKER).is(Blocks.GILDED_BLACKSTONE);
    }

    /** Centro de la arena (a pie de suelo). */
    public static Vec3 center() {
        return new Vec3(0.5, Y + 1.0, 0.5);
    }

    /** Dónde aparece el Archimago: al fondo norte, frente a la puerta. */
    public static Vec3 bossSpot() {
        return new Vec3(0.5, Y + 1.0, -9.5);
    }

    /** Sitio en la biblioteca para el jugador número i (mirando al norte, hacia el puente). */
    public static Vec3 entryPoint(int i) {
        double dx = ((i % 4) - 1.5) * 1.6;
        double dz = (i / 4) * 1.6;
        return new Vec3(0.5 + dx, Y + 1.0, AZ + 3.5 + dz);
    }

    /** ¿Está este punto dentro de la arena (no en el puente ni en la biblioteca)? */
    public static boolean inArena(double x, double z, double margin) {
        return Math.hypot(x, z) <= R - margin;
    }

    /** Construye la cima de la torre (si no existe) y avisa al terminar. */
    public static void ensureBuilt(ServerLevel level, Runnable done) {
        if (isBuilt(level)) {
            done.run();
            return;
        }
        buildRows(level, MIN_X, done);
    }

    /** Vuelve a construirla entera (por si quedó algo de una pelea anterior). */
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
                    if (st != null) {
                        level.setBlock(new BlockPos(x, Y + dy, z), st, 2);
                    } else if (dy >= 1) {
                        // por encima del suelo lo que no es bloque se pone como aire: una reconstrucción borra lo viejo
                        level.setBlock(new BlockPos(x, Y + dy, z), air, 2);
                    }
                }
            }
        }
        if (toX >= MAX_X) {
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
        return Math.abs(x) <= 2 && z >= R + 1 && z <= R + 2 && dy >= 1 && dy <= 6;
    }

    private static boolean bridge(int x, int z) {
        return Math.abs(x) <= 3 && z > R && z <= AZ - AR + 1;
    }

    /** Ángulo (0..2π) del punto respecto a un centro. */
    private static double ang(double x, double z) {
        double a = Math.atan2(z, x);
        return a < 0 ? a + Math.PI * 2.0 : a;
    }

    /** ¿Cae este punto en uno de los n radios (ángulos) con este ancho en bloques? */
    private static boolean onSpoke(double x, double z, int n, double offset, double width) {
        double d = Math.hypot(x, z);
        double step = Math.PI * 2.0 / n;
        double k = (ang(x, z) - offset) / step;
        return Math.abs(k - Math.round(k)) * step * d < width;
    }

    /** Bloque que va en (x, Y+dy, z), o null si es aire. */
    private static BlockState at(int x, int dy, int z) {
        double d = Math.hypot(x, z);
        double da = Math.hypot(x, z - AZ);
        // ---- arena
        if (d <= R + 0.5) {
            if (dy == 0) {
                return arenaFloor(x, z, d);
            }
            if (dy == -1) {
                return Blocks.POLISHED_DEEPSLATE.defaultBlockState();
            }
            if (dy >= 1) {
                return pillar(x, dy, z);
            }
            return towerBody(x, dy, z, d);
        }
        // ---- pretil alrededor de la arena (con barrera invisible encima)
        if (d <= R + 1.5) {
            if (gate(x, dy, z)) {
                return null;
            }
            if (dy == -1 || dy == 0) {
                return Blocks.DEEPSLATE_BRICKS.defaultBlockState();
            }
            if (dy == 1) {
                return Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState();
            }
            if (dy == 2) {
                return Math.abs(x) <= 3 && z > 0 ? null : Blocks.POLISHED_BLACKSTONE_BRICK_SLAB.defaultBlockState();
            }
            if (dy >= 3 && dy <= 8) {
                return Math.abs(x) <= 2 && z > 0 ? null : Blocks.BARRIER.defaultBlockState();
            }
            return dy == -2 ? Blocks.DEEPSLATE_BRICKS.defaultBlockState() : null;
        }
        // ---- cornisa bajo el pretil
        if (d <= R + 2.5 && (dy == -1 || dy == -2)) {
            long h = hash(x, dy, z);
            return (h & 7) == 0 ? Blocks.CHISELED_DEEPSLATE.defaultBlockState() : Blocks.DEEPSLATE_BRICKS.defaultBlockState();
        }
        // ---- puente hacia la biblioteca
        if (bridge(x, z) && da > AR + 0.5) {
            return bridgeBlock(x, dy, z);
        }
        // ---- biblioteca
        if (da <= AR + 1.5) {
            return library(x, dy, z, da);
        }
        // ---- techo cónico de la biblioteca (sobresale un poco)
        if (da <= AR + 2.5 && dy == 9) {
            return Blocks.DEEPSLATE_TILES.defaultBlockState();
        }
        return null;
    }

    private static BlockState arenaFloor(int x, int z, double d) {
        long h = hash(x, 0, z);
        // centro: cristal de lágrima rodeado de piedra negra dorada
        if (d <= 0.5) {
            return Blocks.CRYING_OBSIDIAN.defaultBlockState();
        }
        if (d <= 1.6) {
            return Blocks.CHISELED_POLISHED_BLACKSTONE.defaultBlockState();
        }
        if (Math.abs(d - 2.4) < 0.55) {
            return Blocks.GILDED_BLACKSTONE.defaultBlockState();
        }
        // aros rúnicos de amatista con luces moradas empotradas
        for (double ring : new double[]{6.0, 11.0, 16.0}) {
            if (Math.abs(d - ring) < 0.55) {
                double step = ring < 8 ? Math.PI / 4.0 : ring < 13 ? Math.PI / 6.0 : Math.PI / 8.0;
                double k = ang(x, z) / step;
                if (Math.abs(k - Math.round(k)) * step * d < 0.55) {
                    return Blocks.PEARLESCENT_FROGLIGHT.defaultBlockState();
                }
                return Blocks.AMETHYST_BLOCK.defaultBlockState();
            }
        }
        // borde de ladrillo
        if (d > R - 1.0) {
            return Blocks.DEEPSLATE_BRICKS.defaultBlockState();
        }
        // ocho radios de baldosas que unen los aros
        if (d > 3.0 && onSpoke(x, z, 8, 0.0, 0.6)) {
            return Blocks.DEEPSLATE_TILES.defaultBlockState();
        }
        int r = (int) (h & 15);
        return r < 10 ? Blocks.POLISHED_DEEPSLATE.defaultBlockState() : r < 14 ? Blocks.DEEPSLATE_TILES.defaultBlockState()
                : Blocks.POLISHED_BLACKSTONE.defaultBlockState();
    }

    /** Las ocho columnas del borde (radio 18,5), entre los radios del suelo. */
    private static BlockState pillar(int x, int dy, int z) {
        if (dy > 11) {
            return null;
        }
        for (int i = 0; i < 8; i++) {
            double a = Math.PI / 8.0 + i * Math.PI / 4.0;
            long cx = Math.round(Math.cos(a) * 18.5);
            long cz = Math.round(Math.sin(a) * 18.5);
            long ox = Math.abs(x - cx), oz = Math.abs(z - cz);
            if (ox <= 1 && oz <= 1 && ox + oz <= 1) {
                boolean core = ox == 0 && oz == 0;
                if (dy == 10) {
                    return core ? Blocks.PEARLESCENT_FROGLIGHT.defaultBlockState() : null;
                }
                if (dy == 11) {
                    return core ? Blocks.AMETHYST_CLUSTER.defaultBlockState().setValue(AmethystClusterBlock.FACING, Direction.UP) : null;
                }
                if (!core && dy > 8) {
                    return null;
                }
                if (dy == 1 || dy == 9 || (dy == 5 && core)) {
                    return Blocks.CHISELED_DEEPSLATE.defaultBlockState();
                }
                if (!core && dy == 8) {
                    return Blocks.DEEPSLATE_TILE_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM);
                }
                return core ? Blocks.POLISHED_DEEPSLATE.defaultBlockState() : Blocks.DEEPSLATE_BRICKS.defaultBlockState();
            }
        }
        return null;
    }

    /** El cuerpo de la torre bajo la arena: una pared hueca que se afila hasta una punta, con ventanas de cristal llorón. */
    private static BlockState towerBody(int x, int dy, int z, double d) {
        if (dy > -2) {
            return null;
        }
        double r = (R - 0.5) - Math.max(0, -dy - 10) * 0.55;
        if (r < 0.6) {
            return null;
        }
        if (d > r || d <= r - 2.2) {
            return null;
        }
        // contrafuertes en ocho direcciones
        if (onSpoke(x, z, 8, Math.PI / 8.0, 0.7)) {
            return Blocks.POLISHED_DEEPSLATE.defaultBlockState();
        }
        // ventanas que brillan, en anillos
        if ((-dy) % 9 == 4 && onSpoke(x, z, 12, 0.0, 0.6)) {
            return Blocks.CRYING_OBSIDIAN.defaultBlockState();
        }
        long h = hash(x, dy, z);
        int k = (int) (h & 15);
        if (k == 0) {
            return Blocks.AMETHYST_BLOCK.defaultBlockState();
        }
        return k < 9 ? Blocks.DEEPSLATE_BRICKS.defaultBlockState() : k < 13 ? Blocks.CRACKED_DEEPSLATE_BRICKS.defaultBlockState()
                : Blocks.DEEPSLATE_TILES.defaultBlockState();
    }

    private static BlockState bridgeBlock(int x, int dy, int z) {
        int ax = Math.abs(x);
        if (dy == 0) {
            if (ax <= 2) {
                return ax == 0 ? Blocks.POLISHED_DEEPSLATE.defaultBlockState() : Blocks.DEEPSLATE_TILES.defaultBlockState();
            }
            return Blocks.DEEPSLATE_BRICKS.defaultBlockState();
        }
        if (dy == -1) {
            return Blocks.DEEPSLATE_BRICKS.defaultBlockState();
        }
        // arco por debajo
        if (dy == -2 && ax <= 2) {
            return Blocks.DEEPSLATE_BRICK_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.TOP);
        }
        if (ax == 3) {
            boolean post = z % 5 == 0;
            if (dy == 1) {
                return Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState();
            }
            if (post && dy <= 3) {
                return Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState();
            }
            if (post && dy == 4) {
                return null; // farol (decorate)
            }
            if (dy >= 2 && dy <= 6) {
                return Blocks.BARRIER.defaultBlockState();
            }
        }
        return null;
    }

    private static BlockState library(int x, int dy, int z, double da) {
        long h = hash(x, dy, z);
        // suelo
        if (dy == 0) {
            if (da > AR + 0.5) {
                return Blocks.DEEPSLATE_BRICKS.defaultBlockState();
            }
            if (Math.abs(da - 6.5) < 0.55) {
                return Blocks.POLISHED_DEEPSLATE.defaultBlockState();
            }
            if (da <= 1.6) {
                return Blocks.CHISELED_DEEPSLATE.defaultBlockState();
            }
            return ((x + z) & 1) == 0 ? Blocks.DARK_OAK_PLANKS.defaultBlockState() : Blocks.SPRUCE_PLANKS.defaultBlockState();
        }
        if (dy == -1) {
            return Blocks.DEEPSLATE_BRICKS.defaultBlockState();
        }
        if (dy < -1) {
            // base de la biblioteca: un cono que se afila hacia abajo
            double r = (AR + 1.5) - (-dy - 1) * 0.8;
            if (r > 0.6 && da <= r && da > r - 2.0) {
                return (h & 7) == 0 ? Blocks.AMETHYST_BLOCK.defaultBlockState() : Blocks.DEEPSLATE_BRICKS.defaultBlockState();
            }
            return null;
        }
        // muro de estanterías, con columnas de piedra y la puerta al norte
        if (da > AR + 0.5) {
            boolean door = Math.abs(x) <= 2 && z < AZ && dy <= 5;
            if (door) {
                return null;
            }
            if (Math.abs(x) <= 2 && z < AZ && dy == 6) {
                return Blocks.CHISELED_DEEPSLATE.defaultBlockState(); // dintel de la puerta
            }
            if (dy >= 9) {
                return Blocks.DEEPSLATE_TILES.defaultBlockState();
            }
            if (onSpoke(x, z - AZ, 8, Math.PI / 8.0, 0.8) || dy == 8 || (Math.abs(x) == 3 && z < AZ)) {
                return Blocks.DEEPSLATE_BRICKS.defaultBlockState();
            }
            if (dy == 4 && (h & 3) == 0) {
                return Blocks.CHISELED_BOOKSHELF.defaultBlockState();
            }
            return Blocks.BOOKSHELF.defaultBlockState();
        }
        // techo con un ojo abierto al cielo
        if (dy == 9) {
            if (da <= 2.5) {
                return null;
            }
            if (da <= 3.5) {
                return Blocks.CRYING_OBSIDIAN.defaultBlockState();
            }
            return (h & 3) == 0 ? Blocks.DARK_OAK_PLANKS.defaultBlockState() : Blocks.DEEPSLATE_TILES.defaultBlockState();
        }
        // tejado cónico
        if (dy >= 10 && dy <= 14) {
            double r = (AR + 1.5) - (dy - 9) * 2.0;
            if (da <= r && da > r - 1.6 && da > 2.5) {
                return Blocks.DEEPSLATE_TILES.defaultBlockState();
            }
            return null;
        }
        return null;
    }

    // ------------------------------------------------------------------ adornos

    private static void set(ServerLevel level, int x, int dy, int z, BlockState st) {
        level.setBlock(new BlockPos(x, Y + dy, z), st, 2);
    }

    private static void decorate(ServerLevel level) {
        // faroles del puente
        for (int z = R + 1; z <= AZ - AR + 1; z++) {
            if (z % 5 == 0) {
                for (int sx : new int[]{-3, 3}) {
                    set(level, sx, 4, z, Blocks.SOUL_LANTERN.defaultBlockState());
                }
            }
        }
        // biblioteca: mesa de encantar en el centro, cuatro atriles, velas moradas y faroles colgando del techo
        set(level, 0, 1, AZ, Blocks.ENCHANTING_TABLE.defaultBlockState());
        int[][] lect = {{-4, -4}, {4, -4}, {-4, 4}, {4, 4}};
        for (int[] l : lect) {
            Direction face = l[1] < 0 ? Direction.SOUTH : Direction.NORTH;
            set(level, l[0], 1, AZ + l[1], Blocks.LECTERN.defaultBlockState().setValue(LecternBlock.FACING, face));
        }
        for (int i = 0; i < 10; i++) {
            double a = i * Math.PI / 5.0;
            int cx = (int) Math.round(Math.cos(a) * 7.5);
            int cz = (int) Math.round(Math.sin(a) * 7.5);
            if (Math.abs(cx) <= 2 && cz < 0) {
                continue; // la puerta
            }
            set(level, cx, 1, AZ + cz, Blocks.PURPLE_CANDLE.defaultBlockState().setValue(CandleBlock.CANDLES, 1 + (i % 4))
                    .setValue(CandleBlock.LIT, true));
        }
        for (int i = 0; i < 6; i++) {
            double a = i * Math.PI / 3.0 + Math.PI / 6.0;
            int cx = (int) Math.round(Math.cos(a) * 5.0);
            int cz = (int) Math.round(Math.sin(a) * 5.0);
            set(level, cx, 8, AZ + cz, Blocks.SOUL_LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true));
        }
        // cristales de amatista que flotan alrededor de la torre
        for (int i = 0; i < 10; i++) {
            double a = i * Math.PI / 5.0 + 0.31;
            if (Math.abs(a - Math.PI / 2.0) < 0.35) {
                continue; // por donde va el puente
            }
            int cx = (int) Math.round(Math.cos(a) * (R + 4 + (i % 2)));
            int cz = (int) Math.round(Math.sin(a) * (R + 4 + (i % 2)));
            int cy = -14 + (i * 7) % 18;
            set(level, cx, cy, cz, Blocks.AMETHYST_BLOCK.defaultBlockState());
            set(level, cx + 1, cy, cz, Blocks.BUDDING_AMETHYST.defaultBlockState());
            set(level, cx, cy - 1, cz, Blocks.AMETHYST_BLOCK.defaultBlockState());
            set(level, cx, cy + 1, cz, Blocks.AMETHYST_CLUSTER.defaultBlockState().setValue(AmethystClusterBlock.FACING, Direction.UP));
            set(level, cx, cy - 2, cz, Blocks.AMETHYST_CLUSTER.defaultBlockState().setValue(AmethystClusterBlock.FACING, Direction.DOWN));
        }
        sealGateway(level, false);
    }

    // ------------------------------------------------------------------ puerta de la arena

    /** Cierra (vitral mágico) o abre la salida de la arena hacia el puente. */
    public static void sealGateway(ServerLevel level, boolean closed) {
        for (int x = -2; x <= 2; x++) {
            for (int dy = 1; dy <= 6; dy++) {
                for (int z = R + 1; z <= R + 2; z++) {
                    BlockState st = Blocks.AIR.defaultBlockState();
                    if (closed) {
                        boolean frame = Math.abs(x) == 2 || dy == 6;
                        st = frame ? Blocks.CRYING_OBSIDIAN.defaultBlockState() : Blocks.PURPLE_STAINED_GLASS.defaultBlockState();
                    }
                    level.setBlock(new BlockPos(x, Y + dy, z), st, 2);
                }
            }
        }
        if (closed) {
            level.playSound(null, 0.5, Y + 3.0, R + 1.5, net.minecraft.sounds.SoundEvents.BEACON_ACTIVATE,
                    net.minecraft.sounds.SoundSource.BLOCKS, 2.0F, 0.6F);
        }
    }
}
