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
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

/**
 * La bailarina de la caja de música gigante (capítulo 6, el piano). La prueba ({@code Piano}) decide qué hace; la entidad solo guarda el
 * estado, desde cuándo (tiempo del mundo) y hacia dónde voltea la cabeza, y el cliente la dibuja ({@code geo/bailarina.geo.json}):
 * <ul>
 * <li>{@link #QUIETA}: de frente, con un vaivén chiquito.</li>
 * <li>{@link #BAILA}: gira despacio sobre su caja (mientras toca la melodía); la llave de cuerda de atrás da vueltas.</li>
 * <li>{@link #MIRA}: se detiene de golpe y voltea la cabeza (solo la cabeza, a tirones) hacia {@link #mira()} grados.</li>
 * </ul>
 */
public class Bailarina extends Entity implements Animated {
    public static final int QUIETA = 0, BAILA = 1, MIRA = 2;
    /** Qué tan grande se dibuja: con su caja, como cinco bloques de alto. */
    public static final float ESCALA = 1.6F;
    private static final EntityDataAccessor<Integer> ESTADO = SynchedEntityData.defineId(Bailarina.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DESDE = SynchedEntityData.defineId(Bailarina.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> MIRA_A = SynchedEntityData.defineId(Bailarina.class, EntityDataSerializers.INT);
    private final AnimHost anim = new AnimHost();

    public Bailarina(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
        this.blocksBuilding = false;
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(ESTADO, QUIETA);
        this.entityData.define(DESDE, 0);
        this.entityData.define(MIRA_A, 0);
    }

    public int estado() {
        return entityData.get(ESTADO);
    }

    public int mira() {
        return entityData.get(MIRA_A);
    }

    public void estado(int e) {
        if (estado() == e) {
            return;
        }
        entityData.set(ESTADO, e);
        entityData.set(DESDE, (int) level().getGameTime());
    }

    /** Se detiene y voltea la cabeza hacia ese punto del mundo. */
    public void mirarA(double x, double z) {
        float yaw = (float) Math.toDegrees(Math.atan2(-(x - getX()), z - getZ()));
        entityData.set(MIRA_A, Math.round(Mth.wrapDegrees(yaw - getYRot())));
        entityData.set(ESTADO, MIRA);
        entityData.set(DESDE, (int) level().getGameTime());
    }

    @Override
    public AnimHost axHost() {
        return anim;
    }

    @Override
    public void axAnimate(AnimHost h, float pt) {
        h.play("base", "idle", 0.3F);
    }

    @Override
    public void axPose(AnimHost h, Pose pose, float pt) {
        Geo geo = h.geo();
        float r = (float) Math.PI / 180F;
        float ahora = level().getGameTime() + pt;
        float t = ahora - entityData.get(DESDE);
        int e = estado();
        float giro = 0F, cabeza = 0F, brazos = 0F, inclina = 0F;
        switch (e) {
            case BAILA -> {
                // arranca despacio y luego gira parejo, con los brazos meciéndose
                float v = Math.min(1F, t / 20F);
                giro = (t * 5.5F * v) % 360F;
                brazos = 6F * (float) Math.sin(ahora * 0.2F);
                pose.addRot(geo.bone("llave"), 0, 0, ahora * 9F * r);
            }
            case MIRA -> {
                // la cabeza voltea a tirones (cuatro jalones) hasta quedar viendo a quien se equivocó
                float meta = mira();
                float paso = Math.min(4F, (float) Math.floor(t / 4F));
                cabeza = meta * paso / 4F + (t < 16 ? 4F * (float) Math.sin(t * 3F) : 0F);
                inclina = 10F * Math.min(1F, t / 16F);
            }
            default -> brazos = 3F * (float) Math.sin(ahora * 0.08F);
        }
        pose.addRot(geo.bone("giro"), 0, giro * r, 0);
        pose.addRot(geo.bone("cabeza"), inclina * r, cabeza * r, 0);
        pose.addRot(geo.bone("brazo_der"), 0, 0, brazos * r);
        pose.addRot(geo.bone("brazo_izq"), 0, 0, -brazos * r);
    }

    @Override
    public AABB getBoundingBoxForCulling() {
        return getBoundingBox().inflate(3, 7, 3).move(0, 3, 0);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double d) {
        return d < 96 * 96;
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
