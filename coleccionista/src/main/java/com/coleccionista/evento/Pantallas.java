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
    /** Solo se prenden las pantallas grandes (la torre del patio, la sala): las chicas de las celdas se quedan apagadas. */
    private static boolean soloGrande;
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
        l.add(new Frase("que_tenemos", 94, null, 1F));
        l.add(new Frase("durmieron", 92, null, 1F));
        l.add(new Frase("acerquense", 140, null, 1F));
        l.add(new Frase("presento", 120, null, 1F));
        l.add(new Frase("coleccion", 89, null, 1F));
        l.add(new Frase("pruebas", 122, null, 1F));
        l.add(new Frase("solo_uno", 98, null, 1F));
        l.add(new Frase("los_demas", 100, null, 1F));
        l.add(new Frase("risa", 70, SoundEvents.WITCH_CELEBRATE, 0.55F));
        l.add(new Frase("candado", 195, null, 1F));
        l.add(new Frase("escondidos", 115, null, 1F));
        l.add(new Frase("final_risa", 70, SoundEvents.WITCH_CELEBRATE, 0.55F));
        return l;
    }

    /** La explicación de Encadenados (en la Sala de los grilletes, antes de abrir la reja). */
    public static List<Frase> encadenados() {
        List<Frase> l = new ArrayList<>();
        l.add(new Frase("hola", 90, null, 1F));
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
        l.add(new Frase("llegaron", 140, null, 1F));
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
        l.add(new Frase("hola", 145, null, 1F));
        l.add(new Frase("venda", 165, null, 1F));
        l.add(new Frase("guia", 155, null, 1F));
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
        l.add(new Frase("porton", 125, null, 1F));
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

    /** Capítulo 1, en la sala grande: que formen equipos de 2 o de 3 y se suban a un ascensor. */
    public static List<Frase> equipos() {
        List<Frase> l = new ArrayList<>();
        l.add(new Frase("bienvenidos", 85, null, 1F));
        l.add(new Frase("formen", 98, null, 1F));
        l.add(new Frase("ascensor", 115, null, 1F));
        l.add(new Frase("solos", 100, null, 1F));
        l.add(new Frase("risa", 60, null, 1F));
        return l;
    }

    /** Capítulo 1, en el taller (cada equipo en su circuito): la mesa de la venda, la cinta y las esposas. */
    public static List<Frase> sentidos() {
        List<Frase> l = new ArrayList<>();
        l.add(new Frase("taller", 65, null, 1F));
        l.add(new Frase("mesa", 135, null, 1F));
        l.add(new Frase("venda", 75, null, 1F));
        l.add(new Frase("cinta", 80, null, 1F));
        l.add(new Frase("esposas", 105, null, 1F));
        l.add(new Frase("parejas", 120, null, 1F));
        l.add(new Frase("cadena", 125, null, 1F));
        l.add(new Frase("ultimo", 110, null, 1F));
        l.add(new Frase("abierta", 65, null, 1F));
        return l;
    }

    /** Capítulo 1, la vitrina: llegan las sillas del último equipo. */
    public static List<Frase> vitrina() {
        List<Frase> l = new ArrayList<>();
        l.add(new Frase("ultimos", 65, null, 1F));
        l.add(new Frase("quietos", 115, null, 1F));
        l.add(new Frase("piezas", 105, null, 1F));
        l.add(new Frase("cera", 105, null, 1F));
        l.add(new Frase("risa", 70, SoundEvents.WITCH_CELEBRATE, 0.55F));
        return l;
    }

    /** Capítulo 1, después de la cera: las estatuas nuevas. */
    public static List<Frase> vitrina_fin() {
        List<Frase> l = new ArrayList<>();
        l.add(new Frase("hermosas", 50, null, 1F));
        l.add(new Frase("siguen", 110, null, 1F));
        return l;
    }

    public static List<Frase> antesala() {
        List<Frase> l = new ArrayList<>();
        l.add(new Frase("polvo", 90, null, 1F));
        l.add(new Frase("servian", 104, null, 1F));
        l.add(new Frase("casa", 85, null, 1F));
        return l;
    }

    public static List<Frase> mansion() {
        List<Frase> l = new ArrayList<>();
        l.add(new Frase("bienvenidos", 60, null, 1F));
        l.add(new Frase("no_toquen", 80, null, 1F));
        l.add(new Frase("presentar", 95, null, 1F));
        l.add(new Frase("se_escapo", 88, null, 1F));
        l.add(new Frase("ahi_esta", 65, null, 1F));
        l.add(new Frase("no_ha_comido", 126, null, 1F));
        l.add(new Frase("minuto", 128, null, 1F));
        l.add(new Frase("cascabel", 90, null, 1F));
        return l;
    }

    public static List<Frase> mansion_suelta() {
        List<Frase> l = new ArrayList<>();
        l.add(new Frase("se_acabo", 55, null, 1F));
        l.add(new Frase("a_jugar", 87, null, 1F));
        l.add(new Frase("risa", 60, SoundEvents.WITCH_CELEBRATE, 0.55F));
        return l;
    }

    public static List<Frase> mansion_fin() {
        List<Frase> l = new ArrayList<>();
        l.add(new Frase("a_tu_jaula", 63, null, 1F));
        l.add(new Frase("buena", 50, null, 1F));
        l.add(new Frase("enteros", 80, null, 1F));
        return l;
    }

    public static List<Frase> caja() {
        List<Frase> l = new ArrayList<>();
        l.add(new Frase("bienvenidos", 75, null, 1F));
        l.add(new Frase("gondolas", 126, null, 1F));
        l.add(new Frase("espalda", 210, null, 1F));
        l.add(new Frase("no_se_ve", 130, null, 1F));
        l.add(new Frase("diganse", 165, null, 1F));
        l.add(new Frase("gira", 135, null, 1F));
        l.add(new Frase("voces", 140, null, 1F));
        l.add(new Frase("traidores", 162, null, 1F));
        l.add(new Frase("al_final", 105, null, 1F));
        l.add(new Frase("si_fallan", 85, null, 1F));
        l.add(new Frase("traidor_bala", 100, null, 1F));
        l.add(new Frase("sientense", 66, null, 1F));
        return l;
    }

    public static List<Frase> caja_revision() {
        List<Frase> l = new ArrayList<>();
        l.add(new Frase("circulo", 85, null, 1F));
        l.add(new Frase("juguete", 96, null, 1F));
        l.add(new Frase("uno_por_uno", 112, null, 1F));
        l.add(new Frase("fuerte", 69, null, 1F));
        return l;
    }

    public static List<Frase> caja_traidores() {
        List<Frase> l = new ArrayList<>();
        l.add(new Frase("ahora", 78, null, 1F));
        return l;
    }

    public static List<Frase> caja_fin() {
        List<Frase> l = new ArrayList<>();
        l.add(new Frase("se_acabo", 74, null, 1F));
        l.add(new Frase("eran", 60, null, 1F));
        l.add(new Frase("enteros", 100, null, 1F));
        return l;
    }

    public static List<Frase> sorpresa() {
        List<Frase> l = new ArrayList<>();
        l.add(new Frase("bienvenidos", 65, null, 1F));
        l.add(new Frase("amigo", 152, null, 1F));
        l.add(new Frase("cuerda", 206, null, 1F));
        l.add(new Frase("una_tres", 144, null, 1F));
        l.add(new Frase("hambre", 125, SoundEvents.WITCH_CELEBRATE, 0.5F));
        l.add(new Frase("zapatos", 100, null, 1F));
        l.add(new Frase("empecemos", 50, null, 1F));
        return l;
    }

    public static List<Frase> sorpresa_fin() {
        List<Frase> l = new ArrayList<>();
        l.add(new Frase("lleno", 81, null, 1F));
        l.add(new Frase("zapatos", 75, null, 1F));
        l.add(new Frase("enteros", 80, null, 1F));
        return l;
    }

    public static List<Frase> te() {
        List<Frase> l = new ArrayList<>();
        l.add(new Frase("bienvenidos", 70, null, 1F));
        l.add(new Frase("parejas", 76, null, 1F));
        l.add(new Frase("confien", 100, null, 1F));
        l.add(new Frase("minuto", 85, null, 1F));
        return l;
    }

    public static List<Frase> te_reglas() {
        List<Frase> l = new ArrayList<>();
        l.add(new Frase("hechas", 92, null, 1F));
        l.add(new Frase("piedra", 80, null, 1F));
        l.add(new Frase("caliz", 80, null, 1F));
        l.add(new Frase("cinco", 115, null, 1F));
        l.add(new Frase("solo_uno", 125, SoundEvents.WITCH_CELEBRATE, 0.5F));
        l.add(new Frase("salud", 50, null, 1F));
        return l;
    }

    public static List<Frase> te_fin() {
        List<Frase> l = new ArrayList<>();
        l.add(new Frase("terminado", 50, null, 1F));
        l.add(new Frase("enteros", 80, null, 1F));
        return l;
    }

    public static List<Frase> piano() {
        List<Frase> l = new ArrayList<>();
        l.add(new Frase("bienvenidos", 75, null, 1F));
        l.add(new Frase("bailarina", 135, null, 1F));
        l.add(new Frase("melodia", 160, null, 1F));
        l.add(new Frase("centro", 90, null, 1F));
        l.add(new Frase("a_la_vez", 90, null, 1F));
        l.add(new Frase("se_abre", 100, SoundEvents.WITCH_CELEBRATE, 0.5F));
        l.add(new Frase("varios", 165, null, 1F));
        l.add(new Frase("baile", 60, null, 1F));
        return l;
    }

    public static List<Frase> piano_pistas() {
        List<Frase> l = new ArrayList<>();
        l.add(new Frase("pistas", 105, null, 1F));
        l.add(new Frase("numero", 125, null, 1F));
        return l;
    }

    public static List<Frase> piano_fin() {
        List<Frase> l = new ArrayList<>();
        l.add(new Frase("cansada", 65, null, 1F));
        l.add(new Frase("ultimo", 95, null, 1F));
        l.add(new Frase("enteros", 80, null, 1F));
        return l;
    }

    public static List<Frase> veintiuno() {
        List<Frase> l = new ArrayList<>();
        l.add(new Frase("mesa", 115, null, 1F));
        l.add(new Frase("dos", 125, null, 1F));
        l.add(new Frase("sientense", 95, null, 1F));
        return l;
    }

    public static List<Frase> veintiuno_reglas() {
        List<Frase> l = new ArrayList<>();
        l.add(new Frase("veintiuno", 110, null, 1F));
        l.add(new Frase("dos_cartas", 100, null, 1F));
        l.add(new Frase("pedir", 180, null, 1F));
        l.add(new Frase("pasarse", 100, null, 1F));
        l.add(new Frase("comodines", 180, null, 1F));
        l.add(new Frase("aprieta", 142, null, 1F));
        l.add(new Frase("tres", 90, SoundEvents.WITCH_CELEBRATE, 0.5F));
        l.add(new Frase("suerte", 55, null, 1F));
        return l;
    }

    public static List<Frase> veintiuno_fin() {
        List<Frase> l = new ArrayList<>();
        l.add(new Frase("ganador", 55, null, 1F));
        l.add(new Frase("completa", 116, null, 1F));
        l.add(new Frase("puerta", 118, null, 1F));
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
            case "equipos" -> equipos();
            case "sentidos" -> sentidos();
            case "vitrina" -> vitrina();
            case "vitrina_fin" -> vitrina_fin();
            case "antesala" -> antesala();
            case "mansion" -> mansion();
            case "mansion_suelta" -> mansion_suelta();
            case "mansion_fin" -> mansion_fin();
            case "caja" -> caja();
            case "caja_revision" -> caja_revision();
            case "caja_traidores" -> caja_traidores();
            case "caja_fin" -> caja_fin();
            case "sorpresa" -> sorpresa();
            case "sorpresa_fin" -> sorpresa_fin();
            case "te" -> te();
            case "te_reglas" -> te_reglas();
            case "te_fin" -> te_fin();
            case "piano" -> piano();
            case "piano_pistas" -> piano_pistas();
            case "piano_fin" -> piano_fin();
            case "veintiuno" -> veintiuno();
            case "veintiuno_reglas" -> veintiuno_reglas();
            case "veintiuno_fin" -> veintiuno_fin();
            default -> null;
        };
    }

    // ------------------------------------------------------------------ control

    /** Prende solo las pantallas grandes (las chicas de las celdas se apagan) o todas otra vez. */
    public static void soloGrande(MinecraftServer s, boolean si) {
        servidor = s;
        soloGrande = si;
        enviar(s);
    }

    public static boolean soloGrande() {
        return soloGrande;
    }

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
        t.putBoolean("soloGrande", soloGrande);
        t.putLong("desde", servidor == null ? 0 : servidor.overworld().getGameTime());
        return t;
    }

    private static void enviar(MinecraftServer s) {
        PlayerStore.Sync.toAll(s, KEY, estado());
    }

    /** La clave de la frase que se está diciendo ("coleccionista.voz.<guion>.<clave>"; vacía si no habla). */
    public static String clave() {
        return guion == null ? "" : linea;
    }

    /** La frase actual como texto (para el chat del organizador). */
    public static Component lineaActual() {
        return linea.isEmpty() ? Component.empty() : Component.translatable(linea);
    }
}
