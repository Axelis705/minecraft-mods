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

    public PhoneScreen(int color, String startApp) {
        super(Component.translatable("item.celular.phone"));
        this.color = Math.max(0, Math.min(CASE.length - 1, color));
        this.startApp = startApp;
        Prefs.load();
        apps = new App[]{new InstaApp(this), new CameraApp(this), new GalleryApp(this), new PhoneApp(this), new PatreonApp(this),
                new SettingsApp(this)};
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
            ClientHooks.play(CelularReg.UNLOCK.get(), 1.0F, 0.4F);
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

    private int cols() {
        return 3;
    }

    private int iconSize() {
        return Math.max(22, Math.min(30, dw() / 4 - 4));
    }

    /** x, y, tamaño del icono de la app i en el inicio. */
    int[] iconRect(int i) {
        int is = iconSize(), cols = cols();
        int cell = dw() / cols;
        int top = gridTop();
        int col = i % cols, row = i / cols;
        int x = dx0 + col * cell + cell / 2 - is / 2;
        int y = top + row * (is + 18);
        return new int[]{x, y, is};
    }

    private int gridTop() {
        return cy0 + Math.max(58, (cy1 - cy0) / 3);
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
        // apps
        int[] badges = {ClientHooks.badgeInsta, 0, 0, ClientHooks.badgeCalls, 0, 0};
        for (int i = 0; i < apps.length; i++) {
            int[] r = iconRect(i);
            boolean hov = interactive && in(mx, my, r[0], r[1], r[0] + r[2], r[1] + r[2]);
            int lift = hov ? 1 : 0;
            if (animApp == apps[i] && animOpening) {
                continue; // está saliendo de aquí
            }
            D.rr(g, r[0] + 1, r[1] + 2, r[0] + r[2] + 1, r[1] + r[2] + 2, r[2] / 4, 0x50000000);
            D.app(g, apps[i].icon, r[0], r[1] - lift, r[2]);
            if (badges[i] > 0) {
                String b = badges[i] > 9 ? "9+" : Integer.toString(badges[i]);
                int bw = Math.max(9, font.width(b) + 4);
                D.rr(g, r[0] + r[2] - bw + 3, r[1] - 3, r[0] + r[2] + 3, r[1] + 6, 4, 0xFFFF3B30);
                g.drawString(font, b, r[0] + r[2] + 3 - bw / 2 - font.width(b) / 2, r[1] - 2, 0xFFFFFFFF, false);
            }
            Component label = Component.translatable("celular.app." + apps[i].key);
            g.push();
            g.translate(r[0] + r[2] / 2F, r[1] + r[2] + 3, 0);
            g.scale(0.75F, 0.75F, 1);
            String ls = D.cut(font, label.getString(), (int) ((dw() / cols() - 2) / 0.75F));
            g.drawString(font, ls, -font.width(ls) / 2, 0, 0xFFFFFFFF, true);
            g.pop();
        }
        // acceso rápido abajo: nombre de quien te apoya en Patreon
        Component hint = Component.translatable("celular.home.hint");
        g.push();
        g.translate(cx, cy1 - 12, 0);
        g.scale(0.75F, 0.75F, 1);
        g.drawString(font, hint, -font.width(hint) / 2, 0, 0xC0FFFFFF, true);
        g.pop();
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
        if (app != null && animApp == null) {
            app.draw(g, mxx, myy, pt);
        } else {
            drawHome(g, mxx, myy, interactive);
            if (animApp != null) {
                drawAppAnim(g, now, mxx, myy, pt);
            }
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
