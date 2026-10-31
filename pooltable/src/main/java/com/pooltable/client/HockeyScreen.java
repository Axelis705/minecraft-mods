package com.pooltable.client;

import com.pooltable.block.AirHockeyBE;
import com.pooltable.game.HockeyMatch;
import com.pooltable.game.HockeyPhysics;
import com.pooltable.net.HockeyActionPacket;
import com.pooltable.net.HockeyMovePacket;
import com.pooltable.net.PoolNet;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

/**
 * La cancha de hockey vista desde arriba. El mazo del jugador sigue al ratón (sin pasar del centro); el disco y el mazo rival vienen del servidor.
 * Cada jugador ve su propio lado a la izquierda. Cerrar la pantalla mientras se juega es abandonar el partido.
 */
public final class HockeyScreen extends Screen {
    private static final int W = 420;
    private static final int H = 336;
    private static final double S = 140.0;

    private final BlockPos pos;
    private int left;
    private int top;
    private int cx;
    private int cy;
    private double mouseX;
    private double mouseY;
    private boolean sentLeave = false;
    private Button bEasy;
    private Button bNormal;
    private Button bHard;
    private Button bHost;
    private Button bJoin;
    private Button bLeave;

    public HockeyScreen(BlockPos pos) {
        super(Component.translatable("block.pooltable.air_hockey"));
        this.pos = pos;
    }

    private AirHockeyBE be() {
        Minecraft mc = Minecraft.getInstance();
        return mc.level != null && mc.level.getBlockEntity(pos) instanceof AirHockeyBE t ? t : null;
    }

    private int role() {
        AirHockeyBE t = be();
        var p = Minecraft.getInstance().player;
        return t == null || p == null ? -1 : t.roleOf(p.getUUID());
    }

    private void send(int action, int arg) {
        PoolNet.sendToServer(new HockeyActionPacket(pos, action, arg));
    }

    @Override
    protected void init() {
        left = (width - W) / 2;
        top = Math.max(4, (height - H) / 2);
        cx = left + W / 2;
        cy = top + 50 + (int) (HockeyPhysics.HW * S);
        int by = top + H - 44;
        bEasy = addRenderableWidget(Button.builder(Component.translatable("pooltable.hockey.ui.easy"), b -> send(AirHockeyBE.A_CPU, 0)).bounds(left + 8, by, 128, 20).build());
        bNormal = addRenderableWidget(Button.builder(Component.translatable("pooltable.hockey.ui.normal"), b -> send(AirHockeyBE.A_CPU, 1)).bounds(left + 146, by, 128, 20).build());
        bHard = addRenderableWidget(Button.builder(Component.translatable("pooltable.hockey.ui.hard"), b -> send(AirHockeyBE.A_CPU, 2)).bounds(left + 284, by, 128, 20).build());
        bHost = addRenderableWidget(Button.builder(Component.translatable("pooltable.hockey.ui.host"), b -> send(AirHockeyBE.A_HOST, 0)).bounds(left + 8, by + 22, 200, 20).build());
        bJoin = addRenderableWidget(Button.builder(Component.translatable("pooltable.hockey.ui.join"), b -> send(AirHockeyBE.A_JOIN, 0)).bounds(left + 8, by + 22, 200, 20).build());
        bLeave = addRenderableWidget(Button.builder(Component.translatable("pooltable.hockey.ui.leave"), b -> leave()).bounds(left + 8, by + 22, 200, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("pooltable.hockey.ui.close"), b -> onClose()).bounds(left + 212, by + 22, 200, 20).build());
        updateButtons();
    }

    private void leave() {
        if (role() >= 0 && !sentLeave) {
            sentLeave = true;
            send(AirHockeyBE.A_LEAVE, 0);
        }
    }

    private void updateButtons() {
        AirHockeyBE t = be();
        if (t == null || bEasy == null) {
            return;
        }
        boolean idle = t.match.state == HockeyMatch.State.IDLE;
        int role = role();
        boolean waiting = t.match.state == HockeyMatch.State.WAITING;
        bEasy.visible = idle;
        bNormal.visible = idle;
        bHard.visible = idle;
        bHost.visible = idle;
        bJoin.visible = waiting && role < 0;
        bLeave.visible = role >= 0;
        bLeave.setMessage(Component.translatable(waiting ? "pooltable.hockey.ui.cancel" : "pooltable.hockey.ui.leave"));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        leave();
        super.onClose();
    }

    @Override
    public void tick() {
        AirHockeyBE t = be();
        var p = Minecraft.getInstance().player;
        if (t == null || p == null) {
            super.onClose();
            return;
        }
        var c = t.center();
        if (p.distanceToSqr(c.x, c.y, c.z) > 10 * 10) {
            onClose();
            return;
        }
        int role = role();
        if (role < 0) {
            sentLeave = false;
        }
        updateButtons();
        if (role >= 0) {
            double[] w = mouseWorld(role);
            PoolNet.sendToServer(new HockeyMovePacket(pos, (float) w[0], (float) w[1]));
        }
    }

    private static double sign(int role) {
        return role == 1 ? -1.0 : 1.0;
    }

    /** El ratón en el marco de la mesa, dentro de la mitad y el ancho del jugador. */
    private double[] mouseWorld(int role) {
        double sg = sign(role);
        double x = sg * (mouseX - cx) / S;
        double z = sg * (mouseY - cy) / S;
        x = Math.max(HockeyPhysics.minX(role), Math.min(HockeyPhysics.maxX(role), x));
        z = Math.max(-HockeyPhysics.HW + HockeyPhysics.MALLET_R, Math.min(HockeyPhysics.HW - HockeyPhysics.MALLET_R, z));
        return new double[] {x, z};
    }

    private static void disc(GuiGraphics g, double x, double y, double r, int color) {
        int ri = (int) Math.ceil(r);
        int yc = (int) Math.round(y);
        for (int dy = -ri; dy <= ri; dy++) {
            double w = Math.sqrt(Math.max(0.0, r * r - dy * dy));
            g.fill((int) Math.round(x - w), yc + dy, (int) Math.round(x + w), yc + dy + 1, color);
        }
    }

    private static void ring(GuiGraphics g, double x, double y, double r, int color) {
        for (int a = 0; a < 360; a += 2) {
            int px = (int) Math.round(x + Math.cos(Math.toRadians(a)) * r);
            int py = (int) Math.round(y + Math.sin(Math.toRadians(a)) * r);
            g.fill(px, py, px + 1, py + 1, color);
        }
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        mouseX = mx;
        mouseY = my;
        renderBackground(g);
        AirHockeyBE t = be();
        g.fill(left - 2, top - 2, left + W + 2, top + H + 2, 0xFF1E2A36);
        g.fill(left, top, left + W, top + H, 0xFF101820);
        g.fill(left, top, left + W, top + 18, 0xFF8C2B2B);
        g.drawCenteredString(font, title, cx, top + 5, 0xFFFFFF);
        if (t == null) {
            super.render(g, mx, my, pt);
            return;
        }
        int role = role();
        double sg = sign(role);
        int rw = (int) (2 * HockeyPhysics.HL * S);
        int rh = (int) (2 * HockeyPhysics.HW * S);
        int rx0 = cx - rw / 2;
        int ry0 = cy - rh / 2;

        // marcador: A a la izquierda, B a la derecha (siempre en ese orden)
        String nameA = t.pname[0].isEmpty() ? "-" : t.pname[0];
        String nameB = t.pname[1].isEmpty() ? "-" : t.pname[1];
        Component sa = Component.literal(nameA + (role == 0 ? " " + Component.translatable("pooltable.hockey.ui.you").getString() : "")).withStyle(ChatFormatting.RED);
        Component sb = Component.literal(nameB + (role == 1 ? " " + Component.translatable("pooltable.hockey.ui.you").getString() : "")).withStyle(ChatFormatting.BLUE);
        g.drawString(font, sa, left + 10, top + 24, 0xFFFFFF, true);
        g.drawString(font, sb, left + W - 10 - font.width(sb), top + 24, 0xFFFFFF, true);
        g.pose().pushPose();
        g.pose().translate(cx, top + 22, 0);
        g.pose().scale(2.0F, 2.0F, 1.0F);
        String score = t.match.scoreA + " : " + t.match.scoreB;
        g.drawString(font, score, -font.width(score) / 2, 0, 0xFFFFFF, true);
        g.pose().popPose();

        // la cancha
        g.fill(rx0 - 6, ry0 - 6, rx0 + rw + 6, ry0 + rh + 6, 0xFF5A3A1E);
        g.fill(rx0, ry0, rx0 + rw, ry0 + rh, 0xFFD6EEF8);
        int gh = (int) (HockeyPhysics.GOAL_HALF * S);
        // porterías (la del jugador, a la izquierda)
        g.fill(rx0 - 6, cy - gh, rx0, cy + gh, 0xFF05080C);
        g.fill(rx0 + rw, cy - gh, rx0 + rw + 6, cy + gh, 0xFF05080C);
        g.fill(cx - 1, ry0, cx + 1, ry0 + rh, 0xFFD83A3A);
        ring(g, cx, cy, 0.28 * S, 0xFFD83A3A);
        ring(g, rx0, cy, 0.34 * S, 0xFF6AA6D8);
        ring(g, rx0 + rw, cy, 0.34 * S, 0xFF6AA6D8);

        // mazos: el propio sigue al ratón al instante; el otro viene del servidor
        for (int s = 0; s < 2; s++) {
            double x = t.cmx[s];
            double z = t.cmz[s];
            if (s == role) {
                double[] w = mouseWorld(role);
                x = w[0];
                z = w[1];
            }
            double sx = cx + sg * x * S;
            double sy = cy + sg * z * S;
            int col = s == 0 ? 0xFFE03030 : 0xFF3060E0;
            int light = s == 0 ? 0xFFFF9090 : 0xFF90B0FF;
            disc(g, sx, sy, HockeyPhysics.MALLET_R * S, 0xFF202020);
            disc(g, sx, sy, HockeyPhysics.MALLET_R * S - 1.5, col);
            disc(g, sx, sy, HockeyPhysics.MALLET_R * S * 0.45, light);
        }
        if (t.clientInit || t.match.state != HockeyMatch.State.IDLE) {
            double[] pk = t.puckNow();
            double sx = cx + sg * pk[0] * S;
            double sy = cy + sg * pk[1] * S;
            if (t.match.state != HockeyMatch.State.IDLE && t.match.state != HockeyMatch.State.WAITING) {
                disc(g, sx, sy, HockeyPhysics.PUCK_R * S, 0xFF101010);
                ring(g, sx, sy, HockeyPhysics.PUCK_R * S * 0.6, 0xFFAAAAAA);
            }
        }

        // mensajes grandes
        Component big = null;
        switch (t.match.state) {
            case COUNTDOWN -> big = Component.literal(String.valueOf(Math.max(1, (int) Math.ceil(t.timer)))).withStyle(ChatFormatting.GOLD);
            case GOAL -> big = Component.translatable("pooltable.hockey.ui.goal").withStyle(ChatFormatting.GOLD);
            case OVER -> big = Component.translatable("pooltable.hockey.ui.wins", t.match.scoreA > t.match.scoreB ? nameA : nameB).withStyle(ChatFormatting.GREEN);
            case WAITING -> big = Component.translatable("pooltable.hockey.ui.waiting").withStyle(ChatFormatting.YELLOW);
            default -> {
            }
        }
        if (big != null) {
            g.pose().pushPose();
            g.pose().translate(cx, cy - 8, 0);
            g.pose().scale(3.0F, 3.0F, 1.0F);
            g.drawString(font, big, -font.width(big) / 2, 0, 0xFFFFFF, true);
            g.pose().popPose();
        }
        Component hint;
        if (t.match.state == HockeyMatch.State.IDLE) {
            hint = Component.translatable("pooltable.hockey.ui.choose");
        } else if (role < 0) {
            hint = Component.translatable("pooltable.hockey.ui.spectating");
        } else {
            hint = Component.translatable("pooltable.hockey.ui.hint", HockeyMatch.WIN);
        }
        g.drawCenteredString(font, hint, cx, ry0 + rh + 12, 0xFFB0B8C0);
        super.render(g, mx, my, pt);
    }
}
