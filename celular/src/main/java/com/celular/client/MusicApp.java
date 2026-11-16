package com.celular.client;

import com.celular.music.MusicPlayer;
import com.celular.music.Spectrum;
import com.celular.music.Track;
import com.celular.plat.client.G;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Música: tus canciones de la carpeta {@code .minecraft/celular/musica} (MP3, OGG, WAV). Lista con portadas de colores, reproductor
 * con disco que gira, barra para adelantar, aleatorio, repetir, volumen y ecualizador que se mueve con la canción. Sigue sonando con
 * el celular guardado.
 */
final class MusicApp extends App {
    private boolean player;
    private final Scroll scroll = new Scroll();
    /** Barra de avance arrastrándose: segundo elegido (-1 = no). */
    private double dragSeek = -1;
    private boolean dragVol;
    private final float[] bars = new float[Spectrum.BANDS];
    private long lastBars;
    private float spin;
    private long lastSpin;

    MusicApp(PhoneScreen s) {
        super(s, D.APP_MUSIC, "music");
    }

    @Override
    void enter() {
        Music.init();
        MusicPlayer.scan();
        player = MusicPlayer.current != null && MusicPlayer.playing;
        scroll.reset();
    }

    @Override
    int bg() {
        return player ? cur() : s.bg();
    }

    @Override
    boolean lightStatus() {
        return !player && !Prefs.dark;
    }

    private int cur() {
        Track t = MusicPlayer.current;
        return t == null ? 0xFF1C1C1E : D.hsv(t.hue(), 0.55F, 0.30F);
    }

    // ------------------------------------------------------------------ dibujo

    @Override
    void draw(G g, int mx, int my, float pt) {
        updateBars();
        if (player && MusicPlayer.current != null) {
            drawPlayer(g, mx, my);
        } else {
            player = false;
            drawList(g, mx, my);
        }
    }

    private void updateBars() {
        long now = System.currentTimeMillis();
        float dt = Math.min(0.1F, (now - lastBars) / 1000F);
        lastBars = now;
        float[] lv = MusicPlayer.levels();
        for (int i = 0; i < bars.length; i++) {
            float target = lv == null ? 0 : lv[i];
            bars[i] = target > bars[i] ? bars[i] + (target - bars[i]) * 0.6F : Math.max(target, bars[i] - dt * 1.6F);
        }
        if (MusicPlayer.playing) {
            spin += (now - lastSpin) / 1000F * 0.9F;
        }
        lastSpin = now;
    }

    private void drawList(G g, int mx, int my) {
        Font f = font();
        g.fill(s.dx0, s.dy0, s.dx1, s.dy1, s.bg());
        int top = header(g, "celular.app.music", false, mx, my);
        // botones arriba a la derecha: actualizar y abrir carpeta
        int by = s.cy0 + 3;
        boolean hf = s.in(mx, my, s.dx1 - 17, by, s.dx1 - 3, by + 13), hr = s.in(mx, my, s.dx1 - 33, by, s.dx1 - 19, by + 13);
        D.icon(g, D.FOLDER, s.dx1 - 16, by + 1, 12, hf ? s.accent() : s.fg());
        D.icon(g, D.REFRESH, s.dx1 - 32, by + 1, 12, hr ? s.accent() : s.fg());
        List<Track> lib = MusicPlayer.library;
        int bottom = s.cy1 - (MusicPlayer.current != null ? 28 : 0);
        if (lib.isEmpty()) {
            int cy = top + 26;
            D.circle(g, (s.dx0 + s.dx1) / 2, cy + 14, 16, 0xFFFA233B);
            D.icon(g, D.NOTE, (s.dx0 + s.dx1) / 2 - 9, cy + 5, 18, 0xFFFFFFFF);
            int y = cy + 38;
            centered(g, Component.translatable("celular.music.empty").withStyle(ChatFormatting.BOLD), y, s.fg());
            y += 22;
            Component how = Component.translatable("celular.music.empty_sub");
            for (net.minecraft.util.FormattedCharSequence l : f.split(how, s.dw() - 16)) {
                g.drawString(f, l, (s.dx0 + s.dx1) / 2 - f.width(l) / 2, y, s.sub(), false);
                y += 10;
            }
            y += 6;
            button(g, s.dx0 + 14, y, s.dx1 - 14, y + 16, Component.translatable("celular.music.open_folder"), 0xFFFA233B, mx, my);
            y += 22;
            button(g, s.dx0 + 14, y, s.dx1 - 14, y + 16, Component.translatable("celular.music.refresh"), Prefs.dark ? 0xFF3A3A3C : 0xFF8E8E93, mx, my);
            y += 24;
            small(g, D.cut(f, MusicPlayer.folder().toString(), (int) ((s.dw() - 12) / 0.6F)), s.dx0 + 6, y, s.sub(), 0.6F);
        } else {
            // "aleatorio" y cuántas hay
            int y = top + 4;
            boolean hs = s.in(mx, my, s.dx0 + 6, y, s.dx0 + 66, y + 14);
            D.rr(g, s.dx0 + 6, y, s.dx0 + 66, y + 14, 7, hs ? 0xFFFF4D63 : 0xFFFA233B);
            D.icon(g, D.SHUFFLE, s.dx0 + 9, y + 2, 10, 0xFFFFFFFF);
            small(g, Component.translatable("celular.music.shuffle_all").getString(), s.dx0 + 21, y + 4, 0xFFFFFFFF, 0.75F);
            Component cnt = Component.translatable("celular.music.count", lib.size());
            small(g, cnt.getString(), s.dx1 - 6 - (int) (f.width(cnt) * 0.75F), y + 4, s.sub(), 0.75F);
            int listTop = y + 18;
            int rowH = 22;
            scroll.update(lib.size() * rowH, bottom - listTop);
            g.scissor(s.dx0, listTop, s.dx1, bottom);
            for (int i = 0; i < lib.size(); i++) {
                int ry = listTop + i * rowH - scroll.off();
                if (ry + rowH < listTop || ry > bottom) {
                    continue;
                }
                Track t = lib.get(i);
                boolean isCur = t == MusicPlayer.current;
                if (s.in(mx, my, s.dx0, Math.max(ry, listTop), s.dx1, Math.min(ry + rowH, bottom))) {
                    g.fill(s.dx0, ry, s.dx1, ry + rowH, Prefs.dark ? 0xFF1C1C1E : 0xFFF2F2F7);
                }
                D.cover(g, t.hue(), s.dx0 + 6, ry + 3, 16, 3);
                int tw = s.dw() - 36 - (isCur ? 10 : 0);
                g.drawString(f, D.cut(f, t.title, tw), s.dx0 + 26, ry + 3, t.broken ? s.sub() : isCur ? 0xFFFA233B : s.fg(), false);
                String sub = t.broken ? Component.translatable("celular.music.broken").getString()
                        : (t.artist.isEmpty() ? Component.translatable("celular.music.unknown_artist").getString() : t.artist)
                        + (t.seconds > 0 ? " · " + D.clock(t.seconds) : "");
                small(g, D.cut(f, sub, (int) (tw / 0.7F)), s.dx0 + 26, ry + 13, s.sub(), 0.7F);
                if (isCur) {
                    miniBars(g, s.dx1 - 12, ry + 15, 0xFFFA233B);
                }
                if (i < lib.size() - 1) {
                    g.fill(s.dx0 + 26, ry + rowH - 1, s.dx1, ry + rowH, s.line());
                }
            }
            g.noScissor();
        }
        if (MusicPlayer.current != null) {
            drawMini(g, mx, my, bottom);
        }
    }

    /** Barrita de "ahora suena" abajo de la lista. */
    private void drawMini(G g, int mx, int my, int y) {
        Font f = font();
        Track t = MusicPlayer.current;
        int x0 = s.dx0 + 4, x1 = s.dx1 - 4, y1 = s.cy1 - 2;
        D.rr(g, x0, y + 1, x1, y1, 6, Prefs.dark ? 0xFF2C2C2E : 0xFFE5E5EA);
        // avance
        int len = MusicPlayer.length();
        if (len > 0) {
            int w = (int) ((x1 - x0 - 12) * Math.min(1, MusicPlayer.position() / len));
            g.fill(x0 + 6, y1 - 2, x0 + 6 + w, y1 - 1, 0xFFFA233B);
        }
        D.cover(g, t.hue(), x0 + 4, y + 5, 16, 3);
        g.drawString(f, D.cut(f, t.title, x1 - x0 - 58), x0 + 24, y + 5, s.fg(), false);
        small(g, D.cut(f, t.artist, (int) ((x1 - x0 - 58) / 0.7F)), x0 + 24, y + 15, s.sub(), 0.7F);
        boolean hp = s.in(mx, my, x1 - 34, y + 4, x1 - 18, y + 22), hn = s.in(mx, my, x1 - 17, y + 4, x1 - 2, y + 22);
        D.icon(g, MusicPlayer.playing ? D.PAUSE : D.PLAY, x1 - 32, y + 7, 12, hp ? 0xFFFA233B : s.fg());
        D.icon(g, D.SKIP, x1 - 16, y + 7, 12, hn ? 0xFFFA233B : s.fg());
    }

    private void miniBars(G g, int x, int yBase, int color) {
        for (int i = 0; i < 3; i++) {
            float v = MusicPlayer.playing ? Math.max(0.15F, bars[2 + i * 4]) : 0.15F;
            int h = 2 + (int) (v * 7);
            g.fill(x + i * 3, yBase - h, x + i * 3 + 2, yBase, color);
        }
    }

    private void small(G g, String text, int x, int y, int color, float sc) {
        g.push();
        g.translate(x, y, 0);
        g.scale(sc, sc, 1);
        g.drawString(font(), text, 0, 0, color, false);
        g.pop();
    }

    private void drawPlayer(G g, int mx, int my) {
        Font f = font();
        Track t = MusicPlayer.current;
        int hue = t.hue();
        int top = D.hsv(hue, 0.55F, 0.42F), bot = D.hsv(hue + 30, 0.6F, 0.12F);
        g.fillGradient(s.dx0, s.dy0, s.dx1, s.dy1, top, bot);
        // brillo que respira con la música
        float bass = (bars[1] + bars[2] + bars[3]) / 3F;
        int cx = (s.dx0 + s.dx1) / 2;
        // flecha para bajar
        int hy = s.cy0 + 2;
        boolean hd = s.in(mx, my, s.dx0 + 2, hy, s.dx0 + 18, hy + 14);
        D.icon(g, D.DOWN, s.dx0 + 4, hy + 1, 12, hd ? 0xFFFFFFFF : 0xC0FFFFFF);
        small(g, Component.translatable("celular.music.now_playing").getString().toUpperCase(java.util.Locale.ROOT),
                cx - (int) (f.width(Component.translatable("celular.music.now_playing").getString().toUpperCase(java.util.Locale.ROOT)) * 0.65F / 2),
                hy + 5, 0xB0FFFFFF, 0.65F);
        // disco que gira detrás de la portada
        int cs = Math.min(s.dw() - 40, (s.cy1 - s.cy0) / 2 - 22);
        int coverY = s.cy0 + 20;
        int discR = cs / 2;
        int discX = cx + (MusicPlayer.playing ? cs / 5 : cs / 10);
        int discY = coverY + cs / 2;
        g.scissor(s.dx0, s.dy0, s.dx1, s.dy1);         // el disco no se sale de la pantalla del celular
        D.circle(g, discX, discY, discR, 0xFF111111);
        for (int r = discR - 2; r > discR / 3; r -= 3) {
            D.rrLine(g, discX - r, discY - r, discX + r, discY + r, r, 0xFF262626);
        }
        for (int k = 0; k < 2; k++) {
            double a = spin * 2 * Math.PI + k * Math.PI;
            for (int r = discR / 3 + 2; r < discR - 2; r += 2) {
                int px = discX + (int) Math.round(Math.cos(a) * r), py = discY + (int) Math.round(Math.sin(a) * r);
                g.fill(px, py, px + 1, py + 1, 0x50FFFFFF);
            }
        }
        D.circle(g, discX, discY, discR / 3, D.hsv(hue, 0.7F, 0.9F));
        D.circle(g, discX, discY, 2, 0xFF111111);
        g.noScissor();
        // portada (crece un poquito con los graves)
        int grow = MusicPlayer.playing ? (int) (bass * 3) : 0;
        int cvx = cx - cs / 2 - cs / 6 - grow, cvy = coverY - grow, csz = cs + grow * 2;
        D.rr(g, cvx + 2, cvy + 3, cvx + csz + 2, cvy + csz + 3, 6, 0x60000000);
        D.cover(g, hue, cvx, cvy, csz, 6);
        int y = coverY + cs + 8;
        // título y artista (el título se desliza si es largo)
        int maxW = s.dw() - 16;
        String title = t.title;
        int tw = f.width(title);
        g.scissor(s.dx0 + 8, y, s.dx1 - 8, y + 10);
        if (tw > maxW) {
            long ms = System.currentTimeMillis() % (long) ((tw + 30) * 40L + 2000);
            int off = (int) Math.max(0, (ms - 1000) / 40);
            g.drawString(f, Component.literal(title).withStyle(ChatFormatting.BOLD), s.dx0 + 8 - off, y, 0xFFFFFFFF, false);
            g.drawString(f, Component.literal(title).withStyle(ChatFormatting.BOLD), s.dx0 + 8 - off + tw + 30, y, 0xFFFFFFFF, false);
        } else {
            g.drawString(f, Component.literal(title).withStyle(ChatFormatting.BOLD), cx - tw / 2, y, 0xFFFFFFFF, false);
        }
        g.noScissor();
        y += 11;
        String artist = t.artist.isEmpty() ? Component.translatable("celular.music.unknown_artist").getString() : t.artist;
        artist = D.cut(f, artist, maxW);
        g.drawString(f, artist, cx - f.width(artist) / 2, y, 0xB0FFFFFF, false);
        y += 14;
        // barra de avance
        int len = MusicPlayer.length();
        double pos = dragSeek >= 0 ? dragSeek : MusicPlayer.position();
        int bx0 = s.dx0 + 10, bx1 = s.dx1 - 10;
        float k = len > 0 ? (float) Math.min(1, pos / len) : 0;
        D.rr(g, bx0, y, bx1, y + 3, 1, 0x50FFFFFF);
        D.rr(g, bx0, y, bx0 + Math.max(2, (int) ((bx1 - bx0) * k)), y + 3, 1, 0xFFFFFFFF);
        boolean hb = s.in(mx, my, bx0 - 2, y - 4, bx1 + 2, y + 7) || dragSeek >= 0;
        D.circle(g, bx0 + (int) ((bx1 - bx0) * k), y + 1, hb ? 4 : 3, 0xFFFFFFFF);
        y += 6;
        small(g, D.clock((int) pos), bx0, y, 0xB0FFFFFF, 0.65F);
        String rest = len > 0 ? "-" + D.clock(Math.max(0, len - (int) pos)) : "--:--";
        small(g, rest, bx1 - (int) (f.width(rest) * 0.65F), y, 0xB0FFFFFF, 0.65F);
        y += 10;
        // controles
        int cy = y + 10;
        int[] xs = {cx - 46, cx - 24, cx, cx + 24, cx + 46};
        int[] icons = {D.SHUFFLE, D.PREV, MusicPlayer.playing ? D.PAUSE : D.PLAY, D.SKIP, MusicPlayer.repeat == 2 ? D.REPEAT1 : D.REPEAT};
        for (int i = 0; i < 5; i++) {
            boolean h = s.in(mx, my, xs[i] - 10, cy - 10, xs[i] + 10, cy + 10);
            if (i == 2) {
                D.circle(g, xs[i], cy, h ? 12 : 11, 0xFFFFFFFF);
                D.icon(g, icons[i], xs[i] - 7, cy - 7, 14, top);
            } else {
                boolean on = (i == 0 && MusicPlayer.shuffle) || (i == 4 && MusicPlayer.repeat > 0);
                int size = i == 1 || i == 3 ? 14 : 11;
                int col = on ? 0xFFFFFFFF : (i == 0 || i == 4 ? 0x80FFFFFF : 0xE6FFFFFF);
                if (h) {
                    col = 0xFFFFFFFF;
                }
                D.icon(g, icons[i], xs[i] - size / 2, cy - size / 2, size, col);
                if (on) {
                    g.fill(xs[i] - 1, cy + 8, xs[i] + 1, cy + 10, 0xFFFFFFFF);
                }
            }
        }
        y = cy + 18;
        // volumen
        int vx0 = s.dx0 + 22, vx1 = s.dx1 - 22;
        D.icon(g, D.VOL_LOW, s.dx0 + 8, y - 3, 9, 0xB0FFFFFF);
        D.icon(g, D.VOL_HIGH, s.dx1 - 18, y - 3, 10, 0xB0FFFFFF);
        D.rr(g, vx0, y, vx1, y + 3, 1, 0x50FFFFFF);
        int vk = vx0 + (int) ((vx1 - vx0) * MusicPlayer.volume);
        D.rr(g, vx0, y, Math.max(vx0 + 2, vk), y + 3, 1, 0xFFFFFFFF);
        D.circle(g, vk, y + 1, dragVol ? 4 : 3, 0xFFFFFFFF);
        y += 10;
        // ecualizador
        int eqBottom = s.cy1 - 2;
        int eqTop = Math.max(y + 4, eqBottom - 44);
        if (eqBottom - eqTop >= 6) {
            int n = Spectrum.BANDS;
            int bw = Math.max(2, (s.dw() - 16) / n - 1);
            int ex = cx - (n * (bw + 1)) / 2;
            for (int i = 0; i < n; i++) {
                int h = Math.max(1, (int) (bars[i] * (eqBottom - eqTop)));
                int c = D.mix(D.hsv(hue + i * 6, 0.5F, 1F), 0xFFFFFFFF, 0.25F);
                g.fillGradient(ex + i * (bw + 1), eqBottom - h, ex + i * (bw + 1) + bw, eqBottom, D.alpha(c, 0.95F), D.alpha(c, 0.35F));
            }
        }
    }

    // ------------------------------------------------------------------ entrada

    private int[] progressBar() {
        int cs = Math.min(s.dw() - 40, (s.cy1 - s.cy0) / 2 - 22);
        int y = s.cy0 + 20 + cs + 8 + 11 + 14;
        return new int[]{s.dx0 + 10, y, s.dx1 - 10};
    }

    private int controlsY() {
        return progressBar()[1] + 6 + 10 + 10;
    }

    private int volumeY() {
        return controlsY() + 18;
    }

    @Override
    boolean click(double x, double y, int b) {
        if (player) {
            return clickPlayer(x, y);
        }
        int by = s.cy0 + 3;
        if (s.in(x, y, s.dx1 - 17, by, s.dx1 - 3, by + 13)) {
            ClientHooks.tap();
            Music.openFolder();
            return true;
        }
        if (s.in(x, y, s.dx1 - 33, by, s.dx1 - 19, by + 13)) {
            ClientHooks.tap();
            MusicPlayer.scan();
            return true;
        }
        List<Track> lib = MusicPlayer.library;
        int top = s.cy0 + 19;
        int bottom = s.cy1 - (MusicPlayer.current != null ? 28 : 0);
        if (MusicPlayer.current != null && s.in(x, y, s.dx0 + 4, bottom, s.dx1 - 4, s.cy1)) {
            int x1 = s.dx1 - 4;
            if (s.in(x, y, x1 - 34, bottom, x1 - 18, s.cy1)) {
                MusicPlayer.toggle();
            } else if (s.in(x, y, x1 - 17, bottom, x1, s.cy1)) {
                MusicPlayer.next();
            } else {
                player = true;
            }
            ClientHooks.tap();
            return true;
        }
        if (lib.isEmpty()) {
            int yy = top + 26 + 38 + 22;
            Font f = font();
            yy += f.split(Component.translatable("celular.music.empty_sub"), s.dw() - 16).size() * 10 + 6;
            if (s.in(x, y, s.dx0 + 14, yy, s.dx1 - 14, yy + 16)) {
                ClientHooks.tap();
                Music.openFolder();
            } else if (s.in(x, y, s.dx0 + 14, yy + 22, s.dx1 - 14, yy + 38)) {
                ClientHooks.tap();
                MusicPlayer.scan();
            }
            return true;
        }
        int ly = top + 4;
        if (s.in(x, y, s.dx0 + 6, ly, s.dx0 + 66, ly + 14)) {
            ClientHooks.tap();
            MusicPlayer.shuffle = true;
            Prefs.shuffle = true;
            Prefs.save();
            MusicPlayer.playList(new ArrayList<>(lib), new java.util.Random().nextInt(lib.size()));
            player = true;
            return true;
        }
        int listTop = ly + 18;
        if (y >= listTop && y < bottom) {
            int i = (int) ((y - listTop + scroll.off()) / 22);
            if (i >= 0 && i < lib.size()) {
                ClientHooks.tap();
                if (lib.get(i) == MusicPlayer.current) {
                    if (!MusicPlayer.playing) {
                        MusicPlayer.toggle();
                    }
                } else {
                    MusicPlayer.playList(new ArrayList<>(lib), i);
                }
                player = true;
            }
        }
        return true;
    }

    private boolean clickPlayer(double x, double y) {
        if (s.in(x, y, s.dx0, s.cy0, s.dx0 + 22, s.cy0 + 18)) {
            player = false;
            ClientHooks.tap();
            return true;
        }
        int[] pb = progressBar();
        if (s.in(x, y, pb[0] - 3, pb[1] - 5, pb[2] + 3, pb[1] + 8)) {
            dragSeek = seekAt(x);
            return true;
        }
        int vy = volumeY();
        if (s.in(x, y, s.dx0 + 18, vy - 5, s.dx1 - 18, vy + 8)) {
            dragVol = true;
            setVolume(x);
            return true;
        }
        int cy = controlsY() + 10;
        int cx = (s.dx0 + s.dx1) / 2;
        int[] xs = {cx - 46, cx - 24, cx, cx + 24, cx + 46};
        for (int i = 0; i < 5; i++) {
            if (s.in(x, y, xs[i] - 11, cy - 11, xs[i] + 11, cy + 11)) {
                ClientHooks.tap();
                switch (i) {
                    case 0 -> {
                        MusicPlayer.setShuffle(!MusicPlayer.shuffle);
                        Prefs.shuffle = MusicPlayer.shuffle;
                        Prefs.save();
                    }
                    case 1 -> MusicPlayer.prev();
                    case 2 -> MusicPlayer.toggle();
                    case 3 -> MusicPlayer.next();
                    default -> {
                        MusicPlayer.repeat = (MusicPlayer.repeat + 1) % 3;
                        Prefs.repeat = MusicPlayer.repeat;
                        Prefs.save();
                    }
                }
                return true;
            }
        }
        return true;
    }

    private double seekAt(double x) {
        int[] pb = progressBar();
        double k = Math.max(0, Math.min(1, (x - pb[0]) / (pb[2] - pb[0])));
        return k * MusicPlayer.length();
    }

    private void setVolume(double x) {
        int vx0 = s.dx0 + 22, vx1 = s.dx1 - 22;
        MusicPlayer.volume = (float) Math.max(0, Math.min(1, (x - vx0) / (vx1 - vx0)));
    }

    @Override
    boolean drag(double x, double y, double dx, double dy) {
        if (dragSeek >= 0) {
            dragSeek = seekAt(x);
            return true;
        }
        if (dragVol) {
            setVolume(x);
            return true;
        }
        if (!player) {
            scroll.drag(dy);
        }
        return true;
    }

    @Override
    boolean release(double x, double y, int b) {
        if (dragSeek >= 0) {
            if (MusicPlayer.length() > 0) {
                MusicPlayer.seek(dragSeek);
            }
            dragSeek = -1;
            ClientHooks.tap();
        }
        if (dragVol) {
            dragVol = false;
            Prefs.musicVolume = MusicPlayer.volume;
            Prefs.save();
        }
        return true;
    }

    @Override
    boolean scroll(double x, double y, double d) {
        if (player) {
            MusicPlayer.volume = (float) Math.max(0, Math.min(1, MusicPlayer.volume + d * 0.05));
            Prefs.musicVolume = MusicPlayer.volume;
            Prefs.save();
        } else {
            scroll.wheel(d);
        }
        return true;
    }

    @Override
    boolean key(int key) {
        if (key == 32) {                     // espacio = pausa
            MusicPlayer.toggle();
            return true;
        }
        if (player && key == 262) {          // flecha derecha = siguiente
            MusicPlayer.next();
            return true;
        }
        if (player && key == 263) {
            MusicPlayer.prev();
            return true;
        }
        return false;
    }

    @Override
    boolean back() {
        if (player) {
            player = false;
            return true;
        }
        return false;
    }
}
