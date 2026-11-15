package com.celular.client;

import com.celular.CelularReg;
import com.celular.net.CelularNet;
import com.celular.net.CelularNet.Post;
import com.celular.plat.Net;
import com.celular.plat.client.G;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;

import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

/**
 * Instacraft: el muro con las fotos de todos (doble clic en una foto = me gusta), el perfil de cada jugador con sus fotos y cuántos me
 * gusta tiene, y publicar: se escoge una foto de la galería, un filtro y un texto.
 */
final class InstaApp extends App {
    private static final int FEED = 0, PROFILE = 1, POST = 2, COMPOSE = 3;
    private static final int PINK = 0xFFE1306C;

    private int page = FEED;
    private final Scroll scroll = new Scroll();
    private Post single;
    private UUID profileOf;
    private String profileName = "";
    private boolean confirmDelete;
    private int postFrom = FEED;
    /** Doble clic para "me gusta": la última foto tocada y cuándo. */
    private long lastTapId = -1, lastTapAt;
    /** Corazón grande que aparece sobre la foto. */
    private long heartId = -1, heartAt;
    // publicar
    private Path composePath;
    private BufferedImage composeImg;
    private int filter;
    private ResourceLocation preview;
    private final ResourceLocation[] filterThumbs = new ResourceLocation[Photos.FILTERS.length];
    private EditBox caption;
    private String captionText = "";
    private boolean uploading;
    private long uploadAt;

    InstaApp(PhoneScreen s) {
        super(s, D.APP_INSTA, "insta");
    }

    @Override
    void enter() {
        ClientHooks.badgeInsta = 0;
        if (page == COMPOSE && composeImg != null) {
            placeCaption();
            return;
        }
        if (page == POST || page == COMPOSE) {
            page = FEED;
        }
        refresh();
    }

    @Override
    void leave() {
        if (caption != null) {
            captionText = caption.getValue();
        }
        caption = null;
        s.removeBox();
    }

    private void refresh() {
        if (page == PROFILE && profileOf != null) {
            ClientHooks.profileLoaded = false;
            Net.toServer(new CelularNet.FeedReq(profileOf));
        } else {
            Net.toServer(new CelularNet.FeedReq(null));
        }
    }

    private void go(int p) {
        if (page == COMPOSE && p != COMPOSE) {
            leave();
        }
        page = p;
        scroll.reset();
        confirmDelete = false;
    }

    void openProfile(UUID id, String name) {
        profileOf = id;
        profileName = name;
        go(PROFILE);
        ClientHooks.profileLoaded = false;
        Net.toServer(new CelularNet.FeedReq(id));
    }

    UUID me() {
        Minecraft mc = Minecraft.getInstance();
        return mc.player != null ? mc.player.getUUID() : new UUID(0, 0);
    }

    String myName() {
        Minecraft mc = Minecraft.getInstance();
        return mc.player != null ? mc.player.getScoreboardName() : "";
    }

    @Override
    boolean lightStatus() {
        return !Prefs.dark;
    }

    // ================================================================== dibujo

    @Override
    void draw(G g, int mx, int my, float pt) {
        g.fill(s.dx0, s.dy0, s.dx1, s.dy1, s.bg());
        switch (page) {
            case PROFILE -> drawProfile(g, mx, my);
            case POST -> drawSingle(g, mx, my);
            case COMPOSE -> drawCompose(g, mx, my);
            default -> drawFeed(g, mx, my);
        }
        if (page != COMPOSE) {
            drawTabs(g, mx, my);
        }
    }

    private int tabTop() {
        return s.cy1 - 16;
    }

    private void drawTabs(G g, int mx, int my) {
        int y = tabTop();
        g.fill(s.dx0, y, s.dx1, s.dy1, s.bg());
        g.fill(s.dx0, y, s.dx1, y + 1, s.line());
        int w = s.dw() / 3;
        int[] icons = {D.HOME, D.PLUS, D.USER};
        boolean[] on = {page == FEED, false, page == PROFILE && me().equals(profileOf)};
        for (int i = 0; i < 3; i++) {
            int cx = s.dx0 + i * w + w / 2;
            boolean h = s.in(mx, my, s.dx0 + i * w, y, s.dx0 + (i + 1) * w, y + 16);
            int c = on[i] ? s.fg() : h ? D.mix(s.sub(), s.fg(), 0.5F) : s.sub();
            if (i == 2) {
                D.faceRound(g, me(), cx - 5, y + 3, 10, on[i] ? s.fg() : s.bg());
            } else {
                D.icon(g, icons[i], cx - 6, y + 2, 12, c);
            }
        }
    }

    /** Logo "Instacraft" con degradado de colores letra por letra. */
    private void logo(G g, int x, int y) {
        String t = "Instacraft";
        Font f = font();
        int[] cols = {0xFFFEDA75, 0xFFFA7E1E, 0xFFD62976, 0xFF962FBF, 0xFF4F5BD5};
        for (int i = 0; i < t.length(); i++) {
            float k = i / (float) (t.length() - 1) * (cols.length - 1);
            int a = (int) Math.floor(k), b = Math.min(cols.length - 1, a + 1);
            int c = D.mix(cols[a], cols[b], k - a);
            String ch = t.substring(i, i + 1);
            g.drawString(f, Component.literal(ch).withStyle(ChatFormatting.BOLD, ChatFormatting.ITALIC), x, y, c, false);
            x += f.width(Component.literal(ch).withStyle(ChatFormatting.BOLD, ChatFormatting.ITALIC));
        }
    }

    private void drawFeed(G g, int mx, int my) {
        int top = s.cy0;
        g.fill(s.dx0, top, s.dx1, top + 18, s.bg());
        logo(g, s.dx0 + 6, top + 5);
        boolean hr = s.in(mx, my, s.dx1 - 18, top, s.dx1, top + 18);
        D.icon(g, D.SWITCH, s.dx1 - 16, top + 4, 11, hr ? s.accent() : s.fg());
        g.fill(s.dx0, top + 18, s.dx1, top + 19, s.line());
        top += 19;
        int bottom = tabTop();
        List<Post> posts = ClientHooks.feed;
        if (!ClientHooks.feedLoaded) {
            spinner(g, (s.dx0 + s.dx1) / 2, (top + bottom) / 2, s.fg());
            return;
        }
        if (posts.isEmpty()) {
            D.icon(g, D.CAMERA, (s.dx0 + s.dx1) / 2 - 12, top + 28, 24, s.sub());
            centered(g, Component.translatable("celular.insta.empty"), top + 58, s.sub());
            button(g, s.dx0 + 16, top + 92, s.dx1 - 16, top + 108, Component.translatable("celular.insta.first"), PINK, mx, my);
            return;
        }
        g.scissor(s.dx0, top, s.dx1, bottom);
        int y = top - scroll.off();
        int total = 0;
        for (Post p : posts) {
            int h = postHeight(p);
            if (y + h > top && y < bottom) {
                drawPost(g, p, y, mx, my, top, bottom);
            }
            y += h;
            total += h;
        }
        g.noScissor();
        scroll.update(total + 6, bottom - top);
    }

    private List<FormattedCharSequence> captionLines(Post p) {
        Component c = Component.literal(p.name + " ").withStyle(ChatFormatting.BOLD).append(Component.literal(p.caption).withStyle(ChatFormatting.RESET));
        return font().split(c, s.dw() - 8);
    }

    private int postHeight(Post p) {
        int lines = p.caption.isEmpty() ? 0 : Math.min(4, captionLines(p).size());
        return 16 + s.dw() + 14 + lines * 9 + 10 + 6;
    }

    private void drawPost(G g, Post p, int y, int mx, int my, int clipTop, int clipBottom) {
        Font f = font();
        int x0 = s.dx0, w = s.dw();
        boolean inside = my >= clipTop && my < clipBottom;
        // cabecera
        D.faceRound(g, p.author, x0 + 4, y + 3, 10, 0xFFD62976);
        g.drawString(f, Component.literal(D.cut(f, p.name, w - 60)).withStyle(ChatFormatting.BOLD), x0 + 18, y + 4, s.fg(), false);
        if (p.mine) {
            boolean ht = inside && s.in(mx, my, x0 + w - 14, y + 2, x0 + w - 2, y + 14);
            D.icon(g, D.TRASH, x0 + w - 13, y + 3, 10, confirmDelete && single == p ? 0xFFFF453A : ht ? 0xFFFF453A : s.sub());
        }
        y += 16;
        // foto
        ResourceLocation t = Photos.remote(p.id);
        if (t != null) {
            g.blit(t, x0, y, w, w, 0, 0, Photos.SIZE, Photos.SIZE, Photos.SIZE, Photos.SIZE);
        } else {
            g.fill(x0, y, x0 + w, y + w, s.card());
            spinner(g, x0 + w / 2, y + w / 2, s.sub());
        }
        // corazón grande del doble clic
        long ha = System.currentTimeMillis() - heartAt;
        if (heartId == p.id && ha < 800) {
            float k = ha < 200 ? D.easeBack(ha / 200F) : 1;
            float a = ha > 550 ? 1 - (ha - 550) / 250F : 1;
            int hs = (int) (w * 0.38F * k);
            D.icon(g, D.HEART_FULL, x0 + w / 2 - hs / 2, y + w / 2 - hs / 2, hs, D.alpha(0xFFFFFFFF, a));
        }
        y += w;
        // corazón y me gusta
        boolean hh = inside && s.in(mx, my, x0 + 2, y + 1, x0 + 16, y + 13);
        int hc = p.liked ? 0xFFFF3040 : hh ? 0xFFFF6070 : s.fg();
        D.icon(g, p.liked ? D.HEART_FULL : D.HEART, x0 + 4, y + 2, 11, hc);
        Component likes = Component.translatable(p.likes == 1 ? "celular.insta.like_one" : "celular.insta.likes", p.likes).withStyle(ChatFormatting.BOLD);
        g.drawString(f, likes, x0 + 19, y + 4, s.fg(), false);
        y += 14;
        // texto
        if (!p.caption.isEmpty()) {
            List<FormattedCharSequence> lines = captionLines(p);
            for (int i = 0; i < Math.min(4, lines.size()); i++) {
                g.drawString(f, lines.get(i), x0 + 4, y, s.fg(), false);
                y += 9;
            }
        }
        g.push();
        g.translate(x0 + 4, y + 1, 0);
        g.scale(0.75F, 0.75F, 1);
        g.drawString(f, D.ago(p.time), 0, 0, s.sub(), false);
        g.pop();
    }

    /** ¿Qué hay en (x, y) dentro de una publicación que empieza en py? 0 nada, 1 cara/nombre, 2 foto, 3 corazón, 4 borrar. */
    private int hit(Post p, int py, double x, double y) {
        int x0 = s.dx0, w = s.dw();
        if (s.in(x, y, x0, py, x0 + w - 16, py + 16)) {
            return 1;
        }
        if (p.mine && s.in(x, y, x0 + w - 16, py, x0 + w, py + 16)) {
            return 4;
        }
        if (s.in(x, y, x0, py + 16, x0 + w, py + 16 + w)) {
            return 2;
        }
        if (s.in(x, y, x0, py + 16 + w, x0 + 18, py + 16 + w + 14)) {
            return 3;
        }
        return 0;
    }

    private void drawProfile(G g, int mx, int my) {
        Font f = font();
        boolean mine = me().equals(profileOf);
        int top = header(g, "celular.insta.profile", !mine, mx, my);
        int bottom = tabTop();
        List<Post> posts = ClientHooks.profileLoaded && profileOf != null && profileOf.equals(ClientHooks.profileId) ? ClientHooks.profile : null;
        int c = (s.dw() - 2) / 3;
        int headerH = 46;
        int rows = posts == null ? 0 : (posts.size() + 2) / 3;
        scroll.update(headerH + rows * (c + 1) + 6, bottom - top);
        g.scissor(s.dx0, top, s.dx1, bottom);
        int y = top - scroll.off();
        D.faceRound(g, profileOf, s.dx0 + 8, y + 6, 22, 0xFFD62976);
        g.drawString(f, Component.literal(D.cut(f, profileName, s.dw() - 44)).withStyle(ChatFormatting.BOLD), s.dx0 + 36, y + 7, s.fg(), false);
        if (posts != null) {
            int likes = 0;
            for (Post p : posts) {
                likes += p.likes;
            }
            g.push();
            g.translate(s.dx0 + 36, y + 19, 0);
            g.scale(0.75F, 0.75F, 1);
            g.drawString(f, Component.translatable("celular.insta.stats", posts.size(), likes), 0, 0, s.sub(), false);
            g.pop();
        }
        g.fill(s.dx0, y + headerH - 6, s.dx1, y + headerH - 5, s.line());
        y += headerH;
        if (posts == null) {
            spinner(g, (s.dx0 + s.dx1) / 2, y + 20, s.fg());
        } else if (posts.isEmpty()) {
            centered(g, Component.translatable(mine ? "celular.insta.no_posts_mine" : "celular.insta.no_posts"), y + 14, s.sub());
        } else {
            for (int i = 0; i < posts.size(); i++) {
                int x = s.dx0 + (i % 3) * (c + 1), py = y + (i / 3) * (c + 1);
                if (py + c < top || py > bottom) {
                    continue;
                }
                ResourceLocation t = Photos.remote(posts.get(i).id);
                if (t != null) {
                    g.blit(t, x, py, c, c, 0, 0, Photos.SIZE, Photos.SIZE, Photos.SIZE, Photos.SIZE);
                } else {
                    g.fill(x, py, x + c, py + c, s.card());
                }
                if (s.in(mx, my, x, py, x + c, py + c) && my >= top && my < bottom) {
                    g.fill(x, py, x + c, py + c, 0x30FFFFFF);
                }
            }
        }
        g.noScissor();
    }

    private void drawSingle(G g, int mx, int my) {
        int top = header(g, "celular.insta.post", true, mx, my);
        int bottom = tabTop();
        Post p = fresh(single);
        if (p == null) {
            go(FEED);
            return;
        }
        single = p;
        scroll.update(postHeight(p) + 4, bottom - top);
        g.scissor(s.dx0, top, s.dx1, bottom);
        drawPost(g, p, top - scroll.off(), mx, my, top, bottom);
        g.noScissor();
        if (confirmDelete) {
            Component sure = Component.translatable("celular.insta.delete_sure");
            int w = font().width(sure) + 12;
            int cx = (s.dx0 + s.dx1) / 2;
            D.rr(g, cx - w / 2, top + 20, cx + w / 2, top + 34, 7, 0xF0D03030);
            g.drawString(font(), sure, cx - font().width(sure) / 2, top + 23, 0xFFFFFFFF, false);
        }
    }

    /** La versión más nueva de una publicación (por si cambió su número de me gusta). */
    private Post fresh(Post p) {
        if (p == null) {
            return null;
        }
        for (Post q : ClientHooks.feed) {
            if (q.id == p.id) {
                return q;
            }
        }
        for (Post q : ClientHooks.profile) {
            if (q.id == p.id) {
                return q;
            }
        }
        return p;
    }

    // ================================================================== publicar

    void compose(Path p) {
        BufferedImage img = Photos.read(p);
        if (img == null) {
            return;
        }
        composePath = p;
        composeImg = img;
        filter = 0;
        captionText = "";
        uploading = false;
        go(COMPOSE);
        preview = Photos.image("preview", img);
        BufferedImage small = Photos.shrink(img, 48);
        for (int i = 0; i < Photos.FILTERS.length; i++) {
            filterThumbs[i] = Photos.image("filter" + i, Photos.filter(small, i));
        }
        placeCaption();
    }

    private int previewSize() {
        int avail = s.cy1 - s.cy0 - 19 - 4 - 2 * filterRowH() - 20 - 22 - 6;
        return Math.max(50, Math.min(s.dw() - 24, avail));
    }

    private int filterRowH() {
        return (s.dw() - 10) / 4 + 9;
    }

    private int captionY() {
        return s.cy0 + 19 + 4 + previewSize() + 4 + 2 * filterRowH() + 2;
    }

    private void placeCaption() {
        caption = s.box(s.dx0 + 8, captionY() + 4, s.dw() - 16, 10, "celular.insta.caption_hint", 150);
        caption.setValue(captionText);
        caption.setHint(Component.translatable("celular.insta.caption_hint").withStyle(ChatFormatting.GRAY));
    }

    private void drawCompose(G g, int mx, int my) {
        Font f = font();
        int top = s.cy0;
        g.fill(s.dx0, top, s.dx1, top + 18, s.bg());
        boolean hb = s.in(mx, my, s.dx0, top, s.dx0 + 18, top + 18);
        D.icon(g, D.CLOSE, s.dx0 + 4, top + 4, 10, hb ? 0xFFFF453A : s.fg());
        Component title = Component.translatable("celular.insta.new_post").withStyle(ChatFormatting.BOLD);
        g.drawString(f, title, (s.dx0 + s.dx1) / 2 - f.width(title) / 2, top + 5, s.fg(), false);
        g.fill(s.dx0, top + 18, s.dx1, top + 19, s.line());
        int ps = previewSize();
        int y = top + 23;
        int px = (s.dx0 + s.dx1) / 2 - ps / 2;
        if (preview != null) {
            g.blit(preview, px, y, ps, ps, 0, 0, Photos.SIZE, Photos.SIZE, Photos.SIZE, Photos.SIZE);
        }
        y += ps + 4;
        // filtros (2 filas de 4)
        int cell = (s.dw() - 10) / 4;
        for (int i = 0; i < Photos.FILTERS.length; i++) {
            int fx = s.dx0 + 5 + (i % 4) * cell, fy = y + (i / 4) * filterRowH();
            int ts = cell - 6;
            boolean sel = filter == i;
            boolean h = s.in(mx, my, fx, fy, fx + cell, fy + filterRowH());
            if (sel) {
                D.rr(g, fx + 1, fy - 1, fx + ts + 5, fy + ts + 3, 3, PINK);
            }
            if (filterThumbs[i] != null) {
                g.blit(filterThumbs[i], fx + 3, fy + 1, ts, ts, 0, 0, 48, 48, 48, 48);
            }
            if (h && !sel) {
                g.fill(fx + 3, fy + 1, fx + 3 + ts, fy + 1 + ts, 0x30FFFFFF);
            }
            Component n = Component.translatable("celular.filter." + Photos.FILTERS[i]);
            g.push();
            g.translate(fx + 3 + ts / 2F, fy + ts + 3, 0);
            g.scale(0.6F, 0.6F, 1);
            g.drawString(f, n, -f.width(n) / 2, 0, sel ? s.fg() : s.sub(), false);
            g.pop();
        }
        // caja del texto
        int cy = captionY();
        D.rr(g, s.dx0 + 4, cy, s.dx1 - 4, cy + 16, 5, s.card());
        if (caption != null) {
            caption.setTextColor(s.fg());
        }
        // compartir
        int by = cy + 20;
        if (uploading) {
            long el = System.currentTimeMillis() - uploadAt;
            if (ClientHooks.uploadResult == 1) {
                uploading = false;
                composeImg = null;
                go(FEED);
                ClientHooks.feedLoaded = false;
                refresh();
                ClientHooks.play(CelularReg.LIKE.get(), 1.2F, 0.5F);
                return;
            }
            if (ClientHooks.uploadResult == 2 || el > 15000) {
                uploading = false;
            }
            D.rr(g, s.dx0 + 6, by, s.dx1 - 6, by + 16, 8, 0xFF3A3A3C);
            spinner(g, s.dx0 + 18, by + 8, 0xFFFFFFFF);
            Component t = Component.translatable("celular.insta.posting");
            g.drawString(f, t, (s.dx0 + s.dx1) / 2 - f.width(t) / 2 + 6, by + 4, 0xFFFFFFFF, false);
        } else {
            button(g, s.dx0 + 6, by, s.dx1 - 6, by + 16, Component.translatable("celular.insta.share"), PINK, mx, my);
        }
    }

    private void share() {
        if (uploading || composeImg == null) {
            return;
        }
        BufferedImage img = Photos.filter(composeImg, filter);
        byte[] data = Photos.jpeg(img, 0.85F);
        if (data != null && data.length > CelularNet.MAX_PHOTO) {
            data = Photos.jpeg(img, 0.6F);
        }
        if (data == null || data.length > CelularNet.MAX_PHOTO) {
            return;
        }
        String text = caption != null ? caption.getValue() : "";
        int total = (data.length + CelularNet.CHUNK - 1) / CelularNet.CHUNK;
        int session = (int) (Math.random() * Integer.MAX_VALUE);
        for (int i = 0; i < total; i++) {
            int from = i * CelularNet.CHUNK, len = Math.min(CelularNet.CHUNK, data.length - from);
            byte[] part = new byte[len];
            System.arraycopy(data, from, part, 0, len);
            Net.toServer(new CelularNet.Upload(session, i, total, i == 0 ? text : "", part));
        }
        ClientHooks.uploadResult = 0;
        uploading = true;
        uploadAt = System.currentTimeMillis();
        ClientHooks.tap();
    }

    // ================================================================== entrada

    @Override
    boolean click(double x, double y, int b) {
        if (page == COMPOSE) {
            return clickCompose(x, y);
        }
        int tt = tabTop();
        if (y >= tt) {
            int i = (int) ((x - s.dx0) / (s.dw() / 3));
            ClientHooks.tap();
            if (i == 0) {
                go(FEED);
                refresh();
            } else if (i == 1) {
                pick();
            } else {
                openProfile(me(), myName());
            }
            return true;
        }
        if (page == FEED) {
            int top = s.cy0;
            if (s.in(x, y, s.dx1 - 18, top, s.dx1, top + 18)) {
                ClientHooks.tap();
                ClientHooks.feedLoaded = false;
                refresh();
                return true;
            }
            top += 19;
            if (ClientHooks.feedLoaded && ClientHooks.feed.isEmpty()) {
                if (s.in(x, y, s.dx0 + 16, top + 92, s.dx1 - 16, top + 108)) {
                    pick();
                }
                return true;
            }
            if (y < top) {
                return true;
            }
            int py = top - scroll.off();
            for (Post p : ClientHooks.feed) {
                int h = postHeight(p);
                if (y >= py && y < py + h) {
                    clickPost(p, py, x, y);
                    return true;
                }
                py += h;
            }
            return true;
        }
        if (page == PROFILE) {
            boolean mine = me().equals(profileOf);
            if (!mine && headerArrow(x, y)) {
                back();
                return true;
            }
            int top = s.cy0 + 19;
            List<Post> posts = ClientHooks.profileLoaded ? ClientHooks.profile : null;
            if (posts == null || y < top) {
                return true;
            }
            int c = (s.dw() - 2) / 3;
            int gy = top - scroll.off() + 46;
            for (int i = 0; i < posts.size(); i++) {
                int px = s.dx0 + (i % 3) * (c + 1), py = gy + (i / 3) * (c + 1);
                if (s.in(x, y, px, py, px + c, py + c)) {
                    ClientHooks.tap();
                    single = posts.get(i);
                    postFrom = PROFILE;
                    go(POST);
                    return true;
                }
            }
            return true;
        }
        if (page == POST) {
            if (headerArrow(x, y)) {
                back();
                return true;
            }
            if (single != null && y >= s.cy0 + 19) {
                clickPost(single, s.cy0 + 19 - scroll.off(), x, y);
            }
            return true;
        }
        return false;
    }

    private void clickPost(Post p, int py, double x, double y) {
        int what = hit(p, py, x, y);
        long now = System.currentTimeMillis();
        switch (what) {
            case 1 -> {
                ClientHooks.tap();
                openProfile(p.author, p.name);
            }
            case 2 -> {
                if (lastTapId == p.id && now - lastTapAt < 350) {
                    heartId = p.id;
                    heartAt = now;
                    if (!p.liked) {
                        like(p);
                    }
                    lastTapId = -1;
                } else {
                    lastTapId = p.id;
                    lastTapAt = now;
                }
            }
            case 3 -> like(p);
            case 4 -> {
                if (single != p || !confirmDelete) {
                    single = p;
                    if (page != POST) {
                        postFrom = page;
                        go(POST);
                    }
                    confirmDelete = true;
                } else {
                    Net.toServer(new CelularNet.Delete(p.id));
                    ClientHooks.feed.removeIf(q -> q.id == p.id);
                    ClientHooks.profile.removeIf(q -> q.id == p.id);
                    confirmDelete = false;
                    go(FEED);
                }
                ClientHooks.tap();
            }
            default -> confirmDelete = false;
        }
    }

    private void like(Post p) {
        ClientHooks.likeLocal(p);
        Net.toServer(new CelularNet.Like(p.id));
        if (!p.liked) {
            ClientHooks.play(CelularReg.LIKE.get(), 1.0F, 0.6F);
        }
    }

    private void pick() {
        ClientHooks.tap();
        GalleryApp ga = (GalleryApp) s.switchTo("gallery");
        if (ga != null) {
            ga.picking = true;
        }
    }

    private boolean clickCompose(double x, double y) {
        int top = s.cy0;
        if (s.in(x, y, s.dx0, top, s.dx0 + 18, top + 18)) {
            back();
            return true;
        }
        int y0 = top + 23 + previewSize() + 4;
        int cell = (s.dw() - 10) / 4;
        for (int i = 0; i < Photos.FILTERS.length; i++) {
            int fx = s.dx0 + 5 + (i % 4) * cell, fy = y0 + (i / 4) * filterRowH();
            if (s.in(x, y, fx, fy, fx + cell, fy + filterRowH())) {
                if (filter != i) {
                    filter = i;
                    preview = Photos.image("preview", Photos.filter(composeImg, i));
                    ClientHooks.tap();
                }
                return true;
            }
        }
        int by = captionY() + 20;
        if (s.in(x, y, s.dx0 + 6, by, s.dx1 - 6, by + 16)) {
            share();
            return true;
        }
        return true;
    }

    @Override
    boolean key(int key) {
        if (page == COMPOSE && (key == 257 || key == 335)) {
            share();
            return true;
        }
        return false;
    }

    @Override
    boolean drag(double x, double y, double dx, double dy) {
        if (page != COMPOSE) {
            scroll.drag(dy);
        }
        return true;
    }

    @Override
    boolean scroll(double x, double y, double d) {
        if (page != COMPOSE) {
            scroll.wheel(d);
        }
        return true;
    }

    @Override
    boolean back() {
        switch (page) {
            case COMPOSE -> {
                composeImg = null;
                go(FEED);
                refresh();
                return true;
            }
            case POST -> {
                go(postFrom);
                return true;
            }
            case PROFILE -> {
                go(FEED);
                refresh();
                return true;
            }
            default -> {
                return false;
            }
        }
    }
}
