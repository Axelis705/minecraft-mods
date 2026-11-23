package com.coleccionista.evento;

import com.axeliscore.AxelisCore;
import com.axeliscore.data.PlayerStore;
import com.coleccionista.Coleccionista;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

import java.util.ArrayList;
import java.util.List;

/**
 * Lo que muestran las pantallas (todas a la vez): apagadas, estática, el Coleccionista hablando con subtítulos, o un texto grande. Los
 * discursos son guiones de frases con su duración; cada frase puede tener su audio (la voz de Axelis, {@code voz/<guion>_<n>.ogg}) y
 * mientras no exista se ven solo los subtítulos.
 */
public final class Pantallas {
    private static final String MOD = Coleccionista.MODID;
    public static final String KEY = MOD + ":pantalla";

    /** Una frase: el texto (clave de idioma), cuánto dura en ticks y un sonido opcional (risa, golpe...). */
    public record Frase(String clave, int ticks, SoundEvent sonido, float tono) {
    }

    private static String modo = "apagada", texto = "", linea = "";
    private static boolean hablando;
    private static List<Frase> guion;
    private static String guionId = "";
    private static int indice = -1, quedan;
    private static Runnable alTerminar;
    private static MinecraftServer servidor;

    private Pantallas() {
    }

    public static void init() {
        AxelisCore.onServerTick(Pantallas::tick);
        AxelisCore.onLogin(p -> PlayerStore.Sync.toPlayer(p, KEY, estado()));
    }

    // ------------------------------------------------------------------ guiones

    /** El discurso de bienvenida (al terminar los 10 minutos del despertar). */
    public static List<Frase> bienvenida() {
        List<Frase> l = new ArrayList<>();
        l.add(new Frase("silencio", 50, null, 1F));
        l.add(new Frase("hola", 50, null, 1F));
        l.add(new Frase("que_tenemos", 80, null, 1F));
        l.add(new Frase("acerquense", 125, null, 1F));
        l.add(new Frase("durmieron", 150, null, 1F));
        l.add(new Frase("no_sabes", 140, null, 1F));
        l.add(new Frase("presento", 105, null, 1F));
        l.add(new Frase("piezas", 145, null, 1F));
        l.add(new Frase("fragiles", 140, null, 1F));
        l.add(new Frase("una_sale", 165, null, 1F));
        l.add(new Frase("las_demas", 125, null, 1F));
        l.add(new Frase("risa", 70, SoundEvents.WITCH_CELEBRATE, 0.55F));
        l.add(new Frase("candado", 155, null, 1F));
        l.add(new Frase("escondidos", 140, null, 1F));
        l.add(new Frase("vitrina", 65, null, 1F));
        return l;
    }

    /** La explicación de Encadenados (en la Sala de los grilletes, antes de abrir la reja). */
    public static List<Frase> encadenados() {
        List<Frase> l = new ArrayList<>();
        l.add(new Frase("hola", 100, null, 1F));
        l.add(new Frase("cadena", 115, null, 1F));
        l.add(new Frase("pareja", 120, null, 1F));
        l.add(new Frase("llaves", 120, null, 1F));
        l.add(new Frase("lanzar", 140, null, 1F));
        l.add(new Frase("gas", 175, null, 1F));
        l.add(new Frase("refugio", 105, null, 1F));
        l.add(new Frase("risa", 60, SoundEvents.WITCH_CELEBRATE, 0.55F));
        l.add(new Frase("vitrina", 135, null, 1F));
        return l;
    }

    /** La explicación de Compartir o robar (en el Salón de las mesas). */
    public static List<Frase> compartir() {
        List<Frase> l = new ArrayList<>();
        l.add(new Frase("hola", 130, null, 1F));
        l.add(new Frase("mesa", 110, null, 1F));
        l.add(new Frase("hablen", 155, null, 1F));
        l.add(new Frase("elijan", 120, null, 1F));
        l.add(new Frase("comparten", 85, null, 1F));
        l.add(new Frase("roba", 80, null, 1F));
        l.add(new Frase("roban", 105, null, 1F));
        l.add(new Frase("risa", 60, SoundEvents.WITCH_CELEBRATE, 0.55F));
        l.add(new Frase("vitrina", 150, null, 1F));
        return l;
    }

    /** La final: Las copas. */
    public static List<Frase> copas() {
        List<Frase> l = new ArrayList<>();
        l.add(new Frase("hola", 130, null, 1F));
        l.add(new Frase("llegaron", 135, null, 1F));
        l.add(new Frase("copas", 125, null, 1F));
        l.add(new Frase("veneno", 60, null, 1F));
        l.add(new Frase("turnos", 145, null, 1F));
        l.add(new Frase("promesa", 75, null, 1F));
        l.add(new Frase("risa", 60, SoundEvents.WITCH_CELEBRATE, 0.55F));
        l.add(new Frase("salud", 50, null, 1F));
        return l;
    }

    /** El sacrificio (en la Capilla). */
    public static List<Frase> sacrificio() {
        List<Frase> l = new ArrayList<>();
        l.add(new Frase("hola", 120, null, 1F));
        l.add(new Frase("reja", 130, null, 1F));
        l.add(new Frase("altar", 145, null, 1F));
        l.add(new Frase("votar", 130, null, 1F));
        l.add(new Frase("risa", 60, SoundEvents.WITCH_CELEBRATE, 0.55F));
        l.add(new Frase("vitrina", 150, null, 1F));
        return l;
    }

    /** El traidor (en el Archivo). */
    public static List<Frase> traidor() {
        List<Frase> l = new ArrayList<>();
        l.add(new Frase("hola", 155, null, 1F));
        l.add(new Frase("boveda", 100, null, 1F));
        l.add(new Frase("notas", 140, null, 1F));
        l.add(new Frase("traidor", 145, null, 1F));
        l.add(new Frase("miente", 90, null, 1F));
        l.add(new Frase("intentos", 120, null, 1F));
        l.add(new Frase("si_no", 110, null, 1F));
        l.add(new Frase("risa", 60, SoundEvents.WITCH_CELEBRATE, 0.55F));
        l.add(new Frase("vitrina", 115, null, 1F));
        return l;
    }

    /** A ciegas (en los Laberintos). */
    public static List<Frase> ciegas() {
        List<Frase> l = new ArrayList<>();
        l.add(new Frase("hola", 155, null, 1F));
        l.add(new Frase("venda", 165, null, 1F));
        l.add(new Frase("guia", 165, null, 1F));
        l.add(new Frase("voz", 90, null, 1F));
        l.add(new Frase("trampas", 120, null, 1F));
        l.add(new Frase("mueven", 125, null, 1F));
        l.add(new Frase("baldosas", 155, null, 1F));
        l.add(new Frase("ultimo", 115, null, 1F));
        l.add(new Frase("risa", 60, SoundEvents.WITCH_CELEBRATE, 0.55F));
        l.add(new Frase("vitrina", 85, null, 1F));
        return l;
    }

    public static List<Frase> puertas() {
        List<Frase> l = new ArrayList<>();
        l.add(new Frase("hola", 85, null, 1F));
        l.add(new Frase("tres", 95, null, 1F));
        l.add(new Frase("piso", 140, null, 1F));
        l.add(new Frase("pista", 140, null, 1F));
        l.add(new Frase("juntas", 170, null, 1F));
        l.add(new Frase("tiempo", 160, null, 1F));
        l.add(new Frase("risa", 60, SoundEvents.WITCH_CELEBRATE, 0.55F));
        l.add(new Frase("vitrina", 105, null, 1F));
        return l;
    }

    public static List<Frase> tres() {
        List<Frase> l = new ArrayList<>();
        l.add(new Frase("hola", 85, null, 1F));
        l.add(new Frase("objetos", 125, null, 1F));
        l.add(new Frase("venda", 140, null, 1F));
        l.add(new Frase("cinta", 110, null, 1F));
        l.add(new Frase("esposas", 110, null, 1F));
        l.add(new Frase("decidan", 130, null, 1F));
        l.add(new Frase("cadena", 170, null, 1F));
        l.add(new Frase("risa", 60, SoundEvents.WITCH_CELEBRATE, 0.55F));
        l.add(new Frase("vitrina", 130, null, 1F));
        return l;
    }

    /** Cuando todas las piezas salieron de su celda (lo dice en la pantalla grande del escenario). */
    public static List<Frase> salida() {
        List<Frase> l = new ArrayList<>();
        l.add(new Frase("bravo", 55, null, 1F));
        l.add(new Frase("todas", 125, null, 1F));
        l.add(new Frase("museo", 165, null, 1F));
        l.add(new Frase("salas", 125, null, 1F));
        l.add(new Frase("risa", 60, SoundEvents.WITCH_CELEBRATE, 0.55F));
        l.add(new Frase("esperen", 125, null, 1F));
        return l;
    }

    /** La parada del montacargas ({@link #parada}: la zona a la que va). */
    public static String parada = "";

    public static List<Frase> montacargas() {
        List<Frase> l = new ArrayList<>();
        l.add(new Frase("viaje", 95, null, 1F));
        // la parada: una frase por zona
        switch (parada) {
            case "cloacas" -> l.add(new Frase("cloacas", 115, null, 1F));
            case "archivo" -> l.add(new Frase("archivo", 135, null, 1F));
            case "salon" -> l.add(new Frase("salon", 130, null, 1F));
            case "laberintos" -> l.add(new Frase("laberintos", 110, null, 1F));
            case "capilla" -> l.add(new Frase("capilla", 140, null, 1F));
            case "puertas" -> l.add(new Frase("puertas", 145, null, 1F));
            case "final" -> l.add(new Frase("final", 130, null, 1F));
            case "galeria" -> l.add(new Frase("galeria", 145, null, 1F));
            default -> {
            }
        }
        return l;
    }

    public static List<Frase> guion(String id) {
        return switch (id) {
            case "bienvenida" -> bienvenida();
            case "salida" -> salida();
            case "montacargas" -> montacargas();
            case "encadenados" -> encadenados();
            case "compartir" -> compartir();
            case "copas" -> copas();
            case "sacrificio" -> sacrificio();
            case "traidor" -> traidor();
            case "ciegas" -> ciegas();
            case "tres" -> tres();
            case "puertas" -> puertas();
            default -> null;
        };
    }

    // ------------------------------------------------------------------ control

    public static void modo(MinecraftServer s, String m, String t) {
        servidor = s;
        modo = m;
        texto = t == null ? "" : t;
        if (!"coleccionista".equals(m)) {
            hablando = false;
            linea = "";
        }
        enviar(s);
    }

    /** Empieza un discurso en todas las pantallas. */
    public static boolean hablar(MinecraftServer s, String id, Runnable fin) {
        List<Frase> g = guion(id);
        if (g == null) {
            return false;
        }
        servidor = s;
        guion = g;
        guionId = id;
        indice = -1;
        quedan = 30; // un poco de estática antes de que aparezca
        alTerminar = fin;
        modo = "estatica";
        linea = "";
        hablando = false;
        enviar(s);
        Anuncio.sonido(s, SoundEvents.BEACON_POWER_SELECT, 0.6F, 0.5F);
        Coleccionista.LOGGER.info("COLECCIONISTA_DISCURSO {}", id);
        return true;
    }

    public static void callar(MinecraftServer s) {
        guion = null;
        indice = -1;
        hablando = false;
        linea = "";
        enviar(s);
    }

    public static boolean ocupado() {
        return guion != null;
    }

    private static void tick(MinecraftServer s) {
        if (guion == null || s != servidor) {
            return;
        }
        if (--quedan > 0) {
            return;
        }
        indice++;
        if (indice >= guion.size()) {
            Runnable f = alTerminar;
            guion = null;
            alTerminar = null;
            hablando = false;
            linea = "";
            modo = "coleccionista";
            enviar(s);
            Coleccionista.LOGGER.info("COLECCIONISTA_DISCURSO_FIN {}", guionId);
            if (f != null) {
                f.run();
            }
            return;
        }
        Frase fr = guion.get(indice);
        quedan = fr.ticks();
        modo = "coleccionista";
        hablando = true;
        linea = MOD + ".voz." + guionId + "." + fr.clave();
        enviar(s);
        // la voz grabada (si existe en el paquete de recursos) y el sonido de la frase
        ResourceLocation voz = new ResourceLocation(MOD, "voz." + guionId + "." + fr.clave());
        for (ServerPlayer p : s.getPlayerList().getPlayers()) {
            p.connection.send(new net.minecraft.network.protocol.game.ClientboundSoundPacket(
                    net.minecraft.core.Holder.direct(SoundEvent.createVariableRangeEvent(voz)), SoundSource.VOICE, p.getX(), p.getY(),
                    p.getZ(), 1F, 1F, p.getRandom().nextLong()));
            if (fr.sonido() != null) {
                p.playNotifySound(fr.sonido(), SoundSource.VOICE, 1F, fr.tono());
            }
        }
    }

    // ------------------------------------------------------------------ a los clientes

    public static CompoundTag estado() {
        CompoundTag t = new CompoundTag();
        t.putString("modo", modo);
        t.putString("texto", texto);
        t.putString("linea", linea);
        t.putBoolean("hablando", hablando);
        t.putLong("desde", servidor == null ? 0 : servidor.overworld().getGameTime());
        return t;
    }

    private static void enviar(MinecraftServer s) {
        PlayerStore.Sync.toAll(s, KEY, estado());
    }

    /** La frase actual como texto (para el chat del organizador). */
    public static Component lineaActual() {
        return linea.isEmpty() ? Component.empty() : Component.translatable(linea);
    }
}
