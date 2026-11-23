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
