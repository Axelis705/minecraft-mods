package com.mundosdeaxelis.entity;

import com.mundosdeaxelis.block.VaultLoot;
import com.mundosdeaxelis.registry.ModItems;
import com.mundosdeaxelis.world.DungeonBalance;
import com.mundosdeaxelis.world.Sellos;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Etapa 272: el <b>Mímico</b> (como en Terraria, Dark Souls o Elden Ring). Duerme disfrazado de cofre del santuario de su tier,
 * con su haz de luz y todo. Si lo intentas abrir, lo golpeas o te pegas demasiado, despierta: le salen patas, abre la tapa llena
 * de dientes, salta hacia ti y muerde. De vez en cuando, dormido, «respira» (la tapa tiembla un poco): un jugador atento lo nota.
 * Al morir suelta un botín de cofre de prueba de su tier y una pieza buena (fragmento de corazón, gema vacía o fragmentos de llave),
 * solo si el jugador ya puede saquear ese tier (ver {@link Sellos}).
 */
public class Mimico extends Monster {
    private static final EntityDataAccessor<Boolean> AWAKE = SynchedEntityData.defineId(Mimico.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> TIER = SynchedEntityData.defineId(Mimico.class, EntityDataSerializers.INT);
    /** Golpes de espada B que aguanta y daño base (etapa 1). */
    private static final float HITS = 6.0F;
    private static final float BASE_DAMAGE = 6.0F;
    /** Eventos de entidad: mordisco (abre la tapa del todo) y respiración (la tapa tiembla). */
    public static final byte EV_BITE = 4, EV_BREATHE = 61, EV_WAKE = 62;

    private boolean oriented;
    /** Ya se ajustaron vida y daño a su tier (al ponerlo desde una estructura o con /summon {Tier:N}). */
    private boolean scaled;
    private float sleepYaw;
    private int leapCooldown = 40;
    private boolean leaping;
    private int breatheIn = 120;

    // solo en el cliente: animaciones
    public float wakeAnim, wakeAnimO;
    public int biteTicks, breatheTicks;

    public Mimico(EntityType<? extends Mimico> type, Level level) {
        super(type, level);
        this.xpReward = 12;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, DungeonBalance.hp(1, HITS))
                .add(Attributes.MOVEMENT_SPEED, 0.27D)
                .add(Attributes.ATTACK_DAMAGE, DungeonBalance.dmg(1, BASE_DAMAGE, 1))
                .add(Attributes.ARMOR, 4.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.6D)
                .add(Attributes.FOLLOW_RANGE, 24.0D);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(AWAKE, false);
        this.entityData.define(TIER, 1);
    }

    public boolean isAwake() {
        return this.entityData.get(AWAKE);
    }

    public int tier() {
        return this.entityData.get(TIER);
    }

    /** Pone el tier (1 a 8) y ajusta vida y daño a esa etapa. */
    public void setTier(int t) {
        t = Mth.clamp(t, 1, 8);
        this.entityData.set(TIER, t);
        float hp = DungeonBalance.hp(t, HITS);
        getAttribute(Attributes.MAX_HEALTH).setBaseValue(hp);
        setHealth(hp);
        getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(DungeonBalance.dmg(t, BASE_DAMAGE, 1));
        getAttribute(Attributes.ARMOR).setBaseValue(2.0D + 2.0D * t);
        this.xpReward = 8 + 6 * t;
        this.scaled = true;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.25D, true) {
            @Override
            public boolean canUse() {
                return isAwake() && super.canUse();
            }

            @Override
            public boolean canContinueToUse() {
                return isAwake() && super.canContinueToUse();
            }
        });
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this) {
            @Override
            public boolean canUse() {
                return isAwake() && super.canUse();
            }
        });
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true) {
            @Override
            public boolean canUse() {
                return isAwake() && super.canUse();
            }
        });
    }

    // ------------------------------------------------------------------ despertar

    /** Despierta (una sola vez): grito, tapa abierta, salto hacia quien lo despertó y, si está pegado, un mordisco. */
    public void wake(@Nullable LivingEntity by) {
        if (isAwake() || !(level() instanceof ServerLevel sl)) {
            return;
        }
        this.entityData.set(AWAKE, true);
        sl.broadcastEntityEvent(this, EV_WAKE);
        playSound(SoundEvents.CHEST_OPEN, 1.0F, 0.55F);
        playSound(SoundEvents.RAVAGER_ROAR, 0.7F, 1.7F);
        sl.sendParticles(ParticleTypes.POOF, getX(), getY() + 0.5, getZ(), 14, 0.4, 0.3, 0.4, 0.04);
        sl.sendParticles(ParticleTypes.ANGRY_VILLAGER, getX(), getY() + 1.1, getZ(), 3, 0.3, 0.1, 0.3, 0.0);
        if (by != null && !(by instanceof Player p && (p.isCreative() || p.isSpectator()))) {
            setTarget(by);
            if (by instanceof ServerPlayer sp) {
                sp.displayClientMessage(Component.translatable("message.mundos_de_axelis.mimico.despierta").withStyle(ChatFormatting.RED), true);
            }
            if (distanceToSqr(by) < 4.5D) {
                doHurtTarget(by);
            }
            Vec3 d = by.position().subtract(position()).multiply(1, 0, 1);
            if (d.lengthSqr() > 1.0E-4D) {
                d = d.normalize();
                setDeltaMovement(d.x * 0.35D, 0.42D, d.z * 0.35D);
            }
        }
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!isAwake()) {
            if (!level().isClientSide) {
                wake(player);
            }
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        return super.mobInteract(player, hand);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!isAwake() && !level().isClientSide && !source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            Entity e = source.getEntity();
            wake(e instanceof LivingEntity le ? le : null);
        }
        return super.hurt(source, amount);
    }

    // ------------------------------------------------------------------ cada tick

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            wakeAnimO = wakeAnim;
            wakeAnim = Mth.clamp(wakeAnim + (isAwake() ? 0.16F : -0.1F), 0.0F, 1.0F);
            if (biteTicks > 0) {
                biteTicks--;
            }
            if (breatheTicks > 0) {
                breatheTicks--;
            }
            if (isAwake() && tickCount % 9 == 0) {
                level().addParticle(ParticleTypes.FALLING_HONEY, getX() + (random.nextDouble() - 0.5) * 0.5, getY() + 0.55,
                        getZ() + (random.nextDouble() - 0.5) * 0.5, 0, 0, 0);
            }
            return;
        }
        if (!scaled) {
            // el tier viene de la estructura (marca «mimico_N») o de /summon … {Tier:N}
            String tag = getPersistentData().getString(com.mundosdeaxelis.world.StructPlacer.TAG);
            int t = tier();
            if (tag.startsWith("mimico_")) {
                try {
                    t = Integer.parseInt(tag.substring(7));
                } catch (NumberFormatException ignored) {
                }
            }
            setTier(t);
        }
        if (!oriented) {
            oriented = true;
            orient();
        }
        if (!isAwake()) {
            asleepTick();
        } else {
            awakeTick();
        }
    }

    /** Dormido: quieto como un cofre, mirando a donde mira; a veces «respira»; si alguien se pega mucho, despierta. */
    private void asleepTick() {
        setYRot(sleepYaw);
        setYBodyRot(sleepYaw);
        setYHeadRot(sleepYaw);
        yRotO = sleepYaw;
        yBodyRotO = sleepYaw;
        setDeltaMovement(0.0D, getDeltaMovement().y, 0.0D);
        getNavigation().stop();
        setTarget(null);
        Player near = level().getNearestPlayer(this, 8.0D);
        if (near == null || near.isCreative() || near.isSpectator()) {
            return;
        }
        if (--breatheIn <= 0) {
            breatheIn = 100 + random.nextInt(120);
            level().broadcastEntityEvent(this, EV_BREATHE);
            playSound(SoundEvents.CHEST_CLOSE, 0.25F, 0.55F);
        }
        double d = near.distanceToSqr(this);
        if (d < 1.2D * 1.2D || (d < 2.0D * 2.0D && !near.isShiftKeyDown() && tickCount % 10 == 0 && random.nextInt(4) == 0)) {
            wake(near);
        }
    }

    /** Despierto: va dando saltitos y, si su presa está a media distancia, salta sobre ella y cae con un golpe. */
    private void awakeTick() {
        LivingEntity t = getTarget();
        if (onGround()) {
            if (leaping) {
                leaping = false;
                slam();
            } else if (t != null && getNavigation().isInProgress() && tickCount % 10 == 0) {
                jumpFromGround();
                playSound(SoundEvents.CHEST_CLOSE, 0.35F, 1.6F + random.nextFloat() * 0.2F);
            }
        }
        if (leapCooldown > 0) {
            leapCooldown--;
        } else if (t != null && onGround() && hasLineOfSight(t)) {
            double d = distanceTo(t);
            if (d > 3.0D && d < 9.0D) {
                Vec3 dir = t.position().subtract(position()).multiply(1, 0, 1).normalize();
                double sp = Math.min(1.0D, 0.18D * d);
                setDeltaMovement(dir.x * sp, 0.62D, dir.z * sp);
                hasImpulse = true;
                leaping = true;
                leapCooldown = 90 + random.nextInt(40);
                level().broadcastEntityEvent(this, EV_BITE);
                playSound(SoundEvents.RAVAGER_ATTACK, 0.7F, 1.6F);
            }
        }
    }

    /** La caída del salto: golpe a los que estén muy cerca. */
    private void slam() {
        if (!(level() instanceof ServerLevel sl)) {
            return;
        }
        sl.sendParticles(ParticleTypes.POOF, getX(), getY() + 0.1, getZ(), 18, 0.8, 0.05, 0.8, 0.05);
        sl.sendParticles(ParticleTypes.CRIT, getX(), getY() + 0.3, getZ(), 12, 0.8, 0.1, 0.8, 0.2);
        playSound(SoundEvents.ANVIL_LAND, 0.35F, 0.8F);
        float dmg = (float) getAttributeValue(Attributes.ATTACK_DAMAGE) * 0.6F;
        for (Player p : sl.getEntitiesOfClass(Player.class, getBoundingBox().inflate(1.6D, 0.5D, 1.6D))) {
            if (!p.isCreative() && !p.isSpectator()) {
                p.hurt(damageSources().mobAttack(this), dmg);
                Vec3 k = p.position().subtract(position()).multiply(1, 0, 1).normalize().scale(0.5D);
                p.push(k.x, 0.25D, k.z);
            }
        }
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean ok = super.doHurtTarget(target);
        if (ok) {
            level().broadcastEntityEvent(this, EV_BITE);
            playSound(SoundEvents.EVOKER_FANGS_ATTACK, 0.8F, 1.2F);
            playSound(SoundEvents.CHEST_CLOSE, 0.8F, 1.3F);
        }
        return ok;
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == EV_BITE) {
            biteTicks = 10;
        } else if (id == EV_BREATHE) {
            breatheTicks = 14;
        } else if (id == EV_WAKE) {
            biteTicks = 14;
            for (int i = 0; i < 12; i++) {
                level().addParticle(ParticleTypes.SMOKE, getX() + (random.nextDouble() - 0.5), getY() + 0.6, getZ() + (random.nextDouble() - 0.5), 0, 0.05, 0);
            }
        } else {
            super.handleEntityEvent(id);
        }
    }

    /** Mira hacia el lado más abierto (con el lomo contra la pared), como un cofre bien puesto, en una dirección recta. */
    private void orient() {
        Direction best = Direction.from2DDataValue(Math.floorMod(Math.round(getYRot() / 90.0F), 4));
        int bestScore = -1;
        BlockPos p = blockPosition();
        for (Direction d : Direction.Plane.HORIZONTAL) {
            int s = 0;
            if (level().getBlockState(p.relative(d)).getCollisionShape(level(), p.relative(d)).isEmpty()) {
                s += 2;
                if (level().getBlockState(p.relative(d, 2)).getCollisionShape(level(), p.relative(d, 2)).isEmpty()) {
                    s += 1;
                }
            }
            if (!level().getBlockState(p.relative(d.getOpposite())).getCollisionShape(level(), p.relative(d.getOpposite())).isEmpty()) {
                s += 2;
            }
            if (s > bestScore) {
                bestScore = s;
                best = d;
            }
        }
        sleepYaw = best.toYRot();
        moveTo(p.getX() + 0.5D, getY(), p.getZ() + 0.5D, sleepYaw, 0.0F);
    }

    // ------------------------------------------------------------------ botín

    /** El botín de un mímico de ese tier: un cofre de prueba más una pieza buena segura. */
    public static List<ItemStack> loot(int tier, net.minecraft.util.RandomSource r, @Nullable java.util.Set<String> archive) {
        List<ItemStack> out = new ArrayList<>(VaultLoot.roll(VaultLoot.Kind.TRIAL, tier, r, 0, archive));
        int roll = r.nextInt(100);
        if (roll < 30) {
            out.add(new ItemStack(ModItems.HEART_FRAGMENT.get()));
        } else if (roll < 65) {
            out.add(VaultLoot.emptyGem(tier));
        } else {
            com.mundosdeaxelis.crate.CrateKind k = com.mundosdeaxelis.crate.CrateKind.byIndex(tier);
            out.add(new ItemStack(ModItems.KEY_FRAGMENTS.get(k).get(), 2));
        }
        out.add(new ItemStack(ModItems.ESENCIAS.get(com.mundosdeaxelis.gema.Gemas.Elemento.ofTier(tier)).get()));
        return out;
    }

    @Override
    protected void dropCustomDeathLoot(DamageSource source, int looting, boolean recentlyHit) {
        super.dropCustomDeathLoot(source, looting, recentlyHit);
        if (!(level() instanceof ServerLevel sl)) {
            return;
        }
        ServerPlayer sp = source.getEntity() instanceof ServerPlayer p ? p : null;
        if (sp == null && getLastHurtByMob() instanceof ServerPlayer p2) {
            sp = p2;
        }
        if (sp == null) {
            return;
        }
        if (!Sellos.canLoot(sp, tier())) {
            sp.displayClientMessage(Component.translatable("message.mundos_de_axelis.sello.mimico", Sellos.bossToBeat(tier()))
                    .withStyle(ChatFormatting.LIGHT_PURPLE), true);
            sl.sendParticles(ParticleTypes.SOUL, getX(), getY() + 0.5, getZ(), 14, 0.3, 0.3, 0.3, 0.02);
            return;
        }
        List<ItemStack> items = loot(tier(), random, com.mundosdeaxelis.table.Essence.archive(sp));
        for (ItemStack it : items) {
            ItemEntity ie = spawnAtLocation(it, 0.6F);
            if (ie != null) {
                ie.setDeltaMovement((random.nextDouble() - 0.5) * 0.25, 0.3 + random.nextDouble() * 0.2, (random.nextDouble() - 0.5) * 0.25);
            }
        }
        for (ItemStack it : items) {
            if (VaultLoot.isStar(it)) {
                sl.playSound(null, blockPosition(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.HOSTILE, 0.7F, 1.3F);
                sl.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, getX(), getY() + 0.8, getZ(), 30, 0.3, 0.5, 0.3, 0.35);
                sp.displayClientMessage(Component.translatable("message.mundos_de_axelis.vault.star", it.getHoverName())
                        .withStyle(com.mundosdeaxelis.block.VaultChestBlockEntity.rarity(tier()).style()), true);
                break;
            }
        }
        int ess = VaultLoot.essence(VaultLoot.Kind.TRIAL, tier());
        com.mundosdeaxelis.table.Essence.add(sp, ess);
        com.mundosdeaxelis.table.Essence.sync(sp);
        sp.getPersistentData().putInt("mdc_mimicos", sp.getPersistentData().getInt("mdc_mimicos") + 1);
    }

    // ------------------------------------------------------------------ como un cofre mientras duerme

    @Override
    public boolean isPushable() {
        return isAwake() && super.isPushable();
    }

    @Override
    public boolean canBeCollidedWith() {
        return !isAwake() && isAlive();
    }

    @Override
    protected void doPush(Entity other) {
        if (isAwake()) {
            super.doPush(other);
        }
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public boolean canChangeDimensions() {
        return false;
    }

    @Override
    public boolean shouldShowName() {
        return false;
    }

    @Override
    public int getMaxHeadYRot() {
        return isAwake() ? 30 : 0;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return isAwake() ? SoundEvents.RAVAGER_AMBIENT : null;
    }

    @Override
    public float getVoicePitch() {
        return 1.7F + random.nextFloat() * 0.2F;
    }

    @Override
    protected float getSoundVolume() {
        return 0.6F;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.ZOMBIE_ATTACK_WOODEN_DOOR;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ZOMBIE_BREAK_WOODEN_DOOR;
    }

    @Override
    protected void playStepSound(BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
        playSound(SoundEvents.WOOD_STEP, 0.4F, 1.3F);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Awake", isAwake());
        tag.putInt("Tier", tier());
        tag.putBoolean("Oriented", oriented);
        tag.putBoolean("Scaled", scaled);
        tag.putFloat("SleepYaw", sleepYaw);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.entityData.set(AWAKE, tag.getBoolean("Awake"));
        if (tag.contains("Tier")) {
            int t = Mth.clamp(tag.getInt("Tier"), 1, 8);
            this.entityData.set(TIER, t);
            this.xpReward = 8 + 6 * t;
        }
        oriented = tag.getBoolean("Oriented");
        scaled = tag.getBoolean("Scaled");
        sleepYaw = tag.getFloat("SleepYaw");
    }
}
