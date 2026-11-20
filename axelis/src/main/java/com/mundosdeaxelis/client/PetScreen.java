package com.mundosdeaxelis.client;

import com.mundosdeaxelis.entity.Mascota;
import com.mundosdeaxelis.network.ModNetwork;
import com.mundosdeaxelis.network.PetNamePacket;
import com.mundosdeaxelis.relic.PetData;
import com.mundosdeaxelis.relic.PetKinds;
import com.mundosdeaxelis.relic.PetMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Menú de una mascota (etapa 254): su vista en 3D (te sigue con la mirada), el nombre para cambiarlo, su cariño, su vida, lo
 * que te da, su comida favorita y cómo pelea; botones para sentarla, acariciarla, pedirle un truco, su modo de combate y su
 * acción especial; y su mochila si tiene.
 */
public class PetScreen extends AbstractContainerScreen<PetMenu> {
    private static final ResourceLocation ICONS = new ResourceLocation("textures/gui/icons.png");
    private static final int ACCENT = 0xFFB86A;

    private EditBox name;
    private final List<Btn> buttons = new ArrayList<>();

    private record Btn(int id, int x, int y, int w, int h) {
    }

    public PetScreen(PetMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = PetMenu.W;
        this.imageHeight = menu.height();
        this.inventoryLabelY = menu.invY() - 11;
    }

    private Mascota pet() {
        Entity e = minecraft == null || minecraft.level == null ? null : minecraft.level.getEntity(menu.petId);
        return e instanceof Mascota m ? m : null;
    }

    @Override
    protected void init() {
        super.init();
        UiFx.open();
        Mascota m = pet();
        name = new EditBox(font, leftPos + 9, topPos + 7, 116, 12, Component.translatable("gui.mundos_de_axelis.pet.name"));
        name.setMaxLength(PetData.MAX_NAME);
        name.setBordered(true);
        name.setValue(m != null && m.hasCustomName() ? m.getCustomName().getString() : "");
        name.setHint(Component.translatable("gui.mundos_de_axelis.pet.name_hint").withStyle(s -> s.withColor(0x8A7AA8)));
        addRenderableWidget(name);
        buttons.clear();
        buttons.add(new Btn(-1, 130, 5, 38, 15)); // guardar nombre
        buttons.add(new Btn(PetMenu.BUT_SIT, 8, 86, 52, 15));
        buttons.add(new Btn(PetMenu.BUT_PET, 62, 86, 52, 15));
        buttons.add(new Btn(PetMenu.BUT_TRICK, 116, 86, 52, 15));
        PetKinds.Kind k = m == null ? null : m.kind();
        boolean combat = k != null && k.combat();
        boolean special = k != null && k.special != PetKinds.Special.NONE && k.special != PetKinds.Special.BAG;
        if (combat && special) {
            buttons.add(new Btn(PetMenu.BUT_MODE, 8, 103, 79, 15));
            buttons.add(new Btn(PetMenu.BUT_SPECIAL, 89, 103, 79, 15));
        } else if (combat) {
            buttons.add(new Btn(PetMenu.BUT_MODE, 8, 103, 160, 15));
        } else if (special) {
            buttons.add(new Btn(PetMenu.BUT_SPECIAL, 8, 103, 160, 15));
        }
    }

    @Override
    public void containerTick() {
        super.containerTick();
        name.tick();
        Mascota m = pet();
        if (m == null || !m.isAlive()) {
            onClose();
        }
    }

    private void saveName() {
        ModNetwork.CHANNEL.sendToServer(new PetNamePacket(menu.containerId, name.getValue()));
        UiFx.success();
        name.setFocused(false);
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (key == 256) {
            onClose();
            return true;
        }
        if (name.isFocused()) {
            if (key == 257 || key == 335) {
                saveName();
                return true;
            }
            name.keyPressed(key, scan, mods);
            return true; // mientras escribes, las teclas no cierran el menú
        }
        return super.keyPressed(key, scan, mods);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0) {
            for (Btn b : buttons) {
                if (over(b, mx, my) && enabled(b)) {
                    if (b.id < 0) {
                        saveName();
                    } else if (minecraft != null && minecraft.gameMode != null) {
                        minecraft.gameMode.handleInventoryButtonClick(menu.containerId, b.id);
                        UiFx.click();
                    }
                    return true;
                }
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    private boolean over(Btn b, double mx, double my) {
        int x = leftPos + b.x, y = topPos + b.y;
        return mx >= x && mx < x + b.w && my >= y && my < y + b.h;
    }

    private boolean enabled(Btn b) {
        if (b.id == PetMenu.BUT_SPECIAL) {
            Mascota m = pet();
            boolean toggle = m != null && m.kind() != null && (m.kind().special == PetKinds.Special.LIGHT || m.kind().special == PetKinds.Special.FETCH);
            return toggle || menu.get(PetMenu.D_WAIT) <= 0;
        }
        return true;
    }

    private Component label(Btn b, Mascota m) {
        String g = "gui.mundos_de_axelis.pet.";
        return switch (b.id) {
            case -1 -> Component.translatable(g + "save");
            case PetMenu.BUT_SIT -> Component.translatable(g + (menu.get(PetMenu.D_SIT) == 1 ? "follow" : "sit"));
            case PetMenu.BUT_PET -> Component.translatable(g + "pet");
            case PetMenu.BUT_TRICK -> Component.translatable(g + "trick");
            case PetMenu.BUT_MODE -> Component.translatable(g + "mode." + menu.get(PetMenu.D_MODE));
            case PetMenu.BUT_SPECIAL -> {
                PetKinds.Kind k = m == null ? null : m.kind();
                if (k == null) {
                    yield Component.empty();
                }
                if (k.special == PetKinds.Special.LIGHT || k.special == PetKinds.Special.FETCH) {
                    yield Component.translatable(g + "special." + k.specialKey() + (menu.get(PetMenu.D_TOGGLE) == 1 ? ".on" : ".off"));
                }
                int w = menu.get(PetMenu.D_WAIT);
                Component base = Component.translatable(g + "special." + k.specialKey());
                yield w > 0 ? Component.empty().append(base).append(String.format(" %d:%02d", w / 60, w % 60)) : base;
            }
            default -> Component.empty();
        };
    }

    private Component tip(Btn b, Mascota m) {
        String g = "gui.mundos_de_axelis.pet.tip.";
        return switch (b.id) {
            case -1 -> Component.translatable(g + "save");
            case PetMenu.BUT_SIT -> Component.translatable(g + "sit");
            case PetMenu.BUT_PET -> Component.translatable(g + "pet");
            case PetMenu.BUT_TRICK -> Component.translatable(g + "trick");
            case PetMenu.BUT_MODE -> Component.translatable(g + "mode." + menu.get(PetMenu.D_MODE));
            case PetMenu.BUT_SPECIAL -> m == null || m.kind() == null ? Component.empty() : Component.translatable(g + "special." + m.kind().specialKey());
            default -> Component.empty();
        };
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        renderBackground(g);
        super.render(g, mx, my, pt);
        Mascota m = pet();
        for (Btn b : buttons) {
            Component lab = label(b, m);
            int x = leftPos + b.x, y = topPos + b.y;
            boolean ov = over(b, mx, my);
            int lw = font.width(lab);
            if (lw > b.w - 6) {
                // texto largo: se dibuja un poco más pequeño para que quepa
                PanelUi.button(g, font, x, y, b.w, b.h, Component.empty(), enabled(b), ov, ACCENT);
                float sc = (b.w - 6) / (float) lw;
                g.pose().pushPose();
                g.pose().translate(x + b.w / 2.0F, y + b.h / 2.0F, 0);
                g.pose().scale(sc, sc, 1);
                g.drawString(font, lab, -lw / 2, -4, enabled(b) ? 0xFFFFFFFF : 0xFF6C5E8C, false);
                g.pose().popPose();
            } else {
                PanelUi.button(g, font, x, y, b.w, b.h, lab, enabled(b), ov, ACCENT);
            }
        }
        for (Btn b : buttons) {
            if (over(b, mx, my)) {
                g.renderTooltip(font, font.split(tip(b, m), 180), mx, my);
            }
        }
        // corazones del cariño: explica qué es
        if (mx >= leftPos + 66 && mx < leftPos + 168 && my >= topPos + 24 && my < topPos + 35) {
            g.renderTooltip(font, font.split(Component.translatable("gui.mundos_de_axelis.pet.tip.love", menu.get(PetMenu.D_LOVE)), 180), mx, my);
        }
        renderTooltip(g, mx, my);
    }

    @Override
    protected void renderBg(GuiGraphics g, float pt, int mx, int my) {
        int x = leftPos, y = topPos, w = imageWidth, h = imageHeight;
        PanelUi.frame(g, x, y, w, h, ACCENT);
        // vista de la mascota
        int px = x + 8, py = y + 24, pw = 54, ph = 58;
        g.fill(px - 1, py - 1, px + pw + 1, py + ph + 1, 0xFF05030C);
        g.fillGradient(px, py, px + pw, py + ph, 0xFF2A1F44, 0xFF120B22);
        g.fill(px, py + ph - 10, px + pw, py + ph, 0x22FFFFFF);
        Mascota m = pet();
        if (m != null && m.kind() != null) {
            PetKinds.Kind k = m.kind();
            double tall = Math.max(0.45, Math.max(k.h, k.w * 0.95));
            int scale = (int) Math.max(26, Math.min(80, 40.0 / tall));
            int cx = px + pw / 2, cy = py + ph - 7;
            g.enableScissor(px, py, px + pw, py + ph);
            PetRenderer.gui = true;
            try {
                InventoryScreen.renderEntityInInventoryFollowsMouse(g, cx, cy, scale, (float) cx - mx, (float) (cy - 30) - my, m);
            } finally {
                PetRenderer.gui = false;
            }
            g.disableScissor();
        }
        // mochila e inventario
        if (menu.bagSize > 0) {
            PanelUi.sectionBar(g, x + 5, y + PetMenu.TOP, w - 10, ACCENT);
            for (int r = 0; r < menu.bagRows(); r++) {
                for (int c = 0; c < 9; c++) {
                    PanelUi.slot(g, x + 8 + c * 18, y + menu.bagY() + r * 18, ACCENT, false);
                }
            }
        }
        PanelUi.sectionBar(g, x + 5, y + menu.invY() - 12, w - 10, 0xB8AAD8);
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 9; c++) {
                PanelUi.plain(g, x + 8 + c * 18, y + menu.invY() + r * 18);
            }
        }
        for (int c = 0; c < 9; c++) {
            PanelUi.plain(g, x + 8 + c * 18, y + menu.invY() + 58);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mx, int my) {
        Mascota m = pet();
        int ix = 68;
        // cariño: 5 corazones (cada uno 20 puntos)
        int love = menu.get(PetMenu.D_LOVE);
        for (int i = 0; i < 5; i++) {
            int hx = ix + i * 10, hy = 25;
            g.blit(ICONS, hx, hy, 16, 0, 9, 9);
            int v = love - i * 20;
            if (v >= 20) {
                g.blit(ICONS, hx, hy, 52, 0, 9, 9);
            } else if (v >= 10) {
                g.blit(ICONS, hx, hy, 61, 0, 9, 9);
            }
        }
        g.drawString(font, Component.translatable(love >= PetData.MAX_LOVE ? "gui.mundos_de_axelis.pet.best" : "gui.mundos_de_axelis.pet.love"),
                ix + 52, 26, love >= PetData.MAX_LOVE ? 0xFFFF9EC8 : 0xFFB8AAD8, false);
        if (m == null || m.kind() == null || m.spec() == null) {
            return;
        }
        PetKinds.Kind k = m.kind();
        // vida
        float hp = m.getHealth(), max = m.getMaxHealth();
        int bw = 100;
        g.fill(ix, 38, ix + bw, 43, 0xFF1A1030);
        g.fill(ix, 38, ix + (int) (bw * Math.max(0, Math.min(1, hp / Math.max(1, max)))), 43, 0xFFE8506A);
        g.fill(ix, 38, ix + bw, 39, 0x30FFFFFF);
        g.drawString(font, Component.translatable("gui.mundos_de_axelis.pet.health", (int) Math.ceil(hp), (int) max), ix, 46, 0xFFE8DCFF, false);
        // armadura que te da
        int armor = com.mundosdeaxelis.crate.CratePools.petArmor(m.spec().id());
        g.drawString(font, Component.translatable("gui.mundos_de_axelis.pet.armor", armor), ix, 56, 0xFF9FD8FF, false);
        // comida favorita
        ItemStack food = new ItemStack(k.food());
        g.drawString(font, Component.translatable("gui.mundos_de_axelis.pet.food"), ix, 67, 0xFFB8AAD8, false);
        int fx = ix + font.width(Component.translatable("gui.mundos_de_axelis.pet.food")) + 2;
        g.pose().pushPose();
        g.pose().translate(fx, 64, 0);
        g.pose().scale(0.75F, 0.75F, 1);
        g.renderItem(food, 0, 0);
        g.pose().popPose();
        if (mx >= leftPos + fx && mx < leftPos + fx + 12 && my >= topPos + 64 && my < topPos + 76) {
            g.renderTooltip(font, food.getHoverName(), mx - leftPos, my - topPos);
        }
        // cómo pelea
        Component fight = k.combat()
                ? Component.translatable("gui.mundos_de_axelis.pet.fight." + k.attack.name().toLowerCase(java.util.Locale.ROOT),
                Component.translatable("gui.mundos_de_axelis.pet.element." + k.element.name().toLowerCase(java.util.Locale.ROOT)))
                : Component.translatable("gui.mundos_de_axelis.pet.fight.none");
        g.drawString(font, fight, ix, 77, k.combat() ? 0xFFFFB86A : 0xFF9AE0A0, false);
        if (menu.bagSize > 0) {
            g.drawString(font, Component.translatable("gui.mundos_de_axelis.pet.bag"), 9, PetMenu.TOP + 1, 0xFF000000 | ACCENT, true);
        }
        g.drawString(font, playerInventoryTitle, 9, inventoryLabelY, 0xFFB8AAD8, false);
    }
}
