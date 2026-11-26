package com.coleccionista.client;

import com.axeliscore.data.client.ClientData;
import com.axeliscore.plat.Nbt;
import com.coleccionista.evento.Pantallas;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.common.MinecraftForge;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * (Cliente) El chat durante el evento: se limpia al empezar cada momento (un discurso, una presentación o una indicación nueva del
 * Coleccionista) para que no se apilen los mensajes de antes, y dónde está en pantalla ahorita (Minecraft muestra los mensajes nuevos 10
 * segundos abajo a la izquierda, o todos con el chat abierto).
 */
public final class ChatVisible {
    private static final Deque<Long> MENSAJES = new ArrayDeque<>();
    private static final long DURA = 10_500;

    private ChatVisible() {
    }

    /** Cuándo llegó la última frase del Coleccionista al chat (para saber cuándo empieza "otro momento"). */
    private static long ultimaFrase;
    /** Cuánto tiempo sin frases del Coleccionista cuenta como otro momento (y se limpia el chat antes de la nueva). */
    private static final long OTRO_MOMENTO = 7_000;
    private static String ultimoModo = "";

    public static void init() {
        // el chat se limpia al empezar cada momento importante: un discurso, una presentación de prueba o una indicación del Coleccionista
        // después de un rato callado. Así solo se ven los mensajes de ahorita (no los de las pruebas anteriores, apilados).
        ClientData.listen(Pantallas.KEY, t -> {
            String m = Nbt.s(t, "modo");
            if ("estatica".equals(m) && !"estatica".equals(ultimoModo)) {
                limpiar();
            }
            ultimoModo = m;
        });
        ClientData.listen(com.coleccionista.prueba.Escena.KEY_PRESENTACION, t -> limpiar());
        MinecraftForge.EVENT_BUS.addListener((ClientChatReceivedEvent e) -> {
            if (e.getMessage().getString().startsWith("El Coleccionista: ")) {
                long ahora = System.currentTimeMillis();
                if (ahora - ultimaFrase > OTRO_MOMENTO) {
                    limpiar();
                }
                ultimaFrase = ahora;
            }
            synchronized (MENSAJES) {
                MENSAJES.addLast(System.currentTimeMillis());
                while (MENSAJES.size() > 12) {
                    MENSAJES.removeFirst();
                }
            }
        });
    }

    /** Borra los mensajes que se ven en el chat (no lo que uno escribió). No mientras alguien está escribiendo en el chat. */
    public static void limpiar() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.gui == null || mc.screen instanceof ChatScreen || !EventoCliente.activo()) {
            return;
        }
        mc.gui.getChat().clearMessages(false);
        synchronized (MENSAJES) {
            MENSAJES.clear();
        }
    }

    /** Cuántos renglones del chat se están viendo (0: ninguno). */
    public static int renglones() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui) {
            return 0;
        }
        ChatComponent chat = mc.gui.getChat();
        if (mc.screen instanceof ChatScreen) {
            return chat.getLinesPerPage();
        }
        long ahora = System.currentTimeMillis();
        int n = 0;
        synchronized (MENSAJES) {
            for (long t : MENSAJES) {
                if (ahora - t < DURA) {
                    n++;
                }
            }
        }
        // un mensaje largo ocupa dos renglones
        return Math.min(chat.getLinesPerPage(), n * 3 / 2 + (n > 0 ? 1 : 0));
    }

    /** Hasta dónde llega el chat a la derecha (pixeles de la interfaz), o 0 si no se ve. */
    public static int derecha() {
        if (renglones() == 0) {
            return 0;
        }
        ChatComponent chat = Minecraft.getInstance().gui.getChat();
        return (int) Math.ceil(chat.getWidth() * chat.getScale()) + 14;
    }

    /** Hasta dónde llega el chat hacia arriba (pixeles desde abajo de la pantalla), o 0 si no se ve. */
    public static int arriba() {
        int n = renglones();
        if (n == 0) {
            return 0;
        }
        ChatComponent chat = Minecraft.getInstance().gui.getChat();
        double alto = 9.0 * Minecraft.getInstance().options.chatLineSpacing().get() + 9.0;
        return 40 + (int) Math.ceil(n * Math.max(9.0, alto * 0.5 + 4.5) * chat.getScale());
    }
}
