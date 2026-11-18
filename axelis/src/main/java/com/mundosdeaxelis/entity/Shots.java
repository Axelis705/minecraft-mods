package com.mundosdeaxelis.entity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.phys.AABB;

/** Utilidad de los enemigos que disparan: al morir, sus disparos en el aire desaparecen (nada te daña "desde la tumba"). */
public final class Shots {
    private Shots() {
    }

    public static void cancelOwned(ServerLevel sl, Entity owner) {
        AABB box = owner.getBoundingBox().inflate(48.0D);
        for (AbstractArrow a : sl.getEntitiesOfClass(AbstractArrow.class, box, x -> x.getOwner() == owner)) {
            sl.sendParticles(net.minecraft.core.particles.ParticleTypes.SMOKE, a.getX(), a.getY(), a.getZ(), 4, 0.1, 0.1, 0.1, 0.01);
            a.discard();
        }
    }
}
