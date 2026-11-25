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
 * La máquina de latón de la final (veintiuno): un cepo sobre la mesa donde cada finalista tiene metida la mano izquierda. La prueba
 * ({@code Veintiuno}) dice en qué estado está y cuántas rondas lleva perdidas (los focos rojos); el cliente la dibuja
 * ({@code geo/maquina.geo.json}) y la mueve aquí:
 * <ul>
 * <li>{@link #ABIERTA}: las prensas abiertas (todavía no hay brazo, o ya soltó al ganador).</li>
 * <li>{@link #CERRADA}: atrapa el brazo (las prensas se cierran con un golpe).</li>
 * <li>{@link #APRIETA}: primera ronda perdida: las prensas aprietan (y tiembla todo).</li>
 * <li>{@link #CHISPAS}: segunda: tiembla y la aguja se vuelve loca (las chispas las pone el servidor).</li>
 * <li>{@link #CORTA}: tercera: la guillotina sube un poquito y ¡pum!; aparece la sangre.</li>
 * </ul>
 * No se guarda en el mundo.
 */
public class Maquina extends Entity implements Animated {
    public static final int ABIERTA = 0, CERRADA = 1, APRIETA = 2, CHISPAS = 3, CORTA = 4;
    /** Cuándo baja la guillotina (ticks desde que empieza a cortar). */
    public static final int T_CAE = 12;
    private static final EntityDataAccessor<Integer> ESTADO = SynchedEntityData.defineId(Maquina.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DESDE = SynchedEntityData.defineId(Maquina.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> GOLPES = SynchedEntityData.defineId(Maquina.class, EntityDataSerializers.INT);
    private final AnimHost anim = new AnimHost();

    public Maquina(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
        this.blocksBuilding = false;
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(ESTADO, ABIERTA);
        this.entityData.define(DESDE, 0);
        this.entityData.define(GOLPES, 0);
    }

    public int estado() {
        return entityData.get(ESTADO);
    }

    public int golpes() {
        return entityData.get(GOLPES);
    }

    public void golpes(int g) {
        entityData.set(GOLPES, g);
    }

    public void estado(int e) {
        entityData.set(ESTADO, e);
        entityData.set(DESDE, (int) level().getGameTime());
    }

    /** Ticks desde el último cambio de estado. */
    public float desde(float pt) {
        return level().getGameTime() + pt - entityData.get(DESDE);
    }

    /** ¿Ya bajó la guillotina? */
    public boolean cortado(float pt) {
        return estado() == CORTA && desde(pt) >= T_CAE + 2;
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

    private static float suave(float x) {
        x = Mth.clamp(x, 0F, 1F);
        return x * x * (3 - 2 * x);
    }

    @Override
    public void axPose(AnimHost h, Pose pose, float pt) {
        Geo geo = h.geo();
        float r = (float) Math.PI / 180F;
        float ahora = level().getGameTime() + pt;
        float t = desde(pt);
        int e = estado();
        // las prensas: abiertas (-1 px), cerradas (0) o apretando (+1.3 px hacia el brazo)
        float prensa, temblor = 0F, aguja = -40F, engrane = 0F, cuchilla = 0F;
        switch (e) {
            case CERRADA -> {
                prensa = -1F + suave(t / 4F);
                temblor = t < 6 ? 0.25F * (1F - t / 6F) : 0F;
                aguja = -40F + 20F * suave(t / 6F);
            }
            case APRIETA -> {
                prensa = 0.2F + 1.1F * suave(t / 22F);
                temblor = t < 30 ? 0.35F : 0.08F;
                engrane = Math.min(t, 22F) * 14F;
                aguja = -20F + 70F * suave(t / 22F);
            }
            case CHISPAS -> {
                prensa = 1.3F;
                temblor = t < 40 ? 0.45F : 0.1F;
                aguja = 50F + 60F * (float) Math.sin(ahora * 2.1F) * (t < 40 ? 1F : 0.2F);
                engrane = t * 9F;
            }
            case CORTA -> {
                prensa = 1.3F;
                // la guillotina: sube un poquito (se prepara) y cae de golpe; luego se queda abajo
                if (t < T_CAE - 3) {
                    cuchilla = -1.5F * suave(t / (T_CAE - 3));
                } else if (t < T_CAE) {
                    float c = (t - (T_CAE - 3)) / 3F;
                    cuchilla = -1.5F + 14.5F * c * c;
                } else {
                    cuchilla = 13F;
                }
                temblor = t >= T_CAE && t < T_CAE + 10 ? 0.6F * (1F - (t - T_CAE) / 10F) : 0F;
                aguja = 110F;
            }
            default -> prensa = -1F;
        }
        if (temblor > 0F) {
            pose.addPos(geo.bone("raiz"), temblor * (float) Math.sin(ahora * 7.1F), 0, temblor * (float) Math.cos(ahora * 6.3F));
        }
        pose.addPos(geo.bone("prensa_izq"), prensa, 0, 0);
        pose.addPos(geo.bone("prensa_der"), -prensa, 0, 0);
        pose.addPos(geo.bone("cuchilla"), 0, cuchilla, 0);
        pose.addRot(geo.bone("engrane"), engrane * r, 0, 0);
        pose.addRot(geo.bone("aguja"), aguja * r, 0, 0);
        int g = golpes();
        for (int k = 0; k < 3; k++) {
            pose.hide(geo.bone("luz" + k), k >= g);
        }
        pose.hide(geo.bone("sangre"), !cortado(pt));
    }

    // ------------------------------------------------------------------ como entidad

    @Override
    public AABB getBoundingBoxForCulling() {
        return getBoundingBox().inflate(1, 1.5, 1);
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
