package com.kensei.combat;

import com.kensei.Kensei;
import com.kensei.KenseiConfig;
import com.kensei.net.FxPacket;
import com.kensei.net.KenseiNet;
import com.kensei.net.StatePacket;
import com.kensei.registry.ModSounds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import com.kensei.item.KatanaItem;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEquipmentChangeEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Las reglas del combate en el servidor: invulnerabilidad del esquive, parry perfecto, bloqueo, ganancia de energía y combos.
 * Funciona igual contra mobs y contra jugadores.
 */
@Mod.EventBusSubscriber(modid = Kensei.MODID)
public final class CombatEvents {
    private CombatEvents() {
    }

    /** ¿Es un ataque (de algo o alguien) y no, por ejemplo, caída, hambre o vacío? */
    private static boolean isAttack(DamageSource src) {
        if (src.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return false;
        }
        return src.getEntity() != null || src.getDirectEntity() != null || src.is(DamageTypeTags.IS_PROJECTILE) || src.is(DamageTypeTags.IS_EXPLOSION);
    }

    /** ¿El golpe viene de enfrente del jugador (como el escudo)? */
    private static boolean frontal(LivingEntity victim, DamageSource src) {
        Vec3 pos = src.getSourcePosition();
        if (pos == null) {
            return false;
        }
        Vec3 look = victim.getViewVector(1.0F);
        Vec3 flatLook = new Vec3(look.x, 0, look.z);
        Vec3 to = pos.vectorTo(victim.position());
        Vec3 flatTo = new Vec3(to.x, 0, to.z);
        if (flatLook.lengthSqr() < 1.0E-6 || flatTo.lengthSqr() < 1.0E-6) {
            return true;
        }
        return flatTo.normalize().dot(flatLook.normalize()) < -0.2;
    }

    // ------------------------------------------------------------------ evitar o parar el golpe

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onAttack(LivingAttackEvent e) {
        LivingEntity target = e.getEntity();
        if (target.level().isClientSide) {
            return;
        }
        DamageSource src = e.getSource();
        // Las katanas pegan más rápido que el tiempo de protección normal de los mobs (10 ticks): sin esto se perderían golpes del combo.
        if (src.getEntity() instanceof ServerPlayer att && src.getDirectEntity() == att && src.is(DamageTypes.PLAYER_ATTACK)
                && KatanaUtil.mainHandKatana(att) && target.invulnerableTime > 10) {
            target.invulnerableTime = 10;
        }
        if (!(target instanceof ServerPlayer p) || !isAttack(src)) {
            return;
        }
        long now = p.level().getGameTime();
        CombatState s = CombatState.of(p);

        // esquive: durante unos ticks nada te toca
        if (now < s.dodgeUntil) {
            e.setCanceled(true);
            if (now - s.lastPerfectTick > 6) {
                s.lastPerfectTick = now;
                perfectDodge(p, s, now);
            }
            return;
        }

        // parry perfecto: el golpe llega justo al empezar a bloquear
        if (KatanaUtil.guarding(p) && frontal(p, src) && p.getTicksUsingItem() <= KenseiConfig.PARRY_WINDOW.get()) {
            e.setCanceled(true);
            if (now - s.lastPerfectTick > 4) {
                s.lastPerfectTick = now;
                perfectParry(p, s, src, now);
            }
            s.dodgeUntil = Math.max(s.dodgeUntil, now + 4);
        }
    }

    private static void perfectDodge(ServerPlayer p, CombatState s, long now) {
        com.kensei.duel.DuelManager.stat(p, 1);
        s.add((float) (double) KenseiConfig.GAIN_ON_PERFECT_DODGE.get());
        s.counterUntil = now + KenseiConfig.COUNTER_WINDOW.get();
        s.dirty = true;
        ServerLevel lv = p.serverLevel();
        lv.sendParticles(ParticleTypes.ENCHANTED_HIT, p.getX(), p.getY() + 1.0, p.getZ(), 14, 0.4, 0.5, 0.4, 0.2);
        lv.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.9F, 1.6F);
        KenseiNet.fx(lv, FxPacket.PERFECT_DODGE, p.getX(), p.getY() + 1.0, p.getZ(), 0.0F, 0, -1, p.getId(), p.getYRot());
    }

    private static void perfectParry(ServerPlayer p, CombatState s, DamageSource src, long now) {
        com.kensei.duel.DuelManager.stat(p, 0);
        s.add((float) (double) KenseiConfig.GAIN_ON_PARRY.get());
        s.addStamina((float) (double) KenseiConfig.PARRY_REFUND.get());
        s.counterUntil = now + KenseiConfig.COUNTER_WINDOW.get();
        s.dirty = true;
        Entity att = src.getEntity();
        if (att instanceof LivingEntity le && le != p) {
            le.knockback(0.8, p.getX() - le.getX(), p.getZ() - le.getZ());
            int stun = KenseiConfig.STUN_TICKS.get();
            if (stun > 0) {
                le.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, stun, 9));
                le.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, stun, 1));
            }
        }
        Vec3 look = p.getLookAngle();
        ServerLevel lv = p.serverLevel();
        lv.sendParticles(ParticleTypes.CRIT, p.getX() + look.x * 0.8, p.getY() + 1.3, p.getZ() + look.z * 0.8, 16, 0.2, 0.2, 0.2, 0.5);
        lv.sendParticles(ParticleTypes.ENCHANTED_HIT, p.getX() + look.x * 0.8, p.getY() + 1.3, p.getZ() + look.z * 0.8, 10, 0.2, 0.2, 0.2, 0.3);
        lv.playSound(null, p.getX(), p.getY(), p.getZ(), ModSounds.PARRY.get(), SoundSource.PLAYERS, 1.1F, 1.0F);
        lv.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.6F, 1.9F);
        KenseiNet.fx(lv, FxPacket.PARRY, p.getX() + look.x * 0.9, p.getY() + 1.3, p.getZ() + look.z * 0.9, 0.0F, 0, att == null ? -1 : att.getId(), p.getId(), p.getYRot());
    }

    // ------------------------------------------------------------------ daño: bloqueo normal y combos

    @SubscribeEvent
    public static void onHurt(LivingHurtEvent e) {
        LivingEntity victim = e.getEntity();
        if (victim.level().isClientSide) {
            return;
        }
        DamageSource src = e.getSource();

        // bloqueo tardío: reduce el daño pero no lo anula; cada golpe bloqueado gasta estamina y, si no alcanza, se rompe la guardia
        if (victim instanceof ServerPlayer vp && KatanaUtil.guarding(vp) && isAttack(src) && frontal(vp, src)) {
            CombatState bs = CombatState.of(vp);
            long bnow = vp.level().getGameTime();
            float bcost = (float) (KenseiConfig.COST_BLOCK_BASE.get() + KenseiConfig.COST_BLOCK_PER_DAMAGE.get() * e.getAmount());
            if (!bs.canSpendStamina(bcost, bnow)) {
                bs.useStamina(1000.0F, bnow);
                Moves.breakGuard(vp); // sin estamina no se aguanta el golpe: la guardia se rompe y el daño entra completo
            } else {
            bs.useStamina(bcost, bnow);
            e.setAmount(e.getAmount() * (float) (1.0 - KenseiConfig.BLOCK_REDUCTION.get()));
            ServerLevel bl = vp.serverLevel();
            Vec3 bf = vp.getLookAngle();
            bl.sendParticles(ParticleTypes.CRIT, vp.getX() + bf.x * 0.7, vp.getY() + 1.2, vp.getZ() + bf.z * 0.7, 7, 0.15, 0.2, 0.15, 0.35);
            bl.playSound(null, vp.getX(), vp.getY(), vp.getZ(), SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 0.8F, 1.25F);
            KenseiNet.shake(vp, 0.5F, 4);
            KenseiNet.fx(bl, FxPacket.BLOCK, vp.getX() + bf.x * 0.8, vp.getY() + 1.2, vp.getZ() + bf.z * 0.8, e.getAmount(), 0,
                    src.getEntity() == null ? -1 : src.getEntity().getId(), vp.getId(), vp.getYRot());
            }
        }

        // combos del atacante
        if (src.getEntity() instanceof ServerPlayer p && src.getDirectEntity() == p && victim != p
                && src.is(DamageTypes.PLAYER_ATTACK) && KatanaUtil.mainHandKatana(p) && !Combo.inAoe()) {
            Combo.onHit(p, victim, e, CombatState.of(p));
        }
    }

    // ------------------------------------------------------------------ energía

    @SubscribeEvent
    public static void onDamage(LivingDamageEvent e) {
        LivingEntity victim = e.getEntity();
        if (victim.level().isClientSide || e.getAmount() <= 0) {
            return;
        }
        DamageSource src = e.getSource();
        // efectos del golpe: el atacante con katana (número de daño, estrellas y ondas) y el jugador que recibe daño
        if (victim.level() instanceof ServerLevel sl && isAttack(src)) {
            double hy = victim.getY() + victim.getBbHeight() * 0.62;
            if (src.getEntity() instanceof ServerPlayer att && src.getDirectEntity() == att && src.is(DamageTypes.PLAYER_ATTACK)
                    && KatanaUtil.mainHandKatana(att) && !Combo.inAoe()) {
                com.kensei.duel.DuelManager.stat(att, 2);
                boolean lethal = victim.getHealth() + victim.getAbsorptionAmount() - e.getAmount() <= 0.0F;
                KenseiNet.fx(sl, FxPacket.HIT, victim.getX(), hy, victim.getZ(), e.getAmount(), CombatState.of(att).activeStep | (lethal ? FxPacket.KILL_FLAG : 0),
                        att.getId(), victim.getId(), att.getYRot());
            } else if (victim instanceof ServerPlayer vp && KatanaUtil.holdsKatana(vp)) {
                KenseiNet.fx(sl, FxPacket.DAMAGE_TAKEN, victim.getX(), hy, victim.getZ(), e.getAmount(), 0,
                        src.getEntity() == null ? -1 : src.getEntity().getId(), vp.getId(), vp.getYRot());
            }
        }
        // la energía ya no se gana pegando: se recarga en reposo; recibir daño también pausa la recarga
        if (victim instanceof ServerPlayer vp && KatanaUtil.holdsKatana(vp) && isAttack(src)) {
            CombatState.of(vp).markAction(vp.level().getGameTime());
        }
    }

    // ------------------------------------------------------------------ cada tick, sesión y muerte

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent e) {
        if (e.phase != TickEvent.Phase.END || !(e.player instanceof ServerPlayer p)) {
            return;
        }
        long now = p.level().getGameTime();
        CombatState s = CombatState.of(p);
        double regen = KenseiConfig.REST_REGEN_PER_SECOND.get();
        if (regen > 0 && s.energy < KenseiConfig.maxEnergy() && now - s.lastActionTick >= KenseiConfig.REST_DELAY.get()) {
            s.regen((float) (regen / 20.0));
        }
        if (s.counterUntil > 0 && now > s.counterUntil) {
            s.counterUntil = 0;
            s.dirty = true;
        }
        if (KenseiConfig.STAMINA_ENABLED.get() && s.stamina < KenseiConfig.staminaMax() && now >= s.staminaRegenAt && now >= s.exhaustedUntil) {
            float perTick = (float) (KenseiConfig.STAMINA_REGEN.get() / 20.0);
            if (KatanaUtil.guarding(p)) {
                perTick *= (float) (double) KenseiConfig.STAMINA_GUARD_REGEN.get();
            }
            s.addStamina(perTick);
        }
        Moves.tick(p, s, now);
        if (s.comboStep > 0 && now - s.lastHitTick > KenseiConfig.COMBO_WINDOW.get()) {
            s.comboStep = 0;
            s.dirty = true;
        }
        if (s.dirty && now - s.lastSyncTick >= 2) {
            s.dirty = false;
            s.lastSyncTick = now;
            int counter = s.counterUntil > now ? (int) (s.counterUntil - now) : 0;
            int heavyCd = s.heavyReadyAt > now ? (int) (s.heavyReadyAt - now) : 0;
            int tired = s.exhaustedUntil > now ? (int) (s.exhaustedUntil - now) : 0;
            KenseiNet.sendTo(p, new StatePacket(s.energy, counter, s.comboStep, heavyCd, s.stamina, KenseiConfig.staminaMax(), tired));
        }
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent e) {
        if (e.phase == TickEvent.Phase.END) {
            Combo.tickSlams(e.getServer().overworld().getGameTime());
        }
    }

    /** Al sacar la katana (pasarla a la mano principal) suena el roce del acero. */
    @SubscribeEvent
    public static void onEquip(LivingEquipmentChangeEvent e) {
        if (e.getSlot() == EquipmentSlot.MAINHAND && e.getEntity() instanceof ServerPlayer p
                && e.getTo().getItem() instanceof KatanaItem && !(e.getFrom().getItem() instanceof KatanaItem)) {
            p.level().playSound(null, p.getX(), p.getY(), p.getZ(), ModSounds.DRAW.get(), SoundSource.PLAYERS, 0.6F, 1.0F);
        }
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) {
            CombatState.of(p).dirty = true;
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) {
        CombatState.remove(e.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) {
            CombatState.of(p).reset();
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent e) {
        CombatState.clearAll();
        Combo.clearAll();
    }
}
