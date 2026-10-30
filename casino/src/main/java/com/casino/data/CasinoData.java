package com.casino.data;

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

    public static CasinoData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(CasinoData::load, CasinoData::new, "casino_data");
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
        CompoundTag ls = tag.getCompound("last");
        for (String k : ls.getAllKeys()) {
            try {
                d.lastSpin.put(UUID.fromString(k), ls.getLong(k));
            } catch (IllegalArgumentException ignored) {
                // clave dañada: se ignora
            }
        }
        CompoundTag pd = tag.getCompound("pending");
        for (String k : pd.getAllKeys()) {
            try {
                UUID id = UUID.fromString(k);
                ListTag l = pd.getList(k, Tag.TAG_COMPOUND);
                List<CompoundTag> out = new ArrayList<>();
                for (int i = 0; i < l.size(); i++) {
                    out.add(l.getCompound(i));
                }
                d.pending.put(id, out);
            } catch (IllegalArgumentException ignored) {
                // clave dañada: se ignora
            }
        }
        return d;
    }

    @Override
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
