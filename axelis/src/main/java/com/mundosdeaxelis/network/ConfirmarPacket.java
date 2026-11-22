package com.mundosdeaxelis.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Servidor -> cliente (etapa 282): abre una ventanita de «¿seguro?» con un botón Confirmar que se activa pasados unos segundos.
 * {@code kind} dice qué se confirma (p. ej. «salida_volcan») y {@code pos} el bloque que lo pidió.
 */
public class ConfirmarPacket {
    public final String kind;
    public final BlockPos pos;
    public final int ticks;

    public ConfirmarPacket(String kind, BlockPos pos, int ticks) {
        this.kind = kind;
        this.pos = pos;
        this.ticks = ticks;
    }

    public static void encode(ConfirmarPacket m, FriendlyByteBuf buf) {
        buf.writeUtf(m.kind, 64);
        buf.writeBlockPos(m.pos);
        buf.writeVarInt(m.ticks);
    }

    public static ConfirmarPacket decode(FriendlyByteBuf buf) {
        return new ConfirmarPacket(buf.readUtf(64), buf.readBlockPos(), buf.readVarInt());
    }

    public static void handle(ConfirmarPacket m, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context c = ctx.get();
        c.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> com.mundosdeaxelis.client.ClientBridge.confirmar(m)));
        c.setPacketHandled(true);
    }
}
