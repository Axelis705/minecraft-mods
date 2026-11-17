package com.celular.client;

import com.celular.CelularReg;
import com.celular.net.CelularNet.CallState;
import com.celular.plat.client.BaseScreen;
import com.celular.plat.client.G;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;

/**
 * El celular en la mano: la pantalla de inicio (reloj, fecha y apps) y las apps. Sube desde abajo al abrirse; las apps se abren con
 * una animación desde su icono. Esc o la barrita de abajo regresan; en el inicio, Esc guarda el celular.
 */
public class PhoneScreen extends BaseScreen {
    /** Colores de la carcasa (mismo orden que CelularReg.COLORS): base y brillo. */
    static final int[][] CASE = {
            {0xFF1C1D21, 0xFF3A3C44}, {0xFFE8E8EC, 0xFFFFFFFF}, {0xFFF2A7C3, 0xFFFFD3E4}, {0xFF2F6FD6, 0xFF6FA2FF},
            {0xFF2E9E62, 0xFF6BD99C}, {0xFF7B4FD1, 0xFFB08CFF}, {0xFFD4AF37, 0xFFFFE08A}, {0xFFD03A3A, 0xFFFF7A7A}};

    final int color;
    private final String startApp;
    // marco del celular y su pantalla
    int px, py, pw, ph;
    int dx0, dy0, dx1, dy1;
    /** Contenido: debajo de la barra de estado y encima de la barrita de inicio. */
    int cy0, cy1;

    final App[] apps;
    App app;
    /** Animación al abrir/cerrar una app. */
    private App animApp;
    private long animAt;
    private boolean animOpening;
    private int animIx, animIy, animIs;
    private long openedAt;
    private EditBox box;
    /** Pantalla de bloqueo: activa y cuándo empezó a quitarse. */
    private boolean locked;
    private long unlockAt;

    public PhoneScreen(int color, String startApp) {
        super(Component.translatable("item.celular.phone"));
        this.color = Math.max(0, Math.min(CASE.length - 1, color));
        this.startApp = startApp;
        Prefs.load();
        apps = new App[]{new InstaApp(this), new CameraApp(this), new GalleryApp(this), new PhoneApp(this), new PatreonApp(this),
                new SettingsApp(this), new MusicApp(this), new WeatherApp(this), new CompassApp(this), new CalcApp(this), new FlashApp(this)};
        Music.init();
    }

    @Override
    protected void init() {
        ph = Math.max(190, Math.min(height - 14, 300));
        pw = Math.round(ph * 0.49F);
        px = width / 2 - pw / 2;
        py = height / 2 - ph / 2;
        dx0 = px + 5;
        dy0 = py + 5;
        dx1 = px + pw - 5;
        dy1 = py + ph - 5;
        cy0 = dy0 + 13;
        cy1 = dy1 - 9;
        if (openedAt == 0) {
            openedAt = System.currentTimeMillis();
            locked = Prefs.lockScreen && startApp == null && ClientHooks.call.state == CallState.IDLE;
            if (!locked) {
                ClientHooks.play(CelularReg.UNLOCK.get(), 1.0F, 0.4F);
            }
            if (ClientHooks.call.state != CallState.IDLE) {
                app = apps[3];
                app.enter();
            } else if (startApp != null) {
                for (App a : apps) {
                    if (a.key.equals(startApp)) {
                        app = a;
                        app.enter();
                    }
                }
            }
        } else if (app != null) {
            // cambió el tamaño de la ventana: la app vuelve a poner sus cajas de texto
            removeBox();
            app.enter();
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void removed() {
        if (app != null) {
            app.leave();
        }
        super.removed();
    }

    /** Qué pantalla ven los demás en mi celular (ver PhoneUse). */
    int useScreen() {
        if (locked || app == null) {
            return com.celular.PhoneUse.HOME;
        }
        return switch (app.key) {
            case "insta" -> com.celular.PhoneUse.INSTA;
            case "camera" -> com.celular.PhoneUse.CAMERA;
            case "gallery" -> com.celular.PhoneUse.GALLERY;
            case "phone" -> com.celular.PhoneUse.CALL;
            case "patreon" -> com.celular.PhoneUse.PATREON;
            case "settings" -> com.celular.PhoneUse.SETTINGS;
            case "music" -> Radio.inJam() ? com.celular.PhoneUse.JAM : com.celular.PhoneUse.MUSIC;
            case "weather" -> com.celular.PhoneUse.WEATHER;
            case "compass" -> com.celular.PhoneUse.COMPASS;
            case "calc" -> com.celular.PhoneUse.CALC;
            case "flash" -> Flash.on ? com.celular.PhoneUse.FLASH : com.celular.PhoneUse.HOME;
            case "messages" -> com.celular.PhoneUse.MESSAGES;
            case "notes" -> com.celular.PhoneUse.NOTES;
            case "map" -> com.celular.PhoneUse.MAP;
            case "wallet" -> com.celular.PhoneUse.WALLET;
            default -> com.celular.PhoneUse.HOME;
        };
    }

    // ------------------------------------------------------------------ colores del tema

    int bg() {
        return Prefs.dark ? 0xFF000000 : 0xFFFFFFFF;
    }

    int card() {
        return Prefs.dark ? 0xFF1C1C1E : 0xFFF2F2F7;
    }

    int fg() {
        return Prefs.dark ? 0xFFFFFFFF : 0xFF111111;
    }

    int sub() {
        return Prefs.dark ? 0xFF8E8E93 : 0xFF6E6E73;
    }

    int line() {
        return Prefs.dark ? 0xFF262628 : 0xFFDCDCE0;
    }

    int accent() {
        return 0xFF0A84FF;
    }

    boolean in(double x, double y, double x0, double y0, double x1, double y1) {
        return x >= x0 && x < x1 && y >= y0 && y < y1;
    }

    int dw() {
        return dx1 - dx0;
    }

    // ------------------------------------------------------------------ cajas de texto de las apps

    EditBox box(int x, int y, int w, int h, String hintKey, int max) {
        removeBox();
        box = new EditBox(Minecraft.getInstance().font, x, y, w, h, Component.translatable(hintKey));
        box.setMaxLength(max);
        box.setBordered(false);
        box.setTextColor(fg());
        addRenderableWidget(box);
        setFocused(box);
        box.setFocused(true);
        return box;
    }

    String boxValue() {
        return box == null ? "" : box.getValue();
    }

    void removeBox() {
        if (box != null) {
            removeWidget(box);
            box = null;
        }
    }

    // ------------------------------------------------------------------ navegación

    void openApp(App a, int ix, int iy, int is) {
        if (animApp != null) {
            return;
        }
        ClientHooks.tap();
        animApp = a;
        animOpening = true;
        animAt = System.currentTimeMillis();
        animIx = ix;
        animIy = iy;
        animIs = is;
        if (a instanceof CameraApp) {
            return; // la cámara abre su propia pantalla al terminar la animación
        }
        app = a;
        a.enter();
    }

    void openApp(String key) {
        for (int i = 0; i < apps.length; i++) {
            if (apps[i].key.equals(key)) {
                int[] r = iconRect(i);
                openApp(apps[i], r[0], r[1], r[2]);
                return;
            }
        }
    }

    /** Cambia a otra app sin animación (por ejemplo de la galería a publicar en Instacraft). */
    App switchTo(String key) {
        for (App a : apps) {
            if (a.key.equals(key)) {
                if (app != null) {
                    app.leave();
                }
                removeBox();
                app = a;
                a.enter();
                return a;
            }
        }
        return null;
    }

    void goHome() {
        if (app == null) {
            return;
        }
        app.leave();
        removeBox();
        int idx = java.util.Arrays.asList(apps).indexOf(app);
        int[] r = iconRect(Math.max(0, idx));
        animApp = app;
        animOpening = false;
        animAt = System.currentTimeMillis();
        animIx = r[0];
        animIy = r[1];
        animIs = r[2];
        app = null;
        ClientHooks.tap();
    }

    /** Llegó una llamada con el celular abierto: se va directo a la llamada. */
    void openCall() {
        if (app != apps[3]) {
            switchTo("phone");
        }
    }

    // ------------------------------------------------------------------ inicio

    /** Las apps de abajo (el "dock"): Teléfono, Instacraft, Cámara y Música. Las demás van en la cuadrícula. */
    private static final int[] DOCK = {3, 0, 1, 6};
    /** Orden de la cuadrícula. */
    private static final int[] GRID = {2, 7, 8, 9, 10, 4, 5};

    private int cols() {
        return 4;
    }

    private int iconSize() {
        return Math.max(20, Math.min(28, dw() / 4 - 8));
    }

    private int dockIndex(int app) {
        for (int k = 0; k < DOCK.length; k++) {
            if (DOCK[k] == app) {
                return k;
            }
        }
        return -1;
    }

    /** x, y, tamaño del icono de la app i en el inicio. */
    int[] iconRect(int i) {
        int is = iconSize(), cols = cols();
        int cell = dw() / cols;
        int d = dockIndex(i);
        if (d >= 0) {
            int x = dx0 + d * cell + cell / 2 - is / 2;
            return new int[]{x, dockTop() + (dockH() - is) / 2, is};
        }
        int g = 0;
        for (int k = 0; k < GRID.length; k++) {
            if (GRID[k] == i) {
                g = k;
            }
        }
        int col = g % cols, row = g / cols;
        int x = dx0 + col * cell + cell / 2 - is / 2;
        int y = gridTop() + row * (is + 16);
        return new int[]{x, y, is};
    }

    private int dockH() {
        return iconSize() + 12;
    }

    private int dockTop() {
        return cy1 - dockH() - 4;
    }

    private int gridTop() {
        int rows = (GRID.length + cols() - 1) / cols();
        return dockTop() - 8 - rows * (iconSize() + 16);
    }

    /** Recuadro del widget (música o clima) entre el reloj y las apps. */
    private int[] widgetRect() {
        int y0 = cy0 + 58, y1 = Math.min(y0 + 40, gridTop() - 8);
        return new int[]{dx0 + 7, y0, dx1 - 7, y1};
    }

    private void drawWallpaper(G g) {
        long t = System.currentTimeMillis();
        int w = dw(), h = dy1 - dy0;
        boolean drawn = false;
        if (Prefs.wallpaper == -1 && !Prefs.wallpaperPhoto.isEmpty()) {
            Path p = Paths.get(Prefs.wallpaperPhoto);
            if (Files.exists(p)) {
                ResourceLocation tex = Photos.local(p);
                if (tex != null) {
                    // la foto es cuadrada: se recorta al centro para llenar la pantalla
                    int uw = Math.round(Photos.SIZE * (w / (float) h));
                    g.blit(tex, dx0, dy0, w, h, (Photos.SIZE - uw) / 2F, 0, uw, Photos.SIZE, Photos.SIZE, Photos.SIZE);
                    g.fillGradient(dx0, dy0, dx1, dy0 + h / 3, 0x60000000, 0x00000000);
                    drawn = true;
                }
            }
        }
        if (!drawn) {
            int wi = Math.floorMod(Prefs.wallpaper, D.WALL_COUNT);
            int uw = Math.min(D.WALL_W, Math.round(D.WALL_H * (w / (float) h)));
            g.blit(D.WALLS, dx0, dy0, w, h, wi * D.WALL_W + (D.WALL_W - uw) / 2F, 0, uw, D.WALL_H, D.WALL_W * D.WALL_COUNT, D.WALL_H);
            // brillos que suben despacio
            for (int i = 0; i < 9; i++) {
                double ph = (t / 9000.0 + i * 0.137) % 1.0;
                int x = dx0 + (int) ((Math.sin(i * 12.9898) * 0.5 + 0.5) * w);
                int y = dy1 - (int) (ph * h);
                float a = (float) Math.sin(ph * Math.PI) * 0.35F;
                g.fill(x, y, x + 1 + i % 2, y + 1 + i % 2, D.alpha(0xFFFFFFFF, a));
            }
        }
    }

    private int badge(int i) {
        return i == 0 ? ClientHooks.badgeInsta : i == 3 ? ClientHooks.badgeCalls : 0;
    }

    private void drawHome(G g, int mx, int my, boolean interactive) {
        drawWallpaper(g);
        Font font = Minecraft.getInstance().font;
        LocalDateTime now = LocalDateTime.now();
        // reloj grande
        String time = String.format(java.util.Locale.ROOT, "%d:%02d", now.getHour(), now.getMinute());
        Component date = Component.translatable("celular.date", Component.translatable("celular.day." + now.getDayOfWeek().getValue()),
                now.getDayOfMonth(), Component.translatable("celular.month." + now.getMonthValue()));
        int cx = (dx0 + dx1) / 2;
        g.drawCenteredString(font, date, cx, cy0 + 8, 0xFFFFFFFF);
        g.push();
        float sc = 3.0F;
        g.translate(cx, cy0 + 20, 0);
        g.scale(sc, sc, 1);
        g.drawString(font, time, -font.width(time) / 2, 0, 0xFFFFFFFF, true);
        g.pop();
        drawWidget(g, mx, my, interactive);
        // dock con fondo de vidrio
        int dt = dockTop();
        D.rr(g, dx0 + 4, dt, dx1 - 4, dt + dockH(), 10, 0x55FFFFFF);
        D.rrLine(g, dx0 + 4, dt, dx1 - 4, dt + dockH(), 10, 0x40FFFFFF);
        long t = System.currentTimeMillis();
        for (int i = 0; i < apps.length; i++) {
            int[] r = iconRect(i);
            boolean hov = interactive && in(mx, my, r[0], r[1], r[0] + r[2], r[1] + r[2]);
            if (animApp == apps[i] && animOpening) {
                continue; // está saliendo de aquí
            }
            int lift = hov ? 1 : 0;
            if (hov) {
                D.rr(g, r[0] - 2, r[1] - 2 - lift, r[0] + r[2] + 2, r[1] + r[2] + 2 - lift, r[2] / 4 + 2, 0x30FFFFFF);
            }
            D.rr(g, r[0] + 1, r[1] + 2, r[0] + r[2] + 1, r[1] + r[2] + 2, r[2] / 4, 0x50000000);
            D.app(g, apps[i].icon, r[0], r[1] - lift, r[2]);
            // la app de música "late" mientras suena
            if (i == 6 && com.celular.music.MusicPlayer.playing) {
                float p = (float) (0.5 + 0.5 * Math.sin(t / 220.0));
                D.circle(g, r[0] + r[2] - 1, r[1] + 1, 3, D.mix(0xFFFF2D55, 0xFFFFFFFF, p * 0.4F));
            }
            int bdg = badge(i);
            if (bdg > 0) {
                String b = bdg > 9 ? "9+" : Integer.toString(bdg);
                int bw = Math.max(9, font.width(b) + 4);
                D.rr(g, r[0] + r[2] - bw + 3, r[1] - 3, r[0] + r[2] + 3, r[1] + 6, 4, 0xFFFF3B30);
                g.drawString(font, b, r[0] + r[2] + 3 - bw / 2 - font.width(b) / 2, r[1] - 2, 0xFFFFFFFF, false);
            }
            if (dockIndex(i) < 0) {
                Component label = Component.translatable("celular.app." + apps[i].key);
                g.push();
                g.translate(r[0] + r[2] / 2F, r[1] + r[2] + 3, 0);
                g.scale(0.7F, 0.7F, 1);
                String ls = D.cut(font, label.getString(), (int) ((dw() / cols() - 1) / 0.7F));
                g.drawString(font, ls, -font.width(ls) / 2, 0, 0xFFFFFFFF, true);
                g.pop();
            }
        }
    }

    /** Widget del inicio: la canción que suena (con botones) o el clima y la hora de Minecraft. */
    private void drawWidget(G g, int mx, int my, boolean interactive) {
        int[] w = widgetRect();
        if (w[3] - w[1] < 26) {
            return;
        }
        Font font = Minecraft.getInstance().font;
        boolean hov = interactive && in(mx, my, w[0], w[1], w[2], w[3]);
        D.rr(g, w[0], w[1], w[2], w[3], 9, hov ? 0x70000000 : 0x55000000);
        D.rrLine(g, w[0], w[1], w[2], w[3], 9, 0x30FFFFFF);
        int h = w[3] - w[1];
        com.celular.music.Track tr = com.celular.music.MusicPlayer.current;
        if (tr != null) {
            int cs = h - 10;
            D.cover(g, tr.hue(), w[0] + 5, w[1] + 5, cs, 4);
            int tx = w[0] + cs + 10, tw = w[2] - tx - 34;
            g.drawString(font, D.cut(font, tr.title, tw), tx, w[1] + h / 2 - 9, 0xFFFFFFFF, false);
            g.push();
            g.translate(tx, w[1] + h / 2 + 2, 0);
            g.scale(0.7F, 0.7F, 1);
            g.drawString(font, D.cut(font, tr.artist.isEmpty() ? "♪" : tr.artist, (int) (tw / 0.7F)), 0, 0, 0xC0FFFFFF, false);
            g.pop();
            boolean hp = interactive && in(mx, my, w[2] - 32, w[1], w[2] - 17, w[3]), hn = interactive && in(mx, my, w[2] - 17, w[1], w[2], w[3]);
            D.icon(g, com.celular.music.MusicPlayer.playing ? D.PAUSE : D.PLAY, w[2] - 30, w[1] + h / 2 - 6, 12, hp ? 0xFFFF2D55 : 0xFFFFFFFF);
            D.icon(g, D.SKIP, w[2] - 15, w[1] + h / 2 - 6, 12, hn ? 0xFFFF2D55 : 0xFFFFFFFF);
            return;
        }
        WeatherApp.Info info = WeatherApp.info();
        if (info == null) {
            return;
        }
        int is = Math.min(22, h - 10);
        D.icon(g, info.icon, w[0] + 7, w[1] + (h - is) / 2, is, info.iconColor);
        int tx = w[0] + is + 14;
        g.drawString(font, info.time, tx, w[1] + h / 2 - 9, 0xFFFFFFFF, false);
        g.push();
        g.translate(tx, w[1] + h / 2 + 2, 0);
        g.scale(0.7F, 0.7F, 1);
        g.drawString(font, D.cut(font, info.line.getString(), (int) ((w[2] - tx - 6) / 0.7F)), 0, 0, 0xC0FFFFFF, false);
        g.pop();
    }

    /** Pantalla de bloqueo: reloj grande, fecha, avisos y la canción; se quita con un clic o deslizando hacia arriba. */
    private void drawLock(G g, int mx, int my, long now) {
        float k = unlockAt == 0 ? 0 : D.easeOut((now - unlockAt) / 260F);
        if (k >= 1) {
            locked = false;
            return;
        }
        int lift = (int) (k * (dy1 - dy0));
        g.scissor(dx0, dy0, dx1, dy1 - lift);
        g.push();
        g.translate(0, -lift, 0);
        drawWallpaper(g);
        g.fillGradient(dx0, dy0, dx1, dy1, 0x30000000, 0x90000000);
        Font font = Minecraft.getInstance().font;
        int cx = (dx0 + dx1) / 2;
        D.icon(g, D.LOCK, cx - 5, cy0 + 2, 10, 0xFFFFFFFF);
        LocalDateTime t = LocalDateTime.now();
        Component date = Component.translatable("celular.date", Component.translatable("celular.day." + t.getDayOfWeek().getValue()),
                t.getDayOfMonth(), Component.translatable("celular.month." + t.getMonthValue()));
        g.drawCenteredString(font, date, cx, cy0 + 18, 0xE0FFFFFF);
        String time = String.format(java.util.Locale.ROOT, "%d:%02d", t.getHour(), t.getMinute());
        g.push();
        g.translate(cx, cy0 + 30, 0);
        g.scale(4.0F, 4.0F, 1);
        g.drawString(font, time, -font.width(time) / 2, 0, 0xFFFFFFFF, true);
        g.pop();
        // avisos pendientes
        int y = cy0 + 74;
        if (ClientHooks.badgeCalls > 0) {
            y = lockCard(g, font, y, D.APP_PHONE, Component.translatable("celular.lock.missed", ClientHooks.badgeCalls));
        }
        if (ClientHooks.badgeInsta > 0) {
            y = lockCard(g, font, y, D.APP_INSTA, Component.translatable("celular.lock.likes", ClientHooks.badgeInsta));
        }
        com.celular.music.Track tr = com.celular.music.MusicPlayer.current;
        if (tr != null) {
            int x0 = dx0 + 7, x1 = dx1 - 7, y1 = y + 30;
            D.rr(g, x0, y, x1, y1, 8, 0x70000000);
            D.cover(g, tr.hue(), x0 + 4, y + 4, 22, 4);
            g.drawString(font, D.cut(font, tr.title, x1 - x0 - 64), x0 + 30, y + 6, 0xFFFFFFFF, false);
            g.push();
            g.translate(x0 + 30, y + 17, 0);
            g.scale(0.7F, 0.7F, 1);
            g.drawString(font, D.cut(font, tr.artist, (int) ((x1 - x0 - 64) / 0.7F)), 0, 0, 0xC0FFFFFF, false);
            g.pop();
            boolean hp = in(mx, my, x1 - 32, y, x1 - 17, y1), hn = in(mx, my, x1 - 17, y, x1, y1);
            D.icon(g, com.celular.music.MusicPlayer.playing ? D.PAUSE : D.PLAY, x1 - 30, y + 9, 12, hp ? 0xFFFF2D55 : 0xFFFFFFFF);
            D.icon(g, D.SKIP, x1 - 15, y + 9, 12, hn ? 0xFFFF2D55 : 0xFFFFFFFF);
        }
        // "desliza para abrir" que sube y baja
        float bob = (float) Math.sin(now / 300.0) * 2;
        Component hint = Component.translatable("celular.lock.hint");
        g.push();
        g.translate(cx, cy1 - 18 + bob, 0);
        g.scale(0.75F, 0.75F, 1);
        g.drawString(font, hint, -font.width(hint) / 2, 0, 0xD0FFFFFF, true);
        g.pop();
        g.pop();
        g.noScissor();
    }

    private int lockCard(G g, Font font, int y, int app, Component text) {
        int x0 = dx0 + 7, x1 = dx1 - 7;
        D.rr(g, x0, y, x1, y + 20, 8, 0x70000000);
        D.app(g, app, x0 + 4, y + 3, 14);
        g.push();
        g.translate(x0 + 22, y + 7, 0);
        g.scale(0.75F, 0.75F, 1);
        g.drawString(font, D.cut(font, text.getString(), (int) ((x1 - x0 - 26) / 0.75F)), 0, 0, 0xFFFFFFFF, false);
        g.pop();
        return y + 24;
    }

    private void unlock() {
        if (locked && unlockAt == 0) {
            unlockAt = System.currentTimeMillis();
            ClientHooks.play(CelularReg.UNLOCK.get(), 1.0F, 0.4F);
        }
    }

    /** ¿El clic en la pantalla de bloqueo fue en los botones de la música? (no desbloquea) */
    private boolean lockMusicClick(double x, double y) {
        if (com.celular.music.MusicPlayer.current == null) {
            return false;
        }
        int yy = cy0 + 74 + (ClientHooks.badgeCalls > 0 ? 24 : 0) + (ClientHooks.badgeInsta > 0 ? 24 : 0);
        int x1 = dx1 - 7;
        if (in(x, y, x1 - 32, yy, x1 - 17, yy + 30)) {
            com.celular.music.MusicPlayer.toggle();
            ClientHooks.tap();
            return true;
        }
        if (in(x, y, x1 - 17, yy, x1, yy + 30)) {
            com.celular.music.MusicPlayer.next();
            ClientHooks.tap();
            return true;
        }
        return false;
    }

    // ------------------------------------------------------------------ dibujo

    @Override
    protected void draw(G g, int mx, int my, float pt) {
        long now = System.currentTimeMillis();
        Font font = Minecraft.getInstance().font;
        float open = D.easeOut((now - openedAt) / 320F);
        int slide = Math.round((1 - open) * (height - py + 10));
        g.fillGradient(0, 0, width, height, D.alpha(0x70000000, open), D.alpha(0xA0000000, open));
        g.push();
        g.translate(0, slide, 0);
        boolean interactive = slide == 0 && animApp == null;
        int mxx = interactive ? mx : -999, myy = interactive ? my : -999;
        drawBody(g);
        // pantalla
        g.fill(dx0, dy0, dx1, dy1, 0xFF000000);
        Music.frame();
        if (app != null && animApp == null) {
            app.draw(g, mxx, myy, pt);
        } else {
            drawHome(g, locked ? -999 : mxx, locked ? -999 : myy, interactive && !locked);
            if (animApp != null) {
                drawAppAnim(g, now, mxx, myy, pt);
            }
        }
        if (locked) {
            drawLock(g, mxx, myy, now);
        }
        drawStatusBar(g, font, now);
        // barrita de inicio
        int hb = app != null ? (app.lightStatus() ? 0xFF111111 : 0xFFFFFFFF) : 0xFFFFFFFF;
        boolean hovBar = interactive && app != null && in(mx, my, (dx0 + dx1) / 2 - 24, dy1 - 9, (dx0 + dx1) / 2 + 24, dy1);
        D.rr(g, (dx0 + dx1) / 2 - (hovBar ? 22 : 18), dy1 - 6, (dx0 + dx1) / 2 + (hovBar ? 22 : 18), dy1 - 3, 2, D.alpha(hb, 0.85F));
        drawCorners(g);
        g.pop();
        if (app != null && animApp == null && slide == 0) {
            drawWidgets(g, mx, my, pt);
        }
    }

    private void drawAppAnim(G g, long now, int mx, int my, float pt) {
        float t = (now - animAt) / (animOpening ? 260F : 200F);
        if (t >= 1) {
            App done = animApp;
            animApp = null;
            if (animOpening && done instanceof CameraApp) {
                ClientHooks.open(new CameraScreen(color));
            }
            return;
        }
        float k = animOpening ? D.easeOut(t) : 1 - D.easeOut(t);
        int x0 = (int) (animIx + (dx0 - animIx) * k), y0 = (int) (animIy + (dy0 - animIy) * k);
        int x1 = (int) (animIx + animIs + (dx1 - animIx - animIs) * k), y1 = (int) (animIy + animIs + (dy1 - animIy - animIs) * k);
        D.rr(g, x0, y0, x1, y1, (int) (animIs / 4 + (8 - animIs / 4) * k), animApp.bg());
        int is = (int) (animIs + 10 * k);
        D.app(g, animApp.icon, (x0 + x1) / 2 - is / 2, (y0 + y1) / 2 - is / 2, is);
    }

    private void drawBody(G g) {
        int[] c = CASE[color];
        // sombra
        D.rr(g, px + 3, py + 4, px + pw + 3, py + ph + 4, 16, 0x50000000);
        // carcasa con brillo en el borde
        D.rr(g, px - 1, py - 1, px + pw + 1, py + ph + 1, 16, c[1]);
        D.rr(g, px, py, px + pw, py + ph, 15, c[0]);
        // botones de los lados
        g.fill(px - 2, py + 46, px, py + 60, c[1]);
        g.fill(px - 2, py + 66, px, py + 86, c[1]);
        g.fill(px + pw, py + 58, px + pw + 2, py + 82, c[1]);
        // marco negro de la pantalla
        D.rr(g, px + 3, py + 3, px + pw - 3, py + ph - 3, 13, 0xFF050505);
    }

    /** Esquinas redondeadas de la pantalla (se pintan del color del marco encima de todo). */
    private void drawCorners(G g) {
        int r = 10;
        for (int i = 0; i < r; i++) {
            double dy = r - i - 0.5;
            int in = (int) Math.round(r - Math.sqrt(r * r - dy * dy));
            if (in <= 0) {
                continue;
            }
            g.fill(dx0, dy0 + i, dx0 + in, dy0 + i + 1, 0xFF050505);
            g.fill(dx1 - in, dy0 + i, dx1, dy0 + i + 1, 0xFF050505);
            g.fill(dx0, dy1 - i - 1, dx0 + in, dy1 - i, 0xFF050505);
            g.fill(dx1 - in, dy1 - i - 1, dx1, dy1 - i, 0xFF050505);
        }
    }

    private void drawStatusBar(G g, Font font, long now) {
        boolean dark = app != null && animApp == null && app.lightStatus();
        int col = dark ? 0xFF111111 : 0xFFFFFFFF;
        LocalDateTime t = LocalDateTime.now();
        String time = String.format(java.util.Locale.ROOT, "%d:%02d", t.getHour(), t.getMinute());
        g.push();
        g.translate(dx0 + 10, dy0 + 4, 0);
        g.scale(0.75F, 0.75F, 1);
        g.drawString(font, time, 0, 0, col, false);
        g.pop();
        D.icon(g, D.SIGNAL, dx1 - 33, dy0 + 3, 8, col);
        D.icon(g, D.WIFI, dx1 - 24, dy0 + 3, 8, col);
        D.icon(g, D.BATTERY, dx1 - 15, dy0 + 2, 10, col);
        // la "isla" del centro: crece y muestra la llamada en curso
        CallState st = ClientHooks.call;
        int cx = (dx0 + dx1) / 2;
        if (st.state == CallState.ACTIVE || st.state == CallState.DIALING) {
            String s = st.state == CallState.ACTIVE ? D.clock(ClientHooks.callSeconds()) : "…";
            int w = 30 + (int) (font.width(s) * 0.75F);
            D.rr(g, cx - w / 2, dy0 + 2, cx + w / 2, dy0 + 11, 4, 0xFF000000);
            D.icon(g, D.CALL, cx - w / 2 + 3, dy0 + 3, 7, 0xFF30D158);
            g.push();
            g.translate(cx - w / 2 + 13, dy0 + 4, 0);
            g.scale(0.75F, 0.75F, 1);
            g.drawString(font, s, 0, 0, 0xFF30D158, false);
            g.pop();
        } else if (com.celular.music.MusicPlayer.current != null && com.celular.music.MusicPlayer.playing) {
            // la isla muestra la canción: portada a la izquierda y barritas a la derecha
            com.celular.music.Track tr = com.celular.music.MusicPlayer.current;
            D.rr(g, cx - 22, dy0 + 2, cx + 22, dy0 + 11, 4, 0xFF000000);
            D.rr(g, cx - 20, dy0 + 3, cx - 13, dy0 + 10, 2, D.hsv(tr.hue(), 0.65F, 0.95F));
            float[] lv = com.celular.music.MusicPlayer.levels();
            for (int i = 0; i < 4; i++) {
                float v = lv == null ? 0.2F : Math.max(0.2F, lv[2 + i * 3]);
                int bh = 1 + (int) (v * 6);
                g.fill(cx + 10 + i * 3, dy0 + 10 - bh, cx + 12 + i * 3, dy0 + 10, 0xFFFF2D55);
            }
        } else {
            D.rr(g, cx - 13, dy0 + 2, cx + 13, dy0 + 10, 4, 0xFF000000);
        }
    }

    // ------------------------------------------------------------------ entrada

    private boolean busy() {
        return animApp != null || System.currentTimeMillis() - openedAt < 320;
    }

    @Override
    protected boolean onClick(double x, double y, int b) {
        if (busy()) {
            return true;
        }
        if (box != null && box.isMouseOver(x, y)) {
            return superClick(x, y, b);
        }
        if (box != null) {
            box.setFocused(false);
        }
        if (locked) {
            if (in(x, y, px, py, px + pw, py + ph)) {
                if (!lockMusicClick(x, y)) {
                    unlock();
                }
            } else {
                onClose();
            }
            return true;
        }
        // barrita de inicio
        if (app != null && in(x, y, (dx0 + dx1) / 2 - 24, dy1 - 9, (dx0 + dx1) / 2 + 24, dy1)) {
            goHome();
            return true;
        }
        if (app != null) {
            if (in(x, y, dx0, dy0, dx1, dy1)) {
                app.click(x, y, b);
            }
            return true;
        }
        for (int i = 0; i < apps.length; i++) {
            int[] r = iconRect(i);
            if (in(x, y, r[0], r[1], r[0] + r[2], r[1] + r[2])) {
                openApp(apps[i], r[0], r[1], r[2]);
                return true;
            }
        }
        int[] w = widgetRect();
        if (w[3] - w[1] >= 26 && in(x, y, w[0], w[1], w[2], w[3])) {
            if (com.celular.music.MusicPlayer.current != null) {
                if (in(x, y, w[2] - 32, w[1], w[2] - 17, w[3])) {
                    Radio.toggle();
                    ClientHooks.tap();
                } else if (in(x, y, w[2] - 17, w[1], w[2], w[3])) {
                    Radio.next();
                    ClientHooks.tap();
                } else {
                    openApp("music");
                }
            } else {
                openApp("weather");
            }
            return true;
        }
        if (!in(x, y, px, py, px + pw, py + ph)) {
            onClose();
        }
        return true;
    }

    @Override
    protected boolean onRelease(double x, double y, int b) {
        if (app != null && !busy()) {
            app.release(x, y, b);
        }
        return superRelease(x, y, b);
    }

    @Override
    protected boolean onDrag(double x, double y, int b, double dx, double dy) {
        if (locked) {
            if (dy < -2) {
                unlock();
            }
            return true;
        }
        if (app != null && !busy()) {
            return app.drag(x, y, dx, dy);
        }
        return true;
    }

    @Override
    protected boolean onScroll(double x, double y, double d) {
        return app != null && !busy() && app.scroll(x, y, d);
    }

    @Override
    protected boolean onKey(int key, int scan, int mods) {
        if (locked && key != 256) {
            unlock();
            return true;
        }
        if (key == 256) { // escape
            if (box != null && box.isFocused()) {
                box.setFocused(false);
                return true;
            }
            if (app != null) {
                if (!app.back()) {
                    goHome();
                }
                return true;
            }
            onClose();
            return true;
        }
        if (box != null && box.isFocused()) {
            if (app != null && app.key(key)) {
                return true;
            }
            return superKey(key, scan, mods);
        }
        if (app != null && app.key(key)) {
            return true;
        }
        if (key == 259 && app != null) { // borrar = atrás
            if (!app.back()) {
                goHome();
            }
            return true;
        }
        //#if MC < 26.1
        // la tecla del celular lo guarda
        if (PhoneHud.OPEN.matches(key, scan)) {
            onClose();
            return true;
        }
        //#endif
        return superKey(key, scan, mods);
    }
}
