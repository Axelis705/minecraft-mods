package com.coleccionista.client;

import com.axeliscore.plat.Ids;
import com.coleccionista.Coleccionista;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.PostChain;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.common.MinecraftForge;

/**
 * (Cliente) El filtro de terror del evento (un "shader" propio, sin instalar nada): un poco menos de color, tono frío, más contraste,
 * oscuro en las orillas y grano de película. Se pone solo mientras hay un evento armado; el organizador lo puede apagar en su panel. Si
 * la tarjeta de video no puede con él, se apaga solo y el juego sigue normal.
 */
public final class Filtro {
    public static boolean activo = true;
    private static PostChain cadena;
    private static int ancho, alto;
    private static boolean fallo;

    private Filtro() {
    }

    public static void init() {
        MinecraftForge.EVENT_BUS.addListener(Filtro::alTerminarMundo);
    }

    private static void alTerminarMundo(RenderLevelStageEvent e) {
        if (e.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (!activo || fallo || mc.level == null || !(EventoCliente.activo() || EventoCliente.soyOrganizador())) {
            return;
        }
        try {
            if (cadena == null) {
                cadena = new PostChain(mc.getTextureManager(), mc.getResourceManager(), mc.getMainRenderTarget(),
                        Ids.rl(Coleccionista.MODID, "shaders/post/terror.json"));
                ancho = -1;
            }
            int w = mc.getWindow().getWidth(), h = mc.getWindow().getHeight();
            if (w != ancho || h != alto) {
                cadena.resize(w, h);
                ancho = w;
                alto = h;
            }
            RenderSystem.disableBlend();
            RenderSystem.disableDepthTest();
            RenderSystem.resetTextureMatrix();
            cadena.process(e.getPartialTick());
            mc.getMainRenderTarget().bindWrite(false);
            RenderSystem.enableDepthTest();
        } catch (Exception ex) {
            fallo = true;
            Coleccionista.LOGGER.warn("filtro de terror apagado (no se pudo cargar): {}", ex.toString());
        }
    }
}
