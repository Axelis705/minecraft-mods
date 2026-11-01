package com.casino.client;

import com.casino.game.ScratchCards;
import com.casino.item.ScratchCardItem;
import com.casino.net.CasinoNet;
import com.casino.net.ScratchCardPacket;
import com.casino.registry.ModSounds;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

/**
 * El boleto abierto: 6 casillas tapadas con pintura plateada que se rascan con el ratón (mantén el clic y arrastra). Al descubrir
 * más de la mitad de una casilla se destapa entera y se avisa al servidor; con las 6 descubiertas el servidor paga el premio.
 */
public final class ScratchScreen extends Screen {
    private static final int W = 270;
    private static final int H = 252;
    private static final int CELL = 60;
    private static final int SUB = 4;
    private static final int N = CELL / SUB;
    private static final int GAP = 8;
    private static final double REVEAL_AT = 0.55;
    private static final int BRUSH = 8;

    private final boolean offHand;
    private final int[] symbols;
    private final boolean[][][] rev = new boolean[ScratchCards.SPOTS][N][N];
    private final int[] revCount = new int[ScratchCards.SPOTS];
    private int done;
    private int left;
    private int top;
    private double lastX = Double.NaN;
    private double lastY = Double.NaN;
    private int soundCooldown;
    private int age;

    public ScratchScreen(boolean offHand) {
        super(Component.translatable("item.casino.scratch_card"));
        this.offHand = offHand;
        var p = Minecraft.getInstance().player;
        ItemStack st = p == null ? ItemStack.EMPTY : p.getItemInHand(offHand ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
        this.symbols = ScratchCardItem.symbols(st);
        this.done = ScratchCardItem.claimed(st) ? ScratchCardItem.ALL : ScratchCardItem.mask(st);
    }

    @Override
    protected void init() {
        left = (width - W) / 2;
        top = Math.max(4, (height - H) / 2);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void tick() {
        age++;
        if (soundCooldown > 0) {
            soundCooldown--;
        }
        var p = Minecraft.getInstance().player;
        if (p == null || !(p.getItemInHand(offHand ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND).getItem() instanceof ScratchCardItem)) {
            onClose(); // ya no tiene el boleto en la mano
        }
    }

    private int cellX(int i) {
        return left + (W - (3 * CELL + 2 * GAP)) / 2 + (i % 3) * (CELL + GAP);
    }

    private int cellY(int i) {
        return top + 42 + (i / 3) * (CELL + GAP);
    }

    private boolean isDone(int i) {
        return (done & (1 << i)) != 0;
    }

    private boolean allDone() {
        return done == ScratchCardItem.ALL;
    }

    private void scratchAt(double mx, double my) {
        boolean any = false;
        for (int i = 0; i < ScratchCards.SPOTS; i++) {
            if (isDone(i)) {
                continue;
            }
            double cx0 = cellX(i);
            double cy0 = cellY(i);
            if (mx < cx0 - BRUSH || mx > cx0 + CELL + BRUSH || my < cy0 - BRUSH || my > cy0 + CELL + BRUSH) {
                continue;
            }
            for (int a = 0; a < N; a++) {
                for (int b = 0; b < N; b++) {
                    if (rev[i][a][b]) {
                        continue;
                    }
                    double sx = cx0 + a * SUB + SUB / 2.0;
                    double sy = cy0 + b * SUB + SUB / 2.0;
                    if ((sx - mx) * (sx - mx) + (sy - my) * (sy - my) <= BRUSH * BRUSH) {
                        rev[i][a][b] = true;
                        revCount[i]++;
                        any = true;
                    }
                }
            }
            if (revCount[i] >= N * N * REVEAL_AT) {
                done |= 1 << i;
                CasinoNet.sendToServer(new ScratchCardPacket(offHand, i));
            }
        }
        if (any && soundCooldown == 0) {
            soundCooldown = 2;
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(ModSounds.SCRATCH.get(), 0.9F + (age % 5) * 0.05F, 0.6F));
        }
    }

    private void scratchLine(double x0, double y0, double x1, double y1) {
        if (Double.isNaN(x0)) {
            scratchAt(x1, y1);
            return;
        }
        double d = Math.hypot(x1 - x0, y1 - y0);
        int steps = Math.max(1, (int) Math.ceil(d / 3.0));
        for (int s = 1; s <= steps; s++) {
            double t = s / (double) steps;
            scratchAt(Mth.lerp(t, x0, x1), Mth.lerp(t, y0, y1));
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0) {
            lastX = mx;
            lastY = my;
            scratchAt(mx, my);
            return true;
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (button == 0) {
            scratchLine(lastX, lastY, mx, my);
            lastX = mx;
            lastY = my;
            return true;
        }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        lastX = Double.NaN;
        lastY = Double.NaN;
        return super.mouseReleased(mx, my, button);
    }

    private void frame(GuiGraphics g, int x0, int y0, int x1, int y1, int color) {
        g.fill(x0, y0, x1, y0 + 1, color);
        g.fill(x0, y1 - 1, x1, y1, color);
        g.fill(x0, y0, x0 + 1, y1, color);
        g.fill(x1 - 1, y0, x1, y1, color);
    }

    private void drawSymbol(GuiGraphics g, int symbol, int cx, int cy, float scale) {
        PoseStack ps = g.pose();
        ps.pushPose();
        ps.translate(cx, cy, 0);
        ps.scale(scale, scale, 1.0F);
        g.renderItem(ScratchSymbols.stack(symbol), -8, -8);
        ps.popPose();
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        renderBackground(g);
        // el boleto: marco dorado, fondo rojo oscuro con franja
        g.fill(left - 3, top - 3, left + W + 3, top + H + 3, 0xFFD9AE38);
        g.fill(left, top, left + W, top + H, 0xFF6E0F1C);
        g.fill(left, top, left + W, top + 18, 0xFF3A0810);
        g.fill(left, top + 18, left + W, top + 20, 0xFFD9AE38);
        Component title = Component.translatable("casino.scratch.title").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD);
        g.drawString(font, title, left + (W - font.width(title)) / 2, top + 5, 0xFFFFFF, true);
        boolean win = allDone() && ScratchCards.prizeOf(symbols) > 0;
        int w = ScratchCards.winningSymbol(symbols);
        Component hint = allDone()
                ? (win ? Component.translatable("casino.scratch.won_text", ScratchCards.prizeOf(symbols)).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)
                        : Component.translatable("casino.scratch.lost_text").withStyle(ChatFormatting.GRAY))
                : Component.translatable("casino.scratch.hint").withStyle(ChatFormatting.YELLOW);
        g.drawString(font, hint, left + (W - font.width(hint)) / 2, top + 26, 0xFFFFFF, true);

        for (int i = 0; i < ScratchCards.SPOTS; i++) {
            int x = cellX(i);
            int y = cellY(i);
            boolean winCell = allDone() && w >= 0 && symbols[i] == w;
            g.fill(x - 2, y - 2, x + CELL + 2, y + CELL + 2, 0xFF2A0508);
            g.fill(x, y, x + CELL, y + CELL, winCell ? 0xFFFFF0B0 : 0xFFF3E7C8);
            // símbolo (queda debajo de la pintura)
            drawSymbol(g, symbols[i], x + CELL / 2, y + CELL / 2, 2.6F);
            if (!isDone(i)) {
                // los objetos se dibujan con profundidad (z ~150): la pintura va más adelante o se vería el símbolo a través
                g.pose().pushPose();
                g.pose().translate(0, 0, 300);
                for (int a = 0; a < N; a++) {
                    for (int b = 0; b < N; b++) {
                        if (rev[i][a][b]) {
                            continue;
                        }
                        int n = (a * 7 + b * 13 + i * 5) % 5;
                        int c = switch (n) {
                            case 0 -> 0xFFC9CDD6;
                            case 1 -> 0xFFB5BAC4;
                            case 2 -> 0xFFD6D9E0;
                            case 3 -> 0xFFA9AEB8;
                            default -> 0xFFBFC4CE;
                        };
                        if ((a + b) % 6 == 0) {
                            c = 0xFFE4E7EE; // destellos
                        }
                        g.fill(x + a * SUB, y + b * SUB, x + a * SUB + SUB, y + b * SUB + SUB, c);
                    }
                }
                g.pose().popPose();
            }
            if (winCell) {
                float pulse = 0.5F + 0.5F * Mth.sin((age + pt) * 0.3F);
                int col = 0xFF000000 | ((int) Mth.lerp(pulse, 200, 255) << 16) | ((int) Mth.lerp(pulse, 150, 230) << 8) | 40;
                frame(g, x - 2, y - 2, x + CELL + 2, y + CELL + 2, col);
                frame(g, x - 1, y - 1, x + CELL + 1, y + CELL + 1, col);
            } else {
                frame(g, x - 2, y - 2, x + CELL + 2, y + CELL + 2, 0xFFD9AE38);
            }
        }

        // tabla de premios
        int ty = top + 42 + 2 * (CELL + GAP) + 6;
        g.drawString(font, Component.translatable("casino.scratch.table").withStyle(ChatFormatting.GRAY), left + 12, ty, 0xFFFFFF, false);
        for (int s = 0; s < ScratchCards.SYMBOLS; s++) {
            int cx = left + 12 + (s % 4) * 63;
            int cy = ty + 12 + (s / 4) * 20;
            boolean mine = allDone() && s == w;
            if (mine) {
                g.fill(cx - 2, cy - 2, cx + 60, cy + 17, 0x66FFD23C);
            }
            g.renderItem(ScratchSymbols.stack(s), cx, cy);
            g.drawString(font, Component.literal(String.valueOf(ScratchCards.PRIZES[s])).withStyle(mine ? ChatFormatting.GOLD : ChatFormatting.WHITE), cx + 19, cy + 4, 0xFFFFFF, true);
        }
        g.drawString(font, Component.translatable("casino.scratch.close_hint").withStyle(ChatFormatting.DARK_GRAY), left + 12, top + H - 11, 0xFFFFFF, false);
        super.render(g, mx, my, pt);
    }
}
