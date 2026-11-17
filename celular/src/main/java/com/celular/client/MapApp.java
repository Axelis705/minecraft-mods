package com.celular.client;

import com.celular.net.AppNet;
import com.celular.plat.Ids;
import com.celular.plat.Net;
import com.celular.plat.Plat;
import com.celular.plat.client.G;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.MapColor;

import java.util.UUID;

/**
 * Mapa con amigos: un mapa de verdad de lo que hay alrededor (se dibuja con los colores de los bloques, como los mapas del juego) y
 * encima tú (la flecha) y los amigos que comparten su ubicación, aunque estén lejos. Abajo, la lista con la distancia y hacia dónde.
 */
final class MapApp extends App {
    /** Píxeles del mapa por lado. */
    private static final int N = 128;
    private static final int[] ZOOMS = {1, 2, 4};
    private static int zoomIdx;
    private static ResourceLocation tex;
    private static int shownX, shownZ, shownZoom;
    private static NativeImage work;
    private static int workRow, workX, workZ, workZoom;
    private static int[] prevH;
    private static long lastFull;
    private static AppNet.Friends friends;
    private long askedAt;
    private final Scroll scroll = new Scroll();

    MapApp(PhoneScreen s) {
        super(s, D.APP_MAP, "map");
    }

    static void friends(AppNet.Friends m) {
        friends = m;
    }

    @Override
    void enter() {
        scroll.reset();
        ask();
    }

    private void ask() {
        askedAt = System.currentTimeMillis();
        Net.toServer(new AppNet.Req(AppNet.Req.MAP));
    }

    // ------------------------------------------------------------------ dibujar el terreno (unas filas por cuadro)

    private static int zoom() {
        return ZOOMS[zoomIdx];
    }

    private static int shade(int rgb, float k) {
        int r = Math.min(255, (int) ((rgb >> 16 & 255) * k)), g = Math.min(255, (int) ((rgb >> 8 & 255) * k)), b = Math.min(255,
                (int) ((rgb & 255) * k));
        return 0xFF000000 | r << 16 | g << 8 | b;
    }

    /** Altura y color de una columna del mundo (null de color = chunk sin cargar). */
    private static int height(Level level, BlockPos.MutableBlockPos p, int x, int z, boolean ceiling, int playerY) {
        if (!ceiling) {
            return level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) - 1;
        }
        // con techo (Nether): desde un poco arriba del jugador hacia abajo, el primer bloque con aire encima
        int min = Plat.minY(level);
        boolean air = false;
        for (int y = playerY + 12; y > Math.max(min, playerY - 40); y--) {
            p.set(x, y, z);
            boolean a = level.getBlockState(p).isAir();
            if (!a && air) {
                return y;
            }
            air = a;
        }
        return playerY - 1;
    }

    private static int column(Level level, BlockPos.MutableBlockPos p, int x, int z, boolean ceiling, int playerY, int[] out) {
        if (!level.getChunkSource().hasChunk(x >> 4, z >> 4)) {
            out[0] = Integer.MIN_VALUE;
            return 0xFF202024;
        }
        int min = Plat.minY(level);
        int y = height(level, p, x, z, ceiling, playerY);
        p.set(x, y, z);
        BlockState st = level.getBlockState(p);
        int guard = 0;
        while (st.getMapColor(level, p) == MapColor.NONE && y > min && guard++ < 24) {
            y--;
            p.set(x, y, z);
            st = level.getBlockState(p);
        }
        out[0] = y;
        MapColor mc = st.getMapColor(level, p);
        if (mc == MapColor.WATER) {
            int depth = 0;
            while (depth < 12 && y - depth > min) {
                p.set(x, y - depth - 1, z);
                if (level.getBlockState(p).getMapColor(level, p) != MapColor.WATER) {
                    break;
                }
                depth++;
            }
            return shade(mc.col, 1.08F - depth * 0.045F);
        }
        return 0xFF000000 | mc.col;
    }

    private static void step(Minecraft mc) {
        if (mc.level == null || mc.player == null) {
            return;
        }
        long now = System.currentTimeMillis();
        int px = (int) Math.floor(mc.player.getX()), pz = (int) Math.floor(mc.player.getZ());
        if (work == null) {
            boolean moved = Math.abs(px - shownX) > 6 * zoom() || Math.abs(pz - shownZ) > 6 * zoom();
            if (tex != null && !moved && shownZoom == zoom() && now - lastFull < 2500) {
                return;
            }
            work = new NativeImage(N, N, false);
            workRow = 0;
            workX = px;
            workZ = pz;
            workZoom = zoom();
            prevH = null;
        }
        Level level = mc.level;
        boolean ceiling = level.dimensionType().hasCeiling();
        int playerY = (int) Math.floor(mc.player.getY());
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        int[] h = new int[1];
        int rows = 12;
        for (int r = 0; r < rows && workRow < N; r++, workRow++) {
            int[] heights = new int[N];
            int wz = workZ + (workRow - N / 2) * workZoom;
            for (int c = 0; c < N; c++) {
                int wx = workX + (c - N / 2) * workZoom;
                int col = column(level, p, wx, wz, ceiling, playerY, h);
                heights[c] = h[0];
                // relieve: más claro si sube respecto a la fila de arriba (norte), más oscuro si baja
                if (prevH != null && h[0] != Integer.MIN_VALUE && prevH[c] != Integer.MIN_VALUE) {
                    int d = h[0] - prevH[c];
                    col = shade(col, d > 0 ? 1.12F : d < 0 ? 0.82F : 1.0F);
                }
                Tex.set(work, c, workRow, col);
            }
            prevH = heights;
        }
        if (workRow >= N) {
            tex = Tex.upload(Ids.mod("mapa_amigos"), work);
            work = null;
            shownX = workX;
            shownZ = workZ;
            shownZoom = workZoom;
            lastFull = now;
        }
    }

    // ------------------------------------------------------------------ dibujo

    private static String dir(double dx, double dz) {
        double a = Math.toDegrees(Math.atan2(dx, -dz));          // 0 = norte, 90 = este
        String[] k = {"n", "ne", "e", "se", "s", "sw", "w", "nw"};
        return Component.translatable("celular.map.dir_" + k[(int) Math.floorMod(Math.round(a / 45.0), 8)]).getString();
    }

    @Override
    void draw(G g, int mx, int my, float pt) {
        hits.clear();
        Minecraft mc = Minecraft.getInstance();
        Font f = font();
        if (System.currentTimeMillis() - askedAt > 1000) {
            ask();
        }
        step(mc);
        g.fill(s.dx0, s.dy0, s.dx1, s.dy1, s.bg());
        int top = header(g, "celular.app.map", false, mx, my);
        // compartir ubicación (arriba a la derecha)
        boolean share = friends == null || friends.sharing;
        int sx = s.dx1 - 26, sy = s.cy0 + 4;
        D.rr(g, sx, sy, sx + 20, sy + 11, 5, share ? 0xFF34C759 : (Prefs.dark ? 0xFF39393D : 0xFFD1D1D6));
        D.circle(g, share ? sx + 14 : sx + 6, sy + 5, 4, 0xFFFFFFFF);
        D.icon(g, D.PIN, sx - 12, sy + 1, 9, share ? 0xFF34C759 : s.sub());
        hit(sx - 14, sy - 2, sx + 22, sy + 13, () -> {
            Prefs.shareLocation = !(friends == null || friends.sharing);
            Prefs.save();
            Net.toServer(new AppNet.Req(AppNet.Req.SHARE, null, Prefs.shareLocation ? 1 : 0, ""));
            PhoneHud.toastQuiet(D.APP_MAP, "celular.app.map",
                    Component.translatable(Prefs.shareLocation ? "celular.map.share_on" : "celular.map.share_off"));
        });
        // el mapa
        int size = Math.min(s.dw() - 8, s.cy1 - top - 44);
        int x0 = (s.dx0 + s.dx1) / 2 - size / 2, y0 = top + 4;
        D.rr(g, x0 - 2, y0 - 2, x0 + size + 2, y0 + size + 2, 4, s.line());
        if (tex != null && mc.player != null) {
            g.blit(tex, x0, y0, size, size, 0, 0, N, N, N, N);
        } else {
            g.fill(x0, y0, x0 + size, y0 + size, 0xFF202024);
            spinner(g, x0 + size / 2, y0 + size / 2, 0xFFFFFFFF);
        }
        float k = size / (float) N / Math.max(1, shownZoom);
        double cxw = shownX + 0.5, czw = shownZ + 0.5;
        g.scissor(x0, y0, x0 + size, y0 + size);
        // amigos
        if (friends != null && mc.player != null) {
            for (int i = 0; i < friends.ids.size(); i++) {
                double[] fp = friends.pos.get(i);
                float fx = x0 + size / 2F + (float) ((fp[0] - cxw) * k), fy = y0 + size / 2F + (float) ((fp[2] - czw) * k);
                boolean out = fx < x0 + 4 || fx > x0 + size - 4 || fy < y0 + 4 || fy > y0 + size - 4;
                fx = Math.max(x0 + 4, Math.min(x0 + size - 4, fx));
                fy = Math.max(y0 + 4, Math.min(y0 + size - 4, fy));
                D.faceRound(g, friends.ids.get(i), (int) fx - 4, (int) fy - 4, 8, out ? 0xFFFF9F0A : 0xFFFFFFFF);
            }
        }
        // yo: la flecha que apunta hacia donde miro
        if (mc.player != null) {
            float fx = x0 + size / 2F + (float) ((mc.player.getX() - cxw) * k), fy = y0 + size / 2F + (float) ((mc.player.getZ() - czw) * k);
            g.push();
            g.translate(fx, fy, 0);
            g.rotate((float) Math.toRadians(mc.player.getYRot()) + (float) Math.PI);
            g.fill(-4, 2, 5, 4, 0xFFFFFFFF);
            g.fill(-3, 0, 4, 3, 0xFF0A84FF);
            g.fill(-2, -2, 3, 1, 0xFF0A84FF);
            g.fill(-1, -4, 2, -1, 0xFF0A84FF);
            g.fill(0, -6, 1, -3, 0xFFFFFFFF);
            g.pop();
        }
        g.noScissor();
        // norte y zoom
        small(g, "N", x0 + size / 2 - 2, y0 + 2, 0xFFFFFFFF, 0.7F);
        boolean hz = s.in(mx, my, x0 + size - 16, y0 + size - 16, x0 + size, y0 + size);
        D.rr(g, x0 + size - 15, y0 + size - 15, x0 + size - 2, y0 + size - 2, 4, hz ? 0xC0000000 : 0x90000000);
        String zl = "x" + zoom();
        small(g, zl, x0 + size - 9 - (int) (f.width(zl) * 0.6F / 2), y0 + size - 11, 0xFFFFFFFF, 0.6F);
        hit(x0 + size - 16, y0 + size - 16, x0 + size, y0 + size, () -> {
            zoomIdx = (zoomIdx + 1) % ZOOMS.length;
            work = null;
            lastFull = 0;
        });
        // lista de amigos
        int ly = y0 + size + 6, bottom = s.cy1;
        if (friends == null || friends.ids.isEmpty()) {
            centered(g, Component.translatable(friends == null ? "celular.map.loading" : "celular.map.nobody"), ly + 4, s.sub());
            return;
        }
        int rowH = 14;
        scroll.update(friends.ids.size() * rowH, bottom - ly);
        g.scissor(s.dx0, ly, s.dx1, bottom);
        for (int i = 0; i < friends.ids.size(); i++) {
            int ry = ly + i * rowH - scroll.off();
            if (ry + rowH < ly || ry > bottom || mc.player == null) {
                continue;
            }
            UUID id = friends.ids.get(i);
            double[] fp = friends.pos.get(i);
            double dx = fp[0] - mc.player.getX(), dz = fp[2] - mc.player.getZ();
            int dist = (int) Math.round(Math.sqrt(dx * dx + dz * dz));
            D.faceRound(g, id, s.dx0 + 6, ry + 2, 9, s.line());
            String info = dist + " m · " + dir(dx, dz);
            int iw = (int) (f.width(info) * 0.65F);
            small(g, info, s.dx1 - 5 - iw, ry + 4, s.sub(), 0.65F);
            small(g, D.cut(f, friends.names.get(i), (int) ((s.dw() - 26 - iw) / 0.75F)), s.dx0 + 19, ry + 3, s.fg(), 0.75F);
        }
        g.noScissor();
    }

    @Override
    boolean click(double x, double y, int b) {
        clickHits(x, y);
        return true;
    }

    @Override
    boolean scroll(double x, double y, double d) {
        scroll.wheel(d);
        return true;
    }

    static void reset() {
        friends = null;
        if (work != null) {
            work.close();
        }
        work = null;
        tex = null;
        lastFull = 0;
    }
}
