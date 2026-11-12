package com.casino.command;

import com.casino.plat.Plat;
import com.casino.Casino;
import com.casino.chip.Chips;
import com.casino.data.CasinoData;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Comandos de administración (nivel 2):
 * <ul>
 *   <li>/casino resetwheel [jugadores] - el jugador puede volver a girar la ruleta hoy</li>
 *   <li>/casino chips &lt;jugadores&gt; &lt;cantidad&gt; - regala fichas</li>
 *   <li>/casino build &lt;x y z&gt; [giro 0-3] - construye un casino entero (la esquina mínima y la altura del piso)</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = Casino.MODID)
public final class CasinoCommands {
    private CasinoCommands() {
    }

    @SubscribeEvent
    public static void register(RegisterCommandsEvent e) {
        CommandDispatcher<CommandSourceStack> d = e.getDispatcher();
        d.register(Commands.literal("casino").requires(s -> Plat.isOp(s))
                .then(Commands.literal("resetwheel")
                        .executes(c -> {
                            ServerPlayer p = c.getSource().getPlayerOrException();
                            CasinoData.get(c.getSource().getServer()).resetSpin(p.getUUID());
                            c.getSource().sendSuccess(() -> Component.translatable("casino.cmd.reset", p.getName()), true);
                            return 1;
                        })
                        .then(Commands.argument("players", EntityArgument.players()).executes(c -> {
                            int n = 0;
                            for (ServerPlayer p : EntityArgument.getPlayers(c, "players")) {
                                CasinoData.get(c.getSource().getServer()).resetSpin(p.getUUID());
                                n++;
                            }
                            int count = n;
                            c.getSource().sendSuccess(() -> Component.translatable("casino.cmd.reset_many", count), true);
                            return n;
                        })))
                .then(Commands.literal("build")
                        .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                .executes(c -> build(c.getSource(), BlockPosArgument.getBlockPos(c, "pos"), 0))
                                .then(Commands.argument("rot", IntegerArgumentType.integer(0, 3))
                                        .executes(c -> build(c.getSource(), BlockPosArgument.getBlockPos(c, "pos"), IntegerArgumentType.getInteger(c, "rot")))
                                        .then(Commands.literal("diamond").executes(c -> buildTemplate(c.getSource(), "the_diamond",
                                                BlockPosArgument.getBlockPos(c, "pos"), IntegerArgumentType.getInteger(c, "rot")))))))
                .then(Commands.literal("chips")
                        .then(Commands.argument("players", EntityArgument.players())
                                .then(Commands.argument("amount", IntegerArgumentType.integer(1, 1_000_000)).executes(c -> {
                                    int amount = IntegerArgumentType.getInteger(c, "amount");
                                    int n = 0;
                                    for (ServerPlayer p : EntityArgument.getPlayers(c, "players")) {
                                        Chips.give(p, amount);
                                        n++;
                                    }
                                    int count = n;
                                    c.getSource().sendSuccess(() -> Component.translatable("casino.cmd.chips", amount, count), true);
                                    return n;
                                })))));
    }

    private static int buildTemplate(CommandSourceStack src, String name, BlockPos pos, int rot) {
        var box = com.casino.worldgen.TemplateCasinoPiece.buildNow(src.getLevel(), name, pos, net.minecraft.world.level.block.Rotation.values()[rot & 3]);
        src.sendSuccess(() -> Component.translatable("casino.cmd.build_tpl", name, box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ()), true);
        return 1;
    }

    private static int build(CommandSourceStack src, BlockPos pos, int rot) {
        com.casino.worldgen.CasinoPiece.buildNow(src.getLevel(), pos, rot);
        src.sendSuccess(() -> Component.translatable("casino.cmd.build", pos.getX(), pos.getY(), pos.getZ()), true);
        return 1;
    }
}
