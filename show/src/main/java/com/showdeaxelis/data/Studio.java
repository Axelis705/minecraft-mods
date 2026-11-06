package com.showdeaxelis.data;

import com.showdeaxelis.entity.SpectatorEntity;
import com.showdeaxelis.game.Format;
import com.showdeaxelis.game.Participant;
import com.showdeaxelis.game.Show;
import com.showdeaxelis.game.Side;
import java.util.List;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * El estudio de television: se construye de golpe con /show escenario delante del jugador. Las posiciones se cuentan en coordenadas
 * locales (x hacia la derecha, z hacia el fondo del escenario) para que el estudio salga igual mirando a donde se mire.
 * Publico en gradas que suben hacia atras (z 5..22), escenario (z 25..59) con tres podios con escritorio, y la pantalla grande al fondo.
 * Los jugadores y el presentador miran a la pantalla (de espaldas al publico); las camaras los toman de frente desde el lado de la pantalla.
 */
public final class Studio {
    public static final int HALF_W = 24;
    public static final int HEIGHT = 22;
    public static final int Z_MIN = -3;
    /** La pared del fondo, donde va la pantalla. */
    public static final int BACK_Z = 60;
    public static final int STAGE_Z = 25;
    /** Filas de gradas (la 0 es la mas cercana al escenario). */
    public static final int ROWS = 6;
    /** Centro de cada podio en x local. */
    public static final int[] PODIUM_X = {-13, 0, 13};
    public static final int PODIUM_Z = 36;
    /** Altura de la superficie donde pisan los jugadores en su podio. */
    public static final int PODIUM_FLOOR = 3;
    /** Se sube cada vez que se construye el estudio (para que el director sepa que hay que repintar los podios). */
    public static int version;
    private static final Direction[] HORIZ = {Direction.SOUTH, Direction.WEST, Direction.NORTH, Direction.EAST};
    private static final Block[] SIDE_BLOCKS = {Blocks.RED_CONCRETE, Blocks.LIGHT_BLUE_CONCRETE, Blocks.YELLOW_CONCRETE, Blocks.GREEN_CONCRETE,
        Blocks.PURPLE_CONCRETE, Blocks.ORANGE_CONCRETE};

    private Studio() {
    }

    public static Direction dirOf(int idx) {
        return HORIZ[Math.floorMod(idx, 4)];
    }

    public static int idxOf(Direction d) {
        for (int i = 0; i < 4; i++) {
            if (HORIZ[i] == d) {
                return i;
            }
        }
        return 0;
    }

    /** Posicion del mundo de un punto local. {@code y} es altura sobre la fila del suelo. */
    public static BlockPos at(StageData.Info st, int x, int y, int z) {
        Direction f = dirOf(st.dir());
        Direction r = f.getClockWise();
        return new BlockPos(st.x() + r.getStepX() * x + f.getStepX() * z, st.y() + y, st.z() + r.getStepZ() * x + f.getStepZ() * z);
    }

    public static double[] atD(StageData.Info st, double x, double y, double z) {
        Direction f = dirOf(st.dir());
        Direction r = f.getClockWise();
        return new double[] {st.x() + 0.5 + r.getStepX() * x + f.getStepX() * z, st.y() + y, st.z() + 0.5 + r.getStepZ() * x + f.getStepZ() * z};
    }

    private static void put(ServerLevel lv, StageData.Info st, int x, int y, int z, Block b) {
        lv.setBlock(at(st, x, y, z), b.defaultBlockState(), Block.UPDATE_CLIENTS);
    }

    private static void put(ServerLevel lv, StageData.Info st, int x, int y, int z, BlockState s) {
        lv.setBlock(at(st, x, y, z), s, Block.UPDATE_CLIENTS);
    }

    private static void fill(ServerLevel lv, StageData.Info st, int x1, int y1, int z1, int x2, int y2, int z2, Block b) {
        for (int x = Math.min(x1, x2); x <= Math.max(x1, x2); x++) {
            for (int y = Math.min(y1, y2); y <= Math.max(y1, y2); y++) {
                for (int z = Math.min(z1, z2); z <= Math.max(z1, z2); z++) {
                    put(lv, st, x, y, z, b);
                }
            }
        }
    }

    /** El bloque de color de un lado del marcador (el color 0 es el del primer equipo, etc.). */
    public static Block sideBlock(int sideIndex) {
        return SIDE_BLOCKS[Math.floorMod(sideIndex, SIDE_BLOCKS.length)];
    }

    /** A que podio va cada lado del marcador, y en que mitad (0 izquierda, 1 derecha, 2 completo). Devuelve {podio, mitad}. */
    public static int[] podiumOfSide(Format fmt, int side) {
        if (fmt == Format.TWO_TEAMS) {
            return new int[] {side == 0 ? 0 : 2, 2};
        }
        if (fmt == Format.THREE_TEAMS) {
            return new int[] {Math.min(2, side), 2};
        }
        return new int[] {Math.min(2, side / 2), side % 2};
    }

    /** Centro (x local) de un podio. */
    public static int podiumCenter(int pod) {
        return PODIUM_X[Math.max(0, Math.min(2, pod))];
    }

    /** Construye el estudio. {@code origin} es el bloque del suelo donde esta parado el jugador (que mira hacia el escenario). */
    public static void build(ServerLevel lv, BlockPos origin, Direction facing) {
        StageData.Info st = new StageData.Info(origin.getX(), origin.getY(), origin.getZ(), idxOf(facing));
        StageData.Info old = StageData.get(lv.getServer()).info();
        Displays.clearAll(lv, old);
        Displays.clearAll(lv, st);
        version++;
        // aire: lo de arriba se limpia (la fila del suelo es y=0)
        fill(lv, st, -HALF_W - 1, 1, Z_MIN, HALF_W + 1, HEIGHT + 2, BACK_Z + 1, Blocks.AIR);
        fill(lv, st, -HALF_W - 1, 0, Z_MIN, HALF_W + 1, 0, BACK_Z + 1, Blocks.BLACK_CONCRETE);
        // paredes laterales, pared de atras del publico y techo (todo cerrado)
        fill(lv, st, -HALF_W - 1, 1, Z_MIN, -HALF_W - 1, HEIGHT + 1, BACK_Z, Blocks.BLACK_CONCRETE);
        fill(lv, st, HALF_W + 1, 1, Z_MIN, HALF_W + 1, HEIGHT + 1, BACK_Z, Blocks.BLACK_CONCRETE);
        fill(lv, st, -HALF_W, 1, Z_MIN, HALF_W, HEIGHT + 1, Z_MIN, Blocks.BLACK_CONCRETE);
        fill(lv, st, -HALF_W - 1, HEIGHT + 1, Z_MIN, HALF_W + 1, HEIGHT + 1, BACK_Z, Blocks.BLACK_CONCRETE);

        buildAudience(lv, st);
        buildStage(lv, st);
        buildScreenWall(lv, st);
        buildCeiling(lv, st);
        paintPodiums(lv, st, null);
        StageData.get(lv.getServer()).set(origin, idxOf(facing));
        spawnCrowd(lv, st);
    }

    private static void buildAudience(ServerLevel lv, StageData.Info st) {
        for (int r = 0; r < ROWS; r++) {
            int z0 = 20 - 3 * r;
            for (int x = -HALF_W + 2; x <= HALF_W - 2; x++) {
                boolean aisle = Math.abs(x) <= 2;
                for (int y = 1; y < r; y++) {
                    for (int z = z0; z <= z0 + 2; z++) {
                        put(lv, st, x, y, z, Blocks.BLACK_CONCRETE);
                    }
                }
                for (int z = z0; z <= z0 + 2; z++) {
                    Block top = aisle ? Blocks.LIGHT_GRAY_CONCRETE : Blocks.RED_CONCRETE;
                    if (z == z0 + 2 && !aisle) {
                        top = Blocks.SEA_LANTERN;
                    }
                    put(lv, st, x, r, z, top);
                }
            }
        }
        // pasillo central de la entrada (escalones ya forman el pasillo); barandal al frente
        fill(lv, st, -HALF_W + 2, 1, 23, HALF_W - 2, 1, 23, Blocks.IRON_BARS);
    }

    private static void buildStage(ServerLevel lv, StageData.Info st) {
        // piso de tablero oscuro
        for (int x = -HALF_W + 1; x <= HALF_W - 1; x++) {
            for (int z = STAGE_Z; z <= BACK_Z - 1; z++) {
                boolean alt = (((x + 40) >> 2) + (z >> 2)) % 2 == 0;
                put(lv, st, x, 1, z, alt ? Blocks.BLACK_CONCRETE : Blocks.GRAY_CONCRETE);
            }
        }
        // borde luminoso al frente y lineas de luz en el piso
        fill(lv, st, -HALF_W + 1, 1, STAGE_Z, HALF_W - 1, 1, STAGE_Z, Blocks.SEA_LANTERN);
        for (int x : new int[] {-20, -13, -6, 0, 6, 13, 20}) {
            fill(lv, st, x, 1, STAGE_Z + 2, x, 1, BACK_Z - 2, Blocks.SEA_LANTERN);
        }
        fill(lv, st, -20, 1, 30, 20, 1, 30, Blocks.SEA_LANTERN);
        fill(lv, st, -20, 1, 52, 20, 1, 52, Blocks.SEA_LANTERN);
        // los tres podios: tarima, escritorio y botones
        for (int i = 0; i < 3; i++) {
            int cx = PODIUM_X[i];
            fill(lv, st, cx - 5, 2, PODIUM_Z - 6, cx + 5, 2, PODIUM_Z + 2, Blocks.WHITE_CONCRETE);
            fill(lv, st, cx - 4, 2, PODIUM_Z - 5, cx + 4, 2, PODIUM_Z + 1, Blocks.GRAY_CONCRETE);
            fill(lv, st, cx - 5, 2, PODIUM_Z - 6, cx + 5, 2, PODIUM_Z - 6, Blocks.SEA_LANTERN);
            for (int dx : new int[] {-3, 0, 3}) {
                put(lv, st, cx + dx, 4, PODIUM_Z, Blocks.POLISHED_BLACKSTONE_BUTTON.defaultBlockState()
                        .setValue(net.minecraft.world.level.block.ButtonBlock.FACE, net.minecraft.world.level.block.state.properties.AttachFace.FLOOR));
            }
        }
        // el escritorio del presentador, a la derecha de los podios (tambien mira a la pantalla)
        fill(lv, st, 19, 2, 34, 23, 2, 35, Blocks.QUARTZ_BLOCK);
        fill(lv, st, 18, 1, 31, 24, 1, 31, Blocks.SEA_LANTERN);
        put(lv, st, 21, 3, 34, Blocks.LECTERN);
    }

    private static void buildScreenWall(ServerLevel lv, StageData.Info st) {
        fill(lv, st, -HALF_W, 1, BACK_Z, HALF_W, HEIGHT, BACK_Z, Blocks.PURPLE_CONCRETE);
        fill(lv, st, -19, 5, BACK_Z, 19, 18, BACK_Z, Blocks.BLACK_CONCRETE);
        // franjas de la tabla de la pantalla: 4 filas de 2 columnas (los textos se dibujan encima, ver Displays)
        for (int row = 0; row < 4; row++) {
            int lo = 13 - 2 * row;
            Block pb = row % 2 == 0 ? Blocks.BLUE_CONCRETE : Blocks.CYAN_CONCRETE;
            fill(lv, st, -18, lo, BACK_Z, -1, lo + 1, BACK_Z, pb);
            fill(lv, st, 1, lo, BACK_Z, 18, lo + 1, BACK_Z, pb);
        }
        fill(lv, st, -19, 17, BACK_Z, 19, 18, BACK_Z, Blocks.BLACK_CONCRETE);
        // marco luminoso de la pantalla
        fill(lv, st, -20, 4, BACK_Z, 20, 4, BACK_Z, Blocks.SEA_LANTERN);
        fill(lv, st, -20, 19, BACK_Z, 20, 19, BACK_Z, Blocks.SEA_LANTERN);
        fill(lv, st, -20, 5, BACK_Z, -20, 18, BACK_Z, Blocks.SEA_LANTERN);
        fill(lv, st, 20, 5, BACK_Z, 20, 18, BACK_Z, Blocks.SEA_LANTERN);
        // cortinas rojas a los lados del escenario
        for (int z = STAGE_Z + 1; z <= BACK_Z - 1; z++) {
            Block b = z % 3 == 0 ? Blocks.RED_CONCRETE : Blocks.RED_WOOL;
            for (int side : new int[] {-1, 1}) {
                fill(lv, st, side * (HALF_W - 1), 2, z, side * HALF_W, HEIGHT - 2, z, b);
            }
        }
    }

    private static void buildCeiling(ServerLevel lv, StageData.Info st) {
        int ty = HEIGHT - 2;
        // reflectores colgados de una estructura de barras
        for (int z : new int[] {32, 42, 52}) {
            fill(lv, st, -HALF_W + 1, ty, z, HALF_W - 1, ty, z, Blocks.IRON_BARS);
        }
        for (int x : new int[] {-20, -10, 0, 10, 20}) {
            fill(lv, st, x, ty, 28, x, ty, 56, Blocks.IRON_BARS);
        }
        Block[] fr = {Blocks.OCHRE_FROGLIGHT, Blocks.VERDANT_FROGLIGHT, Blocks.PEARLESCENT_FROGLIGHT};
        int k = 0;
        for (int z : new int[] {32, 42, 52}) {
            for (int x : new int[] {-20, -10, 0, 10, 20}) {
                put(lv, st, x, ty - 1, z, fr[k++ % 3]);
            }
        }
        // luz invisible repartida por todo el estudio para que no se vea oscuro
        for (int x = -18; x <= 18; x += 12) {
            for (int z = 8; z <= 56; z += 12) {
                put(lv, st, x, 9, z, Blocks.LIGHT);
            }
        }
        for (int x : new int[] {-12, 12}) {
            for (int z : new int[] {10, 16}) {
                put(lv, st, x, 11, z, Blocks.LIGHT);
            }
        }
    }

    /** Pinta los escritorios de cada podio con el color de quien juega ahi (o los colores de siempre si {@code sh} es null). */
    public static void paintPodiums(ServerLevel lv, StageData.Info st, Show sh) {
        Block[] left = {Blocks.RED_CONCRETE, Blocks.LIGHT_BLUE_CONCRETE, Blocks.YELLOW_CONCRETE};
        Block[] right = left.clone();
        if (sh != null) {
            Block neutral = Blocks.GRAY_CONCRETE;
            for (int i = 0; i < 3; i++) {
                left[i] = neutral;
                right[i] = neutral;
            }
            for (Side sd : sh.sides) {
                if (sd.members.isEmpty()) {
                    continue;
                }
                int[] pm = podiumOfSide(sh.format, sd.index);
                Block b = sideBlock(sd.index);
                if (pm[1] == 2) {
                    left[pm[0]] = b;
                    right[pm[0]] = b;
                } else if (pm[1] == 0) {
                    left[pm[0]] = b;
                } else {
                    right[pm[0]] = b;
                }
            }
        }
        for (int i = 0; i < 3; i++) {
            int cx = PODIUM_X[i];
            fill(lv, st, cx - 4, 3, PODIUM_Z, cx, 3, PODIUM_Z + 1, left[i]);
            fill(lv, st, cx + 1, 3, PODIUM_Z, cx + 4, 3, PODIUM_Z + 1, right[i]);

        }
    }

    /** Pone al publico en las gradas (borra el anterior). */
    public static void spawnCrowd(ServerLevel lv, StageData.Info st) {
        clearCrowd(lv, st);
        Random rnd = new Random();
        Direction f = dirOf(st.dir());
        float yaw = (float) Math.toDegrees(Math.atan2(-f.getStepX(), f.getStepZ()));
        for (int r = 0; r < ROWS; r++) {
            int z0 = 20 - 3 * r;
            for (int x = -HALF_W + 3; x <= HALF_W - 3; x += 3) {
                if (Math.abs(x) <= 3) {
                    continue;
                }
                double[] p = atD(st, x + (rnd.nextDouble() - 0.5) * 0.8, r + 1, z0 + 1.3 + rnd.nextDouble() * 0.5);
                SpectatorEntity e = new SpectatorEntity(lv);
                e.moveTo(p[0], p[1], p[2], yaw, 0);
                e.setYHeadRot(yaw);
                e.setYBodyRot(yaw);
                e.setSkin(rnd.nextInt(SpectatorEntity.SKINS));
                lv.addFreshEntity(e);
            }
        }
    }

    private static AABB area(StageData.Info st) {
        double[] a = atD(st, -HALF_W - 2, 0, Z_MIN - 1);
        double[] b = atD(st, HALF_W + 2, HEIGHT + 2, BACK_Z + 1);
        return new AABB(Math.min(a[0], b[0]), a[1], Math.min(a[2], b[2]), Math.max(a[0], b[0]), b[1], Math.max(a[2], b[2]));
    }

    public static void clearCrowd(ServerLevel lv, StageData.Info st) {
        for (SpectatorEntity e : lv.getEntitiesOfClass(SpectatorEntity.class, area(st))) {
            e.discard();
        }
    }

    /** El area del estudio en el mundo (para buscar cosas dentro). */
    public static AABB studioArea(StageData.Info st) {
        return area(st);
    }

    /** Cambia el humor de todo el publico: 0 normal, 1 aplaude, 2 vitorea, 3 abuchea, 4 se rie. */
    public static void crowd(MinecraftServer server, int mood, int ticks) {
        if (server == null) {
            return;
        }
        StageData.Info st = StageData.get(server).info();
        if (st == null) {
            return;
        }
        ServerLevel lv = server.overworld();
        List<SpectatorEntity> list = lv.getEntitiesOfClass(SpectatorEntity.class, area(st));
        Random rnd = new Random();
        for (SpectatorEntity e : list) {
            e.setMood(mood, ticks + rnd.nextInt(20));
        }
    }

    /** Posicion (x local) de un jugador dentro de su podio. */
    public static double slotX(Show sh, Participant p, Side sd, int k) {
        int[] pm = podiumOfSide(sh.format, sd.index);
        int cx = podiumCenter(pm[0]);
        if (sh.format == Format.TWO_TEAMS) {
            return cx + (k - 1) * 2.5;
        }
        if (sh.format == Format.THREE_TEAMS) {
            return cx + (k == 0 ? -2.0 : 2.0);
        }
        return cx + (pm[1] == 0 ? -2.0 : 2.0);
    }

    /** Coloca a cada jugador en su lugar del podio, mirando al publico, y al presentador en su escritorio. */
    public static void teleportToPodiums(MinecraftServer server, Show sh) {
        StageData.Info st = StageData.get(server).info();
        if (st == null) {
            return;
        }
        ServerLevel lv = server.overworld();
        Direction f = dirOf(st.dir());
        float yaw = (float) Math.toDegrees(Math.atan2(-f.getStepX(), f.getStepZ())); // mirando a la pantalla (+F)
        for (Side sd : sh.sides) {
            int k = 0;
            for (Participant p : sd.members) {
                double[] pos = atD(st, slotX(sh, p, sd, k), PODIUM_FLOOR, PODIUM_Z - 1.4);
                ServerPlayer sp = server.getPlayerList().getPlayer(p.id);
                if (sp != null) {
                    sp.teleportTo(lv, pos[0], pos[1], pos[2], yaw, 0f);
                }
                k++;
            }
        }
        if (sh.host != null && sh.byId(sh.host) == null) {
            ServerPlayer hp = server.getPlayerList().getPlayer(sh.host);
            if (hp != null) {
                double[] pos = atD(st, 21, 2, 32.6);
                hp.teleportTo(lv, pos[0], pos[1], pos[2], yaw, 0f);
            }
        }
    }
}
