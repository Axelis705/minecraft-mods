package com.casino.client;

import com.casino.decor.NeonLetterBlock;
import com.casino.net.CasinoNet;
import com.casino.net.NeonPacket;
import com.casino.plat.Ids;
import com.casino.plat.client.CasinoScreen;
import com.casino.plat.client.G;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

/**
 * Elegir letra y color de una letra de neón. Con el teclado se escribe: cada tecla pone la letra y salta a la siguiente de la
 * derecha; Retroceso vuelve a la anterior. Abajo se ve el letrero completo con la letra actual marcada.
 */
public final class NeonScreen extends CasinoScreen {
    private static final int COLS = 12;
    private static final int CELL = 22;
    private static final String[] NAMES = {"excl", "quest", "dollar", "and", "heart", "spade", "diamond", "club", "star", "dash", "space"};
    private static final String SPECIAL = "!?$&♥♠♦♣★- ";
    private static final int[] SWATCH = {0xFFFF2846, 0xFFFF46C8, 0xFFFFC428, 0xFF3CFF78, 0xFF28DCFF, 0xFFF0F0FF};

    private BlockPos pos;
    private int glyph;
    private int color;
    private int left;
    private int top;
    private int w;
    private int h;

    public NeonScreen(BlockPos pos) {
        super(Component.translatable("casino.neon.title"));
        this.pos = pos;
        BlockState s = state(pos);
        if (s != null) {
            glyph = s.getValue(NeonLetterBlock.GLYPH);
            color = s.getValue(NeonLetterBlock.COLOR);
        }
    }

    private static BlockState state(BlockPos p) {
        Level l = Minecraft.getInstance().level;
        if (l == null) {
            return null;
        }
        BlockState s = l.getBlockState(p);
        return s.getBlock() instanceof NeonLetterBlock ? s : null;
    }

    static ResourceLocation tex(int color, int glyph) {
        char c = NeonLetterBlock.CHARS.charAt(glyph);
        int sp = SPECIAL.indexOf(c);
        String name = sp >= 0 ? NAMES[sp] : String.valueOf(Character.toLowerCase(c));
        return Ids.casino("textures/block/decor/neon/" + NeonLetterBlock.COLORS[color] + "_" + name + ".png");
    }

    @Override
    protected void init() {
        w = COLS * CELL + 16;
        int rows = (NeonLetterBlock.CHARS.length() + COLS - 1) / COLS;
        h = 40 + rows * CELL + 34 + 50 + 30;
        left = (width - w) / 2;
        top = Math.max(4, (height - h) / 2);
        addRenderableWidget(Button.builder(Component.translatable("casino.neon.done"), b -> onClose())
                .bounds(left + w / 2 - 40, top + h - 26, 80, 20).build());
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void set(int g, int c) {
        glyph = g;
        color = c;
        CasinoNet.sendToServer(new NeonPacket(pos, g, c));
    }

    @Override
    protected boolean onClick(double mx, double my, int button) {
        int gx = left + 8;
        int gy = top + 40;
        int n = NeonLetterBlock.CHARS.length();
        for (int i = 0; i < n; i++) {
            int x = gx + (i % COLS) * CELL;
            int y = gy + (i / COLS) * CELL;
            if (mx >= x && mx < x + CELL - 2 && my >= y && my < y + CELL - 2) {
                set(i, color);
                return true;
            }
        }
        int sy = gy + ((n + COLS - 1) / COLS) * CELL + 16;
        for (int c = 0; c < SWATCH.length; c++) {
            int x = left + w / 2 - SWATCH.length * 13 + c * 26;
            if (mx >= x && mx < x + 22 && my >= sy && my < sy + 16) {
                set(glyph, c);
                // el color se aplica a todo el letrero
                for (BlockPos p : row()) {
                    BlockState s = state(p);
                    if (s != null && !p.equals(pos)) {
                        CasinoNet.sendToServer(new NeonPacket(p, s.getValue(NeonLetterBlock.GLYPH), c));
                    }
                }
                return true;
            }
        }
        return super.onClick(mx, my, button);
    }

    @Override
    protected boolean onKey(int key, int scan, int mods) {
        int g = -1;
        if (key >= 65 && key <= 90) {
            g = key - 65;
        } else if (key >= 48 && key <= 57) {
            g = 26 + key - 48;
        } else if (key == 32) {
            g = NeonLetterBlock.CHARS.indexOf(' ');
        } else if (key == 45) {
            g = NeonLetterBlock.CHARS.indexOf('-');
        }
        if (g >= 0) {
            set(g, color);
            BlockState s = state(pos);
            if (s != null) {
                BlockPos nx = NeonLetterBlock.next(s, pos);
                if (state(nx) != null && state(nx).getValue(NeonLetterBlock.FACING) == s.getValue(NeonLetterBlock.FACING)) {
                    move(nx);
                }
            }
            return true;
        }
        if (key == 259) { // retroceso
            BlockState s = state(pos);
            if (s != null) {
                BlockPos pv = NeonLetterBlock.previous(s, pos);
                if (state(pv) != null) {
                    move(pv);
                }
            }
            return true;
        }
        if (key == 262 || key == 263) { // flechas
            BlockState s = state(pos);
            if (s != null) {
                BlockPos t = key == 262 ? NeonLetterBlock.next(s, pos) : NeonLetterBlock.previous(s, pos);
                if (state(t) != null) {
                    move(t);
                }
            }
            return true;
        }
        return super.onKey(key, scan, mods);
    }

    private void move(BlockPos p) {
        pos = p;
        BlockState s = state(p);
        if (s != null) {
            glyph = s.getValue(NeonLetterBlock.GLYPH);
        }
    }

    /** Todas las letras unidas en la misma fila (mismo sentido), de izquierda a derecha. */
    private List<BlockPos> row() {
        List<BlockPos> out = new ArrayList<>();
        BlockState s = state(pos);
        if (s == null) {
            return out;
        }
        BlockPos start = pos;
        for (int i = 0; i < 32; i++) {
            BlockPos pv = NeonLetterBlock.previous(s, start);
            BlockState ps = state(pv);
            if (ps == null || ps.getValue(NeonLetterBlock.FACING) != s.getValue(NeonLetterBlock.FACING)) {
                break;
            }
            start = pv;
        }
        BlockPos p = start;
        for (int i = 0; i < 32; i++) {
            BlockState ps = state(p);
            if (ps == null || ps.getValue(NeonLetterBlock.FACING) != s.getValue(NeonLetterBlock.FACING)) {
                break;
            }
            out.add(p);
            p = NeonLetterBlock.next(ps, p);
        }
        return out;
    }

    @Override
    protected void draw(G g, int mx, int my, float pt) {
        drawBackground(g, mx, my, pt);
        g.fill(left - 2, top - 2, left + w + 2, top + h + 2, 0xFFB0307A);
        g.fill(left, top, left + w, top + h, 0xF0100C16);
        g.drawCenteredString(font, Component.translatable("casino.neon.title").withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD), left + w / 2, top + 7, 0xFFFFFF);
        g.drawCenteredString(font, Component.translatable("casino.neon.help").withStyle(ChatFormatting.GRAY), left + w / 2, top + 22, 0xFFFFFF);
        int gx = left + 8;
        int gy = top + 40;
        int n = NeonLetterBlock.CHARS.length();
        for (int i = 0; i < n; i++) {
            int x = gx + (i % COLS) * CELL;
            int y = gy + (i / COLS) * CELL;
            boolean sel = i == glyph;
            boolean hover = mx >= x && mx < x + CELL - 2 && my >= y && my < y + CELL - 2;
            g.fill(x - 1, y - 1, x + CELL - 1, y + CELL - 1, sel ? 0xFFFFD040 : hover ? 0xFF7A6A90 : 0xFF2A2234);
            g.blit(tex(color, i), x, y, CELL - 2, CELL - 2, 0, 0, 32, 32, 32, 32);
        }
        int sy = gy + ((n + COLS - 1) / COLS) * CELL + 4;
        g.drawCenteredString(font, Component.translatable("casino.neon.color").withStyle(ChatFormatting.WHITE), left + w / 2, sy, 0xFFFFFF);
        sy += 12;
        for (int c = 0; c < SWATCH.length; c++) {
            int x = left + w / 2 - SWATCH.length * 13 + c * 26;
            g.fill(x - 2, sy - 2, x + 24, sy + 18, c == color ? 0xFFFFFFFF : 0xFF2A2234);
            g.fill(x, sy, x + 22, sy + 16, SWATCH[c]);
        }
        // el letrero
        List<BlockPos> row = row();
        int ly = sy + 26;
        int size = Math.min(26, (w - 16) / Math.max(1, row.size()));
        int lx = left + w / 2 - row.size() * size / 2;
        for (int i = 0; i < row.size(); i++) {
            BlockState s = state(row.get(i));
            if (s == null) {
                continue;
            }
            int x = lx + i * size;
            if (row.get(i).equals(pos)) {
                g.fill(x - 1, ly - 1, x + size - 1, ly + size - 1, 0xFFFFD040);
            }
            g.blit(tex(s.getValue(NeonLetterBlock.COLOR), s.getValue(NeonLetterBlock.GLYPH)), x, ly, size - 2, size - 2, 0, 0, 32, 32, 32, 32);
        }
        drawWidgets(g, mx, my, pt);
    }
}
