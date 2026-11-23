package com.coleccionista.evento;

import com.axeliscore.plat.Plat;
import com.coleccionista.Coleccionista;
import com.mojang.brigadier.ParseResults;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.CommandEvent;
import net.minecraftforge.event.ServerChatEvent;

import java.util.Set;

/**
 * El chat del evento: el organizador lo puede cerrar ({@code /evento chat cerrar} o el botón del panel) para que nadie rompa la
 * inmersión escribiendo. Cerrado, solo escriben el host, los organizadores y los operadores; los demás tampoco pueden mandar mensajes
 * privados ni usar /me o /say. La voz no se toca.
 */
public final class Chat {
    private static final String MOD = Coleccionista.MODID;
    /** Comandos que sirven para escribirle a otros (se bloquean con el chat cerrado). */
    private static final Set<String> MENSAJES = Set.of("msg", "tell", "w", "me", "say", "teammsg", "tm");

    private Chat() {
    }

    public static void init() {
        MinecraftForge.EVENT_BUS.addListener(Chat::alEscribir);
        MinecraftForge.EVENT_BUS.addListener(Chat::alComando);
    }

    /** ¿Puede escribir aunque el chat esté cerrado? */
    public static boolean puede(MinecraftServer s, ServerPlayer p) {
        return Plat.isOp(p) || Plat.isHost(p) || Evento.get(s).esOrganizador(p.getUUID());
    }

    public static boolean cerrado(MinecraftServer s) {
        return Evento.get(s).chatCerrado;
    }

    public static void cerrar(MinecraftServer s, boolean cerrar) {
        Evento ev = Evento.get(s);
        ev.chatCerrado = cerrar;
        ev.cambio(s);
        Anuncio.todos(s, Component.translatable(MOD + (cerrar ? ".chat.cerrado" : ".chat.abierto")).withStyle(ChatFormatting.DARK_GRAY,
                ChatFormatting.ITALIC));
        Coleccionista.LOGGER.info("COLECCIONISTA_CHAT {}", cerrar ? "cerrado" : "abierto");
    }

    private static void alEscribir(ServerChatEvent e) {
        ServerPlayer p = e.getPlayer();
        MinecraftServer s = Plat.server(p);
        if (s != null && cerrado(s) && !puede(s, p)) {
            e.setCanceled(true);
            Plat.msg(p, Component.translatable(MOD + ".chat.no_puedes").withStyle(ChatFormatting.DARK_RED), true);
        }
    }

    private static void alComando(CommandEvent e) {
        ParseResults<CommandSourceStack> pr = e.getParseResults();
        if (!(pr.getContext().getSource().getEntity() instanceof ServerPlayer p) || pr.getContext().getNodes().isEmpty()) {
            return;
        }
        String raiz = pr.getContext().getNodes().get(0).getNode().getName();
        MinecraftServer s = Plat.server(p);
        if (s != null && MENSAJES.contains(raiz) && cerrado(s) && !puede(s, p)) {
            e.setCanceled(true);
            Plat.msg(p, Component.translatable(MOD + ".chat.no_puedes").withStyle(ChatFormatting.DARK_RED), true);
        }
    }
}
