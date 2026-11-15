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
    }

    private static final String[] TOGGLES = {"dark", "notifications", "shutter", "grid"};

    private boolean value(int i) {
        return switch (i) {
            case 0 -> Prefs.dark;
            case 1 -> Prefs.notifications;
            case 2 -> Prefs.shutterSound;
            default -> Prefs.grid;
        };
    }

    private void flip(int i) {
        switch (i) {
            case 0 -> Prefs.dark = !Prefs.dark;
            case 1 -> Prefs.notifications = !Prefs.notifications;
            case 2 -> Prefs.shutterSound = !Prefs.shutterSound;
            default -> Prefs.grid = !Prefs.grid;
        }
        Prefs.save();
    }

    @Override
    void draw(G g, int mx, int my, float pt) {
        Font f = font();
        g.fill(s.dx0, s.dy0, s.dx1, s.dy1, s.bg());
        if (walls) {
            drawWalls(g, mx, my);
            return;
        }
        int y = header(g, "celular.app.settings", false, mx, my) + 6;
        // fondo de pantalla
        int x0 = s.dx0 + 6, x1 = s.dx1 - 6;
        D.rr(g, x0, y, x1, y + 22, 6, s.card());
        boolean hw = s.in(mx, my, x0, y, x1, y + 22);
        D.rr(g, x0 + 4, y + 4, x0 + 18, y + 18, 4, 0xFF5E5CE6);
        D.icon(g, D.WALLPAPER, x0 + 5, y + 5, 12, 0xFFFFFFFF);
        g.drawString(f, Component.translatable("celular.settings.wallpaper"), x0 + 23, y + 7, hw ? s.accent() : s.fg(), false);
        D.icon(g, D.NEXT, x1 - 12, y + 6, 10, s.sub());
        y += 28;
        int[] colors = {0xFF636366, 0xFFFF3B30, 0xFFFF9F0A, 0xFF34C759};
        int[] icons = {D.GEAR, D.CHECK, D.CAMERA, D.GALLERY};
        D.rr(g, x0, y, x1, y + TOGGLES.length * 22, 6, s.card());
        for (int i = 0; i < TOGGLES.length; i++) {
            int ry = y + i * 22;
            D.rr(g, x0 + 4, ry + 4, x0 + 18, ry + 18, 4, colors[i]);
            D.icon(g, icons[i], x0 + 5, ry + 5, 12, 0xFFFFFFFF);
            g.drawString(f, D.cut(f, Component.translatable("celular.settings." + TOGGLES[i]).getString(), x1 - x0 - 50), x0 + 23, ry + 7, s.fg(), false);
            boolean on = value(i);
            int sx = x1 - 26, sy = ry + 6;
            D.rr(g, sx, sy, sx + 20, sy + 11, 5, on ? 0xFF34C759 : (Prefs.dark ? 0xFF39393D : 0xFFD1D1D6));
            D.circle(g, on ? sx + 14 : sx + 6, sy + 5, 4, 0xFFFFFFFF);
            if (i < TOGGLES.length - 1) {
                g.fill(x0 + 23, ry + 21, x1, ry + 22, s.line());
            }
        }
        y += TOGGLES.length * 22 + 10;
        // acerca de
        Component about = Component.translatable("celular.settings.about").withStyle(ChatFormatting.BOLD);
        g.drawString(f, about, x0 + 2, y, s.sub(), false);
        y += 11;
        for (net.minecraft.util.FormattedCharSequence l : f.split(Component.translatable("celular.settings.about_text"), x1 - x0 - 4)) {
            g.push();
            g.translate(x0 + 2, y, 0);
            g.scale(0.75F, 0.75F, 1);
            g.drawString(f, l, 0, 0, s.sub(), false);
            g.pop();
            y += 8;
        }
    }

    private int wallCell() {
        return (s.dw() - 16) / 3;
    }

    private void drawWalls(G g, int mx, int my) {
        int top = header(g, "celular.settings.wallpaper", true, mx, my) + 6;
        int c = wallCell(), ch = (int) (c * 1.9F);
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
        int y = top + 2 * (ch + 6) + 4;
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
            int c = wallCell(), ch = (int) (c * 1.9F);
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
        int y0 = s.cy0 + 19 + 6;
        int x0 = s.dx0 + 6, x1 = s.dx1 - 6;
        if (s.in(x, y, x0, y0, x1, y0 + 22)) {
            walls = true;
            ClientHooks.tap();
            return true;
        }
        int ty = y0 + 28;
        for (int i = 0; i < TOGGLES.length; i++) {
            if (s.in(x, y, x0, ty + i * 22, x1, ty + i * 22 + 22)) {
                flip(i);
                ClientHooks.tap();
                return true;
            }
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
