package com.mundosdeaxelis.network;

import com.mundosdeaxelis.client.ClientBridge;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Servidor -> quien abre el crate: abre la pantalla de la ruleta con los datos de esta apertura. */
public class CrateOpenPacket {
    public final BlockPos pos;
    public final int kind;
    public final long seed;
    public final String winner;
    public final String opener;

    public CrateOpenPacket(BlockPos pos, int kind, long seed, String winner, String opener) {
        this.pos = pos;
        this.kind = kind;
        this.seed = seed;
        this.winner = winner;
        this.opener = opener;
    }

    public static void encode(CrateOpenPacket m, FriendlyByteBuf buf) {
        buf.writeBlockPos(m.pos);
        buf.writeVarInt(m.kind);
        buf.writeLong(m.seed);
        buf.writeUtf(m.winner);
        buf.writeUtf(m.opener);
    }

    public static CrateOpenPacket decode(FriendlyByteBuf buf) {
        return new CrateOpenPacket(buf.readBlockPos(), buf.readVarInt(), buf.readLong(), buf.readUtf(), buf.readUtf());
    }

    public static void handle(CrateOpenPacket m, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context c = ctx.get();
        c.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientBridge.openCrate(m)));
        c.setPacketHandled(true);
    }
}
