package com.mundosdeaxelis.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Axis;
import com.mundosdeaxelis.MundosDeAxelis;
import com.mundosdeaxelis.lock.LockGame;
import com.mundosdeaxelis.network.LockpickResultPacket;
import com.mundosdeaxelis.network.ModNetwork;
import com.mundosdeaxelis.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * La cerradura de un cofre: una aguja gira alrededor de la rueda y hay que pulsar justo cuando pasa por una clavija roja
 * (clic, Espacio o Intro). Las reglas están en {@link LockGame}; esta pantalla solo dibuja, suena y recoge las pulsaciones.
 * Al terminar avisa al servidor (que es quien abre el cofre).
 */
public class LockpickScreen extends Screen {
    private static final ResourceLocation TEX = new ResourceLocation(MundosDeAxelis.MODID, "textures/gui/lockpick.png");
    private static final int ACCENT = 0xC8A24A;
    private static final int CW = 372;
    private static final int CH = 232;

    private final BlockPos pos;
    private final LockGame game;
    private boolean sent = false;
    private double last = -1.0D;
    private double endTimer = 0.0D;
    private double shake = 0.0D;
    private double flashT = 0.0D;
    private int flashKind = 0;
    private double flashAngle = 0.0D;
    private double carouselFx = 0.0D;

    public LockpickScreen(BlockPos pos, int level, long seed) {
        super(Component.literal("Cerradura"));
        this.pos = pos;
        this.game = new LockGame(level, seed);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // ------------------------------------------------------------------ entrada

    private void press() {
        if (game.state != LockGame.State.PLAYING) {
            return;
        }
        flashAngle = game.tip() + game.fieldRot;
        game.click();
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0 || button == 1) {
            press();
            return true;
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (key == GLFW.GLFW_KEY_SPACE || key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
            press();
            return true;
        }
        return super.keyPressed(key, scan, mods);
    }

    private void finish(int code) {
        if (sent) {
            return;
        }
        sent = true;
        ModNetwork.CHANNEL.sendToServer(new LockpickResultPacket(pos, code));
        if (this.minecraft != null) {
            this.minecraft.setScreen(null);
        }
    }

    @Override
    public void onClose() {
        finish(game.state == LockGame.State.WON ? 1 : game.state == LockGame.State.LOST ? 2 : 0);
    }

    // ------------------------------------------------------------------ dibujo

    private void handleEvents() {
        for (LockGame.Event e : game.events) {
            switch (e) {
                case PIN -> {
                    UiFx.lock(ModSounds.LOCK_PIN, 0.9F + 0.04F * game.openedCount(), 0.9F);
                    flashKind = 1;
                    flashT = 0.35D;
                }
                case MISS -> {
                    UiFx.lock(ModSounds.LOCK_MISS, 1.0F, 0.9F);
                    flashKind = 2;
                    flashT = 0.35D;
                    shake = 0.3D;
                }
                case FAKE -> {
                    UiFx.lock(ModSounds.LOCK_FAKE, 1.0F, 0.9F);
                    flashKind = 3;
                    flashT = 0.45D;
                    shake = 0.2D;
                }
                case CAROUSEL -> {
                    UiFx.lock(ModSounds.LOCK_TURN, 1.0F, 0.8F);
                    carouselFx = 0.4D;
                }
                case WIN -> UiFx.lock(ModSounds.LOCK_OPEN, 1.0F, 1.0F);
                case LOSE -> UiFx.error();
            }
        }
        game.events.clear();
    }

    private static String clock(double sec) {
        int s = (int) Math.ceil(sec);
        return String.format("%02d:%02d", s / 60, s % 60);
    }

    private void blit(GuiGraphics g, int x, int y, int u, int v, int w, int h) {
        g.blit(TEX, x, y, (float) u, (float) v, w, h, 256, 256);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        double now = UiFx.time();
        double dt = last < 0 ? 0.0D : Math.min(0.1D, now - last);
        last = now;
        game.update(dt);
        handleEvents();
        shake = Math.max(0.0D, shake - dt);
        flashT = Math.max(0.0D, flashT - dt);
        carouselFx = Math.max(0.0D, carouselFx - dt);
        if (game.state != LockGame.State.PLAYING) {
            endTimer += dt;
            if (endTimer > 1.2D) {
                finish(game.state == LockGame.State.WON ? 1 : 2);
                return;
            }
        }

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        g.fillGradient(0, 0, this.width, this.height, 0xB0070A12, 0xD0101420);
        float s = Math.max(0.45F, Math.min(1.0F, (this.height - 20) / (float) CH));
        float ox = (this.width / s - CW) / 2.0F;
        float oy = (this.height / s - CH) / 2.0F;
        g.pose().pushPose();
        g.pose().scale(s, s, 1.0F);
        g.pose().translate(ox, oy, 0.0F);

        PanelUi.frame(g, 0, 0, CW, CH, ACCENT);
        PanelUi.title(g, this.font, Component.literal("Cerradura · Nivel " + game.level), 14, 9, ACCENT);
        g.drawString(this.font, Component.literal("Pulsa cuando la aguja pase por una clavija roja").withStyle(ChatFormatting.GRAY), 14, 21, 0xFFB8C0D0, false);

        // ---- rueda
        int fcx = 14 + 80;
        int fcy = 38 + 80;
        double sx = shake > 0 ? Math.sin(now * 90.0D) * 3.0D * (shake / 0.3D) : 0.0D;
        double sy = shake > 0 ? Math.cos(now * 77.0D) * 2.0D * (shake / 0.3D) : 0.0D;
        g.pose().pushPose();
        g.pose().translate(fcx + sx, fcy + sy, 0.0F);
        g.pose().pushPose();
        g.pose().mulPose(Axis.ZP.rotationDegrees((float) game.fieldRot));
        blit(g, -80, -80, 0, 0, 160, 160);
        for (int i = 0; i < LockGame.SLOTS; i++) {
            boolean real = game.hasPin[i];
            boolean fake = i == game.fakeSlot;
            if (!real && !fake) {
                continue;
            }
            g.pose().pushPose();
            g.pose().mulPose(Axis.ZP.rotationDegrees((float) (i * LockGame.SLOT_DEG)));
            int u = fake ? 192 : (game.opened[i] ? 176 : 160);
            blit(g, -6, -57, u, 0, 12, 44);
            g.pose().popPose();
        }
        g.pose().popPose();
        // aguja (no gira con el campo)
        g.pose().pushPose();
        g.pose().mulPose(Axis.ZP.rotationDegrees((float) game.needle));
        blit(g, -8, -64, 208, 0, 16, 72);
        g.pose().popPose();
        // destello donde se pulsó
        if (flashT > 0) {
            int col = flashKind == 1 ? 0x60E070 : flashKind == 2 ? 0xFF4050 : 0xE050C0;
            int al = (int) (200 * (flashT / 0.45D));
            g.pose().pushPose();
            g.pose().mulPose(Axis.ZP.rotationDegrees((float) flashAngle));
            int r = (int) (4 + (1.0D - flashT / 0.45D) * 8);
            g.fill(-r, -62 - r, r, -62 + r, (Math.min(255, al) << 24) | col);
            g.pose().popPose();
        }
        g.pose().popPose();

        // ---- corazones
        int hx = fcx - (game.maxLives * 15 - 4) / 2;
        for (int i = 0; i < game.maxLives; i++) {
            blit(g, hx + i * 15, 38 + 160 + 12, i < game.lives ? 160 : 176, 48, 11, 11);
        }

        // ---- tarjetas de modificadores
        int rx = 14 + 160 + 22;
        int rw = CW - rx - 14;
        int y = 38;
        List<Object[]> cards = new ArrayList<>();
        if (game.mods.isEmpty()) {
            cards.add(new Object[]{112, 96, "Cerradura sencilla", "Abre todas las clavijas antes de que se acabe el tiempo. No pierdas corazones."});
        }
        if (game.mods.contains(LockGame.Mod.CAROUSEL)) {
            cards.add(new Object[]{160, 96, "Carrusel mareante", "Cada 5 segundos el campo de juego gira 90° en una dirección al azar."});
        }
        if (game.mods.contains(LockGame.Mod.FAKE)) {
            cards.add(new Object[]{176, 96, "Clavija falsa", "Entre las demás aparece una falsa. Pulsarla vuelve a cerrar todas las abiertas, y cambia de sitio con cada pulsación."});
        }
        if (game.mods.contains(LockGame.Mod.MOOD)) {
            cards.add(new Object[]{192, 96, "Cambios de humor", "Tras cada acierto, la aguja cambia de sentido."});
        }
        for (Object[] c : cards) {
            int iconU = (Integer) c[0];
            if (iconU == 112) {
                iconU = 224;
            }
            String title = (String) c[2];
            List<FormattedCharSequence> lines = this.font.split(Component.literal((String) c[3]), rw - 10);
            int bodyH = lines.size() * 10 + 8;
            g.fill(rx - 1, y - 1, rx + rw + 1, y + 20 + bodyH + 1, 0xFF0A0E1A);
            g.fill(rx, y, rx + rw, y + 20, 0xFF1A2236);
            g.fill(rx, y + 19, rx + rw, y + 20, 0xFF3C4C70);
            blit(g, rx + 3, y + 2, iconU, 96, 16, 16);
            g.drawString(this.font, title, rx + 24, y + 6, 0xFFEAF0FF, true);
            g.fillGradient(rx, y + 20, rx + rw, y + 20 + bodyH, 0xFF9A5A2C, 0xFF7C4620);
            for (int k = 0; k < lines.size(); k++) {
                g.drawString(this.font, lines.get(k), rx + 5, y + 24 + k * 10, 0xFFFFF4E0, true);
            }
            y += 20 + bodyH + 6;
        }

        // ---- datos de abajo
        int cy2 = 38 + 160 + 6;
        int chipW = (rw - 8) / 3;
        String[] txt = {LockGame.speedPct(game.level) + "%", clock(game.timeLeft), game.openedCount() + "/" + game.totalPins};
        int[] icons = {224, 208, 160};
        int[] icv = {96, 96, 112};
        for (int i = 0; i < 3; i++) {
            int x = rx + i * (chipW + 4);
            g.fill(x - 1, cy2 - 1, x + chipW + 1, cy2 + 21, 0xFF0A0E1A);
            g.fill(x, cy2, x + chipW, cy2 + 20, 0xFF1A2236);
            blit(g, x + 3, cy2 + 2, icons[i], icv[i], 16, 16);
            int col = (i == 1 && game.timeLeft < 6.0D && ((int) (now * 4) % 2 == 0)) ? 0xFFFF6060 : 0xFFEAF0FF;
            g.drawString(this.font, txt[i], x + 22, cy2 + 6, col, true);
        }
        g.drawString(this.font, Component.literal("Clic / Espacio: abrir clavija   ·   Esc: salir").withStyle(ChatFormatting.DARK_GRAY),
                14, CH - 14, 0xFF8890A8, false);

        // ---- final
        if (game.state != LockGame.State.PLAYING) {
            boolean won = game.state == LockGame.State.WON;
            int a = (int) Math.min(170, endTimer * 400);
            g.fill(14, 38 + 62, 14 + 160, 38 + 98, (a << 24) | 0x05080E);
            g.drawCenteredString(this.font, won ? "¡Abierta!" : "La cerradura se atasca", fcx, 38 + 76, won ? 0xFF7CF08C : 0xFFFF7070);
        }
        g.pose().popPose();
    }
}
