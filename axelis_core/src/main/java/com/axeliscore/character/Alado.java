package com.axeliscore.character;

import com.axeliscore.anim.AnimHost;
import com.axeliscore.anim.Pose;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomFlyingGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * Alado: criatura voladora (ave de tormenta de muestra) con alas de dos partes que aletean con retraso en la punta, cola de abanico y
 * patas que se recogen al volar. Animaciones: volar, planear (al bajar), posada (alas plegadas), atacar con las garras, daño y morir.
 */
public class Alado extends CoreMob {
    public Alado(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.moveControl = new FlyingMoveControl(this, 10, false);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 16.0).add(Attributes.FLYING_SPEED, 0.6).add(Attributes.MOVEMENT_SPEED, 0.25)
                .add(Attributes.ATTACK_DAMAGE, 3.0).add(Attributes.FOLLOW_RANGE, 32.0);
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        return new FlyingPathNavigation(this, level);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.2, true));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomFlyingGoal(this, 1.0));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
    }

    @Override
    public void aiStep() {
        super.aiStep();
        this.fallDistance = 0;
    }

    @Override
    public void axAnimate(AnimHost h, float pt) {
        if (axDeath(h, "action")) {
            return;
        }
        axActions(h, n -> "action");
        axHurt(h, "action");
        axSwing(h, "attack", "action");
        var an = h.animator();
        if (onGround()) {
            an.play("base", h.clip("idle"), 0.4F, 1F, false);
            return;
        }
        double vy = getY() - yo;
        float hs = groundSpeed();
        if (vy < -0.04 && hs > 1.5F) {
            an.play("base", h.clip("glide"), 0.5F, 1F, false);
        } else {
            an.play("base", h.clip("fly"), 0.3F, (float) Math.max(0.8, Math.min(1.6, 1.0 + vy * 6)), false);
        }
    }

    @Override
    public void axPose(AnimHost h, Pose pose, float pt) {
        if (deathTime <= 0) {
            axLook(h, pose, pt, "head", null, 1F);
        }
    }
}
