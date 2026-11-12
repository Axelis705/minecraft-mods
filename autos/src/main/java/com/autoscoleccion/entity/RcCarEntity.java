package com.autoscoleccion.entity;

import com.autoscoleccion.game.CarDef;
import com.autoscoleccion.game.CarStats;
import com.autoscoleccion.game.Cars;
import com.autoscoleccion.server.Driving;
import com.autoscoleccion.server.Races;
import net.minecraft.core.particles.DustParticleOptions;
import org.joml.Vector3f;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;
import java.util.UUID;

/**
 * Un cochecito a control remoto. Lo maneja el cliente de su dueño (como los botes): ese cliente calcula la física y manda la
 * posición al servidor, que la reparte a los demás. No se guarda en el mundo: al salir, alejarse o desconectarse vuelve al garaje.
 */
public class RcCarEntity extends Entity {
    private static final DustParticleOptions EXHAUST = new DustParticleOptions(new Vector3f(0.35F, 0.35F, 0.38F), 0.45F);
    private static final DustParticleOptions TIRE_SMOKE = new DustParticleOptions(new Vector3f(0.86F, 0.86F, 0.88F), 0.7F);
    private static final EntityDataAccessor<Integer> CAR = SynchedEntityData.defineId(RcCarEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Optional<UUID>> OWNER = SynchedEntityData.defineId(RcCarEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Float> STEER = SynchedEntityData.defineId(RcCarEntity.class, EntityDataSerializers.FLOAT);

    /** true en el cliente del dueño mientras maneja. */
    public boolean local;
    /** Auto manejado por la computadora en una carrera (solo en el servidor). */
    public boolean bot;
    /** Los bots van un poco más lento que el máximo (cada uno distinto). */
    public float topMul = 1.0F;
    // lo que pide el jugador (lo pone el cliente antes de cada tick)
    public float throttle;
    public float steerIn;
    public boolean handbrake;
    // estado de la física (solo en el cliente que maneja)
    public float speed;
    public float steer;
    public boolean bumped;
    public boolean drifting;
    // animación de las llantas (en todos los clientes)
    public float spin;
    public float spinO;
    public float steerO;
    // interpolación para los que solo miran
    private int lerpSteps;
    private double lx, ly, lz;
    private float lyaw;

    public RcCarEntity(EntityType<? extends RcCarEntity> type, Level level) {
        super(type, level);
        setMaxUpStep(0.6F);
    }

    @Override
    protected void defineSynchedData() {
        entityData.define(CAR, 0);
        entityData.define(OWNER, Optional.empty());
        entityData.define(STEER, 0.0F);
    }

    public int car() {
        return Mth.clamp(entityData.get(CAR), 0, Cars.COUNT - 1);
    }

    public CarDef def() {
        return Cars.get(car());
    }

    public void setCar(int car) {
        entityData.set(CAR, car);
    }

    public UUID owner() {
        return entityData.get(OWNER).orElse(null);
    }

    public void setOwner(UUID id) {
        entityData.set(OWNER, Optional.ofNullable(id));
    }

    public float syncedSteer() {
        return entityData.get(STEER);
    }

    public void setSyncedSteer(float s) {
        entityData.set(STEER, s);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean canBeCollidedWith() {
        return false;
    }

    @Override
    public boolean isControlledByLocalInstance() {
        return local;
    }

    @Override
    public void lerpTo(double x, double y, double z, float yRot, float xRot, int steps, boolean teleport) {
        lx = x;
        ly = y;
        lz = z;
        lyaw = yRot;
        lerpSteps = 3;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            spinO = spin;
            steerO = steer;
            if (local) {
                drive();
            } else {
                if (lerpSteps > 0) {
                    double k = 1.0 / lerpSteps;
                    setPos(getX() + (lx - getX()) * k, getY() + (ly - getY()) * k, getZ() + (lz - getZ()) * k);
                    setYRot(getYRot() + Mth.wrapDegrees(lyaw - getYRot()) * (float) k);
                    lerpSteps--;
                }
                steer += (syncedSteer() - steer) * 0.5F;
            }
            // las llantas giran según lo que avanzó (hacia adelante o atrás)
            double dx = getX() - xo, dz = getZ() - zo;
            double yr = Math.toRadians(getYRot());
            double fwd = -Math.sin(yr) * dx + Math.cos(yr) * dz;
            float radius = def().style.wheelR / 16.0F * 0.9F;
            spin += (float) (fwd / Math.max(0.02F, radius));
        } else if (bot) {
            if (!Races.botAlive(this)) {
                discard();
                return;
            }
            drive();
            setSyncedSteer(steer);
        } else if (tickCount % 10 == 0 && !Driving.keep(this)) {
            discard();
        }
    }

    /** La física del auto (solo en el cliente del dueño). */
    private void drive() {
        CarStats st = CarStats.of(def());
        float top = st.top * topMul;
        if (throttle > 0) {
            speed += st.accel * topMul * (speed < 0 ? 2.5F : 1.0F) * (1.0F - Math.max(0, speed) / top * 0.6F);
        } else if (throttle < 0) {
            speed -= speed > 0 ? st.accel * 2.6F : st.accel * 0.8F;
        } else {
            speed *= 0.965F;
            if (Math.abs(speed) < 0.004F) {
                speed = 0;
            }
        }
        if (handbrake) {
            speed *= 0.94F;
        }
        if (isInWater()) {
            speed *= 0.85F;
        }
        speed = Mth.clamp(speed, -top * 0.45F, top);
        float frac = Math.min(1.0F, Math.abs(speed) / top);
        float target = steerIn * (1.0F - 0.45F * frac);
        steer += (target - steer) * 0.35F;
        float yawRate = steer * speed * st.turn * (handbrake ? 1.6F : 1.0F);
        setYRot(getYRot() + yawRate);
        double yr = Math.toRadians(getYRot());
        double fx = -Math.sin(yr) * speed, fz = Math.cos(yr) * speed;
        Vec3 v = getDeltaMovement();
        float grip = handbrake ? 0.07F : st.grip;
        double vx = v.x + (fx - v.x) * grip, vz = v.z + (fz - v.z) * grip;
        double vy = onGround() ? -0.08 : Math.max(-1.2, v.y - 0.08);
        drifting = Math.hypot(vx - fx, vz - fz) > 0.12 && Math.abs(speed) > 0.15;
        move(MoverType.SELF, new Vec3(vx, vy, vz));
        bumped = false;
        if (horizontalCollision) {
            if (Math.abs(speed) > 0.12F) {
                bumped = true;
            }
            speed *= -0.25F;
            vx *= 0.2;
            vz *= 0.2;
        }
        setDeltaMovement(vx, onGround() ? 0 : vy, vz);
        // humo del escape y del derrape
        if (throttle > 0 && tickCount % 3 == 0) {
            double bx = getX() + Math.sin(yr) * 0.55, bz = getZ() - Math.cos(yr) * 0.55;
            // humo chiquito (las partículas normales de Minecraft eran enormes junto al auto)
            level().addParticle(EXHAUST, bx, getY() + 0.1, bz, Math.sin(yr) * 0.02, 0.01, -Math.cos(yr) * 0.02);
        }
        if (drifting) {
            for (int side = -1; side <= 1; side += 2) {
                double rx = getX() + Math.sin(yr) * 0.4 + Math.cos(yr) * 0.22 * side, rz = getZ() - Math.cos(yr) * 0.4 + Math.sin(yr) * 0.22 * side;
                level().addParticle(TIRE_SMOKE, rx, getY() + 0.04, rz, 0, 0.005, 0);
            }
        }
    }

    /** El dueño de este auto, si está conectado en este mundo. */
    public Player ownerPlayer() {
        UUID id = owner();
        return id == null ? null : level().getPlayerByUUID(id);
    }
}
