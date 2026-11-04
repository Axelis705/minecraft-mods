package com.mundosdeaxelis.world;

import com.mundosdeaxelis.MundosDeAxelis;
import com.mundosdeaxelis.network.ModNetwork;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

/**
 * Salir de una dungeon sin enfrentar al jefe, para quien no se siente preparado. Se ofrece en la antesala con un mensaje en
 * el chat (con clic) y funciona hasta que el jefe despierta. Salir no da las recompensas del jefe, pero puedes volver a entrar
 * después. Cada dungeon nueva solo tiene que llamar a {@link #offer} y a {@link #canLeave}.
 */
@Mod.EventBusSubscriber(modid = MundosDeAxelis.MODID)
public final class BossSkip {
    private BossSkip() {
    }

    /** ¿Puede este jugador salir ahora? Está en una dungeon y su jefe todavía no despertó. */
    public static boolean canLeave(ServerPlayer p) {
        if (!(p.level() instanceof ServerLevel sl) || !BossTravel.inArena(sl)) {
            return false;
        }
        if (ModDimensions.isIce(sl.dimension())) {
            return IceTravel.of(sl).canLeave();
        }
        if (ModDimensions.isForge(sl.dimension())) {
            return ForgeManager.of(sl).isInProgress();
        }
        return DungeonManager.of(sl).isInProgress();
    }

    /** Una sola línea de chat con dos botones: volver a ver la ficha del jefe y salir sin enfrentarlo. */
    private static Component line() {
        MutableComponent sheet = Component.translatable("message.mundos_de_axelis.skip_boss.sheet")
                .withStyle(s -> s.withColor(ChatFormatting.AQUA).withUnderlined(true).withBold(true)
                        .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/ficha_jefe"))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.translatable("message.mundos_de_axelis.skip_boss.sheet_hover"))));
        MutableComponent btn = Component.translatable("message.mundos_de_axelis.skip_boss.button")
                .withStyle(s -> s.withColor(ChatFormatting.GOLD).withUnderlined(true).withBold(true)
                        .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/salir_dungeon"))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.translatable("message.mundos_de_axelis.skip_boss.hover"))));
        return Component.translatable("message.mundos_de_axelis.skip_boss.offer").withStyle(ChatFormatting.YELLOW)
                .append(Component.literal(" ")).append(sheet).append(Component.literal("  ")).append(btn);
    }

    /**
     * Llegada a la antesala: una sola línea en el chat para todos (volver a ver la ficha del jefe o salir). La ficha en sí se
     * abre a cada jugador al llegar a la antesala con {@link #dossier}. Sirve para cualquier dungeon: la etapa sale de la dimensión.
     */
    public static void offer(List<ServerPlayer> players) {
        Component msg = line();
        for (ServerPlayer p : players) {
            p.sendSystemMessage(msg);
        }
    }

    /** Abre a este jugador la ficha del jefe de su dungeon (descripción, números, ataques y consejos; con botón para salir sin pelear). */
    public static void dossier(ServerPlayer p, List<ServerPlayer> group) {
        sendDossier(p, group);
    }

    /** Al abrirse la puerta del jefe: solo el recordatorio de una línea (la ficha ya se mostró en la antesala). */
    public static void remind(List<ServerPlayer> players) {
        Component msg = line();
        for (ServerPlayer p : players) {
            p.sendSystemMessage(msg);
        }
    }

    private static void sendDossier(ServerPlayer p, List<ServerPlayer> group) {
        int stage = BossTravel.stageOf(p.level());
        if (stage > 0) {
            ModNetwork.sendTo(p, BossInfo.dossier(stage, group, p));
        }
    }

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("ficha_jefe")
                .executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    if (BossTravel.stageOf(p.level()) == 0) {
                        ctx.getSource().sendFailure(Component.translatable("message.mundos_de_axelis.skip_boss.not_inside"));
                        return 0;
                    }
                    List<ServerPlayer> group = new java.util.ArrayList<>();
                    for (ServerPlayer q : ((ServerLevel) p.level()).players()) {
                        if (q.isAlive() && !q.isSpectator()) {
                            group.add(q);
                        }
                    }
                    sendDossier(p, group);
                    return 1;
                }));
        event.getDispatcher().register(Commands.literal("salir_dungeon")
                .executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    if (!BossTravel.inArena(p.level())) {
                        ctx.getSource().sendFailure(Component.translatable("message.mundos_de_axelis.skip_boss.not_inside"));
                        return 0;
                    }
                    if (!canLeave(p)) {
                        ctx.getSource().sendFailure(Component.translatable("message.mundos_de_axelis.skip_boss.too_late"));
                        return 0;
                    }
                    MutableComponent yes = Component.translatable("message.mundos_de_axelis.skip_boss.yes")
                            .withStyle(s -> s.withColor(ChatFormatting.RED).withUnderlined(true).withBold(true)
                                    .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/salir_dungeon confirmar")));
                    p.sendSystemMessage(Component.translatable("message.mundos_de_axelis.skip_boss.confirm").withStyle(ChatFormatting.YELLOW)
                            .append(Component.literal(" ")).append(yes));
                    return 1;
                })
                .then(Commands.literal("confirmar").executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    if (!canLeave(p)) {
                        ctx.getSource().sendFailure(Component.translatable("message.mundos_de_axelis.skip_boss.too_late"));
                        return 0;
                    }
                    p.sendSystemMessage(Component.translatable("message.mundos_de_axelis.skip_boss.left").withStyle(ChatFormatting.GRAY));
                    BossTravel.leave(p);
                    return 1;
                })));
    }
}
