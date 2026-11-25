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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
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
 * La escopeta de la caja de música: una escopeta vieja de dos cañones en la punta de un brazo mecánico que sale de un foso bajo el
 * escenario. La entidad se queda en el fondo del foso; lo que se mueve es el modelo ({@code geo/escopeta.geo.json}): el mástil sube (con
 * el arma parada, apuntando al techo, y dando una vuelta completa al salir), luego el arma baja despacio hacia su víctima (el pitch de la
 * entidad) y gira (el yaw). Carga (los cañones se quiebran y se cierran de golpe), dispara (culatazo y fogonazo) o hace clic en vacío.
 * <p>
 * El servidor decide el estado y a dónde apunta; el cliente lo dibuja suave. No se guarda en el mundo: la prueba la pone y la quita.
 */
public class Escopeta extends Entity implements Animated {
    /** 0 guardada (abajo), 1 subiendo, 2 arriba, 3 bajando. */
    private static final EntityDataAccessor<Integer> ESTADO = SynchedEntityData.defineId(Escopeta.class, EntityDataSerializers.INT);
    /** El tick (de la entidad) en que cambió de estado: el cliente anima desde ahí. */
    private static final EntityDataAccessor<Integer> DESDE = SynchedEntityData.defineId(Escopeta.class, EntityDataSerializers.INT);
    /** Cuántos disparos, cargas y clics en vacío lleva (cuando cambian, el cliente hace la animación). */
    private static final EntityDataAccessor<Integer> DISPAROS = SynchedEntityData.defineId(Escopeta.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> CARGAS = SynchedEntityData.defineId(Escopeta.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> CLICS = SynchedEntityData.defineId(Escopeta.class, EntityDataSerializers.INT);
    /** Cuánto sube el mástil (px) y a qué altura queda el eje del arma arriba del todo (px sobre el fondo del foso). */
    public static final float SUBE = 61F, ALTURA = 78F;
    public static final int TICKS_SUBIR = 130, TICKS_BAJAR = 90;
    /** Qué tan rápido gira (grados por tick): despacio, como un mecanismo de cuerda. */
    public static final float VEL_GIRO = 1.6F, VEL_INCLINA = 1.0F;
    private final AnimHost anim = new AnimHost();
    private float yawObj, pitchObj;
    private boolean girando;
    private int ultimoTic;

    public Escopeta(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
        this.blocksBuilding = false;
        // guardada: apuntando al techo
        setXRot(-90F);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(ESTADO, 0);
        this.entityData.define(DESDE, 0);
        this.entityData.define(DISPAROS, 0);
        this.entityData.define(CARGAS, 0);
        this.entityData.define(CLICS, 0);
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

    public int cargas() {
        return entityData.get(CARGAS);
    }

    public int clics() {
        return entityData.get(CLICS);
    }

    private void estado(int e) {
        entityData.set(ESTADO, e);
        entityData.set(DESDE, tickCount);
    }

    /** Sube del foso (la compuerta ya debe estar abierta). */
    public void subir() {
        if (estado() == 1 || estado() == 2) {
            return;
        }
        setXRot(-90F);
        estado(1);
    }

    /** Vuelve a meterse al foso (primero se endereza apuntando al techo). */
    public void bajar() {
        if (estado() == 0 || estado() == 3) {
            return;
        }
        apuntar(getYRot(), -90F);
        estado(3);
    }

    /** ¿Ya subió del todo? */
    public boolean arriba() {
        return estado() == 2;
    }

    /** ¿Ya está guardada en el foso? */
    public boolean guardada() {
        return estado() == 0;
    }

    /** A dónde apuntar: el giro y la inclinación (grados, como el yaw y el pitch de Minecraft: pitch positivo es hacia abajo). */
    public void apuntar(float yaw, float pitch) {
        yawObj = yaw;
        pitchObj = Mth.clamp(pitch, -90F, 60F);
        girando = true;
    }

    /** Apunta a un punto del mundo (desde el eje del arma, arriba). */
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

    /** El eje del arma cuando está arriba (para apuntar y para los sonidos). */
    public Vec3 canon() {
        return position().add(0, ALTURA / 16.0, 0);
    }

    /** Las bocas de los cañones ahora (hacia donde mira). */
    public Vec3 punta() {
        double r = Math.toRadians(getYRot()), p = Math.toRadians(getXRot());
        double l = 28 / 16.0;
        return canon().add(-Math.sin(r) * Math.cos(p) * l, -Math.sin(p) * l, Math.cos(r) * Math.cos(p) * l);
    }

    /** Los cañones se quiebran y se cierran de golpe (la carga). */
    public void cargar() {
        entityData.set(CARGAS, cargas() + 1);
        sonido(SoundEvents.IRON_TRAPDOOR_CLOSE, 0.9F, 1.5F);
    }

    public void disparar() {
        entityData.set(DISPAROS, disparos() + 1);
    }

    /** Clic: los martillos caen... y no sale nada. */
    public void clic() {
        entityData.set(CLICS, clics() + 1);
    }

    private void sonido(SoundEvent e, float vol, float tono) {
        level().playSound(null, canon().x, canon().y, canon().z, e, SoundSource.HOSTILE, vol, tono);
    }

    private static SoundEvent propio(String id) {
        return SoundEvent.createVariableRangeEvent(new ResourceLocation(com.coleccionista.Coleccionista.MODID, id));
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            return;
        }
        int e = estado(), dt = tickCount - desde();
        if (e == 1) {
            // sube a tirones, con el mecanismo crujiendo
            if (dt % 12 == 0) {
                level().playSound(null, blockPosition().above(2), SoundEvents.PISTON_EXTEND, SoundSource.HOSTILE, 0.7F, 0.45F + random.nextFloat() * 0.1F);
            }
            if (dt % 7 == 0) {
                level().playSound(null, blockPosition().above(2), SoundEvents.CHAIN_STEP, SoundSource.HOSTILE, 0.8F, 0.5F);
            }
            if (dt >= TICKS_SUBIR) {
                estado(2);
                level().playSound(null, blockPosition().above(4), SoundEvents.ANVIL_LAND, SoundSource.HOSTILE, 0.6F, 0.5F);
            }
        } else if (e == 3) {
            if (dt % 14 == 0) {
                level().playSound(null, blockPosition().above(2), SoundEvents.PISTON_CONTRACT, SoundSource.HOSTILE, 0.6F, 0.5F);
            }
            if (dt >= TICKS_BAJAR) {
                estado(0);
            }
        }
        if (girando && e != 1) {
            // gira despacio (como un mecanismo de cuerda), con su tic-tac
            float dy = Mth.wrapDegrees(yawObj - getYRot()), dp = pitchObj - getXRot();
            // al guardarse se endereza rápido (para caber por la compuerta)
            float vi = e == 3 ? 4F : VEL_INCLINA;
            setYRot(getYRot() + Mth.clamp(dy, -VEL_GIRO, VEL_GIRO));
            setXRot(getXRot() + Mth.clamp(dp, -vi, vi));
            if (tickCount - ultimoTic >= 40) {
                ultimoTic = tickCount;
                sonido(propio("caja_tictac"), 1.2F, 1F);
            }
            if (tickCount % 5 == 0) {
                sonido(SoundEvents.DISPENSER_DISPENSE, 0.2F, 1.9F);
            }
            if (Math.abs(dy) <= VEL_GIRO && Math.abs(dp) <= vi) {
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

    /** Qué tan arriba está (0 guardada, 1 arriba), con un rebote al llegar. */
    public float subida(float pt) {
        float t = tickCount + pt - desde();
        return switch (estado()) {
            case 1 -> {
                float x = Mth.clamp(t / TICKS_SUBIR, 0F, 1F);
                // sube a tirones (como engranes que se traban) y rebota un poquito al final
                float base = x * x * (3 - 2 * x);
                float tiron = (float) (Math.sin(x * Math.PI * 9) * 0.02F * (1F - x));
                yield Mth.clamp(base + tiron, 0F, 1.03F);
            }
            case 2 -> 1F + (float) (Math.exp(-t / 6F) * Math.sin(t * 0.9F) * 0.025F);
            case 3 -> {
                float x = Mth.clamp((t - 20F) / (TICKS_BAJAR - 20F), 0F, 1F);
                yield 1F - x * x;
            }
            default -> 0F;
        };
    }

    /** Ticks desde que cambió un contador (el cliente guarda cuándo lo vio cambiar). */
    private static float desdeCambio(AnimHost h, String clave, int valor, float ahora) {
        Object u = h.data.get(clave);
        Object en = h.data.get(clave + "En");
        if (!(u instanceof Integer v) || v != valor) {
            if (u instanceof Integer) {
                h.data.put(clave + "En", ahora);
                en = ahora;
            }
            h.data.put(clave, valor);
        }
        return en instanceof Float f ? ahora - f : 999F;
    }

    @Override
    public void axPose(AnimHost h, Pose pose, float pt) {
        Geo geo = h.geo();
        float r = (float) Math.PI / 180F;
        float sub = subida(pt);
        // el mástil sube y baja (el modelo, no la entidad); los engranes de la base giran con él
        pose.addPos(geo.bone("mastil"), 0, -(1F - Math.min(1F, sub)) * SUBE, 0);
        pose.addRot(geo.bone("engrane"), sub * 720F * r, 0, 0);
        pose.addRot(geo.bone("engrane2"), -sub * 720F * r, 0, 0);
        // al salir del foso da una vuelta completa (despacio, como una bailarina de caja de música)
        if (estado() == 1) {
            float x = Mth.clamp((sub - 0.5F) / 0.5F, 0F, 1F);
            pose.addRot(geo.bone("giro"), 0, (1F - x * x * (3 - 2 * x)) * 360F * r, 0);
        }
        float ahora = tickCount + pt;
        // el culatazo de cada disparo (salta hacia arriba y atrás, y vuelve) y el fogonazo
        float tDisparo = desdeCambio(h, "disparos", disparos(), ahora);
        float kick = tDisparo < 30F ? (float) (Math.exp(-tDisparo / 4F) * 26F) : 0F;
        float pitch = Mth.lerp(pt, xRotO, getXRot());
        pose.addRot(geo.bone("codo"), (pitch - kick) * r, 0, 0);
        pose.addPos(geo.bone("arma"), 0, 0, Math.min(2F, kick / 10F));
        pose.hide(geo.bone("fogonazo"), tDisparo > 2.5F);
        // la carga: los cañones se quiebran hacia abajo y se cierran de golpe
        float tCarga = desdeCambio(h, "cargas", cargas(), ahora);
        float quiebre = 0F;
        if (tCarga < 22F) {
            quiebre = tCarga < 12F ? 32F * Mth.sin(tCarga / 12F * (float) Math.PI / 2F) : 32F * Math.max(0F, 1F - (tCarga - 12F) / 3F);
        }
        pose.addRot(geo.bone("canones"), quiebre * r, 0, 0);
        // los martillos: echados para atrás; caen al disparar o en el clic vacío, y se vuelven a echar para atrás
        float tClic = Math.min(tDisparo, desdeCambio(h, "clics", clics(), ahora));
        float martillo = tClic < 6F ? 0F : tClic < 30F ? -35F * (tClic - 6F) / 24F : -35F;
        pose.addRot(geo.bone("martillos"), martillo * r, 0, 0);
        // la llave de cuerda gira sola (más rápido mientras sube)
        pose.addRot(geo.bone("llave"), ahora * (estado() == 1 ? 14F : 3F) * r, 0, 0);
    }

    // ------------------------------------------------------------------ como entidad

    @Override
    public AABB getBoundingBoxForCulling() {
        return getBoundingBox().inflate(3, 6, 3).move(0, 3, 0);
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
