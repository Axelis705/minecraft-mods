package com.mundosdeaxelis.world;

import com.mundosdeaxelis.network.ModNetwork;
import com.mundosdeaxelis.network.NoticePacket;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerPlayer;

/** Títulos grandes en pantalla. */
public final class Titles {
    private Titles() {
    }

    public static void show(ServerPlayer player, String title, String subtitle) {
        player.connection.send(new ClientboundSetTitlesAnimationPacket(15, 110, 30));
        player.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal(subtitle)));
        player.connection.send(new ClientboundSetTitleTextPacket(Component.literal(title)));
    }

    /** Aviso pequeño en el panel de arriba a la derecha (unos 10 s) para lo que no es lo bastante importante como para un título gigante. */
    public static void hint(ServerPlayer player, String title, String subtitle) {
        notice(player, title, subtitle, colorFor(title), 200);
    }

    /** Tarjeta en el panel de avisos: título en negrita, texto opcional debajo, color del borde y duración en ticks. */
    public static void notice(ServerPlayer player, String title, String subtitle, int rgb, int ticks) {
        ModNetwork.sendTo(player, new NoticePacket(title, subtitle, rgb, ticks));
    }

    /** Mensaje de una línea que se queda varios segundos en el panel de avisos. */
    public static void actionBarLong(ServerPlayer player, String text, int seconds) {
        notice(player, text, "", 0x9FD8FF, seconds * 20 + 80);
    }

    /** Ficha de «lo que obtienes» (abajo a la derecha) con el icono exacto del objeto. */
    public static void loot(ServerPlayer player, net.minecraft.world.item.Item item, int count) {
        net.minecraft.resources.ResourceLocation id = net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(item);
        if (id != null) {
            ModNetwork.sendTo(player, new com.mundosdeaxelis.network.LootPacket(id.toString(), count));
        }
    }

    /** Ficha de Esencia recibida. */
    public static void lootEssence(ServerPlayer player, int count) {
        ModNetwork.sendTo(player, new com.mundosdeaxelis.network.LootPacket("", count));
    }

    private static int colorFor(String title) {
        String t = title == null ? "" : title.toLowerCase();
        if (t.contains("superada") || t.contains("despejada")) {
            return 0x7BD66B;
        }
        if (t.contains("sellada") || t.contains("losas")) {
            return 0xFFC857;
        }
        if (t.contains("runas")) {
            return 0xC79BFF;
        }
        return 0x8FD8FF;
    }

    /** Mensaje pequeño: el cliente lo recoge de la barra de acción y lo muestra en el panel de avisos. */
    public static void actionBar(ServerPlayer player, String text) {
        player.displayClientMessage(Component.literal(text), true);
    }
}
