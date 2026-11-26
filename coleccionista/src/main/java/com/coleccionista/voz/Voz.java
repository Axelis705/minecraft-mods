package com.coleccionista.voz;

import com.axeliscore.AxelisCore;
import com.coleccionista.Coleccionista;
import com.coleccionista.evento.Evento;
import com.coleccionista.evento.Pieza;
import net.minecraft.server.MinecraftServer;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * (Servidor) La voz del evento con Simple Voice Chat (si está instalado): por cercanía entre los vivos; los fantasmas tienen su propio
 * canal (solo se oyen entre ellos, en todo el mapa, y oyen a los vivos que están cerca); el organizador puede usar el altavoz (todos lo
 * oyen); y en algunas pruebas cada sala o laberinto solo oye a los suyos.
 */
public final class Voz {
    private static final Set<UUID> ALTAVOZ = new LinkedHashSet<>();
    private static Map<UUID, Integer> separados = Map.of();
    private static long n;
    /** Qué tan grave suena el Coleccionista disfrazado (menos de 1: más grave; 1: su voz normal). */
    public static float tonoDisfraz = 0.78F;

    private Voz() {
    }

    public static void init() {
        AxelisCore.onServerTick(Voz::tick);
    }

    private static void tick(MinecraftServer s) {
        if (++n % 10 == 0) {
            actualizar(s);
        }
    }

    /** Copia quién está vivo, quién es fantasma y quién organiza para el hilo de la voz (cada medio segundo, y al morir o revivir). */
    public static void actualizar(MinecraftServer s) {
        Evento ev = Evento.get(s);
        Set<UUID> vivos = new HashSet<>(), fantasmas = new HashSet<>(), org = new HashSet<>();
        for (Pieza p : ev.piezas()) {
            if (!p.bot) {
                (p.vivo() ? vivos : fantasmas).add(p.id);
            }
        }
        for (var sp : s.getPlayerList().getPlayers()) {
            if (ev.esOrganizador(sp.getUUID()) && !vivos.contains(sp.getUUID()) && !fantasmas.contains(sp.getUUID())) {
                org.add(sp.getUUID());
            }
        }
        Map<UUID, Float> disfrazados = new HashMap<>();
        if (tonoDisfraz > 0F && Math.abs(tonoDisfraz - 1F) > 0.01F) {
            for (UUID id : ev.disfrazados()) {
                disfrazados.put(id, tonoDisfraz);
            }
        }
        VozPuente.tonosDisfraz = Map.copyOf(disfrazados);
        VozPuente.vivos = Set.copyOf(vivos);
        VozPuente.fantasmas = Set.copyOf(fantasmas);
        VozPuente.organizadores = Set.copyOf(org);
        VozPuente.altavoz = Set.copyOf(ALTAVOZ);
        VozPuente.separados = separados;
        Runnable r = VozPuente.sincronizar;
        if (r != null && n % 40 == 0) {
            r.run();
        }
    }

    /** Durante una prueba por grupos: cada pieza solo oye a las de su grupo (los fantasmas y el organizador oyen a todos). */
    public static void separar(Map<UUID, Integer> grupos) {
        if (!VozPuente.separarSalas) {
            return;
        }
        separados = Map.copyOf(new HashMap<>(grupos));
        VozPuente.separados = separados;
        Coleccionista.LOGGER.info("COLECCIONISTA_VOZ separados={}", separados.size());
    }

    public static void juntar() {
        if (!separados.isEmpty()) {
            separados = Map.of();
            VozPuente.separados = separados;
            Coleccionista.LOGGER.info("COLECCIONISTA_VOZ juntos");
        }
    }

    /** Las voces cambiadas: a quién y con qué tono (vacío = todos con su voz). */
    public static void tonos(Map<UUID, Float> t) {
        VozPuente.tonos = Map.copyOf(t);
        Coleccionista.LOGGER.info("COLECCIONISTA_VOZ tonos={}", t.size());
    }

    /** A quién se le corta el micrófono (vacío = a nadie). */
    /** La clave con que cada jugador sabe si lleva la cinta (para su micrófono y su aviso). */
    public static final String KEY_MUDO = Coleccionista.MODID + ":mudo";

    /** Le avisa a cada jugador conectado si lleva la cinta (la de la prueba o la de {@code /evento probar cinta}). */
    public static void avisarMudos() {
        MinecraftServer s = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
        if (s == null) {
            return;
        }
        for (net.minecraft.server.level.ServerPlayer sp : s.getPlayerList().getPlayers()) {
            boolean m = VozPuente.mudos.contains(sp.getUUID()) || VozPuente.mudosPrueba.contains(sp.getUUID());
            net.minecraft.nbt.CompoundTag t = new net.minecraft.nbt.CompoundTag();
            t.putBoolean("mudo", m);
            com.axeliscore.data.PlayerStore.Sync.toPlayer(sp, KEY_MUDO, t);
        }
    }

    public static void mudos(Set<UUID> ids) {
        VozPuente.mudos = Set.copyOf(ids);
        avisarMudos();
        Coleccionista.LOGGER.info("COLECCIONISTA_VOZ mudos={}", ids.size());
    }

    public static boolean altavoz(UUID id) {
        return ALTAVOZ.contains(id);
    }

    public static void altavoz(UUID id, boolean si) {
        if (si) {
            ALTAVOZ.add(id);
        } else {
            ALTAVOZ.remove(id);
        }
        VozPuente.altavoz = Set.copyOf(ALTAVOZ);
        Coleccionista.LOGGER.info("COLECCIONISTA_VOZ altavoz {} {}", id, si);
    }
}
