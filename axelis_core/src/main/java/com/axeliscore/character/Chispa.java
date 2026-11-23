package com.axeliscore.character;

import com.axeliscore.anim.AnimHost;
import com.axeliscore.anim.Pose;
import com.axeliscore.plat.Plat;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.EnumSet;

/**
 * Chispa: mascota de muestra (zorrito de orejas grandes y cola esponjosa). Sigue a quien tenga cerca, se alegra cuando llegas (brinca y
 * da una vuelta), pide comida parada en dos patas, juega a saltar, se sienta y se duerme si la dejas sola. Clic: se pone contenta.
 */
public class Chispa extends CoreMob {
    private int nextGesture = 160, greet, lonely;
    private boolean sleeping;

    public Chispa(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 10.0).add(Attributes.MOVEMENT_SPEED, 0.32);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(3, new Follow());
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
    }

    /** Sigue al jugador más cercano (hasta 16 bloques) y se detiene a 2.5. */
    private final class Follow extends Goal {
        private Player p;

        Follow() {
            setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (sleeping || isNoAi()) {
                return false;
            }
            p = level().getNearestPlayer(Chispa.this, 16.0);
            return p != null && !p.isSpectator() && distanceTo(p) > 4.0F;
        }

        @Override
        public boolean canContinueToUse() {
            return p != null && p.isAlive() && distanceTo(p) > 2.5F && distanceTo(p) < 20F;
        }

        @Override
        public void tick() {
            getLookControl().setLookAt(p, 30F, 30F);
            Plat.walkTo(Chispa.this, p, distanceTo(p) > 8 ? 1.25 : 1.0);
        }

        @Override
        public void stop() {
            getNavigation().stop();
        }
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide() || isNoAi() || showcasing()) {
            return;
        }
        Player p = level().getNearestPlayer(this, 6.0);
        if (greet > 0) {
            greet--;
        }
        if (p != null) {
            lonely = 0;
            if (sleeping) {
                sleeping = false;
                playAction("happy");
                greet = 600;
            } else if (greet == 0) {
                playAction("happy");
                greet = 900;
            }
        } else if (++lonely > 600 && !sleeping && getNavigation().isDone()) {
            sleeping = true;
            playAction("sleep", 100000);
        }
        if (--nextGesture <= 0) {
            nextGesture = 240 + getRandom().nextInt(400);
            if (!sleeping && p != null && getNavigation().isDone()) {
                String[] g = {"beg", "play", "sit"};
                String pick = g[getRandom().nextInt(g.length)];
                playAction(pick, pick.equals("sit") ? 120 : 0);
            }
        }
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand == InteractionHand.MAIN_HAND && !level().isClientSide()) {
            sleeping = false;
            playAction("happy");
        }
        return Plat.ok(level().isClientSide());
    }

    @Override
    public void axAnimate(AnimHost h, float pt) {
        if (axDeath(h, "action")) {
            return;
        }
        axActions(h, n -> "action");
        axHurt(h, "action");
        axMove(h, 0.25F, 3.4F, 1.2F, 4.0F);
    }

    @Override
    public void axPose(AnimHost h, Pose pose, float pt) {
        if (deathTime <= 0 && !axDoing(h, "sleep") && !axDoing(h, "happy")) {
            axLook(h, pose, pt, "head", null, 1F);
        }
    }
}
