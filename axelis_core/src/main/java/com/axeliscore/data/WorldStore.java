package com.axeliscore.data;

import com.axeliscore.plat.Nbt;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Datos de un mod que se guardan con el mundo (un archivo por mod): una parte general ({@link #global()}) y una por jugador
 * ({@link #player(UUID)}). Se cambian los CompoundTag directamente y luego se llama {@link #dirty()} para que se guarden.
 * <pre>
 *     WorldStore s = WorldStore.get(server, "pesca");
 *     s.player(p.getUUID()).putInt("peces", 12);
 *     s.dirty();
 * </pre>
 */
public final class WorldStore extends SavedData {
    private static final Map<String, Object> TYPES = new HashMap<>();
    private CompoundTag global = new CompoundTag();
    private final Map<UUID, CompoundTag> players = new HashMap<>();

    public WorldStore() {
    }

    /** Los datos del mod {@code modid} en este mundo (se crean vacíos la primera vez). */
    public static WorldStore get(MinecraftServer server, String modid) {
        //#if MC >= 1.21.5
        //$$ @SuppressWarnings("unchecked")
        //$$ net.minecraft.world.level.saveddata.SavedDataType<WorldStore> type = (net.minecraft.world.level.saveddata.SavedDataType<WorldStore>)
        //$$         TYPES.computeIfAbsent(modid, m -> new net.minecraft.world.level.saveddata.SavedDataType<>(com.axeliscore.plat.Ids.rl(m, "axelis_datos"),
        //$$                 WorldStore::new, CompoundTag.CODEC.xmap(WorldStore::load, d -> d.save(new CompoundTag())), null));
        //$$ return server.overworld().getDataStorage().computeIfAbsent(type);
        //#elif MC >= 1.20.5
        //$$ return server.overworld().getDataStorage().computeIfAbsent(new SavedData.Factory<>(WorldStore::new, (t, reg) -> load(t), null),
        //$$         "axelis_" + modid);
        //#else
        return server.overworld().getDataStorage().computeIfAbsent(WorldStore::load, WorldStore::new, "axelis_" + modid);
        //#endif
    }

    /** Datos generales del mod en este mundo. */
    public CompoundTag global() {
        return global;
    }

    /** Datos de un jugador (se crean vacíos si no tiene). */
    public CompoundTag player(UUID id) {
        return players.computeIfAbsent(id, k -> new CompoundTag());
    }

    public boolean hasPlayer(UUID id) {
        return players.containsKey(id);
    }

    /** Todos los jugadores que tienen datos (por ejemplo, para una tabla de récords). */
    public Set<UUID> players() {
        return players.keySet();
    }

    /** Avisa que algo cambió (se guarda con el mundo). */
    public void dirty() {
        setDirty();
    }

    public static WorldStore load(CompoundTag tag) {
        WorldStore d = new WorldStore();
        d.global = Nbt.c(tag, "global").copy();
        CompoundTag ps = Nbt.c(tag, "players");
        for (String k : ps.getAllKeys()) {
            try {
                d.players.put(UUID.fromString(k), Nbt.c(ps, k).copy());
            } catch (IllegalArgumentException ignored) {
                // clave dañada: se ignora
            }
        }
        return d;
    }

    //#if MC >= 1.20.5 && MC < 1.21.5
    //$$ @Override
    //$$ public CompoundTag save(CompoundTag tag, net.minecraft.core.HolderLookup.Provider reg) {
    //$$     return save(tag);
    //$$ }
    //#endif

    //#if MC < 1.20.5
    @Override
    //#endif
    public CompoundTag save(CompoundTag tag) {
        tag.put("global", global.copy());
        CompoundTag ps = new CompoundTag();
        players.forEach((k, v) -> ps.put(k.toString(), v.copy()));
        tag.put("players", ps);
        return tag;
    }
}
