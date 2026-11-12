package com.autoscoleccion.client;

import com.autoscoleccion.game.CarDef;
import com.autoscoleccion.game.Cars;
import com.autoscoleccion.game.TrackLayout;
import com.autoscoleccion.net.AutosNet;
import com.autoscoleccion.net.RaceActionPacket;
import com.autoscoleccion.net.TrackInfoPacket;
import com.autoscoleccion.game.CarMesh;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/** Pantalla del bloque de mini pista: construirla, elegir auto, vueltas y bots, anotarse y arrancar la carrera. */
public class TrackScreen extends Screen {
    private TrackInfoPacket info;
    private int carPick;
    private final List<Integer> owned = new ArrayList<>();
    private int px, py, pw, ph;

    public TrackScreen(TrackInfoPacket info) {
        super(Component.literal("Mini pista"));
        this.info = info;
        for (CarDef c : Cars.all()) {
            if (AutosClient.counts[c.index()] > 0) {
                owned.add(c.index());
            }
        }
        carPick = owned.isEmpty() ? -1 : owned.size() - 1;
    }

    public BlockPos pos() {
        return info.pos();
    }

    public void update(TrackInfoPacket m) {
        info = m;
        rebuild();
    }

    private void send(int action, int arg) {
        AutosNet.toServer(new RaceActionPacket(info.pos(), action, arg));
        Sfx.play("key", 1.1F, 0.4F);
    }

    @Override
    protected void init() {
        rebuild();
    }

    private void rebuild() {
        clearWidgets();
        pw = Math.min(340, width - 20);
        ph = Math.min(info.built() ? 230 : 270, height - 20);
        px = (width - pw) / 2;
        py = (height - ph) / 2;
        int bx = px + pw / 2;
        if (!info.built()) {
            int n = TrackLayout.ALL.length;
            addRenderableWidget(Button.builder(Component.literal("<"), b -> send(RaceActionPacket.THEME, (info.theme() + n - 1) % n))
                    .bounds(px + 10, py + 30, 20, 20).build());
            addRenderableWidget(Button.builder(Component.literal(">"), b -> send(RaceActionPacket.THEME, (info.theme() + 1) % n))
                    .bounds(px + pw - 30, py + 30, 20, 20).build());
            addRenderableWidget(Button.builder(Component.literal("Construir esta pista"), b -> send(RaceActionPacket.BUILD, 0))
                    .bounds(bx - 80, py + ph - 52, 160, 20).build());
            addRenderableWidget(Button.builder(Component.literal("Cerrar"), b -> onClose()).bounds(bx - 40, py + ph - 26, 80, 18).build());
            return;
        }
        int right = px + pw - 150;
        // vueltas y bots
        addRenderableWidget(Button.builder(Component.literal("-"), b -> send(RaceActionPacket.LAPS, info.laps() - 1)).bounds(right, py + 40, 20, 18).build());
        addRenderableWidget(Button.builder(Component.literal("+"), b -> send(RaceActionPacket.LAPS, info.laps() + 1)).bounds(right + 120, py + 40, 20, 18).build());
        addRenderableWidget(Button.builder(Component.literal("-"), b -> send(RaceActionPacket.BOTS, info.bots() - 1)).bounds(right, py + 64, 20, 18).build());
        addRenderableWidget(Button.builder(Component.literal("+"), b -> send(RaceActionPacket.BOTS, info.bots() + 1)).bounds(right + 120, py + 64, 20, 18).build());
        // auto
        int lx = px + 12;
        if (!owned.isEmpty()) {
            addRenderableWidget(Button.builder(Component.literal("<"), b -> {
                carPick = (carPick - 1 + owned.size()) % owned.size();
            }).bounds(lx, py + 108, 20, 20).build());
            addRenderableWidget(Button.builder(Component.literal(">"), b -> {
                carPick = (carPick + 1) % owned.size();
            }).bounds(lx + 150, py + 108, 20, 20).build());
        }
        boolean running = info.running();
        String me = Minecraft.getInstance().player != null ? Minecraft.getInstance().player.getGameProfile().getName() + " · " : "";
        boolean joined = info.names().stream().anyMatch(n -> !me.isEmpty() && n.startsWith(me));
        Button join = Button.builder(Component.literal(joined ? "Cambiar mi auto" : "Anotarme"), b -> {
            if (carPick >= 0) {
                send(RaceActionPacket.JOIN, owned.get(carPick));
            }
        }).bounds(lx, py + ph - 52, 84, 20).build();
        join.active = !running && carPick >= 0;
        addRenderableWidget(join);
        Button leave = Button.builder(Component.literal("Salirme"), b -> send(RaceActionPacket.LEAVE, 0)).bounds(lx + 88, py + ph - 52, 82, 20).build();
        leave.active = joined && !running;
        addRenderableWidget(leave);
        if (running) {
            addRenderableWidget(Button.builder(Component.literal("Cancelar carrera"), b -> send(RaceActionPacket.CANCEL, 0))
                    .bounds(right, py + ph - 52, 140, 20).build());
        } else {
            Button go = Button.builder(Component.literal("¡Empezar carrera!"), b -> {
                send(RaceActionPacket.START, 0);
                onClose();
            }).bounds(right, py + ph - 52, 140, 20).build();
            go.active = !info.names().isEmpty();
            addRenderableWidget(go);
        }
        addRenderableWidget(Button.builder(Component.literal("Cerrar"), b -> onClose()).bounds(px + pw / 2 - 40, py + ph - 26, 80, 18).build());
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        renderBackground(g);
        Gfx.begin(g, false);
        Gfx.round(px, py, px + pw, py + ph, 8, 0xF0101420);
        Gfx.round(px + 2, py + 2, px + pw - 2, py + 22, 6, 0xFF1C2236);
        Gfx.end();
        Gfx.text(g, font, "Mini pista de carreras", px + pw / 2.0F, py + 8, 1.2F, 0xFFFFD34D);
        TrackLayout lay = TrackLayout.ALL[Math.max(0, Math.min(TrackLayout.ALL.length - 1, info.theme()))];
        if (!info.built()) {
            Gfx.text(g, font, lay.name, px + pw / 2.0F, py + 32, 1.3F, THEME_COL[info.theme() % THEME_COL.length]);
            Gfx.text(g, font, lay.blurb, px + pw / 2.0F, py + 46, 0.75F, 0xFFE6E6F0);
            miniMap(g, lay, px + pw / 2.0F, py + 58, pw - 60, ph - 140);
            Gfx.text(g, font, "Tamaño: " + size(lay) + " bloques · " + Math.round(lay.length) + " bloques por vuelta", px + pw / 2.0F, py + ph - 78, 0.7F,
                    0xFFE6E6F0);
            Gfx.text(g, font, "Se construye hacia donde mira el bloque y reemplaza el terreno: ¡revisa que no haya nada importante!",
                    px + pw / 2.0F, py + ph - 66, 0.6F, 0xFFFFB04D);
            super.render(g, mx, my, pt);
            return;
        }
        Gfx.text(g, font, lay.name, px + pw / 2.0F, py + 28, 0.8F, THEME_COL[info.theme() % THEME_COL.length]);
        int right = px + pw - 150;
        Gfx.text(g, font, info.laps() + (info.laps() == 1 ? " vuelta" : " vueltas"), right + 70, py + 45, 1.0F, 0xFFFFFFFF);
        Gfx.text(g, font, info.bots() == 0 ? "sin bots" : info.bots() + (info.bots() == 1 ? " bot" : " bots"), right + 70, py + 69, 1.0F, 0xFFFFFFFF);
        Gfx.textL(g, font, "Corredores:", right, py + 92, 0.85F, 0xFF9AE6FF);
        List<String> names = info.names();
        if (names.isEmpty()) {
            Gfx.textL(g, font, "nadie todavía", right, py + 104, 0.8F, 0x80FFFFFF);
        }
        for (int i = 0; i < names.size() && i < 8; i++) {
            Gfx.textL(g, font, names.get(i), right, py + 104 + i * 10, 0.75F, 0xFFE6E6F0);
        }
        if (info.running()) {
            Gfx.text(g, font, "¡Carrera en curso!", px + pw / 2.0F, py + 38, 0.9F, 0xFF6CFF8A);
        }
        int lx = px + 12;
        Gfx.textL(g, font, "Tu auto:", lx, py + 32, 0.85F, 0xFF9AE6FF);
        if (carPick < 0) {
            Gfx.textL(g, font, "No tienes autos. ¡Abre cajas primero!", lx, py + 50, 0.8F, 0xFFFFB04D);
        } else {
            CarDef c = Cars.get(owned.get(carPick));
            float t = (System.currentTimeMillis() % 100000) / 50.0F;
            CarModel.renderInGui(g, c.style, lx + 85, py + 80, 200, 3.6F, t * 1.2F, 20, CarMesh.MODE_NORMAL, t);
            Gfx.text(g, font, c.name, lx + 85, py + 113, 0.9F, 0xFF000000 | c.rarity.rgb);
            Gfx.text(g, font, (carPick + 1) + " / " + owned.size(), lx + 85, py + 132, 0.65F, 0x80FFFFFF);
        }
        Gfx.text(g, font, "Todos manejan desde aquí con W A S D. Gana el primero en completar las vueltas.", px + pw / 2.0F, py + ph - 64, 0.6F,
                0xA0FFFFFF);
        super.render(g, mx, my, pt);
    }

    private static final int[] THEME_COL = {0xFFFFFFFF, 0xFFFFC66B, 0xFFFF6CE0, 0xFF9AE6FF, 0xFFFF7A3A};
    private static final int[] ROAD_COL = {0xFF55575E, 0xFFE3CF94, 0xFF1D1D22, 0xFFB9D8F2, 0xFF2A2730};
    private static final int[] EDGE_COL = {0xFFD7263D, 0xFFE07A2E, 0xFFFF3FD0, 0xFF3A6BD8, 0xFFFF6A1A};

    private static String size(TrackLayout t) {
        float[] b = t.bounds(t.halfW + 2.2F);
        return Math.round(b[2] - b[0]) + " × " + Math.round(b[3] - b[1]);
    }

    /** Dibujo de la pista vista desde arriba, con el bloque (punto amarillo) y la meta. */
    private void miniMap(GuiGraphics g, TrackLayout t, float cx, float top, float w, float h) {
        float[] b = t.bounds(t.halfW + 2);
        float bx0 = Math.min(b[0], -2), bx1 = Math.max(b[2], 2), bz0 = Math.min(b[1], -2), bz1 = Math.max(b[3], 2);
        float k = Math.min(w / (bx1 - bx0), h / (bz1 - bz0));
        float ox = cx - (bx0 + bx1) / 2 * k, oy = top + h / 2 + (bz0 + bz1) / 2 * k;
        int ti = info.theme() % ROAD_COL.length;
        Gfx.begin(g, false);
        Gfx.round(cx - w / 2 - 6, top - 4, cx + w / 2 + 6, top + h + 4, 6, 0xFF0B0E16);
        int n = 160;
        for (int pass = 0; pass < 2; pass++) {
            float lw = (pass == 0 ? t.halfW + 1.2F : t.halfW) * 2 * k;
            for (int i = 0; i < n; i++) {
                float[] a = t.at(i * t.length / n), c = t.at((i + 1) * t.length / n);
                Gfx.line(ox + a[0] * k, oy - a[1] * k, ox + c[0] * k, oy - c[1] * k, Math.max(1.5F, lw),
                        pass == 0 ? EDGE_COL[ti] : ROAD_COL[ti]);
            }
        }
        float[] s0 = t.at(0);
        Gfx.line(ox + (s0[0] + s0[3] * t.halfW) * k, oy - (s0[1] - s0[2] * t.halfW) * k, ox + (s0[0] - s0[3] * t.halfW) * k,
                oy - (s0[1] + s0[2] * t.halfW) * k, 2, 0xFFFFFFFF);
        Gfx.circle(ox, oy, 3, 0xFFFFD34D);
        Gfx.end();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
