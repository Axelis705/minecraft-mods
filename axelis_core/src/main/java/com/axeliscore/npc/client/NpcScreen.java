package com.axeliscore.npc.client;

import com.axeliscore.data.client.ClientData;
import com.axeliscore.npc.Mision;
import com.axeliscore.npc.Misiones;
import com.axeliscore.npc.NpcAcciones;
import com.axeliscore.npc.NpcDef;
import com.axeliscore.npc.Objetivo;
import com.axeliscore.npc.Tienda;
import com.axeliscore.plat.Nbt;
import com.axeliscore.plat.client.G;
import com.axeliscore.ui.Button;
import com.axeliscore.ui.InfoCard;
import com.axeliscore.ui.ListView;
import com.axeliscore.ui.Tabs;
import com.axeliscore.ui.Theme;
import com.axeliscore.ui.Ui;
import com.axeliscore.ui.UiScreen;
import com.axeliscore.ui.Widget;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * (Cliente) La ventana de un NPC: hablar (su saludo), sus misiones (aceptar y entregar), su tienda (comprar y vender) y la amistad
 * (niveles y regalos). Se abre al hacerle clic; los colores son los del {@link NpcDef#tema}.
 */
public class NpcScreen extends UiScreen {
    private static final int HABLAR = 0, MISIONES = 1, TIENDA = 2, AMISTAD = 3;

    private final NpcDef def;
    private final int ent;
    private final String saludo;
    private final int[] pestanas;
    private int tab;
    private String elegida = "";
    private int oferta = -1;
    private CompoundTag visto;
    private float mostrado = -1;

    public NpcScreen() {
        this(ClientData.get(NpcAcciones.ABIERTO));
    }

    private NpcScreen(CompoundTag t) {
        this(NpcDef.porId(Nbt.s(t, "npc")), Nbt.i(t, "ent"), Nbt.s(t, "saludo"));
    }

    private NpcScreen(NpcDef def, int ent, String saludo) {
        super(def == null ? Component.literal("?") : def.nombre(), 330, 222, def == null ? Theme.PERGAMINO : def.tema());
        this.def = def;
        this.ent = ent;
        this.saludo = saludo;
        List<Integer> l = new ArrayList<>(List.of(HABLAR, MISIONES));
        if (def != null && def.tienda() != null) {
            l.add(TIENDA);
        }
        l.add(AMISTAD);
        this.pestanas = l.stream().mapToInt(Integer::intValue).toArray();
        // si tiene algo para entregar o una misión nueva, abre en misiones
        if (def != null && NpcCliente.marca(def) > 0) {
            tab = MISIONES;
        }
    }

    /** Para las pruebas: cambiar de pestaña (0 hablar, 1 misiones, 2 tienda, 3 amistad). */
    public void mostrar(int t) {
        tab = t;
        init();
    }

    /** Para las pruebas: elegir una misión o una oferta. */
    public void elegir(String mision, int oferta) {
        this.elegida = mision;
        this.oferta = oferta;
        init();
    }

    @Override
    protected void build() {
        if (def == null) {
            return;
        }
        String[] nombres = new String[pestanas.length];
        int activa = 0;
        for (int i = 0; i < pestanas.length; i++) {
            nombres[i] = Component.translatable("axelis_core.npc.tab" + pestanas[i]).getString();
            if (pestanas[i] == tab) {
                activa = i;
            }
        }
        Tabs t = add(new Tabs(px + 10, py + 58, pw - 20, 16, i -> {
            tab = pestanas[i];
            rehacer();
        }, nombres));
        t.active = activa;
        rehacer();
    }

    private void rehacer() {
        widgets.removeIf(w -> !(w instanceof Tabs));
        visto = NpcCliente.yo();
        int x = px + 12, y = py + 80, iw = pw - 24;
        switch (tab) {
            case MISIONES -> misiones(x, y, iw);
            case TIENDA -> tienda(x, y, iw);
            case AMISTAD -> {
            }
            default -> {
                int bw = (iw - 12) / 3;
                long nuevas = def.misiones().stream().filter(m -> {
                    Misiones.Estado e = NpcCliente.estado(m);
                    return e == Misiones.Estado.DISPONIBLE || e == Misiones.Estado.LISTA;
                }).count();
                add(new Button(x, py + ph - 30, bw, 20, Component.translatable("axelis_core.npc.tab1").getString() + (nuevas > 0 ? " (" + nuevas
                        + ")" : ""), () -> irA(MISIONES)));
                Button ti = add(new Button(x + bw + 6, py + ph - 30, bw, 20, Component.translatable("axelis_core.npc.tab2").getString(),
                        () -> irA(TIENDA)));
                ti.enabled = def.tienda() != null;
                add(new Button(x + 2 * (bw + 6), py + ph - 30, bw, 20, Component.translatable("axelis_core.npc.adios").getString(), this::onClose));
            }
        }
    }

    private void irA(int t) {
        tab = t;
        init();
    }

    // ------------------------------------------------------------------ misiones

    private List<Mision> listaMisiones() {
        List<Mision> l = new ArrayList<>(def.misiones());
        l.sort((a, b) -> Integer.compare(orden(NpcCliente.estado(a)), orden(NpcCliente.estado(b))));
        return l;
    }

    private static int orden(Misiones.Estado e) {
        return switch (e) {
            case LISTA -> 0;
            case DISPONIBLE -> 1;
            case ACTIVA -> 2;
            case BLOQUEADA -> 3;
            case HECHA -> 4;
        };
    }

    static String marcaDe(Misiones.Estado e) {
        return switch (e) {
            case LISTA -> "?";
            case DISPONIBLE -> "!";
            case ACTIVA -> "…";
            case BLOQUEADA -> "✗";
            case HECHA -> "✔";
        };
    }

    static int colorDe(Misiones.Estado e, Theme t) {
        return switch (e) {
            case LISTA -> 0xFF4CC86A;
            case DISPONIBLE -> 0xFFE0A020;
            case ACTIVA -> t.accent2;
            default -> t.textDim;
        };
    }

    private void misiones(int x, int y, int iw) {
        List<Mision> l = listaMisiones();
        if (l.isEmpty()) {
            return;
        }
        if (elegida.isEmpty() || l.stream().noneMatch(m -> m.id.equals(elegida))) {
            elegida = l.get(0).id;
        }
        int lw = 124;
        ListView<Mision> lv = add(new ListView<>(x, y, lw, ph - 80 - 10, 18, (g, m, rx, ry, rw, rh, sel, hov, th) -> {
            Misiones.Estado e = NpcCliente.estado(m);
            String mk = marcaDe(e);
            g.drawString(Ui.font(), mk, rx + 1, ry + 5, colorDe(e, th), false);
            String n = e == Misiones.Estado.BLOQUEADA ? "???" : m.titulo().getString();
            g.drawString(Ui.font(), Ui.fit(n, rw - 14), rx + 12, ry + 5, e == Misiones.Estado.HECHA || e == Misiones.Estado.BLOQUEADA ? th.textDim
                    : sel ? th.title : th.text, false);
        }, m -> {
            elegida = m.id;
            rehacer();
        }));
        lv.items.addAll(l);
        for (int i = 0; i < l.size(); i++) {
            if (l.get(i).id.equals(elegida)) {
                lv.selected = i;
            }
        }
        Mision m = Misiones.porId(elegida);
        if (m == null) {
            return;
        }
        Misiones.Estado e = NpcCliente.estado(m);
        int dx = x + lw + 8, dw = iw - lw - 8, by = py + ph - 30;
        CompoundTag d = new CompoundTag();
        d.putString("npc", def.id);
        d.putInt("ent", ent);
        d.putString("mision", m.id);
        switch (e) {
            case DISPONIBLE -> add(new Button(dx, by, dw, 20, Component.translatable("axelis_core.mision.aceptar").getString(),
                    () -> ClientData.send(NpcAcciones.ACEPTAR, d)));
            case LISTA -> add(new Button(dx, by, dw, 20, Component.translatable("axelis_core.mision.entregar").getString(),
                    () -> ClientData.send(NpcAcciones.ENTREGAR, d)));
            case ACTIVA -> {
                int bw = (dw - 6) / 2;
                Button b = add(new Button(dx, by, bw, 20, Component.translatable("axelis_core.mision.en_curso").getString(), null));
                b.enabled = false;
                add(new Button(dx + bw + 6, by, bw, 20, Component.translatable("axelis_core.mision.abandonar").getString(),
                        () -> ClientData.send(NpcAcciones.ABANDONAR, d)));
            }
            default -> {
            }
        }
    }

    private void detalleMision(G g, int x, int y, int w) {
        Mision m = Misiones.porId(elegida);
        if (m == null) {
            Ui.text(g, Component.translatable("axelis_core.npc.sin_misiones").getString(), x, y + 4, w, theme.textDim, 10);
            return;
        }
        detalle(g, m, x, y, w, ph - 80 - 40, theme);
    }

    /** Dibuja título, descripción, objetivos y premios de una misión (lo usa también el diario). */
    static void detalle(G g, Mision m, int x, int y, int w, int alto, Theme theme) {
        var f = Ui.font();
        Misiones.Estado e = NpcCliente.estado(m);
        if (e == Misiones.Estado.BLOQUEADA) {
            g.drawString(f, "???", x, y, theme.textDim, false);
            Component por;
            if (!m.requerida().isEmpty() && NpcCliente.hechas(m.requerida()) == 0) {
                Mision antes = Misiones.porId(m.requerida());
                por = Component.translatable("axelis_core.mision.requiere", antes == null ? Component.literal(m.requerida()) : antes.titulo());
            } else {
                NpcDef d = NpcDef.porId(m.npc());
                por = Component.translatable("axelis_core.mision.pide_amistad", d == null ? Component.literal("?") : d.nombreNivel(m.nivelPedido()));
            }
            Ui.text(g, por.getString(), x, y + 14, w, theme.textDim, 10);
            return;
        }
        g.drawString(f, Ui.fit(m.titulo().getString(), w - 50), x, y, colorRareza(m.rareza(), theme), !claro(theme));
        String tipo = m.esDiaria() ? Component.translatable("axelis_core.mision.diaria").getString() : "";
        if (!tipo.isEmpty()) {
            g.drawString(f, tipo, x + w - f.width(tipo), y, theme.accent2, false);
        }
        int yy = Ui.text(g, m.descripcion().getString(), x, y + 12, w, theme.text, 9) + 3;
        int[] prog = NpcCliente.progreso(m.id);
        boolean activa = e == Misiones.Estado.ACTIVA || e == Misiones.Estado.LISTA;
        List<Objetivo> objs = m.objetivos();
        for (int i = 0; i < objs.size() && yy < y + alto - 30; i++) {
            Objetivo o = objs.get(i);
            int v = i < prog.length ? prog[i] : 0;
            boolean hecho = activa && v >= o.cantidad;
            String cuenta = o.cantidad > 1 ? (activa ? v + "/" : "") + o.cantidad : "";
            String s = (hecho ? "✔ " : "• ") + o.descripcion().getString();
            g.drawString(f, Ui.fit(s, w - f.width(cuenta) - 6), x, yy, hecho ? theme.good : theme.text, false);
            if (!cuenta.isEmpty()) {
                g.drawString(f, cuenta, x + w - f.width(cuenta), yy, hecho ? theme.good : theme.textDim, false);
            }
            yy += 10;
        }
        // premios
        List<Mision.Premio> pr = m.premios();
        if (!pr.isEmpty()) {
            int py2 = y + alto - 20;
            g.drawString(f, Component.translatable("axelis_core.mision.premios").getString(), x, py2 + 5, theme.textDim, false);
            int ix = x + f.width(Component.translatable("axelis_core.mision.premios").getString()) + 4;
            for (Mision.Premio p : pr) {
                ItemStack st = p.icono();
                Ui.inset(g, ix, py2, 20, 20, theme);
                g.renderItem(st, ix + 2, py2 + 2);
                g.renderItemDecorations(f, st, ix + 2, py2 + 2);
                ix += 22;
            }
        }
    }

    /** ¿El fondo del tema es claro (pergamino)? Entonces los colores claros no se leen. */
    static boolean claro(Theme t) {
        int c = t.bgTop;
        float lum = (0.299F * ((c >> 16) & 255) + 0.587F * ((c >> 8) & 255) + 0.114F * (c & 255)) / 255F;
        return lum > 0.6F;
    }

    /** Color del título según la rareza, que se lea en cualquier tema. */
    static int colorRareza(com.axeliscore.ui.Rareza r, Theme t) {
        if (r == com.axeliscore.ui.Rareza.COMUN) {
            return t.title;
        }
        return claro(t) ? r.dark : r.light;
    }

    /** Cuadro de información de un premio (al pasar el mouse). */
    static InfoCard premioBajo(Mision m, int x, int y, int w, int alto, int mx, int my, Theme theme) {
        if (m == null || NpcCliente.estado(m) == Misiones.Estado.BLOQUEADA) {
            return null;
        }
        var f = Ui.font();
        int py2 = y + alto - 20;
        int ix = x + f.width(Component.translatable("axelis_core.mision.premios").getString()) + 4;
        for (Mision.Premio p : m.premios()) {
            if (mx >= ix && mx < ix + 20 && my >= py2 && my < py2 + 20) {
                return new InfoCard(p.texto()).icon(p.icono());
            }
            ix += 22;
        }
        return null;
    }

    // ------------------------------------------------------------------ tienda

    private void tienda(int x, int y, int iw) {
        Tienda t = def.tienda();
        if (t == null) {
            return;
        }
        List<Integer> idx = new ArrayList<>();
        for (int i = 0; i < t.ofertas().size(); i++) {
            idx.add(i);
        }
        ListView<Integer> lv = add(new ListView<>(x, y, iw, ph - 80 - 40, 22, (g, i, rx, ry, rw, rh, sel, hov, th) -> fila(g, t, i, rx, ry, rw,
                sel, th), i -> {
            oferta = i;
            rehacer();
        }));
        lv.cards = i -> {
            Tienda.Oferta o = t.ofertas().get(i);
            InfoCard c = new InfoCard(o.icono().getHoverName()).icon(o.icono());
            c.pair(Component.translatable(o.vende ? "axelis_core.tienda.precio" : "axelis_core.tienda.pagan"),
                    Component.literal(o.precio + " ").append(t.iconoMoneda(1).getHoverName()));
            if (o.limitada()) {
                c.pair(Component.translatable("axelis_core.tienda.hoy"), Component.literal(String.valueOf(NpcCliente.quedan(def, i))));
            }
            if (o.nivel > 0) {
                c.pair(Component.translatable("axelis_core.npc.amistad"), def.nombreNivel(o.nivel));
            }
            return c;
        };
        lv.items.addAll(idx);
        lv.selected = oferta;
        int by = py + ph - 30;
        if (oferta >= 0 && oferta < t.ofertas().size()) {
            Tienda.Oferta o = t.ofertas().get(oferta);
            String verbo = Component.translatable(o.vende ? "axelis_core.tienda.comprar" : "axelis_core.tienda.vender").getString();
            int bw = 70;
            for (int k = 0; k < 2; k++) {
                int veces = k == 0 ? 1 : 5;
                int fo = oferta;
                Button b = add(new Button(px + pw - 12 - (2 - k) * (bw + 4) + 4, by, bw, 20, verbo + " ×" + veces, () -> {
                    CompoundTag d = new CompoundTag();
                    d.putString("npc", def.id);
                    d.putInt("ent", ent);
                    d.putInt("oferta", fo);
                    d.putInt("veces", veces);
                    ClientData.send(NpcAcciones.TIENDA, d);
                }));
                b.enabled = puede(t, oferta, 1);
            }
        }
    }

    private boolean puede(Tienda t, int i, int veces) {
        Tienda.Oferta o = t.ofertas().get(i);
        if (o.nivel > 0 && def.nivelDe(NpcCliente.puntos(def.id)) < o.nivel) {
            return false;
        }
        if (NpcCliente.quedan(def, i) < 1) {
            return false;
        }
        return o.vende ? NpcCliente.contar(t.moneda()) >= o.precio * veces : NpcCliente.contar(o.objeto) >= o.cuantos * veces;
    }

    private void fila(G g, Tienda t, int i, int rx, int ry, int rw, boolean sel, Theme th) {
        var f = Ui.font();
        Tienda.Oferta o = t.ofertas().get(i);
        boolean bloqueada = o.nivel > 0 && def.nivelDe(NpcCliente.puntos(def.id)) < o.nivel;
        int quedan = NpcCliente.quedan(def, i);
        ItemStack st = o.icono();
        g.renderItem(st, rx, ry + 3);
        g.renderItemDecorations(f, st, rx, ry + 3);
        String flecha = o.vende ? "" : "⇄ ";
        String n = flecha + st.getHoverName().getString();
        int col = bloqueada || quedan < 1 ? th.textDim : sel ? th.title : th.text;
        g.drawString(f, Ui.fit(n, rw - 110), rx + 22, ry + 7, col, false);
        // precio con la moneda
        String pr = (o.vende ? "" : "+") + o.precio;
        int ex = rx + rw - 20;
        ItemStack mon = t.iconoMoneda(1);
        g.renderItem(mon, ex, ry + 3);
        g.drawString(f, pr, ex - f.width(pr) - 2, ry + 7, o.vende ? th.accent : th.good, false);
        String extra = bloqueada ? "✗ " + def.nombreNivel(o.nivel).getString() : o.limitada() ? Component.translatable(
                "axelis_core.tienda.quedan", quedan).getString() : "";
        if (!extra.isEmpty()) {
            int w = f.width(extra);
            g.drawString(f, extra, ex - f.width(pr) - 10 - w, ry + 7, quedan < 1 || bloqueada ? th.bad : th.textDim, false);
        }
    }

    // ------------------------------------------------------------------ dibujo

    @Override
    protected void content(G g, int mx, int my, float pt) {
        if (def == null) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        Entity e = mc.level == null ? null : mc.level.getEntity(ent);
        if (e == null || mc.player == null || e.distanceToSqr(mc.player) > 10 * 10) {
            onClose();
            return;
        }
        if (NpcCliente.yo() != visto) {
            rehacer();
        }
        var f = Ui.font();
        // retrato y amistad
        ResourceLocation skin = NpcCliente.piel(def);
        int rx = px + 12, ry = py + 20;
        Ui.inset(g, rx - 1, ry - 1, 34, 34, theme);
        g.blit(skin, rx, ry, 32, 32, 8, 8, 8, 8, 64, 64);
        g.blit(skin, rx, ry, 32, 32, 40, 8, 8, 8, 64, 64);
        int pts = NpcCliente.puntos(def.id);
        int lv = def.nivelDe(pts);
        g.drawString(f, "♥ " + def.nombreNivel(lv).getString(), rx + 40, ry + 4, 0xFFFF7AB8, true);
        float frac = 1F;
        String txt = pts + "";
        if (lv + 1 < def.niveles()) {
            int desde = def.umbral(lv), hasta = def.umbral(lv + 1);
            frac = (pts - desde) / (float) Math.max(1, hasta - desde);
            txt = pts + "/" + hasta;
        }
        if (mostrado < 0) {
            mostrado = frac;
        }
        mostrado += (frac - mostrado) * 0.1F;
        Ui.bar(g, rx + 40, ry + 16, 120, 7, mostrado, 0xFFFF9AD0, 0xFFD0407F, time);
        g.drawString(f, txt, rx + 164, ry + 15, theme.textDim, false);
        // moneda (si tiene tienda)
        if (def.tienda() != null) {
            int n = NpcCliente.contar(def.tienda().moneda());
            String s = String.valueOf(n);
            int ex = px + pw - 30;
            g.renderItem(def.tienda().iconoMoneda(1), ex, ry + 6);
            g.drawString(f, s, ex - f.width(s) - 3, ry + 10, theme.title, true);
        }
        int x = px + 12, y = py + 80, iw = pw - 24;
        switch (tab) {
            case MISIONES -> {
                if (def.misiones().isEmpty()) {
                    Ui.text(g, Component.translatable("axelis_core.npc.sin_misiones").getString(), x, y + 4, iw, theme.textDim, 10);
                } else {
                    detalleMision(g, x + 132, y, iw - 132);
                }
            }
            case TIENDA -> {
            }
            case AMISTAD -> amistad(g, x, y, iw);
            default -> hablar(g, x, y, iw);
        }
    }

    private void hablar(G g, int x, int y, int w) {
        String s = Component.translatable(saludo.isEmpty() ? "axelis_core.npc.hola" : saludo, Minecraft.getInstance().player.getName())
                .getString();
        int alto = Math.max(40, Ui.wrap(s, w - 20).size() * 10 + 16);
        Ui.round(g, x, y, w, alto, Ui.alpha(0xFFFFFFFF, 0.88F));
        // el texto aparece letra por letra
        int letras = (int) (openTime() * 60);
        String vis = s.substring(0, Math.min(s.length(), letras));
        Ui.text(g, vis, x + 10, y + 8, w - 20, 0xFF2A2018, 10);
        int m = NpcCliente.marca(def);
        if (m > 0) {
            String aviso = Component.translatable(m == 2 ? "axelis_core.npc.tiene_lista" : "axelis_core.npc.tiene_mision").getString();
            g.drawString(Ui.font(), aviso, x + 2, y + alto + 6, m == 2 ? theme.good : theme.accent, true);
        }
    }

    private void amistad(G g, int x, int y, int w) {
        var f = Ui.font();
        int pts = NpcCliente.puntos(def.id);
        int lv = def.nivelDe(pts);
        for (int i = 0; i < def.niveles(); i++) {
            int yy = y + i * 16;
            boolean llego = i <= lv;
            Ui.inset(g, x, yy, w, 14, theme);
            if (i == lv) {
                g.fill(x + 1, yy + 1, x + w - 1, yy + 13, Ui.alpha(0xFFFF7AB8, 0.25F));
            }
            g.drawString(f, (llego ? "♥ " : "♡ ") + def.nombreNivel(i).getString(), x + 4, yy + 3, llego ? 0xFFFF7AB8 : theme.textDim, false);
            String pt = def.umbral(i) + " " + Component.translatable("axelis_core.npc.puntos").getString();
            g.drawString(f, pt, x + w - f.width(pt) - 4, yy + 3, theme.textDim, false);
            if (def.tieneRegalo(i)) {
                String r = Component.translatable(llego ? "axelis_core.npc.regalo_dado" : "axelis_core.npc.regalo").getString();
                g.drawString(f, r, x + w / 2, yy + 3, llego ? theme.good : theme.accent, false);
            }
        }
        int yy = y + def.niveles() * 16 + 4;
        Ui.text(g, Component.translatable("axelis_core.npc.como_amistad").getString(), x, yy, w, theme.textDim, 10);
    }

    @Override
    protected InfoCard extraCard(int mx, int my) {
        if (tab == MISIONES && def != null) {
            int x = px + 12 + 132, y = py + 80;
            return premioBajo(Misiones.porId(elegida), x, y, pw - 24 - 132, ph - 80 - 40, mx, my, theme);
        }
        return null;
    }

    /** Para las pruebas: aprieta el botón cuyo texto empieza con el de esa clave. Devuelve true si lo encontró y estaba activo. */
    public boolean pulsar(String clave) {
        String txt = Component.translatable(clave).getString();
        for (Widget w : new ArrayList<>(widgets)) {
            if (w instanceof Button b && b.label.startsWith(txt) && b.enabled && b.onClick != null) {
                b.onClick.run();
                return true;
            }
        }
        return false;
    }

    /** Para las pruebas: centro del renglón {@code i} de la lista que se ve. */
    public int[] fila(int i) {
        for (Widget w : widgets) {
            if (w instanceof ListView<?> l) {
                return new int[] {l.x + 60, l.y + i * l.rowH + l.rowH / 2};
            }
        }
        return null;
    }

    /** Para las pruebas: la pestaña que se ve. */
    public int pestana() {
        return tab;
    }

    /** Para las pruebas: centro de un botón por su texto (o null). */
    public int[] boton(String texto) {
        for (Widget w : widgets) {
            if (w instanceof Button b && b.label.startsWith(texto)) {
                return new int[] {b.x + b.w / 2, b.y + b.h / 2};
            }
        }
        return null;
    }
}
