package com.coleccionista.client;

import com.axeliscore.data.client.ClientData;
import com.axeliscore.plat.client.G;
import com.axeliscore.ui.Button;
import com.axeliscore.ui.InfoCard;
import com.axeliscore.ui.Tabs;
import com.axeliscore.ui.Theme;
import com.axeliscore.ui.Ui;
import com.axeliscore.ui.UiScreen;
import com.axeliscore.ui.Widget;
import com.coleccionista.Coleccionista;
import com.coleccionista.evento.Panel;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * (Cliente) La pantalla del organizador (tecla O, solo operadores u organizadores): la <b>línea de tiempo</b> del evento por capítulos.
 * Cada capítulo es una fila de pasos en orden, de izquierda a derecha (con la rueda del ratón o las flechas se recorre); al pasar el ratón
 * sobre un paso sale qué hace, y con clic se hace. Abajo, lo de siempre: pausa, reloj, chat, disfraz y el panel completo.
 * <ul>
 *     <li>Capítulo 1 · El despertar y Los sentidos (lo nuevo).</li>
 *     <li>Capítulo 2 · La mascota (las escondidas en la casa; el organizador es Cascabel).</li>
 *     <li>Capítulos 3 a 8 · las pruebas que ya existen, cada una con "llevar", preparar, empezar y terminar.</li>
 * </ul>
 */
public class LineaScreen extends UiScreen {
    private static final String MOD = Coleccionista.MODID;
    private static int capitulo;
    private static int desde;
    /** Los pasos que ya se usaron en esta sesión (se marcan con palomita). */
    private static final java.util.Set<String> hechos = new java.util.HashSet<>();
    private static final int NODO = 62, VISIBLES = 6;

    /** Un paso: lo que se pide al servidor (op y datos), su nombre corto y su ayuda. */
    record Paso(String op, String clave, CompoundTag extra) {
    }

    /** Un capítulo: su nombre y sus pasos. */
    record Capitulo(String clave, List<Paso> pasos) {
    }

    public LineaScreen() {
        super(Component.translatable(MOD + ".linea.titulo"), 440, 236, PanelScreen.TEMA);
    }

    private static String tr(String k, Object... a) {
        return Component.translatable(MOD + ".linea." + k, a).getString();
    }

    private static CompoundTag zona(String z) {
        CompoundTag t = new CompoundTag();
        t.putString("zona", z);
        return t;
    }

    /** Todos los capítulos, en orden. */
    static List<Capitulo> capitulos() {
        List<Capitulo> l = new ArrayList<>();
        l.add(new Capitulo("c1", List.of(
                new Paso("sala", "sala", null),
                new Paso("despertar", "despertar", null),
                new Paso("discurso", "discurso", null),
                new Paso("abrir_celdas", "abrir_celdas", null),
                new Paso("salida", "salida", null),
                new Paso("porton", "porton", null),
                new Paso("cap1_equipos", "equipos", null),
                new Paso("cap1_bajar", "bajar", null),
                new Paso("sen_saltar", "saltar", null),
                new Paso("sen_terminar", "terminar", null),
                new Paso("sen_limpiar", "limpiar", null))));
        l.add(new Capitulo("c2", List.of(
                new Paso("man_llegar", "man_llegar", null),
                new Paso("man_soy", "man_soy", null),
                new Paso("man_saltar", "man_saltar", null),
                new Paso("man_soltar", "man_soltar", null),
                new Paso("man_terminar", "man_terminar", null),
                new Paso("man_limpiar", "man_limpiar", null))));
        l.add(new Capitulo("c3", List.of(
                new Paso("caja_llamar", "caja_llamar", null),
                new Paso("caja_llegar", "caja_llegar", null),
                new Paso("caja_saltar", "caja_saltar", null),
                new Paso("caja_bien", "caja_bien", null),
                new Paso("caja_bala", "caja_bala", null),
                new Paso("caja_auto", "caja_auto", null),
                new Paso("caja_terminar", "caja_terminar", null),
                new Paso("caja_limpiar", "caja_limpiar", null))));
        l.add(new Capitulo("c4", List.of(
                new Paso("sor_llamar", "sor_llamar", null),
                new Paso("sor_llegar", "sor_llegar", null),
                new Paso("sor_saltar", "sor_saltar", null),
                new Paso("sor_terminar", "sor_terminar", null),
                new Paso("sor_limpiar", "sor_limpiar", null))));
        // (el traidor de antes, en el Archivo, sigue en el panel y en /evento traidor)
        String[][] p = {
                {"c5", "salon", "com"}, {"c6", "laberintos", "cie"}, {"c7", "capilla", "sac"},
                {"c8", "puertas", "pue"}, {"c9", "final", "cop"}};
        for (String[] c : p) {
            List<Paso> ps = new ArrayList<>();
            ps.add(new Paso("llevar", "llevar", zona(c[1])));
            ps.add(new Paso(c[2] + "_preparar", "preparar", null));
            if ("pue".equals(c[2])) {
                ps.add(new Paso("pue_discurso", "pue_discurso", null));
                ps.add(new Paso("pue_ronda", "pue_ronda", null));
                ps.add(new Paso("pue_cerrar", "pue_cerrar", null));
            } else {
                ps.add(new Paso(c[2] + "_empezar", "empezar", null));
                ps.add(new Paso(c[2] + "_ya", "ya", null));
                switch (c[2]) {
                    case "tra" -> ps.add(new Paso("tra_cortar", "tra_cortar", null));
                    case "com" -> {
                        ps.add(new Paso("com_elegir", "com_elegir", null));
                        ps.add(new Paso("com_revelar", "com_revelar", null));
                    }
                    case "cie" -> ps.add(new Paso("cie_cortar", "cie_cortar", null));
                    case "sac" -> ps.add(new Paso("sac_votar", "sac_votar", null));
                    case "cop" -> ps.add(new Paso("cop_beber", "cop_beber", null));
                    default -> {
                    }
                }
            }
            ps.add(new Paso(c[2] + "_terminar", "terminar_prueba", null));
            l.add(new Capitulo(c[0], ps));
        }
        return l;
    }

    @Override
    protected void build() {
        List<Capitulo> caps = capitulos();
        capitulo = Math.max(0, Math.min(caps.size() - 1, capitulo));
        String[] nombres = new String[caps.size()];
        for (int i = 0; i < caps.size(); i++) {
            nombres[i] = tr("cap_corto", i + 1);
        }
        Tabs t = add(new Tabs(px + 12, py + 28, pw - 24, 15, i -> {
            capitulo = i;
            desde = 0;
            init();
        }, nombres));
        t.active = capitulo;
        Capitulo cap = caps.get(capitulo);
        int n = cap.pasos().size();
        desde = Math.max(0, Math.min(Math.max(0, n - VISIBLES), desde));
        // los pasos visibles, en fila
        int x0 = px + 30, y = py + 88;
        for (int i = desde; i < Math.min(n, desde + VISIBLES); i++) {
            add(new Nodo(x0 + (i - desde) * NODO, y, i + 1, cap, cap.pasos().get(i)));
        }
        Button izq = add(new Button(px + 10, y + 4, 16, 22, "◀", () -> {
            desde = Math.max(0, desde - 1);
            init();
        }));
        izq.enabled = desde > 0;
        Button der = add(new Button(px + pw - 26, y + 4, 16, 22, "▶", () -> {
            desde++;
            init();
        }));
        der.enabled = desde + VISIBLES < n;
        // abajo: lo de siempre
        int by = py + ph - 52, bw = (pw - 24 - 6 * 6) / 7, bx = px + 12;
        boolean pausa = "PAUSA".equals(EventoCliente.fase());
        add(new Button(bx, by, bw, 16, pausa ? tr("reanudar") : tr("pausar"), () -> pedir("pausa", null)))
                .help(Component.translatable(MOD + ".linea.pausar_ayuda"));
        add(new Button(bx + (bw + 6), by, bw, 16, "+1:00", () -> {
            CompoundTag d = new CompoundTag();
            d.putInt("n", 60);
            pedir("reloj_mas", d);
        })).help(Component.translatable(MOD + ".panel.mas_minuto"));
        add(new Button(bx + (bw + 6) * 2, by, bw, 16, EventoCliente.chatCerrado() ? tr("chat_abrir") : tr("chat_cerrar"), () -> pedir("chat", null)))
                .help(Component.translatable(MOD + ".linea.chat_ayuda"));
        add(new Button(bx + (bw + 6) * 3, by, bw, 16, tr("disfraz"), () -> pedir("disfraz", null)))
                .help(Component.translatable(MOD + ".panel.g_disfraz_ayuda"));
        boolean auto = EventoCliente.autoInscribir();
        add(new Button(bx + (bw + 6) * 4, by, bw, 16, auto ? tr("auto_si") : tr("auto_no"), () -> {
            CompoundTag d = new CompoundTag();
            d.putBoolean("si", !auto);
            pedir("auto_inscripcion", d);
        })).help(Component.translatable(MOD + ".linea.auto_ayuda"));
        boolean pocos = EventoCliente.pocos();
        add(new Button(bx + (bw + 6) * 5, by, bw, 16, pocos ? tr("pocos_si") : tr("pocos_no"), () -> {
            CompoundTag d = new CompoundTag();
            d.putBoolean("si", !pocos);
            pedir("pocos", d);
        })).help(Component.translatable(MOD + ".linea.pocos_ayuda"));
        add(new Button(bx + (bw + 6) * 6, by, bw, 16, tr("panel"), () -> com.axeliscore.plat.client.Screens.open(new PanelScreen())))
                .help(Component.translatable(MOD + ".linea.panel_ayuda"));
        int mitad = (pw - 24 - 6) / 2;
        add(new Button(bx, by + 22, mitad, 16, tr("sesion"), () -> {
            hechos.clear();
            init();
        })).help(Component.translatable(MOD + ".linea.sesion_ayuda"));
        // yo: organizador o pieza (sin salir del mundo)
        boolean soyPieza = EventoCliente.yo() != null;
        add(new Button(bx + mitad + 6, by + 22, mitad, 16, Component.translatable(MOD + ".panel." + (soyPieza ? "yo_organizador" : "yo_pieza")).getString(),
                () -> {
                    pedir(soyPieza ? "yo_organizador" : "yo_pieza", null);
                    com.axeliscore.plat.client.Screens.close();
                })).help(Component.translatable(MOD + ".panel." + (soyPieza ? "yo_organizador" : "yo_pieza") + "_ayuda"));
    }

    /** Para la prueba automática: dónde queda (en la pantalla) el paso visible número i (empezando en 0). */
    public int[] nodo(int i) {
        return new int[] {px + 30 + i * NODO + 11, py + 88 + 11};
    }

    /** Para la prueba automática: cambia de capítulo. */
    public void capitulo(int i) {
        capitulo = i;
        desde = 0;
        init();
    }

    private static void pedir(String op, CompoundTag extra) {
        CompoundTag t = Panel.pedido(op, null);
        if (extra != null) {
            t.merge(extra);
        }
        ClientData.send(Panel.ACCION, t);
    }

    @Override
    protected boolean onScroll(double x, double y, double delta) {
        if (y > py + 70 && y < py + 170) {
            desde = Math.max(0, desde + (delta < 0 ? 1 : -1));
            init();
            return true;
        }
        return super.onScroll(x, y, delta);
    }

    @Override
    protected void content(G g, int mx, int my, float pt) {
        Font f = Ui.font();
        Capitulo cap = capitulos().get(capitulo);
        // el título del capítulo y su resumen
        g.drawString(f, Component.translatable(MOD + ".linea." + cap.clave() + ".titulo").withStyle(ChatFormatting.BOLD), px + 14, py + 50, 0xFFE8D8C0);
        Ui.textoQueCabe(g, tr(cap.clave() + ".resumen"), px + 14, py + 60, pw - 28, 12, 0xFFA89E94, false, false);
        // la línea que une los pasos (y se sigue a los lados si hay más)
        int y = py + 88 + 11, n = cap.pasos().size();
        int xa = px + 30 + 11, xb = px + 30 + (Math.min(n, desde + VISIBLES) - desde - 1) * NODO + 11;
        g.fill(desde > 0 ? px + 28 : xa, y - 1, desde + VISIBLES < n ? px + pw - 28 : xb, y + 1, 0xFF6A2A30);
        // en qué va el evento
        String est = tr("estado", EventoCliente.prueba().isEmpty() ? "—" : EventoCliente.prueba(), SentidosCliente.fase());
        Ui.textoQueCabe(g, est, px + 14, py + ph - 72, pw - 28, 12, 0xFF8FA0A8, false, false);
    }

    /** Un paso de la línea: un círculo con su número y el nombre corto debajo (dos renglones); con clic se hace. */
    static final class Nodo extends Widget {
        final int numero;
        final Capitulo cap;
        final Paso paso;
        boolean apretado;

        Nodo(int x, int y, int numero, Capitulo cap, Paso paso) {
            super(x, y - 2, NODO - 6, 70);
            this.numero = numero;
            this.cap = cap;
            this.paso = paso;
        }

        private String llave() {
            return cap.clave() + "." + numero;
        }

        @Override
        protected void draw(G g, int mx, int my, Theme t) {
            Font f = Ui.font();
            boolean hecho = hechos.contains(llave());
            int cx = x + 11, cy = y + 13;
            int borde = hover > 0.5F ? 0xFFF0C060 : hecho ? 0xFF5FA060 : 0xFFB0303A;
            int fondo = hecho ? 0xFF203A22 : apretado ? 0xFF501820 : 0xFF2A1014;
            Ui.round(g, cx - 11, cy - 11, 22, 22, borde);
            Ui.round(g, cx - 9, cy - 9, 18, 18, fondo);
            String num = hecho ? "✔" : String.valueOf(numero);
            g.drawCenteredString(f, num, cx, cy - 4, hecho ? 0xFF9FE0A0 : 0xFFF0E0D0);
            // el nombre corto, centrado y en hasta tres renglones (si no cabe, la letra se achica)
            String nom = tr("p." + paso.clave());
            int ancho = NODO - 6, col = hover > 0.5F ? 0xFFFFE8B0 : 0xFFD8CCC0;
            float k = 1F;
            List<String> ls = Ui.wrap(nom, ancho);
            while (k > 0.65F && !cabe(f, ls, k, ancho)) {
                k -= 0.1F;
                ls = Ui.wrap(nom, (int) (ancho / k));
            }
            if (k < 1F) {
                Ui.APRETADOS.add(nom);
            }
            int mid = cx + ancho / 2 - 11;
            for (int i = 0; i < ls.size() && i < 3; i++) {
                g.push();
                g.translate(mid, cy + 15 + i * 10 * k, 0);
                g.scale(k, k, 1F);
                String l = ls.get(i);
                if (f.width(l) * k > ancho) {
                    l = Ui.fit(l, (int) (ancho / k));
                }
                g.drawString(f, l, -f.width(l) / 2, 0, col, true);
                g.pop();
            }
        }

        private static boolean cabe(Font f, List<String> ls, float k, int ancho) {
            if (ls.size() > 3) {
                return false;
            }
            for (String l : ls) {
                if (f.width(l) * k > ancho) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public boolean click(double mx, double my, int button) {
            if (button == 0 && inside(mx, my)) {
                apretado = true;
                return true;
            }
            return false;
        }

        @Override
        public boolean release(double mx, double my, int button) {
            boolean era = apretado;
            apretado = false;
            if (era && inside(mx, my)) {
                Ui.click(1F);
                pedir(paso.op(), paso.extra());
                hechos.add(llave());
                return true;
            }
            return false;
        }

        @Override
        public InfoCard card(double mx, double my) {
            return new InfoCard(Component.literal(numero + " · " + tr("p." + paso.clave()))).color(0xFFB0303A).width(220)
                    .text(Component.translatable(MOD + ".linea.p." + paso.clave() + ".ayuda"))
                    .hint(Component.translatable(MOD + ".linea.clic"));
        }
    }
}
