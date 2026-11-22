package com.mundosdeaxelis.client;

import com.mundosdeaxelis.ClientConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.common.ForgeConfigSpec;

/** Opciones propias de Mundos de Axelis (se abren con el cuadrito del menú de pausa o desde la lista de mods). */
public class MundosOptionsScreen extends Screen {
    private final Screen parent;

    public MundosOptionsScreen(Screen parent) {
        super(Component.translatable("gui.mundos_de_axelis.options"));
        this.parent = parent;
    }

    /** Botón "Configurar" de la lista de mods: abre esta misma pantalla. Solo se llama en el cliente. */
    public static void registerConfigButton(net.minecraftforge.fml.ModLoadingContext context) {
        context.registerExtensionPoint(net.minecraftforge.client.ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new net.minecraftforge.client.ConfigScreenHandler.ConfigScreenFactory((mc, parent) -> new MundosOptionsScreen(parent)));
    }

    @Override
    protected void init() {
        // dos columnas de interruptores; al pasar el ratón cada uno explica qué hace
        int w = 150;
        int x0 = this.width / 2 - w - 5, x1 = this.width / 2 + 5;
        int y = this.height / 4 + 8;
        toggle(x0, y, w, "souls", ClientConfig.SHOW_SOULS);
        toggle(x1, y, w, "map", ClientConfig.SHOW_MAP);
        toggle(x0, y + 24, w, "few_notices", ClientConfig.FEW_NOTICES);
        toggle(x1, y + 24, w, "music", ClientConfig.DUNGEON_MUSIC);
        toggle(x0, y + 48, w, "shake", ClientConfig.SCREEN_SHAKE);
        toggle(x1, y + 48, w, "flashes", ClientConfig.FLASHES);
        toggle(x0, y + 72, w, "attack_camera", ClientConfig.ATTACK_CAMERA);
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose()).bounds(this.width / 2 - 100, y + 108, 200, 20).build());
    }

    private void toggle(int x, int y, int w, String key, ForgeConfigSpec.BooleanValue value) {
        String k = "gui.mundos_de_axelis.options." + key;
        addRenderableWidget(CycleButton.onOffBuilder(value.get())
                .withTooltip(v -> net.minecraft.client.gui.components.Tooltip.create(Component.translatable(k + ".tip")))
                .create(x, y, w, 20, Component.translatable(k), (b, v) -> {
                    value.set(v);
                    ClientConfig.SPEC.save();
                }));
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        renderBackground(g);
        g.drawCenteredString(this.font, this.title, this.width / 2, this.height / 4 - 16, 0xFFD27A);
        super.render(g, mx, my, pt);
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(this.parent);
    }
}
