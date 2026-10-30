package com.kensei.client;

import com.kensei.entity.RivalEntity;
import com.kensei.net.DuelRequestPacket;
import com.kensei.net.KenseiNet;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * El menú que sale al pulsar la tecla de duelo: elegir 1 contra 1 o 2 contra 2 y con quién. Cada puesto (aliado, rival 1, rival 2) es un
 * botón que va pasando por los jugadores cercanos y por el rival de práctica. Los jugadores tienen que aceptar; el rival de práctica entra solo.
 */
public class DuelMenuScreen extends Screen {
    private static final int W = 270;
    private static final int H = 200;

    private record Cand(int id, String name) {
    }

    private final int targetId;
    private final List<Cand> cands = new ArrayList<>();
    private int mode = 1;
    /** Qué candidato tiene cada puesto: 0 = aliado, 1 = rival 1, 2 = rival 2 (1v1 usa solo el 1). */
    private final int[] pick = new int[3];

    private Button mode1;
    private Button mode2;
    private final Button[] slotButtons = new Button[3];
    private Button go;

    public DuelMenuScreen(int targetId) {
        super(Component.translatable("kensei.duelmenu.title"));
        this.targetId = targetId;
    }

    @Override
    protected void init() {
        buildCandidates();
        presetSlots();
        int x = (this.width - W) / 2;
        int y = (this.height - H) / 2;

        mode1 = this.addRenderableWidget(Button.builder(Component.empty(), b -> setMode(1)).bounds(x + 14, y + 30, 118, 22).build());
        mode2 = this.addRenderableWidget(Button.builder(Component.empty(), b -> setMode(2)).bounds(x + W - 14 - 118, y + 30, 118, 22).build());
        for (int i = 0; i < 3; i++) {
            final int slot = i;
            slotButtons[i] = this.addRenderableWidget(Button.builder(Component.empty(), b -> cycle(slot)).bounds(x + 110, y + 64 + i * 26, W - 110 - 14, 20).build());
        }
        go = this.addRenderableWidget(Button.builder(Component.translatable("kensei.duelmenu.go").withStyle(ChatFormatting.BOLD), b -> send())
                .bounds(x + 14, y + H - 34, 118, 22).build());
        this.addRenderableWidget(Button.builder(Component.translatable("kensei.duelmenu.cancel"), b -> onClose()).bounds(x + W - 14 - 118, y + H - 34, 118, 22).build());
        refresh();
    }

    // ------------------------------------------------------------------ candidatos

    private void buildCandidates() {
        cands.clear();
        Minecraft mc = Minecraft.getInstance();
        List<AbstractClientPlayer> near = new ArrayList<>();
        if (mc.level != null && mc.player != null) {
            for (AbstractClientPlayer p : mc.level.players()) {
                if (p != mc.player && !p.isSpectator() && p.distanceTo(mc.player) <= 96.0F) {
                    near.add(p);
                }
            }
            near.sort(Comparator.comparingDouble(p -> p.distanceToSqr(mc.player)));
        }
        for (AbstractClientPlayer p : near) {
            cands.add(new Cand(p.getId(), p.getGameProfile().getName()));
        }
        cands.add(new Cand(-1, Component.translatable("kensei.duelmenu.bot").getString()));
    }

    private int botIndex() {
        return cands.size() - 1;
    }

    private boolean isBot(int idx) {
        return idx == botIndex();
    }

    /** Valores de partida: el rival al que se apuntaba; si no, el jugador más cercano; si no hay nadie, el rival de práctica. */
    private void presetSlots() {
        int preFoe = -1;
        Minecraft mc = Minecraft.getInstance();
        Entity t = targetId >= 0 && mc.level != null ? mc.level.getEntity(targetId) : null;
        if (t instanceof RivalEntity) {
            preFoe = botIndex();
        } else if (t != null) {
            for (int i = 0; i < cands.size(); i++) {
                if (cands.get(i).id == targetId) {
                    preFoe = i;
                }
            }
        }
        if (preFoe < 0) {
            preFoe = 0; // el más cercano (o el rival de práctica si no hay jugadores)
        }
        pick[1] = preFoe;
        pick[2] = firstHumanNot(pick[1], -1);
        pick[0] = firstHumanNot(pick[1], pick[2]);
    }

    /** El jugador más cercano que no sea ninguno de estos dos (o el rival de práctica si no queda ninguno). */
    private int firstHumanNot(int a, int b) {
        for (int i = 0; i < botIndex(); i++) {
            if (i != a && i != b) {
                return i;
            }
        }
        return botIndex();
    }

    private boolean active(int s) {
        return mode == 2 || s == 1;
    }

    private boolean usedByOther(int idx, int slot) {
        if (isBot(idx)) {
            return false; // el rival de práctica se puede repetir
        }
        for (int s = 0; s < 3; s++) {
            if (s != slot && active(s) && pick[s] == idx) {
                return true;
            }
        }
        return false;
    }

    private void cycle(int slot) {
        int n = cands.size();
        int cur = pick[slot];
        for (int step = 1; step <= n; step++) {
            int idx = (cur + step) % n;
            if (!usedByOther(idx, slot)) {
                pick[slot] = idx;
                break;
            }
        }
        refresh();
    }

    private void setMode(int m) {
        mode = m;
        // al pasar a 2v2 se evita repetir jugadores entre puestos
        if (m == 2) {
            for (int s = 0; s < 3; s++) {
                if (usedByOther(pick[s], s)) {
                    pick[s] = botIndex();
                }
            }
        }
        refresh();
    }

    private String slotLabel(int s) {
        return switch (s) {
            case 0 -> Component.translatable("kensei.duelmenu.ally").getString();
            case 1 -> Component.translatable(mode == 1 ? "kensei.duelmenu.foe" : "kensei.duelmenu.foe1").getString();
            default -> Component.translatable("kensei.duelmenu.foe2").getString();
        };
    }

    private void refresh() {
        mode1.setMessage(Component.translatable("kensei.duelmenu.mode1").withStyle(mode == 1 ? ChatFormatting.GOLD : ChatFormatting.WHITE, mode == 1 ? ChatFormatting.BOLD : ChatFormatting.RESET));
        mode2.setMessage(Component.translatable("kensei.duelmenu.mode2").withStyle(mode == 2 ? ChatFormatting.GOLD : ChatFormatting.WHITE, mode == 2 ? ChatFormatting.BOLD : ChatFormatting.RESET));
        for (int s = 0; s < 3; s++) {
            boolean show = active(s);
            slotButtons[s].visible = show;
            slotButtons[s].active = show;
            Cand c = cands.get(pick[s]);
            slotButtons[s].setMessage(Component.literal(c.name).withStyle(isBot(pick[s]) ? ChatFormatting.AQUA : ChatFormatting.WHITE));
        }
        // en 1v1 el único botón de rival sube a la primera fila
        int y = (this.height - H) / 2;
        if (mode == 1) {
            slotButtons[1].setY(y + 64);
        } else {
            for (int s = 0; s < 3; s++) {
                slotButtons[s].setY(y + 64 + s * 26);
            }
        }
    }

    private void send() {
        int[] slots;
        if (mode == 1) {
            slots = new int[] {cands.get(pick[1]).id};
        } else {
            slots = new int[] {cands.get(pick[0]).id, cands.get(pick[1]).id, cands.get(pick[2]).id};
        }
        KenseiNet.CHANNEL.sendToServer(new DuelRequestPacket(mode, slots));
        onClose();
    }

    // ------------------------------------------------------------------ dibujo

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        this.renderBackground(g);
        int x = (this.width - W) / 2;
        int y = (this.height - H) / 2;
        // panel: marco azul y fondo oscuro, con una franja de título
        g.fill(x - 2, y - 2, x + W + 2, y + H + 2, 0xFF0B0D12);
        g.fill(x - 1, y - 1, x + W + 1, y + H + 1, 0xFF2E7F9C);
        g.fill(x, y, x + W, y + H, 0xF0141922);
        g.fill(x, y, x + W, y + 22, 0xFF1E2A38);
        g.fill(x, y + 22, x + W, y + 23, 0xFF2E7F9C);
        g.drawCenteredString(this.font, Component.literal("⚔ ").append(this.title).withStyle(ChatFormatting.BOLD), this.width / 2, y + 7, 0xFFFFD84A);
        // etiquetas de los puestos
        for (int s = 0; s < 3; s++) {
            boolean show = active(s);
            if (!show) {
                continue;
            }
            int ly = (mode == 1 && s == 1 ? 64 : 64 + s * 26) + 6;
            g.drawString(this.font, slotLabel(s), x + 16, y + ly, 0xFFB4BCC8, true);
        }
        // ayuda
        boolean onlyBot = cands.size() == 1;
        Component hint = Component.translatable(onlyBot ? "kensei.duelmenu.hint_alone" : "kensei.duelmenu.hint");
        List<net.minecraft.util.FormattedCharSequence> lines = this.font.split(hint, W - 28);
        int hy = y + (mode == 1 ? 94 : 144);
        for (net.minecraft.util.FormattedCharSequence l : lines) {
            g.drawString(this.font, l, x + 14, hy, 0xFF8A94A2, false);
            hy += 10;
        }
        super.render(g, mx, my, pt);
    }
}
