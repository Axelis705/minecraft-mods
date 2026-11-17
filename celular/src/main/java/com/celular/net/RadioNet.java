package com.celular.net;

import com.celular.radio.SongInfo;
import net.minecraft.network.FriendlyByteBuf;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Mensajes de la música del mundo: subir y bajar canciones, la lista, los altavoces y los Jams. */
public final class RadioNet {
    /** Pedazo de canción que sube el cliente (los mensajes al servidor tienen un límite de ~32 KB). */
    public static final int UP_CHUNK = 28000;
    /** Pedazo de canción que baja el servidor. */
    public static final int DOWN_CHUNK = 60000;
    /** Cuántos pedazos se piden de una vez al bajar. */
    public static final int DOWN_BATCH = 6;

    private RadioNet() {
    }

    // =====================================================================================================================
    //  Del cliente al servidor
    // =====================================================================================================================

    /** Un pedazo de una canción que se sube. El primero (index 0) lleva el nombre, el artista, el tipo y el tamaño total. */
    public static final class SongUp {
        public final int session, index, total, size;
        public final String ext, title, artist;
        public final byte[] data;

        public SongUp(int session, int index, int total, int size, String ext, String title, String artist, byte[] data) {
            this.session = session;
            this.index = index;
            this.total = total;
            this.size = size;
            this.ext = ext;
            this.title = title;
            this.artist = artist;
            this.data = data;
        }

        void write(FriendlyByteBuf b) {
            b.writeVarInt(session);
            b.writeVarInt(index);
            b.writeVarInt(total);
            b.writeVarInt(size);
            b.writeUtf(ext, 4);
            b.writeUtf(title, 120);
            b.writeUtf(artist, 120);
            b.writeByteArray(data);
        }

        static SongUp read(FriendlyByteBuf b) {
            return new SongUp(b.readVarInt(), b.readVarInt(), b.readVarInt(), b.readVarInt(), b.readUtf(4), b.readUtf(120), b.readUtf(120),
                    b.readByteArray(UP_CHUNK + 64));
        }
    }

    /** Un pedido sencillo: qué ({@code what}) con dos números y un texto. */
    public static final class Req {
        public static final int LIB = 0, DELETE = 1, GET = 2, UP_CANCEL = 3, JAM_LIST = 10, JAM_CREATE = 11, JAM_JOIN = 12, JAM_LEAVE = 13,
                JAM_ADD = 14, JAM_SKIP = 15, JAM_PAUSE = 16, JAM_REMOVE = 17, JAM_INVITE = 18, JAM_DJ = 19;
        public final int what;
        public final long a, b;
        public final String s;

        public Req(int what, long a, long b, String s) {
            this.what = what;
            this.a = a;
            this.b = b;
            this.s = s == null ? "" : s;
        }

        public Req(int what) {
            this(what, 0, 0, "");
        }

        void write(FriendlyByteBuf buf) {
            buf.writeVarInt(what);
            buf.writeLong(a);
            buf.writeLong(b);
            buf.writeUtf(s, 64);
        }

        static Req read(FriendlyByteBuf buf) {
            return new Req(buf.readVarInt(), buf.readLong(), buf.readLong(), buf.readUtf(64));
        }
    }

    /** Lo que suena en mi altavoz: canción (0 = apagado), segundo, si suena y desde qué entidad sale (yo o el celular tirado). */
    public static final class SpeakerSet {
        public final long song;
        public final long posMs;
        public final boolean playing;
        public final int source;

        public SpeakerSet(long song, long posMs, boolean playing, int source) {
            this.song = song;
            this.posMs = posMs;
            this.playing = playing;
            this.source = source;
        }

        void write(FriendlyByteBuf b) {
            b.writeLong(song);
            b.writeLong(posMs);
            b.writeBoolean(playing);
            b.writeVarInt(source);
        }

        static SpeakerSet read(FriendlyByteBuf b) {
            return new SpeakerSet(b.readLong(), b.readLong(), b.readBoolean(), b.readVarInt());
        }
    }

    // =====================================================================================================================
    //  Del servidor al cliente
    // =====================================================================================================================

    /** Respuesta a cada pedazo subido: {@code next} es el siguiente que espera. */
    public static final class SongAck {
        public static final int MORE = 0, DONE = 1, DUPLICATE = 2, ERROR = 3;
        public final int session, next, status;
        /** Clave de traducción del error (o "" si no hay). */
        public final String msg;

        public SongAck(int session, int next, int status, String msg) {
            this.session = session;
            this.next = next;
            this.status = status;
            this.msg = msg;
        }

        void write(FriendlyByteBuf b) {
            b.writeVarInt(session);
            b.writeVarInt(next);
            b.writeVarInt(status);
            b.writeUtf(msg, 120);
        }

        static SongAck read(FriendlyByteBuf b) {
            return new SongAck(b.readVarInt(), b.readVarInt(), b.readVarInt(), b.readUtf(120));
        }
    }

    /** Las canciones del mundo y cuánto espacio queda. */
    public static final class Library {
        public final List<SongInfo> songs;
        public final long used, max;
        public final int maxSongMb, maxPerPlayer;
        public final boolean admin;

        public Library(List<SongInfo> songs, long used, long max, int maxSongMb, int maxPerPlayer, boolean admin) {
            this.songs = songs;
            this.used = used;
            this.max = max;
            this.maxSongMb = maxSongMb;
            this.maxPerPlayer = maxPerPlayer;
            this.admin = admin;
        }

        void write(FriendlyByteBuf b) {
            b.writeVarInt(songs.size());
            for (SongInfo s : songs) {
                s.write(b);
            }
            b.writeLong(used);
            b.writeLong(max);
            b.writeVarInt(maxSongMb);
            b.writeVarInt(maxPerPlayer);
            b.writeBoolean(admin);
        }

        static Library read(FriendlyByteBuf b) {
            int n = Math.min(b.readVarInt(), 5000);
            List<SongInfo> l = new ArrayList<>(n);
            for (int i = 0; i < n; i++) {
                l.add(SongInfo.read(b));
            }
            return new Library(l, b.readLong(), b.readLong(), b.readVarInt(), b.readVarInt(), b.readBoolean());
        }
    }

    /** Un pedazo de una canción que se baja. */
    public static final class SongData {
        public final long id;
        public final int index, total;
        public final byte[] data;

        public SongData(long id, int index, int total, byte[] data) {
            this.id = id;
            this.index = index;
            this.total = total;
            this.data = data;
        }

        void write(FriendlyByteBuf b) {
            b.writeLong(id);
            b.writeVarInt(index);
            b.writeVarInt(total);
            b.writeByteArray(data);
        }

        static SongData read(FriendlyByteBuf b) {
            return new SongData(b.readLong(), b.readVarInt(), b.readVarInt(), b.readByteArray(DOWN_CHUNK + 64));
        }
    }

    /** El altavoz de un jugador (encendido o apagado). */
    public static final class SpeakerState {
        public final UUID owner;
        public final String name;
        public final int source;
        public final SongInfo song;
        public final long posMs;
        public final boolean playing;
        /** Si suena desde una bocina: su posición (BlockPos.asLong); {@link #NO_BLOCK} = desde la entidad {@code source}. */
        public final long block;
        public static final long NO_BLOCK = Long.MIN_VALUE;

        public SpeakerState(UUID owner, String name, int source, SongInfo song, long posMs, boolean playing) {
            this(owner, name, source, song, posMs, playing, NO_BLOCK);
        }

        public SpeakerState(UUID owner, String name, int source, SongInfo song, long posMs, boolean playing, long block) {
            this.owner = owner;
            this.name = name;
            this.source = source;
            this.song = song;
            this.posMs = posMs;
            this.playing = playing;
            this.block = block;
        }

        void write(FriendlyByteBuf b) {
            b.writeUUID(owner);
            b.writeUtf(name, 40);
            b.writeVarInt(source);
            b.writeBoolean(song != null);
            if (song != null) {
                song.write(b);
            }
            b.writeLong(posMs);
            b.writeBoolean(playing);
            b.writeLong(block);
        }

        static SpeakerState read(FriendlyByteBuf b) {
            UUID o = b.readUUID();
            String n = b.readUtf(40);
            int src = b.readVarInt();
            SongInfo s = b.readBoolean() ? SongInfo.read(b) : null;
            return new SpeakerState(o, n, src, s, b.readLong(), b.readBoolean(), b.readLong());
        }
    }

    /** El Jam en el que estoy (id 0 = en ninguno). */
    public static final class JamState {
        public final long id;
        public final String name, host;
        public final boolean amHost;
        public final List<String> members;
        public final List<SongInfo> queue;
        public final List<String> addedBy;
        public final int index;
        public final long posMs;
        public final boolean playing;
        public final int votes, needed;

        public JamState(long id, String name, String host, boolean amHost, List<String> members, List<SongInfo> queue, List<String> addedBy,
                        int index, long posMs, boolean playing, int votes, int needed) {
            this.id = id;
            this.name = name;
            this.host = host;
            this.amHost = amHost;
            this.members = members;
            this.queue = queue;
            this.addedBy = addedBy;
            this.index = index;
            this.posMs = posMs;
            this.playing = playing;
            this.votes = votes;
            this.needed = needed;
        }

        public static JamState none() {
            return new JamState(0, "", "", false, List.of(), List.of(), List.of(), 0, 0, false, 0, 0);
        }

        /** La canción que va ahora (o null si la cola terminó o está vacía). */
        public SongInfo current() {
            return index >= 0 && index < queue.size() ? queue.get(index) : null;
        }

        void write(FriendlyByteBuf b) {
            b.writeLong(id);
            b.writeUtf(name, 40);
            b.writeUtf(host, 40);
            b.writeBoolean(amHost);
            b.writeVarInt(members.size());
            for (String m : members) {
                b.writeUtf(m, 40);
            }
            b.writeVarInt(queue.size());
            for (int i = 0; i < queue.size(); i++) {
                queue.get(i).write(b);
                b.writeUtf(addedBy.get(i), 40);
            }
            b.writeVarInt(index);
            b.writeLong(posMs);
            b.writeBoolean(playing);
            b.writeVarInt(votes);
            b.writeVarInt(needed);
        }

        static JamState read(FriendlyByteBuf b) {
            long id = b.readLong();
            String name = b.readUtf(40), host = b.readUtf(40);
            boolean am = b.readBoolean();
            int n = Math.min(b.readVarInt(), 200);
            List<String> mem = new ArrayList<>(n);
            for (int i = 0; i < n; i++) {
                mem.add(b.readUtf(40));
            }
            int q = Math.min(b.readVarInt(), 500);
            List<SongInfo> queue = new ArrayList<>(q);
            List<String> by = new ArrayList<>(q);
            for (int i = 0; i < q; i++) {
                queue.add(SongInfo.read(b));
                by.add(b.readUtf(40));
            }
            return new JamState(id, name, host, am, mem, queue, by, b.readVarInt(), b.readLong(), b.readBoolean(), b.readVarInt(), b.readVarInt());
        }
    }

    /** Los Jams que hay en el mundo (para unirse). */
    public static final class JamList {
        public final List<Long> ids = new ArrayList<>();
        public final List<String> names = new ArrayList<>();
        public final List<String> hosts = new ArrayList<>();
        public final List<Integer> members = new ArrayList<>();
        public final List<String> now = new ArrayList<>();

        void write(FriendlyByteBuf b) {
            b.writeVarInt(ids.size());
            for (int i = 0; i < ids.size(); i++) {
                b.writeLong(ids.get(i));
                b.writeUtf(names.get(i), 40);
                b.writeUtf(hosts.get(i), 40);
                b.writeVarInt(members.get(i));
                b.writeUtf(now.get(i), 120);
            }
        }

        static JamList read(FriendlyByteBuf b) {
            JamList l = new JamList();
            int n = Math.min(b.readVarInt(), 200);
            for (int i = 0; i < n; i++) {
                l.ids.add(b.readLong());
                l.names.add(b.readUtf(40));
                l.hosts.add(b.readUtf(40));
                l.members.add(b.readVarInt());
                l.now.add(b.readUtf(120));
            }
            return l;
        }
    }
}
