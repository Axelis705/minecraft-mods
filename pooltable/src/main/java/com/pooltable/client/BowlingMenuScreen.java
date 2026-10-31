package com.pooltable.client;

import com.pooltable.block.BowlingLaneBE;
import com.pooltable.net.BowlingActionPacket;
import com.pooltable.net.PoolNet;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

/** Menú de la pista: apuntarse (de 1 a 4 jugadores), empezar la partida o terminarla y volver a la práctica libre. */
public final class BowlingMenuScreen extends Screen {
    private static final int W = 260;
    private static final int H = 196;
    private final BlockPos pos;
    private int left;
    private int top;
    private Button joinButton;
    private Button startButton;
    private Button stopButton;

    public BowlingMenuScreen(BlockPos pos) {
        super(Component.translatable("block.pooltable.bowling_lane"));
        this.pos = pos;
    }

    private BowlingLaneBE be() {
        Minecraft mc = Minecraft.getInstance();
        return mc.level != null && mc.level.getBlockEntity(pos) instanceof BowlingLaneBE t ? t : null;
    }

    private void send(int action) {
        PoolNet.sendToServer(new BowlingActionPacket(pos, action));
    }

    @Override
    protected void init() {
        left = (width - W) / 2;
        top = (height - H) / 2;
        joinButton = addRenderableWidget(Button.builder(Component.translatable("pooltable.bowling.menu.join"), b -> {
            BowlingLaneBE t = be();
            boolean in = t != null && minecraft.player != null && t.pids.contains(minecraft.player.getUUID());
            send(in ? BowlingLaneBE.A_LEAVE : BowlingLaneBE.A_JOIN);
        }).bounds(left + 8, top + H - 52, 118, 20).build());
        startButton = addRenderableWidget(Button.builder(Component.translatable("pooltable.bowling.menu.start"), b -> send(BowlingLaneBE.A_START))
                .bounds(left + 134, top + H - 52, 118, 20).build());
        stopButton = addRenderableWidget(Button.builder(Component.translatable("pooltable.bowling.menu.stop"), b -> send(BowlingLaneBE.A_STOP))
                .bounds(left + 8, top + H - 28, 118, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose()).bounds(left + 134, top + H - 28, 118, 20).build());
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void tick() {
        BowlingLaneBE t = be();
        var p = Minecraft.getInstance().player;
        if (t == null || p == null) {
            onClose();
            return;
        }
        var c = t.center();
        if (p.distanceToSqr(c.x, c.y, c.z) > 16 * 16) {
            onClose();
            return;
        }
        boolean in = t.pids.contains(p.getUUID());
        boolean running = t.playing && t.game != null && !t.game.over;
        joinButton.setMessage(Component.translatable(in ? "pooltable.bowling.menu.leave" : "pooltable.bowling.menu.join"));
        joinButton.active = !running;
        startButton.active = !t.pids.isEmpty() && !running;
        stopButton.active = t.playing || !t.pids.isEmpty();
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        renderBackground(g);
        g.fill(left - 2, top - 2, left + W + 2, top + H + 2, 0xFF1E2A36);
        g.fill(left, top, left + W, top + H, 0xFF101820);
        g.fill(left, top, left + W, top + 18, 0xFF2B4A6B);
        g.drawCenteredString(font, title, left + W / 2, top + 5, 0xFFFFFF);
        BowlingLaneBE t = be();
        int y = top + 24;
        if (t != null) {
            boolean running = t.playing && t.game != null && !t.game.over;
            Component status;
            if (running) {
                status = Component.translatable("pooltable.bowling.menu.turn", t.pnames.get(t.game.current)).withStyle(ChatFormatting.GOLD);
            } else if (t.playing && t.game != null) {
                int w = t.game.winner();
                status = Component.translatable("pooltable.bowling.menu.over", t.pnames.get(w), t.game.scores[w].total()).withStyle(ChatFormatting.GREEN);
            } else if (t.pids.isEmpty()) {
                status = Component.translatable("pooltable.bowling.menu.practice").withStyle(ChatFormatting.AQUA);
            } else {
                status = Component.translatable("pooltable.bowling.menu.waiting").withStyle(ChatFormatting.YELLOW);
            }
            g.drawString(font, status, left + 8, y, 0xFFFFFF, false);
            y += 14;
            for (int i = 0; i < t.pids.size(); i++) {
                String line = (i + 1) + ". " + t.pnames.get(i);
                if (t.game != null && i < t.game.scores.length) {
                    line += "  -  " + t.game.scores[i].total();
                }
                boolean cur = running && t.game.current == i;
                g.drawString(font, Component.literal(line).withStyle(cur ? ChatFormatting.GOLD : ChatFormatting.WHITE), left + 14, y, 0xFFFFFF, false);
                y += 11;
            }
        }
        int hy = top + 82;
        for (int i = 1; i <= 5; i++) {
            g.drawString(font, Component.translatable("pooltable.bowling.menu.help" + i).withStyle(ChatFormatting.GRAY), left + 8, hy, 0xFFFFFF, false);
            hy += 10;
        }
        super.render(g, mx, my, pt);
    }
}
