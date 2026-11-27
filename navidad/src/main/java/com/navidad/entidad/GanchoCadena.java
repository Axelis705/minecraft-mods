package com.navidad.entidad;

import com.axeliscore.combat.Combate;
import com.axeliscore.combat.Jefe;
import com.navidad.Navidad;
import com.navidad.registry.ModRegistry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * La cadena con gancho del Krampus (y la que puedes ganarte): vuela derecho por donde la aventaron y deja la cadena dibujada desde la
 * mano de su dueño.
 * <ul>
 *     <li>Si engancha a alguien, lo jala hasta su dueño (si su dueño es el Krampus, le toca una tanda de varazos).</li>
 *     <li>Si el dueño es una persona y engancha a algo muy pesado (un jefe, un gólem), es la persona la que sale volando hacia él.</li>
 *     <li>Si la persona la clava en un bloque, la jala hasta ahí (como un gancho para escalar), sin daño por caída.</li>
 * </ul>
 */
public class GanchoCadena extends Projectile {
    public static final int VOLANDO = 0, JALANDO = 1, ANCLADO = 2;
    private static final EntityDataAccessor<Integer> ESTADO = SynchedEntityData.defineId(GanchoCadena.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> PRESA = SynchedEntityData.defineId(GanchoCadena.class, EntityDataSerializers.INT);

    private Vec3 desde = Vec3.ZERO;
    private double alcance = 20;
    private int edad, jalando;
    private boolean alReves;
    /** Para la prueba con fotos y el registro. */
    public static int enganches, anclajes;

    public GanchoCadena(EntityType<? extends GanchoCadena> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    /** Avienta un gancho desde {@code desde} hacia {@code dir}. */
    public static GanchoCadena lanzar(Level l, Entity dueno, Vec3 desde, Vec3 dir, float rapidez, double alcance) {
        GanchoCadena g = ModRegistry.GANCHO_CADENA.get().create(l);
        if (g == null) {
            return null;
        }
        g.setOwner(dueno);
        g.setPos(desde.x, desde.y, desde.z);
        g.desde = desde;
        g.alcance = alcance;
        g.setDeltaMovement(dir.normalize().scale(rapidez));
        g.updateRotation();
        l.addFreshEntity(g);
        l.playSound(null, g.blockPosition(), ModRegistry.GANCHO.get(), SoundSource.HOSTILE, 1.4F, 0.9F + l.random.nextFloat() * 0.2F);
        return g;
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(ESTADO, VOLANDO);
        this.entityData.define(PRESA, -1);
    }

    public int estado() {
        return entityData.get(ESTADO);
    }

    /** (Las dos partes) A quién trae enganchado (null si a nadie). */
    public Entity presa() {
        int id = entityData.get(PRESA);
        return id < 0 ? null : level().getEntity(id);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            return;
        }
        edad++;
        Entity dueno = getOwner();
        if (dueno == null || !dueno.isAlive() || edad > 80 || dueno.level() != level()) {
            terminar(null);
            return;
        }
        switch (estado()) {
            case VOLANDO -> volar(dueno);
            case JALANDO -> jalar(dueno);
            default -> anclado(dueno);
        }
    }

    private void volar(Entity dueno) {
        HitResult hit = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity);
        if (hit.getType() != HitResult.Type.MISS) {
            onHit(hit);
            if (estado() != VOLANDO || isRemoved()) {
                return;
            }
        }
        Vec3 v = getDeltaMovement();
        setPos(getX() + v.x, getY() + v.y, getZ() + v.z);
        if (position().distanceTo(desde) > alcance) {
            terminar(null);
        }
    }

    @Override
    protected boolean canHitEntity(Entity e) {
        Entity dueno = getOwner();
        return super.canHitEntity(e) && e instanceof LivingEntity && e != dueno && !(e instanceof GanchoCadena)
                && (dueno == null || Combate.blanco(dueno, e)) && !e.isPassenger();
    }

    @Override
    protected void onHitEntity(EntityHitResult r) {
        if (!(r.getEntity() instanceof LivingEntity e)) {
            return;
        }
        Entity dueno = getOwner();
        Combate.danar(e, Combate.fuente(level(), dueno), dueno instanceof Krampus k ? k.dano(Krampus.CADENA_DMG) : 4F);
        // una persona no puede jalar algo muy pesado: sale volando ella hacia él
        alReves = dueno instanceof Player && (e instanceof Jefe || e.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE) >= 0.8
                || e.getBbWidth() > 1.6F);
        entityData.set(PRESA, e.getId());
        entityData.set(ESTADO, JALANDO);
        jalando = 0;
        enganches++;
        level().playSound(null, e.blockPosition(), ModRegistry.KRAMPUS_CADENAS.get(), SoundSource.HOSTILE, 1.4F, 1.1F);
        if (e instanceof ServerPlayer p && dueno instanceof Krampus) {
            p.displayClientMessage(net.minecraft.network.chat.Component.translatable("navidad.krampus.enganchado")
                    .withStyle(net.minecraft.ChatFormatting.RED), true);
        }
        Navidad.LOGGER.info("NAVIDAD_GANCHO enganchó a {} (de {})", e.getName().getString(), dueno == null ? "?" : dueno.getName().getString());
    }

    @Override
    protected void onHitBlock(BlockHitResult r) {
        if (getOwner() instanceof Player) {
            Vec3 p = r.getLocation();
            setPos(p.x, p.y, p.z);
            setDeltaMovement(Vec3.ZERO);
            entityData.set(ESTADO, ANCLADO);
            jalando = 0;
            anclajes++;
            level().playSound(null, r.getBlockPos(), ModRegistry.KRAMPUS_CADENAS.get(), SoundSource.PLAYERS, 1.2F, 1.3F);
        } else {
            terminar(null);
        }
    }

    private static Vec3 centro(Entity e) {
        return e.position().add(0, e.getBbHeight() * 0.55, 0);
    }

    private void jalar(Entity dueno) {
        Entity e = presa();
        jalando++;
        if (!(e instanceof LivingEntity le) || !le.isAlive() || jalando > 18) {
            terminar(e instanceof LivingEntity l ? l : null);
            return;
        }
        Entity quien = alReves ? dueno : e;
        Entity hacia = alReves ? e : dueno;
        Vec3 d = centro(hacia).subtract(centro(quien));
        double dist = d.length();
        Vec3 c = centro(e);
        setPos(c.x, c.y, c.z);
        if (dist < 2.4) {
            quien.setDeltaMovement(quien.getDeltaMovement().scale(0.2));
            quien.hurtMarked = true;
            terminar(le);
            return;
        }
        Vec3 v = d.scale(1 / dist).scale(Math.min(1.25, 0.35 + dist * 0.09));
        quien.setDeltaMovement(v.x, Math.max(v.y, 0) + 0.12, v.z);
        quien.hurtMarked = true;
        quien.fallDistance = 0;
    }

    private void anclado(Entity dueno) {
        jalando++;
        Vec3 d = position().subtract(dueno.position().add(0, 1.0, 0));
        double dist = d.length();
        if (dist < 1.4 || jalando > 30 || dueno.isShiftKeyDown()) {
            dueno.fallDistance = 0;
            terminar(null);
            return;
        }
        Vec3 v = d.scale(1 / dist).scale(Math.min(1.2, 0.4 + dist * 0.08));
        dueno.setDeltaMovement(v.x, v.y + 0.08, v.z);
        dueno.hurtMarked = true;
        dueno.fallDistance = 0;
    }

    private void terminar(LivingEntity jalado) {
        if (isRemoved()) {
            return;
        }
        if (getOwner() instanceof Krampus k) {
            k.ganchoRegreso(jalado);
        }
        discard();
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double d) {
        return d < 96 * 96;
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
    }
}
