package com.axeliscore.character;

import com.axeliscore.anim.AnimHost;
import com.axeliscore.anim.Pose;
import com.axeliscore.plat.Plat;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * Bestia: criatura de cuatro patas (lobo de ceniza de muestra). Patas con rodilla, cuello, mandíbula que se abre, orejas, cola de tres
 * partes y ojos que brillan. Animaciones: quieto, caminar, galopar, sentarse, morder, aullar, olfatear, contenta, daño y morir.
 * Comportamiento de muestra: pasea, olfatea, se sienta, aúlla de noche y se defiende si le pegan.
 */
public class Bestia extends CoreMob {
    private int nextGesture = 200;

    public Bestia(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 30.0).add(Attributes.MOVEMENT_SPEED, 0.3).add(Attributes.ATTACK_DAMAGE, 5.0)
                .add(Attributes.FOLLOW_RANGE, 32.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.3, true));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide() || isNoAi() || showcasing()) {
            return;
        }
        if (--nextGesture <= 0) {
            nextGesture = 300 + getRandom().nextInt(500);
            if (getTarget() == null && getNavigation().isDone()) {
                long t = Plat.dayTime(level()) % 24000L;
                boolean night = t > 13000 && t < 23000;
                if (night && getRandom().nextInt(3) == 0) {
                    playAction("howl");
                } else {
                    String[] g = {"sniff", "sniff", "sit", "happy"};
                    String pick = g[getRandom().nextInt(g.length)];
                    playAction(pick, pick.equals("sit") ? 200 : 0);
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
        axSwing(h, "attack", "action");
        axMove(h, 0.3F, 3.6F, 1.0F, 4.0F);
    }

    @Override
    public void axPose(AnimHost h, Pose pose, float pt) {
        if (deathTime <= 0 && !axDoing(h, "howl") && !axDoing(h, "sniff")) {
            axLook(h, pose, pt, "head", "neck", 0.65F);
        }
    }
}
