package com.kensei.combat;

import com.kensei.KenseiConfig;
import com.kensei.net.KenseiNet;
import com.kensei.registry.ModSounds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.AttackEntityEvent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Los ataques con katana, estilo "Combat Warriors": el clic izquierdo lanza una cadena de 4 tajos (el cuarto es el final del combo) y la
 * tecla R un ataque crítico que rompe la guardia. El cliente solo avisa "quiero atacar"; aquí se decide si se puede, se da un paso
 * adelante hacia el enemigo más cercano y se busca a quién alcanza el golpe en un abanico delante del jugador (no hace falta apuntar
 * exacto, sobre todo en tercera persona). Todo se calcula en el servidor.
 */
public final class Moves {
    /** Códigos de animación que se mandan a los clientes: 1 a 4 = tajo del combo, 5 = crítico, 6 = esquive. */
    public static final int ANIM_HEAVY = 5;
    public static final int ANIM_DODGE = 6;
    /** Estocada corriendo (clic izquierdo mientras se corre, sin cadena de tajos en curso). */
    public static final int ANIM_THRUST = 7;

    /** Ticks de margen para el aviso de red: un clic que llega 1 o 2 ticks antes de tiempo por la latencia no se pierde. */
    private static final int LAG_SLACK = 2;

    private Moves() {
    }

    private static boolean canAct(ServerPlayer p) {
        if (com.kensei.duel.DuelManager.frozen(p)) {
            return false; // cuenta atrás o fin de ronda de un duelo
        }
        if (!p.isAlive() || p.isSpectator() || p.isPassenger() || p.isSleeping() || p.isFallFlying() || p.isDeadOrDying()) {
            return false;
        }
        MobEffectInstance slow = p.getEffect(MobEffects.MOVEMENT_SLOWDOWN);
        return slow == null || slow.getAmplifier() < 4; // aturdido
    }

    // ------------------------------------------------------------------ pedir un ataque

    /** Cuánta estamina cuesta un tajo (1 a 3 normales, 4 final, 7 estocada). */
    public static float swingCost(int step) {
        double c = step == ANIM_THRUST ? KenseiConfig.COST_THRUST.get() : step >= 4 ? KenseiConfig.COST_FINISHER.get() : KenseiConfig.COST_LIGHT.get();
        return (float) c;
    }

    /** No hay estamina para atacar: un aviso corto (no se repite en cada clic). */
    private static void denyNoStamina(ServerPlayer p, CombatState s, long now) {
        if (now - s.lastDenyTick >= 12) {
            s.lastDenyTick = now;
            p.displayClientMessage(Component.translatable(s.exhausted(now) ? "kensei.msg.exhausted" : "kensei.msg.no_stamina"), true);
            p.level().playSound(null, p.getX(), p.getY(), p.getZ(), net.minecraft.sounds.SoundEvents.DISPENSER_FAIL, SoundSource.PLAYERS, 0.4F, 1.5F);
        }
        s.dirty = true;
    }

    /** Se gastó la última estamina: jadeo, un momento sin poder hacer nada fuerte y más lento. */
    public static void onExhausted(ServerPlayer p) {
        p.displayClientMessage(Component.translatable("kensei.msg.exhausted"), true);
        ServerLevel lv = p.serverLevel();
        lv.playSound(null, p.getX(), p.getY(), p.getZ(), net.minecraft.sounds.SoundEvents.PLAYER_BREATH, SoundSource.PLAYERS, 0.9F, 0.9F);
        lv.sendParticles(ParticleTypes.CLOUD, p.getX(), p.getY() + 1.7, p.getZ(), 4, 0.2, 0.1, 0.2, 0.01);
        int t = KenseiConfig.EXHAUST_TICKS.get();
        if (t > 0) {
            p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, t, 1, false, false, false));
        }
    }

    /** Atacar frena un poco mientras dura el golpe: hay que decidir cuándo pegar. */
    private static void slowForAttack(ServerPlayer p, int ticks, int amp) {
        if (KenseiConfig.ATTACK_SLOWS.get() && ticks > 0) {
            p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ticks, amp, false, false, false));
        }
    }

    /** Clic izquierdo con katana. */
    public static void trySwing(ServerPlayer p) {
        if (!canAct(p) || !KatanaUtil.mainHandKatana(p) || KatanaUtil.guarding(p)) {
            return;
        }
        long now = p.level().getGameTime();
        CombatState s = CombatState.of(p);
        if (now < s.swingReadyAt - LAG_SLACK) {
            return;
        }
        boolean chain = s.swingStep > 0 && now - s.lastSwingTick <= KenseiConfig.COMBO_WINDOW.get();
        boolean thrust = !chain && p.isSprinting() && p.onGround();
        int step = thrust ? ANIM_THRUST : (chain ? s.swingStep % 4 + 1 : 1);
        float cost = swingCost(step);
        if (!s.canSpendStamina(cost, now)) {
            denyNoStamina(p, s, now);
            return;
        }
        if (thrust) {
            s.swingStep = 0; // corriendo y sin cadena en curso: estocada
            s.swingReadyAt = now + KenseiConfig.THRUST_COOLDOWN.get();
        } else {
            s.swingStep = step;
            s.swingReadyAt = now + (step >= 4 ? KenseiConfig.M1_RECOVERY.get() : KenseiConfig.M1_INTERVAL.get());
        }
        if (s.useStamina(cost, now)) {
            onExhausted(p);
        }
        slowForAttack(p, thrust ? 0 : (step >= 4 ? 12 : 7), 0);
        s.lastSwingTick = now;
        s.markAction(now);
        s.pendingStep = step;
        s.pendingHitAt = now + KenseiConfig.M1_HIT_DELAY.get();

        lunge(p, step);
        p.swing(InteractionHand.MAIN_HAND, false);
        p.serverLevel().playSound(null, p.getX(), p.getY(), p.getZ(), ModSounds.SLASH.get(), SoundSource.PLAYERS, 0.8F, 0.92F + 0.05F * step);
        KenseiNet.broadcastAnim(p, step, 0.0F, false);
    }

    /** Tecla R: ataque crítico. */
    public static void tryHeavy(ServerPlayer p) {
        if (!canAct(p) || !KatanaUtil.mainHandKatana(p) || KatanaUtil.guarding(p)) {
            return;
        }
        long now = p.level().getGameTime();
        CombatState s = CombatState.of(p);
        if (now < s.swingReadyAt - LAG_SLACK || now < s.heavyReadyAt - LAG_SLACK) {
            return;
        }
        float heavyCost = (float) (double) KenseiConfig.COST_HEAVY.get();
        if (!s.canSpendStamina(heavyCost, now)) {
            denyNoStamina(p, s, now);
            return;
        }
        if (s.useStamina(heavyCost, now)) {
            onExhausted(p);
        }
        int windup = KenseiConfig.HEAVY_WINDUP.get();
        slowForAttack(p, windup + KenseiConfig.HEAVY_RECOVERY.get(), 1);
        s.pendingHeavyAt = now + windup;
        s.markAction(now);
        s.pendingHitAt = 0;
        s.swingStep = 0;
        s.swingReadyAt = now + windup + KenseiConfig.HEAVY_RECOVERY.get();
        s.heavyReadyAt = now + windup + KenseiConfig.HEAVY_COOLDOWN.get();
        s.dirty = true;
        ServerLevel lv = p.serverLevel();
        lv.playSound(null, p.getX(), p.getY(), p.getZ(), ModSounds.HEAVY_CHARGE.get(), SoundSource.PLAYERS, 0.8F, 1.0F);
        lv.sendParticles(ParticleTypes.ENCHANTED_HIT, p.getX(), p.getY() + 1.0, p.getZ(), 6, 0.3, 0.4, 0.3, 0.1);
        KenseiNet.broadcastAnim(p, ANIM_HEAVY, 0.0F, true);
    }

    // ------------------------------------------------------------------ cada tick: los golpes que caen

    public static void tick(ServerPlayer p, CombatState s, long now) {
        if (s.pendingHitAt > 0 && now >= s.pendingHitAt) {
            int step = s.pendingStep;
            s.pendingHitAt = 0;
            if (p.isAlive() && KatanaUtil.mainHandKatana(p)) {
                executeSwing(p, s, step);
            }
        }
        if (s.pendingHeavyAt > 0 && now >= s.pendingHeavyAt) {
            s.pendingHeavyAt = 0;
            if (p.isAlive() && KatanaUtil.mainHandKatana(p)) {
                executeHeavy(p, s);
            }
        }
    }

    /** El golpe no tocó a nadie: se pierde más estamina (y la recarga espera) para que no se pueda machacar el clic al aire. */
    private static void whiff(ServerPlayer p, CombatState s, float factor) {
        float extra = (float) (double) KenseiConfig.COST_WHIFF.get() * factor;
        if (extra > 0 && s.useStamina(extra, p.level().getGameTime())) {
            onExhausted(p);
        }
    }

    private static void executeSwing(ServerPlayer p, CombatState s, int step) {
        ServerLevel lv = p.serverLevel();
        Vec3 f = flatLook(p);
        // la estela del tajo (el abanico blanco del juego)
        lv.sendParticles(ParticleTypes.SWEEP_ATTACK, p.getX() + f.x * 1.5, p.getY() + p.getBbHeight() * 0.65, p.getZ() + f.z * 1.5, 1, 0, 0, 0, 0);
        boolean thrust = step == ANIM_THRUST;
        List<LivingEntity> targets = findTargets(p, thrust ? KenseiConfig.THRUST_RANGE.get() : KenseiConfig.M1_RANGE.get(),
                thrust ? KenseiConfig.THRUST_ANGLE.get() : KenseiConfig.M1_ANGLE.get(), KenseiConfig.M1_MAX_TARGETS.get());
        if (targets.isEmpty()) {
            whiff(p, s, 1.0F); // golpe al aire: gasta estamina de más
            return;
        }
        float scale = (float) (double) KenseiConfig.M1_DAMAGE_SCALE.get();
        if (thrust) {
            scale *= (float) (double) KenseiConfig.THRUST_DAMAGE.get();
        }
        boolean any = false;
        s.activeStep = step;
        try {
            for (LivingEntity t : targets) {
                if (hit(p, t, scale)) {
                    any = true;
                    lv.sendParticles(ParticleTypes.ENCHANTED_HIT, t.getX(), t.getY() + t.getBbHeight() * 0.6, t.getZ(), 5, 0.2, 0.2, 0.2, 0.25);
                    if (thrust && t.isAlive()) {
                        t.knockback(0.7, -f.x, -f.z);
                        lv.sendParticles(ParticleTypes.CRIT, t.getX(), t.getY() + t.getBbHeight() * 0.6, t.getZ(), 8, 0.2, 0.2, 0.2, 0.3);
                    }
                }
            }
        } finally {
            s.activeStep = 0;
        }
        if (any) {
            lv.playSound(null, p.getX(), p.getY(), p.getZ(), ModSounds.HIT.get(), SoundSource.PLAYERS, 0.9F, 0.95F + 0.04F * step);
            damageWeapon(p);
            KenseiNet.shake(p, (step == 4 || thrust) ? 1.1F : 0.45F, (step == 4 || thrust) ? 7 : 4);
        }
    }

    private static void executeHeavy(ServerPlayer p, CombatState s) {
        ServerLevel lv = p.serverLevel();
        Vec3 f = flatLook(p);
        lunge(p, 4);
        lv.sendParticles(ParticleTypes.SWEEP_ATTACK, p.getX() + f.x * 1.4, p.getY() + p.getBbHeight() * 0.6, p.getZ() + f.z * 1.4, 1, 0, 0, 0, 0);
        lv.sendParticles(ParticleTypes.SWEEP_ATTACK, p.getX() + f.x * 2.4, p.getY() + p.getBbHeight() * 0.45, p.getZ() + f.z * 2.4, 1, 0, 0, 0, 0);
        lv.playSound(null, p.getX(), p.getY(), p.getZ(), ModSounds.SLASH.get(), SoundSource.PLAYERS, 1.0F, 0.7F);
        List<LivingEntity> targets = findTargets(p, KenseiConfig.HEAVY_RANGE.get(), KenseiConfig.HEAVY_ANGLE.get(), KenseiConfig.HEAVY_MAX_TARGETS.get());
        if (targets.isEmpty()) {
            whiff(p, s, 2.0F);
            return;
        }
        float scale = (float) (double) KenseiConfig.HEAVY_DAMAGE.get();
        boolean any = false;
        s.activeStep = 5;
        try {
            for (LivingEntity t : targets) {
                if (KenseiConfig.HEAVY_BREAKS_GUARD.get()) {
                    breakGuard(t);
                }
                if (hit(p, t, scale)) {
                    any = true;
                    double kb = KenseiConfig.HEAVY_KNOCKBACK.get();
                    if (kb > 0 && t.isAlive()) {
                        t.knockback(kb, -f.x, -f.z);
                        Vec3 m = t.getDeltaMovement();
                        t.setDeltaMovement(m.x, Math.max(m.y, 0.28), m.z);
                        t.hurtMarked = true;
                    }
                    lv.sendParticles(ParticleTypes.CRIT, t.getX(), t.getY() + t.getBbHeight() * 0.6, t.getZ(), 16, 0.3, 0.3, 0.3, 0.45);
                    lv.sendParticles(ParticleTypes.EXPLOSION, t.getX(), t.getY() + t.getBbHeight() * 0.5, t.getZ(), 1, 0, 0, 0, 0);
                }
            }
        } finally {
            s.activeStep = 0;
        }
        if (any) {
            lv.playSound(null, p.getX(), p.getY(), p.getZ(), ModSounds.HEAVY_HIT.get(), SoundSource.PLAYERS, 1.1F, 1.0F);
            damageWeapon(p);
            KenseiNet.shake(p, 1.8F, 10);
        }
    }

    // ------------------------------------------------------------------ pegar

    /** Un golpe contra un objetivo, como el de un jugador (daño del arma + encantamientos). Devuelve si hizo daño. */
    private static boolean hit(ServerPlayer p, LivingEntity t, float scale) {
        if (MinecraftForge.EVENT_BUS.post(new AttackEntityEvent(p, t))) {
            return false;
        }
        ItemStack weapon = p.getMainHandItem();
        float dmg = ((float) p.getAttributeValue(Attributes.ATTACK_DAMAGE) + EnchantmentHelper.getDamageBonus(weapon, t.getMobType())) * scale;
        boolean ok = t.hurt(p.damageSources().playerAttack(p), dmg);
        if (!ok) {
            return false;
        }
        p.setLastHurtMob(t);
        int fire = EnchantmentHelper.getFireAspect(p);
        if (fire > 0 && !t.isOnFire()) {
            t.setSecondsOnFire(fire * 4);
        }
        int kb = EnchantmentHelper.getKnockbackBonus(p);
        if (kb > 0) {
            Vec3 f = flatLook(p);
            t.knockback(kb * 0.5, -f.x, -f.z);
        }
        EnchantmentHelper.doPostDamageEffects(p, t);
        p.causeFoodExhaustion(0.1F);
        // impacto: el enemigo se "congela" un instante (como una pausa al golpear) y salen chispas en la dirección del tajo
        if (t.isAlive() && !(t instanceof Player)) {
            t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 3, 5, false, false, false));
        }
        Vec3 look = flatLook(p);
        ServerLevel lv = p.serverLevel();
        for (int i = 0; i < 5; i++) {
            double sx = look.x + (p.getRandom().nextDouble() - 0.5) * 0.9;
            double sy = (p.getRandom().nextDouble() - 0.3) * 0.7;
            double sz = look.z + (p.getRandom().nextDouble() - 0.5) * 0.9;
            lv.sendParticles(ParticleTypes.CRIT, t.getX(), t.getY() + t.getBbHeight() * 0.6, t.getZ(), 0, sx, sy, sz, 0.55);
        }
        return true;
    }

    private static void damageWeapon(ServerPlayer p) {
        p.getMainHandItem().hurtAndBreak(1, p, pl -> pl.broadcastBreakEvent(InteractionHand.MAIN_HAND));
    }

    /** El crítico rompe la guardia: se baja la katana o el escudo y queda un momento sin poder bloquear. */
    public static void breakGuard(LivingEntity t) {
        if (t instanceof com.kensei.entity.RivalEntity r) {
            r.breakGuard();
            return;
        }
        if (!(t instanceof Player vp)) {
            return;
        }
        boolean broke = false;
        if (KatanaUtil.guarding(vp)) {
            Item it = vp.getUseItem().getItem();
            vp.stopUsingItem();
            vp.getCooldowns().addCooldown(it, 40);
            broke = true;
        } else if (vp.isBlocking()) {
            vp.disableShield(true);
            broke = true;
        }
        if (broke) {
            vp.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 14, 2, false, false, false));
            vp.level().playSound(null, vp.getX(), vp.getY(), vp.getZ(), ModSounds.GUARD_BREAK.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
            if (vp.level() instanceof ServerLevel gl) {
                KenseiNet.fx(gl, com.kensei.net.FxPacket.GUARD_BREAK, vp.getX(), vp.getY() + 1.3, vp.getZ(), 0.0F, 0, -1, vp.getId(), vp.getYRot());
            }
            if (vp instanceof ServerPlayer sv) {
                sv.displayClientMessage(Component.translatable("kensei.msg.guard_broken"), true);
            }
        }
    }

    // ------------------------------------------------------------------ puntería y avance

    private static Vec3 flatLook(LivingEntity e) {
        double yaw = Math.toRadians(e.getYRot());
        return new Vec3(-Math.sin(yaw), 0, Math.cos(yaw));
    }

    /** A quién alcanza un golpe: los seres vivos delante del jugador, dentro del alcance y del abanico, ordenados del más cercano al más lejano. */
    public static List<LivingEntity> findTargets(ServerPlayer p, double range, double halfAngleDeg, int max) {
        Vec3 f = flatLook(p);
        double cosMin = Math.cos(Math.toRadians(halfAngleDeg));
        AABB box = p.getBoundingBox().inflate(range + 1.0, 1.0, range + 1.0);
        List<LivingEntity> list = new ArrayList<>();
        for (LivingEntity e : p.level().getEntitiesOfClass(LivingEntity.class, box,
                o -> o != p && o.isAlive() && !o.isSpectator() && o.isAttackable() && !p.isPassengerOfSameVehicle(o))) {
            if (e instanceof TamableAnimal ta && ta.isOwnedBy(p)) {
                continue;
            }
            if (e instanceof Player op && !p.canHarmPlayer(op)) {
                continue;
            }
            if (com.kensei.duel.DuelManager.allies(p, e)) {
                continue;
            }
            AABB bb = e.getBoundingBox();
            if (bb.maxY < p.getY() - 0.8 || bb.minY > p.getY() + p.getBbHeight() + 0.4) {
                continue;
            }
            double dx = e.getX() - p.getX();
            double dz = e.getZ() - p.getZ();
            double dist = Math.sqrt(dx * dx + dz * dz);
            if (dist - e.getBbWidth() * 0.5 > range) {
                continue;
            }
            if (dist > 0.6 && (dx * f.x + dz * f.z) / dist < cosMin) {
                continue;
            }
            if (!p.hasLineOfSight(e)) {
                continue;
            }
            list.add(e);
        }
        list.sort(Comparator.comparingDouble(o -> o.distanceToSqr(p)));
        return list.size() > max ? new ArrayList<>(list.subList(0, max)) : list;
    }

    /** Paso adelante al atacar: se acerca al enemigo más cercano (sin pasarse) o da un pasito si no hay nadie. Solo en el suelo. */
    private static void lunge(ServerPlayer p, int step) {
        double base = KenseiConfig.M1_LUNGE.get();
        if (base <= 0 || !p.onGround()) {
            return;
        }
        Vec3 f = flatLook(p);
        List<LivingEntity> near = findTargets(p, 5.0, 45.0, 1);
        double power;
        double dist0 = 0.0;
        if (near.isEmpty()) {
            power = base * 0.5;
        } else {
            LivingEntity t = near.get(0);
            dist0 = Math.sqrt((t.getX() - p.getX()) * (t.getX() - p.getX()) + (t.getZ() - p.getZ()) * (t.getZ() - p.getZ())) - t.getBbWidth() * 0.5;
            power = dist0 <= 1.5 ? 0.0 : Math.min(base * 1.7, (dist0 - 1.3) * 0.2 + 0.05);
        }
        if (step == ANIM_THRUST) {
            power = near.isEmpty() ? base * 2.6 : (dist0 <= 1.5 ? 0.0 : Math.min(base * 3.0, (dist0 - 1.3) * 0.3 + 0.1));
        } else if (step >= 4) {
            power *= 1.25;
        }
        if (power <= 0) {
            return;
        }
        Vec3 m = p.getDeltaMovement();
        p.setDeltaMovement(m.x * 0.3 + f.x * power, m.y, m.z * 0.3 + f.z * power);
        p.hurtMarked = true;
    }
}
