package com.mundosdeaxelis.relic;

import com.mundosdeaxelis.MundosDeAxelis;
import com.mundosdeaxelis.registry.ModItems;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Efectos de reliquias que dependen de eventos del juego. */
@Mod.EventBusSubscriber(modid = MundosDeAxelis.MODID)
public final class RelicEvents {
    private RelicEvents() {
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            ItemStack ring = RelicItem.carried(sp, ModItems.PHOENIX_RING);
            if (!ring.isEmpty() && !sp.getCooldowns().isOnCooldown(ring.getItem()) && !event.getSource().is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)) {
                event.setCanceled(true);
                PhoenixRingItem.rebirth(sp);
                return;
            }
        }
        if (event.getSource().getEntity() instanceof ServerPlayer killer && event.getEntity() != killer
                && !RelicItem.carried(killer, ModItems.WOLF_FANG).isEmpty()) {
            WolfFangItem.onKill(killer, event.getEntity());
        }
        if (event.getSource().getEntity() instanceof ServerPlayer killer2 && event.getEntity() != killer2) {
            SoulLanternItem.onKill(killer2, event.getEntity());
        }
    }

    @SubscribeEvent
    public static void onFall(LivingFallEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            if (!RelicItem.carried(sp, ModItems.FEATHER_TALISMAN).isEmpty()
                    || sp.getPersistentData().getLong("mdc_nofall") > sp.level().getGameTime()) {
                event.setDamageMultiplier(0.0F);
            } else if (!RelicItem.carried(sp, ModItems.CLOUD_BOTTLE).isEmpty()) {
                event.setDamageMultiplier(event.getDamageMultiplier() * 0.5F);
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onHurt(LivingHurtEvent event) {
        if (event.getEntity() instanceof ServerPlayer shielded && CelestialAegisItem.isActive(shielded)
                && !event.getSource().is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            event.setCanceled(true);
            CelestialAegisItem.absorb(shielded);
            return;
        }
        // ---- reliquias nuevas ----
        MusicBoxItem.onHurt(event);
        if (event.getEntity() instanceof ServerPlayer shield && MirrorShieldItem.isShielding(shield)
                && !event.getSource().is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            MirrorShieldItem.onHurt(event, shield);
        }
        if (event.getEntity() instanceof ServerPlayer linked) {
            RedThreadItem.onHurt(event, linked);
        }
        if (event.getSource().getEntity() instanceof ServerPlayer striker && event.getEntity() != striker) {
            LivingEntity target = event.getEntity();
            float amount = event.getAmount() * AstralSpyglassItem.bonus(target);
            if (target.isOnFire() && !RelicItem.carried(striker, ModItems.HATCHLING_EGG).isEmpty()) {
                amount *= HatchlingEggItem.BURN_BONUS;
            }
            event.setAmount(amount);
            if (!RelicItem.carried(striker, ModItems.ASTRAL_SPYGLASS).isEmpty() && target.level() instanceof ServerLevel) {
                target.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.GLOWING, 100, 0, false, false));
            }
        }
        if (event.getSource().getEntity() instanceof ServerPlayer hitter && event.getEntity() != hitter
                && !RelicItem.carried(hitter, ModItems.RAGE_GAUNTLET).isEmpty()) {
            RageGauntletItem.onHit(hitter, event.getEntity());
        }
        if (event.getEntity() instanceof ServerPlayer victim && !RelicItem.carried(victim, ModItems.WARRIOR_MEDAL).isEmpty()) {
            WarriorMedalItem.onHurt(victim);
        }
        if (event.getSource().getEntity() instanceof ServerPlayer attacker && event.getEntity() != attacker
                && !RelicItem.carried(attacker, ModItems.VAMPIRE_CHALICE).isEmpty()) {
            VampireChaliceItem.lifesteal(attacker, event.getAmount());
        }
    }
    /** El Escudo Espejo devuelve los proyectiles que le van a dar al jugador. */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onProjectile(ProjectileImpactEvent event) {
        if (event.getRayTraceResult() instanceof EntityHitResult hit && hit.getEntity() instanceof ServerPlayer sp
                && MirrorShieldItem.isShielding(sp)) {
            Projectile pr = event.getProjectile();
            if (pr.getOwner() != sp && pr.level() instanceof ServerLevel sl) {
                event.setCanceled(true);
                MirrorShieldItem.reflect(sl, sp, pr);
            }
        }
    }

    @SubscribeEvent
    public static void onStopped(ServerStoppedEvent event) {
        RewindAnchorItem.clearAll();
        RedThreadItem.clearAll();
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        RewindAnchorItem.forget(event.getEntity().getUUID());
    }
}
