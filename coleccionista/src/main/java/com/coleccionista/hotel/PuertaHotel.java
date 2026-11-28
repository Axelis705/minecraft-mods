package com.coleccionista.hotel;

import com.axeliscore.anim.AnimHost;
import com.axeliscore.anim.Animated;
import com.axeliscore.anim.Geo;
import com.axeliscore.anim.Pose;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * Una puerta de servicio del hotel: la hoja vieja de metal verde que se abre despacio, rechinando. El hueco tiene barrera (choca y se
 * pica con clic derecho, ver {@link JuegoHotel}); la entidad solo la dibuja y la anima. Mira hacia el pasillo (el frente del modelo, con
 * la manija y la placa, queda de ese lado) y abre hacia adentro del clóset.
 * <p>
 * Según lo que haya detrás (lo decide el servidor en cada partida), se le nota un detalle: las de la trampa tienen un hilo de nylon que
 * sube a la manija, una mancha en el piso y la placa chueca; la de la salida deja pasar luz de colores por abajo.
 */
public class PuertaHotel extends net.minecraft.world.entity.Entity implements Animated {
    /** 0 cerrada, 1 abriéndose (despacio), 2 abierta, 3 abriéndose de golpe, 4 cerrándose (portazo). */
    private static final EntityDataAccessor<Integer> ESTADO = SynchedEntityData.defineId(PuertaHotel.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DESDE = SynchedEntityData.defineId(PuertaHotel.class, EntityDataSerializers.INT);
    /** Qué hay detrás: 0 un clóset, 1 la trampa, 2 la salida. */
    private static final EntityDataAccessor<Integer> TIPO = SynchedEntityData.defineId(PuertaHotel.class, EntityDataSerializers.INT);
    public static final int NORMAL = 0, TRAMPA = 1, SALIDA = 2;
    public static final int CERRADA = 0, ABRIENDO = 1, ABIERTA = 2, DE_GOLPE = 3, CERRANDO = 4;
    /** Cuánto tarda en abrirse (ticks): despacio y de golpe. */
    public static final int TICKS_ABRIR = 32, TICKS_GOLPE = 10, TICKS_CERRAR = 18;
    /** El número de la puerta (hotel.serv.N), para encontrarla. */
    public int numero = -1;
    private final AnimHost anim = new AnimHost();

    public PuertaHotel(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
        this.blocksBuilding = false;
    }

    @Override
    protected void defineSynchedData() {
        entityData.define(ESTADO, CERRADA);
        entityData.define(DESDE, 0);
        entityData.define(TIPO, NORMAL);
    }

    public int estado() {
        return entityData.get(ESTADO);
    }

    public int tipo() {
        return entityData.get(TIPO);
    }

    public void tipo(int t) {
        entityData.set(TIPO, t);
    }

    public int desde() {
        return entityData.get(DESDE);
    }

    private void estado(int e) {
        entityData.set(ESTADO, e);
        entityData.set(DESDE, tickCount);
    }

    public boolean cerrada() {
        return estado() == CERRADA;
    }

    /** Ticks desde el último cambio de estado. */
    public int hace() {
        return tickCount - desde();
    }

    public void abrir(boolean deGolpe) {
        if (estado() == ABRIENDO || estado() == DE_GOLPE || estado() == ABIERTA) {
            return;
        }
        estado(deGolpe ? DE_GOLPE : ABRIENDO);
    }

    public void cerrar() {
        if (estado() == CERRADA || estado() == CERRANDO) {
            return;
        }
        estado(CERRANDO);
    }

    /** La deja cerrada sin animar (al preparar una partida). */
    public void reiniciar(int tipo) {
        tipo(tipo);
        estado(CERRADA);
    }

    @Override
    public void tick() {
        super.tick();
        int e = estado(), dt = hace();
        if (level().isClientSide) {
            // la salida: de vez en cuando, polvito de colores que se cuela por abajo
            if (tipo() == SALIDA && e == CERRADA && random.nextInt(9) == 0) {
                Vec3 f = frente(0.25);
                int[] c = {0xFF3A30, 0xFFD030, 0x40E050, 0x3A8AFF};
                int k = c[random.nextInt(4)];
                Vector3f col = new Vector3f((k >> 16 & 255) / 255F, (k >> 8 & 255) / 255F, (k & 255) / 255F);
                double lado = (random.nextDouble() - 0.5) * 0.8;
                double r = Math.toRadians(getYRot());
                level().addParticle(new DustParticleOptions(col, 0.6F), f.x + Math.cos(r) * lado, getY() + 0.04, f.z + Math.sin(r) * lado,
                        0, 0.01, 0);
            }
            return;
        }
        if (e == ABRIENDO && dt >= TICKS_ABRIR || e == DE_GOLPE && dt >= TICKS_GOLPE) {
            estado(ABIERTA);
        } else if (e == CERRANDO && dt >= TICKS_CERRAR) {
            estado(CERRADA);
        }
    }

    /** Un punto en el piso del lado del pasillo, a {@code d} bloques de la puerta. */
    public Vec3 frente(double d) {
        double r = Math.toRadians(getYRot());
        return position().add(-Math.sin(r) * d, 0, Math.cos(r) * d);
    }

    // ------------------------------------------------------------------ cómo se dibuja (cliente)

    @Override
    public AnimHost axHost() {
        return anim;
    }

    @Override
    public void axAnimate(AnimHost h, float pt) {
        String clip = switch (estado()) {
            case ABRIENDO -> "abrir";
            case DE_GOLPE -> "abrir_rapido";
            case ABIERTA -> "abierta";
            case CERRANDO -> "cerrar";
            default -> "cerrada";
        };
        Object antes = h.data.get("clip");
        if (!clip.equals(antes) && h.clip(clip) != null) {
            h.data.put("clip", clip);
            boolean una = clip.equals("abrir") || clip.equals("abrir_rapido") || clip.equals("cerrar");
            h.animator().play("base", h.clip(clip), una ? 0.02F : 0.1F, 1F, una);
        }
    }

    @Override
    public void axPose(AnimHost h, Pose pose, float pt) {
        Geo geo = h.geo();
        int t = tipo();
        pose.hide(geo.bone("hilo"), t != TRAMPA);
        pose.hide(geo.bone("mancha"), t != TRAMPA);
        pose.hide(geo.bone("luz"), t != SALIDA || estado() != CERRADA);
        if (t == TRAMPA) {
            pose.addRot(geo.bone("placa"), 0, 0, 11F * (float) Math.PI / 180F);
        }
    }

    // ------------------------------------------------------------------ como entidad

    @Override
    public AABB getBoundingBoxForCulling() {
        return getBoundingBox().inflate(1.2, 2.2, 1.2).move(0, 1, 0);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double d) {
        return d < 48 * 48;
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
