package com.coleccionista.evento;

import com.axeliscore.anim.AnimHost;
import com.axeliscore.anim.Animated;
import com.axeliscore.anim.Geo;
import com.axeliscore.anim.Pose;
import net.minecraft.core.BlockPos;
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
import net.minecraft.world.phys.Vec3;

/**
 * La caja sorpresa gigante del capítulo 4, con Don Resorte adentro. La prueba ({@code Sorpresa}) decide qué pasa; la entidad solo guarda
 * el estado (y desde cuándo) y cuántas vueltas lleva la manivela, y el cliente lo dibuja ({@code geo/caja_sorpresa.geo.json}):
 * <ul>
 * <li>{@link #CERRADA}: quieta (Don Resorte aplastado adentro: su cabeza gigante no tiene cómo caber).</li>
 * <li>{@link #SALTO}: la tapa revienta, el resorte se estira, la cabeza se infla y rebota; los brazos se agitan.</li>
 * <li>{@link #AGARRA}: se inclina hacia quien le dio cuerda, lo agarra con los guantes y abre la boca enorme.</li>
 * <li>{@link #MUERDE}: la mordida (la quijada se cierra de golpe) y luego mastica, sacudiendo la cabeza.</li>
 * <li>{@link #ESCUPE}: abre la boca y escupe los zapatos (vuelan hasta su lugar en el escenario).</li>
 * <li>{@link #GUARDA}: se vuelve a meter (aplastándose) y la tapa se cierra de golpe.</li>
 * </ul>
 * El frente de la caja (con la manivela) mira hacia donde mira la entidad (su yaw). No se guarda en el mundo.
 */
public class CajaSorpresa extends Entity implements Animated {
    public static final int CERRADA = 0, SALTO = 1, AGARRA = 2, MUERDE = 3, ESCUPE = 4, GUARDA = 5;
    /** Cuánto dura cada parte (ticks). */
    public static final int T_SALTO = 34, T_AGARRA = 28, T_MUERDE = 80, T_ESCUPE = 34, T_GUARDA = 30;
    /** Cuánto se estira el resorte (px) y cuántos ticks tarda una vuelta de manivela. */
    public static final float ESTIRA = 36F;
    public static final int TICKS_VUELTA = 30;
    private static final EntityDataAccessor<Integer> ESTADO = SynchedEntityData.defineId(CajaSorpresa.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DESDE = SynchedEntityData.defineId(CajaSorpresa.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> VUELTAS = SynchedEntityData.defineId(CajaSorpresa.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> VUELTA_EN = SynchedEntityData.defineId(CajaSorpresa.class, EntityDataSerializers.INT);
    /** A dónde vuelan los zapatos (relativo a la caja, en bloques x10) cuando escupe. */
    private static final EntityDataAccessor<BlockPos> ZAPATOS = SynchedEntityData.defineId(CajaSorpresa.class, EntityDataSerializers.BLOCK_POS);
    private final AnimHost anim = new AnimHost();

    public CajaSorpresa(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
        this.blocksBuilding = false;
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(ESTADO, CERRADA);
        this.entityData.define(DESDE, 0);
        this.entityData.define(VUELTAS, 0);
        this.entityData.define(VUELTA_EN, -1000);
        this.entityData.define(ZAPATOS, BlockPos.ZERO);
    }

    public int estado() {
        return entityData.get(ESTADO);
    }

    public int desde() {
        return entityData.get(DESDE);
    }

    /** Ticks desde que cambió de estado. */
    public int dt() {
        return tickCount - desde();
    }

    public void estado(int e) {
        entityData.set(ESTADO, e);
        entityData.set(DESDE, tickCount);
    }

    public int vueltas() {
        return entityData.get(VUELTAS);
    }

    /** Una vuelta más de la manivela (el cliente la gira despacio, con su música). */
    public void vuelta() {
        entityData.set(VUELTAS, vueltas() + 1);
        entityData.set(VUELTA_EN, tickCount);
    }

    /** A dónde caen los zapatos (un punto del mundo): se guarda relativo a la caja. */
    public void zapatosA(Vec3 p) {
        Vec3 d = p.subtract(position());
        entityData.set(ZAPATOS, new BlockPos((int) Math.round(d.x * 10), (int) Math.round(d.y * 10), (int) Math.round(d.z * 10)));
    }

    public Vec3 zapatos() {
        BlockPos b = entityData.get(ZAPATOS);
        return position().add(b.getX() / 10.0, b.getY() / 10.0, b.getZ() / 10.0);
    }

    /** Hacia el frente (donde está la manivela y se para quien le da cuerda), en el mundo. */
    public Vec3 frente() {
        double r = Math.toRadians(getYRot());
        return new Vec3(-Math.sin(r), 0, Math.cos(r));
    }

    /** Dónde queda la boca de Don Resorte (arriba de la caja, un poco al frente), para la sangre y para llevar ahí a la víctima. */
    public Vec3 boca() {
        return position().add(frente().scale(1.0)).add(0, (34 + ESTIRA) / 16.0 + 0.2, 0);
    }

    @Override
    public void tick() {
        super.tick();
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

    /** Un rebote amortiguado: de 0 a 1 con sobrepaso (para el resorte y la cabeza). */
    private static float resorte(float t, float dura) {
        if (t <= 0) {
            return 0F;
        }
        float x = t / dura;
        return 1F - (float) (Math.exp(-x * 5.5) * Math.cos(x * 16));
    }

    @Override
    public void axPose(AnimHost h, Pose pose, float pt) {
        Geo geo = h.geo();
        float r = (float) Math.PI / 180F;
        float t = tickCount + pt - desde();
        int e = estado();
        // qué tan afuera está (0 guardado, 1 afuera), la tapa (grados, negativo abre), la quijada, los brazos y la inclinación
        float fuera, tapa, quijada = 0F, brazos = 0F, abrazos = 0F, inclina = 0F, sacude = 0F, escala;
        switch (e) {
            case SALTO -> {
                tapa = t < 4 ? -125F * suave(t / 4F) : -118F - 7F * (float) Math.cos((t - 4) * 0.6F) * (float) Math.exp(-(t - 4) / 6F);
                fuera = t < 2 ? 0F : resorte(t - 2, 26F);
                escala = 0.45F + 0.55F * resorte(t - 3, 24F);
                quijada = 18F + 14F * (float) Math.sin(t * 0.9F) * (float) Math.exp(-t / 14F);
                brazos = -40F + 30F * (float) Math.sin(t * 1.3F);
                abrazos = 25F * (float) Math.sin(t * 1.1F);
            }
            case AGARRA -> {
                tapa = -118F;
                fuera = 1F;
                escala = 1F;
                float x = suave(t / 10F);
                brazos = -40F - 55F * x;
                abrazos = 10F * (1F - x);
                inclina = 18F * suave((t - 6) / 14F);
                quijada = 18F + 52F * suave((t - 8) / 14F);
            }
            case MUERDE -> {
                tapa = -118F;
                fuera = 1F;
                escala = 1F;
                inclina = 18F - 8F * suave(t / 20F);
                brazos = -95F + 40F * suave(t / 20F);
                // ¡ñam! se cierra de golpe; luego mastica (abre y cierra) sacudiendo la cabeza
                if (t < 2) {
                    quijada = 70F * (1F - t / 2F);
                } else {
                    float c = (t - 2) / 9F;
                    quijada = 6F + 22F * (0.5F + 0.5F * (float) Math.sin(c * Math.PI * 2 - Math.PI / 2));
                    sacude = 7F * (float) Math.sin(t * 0.7F) * (1F - suave((t - 60) / 20F));
                }
            }
            case ESCUPE -> {
                tapa = -118F;
                fuera = 1F;
                escala = 1F;
                quijada = t < 6 ? 45F * suave(t / 6F) : 45F - 30F * suave((t - 10) / 14F);
                inclina = t < 6 ? -8F * suave(t / 6F) : -8F + 22F * suave((t - 6) / 4F) - 14F * suave((t - 12) / 12F);
                brazos = -30F;
            }
            case GUARDA -> {
                float x = suave(t / 20F);
                fuera = 1F - x;
                escala = 1F - 0.55F * x;
                tapa = t < 20 ? -118F : -118F * (1F - suave((t - 20) / 4F));
                quijada = 10F * (1F - x);
                brazos = -30F - 60F * x;
            }
            default -> {
                tapa = 0F;
                fuera = 0F;
                escala = 0.45F;
                // guardado: los brazos doblados hacia el frente (si no, los guantes se saldrían por abajo de la caja)
                brazos = -90F;
            }
        }
        pose.addRot(geo.bone("tapa"), tapa * r, 0, 0);
        // el payaso sube con el resorte; guardado va hundido (y la cabeza aplastada)
        float hund = 9F;
        pose.addPos(geo.bone("payaso"), 0, fuera * ESTIRA - hund * (1F - fuera), 0);
        for (int k = 0; k < 8; k++) {
            pose.addPos(geo.bone("anillo" + k), 0, fuera * ESTIRA * (k + 1) / 8F, 0);
        }
        int cab = geo.bone("cabeza");
        pose.mulScale(cab, escala, escala * (e == CERRADA ? 0.8F : 1F), escala);
        pose.addRot(cab, 0, 0, sacude * r);
        pose.addRot(geo.bone("cuerpo"), inclina * r, 0, 0);
        pose.addRot(geo.bone("mandibula"), quijada * r, 0, 0);
        pose.addRot(geo.bone("brazo_der"), brazos * r, 0, -abrazos * r);
        pose.addRot(geo.bone("brazo_izq"), brazos * r, 0, abrazos * r);
        pose.mulScale(geo.bone("brazo_der"), escala, escala, escala);
        pose.mulScale(geo.bone("brazo_izq"), escala, escala, escala);
        // la manivela: cada vuelta, un giro completo despacio
        float desdeVuelta = tickCount + pt - entityData.get(VUELTA_EN);
        float giro = (vueltas() - 1 + suave(desdeVuelta / TICKS_VUELTA)) * 360F;
        pose.addRot(geo.bone("manivela"), 0, 0, giro * r);
        // mientras le dan cuerda, la caja tiembla un poquito (algo se mueve adentro)
        if (e == CERRADA && desdeVuelta < TICKS_VUELTA) {
            float tiembla = (float) Math.sin((tickCount + pt) * 2.3F) * 0.6F;
            pose.addRot(geo.bone("caja"), 0, 0, tiembla * r);
            pose.addRot(geo.bone("tapa"), tiembla * r, 0, 0);
        }
        if (e == MUERDE) {
            pose.addRot(geo.bone("caja"), 0, 0, sacude * 0.15F * r);
        }
    }

    // ------------------------------------------------------------------ como entidad

    @Override
    public AABB getBoundingBoxForCulling() {
        return getBoundingBox().inflate(4, 7, 4).move(0, 3, 0);
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
