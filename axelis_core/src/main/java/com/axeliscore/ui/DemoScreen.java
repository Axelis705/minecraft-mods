package com.axeliscore.ui;

import com.axeliscore.plat.client.G;
import com.axeliscore.ui.client.Cards;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Muestra de la interfaz del Core (/axeliscore efecto demo): pestañas, botones (con texto y con ícono), lista, barras, cuadrícula de
 * tarjetas con cuadros de información, interruptores, deslizadores y los temas con su marco.
 */
public class DemoScreen extends UiScreen {
    private int tab;
    private float health = 0.7F, shown = 0.7F;
    private String picked = "";
    private Entry detail;
    private boolean music = true, look = true;
    private float volume = 0.8F, level = 3;

    /** Una cosa de muestra (nombre, objeto, rareza, descripción, si ya se descubrió). */
    record Entry(String name, Item item, Rareza r, String desc, boolean known) {
        InfoCard card() {
            if (!known) {
                return new InfoCard(Component.literal("???")).color(0xFF8A90A0).text(Component.literal("Todavía no lo descubres."), 0xFF9AA3BC)
                        .hint(Component.literal("Pista: busca en el mar de noche"));
            }
            return new InfoCard(Component.literal(name)).rareza(r).icon(new ItemStack(item)).subtitle(Component.literal("Colección de muestra"))
                    .text(Component.literal(desc)).bar(Component.literal("Rareza"), (r.ordinal() + 1) / 6F, r.light, (r.ordinal() + 1) + "/6")
                    .pair(Component.literal("Valor"), Component.literal((r.ordinal() + 1) * 15 + " monedas"))
                    .stars(Component.literal("Maestría"), Math.min(3, r.ordinal()), 3);
        }
    }

    private static final Entry[] ENTRIES = {
            new Entry("Bacalao", Items.COD, Rareza.COMUN, "Abunda en los mares fríos. Fácil de pescar.", true),
            new Entry("Salmón", Items.SALMON, Rareza.COMUN, "Sube los ríos saltando contra la corriente.", true),
            new Entry("Prisma", Items.PRISMARINE_CRYSTALS, Rareza.POCO_COMUN, "Brilla bajo el agua cerca de los templos.", true),
            new Entry("Pez globo", Items.PUFFERFISH, Rareza.RARO, "Se infla cuando se asusta. ¡No lo comas!", true),
            new Entry("Pez tropical", Items.TROPICAL_FISH, Rareza.RARO, "Colores que cambian en cada arrecife.", true),
            new Entry("Esponja", Items.SPONGE, Rareza.RARO, "Absorbe toda el agua de alrededor.", true),
            new Entry("Concha de nautilo", Items.NAUTILUS_SHELL, Rareza.EPICO, "Muy antigua; la traen los ahogados.", true),
            new Entry("Huevo de tortuga", Items.TURTLE_EGG, Rareza.EPICO, "Hay que cuidarlo de los zombis.", true),
            new Entry("Corazón del mar", Items.HEART_OF_THE_SEA, Rareza.LEGENDARIO, "Late despacio en los cofres enterrados.", true),
            new Entry("Tridente", Items.TRIDENT, Rareza.MITICO, "El arma de los ahogados. Vuelve a tu mano.", true),
            new Entry("", Items.AIR, Rareza.COMUN, "", false), new Entry("", Items.AIR, Rareza.COMUN, "", false),
            new Entry("", Items.AIR, Rareza.COMUN, "", false), new Entry("", Items.AIR, Rareza.COMUN, "", false)};

    public DemoScreen() {
        super(Component.literal("Axelis Core"), 320, 206, Theme.NOCHE);
    }

    /** Para las pruebas: cambiar de pestaña sin hacer clic. */
    public void showTab(int i) {
        tab = i;
        init();
    }

    /** Para las pruebas: elegir una tarjeta de la cuadrícula. */
    public void pick(int i) {
        detail = ENTRIES[Math.max(0, Math.min(ENTRIES.length - 1, i))];
    }

    /** Para las pruebas: dónde queda la tarjeta {@code i} de la cuadrícula (centro), o null. */
    public int[] cellCenter(int i) {
        for (Widget w : widgets) {
            if (w instanceof CardGrid<?> cg) {
                int cols = cg.cols();
                int used = cols * (cg.cellW + cg.gap) - cg.gap;
                int ox = cg.x + (cg.w - 6 - used) / 2;
                return new int[] {ox + (i % cols) * (cg.cellW + cg.gap) + cg.cellW / 2, cg.y + 4 + (i / cols) * (cg.cellH + cg.gap) + cg.cellH / 2};
            }
        }
        return null;
    }

    /** Para las pruebas: el centro del botón con ícono número {@code i}. */
    public int[] iconCenter(int i) {
        int n = 0;
        for (Widget w : widgets) {
            if (w instanceof IconButton b && n++ == i) {
                return new int[] {b.x + b.w / 2, b.y + b.h / 2};
            }
        }
        return null;
    }

    /** Para las pruebas: el centro del renglón {@code i} de la lista. */
    public int[] rowCenter(int i) {
        for (Widget w : widgets) {
            if (w instanceof ListView<?> l) {
                return new int[] {l.x + 40, l.y + i * l.rowH + l.rowH / 2};
            }
        }
        return null;
    }

    public void theme(int i) {
        Theme[] th = {Theme.NOCHE, Theme.MADERA, Theme.PERGAMINO, Theme.MAR, Theme.PIEDRA};
        setTheme(th[Math.floorMod(i, th.length)]);
    }

    @Override
    protected void build() {
        Tabs t = add(new Tabs(px + 10, py + 22, pw - 20, 16, i -> {
            tab = i;
            rebuildTab();
        }, "Botones", "Lista", "Barras", "Tarjetas", "Ajustes", "Temas"));
        t.active = tab;
        rebuildTab();
    }

    private void rebuildTab() {
        widgets.removeIf(w -> !(w instanceof Tabs));
        int x = px + 16, y = py + 48, iw = pw - 32;
        if (tab == 0) {
            int bw = (iw - 8) / 2;
            add(new Button(x, y, bw, 20, "Aviso", () -> Toasts.add(Items.TROPICAL_FISH, Component.literal("¡Pez nuevo!"),
                    Component.literal("Pez tropical · 31 cm"), 0x7FF0E0)).help(Component.literal("Aviso"),
                    Component.literal("Muestra un aviso arriba a la derecha con el dibujo de un objeto.")));
            add(new Button(x + bw + 8, y, bw, 20, "Título", () -> com.axeliscore.fx.client.FxClient.title(Component.literal("¡Récord!"),
                    Component.literal("Salmón de 54 cm"), 0xFFD166, 60)));
            add(new Button(x, y + 26, bw, 20, "Temblor", () -> com.axeliscore.fx.client.FxClient.fx(com.axeliscore.fx.Fx.SHAKE, 0, 1F, 20)));
            add(new Button(x + bw + 8, y + 26, bw, 20, "Destello", () -> com.axeliscore.fx.client.FxClient.fx(com.axeliscore.fx.Fx.FLASH,
                    0xC0FFFFFF, 0, 10)));
            Button off = add(new Button(x, y + 52, iw, 20, "Apagado", null));
            off.enabled = false;
            Item[] icons = {Items.FISHING_ROD, Items.COMPASS, Items.MAP, Items.BOOK, Items.NETHER_STAR};
            String[] names = {"Caña", "Brújula", "Mapa", "Libro", "Estrella"};
            Rareza[] rr = {Rareza.COMUN, Rareza.POCO_COMUN, Rareza.RARO, Rareza.EPICO, Rareza.LEGENDARIO};
            for (int i = 0; i < icons.length; i++) {
                int fi = i;
                IconButton b = add(new IconButton(x + i * 26, y + 82, 22, new ItemStack(icons[i]), () -> picked = names[fi]));
                b.card(new InfoCard(Component.literal(names[i])).rareza(rr[i]).icon(new ItemStack(icons[i]))
                        .text(Component.literal("Botón con ícono: sirve para barras de herramientas y filtros."))
                        .hint(Component.literal("Clic: elegir")));
                b.selected = names[i].equals(picked);
            }
        } else if (tab == 1) {
            ListView<Entry> l = add(new ListView<>(x, y, iw, 118, 20, (g, it, rx, ry, rw, rh, sel, hov, th) -> {
                if (!it.known()) {
                    g.drawString(Ui.font(), "???", rx + 22, ry + 6, th.textDim, false);
                    return;
                }
                g.renderItem(new ItemStack(it.item()), rx, ry + 2);
                g.drawString(Ui.font(), it.name(), rx + 22, ry + 6, sel ? th.title : th.text, false);
                String r = it.r().nombre().getString();
                g.drawString(Ui.font(), r, rx + rw - Ui.font().width(r), ry + 6, it.r().light, false);
            }, it -> picked = it.name()));
            l.cards = Entry::card;
            for (Entry e : ENTRIES) {
                l.items.add(e);
            }
        } else if (tab == 2) {
            int bw = (iw - 8) / 2;
            add(new Button(x, y + 60, bw, 20, "- Vida", () -> health = Math.max(0, health - 0.15F)));
            add(new Button(x + bw + 8, y + 60, bw, 20, "+ Vida", () -> health = Math.min(1, health + 0.15F)));
        } else if (tab == 3) {
            CardGrid<Entry> grid = add(new CardGrid<>(x, y, 150, 140, 32, 40, 4, (g, it, cx, cy, cw, ch, hov, sel, th) ->
                    CardGrid.slot(g, cx, cy, cw, ch, new ItemStack(it.item()), it.name(), it.r(), !it.known(), hov, sel, th),
                    Entry::card, e -> detail = e));
            for (Entry e : ENTRIES) {
                grid.items.add(e);
            }
        } else if (tab == 4) {
            add(new Toggle(x, y, iw, "Música", music, v -> music = v).card(new InfoCard(Component.literal("Música"))
                    .text(Component.literal("Interruptor: se enciende y se apaga con un clic."))));
            add(new Toggle(x, y + 20, iw, "Tarjeta al mirar", look, v -> look = v));
            add(new Slider(x, y + 44, iw, "Volumen", 0, 1, 0.05F, volume, v -> Math.round(v * 100) + " %", v -> volume = v));
            String[] lv = {"Muy fácil", "Fácil", "Normal", "Difícil", "Experto"};
            add(new Slider(x, y + 74, iw, "Dificultad", 1, 5, 1, level, v -> lv[Math.round(v) - 1], v -> level = v)
                    .card(new InfoCard(Component.literal("Dificultad")).text(Component.literal("Deslizador por pasos (también con la rueda)."))));
        } else {
            String[] names = {"Noche", "Madera", "Pergamino", "Mar", "Piedra"};
            for (int i = 0; i < names.length; i++) {
                int fi = i;
                add(new Button(x, y + i * 24, 110, 20, names[i], () -> theme(fi)));
            }
        }
    }

    @Override
    protected void content(G g, int mx, int my, float pt) {
        int x = px + 16, y = py + 48;
        if ((tab == 0 || tab == 1) && !picked.isEmpty()) {
            g.drawString(Ui.font(), "Elegiste: " + picked, x, py + ph - 18, theme.accent, false);
        }
        if (tab == 2) {
            shown += (health - shown) * 0.12F;
            g.drawString(Ui.font(), "Vida", x, y, theme.text, false);
            Ui.bar(g, x, y + 11, pw - 32, 8, shown, 0xFF6EE07A, 0xFF2E8B3A, time);
            g.drawString(Ui.font(), "Experiencia", x, y + 26, theme.text, false);
            Ui.bar(g, x, y + 37, pw - 32, 8, (time * 0.2F) % 1F, 0xFF5FD0FF, 0xFF2A5FB0, time);
        }
        if (tab == 3) {
            // la tarjeta elegida, dibujada dentro de la pantalla (página de detalle)
            InfoCard c = detail != null ? detail.card() : new InfoCard(Component.literal("Elige una tarjeta"))
                    .text(Component.literal("Pasa el mouse para ver su cuadro de información; haz clic para verla aquí."), 0xFF9AA3BC);
            c.width(pw - 32 - 150 - 8);
            Cards.draw(g, c, x + 158, y, 1F);
        }
        if (tab == 5) {
            Ui.text(g, "Cada tema trae sus colores y su marco. Los mods pueden usar uno listo o hacer el suyo con Theme.of(fondo, acento).",
                    x + 124, y + 2, pw - 32 - 124, theme.text, 10);
        }
    }
}
