package com.axeliscore;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** /axeliscore: muestra la versión del Core y qué mods lo usan (y, para las pruebas, deja una línea en el registro). */
@Mod.EventBusSubscriber(modid = AxelisCore.MODID)
public final class CoreCommands {
    private CoreCommands() {
    }

    @SubscribeEvent
    public static void register(RegisterCommandsEvent e) {
        CommandDispatcher<CommandSourceStack> d = e.getDispatcher();
        d.register(Commands.literal("axeliscore").executes(c -> {
            String users = String.join(", ", CoreUsers.list());
            c.getSource().sendSuccess(() -> Component.literal(AxelisCore.version() + (users.isEmpty() ? "" : " · usado por: " + users)), false);
            AxelisCore.LOGGER.info("AXCORE_INFO {} usuarios=[{}]", AxelisCore.version(), users);
            return 1;
        }));
    }
}
