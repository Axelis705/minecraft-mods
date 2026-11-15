package com.casino.diamond;

import com.casino.plat.CasinoBE;
import com.casino.plat.Nbt;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * Pantalla de récords. Varias pantallas juntas (misma pared, misma dirección) forman una sola grande: la de arriba a la izquierda
 * (vista de frente) es la "ancla" y dibuja todo el rectángulo. Va cambiando de canal sola cada {@link #CHANNEL_TICKS}; con la mano
 * vacía y clic derecho se pasa al siguiente canal.
 */
public class RecordScreenBE extends CasinoBE {
    public static final int CHANNELS = 8;
    public static final int CHANNEL_TICKS = 200;
    public static final int MAX_W = 16;
    public static final int MAX_H = 9;

    /** Canales que se adelantó a mano (se suma al reloj). Lo guarda la ancla. */
    public int offset;

    // solo en el cliente: si es la ancla y el tamaño del rectángulo
    public boolean anchor;
    public int width = 1;
    public int height = 1;
    private long checked = Long.MIN_VALUE;

    public RecordScreenBE(BlockPos pos, BlockState state) {
        super(DiamondReg.RECORD_BE.get(), pos, state);
    }

    public Direction facing() {
        BlockState s = getBlockState();
        return s.hasProperty(RecordScreenBlock.FACING) ? s.getValue(RecordScreenBlock.FACING) : Direction.NORTH;
    }

    /** Hacia dónde es "la derecha" para quien mira la pantalla de frente. */
    public static Direction right(Direction facing) {
        return facing.getCounterClockWise();
    }

    static boolean same(BlockGetter g, BlockPos p, Direction f) {
        BlockState s = g.getBlockState(p);
        return s.getBlock() instanceof RecordScreenBlock && s.getValue(RecordScreenBlock.FACING) == f;
    }

    public static boolean isAnchor(BlockGetter g, BlockPos p, Direction f) {
        return !same(g, p.relative(right(f).getOpposite()), f) && !same(g, p.above(), f);
    }

    /** La ancla del grupo al que pertenece esta pantalla (camina a la izquierda y hacia arriba). */
    public static BlockPos findAnchor(BlockGetter g, BlockPos p, Direction f) {
        BlockPos cur = p;
        Direction left = right(f).getOpposite();
        for (int i = 0; i < 64; i++) {
            if (same(g, cur.relative(left), f)) {
                cur = cur.relative(left);
            } else if (same(g, cur.above(), f)) {
                cur = cur.above();
            } else {
                break;
            }
        }
        return cur;
    }

    /** Tamaño del rectángulo que empieza en esta ancla: ancho por la fila de arriba y luego filas completas hacia abajo. */
    public static int[] measure(BlockGetter g, BlockPos p, Direction f) {
        Direction r = right(f);
        int w = 1;
        while (w < MAX_W && same(g, p.relative(r, w), f)) {
            w++;
        }
        int h = 1;
        rows:
        while (h < MAX_H) {
            BlockPos row = p.below(h);
            for (int x = 0; x < w; x++) {
                if (!same(g, row.relative(r, x), f)) {
                    break rows;
                }
            }
            h++;
        }
        return new int[]{w, h};
    }

    public void clientTick(Level level) {
        long now = level.getGameTime();
        if (now - checked < 10 && now >= checked) {
            return;
        }
        checked = now;
        Direction f = facing();
        anchor = isAnchor(level, worldPosition, f);
        if (anchor) {
            int[] m = measure(level, worldPosition, f);
            width = m[0];
            height = m[1];
        }
    }

    public static void serverTick(Level level, BlockPos pos, BlockState st, RecordScreenBE be) {
        long now = level.getGameTime();
        if ((now + (pos.asLong() & 63)) % 60 != 0 || !isAnchor(level, pos, be.facing())) {
            return;
        }
        for (var pl : level.players()) {
            if (pl instanceof ServerPlayer sp && sp.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) < 48 * 48) {
                com.casino.net.LeaderboardPacket.sendIfChanged(sp);
            }
        }
    }

    /** Canal que se ve ahora y cuántos ticks lleva en él. */
    public int channel(long now) {
        long base = now / CHANNEL_TICKS + offset + (worldPosition.asLong() & 7);
        return (int) Math.floorMod(base, CHANNELS);
    }

    public int channelAge(long now) {
        return (int) (now % CHANNEL_TICKS);
    }

    public void sync() {
        setChanged();
        if (level != null && !level.isClientSide()) {
            BlockState s = getBlockState();
            level.sendBlockUpdated(worldPosition, s, s, 3);
        }
    }

    @Override
    protected AABB renderBox() {
        if (!anchor) {
            return new AABB(worldPosition);
        }
        Direction r = right(facing());
        BlockPos far = worldPosition.relative(r, width - 1).below(height - 1);
        return new AABB(Math.min(worldPosition.getX(), far.getX()), far.getY(), Math.min(worldPosition.getZ(), far.getZ()),
                Math.max(worldPosition.getX(), far.getX()) + 1, worldPosition.getY() + 1, Math.max(worldPosition.getZ(), far.getZ()) + 1).inflate(0.5);
    }

    @Override
    protected void writeData(CompoundTag tag) {
        tag.putInt("Off", offset);
    }

    @Override
    protected void readData(CompoundTag tag) {
        offset = Nbt.i(tag, "Off");
    }
}
