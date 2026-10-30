package com.scardax.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.scardax.Scardax;
import com.scardax.net.RulesPacket;
import com.scardax.net.ScardaxNet;
import com.scardax.rules.SpawnRules;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** /scardax (o /quitarmobs): abre la pantalla de control de mobs. Solo operadores (nivel 2). */
@Mod.EventBusSubscriber(modid = Scardax.MODID)
public final class ScardaxCommand {
    private ScardaxCommand() {
    }

    @SubscribeEvent
    public static void onRegister(RegisterCommandsEvent e) {
        CommandDispatcher<CommandSourceStack> d = e.getDispatcher();
        for (String name : new String[]{"scardax", "quitarmobs"}) {
            d.register(Commands.literal(name)
                    .requires(s -> s.hasPermission(2))
                    .executes(ctx -> open(ctx.getSource())));
        }
    }

    private static int open(CommandSourceStack src) throws CommandSyntaxException {
        ServerPlayer sp = src.getPlayerOrException();
        SpawnRules rules = SpawnRules.of(src.getServer());
        ScardaxNet.sendTo(sp, new RulesPacket(true, rules.strict(), rules.all()));
        return 1;
    }
}
