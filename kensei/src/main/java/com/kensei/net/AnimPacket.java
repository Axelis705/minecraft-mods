package com.kensei.net;

import com.kensei.client.ClientAnims;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Servidor -> clientes cercanos: esta entidad empezó un movimiento (para que todos vean la misma animación). */
public class AnimPacket {
    public final int entityId;
    public final int move;
    public final float arg;

    public AnimPacket(int entityId, int move, float arg) {
        this.entityId = entityId;
        this.move = move;
        this.arg = arg;
    }

    public static void encode(AnimPacket m, FriendlyByteBuf buf) {
        buf.writeVarInt(m.entityId);
        buf.writeByte(m.move);
        buf.writeFloat(m.arg);
    }

    public static AnimPacket decode(FriendlyByteBuf buf) {
        return new AnimPacket(buf.readVarInt(), buf.readByte(), buf.readFloat());
    }

    public static void handle(AnimPacket m, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context c = ctx.get();
        c.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientAnims.receive(m.entityId, m.move, m.arg)));
        c.setPacketHandled(true);
    }
}
