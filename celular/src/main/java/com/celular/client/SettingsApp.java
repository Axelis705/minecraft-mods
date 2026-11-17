package com.celular.client;

import com.celular.plat.client.G;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;

/** Ajustes: fondo de pantalla, modo oscuro, notificaciones, sonido de la cámara y la cuadrícula del visor. */
final class SettingsApp extends App {
    private boolean walls;

    SettingsApp(PhoneScreen s) {
        super(s, D.APP_SETTINGS, "settings");
    }

    @Override
    void enter() {
        walls = false;
        scroll.reset();
    }

    private final Scroll scroll = new Scroll();
    private static final String[] TOGGLES = {"dark", "lock", "notifications", "msg_toasts", "shutter", "grid", "music_mute", "music_toasts",
            "speaker", "hear_others", "party", "share_location"};

    private boolean value(int i) {
        return switch (i) {
            case 0 -> Prefs.dark;
            case 1 -> Prefs.lockScreen;
            case 2 -> Prefs.notifications;
            case 3 -> Prefs.msgToasts;
            case 4 -> Prefs.shutterSound;
            case 5 -> Prefs.grid;
            case 6 -> Prefs.muteGameMusic;
            case 7 -> Prefs.musicToasts;
            case 8 -> Prefs.speaker;
            case 9 -> Prefs.hearOthers;
            case 10 -> Prefs.partyNotes;
            default -> Prefs.shareLocation;
        };
    }

    private void flip(int i) {
        switch (i) {
            case 0 -> Prefs.dark = !Prefs.dark;
            case 1 -> Prefs.lockScreen = !Prefs.lockScreen;
            case 2 -> Prefs.notifications = !Prefs.notifications;
            case 3 -> Prefs.msgToasts = !Prefs.msgToasts;
            case 4 -> Prefs.shutterSound = !Prefs.shutterSound;
            case 5 -> Prefs.grid = !Prefs.grid;
            case 6 -> Prefs.muteGameMusic = !Prefs.muteGameMusic;
            case 7 -> Prefs.musicToasts = !Prefs.musicToasts;
            case 8 -> Prefs.speaker = !Prefs.speaker;
            case 9 -> Prefs.hearOthers = !Prefs.hearOthers;
            case 10 -> Prefs.partyNotes = !Prefs.partyNotes;
            default -> {
                Prefs.shareLocation = !Prefs.shareLocation;
                com.celular.plat.Net.toServer(new com.celular.net.AppNet.Req(com.celular.net.AppNet.Req.SHARE, null, Prefs.shareLocation ? 1 : 0, ""));
            }
        }
        Prefs.save();
    }

    @Override
    void draw(G g, int mx, int my, float pt) {
        hits.clear();
        Font f = font();
        g.fill(s.dx0, s.dy0, s.dx1, s.dy1, s.bg());
        if (walls) {
            drawWalls(g, mx, my);
            return;
        }
        int top = header(g, "celular.app.settings", false, mx, my);
        int x0 = s.dx0 + 6, x1 = s.dx1 - 6;
        // todo lo de abajo se desplaza
        int[] colors = {0xFF636366, 0xFF5856D6, 0xFFFF3B30, 0xFF30D158, 0xFFFF9F0A, 0xFF34C759, 0xFFFA233B, 0xFFFF2D55, 0xFF1DB954, 0xFF0A84FF,
                0xFFBF5AF2, 0xFFFF9500};
        int[] icons = {D.MOON, D.LOCK, D.CHECK, D.CHAT, D.CAMERA, D.GALLERY, D.NOTE, D.NOTE, D.SPEAKER, D.HEADPHONES, D.STAR, D.PIN};
        Component about = Component.translatable("celular.settings.about").withStyle(ChatFormatting.BOLD);
        java.util.List<net.minecraft.util.FormattedCharSequence> aboutLines = f.split(Component.translatable("celular.settings.about_text"),
                (int) ((x1 - x0 - 4) / 0.75F));
        int content = 6 + 28 + TOGGLES.length * 22 + 10 + 11 + aboutLines.size() * 8 + 6;
        scroll.update(content, s.cy1 - top);
        g.scissor(s.dx0, top, s.dx1, s.cy1);
        int y = top + 6 - scroll.off();
        // fondo de pantalla
        D.rr(g, x0, y, x1, y + 22, 6, s.card());
        boolean hw = s.in(mx, my, x0, Math.max(y, top), x1, y + 22);
        D.rr(g, x0 + 4, y + 4, x0 + 18, y + 18, 4, 0xFF5E5CE6);
        D.icon(g, D.WALLPAPER, x0 + 5, y + 5, 12, 0xFFFFFFFF);
        g.drawString(f, Component.translatable("celular.settings.wallpaper"), x0 + 23, y + 7, hw ? s.accent() : s.fg(), false);
        D.icon(g, D.NEXT, x1 - 12, y + 6, 10, s.sub());
        hitIn(x0, y, x1, y + 22, top, s.cy1, () -> {
            walls = true;
            scroll.reset();
        });
        y += 28;
        D.rr(g, x0, y, x1, y + TOGGLES.length * 22, 6, s.card());
        for (int i = 0; i < TOGGLES.length; i++) {
            int ry = y + i * 22;
            D.rr(g, x0 + 4, ry + 4, x0 + 18, ry + 18, 4, colors[i]);
            D.icon(g, icons[i], x0 + 5, ry + 5, 12, 0xFFFFFFFF);
            g.drawString(f, D.cut(f, Component.translatable("celular.settings." + TOGGLES[i]).getString(), x1 - x0 - 50), x0 + 23, ry + 7, s.fg(), false);
            boolean on = value(i);
            int sx = x1 - 26, sy = ry + 6;
            Anim.toggle(g, sx, sy, on, "set:" + TOGGLES[i]);
            if (i < TOGGLES.length - 1) {
                g.fill(x0 + 23, ry + 21, x1, ry + 22, s.line());
            }
            int ti = i;
            hitIn(x0, ry, x1, ry + 22, top, s.cy1, () -> flip(ti));
        }
        y += TOGGLES.length * 22 + 10;
        // acerca de
        g.drawString(f, about, x0 + 2, y, s.sub(), false);
        y += 11;
        for (net.minecraft.util.FormattedCharSequence l : aboutLines) {
            g.push();
            g.translate(x0 + 2, y, 0);
            g.scale(0.75F, 0.75F, 1);
            g.drawString(f, l, 0, 0, s.sub(), false);
            g.pop();
            y += 8;
        }
        g.noScissor();
    }

    private int wallCell() {
        return (s.dw() - 16) / 3;
    }

    /** Alto de cada fondo en la lista (más bajitos si no caben las 3 filas). */
    private int wallH(int c) {
        int rows = (D.WALL_COUNT + 2) / 3;
        return Math.min((int) (c * 1.9F), (s.cy1 - s.cy0 - 60) / rows - 6);
    }

    private void drawWalls(G g, int mx, int my) {
        int top = header(g, "celular.settings.wallpaper", true, mx, my) + 6;
        int c = wallCell(), ch = wallH(c);
        for (int i = 0; i < D.WALL_COUNT; i++) {
            int x = s.dx0 + 5 + (i % 3) * (c + 3), y = top + (i / 3) * (ch + 6);
            boolean sel = Prefs.wallpaper == i;
            if (sel) {
                D.rr(g, x - 2, y - 2, x + c + 2, y + ch + 2, 5, s.accent());
            }
            int uw = Math.round(D.WALL_H * (c / (float) ch));
            g.blit(D.WALLS, x, y, c, ch, i * D.WALL_W + (D.WALL_W - uw) / 2F, 0, uw, D.WALL_H, D.WALL_W * D.WALL_COUNT, D.WALL_H);
            if (s.in(mx, my, x, y, x + c, y + ch) && !sel) {
                g.fill(x, y, x + c, y + ch, 0x30FFFFFF);
            }
        }
        int y = top + (D.WALL_COUNT + 2) / 3 * (ch + 6) + 4;
        centered(g, Component.translatable("celular.settings.wallpaper_photo"), y, s.sub());
    }

    @Override
    boolean click(double x, double y, int b) {
        if (walls) {
            if (headerArrow(x, y)) {
                walls = false;
                ClientHooks.tap();
                return true;
            }
            int top = s.cy0 + 19 + 6;
            int c = wallCell(), ch = wallH(c);
            for (int i = 0; i < D.WALL_COUNT; i++) {
                int wx = s.dx0 + 5 + (i % 3) * (c + 3), wy = top + (i / 3) * (ch + 6);
                if (s.in(x, y, wx, wy, wx + c, wy + ch)) {
                    Prefs.wallpaper = i;
                    Prefs.save();
                    ClientHooks.tap();
                    return true;
                }
            }
            return true;
        }
        clickHits(x, y);
        return true;
    }

    @Override
    boolean drag(double x, double y, double dx, double dy) {
        if (!walls) {
            scroll.drag(dy);
        }
        return true;
    }

    @Override
    boolean scroll(double x, double y, double d) {
        if (!walls) {
            scroll.wheel(d);
        }
        return true;
    }

    @Override
    boolean back() {
        if (walls) {
            walls = false;
            return true;
        }
        return false;
    }
}
