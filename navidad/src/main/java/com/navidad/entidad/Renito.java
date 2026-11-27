package com.navidad.entidad;

import com.axeliscore.anim.AnimHost;
import com.axeliscore.anim.Animated;
import com.axeliscore.anim.Animator;
import com.axeliscore.anim.Clip;
import com.axeliscore.anim.Pose;
import com.navidad.Logros;
import com.navidad.Navidad;
import com.navidad.registry.ModRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowOwnerGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.SitWhenOrderedToGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;

/**
 * El renito: un reno bebé de nariz roja que brilla (se ve en la oscuridad). Sale de vez en cuando con las manadas de renos en la nieve.
 * Con zanahorias (a veces) o una zanahoria dorada (siempre) se vuelve tuyo: le aparece un collar rojo con cascabel, te sigue dando
 * brinquitos, se sienta cuando se lo pides (clic con la mano vacía) y de noche su nariz deja una estela de brillitos.
 */
public class Renito extends TamableAnimal implements Animated {
    private static final EntityDataAccessor<String> GESTO = SynchedEntityData.defineId(Renito.class, EntityDataSerializers.STRING);
    public static final Ingredient COMIDA = Ingredient.of(Items.CARROT, Items.GOLDEN_CARROT, Items.APPLE);
    private final AnimHost anim = new AnimHost();
    private int gestos;
    /** Para la prueba con fotos. */
    public static int domados;

    public Renito(EntityType<? extends TamableAnimal> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return TamableAnimal.createMobAttributes().add(Attributes.MAX_HEALTH, 12.0).add(Attributes.MOVEMENT_SPEED, 0.27);
    }

    public static boolean puedeSalir(EntityType<Renito> t, ServerLevelAccessor l, MobSpawnType why, BlockPos p, RandomSource r) {
        return Mob.checkMobSpawnRules(t, l, why, p, r) && l.getRawBrightness(p, 0) > 8;
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(GESTO, "");
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new PanicGoal(this, 1.7) {
            @Override
            public boolean canUse() {
                return !isTame() && super.canUse();
            }
        });
        this.goalSelector.addGoal(2, new SitWhenOrderedToGoal(this));
        this.goalSelector.addGoal(3, new TemptGoal(this, 1.15, COMIDA, false));
        this.goalSelector.addGoal(4, new FollowOwnerGoal(this, 1.3, 6F, 2F, false));
        this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.8));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 8F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
    }

    @Override
    public boolean isFood(ItemStack st) {
        return COMIDA.test(st);
    }

    @Override
    public boolean canFallInLove() {
        return false;
    }

    @Override
    public AgeableMob getBreedOffspring(ServerLevel l, AgeableMob otro) {
        return null;
    }

    public void gesto(String clip) {
        gestos++;
        this.entityData.set(GESTO, clip + "#" + gestos);
    }

    @Override
    public InteractionResult mobInteract(Player p, InteractionHand hand) {
        ItemStack st = p.getItemInHand(hand);
        boolean comida = isFood(st);
        if (level().isClientSide) {
            return comida || isOwnedBy(p) ? InteractionResult.CONSUME : InteractionResult.PASS;
        }
        if (!isTame() && comida) {
            boolean dorada = st.is(Items.GOLDEN_CARROT);
            usePlayerItem(p, hand, st);
            if ((dorada || random.nextInt(4) == 0) && !net.minecraftforge.event.ForgeEventFactory.onAnimalTame(this, p)) {
                tame(p);
                getNavigation().stop();
                setOrderedToSit(false);
                level().broadcastEntityEvent(this, (byte) 7);
                gesto("happy");
                domados++;
                p.displayClientMessage(Component.translatable("navidad.renito.tuyo").withStyle(ChatFormatting.RED), true);
                if (p instanceof ServerPlayer sp) {
                    Logros.dar(sp, "renito");
                }
                Navidad.LOGGER.info("NAVIDAD_RENITO domado por {}", p.getGameProfile().getName());
            } else {
                level().broadcastEntityEvent(this, (byte) 6);
                gesto("sniff");
            }
            return InteractionResult.SUCCESS;
        }
        if (isTame() && isOwnedBy(p)) {
            if (comida && getHealth() < getMaxHealth()) {
                usePlayerItem(p, hand, st);
                heal(4F);
                gesto("happy");
                return InteractionResult.SUCCESS;
            }
            if (st.isEmpty()) {
                boolean sienta = !isOrderedToSit();
                setOrderedToSit(sienta);
                setInSittingPose(sienta);
                getNavigation().stop();
                p.displayClientMessage(Component.translatable(sienta ? "navidad.renito.sienta" : "navidad.renito.sigue"), true);
                return InteractionResult.SUCCESS;
            }
        }
        return super.mobInteract(p, hand);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        long t = level().getDayTime() % 24000L;
        boolean noche = t > 12800 && t < 23200;
        if (level().isClientSide) {
            // la nariz brilla: de noche deja una estelita
            if (noche && tickCount % 3 == 0) {
                float a = yBodyRot * Mth.DEG_TO_RAD;
                double nx = getX() - Mth.sin(a) * 0.55, nz = getZ() + Mth.cos(a) * 0.55;
                level().addParticle(ParticleTypes.END_ROD, nx, getY() + 0.5, nz, 0, 0.005, 0);
            }
            return;
        }
        if (isTame() && !isOrderedToSit() && getNavigation().isDone() && random.nextInt(900) == 0) {
            gesto(random.nextBoolean() ? "happy" : "sniff");
        }
    }

    @Override
    public boolean causeFallDamage(float dist, float mult, DamageSource src) {
        return super.causeFallDamage(Math.max(0, dist - 4), mult, src);
    }

    @Override
    public boolean removeWhenFarAway(double d) {
        return !isTame() && !hasCustomName();
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.GOAT_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource s) {
        return SoundEvents.GOAT_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.GOAT_DEATH;
    }

    @Override
    public float getVoicePitch() {
        return 1.6F;
    }

    @Override
    protected void playStepSound(BlockPos p, BlockState s) {
        playSound(SoundEvents.GOAT_STEP, 0.15F, 1.5F);
        if (isTame() && tickCount % 4 == 0) {
            playSound(ModRegistry.CASCABEL.get(), 0.25F, 1.8F);
        }
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
                an.play("action", c, 0.15F, 1F, true);
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
        float raw = (float) (Math.sqrt(dx * dx + dz * dz) * 20);
        Object o = h.data.get("spd");
        float prev = o instanceof Float f ? f : raw;
        float spd = prev + (raw - prev) * 0.25F;
        h.data.put("spd", spd);
        if (isInSittingPose()) {
            an.play("base", h.clip("sit"), 0.3F, 1F, false);
        } else if (spd > 3.4F) {
            an.play("base", h.clip("run"), 0.2F, Mth.clamp(spd / 6F, 0.8F, 2.0F), false);
        } else if (spd > 0.3F) {
            an.play("base", h.clip("walk"), 0.25F, Mth.clamp(spd / 1.8F, 0.6F, 2.2F), false);
        } else {
            an.play("base", h.clip("idle"), 0.35F, 1F, false);
        }
    }

    @Override
    public void axPose(AnimHost h, Pose pose, float pt) {
        var geo = h.geo();
        int collar = geo.bone("collar");
        if (collar >= 0) {
            pose.hide(collar, !isTame());
        }
        if (deathTime <= 0 && !isInSittingPose()) {
            int head = geo.bone("head");
            if (head >= 0) {
                float yaw = Mth.clamp(Mth.wrapDegrees(Mth.rotLerp(pt, yHeadRotO, yHeadRot) - Mth.rotLerp(pt, yBodyRotO, yBodyRot)), -60F, 60F);
                float pitch = Mth.lerp(pt, xRotO, getXRot());
                pose.addRot(head, pitch * 0.6F * Mth.DEG_TO_RAD, yaw * Mth.DEG_TO_RAD, 0);
            }
        }
    }
}
