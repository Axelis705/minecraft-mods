package com.mundosdeaxelis.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Cliente -> servidor (etapa 282): el jugador pulsó «Confirmar». El servidor revisa que haya esperado lo necesario. */
public class ConfirmadoPacket {
    private final String kind;
    private final BlockPos pos;

    public ConfirmadoPacket(String kind, BlockPos pos) {
        this.kind = kind;
        this.pos = pos;
    }

    public static void encode(ConfirmadoPacket m, FriendlyByteBuf buf) {
        buf.writeUtf(m.kind, 64);
        buf.writeBlockPos(m.pos);
    }

    public static ConfirmadoPacket decode(FriendlyByteBuf buf) {
        return new ConfirmadoPacket(buf.readUtf(64), buf.readBlockPos());
    }

    public static void handle(ConfirmadoPacket m, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context c = ctx.get();
        ServerPlayer sp = c.getSender();
        c.enqueueWork(() -> {
            if (sp != null) {
                com.mundosdeaxelis.world.Confirmar.confirmado(sp, m.kind, m.pos);
            }
        });
        c.setPacketHandled(true);
    }
}
