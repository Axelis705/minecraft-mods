package com.coleccionista.client.inicio;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;

import java.util.function.DoubleConsumer;
import java.util.function.DoubleFunction;
import java.util.function.DoubleSupplier;

/**
 * (Cliente) Cómo se ven los menús del evento: paneles casi negros con borde dorado, botones de terciopelo que se encienden al pasar el
 * ratón (con una notita de caja de música) y deslizadores con su perilla dorada.
 */
public final class Estilo {
    public static final int ORO = 0xFFD9A93A, ORO_OSCURO = 0xFF7A5A1E, VINO = 0xFF5A0D16, VINO_CLARO = 0xFF8E1726, TINTA = 0xF0100709,
            TEXTO = 0xFFF2E6CF, TEXTO_APAGADO = 0xFFA89880;

    private Estilo() {
    }

    /** Un panel: fondo casi negro, borde dorado de un píxel y otro oscuro por dentro. */
    public static void panel(GuiGraphics g, int x0, int y0, int x1, int y1) {
        g.fill(x0, y0, x1, y1, TINTA);
        marco(g, x0, y0, x1, y1, ORO_OSCURO);
        marco(g, x0 + 1, y0 + 1, x1 - 1, y1 - 1, 0x60000000);
    }

    public static void marco(GuiGraphics g, int x0, int y0, int x1, int y1, int c) {
        g.fill(x0, y0, x1, y0 + 1, c);
        g.fill(x0, y1 - 1, x1, y1, c);
        g.fill(x0, y0, x0 + 1, y1, c);
        g.fill(x1 - 1, y0, x1, y1, c);
    }

    /** Un texto grande (escalado) con sombra roja, centrado en x si {@code centro}. */
    public static void grande(GuiGraphics g, String s, float x, float y, float esc, int color, boolean centro) {
        Font f = Minecraft.getInstance().font;
        g.pose().pushPose();
        g.pose().translate(centro ? x - f.width(s) * esc / 2F : x, y, 0);
        g.pose().scale(esc, esc, 1);
        g.drawString(f, s, 1, 1, 0xAA3A0008, false);
        g.drawString(f, s, 0, 0, color, false);
        g.pose().popPose();
    }

    // ------------------------------------------------------------------ el botón

    /** Un botón del evento. */
    public static class Boton extends AbstractButton {
        private final Runnable accion;
        private boolean antes;
        /** Cuánto está encendido (sube al pasar el ratón y baja al irse). */
        private float luz;

        public Boton(int x, int y, int w, int h, Component texto, Runnable accion) {
            super(x, y, w, h, texto);
            this.accion = accion;
        }

        @Override
        public void onPress() {
            accion.run();
        }

        @Override
        protected void renderWidget(GuiGraphics g, int mx, int my, float pt) {
            boolean encima = isHoveredOrFocused() && active;
            if (encima && !antes) {
                Inicio.sonar("menu.encima", SoundSource.MASTER, 0.35F, 0.9F + (float) Math.random() * 0.2F);
            }
            antes = encima;
            luz = Mth.clamp(luz + (encima ? 0.2F : -0.12F), 0F, 1F);
            int x0 = getX(), y0 = getY(), x1 = x0 + width, y1 = y0 + height;
            g.fill(x0, y0, x1, y1, mezcla(0xE0140609, 0xF0420A12, luz));
            // una franja de terciopelo que se llena de izquierda a derecha al pasar el ratón
            int lleno = (int) (width * luz);
            if (lleno > 0) {
                g.fillGradient(x0, y0, x0 + lleno, y1, 0x705A0D16, 0x308E1726);
            }
            marco(g, x0, y0, x1, y1, active ? mezcla(ORO_OSCURO, ORO, luz) : 0xFF3A3028);
            Font f = Minecraft.getInstance().font;
            int c = !active ? 0xFF6A6058 : mezcla(TEXTO_APAGADO, TEXTO, luz);
            String s = getMessage().getString();
            int tx = x0 + (width - f.width(s)) / 2 + (int) (luz * 2);
            g.drawString(f, s, tx, y0 + (height - 8) / 2, c, true);
            // una flechita dorada a la izquierda cuando está encendido
            if (luz > 0.3F) {
                int ax = tx - 8 - (int) ((1 - luz) * 4);
                g.drawString(f, "›", ax, y0 + (height - 8) / 2, (int) (luz * 255) << 24 | (ORO & 0xFFFFFF), true);
            }
        }

        @Override
        public void playDownSound(SoundManager sm) {
            Inicio.sonar("menu.clic", SoundSource.MASTER, 0.6F, 1F);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput o) {
            defaultButtonNarrationText(o);
        }
    }

    // ------------------------------------------------------------------ el deslizador

    /** Un deslizador del evento (de 0 a 1), con su texto ("Música: 80%"). */
    public static class Deslizador extends AbstractSliderButton {
        private final DoubleConsumer poner;
        private final DoubleFunction<Component> texto;

        public Deslizador(int x, int y, int w, int h, DoubleSupplier leer, DoubleConsumer poner, DoubleFunction<Component> texto) {
            super(x, y, w, h, Component.empty(), Mth.clamp(leer.getAsDouble(), 0, 1));
            this.poner = poner;
            this.texto = texto;
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            setMessage(texto.apply(value));
        }

        @Override
        protected void applyValue() {
            poner.accept(value);
        }

        @Override
        public void renderWidget(GuiGraphics g, int mx, int my, float pt) {
            boolean encima = isHoveredOrFocused();
            int x0 = getX(), y0 = getY(), x1 = x0 + width, y1 = y0 + height;
            g.fill(x0, y0, x1, y1, 0xE0140609);
            int lleno = (int) ((width - 2) * value);
            g.fillGradient(x0 + 1, y0 + 1, x0 + 1 + lleno, y1 - 1, 0xC05A0D16, 0xC08E1726);
            marco(g, x0, y0, x1, y1, encima ? ORO : ORO_OSCURO);
            // la perilla: dos muescas (arriba y abajo) para no tapar el texto
            int px = x0 + 1 + lleno, cp = encima ? ORO : 0xFFB08A34;
            g.fill(px - 2, y0, px + 2, y0 + 3, cp);
            g.fill(px - 2, y1 - 3, px + 2, y1, cp);
            Font f = Minecraft.getInstance().font;
            String s = getMessage().getString();
            g.drawString(f, s, x0 + (width - f.width(s)) / 2, y0 + (height - 8) / 2, encima ? TEXTO : TEXTO_APAGADO, true);
        }

        @Override
        public void playDownSound(SoundManager sm) {
            Inicio.sonar("menu.encima", SoundSource.MASTER, 0.3F, 1.2F);
        }
    }

    /** Mezcla dos colores ARGB ({@code k} de 0 a 1). */
    public static int mezcla(int a, int b, float k) {
        int r = 0;
        for (int s = 0; s < 32; s += 8) {
            int ca = a >>> s & 0xFF, cb = b >>> s & 0xFF;
            r |= Math.round(ca + (cb - ca) * k) << s;
        }
        return r;
    }
}
