package com.kensei.duel;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * La arena de un duelo: un disco flotante de piedra oscura con un emblema rojo y dorado, ocho pilares con faroles y un cilindro de
 * bloques barrera invisibles (paredes y techo) para que nadie salga ni caiga. Se construye en el cielo, justo sobre el lugar de quien
 * desafía, y al terminar se devuelve cada bloque a como estaba.
 */
public final class DuelArena {
    /** Cuánto hay desde el suelo hasta el techo de barreras. */
    private static final int HEIGHT = 15;
    private static final int FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;

    public final ServerLevel level;
    public final int cx;
    public final int cz;
    public final int floorY;
    public final int radius;

    private final List<BlockPos> changed = new ArrayList<>();
    private final Map<BlockPos, BlockState> originals = new HashMap<>();
    private boolean built = false;

    public DuelArena(ServerLevel level, int cx, int cz, int radius) {
        this.level = level;
        this.cx = cx;
        this.cz = cz;
        this.radius = radius;
        this.floorY = Math.max(level.getMinBuildHeight() + 24, level.getMaxBuildHeight() - 34);
    }

    /** Zona que ocupa (con margen), para no construir dos arenas encimadas. */
    public AABB bounds() {
        int r = radius + 4;
        return new AABB(cx - r, floorY - 9, cz - r, cx + r + 1, floorY + HEIGHT + 2, cz + r + 1);
    }

    /** ¿Hay mucho construido en el lugar (por ejemplo una base en el cielo)? Se revisa una cuadrícula de puntos. */
    public boolean crowded() {
        int solid = 0;
        for (int dx = -radius; dx <= radius; dx += 3) {
            for (int dz = -radius; dz <= radius; dz += 3) {
                for (int y = floorY - 8; y <= floorY + HEIGHT; y += 3) {
                    if (!level.getBlockState(new BlockPos(cx + dx, y, cz + dz)).isAir()) {
                        solid++;
                    }
                }
            }
        }
        return solid > 25;
    }

    private void set(int x, int y, int z, BlockState state) {
        BlockPos p = new BlockPos(x, y, z);
        if (!originals.containsKey(p)) {
            BlockState old = level.getBlockState(p);
            originals.put(p, old);
            changed.add(p);
        }
        level.setBlock(p, state, FLAGS);
    }

    private BlockState floorTile(int dx, int dz, double d) {
        if (d <= 2.3) {
            return Blocks.RED_CONCRETE.defaultBlockState();
        }
        if (d <= 3.2) {
            return Blocks.GOLD_BLOCK.defaultBlockState();
        }
        if (d > radius - 1.6 && d <= radius - 0.6) {
            return Blocks.GOLD_BLOCK.defaultBlockState();
        }
        if (Math.abs(dz) <= 0 && Math.abs(dx) > 3 && Math.abs(dx) < radius - 1.6) {
            return Blocks.POLISHED_BLACKSTONE.defaultBlockState(); // línea central
        }
        return (((dx + dz) & 1) == 0 ? Blocks.DEEPSLATE_TILES : Blocks.POLISHED_DEEPSLATE).defaultBlockState();
    }

    public void build() {
        if (built) {
            return;
        }
        built = true;
        int r = radius;
        // cargar las columnas que se tocan
        for (int x = cx - r - 2; x <= cx + r + 2; x += 16) {
            for (int z = cz - r - 2; z <= cz + r + 2; z += 16) {
                level.getChunk(x >> 4, z >> 4);
            }
        }
        level.getChunk((cx + r + 2) >> 4, (cz + r + 2) >> 4);
        BlockState barrier = Blocks.BARRIER.defaultBlockState();
        for (int dx = -r - 2; dx <= r + 2; dx++) {
            for (int dz = -r - 2; dz <= r + 2; dz++) {
                double d = Math.sqrt(dx * dx + dz * dz);
                int x = cx + dx;
                int z = cz + dz;
                if (d <= r + 0.5) {
                    set(x, floorY, z, floorTile(dx, dz, d));
                    set(x, floorY - 1, z, Blocks.POLISHED_BLACKSTONE.defaultBlockState());
                    // la isla se va afinando hacia abajo
                    for (int k = 2; k <= 7; k++) {
                        if (d <= r - (k - 1) * 1.9) {
                            set(x, floorY - k, z, (k % 2 == 0 ? Blocks.BLACKSTONE : Blocks.DEEPSLATE).defaultBlockState());
                        }
                    }
                } else if (d <= r + 1.8) {
                    for (int y = floorY; y <= floorY + HEIGHT - 1; y++) {
                        set(x, y, z, barrier);
                    }
                }
                if (d <= r + 1.8) {
                    set(x, floorY + HEIGHT, z, barrier);
                }
            }
        }
        // ocho pilares con farol
        for (int k = 0; k < 8; k++) {
            double a = Math.PI / 4.0 * k + Math.PI / 8.0;
            int px = cx + (int) Math.round(Math.cos(a) * (r - 2.2));
            int pz = cz + (int) Math.round(Math.sin(a) * (r - 2.2));
            for (int y = floorY + 1; y <= floorY + 4; y++) {
                set(px, y, pz, Blocks.POLISHED_BLACKSTONE_BRICK_WALL.defaultBlockState());
            }
            set(px, floorY + 5, pz, Blocks.LANTERN.defaultBlockState());
        }
        // luz invisible para que no salgan monstruos y se vea bien
        BlockState light = Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, 15);
        set(cx, floorY + 7, cz, light);
        for (int k = 0; k < 4; k++) {
            double a = Math.PI / 2.0 * k;
            set(cx + (int) Math.round(Math.cos(a) * r * 0.5), floorY + 7, cz + (int) Math.round(Math.sin(a) * r * 0.5), light);
        }
    }

    /** Devuelve todos los bloques a como estaban. */
    public void restore() {
        if (!built) {
            return;
        }
        built = false;
        for (BlockPos p : changed) {
            BlockState old = originals.get(p);
            level.setBlock(p, old == null ? Blocks.AIR.defaultBlockState() : old, FLAGS);
        }
        changed.clear();
        originals.clear();
    }

    /** Dónde empieza cada peleador: el equipo 0 a la izquierda (mirando al este), el 1 a la derecha (mirando al oeste). */
    public Vec3 spawn(int team, int slot, int teamSize) {
        double off = radius * 0.45;
        double x = cx + 0.5 + (team == 0 ? -off : off);
        double z = cz + 0.5 + (teamSize <= 1 ? 0.0 : (slot == 0 ? -2.5 : 2.5));
        return new Vec3(x, floorY + 1, z);
    }

    public static float spawnYaw(int team) {
        return team == 0 ? -90.0F : 90.0F;
    }

    /** Dónde se pone a los espectadores: en el borde, mirando hacia el centro. */
    public Vec3 spectatorSpot() {
        return new Vec3(cx + 0.5, floorY + 4, cz + 0.5 + radius - 2.5);
    }

    public Vec3 center() {
        return new Vec3(cx + 0.5, floorY + 1, cz + 0.5);
    }
}
