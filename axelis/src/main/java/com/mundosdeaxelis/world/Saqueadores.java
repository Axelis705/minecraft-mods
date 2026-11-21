package com.mundosdeaxelis.world;

import com.mundosdeaxelis.MundosDeAxelis;
import com.mundosdeaxelis.entity.Saqueador;
import com.mundosdeaxelis.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Etapa 276: cuándo aparece un {@link Saqueador}. Cada 30 segundos, a cada jugador (en supervivencia) que está dentro de una
 * estructura que ya puede saquear le puede salir uno (4 %), a 8–14 bloques, en un sitio pisable de la misma estructura. Después,
 * a ese jugador no le sale otro en 8 minutos. Así sale, de media, uno cada 12 o 15 minutos de exploración.
 */
@Mod.EventBusSubscriber(modid = MundosDeAxelis.MODID)
public final class Saqueadores {
    private Saqueadores() {
    }

    public static final int EVERY = 20 * 30;
    public static final int CHANCE = 4;
    public static final long COOLDOWN = 20L * 60 * 8;
    private static final Map<UUID, Long> LAST = new HashMap<>();

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent e) {
        if (e.phase != TickEvent.Phase.END) {
            return;
        }
        MinecraftServer server = e.getServer();
        if (server.getTickCount() % EVERY != 17) {
            return;
        }
        long now = server.getTickCount();
        for (ServerPlayer sp : server.getPlayerList().getPlayers()) {
            if (sp.isCreative() || sp.isSpectator()) {
                continue;
            }
            Long last = LAST.get(sp.getUUID());
            if (last != null && now - last < COOLDOWN) {
                continue;
            }
            ServerLevel sl = sp.serverLevel();
            int tier = Sellos.tierAt(sl, sp.blockPosition());
            if (tier <= 0 || !Sellos.canLoot(sp, tier) || sl.random.nextInt(100) >= CHANCE) {
                continue;
            }
            if (spawnNear(sl, sp, tier) != null) {
                LAST.put(sp.getUUID(), now);
            }
        }
    }

    /** Pone un Saqueador de ese tier cerca del jugador (en la misma estructura); null si no encontró sitio. */
    @Nullable
    public static Saqueador spawnNear(ServerLevel sl, ServerPlayer sp, int tier) {
        RandomSource r = sl.random;
        BlockPos at = sp.blockPosition();
        for (int tries = 0; tries < 24; tries++) {
            double ang = r.nextDouble() * Math.PI * 2;
            int d = 8 + r.nextInt(7);
            int x = at.getX() + (int) Math.round(Math.cos(ang) * d), z = at.getZ() + (int) Math.round(Math.sin(ang) * d);
            for (int dy = 3; dy >= -3; dy--) {
                BlockPos p = new BlockPos(x, at.getY() + dy, z);
                if (sl.getBlockState(p).getCollisionShape(sl, p).isEmpty() && sl.getBlockState(p.above()).getCollisionShape(sl, p.above()).isEmpty()
                        && sl.getBlockState(p.below()).isFaceSturdy(sl, p.below(), net.minecraft.core.Direction.UP)
                        && sl.getFluidState(p).isEmpty() && Sellos.tierAt(sl, p) == tier) {
                    Saqueador s = ModEntities.SAQUEADOR.get().create(sl);
                    if (s == null) {
                        return null;
                    }
                    s.moveTo(x + 0.5, p.getY(), z + 0.5, r.nextFloat() * 360.0F, 0.0F);
                    s.finalizeSpawn(sl, sl.getCurrentDifficultyAt(p), MobSpawnType.EVENT, null, null);
                    s.setTier(tier);
                    sl.addFreshEntity(s);
                    return s;
                }
            }
        }
        return null;
    }

    public static void clearRuntime() {
        LAST.clear();
    }
}
