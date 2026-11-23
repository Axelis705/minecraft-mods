package com.coleccionista.client;

import com.axeliscore.data.client.ClientData;
import com.axeliscore.plat.Ids;
import com.axeliscore.plat.Nbt;
import com.axeliscore.plat.client.G;
import com.axeliscore.ui.Button;
import com.axeliscore.ui.ListView;
import com.axeliscore.ui.Slider;
import com.axeliscore.ui.Tabs;
import com.axeliscore.ui.Theme;
import com.axeliscore.ui.Toggle;
import com.axeliscore.ui.Ui;
import com.axeliscore.ui.UiScreen;
import com.coleccionista.Coleccionista;
import com.coleccionista.evento.Panel;
import com.coleccionista.evento.Tomas;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * (Cliente) El panel del organizador (tecla O, solo operadores u organizadores):
 * <ul>
 *     <li>Piezas: la lista de todos (cara, número, fichas, equipo, si es fantasma) y lo que se le puede hacer a cada uno.</li>
 *     <li>Evento: parejas y equipos al azar, el reloj, pausa, anunciar y reiniciar.</li>
 *     <li>Cámaras: tomas guardadas para transmitir (ir, viajar suave, recorrido), seguir a una pieza, esconder el HUD y los fantasmas.</li>
 * </ul>
 */
public class PanelScreen extends UiScreen {
    private static final String MOD = Coleccionista.MODID;
    public static final Theme TEMA = Theme.of(0xFF1E0E12, 0xFFE0404A).withFondo(Ids.core("textures/gui/fondo_piedra.png"), 0.62F, 0)
            .withFrame(Ids.core("textures/gui/marco_piedra.png"));
    private static int tab;
    private static UUID elegida;
    private static int tomaElegida = -1;
    private static long confirmarReinicio;
    private int hash;

    public PanelScreen() {
        super(Component.translatable(MOD + ".panel.titulo"), 400, 244, TEMA);
    }

    /** Para las pruebas. */
    public void pestana(int i) {
        tab = i;
        init();
    }

    private static String tr(String k, Object... a) {
        return Component.translatable(MOD + ".panel." + k, a).getString();
    }

    private static void pedir(String op, UUID id, CompoundTag extra) {
        CompoundTag t = Panel.pedido(op, id);
        if (extra != null) {
            t.merge(extra);
        }
        ClientData.send(Panel.ACCION, t);
    }

    private static void pedir(String op, UUID id) {
        pedir(op, id, null);
    }

    private static CompoundTag n(String k, int v) {
        CompoundTag t = new CompoundTag();
        t.putInt(k, v);
        return t;
    }

    private static int hashDatos() {
        CompoundTag e = ClientData.get(com.coleccionista.evento.Evento.KEY), t = ClientData.get(Tomas.KEY);
        // de Encadenados solo importa la fase (el gas cambia a cada rato)
        return ((e == null ? 0 : e.hashCode()) * 31 + (t == null ? 0 : t.hashCode()) + tab) * 31 + EncadenadosCliente.fase.hashCode() + CompartirCliente.fase().hashCode() * 7
                + PuertasCliente.fase().hashCode() * 13 + PuertasCliente.ronda() + pruebaSel * 101 + CopasCliente.fase().hashCode() * 17
                + SacrificioCliente.fase().hashCode() * 19;
    }

    @Override
    public void tick() {
        super.tick();
        // si cambió algo en el servidor (alguien eliminado, una toma nueva), se vuelve a armar
        if (hashDatos() != hash) {
            init();
        }
    }

    @Override
    protected void build() {
        hash = hashDatos();
        Tabs t = add(new Tabs(px + 12, py + 30, pw - 24, 15, i -> {
            tab = i;
            init();
        }, tr("tab_piezas"), tr("tab_evento"), tr("tab_pruebas"), tr("tab_camaras")));
        t.active = tab;
        if (tab == 0) {
            piezas();
        } else if (tab == 1) {
            evento();
        } else if (tab == 2) {
            pruebas();
        } else {
            camaras();
        }
    }

    // ------------------------------------------------------------------ piezas

    private void piezas() {
        int y0 = py + 52;
        List<CompoundTag> ps = EventoCliente.piezas();
        ListView<CompoundTag> lista = add(new ListView<>(px + 12, y0, 196, py + ph - 12 - y0 - 22, 20, this::fila, p -> {
            elegida = p.getUUID("id");
            init();
        }));
        lista.items.addAll(ps);
        for (int i = 0; i < ps.size(); i++) {
            if (ps.get(i).getUUID("id").equals(elegida)) {
                lista.selected = i;
            }
        }
        int by = py + ph - 30;
        add(new Button(px + 12, by, 96, 16, tr("inscribir_todos"), () -> pedir("inscribir_todos", null)))
                .help(Component.translatable(MOD + ".panel.inscribir_todos_ayuda"));
        add(new Button(px + 112, by, 96, 16, tr("yo_organizador"), () -> pedir("yo_organizador", null)))
                .help(Component.translatable(MOD + ".panel.yo_organizador_ayuda"));
        CompoundTag p = EventoCliente.pieza(elegida);
        if (p == null) {
            return;
        }
        UUID id = elegida;
        boolean vivo = EventoCliente.vivo(p);
        int x = px + 218, w = (pw - 230 - 6) / 2, y = py + 112;
        Button b1 = add(new Button(x, y, w, 16, tr("eliminar"), () -> pedir("eliminar", id)));
        b1.enabled = vivo;
        b1.help(Component.translatable(MOD + ".panel.eliminar_ayuda"));
        Button b3 = add(new Button(x + w + 6, y, w, 16, tr("revivir"), () -> pedir("revivir", id)));
        b3.enabled = !vivo;
        b3.help(Component.translatable(MOD + ".panel.revivir_ayuda"));
        y += 20;
        add(new Button(x, y, w, 16, tr("ir"), () -> {
            pedir("ir", id);
            onClose();
        }));
        add(new Button(x + w + 6, y, w, 16, tr("seguir"), () -> {
            Camara.seguir(id);
            onClose();
        })).help(Component.translatable(MOD + ".panel.seguir_ayuda"));
        y += 20;
        Button q = add(new Button(x, y, w * 2 + 6, 16, tr("quitar"), () -> pedir("quitar", id)));
        q.help(Component.translatable(MOD + ".panel.quitar_ayuda"));
    }

    private void fila(G g, CompoundTag p, int x, int y, int w, int h, boolean sel, boolean hov, Theme t) {
        Font f = Ui.font();
        boolean vivo = EventoCliente.vivo(p);
        UUID id = p.getUUID("id");
        PlayerFaceRenderer.draw(g.raw, EventoCliente.piel(id), x + 2, y + 2, 16);
        if (!vivo) {
            g.fill(x + 2, y + 2, x + 18, y + 18, 0x9030607A);
        }
        g.drawString(f, String.format("%02d", Nbt.i(p, "numero")), x + 22, y + 6, vivo ? EventoHud.ROJO_CL : 0xFF607880, false);
        String nom = Nbt.s(p, "nombre");
        g.drawString(f, Ui.fit(nom, w - 92), x + 38, y + 6, vivo ? (sel ? t.title : t.text) : 0xFF8090A0, false);
        int rx = x + w - 4;
        if (!vivo) {
            String c = "☠ " + Nbt.i(p, "orden");
            g.drawString(f, c, rx - f.width(c), y + 6, 0xFF8A3A40, false);
            return;
        }
        if (p.getInt("equipo") >= 0) {
            String e = "E" + (p.getInt("equipo") + 1);
            g.drawString(f, e, rx - f.width(e), y + 6, 0xFF8FD8E8, false);
        }
    }

    // ------------------------------------------------------------------ evento

    private void evento() {
        int x = px + 16, y = py + 64, w = pw - 32;
        int bw = (w - 4 * 6) / 5;
        add(new Button(x, y, bw, 16, tr("parejas"), () -> pedir("parejas", null)));
        for (int i = 0; i < 3; i++) {
            int tam = 3 + i;
            add(new Button(x + (bw + 6) * (i + 1), y, bw, 16, tr("equipos_de", tam), () -> pedir("equipos", null, n("n", tam))));
        }
        add(new Button(x + (bw + 6) * 4, y, bw, 16, tr("sin_equipos"), () -> pedir("sin_equipos", null)));
        y += 36;
        int[] mins = {5, 10, 15, 20};
        for (int i = 0; i < mins.length; i++) {
            int m = mins[i];
            add(new Button(x + (bw + 6) * i, y, bw, 16, m + ":00", () -> pedir("reloj", null, n("n", m * 60))));
        }
        add(new Button(x + (bw + 6) * 4, y, (bw - 6) / 2, 16, "+1", () -> pedir("reloj_mas", null, n("n", 60))))
                .help(Component.translatable(MOD + ".panel.mas_minuto"));
        add(new Button(x + (bw + 6) * 4 + (bw + 6) / 2, y, (bw - 6) / 2, 16, "■", () -> pedir("reloj", null, n("n", 0))))
                .help(Component.translatable(MOD + ".panel.parar_reloj"));
        y += 36;
        boolean pausa = "PAUSA".equals(EventoCliente.fase());
        int cw = (w - 2 * 6) / 3;
        add(new Button(x, y, cw, 18, pausa ? tr("reanudar") : tr("pausar"), () -> pedir("pausa", null)));
        add(new Button(x + cw + 6, y, cw, 18, tr("anunciar"), () -> Minecraft.getInstance().setScreen(new ChatScreen("/evento anunciar "))))
                .help(Component.translatable(MOD + ".panel.anunciar_ayuda"));
        boolean armado = System.currentTimeMillis() - confirmarReinicio < 4000;
        add(new Button(x + (cw + 6) * 2, y, cw, 18, armado ? tr("reiniciar_seguro") : tr("reiniciar"), () -> {
            if (System.currentTimeMillis() - confirmarReinicio < 4000) {
                pedir("reiniciar", null);
                confirmarReinicio = 0;
            } else {
                confirmarReinicio = System.currentTimeMillis();
            }
            init();
        })).help(Component.translatable(MOD + ".panel.reiniciar_ayuda"));
        // el guion del inicio: sala de espera → ¡pum! → discurso → abrir celdas; y aparecer como el Coleccionista
        y += 36;
        String[][] guion = {{"sala", "g_sala"}, {"despertar", "g_despertar"}, {"discurso", "g_discurso"}, {"abrir_celdas", "g_abrir"},
                {"disfraz", "g_disfraz"}};
        for (int i = 0; i < guion.length; i++) {
            String op = guion[i][0];
            add(new Button(x + (bw + 6) * i, y, bw, 16, tr(guion[i][1]), () -> pedir(op, null)))
                    .help(Component.translatable(MOD + ".panel." + guion[i][1] + "_ayuda"));
        }
    }

    // ------------------------------------------------------------------ pruebas

    /** Las pruebas del panel: número, clave del nombre y sus botones (cada botón: acción y si se puede usar ahora). */
    private static int pruebaSel;

    private record Op(String id, java.util.function.BooleanSupplier on) {
    }

    private static String[] pruebasNombres() {
        return new String[] {"1 · " + tr("p_encadenados"), "3 · " + tr("p_compartir"), "5 · " + tr("p_sacrificio"), "6 · " + tr("p_puertas"),
                "7 · " + tr("p_copas")};
    }

    private static List<Op> ops(int i) {
        List<Op> l = new ArrayList<>();
        if (i == 0) {
            String f = EncadenadosCliente.fase;
            l.add(new Op("enc_preparar", () -> !"JUEGO".equals(f) && !"HABLANDO".equals(f) && !"CIERRE".equals(f)));
            l.add(new Op("enc_empezar", () -> "LISTA".equals(f)));
            l.add(new Op("enc_ya", () -> "LISTA".equals(f)));
            l.add(new Op("enc_soltar", () -> EncadenadosCliente.enJuego() && !EncadenadosCliente.grilletes.isEmpty()));
            l.add(new Op("enc_cerrar", () -> "JUEGO".equals(f)));
            l.add(new Op("enc_terminar", EncadenadosCliente::activa));
        } else if (i == 1) {
            String c = CompartirCliente.fase();
            l.add(new Op("com_preparar", () -> "NADA".equals(c) || "LISTA".equals(c) || "FIN".equals(c)));
            l.add(new Op("com_empezar", () -> "LISTA".equals(c)));
            l.add(new Op("com_ya", () -> "LISTA".equals(c)));
            l.add(new Op("com_elegir", () -> "HABLAR".equals(c) || "LISTA".equals(c)));
            l.add(new Op("com_revelar", () -> "ELEGIR".equals(c)));
            l.add(new Op("com_terminar", CompartirCliente::activa));
        } else if (i == 2) {
            String f = SacrificioCliente.fase();
            l.add(new Op("sac_preparar", () -> "NADA".equals(f) || "LISTA".equals(f) || "FIN".equals(f)));
            l.add(new Op("sac_empezar", () -> "LISTA".equals(f)));
            l.add(new Op("sac_ya", () -> "LISTA".equals(f)));
            l.add(new Op("sac_votar", () -> "OFRECER".equals(f)));
            l.add(new Op("sac_terminar", SacrificioCliente::activa));
        } else if (i == 4) {
            String f = CopasCliente.fase();
            l.add(new Op("cop_preparar", () -> "NADA".equals(f) || "LISTA".equals(f) || "FIN".equals(f)));
            l.add(new Op("cop_empezar", () -> "LISTA".equals(f)));
            l.add(new Op("cop_ya", () -> "LISTA".equals(f)));
            l.add(new Op("cop_beber", () -> "TURNO".equals(f)));
            l.add(new Op("cop_terminar", CopasCliente::activa));
        } else {
            String f = PuertasCliente.fase();
            l.add(new Op("pue_preparar", () -> "NADA".equals(f) || "LISTA".equals(f) || "FIN".equals(f)));
            l.add(new Op("pue_ronda", () -> "LISTA".equals(f) || "ENTRE".equals(f)));
            l.add(new Op("pue_cerrar", () -> "ELEGIR".equals(f)));
            l.add(new Op("pue_terminar", PuertasCliente::activa));
        }
        return l;
    }

    private static String estadoPrueba(int i) {
        if (i == 0) {
            String est = Component.translatable(MOD + ".panel.enc_estado." + EncadenadosCliente.fase.toLowerCase(Locale.ROOT)).getString();
            if (EncadenadosCliente.activa()) {
                est += " · " + tr("enc_cuenta", EncadenadosCliente.salvados, EncadenadosCliente.cupo, EncadenadosCliente.equipos,
                        EncadenadosCliente.grilletes.size());
            }
            return est;
        }
        if (i == 1) {
            return Component.translatable(MOD + ".panel.com_estado." + CompartirCliente.fase().toLowerCase(Locale.ROOT)).getString();
        }
        if (i == 4) {
            return Component.translatable(MOD + ".panel.cop_estado." + CopasCliente.fase().toLowerCase(Locale.ROOT)).getString();
        }
        if (i == 2) {
            return Component.translatable(MOD + ".panel.sac_estado." + SacrificioCliente.fase().toLowerCase(Locale.ROOT)).getString();
        }
        return Component.translatable(MOD + ".panel.pue_estado." + PuertasCliente.fase().toLowerCase(Locale.ROOT), PuertasCliente.ronda(),
                PuertasCliente.rondas()).getString();
    }

    private void pruebas() {
        int y0 = py + 52;
        ListView<String> lista = add(new ListView<>(px + 12, y0, 132, py + ph - 12 - y0, 18, (g, s, x, y, w, h, sel, hov, t) ->
                g.drawString(Ui.font(), Ui.fit(s, w - 8), x + 4, y + 5, sel ? t.title : t.text, false), s -> {
                    String[] ns = pruebasNombres();
                    for (int i = 0; i < ns.length; i++) {
                        if (ns[i].equals(s)) {
                            pruebaSel = i;
                        }
                    }
                    init();
                }));
        lista.items.addAll(List.of(pruebasNombres()));
        lista.selected = pruebaSel;
        int x = px + 152, w = pw - 164, bw = (w - 6) / 2;
        List<Op> os = ops(pruebaSel);
        for (int i = 0; i < os.size(); i++) {
            Op o = os.get(i);
            Button b = add(new Button(x + (i % 2) * (bw + 6), py + 80 + (i / 2) * 20, bw, 16, tr(o.id()), () -> pedir(o.id(), null)));
            b.enabled = o.on().getAsBoolean();
            b.help(Component.translatable(MOD + ".panel." + o.id() + "_ayuda"));
        }
    }

    // ------------------------------------------------------------------ cámaras

    private static List<CompoundTag> tomas() {
        CompoundTag d = ClientData.get(Tomas.KEY);
        List<CompoundTag> r = new ArrayList<>();
        if (d != null) {
            for (Tag x : d.getList("lista", Tag.TAG_COMPOUND)) {
                r.add((CompoundTag) x);
            }
        }
        return r;
    }

    private static double[] punto(CompoundTag t) {
        return new double[] {Nbt.d(t, "x"), Nbt.d(t, "y"), Nbt.d(t, "z"), t.getFloat("yaw"), t.getFloat("pitch")};
    }

    private void camaras() {
        int y0 = py + 52;
        List<CompoundTag> ts = tomas();
        ListView<Integer> lista = add(new ListView<>(px + 12, y0, 170, py + ph - 12 - y0 - 22, 14, (g, i, x, y, w, h, sel, hov, t) -> {
            CompoundTag k = ts.get(i);
            g.drawString(Ui.font(), (i + 1) + ". " + Ui.fit(Nbt.s(k, "nombre"), w - 24), x + 3, y + 3, sel ? t.title : t.text, false);
        }, i -> {
            tomaElegida = i;
            init();
        }));
        for (int i = 0; i < ts.size(); i++) {
            lista.items.add(i);
        }
        lista.selected = tomaElegida < ts.size() ? tomaElegida : -1;
        int by = py + ph - 30, bw = (170 - 18) / 4;
        add(new Button(px + 12, by, bw + 22, 16, tr("guardar_toma"), () -> pedir("toma_guardar", null)))
                .help(Component.translatable(MOD + ".panel.guardar_toma_ayuda"));
        boolean hay = tomaElegida >= 0 && tomaElegida < ts.size();
        int ti = tomaElegida;
        Button bb = add(new Button(px + 12 + bw + 28, by, bw - 4, 16, "✕", () -> {
            pedir("toma_borrar", null, n("n", ti));
            tomaElegida = -1;
        }));
        bb.enabled = hay;
        bb.help(Component.translatable(MOD + ".panel.borrar_toma"));
        CompoundTag sube = n("n", ti);
        sube.putInt("d", -1);
        CompoundTag baja = n("n", ti);
        baja.putInt("d", 1);
        Button up = add(new Button(px + 12 + (bw + 6) * 2 + 18, by, bw - 12, 16, "↑", () -> {
            pedir("toma_mover", null, sube);
            tomaElegida = Math.max(0, ti - 1);
        }));
        up.enabled = hay && ti > 0;
        Button down = add(new Button(px + 12 + (bw + 6) * 3 + 12, by, bw - 12, 16, "↓", () -> {
            pedir("toma_mover", null, baja);
            tomaElegida = Math.min(ts.size() - 1, ti + 1);
        }));
        down.enabled = hay && ti < ts.size() - 1;
        int x = px + 194, w = pw - 206, y = y0;
        int hw = (w - 6) / 2;
        Button ir = add(new Button(x, y, hw, 16, tr("ir_toma"), () -> {
            double[] q = punto(ts.get(ti));
            Camara.parar();
            Camara.viajar(q[0], q[1], q[2], (float) q[3], (float) q[4]);
            Camara.segundosPorTramo = Math.max(0.6F, Camara.segundosPorTramo);
            onClose();
        }));
        ir.enabled = hay;
        ir.help(Component.translatable(MOD + ".panel.viajar_ayuda"));
        Button rec = add(new Button(x + hw + 6, y, hw, 16, tr("recorrido"), () -> {
            List<double[]> pts = new ArrayList<>();
            for (CompoundTag k : ts) {
                pts.add(punto(k));
            }
            Camara.recorrido(pts);
            onClose();
        }));
        rec.enabled = ts.size() >= 2;
        rec.help(Component.translatable(MOD + ".panel.recorrido_ayuda"));
        y += 24;
        add(new Slider(x, y, w, tr("segundos"), 2F, 15F, 0.5F, Camara.segundosPorTramo, v -> String.format(Locale.ROOT, "%.1f s", v),
                v -> Camara.segundosPorTramo = v));
        y += 28;
        add(new Toggle(x, y, w, tr("ocultar_hud"), EventoHud.oculto, v -> EventoHud.oculto = v));
        y += 18;
        add(new Toggle(x, y, w, tr("ver_fantasmas"), Camara.verFantasmas, v -> Camara.verFantasmas = v));
        y += 22;
        add(new Button(x, y, w, 16, tr("espectador"), () -> Minecraft.getInstance().player.connection.sendCommand("gamemode spectator")))
                .help(Component.translatable(MOD + ".panel.espectador_ayuda"));
    }

    @Override
    protected void content(G g, int mx, int my, float pt) {
        Font f = Ui.font();
        Ui.title(g, title.getString(), px + pw / 2, py + 12, theme);
        if (tab == 0) {
            CompoundTag p = EventoCliente.pieza(elegida);
            int x = px + 218, y = py + 54;
            if (p == null) {
                g.drawString(f, tr("elige"), x, y + 6, theme.textDim, false);
                if (EventoCliente.piezas().isEmpty()) {
                    Ui.text(g, Component.translatable(MOD + ".panel.vacio").getString(), x, y + 22, pw - 230, theme.textDim, 10);
                }
                return;
            }
            boolean vivo = EventoCliente.vivo(p);
            g.fill(x - 2, y - 2, x + 34, y + 34, vivo ? EventoHud.PORCELANA : 0xFF5A8090);
            PlayerFaceRenderer.draw(g.raw, EventoCliente.piel(p.getUUID("id")), x, y, 32);
            g.drawString(f, "PIEZA " + String.format("%02d", Nbt.i(p, "numero")), x + 40, y, EventoHud.ROJO_CL, true);
            g.drawString(f, Nbt.s(p, "nombre") + (p.getBoolean("bot") ? " (bot)" : ""), x + 40, y + 11, theme.title, true);
            String estado = vivo ? tr("vivo") : tr("fantasma", Nbt.i(p, "orden"));
            g.drawString(f, estado, x + 40, y + 22, vivo ? 0xFF8EE07A : 0xFF8FD8E8, false);
            if (p.getInt("equipo") >= 0) {
                g.drawString(f, tr("equipo", p.getInt("equipo") + 1), x, y + 40, theme.textDim, false);
            }
        } else if (tab == 1) {
            int x = px + 16;
            g.drawString(f, tr("sec_equipos"), x, py + 54, theme.accent, false);
            int r = EventoCliente.resta();
            g.drawString(f, tr("sec_reloj") + (r >= 0 ? "  " + String.format("%d:%02d", r / 60, r % 60) : ""), x, py + 90, theme.accent, false);
            g.drawString(f, tr("sec_control"), x, py + 126, theme.accent, false);
            g.drawString(f, tr("sec_guion"), x, py + 162, theme.accent, false);
            String pr = EventoCliente.prueba();
            Ui.text(g, Component.translatable(MOD + ".panel.estado", EventoCliente.fase(), pr.isEmpty() ? "-" : pr, EventoCliente.vivos(),
                    EventoCliente.total()).getString(), x, py + ph - 22, pw - 32, theme.textDim, 10);
        } else if (tab == 2) {
            int x = px + 152, w = pw - 164;
            g.drawString(f, pruebasNombres()[pruebaSel], x, py + 54, theme.accent, false);
            g.drawString(f, Ui.fit(estadoPrueba(pruebaSel), w), x, py + 66, theme.title, false);
            String[] notas = {"enc_nota", "com_nota", "sac_nota", "pue_nota", "cop_nota"};
            int filas = (ops(pruebaSel).size() + 1) / 2;
            Ui.text(g, Component.translatable(MOD + ".panel." + notas[pruebaSel]).getString(), x, py + 86 + filas * 20, w, theme.textDim, 10);
            Ui.text(g, Component.translatable(MOD + ".panel.pruebas_proximas").getString(), x, py + ph - 24, w, 0xFF7A6E68, 10);
        } else {
            int x = px + 194;
            Ui.text(g, Component.translatable(MOD + ".panel.camara_nota").getString(), x, py + ph - 52, pw - 206, theme.textDim, 10);
            if (tomas().isEmpty()) {
                Ui.text(g, Component.translatable(MOD + ".panel.sin_tomas").getString(), px + 16, py + 60, 160, theme.textDim, 10);
            }
        }
    }
}
