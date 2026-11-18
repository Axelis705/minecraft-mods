package com.mundosdeaxelis.relic;

import com.mundosdeaxelis.MundosDeAxelis;
import com.mundosdeaxelis.fx.Fx;
import com.mundosdeaxelis.fx.Vfx;
import com.mundosdeaxelis.vfx.VfxTex;
import com.mundosdeaxelis.vfx.VfxType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

/**
 * Compañeros mágicos (dragoncito y hadas). No son entidades de verdad: el servidor simula su posición,
 * decide a quién atacar o curar y manda un sprite al cliente (ver {@link Vfx}).
 */
@Mod.EventBusSubscriber(modid = MundosDeAxelis.MODID)
public final class Familiars {
    private Familiars() {
    }

    private enum Kind {DRAGON, FAIRY}

    private static final class F {
        Kind kind;
        UUID owner;
        ServerLevel level;
        Vec3 pos;
        Vec3 vel = Vec3.ZERO;
        int age;
        int life;
        int sprite;
        int slot;
        int cooldown;
        float power;
        LivingEntity task;
        int rgb;
        String relic;
    }

    private static final List<F> LIST = new ArrayList<>();
    private static final double FAIRY_SIZE = 0.85;
    /** Especie 10 del modelo de criaturas 3D = hada. */
    private static final int FAIRY_SPECIES = 10;
    /** Dibujo del dragoncito: DRAGON3D = modelo en 3D; DRAGON = el dibujo plano de antes. */
    private static final int DRAGON_TEX = VfxTex.DRAGON3D;

    @SubscribeEvent
    public static void onStopped(ServerStoppedEvent event) {
        LIST.clear();
    }

    /** Un dragoncito por jugador (si ya había uno, se va). */
    public static void spawnDragon(ServerPlayer sp, int ticks, float power) {
        removeAll(sp, Kind.DRAGON);
        ServerLevel sl = sp.serverLevel();
        F f = new F();
        f.kind = Kind.DRAGON;
        f.owner = sp.getUUID();
        f.level = sl;
        f.life = ticks;
        f.power = power;
        f.rgb = 0xFFFFFF;
        f.relic = "hatchling_egg";
        Vec3 look = sp.getLookAngle();
        f.pos = sp.getEyePosition().add(look.x * 1.6, -0.2, look.z * 1.6);
        f.cooldown = 30;
        f.sprite = Vfx.sprite(sl, f.pos, DRAGON_TEX, 0.1, look, 0xFFFFFF, false, ticks + 10);
        LIST.add(f);
    }

    public static void spawnFairies(ServerPlayer sp, int ticks, float power) {
        removeAll(sp, Kind.FAIRY);
        ServerLevel sl = sp.serverLevel();
        for (int i = 0; i < 3; i++) {
            F f = new F();
            f.kind = Kind.FAIRY;
            f.owner = sp.getUUID();
            f.level = sl;
            f.life = ticks;
            f.power = power;
            f.slot = i;
            f.rgb = i == 0 ? 0xFFB6EE : (i == 1 ? 0xB8E8FF : 0xFFF0A8);
            f.relic = "fairy_jar";
            f.pos = sp.position().add(0, 1.2, 0);
            f.cooldown = 20 + i * 14;
            f.sprite = Vfx.make(sl, VfxType.SPRITE).at(f.pos).a(VfxTex.CREATURE3D).b(FAIRY_SPECIES).size(FAIRY_SIZE).rgb(f.rgb).dir(sp.getLookAngle()).life(ticks + 10).send();
            LIST.add(f);
        }
    }

    private static void removeAll(ServerPlayer sp, Kind kind) {
        Iterator<F> it = LIST.iterator();
        while (it.hasNext()) {
            F f = it.next();
            if (f.owner.equals(sp.getUUID()) && f.kind == kind) {
                Vfx.remove(f.level, f.sprite);
                it.remove();
            }
        }
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || LIST.isEmpty()) {
            return;
        }
        Iterator<F> it = LIST.iterator();
        while (it.hasNext()) {
            F f = it.next();
            ServerPlayer owner = f.level.getServer().getPlayerList().getPlayer(f.owner);
            boolean gone = owner == null || !owner.isAlive() || owner.serverLevel() != f.level;
            f.age++;
            if (gone || f.age > f.life) {
                finish(f, gone);
                it.remove();
                continue;
            }
            try {
                if (f.kind == Kind.DRAGON) {
                    tickDragon(f, owner);
                } else {
                    tickFairy(f, owner);
                }
            } catch (Exception e) {
                MundosDeAxelis.LOGGER.error("Error en un compañero mágico", e);
                finish(f, true);
                it.remove();
            }
        }
    }

    private static void finish(F f, boolean silent) {
        Vfx.remove(f.level, f.sprite);
        if (!silent) {
            RelicSounds.play(f.level, f.pos, f.relic, "end", 0.9F, f.kind == Kind.FAIRY ? 1.0F + f.slot * 0.1F : 1.0F);
            Vfx.flash(f.level, f.pos, f.kind == Kind.DRAGON ? 0xFF8A2E : f.rgb, f.kind == Kind.DRAGON ? 2.2 : 1.1, 12);
            RelicFx.burst(f.level, Fx.spark(f.kind == Kind.DRAGON ? 0xFFAA44 : f.rgb, 0.7F), f.pos, 14, 0.3);
        }
    }

    private static void push(F f, Vec3 to, double maxStep, double smooth) {
        Vec3 d = to.subtract(f.pos);
        double len = d.length();
        Vec3 step = d.scale(smooth);
        if (step.length() > maxStep) {
            step = step.normalize().scale(maxStep);
        }
        f.vel = f.vel.scale(0.5).add(step.scale(0.5));
        f.pos = f.pos.add(f.vel);
        if (len < 0.001) {
            f.vel = Vec3.ZERO;
        }
    }

    // ------------------------------------------------------------------ dragoncito

    private static void tickDragon(F f, ServerPlayer owner) {
        ServerLevel sl = f.level;
        double grow = Math.min(1.0, f.age / 24.0);
        double size = 0.25 + 1.25 * Math.min(1.0, grow * grow * (3 - 2 * grow));
        Vec3 look = owner.getLookAngle();
        Vec3 right = look.cross(new Vec3(0, 1, 0));
        right = right.lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : right.normalize();
        // objetivo: el enemigo más cercano a ti
        LivingEntity foe = null;
        if (f.age > 30) {
            List<LivingEntity> foes = RelicFx.foes(sl, owner.position(), 14, owner);
            for (LivingEntity e : foes) {
                if (owner.hasLineOfSight(e) || f.pos.distanceTo(e.position()) < 8) {
                    foe = e;
                    break;
                }
            }
        }
        Vec3 desired;
        Vec3 faceDir;
        if (foe != null) {
            Vec3 toOwner = owner.position().subtract(foe.position());
            toOwner = new Vec3(toOwner.x, 0, toOwner.z);
            toOwner = toOwner.lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : toOwner.normalize();
            desired = foe.position().add(toOwner.scale(4.2)).add(0, 2.4 + Math.sin(f.age * 0.15) * 0.25, 0);
            faceDir = foe.position().subtract(f.pos);
        } else {
            double ang = f.age * 0.03;
            desired = owner.position().add(right.scale(1.1 + Math.cos(ang) * 0.5)).add(look.scale(-0.4 + Math.sin(ang) * 0.5))
                    .add(0, 2.2 + Math.sin(f.age * 0.18) * 0.25, 0);
            faceDir = f.vel.x * f.vel.x + f.vel.z * f.vel.z > 0.003 ? f.vel : look;
        }
        int left = f.life - f.age;
        if (left < 16) {
            desired = f.pos.add(0, 0.7, 0);
        }
        if (f.age <= 24) {
            desired = owner.getEyePosition().add(look.x * 1.5, -0.1 + grow * 0.7, look.z * 1.5);
        }
        push(f, desired, 0.7, 0.14);
        // aliento de fuego
        f.cooldown--;
        if (foe != null && f.cooldown <= 0 && f.pos.distanceTo(foe.getEyePosition()) < 12) {
            f.cooldown = 26;
            Vec3 flat = new Vec3(faceDir.x, 0, faceDir.z);
            flat = flat.lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : flat.normalize();
            Vec3 mouth = f.pos.add(flat.scale(size * 0.58)).add(0, size * 0.46, 0);
            Vec3 to = foe.getBoundingBox().getCenter();
            Vfx.make(sl, VfxType.BEAM).at(mouth).dir(to.subtract(mouth)).rgb(0xFF7A1A).size(0.22).life(9)
                    .flags(VfxType.F_FLICKER | VfxType.F_CAPS).send();
            Vfx.make(sl, VfxType.BEAM).at(mouth).dir(to.subtract(mouth)).rgb(0xFFD070).size(0.1).life(9).flags(VfxType.F_FLICKER).send();
            RelicFx.line(sl, ParticleTypes.FLAME, mouth, to, 0.5);
            RelicFx.burst(sl, ParticleTypes.FLAME, to, 10, 0.12);
            Vfx.flash(sl, to, 0xFF8A2E, 1.4, 8);
            RelicSounds.play(sl, mouth, "hatchling_egg", "breath", 0.85F, 1.0F);
            float dmg = 4.5F * f.power;
            RelicFx.damage(sl, owner, foe, dmg);
            foe.setSecondsOnFire(4);
            for (LivingEntity e : RelicFx.foes(sl, to, 2.2, owner)) {
                if (e != foe) {
                    RelicFx.damage(sl, owner, e, dmg * 0.5F);
                    e.setSecondsOnFire(3);
                }
            }
        }
        if (f.age % 3 == 0) {
            sl.sendParticles(ParticleTypes.SMALL_FLAME, f.pos.x, f.pos.y + 0.1, f.pos.z, 1, 0.15, 0.1, 0.15, 0.0);
        }
        Vec3 fd = new Vec3(faceDir.x, 0, faceDir.z);
        Vfx.make(sl, VfxType.SPRITE).at(f.pos).a(DRAGON_TEX).size(size).rgb(0xFFFFFF).dir(fd).life(0).update(f.sprite);
    }

    // ------------------------------------------------------------------ hadas

    private static void tickFairy(F f, ServerPlayer owner) {
        ServerLevel sl = f.level;
        double phase = f.age * 0.07 + f.slot * Math.PI * 2 / 3;
        Vec3 orbit = owner.position().add(Math.cos(phase) * 1.7, 1.5 + Math.sin(f.age * 0.11 + f.slot * 2) * 0.5, Math.sin(phase) * 1.7);
        Vec3 desired = orbit;
        f.cooldown--;
        if (f.task != null && (!f.task.isAlive() || f.task.getHealth() >= f.task.getMaxHealth() - 0.5F)) {
            f.task = null;
        }
        if (f.task == null && f.cooldown <= 0) {
            // ¿alguien herido?
            LivingEntity hurt = null;
            double worst = 0.8;
            for (LivingEntity a : RelicFx.allies(sl, owner.position(), 14, owner, true)) {
                double ratio = a.getHealth() / a.getMaxHealth();
                if (ratio < worst) {
                    worst = ratio;
                    hurt = a;
                }
            }
            if (hurt != null) {
                f.task = hurt;
            } else {
                List<LivingEntity> foes = RelicFx.foes(sl, owner.position(), 14, owner);
                if (!foes.isEmpty()) {
                    LivingEntity foe = foes.get(Math.min(f.slot, foes.size() - 1));
                    f.cooldown = 38 + f.slot * 4;
                    Vec3 to = foe.getBoundingBox().getCenter();
                    Vfx.bolt(sl, f.pos, to, f.rgb, 0.035, 6);
                    Vfx.flash(sl, to, f.rgb, 0.8, 7);
                    RelicSounds.play(sl, f.pos, "fairy_jar", "bolt", 0.7F, 0.95F + f.slot * 0.12F);
                    RelicFx.damage(sl, owner, foe, 3.2F * f.power);
                    RelicFx.burst(sl, Fx.spark(f.rgb, 0.5F), to, 6, 0.2);
                }
            }
        }
        if (f.task != null) {
            desired = f.task.position().add(0, f.task.getBbHeight() + 0.5, 0);
            if (f.pos.distanceTo(desired) < 1.1) {
                f.task.heal(3.0F * f.power);
                f.cooldown = 50;
                RelicSounds.play(sl, f.pos, "fairy_jar", "heal", 0.8F, 1.0F + f.slot * 0.08F);
                sl.sendParticles(Fx.heart(0xFF8FC8, 0.7F), f.task.getX(), f.task.getY() + f.task.getBbHeight() * 0.8, f.task.getZ(), 5, 0.4, 0.4, 0.4, 0.05);
                Vfx.ring(sl, f.task.position().add(0, 0.1, 0), 0xFF9BE8, 1.4, 14);
                f.task = null;
            }
        }
        push(f, desired, 0.55, 0.2);
        sl.sendParticles(Fx.spark(f.rgb, 0.28F), f.pos.x, f.pos.y, f.pos.z, 1, 0.08, 0.08, 0.08, 0.0);
        Vec3 fd = f.vel;
        Vfx.make(sl, VfxType.SPRITE).at(f.pos).a(VfxTex.CREATURE3D).b(FAIRY_SPECIES).size(FAIRY_SIZE).rgb(f.rgb).dir(owner.position().subtract(f.pos)).life(0).update(f.sprite);
    }
}
