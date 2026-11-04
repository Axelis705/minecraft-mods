package com.mundosdeaxelis.client;

import com.mundosdeaxelis.MundosDeAxelis;
import com.mundosdeaxelis.network.FreezeHitPacket;
import com.mundosdeaxelis.network.ModNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.MovementInputUpdateEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Lado del cliente de la congelación: mientras estás atrapado en hielo se ignoran las teclas de movimiento, cada pulsación de
 * salto se manda al servidor (rompe un golpe de hielo) y se dibuja escarcha en los bordes de la pantalla con una barra de
 * progreso que se agita con cada pulsación.
 */
@Mod.EventBusSubscriber(modid = MundosDeAxelis.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class FrozenClient {
    private FrozenClient() {
    }

    private static boolean frozen;
    private static int hits;
    private static int max = 1;
    private static boolean jumpWas;
    private static long ticks;
    private static long lastPress = -100;
    private static long frozenAt;

    public static void set(boolean f, int h, int m) {
        if (f && !frozen) {
            frozenAt = ticks;
            jumpWas = true; // el salto que ya estuviera pulsado no cuenta
        }
        if (f && h < hits && frozen) {
            lastPress = ticks;
        }
        frozen = f;
        hits = h;
        max = Math.max(1, m);
    }

    public static boolean isFrozen() {
        return frozen;
    }

    @SubscribeEvent
    public static void onInput(MovementInputUpdateEvent e) {
        if (frozen) {
            var in = e.getInput();
            in.leftImpulse = 0.0F;
            in.forwardImpulse = 0.0F;
            in.up = false;
            in.down = false;
            in.left = false;
            in.right = false;
            in.jumping = false;
            in.shiftKeyDown = false;
        }
    }

    @SubscribeEvent
    public static void onTick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END || Minecraft.getInstance().isPaused()) {
            return;
        }
        ticks++;
        Minecraft mc = Minecraft.getInstance();
        if (!frozen || mc.player == null || mc.screen != null) {
            jumpWas = mc.options.keyJump.isDown();
            return;
        }
        boolean down = mc.options.keyJump.isDown();
        if (down && !jumpWas) {
            lastPress = ticks;
            ModNetwork.CHANNEL.sendToServer(new FreezeHitPacket());
        }
        jumpWas = down;
    }

    private static int a(float alpha, int rgb) {
        return Mth.clamp((int) (alpha * 255.0F), 0, 255) << 24 | (rgb & 0xFFFFFF);
    }

    public static void render(GuiGraphics g, float pt, int width, int height) {
        Minecraft mc = Minecraft.getInstance();
        if (!frozen || mc.player == null || mc.options.hideGui) {
            return;
        }
        float now = ticks + pt;
        float tight = 1.0F - hits / (float) max; // 0 = hielo nuevo, 1 = a punto de romperse
        float in = Mth.clamp((now - frozenAt) / 8.0F, 0.0F, 1.0F);
        float thick = (0.30F + 0.18F * (1.0F - tight)) * in;
        // escarcha en los cuatro bordes
        int bw = (int) (width * 0.16F * thick / 0.30F);
        int bh = (int) (height * 0.20F * thick / 0.30F);
        int c1 = 0xBFEFFF, c2 = 0x4FA8E8;
        g.fillGradient(0, 0, width, bh, a(0.75F * in, c1), a(0.0F, c2));
        g.fillGradient(0, height - bh, width, height, a(0.0F, c2), a(0.75F * in, c1));
        for (int i = 0; i < bw; i++) {
            float k = 1.0F - i / (float) Math.max(1, bw);
            int al = (int) (k * k * 190 * in);
            g.fill(i, 0, i + 1, height, al << 24 | c1);
            g.fill(width - 1 - i, 0, width - i, height, al << 24 | c1);
        }
        g.fill(0, 0, width, height, a(0.10F * in, 0x7FC8FF));
        // cristales de hielo en las esquinas (rombos que crecen al principio)
        int s = (int) (26 * in);
        for (int k = 0; k < 4; k++) {
            int cx = (k % 2 == 0) ? 0 : width;
            int cy = (k < 2) ? 0 : height;
            for (int i = 0; i < s; i += 3) {
                g.fill(cx - (k % 2 == 0 ? -i : i + 3), cy - (k < 2 ? -i : i + 3), cx - (k % 2 == 0 ? -i - 3 : i), cy - (k < 2 ? -i - 3 : i), a(0.5F * in, 0xFFFFFF));
            }
        }
        // barra de progreso (se agita al pulsar)
        Font f = mc.font;
        int bwid = 150;
        float shake = Mth.clamp(1.0F - (now - lastPress) / 5.0F, 0.0F, 1.0F);
        int sx = (int) (Math.sin(now * 2.7F) * 3.0F * shake);
        int sy = (int) (Math.cos(now * 3.3F) * 2.0F * shake);
        int x = (width - bwid) / 2 + sx;
        int y = height / 2 + 34 + sy;
        String txt = "¡PULSA ESPACIO!";
        int tw = f.width(txt);
        boolean blink = (int) (now / 6.0F) % 2 == 0;
        g.drawString(f, txt, (width - tw) / 2 + sx, y - 14, a(1.0F, blink ? 0xFFFFFF : 0xBFEFFF), true);
        g.fill(x - 2, y - 2, x + bwid + 2, y + 10, a(0.85F, 0x0A1A2C));
        g.fill(x - 1, y - 1, x + bwid + 1, y + 9, a(1.0F, 0x7FD0FF));
        g.fill(x, y, x + bwid, y + 8, a(1.0F, 0x0A1A2C));
        int fillW = (int) ((bwid) * Mth.clamp(tight, 0.0F, 1.0F));
        g.fillGradient(x, y, x + fillW, y + 8, a(1.0F, 0xE8FAFF), a(1.0F, 0x5AB8F0));
        for (int i = 1; i < 10; i++) {
            g.fill(x + bwid * i / 10, y, x + bwid * i / 10 + 1, y + 8, a(0.35F, 0x0A1A2C));
        }
        String sub = "o pídele ayuda a tu compañero (clic derecho)";
        g.drawString(f, sub, (width - f.width(sub)) / 2 + sx, y + 14, a(0.8F, 0xBFD8EE), false);
    }
}
