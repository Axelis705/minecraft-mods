package com.celular.client;

import com.celular.CelularReg;
import com.celular.net.CelularNet;
import com.celular.net.CelularNet.CallState;
import com.celular.plat.Net;
import com.celular.plat.client.G;
import net.minecraft.ChatFormatting;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
//#if MC >= 26.1
//$$ import com.celular.plat.Ids;
//#endif

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Lo que se ve en pantalla sin abrir el celular: las notificaciones (bajan desde arriba a la derecha), la llamada entrante y la llamada
 * en curso. También atiende las teclas: abrir el celular, contestar y colgar.
 */
public final class PhoneHud {
    //#if MC >= 26.1
    //$$ public static final KeyMapping.Category CAT = KeyMapping.Category.register(Ids.mod("main"));
    //$$ public static final KeyMapping OPEN = new SafeKey("key.celular.open", InputConstants.KEY_K, CAT);
    //$$ public static final KeyMapping ANSWER = new SafeKey("key.celular.answer", InputConstants.KEY_Y, CAT);
    //$$ public static final KeyMapping HANGUP = new SafeKey("key.celular.hangup", InputConstants.KEY_U, CAT);
    //$$ public static final KeyMapping MUSIC_TOGGLE = new SafeKey("key.celular.music_toggle", InputConstants.UNKNOWN.getValue(), CAT);
    //$$ public static final KeyMapping MUSIC_NEXT = new SafeKey("key.celular.music_next", InputConstants.UNKNOWN.getValue(), CAT);
    //$$ public static final KeyMapping MUSIC_PREV = new SafeKey("key.celular.music_prev", InputConstants.UNKNOWN.getValue(), CAT);
    //$$ public static final KeyMapping FLASHLIGHT = new SafeKey("key.celular.flashlight", InputConstants.UNKNOWN.getValue(), CAT);
    //#else
    public static final KeyMapping OPEN = new SafeKey("key.celular.open", InputConstants.KEY_K, "key.categories.celular");
    public static final KeyMapping ANSWER = new SafeKey("key.celular.answer", InputConstants.KEY_Y, "key.categories.celular");
    public static final KeyMapping HANGUP = new SafeKey("key.celular.hangup", InputConstants.KEY_U, "key.categories.celular");
    public static final KeyMapping MUSIC_TOGGLE = new SafeKey("key.celular.music_toggle", InputConstants.UNKNOWN.getValue(), "key.categories.celular");
    public static final KeyMapping MUSIC_NEXT = new SafeKey("key.celular.music_next", InputConstants.UNKNOWN.getValue(), "key.categories.celular");
    public static final KeyMapping MUSIC_PREV = new SafeKey("key.celular.music_prev", InputConstants.UNKNOWN.getValue(), "key.categories.celular");
    public static final KeyMapping FLASHLIGHT = new SafeKey("key.celular.flashlight", InputConstants.UNKNOWN.getValue(), "key.categories.celular");
    //#endif

    private static final class Toast {
        final int app;
        final String title;
        final Component text;
        final long born = System.currentTimeMillis();

        Toast(int app, String title, Component text) {
            this.app = app;
            this.title = title;
            this.text = text;
        }
    }

    private static final List<Toast> TOASTS = new ArrayList<>();
    private static final long TOAST_MS = 4500;
    private static long lastRingback;
    private static Object lastConnection;

    private PhoneHud() {
    }

    public static void toast(int app, String titleKey, Component text) {
        if (add(app, titleKey, text)) {
            ClientHooks.play(CelularReg.NOTIFY.get(), 1.0F, 0.5F);
        }
    }

    /** Aviso sin sonido (por ejemplo "ahora suena" de la música). */
    public static void toastQuiet(int app, String titleKey, Component text) {
        add(app, titleKey, text);
    }

    private static boolean add(int app, String titleKey, Component text) {
        Prefs.load();
        if (!Prefs.notifications || !ClientHooks.hasPhone()) {
            return false;
        }
        TOASTS.add(new Toast(app, titleKey, text));
        while (TOASTS.size() > 3) {
            TOASTS.remove(0);
        }
        return true;
    }

    private static void keys(Minecraft mc) {
        while (OPEN.consumeClick()) {
            if (mc.player != null && ClientHooks.screen() == null) {
                int c = CelularReg.phoneColor(mc.player);
                if (c >= 0) {
                    ClientHooks.openPhone(c);
                } else {
                    com.celular.plat.Plat.msg(mc.player, Component.translatable("celular.no_phone").withStyle(ChatFormatting.GRAY), true);
                }
            }
        }
        while (MUSIC_TOGGLE.consumeClick()) {
            Radio.toggle();
        }
        while (MUSIC_NEXT.consumeClick()) {
            Radio.next();
        }
        while (MUSIC_PREV.consumeClick()) {
            Radio.prev();
        }
        while (FLASHLIGHT.consumeClick()) {
            Flash.toggle();
        }
        CallState st = ClientHooks.call;
        while (ANSWER.consumeClick()) {
            if (st.state == CallState.INCOMING) {
                Net.toServer(new CelularNet.CallAct(CelularNet.CallAct.ACCEPT, null));
            }
        }
        while (HANGUP.consumeClick()) {
            if (st.state != CallState.IDLE) {
                Net.toServer(new CelularNet.CallAct(st.state == CallState.INCOMING ? CelularNet.CallAct.DECLINE : CelularNet.CallAct.HANGUP, null));
            }
        }
    }

    public static void render(G g, int sw, int sh) {
        Minecraft mc = Minecraft.getInstance();
        Object conn = mc.getConnection();
        if (conn != lastConnection) {
            lastConnection = conn;
            ClientHooks.reset();
        }
        if (mc.player == null) {
            return;
        }
        keys(mc);
        Music.frame();
        CompassApp.watchDeath();
        long now = System.currentTimeMillis();
        CallState st = ClientHooks.call;
        // tono de "está sonando" para quien llama
        if (st.state == CallState.DIALING && now - lastRingback > 3000) {
            lastRingback = now;
            ClientHooks.play(CelularReg.RINGBACK.get(), 1.0F, 0.5F);
        }
        if (ClientHooks.screen() instanceof CameraScreen) {
            return; // nada encima de las fotos
        }
        Font font = mc.font;
        boolean phoneOpen = ClientHooks.screen() instanceof PhoneScreen;
        if (!phoneOpen) {
            if (st.state == CallState.INCOMING) {
                incoming(g, font, sw, now, st);
            } else if (st.state == CallState.ACTIVE || st.state == CallState.DIALING) {
                pill(g, font, sw, now, st);
            }
        }
        drawToasts(g, font, sw, now, phoneOpen);
    }

    private static void incoming(G g, Font font, int sw, long now, CallState st) {
        int w = 190, h = 44, x = sw / 2 - w / 2, y = 6;
        float pulse = (float) (0.5 + 0.5 * Math.sin(now / 160.0));
        D.rr(g, x, y, x + w, y + h, 12, 0xE0101418);
        D.rrLine(g, x, y, x + w, y + h, 12, D.mix(0xFF2BD46B, 0xFF0F6A33, pulse));
        // cara que "vibra"
        int shake = (int) (Math.sin(now / 30.0) * (now % 1200 < 500 ? 1.5 : 0));
        D.faceRound(g, st.other, x + 8 + shake, y + 8, 28, 0xFF2BD46B);
        g.drawString(font, Component.translatable("celular.call.incoming").withStyle(ChatFormatting.GRAY), x + 42, y + 8, 0xFFB0B8C0, false);
        g.drawString(font, D.cut(font, st.name, w - 50), x + 42, y + 19, 0xFFFFFFFF, false);
        Component keys = Component.translatable("celular.call.keys", ANSWER.getTranslatedKeyMessage(), HANGUP.getTranslatedKeyMessage());
        g.drawString(font, keys, x + 42, y + 31, 0xFF7CF0A6, false);
    }

    private static void pill(G g, Font font, int sw, long now, CallState st) {
        boolean active = st.state == CallState.ACTIVE;
        String t = active ? D.clock(ClientHooks.callSeconds()) : "…";
        Component line = Component.literal(D.cut(font, st.name, 90) + "  " + t);
        Component hint = Component.translatable("celular.call.hangup_key", HANGUP.getTranslatedKeyMessage());
        int w = Math.max(font.width(line), font.width(hint)) + 30, h = 24, x = sw / 2 - w / 2, y = 4;
        D.rr(g, x, y, x + w, y + h, 12, active ? 0xE0128A3E : 0xE0303840);
        D.icon(g, D.CALL, x + 7, y + 6, 12, 0xFFFFFFFF);
        g.drawString(font, line, x + 23, y + 3, 0xFFFFFFFF, false);
        g.drawString(font, hint, x + 23, y + 13, 0xFFD8FFE4, false);
        if (active && st.voice) {
            // ondas de voz
            for (int i = 0; i < 3; i++) {
                int bh = 2 + (int) (4 * Math.abs(Math.sin(now / 120.0 + i * 1.3)));
                g.fill(x + w - 14 + i * 3, y + 12 - bh / 2, x + w - 12 + i * 3, y + 12 + bh / 2 + 1, 0xFFFFFFFF);
            }
        }
    }

    private static void drawToasts(G g, Font font, int sw, long now, boolean phoneOpen) {
        int y = 6;
        for (Iterator<Toast> it = TOASTS.iterator(); it.hasNext(); ) {
            Toast t = it.next();
            long age = now - t.born;
            if (age > TOAST_MS) {
                it.remove();
                continue;
            }
            float in = D.easeOut(age / 260F), out = D.easeOut((TOAST_MS - age) / 260F);
            float k = Math.min(in, out);
            int w = 170, x = sw - (int) ((w + 6) * k);
            List<FormattedCharSequence> lines = font.split(t.text, w - 32);
            int n = Math.min(2, lines.size());
            int h = 16 + n * 10;
            D.rr(g, x, y, x + w, y + h, 8, 0xE8181B20);
            D.app(g, t.app, x + 5, y + 5, 18);
            g.drawString(font, Component.translatable(t.title).withStyle(ChatFormatting.BOLD), x + 27, y + 4, 0xFFFFFFFF, false);
            g.drawString(font, Component.translatable("celular.time.now"), x + w - 6 - font.width(Component.translatable("celular.time.now")),
                    y + 4, 0xFF8A9099, false);
            for (int i = 0; i < n; i++) {
                g.drawString(font, lines.get(i), x + 27, y + 14 + i * 10, 0xFFC8CDD3, false);
            }
            y += (int) ((h + 4) * k);
        }
    }
}
