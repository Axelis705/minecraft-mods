package com.axeliscore.ui;

import com.axeliscore.net.CoreNet;
import com.axeliscore.net.Txt;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.IntConsumer;

/**
 * Diálogos con personajes, desde el servidor: abre una ventana con el retrato de quien habla (la cara de su piel), el texto que se va
 * escribiendo y botones de respuesta. La respuesta vuelve al servidor:
 * <pre>
 *     Dialogs.show(player, Component.literal("Nilo"), skin, Component.literal("¿Quieres aprender a pescar?"),
 *             List.of(Component.literal("¡Sí!"), Component.literal("Ahora no")), choice -> { ... });
 * </pre>
 * Sin respuestas sale un solo botón "Seguir". Si el jugador cierra la ventana, llega -1.
 */
public final class Dialogs {
    public static final int THEME_PERGAMINO = 0, THEME_NOCHE = 1, THEME_MAR = 2;
    private static final Map<UUID, Map<Integer, IntConsumer>> PENDING = new ConcurrentHashMap<>();
    private static int nextId = 1;

    private Dialogs() {
    }

    public static void show(ServerPlayer p, Component speaker, ResourceLocation skin, Component text, List<Component> options,
            IntConsumer onChoice) {
        show(p, speaker, skin, text, options, THEME_PERGAMINO, onChoice);
    }

    public static synchronized void show(ServerPlayer p, Component speaker, ResourceLocation skin, Component text, List<Component> options,
            int theme, IntConsumer onChoice) {
        int id = nextId++;
        PENDING.computeIfAbsent(p.getUUID(), k -> new ConcurrentHashMap<>()).put(id, onChoice == null ? c -> {
        } : onChoice);
        List<String> o = new ArrayList<>();
        for (Component c : options) {
            o.add(Txt.pack(c));
        }
        CoreNet.NET.toPlayer(p, new CoreNet.DialogMsg(id, Txt.pack(speaker), skin == null ? "" : skin.toString(), Txt.pack(text), o, theme));
    }

    /** Llega la respuesta del cliente. */
    public static void reply(ServerPlayer p, int id, int choice) {
        Map<Integer, IntConsumer> m = PENDING.get(p.getUUID());
        IntConsumer cb = m == null ? null : m.remove(id);
        if (cb != null) {
            cb.accept(choice);
        }
    }

    /** Al salir del servidor se olvidan sus diálogos pendientes. */
    public static void forget(UUID player) {
        PENDING.remove(player);
    }
}
