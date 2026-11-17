package com.celular.net;

import com.celular.radio.SongInfo;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Prueba de los mensajes de red (la usa {@code /celular prueba}): escribe cada mensaje y lo vuelve a leer, y revisa que llegue igual
 * y que no sobren ni falten bytes. Así un cambio en un lado y no en el otro se nota en el CI, en todas las versiones.
 */
public final class NetTest {
    private NetTest() {
    }

    private interface W {
        void write(FriendlyByteBuf b);
    }

    private static FriendlyByteBuf buf(W w) {
        FriendlyByteBuf b = new FriendlyByteBuf(Unpooled.buffer());
        w.write(b);
        return b;
    }

    private static SongInfo song(long id) {
        SongInfo s = new SongInfo();
        s.id = id;
        s.hash = "0123456789abcdef0123456789abcdef01234567";
        s.ext = "ogg";
        s.title = "Canción ñ " + id;
        s.artist = "Artista";
        s.uploader = "Axelis";
        s.uploaderId = UUID.randomUUID().toString();
        s.size = 1234567;
        s.seconds = 215;
        s.added = 1794896128887L;
        return s;
    }

    /** Devuelve cuántas fallaron; cada resultado se avisa por {@code log}. */
    public static int run(Consumer<String> log) {
        int bad = 0;
        try {
            SongInfo a = song(77);
            FriendlyByteBuf b = buf(a::write);
            SongInfo a2 = SongInfo.read(b);
            bad += check(log, "cancion", a2.id == 77 && a2.title.equals(a.title) && a2.size == a.size && a2.seconds == 215 && a2.added == a.added
                    && a2.hash.equals(a.hash) && b.readableBytes() == 0);

            RadioNet.JamState j = new RadioNet.JamState(5, "Fiesta", "Axelis", true, List.of("Axelis", "Odette"), List.of(song(1), song(2)),
                    List.of("Axelis", "Odette"), 1, 42000, true, 1, 2);
            b = buf(j::write);
            RadioNet.JamState j2 = RadioNet.JamState.read(b);
            bad += check(log, "jam", j2.id == 5 && j2.name.equals("Fiesta") && j2.amHost && j2.members.size() == 2 && j2.queue.size() == 2
                    && j2.addedBy.get(1).equals("Odette") && j2.index == 1 && j2.posMs == 42000 && j2.playing && j2.votes == 1 && j2.needed == 2
                    && j2.current().id == 2 && b.readableBytes() == 0);

            RadioNet.Req r = new RadioNet.Req(RadioNet.Req.JAM_INVITE, 9, 3, "texto");
            b = buf(r::write);
            RadioNet.Req r2 = RadioNet.Req.read(b);
            bad += check(log, "pedido_musica", r2.what == RadioNet.Req.JAM_INVITE && r2.a == 9 && r2.b == 3 && r2.s.equals("texto")
                    && b.readableBytes() == 0);

            UUID u = UUID.randomUUID();
            RadioNet.SpeakerState sp = new RadioNet.SpeakerState(u, "Axelis", 321, song(4), 9000, true);
            b = buf(sp::write);
            RadioNet.SpeakerState sp2 = RadioNet.SpeakerState.read(b);
            RadioNet.SpeakerState off = new RadioNet.SpeakerState(u, "Axelis", 321, null, 0, false);
            FriendlyByteBuf b2 = buf(off::write);
            RadioNet.SpeakerState off2 = RadioNet.SpeakerState.read(b2);
            bad += check(log, "altavoz", sp2.owner.equals(u) && sp2.source == 321 && sp2.song != null && sp2.song.id == 4 && sp2.posMs == 9000
                    && sp2.playing && off2.song == null && b.readableBytes() == 0 && b2.readableBytes() == 0);

            byte[] data = new byte[RadioNet.DOWN_CHUNK];
            data[0] = 1;
            data[data.length - 1] = 2;
            RadioNet.SongData sd = new RadioNet.SongData(7, 3, 9, data);
            b = buf(sd::write);
            RadioNet.SongData sd2 = RadioNet.SongData.read(b);
            bad += check(log, "pedazo_cancion", sd2.id == 7 && sd2.index == 3 && sd2.total == 9 && sd2.data.length == data.length
                    && sd2.data[0] == 1 && sd2.data[data.length - 1] == 2 && b.readableBytes() == 0);

            AppNet.Req q = new AppNet.Req(AppNet.Req.WALLET_SEND, u, 250, "");
            b = buf(q::write);
            AppNet.Req q2 = AppNet.Req.read(b);
            AppNet.Req none = new AppNet.Req(AppNet.Req.MSG_LIST);
            b2 = buf(none::write);
            AppNet.Req none2 = AppNet.Req.read(b2);
            bad += check(log, "pedido_apps", q2.what == AppNet.Req.WALLET_SEND && q2.who.equals(u) && q2.n == 250 && none2.what == AppNet.Req.MSG_LIST
                    && b.readableBytes() == 0 && b2.readableBytes() == 0);

            AppNet.MsgList ml = new AppNet.MsgList();
            AppNet.Conv c = new AppNet.Conv();
            c.id = u;
            c.name = "Odette";
            c.last = "hola 😀 ñ";
            c.lastMine = true;
            c.time = 123;
            c.unread = 4;
            c.online = true;
            ml.convs.add(c);
            b = buf(ml::write);
            AppNet.MsgList ml2 = AppNet.MsgList.read(b);
            AppNet.Conv c2 = ml2.convs.get(0);
            bad += check(log, "lista_mensajes", c2.id.equals(u) && c2.last.equals(c.last) && c2.lastMine && c2.unread == 4 && c2.online
                    && b.readableBytes() == 0);

            AppNet.Msg m = new AppNet.Msg();
            m.mine = false;
            m.from = "Odette";
            m.text = "¿vienes?";
            m.time = 99;
            AppNet.MsgNew mn = new AppNet.MsgNew(u, "Odette", m);
            b = buf(mn::write);
            AppNet.MsgNew mn2 = AppNet.MsgNew.read(b);
            AppNet.MsgThread th = new AppNet.MsgThread(u, "Odette", false, new java.util.ArrayList<>(List.of(m, m)));
            b2 = buf(th::write);
            AppNet.MsgThread th2 = AppNet.MsgThread.read(b2);
            bad += check(log, "mensaje", mn2.conv.equals(u) && mn2.msg.text.equals("¿vienes?") && mn2.msg.time == 99 && th2.msgs.size() == 2
                    && !th2.online && b.readableBytes() == 0 && b2.readableBytes() == 0);

            AppNet.Friends f = new AppNet.Friends();
            f.sharing = true;
            f.ids.add(u);
            f.names.add("Odette");
            f.pos.add(new double[]{10.5, 64, -300.25, 90});
            b = buf(f::write);
            AppNet.Friends f2 = AppNet.Friends.read(b);
            bad += check(log, "amigos", f2.sharing && f2.ids.get(0).equals(u) && f2.pos.get(0)[2] == -300.25 && f2.pos.get(0)[3] == 90
                    && b.readableBytes() == 0);

            AppNet.WalletHist h = new AppNet.WalletHist();
            h.who.add("Odette");
            h.amount.add(-250);
            h.time.add(5L);
            h.who.add("Axelis");
            h.amount.add(1000);
            h.time.add(6L);
            b = buf(h::write);
            AppNet.WalletHist h2 = AppNet.WalletHist.read(b);
            bad += check(log, "billetera", h2.amount.get(0) == -250 && h2.amount.get(1) == 1000 && h2.who.get(1).equals("Axelis")
                    && b.readableBytes() == 0);

            CelularNet.UseScreen us = new CelularNet.UseScreen(16);
            b = buf(us::write);
            bad += check(log, "pantalla_red", CelularNet.UseScreen.read(b).screen == 16 && b.readableBytes() == 0);
        } catch (Throwable e) {
            log.accept("red FALLO " + e);
            bad++;
        }
        return bad;
    }

    private static int check(Consumer<String> log, String name, boolean ok) {
        log.accept(name + (ok ? " OK" : " FALLO"));
        return ok ? 0 : 1;
    }
}
