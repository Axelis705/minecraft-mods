package com.mundosdeaxelis.world;

import com.mundosdeaxelis.MundosDeAxelis;
import com.mundosdeaxelis.entity.Mascota;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

/**
 * Etapa 284: la «cabeza» de los jefes. En lugar de ir siempre por el jugador más cercano (o por el último que le pegó), cada jefe
 * lleva la cuenta de la <b>amenaza</b> de cada uno:
 * <ul>
 * <li>el daño que le hace cada jugador y cada mascota suma amenaza (y se va olvidando poco a poco);</li>
 * <li>estar cerca también cuenta: al que tiene encima lo ve más;</li>
 * <li>las mascotas que le muerden son objetivo: si el jugador se aleja y su mascota sigue pegando, el jefe se gira y la ataca
 * (ya no se queda persiguiendo a quien huye mientras le muerden);</li>
 * <li>no cambia de objetivo a cada momento: el actual vale un poco más y hay un tiempo mínimo entre cambios;</li>
 * <li>si no puede llegar a alguien (está subido muy arriba o fuera de su alcance), ese casi no cuenta;</li>
 * <li>contra el «kiteo»: si su objetivo se mantiene lejos mucho rato, su amenaza baja (va a por otro que tenga a mano) y, si no
 * hay otro, el jefe da un salto de persecución hacia él.</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = MundosDeAxelis.MODID)
public final class JefeIA {
    private JefeIA() {
    }

    /** Cada cuánto se vuelve a pensar el objetivo (ticks), lo mínimo entre dos cambios y cuánto se olvida la amenaza. */
    public static final int CADA = 10, CAMBIO_MIN = 50;
    public static final float OLVIDO = 0.95F;
    /** Lejos: a más de esto el objetivo «está kiteando»; y cuánto aguanta así antes de que el jefe reaccione (ticks). */
    public static final double LEJOS = 9.0;
    public static final int KITEO = 120, SALTO_CD = 160;

    static final class Cerebro {
        final Map<UUID, Float> amenaza = new HashMap<>();
        LivingEntity actual;
        long cambio = -1000, eval = -1000, salto = -1000, manotazo = -1000;
        int kiteo;
    }

    private static final Map<Mob, Cerebro> CEREBROS = new WeakHashMap<>();

    public static void clearRuntime() {
        CEREBROS.clear();
    }

    private static Cerebro cerebro(Mob m) {
        return CEREBROS.computeIfAbsent(m, k -> new Cerebro());
    }

    /** Jefes y mini-jefes del mod. */
    public static boolean esJefe(Entity e) {
        return e instanceof Mob && com.mundosdeaxelis.arma.Armas.esJefe((LivingEntity) e);
    }

    // ------------------------------------------------------------------ amenaza por daño

    /** Quién hizo el daño: el jugador o la mascota (y el que disparó, si fue un proyectil). */
    static LivingEntity autor(net.minecraft.world.damagesource.DamageSource src) {
        Entity a = src.getEntity();
        if (a instanceof LivingEntity le) {
            return le;
        }
        if (src.getDirectEntity() instanceof Projectile p && p.getOwner() instanceof LivingEntity le) {
            return le;
        }
        return null;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onDamage(LivingDamageEvent e) {
        if (!(e.getEntity() instanceof Mob jefe) || jefe.level().isClientSide || !esJefe(jefe) || e.getAmount() <= 0.0F) {
            return;
        }
        LivingEntity a = autor(e.getSource());
        if (a == null || a == jefe || !(a instanceof ServerPlayer || a instanceof Mascota)) {
            return;
        }
        sumar(jefe, a, e.getAmount() * (a instanceof Mascota ? 1.2F : 1.0F));
    }

    /** Suma amenaza a {@code quien} (también lo usa la prueba). */
    public static void sumar(Mob jefe, LivingEntity quien, float cuanto) {
        cerebro(jefe).amenaza.merge(quien.getUUID(), cuanto, Float::sum);
    }

    public static float amenaza(Mob jefe, LivingEntity quien) {
        return cerebro(jefe).amenaza.getOrDefault(quien.getUUID(), 0.0F);
    }

    // ------------------------------------------------------------------ elegir objetivo

    /** Solo para la prueba del servidor (allí no hay jugadores de verdad): candidatos de mentira. */
    public static final List<LivingEntity> PRUEBA = new ArrayList<>();

    /** Los que puede atacar: jugadores (los creativos solo si {@code creativos}) y las mascotas de esos jugadores que pelean. */
    public static List<LivingEntity> candidatos(Mob jefe, ServerLevel sl, double range, boolean creativos) {
        List<LivingEntity> out = new ArrayList<>();
        for (LivingEntity t : PRUEBA) {
            if (t.isAlive() && t.level() == sl && t.distanceTo(jefe) <= range) {
                out.add(t);
            }
        }
        List<ServerPlayer> ps = new ArrayList<>();
        for (ServerPlayer p : sl.players()) {
            if (p.isAlive() && !p.isSpectator() && (creativos || !p.isCreative()) && p.distanceTo(jefe) <= range) {
                ps.add(p);
                out.add(p);
            }
        }
        if (ps.isEmpty()) {
            return out;
        }
        Cerebro c = cerebro(jefe);
        // (las mascotas solo cuentan si su dueño está en la pelea)
        for (Mascota m : sl.getEntitiesOfClass(Mascota.class, jefe.getBoundingBox().inflate(range), x -> x.isAlive() && x.canFight())) {
            if (m.getOwner() instanceof ServerPlayer o && ps.contains(o)
                    && (c.amenaza.getOrDefault(m.getUUID(), 0.0F) > 0.0F || m.distanceTo(jefe) < 4.0F || m.getTarget() == jefe)) {
                out.add(m);
            }
        }
        return out;
    }

    /** ¿Puede llegar? (si no vuela: que no esté muy por encima de él) */
    static boolean alcanzable(Mob jefe, LivingEntity t) {
        if (jefe.isNoGravity()) {
            return true;
        }
        double dy = t.getY() - jefe.getY();
        return dy < jefe.getBbHeight() + 2.5 && dy > -6.0;
    }

    /** Lo que vale cada uno como objetivo ahora mismo. */
    static double valor(Mob jefe, Cerebro c, LivingEntity t) {
        double d = jefe.distanceTo(t);
        double v = c.amenaza.getOrDefault(t.getUUID(), 0.0F) + 24.0 / (1.0 + d * 0.5);
        if (t instanceof Mascota) {
            v *= 0.9;
        }
        if (!alcanzable(jefe, t)) {
            // subido a un pilar o fuera de su alcance: casi no cuenta (si no hay nadie más, sigue con él)
            return v * 0.1;
        }
        if (t == c.actual) {
            v *= 1.35;
        }
        return v;
    }

    /**
     * El objetivo que conviene ahora entre los que hay a menos de {@code range} (null si no hay nadie). Se recalcula cada medio
     * segundo; entre medias devuelve el mismo.
     */
    public static LivingEntity objetivo(Mob jefe, ServerLevel sl, double range, boolean creativos) {
        Cerebro c = cerebro(jefe);
        long now = sl.getGameTime();
        boolean valido = c.actual != null && c.actual.isAlive() && !c.actual.isRemoved() && c.actual.level() == sl
                && c.actual.distanceTo(jefe) <= range && !(c.actual instanceof ServerPlayer sp && (sp.isSpectator() || !creativos && sp.isCreative()));
        if (valido && now - c.eval < CADA) {
            return c.actual;
        }
        c.eval = now;
        // se va olvidando
        for (Iterator<Map.Entry<UUID, Float>> it = c.amenaza.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<UUID, Float> en = it.next();
            float v = en.getValue() * OLVIDO;
            if (v < 0.05F) {
                it.remove();
            } else {
                en.setValue(v);
            }
        }
        if (!valido) {
            c.actual = null;
        }
        // kiteo: el objetivo se mantiene lejos
        if (c.actual != null && jefe.distanceTo(c.actual) > LEJOS) {
            c.kiteo += CADA;
        } else {
            c.kiteo = 0;
        }
        if (c.kiteo >= KITEO && c.actual != null) {
            // deja de valer tanto: si tiene a otro a mano, va a por él
            c.amenaza.computeIfPresent(c.actual.getUUID(), (k, v) -> v * 0.5F);
        }
        List<LivingEntity> cs = candidatos(jefe, sl, range, creativos);
        LivingEntity best = null;
        double bv = -1;
        for (LivingEntity t : cs) {
            double v = valor(jefe, c, t);
            if (v > bv) {
                bv = v;
                best = t;
            }
        }
        if (best == null) {
            c.actual = null;
            return null;
        }
        if (best != c.actual) {
            boolean puede = c.actual == null || now - c.cambio >= CAMBIO_MIN || !alcanzable(jefe, c.actual);
            if (puede) {
                c.actual = best;
                c.cambio = now;
                c.kiteo = 0;
            }
        }
        return c.actual;
    }

    /** ¿Lleva mucho rato sin poder alcanzar a su objetivo? (el jefe puede usar su salto, carga o lo que tenga) */
    public static boolean leKitean(Mob jefe) {
        return cerebro(jefe).kiteo >= KITEO;
    }

    /**
     * Salto de persecución: si le kitean y está en el suelo, salta hacia su objetivo (con aviso de humo y sonido). Para los jefes
     * que no tienen su propio salto o carga. Devuelve true si saltó.
     */
    public static boolean saltoPersecucion(Mob jefe, ServerLevel sl) {
        Cerebro c = cerebro(jefe);
        LivingEntity t = c.actual;
        long now = sl.getGameTime();
        if (t == null || !leKitean(jefe) || !jefe.onGround() || now - c.salto < SALTO_CD || !alcanzable(jefe, t)) {
            return false;
        }
        c.salto = now;
        Vec3 d = new Vec3(t.getX() - jefe.getX(), 0, t.getZ() - jefe.getZ());
        double dist = d.length();
        if (dist < 1.0E-3) {
            return false;
        }
        d = d.normalize();
        double fuerza = Math.min(1.6, 0.45 + dist * 0.07);
        jefe.setDeltaMovement(d.x * fuerza, 0.55 + Math.min(0.35, dist * 0.02), d.z * fuerza);
        jefe.hasImpulse = true;
        jefe.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES, t.getEyePosition());
        sl.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, jefe.getX(), jefe.getY() + 0.2, jefe.getZ(), 14, jefe.getBbWidth() * 0.4, 0.1,
                jefe.getBbWidth() * 0.4, 0.02);
        sl.playSound(null, jefe.blockPosition(), SoundEvents.RAVAGER_ROAR, SoundSource.HOSTILE, 0.8F, 1.3F);
        c.kiteo = 0;
        return true;
    }

    /**
     * Las mascotas que le muerden de cerca se llevan un manotazo (cada 1,2 s): así el jefe no se deja morder mientras persigue a
     * otro. Para los jefes cuyos ataques de área solo miran a los jugadores. Devuelve true si golpeó.
     */
    public static boolean manotazo(Mob jefe, ServerLevel sl, float dmg) {
        Cerebro c = cerebro(jefe);
        long now = sl.getGameTime();
        if (now - c.manotazo < 24) {
            return false;
        }
        double reach = jefe.getBbWidth() * 0.5 + 2.2;
        Mascota best = null;
        double bd = Double.MAX_VALUE;
        for (Mascota m : sl.getEntitiesOfClass(Mascota.class, jefe.getBoundingBox().inflate(reach, 1.5, reach), x -> x.isAlive() && x.canFight())) {
            double d = m.distanceToSqr(jefe);
            if (d < bd && (c.amenaza.getOrDefault(m.getUUID(), 0.0F) > 0.0F || m.getTarget() == jefe || c.actual == m)) {
                bd = d;
                best = m;
            }
        }
        if (best == null) {
            return false;
        }
        c.manotazo = now;
        jefe.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
        jefe.getLookControl().setLookAt(best, 40.0F, 40.0F);
        best.hurt(jefe.damageSources().mobAttack(jefe), dmg);
        Vec3 d = new Vec3(best.getX() - jefe.getX(), 0, best.getZ() - jefe.getZ());
        d = d.lengthSqr() < 1.0E-4 ? new Vec3(0, 0, 1) : d.normalize();
        best.setDeltaMovement(best.getDeltaMovement().add(d.x * 0.7, 0.3, d.z * 0.7));
        best.hurtMarked = true;
        sl.sendParticles(ParticleTypes.SWEEP_ATTACK, best.getX(), best.getY() + best.getBbHeight() * 0.5, best.getZ(), 1, 0, 0, 0, 0);
        return true;
    }

    /**
     * Para los mini-jefes con metas de Minecraft: sustituye a «el último que me pegó» y «el jugador más cercano». Cada medio
     * segundo elige con la amenaza (incluidas las mascotas) y, si le kitean, salta.
     */
    public static final class MetaObjetivo extends Goal {
        private final Mob jefe;
        private final double range;
        private final java.util.function.BooleanSupplier activo;

        public MetaObjetivo(Mob jefe, double range) {
            this(jefe, range, () -> true);
        }

        /** {@code activo}: si está despierto (el Coloso duerme hasta que alguien se acerca). */
        public MetaObjetivo(Mob jefe, double range, java.util.function.BooleanSupplier activo) {
            this.jefe = jefe;
            this.range = range;
            this.activo = activo;
            setFlags(EnumSet.of(Flag.TARGET));
        }

        @Override
        public boolean canUse() {
            return activo.getAsBoolean() && jefe.level() instanceof ServerLevel sl && objetivo(jefe, sl, range, false) != null;
        }

        @Override
        public boolean canContinueToUse() {
            return canUse();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            if (!(jefe.level() instanceof ServerLevel sl)) {
                return;
            }
            LivingEntity t = objetivo(jefe, sl, range, false);
            if (t != jefe.getTarget()) {
                jefe.setTarget(t);
            }
            saltoPersecucion(jefe, sl);
        }

        @Override
        public void stop() {
            jefe.setTarget(null);
        }
    }
}
