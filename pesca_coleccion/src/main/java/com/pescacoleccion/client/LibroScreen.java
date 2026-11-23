package com.pescacoleccion.client;

import com.axeliscore.data.client.ClientData;
import com.axeliscore.plat.Ids;
import com.axeliscore.plat.Nbt;
import com.axeliscore.plat.client.G;
import com.axeliscore.ui.Button;
import com.axeliscore.ui.CardGrid;
import com.axeliscore.ui.InfoCard;
import com.axeliscore.ui.ListView;
import com.axeliscore.ui.Rareza;
import com.axeliscore.ui.Tabs;
import com.axeliscore.ui.Theme;
import com.axeliscore.ui.Ui;
import com.axeliscore.ui.UiScreen;
import com.axeliscore.ui.client.Cards;
import com.pescacoleccion.PescaColeccion;
import com.pescacoleccion.pesca.Cana;
import com.pescacoleccion.pesca.Coleccion;
import com.pescacoleccion.pesca.Especie;
import com.pescacoleccion.pesca.Especies;
import com.pescacoleccion.pesca.Mejoras;
import com.pescacoleccion.pesca.PezItem;
import com.pescacoleccion.registry.ModRegistry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.Locale;

/**
 * (Cliente) El libro de pesca: los 25 peces (sombra hasta pescarlos, con una pista de dónde buscarlos), la página de cada uno con su
 * dibujo grande, las mejoras para siempre que dan las estrellas, las cañas que se ganan descubriendo especies y los récords del mundo.
 */
public class LibroScreen extends UiScreen {
    private static final String MOD = PescaColeccion.MODID;
    private int tab;
    private Especie detail;

    public LibroScreen() {
        super(Component.translatable(MOD + ".libro.titulo"), 380, 228, Theme.MADERA);
    }

    /** Para las pruebas. */
    public void showTab(int i) {
        tab = i;
        init();
    }

    public void pick(String id) {
        detail = Especies.get(id);
    }

    /** Para las pruebas: el centro de la tarjeta {@code i} de la cuadrícula. */
    public int[] cellCenter(int i) {
        for (var w : widgets) {
            if (w instanceof CardGrid<?> cg) {
                int cols = cg.cols();
                int used = cols * (cg.cellW + cg.gap) - cg.gap;
                int ox = cg.x + (cg.w - 6 - used) / 2;
                return new int[] {ox + (i % cols) * (cg.cellW + cg.gap) + cg.cellW / 2, cg.y + 4 + (i / cols) * (cg.cellH + cg.gap) + cg.cellH / 2};
            }
        }
        return null;
    }

    private static CompoundTag datos() {
        return ClientData.player(MOD);
    }

    private static CompoundTag pez(String id) {
        return Nbt.c(Nbt.c(datos(), "peces"), id);
    }

    private static boolean conocido(Especie e) {
        return Nbt.i(pez(e.id()), "n") > 0;
    }

    private static ResourceLocation arte(Especie e, boolean sombra) {
        return Ids.rl(MOD, "textures/gui/peces/" + e.id() + (sombra ? "_sombra" : "") + ".png");
    }

    private static String cm(float v) {
        return String.format(Locale.ROOT, v >= 100 ? "%.0f cm" : "%.1f cm", v);
    }

    private static Component nombre(Especie e) {
        return Component.translatable("item." + MOD + "." + e.itemId());
    }

    @Override
    protected void build() {
        Tabs t = add(new Tabs(px + 12, py + 34, pw - 24, 16, i -> {
            tab = i;
            rebuild();
        }, Component.translatable(MOD + ".libro.peces").getString(), Component.translatable(MOD + ".libro.mejoras").getString(),
                Component.translatable(MOD + ".libro.records").getString()));
        t.active = tab;
        rebuild();
    }

    private void rebuild() {
        widgets.removeIf(w -> !(w instanceof Tabs));
        int x = px + 12, y = py + 56;
        if (tab == 0) {
            CardGrid<Especie> grid = add(new CardGrid<>(x, y, 186, pw > 0 ? ph - 66 : 150, 32, 40, 3, (g, e, cx, cy, cw, ch, hov, sel, th) -> {
                boolean known = conocido(e);
                Rareza r = Rareza.of(e.rareza());
                CardGrid.slot(g, cx, cy, cw, ch, null, null, known ? r : null, false, hov, sel, th);
                g.blit(arte(e, !known), cx, cy + 1, 32, 32, 0, 0, 32, 32, 32, 32);
                int st = Integer.bitCount(Nbt.i(pez(e.id()), "e") & 7);
                for (int s = 0; s < 3; s++) {
                    int c = s < st ? 0xFFFFD24A : 0xFF3A3F4E;
                    g.fill(cx + 7 + s * 7, cy + ch - 5, cx + 12 + s * 7, cy + ch - 2, c);
                }
            }, LibroScreen::card, e -> detail = e));
            grid.items.addAll(Especies.all());
        } else if (tab == 1) {
            ListView<Object> l = add(new ListView<>(x, y, pw - 24, ph - 66, 22, (g, it, rx, ry, rw, rh, sel, hov, th) -> drawUpgradeRow(g, it, rx,
                    ry, rw, rh, th), null));
            l.cards = LibroScreen::upgradeCard;
            for (Cana c : Cana.values()) {
                if (c.ordinal() > 0) {
                    l.items.add(c);
                }
            }
            for (Mejoras.Mejora m : Mejoras.TODAS) {
                l.items.add(m);
            }
            // botones para reclamar las cañas ya ganadas
            int by = y;
            for (Cana c : Cana.values()) {
                if (c.ordinal() == 0) {
                    continue;
                }
                if (puedeReclamar(c)) {
                    Button b = add(new Button(px + pw - 12 - 70, by + 2, 62, 18, Component.translatable(MOD + ".libro.reclamar").getString(), () -> {
                        CompoundTag t = new CompoundTag();
                        t.putInt("cana", c.ordinal());
                        ClientData.send(MOD + ":reclamar_cana", t);
                    }));
                    b.card(new InfoCard(Component.translatable(MOD + ".libro.reclamar")).text(Component.translatable(MOD + ".libro.reclamar_ayuda")));
                }
                by += 22;
            }
        } else {
            ListView<Especie> l = add(new ListView<>(x, y, pw - 24, ph - 66, 20, (g, e, rx, ry, rw, rh, sel, hov, th) -> {
                CompoundTag r = Nbt.c(ClientData.get(com.pescacoleccion.pesca.Libro.RECORDS), e.id());
                boolean known = conocido(e);
                g.blit(arte(e, !known), rx, ry + 1, 18, 18, 0, 0, 32, 32, 32, 32);
                g.drawString(Ui.font(), known ? nombre(e).getString() : "???", rx + 24, ry + 6, known ? Rareza.of(e.rareza()).light : th.textDim, false);
                float rcm = Nbt.f(r, "cm");
                String s = rcm > 0 ? cm(rcm) + " — " + Nbt.s(r, "quien") : "—";
                g.drawString(Ui.font(), s, rx + rw - Ui.font().width(s), ry + 6, rcm > 0 ? th.text : th.textDim, false);
            }, null));
            l.items.addAll(Especies.all());
        }
    }

    private static boolean puedeReclamar(Cana c) {
        return Mejoras.puedeReclamar(datos(), c);
    }

    // ------------------------------------------------------------------ cuadros

    /** El cuadro de un pez en la cuadrícula: si ya se pescó, todo; si no, una pista de dónde buscarlo. */
    private static InfoCard card(Especie e) {
        Rareza r = Rareza.of(e.rareza());
        if (!conocido(e)) {
            return new InfoCard(Component.literal("???")).color(0xFF8A90A0).image(arte(e, true), 32)
                    .text(Component.translatable(MOD + ".libro.no_pescado"), 0xFF9AA3BC)
                    .pair(Component.translatable(MOD + ".libro.pista"), PezItem.donde(e));
        }
        CompoundTag f = pez(e.id());
        int st = Nbt.i(f, "e");
        return new InfoCard(nombre(e)).rareza(r).image(arte(e, false), 32).subtitle(PezItem.donde(e))
                .pair(Component.translatable(MOD + ".libro.veces"), Component.literal(String.valueOf(Nbt.i(f, "n"))))
                .pair(Component.translatable(MOD + ".libro.mejor"), Component.literal(cm(Nbt.f(f, "max"))))
                .stars(Component.translatable(MOD + ".pez.maestria"), Integer.bitCount(st & 7), 3);
    }

    /** La página de un pez (dentro del libro, a la derecha). */
    private static InfoCard page(Especie e, int width) {
        InfoCard c = card(e).width(width);
        if (conocido(e)) {
            c.divider().text(Component.translatable(MOD + ".pez." + e.id() + ".historia"));
            int st = Nbt.i(pez(e.id()), "e");
            c.divider();
            c.text(Component.literal((st & 1) != 0 ? "★ " : "☆ ").append(Component.translatable(MOD + ".libro.estrella1")), (st & 1) != 0 ? 0xFFFFD24A : 0xFF9AA3BC);
            c.text(Component.literal((st & 2) != 0 ? "★ " : "☆ ").append(Component.translatable(MOD + ".libro.estrella2",
                    cm(e.cmMin() + (e.cmMax() - e.cmMin()) * 0.75F))), (st & 2) != 0 ? 0xFFFFD24A : 0xFF9AA3BC);
            c.text(Component.literal((st & 4) != 0 ? "★ " : "☆ ").append(Component.translatable(MOD + ".libro.estrella3")), (st & 4) != 0 ? 0xFFFFD24A : 0xFF9AA3BC);
        }
        return c;
    }

    private static InfoCard upgradeCard(Object it) {
        if (it instanceof Cana c) {
            ItemStack st = new ItemStack(ModRegistry.CANAS.get(c).get());
            InfoCard card = ((com.pescacoleccion.pesca.CanaItem) st.getItem()).card(st, false);
            card.parts.removeIf(p -> p.kind == InfoCard.Kind.HINT);
            return card.text(Component.translatable(MOD + ".libro.cana_pide", Mejoras.CANA_ESPECIES[c.ordinal()]), 0xFF9AA3BC);
        }
        Mejoras.Mejora m = (Mejoras.Mejora) it;
        boolean on = Coleccion.estrellas(datos()) >= m.estrellas();
        return new InfoCard(Component.translatable(MOD + ".mejora." + m.id())).color(on ? 0xFFFFD24A : 0xFF8A90A0)
                .text(Component.translatable(MOD + ".mejora." + m.id() + ".que"))
                .pair(Component.translatable(MOD + ".libro.pide"), Component.literal(m.estrellas() + " ★"));
    }

    private void drawUpgradeRow(G g, Object it, int x, int y, int w, int h, Theme th) {
        CompoundTag d = datos();
        if (it instanceof Cana c) {
            ItemStack st = new ItemStack(ModRegistry.CANAS.get(c).get());
            g.renderItem(st, x, y + 3);
            boolean got = (Nbt.i(d, "canas") & (1 << c.ordinal())) != 0;
            int need = Mejoras.CANA_ESPECIES[c.ordinal()];
            int have = Mejoras.especies(d);
            g.drawString(Ui.font(), st.getHoverName().getString(), x + 22, y + 3, c.rareza.light, false);
            String s = got ? "✔ " + Component.translatable(MOD + ".libro.reclamada").getString()
                    : have >= need ? "" : have + "/" + need + " " + Component.translatable(MOD + ".libro.especies").getString();
            g.drawString(Ui.font(), s, x + 22, y + 12, got ? th.good : th.textDim, false);
            return;
        }
        Mejoras.Mejora m = (Mejoras.Mejora) it;
        int stars = Coleccion.estrellas(d);
        boolean on = stars >= m.estrellas();
        Cards.star(g, x + 4, y + 7, on ? 0xFFFFD24A : 0xFF3A3F4E, on ? 0xFFB8860B : 0xFF262A36, 1);
        g.drawString(Ui.font(), Component.translatable(MOD + ".mejora." + m.id()).getString(), x + 22, y + 3, on ? 0xFFFFE6A0 : th.textDim, false);
        g.drawString(Ui.font(), Component.translatable(MOD + ".mejora." + m.id() + ".que").getString(), x + 22, y + 12, on ? th.text : 0xFF7A6A58,
                false);
        String need = on ? "✔" : stars + "/" + m.estrellas() + "★";
        g.drawString(Ui.font(), need, x + w - Ui.font().width(need), y + 7, on ? th.good : th.textDim, false);
    }

    // ------------------------------------------------------------------ dibujo

    @Override
    protected void content(G g, int mx, int my, float pt) {
        CompoundTag d = datos();
        int especies = Mejoras.especies(d), estrellas = Coleccion.estrellas(d);
        int x = px + 12, y = py + 20;
        // progreso de la colección
        String a = Component.translatable(MOD + ".libro.progreso", especies, Especies.all().size()).getString();
        g.drawString(Ui.font(), a, x, y, theme.text, false);
        Ui.bar(g, x + Ui.font().width(a) + 6, y + 1, 90, 6, especies / (float) Especies.all().size(), 0xFF7CE38B, 0xFF2E8B3A, time);
        String b = estrellas + " / 75 ★";
        g.drawString(Ui.font(), b, px + pw - 12 - 96 - Ui.font().width(b), y, 0xFFFFD24A, false);
        Ui.bar(g, px + pw - 12 - 90, y + 1, 90, 6, estrellas / 75F, 0xFFFFD24A, 0xFFB8860B, time);
        if (tab == 0) {
            int w = pw - 24 - 186 - 8;
            if (detail != null) {
                Cards.draw(g, page(detail, w), px + 12 + 186 + 8, py + 56, 1F);
            } else {
                Ui.text(g, Component.translatable(MOD + ".libro.elige").getString(), px + 12 + 186 + 12, py + 62, w - 8, theme.textDim, 10);
            }
        }
    }
}
