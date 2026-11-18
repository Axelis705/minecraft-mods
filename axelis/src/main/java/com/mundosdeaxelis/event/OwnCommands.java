package com.mundosdeaxelis.event;

import com.mundosdeaxelis.MundosDeAxelis;
import net.minecraft.commands.Commands;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Comandos de prueba (solo operadores): {@code /mundos prueba ...} (ver {@link TestCommands}). */
@Mod.EventBusSubscriber(modid = MundosDeAxelis.MODID)
public final class OwnCommands {
    private OwnCommands() {
    }

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("mundos")
                .requires(source -> source.hasPermission(2))
                .then(TestCommands.build())
                .then(com.mundosdeaxelis.world.Estructuras.build()));
    }
}
