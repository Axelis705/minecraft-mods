package com.mundosdeaxelis.network;

import com.mundosdeaxelis.client.ClientBridge;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Servidor -> cliente: abre la pantalla de la cerradura de un cofre (nivel 1 a 5 y semilla de la disposición de clavijas). */
public class LockpickOpenPacket {
    public final BlockPos pos;
    public final int level;
    public final long seed;

    public LockpickOpenPacket(BlockPos pos, int level, long seed) {
        this.pos = pos;
        this.level = level;
        this.seed = seed;
    }

    public static void encode(LockpickOpenPacket m, FriendlyByteBuf buf) {
        buf.writeBlockPos(m.pos);
        buf.writeVarInt(m.level);
        buf.writeLong(m.seed);
    }

    public static LockpickOpenPacket decode(FriendlyByteBuf buf) {
        return new LockpickOpenPacket(buf.readBlockPos(), buf.readVarInt(), buf.readLong());
    }

    public static void handle(LockpickOpenPacket m, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context c = ctx.get();
        c.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientBridge.openLockpick(m)));
        c.setPacketHandled(true);
    }
}
