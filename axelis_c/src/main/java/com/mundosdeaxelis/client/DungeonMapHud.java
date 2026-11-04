package com.mundosdeaxelis.client;

import com.mundosdeaxelis.MundosDeAxelis;
import com.mundosdeaxelis.network.DungeonMapPacket;
import com.mundosdeaxelis.world.DungeonLayout;
import com.mundosdeaxelis.world.ModDimensions;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Minimapa de la dungeon del Rey Brote, arriba a la derecha (debajo de los iconos de efectos). Norte arriba: la arena del jefe queda
 * arriba. El camino principal se ve siempre (apagado hasta pasar por él); las salas laterales solo aparecen como «?» cuando estás
 * al lado de una puerta, y se muestran completas al entrar. Una raya blanca marca la sala actual y un punto, dónde estás.
 */
@Mod.EventBusSubscriber(modid = MundosDeAxelis.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class DungeonMapHud {
    private static final int PITCH = DungeonLayout.PITCH;
    private static final int ANTE_Z = DungeonLayout.ANTE_ZC;
    /** Borde de abajo del mapa y última vez que se dibujó (el panel de avisos se coloca debajo). */
    static int bottom;
    static long lastDraw;
    private static int CS = 13;
    private static int GAP = 5;

    private record Cell(int id, int col, int row, int type, int doors, boolean path, boolean cleared, int shape) {
    }

    private static final Map<Long, Cell> cells = new HashMap<>();
    private static final Set<Integer> visited = new HashSet<>();
    private static boolean on;
    private static int minC, maxC, maxR;

    private DungeonMapHud() {
    }

    private static long key(int col, int row) {
        return ((long) (col + 64) << 32) | (row + 64);
    }

    public static void set(DungeonMapPacket p) {
        if (!p.on) {
            on = false;
            cells.clear();
            visited.clear();
            return;
        }
        boolean fresh = !on || cells.size() != p.cells.size();
        if (fresh) {
            visited.clear();
        }
        on = true;
        cells.clear();
        minC = 0;
        maxC = 0;
        maxR = 0;
        for (int[] c : p.cells) {
            cells.put(key(c[1], c[2]), new Cell(c[0], c[1], c[2], c[3], c[4], c[5] == 1, c[6] == 1, c[7]));
            if (c[5] == 2) {
                visited.add(c[0]); // la dungeon de la Forja avisa qué salas ya se visitaron (aunque las visitara otro jugador)
            }
            minC = Math.min(minC, c[1]);
            maxC = Math.max(maxC, c[1]);
            maxR = Math.max(maxR, c[2]);
        }
    }

    private static int colorOf(int type, boolean cleared) {
        if (cleared && (type == 1 || type == 2 || type == 4)) {
            return 0xFF5F8F6A; // sala de combate despejada
        }
        return switch (type) {
            case 0 -> 0xFF4CAF50; // entrada
            case 1 -> 0xFFC0392B; // combate
            case 2 -> 0xFF9B59B6; // mini-jefe
            case 3 -> 0xFFF1C40F; // antesala
            case 4 -> 0xFF1ABC9C; // llave
            case 5 -> 0xFF3498DB; // cripta
            case 6 -> 0xFFE67E22; // puente de losas
            case 7 -> 0xFFE056FD; // runas
            default -> 0xFF888888;
        };
    }

    private static int dim(int argb) {
        int r = (argb >> 16 & 255) / 2;
        int g = (argb >> 8 & 255) / 2;
        int b = (argb & 255) / 2;
        return 0xFF000000 | r << 16 | g << 8 | b;
    }

    @SubscribeEvent
    public static void onOverlay(RenderGuiOverlayEvent.Post event) {
        if (event.getOverlay() != VanillaGuiOverlay.HOTBAR.type() || !on || cells.isEmpty()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer pl = mc.player;
        if (pl == null || mc.level == null || mc.options.hideGui || !(ModDimensions.isForest(mc.level.dimension()) || ModDimensions.isForge(mc.level.dimension()))) {
            return;
        }
        if (pl.getZ() < 34.0) {
            return; // en la arena del jefe no hace falta
        }
        int pcol = Math.floorDiv((int) Math.floor(pl.getX()) + PITCH / 2, PITCH);
        int prow = Math.floorDiv((int) Math.floor(pl.getZ()) - ANTE_Z + PITCH / 2, PITCH);
        Cell cur = cells.get(key(pcol, prow));
        if (cur != null) {
            visited.add(cur.id());
        }
        GuiGraphics g = event.getGuiGraphics();
        int sw = event.getWindow().getGuiScaledWidth();
        int sh = event.getWindow().getGuiScaledHeight();
        // dungeons largas: si el plano no cabe en la pantalla, las casillas se achican
        int free = sh - 60 - 30;
        CS = 13;
        GAP = 5;
        while (CS > 7 && (maxR + 1) * (CS + GAP) - GAP + 8 > free) {
            CS--;
            GAP = CS >= 10 ? 5 : 4;
        }
        int pitch = CS + GAP;
        int w = (maxC - minC + 1) * pitch - GAP + 8;
        int h = (maxR + 1) * pitch - GAP + 8;
        int rows = 0;
        boolean good = false;
        boolean bad = false;
        for (MobEffectInstance e : pl.getActiveEffects()) {
            if (e.getEffect().isBeneficial()) {
                good = true;
            } else {
                bad = true;
            }
        }
        rows = (good ? 1 : 0) + (bad ? 1 : 0);
        int x0 = sw - w - 6;
        int y0 = 4 + rows * 26;
        bottom = y0 + h + 2;
        lastDraw = System.currentTimeMillis();
        g.fill(x0 - 1, y0 - 1, x0 + w + 1, y0 + h + 1, 0xFF1B1B1B);
        g.fill(x0, y0, x0 + w, y0 + h, 0xB0101418);
        long tick = mc.level.getGameTime();
        // conexiones
        for (Cell c : cells.values()) {
            if (!shown(c)) {
                continue;
            }
            int cx = x0 + 4 + (c.col() - minC) * pitch;
            int cy = y0 + 4 + c.row() * pitch;
            if ((c.doors() & 2) != 0 && shownAt(c.col() + 1, c.row())) {
                g.fill(cx + CS, cy + CS / 2 - 1, cx + CS + GAP, cy + CS / 2 + 1, 0xFF707070);
            }
            if ((c.doors() & 4) != 0 && shownAt(c.col(), c.row() + 1)) {
                g.fill(cx + CS / 2 - 1, cy + CS, cx + CS / 2 + 1, cy + CS + GAP, 0xFF707070);
            }
        }
        // salas
        for (Cell c : cells.values()) {
            if (!shown(c)) {
                continue;
            }
            int cx = x0 + 4 + (c.col() - minC) * pitch;
            int cy = y0 + 4 + c.row() * pitch;
            boolean seen = visited.contains(c.id());
            if (!seen && !c.path()) {
                g.fill(cx, cy, cx + CS, cy + CS, 0xFF3A3A3A);
                g.drawString(mc.font, "?", cx + 4, cy + 2, 0xFFBBBBBB, false);
                continue;
            }
            int col = colorOf(c.type(), c.cleared());
            int fc = seen ? col : dim(col);
            if (c.shape() == 1) {
                g.fill(cx + 3, cy, cx + CS - 3, cy + CS, fc); // sala larga norte-sur
            } else if (c.shape() == 2) {
                g.fill(cx, cy + 3, cx + CS, cy + CS - 3, fc); // sala larga este-oeste
            } else if (c.shape() == 3) {
                int k = Math.max(1, CS / 6);
                g.fill(cx + k, cy, cx + CS - k, cy + CS, fc); // sala redonda / con esquinas cortadas
                g.fill(cx, cy + k, cx + CS, cy + CS - k, fc);
            } else if (c.shape() == 4) {
                int k = Math.max(2, CS / 4);
                g.fill(cx + k, cy, cx + CS - k, cy + CS, fc); // sala en cruz
                g.fill(cx, cy + k, cx + CS, cy + CS - k, fc);
            } else {
                g.fill(cx, cy, cx + CS, cy + CS, fc);
            }
            if (c.cleared() && (c.type() == 1 || c.type() == 2 || c.type() == 4)) {
                g.fill(cx + CS / 2 - 1, cy + CS / 2 - 1, cx + CS / 2 + 2, cy + CS / 2 + 2, 0xFFE8FFE8);
            }
            if (cur != null && cur.id() == c.id() && (tick / 10) % 2 == 0) {
                g.fill(cx - 1, cy - 1, cx + CS + 1, cy, 0xFFFFFFFF);
                g.fill(cx - 1, cy + CS, cx + CS + 1, cy + CS + 1, 0xFFFFFFFF);
                g.fill(cx - 1, cy, cx, cy + CS, 0xFFFFFFFF);
                g.fill(cx + CS, cy, cx + CS + 1, cy + CS, 0xFFFFFFFF);
            }
        }
        // tú
        if (cur != null) {
            double fx = (pl.getX() - (cur.col() * PITCH - 10.0)) / 21.0;
            double fz = (pl.getZ() - (ANTE_Z + cur.row() * PITCH - 10.0)) / 21.0;
            fx = Math.max(0.0, Math.min(1.0, fx));
            fz = Math.max(0.0, Math.min(1.0, fz));
            int px = x0 + 4 + (cur.col() - minC) * pitch + (int) Math.round(fx * (CS - 3));
            int py = y0 + 4 + cur.row() * pitch + (int) Math.round(fz * (CS - 3));
            g.fill(px, py, px + 3, py + 3, 0xFFFFFF55);
        }
    }

    private static boolean shownAt(int col, int row) {
        Cell c = cells.get(key(col, row));
        return c != null && shown(c);
    }

    /** ¿Se dibuja esta sala? Las del camino siempre; las laterales solo si ya se visitó o hay una puerta desde una sala visitada. */
    private static boolean shown(Cell c) {
        if (c.path() || visited.contains(c.id())) {
            return true;
        }
        int[][] d = {{0, -1}, {1, 0}, {0, 1}, {-1, 0}};
        for (int i = 0; i < 4; i++) {
            if ((c.doors() & (1 << i)) != 0) {
                Cell o = cells.get(key(c.col() + d[i][0], c.row() + d[i][1]));
                if (o != null && visited.contains(o.id())) {
                    return true;
                }
            }
        }
        return false;
    }
}
