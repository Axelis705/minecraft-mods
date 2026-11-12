package com.casino.client;

import com.casino.diamond.RaceBE;
import com.casino.game.HorseRace;
import com.casino.plat.client.CasinoBER;
import com.casino.plat.client.Draw;
import com.casino.plat.client.VB;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.ArrayList;
import java.util.List;

/**
 * La pantalla gigante de las carreras: tablero de apuestas, salida con cuenta regresiva, la carrera con cámara que sigue al que va
 * adelante (gradas y pista en capas que se mueven a distinta velocidad), barra de posiciones y resultados con confeti.
 * Todo se calcula con la semilla de la carrera y el reloj del mundo, así que todos los jugadores ven lo mismo.
 */
public class RaceScreenRenderer extends CasinoBER<RaceBE> {
    private static final int FULL = 0xF000F0;
    private static final float ATLAS = 256F;
    /** Parte del recorrido que se ve a lo ancho de la pantalla. */
    private static final double VIEW = 0.16;
    private static final double START_CAM = 0.06;

    // regiones del atlas (ver herramientas/race_scene.py)
    private static final float[] STANDS = {0, 0, 256, 64};
    private static final float[] GRASS = {128, 64, 64, 16};
    private static final float[] DIRT = {192, 64, 64, 16};
    private static final float[] RAIL = {128, 80, 64, 8};
    private static final float[] GATE = {0, 96, 32, 48};
    private static final float[] FINISH = {32, 96, 16, 64};
    private static final float[] POLE = {48, 96, 8, 40};
    private static final float[] PANEL = {64, 96, 64, 64};
    private static final float[] WHITE = {249, 249, 6, 6};

    private float W;
    private float H;
    private float U;

    public RaceScreenRenderer(BlockEntityRendererProvider.Context ctx) {
    }

    /** Un texto: (x, y) = esquina inferior izquierda en bloques; size = alto de la letra en bloques; align 0 izquierda, .5 centro, 1 derecha. */
    private record Txt(Component c, float x, float y, float size, int color, float align, boolean shadow) {
    }

    @Override
    protected void draw(RaceBE be, float partial, PoseStack ps, Draw d, int light, int overlay) {
        if (be.getLevel() == null || be.seed == 0) {
            return;
        }
        W = be.width;
        H = be.height;
        U = H / 6F;
        long now = be.getLevel().getGameTime();
        RaceBE.Phase ph = be.phase(now);
        HorseRace r = be.race();
        List<Txt> texts = new ArrayList<>();

        ps.pushPose();
        ps.translate(0.5, 0, 0.5);
        ps.mulPose(Axis.YP.rotationDegrees(-be.facing().toYRot()));
        ps.translate(-0.5 + be.left, 0, -0.5);
        PoseStack.Pose pose = ps.last();

        float time = (now % 24000L) + partial;
        double t = be.raceSeconds(now, partial);

        if (ph == RaceBE.Phase.BET) {
            drawBoard(d, pose, be, r, now, partial, time, texts);
        } else {
            boolean results = ph == RaceBE.Phase.RESULT && be.elapsed(now) - (HorseRace.BET_TICKS + HorseRace.GATE_TICKS + r.raceTicks()) > 70;
            if (results) {
                drawResults(d, pose, be, r, now, time, texts);
            } else {
                drawRace(d, pose, be, r, Math.max(0, t), ph, now, partial, time, texts);
            }
        }

        // textos al final (en 1.20 pedir otro pincel cierra el anterior)
        var font = Draw.font();
        for (Txt x : texts) {
            float s = x.size() / 9F;
            float w = font.width(x.c()) * s;
            if (x.shadow()) {
                ps.pushPose();
                ps.translate(x.x() - w * x.align() + x.size() * 0.09F, x.y() + x.size() - x.size() * 0.09F, 0.030F);
                ps.scale(s, -s, s);
                MutableComponent sh = Component.literal(x.c().getString());
                if (x.c().getStyle().isBold()) {
                    sh = sh.withStyle(ChatFormatting.BOLD);
                }
                d.text(ps, sh, 0, 0, 0xFF000000 | ((x.color() & 0xFCFCFC) >> 2), 0, FULL);
                ps.popPose();
            }
            ps.pushPose();
            ps.translate(x.x() - w * x.align(), x.y() + x.size(), 0.032F);
            ps.scale(s, -s, s);
            d.text(ps, x.c(), 0, 0, x.color(), 0, FULL);
            ps.popPose();
        }
        ps.popPose();
    }

    // ------------------------------------------------------------------ quads

    private static void v(VB vb, PoseStack.Pose p, float x, float y, float z, float u, float w, int argb) {
        vb.v(p, x, y, z, (argb >> 16) & 255, (argb >> 8) & 255, argb & 255, 255, u, w, OverlayTexture.NO_OVERLAY, FULL, 0, 0, 1);
    }

    /** Rectángulo con un pedazo del atlas, recortado a lo ancho de la pantalla. */
    private void rect(VB vb, PoseStack.Pose p, float x0, float y0, float x1, float y1, float z, float[] reg, float su0, float su1, int argb) {
        if (x1 <= 0 || x0 >= W || x1 <= x0) {
            return;
        }
        float u0 = (reg[0] + reg[2] * su0) / ATLAS;
        float u1 = (reg[0] + reg[2] * su1) / ATLAS;
        if (x0 < 0) {
            u0 += (0 - x0) / (x1 - x0) * (u1 - u0);
            x0 = 0;
        }
        if (x1 > W) {
            u1 -= (x1 - W) / (x1 - x0) * (u1 - u0);
            x1 = W;
        }
        float v0 = reg[1] / ATLAS;
        float v1 = (reg[1] + reg[3]) / ATLAS;
        v(vb, p, x0, y0, z, u0, v1, argb);
        v(vb, p, x1, y0, z, u1, v1, argb);
        v(vb, p, x1, y1, z, u1, v0, argb);
        v(vb, p, x0, y1, z, u0, v0, argb);
    }

    private void fill(VB vb, PoseStack.Pose p, float x0, float y0, float x1, float y1, float z, int argb) {
        rect(vb, p, x0, y0, x1, y1, z, WHITE, 0, 1, argb);
    }

    /** Franja que se repite de lado y se desplaza (offset en bloques). tileW = ancho de cada repetición. */
    private void strip(VB vb, PoseStack.Pose p, float y0, float y1, float z, float[] reg, float tileW, double offset) {
        double start = -((offset % tileW) + tileW) % tileW;
        for (double x = start; x < W; x += tileW) {
            rect(vb, p, (float) x, y0, (float) (x + tileW), y1, z, reg, 0, 1, 0xFFFFFFFF);
        }
    }

    // ------------------------------------------------------------------ caballos

    private void horse(Draw d, PoseStack.Pose p, HorseRace r, int i, float x, float y, float h, int frame, float z) {
        float w = h * RaceArt.FW / RaceArt.FH;
        if (x + w <= 0 || x >= W) {
            return;
        }
        int pat = r.pattern[i] % 4;
        float u0 = (frame & 7) * RaceArt.FW;
        int[] layers = {1, 2 + pat, 6 + pat, 0};
        int[] tints = {RaceArt.COAT[r.coat[i] % RaceArt.COAT.length], RaceArt.silkA(r, i), RaceArt.silkB(r, i), 0xFFFFFFFF};
        for (int k = 0; k < 4; k++) {
            VB vb = d.cutout(RaceArt.HORSE);
            float[] reg = {u0, RaceArt.layerV(layers[k]), RaceArt.FW, RaceArt.FH};
            hrect(vb, p, x, y, x + w, y + h, z + k * 0.0006F, reg, tints[k]);
        }
    }

    private void hrect(VB vb, PoseStack.Pose p, float x0, float y0, float x1, float y1, float z, float[] reg, int argb) {
        float su0 = 0;
        float su1 = 1;
        float u0 = (reg[0] + reg[2] * su0) / RaceArt.SHEET_W;
        float u1 = (reg[0] + reg[2] * su1) / RaceArt.SHEET_W;
        if (x0 < 0) {
            u0 += (0 - x0) / (x1 - x0) * (u1 - u0);
            x0 = 0;
        }
        if (x1 > W) {
            u1 -= (x1 - W) / (x1 - x0) * (u1 - u0);
            x1 = W;
        }
        float v0 = reg[1] / RaceArt.SHEET_H;
        float v1 = (reg[1] + reg[3]) / RaceArt.SHEET_H;
        v(vb, p, x0, y0, z, u0, v1, argb);
        v(vb, p, x1, y0, z, u1, v1, argb);
        v(vb, p, x1, y1, z, u1, v0, argb);
        v(vb, p, x0, y1, z, u0, v0, argb);
    }

    /** Insignia con el número (cuadro de su seda) en el mundo. */
    private void badge(VB vb, PoseStack.Pose p, HorseRace r, int i, float x, float y, float s, float z, List<Txt> texts) {
        fill(vb, p, x - s * 0.08F, y - s * 0.08F, x + s * 1.08F, y + s * 1.08F, z, 0xFF000000);
        fill(vb, p, x, y, x + s, y + s, z + 0.001F, RaceArt.silkA(r, i));
        fill(vb, p, x, y, x + s, y + s * 0.2F, z + 0.002F, RaceArt.silkB(r, i));
        texts.add(new Txt(Component.literal(String.valueOf(i + 1)).withStyle(ChatFormatting.BOLD), x + s / 2, y + s * 0.2F, s * 0.62F,
                RaceArt.light(RaceArt.silkA(r, i)) ? 0xFF101010 : 0xFFFFFFFF, 0.5F, false));
    }

    // ------------------------------------------------------------------ fases

    private void drawBoard(Draw d, PoseStack.Pose p, RaceBE be, HorseRace r, long now, float partial, float time, List<Txt> texts) {
        VB vb = d.cutout(RaceArt.SCENE);
        fill(vb, p, 0, 0, W, H, 0.004F, 0xFF06060A);
        // gradas arriba (se mueven despacio)
        strip(vb, p, H - 1.6F * U, H, 0.006F, STANDS, 6.4F * U, time * 0.01);
        rect(vb, p, 0, 0, W, H - 1.6F * U, 0.006F, PANEL, 0, 1, 0xFFFFFFFF);
        fill(vb, p, 0, H - 1.65F * U, W, H - 1.6F * U, 0.008F, 0xFFC8A040);
        int left = be.betTicksLeft(now);
        boolean hurry = left < 200;
        float rowH = 0.6F * U;
        float top = H - 1.95F * U;
        int mine = myHorse(be);
        for (int i = 0; i < HorseRace.N; i++) {
            float y = top - (i + 1) * rowH;
            int bg = i % 2 == 0 ? 0xFF1C1A2C : 0xFF141220;
            if (i == mine) {
                bg = 0xFF3A2E10;
            }
            fill(vb, p, 0.25F, y + 0.04F, W - 0.25F, y + rowH - 0.04F, 0.010F, bg);
            badge(vb, p, r, i, 0.4F, y + 0.1F * U, 0.42F * U, 0.012F, texts);
            texts.add(new Txt(RaceBE.horseName(r, i).withStyle(ChatFormatting.BOLD), 2.55F * U, y + 0.2F * U, 0.26F * U, 0xFFF4F0FF, 0, true));
            texts.add(new Txt(Component.literal(RaceArt.stars(r.form[i])), W * 0.62F, y + 0.21F * U, 0.22F * U, 0xFFE8C050, 0.5F, false));
            texts.add(new Txt(Component.literal(r.oddsText(i)).withStyle(ChatFormatting.BOLD), W - 0.5F, y + 0.17F * U, 0.32F * U,
                    0xFFFFD040, 1, true));
        }
        // caballitos al paso junto a cada nombre
        for (int i = 0; i < HorseRace.N; i++) {
            float y = top - (i + 1) * rowH;
            horse(d, p, r, i, 1.0F * U, y + 0.04F * U, 0.52F * U, (int) (time / 3 + i * 2), 0.014F);
        }
        texts.add(new Txt(Component.translatable("casino.race.next").withStyle(ChatFormatting.BOLD), 0.4F, H - 1.5F * U + 1.0F * U, 0.34F * U,
                0xFFFFFFFF, 0, true));
        int col = hurry && ((int) time / 5) % 2 == 0 ? 0xFFFF5050 : 0xFF60FF90;
        texts.add(new Txt(Component.literal(RaceArt.time(left)).withStyle(ChatFormatting.BOLD), W - 0.4F, H - 1.5F * U + 0.95F * U, 0.5F * U,
                col, 1, true));
        texts.add(new Txt(Component.translatable("casino.race.board_odds"), W - 0.5F, top + 0.06F * U, 0.18F * U, 0xFFB0A8C8, 1, false));
        texts.add(new Txt(Component.translatable("casino.race.board_form"), W * 0.62F, top + 0.06F * U, 0.18F * U, 0xFFB0A8C8, 0.5F, false));
        texts.add(new Txt(Component.translatable("casino.race.board_hint", RaceClient.keyName()), W / 2, 0.12F * U, 0.22F * U, 0xFFE0D8FF, 0.5F, true));
    }

    private int myHorse(RaceBE be) {
        var pl = Minecraft.getInstance().player;
        if (pl == null) {
            return -1;
        }
        RaceBE.Bet b = be.bets.get(pl.getUUID());
        return b == null ? -1 : b.horse();
    }

    private double camera(HorseRace r, double t) {
        double lead = 0;
        for (int i = 0; i < HorseRace.N; i++) {
            lead = Math.max(lead, r.progress(i, t));
        }
        return Math.max(START_CAM, Math.min(1.03, lead - 0.035));
    }

    private float sx(double prog, double cam) {
        return (float) ((prog - cam) * (W / VIEW) + W * 0.5);
    }

    private void drawRace(Draw d, PoseStack.Pose p, RaceBE be, HorseRace r, double t, RaceBE.Phase ph, long now, float partial, float time,
                          List<Txt> texts) {
        double cam = camera(r, t);
        double scroll = cam * (W / VIEW);
        VB vb = d.cutout(RaceArt.SCENE);
        fill(vb, p, 0, 0, W, H, 0.004F, 0xFF06060A);
        float yInfo = 0.62F * U;
        float yRail0 = yInfo;
        float yDirt0 = yInfo + 0.22F * U;
        float yDirt1 = 3.55F * U;
        float yGrass1 = 4.0F * U;
        // gradas (lentas), pasto con valla, arena por filas
        strip(vb, p, yGrass1, H, 0.006F, STANDS, (H - yGrass1) * 4, scroll * 0.3);
        strip(vb, p, yDirt1, yGrass1, 0.006F, GRASS, (yGrass1 - yDirt1) * 4, scroll);
        float rowH = (yDirt1 - yDirt0) / 6;
        for (int k = 0; k < 6; k++) {
            strip(vb, p, yDirt0 + k * rowH, yDirt0 + (k + 1) * rowH, 0.006F, DIRT, rowH * 4, scroll + k * 0.7);
        }
        // marcas de distancia y meta
        for (int k = 1; k <= 3; k++) {
            float x = sx(k * 0.25, cam);
            rect(vb, p, x - 0.06F * U, yDirt1 - 0.1F * U, x + 0.06F * U, yDirt1 + 0.5F * U, 0.008F, POLE, 0, 1, 0xFFFFFFFF);
            if (x > -0.5F && x < W + 0.5F) {
                texts.add(new Txt(Component.literal((1000 - k * 250) + " m"), x, yDirt1 + 0.55F * U, 0.2F * U, 0xFFFFFFFF, 0.5F, true));
            }
        }
        float fx = sx(1.0, cam);
        rect(vb, p, fx - 0.14F * U, yDirt0, fx + 0.14F * U, yDirt1 + 0.75F * U, 0.0125F, FINISH, 0, 1, 0xFFFFFFFF);
        // cajones de salida
        float gx = sx(0, cam);
        for (int i = 0; i < HorseRace.N; i++) {
            float yb = laneY(i, yDirt0, rowH);
            rect(vb, p, gx - 0.2F * U, yb - 0.05F * U, gx + 0.45F * U, yb + 0.95F * U, 0.0105F + i * 0.0002F, GATE, 0, 1, 0xFFFFFFFF);
        }
        // valla del frente y franja de información
        strip(vb, p, yRail0, yRail0 + 0.3F * U, 0.020F, RAIL, 0.3F * U * 8, scroll);
        fill(vb, p, 0, 0, W, yInfo, 0.021F, 0xFF0C0A14);
        fill(vb, p, 0, yInfo - 0.04F * U, W, yInfo, 0.022F, 0xFFC8A040);

        // caballos (de atrás hacia adelante)
        float hh = 1.1F * U;
        int[] stand = r.standings(t);
        boolean running = ph == RaceBE.Phase.RUN || ph == RaceBE.Phase.RESULT;
        for (int i = 0; i < HorseRace.N; i++) {
            double pr = r.progress(i, t);
            float yb = laneY(i, yDirt0, rowH);
            float nose = sx(pr, cam);
            float w = hh * RaceArt.FW / RaceArt.FH;
            float x = nose - w * 0.86F;
            int frame = running && t > 0 ? (int) (pr * 430 + i * 3) : 6;
            horse(d, p, r, i, x, yb - 0.08F * U, hh, frame, 0.013F + i * 0.0015F);
            VB ub = d.cutout(RaceArt.SCENE);
            if (nose > -0.5F && nose < W + 0.5F) {
                badge(ub, p, r, i, Math.max(0.05F, Math.min(W - 0.4F * U, x + w * 0.45F)), yb + hh * 0.98F, 0.3F * U, 0.024F, texts);
            }
        }

        // barra de posiciones (abajo)
        VB ub = d.cutout(RaceArt.SCENE);
        float bx0 = 2.2F * U;
        float bx1 = W - 2.6F * U;
        float by = yInfo * 0.42F;
        fill(ub, p, bx0, by - 0.03F * U, bx1, by + 0.03F * U, 0.023F, 0xFF5A5470);
        rect(ub, p, bx1 - 0.06F * U, by - 0.2F * U, bx1 + 0.06F * U, by + 0.2F * U, 0.0235F, FINISH, 0, 0.25F, 0xFFFFFFFF);
        for (int k = HorseRace.N - 1; k >= 0; k--) {
            int i = indexAt(stand, k);
            double pr = Math.min(1, r.progress(i, t));
            float x = (float) (bx0 + (bx1 - bx0) * pr);
            float s = 0.26F * U;
            badge(ub, p, r, i, x - s / 2, by - s / 2, s, 0.024F + (HorseRace.N - k) * 0.0008F, texts);
        }
        int leader = indexAt(stand, 0);
        double leadP = Math.min(1, r.progress(leader, t));
        int meters = (int) Math.max(0, Math.round((1 - leadP) * 1000));
        texts.add(new Txt(Component.literal(meters + " m").withStyle(ChatFormatting.BOLD), 0.3F, by - 0.13F * U, 0.26F * U, 0xFFFFFFFF, 0, true));
        MutableComponent lead = Component.literal("1º ").append(RaceBE.horseName(r, leader));
        texts.add(new Txt(lead.withStyle(ChatFormatting.BOLD), W - 0.25F, by - 0.11F * U, 0.22F * U, 0xFFFFD040, 1, true));

        // orden en vivo arriba (como en la tele)
        fill(ub, p, 0, H - 0.62F * U, 0.3F + HorseRace.N * 0.62F * U + 0.1F, H, 0.0235F, 0xFF0C0A14);
        fill(ub, p, 0, H - 0.66F * U, 0.3F + HorseRace.N * 0.62F * U + 0.1F, H - 0.62F * U, 0.0236F, 0xFFC8A040);
        for (int k = 0; k < HorseRace.N; k++) {
            int i = indexAt(stand, k);
            float x = 0.3F + k * 0.62F * U;
            badge(ub, p, r, i, x + 0.22F * U, H - 0.5F * U, 0.34F * U, 0.024F, texts);
            texts.add(new Txt(Component.literal(String.valueOf(k + 1)), x + 0.08F * U, H - 0.45F * U, 0.18F * U, 0xFFFFFFFF, 0.5F, true));
        }

        // cuenta regresiva y "¡arrancan!"
        if (ph == RaceBE.Phase.GATE) {
            long gt = be.elapsed(now) - HorseRace.BET_TICKS;
            int left = (int) (HorseRace.GATE_TICKS - gt);
            int n = (left + 19) / 20;
            float frac = (left % 20 + partial) / 20F;
            if (n <= 3) {
                float size = (1.4F + 0.6F * frac) * U;
                texts.add(new Txt(Component.literal(String.valueOf(n)).withStyle(ChatFormatting.BOLD), W / 2, H * 0.45F, size,
                        n == 1 ? 0xFF60FF90 : 0xFFFFD040, 0.5F, true));
            } else {
                texts.add(new Txt(Component.translatable("casino.race.to_post").withStyle(ChatFormatting.BOLD), W / 2, H * 0.5F, 0.55F * U,
                        0xFFFFFFFF, 0.5F, true));
            }
        } else if (ph == RaceBE.Phase.RUN && t < 1.2) {
            texts.add(new Txt(Component.translatable("casino.race.go").withStyle(ChatFormatting.BOLD), W / 2, H * 0.48F, 0.8F * U, 0xFF60FF90,
                    0.5F, true));
        }
        // ganador al cruzar
        int w = r.order[0];
        if (t >= r.finish[w]) {
            float pulse = 0.9F + 0.1F * (float) Math.sin(time * 0.4);
            fill(ub, p, W * 0.18F, H * 0.47F, W * 0.82F, H * 0.47F + 0.95F * U, 0.0255F, 0xFF120E1C);
            fill(ub, p, W * 0.18F, H * 0.47F, W * 0.82F, H * 0.47F + 0.06F * U, 0.0257F, 0xFFC8A040);
            texts.add(new Txt(Component.translatable("casino.race.winner_big", w + 1, RaceBE.horseName(r, w)).withStyle(ChatFormatting.BOLD),
                    W / 2, H * 0.47F + 0.3F * U, 0.42F * U * pulse, 0xFFFFD040, 0.5F, true));
            confetti(ub, p, time, 0.027F, 40);
        }
    }

    private static float laneY(int i, float yDirt0, float rowH) {
        return yDirt0 + (HorseRace.N - 1 - i) * rowH * 0.92F + rowH * 0.15F;
    }

    private static int indexAt(int[] stand, int k) {
        for (int i = 0; i < stand.length; i++) {
            if (stand[i] == k) {
                return i;
            }
        }
        return 0;
    }

    private void confetti(VB vb, PoseStack.Pose p, float time, float z, int n) {
        for (int k = 0; k < n; k++) {
            float hx = (k * 0.6180339F) % 1F;
            float speed = 0.02F + (k * 37 % 11) * 0.003F;
            float y = H - ((time * speed + k * 0.37F) % (H + 0.4F));
            float x = hx * W + (float) Math.sin(time * 0.1 + k) * 0.25F;
            float s = 0.09F * U;
            fill(vb, p, x, y, x + s, y + s * 0.6F, z + k * 0.00001F, RaceArt.SILK[k % RaceArt.SILK.length]);
        }
    }

    private void drawResults(Draw d, PoseStack.Pose p, RaceBE be, HorseRace r, long now, float time, List<Txt> texts) {
        VB vb = d.cutout(RaceArt.SCENE);
        fill(vb, p, 0, 0, W, H, 0.004F, 0xFF06060A);
        strip(vb, p, H - 1.4F * U, H, 0.006F, STANDS, 5.6F * U, time * 0.01);
        rect(vb, p, 0, 0, W, H - 1.4F * U, 0.006F, PANEL, 0, 1, 0xFFFFFFFF);
        texts.add(new Txt(Component.translatable("casino.race.results").withStyle(ChatFormatting.BOLD), W / 2, H - 1.2F * U, 0.55F * U, 0xFFFFD040,
                0.5F, true));
        int[] medal = {0xFFE8C040, 0xFFC8C8D0, 0xFFC07840};
        float rowH = 1.05F * U;
        for (int k = 0; k < 3; k++) {
            int i = r.order[k];
            float y = H - 1.7F * U - (k + 1) * rowH;
            vb = d.cutout(RaceArt.SCENE);
            fill(vb, p, 0.4F, y + 0.06F, W - 0.4F, y + rowH - 0.06F, 0.010F, k == 0 ? 0xFF2E2410 : 0xFF181626);
            fill(vb, p, 0.4F, y + 0.06F, 0.55F, y + rowH - 0.06F, 0.011F, medal[k]);
            texts.add(new Txt(Component.literal((k + 1) + "º").withStyle(ChatFormatting.BOLD), 1.0F * U, y + 0.3F * U, 0.42F * U, medal[k], 0.5F, true));
            horse(d, p, r, i, 1.5F * U, y + 0.1F * U, rowH * 0.82F, (int) (time / 2) + k, 0.013F);
            VB ub = d.cutout(RaceArt.SCENE);
            badge(ub, p, r, i, 3.0F * U, y + 0.33F * U, 0.38F * U, 0.014F, texts);
            texts.add(new Txt(RaceBE.horseName(r, i).withStyle(ChatFormatting.BOLD), 3.6F * U, y + 0.36F * U, 0.32F * U, 0xFFFFFFFF, 0, true));
            if (k == 0) {
                texts.add(new Txt(Component.translatable("casino.race.paid_odds", r.oddsText(i)).withStyle(ChatFormatting.BOLD), W - 0.6F,
                        y + 0.36F * U, 0.3F * U, 0xFF60FF90, 1, true));
            }
        }
        // lo que ganó o perdió quien mira
        int mine = myHorse(be);
        var pl = Minecraft.getInstance().player;
        if (mine >= 0 && pl != null) {
            RaceBE.Bet b = be.bets.get(pl.getUUID());
            Component c = mine == r.order[0]
                    ? Component.translatable("casino.race.screen_won", r.payout(mine, b.amount())).withStyle(ChatFormatting.BOLD)
                    : Component.translatable("casino.race.screen_lost", r.place[mine] + 1);
            texts.add(new Txt(c, W / 2, 0.25F * U, 0.3F * U, mine == r.order[0] ? 0xFF60FF90 : 0xFFC0B8D8, 0.5F, true));
        }
        VB ub = d.cutout(RaceArt.SCENE);
        confetti(ub, p, time, 0.026F, 60);
    }
}
