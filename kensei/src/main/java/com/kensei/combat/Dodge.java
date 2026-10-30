package com.kensei.combat;

import com.kensei.KenseiConfig;
import com.kensei.net.KenseiNet;
import com.kensei.registry.ModSounds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.Vec3;

/** El esquive: un impulso rápido en la dirección que el jugador esté apretando, con invulnerabilidad corta. Cuesta energía. */
public final class Dodge {
    private Dodge() {
    }

    /**
     * @param fwd    -1 atrás, 0 nada, 1 adelante
     * @param strafe -1 izquierda, 0 nada, 1 derecha
     */
    public static void tryDodge(ServerPlayer p, int fwd, int strafe) {
        fwd = Math.max(-1, Math.min(1, fwd));
        strafe = Math.max(-1, Math.min(1, strafe));
        if (!p.isAlive() || p.isSpectator() || p.isPassenger() || p.isSleeping() || p.isFallFlying() || p.isDeadOrDying()) {
            return;
        }
        if (com.kensei.duel.DuelManager.frozen(p)) {
            return;
        }
        if (KenseiConfig.REQUIRE_KATANA.get() && !KatanaUtil.holdsKatana(p)) {
            p.displayClientMessage(Component.translatable("kensei.msg.need_katana"), true);
            return;
        }
        MobEffectInstance slow = p.getEffect(MobEffects.MOVEMENT_SLOWDOWN);
        if (slow != null && slow.getAmplifier() >= 4) {
            return; // aturdido
        }
        long now = p.level().getGameTime();
        CombatState s = CombatState.of(p);
        if (now < s.dodgeReadyAt - 1) {
            return;
        }
        float cost = (float) (double) KenseiConfig.DODGE_COST.get();
        if (!s.spend(cost)) {
            p.displayClientMessage(Component.translatable("kensei.msg.no_energy"), true);
            p.level().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.DISPENSER_FAIL, SoundSource.PLAYERS, 0.5F, 1.4F);
            s.dodgeReadyAt = now + 6;
            return;
        }
        s.markAction(now);
        s.dodgeUntil = now + KenseiConfig.DODGE_IFRAMES.get();
        s.dodgeReadyAt = now + KenseiConfig.DODGE_COOLDOWN.get();

        if (fwd == 0 && strafe == 0) {
            fwd = -1; // sin dirección: salto atrás
        }
        double yaw = Math.toRadians(p.getYRot());
        Vec3 forward = new Vec3(-Math.sin(yaw), 0, Math.cos(yaw));
        Vec3 right = new Vec3(-Math.cos(yaw), 0, -Math.sin(yaw));
        Vec3 v = forward.scale(fwd).add(right.scale(strafe)).normalize().scale(KenseiConfig.DODGE_POWER.get());
        double vy = p.onGround() ? 0.15 : Math.max(p.getDeltaMovement().y, -0.1);
        p.setDeltaMovement(v.x, vy, v.z);
        p.hurtMarked = true;
        p.fallDistance = 0;
        s.cancelPending(); // esquivar corta lo que estaba preparando
        float dirYaw = (float) Math.toDegrees(Math.atan2(-v.x, v.z));
        KenseiNet.broadcastAnim(p, Moves.ANIM_DODGE, dirYaw, true);

        ServerLevel level = p.serverLevel();
        level.sendParticles(ParticleTypes.CLOUD, p.getX(), p.getY() + 0.2, p.getZ(), 8, 0.25, 0.05, 0.25, 0.02);
        level.sendParticles(ParticleTypes.SWEEP_ATTACK, p.getX(), p.getY() + 1.0, p.getZ(), 1, 0, 0, 0, 0);
        level.playSound(null, p.getX(), p.getY(), p.getZ(), ModSounds.DODGE.get(), SoundSource.PLAYERS, 0.8F, 1.0F);
    }
}
