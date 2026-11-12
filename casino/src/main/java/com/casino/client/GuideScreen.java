package com.casino.client;

import com.casino.plat.client.CasinoScreen;
import com.casino.plat.client.G;
import com.casino.registry.ModDecor;
import com.casino.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/** La Guía del Casino: un libro abierto de dos páginas. Los textos están en los archivos de idioma (casino.guide.pN.*). */
public final class GuideScreen extends CasinoScreen {
    public static final int PAGES = 15;
    private static final int PW = 150;
    private static final int PH = 196;
    private static final int TEXT = 0xFF3A2614;
    private static int remembered = 0;

    private int spread;
    private int left;
    private int top;

    public GuideScreen() {
        super(Component.translatable("item.casino.casino_guide"));
        spread = remembered;
    }

    private static List<Supplier<Item>> icons() {
        List<Supplier<Item>> l = new ArrayList<>();
        l.add(() -> ModItems.CHIP_500.get());
        l.add(() -> ModItems.CHIP_100.get());
        l.add(() -> ModItems.CASHIER.get());
        l.add(() -> ModItems.LUCKY_WHEEL.get());
        l.add(() -> ModItems.SLOT.get());
        l.add(() -> ModItems.PLINKO.get());
        l.add(() -> ModItems.BLACKJACK.get());
        l.add(() -> ModItems.ROULETTE.get());
        l.add(() -> ModItems.POKER.get());
        l.add(() -> ModItems.SCRATCH_CARD.get());
        l.add(() -> ModDecor.MARBLE.get().asItem());
        l.add(() -> ModDecor.CHAIRS.get(0).get().asItem());
        l.add(() -> ModDecor.NEON_LETTER.get().asItem());
        l.add(() -> ModDecor.SPEAKER.get().asItem());
        l.add(() -> ModItems.CHIP_1.get());
        return l;
    }

    @Override
    protected void init() {
        left = (width - PW * 2 - 8) / 2;
        top = Math.max(4, (height - PH - 26) / 2);
        addRenderableWidget(Button.builder(Component.literal("◀ ").append(Component.translatable("casino.guide.prev")), b -> turn(-1))
                .bounds(left, top + PH + 4, 90, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("casino.guide.next").append(" ▶"), b -> turn(1))
                .bounds(left + PW * 2 + 8 - 90, top + PH + 4, 90, 20).build());
    }

    private void turn(int d) {
        int max = (PAGES - 1) / 2;
        spread = Math.max(0, Math.min(max, spread + d));
        remembered = spread;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    protected boolean onKey(int key, int scan, int mods) {
        if (key == 262) {
            turn(1);
            return true;
        }
        if (key == 263) {
            turn(-1);
            return true;
        }
        return super.onKey(key, scan, mods);
    }

    @Override
    protected boolean onClick(double mx, double my, int button) {
        // índice: página 1
        int page = spread * 2 == 0 ? 1 : -1;
        if (page == 1) {
            int x = left + PW + 8 + 12;
            int y = top + 34;
            for (int i = 2; i < PAGES; i++) {
                if (mx >= x && mx < x + PW - 24 && my >= y && my < y + 11) {
                    spread = i / 2;
                    remembered = spread;
                    return true;
                }
                y += 11;
            }
        }
        return super.onClick(mx, my, button);
    }

    @Override
    protected void draw(G g, int mx, int my, float pt) {
        drawBackground(g, mx, my, pt);
        // tapas de cuero y hojas
        g.fill(left - 6, top - 6, left + PW * 2 + 14, top + PH + 6, 0xFF5A0E16);
        g.fill(left - 4, top - 4, left + PW * 2 + 12, top + PH + 4, 0xFFC9A04A);
        g.fill(left - 3, top - 3, left + PW * 2 + 11, top + PH + 3, 0xFF7A1420);
        for (int s = 0; s < 2; s++) {
            int x = left + s * (PW + 8);
            g.fillGradient(x, top, x + PW, top + PH, 0xFFF4E9CF, 0xFFE8D6AE);
        }
        g.fill(left + PW, top, left + PW + 8, top + PH, 0xFF8A6A3A);
        g.fill(left + PW + 3, top, left + PW + 5, top + PH, 0xFF5E4424);
        int p = spread * 2;
        page(g, p, left, mx, my);
        if (p + 1 < PAGES) {
            page(g, p + 1, left + PW + 8, mx, my);
        }
        drawWidgets(g, mx, my, pt);
    }

    private void page(G g, int i, int x, int mx, int my) {
        List<Supplier<Item>> ic = icons();
        g.renderItem(new ItemStack(ic.get(i).get()), x + PW - 22, top + 6);
        Component title = Component.translatable("casino.guide.p" + i + ".title").withStyle(ChatFormatting.BOLD);
        if (i == 0) {
            g.drawCenteredString(font, Component.translatable("casino.guide.p0.title").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD),
                    x + PW / 2, top + 40, 0xFFFFFF);
            for (int k = 0; k < 5; k++) {
                Item[] chips = {ModItems.CHIP_1.get(), ModItems.CHIP_5.get(), ModItems.CHIP_25.get(), ModItems.CHIP_100.get(), ModItems.CHIP_500.get()};
                g.renderItem(new ItemStack(chips[k]), x + PW / 2 - 48 + k * 20, top + 56);
            }
            text(g, Component.translatable("casino.guide.p0.body").getString(), x + 10, top + 84, PW - 20, PH - 92);
        } else {
            g.drawString(font, title, x + 10, top + 10, 0xFF6A1018, false);
            g.fill(x + 10, top + 21, x + PW - 26, top + 22, 0xFFB08A40);
            if (i == 1) {
                int y = top + 34;
                for (int k = 2; k < PAGES; k++) {
                    boolean hover = mx >= x + 12 && mx < x + PW - 12 && my >= y && my < y + 11;
                    g.drawString(font, Component.literal((k + 1) + ". ").append(Component.translatable("casino.guide.p" + k + ".title")),
                            x + 12, y, hover ? 0xFFB01020 : TEXT, false);
                    y += 11;
                }
            } else {
                text(g, Component.translatable("casino.guide.p" + i + ".body").getString(), x + 10, top + 28, PW - 20, PH - 46);
            }
        }
        g.drawCenteredString(font, Component.translatable("casino.guide.page", i + 1, PAGES).withStyle(ChatFormatting.GRAY), x + PW / 2, top + PH - 12, 0xFFFFFF);
    }

    /** Escribe párrafos ajustados al ancho; si no caben, achica la letra. */
    private void text(G g, String body, int x, int y, int w, int h) {
        float scale = 1.0F;
        List<FormattedCharSequence> lines = lines(body, w);
        while (lines.size() * 10 * scale > h && scale > 0.6F) {
            scale -= 0.05F;
            lines = lines(body, (int) (w / scale));
        }
        g.push();
        g.translate(x, y, 0);
        g.scale(scale, scale, 1);
        int ly = 0;
        for (FormattedCharSequence l : lines) {
            if (l != null) {
                g.drawString(font, l, 0, ly, TEXT, false);
                ly += 10;
            } else {
                ly += 4;
            }
        }
        g.pop();
    }

    /** null = espacio entre párrafos. */
    private List<FormattedCharSequence> lines(String body, int w) {
        List<FormattedCharSequence> out = new ArrayList<>();
        String[] paras = body.split("\n");
        for (int k = 0; k < paras.length; k++) {
            out.addAll(font.split(Component.literal(paras[k]), w));
            if (k < paras.length - 1) {
                out.add(null);
            }
        }
        return out;
    }
}
