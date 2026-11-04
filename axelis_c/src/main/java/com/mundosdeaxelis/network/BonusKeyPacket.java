package com.mundosdeaxelis.network;

import com.mundosdeaxelis.client.ClientBridge;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Servidor -> jugador: una caja le dio además la llave de la caja siguiente (índice de {@code CrateKind}); activa la animación en pantalla. */
public class BonusKeyPacket {
    public final int kind;

    public BonusKeyPacket(int kind) {
        this.kind = kind;
    }

    public static void encode(BonusKeyPacket m, FriendlyByteBuf buf) {
        buf.writeVarInt(m.kind);
    }

    public static BonusKeyPacket decode(FriendlyByteBuf buf) {
        return new BonusKeyPacket(buf.readVarInt());
    }

    public static void handle(BonusKeyPacket m, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context c = ctx.get();
        c.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientBridge.bonusKey(m)));
        c.setPacketHandled(true);
    }
}
