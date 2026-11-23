package com.axeliscore.character;

import com.axeliscore.anim.AnimHost;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * Gelatina: un bloque blandito con un brote en la cabeza. Se mueve a brincos (de verdad: la física la lanza) y la animación la aplasta
 * al despegar y al caer. Es curiosa: brinca hacia quien tiene cerca. No ataca.
 */
public class Gelatina extends CoreMob {
    private int hopTimer = 30;

    public Gelatina(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 12.0).add(Attributes.MOVEMENT_SPEED, 0.25);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide() || isNoAi() || !onGround()) {
            return;
        }
        if (--hopTimer > 0) {
            return;
        }
        hopTimer = 25 + getRandom().nextInt(45);
        Player p = level().getNearestPlayer(this, 10.0);
        float yaw;
        if (p != null && distanceTo(p) > 2.5F) {
            yaw = (float) Math.toDegrees(Math.atan2(-(p.getX() - getX()), p.getZ() - getZ()));
        } else {
            yaw = getYRot() + (getRandom().nextFloat() - 0.5F) * 140F;
        }
        setYRot(yaw);
        setYBodyRot(yaw);
        setYHeadRot(yaw);
        float r = yaw * Mth.DEG_TO_RAD;
        setDeltaMovement(-Mth.sin(r) * 0.28, 0.42, Mth.cos(r) * 0.28);
        playAction("jump");
    }

    @Override
    public void axAnimate(AnimHost h, float pt) {
        if (axDeath(h, "action")) {
            return;
        }
        axActions(h, n -> "action");
        axHurt(h, "action");
        // al caer al suelo: se aplasta
        boolean ground = onGround();
        Object was = h.data.get("ground");
        if (ground && Boolean.FALSE.equals(was) && h.clip("land") != null) {
            h.animator().play("action", h.clip("land"), 0.05F, 1F, true);
        }
        h.data.put("ground", ground);
        h.animator().play("base", h.clip("idle"), 0.3F, 1F, false);
    }
}
