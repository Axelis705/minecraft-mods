package com.navidad.entidad;

import com.axeliscore.anim.AnimHost;
import com.axeliscore.anim.Pose;
import com.axeliscore.character.CoreMob;
import com.navidad.Temporada;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec3;

/**
 * Reno: pasea por la nieve; con zanahorias te sigue y si le das tres se hace tu amigo y te deja montarlo (clic derecho con la mano
 * vacía). Montado corre rápido y salta alto; de noche en temporada navideña... ¡vuela un poquito al saltar!
 */
public class Reno extends CoreMob {
    private static final EntityDataAccessor<Integer> CARINO = SynchedEntityData.defineId(Reno.class, EntityDataSerializers.INT);
    private static final Ingredient COMIDA = Ingredient.of(Items.CARROT, Items.GOLDEN_CARROT, Items.APPLE, Items.WHEAT);

    public Reno(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.setMaxUpStep(1.1F);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 24.0).add(Attributes.MOVEMENT_SPEED, 0.25).add(Attributes.FOLLOW_RANGE, 20.0);
    }

    public static boolean puedeSalir(EntityType<Reno> t, ServerLevelAccessor l, MobSpawnType why, BlockPos p, RandomSource r) {
        return Mob.checkMobSpawnRules(t, l, why, p, r) && l.getRawBrightness(p, 0) > 8;
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(CARINO, 0);
    }

    public boolean amigo() {
        return entityData.get(CARINO) >= 3;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new PanicGoal(this, 1.6));
        this.goalSelector.addGoal(3, new TemptGoal(this, 1.1, COMIDA, false));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.7));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
    }

    @Override
    protected InteractionResult mobInteract(Player p, InteractionHand hand) {
        ItemStack st = p.getItemInHand(hand);
        if (COMIDA.test(st)) {
            if (!level().isClientSide) {
                if (!p.getAbilities().instabuild) {
                    st.shrink(1);
                }
                heal(4F);
                int c = entityData.get(CARINO) + 1;
                entityData.set(CARINO, c);
                playAction("happy");
                playSound(SoundEvents.HORSE_EAT, 1F, 1.2F);
                ((ServerLevel) level()).sendParticles(c >= 3 ? ParticleTypes.HEART : ParticleTypes.HAPPY_VILLAGER, getX(), getY() + 1.3, getZ(), 5,
                        0.4, 0.3, 0.4, 0.02);
                if (c == 3) {
                    p.displayClientMessage(Component.translatable("navidad.reno.amigo"), true);
                    if (p instanceof net.minecraft.server.level.ServerPlayer sp) {
                        com.navidad.Logros.dar(sp, "reno");
                        com.axeliscore.npc.Misiones.evento(sp, com.navidad.Navidad.MODID + ":reno_amigo", 1);
                    }
                }
            }
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        if (st.isEmpty() && !isVehicle() && !p.isSecondaryUseActive()) {
            if (!amigo()) {
                if (!level().isClientSide) {
                    p.displayClientMessage(Component.translatable("navidad.reno.timido"), true);
                }
                return InteractionResult.sidedSuccess(level().isClientSide);
            }
            if (!level().isClientSide) {
                p.setYRot(getYRot());
                p.startRiding(this);
            }
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        return super.mobInteract(p, hand);
    }

    // ------------------------------------------------------------------ montarlo

    @Override
    public LivingEntity getControllingPassenger() {
        return getFirstPassenger() instanceof Player p ? p : null;
    }

    @Override
    protected void tickRidden(Player p, Vec3 in) {
        super.tickRidden(p, in);
        setRot(p.getYRot(), p.getXRot() * 0.5F);
        this.yRotO = this.yBodyRot = this.yHeadRot = getYRot();
        // saltar: con la tecla de salto del jinete (se lee en el cliente que lo controla)
        if (level().isClientSide && onGround() && com.navidad.client.Montar.saltando()) {
            boolean magia = esNocheMagica();
            setDeltaMovement(getDeltaMovement().add(0, magia ? 0.9 : 0.6, 0));
        }
    }

    @Override
    protected Vec3 getRiddenInput(Player p, Vec3 in) {
        float lado = p.xxa * 0.5F, frente = p.zza;
        if (frente <= 0F) {
            frente *= 0.3F;
        }
        return new Vec3(lado, 0, frente);
    }

    @Override
    protected float getRiddenSpeed(Player p) {
        return (float) getAttributeValue(Attributes.MOVEMENT_SPEED) * 1.5F;
    }

    @Override
    public double getPassengersRidingOffset() {
        return 1.05;
    }

    @Override
    public void travel(Vec3 v) {
        super.travel(v);
        // de noche, en temporada, cae despacito (como si volara)
        if (isVehicle() && !onGround() && getDeltaMovement().y < -0.1 && esNocheMagica()) {
            setDeltaMovement(getDeltaMovement().multiply(1, 0.75, 1));
            if (level().isClientSide && tickCount % 2 == 0) {
                level().addParticle(ParticleTypes.END_ROD, getX(), getY() + 0.3, getZ(), 0, -0.02, 0);
            }
        }
    }

    private boolean esNocheMagica() {
        long t = level().getDayTime() % 24000L;
        boolean noche = t > 13000 && t < 23000;
        return noche && (level().isClientSide ? com.navidad.client.Montar.temporada() : level().getServer() != null
                && Temporada.activa(level().getServer()));
    }

    @Override
    public boolean causeFallDamage(float dist, float mult, DamageSource src) {
        return super.causeFallDamage(Math.max(0, dist - 6), mult, src);
    }

    @Override
    public boolean removeWhenFarAway(double d) {
        return !amigo();
    }

    // ------------------------------------------------------------------ animación y guardado

    @Override
    public void axAnimate(AnimHost h, float pt) {
        if (axDeath(h, "action")) {
            return;
        }
        axActions(h, n -> "action");
        axHurt(h, "action");
        axMove(h, 0.3F, 3.2F, 1.0F, 4.0F);
    }

    @Override
    public void axPose(AnimHost h, Pose pose, float pt) {
        if (deathTime <= 0 && !axDoing(h, "sniff") && !isVehicle()) {
            axLook(h, pose, pt, "head", "neck", 0.65F);
        }
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!level().isClientSide && !isVehicle() && getRandom().nextInt(900) == 0 && getNavigation().isDone()) {
            playAction(getRandom().nextBoolean() ? "sniff" : "happy");
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag t) {
        super.addAdditionalSaveData(t);
        t.putInt("Carino", entityData.get(CARINO));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag t) {
        super.readAdditionalSaveData(t);
        entityData.set(CARINO, t.getInt("Carino"));
    }

    @Override
    protected net.minecraft.sounds.SoundEvent getAmbientSound() {
        return SoundEvents.HORSE_BREATHE;
    }

    @Override
    protected net.minecraft.sounds.SoundEvent getHurtSound(DamageSource s) {
        return SoundEvents.HORSE_HURT;
    }

    @Override
    protected net.minecraft.sounds.SoundEvent getDeathSound() {
        return SoundEvents.HORSE_DEATH;
    }

    @Override
    protected void playStepSound(BlockPos p, net.minecraft.world.level.block.state.BlockState s) {
        playSound(SoundEvents.HORSE_STEP, 0.3F, 1.2F);
    }

    @Override
    public boolean isPushable() {
        return !isVehicle();
    }

    @Override
    protected void positionRider(Entity e, MoveFunction f) {
        super.positionRider(e, f);
    }
}
