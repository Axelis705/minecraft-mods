package com.casino.client;

import com.casino.heist.HeistActionPacket;
import com.casino.heist.HeistGamePacket;
import com.casino.net.CasinoNet;
import com.casino.plat.client.CasinoScreen;
import com.casino.plat.client.G;
import com.casino.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.RandomSource;

import java.util.Locale;

/**
 * Los minijuegos del robo, todos de reflejos o de cabeza:
 * <ul>
 * <li>Ganzúa (puerta de servicio): una aguja va y viene; pulsa ESPACIO (o clic) cuando esté en la zona verde para subir cada uno de
 * los 3 pernos. 3 fallos y se rompe la ganzúa (sube la sospecha).</li>
 * <li>Hackeo (consola de cámaras): igual, pero en una terminal, con 4 canales cada vez más rápidos y solo 2 fallos.</li>
 * <li>Saqueo (carrito de la bóveda): un solo intento; en el centro dorado agarras todo, en verde casi todo, fuera solo un poco.</li>
 * <li>Caja fuerte: tres diales de 00 a 99. Las pistas que encontró el equipo salen en dorado; gira cada dial (rueda del ratón o clic
 * arriba/abajo) y abre. Si te equivocas, sube la sospecha.</li>
 * </ul>
 */
public final class HeistGameScreen extends CasinoScreen {
    private final HeistGamePacket d;
    private final RandomSource rnd = RandomSource.create();
    private int age;
    private int stage;
    private int strikes;
    private float zone;
    private float zoneW;
    private boolean sent;
    private int doneAt = -1;
    private int result = -1;
    private int flash;
    private int flashCol;
    private final int[] dial = {50, 50, 50};

    public HeistGameScreen(HeistGamePacket d) {
        super(Component.translatable("casino.heist.game." + d.mode));
        this.d = d;
        newZone();
    }

    private int stages() {
        return d.mode == HeistGamePacket.LOCKPICK ? 3 : d.mode == HeistGamePacket.HACK ? 4 : 1;
    }

    private int maxStrikes() {
        return d.mode == HeistGamePacket.LOCKPICK ? 3 : 2;
    }

    private void newZone() {
        zoneW = d.mode == HeistGamePacket.LOOT ? 0.22F : d.mode == HeistGamePacket.HACK ? 0.15F - stage * 0.015F : 0.2F - stage * 0.03F;
        zone = 0.08F + rnd.nextFloat() * (0.84F - zoneW);
    }

    private float speed() {
        return switch (d.mode) {
            case HeistGamePacket.HACK -> 0.11F + stage * 0.025F;
            case HeistGamePacket.LOOT -> 0.13F;
            default -> 0.085F + stage * 0.025F;
        };
    }

    /** Dónde está la aguja (0..1). */
    private float needle(float pt) {
        return 0.5F + 0.5F * (float) Math.sin((age + pt) * speed());
    }

    @Override
    protected void init() {
        if (d.mode == HeistGamePacket.SAFE) {
            addRenderableWidget(Button.builder(Component.translatable("casino.heist.game.open").withStyle(ChatFormatting.BOLD), b -> {
                send(HeistActionPacket.SAFE, dial[0], dial[1], dial[2]);
                onClose();
            }).bounds(width / 2 - 45, height / 2 + 62, 90, 20).build());
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static void play(SoundEvent s, float pitch) {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(s, pitch, 0.8F));
    }

    private void send(int action, int... v) {
        if (!sent) {
            sent = true;
            CasinoNet.sendToServer(new HeistActionPacket(action, d.pos, v));
        }
    }

    @Override
    public void onClose() {
        if (!sent) {
            sent = true;
            CasinoNet.sendToServer(new HeistActionPacket(HeistActionPacket.CANCEL, d.pos));
        }
        super.onClose();
    }

    @Override
    public void tick() {
        age++;
        if (flash > 0) {
            flash--;
        }
        if (doneAt >= 0 && age - doneAt > 14) {
            int action = d.mode == HeistGamePacket.LOCKPICK ? HeistActionPacket.LOCKPICK : d.mode == HeistGamePacket.HACK ? HeistActionPacket.HACK
                    : HeistActionPacket.LOOT;
            send(action, result);
            onClose();
        }
        if (d.mode == HeistGamePacket.LOOT && d.data.length > 1 && d.data[1] - age <= 0 && doneAt < 0) {
            onClose();
        }
    }

    private void press() {
        if (d.mode == HeistGamePacket.SAFE || doneAt >= 0) {
            return;
        }
        float n = needle(0);
        boolean in = n >= zone && n <= zone + zoneW;
        if (d.mode == HeistGamePacket.LOOT) {
            float c = zone + zoneW / 2;
            result = Math.abs(n - c) < zoneW * 0.18F ? 2 : in ? 1 : 0;
            flash = 10;
            flashCol = result == 2 ? 0xFFFFD45A : result == 1 ? 0xFF50E070 : 0xFFE04040;
            play(result > 0 ? ModSounds.COINS.get() : ModSounds.DENIED.get(), result == 2 ? 1.3F : 1F);
            doneAt = age;
            return;
        }
        if (in) {
            stage++;
            flash = 8;
            flashCol = 0xFF50E070;
            play(ModSounds.SLOT_STOP.get(), 1.2F + stage * 0.15F);
            if (stage >= stages()) {
                result = 1;
                doneAt = age;
                play(ModSounds.WHEEL_WIN.get(), 1.2F);
            } else {
                newZone();
            }
        } else {
            strikes++;
            flash = 8;
            flashCol = 0xFFE04040;
            play(ModSounds.DENIED.get(), 1F);
            if (strikes >= maxStrikes()) {
                result = 0;
                doneAt = age;
            }
        }
    }

    @Override
    protected boolean onKey(int key, int scan, int mods) {
        if (key == 32 || key == 69 || key == 257) {
            press();
            return true;
        }
        if (d.mode == HeistGamePacket.SAFE && key >= 49 && key <= 51) {
            return true;
        }
        return super.onKey(key, scan, mods);
    }

    @Override
    protected boolean onClick(double x, double y, int b) {
        if (d.mode == HeistGamePacket.SAFE) {
            for (int i = 0; i < 3; i++) {
                int cx = width / 2 + (i - 1) * 74, cy = height / 2 + 4;
                if (Math.abs(x - cx) < 30 && Math.abs(y - cy) < 30) {
                    turn(i, (y < cy ? 1 : -1) * (shift() ? 10 : 1));
                    return true;
                }
            }
            return super.onClick(x, y, b);
        }
        if (b == 0) {
            press();
            return true;
        }
        return super.onClick(x, y, b);
    }

    @Override
    protected boolean onScroll(double x, double y, double delta) {
        if (d.mode != HeistGamePacket.SAFE) {
            return false;
        }
        int i = x < width / 2 - 37 ? 0 : x > width / 2 + 37 ? 2 : 1;
        turn(i, (delta > 0 ? 1 : -1) * (shift() ? 10 : 1));
        return true;
    }

    private void turn(int i, int by) {
        dial[i] = Math.floorMod(dial[i] + by, 100);
        play(ModSounds.SLOT_TICK.get(), 0.8F + (dial[i] % 10) * 0.04F);
    }

    // ================================================================== dibujo

    @Override
    protected void draw(G g, int mx, int my, float pt) {
        drawBackground(g, mx, my, pt);
        switch (d.mode) {
            case HeistGamePacket.SAFE -> drawSafe(g);
            case HeistGamePacket.HACK -> drawHack(g, pt);
            case HeistGamePacket.LOOT -> drawLoot(g, pt);
            default -> drawLock(g, pt);
        }
        drawWidgets(g, mx, my, pt);
    }

    private void panel(G g, int x0, int y0, int x1, int y1, int edge, int top, int bottom) {
        g.fill(x0 + 3, y0 + 4, x1 + 3, y1 + 4, 0x80000000);
        g.fill(x0 - 2, y0 - 2, x1 + 2, y1 + 2, edge);
        g.fillGradient(x0, y0, x1, y1, top, bottom);
    }

    private void bar(G g, int x, int y, int w, int h, float pt, boolean loot) {
        g.fill(x - 1, y - 1, x + w + 1, y + h + 1, 0xFF000000);
        g.fill(x, y, x + w, y + h, 0xFF2A2432);
        int zx = x + (int) (zone * w), zw = Math.max(2, (int) (zoneW * w));
        g.fill(zx, y, zx + zw, y + h, 0xFF2F9A4A);
        if (loot) {
            int c = zx + zw / 2, pw = Math.max(2, (int) (zw * 0.36F));
            g.fill(c - pw / 2, y, c + pw / 2, y + h, 0xFFE8B83A);
        }
        float n = doneAt >= 0 ? needle(0) : needle(pt);
        int nx = x + (int) (n * w);
        g.fill(nx - 1, y - 4, nx + 2, y + h + 4, 0xFFFFFFFF);
        g.fill(nx - 3, y - 6, nx + 4, y - 4, 0xFFFFFFFF);
        if (flash > 0) {
            int a = flash * 20;
            g.fill(x, y, x + w, y + h, a << 24 | (flashCol & 0xFFFFFF));
        }
    }

    private void drawLock(G g, float pt) {
        int cx = width / 2, cy = height / 2;
        panel(g, cx - 130, cy - 80, cx + 130, cy + 80, 0xFF6A6470, 0xF0201C26, 0xF012101A);
        Component t = Component.translatable("casino.heist.game.0").withStyle(ChatFormatting.BOLD);
        g.drawString(font, t, cx - font.width(t) / 2, cy - 72, 0xFFFFD45A, true);
        // cilindro de la cerradura con 3 pernos
        g.fill(cx - 70, cy - 46, cx + 70, cy + 6, 0xFFB8902E);
        g.fill(cx - 66, cy - 42, cx + 66, cy + 2, 0xFF8A6A20);
        for (int i = 0; i < 3; i++) {
            int px = cx - 40 + i * 40;
            boolean up = i < stage;
            boolean cur = i == stage;
            int lift = up ? 14 : 0;
            g.fill(px - 7, cy - 38, px + 7, cy - 2, 0xFF3A2A10);
            g.fill(px - 5, cy - 30 - lift, px + 5, cy - 4 - lift, up ? 0xFF7CFF9A : cur ? 0xFFE8E4F0 : 0xFFA8A4B0);
            if (cur && doneAt < 0) {
                g.fill(px - 7, cy - 40, px + 7, cy - 38, 0xFFFFD45A);
            }
        }
        g.fill(cx - 3, cy - 18, cx + 3, cy + 0, 0xFF000000);
        bar(g, cx - 110, cy + 22, 220, 12, pt, false);
        strikes(g, cx, cy + 44);
        hint(g, cx, cy + 60, "casino.heist.game.hint_press");
    }

    private void drawHack(G g, float pt) {
        int cx = width / 2, cy = height / 2;
        panel(g, cx - 140, cy - 84, cx + 140, cy + 84, 0xFF1A4A2A, 0xF0020A04, 0xF0010402);
        for (int y = cy - 84; y < cy + 84; y += 3) {
            g.fill(cx - 140, y, cx + 140, y + 1, 0x10A0FFA0);
        }
        g.drawString(font, "> SECURITY-NET v2.3", cx - 132, cy - 78, 0xFF40FF70, false);
        g.drawString(font, String.format(Locale.ROOT, "> canal %d/%d", Math.min(stage + 1, 4), 4), cx - 132, cy - 68, 0xFF40FF70, false);
        // onda que se mueve
        for (int i = 0; i < 260; i += 2) {
            float tt = (age + pt) * 0.2F + i * 0.08F;
            int y = cy - 30 + (int) (Math.sin(tt) * 12 * Math.sin(i * 0.02 + age * 0.03));
            g.fill(cx - 130 + i, y, cx - 128 + i, y + 2, 0xFF30D060);
        }
        for (int i = 0; i < 4; i++) {
            int bx = cx - 66 + i * 34;
            g.fill(bx, cy - 4, bx + 28, cy + 8, i < stage ? 0xFF30D060 : i == stage ? 0xFF1A5A2A : 0xFF0A2A12);
            g.drawString(font, i < stage ? "OK" : "--", bx + 8, cy - 2, 0xFF02140A, false);
        }
        bar(g, cx - 120, cy + 22, 240, 12, pt, false);
        strikes(g, cx, cy + 44);
        hint(g, cx, cy + 62, "casino.heist.game.hint_hack");
    }

    private void drawLoot(G g, float pt) {
        int cx = width / 2, cy = height / 2;
        int kind = d.data.length > 0 ? d.data[0] : 0;
        int[] col = {0xFF4FA060, 0xFFE8B83A, 0xFF6FE8F0, 0xFFB46CF0};
        panel(g, cx - 120, cy - 56, cx + 120, cy + 56, col[kind % 4], 0xF0201C26, 0xF012101A);
        Component t = Component.translatable("casino.heist.game.loot_" + kind).withStyle(ChatFormatting.BOLD);
        g.drawString(font, t, cx - font.width(t) / 2, cy - 48, col[kind % 4], true);
        int left = d.data.length > 1 ? d.data[1] - age : 0;
        String tm = String.format(Locale.ROOT, "%d:%02d", Math.max(0, left) / 20 / 60, Math.max(0, left) / 20 % 60);
        g.drawString(font, tm, cx - font.width(tm) / 2, cy - 34, left < 400 ? 0xFFFF5050 : 0xFFFFFFFF, true);
        bar(g, cx - 100, cy - 10, 200, 14, pt, true);
        if (doneAt >= 0) {
            Component r = Component.translatable("casino.heist.game.grab_" + result).withStyle(ChatFormatting.BOLD);
            g.push();
            g.translate(cx, cy + 20, 0);
            g.scale(1.5F, 1.5F, 1);
            g.drawString(font, r, -font.width(r) / 2, 0, flashCol, true);
            g.pop();
        } else {
            hint(g, cx, cy + 22, "casino.heist.game.hint_loot");
        }
    }

    private void drawSafe(G g) {
        int cx = width / 2, cy = height / 2;
        panel(g, cx - 130, cy - 70, cx + 130, cy + 90, 0xFF5A5E66, 0xF03A3E46, 0xF0202228);
        Component t = Component.translatable("casino.heist.game.1").withStyle(ChatFormatting.BOLD);
        g.drawString(font, t, cx - font.width(t) / 2, cy - 62, 0xFFFFFFFF, true);
        for (int i = 0; i < 3; i++) {
            int dx = cx + (i - 1) * 74, dy = cy + 4;
            // dial redondo: aro oscuro, aro de acero y centro claro
            int[][] rings = {{30, 0xFF15161A}, {27, 0xFF8A8E96}, {22, 0xFF2A2C32}, {20, 0xFFC4C8D0}};
            for (int[] rc : rings) {
                int r = rc[0];
                for (int row = -r; row <= r; row++) {
                    int hw = (int) Math.round(Math.sqrt(r * r - row * row));
                    g.fill(dx - hw, dy + row, dx + hw, dy + row + 1, rc[1]);
                }
            }
            for (int k = 0; k < 20; k++) {
                double a = (k / 20.0) * Math.PI * 2 + dial[i] * Math.PI * 2 / 100.0;
                int tx = dx + (int) (Math.sin(a) * 25), ty = dy - (int) (Math.cos(a) * 25);
                g.fill(tx, ty, tx + 1, ty + 2, 0xFFFFFFFF);
            }
            String num = String.format(Locale.ROOT, "%02d", dial[i]);
            g.push();
            g.translate(dx, dy - 6, 0);
            g.scale(1.6F, 1.6F, 1);
            g.drawString(font, num, -font.width(num) / 2, 0, 0xFF101014, false);
            g.pop();
            g.fill(dx - 2, dy - 34, dx + 2, dy - 30, 0xFFE04040);
            int known = d.data.length > i ? d.data[i] : -1;
            String k = known >= 0 ? String.format(Locale.ROOT, "%02d", known) : "??";
            g.drawString(font, Component.translatable("casino.heist.game.clue", k), dx - 26, dy + 38, known >= 0 ? 0xFFFFD45A : 0xFF8A8E96, false);
        }
        hint(g, cx, cy - 46, "casino.heist.game.hint_safe");
    }

    private void strikes(G g, int cx, int y) {
        int n = maxStrikes();
        for (int i = 0; i < n; i++) {
            int x = cx - n * 8 + i * 16;
            g.fill(x, y, x + 10, y + 10, i < strikes ? 0xFFE04040 : 0xFF3A3440);
            if (i < strikes) {
                g.drawString(font, "x", x + 2, y + 1, 0xFF200000, false);
            }
        }
    }

    private void hint(G g, int cx, int y, String key) {
        Component h = Component.translatable(key);
        g.drawString(font, h, cx - font.width(h) / 2, y, 0xFFB8BCD0, false);
    }
}
