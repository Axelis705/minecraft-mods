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
 * Escenario: publico en gradas (z 1..10), escenario (z 11..23) con tres podios de colores (z 15) y pared de fondo (z 24).
 */
public final class Studio {
    public static final int HALF_W = 15;
    public static final int DEPTH = 26;
    public static final int HEIGHT = 15;
    /** Centro de cada podio en x local. */
    public static final int[] PODIUM_X = {-9, 0, 9};
    public static final int PODIUM_Z = 15;
    private static final Direction[] HORIZ = {Direction.SOUTH, Direction.WEST, Direction.NORTH, Direction.EAST};

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

    private static Block podiumBlock(int i) {
        return switch (i) {
            case 0 -> Blocks.RED_CONCRETE;
            case 1 -> Blocks.BLUE_CONCRETE;
            default -> Blocks.YELLOW_CONCRETE;
        };
    }

    /** Construye el estudio. {@code origin} es el bloque del suelo donde esta parado el jugador (que mira hacia el escenario). */
    public static void build(ServerLevel lv, BlockPos origin, Direction facing) {
        StageData.Info st = new StageData.Info(origin.getX(), origin.getY(), origin.getZ(), idxOf(facing));
        // la fila del suelo es y=0; todo lo de arriba se limpia
        fill(lv, st, -HALF_W, 1, -2, HALF_W, HEIGHT, DEPTH, Blocks.AIR);
        fill(lv, st, -HALF_W, 0, -2, HALF_W, 0, DEPTH, Blocks.BLACK_CONCRETE);
        // gradas del publico
        for (int r = 0; r < 5; r++) {
            int z0 = 1 + r * 2;
            for (int h = 1; h <= r + 1; h++) {
                fill(lv, st, -HALF_W + 2, h, z0, HALF_W - 2, h, z0 + 1, h == r + 1 ? Blocks.GRAY_CONCRETE : Blocks.BLACK_CONCRETE);
            }
            fill(lv, st, -HALF_W + 2, r + 1, z0, HALF_W - 2, r + 1, z0, Blocks.RED_CONCRETE);
        }
        // escenario elevado con franja luminosa al frente
        fill(lv, st, -HALF_W + 1, 1, 11, HALF_W - 1, 1, DEPTH - 2, Blocks.POLISHED_DEEPSLATE);
        fill(lv, st, -HALF_W + 1, 1, 11, HALF_W - 1, 1, 11, Blocks.SEA_LANTERN);
        for (int x = -HALF_W + 3; x <= HALF_W - 3; x += 6) {
            fill(lv, st, x - 1, 1, 18, x + 1, 1, 20, Blocks.WHITE_CONCRETE);
        }
        // pared de fondo con la "pantalla" y marco luminoso
        fill(lv, st, -HALF_W, 1, DEPTH - 1, HALF_W, HEIGHT - 3, DEPTH, Blocks.PURPLE_CONCRETE);
        fill(lv, st, -11, 3, DEPTH - 1, 11, 11, DEPTH - 1, Blocks.BLACK_CONCRETE);
        fill(lv, st, -12, 2, DEPTH - 1, 12, 2, DEPTH - 1, Blocks.SEA_LANTERN);
        fill(lv, st, -12, 12, DEPTH - 1, 12, 12, DEPTH - 1, Blocks.SEA_LANTERN);
        fill(lv, st, -12, 3, DEPTH - 1, -12, 11, DEPTH - 1, Blocks.SEA_LANTERN);
        fill(lv, st, 12, 3, DEPTH - 1, 12, 11, DEPTH - 1, Blocks.SEA_LANTERN);
        // paredes laterales y techo con luces
        fill(lv, st, -HALF_W - 1, 1, -2, -HALF_W - 1, HEIGHT, DEPTH, Blocks.BLACK_CONCRETE);
        fill(lv, st, HALF_W + 1, 1, -2, HALF_W + 1, HEIGHT, DEPTH, Blocks.BLACK_CONCRETE);
        fill(lv, st, -HALF_W, 1, -3, HALF_W, HEIGHT, -3, Blocks.BLACK_CONCRETE);
        fill(lv, st, -HALF_W - 1, HEIGHT + 1, -3, HALF_W + 1, HEIGHT + 1, DEPTH, Blocks.BLACK_CONCRETE);
        for (int x = -HALF_W + 2; x <= HALF_W - 2; x += 4) {
            for (int z = 0; z <= DEPTH - 2; z += 4) {
                put(lv, st, x, HEIGHT, z, Blocks.GLOWSTONE);
            }
        }
        // los tres podios y sus botones
        for (int i = 0; i < 3; i++) {
            int cx = PODIUM_X[i];
            fill(lv, st, cx - 3, 2, PODIUM_Z, cx + 3, 2, PODIUM_Z + 1, podiumBlock(i));
            for (int dx = -2; dx <= 2; dx += 2) {
                put(lv, st, cx + dx, 3, PODIUM_Z, Blocks.WHITE_CONCRETE_POWDER);
            }
            put(lv, st, cx, HEIGHT, PODIUM_Z, Blocks.SEA_LANTERN);
        }
        // el escritorio del presentador a la derecha del escenario
        fill(lv, st, 10, 2, 12, 13, 2, 13, Blocks.QUARTZ_BLOCK);
        put(lv, st, 11, 3, 12, Blocks.LECTERN);
        StageData.get(lv.getServer()).set(origin, idxOf(facing));
        spawnCrowd(lv, st);
    }

    /** Pone al publico en las gradas (borra el anterior). */
    public static void spawnCrowd(ServerLevel lv, StageData.Info st) {
        clearCrowd(lv, st);
        Random rnd = new Random();
        Direction f = dirOf(st.dir());
        float yaw = (float) Math.toDegrees(Math.atan2(-f.getStepX(), f.getStepZ()));
        for (int r = 0; r < 5; r++) {
            for (double x = -HALF_W + 3; x <= HALF_W - 3; x += 2.5) {
                double[] p = atD(st, x + rnd.nextDouble() * 0.6, r + 2, 1.6 + r * 2 + rnd.nextDouble() * 0.4);
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
        double[] a = atD(st, -HALF_W - 2, 0, -3);
        double[] b = atD(st, HALF_W + 2, HEIGHT + 2, DEPTH + 1);
        return new AABB(a[0], a[1], a[2], b[0], b[1], b[2]);
    }

    public static void clearCrowd(ServerLevel lv, StageData.Info st) {
        for (SpectatorEntity e : lv.getEntitiesOfClass(SpectatorEntity.class, area(st))) {
            e.discard();
        }
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

    /** Coloca a cada jugador en su lugar del podio, mirando al publico, y al presentador en su escritorio. */
    public static void teleportToPodiums(MinecraftServer server, Show sh) {
        StageData.Info st = StageData.get(server).info();
        if (st == null) {
            return;
        }
        ServerLevel lv = server.overworld();
        Direction f = dirOf(st.dir());
        float yaw = (float) Math.toDegrees(Math.atan2(f.getStepX(), -f.getStepZ())); // mirando hacia el publico (-F)
        for (Side sd : sh.sides) {
            int pod;
            int slotsWide;
            if (sh.format == Format.TWO_TEAMS) {
                pod = sd.index == 0 ? 0 : 2;
                slotsWide = 3;
            } else if (sh.format == Format.THREE_TEAMS) {
                pod = sd.index;
                slotsWide = 2;
            } else {
                pod = sd.index / 2;
                slotsWide = 2;
            }
            int k = 0;
            for (Participant p : sd.members) {
                int slot = sh.format == Format.FFA ? sd.index % 2 : k;
                double dx = slotsWide == 3 ? (slot - 1) * 2.0 : (slot == 0 ? -1.0 : 1.0);
                double[] pos = atD(st, PODIUM_X[Math.min(pod, 2)] + dx, 2, PODIUM_Z + 2.5);
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
                double[] pos = atD(st, 11.5, 2, 14.6);
                hp.teleportTo(lv, pos[0], pos[1], pos[2], yaw + 90f, 0f);
            }
        }
    }
}
