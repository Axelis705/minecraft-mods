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
        ph = Math.min(230, height - 20);
        px = (width - pw) / 2;
        py = (height - ph) / 2;
        int bx = px + pw / 2;
        if (!info.built()) {
            addRenderableWidget(Button.builder(Component.literal("Construir la pista aquí"), b -> send(RaceActionPacket.BUILD, 0))
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
        if (!info.built()) {
            String[] lines = {"Aquí se construye una mini pista para correr con tus", "autos contra tus amigos o contra bots.", "",
                    "Se hace hacia donde mira el bloque (unos " + size() + " bloques).", "Reemplaza el terreno de esa zona: ¡revisa que no haya",
                    "nada importante enfrente!"};
            for (int i = 0; i < lines.length; i++) {
                Gfx.text(g, font, lines[i], px + pw / 2.0F, py + 40 + i * 12, 0.9F, i >= 4 ? 0xFFFFB04D : 0xFFE6E6F0);
            }
            super.render(g, mx, my, pt);
            return;
        }
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
            Gfx.text(g, font, "¡Carrera en curso!", px + pw / 2.0F, py + 28, 0.9F, 0xFF6CFF8A);
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

    private static String size() {
        float[] b = TrackLayout.MAIN.bounds(TrackLayout.HALF_W + 2.2F);
        return Math.round(b[2] - b[0]) + " × " + Math.round(b[3] - b[1]);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
