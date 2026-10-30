package com.kensei.entity;

import com.kensei.KenseiConfig;
import com.kensei.combat.CombatState;
import com.kensei.combat.Moves;
import com.kensei.duel.Duel;
import com.kensei.duel.DuelManager;
import com.kensei.net.FxPacket;
import com.kensei.net.KenseiNet;
import com.kensei.registry.ModItems;
import com.kensei.registry.ModSounds;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import org.joml.Vector3f;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * El rival de práctica: un espadachín con katana para probar el combate sin amigos. Ataca con tajos en cadena y con un golpe crítico
 * (que se anuncia levantando la katana y no se puede parrear), se cubre, esquiva cuando le vas a pegar y se aturde si lo parreas.
 * Tiene dos modos (agáchate y haz clic derecho para cambiar): rival (pelea) y maniquí (se queda quieto, no ataca y se cura solo).
 * También se usa como oponente en los duelos.
 */
public class RivalEntity extends PathfinderMob {
    private static final EntityDataAccessor<Boolean> FIGHTER = SynchedEntityData.defineId(RivalEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> VIS = SynchedEntityData.defineId(RivalEntity.class, EntityDataSerializers.INT);

    /** Postura visible: nada, preparando un tajo, preparando el crítico, en guardia. */
    public static final int VIS_NONE = 0;
    public static final int VIS_LIGHT = 1;
    public static final int VIS_HEAVY = 2;
    public static final int VIS_GUARD = 3;

    /** Lo creó un duelo: no se guarda con el mundo y no cambia de modo. */
    private boolean duelBot = false;

    // ataque en curso
    private int atk = 0; // 0 nada, 1 preparando, 2 recuperándose
    private int atkTimer = 0;
    private int atkTb = 0;
    private boolean atkHeavy = false;
    private boolean animSent = false;
    private int chainLeft = 0;
    private int chainIndex = 0;
    private int cooldown = 30;
    private int heavyCd = 60;
    // defensa
    private int guardTicks = 0;
    private int guardHits = 0;
    private int reactCd = 0;
    private int dodgeCd = 0;
    private int dodgeUntil = -1;
    private boolean perfectShown = false;
    private float energy = 300.0F;
    // estamina (como la del jugador: los golpes y el bloqueo la gastan y se agota)
    private float stamina = 100.0F;
    private int staminaWait = 0;
    private int exhaustedTicks = 0;
    // movimiento
    private int strafeDir = 1;
    private int strafeTimer = 0;
    private boolean heavyNow = false;
    private int lastHurtTick = 0;
    private LivingEntity rivalTarget = null;
    private int retargetIn = 0;

    public RivalEntity(EntityType<? extends RivalEntity> type, Level level) {
        super(type, level);
        this.xpReward = 0;
        this.setPersistenceRequired();
        this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ModItems.KATANA.get()));
        this.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
        this.setCanPickUpLoot(false);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 40.0)
                .add(Attributes.MOVEMENT_SPEED, 0.32)
                .add(Attributes.ATTACK_DAMAGE, 4.0)
                .add(Attributes.FOLLOW_RANGE, 32.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.35)
                .add(Attributes.ARMOR, 2.0);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(FIGHTER, true);
        this.entityData.define(VIS, VIS_NONE);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
    }

    // ------------------------------------------------------------------ estado

    public boolean isFighter() {
        return this.entityData.get(FIGHTER);
    }

    public void setFighter(boolean v) {
        this.entityData.set(FIGHTER, v);
    }

    public int visState() {
        return this.entityData.get(VIS);
    }

    public void setDuelBot(boolean v) {
        this.duelBot = v;
        if (v) {
            setFighter(true);
        }
    }

    public boolean isDuelBot() {
        return duelBot;
    }

    public boolean isGuarding() {
        return guardTicks > 0;
    }

    /** Energía de esquive (0 a 300) para el marcador del duelo. */
    public int energyInt() {
        return (int) energy;
    }

    public float staminaFraction() {
        return Math.max(0.0F, Math.min(1.0F, stamina / 100.0F));
    }

    public boolean isExhausted() {
        return exhaustedTicks > 0;
    }

    private void spendStamina(float cost) {
        if (!KenseiConfig.STAMINA_ENABLED.get()) {
            return;
        }
        stamina = Math.max(0.0F, stamina - cost);
        staminaWait = 18;
        if (stamina <= 0.01F) {
            exhaustedTicks = 30;
            if (level() instanceof ServerLevel sl) {
                sl.playSound(null, getX(), getY(), getZ(), SoundEvents.PLAYER_BREATH, SoundSource.HOSTILE, 0.8F, 0.9F);
                sl.sendParticles(ParticleTypes.CLOUD, getX(), getY() + 1.7, getZ(), 4, 0.2, 0.1, 0.2, 0.01);
            }
        }
    }

    public int heavyCooldownTicks() {
        return Math.max(0, heavyCd);
    }

    /** ¿Está pegando ahora mismo con el golpe crítico? (ese golpe no se puede parrear) */
    public boolean isHeavyStrike() {
        return heavyNow;
    }

    /** Vuelve a empezar (cada ronda de un duelo). */
    public void resetCombat() {
        stamina = 100.0F;
        staminaWait = 0;
        exhaustedTicks = 0;
        atk = 0;
        atkTimer = 0;
        chainLeft = 0;
        chainIndex = 0;
        cooldown = 40;
        heavyCd = 80;
        guardTicks = 0;
        guardHits = 0;
        reactCd = 0;
        dodgeCd = 0;
        dodgeUntil = -1;
        energy = 300.0F;
        rivalTarget = null;
        getNavigation().stop();
        setVis(VIS_NONE);
    }

    private void setVis(int v) {
        if (this.entityData.get(VIS) != v) {
            this.entityData.set(VIS, v);
        }
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public boolean shouldBeSaved() {
        return !duelBot && super.shouldBeSaved();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Fighter", isFighter());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("Fighter")) {
            setFighter(tag.getBoolean("Fighter"));
        }
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!duelBot && player.isShiftKeyDown() && hand == InteractionHand.MAIN_HAND) {
            if (!level().isClientSide) {
                boolean fight = !isFighter();
                setFighter(fight);
                resetCombat();
                player.displayClientMessage(Component.translatable(fight ? "kensei.rival.mode_fight" : "kensei.rival.mode_dummy"), true);
                level().playSound(null, getX(), getY(), getZ(), SoundEvents.UI_BUTTON_CLICK.get(), SoundSource.NEUTRAL, 0.6F, fight ? 1.4F : 0.9F);
            }
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        return super.mobInteract(player, hand);
    }

    // ------------------------------------------------------------------ recibir daño

    private boolean inFront(Entity att) {
        Vec3 look = getViewVector(1.0F);
        Vec3 to = att.position().subtract(position());
        Vec3 fl = new Vec3(look.x, 0, look.z);
        Vec3 ft = new Vec3(to.x, 0, to.z);
        if (fl.lengthSqr() < 1.0E-6 || ft.lengthSqr() < 1.0E-6) {
            return true;
        }
        return fl.normalize().dot(ft.normalize()) > 0.25;
    }

    @Override
    public boolean hurt(DamageSource src, float amount) {
        if (level().isClientSide || src.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return super.hurt(src, amount);
        }
        Entity att = src.getEntity();
        if (att != null && level() instanceof ServerLevel sl) {
            if (tickCount < dodgeUntil) {
                if (!perfectShown) {
                    perfectShown = true;
                    DuelManager.stat(this, 1);
                    KenseiNet.fx(sl, FxPacket.PERFECT_DODGE, getX(), getY() + 1.0, getZ(), 0.0F, 0, att.getId(), getId(), getYRot());
                    sl.playSound(null, getX(), getY(), getZ(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.HOSTILE, 0.8F, 1.6F);
                }
                return false;
            }
            if (guardTicks > 0 && inFront(att)) {
                float blockCost = (float) (KenseiConfig.COST_BLOCK_BASE.get() + KenseiConfig.COST_BLOCK_PER_DAMAGE.get() * amount);
                boolean canHold = !KenseiConfig.STAMINA_ENABLED.get() || (stamina >= blockCost && exhaustedTicks <= 0);
                if (canHold) {
                    spendStamina(blockCost);
                    amount *= 0.25F;
                } else {
                    stamina = 0.0F;
                    exhaustedTicks = 30;
                    breakGuard(); // sin estamina no aguanta el golpe
                }
                guardHits++;
                sl.playSound(null, getX(), getY(), getZ(), SoundEvents.SHIELD_BLOCK, SoundSource.HOSTILE, 0.8F, 1.25F);
                Vec3 f = getViewVector(1.0F);
                sl.sendParticles(ParticleTypes.CRIT, getX() + f.x * 0.7, getY() + 1.2, getZ() + f.z * 0.7, 7, 0.15, 0.2, 0.15, 0.35);
                KenseiNet.fx(sl, FxPacket.BLOCK, getX() + f.x * 0.8, getY() + 1.2, getZ() + f.z * 0.8, amount, 0, att.getId(), getId(), getYRot());
                if (guardHits >= 4 && guardTicks > 0) {
                    breakGuard();
                }
            }
        }
        boolean ok = super.hurt(src, amount);
        if (ok) {
            lastHurtTick = tickCount;
            // un golpe recibido mientras prepara un tajo suelto a veces le corta el ataque (el crítico no se interrumpe)
            if (atk == 1 && !atkHeavy && random.nextFloat() < 0.55F) {
                cancelAttack();
            }
            if (!isFighter() && !duelBot && getHealth() < 8.0F && isAlive()) {
                setHealth(getMaxHealth()); // el maniquí no muere
            }
        }
        return ok;
    }

    /** Se le rompe la guardia (por un crítico o por tantos golpes): queda aturdido un momento. */
    public void breakGuard() {
        if (guardTicks <= 0) {
            return;
        }
        guardTicks = 0;
        guardHits = 0;
        cancelAttack();
        addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 9, false, false, false));
        if (level() instanceof ServerLevel sl) {
            sl.playSound(null, getX(), getY(), getZ(), ModSounds.GUARD_BREAK.get(), SoundSource.HOSTILE, 1.0F, 1.0F);
            KenseiNet.fx(sl, FxPacket.GUARD_BREAK, getX(), getY() + 1.3, getZ(), 0.0F, 0, -1, getId(), getYRot());
        }
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide && visState() == VIS_HEAVY && tickCount % 2 == 0) {
            // el crítico se anuncia con chispas rojas sobre la cabeza: esquívalo (no se puede parrear)
            for (int i = 0; i < 2; i++) {
                level().addParticle(new DustParticleOptions(new Vector3f(1.0F, 0.12F, 0.12F), 1.4F),
                        getX() + (random.nextDouble() - 0.5) * 0.9, getY() + 2.1 + random.nextDouble() * 0.5, getZ() + (random.nextDouble() - 0.5) * 0.9,
                        0.0, 0.02, 0.0);
            }
        }
    }

    // ------------------------------------------------------------------ cerebro

    private void cancelAttack() {
        atk = 0;
        atkTimer = 0;
        chainLeft = 0;
        chainIndex = 0;
        cooldown = Math.max(cooldown, 20);
        setVis(VIS_NONE);
    }

    private LivingEntity pickTarget(Duel duel) {
        if (duel != null) {
            return duel.enemyFor(getUUID());
        }
        if (!isFighter()) {
            return null;
        }
        return level().getNearestPlayer(getX(), getY(), getZ(), 24.0, e -> e instanceof Player p && !p.isSpectator() && p.isAlive());
    }

    private void faceTarget(LivingEntity t, float step) {
        double dx = t.getX() - getX();
        double dz = t.getZ() - getZ();
        float want = (float) (Mth.atan2(dz, dx) * 57.2957763671875) - 90.0F;
        float y = Mth.approachDegrees(getYRot(), want, step);
        setYRot(y);
        setYBodyRot(y);
        setYHeadRot(y);
        getLookControl().setLookAt(t, 60.0F, 60.0F);
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        Duel duel = DuelManager.fighterDuel(this);
        // maniquí: se cura solo si lo dejan en paz y mira a quien tenga cerca
        if (!isFighter() && duel == null) {
            if (tickCount - lastHurtTick > 60 && tickCount % 10 == 0 && getHealth() < getMaxHealth()) {
                heal(2.0F);
            }
            Player near = level().getNearestPlayer(this, 8.0);
            if (near != null) {
                getLookControl().setLookAt(near, 30.0F, 30.0F);
            }
            setVis(VIS_NONE);
            return;
        }
        if (cooldown > 0) {
            cooldown--;
        }
        if (heavyCd > 0) {
            heavyCd--;
        }
        if (reactCd > 0) {
            reactCd--;
        }
        if (dodgeCd > 0) {
            dodgeCd--;
        }
        if (tickCount >= dodgeUntil) {
            perfectShown = false;
        }
        if (atk == 0 && guardTicks == 0 && energy < 300.0F) {
            energy = Math.min(300.0F, energy + 1.3F);
        }
        if (exhaustedTicks > 0) {
            exhaustedTicks--;
        } else if (staminaWait > 0) {
            staminaWait--;
        } else if (stamina < 100.0F) {
            stamina = Math.min(100.0F, stamina + (guardTicks > 0 ? 0.55F : 1.5F));
        }
        if (duel != null && duel.frozen(getUUID())) {
            getNavigation().stop();
            if (atk != 0 || guardTicks != 0) {
                cancelAttack();
                guardTicks = 0;
            }
            setVis(VIS_NONE);
            return;
        }
        if (--retargetIn <= 0 || rivalTarget == null || !rivalTarget.isAlive() || rivalTarget.isRemoved()) {
            rivalTarget = pickTarget(duel);
            retargetIn = 10;
        }
        LivingEntity t = rivalTarget;
        if (t == null || t.level() != level()) {
            getNavigation().stop();
            setVis(VIS_NONE);
            return;
        }
        // aturdido (parry o guardia rota): no hace nada
        MobEffectInstance slow = getEffect(MobEffects.MOVEMENT_SLOWDOWN);
        if (slow != null && slow.getAmplifier() >= 4 && slow.getDuration() > 6) {
            if (atk != 0 || guardTicks != 0) {
                cancelAttack();
                guardTicks = 0;
            }
            getNavigation().stop();
            setVis(VIS_NONE);
            return;
        }
        double dist = distanceTo(t);
        faceTarget(t, atk == 1 ? 18.0F : 30.0F);

        switch (atk) {
            case 1 -> windup(t);
            case 2 -> recover(t);
            default -> idle(t, dist);
        }
        setVis(atk == 1 ? (atkHeavy ? VIS_HEAVY : VIS_LIGHT) : guardTicks > 0 ? VIS_GUARD : VIS_NONE);
    }

    /** ¿Lo que va a hacer el rival es un ataque que viene? (lo que el otro prepara y si es el crítico) → 0 nada, 1 tajo, 2 crítico */
    private int incoming(LivingEntity t, double dist) {
        if (dist > 5.0) {
            return 0;
        }
        if (t instanceof ServerPlayer sp) {
            CombatState s = CombatState.of(sp);
            if (s.pendingHeavyAt > 0) {
                return 2;
            }
            if (s.pendingHitAt > 0) {
                return 1;
            }
            return 0;
        }
        if (t instanceof RivalEntity r && r.atk == 1) {
            return r.atkHeavy ? 2 : 1;
        }
        return 0;
    }

    private void idle(LivingEntity t, double dist) {
        // reaccionar: cubrirse o esquivar cuando el otro prepara un golpe
        if (KenseiConfig.RIVAL_DEFENDS.get() && guardTicks == 0 && reactCd == 0) {
            int inc = incoming(t, dist);
            if (inc > 0) {
                reactCd = 14;
                float r = random.nextFloat();
                if (inc == 2) {
                    if (r < 0.65F && energy >= 100.0F && dodgeCd == 0) {
                        dodge(t);
                        return;
                    }
                } else if (r < 0.38F) {
                    guardTicks = 16 + random.nextInt(10);
                    guardHits = 0;
                } else if (r < 0.52F && energy >= 100.0F && dodgeCd == 0) {
                    dodge(t);
                    return;
                }
            }
        }
        if (guardTicks > 0) {
            guardTicks--;
            getNavigation().stop();
            if (dist < 2.0) {
                getMoveControl().strafe(-0.35F, 0.0F);
            }
            return;
        }
        // moverse: acercarse, mantener distancia o dar vueltas
        if (--strafeTimer <= 0) {
            strafeTimer = 20 + random.nextInt(30);
            strafeDir = random.nextBoolean() ? 1 : -1;
        }
        if (dist > 3.0) {
            getNavigation().moveTo(t, 1.15);
        } else {
            getNavigation().stop();
            if (dist < 1.7) {
                getMoveControl().strafe(-0.7F, strafeDir * 0.35F);
            } else {
                getMoveControl().strafe(0.0F, strafeDir * 0.55F);
            }
        }
        // a veces se cubre sin que nadie ataque, para no ser tan fácil
        if (KenseiConfig.RIVAL_DEFENDS.get() && cooldown > 12 && dist < 4.5 && random.nextInt(90) == 0) {
            guardTicks = 14 + random.nextInt(14);
            guardHits = 0;
            return;
        }
        if (cooldown <= 0 && dist <= 3.1 && hasLineOfSight(t) && exhaustedTicks <= 0 && stamina >= 10.0F) {
            startAttack();
        }
    }

    private void dodge(LivingEntity t) {
        energy -= 100.0F;
        dodgeCd = 28;
        dodgeUntil = tickCount + 8;
        Vec3 away = new Vec3(getX() - t.getX(), 0, getZ() - t.getZ());
        if (away.lengthSqr() < 1.0E-4) {
            away = new Vec3(0, 0, 1);
        }
        away = away.normalize();
        Vec3 side = new Vec3(-away.z, 0, away.x).scale(random.nextBoolean() ? 1 : -1);
        Vec3 dir = random.nextInt(3) == 0 ? away : side.scale(0.9).add(away.scale(0.45)).normalize();
        setDeltaMovement(dir.x * 0.95, 0.14, dir.z * 0.95);
        hurtMarked = true;
        getNavigation().stop();
        float yaw = (float) Math.toDegrees(Math.atan2(-dir.x, dir.z));
        KenseiNet.broadcastAnim(this, Moves.ANIM_DODGE, yaw, false);
        level().playSound(null, getX(), getY(), getZ(), ModSounds.DODGE.get(), SoundSource.HOSTILE, 0.8F, 1.0F);
        if (level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.CLOUD, getX(), getY() + 0.2, getZ(), 8, 0.25, 0.05, 0.25, 0.02);
        }
        cooldown = Math.max(cooldown, 12); // después de esquivar contraataca pronto
    }

    private void startAttack() {
        double agg = KenseiConfig.RIVAL_AGGRESSION.get();
        boolean heavy = chainLeft == 0 && heavyCd <= 0 && random.nextInt(4) == 0;
        if (chainLeft == 0) {
            chainIndex = 0;
            chainLeft = heavy ? 1 : 2 + random.nextInt(3);
        }
        float cost = heavy ? (float) (double) KenseiConfig.COST_HEAVY.get()
                : (float) (double) (chainIndex >= 3 ? KenseiConfig.COST_FINISHER.get() : KenseiConfig.COST_LIGHT.get());
        if (KenseiConfig.STAMINA_ENABLED.get() && (exhaustedTicks > 0 || stamina < cost)) {
            chainLeft = 0; // sin aire: corta la cadena y descansa
            chainIndex = 0;
            atk = 0;
            cooldown = 20;
            return;
        }
        spendStamina(cost);
        atkHeavy = heavy;
        int ta = heavy ? 11 : (chainIndex == 0 ? 8 : 4);
        atkTb = heavy ? KenseiConfig.HEAVY_WINDUP.get() : Math.max(1, KenseiConfig.M1_HIT_DELAY.get() + 1);
        if (agg > 1.5 && !heavy) {
            ta = Math.max(3, (int) (ta / 1.4));
        }
        atkTimer = ta + atkTb;
        animSent = false;
        atk = 1;
        if (heavy) {
            heavyCd = 160;
            level().playSound(null, getX(), getY(), getZ(), ModSounds.HEAVY_CHARGE.get(), SoundSource.HOSTILE, 0.9F, 0.9F);
            if (level() instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.ENCHANTED_HIT, getX(), getY() + 2.0, getZ(), 10, 0.3, 0.3, 0.3, 0.1);
            }
        } else {
            level().playSound(null, getX(), getY(), getZ(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.HOSTILE, 0.4F, 1.4F);
        }
    }

    private void windup(LivingEntity t) {
        getNavigation().stop();
        atkTimer--;
        if (!animSent && atkTimer <= atkTb) {
            animSent = true;
            int step = atkHeavy ? Moves.ANIM_HEAVY : Math.min(4, chainIndex + 1);
            KenseiNet.broadcastAnim(this, step, 0.0F, false);
            if (!atkHeavy) {
                level().playSound(null, getX(), getY(), getZ(), ModSounds.SLASH.get(), SoundSource.HOSTILE, 0.8F, 0.92F + 0.05F * step);
                // paso corto hacia el enemigo, como el del jugador
                Vec3 to = new Vec3(t.getX() - getX(), 0, t.getZ() - getZ());
                if (to.lengthSqr() > 3.0) {
                    Vec3 d = to.normalize().scale(0.26);
                    Vec3 m = getDeltaMovement();
                    setDeltaMovement(d.x, m.y, d.z);
                }
            } else {
                level().playSound(null, getX(), getY(), getZ(), ModSounds.SLASH.get(), SoundSource.HOSTILE, 1.0F, 0.7F);
            }
        }
        if (atkTimer <= 0) {
            strike(t);
        }
    }

    private void strike(LivingEntity t) {
        boolean heavy = atkHeavy;
        swing(InteractionHand.MAIN_HAND);
        double reach = heavy ? 3.9 : 3.4;
        Vec3 look = getViewVector(1.0F);
        Vec3 to = new Vec3(t.getX() - getX(), 0, t.getZ() - getZ());
        double dist = to.length();
        boolean facing = dist < 0.6 || to.normalize().dot(new Vec3(look.x, 0, look.z).normalize()) > 0.45;
        if (t.isAlive() && dist - t.getBbWidth() * 0.5 <= reach && facing && level() instanceof ServerLevel sl) {
            heavyNow = heavy;
            try {
                if (heavy) {
                    Moves.breakGuard(t);
                }
                float dmg = (float) (double) (heavy ? KenseiConfig.RIVAL_HEAVY_DAMAGE.get() : KenseiConfig.RIVAL_LIGHT_DAMAGE.get());
                if (!heavy && chainIndex >= 3) {
                    dmg *= 1.25F;
                }
                boolean ok = t.hurt(damageSources().mobAttack(this), dmg);
                if (ok) {
                    DuelManager.stat(this, 2);
                    double kb = heavy ? 1.1 : (chainIndex >= 3 ? 0.8 : 0.35);
                    t.knockback(kb, getX() - t.getX(), getZ() - t.getZ());
                    if (heavy && t.isAlive()) {
                        Vec3 m = t.getDeltaMovement();
                        t.setDeltaMovement(m.x, Math.max(m.y, 0.28), m.z);
                        t.hurtMarked = true;
                    }
                    sl.sendParticles(ParticleTypes.CRIT, t.getX(), t.getY() + t.getBbHeight() * 0.6, t.getZ(), heavy ? 16 : 6, 0.25, 0.25, 0.25, heavy ? 0.45 : 0.3);
                    sl.playSound(null, t.getX(), t.getY(), t.getZ(), heavy ? ModSounds.HEAVY_HIT.get() : ModSounds.HIT.get(), SoundSource.HOSTILE, heavy ? 1.1F : 0.9F, 1.0F);
                    if (t instanceof ServerPlayer sp) {
                        KenseiNet.shake(sp, heavy ? 1.8F : 0.6F, heavy ? 10 : 4);
                    }
                }
            } finally {
                heavyNow = false;
            }
        }
        atk = 2;
        atkTimer = heavy ? 26 : (chainLeft > 1 ? 7 : 12);
        chainLeft--;
        chainIndex++;
    }

    private void recover(LivingEntity t) {
        getNavigation().stop();
        if (--atkTimer > 0) {
            return;
        }
        atk = 0;
        if (chainLeft > 0 && !atkHeavy) {
            startAttack(); // sigue la cadena
            return;
        }
        chainLeft = 0;
        chainIndex = 0;
        double agg = KenseiConfig.RIVAL_AGGRESSION.get();
        cooldown = (int) ((26 + random.nextInt(34)) / agg);
    }
}
