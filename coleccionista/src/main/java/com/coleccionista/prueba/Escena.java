package com.coleccionista.prueba;

import com.axeliscore.data.PlayerStore;
import com.coleccionista.Coleccionista;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;

/**
 * Lo que se ve en la pantalla de todos al cambiar de prueba: un fundido a negro con una frase (para llevarlos a otro lugar sin que lo
 * vean) y la presentación de cine de cada prueba (número, nombre y su frase).
 */
public final class Escena {
    private static final String MOD = Coleccionista.MODID;
    public static final String KEY_FUNDIDO = MOD + ":fundido";
    public static final String KEY_PRESENTACION = MOD + ":presentacion";
    private static int contador;

    private Escena() {
    }

    /** Todos a negro por {@code ticks} (con un texto de idioma en medio, puede ser vacío). */
    public static void fundido(MinecraftServer s, String clave, int ticks) {
        CompoundTag t = new CompoundTag();
        t.putString("texto", clave == null ? "" : clave);
        t.putInt("ticks", ticks);
        t.putInt("n", ++contador);
        PlayerStore.Sync.toAll(s, KEY_FUNDIDO, t);
    }

    /** La presentación de una prueba: "PRUEBA I", el nombre en grande y su frase. */
    public static void presentacion(MinecraftServer s, String numero, String claveTitulo, String claveLema) {
        CompoundTag t = new CompoundTag();
        t.putString("numero", numero);
        t.putString("titulo", claveTitulo);
        t.putString("lema", claveLema);
        t.putInt("n", ++contador);
        PlayerStore.Sync.toAll(s, KEY_PRESENTACION, t);
        Coleccionista.LOGGER.info("COLECCIONISTA_PRESENTACION {}", numero);
    }
}
