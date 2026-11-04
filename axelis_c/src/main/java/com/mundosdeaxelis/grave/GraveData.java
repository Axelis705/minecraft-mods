package com.mundosdeaxelis.grave;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Lista de lápidas activas del mundo (quién, dónde y cuándo caducan), para avisar al jugador aunque salga y vuelva a entrar. */
public final class GraveData extends SavedData {
    public record Entry(UUID owner, ResourceLocation dim, BlockPos pos, long expire) {
    }

    private final List<Entry> list = new ArrayList<>();

    public static GraveData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(GraveData::load, GraveData::new, "mdc_graves");
    }

    public void add(Entry e) {
        list.add(e);
        setDirty();
    }

    public void remove(ResourceLocation dim, BlockPos pos) {
        if (list.removeIf(e -> e.dim.equals(dim) && e.pos.equals(pos))) {
            setDirty();
        }
    }

    /** Todas las lápidas apuntadas en ese mundo. */
    public List<Entry> inDimension(ResourceLocation dim) {
        List<Entry> out = new ArrayList<>();
        for (Entry e : list) {
            if (e.dim.equals(dim)) {
                out.add(e);
            }
        }
        return out;
    }

    /** Lápidas de ese jugador que aún no han caducado. */
    public List<Entry> of(UUID owner, long now) {
        List<Entry> out = new ArrayList<>();
        boolean changed = list.removeIf(e -> e.expire <= now - 200);
        if (changed) {
            setDirty();
        }
        for (Entry e : list) {
            if (e.owner.equals(owner) && e.expire > now) {
                out.add(e);
            }
        }
        return out;
    }

    public static GraveData load(CompoundTag tag) {
        GraveData d = new GraveData();
        for (Tag t : tag.getList("graves", Tag.TAG_COMPOUND)) {
            CompoundTag c = (CompoundTag) t;
            d.list.add(new Entry(c.getUUID("owner"), new ResourceLocation(c.getString("dim")), NbtUtils.readBlockPos(c.getCompound("pos")), c.getLong("expire")));
        }
        return d;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag l = new ListTag();
        for (Entry e : list) {
            CompoundTag c = new CompoundTag();
            c.putUUID("owner", e.owner);
            c.putString("dim", e.dim.toString());
            c.put("pos", NbtUtils.writeBlockPos(e.pos));
            c.putLong("expire", e.expire);
            l.add(c);
        }
        tag.put("graves", l);
        return tag;
    }
}
