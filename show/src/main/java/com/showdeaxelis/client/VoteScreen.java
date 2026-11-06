package com.showdeaxelis.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;

/** La ventana para votar al impostor con un clic (modo voz: no hace falta escribir en el chat). */
public final class VoteScreen extends Screen {
    public VoteScreen() {
        super(Component.literal("¿Quien es el impostor?"));
    }

    public static void open() {
        Minecraft.getInstance().setScreen(new VoteScreen());
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    protected void init() {
        ListTag pl = ShowClient.state.getList("players", 10);
        String me = Minecraft.getInstance().player == null ? "" : Minecraft.getInstance().player.getName().getString();
        int n = 0;
        for (int i = 0; i < pl.size(); i++) {
            CompoundTag p = pl.getCompound(i);
            if (!p.getString("n").equals(me)) {
                n++;
            }
        }
        int bw = 200;
        int y = height / 2 - (n * 24) / 2;
        for (int i = 0; i < pl.size(); i++) {
            String name = pl.getCompound(i).getString("n");
            if (name.equals(me)) {
                continue;
            }
            addRenderableWidget(Button.builder(Component.literal(name), b -> {
                ShowClient.send("vote", name, 0);
                onClose();
            }).bounds(width / 2 - bw / 2, y, bw, 20).build());
            y += 24;
        }
        addRenderableWidget(Button.builder(Component.literal("Cerrar (sin votar todavia)"), b -> onClose()).bounds(width / 2 - bw / 2, y + 6, bw, 20).build());
    }

    @Override
    public void tick() {
        CompoundTag im = ShowClient.state.getCompound("imp");
        if (!ShowClient.state.getString("phase").equals("IMPOSTOR") || !im.getString("stage").equals("VOTE") || im.getBoolean("iVoted")) {
            onClose();
        }
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        g.fill(0, 0, width, height, 0x90000000);
        g.drawCenteredString(font, "¿Quien es el impostor? Elige a quien acusas", width / 2, height / 2 - 70 - (Math.max(0, ShowClient.state.getList("players", 10).size() - 1) * 12), 0xFFFFC107);
        super.render(g, mx, my, pt);
    }
}
