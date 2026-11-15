package com.casino.client;

import com.casino.data.CasinoStats;
import com.casino.diamond.RecordScreenBE;
import com.casino.net.LeaderboardPacket;
import com.casino.plat.Ids;
import com.casino.plat.client.CasinoBER;
import com.casino.plat.client.Draw;
import com.casino.plat.client.VB;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Pantalla de récords: marco negro con filo dorado, encabezado con el nombre del canal, la lista (medallas, nombre, barra que crece y
 * el número), y abajo una cinta con los últimos premios. Al cambiar de canal sale estática como en una tele vieja y el "CH" en verde.
 * Se adapta al tamaño: en una pantalla de 1×1 caben pocas filas; en una de 8×4 caben las 10 y dos columnas.
 */
public class RecordScreenRenderer extends CasinoBER<RecordScreenBE> {
    private static final ResourceLocation TEX = Ids.casino("textures/entity/record_screen.png");
    private static final int FULL = 0xF000F0;
    private static final float ATLAS = 64F;
    private static final float[] WHITE = {1, 1, 6, 6};
    private static final float[] NOISE = {0, 16, 64, 48};
    private static final float Z = 0.0015F;

    /** Color de cada canal (encabezado y barras). */
    private static final int[] ACCENT = {0xFFF2C230, 0xFF3FD27A, 0xFF35C3E8, 0xFFE8484A, 0xFFB46CF0, 0xFFFF9A3C, 0xFF4C8DF6, 0xFFFF6FB5};
    private static final int[] MEDAL = {0xFFF6CB3A, 0xFFC9D1DB, 0xFFD08A4E};

    private float W;
    private float H;
    private float time;
    private final List<Txt> texts = new ArrayList<>();

    public RecordScreenRenderer(BlockEntityRendererProvider.Context ctx) {
    }

    /** Un texto: (x, y) = esquina inferior izquierda; size = alto de la letra; align 0 izquierda, .5 centro, 1 derecha. */
    private record Txt(Component c, float x, float y, float size, int color, float align, boolean shadow) {
    }

    @Override
    protected void draw(RecordScreenBE be, float partial, PoseStack ps, Draw d, int light, int overlay) {
        if (be.getLevel() == null || !be.anchor) {
            return;
        }
        W = be.width;
        H = be.height;
        long now = be.getLevel().getGameTime();
        time = now + partial;
        int ch = be.channel(now);
        int age = be.channelAge(now);
        texts.clear();

        ps.pushPose();
        ps.translate(0.5, 0, 0.5);
        ps.mulPose(Axis.YP.rotationDegrees(-be.facing().toYRot()));
        ps.translate(-0.5, -(H - 1), -0.5 + 0.128);
        PoseStack.Pose p = ps.last();
        VB vb = d.cutout(TEX);

        // marco: negro brillante con filo dorado
        float b = Math.min(0.09F, 0.035F + 0.012F * Math.min(W, H));
        fill(vb, p, 0, 0, W, H, 0, 0xFF07070B);
        frame(vb, p, b * 0.35F, b * 0.35F, W - b * 0.35F, H - b * 0.35F, b * 0.22F, Z, 0xFFC9A227);
        float x0 = b, y0 = b, x1 = W - b, y1 = H - b;
        int acc = ACCENT[ch % ACCENT.length];

        // fondo con degradado y la barra de "refresco" que sube
        int bgLo = 0xFF070A1C;
        int bgHi = mix(0xFF141038, acc, 0.10F);
        gquad(vb, p, x0, y0, x1, y1, Z * 2, bgLo, bgHi);
        float sweep = (time * 0.012F) % 1.6F - 0.3F;
        float sh = 0.12F * (y1 - y0);
        float sy = y0 + sweep * (y1 - y0);
        sweepBand(vb, p, x0, x1, y0, y1, sy, sh, bgLo, bgHi);

        LeaderboardPacket data = Leaderboard.get();
        float hh = clamp(H * 0.17F, 0.2F, 1.0F);
        float th = H >= 2 ? clamp(H * 0.085F, 0.15F, 0.5F) : 0F;
        boolean staticNoise = age < 12 || data == null;

        if (staticNoise) {
            noise(vb, p, x0, y0, x1, y1, Z * 4, acc);
            if (data == null) {
                txt(Component.translatable("casino.records.connecting"), (x0 + x1) / 2, (y0 + y1) / 2 - hh * 0.25F, hh * 0.5F, 0xFFFFFFFF, 0.5F, true);
            }
        } else {
            // encabezado
            float hy0 = y1 - hh;
            gquad(vb, p, x0, hy0, x1, y1, Z * 4, mix(acc, 0xFF000000, 0.55F), mix(acc, 0xFF000000, 0.15F));
            fill(vb, p, x0, hy0 - hh * 0.05F, x1, hy0, Z * 5, acc);
            Component title = Component.translatable("casino.records.ch_" + ch).withStyle(ChatFormatting.BOLD);
            float ts = fitSize(title, hh * 0.58F, (x1 - x0) * 0.92F);
            txt(title, (x0 + x1) / 2, hy0 + (hh - ts) / 2 - ts * 0.05F, ts, 0xFFFFFFFF, 0.5F, true);

            float by0 = y0 + th + (th > 0 ? hh * 0.06F : 0);
            float by1 = hy0 - hh * 0.12F;
            int rowAge = age - 12;
            switch (ch) {
                case 0 -> list(vb, p, data.lists.get(LeaderboardPacket.NET), x0, by0, x1, by1, acc, rowAge, true);
                case 1 -> list(vb, p, data.lists.get(LeaderboardPacket.WAGERED), x0, by0, x1, by1, acc, rowAge, false);
                case 2 -> list(vb, p, data.lists.get(LeaderboardPacket.BETS), x0, by0, x1, by1, acc, rowAge, false);
                case 3 -> list(vb, p, data.lists.get(LeaderboardPacket.LOST), x0, by0, x1, by1, acc, rowAge, false);
                case 4 -> list(vb, p, data.lists.get(LeaderboardPacket.BIGGEST), x0, by0, x1, by1, acc, rowAge, false);
                case 5 -> today(vb, p, data, x0, by0, x1, by1, acc, rowAge);
                case 6 -> casino(vb, p, data, x0, by0, x1, by1, acc, rowAge);
                default -> recent(vb, p, data, x0, by0, x1, by1, acc, rowAge);
            }
            if (th > 0) {
                ticker(vb, p, data, x0, y0, x1, y0 + th);
            }
        }
        // "CH 3" en verde arriba a la derecha, como en las teles viejas
        if (age < 50) {
            float cs = clamp(H * 0.09F, 0.1F, 0.35F);
            txt(Component.literal(String.format(Locale.ROOT, "CH %02d", ch + 1)), x1 - cs * 0.4F, y1 - cs * 1.4F, cs, 0xFF6CFF6C, 1F, true);
        }

        flushTexts(ps, d);
        ps.popPose();
    }

    // ------------------------------------------------------------------ canales

    /** Una lista de récords: medalla, nombre, barra y número; las filas entran una tras otra. */
    private void list(VB vb, PoseStack.Pose p, List<CasinoStats.Row> rows, float x0, float y0, float x1, float y1, int acc, int age, boolean plus) {
        if (rows.isEmpty()) {
            empty(x0, y0, x1, y1);
            return;
        }
        float bodyH = y1 - y0;
        float rh = Math.min(clamp(bodyH / 6F, 0.15F, 0.6F), (x1 - x0) / 8.5F);
        int fit = Math.max(1, Math.min(rows.size(), (int) (bodyH / rh)));
        long max = Math.max(1, rows.get(0).value());
        float pad = rh * 0.12F;
        for (int i = 0; i < fit; i++) {
            int ra = age - i * 3;
            if (ra < 0) {
                break;
            }
            CasinoStats.Row r = rows.get(i);
            float ry1 = y1 - i * rh;
            float ry0 = ry1 - rh + pad * 0.5F;
            float grow = ease(Math.min(1F, ra / 12F));
            // fondo de la fila (la primera late en dorado)
            int zebra = i % 2 == 0 ? 0xFF161A33 : 0xFF10132A;
            if (i == 0) {
                float pulse = 0.5F + 0.5F * (float) Math.sin(time * 0.15F);
                zebra = mix(zebra, 0xFF4A3A0A, 0.35F + 0.25F * pulse);
            }
            fill(vb, p, x0 + pad, ry0, x1 - pad, ry1, Z * 6, zebra);
            // barra proporcional
            float bx0 = x0 + pad + rh * 1.1F;
            float bw = (x1 - pad - bx0) * (r.value() / (float) max) * grow;
            gquad(vb, p, bx0, ry0, bx0 + bw, ry0 + (ry1 - ry0) * 0.18F, Z * 7, mix(acc, 0xFF000000, 0.4F), acc);
            // medalla / número
            float ms = (ry1 - ry0) * 0.86F;
            float mx = x0 + pad + rh * 0.12F;
            float my = ry0 + (ry1 - ry0 - ms) / 2;
            int mc = i < 3 ? MEDAL[i] : 0xFF2A2F55;
            fill(vb, p, mx - ms * 0.06F, my - ms * 0.06F, mx + ms * 1.06F, my + ms * 1.06F, Z * 7, 0xFF000000);
            gquad(vb, p, mx, my, mx + ms, my + ms, Z * 8, mix(mc, 0xFF000000, 0.35F), mc);
            float s = (ry1 - ry0) * 0.62F;
            txt(Component.literal(String.valueOf(i + 1)).withStyle(ChatFormatting.BOLD), mx + ms / 2, my + (ms - s) / 2 + s * 0.08F, s * 0.92F,
                    i < 3 ? 0xFF1A1205 : 0xFFFFFFFF, 0.5F, false);
            // nombre y número
            String val = (plus ? "+" : "") + fmt(r.value());
            float vw = width(val, s);
            float nameX = mx + ms + rh * 0.2F;
            float ty = ry0 + (ry1 - ry0 - s) / 2 + s * 0.12F;
            String name = clip(r.name(), x1 - pad - rh * 0.25F - vw - rh * 0.3F - nameX, s);
            txt(Component.literal(name), nameX, ty, s, i == 0 ? 0xFFFFE680 : 0xFFF0F0F0, 0F, true);
            txt(Component.literal(val).withStyle(ChatFormatting.BOLD), x1 - pad - rh * 0.25F, ty, s, i == 0 ? 0xFFFFD84A : lighten(acc), 1F, true);
        }
    }

    /** Hoy: dos columnas (ganancia y apostado) si cabe; si no, solo la ganancia. */
    private void today(VB vb, PoseStack.Pose p, LeaderboardPacket data, float x0, float y0, float x1, float y1, int acc, int age) {
        List<CasinoStats.Row> net = data.lists.get(LeaderboardPacket.TODAY_NET);
        List<CasinoStats.Row> wag = data.lists.get(LeaderboardPacket.TODAY_WAGERED);
        if (net.isEmpty() && wag.isEmpty()) {
            empty(x0, y0, x1, y1);
            return;
        }
        float sub = clamp((y1 - y0) * 0.12F, 0.1F, 0.4F);
        if (x1 - x0 >= 3.6F) {
            float mid = (x0 + x1) / 2;
            label("casino.records.today_net", x0, y1 - sub, mid, sub, acc);
            label("casino.records.today_wagered", mid, y1 - sub, x1, sub, acc);
            fill(vb, p, mid - 0.01F, y0, mid + 0.01F, y1 - sub * 1.2F, Z * 6, mix(acc, 0xFF000000, 0.5F));
            list(vb, p, net, x0, y0, mid, y1 - sub * 1.3F, acc, age, true);
            list(vb, p, wag, mid, y0, x1, y1 - sub * 1.3F, 0xFF3FD27A, age, false);
        } else {
            label("casino.records.today_net", x0, y1 - sub, x1, sub, acc);
            list(vb, p, net, x0, y0, x1, y1 - sub * 1.3F, acc, age, true);
        }
    }

    /** El casino en números: totales a la izquierda y barras de apuestas por juego a la derecha (si hay espacio). */
    private void casino(VB vb, PoseStack.Pose p, LeaderboardPacket data, float x0, float y0, float x1, float y1, int acc, int age) {
        long wag = 0, paid = 0, bets = 0;
        for (int i = 0; i < CasinoStats.GAMES; i++) {
            wag += data.gameWagered[i];
            paid += data.gamePaid[i];
            bets += data.gameBets[i];
        }
        boolean two = x1 - x0 >= 3.6F && y1 - y0 >= 1.0F;
        float lx1 = two ? x0 + (x1 - x0) * 0.46F : x1;
        String[] keys = {"casino.records.total_wagered", "casino.records.total_paid", "casino.records.house", "casino.records.bets",
                "casino.records.players", "casino.records.today"};
        long[] vals = {wag, paid, wag - paid, bets, data.players, data.todayWagered};
        float bodyH = y1 - y0;
        float rh = Math.min(clamp(bodyH / 6F, 0.14F, 0.6F), (lx1 - x0) / 7.5F);
        int fit = Math.min(keys.length, (int) (bodyH / rh));
        float s = rh * 0.56F;
        for (int i = 0; i < fit; i++) {
            if (age - i * 3 < 0) {
                break;
            }
            float ry1 = y1 - i * rh;
            float ty = ry1 - rh + (rh - s) / 2 + s * 0.1F;
            fill(vb, p, x0 + rh * 0.1F, ry1 - rh + rh * 0.08F, lx1 - rh * 0.1F, ry1, Z * 6, i % 2 == 0 ? 0xFF161A33 : 0xFF10132A);
            Component k = Component.translatable(keys[i]);
            String v = fmt(vals[i]);
            float vw = width(v, s);
            txt(Component.literal(clip(k.getString(), lx1 - x0 - rh * 0.5F - vw - rh * 0.3F, s)), x0 + rh * 0.25F, ty, s, 0xFFD8DCF0, 0F, true);
            int vc = i == 2 ? (vals[i] >= 0 ? 0xFF6CFF8C : 0xFFFF6C6C) : 0xFFFFFFFF;
            txt(Component.literal(v).withStyle(ChatFormatting.BOLD), lx1 - rh * 0.25F, ty, s, vc, 1F, true);
        }
        if (!two) {
            return;
        }
        // barras: apuestas por juego
        float bx0 = lx1 + (x1 - x0) * 0.04F;
        float gh = bodyH / CasinoStats.GAMES;
        float gs = Math.min(gh * 0.5F, (x1 - bx0) / 14F);
        long max = 1;
        for (long v : data.gameBets) {
            max = Math.max(max, v);
        }
        float labelW = 0;
        for (int i = 0; i < CasinoStats.GAMES; i++) {
            labelW = Math.max(labelW, width(Component.translatable("casino.records.game_" + i).getString(), gs));
        }
        labelW = Math.min(labelW, (x1 - bx0) * 0.42F);
        for (int i = 0; i < CasinoStats.GAMES; i++) {
            float gy1 = y1 - i * gh;
            float gy0 = gy1 - gh * 0.78F;
            float grow = ease(clamp((age - i * 2) / 14F, 0F, 1F));
            float barX = bx0 + labelW + gs * 0.5F;
            float barMax = x1 - gs * 0.3F - barX - width(fmt(max), gs) - gs * 0.4F;
            float bw = Math.max(0.01F, barMax * (data.gameBets[i] / (float) max) * grow);
            int c = ACCENT[(i + 1) % ACCENT.length];
            fill(vb, p, barX, gy0, barX + barMax, gy1, Z * 6, 0xFF10132A);
            gquad(vb, p, barX, gy0, barX + bw, gy1, Z * 7, mix(c, 0xFF000000, 0.45F), c);
            float ty = gy0 + (gy1 - gy0 - gs) / 2 + gs * 0.1F;
            txt(Component.literal(clip(Component.translatable("casino.records.game_" + i).getString(), labelW, gs)), bx0, ty, gs, 0xFFE0E0F0, 0F, true);
            txt(Component.literal(fmt(data.gameBets[i])), barX + bw + gs * 0.3F, ty, gs, 0xFFFFFFFF, 0F, true);
        }
    }

    /** Últimos premios: quién, cuánto y en qué juego. */
    private void recent(VB vb, PoseStack.Pose p, LeaderboardPacket data, float x0, float y0, float x1, float y1, int acc, int age) {
        List<CasinoStats.Row> rows = new ArrayList<>();
        for (CasinoStats.Win w : data.recent) {
            rows.add(new CasinoStats.Row(w.name(), w.amount()));
        }
        if (rows.isEmpty()) {
            empty(x0, y0, x1, y1);
            return;
        }
        float bodyH = y1 - y0;
        float rh = Math.min(clamp(bodyH / 6F, 0.15F, 0.6F), (x1 - x0) / 8.5F);
        int fit = Math.max(1, Math.min(rows.size(), (int) (bodyH / rh)));
        float pad = rh * 0.12F;
        float s = (rh - pad * 0.5F) * 0.6F;
        for (int i = 0; i < fit; i++) {
            if (age - i * 3 < 0) {
                break;
            }
            CasinoStats.Win w = data.recent.get(i);
            float ry1 = y1 - i * rh;
            float ry0 = ry1 - rh + pad * 0.5F;
            int c = ACCENT[(w.game() + 1) % ACCENT.length];
            fill(vb, p, x0 + pad, ry0, x1 - pad, ry1, Z * 6, i % 2 == 0 ? 0xFF161A33 : 0xFF10132A);
            fill(vb, p, x0 + pad, ry0, x0 + pad + rh * 0.12F, ry1, Z * 7, c);
            float ty = ry0 + (ry1 - ry0 - s) / 2 + s * 0.12F;
            String amount = "+" + fmt(w.amount());
            String game = Component.translatable("casino.records.game_" + Math.max(0, Math.min(CasinoStats.GAMES - 1, w.game()))).getString();
            float aw = width(amount, s);
            float gw = (x1 - x0) >= 3F ? width(game, s * 0.8F) : 0;
            float nameX = x0 + pad + rh * 0.35F;
            float right = x1 - pad - rh * 0.25F;
            txt(Component.literal(clip(w.name(), right - aw - gw - rh * 0.6F - nameX, s)), nameX, ty, s, 0xFFF0F0F0, 0F, true);
            if (gw > 0) {
                txt(Component.literal(game), right - aw - rh * 0.3F, ty + s * 0.1F, s * 0.8F, lighten(c), 1F, true);
            }
            txt(Component.literal(amount).withStyle(ChatFormatting.BOLD), right, ty, s, 0xFFFFD84A, 1F, true);
        }
    }

    /** Cinta de abajo: los últimos premios pasando de derecha a izquierda (letra por letra, para que no se salgan del marco). */
    private void ticker(VB vb, PoseStack.Pose p, LeaderboardPacket data, float x0, float y0, float x1, float y1) {
        gquad(vb, p, x0, y0, x1, y1, Z * 4, 0xFF2A0606, 0xFF5A0F12);
        fill(vb, p, x0, y1, x1, y1 + (y1 - y0) * 0.06F, Z * 5, 0xFFC9A227);
        float s = (y1 - y0) * 0.6F;
        float ty = y0 + (y1 - y0 - s) / 2 + s * 0.1F;
        // etiqueta fija "EN VIVO" a la izquierda
        Component live = Component.translatable("casino.records.live").withStyle(ChatFormatting.BOLD);
        float lw = width(live.getString(), s) + s * 0.8F;
        fill(vb, p, x0, y0, x0 + lw, y1, Z * 6, 0xFFD81E2A);
        boolean blink = ((int) (time / 10)) % 2 == 0;
        txt(live, x0 + lw / 2, ty, s, blink ? 0xFFFFFFFF : 0xFFFFC0C0, 0.5F, false);
        List<String> parts = new ArrayList<>();
        List<Integer> colors = new ArrayList<>();
        if (data.recent.isEmpty()) {
            parts.add(Component.translatable("casino.records.empty").getString() + "     ");
            colors.add(0xFFFFFFFF);
        }
        for (CasinoStats.Win w : data.recent) {
            parts.add("★ ");
            colors.add(0xFFFFD84A);
            parts.add(w.name());
            colors.add(0xFFFFE680);
            String game = Component.translatable("casino.records.game_" + Math.max(0, Math.min(CasinoStats.GAMES - 1, w.game()))).getString();
            parts.add(" " + Component.translatable("casino.records.won_in", fmt(w.amount()), game).getString() + "     ");
            colors.add(0xFFFFFFFF);
        }
        Font font = Draw.font();
        float k = s / 9F;
        float total = 0;
        for (String part : parts) {
            total += font.width(part) * k;
        }
        float start = x0 + lw + s * 0.3F;
        float end = x1 - s * 0.2F;
        if (total <= 0) {
            return;
        }
        float off = (time * 0.04F * Math.max(1F, s * 4F)) % total;
        for (int rep = 0; rep < 2 + (int) ((end - start) / total); rep++) {
            float x = start - off + rep * total;
            for (int i = 0; i < parts.size(); i++) {
                String part = parts.get(i);
                for (int c = 0; c < part.length(); c++) {
                    String ch = String.valueOf(part.charAt(c));
                    float cw = font.width(ch) * k;
                    if (x >= start && x + cw <= end && part.charAt(c) != ' ') {
                        texts.add(new Txt(Component.literal(ch), x, ty, s, colors.get(i), 0F, false));
                    }
                    x += cw;
                }
            }
        }
    }

    private void empty(float x0, float y0, float x1, float y1) {
        Component c = Component.translatable("casino.records.empty");
        float s = fitSize(c, clamp((y1 - y0) * 0.16F, 0.08F, 0.4F), (x1 - x0) * 0.9F);
        float bob = (float) Math.sin(time * 0.1F) * s * 0.1F;
        txt(c, (x0 + x1) / 2, (y0 + y1) / 2 - s / 2 + bob, s, 0xFFD8DCF0, 0.5F, true);
    }

    private void label(String key, float x0, float y, float x1, float s, int acc) {
        Component c = Component.translatable(key).withStyle(ChatFormatting.BOLD);
        float ts = fitSize(c, s * 0.8F, (x1 - x0) * 0.9F);
        txt(c, (x0 + x1) / 2, y, ts, lighten(acc), 0.5F, true);
    }

    // ------------------------------------------------------------------ dibujo

    private static void v(VB vb, PoseStack.Pose p, float x, float y, float z, float u, float w, int argb) {
        vb.v(p, x, y, z, (argb >> 16) & 255, (argb >> 8) & 255, argb & 255, 255, u, w, OverlayTexture.NO_OVERLAY, FULL, 0, 0, 1);
    }

    private static void fill(VB vb, PoseStack.Pose p, float x0, float y0, float x1, float y1, float z, int argb) {
        gquad(vb, p, x0, y0, x1, y1, z, argb, argb);
    }

    /** Rectángulo con degradado de abajo (lo) hacia arriba (hi). */
    private static void gquad(VB vb, PoseStack.Pose p, float x0, float y0, float x1, float y1, float z, int lo, int hi) {
        if (x1 <= x0 || y1 <= y0) {
            return;
        }
        float u0 = (WHITE[0] + 1) / ATLAS, u1 = (WHITE[0] + WHITE[2] - 1) / ATLAS;
        float v0 = (WHITE[1] + 1) / ATLAS, v1 = (WHITE[1] + WHITE[3] - 1) / ATLAS;
        v(vb, p, x0, y0, z, u0, v1, lo);
        v(vb, p, x1, y0, z, u1, v1, lo);
        v(vb, p, x1, y1, z, u1, v0, hi);
        v(vb, p, x0, y1, z, u0, v0, hi);
    }

    private static void frame(VB vb, PoseStack.Pose p, float x0, float y0, float x1, float y1, float t, float z, int c) {
        fill(vb, p, x0, y0, x1, y0 + t, z, c);
        fill(vb, p, x0, y1 - t, x1, y1, z, c);
        fill(vb, p, x0, y0, x0 + t, y1, z, c);
        fill(vb, p, x1 - t, y0, x1, y1, z, c);
    }

    /** La franja un poco más clara que sube por el fondo (se funde con el degradado en sus bordes). */
    private static void sweepBand(VB vb, PoseStack.Pose p, float x0, float x1, float y0, float y1, float sy, float sh, int lo, int hi) {
        float a = Math.max(y0, sy - sh), m = Math.min(y1, Math.max(y0, sy)), b = Math.min(y1, sy + sh);
        if (b <= y0 || a >= y1) {
            return;
        }
        float span = y1 - y0;
        int ca = mix(lo, hi, (a - y0) / span);
        int cm = mix(mix(lo, hi, (m - y0) / span), 0xFF3A3F7A, 0.28F);
        int cb = mix(lo, hi, (b - y0) / span);
        gquad(vb, p, x0, a, x1, m, Z * 3, ca, cm);
        gquad(vb, p, x0, m, x1, b, Z * 3, cm, cb);
    }

    /** Estática de tele: un pedazo de la textura de ruido que cambia cada tick, teñido con el color del canal. */
    private void noise(VB vb, PoseStack.Pose p, float x0, float y0, float x1, float y1, float z, int acc) {
        int f = (int) time;
        float ou = ((f * 37) % 32) / ATLAS;
        float ov = ((f * 53) % 24) / ATLAS;
        float u0 = NOISE[0] / ATLAS + ou, u1 = u0 + 32 / ATLAS;
        float v0 = NOISE[1] / ATLAS + ov, v1 = v0 + 24 / ATLAS;
        int tint = mix(0xFFFFFFFF, acc, 0.25F);
        v(vb, p, x0, y0, z, u0, v1, tint);
        v(vb, p, x1, y0, z, u1, v1, tint);
        v(vb, p, x1, y1, z, u1, v0, tint);
        v(vb, p, x0, y1, z, u0, v0, tint);
    }

    private void txt(Component c, float x, float y, float size, int color, float align, boolean shadow) {
        texts.add(new Txt(c, x, y, size, color, align, shadow));
    }

    /** Los textos al final (en 1.20 pedir otro pincel cierra el anterior). */
    private void flushTexts(PoseStack ps, Draw d) {
        Font font = Draw.font();
        for (Txt x : texts) {
            float s = x.size() / 9F;
            float w = font.width(x.c()) * s;
            if (x.shadow()) {
                ps.pushPose();
                ps.translate(x.x() - w * x.align() + x.size() * 0.08F, x.y() + x.size() - x.size() * 0.08F, 0.030F);
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
        texts.clear();
    }

    // ------------------------------------------------------------------ utilidades

    private static float width(String s, float size) {
        return Draw.font().width(s) * size / 9F;
    }

    /** Achica la letra si el texto no cabe a lo ancho. */
    private static float fitSize(Component c, float size, float maxW) {
        float w = Draw.font().width(c) * size / 9F;
        return w > maxW ? size * maxW / w : size;
    }

    /** Recorta un nombre largo con "..." para que quepa. */
    private static String clip(String s, float maxW, float size) {
        if (maxW <= 0) {
            return "";
        }
        Font f = Draw.font();
        int px = (int) (maxW * 9F / size);
        if (f.width(s) <= px) {
            return s;
        }
        int room = Math.max(0, px - f.width(".."));
        String cut = s;
        while (!cut.isEmpty() && f.width(cut) > room) {
            cut = cut.substring(0, cut.length() - 1);
        }
        return cut + "..";
    }

    /** 12,345 / 123.4K / 1.23M */
    static String fmt(long v) {
        long a = Math.abs(v);
        String sign = v < 0 ? "-" : "";
        if (a < 100_000) {
            return sign + String.format(Locale.ROOT, "%,d", a);
        }
        if (a < 1_000_000) {
            return sign + String.format(Locale.ROOT, "%.1fK", a / 1000.0);
        }
        if (a < 1_000_000_000) {
            return sign + String.format(Locale.ROOT, "%.2fM", a / 1_000_000.0);
        }
        return sign + String.format(Locale.ROOT, "%.2fB", a / 1_000_000_000.0);
    }

    private static float clamp(float v, float lo, float hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    private static float ease(float t) {
        float u = 1 - t;
        return 1 - u * u * u;
    }

    private static int mix(int a, int b, float t) {
        t = clamp(t, 0, 1);
        int r = (int) (((a >> 16) & 255) * (1 - t) + ((b >> 16) & 255) * t);
        int g = (int) (((a >> 8) & 255) * (1 - t) + ((b >> 8) & 255) * t);
        int bl = (int) ((a & 255) * (1 - t) + (b & 255) * t);
        return 0xFF000000 | r << 16 | g << 8 | bl;
    }

    private static int lighten(int c) {
        return mix(c, 0xFFFFFFFF, 0.35F);
    }
}
