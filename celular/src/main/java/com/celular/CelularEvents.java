package com.celular;

import com.celular.call.CallManager;
import com.celular.insta.Instacraft;
import com.celular.patreon.Patreon;
import com.celular.plat.Plat;
import com.celular.radio.Jams;
import com.celular.radio.Speakers;
import com.celular.radio.WorldMusic;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Eventos del servidor y los comandos (nivel de operador):
 * <ul>
 *   <li>/celular patreon agregar &lt;nombre&gt; · quitar &lt;nombre&gt; · lista</li>
 *   <li>/celular instacraft borrar &lt;jugador&gt; - borra todas sus publicaciones</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = Celular.MODID)
public final class CelularEvents {
    private CelularEvents() {
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) {
            CallManager.logout(p);
            Instacraft.logout(p.getUUID());
            WorldMusic.logout(p.getUUID());
            Jams.leave(Plat.server(p), p.getUUID(), false);
            Speakers.ownerGone(Plat.server(p), p.getUUID());
            com.celular.PhoneUse.logout(p);
            com.celular.map.Friends.logout(p.getUUID());
        }
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) {
            Speakers.sendAll(p);
            com.celular.PhoneUse.login(p);
            com.celular.msg.Messages.login(p);
        }
    }

    @SubscribeEvent
    public static void onDimension(PlayerEvent.PlayerChangedDimensionEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) {
            Speakers.ownerGone(Plat.server(p), p.getUUID());
            Speakers.sendAll(p);
        }
    }

    @SubscribeEvent
    public static void onStopping(ServerStoppingEvent e) {
        CallManager.clear();
        Jams.clear();
        Speakers.clear();
        com.celular.PhoneUse.clear(e.getServer());
        com.celular.msg.Messages.stopping(e.getServer());
        com.celular.wallet.Wallet.stopping();
    }

    @SubscribeEvent
    public static void onCommands(RegisterCommandsEvent e) {
        CommandDispatcher<CommandSourceStack> d = e.getDispatcher();
        d.register(Commands.literal("celular").requires(s -> Plat.isOp(s))
                .then(Commands.literal("patreon")
                        .then(Commands.literal("agregar").then(Commands.argument("nombre", StringArgumentType.greedyString()).executes(c -> {
                            String n = StringArgumentType.getString(c, "nombre").trim();
                            boolean ok = !n.isEmpty() && n.length() <= 40 && Patreon.add(n);
                            Celular.LOGGER.info("Celular: Patreon agregar '{}' -> {}", n, ok);
                            c.getSource().sendSuccess(() -> Component.translatable(ok ? "celular.cmd.patreon_added" : "celular.cmd.patreon_exists", n), true);
                            return ok ? 1 : 0;
                        })))
                        .then(Commands.literal("quitar").then(Commands.argument("nombre", StringArgumentType.greedyString()).executes(c -> {
                            String n = StringArgumentType.getString(c, "nombre").trim();
                            boolean ok = Patreon.remove(n);
                            c.getSource().sendSuccess(() -> Component.translatable(ok ? "celular.cmd.patreon_removed" : "celular.cmd.patreon_missing", n), true);
                            return ok ? 1 : 0;
                        })))
                        .then(Commands.literal("lista").executes(c -> {
                            String all = String.join(", ", Patreon.list());
                            Celular.LOGGER.info("Celular: lista de Patreon: {}", all);
                            c.getSource().sendSuccess(() -> Component.translatable("celular.cmd.patreon_list", all), false);
                            return 1;
                        })))
                .then(Commands.literal("musica")
                        .then(Commands.literal("borrar").then(Commands.argument("jugador", StringArgumentType.word()).executes(c -> {
                            String n = StringArgumentType.getString(c, "jugador");
                            int k = WorldMusic.clearUploader(c.getSource().getServer(), n);
                            c.getSource().sendSuccess(() -> Component.translatable("celular.cmd.music_cleared", k, n), true);
                            return k;
                        })))
                        .then(Commands.literal("total").executes(c -> {
                            int k = WorldMusic.count(c.getSource().getServer());
                            long mb = WorldMusic.usedBytes(c.getSource().getServer()) / (1024 * 1024);
                            c.getSource().sendSuccess(() -> Component.translatable("celular.cmd.music_total", k, mb), false);
                            return k;
                        })))
                .then(Commands.literal("instacraft")
                        .then(Commands.literal("borrar").then(Commands.argument("jugador", StringArgumentType.word()).executes(c -> {
                            String n = StringArgumentType.getString(c, "jugador");
                            int k = Instacraft.clearAuthor(c.getSource().getServer(), n);
                            c.getSource().sendSuccess(() -> Component.translatable("celular.cmd.insta_cleared", k, n), true);
                            return k;
                        })))
                        .then(Commands.literal("total").executes(c -> {
                            int k = Instacraft.count(c.getSource().getServer());
                            Celular.LOGGER.info("Celular: Instacraft tiene {} publicaciones", k);
                            c.getSource().sendSuccess(() -> Component.translatable("celular.cmd.insta_total", k), false);
                            return k;
                        }))));
    }
}
