package com.showdeaxelis.client;

import com.showdeaxelis.game.DrawGame;
import com.showdeaxelis.net.ShowNet;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;

/** La pantalla del dibujante: un lienzo para dibujar con el raton, colores, grosores y borrar. */
public final class DrawScreen extends Screen {
    private static final int[] SIZES = {2, 3, 4, 6};
    private int color = 0;
    private int size = 3;
    private boolean drawing;
    private int lastX;
    private int lastY;
    private final List<Integer> buf = new ArrayList<>();
    private float scale = 2f;
    private int cx;
    private int cy;
    private int palX;

    public DrawScreen() {
        super(Component.literal("Dibujar"));
    }

    public static void open() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && !(mc.screen instanceof DrawScreen)) {
            mc.setScreen(new DrawScreen());
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    protected void init() {
        scale = Math.max(1f, Math.min((width - 150) / (float) DrawGame.W, (height - 70) / (float) DrawGame.H));
        cx = 10;
        cy = 40;
        palX = cx + (int) (DrawGame.W * scale) + 14;
        addRenderableWidget(Button.builder(Component.literal("Borrar todo"), b -> {
            DrawCanvas.clear();
            ShowNet.CHANNEL.sendToServer(new ShowNet.Draw(1, 0, 0, new int[0]));
        }).bounds(palX, cy + 128, 100, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Cerrar"), b -> onClose()).bounds(palX, cy + 152, 100, 20).build());
    }

    private CompoundTag dr() {
        return ShowClient.state.getCompound("dr");
    }

    private boolean live() {
        return ShowClient.active() && ShowClient.state.getString("phase").equals("DRAW") && dr().getBoolean("iDraw") && !dr().getBoolean("over");
    }

    @Override
    public void tick() {
        if (!live()) {
            onClose();
        }
    }

    private int toX(double mx) {
        return Math.max(0, Math.min(DrawGame.W - 1, (int) ((mx - cx) / scale)));
    }

    private int toY(double my) {
        return Math.max(0, Math.min(DrawGame.H - 1, (int) ((my - cy) / scale)));
    }

    private boolean inCanvas(double mx, double my) {
        return mx >= cx && my >= cy && mx < cx + DrawGame.W * scale && my < cy + DrawGame.H * scale;
    }

    private void flush() {
        if (buf.size() >= 2) {
            int[] pts = new int[buf.size()];
            for (int i = 0; i < pts.length; i++) {
                pts[i] = buf.get(i);
            }
            ShowNet.CHANNEL.sendToServer(new ShowNet.Draw(0, color, size, pts));
        }
        int lx = buf.size() >= 2 ? buf.get(buf.size() - 2) : lastX;
        int ly = buf.size() >= 2 ? buf.get(buf.size() - 1) : lastY;
        buf.clear();
        if (drawing) {
            buf.add(lx);
            buf.add(ly);
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (super.mouseClicked(mx, my, button)) {
            return true;
        }
        if (button == 0 && inCanvas(mx, my)) {
            drawing = true;
            lastX = toX(mx);
            lastY = toY(my);
            buf.clear();
            buf.add(lastX);
            buf.add(lastY);
            DrawCanvas.stroke(color, size, new int[] {lastX, lastY});
            return true;
        }
        if (button == 0) {
            for (int i = 0; i < DrawCanvas.PALETTE.length; i++) {
                int px = palX + (i % 4) * 25;
                int py = cy + (i / 4) * 25;
                if (mx >= px && mx < px + 22 && my >= py && my < py + 22) {
                    color = i;
                    return true;
                }
            }
            for (int i = 0; i < SIZES.length; i++) {
                int px = palX + i * 25;
                int py = cy + 106;
                if (mx >= px && mx < px + 22 && my >= py && my < py + 18) {
                    size = SIZES[i];
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (drawing && button == 0) {
            int x = toX(mx);
            int y = toY(my);
            if (Math.abs(x - lastX) + Math.abs(y - lastY) >= 2) {
                DrawCanvas.stroke(color, size, new int[] {lastX, lastY, x, y});
                lastX = x;
                lastY = y;
                buf.add(x);
                buf.add(y);
                if (buf.size() >= 16) {
                    flush();
                }
            }
            return true;
        }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        if (drawing && button == 0) {
            drawing = false;
            if (buf.size() == 2) {
                buf.add(buf.get(0));
                buf.add(buf.get(1));
            }
            flush();
            buf.clear();
            return true;
        }
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        renderBackground(g);
        CompoundTag d = dr();
        String word = d.getString("word");
        g.drawString(font, "Dibuja: " + word.toUpperCase(java.util.Locale.ROOT) + "   (no la digas en el chat)", 10, 10, 0xFFFFE082, true);
        int left = Math.max(0, d.getInt("left") - (ShowClient.clientTick - ShowClient.stateStamp));
        g.drawString(font, "Tiempo: " + (left / 20) + " s     Acertaron: " + d.getList("solved", 8).size(), 10, 24, 0xFFFFFFFF, true);
        DrawCanvas.render(g, cx, cy, (int) (DrawGame.W * scale), (int) (DrawGame.H * scale));
        for (int i = 0; i < DrawCanvas.PALETTE.length; i++) {
            int px = palX + (i % 4) * 25;
            int py = cy + (i / 4) * 25;
            g.fill(px - 2, py - 2, px + 24, py + 24, i == color ? 0xFFFFFFFF : 0xFF000000);
            g.fill(px, py, px + 22, py + 22, 0xFF000000 | DrawCanvas.PALETTE[i]);
            if (i == 1) {
                g.drawString(font, "borrar", px + 1, py + 7, 0xFF888888, false);
            }
        }
        for (int i = 0; i < SIZES.length; i++) {
            int px = palX + i * 25;
            int py = cy + 106;
            g.fill(px - 2, py - 2, px + 24, py + 20, SIZES[i] == size ? 0xFFFFFFFF : 0xFF000000);
            g.fill(px, py, px + 22, py + 18, 0xFF333333);
            int r = Math.max(1, SIZES[i] - 1);
            g.fill(px + 11 - r, py + 9 - r, px + 11 + r, py + 9 + r, 0xFFFFFFFF);
        }
        super.render(g, mx, my, pt);
    }
}
