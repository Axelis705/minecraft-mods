package com.coleccionista.evento;

import com.axeliscore.anim.AnimHost;
import com.axeliscore.anim.Animated;
import com.axeliscore.anim.Geo;
import com.axeliscore.anim.Pose;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * El arma de la caja de música: un revólver viejo en la punta de un brazo de madera que baja del techo del escenario, gira despacio hasta
 * apuntarle a una pieza y (si el Coleccionista lo decide) dispara. La entidad se queda en el techo; lo que se mueve es el modelo
 * ({@code geo/arma_techo.geo.json}): el mástil baja, el cañón gira (el giro de la entidad) y se inclina (su pitch).
 * <p>
 * El servidor decide el estado y a dónde apunta; el cliente lo dibuja suave (bajar con un rebote, el culatazo y el fogonazo de cada
 * disparo). No se guarda en el mundo: la prueba la pone y la quita.
 */
public class ArmaTecho extends Entity implements Animated {
    /** 0 arriba (escondida), 1 bajando, 2 abajo, 3 subiendo. */
    private static final EntityDataAccessor<Integer> ESTADO = SynchedEntityData.defineId(ArmaTecho.class, EntityDataSerializers.INT);
    /** El tick (de la entidad) en que cambió de estado: el cliente anima desde ahí. */
    private static final EntityDataAccessor<Integer> DESDE = SynchedEntityData.defineId(ArmaTecho.class, EntityDataSerializers.INT);
    /** Cuántos disparos lleva (cuando cambia, el cliente hace el culatazo y el fogonazo). */
    private static final EntityDataAccessor<Integer> DISPAROS = SynchedEntityData.defineId(ArmaTecho.class, EntityDataSerializers.INT);
    /** Cuánto baja el modelo (bloques) y en cuántos ticks. */
    public static final float BAJA = 5.5F;
    public static final int TICKS_BAJAR = 90, TICKS_SUBIR = 70;
    private final AnimHost anim = new AnimHost();
    private float yawObj, pitchObj;
    private boolean girando;

    public ArmaTecho(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
        this.blocksBuilding = false;
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(ESTADO, 0);
        this.entityData.define(DESDE, 0);
        this.entityData.define(DISPAROS, 0);
    }

    public int estado() {
        return entityData.get(ESTADO);
    }

    public int desde() {
        return entityData.get(DESDE);
    }

    public int disparos() {
        return entityData.get(DISPAROS);
    }

    private void estado(int e) {
        entityData.set(ESTADO, e);
        entityData.set(DESDE, tickCount);
    }

    /** Baja del techo (con crujidos de madera). */
    public void bajar() {
        if (estado() == 1 || estado() == 2) {
            return;
        }
        estado(1);
    }

    public void subir() {
        if (estado() == 0 || estado() == 3) {
            return;
        }
        estado(3);
    }

    /** ¿Ya bajó del todo? */
    public boolean abajo() {
        return estado() == 2;
    }

    /** A dónde apuntar: el giro y la inclinación (grados, como el yaw y el pitch de Minecraft: pitch positivo es hacia abajo). */
    public void apuntar(float yaw, float pitch) {
        yawObj = yaw;
        pitchObj = pitch;
        girando = true;
    }

    /** Apunta a un punto del mundo (desde el cañón). */
    public void apuntarA(Vec3 p) {
        Vec3 c = canon();
        double dx = p.x - c.x, dy = p.y - c.y, dz = p.z - c.z;
        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)));
        apuntar(yaw, pitch);
    }

    /** ¿Ya está apuntando a donde le dijeron? */
    public boolean apuntando() {
        return !girando;
    }

    /** Dónde queda el cañón cuando está abajo (para apuntar y para el fogonazo). */
    public Vec3 canon() {
        return position().add(0, -92 / 16.0, 0);
    }

    /** La punta del cañón ahora (hacia donde mira). */
    public Vec3 punta() {
        double r = Math.toRadians(getYRot()), p = Math.toRadians(getXRot());
        return canon().add(-Math.sin(r) * Math.cos(p) * 0.85, -Math.sin(p) * 0.85, Math.cos(r) * Math.cos(p) * 0.85);
    }

    public void disparar() {
        entityData.set(DISPAROS, disparos() + 1);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            return;
        }
        int e = estado(), dt = tickCount - desde();
        if (e == 1) {
            // crujidos de la madera mientras baja
            if (dt % 14 == 0) {
                level().playSound(null, blockPosition().below(2), SoundEvents.WOODEN_TRAPDOOR_OPEN, SoundSource.HOSTILE, 0.9F,
                        0.4F + random.nextFloat() * 0.15F);
            }
            if (dt % 9 == 0) {
                level().playSound(null, blockPosition(), SoundEvents.CHAIN_STEP, SoundSource.HOSTILE, 0.7F, 0.5F);
            }
            if (dt >= TICKS_BAJAR) {
                estado(2);
                level().playSound(null, blockPosition().below(5), SoundEvents.WOOD_PLACE, SoundSource.HOSTILE, 1.4F, 0.5F);
            }
        } else if (e == 3) {
            if (dt % 16 == 0) {
                level().playSound(null, blockPosition().below(2), SoundEvents.WOODEN_TRAPDOOR_CLOSE, SoundSource.HOSTILE, 0.8F, 0.45F);
            }
            if (dt >= TICKS_SUBIR) {
                estado(0);
            }
        }
        if (girando) {
            // gira despacio (como un mecanismo de cuerda), con un tic-tac
            float dy = Mth.wrapDegrees(yawObj - getYRot()), dp = pitchObj - getXRot();
            float paso = 3.2F;
            setYRot(getYRot() + Mth.clamp(dy, -paso, paso));
            setXRot(getXRot() + Mth.clamp(dp, -1.6F, 1.6F));
            if (tickCount % 6 == 0 && (Math.abs(dy) > 0.5F || Math.abs(dp) > 0.5F)) {
                level().playSound(null, blockPosition().below(5), SoundEvents.DISPENSER_DISPENSE, SoundSource.HOSTILE, 0.25F, 1.8F);
            }
            if (Math.abs(dy) <= 0.5F && Math.abs(dp) <= 0.5F) {
                setYRot(yawObj);
                setXRot(pitchObj);
                girando = false;
            }
        }
    }

    // ------------------------------------------------------------------ cómo se dibuja (cliente)

    @Override
    public AnimHost axHost() {
        return anim;
    }

    @Override
    public void axAnimate(AnimHost h, float pt) {
        h.play("base", "idle", 0.3F);
    }

    /** Qué tan abajo está el mástil (0 arriba, 1 abajo), con un rebote al llegar. */
    public float bajada(float pt) {
        float t = (tickCount + pt - desde());
        return switch (estado()) {
            case 1 -> {
                float x = Mth.clamp(t / TICKS_BAJAR, 0F, 1F);
                // baja a tirones (como soltando cuerda) y rebota un poquito al final
                float base = 1F - (float) Math.pow(1F - x, 2.2F);
                float tiron = (float) (Math.sin(x * Math.PI * 7) * 0.025F * (1F - x));
                yield Mth.clamp(base + tiron, 0F, 1.04F);
            }
            case 2 -> {
                float rebote = (float) (Math.exp(-t / 6F) * Math.sin(t * 0.9F) * 0.03F);
                yield 1F + rebote;
            }
            case 3 -> 1F - Mth.clamp(t / TICKS_SUBIR, 0F, 1F) * Mth.clamp(t / TICKS_SUBIR, 0F, 1F);
            default -> 0F;
        };
    }

    @Override
    public void axPose(AnimHost h, Pose pose, float pt) {
        Geo geo = h.geo();
        float r = (float) Math.PI / 180F;
        // el mástil sube y baja (el modelo, no la entidad): arriba queda metido en el techo
        int mastil = geo.bone("mastil");
        pose.addPos(mastil, 0, (1F - bajada(pt)) * BAJA * 16F, 0);
        // el cañón se inclina hacia la pieza; con cada disparo, el culatazo (salta hacia arriba y atrás, y vuelve)
        float pitch = Mth.lerp(pt, xRotO, getXRot());
        Object ud = h.data.get("disparos");
        Object ut = h.data.get("disparoEn");
        float now = tickCount + pt;
        if (!(ud instanceof Integer u) || u != disparos()) {
            if (ud instanceof Integer) {
                h.data.put("disparoEn", now);
                ut = now;
            }
            h.data.put("disparos", disparos());
        }
        float desdeDisparo = ut instanceof Float f ? now - f : 999F;
        float kick = desdeDisparo < 30F ? (float) (Math.exp(-desdeDisparo / 4F) * 28F) : 0F;
        pose.addRot(geo.bone("codo"), (pitch - kick) * r, 0, 0);
        pose.addPos(geo.bone("arma"), 0, 0, Math.min(1.5F, kick / 12F));
        pose.hide(geo.bone("fogonazo"), desdeDisparo > 2.5F);
    }

    // ------------------------------------------------------------------ como entidad

    @Override
    public AABB getBoundingBoxForCulling() {
        return getBoundingBox().inflate(3, BAJA + 2, 3);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double d) {
        return d < 64 * 64;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean hurt(DamageSource src, float amount) {
        return false;
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag t) {
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag t) {
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return new ClientboundAddEntityPacket(this);
    }
}
