package com.celular.client;

import com.celular.plat.Ids;
import com.celular.plat.client.G;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

/** Dibujos que usan todas las pantallas del celular: rectángulos redondeados, iconos, caras de jugadores, colores. */
public final class D {
    public static final ResourceLocation APPS = Ids.mod("textures/gui/apps.png");
    public static final ResourceLocation UI = Ids.mod("textures/gui/ui.png");
    public static final ResourceLocation WALLS = Ids.mod("textures/gui/wallpapers.png");
    /** Cuántos fondos trae el celular (en wallpapers.png, uno al lado del otro, de 180 × 390). */
    public static final int WALL_COUNT = 6;
    public static final int WALL_W = 180, WALL_H = 390;

    // iconos de ui.png (24 × 24 cada uno, 8 por fila, 4 filas)
    public static final int HEART = 0, HEART_FULL = 1, HOME = 2, PLUS = 3, USER = 4, CALL = 5, HANGUP = 6, CAMERA = 7, SWITCH = 8,
            TRASH = 9, BACK = 10, SEND = 11, STAR = 12, WIFI = 13, BATTERY = 14, SIGNAL = 15, GALLERY = 16, GEAR = 17, MIC = 18,
            CHECK = 19, CLOCK = 20, PEOPLE = 21, CLOSE = 22, WALLPAPER = 23, NEXT = 24;
    // apps de apps.png (64 × 64 cada una)
    public static final int APP_INSTA = 0, APP_CAMERA = 1, APP_GALLERY = 2, APP_PHONE = 3, APP_PATREON = 4, APP_SETTINGS = 5;

    private D() {
    }

    public static void rr(G g, int x0, int y0, int x1, int y1, int r, int c) {
        r = Math.min(r, Math.min((x1 - x0) / 2, (y1 - y0) / 2));
        if (r <= 0) {
            g.fill(x0, y0, x1, y1, c);
            return;
        }
        g.fill(x0, y0 + r, x1, y1 - r, c);
        for (int i = 0; i < r; i++) {
            double dy = r - i - 0.5;
            int in = (int) Math.round(r - Math.sqrt(r * r - dy * dy));
            g.fill(x0 + in, y0 + i, x1 - in, y0 + i + 1, c);
            g.fill(x0 + in, y1 - i - 1, x1 - in, y1 - i, c);
        }
    }

    /** Borde de un rectángulo redondeado (1 de grosor). */
    public static void rrLine(G g, int x0, int y0, int x1, int y1, int r, int c) {
        r = Math.min(r, Math.min((x1 - x0) / 2, (y1 - y0) / 2));
        g.fill(x0 + r, y0, x1 - r, y0 + 1, c);
        g.fill(x0 + r, y1 - 1, x1 - r, y1, c);
        g.fill(x0, y0 + r, x0 + 1, y1 - r, c);
        g.fill(x1 - 1, y0 + r, x1, y1 - r, c);
        int lastIn = r;
        for (int i = 0; i < r; i++) {
            double dy = r - i - 0.5;
            int in = (int) Math.round(r - Math.sqrt(r * r - dy * dy));
            int w = Math.max(1, lastIn - in);
            if (i == 0) {
                w = 1;
            }
            g.fill(x0 + in, y0 + i, x0 + in + w, y0 + i + 1, c);
            g.fill(x1 - in - w, y0 + i, x1 - in, y0 + i + 1, c);
            g.fill(x0 + in, y1 - i - 1, x0 + in + w, y1 - i, c);
            g.fill(x1 - in - w, y1 - i - 1, x1 - in, y1 - i, c);
            lastIn = in;
        }
    }

    public static void circle(G g, int cx, int cy, int rad, int c) {
        rr(g, cx - rad, cy - rad, cx + rad, cy + rad, rad, c);
    }

    public static void icon(G g, int idx, int x, int y, int size, int argb) {
        g.blitTint(UI, x, y, size, size, (idx % 8) * 24, (idx / 8) * 24, 24, 24, 192, 96, argb);
    }

    public static void app(G g, int idx, int x, int y, int size) {
        g.blit(APPS, x, y, size, size, idx * 64, 0, 64, 64, 384, 64);
    }

    public static ResourceLocation skin(UUID id) {
        ClientPacketListener conn = Minecraft.getInstance().getConnection();
        PlayerInfo info = conn != null && id != null ? conn.getPlayerInfo(id) : null;
        UUID u = id == null ? new UUID(0, 0) : id;
        //#if MC >= 26.1
        //$$ return info != null ? info.getSkin().body().texturePath() : DefaultPlayerSkin.get(u).body().texturePath();
        //#elif MC >= 1.20.2
        //$$ return info != null ? info.getSkin().texture() : DefaultPlayerSkin.get(u).texture();
        //#else
        return info != null ? info.getSkinLocation() : DefaultPlayerSkin.getDefaultSkin(u);
        //#endif
    }

    /** La cara del jugador (con su "sombrero"). */
    public static void face(G g, UUID id, int x, int y, int size) {
        ResourceLocation s = skin(id);
        g.blit(s, x, y, size, size, 8, 8, 8, 8, 64, 64);
        g.blit(s, x, y, size, size, 40, 8, 8, 8, 64, 64);
    }

    /** Cara con un marco redondeado alrededor. */
    public static void faceRound(G g, UUID id, int x, int y, int size, int ring) {
        rr(g, x - 1, y - 1, x + size + 1, y + size + 1, Math.max(2, size / 4), ring);
        face(g, id, x, y, size);
    }

    public static String cut(Font f, String s, int w) {
        if (f.width(s) <= w) {
            return s;
        }
        return f.plainSubstrByWidth(s, Math.max(0, w - f.width("…"))) + "…";
    }

    public static int mix(int a, int b, float t) {
        t = Math.max(0, Math.min(1, t));
        int aa = a >>> 24, ar = a >> 16 & 255, ag = a >> 8 & 255, ab = a & 255;
        int ba = b >>> 24, br = b >> 16 & 255, bg = b >> 8 & 255, bb = b & 255;
        return (int) (aa + (ba - aa) * t) << 24 | (int) (ar + (br - ar) * t) << 16 | (int) (ag + (bg - ag) * t) << 8 | (int) (ab + (bb - ab) * t);
    }

    public static int alpha(int c, float a) {
        return Math.max(0, Math.min(255, (int) ((c >>> 24) * a))) << 24 | (c & 0xFFFFFF);
    }

    public static float easeOut(float t) {
        t = Math.max(0, Math.min(1, t));
        return 1 - (1 - t) * (1 - t) * (1 - t);
    }

    public static float easeBack(float t) {
        t = Math.max(0, Math.min(1, t));
        float c1 = 1.70158F, c3 = c1 + 1;
        return 1 + c3 * (float) Math.pow(t - 1, 3) + c1 * (float) Math.pow(t - 1, 2);
    }

    /** "ahora", "hace 5 min", "hace 2 h", "hace 3 d". */
    public static net.minecraft.network.chat.Component ago(long time) {
        long s = Math.max(0, (System.currentTimeMillis() - time) / 1000);
        if (s < 60) {
            return net.minecraft.network.chat.Component.translatable("celular.time.now");
        }
        if (s < 3600) {
            return net.minecraft.network.chat.Component.translatable("celular.time.min", s / 60);
        }
        if (s < 86400) {
            return net.minecraft.network.chat.Component.translatable("celular.time.hour", s / 3600);
        }
        return net.minecraft.network.chat.Component.translatable("celular.time.day", s / 86400);
    }

    public static String clock(int secs) {
        return String.format(java.util.Locale.ROOT, "%d:%02d", secs / 60, secs % 60);
    }
}
