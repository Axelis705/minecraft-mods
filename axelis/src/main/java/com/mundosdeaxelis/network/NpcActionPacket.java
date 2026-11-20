package com.mundosdeaxelis.network;

import com.mundosdeaxelis.npc.NpcService;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

/** Cliente -> servidor: algo pedido en la ventana de un habitante (comprar, pistas, mudarse...). El servidor lo revisa todo. */
public class NpcActionPacket {
    public final int entityId;
    public final int action;
    public final int arg;

    public NpcActionPacket(int entityId, int action, int arg) {
        this.entityId = entityId;
        this.action = action;
        this.arg = arg;
    }

    public static void encode(NpcActionPacket m, FriendlyByteBuf buf) {
        buf.writeVarInt(m.entityId);
        buf.writeVarInt(m.action);
        buf.writeVarInt(m.arg);
    }

    public static NpcActionPacket decode(FriendlyByteBuf buf) {
        return new NpcActionPacket(buf.readVarInt(), buf.readVarInt(), buf.readVarInt());
    }

    public static void handle(NpcActionPacket m, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context c = ctx.get();
        ServerPlayer sp = c.getSender();
        c.enqueueWork(() -> {
            if (sp != null) {
                NpcService.action(sp, m.entityId, m.action, m.arg);
            }
        });
        c.setPacketHandled(true);
    }
}
