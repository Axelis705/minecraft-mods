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
        inviting = false;
        Radio.requestLibrary();
        if (tab == TAB_JAM) {
            Radio.jamListRequest();
            jamListAt = System.currentTimeMillis();
        }
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

    // ------------------------------------------------------------------ pestañas (Mías, Mundo, Jam)

    private static final int RED = 0xFFFA233B, GREEN = 0xFF1DB954;
    private static final int TAB_MINE = 0, TAB_WORLD = 1, TAB_JAM = 2;
    /** Pestaña elegida (se recuerda al volver a abrir la app). */
    private static int tab;
    /** En el Jam: eligiendo a quién invitar. */
    private boolean inviting;
    /** Canción del mundo esperando el segundo toque para borrarse. */
    private long confirmDel;
    private long confirmAt, jamListAt, contactsAt;

    private void drawList(G g, int mx, int my) {
        hits.clear();
        Font f = font();
        g.fill(s.dx0, s.dy0, s.dx1, s.dy1, s.bg());
        int top = header(g, "celular.app.music", false, mx, my);
        int by = s.cy0 + 3;
        if (tab == TAB_MINE) {
            // botones arriba a la derecha: actualizar y abrir carpeta
            boolean hf = s.in(mx, my, s.dx1 - 17, by, s.dx1 - 3, by + 13), hr = s.in(mx, my, s.dx1 - 33, by, s.dx1 - 19, by + 13);
            D.icon(g, D.FOLDER, s.dx1 - 16, by + 1, 12, hf ? s.accent() : s.fg());
            D.icon(g, D.REFRESH, s.dx1 - 32, by + 1, 12, hr ? s.accent() : s.fg());
            hit(s.dx1 - 17, by, s.dx1 - 3, by + 13, Music::openFolder);
            hit(s.dx1 - 33, by, s.dx1 - 19, by + 13, MusicPlayer::scan);
        } else {
            boolean hr = s.in(mx, my, s.dx1 - 17, by, s.dx1 - 3, by + 13);
            D.icon(g, D.REFRESH, s.dx1 - 16, by + 1, 12, hr ? s.accent() : s.fg());
            hit(s.dx1 - 17, by, s.dx1 - 3, by + 13, () -> {
                Radio.requestLibrary();
                Radio.jamListRequest();
            });
        }
        drawTabs(g, top, mx, my);
        int ctop = top + 19;
        int bottom = s.cy1 - (MusicPlayer.current != null ? 28 : 0);
        switch (tab) {
            case TAB_WORLD -> drawWorld(g, mx, my, ctop, bottom);
            case TAB_JAM -> {
                if (inviting && Radio.inJam()) {
                    drawInvite(g, mx, my, ctop, bottom);
                } else {
                    inviting = false;
                    drawJam(g, mx, my, ctop, bottom);
                }
            }
            default -> drawMine(g, mx, my, ctop, bottom);
        }
        if (MusicPlayer.current != null) {
            drawMini(g, mx, my, bottom);
            int x1 = s.dx1 - 4;
            hit(s.dx0 + 4, bottom, s.dx1 - 4, s.cy1, () -> player = true);
            hit(x1 - 34, bottom, x1 - 18, s.cy1, Radio::toggle);
            hit(x1 - 17, bottom, x1, s.cy1, Radio::next);
        }
    }

    private void drawTabs(G g, int top, int mx, int my) {
        int x0 = s.dx0 + 6, x1 = s.dx1 - 6, y = top + 3;
        D.rr(g, x0, y, x1, y + 13, 6, Prefs.dark ? 0xFF2C2C2E : 0xFFE5E5EA);
        String[] keys = {"celular.radio.tab_mine", "celular.radio.tab_world", "celular.radio.tab_jam"};
        int w = (x1 - x0) / 3;
        for (int i = 0; i < 3; i++) {
            int sx = x0 + i * w, ex = i == 2 ? x1 : sx + w;
            boolean sel = tab == i, h = s.in(mx, my, sx, y, ex, y + 13);
            if (sel) {
                D.rr(g, sx + 1, y + 1, ex - 1, y + 12, 5, RED);
            }
            String t = Component.translatable(keys[i]).getString();
            int tw = (int) (font().width(t) * 0.7F);
            small(g, t, (sx + ex) / 2 - tw / 2, y + 4, sel ? 0xFFFFFFFF : h ? s.fg() : s.sub(), 0.7F);
            if (i == TAB_JAM && (Radio.inJam() || !Radio.invites.isEmpty())) {
                D.circle(g, ex - 4, y + 3, 2, Radio.invites.isEmpty() ? GREEN : 0xFFFF9F0A);
            }
            int ti = i;
            hit(sx, y, ex, y + 13, () -> {
                if (tab != ti) {
                    tab = ti;
                    scroll.reset();
                    inviting = false;
                    if (ti == TAB_WORLD) {
                        Radio.requestLibrary();
                    } else if (ti == TAB_JAM) {
                        Radio.jamListRequest();
                        jamListAt = System.currentTimeMillis();
                    }
                }
            });
        }
    }

    // ---- Mías: las canciones de la carpeta

    private int emptyButtonsY(int ctop) {
        return ctop + 2 + 38 + 22 + font().split(Component.translatable("celular.music.empty_sub"), s.dw() - 16).size() * 10 + 6;
    }

    private void drawMine(G g, int mx, int my, int ctop, int bottom) {
        Font f = font();
        List<Track> lib = MusicPlayer.library;
        if (lib.isEmpty()) {
            int cy = ctop + 2;
            D.circle(g, (s.dx0 + s.dx1) / 2, cy + 14, 16, RED);
            D.icon(g, D.NOTE, (s.dx0 + s.dx1) / 2 - 9, cy + 5, 18, 0xFFFFFFFF);
            int y = cy + 38;
            centered(g, Component.translatable("celular.music.empty").withStyle(ChatFormatting.BOLD), y, s.fg());
            y += 22;
            Component how = Component.translatable("celular.music.empty_sub");
            for (net.minecraft.util.FormattedCharSequence l : f.split(how, s.dw() - 16)) {
                g.drawString(f, l, (s.dx0 + s.dx1) / 2 - f.width(l) / 2, y, s.sub(), false);
                y += 10;
            }
            y = emptyButtonsY(ctop);
            button(g, s.dx0 + 14, y, s.dx1 - 14, y + 16, Component.translatable("celular.music.open_folder"), RED, mx, my);
            hit(s.dx0 + 14, y, s.dx1 - 14, y + 16, Music::openFolder);
            y += 22;
            button(g, s.dx0 + 14, y, s.dx1 - 14, y + 16, Component.translatable("celular.music.refresh"), Prefs.dark ? 0xFF3A3A3C : 0xFF8E8E93, mx, my);
            hit(s.dx0 + 14, y, s.dx1 - 14, y + 16, MusicPlayer::scan);
            y += 24;
            small(g, D.cut(f, MusicPlayer.folder().toString(), (int) ((s.dw() - 12) / 0.6F)), s.dx0 + 6, y, s.sub(), 0.6F);
            return;
        }
        // "aleatorio" y cuántas hay
        int y = ctop;
        boolean hs = s.in(mx, my, s.dx0 + 6, y, s.dx0 + 66, y + 14);
        D.rr(g, s.dx0 + 6, y, s.dx0 + 66, y + 14, 7, hs ? 0xFFFF4D63 : RED);
        D.icon(g, D.SHUFFLE, s.dx0 + 9, y + 2, 10, 0xFFFFFFFF);
        small(g, Component.translatable("celular.music.shuffle_all").getString(), s.dx0 + 21, y + 4, 0xFFFFFFFF, 0.75F);
        hit(s.dx0 + 6, y, s.dx0 + 66, y + 14, () -> {
            Radio.leaveJamForLocal();
            MusicPlayer.shuffle = true;
            Prefs.shuffle = true;
            Prefs.save();
            MusicPlayer.playList(new ArrayList<>(lib), new java.util.Random().nextInt(lib.size()));
            player = true;
        });
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
            boolean hu = s.in(mx, my, s.dx1 - 16, Math.max(ry, listTop), s.dx1, Math.min(ry + rowH, bottom));
            if (s.in(mx, my, s.dx0, Math.max(ry, listTop), s.dx1, Math.min(ry + rowH, bottom))) {
                g.fill(s.dx0, ry, s.dx1, ry + rowH, Prefs.dark ? 0xFF1C1C1E : 0xFFF2F2F7);
            }
            D.cover(g, t.hue(), s.dx0 + 6, ry + 3, 16, 3);
            int tw = s.dw() - 46 - (isCur ? 10 : 0);
            g.drawString(f, D.cut(f, t.title, tw), s.dx0 + 26, ry + 3, t.broken ? s.sub() : isCur ? RED : s.fg(), false);
            String sub = t.broken ? Component.translatable("celular.music.broken").getString()
                    : (t.artist.isEmpty() ? Component.translatable("celular.music.unknown_artist").getString() : t.artist)
                    + (t.seconds > 0 ? " · " + D.clock(t.seconds) : "");
            small(g, D.cut(f, sub, (int) (tw / 0.7F)), s.dx0 + 26, ry + 13, s.sub(), 0.7F);
            if (isCur) {
                miniBars(g, s.dx1 - 26, ry + 15, RED);
            }
            // subir al mundo
            if (Radio.uploading == t) {
                String pc = (int) (Radio.upProgress * 100) + "%";
                small(g, pc, s.dx1 - 4 - (int) (f.width(pc) * 0.6F), ry + 8, RED, 0.6F);
                g.fill(s.dx0 + 26, ry + rowH - 2, s.dx0 + 26 + (int) ((s.dw() - 30) * Radio.upProgress), ry + rowH - 1, RED);
            } else if (t.worldId != 0) {
                D.icon(g, D.GLOBE, s.dx1 - 14, ry + 6, 10, GREEN);
            } else if (!t.broken) {
                D.icon(g, D.UPLOAD, s.dx1 - 14, ry + 6, 10, hu ? s.accent() : s.sub());
            }
            if (i < lib.size() - 1) {
                g.fill(s.dx0 + 26, ry + rowH - 1, s.dx1, ry + rowH, s.line());
            }
            int ti = i;
            hitIn(s.dx0, ry, s.dx1, ry + rowH, listTop, bottom, () -> {
                Radio.leaveJamForLocal();
                if (t == MusicPlayer.current && !MusicPlayer.external) {
                    if (!MusicPlayer.playing) {
                        MusicPlayer.toggle();
                    }
                } else {
                    MusicPlayer.playList(new ArrayList<>(lib), ti);
                }
                player = true;
            });
            hitIn(s.dx1 - 16, ry, s.dx1, ry + rowH, listTop, bottom, () -> uploadClick(t));
        }
        g.noScissor();
    }

    private void uploadClick(Track t) {
        if (Radio.uploading == t) {
            Radio.cancelUpload();
            PhoneHud.toastQuiet(D.APP_MUSIC, "celular.app.music", Component.translatable("celular.radio.upload_cancel"));
        } else if (t.worldId != 0) {
            PhoneHud.toastQuiet(D.APP_MUSIC, "celular.app.music", Component.translatable("celular.radio.already"));
        } else if (t.broken) {
            PhoneHud.toastQuiet(D.APP_MUSIC, "celular.app.music", Component.translatable("celular.music.broken"));
        } else if (Radio.uploading != null) {
            PhoneHud.toastQuiet(D.APP_MUSIC, "celular.app.music", Component.translatable("celular.radio.upload_busy"));
        } else {
            Radio.upload(t);
            PhoneHud.toastQuiet(D.APP_MUSIC, "celular.app.music", Component.translatable("celular.radio.uploading", t.title));
        }
    }

    // ---- Mundo: las canciones subidas al mundo

    private static String mb(long bytes) {
        double m = bytes / (1024.0 * 1024.0);
        return m < 10 ? String.format(java.util.Locale.ROOT, "%.1f", m) : Long.toString(Math.round(m));
    }

    private void drawWorld(G g, int mx, int my, int ctop, int bottom) {
        Font f = font();
        int y = ctop;
        // altavoz encendido / apagado
        String lbl = Component.translatable("celular.radio.speaker").getString();
        int pw = 21 + (int) (f.width(lbl) * 0.7F) + 5;
        boolean hs = s.in(mx, my, s.dx0 + 6, y, s.dx0 + 6 + pw, y + 14);
        int off = Prefs.dark ? 0xFF3A3A3C : 0xFFAEAEB2;
        D.rr(g, s.dx0 + 6, y, s.dx0 + 6 + pw, y + 14, 7, Prefs.speaker ? (hs ? 0xFF3BD873 : GREEN) : (hs ? D.mix(off, 0xFFFFFFFF, 0.15F) : off));
        D.icon(g, D.SPEAKER, s.dx0 + 9, y + 2, 10, 0xFFFFFFFF);
        small(g, lbl, s.dx0 + 21, y + 4, 0xFFFFFFFF, 0.7F);
        hit(s.dx0 + 6, y, s.dx0 + 6 + pw, y + 14, () -> {
            Prefs.speaker = !Prefs.speaker;
            Prefs.save();
            PhoneHud.toastQuiet(D.APP_MUSIC, "celular.app.music",
                    Component.translatable(Prefs.speaker ? "celular.radio.speaker_on" : "celular.radio.speaker_off"));
        });
        if (Radio.loaded) {
            String q = mb(Radio.used) + "/" + mb(Radio.max) + " MB";
            small(g, q, s.dx1 - 6 - (int) (f.width(q) * 0.65F), y + 5, s.sub(), 0.65F);
        }
        y += 17;
        // subida en curso
        Track up = Radio.uploading;
        if (up != null) {
            D.icon(g, D.UPLOAD, s.dx0 + 6, y, 8, RED);
            String pc = " " + (int) (Radio.upProgress * 100) + "%";
            small(g, D.cut(f, up.title, (int) ((s.dw() - 40) / 0.65F)) + pc, s.dx0 + 16, y + 1, s.fg(), 0.65F);
            g.fill(s.dx0 + 16, y + 8, s.dx1 - 6, y + 9, s.line());
            g.fill(s.dx0 + 16, y + 8, s.dx0 + 16 + (int) ((s.dw() - 22) * Radio.upProgress), y + 9, RED);
            y += 12;
        }
        // altavoces de otros que se oyen cerca
        int nn = 0;
        for (String n : Radio.nearbySpeakers()) {
            if (nn++ >= 2) {
                break;
            }
            D.icon(g, D.SPEAKER, s.dx0 + 6, y, 8, GREEN);
            small(g, D.cut(f, n, (int) ((s.dw() - 22) / 0.65F)), s.dx0 + 16, y + 1, s.sub(), 0.65F);
            y += 10;
        }
        int listTop = y + 1;
        List<com.celular.radio.SongInfo> songs = Radio.songs;
        if (!Radio.loaded) {
            spinner(g, (s.dx0 + s.dx1) / 2, listTop + 30, s.sub());
            return;
        }
        if (songs.isEmpty()) {
            int cy = listTop + 8;
            D.circle(g, (s.dx0 + s.dx1) / 2, cy + 12, 13, GREEN);
            D.icon(g, D.GLOBE, (s.dx0 + s.dx1) / 2 - 7, cy + 5, 14, 0xFFFFFFFF);
            centered(g, Component.translatable("celular.radio.world_empty").withStyle(ChatFormatting.BOLD), cy + 32, s.fg());
            centered(g, Component.translatable("celular.radio.world_empty_sub"), cy + 54, s.sub());
            return;
        }
        int rowH = 22;
        long now = System.currentTimeMillis();
        scroll.update(songs.size() * rowH, bottom - listTop);
        g.scissor(s.dx0, listTop, s.dx1, bottom);
        boolean jam = Radio.inJam();
        for (int k = 0; k < songs.size(); k++) {
            int ry = listTop + k * rowH - scroll.off();
            if (ry + rowH < listTop || ry > bottom) {
                continue;
            }
            com.celular.radio.SongInfo i = songs.get(k);
            boolean isCur = MusicPlayer.current != null && MusicPlayer.current.worldId == i.id;
            boolean del = Radio.mine(i) || Radio.admin;
            if (s.in(mx, my, s.dx0, Math.max(ry, listTop), s.dx1, Math.min(ry + rowH, bottom))) {
                g.fill(s.dx0, ry, s.dx1, ry + rowH, Prefs.dark ? 0xFF1C1C1E : 0xFFF2F2F7);
            }
            D.cover(g, Math.floorMod((i.title + "|" + i.artist).hashCode(), 360), s.dx0 + 6, ry + 3, 16, 3);
            int right = 15 + (del ? 13 : 0);
            int tw = s.dw() - 26 - right;
            g.drawString(f, D.cut(f, i.title, tw), s.dx0 + 26, ry + 3, isCur ? RED : s.fg(), false);
            String sub = (i.artist.isEmpty() ? Component.translatable("celular.music.unknown_artist").getString() : i.artist) + " · "
                    + i.uploader + " · " + D.clock(i.seconds);
            small(g, D.cut(f, sub, (int) (tw / 0.7F)), s.dx0 + 26, ry + 13, s.sub(), 0.7F);
            // estado: agregar al Jam, bajando, por bajar o sonando
            int sx = s.dx1 - (del ? 27 : 14);
            boolean hst = s.in(mx, my, sx - 2, Math.max(ry, listTop), sx + 12, Math.min(ry + rowH, bottom));
            float pr = Radio.progress(i.id);
            if (jam) {
                D.icon(g, D.PLUS, sx, ry + 6, 10, hst ? GREEN : s.sub());
            } else if (pr >= 0) {
                String pc = (int) (pr * 100) + "%";
                small(g, pc, sx + 10 - (int) (f.width(pc) * 0.55F), ry + 8, GREEN, 0.55F);
            } else if (Radio.queued(i.id) || Radio.pendingPlay == i.id) {
                small(g, "···", sx + 1, ry + 7, GREEN, 0.7F);
            } else if (isCur) {
                miniBars(g, sx + 2, ry + 15, RED);
            } else if (!Radio.ready(i)) {
                D.icon(g, D.DOWNLOAD, sx, ry + 6, 10, D.alpha(s.sub(), 0.6F));
            }
            if (del) {
                boolean confirming = confirmDel == i.id && now - confirmAt < 3000;
                boolean hd = s.in(mx, my, s.dx1 - 14, Math.max(ry, listTop), s.dx1, Math.min(ry + rowH, bottom));
                D.icon(g, D.TRASH, s.dx1 - 13, ry + 6, 10, confirming ? 0xFFFF3B30 : hd ? 0xFFFF6961 : s.sub());
            }
            if (k < songs.size() - 1) {
                g.fill(s.dx0 + 26, ry + rowH - 1, s.dx1, ry + rowH, s.line());
            }
            hitIn(s.dx0, ry, s.dx1, ry + rowH, listTop, bottom, () -> {
                if (Radio.inJam()) {
                    Radio.jamAdd(i);
                    PhoneHud.toastQuiet(D.APP_MUSIC, "celular.app.music", Component.translatable("celular.radio.added", i.title));
                } else {
                    Radio.play(i);
                    if (Radio.ready(i)) {
                        player = true;
                    }
                }
            });
            if (del) {
                hitIn(s.dx1 - 14, ry, s.dx1, ry + rowH, listTop, bottom, () -> {
                    long t = System.currentTimeMillis();
                    if (confirmDel == i.id && t - confirmAt < 3000) {
                        confirmDel = 0;
                        Radio.delete(i);
                    } else {
                        confirmDel = i.id;
                        confirmAt = t;
                        PhoneHud.toastQuiet(D.APP_MUSIC, "celular.app.music", Component.translatable("celular.radio.confirm_delete"));
                    }
                });
            }
        }
        g.noScissor();
    }

    // ---- Jam: oír lo mismo con otros, al mismo tiempo

    private String me() {
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        return mc.player == null ? "" : mc.player.getScoreboardName();
    }

    /** Botoncito redondo con texto chico; devuelve su ancho. */
    private int pill(G g, int x1, int y, String text, int color, int mx, int my, Runnable run) {
        int w = (int) (font().width(text) * 0.65F) + 10;
        int x0 = x1 - w;
        boolean h = s.in(mx, my, x0, y, x1, y + 12);
        D.rr(g, x0, y, x1, y + 12, 6, h ? D.mix(color, 0xFFFFFFFF, 0.15F) : color);
        small(g, text, x0 + 5, y + 4, 0xFFFFFFFF, 0.65F);
        hit(x0, y, x1, y + 12, run);
        return w;
    }

    private void drawJam(G g, int mx, int my, int ctop, int bottom) {
        long now = System.currentTimeMillis();
        if (now - jamListAt > 4000 && !Radio.inJam()) {
            jamListAt = now;
            Radio.jamListRequest();
        }
        if (Radio.inJam()) {
            drawInJam(g, mx, my, ctop, bottom);
            return;
        }
        Font f = font();
        int y = ctop;
        // invitaciones
        int shown = 0;
        for (String[] v : new ArrayList<>(Radio.invites)) {
            if (shown++ >= 2) {
                break;
            }
            D.rr(g, s.dx0 + 6, y, s.dx1 - 6, y + 18, 5, s.card());
            D.icon(g, D.HEADPHONES, s.dx0 + 9, y + 4, 10, 0xFFFF9F0A);
            int pw = pill(g, s.dx1 - 9, y + 3, Component.translatable("celular.radio.join").getString(), GREEN, mx, my, () -> {
                try {
                    Radio.jamJoin(Long.parseLong(v[0]));
                } catch (NumberFormatException ignored) {
                }
            });
            int tw = s.dw() - 30 - pw;
            small(g, D.cut(f, Component.translatable("celular.radio.invites_from", v[2]).getString(), (int) (tw / 0.65F)), s.dx0 + 22, y + 3, s.fg(),
                    0.65F);
            small(g, D.cut(f, v[1], (int) (tw / 0.6F)), s.dx0 + 22, y + 10, s.sub(), 0.6F);
            y += 21;
        }
        // crear
        button(g, s.dx0 + 6, y, s.dx1 - 6, y + 15, Component.translatable("celular.radio.jam_create"), GREEN, mx, my);
        hit(s.dx0 + 6, y, s.dx1 - 6, y + 15, () -> Radio.jamCreate(""));
        y += 20;
        small(g, Component.translatable("celular.radio.jam_list").getString().toUpperCase(java.util.Locale.ROOT), s.dx0 + 6, y, s.sub(), 0.6F);
        y += 8;
        com.celular.net.RadioNet.JamList l = Radio.jams;
        if (l == null) {
            spinner(g, (s.dx0 + s.dx1) / 2, y + 20, s.sub());
            return;
        }
        if (l.ids.isEmpty()) {
            centered(g, Component.translatable("celular.radio.jam_none").withStyle(ChatFormatting.BOLD), y + 12, s.fg());
            centered(g, Component.translatable("celular.radio.jam_none_sub"), y + 26, s.sub());
            return;
        }
        int listTop = y, rowH = 22;
        scroll.update(l.ids.size() * rowH, bottom - listTop);
        g.scissor(s.dx0, listTop, s.dx1, bottom);
        for (int k = 0; k < l.ids.size(); k++) {
            int ry = listTop + k * rowH - scroll.off();
            if (ry + rowH < listTop || ry > bottom) {
                continue;
            }
            if (s.in(mx, my, s.dx0, Math.max(ry, listTop), s.dx1, Math.min(ry + rowH, bottom))) {
                g.fill(s.dx0, ry, s.dx1, ry + rowH, Prefs.dark ? 0xFF1C1C1E : 0xFFF2F2F7);
            }
            D.circle(g, s.dx0 + 14, ry + 11, 8, GREEN);
            D.icon(g, D.HEADPHONES, s.dx0 + 9, ry + 6, 10, 0xFFFFFFFF);
            int tw = s.dw() - 30;
            g.drawString(f, D.cut(f, l.names.get(k), tw), s.dx0 + 26, ry + 3, s.fg(), false);
            String sub = l.hosts.get(k) + " · " + Component.translatable("celular.radio.people", l.members.get(k)).getString()
                    + (l.now.get(k).isEmpty() ? "" : " · ♪ " + l.now.get(k));
            small(g, D.cut(f, sub, (int) (tw / 0.7F)), s.dx0 + 26, ry + 13, s.sub(), 0.7F);
            if (k < l.ids.size() - 1) {
                g.fill(s.dx0 + 26, ry + rowH - 1, s.dx1, ry + rowH, s.line());
            }
            long id = l.ids.get(k);
            hitIn(s.dx0, ry, s.dx1, ry + rowH, listTop, bottom, () -> Radio.jamJoin(id));
        }
        g.noScissor();
    }

    private void drawInJam(G g, int mx, int my, int ctop, int bottom) {
        Font f = font();
        com.celular.net.RadioNet.JamState j = Radio.jam;
        int y = ctop;
        // nombre y salir
        D.icon(g, D.HEADPHONES, s.dx0 + 6, y + 1, 10, GREEN);
        int pw = pill(g, s.dx1 - 6, y, Component.translatable("celular.radio.leave").getString(), 0xFFFF3B30, mx, my, () -> {
            Radio.jamLeave();
            Radio.jam = com.celular.net.RadioNet.JamState.none();
            if (MusicPlayer.external) {
                MusicPlayer.stop();
            }
        });
        g.drawString(f, Component.literal(D.cut(f, j.name, s.dw() - 24 - pw)).withStyle(ChatFormatting.BOLD), s.dx0 + 19, y + 2, s.fg(), false);
        y += 14;
        // anfitrión, gente e invitar
        int iw = pill(g, s.dx1 - 6, y, Component.translatable("celular.radio.invite").getString(), s.accent(), mx, my, () -> {
            inviting = true;
            scroll.reset();
            requestContacts();
        });
        D.icon(g, D.CROWN, s.dx0 + 6, y + 2, 8, 0xFFFFCC00);
        String who = j.host + " · " + Component.translatable("celular.radio.people", j.members.size()).getString();
        small(g, D.cut(f, who, (int) ((s.dw() - 22 - iw) / 0.65F)), s.dx0 + 16, y + 3, s.sub(), 0.65F);
        y += 16;
        // lo que suena
        com.celular.radio.SongInfo cur = j.current();
        D.rr(g, s.dx0 + 6, y, s.dx1 - 6, y + 34, 6, s.card());
        if (cur == null) {
            small(g, D.cut(f, Component.translatable("celular.radio.queue_empty").getString(), (int) ((s.dw() - 20) / 0.65F)), s.dx0 + 10, y + 7,
                    s.sub(), 0.65F);
            small(g, D.cut(f, Component.translatable("celular.radio.queue_hint").getString(), (int) ((s.dw() - 20) / 0.65F)), s.dx0 + 10, y + 17,
                    s.sub(), 0.65F);
            hit(s.dx0 + 6, y, s.dx1 - 6, y + 34, () -> {
                tab = TAB_WORLD;
                scroll.reset();
            });
        } else {
            D.cover(g, Math.floorMod((cur.title + "|" + cur.artist).hashCode(), 360), s.dx0 + 9, y + 3, 18, 3);
            int tw = s.dw() - 72;
            g.drawString(f, D.cut(f, cur.title, tw), s.dx0 + 31, y + 4, s.fg(), false);
            String sub = cur.artist.isEmpty() ? Component.translatable("celular.music.unknown_artist").getString() : cur.artist;
            float pr = Radio.progress(cur.id);
            if (!Radio.ready(cur)) {
                sub = Component.translatable("celular.radio.loading").getString() + (pr >= 0 ? " " + (int) (pr * 100) + "%" : "");
            }
            small(g, D.cut(f, sub, (int) (tw / 0.7F)), s.dx0 + 31, y + 14, s.sub(), 0.7F);
            double pos = Math.min(cur.seconds, Radio.jamPos());
            int bx0 = s.dx0 + 10, bx1 = s.dx1 - 10;
            g.fill(bx0, y + 28, bx1, y + 29, s.line());
            g.fill(bx0, y + 28, bx0 + (int) ((bx1 - bx0) * (cur.seconds > 0 ? pos / cur.seconds : 0)), y + 29, GREEN);
            // controles
            int cx = s.dx1 - 24;
            if (j.amHost) {
                boolean hp = s.in(mx, my, cx - 16, y + 4, cx - 2, y + 22), hn = s.in(mx, my, cx, y + 4, cx + 14, y + 22);
                D.icon(g, j.playing ? D.PAUSE : D.PLAY, cx - 15, y + 7, 12, hp ? GREEN : s.fg());
                D.icon(g, D.SKIP, cx + 1, y + 7, 12, hn ? GREEN : s.fg());
                hit(cx - 16, y + 4, cx - 2, y + 22, Radio::toggle);
                hit(cx, y + 4, cx + 14, y + 22, Radio::next);
            } else {
                boolean hv = s.in(mx, my, cx - 16, y + 4, cx + 16, y + 22);
                D.icon(g, D.THUMB, cx - 14, y + 5, 10, hv ? GREEN : s.fg());
                small(g, j.votes + "/" + j.needed, cx - 2, y + 8, s.sub(), 0.65F);
                hit(cx - 16, y + 4, cx + 16, y + 22, () -> {
                    Radio.next();
                    PhoneHud.toastQuiet(D.APP_MUSIC, "celular.app.music", Component.translatable("celular.radio.voted"));
                });
            }
        }
        y += 38;
        // botonera de DJ: lo oyen todos los del Jam
        String[] djKeys = {"celular.radio.dj_horn", "celular.radio.dj_scratch", "celular.radio.dj_claps", "celular.radio.dj_drop"};
        int[] djIcons = {D.SPEAKER, D.REFRESH, D.PEOPLE, D.VOL_HIGH};
        int[] djCols = {0xFFFF9F0A, 0xFFBF5AF2, 0xFF30D158, 0xFFFF375F};
        int gap = 3, bw = (s.dw() - 12 - gap * 3) / 4;
        long now = System.currentTimeMillis();
        for (int k = 0; k < 4; k++) {
            int bx = s.dx0 + 6 + k * (bw + gap);
            boolean hov = s.in(mx, my, bx, y, bx + bw, y + 18);
            boolean lit = Radio.djFx == k && now - Radio.djAt < 700;
            int col = lit ? D.mix(djCols[k], 0xFFFFFFFF, 0.35F) : hov ? D.mix(djCols[k], 0xFFFFFFFF, 0.15F) : djCols[k];
            D.rr(g, bx, y, bx + bw, y + 18, 5, col);
            D.icon(g, djIcons[k], bx + bw / 2 - 4, y + 2, 8, 0xFFFFFFFF);
            String lbl = D.cut(f, Component.translatable(djKeys[k]).getString(), (int) ((bw - 2) / 0.5F));
            small(g, lbl, bx + bw / 2 - (int) (f.width(lbl) * 0.5F / 2), y + 12, 0xFFFFFFFF, 0.5F);
            int fx = k;
            hit(bx, y, bx + bw, y + 18, () -> Radio.djPress(fx));
        }
        if (now - Radio.djAt < 1500 && !Radio.djWho.isEmpty()) {
            String w = "♪ " + Radio.djWho;
            small(g, D.cut(f, w, (int) ((s.dw() - 12) / 0.55F)), s.dx1 - 6 - (int) (f.width(w) * 0.55F), y + 20, djCols[Math.max(0, Radio.djFx)], 0.55F);
        }
        y += 24;
        // cola
        small(g, Component.translatable("celular.radio.queue").getString().toUpperCase(java.util.Locale.ROOT), s.dx0 + 6, y, s.sub(), 0.6F);
        boolean ha = s.in(mx, my, s.dx1 - 20, y - 3, s.dx1, y + 8);
        D.icon(g, D.PLUS, s.dx1 - 15, y - 2, 9, ha ? GREEN : s.sub());
        hit(s.dx1 - 20, y - 3, s.dx1, y + 8, () -> {
            tab = TAB_WORLD;
            scroll.reset();
        });
        y += 9;
        int listTop = y, rowH = 16;
        int from = Math.max(0, j.index + 1);
        int n = Math.max(0, j.queue.size() - from);
        if (n == 0) {
            small(g, D.cut(f, Component.translatable("celular.radio.queue_hint").getString(), (int) ((s.dw() - 12) / 0.65F)), s.dx0 + 6, y + 3,
                    s.sub(), 0.65F);
            return;
        }
        String meName = me();
        scroll.update(n * rowH, bottom - listTop);
        g.scissor(s.dx0, listTop, s.dx1, bottom);
        for (int k = 0; k < n; k++) {
            int qi = from + k;
            int ry = listTop + k * rowH - scroll.off();
            if (ry + rowH < listTop || ry > bottom) {
                continue;
            }
            com.celular.radio.SongInfo i = j.queue.get(qi);
            String by = qi < j.addedBy.size() ? j.addedBy.get(qi) : "";
            boolean canRemove = j.amHost || by.equals(meName);
            small(g, Integer.toString(k + 1), s.dx0 + 6, ry + 5, s.sub(), 0.65F);
            int tw = s.dw() - 26 - (canRemove ? 12 : 0);
            small(g, D.cut(f, i.title, (int) (tw / 0.75F)), s.dx0 + 16, ry + 2, s.fg(), 0.75F);
            small(g, D.cut(f, Component.translatable("celular.radio.by", by).getString(), (int) (tw / 0.6F)), s.dx0 + 16, ry + 10, s.sub(), 0.6F);
            if (canRemove) {
                boolean hx = s.in(mx, my, s.dx1 - 14, Math.max(ry, listTop), s.dx1, Math.min(ry + rowH, bottom));
                D.icon(g, D.CLOSE, s.dx1 - 12, ry + 4, 8, hx ? 0xFFFF3B30 : s.sub());
                hitIn(s.dx1 - 14, ry, s.dx1, ry + rowH, listTop, bottom, () -> Radio.jamRemove(qi));
            }
        }
        g.noScissor();
    }

    private void requestContacts() {
        long now = System.currentTimeMillis();
        if (now - contactsAt > 2000) {
            contactsAt = now;
            com.celular.plat.Net.toServer(new com.celular.net.CelularNet.Simple(com.celular.net.CelularNet.Simple.CONTACTS));
        }
    }

    private void drawInvite(G g, int mx, int my, int ctop, int bottom) {
        Font f = font();
        int y = ctop;
        boolean hb = s.in(mx, my, s.dx0 + 4, y, s.dx0 + 18, y + 12);
        D.icon(g, D.BACK, s.dx0 + 5, y, 10, hb ? s.accent() : s.fg());
        hit(s.dx0 + 2, y - 2, s.dx0 + 20, y + 13, () -> inviting = false);
        g.drawString(f, Component.translatable("celular.radio.invite_title").withStyle(ChatFormatting.BOLD), s.dx0 + 19, y + 2, s.fg(), false);
        y += 15;
        com.celular.net.CelularNet.Contacts c = ClientHooks.contacts;
        if (c == null) {
            spinner(g, (s.dx0 + s.dx1) / 2, y + 20, s.sub());
            return;
        }
        String meName = me();
        List<Integer> who = new ArrayList<>();
        for (int i = 0; i < c.ids.size(); i++) {
            if (c.phone.get(i) && !c.names.get(i).equals(meName) && !Radio.jam.members.contains(c.names.get(i))) {
                who.add(i);
            }
        }
        if (who.isEmpty()) {
            centered(g, Component.translatable("celular.radio.invite_none"), y + 14, s.sub());
            return;
        }
        int listTop = y, rowH = 20;
        scroll.update(who.size() * rowH, bottom - listTop);
        g.scissor(s.dx0, listTop, s.dx1, bottom);
        for (int k = 0; k < who.size(); k++) {
            int i = who.get(k);
            int ry = listTop + k * rowH - scroll.off();
            if (ry + rowH < listTop || ry > bottom) {
                continue;
            }
            if (s.in(mx, my, s.dx0, Math.max(ry, listTop), s.dx1, Math.min(ry + rowH, bottom))) {
                g.fill(s.dx0, ry, s.dx1, ry + rowH, Prefs.dark ? 0xFF1C1C1E : 0xFFF2F2F7);
            }
            java.util.UUID id = c.ids.get(i);
            String name = c.names.get(i);
            D.faceRound(g, id, s.dx0 + 6, ry + 3, 14, s.line());
            g.drawString(f, D.cut(f, name, s.dw() - 44), s.dx0 + 25, ry + 6, s.fg(), false);
            D.icon(g, D.SEND, s.dx1 - 16, ry + 5, 10, s.accent());
            hitIn(s.dx0, ry, s.dx1, ry + rowH, listTop, bottom, () -> {
                Radio.jamInvite(id);
                PhoneHud.toastQuiet(D.APP_MUSIC, "celular.app.music", Component.translatable("celular.radio.invite_sent", name));
            });
        }
        g.noScissor();
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
        boolean guest = Radio.inJam() && !Radio.jam.amHost;
        D.icon(g, MusicPlayer.playing ? D.PAUSE : D.PLAY, x1 - 32, y + 7, 12, guest ? s.sub() : hp ? 0xFFFA233B : s.fg());
        D.icon(g, D.SKIP, x1 - 16, y + 7, 12, hn ? 0xFFFA233B : s.fg());
    }

    private void miniBars(G g, int x, int yBase, int color) {
        for (int i = 0; i < 3; i++) {
            float v = MusicPlayer.playing ? Math.max(0.15F, bars[2 + i * 4]) : 0.15F;
            int h = 2 + (int) (v * 7);
            g.fill(x + i * 3, yBase - h, x + i * 3 + 2, yBase, color);
        }
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
        boolean jam = Radio.inJam();
        String label = (jam ? Component.translatable("celular.radio.jam_label", Radio.jam.name)
                : Component.translatable("celular.music.now_playing")).getString().toUpperCase(java.util.Locale.ROOT);
        label = D.cut(f, label, (int) ((s.dw() - 44) / 0.65F));
        small(g, label, cx - (int) (f.width(label) * 0.65F / 2), hy + 5, jam ? 0xFF7CF0A6 : 0xB0FFFFFF, 0.65F);
        if (Radio.broadcasting()) {
            // lo que suena lo oyen los de cerca
            D.icon(g, D.SPEAKER, s.dx1 - 15, hy + 2, 10, 0xFF7CF0A6);
        }
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
                D.circle(g, xs[i], cy, h ? 12 : 11, jam && !Radio.jam.amHost ? 0x80FFFFFF : 0xFFFFFFFF);
                D.icon(g, icons[i], xs[i] - 7, cy - 7, 14, top);
            } else {
                boolean on = !jam && ((i == 0 && MusicPlayer.shuffle) || (i == 4 && MusicPlayer.repeat > 0));
                int size = i == 1 || i == 3 ? 14 : 11;
                int col = on ? 0xFFFFFFFF : (i == 0 || i == 4 ? 0x80FFFFFF : 0xE6FFFFFF);
                if (h) {
                    col = 0xFFFFFFFF;
                }
                if (jam && i != 3) {
                    col = 0x30FFFFFF;                  // en el Jam no se usan (solo saltar)
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
        clickHits(x, y);
        return true;
    }

    private boolean clickPlayer(double x, double y) {
        if (s.in(x, y, s.dx0, s.cy0, s.dx0 + 22, s.cy0 + 18)) {
            player = false;
            ClientHooks.tap();
            return true;
        }
        int[] pb = progressBar();
        if (!Radio.inJam() && s.in(x, y, pb[0] - 3, pb[1] - 5, pb[2] + 3, pb[1] + 8)) {
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
                if (Radio.inJam()) {
                    if (i == 2) {
                        Radio.toggle();
                    } else if (i == 3) {
                        Radio.next();
                    }
                    return true;
                }
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
            Radio.toggle();
            return true;
        }
        if (player && key == 262) {          // flecha derecha = siguiente
            Radio.next();
            return true;
        }
        if (player && key == 263) {
            Radio.prev();
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
