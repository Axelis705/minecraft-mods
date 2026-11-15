package com.casino.heist;

import com.casino.entity.DealerAnim;
import com.casino.entity.DealerEntity;
import com.casino.plat.Nbt;
import com.casino.plat.Plat;
import com.casino.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.EnumSet;
import java.util.Iterator;

/**
 * Los guardias del robo (crupieres con {@link DealerEntity#GAME_SECURITY}). Tipo 0: táser y porra (zona de empleados); tipo 1: pistola
 * (bóveda). Sin robo hacen su ronda y no molestan a nadie. En la parte sigilosa ven hacia donde miran (más de cerca si vas agachado),
 * suben la sospecha y van a revisar; si te alcanzan, alarma. Con la alarma o en el escape persiguen y atacan solo a los ladrones; el
 * daño es un porcentaje de la vida máxima del jugador.
 */
public final class GuardAI {
    public static final int TASER = 0;
    public static final int ARMED = 1;
    static final double SPEED_WALK = 0.32;
    static final double SPEED_RUN = 0.62;
    static final float FOV = 70F;
    static final double SIGHT = 14;
    static final double SIGHT_SNEAK = 7;

    private GuardAI() {
    }

    public static void register(DealerEntity e, GoalSelector goals) {
        goals.addGoal(1, new Combat(e));
        goals.addGoal(2, new Investigate(e));
        goals.addGoal(3, new Patrol(e));
    }

    private static boolean walk(DealerEntity e, double x, double y, double z, double speed) {
        // ". moveTo" con espacio: el preprocesador cambia ".moveTo(" por ".snapTo(" en 26.x (eso es teletransportar)
        return e.getNavigation(). moveTo(x, y, z, speed);
    }

    // ================================================================== vista (lo llama el guardia cada 5 ticks)

    /** Revisa si ve a alguien del robo; devuelve a quién vio (o null). */
    public static ServerPlayer look(DealerEntity g) {
        HeistManager.Heist h = HeistManager.at(g.level(), g.blockPosition());
        if (h == null || h.phase != HeistManager.Phase.STEALTH || h.alarm) {
            return null;
        }
        Vec3 eye = g.getEyePosition();
        for (ServerPlayer p : h.crewOnline(g.level())) {
            Vec3 t = p.getEyePosition();
            double dx = t.x - eye.x, dz = t.z - eye.z;
            double d = Math.sqrt(dx * dx + dz * dz);
            double r = p.isCrouching() ? SIGHT_SNEAK : SIGHT;
            if (d > r || Math.abs(t.y - eye.y) > 4) {
                continue;
            }
            float want = (float) (Mth.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0F;
            boolean front = Math.abs(Mth.wrapDegrees(want - g.getYHeadRot())) <= FOV || d < 1.6;
            if (front && HeistManager.clear(g.level(), eye, t, g)) {
                // de cerca y de frente sube más rápido; tan cerca que te toca: alarma
                HeistManager.spot(h, p, d < 4 ? 16F : 9F, eye);
                if (d < 2.0) {
                    g.say(DealerEntity.SAY + 2);
                    HeistManager.alarm(g.level(), h, "alarm.caught");
                }
                return p;
            }
        }
        return null;
    }

    // ================================================================== ronda

    static final class Patrol extends Goal {
        private final DealerEntity e;
        private boolean toFar;
        private int wait;

        Patrol(DealerEntity e) {
            this.e = e;
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (!e.isSecurity() || e.home() == null || e.investigating() != null) {
                return false;
            }
            HeistManager.Heist h = HeistManager.at(e.level(), e.blockPosition());
            return h == null || !h.hostile();
        }

        @Override
        public void tick() {
            BlockPos home = e.home();
            int dx = e.patrolDx(), dz = e.patrolDz();
            Vec3 a = Vec3.atBottomCenterOf(home);
            Vec3 b = a.add(dx, 0, dz);
            Vec3 target = toFar ? b : a;
            double d2 = e.position().distanceToSqr(target.x, e.getY(), target.z);
            if (d2 < 0.6) {
                if (dx == 0 && dz == 0) {
                    // de guardia en su lugar: mira hacia donde le tocó y de vez en cuando voltea
                    if (e.getRandom().nextInt(80) == 0) {
                        e.getLookControl().setLookAt(e.getX() + e.getRandom().nextGaussian() * 4, e.getEyeY(), e.getZ() + e.getRandom().nextGaussian() * 4);
                    }
                    return;
                }
                if (wait <= 0) {
                    wait = 40 + e.getRandom().nextInt(70);
                    e.getNavigation().stop();
                }
                if (--wait == 0) {
                    toFar = !toFar;
                } else if (wait % 25 == 0) {
                    e.getLookControl().setLookAt(e.getX() + e.getRandom().nextGaussian() * 5, e.getEyeY(), e.getZ() + e.getRandom().nextGaussian() * 5);
                }
            } else if (e.getNavigation().isDone()) {
                walk(e, target.x, target.y, target.z, SPEED_WALK);
            }
        }

        @Override
        public boolean canContinueToUse() {
            return canUse();
        }

        @Override
        public void stop() {
            e.getNavigation().stop();
        }
    }

    // ================================================================== ir a revisar

    static final class Investigate extends Goal {
        private final DealerEntity e;
        private int left;

        Investigate(DealerEntity e) {
            this.e = e;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return e.isSecurity() && e.investigating() != null;
        }

        @Override
        public void start() {
            left = 140;
            Vec3 p = e.investigating();
            walk(e, p.x, p.y, p.z, SPEED_WALK * 1.3);
            e.anim(DealerAnim.STOP);
        }

        @Override
        public void tick() {
            Vec3 p = e.investigating();
            if (p == null) {
                return;
            }
            e.getLookControl().setLookAt(p.x, p.y + 1.5, p.z);
            if (--left <= 0 || e.position().distanceToSqr(p) < 2.0 && e.getNavigation().isDone()) {
                e.investigate(null);
            } else if (e.getNavigation().isDone() && left % 20 == 0) {
                walk(e, p.x, p.y, p.z, SPEED_WALK * 1.3);
            }
        }

        @Override
        public boolean canContinueToUse() {
            HeistManager.Heist h = HeistManager.at(e.level(), e.blockPosition());
            return canUse() && (h == null || !h.hostile());
        }

        @Override
        public void stop() {
            e.investigate(null);
            e.getNavigation().stop();
        }
    }

    // ================================================================== combate

    static final class Combat extends Goal {
        private final DealerEntity e;
        private ServerPlayer target;
        private int meleeCd;
        private int rangedCd;
        private int repath;

        Combat(DealerEntity e) {
            this.e = e;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        private ServerPlayer find() {
            HeistManager.Heist h = HeistManager.at(e.level(), e.blockPosition());
            if (h == null || !h.hostile()) {
                return null;
            }
            ServerPlayer best = null;
            double bd = 40 * 40;
            for (ServerPlayer p : h.crewOnline(e.level())) {
                double d = p.distanceToSqr(e);
                boolean see = HeistManager.clear(e.level(), e.getEyePosition(), p.getEyePosition(), e);
                if (!see) {
                    d *= 3; // prefiere al que ve
                }
                if (d < bd && Math.abs(p.getY() - e.getY()) < 14) {
                    bd = d;
                    best = p;
                }
            }
            return best;
        }

        @Override
        public boolean canUse() {
            if (!e.isSecurity()) {
                return false;
            }
            target = find();
            return target != null;
        }

        @Override
        public boolean canContinueToUse() {
            if (!e.isSecurity() || target == null || !target.isAlive()) {
                return false;
            }
            HeistManager.Heist h = HeistManager.at(e.level(), e.blockPosition());
            return h != null && h.hostile() && h.isCrew(target);
        }

        @Override
        public void start() {
            e.setCombat(1);
            e.investigate(null);
            rangedCd = 20;
        }

        @Override
        public void stop() {
            e.setCombat(0);
            target = null;
            e.getNavigation().stop();
        }

        @Override
        public void tick() {
            if (target == null) {
                return;
            }
            if (e.tickCount % 40 == 0) {
                ServerPlayer t = find();
                if (t != null) {
                    target = t;
                }
            }
            e.getLookControl().setLookAt(target, 40F, 40F);
            double d = Math.sqrt(e.distanceToSqr(target));
            boolean see = HeistManager.clear(e.level(), e.getEyePosition(), target.getEyePosition(), e);
            meleeCd--;
            rangedCd--;
            int kind = e.guardKind();
            if (kind == ARMED) {
                // dispara de lejos; si no te ve, se acerca
                if (!see || d > 13) {
                    e.setCombat(1);
                    if (--repath <= 0) {
                        repath = 10;
                        walk(e, target.getX(), target.getY(), target.getZ(), SPEED_RUN);
                    }
                } else {
                    e.getNavigation().stop();
                    e.setCombat(2);
                    if (rangedCd <= 0) {
                        rangedCd = 22 + e.getRandom().nextInt(10);
                        shoot(e, target, d);
                    }
                }
                if (d < 2.4 && meleeCd <= 0) {
                    meleeCd = 24;
                    baton(e, target, 0.12F);
                }
            } else {
                // táser de cerca y porra al alcanzarte
                if (d > 2.2) {
                    if (--repath <= 0) {
                        repath = 8;
                        walk(e, target.getX(), target.getY(), target.getZ(), SPEED_RUN);
                    }
                } else {
                    e.getNavigation().stop();
                }
                e.setCombat(see && d > 2.6 && d < 9 ? 2 : 1);
                if (d <= 2.6 && meleeCd <= 0) {
                    meleeCd = 20;
                    baton(e, target, 0.12F);
                } else if (see && d > 2.6 && d < 9 && rangedCd <= 0) {
                    rangedCd = 70;
                    taser(e, target);
                }
            }
        }
    }

    // ================================================================== ataques

    static void baton(DealerEntity g, ServerPlayer t, float pct) {
        Plat.swing(g);
        g.anim(DealerAnim.STOP);
        HeistManager.hurtPct(t, g.level().damageSources().mobAttack(g), pct);
        Vec3 k = t.position().subtract(g.position()).normalize().scale(0.45);
        t.push(k.x, 0.18, k.z);
        //#if MC < 26.3
        t.hurtMarked = true;
        //#endif
        g.level().playSound(null, t.blockPosition(), SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.HOSTILE, 1F, 0.8F);
    }

    static void taser(DealerEntity g, ServerPlayer t) {
        Vec3 a = g.getEyePosition().add(0, -0.35, 0), b = t.position().add(0, 1.0, 0);
        tracer(g.level(), a, b, true);
        g.level().playSound(null, g.blockPosition(), SoundEvents.FIRE_EXTINGUISH, SoundSource.HOSTILE, 0.9F, 2.0F);
        if (g.getRandom().nextFloat() < 0.75F) {
            HeistManager.hurtPct(t, g.level().damageSources().mobAttack(g), 0.05F);
            t.addEffect(HeistManager.slowness(70, 2));
            t.addEffect(HeistManager.nausea(80));
            g.level().playSound(null, t.blockPosition(), SoundEvents.BEEHIVE_WORK, SoundSource.HOSTILE, 1F, 2.0F);
        }
    }

    static void shoot(DealerEntity g, ServerPlayer t, double dist) {
        Vec3 a = g.getEyePosition().add(0, -0.3, 0);
        Vec3 aim = t.position().add(0, t.isCrouching() ? 0.8 : 1.1, 0);
        float chance = (float) Math.max(0.3, 0.8 - dist * 0.025) - (t.isCrouching() ? 0.1F : 0F) - (t.isSprinting() ? 0.15F : 0F);
        boolean hit = g.getRandom().nextFloat() < chance;
        Vec3 end = hit ? aim : aim.add(g.getRandom().nextGaussian() * 0.9, g.getRandom().nextGaussian() * 0.6, g.getRandom().nextGaussian() * 0.9);
        tracer(g.level(), a, end, false);
        g.level().playSound(null, g.blockPosition(), SoundEvents.FIREWORK_ROCKET_BLAST, SoundSource.HOSTILE, 2.2F, 1.7F);
        if (g.level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.SMOKE, a.x, a.y, a.z, 3, 0.05, 0.05, 0.05, 0.01);
            sl.sendParticles(ParticleTypes.FLAME, a.x, a.y, a.z, 1, 0.02, 0.02, 0.02, 0.0);
        }
        if (hit) {
            HeistManager.hurtPct(t, g.level().damageSources().mobAttack(g), 0.2F);
        } else {
            g.level().playSound(null, BlockPos.containing(end), SoundEvents.ARROW_HIT, SoundSource.HOSTILE, 0.8F, 1.8F);
        }
    }

    static void tracer(Level level, Vec3 a, Vec3 b, boolean electric) {
        if (!(level instanceof ServerLevel sl)) {
            return;
        }
        Vec3 d = b.subtract(a);
        int n = (int) Math.max(4, d.length() * 4);
        //#if MC >= 1.21.2
        //$$ DustParticleOptions dust = new DustParticleOptions(electric ? 0x80D9FF : 0xFFD966, 0.6F);
        //#else
        DustParticleOptions dust = new DustParticleOptions(electric ? new Vector3f(0.5F, 0.85F, 1F) : new Vector3f(1F, 0.85F, 0.4F), 0.6F);
        //#endif
        for (int i = 0; i <= n; i++) {
            Vec3 p = a.add(d.scale(i / (double) n));
            if (electric) {
                sl.sendParticles(ParticleTypes.ELECTRIC_SPARK, p.x, p.y, p.z, 1, 0.04, 0.04, 0.04, 0.0);
            } else {
                sl.sendParticles(dust, p.x, p.y, p.z, 1, 0, 0, 0, 0);
            }
        }
    }

    // ================================================================== aparecer, refuerzos y calma

    /** Datos para volver a poner a un guardia que cayó. */
    public static CompoundTag spawnData(DealerEntity g) {
        CompoundTag t = new CompoundTag();
        BlockPos h = g.home() != null ? g.home() : g.blockPosition();
        t.putInt("x", h.getX());
        t.putInt("y", h.getY());
        t.putInt("z", h.getZ());
        t.putFloat("yaw", g.getYRot());
        t.putInt("kind", g.guardKind());
        t.putInt("dx", g.patrolDx());
        t.putInt("dz", g.patrolDz());
        return t;
    }

    public static DealerEntity spawn(Level level, BlockPos pos, float yaw, int kind, int dx, int dz) {
        DealerEntity g = Plat.create(ModEntities.DEALER.get(), level);
        if (g == null) {
            return null;
        }
        g.setGame(DealerEntity.GAME_SECURITY);
        g.setVariant(kind);
        g.setHome(pos);
        g.setPatrol(dx, dz);
        g.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, yaw, 0F);
        g.setYHeadRot(yaw);
        g.setYBodyRot(yaw);
        if (kind == ARMED) {
            var hp = g.getAttribute(Attributes.MAX_HEALTH);
            if (hp != null) {
                hp.setBaseValue(30.0);
            }
        }
        g.setHealth(g.getMaxHealth());
        level.addFreshEntity(g);
        return g;
    }

    private static DealerEntity spawn(Level level, CompoundTag t, int kindOverride) {
        return spawn(level, new BlockPos(Nbt.i(t, "x"), Nbt.i(t, "y"), Nbt.i(t, "z")), Nbt.f(t, "yaw"),
                kindOverride >= 0 ? kindOverride : Nbt.i(t, "kind"), Nbt.i(t, "dx"), Nbt.i(t, "dz"));
    }

    /** Refuerzos durante el escape: vuelven hasta 3 guardias caídos (más adelante, todos con pistola). */
    static void reinforce(Level level, HeistManager.Heist h) {
        int n = 0;
        boolean armed = level.getGameTime() - h.phaseAt > 800;
        for (Iterator<CompoundTag> it = h.deadGuards.iterator(); it.hasNext() && n < 3; ) {
            CompoundTag t = it.next();
            BlockPos p = new BlockPos(Nbt.i(t, "x"), Nbt.i(t, "y"), Nbt.i(t, "z"));
            boolean near = false;
            for (Player pl : h.crewOnline(level)) {
                if (pl.blockPosition().distSqr(p) < 100) {
                    near = true;
                }
            }
            if (!near) {
                spawn(level, t, armed ? ARMED : -1);
                it.remove();
                n++;
            }
        }
    }

    /** Al terminar el robo: vuelven todos los guardias que cayeron. */
    static void restore(Level level, HeistManager.Heist h) {
        for (CompoundTag t : h.deadGuards) {
            spawn(level, t, -1);
        }
        h.deadGuards.clear();
    }

    /** Al terminar: todos tranquilos y de vuelta a su lugar. */
    static void calm(Level level, BlockPos vault) {
        int[] z = HeistManager.zoneOf(level, vault);
        for (DealerEntity g : level.getEntitiesOfClass(DealerEntity.class, new AABB(z[0], z[1], z[2], z[3] + 1, z[4] + 1, z[5] + 1))) {
            if (g.isSecurity()) {
                g.setCombat(0);
                g.investigate(null);
                g.setHealth(g.getMaxHealth());
            }
        }
    }
}
