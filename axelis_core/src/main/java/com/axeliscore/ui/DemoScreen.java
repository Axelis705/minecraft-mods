package com.axeliscore.ui;

import com.axeliscore.plat.client.G;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Muestra de la interfaz del Core (/axeliscore efecto demo): pestañas, botones, lista con desplazamiento, barras y avisos. */
public class DemoScreen extends UiScreen {
    private int tab;
    private float health = 0.7F, shown = 0.7F;
    private String picked = "";
    private static final Object[][] FISH = {{"Bacalao", Items.COD, "Común"}, {"Salmón", Items.SALMON, "Común"}, {"Pez globo", Items.PUFFERFISH,
            "Raro"}, {"Pez tropical", Items.TROPICAL_FISH, "Raro"}, {"Bota vieja", Items.LEATHER_BOOTS, "Basura"}, {"Concha", Items.NAUTILUS_SHELL,
            "Épico"}, {"Corazón del mar", Items.HEART_OF_THE_SEA, "Legendario"}, {"Tinta", Items.INK_SAC, "Común"}, {"Algas", Items.KELP,
            "Basura"}, {"Prisma", Items.PRISMARINE_CRYSTALS, "Raro"}};

    public DemoScreen() {
        super(Component.literal("Axelis Core"), 280, 190, Theme.NOCHE);
    }

    /** Para las pruebas: cambiar de pestaña sin hacer clic. */
    public void showTab(int i) {
        tab = i;
        init();
    }

    @Override
    protected void build() {
        Tabs t = add(new Tabs(px + 10, py + 22, pw - 20, 16, i -> {
            tab = i;
            rebuildTab();
        }, "Botones", "Lista", "Barras"));
        t.active = tab;
        rebuildTab();
    }

    private void rebuildTab() {
        widgets.removeIf(w -> !(w instanceof Tabs));
        int x = px + 16, y = py + 48;
        if (tab == 0) {
            add(new Button(x, y, 120, 20, "Aviso", () -> Toasts.add(Items.TROPICAL_FISH, Component.literal("¡Pez nuevo!"),
                    Component.literal("Pez tropical · 31 cm"), 0x7FF0E0)).help(Component.literal("Muestra un aviso arriba a la derecha")));
            add(new Button(x + 128, y, 120, 20, "Título", () -> com.axeliscore.fx.client.FxClient.title(Component.literal("¡Récord!"),
                    Component.literal("Salmón de 54 cm"), 0xFFD166, 60)));
            add(new Button(x, y + 26, 120, 20, "Temblor", () -> com.axeliscore.fx.client.FxClient.fx(com.axeliscore.fx.Fx.SHAKE, 0, 1F, 20)));
            add(new Button(x + 128, y + 26, 120, 20, "Destello", () -> com.axeliscore.fx.client.FxClient.fx(com.axeliscore.fx.Fx.FLASH,
                    0xC0FFFFFF, 0, 10)));
            Button off = add(new Button(x, y + 52, 248, 20, "Apagado", null));
            off.enabled = false;
        } else if (tab == 1) {
            ListView<Object[]> l = add(new ListView<>(x, y, 248, 110, 20, (g, it, rx, ry, rw, rh, sel, hov, th) -> {
                g.renderItem(new ItemStack((net.minecraft.world.item.Item) it[1]), rx, ry + 2);
                g.drawString(Ui.font(), (String) it[0], rx + 22, ry + 6, sel ? th.title : th.text, false);
                String r = (String) it[2];
                int c = switch (r) {
                    case "Raro" -> 0xFF5FD0FF;
                    case "Épico" -> 0xFFC08CFF;
                    case "Legendario" -> 0xFFFFC14A;
                    case "Basura" -> 0xFF8A8A8A;
                    default -> th.textDim;
                };
                g.drawString(Ui.font(), r, rx + rw - Ui.font().width(r), ry + 6, c, false);
            }, it -> picked = (String) it[0]));
            for (Object[] f : FISH) {
                l.items.add(f);
            }
            for (Object[] f : FISH) {
                l.items.add(f);
            }
        } else {
            add(new Button(x, y + 60, 120, 20, "- Vida", () -> health = Math.max(0, health - 0.15F)));
            add(new Button(x + 128, y + 60, 120, 20, "+ Vida", () -> health = Math.min(1, health + 0.15F)));
        }
    }

    @Override
    protected void content(G g, int mx, int my, float pt) {
        int x = px + 16, y = py + 48;
        if (tab == 1 && !picked.isEmpty()) {
            g.drawString(Ui.font(), "Elegiste: " + picked, x, py + ph - 18, theme.accent, false);
        }
        if (tab == 2) {
            shown += (health - shown) * 0.12F;
            g.drawString(Ui.font(), "Vida", x, y, theme.text, false);
            Ui.bar(g, x, y + 11, 248, 8, shown, 0xFF6EE07A, 0xFF2E8B3A, time);
            g.drawString(Ui.font(), "Experiencia", x, y + 26, theme.text, false);
            Ui.bar(g, x, y + 37, 248, 8, (time * 0.2F) % 1F, 0xFF5FD0FF, 0xFF2A5FB0, time);
        }
    }
}
