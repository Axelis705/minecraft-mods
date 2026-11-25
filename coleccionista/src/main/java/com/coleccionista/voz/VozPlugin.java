package com.coleccionista.voz;

import com.coleccionista.Coleccionista;
import de.maxhenkel.voicechat.api.ForgeVoicechatPlugin;
import de.maxhenkel.voicechat.api.VoicechatApi;
import de.maxhenkel.voicechat.api.VoicechatConnection;
import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import de.maxhenkel.voicechat.api.audiochannel.StaticAudioChannel;
import de.maxhenkel.voicechat.api.events.CreateGroupEvent;
import de.maxhenkel.voicechat.api.events.EntitySoundPacketEvent;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import de.maxhenkel.voicechat.api.events.JoinGroupEvent;
import de.maxhenkel.voicechat.api.events.LocationalSoundPacketEvent;
import de.maxhenkel.voicechat.api.events.MicrophonePacketEvent;
import de.maxhenkel.voicechat.api.events.PacketEvent;
import de.maxhenkel.voicechat.api.events.PlayerDisconnectedEvent;
import de.maxhenkel.voicechat.api.events.VoicechatServerStartedEvent;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * El complemento para Simple Voice Chat (solo se carga si está instalado). Reglas:
 * <ul>
 *   <li>Lo que dice un fantasma no llega a nadie por cercanía; va por el "canal de los fantasmas" a los demás fantasmas (y al organizador),
 *   en todo el mapa.</li>
 *   <li>Los fantasmas oyen a los vivos que están cerca, como siempre.</li>
 *   <li>Con el altavoz, lo que dice el organizador le llega a todos, estén donde estén.</li>
 *   <li>Durante algunas pruebas, cada sala o laberinto solo oye a los suyos.</li>
 *   <li>Las piezas no pueden crear ni entrar a grupos (para que nadie hable en secreto desde lejos).</li>
 * </ul>
 */
@ForgeVoicechatPlugin
public class VozPlugin implements VoicechatPlugin {
    private static volatile VoicechatServerApi server;
    /** Los canales directos de quien habla a quien oye (fantasmas y altavoz). */
    private static final Map<String, StaticAudioChannel> CANALES = new ConcurrentHashMap<>();
    /** Para cambiar la voz: lo que hace falta de cada quien que habla (decodificador, cambio de tono y codificador). */
    private static final Map<UUID, Disfraz> DISFRACES = new ConcurrentHashMap<>();

    private static final class Disfraz {
        final de.maxhenkel.voicechat.api.opus.OpusDecoder dec;
        final de.maxhenkel.voicechat.api.opus.OpusEncoder enc;
        final Tono tono;

        Disfraz(VoicechatServerApi api, float razon) {
            dec = api.createDecoder();
            enc = api.createEncoder(de.maxhenkel.voicechat.api.opus.OpusEncoderMode.VOIP);
            tono = new Tono(razon);
        }

        void cerrar() {
            dec.close();
            enc.close();
        }
    }

    @Override
    public String getPluginId() {
        return Coleccionista.MODID;
    }

    @Override
    public void initialize(VoicechatApi api) {
        VozPuente.disponible = true;
        VozPuente.sincronizar = VozPlugin::sincronizar;
        Coleccionista.LOGGER.info("COLECCIONISTA_VOZ Simple Voice Chat listo");
    }

    @Override
    public void registerEvents(EventRegistration r) {
        r.registerEvent(VoicechatServerStartedEvent.class, e -> server = e.getVoicechat());
        r.registerEvent(MicrophonePacketEvent.class, this::alHablar);
        r.registerEvent(EntitySoundPacketEvent.class, this::filtrar);
        r.registerEvent(LocationalSoundPacketEvent.class, this::filtrar);
        r.registerEvent(JoinGroupEvent.class, e -> {
            VoicechatConnection c = e.getConnection();
            if (c != null && !VozPuente.puedeGrupo(c.getPlayer().getUuid())) {
                e.cancel();
            }
        });
        r.registerEvent(CreateGroupEvent.class, e -> {
            VoicechatConnection c = e.getConnection();
            if (c != null && !VozPuente.puedeGrupo(c.getPlayer().getUuid())) {
                e.cancel();
            }
        });
        r.registerEvent(PlayerDisconnectedEvent.class, e -> {
            String id = e.getPlayerUuid().toString();
            CANALES.keySet().removeIf(k -> k.contains(id));
            Disfraz d = DISFRACES.remove(e.getPlayerUuid());
            if (d != null) {
                d.cerrar();
            }
        });
    }

    /** La voz por cercanía: ¿la oye este receptor? */
    private void filtrar(PacketEvent<?> e) {
        VoicechatConnection de = e.getSenderConnection(), a = e.getReceiverConnection();
        if (de != null && a != null && !VozPuente.oye(de.getPlayer().getUuid(), a.getPlayer().getUuid())) {
            e.cancel();
        }
    }

    private void alHablar(MicrophonePacketEvent e) {
        VoicechatServerApi api = server;
        VoicechatConnection de = e.getSenderConnection();
        if (api == null || de == null) {
            return;
        }
        UUID yo = de.getPlayer().getUuid();
        if (VozPuente.mudos.contains(yo)) {
            e.cancel(); // tiene la cinta en la boca: nadie lo oye
            return;
        }
        if (disfrazar(api, de, yo, e)) {
            return;
        }
        boolean fantasma = VozPuente.fantasmas.contains(yo), altavoz = VozPuente.altavoz.contains(yo);
        if (!fantasma && !altavoz) {
            return;
        }
        byte[] data = e.getPacket().getOpusEncodedData();
        e.cancel();
        if (data == null || data.length == 0) {
            return;
        }
        for (VoicechatConnection c : conectados(api)) {
            UUID otro = c.getPlayer().getUuid();
            if (otro.equals(yo) || fantasma && !VozPuente.oyeAlFantasma(otro)) {
                continue;
            }
            StaticAudioChannel canal = canal(api, yo, c);
            if (canal != null) {
                canal.send(data);
            }
        }
    }

    /** Hasta dónde se oye la voz cambiada (bloques; la de Simple Voice Chat por defecto es 48). */
    private static final double DISTANCIA_TONO = 32;

    /**
     * Si a esta persona le toca la voz cambiada: se calla el paquete original, se decodifica, se le cambia el tono, se vuelve a codificar y
     * se le manda (por canales directos) a quien lo oiría: los que están cerca y en su mismo grupo (su góndola), los fantasmas y el
     * organizador cercanos. Devuelve si se encargó del paquete.
     */
    private static boolean disfrazar(VoicechatServerApi api, VoicechatConnection de, UUID yo, MicrophonePacketEvent e) {
        Float razon = VozPuente.tonos.get(yo);
        Disfraz d = DISFRACES.get(yo);
        if (razon == null || Math.abs(razon - 1F) < 0.01F) {
            if (d != null) {
                DISFRACES.remove(yo);
                d.cerrar();
            }
            return false;
        }
        if (d == null || Math.abs(d.tono.razon() - razon) > 0.001F) {
            if (d != null) {
                d.cerrar();
            }
            d = new Disfraz(api, razon);
            DISFRACES.put(yo, d);
        }
        byte[] data = e.getPacket().getOpusEncodedData();
        e.cancel();
        byte[] nuevo;
        synchronized (d) {
            if (data == null || data.length == 0) {
                // dejó de hablar: todo de cero para la próxima
                d.dec.resetState();
                d.enc.resetState();
                d.tono.reiniciar();
                return true;
            }
            try {
                short[] pcm = d.dec.decode(data);
                if (pcm == null || pcm.length == 0) {
                    return true;
                }
                nuevo = d.enc.encode(d.tono.procesar(pcm));
            } catch (RuntimeException ex) {
                // si algo falla, que se oiga su voz normal (mejor que callarlo)
                Coleccionista.LOGGER.debug("COLECCIONISTA_VOZ no se pudo cambiar el tono: {}", ex.toString());
                nuevo = data;
            }
        }
        if (nuevo == null || nuevo.length == 0) {
            return true;
        }
        de.maxhenkel.voicechat.api.Position p = de.getPlayer().getPosition();
        for (VoicechatConnection c : conectados(api)) {
            UUID otro = c.getPlayer().getUuid();
            if (otro.equals(yo) || !VozPuente.oye(yo, otro)) {
                continue;
            }
            de.maxhenkel.voicechat.api.Position q = c.getPlayer().getPosition();
            double dx = p.getX() - q.getX(), dy = p.getY() - q.getY(), dz = p.getZ() - q.getZ();
            if (dx * dx + dy * dy + dz * dz > DISTANCIA_TONO * DISTANCIA_TONO) {
                continue;
            }
            StaticAudioChannel canal = canal(api, yo, c);
            if (canal != null) {
                canal.send(nuevo);
            }
        }
        return true;
    }

    private static Iterable<VoicechatConnection> conectados(VoicechatServerApi api) {
        java.util.List<VoicechatConnection> l = new java.util.ArrayList<>();
        Set<UUID> todos = new java.util.HashSet<>(VozPuente.vivos);
        todos.addAll(VozPuente.fantasmas);
        todos.addAll(VozPuente.organizadores);
        for (UUID id : todos) {
            VoicechatConnection c = api.getConnectionOf(id);
            if (c != null && c.isConnected()) {
                l.add(c);
            }
        }
        return l;
    }

    private static StaticAudioChannel canal(VoicechatServerApi api, UUID de, VoicechatConnection a) {
        String k = de + ">" + a.getPlayer().getUuid();
        StaticAudioChannel c = CANALES.get(k);
        if (c == null || c.isClosed()) {
            c = api.createStaticAudioChannel(UUID.nameUUIDFromBytes(k.getBytes(StandardCharsets.UTF_8)), a.getPlayer().getServerLevel(), a);
            if (c == null) {
                return null;
            }
            CANALES.put(k, c);
        }
        return c;
    }

    /** Saca de sus grupos a las piezas que no deben estar en uno (cada dos segundos, desde el hilo del servidor). */
    private static void sincronizar() {
        VoicechatServerApi api = server;
        if (api == null) {
            return;
        }
        Set<UUID> todos = new java.util.HashSet<>(VozPuente.vivos);
        todos.addAll(VozPuente.fantasmas);
        for (UUID id : todos) {
            VoicechatConnection c = api.getConnectionOf(id);
            if (c != null && c.isInGroup() && !VozPuente.puedeGrupo(id)) {
                c.setGroup(null);
            }
        }
    }
}
