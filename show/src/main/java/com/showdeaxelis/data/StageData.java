package com.showdeaxelis.data;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

/** Donde esta el estudio construido en el mundo (se guarda con el mundo). */
public final class StageData extends SavedData {
    public record Info(int x, int y, int z, int dir) {
    }

    private Info info;

    public static StageData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(StageData::load, StageData::new, "show_de_axelis_stage");
    }

    public Info info() {
        return info;
    }

    public void set(BlockPos p, int dir) {
        info = new Info(p.getX(), p.getY(), p.getZ(), dir);
        setDirty();
    }

    public static StageData load(CompoundTag tag) {
        StageData d = new StageData();
        if (tag.contains("x")) {
            d.info = new Info(tag.getInt("x"), tag.getInt("y"), tag.getInt("z"), tag.getInt("d"));
        }
        return d;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        if (info != null) {
            tag.putInt("x", info.x());
            tag.putInt("y", info.y());
            tag.putInt("z", info.z());
            tag.putInt("d", info.dir());
        }
        return tag;
    }
}
