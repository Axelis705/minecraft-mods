package com.coleccionista.voz;

import com.coleccionista.Coleccionista;
import com.coleccionista.evento.Evento;
import com.coleccionista.evento.Pieza;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/** {@code /evento voz}: el estado del chat de voz, el altavoz del organizador, los grupos y una prueba de quién oye a quién. */
public final class ComandosVoz {
    private static final String MOD = Coleccionista.MODID;

    private ComandosVoz() {
    }

    public static LiteralArgumentBuilder<CommandSourceStack> voz(SuggestionProvider<CommandSourceStack> piezas) {
        return Commands.literal("voz")
                .executes(ComandosVoz::estado)
                .then(Commands.literal("altavoz")
                        .then(Commands.literal("si").executes(c -> altavoz(c, true)))
                        .then(Commands.literal("no").executes(c -> altavoz(c, false))))
                .then(Commands.literal("grupos")
                        .then(Commands.literal("si").executes(c -> grupos(c, true)))
                        .then(Commands.literal("no").executes(c -> grupos(c, false))))
                .then(Commands.literal("fantasmas_organizador")
                        .then(Commands.literal("si").executes(c -> fantasmasOrg(c, true)))
                        .then(Commands.literal("no").executes(c -> fantasmasOrg(c, false))))
                .then(Commands.literal("probar")
                        .then(Commands.argument("habla", StringArgumentType.word()).suggests(piezas)
                                .then(Commands.argument("oye", StringArgumentType.word()).suggests(piezas).executes(ComandosVoz::probar))));
    }

    private static int estado(CommandContext<CommandSourceStack> c) {
        Voz.actualizar(c.getSource().getServer());
        c.getSource().sendSuccess(() -> Component.translatable(MOD + ".voz.estado",
                Component.translatable(MOD + (VozPuente.disponible ? ".voz.si" : ".voz.no_instalado")),
                VozPuente.vivos.size(), VozPuente.fantasmas.size(), VozPuente.altavoz.size(),
                Component.translatable(MOD + (VozPuente.gruposVivos ? ".voz.si" : ".voz.no"))), false);
        return 1;
    }

    private static int altavoz(CommandContext<CommandSourceStack> c, boolean si) {
        if (!(c.getSource().getEntity() instanceof ServerPlayer sp)) {
            c.getSource().sendFailure(Component.translatable(MOD + ".voz.solo_jugador"));
            return 0;
        }
        Voz.altavoz(sp.getUUID(), si);
        c.getSource().sendSuccess(() -> Component.translatable(MOD + (si ? ".voz.altavoz_si" : ".voz.altavoz_no")), false);
        if (!VozPuente.disponible) {
            c.getSource().sendFailure(Component.translatable(MOD + ".voz.no_instalado"));
        }
        return 1;
    }

    private static int grupos(CommandContext<CommandSourceStack> c, boolean si) {
        VozPuente.gruposVivos = si;
        c.getSource().sendSuccess(() -> Component.translatable(MOD + (si ? ".voz.grupos_si" : ".voz.grupos_no")), true);
        return 1;
    }

    private static int fantasmasOrg(CommandContext<CommandSourceStack> c, boolean si) {
        VozPuente.organizadorOyeFantasmas = si;
        c.getSource().sendSuccess(() -> Component.translatable(MOD + (si ? ".voz.org_si" : ".voz.org_no")), true);
        return 1;
    }

    /** ¿"oye" escucha a "habla" por cercanía? (y si "habla" es fantasma, ¿le llega por el canal de los fantasmas?) */
    private static int probar(CommandContext<CommandSourceStack> c) {
        Evento ev = Evento.get(c.getSource().getServer());
        Pieza a = ev.buscar(StringArgumentType.getString(c, "habla")), b = ev.buscar(StringArgumentType.getString(c, "oye"));
        if (a == null || b == null) {
            c.getSource().sendFailure(Component.translatable(MOD + ".cmd.no_existe", StringArgumentType.getString(c, a == null ? "habla" : "oye")));
            return 0;
        }
        Voz.actualizar(c.getSource().getServer());
        // con el estado de las piezas (también bots, que no tienen voz pero sirven para probar las reglas)
        java.util.Set<java.util.UUID> fant = new java.util.HashSet<>();
        for (Pieza p : ev.piezas()) {
            if (!p.vivo()) {
                fant.add(p.id);
            }
        }
        boolean oye = !a.vivo() ? VozPuente.oyeAlFantasma(b.id, fant, VozPuente.organizadores)
                : VozPuente.oye(a.id, b.id, fant, VozPuente.organizadores, VozPuente.separados);
        Coleccionista.LOGGER.info("COLECCIONISTA_VOZ probar {} -> {} oye={}", a.nombre, b.nombre, oye);
        c.getSource().sendSuccess(() -> Component.translatable(MOD + (oye ? ".voz.oye" : ".voz.no_oye"), b.nombre, a.nombre), false);
        return oye ? 1 : 0;
    }
}
