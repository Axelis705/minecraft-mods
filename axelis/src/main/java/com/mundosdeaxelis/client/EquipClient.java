package com.mundosdeaxelis.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.mundosdeaxelis.MundosDeAxelis;
import com.mundosdeaxelis.network.EquipActionPacket;
import com.mundosdeaxelis.network.EquipSyncPacket;
import com.mundosdeaxelis.network.ModNetwork;
import com.mundosdeaxelis.relic.Equipment;
import com.mundosdeaxelis.relic.RelicItem;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * Lado del cliente del equipo: teclas (G menú, R reliquia, V mascotas), selección de reliquia con la rueda del ratón
 * y el panelito del borde izquierdo con lo equipado y sus enfriamientos.
 */
@Mod.EventBusSubscriber(modid = MundosDeAxelis.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class EquipClient {
    private EquipClient() {
    }

    private static final String CAT = "key.categories." + MundosDeAxelis.MODID;
    public static final KeyMapping MENU = new KeyMapping("key." + MundosDeAxelis.MODID + ".equip_menu",
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_G, CAT);
    public static final KeyMapping RELIC = new KeyMapping("key." + MundosDeAxelis.MODID + ".use_relic",
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_R, CAT);

    // ------------------------------------------------------------------ datos recibidos del servidor

    private static final ItemStack[] pets = blank(Equipment.MAX_PETS);
    private static final ItemStack[] relics = blank(Equipment.MAX_RELICS);
    private static int petSlots = 3;
    private static int relicSlots = 6;
    /** Posición dentro de la lista de reliquias de tecla (no el número de ranura). */
    private static int selected;

    private static ItemStack[] blank(int n) {
        ItemStack[] a = new ItemStack[n];
        java.util.Arrays.fill(a, ItemStack.EMPTY);
        return a;
    }

    public static void set(EquipSyncPacket p) {
        for (int i = 0; i < pets.length; i++) {
            pets[i] = i < p.pets.length ? p.pets[i] : ItemStack.EMPTY;
        }
        for (int i = 0; i < relics.length; i++) {
            relics[i] = i < p.relics.length ? p.relics[i] : ItemStack.EMPTY;
        }
        petSlots = p.petSlots;
        relicSlots = p.relicSlots;
        selected = Mth.clamp(selected, 0, Math.max(0, keyRelics().size() - 1));
    }

    private static boolean isKeyRelic(ItemStack s) {
        return s.getItem() instanceof RelicItem r && !r.isPet() && r.kind() == RelicItem.Kind.KEY;
    }

    /** Números de ranura de las reliquias equipadas que se lanzan con la tecla R. */
    private static List<Integer> keyRelics() {
        List<Integer> out = new ArrayList<>();
        for (int i = 0; i < Math.min(relicSlots, relics.length); i++) {
            if (isKeyRelic(relics[i])) {
                out.add(i);
            }
        }
        return out;
    }

    // ------------------------------------------------------------------ teclas

    private static boolean relicWasDown;
    private static boolean scrolled;

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null) {
            relicWasDown = false;
            scrolled = false;
            return;
        }
        while (MENU.consumeClick()) {
            ModNetwork.CHANNEL.sendToServer(new EquipActionPacket(EquipActionPacket.OPEN, 0));
        }
        boolean down = RELIC.isDown();
        if (!down && relicWasDown) {
            // al soltar la tecla se lanza la reliquia elegida (si entre medias se giró la rueda, solo se eligió)
            if (!scrolled) {
                List<Integer> list = keyRelics();
                if (!list.isEmpty()) {
                    int slot = list.get(Mth.clamp(selected, 0, list.size() - 1));
                    ModNetwork.CHANNEL.sendToServer(new EquipActionPacket(EquipActionPacket.USE_RELIC, slot));
                } else {
                    mc.player.displayClientMessage(Component.translatable("message.mundos_de_axelis.equip_no_relics"), true);
                }
            }
            scrolled = false;
        }
        relicWasDown = down;
    }

    /** Mantén R y gira la rueda del ratón para elegir la reliquia. */
    @SubscribeEvent
    public static void onScroll(InputEvent.MouseScrollingEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null || !RELIC.isDown()) {
            return;
        }
        List<Integer> list = keyRelics();
        event.setCanceled(true);
        scrolled = true;
        if (list.size() > 1) {
            int dir = event.getScrollDelta() > 0 ? 1 : -1;
            selected = Math.floorMod(selected + dir, list.size());
        }
    }

    // ------------------------------------------------------------------ panel

    private static float alpha;

    public static void render(GuiGraphics g, float pt, int width, int height) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null || mc.options.hideGui) {
            return;
        }
        List<Integer> keyList = keyRelics();
        if (keyList.isEmpty()) {
            return;
        }
        Font font = mc.font;
        // aparece del todo cuando mantienes la tecla de reliquias, y suave el resto del tiempo
        alpha = Mth.lerp(0.2F, alpha, RELIC.isDown() ? 1.0F : 0.78F);
        int x0 = 6;
        int y = height / 2 - 16;
        {
            List<ItemStack> rl = new ArrayList<>();
            for (int s : keyList) {
                rl.add(relics[s]);
            }
            int sel = Mth.clamp(selected, 0, rl.size() - 1);
            drawRow(g, font, p, pt, x0, y, RELIC.getTranslatedKeyMessage().getString(), rl, sel, 0x7FD2FF);
            Component name = rl.get(sel).getHoverName();
            g.drawString(font, name, x0 + 2, y + 21, ((int) (alpha * 255) << 24) | 0xFFFFFF, true);
        }
    }

    private static void drawRow(GuiGraphics g, Font font, LocalPlayer p, float pt, int x0, int y, String key, List<ItemStack> items,
                                int selected, int accent) {
        int a = (int) (alpha * 255);
        int w = 22 + items.size() * 19;
        g.fill(x0 - 2, y - 2, x0 + w, y + 20, (a * 6 / 10) << 24 | 0x0A0614);
        g.fill(x0 - 2, y - 2, x0 - 1, y + 20, (a << 24) | accent);
        g.drawString(font, key, x0 + 2, y + 5, (a << 24) | accent, true);
        int x = x0 + 20;
        for (int i = 0; i < items.size(); i++) {
            ItemStack s = items.get(i);
            if (i == selected) {
                g.fill(x - 2, y - 1, x + 18, y + 19, (a << 24) | accent);
                g.fill(x - 1, y, x + 17, y + 18, 0xFF0A0614);
            }
            g.renderItem(s, x, y + 1);
            float cd = p.getCooldowns().getCooldownPercent(s.getItem(), pt);
            if (cd > 0.0F) {
                int top = y + 1 + Mth.floor(16.0F * (1.0F - cd));
                g.fill(x, top, x + 16, y + 17, 0x80FFFFFF);
            }
            x += 19;
        }
    }

    // ------------------------------------------------------------------ botón en el inventario

    /** Botoncito junto al inventario (normal y creativo) que abre el menú de equipo, igual que la tecla G. */
    private static final class EquipButton extends net.minecraft.client.gui.components.Button {
        EquipButton() {
            super(0, 0, 20, 20, Component.translatable("gui.mundos_de_axelis.equip.button", MENU.getTranslatedKeyMessage().getString()),
                    b -> ModNetwork.CHANNEL.sendToServer(new EquipActionPacket(EquipActionPacket.OPEN, 0)), DEFAULT_NARRATION);
            setTooltip(net.minecraft.client.gui.components.Tooltip.create(getMessage()));
        }

        @Override
        protected void renderWidget(GuiGraphics g, int mx, int my, float pt) {
            boolean over = isHoveredOrFocused();
            int x = getX(), y = getY();
            g.fill(x, y, x + 20, y + 20, over ? 0xFFFFE9A8 : 0xFF8A6A2A);
            g.fill(x + 1, y + 1, x + 19, y + 19, over ? 0xFF4A3380 : 0xFF2C1D4E);
            g.fillGradient(x + 2, y + 2, x + 18, y + 18, over ? 0xFF5A3F94 : 0xFF3A2866, 0xFF140C26);
            // dibujito: una estrella y una patita (equipo = reliquia + mascota)
            int c = over ? 0xFFFFFFFF : 0xFFFFE9A8;
            g.fill(x + 9, y + 4, x + 11, y + 12, c);
            g.fill(x + 6, y + 7, x + 14, y + 9, c);
            g.fill(x + 7, y + 5, x + 8, y + 6, 0xFFB890FF);
            g.fill(x + 12, y + 5, x + 13, y + 6, 0xFFB890FF);
            g.fill(x + 6, y + 13, x + 9, y + 16, 0xFF7FD2FF);
            g.fill(x + 11, y + 13, x + 14, y + 16, 0xFFFFD27F);
        }
    }

    private static final java.util.Map<net.minecraft.client.gui.screens.Screen, EquipButton> BUTTONS = new java.util.WeakHashMap<>();

    @SubscribeEvent
    public static void onScreenInit(net.minecraftforge.client.event.ScreenEvent.Init.Post event) {
        if (event.getScreen() instanceof net.minecraft.client.gui.screens.inventory.InventoryScreen
                || event.getScreen() instanceof net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen) {
            EquipButton b = new EquipButton();
            BUTTONS.put(event.getScreen(), b);
            place(event.getScreen(), b);
        }
    }

    /** Se coloca a la derecha del cuadro del inventario, un poco por encima (y se recoloca si se abre o cierra el libro de recetas). */
    private static void place(net.minecraft.client.gui.screens.Screen s, EquipButton b) {
        if (s instanceof net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?> c) {
            b.setX(c.getGuiLeft() + c.getXSize() + 4);
            // justo encima de la esquina, para no tapar los iconos de efectos (poción) que salen a la derecha
            b.setY(c.getGuiTop() - 22);
        }
    }

    @SubscribeEvent
    public static void onScreenRender(net.minecraftforge.client.event.ScreenEvent.Render.Pre event) {
        EquipButton b = BUTTONS.get(event.getScreen());
        if (b != null) {
            place(event.getScreen(), b);
        }
    }

    @SubscribeEvent
    public static void onScreenRenderPost(net.minecraftforge.client.event.ScreenEvent.Render.Post event) {
        EquipButton b = BUTTONS.get(event.getScreen());
        if (b != null) {
            b.render(event.getGuiGraphics(), event.getMouseX(), event.getMouseY(), event.getPartialTick());
        }
    }

    @SubscribeEvent
    public static void onScreenClick(net.minecraftforge.client.event.ScreenEvent.MouseButtonPressed.Pre event) {
        EquipButton b = BUTTONS.get(event.getScreen());
        if (b != null && b.mouseClicked(event.getMouseX(), event.getMouseY(), event.getButton())) {
            event.setCanceled(true);
        }
    }
}
