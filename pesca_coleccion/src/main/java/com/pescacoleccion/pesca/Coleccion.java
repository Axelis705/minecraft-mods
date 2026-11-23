package com.pescacoleccion.pesca;

import com.axeliscore.data.PlayerStore;
import com.axeliscore.data.WorldStore;
import com.axeliscore.plat.Nbt;
import com.axeliscore.plat.Plat;
import com.pescacoleccion.PescaColeccion;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;

/**
 * (Servidor) La colección de cada jugador y los récords del mundo. Por jugador: {"peces": {id: {"n": veces, "max": cm, "e": estrellas}}};
 * estrellas: 1 = pescado, 2 = grande, 4 = sin errores. Récords del mundo: {"records": {id: {"cm": cm, "quien": nombre}}}.
 */
public final class Coleccion {
    public static final String MOD = PescaColeccion.MODID;

    /** Lo que cambió al pescar. */
    public record Resultado(boolean nuevo, boolean recordPersonal, boolean recordMundo, int estrellasAntes, int estrellasAhora) {
        public int estrellasNuevas() {
            return Integer.bitCount(estrellasAhora & ~estrellasAntes & 7);
        }
    }

    private Coleccion() {
    }

    public static Resultado registrar(ServerPlayer p, Especie e, float cm, boolean perfecto) {
        CompoundTag d = PlayerStore.of(p, MOD);
        CompoundTag peces = Nbt.c(d, "peces");
        CompoundTag f = Nbt.c(peces, e.id());
        int n = Nbt.i(f, "n") + 1;
        float old = Nbt.f(f, "max");
        int antes = Nbt.i(f, "e");
        int ahora = antes | 1 | (e.grande(cm) ? 2 : 0) | (perfecto ? 4 : 0);
        f.putInt("n", n);
        f.putFloat("max", Math.max(old, cm));
        f.putInt("e", ahora);
        peces.put(e.id(), f);
        d.put("peces", peces);
        d.putInt("total", Nbt.i(d, "total") + 1);
        // récord del mundo
        WorldStore w = WorldStore.get(Plat.server(p), MOD);
        CompoundTag recs = Nbt.c(w.global(), "records");
        CompoundTag r = Nbt.c(recs, e.id());
        boolean mundo = cm > Nbt.f(r, "cm");
        if (mundo) {
            r.putFloat("cm", cm);
            r.putString("quien", p.getGameProfile().getName());
            recs.put(e.id(), r);
            w.global().put("records", recs);
            w.dirty();
        }
        PlayerStore.save(p, MOD);
        return new Resultado(n == 1, cm > old && n > 1, mundo, antes, ahora);
    }

    /** Estrellas de maestría que tiene el jugador en total (de 75). */
    public static int estrellas(CompoundTag datos) {
        CompoundTag peces = Nbt.c(datos, "peces");
        int s = 0;
        for (Especie e : Especies.all()) {
            s += Integer.bitCount(Nbt.i(Nbt.c(peces, e.id()), "e") & 7);
        }
        return s;
    }
}
