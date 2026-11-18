package com.mundosdeaxelis.relic;

import com.mundosdeaxelis.fx.Fx;
import com.mundosdeaxelis.fx.Vfx;
import com.mundosdeaxelis.vfx.VfxType;
import com.mundosdeaxelis.world.Scheduler;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/** Proyectiles "virtuales" (sin entidad): el servidor los mueve, detecta golpes y manda un orbe con estela al cliente. */
public final class Missiles {
    private Missiles() {
    }

    public interface OnHit {
        /** target es null cuando choca con un bloque o se acaba su vida. */
        void hit(Vec3 pos, LivingEntity target);
    }

    public static final class Spec {
        public ServerPlayer owner;
        public Vec3 pos = Vec3.ZERO;
        public Vec3 dir = new Vec3(0, 1, 0);
        public double speed = 1.0;
        /** Cuánto gira hacia el objetivo cada tick (0 a 1). */
        public double turn = 0.0;
        public int life = 40;
        public int rgb = 0xFFFFFF;
        public double size = 0.3;
        public LivingEntity homing;
        public boolean pierce;
        public double hitRadius = 0.35;
        public OnHit onHit;
        /** Cada tick, antes de moverse (para zigzag u otros efectos). */
        public java.util.function.Consumer<Spec> onTick;
        public boolean trail = true;
        public net.minecraft.core.particles.ParticleOptions particles;

        public Spec(ServerPlayer owner) {
            this.owner = owner;
        }
    }

    public static void launch(Spec m) {
        ServerLevel sl = m.owner.serverLevel();
        m.dir = m.dir.normalize();
        int flags = m.trail ? VfxType.F_TRAIL : 0;
        int id = Vfx.make(sl, VfxType.ORB).at(m.pos).rgb(m.rgb).size(m.size).life(m.life + 6).flags(flags).send();
        Set<LivingEntity> already = new HashSet<>();
        step(sl, m, id, 0, flags, already);
    }

    private static void step(ServerLevel sl, Spec m, int id, int t, int flags, Set<LivingEntity> already) {
        if (!m.owner.isAlive() || m.owner.isRemoved() || t > m.life) {
            finish(sl, m, id, m.pos, null);
            return;
        }
        if (m.onTick != null) {
            m.onTick.accept(m);
        }
        if (m.homing != null && m.homing.isAlive() && m.turn > 0) {
            Vec3 want = m.homing.getBoundingBox().getCenter().subtract(m.pos).normalize();
            m.dir = m.dir.scale(1.0 - m.turn).add(want.scale(m.turn)).normalize();
        }
        Vec3 next = m.pos.add(m.dir.scale(m.speed));
        BlockHitResult bh = sl.clip(new ClipContext(m.pos, next, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, m.owner));
        Vec3 end = bh.getType() == HitResult.Type.MISS ? next : bh.getLocation();
        LivingEntity hit = null;
        Vec3 hitPos = null;
        double best = Double.MAX_VALUE;
        for (LivingEntity e : RelicFx.foes(sl, m.pos.add(end).scale(0.5), m.pos.distanceTo(end) * 0.5 + 3.0, m.owner)) {
            if (already.contains(e)) {
                continue;
            }
            Optional<Vec3> c = e.getBoundingBox().inflate(m.hitRadius).clip(m.pos, end);
            if (c.isPresent()) {
                double d = c.get().distanceToSqr(m.pos);
                if (d < best) {
                    best = d;
                    hit = e;
                    hitPos = c.get();
                }
            }
        }
        if (m.particles != null) {
            sl.sendParticles(m.particles, end.x, end.y, end.z, 1, 0.05, 0.05, 0.05, 0.0);
        }
        if (hit != null) {
            m.pos = hitPos;
            Vfx.make(sl, VfxType.ORB).at(m.pos).rgb(m.rgb).size(m.size).flags(flags).life(0).update(id);
            m.onHit.hit(hitPos, hit);
            if (!m.pierce) {
                Vfx.remove(sl, id);
                return;
            }
            already.add(hit);
        } else if (bh.getType() != HitResult.Type.MISS) {
            finish(sl, m, id, end, null);
            return;
        }
        m.pos = end;
        Vfx.make(sl, VfxType.ORB).at(m.pos).rgb(m.rgb).size(m.size).flags(flags).life(0).update(id);
        Scheduler.runLater(1, () -> step(sl, m, id, t + 1, flags, already));
    }

    private static void finish(ServerLevel sl, Spec m, int id, Vec3 at, LivingEntity target) {
        Vfx.remove(sl, id);
        if (m.onHit != null) {
            m.onHit.hit(at, target);
        }
    }

    /** Pequeño estallido de chispas del color dado. */
    public static void spark(ServerLevel sl, Vec3 at, int rgb, int n) {
        RelicFx.burst(sl, Fx.spark(rgb, 0.7F), at, n, 0.28);
    }
}
