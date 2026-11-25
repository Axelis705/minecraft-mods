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
 * Una de las dos manos gigantes de porcelana del salón de té (capítulo 5). Salen de la mesa (cada una frente a su jugador, apuntando al
 * rival), se sacuden tres veces ("¡piedra… papel… o tijera!") y a la tercera muestran la jugada. La prueba ({@code Te}) decide qué pasa; la
 * entidad solo guarda el estado, desde cuándo (tiempo del mundo, igual en el servidor y en el cliente) y la jugada, y el cliente la dibuja
 * ({@code geo/mano_gigante.geo.json}):
 * <ul>
 * <li>{@link #GUARDADA}: metida en la mesa (no se ve).</li>
 * <li>{@link #SUBE}: sale de la mesa, empuñada.</li>
 * <li>{@link #LISTA}: empuñada, esperando (respira un poquito).</li>
 * <li>{@link #SACUDE}: tres sacudidas ({@link #T_SACUDIDA} ticks cada una) y luego se abre en la jugada (y se queda así).</li>
 * <li>{@link #BAJA}: se vuelve a meter en la mesa.</li>
 * </ul>
 * No se guarda en el mundo.
 */
public class ManoGigante extends Entity implements Animated {
    public static final int GUARDADA = 0, SUBE = 1, LISTA = 2, SACUDE = 3, BAJA = 4;
    public static final int NADA = 0, PIEDRA = 1, PAPEL = 2, TIJERA = 3;
    /** Cuánto dura cada sacudida y cuánto tarda en salir o en meterse (ticks). */
    public static final int T_SACUDIDA = 12, T_SUBE = 14, T_BAJA = 16;
    /** Cuándo (desde que empieza a sacudirse) ya se ve la jugada. */
    public static final int T_MUESTRA = 3 * T_SACUDIDA;
    /** Qué tan grande se dibuja: la mano mide como un bloque y medio. */
    public static final float ESCALA = 1.7F;
    /** Hacia dónde se doblan los dedos (hacia la palma). */
    private static final float CURVA = -1F;
    private static final EntityDataAccessor<Integer> ESTADO = SynchedEntityData.defineId(ManoGigante.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DESDE = SynchedEntityData.defineId(ManoGigante.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> GESTO = SynchedEntityData.defineId(ManoGigante.class, EntityDataSerializers.INT);
    private final AnimHost anim = new AnimHost();

    public ManoGigante(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
        this.blocksBuilding = false;
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(ESTADO, GUARDADA);
        this.entityData.define(DESDE, 0);
        this.entityData.define(GESTO, NADA);
    }

    public int estado() {
        return entityData.get(ESTADO);
    }

    public int gesto() {
        return entityData.get(GESTO);
    }

    private int ahora() {
        return (int) level().getGameTime();
    }

    public void estado(int e) {
        entityData.set(ESTADO, e);
        entityData.set(DESDE, ahora());
    }

    /** Empieza a sacudirse; a la tercera sacudida muestra {@code gesto}. */
    public void jugar(int gesto) {
        entityData.set(GESTO, gesto);
        estado(SACUDE);
    }

    /** Vuelve a empuñarse (para otra ronda). */
    public void empunar() {
        entityData.set(GESTO, NADA);
        estado(estado() == GUARDADA || estado() == BAJA ? SUBE : LISTA);
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

    /** Qué tanto está afuera de la mesa (0 metida, 1 arriba). El renderizador la baja (en bloques) según esto. */
    public float fuera(float pt) {
        float t = level().getGameTime() + pt - entityData.get(DESDE);
        return switch (estado()) {
            case SUBE -> suave(t / T_SUBE);
            case LISTA, SACUDE -> 1F;
            case BAJA -> 1F - suave(t / T_BAJA);
            default -> 0F;
        };
    }

    private static float suave(float x) {
        x = Mth.clamp(x, 0F, 1F);
        return x * x * (3 - 2 * x);
    }

    @Override
    public void axPose(AnimHost h, Pose pose, float pt) {
        Geo geo = h.geo();
        float r = (float) Math.PI / 180F;
        float t = level().getGameTime() + pt - entityData.get(DESDE);
        int e = estado();
        // qué tan afuera (0: metida en la mesa, 1: arriba), cuánto sube o baja el brazo (grados) y qué tan abierta (0 puño, 1 la jugada)
        float fuera, brazo = 0F, abre = 0F;
        int g = NADA;
        switch (e) {
            case SUBE -> {
                fuera = suave(t / T_SUBE);
                brazo = -12F * (float) Math.sin(Math.min(1F, t / T_SUBE) * Math.PI);
            }
            case LISTA -> {
                fuera = 1F;
                brazo = 2F * (float) Math.sin((level().getGameTime() + pt) * 0.08F);
            }
            case SACUDE -> {
                fuera = 1F;
                if (t < T_MUESTRA) {
                    // arriba y ¡abajo! (cae rápido y rebota), tres veces
                    float c = (t % T_SACUDIDA) / T_SACUDIDA;
                    brazo = c < 0.55F ? -26F * suave(c / 0.55F) : -26F * (1F - suave((c - 0.55F) / 0.2F)) + 3F * (float) Math.sin((c - 0.75F) * 20F);
                } else {
                    float c = (t - T_MUESTRA) / 6F;
                    abre = suave(c);
                    brazo = 4F * (float) Math.sin(Math.min(1F, c) * Math.PI);
                    g = gesto();
                }
            }
            case BAJA -> fuera = 1F - suave(t / T_BAJA);
            default -> fuera = 0F;
        }
        // (lo de meterse en la mesa lo hace el renderizador: baja todo el modelo)
        pose.addRot(geo.bone("brazo"), brazo * r, 0, 0);
        // los dedos: empuñados (piedra) o abiertos según la jugada
        float[] cierra = new float[4];
        boolean pulgarCerrado;
        for (int k = 0; k < 4; k++) {
            boolean abierto = g == PAPEL || g == TIJERA && k < 2;
            cierra[k] = abierto ? 1F - abre : 1F;
        }
        pulgarCerrado = g != PAPEL;
        float pc = pulgarCerrado ? 1F : 1F - abre;
        for (int k = 0; k < 4; k++) {
            pose.addRot(geo.bone("dedo" + k), 0, CURVA * 95F * cierra[k] * r, 0);
            pose.addRot(geo.bone("dedo" + k + "_p"), 0, CURVA * 100F * cierra[k] * r, 0);
        }
        if (g == TIJERA) {
            // la V: el índice un poco arriba y el de en medio un poco abajo
            pose.addRot(geo.bone("dedo0"), -13F * abre * r, 0, 0);
            pose.addRot(geo.bone("dedo1"), 13F * abre * r, 0, 0);
        }
        pose.addRot(geo.bone("pulgar"), 0, CURVA * 50F * pc * r, 0);
        pose.addRot(geo.bone("pulgar_p"), 0, CURVA * 60F * pc * r, 0);
    }

    // ------------------------------------------------------------------ como entidad

    @Override
    public AABB getBoundingBoxForCulling() {
        return getBoundingBox().inflate(2, 2, 2);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double d) {
        return d < 80 * 80;
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
