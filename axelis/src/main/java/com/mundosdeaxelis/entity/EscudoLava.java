package com.mundosdeaxelis.entity;

import com.mundosdeaxelis.MundosDeAxelis;
import com.mundosdeaxelis.fx.Vfx;
import com.mundosdeaxelis.vfx.VfxType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Escudo de lava (cultistas del volcán): una burbuja de magma que el cultista le pone a un aliado del volcán (salamandra, diablillo,
 * larva u otro cultista). Absorbe {@link #CAPACITY} de daño o dura {@link #DURATION} ticks, lo que pase antes. Cada golpe que para
 * hace brillar la burbuja; al romperse estalla en chispas. Solo existe en el servidor (no se guarda: dura unos segundos).
 */
@Mod.EventBusSubscriber(modid = MundosDeAxelis.MODID)
public final class EscudoLava {
    public static final int COLOR = 0xFF7A1E;
    /** Daño que aguanta (4 corazones). */
    public static final float CAPACITY = 8.0F;
    /** Lo que dura si nadie lo rompe (12 s). */
    public static final int DURATION = 240;

    private static final class Shield {
        final LivingEntity e;
        float hp = CAPACITY;
        int left = DURATION;
        int dome;
        int rune;

        Shield(LivingEntity e) {
            this.e = e;
        }
    }

    private static final Map<LivingEntity, Shield> SHIELDS = new WeakHashMap<>();

    private EscudoLava() {
    }

    /** ¿Es de los que el cultista protege? */
    public static boolean isAlly(Entity e) {
        return e instanceof SalamandraLava || e instanceof Diablillo || e instanceof LarvaMagma || e instanceof CultistaVolcan;
    }

    public static boolean has(LivingEntity e) {
        Shield s = SHIELDS.get(e);
        return s != null && s.hp > 0 && s.left > 0;
    }

    /** Daño que le queda por aguantar (0 si no tiene escudo). */
    public static float left(LivingEntity e) {
        Shield s = SHIELDS.get(e);
        return s == null ? 0F : Math.max(0F, s.hp);
    }

    private static double radius(LivingEntity e) {
        return Math.max(e.getBbWidth(), e.getBbHeight()) * 0.62 + 0.35;
    }

    /** Pone (o renueva) el escudo. */
    public static void give(ServerLevel sl, LivingEntity target) {
        Shield old = SHIELDS.remove(target);
        if (old != null) {
            Vfx.remove(sl, old.dome);
            Vfx.remove(sl, old.rune);
        }
        Shield s = new Shield(target);
        s.dome = Vfx.domeFollow(sl, target, COLOR, radius(target), DURATION + 10);
        s.rune = Vfx.runeFollow(sl, target, COLOR, radius(target) * 0.9, DURATION + 10);
        SHIELDS.put(target, s);
        Vec3 c = target.position().add(0, target.getBbHeight() * 0.5, 0);
        sl.playSound(null, c.x, c.y, c.z, SoundEvents.RESPAWN_ANCHOR_CHARGE, SoundSource.HOSTILE, 1.0F, 0.75F);
        sl.playSound(null, c.x, c.y, c.z, SoundEvents.LAVA_POP, SoundSource.HOSTILE, 1.2F, 0.6F);
        sl.sendParticles(ParticleTypes.LAVA, c.x, c.y, c.z, 8, radius(target) * 0.5, 0.3, radius(target) * 0.5, 0.0);
        Vfx.ring(sl, target.position().add(0, 0.08, 0), COLOR, radius(target) + 0.8, 14);
    }

    private static void drop(ServerLevel sl, Shield s, boolean broke) {
        Vfx.remove(sl, s.dome);
        Vfx.remove(sl, s.rune);
        LivingEntity e = s.e;
        Vec3 c = e.position().add(0, e.getBbHeight() * 0.5, 0);
        double r = radius(e);
        if (broke) {
            // estalla: chispas, gotas de lava y un destello
            sl.playSound(null, c.x, c.y, c.z, SoundEvents.GLASS_BREAK, SoundSource.HOSTILE, 1.1F, 0.55F);
            sl.playSound(null, c.x, c.y, c.z, SoundEvents.LAVA_EXTINGUISH, SoundSource.HOSTILE, 0.9F, 0.8F);
            sl.sendParticles(ParticleTypes.LAVA, c.x, c.y, c.z, 14, r * 0.6, r * 0.5, r * 0.6, 0.0);
            sl.sendParticles(ParticleTypes.FLAME, c.x, c.y, c.z, 30, r * 0.5, r * 0.5, r * 0.5, 0.08);
            sl.sendParticles(ParticleTypes.LARGE_SMOKE, c.x, c.y, c.z, 10, r * 0.4, r * 0.4, r * 0.4, 0.02);
            Vfx.flash(sl, c, COLOR, r * 2.2, 8);
        } else if (e.isAlive()) {
            // se apaga solo
            sl.playSound(null, c.x, c.y, c.z, SoundEvents.FIRE_EXTINGUISH, SoundSource.HOSTILE, 0.7F, 0.7F);
            sl.sendParticles(ParticleTypes.SMOKE, c.x, c.y, c.z, 14, r * 0.5, r * 0.5, r * 0.5, 0.02);
        }
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || SHIELDS.isEmpty()) {
            return;
        }
        List<Shield> gone = new ArrayList<>();
        Iterator<Shield> it = SHIELDS.values().iterator();
        while (it.hasNext()) {
            Shield s = it.next();
            LivingEntity e = s.e;
            if (e.isRemoved() || !e.isAlive() || --s.left <= 0) {
                it.remove();
                gone.add(s);
                continue;
            }
            if (e.level() instanceof ServerLevel sl && e.tickCount % 4 == 0) {
                // gotas de magma que resbalan por la burbuja
                double r = radius(e);
                double a = e.tickCount * 0.6;
                Vec3 c = e.position().add(0, e.getBbHeight() * 0.5, 0);
                sl.sendParticles(ParticleTypes.DRIPPING_LAVA, c.x + Math.cos(a) * r * 0.9, c.y + r * 0.35, c.z + Math.sin(a) * r * 0.9, 1,
                        0.0, 0.0, 0.0, 0.0);
                if (e.tickCount % 12 == 0) {
                    sl.sendParticles(ParticleTypes.SMALL_FLAME, c.x, c.y, c.z, 3, r * 0.6, r * 0.5, r * 0.6, 0.01);
                }
            }
        }
        for (Shield s : gone) {
            if (s.e.level() instanceof ServerLevel sl) {
                drop(sl, s, false);
            }
        }
    }

    /** El escudo se come el daño antes que la vida (salvo /kill y el vacío). */
    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onHurt(LivingHurtEvent event) {
        LivingEntity e = event.getEntity();
        if (SHIELDS.isEmpty() || !(e.level() instanceof ServerLevel sl)) {
            return;
        }
        Shield s = SHIELDS.get(e);
        if (s == null || s.hp <= 0 || event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return;
        }
        float amount = event.getAmount();
        float soak = Math.min(amount, s.hp);
        s.hp -= soak;
        event.setAmount(amount - soak);
        Vec3 c = e.position().add(0, e.getBbHeight() * 0.5, 0);
        if (s.hp <= 0.01F) {
            SHIELDS.remove(e);
            drop(sl, s, true);
            return;
        }
        // golpe parado: la burbuja brilla y salta una chispa hacia quien pegó
        Vfx.make(sl, VfxType.DOME).follow(e, 0, e.getBbHeight() * 0.5, 0).rgb(COLOR).size(radius(e)).a(1.0).life(0).update(s.dome);
        sl.playSound(null, c.x, c.y, c.z, SoundEvents.SHIELD_BLOCK, SoundSource.HOSTILE, 0.9F, 0.6F);
        sl.playSound(null, c.x, c.y, c.z, SoundEvents.LAVA_POP, SoundSource.HOSTILE, 1.0F, 0.9F);
        Entity by = event.getSource().getEntity();
        Vec3 hit = by != null ? c.add(by.position().add(0, by.getBbHeight() * 0.5, 0).subtract(c).normalize().scale(radius(e))) : c;
        sl.sendParticles(ParticleTypes.LAVA, hit.x, hit.y, hit.z, 4, 0.15, 0.15, 0.15, 0.0);
        sl.sendParticles(ParticleTypes.FLAME, hit.x, hit.y, hit.z, 8, 0.15, 0.15, 0.15, 0.05);
    }
}
