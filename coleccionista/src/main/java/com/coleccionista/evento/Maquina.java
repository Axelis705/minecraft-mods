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
 * La máquina de latón de la final (veintiuno), chica, sobre la mesa: el antebrazo izquierdo de cada finalista va ENCIMA, sujeto con dos
 * correas, y una guillotina chiquita a la mitad del brazo. La prueba
 * ({@code Veintiuno}) dice en qué estado está y cuántas rondas lleva perdidas (los focos rojos); el cliente la dibuja
 * ({@code geo/maquina.geo.json}) y la mueve aquí:
 * <ul>
 * <li>{@link #ABIERTA}: las correas abiertas, arriba (todavía no hay brazo, o ya soltó al ganador).</li>
 * <li>{@link #CERRADA}: las correas bajan de golpe sobre el brazo.</li>
 * <li>{@link #APRIETA}: primera ronda perdida: las correas aprietan (y tiembla todo).</li>
 * <li>{@link #CHISPAS}: segunda: tiembla y la aguja se vuelve loca (las chispas las pone el servidor).</li>
 * <li>{@link #CORTA}: tercera: la guillotina sube un poquito y ¡pum!; aparece la sangre.</li>
 * </ul>
 * No se guarda en el mundo.
 */
public class Maquina extends Entity implements Animated {
    public static final int ABIERTA = 0, CERRADA = 1, APRIETA = 2, CHISPAS = 3, CORTA = 4;
    /** Cuándo baja la guillotina (ticks desde que empieza a cortar). */
    public static final int T_CAE = 12;
    /** Cuánto baja la hoja (px): del reposo hasta el cojín, atravesando el brazo. */
    public static final float CAIDA = 5.5F;
    private static final EntityDataAccessor<Integer> ESTADO = SynchedEntityData.defineId(Maquina.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DESDE = SynchedEntityData.defineId(Maquina.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> GOLPES = SynchedEntityData.defineId(Maquina.class, EntityDataSerializers.INT);
    /** De quién es el brazo que tiene encima (para dibujar, ya cortado, la mitad que se queda en la máquina con su piel). */
    private static final EntityDataAccessor<java.util.Optional<java.util.UUID>> DUENO = SynchedEntityData.defineId(Maquina.class,
            EntityDataSerializers.OPTIONAL_UUID);
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
        this.entityData.define(DUENO, java.util.Optional.empty());
    }

    public java.util.Optional<java.util.UUID> dueno() {
        return entityData.get(DUENO);
    }

    public void dueno(java.util.UUID id) {
        entityData.set(DUENO, java.util.Optional.ofNullable(id));
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
        // ojo: en Pose.addPos la y positiva va hacia ABAJO. Las correas: abiertas van 4 px arriba del brazo; cerradas, sobre el brazo;
        // apretando, medio px más abajo (se le hunden). La hoja: arriba; al cortar sube un poquito y cae 5.3 px (atraviesa el brazo).
        float correa, temblor = 0F, aguja = -40F, engrane = 0F, cuchilla = 0F;
        switch (e) {
            case CERRADA -> {
                // ¡clac! bajan de golpe y rebotan un poquito
                correa = t < 3 ? -4F * (1F - suave(t / 3F)) : -0.4F * (float) Math.exp(-(t - 3) / 2F) * (float) Math.cos((t - 3) * 1.6F);
                temblor = t < 6 ? 0.2F * (1F - t / 6F) : 0F;
                aguja = -40F + 20F * suave(t / 6F);
            }
            case APRIETA -> {
                correa = 0.55F * suave(t / 18F);
                temblor = t < 30 ? 0.3F : 0.06F;
                engrane = Math.min(t, 22F) * 14F;
                aguja = -20F + 70F * suave(t / 22F);
            }
            case CHISPAS -> {
                correa = 0.6F;
                temblor = t < 40 ? 0.4F : 0.08F;
                aguja = 50F + 60F * (float) Math.sin(ahora * 2.1F) * (t < 40 ? 1F : 0.2F);
                engrane = t * 9F;
            }
            case CORTA -> {
                correa = 0.6F;
                if (t < T_CAE - 3) {
                    cuchilla = -1.2F * suave(t / (T_CAE - 3));
                } else if (t < T_CAE) {
                    float c = (t - (T_CAE - 3)) / 3F;
                    cuchilla = -1.2F + (CAIDA + 1.2F) * c * c;
                } else {
                    cuchilla = CAIDA;
                }
                temblor = t >= T_CAE && t < T_CAE + 10 ? 0.5F * (1F - (t - T_CAE) / 10F) : 0F;
                aguja = 110F;
            }
            default -> correa = -4F;
        }
        if (temblor > 0F) {
            pose.addPos(geo.bone("raiz"), temblor * (float) Math.sin(ahora * 7.1F), 0, temblor * (float) Math.cos(ahora * 6.3F));
        }
        pose.addPos(geo.bone("correa_a"), 0, correa, 0);
        pose.addPos(geo.bone("correa_b"), 0, correa, 0);
        pose.addPos(geo.bone("cuchilla"), 0, cuchilla, 0);
        pose.addRot(geo.bone("engrane"), engrane * r, 0, 0);
        pose.addRot(geo.bone("aguja"), aguja * r, 0, 0);
        int g = golpes();
        for (int k = 0; k < 3; k++) {
            pose.hide(geo.bone("luz" + k), k >= g);
        }
        boolean sangre = cortado(pt);
        pose.hide(geo.bone("sangre"), !sangre);
        pose.hide(geo.bone("sangre_hoja"), !sangre);
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
