package com.coleccionista.client;

import com.axeliscore.forma.client.FormasCliente;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;

/**
 * (Cliente) Para las piezas, mientras dura el evento: solo primera persona (F5 no hace nada: nadie se ve la espalda, ni la etiqueta que
 * lleve ahí) y, en lugar del inventario, el "bolsillo": solo la fila de abajo, con el logo del Coleccionista ({@link BolsilloScreen}).
 * El organizador (que no es pieza) y quien está transformado (Cascabel) sí pueden cambiar la cámara.
 */
public final class SoloPrimera {
    /** Para las pruebas automáticas (fotos en tercera persona). */
    public static boolean permitir;

    private SoloPrimera() {
    }

    public static void init() {
        MinecraftForge.EVENT_BUS.addListener(SoloPrimera::tick);
        MinecraftForge.EVENT_BUS.addListener(SoloPrimera::pantalla);
    }

    /** ¿Soy una pieza del evento (viva o fantasma) y no estoy transformado? */
    public static boolean aplica() {
        Minecraft mc = Minecraft.getInstance();
        return !permitir && mc.player != null && EventoCliente.activo() && EventoCliente.yo() != null && !FormasCliente.transformado(mc.player);
    }

    private static void tick(TickEvent.ClientTickEvent e) {
        Minecraft mc = Minecraft.getInstance();
        if (!aplica()) {
            return;
        }
        if (e.phase == TickEvent.Phase.START) {
            // la tecla F5 se "come" antes de que el juego la use
            while (mc.options.keyTogglePerspective.consumeClick()) {
                // nada
            }
        }
        if (mc.options.getCameraType() != CameraType.FIRST_PERSON) {
            mc.options.setCameraType(CameraType.FIRST_PERSON);
        }
    }

    private static void pantalla(ScreenEvent.Opening e) {
        Minecraft mc = Minecraft.getInstance();
        if (!aplica() || mc.player == null || mc.player.isCreative() || mc.player.isSpectator()) {
            return;
        }
        if (e.getNewScreen() instanceof InventoryScreen || e.getNewScreen() instanceof CreativeModeInventoryScreen) {
            // si el arma me apunta y no he marcado mi símbolo, la E abre eso
            e.setNewScreen(CajaCliente.elige() ? new EligeSimboloScreen() : new BolsilloScreen());
        }
    }
}
