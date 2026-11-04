package com.mundosdeaxelis.world;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

/** Recuerda con qué semilla y cuántos jugadores se construyó la última dungeon, para poder borrarla al hacer una nueva. */
public class DungeonSave extends SavedData {
    public boolean valid = false;
    public long seed = 0L;
    public int players = 1;
    /** Si la dungeon se construyó con el plano grande (la Forja); las antiguas no lo tienen. */
    public boolean big = false;

    public static DungeonSave get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(DungeonSave::load, DungeonSave::new, "mdc_forest_dungeon");
    }

    public static DungeonSave load(CompoundTag tag) {
        DungeonSave s = new DungeonSave();
        s.valid = tag.getBoolean("Valid");
        s.seed = tag.getLong("Seed");
        s.players = Math.max(1, tag.getInt("Players"));
        s.big = tag.getBoolean("Big");
        return s;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putBoolean("Valid", valid);
        tag.putLong("Seed", seed);
        tag.putInt("Players", players);
        tag.putBoolean("Big", big);
        return tag;
    }
}
