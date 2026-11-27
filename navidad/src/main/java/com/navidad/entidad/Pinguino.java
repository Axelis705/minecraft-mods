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
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowOwnerGoal;
import net.minecraft.world.entity.ai.goal.FollowParentGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.SitWhenOrderedToGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * El pingüino, la mascota del mod. Sale en grupitos (con bebés) en la nieve y en las costas heladas. Le encanta el pescado: si le das,
 * a veces se vuelve tuyo (entonces lleva bufanda y gorrito de Santa), te sigue, se sienta cuando se lo pides (clic con la mano vacía),
 * nada rápido y, cuando va con prisa sobre hielo o nieve, <b>se avienta de panza</b>. Si tiene agua cerca, de vez en cuando pesca algo y te
 * lo trae. Con pescado también se cruzan (y el bebé de dos pingüinos tuyos también es tuyo).
 */
public class Pinguino extends TamableAnimal implements Animated {
    private static final EntityDataAccessor<Boolean> DESLIZA = SynchedEntityData.defineId(Pinguino.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<String> GESTO = SynchedEntityData.defineId(Pinguino.class, EntityDataSerializers.STRING);
    public static final Ingredient PESCADO = Ingredient.of(Items.COD, Items.SALMON, Items.TROPICAL_FISH, Items.COOKED_COD, Items.COOKED_SALMON);

    private final AnimHost anim = new AnimHost();
    private int gestos, pesca = 2400, deslizando;
    /** Cuántos pingüinos se han hecho de alguien (para la prueba con fotos). */
    public static int domados;

    public Pinguino(EntityType<? extends TamableAnimal> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return TamableAnimal.createMobAttributes().add(Attributes.MAX_HEALTH, 10.0).add(Attributes.MOVEMENT_SPEED, 0.22);
    }

    /** Sale en la nieve, en el hielo o en la arena de las costas frías (no hace falta pasto como a los demás animales). */
    public static boolean puedeSalir(EntityType<Pinguino> t, ServerLevelAccessor l, MobSpawnType why, BlockPos p, RandomSource r) {
        BlockState abajo = l.getBlockState(p.below());
        return (abajo.is(Blocks.SNOW_BLOCK) || abajo.is(Blocks.GRASS_BLOCK) || abajo.is(BlockTags.ICE) || abajo.is(Blocks.SAND)
                || abajo.is(Blocks.GRAVEL) || abajo.is(Blocks.STONE) || abajo.is(Blocks.POWDER_SNOW)) && l.getRawBrightness(p, 0) > 7;
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DESLIZA, false);
        this.entityData.define(GESTO, "");
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new PanicGoal(this, 1.6) {
            @Override
            public boolean canUse() {
                return !isTame() && super.canUse();
            }
        });
        this.goalSelector.addGoal(2, new SitWhenOrderedToGoal(this));
        this.goalSelector.addGoal(3, new BreedGoal(this, 1.0));
        this.goalSelector.addGoal(4, new TemptGoal(this, 1.15, PESCADO, false));
        this.goalSelector.addGoal(5, new FollowOwnerGoal(this, 1.25, 6F, 2F, false));
        this.goalSelector.addGoal(6, new FollowParentGoal(this, 1.1));
        this.goalSelector.addGoal(7, new RandomStrollGoal(this, 0.8));
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 7F));
        this.goalSelector.addGoal(9, new RandomLookAroundGoal(this));
    }

    @Override
    public boolean isFood(ItemStack st) {
        return PESCADO.test(st);
    }

    // ------------------------------------------------------------------ darle de comer, domarlo y pedirle que se siente

    @Override
    public InteractionResult mobInteract(Player p, InteractionHand hand) {
        ItemStack st = p.getItemInHand(hand);
        boolean pescado = isFood(st);
        if (level().isClientSide) {
            return pescado || isOwnedBy(p) ? InteractionResult.CONSUME : InteractionResult.PASS;
        }
        if (!isTame() && pescado && !isBaby()) {
            usePlayerItem(p, hand, st);
            if (random.nextInt(3) == 0 && !net.minecraftforge.event.ForgeEventFactory.onAnimalTame(this, p)) {
                tame(p);
                getNavigation().stop();
                setOrderedToSit(false);
                level().broadcastEntityEvent(this, (byte) 7);
                gesto("happy");
                domados++;
                p.displayClientMessage(Component.translatable("navidad.pinguino.tuyo").withStyle(ChatFormatting.AQUA), true);
                if (p instanceof ServerPlayer sp) {
                    Logros.dar(sp, "pinguino");
                }
                Navidad.LOGGER.info("NAVIDAD_PINGUINO domado por {}", p.getGameProfile().getName());
            } else {
                level().broadcastEntityEvent(this, (byte) 6);
                gesto("flap");
            }
            return InteractionResult.SUCCESS;
        }
        if (isTame() && isOwnedBy(p)) {
            if (pescado && getHealth() < getMaxHealth()) {
                usePlayerItem(p, hand, st);
                heal(4F);
                gesto("happy");
                return InteractionResult.SUCCESS;
            }
            if (st.isEmpty()) {
                boolean sienta = !isOrderedToSit();
                setOrderedToSit(sienta);
                setInSittingPose(sienta);
                this.jumping = false;
                getNavigation().stop();
                setTarget(null);
                p.displayClientMessage(Component.translatable(sienta ? "navidad.pinguino.sienta" : "navidad.pinguino.sigue"), true);
                return InteractionResult.SUCCESS;
            }
        }
        return super.mobInteract(p, hand);
    }

    @Override
    public AgeableMob getBreedOffspring(ServerLevel l, AgeableMob otro) {
        Pinguino hijo = ModRegistry.PINGUINO.get().create(l);
        if (hijo != null && isTame() && getOwnerUUID() != null) {
            hijo.setOwnerUUID(getOwnerUUID());
            hijo.setTame(true);
        }
        return hijo;
    }

    /** (Servidor) Un gesto que ven todos (feliz, aleteo). */
    public void gesto(String clip) {
        gestos++;
        this.entityData.set(GESTO, clip + "#" + gestos);
    }

    // ------------------------------------------------------------------ cada tick: deslizarse de panza, nadar rápido y pescar

    private boolean resbaloso(BlockState b) {
        return b.is(BlockTags.ICE) || b.is(Blocks.SNOW_BLOCK) || b.is(Blocks.SNOW) || b.is(Blocks.POWDER_SNOW);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide) {
            return;
        }
        // de panza: cuando va con prisa (siguiéndote de lejos o huyendo) sobre hielo o nieve
        BlockState suelo = level().getBlockState(blockPosition().below());
        BlockState aqui = level().getBlockState(blockPosition());
        double v = getDeltaMovement().horizontalDistance();
        boolean prisa = v > 0.11 && (getTarget() != null || getLastHurtByMob() != null && tickCount - getLastHurtByMobTimestamp() < 100 || isTame() && getOwner() != null && distanceToSqr(getOwner()) > 25);
        boolean panza = onGround() && (resbaloso(suelo) || resbaloso(aqui)) && prisa && !isInSittingPose() && !isBaby();
        deslizando = panza ? Math.min(deslizando + 1, 40) : Math.max(deslizando - 2, 0);
        boolean desliza = deslizando > 4;
        if (desliza != this.entityData.get(DESLIZA)) {
            this.entityData.set(DESLIZA, desliza);
            if (desliza) {
                playSound(SoundEvents.SNOW_STEP, 0.8F, 1.4F);
            }
        }
        if (desliza) {
            // la panza resbala: un empujoncito hacia adelante (sin pasarse)
            Vec3 m = getDeltaMovement();
            if (m.horizontalDistance() < 0.42) {
                setDeltaMovement(m.x * 1.12, m.y, m.z * 1.12);
            }
            if (tickCount % 3 == 0) {
                ((ServerLevel) level()).sendParticles(ParticleTypes.SNOWFLAKE, getX(), getY() + 0.1, getZ(), 2, 0.2, 0.02, 0.2, 0.01);
            }
        }
        // en el agua es rapidísimo
        if (isInWater()) {
            Vec3 m = getDeltaMovement();
            if (m.horizontalDistance() > 0.02 && m.horizontalDistance() < 0.3) {
                setDeltaMovement(m.x * 1.18, m.y, m.z * 1.18);
            }
        }
        // los tuyos, si tienen agua cerca, de vez en cuando pescan algo y te lo traen
        if (isTame() && !isOrderedToSit() && --pesca <= 0) {
            pesca = 4800 + random.nextInt(4800);
            LivingEntity duenio = getOwner();
            if (duenio instanceof ServerPlayer dp && dp.distanceToSqr(this) < 14 * 14 && aguaCerca()) {
                traerPescado(dp);
            }
        }
        // de vez en cuando, si está quieto, aletea
        if (!isOrderedToSit() && getNavigation().isDone() && random.nextInt(1200) == 0) {
            gesto("flap");
        }
    }

    private boolean aguaCerca() {
        if (isInWater()) {
            return true;
        }
        for (BlockPos q : BlockPos.betweenClosed(blockPosition().offset(-5, -2, -5), blockPosition().offset(5, 1, 5))) {
            if (level().getFluidState(q).isSource()) {
                return true;
            }
        }
        return false;
    }

    /** Le trae a su dueño un pescado (y muy rara vez algo brillante del fondo del mar). */
    public void traerPescado(ServerPlayer dp) {
        float x = random.nextFloat();
        ItemStack st = new ItemStack(x < 0.04F ? Items.NAUTILUS_SHELL : x < 0.12F ? Items.PRISMARINE_CRYSTALS : x < 0.45F ? Items.SALMON : Items.COD);
        ItemEntity it = new ItemEntity(level(), dp.getX(), dp.getY() + 0.5, dp.getZ(), st);
        it.setPickUpDelay(10);
        level().addFreshEntity(it);
        gesto("happy");
        playSound(ModRegistry.PINGUINO_FELIZ.get(), 1F, 1F);
        dp.displayClientMessage(Component.translatable("navidad.pinguino.pesco", st.getHoverName()).withStyle(ChatFormatting.AQUA), true);
        Navidad.LOGGER.info("NAVIDAD_PINGUINO pesco {}", st.getItem());
    }

    @Override
    public boolean causeFallDamage(float dist, float mult, DamageSource src) {
        return super.causeFallDamage(Math.max(0, dist - 3), mult, src);
    }

    @Override
    public boolean canBreatheUnderwater() {
        return false;
    }

    @Override
    public int getMaxAirSupply() {
        return 2400;
    }

    @Override
    public boolean removeWhenFarAway(double d) {
        return !isTame() && !hasCustomName();
    }

    // ------------------------------------------------------------------ sonidos

    @Override
    protected SoundEvent getAmbientSound() {
        return isInSittingPose() && random.nextBoolean() ? null : ModRegistry.PINGUINO_HOLA.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource s) {
        return ModRegistry.PINGUINO_LASTIMA.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModRegistry.PINGUINO_LASTIMA.get();
    }

    @Override
    public float getVoicePitch() {
        return isBaby() ? 1.5F : super.getVoicePitch();
    }

    @Override
    protected void playStepSound(BlockPos p, BlockState s) {
        playSound(SoundEvents.CHICKEN_STEP, 0.15F, 1.4F);
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
        // gestos que manda el servidor
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
        // la capa de abajo: nadar, de panza, sentado/dormido, o caminar según la velocidad
        double dx = getX() - xo, dz = getZ() - zo;
        float raw = (float) (Math.sqrt(dx * dx + dz * dz) * 20);
        Object o = h.data.get("spd");
        float spd = (o instanceof Float f ? f : raw) + (raw - (o instanceof Float f2 ? f2 : raw)) * 0.25F;
        h.data.put("spd", spd);
        if (isInWater()) {
            an.play("base", h.clip("swim"), 0.3F, 1F, false);
        } else if (this.entityData.get(DESLIZA)) {
            an.play("base", h.clip("slide"), 0.2F, 1F, false);
        } else if (isInSittingPose()) {
            long t = level().getDayTime() % 24000L;
            an.play("base", h.clip(t > 13000 && t < 23000 ? "sleep" : "sit"), 0.3F, 1F, false);
        } else if (spd > 3.0F) {
            an.play("base", h.clip("run"), 0.2F, Mth.clamp(spd / 4.5F, 0.7F, 1.8F), false);
        } else if (spd > 0.25F) {
            an.play("base", h.clip("walk"), 0.25F, Mth.clamp(spd / 1.6F, 0.6F, 2.2F), false);
        } else {
            an.play("base", h.clip("idle"), 0.35F, 1F, false);
        }
    }

    @Override
    public void axPose(AnimHost h, Pose pose, float pt) {
        var geo = h.geo();
        // la bufanda y el gorrito solo cuando es de alguien
        int b = geo.bone("bufanda"), gorro = geo.bone("gorro");
        if (b >= 0) {
            pose.hide(b, !isTame());
        }
        if (gorro >= 0) {
            pose.hide(gorro, !isTame());
        }
        // los bebés: plumón gris
        if (isBaby()) {
            for (String n : new String[]{"body", "head", "flipper_r", "flipper_l"}) {
                int k = geo.bone(n);
                if (k >= 0) {
                    pose.tinte(k, 0xFFB4B8C4);
                }
            }
        }
        // mirar (con la cabeza) hacia donde mira, si no está de panza ni nadando
        if (deathTime <= 0 && !this.entityData.get(DESLIZA) && !isInWater() && !isInSittingPose()) {
            int head = geo.bone("head");
            if (head >= 0) {
                float yaw = Mth.clamp(Mth.wrapDegrees(Mth.rotLerp(pt, yHeadRotO, yHeadRot) - Mth.rotLerp(pt, yBodyRotO, yBodyRot)), -60F, 60F);
                float pitch = Mth.lerp(pt, xRotO, getXRot());
                float r = (float) Math.PI / 180F;
                pose.addRot(head, pitch * 0.6F * r, yaw * r, 0);
            }
        }
    }

    // ------------------------------------------------------------------ guardado

    @Override
    public void addAdditionalSaveData(CompoundTag t) {
        super.addAdditionalSaveData(t);
        t.putInt("Pesca", pesca);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag t) {
        super.readAdditionalSaveData(t);
        if (t.contains("Pesca")) {
            pesca = t.getInt("Pesca");
        }
    }
}
