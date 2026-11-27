package com.navidad;

import com.axeliscore.data.WorldStore;
import net.minecraft.server.MinecraftServer;

import java.time.LocalDate;
import java.time.Month;
import java.time.ZoneId;

/**
 * La temporada navideña. Por defecto está <b>siempre</b> activa (quien instala un mod de Navidad quiere sentir la Navidad al jugar);
 * con {@code /navidad temporada auto} queda solo del 1 de diciembre al 6 de enero (hora del centro de México, de donde son Axelis y sus
 * amigos) y con {@code no} se apaga (se guarda con el mundo). Los días especiales (Nochebuena, Navidad, Año Nuevo, Reyes) siempre
 * dependen de la fecha de verdad.
 */
public final class Temporada {
    public static final ZoneId ZONA = ZoneId.of("America/Mexico_City");
    /** Para probar el calendario: el día de diciembre que se finge (0 = el de verdad). */
    public static int diaPrueba;

    private Temporada() {
    }

    public static LocalDate hoy() {
        return LocalDate.now(ZONA);
    }

    /** El día de diciembre (1 a 31) o 0 si no es diciembre. */
    public static int diaDiciembre() {
        if (diaPrueba > 0) {
            return diaPrueba;
        }
        LocalDate h = hoy();
        return h.getMonth() == Month.DECEMBER ? h.getDayOfMonth() : 0;
    }

    public static String modo(MinecraftServer s) {
        String m = WorldStore.get(s, Navidad.MODID).global().getString("temporada");
        return m.isEmpty() ? "si" : m;
    }

    public static void modo(MinecraftServer s, String m) {
        WorldStore w = WorldStore.get(s, Navidad.MODID);
        w.global().putString("temporada", m);
        w.dirty();
    }

    /** ¿Es temporada navideña? */
    public static boolean activa(MinecraftServer s) {
        String m = modo(s);
        if ("si".equals(m)) {
            return true;
        }
        if ("no".equals(m)) {
            return false;
        }
        LocalDate h = hoy();
        return h.getMonth() == Month.DECEMBER || (h.getMonth() == Month.JANUARY && h.getDayOfMonth() <= 6) || diaPrueba > 0;
    }
}
