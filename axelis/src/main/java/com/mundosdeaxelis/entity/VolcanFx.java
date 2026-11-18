package com.mundosdeaxelis.entity;

import com.mundosdeaxelis.fx.Vfx;
import com.mundosdeaxelis.vfx.VfxType;
import com.mundosdeaxelis.world.Scheduler;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

/**
 * Ataques compartidos de los enemigos del volcán (etapa 4): bolas de lava en arco con aviso en el suelo, bolas de fuego rectas y
 * columnas de fuego con aviso. Sin entidades propias: el servidor mueve un orbe de {@link Vfx} y aplica el daño al llegar.
 */
public final class VolcanFx {
    public static final int LAVA = 0xFF6A1A;
    public static final int HOT = 0xFFC040;
    public static final int WARN = 0xFF3A2A;

    private VolcanFx() {
    }

    /** Suelo bajo un punto (hasta 6 bloques hacia abajo), para poner el aviso donde va a caer. */
    public static Vec3 ground(ServerLevel sl, Vec3 at) {
        BlockHitResult h = sl.clip(new ClipContext(at.add(0, 1.0, 0), at.add(0, -6.0, 0), ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY,
                null));
        return h.getType() == HitResult.Type.MISS ? at : h.getLocation();
    }

    /**
     * Bola de lava en arco: avisa con un círculo rojo donde caerá y, al llegar, quema a quien siga dentro (radio {@code r}).
     */
    public static void lavaGlob(ServerLevel sl, LivingEntity owner, Vec3 from, Vec3 target, int ticks, double arc, float dmg, double r) {
        Vec3 land = ground(sl, target);
        Vfx.ring(sl, land.add(0, 0.08, 0), WARN, r, ticks + 4);
        int id = Vfx.make(sl, VfxType.ORB).at(from).rgb(LAVA).size(0.35).life(ticks + 6).flags(VfxType.F_TRAIL).send();
        for (int t = 1; t <= ticks; t++) {
            final double u = t / (double) ticks;
            final boolean last = t == ticks;
            Scheduler.runLater(t, () -> {
                if (owner.isRemoved() || !owner.isAlive()) {
                    Vfx.remove(sl, id);
                    return;
                }
                Vec3 p = from.lerp(land, u).add(0, arc * 4 * u * (1 - u), 0);
                Vfx.make(sl, VfxType.ORB).at(p).rgb(LAVA).size(0.35).flags(VfxType.F_TRAIL).life(0).update(id);
                if (last) {
                    Vfx.remove(sl, id);
                    splash(sl, owner, land, dmg, r);
                }
            });
        }
    }

    private static void splash(ServerLevel sl, LivingEntity owner, Vec3 at, float dmg, double r) {
        sl.sendParticles(ParticleTypes.LAVA, at.x, at.y + 0.2, at.z, 10, r * 0.4, 0.1, r * 0.4, 0.0);
        sl.sendParticles(ParticleTypes.FLAME, at.x, at.y + 0.2, at.z, 16, r * 0.4, 0.2, r * 0.4, 0.03);
        sl.playSound(null, at.x, at.y, at.z, SoundEvents.LAVA_POP, SoundSource.HOSTILE, 1.4F, 0.7F);
        sl.playSound(null, at.x, at.y, at.z, SoundEvents.FIRECHARGE_USE, SoundSource.HOSTILE, 0.8F, 0.8F);
        for (ServerPlayer p : sl.getEntitiesOfClass(ServerPlayer.class, new AABB(at, at).inflate(r, 1.5, r))) {
            if (p.isCreative() || p.isSpectator() || p.position().distanceTo(at) > r + 0.4) {
                continue;
            }
            p.hurt(owner.damageSources().mobAttack(owner), dmg);
            p.setSecondsOnFire(4);
        }
    }

    /** Bola de fuego casi recta que gira un poco hacia el objetivo. */
    public static void fireball(ServerLevel sl, LivingEntity owner, LivingEntity target, Vec3 from, float dmg, double speed, double size) {
        Vec3 dir = target.getBoundingBox().getCenter().subtract(from).normalize();
        int id = Vfx.make(sl, VfxType.ORB).at(from).rgb(HOT).size(size).life(60).flags(VfxType.F_TRAIL).send();
        sl.playSound(null, from.x, from.y, from.z, SoundEvents.FIRECHARGE_USE, SoundSource.HOSTILE, 0.9F, 1.3F);
        step(sl, owner, target, from, dir, id, 0, dmg, speed, size);
    }

    private static void step(ServerLevel sl, LivingEntity owner, LivingEntity target, Vec3 pos, Vec3 dir, int id, int age, float dmg, double speed,
                             double size) {
        if (age > 50 || owner.isRemoved() || !owner.isAlive()) {
            pop(sl, id, pos);
            return;
        }
        if (target.isAlive()) {
            Vec3 want = target.getBoundingBox().getCenter().subtract(pos).normalize();
            dir = dir.scale(0.95).add(want.scale(0.05)).normalize();
        }
        Vec3 next = pos.add(dir.scale(speed));
        BlockHitResult bh = sl.clip(new ClipContext(pos, next, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, owner));
        Vec3 end = bh.getType() == HitResult.Type.MISS ? next : bh.getLocation();
        for (ServerPlayer p : sl.getEntitiesOfClass(ServerPlayer.class, new AABB(pos, end).inflate(1.0))) {
            if (p.isSpectator() || p.isCreative()) {
                continue;
            }
            Optional<Vec3> c = p.getBoundingBox().inflate(0.25).clip(pos, end);
            if (c.isPresent()) {
                p.hurt(owner.damageSources().mobAttack(owner), dmg);
                p.setSecondsOnFire(2);
                pop(sl, id, c.get());
                return;
            }
        }
        if (bh.getType() != HitResult.Type.MISS) {
            pop(sl, id, end);
            return;
        }
        if (age % 2 == 0) {
            sl.sendParticles(ParticleTypes.SMALL_FLAME, end.x, end.y, end.z, 1, 0.02, 0.02, 0.02, 0.0);
        }
        Vfx.make(sl, VfxType.ORB).at(end).rgb(HOT).size(size).flags(VfxType.F_TRAIL).life(0).update(id);
        final Vec3 d = dir;
        Scheduler.runLater(1, () -> step(sl, owner, target, end, d, id, age + 1, dmg, speed, size));
    }

    private static void pop(ServerLevel sl, int id, Vec3 at) {
        Vfx.remove(sl, id);
        sl.sendParticles(ParticleTypes.FLAME, at.x, at.y, at.z, 12, 0.15, 0.15, 0.15, 0.06);
        sl.sendParticles(ParticleTypes.SMOKE, at.x, at.y, at.z, 4, 0.1, 0.1, 0.1, 0.02);
    }

    /**
     * Columna de fuego: aviso (círculo y runa) durante {@code warn} ticks y luego una llamarada que quema y levanta a quien siga
     * dentro (radio {@code r}).
     */
    public static void pillar(ServerLevel sl, LivingEntity owner, Vec3 at, int warn, float dmg, double r) {
        Vec3 g = ground(sl, at);
        Vfx.ring(sl, g.add(0, 0.08, 0), WARN, r, warn);
        Vfx.rune(sl, g.add(0, 0.06, 0), LAVA, r * 0.85, warn);
        sl.playSound(null, g.x, g.y, g.z, SoundEvents.LAVA_AMBIENT, SoundSource.HOSTILE, 1.5F, 0.8F);
        for (int k = 0; k < warn; k += 6) {
            Scheduler.runLater(k, () -> {
                if (owner.isAlive()) {
                    sl.sendParticles(ParticleTypes.SMOKE, g.x, g.y + 0.1, g.z, 4, r * 0.4, 0.05, r * 0.4, 0.01);
                    sl.sendParticles(ParticleTypes.LAVA, g.x, g.y + 0.1, g.z, 1, r * 0.3, 0.0, r * 0.3, 0.0);
                }
            });
        }
        Scheduler.runLater(warn, () -> {
            if (!owner.isAlive()) {
                return;
            }
            Vfx.column(sl, g, LAVA, r * 0.9, 5.0, 18);
            sl.sendParticles(ParticleTypes.FLAME, g.x, g.y + 1.0, g.z, 40, r * 0.35, 1.2, r * 0.35, 0.05);
            sl.sendParticles(ParticleTypes.LAVA, g.x, g.y + 0.5, g.z, 10, r * 0.3, 0.4, r * 0.3, 0.0);
            sl.playSound(null, g.x, g.y, g.z, SoundEvents.BLAZE_SHOOT, SoundSource.HOSTILE, 1.6F, 0.6F);
            sl.playSound(null, g.x, g.y, g.z, SoundEvents.FIRECHARGE_USE, SoundSource.HOSTILE, 1.4F, 0.5F);
            for (ServerPlayer p : sl.getEntitiesOfClass(ServerPlayer.class, new AABB(g, g).inflate(r, 3.0, r))) {
                if (p.isCreative() || p.isSpectator() || Math.hypot(p.getX() - g.x, p.getZ() - g.z) > r + 0.3) {
                    continue;
                }
                p.hurt(owner.damageSources().indirectMagic(owner, owner), dmg);
                p.setSecondsOnFire(4);
                p.setDeltaMovement(p.getDeltaMovement().add(0, 0.55, 0));
                p.hurtMarked = true;
            }
        });
    }
}
