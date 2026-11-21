package com.mundosdeaxelis.arrojadizo;

import com.mundosdeaxelis.world.LoadoutRef;
import com.mundosdeaxelis.world.Scheduler;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Etapa 273: los <b>arrojadizos</b> (como las bombas de Elden Ring o Terraria): uno por tier, se encuentran en los cofres y
 * barriles de su estructura, se lanzan con clic derecho y solo dañan a los enemigos (nunca a quien lo lanza ni a otros
 * jugadores, mascotas o habitantes). El daño sigue el golpe de referencia del jugador de ese tier ({@link LoadoutRef#hit}).
 * <ol>
 *     <li><b>Bomba de Esporas</b> (bosque): una nube verde que envenena y daña poco a poco durante 5 s.</li>
 *     <li><b>Granada de Hierro</b> (forja): explota y empuja (no rompe bloques).</li>
 *     <li><b>Orbe de Escarcha</b> (hielo): congela y deja muy lentos a los enemigos.</li>
 *     <li><b>Bomba Volcánica</b> (volcán): una erupción que los lanza por el aire; sirve también contra los que no arden.</li>
 *     <li><b>Orbe Arcano</b> (magia): un remolino que atrae a los enemigos al centro y luego estalla.</li>
 * </ol>
 */
public final class Arrojadizos {
    private Arrojadizos() {
    }

    public enum Tipo {
        ESPORAS("bomba_esporas", 1, 0x7BC74A),
        HIERRO("granada_hierro", 2, 0xFF9A3A),
        ESCARCHA("orbe_escarcha", 3, 0x9FE6FF),
        VOLCAN("bomba_volcanica", 4, 0xFF5A1E),
        ARCANO("orbe_arcano", 5, 0xC060FF);

        public final String id;
        public final int tier;
        public final int color;

        Tipo(String id, int tier, int color) {
            this.id = id;
            this.tier = tier;
            this.color = color;
        }

        public static Tipo ofTier(int t) {
            Tipo[] v = values();
            return v[Math.max(0, Math.min(v.length - 1, t - 1))];
        }

        public DustParticleOptions dust(float size) {
            return new DustParticleOptions(new Vector3f(((color >> 16) & 255) / 255.0F, ((color >> 8) & 255) / 255.0F, (color & 255) / 255.0F), size);
        }
    }

    /** ¿Le afecta? Solo enemigos: ni quien lo lanzó, ni jugadores, ni mascotas domadas, ni habitantes, ni soportes. */
    public static boolean foe(LivingEntity e, @Nullable Entity owner) {
        if (!e.isAlive() || e == owner || e instanceof Player || e instanceof ArmorStand || e instanceof com.mundosdeaxelis.entity.Habitante
                || e instanceof com.mundosdeaxelis.entity.Mascota || (e instanceof TamableAnimal t && t.isTame())) {
            return false;
        }
        return e instanceof Enemy || (e instanceof Mob m && owner != null && m.getTarget() == owner);
    }

    static List<LivingEntity> foes(ServerLevel sl, Vec3 c, double r, @Nullable Entity owner) {
        return sl.getEntitiesOfClass(LivingEntity.class, new AABB(c, c).inflate(r), e -> foe(e, owner) && e.distanceToSqr(c) <= r * r);
    }

    /** Daño de referencia: un golpe de espada del jugador en ese tier. */
    public static float hit(int tier) {
        return LoadoutRef.hit(tier);
    }

    /** Lo que pasa donde cae. */
    public static void burst(ServerLevel sl, Vec3 c, Tipo t, @Nullable Entity owner, @Nullable Entity projectile) {
        float h = hit(t.tier);
        Entity src = projectile != null ? projectile : owner;
        switch (t) {
            case ESPORAS -> spores(sl, c, h, owner, src);
            case HIERRO -> grenade(sl, c, h, owner, src);
            case ESCARCHA -> frost(sl, c, h, owner, src);
            case VOLCAN -> volcano(sl, c, h, owner, src);
            case ARCANO -> arcane(sl, c, h, owner, src);
        }
    }

    private static DamageSource dmg(ServerLevel sl, @Nullable Entity src, @Nullable Entity owner) {
        return src != null ? sl.damageSources().thrown(src, owner) : sl.damageSources().magic();
    }

    private static DamageSource magic(ServerLevel sl, @Nullable Entity src, @Nullable Entity owner) {
        return src != null ? sl.damageSources().indirectMagic(src, owner) : sl.damageSources().magic();
    }

    // ------------------------------------------------------------------ 1. esporas: nube venenosa

    private static void spores(ServerLevel sl, Vec3 c, float h, @Nullable Entity owner, @Nullable Entity src) {
        sl.playSound(null, c.x, c.y, c.z, SoundEvents.PUFFER_FISH_BLOW_UP, SoundSource.PLAYERS, 1.0F, 0.7F);
        sl.playSound(null, c.x, c.y, c.z, SoundEvents.SLIME_SQUISH, SoundSource.PLAYERS, 0.8F, 0.6F);
        Tipo t = Tipo.ESPORAS;
        for (int i = 0; i <= 10; i++) {
            final int k = i;
            Scheduler.runLater(i * 10, () -> {
                sl.sendParticles(t.dust(1.6F), c.x, c.y + 0.6, c.z, 26, 1.6, 0.6, 1.6, 0.0);
                sl.sendParticles(ParticleTypes.SPORE_BLOSSOM_AIR, c.x, c.y + 0.8, c.z, 12, 1.6, 0.6, 1.6, 0.0);
                if (k == 0) {
                    sl.sendParticles(ParticleTypes.SNEEZE, c.x, c.y + 0.4, c.z, 20, 0.4, 0.3, 0.4, 0.08);
                }
                for (LivingEntity e : foes(sl, c, 3.0D, owner)) {
                    e.addEffect(new MobEffectInstance(MobEffects.POISON, 60, 1), owner);
                    e.hurt(magic(sl, src, owner), h * 0.12F);
                }
            });
        }
    }

    // ------------------------------------------------------------------ 2. granada de hierro: explosión que empuja

    private static void grenade(ServerLevel sl, Vec3 c, float h, @Nullable Entity owner, @Nullable Entity src) {
        sl.playSound(null, c.x, c.y, c.z, SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 1.1F, 1.2F);
        sl.sendParticles(ParticleTypes.EXPLOSION, c.x, c.y + 0.3, c.z, 3, 0.6, 0.3, 0.6, 0.0);
        sl.sendParticles(ParticleTypes.LAVA, c.x, c.y + 0.3, c.z, 10, 0.4, 0.2, 0.4, 0.0);
        sl.sendParticles(ParticleTypes.LARGE_SMOKE, c.x, c.y + 0.4, c.z, 16, 0.8, 0.4, 0.8, 0.03);
        sl.sendParticles(Tipo.HIERRO.dust(1.2F), c.x, c.y + 0.4, c.z, 30, 1.2, 0.5, 1.2, 0.0);
        double r = 3.5D;
        for (LivingEntity e : foes(sl, c, r, owner)) {
            double d = Math.sqrt(e.distanceToSqr(c));
            float f = (float) (1.0D - 0.5D * d / r);
            e.hurt(sl.damageSources().explosion(src, owner), h * 2.0F * f);
            Vec3 k = e.position().subtract(c).multiply(1, 0, 1);
            k = k.lengthSqr() < 1.0E-4 ? Vec3.ZERO : k.normalize();
            double kr = 1.0D - e.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.KNOCKBACK_RESISTANCE);
            e.push(k.x * 0.9D * kr, 0.35D * kr, k.z * 0.9D * kr);
            e.hurtMarked = true;
        }
    }

    // ------------------------------------------------------------------ 3. escarcha: congela

    private static void frost(ServerLevel sl, Vec3 c, float h, @Nullable Entity owner, @Nullable Entity src) {
        sl.playSound(null, c.x, c.y, c.z, SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 1.0F, 1.4F);
        sl.playSound(null, c.x, c.y, c.z, SoundEvents.POWDER_SNOW_BREAK, SoundSource.PLAYERS, 1.0F, 0.8F);
        sl.sendParticles(ParticleTypes.SNOWFLAKE, c.x, c.y + 0.5, c.z, 60, 1.5, 0.6, 1.5, 0.04);
        sl.sendParticles(ParticleTypes.ITEM_SNOWBALL, c.x, c.y + 0.4, c.z, 24, 0.6, 0.3, 0.6, 0.1);
        sl.sendParticles(Tipo.ESCARCHA.dust(1.4F), c.x, c.y + 0.4, c.z, 30, 1.5, 0.4, 1.5, 0.0);
        for (LivingEntity e : foes(sl, c, 3.2D, owner)) {
            e.hurt(magic(sl, src, owner), h * 0.9F);
            e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 120, 3), owner);
            e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 120, 0), owner);
            e.setTicksFrozen(Math.max(e.getTicksFrozen(), 300));
            sl.sendParticles(ParticleTypes.SNOWFLAKE, e.getX(), e.getY() + e.getBbHeight() * 0.6, e.getZ(), 10, 0.3, 0.4, 0.3, 0.01);
        }
    }

    // ------------------------------------------------------------------ 4. volcánica: erupción que lanza por el aire

    private static void volcano(ServerLevel sl, Vec3 c, float h, @Nullable Entity owner, @Nullable Entity src) {
        sl.playSound(null, c.x, c.y, c.z, SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 1.3F, 0.7F);
        sl.playSound(null, c.x, c.y, c.z, SoundEvents.LAVA_POP, SoundSource.PLAYERS, 1.0F, 0.8F);
        sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER, c.x, c.y + 0.2, c.z, 1, 0.0, 0.0, 0.0, 0.0);
        sl.sendParticles(ParticleTypes.LAVA, c.x, c.y + 0.3, c.z, 30, 0.8, 0.3, 0.8, 0.0);
        sl.sendParticles(ParticleTypes.FLAME, c.x, c.y + 0.5, c.z, 40, 0.3, 1.5, 0.3, 0.08);
        sl.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, c.x, c.y + 0.5, c.z, 10, 0.5, 0.4, 0.5, 0.05);
        for (LivingEntity e : foes(sl, c, 4.0D, owner)) {
            double d = Math.sqrt(e.distanceToSqr(c));
            float f = (float) (1.0D - 0.4D * d / 4.0D);
            e.hurt(sl.damageSources().explosion(src, owner), h * 2.4F * f);
            if (!e.fireImmune()) {
                e.setSecondsOnFire(5);
            }
            double kr = 1.0D - e.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.KNOCKBACK_RESISTANCE);
            e.push(0.0D, 0.9D * Math.max(0.25D, kr), 0.0D);
            e.hurtMarked = true;
        }
        for (int i = 1; i <= 3; i++) {
            Scheduler.runLater(i * 6, () -> sl.sendParticles(ParticleTypes.LAVA, c.x, c.y + 0.5, c.z, 8, 1.0, 0.3, 1.0, 0.0));
        }
    }

    // ------------------------------------------------------------------ 5. arcano: remolino y estallido

    private static void arcane(ServerLevel sl, Vec3 c, float h, @Nullable Entity owner, @Nullable Entity src) {
        sl.playSound(null, c.x, c.y, c.z, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0F, 0.6F);
        sl.playSound(null, c.x, c.y, c.z, SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.8F, 1.8F);
        Vec3 mid = c.add(0, 0.6, 0);
        for (int i = 0; i < 6; i++) {
            final int k = i;
            Scheduler.runLater(i * 5, () -> {
                double rr = 3.0D - k * 0.4D;
                for (int a = 0; a < 16; a++) {
                    double ang = a * Math.PI / 8 + k * 0.6;
                    sl.sendParticles(ParticleTypes.PORTAL, mid.x + Math.cos(ang) * rr, mid.y, mid.z + Math.sin(ang) * rr, 1, 0, 0, 0, 0.0);
                }
                sl.sendParticles(Tipo.ARCANO.dust(1.5F), mid.x, mid.y, mid.z, 10, 0.3, 0.3, 0.3, 0.0);
                for (LivingEntity e : foes(sl, c, 5.0D, owner)) {
                    Vec3 p = mid.subtract(e.position().add(0, e.getBbHeight() * 0.5, 0));
                    if (p.lengthSqr() > 0.5D) {
                        Vec3 v = p.normalize().scale(0.32D);
                        e.setDeltaMovement(e.getDeltaMovement().scale(0.5D).add(v.x, v.y * 0.5D + 0.02D, v.z));
                        e.hurtMarked = true;
                    }
                }
            });
        }
        Scheduler.runLater(32, () -> {
            sl.playSound(null, mid.x, mid.y, mid.z, SoundEvents.AMETHYST_BLOCK_BREAK, SoundSource.PLAYERS, 1.2F, 0.6F);
            sl.playSound(null, mid.x, mid.y, mid.z, SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 0.7F, 1.6F);
            sl.sendParticles(ParticleTypes.END_ROD, mid.x, mid.y, mid.z, 40, 0.2, 0.2, 0.2, 0.25);
            sl.sendParticles(ParticleTypes.WITCH, mid.x, mid.y, mid.z, 40, 1.2, 0.6, 1.2, 0.1);
            sl.sendParticles(Tipo.ARCANO.dust(2.0F), mid.x, mid.y, mid.z, 40, 1.5, 0.6, 1.5, 0.0);
            for (LivingEntity e : foes(sl, c, 3.0D, owner)) {
                e.hurt(magic(sl, src, owner), h * 2.6F);
                Vec3 k = e.position().subtract(c).multiply(1, 0, 1);
                k = k.lengthSqr() < 1.0E-4 ? Vec3.ZERO : k.normalize();
                e.push(k.x * 0.5D, 0.3D, k.z * 0.5D);
                e.hurtMarked = true;
            }
        });
    }
}
