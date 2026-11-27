package com.navidad.nieve;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.SnowGolem;
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.projectile.Snowball;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;

/**
 * La bola de nieve del lanzanieves (se ve como una bola de nieve normal). A los mobs malos les hace un poquito de daño, a los blazes
 * bastante; a todos los empuja y los va escarchando (se alentan, como en la nieve polvo, pero sin llegar a congelarse). A las personas y
 * animales solo los empuja; a tus mascotas ni eso. Apaga el fuego y a veces deja nieve donde pega.
 */
public class BolaNieve extends Snowball {
    public BolaNieve(Level l, LivingEntity quien) {
        super(l, quien);
    }

    @Override
    protected void onHitEntity(EntityHitResult r) {
        Entity e = r.getEntity();
        Entity quien = getOwner();
        if (e instanceof SnowGolem || e instanceof TamableAnimal t && quien instanceof LivingEntity le && t.isOwnedBy(le)) {
            return;
        }
        float dano = e instanceof Blaze ? 4F : e instanceof Enemy ? 1.5F : 0F;
        e.hurt(damageSources().thrown(this, quien), dano);
        if (e instanceof LivingEntity vivo && vivo.canFreeze()) {
            vivo.setTicksFrozen(Math.min(vivo.getTicksRequiredToFreeze() - 8, vivo.getTicksFrozen() + 16));
        }
    }

    @Override
    protected void onHitBlock(BlockHitResult r) {
        super.onHitBlock(r);
        Level l = level();
        if (l.isClientSide) {
            return;
        }
        BlockPos p = r.getBlockPos();
        BlockPos frente = p.relative(r.getDirection());
        BlockState f = l.getBlockState(frente);
        if (f.getBlock() instanceof BaseFireBlock) {
            l.removeBlock(frente, false);
            return;
        }
        // a veces queda una capita de nieve donde pegó (arriba de algo firme, en el mundo normal)
        if (r.getDirection() == Direction.UP && f.isAir() && random.nextFloat() < 0.07F && !l.dimensionType().ultraWarm()
                && Blocks.SNOW.defaultBlockState().canSurvive(l, frente)) {
            l.setBlock(frente, Blocks.SNOW.defaultBlockState(), 3);
        }
    }
}
