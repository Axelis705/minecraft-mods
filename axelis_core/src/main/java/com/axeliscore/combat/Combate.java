package com.axeliscore.combat;

import com.axeliscore.plat.Ids;
import com.axeliscore.plat.Plat;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * (Servidor) Ayudas de combate para todos los mods: a quién se le puede pegar, de dónde viene el daño, dónde está el suelo, sonidos y
 * partículas por nombre (iguales en todas las versiones).
 */
public final class Combate {
    private Combate() {
    }

    /**
     * ¿Son del mismo bando? (no se pegan entre sí): el mismo, el mismo equipo, la mascota de uno, o dos criaturas del mismo tipo si
     * ninguna es jugador.
     */
    public static boolean aliados(Entity a, Entity b) {
        if (a == null || b == null) {
            return false;
        }
        if (a == b || a.isAlliedTo(b)) {
            return true;
        }
        if (b instanceof OwnableEntity o && o.getOwner() == a || a instanceof OwnableEntity o2 && o2.getOwner() == b) {
            return true;
        }
        return !(a instanceof Player) && !(b instanceof Player) && a.getType() == b.getType();
    }

    /** ¿Se le puede pegar? (vivo, no es aliado del atacante, no es un jugador en creativo o espectador). */
    public static boolean blanco(Entity atacante, Entity e) {
        if (!(e instanceof LivingEntity le) || !le.isAlive() || aliados(atacante, e)) {
            return false;
        }
        return !(e instanceof Player p) || !(p.isCreative() || p.isSpectator());
    }

    /** El daño de un ataque de {@code quien} (si no hay quién, daño mágico). */
    public static DamageSource fuente(Level l, Entity quien) {
        if (quien instanceof Player p) {
            return l.damageSources().playerAttack(p);
        }
        if (quien instanceof LivingEntity le) {
            return l.damageSources().mobAttack(le);
        }
        return l.damageSources().magic();
    }

    /** Daño de un ataque (con los estados del que lo recibe, como marcado, ya contados por el Core). Devuelve true si le hizo daño. */
    public static boolean danar(LivingEntity e, DamageSource src, float cantidad) {
        return Plat.hurt(e, src, cantidad);
    }

    /** Empuja desde un punto (horizontal {@code fuerza}, hacia arriba {@code arriba}). */
    public static void empujarDesde(Entity e, Vec3 desde, double fuerza, double arriba) {
        Vec3 d = new Vec3(e.getX() - desde.x, 0, e.getZ() - desde.z);
        d = d.lengthSqr() < 1.0E-4 ? new Vec3(0, 0, 0) : d.normalize();
        double k = e instanceof LivingEntity le ? Math.max(0, 1 - le.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.KNOCKBACK_RESISTANCE)) : 1;
        Plat.push(e, d.x * fuerza * k, arriba * k, d.z * fuerza * k);
    }

    /** La altura del suelo bajo un punto (busca de 3 bloques arriba a 8 abajo); si no hay, la del punto. */
    public static double suelo(Level l, Vec3 p) {
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        int bx = (int) Math.floor(p.x), bz = (int) Math.floor(p.z), by = (int) Math.floor(p.y);
        for (int y = by + 3; y >= by - 8; y--) {
            m.set(bx, y, bz);
            BlockState st = l.getBlockState(m);
            var shape = st.getCollisionShape(l, m);
            if (!shape.isEmpty()) {
                double top = y + shape.max(Direction.Axis.Y);
                if (top <= p.y + 3.01) {
                    return top;
                }
            }
        }
        return p.y;
    }

    /** El punto sobre el suelo. */
    public static Vec3 alSuelo(Level l, Vec3 p) {
        return new Vec3(p.x, suelo(l, p), p.z);
    }

    /** Un sonido por su nombre ("minecraft:entity.generic.explode" o "mimod:jefe.rugido"), igual en todas las versiones. */
    public static void sonido(Level l, Vec3 p, String id, float vol, float pitch) {
        if (id == null || id.isEmpty()) {
            return;
        }
        try {
            SoundEvent s = SoundEvent.createVariableRangeEvent(Ids.parse(id));
            l.playSound(null, p.x, p.y, p.z, s, SoundSource.HOSTILE, vol, pitch);
        } catch (RuntimeException ignored) {
            // un nombre mal escrito no debe tirar el juego
        }
    }

    /**
     * Partículas por nombre: "explosion", "flame", "soul", "snow", "crit", "magic", "smoke", "cloud", "spark", "end_rod", "lava",
     * "splash", "heart" o "" (ninguna).
     */
    public static ParticleOptions particula(String n) {
        return switch (n == null ? "" : n) {
            case "explosion" -> ParticleTypes.EXPLOSION;
            case "flame" -> ParticleTypes.FLAME;
            case "soul" -> ParticleTypes.SOUL_FIRE_FLAME;
            case "snow" -> ParticleTypes.SNOWFLAKE;
            case "crit" -> ParticleTypes.CRIT;
            case "magic" -> ParticleTypes.ENCHANTED_HIT;
            case "smoke" -> ParticleTypes.LARGE_SMOKE;
            case "cloud" -> ParticleTypes.CLOUD;
            case "spark" -> ParticleTypes.ELECTRIC_SPARK;
            case "end_rod" -> ParticleTypes.END_ROD;
            case "lava" -> ParticleTypes.LAVA;
            case "splash" -> ParticleTypes.SPLASH;
            case "heart" -> ParticleTypes.HEART;
            default -> null;
        };
    }

    /** Lanza partículas desde el servidor (las ven todos los que estén cerca). */
    public static void particulas(Level l, String n, Vec3 p, int cuantas, double dx, double dy, double dz, double vel) {
        ParticleOptions o = particula(n);
        if (o != null && l instanceof ServerLevel sl) {
            sl.sendParticles(o, p.x, p.y, p.z, cuantas, dx, dy, dz, vel);
        }
    }

    /** Partículas en un anillo en el suelo (para el golpe de un aviso redondo). */
    public static void anillo(Level l, String n, Vec3 c, double r, int cuantas) {
        ParticleOptions o = particula(n);
        if (o == null || !(l instanceof ServerLevel sl)) {
            return;
        }
        for (int i = 0; i < cuantas; i++) {
            double a = i * Math.PI * 2 / cuantas;
            sl.sendParticles(o, c.x + Math.cos(a) * r, c.y + 0.15, c.z + Math.sin(a) * r, 1, 0.05, 0.05, 0.05, 0.02);
        }
    }

    /** Dirección "adelante" de un ángulo de giro de Minecraft (0 = sur). */
    public static Vec3 adelante(float yaw) {
        double r = Math.toRadians(yaw);
        return new Vec3(-Math.sin(r), 0, Math.cos(r));
    }

    /** El ángulo de giro (de Minecraft) para mirar de un punto a otro. */
    public static float giro(Vec3 desde, Vec3 hacia) {
        return (float) Math.toDegrees(Math.atan2(-(hacia.x - desde.x), hacia.z - desde.z));
    }
}
