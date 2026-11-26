package com.coleccionista.client;

import com.axeliscore.ui.Ui;
import com.coleccionista.Coleccionista;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * (Cliente) Los créditos del final: suben despacio sobre negro (después de la luz blanca).
 * <ul>
 *     <li>EL COLECCIONISTA · Un evento de Axelis.</li>
 *     <li>La colección: cada pieza en el orden en que cayó, con su cara, su número y dónde cayó.</li>
 *     <li>La única pieza que salió: el ganador, en grande.</li>
 *     <li>Hecho por Axelis (la idea, la organización y la voz del Coleccionista).</li>
 *     <li>Gracias por jugar.</li>
 * </ul>
 * Duran lo que diga el servidor (según cuántas piezas hay); con Esc se saltan (queda negro hasta lo de después).
 */
public class Creditos extends Screen {
    private static final String MOD = Coleccionista.MODID;
    private static final int CREMA = 0xFFEFE3CC, ORO = 0xFFC9A25A, ROJO = 0xFFB3192B, GRIS = 0xFF8C847C;

    /** Un renglón de los créditos: qué se dibuja y cuánto mide. */
    private record Renglon(int alto, Dibujo d) {
    }

    @FunctionalInterface
    private interface Dibujo {
        void en(GuiGraphics g, Font f, int cx, int y, float a);
    }

    private final List<Renglon> renglones = new ArrayList<>();
    private final int dura;
    private final long inicio = System.currentTimeMillis();
    private int alto;

    public Creditos(ListTag piezas, int duraTicks, UUID ganador) {
        super(Component.translatable(MOD + ".des.creditos.titulo"));
        this.dura = Math.max(200, duraTicks);
        armar(piezas, ganador);
    }

    private static String tr(String k, Object... a) {
        return Component.translatable(MOD + ".des.creditos." + k, a).getString();
    }

    private static void texto(GuiGraphics g, Font f, String s, int cx, int y, float escala, int color, float a) {
        g.pose().pushPose();
        g.pose().translate(cx, y, 0);
        g.pose().scale(escala, escala, 1F);
        g.drawString(f, s, -f.width(s) / 2, 0, Ui.alpha(color, a), false);
        g.pose().popPose();
    }

    private static void espaciado(GuiGraphics g, Font f, String s, int cx, int y, float escala, int color, float a) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            b.append(s.charAt(i));
            if (i < s.length() - 1) {
                b.append(' ');
            }
        }
        texto(g, f, b.toString(), cx, y, escala, color, a);
    }

    private static void linea(GuiGraphics g, int cx, int y, int ancho, float a) {
        g.fillGradient(cx - ancho, y, cx, y + 1, Ui.alpha(0x008E1726, a), Ui.alpha(0xFFC9A25A, a));
        g.fill(cx - ancho, y, cx, y + 1, Ui.alpha(0x408E1726, a));
        g.fill(cx, y, cx + ancho, y + 1, Ui.alpha(0x408E1726, a));
        g.fill(cx - ancho / 3, y, cx + ancho / 3, y + 1, Ui.alpha(0xFFC9A25A, a));
    }

    private void armar(ListTag piezas, UUID ganador) {
        // el título
        renglones.add(new Renglon(22, (g, f, cx, y, a) -> espaciado(g, f, tr("el"), cx, y, 1.4F, 0xFFB8A888, a)));
        renglones.add(new Renglon(40, (g, f, cx, y, a) -> texto(g, f, tr("titulo"), cx, y, 3.4F, CREMA, a)));
        renglones.add(new Renglon(14, (g, f, cx, y, a) -> linea(g, cx, y + 4, 90, a)));
        renglones.add(new Renglon(70, (g, f, cx, y, a) -> texto(g, f, tr("evento"), cx, y, 1.2F, ORO, a)));
        // la colección (las que cayeron, en orden)
        List<CompoundTag> caidas = new ArrayList<>();
        CompoundTag gano = null;
        for (int i = 0; i < piezas.size(); i++) {
            CompoundTag c = piezas.getCompound(i);
            if (c.getBoolean("gano") || (ganador != null && c.hasUUID("id") && ganador.equals(c.getUUID("id")))) {
                gano = c;
            } else if (c.getInt("orden") > 0) {
                caidas.add(c);
            }
        }
        if (!caidas.isEmpty()) {
            renglones.add(new Renglon(22, (g, f, cx, y, a) -> espaciado(g, f, tr("coleccion"), cx, y, 1.6F, ROJO, a)));
            renglones.add(new Renglon(24, (g, f, cx, y, a) -> linea(g, cx, y + 4, 70, a)));
            for (CompoundTag c : caidas) {
                UUID id = c.hasUUID("id") ? c.getUUID("id") : null;
                String nombre = c.getString("nombre"), etiqueta = c.getString("etiqueta"), motivo = c.getString("motivo");
                renglones.add(new Renglon(40, (g, f, cx, y, a) -> {
                    int x = cx - 110;
                    if (id != null) {
                        g.setColor(1F, 1F, 1F, a);
                        PlayerFaceRenderer.draw(g, EventoCliente.piel(id), x, y, 28);
                        g.setColor(1F, 1F, 1F, 1F);
                    }
                    g.drawString(f, tr("pieza", etiqueta), x + 38, y + 3, Ui.alpha(ORO, a), false);
                    g.drawString(f, nombre, x + 38 + f.width(tr("pieza", etiqueta)) + 6, y + 3, Ui.alpha(CREMA, a), false);
                    String m = motivo.isEmpty() ? tr("cayo_sin") : tr("cayo", motivo);
                    g.drawString(f, m, x + 38, y + 16, Ui.alpha(GRIS, a), false);
                }));
            }
            renglones.add(new Renglon(50, (g, f, cx, y, a) -> {
            }));
        }
        // el ganador
        if (gano != null) {
            UUID id = gano.hasUUID("id") ? gano.getUUID("id") : null;
            String nombre = gano.getString("nombre"), etiqueta = gano.getString("etiqueta");
            renglones.add(new Renglon(30, (g, f, cx, y, a) -> espaciado(g, f, tr("salio"), cx, y, 1.5F, ORO, a)));
            renglones.add(new Renglon(78, (g, f, cx, y, a) -> {
                if (id != null) {
                    // un marco dorado y la cara grande
                    g.fill(cx - 34, y + 2, cx + 34, y + 70, Ui.alpha(0xFFC9A25A, a));
                    g.fill(cx - 33, y + 3, cx + 33, y + 69, Ui.alpha(0xFF120608, a));
                    g.setColor(1F, 1F, 1F, a);
                    PlayerFaceRenderer.draw(g, EventoCliente.piel(id), cx - 32, y + 4, 64);
                    g.setColor(1F, 1F, 1F, 1F);
                }
            }));
            renglones.add(new Renglon(30, (g, f, cx, y, a) -> texto(g, f, nombre, cx, y, 2.6F, CREMA, a)));
            renglones.add(new Renglon(70, (g, f, cx, y, a) -> texto(g, f, tr("pieza", etiqueta), cx, y, 1.1F, GRIS, a)));
        }
        // hecho por
        renglones.add(new Renglon(18, (g, f, cx, y, a) -> espaciado(g, f, tr("hecho"), cx, y, 1.0F, GRIS, a)));
        renglones.add(new Renglon(36, (g, f, cx, y, a) -> espaciado(g, f, tr("axelis"), cx, y, 3.0F, CREMA, a)));
        renglones.add(new Renglon(14, (g, f, cx, y, a) -> linea(g, cx, y + 4, 60, a)));
        renglones.add(new Renglon(14, (g, f, cx, y, a) -> texto(g, f, tr("idea"), cx, y, 1.0F, 0xFFB8A888, a)));
        renglones.add(new Renglon(14, (g, f, cx, y, a) -> texto(g, f, tr("organizacion"), cx, y, 1.0F, 0xFFB8A888, a)));
        renglones.add(new Renglon(80, (g, f, cx, y, a) -> texto(g, f, tr("voz"), cx, y, 1.0F, 0xFFB8A888, a)));
        renglones.add(new Renglon(20, (g, f, cx, y, a) -> texto(g, f, tr("gracias"), cx, y, 1.6F, CREMA, a)));
        for (Renglon r : renglones) {
            alto += r.alto();
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        DesenlaceCliente.creditosCerrados();
        super.onClose();
    }

    @Override
    public void renderBackground(GuiGraphics g) {
    }

    private static float suave(float x) {
        x = Mth.clamp(x, 0, 1);
        return x * x * (3 - 2 * x);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        float t = (System.currentTimeMillis() - inicio) / 1000F;
        float total = dura / 20F;
        g.fill(0, 0, width, height, 0xFF000000);
        Font f = font;
        // suben desde abajo hasta que el último renglón queda en medio, y ahí se quedan (con un margen al final)
        float viaje = alto + height / 2F - 10;
        float k = Mth.clamp((t - 1.2F) / Math.max(4F, total - 5.5F), 0, 1);
        int y = (int) (height - viaje * k);
        int cx = width / 2;
        for (Renglon r : renglones) {
            if (y > -80 && y < height + 10) {
                // se desvanecen cerca de las orillas de arriba y de abajo
                float borde = height * 0.16F;
                float a = Mth.clamp(Math.min((y + 30) / borde, (height - y) / borde), 0, 1);
                if (a > 0.01F) {
                    r.d().en(g, f, cx, y, a);
                }
            }
            y += r.alto();
        }
        // al empezar, la luz blanca se va apagando; al final se va a negro
        if (t < 2.2F) {
            g.fill(0, 0, width, height, Ui.alpha(0xFFFFFDF6, 1 - suave(t / 2.2F)));
        }
        if (t > total - 1.2F) {
            g.fill(0, 0, width, height, Ui.alpha(0xFF000000, suave((t - (total - 1.2F)) / 1.0F)));
        }
        if (t > 3F && t < total - 2F) {
            String s = tr("saltar");
            g.drawString(f, s, width - 8 - f.width(s), height - 14, 0x80777068, false);
        }
    }
}
