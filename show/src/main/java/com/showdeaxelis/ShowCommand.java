package com.showdeaxelis;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.showdeaxelis.data.Banks;
import com.showdeaxelis.data.ShowManager;
import com.showdeaxelis.data.StageData;
import com.showdeaxelis.data.Studio;
import com.showdeaxelis.game.Format;
import com.showdeaxelis.game.Phase;
import com.showdeaxelis.game.Show;
import java.util.UUID;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** /show: construir el estudio, recargar los bancos y probar el sistema (nivel de permiso 2). El show en si se maneja con el panel (tecla Y). */
@Mod.EventBusSubscriber(modid = ShowMod.MOD_ID)
public final class ShowCommand {
    private ShowCommand() {
    }

    @SubscribeEvent
    public static void register(RegisterCommandsEvent e) {
        register(e.getDispatcher());
    }

    private static int ok(CommandSourceStack s, String text) {
        s.sendSuccess(() -> Component.literal(text), false);
        return 1;
    }

    static void register(CommandDispatcher<CommandSourceStack> d) {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("show").requires(s -> s.hasPermission(2));
        root.executes(c -> ok(c.getSource(), "Show de Axelis: pulsa Y para abrir el panel del presentador. Comandos: escenario, publico, recargar, restaurar, parar."));
        root.then(Commands.literal("escenario").executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            Direction f = p.getDirection();
            Studio.build(p.serverLevel(), p.blockPosition().below(), f);
            ShowManager.sync();
            return ok(c.getSource(), "Estudio construido delante de ti, mirando hacia donde miras. Puedes volver a usarlo en otro lugar.");
        }));
        root.then(Commands.literal("construir").then(Commands.argument("pos", BlockPosArgument.blockPos())
                .then(Commands.argument("dir", StringArgumentType.word()).executes(c -> {
                    BlockPos pos = BlockPosArgument.getLoadedBlockPos(c, "pos");
                    Direction f = Direction.byName(StringArgumentType.getString(c, "dir"));
                    if (f == null || f.getAxis().isVertical()) {
                        f = Direction.SOUTH;
                    }
                    Studio.build(c.getSource().getLevel(), pos, f);
                    return ok(c.getSource(), "Estudio construido en " + pos.toShortString());
                }))));
        root.then(Commands.literal("publico").executes(c -> {
            StageData.Info st = StageData.get(c.getSource().getServer()).info();
            if (st == null) {
                return ok(c.getSource(), "Primero construye el estudio con /show escenario.");
            }
            Studio.spawnCrowd(c.getSource().getServer().overworld(), st);
            return ok(c.getSource(), "Publico nuevo en las gradas.");
        }));
        root.then(Commands.literal("recargar").executes(c -> ok(c.getSource(), "Bancos recargados: " + Banks.load())));
        root.then(Commands.literal("restaurar").executes(c -> {
            Banks.install(true);
            return ok(c.getSource(), "Bancos de fabrica restaurados: " + Banks.load());
        }));
        root.then(Commands.literal("parar").executes(c -> {
            ShowManager.show = null;
            ShowManager.sync();
            return ok(c.getSource(), "Show terminado.");
        }));
        root.then(Commands.literal("probar").executes(c -> {
            String r = Banks.load();
            Show s = new Show(new UUID(0, 99), "Host", false);
            s.setFormat(Format.TWO_TEAMS);
            for (int i = 0; i < 4; i++) {
                s.assign(new UUID(0, i), "J" + i, i % 2);
            }
            boolean started = s.start();
            boolean r100 = !Banks.Q100.isEmpty() && s.start100(Banks.Q100.get(0), 1);
            s.finish100();
            boolean tr = !Banks.TRIVIA.isEmpty() && s.startTrivia(Banks.TRIVIA.get(0), 10, 0);
            s.finishTrivia();
            java.util.List<UUID> ids = java.util.List.of(new UUID(0, 0), new UUID(0, 1), new UUID(0, 2), new UUID(0, 3));
            boolean imp = s.startImpostor("Prueba", "Palabra", ids, 1);
            s.finishImpostor();
            boolean tb = !Banks.TABOO.isEmpty() && s.startTaboo(Banks.TABOO.get(0).word(), Banks.TABOO.get(0).forbidden(), 30, 0);
            boolean good = started && r100 && tr && imp && tb && s.phase == Phase.TABOO;
            return ok(c.getSource(), (good ? "PRUEBA_SHOW_OK " : "PRUEBA_SHOW_FALLO ") + r);
        }));
        d.register(root);
    }
}
