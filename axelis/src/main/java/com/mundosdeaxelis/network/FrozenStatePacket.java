package com.mundosdeaxelis.network;

import com.mundosdeaxelis.client.ClientBridge;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Servidor -> cliente: estás atrapado en hielo (cuántos saltos faltan de cuántos) o ya no. */
public class FrozenStatePacket {
    public final boolean frozen;
    public final int hits;
    public final int max;

    public FrozenStatePacket(boolean frozen, int hits, int max) {
        this.frozen = frozen;
        this.hits = hits;
        this.max = max;
    }

    public static void encode(FrozenStatePacket msg, FriendlyByteBuf buf) {
        buf.writeBoolean(msg.frozen);
        buf.writeVarInt(msg.hits);
        buf.writeVarInt(msg.max);
    }

    public static FrozenStatePacket decode(FriendlyByteBuf buf) {
        return new FrozenStatePacket(buf.readBoolean(), buf.readVarInt(), buf.readVarInt());
    }

    public static void handle(FrozenStatePacket msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientBridge.frozen(msg.frozen, msg.hits, msg.max)));
        context.setPacketHandled(true);
    }
}
