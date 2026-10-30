package com.kensei.entity;

import com.kensei.registry.ModEntities;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

/**
 * /kensei rival (aparece un rival de práctica que pelea), /kensei rival dummy (un maniquí que no ataca) y /kensei rival clear (los quita,
 * solo administradores). También se pueden sacar con el huevo de aparición de la pestaña creativa de Kensei.
 */
public final class RivalCommands {
    private RivalCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("kensei").then(Commands.literal("rival")
                .executes(c -> spawn(c.getSource().getPlayerOrException(), true))
                .then(Commands.literal("fight").executes(c -> spawn(c.getSource().getPlayerOrException(), true)))
                .then(Commands.literal("dummy").executes(c -> spawn(c.getSource().getPlayerOrException(), false)))
                .then(Commands.literal("clear").requires(s -> s.hasPermission(2)).executes(c -> {
                    int n = 0;
                    for (ServerLevel lv : c.getSource().getServer().getAllLevels()) {
                        for (RivalEntity r : lv.getEntities(ModEntities.RIVAL.get(), e -> !e.isDuelBot())) {
                            r.discard();
                            n++;
                        }
                    }
                    final int total = n;
                    c.getSource().sendSuccess(() -> Component.translatable("kensei.rival.cleared", total), true);
                    return total;
                }))));
    }

    private static int spawn(ServerPlayer p, boolean fight) {
        ServerLevel level = p.serverLevel();
        RivalEntity r = ModEntities.RIVAL.get().create(level);
        if (r == null) {
            return 0;
        }
        Vec3 look = p.getLookAngle();
        double fx = look.x;
        double fz = look.z;
        double len = Math.sqrt(fx * fx + fz * fz);
        if (len < 1.0E-4) {
            fx = 0;
            fz = 1;
            len = 1;
        }
        double x = p.getX() + fx / len * 3.5;
        double z = p.getZ() + fz / len * 3.5;
        r.moveTo(x, p.getY(), z, p.getYRot() + 180.0F, 0.0F);
        r.setFighter(fight);
        level.addFreshEntity(r);
        p.sendSystemMessage(Component.translatable(fight ? "kensei.rival.spawned_fight" : "kensei.rival.spawned_dummy"));
        return 1;
    }
}
