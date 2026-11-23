package com.pescacoleccion.client;

import com.axeliscore.data.client.ClientData;
import com.axeliscore.plat.Nbt;
import com.axeliscore.plat.Plat;
import com.axeliscore.plat.client.G;
import com.axeliscore.ui.Button;
import com.axeliscore.ui.InfoCard;
import com.axeliscore.ui.ListView;
import com.axeliscore.ui.Rareza;
import com.axeliscore.ui.Tabs;
import com.axeliscore.ui.Theme;
import com.axeliscore.ui.Ui;
import com.axeliscore.ui.UiScreen;
import com.axeliscore.ui.client.Cards;
import com.pescacoleccion.PescaColeccion;
import com.pescacoleccion.nilo.Encargos;
import com.pescacoleccion.nilo.NiloMundo;
import com.pescacoleccion.nilo.Tienda;
import com.pescacoleccion.pesca.Cebo;
import com.pescacoleccion.pesca.Especie;
import com.pescacoleccion.pesca.Especies;
import com.pescacoleccion.pesca.PezItem;
import com.pescacoleccion.registry.ModRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;
import java.util.Locale;

/**
 * (Cliente) La ventana de Nilo: sus 3 encargos (entregar, cobrar o cambiar uno al día), su tienda (carnadas por esmeraldas y te compra
 * los peces) y la amistad (corazones, su historia y lo que pica aquí ahora). Todo lo decide el servidor; aquí solo se pide.
 */
public class NiloScreen extends UiScreen {
    private static final String MOD = PescaColeccion.MODID;
    private static final String[] CORAZON = {".##.##.", "#######", "#######", ".#####.", "..###..", "...#..."};
    private int tab;
    private ListView<Integer> venta;
    private ListView<Especie> pican;
    private int hashDatos, hashInv;

    public NiloScreen() {
        super(Component.translatable(MOD + ".nilo.titulo"), 380, 236, Theme.MAR);
    }

    /** Para las pruebas. */
    public void showTab(int i) {
        tab = i;
        init();
    }

    private static String tr(String k, Object... args) {
        return Component.translatable(MOD + ".nilo." + k, args).getString();
    }

    private static CompoundTag nilo() {
        return Nbt.c(ClientData.player(MOD), "nilo");
    }

    private static List<ItemStack> inv() {
        return Plat.mainItems(Minecraft.getInstance().player.getInventory());
    }

    @Override
    protected void build() {
        Tabs t = add(new Tabs(px + 12, py + 52, pw - 24, 16, i -> {
            tab = i;
            rebuild();
        }, tr("tab_encargos"), tr("tab_tienda"), tr("tab_nilo")));
        t.active = tab;
        rebuild();
    }

    private void rebuild() {
        widgets.removeIf(w -> !(w instanceof Tabs));
        venta = null;
        pican = null;
        int y0 = py + 86;
        if (tab == 0) {
            ListTag l = Nbt.list(nilo(), "encargos");
            int cw = (pw - 24 - 12) / 3;
            boolean puedeCambiar = Nbt.l(nilo(), "cambio") != dia() + 1;
            for (int i = 0; i < Encargos.CUANTOS && i < l.size(); i++) {
                CompoundTag q = Nbt.at(l, i);
                int x = px + 12 + i * (cw + 6), slot = i;
                String que = Nbt.b(q, "ok") ? null : Encargos.cumplido(q) ? "cobrar" : entrega(q) ? "entregar" : null;
                Button b = add(new Button(x + 4, py + ph - 26, cw - 30, 18, etiqueta(q), () -> pedir(slot, que)));
                b.enabled = que != null;
                Button r = add(new Button(x + cw - 24, py + ph - 26, 20, 18, "↻", () -> pedir(slot, "cambiar")));
                r.enabled = puedeCambiar && !Nbt.b(q, "ok");
                r.help(Component.translatable(MOD + ".nilo.cambiar"));
            }
        } else if (tab == 1) {
            int w = (pw - 30) / 2;
            ListView<Integer> compra = add(new ListView<>(px + 12, y0, w, py + ph - 10 - y0, 22, this::filaCompra, this::comprar));
            compra.cards = this::cardOferta;
            for (int i = 0; i < Tienda.OFERTAS.size(); i++) {
                compra.items.add(i);
            }
            venta = add(new ListView<>(px + 18 + w, y0, w, py + ph - 10 - y0, 22, this::filaVenta, this::vender));
            venta.cards = s -> {
                ItemStack st = s < inv().size() ? inv().get(s) : ItemStack.EMPTY;
                return st.getItem() instanceof PezItem pz ? pz.card(st, false) : null;
            };
            refrescarVenta();
        } else {
            int w = 168;
            pican = add(new ListView<>(px + 12, y0, w, py + ph - 10 - y0, 20, this::filaPez, null));
            pican.cards = LibroScreen::card;
            var mc = Minecraft.getInstance();
            for (Especie e : Especies.all()) {
                if (Especies.pica(e, mc.level, mc.player.blockPosition())) {
                    pican.items.add(e);
                }
            }
        }
        hashDatos = nilo().toString().hashCode();
    }

    private static long dia() {
        return Plat.dayTime(Minecraft.getInstance().level) / 24000L;
    }

    private static boolean entrega(CompoundTag q) {
        String t = Nbt.s(q, "t");
        return t.equals("entrega") || t.equals("grande");
    }

    private static String etiqueta(CompoundTag q) {
        if (Nbt.b(q, "ok")) {
            return "✔ " + tr("hecho");
        }
        if (Encargos.cumplido(q)) {
            return tr("cobrar");
        }
        if (entrega(q)) {
            return tr("entregar");
        }
        return Nbt.i(q, "p") + " / " + Math.max(1, Nbt.i(q, "n"));
    }

    private void pedir(int slot, String que) {
        if (que == null) {
            return;
        }
        CompoundTag t = new CompoundTag();
        t.putInt("slot", slot);
        t.putString("que", que);
        ClientData.send(MOD + ":nilo_encargo", t);
    }

    // ------------------------------------------------------------------ tienda

    private void comprar(Integer i) {
        Tienda.Oferta o = Tienda.OFERTAS.get(i);
        if (nivel() < o.nivel() || Tienda.esmeraldas(Minecraft.getInstance().player) < o.precio()) {
            return;
        }
        CompoundTag t = new CompoundTag();
        t.putInt("oferta", i);
        ClientData.send(MOD + ":nilo_comprar", t);
    }

    private void vender(Integer slot) {
        CompoundTag t = new CompoundTag();
        t.putInt("slot", slot);
        ClientData.send(MOD + ":nilo_vender", t);
    }

    private void refrescarVenta() {
        if (venta == null) {
            return;
        }
        venta.items.clear();
        List<ItemStack> l = inv();
        int h = 1;
        for (int i = 0; i < l.size(); i++) {
            if (Tienda.precio(l.get(i)) > 0) {
                venta.items.add(i);
                h = h * 31 + i * 7 + l.get(i).getCount();
            }
        }
        hashInv = h;
    }

    private int hashVenta() {
        List<ItemStack> l = inv();
        int h = 1;
        for (int i = 0; i < l.size(); i++) {
            if (Tienda.precio(l.get(i)) > 0) {
                h = h * 31 + i * 7 + l.get(i).getCount();
            }
        }
        return h;
    }

    private InfoCard cardOferta(Integer i) {
        Tienda.Oferta o = Tienda.OFERTAS.get(i);
        ItemStack st = new ItemStack(o.item().get(), o.cuantos());
        InfoCard c = st.getItem() instanceof com.axeliscore.item.CoreItem ci ? ci.card(st, false) : null;
        if (c == null) {
            c = new InfoCard(st.getHoverName()).icon(st);
        }
        c.parts.removeIf(p -> p.kind == InfoCard.Kind.HINT);
        c.pair(Component.translatable(MOD + ".nilo.precio"), Component.literal(o.precio() + " " + tr("esmeraldas")));
        if (nivel() < o.nivel()) {
            c.text(Component.translatable(MOD + ".nilo.pide_nivel", Component.translatable(MOD + ".nilo.nivel." + o.nivel())), 0xFFFF9A6A);
        }
        return c;
    }

    private void filaCompra(G g, Integer i, int x, int y, int w, int h, boolean sel, boolean hov, Theme th) {
        Tienda.Oferta o = Tienda.OFERTAS.get(i);
        ItemStack st = new ItemStack(o.item().get(), o.cuantos());
        boolean puede = nivel() >= o.nivel();
        g.renderItem(st, x, y + 3);
        String nom = st.getHoverName().getString() + (o.cuantos() > 1 ? " ×" + o.cuantos() : "");
        g.drawString(Ui.font(), Ui.fit(nom, w - 62), x + 22, y + 7, puede ? th.text : th.textDim, false);
        if (puede) {
            String pr = String.valueOf(o.precio());
            int pw2 = Ui.font().width(pr);
            g.drawString(Ui.font(), pr, x + w - 18 - pw2, y + 7, 0xFF7CF29A, false);
            g.renderItem(new ItemStack(Items.EMERALD), x + w - 16, y + 3);
        } else {
            String s = (o.nivel() + 1) + " ♥";
            g.drawString(Ui.font(), s, x + w - Ui.font().width(s), y + 7, th.textDim, false);
        }
    }

    private void filaVenta(G g, Integer s, int x, int y, int w, int h, boolean sel, boolean hov, Theme th) {
        ItemStack st = s < inv().size() ? inv().get(s) : ItemStack.EMPTY;
        if (!(st.getItem() instanceof PezItem pz) || pz.especie() == null) {
            return;
        }
        g.renderItem(st, x, y + 3);
        String cm = String.format(Locale.ROOT, PezItem.cm(st) >= 100 ? "%.0f cm" : "%.1f cm", PezItem.cm(st));
        g.drawString(Ui.font(), Ui.fit(st.getHoverName().getString(), w - 70), x + 24, y + 3, Rareza.of(pz.especie().rareza()).light, false);
        g.drawString(Ui.font(), cm + (PezItem.grande(st, pz.especie()) ? " ★" : "") + (st.getCount() > 1 ? "  ×" + st.getCount() : ""), x + 24, y + 12,
                th.textDim, false);
        String pr = "+" + Tienda.precio(st);
        g.drawString(Ui.font(), pr, x + w - 18 - Ui.font().width(pr), y + 7, 0xFF7CF29A, false);
        g.renderItem(new ItemStack(Items.EMERALD), x + w - 16, y + 3);
    }

    private void filaPez(G g, Especie e, int x, int y, int w, int h, boolean sel, boolean hov, Theme th) {
        boolean known = LibroScreen.conocido(e);
        g.blit(LibroScreen.arte(e, !known), x, y + 1, 18, 18, 0, 0, 32, 32, 32, 32);
        String n = known ? Component.translatable("item." + MOD + "." + e.itemId()).getString() : "???";
        g.drawString(Ui.font(), Ui.fit(n, w - 70), x + 24, y + 6, known ? Rareza.of(e.rareza()).light : th.textDim, false);
        String rz = Rareza.of(e.rareza()).nombre().getString();
        g.drawString(Ui.font(), rz, x + w - Ui.font().width(rz), y + 6, Ui.alpha(Rareza.of(e.rareza()).light, 0.7F), false);
    }

    // ------------------------------------------------------------------ dibujo

    private static int nivel() {
        return Encargos.nivel(Nbt.i(nilo(), "amistad"));
    }

    private static void corazon(G g, int x, int y, int c) {
        for (int r = 0; r < CORAZON.length; r++) {
            for (int k = 0; k < CORAZON[r].length(); k++) {
                if (CORAZON[r].charAt(k) == '#') {
                    g.fill(x + k, y + r, x + k + 1, y + r + 1, c);
                }
            }
        }
    }

    @Override
    protected void content(G g, int mx, int my, float pt) {
        if (nilo().toString().hashCode() != hashDatos) {
            rebuild();
        }
        if (venta != null && hashVenta() != hashInv) {
            refrescarVenta();
        }
        CompoundTag nd = nilo();
        int am = Nbt.i(nd, "amistad"), nv = Encargos.nivel(am);
        // retrato
        Ui.inset(g, px + 12, py + 20, 28, 28, theme);
        g.blit(NiloMundo.PIEL, px + 14, py + 22, 24, 24, 8, 8, 8, 8, 64, 64);
        g.blit(NiloMundo.PIEL, px + 14, py + 22, 24, 24, 40, 8, 8, 8, 64, 64);
        g.drawString(Ui.font(), "Nilo", px + 46, py + 22, theme.title, true);
        g.drawString(Ui.font(), tr("nivel." + nv), px + 46 + Ui.font().width("Nilo") + 6, py + 22, 0xFFFF9AD5, false);
        for (int i = 0; i < 5; i++) {
            corazon(g, px + 46 + i * 9, py + 35, i <= nv ? 0xFFFF6FA8 : 0xFF3A3F55);
        }
        if (nv < Encargos.NIVELES.length - 1) {
            int a = Encargos.NIVELES[nv], b = Encargos.NIVELES[nv + 1];
            Ui.bar(g, px + 46 + 5 * 9 + 4, py + 36, 80, 5, (am - a) / (float) (b - a), 0xFFFF9AD5, 0xFFB8407F, time);
            g.drawString(Ui.font(), am + "/" + b, px + 46 + 5 * 9 + 90, py + 35, theme.textDim, false);
        }
        // esmeraldas
        String es = String.valueOf(Tienda.esmeraldas(Minecraft.getInstance().player));
        g.renderItem(new ItemStack(Items.EMERALD), px + pw - 30, py + 24);
        g.drawString(Ui.font(), es, px + pw - 32 - Ui.font().width(es), py + 29, 0xFF7CF29A, true);
        if (tab == 0) {
            encargos(g, nd);
        } else if (tab == 1) {
            g.drawString(Ui.font(), tr("vende"), px + 14, py + 75, theme.textDim, false);
            g.drawString(Ui.font(), tr("compra"), px + 20 + (pw - 30) / 2, py + 75, theme.textDim, false);
            if (venta != null && venta.items.isEmpty()) {
                Ui.text(g, tr("sin_peces"), px + 26 + (pw - 30) / 2, py + 96, (pw - 30) / 2 - 16, theme.textDim, 10);
            }
        } else {
            g.drawString(Ui.font(), tr("pican"), px + 14, py + 75, theme.textDim, false);
            if (pican != null && pican.items.isEmpty()) {
                Ui.text(g, tr("no_pican"), px + 20, py + 96, 150, theme.textDim, 10);
            }
            InfoCard c = new InfoCard(Component.translatable(MOD + ".nilo.historia_titulo")).color(0xFFFF9AD5).width(pw - 24 - 176)
                    .text(Component.translatable(MOD + ".nilo.historia." + nv));
            if (nv < Encargos.NIVELES.length - 1) {
                c.divider().pair(Component.translatable(MOD + ".nilo.proximo"), Component.translatable(MOD + ".nilo.regalo." + (nv + 1)));
            }
            c.pair(Component.translatable(MOD + ".nilo.hechos"), Component.literal(String.valueOf(Nbt.i(nd, "hechos"))));
            Cards.draw(g, c, px + 12 + 176, py + 72, 1F);
        }
    }

    /** Las 3 tarjetas de los encargos: cada una en su columna (el cuadro completo sale al pasar el mouse). */
    private void encargos(G g, CompoundTag nd) {
        ListTag l = Nbt.list(nd, "encargos");
        int cw = (pw - 24 - 12) / 3, top = py + 74, h = ph - 74 - 32;
        if (l.isEmpty()) {
            Ui.text(g, tr("sin_encargos"), px + 16, py + 80, pw - 32, theme.textDim, 10);
        }
        for (int i = 0; i < Encargos.CUANTOS && i < l.size(); i++) {
            panelEncargo(g, Nbt.at(l, i), px + 12 + i * (cw + 6), top, cw, h);
        }
    }

    /** Una tarjeta compacta: ícono y tipo, qué pide, cuánto llevas y el premio con íconos. */
    private void panelEncargo(G g, CompoundTag q, int x, int y, int w, int h) {
        String t = Nbt.s(q, "t");
        boolean ok = Nbt.b(q, "ok"), listo = Encargos.cumplido(q);
        int borde = ok ? 0xFF7CF29A : listo ? 0xFFFFD24A : 0xFF8FD8FF;
        Ui.round(g, x, y, w, h, Ui.alpha(borde, ok || listo ? 0.9F : 0.45F));
        g.fillGradient(x + 1, y + 1, x + w - 1, y + h - 1, 0xF0102A34, 0xF00A1A22);
        g.fill(x + 1, y + 1, x + w - 1, y + 2, Ui.alpha(borde, 0.7F));
        g.renderItem(icono(q), x + 4, y + 5);
        g.drawString(Ui.font(), Ui.fit(tr("t." + t), w - 26), x + 23, y + 9, borde, true);
        int yy = Ui.text(g, que(q).getString(), x + 5, y + 25, w - 10, ok ? theme.textDim : theme.text, 10);
        if (ok) {
            Ui.text(g, "✔ " + tr("manana"), x + 5, yy + 3, w - 10, 0xFF7CF29A, 10);
            return;
        }
        // progreso
        int n = Math.max(1, Nbt.i(q, "n")), p = Math.min(n, llevas(q));
        String pr = p + "/" + n;
        g.drawString(Ui.font(), pr, x + w - 5 - Ui.font().width(pr), yy + 3, listo ? 0xFFFFD24A : theme.textDim, false);
        Ui.bar(g, x + 5, yy + 5, w - 16 - Ui.font().width(pr), 4, p / (float) n, listo ? 0xFFFFD24A : 0xFF6FB7FF, listo ? 0xFFB8860B : 0xFF2F6FD6, time);
        // premio: esmeraldas, carnada y amistad
        int ry = y + h - 20, rx = x + 4;
        g.renderItem(new ItemStack(Items.EMERALD), rx, ry);
        g.drawString(Ui.font(), String.valueOf(Nbt.i(q, "es")), rx + 16, ry + 8, 0xFF7CF29A, true);
        rx += 18 + Ui.font().width(String.valueOf(Nbt.i(q, "es")));
        int cb = Nbt.i(q, "cb");
        if (cb >= 0 && cb < Cebo.values().length && Nbt.i(q, "cbn") > 0) {
            g.renderItem(new ItemStack(ModRegistry.CEBOS.get(Cebo.values()[cb]).get()), rx, ry);
            String c = "×" + Nbt.i(q, "cbn");
            g.drawString(Ui.font(), c, rx + 16, ry + 8, 0xFFFFFFFF, true);
            rx += 18 + Ui.font().width(c);
        }
        corazon(g, rx + 1, ry + 6, 0xFFFF6FA8);
        g.drawString(Ui.font(), "+" + Nbt.i(q, "am"), rx + 10, ry + 6, 0xFFFF9AD5, true);
    }

    /** Lo que lleva de un encargo (en los de entregar, los peces que trae encima). */
    private static int llevas(CompoundTag q) {
        String t = Nbt.s(q, "t");
        if (!t.equals("entrega") && !t.equals("grande")) {
            return Nbt.i(q, "p");
        }
        Especie e = Especies.get(Nbt.s(q, "e"));
        float min = t.equals("grande") ? Nbt.f(q, "cm") : 0;
        int tiene = 0;
        for (ItemStack st : Plat.mainAndOffhand(Minecraft.getInstance().player.getInventory())) {
            if (st.getItem() instanceof PezItem pz && e != null && pz.especie() == e && PezItem.cm(st) >= min) {
                tiene += st.getCount();
            }
        }
        return tiene;
    }

    @Override
    protected InfoCard extraCard(int mx, int my) {
        if (tab != 0) {
            return null;
        }
        ListTag l = Nbt.list(nilo(), "encargos");
        int cw = (pw - 24 - 12) / 3, top = py + 74, h = ph - 74 - 32;
        for (int i = 0; i < Encargos.CUANTOS && i < l.size(); i++) {
            int x = px + 12 + i * (cw + 6);
            if (mx >= x && mx < x + cw && my >= top && my < top + h) {
                return tarjeta(Nbt.at(l, i), 200);
            }
        }
        return null;
    }

    private static ItemStack icono(CompoundTag q) {
        Especie e = Especies.get(Nbt.s(q, "e"));
        return switch (Nbt.s(q, "t")) {
            case "entrega", "grande" -> e != null ? new ItemStack(ModRegistry.PECES.get(e.id()).get()) : new ItemStack(Items.COD);
            case "rareza" -> new ItemStack(ModRegistry.PECES.get(primera(Nbt.i(q, "r"))).get());
            case "perfecta" -> new ItemStack(Items.NETHER_STAR);
            case "noche" -> new ItemStack(Items.CLOCK);
            case "lluvia" -> new ItemStack(Items.WATER_BUCKET);
            default -> new ItemStack(ModRegistry.LIBRO.get());
        };
    }

    private static Component que(CompoundTag q) {
        String t = Nbt.s(q, "t");
        int n = Math.max(1, Nbt.i(q, "n"));
        Especie e = Especies.get(Nbt.s(q, "e"));
        String nombre = e != null ? Component.translatable("item." + MOD + "." + e.itemId()).getString() : "";
        return switch (t) {
            case "entrega" -> Component.translatable(MOD + ".nilo.e.entrega", n, nombre);
            case "grande" -> Component.translatable(MOD + ".nilo.e.grande", nombre, String.format(Locale.ROOT, "%.0f", Nbt.f(q, "cm")));
            case "rareza" -> Component.translatable(MOD + ".nilo.e.rareza", n, Rareza.of(Nbt.i(q, "r")).nombre());
            default -> Component.translatable(MOD + ".nilo.e." + t, n);
        };
    }

    /** La tarjeta de un encargo: qué pide, cuánto llevas y qué da. */
    static InfoCard tarjeta(CompoundTag q, int w) {
        String t = Nbt.s(q, "t");
        int n = Math.max(1, Nbt.i(q, "n")), p = Nbt.i(q, "p");
        Especie e = Especies.get(Nbt.s(q, "e"));
        ItemStack icono = icono(q);
        Component que = que(q);
        boolean ok = Nbt.b(q, "ok"), listo = Encargos.cumplido(q);
        InfoCard c = new InfoCard(Component.translatable(MOD + ".nilo.t." + t)).icon(icono).width(w)
                .color(ok ? 0xFF7CF29A : listo ? 0xFFFFD24A : 0xFF8FD8FF).text(que);
        if (ok) {
            c.text(Component.translatable(MOD + ".nilo.manana"), 0xFF7CF29A);
            return c;
        }
        if (t.equals("entrega") || t.equals("grande")) {
            int tiene = 0;
            float min = t.equals("grande") ? Nbt.f(q, "cm") : 0;
            for (ItemStack st : Plat.mainAndOffhand(Minecraft.getInstance().player.getInventory())) {
                if (st.getItem() instanceof PezItem pz && e != null && pz.especie() == e && PezItem.cm(st) >= min) {
                    tiene += st.getCount();
                }
            }
            c.bar(Component.translatable(MOD + ".nilo.llevas"), Math.min(1F, tiene / (float) n), 0xFF7CF29A, Math.min(tiene, n) + "/" + n);
        } else {
            c.bar(Component.translatable(MOD + ".nilo.llevas"), Math.min(1F, p / (float) n), listo ? 0xFFFFD24A : 0xFF6FB7FF, Math.min(p, n) + "/" + n);
        }
        c.divider();
        c.pair(Component.translatable(MOD + ".nilo.premio"), Component.literal(Nbt.i(q, "es") + " " + tr("esmeraldas")));
        int cb = Nbt.i(q, "cb");
        if (cb >= 0 && cb < Cebo.values().length && Nbt.i(q, "cbn") > 0) {
            ItemStack st = new ItemStack(ModRegistry.CEBOS.get(Cebo.values()[cb]).get());
            c.pair(Component.literal("+"), Component.literal(Nbt.i(q, "cbn") + " " + st.getHoverName().getString()));
        }
        c.pair(Component.translatable(MOD + ".nilo.amistad"), Component.literal("+" + Nbt.i(q, "am") + " ♥"));
        return c;
    }

    /** La primera especie de esa rareza (para el dibujo del encargo). */
    private static String primera(int rareza) {
        for (Especie e : Especies.all()) {
            if (e.rareza() == rareza) {
                return e.id();
            }
        }
        return Especies.all().get(0).id();
    }
}
