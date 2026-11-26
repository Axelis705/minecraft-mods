package com.coleccionista.client.inicio;

import com.coleccionista.Coleccionista;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.GenericDirtMessageScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * (Cliente) La pausa del evento, sencilla: Volver al juego, Configurar ({@link ConfigurarScreen}) y Salir del evento (regresa a la
 * pantalla del Coleccionista). Lo demás de Minecraft (logros, estadísticas, abrir a LAN...) no aparece.
 */
public class PausaScreen extends Screen {
    private static final String MOD = Coleccionista.MODID;

    public PausaScreen() {
        super(Component.translatable(MOD + ".pausa.titulo"));
    }

    @Override
    protected void init() {
        int bw = 150, bh = 20, x = width / 2 - bw / 2, y = height / 2 - 30;
        addRenderableWidget(new Estilo.Boton(x, y, bw, bh, Component.translatable(MOD + ".pausa.volver"), this::onClose));
        addRenderableWidget(new Estilo.Boton(x, y + bh + 6, bw, bh, Component.translatable(MOD + ".inicio.configurar"),
                () -> minecraft.setScreen(new ConfigurarScreen(this))));
        addRenderableWidget(new Estilo.Boton(x, y + 2 * (bh + 6) + 8, bw, bh, Component.translatable(MOD + ".pausa.salir"), this::salir));
    }

    private void salir() {
        boolean local = minecraft.isLocalServer();
        Coleccionista.LOGGER.info("COLECCIONISTA_INICIO salir del evento");
        if (minecraft.level != null) {
            minecraft.level.disconnect();
        }
        if (local) {
            minecraft.clearLevel(new GenericDirtMessageScreen(Component.translatable("menu.savingLevel")));
        } else {
            minecraft.clearLevel();
        }
        minecraft.setScreen(new InicioScreen());
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        g.fillGradient(0, 0, width, height, 0xC0000000, 0xD8200508);
        int w = 190, x0 = width / 2 - w / 2, y0 = height / 2 - 64;
        Estilo.panel(g, x0, y0, x0 + w, height / 2 + 58);
        Estilo.grande(g, getTitle().getString(), width / 2F, y0 + 8, 2F, 0xFFE9D9B8, true);
        g.fill(x0 + 10, y0 + 28, x0 + w - 10, y0 + 29, Estilo.ORO_OSCURO);
        super.render(g, mx, my, pt);
    }

    @Override
    public boolean isPauseScreen() {
        return true;
    }
}
