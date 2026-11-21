package com.mundosdeaxelis.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mundosdeaxelis.crate.CrateKind;
import com.mundosdeaxelis.forja.ForjaGame;
import com.mundosdeaxelis.network.ForjaResultPacket;
import com.mundosdeaxelis.network.ModNetwork;
import com.mundosdeaxelis.registry.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * La forja del Gran Yunque (etapa 270). Primero eliges qué llave forjar (hacen falta 4 fragmentos de la misma caja) y luego juegas
 * las tres fases de {@link ForjaGame}: fundir (mantén pulsado para avivar el fuego y deja la aguja en la zona dorada), martillar
 * (golpea cuando el indicador pasa por el punto dorado) y templar (mete la llave al agua cuando brille en rojo cereza). Clic o
 * Espacio. Al terminar avisa al servidor, que es quien gasta los fragmentos y da la llave. Salir a mitad quiebra la llave.
 */
public class ForjaScreen extends Screen {
    private static final int W = 360, H = 236;
    private static final int GOLD = 0xFFE8B84A, EMBER = 0xFFFF7A2A, TEXT = 0xFFF2E6D0, DIM = 0xFF8C7C6C;

    private final BlockPos pos;
    private final int[] counts;
    private final long seed;
    private ForjaGame game;
    private int tier;
    private boolean sent = false, doneHandled = false;
    private long lastNanos = 0L;
    private double time = 0.0, doneT = 0.0, shake = 0.0, bellowsT = 0.0;
    private ForjaGame.Phase lastPhase = null;
    private boolean wasInBand = false;
    private final List<double[]> sparks = new ArrayList<>();
    private final Random fx = new Random();

    public ForjaScreen(BlockPos pos, int[] counts, long seed) {
        super(Component.translatable("block.mundos_de_axelis.gran_yunque"));
        this.pos = pos;
        this.counts = counts;
        this.seed = seed;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private int left() {
        return (width - W) / 2;
    }

    private int top() {
        return (height - H) / 2;
    }

    private static void sound(SoundEvent ev, float pitch, float vol) {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(ev, pitch, vol));
    }

    // ------------------------------------------------------------------ entrada

    private void start(int t) {
        tier = t;
        game = new ForjaGame(t, seed ^ (t * 0x9E3779B97F4A7C15L));
        sound(SoundEvents.FIRECHARGE_USE, 0.7F, 0.6F);
    }

    private void press() {
        if (game != null && game.phase != ForjaGame.Phase.DONE) {
            game.press();
        } else if (game != null && doneT > 0.6) {
            onClose();
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (game == null) {
            int l = left(), t = top();
            for (int i = 1; i <= 8; i++) {
                int[] c = cell(i, l, t);
                if (mx >= c[0] && mx < c[0] + 80 && my >= c[1] && my < c[1] + 54 && counts[i] >= 4) {
                    start(i);
                    return true;
                }
            }
            return super.mouseClicked(mx, my, button);
        }
        if (button == 0 || button == 1) {
            press();
            return true;
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        if (game != null) {
            game.release();
        }
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (game != null && (key == GLFW.GLFW_KEY_SPACE || key == GLFW.GLFW_KEY_ENTER)) {
            press();
            return true;
        }
        return super.keyPressed(key, scan, mods);
    }

    @Override
    public boolean keyReleased(int key, int scan, int mods) {
        if (game != null && key == GLFW.GLFW_KEY_SPACE) {
            game.release();
        }
        return super.keyReleased(key, scan, mods);
    }

    /** Al salir (de cualquier forma): si la forja ya había empezado y no terminó, la llave se quiebra. */
    @Override
    public void removed() {
        send();
        super.removed();
    }

    private void send() {
        if (!sent) {
            sent = true;
            int code = game == null ? 0 : game.phase == ForjaGame.Phase.DONE ? game.code() : 3;
            ModNetwork.CHANNEL.sendToServer(new ForjaResultPacket(pos, Math.max(1, tier), code));
        }
    }

    // ------------------------------------------------------------------ lógica por cuadro

    private void step() {
        long now = System.nanoTime();
        double dt = lastNanos == 0 ? 0.0 : Math.min(0.1, (now - lastNanos) / 1.0e9);
        lastNanos = now;
        time += dt;
        shake = Math.max(0.0, shake - dt * 3.0);
        for (int i = sparks.size() - 1; i >= 0; i--) {
            double[] s = sparks.get(i);
            s[0] += s[2] * dt;
            s[1] += s[3] * dt;
            s[3] += 260 * dt;
            s[4] -= dt;
            if (s[4] <= 0) {
                sparks.remove(i);
            }
        }
        if (game == null) {
            return;
        }
        ForjaGame.Phase before = game.phase;
        int hitsBefore = game.struck;
        int temperBefore = game.temperHit;
        game.tick(dt);
        if (game.phase == ForjaGame.Phase.MELT && game.holding) {
            bellowsT -= dt;
            if (bellowsT <= 0) {
                bellowsT = 0.35;
                sound(SoundEvents.BLAZE_SHOOT, 0.5F, 0.12F);
            }
        }
        if (game.phase == ForjaGame.Phase.MELT) {
            boolean in = game.inBand();
            if (in && !wasInBand) {
                sound(SoundEvents.AMETHYST_BLOCK_CHIME, 1.4F, 0.4F);
            }
            wasInBand = in;
            if (fx.nextInt(4) == 0) {
                spark(left() + 92, top() + 132, 0.4);
            }
        }
        if (game.struck != hitsBefore || (before == ForjaGame.Phase.HAMMER && game.phase == ForjaGame.Phase.DONE && game.lastHitT == 0)) {
            int h = game.lastHit;
            sound(h == 2 ? SoundEvents.ANVIL_LAND : h == 1 ? SoundEvents.ANVIL_USE : SoundEvents.ITEM_BREAK, h == 2 ? 1.25F : 1.0F, 0.7F);
            shake = h == 0 ? 1.0 : 0.6;
            for (int i = 0; i < (h == 2 ? 26 : h == 1 ? 14 : 6); i++) {
                spark(left() + W / 2.0, top() + 128, 1.0);
            }
        }
        if (game.temperHit != temperBefore && game.temperHit >= 0) {
            sound(SoundEvents.FIRE_EXTINGUISH, 1.0F, 0.8F);
            sound(SoundEvents.BUCKET_EMPTY, 1.0F, 0.6F);
        }
        if (game.phase == ForjaGame.Phase.DONE && !doneHandled) {
            doneHandled = true;
            doneT = 0.0;
            if (game.result == ForjaGame.Result.FAIL) {
                sound(SoundEvents.GLASS_BREAK, 0.8F, 0.8F);
            } else {
                sound(SoundEvents.PLAYER_LEVELUP, game.result == ForjaGame.Result.PERFECT ? 1.3F : 1.0F, 0.7F);
            }
            send();
        }
        if (game.phase == ForjaGame.Phase.DONE) {
            doneT += dt;
        }
        if (game.phase != lastPhase && game.phase == ForjaGame.Phase.HAMMER) {
            sound(SoundEvents.LAVA_POP, 1.0F, 0.8F);
        }
        lastPhase = game.phase;
    }

    private void spark(double x, double y, double power) {
        sparks.add(new double[]{x, y, (fx.nextDouble() - 0.5) * 220 * power, -60 - fx.nextDouble() * 160 * power, 0.4 + fx.nextDouble() * 0.5});
    }

    // ------------------------------------------------------------------ dibujo

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        step();
        renderBackground(g);
        int l = left(), t = top();
        int sx = (int) (Math.sin(time * 70) * 3 * shake), sy = (int) (Math.cos(time * 55) * 2 * shake);
        g.pose().pushPose();
        g.pose().translate(sx, sy, 0);
        // marco: hierro ennegrecido con borde dorado y brasas abajo
        g.fill(l - 2, t - 2, l + W + 2, t + H + 2, 0xFF3A2A18);
        g.fill(l - 1, t - 1, l + W + 1, t + H + 1, GOLD);
        g.fillGradient(l, t, l + W, t + H, 0xFF1A1412, 0xFF3A1A10);
        g.fillGradient(l, t + H - 40, l + W, t + H, 0x00000000, 0x60FF5A10);
        g.drawCenteredString(font, Component.translatable("block.mundos_de_axelis.gran_yunque"), l + W / 2, t + 7, GOLD);
        if (game == null) {
            renderSelect(g, mx, my, l, t);
        } else {
            switch (game.phase) {
                case MELT -> renderMelt(g, l, t);
                case HAMMER -> renderHammer(g, l, t);
                case TEMPER -> renderTemper(g, l, t);
                case DONE -> renderDone(g, l, t);
            }
        }
        for (double[] s : sparks) {
            int a = (int) (Math.min(1.0, s[4] * 2.5) * 255);
            g.fill((int) s[0], (int) s[1], (int) s[0] + 2, (int) s[1] + 1, (a << 24) | (s[4] > 0.4 ? 0xFFE08A : 0xFF7A2A));
        }
        g.pose().popPose();
        super.render(g, mx, my, pt);
    }

    private int[] cell(int i, int l, int t) {
        int c = (i - 1) % 4, r = (i - 1) / 4;
        return new int[]{l + 12 + c * 86, t + 40 + r * 62};
    }

    private void renderSelect(GuiGraphics g, int mx, int my, int l, int t) {
        g.drawCenteredString(font, Component.translatable("gui.mundos_de_axelis.forja.elige"), l + W / 2, t + 22, TEXT);
        boolean any = false;
        for (int i = 1; i <= 8; i++) {
            CrateKind k = CrateKind.byIndex(i);
            int[] c = cell(i, l, t);
            boolean ok = counts[i] >= 4;
            any |= ok;
            boolean over = ok && mx >= c[0] && mx < c[0] + 80 && my >= c[1] && my < c[1] + 54;
            int edge = ok ? (over ? 0xFFFFFFFF : 0xFF000000 | k.color) : 0xFF3A3230;
            g.fill(c[0], c[1], c[0] + 80, c[1] + 54, edge);
            g.fillGradient(c[0] + 1, c[1] + 1, c[0] + 79, c[1] + 53, ok ? 0xFF2A2018 : 0xFF181412, ok ? 0xFF1A120E : 0xFF100C0A);
            ItemStack key = new ItemStack(ModItems.KEYS.get(k).get());
            g.pose().pushPose();
            g.pose().translate(c[0] + 4, c[1] + 4, 0);
            g.pose().scale(1.5F, 1.5F, 1.0F);
            if (!ok) {
                RenderSystem.setShaderColor(0.35F, 0.32F, 0.3F, 1.0F);
            }
            g.renderItem(key, 0, 0);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            g.pose().popPose();
            g.renderItem(new ItemStack(com.mundosdeaxelis.forja.Forja.fragment(i)), c[0] + 60, c[1] + 6);
            Component name = key.getHoverName();
            float sc = Math.min(1.0F, 72.0F / Math.max(1, font.width(name)));
            g.pose().pushPose();
            g.pose().translate(c[0] + 4, c[1] + 31, 0);
            g.pose().scale(sc, sc, 1.0F);
            g.drawString(font, name, 0, 0, ok ? 0xFF000000 | k.color : DIM, false);
            g.pose().popPose();
            g.drawString(font, counts[i] + "/4", c[0] + 4, c[1] + 42, ok ? 0xFF8CFFA8 : 0xFFFF8C8C, false);
            String cost = "◆" + com.mundosdeaxelis.forja.Forja.cost(i);
            g.drawString(font, cost, c[0] + 76 - font.width(cost), c[1] + 42, 0xFF7FE8FF, false);
        }
        Component hint = any ? Component.translatable("gui.mundos_de_axelis.forja.hint") : Component.translatable("gui.mundos_de_axelis.forja.ninguna");
        g.drawCenteredString(font, hint, l + W / 2, t + H - 26, any ? DIM : 0xFFFF9C7C);
        g.drawCenteredString(font, Component.translatable("gui.mundos_de_axelis.forja.riesgo"), l + W / 2, t + H - 14, 0xFF9C6C5C);
    }

    private void header(GuiGraphics g, int l, int t, String phaseKey, int step) {
        Component title = Component.translatable("gui.mundos_de_axelis.forja." + phaseKey);
        g.drawCenteredString(font, Component.literal(step + "/3 · ").append(title), l + W / 2, t + 22, EMBER);
        ItemStack key = new ItemStack(ModItems.KEYS.get(CrateKind.byIndex(tier)).get());
        g.drawString(font, key.getHoverName(), l + 10, t + 22, 0xFF000000 | CrateKind.byIndex(tier).color, false);
    }

    /** Color del metal según el calor (0 frío oscuro … 1 blanco). */
    private static int heatColor(double h) {
        h = Mth.clamp(h, 0.0, 1.0);
        int[][] stops = {{40, 24, 20}, {130, 20, 20}, {210, 40, 30}, {255, 120, 30}, {255, 200, 70}, {255, 250, 220}};
        double f = h * (stops.length - 1);
        int i = Math.min(stops.length - 2, (int) f);
        double u = f - i;
        int r = (int) (stops[i][0] + (stops[i + 1][0] - stops[i][0]) * u);
        int gg = (int) (stops[i][1] + (stops[i + 1][1] - stops[i][1]) * u);
        int b = (int) (stops[i][2] + (stops[i + 1][2] - stops[i][2]) * u);
        return 0xFF000000 | (r << 16) | (gg << 8) | b;
    }

    private void renderMelt(GuiGraphics g, int l, int t) {
        header(g, l, t, "fundir", 1);
        // crisol
        int cx = l + 40, cy = t + 70;
        g.fill(cx - 4, cy - 4, cx + 108, cy + 92, 0xFF2A2A2E);
        g.fill(cx, cy, cx + 104, cy + 88, 0xFF141416);
        int lava = heatColor(game.temp * 0.95);
        double lvl = 0.35 + 0.45 * game.melt;
        int top = cy + 88 - (int) (88 * lvl);
        g.fillGradient(cx, top, cx + 104, cy + 88, lava, heatColor(game.temp * 0.6));
        for (int i = 0; i < 6; i++) {
            int bx = cx + 8 + (int) ((Math.sin(time * 1.7 + i * 1.3) * 0.5 + 0.5) * 88);
            int by = top + 2 + (int) ((time * 18 + i * 9) % Math.max(4, (cy + 88 - top - 4)));
            g.fill(bx, by, bx + 3, by + 2, 0xA0FFF0B0);
        }
        // los fragmentos se hunden en el metal
        ItemStack frag = new ItemStack(com.mundosdeaxelis.forja.Forja.fragment(tier));
        for (int i = 0; i < 4; i++) {
            float sink = (float) Mth.clamp(game.melt * 1.3 - i * 0.12, 0.0, 1.0);
            if (sink >= 1.0F) {
                continue;
            }
            int fy = top - 14 + (int) (sink * 22);
            RenderSystem.setShaderColor(1.0F, 1.0F - 0.5F * sink, 1.0F - 0.8F * sink, 1.0F);
            g.renderItem(frag, cx + 10 + i * 22, fy);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        }
        // termómetro con la zona de fusión
        int tx = l + 190, ty = t + 48, th = 140;
        g.fill(tx - 2, ty - 2, tx + 22, ty + th + 2, 0xFF3A3230);
        for (int y = 0; y < th; y++) {
            double v = 1.0 - (double) y / th;
            g.fill(tx, ty + y, tx + 20, ty + y + 1, heatColor(v) & 0x90FFFFFF | 0x90000000);
        }
        int b0 = ty + (int) ((1.0 - game.bandHi) * th), b1 = ty + (int) ((1.0 - game.bandLo) * th);
        int pulse = (int) (0x60 + 0x60 * Math.sin(time * 6));
        g.fill(tx - 4, b0, tx + 24, b1, (pulse << 24) | 0xE8B84A);
        g.fill(tx - 4, b0, tx + 24, b0 + 1, GOLD);
        g.fill(tx - 4, b1 - 1, tx + 24, b1, GOLD);
        int ny = ty + (int) ((1.0 - game.temp) * th);
        g.fill(tx - 8, ny - 1, tx + 28, ny + 1, game.inBand() ? 0xFF8CFFA8 : game.temp > 0.96 ? 0xFFFF4040 : 0xFFFFFFFF);
        g.drawString(font, "▶", tx - 14, ny - 4, 0xFFFFFFFF, false);
        // barra de fusión y tiempo
        int px = l + 232, pw = 112;
        g.drawString(font, Component.translatable("gui.mundos_de_axelis.forja.fusion"), px, t + 60, TEXT, false);
        g.fill(px, t + 72, px + pw, t + 82, 0xFF2A2018);
        g.fill(px + 1, t + 73, px + 1 + (int) ((pw - 2) * game.melt), t + 81, game.inBand() ? 0xFFFFB347 : 0xFFB06A30);
        double left = Math.max(0.0, ForjaGame.MELT_LIMIT - game.meltTime);
        g.drawString(font, Component.translatable("gui.mundos_de_axelis.forja.tiempo", String.format(java.util.Locale.ROOT, "%.1f", left)), px, t + 92,
                left < 4 ? 0xFFFF8C8C : DIM, false);
        if (game.temp > 0.96) {
            g.drawString(font, Component.translatable("gui.mundos_de_axelis.forja.quema"), px, t + 108, 0xFFFF5050, false);
        }
        drawWrapped(g, Component.translatable(game.holding ? "gui.mundos_de_axelis.forja.fundir_soplando" : "gui.mundos_de_axelis.forja.fundir_ayuda"),
                px, t + 126, pw, TEXT);
    }

    private void renderHammer(GuiGraphics g, int l, int t) {
        header(g, l, t, "martillar", 2);
        // yunque
        int ax = l + W / 2, ay = t + 140;
        g.fill(ax - 70, ay, ax + 70, ay + 16, 0xFF4A4C52);
        g.fill(ax - 82, ay - 2, ax - 70, ay + 6, 0xFF4A4C52);
        g.fill(ax - 30, ay + 16, ax + 30, ay + 40, 0xFF3A3C42);
        g.fill(ax - 50, ay + 40, ax + 50, ay + 50, 0xFF4A4C52);
        g.fill(ax - 70, ay, ax + 70, ay + 2, 0xFF8A8C94);
        // la llave al rojo sobre el yunque
        renderKey(g, ax - 24, ay - 46, 3.0F, heatColor(0.78 + 0.05 * Math.sin(time * 5)));
        // barra del martillo
        int bx = l + 40, bw = W - 80, by = t + 50;
        g.fill(bx - 1, by - 1, bx + bw + 1, by + 13, 0xFF000000);
        g.fill(bx, by, bx + bw, by + 12, 0xFF2A2018);
        int gz0 = bx + (int) ((game.sweet - game.goodHalf) * bw), gz1 = bx + (int) ((game.sweet + game.goodHalf) * bw);
        int pz0 = bx + (int) ((game.sweet - game.perfectHalf) * bw), pz1 = bx + (int) ((game.sweet + game.perfectHalf) * bw);
        g.fill(Math.max(bx, gz0), by, Math.min(bx + bw, gz1), by + 12, 0xFF8A6A40);
        g.fill(Math.max(bx, pz0), by, Math.min(bx + bw, pz1), by + 12, GOLD);
        int hx = bx + (int) (game.hammerPos * bw);
        g.fill(hx - 1, by - 5, hx + 2, by + 17, 0xFFFFFFFF);
        g.renderItem(new ItemStack(net.minecraft.world.item.Items.IRON_AXE), hx - 8, by - 22);
        // golpes
        int sx0 = l + W / 2 - game.strikes * 7;
        for (int i = 0; i < game.strikes; i++) {
            int col = i < game.struck ? (i == game.struck - 1 && game.lastHit == 0 ? 0xFFFF5050 : 0xFFE8B84A) : 0xFF3A3230;
            g.fill(sx0 + i * 14, t + 70, sx0 + i * 14 + 10, t + 78, col);
        }
        feedback(g, l, t);
        g.drawCenteredString(font, Component.translatable("gui.mundos_de_axelis.forja.martillar_ayuda"), l + W / 2, t + H - 16, DIM);
    }

    private void renderTemper(GuiGraphics g, int l, int t) {
        header(g, l, t, "templar", 3);
        // la llave se enfría: su color baja del blanco al oscuro
        double heat = 1.0 - game.cool;
        renderKey(g, l + 60, t + 90, 4.0F, heatColor(heat));
        g.renderItem(new ItemStack(net.minecraft.world.item.Items.WATER_BUCKET), l + W - 70, t + 120);
        // barra de colores con la ventana del rojo cereza
        int bx = l + 40, bw = W - 80, by = t + 52;
        g.fill(bx - 1, by - 1, bx + bw + 1, by + 13, 0xFF000000);
        for (int x = 0; x < bw; x++) {
            g.fill(bx + x, by, bx + x + 1, by + 12, heatColor(1.0 - (double) x / bw));
        }
        int w0 = bx + (int) ((game.target - game.targetHalf) * bw), w1 = bx + (int) ((game.target + game.targetHalf) * bw);
        g.fill(w0, by - 4, w1, by - 2, GOLD);
        g.fill(w0, by + 14, w1, by + 16, GOLD);
        g.fill(w0, by - 4, w0 + 1, by + 16, GOLD);
        g.fill(w1 - 1, by - 4, w1, by + 16, GOLD);
        int mx = bx + (int) (game.cool * bw);
        g.fill(mx - 1, by - 6, mx + 2, by + 18, 0xFFFFFFFF);
        feedback(g, l, t);
        drawWrapped(g, Component.translatable("gui.mundos_de_axelis.forja.templar_ayuda"), l + 190, t + 100, 140, TEXT);
    }

    private void renderDone(GuiGraphics g, int l, int t) {
        ForjaGame.Result r = game.result;
        String key = r == ForjaGame.Result.PERFECT ? "perfecta" : r == ForjaGame.Result.OK ? "lista" : "quebrada";
        int col = r == ForjaGame.Result.PERFECT ? GOLD : r == ForjaGame.Result.OK ? 0xFF8CFFA8 : 0xFFFF6060;
        if (r != ForjaGame.Result.FAIL) {
            renderKey(g, l + W / 2 - 32, t + 40, 4.0F, 0xFFFFFFFF);
        }
        float sc = (float) Math.min(2.0, 0.6 + doneT * 4);
        g.pose().pushPose();
        g.pose().translate(l + W / 2.0, t + 120, 0);
        g.pose().scale(sc, sc, 1.0F);
        g.drawCenteredString(font, Component.translatable("gui.mundos_de_axelis.forja.fin_" + key), 0, 0, col);
        g.pose().popPose();
        g.drawCenteredString(font, Component.translatable("gui.mundos_de_axelis.forja.resumen", game.perfects, game.goods, game.misses), l + W / 2, t + 150, TEXT);
        g.drawCenteredString(font, Component.translatable("gui.mundos_de_axelis.forja.sub_" + key), l + W / 2, t + 166, DIM);
        if (doneT > 0.6) {
            g.drawCenteredString(font, Component.translatable("gui.mundos_de_axelis.forja.cerrar"), l + W / 2, t + H - 16, DIM);
        }
    }

    private void feedback(GuiGraphics g, int l, int t) {
        if (game.lastHitT < 0.9 && game.lastHit >= 0) {
            String k = game.lastHit == 2 ? "perfecto" : game.lastHit == 1 ? "bien" : "fallo";
            int col = game.lastHit == 2 ? GOLD : game.lastHit == 1 ? TEXT : 0xFFFF5050;
            float sc = 2.0F - (float) game.lastHitT;
            g.pose().pushPose();
            g.pose().translate(l + W / 2.0, t + 88, 0);
            g.pose().scale(sc, sc, 1.0F);
            g.drawCenteredString(font, Component.translatable("gui.mundos_de_axelis.forja.golpe_" + k), 0, 0, col);
            g.pose().popPose();
        }
    }

    /** La llave de la caja del tier, grande y teñida del color del metal caliente. */
    private void renderKey(GuiGraphics g, int x, int y, float scale, int argb) {
        ItemStack key = new ItemStack(ModItems.KEYS.get(CrateKind.byIndex(tier)).get());
        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        g.pose().scale(scale, scale, 1.0F);
        RenderSystem.setShaderColor(((argb >> 16) & 255) / 255.0F, ((argb >> 8) & 255) / 255.0F, (argb & 255) / 255.0F, 1.0F);
        g.renderItem(key, 0, 0);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        g.pose().popPose();
    }

    private void drawWrapped(GuiGraphics g, Component text, int x, int y, int w, int color) {
        int yy = y;
        for (net.minecraft.util.FormattedCharSequence line : font.split(text, w)) {
            g.drawString(font, line, x, yy, color, false);
            yy += 10;
        }
    }
}
