package com.pooltable.client;

import com.pooltable.block.AirHockeyBE;
import com.pooltable.game.HockeyMatch;
import com.pooltable.game.HockeyPhysics;
import com.pooltable.net.HockeyActionPacket;
import com.pooltable.net.HockeyMovePacket;
import com.pooltable.net.PoolNet;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

/**
 * Los controles del hockey. La mesa se ve en 3D en el mundo, con la cámara de {@link HockeyCam}; esta pantalla solo pone el marcador, los botones y
 * maneja el mazo: mientras se juega el ratón queda oculto y capturado (no puede salirse), y sus movimientos mueven el mazo.
 * Cerrar la pantalla mientras se juega es abandonar el partido.
 */
public final class HockeyScreen extends Screen {
    /** Metros de mesa por píxel de ratón. */
    private static final double SENS = 0.0026;
    public static HockeyScreen current = null;

    private final BlockPos pos;
    private boolean sentLeave = false;
    private boolean grabbed = false;
    private boolean haveLast = false;
    private double lastX;
    private double lastY;
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

    public AirHockeyBE table() {
        Minecraft mc = Minecraft.getInstance();
        return mc.level != null && mc.level.getBlockEntity(pos) instanceof AirHockeyBE t ? t : null;
    }

    private int role() {
        AirHockeyBE t = table();
        var p = Minecraft.getInstance().player;
        return t == null || p == null ? -1 : t.roleOf(p.getUUID());
    }

    private void send(int action, int arg) {
        PoolNet.sendToServer(new HockeyActionPacket(pos, action, arg));
    }

    @Override
    protected void init() {
        current = this;
        AirHockeyBE t = table();
        if (t != null && !HockeyCam.active) {
            HockeyCam.engage(t);
        }
        int cx = width / 2;
        int by = height - 50;
        bEasy = addRenderableWidget(Button.builder(Component.translatable("pooltable.hockey.ui.easy"), b -> send(AirHockeyBE.A_CPU, 0)).bounds(cx - 155, by, 100, 20).build());
        bNormal = addRenderableWidget(Button.builder(Component.translatable("pooltable.hockey.ui.normal"), b -> send(AirHockeyBE.A_CPU, 1)).bounds(cx - 50, by, 100, 20).build());
        bHard = addRenderableWidget(Button.builder(Component.translatable("pooltable.hockey.ui.hard"), b -> send(AirHockeyBE.A_CPU, 2)).bounds(cx + 55, by, 100, 20).build());
        bHost = addRenderableWidget(Button.builder(Component.translatable("pooltable.hockey.ui.host"), b -> send(AirHockeyBE.A_HOST, 0)).bounds(cx - 155, by + 24, 150, 20).build());
        bJoin = addRenderableWidget(Button.builder(Component.translatable("pooltable.hockey.ui.join"), b -> send(AirHockeyBE.A_JOIN, 0)).bounds(cx - 155, by + 24, 150, 20).build());
        bLeave = addRenderableWidget(Button.builder(Component.translatable("pooltable.hockey.ui.leave"), b -> leave()).bounds(cx - 155, by + 24, 150, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("pooltable.hockey.ui.close"), b -> onClose()).bounds(cx + 5, by + 24, 150, 20).build());
        updateButtons();
    }

    private void leave() {
        if (role() >= 0 && !sentLeave) {
            sentLeave = true;
            send(AirHockeyBE.A_LEAVE, 0);
        }
    }

    private void updateButtons() {
        AirHockeyBE t = table();
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
    public void removed() {
        setGrab(false);
        HockeyCam.release();
        AirHockeyBE.localPos = null;
        AirHockeyBE.localRole = -1;
        if (current == this) {
            current = null;
        }
        super.removed();
    }

    /** Oculta y captura el ratón (o lo suelta). Con el cursor deshabilitado el ratón no puede salirse de la ventana. */
    private void setGrab(boolean on) {
        if (grabbed == on) {
            return;
        }
        grabbed = on;
        haveLast = false;
        Minecraft mc = Minecraft.getInstance();
        long w = mc.getWindow().getWindow();
        double cx = mc.getWindow().getScreenWidth() / 2.0;
        double cy = mc.getWindow().getScreenHeight() / 2.0;
        InputConstants.grabOrReleaseMouse(w, on ? 212995 : 212993, cx, cy);
    }

    private boolean playing(AirHockeyBE t, int role) {
        if (role < 0) {
            return false;
        }
        return switch (t.match.state) {
            case COUNTDOWN, PLAYING, GOAL -> true;
            default -> false;
        };
    }

    @Override
    public void tick() {
        AirHockeyBE t = table();
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
        boolean pl = playing(t, role);
        if (role >= 0 && !pos.equals(AirHockeyBE.localPos)) {
            AirHockeyBE.localPos = pos;
            AirHockeyBE.localRole = role;
            AirHockeyBE.localX = t.cmx[role];
            AirHockeyBE.localZ = t.cmz[role];
        }
        setGrab(pl);
        if (role >= 0) {
            PoolNet.sendToServer(new HockeyMovePacket(pos, (float) AirHockeyBE.localX, (float) AirHockeyBE.localZ));
        }
    }

    @Override
    public void mouseMoved(double x, double y) {
        if (!grabbed) {
            return;
        }
        int role = role();
        if (role < 0) {
            return;
        }
        if (!haveLast) {
            lastX = x;
            lastY = y;
            haveLast = true;
            return;
        }
        double scale = Minecraft.getInstance().getWindow().getGuiScale();
        double dx = Math.max(-300, Math.min(300, (x - lastX) * scale));
        double dy = Math.max(-300, Math.min(300, (y - lastY) * scale));
        lastX = x;
        lastY = y;
        // la cámara mira la mesa desde un lado: derecha de la pantalla = hacia el lado B, abajo = hacia el jugador
        double nx = AirHockeyBE.localX + dx * SENS;
        double nz = AirHockeyBE.localZ + dy * SENS;
        AirHockeyBE.localX = Math.max(HockeyPhysics.minX(role), Math.min(HockeyPhysics.maxX(role), nx));
        AirHockeyBE.localZ = Math.max(-HockeyPhysics.HW + HockeyPhysics.MALLET_R, Math.min(HockeyPhysics.HW - HockeyPhysics.MALLET_R, nz));
    }

    /** Un rectángulo con las esquinas redondeadas (a base de rectángulos). */
    private static void round(GuiGraphics g, int x, int y, int w, int h, int color) {
        g.fill(x + 3, y, x + w - 3, y + h, color);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, color);
        g.fill(x, y + 3, x + w, y + h - 3, color);
    }

    /** El marcador tipo píldora: tu lado en lima, el rival en magenta, el marcador en el centro. */
    private void drawPill(GuiGraphics g, int cx, int y, String nameA, String nameB, int sa, int sb) {
        int wTotal = 260;
        int h = 28;
        int x = cx - wTotal / 2;
        round(g, x - 2, y - 2, wTotal + 4, h + 4, 0xAAFFFFFF);
        round(g, x, y, wTotal, h, 0xFF16405E);
        round(g, x + 3, y + 3, wTotal / 2 - 3 - 22, h - 6, 0xFF7AB82A);
        round(g, x + wTotal / 2 + 22, y + 3, wTotal / 2 - 3 - 22, h - 6, 0xFFB02A86);
        round(g, cx - 22, y + 2, 44, h - 4, 0xFFEAF6FF);
        g.fill(x + 6, y + 4, x + wTotal / 2 - 24, y + 7, 0x55FFFFFF);
        g.fill(x + wTotal / 2 + 24, y + 4, x + wTotal - 6, y + 7, 0x55FFFFFF);
        String a = font.plainSubstrByWidth(nameA, 90);
        String b = font.plainSubstrByWidth(nameB, 90);
        g.drawString(font, a, x + 10, y + 10, 0xFFFFFFFF, true);
        g.drawString(font, b, x + wTotal - 10 - font.width(b), y + 10, 0xFFFFFFFF, true);
        String score = sa + " - " + sb;
        g.pose().pushPose();
        g.pose().translate(cx, y + 5, 0);
        g.pose().scale(1.5F, 1.5F, 1.0F);
        g.drawString(font, score, -font.width(score) / 2, 0, 0xFF16405E, false);
        g.pose().popPose();
        String first = Component.translatable("pooltable.hockey.ui.first", HockeyMatch.WIN).getString();
        g.pose().pushPose();
        g.pose().translate(cx, y + h - 6, 0);
        g.pose().scale(0.5F, 0.5F, 1.0F);
        g.drawString(font, first, -font.width(first) / 2, 0, 0xFF5E86A4, false);
        g.pose().popPose();
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        AirHockeyBE t = table();
        int cx = width / 2;
        if (t == null) {
            super.render(g, mx, my, pt);
            return;
        }
        int role = role();
        String nameA = t.pname[0].isEmpty() ? "-" : t.pname[0];
        String nameB = t.pname[1].isEmpty() ? "-" : t.pname[1];
        String you = Component.translatable("pooltable.hockey.ui.you").getString();
        drawPill(g, cx, 6, nameA + (role == 0 ? " " + you : ""), nameB + (role == 1 ? " " + you : ""), t.match.scoreA, t.match.scoreB);

        Component big = null;
        int bigColor = 0xFFFFD84A;
        switch (t.match.state) {
            case COUNTDOWN -> big = Component.literal(String.valueOf(Math.max(1, (int) Math.ceil(t.timer))));
            case GOAL -> {
                big = Component.translatable("pooltable.hockey.ui.goal");
                bigColor = 0xFFB4F03C;
            }
            case OVER -> {
                big = Component.translatable("pooltable.hockey.ui.wins", t.match.scoreA > t.match.scoreB ? nameA : nameB);
                bigColor = 0xFF7CFFB0;
            }
            case WAITING -> big = Component.translatable("pooltable.hockey.ui.waiting");
            default -> {
            }
        }
        if (big != null) {
            // el texto "salta" al aparecer: crece pasando de su tamaño y se asienta
            double el = t.match.state == HockeyMatch.State.GOAL ? Math.max(0, 1.6 - t.timer) : t.match.state == HockeyMatch.State.COUNTDOWN ? 1.0 - (t.timer - Math.floor(t.timer)) : 1.0;
            float k = (float) Math.min(1.0, el / 0.35);
            float c1 = 1.70158F;
            float pop = 1.0F + (c1 + 1.0F) * (float) Math.pow(k - 1.0F, 3) + c1 * (float) Math.pow(k - 1.0F, 2);
            float sc = 3.2F * Math.max(0.2F, pop);
            g.pose().pushPose();
            g.pose().translate(cx, 52, 0);
            g.pose().scale(sc, sc, 1.0F);
            String str = big.getString();
            int w2 = font.width(str);
            int x0 = -w2 / 2;
            for (int[] d : new int[][] {{-1, 0}, {1, 0}, {0, -1}, {0, 1}, {1, 1}}) {
                g.drawString(font, str, x0 + d[0], d[1], 0xFF123A5C, false);
            }
            g.drawString(font, str, x0, 0, bigColor, false);
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
        if (!grabbed) {
            g.drawCenteredString(font, hint, cx, height - 64, 0xFFE0E6EC);
        } else {
            g.drawCenteredString(font, hint, cx, height - 14, 0xFFB0B8C0);
        }
        super.render(g, mx, my, pt);
    }
}
