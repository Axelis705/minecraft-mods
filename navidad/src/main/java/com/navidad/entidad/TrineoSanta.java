package com.navidad.entidad;

import com.axeliscore.anim.AnimHost;
import com.axeliscore.anim.Animated;
import com.axeliscore.anim.Animator;
import com.axeliscore.anim.Clip;
import com.axeliscore.anim.Pose;
import com.axeliscore.data.PlayerStore;
import com.navidad.Navidad;
import com.navidad.registry.ModRegistry;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * El trineo de Santa cruzando el cielo (con Santa adentro y sus renos adelante). Llega de lejos con los cascabeles sonando (el jugador ve
 * un aviso arriba de la pantalla con una flecha hacia donde viene), baja la velocidad al pasar encima de alguien, Santa se ríe
 * ("¡Jo, jo, jo!"), lo saluda con la mano y le avienta dos regalos del costal, que bajan con paracaídas. Luego sigue su camino y se va.
 */
public class TrineoSanta extends Entity implements Animated {
    private static final EntityDataAccessor<String> GESTO = SynchedEntityData.defineId(TrineoSanta.class, EntityDataSerializers.STRING);
    /** Lo que le llega al cliente para el aviso en pantalla ("viene" y "jojo"). */
    public static final String AVISO = Navidad.MODID + ":santa";
    private static final double RAPIDO = 0.55, LENTO = 0.2, CERCA = 30;
    private int vida = 1100, regalos, gestos, saludaEn = -1, lanzaEn = -1;
    private ServerPlayer lanzaA;
    private double vel = RAPIDO;
    /** Cuántos regalos le soltó a cada quien en este viaje, y cuándo lo saludó. */
    private final Map<UUID, Integer> dados = new HashMap<>();
    private final Map<UUID, Integer> saludado = new HashMap<>();
    private final AnimHost anim = new AnimHost();
    private static int avisos;

    public TrineoSanta(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(GESTO, "");
    }

    public void gesto(String clip) {
        gestos++;
        this.entityData.set(GESTO, clip + "#" + gestos);
    }

    /** Hacia dónde va (sin altura). */
    private Vec3 rumbo() {
        Vec3 v = getDeltaMovement();
        double h = Math.sqrt(v.x * v.x + v.z * v.z);
        return h < 1e-4 ? new Vec3(0, 0, 1) : new Vec3(v.x / h, 0, v.z / h);
    }

    @Override
    public void tick() {
        super.tick();
        Vec3 dir = rumbo();
        float yaw = (float) Math.toDegrees(Math.atan2(-dir.x, dir.z));
        if (tickCount <= 1) {
            yRotO = yaw;
        }
        setYRot(yaw);
        Vec3 v = getDeltaMovement();
        setPos(getX() + v.x, getY() + Math.sin(tickCount * 0.08) * 0.03, getZ() + v.z);
        if (level().isClientSide) {
            // la estela mágica: polvito dorado detrás del trineo y de las patas de los renos
            if (tickCount % 2 == 0) {
                level().addParticle(ParticleTypes.END_ROD, getX() - dir.x * 2.2, getY() + 0.6, getZ() - dir.z * 2.2, 0, -0.02, 0);
                level().addParticle(ParticleTypes.SNOWFLAKE, getX() - dir.x * 3, getY() + 0.3, getZ() - dir.z * 3, 0, -0.05, 0);
            }
            if (tickCount % 3 == 0) {
                double d = 2.4 + random.nextDouble() * 2.2, lado = (random.nextBoolean() ? 1 : -1) * 0.6;
                level().addParticle(ParticleTypes.WAX_ON, getX() + dir.x * d - dir.z * lado, getY() + 0.2, getZ() + dir.z * d + dir.x * lado,
                        0, -0.03, 0);
            }
            return;
        }
        ServerLevel l = (ServerLevel) level();
        // baja la velocidad encima de la gente, para que alcance a saludar y aventar los regalos
        double cerca = Double.MAX_VALUE;
        for (ServerPlayer p : l.players()) {
            double dx = p.getX() - getX(), dz = p.getZ() - getZ();
            cerca = Math.min(cerca, Math.sqrt(dx * dx + dz * dz));
        }
        vel += ((cerca < CERCA ? LENTO : RAPIDO) - vel) * 0.04;
        setDeltaMovement(dir.x * vel, 0, dir.z * vel);
        if (tickCount % 40 == 0) {
            l.playSound(null, blockPosition(), ModRegistry.CASCABELES.get(), SoundSource.AMBIENT, 6F, 1F);
        }
        // saludar a quien pasa abajo; los regalos empiezan cuando ya saludó
        for (ServerPlayer p : l.players()) {
            double dx = p.getX() - getX(), dz = p.getZ() - getZ();
            if (dx * dx + dz * dz > 26 * 26 || Math.abs(p.getY() - getY()) > 80) {
                continue;
            }
            if (!saludado.containsKey(p.getUUID())) {
                saludado.put(p.getUUID(), tickCount);
                saludar(p);
            }
        }
        if (saludaEn == tickCount) {
            gesto("saluda");
        }
        if (lanzaEn == tickCount && lanzaA != null) {
            // el regalo sale del costal por el lado derecho, con un empujoncito hacia afuera
            Vec3 der = new Vec3(-dir.z, 0, dir.x);
            double x = getX() + der.x * 1.3 + dir.x * 0.2, y = getY() + 1.9, z = getZ() + der.z * 1.3 + dir.z * 0.2;
            RegaloCayendo r = RegaloCayendo.soltar(l, x, y, z, premio(l.random), lanzaA);
            if (r != null) {
                r.setDeltaMovement(der.x * 0.18 + dir.x * vel * 0.6, 0.12, der.z * 0.18 + dir.z * vel * 0.6);
            }
            l.sendParticles(ParticleTypes.END_ROD, x, y, z, 8, 0.2, 0.2, 0.2, 0.05);
            lanzaA = null;
        }
        if (tickCount % 28 == 0 && regalos < 8 && lanzaA == null && (saludaEn < 0 || tickCount > saludaEn + 40)) {
            for (ServerPlayer p : l.players()) {
                Integer desde = saludado.get(p.getUUID());
                int ya = dados.getOrDefault(p.getUUID(), 0);
                if (desde == null || ya >= 2 || tickCount < desde + 70) {
                    continue;
                }
                double dx = p.getX() - getX(), dz = p.getZ() - getZ();
                if (dx * dx + dz * dz > 34 * 34) {
                    continue;
                }
                dados.put(p.getUUID(), ya + 1);
                regalos++;
                gesto("lanza");
                lanzaEn = tickCount + 12;
                lanzaA = p;
                break;
            }
        }
        if (--vida <= 0) {
            discard();
        }
    }

    /** Lo que trae cada regalo: casi siempre algo sencillo, a veces algo bueno y muy de vez en cuando algo increíble. */
    static ItemStack premio(net.minecraft.util.RandomSource r) {
        float x = r.nextFloat();
        var rz = x < 0.03F ? com.navidad.objeto.Premios.Rareza.EPICO : x < 0.25F ? com.navidad.objeto.Premios.Rareza.RARO
                : com.navidad.objeto.Premios.Rareza.COMUN;
        return com.navidad.objeto.Premios.sacar(rz, r);
    }

    /** Santa se ríe y luego saluda con la mano; al jugador le sale el "¡Jo, jo, jo!" animado en la pantalla. */
    private void saludar(ServerPlayer p) {
        gesto("rie");
        saludaEn = tickCount + 32;
        level().playSound(null, blockPosition(), ModRegistry.RISA_SANTA.get(), SoundSource.AMBIENT, 6F, 1F);
        avisar(p, "jojo", this);
        Navidad.LOGGER.info("NAVIDAD_SANTA saluda a {}", p.getGameProfile().getName());
    }

    /** Le manda al jugador el aviso de la pantalla: "viene" (con dónde está el trineo) o "jojo". */
    public static void avisar(ServerPlayer p, String fase, TrineoSanta t) {
        CompoundTag c = new CompoundTag();
        c.putString("fase", fase);
        c.putInt("n", ++avisos);
        c.putInt("id", t.getId());
        c.putDouble("x", t.getX());
        c.putDouble("z", t.getZ());
        c.putString("nombre", p.getGameProfile().getName());
        PlayerStore.Sync.toPlayer(p, AVISO, c);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double d) {
        return d < 320 * 320;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag t) {
        if (t.contains("Vida")) {
            vida = t.getInt("Vida");
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag t) {
        t.putInt("Vida", vida);
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return new ClientboundAddEntityPacket(this);
    }

    // ------------------------------------------------------------------ animación (cliente)

    @Override
    public AnimHost axHost() {
        return anim;
    }

    @Override
    public void axAnimate(AnimHost h, float pt) {
        Animator an = h.animator();
        String g = this.entityData.get(GESTO);
        if (!g.equals(h.data.get("gesto"))) {
            h.data.put("gesto", g);
            String[] p = g.split("#");
            Clip c = p.length > 0 && !p[0].isEmpty() ? h.clip(p[0]) : null;
            if (c != null) {
                an.play("action", c, 0.12F, 1F, true);
            }
        }
        an.play("base", h.clip("idle"), 0.3F, 1F, false);
    }

    @Override
    public void axPose(AnimHost h, Pose pose, float pt) {
        // el farol titila un poquito
        int f = h.geo().bone("farol_luz");
        if (f >= 0) {
            int v = (int) (215 + 40 * Mth.sin((tickCount + pt) * 0.7F) * Mth.sin((tickCount + pt) * 0.23F));
            pose.tinte(f, 0xFF000000 | v << 16 | v << 8 | v);
        }
    }
}
