package com.pooltable.client;

import com.pooltable.block.PoolTableBE;
import com.pooltable.game.PoolRules;
import com.pooltable.net.PoolActionPacket;
import com.pooltable.net.PoolNet;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;

/**
 * Menú de la mesa: elegir el modo de juego, apuntarse (de 1 a 6 jugadores), empezar, salir o reiniciar.
 * Solo muestra el estado de la mesa y manda acciones al servidor; el servidor comprueba todo.
 */
public final class PoolMenuScreen extends Screen {
    private static final int W = 300;
    private static final int H = 206;
    private final BlockPos pos;
    private int left;
    private int top;
    private final Button[] modeButtons = new Button[PoolRules.Mode.values().length];
    private Button joinButton;
    private Button startButton;
    private Button resetButton;

    public PoolMenuScreen(BlockPos pos) {
        super(Component.translatable("block.pooltable.pool_table"));
        this.pos = pos;
    }

    private PoolTableBE be() {
        Minecraft mc = Minecraft.getInstance();
        return mc.level != null && mc.level.getBlockEntity(pos) instanceof PoolTableBE t ? t : null;
    }

    private void send(int action, int arg) {
        PoolNet.sendToServer(new PoolActionPacket(pos, action, arg));
    }

    @Override
    protected void init() {
        left = (width - W) / 2;
        top = (height - H) / 2;
        PoolRules.Mode[] modes = PoolRules.Mode.values();
        int bw = 70;
        for (int i = 0; i < modes.length; i++) {
            final int idx = i;
            modeButtons[i] = addRenderableWidget(Button.builder(Component.translatable("pooltable.mode." + modes[i].id + ".short"),
                    b -> send(PoolTableBE.A_MODE, idx)).bounds(left + 4 + i * (bw + 4), top + 22, bw, 20).build());
        }
        joinButton = addRenderableWidget(Button.builder(Component.translatable("pooltable.menu.join"), b -> {
            PoolTableBE t = be();
            boolean in = t != null && minecraft.player != null && t.pids.contains(minecraft.player.getUUID());
            send(in ? PoolTableBE.A_LEAVE : PoolTableBE.A_JOIN, 0);
        }).bounds(left + 6, top + H - 52, 140, 20).build());
        startButton = addRenderableWidget(Button.builder(Component.translatable("pooltable.menu.start"),
                b -> send(PoolTableBE.A_START, 0)).bounds(left + W - 146, top + H - 52, 140, 20).build());
        resetButton = addRenderableWidget(Button.builder(Component.translatable("pooltable.menu.reset"),
                b -> send(PoolTableBE.A_RESET, 0)).bounds(left + 6, top + H - 28, 140, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("pooltable.menu.close"), b -> onClose())
                .bounds(left + W - 146, top + H - 28, 140, 20).build());
        refresh();
    }

    /** Pone los botones al día con lo que sabe el cliente de la mesa. */
    private void refresh() {
        PoolTableBE t = be();
        if (t == null) {
            return;
        }
        boolean me = minecraft != null && minecraft.player != null && t.pids.contains(minecraft.player.getUUID());
        PoolRules.Mode[] modes = PoolRules.Mode.values();
        for (int i = 0; i < modes.length; i++) {
            Component label = Component.translatable("pooltable.mode." + modes[i].id + ".short");
            boolean sel = t.mode() == modes[i];
            modeButtons[i].setMessage(sel ? label.copy().withStyle(ChatFormatting.YELLOW, ChatFormatting.UNDERLINE) : label);
            modeButtons[i].active = !t.playing;
        }
        joinButton.setMessage(Component.translatable(me ? "pooltable.menu.leave" : "pooltable.menu.join"));
        joinButton.active = me || (!t.playing && t.pids.size() < PoolRules.MAX_PLAYERS);
        startButton.active = !t.playing && me && t.validCount();
        startButton.setMessage(Component.translatable(t.playing ? "pooltable.menu.playing" : "pooltable.menu.start"));
        resetButton.active = true;
    }

    @Override
    public void tick() {
        if (be() == null) {
            onClose();
            return;
        }
        refresh();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private int nameColor(PoolTableBE t, int i, int n) {
        if (t.mode().teams(n)) {
            return t.rules.teamOf(i) == 0 ? 0x55FFFF : 0xFFAA55;
        }
        return 0xFFFFFF;
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        renderBackground(g);
        g.fill(left, top, left + W, top + H, 0xE0151A20);
        g.fill(left, top, left + W, top + 1, 0xFF3C8A5A);
        g.fill(left, top + H - 1, left + W, top + H, 0xFF3C8A5A);
        g.drawCenteredString(font, title, left + W / 2, top + 8, 0xFFFFFF);
        PoolTableBE t = be();
        if (t != null) {
            PoolRules.Mode mode = t.mode();
            int n = t.playing ? Math.max(1, t.rules.players) : t.pids.size();
            // descripción del modo
            Component desc = Component.translatable("pooltable.mode." + mode.id + ".desc").withStyle(ChatFormatting.GRAY);
            List<FormattedCharSequence> lines = font.split(desc, W - 16);
            for (int i = 0; i < lines.size() && i < 3; i++) {
                g.drawString(font, lines.get(i), left + 8, top + 47 + i * 10, 0xAAAAAA, false);
            }
            // jugadores
            g.drawString(font, Component.translatable("pooltable.menu.players", t.pids.size(), Component.translatable("pooltable.mode." + mode.id + ".count")),
                    left + 8, top + 80, 0xFFFFFF, false);
            for (int i = 0; i < t.pids.size(); i++) {
                int col = i / 3;
                int row = i % 3;
                boolean turn = t.playing && i == t.rules.turn;
                Component line = Component.literal((turn ? "> " : "  ") + (i + 1) + ". " + t.pnames.get(i) + extra(t, i));
                g.drawString(font, line, left + 8 + col * 148, top + 92 + row * 11, turn ? 0xFFFF55 : nameColor(t, i, n), false);
            }
            // estado
            Component st = status(t);
            g.drawString(font, st, left + 8, top + 130, 0xFFFFFF, false);
            if (!t.playing && !t.validCount() && !t.pids.isEmpty()) {
                g.drawString(font, Component.translatable("pooltable.menu.need", Component.translatable("pooltable.mode." + mode.id + ".count")),
                        left + 8, top + 141, 0xFF8080, false);
            }
        }
        super.render(g, mx, my, pt);
    }

    /** Lo que se escribe junto al nombre: puntos, grupo o equipo, según el modo. */
    private String extra(PoolTableBE t, int i) {
        if (!t.playing) {
            if (t.mode().teams(t.pids.size())) {
                return " [" + (t.rules.teamOf(i) == 0 ? "A" : "B") + "]";
            }
            return "";
        }
        return switch (t.mode()) {
            case ROTATION -> " - " + t.rules.score[i];
            case EIGHT -> {
                int team = t.rules.teamOf(i);
                String s = t.mode().teams(t.rules.players) ? " [" + (team == 0 ? "A" : "B") + "]" : "";
                int g = t.rules.group[team];
                yield g < 0 ? s : s + " (" + Component.translatable(g == 0 ? "pooltable.group.solids" : "pooltable.group.stripes").getString()
                        + " " + t.ph.remaining(g) + ")";
            }
            default -> "";
        };
    }

    private Component status(PoolTableBE t) {
        if (!t.playing) {
            if (t.pids.isEmpty()) {
                return Component.translatable("pooltable.menu.free");
            }
            return Component.translatable(t.validCount() ? "pooltable.menu.ready" : "pooltable.menu.waiting").withStyle(t.validCount() ? ChatFormatting.GREEN : ChatFormatting.YELLOW);
        }
        if (t.mode() == PoolRules.Mode.SOLO) {
            return Component.translatable("pooltable.menu.solo_status", t.rules.shots, t.rules.best > 0 ? Integer.toString(t.rules.best) : "-")
                    .withStyle(ChatFormatting.GREEN);
        }
        return Component.translatable("pooltable.status.turn", t.rules.turn < t.pnames.size() ? t.pnames.get(t.rules.turn) : "?")
                .withStyle(ChatFormatting.GREEN);
    }
}
