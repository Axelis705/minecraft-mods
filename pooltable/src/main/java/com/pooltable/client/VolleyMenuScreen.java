package com.pooltable.client;

import com.pooltable.block.VolleyCourtBE;
import com.pooltable.net.PoolNet;
import com.pooltable.net.VolleyMenuPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

/** Menú de la cancha: elegir equipo, tamaño (auto, 1 contra 1, 2 contra 2, 3 contra 3), nivel de los bots, empezar o terminar. */
public final class VolleyMenuScreen extends Screen {
    private static final int W = 280;
    private static final int H = 214;
    private final BlockPos pos;
    private int left;
    private int top;
    private Button teamA;
    private Button teamB;
    private Button sizeBtn;
    private Button diffBtn;
    private Button startBtn;
    private Button stopBtn;
    private Button leaveBtn;

    public VolleyMenuScreen(BlockPos pos) {
        super(Component.translatable("block.pooltable.volley_court"));
        this.pos = pos;
    }

    private VolleyCourtBE be() {
        Minecraft mc = Minecraft.getInstance();
        return mc.level != null && mc.level.getBlockEntity(pos) instanceof VolleyCourtBE t ? t : null;
    }

    private void send(int action, int arg) {
        PoolNet.sendToServer(new VolleyMenuPacket(pos, action, arg));
    }

    @Override
    protected void init() {
        left = (width - W) / 2;
        top = (height - H) / 2;
        teamA = addRenderableWidget(Button.builder(Component.translatable("pooltable.voley.menu.join.0"), b -> send(VolleyCourtBE.A_JOIN, 0))
                .bounds(left + 8, top + 22, 130, 20).build());
        teamB = addRenderableWidget(Button.builder(Component.translatable("pooltable.voley.menu.join.1"), b -> send(VolleyCourtBE.A_JOIN, 1))
                .bounds(left + 142, top + 22, 130, 20).build());
        sizeBtn = addRenderableWidget(Button.builder(Component.empty(), b -> {
            VolleyCourtBE t = be();
            if (t != null) {
                send(VolleyCourtBE.A_SIZE, (t.sizeSetting + 1) % 4);
            }
        }).bounds(left + 8, top + 118, 130, 20).build());
        diffBtn = addRenderableWidget(Button.builder(Component.empty(), b -> {
            VolleyCourtBE t = be();
            if (t != null) {
                send(VolleyCourtBE.A_DIFF, (t.diffSetting + 1) % 3);
            }
        }).bounds(left + 142, top + 118, 130, 20).build());
        startBtn = addRenderableWidget(Button.builder(Component.translatable("pooltable.voley.menu.start"), b -> {
            send(VolleyCourtBE.A_START, 0);
            onClose();
        }).bounds(left + 8, top + 144, 130, 20).build());
        stopBtn = addRenderableWidget(Button.builder(Component.translatable("pooltable.voley.menu.stop"), b -> send(VolleyCourtBE.A_STOP, 0))
                .bounds(left + 142, top + 144, 130, 20).build());
        leaveBtn = addRenderableWidget(Button.builder(Component.translatable("pooltable.voley.menu.leave"), b -> send(VolleyCourtBE.A_LEAVE, 0))
                .bounds(left + 8, top + 170, 130, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose()).bounds(left + 142, top + 170, 130, 20).build());
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private boolean isMember(VolleyCourtBE t) {
        var p = Minecraft.getInstance().player;
        return p != null && t.lobbyNames.contains(p.getGameProfile().getName());
    }

    @Override
    public void tick() {
        VolleyCourtBE t = be();
        var p = Minecraft.getInstance().player;
        if (t == null || p == null) {
            onClose();
            return;
        }
        var c = t.world(0, 1, 0);
        if (p.distanceToSqr(c.x, c.y, c.z) > 20 * 20) {
            onClose();
            return;
        }
        boolean playing = t.phase == VolleyCourtBE.PLAYING;
        boolean in = isMember(t);
        teamA.active = !playing;
        teamB.active = !playing;
        String[] sizes = {"auto", "1", "2", "3"};
        sizeBtn.setMessage(Component.translatable("pooltable.voley.menu.size", Component.translatable("pooltable.voley.menu.size." + sizes[t.sizeSetting])));
        diffBtn.setMessage(Component.translatable("pooltable.voley.menu.diff", Component.translatable("pooltable.voley.menu.diff." + t.diffSetting)));
        sizeBtn.active = in && !playing;
        diffBtn.active = in && !playing;
        startBtn.active = in && t.phase == VolleyCourtBE.LOBBY;
        stopBtn.active = in && playing;
        leaveBtn.active = in;
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        renderBackground(g);
        g.fill(left - 2, top - 2, left + W + 2, top + H + 2, 0xFFEAF6FF);
        g.fill(left, top, left + W, top + H, 0xFF16405E);
        g.fill(left, top, left + W, top + 18, 0xFF2D8CFF);
        g.fill(left + W / 2, top, left + W, top + 18, 0xFFFF8A1E);
        g.drawCenteredString(font, title, left + W / 2, top + 5, 0xFFFFFF);
        VolleyCourtBE t = be();
        if (t != null) {
            int ya = top + 46;
            int yb = top + 46;
            for (int i = 0; i < t.lobbyNames.size(); i++) {
                boolean a = t.lobbyTeams.get(i) == 0;
                g.drawString(font, "• " + t.lobbyNames.get(i), a ? left + 12 : left + 146, a ? ya : yb, a ? 0xFF8CC8FF : 0xFFFFC27A, false);
                if (a) {
                    ya += 11;
                } else {
                    yb += 11;
                }
            }
            Component st = Component.translatable(switch (t.phase) {
                case VolleyCourtBE.PLAYING -> "pooltable.voley.menu.status.playing";
                case VolleyCourtBE.LOBBY -> "pooltable.voley.menu.status.lobby";
                default -> "pooltable.voley.menu.status.free";
            });
            g.drawCenteredString(font, st, left + W / 2, top + 100, 0xFFFFFF);
        }
        g.drawCenteredString(font, Component.translatable("pooltable.voley.menu.help"), left + W / 2, top + 196, 0xFF9FB8CC);
        super.render(g, mx, my, pt);
    }
}
