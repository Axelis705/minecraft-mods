package com.mundosdeaxelis.world;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.AABB;

/**
 * Al construir o borrar una dungeon, algunas plantas y linternas se quedan sin apoyo y "se rompen" soltando
 * objetos (flores, semillas, bayas, linternas). Esto limpia esos objetos sueltos que salieron durante la obra.
 */
public final class DungeonCleanup {
    private DungeonCleanup() {
    }

    /**
     * Borra los objetos del suelo dentro de la caja que aparecieron DESPUÉS de empezar la obra
     * (los que ya existían antes, por ejemplo cosas que soltó un jugador, no se tocan).
     */
    public static void purge(ServerLevel level, int x0, int y0, int z0, int x1, int y1, int z1, long startTick) {
        long elapsed = Math.max(0L, level.getGameTime() - startTick);
        AABB box = new AABB(x0, y0, z0, x1 + 1, y1 + 1, z1 + 1);
        for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, box)) {
            if (item.tickCount <= elapsed + 10) {
                item.discard();
            }
        }
    }
}
