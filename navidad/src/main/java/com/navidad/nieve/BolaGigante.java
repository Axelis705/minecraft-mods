package com.navidad.nieve;

import com.navidad.registry.ModRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.SnowGolem;
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * La bola gigante del lanzanieves: vuela en arco y al pegar revienta en una nevada: empuja y escarcha todo lo que está cerca (a los
 * mobs malos les duele), apaga el fuego y deja el piso nevado alrededor.
 */
public class BolaGigante extends ThrowableItemProjectile {
    public BolaGigante(EntityType<? extends BolaGigante> t, Level l) {
        super(t, l);
    }

    public BolaGigante(Level l, LivingEntity quien) {
        super(ModRegistry.BOLA_GIGANTE.get(), quien, l);
    }

    @Override
    protected Item getDefaultItem() {
        return Items.SNOWBALL;
    }

    @Override
    protected float getGravity() {
        return 0.035F;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide && tickCount % 2 == 0) {
            level().addParticle(ParticleTypes.SNOWFLAKE, getX(), getY() + 0.2, getZ(), 0, 0, 0);
        }
    }

    @Override
    protected void onHit(HitResult r) {
        super.onHit(r);
        if (!(level() instanceof ServerLevel l)) {
            return;
        }
        Vec3 c = r.getLocation();
        l.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(Items.SNOWBALL)), c.x, c.y + 0.3, c.z, 50, 0.6, 0.4, 0.6, 0.25);
        l.sendParticles(ParticleTypes.SNOWFLAKE, c.x, c.y + 0.5, c.z, 60, 1.4, 0.8, 1.4, 0.08);
        l.sendParticles(ParticleTypes.POOF, c.x, c.y + 0.3, c.z, 12, 0.6, 0.3, 0.6, 0.02);
        l.playSound(null, BlockPos.containing(c), com.navidad.registry.ModRegistry.LANZANIEVES_IMPACTO.get(), SoundSource.PLAYERS, 1.6F, 1F);
        l.playSound(null, BlockPos.containing(c), SoundEvents.SNOW_BREAK, SoundSource.PLAYERS, 1.2F, 0.6F);
        l.playSound(null, BlockPos.containing(c), SoundEvents.POWDER_SNOW_BREAK, SoundSource.PLAYERS, 1.4F, 0.8F);
        Entity quien = getOwner();
        for (Entity e : l.getEntities(this, new AABB(c, c).inflate(3.5))) {
            if (e == quien || e instanceof SnowGolem || e instanceof TamableAnimal t && quien instanceof LivingEntity le && t.isOwnedBy(le)) {
                continue;
            }
            Vec3 d = e.position().subtract(c);
            double dist = Math.max(0.5, d.length());
            if (dist > 3.5) {
                continue;
            }
            double fuerza = (3.6 - dist) * 0.35;
            Vec3 emp = d.normalize().scale(fuerza).add(0, 0.25 + fuerza * 0.3, 0);
            e.setDeltaMovement(e.getDeltaMovement().add(emp));
            e.hurtMarked = true;
            float dano = e instanceof Blaze ? 8F : e instanceof Enemy ? 4F : 0F;
            if (dano > 0) {
                e.hurt(damageSources().thrown(this, quien), dano);
            }
            if (e instanceof LivingEntity vivo && vivo.canFreeze()) {
                vivo.setTicksFrozen(Math.max(vivo.getTicksFrozen(), vivo.getTicksRequiredToFreeze() - 8));
            }
        }
        // apaga el fuego y deja nieve alrededor
        BlockPos centro = BlockPos.containing(c);
        if (!l.dimensionType().ultraWarm()) {
            for (BlockPos q : BlockPos.betweenClosed(centro.offset(-2, -2, -2), centro.offset(2, 2, 2))) {
                if (q.distSqr(centro) > 6.5) {
                    continue;
                }
                if (l.getBlockState(q).getBlock() instanceof BaseFireBlock) {
                    l.removeBlock(q, false);
                } else if (l.getBlockState(q).isAir() && random.nextFloat() < 0.6F && Blocks.SNOW.defaultBlockState().canSurvive(l, q)) {
                    l.setBlock(q, Blocks.SNOW.defaultBlockState(), 3);
                }
            }
        }
        discard();
    }
}
