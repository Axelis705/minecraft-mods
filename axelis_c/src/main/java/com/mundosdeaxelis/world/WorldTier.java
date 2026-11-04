package com.mundosdeaxelis.world;

import com.mundosdeaxelis.MundosDeAxelis;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * Dificultad global del mundo: sube con cada jefe de etapa vencido (0 = ninguno, 8 = los ocho). Se guarda con el mundo y
 * afecta a los monstruos de Minecraft (más vida y daño, y aparecen otros más fuertes). Ver {@link WorldTierEvents}.
 */
public class WorldTier extends SavedData {
    public static final int MAX = 8;
    private static final String NAME = MundosDeAxelis.MODID + "_world_tier";

    private int level = 0;

    public static WorldTier get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(WorldTier::load, WorldTier::new, NAME);
    }

    private static WorldTier load(CompoundTag tag) {
        WorldTier t = new WorldTier();
        t.level = Math.max(0, Math.min(MAX, tag.getInt("level")));
        return t;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putInt("level", level);
        return tag;
    }

    public int level() {
        return level;
    }

    /** Fija el nivel (para pruebas o al vencer un jefe); devuelve true si cambió. */
    public boolean set(int lv) {
        lv = Math.max(0, Math.min(MAX, lv));
        if (lv == level) {
            return false;
        }
        level = lv;
        setDirty();
        return true;
    }

    /** Un jefe de la etapa {@code stage} (1..8) fue vencido: el mundo sube hasta ese nivel, nunca baja. */
    public static void bossDefeated(MinecraftServer server, int stage) {
        WorldTier t = get(server);
        if (stage > t.level) {
            t.set(stage);
            WorldTierEvents.announce(server, stage);
        }
        // cada jugador conectado registra el avance en su Archivo (abre la caja siguiente)
        for (net.minecraft.server.level.ServerPlayer sp : server.getPlayerList().getPlayers()) {
            com.mundosdeaxelis.table.Essence.registerProgress(sp, stage);
        }
    }
}
