package com.autoscoleccion.client;

import com.autoscoleccion.AutosMod;
import com.autoscoleccion.net.RaceStatePacket;
import com.autoscoleccion.net.TrackInfoPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

/** La carrera vista desde el cliente: lo que manda el servidor y el letrero de arriba (vuelta, lugar, tiempo, cuenta regresiva). */
@Mod.EventBusSubscriber(modid = AutosMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class RaceClient {
    private static RaceStatePacket state;
    private static long stateAt;
    private static int lastCount = -1;
    private static int lastLap = 1;
    private static boolean lastFinished;

    private RaceClient() {
    }

    public static void onTrackInfo(TrackInfoPacket m) {
        Minecraft mc = Minecraft.getInstance();
        if (m.open()) {
            mc.setScreen(new TrackScreen(m));
        } else if (mc.screen instanceof TrackScreen s && s.pos().equals(m.pos())) {
            s.update(m);
        }
    }

    public static void onState(RaceStatePacket m) {
        if (m.phase() < 0) {
            state = null;
            lastCount = -1;
            lastLap = 1;
            lastFinished = false;
            return;
        }
        if (state == null || m.lap() > lastLap) {
            if (state != null && m.phase() == 1) {
                Sfx.play("tally", 1.2F, 0.8F);
            }
        }
        lastLap = m.lap();
        if (m.finished() && !lastFinished) {
            Sfx.play("result", 1.0F, 0.9F);
        }
        lastFinished = m.finished();
        state = m;
        stateAt = System.currentTimeMillis();
    }

    /** Durante la cuenta regresiva el auto no se mueve. */
    public static boolean frozen() {
        return state != null && state.phase() == 0;
    }

    public static boolean racing() {
        return state != null;
    }

    @SubscribeEvent
    public static void onHud(RenderGuiEvent.Post e) {
        RaceStatePacket s = state;
        if (s == null) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        GuiGraphics g = e.getGuiGraphics();
        int W = mc.getWindow().getGuiScaledWidth(), H = mc.getWindow().getGuiScaledHeight();
        long now = System.currentTimeMillis();
        float cx = W / 2.0F;
        // panel de arriba
        Gfx.begin(g, false);
        Gfx.round(cx - 110, 4, cx + 110, 34, 6, 0xC00B0B14);
        Gfx.end();
        Gfx.text(g, mc.font, "Vuelta " + s.lap() + "/" + s.laps(), cx - 70, 10, 1.0F, 0xFFFFFFFF);
        String place = s.place() + "º de " + s.total();
        Gfx.text(g, mc.font, place, cx, 9, 1.4F, s.place() == 1 ? 0xFFFFD34D : 0xFFFFFFFF);
        int ms = s.timeMs() + (s.phase() == 1 && !s.finished() ? (int) Math.min(250, now - stateAt) : 0);
        Gfx.text(g, mc.font, String.format("%d:%02d.%d", ms / 60000, (ms / 1000) % 60, (ms / 100) % 10), cx + 72, 10, 1.0F, 0xFF9AE6FF);
        Gfx.text(g, mc.font, "Mini pista", cx, 24, 0.6F, 0x90FFFFFF);
        // tabla a la izquierda
        List<String> board = s.board();
        Gfx.begin(g, false);
        Gfx.round(4, 40, 128, 46 + board.size() * 10, 4, 0x900B0B14);
        Gfx.end();
        for (int i = 0; i < board.size(); i++) {
            Gfx.textL(g, mc.font, board.get(i), 8, 44 + i * 10, 0.75F, i == s.me() ? 0xFFFFD34D : 0xFFE6E6F0);
        }
        // cuenta regresiva
        if (s.phase() == 0) {
            int secs = (int) Math.ceil(s.timer() / 20.0);
            if (secs <= 3 && secs != lastCount) {
                lastCount = secs;
                Sfx.play("count", 1.0F, 0.9F);
            }
            String txt = secs > 3 ? "¡Prepárate!" : String.valueOf(secs);
            float pulse = 1.0F + 0.25F * ((s.timer() % 20) / 20.0F);
            Gfx.text(g, mc.font, txt, cx, H * 0.32F, 4.0F * pulse * (secs > 3 ? 0.6F : 1), secs <= 1 ? 0xFFFF5A5A : 0xFFFFD34D);
        } else if (s.phase() == 1 && lastCount > 0) {
            lastCount = 0;
            Sfx.play("go", 1.0F, 1.0F);
        }
        if (s.phase() == 1 && s.timeMs() < 1500 && !s.finished()) {
            Gfx.text(g, mc.font, "¡YA!", cx, H * 0.32F, 4.5F, 0xFF6CFF8A);
        }
        if (s.finished() && s.phase() == 1) {
            Gfx.text(g, mc.font, "¡Llegaste " + s.place() + "º!", cx, H * 0.3F, 2.5F, s.place() == 1 ? 0xFFFFD34D : 0xFFFFFFFF);
            Gfx.text(g, mc.font, "Esperando a los demás…", cx, H * 0.3F + 26, 0.9F, 0xC0FFFFFF);
        }
        if (s.phase() == 2) {
            Gfx.begin(g, false);
            Gfx.round(cx - 120, H * 0.22F, cx + 120, H * 0.22F + 40 + board.size() * 13, 8, 0xE00B0B14);
            Gfx.end();
            Gfx.text(g, mc.font, "Resultados", cx, H * 0.22F + 8, 1.6F, 0xFFFFD34D);
            for (int i = 0; i < board.size(); i++) {
                Gfx.text(g, mc.font, board.get(i), cx, H * 0.22F + 30 + i * 13, 1.0F, i == s.me() ? 0xFFFFD34D : 0xFFFFFFFF);
            }
        }
    }
}
