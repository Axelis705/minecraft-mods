package com.celular.voice;

import com.celular.call.CallManager;
import de.maxhenkel.voicechat.api.ForgeVoicechatPlugin;
import de.maxhenkel.voicechat.api.VoicechatApi;
import de.maxhenkel.voicechat.api.VoicechatConnection;
import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import de.maxhenkel.voicechat.api.audiochannel.StaticAudioChannel;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import de.maxhenkel.voicechat.api.events.MicrophonePacketEvent;
import de.maxhenkel.voicechat.api.events.PlayerDisconnectedEvent;
import de.maxhenkel.voicechat.api.events.VoicechatServerStartedEvent;
import de.maxhenkel.voicechat.api.opus.OpusDecoder;
import de.maxhenkel.voicechat.api.opus.OpusEncoder;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Llamadas con voz usando Simple Voice Chat (solo si está instalado; si no, esta clase nunca se carga). Lo que dice cada uno en una
 * llamada le llega al otro por un canal directo, sin importar la distancia, con filtro de teléfono (solo los tonos de voz, un poco de
 * saturación y un leve siseo de línea). Los que están cerca siguen oyendo normal a quien habla, como en la vida real.
 */
@ForgeVoicechatPlugin
public class VoicePlugin implements VoicechatPlugin {
    private static volatile VoicechatServerApi server;
    /** Por quien habla: su decodificador, el codificador y el canal hacia la otra persona (y su filtro, que guarda estado). */
    private static final Map<UUID, Line> LINES = new ConcurrentHashMap<>();

    private static final class Line {
        UUID to;
        OpusDecoder dec;
        OpusEncoder enc;
        StaticAudioChannel channel;
        final PhoneFilter filter = new PhoneFilter();

        void close() {
            if (dec != null) {
                dec.close();
            }
            if (enc != null) {
                enc.close();
            }
        }
    }

    @Override
    public String getPluginId() {
        return "celular";
    }

    @Override
    public void initialize(VoicechatApi api) {
        VoiceBridge.available = true;
    }

    @Override
    public void registerEvents(EventRegistration r) {
        r.registerEvent(VoicechatServerStartedEvent.class, e -> server = e.getVoicechat());
        r.registerEvent(MicrophonePacketEvent.class, this::onMic);
        r.registerEvent(PlayerDisconnectedEvent.class, e -> {
            Line l = LINES.remove(e.getPlayerUuid());
            if (l != null) {
                l.close();
            }
        });
    }

    private void onMic(MicrophonePacketEvent e) {
        VoicechatServerApi api = server;
        VoicechatConnection from = e.getSenderConnection();
        if (api == null || from == null) {
            return;
        }
        UUID me = from.getPlayer().getUuid();
        UUID other = CallManager.voicePartner(me);
        if (other == null) {
            Line old = LINES.remove(me);
            if (old != null) {
                old.close();
            }
            return;
        }
        VoicechatConnection to = api.getConnectionOf(other);
        if (to == null || !to.isConnected()) {
            return;
        }
        Line l = LINES.get(me);
        if (l == null || !other.equals(l.to) || l.channel == null || l.channel.isClosed()) {
            if (l != null) {
                l.close();
            }
            l = new Line();
            l.to = other;
            l.dec = api.createDecoder();
            l.enc = api.createEncoder();
            l.channel = api.createStaticAudioChannel(UUID.randomUUID(), to.getPlayer().getServerLevel(), to);
            if (l.channel == null) {
                return;
            }
            LINES.put(me, l);
        }
        byte[] data = e.getPacket().getOpusEncodedData();
        if (data == null || data.length == 0) {
            return;
        }
        short[] pcm = l.dec.decode(data);
        if (pcm == null || pcm.length == 0) {
            return;
        }
        l.filter.apply(pcm);
        l.channel.send(l.enc.encode(pcm));
    }
}
