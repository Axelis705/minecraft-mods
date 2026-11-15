package com.celular.client;

import com.celular.plat.client.G;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Galería: las fotos tomadas con la cámara. Al tocar una se ve en grande y se puede publicar, ponerla de fondo o borrarla. */
final class GalleryApp extends App {
    private List<Path> photos = new ArrayList<>();
    private Path open;
    private final Scroll scroll = new Scroll();
    private boolean confirmDelete;
    private long wallpaperSetAt;
    /** Si se abrió desde Instacraft para escoger qué publicar. */
    boolean picking;

    GalleryApp(PhoneScreen s) {
        super(s, D.APP_GALLERY, "gallery");
    }

    @Override
    void enter() {
        photos = Photos.gallery();
        open = null;
        confirmDelete = false;
        scroll.reset();
    }

    @Override
    void leave() {
        picking = false;
    }

    private int cell() {
        return (s.dw() - 2) / 3;
    }

    @Override
    void draw(G g, int mx, int my, float pt) {
        g.fill(s.dx0, s.dy0, s.dx1, s.dy1, s.bg());
        if (open != null) {
            drawPhoto(g, mx, my);
            return;
        }
        int top = header(g, picking ? "celular.gallery.pick" : "celular.app.gallery", picking, mx, my);
        // botón de la cámara
        boolean hc = s.in(mx, my, s.dx1 - 18, s.cy0 + 2, s.dx1 - 2, s.cy0 + 16);
        D.icon(g, D.CAMERA, s.dx1 - 16, s.cy0 + 3, 12, hc ? s.accent() : s.fg());
        if (photos.isEmpty()) {
            D.icon(g, D.GALLERY, (s.dx0 + s.dx1) / 2 - 12, top + 30, 24, s.sub());
            centered(g, Component.translatable("celular.gallery.empty"), top + 60, s.sub());
            button(g, s.dx0 + 16, top + 92, s.dx1 - 16, top + 108, Component.translatable("celular.gallery.open_camera"), s.accent(), mx, my);
            return;
        }
        int c = cell();
        int rows = (photos.size() + 2) / 3;
        scroll.update(rows * (c + 1) + 14, s.cy1 - top);
        g.scissor(s.dx0, top, s.dx1, s.cy1);
        for (int i = 0; i < photos.size(); i++) {
            int x = s.dx0 + (i % 3) * (c + 1), y = top + (i / 3) * (c + 1) - scroll.off();
            if (y + c < top || y > s.cy1) {
                continue;
            }
            ResourceLocation t = Photos.local(photos.get(i));
            if (t != null) {
                g.blit(t, x, y, c, c, 0, 0, Photos.SIZE, Photos.SIZE, Photos.SIZE, Photos.SIZE);
            } else {
                g.fill(x, y, x + c, y + c, s.card());
            }
            if (s.in(mx, my, x, y, x + c, y + c) && my >= top && my < s.cy1) {
                g.fill(x, y, x + c, y + c, 0x30FFFFFF);
            }
        }
        g.noScissor();
        Component n = Component.translatable("celular.gallery.count", photos.size());
        int ny = top + rows * (c + 1) + 3 - scroll.off();
        if (ny < s.cy1 - 8) {
            g.push();
            g.translate((s.dx0 + s.dx1) / 2F, ny, 0);
            g.scale(0.75F, 0.75F, 1);
            g.drawString(font(), n, -font().width(n) / 2, 0, s.sub(), false);
            g.pop();
        }
    }

    private void drawPhoto(G g, int mx, int my) {
        g.fill(s.dx0, s.dy0, s.dx1, s.dy1, 0xFF000000);
        int top = s.cy0;
        boolean hb = s.in(mx, my, s.dx0, top, s.dx0 + 18, top + 18);
        D.icon(g, D.BACK, s.dx0 + 3, top + 3, 12, hb ? s.accent() : 0xFFFFFFFF);
        String name = open.getFileName().toString().replace(".jpg", "").replace("foto_", "");
        g.push();
        g.translate(s.dx0 + 20, top + 6, 0);
        g.scale(0.75F, 0.75F, 1);
        g.drawString(font(), pretty(name), 0, 0, 0xFFB0B0B0, false);
        g.pop();
        int w = s.dw();
        int y = top + 22;
        ResourceLocation t = Photos.local(open);
        if (t != null) {
            g.blit(t, s.dx0, y, w, w, 0, 0, Photos.SIZE, Photos.SIZE, Photos.SIZE, Photos.SIZE);
        } else {
            spinner(g, (s.dx0 + s.dx1) / 2, y + w / 2, 0xFFFFFFFF);
        }
        int by = Math.min(s.cy1 - 40, y + w + 8);
        int half = (s.dw() - 18) / 2;
        button(g, s.dx0 + 6, by, s.dx0 + 6 + half, by + 16, Component.translatable("celular.gallery.post"), 0xFFE1306C, mx, my);
        boolean wallOk = System.currentTimeMillis() - wallpaperSetAt < 1500;
        button(g, s.dx1 - 6 - half, by, s.dx1 - 6, by + 16, Component.translatable(wallOk ? "celular.gallery.wallpaper_ok" : "celular.gallery.wallpaper"),
                wallOk ? 0xFF30A050 : 0xFF3A3A3C, mx, my);
        button(g, s.dx0 + 6, by + 20, s.dx1 - 6, by + 36, Component.translatable(confirmDelete ? "celular.gallery.delete_sure" : "celular.gallery.delete")
                .withStyle(confirmDelete ? ChatFormatting.BOLD : ChatFormatting.RESET), confirmDelete ? 0xFFD03030 : 0xFF2C2C2E, mx, my);
    }

    private static String pretty(String n) {
        // 20261115_142301 -> 15/11/2026 14:23
        if (n.length() >= 15 && n.charAt(8) == '_') {
            return n.substring(6, 8) + "/" + n.substring(4, 6) + "/" + n.substring(0, 4) + " " + n.substring(9, 11) + ":" + n.substring(11, 13);
        }
        return n;
    }

    @Override
    boolean click(double x, double y, int b) {
        if (open != null) {
            int top = s.cy0;
            if (s.in(x, y, s.dx0, top, s.dx0 + 18, top + 18)) {
                back();
                return true;
            }
            int w = s.dw();
            int by = Math.min(s.cy1 - 40, top + 22 + w + 8);
            int half = (s.dw() - 18) / 2;
            if (s.in(x, y, s.dx0 + 6, by, s.dx0 + 6 + half, by + 16)) {
                ClientHooks.tap();
                Path p = open;
                InstaApp ia = (InstaApp) s.switchTo("insta");
                if (ia != null) {
                    ia.compose(p);
                }
                return true;
            }
            if (s.in(x, y, s.dx1 - 6 - half, by, s.dx1 - 6, by + 16)) {
                Prefs.wallpaper = -1;
                Prefs.wallpaperPhoto = open.toString();
                Prefs.save();
                wallpaperSetAt = System.currentTimeMillis();
                ClientHooks.tap();
                return true;
            }
            if (s.in(x, y, s.dx0 + 6, by + 20, s.dx1 - 6, by + 36)) {
                if (!confirmDelete) {
                    confirmDelete = true;
                } else {
                    Photos.delete(open);
                    if (open.toString().equals(Prefs.wallpaperPhoto)) {
                        Prefs.wallpaper = 0;
                        Prefs.save();
                    }
                    photos = Photos.gallery();
                    open = null;
                    confirmDelete = false;
                }
                ClientHooks.tap();
                return true;
            }
            confirmDelete = false;
            return true;
        }
        if (picking && headerArrow(x, y)) {
            s.switchTo("insta");
            return true;
        }
        if (s.in(x, y, s.dx1 - 18, s.cy0, s.dx1, s.cy0 + 18) || (photos.isEmpty() && s.in(x, y, s.dx0 + 16, s.cy0 + 19 + 92, s.dx1 - 16, s.cy0 + 19 + 108))) {
            ClientHooks.tap();
            Minecraft.getInstance().setScreen(new CameraScreen(s.color));
            return true;
        }
        int top = s.cy0 + 19, c = cell();
        if (y < top || y >= s.cy1) {
            return false;
        }
        for (int i = 0; i < photos.size(); i++) {
            int px = s.dx0 + (i % 3) * (c + 1), py = top + (i / 3) * (c + 1) - scroll.off();
            if (s.in(x, y, px, py, px + c, py + c)) {
                ClientHooks.tap();
                if (picking) {
                    Path p = photos.get(i);
                    InstaApp ia = (InstaApp) s.switchTo("insta");
                    if (ia != null) {
                        ia.compose(p);
                    }
                } else {
                    open = photos.get(i);
                    confirmDelete = false;
                }
                return true;
            }
        }
        return false;
    }

    @Override
    boolean drag(double x, double y, double dx, double dy) {
        if (open == null) {
            scroll.drag(dy);
        }
        return true;
    }

    @Override
    boolean scroll(double x, double y, double d) {
        scroll.wheel(d);
        return true;
    }

    @Override
    boolean back() {
        if (open != null) {
            open = null;
            confirmDelete = false;
            return true;
        }
        if (picking) {
            s.switchTo("insta");
            return true;
        }
        return false;
    }
}
