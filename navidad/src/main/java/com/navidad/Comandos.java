package com.navidad;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.navidad.entidad.Pinata;
import com.navidad.objeto.Ropa;
import com.navidad.registry.ModRegistry;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/** {@code /navidad ...}: la temporada, el calendario, regalos, piñatas, ropa y lo demás (casi todo para operadores). */
public final class Comandos {
    private Comandos() {
    }

    public static void registrar(CommandDispatcher<CommandSourceStack> d) {
        List<String> ropa = new ArrayList<>(Ropa.CABEZA);
        ropa.addAll(Ropa.CUERPO);
        d.register(Commands.literal(Navidad.MODID)
                .executes(c -> {
                    c.getSource().sendSuccess(() -> Component.translatable("navidad.hola", com.axeliscore.AxelisCore.version(),
                            Component.translatable(Temporada.activa(c.getSource().getServer()) ? "navidad.temporada.si" : "navidad.temporada.no")), false);
                    Navidad.LOGGER.info("PRUEBA_NAVIDAD_OK {}", com.axeliscore.AxelisCore.version());
                    return 1;
                })
                .then(Commands.literal("temporada").requires(s -> s.hasPermission(2))
                        .then(Commands.argument("modo", StringArgumentType.word())
                                .suggests((c, b) -> SharedSuggestionProvider.suggest(List.of("si", "no", "auto"), b))
                                .executes(c -> {
                                    String m = StringArgumentType.getString(c, "modo");
                                    if (!List.of("si", "no", "auto").contains(m)) {
                                        c.getSource().sendFailure(Component.literal("si | no | auto"));
                                        return 0;
                                    }
                                    Temporada.modo(c.getSource().getServer(), m);
                                    c.getSource().sendSuccess(() -> Component.translatable("navidad.temporada.modo", m), true);
                                    return 1;
                                })))
                .then(Commands.literal("calendario").requires(s -> s.hasPermission(2))
                        .then(Commands.literal("dia").then(Commands.argument("dia", IntegerArgumentType.integer(0, 31)).executes(c -> {
                            Temporada.diaPrueba = IntegerArgumentType.getInteger(c, "dia");
                            c.getSource().sendSuccess(() -> Component.translatable("navidad.calendario.fingido", Temporada.diaPrueba), true);
                            return 1;
                        }))))
                .then(Commands.literal("regalo").requires(s -> s.hasPermission(2))
                        .executes(c -> regalo(c, List.of(c.getSource().getPlayerOrException())))
                        .then(Commands.argument("quien", EntityArgument.players())
                                .executes(c -> regalo(c, new ArrayList<>(EntityArgument.getPlayers(c, "quien"))))))
                .then(Commands.literal("pinata").requires(s -> s.hasPermission(2)).executes(Comandos::pinata))
                .then(Commands.literal("ropa").requires(s -> s.hasPermission(2))
                        .then(Commands.argument("prenda", StringArgumentType.word())
                                .suggests((c, b) -> SharedSuggestionProvider.suggest(ropa, b))
                                .executes(c -> {
                                    String p = StringArgumentType.getString(c, "prenda");
                                    if (!ropa.contains(p)) {
                                        c.getSource().sendFailure(Component.literal(String.join(" | ", ropa)));
                                        return 0;
                                    }
                                    ServerPlayer sp = c.getSource().getPlayerOrException();
                                    Ropa.alternar(c.getSource().getServer(), sp, p);
                                    return 1;
                                }))));
    }

    private static int regalo(CommandContext<CommandSourceStack> c, List<ServerPlayer> ps) {
        for (ServerPlayer p : ps) {
            ItemStack st = new ItemStack(ModRegistry.REGALO_MISTERIOSO.get());
            if (!p.getInventory().add(st)) {
                p.drop(st, false);
            }
        }
        c.getSource().sendSuccess(() -> Component.translatable("navidad.regalo.dado", ps.size()), true);
        return ps.size();
    }

    private static int pinata(CommandContext<CommandSourceStack> c) {
        ServerLevel l = c.getSource().getLevel();
        Vec3 pos = c.getSource().getPosition();
        Vec3 mira = Vec3.directionFromRotation(0, c.getSource().getRotation().y);
        Pinata p = ModRegistry.PINATA_ENT.get().create(l);
        if (p == null) {
            return 0;
        }
        p.moveTo(pos.x + mira.x * 2.5, pos.y + 0.3, pos.z + mira.z * 2.5, c.getSource().getRotation().y, 0);
        l.addFreshEntity(p);
        l.playSound(null, p.blockPosition(), ModRegistry.CANCION_PINATA.get(), net.minecraft.sounds.SoundSource.RECORDS, 1.2F, 1F);
        return 1;
    }
}
