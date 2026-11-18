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

    @Override
    protected void init() {
        int w = 220;
        int x = this.width / 2 - w / 2;
        int y = this.height / 4 + 8;
        toggle(x, y, w, "souls", ClientConfig.SHOW_SOULS, false);
        toggle(x, y + 24, w, "map", ClientConfig.SHOW_MAP, false);
        toggle(x, y + 48, w, "few_notices", ClientConfig.FEW_NOTICES, false);
        toggle(x, y + 72, w, "music", ClientConfig.DUNGEON_MUSIC, false);
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose()).bounds(this.width / 2 - 100, y + 112, 200, 20).build());
    }

    private void toggle(int x, int y, int w, String label, ForgeConfigSpec.BooleanValue value, boolean unused) {
        addRenderableWidget(CycleButton.onOffBuilder(value.get()).create(x, y, w, 20, Component.translatable("gui.mundos_de_axelis.options." + label), (b, v) -> {
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
