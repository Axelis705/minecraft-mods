package com.casino.west;

import com.casino.chip.Chips;
import com.casino.data.CasinoStats;
import com.casino.game.BullRide;
import com.casino.plat.Net;
import com.casino.plat.Plat;
import com.casino.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * La montada en el toro mecánico: el jugador "monta" esta entidad invisible, que vive en el pivote del toro. En el servidor corre la
 * física ({@link BullRide}) con las teclas que manda el jinete; el resultado (inclinación del jinete, giro, cabeceo y ladeo del toro,
 * tiempo) se sincroniza solo, y con eso el dibujante mueve al toro y la cámara del jinete se mece. Al caer, bajarse (Shift) o domar al
 * toro se paga según el tiempo aguantado.
 */
public class BullRideEntity extends Entity {
    private static final EntityDataAccessor<Float> LEAN = SynchedEntityData.defineId(BullRideEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> ROLL = SynchedEntityData.defineId(BullRideEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> PITCH = SynchedEntityData.defineId(BullRideEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> SPIN = SynchedEntityData.defineId(BullRideEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> CLOCK = SynchedEntityData.defineId(BullRideEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> BET = SynchedEntityData.defineId(BullRideEntity.class, EntityDataSerializers.INT);

    /** Altura del pivote sobre la base del bloque y del asiento sobre el pivote. */
    public static final double PIVOT = 0.62;
    private static final double SADDLE = 0.80;
    /** Cuánto queda la cadera sobre los pies del jugador sentado. */
    private static final double HIP = 0.6;

    // servidor
    private BullRide ride;
    private int input;
    private boolean ended;
    private int lastTier = -1;
    private int lastMove;
    private BlockPos bull = BlockPos.ZERO;
    /** Quién iba montado (para pagarle aunque ya se haya bajado). */
    private java.util.UUID lastRider;
    // cliente (y servidor): valores del tick anterior, para suavizar el dibujo
    public float pLean, pRoll, pPitch, pSpin;
    private float spinApplied;
    private boolean spinInit;

    public BullRideEntity(EntityType<? extends BullRideEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        setNoGravity(true);
        setInvisible(true);
    }

    /** Arranca una montada (servidor): el jugador ya pagó. */
    public static boolean start(ServerPlayer p, BlockPos bullPos, int bet) {
        Level level = p.level();
        BullRideEntity e = Plat.create(WestReg.BULL_RIDE.get(), level);
        if (e == null) {
            return false;
        }
        e.bull = bullPos.immutable();
        e.lastRider = p.getUUID();
        e.ride = new BullRide(level.getRandom().nextLong());
        e.entityData.set(BET, bet);
        Direction f = facing(level.getBlockState(bullPos));
        e.setPos(bullPos.getX() + 0.5, bullPos.getY() + PIVOT, bullPos.getZ() + 0.5);
        e.setYRot(f.toYRot());
        e.yRotO = f.toYRot();
        level.addFreshEntity(e);
        if (p.isPassenger()) {
            p.stopRiding();
        }
        p.setYRot(f.toYRot());
        p.setYHeadRot(f.toYRot());
        if (!p.startRiding(e)) {
            e.discard();
            return false;
        }
        level.playSound(null, bullPos, ModSounds.BULL_MOTOR.get(), SoundSource.BLOCKS, 0.7F, 0.7F);
        return true;
    }

    private static Direction facing(BlockState s) {
        return s.hasProperty(WestBlocks.FACING) ? s.getValue(WestBlocks.FACING) : Direction.SOUTH;
    }

    // ------------------------------------------------------------------ lo que se ve

    public float lean(float pt) {
        return pLean + (entityData.get(LEAN) - pLean) * pt;
    }

    public float roll(float pt) {
        return pRoll + (entityData.get(ROLL) - pRoll) * pt;
    }

    public float pitchDeg(float pt) {
        return pPitch + (entityData.get(PITCH) - pPitch) * pt;
    }

    public float spin(float pt) {
        return pSpin + (entityData.get(SPIN) - pSpin) * pt;
    }

    /** Ticks desde que se subió (incluye la cuenta regresiva). */
    public int clock() {
        return entityData.get(CLOCK);
    }

    /** Ticks aguantados en el toro (sin la cuenta regresiva). */
    public int rideTicks() {
        return Math.max(0, clock() - BullRide.COUNTDOWN);
    }

    public int bet() {
        return entityData.get(BET);
    }

    public void setInput(int dir) {
        this.input = Integer.signum(dir);
    }

    /** El bloque del toro debajo (lo busca en el cliente por la posición). */
    public BlockPos bullPos() {
        return ended || ride != null ? bull : BlockPos.containing(getX(), getY() - PIVOT + 0.01, getZ());
    }

    // ------------------------------------------------------------------ cada tick

    @Override
    public void tick() {
        pLean = entityData.get(LEAN);
        pRoll = entityData.get(ROLL);
        pPitch = entityData.get(PITCH);
        pSpin = entityData.get(SPIN);
        super.tick();
        if (level().isClientSide()) {
            if (isVehicle()) {
                Net.onClient(() -> com.casino.client.WestClient.bullTick(this));
            }
            return;
        }
        if (ended) {
            discard();
            return;
        }
        if (ride == null || !(level().getBlockState(bull).getBlock() instanceof WestBlocks.Bull)) {
            finish(Reason.BROKEN);
            return;
        }
        Entity rider = getFirstPassenger();
        if (rider == null) {
            finish(Reason.CASHOUT);
            return;
        }
        int clock = clock() + 1;
        entityData.set(CLOCK, clock);
        if (clock < BullRide.COUNTDOWN) {
            if (clock % 20 == 0) {
                level().playSound(null, bull, SoundEvents.NOTE_BLOCK_HAT.value(), SoundSource.BLOCKS, 0.8F, 1.2F);
            }
            return;
        }
        if (clock == BullRide.COUNTDOWN) {
            level().playSound(null, bull, ModSounds.BULL_BELL.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
            level().playSound(null, bull, ModSounds.BULL_MOTOR.get(), SoundSource.BLOCKS, 1.0F, 1.1F);
        }
        boolean ok = ride.step(input);
        entityData.set(LEAN, (float) ride.lean);
        entityData.set(ROLL, (float) ride.roll);
        entityData.set(PITCH, (float) ride.pitch);
        entityData.set(SPIN, (float) ride.spin);
        if (ride.move != BullRide.MOVE_NONE && lastMove == BullRide.MOVE_NONE) {
            level().playSound(null, bull, ModSounds.BULL_MOTOR.get(), SoundSource.BLOCKS, 0.55F, 0.85F + level().getRandom().nextFloat() * 0.4F);
        }
        lastMove = ride.move;
        int tier = BullRide.tier(ride.t);
        if (tier > lastTier) {
            lastTier = tier;
            level().playSound(null, bull, ModSounds.BULL_BELL.get(), SoundSource.BLOCKS, 0.9F, 1.0F + tier * 0.06F);
            if (rider instanceof ServerPlayer sp) {
                Plat.msg(sp, Component.translatable("casino.bull.tier", BullRide.TIER_TICKS[tier] / 20, x(BullRide.TIER_X10[tier]))
                        .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), true);
            }
        }
        if (!ok) {
            finish(Reason.THROWN);
        } else if (ride.tamed()) {
            finish(Reason.TAMED);
        }
    }

    enum Reason { THROWN, CASHOUT, TAMED, BROKEN }

    private static String x(int x10) {
        return x10 % 10 == 0 ? "x" + x10 / 10 : "x" + x10 / 10 + "." + x10 % 10;
    }

    private void finish(Reason why) {
        if (ended) {
            return;
        }
        ended = true;
        Entity rider = getFirstPassenger();
        int bet = bet();
        int t = ride == null ? 0 : ride.t;
        boolean countdown = clock() < BullRide.COUNTDOWN;
        ServerPlayer sp = rider instanceof ServerPlayer s ? s : null;
        if (sp == null) {
            // ya se bajó: buscamos al jugador que estaba montado (sigue en línea)
            sp = lastRider != null && Plat.server(this) != null ? Plat.server(this).getPlayerList().getPlayer(lastRider) : null;
        }
        if (why == Reason.THROWN && rider != null) {
            throwRider(rider);
        } else {
            ejectPassengers();
        }
        if (sp == null) {
            discard();
            return;
        }
        if (countdown || why == Reason.BROKEN) {
            // no alcanzó a arrancar: se le devuelve la apuesta
            Chips.give(sp, bet);
            Plat.msg(sp, Component.translatable("casino.bull.refund", bet).withStyle(ChatFormatting.YELLOW), false);
            discard();
            return;
        }
        long pay = BullRide.payout(bet, t);
        if (pay > 0) {
            Chips.give(sp, (int) pay);
        }
        CasinoStats.play(sp, CasinoStats.BULL, bet, pay);
        String secs = String.format(java.util.Locale.ROOT, "%.1f", t / 20.0);
        Component head = switch (why) {
            case THROWN -> Component.translatable("casino.bull.thrown", secs);
            case TAMED -> Component.translatable("casino.bull.tamed", secs);
            default -> Component.translatable("casino.bull.cashout", secs);
        };
        Component tail = pay > 0 ? Component.translatable("casino.bull.won", pay, x(BullRide.payoutX10(t))).withStyle(ChatFormatting.GREEN)
                : Component.translatable("casino.bull.lost", bet).withStyle(ChatFormatting.RED);
        Plat.msg(sp, head.copy().withStyle(why == Reason.TAMED ? ChatFormatting.GOLD : ChatFormatting.YELLOW).append(" ").append(tail), false);
        Plat.msg(sp, tail, true);
        if (why == Reason.TAMED) {
            level().playSound(null, bull, SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0F, 0.9F);
        }
        if (level().getBlockEntity(bull) instanceof BullBE be) {
            be.offer(sp, t);
        }
        discard();
    }

    private void throwRider(Entity rider) {
        double yaw = Math.toRadians(getYRot() + spin(1F));
        // derecha del toro = (-cos, sin) en x/z con el giro de Minecraft
        double rx = -Math.cos(yaw), rz = -Math.sin(yaw);
        double side = Math.signum(entityData.get(LEAN));
        if (side == 0) {
            side = 1;
        }
        ejectPassengers();
        Vec3 v = new Vec3(rx * side * 0.55, 0.42, rz * side * 0.55);
        rider.setDeltaMovement(v);
        rider.resetFallDistance();
        if (rider instanceof ServerPlayer sp) {
            sp.connection.send(new net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket(sp));
        }
        level().playSound(null, bull, SoundEvents.HORSE_ANGRY, SoundSource.BLOCKS, 0.6F, 0.7F);
    }

    // ------------------------------------------------------------------ montar

    @Override
    protected void positionRider(Entity passenger, Entity.MoveFunction move) {
        if (passenger instanceof net.minecraft.world.entity.player.Player) {
            lastRider = passenger.getUUID();
        }
        Vec3 seat = seat(1F);
        move.accept(passenger, seat.x, seat.y - HIP, seat.z);
        // el cuerpo gira con el toro (como en un bote)
        float s = entityData.get(SPIN);
        if (!spinInit) {
            spinApplied = s;
            spinInit = true;
        }
        float d = s - spinApplied;
        spinApplied = s;
        if (d != 0) {
            passenger.setYRot(passenger.getYRot() + d);
            passenger.setYHeadRot(passenger.getYHeadRot() + d);
        }
        if (passenger instanceof LivingEntity le) {
            le.setYBodyRot(getYRot() + s);
        }
    }

    /** Punto de la silla en el mundo, con el toro girado, cabeceando y ladeado, y el jinete inclinado hacia un lado. */
    public Vec3 seat(float pt) {
        double yaw = Math.toRadians(getYRot() + spin(pt));
        double roll = Math.toRadians(roll(pt));
        double pitch = Math.toRadians(pitchDeg(pt));
        // local: x a la derecha del toro, y arriba, z hacia la cabeza
        double lx = lean(pt) * 0.30, ly = SADDLE, lz = -0.05;
        // ladeo (alrededor de z)
        double x1 = lx * Math.cos(roll) - ly * Math.sin(roll);
        double y1 = lx * Math.sin(roll) + ly * Math.cos(roll);
        double z1 = lz;
        // cabeceo (alrededor de x): nariz arriba = pitch positivo
        double y2 = y1 * Math.cos(pitch) + z1 * Math.sin(pitch);
        double z2 = -y1 * Math.sin(pitch) + z1 * Math.cos(pitch);
        double x2 = x1;
        // giro (Minecraft: yaw 0 = mira a +z; la derecha es -x)
        double sin = Math.sin(yaw), cos = Math.cos(yaw);
        double fx = -sin, fz = cos;     // frente
        double rx = -cos, rz = -sin;    // derecha
        return new Vec3(getX() + rx * x2 + fx * z2, getY() + y2, getZ() + rz * x2 + fz * z2);
    }

    @Override
    public Vec3 getDismountLocationForPassenger(LivingEntity passenger) {
        Direction f = Direction.fromYRot(getYRot());
        BlockPos b = bullPos();
        for (Direction d : new Direction[]{f.getClockWise(), f.getCounterClockWise(), f.getOpposite(), f}) {
            BlockPos p = b.relative(d, 2);
            Vec3 v = new Vec3(p.getX() + 0.5, p.getY(), p.getZ() + 0.5);
            if (level().noCollision(passenger, passenger.getBoundingBox().move(v.subtract(passenger.position())))) {
                return v;
            }
        }
        return new Vec3(b.getX() + 0.5, b.getY() + 1.6, b.getZ() + 0.5);
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    //#if MC >= 1.20.5
    //$$ @Override
    //$$ protected void defineSynchedData(SynchedEntityData.Builder b) {
    //$$     b.define(LEAN, 0F);
    //$$     b.define(ROLL, 0F);
    //$$     b.define(PITCH, 0F);
    //$$     b.define(SPIN, 0F);
    //$$     b.define(CLOCK, 0);
    //$$     b.define(BET, 0);
    //$$ }
    //#else
    @Override
    protected void defineSynchedData() {
        entityData.define(LEAN, 0F);
        entityData.define(ROLL, 0F);
        entityData.define(PITCH, 0F);
        entityData.define(SPIN, 0F);
        entityData.define(CLOCK, 0);
        entityData.define(BET, 0);
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
}
