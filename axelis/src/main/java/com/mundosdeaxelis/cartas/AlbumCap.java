package com.mundosdeaxelis.cartas;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraftforge.common.util.INBTSerializable;

/** El album de un jugador (se guarda con el jugador y sobrevive a la muerte). */
public final class AlbumCap extends AlbumLogic implements INBTSerializable<CompoundTag> {
    @Override
    public CompoundTag serializeNBT() {
        CompoundTag t = new CompoundTag();
        t.put("n", list(normal));
        t.put("h", list(holo));
        return t;
    }

    private static ListTag list(java.util.Set<String> s) {
        ListTag l = new ListTag();
        for (String id : s) {
            l.add(StringTag.valueOf(id));
        }
        return l;
    }

    @Override
    public void deserializeNBT(CompoundTag t) {
        clear();
        for (Tag x : t.getList("n", Tag.TAG_STRING)) {
            if (Cards.get(x.getAsString()) != null) {
                normal.add(x.getAsString());
            }
        }
        for (Tag x : t.getList("h", Tag.TAG_STRING)) {
            if (Cards.get(x.getAsString()) != null) {
                holo.add(x.getAsString());
            }
        }
    }

    public void copyFrom(AlbumCap o) {
        clear();
        normal.addAll(o.normal);
        holo.addAll(o.holo);
    }
}
