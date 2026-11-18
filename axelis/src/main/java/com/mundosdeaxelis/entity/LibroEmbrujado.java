package com.mundosdeaxelis.entity;

import com.mundosdeaxelis.fx.Vfx;
import com.mundosdeaxelis.vfx.VfxType;
import com.mundosdeaxelis.world.Scheduler;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

/**
 * Libro embrujado (Torre del Archimago): un libro que vuela abriendo y cerrando las tapas, da vueltas a unos 6 bloques de ti y,
 * tras pasar las hojas rápido (aviso de un segundo), lanza una página-orbe morada que hace poco daño. Frágil: dos o tres golpes.
 */
public class LibroEmbrujado extends Monster {
    private static final EntityDataAccessor<Boolean> CHARGING = SynchedEntityData.defineId(LibroEmbrujado.class, EntityDataSerializers.BOOLEAN);
    private static final int COLOR = 0xB57CFF;
    private static final float BOLT_DAMAGE = 3.0F;

    private int shootCd = 40;
    private int chargeTicks;
    private int moveCd;
    private final double orbit;

    public LibroEmbrujado(EntityType<? extends LibroEmbrujado> type, Level level) {
        super(type, level);
        this.xpReward = 5;
        this.moveControl = new FlyingMoveControl(this, 20, true);
        this.setNoGravity(true);
        this.orbit = this.random.nextBoolean() ? 1.0D : -1.0D;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 10.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.FLYING_SPEED, 0.55D)
                .add(Attributes.FOLLOW_RANGE, 22.0D);
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        FlyingPathNavigation nav = new FlyingPathNavigation(this, level);
        nav.setCanOpenDoors(false);
        nav.setCanFloat(true);
        nav.setCanPassDoors(true);
        return nav;
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(CHARGING, false);
    }

    /** Pasa las hojas rápido: va a disparar (el modelo aletea más deprisa). */
    public boolean isCharging() {
        return this.entityData.get(CHARGING);
    }

    @Override
    protected void registerGoals() {
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    protected void checkFallDamage(double y, boolean onGround, net.minecraft.world.level.block.state.BlockState state, net.minecraft.core.BlockPos pos) {
    }

    @Override
    public boolean isNoGravity() {
        return true;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level().isClientSide && this.random.nextInt(4) == 0) {
            this.level().addParticle(ParticleTypes.ENCHANT, getX() + (this.random.nextDouble() - 0.5) * 0.8, getY() + 0.4,
                    getZ() + (this.random.nextDouble() - 0.5) * 0.8, 0.0, 0.05, 0.0);
        }
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (!(this.level() instanceof ServerLevel sl)) {
            return;
        }
        LivingEntity t = getTarget();
        if (t == null || !t.isAlive()) {
            this.entityData.set(CHARGING, false);
            chargeTicks = 0;
            if (--moveCd <= 0) {
                // sin nadie a quien perseguir: flota despacio por la sala
                moveCd = 40 + this.random.nextInt(40);
                getMoveControl().setWantedPosition(getX() + this.random.nextGaussian() * 3, getY() + this.random.nextGaussian() * 0.6,
                        getZ() + this.random.nextGaussian() * 3, 0.5D);
            }
            return;
        }
        getLookControl().setLookAt(t, 40.0F, 40.0F);
        if (--moveCd <= 0) {
            moveCd = 14 + this.random.nextInt(10);
            Vec3 off = position().subtract(t.position());
            double ang = Math.atan2(off.z, off.x) + orbit * (0.45 + this.random.nextDouble() * 0.5);
            double dist = 5.0 + this.random.nextDouble() * 2.5;
            getMoveControl().setWantedPosition(t.getX() + Math.cos(ang) * dist, t.getY() + 1.6 + this.random.nextDouble() * 1.2,
                    t.getZ() + Math.sin(ang) * dist, 1.0D);
        }
        if (isCharging()) {
            chargeTicks++;
            if (chargeTicks % 5 == 0) {
                sl.playSound(null, getX(), getY(), getZ(), SoundEvents.BOOK_PAGE_TURN, SoundSource.HOSTILE, 0.9F, 1.4F + this.random.nextFloat() * 0.3F);
                sl.sendParticles(ParticleTypes.ENCHANT, getX(), getY() + 0.4, getZ(), 6, 0.3, 0.3, 0.3, 0.4);
            }
            if (chargeTicks >= 20) {
                if (hasLineOfSight(t)) {
                    fire(sl, t);
                }
                this.entityData.set(CHARGING, false);
                chargeTicks = 0;
                shootCd = 55 + this.random.nextInt(35);
            }
        } else if (--shootCd <= 0 && hasLineOfSight(t) && distanceToSqr(t) < 18.0 * 18.0) {
            this.entityData.set(CHARGING, true);
            chargeTicks = 0;
        }
    }

    /** Lanza una página-orbe: vuela recta (gira un poco hacia ti), sin entidad propia; el servidor la mueve y avisa al cliente. */
    private void fire(ServerLevel sl, LivingEntity t) {
        Vec3 start = position().add(0, 0.45, 0);
        Vec3 dir = t.getBoundingBox().getCenter().subtract(start).normalize();
        sl.playSound(null, getX(), getY(), getZ(), com.mundosdeaxelis.registry.ModSounds.ARCHIMAGO_ORB.get(), SoundSource.HOSTILE, 0.8F, 1.5F);
        int id = Vfx.make(sl, VfxType.ORB).at(start).rgb(COLOR).size(0.22).life(46).flags(VfxType.F_TRAIL).send();
        step(sl, this, t, start, dir, id, 0);
    }

    private static void step(ServerLevel sl, LibroEmbrujado owner, LivingEntity target, Vec3 pos, Vec3 dir, int id, int age) {
        if (age > 40 || owner.isRemoved()) {
            pop(sl, id, pos);
            return;
        }
        if (target.isAlive()) {
            Vec3 want = target.getBoundingBox().getCenter().subtract(pos).normalize();
            dir = dir.scale(0.93).add(want.scale(0.07)).normalize();
        }
        Vec3 next = pos.add(dir.scale(0.55));
        BlockHitResult bh = sl.clip(new ClipContext(pos, next, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, owner));
        Vec3 end = bh.getType() == HitResult.Type.MISS ? next : bh.getLocation();
        for (Player p : sl.getEntitiesOfClass(Player.class, new AABB(pos, end).inflate(1.0))) {
            if (p.isSpectator() || p.isCreative()) {
                continue;
            }
            Optional<Vec3> c = p.getBoundingBox().inflate(0.3).clip(pos, end);
            if (c.isPresent()) {
                p.hurt(owner.damageSources().indirectMagic(owner, owner), BOLT_DAMAGE);
                pop(sl, id, c.get());
                return;
            }
        }
        if (bh.getType() != HitResult.Type.MISS) {
            pop(sl, id, end);
            return;
        }
        Vfx.make(sl, VfxType.ORB).at(end).rgb(COLOR).size(0.22).flags(VfxType.F_TRAIL).life(0).update(id);
        final Vec3 d = dir;
        Scheduler.runLater(1, () -> step(sl, owner, target, end, d, id, age + 1));
    }

    private static void pop(ServerLevel sl, int id, Vec3 at) {
        Vfx.remove(sl, id);
        sl.sendParticles(ParticleTypes.ENCHANT, at.x, at.y, at.z, 14, 0.2, 0.2, 0.2, 0.6);
        sl.sendParticles(ParticleTypes.WITCH, at.x, at.y, at.z, 4, 0.1, 0.1, 0.1, 0.02);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.BOOK_PAGE_TURN;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.BOOK_PUT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ENCHANTMENT_TABLE_USE;
    }

    @Override
    protected float getSoundVolume() {
        return 0.8F;
    }
}
