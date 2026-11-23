package com.pescacoleccion.pesca;

import com.axeliscore.anim.AnimHost;
import com.axeliscore.anim.Animated;
import com.axeliscore.plat.Plat;
import com.pescacoleccion.PescaColeccion;
import com.pescacoleccion.pelea.Niveles;
import com.pescacoleccion.pelea.PeleaSim;
import com.pescacoleccion.pelea.Peleas;
import com.pescacoleccion.registry.ModRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * El corcho de la caña. Vuela, cae al agua y flota; un pez se acerca (burbujas), da piquetes falsos y luego la picada de verdad: hay que
 * tirar (clic derecho) justo en la buena. Si se tira antes, el pez se asusta. Si se tira a tiempo, empieza la pelea y el corcho se
 * mueve por el agua mientras dura (lo ven todos).
 */
public class Anzuelo extends Entity implements Animated {
    public static final int VUELA = 0, FLOTA = 1, PIQUETE = 2, PICA = 3, PELEA = 4, SUELO = 5;
    private static final EntityDataAccessor<Integer> OWNER = SynchedEntityData.defineId(Anzuelo.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> STATE = SynchedEntityData.defineId(Anzuelo.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> PULSE = SynchedEntityData.defineId(Anzuelo.class, EntityDataSerializers.INT);
    /** El corcho de cada jugador (uno a la vez). */
    private static final Map<UUID, Anzuelo> ACTIVOS = new ConcurrentHashMap<>();

    private final AnimHost anim = new AnimHost();
    // servidor
    private Cana cana = Cana.BAMBU;
    private int espera = -1, t, rareza = -1, idx, piqueteHasta, picaHasta;
    private int[] picadas;
    private Vec3 llegada, centro, destino;

    public Anzuelo(EntityType<? extends Anzuelo> type, Level level) {
        super(type, level);
    }

    //#if MC >= 1.20.5
    //$$ @Override
    //$$ protected void defineSynchedData(SynchedEntityData.Builder b) {
    //$$     b.define(OWNER, 0);
    //$$     b.define(STATE, VUELA);
    //$$     b.define(PULSE, 0);
    //$$ }
    //#else
    @Override
    protected void defineSynchedData() {
        this.entityData.define(OWNER, 0);
        this.entityData.define(STATE, VUELA);
        this.entityData.define(PULSE, 0);
    }
    //#endif

    //#if MC >= 1.21.6
    //$$ @Override
    //$$ protected void addAdditionalSaveData(net.minecraft.world.level.storage.ValueOutput out) {
    //$$ }
    //$$
    //$$ @Override
    //$$ protected void readAdditionalSaveData(net.minecraft.world.level.storage.ValueInput in) {
    //$$ }
    //#else
    @Override
    protected void addAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
    }

    @Override
    protected void readAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
    }
    //#endif

    //#if MC >= 1.21.2
    //$$ @Override
    //$$ public boolean hurtServer(ServerLevel level, net.minecraft.world.damagesource.DamageSource source, float amount) {
    //$$     return false;
    //$$ }
    //#endif

    //#if MC < 1.20.5
    @Override
    public net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getAddEntityPacket() {
        return new net.minecraft.network.protocol.game.ClientboundAddEntityPacket(this);
    }
    //#endif

    /** No se guarda con el mundo (si se cierra el juego, el corcho desaparece). */
    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    public AnimHost axHost() {
        return anim;
    }

    public int state() {
        return this.entityData.get(STATE);
    }

    private void state(int s) {
        this.entityData.set(STATE, s);
    }

    public int pulse() {
        return this.entityData.get(PULSE);
    }

    public Player owner() {
        Entity e = level().getEntity(this.entityData.get(OWNER));
        return e instanceof Player p ? p : null;
    }

    // ------------------------------------------------------------------ lanzar y tirar (servidor)

    /** El corcho del jugador, si tiene uno. */
    public static Anzuelo activo(ServerPlayer p) {
        Anzuelo a = ACTIVOS.get(p.getUUID());
        if (a != null && (a.isRemoved() || a.level() != p.level())) {
            ACTIVOS.remove(p.getUUID());
            return null;
        }
        return a;
    }

    public static void lanzar(ServerPlayer p, Cana cana) {
        Anzuelo a = new Anzuelo(ModRegistry.ANZUELO.get(), p.level());
        a.entityData.set(OWNER, p.getId());
        a.cana = cana;
        float yaw = (float) Math.toRadians(p.getYRot()), pitch = (float) Math.toRadians(p.getXRot());
        Vec3 look = new Vec3(-Math.sin(yaw) * Math.cos(pitch), -Math.sin(pitch), Math.cos(yaw) * Math.cos(pitch));
        a.setPos(p.getX() + look.x * 0.4, p.getEyeY() - 0.15, p.getZ() + look.z * 0.4);
        double speed = 0.75 + Math.random() * 0.15;
        a.setDeltaMovement(look.x * speed, look.y * speed + 0.18, look.z * speed);
        p.level().addFreshEntity(a);
        ACTIVOS.put(p.getUUID(), a);
        p.level().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.FISHING_BOBBER_THROW, SoundSource.NEUTRAL, 0.5F,
                0.4F / (0.8F + (float) Math.random() * 0.4F));
    }

    /** Clic derecho con el corcho afuera: en la picada buena empieza la pelea; si no, se recoge (y si había un pez, se asusta). */
    public void tirar(ServerPlayer p) {
        int s = state();
        if (s == PELEA) {
            return;
        }
        if (s == PICA) {
            empezarPelea(p);
            return;
        }
        boolean asustado = picadas != null;
        level().playSound(null, getX(), getY(), getZ(), SoundEvents.FISHING_BOBBER_RETRIEVE, SoundSource.NEUTRAL, 1F,
                0.4F / (0.8F + (float) Math.random() * 0.4F));
        if (asustado) {
            Plat.msg(p, Component.translatable("pesca_coleccion.pesca.asustado"), true);
        }
        quitar();
    }

    private void quitar() {
        Player o = owner();
        if (o != null) {
            ACTIVOS.remove(o.getUUID(), this);
        }
        discard();
    }

    private void empezarPelea(ServerPlayer p) {
        state(PELEA);
        this.entityData.set(PULSE, pulse() + 1);
        centro = position();
        destino = centro;
        int r = rareza < 0 ? 0 : rareza;
        PeleaSim.Params params = Niveles.base(r, random.nextLong());
        Niveles.cana(params, cana.zone, cana.control, cana.lineHp, cana.damage);
        if (PescaColeccion.facil()) {
            Niveles.facil(params);
        }
        level().playSound(null, getX(), getY(), getZ(), SoundEvents.FISHING_BOBBER_SPLASH, SoundSource.NEUTRAL, 0.9F, 0.8F);
        Peleas.start(p, params, PescaColeccion.MODID + ".prueba.pez_" + r, r, sim -> terminar(p, r, sim));
    }

    private void terminar(ServerPlayer p, int r, PeleaSim sim) {
        if (isRemoved()) {
            return;
        }
        if (sim.result == PeleaSim.PESCADO) {
            ItemStack st = com.pescacoleccion.pelea.Prueba.premio(r);
            ItemEntity it = new ItemEntity(level(), getX(), getY(), getZ(), st);
            double dx = p.getX() - getX(), dy = p.getY() - getY(), dz = p.getZ() - getZ();
            it.setDeltaMovement(dx * 0.1, dy * 0.1 + Math.sqrt(Math.sqrt(dx * dx + dy * dy + dz * dz)) * 0.08, dz * 0.1);
            level().addFreshEntity(it);
            p.giveExperiencePoints(1 + r * 2 + random.nextInt(3));
            com.pescacoleccion.pelea.Prueba.avisar(p, r, st, sim.flawless());
            splash(20, 0.4);
        } else if (sim.result == PeleaSim.HILO_ROTO) {
            level().playSound(null, getX(), getY(), getZ(), SoundEvents.FISHING_BOBBER_RETRIEVE, SoundSource.NEUTRAL, 1F, 0.5F);
            Plat.msg(p, Component.translatable("pesca_coleccion.pesca.hilo_roto"), true);
        } else {
            Plat.msg(p, Component.translatable("pesca_coleccion.pesca.escapo"), true);
        }
        quitar();
    }

    // ------------------------------------------------------------------ cada tick

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) {
            return;
        }
        Player o = owner();
        if (o == null || !o.isAlive() || o.level() != level() || distanceToSqr(o) > 40 * 40
                || state() != PELEA && !(o.getMainHandItem().getItem() instanceof CanaItem) && !(o.getOffhandItem().getItem() instanceof CanaItem)) {
            quitar();
            return;
        }
        Vec3 v = getDeltaMovement();
        int s = state();
        if (s == VUELA || s == SUELO) {
            v = v.add(0, -0.03, 0).scale(s == VUELA ? 0.98 : 0.6);
            if (isInWater()) {
                state(FLOTA);
                v = v.multiply(0.3, 0.1, 0.3);
                espera = Captura.espera(random, cana, level().isRaining());
                splash(6, 0.15);
                level().playSound(null, getX(), getY(), getZ(), SoundEvents.FISHING_BOBBER_SPLASH, SoundSource.NEUTRAL, 0.25F, 1.4F);
            } else if (onGround() && s == VUELA) {
                state(SUELO);
                v = Vec3.ZERO;
            }
        } else {
            double surface = superficie();
            double dip = s == PIQUETE ? 0.12 : s == PICA ? 0.32 : s == PELEA ? 0.18 : 0;
            double dy = surface - 0.1 - dip - getY();
            v = new Vec3(v.x * 0.88, v.y * 0.55 + dy * 0.3, v.z * 0.88);
            if (s == PELEA) {
                v = v.add(pelea());
            } else {
                timeline();
            }
        }
        setDeltaMovement(v);
        move(MoverType.SELF, v);
    }

    /** Altura de la superficie del agua en esta columna. */
    private double superficie() {
        BlockPos bp = blockPosition();
        FluidState fs = level().getFluidState(bp);
        if (fs.isEmpty()) {
            bp = bp.below();
            fs = level().getFluidState(bp);
            if (fs.isEmpty()) {
                return getY() - 0.2;
            }
        }
        for (int i = 0; i < 3 && !level().getFluidState(bp.above()).isEmpty(); i++) {
            bp = bp.above();
            fs = level().getFluidState(bp);
        }
        return bp.getY() + fs.getHeight(level(), bp);
    }

    /** Espera, pez que se acerca (burbujas), piquetes y la picada buena. */
    private void timeline() {
        if (picadas == null) {
            if (espera > 0 && --espera == 0) {
                rareza = Captura.rareza(random, cana.suerte);
                picadas = Captura.picadas(random, rareza);
                idx = 0;
                t = 0;
                double a = random.nextDouble() * Math.PI * 2;
                llegada = position().add(Math.cos(a) * 3, -0.4, Math.sin(a) * 3);
            }
            return;
        }
        t++;
        // el pez se acerca: un camino de burbujas hacia el corcho
        if (idx == 0 && t < picadas[0] && llegada != null) {
            double k = t / (double) picadas[0];
            Vec3 at = llegada.add(position().subtract(llegada).scale(k));
            ((ServerLevel) level()).sendParticles(ParticleTypes.BUBBLE, at.x, superficie() - 0.15, at.z, 2, 0.05, 0.02, 0.05, 0.01);
            if (t % 3 == 0) {
                ((ServerLevel) level()).sendParticles(ParticleTypes.FISHING, at.x, superficie(), at.z, 1, 0.02, 0, 0.02, 0);
            }
        }
        if (state() == PIQUETE && t >= piqueteHasta) {
            state(FLOTA);
        }
        if (idx < picadas.length && t == picadas[idx]) {
            boolean buena = idx == picadas.length - 1;
            idx++;
            this.entityData.set(PULSE, pulse() + 1);
            if (buena) {
                state(PICA);
                picaHasta = t + Captura.ventana(rareza);
                splash(12, 0.25);
                level().playSound(null, getX(), getY(), getZ(), SoundEvents.FISHING_BOBBER_SPLASH, SoundSource.NEUTRAL, 0.6F,
                        1F + (float) (random.nextDouble() - 0.5) * 0.4F);
            } else {
                state(PIQUETE);
                piqueteHasta = t + 4;
                splash(3, 0.08);
                level().playSound(null, getX(), getY(), getZ(), SoundEvents.FISHING_BOBBER_SPLASH, SoundSource.NEUTRAL, 0.15F, 1.8F);
            }
        }
        if (state() == PICA && t > picaHasta) {
            // no se tiró a tiempo: el pez se va y luego vendrá otro
            state(FLOTA);
            picadas = null;
            espera = Captura.espera(random, cana, level().isRaining()) / 2;
        }
    }

    /** Durante la pelea el pez arrastra el corcho por el agua. */
    private Vec3 pelea() {
        t++;
        if (centro == null) {
            centro = position();
            destino = centro;
        }
        if (t % 18 == 0 || position().distanceToSqr(destino) < 0.1) {
            double a = random.nextDouble() * Math.PI * 2, r = 0.8 + random.nextDouble() * 1.8;
            destino = centro.add(Math.cos(a) * r, 0, Math.sin(a) * r);
        }
        if (t % 6 == 0) {
            splash(4, 0.12);
        }
        if (t % 40 == 0) {
            level().playSound(null, getX(), getY(), getZ(), SoundEvents.FISHING_BOBBER_SPLASH, SoundSource.NEUTRAL, 0.3F, 0.7F + random.nextFloat() * 0.3F);
        }
        Vec3 d = destino.subtract(position());
        double len = Math.sqrt(d.x * d.x + d.z * d.z);
        return len < 0.01 ? Vec3.ZERO : new Vec3(d.x / len * 0.03, 0, d.z / len * 0.03);
    }

    private void splash(int n, double spread) {
        if (level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.SPLASH, getX(), getY() + 0.1, getZ(), n, spread, 0.05, spread, 0.1);
            sl.sendParticles(ParticleTypes.BUBBLE, getX(), getY() - 0.1, getZ(), Math.max(1, n / 3), spread, 0.05, spread, 0.02);
        }
    }

    // ------------------------------------------------------------------ animación (cliente)

    @Override
    public void axAnimate(AnimHost h, float pt) {
        int s = state();
        Object lp = h.data.get("pulso");
        int p = pulse();
        if (!(lp instanceof Integer li) || li != p) {
            h.data.put("pulso", p);
            if (s == PIQUETE) {
                h.animator().play("action", h.clip("piquete"), 0.02F, 1F, true);
            } else if (s == PICA) {
                h.animator().play("action", h.clip("pica"), 0.02F, 1F, true);
            }
        }
        h.play("base", s == PELEA ? "pelea" : s == VUELA ? "vuela" : "flota", 0.2F);
    }
}
