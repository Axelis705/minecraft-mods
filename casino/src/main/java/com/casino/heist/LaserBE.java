package com.casino.heist;

import com.casino.plat.CasinoBE;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * Emisor láser: un rayo rojo hasta la pared de enfrente que se prende y apaga con su ritmo ({@link HeistBlocks.Laser#PATTERN}). El rayo
 * va a 0.6 del alto del bloque: el de abajo se brinca, el de arriba se pasa agachado. Si alguien del robo lo cruza en la parte
 * sigilosa, suena la alarma; durante el escape quema un poco.
 */
public class LaserBE extends CasinoBE {
    public static final int PERIOD = 80;
    public static final int ON = 48;
    public static final int MAX = 10;
    public static final double HEIGHT = 0.6;
    private int length = -1;

    public LaserBE(BlockPos pos, BlockState state) {
        super(HeistReg.LASER_BE.get(), pos, state);
    }

    public Direction facing() {
        BlockState s = getBlockState();
        return s.hasProperty(HeistBlocks.FACING) ? s.getValue(HeistBlocks.FACING) : Direction.NORTH;
    }

    public int pattern() {
        BlockState s = getBlockState();
        return s.hasProperty(HeistBlocks.Laser.PATTERN) ? s.getValue(HeistBlocks.Laser.PATTERN) : 0;
    }

    /** ¿Está prendido en este momento? (mismo cálculo en servidor y cliente) */
    public static boolean on(long time, int pattern) {
        return Math.floorMod(time + pattern * 20L, PERIOD) < ON;
    }

    /** Cuántos bloques llega el rayo (hasta el primer bloque sólido). */
    public static int length(BlockGetter g, BlockPos pos, Direction f) {
        for (int i = 1; i <= MAX; i++) {
            BlockPos p = pos.relative(f, i);
            if (!g.getBlockState(p).getCollisionShape(g, p).isEmpty()) {
                return i - 1;
            }
        }
        return MAX;
    }

    public AABB beam(int len) {
        Direction f = facing();
        double cx = worldPosition.getX() + 0.5, cz = worldPosition.getZ() + 0.5, y = worldPosition.getY() + HEIGHT;
        double ex = cx + f.getStepX() * (len + 0.5), ez = cz + f.getStepZ() * (len + 0.5);
        return new AABB(Math.min(cx, ex) - 0.03, y - 0.03, Math.min(cz, ez) - 0.03, Math.max(cx, ex) + 0.03, y + 0.03, Math.max(cz, ez) + 0.03);
    }

    void serverTick() {
        if (level == null) {
            return;
        }
        long t = level.getGameTime();
        if (length < 0 || t % 100 == 0) {
            length = length(level, worldPosition, facing());
        }
        if (t % 2 != 0 || !on(t, pattern()) || length <= 0) {
            return;
        }
        HeistManager.Heist h = HeistManager.at(level, worldPosition);
        if (h == null) {
            return;
        }
        for (Player p : level.getEntitiesOfClass(Player.class, beam(length))) {
            if (p instanceof ServerPlayer sp && h.isCrew(sp)) {
                HeistManager.laser(h, sp, worldPosition);
            }
        }
    }

    @Override
    protected void writeData(CompoundTag tag) {
    }

    @Override
    protected void readData(CompoundTag tag) {
    }
}
