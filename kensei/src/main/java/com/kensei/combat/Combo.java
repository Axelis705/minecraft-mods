package com.kensei.combat;

import com.kensei.KenseiConfig;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Combos de katana: golpes seguidos (1, 2, 3) y un golpe final (4) que levanta al enemigo. Si el enemigo está en el aire, cada golpe lo
 * mantiene flotando; el último golpe aéreo lo manda al suelo con un remate. Todo se calcula en el servidor.
 */
public final class Combo {
    private static final String AIR_UNTIL = "kensei_air_until";
    private static final String AIR_HITS = "kensei_air_hits";

    /** Mientras se reparte daño en área no se vuelven a contar combos. */
    private static boolean inAoe = false;

    private static final List<Slam> SLAMS = new ArrayList<>();

    private Combo() {
    }

    private static final class Slam {
        final LivingEntity target;
        final ServerPlayer owner;
        final long start;

        Slam(LivingEntity target, ServerPlayer owner, long start) {
            this.target = target;
            this.owner = owner;
            this.start = start;
        }
    }

    public static boolean inAoe() {
        return inAoe;
    }

    /** Se llama cuando el jugador (con katana en la mano principal) va a hacer daño con un golpe normal. */
    public static void onHit(ServerPlayer p, LivingEntity target, LivingHurtEvent e, CombatState s) {
        long now = p.level().getGameTime();
        boolean pvp = target instanceof Player;
        float bonus = 1.0F;

        // contraataque tras un parry o esquive perfecto
        if (now <= s.counterUntil) {
            bonus *= (float) (double) KenseiConfig.COUNTER_MULTIPLIER.get();
            s.counterUntil = 0;
            s.dirty = true;
            ServerLevel lv = p.serverLevel();
            lv.sendParticles(ParticleTypes.CRIT, target.getX(), target.getY() + target.getBbHeight() * 0.6, target.getZ(), 18, 0.3, 0.3, 0.3, 0.4);
            lv.sendParticles(ParticleTypes.SWEEP_ATTACK, target.getX(), target.getY() + target.getBbHeight() * 0.6, target.getZ(), 1, 0, 0, 0, 0);
            lv.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1.0F, 0.8F);
        }

        CompoundTag td = target.getPersistentData();
        // en qué golpe va: lo dicen los tajos de Moves (1 a 4 normales, 5 crítico); 0 = golpe de otra fuente (se cuenta por golpes seguidos)
        int step = s.activeStep;
        boolean airborne = td.getLong(AIR_UNTIL) > now && !target.onGround();
        if (airborne) {
            int hits = td.getInt(AIR_HITS) + 1;
            if (step == 5) {
                hits = Math.max(hits, KenseiConfig.AIR_HITS_BEFORE_SLAM.get());
            }
            td.putInt(AIR_HITS, hits);
            if (hits >= KenseiConfig.AIR_HITS_BEFORE_SLAM.get()) {
                bonus *= (float) (double) KenseiConfig.SLAM_MULTIPLIER.get();
                slam(p, target, now);
                td.remove(AIR_UNTIL);
                td.remove(AIR_HITS);
                s.comboStep = 0;
            } else {
                bonus *= 1.1F;
                hover(p, target);
                s.comboStep = Math.min(hits + 4, 9);
            }
        } else if (step == 5 || step == 7) {
            s.comboStep = 0; // el crítico y la estocada no cuentan para el combo
        } else if (step > 0) {
            if (step >= 4) {
                bonus *= (float) (double) KenseiConfig.FINISHER_MULTIPLIER.get();
                if (canLaunch(target)) {
                    launch(p, target);
                    td.putLong(AIR_UNTIL, now + 70);
                    td.putInt(AIR_HITS, 0);
                }
                s.comboStep = 0;
            } else {
                bonus *= 1.0F + 0.1F * (step - 1);
                s.comboStep = step;
            }
        } else if (now != s.lastHitTick) {
            if (now - s.lastHitTick <= KenseiConfig.COMBO_WINDOW.get() && s.comboStep > 0) {
                s.comboStep++;
            } else {
                s.comboStep = 1;
            }
            if (s.comboStep >= 4) {
                bonus *= (float) (double) KenseiConfig.FINISHER_MULTIPLIER.get();
                if (canLaunch(target)) {
                    launch(p, target);
                    td.putLong(AIR_UNTIL, now + 70);
                    td.putInt(AIR_HITS, 0);
                }
                s.comboStep = 0;
            } else {
                bonus *= 1.0F + 0.1F * (s.comboStep - 1);
            }
        }
        s.lastHitTick = now;
        s.dirty = true;

        if (pvp) {
            bonus = 1.0F + (bonus - 1.0F) * (float) (double) KenseiConfig.PVP_BONUS_SCALE.get();
        }
        e.setAmount(e.getAmount() * bonus);
    }

    private static boolean canLaunch(LivingEntity t) {
        if (t.isNoGravity() || t.getMaxHealth() > 150.0F || t.isPassenger()) {
            return false;
        }
        return !(t instanceof Player) || KenseiConfig.PVP_LAUNCH.get();
    }

    private static void launch(ServerPlayer p, LivingEntity t) {
        Vec3 look = p.getLookAngle();
        Vec3 flat = new Vec3(look.x, 0, look.z);
        flat = flat.lengthSqr() < 1.0E-4 ? Vec3.ZERO : flat.normalize().scale(0.15);
        t.setDeltaMovement(flat.x, KenseiConfig.LAUNCH_POWER.get(), flat.z);
        t.hurtMarked = true;
        t.fallDistance = 0;
        // el jugador salta tras el enemigo
        p.setDeltaMovement(p.getDeltaMovement().x * 0.5, 0.62, p.getDeltaMovement().z * 0.5);
        p.hurtMarked = true;
        p.fallDistance = 0;
        ServerLevel lv = p.serverLevel();
        lv.sendParticles(ParticleTypes.CLOUD, t.getX(), t.getY() + 0.1, t.getZ(), 12, 0.3, 0.05, 0.3, 0.05);
        lv.playSound(null, t.getX(), t.getY(), t.getZ(), SoundEvents.PLAYER_ATTACK_KNOCKBACK, SoundSource.PLAYERS, 1.0F, 1.2F);
    }

    /** Golpe en el aire: el enemigo y el jugador se quedan flotando un poco más. */
    private static void hover(ServerPlayer p, LivingEntity t) {
        Vec3 m = t.getDeltaMovement();
        t.setDeltaMovement(m.x * 0.2, 0.24, m.z * 0.2);
        t.hurtMarked = true;
        t.fallDistance = 0;
        Vec3 pm = p.getDeltaMovement();
        p.setDeltaMovement(pm.x * 0.4, 0.30, pm.z * 0.4);
        p.hurtMarked = true;
        p.fallDistance = 0;
    }

    private static void slam(ServerPlayer p, LivingEntity t, long now) {
        t.setDeltaMovement(0, -1.6, 0);
        t.hurtMarked = true;
        SLAMS.add(new Slam(t, p, now));
        ServerLevel lv = p.serverLevel();
        lv.playSound(null, t.getX(), t.getY(), t.getZ(), SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS, 1.0F, 0.7F);
    }

    /** Cada tick del servidor: revisa los enemigos que van cayendo por un remate y, al tocar el suelo, golpea en área. */
    public static void tickSlams(long now) {
        Iterator<Slam> it = SLAMS.iterator();
        while (it.hasNext()) {
            Slam sl = it.next();
            LivingEntity t = sl.target;
            if (t.isRemoved() || !t.isAlive() || now - sl.start > 40) {
                it.remove();
                continue;
            }
            if (now - sl.start >= 2 && t.onGround()) {
                it.remove();
                landSlam(sl);
            }
        }
    }

    private static void landSlam(Slam sl) {
        LivingEntity t = sl.target;
        if (!(t.level() instanceof ServerLevel lv)) {
            return;
        }
        lv.sendParticles(ParticleTypes.EXPLOSION, t.getX(), t.getY() + 0.1, t.getZ(), 1, 0, 0, 0, 0);
        lv.sendParticles(ParticleTypes.CLOUD, t.getX(), t.getY() + 0.1, t.getZ(), 25, 1.2, 0.05, 1.2, 0.08);
        lv.playSound(null, t.getX(), t.getY(), t.getZ(), SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 0.6F, 1.4F);
        double dmg = KenseiConfig.SLAM_AOE_DAMAGE.get();
        if (dmg <= 0 || !sl.owner.isAlive()) {
            return;
        }
        inAoe = true;
        try {
            for (LivingEntity o : lv.getEntitiesOfClass(LivingEntity.class, t.getBoundingBox().inflate(3.0, 1.0, 3.0))) {
                if (o == t || o == sl.owner) {
                    continue;
                }
                if (o instanceof Player op && (!sl.owner.canHarmPlayer(op) || com.kensei.duel.DuelManager.allies(sl.owner, op))) {
                    continue;
                }
                o.hurt(lv.damageSources().playerAttack(sl.owner), (float) dmg);
                o.knockback(0.5, sl.owner.getX() - o.getX(), sl.owner.getZ() - o.getZ());
            }
        } finally {
            inAoe = false;
        }
    }

    public static void clearAll() {
        SLAMS.clear();
    }
}
