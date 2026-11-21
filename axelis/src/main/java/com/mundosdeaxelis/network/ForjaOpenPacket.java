package com.mundosdeaxelis.network;

import com.mundosdeaxelis.client.ClientBridge;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Servidor -> cliente (etapa 270): abre la forja del Gran Yunque con los fragmentos de llave que llevas de cada tier (1 a 8). */
public class ForjaOpenPacket {
    public final BlockPos pos;
    /** Fragmentos de cada tier: índice 1 a 8 (el 0 no se usa). */
    public final int[] counts;
    public final long seed;

    public ForjaOpenPacket(BlockPos pos, int[] counts, long seed) {
        this.pos = pos;
        this.counts = counts;
        this.seed = seed;
    }

    public static void encode(ForjaOpenPacket m, FriendlyByteBuf buf) {
        buf.writeBlockPos(m.pos);
        for (int t = 1; t <= 8; t++) {
            buf.writeVarInt(m.counts[t]);
        }
        buf.writeLong(m.seed);
    }

    public static ForjaOpenPacket decode(FriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        int[] c = new int[9];
        for (int t = 1; t <= 8; t++) {
            c[t] = buf.readVarInt();
        }
        return new ForjaOpenPacket(pos, c, buf.readLong());
    }

    public static void handle(ForjaOpenPacket m, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context c = ctx.get();
        c.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientBridge.openForja(m)));
        c.setPacketHandled(true);
    }
}
