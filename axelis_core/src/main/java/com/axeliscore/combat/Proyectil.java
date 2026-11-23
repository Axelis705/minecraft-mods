package com.axeliscore.combat;

import com.axeliscore.fx.Fx;
import com.axeliscore.plat.Plat;
import com.axeliscore.registry.CoreEntities;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

/**
 * Proyectil del Core (bolas de fuego, cristales, lanzas...): su {@link ProyectilTipo} dice cómo se ve, cómo vuela y qué hace al pegar.
 * <pre>
 *     Proyectil.hacia(level, MisTipos.BOLA_FUEGO, jefe, jefe.getEyePosition(), objetivo, 0.9F);
 *     Proyectil.hacia(level, MisTipos.ESPIRITU, jefe, boca, objetivo, 0.5F).persiguiendo(objetivo);
 *     Proyectil.lanzar(level, MisTipos.LANZA, jugador, desde, jugador.getLookAngle().scale(2.2));
 * </pre>
 * No rompe bloques. Los escudos lo paran (es daño de proyectil). No se guarda con el mundo.
 */
public class Proyectil extends Entity {
    private static final EntityDataAccessor<String> TIPO = SynchedEntityData.defineId(Proyectil.class, EntityDataSerializers.STRING);

    private Entity dueno, objetivo;

    public Proyectil(EntityType<? extends Proyectil> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    // ------------------------------------------------------------------ lanzar (servidor)

    /** Lanza un proyectil desde {@code desde} con esa velocidad (bloques por tick). */
    public static Proyectil lanzar(Level l, ProyectilTipo t, Entity dueno, Vec3 desde, Vec3 vel) {
        Proyectil p = new Proyectil(CoreEntities.PROYECTIL.get(), l);
        p.entityData.set(TIPO, t.id);
        p.dueno = dueno;
        p.setPos(desde.x, desde.y, desde.z);
        p.setDeltaMovement(vel);
        p.mirar(vel);
        l.addFreshEntity(p);
        Combate.sonido(l, desde, t.sonidoLanzar, 1F, 0.9F + l.getRandom().nextFloat() * 0.2F);
        return p;
    }

    /** Lanza hacia el centro de {@code objetivo} con esa rapidez (si cae con gravedad, apunta un poco más arriba). */
    public static Proyectil hacia(Level l, ProyectilTipo t, Entity dueno, Vec3 desde, Entity objetivo, float rapidez) {
        Vec3 to = objetivo.position().add(0, objetivo.getBbHeight() * 0.5, 0);
        Vec3 d = to.subtract(desde);
        double dist = d.length();
        if (t.gravedad > 0 && rapidez > 0) {
            double ticks = dist / rapidez;
            d = d.add(0, 0.5 * t.gravedad * ticks * ticks, 0);
        }
        Vec3 vel = d.lengthSqr() < 1.0E-6 ? new Vec3(0, 0, rapidez) : d.normalize().scale(rapidez);
        Proyectil p = lanzar(l, t, dueno, desde, vel);
        p.objetivo = objetivo;
        return p;
    }

    /** Que persiga a alguien (la fuerza la dice el tipo con {@link ProyectilTipo#persigue}). */
    public Proyectil persiguiendo(Entity e) {
        this.objetivo = e;
        return this;
    }

    public ProyectilTipo tipo() {
        return ProyectilTipo.porId(entityData.get(TIPO));
    }

    public Entity dueno() {
        return dueno;
    }

    private void mirar(Vec3 v) {
        double h = Math.sqrt(v.x * v.x + v.z * v.z);
        setYRot((float) Math.toDegrees(Math.atan2(-v.x, v.z)));
        setXRot((float) Math.toDegrees(Math.atan2(-v.y, h)));
        this.yRotO = getYRot();
        this.xRotO = getXRot();
    }

    // ------------------------------------------------------------------ vuelo (las dos partes)

    @Override
    public void tick() {
        super.tick();
        ProyectilTipo t = tipo();
        if (t == null) {
            if (!level().isClientSide() && tickCount > 5) {
                discard();
            }
            return;
        }
        Vec3 v = getDeltaMovement();
        if (!level().isClientSide() && t.persigue > 0 && objetivo != null && objetivo.isAlive() && objetivo.level() == level()) {
            Vec3 to = objetivo.position().add(0, objetivo.getBbHeight() * 0.5, 0).subtract(position());
            double sp = v.length();
            if (to.lengthSqr() > 1.0E-4 && sp > 1.0E-4) {
                Vec3 want = to.normalize().scale(sp);
                v = v.add(want.subtract(v).scale(Math.min(1, t.persigue))).normalize().scale(sp);
            }
        }
        Vec3 from = position(), to = from.add(v);
        if (!level().isClientSide()) {
            HitResult hr = golpe(from, to);
            if (hr != null) {
                impacto(hr.getLocation(), hr instanceof net.minecraft.world.phys.EntityHitResult eh ? eh.getEntity() : null);
                return;
            }
            if (tickCount > t.vida || (!t.atraviesaAgua && isInWater())) {
                Combate.particulas(level(), "smoke", position(), 4, 0.1, 0.1, 0.1, 0.01);
                discard();
                return;
            }
        } else if (!t.estela.isEmpty()) {
            var pt = Combate.particula(t.estela);
            if (pt != null) {
                for (int i = 0; i < 2; i++) {
                    double k = i / 2.0;
                    level().addParticle(pt, from.x + v.x * k, from.y + v.y * k, from.z + v.z * k, 0, 0, 0);
                }
            }
        }
        setPos(to.x, to.y, to.z);
        v = v.scale(t.arrastre).add(0, -t.gravedad, 0);
        setDeltaMovement(v);
        mirar(v);
    }

    /** Lo primero que toca entre {@code from} y {@code to}: una criatura (no aliada) o un bloque. */
    private HitResult golpe(Vec3 from, Vec3 to) {
        BlockHitResult bh = level().clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        Vec3 end = bh.getType() == HitResult.Type.MISS ? to : bh.getLocation();
        Entity mejor = null;
        Vec3 donde = null;
        double best = Double.MAX_VALUE;
        AABB caja = getBoundingBox().expandTowards(end.subtract(from)).inflate(1.0);
        for (Entity e : level().getEntities(this, caja, this::puedePegar)) {
            Optional<Vec3> hit = e.getBoundingBox().inflate(0.3).clip(from, end);
            if (hit.isPresent()) {
                double d = from.distanceToSqr(hit.get());
                if (d < best) {
                    best = d;
                    mejor = e;
                    donde = hit.get();
                }
            }
        }
        if (mejor != null) {
            return new net.minecraft.world.phys.EntityHitResult(mejor, donde);
        }
        return bh.getType() == HitResult.Type.MISS ? null : bh;
    }

    private boolean puedePegar(Entity e) {
        return e != dueno && e instanceof LivingEntity && !e.isSpectator() && e.isPickable() && Combate.blanco(dueno, e);
    }

    private void impacto(Vec3 at, Entity golpeado) {
        ProyectilTipo t = tipo();
        Level l = level();
        DamageSource src = dueno instanceof LivingEntity le ? l.damageSources().mobProjectile(this, le) : l.damageSources().magic();
        if (t.area > 0) {
            double r = t.area;
            for (LivingEntity e : l.getEntitiesOfClass(LivingEntity.class, new AABB(at.x - r, at.y - r, at.z - r, at.x + r, at.y + r, at.z + r))) {
                if (!Combate.blanco(dueno, e)) {
                    continue;
                }
                double d = Math.sqrt(e.distanceToSqr(at.x, Math.max(e.getY(), Math.min(at.y, e.getY() + e.getBbHeight())), at.z));
                if (d > r + e.getBbWidth() / 2) {
                    continue;
                }
                float k = (float) Math.max(0.6, 1 - d / Math.max(0.1, r) * 0.4);
                e.invulnerableTime = 0;
                Combate.danar(e, src, t.dano * k);
                Combate.empujarDesde(e, at, t.empuje, t.empuje * 0.6);
                if (t.estado != null) {
                    Estados.aplicar(e, t.estado, t.estadoTicks, t.estadoNivel, dueno);
                }
            }
        } else if (golpeado instanceof LivingEntity le && Combate.blanco(dueno, le)) {
            le.invulnerableTime = 0;
            Combate.danar(le, src, t.dano);
            Vec3 v = getDeltaMovement().normalize();
            Plat.push(le, v.x * t.empuje, 0.15 + t.empuje * 0.3, v.z * t.empuje);
            if (t.estado != null) {
                Estados.aplicar(le, t.estado, t.estadoTicks, t.estadoNivel, dueno);
            }
        }
        Combate.sonido(l, at, t.sonidoImpacto, 1F, 0.9F + l.getRandom().nextFloat() * 0.2F);
        String pn = t.particulasImpacto;
        if (pn != null && !pn.isEmpty()) {
            Combate.particulas(l, pn, at, pn.equals("explosion") ? Math.max(1, (int) t.area) : 10, Math.max(0.2, t.area * 0.3), 0.2,
                    Math.max(0.2, t.area * 0.3), pn.equals("explosion") ? 0 : 0.05);
        }
        if (t.temblor > 0) {
            Fx.shakeNear(this, Math.max(6, t.area * 4), t.temblor, 8);
        }
        discard();
    }

    // ------------------------------------------------------------------ lo de siempre de una entidad

    //#if MC >= 1.20.5
    //$$ @Override
    //$$ protected void defineSynchedData(SynchedEntityData.Builder b) {
    //$$     b.define(TIPO, "");
    //$$ }
    //#else
    @Override
    protected void defineSynchedData() {
        this.entityData.define(TIPO, "");
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

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

}
