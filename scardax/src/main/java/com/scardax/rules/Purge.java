package com.scardax.rules;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;

/** Quita de los mundos los mobs ya cargados cuyo tipo está en 0 %. No toca a los que tienen nombre, mascotas, atados ni montados. */
public final class Purge {
    private Purge() {
    }

    public static int run(MinecraftServer server) {
        SpawnRules rules = SpawnRules.of(server);
        int removed = 0;
        for (ServerLevel level : server.getAllLevels()) {
            List<Entity> kill = new ArrayList<>();
            for (Entity e : level.getAllEntities()) {
                if (!(e instanceof Mob m) || protectedMob(m)) {
                    continue;
                }
                ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(m.getType());
                if (id != null && rules.get(id) == 0) {
                    kill.add(m);
                }
            }
            for (Entity e : kill) {
                e.discard();
                removed++;
            }
        }
        return removed;
    }

    private static boolean protectedMob(Mob m) {
        return m.hasCustomName() || m.isPersistenceRequired() || m.isLeashed() || m.isVehicle() || m.isPassenger()
                || (m instanceof TamableAnimal t && t.isTame());
    }
}
