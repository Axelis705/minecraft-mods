package com.casino.heist;

import com.casino.plat.CasinoBE;
import com.casino.plat.Nbt;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Cámara de seguridad: gira de un lado a otro (±{@link #SWEEP}°). Durante la parte sigilosa de un robo, si ve a alguien del equipo
 * (de frente, cerca y sin paredes de por medio) sube la sospecha. Agachado se te ve desde más cerca. La luz se pone roja al verte.
 */
public class CameraBE extends CasinoBE {
    public static final float SWEEP = 42F;
    public static final float FOV = 26F;
    public static final double RANGE = 12;
    public static final double RANGE_SNEAK = 6.5;

    /** Sincronizado: si está viendo a alguien (luz roja) y si está apagada (consola hackeada). */
    public boolean seeing;
    public boolean off;
    private int seeLeft;

    public CameraBE(BlockPos pos, BlockState state) {
        super(HeistReg.CAMERA_BE.get(), pos, state);
    }

    public Direction facing() {
        BlockState s = getBlockState();
        return s.hasProperty(HeistBlocks.FACING) ? s.getValue(HeistBlocks.FACING) : Direction.NORTH;
    }

    /** Hacia dónde mira ahora (grados al estilo Minecraft), igual en el servidor y en el cliente. */
    public float yaw(double time) {
        double phase = (worldPosition.getX() * 7 + worldPosition.getZ() * 13) % 160;
        return facing().toYRot() + SWEEP * (float) Math.sin((time + phase) * Math.PI * 2 / 160.0);
    }

    void serverTick() {
        if (level == null || level.getGameTime() % 4 != 0) {
            return;
        }
        HeistManager.Heist h = HeistManager.at(level, worldPosition);
        boolean nowOff = h != null && h.camerasOff(level.getGameTime());
        boolean saw = false;
        if (h != null && !nowOff && h.phase == HeistManager.Phase.STEALTH) {
            Vec3 eye = Vec3.atCenterOf(worldPosition).add(facing().getStepX() * 0.3, -0.1, facing().getStepZ() * 0.3);
            float yaw = yaw(level.getGameTime());
            for (ServerPlayer p : h.crewOnline(level)) {
                if (sees(eye, yaw, p)) {
                    HeistManager.spot(h, p, 7F, eye);
                    saw = true;
                }
            }
        }
        if (saw) {
            seeLeft = 10;
        } else if (seeLeft > 0) {
            seeLeft -= 4;
        }
        boolean s = seeLeft > 0;
        if (s != seeing || nowOff != off) {
            seeing = s;
            off = nowOff;
            sync();
        }
    }

    private boolean sees(Vec3 eye, float yaw, ServerPlayer p) {
        Vec3 t = p.getEyePosition();
        double dx = t.x - eye.x, dz = t.z - eye.z, dy = t.y - eye.y;
        double d = Math.sqrt(dx * dx + dz * dz);
        double r = p.isCrouching() ? RANGE_SNEAK : RANGE;
        if (d > r || Math.abs(dy) > 7 || d < 0.3) {
            return d < 1.2;
        }
        float want = (float) (Mth.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0F;
        if (Math.abs(Mth.wrapDegrees(want - yaw)) > FOV) {
            return false;
        }
        // de arriba hacia abajo: la cámara está en el techo y ve hacia abajo, pero no a través del piso
        return HeistManager.clear(level, eye, t, p);
    }

    public void sync() {
        setChanged();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    protected void writeData(CompoundTag tag) {
        tag.putBoolean("seeing", seeing);
        tag.putBoolean("off", off);
    }

    @Override
    protected void readData(CompoundTag tag) {
        seeing = Nbt.b(tag, "seeing");
        off = Nbt.b(tag, "off");
    }
}
