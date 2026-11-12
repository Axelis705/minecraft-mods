package com.casino.plat.client;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
//#if MC >= 26.1
//$$ import net.minecraft.client.gui.GuiGraphicsExtractor;
//$$ import net.minecraft.client.input.KeyEvent;
//$$ import net.minecraft.client.input.MouseButtonEvent;
//#else
import net.minecraft.client.gui.GuiGraphics;
//#endif

/**
 * Base de las pantallas del casino: cada una dibuja en {@link #draw} y recibe teclas y clics con números simples; aquí se conecta con
 * la forma de cada versión. El fondo oscuro solo se pone si la pantalla lo pide con {@link #drawBackground}.
 */
public abstract class CasinoScreen extends Screen {
    /** ¿Está apretado el botón izquierdo del mouse ahora mismo? */
    public static boolean leftDown() {
        return net.minecraft.client.Minecraft.getInstance().mouseHandler.isLeftPressed();
    }

    /** ¿Está apretado Shift? */
    public static boolean shift() {
        //#if MC >= 26.1
        //$$ return net.minecraft.client.Minecraft.getInstance().hasShiftDown();
        //#else
        return Screen.hasShiftDown();
        //#endif
    }

    private boolean bgNow;
    //#if MC >= 26.1
    //$$ private MouseButtonEvent lastMouse;
    //$$ private KeyEvent lastKey;
    //#endif

    protected CasinoScreen(Component title) {
        super(title);
    }

    protected abstract void draw(G g, int mx, int my, float pt);

    /** Dibuja los botones y demás componentes. */
    protected void drawWidgets(G g, int mx, int my, float pt) {
        //#if MC >= 26.1
        //$$ super.extractRenderState(g.raw, mx, my, pt);
        //#else
        super.render(g.raw, mx, my, pt);
        //#endif
    }

    /** El fondo oscuro de Minecraft detrás de la pantalla. */
    protected void drawBackground(G g, int mx, int my, float pt) {
        bgNow = true;
        //#if MC >= 26.1
        //$$ super.extractBackground(g.raw, mx, my, pt);
        //#elif MC >= 1.20.2
        //$$ super.renderBackground(g.raw, mx, my, pt);
        //#else
        super.renderBackground(g.raw);
        //#endif
        bgNow = false;
    }

    //#if MC >= 26.1
    //$$ @Override
    //$$ public void extractRenderState(GuiGraphicsExtractor gg, int mx, int my, float pt) {
    //$$     draw(new G(gg), mx, my, pt);
    //$$ }
    //$$
    //$$ @Override
    //$$ public void extractBackground(GuiGraphicsExtractor gg, int mx, int my, float pt) {
    //$$     // solo cuando la pantalla lo pide (Minecraft lo llamaría siempre)
    //$$     if (bgNow) {
    //$$         super.extractBackground(gg, mx, my, pt);
    //$$     }
    //$$ }
    //$$
    //$$ @Override
    //$$ public boolean mouseClicked(MouseButtonEvent e, boolean dbl) {
    //$$     lastMouse = e;
    //$$     return onClick(e.x(), e.y(), e.button());
    //$$ }
    //$$
    //$$ @Override
    //$$ public boolean mouseReleased(MouseButtonEvent e) {
    //$$     lastMouse = e;
    //$$     return onRelease(e.x(), e.y(), e.button());
    //$$ }
    //$$
    //$$ @Override
    //$$ public boolean mouseDragged(MouseButtonEvent e, double dx, double dy) {
    //$$     lastMouse = e;
    //$$     return onDrag(e.x(), e.y(), e.button(), dx, dy);
    //$$ }
    //$$
    //$$ @Override
    //$$ public boolean keyPressed(KeyEvent e) {
    //$$     lastKey = e;
    //#if MC >= 26.3
    //$$     return onKey(e.key(), e.keycode(), e.modifiers());
    //#else
    //$$     return onKey(e.key(), e.scancode(), e.modifiers());
    //#endif
    //$$ }
    //$$
    //$$ protected boolean superClick(double x, double y, int b) {
    //$$     return super.mouseClicked(lastMouse, false);
    //$$ }
    //$$
    //$$ protected boolean superRelease(double x, double y, int b) {
    //$$     return super.mouseReleased(lastMouse);
    //$$ }
    //$$
    //$$ protected boolean superDrag(double x, double y, int b, double dx, double dy) {
    //$$     return super.mouseDragged(lastMouse, dx, dy);
    //$$ }
    //$$
    //$$ protected boolean superKey(int key, int scan, int mods) {
    //$$     return super.keyPressed(lastKey);
    //$$ }
    //#else
    @Override
    public void render(GuiGraphics gg, int mx, int my, float pt) {
        draw(new G(gg), mx, my, pt);
    }

    //#if MC >= 1.20.2
    //$$ @Override
    //$$ public void renderBackground(GuiGraphics gg, int mx, int my, float pt) {
    //$$     // solo cuando la pantalla lo pide (Screen.render lo llamaría siempre)
    //$$     if (bgNow) {
    //$$         super.renderBackground(gg, mx, my, pt);
    //$$     }
    //$$ }
    //#endif

    @Override
    public boolean mouseClicked(double x, double y, int b) {
        return onClick(x, y, b);
    }

    @Override
    public boolean mouseReleased(double x, double y, int b) {
        return onRelease(x, y, b);
    }

    @Override
    public boolean mouseDragged(double x, double y, int b, double dx, double dy) {
        return onDrag(x, y, b, dx, dy);
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        return onKey(key, scan, mods);
    }

    protected boolean superClick(double x, double y, int b) {
        return super.mouseClicked(x, y, b);
    }

    protected boolean superRelease(double x, double y, int b) {
        return super.mouseReleased(x, y, b);
    }

    protected boolean superDrag(double x, double y, int b, double dx, double dy) {
        return super.mouseDragged(x, y, b, dx, dy);
    }

    protected boolean superKey(int key, int scan, int mods) {
        return super.keyPressed(key, scan, mods);
    }
    //#endif

    //#if MC >= 1.20.2
    //$$ @Override
    //$$ public boolean mouseScrolled(double x, double y, double sx, double sy) {
    //$$     return onScroll(x, y, sy) || super.mouseScrolled(x, y, sx, sy);
    //$$ }
    //#else
    @Override
    public boolean mouseScrolled(double x, double y, double delta) {
        return onScroll(x, y, delta) || super.mouseScrolled(x, y, delta);
    }
    //#endif

    protected boolean onClick(double x, double y, int b) {
        return superClick(x, y, b);
    }

    protected boolean onRelease(double x, double y, int b) {
        return superRelease(x, y, b);
    }

    protected boolean onDrag(double x, double y, int b, double dx, double dy) {
        return superDrag(x, y, b, dx, dy);
    }

    protected boolean onKey(int key, int scan, int mods) {
        return superKey(key, scan, mods);
    }

    protected boolean onScroll(double x, double y, double delta) {
        return false;
    }
}
