package com.casino.west;

import com.casino.data.CasinoData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

/**
 * Récords del Oeste de todo el mundo: cuando aparecen varios saloons, todos los carteles (toro, tiro, duelo, vencidas, dados) muestran
 * al mismo campeón. Se guardan en {@link CasinoData}; cada cartel revisa el récord cada 2 segundos y se actualiza si cambió.
 */
public final class WestRecords {
    public static final String BULL = "bull";
    public static final String GALLERY = "gallery";
    public static final String DUEL = "duel";
    public static final String ARM = "arm";
    public static final String DICE = "dice";

    public record Rec(int value, String name) {
    }

    private WestRecords() {
    }

    /** En el duelo y las vencidas gana el tiempo más corto; en lo demás, el número más alto. */
    private static boolean lowerIsBetter(String game) {
        return DUEL.equals(game) || ARM.equals(game);
    }

    public static boolean better(String game, int value, int current) {
        if (value <= 0) {
            return false;
        }
        if (current <= 0) {
            return true;
        }
        return lowerIsBetter(game) ? value < current : value > current;
    }

    private static CasinoData data(Level level) {
        return level instanceof ServerLevel sl ? CasinoData.get(sl.getServer()) : null;
    }

    /** ¿Es nuevo récord del mundo? Si lo es, lo guarda y devuelve true. */
    public static boolean offer(Level level, String game, int value, String name) {
        CasinoData d = data(level);
        if (d == null || !better(game, value, d.westRecord(game))) {
            return false;
        }
        d.setWestRecord(game, value, name);
        return true;
    }

    /** El récord del mundo. Si el cartel traía uno mejor (de antes de que los récords fueran del mundo), primero lo sube. */
    public static Rec pull(Level level, String game, int localValue, String localName) {
        CasinoData d = data(level);
        if (d == null) {
            return new Rec(localValue, localName);
        }
        if (localName != null && !localName.isEmpty() && better(game, localValue, d.westRecord(game))) {
            d.setWestRecord(game, localValue, localName);
        }
        return new Rec(d.westRecord(game), d.westRecordName(game));
    }

    /** ¿Le toca a este cartel revisar el récord? (cada 40 ticks, repartido según la posición para no hacerlo todos juntos) */
    public static boolean due(Level level, BlockPos pos) {
        return (level.getGameTime() + (pos.asLong() & 31)) % 40 == 0;
    }
}
