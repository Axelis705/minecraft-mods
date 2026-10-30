package com.scardax.rules;

import com.scardax.Scardax;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Aplica las reglas: cada vez que un mob va a aparecer solo (de forma natural, al generar el terreno, en estructuras, en spawners...)
 * se decide si aparece. Los mobs que invoca un jugador (huevos, comandos, crías...) nunca se tocan.
 */
@Mod.EventBusSubscriber(modid = Scardax.MODID)
public final class SpawnHandler {
    /** Evita que los mobs extra que creamos vuelvan a multiplicarse. */
    private static boolean making = false;
    private static final String PASSED = "scardax_passed";

    private SpawnHandler() {
    }

    /** Los tipos de aparición que dependen del mundo (no de un jugador). */
    private static boolean affected(MobSpawnType t) {
        return switch (t) {
            case NATURAL, CHUNK_GENERATION, STRUCTURE, SPAWNER, JOCKEY, PATROL, REINFORCEMENT -> true;
            default -> false;
        };
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent e) {
        SpawnRules.of(e.getServer());
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent e) {
        SpawnRules.clearActive();
    }

    /**
     * Modo estricto (se activa en la pantalla): algunos mods meten mobs al mundo sin pasar por el evento normal de spawneo. Con el modo
     * estricto, todo mob nuevo que entre al mundo también pasa por la regla. Ojo: también afecta a huevos, comandos y crías de los
     * mobs que estén por debajo de 100 %. Los mobs ya guardados en el mundo no se tocan (para eso está el botón de limpiar).
     */
    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent e) {
        if (e.getLevel().isClientSide() || e.loadedFromDisk() || !(e.getEntity() instanceof Mob mob) || !SpawnRules.strictNow()
                || mob.getTags().contains(PASSED)) {
            return;
        }
        int pct = SpawnRules.percentOf(ForgeRegistries.ENTITY_TYPES.getKey(mob.getType()));
        if (pct < 100 && e.getLevel().random.nextInt(100) >= pct) {
            e.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onFinalizeSpawn(MobSpawnEvent.FinalizeSpawn e) {
        if (!affected(e.getSpawnType())) {
            return;
        }
        Mob mob = e.getEntity();
        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(mob.getType());
        int pct = SpawnRules.percentOf(id);
        if (pct == 100) {
            return;
        }
        RandomSource rnd = e.getLevel().getRandom();
        if (pct <= 0) {
            e.setSpawnCancelled(true);
            return;
        }
        if (pct < 100) {
            if (rnd.nextInt(100) >= pct) {
                e.setSpawnCancelled(true);
            } else {
                mob.addTag(PASSED); // ya tuvo su oportunidad: el modo estricto no lo vuelve a tirar
            }
            return;
        }
        // más de 100: solo en apariciones naturales, se intentan crear mobs de más junto a este
        if (making || e.getSpawnType() != MobSpawnType.NATURAL) {
            return;
        }
        double extra = (pct - 100) / 100.0;
        int n = (int) extra + (rnd.nextDouble() < extra - (int) extra ? 1 : 0);
        if (n <= 0) {
            return;
        }
        ServerLevel level = e.getLevel().getLevel();
        making = true;
        try {
            for (int i = 0; i < n; i++) {
                spawnNear(level, mob.getType(), e.getX(), e.getY(), e.getZ(), rnd);
            }
        } finally {
            making = false;
        }
    }

    private static void spawnNear(ServerLevel level, EntityType<?> type, double x, double y, double z, RandomSource rnd) {
        for (int tries = 0; tries < 4; tries++) {
            BlockPos p = BlockPos.containing(x + rnd.nextInt(7) - 3, y, z + rnd.nextInt(7) - 3);
            if (canSpawn(type, level, p, rnd)) {
                type.spawn(level, p, MobSpawnType.NATURAL);
                return;
            }
        }
    }

    private static <T extends Entity> boolean canSpawn(EntityType<T> type, ServerLevel level, BlockPos p, RandomSource rnd) {
        return SpawnPlacements.checkSpawnRules(type, level, MobSpawnType.NATURAL, p, rnd);
    }
}
