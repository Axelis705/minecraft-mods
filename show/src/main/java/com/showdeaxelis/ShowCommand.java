package com.showdeaxelis;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.showdeaxelis.data.Banks;
import com.showdeaxelis.data.Displays;
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
            ShowManager.screensNow(p.getServer());
            ShowManager.sync();
            return ok(c.getSource(), "Estudio construido delante de ti, mirando hacia donde miras. Puedes volver a usarlo en otro lugar.");
        }));
        root.then(Commands.literal("construir").then(Commands.argument("pos", BlockPosArgument.blockPos())
                .then(Commands.argument("dir", StringArgumentType.word()).executes(c -> {
                    BlockPos pos = BlockPosArgument.getBlockPos(c, "pos");
                    Direction f = Direction.byName(StringArgumentType.getString(c, "dir"));
                    if (f == null || f.getAxis().isVertical()) {
                        f = Direction.SOUTH;
                    }
                    Studio.build(c.getSource().getLevel(), pos, f);
                    ShowManager.screensNow(c.getSource().getServer());
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
        root.then(Commands.literal("pantalla").then(Commands.literal("girar").executes(c -> {
            Displays.flip = !Displays.flip;
            Displays.refresh();
            return ok(c.getSource(), "Pantalla del estudio volteada. Si el texto se veia al reves o de espaldas, ahora deberia verse bien.");
        })));
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
            s.finishTaboo();
            boolean dw = !Banks.DIBUJOS.isEmpty() && s.startDraw(Banks.DIBUJOS.get(0), 30, 0, null);
            boolean good = started && r100 && tr && imp && tb && dw && s.phase == Phase.DRAW;
            return ok(c.getSource(), (good ? "PRUEBA_SHOW_OK " : "PRUEBA_SHOW_FALLO ") + r);
        }));
        root.then(Commands.literal("probarbots").executes(c -> {
            Banks.load();
            return ok(c.getSource(), testBots());
        }));
        d.register(root);
    }

    /** Juega solo, en silencio, con bots: trivia, dibujo, impostor, adivina la palabra y 100 mexicanos. Devuelve PRUEBA_BOTS_OK o lo que fallo. */
    private static String testBots() {
        com.showdeaxelis.data.Bots.quiet = true;
        com.showdeaxelis.data.Bots.reset();
        StringBuilder bad = new StringBuilder();
        try {
            Show s = new Show(new UUID(0, 98), "Host", false);
            s.manual = false;
            s.setFormat(Format.FFA);
            if (com.showdeaxelis.data.Bots.fill(s) < 4 || !s.start()) {
                return "PRUEBA_BOTS_FALLO no se pudo armar el show";
            }
            int t = 0;
            // trivia
            s.startTrivia(Banks.TRIVIA.get(0), 20, t);
            for (int i = 0; i < 500 && !s.t.revealed; i++, t++) {
                com.showdeaxelis.data.Bots.tick(s, t);
                s.tick(t);
            }
            if (!s.t.revealed || s.t.answers.isEmpty()) {
                bad.append(" trivia");
            }
            s.finishTrivia();
            // dibujo
            s.startDraw("casa", 20, t, null);
            for (int i = 0; i < 600 && !s.draw.over; i++, t++) {
                com.showdeaxelis.data.Bots.tick(s, t);
                s.tick(t);
            }
            if (!s.draw.over || s.draw.strokes.isEmpty()) {
                bad.append(" dibujo");
            }
            s.finishDraw();
            // impostor
            java.util.List<UUID> ids = new java.util.ArrayList<>();
            for (var p : s.players) {
                ids.add(p.id);
            }
            s.startImpostor("Prueba", "Palabra", ids, 0);
            for (int i = 0; i < 4000 && s.imp.stage == com.showdeaxelis.game.ImpostorGame.Stage.CLUES; i++, t++) {
                com.showdeaxelis.data.Bots.tick(s, t);
            }
            s.impostorStartVote();
            for (int i = 0; i < 1000 && s.imp.votes.size() < s.players.size(); i++, t++) {
                com.showdeaxelis.data.Bots.tick(s, t);
            }
            if (s.imp.votes.size() < s.players.size()) {
                bad.append(" impostor");
            }
            s.impostorTally();
            for (int i = 0; i < 600 && s.imp.stage == com.showdeaxelis.game.ImpostorGame.Stage.GUESS; i++, t++) {
                com.showdeaxelis.data.Bots.tick(s, t);
            }
            s.finishImpostor();
            // adivina la palabra
            s.startTaboo("Playa", java.util.List.of("mar"), 30, t);
            for (int i = 0; i < 700 && !s.taboo.over; i++, t++) {
                com.showdeaxelis.data.Bots.tick(s, t);
                s.tick(t);
            }
            if (!s.taboo.over) {
                bad.append(" taboo");
            }
            s.finishTaboo();
            // 100 mexicanos (2 equipos de bots): representantes, campana, cara a cara, jugar
            Show h = new Show(new UUID(0, 97), "Host", false);
            h.manual = false;
            h.autoWrong = true;
            h.setFormat(Format.TWO_TEAMS);
            com.showdeaxelis.data.Bots.reset();
            if (com.showdeaxelis.data.Bots.fill(h) < 4 || !h.start() || !h.start100(Banks.Q100.get(0), 1)) {
                bad.append(" 100mex-inicio");
            } else {
                boolean buzzed = false;
                boolean chose = false;
                h.callReps();
                h.announceQuestion();
                for (int i = 0; i < 3000 && !chose; i++, t++) {
                    com.showdeaxelis.data.Bots.tick(h, t);
                    h.tick(t);
                    for (var e : h.drain()) {
                        buzzed |= e.type().equals("buzz");
                    }
                    if (h.r.stage == com.showdeaxelis.game.Round100.CHOOSE) {
                        chose = true;
                        h.choosePlay(true);
                    }
                }
                for (int i = 0; i < 3000 && h.r.stage != com.showdeaxelis.game.Round100.END; i++, t++) {
                    com.showdeaxelis.data.Bots.tick(h, t);
                    h.tick(t);
                    h.drain();
                }
                if (!buzzed || !chose || h.r.stage != com.showdeaxelis.game.Round100.END) {
                    bad.append(" 100mex(buzz=").append(buzzed).append(",cara=").append(chose).append(",etapa=").append(h.r.stage).append(")");
                }
            }
        } catch (Throwable e) {
            bad.append(" excepcion:").append(e);
        } finally {
            com.showdeaxelis.data.Bots.quiet = false;
            com.showdeaxelis.data.Bots.reset();
        }
        return bad.length() == 0 ? "PRUEBA_BOTS_OK" : "PRUEBA_BOTS_FALLO" + bad;
    }
}
