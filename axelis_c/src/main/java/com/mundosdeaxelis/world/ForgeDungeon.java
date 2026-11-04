package com.mundosdeaxelis.world;

import com.mundosdeaxelis.block.VaultChestBlock;
import com.mundosdeaxelis.block.VaultChestBlockEntity;
import com.mundosdeaxelis.block.VaultLoot;
import com.mundosdeaxelis.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * La obra de la dungeon del Gran Herrero (etapa 2, Forja), PROCEDURAL: se construye a partir de un {@link DungeonLayout} (semilla + número de
 * jugadores). Cada sala ocupa una celda de 32x32 con interior de 19x19; entre salas vecinas hay pasillos de 11 de largo.
 * Al hacer una dungeon nueva se borra la anterior (se vuelve a generar su plano con la semilla guardada en {@link DungeonSave}).
 * La obra se hace por tareas pequeñas con un presupuesto de bloques por tick para no trabar el servidor.
 * Las reglas (oleadas, pruebas, llaves) están en {@link ForgeManager}.
 */
public final class ForgeDungeon {
    public static final int Y = 64;
    public static final int HALF = DungeonLayout.HALF;
    /** Distancia del centro de la sala a su pared. */
    public static final int WALL = HALF + 1;
    private static final int BUDGET = 9000;

    private static boolean building = false;
    /** En qué copia de dimensión se está construyendo ahora (solo se construye una a la vez). */
    private static net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> buildingIn = null;
    private static int placed = 0;

    private ForgeDungeon() {
    }

    public static boolean isBuilding() {
        return building;
    }

    /** ¿Se está construyendo la dungeon de ESTA copia? (las demás copias siguen su curso mientras tanto) */
    public static boolean isBuilding(net.minecraft.world.level.Level level) {
        return building && level.dimension().equals(buildingIn);
    }

    // ------------------------------------------------------------------ arranque

    /** Borra la dungeon anterior (si hay) y construye la del plano dado. Avisa al terminar. */
    public static void build(ServerLevel level, DungeonLayout lay, Runnable done) {
        building = true;
        buildingIn = level.dimension();
        // las lápidas de quien murió aquí se apartan (sin soltar nada) y vuelven al claro de entrada cuando termina la obra
        List<com.mundosdeaxelis.grave.GraveEvents.Pending> graves = com.mundosdeaxelis.grave.GraveEvents.detach(level);
        DungeonSave save = DungeonSave.get(level);
        ArrayDeque<Runnable> t = new ArrayDeque<>();
        planClear(level, save, t);
        t.add(() -> {
            save.valid = true;
            save.seed = lay.seed;
            save.players = lay.players;
            save.big = lay.big;
            save.setDirty();
        });
        for (DungeonLayout.Cell c : lay.cells) {
            t.add(() -> foundation(level, c));
            t.add(() -> shell(level, c));
            if (c.type == DungeonLayout.Type.PARKOUR) {
                t.add(() -> parkourExtend(level, lay, c));
            }
            t.add(() -> doors(level, c));
            t.add(() -> decorate(level, lay, c));
        }
        for (DungeonLayout.Cell c : lay.cells) {
            for (int d = 1; d <= 2; d++) {
                if (c.door[d]) {
                    final int dd = d;
                    t.add(() -> corridor(level, c, dd));
                }
            }
        }
        t.add(() -> passage(level, lay.ante()));
        // limites de la obra (nueva y vieja) para limpiar al final los objetos que sueltan las plantas y linternas sin apoyo
        final int[] box = {Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE};
        List<DungeonLayout.Cell> allCells = new ArrayList<>(lay.cells);
        if (save.valid) {
            allCells.addAll(DungeonLayout.generate(save.seed, save.players, save.big).cells);
        }
        for (DungeonLayout.Cell c : allCells) {
            box[0] = Math.min(box[0], c.cx() - 32);
            box[1] = Math.min(box[1], c.cz() - 32);
            box[2] = Math.max(box[2], c.cx() + 32);
            box[3] = Math.max(box[3], c.cz() + 32);
        }
        final long startTick = level.getGameTime();
        Runnable purge = () -> DungeonCleanup.purge(level, box[0], Y - 12, box[1], box[2], Y + 30, box[3], startTick);
        run(t, () -> {
            purge.run();
            Scheduler.runLater(40, purge);
            Scheduler.runLater(200, purge);
            com.mundosdeaxelis.grave.GraveEvents.attach(level, graves, new BlockPos(lay.start().cx(), Y + 1, lay.start().cz()));
            building = false;
            done.run();
        });
    }

    private static void run(ArrayDeque<Runnable> tasks, Runnable done) {
        placed = 0;
        int ran = 0;
        while (!tasks.isEmpty() && placed < BUDGET && ran < 40) {
            tasks.poll().run();
            ran++;
        }
        if (tasks.isEmpty()) {
            done.run();
        } else {
            Scheduler.runLater(1, () -> run(tasks, done));
        }
    }

    // ------------------------------------------------------------------ borrado

    private static void planClear(ServerLevel level, DungeonSave save, ArrayDeque<Runnable> t) {
        if (save.valid) {
            DungeonLayout old = DungeonLayout.generate(save.seed, save.players, save.big);
            for (DungeonLayout.Cell c : old.cells) {
                t.add(() -> clearBox(level, c.cx() - 12, Y - 9, c.cz() - (c.type == DungeonLayout.Type.ANTE ? 11 : 12),
                        c.cx() + 12, Y + 18, c.cz() + 12));
                if (c.type == DungeonLayout.Type.PARKOUR && c.entry >= 0) {
                    // la sala de saltos se alarga por el fondo: también se borra esa parte
                    t.add(() -> {
                        BlockPos p0 = loc(c, 10, Y - 9, -12);
                        BlockPos p1 = loc(c, 24, Y + 18, 12);
                        clearBox(level, Math.min(p0.getX(), p1.getX()), Y - 9, Math.min(p0.getZ(), p1.getZ()),
                                Math.max(p0.getX(), p1.getX()), Y + 18, Math.max(p0.getZ(), p1.getZ()));
                    });
                }
                for (int d = 1; d <= 2; d++) {
                    if (c.door[d]) {
                        final int dd = d;
                        t.add(() -> {
                            int dx = DungeonLayout.dx(dd);
                            int dz = DungeonLayout.dz(dd);
                            int lx = Math.abs(dz);
                            int lz = Math.abs(dx);
                            int x0 = c.cx() + dx * WALL + lx * -4;
                            int x1 = c.cx() + dx * (DungeonLayout.PITCH - WALL) + lx * 4;
                            int z0 = c.cz() + dz * WALL + lz * -4;
                            int z1 = c.cz() + dz * (DungeonLayout.PITCH - WALL) + lz * 4;
                            clearBox(level, Math.min(x0, x1), Y - 9, Math.min(z0, z1), Math.max(x0, x1), Y + 12, Math.max(z0, z1));
                        });
                    }
                }
            }
            t.add(() -> clearBox(level, -4, Y - 9, 32, 4, Y + 12, 34));
        }
    }

    private static void clearBox(ServerLevel level, int x0, int y0, int z0, int x1, int y1, int z1) {
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        BlockState air = Blocks.AIR.defaultBlockState();
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                for (int y = y0; y <= y1; y++) {
                    m.set(x, y, z);
                    if (!level.getBlockState(m).isAir()) {
                        level.setBlock(m, air, 2);
                        placed++;
                    }
                }
            }
        }
    }

    // ------------------------------------------------------------------ utilidades

    private static long hash(int x, int y, int z) {
        long h = x * 374761393L + y * 1103515245L + z * 668265263L;
        h = (h ^ (h >> 13)) * 1274126177L;
        return h ^ (h >> 16);
    }

    private static void set(ServerLevel level, int x, int y, int z, BlockState s) {
        level.setBlock(new BlockPos(x, y, z), s, 2);
        placed++;
    }

    private static void fill(ServerLevel level, int x0, int y0, int z0, int x1, int y1, int z1, BlockState s) {
        for (int x = x0; x <= x1; x++) {
            for (int y = y0; y <= y1; y++) {
                for (int z = z0; z <= z1; z++) {
                    set(level, x, y, z, s);
                }
            }
        }
    }

    private static void put(ServerLevel level, DungeonLayout.Cell c, int ox, int y, int oz, BlockState s) {
        set(level, c.cx() + ox, y, c.cz() + oz, s);
    }

    private static boolean isAir(ServerLevel level, int x, int y, int z) {
        return level.getBlockState(new BlockPos(x, y, z)).isAir();
    }

    private static BlockState wall(int x, int y, int z) {
        long h = hash(x, y, z) & 0xFF;
        if (h < 100) {
            return Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState();
        }
        if (h < 170) {
            return Blocks.DEEPSLATE_BRICKS.defaultBlockState();
        }
        if (h < 215) {
            return Blocks.CRACKED_POLISHED_BLACKSTONE_BRICKS.defaultBlockState();
        }
        if (h < 240) {
            return Blocks.BLACKSTONE.defaultBlockState();
        }
        return Blocks.CRACKED_DEEPSLATE_BRICKS.defaultBlockState();
    }

    private static BlockState floor(int x, int z, boolean room) {
        long h = hash(x, 1, z) & 0xFF;
        if (h < 90) {
            return Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState();
        }
        if (h < 150) {
            return Blocks.DEEPSLATE_TILES.defaultBlockState();
        }
        if (h < 200) {
            return room ? Blocks.POLISHED_BASALT.defaultBlockState() : Blocks.SMOOTH_BASALT.defaultBlockState();
        }
        if (h < 230) {
            return Blocks.BLACKSTONE.defaultBlockState();
        }
        return Blocks.COBBLED_DEEPSLATE.defaultBlockState();
    }

    /** Suelo de las salas: igual que {@link #floor}, pero con alguna baldosa agrietada y algún ascua incrustada (brilla, no quema). */
    private static BlockState embersFloor(int x, int z, boolean room) {
        long h = hash(x, 9, z) & 0xFF;
        if (h < 7) {
            return Blocks.SHROOMLIGHT.defaultBlockState();
        }
        if (h < 22) {
            return Blocks.CRACKED_POLISHED_BLACKSTONE_BRICKS.defaultBlockState();
        }
        return floor(x, z, room);
    }

    private static BlockState lantern(boolean hanging, boolean soul) {
        return (soul ? Blocks.SOUL_LANTERN : Blocks.LANTERN).defaultBlockState().setValue(BlockStateProperties.HANGING, hanging);
    }

    public static Direction dirOf(int d) {
        return switch (d & 3) {
            case 0 -> Direction.NORTH;
            case 1 -> Direction.EAST;
            case 2 -> Direction.SOUTH;
            default -> Direction.WEST;
        };
    }

    /** Alto interior de la sala. */
    public static int height(DungeonLayout.Cell c) {
        return switch (c.type) {
            case START, ANTE, RUNES -> 8;
            case ELITE, PARKOUR -> 10;
            case KEY -> 9;
            case CRYPT -> 7;
            case COMBAT -> switch (c.theme) {
                case ROOTS, NURSERY -> 8;
                case SPINES -> 7;
                case POLLEN -> 10;
                case RAIZAL -> 9;
            };
        };
    }

    /** Punto de una sala sin salida en sus coordenadas propias: a = hacia el fondo (lejos de la puerta), b = hacia un lado. */
    public static BlockPos loc(DungeonLayout.Cell c, int a, int y, int b) {
        int f = (c.entry + 2) & 3;
        int fx = DungeonLayout.dx(f);
        int fz = DungeonLayout.dz(f);
        return new BlockPos(c.cx() + a * fx + b * -fz, y, c.cz() + a * fz + b * fx);
    }

    /** Punto del muro de la sala en la dirección d (u = desplazamiento lateral). */
    public static BlockPos wallPos(DungeonLayout.Cell c, int d, int u, int y) {
        int dx = DungeonLayout.dx(d);
        int dz = DungeonLayout.dz(d);
        return new BlockPos(c.cx() + dx * WALL + Math.abs(dz) * u, y, c.cz() + dz * WALL + Math.abs(dx) * u);
    }

    // ------------------------------------------------------------------ estructura

    private static void foundation(ServerLevel level, DungeonLayout.Cell c) {
        boolean start = c.type == DungeonLayout.Type.START;
        int depth = c.type == DungeonLayout.Type.PARKOUR ? 8 : 3;
        for (int ox = -WALL; ox <= WALL; ox++) {
            for (int oz = -WALL; oz <= WALL; oz++) {
                int x = c.cx() + ox;
                int z = c.cz() + oz;
                for (int y = Y - depth; y <= Y - 1; y++) {
                    set(level, x, y, z, y >= Y - 2 && !(c.type == DungeonLayout.Type.PARKOUR) ? Blocks.BLACKSTONE.defaultBlockState() : Blocks.DEEPSLATE.defaultBlockState());
                }
                if (start && Math.abs(ox) < WALL && Math.abs(oz) < WALL) {
                    long h = hash(x, 2, z);
                    BlockState top = Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState();
                    if (h % 17 == 0) {
                        top = Blocks.POLISHED_BASALT.defaultBlockState();
                    } else if (h % 13 == 0) {
                        top = Blocks.BLACKSTONE.defaultBlockState();
                    } else if (h % 29 == 1) {
                        top = Blocks.CRACKED_POLISHED_BLACKSTONE_BRICKS.defaultBlockState();
                    }
                    if (Math.hypot(ox, oz) < 3.5) {
                        top = Blocks.CHISELED_POLISHED_BLACKSTONE.defaultBlockState();
                    }
                    set(level, x, Y, z, top);
                } else {
                    set(level, x, Y, z, embersFloor(x, z, true));
                }
            }
        }
    }

    private static void shell(ServerLevel level, DungeonLayout.Cell c) {
        int h = height(c);
        boolean open = c.type == DungeonLayout.Type.START;
        for (int ox = -WALL; ox <= WALL; ox++) {
            for (int oz = -WALL; oz <= WALL; oz++) {
                int x = c.cx() + ox;
                int z = c.cz() + oz;
                boolean edge = Math.abs(ox) == WALL || Math.abs(oz) == WALL;
                if (edge) {
                    for (int y = Y + 1; y <= Y + h; y++) {
                        set(level, x, y, z, y == Y + h && hash(x, 4, z) % 3 == 0 ? Blocks.CHISELED_POLISHED_BLACKSTONE.defaultBlockState() : wall(x, y, z));
                    }
                }
                if (!open) {
                    set(level, x, Y + h + 1, z, wall(x, Y + h + 1, z));
                    set(level, x, Y + h + 2, z, Blocks.BLACKSTONE.defaultBlockState());
                } else if (edge) {
                    set(level, x, Y + h + 1, z, hash(x, 9, z) % 2 == 0 ? Blocks.BLACKSTONE.defaultBlockState() : Blocks.POLISHED_BLACKSTONE_BRICK_SLAB.defaultBlockState());
                }
            }
        }
    }

    private static void opening(ServerLevel level, DungeonLayout.Cell c, int d) {
        for (int u = -2; u <= 2; u++) {
            for (int y = Y + 1; y <= Y + 4; y++) {
                BlockPos p = wallPos(c, d, u, y);
                set(level, p.getX(), p.getY(), p.getZ(), Blocks.AIR.defaultBlockState());
            }
        }
        for (int y = Y + 1; y <= Y + 5; y++) {
            for (int u : new int[]{-3, 3}) {
                BlockPos p = wallPos(c, d, u, y);
                set(level, p.getX(), p.getY(), p.getZ(), Blocks.CHISELED_POLISHED_BLACKSTONE.defaultBlockState());
            }
        }
        for (int u = -2; u <= 2; u++) {
            BlockPos p = wallPos(c, d, u, Y + 5);
            set(level, p.getX(), p.getY(), p.getZ(), Blocks.CHISELED_POLISHED_BLACKSTONE.defaultBlockState());
        }
    }

    private static void doors(ServerLevel level, DungeonLayout.Cell c) {
        for (int d = 0; d < 4; d++) {
            if (c.door[d]) {
                opening(level, c, d);
            }
        }
    }

    /** Pasillo entre la celda a y su vecina hacia d (este o sur). */
    private static void corridor(ServerLevel level, DungeonLayout.Cell a, int d) {
        int dx = DungeonLayout.dx(d);
        int dz = DungeonLayout.dz(d);
        int lx = Math.abs(dz);
        int lz = Math.abs(dx);
        for (int t = WALL + 1; t <= DungeonLayout.PITCH - WALL - 1; t++) {
            for (int u = -3; u <= 3; u++) {
                int x = a.cx() + dx * t + lx * u;
                int z = a.cz() + dz * t + lz * u;
                for (int y = Y - 3; y <= Y - 1; y++) {
                    set(level, x, y, z, y >= Y - 2 ? Blocks.BLACKSTONE.defaultBlockState() : Blocks.DEEPSLATE.defaultBlockState());
                }
                set(level, x, Y, z, floor(x, z, false));
                if (Math.abs(u) == 3) {
                    for (int y = Y + 1; y <= Y + 4; y++) {
                        set(level, x, y, z, wall(x, y, z));
                    }
                }
                boolean lamp = u == 0 && (t - WALL - 1) % 5 == 2;
                set(level, x, Y + 5, z, lamp ? Blocks.SHROOMLIGHT.defaultBlockState() : wall(x, Y + 5, z));
                set(level, x, Y + 6, z, Blocks.BLACKSTONE.defaultBlockState());
                long h = hash(x, 5, z);
                if (Math.abs(u) <= 2 && h % 17 == 5 && !lamp) {
                    set(level, x, Y + 4, z, Blocks.CHAIN.defaultBlockState());
                }
            }
        }
    }

    /** Paso corto entre la antesala y la arena del jefe. */
    private static void passage(ServerLevel level, DungeonLayout.Cell ante) {
        int cx = ante.cx();
        for (int z = 32; z <= 34; z++) {
            for (int x = cx - 3; x <= cx + 3; x++) {
                for (int y = Y - 3; y <= Y - 1; y++) {
                    set(level, x, y, z, Blocks.DEEPSLATE.defaultBlockState());
                }
                boolean inner = Math.abs(x - cx) <= 2;
                set(level, x, Y, z, inner ? Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState() : wall(x, Y, z));
                for (int y = Y + 1; y <= Y + 4; y++) {
                    if (inner) {
                        set(level, x, y, z, Blocks.AIR.defaultBlockState());
                    } else if (z >= 33) {
                        set(level, x, y, z, wall(x, y, z));
                    }
                }
                if (z >= 33) {
                    set(level, x, Y + 5, z, wall(x, Y + 5, z));
                }
            }
        }
    }

    // ------------------------------------------------------------------ puertas

    /** Cierra (rejas) o abre (aire) la puerta de la sala {@code c} hacia la dirección {@code d}. */
    public static void setGate(ServerLevel level, DungeonLayout.Cell c, int d, boolean closed) {
        BlockState s = closed ? Blocks.IRON_BARS.defaultBlockState() : Blocks.AIR.defaultBlockState();
        for (int u = -2; u <= 2; u++) {
            for (int y = Y + 1; y <= Y + 4; y++) {
                level.setBlock(wallPos(c, d, u, y), s, 2);
            }
        }
    }

    /** Cierra o abre todas las puertas de una sala. */
    public static void lock(ServerLevel level, DungeonLayout.Cell c, boolean closed) {
        for (int d = 0; d < 4; d++) {
            if (c.door[d]) {
                setGate(level, c, d, closed);
            }
        }
    }

    /** Cierra o abre las puertas de la sala MENOS la de entrada (la salida y las que dan a salas laterales). */
    public static void lockExits(ServerLevel level, DungeonLayout lay, DungeonLayout.Cell c, boolean closed) {
        int in = lay.entryDir(c);
        for (int d = 0; d < 4; d++) {
            if (c.door[d] && d != in) {
                setGate(level, c, d, closed);
            }
        }
    }

    /** Cierra o abre solo la puerta de entrada de la sala. */
    public static void lockEntry(ServerLevel level, DungeonLayout lay, DungeonLayout.Cell c, boolean closed) {
        int in = lay.entryDir(c);
        if (in >= 0 && c.door[in]) {
            setGate(level, c, in, closed);
        }
    }

    /** La puerta del jefe (pared norte de la antesala). */
    public static void setBossGate(ServerLevel level, DungeonLayout lay, boolean closed) {
        setGate(level, lay.ante(), 0, closed);
    }

    // ------------------------------------------------------------------ decoración común

    private static void lights(ServerLevel level, DungeonLayout.Cell c, int[] xs, int[] zs) {
        int cy = Y + height(c) + 1;
        for (int ox : xs) {
            for (int oz : zs) {
                put(level, c, ox, cy, oz, Blocks.SHROOMLIGHT.defaultBlockState());
                if (hash(ox, 3, oz + c.id) % 3 == 0) {
                    put(level, c, ox, cy - 1, oz, Blocks.CHAIN.defaultBlockState());
                }
            }
        }
    }

    private static final int[] L3 = {-6, 0, 6};
    private static final int[] L4 = {-6, -2, 2, 6};

    private static void sprinkle(ServerLevel level, DungeonLayout.Cell c) {
        int cy = Y + height(c);
        for (int ox = -HALF; ox <= HALF; ox++) {
            for (int oz = -HALF; oz <= HALF; oz++) {
                int x = c.cx() + ox;
                int z = c.cz() + oz;
                long h = hash(x, 5, z);
                if (h % 23 == 3 && isAir(level, x, Y + 1, z)) {
                    set(level, x, Y + 1, z, (h >> 8) % 2 == 0 ? Blocks.BLACKSTONE_SLAB.defaultBlockState() : Blocks.COBBLED_DEEPSLATE_SLAB.defaultBlockState());
                }
                if (h % 17 == 5) {
                    int len = 1 + (int) ((h >> 6) % 3);
                    for (int i = 0; i < len; i++) {
                        if (isAir(level, x, cy - i, z)) {
                            set(level, x, cy - i, z, Blocks.CHAIN.defaultBlockState());
                        }
                    }
                }
            }
        }
    }

    private static void pillar(ServerLevel level, int x, int z, int h, boolean lantern) {
        for (int y = 1; y <= h; y++) {
            set(level, x, Y + y, z, y == h ? Blocks.CHISELED_POLISHED_BLACKSTONE.defaultBlockState() : wall(x, y, z));
        }
        if (lantern) {
            set(level, x, Y + h + 1, z, lantern(false, false));
        }
    }

    private static void rune(ServerLevel level, int cx, int cz, int r) {
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                double d = Math.hypot(dx, dz);
                if (d > r - 0.5 && d <= r + 0.5) {
                    set(level, cx + dx, Y, cz + dz, Blocks.CHISELED_POLISHED_BLACKSTONE.defaultBlockState());
                } else if (d < 1.6) {
                    set(level, cx + dx, Y, cz + dz, Blocks.GILDED_BLACKSTONE.defaultBlockState());
                }
            }
        }
    }

    private static void chest(ServerLevel level, BlockPos p, boolean sealed, Direction face, VaultLoot.Kind kind) {
        Block b = (sealed ? ModBlocks.VAULT_CHEST_SEALED_FORGE : ModBlocks.VAULT_CHEST_FORGE).get();
        level.setBlock(p, b.defaultBlockState().setValue(VaultChestBlock.FACING, face), 2);
        if (level.getBlockEntity(p) instanceof VaultChestBlockEntity be) {
            be.configure(kind, ForgeManager.STAGE);
        }
        placed++;
    }

    // ------------------------------------------------------------------ decoración por tipo

    private static void decorate(ServerLevel level, DungeonLayout lay, DungeonLayout.Cell c) {
        switch (c.type) {
            case START -> decStart(level, c);
            case ANTE -> decAnte(level, lay, c);
            case ELITE -> decElite(level, c);
            case COMBAT -> decCombat(level, c);
            case KEY -> decKey(level, c);
            case CRYPT -> decCrypt(level, c);
            case PARKOUR -> decParkour(level, lay, c);
            case RUNES -> decRunes(level, c);
        }
        cutCorners(level, c);
        lockedChest(level, lay, c);
    }

    // ------------------------------------------------------------------ esquinas cortadas (para que las salas no sean cuadradas)

    /**
     * Salas largas (rectángulo de 9 × 19): solo algunas salas de combate que están en línea recta (dos puertas enfrentadas, sin
     * ramas), para que las puertas queden siempre en los extremos. 0 = no, 1 = larga hacia norte-sur, 2 = larga hacia este-oeste.
     */
    public static int rectAxis(DungeonLayout.Cell c) {
        if (c.type != DungeonLayout.Type.COMBAT || c.theme == DungeonLayout.Theme.SPINES) {
            return 0;
        }
        boolean ns = c.door[0] && c.door[2] && !c.door[1] && !c.door[3];
        boolean ew = c.door[1] && c.door[3] && !c.door[0] && !c.door[2];
        if (!ns && !ew || RandomSource.create(c.seed ^ 0x5EC7L).nextInt(100) >= 60) {
            return 0;
        }
        return ns ? 1 : 2;
    }

    /**
     * Forma de la sala: 0 = cuadrada, 1 = octógono, 2 = redonda, 3 = rectángulo largo (ver {@link #rectAxis}), 4 = cruz (cuatro brazos
     * de 9 de ancho), 5 = hexágono con las puntas hacia este-oeste, 6 = hexágono con las puntas hacia norte-sur, 7 = ábside de la
     * cripta (ancha en la entrada y en punta al fondo), 8 = foso con muescas en las esquinas (prueba de saltos).
     */
    private static int cutKind(DungeonLayout.Cell c) {
        if (rectAxis(c) != 0) {
            return 3;
        }
        boolean alt = RandomSource.create(c.seed ^ 0x5A9EL).nextBoolean();
        return switch (c.type) {
            case START, ELITE, RUNES -> 2;
            case ANTE -> 4;
            case KEY -> 1;
            case CRYPT -> 7;
            case PARKOUR -> 8;
            case COMBAT -> switch (c.theme) {
                case ROOTS -> alt ? 2 : 5;
                case POLLEN -> 2;
                case NURSERY -> 1;
                case RAIZAL -> alt ? 5 : 6;
                case SPINES -> 4;
            };
        };
    }

    /** Coordenadas propias de una sala sin salida: a = hacia el fondo, b = hacia un lado (las mismas que {@link #loc}). */
    private static int locA(DungeonLayout.Cell c, int ox, int oz) {
        int f = (c.entry + 2) & 3;
        return ox * DungeonLayout.dx(f) + oz * DungeonLayout.dz(f);
    }

    private static int locB(DungeonLayout.Cell c, int ox, int oz) {
        int f = (c.entry + 2) & 3;
        return -ox * DungeonLayout.dz(f) + oz * DungeonLayout.dx(f);
    }

    /** ¿Esta casilla del interior (desplazamiento ox, oz respecto al centro) queda dentro de una esquina rellena? */
    public static boolean solidCorner(DungeonLayout.Cell c, int ox, int oz) {
        int ax = Math.abs(ox);
        int az = Math.abs(oz);
        return switch (cutKind(c)) {
            case 1 -> ax + az >= 15;
            case 2 -> Math.hypot(ox, oz) > 9.6;
            case 3 -> rectAxis(c) == 1 ? ax >= 5 : az >= 5;
            case 4 -> ax >= 5 && az >= 5;
            case 5 -> ax + az * 0.5 > 10.4;
            case 6 -> az + ax * 0.5 > 10.4;
            case 7 -> {
                int a = locA(c, ox, oz);
                int b = Math.abs(locB(c, ox, oz));
                yield b > 8 - Math.max(0, a - 3);
            }
            case 8 -> {
                int a = Math.abs(locA(c, ox, oz));
                int b = Math.abs(locB(c, ox, oz));
                yield (a >= 8 && b >= 5) || (a >= 9 && b >= 4);
            }
            default -> false;
        };
    }

    /** Forma que se dibuja en el minimapa: 1/2 = sala larga, 3 = esquinas cortadas, 4 = cruz, 0 = cuadrada. */
    public static int mapShape(DungeonLayout.Cell c) {
        int r = rectAxis(c);
        if (r != 0) {
            return r;
        }
        return switch (cutKind(c)) {
            case 1, 2, 5, 6, 7 -> 3;
            case 4, 8 -> 4;
            default -> 0;
        };
    }

    private static void cutCorners(ServerLevel level, DungeonLayout.Cell c) {
        if (cutKind(c) == 0) {
            return;
        }
        int h = height(c);
        for (int ox = -HALF; ox <= HALF; ox++) {
            for (int oz = -HALF; oz <= HALF; oz++) {
                if (!solidCorner(c, ox, oz)) {
                    continue;
                }
                int x = c.cx() + ox;
                int z = c.cz() + oz;
                for (int y = Y + 1; y <= Y + h; y++) {
                    long hh = hash(x, y, z);
                    set(level, x, y, z, y == Y + h && hh % 3 == 0 ? Blocks.CHISELED_POLISHED_BLACKSTONE.defaultBlockState()
                            : y == Y + 1 && hh % 5 == 0 ? Blocks.BLACKSTONE.defaultBlockState() : wall(x, y, z));
                }
            }
        }
    }

    /** Cofre con cerradura en una esquina de las salas de combate: cuanto más hondo, más difícil la cerradura y mejor el botín. */
    /** Nivel de la cerradura del cofre de esta sala (1..5), o 0 si la sala no lleva cofre con cerradura. Lo usan la obra y las oleadas. */
    public static int lockLevelFor(DungeonLayout lay, DungeonLayout.Cell c) {
        int lvl;
        switch (c.type) {
            case ELITE -> lvl = 2;
            case KEY -> lvl = 3;
            case COMBAT -> {
                RandomSource rr = RandomSource.create(c.seed ^ 0x10C4EDL);
                if (rr.nextInt(100) >= 65) {
                    return 0;
                }
                int combat = Math.max(1, lay.path.size() - 4);
                lvl = 2 + Math.round((c.order - 2) * 3.0F / Math.max(1, combat - 1));
            }
            default -> {
                return 0;
            }
        }
        return Math.max(1, Math.min(5, lvl));
    }

    /** Cofre con cerradura en una esquina de las salas de combate: cuanto más hondo, más difícil la cerradura y mejor el botín. */
    private static void lockedChest(ServerLevel level, DungeonLayout lay, DungeonLayout.Cell c) {
        int lvl = lockLevelFor(lay, c);
        if (lvl == 0) {
            return;
        }
        RandomSource r = RandomSource.create(c.seed ^ 0x7A11L);
        int sx = r.nextBoolean() ? 1 : -1;
        int sz = r.nextBoolean() ? 1 : -1;
        int axis = rectAxis(c);
        // sala larga: el cofre va pegado a un extremo, dentro del pasillo (3 de lado en vez de 5)
        int off = axis == 1 ? 3 : cutKind(c) == 4 ? 4 : 5;
        BlockPos p = axis == 2 ? new BlockPos(c.cx() + sx * 8, Y + 1, c.cz() + sz * 3) : new BlockPos(c.cx() + sx * off, Y + 1, c.cz() + sz * 8);
        // si cae junto a una puerta o dentro de un bloque, probar la esquina contraria
        if (!level.getBlockState(p).canBeReplaced() || !level.getBlockState(p.above()).isAir()) {
            sx = -sx;
            sz = -sz;
            p = axis == 2 ? new BlockPos(c.cx() + sx * 8, Y + 1, c.cz() + sz * 3) : new BlockPos(c.cx() + sx * off, Y + 1, c.cz() + sz * 8);
        }
        if (solidCorner(c, p.getX() - c.cx(), p.getZ() - c.cz())) {
            return;
        }
        BlockState below = level.getBlockState(p.below());
        if (below.isAir()) {
            return;
        }
        Direction face = axis == 2 ? (sx > 0 ? Direction.WEST : Direction.EAST) : (sz > 0 ? Direction.NORTH : Direction.SOUTH);
        level.setBlock(p, ModBlocks.VAULT_CHEST_LOCKED_FORGE.get().defaultBlockState().setValue(VaultChestBlock.FACING, face), 2);
        if (level.getBlockEntity(p) instanceof VaultChestBlockEntity be) {
            be.configure(VaultLoot.Kind.LOCKED, ForgeManager.STAGE);
            be.configureLock(lvl);
        }
        placed++;
    }

    private static void decStart(ServerLevel level, DungeonLayout.Cell c) {
        RandomSource rnd = RandomSource.create(c.seed);
        for (int sx : new int[]{-1, 1}) {
            for (int sz : new int[]{-1, 1}) {
                brazier(level, c.cx() + sx * 5 - (sx > 0 ? 0 : 1), c.cz() + sz * 5 - (sz > 0 ? 0 : 1), 5 + rnd.nextInt(3));
                put(level, c, sx * 3, Y + 1, sz * 3, Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState());
                put(level, c, sx * 3, Y + 2, sz * 3, Blocks.POLISHED_BLACKSTONE_BRICK_WALL.defaultBlockState());
                put(level, c, sx * 3, Y + 3, sz * 3, lantern(false, false));
            }
        }
    }

    /** Columna de 2x2 con un caldero de lava encima (el «faro» de la entrada). */
    private static void brazier(ServerLevel level, int bx, int bz, int height) {
        for (int dx = 0; dx <= 1; dx++) {
            for (int dz = 0; dz <= 1; dz++) {
                for (int y = 1; y <= height; y++) {
                    set(level, bx + dx, Y + y, bz + dz, y == height ? Blocks.CHISELED_POLISHED_BLACKSTONE.defaultBlockState() : wall(bx + dx, y, bz + dz));
                }
            }
        }
        set(level, bx, Y + height + 1, bz, Blocks.LAVA_CAULDRON.defaultBlockState());
        set(level, bx + 1, Y + height + 1, bz + 1, Blocks.LAVA_CAULDRON.defaultBlockState());
    }

    /** Un yunque de piedra: base ancha, cuello y mesa con los dos cuernos (largo en x o en z). */
    private static void stoneAnvil(ServerLevel level, int cx, int cz, boolean alongX) {
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

    private static void decAnte(ServerLevel level, DungeonLayout lay, DungeonLayout.Cell c) {
        lights(level, c, L3, L3);
        for (int oz = -HALF; oz <= HALF; oz++) {
            for (int ox = -1; ox <= 1; ox++) {
                put(level, c, ox, Y + 1, oz, Blocks.RED_CARPET.defaultBlockState());
            }
        }
        for (int sx : new int[]{-1, 1}) {
            for (int sz : new int[]{-1, 1}) {
                pillar(level, c.cx() + sx * 5, c.cz() + sz * 4, height(c) - 1, false);
            }
        }
        // puerta del jefe en la pared norte, cerrada hasta la cuenta atrás
        opening(level, c, 0);
        setBossGate(level, lay, true);
        // dos estatuas de herrero con el martillo hacia el centro: avisan de lo que viene
        statue(level, c, 7, 3, -1);
        statue(level, c, -7, 3, 1);
        // dos cofres de provisiones, uno por lado, mirando al centro
        chest(level, new BlockPos(c.cx() - 8, Y + 1, c.cz() - 3), false, Direction.EAST, VaultLoot.Kind.ANTE);
        chest(level, new BlockPos(c.cx() + 8, Y + 1, c.cz() - 3), false, Direction.WEST, VaultLoot.Kind.ANTE);
        for (int sx : new int[]{-1, 1}) {
            put(level, c, sx * 8, Y + 1, -4, Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState());
            put(level, c, sx * 8, Y + 2, -4, Blocks.POLISHED_BLACKSTONE_BRICK_WALL.defaultBlockState());
            put(level, c, sx * 8, Y + 3, -4, lantern(false, false));
        }
        sprinkle(level, c);
    }

    /**
     * Estatua de un herrero gigante hecha de bloques (hierro, piedra negra y un corazón de brasa que brilla). {@code sx} = hacia qué
     * lado (±1) sostiene el martillo. Mide 3 de ancho más el martillo y 7 de alto.
     */
    private static void statue(ServerLevel level, DungeonLayout.Cell c, int ox, int oz, int sx) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                put(level, c, ox + dx, Y + 1, oz + dz, Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState());
            }
        }
        for (int y = 2; y <= 3; y++) {
            put(level, c, ox - 1, Y + y, oz, Blocks.IRON_BLOCK.defaultBlockState());
            put(level, c, ox + 1, Y + y, oz, Blocks.IRON_BLOCK.defaultBlockState());
        }
        for (int dx = -1; dx <= 1; dx++) {
            put(level, c, ox + dx, Y + 4, oz, Blocks.IRON_BLOCK.defaultBlockState());
            put(level, c, ox + dx, Y + 5, oz, dx == 0 ? Blocks.SHROOMLIGHT.defaultBlockState() : Blocks.IRON_BLOCK.defaultBlockState());
        }
        put(level, c, ox, Y + 6, oz, Blocks.CHISELED_POLISHED_BLACKSTONE.defaultBlockState());
        put(level, c, ox, Y + 7, oz, Blocks.POLISHED_BLACKSTONE_BRICK_SLAB.defaultBlockState());
        // el martillo: mango de pared y cabeza de hierro
        for (int y = 2; y <= 5; y++) {
            put(level, c, ox + 2 * sx, Y + y, oz, Blocks.POLISHED_BLACKSTONE_BRICK_WALL.defaultBlockState());
        }
        for (int t = 1; t <= 3; t++) {
            put(level, c, ox + t * sx, Y + 6, oz, t == 2 ? Blocks.CHISELED_POLISHED_BLACKSTONE.defaultBlockState() : Blocks.IRON_BLOCK.defaultBlockState());
        }
    }

    private static void decElite(ServerLevel level, DungeonLayout.Cell c) {
        lights(level, c, L3, L3);
        rune(level, c.cx(), c.cz(), 4);
        rune(level, c.cx(), c.cz(), 7);
        for (int i = 0; i < 8; i++) {
            double a = Math.PI * 2 * (i + 0.5) / 8;
            pillar(level, c.cx() + (int) Math.round(Math.cos(a) * 8), c.cz() + (int) Math.round(Math.sin(a) * 8), height(c) - 1, false);
        }
        sprinkle(level, c);
    }

    private static void decCombat(ServerLevel level, DungeonLayout.Cell c) {
        int h = height(c);
        switch (c.theme) {
            case ROOTS -> {
                // Sala de Fundición: runa en el suelo y cuatro columnas con calderos de lava
                lights(level, c, L3, L3);
                rune(level, c.cx(), c.cz(), 5);
                for (int sx : new int[]{-1, 1}) {
                    for (int sz : new int[]{-1, 1}) {
                        pillar(level, c.cx() + sx * 6, c.cz() + sz * 6, h - 1, false);
                        set(level, c.cx() + sx * 6, Y + h, c.cz() + sz * 6, Blocks.LAVA_CAULDRON.defaultBlockState());
                    }
                }
            }
            case SPINES -> {
                // Pasillo de Chispas (en cruz): repisas a los lados de los cuatro brazos, con linternas y montones de carbón
                lights(level, c, L3, L4);
                for (int sx : new int[]{-1, 1}) {
                    for (int t : new int[]{6, 7, 8, 9}) {
                        for (int sz : new int[]{-1, 1}) {
                            put(level, c, sx * 4, Y + 1, sz * t, Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState());
                            put(level, c, sz * t, Y + 1, sx * 4, Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState());
                        }
                    }
                    for (int sz : new int[]{-1, 1}) {
                        for (int[] q : new int[][]{{4 * sx, 7 * sz}, {7 * sz, 4 * sx}}) {
                            put(level, c, q[0], Y + 2, q[1], Blocks.POLISHED_BLACKSTONE_BRICK_WALL.defaultBlockState());
                            put(level, c, q[0], Y + 3, q[1], lantern(false, false));
                        }
                    }
                }
                for (int sx : new int[]{-1, 1}) {
                    for (int sz : new int[]{-1, 1}) {
                        for (int dx = 0; dx <= 1; dx++) {
                            for (int dz = 0; dz <= 1; dz++) {
                                put(level, c, sx * (3 + dx), Y + 1, sz * (3 + dz), Blocks.COAL_BLOCK.defaultBlockState());
                            }
                        }
                    }
                }
            }
            case NURSERY -> {
                // Sala de los Yunques: cuatro yunques de piedra sobre plataformas
                lights(level, c, L3, L3);
                for (int sx : new int[]{-1, 1}) {
                    for (int sz : new int[]{-1, 1}) {
                        stoneAnvil(level, c.cx() + sx * 6, c.cz() + sz * 6, sx != sz);
                        pillar(level, c.cx() + sx * 8, c.cz() + sz * 8, h, false);
                    }
                }
            }
            case POLLEN -> {
                // Horno central: suelo incandescente rodeado de un brocal de ladrillos del Nether
                lights(level, c, L3, L3);
                for (int ox = -3; ox <= 3; ox++) {
                    for (int oz = -3; oz <= 3; oz++) {
                        double d = Math.hypot(ox, oz);
                        if (d < 1.8) {
                            put(level, c, ox, Y, oz, Blocks.SHROOMLIGHT.defaultBlockState());
                        } else if (d < 2.7) {
                            put(level, c, ox, Y, oz, Blocks.RED_NETHER_BRICKS.defaultBlockState());
                        } else if (d < 3.6) {
                            put(level, c, ox, Y, oz, Blocks.CHISELED_NETHER_BRICKS.defaultBlockState());
                        }
                    }
                }
                for (int sx : new int[]{-1, 1}) {
                    for (int sz : new int[]{-1, 1}) {
                        for (int dx = -1; dx <= 1; dx++) {
                            for (int dz = -1; dz <= 1; dz++) {
                                put(level, c, sx * 6 + dx, Y, sz * 6 + dz, Blocks.POLISHED_BASALT.defaultBlockState());
                                if (Math.abs(dx) + Math.abs(dz) == 2) {
                                    put(level, c, sx * 6 + dx, Y + 1, sz * 6 + dz, Blocks.COAL_BLOCK.defaultBlockState());
                                }
                            }
                        }
                        put(level, c, sx * 6, Y + h, sz * 6, lantern(true, false));
                    }
                }
            }
            case RAIZAL -> {
                // Cámara de los Fuelles: columnas de hierro y piedra con cadenas
                lights(level, c, L3, L3);
                for (int sx : new int[]{-1, 1}) {
                    for (int sz : new int[]{-1, 1}) {
                        int px = c.cx() + sx * 5;
                        int pz = c.cz() + sz * 5;
                        for (int y = 1; y <= h; y++) {
                            set(level, px, Y + y, pz, y % 3 == 0 ? Blocks.IRON_BLOCK.defaultBlockState() : Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState());
                        }
                        set(level, px + sx, Y + 1, pz, Blocks.BLACKSTONE_WALL.defaultBlockState());
                        set(level, px, Y + 1, pz + sz, Blocks.BLACKSTONE_WALL.defaultBlockState());
                    }
                }
            }
        }
        sprinkle(level, c);
    }

    private static void decKey(ServerLevel level, DungeonLayout.Cell c) {
        lights(level, c, L3, L3);
        for (int ox = -2; ox <= 2; ox++) {
            for (int oz = -2; oz <= 2; oz++) {
                put(level, c, ox, Y, oz, Math.abs(ox) + Math.abs(oz) <= 1 ? Blocks.POLISHED_BLACKSTONE.defaultBlockState() : Blocks.CHISELED_POLISHED_BLACKSTONE.defaultBlockState());
            }
        }
        put(level, c, 0, Y + 1, 0, Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState());
        put(level, c, 0, Y + 2, 0, Blocks.SOUL_LANTERN.defaultBlockState().setValue(BlockStateProperties.HANGING, false));
        for (int sx : new int[]{-1, 1}) {
            for (int sz : new int[]{-1, 1}) {
                pillar(level, c.cx() + sx * 7, c.cz() + sz * 7, height(c) - 1, false);
            }
        }
        sprinkle(level, c);
    }

    private static void decCrypt(ServerLevel level, DungeonLayout.Cell c) {
        for (int ox = -HALF; ox <= HALF; ox++) {
            for (int oz = -HALF; oz <= HALF; oz++) {
                long h = (hash(c.cx() + ox, 13, c.cz() + oz) & 0xFF) % 7;
                put(level, c, ox, Y, oz, h < 3 ? Blocks.DEEPSLATE_BRICKS.defaultBlockState()
                        : h < 5 ? Blocks.CRACKED_DEEPSLATE_BRICKS.defaultBlockState() : Blocks.POLISHED_DEEPSLATE.defaultBlockState());
            }
        }
        int top = Y + height(c);
        for (int a : new int[]{-5, 0, 5}) {
            for (int b : new int[]{-6, 6}) {
                BlockPos p = loc(c, a, top, b);
                set(level, p.getX(), p.getY(), p.getZ(), lantern(true, true));
            }
        }
        // tumbas a los lados y telarañas
        for (int a = -5; a <= 3; a += 4) {
            for (int b : new int[]{-6, 6}) {
                for (int k = 0; k < 2; k++) {
                    BlockPos p = loc(c, a + k, Y + 1, b);
                    set(level, p.getX(), p.getY(), p.getZ(), Blocks.POLISHED_BLACKSTONE_BRICK_SLAB.defaultBlockState());
                }
            }
        }
        for (int ox = -HALF; ox <= HALF; ox++) {
            for (int oz = -HALF; oz <= HALF; oz++) {
                long h = hash(c.cx() + ox, 14, c.cz() + oz);
                if (h % 29 == 0 && isAir(level, c.cx() + ox, Y + 1, c.cz() + oz)) {
                    put(level, c, ox, Y + 1, oz, Blocks.COBWEB.defaultBlockState());
                }
                if (h % 41 == 1 && isAir(level, c.cx() + ox, Y + 1, c.cz() + oz)) {
                    put(level, c, ox, Y + 1, oz, Blocks.BONE_BLOCK.defaultBlockState());
                }
            }
        }
        // tarima del fondo con el cofre sellado
        for (int a = 6; a <= 8; a++) {
            for (int b = -2; b <= 2; b++) {
                BlockPos p = loc(c, a, Y + 1, b);
                set(level, p.getX(), p.getY(), p.getZ(), Blocks.POLISHED_DEEPSLATE.defaultBlockState());
            }
        }
        BlockPos cp = loc(c, 8, Y + 2, 0);
        chest(level, cp, true, dirOf(c.entry), VaultLoot.Kind.CRYPT);
        for (int b : new int[]{-2, 2}) {
            BlockPos p = loc(c, 8, Y + 2, b);
            set(level, p.getX(), p.getY(), p.getZ(), lantern(false, true));
        }
    }

    /** Material de las almohadillas que se rompen en la prueba de saltos. */
    public static BlockState padBlock() {
        return Blocks.RED_NETHER_BRICKS.defaultBlockState();
    }

    // ------------------------------------------------------------------ prueba de saltos (sala alargada, filas a 3 bloques)

    /** Sala sin sitio para alargarse: si su fondo mira al norte desde la fila 0, ahí está la arena del jefe. */
    public static boolean parkourCramped(DungeonLayout.Cell c) {
        int f = (c.entry + 2) & 3;
        return DungeonLayout.dz(f) < 0 && c.row <= 0;
    }

    /** Filas del puente según los jugadores: de 5 con uno a 7 con seis (4 si la sala no puede alargarse). */
    public static int parkourRows(int players, DungeonLayout.Cell c) {
        if (parkourCramped(c)) {
            return 4;
        }
        int p = DungeonLayout.clampPlayers(players);
        return 5 + Math.round((p - 1) * 2 / 5.0F);
    }

    /** Dónde empieza la plataforma del fondo (a): una fila más allá de la última, a 3 bloques de distancia. */
    public static int parkourEnd(int rows) {
        return -4 + 3 * rows;
    }

    /** Cuánto se alarga la sala por el fondo más allá de su tamaño normal (a = 10 en adelante). */
    public static int parkourExt(int players, DungeonLayout.Cell c) {
        if (parkourCramped(c)) {
            return 0;
        }
        return Math.max(0, parkourEnd(parkourRows(players, c)) + 2 - HALF);
    }

    /** Si el punto (x, z) está dentro de la sala de saltos, contando la parte alargada. */
    public static boolean inParkourRoom(DungeonLayout lay, DungeonLayout.Cell c, double x, double z) {
        int f = (c.entry + 2) & 3;
        int fx = DungeonLayout.dx(f);
        int fz = DungeonLayout.dz(f);
        double rx = x - (c.cx() + 0.5);
        double rz = z - (c.cz() + 0.5);
        double a = rx * fx + rz * fz;
        double b = -rx * fz + rz * fx;
        return Math.abs(b) <= HALF + 0.5 && a >= -HALF - 0.5 && a <= HALF + 0.5 + parkourExt(lay.players, c);
    }

    /** Posición (a) de cada fila: la primera a 3 bloques de la plataforma de entrada y las demás cada 3 (2 de aire entre una y otra). */
    public static int[] parkourRowPos(int rows) {
        int[] out = new int[rows];
        for (int i = 0; i < rows; i++) {
            out[i] = -4 + 3 * i;
        }
        return out;
    }

    /** Qué lado es el falso en cada fila (true = izquierda). Sale de la semilla de la sala, así que obra y reglas coinciden. */
    public static boolean[] parkourFakeLeft(DungeonLayout.Cell c, int rows) {
        Random r = new Random(c.seed ^ 0x5EA11D0L);
        boolean[] out = new boolean[rows];
        for (int i = 0; i < rows; i++) {
            out[i] = r.nextBoolean();
        }
        return out;
    }

    /** Bloques de las losas falsas, cada uno con el número de losa (fila × 2 + lado) para romper solo la que se pisó. */
    public static Map<BlockPos, Integer> parkourFakes(DungeonLayout lay, DungeonLayout.Cell c) {
        Map<BlockPos, Integer> out = new HashMap<>();
        int rows = parkourRows(lay.players, c);
        int[] rowA = parkourRowPos(rows);
        boolean[] left = parkourFakeLeft(c, rows);
        for (int i = 0; i < rows; i++) {
            int side = left[i] ? 0 : 1;
            for (int bb = 0; bb < 3; bb++) {
                int bOff = side == 0 ? -4 + bb : 1 + bb;
                out.put(loc(c, rowA[i], Y, bOff), i * 2 + side);
            }
        }
        return out;
    }

    /** Alarga la sala de saltos por el fondo: quita el muro de siempre y levanta suelo, muros y techo más allá (sin salir de su celda). */
    private static void parkourExtend(ServerLevel level, DungeonLayout lay, DungeonLayout.Cell c) {
        int ext = parkourExt(lay.players, c);
        if (ext <= 0) {
            return;
        }
        int h = height(c);
        int aWall = HALF + ext + 1;
        for (int a = WALL; a <= aWall; a++) {
            for (int b = -WALL; b <= WALL; b++) {
                boolean sideWall = Math.abs(b) == WALL;
                boolean endWall = a == aWall;
                if (a > WALL) {
                    BlockPos g = loc(c, a, Y, b);
                    for (int y = Y - 8; y <= Y - 1; y++) {
                        set(level, g.getX(), y, g.getZ(), Blocks.DEEPSLATE.defaultBlockState());
                    }
                    set(level, g.getX(), Y, g.getZ(), embersFloor(g.getX(), g.getZ(), true));
                }
                for (int y = Y + 1; y <= Y + h; y++) {
                    BlockPos q = loc(c, a, y, b);
                    if (sideWall || endWall) {
                        set(level, q.getX(), q.getY(), q.getZ(), y == Y + h && hash(q.getX(), 4, q.getZ()) % 3 == 0 ? Blocks.CHISELED_POLISHED_BLACKSTONE.defaultBlockState() : wall(q.getX(), y, q.getZ()));
                    } else {
                        set(level, q.getX(), q.getY(), q.getZ(), Blocks.AIR.defaultBlockState());
                    }
                }
                if (a > WALL) {
                    BlockPos r1 = loc(c, a, Y + h + 1, b);
                    set(level, r1.getX(), r1.getY(), r1.getZ(), wall(r1.getX(), r1.getY(), r1.getZ()));
                    set(level, r1.getX(), r1.getY() + 1, r1.getZ(), Blocks.BLACKSTONE.defaultBlockState());
                }
            }
        }
    }

    private static void decParkour(ServerLevel level, DungeonLayout lay, DungeonLayout.Cell c) {
        int top = Y + height(c);
        int rows = parkourRows(lay.players, c);
        int endA = parkourEnd(rows);
        int aMax = HALF + parkourExt(lay.players, c);
        // foso de a = -6 hasta justo antes de la plataforma del fondo
        for (int a = -6; a <= endA - 1; a++) {
            for (int b = -HALF; b <= HALF; b++) {
                for (int y = Y - 5; y <= Y; y++) {
                    BlockPos p = loc(c, a, y, b);
                    set(level, p.getX(), p.getY(), p.getZ(), Blocks.AIR.defaultBlockState());
                }
                BlockPos fl = loc(c, a, Y - 6, b);
                set(level, fl.getX(), fl.getY(), fl.getZ(), hash(fl.getX(), 15, fl.getZ()) % 3 == 0 ? Blocks.BLACKSTONE.defaultBlockState() : Blocks.COBBLED_DEEPSLATE.defaultBlockState());
                BlockPos w = loc(c, a, Y - 5, b);
                set(level, w.getX(), w.getY(), w.getZ(), Blocks.WATER.defaultBlockState());
            }
        }
        // puente de losas: filas de dos losas (izquierda / derecha); en cada fila UNA es falsa y se rompe al pisarla
        int[] rowA = parkourRowPos(rows);
        for (int i = 0; i < rows; i++) {
            for (int side = 0; side < 2; side++) {
                for (int bb = 0; bb < 3; bb++) {
                    int bOff = side == 0 ? -4 + bb : 1 + bb;
                    BlockPos q = loc(c, rowA[i], Y, bOff);
                    set(level, q.getX(), q.getY(), q.getZ(), padBlock());
                }
            }
        }
        // escaleras para volver a subir (pegadas a la plataforma de entrada)
        Direction up = dirOf((c.entry + 2) & 3);
        for (int b : new int[]{-8, 8}) {
            for (int y = Y - 5; y <= Y; y++) {
                BlockPos q = loc(c, -6, y, b);
                set(level, q.getX(), q.getY(), q.getZ(), Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, up));
            }
        }
        // plataforma del fondo: cofre de premio
        for (int a = endA; a <= aMax; a++) {
            for (int b = -3; b <= 3; b++) {
                BlockPos q = loc(c, a, Y, b);
                set(level, q.getX(), q.getY(), q.getZ(), Blocks.CHISELED_POLISHED_BLACKSTONE.defaultBlockState());
            }
        }
        chest(level, loc(c, endA + 1, Y + 1, 0), false, dirOf(c.entry), VaultLoot.Kind.TRIAL);
        for (int b : new int[]{-3, 3}) {
            BlockPos q = loc(c, endA + 1, Y + 1, b);
            set(level, q.getX(), q.getY(), q.getZ(), Blocks.POLISHED_BLACKSTONE_BRICK_WALL.defaultBlockState());
            set(level, q.getX(), q.getY() + 1, q.getZ(), lantern(false, false));
        }
        // luces sobre el foso
        for (int a = -4; a <= endA - 1; a += 4) {
            for (int b : new int[]{-5, 0, 5}) {
                BlockPos q = loc(c, a, top + 1, b);
                set(level, q.getX(), q.getY(), q.getZ(), Blocks.SHROOMLIGHT.defaultBlockState());
            }
        }
    }

    /** Posiciones de los cristales de la prueba de runas (a la altura de los ojos de quien los toca). */
    public static BlockPos[] pedestals(DungeonLayout.Cell c) {
        BlockPos[] out = new BlockPos[4];
        int[] bs = {-6, -2, 2, 6};
        for (int i = 0; i < 4; i++) {
            out[i] = loc(c, 0, Y + 2, bs[i]);
        }
        return out;
    }

    public static BlockState crystalOff() {
        return Blocks.POLISHED_BLACKSTONE.defaultBlockState();
    }

    public static BlockState crystalOn(int i) {
        return switch (i & 3) {
            case 0 -> Blocks.SHROOMLIGHT.defaultBlockState();
            case 1 -> Blocks.OCHRE_FROGLIGHT.defaultBlockState();
            case 2 -> Blocks.SEA_LANTERN.defaultBlockState();
            default -> Blocks.PEARLESCENT_FROGLIGHT.defaultBlockState();
        };
    }

    public static void setCrystal(ServerLevel level, DungeonLayout.Cell c, int i, boolean on) {
        BlockPos p = pedestals(c)[i];
        level.setBlock(p, on ? crystalOn(i) : crystalOff(), 2);
    }

    /** Reja de la cámara del tesoro en la prueba de runas. */
    public static void setRuneGate(ServerLevel level, DungeonLayout.Cell c, boolean closed) {
        BlockState s = closed ? Blocks.IRON_BARS.defaultBlockState() : Blocks.AIR.defaultBlockState();
        for (int b = -2; b <= 2; b++) {
            for (int y = Y + 1; y <= Y + 4; y++) {
                level.setBlock(loc(c, 6, y, b), s, 2);
            }
        }
    }

    private static void decRunes(ServerLevel level, DungeonLayout.Cell c) {
        lights(level, c, L3, L3);
        BlockPos[] ped = pedestals(c);
        for (int i = 0; i < 4; i++) {
            BlockPos p = ped[i];
            set(level, p.getX(), p.getY() - 1, p.getZ(), Blocks.CHISELED_POLISHED_BLACKSTONE.defaultBlockState());
            set(level, p.getX(), p.getY(), p.getZ(), crystalOff());
            // base de runa alrededor
            for (int da = -1; da <= 1; da++) {
                for (int db = -1; db <= 1; db++) {
                    if (da == 0 && db == 0) {
                        continue;
                    }
                    BlockPos q = loc(c, da, Y, new int[]{-6, -2, 2, 6}[i] + db);
                    set(level, q.getX(), q.getY(), q.getZ(), Blocks.POLISHED_BLACKSTONE.defaultBlockState());
                }
            }
        }
        // cámara del tesoro al fondo
        for (int a = 6; a <= 9; a++) {
            for (int y = Y + 1; y <= Y + 4; y++) {
                for (int b : new int[]{-3, 3}) {
                    BlockPos q = loc(c, a, y, b);
                    set(level, q.getX(), q.getY(), q.getZ(), wall(q.getX(), y, q.getZ()));
                }
            }
        }
        for (int a = 6; a <= 9; a++) {
            for (int b = -2; b <= 2; b++) {
                BlockPos q = loc(c, a, Y, b);
                set(level, q.getX(), q.getY(), q.getZ(), Blocks.POLISHED_BLACKSTONE.defaultBlockState());
            }
        }
        chest(level, loc(c, 8, Y + 1, 0), false, dirOf(c.entry), VaultLoot.Kind.TRIAL);
        BlockPos lp = loc(c, 9, Y + 1, 0);
        set(level, lp.getX(), lp.getY(), lp.getZ(), lantern(false, false));
        setRuneGate(level, c, true);
        for (int sx : new int[]{-1, 1}) {
            for (int sz : new int[]{-1, 1}) {
                pillar(level, c.cx() + sx * 6, c.cz() + sz * 6, height(c) - 1, false);
            }
        }
        sprinkle(level, c);
    }

    // ------------------------------------------------------------------ puntos para los mobs

    /** Puntos del suelo (x, y, z) repartidos por el borde de una sala, lejos de las puertas. */
    public static List<double[]> groundPoints(DungeonLayout.Cell c) {
        List<double[]> out = new ArrayList<>();
        int[] side = {-6, -3, 3, 6};
        for (int s : side) {
            out.add(new double[]{c.cx() + 8.5, Y + 1, c.cz() + s + 0.5});
            out.add(new double[]{c.cx() - 7.5, Y + 1, c.cz() + s + 0.5});
            out.add(new double[]{c.cx() + s + 0.5, Y + 1, c.cz() + 8.5});
            out.add(new double[]{c.cx() + s + 0.5, Y + 1, c.cz() - 7.5});
        }
        out.add(new double[]{c.cx() + 4.5, Y + 1, c.cz() + 4.5});
        out.add(new double[]{c.cx() - 3.5, Y + 1, c.cz() + 4.5});
        out.add(new double[]{c.cx() + 4.5, Y + 1, c.cz() - 3.5});
        out.add(new double[]{c.cx() - 3.5, Y + 1, c.cz() - 3.5});
        int axis = rectAxis(c);
        if (axis != 0) {
            // sala larga: más puntos repartidos a lo largo del pasillo
            for (int s : new int[]{-2, 2}) {
                for (int t : new int[]{-6, -3, 0, 3, 6}) {
                    out.add(axis == 1 ? new double[]{c.cx() + s + 0.5, Y + 1, c.cz() + t + 0.5} : new double[]{c.cx() + t + 0.5, Y + 1, c.cz() + s + 0.5});
                }
            }
        }
        out.removeIf(p -> solidCorner(c, (int) Math.floor(p[0] - c.cx()), (int) Math.floor(p[2] - c.cz())));
        return out;
    }

    /** Puntos elevados para los Espinosos (repisas del Pasillo de Espinas y macetas del Vivero). */
    public static List<double[]> perchPoints(DungeonLayout.Cell c) {
        List<double[]> out = new ArrayList<>();
        if (c.theme == DungeonLayout.Theme.SPINES) {
            // repisas a los lados de los cuatro brazos de la cruz
            for (int sx : new int[]{-1, 1}) {
                for (int t : new int[]{-8, -6, 6, 8}) {
                    out.add(new double[]{c.cx() + sx * 4 + (sx > 0 ? 0.5 : 0.5), Y + 2, c.cz() + t + 0.5});
                    out.add(new double[]{c.cx() + t + 0.5, Y + 2, c.cz() + sx * 4 + 0.5});
                }
            }
        } else {
            for (int sx : new int[]{-1, 1}) {
                for (int sz : new int[]{-1, 1}) {
                    out.add(new double[]{c.cx() + sx * 6 + 0.5, c.theme == DungeonLayout.Theme.NURSERY ? Y + 2 : Y + 3, c.cz() + sz * 6 + 0.5});
                }
            }
        }
        out.removeIf(p -> solidCorner(c, (int) Math.floor(p[0] - c.cx()), (int) Math.floor(p[2] - c.cz())));
        return out;
    }
}
