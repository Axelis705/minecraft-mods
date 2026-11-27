package com.navidad.entidad;

import com.axeliscore.anim.AnimHost;
import com.axeliscore.anim.Animated;
import com.axeliscore.anim.Animator;
import com.axeliscore.anim.Clip;
import com.axeliscore.anim.Pose;
import com.navidad.registry.ModRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.UUID;

/**
 * El Cascanueces guardián: un soldadito de madera que pones donde quieras (junto a tu árbol, por ejemplo). Se queda firme en su puesto;
 * de noche marcha haciendo su ronda alrededor; si se acerca un monstruo (menos los creepers, que explotan) lo saca a sablazos de bastón
 * de caramelo, y si llega el duende ladrón, lo atrapa (suelta el regalo). Si es tuyo: con la mano vacía te saluda; agachado, lo levantas.
 * Si lo tumban, suelta su figura para que lo vuelvas a poner. Se cura solo poco a poco.
 */
public class Cascanueces extends PathfinderMob implements Animated {
    private static final EntityDataAccessor<String> GESTO = SynchedEntityData.defineId(Cascanueces.class, EntityDataSerializers.STRING);
    private final AnimHost anim = new AnimHost();
    private int gestos;
    private UUID dueno;
    private BlockPos puesto;
    /** Para la prueba con fotos. */
    public static int puestos, golpes;

    public Cascanueces(EntityType<? extends PathfinderMob> t, Level l) {
        super(t, l);
        setPersistenceRequired();
        setMaxUpStep(1.0F);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes().add(Attributes.MAX_HEALTH, 30).add(Attributes.MOVEMENT_SPEED, 0.28)
                .add(Attributes.ATTACK_DAMAGE, 4).add(Attributes.ARMOR, 4).add(Attributes.KNOCKBACK_RESISTANCE, 0.5)
                .add(Attributes.FOLLOW_RANGE, 18);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(GESTO, "");
    }

    public void gesto(String clip) {
        gestos++;
        this.entityData.set(GESTO, clip + "#" + gestos);
    }

    public void ponerEn(Player p, BlockPos pos) {
        dueno = p.getUUID();
        puesto = pos.immutable();
        puestos++;
    }

    /** Quién lo puso (si está conectado). */
    public Player dueno() {
        return dueno == null ? null : level().getPlayerByUUID(dueno);
    }

    public BlockPos puesto() {
        return puesto == null ? blockPosition() : puesto;
    }

    private boolean cercaDelPuesto(Entity e, double r) {
        return e.distanceToSqr(Vec3.atCenterOf(puesto())) < r * r;
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.15, true));
        goalSelector.addGoal(4, new VolverGoal());
        goalSelector.addGoal(5, new RondaGoal());
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 8F));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, DuendeLadron.class, 10, true, false, e -> cercaDelPuesto(e, 16)));
        targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Monster.class, 10, true, false,
                e -> !(e instanceof Creeper) && cercaDelPuesto(e, 12)));
    }

    /** Si se aleja mucho de su puesto (persiguiendo algo), regresa. */
    private class VolverGoal extends Goal {
        VolverGoal() {
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return getTarget() == null && distanceToSqr(Vec3.atBottomCenterOf(puesto())) > 3 * 3;
        }

        @Override
        public void start() {
            BlockPos p = puesto();
            getNavigation().moveTo(p.getX() + 0.5, p.getY(), p.getZ() + 0.5, 0.9);
        }

        @Override
        public boolean canContinueToUse() {
            return getTarget() == null && !getNavigation().isDone();
        }
    }

    /** De noche, la ronda: marcha a puntos alrededor de su puesto. */
    private class RondaGoal extends Goal {
        private int espera;

        RondaGoal() {
            setFlags(EnumSet.of(Flag.MOVE));
        }

        private boolean noche() {
            long t = level().getDayTime() % 24000L;
            return t > 12800 && t < 23200;
        }

        @Override
        public boolean canUse() {
            if (getTarget() != null || !noche()) {
                return false;
            }
            if (espera > 0) {
                espera--;
                return false;
            }
            return true;
        }

        @Override
        public void start() {
            double a = random.nextDouble() * Math.PI * 2, r = 2.5 + random.nextDouble() * 3.5;
            BlockPos p = puesto();
            getNavigation().moveTo(p.getX() + 0.5 + Math.cos(a) * r, p.getY(), p.getZ() + 0.5 + Math.sin(a) * r, 0.7);
            espera = 40 + random.nextInt(60);
        }

        @Override
        public boolean canContinueToUse() {
            return getTarget() == null && !getNavigation().isDone();
        }
    }

    @Override
    public boolean doHurtTarget(Entity e) {
        gesto("attack");
        level().playSound(null, blockPosition(), ModRegistry.CASCANUECES_CLAC.get(), SoundSource.NEUTRAL, 1F, 0.9F + random.nextFloat() * 0.2F);
        golpes++;
        return super.doHurtTarget(e);
    }

    @Override
    public boolean hurt(DamageSource src, float n) {
        // no le hace caso a su dueño ni a las mascotas de nadie
        if (src.getEntity() instanceof Player p && p.getUUID().equals(dueno) && !p.isShiftKeyDown()) {
            return false;
        }
        return super.hurt(src, n);
    }

    @Override
    public boolean canAttack(LivingEntity e) {
        return !(e instanceof Player) && super.canAttack(e);
    }

    @Override
    protected InteractionResult mobInteract(Player p, InteractionHand h) {
        if (h != InteractionHand.MAIN_HAND || !p.getItemInHand(h).isEmpty()) {
            return InteractionResult.PASS;
        }
        if (level().isClientSide) {
            return InteractionResult.SUCCESS;
        }
        boolean mio = dueno == null || p.getUUID().equals(dueno);
        if (p.isShiftKeyDown() && mio) {
            ItemStack st = new ItemStack(ModRegistry.CASCANUECES_ITEM.get());
            if (!p.getInventory().add(st)) {
                p.drop(st, false);
            }
            ((ServerLevel) level()).sendParticles(ParticleTypes.POOF, getX(), getY() + 1, getZ(), 10, 0.3, 0.5, 0.3, 0.02);
            level().playSound(null, blockPosition(), SoundEvents.ARMOR_STAND_BREAK, SoundSource.NEUTRAL, 0.8F, 1.3F);
            discard();
            return InteractionResult.CONSUME;
        }
        gesto("salute");
        level().playSound(null, blockPosition(), ModRegistry.CASCANUECES_CLAC.get(), SoundSource.NEUTRAL, 0.8F, 1.3F);
        p.displayClientMessage(Component.translatable(mio ? "navidad.cascanueces.ordenes" : "navidad.cascanueces.de_otro")
                .withStyle(ChatFormatting.GOLD), true);
        return InteractionResult.CONSUME;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!level().isClientSide && tickCount % 40 == 0 && getTarget() == null && getHealth() < getMaxHealth()) {
            heal(1F);
        }
    }

    @Override
    protected void dropCustomDeathLoot(DamageSource src, int looting, boolean byPlayer) {
        super.dropCustomDeathLoot(src, looting, byPlayer);
        spawnAtLocation(new ItemStack(ModRegistry.CASCANUECES_ITEM.get()));
        Player d = dueno();
        if (d instanceof ServerPlayer sp) {
            sp.displayClientMessage(Component.translatable("navidad.cascanueces.cayo").withStyle(ChatFormatting.GRAY), false);
        }
    }

    @Override
    public boolean removeWhenFarAway(double d) {
        return false;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource s) {
        return SoundEvents.WOOD_HIT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.WOOD_BREAK;
    }

    @Override
    protected void playStepSound(BlockPos p, BlockState s) {
        playSound(SoundEvents.WOOD_STEP, 0.4F, 1.5F);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag t) {
        super.addAdditionalSaveData(t);
        if (dueno != null) {
            t.putUUID("Dueno", dueno);
        }
        if (puesto != null) {
            t.putIntArray("Puesto", new int[]{puesto.getX(), puesto.getY(), puesto.getZ()});
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag t) {
        super.readAdditionalSaveData(t);
        dueno = t.hasUUID("Dueno") ? t.getUUID("Dueno") : null;
        int[] p = t.getIntArray("Puesto");
        puesto = p.length == 3 ? new BlockPos(p[0], p[1], p[2]) : null;
    }

    // ------------------------------------------------------------------ animación (cliente)

    @Override
    public AnimHost axHost() {
        return anim;
    }

    @Override
    public void axAnimate(AnimHost h, float pt) {
        Animator an = h.animator();
        if (deathTime > 0) {
            if (!Boolean.TRUE.equals(h.data.get("muerto"))) {
                h.data.put("muerto", true);
                an.stop("action", 0.1F);
                h.play("base", "death", 0.1F);
            }
            return;
        }
        String g = this.entityData.get(GESTO);
        if (!g.equals(h.data.get("gesto"))) {
            h.data.put("gesto", g);
            String[] p = g.split("#");
            Clip c = p.length > 0 && !p[0].isEmpty() ? h.clip(p[0]) : null;
            if (c != null && h.data.containsKey("listo")) {
                an.play("action", c, 0.1F, 1F, true);
            }
        }
        h.data.put("listo", true);
        if (hurtTime > 0 && hurtTime == hurtDuration && !Integer.valueOf(tickCount).equals(h.data.get("ay"))) {
            h.data.put("ay", tickCount);
            Clip c = h.clip("hurt");
            if (c != null && an.finished("action")) {
                an.play("action", c, 0.05F, 1F, true);
            }
        }
        double dx = getX() - xo, dz = getZ() - zo;
        float vel = (float) (Math.sqrt(dx * dx + dz * dz) * 20);
        if (vel > 0.4F) {
            an.play("base", h.clip("walk"), 0.2F, Mth.clamp(vel / 3.2F, 0.7F, 1.8F), false);
        } else {
            an.play("base", h.clip("idle"), 0.3F, 1F, false);
        }
    }

    @Override
    public void axPose(AnimHost h, Pose pose, float pt) {
        if (deathTime > 0) {
            return;
        }
        int cab = h.geo().bone("cabeza");
        if (cab >= 0) {
            float yaw = Mth.clamp(Mth.wrapDegrees(Mth.rotLerp(pt, yHeadRotO, yHeadRot) - Mth.rotLerp(pt, yBodyRotO, yBodyRot)), -45F, 45F);
            pose.addRot(cab, Mth.lerp(pt, xRotO, getXRot()) * 0.4F * Mth.DEG_TO_RAD, yaw * Mth.DEG_TO_RAD, 0);
        }
    }
}
