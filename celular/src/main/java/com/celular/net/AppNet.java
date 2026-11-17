package com.celular.net;

import net.minecraft.network.FriendlyByteBuf;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Mensajes de red de las apps nuevas: Mensajes, Mapa con amigos y Billetera. */
public final class AppNet {
    private AppNet() {
    }

    private static final UUID NONE = new UUID(0L, 0L);

    private static void uuid(FriendlyByteBuf b, UUID u) {
        b.writeUUID(u == null ? NONE : u);
    }

    // ================================================================== del cliente al servidor

    /** Un pedido de una app: qué ({@code what}), a quién, un número y un texto. */
    public static final class Req {
        public static final int MSG_LIST = 0, MSG_OPEN = 1, MSG_SEND = 2, MSG_READ = 3, MAP = 10, SHARE = 11, WALLET_SEND = 20,
                WALLET_HIST = 21, TAKE_OUT = 30;
        public final int what;
        public final UUID who;
        public final long n;
        public final String s;

        public Req(int what, UUID who, long n, String s) {
            this.what = what;
            this.who = who;
            this.n = n;
            this.s = s == null ? "" : s;
        }

        public Req(int what) {
            this(what, null, 0, "");
        }

        public void write(FriendlyByteBuf b) {
            b.writeVarInt(what);
            uuid(b, who);
            b.writeLong(n);
            b.writeUtf(s, 300);
        }

        public static Req read(FriendlyByteBuf b) {
            return new Req(b.readVarInt(), b.readUUID(), b.readLong(), b.readUtf(300));
        }
    }

    // ================================================================== del servidor al cliente

    /** Una conversación en la lista de Mensajes. */
    public static final class Conv {
        public UUID id;
        public String name = "";
        public String last = "";
        public boolean lastMine;
        public long time;
        public int unread;
        public boolean online;
    }

    public static final class MsgList {
        public final List<Conv> convs = new ArrayList<>();

        public void write(FriendlyByteBuf b) {
            b.writeVarInt(convs.size());
            for (Conv c : convs) {
                b.writeUUID(c.id);
                b.writeUtf(c.name, 64);
                b.writeUtf(c.last, 300);
                b.writeBoolean(c.lastMine);
                b.writeLong(c.time);
                b.writeVarInt(c.unread);
                b.writeBoolean(c.online);
            }
        }

        public static MsgList read(FriendlyByteBuf b) {
            MsgList m = new MsgList();
            int n = Math.min(b.readVarInt(), 500);
            for (int i = 0; i < n; i++) {
                Conv c = new Conv();
                c.id = b.readUUID();
                c.name = b.readUtf(64);
                c.last = b.readUtf(300);
                c.lastMine = b.readBoolean();
                c.time = b.readLong();
                c.unread = b.readVarInt();
                c.online = b.readBoolean();
                m.convs.add(c);
            }
            return m;
        }
    }

    /** Un mensaje de una conversación. */
    public static final class Msg {
        public boolean mine;
        public String from = "";
        public String text = "";
        public long time;

        void write(FriendlyByteBuf b) {
            b.writeBoolean(mine);
            b.writeUtf(from, 64);
            b.writeUtf(text, 300);
            b.writeLong(time);
        }

        static Msg read(FriendlyByteBuf b) {
            Msg m = new Msg();
            m.mine = b.readBoolean();
            m.from = b.readUtf(64);
            m.text = b.readUtf(300);
            m.time = b.readLong();
            return m;
        }
    }

    /** Una conversación entera (al abrirla). */
    public static final class MsgThread {
        public final UUID id;
        public final String name;
        public final boolean online;
        public final List<Msg> msgs;

        public MsgThread(UUID id, String name, boolean online, List<Msg> msgs) {
            this.id = id;
            this.name = name;
            this.online = online;
            this.msgs = msgs;
        }

        public void write(FriendlyByteBuf b) {
            b.writeUUID(id);
            b.writeUtf(name, 64);
            b.writeBoolean(online);
            b.writeVarInt(msgs.size());
            for (Msg m : msgs) {
                m.write(b);
            }
        }

        public static MsgThread read(FriendlyByteBuf b) {
            UUID id = b.readUUID();
            String name = b.readUtf(64);
            boolean on = b.readBoolean();
            int n = Math.min(b.readVarInt(), 1000);
            List<Msg> l = new ArrayList<>();
            for (int i = 0; i < n; i++) {
                l.add(Msg.read(b));
            }
            return new MsgThread(id, name, on, l);
        }
    }

    /** Llegó un mensaje nuevo a la conversación {@code conv} (el que lo mandó o el grupo de todos). */
    public static final class MsgNew {
        public final UUID conv;
        public final String convName;
        public final Msg msg;

        public MsgNew(UUID conv, String convName, Msg msg) {
            this.conv = conv;
            this.convName = convName;
            this.msg = msg;
        }

        public void write(FriendlyByteBuf b) {
            b.writeUUID(conv);
            b.writeUtf(convName, 64);
            msg.write(b);
        }

        public static MsgNew read(FriendlyByteBuf b) {
            return new MsgNew(b.readUUID(), b.readUtf(64), Msg.read(b));
        }
    }

    /** Dónde están los amigos que comparten su ubicación (solo los de tu misma dimensión). */
    public static final class Friends {
        public final List<UUID> ids = new ArrayList<>();
        public final List<String> names = new ArrayList<>();
        public final List<double[]> pos = new ArrayList<>();
        public boolean sharing;

        public void write(FriendlyByteBuf b) {
            b.writeBoolean(sharing);
            b.writeVarInt(ids.size());
            for (int i = 0; i < ids.size(); i++) {
                b.writeUUID(ids.get(i));
                b.writeUtf(names.get(i), 64);
                double[] p = pos.get(i);
                b.writeDouble(p[0]);
                b.writeDouble(p[1]);
                b.writeDouble(p[2]);
                b.writeFloat((float) p[3]);
            }
        }

        public static Friends read(FriendlyByteBuf b) {
            Friends f = new Friends();
            f.sharing = b.readBoolean();
            int n = Math.min(b.readVarInt(), 500);
            for (int i = 0; i < n; i++) {
                f.ids.add(b.readUUID());
                f.names.add(b.readUtf(64));
                f.pos.add(new double[]{b.readDouble(), b.readDouble(), b.readDouble(), b.readFloat()});
            }
            return f;
        }
    }

    /** Movimientos de la billetera: con quién, cuánto (positivo = te llegó) y cuándo. */
    public static final class WalletHist {
        public final List<String> who = new ArrayList<>();
        public final List<Integer> amount = new ArrayList<>();
        public final List<Long> time = new ArrayList<>();

        public void write(FriendlyByteBuf b) {
            b.writeVarInt(who.size());
            for (int i = 0; i < who.size(); i++) {
                b.writeUtf(who.get(i), 64);
                b.writeVarInt(amount.get(i) + Integer.MAX_VALUE / 2);
                b.writeLong(time.get(i));
            }
        }

        public static WalletHist read(FriendlyByteBuf b) {
            WalletHist h = new WalletHist();
            int n = Math.min(b.readVarInt(), 200);
            for (int i = 0; i < n; i++) {
                h.who.add(b.readUtf(64));
                h.amount.add(b.readVarInt() - Integer.MAX_VALUE / 2);
                h.time.add(b.readLong());
            }
            return h;
        }
    }
}
