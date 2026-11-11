package com.autoscoleccion.client;

import com.autoscoleccion.game.Boxes;
import com.autoscoleccion.game.CarDef;
import com.autoscoleccion.game.CarMesh;
import com.autoscoleccion.game.Cars;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * El garaje: tu colección por series (pestañas). Cada auto tiene su tarjeta con el modelo 3D girando (los que te faltan son una
 * silueta con "???"). Al pasar el mouse o hacer clic, el panel de la derecha lo muestra en grande con sus datos y en qué cajas sale.
 */
public class GarageScreen extends Screen {
    private int tab;
    private int selected = -1;
    private final long t0 = System.currentTimeMillis();
    private final float[] hover = new float[Cars.COUNT];
    private long lastFrame = System.currentTimeMillis();

    // geometría calculada en render (para los clics)
    private float px, py, pw, ph, gridX, gridY, cellW, cellH;
    private final float[] tabX = new float[4];
    private float tabY;

    public GarageScreen() {
        super(Component.literal("Garaje"));
        int[] c = AutosClient.counts;
        for (int i = 0; i < Cars.COUNT; i++) {
            if (c[i] > 0) {
                tab = Cars.get(i).line;
            }
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private List<CarDef> cars() {
        List<CarDef> out = new ArrayList<>();
        for (CarDef c : Cars.all()) {
            if (c.line == tab) {
                out.add(c);
            }
        }
        return out;
    }

    private static boolean owned(CarDef c) {
        return AutosClient.counts[c.index()] > 0;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (my >= tabY && my <= tabY + 16) {
            for (int i = 0; i < 3; i++) {
                if (mx >= tabX[i] && mx < tabX[i + 1] - 4) {
                    if (tab != i) {
                        tab = i;
                        selected = -1;
                        Sfx.play("whoosh", 1.2F, 0.5F);
                    }
                    return true;
                }
            }
        }
        int idx = cellAt(mx, my);
        if (idx >= 0) {
            selected = cars().get(idx).index();
            Sfx.play("key", 1.1F, 0.5F);
        }
        return true;
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (key == GLFW.GLFW_KEY_RIGHT || key == GLFW.GLFW_KEY_D) {
            tab = (tab + 1) % 3;
            selected = -1;
            return true;
        }
        if (key == GLFW.GLFW_KEY_LEFT || key == GLFW.GLFW_KEY_A) {
            tab = (tab + 2) % 3;
            selected = -1;
            return true;
        }
        return super.keyPressed(key, scan, mods);
    }

    private int cellAt(double mx, double my) {
        int n = cars().size();
        for (int i = 0; i < n; i++) {
            float x = gridX + (i % 5) * cellW, y = gridY + (i / 5) * cellH;
            if (mx >= x + 2 && mx < x + cellW - 2 && my >= y + 2 && my < y + cellH - 2) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        long now = System.currentTimeMillis();
        float dt = Math.min(0.1F, (now - lastFrame) / 1000.0F);
        lastFrame = now;
        float time = (now % 100000) / 50.0F;
        int W = width, H = height;
        pw = Math.min(W - 12, 470);
        ph = Math.min(H - 12, 270);
        px = (W - pw) / 2;
        py = (H - ph) / 2;
        float appear = Gfx.ease((now - t0) / 350.0F);

        Gfx.begin(g, false);
        Gfx.vgrad(0, 0, W, H, 0xE0050611, 0xE0120A1E);
        Gfx.round(px, py, px + pw, py + ph, 8, 0xF00E0E1A);
        // piso de garaje: líneas en perspectiva
        for (int i = 0; i < 9; i++) {
            float y = py + ph * 0.55F + i * i * 1.6F;
            if (y < py + ph - 4) {
                Gfx.rect(px + 6, y, px + pw - 6, y + 1, 0x10FFFFFF);
            }
        }
        Gfx.end();
        Gfx.begin(g, true);
        Gfx.glow(px, py, px + pw, py + ph, 0x403FE0FF, 7);
        Gfx.end();

        // encabezado y progreso total
        int total = 0;
        for (CarDef c : Cars.all()) {
            if (owned(c)) {
                total++;
            }
        }
        Gfx.textL(g, font, "MI GARAJE", px + 12, py + 9, 1.7F, 0xFFFFE066);
        String prog = total + "/" + Cars.COUNT + " autos";
        Gfx.textL(g, font, prog, px + pw - 12 - font.width(prog) * 0.9F, py + 8, 0.9F, 0xFFFFFFFF);
        Gfx.begin(g, false);
        float bx0 = px + pw - 132, bx1 = px + pw - 12;
        Gfx.round(bx0, py + 19, bx1, py + 25, 3, 0xFF22223A);
        Gfx.round(bx0, py + 19, bx0 + (bx1 - bx0) * total / (float) Cars.COUNT * appear, py + 25, 3, 0xFFFFD23F);
        Gfx.end();

        // pestañas
        tabY = py + 32;
        float tx = px + 12;
        for (int i = 0; i < 3; i++) {
            int have = 0;
            for (CarDef c : Cars.all()) {
                if (c.line == i && owned(c)) {
                    have++;
                }
            }
            String label = Boxes.NAME[i] + "  " + have + "/10";
            float tw = font.width(label) * 0.8F + 16;
            tabX[i] = tx;
            boolean on = tab == i;
            Gfx.begin(g, false);
            Gfx.round(tx, tabY, tx + tw, tabY + 16, 4, on ? 0xFF000000 | Boxes.COLOR[i] : 0xFF1C1C2C);
            Gfx.end();
            Gfx.textL(g, font, label, tx + 8, tabY + 5, 0.8F, on ? 0xFF101018 : 0xFF000000 | Boxes.COLOR[i]);
            tx += tw + 4;
        }
        tabX[3] = tx;

        // cuadrícula de 5 x 2
        float detailW = Math.min(150, pw * 0.34F);
        gridX = px + 10;
        gridY = tabY + 22;
        cellW = (pw - detailW - 26) / 5.0F;
        cellH = (py + ph - 10 - gridY) / 2.0F;
        List<CarDef> list = cars();
        int hov = cellAt(mx, my);
        for (int i = 0; i < list.size(); i++) {
            CarDef c = list.get(i);
            int idx = c.index();
            hover[idx] += ((i == hov ? 1 : 0) - hover[idx]) * Math.min(1, dt * 12);
            float x = gridX + (i % 5) * cellW, y = gridY + (i / 5) * cellH;
            float lift = hover[idx] * 3;
            boolean own = owned(c);
            int rc = c.rarity.rgb;
            Gfx.begin(g, false);
            Gfx.round(x + 2, y + 2 - lift, x + cellW - 2, y + cellH - 2 - lift, 5, own ? 0xFF171726 : 0xFF0F0F17);
            if (own) {
                Gfx.vgrad(x + 3, y + 3 - lift, x + cellW - 3, y + cellH * 0.6F - lift, Gfx.argb(rc, 0.35F), Gfx.argb(rc, 0.0F));
            }
            Gfx.end();
            if (own || selected == idx || hover[idx] > 0.05F) {
                Gfx.begin(g, true);
                Gfx.glow(x + 2, y + 2 - lift, x + cellW - 2, y + cellH - 2 - lift, Gfx.argb(own ? rc : 0x6A6A8A, selected == idx ? 0.8F : 0.25F + 0.4F * hover[idx]),
                        4);
                Gfx.end();
            }
            float size = Math.min(cellW / 19.0F, cellH / 13.0F) * appear;
            float yaw = -35 + (own ? time * (1.2F + 4 * hover[idx]) : 0);
            CarModel.renderInGui(g, c.style, x + cellW / 2, y + cellH * 0.42F - lift, 150, size, yaw, 20, own ? CarMesh.MODE_NORMAL : CarMesh.MODE_SILHOUETTE,
                    time);
            String name = own ? c.name : "???";
            float ns = Math.min(0.7F, (cellW - 6) / Math.max(1, font.width(name)));
            Gfx.text(g, font, name, x + cellW / 2, y + cellH - 19 - lift, ns, own ? 0xFF000000 | rc : 0xFF55556A);
            String stars = "★".repeat(c.rarity.stars);
            Gfx.text(g, font, stars, x + cellW / 2, y + cellH - 11 - lift, 0.6F, own ? 0xFF000000 | rc : 0xFF34344A);
            Gfx.textL(g, font, "#" + c.number, x + 5, y + 5 - lift, 0.55F, 0xFF8A8AA8);
            int cnt = AutosClient.counts[idx];
            if (cnt > 1) {
                Gfx.textL(g, font, "x" + cnt, x + cellW - 6 - font.width("x" + cnt) * 0.65F, y + 5 - lift, 0.65F, 0xFFFFFFFF);
            }
        }

        // panel de detalle
        int show = hov >= 0 ? list.get(hov).index() : selected;
        float dx = px + pw - detailW - 10, dy = gridY;
        float dh = py + ph - 10 - dy;
        Gfx.begin(g, false);
        Gfx.round(dx, dy, dx + detailW, dy + dh, 6, 0xFF13131F);
        Gfx.end();
        if (show < 0) {
            List<FormattedCharSequence> lines = font.split(Component.literal("Pasa el mouse sobre un auto para verlo en grande. "
                    + "Consigue cajas en la máquina expendedora (con un lingote de oro) y exhíbelos en una vitrina."), (int) ((detailW - 14) / 0.75F));
            float ly = dy + 12;
            for (FormattedCharSequence l : lines) {
                g.pose().pushPose();
                g.pose().translate(dx + 7, ly, 0);
                g.pose().scale(0.75F, 0.75F, 1);
                g.drawString(font, l, 0, 0, 0xFF9A9AC0, false);
                g.pose().popPose();
                ly += 9;
            }
        } else {
            CarDef c = Cars.get(show);
            boolean own = owned(c);
            int rc = c.rarity.rgb;
            Gfx.begin(g, true);
            Gfx.blob(dx + detailW / 2, dy + dh * 0.32F, detailW * 0.55F, Gfx.argb(own ? rc : 0x404060, 0.35F));
            Gfx.end();
            Gfx.begin(g, false);
            float ey = dy + dh * 0.46F;
            Gfx.quad(dx + 14, ey, 0x00000000, dx + detailW - 14, ey, 0x00000000, dx + detailW - 30, ey - 8, 0x40FFFFFF, dx + 30, ey - 8, 0x40FFFFFF);
            Gfx.end();
            CarModel.renderInGui(g, c.style, dx + detailW / 2, dy + dh * 0.3F, 200, detailW / 19.0F, -35 + time * 1.5F, 18,
                    own ? CarMesh.MODE_NORMAL : CarMesh.MODE_SILHOUETTE, time);
            float ty = dy + dh * 0.5F;
            Gfx.text(g, font, own ? c.name : "???", dx + detailW / 2, ty, Math.min(1.1F, (detailW - 8) / Math.max(1, font.width(own ? c.name : "???"))),
                    own ? 0xFF000000 | rc : 0xFF6A6A8A);
            Gfx.text(g, font, "★".repeat(c.rarity.stars) + " " + c.rarity.label, dx + detailW / 2, ty + 13, 0.75F, 0xFF000000 | rc);
            float ly = ty + 26;
            if (own) {
                for (FormattedCharSequence l : font.split(Component.literal(c.blurb), (int) ((detailW - 14) / 0.7F))) {
                    g.pose().pushPose();
                    g.pose().translate(dx + 7, ly, 0);
                    g.pose().scale(0.7F, 0.7F, 1);
                    g.drawString(font, l, 0, 0, 0xFFCFCFE0, false);
                    g.pose().popPose();
                    ly += 8;
                }
                ly += 3;
            }
            Gfx.textL(g, font, "Sale en:", dx + 7, ly, 0.7F, 0xFF9A9AC0);
            ly += 9;
            for (int b = 0; b < Boxes.COUNT; b++) {
                double p = Boxes.chance(c, b);
                if (p > 0) {
                    Gfx.textL(g, font, String.format(Locale.ROOT, "%s  %.1f%%", Boxes.NAME[b], p * 100), dx + 10, ly, 0.68F, 0xFF000000 | Boxes.COLOR[b]);
                    ly += 8;
                }
            }
        }
        Gfx.text(g, font, "← → cambiar serie · ESC salir", px + pw / 2, py + ph - 8, 0.6F, 0x60FFFFFF);
    }
}
