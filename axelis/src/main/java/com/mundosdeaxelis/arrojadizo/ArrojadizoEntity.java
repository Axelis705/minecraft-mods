package com.mundosdeaxelis.arrojadizo;

import com.mundosdeaxelis.registry.ModEntities;
import com.mundosdeaxelis.registry.ModItems;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** El arrojadizo en vuelo: deja una estela de su color y, al tocar algo, hace su efecto (ver {@link Arrojadizos}). */
public class ArrojadizoEntity extends ThrowableItemProjectile {
    public ArrojadizoEntity(EntityType<? extends ArrojadizoEntity> type, Level level) {
        super(type, level);
    }

    public ArrojadizoEntity(Level level, LivingEntity owner) {
        super(ModEntities.ARROJADIZO.get(), owner, level);
    }

    @Override
    protected Item getDefaultItem() {
        return ModItems.ARROJADIZOS.get(Arrojadizos.Tipo.ESPORAS).get();
    }

    public Arrojadizos.Tipo tipo() {
        return getItem().getItem() instanceof ArrojadizoItem ai ? ai.tipo : Arrojadizos.Tipo.ESPORAS;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            Arrojadizos.Tipo t = tipo();
            level().addParticle(t.dust(0.9F), getX(), getY() + 0.1, getZ(), 0, 0, 0);
            switch (t) {
                case ESPORAS -> level().addParticle(ParticleTypes.SPORE_BLOSSOM_AIR, getX(), getY(), getZ(), 0, 0, 0);
                case HIERRO, VOLCAN -> level().addParticle(tickCount % 2 == 0 ? ParticleTypes.SMOKE : ParticleTypes.SMALL_FLAME, getX(), getY() + 0.15, getZ(), 0, 0.01, 0);
                case ESCARCHA -> level().addParticle(ParticleTypes.SNOWFLAKE, getX(), getY(), getZ(), 0, 0, 0);
                case ARCANO -> level().addParticle(ParticleTypes.PORTAL, getX(), getY(), getZ(), 0, 0, 0);
            }
        } else if (tickCount > 200) {
            discard();
        }
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (level() instanceof ServerLevel sl && !isRemoved()) {
            Vec3 at = result.getType() == HitResult.Type.ENTITY ? ((EntityHitResult) result).getEntity().position() : result.getLocation();
            Arrojadizos.burst(sl, at, tipo(), getOwner(), this);
            discard();
        }
    }
}
