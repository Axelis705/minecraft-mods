package com.celular.net;

import com.celular.call.CallManager;
import com.celular.client.ClientHooks;
import com.celular.insta.Instacraft;
import com.celular.patreon.Patreon;
import com.celular.plat.Net;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Todos los mensajes del celular entre cliente y servidor. */
public final class CelularNet {
    /** Tamaño máximo de cada pedazo de foto que sube el cliente (los mensajes al servidor tienen un límite de ~32 KB). */
    public static final int CHUNK = 28000;
    /** Tamaño máximo de una foto ya comprimida. */
    public static final int MAX_PHOTO = 96 * 1024;

    private CelularNet() {
    }

    public static void register() {
        int id = 0;
        // ---- del cliente al servidor
        Net.register(id++, Upload.class, Upload::write, Upload::read, (m, p) -> Instacraft.upload(p, m));
        Net.register(id++, FeedReq.class, FeedReq::write, FeedReq::read, (m, p) -> Instacraft.sendFeed(p, m.author));
        Net.register(id++, ImgReq.class, ImgReq::write, ImgReq::read, (m, p) -> Instacraft.sendImage(p, m.id));
        Net.register(id++, Like.class, Like::write, Like::read, (m, p) -> Instacraft.like(p, m.id));
        Net.register(id++, Delete.class, Delete::write, Delete::read, (m, p) -> Instacraft.delete(p, m.id));
        Net.register(id++, CallAct.class, CallAct::write, CallAct::read, (m, p) -> CallManager.action(p, m.action, m.target));
        Net.register(id++, Simple.class, Simple::write, Simple::read, CelularNet::simple);
        // ---- del servidor al cliente
        Net.register(id++, Feed.class, Feed::write, Feed::read, (m, p) -> Net.onClient(() -> ClientHooks.feed(m)));
        Net.register(id++, Img.class, Img::write, Img::read, (m, p) -> Net.onClient(() -> ClientHooks.image(m)));
        Net.register(id++, PostUpd.class, PostUpd::write, PostUpd::read, (m, p) -> Net.onClient(() -> ClientHooks.postUpdate(m.post)));
        Net.register(id++, CallState.class, CallState::write, CallState::read, (m, p) -> Net.onClient(() -> ClientHooks.callState(m)));
        Net.register(id++, Names.class, Names::write, Names::read, (m, p) -> Net.onClient(() -> ClientHooks.names(m)));
        Net.register(id++, Contacts.class, Contacts::write, Contacts::read, (m, p) -> Net.onClient(() -> ClientHooks.contacts(m)));
        Net.register(id++, Notify.class, Notify::write, Notify::read, (m, p) -> Net.onClient(() -> ClientHooks.notify(m)));
        // ---- música del mundo (altavoces y Jams)
        Net.register(id++, RadioNet.SongUp.class, RadioNet.SongUp::write, RadioNet.SongUp::read, (m, p) -> {
            if (p != null) {
                com.celular.radio.WorldMusic.upload(p, m);
            }
        });
        Net.register(id++, RadioNet.Req.class, RadioNet.Req::write, RadioNet.Req::read, CelularNet::radio);
        Net.register(id++, RadioNet.SpeakerSet.class, RadioNet.SpeakerSet::write, RadioNet.SpeakerSet::read, (m, p) -> {
            if (p != null) {
                com.celular.radio.Speakers.set(p, m);
            }
        });
        Net.register(id++, RadioNet.SongAck.class, RadioNet.SongAck::write, RadioNet.SongAck::read, (m, p) -> Net.onClient(() -> com.celular.client.Radio.ack(m)));
        Net.register(id++, RadioNet.Library.class, RadioNet.Library::write, RadioNet.Library::read, (m, p) -> Net.onClient(() -> com.celular.client.Radio.library(m)));
        Net.register(id++, RadioNet.SongData.class, RadioNet.SongData::write, RadioNet.SongData::read, (m, p) -> Net.onClient(() -> com.celular.client.Radio.data(m)));
        Net.register(id++, RadioNet.SpeakerState.class, RadioNet.SpeakerState::write, RadioNet.SpeakerState::read, (m, p) -> Net.onClient(() -> com.celular.client.Radio.speaker(m)));
        Net.register(id++, RadioNet.JamState.class, RadioNet.JamState::write, RadioNet.JamState::read, (m, p) -> Net.onClient(() -> com.celular.client.Radio.jam(m)));
        Net.register(id++, RadioNet.JamList.class, RadioNet.JamList::write, RadioNet.JamList::read, (m, p) -> Net.onClient(() -> com.celular.client.Radio.jamList(m)));
        // ---- qué pantalla muestra el celular (para que los demás lo vean) y la linterna
        Net.register(id++, UseScreen.class, UseScreen::write, UseScreen::read, (m, p) -> {
            if (p != null) {
                com.celular.PhoneUse.set(p, m.screen);
            }
        });
        // ---- Mensajes, Mapa con amigos y Billetera
        Net.register(id++, AppNet.Req.class, AppNet.Req::write, AppNet.Req::read, CelularNet::app);
        Net.register(id++, AppNet.MsgList.class, AppNet.MsgList::write, AppNet.MsgList::read, (m, p) -> Net.onClient(() -> com.celular.client.Chats.list(m)));
        Net.register(id++, AppNet.MsgThread.class, AppNet.MsgThread::write, AppNet.MsgThread::read, (m, p) -> Net.onClient(() -> com.celular.client.Chats.thread(m)));
        Net.register(id++, AppNet.MsgNew.class, AppNet.MsgNew::write, AppNet.MsgNew::read, (m, p) -> Net.onClient(() -> com.celular.client.Chats.incoming(m)));
        Net.register(id++, AppNet.Friends.class, AppNet.Friends::write, AppNet.Friends::read, (m, p) -> Net.onClient(() -> com.celular.client.Chats.friends(m)));
        Net.register(id++, AppNet.WalletHist.class, AppNet.WalletHist::write, AppNet.WalletHist::read, (m, p) -> Net.onClient(() -> com.celular.client.Chats.walletHist(m)));
        Net.done();
    }

    private static void radio(RadioNet.Req m, ServerPlayer p) {
        if (p == null) {
            return;
        }
        switch (m.what) {
            case RadioNet.Req.LIB -> com.celular.radio.WorldMusic.sendLibrary(p);
            case RadioNet.Req.DELETE -> com.celular.radio.WorldMusic.delete(p, m.a);
            case RadioNet.Req.GET -> com.celular.radio.WorldMusic.download(p, m.a, (int) m.b);
            case RadioNet.Req.UP_CANCEL -> com.celular.radio.WorldMusic.cancel(p);
            default -> com.celular.radio.Jams.handle(p, m);
        }
    }

    private static void app(AppNet.Req m, ServerPlayer p) {
        if (p == null) {
            return;
        }
        switch (m.what) {
            case AppNet.Req.MAP -> com.celular.map.Friends.send(p);
            case AppNet.Req.SHARE -> com.celular.map.Friends.share(p, m.n != 0);
            case AppNet.Req.WALLET_SEND -> com.celular.wallet.Wallet.send(p, m.who, m.n);
            case AppNet.Req.WALLET_HIST -> com.celular.wallet.Wallet.sendHist(p);
            case AppNet.Req.TAKE_OUT -> com.celular.PhoneUse.takeOut(p);
            default -> com.celular.msg.Messages.handle(p, m);
        }
    }

    private static void simple(Simple m, ServerPlayer p) {
        if (p == null) {
            return;
        }
        switch (m.what) {
            case Simple.PATREON -> Net.toPlayer(p, new Names(Names.PATREON, Patreon.list()));
            case Simple.CONTACTS -> Net.toPlayer(p, CallManager.contacts(p));
            default -> {
            }
        }
    }

    // =====================================================================================================================
    //  Mensajes
    // =====================================================================================================================

    /** Un pedazo de una foto que se publica. El primero (index 0) lleva el texto. */
    public static final class Upload {
        public final int session, index, total;
        public final String caption;
        public final byte[] data;

        public Upload(int session, int index, int total, String caption, byte[] data) {
            this.session = session;
            this.index = index;
            this.total = total;
            this.caption = caption;
            this.data = data;
        }

        void write(FriendlyByteBuf b) {
            b.writeVarInt(session);
            b.writeVarInt(index);
            b.writeVarInt(total);
            b.writeUtf(caption, 300);
            b.writeByteArray(data);
        }

        static Upload read(FriendlyByteBuf b) {
            return new Upload(b.readVarInt(), b.readVarInt(), b.readVarInt(), b.readUtf(300), b.readByteArray(CHUNK + 64));
        }
    }

    /** Pide el muro de Instacraft (de todos, o de una persona si author no es null). */
    public static final class FeedReq {
        public final UUID author;

        public FeedReq(UUID author) {
            this.author = author;
        }

        void write(FriendlyByteBuf b) {
            b.writeBoolean(author != null);
            if (author != null) {
                b.writeUUID(author);
            }
        }

        static FeedReq read(FriendlyByteBuf b) {
            return new FeedReq(b.readBoolean() ? b.readUUID() : null);
        }
    }

    public static final class ImgReq {
        public final long id;

        public ImgReq(long id) {
            this.id = id;
        }

        void write(FriendlyByteBuf b) {
            b.writeLong(id);
        }

        static ImgReq read(FriendlyByteBuf b) {
            return new ImgReq(b.readLong());
        }
    }

    public static final class Like {
        public final long id;

        public Like(long id) {
            this.id = id;
        }

        void write(FriendlyByteBuf b) {
            b.writeLong(id);
        }

        static Like read(FriendlyByteBuf b) {
            return new Like(b.readLong());
        }
    }

    public static final class Delete {
        public final long id;

        public Delete(long id) {
            this.id = id;
        }

        void write(FriendlyByteBuf b) {
            b.writeLong(id);
        }

        static Delete read(FriendlyByteBuf b) {
            return new Delete(b.readLong());
        }
    }

    /** Llamar, contestar, rechazar o colgar. */
    public static final class CallAct {
        public static final int DIAL = 0, ACCEPT = 1, DECLINE = 2, HANGUP = 3;
        public final int action;
        public final UUID target;

        public CallAct(int action, UUID target) {
            this.action = action;
            this.target = target;
        }

        void write(FriendlyByteBuf b) {
            b.writeVarInt(action);
            b.writeBoolean(target != null);
            if (target != null) {
                b.writeUUID(target);
            }
        }

        static CallAct read(FriendlyByteBuf b) {
            int a = b.readVarInt();
            return new CallAct(a, b.readBoolean() ? b.readUUID() : null);
        }
    }

    /** Peticiones sin datos: la lista de Patreon o la de contactos. */
    /** El celular dice qué app tiene abierta (0 = guardado; {@link com.celular.PhoneUse#FLASH} = linterna encendida). */
    public static final class UseScreen {
        public final int screen;

        public UseScreen(int screen) {
            this.screen = screen;
        }

        void write(FriendlyByteBuf b) {
            b.writeVarInt(screen);
        }

        static UseScreen read(FriendlyByteBuf b) {
            return new UseScreen(b.readVarInt());
        }
    }

    public static final class Simple {
        public static final int PATREON = 0, CONTACTS = 1;
        public final int what;

        public Simple(int what) {
            this.what = what;
        }

        void write(FriendlyByteBuf b) {
            b.writeVarInt(what);
        }

        static Simple read(FriendlyByteBuf b) {
            return new Simple(b.readVarInt());
        }
    }

    /** Una publicación de Instacraft como la ve un jugador. */
    public static final class Post {
        public final long id;
        public final UUID author;
        public final String name, caption;
        public final long time;
        public final int likes;
        public final boolean liked, mine;

        public Post(long id, UUID author, String name, String caption, long time, int likes, boolean liked, boolean mine) {
            this.id = id;
            this.author = author;
            this.name = name;
            this.caption = caption;
            this.time = time;
            this.likes = likes;
            this.liked = liked;
            this.mine = mine;
        }

        void write(FriendlyByteBuf b) {
            b.writeLong(id);
            b.writeUUID(author);
            b.writeUtf(name, 64);
            b.writeUtf(caption, 300);
            b.writeLong(time);
            b.writeVarInt(likes);
            b.writeBoolean(liked);
            b.writeBoolean(mine);
        }

        static Post read(FriendlyByteBuf b) {
            return new Post(b.readLong(), b.readUUID(), b.readUtf(64), b.readUtf(300), b.readLong(), b.readVarInt(), b.readBoolean(),
                    b.readBoolean());
        }
    }

    public static final class Feed {
        public final UUID author;
        public final List<Post> posts;

        public Feed(UUID author, List<Post> posts) {
            this.author = author;
            this.posts = posts;
        }

        void write(FriendlyByteBuf b) {
            b.writeBoolean(author != null);
            if (author != null) {
                b.writeUUID(author);
            }
            b.writeVarInt(posts.size());
            for (Post p : posts) {
                p.write(b);
            }
        }

        static Feed read(FriendlyByteBuf b) {
            UUID a = b.readBoolean() ? b.readUUID() : null;
            int n = Math.min(b.readVarInt(), 500);
            List<Post> l = new ArrayList<>(n);
            for (int i = 0; i < n; i++) {
                l.add(Post.read(b));
            }
            return new Feed(a, l);
        }
    }

    public static final class Img {
        public final long id;
        public final byte[] data;

        public Img(long id, byte[] data) {
            this.id = id;
            this.data = data;
        }

        void write(FriendlyByteBuf b) {
            b.writeLong(id);
            b.writeByteArray(data);
        }

        static Img read(FriendlyByteBuf b) {
            return new Img(b.readLong(), b.readByteArray(MAX_PHOTO + 64));
        }
    }

    public static final class PostUpd {
        public final Post post;

        public PostUpd(Post post) {
            this.post = post;
        }

        void write(FriendlyByteBuf b) {
            post.write(b);
        }

        static PostUpd read(FriendlyByteBuf b) {
            return new PostUpd(Post.read(b));
        }
    }

    /** Cómo va la llamada de este jugador. */
    public static final class CallState {
        public static final int IDLE = 0, DIALING = 1, INCOMING = 2, ACTIVE = 3;
        /** Por qué terminó (o no se pudo hacer) la llamada. */
        public static final int R_NONE = 0, R_ENDED = 1, R_BUSY = 2, R_NO_PHONE = 3, R_OFFLINE = 4, R_DECLINED = 5, R_NO_ANSWER = 6,
                R_SELF = 7, R_MISSED = 8;
        public final int state, reason;
        public final UUID other;
        public final String name;
        /** Segundos que ya lleva la llamada (para el reloj). */
        public final int seconds;
        public final boolean voice;

        public CallState(int state, int reason, UUID other, String name, int seconds, boolean voice) {
            this.state = state;
            this.reason = reason;
            this.other = other;
            this.name = name;
            this.seconds = seconds;
            this.voice = voice;
        }

        void write(FriendlyByteBuf b) {
            b.writeVarInt(state);
            b.writeVarInt(reason);
            b.writeBoolean(other != null);
            if (other != null) {
                b.writeUUID(other);
            }
            b.writeUtf(name == null ? "" : name, 64);
            b.writeVarInt(seconds);
            b.writeBoolean(voice);
        }

        static CallState read(FriendlyByteBuf b) {
            int s = b.readVarInt(), r = b.readVarInt();
            UUID o = b.readBoolean() ? b.readUUID() : null;
            return new CallState(s, r, o, b.readUtf(64), b.readVarInt(), b.readBoolean());
        }
    }

    /** Una lista de nombres (los que apoyan en Patreon). */
    public static final class Names {
        public static final int PATREON = 0;
        public final int what;
        public final List<String> names;

        public Names(int what, List<String> names) {
            this.what = what;
            this.names = names;
        }

        void write(FriendlyByteBuf b) {
            b.writeVarInt(what);
            b.writeVarInt(names.size());
            for (String s : names) {
                b.writeUtf(s, 64);
            }
        }

        static Names read(FriendlyByteBuf b) {
            int w = b.readVarInt(), n = Math.min(b.readVarInt(), 1000);
            List<String> l = new ArrayList<>(n);
            for (int i = 0; i < n; i++) {
                l.add(b.readUtf(64));
            }
            return new Names(w, l);
        }
    }

    /** Los jugadores conectados a los que se puede llamar. */
    public static final class Contacts {
        public final List<UUID> ids = new ArrayList<>();
        public final List<String> names = new ArrayList<>();
        public final List<Boolean> phone = new ArrayList<>();

        void write(FriendlyByteBuf b) {
            b.writeVarInt(ids.size());
            for (int i = 0; i < ids.size(); i++) {
                b.writeUUID(ids.get(i));
                b.writeUtf(names.get(i), 64);
                b.writeBoolean(phone.get(i));
            }
        }

        static Contacts read(FriendlyByteBuf b) {
            Contacts c = new Contacts();
            int n = Math.min(b.readVarInt(), 1000);
            for (int i = 0; i < n; i++) {
                c.ids.add(b.readUUID());
                c.names.add(b.readUtf(64));
                c.phone.add(b.readBoolean());
            }
            return c;
        }
    }

    /** Una notificación en el celular. */
    public static final class Notify {
        public static final int LIKE = 0, NEW_POST = 1, MISSED_CALL = 2, POSTED = 3, ERROR = 4, JAM_INVITE = 5, WALLET = 6, WALLET_SENT = 7,
                WALLET_IN = 8, DJ = 9, SPEAKER_PAIR = 10, TAKE_OUT = 11;
        public final int type;
        public final String who;
        /** Texto extra (o una clave de traducción en ERROR). */
        public final String text;

        public Notify(int type, String who, String text) {
            this.type = type;
            this.who = who;
            this.text = text;
        }

        void write(FriendlyByteBuf b) {
            b.writeVarInt(type);
            b.writeUtf(who, 64);
            b.writeUtf(text, 300);
        }

        static Notify read(FriendlyByteBuf b) {
            return new Notify(b.readVarInt(), b.readUtf(64), b.readUtf(300));
        }
    }
}
