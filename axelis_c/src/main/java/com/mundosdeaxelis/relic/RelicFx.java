package com.mundosdeaxelis.relic;

import com.mundosdeaxelis.fx.Fx;
import com.mundosdeaxelis.network.ScreenFxPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Herramientas comunes para los efectos de las reliquias (geometría de partículas, objetivos, daño). */
public final class RelicFx {
    private RelicFx() {
    }

    public static Vec3 look(ServerPlayer sp) {
        return sp.getLookAngle();
    }

    /** Posición aproximada de la mano derecha (para que las chispas salgan de la reliquia). */
    public static Vec3 hand(ServerPlayer sp) {
        Vec3 look = sp.getLookAngle();
        Vec3 right = look.cross(new Vec3(0, 1, 0));
        if (right.lengthSqr() < 1.0E-4) {
            right = new Vec3(1, 0, 0);
        }
        right = right.normalize();
        return sp.getEyePosition().add(look.scale(0.75)).add(right.scale(0.32)).add(0, -0.28, 0);
    }

    public static boolean isFoe(LivingEntity e, ServerPlayer sp) {
        return e != sp && e.isAlive() && e instanceof Enemy;
    }

    public static List<LivingEntity> foes(ServerLevel sl, Vec3 c, double r, ServerPlayer sp) {
        List<LivingEntity> list = new ArrayList<>(sl.getEntitiesOfClass(LivingEntity.class,
                new AABB(c.x - r, c.y - r, c.z - r, c.x + r, c.y + r, c.z + r), e -> isFoe(e, sp) && e.position().distanceTo(c) <= r));
        list.sort(Comparator.comparingDouble(e -> e.position().distanceToSqr(c)));
        return list;
    }

    /** Enemigo más cercano a la línea de mira, dentro de un cono. */
    public static LivingEntity aimedFoe(ServerLevel sl, ServerPlayer sp, double range, double minDot) {
        Vec3 eye = sp.getEyePosition();
        Vec3 look = sp.getLookAngle();
        LivingEntity best = null;
        double bestDot = minDot;
        for (LivingEntity e : foes(sl, eye, range, sp)) {
            Vec3 to = e.getBoundingBox().getCenter().subtract(eye);
            double d = to.length();
            if (d < 0.5) {
                continue;
            }
            double dot = to.scale(1.0 / d).dot(look);
            if (dot > bestDot && sp.hasLineOfSight(e)) {
                bestDot = dot;
                best = e;
            }
        }
        return best;
    }

    /** Hasta dónde llega la mirada (bloque que toca o el final del alcance). */
    public static Vec3 target(ServerPlayer sp, double range) {
        Vec3 from = sp.getEyePosition();
        Vec3 to = from.add(sp.getLookAngle().scale(range));
        BlockHitResult hit = sp.level().clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, sp));
        return hit.getType() == HitResult.Type.MISS ? to : hit.getLocation();
    }

    public static void hurt(ServerLevel sl, ServerPlayer sp, LivingEntity e, float dmg) {
        e.hurt(sl.damageSources().playerAttack(sp), dmg);
    }

    public static void shake(ServerPlayer sp, float degrees, int ticks) {
        Fx.screen(sp, ScreenFxPacket.SHAKE, Math.round(degrees * 10.0F), ticks);
    }

    public static void shakeNear(ServerLevel sl, Vec3 c, double r, float degrees, int ticks) {
        for (ServerPlayer p : sl.players()) {
            if (p.position().distanceToSqr(c) <= r * r) {
                shake(p, degrees, ticks);
            }
        }
    }

    // ---- Geometría de partículas ----

    public static void line(ServerLevel sl, ParticleOptions p, Vec3 a, Vec3 b, double step) {
        Vec3 d = b.subtract(a);
        double len = d.length();
        if (len < 0.01) {
            return;
        }
        Vec3 dir = d.scale(1.0 / len);
        for (double s = 0; s <= len; s += step) {
            Vec3 q = a.add(dir.scale(s));
            sl.sendParticles(p, q.x, q.y, q.z, 1, 0, 0, 0, 0);
        }
    }

    /** Relámpago con zigzag entre dos puntos. */
    public static void bolt(ServerLevel sl, Vec3 a, Vec3 b, int rgb) {
        Vec3 d = b.subtract(a);
        double len = d.length();
        if (len < 0.1) {
            return;
        }
        int segs = Math.max(3, (int) (len / 1.2));
        Vec3 prev = a;
        for (int i = 1; i <= segs; i++) {
            Vec3 q = a.add(d.scale((double) i / segs));
            if (i < segs) {
                q = q.add((sl.random.nextDouble() - 0.5) * 0.9, (sl.random.nextDouble() - 0.5) * 0.9, (sl.random.nextDouble() - 0.5) * 0.9);
            }
            line(sl, Fx.dust(rgb, 0.7F), prev, q, 0.28);
            line(sl, Fx.dust(0xFFFFFF, 0.35F), prev, q, 0.45);
            sl.sendParticles(Fx.bolt(rgb, 1.0F), q.x, q.y, q.z, 1, 0, 0, 0, 0);
            prev = q;
        }
        sl.sendParticles(ParticleTypes.ELECTRIC_SPARK, b.x, b.y, b.z, 6, 0.3, 0.3, 0.3, 0.4);
    }

    /** Esfera de puntos (distribución en espiral de Fibonacci). */
    public static void sphere(ServerLevel sl, ParticleOptions p, Vec3 c, double r, int n, double phase) {
        double golden = Math.PI * (3.0 - Math.sqrt(5.0));
        for (int i = 0; i < n; i++) {
            double y = 1.0 - (i + 0.5) * 2.0 / n;
            double rad = Math.sqrt(Math.max(0, 1.0 - y * y));
            double a = i * golden + phase;
            sl.sendParticles(p, c.x + Math.cos(a) * rad * r, c.y + y * r, c.z + Math.sin(a) * rad * r, 1, 0, 0, 0, 0);
        }
    }

    /** Estallido que sale en todas direcciones (las partículas se mueven solas). */
    public static void burst(ServerLevel sl, ParticleOptions p, Vec3 c, int n, double speed) {
        for (int i = 0; i < n; i++) {
            double u = sl.random.nextDouble() * 2.0 - 1.0;
            double a = sl.random.nextDouble() * Math.PI * 2.0;
            double r = Math.sqrt(1.0 - u * u);
            sl.sendParticles(p, c.x, c.y, c.z, 0, Math.cos(a) * r * speed, u * speed, Math.sin(a) * r * speed, 1.0);
        }
    }

    /** Anillo en un plano cualquiera (normal = hacia dónde "mira" el anillo). */
    public static void ringFacing(ServerLevel sl, ParticleOptions p, Vec3 c, Vec3 normal, double r, int n, double phase) {
        Vec3 nrm = normal.normalize();
        Vec3 ref = Math.abs(nrm.y) > 0.95 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
        Vec3 u = nrm.cross(ref).normalize();
        Vec3 v = nrm.cross(u).normalize();
        for (int i = 0; i < n; i++) {
            double a = phase + Math.PI * 2.0 * i / n;
            Vec3 q = c.add(u.scale(Math.cos(a) * r)).add(v.scale(Math.sin(a) * r));
            sl.sendParticles(p, q.x, q.y, q.z, 1, 0, 0, 0, 0);
        }
    }

    /** Círculo mágico en el suelo: anillo, runas y chispas. */
    public static void sigil(ServerLevel sl, Vec3 c, double r, int rgb, double phase) {
        Fx.ring(sl, Fx.dust(rgb, 0.7F), c.x, c.y + 0.08, c.z, r, (int) (r * 10), phase);
        Fx.ring(sl, Fx.dust(rgb, 0.5F), c.x, c.y + 0.08, c.z, r * 0.62, (int) (r * 6), -phase);
        int runes = 6;
        for (int i = 0; i < runes; i++) {
            double a = phase * 0.5 + Math.PI * 2.0 * i / runes;
            sl.sendParticles(Fx.rune(rgb, 1.0F), c.x + Math.cos(a) * r * 0.81, c.y + 0.15, c.z + Math.sin(a) * r * 0.81, 1, 0, 0, 0, 0);
        }
        sl.sendParticles(Fx.spark(0xFFFFFF, 0.6F), c.x, c.y + 0.1, c.z, 1, r * 0.4, 0, r * 0.4, 0);
    }

    /** Bloque del suelo bajo un punto (para partículas de tierra). */
    public static BlockState groundState(ServerLevel sl, double x, double y, double z) {
        BlockPos bp = BlockPos.containing(x, y - 0.5, z);
        for (int i = 0; i < 4; i++) {
            BlockState st = sl.getBlockState(bp);
            if (!st.isAir() && st.getFluidState().isEmpty()) {
                return st;
            }
            bp = bp.below();
        }
        return Blocks.STONE.defaultBlockState();
    }

    public static ParticleOptions blockDust(BlockState st) {
        return new BlockParticleOption(ParticleTypes.BLOCK, st);
    }

    // ---- Aliados, terreno y líneas (reliquias nuevas) ----

    /** Aliados: el otro jugador y las mascotas domesticadas. */
    public static boolean isAlly(LivingEntity e, ServerPlayer sp) {
        if (e == sp || !e.isAlive()) {
            return false;
        }
        if (e instanceof net.minecraft.world.entity.player.Player) {
            return true;
        }
        return e instanceof net.minecraft.world.entity.TamableAnimal t && t.isTame();
    }

    public static List<LivingEntity> allies(ServerLevel sl, Vec3 c, double r, ServerPlayer sp, boolean includeSelf) {
        List<LivingEntity> list = new ArrayList<>(sl.getEntitiesOfClass(LivingEntity.class,
                new AABB(c.x - r, c.y - r, c.z - r, c.x + r, c.y + r, c.z + r),
                e -> (isAlly(e, sp) || (includeSelf && e == sp)) && e.position().distanceTo(c) <= r));
        list.sort(Comparator.comparingDouble(e -> e.position().distanceToSqr(c)));
        return list;
    }

    /** Altura de la superficie del suelo cerca de (x, y, z); si no hay, devuelve y. */
    public static double groundY(ServerLevel sl, double x, double y, double z) {
        BlockPos.MutableBlockPos bp = new BlockPos.MutableBlockPos(Math.floor(x), Math.floor(y) + 2, Math.floor(z));
        for (int i = 0; i < 9; i++) {
            BlockState st = sl.getBlockState(bp);
            if (!st.isAir() && !st.getCollisionShape(sl, bp).isEmpty()) {
                return bp.getY() + st.getCollisionShape(sl, bp).max(net.minecraft.core.Direction.Axis.Y);
            }
            bp.move(0, -1, 0);
        }
        return y;
    }

    /** Distancia de un punto a un segmento. */
    public static double segDist(Vec3 p, Vec3 a, Vec3 b) {
        Vec3 ab = b.subtract(a);
        double l2 = ab.lengthSqr();
        double t = l2 < 1.0E-9 ? 0 : Math.max(0, Math.min(1, p.subtract(a).dot(ab) / l2));
        return p.distanceTo(a.add(ab.scale(t)));
    }

    /** Enemigos cerca de un segmento (radio contado desde el centro de su caja). */
    public static List<LivingEntity> foesAlong(ServerLevel sl, Vec3 a, Vec3 b, double radius, ServerPlayer sp) {
        Vec3 mid = a.add(b).scale(0.5);
        double half = a.distanceTo(b) * 0.5 + radius + 2.0;
        List<LivingEntity> list = new ArrayList<>();
        for (LivingEntity e : foes(sl, mid, half, sp)) {
            if (segDist(e.getBoundingBox().getCenter(), a, b) <= radius + e.getBbWidth() * 0.5) {
                list.add(e);
            }
        }
        return list;
    }

    /** Empuja a una entidad alejándola de un punto. */
    public static void knock(LivingEntity e, Vec3 from, double horizontal, double up) {
        Vec3 d = e.position().subtract(from);
        Vec3 h = new Vec3(d.x, 0, d.z);
        h = h.lengthSqr() < 1.0E-6 ? new Vec3(0, 0, 0) : h.normalize();
        e.setDeltaMovement(e.getDeltaMovement().add(h.x * horizontal, up, h.z * horizontal));
        e.hurtMarked = true;
    }

    public static boolean isNight(ServerLevel sl) {
        long t = sl.getDayTime() % 24000L;
        return t >= 13000L && t <= 23000L;
    }

    /** Daño propio de las reliquias nuevas (cuenta como ataque del jugador). */
    public static void damage(ServerLevel sl, ServerPlayer sp, LivingEntity e, float amount) {
        if (e.isAlive() && amount > 0) {
            e.hurt(sl.damageSources().playerAttack(sp), amount);
        }
    }

    public static void sendActionBar(ServerPlayer sp, String key) {
        sp.displayClientMessage(net.minecraft.network.chat.Component.translatable(key), true);
    }
}
