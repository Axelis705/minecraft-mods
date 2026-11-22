package com.mundosdeaxelis.world;

import com.mundosdeaxelis.network.ConfirmarPacket;
import com.mundosdeaxelis.network.ModNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Etapa 282: acciones que hay que confirmar con una ventanita (el botón se activa a los 3 s). El servidor recuerda cuándo se
 * abrió y no acepta la confirmación antes de tiempo, ni tarde (pasados 2 min), ni lejos del bloque.
 */
public final class Confirmar {
    private Confirmar() {
    }

    /** Lo que se espera antes de poder confirmar (60 ticks = 3 s). */
    public static final int ESPERA = 60;

    private record Pendiente(String kind, BlockPos pos, long desde) {
    }

    private static final Map<UUID, Pendiente> PENDIENTES = new HashMap<>();

    public static void clearRuntime() {
        PENDIENTES.clear();
    }

    /** Abre la ventanita al jugador. */
    public static void pedir(ServerPlayer sp, String kind, BlockPos pos) {
        registrar(sp, kind, pos);
        ModNetwork.sendTo(sp, new ConfirmarPacket(kind, pos, ESPERA));
    }

    /** Apunta que se le pidió confirmar (sin mandar la ventanita: también lo usa la prueba). */
    public static void registrar(ServerPlayer sp, String kind, BlockPos pos) {
        PENDIENTES.put(sp.getUUID(), new Pendiente(kind, pos.immutable(), sp.level().getGameTime()));
    }

    /** ¿Vale la confirmación ahora? (la quita de las pendientes si vale). */
    public static boolean valida(ServerPlayer sp, String kind, BlockPos pos) {
        Pendiente p = PENDIENTES.get(sp.getUUID());
        long now = sp.level().getGameTime();
        if (p == null || !p.kind.equals(kind) || !p.pos.equals(pos) || now < p.desde + ESPERA - 4 || now > p.desde + 2400) {
            return false;
        }
        if (sp.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > 64.0) {
            return false;
        }
        PENDIENTES.remove(sp.getUUID());
        return true;
    }

    /** El jugador pulsó Confirmar. */
    public static void confirmado(ServerPlayer sp, String kind, BlockPos pos) {
        if (!valida(sp, kind, pos)) {
            return;
        }
        switch (kind) {
            case "salida_volcan" -> {
                if (sp.level().getBlockState(pos).getBlock() instanceof com.mundosdeaxelis.block.PiedraRegresoBlock) {
                    Volcanes.goBack(sp, pos);
                }
            }
            default -> {
            }
        }
    }
}
