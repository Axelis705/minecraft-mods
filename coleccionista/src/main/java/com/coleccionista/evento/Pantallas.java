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
        l.add(new Frase("hola", 50, null, 1F));
        l.add(new Frase("no_sabes", 90, null, 1F));
        l.add(new Frase("coleccion", 70, null, 1F));
        l.add(new Frase("piezas", 100, null, 1F));
        l.add(new Frase("una_sale", 100, null, 1F));
        l.add(new Frase("listo", 80, null, 1F));
        l.add(new Frase("risa", 70, SoundEvents.WITCH_CELEBRATE, 0.55F));
        l.add(new Frase("candado", 110, null, 1F));
        return l;
    }

    public static List<Frase> guion(String id) {
        return switch (id) {
            case "bienvenida" -> bienvenida();
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
