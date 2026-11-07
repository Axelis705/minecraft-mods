package com.pooltable;

import com.mojang.brigadier.CommandDispatcher;
import com.pooltable.block.VolleyCourtBE;
import com.pooltable.block.VolleyCourtBlock;
import com.pooltable.registry.ModBlocks;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** /voley probar <x y z>: pone una cancha, juega una partida solo de bots y dice si todo salió bien (nivel de permiso 2). */
@Mod.EventBusSubscriber(modid = PoolTable.MODID)
public final class VolleyCommand {
    private VolleyCommand() {
    }

    @SubscribeEvent
    public static void register(RegisterCommandsEvent e) {
        CommandDispatcher<CommandSourceStack> d = e.getDispatcher();
        d.register(Commands.literal("voley").requires(s -> s.hasPermission(2))
                .then(Commands.literal("probar").then(Commands.argument("pos", BlockPosArgument.blockPos()).executes(c -> test(c.getSource(), BlockPosArgument.getLoadedBlockPos(c, "pos"))))));
    }

    private static int test(CommandSourceStack src, BlockPos pos) {
        ServerLevel level = src.getLevel();
        level.setBlock(pos, ModBlocks.VOLLEY_COURT.get().defaultBlockState().setValue(VolleyCourtBlock.FACING, Direction.EAST), 3);
        if (!(level.getBlockEntity(pos) instanceof VolleyCourtBE be)) {
            src.sendFailure(Component.literal("PRUEBA_VOLEY_FALLA sin entidad de bloque"));
            return 0;
        }
        String res;
        try {
            res = be.debugRun(1200);
        } catch (RuntimeException ex) {
            res = "FALLA excepcion " + ex;
        }
        level.removeBlock(pos, false);
        boolean ok = res.startsWith("OK");
        String msg = (ok ? "PRUEBA_VOLEY_OK " : "PRUEBA_VOLEY_FALLA ") + res;
        src.sendSuccess(() -> Component.literal(msg), true);
        return ok ? 1 : 0;
    }
}
