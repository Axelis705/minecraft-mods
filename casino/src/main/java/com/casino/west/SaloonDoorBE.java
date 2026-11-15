package com.casino.west;

import com.casino.plat.CasinoBE;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * Hoja de una puerta de saloon. Todo pasa en el cliente: cada tick mira quién cruza el hueco, empuja la hoja hacia donde camina y
 * la deja abierta mientras pasa; al soltarla vuelve como resorte con rebotes (amortiguado). Ángulo en grados: + = abierta hacia FACING.
 */
public class SaloonDoorBE extends CasinoBE {
    /** Rigidez y freno del resorte (por tick): oscila unas 3-4 veces antes de quedarse quieta. */
    private static final float K = 0.11F, DAMP = 0.16F;
    private static final float OPEN = 82F, MAX = 100F;

    public float angle, prevAngle, vel;
    private float target;
    private int holdTicks;
    private long lastCreak;

    public SaloonDoorBE(BlockPos pos, BlockState state) {
        this(WestReg.DOOR_BE.get(), pos, state);
    }

    public SaloonDoorBE(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public Direction facing() {
        BlockState s = getBlockState();
        return s.hasProperty(WestBlocks.FACING) ? s.getValue(WestBlocks.FACING) : Direction.NORTH;
    }

    /** Empujón (dir +1 hacia FACING, -1 al revés). */
    public void kick(int dir, float strength) {
        vel += dir * strength;
        creak(Math.abs(strength));
    }

    private void creak(float strength) {
        if (level == null) {
            return;
        }
        long now = level.getGameTime();
        if (now - lastCreak > 14 && strength > 3F) {
            lastCreak = now;
            float pitch = 0.75F + level.random.nextFloat() * 0.35F;
            level.playLocalSound(worldPosition.getX() + 0.5, worldPosition.getY() + 1, worldPosition.getZ() + 0.5,
                    com.casino.registry.ModSounds.SALOON_DOOR.get(), SoundSource.BLOCKS, 0.55F, pitch, false);
        }
    }

    public void clientTick() {
        if (level == null) {
            return;
        }
        prevAngle = angle;
        Direction f = facing();
        boolean vip = getBlockState().getBlock() instanceof WestBlocks.VipGate;
        int need = vip ? getBlockState().getValue(WestBlocks.VipGate.ACCESS) : 0;
        AABB box = new AABB(worldPosition).inflate(f.getAxis() == Direction.Axis.X ? 0.35 : 0.02, 0, f.getAxis() == Direction.Axis.Z ? 0.35 : 0.02)
                .expandTowards(0, 1, 0);
        boolean someone = false;
        for (Entity e : level.getEntities((Entity) null, box, e -> e.isAlive() && !e.isSpectator())) {
            if (vip && !WestBlocks.VipGate.allowed(e, need)) {
                continue;
            }
            double mv = (e.getX() - e.xo) * f.getStepX() + (e.getZ() - e.zo) * f.getStepZ();
            double side = (e.getX() - worldPosition.getX() - 0.5) * f.getStepX() + (e.getZ() - worldPosition.getZ() - 0.5) * f.getStepZ();
            int dir;
            if (Math.abs(mv) > 0.01) {
                dir = mv > 0 ? 1 : -1;
            } else {
                dir = side > 0 ? 1 : -1;      // parado en el hueco: la hoja queda del lado donde está
            }
            if (Math.abs(target) < 1 || Math.signum(target) == dir) {
                target = dir * OPEN;
            }
            if (Math.abs(mv) > 0.02 && Math.abs(angle) < 30) {
                kick(dir, (float) Math.min(18, Math.abs(mv) * 70));
            }
            someone = true;
        }
        if (someone) {
            holdTicks = 6;
        } else if (holdTicks > 0 && --holdTicks == 0) {
            target = 0;                       // ya pasó: la suelta y regresa sola
        }
        float acc = -K * (angle - target) - DAMP * vel;
        vel += acc;
        angle += vel;
        if (angle > MAX) {
            angle = MAX;
            vel = -vel * 0.35F;
        } else if (angle < -MAX) {
            angle = -MAX;
            vel = -vel * 0.35F;
        }
        if (target == 0 && Math.abs(angle) < 0.05F && Math.abs(vel) < 0.05F) {
            angle = 0;
            vel = 0;
        }
        // al cruzar el centro rápido, rechina
        if (Math.signum(prevAngle) != Math.signum(angle) && Math.abs(vel) > 4F) {
            creak(Math.abs(vel));
        }
    }

    public float angle(float partial) {
        return prevAngle + (angle - prevAngle) * partial;
    }

    @Override
    protected AABB renderBox() {
        return new AABB(worldPosition).inflate(1.0, 1.0, 1.0);
    }

    @Override
    protected void writeData(CompoundTag tag) {
    }

    @Override
    protected void readData(CompoundTag tag) {
    }
}
