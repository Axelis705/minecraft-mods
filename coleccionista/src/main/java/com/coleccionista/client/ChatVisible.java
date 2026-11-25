package com.coleccionista.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.common.MinecraftForge;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * (Cliente) Dónde está el chat en pantalla ahorita (para que los subtítulos del Coleccionista no queden debajo de él): Minecraft muestra
 * los mensajes nuevos 10 segundos abajo a la izquierda (o todos, con el chat abierto).
 */
public final class ChatVisible {
    private static final Deque<Long> MENSAJES = new ArrayDeque<>();
    private static final long DURA = 10_500;

    private ChatVisible() {
    }

    public static void init() {
        MinecraftForge.EVENT_BUS.addListener((ClientChatReceivedEvent e) -> {
            synchronized (MENSAJES) {
                MENSAJES.addLast(System.currentTimeMillis());
                while (MENSAJES.size() > 12) {
                    MENSAJES.removeFirst();
                }
            }
        });
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
