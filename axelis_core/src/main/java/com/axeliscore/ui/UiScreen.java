package com.axeliscore.ui;

import com.axeliscore.plat.client.CoreScreen;
import com.axeliscore.plat.client.G;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Pantalla con el estilo del Core: un panel centrado (de {@link #pw} × {@link #ph}) que aparece con un pequeño rebote, título,
 * componentes ({@link Widget}) y ayudas al pasar el mouse. Las subclases agregan componentes en {@link #build} (con posiciones
 * relativas al panel: {@link #px}, {@link #py}) y dibujan lo suyo en {@link #content}.
 */
public abstract class UiScreen extends CoreScreen {
    protected Theme theme;
    /** Solo para las pruebas automáticas: si no es null, la pantalla hace como si el mouse estuviera ahí. */
    public static int[] fakeMouse;
    protected final int pw, ph;
    protected int px, py;
    protected final List<Widget> widgets = new ArrayList<>();
    private long opened = -1, last;
    protected float time;
    /**
     * false: sin el panel del centro ni el fondo oscuro (la pantalla dibuja sus propios paneles en {@link #content} y se ve el mundo
     * detrás, como el editor de animaciones).
     */
    protected boolean panelPrincipal = true;

    protected UiScreen(Component title, int pw, int ph, Theme theme) {
        super(title);
        this.pw = pw;
        this.ph = ph;
        this.theme = theme;
    }

    @Override
    protected void init() {
        super.init();
        px = (width - pw) / 2;
        py = (height - ph) / 2;
        widgets.clear();
        build();
        if (opened < 0) {
            opened = System.currentTimeMillis();
        }
    }

    /** Agrega los componentes (se llama al abrir y al cambiar el tamaño de la ventana). */
    protected abstract void build();

    /** Lo propio de la pantalla (dentro del panel). */
    protected void content(G g, int mx, int my, float pt) {
    }

    protected <W extends Widget> W add(W w) {
        widgets.add(w);
        return w;
    }

    /** Cuánto lleva abierta (segundos). */
    protected float openTime() {
        return (System.currentTimeMillis() - opened) / 1000F;
    }

    /** Vuelve a armar la pantalla (por ejemplo, después de cambiar datos desde afuera). */
    public void reconstruir() {
        init();
    }

    /** Cambia el tema (colores y marco) y vuelve a armar la pantalla. */
    public void setTheme(Theme t) {
        this.theme = t;
        init();
    }

    @Override
    protected void draw(G g, int mx, int my, float pt) {
        if (fakeMouse != null) {
            mx = fakeMouse[0];
            my = fakeMouse[1];
        }
        long now = System.currentTimeMillis();
        float dt = last == 0 ? 0 : Math.min(0.1F, (now - last) / 1000F);
        last = now;
        time += dt;
        if (panelPrincipal) {
            drawBackground(g, mx, my, pt);
        }
        float a = Ui.pop(Math.min(1F, openTime() / 0.22F));
        g.push();
        if (panelPrincipal) {
            float s = 0.9F + 0.1F * a;
            g.translate(width / 2F, height / 2F, 0);
            g.scale(s, s, 1);
            g.translate(-width / 2F, -height / 2F, 0);
            Ui.panel(g, px, py, pw, ph, theme, Math.min(1F, openTime() / 0.12F));
            Ui.title(g, getTitle().getString(), px + pw / 2, py + 8, theme);
        }
        content(g, mx, my, pt);
        for (Widget w : widgets) {
            if (w.visible) {
                w.render(g, mx, my, dt, theme);
            }
        }
        g.pop();
        ondas(g);
        // cuadro de información de lo que está bajo el mouse (el de más arriba), que aparece suave
        InfoCard card = null;
        Object key = null;
        for (int i = widgets.size() - 1; i >= 0; i--) {
            Widget w = widgets.get(i);
            if (w.visible && w.inside(mx, my)) {
                card = w.card(mx, my);
                key = w.hoverKey(mx, my);
                if (card != null) {
                    break;
                }
            }
        }
        if (card == null) {
            card = extraCard(mx, my);
            key = card == null ? null : card.title == null ? "extra" : card.title.getString();
        }
        if (!java.util.Objects.equals(key, hoverKey)) {
            hoverKey = key;
            hoverSince = time;
        }
        if (card != null) {
            com.axeliscore.ui.client.Cards.drawAtMouse(g, card, mx, my, width, height, time - hoverSince);
        }
    }

    private Object hoverKey;
    private float hoverSince;

    /** Cuadro de información de algo que no es un componente (por ejemplo, partes que dibuja {@link #content}). */
    protected InfoCard extraCard(int mx, int my) {
        return null;
    }

    // Se recorre una copia: un botón puede rehacer la pantalla (pestaña, comprar, vender) y cambiar la lista mientras se recorre
    // (en 26.3 tiraba ConcurrentModificationException al soltar el clic).

    /** Ondas de los clics (x, y, cuándo). */
    private final List<float[]> ondas = new ArrayList<>();

    private void ondas(G g) {
        float now = Ui.tiempo();
        ondas.removeIf(o -> now - o[2] > 0.3F);
        for (float[] o : ondas) {
            float k = (now - o[2]) / 0.3F;
            int r = 2 + Math.round(k * 10);
            int c = Ui.alpha(0xFFFFFFFF, (1 - k) * 0.55F);
            int x = (int) o[0], y = (int) o[1];
            // cuadro que se abre (al estilo de pixel art)
            g.fill(x - r, y - r, x + r, y - r + 1, c);
            g.fill(x - r, y + r - 1, x + r, y + r, c);
            g.fill(x - r, y - r + 1, x - r + 1, y + r - 1, c);
            g.fill(x + r - 1, y - r + 1, x + r, y + r - 1, c);
        }
    }

    @Override
    protected boolean onClick(double x, double y, int b) {
        if (b == 0) {
            ondas.add(new float[] {(float) x, (float) y, Ui.tiempo()});
        }
        List<Widget> l = new ArrayList<>(widgets);
        for (int i = l.size() - 1; i >= 0; i--) {
            if (l.get(i).click(x, y, b)) {
                return true;
            }
        }
        return super.onClick(x, y, b);
    }

    @Override
    protected boolean onRelease(double x, double y, int b) {
        boolean any = false;
        for (Widget w : new ArrayList<>(widgets)) {
            any |= w.release(x, y, b);
        }
        return any || super.onRelease(x, y, b);
    }

    @Override
    protected boolean onDrag(double x, double y, int b, double dx, double dy) {
        for (Widget w : new ArrayList<>(widgets)) {
            if (w.drag(x, y, b)) {
                return true;
            }
        }
        return super.onDrag(x, y, b, dx, dy);
    }

    @Override
    protected boolean onScroll(double x, double y, double delta) {
        for (Widget w : new ArrayList<>(widgets)) {
            if (w.scroll(x, y, delta)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
