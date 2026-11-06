package com.mundosdeaxelis.estructuras;

import com.mundosdeaxelis.world.Estructuras;
import net.minecraft.commands.Commands;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Mod de comprobacion: registra /mundos estructura ... igual que lo hace Mundos de Axelis, para probar el codigo en un servidor real. */
@Mod("mundos_de_axelis")
@Mod.EventBusSubscriber
public final class CheckMod {
    @SubscribeEvent
    public static void register(RegisterCommandsEvent e) {
        e.getDispatcher().register(Commands.literal("mundos").requires(s -> s.hasPermission(2)).then(Estructuras.build()));
    }
}
