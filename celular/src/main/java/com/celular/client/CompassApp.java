package com.celular.client;

import com.celular.plat.client.G;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Brújula y GPS: rosa de los vientos que gira contigo, tus coordenadas, y lugares guardados (tu casa, una mina...) con la distancia y
 * una flecha que apunta hacia ellos. También recuerda dónde moriste por última vez. Los lugares se guardan en la computadora, por
 * mundo o servidor ({@code .minecraft/celular/lugares.properties}).
 */
final class CompassApp extends App {
    static final class Place {
        String name;
        double x, y, z;
        String dim;

        Place(String name, double x, double y, double z, String dim) {
            this.name = name;
            this.x = x;
            this.y = y;
            this.z = z;
            this.dim = dim;
        }
    }

    private static final List<Place> places = new ArrayList<>();
    private static String loadedFor = "";
    private static Place death;
    private static boolean wasDead;
    /** Lugar al que apunta la flecha grande (null = solo norte). */
    private static Place target;
    private final Scroll scroll = new Scroll();
    private boolean naming;
    private Place confirmDelete;

    CompassApp(PhoneScreen s) {
        super(s, D.APP_COMPASS, "compass");
    }

    @Override
    int bg() {
        return 0xFF0E0F13;
    }

    @Override
    boolean lightStatus() {
        return false;
    }

    // ------------------------------------------------------------------ guardar

    private static String worldKey() {
        Minecraft mc = Minecraft.getInstance();
        try {
            if (mc.getSingleplayerServer() != null) {
                return "un:" + mc.getSingleplayerServer().getWorldData().getLevelName();
            }
            if (mc.getCurrentServer() != null) {
                return "sv:" + mc.getCurrentServer().ip;
            }
        } catch (Exception ignored) {
        }
        return "otro";
    }

    private static Path file() {
        return FMLPaths.GAMEDIR.get().resolve("celular").resolve("lugares.properties");
    }

    private static String dim() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return "";
        }
        String k = mc.level.dimension().toString();
        int a = k.lastIndexOf(" / "), b = k.lastIndexOf(']');
        return a >= 0 && b > a ? k.substring(a + 3, b) : k;
    }

    private static void load() {
        String key = worldKey();
        if (key.equals(loadedFor)) {
            return;
        }
        loadedFor = key;
        places.clear();
        death = null;
        target = null;
        Properties p = new Properties();
        if (Files.exists(file())) {
            try (Reader r = Files.newBufferedReader(file(), StandardCharsets.UTF_8)) {
                p.load(r);
            } catch (IOException ignored) {
            }
        }
        String prefix = key.replace(' ', '_').replace('=', '_').replace(':', '_') + ".";
        for (int i = 0; i < 200; i++) {
            String v = p.getProperty(prefix + i);
            if (v == null) {
                break;
            }
            Place pl = parse(v);
            if (pl != null) {
                places.add(pl);
            }
        }
        String d = p.getProperty(prefix + "muerte");
        death = d == null ? null : parse(d);
    }

    private static Place parse(String v) {
        String[] a = v.split("\\|", 5);
        if (a.length < 5) {
            return null;
        }
        try {
            return new Place(a[4], Double.parseDouble(a[0]), Double.parseDouble(a[1]), Double.parseDouble(a[2]), a[3]);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String str(Place p) {
        return Math.round(p.x) + "|" + Math.round(p.y) + "|" + Math.round(p.z) + "|" + p.dim + "|" + p.name;
    }

    private static void save() {
        Properties p = new Properties();
        if (Files.exists(file())) {
            try (Reader r = Files.newBufferedReader(file(), StandardCharsets.UTF_8)) {
                p.load(r);
            } catch (IOException ignored) {
            }
        }
        String prefix = loadedFor.replace(' ', '_').replace('=', '_').replace(':', '_') + ".";
        p.keySet().removeIf(k -> k.toString().startsWith(prefix));
        for (int i = 0; i < places.size(); i++) {
            p.setProperty(prefix + i, str(places.get(i)));
        }
        if (death != null) {
            p.setProperty(prefix + "muerte", str(death));
        }
        try {
            Files.createDirectories(file().getParent());
            try (Writer w = Files.newBufferedWriter(file(), StandardCharsets.UTF_8)) {
                p.store(w, "Lugares guardados del celular (por mundo)");
            }
        } catch (IOException ignored) {
        }
    }

    /** Cada cuadro: si el jugador murió, recuerda dónde. */
    static void watchDeath() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }
        boolean dead = mc.player.isDeadOrDying();
        if (dead && !wasDead) {
            load();
            death = new Place("", mc.player.getX(), mc.player.getY(), mc.player.getZ(), dim());
            save();
        }
        wasDead = dead;
    }

    @Override
    void enter() {
        load();
        naming = false;
        confirmDelete = null;
        scroll.reset();
    }

    @Override
    void leave() {
        naming = false;
    }

    // ------------------------------------------------------------------ dibujo

    private static String dir8(float yaw) {
        String[] n = Component.translatable("celular.compass.dirs").getString().split(",");
        int i = Math.floorMod(Math.round(yaw / 45F), 8);
        return n.length == 8 ? n[i] : "?";
    }

    /** Ángulo (grados, 0 = de frente) hacia un lugar, según hacia dónde mira el jugador. */
    private static float bearing(Place p) {
        Minecraft mc = Minecraft.getInstance();
        double dx = p.x - mc.player.getX(), dz = p.z - mc.player.getZ();
        float to = (float) Math.toDegrees(Math.atan2(-dx, dz));
        return net.minecraft.util.Mth.wrapDegrees(to - mc.player.getYRot());
    }

    private static int dist(Place p) {
        Minecraft mc = Minecraft.getInstance();
        return (int) Math.round(Math.sqrt(Math.pow(p.x - mc.player.getX(), 2) + Math.pow(p.z - mc.player.getZ(), 2)));
    }

    private int roseR() {
        return Math.min(s.dw() / 2 - 14, (s.cy1 - s.cy0) / 5);
    }

    @Override
    void draw(G g, int mx, int my, float pt) {
        Font f = font();
        Minecraft mc = Minecraft.getInstance();
        g.fill(s.dx0, s.dy0, s.dx1, s.dy1, 0xFF0E0F13);
        if (mc.player == null) {
            return;
        }
        load();
        int cx = (s.dx0 + s.dx1) / 2;
        float yaw = mc.player.getYRot();
        int r = roseR();
        int cy = s.cy0 + 14 + r;
        // rosa de los vientos (gira al revés de hacia donde miras)
        D.circle(g, cx, cy, r + 3, 0xFF22252E);
        D.circle(g, cx, cy, r, 0xFF15171D);
        double rot = Math.toRadians(-yaw + 180);
        for (int i = 0; i < 72; i++) {
            double a = rot + i * Math.PI / 36;
            int len = i % 18 == 0 ? 6 : i % 2 == 0 ? 3 : 2;
            for (int k = 0; k < len; k++) {
                int px = cx + (int) Math.round(Math.sin(a) * (r - 2 - k)), py = cy - (int) Math.round(Math.cos(a) * (r - 2 - k));
                g.fill(px, py, px + 1, py + 1, i % 18 == 0 ? 0xFFFFFFFF : 0xFF8E8E93);
            }
        }
        String[] card = Component.translatable("celular.compass.card").getString().split(",");
        for (int i = 0; i < Math.min(4, card.length); i++) {
            double a = rot + i * Math.PI / 2;
            int px = cx + (int) Math.round(Math.sin(a) * (r - 13)), py = cy - (int) Math.round(Math.cos(a) * (r - 13));
            g.drawString(f, card[i], px - f.width(card[i]) / 2, py - 4, i == 0 ? 0xFFFF453A : 0xFFFFFFFF, false);
        }
        // flecha hacia el lugar elegido (o la aguja del norte)
        double aim = target != null ? Math.toRadians(bearing(target)) : rot;
        int col = target != null ? 0xFF30D158 : 0xFFFF453A;
        for (int k = 0; k < r - 18; k++) {
            int w = Math.max(0, (r - 18 - k) / 6);
            int px = cx + (int) Math.round(Math.sin(aim) * k), py = cy - (int) Math.round(Math.cos(aim) * k);
            g.fill(px - w, py - w, px + w + 1, py + w + 1, col);
        }
        D.circle(g, cx, cy, 3, 0xFFFFFFFF);
        // rayita de "al frente"
        g.fill(cx - 1, cy - r - 6, cx + 1, cy - r - 1, 0xFFFFFFFF);
        int y = cy + r + 8;
        // hacia dónde miras y coordenadas
        String head = dir8(yaw) + "  " + Math.floorMod(Math.round(yaw + 180), 360) + "°";
        g.drawString(f, Component.literal(head).withStyle(ChatFormatting.BOLD), cx - f.width(head) / 2 - 2, y, 0xFFFFFFFF, false);
        y += 11;
        String xyz = "X " + (int) Math.floor(mc.player.getX()) + "  Y " + (int) Math.floor(mc.player.getY()) + "  Z " + (int) Math.floor(mc.player.getZ());
        small(g, xyz, cx - (int) (f.width(xyz) * 0.75F / 2), y, 0xFFB0B4BE, 0.75F);
        y += 10;
        if (target != null) {
            String t = D.cut(f, (target == death ? Component.translatable("celular.compass.death").getString() : target.name) + " · " + dist(target) + " m", s.dw() - 10);
            small(g, t, cx - (int) (f.width(t) * 0.75F / 2), y, 0xFF30D158, 0.75F);
        }
        y += 12;
        // botón guardar aquí
        int x0 = s.dx0 + 6, x1 = s.dx1 - 6;
        if (!naming) {
            button(g, x0, y, x1, y + 15, Component.translatable("celular.compass.save_here"), 0xFF0A84FF, mx, my);
        } else {
            D.rr(g, x0, y, x1, y + 15, 7, 0xFF2C2C2E);
        }
        y += 19;
        // lista de lugares
        List<Place> list = new ArrayList<>();
        if (death != null) {
            list.add(death);
        }
        list.addAll(places);
        int rowH = 20, top = y, bottom = s.cy1 - 2;
        scroll.update(list.size() * rowH, bottom - top);
        g.scissor(s.dx0, top, s.dx1, bottom);
        if (list.isEmpty()) {
            centered(g, Component.translatable("celular.compass.empty"), top + 6, 0xFF8E8E93);
        }
        for (int i = 0; i < list.size(); i++) {
            Place p = list.get(i);
            int ry = top + i * rowH - scroll.off();
            if (ry + rowH < top || ry > bottom) {
                continue;
            }
            boolean sel = p == target;
            D.rr(g, x0, ry, x1, ry + rowH - 2, 6, sel ? 0xFF1F3B2A : 0xFF1C1C1E);
            boolean isDeath = p == death;
            D.icon(g, isDeath ? D.SKULL : D.PIN, x0 + 4, ry + 4, 10, isDeath ? 0xFFFF453A : 0xFF0A84FF);
            String name = isDeath ? Component.translatable("celular.compass.death").getString() : p.name;
            boolean here = p.dim.equals(dim());
            g.drawString(f, D.cut(f, name, x1 - x0 - 50), x0 + 18, ry + 2, 0xFFFFFFFF, false);
            String info = here ? dist(p) + " m · " + Math.round(p.x) + ", " + Math.round(p.z) : Component.translatable("celular.compass.other_dim").getString();
            small(g, D.cut(f, info, (int) ((x1 - x0 - 50) / 0.65F)), x0 + 18, ry + 11, 0xFF8E8E93, 0.65F);
            if (here) {
                // flechita hacia el lugar
                double a = Math.toRadians(bearing(p));
                int ax = x1 - 22, ay = ry + 9;
                for (int k = -4; k <= 4; k++) {
                    int px = ax + (int) Math.round(Math.sin(a) * k), py = ay - (int) Math.round(Math.cos(a) * k);
                    g.fill(px, py, px + 1 + (k > 2 ? 1 : 0), py + 1 + (k > 2 ? 1 : 0), sel ? 0xFF30D158 : 0xFFFFFFFF);
                }
            }
            boolean conf = confirmDelete == p;
            D.icon(g, D.TRASH, x1 - 12, ry + 4, 10, conf ? 0xFFFF453A : 0xFF6E6E73);
        }
        g.noScissor();
    }

    // ------------------------------------------------------------------ entrada

    private int saveY() {
        int r = roseR();
        return s.cy0 + 14 + r + r + 8 + 11 + 10 + 12;
    }

    @Override
    boolean click(double x, double y, int b) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return true;
        }
        int x0 = s.dx0 + 6, x1 = s.dx1 - 6;
        int sy = saveY();
        if (!naming && s.in(x, y, x0, sy, x1, sy + 15)) {
            ClientHooks.tap();
            naming = true;
            var box = s.box(x0 + 6, sy + 4, x1 - x0 - 30, 10, "celular.compass.name_hint", 24);
            box.setValue(Component.translatable("celular.compass.default_name", places.size() + 1).getString());
            return true;
        }
        List<Place> list = new ArrayList<>();
        if (death != null) {
            list.add(death);
        }
        list.addAll(places);
        int top = sy + 19, bottom = s.cy1 - 2;
        if (y >= top && y < bottom) {
            int i = (int) ((y - top + scroll.off()) / 20);
            if (i >= 0 && i < list.size()) {
                Place p = list.get(i);
                ClientHooks.tap();
                if (x >= x1 - 14) {
                    if (confirmDelete == p) {
                        if (p == death) {
                            death = null;
                        } else {
                            places.remove(p);
                        }
                        if (target == p) {
                            target = null;
                        }
                        confirmDelete = null;
                        save();
                    } else {
                        confirmDelete = p;
                    }
                } else {
                    target = target == p ? null : p;
                    confirmDelete = null;
                }
            }
        }
        return true;
    }

    @Override
    boolean key(int key) {
        if (naming && (key == 257 || key == 335)) {          // Enter: guardar
            Minecraft mc = Minecraft.getInstance();
            String name = s.boxValue().trim();
            if (name.isEmpty()) {
                name = Component.translatable("celular.compass.default_name", places.size() + 1).getString();
            }
            Place p = new Place(name, mc.player.getX(), mc.player.getY(), mc.player.getZ(), dim());
            places.add(p);
            target = p;
            save();
            naming = false;
            s.removeBox();
            ClientHooks.play(com.celular.CelularReg.LIKE.get(), 1.2F, 0.5F);
            return true;
        }
        return false;
    }

    @Override
    boolean scroll(double x, double y, double d) {
        scroll.wheel(d);
        return true;
    }

    @Override
    boolean drag(double x, double y, double dx, double dy) {
        scroll.drag(dy);
        return true;
    }

    @Override
    boolean back() {
        if (naming) {
            naming = false;
            s.removeBox();
            return true;
        }
        return false;
    }
}
