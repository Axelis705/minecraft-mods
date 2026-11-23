package com.axeliscore.net;

import com.axeliscore.AxelisCore;
import com.axeliscore.plat.Net;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;

import java.util.ArrayList;
import java.util.List;

/**
 * Mensajes del propio Core (efectos de pantalla, avisos, títulos y diálogos). Cada mod tiene su propio canal con {@link Net}.
 */
public final class CoreNet {
    public static final Net NET = new Net(AxelisCore.MODID, "main", 1);

    private CoreNet() {
    }

    /** Efecto de pantalla: 0 temblor, 1 destello, 2 viñeta. */
    public record FxMsg(int kind, int color, float strength, int ticks) {
        void write(FriendlyByteBuf b) {
            b.writeVarInt(kind);
            b.writeInt(color);
            b.writeFloat(strength);
            b.writeVarInt(ticks);
        }

        static FxMsg read(FriendlyByteBuf b) {
            return new FxMsg(b.readVarInt(), b.readInt(), b.readFloat(), b.readVarInt());
        }
    }

    /** Título grande en el centro. */
    public record TitleMsg(String title, String sub, int color, int ticks) {
        void write(FriendlyByteBuf b) {
            b.writeUtf(title);
            b.writeUtf(sub);
            b.writeInt(color);
            b.writeVarInt(ticks);
        }

        static TitleMsg read(FriendlyByteBuf b) {
            return new TitleMsg(b.readUtf(), b.readUtf(), b.readInt(), b.readVarInt());
        }
    }

    /** Aviso que entra por la derecha (con el dibujo de un objeto). */
    public record ToastMsg(String item, String title, String text, int color) {
        void write(FriendlyByteBuf b) {
            b.writeUtf(item);
            b.writeUtf(title);
            b.writeUtf(text);
            b.writeInt(color);
        }

        static ToastMsg read(FriendlyByteBuf b) {
            return new ToastMsg(b.readUtf(), b.readUtf(), b.readUtf(), b.readInt());
        }
    }

    /** Un diálogo: quién habla (y su piel para el retrato), el texto y las respuestas posibles. */
    public record DialogMsg(int id, String speaker, String skin, String text, List<String> options, int theme) {
        void write(FriendlyByteBuf b) {
            b.writeVarInt(id);
            b.writeUtf(speaker);
            b.writeUtf(skin);
            b.writeUtf(text);
            b.writeVarInt(options.size());
            for (String o : options) {
                b.writeUtf(o);
            }
            b.writeVarInt(theme);
        }

        static DialogMsg read(FriendlyByteBuf b) {
            int id = b.readVarInt();
            String sp = b.readUtf(), sk = b.readUtf(), tx = b.readUtf();
            int n = b.readVarInt();
            List<String> o = new ArrayList<>();
            for (int i = 0; i < n; i++) {
                o.add(b.readUtf());
            }
            return new DialogMsg(id, sp, sk, tx, o, b.readVarInt());
        }
    }

    /** Abrir una pantalla del cliente por nombre. */
    public record OpenMsg(String id) {
        void write(FriendlyByteBuf b) {
            b.writeUtf(id);
        }

        static OpenMsg read(FriendlyByteBuf b) {
            return new OpenMsg(b.readUtf());
        }
    }

    /** La respuesta elegida (-1 = cerró el diálogo). */
    public record ReplyMsg(int id, int choice) {
        void write(FriendlyByteBuf b) {
            b.writeVarInt(id);
            b.writeVarInt(choice);
        }

        static ReplyMsg read(FriendlyByteBuf b) {
            return new ReplyMsg(b.readVarInt(), b.readVarInt());
        }
    }

    /** Datos para el cliente, por clave ({@link com.axeliscore.data.PlayerStore.Sync}). */
    public record DataMsg(String key, CompoundTag tag) {
        void write(FriendlyByteBuf b) {
            b.writeUtf(key);
            b.writeNbt(tag);
        }

        static DataMsg read(FriendlyByteBuf b) {
            String k = b.readUtf();
            CompoundTag t = b.readNbt();
            return new DataMsg(k, t == null ? new CompoundTag() : t);
        }
    }

    /** Una acción del cliente para el servidor, por clave ({@link com.axeliscore.data.Actions}). */
    public record ActionMsg(String key, CompoundTag tag) {
        void write(FriendlyByteBuf b) {
            b.writeUtf(key);
            b.writeNbt(tag);
        }

        static ActionMsg read(FriendlyByteBuf b) {
            String k = b.readUtf();
            CompoundTag t = b.readNbt();
            return new ActionMsg(k, t == null ? new CompoundTag() : t);
        }
    }

    /** Se llama una vez al arrancar (dentro de enqueueWork del arranque común). */
    public static void register() {
        NET.register(FxMsg.class, FxMsg::write, FxMsg::read, (m, sender) -> Net.onClient(() -> ClientSide.fx(m)));
        NET.register(TitleMsg.class, TitleMsg::write, TitleMsg::read, (m, sender) -> Net.onClient(() -> ClientSide.title(m)));
        NET.register(ToastMsg.class, ToastMsg::write, ToastMsg::read, (m, sender) -> Net.onClient(() -> ClientSide.toast(m)));
        NET.register(DialogMsg.class, DialogMsg::write, DialogMsg::read, (m, sender) -> Net.onClient(() -> ClientSide.dialog(m)));
        NET.register(ReplyMsg.class, ReplyMsg::write, ReplyMsg::read, (m, sender) -> {
            if (sender != null) {
                com.axeliscore.ui.Dialogs.reply(sender, m.id(), m.choice());
            }
        });
        NET.register(OpenMsg.class, OpenMsg::write, OpenMsg::read, (m, sender) -> Net.onClient(() -> ClientSide.open(m)));
        NET.register(DataMsg.class, DataMsg::write, DataMsg::read, (m, sender) -> Net.onClient(() -> ClientSide.data(m)));
        NET.register(ActionMsg.class, ActionMsg::write, ActionMsg::read, (m, sender) -> {
            if (sender != null) {
                com.axeliscore.data.Actions.receive(sender, m.key(), m.tag());
            }
        });
        NET.done();
    }

    /** Puente al cliente (las clases del cliente solo se cargan allá). */
    static final class ClientSide {
        static void fx(FxMsg m) {
            com.axeliscore.fx.client.FxClient.fx(m.kind(), m.color(), m.strength(), m.ticks());
        }

        static void title(TitleMsg m) {
            com.axeliscore.fx.client.FxClient.title(Txt.unpack(m.title()), Txt.unpack(m.sub()), m.color(), m.ticks());
        }

        static void toast(ToastMsg m) {
            com.axeliscore.ui.Toasts.add(com.axeliscore.plat.Plat.item(m.item()), Txt.unpack(m.title()), Txt.unpack(m.text()), m.color());
        }

        static void open(OpenMsg m) {
            com.axeliscore.ui.ScreenRegistry.open(m.id());
        }

        static void dialog(DialogMsg m) {
            com.axeliscore.ui.DialogScreen.open(m);
        }

        static void data(DataMsg m) {
            com.axeliscore.data.client.ClientData.receive(m.key(), m.tag());
        }
    }
}
