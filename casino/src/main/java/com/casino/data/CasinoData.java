package com.casino.data;

import com.casino.plat.Nbt;
import com.casino.game.Prize;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Datos del casino que se guardan con el mundo: el día del último giro de cada jugador y los premios pendientes de entregar. */
public final class CasinoData extends SavedData {
    private final Map<UUID, Long> lastSpin = new HashMap<>();
    private final Map<UUID, List<CompoundTag>> pending = new HashMap<>();

    //#if MC >= 1.21.5
    //$$ private static final net.minecraft.world.level.saveddata.SavedDataType<CasinoData> TYPE = new net.minecraft.world.level.saveddata.SavedDataType<>(
    //$$         com.casino.plat.Ids.casino("casino_data"), CasinoData::new,
    //$$         CompoundTag.CODEC.xmap(CasinoData::load, d -> d.save(new CompoundTag())), null);
    //#endif

    public static CasinoData get(MinecraftServer server) {
        //#if MC >= 1.21.5
        //$$ return server.overworld().getDataStorage().computeIfAbsent(TYPE);
        //#elif MC >= 1.20.5
        //$$ return server.overworld().getDataStorage().computeIfAbsent(new SavedData.Factory<>(CasinoData::new, (t, reg) -> load(t), null),
        //$$         "casino_data");
        //#else
        return server.overworld().getDataStorage().computeIfAbsent(CasinoData::load, CasinoData::new, "casino_data");
        //#endif
    }

    /** El día de hoy (reloj del servidor); la ruleta se reinicia a medianoche. */
    public static long today() {
        return LocalDate.now().toEpochDay();
    }

    /** Tiempo que falta para que se reinicie la ruleta. */
    public static Duration untilReset() {
        LocalDateTime now = LocalDateTime.now();
        return Duration.between(now, now.toLocalDate().plusDays(1).atStartOfDay());
    }

    public boolean canSpin(UUID id) {
        Long d = lastSpin.get(id);
        return d == null || d < today();
    }

    public void markSpin(UUID id) {
        lastSpin.put(id, today());
        setDirty();
    }

    public void resetSpin(UUID id) {
        lastSpin.remove(id);
        setDirty();
    }

    public void addPending(UUID id, Prize prize) {
        pending.computeIfAbsent(id, k -> new ArrayList<>()).add(prize.save());
        setDirty();
    }

    /** Saca los premios pendientes de un jugador (y los borra). */
    public List<Prize> takePending(UUID id) {
        List<CompoundTag> l = pending.remove(id);
        List<Prize> out = new ArrayList<>();
        if (l != null) {
            for (CompoundTag t : l) {
                out.add(Prize.load(t));
            }
            setDirty();
        }
        return out;
    }

    public static CasinoData load(CompoundTag tag) {
        CasinoData d = new CasinoData();
        CompoundTag ls = Nbt.c(tag, "last");
        for (String k : ls.getAllKeys()) {
            try {
                d.lastSpin.put(UUID.fromString(k), Nbt.l(ls, k));
            } catch (IllegalArgumentException ignored) {
                // clave dañada: se ignora
            }
        }
        CompoundTag pd = Nbt.c(tag, "pending");
        for (String k : pd.getAllKeys()) {
            try {
                UUID id = UUID.fromString(k);
                ListTag l = Nbt.list(pd, k);
                List<CompoundTag> out = new ArrayList<>();
                for (int i = 0; i < l.size(); i++) {
                    out.add(Nbt.at(l, i));
                }
                d.pending.put(id, out);
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
        CompoundTag ls = new CompoundTag();
        lastSpin.forEach((k, v) -> ls.putLong(k.toString(), v));
        tag.put("last", ls);
        CompoundTag pd = new CompoundTag();
        pending.forEach((k, v) -> {
            ListTag l = new ListTag();
            l.addAll(v);
            pd.put(k.toString(), l);
        });
        tag.put("pending", pd);
        return tag;
    }
}
