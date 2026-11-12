package com.autoscoleccion.client;

import com.autoscoleccion.game.Boxes;
import com.autoscoleccion.game.ComboGame;
import com.autoscoleccion.game.RhythmGame;
import com.autoscoleccion.game.SequenceGame;
import com.autoscoleccion.game.ShiftGame;
import com.autoscoleccion.game.SkillGame;
import com.autoscoleccion.net.AutosNet;
import com.autoscoleccion.net.GameOutcomePacket;
import com.autoscoleccion.net.GameResultPacket;
import com.autoscoleccion.net.StartGamePacket;
import com.autoscoleccion.registry.ModItems;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;

/**
 * La pantalla de la máquina expendedora: fondo de carretera "synthwave" que se mueve, la prueba de habilidad que tocó (cambios,
 * ritmo, arrancón o secuencia), avisos que saltan (¡PERFECTO!, Fallo...), y al final el puntaje, las probabilidades de cada caja y una
 * ruleta que termina en la caja que salió. El puntaje de verdad lo calcula el servidor con las teclas que se le mandan.
 */
public class MinigameScreen extends Screen {
    /** Tiempo de la tarjeta "cómo se juega" + cuenta regresiva antes de empezar (ms). */
    static final int INTRO = 4200;
    private static final int[] LANE_COL = {0xFF4FA0, 0x3FE0FF, 0xFFD23F, 0x5BFF8A};

    private final StartGamePacket start;
    private final SkillGame game;
    private final long t0 = System.currentTimeMillis();
    private final List<SkillGame.Input> inputs = new ArrayList<>();
    private final Set<Integer> held = new HashSet<>();
    private boolean sent;
    private long sentAt;
    private GameOutcomePacket outcome;
    private long outcomeAt;
    private int popsSeen;
    private int lastCount = 99;
    private long shakeUntil;
    private int lastKeyShown;
    private long keyShownAt;
    private long[] rouletteAt;
    private int[] avail = {0, 1, 2};
    private int rouletteSeen;
    private int tallySeen;
    private boolean revealed;
    private final Random fx = new Random();
    private final List<float[]> sparks = new ArrayList<>();
    private long lastFrame = System.currentTimeMillis();

    public MinigameScreen(StartGamePacket start) {
        super(Component.literal("Máquina expendedora"));
        this.start = start;
        this.game = SkillGame.create(start.type(), start.seed());
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private int gameTime() {
        return (int) (System.currentTimeMillis() - t0 - INTRO);
    }

    // ================================================================================================================ teclado

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (outcome != null && revealed) {
            onClose();
            return true;
        }
        if (key == GLFW.GLFW_KEY_ESCAPE) {
            if (!sent) {
                send(Math.max(0, gameTime()));
            } else if (outcome != null) {
                onClose();
            }
            return true;
        }
        int t = gameTime();
        if (t < 0 || sent || held.contains(key)) {
            return true;
        }
        held.add(key);
        record(t, key, true);
        if (game instanceof RhythmGame) {
            for (int i = 0; i < 4; i++) {
                if (RhythmGame.LANES[i] == key) {
                    Sfx.play("note", new float[] {0.8F, 0.9F, 1.07F, 1.2F}[i], 0.5F);
                }
            }
        } else if (key != SkillGame.K_W || !(game instanceof ComboGame)) {
            Sfx.play("key", 0.9F + fx.nextFloat() * 0.25F, 0.5F);
        }
        return true;
    }

    @Override
    public boolean keyReleased(int key, int scan, int mods) {
        if (held.remove(key)) {
            int t = gameTime();
            if (t >= 0 && !sent) {
                record(t, key, false);
            }
        }
        return true;
    }

    @Override
    public boolean mouseClicked(double x, double y, int button) {
        if (outcome != null && revealed) {
            onClose();
        }
        return true;
    }

    private void record(int t, int key, boolean down) {
        if (inputs.size() >= AutosNet.MAX_INPUTS - 2) {
            return;
        }
        game.update(t);
        if (game.done()) {
            return;
        }
        game.input(t, key, down);
        inputs.add(new SkillGame.Input(t, key, down));
    }

    private void send(int t) {
        if (sent) {
            return;
        }
        sent = true;
        sentAt = System.currentTimeMillis();
        AutosNet.toServer(new GameResultPacket(start.session(), t, new ArrayList<>(inputs)));
        Sfx.play("result", 1.0F, 0.7F);
    }

    @Override
    public void onClose() {
        if (!sent) {
            send(Math.max(0, gameTime()));
        }
        super.onClose();
    }

    /** Llega el puntaje oficial del servidor. */
    public void outcome(GameOutcomePacket m) {
        outcome = m;
        outcomeAt = System.currentTimeMillis();
        // la ruleta solo pasa por las cajas que podían salir, y termina en la que salió
        float[] o = {m.o0(), m.o1(), m.o2(), m.o3()};
        int n = 0;
        int[] av = new int[4];
        int pos = 0;
        for (int b = 0; b < 4; b++) {
            if (o[b] > 0.0005F || b == m.box()) {
                if (b == m.box()) {
                    pos = n;
                }
                av[n++] = b;
            }
        }
        avail = java.util.Arrays.copyOf(av, n);
        int steps = n * 4 + pos + 1;
        rouletteAt = new long[steps];
        long acc = 2500;
        for (int i = 0; i < steps; i++) {
            acc += (long) (55 * Math.pow(1.17, i));
            rouletteAt[i] = acc;
        }
    }

    // ================================================================================================================ dibujo

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        long now = System.currentTimeMillis();
        float dt = Math.min(0.1F, (now - lastFrame) / 1000.0F);
        lastFrame = now;
        int t = gameTime();
        if (t >= 0 && !sent) {
            game.update(t);
            if (game.done()) {
                send(t);
            }
        }
        sounds(t, now);

        int W = width, H = height;
        int pw = Math.min(W - 16, 420), ph = Math.min(H - 16, 250);
        float ox = 0, oy = 0;
        if (now < shakeUntil) {
            ox = (fx.nextFloat() - 0.5F) * 6;
            oy = (fx.nextFloat() - 0.5F) * 6;
        }
        float px = (W - pw) / 2.0F + ox, py = (H - ph) / 2.0F + oy;

        Gfx.begin(g, false);
        Gfx.vgrad(0, 0, W, H, 0xF0050611, 0xF0160A2A);
        Gfx.end();
        background(g, px, py, pw, ph, now, t);

        // encabezado
        Gfx.text(g, font, SkillGame.NAME[game.type()].toUpperCase(Locale.ROOT), px + pw / 2.0F, py + 8, 1.7F, 0xFFFFE066);
        Gfx.text(g, font, "Prueba de habilidad · mejor puntaje = mejor caja", px + pw / 2.0F, py + 25, 0.75F, 0xFFB9A6FF);

        float gx = px + 14, gy = py + 40, gw = pw - 28, gh = ph - 74;
        if (t >= 0 && !(sent && outcome != null && now - outcomeAt > 300)) {
            switch (game.type()) {
                case SkillGame.SHIFT -> drawShift(g, (ShiftGame) game, gx, gy, gw, gh, now);
                case SkillGame.RHYTHM -> drawRhythm(g, (RhythmGame) game, gx, gy, gw, gh, t);
                case SkillGame.COMBO -> drawCombo(g, (ComboGame) game, gx, gy, gw, gh, now);
                default -> drawSequence(g, (SequenceGame) game, gx, gy, gw, gh, now);
            }
            drawPops(g, gx + gw / 2, gy + 6, t);
        }

        // pie: barra de puntaje en vivo
        if (!sent || outcome == null) {
            float fy = py + ph - 24;
            double sc = t >= 0 ? game.score() : 0;
            Gfx.begin(g, false);
            Gfx.round(px + 14, fy, px + pw - 120, fy + 10, 4, 0xC0101020);
            Gfx.hgrad(px + 16, fy + 2, px + 16 + (pw - 138) * (float) sc, fy + 8, 0xFF7B3FE4, 0xFF3FE0FF);
            Gfx.end();
            Gfx.textL(g, font, String.format(Locale.ROOT, "Puntaje %d%%", Math.round(sc * 100)), px + pw - 112, fy + 1, 0.85F, 0xFFFFFFFF);
            Gfx.text(g, font, "ESC: terminar ya", px + pw / 2.0F, py + ph - 11, 0.65F, 0x80FFFFFF);
        }

        if (t < 0) {
            intro(g, px, py, pw, ph, t);
        } else if (t < 700) {
            float k = t / 700.0F;
            Gfx.text(g, font, "¡YA!", px + pw / 2.0F, py + ph / 2.0F - 20 - 10 * k, 4.0F + k * 2, Gfx.argb(0x5BFF8A, 1 - k));
        }
        if (sent) {
            results(g, px, py, pw, ph, now);
        }
        updateSparks(g, dt);
    }

    private void sounds(int t, long now) {
        if (t < 0) {
            int n = (int) Math.ceil(-t / 1000.0);
            if (n <= 3 && n < lastCount) {
                lastCount = n;
                Sfx.play("count", 1.0F, 0.8F);
            }
        } else if (lastCount > 0) {
            lastCount = 0;
            Sfx.play("go", 1.0F, 0.9F);
            if (game instanceof ShiftGame || game instanceof ComboGame) {
                Sfx.play("rev", 1.0F, 0.6F);
            }
        }
        List<SkillGame.Pop> pops = game.pops();
        while (popsSeen < pops.size()) {
            SkillGame.Pop p = pops.get(popsSeen++);
            if (p.value() <= 0.0) {
                Sfx.play(game instanceof SequenceGame ? "error" : "miss", 1.0F, 0.7F);
                shakeUntil = now + 220;
            } else if (game instanceof RhythmGame) {
                if (p.value() >= 1.0) {
                    burst(width / 2.0F, height / 2.0F + 40, 0xFFE066, 6);
                }
            } else if (p.value() >= 0.92) {
                Sfx.play("perfect", 1.0F, 0.8F);
                burst(width / 2.0F, height / 2.0F, 0xFFE066, 18);
            } else {
                Sfx.play(game instanceof ComboGame ? "good" : "shift", 1.0F, 0.7F);
                burst(width / 2.0F, height / 2.0F, 0x5BFF8A, 8);
            }
        }
        if (outcome != null) {
            long e = now - outcomeAt;
            int tallyN = (int) Math.min(14, e / 85);
            while (tallySeen < tallyN && e < 1300) {
                tallySeen++;
                Sfx.play("tally", 0.8F + tallySeen * 0.05F, 0.5F);
            }
            while (rouletteSeen < rouletteAt.length && e >= rouletteAt[rouletteSeen]) {
                rouletteSeen++;
                boolean last = rouletteSeen == rouletteAt.length;
                Sfx.play(last ? "whoosh" : "key", last ? 1.0F : 1.3F, 0.6F);
            }
            if (!revealed && rouletteSeen == rouletteAt.length && e >= rouletteAt[rouletteAt.length - 1] + 250) {
                revealed = true;
                Sfx.play(outcome.box() >= 2 ? "reveal_legend" : outcome.box() == 1 ? "reveal_epic" : "reveal_rare", 1.0F, 0.9F);
                burst(width / 2.0F, height / 2.0F, Boxes.COLOR[outcome.box()], outcome.box() >= 2 ? 60 : 30);
            }
        }
    }

    private void background(GuiGraphics g, float px, float py, int pw, int ph, long now, int t) {
        g.enableScissor((int) px, (int) py, (int) px + pw, (int) py + ph);
        float horizon = py + ph * 0.46F;
        float cx = px + pw / 2.0F;
        Gfx.begin(g, false);
        Gfx.vgrad(px, py, px + pw, horizon, 0xFF0B0820, 0xFF3A1250);
        // sol con franjas
        float sr = ph * 0.3F;
        Gfx.arc(cx, horizon, 0, sr, 0, Math.PI, 0xFFFF4FA0, 0xFFFFC24A, 32);
        for (int i = 0; i < 5; i++) {
            float y = horizon - sr * (0.08F + i * 0.13F);
            Gfx.rect(cx - sr, y, cx + sr, y + 1.5F + i * 0.3F, 0xFF2A0F44);
        }
        Gfx.vgrad(px, horizon, px + pw, py + ph, 0xFF1A0830, 0xFF06030C);
        Gfx.end();
        // cuadrícula que avanza (más rápido mientras se juega)
        Gfx.begin(g, true);
        double speed = t < 0 ? 0.6 : sent ? 0.4 : 1.6;
        double scroll = (now / 1000.0 * speed) % 1.0;
        for (int i = 0; i < 12; i++) {
            double f = (i + scroll) / 12.0;
            float y = horizon + (float) (f * f) * (py + ph - horizon);
            Gfx.rect(px, y, px + pw, y + 1, Gfx.argb(0xFF3FD0, (float) (0.15 + 0.6 * f)));
        }
        for (int i = -10; i <= 10; i++) {
            float bx = cx + i * pw * 0.12F;
            Gfx.line(cx + i * 6, horizon, bx, py + ph, 1, Gfx.argb(0xFF3FD0, 0.45F));
        }
        Gfx.rect(px, horizon - 1, px + pw, horizon + 1, 0xC0FF7AE0);
        Gfx.end();
        Gfx.begin(g, false);
        Gfx.rect(px, py, px + pw, py + ph, 0x70000000);
        Gfx.end();
        g.disableScissor();
        Gfx.begin(g, true);
        int edge = Gfx.mix(0x3FE0FF, 0xFF4FA0, 0.5F + 0.5F * (float) Math.sin(now / 600.0));
        Gfx.glow(px, py, px + pw, py + ph, Gfx.argb(edge, 0.55F), 8);
        Gfx.end();
        Gfx.begin(g, false);
        Gfx.rect(px, py, px + pw, py + 1, 0xFF000000 | edge);
        Gfx.rect(px, py + ph - 1, px + pw, py + ph, 0xFF000000 | edge);
        Gfx.rect(px, py, px + 1, py + ph, 0xFF000000 | edge);
        Gfx.rect(px + pw - 1, py, px + pw, py + ph, 0xFF000000 | edge);
        Gfx.end();
    }

    private void intro(GuiGraphics g, float px, float py, int pw, int ph, int t) {
        float cw = Math.min(pw - 40, 300), ch = 110;
        float cx = px + pw / 2.0F, cy = py + ph / 2.0F;
        float appear = Gfx.back((t + INTRO) / 400.0F);
        g.pose().pushPose();
        g.pose().translate(cx, cy, 0);
        g.pose().scale(appear, appear, 1);
        Gfx.begin(g, false);
        Gfx.round(-cw / 2, -ch / 2, cw / 2, ch / 2, 8, 0xE0121226);
        Gfx.end();
        Gfx.begin(g, true);
        Gfx.glow(-cw / 2, -ch / 2, cw / 2, ch / 2, 0x603FE0FF, 6);
        Gfx.end();
        Gfx.text(g, font, "¿CÓMO SE JUEGA?", 0, -ch / 2 + 8, 1.2F, 0xFF3FE0FF);
        List<FormattedCharSequence> lines = font.split(Component.literal(SkillGame.HOWTO[game.type()]), (int) cw - 24);
        int y = (int) (-ch / 2 + 26);
        for (FormattedCharSequence l : lines) {
            g.drawString(font, l, -font.width(l) / 2, y, 0xFFEFEFFF, true);
            y += 10;
        }
        g.pose().popPose();
        int remaining = -t;
        if (remaining <= 3000) {
            int n = (int) Math.ceil(remaining / 1000.0);
            float frac = (remaining % 1000) / 1000.0F;
            float s = 3.0F + 2.5F * Gfx.back(1 - frac) - 2.0F * (1 - frac);
            Gfx.text(g, font, String.valueOf(n), cx, cy + ch / 2 + 6, Math.max(2.5F, s), Gfx.argb(0xFFE066, 0.4F + 0.6F * frac));
        }
    }

    // ---------------------------------------------------------------------------------------------------------- avisos

    private void drawPops(GuiGraphics g, float cx, float top, int t) {
        List<SkillGame.Pop> pops = game.pops();
        int shown = 0;
        for (int i = pops.size() - 1; i >= 0 && shown < 3; i--) {
            SkillGame.Pop p = pops.get(i);
            int age = t - p.t();
            if (age < 0 || age > 900) {
                continue;
            }
            shown++;
            float k = age / 900.0F;
            int col = p.value() >= 0.92 ? 0xFFE066 : p.value() >= 0.6 ? 0x5BFF8A : p.value() > 0 ? 0x9FD8FF : 0xFF4A5A;
            float sc = 1.3F + 0.8F * Gfx.back(Math.min(1, age / 160.0F)) - 0.6F * k;
            Gfx.text(g, font, p.text(), cx, top + 4 + shown * 2 - 18 * k, sc, Gfx.argb(col, 1 - k * k));
        }
    }

    // ---------------------------------------------------------------------------------------------------------- piezas

    private static float ang(double v) {
        return (float) Math.toRadians(215 - 250 * v);
    }

    /** Tacómetro: franja verde (zona de cambio), zona roja y aguja. */
    private void tach(GuiGraphics g, float cx, float cy, float R, double v, double center, double width, boolean showZone) {
        Gfx.begin(g, false);
        Gfx.circle(cx, cy, R + 8, 0xE00B0B14);
        Gfx.arc(cx, cy, R + 5, R + 8, 0, Math.PI * 2, 0xFF2A2A40, 0xFF2A2A40, 40);
        Gfx.arc(cx, cy, R - 7, R, ang(0), ang(1), 0xFF22223A, 0xFF34345A, 40);
        Gfx.arc(cx, cy, R - 9, R + 1, ang(0.9), ang(1.0), 0xFFB01830, 0xFFFF3050, 6);
        if (showZone) {
            Gfx.arc(cx, cy, R - 12, R + 3, ang(center - width / 2), ang(center + width / 2), 0xFF1BA854, 0xFF4CFF8F, 8);
        }
        for (int i = 0; i <= 10; i++) {
            float a = ang(i / 10.0);
            float c = (float) Math.cos(a), s = (float) -Math.sin(a);
            Gfx.line(cx + c * (R - 17), cy + s * (R - 17), cx + c * (R - 11), cy + s * (R - 11), i % 5 == 0 ? 2 : 1, 0xC0FFFFFF);
        }
        Gfx.end();
        Gfx.begin(g, true);
        Gfx.arc(cx, cy, R - 7, R, ang(0), ang(v), 0x10FF7A2A, 0x90FF7A2A, 30);
        if (showZone) {
            Gfx.arc(cx, cy, R - 16, R + 7, ang(center - width / 2), ang(center + width / 2), 0x204CFF8F, 0x404CFF8F, 8);
        }
        float a = ang(v);
        float c = (float) Math.cos(a), s = (float) -Math.sin(a);
        Gfx.blob(cx + c * R * 0.9F, cy + s * R * 0.9F, 9, 0xA0FF9A4A);
        Gfx.end();
        Gfx.begin(g, false);
        Gfx.line(cx - c * 8, cy - s * 8, cx + c * R * 0.93F, cy + s * R * 0.93F, 3.2F, 0xFFFF6A1A);
        Gfx.line(cx, cy, cx + c * R * 0.9F, cy + s * R * 0.9F, 1.2F, 0xFFFFE8C8);
        Gfx.circle(cx, cy, 6, 0xFF2A2A36);
        Gfx.circle(cx, cy, 3, 0xFFFF6A1A);
        Gfx.end();
        Gfx.text(g, font, String.valueOf((int) Math.round(v * 9000)), cx, cy + R * 0.35F, 1.4F, 0xFFFFFFFF);
        Gfx.text(g, font, "RPM", cx, cy + R * 0.35F + 13, 0.7F, 0xFF9A9AC0);
    }

    /** Una tecla dibujada como tecla de verdad (con canto abajo). */
    private void keycap(GuiGraphics g, float x, float y, float s, String label, int face, int border, boolean pressed, float scale) {
        float d = pressed ? 2 : 0;
        Gfx.begin(g, false);
        Gfx.round(x, y + 3, x + s, y + s + 3, 5, 0xFF07080C);
        Gfx.round(x, y + d, x + s, y + s + d, 5, 0xFF000000 | border);
        Gfx.round(x + 2, y + 2 + d, x + s - 2, y + s - 3 + d, 4, 0xFF000000 | face);
        Gfx.vgrad(x + 4, y + 3 + d, x + s - 4, y + s * 0.45F + d, 0x30FFFFFF, 0x00FFFFFF);
        Gfx.end();
        Gfx.text(g, font, label, x + s / 2, y + s / 2 - 4 * scale + d - 1, scale, 0xFFFFFFFF);
    }

    private void gearDots(GuiGraphics g, float cx, float y, int n, int cur, double[] res) {
        Gfx.begin(g, false);
        float sp = 14;
        float x0 = cx - (n - 1) * sp / 2;
        for (int i = 0; i < n; i++) {
            double r = res[i];
            int col = r < 0 ? 0xFF33334A : r >= 0.92 ? 0xFFFFD23F : r >= 0.4 ? 0xFF4CFF8F : r > 0 ? 0xFF9FD8FF : 0xFFFF3A50;
            if (i == cur && r < 0) {
                Gfx.circle(x0 + i * sp, y, 6, 0xFFFFFFFF);
            }
            Gfx.circle(x0 + i * sp, y, 4.5F, col);
        }
        Gfx.end();
    }

    // ---------------------------------------------------------------------------------------------------------- 1) cambios

    private void drawShift(GuiGraphics g, ShiftGame sg, float gx, float gy, float gw, float gh, long now) {
        float R = Math.min(gh * 0.48F, gw * 0.26F);
        float cx = gx + gw * 0.34F, cy = gy + gh * 0.6F;
        boolean wait = sg.waiting() || sg.done();
        tach(g, cx, cy, R, sg.rpm(), sg.center(), sg.width(), !wait);
        float kx = gx + gw * 0.74F, ky = gy + gh * 0.18F;
        int key = sg.shownKey();
        keyArea(g, kx, ky, key, now, wait ? "PREPÁRATE" : "ESPERA...");
        double[] res = new double[ShiftGame.GEARS];
        for (int i = 0; i < res.length; i++) {
            res[i] = sg.result(i);
        }
        Gfx.text(g, font, "MARCHA " + Math.min(ShiftGame.GEARS, sg.gear() + 1) + "/" + ShiftGame.GEARS, kx, ky + 66, 1.0F, 0xFFB9A6FF);
        gearDots(g, kx, ky + 84, ShiftGame.GEARS, sg.gear(), res);
    }

    private void keyArea(GuiGraphics g, float kx, float ky, int key, long now, String idle) {
        if (key != lastKeyShown) {
            lastKeyShown = key;
            keyShownAt = now;
        }
        float s = 50;
        if (key != 0) {
            float pop = Gfx.back(Math.min(1, (now - keyShownAt) / 180.0F));
            g.pose().pushPose();
            g.pose().translate(kx, ky + s / 2, 0);
            g.pose().scale(pop, pop, 1);
            Gfx.begin(g, true);
            Gfx.blob(0, 0, s, 0x503FE0FF);
            Gfx.end();
            keycap(g, -s / 2, -s / 2, s, SkillGame.keyName(key), 0x2A2F4A, 0x3FE0FF, held.contains(key), 2.6F);
            g.pose().popPose();
            Gfx.text(g, font, "¡PULSA EN VERDE!", kx, ky + s + 6, 0.75F, 0xFF4CFF8F);
        } else {
            keycap(g, kx - s / 2, ky, s, "?", 0x1A1C2A, 0x34345A, false, 2.6F);
            Gfx.text(g, font, idle, kx, ky + s + 6, 0.75F, 0xFF9A9AC0);
        }
    }

    // ---------------------------------------------------------------------------------------------------------- 2) ritmo

    private void drawRhythm(GuiGraphics g, RhythmGame rg, float gx, float gy, float gw, float gh, int t) {
        float lw = Math.min(46, gw / 7);
        float lx0 = gx + gw / 2 - lw * 2;
        float top = gy, hitY = gy + gh - 20;
        Gfx.begin(g, false);
        for (int i = 0; i < 4; i++) {
            float x = lx0 + i * lw;
            Gfx.vgrad(x + 1, top, x + lw - 1, hitY + 14, 0x30101020, 0xA0101020);
        }
        Gfx.end();
        Gfx.begin(g, true);
        for (int i = 0; i < 4; i++) {
            float x = lx0 + i * lw;
            if (held.contains(RhythmGame.LANES[i])) {
                Gfx.vgrad(x + 1, top, x + lw - 1, hitY, Gfx.argb(LANE_COL[i], 0), Gfx.argb(LANE_COL[i], 0.5F));
            }
        }
        Gfx.rect(lx0, hitY - 1, lx0 + 4 * lw, hitY + 1, 0xFFFFFFFF);
        Gfx.glow(lx0, hitY - 1, lx0 + 4 * lw, hitY + 1, 0x60FFFFFF, 5);
        Gfx.end();
        Gfx.begin(g, false);
        for (RhythmGame.Note n : rg.notes()) {
            int dt = n.t - t;
            int fall = rg.fallTime(n.t);
            if (dt > fall) {
                continue;
            }
            float x = lx0 + n.lane * lw;
            if (n.result > 0) {
                int since = -dt;
                if (since >= 0 && since < 260) {
                    float k = since / 260.0F;
                    Gfx.arc(x + lw / 2, hitY, 6 + 16 * k, 8 + 18 * k, 0, Math.PI * 2, Gfx.argb(LANE_COL[n.lane], 1 - k), Gfx.argb(0xFFFFFF, 0), 20);
                }
                continue;
            }
            float y = hitY - dt / (float) fall * (hitY - top);
            if (y > gy + gh + 6) {
                continue;
            }
            int col = n.result == 0 ? 0xFF5A2030 : 0xFF000000 | LANE_COL[n.lane];
            Gfx.round(x + 4, y - 5, x + lw - 4, y + 5, 4, col);
            Gfx.rect(x + 7, y - 4, x + lw - 7, y - 2, 0x60FFFFFF);
        }
        Gfx.end();
        for (int i = 0; i < 4; i++) {
            float x = lx0 + i * lw;
            keycap(g, x + lw / 2 - 9, hitY + 4, 18, SkillGame.keyName(RhythmGame.LANES[i]), held.contains(RhythmGame.LANES[i]) ? 0x3A3F66 : 0x22263A,
                    LANE_COL[i], held.contains(RhythmGame.LANES[i]), 1.0F);
        }
        if (rg.combo() >= 3) {
            float cx = gx + gw * 0.86F;
            Gfx.text(g, font, "COMBO", cx, gy + gh * 0.3F, 0.9F, 0xFFB9A6FF);
            Gfx.text(g, font, "x" + rg.combo(), cx, gy + gh * 0.3F + 12, 2.2F, 0xFFFFE066);
        }
    }

    // ---------------------------------------------------------------------------------------------------------- 3) arrancón

    private void drawCombo(GuiGraphics g, ComboGame cg, float gx, float gy, float gw, float gh, long now) {
        // medidor vertical grande a la izquierda y tecla W a la derecha
        float mx0 = gx + gw * 0.3F, mx1 = mx0 + 46, my0 = gy + 8, my1 = gy + gh - 6;
        double v = cg.launchRpm();
        double lo = Math.max(0, cg.bandLo()), hi = Math.min(1, cg.bandHi());
        boolean in = v >= lo && v <= hi;
        float yLo = my1 - (my1 - my0) * (float) lo, yHi = my1 - (my1 - my0) * (float) hi;
        float ny = my1 - (my1 - my0) * (float) Math.min(1, v);
        Gfx.begin(g, false);
        Gfx.round(mx0 - 4, my0 - 4, mx1 + 4, my1 + 4, 6, 0xFF0B0B14);
        Gfx.vgrad(mx0, my0, mx1, my1, 0xFF3A1830, 0xFF181830);
        for (int i = 1; i < 10; i++) {
            float y = my1 - (my1 - my0) * i / 10.0F;
            Gfx.rect(mx0, y, mx0 + (i % 5 == 0 ? 14 : 8), y + 1, 0x60FFFFFF);
        }
        Gfx.vgrad(mx0 + 2, yHi, mx1 - 2, yLo, 0xFF4CFF8F, 0xFF1BA854);
        Gfx.rect(mx0 - 8, ny - 2, mx1 + 8, ny + 2, 0xFFFFE8C8);
        Gfx.end();
        Gfx.begin(g, true);
        Gfx.glow(mx0 + 2, yHi, mx1 - 2, yLo, in ? 0x804CFF8F : 0x304CFF8F, in ? 8 : 4);
        Gfx.blob(mx0 + 23, ny, 22, in ? 0xA04CFF8F : 0xA0FF7A2A);
        Gfx.end();
        Gfx.text(g, font, String.valueOf((int) Math.round(v * 9000)), mx0 + 23, my1 + 8, 0.9F, 0xFFFFFFFF);
        // tecla W y textos
        float kx = gx + gw * 0.7F;
        boolean down = cg.holding();
        keycap(g, kx - 26, gy + gh * 0.18F, 52, "W", down ? 0x3A5F46 : 0x2A2F4A, down ? 0x4CFF8F : 0x3FE0FF, down, 2.6F);
        Gfx.text(g, font, down ? "ACELERANDO" : "SUELTA = BAJA", kx, gy + gh * 0.18F + 62, 0.85F, down ? 0xFF4CFF8F : 0xFFFF9A4A);
        Gfx.text(g, font, String.format(Locale.ROOT, "En verde: %d%%", Math.round(cg.launchSoFar() * 100)), kx, gy + gh * 0.18F + 76, 0.85F,
                0xFFFFFFFF);
        Gfx.begin(g, false);
        float px0 = kx - 50, px1 = kx + 50, py = gy + gh * 0.18F + 92;
        Gfx.round(px0, py, px1, py + 5, 2, 0xFF22223A);
        Gfx.round(px0, py, px0 + (px1 - px0) * (float) cg.launchProgress(), py + 5, 2, 0xFFFF7AE0);
        Gfx.end();
    }

    // ---------------------------------------------------------------------------------------------------------- 4) secuencia

    private void drawSequence(GuiGraphics g, SequenceGame sq, float gx, float gy, float gw, float gh, long now) {
        int[] seq = sq.sequence();
        int n = seq.length;
        float s = Math.min(34, (gw - 20) / n - 5), gap = 5;
        float x0 = gx + gw / 2 - (n * (s + gap) - gap) / 2;
        float y = gy + gh * 0.36F;
        if (sq.between()) {
            Gfx.text(g, font, sq.done() ? "¡TERMINADO!" : "RONDA " + (sq.round() + 1) + "/" + SequenceGame.ROUNDS, gx + gw / 2, y, 2.0F, 0xFFFFE066);
        } else {
            boolean running = sq.running();
            for (int i = 0; i < n; i++) {
                boolean done = i < sq.pos();
                boolean cur = i == sq.pos() && running;
                int face = sq.failed() && i == sq.pos() ? 0x7A1A2A : done ? 0x1E7A44 : cur ? 0x3A3F66 : 0x22263A;
                float bob = cur ? (float) Math.sin(now / 90.0) * 1.5F : 0;
                keycap(g, x0 + i * (s + gap), y + bob, s, SkillGame.keyName(seq[i]), face, cur ? 0x3FE0FF : 0x34345A, done, s / 18.0F);
            }
            if (!running) {
                Gfx.text(g, font, "¡LÉELA!", gx + gw / 2, y - 18, 1.2F, 0xFF3FE0FF);
            }
            float tl = (float) sq.timeLeft();
            Gfx.begin(g, false);
            Gfx.round(gx + gw * 0.15F, y + s + 16, gx + gw * 0.85F, y + s + 24, 3, 0xFF22223A);
            Gfx.round(gx + gw * 0.15F, y + s + 16, gx + gw * (0.15F + 0.7F * tl), y + s + 24, 3, Gfx.mix(0xFF3050, 0x4CFF8F, tl) | 0xFF000000);
            Gfx.end();
        }
        double[] res = new double[SequenceGame.ROUNDS];
        for (int i = 0; i < res.length; i++) {
            res[i] = sq.result(i);
        }
        gearDots(g, gx + gw / 2, gy + gh - 4, SequenceGame.ROUNDS, sq.round(), res);
    }

    // ---------------------------------------------------------------------------------------------------------- resultado

    private static String grade(double s) {
        if (s >= 0.9) {
            return "¡LEYENDA DEL VOLANTE!";
        }
        if (s >= 0.75) {
            return "¡PILOTO PRO!";
        }
        if (s >= 0.55) {
            return "¡Muy bien!";
        }
        if (s >= 0.35) {
            return "Nada mal";
        }
        return "Sigue practicando";
    }

    private void results(GuiGraphics g, float px, float py, int pw, int ph, long now) {
        Gfx.begin(g, false);
        float wait = Math.min(1, (now - sentAt) / 250.0F);
        Gfx.rect(px + 1, py + 1, px + pw - 1, py + ph - 1, Gfx.argb(0x06050E, 0.86F * wait));
        Gfx.end();
        float cx = px + pw / 2.0F;
        if (outcome == null) {
            Gfx.begin(g, true);
            for (int i = 0; i < 8; i++) {
                double a = now / 120.0 + i * Math.PI / 4;
                Gfx.blob(cx + (float) Math.cos(a) * 14, py + ph / 2.0F + (float) Math.sin(a) * 14, 4, Gfx.argb(0x3FE0FF, i / 8.0F));
            }
            Gfx.end();
            Gfx.text(g, font, "Calculando puntaje...", cx, py + ph / 2.0F + 26, 1.0F, 0xFFB9A6FF);
            return;
        }
        long e = now - outcomeAt;
        float sc = outcome.score() * Gfx.ease(e / 1200.0F);
        Gfx.text(g, font, "PUNTAJE", cx, py + 12, 0.9F, 0xFFB9A6FF);
        Gfx.text(g, font, Math.round(sc * 100) + "%", cx, py + 24, 3.4F, 0xFFFFFFFF);
        if (e > 1200) {
            float k = Gfx.back(Math.min(1, (e - 1200) / 300.0F));
            Gfx.text(g, font, grade(outcome.score()), cx, py + 58, 1.3F * k, 0xFFFFE066);
        }
        // las cuatro cajas con su probabilidad y la ruleta
        float cw = Math.min(96, (pw - 46) / 4.0F), chh = 112;
        float y0 = py + 78;
        float[] odds = {outcome.o0(), outcome.o1(), outcome.o2(), outcome.o3()};
        int hi = -1;
        if (e >= 2500) {
            hi = avail[rouletteSeen > 0 ? (rouletteSeen - 1) % avail.length : 0];
        }
        boolean done = revealed;
        for (int b = 0; b < 4; b++) {
            float x = cx - 2.0F * cw - 15 + b * (cw + 10);
            float appear = Gfx.back(Math.min(1, Math.max(0, (e - 300 - b * 120) / 350.0F)));
            if (appear <= 0) {
                continue;
            }
            boolean on = hi == b;
            boolean win = done && b == outcome.box();
            float scale = appear * (win ? 1.12F + 0.03F * (float) Math.sin(now / 150.0) : on ? 1.05F : done ? 0.92F : 1.0F);
            g.pose().pushPose();
            g.pose().translate(x + cw / 2, y0 + chh / 2, 0);
            g.pose().scale(scale, scale, 1);
            float lx = -cw / 2, ly = -chh / 2;
            int col = Boxes.COLOR[b];
            if (win) {
                Gfx.begin(g, true);
                for (int r = 0; r < 12; r++) {
                    double a = now / 900.0 + r * Math.PI / 6;
                    float c0 = (float) Math.cos(a), s0 = (float) Math.sin(a), c1 = (float) Math.cos(a + 0.18), s1 = (float) Math.sin(a + 0.18);
                    Gfx.quad(0, 0, Gfx.argb(col, 0.5F), c0 * 140, s0 * 140, Gfx.argb(col, 0), c1 * 140, s1 * 140, Gfx.argb(col, 0), 0, 0,
                            Gfx.argb(col, 0.5F));
                }
                Gfx.end();
            }
            Gfx.begin(g, false);
            Gfx.round(lx, ly, lx + cw, ly + chh, 6, done && !win ? 0xC0121220 : 0xF01A1830);
            Gfx.vgrad(lx + 2, ly + 2, lx + cw - 2, ly + 40, Gfx.argb(col, 0.45F), Gfx.argb(col, 0.0F));
            Gfx.end();
            Gfx.begin(g, true);
            if (on || win) {
                Gfx.glow(lx, ly, lx + cw, ly + chh, Gfx.argb(col, win ? 0.9F : 0.7F), win ? 10 : 6);
            }
            Gfx.end();
            // el ícono 3D de la caja
            g.pose().pushPose();
            g.pose().translate(0, ly + 30, 100);
            g.pose().scale(2.2F, 2.2F, 2.2F);
            g.renderItem(new ItemStack(ModItems.BOXES.get(b).get()), -8, -8);
            g.pose().popPose();
            Gfx.text(g, font, Boxes.NAME[b], 0, ly + 56, 0.75F, 0xFF000000 | col);
            float fill = Gfx.ease((e - 1200 - b * 150) / 900.0F);
            Gfx.begin(g, false);
            Gfx.round(lx + 10, ly + 72, lx + cw - 10, ly + 80, 3, 0xFF22223A);
            Gfx.round(lx + 10, ly + 72, lx + 10 + (cw - 20) * Math.max(0.02F, odds[b] * fill), ly + 80, 3, 0xFF000000 | col);
            Gfx.end();
            Gfx.text(g, font, String.format(Locale.ROOT, "%.0f%%", odds[b] * 100 * fill), 0, ly + 86, 1.2F, 0xFFFFFFFF);
            g.pose().popPose();
        }
        if (done) {
            int b = outcome.box();
            float k = Gfx.back(Math.min(1, (e - rouletteAt[rouletteAt.length - 1] - 250) / 350.0F));
            Gfx.text(g, font, "¡Caja " + Boxes.NAME[b] + "!", cx, py + ph - 38, 1.6F * k, 0xFF000000 | Boxes.COLOR[b]);
            Gfx.text(g, font, "Salió por la bandeja de la máquina · clic o tecla para cerrar", cx, py + ph - 16, 0.7F, 0xC0FFFFFF);
        }
    }

    // ---------------------------------------------------------------------------------------------------------- chispas

    private void burst(float x, float y, int rgb, int n) {
        for (int i = 0; i < n; i++) {
            double a = fx.nextDouble() * Math.PI * 2;
            float sp = 60 + fx.nextFloat() * 160;
            sparks.add(new float[] {x, y, (float) Math.cos(a) * sp, (float) Math.sin(a) * sp - 60, 1.0F, rgb, 1.5F + fx.nextFloat() * 2});
        }
    }

    private void updateSparks(GuiGraphics g, float dt) {
        if (sparks.isEmpty()) {
            return;
        }
        Gfx.begin(g, true);
        for (int i = sparks.size() - 1; i >= 0; i--) {
            float[] s = sparks.get(i);
            s[0] += s[2] * dt;
            s[1] += s[3] * dt;
            s[3] += 220 * dt;
            s[4] -= dt * 1.1F;
            if (s[4] <= 0) {
                sparks.remove(i);
                continue;
            }
            Gfx.blob(s[0], s[1], s[6] * 2.2F, Gfx.argb((int) s[5], s[4]));
        }
        Gfx.end();
    }
}
