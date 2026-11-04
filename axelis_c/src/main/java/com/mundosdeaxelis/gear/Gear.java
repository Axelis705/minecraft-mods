package com.mundosdeaxelis.gear;

import com.mundosdeaxelis.crate.Tier;
import com.mundosdeaxelis.fx.Fx;
import com.mundosdeaxelis.fx.Vfx;
import com.mundosdeaxelis.network.ModNetwork;
import com.mundosdeaxelis.network.OreRadarPacket;
import com.mundosdeaxelis.registry.ModSounds;
import com.mundosdeaxelis.relic.RelicFx;
import com.mundosdeaxelis.world.Scheduler;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.Tags;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** La lógica de las habilidades del equipo especial (armas y herramientas). Solo se ejecuta en el servidor. */
public final class Gear {
    private Gear() {
    }

    /** Verdadero mientras una habilidad provoca daño propio (evita que ese daño dispare otra vez los efectos). */
    public static boolean busy;

    // ------------------------------------------------------------------ texto

    public static void tooltip(GearTool t, List<Component> tip) {
        Tier tier = t.tier();
        Tier seen = t instanceof net.minecraft.world.item.Item it ? com.mundosdeaxelis.crate.Tiers.shown(it) : tier;
        tip.add(Component.translatable("tooltip.mundos_de_axelis.tier", seen.label()).withStyle(ChatFormatting.GRAY));
        GearDef d = t.def();
        if (d != null) {
            tip.add(Component.translatable("tooltip.mundos_de_axelis.set",
                    Component.translatable("set.mundos_de_axelis." + d.set().id).withStyle(Style.EMPTY.withColor(d.set().color)))
                    .withStyle(ChatFormatting.GOLD));
            for (Map.Entry<Perk, Integer> e : d.perks().entrySet()) {
                if (e.getKey() == Perk.SKILL) {
                    continue;
                }
                tip.add(perkLine(e.getKey(), e.getValue()).withStyle(ChatFormatting.GREEN));
            }
            Skill sk = Skill.byLevel(d.lvl(Perk.SKILL));
            if (sk != null) {
                tip.add(Component.translatable("perk.mundos_de_axelis.skill" + sk.level).withStyle(ChatFormatting.AQUA));
                tip.add(Component.translatable("tooltip.mundos_de_axelis.souls_cost", sk.souls).withStyle(ChatFormatting.DARK_AQUA));
                tip.add(Component.translatable("tooltip.mundos_de_axelis.gear.cooldown", Math.round(sk.cooldown * tier.cooldown / 20F))
                        .withStyle(ChatFormatting.DARK_GRAY));
            } else if (d.has(Perk.RADAR)) {
                tip.add(Component.translatable("tooltip.mundos_de_axelis.gear.cooldown", Math.round(RADAR_CD * tier.cooldown / 20F))
                        .withStyle(ChatFormatting.DARK_GRAY));
            }
            return;
        }
        Ability a = t.ability();
        tip.add(Component.translatable("tooltip.mundos_de_axelis.gear").withStyle(ChatFormatting.GOLD));
        tip.add(Component.translatable("tooltip.mundos_de_axelis." + a.id + ".1").withStyle(ChatFormatting.GRAY));
        if (a.hasActive()) {
            tip.add(Component.translatable("tooltip.mundos_de_axelis." + a.id + ".2").withStyle(ChatFormatting.AQUA));
            if (a.souls > 0) {
                tip.add(Component.translatable("tooltip.mundos_de_axelis.souls_cost", a.souls).withStyle(ChatFormatting.DARK_AQUA));
            }
            tip.add(Component.translatable("tooltip.mundos_de_axelis.gear.cooldown", Math.round(a.cooldown * tier.cooldown / 20F))
                    .withStyle(ChatFormatting.DARK_GRAY));
        }
        tip.add(Component.translatable("tooltip.mundos_de_axelis." + a.id + ".lore").withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC));
    }

    /** Línea de una capacidad. La tala, la veta y el área llevan los números reales del set (crecen set por set). */
    private static net.minecraft.network.chat.MutableComponent perkLine(Perk p, int level) {
        return switch (p) {
            case FELL -> Component.translatable("perk.mundos_de_axelis.fell_n", GearEvents.fellLogs(level));
            case VEIN -> Component.translatable("perk.mundos_de_axelis.vein_n", GearEvents.veinCap(level));
            case AREA -> {
                int r = GearEvents.areaR(level);
                yield Component.translatable("perk.mundos_de_axelis.area_n", 2 * r + 1, 2 * r + 1, GearEvents.areaD(level));
            }
            default -> Component.translatable("perk.mundos_de_axelis." + p.id + level);
        };
    }

    public static final int RADAR_CD = 20 * 8;

    // ------------------------------------------------------------------ pasivas

    public static void passive(GearTool t, ServerPlayer sp, ServerLevel sl) {
        int haste = t.lvl(Perk.HASTE);
        if (haste > 0 && sp.tickCount % 20 == 0) {
            sp.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 40, haste - 1, true, false, true));
        }
    }

    // ------------------------------------------------------------------ al golpear

    /** Devuelve el daño final del golpe (puede aumentarlo) y aplica los efectos del arma. */
    public static float onHit(Ability a, ServerPlayer sp, ServerLevel sl, LivingEntity target, float amount) {
        Tier tier = Tiers_of(a);
        float power = tier.power;
        Vec3 c = target.getBoundingBox().getCenter();
        switch (a) {
            case DAWN -> {
                if (sl.isDay() && sl.canSeeSky(sp.blockPosition())) {
                    amount *= 1.30F;
                    RelicFx.burst(sl, Fx.spark(a.color, 0.7F), c, 8, 0.2);
                    if (target.isInvertedHealAndHarm()) {
                        target.setSecondsOnFire(4);
                    }
                }
            }
            case FROST -> {
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 1));
                target.setTicksFrozen(Math.min(target.getTicksRequiredToFreeze() + 60, target.getTicksFrozen() + 100));
                RelicFx.burst(sl, Fx.shard(a.color, 0.7F), c, 10, 0.25);
            }
            case STORM -> {
                if (sl.random.nextFloat() < 0.35F) {
                    chain(sl, sp, target, 4.5F * power);
                }
            }
            case VAMPIRE -> {
                float k = sp.hasEffect(MobEffects.DAMAGE_BOOST) ? 0.30F : 0.15F;
                heal(sl, sp, Math.min(6.0F, amount * k));
            }
            case DRAGON -> {
                target.setSecondsOnFire(5);
                RelicFx.burst(sl, Fx.ember(a.color, 0.8F), c, 12, 0.25);
            }
            default -> {
            }
        }
        return amount;
    }

    private static Tier Tiers_of(Ability a) {
        return com.mundosdeaxelis.crate.Tiers.of(a.id);
    }

    private static void heal(ServerLevel sl, LivingEntity e, float hp) {
        if (hp <= 0 || !e.isAlive()) {
            return;
        }
        e.heal(hp);
        Vec3 p = e.getBoundingBox().getCenter();
        sl.sendParticles(Fx.heart(0xFF4F8B, 0.8F), p.x, p.y + 0.4, p.z, 2, 0.3, 0.3, 0.3, 0.0);
    }

    private static void chain(ServerLevel sl, ServerPlayer sp, LivingEntity first, float dmg) {
        Vec3 from = first.getBoundingBox().getCenter();
        Vfx.bolt(sl, from.add(0, 7, 0), from, 0xBFE0FF, 0.8, 7);
        sl.playSound(null, first.blockPosition(), SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 0.6F, 1.6F);
        Set<LivingEntity> done = new HashSet<>();
        done.add(first);
        LivingEntity cur = first;
        busy = true;
        try {
            for (int i = 0; i < 3; i++) {
                LivingEntity next = null;
                for (LivingEntity e : RelicFx.foes(sl, cur.getBoundingBox().getCenter(), 7.0, sp)) {
                    if (!done.contains(e)) {
                        next = e;
                        break;
                    }
                }
                if (next == null) {
                    break;
                }
                done.add(next);
                Vec3 a = cur.getBoundingBox().getCenter(), b = next.getBoundingBox().getCenter();
                Vfx.bolt(sl, a, b, 0xBFE0FF, 0.5, 6);
                Vfx.flash(sl, b, 0xBFE0FF, 1.4, 6);
                RelicFx.damage(sl, sp, next, dmg);
                cur = next;
            }
        } finally {
            busy = false;
        }
    }

    // ------------------------------------------------------------------ habilidad activa

    public static boolean tryActive(GearTool t, ServerPlayer sp, ServerLevel sl, ItemStack stack) {
        Item item = stack.getItem();
        if (sp.getCooldowns().isOnCooldown(item)) {
            return false;
        }
        Ability a = t.ability();
        GearDef d = t.def();
        int cost;
        int cd;
        Skill sk = null;
        if (a != null) {
            cost = a.souls;
            cd = a.cooldown;
        } else {
            sk = Skill.byLevel(d.lvl(Perk.SKILL));
            if (sk != null) {
                cost = sk.souls;
                cd = sk.cooldown;
            } else if (d.has(Perk.RADAR)) {
                cost = 0;
                cd = RADAR_CD;
            } else {
                return false;
            }
        }
        if (cost > 0 && Souls.get(sp) < cost) {
            sp.displayClientMessage(Component.translatable("message.mundos_de_axelis.souls_missing", cost, Souls.get(sp))
                    .withStyle(ChatFormatting.RED), true);
            sl.playSound(null, sp.blockPosition(), SoundEvents.SOUL_ESCAPE, SoundSource.PLAYERS, 0.6F, 0.7F);
            return false;
        }
        boolean ok = a != null ? active(a, sp, sl, stack) : (sk != null ? skill(d, sk, sp, sl, stack) : prospect(sp, sl));
        if (!ok) {
            return false;
        }
        if (cost > 0) {
            Souls.spend(sp, cost);
        }
        sp.getCooldowns().addCooldown(item, Math.round(cd * t.tier().cooldown));
        return true;
    }

    // ------------------------------------------------------------------ habilidades de las espadas de la escalera

    private static boolean skill(GearDef d, Skill s, ServerPlayer sp, ServerLevel sl, ItemStack stack) {
        float power = d.tier().power;
        float rf = 0.8F + 0.05F * d.set().rank;
        switch (s) {
            case HOP -> hop(sp, sl, d, rf);
            case SHOVE -> shove(sp, sl, d, rf);
            case SURGE -> surge(sp, sl, d, 0);
            case NOVA -> nova(sp, sl, d, 5.0, 6.0F * power);
            case RUSH -> {
                return active(Ability.DRAGON, sp, sl, stack);
            }
            case OVERDRIVE -> {
                surge(sp, sl, d, 1);
                nova(sp, sl, d, 7.0, 10.0F * power);
                heal(sl, sp, 4.0F);
            }
        }
        return true;
    }

    /** Brinco: un saltito hacia adelante y arriba, y caes despacio (la habilidad más sencilla). */
    private static void hop(ServerPlayer sp, ServerLevel sl, GearDef d, float rf) {
        Vec3 look = sp.getLookAngle();
        Vec3 h = new Vec3(look.x, 0, look.z);
        h = h.lengthSqr() < 1.0E-4 ? new Vec3(0, 0, 0) : h.normalize();
        double fwd = 0.35 + 0.15 * rf;
        sp.setDeltaMovement(h.x * fwd, 0.55 + 0.25 * rf, h.z * fwd);
        sp.hurtMarked = true;
        sp.fallDistance = 0;
        sp.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 100, 0));
        int col = d.set().color;
        Vec3 at = sp.position();
        Vfx.ring(sl, at.add(0, 0.1, 0), col, 1.8, 10);
        RelicFx.burst(sl, Fx.spark(col, 0.7F), at.add(0, 0.2, 0), 16, 0.25);
        sound(sl, at, ModSounds.RELIC_WHOOSH.get(), 0.6F, 1.5F);
    }

    /** Empujón: aparta a los enemigos cercanos sin hacerles daño. */
    private static void shove(ServerPlayer sp, ServerLevel sl, GearDef d, float rf) {
        int col = d.set().color;
        Vec3 at = sp.position();
        double radius = 3.0 + 1.5 * rf;
        Vfx.ring(sl, at.add(0, 0.1, 0), col, radius + 0.5, 12);
        RelicFx.burst(sl, Fx.spark(col, 0.8F), at.add(0, 0.6, 0), 24, 0.4);
        sound(sl, at, ModSounds.RELIC_WHOOSH.get(), 0.8F, 0.9F);
        for (LivingEntity e : RelicFx.foes(sl, at, radius, sp)) {
            RelicFx.knock(e, at, 0.7 + 0.25 * rf, 0.25);
        }
    }

    private static void surge(ServerPlayer sp, ServerLevel sl, GearDef d, int boost) {
        int amp = (d.tier().ordinal() >= Tier.EPIC.ordinal() ? 1 : 0) + boost;
        int dur = 160 + 20 * d.rung();
        sp.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, dur, Math.min(2, amp)));
        sp.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, dur, Math.min(2, amp)));
        int col = d.set().color;
        Vec3 c = sp.position().add(0, 1.0, 0);
        Vfx.flash(sl, c, col, 3.0, 10);
        Vfx.ring(sl, sp.position().add(0, 0.1, 0), col, 3.2, 14);
        RelicFx.burst(sl, Fx.ember(col, 0.9F), c, 26, 0.35);
        sound(sl, sp.position(), ModSounds.RELIC_WHOOSH.get(), 0.9F, 1.1F);
        sound(sl, sp.position(), ModSounds.RELIC_BOOM.get(), 0.6F, 1.3F);
    }

    private static void nova(ServerPlayer sp, ServerLevel sl, GearDef d, double radius, float dmg) {
        int col = d.set().color;
        Vec3 at = sp.position();
        Vfx.ring(sl, at.add(0, 0.1, 0), col, radius + 0.8, 16);
        Vfx.column(sl, at, col, 0.7, 2.6, 14);
        Vfx.flash(sl, at.add(0, 0.9, 0), col, 3.4, 10);
        RelicFx.burst(sl, Fx.spark(col, 0.9F), at.add(0, 0.8, 0), 40, 0.5);
        RelicFx.shakeNear(sl, at, 14, 0.6F, 8);
        sound(sl, at, ModSounds.RELIC_BOOM.get(), 1.0F, 1.0F);
        busy = true;
        try {
            for (LivingEntity e : RelicFx.foes(sl, at, radius, sp)) {
                RelicFx.damage(sl, sp, e, dmg);
                RelicFx.knock(e, at, 0.55, 0.28);
            }
        } finally {
            busy = false;
        }
    }

    /** Efectos de golpe de una espada de la escalera (fuego, hielo, rayos, vida). Devuelve el daño final. */
    public static float onHitDef(GearDef d, ServerPlayer sp, ServerLevel sl, LivingEntity target, float amount) {
        Vec3 c = target.getBoundingBox().getCenter();
        int fire = d.lvl(Perk.FIRE);
        if (fire > 0) {
            target.setSecondsOnFire(new int[]{0, 3, 5, 8}[Math.min(3, fire)]);
            RelicFx.burst(sl, Fx.ember(0xFF7A2A, 0.7F), c, 4 + 2 * fire, 0.2);
        }
        int frost = d.lvl(Perk.FROST);
        if (frost > 0) {
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60 + 20 * frost, frost - 1));
            if (frost >= 2) {
                target.setTicksFrozen(Math.min(target.getTicksRequiredToFreeze() + 40, target.getTicksFrozen() + 80));
            }
            RelicFx.burst(sl, Fx.shard(0x9FDBFF, 0.6F), c, 6, 0.2);
        }
        int zap = d.lvl(Perk.ZAP);
        if (zap > 0 && sl.random.nextFloat() < 0.10F + 0.10F * zap) {
            chain(sl, sp, target, 4.5F * d.tier().power);
        }
        int life = d.lvl(Perk.LIFE);
        if (life > 0) {
            heal(sl, sp, Math.min(4.0F, amount * (0.02F + 0.02F * life)));
        }
        return amount;
    }

    private static void sound(ServerLevel sl, Vec3 p, SoundEvent s, float vol, float pitch) {
        sl.playSound(null, p.x, p.y, p.z, s, SoundSource.PLAYERS, vol, pitch);
    }

    private static boolean active(Ability a, ServerPlayer sp, ServerLevel sl, ItemStack stack) {
        float power = Tiers_of(a).power;
        Vec3 look = sp.getLookAngle();
        Vec3 at = sp.position();
        switch (a) {
            case DAWN -> {
                Vec3 c = sp.getEyePosition().add(look.scale(2.0));
                Vfx.flash(sl, c, 0xFFF4C0, 4.5, 12);
                Vfx.ring(sl, at.add(0, 0.1, 0), a.color, 7.0, 14);
                sound(sl, at, ModSounds.RELIC_MAGIC.get(), 1.0F, 1.4F);
                for (LivingEntity e : RelicFx.foes(sl, at, 7.0, sp)) {
                    e.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0));
                    e.addEffect(new MobEffectInstance(MobEffects.GLOWING, 100, 0));
                    if (e.isInvertedHealAndHarm()) {
                        e.setSecondsOnFire(4);
                        RelicFx.damage(sl, sp, e, 4.0F * power);
                    }
                }
                return true;
            }
            case FROST -> {
                Vec3 c = at.add(0, 0.9, 0);
                Vfx.ring(sl, at.add(0, 0.1, 0), a.color, 5.5, 18);
                Vfx.flash(sl, c, 0xDFF4FF, 3.0, 10);
                RelicFx.burst(sl, Fx.shard(a.color, 0.9F), c, 50, 0.4);
                sound(sl, at, ModSounds.RELIC_ICE.get(), 1.0F, 1.0F);
                busy = true;
                try {
                    for (LivingEntity e : RelicFx.foes(sl, at, 5.5, sp)) {
                        RelicFx.damage(sl, sp, e, 4.0F * power);
                        e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 2));
                        e.setTicksFrozen(e.getTicksRequiredToFreeze() + 80);
                        RelicFx.knock(e, at, 0.3, 0.1);
                    }
                } finally {
                    busy = false;
                }
                return true;
            }
            case VAMPIRE -> {
                sp.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 200, 1));
                sp.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 200, 1));
                Vec3 c = at.add(0, 1.0, 0);
                Vfx.flash(sl, c, a.color, 3.0, 10);
                RelicFx.burst(sl, Fx.ember(a.color, 0.9F), c, 30, 0.3);
                sound(sl, at, ModSounds.RELIC_DARK.get(), 1.0F, 0.9F);
                return true;
            }
            case DRAGON -> {
                Vec3 h = new Vec3(look.x, 0, look.z);
                h = h.lengthSqr() < 1.0E-4 ? new Vec3(0, 0, 1) : h.normalize();
                sp.setDeltaMovement(h.x * 2.1, 0.18 + Math.max(0, look.y) * 0.6, h.z * 2.1);
                sp.hurtMarked = true;
                sp.fallDistance = 0;
                Vfx.slash(sl, sp.getEyePosition().add(look.scale(1.0)), look.scale(9), a.color, 4.2, 0.0, 10);
                Vfx.flash(sl, at.add(0, 1, 0), a.color, 3.0, 8);
                sound(sl, at, ModSounds.RELIC_WHOOSH.get(), 1.0F, 0.8F);
                sound(sl, at, ModSounds.RELIC_FIRE.get(), 0.8F, 1.0F);
                Set<LivingEntity> hit = new HashSet<>();
                for (int t = 1; t <= 8; t++) {
                    Scheduler.runLater(t, () -> {
                        if (!sp.isAlive()) {
                            return;
                        }
                        Vec3 p = sp.position().add(0, 1.0, 0);
                        RelicFx.burst(sl, Fx.ember(a.color, 0.8F), p, 6, 0.1);
                        sp.fallDistance = 0;
                        busy = true;
                        try {
                            for (LivingEntity e : RelicFx.foes(sl, p, 2.6, sp)) {
                                if (hit.add(e)) {
                                    RelicFx.damage(sl, sp, e, 9.0F * power);
                                    e.setSecondsOnFire(4);
                                    Vfx.flash(sl, e.getBoundingBox().getCenter(), a.color, 2.0, 8);
                                    RelicFx.knock(e, p, 0.5, 0.25);
                                }
                            }
                        } finally {
                            busy = false;
                        }
                    });
                }
                return true;
            }
            case PROSPECTOR -> {
                return prospect(sp, sl);
            }
            case THUNDER -> {
                HitResult hr = sp.pick(24.0, 1.0F, false);
                Vec3 target = hr.getLocation();
                LivingEntity f = RelicFx.aimedFoe(sl, sp, 24.0, 0.97);
                if (f != null) {
                    target = f.position();
                }
                strike(sl, sp, target, power);
                return true;
            }
            default -> {
                return false;
            }
        }
    }

    private static void strike(ServerLevel sl, ServerPlayer sp, Vec3 t, float power) {
        int col = Ability.THUNDER.color;
        sound(sl, t, SoundEvents.LIGHTNING_BOLT_THUNDER, 2.0F, 1.0F);
        sound(sl, t, ModSounds.RELIC_BOOM.get(), 1.0F, 1.0F);
        for (int i = 0; i < 3; i++) {
            final int k = i;
            Scheduler.runLater(i * 3, () -> {
                double ox = (sl.random.nextDouble() - 0.5) * 2.0 * (k == 0 ? 0 : 2), oz = (sl.random.nextDouble() - 0.5) * 2.0 * (k == 0 ? 0 : 2);
                Vec3 end = t.add(ox, 0, oz);
                Vfx.bolt(sl, end.add(ox * 0.5, 18, oz * 0.5), end, k == 0 ? 0xFFFFFF : col, k == 0 ? 1.6 : 1.0, 9);
                Vfx.flash(sl, end.add(0, 0.5, 0), col, 3.0, 8);
            });
        }
        Vfx.ring(sl, t.add(0, 0.1, 0), col, 5.0, 16);
        Vfx.column(sl, t, col, 0.6, 5.0, 14);
        RelicFx.burst(sl, Fx.spark(col, 0.9F), t.add(0, 0.5, 0), 40, 0.5);
        RelicFx.shakeNear(sl, t, 20, 1.0F, 10);
        busy = true;
        try {
            for (LivingEntity e : RelicFx.foes(sl, t, 4.5, sp)) {
                RelicFx.damage(sl, sp, e, 12.0F * power);
                e.setSecondsOnFire(3);
                RelicFx.knock(e, t, 0.5, 0.3);
            }
        } finally {
            busy = false;
        }
    }

    // ------------------------------------------------------------------ Pico Prospector

    private static int oreColor(BlockState s) {
        net.minecraft.resources.ResourceLocation k = ForgeRegistries.BLOCKS.getKey(s.getBlock());
        String n = k == null ? "" : k.getPath();
        if (n.contains("coal")) return 0x555566;
        if (n.contains("iron")) return 0xE8B898;
        if (n.contains("gold")) return 0xFFDC4A;
        if (n.contains("diamond")) return 0x4AEDD9;
        if (n.contains("emerald")) return 0x17DD62;
        if (n.contains("redstone")) return 0xFF2020;
        if (n.contains("lapis")) return 0x3A66E0;
        if (n.contains("copper")) return 0xE8804A;
        if (n.contains("debris")) return 0xA0643C;
        if (n.contains("quartz")) return 0xFFFFFF;
        return 0xFFE070;
    }

    private static boolean prospect(ServerPlayer sp, ServerLevel sl) {
        BlockPos c = sp.blockPosition();
        int r = 12;
        List<BlockPos> found = new ArrayList<>();
        List<Integer> cols = new ArrayList<>();
        BlockPos.MutableBlockPos mp = new BlockPos.MutableBlockPos();
        for (int dx = -r; dx <= r; dx++) {
            for (int dy = -r; dy <= r; dy++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (dx * dx + dy * dy + dz * dz > r * r) {
                        continue;
                    }
                    mp.set(c.getX() + dx, c.getY() + dy, c.getZ() + dz);
                    if (!sl.hasChunkAt(mp)) {
                        continue;
                    }
                    BlockState s = sl.getBlockState(mp);
                    if (s.is(Tags.Blocks.ORES)) {
                        found.add(mp.immutable());
                        cols.add(oreColor(s));
                    }
                }
            }
        }
        if (found.size() > 120) {
            found = found.subList(0, 120);
            cols = cols.subList(0, 120);
        }
        long[] pos = new long[found.size()];
        int[] col = new int[found.size()];
        for (int i = 0; i < pos.length; i++) {
            pos[i] = found.get(i).asLong();
            col[i] = cols.get(i);
        }
        ModNetwork.sendTo(sp, new OreRadarPacket(120, pos, col));
        Vec3 at = sp.position();
        Vfx.ring(sl, at.add(0, 0.1, 0), Ability.PROSPECTOR.color, 3.0, 14);
        sound(sl, at, ModSounds.RELIC_MAGIC.get(), 0.8F, 1.6F);
        sp.displayClientMessage(Component.translatable(pos.length == 0 ? "message.mundos_de_axelis.gear_no_ore"
                : "message.mundos_de_axelis.gear_ore_found", pos.length).withStyle(ChatFormatting.YELLOW), true);
        return true;
    }
}
