package com.mundosdeaxelis.cartas;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Las cartas de un sobre se entregan al TERMINAR de abrirlo (cuando la ultima carta se voltea), no al hacer clic.
 * Mientras tanto quedan guardadas aqui, en el servidor. Si el jugador cierra la pantalla antes, se desconecta o
 * pasa mucho tiempo, se entregan igual: nunca se pierde un sobre.
 */
@Mod.EventBusSubscriber(modid = Cartas.MOD_ID)
public final class PackDelivery {
    private record Pending(List<PackRoller.Pull> pulls, long since) {
    }

    private static final Map<UUID, Pending> PENDING = new HashMap<>();
    /** Si la pantalla nunca avisa (error, cierre raro), a los 2 minutos se entregan solas. */
    private static final long TIMEOUT_TICKS = 2400L;

    private PackDelivery() {
    }

    /** Guarda las cartas de un sobre recien sorteado. Si habia otro pendiente, lo entrega primero. */
    public static void hold(ServerPlayer sp, List<PackRoller.Pull> pulls) {
        deliver(sp);
        PENDING.put(sp.getUUID(), new Pending(pulls, sp.level().getGameTime()));
    }

    public static boolean hasPending(ServerPlayer sp) {
        return PENDING.containsKey(sp.getUUID());
    }

    /** Pasa las cartas pendientes al inventario (o al piso si esta lleno). */
    public static void deliver(ServerPlayer sp) {
        Pending p = PENDING.remove(sp.getUUID());
        if (p == null) {
            return;
        }
        for (PackRoller.Pull pull : p.pulls()) {
            ItemStack stack = CartasItems.stack(pull.card(), pull.holo(), 1);
            if (!sp.getInventory().add(stack)) {
                sp.drop(stack, false);
            }
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) {
        if (e.getEntity() instanceof ServerPlayer sp) {
            deliver(sp);
        }
    }

    @SubscribeEvent
    public static void onTick(TickEvent.PlayerTickEvent e) {
        if (e.phase != TickEvent.Phase.END || !(e.player instanceof ServerPlayer sp) || PENDING.isEmpty() || sp.tickCount % 40 != 0) {
            return;
        }
        Pending p = PENDING.get(sp.getUUID());
        if (p != null && sp.level().getGameTime() - p.since() > TIMEOUT_TICKS) {
            deliver(sp);
        }
    }
}
