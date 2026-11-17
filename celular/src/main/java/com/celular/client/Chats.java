package com.celular.client;

import com.celular.msg.Messages;
import com.celular.net.AppNet;
import com.celular.plat.Net;
import net.minecraft.network.chat.Component;

import java.util.UUID;

/** Lo que el celular sabe de los Mensajes: la lista de chats, el chat abierto y cuántos sin leer hay (para el globito). */
public final class Chats {
    public static AppNet.MsgList list;
    public static AppNet.MsgThread open;
    /** El chat que se está viendo ahora en pantalla (null = ninguno). */
    public static UUID viewing;
    public static int badge;
    /** Se cambió algo de la lista (para que la app la vuelva a pedir). */
    public static boolean stale;

    private Chats() {
    }

    public static void list(AppNet.MsgList m) {
        list = m;
        stale = false;
        int n = 0;
        for (AppNet.Conv c : m.convs) {
            n += c.unread;
        }
        badge = n;
    }

    public static void thread(AppNet.MsgThread m) {
        open = m;
    }

    public static void incoming(AppNet.MsgNew m) {
        boolean seeing = viewing != null && viewing.equals(m.conv);
        if (open != null && open.id.equals(m.conv)) {
            open.msgs.add(m.msg);
        }
        if (seeing && !m.msg.mine) {
            Net.toServer(new AppNet.Req(AppNet.Req.MSG_READ, m.conv, 0, ""));
        }
        stale = true;
        if (!m.msg.mine && !seeing) {
            badge++;
            String who = Messages.ALL.equals(m.conv) ? Component.translatable("celular.msg.all").getString() + " · " + m.msg.from : m.msg.from;
            PhoneHud.toast(D.APP_MESSAGES, "celular.app.messages", Component.literal(who + ": " + m.msg.text));
        }
    }

    /** (El mapa y la billetera no son públicos: sus datos llegan por aquí.) */
    public static void friends(AppNet.Friends m) {
        MapApp.friends(m);
    }

    public static void walletHist(AppNet.WalletHist m) {
        WalletApp.hist(m);
    }

    static void reset() {
        list = null;
        open = null;
        viewing = null;
        badge = 0;
        stale = false;
    }
}
