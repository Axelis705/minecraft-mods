package com.navidad.entidad;

import com.axeliscore.character.Persona;
import com.navidad.Temporada;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;

/**
 * El hombre de jengibre: una galleta chiquita que corre muy rápido cuando te acercas y se burla de ti. Si lo alcanzas suelta galletas.
 * Sale de día en praderas y bosques durante la temporada navideña.
 */
public class HombreJengibre extends Persona {
    private int burla;

    public HombreJengibre(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.xpReward = 3;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 8.0).add(Attributes.MOVEMENT_SPEED, 0.33).add(Attributes.FOLLOW_RANGE, 16.0);
    }

    public static boolean puedeSalir(EntityType<HombreJengibre> t, ServerLevelAccessor l, MobSpawnType why, BlockPos p, RandomSource r) {
        if (why == MobSpawnType.NATURAL || why == MobSpawnType.CHUNK_GENERATION) {
            if (l.getServer() == null || !Temporada.activa(l.getServer())) {
                return false;
            }
        }
        return Mob.checkMobSpawnRules(t, l, why, p, r) && l.getRawBrightness(p, 0) > 8;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new AvoidEntityGoal<>(this, Player.class, 9F, 1.3, 1.75, e -> !((Player) e).isCreative() || true));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.7));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 10F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide || showcasing()) {
            return;
        }
        if (burla > 0) {
            burla--;
        }
        if (tickCount % 20 == 0 && burla == 0) {
            Player p = level().getNearestPlayer(this, 10.0);
            if (p != null && !p.isSpectator()) {
                burla = 160 + getRandom().nextInt(160);
                int k = 1 + getRandom().nextInt(4);
                p.displayClientMessage(Component.translatable("navidad.jengibre.burla" + k), true);
                playAction(getRandom().nextBoolean() ? "laugh" : "cheer");
                playSound(SoundEvents.VILLAGER_CELEBRATE, 0.6F, 1.9F);
            }
        }
    }

    @Override
    protected InteractionResult mobInteract(Player p, InteractionHand hand) {
        if (!level().isClientSide) {
            p.displayClientMessage(Component.translatable("navidad.jengibre.burla" + (1 + getRandom().nextInt(4))), true);
            playAction("laugh");
        }
        return InteractionResult.sidedSuccess(level().isClientSide);
    }

    @Override
    public float getVoicePitch() {
        return 1.9F;
    }

    @Override
    protected net.minecraft.sounds.SoundEvent getHurtSound(net.minecraft.world.damagesource.DamageSource s) {
        return SoundEvents.GENERIC_EAT;
    }

    @Override
    protected net.minecraft.sounds.SoundEvent getDeathSound() {
        return SoundEvents.PLAYER_BURP;
    }

    @Override
    public boolean removeWhenFarAway(double d) {
        return true;
    }
}
