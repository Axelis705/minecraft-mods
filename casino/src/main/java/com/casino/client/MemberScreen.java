package com.casino.client;

import com.casino.chip.Chips;
import com.casino.data.CasinoStats;
import com.casino.data.Membership;
import com.casino.net.CasinoNet;
import com.casino.net.MemberPacket;
import com.casino.plat.client.CasinoScreen;
import com.casino.plat.client.G;
import com.casino.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Tarjeta de socio: la tarjeta con el color de tu nivel (Bronce a Diamante) y tu nombre, la barra hacia el siguiente nivel, tus
 * números, el bono diario y los 20 logros (con su premio y, si cuentan algo, cuánto llevas).
 */
public final class MemberScreen extends CasinoScreen {
    private static final int W = 330;
    private static final int H = 236;
    private final MemberPacket.Open d;
    private int left;
    private int top;
    private int age;
    private ItemStack[] icons;

    public MemberScreen(MemberPacket.Open d) {
        super(Component.translatable("item.casino.member_card"));
        this.d = d;
    }

    private static ItemStack[] icons() {
        Item[] it = {
                Chips.itemFor(1).get(), Chips.itemFor(500).get(), Items.GOLD_INGOT, Items.DIAMOND_BLOCK, ModItems.CASINO_GUIDE.get(), Items.CLOCK,
                Items.NETHERITE_INGOT, ModItems.BLACKJACK.get(), ModItems.POKER.get(), Items.GOLDEN_HELMET, ModItems.ROULETTE.get(), ModItems.SLOT.get(),
                ModItems.PLINKO.get(), Items.SADDLE, ModItems.SCRATCH_CARD.get(), ModItems.LUCKY_WHEEL.get(), Items.HONEY_BOTTLE, ModItems.VIP_GOLD.get(),
                Items.DIAMOND, Items.EMERALD_BLOCK};
        ItemStack[] out = new ItemStack[it.length];
        for (int i = 0; i < it.length; i++) {
            out[i] = new ItemStack(it[i]);
        }
        return out;
    }

    @Override
    protected void init() {
        left = (width - W) / 2;
        top = Math.max(4, (height - H) / 2);
        if (icons == null) {
            icons = icons();
        }
        int lvl = d.level();
        Component bonus = d.bonus ? Component.translatable("casino.member.bonus", Membership.DAILY[lvl]).withStyle(ChatFormatting.GOLD)
                : Component.translatable("casino.member.bonus_wait").withStyle(ChatFormatting.GRAY);
        Button b = Button.builder(bonus, x -> CasinoNet.sendToServer(new MemberPacket.Claim())).bounds(left + 10, top + H - 30, 150, 20).build();
        b.active = d.bonus;
        addRenderableWidget(b);
        addRenderableWidget(Button.builder(Component.translatable("casino.cashier.close"), x -> onClose()).bounds(left + W - 70, top + H - 30, 60, 20).build());
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void tick() {
        age++;
    }

    private static String fmt(long v) {
        return String.format(Locale.ROOT, "%,d", v);
    }

    private static int mix(int a, int b, float t) {
        int r = (int) (((a >> 16) & 255) * (1 - t) + ((b >> 16) & 255) * t);
        int g = (int) (((a >> 8) & 255) * (1 - t) + ((b >> 8) & 255) * t);
        int bl = (int) ((a & 255) * (1 - t) + (b & 255) * t);
        return r << 16 | g << 8 | bl;
    }

    @Override
    protected void draw(G g, int mx, int my, float pt) {
        drawBackground(g, mx, my, pt);
        int x0 = left, y0 = top, x1 = left + W, y1 = top + H;
        g.fill(x0 + 3, y0 + 4, x1 + 3, y1 + 4, 0x80000000);
        g.fill(x0 - 2, y0 - 2, x1 + 2, y1 + 2, 0xFFC9A227);
        g.fill(x0 - 1, y0 - 1, x1 + 1, y1 + 1, 0xFF2A1A05);
        g.fillGradient(x0, y0, x1, y1, 0xF4181026, 0xF40C0814);
        int lvl = d.level();
        int col = Membership.COLOR[lvl];

        // ---- la tarjeta
        int cx = x0 + 10, cy = y0 + 10, cw = 150, ch = 90;
        g.fill(cx - 1, cy - 1, cx + cw + 1, cy + ch + 1, 0xFF000000 | mix(col, 0x000000, 0.6F));
        g.fillGradient(cx, cy, cx + cw, cy + ch, 0xFF000000 | mix(col, 0xFFFFFF, 0.15F), 0xFF000000 | mix(col, 0x000000, 0.45F));
        // brillo que cruza la tarjeta
        float t = ((age + pt) % 80) / 80F;
        int sx = cx - 40 + (int) (t * (cw + 80));
        for (int i = 0; i < 14; i++) {
            int x = sx + i;
            if (x >= cx && x < cx + cw) {
                int a = (int) (60 * Math.sin(Math.PI * i / 14));
                g.fill(x, cy, x + 1, cy + ch, a << 24 | 0xFFFFFF);
            }
        }
        g.fill(cx + 8, cy + 30, cx + 24, cy + 42, 0xFFD9B44A);   // chip de la tarjeta
        g.fill(cx + 9, cy + 31, cx + 23, cy + 41, 0xFFB8902E);
        g.drawString(font, Component.literal("THE DIAMOND").withStyle(ChatFormatting.BOLD), cx + 8, cy + 8, 0xFFFFFFFF);
        g.drawString(font, Component.translatable("casino.member.card_title"), cx + 8, cy + 18, 0xFFEFE6C8);
        String stars = "★".repeat(lvl + 1);
        g.drawString(font, stars, cx + cw - 8 - font.width(stars), cy + 8, 0xFFFFF2A0);
        Component lv = Component.translatable("casino.member.level_" + Membership.LEVELS[lvl]).withStyle(ChatFormatting.BOLD);
        g.push();
        g.translate(cx + 30, cy + 32, 0);
        g.scale(1.5F, 1.5F, 1);
        g.drawString(font, lv, 0, 0, 0xFFFFFFFF);
        g.pop();
        g.drawString(font, d.name, cx + 8, cy + ch - 14, 0xFFFFFFFF);

        // ---- barra al siguiente nivel
        int by = cy + ch + 8;
        if (lvl < Membership.AT.length - 1) {
            long a = Membership.AT[lvl], b = Membership.AT[lvl + 1];
            float f = (float) Math.min(1.0, (d.wagered - a) / (double) (b - a));
            g.drawString(font, Component.translatable("casino.member.next", Component.translatable("casino.member.level_" + Membership.LEVELS[lvl + 1])),
                    cx, by, 0xFFD8DCF0);
            g.fill(cx, by + 11, cx + cw, by + 19, 0xFF000000);
            g.fill(cx + 1, by + 12, cx + 1 + (int) ((cw - 2) * f), by + 18, 0xFF000000 | Membership.COLOR[lvl + 1]);
            String txt = fmt(d.wagered) + " / " + fmt(b);
            g.drawString(font, txt, cx + cw / 2 - font.width(txt) / 2, by + 22, 0xFFB0B4C8);
        } else {
            g.drawString(font, Component.translatable("casino.member.max").withStyle(ChatFormatting.AQUA), cx, by + 6, 0xFFFFFFFF);
        }
        // ---- números
        int sy = by + 36;
        String[][] rows = {{"casino.records.bets", fmt(d.bets)}, {"casino.member.won", fmt(d.won)}, {"casino.records.ch_4", fmt(d.biggest)},
                {"casino.member.net", (d.won - d.wagered >= 0 ? "+" : "") + fmt(d.won - d.wagered)}};
        for (int i = 0; i < rows.length; i++) {
            Component k = Component.translatable(rows[i][0]);
            String kv = k.getString();
            if (kv.length() > 18) {
                kv = kv.substring(0, 18);
            }
            g.drawString(font, kv, cx, sy + i * 10, 0xFF9EA4C0);
            int vc = i == 3 ? (d.won >= d.wagered ? 0xFF6CFF8C : 0xFFFF7070) : 0xFFFFFFFF;
            g.drawString(font, rows[i][1], cx + cw - font.width(rows[i][1]), sy + i * 10, vc);
        }

        // ---- logros
        int ax = x0 + 172, ay = y0 + 10;
        int got = Long.bitCount(d.achievements & ((1L << Membership.COUNT) - 1));
        g.drawString(font, Component.translatable("casino.member.achievements", got, Membership.COUNT).withStyle(ChatFormatting.BOLD), ax, ay, 0xFFFFE680);
        int slot = 29;
        List<Component> tip = null;
        for (int i = 0; i < Membership.COUNT; i++) {
            int gx = ax + (i % 5) * slot, gy = ay + 14 + (i / 5) * slot;
            boolean has = (d.achievements & (1L << i)) != 0;
            boolean hover = mx >= gx && mx < gx + 26 && my >= gy && my < gy + 26;
            g.fill(gx, gy, gx + 26, gy + 26, has ? 0xFFC9A227 : 0xFF3A3450);
            g.fill(gx + 1, gy + 1, gx + 25, gy + 25, has ? 0xFF3B2A08 : 0xFF15121F);
            if (hover) {
                g.fill(gx + 1, gy + 1, gx + 25, gy + 25, 0x40FFFFFF);
            }
            if (has) {
                g.renderItem(icons[i], gx + 5, gy + 5);
            } else {
                // bloqueado: candado y signo (el objeto se ve al pasar el ratón)
                if (hover) {
                    g.renderItem(icons[i], gx + 5, gy + 5);
                } else {
                    g.drawString(font, "?", gx + 11, gy + 9, 0xFF6A6480);
                }
            }
            if (hover) {
                tip = new ArrayList<>();
                tip.add(Component.translatable("casino.ach." + i).withStyle(has ? ChatFormatting.GOLD : ChatFormatting.WHITE));
                tip.add(Component.translatable("casino.ach." + i + ".desc").withStyle(ChatFormatting.GRAY));
                String prog = progress(i);
                if (prog != null && !has) {
                    tip.add(Component.literal(prog).withStyle(ChatFormatting.AQUA));
                }
                tip.add(Component.translatable("casino.member.reward", fmt(Membership.REWARD[i])).withStyle(ChatFormatting.YELLOW));
                tip.add(Component.translatable(has ? "casino.member.done" : "casino.member.locked")
                        .withStyle(has ? ChatFormatting.GREEN : ChatFormatting.DARK_GRAY));
            }
        }
        // bono por nivel (una línea abajo de los logros)
        g.drawString(font, Component.translatable("casino.member.daily_info", Membership.DAILY[lvl]), ax, ay + 14 + 4 * slot + 4, 0xFF9EA4C0);
        drawWidgets(g, mx, my, pt);
        if (tip != null) {
            g.renderTooltip(font, tip, mx, my);
        }
    }

    /** Cuánto lleva en los logros que cuentan algo. */
    private String progress(int i) {
        return switch (i) {
            case Membership.ALL_GAMES -> Integer.bitCount(d.gamesMask) + " / " + CasinoStats.GAMES;
            case Membership.REGULAR -> Math.min(100, d.bets) + " / 100";
            case Membership.VETERAN -> Math.min(1000, d.bets) + " / 1000";
            case Membership.WHEEL_FAN -> Math.min(Membership.WHEEL_NEEDED, d.wheelSpins) + " / " + Membership.WHEEL_NEEDED;
            case Membership.COCKTAILS -> Math.min(Membership.DRINKS_NEEDED, d.drinks) + " / " + Membership.DRINKS_NEEDED;
            case Membership.DIAMOND -> fmt(Math.min(d.wagered, Membership.AT[4])) + " / " + fmt(Membership.AT[4]);
            case Membership.NET_10K -> fmt(Math.max(0, d.won - d.wagered)) + " / 10,000";
            default -> null;
        };
    }
}
