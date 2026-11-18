package com.mundosdeaxelis.crate;

import com.mundosdeaxelis.table.Essence;
import net.minecraft.server.level.ServerPlayer;

/**
 * Garantía de las cajas. Por jugador y por caja se cuentan las aperturas seguidas sin cada una de las (hasta tres) rarezas más altas que da la caja
 * (de Épica para arriba); al llegar a su límite sale esa rareza seguro. La de Naturaleza, por ejemplo, no tiene garantía porque
 * no da Épicas. El contador se reinicia cuando sale esa rareza o una mejor.
 */
public final class Pity {
    private Pity() {
    }

    /** Rareza que debe salir en la próxima apertura por garantía, o null si todavía no toca. La más alta manda. */
    public static Tier forced(ServerPlayer sp, CrateKind k) {
        int[] c = Essence.pity(sp, k.id);
        Tier[] ts = k.pityTiers();
        for (int i = ts.length - 1; i >= 0; i--) {
            int need = k.pity(ts[i]);
            if (need > 0 && c[i] + 1 >= need) {
                return ts[i];
            }
        }
        return null;
    }

    /** Anota el resultado de una apertura. */
    public static void record(ServerPlayer sp, CrateKind k, Tier got) {
        int[] c = Essence.pity(sp, k.id);
        Tier[] ts = k.pityTiers();
        for (int i = 0; i < ts.length; i++) {
            Tier t = ts[i];
            if (k.pity(t) <= 0) {
                continue;
            }
            c[i] = got.ordinal() >= t.ordinal() ? 0 : c[i] + 1;
        }
        Essence.setPity(sp, k.id, c);
        Essence.sync(sp);
    }

    /** Cuántas aperturas faltan para la garantía de esa rareza (con los contadores que conoce quien pregunta). */
    public static int left(int[] counters, CrateKind k, Tier t) {
        int need = k.pity(t);
        if (need <= 0) {
            return 0;
        }
        int i = Math.max(0, k.pityIndex(t));
        return Math.max(1, need - counters[i]);
    }
}
