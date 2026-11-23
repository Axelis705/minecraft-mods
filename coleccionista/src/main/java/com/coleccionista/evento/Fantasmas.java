package com.coleccionista.evento;

import com.axeliscore.plat.Plat;
import com.coleccionista.Coleccionista;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.GameType;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.CommandEvent;
import net.minecraftforge.event.ServerChatEvent;

import java.util.Locale;
import java.util.Set;

/**
 * Los eliminados quedan como fantasmas: flotan y ven todo (modo espectador), pero no tocan nada, los vivos no los ven y no les pueden
 * decir nada (ni por el chat ni por mensajes privados; en la voz van a su propio canal). Así nadie ayuda a nadie desde afuera.
 */
public final class Fantasmas {
    private static final String MOD = Coleccionista.MODID;
    /** Comandos con los que un fantasma podría pasarle información a un vivo. */
    private static final Set<String> BLOQUEADOS = Set.of("msg", "tell", "w", "me", "teammsg", "tm", "say", "tellraw", "title");

    private Fantasmas() {
    }

    public static void init() {
        MinecraftForge.EVENT_BUS.addListener(Fantasmas::chat);
        MinecraftForge.EVENT_BUS.addListener(Fantasmas::comando);
    }

    /** La pieza ya es fantasma: el jugador pasa a espectador; el bot se desvanece. */
    static void convertir(MinecraftServer s, Evento ev, Pieza p) {
        Entity e = Evento.entidad(s, p);
        if (e != null) {
            Cuerpos.dejar(e, p);
        }
        if (e instanceof ServerPlayer sp) {
            sp.setGameMode(GameType.SPECTATOR);
            Plat.msg(sp, Component.translatable(MOD + ".fantasma.eres").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), false);
        } else if (e instanceof com.coleccionista.bot.PiezaBot b) {
            if (e.level() instanceof ServerLevel l) {
                l.sendParticles(ParticleTypes.SOUL, e.getX(), e.getY() + 1.0, e.getZ(), 24, 0.3, 0.6, 0.3, 0.02);
                l.sendParticles(ParticleTypes.LARGE_SMOKE, e.getX(), e.getY() + 0.8, e.getZ(), 10, 0.25, 0.4, 0.25, 0.01);
            }
            b.fantasma(true);
        }
        com.coleccionista.voz.Voz.actualizar(s);
        if (e instanceof ServerPlayer sp && com.coleccionista.voz.VozPuente.disponible) {
            Plat.msg(sp, Component.translatable(MOD + ".fantasma.voz").withStyle(ChatFormatting.DARK_AQUA, ChatFormatting.ITALIC), false);
        }
    }

    /** Vuelve a ser jugador normal (al revivir o al terminar el evento). */
    static void restaurar(MinecraftServer s, Pieza p) {
        ServerPlayer sp = s.getPlayerList().getPlayer(p.id);
        if (sp != null && sp.isSpectator()) {
            sp.setGameMode(GameType.ADVENTURE);
        }
        if (p.bot && Evento.entidad(s, p) instanceof com.coleccionista.bot.PiezaBot b) {
            b.fantasma(false);
        }
        com.coleccionista.voz.Voz.actualizar(s);
    }

    /** Al conectarse: si ya era fantasma, sigue siéndolo. */
    static void alEntrar(Evento ev, ServerPlayer p) {
        Pieza x = ev.pieza(p);
        if (x != null && !x.vivo() && !p.isSpectator()) {
            p.setGameMode(GameType.SPECTATOR);
        }
    }

    /** Lo que escribe un fantasma solo lo leen los otros fantasmas y los organizadores. */
    private static void chat(ServerChatEvent e) {
        ServerPlayer p = e.getPlayer();
        Evento ev = Evento.get(p.server);
        if (!ev.esFantasma(p.getUUID())) {
            return;
        }
        e.setCanceled(true);
        Component m = Component.literal("[Fantasma] ").withStyle(ChatFormatting.DARK_AQUA)
                .append(Component.literal(p.getGameProfile().getName() + ": ").withStyle(ChatFormatting.AQUA))
                .append(e.getMessage().copy().withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        for (ServerPlayer o : p.server.getPlayerList().getPlayers()) {
            if (ev.esFantasma(o.getUUID()) || ev.esOrganizador(o.getUUID()) || (ev.pieza(o) == null && Plat.isOp(o))) {
                Plat.msg(o, m, false);
            }
        }
    }

    private static void comando(CommandEvent e) {
        CommandSourceStack src = e.getParseResults().getContext().getSource();
        if (!(src.getEntity() instanceof ServerPlayer p) || !Evento.get(p.server).esFantasma(p.getUUID())) {
            return;
        }
        String cmd = e.getParseResults().getReader().getString().trim();
        if (cmd.startsWith("/")) {
            cmd = cmd.substring(1);
        }
        String raiz = cmd.split(" ", 2)[0].toLowerCase(Locale.ROOT);
        if (raiz.startsWith("minecraft:")) {
            raiz = raiz.substring("minecraft:".length());
        }
        if (BLOQUEADOS.contains(raiz)) {
            e.setCanceled(true);
            Plat.msg(p, Component.translatable(MOD + ".fantasma.no_hablar").withStyle(ChatFormatting.RED), false);
        }
    }
}
