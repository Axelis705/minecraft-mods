package com.casino.client;

import com.casino.data.Membership;
import com.casino.net.MemberSyncPacket;
import com.casino.plat.client.G;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Locale;

/**
 * La mini tarjeta de socio a la izquierda de la pantalla: aparece sola al entrar a un casino y se va al salir. Muestra tu cara (de tu
 * skin), tu nivel con sus estrellas, la barra hacia el siguiente nivel, cuánto te falta, qué te toca al subir, tus logros y si el bono
 * diario está listo. La tecla J la esconde o la vuelve a mostrar.
 */
public final class MemberHud {
    //#if MC >= 26.1
    //$$ public static final KeyMapping TOGGLE = new KeyMapping("key.casino.member_hud", com.mojang.blaze3d.platform.InputConstants.KEY_J, RaceClient.CAT);
    //#else
    public static final KeyMapping TOGGLE = new KeyMapping("key.casino.member_hud", com.mojang.blaze3d.platform.InputConstants.KEY_J, "key.categories.casino");
    //#endif

    /** Cofre de nivel que llegó con una pantalla abierta: se abre cuando la cierres. */
    public static com.casino.net.LevelChestPacket pendingChest;
    private static MemberSyncPacket data;
    private static com.casino.net.TournamentPacket tour;
    private static long tourAt;

    public static void tournament(com.casino.net.TournamentPacket m) {
        tour = m.active ? m : null;
        tourAt = System.currentTimeMillis();
    }
    private static boolean hidden;
    private static float slide;
    private static long lastFrame;
    /** Lo apostado que se muestra: sube poco a poco hasta el valor real (la barra se llena con animación). */
    private static double shown = -1;
    /** Brillo cuando cambia algo (subir de nivel, logro nuevo). */
    private static float flash;
    private static int lastLevel = -1;
    private static long lastAch;

    private MemberHud() {
    }

    public static void set(MemberSyncPacket m) {
        if (data != null) {
            int lv = Membership.level(m.wagered);
            if (lv != Membership.level(data.wagered) || m.achievements != data.achievements) {
                flash = 1;
            }
        }
        data = m;
    }

    private static String fmt(long v) {
        return String.format(Locale.ROOT, "%,d", v);
    }

    /** ¿El jugador está dentro de alguno de los casinos? */
    private static boolean inside(Minecraft mc) {
        if (data == null || mc.level == null || mc.player == null) {
            return false;
        }
        String dim = mc.level.dimension().toString();
        double x = mc.player.getX(), y = mc.player.getY(), z = mc.player.getZ();
        List<int[]> boxes = data.boxes;
        for (int i = 0; i < boxes.size(); i++) {
            int[] b = boxes.get(i);
            if (data.dims.get(i).equals(dim) && x >= b[0] && x < b[3] + 1 && y >= b[1] - 2 && y < b[4] + 2 && z >= b[2] && z < b[5] + 1) {
                return true;
            }
        }
        return false;
    }

    private static ResourceLocation skin(Minecraft mc) {
        //#if MC >= 26.1
        //$$ return mc.player.getSkin().body().texturePath();
        //#elif MC >= 1.20.2
        //$$ return mc.player.getSkin().texture();
        //#else
        return mc.player.getSkinTextureLocation();
        //#endif
    }

    private static int mix(int a, int b, float t) {
        int r = (int) (((a >> 16) & 255) * (1 - t) + ((b >> 16) & 255) * t);
        int g = (int) (((a >> 8) & 255) * (1 - t) + ((b >> 8) & 255) * t);
        int bl = (int) ((a & 255) * (1 - t) + (b & 255) * t);
        return r << 16 | g << 8 | bl;
    }

    public static void render(G g, int sw, int sh) {
        Minecraft mc = Minecraft.getInstance();
        while (TOGGLE.consumeClick()) {
            hidden = !hidden;
        }
        if (pendingChest != null && ClientScreens.none() && mc.player != null) {
            com.casino.net.LevelChestPacket m = pendingChest;
            pendingChest = null;
            ClientHooks.open(new LevelChestScreen(m));
            return;
        }
        long ms = System.currentTimeMillis();
        float dt = lastFrame == 0 ? 0 : Math.min(0.1F, (ms - lastFrame) / 1000F);
        lastFrame = ms;
        //#if MC >= 26.1
        //$$ boolean show = !hidden && ClientScreens.none() && inside(mc);
        //#else
        boolean show = !hidden && !mc.options.hideGui && ClientScreens.none() && inside(mc);
        //#endif
        slide = Math.max(0, Math.min(1, slide + (show ? dt * 4 : -dt * 4)));
        flash = Math.max(0, flash - dt * 0.8F);
        if (slide <= 0 || data == null || mc.player == null) {
            return;
        }
        Font font = mc.font;
        long wag = data.wagered;
        shown = shown < 0 ? wag : shown + (wag - shown) * Math.min(1, dt * 3);
        if (Math.abs(wag - shown) < 1) {
            shown = wag;
        }
        int lvl = Membership.level((long) shown);
        int col = Membership.COLOR[lvl];
        float time = (ms % 100000) / 1000F;

        int w = 150;
        int h = 86;
        float ease = 1 - (1 - slide) * (1 - slide) * (1 - slide);
        int x = 4 - (int) ((w + 8) * (1 - ease));
        int y = sh / 2 - h / 2;

        // ---- fondo: marco del color del nivel, cuerpo oscuro con un tinte del nivel
        g.fill(x + 2, y + 3, x + w + 2, y + h + 3, 0x60000000);
        int frame = 0xFF000000 | mix(col, 0xFFFFFF, flash * 0.6F);
        g.fill(x - 1, y - 1, x + w + 1, y + h + 1, frame);
        g.fillGradient(x, y, x + w, y + h, 0xE8000000 | mix(col, 0x0A0812, 0.78F), 0xE8000000 | mix(col, 0x05040A, 0.9F));
        // brillo que cruza de vez en cuando
        float t = (time % 6F) / 1.2F;
        if (t < 1) {
            int sx = x - 30 + (int) (t * (w + 60));
            for (int i = 0; i < 18; i++) {
                int px = sx + i;
                if (px >= x && px < x + w) {
                    int a = (int) (34 * Math.sin(Math.PI * i / 18));
                    g.fill(px, y, px + 1, y + h, a << 24 | 0xFFFFFF);
                }
            }
        }

        // ---- cara del jugador (de su skin), con el sombrero encima
        int fx = x + 6, fy = y + 6, fs = 26;
        g.fill(fx - 1, fy - 1, fx + fs + 1, fy + fs + 1, 0xFF000000 | col);
        ResourceLocation tex = skin(mc);
        g.blit(tex, fx, fy, fs, fs, 8, 8, 8, 8, 64, 64);
        g.blit(tex, fx, fy, fs, fs, 40, 8, 8, 8, 64, 64);

        // ---- nivel, estrellas y nombre
        int tx = fx + fs + 6;
        String stars = "★".repeat(lvl + 1);
        g.drawString(font, stars, tx, y + 6, 0xFFFFE680, true);
        Component lv = Component.translatable("casino.member.level_" + Membership.LEVELS[lvl]).withStyle(ChatFormatting.BOLD);
        g.drawString(font, lv, tx, y + 16, 0xFF000000 | mix(col, 0xFFFFFF, 0.25F), true);
        String name = font.plainSubstrByWidth(mc.player.getName().getString(), x + w - 6 - tx);
        g.drawString(font, name, tx, y + 26, 0xFFD8DCEA, false);

        // ---- barra al siguiente nivel
        int by = y + 38;
        int bx = x + 6, bw = w - 12;
        if (!data.member) {
            List<net.minecraft.util.FormattedCharSequence> lines = font.split(Component.translatable("casino.hud.join"), bw);
            for (int i = 0; i < Math.min(3, lines.size()); i++) {
                g.drawString(font, lines.get(i), bx, by + i * 10, 0xFFE8E0C0, false);
            }
        } else if (lvl < Membership.AT.length - 1) {
            long a = Membership.AT[lvl], b = Membership.AT[lvl + 1];
            float f = (float) Math.max(0, Math.min(1.0, (shown - a) / (double) (b - a)));
            int next = Membership.COLOR[lvl + 1];
            g.fill(bx, by, bx + bw, by + 7, 0xFF000000);
            g.fill(bx + 1, by + 1, bx + bw - 1, by + 6, 0xFF1C1828);
            int fw = (int) ((bw - 2) * f);
            if (fw > 0) {
                g.fillGradient(bx + 1, by + 1, bx + 1 + fw, by + 6, 0xFF000000 | mix(next, 0xFFFFFF, 0.35F), 0xFF000000 | mix(next, 0x000000, 0.25F));
                // destello que recorre la parte llena
                int sp = (int) ((time * 40) % (fw + 20)) - 10;
                for (int i = 0; i < 8; i++) {
                    int px = bx + 1 + sp + i;
                    if (px > bx && px < bx + 1 + fw) {
                        g.fill(px, by + 1, px + 1, by + 6, ((int) (90 * Math.sin(Math.PI * i / 8))) << 24 | 0xFFFFFF);
                    }
                }
            }
            String pct = (int) (f * 100) + "%";
            g.drawString(font, pct, bx + bw - font.width(pct), by - 9, 0xFFB8BCD0, false);
            Component nextName = Component.translatable("casino.member.level_" + Membership.LEVELS[lvl + 1]).withStyle(s -> s.withColor(next));
            long left = Math.max(0, b - (long) shown);
            g.drawString(font, Component.translatable("casino.hud.left", fmt(left), nextName), bx, by + 10, 0xFFE8E4F0, false);
            // qué te toca al subir
            Component reward = Component.translatable("casino.hud.reward", fmt(Membership.LEVEL_REWARD[lvl + 1]));
            g.drawString(font, font.plainSubstrByWidth(reward.getString(), bw), bx, by + 21, 0xFFFFD45A, false);
        } else {
            g.drawString(font, Component.translatable("casino.member.max").withStyle(ChatFormatting.AQUA), bx, by + 2, 0xFFFFFFFF, true);
            g.drawString(font, Component.translatable("casino.hud.total", fmt((long) shown)), bx, by + 14, 0xFFE8E4F0, false);
        }

        // ---- logros y bono
        int ly = y + h - 13;
        g.fill(x + 4, ly - 3, x + w - 4, ly - 2, 0x40FFFFFF);
        int got = Long.bitCount(data.achievements & ((1L << Membership.COUNT) - 1));
        g.drawString(font, Component.translatable("casino.hud.ach", got, Membership.COUNT), bx, ly, 0xFFC8C0DC, false);
        if (data.member && data.bonusReady) {
            boolean on = (ms / 500) % 2 == 0;
            Component bonus = Component.translatable("casino.hud.bonus");
            g.drawString(font, bonus, x + w - 6 - font.width(bonus), ly, on ? 0xFF7CFF9A : 0xFF3FA060, true);
        }
        // tecla para ocultar, muy discreta, debajo
        Component key = Component.translatable("casino.hud.hide", TOGGLE.getTranslatedKeyMessage());
        g.drawString(font, key, x + 2, y + h + 4, 0x90A0A0B0, false);
        if (tour != null) {
            drawTournament(g, font, x, y + h + 16, w, ms);
        }
    }

    /** El torneo de esta noche, debajo de la tarjeta. */
    private static void drawTournament(G g, Font font, int x, int y, int w, long ms) {
        com.casino.net.TournamentPacket t = tour;
        int h = 62;
        boolean pulse = (ms / 600) % 2 == 0;
        g.fill(x - 1, y - 1, x + w + 1, y + h + 1, pulse ? 0xFFFFD45A : 0xFFC9A227);
        g.fillGradient(x, y, x + w, y + h, 0xE8201206, 0xE8100A04);
        Component title = Component.translatable("casino.tournament.hud_title", Component.translatable("casino.records.game_" + t.game))
                .withStyle(ChatFormatting.BOLD);
        g.drawString(font, font.plainSubstrByWidth(title.getString(), w - 10), x + 5, y + 4, 0xFFFFD45A, true);
        int left = Math.max(0, t.ticksLeft - (int) ((ms - tourAt) / 50));
        String tm = String.format(Locale.ROOT, "%d:%02d", left / 20 / 60, left / 20 % 60);
        g.drawString(font, Component.translatable("casino.tournament.hud_left", tm), x + 5, y + 15, 0xFFE8E4F0, false);
        int[] medal = {0xFFF6CB3A, 0xFFC9D1DB, 0xFFD08A4E};
        for (int i = 0; i < 3; i++) {
            if (t.top[i] == null || t.top[i].isEmpty()) {
                continue;
            }
            String n = font.plainSubstrByWidth((i + 1) + ". " + t.top[i], w - 60);
            g.drawString(font, n, x + 5, y + 26 + i * 9, medal[i], false);
            String sc = (t.topScore[i] >= 0 ? "+" : "") + fmt(t.topScore[i]);
            g.drawString(font, sc, x + w - 5 - font.width(sc), y + 26 + i * 9, t.topScore[i] >= 0 ? 0xFF7CFF9A : 0xFFFF7070, false);
        }
        Component me = t.rank >= 0 ? Component.translatable("casino.tournament.hud_me", t.rank + 1, (t.mine >= 0 ? "+" : "") + fmt(t.mine))
                : Component.translatable("casino.tournament.hud_join");
        g.drawString(font, font.plainSubstrByWidth(me.getString(), w - 10), x + 5, y + 53, 0xFF80E0FF, false);
    }
}
