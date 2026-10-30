package com.scardax.rules;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;

/**
 * Las reglas de spawneo de este mundo: para cada tipo de mob, un porcentaje. 100 = normal (no se guarda), 0 = no aparece nunca,
 * menos de 100 = aparece menos, más de 100 = aparece más (hasta 300). Se guardan con el mundo.
 */
public class SpawnRules extends SavedData {
    public static final int MAX = 300;
    private static final String NAME = "scardax_spawn_rules";

    /** Las reglas del mundo en marcha (null si no hay servidor): así el evento de spawneo no busca en disco. */
    private static SpawnRules active;

    private final Map<ResourceLocation, Integer> percent = new HashMap<>();
    /** Modo estricto: además del evento normal de spawneo, se controla cualquier mob que otro mod meta al mundo por su cuenta. */
    private boolean strict = false;

    public static SpawnRules of(MinecraftServer server) {
        SpawnRules r = server.overworld().getDataStorage().computeIfAbsent(SpawnRules::load, SpawnRules::new, NAME);
        active = r;
        return r;
    }

    public static void clearActive() {
        active = null;
    }

    /** Porcentaje de este mob en el mundo en marcha (100 si no hay regla). */
    public static int percentOf(ResourceLocation id) {
        SpawnRules r = active;
        return r == null || id == null ? 100 : r.percent.getOrDefault(id, 100);
    }

    public static boolean strictNow() {
        SpawnRules r = active;
        return r != null && r.strict;
    }

    public boolean strict() {
        return strict;
    }

    public void setStrict(boolean v) {
        strict = v;
        setDirty();
    }

    public int get(ResourceLocation id) {
        return percent.getOrDefault(id, 100);
    }

    public void set(ResourceLocation id, int value) {
        int v = Math.max(0, Math.min(MAX, value));
        if (v == 100) {
            percent.remove(id);
        } else {
            percent.put(id, v);
        }
        setDirty();
    }

    /** Copia de todas las reglas distintas de 100. */
    public Map<ResourceLocation, Integer> all() {
        return new HashMap<>(percent);
    }

    public static SpawnRules load(CompoundTag tag) {
        SpawnRules r = new SpawnRules();
        r.strict = tag.getBoolean("Strict");
        ListTag list = tag.getList("Rules", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag c = list.getCompound(i);
            ResourceLocation id = ResourceLocation.tryParse(c.getString("Id"));
            int v = c.getInt("Percent");
            if (id != null && v != 100) {
                r.percent.put(id, Math.max(0, Math.min(MAX, v)));
            }
        }
        return r;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        for (Map.Entry<ResourceLocation, Integer> e : percent.entrySet()) {
            CompoundTag c = new CompoundTag();
            c.putString("Id", e.getKey().toString());
            c.putInt("Percent", e.getValue());
            list.add(c);
        }
        tag.put("Rules", list);
        tag.putBoolean("Strict", strict);
        return tag;
    }
}
