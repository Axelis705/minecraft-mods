package com.celular.client;

import com.celular.CelularReg;
import com.celular.net.CelularNet;
import com.celular.net.CelularNet.CallState;
import com.celular.net.CelularNet.Post;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Lo que el cliente sabe del celular (muro, contactos, llamada...) y lo que hace cuando llegan mensajes del servidor. */
public final class ClientHooks {
    public static List<Post> feed = new ArrayList<>();
    public static boolean feedLoaded;
    public static UUID profileId;
    public static List<Post> profile = new ArrayList<>();
    public static boolean profileLoaded;
    public static CelularNet.Contacts contacts;
    public static List<String> patreon;

    public static CallState call = new CallState(CallState.IDLE, 0, null, "", 0, false);
    /** Hora (del reloj de la PC) en que empezó la llamada activa. */
    public static long callStart;
    /** El último motivo por el que terminó una llamada y cuándo (para mostrarlo un momento). */
    public static CallState lastEnd;
    public static long lastEndAt;

    public static final class Recent {
        public final UUID id;
        public final String name;
        /** 0 hecha, 1 recibida, 2 perdida. */
        public final int type;
        public final long time;
        public final int seconds;

        Recent(UUID id, String name, int type, int seconds) {
            this.id = id;
            this.name = name;
            this.type = type;
            this.time = System.currentTimeMillis();
            this.seconds = seconds;
        }
    }

    public static final List<Recent> recents = new ArrayList<>();
    public static int badgeInsta, badgeCalls;
    /** Color de la carcasa del último celular abierto. */
    public static int lastColor;
    /** Resultado de la última publicación: 0 nada, 1 publicada, 2 error. */
    public static int uploadResult;
    public static String uploadError = "";

    private ClientHooks() {
    }

    public static void openPhone(int color) {
        lastColor = color;
        takeOut();
        open(new PhoneScreen(color, null));
    }

    // ------------------------------------------------------------------ sacar el celular a la mano (para que los demás lo vean)

    /** Casilla que había antes de sacar el celular con la K (-1 = no se cambió). */
    private static int restoreSlot = -1;

    private static int selected(net.minecraft.world.entity.player.Inventory inv) {
        //#if MC >= 1.21.5
        //$$ return inv.getSelectedSlot();
        //#else
        return inv.selected;
        //#endif
    }

    private static void select(net.minecraft.world.entity.player.Inventory inv, int slot) {
        //#if MC >= 1.21.5
        //$$ inv.setSelectedSlot(slot);
        //#else
        inv.selected = slot;
        //#endif
    }

    /** Si el celular está en la barra rápida pero no en la mano, se elige esa casilla (el juego se lo avisa solo al servidor). */
    private static void takeOut() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || Flash.inHand(mc)) {
            return;
        }
        net.minecraft.world.entity.player.Inventory inv = mc.player.getInventory();
        for (int i = 0; i < 9; i++) {
            if (inv.getItem(i).getItem() instanceof com.celular.PhoneItem) {
                restoreSlot = selected(inv);
                select(inv, i);
                return;
            }
        }
    }

    /** Cada cuadro: al cerrar el celular se vuelve a la casilla de antes (salvo con la linterna encendida). */
    static void putAwayIfClosed() {
        if (restoreSlot < 0) {
            return;
        }
        Object sc = screen();
        if (sc instanceof PhoneScreen || sc instanceof CameraScreen) {
            return;
        }
        if (call.state == CallState.ACTIVE || call.state == CallState.DIALING) {
            return;                 // en llamada se queda en la mano (en la oreja); al colgar se guarda
        }
        int r = restoreSlot;
        restoreSlot = -1;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || Flash.on) {
            return;
        }
        net.minecraft.world.entity.player.Inventory inv = mc.player.getInventory();
        if (inv.getItem(selected(inv)).getItem() instanceof com.celular.PhoneItem && r >= 0 && r < 9) {
            select(inv, r);
        }
    }

    /** Abre una pantalla (en 26.2+ las pantallas viven en gui). */
    public static void open(net.minecraft.client.gui.screens.Screen s) {
        //#if MC >= 26.2
        //$$ Minecraft.getInstance().gui.setScreen(s);
        //#else
        Minecraft.getInstance().setScreen(s);
        //#endif
    }

    /** La pantalla abierta ahora (o null). */
    public static net.minecraft.client.gui.screens.Screen screen() {
        //#if MC >= 26.2
        //$$ return Minecraft.getInstance().gui.screen();
        //#else
        return Minecraft.getInstance().screen;
        //#endif
    }

    public static void play(SoundEvent s, float pitch, float vol) {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(s, pitch, vol));
    }

    public static void tap() {
        play(CelularReg.TAP.get(), 1.0F, 0.35F);
    }

    // ------------------------------------------------------------------ mensajes del servidor

    public static void feed(CelularNet.Feed m) {
        if (m.author == null) {
            feed = new ArrayList<>(m.posts);
            feedLoaded = true;
        } else {
            profileId = m.author;
            profile = new ArrayList<>(m.posts);
            profileLoaded = true;
        }
    }

    public static void image(CelularNet.Img m) {
        Photos.received(m.id, m.data);
    }

    public static void postUpdate(Post p) {
        replace(feed, p);
        replace(profile, p);
    }

    private static void replace(List<Post> l, Post p) {
        for (int i = 0; i < l.size(); i++) {
            if (l.get(i).id == p.id) {
                l.set(i, p);
            }
        }
    }

    /** Cambia "me gusta" en la pantalla de una vez (el servidor lo confirma después). */
    public static void likeLocal(Post p) {
        Post n = new Post(p.id, p.author, p.name, p.caption, p.time, p.likes + (p.liked ? -1 : 1), !p.liked, p.mine);
        postUpdate(n);
    }

    public static void names(CelularNet.Names m) {
        if (m.what == CelularNet.Names.PATREON) {
            patreon = m.names;
        }
    }

    public static void contacts(CelularNet.Contacts m) {
        contacts = m;
    }

    public static void callState(CallState m) {
        CallState old = call;
        call = m;
        Minecraft mc = Minecraft.getInstance();
        if (m.state == CallState.ACTIVE) {
            callStart = System.currentTimeMillis() - m.seconds * 1000L;
        }
        if (m.state == CallState.INCOMING) {
            if (screen() instanceof PhoneScreen ps) {
                ps.openCall();
            }
        }
        if (m.state == CallState.IDLE) {
            lastEnd = m;
            lastEndAt = System.currentTimeMillis();
            if (old.state != CallState.IDLE && old.other != null) {
                int type;
                if (old.state == CallState.DIALING) {
                    type = 0;
                } else if (old.state == CallState.INCOMING) {
                    type = m.reason == CallState.R_ENDED ? 1 : 2;
                } else {
                    type = wasCaller ? 0 : 1;
                }
                recents.add(0, new Recent(old.other, old.name, type, m.seconds));
                while (recents.size() > 40) {
                    recents.remove(recents.size() - 1);
                }
                if (type == 2) {
                    badgeCalls++;
                }
            }
            if (old.state == CallState.ACTIVE) {
                play(CelularReg.HANGUP.get(), 1.0F, 0.6F);
            }
        }
        if (m.state == CallState.DIALING) {
            wasCaller = true;
        } else if (m.state == CallState.INCOMING) {
            wasCaller = false;
        }
    }

    private static boolean wasCaller;

    public static void notify(CelularNet.Notify m) {
        switch (m.type) {
            case CelularNet.Notify.POSTED -> {
                uploadResult = 1;
                PhoneHud.toast(D.APP_INSTA, "celular.app.insta", net.minecraft.network.chat.Component.translatable("celular.notify.posted"));
            }
            case CelularNet.Notify.ERROR -> {
                uploadResult = 2;
                uploadError = m.text;
                PhoneHud.toast(D.APP_INSTA, "celular.app.insta", net.minecraft.network.chat.Component.translatable(m.text));
            }
            case CelularNet.Notify.LIKE -> {
                badgeInsta++;
                PhoneHud.toast(D.APP_INSTA, "celular.app.insta", net.minecraft.network.chat.Component.translatable("celular.notify.like", m.who));
            }
            case CelularNet.Notify.NEW_POST -> {
                PhoneHud.toast(D.APP_INSTA, "celular.app.insta",
                        m.text.isEmpty() ? net.minecraft.network.chat.Component.translatable("celular.notify.new_post", m.who)
                                : net.minecraft.network.chat.Component.translatable("celular.notify.new_post_caption", m.who, m.text));
                if (feedLoaded) {
                    com.celular.plat.Net.toServer(new CelularNet.FeedReq(null));
                }
            }
            case CelularNet.Notify.MISSED_CALL -> PhoneHud.toast(D.APP_PHONE, "celular.app.phone",
                    net.minecraft.network.chat.Component.translatable("celular.notify.missed", m.who));
            case CelularNet.Notify.JAM_INVITE -> Radio.invite(m);
            case CelularNet.Notify.DJ -> Radio.dj(m);
            case CelularNet.Notify.SPEAKER_PAIR -> Radio.paired(m);
            case CelularNet.Notify.WALLET -> PhoneHud.toast(D.APP_WALLET, "celular.app.wallet", net.minecraft.network.chat.Component.translatable(m.text));
            case CelularNet.Notify.WALLET_SENT -> PhoneHud.toast(D.APP_WALLET, "celular.app.wallet",
                    net.minecraft.network.chat.Component.translatable("celular.wallet.sent", m.text, m.who));
            case CelularNet.Notify.WALLET_IN -> PhoneHud.toast(D.APP_WALLET, "celular.app.wallet",
                    net.minecraft.network.chat.Component.translatable("celular.wallet.received", m.who, m.text));
            default -> {
            }
        }
    }

    /** Segundos que lleva la llamada activa. */
    public static int callSeconds() {
        return (int) ((System.currentTimeMillis() - callStart) / 1000);
    }

    public static boolean hasPhone() {
        Minecraft mc = Minecraft.getInstance();
        return mc.player != null && CelularReg.phoneColor(mc.player) >= 0;
    }

    /** Al salir de un mundo: se olvida todo. */
    public static void reset() {
        feed = new ArrayList<>();
        feedLoaded = false;
        profile = new ArrayList<>();
        profileLoaded = false;
        profileId = null;
        contacts = null;
        patreon = null;
        call = new CallState(CallState.IDLE, 0, null, "", 0, false);
        badgeInsta = 0;
        Photos.clearAll();
        Radio.reset();
        Flash.reset();
        restoreSlot = -1;
        Chats.reset();
        Notes.reset();
        MapApp.reset();
        WalletApp.reset();
        com.celular.music.MusicPlayer.stop();          // al salir del mundo se calla la música
    }
}
