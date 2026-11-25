package com.coleccionista.evento;

import com.axeliscore.data.PlayerStore;
import com.axeliscore.fx.Fx;
import com.axeliscore.plat.Plat;
import com.coleccionista.Coleccionista;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

import java.util.List;

/** Lo que se le dice a todos: mensajes en el chat, sonidos, títulos y las escenas de eliminación y del ganador. */
public final class Anuncio {
    private static final String MOD = Coleccionista.MODID;
    /** Rojo del Coleccionista. */
    public static final int ROJO = 0xB3121A;

    private Anuncio() {
    }

    public static void todos(MinecraftServer s, Component c) {
        for (ServerPlayer p : s.getPlayerList().getPlayers()) {
            Plat.msg(p, c, false);
        }
    }

    /** Un aviso solo para quien organiza (operadores, el anfitrión y los organizadores), con un "ding". */
    public static void organizadores(MinecraftServer s, Component c) {
        Evento ev = Evento.get(s);
        for (ServerPlayer p : s.getPlayerList().getPlayers()) {
            if (Plat.isOp(p) || Plat.isHost(p) || ev.esOrganizador(p.getUUID())) {
                Plat.msg(p, Component.literal("[Organizador] ").withStyle(ChatFormatting.DARK_GRAY).append(c), false);
                p.playNotifySound(net.minecraft.sounds.SoundEvents.NOTE_BLOCK_PLING.get(), SoundSource.MASTER, 0.7F, 1.4F);
            }
        }
    }

    public static void sonido(MinecraftServer s, SoundEvent e, float vol, float tono) {
        for (ServerPlayer p : s.getPlayerList().getPlayers()) {
            p.playNotifySound(e, SoundSource.MASTER, vol, tono);
        }
    }

    /** "Pieza 07 · Fulano" en rojo. */
    public static MutableComponent nombre(Pieza p) {
        return Component.literal("Pieza " + p.etiqueta() + " · " + p.nombre).withStyle(ChatFormatting.RED, ChatFormatting.BOLD);
    }

    /** Una frase del Coleccionista en el chat. */
    public static MutableComponent coleccionista(Component texto) {
        return Component.literal("El Coleccionista: ").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD)
                .append(texto.copy().withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
    }

    private static void aviso(MinecraftServer s, String tipo, Pieza p, int quedan, String motivo) {
        CompoundTag t = p.save();
        t.putString("tipo", tipo);
        t.putInt("quedan", quedan);
        t.putString("motivo", motivo == null ? "" : motivo);
        t.putLong("cuando", s.overworld().getGameTime());
        PlayerStore.Sync.toAll(s, Evento.KEY_AVISO, t);
    }

    static void eliminado(MinecraftServer s, Evento ev, Pieza p, int quedan) {
        MutableComponent m = Component.literal("☠ ").withStyle(ChatFormatting.DARK_RED).append(nombre(p))
                .append(Component.translatable(MOD + ".chat.eliminado").withStyle(ChatFormatting.GRAY));
        if (!p.motivo.isEmpty()) {
            m.append(Component.literal(" (" + p.motivo + ")").withStyle(ChatFormatting.DARK_GRAY));
        }
        m.append(Component.translatable(MOD + ".chat.quedan", quedan).withStyle(ChatFormatting.DARK_GRAY));
        todos(s, m);
        sonido(s, SoundEvents.BELL_BLOCK, 1.0F, 0.5F);
        sonido(s, SoundEvents.SOUL_ESCAPE, 1.0F, 0.6F);
        aviso(s, "eliminado", p, quedan, p.motivo);
    }

    static void ganador(MinecraftServer s, Evento ev, Pieza g) {
        todos(s, coleccionista(Component.translatable(MOD + ".chat.ganador", g.nombre)));
        for (ServerPlayer p : s.getPlayerList().getPlayers()) {
            Fx.title(p, Component.literal("Pieza " + g.etiqueta() + " · " + g.nombre),
                    Component.translatable(MOD + ".titulo.ganador"), 0xFFD27A);
        }
        sonido(s, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1.0F, 0.8F);
        sonido(s, SoundEvents.BELL_BLOCK, 1.0F, 1.0F);
        aviso(s, "ganador", g, 1, "");
    }

    static void revivido(MinecraftServer s, Pieza p) {
        todos(s, Component.literal("↺ ").withStyle(ChatFormatting.GREEN).append(nombre(p).withStyle(ChatFormatting.GREEN))
                .append(Component.translatable(MOD + ".chat.revivido").withStyle(ChatFormatting.GRAY)));
    }

    static void equipos(MinecraftServer s, List<List<Pieza>> eq) {
        boolean parejas = !eq.isEmpty() && eq.get(0).size() <= 2;
        todos(s, coleccionista(Component.translatable(MOD + (parejas ? ".chat.parejas" : ".chat.equipos"))));
        for (int i = 0; i < eq.size(); i++) {
            MutableComponent m = Component.literal("  " + (parejas ? "Pareja " : "Equipo ") + (i + 1) + ": ")
                    .withStyle(ChatFormatting.GOLD);
            boolean primero = true;
            for (Pieza p : eq.get(i)) {
                if (!primero) {
                    m.append(Component.literal(", ").withStyle(ChatFormatting.DARK_GRAY));
                }
                m.append(Component.literal(p.etiqueta() + " " + p.nombre).withStyle(ChatFormatting.WHITE));
                primero = false;
            }
            todos(s, m);
        }
        sonido(s, SoundEvents.UI_BUTTON_CLICK.get(), 0.6F, 0.7F);
    }

    static void pausa(MinecraftServer s, boolean si) {
        for (ServerPlayer p : s.getPlayerList().getPlayers()) {
            if (si) {
                Fx.title(p, Component.translatable(MOD + ".titulo.pausa"), Component.translatable(MOD + ".titulo.pausa_sub"), 0xD8D8D8);
            }
        }
        sonido(s, si ? SoundEvents.BEACON_DEACTIVATE : SoundEvents.BEACON_ACTIVATE, 0.8F, 1.0F);
    }

    static void tiempo(MinecraftServer s) {
        todos(s, Component.literal("⏱ ").withStyle(ChatFormatting.RED).append(Component.translatable(MOD + ".chat.tiempo")
                .withStyle(ChatFormatting.RED, ChatFormatting.BOLD)));
        sonido(s, SoundEvents.BELL_BLOCK, 1.0F, 0.7F);
    }

    /** Lo que escribe el organizador como si lo dijera el Coleccionista. */
    public static void decir(MinecraftServer s, String texto) {
        todos(s, coleccionista(Component.literal(texto)));
        sonido(s, SoundEvents.AMBIENT_CAVE.get(), 0.6F, 0.8F);
    }
}
