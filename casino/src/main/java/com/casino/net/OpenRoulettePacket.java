package com.casino.net;

import com.casino.client.ClientHooks;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Servidor -> jugador: abrir el panel de una mesa de ruleta (cuando le hace clic al crupier). */
public final class OpenRoulettePacket {
    public final BlockPos pos;

    public OpenRoulettePacket(BlockPos pos) {
        this.pos = pos;
    }

    public static void encode(OpenRoulettePacket m, FriendlyByteBuf b) {
        b.writeBlockPos(m.pos);
    }

    public static OpenRoulettePacket decode(FriendlyByteBuf b) {
        return new OpenRoulettePacket(b.readBlockPos());
    }

    public static void handle(OpenRoulettePacket m, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientHooks.openRoulette(m.pos)));
        ctx.get().setPacketHandled(true);
    }
}
