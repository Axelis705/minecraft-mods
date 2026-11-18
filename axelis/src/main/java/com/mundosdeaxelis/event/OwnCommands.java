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
                .then(Commands.literal("desbloquear")
                        .then(Commands.argument("etapa", com.mojang.brigadier.arguments.IntegerArgumentType.integer(1, 8))
                                .executes(ctx -> unlock(ctx.getSource(), com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(ctx, "etapa")))))
                .then(TestCommands.build())
                .then(com.mundosdeaxelis.world.Estructuras.build()));
    }

    /**
     * Abre las Piedras de Portal hasta esa etapa sin tener que vencer a los jefes anteriores (para probar o saltar adelante):
     * el mundo queda como si ya hubieran caído los jefes de las etapas de antes, y cada jugador conectado lo apunta en su Archivo.
     */
    private static int unlock(net.minecraft.commands.CommandSourceStack src, int etapa) {
        com.mundosdeaxelis.world.WorldTier t = com.mundosdeaxelis.world.WorldTier.get(src.getServer());
        int need = etapa - 1;
        if (t.level() < need) {
            t.set(need);
        }
        for (net.minecraft.server.level.ServerPlayer sp : src.getServer().getPlayerList().getPlayers()) {
            if (need > 0) {
                com.mundosdeaxelis.table.Essence.registerProgress(sp, need); // apunta todas las etapas de una vez (un solo aviso)
            }
        }
        int lv = t.level();
        src.sendSuccess(() -> net.minecraft.network.chat.Component.literal("Listo: puedes usar las Piedras de Portal hasta la etapa " + etapa
                + " (nivel del mundo " + lv + "). El mundo normal no se vuelve más difícil."), true);
        return 1;
    }
}
