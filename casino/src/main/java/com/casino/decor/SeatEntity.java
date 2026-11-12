package com.casino.decor;

import com.casino.registry.ModDecor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Asiento invisible: el jugador "monta" esta entidad para quedar sentado en una silla, taburete o sofá. Se borra sola cuando
 * nadie está sentado o el mueble ya no está.
 */
public class SeatEntity extends Entity {
    /** Cuánto queda la cadera del jugador sobre el punto donde está (en la pose de sentado). */
    private static final double HIP = 0.6;

    public SeatEntity(EntityType<? extends SeatEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        setNoGravity(true);
        setInvisible(true);
    }

    /** Sienta al jugador en el mueble de esa posición. Devuelve false si ya hay alguien. */
    public static boolean sit(Level level, BlockPos pos, double seatHeight, Direction front, LivingEntity who) {
        List<SeatEntity> seats = level.getEntitiesOfClass(SeatEntity.class, new net.minecraft.world.phys.AABB(pos));
        for (SeatEntity s : seats) {
            if (s.isVehicle()) {
                return false;
            }
            s.discard();
        }
        SeatEntity seat = new SeatEntity(ModDecor.SEAT.get(), level);
        float yaw = front.toYRot();
        seat.setPos(pos.getX() + 0.5, pos.getY() + seatHeight - HIP, pos.getZ() + 0.5);
        seat.setYRot(yaw);
        seat.yRotO = yaw;
        level.addFreshEntity(seat);
        who.setYRot(yaw);
        who.setYBodyRot(yaw);
        who.setYHeadRot(yaw);
        return who.startRiding(seat);
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide() && (!isVehicle() || !(level().getBlockState(blockPos()).getBlock() instanceof Seat))) {
            ejectPassengers();
            discard();
        }
    }

    /** El bloque debajo de la cadera (el asiento está un poco más abajo que el punto de la cadera). */
    private BlockPos blockPos() {
        return BlockPos.containing(getX(), getY() + HIP - 0.05, getZ());
    }

    @Override
    protected void positionRider(Entity passenger, Entity.MoveFunction move) {
        move.accept(passenger, getX(), getY(), getZ());
        clamp(passenger);
    }

    @Override
    public void onPassengerTurned(Entity passenger) {
        clamp(passenger);
    }

    /** Como en un bote: el cuerpo mira al frente del mueble y la cabeza gira hasta 100 grados. */
    private void clamp(Entity e) {
        e.setYBodyRot(getYRot());
        float d = Mth.wrapDegrees(e.getYRot() - getYRot());
        float c = Mth.clamp(d, -100.0F, 100.0F);
        e.yRotO += c - d;
        e.setYRot(e.getYRot() + c - d);
        e.setYHeadRot(e.getYRot());
    }

    @Override
    public Vec3 getDismountLocationForPassenger(LivingEntity passenger) {
        BlockPos pos = blockPos();
        BlockState st = level().getBlockState(pos);
        Direction front = st.getBlock() instanceof Seat s ? s.front(st) : Direction.NORTH;
        for (BlockPos p : new BlockPos[]{pos.relative(front), pos.relative(front.getClockWise()), pos.relative(front.getCounterClockWise()),
                pos.relative(front.getOpposite()), pos.above()}) {
            Vec3 v = new Vec3(p.getX() + 0.5, p.getY(), p.getZ() + 0.5);
            if (level().noCollision(passenger, passenger.getBoundingBox().move(v.subtract(passenger.position())))
                    && !level().getBlockState(p.below()).isAir()) {
                return v;
            }
        }
        return new Vec3(pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5);
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    //#if MC >= 1.20.5
    //$$ @Override
    //$$ protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder b) {
    //$$ }
    //#else
    @Override
    protected void defineSynchedData() {
    }
    //#endif

    //#if MC >= 1.21.2
    //$$ @Override
    //$$ public boolean hurtServer(net.minecraft.server.level.ServerLevel level, net.minecraft.world.damagesource.DamageSource src, float amount) {
    //$$     return false;
    //$$ }
    //#endif

    //#if MC >= 1.21.6
    //$$ @Override
    //$$ protected void readAdditionalSaveData(net.minecraft.world.level.storage.ValueInput in) {
    //$$ }
    //$$
    //$$ @Override
    //$$ protected void addAdditionalSaveData(net.minecraft.world.level.storage.ValueOutput out) {
    //$$ }
    //#else
    @Override
    protected void readAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
    }

    @Override
    protected void addAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
    }
    //#endif

    /** Lo que tiene que saber el asiento de su mueble. */
    public interface Seat {
        /** Hacia dónde mira quien se sienta. */
        Direction front(BlockState state);
    }
}
