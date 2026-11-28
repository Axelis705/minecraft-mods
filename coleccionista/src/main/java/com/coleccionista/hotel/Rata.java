package com.coleccionista.hotel;

import com.axeliscore.anim.AnimHost;
import com.axeliscore.anim.Geo;
import com.axeliscore.anim.Pose;
import com.axeliscore.character.CoreMob;
import com.coleccionista.Coleccionista;
import com.coleccionista.registry.ModRegistry;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;

/**
 * La rata de cuerda: un ratón de juguete de hojalata, rápido y molesto. Sale de su nido, corre hacia quien esté cerca y muerde (poquito).
 * Cuando se le acaba la cuerda se queda quieta un rato (la llave de la espalda deja de girar) y luego sigue. Se le puede pegar: con la
 * mano aguanta varios golpes; con un tubo, uno solo (se voltea y le salta el resorte).
 */
public class Rata extends CoreMob {
    public static float danio = 1.5F;
    /** Cuánto corre antes de que se le acabe la cuerda, y cuánto se queda quieta (ticks). */
    public static int cuerda = 200, descanso = 80;
    private ServerPlayer presa;
    private int corriendo, quieta, ultimaMordida = -100;
    /** El nido del que salió (hotel.rata.N), para saber cuántas hay de cada uno. */
    public int nido = -1;

    public Rata(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        xpReward = 0;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes().add(Attributes.MAX_HEALTH, 5).add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.FOLLOW_RANGE, 16).add(Attributes.ATTACK_DAMAGE, 1.5);
    }

    @Override
    protected void registerGoals() {
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide || isDeadOrDying() || showcasing()) {
            return;
        }
        // la cuerda: corre un rato y se detiene (la llave deja de girar), luego vuelve
        if (quieta > 0) {
            quieta--;
            getNavigation().stop();
            if (quieta == 0) {
                level().playSound(null, blockPosition(), ModRegistry.HOTEL_CUERDA.get(), SoundSource.HOSTILE, 0.7F, 1.4F);
            }
            return;
        }
        if (tickCount % 10 == 0) {
            presa = buscar();
        }
        if (presa == null) {
            corriendo = Math.max(0, corriendo - 2);
            if (tickCount % 60 == 0 && random.nextInt(3) == 0) {
                getNavigation().moveTo(getX() + random.nextInt(7) - 3, getY(), getZ() + random.nextInt(7) - 3, 0.7);
            }
            return;
        }
        if (++corriendo > cuerda) {
            corriendo = 0;
            quieta = descanso;
            getNavigation().stop();
            level().playSound(null, blockPosition(), ModRegistry.HOTEL_CUERDA.get(), SoundSource.HOSTILE, 0.6F, 0.6F);
            return;
        }
        if (tickCount % 6 == 0) {
            getNavigation().moveTo(presa, 1.25);
        }
        getLookControl().setLookAt(presa, 30F, 30F);
        if (distanceToSqr(presa) < 1.3 * 1.3 && tickCount - ultimaMordida > 24) {
            ultimaMordida = tickCount;
            playAction("atacar", 9);
            presa.hurt(damageSources().mobAttack(this), danio);
            level().playSound(null, blockPosition(), ModRegistry.HOTEL_CHILLIDO.get(), SoundSource.HOSTILE, 0.8F, 1.2F + random.nextFloat() * 0.3F);
        }
        if (tickCount % 30 == 0) {
            level().playSound(null, blockPosition(), ModRegistry.HOTEL_CUERDA.get(), SoundSource.HOSTILE, 0.35F, 1.6F);
        }
    }

    private ServerPlayer buscar() {
        if (!(level() instanceof ServerLevel l)) {
            return null;
        }
        ServerPlayer mejor = null;
        double md = 10 * 10;
        for (ServerPlayer p : l.players()) {
            if (!JuegoHotel.presaValida(p) || Math.abs(p.getY() - getY()) > 2.5) {
                continue;
            }
            double d = distanceToSqr(p);
            if (d < md && hasLineOfSight(p)) {
                md = d;
                mejor = p;
            }
        }
        return mejor;
    }

    @Override
    public boolean hurt(DamageSource src, float amount) {
        boolean r = super.hurt(src, amount);
        if (r && level() instanceof ServerLevel l) {
            l.sendParticles(ParticleTypes.CRIT, getX(), getY() + 0.3, getZ(), 6, 0.2, 0.1, 0.2, 0.1);
            if (isDeadOrDying()) {
                Coleccionista.LOGGER.info("COLECCIONISTA_HOTEL rata_muere nido={}", nido);
            }
        }
        return r;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource s) {
        return ModRegistry.HOTEL_CHILLIDO.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.CHAIN_BREAK;
    }

    @Override
    protected float getSoundVolume() {
        return 0.8F;
    }

    @Override
    public boolean removeWhenFarAway(double d) {
        return false;
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    protected void dropAllDeathLoot(DamageSource s) {
    }

    /** ¿Se quedó sin cuerda? (para el cliente: la llave se detiene). */
    public boolean sinCuerda() {
        return getNavigation().isDone() && walkDist - walkDistO < 0.001F;
    }

    // ------------------------------------------------------------------ cómo se dibuja (cliente)

    @Override
    public void axAnimate(AnimHost h, float pt) {
        if (axDeath(h, "action")) {
            return;
        }
        axActions(h, n -> "action");
        axHurt(h, "action");
        axMove(h, 0.2F, 2.6F, 1.8F, 4.5F);
    }

    @Override
    public void axPose(AnimHost h, Pose pose, float pt) {
        Geo geo = h.geo();
        pose.hide(geo.bone("resorte"), deathTime <= 0);
    }
}
