package com.axeliscore.character;

import com.axeliscore.anim.AnimHost;
import com.axeliscore.anim.Pose;
import com.axeliscore.plat.Plat;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Coloso: jefe grande de muestra (golem de piedra con cristales que brillan). Ataques con aviso, sincronizados con la animación:
 * <ul>
 *     <li>Martillazo (cerca): levanta los dos puños y los azota contra el suelo (daño en círculo y los avienta hacia arriba).</li>
 *     <li>Barrido (cerca): un brazo de lado a lado delante de él.</li>
 *     <li>Pisotón (media distancia): onda que empuja.</li>
 *     <li>Rugido: empuja a todos y anuncia la segunda fase (a la mitad de la vida: se arrodilla, los cristales crecen y pega más).</li>
 * </ul>
 * Barra de jefe propia. Todos los números están aquí arriba para ajustarlos fácil.
 */
public class Coloso extends CoreMob {
    static final float SLAM_DMG = 14, SWEEP_DMG = 10, STOMP_DMG = 7;
    /** Tiempos de impacto de cada animación (ticks), los mismos que el evento de la animación. */
    static final int SLAM_HIT = 23, SWEEP_HIT = 15, STOMP_HIT = 12, ROAR_HIT = 8, PHASE_LEN = 52;

    //#if MC >= 26.1
    //$$ private final ServerBossEvent bar = new ServerBossEvent(java.util.UUID.randomUUID(), Component.translatable("entity.axelis_core.coloso"),
    //$$         BossEvent.BossBarColor.PURPLE, BossEvent.BossBarOverlay.NOTCHED_10);
    //#else
    private final ServerBossEvent bar = new ServerBossEvent(Component.translatable("entity.axelis_core.coloso"), BossEvent.BossBarColor.PURPLE,
            BossEvent.BossBarOverlay.NOTCHED_10);
    //#endif
    private int cooldown = 40, busy, hitAt = -1;
    private String hitKind = "";
    private boolean phase2;

    public Coloso(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.xpReward = 120;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 300.0).add(Attributes.ARMOR, 10.0).add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.MOVEMENT_SPEED, 0.22).add(Attributes.ATTACK_DAMAGE, 12.0).add(Attributes.FOLLOW_RANGE, 40.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.6));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 16.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public void startSeenByPlayer(ServerPlayer p) {
        super.startSeenByPlayer(p);
        bar.addPlayer(p);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer p) {
        super.stopSeenByPlayer(p);
        bar.removePlayer(p);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide()) {
            return;
        }
        bar.setProgress(getHealth() / getMaxHealth());
        if (isNoAi() || deathTime > 0) {
            return;
        }
        if (hitAt >= 0 && tickCount >= hitAt) {
            hit(hitKind);
            hitAt = -1;
        }
        if (busy > 0) {
            busy--;
            getNavigation().stop();
            return;
        }
        if (!phase2 && getHealth() < getMaxHealth() * 0.5F) {
            phase2 = true;
            start("phase", PHASE_LEN, -1, "");
            bar.setColor(BossEvent.BossBarColor.RED);
            return;
        }
        LivingEntity t = getTarget();
        if (t == null || !t.isAlive()) {
            return;
        }
        getLookControl().setLookAt(t, 30F, 30F);
        double d = distanceTo(t);
        if (--cooldown > 0) {
            if (d > 3.2) {
                Plat.walkTo(this, t, 1.0);
            }
            return;
        }
        int speedUp = phase2 ? 12 : 0;
        if (d < 4.0) {
            if (getRandom().nextBoolean()) {
                start("slam", 40, SLAM_HIT, "slam");
            } else {
                start("sweep", 28, SWEEP_HIT, "sweep");
            }
            cooldown = 50 - speedUp;
        } else if (d < 9.0 && getRandom().nextInt(3) > 0) {
            start("stomp", 22, STOMP_HIT, "stomp");
            cooldown = 45 - speedUp;
        } else if (d < 14.0 && getRandom().nextInt(4) == 0) {
            start("roar", 44, ROAR_HIT, "roar");
            cooldown = 70;
        } else {
            Plat.walkTo(this, t, 1.0);
            cooldown = 10;
        }
    }

    private void start(String clip, int lock, int hitIn, String kind) {
        getNavigation().stop();
        playAction(clip);
        busy = lock;
        hitAt = hitIn >= 0 ? tickCount + hitIn : -1;
        hitKind = kind;
    }

    /** El golpe: a quién le pega y cómo los avienta. */
    private void hit(String kind) {
        float mult = phase2 ? 1.3F : 1F;
        Vec3 c = position();
        double r = kind.equals("stomp") ? 6.5 : kind.equals("roar") ? 8 : 4.6;
        Vec3 fwd = Vec3.directionFromRotation(0, getYRot());
        for (LivingEntity e : level().getEntitiesOfClass(LivingEntity.class, new AABB(c.x - r, c.y - 2, c.z - r, c.x + r, c.y + 4, c.z + r))) {
            if (e == this || e.distanceTo(this) > r) {
                continue;
            }
            Vec3 to = e.position().subtract(c);
            Vec3 dir = new Vec3(to.x, 0, to.z).normalize();
            switch (kind) {
                case "slam" -> {
                    Plat.hurt(e, damageSources().mobAttack(this), SLAM_DMG * mult);
                    Plat.push(e, dir.x * 0.6, 0.75, dir.z * 0.6);
                }
                case "sweep" -> {
                    if (dir.dot(fwd) < 0.1) {
                        continue; // solo delante
                    }
                    Plat.hurt(e, damageSources().mobAttack(this), SWEEP_DMG * mult);
                    Vec3 side = new Vec3(-fwd.z, 0, fwd.x);
                    Plat.push(e, side.x * 1.1, 0.3, side.z * 1.1);
                }
                case "stomp" -> {
                    if (!e.onGround()) {
                        continue; // saltando se esquiva
                    }
                    Plat.hurt(e, damageSources().mobAttack(this), STOMP_DMG * mult);
                    Plat.push(e, dir.x * 1.0, 0.4, dir.z * 1.0);
                }
                default -> {
                    Plat.push(e, dir.x * 1.4, 0.35, dir.z * 1.4);
                }
            }
        }
    }

    @Override
    public void axAnimate(AnimHost h, float pt) {
        if (axDeath(h, "action")) {
            return;
        }
        axActions(h, n -> "action");
        axHurt(h, "action");
        axMove(h, 0.2F, 99F, 0.85F, 1F);
    }

    @Override
    public void axPose(AnimHost h, Pose pose, float pt) {
        var act = h.animator().get("action");
        if (deathTime <= 0 && (act == null || !act.playing())) {
            axLook(h, pose, pt, "head", "torso", 0.6F);
        }
    }

    @Override
    public boolean removeWhenFarAway(double d) {
        return false;
    }
}
