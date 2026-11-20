package com.mundosdeaxelis.network;

import com.mundosdeaxelis.client.ClientBridge;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/**
 * Servidor -> cliente: abre (o actualiza, si ya está abierta) la ventana de diálogo de un habitante. Lleva su tipo y estado,
 * cuántos vecinos hay (para la tienda), si ya no está el guardián de la llave, cómo terminó lo último que se pidió
 * ({@code result}, ver {@code NpcService.R_*}), una frase opcional (pistas) y un dato extra (qué se compró, cuánto hierro...).
 */
public class NpcOpenPacket {
    public final int entityId;
    public final String kind;
    public final int state;
    public final int residents;
    public final boolean guardGone;
    public final int result;
    public final Component msg;
    public final int extra;

    public NpcOpenPacket(int entityId, String kind, int state, int residents, boolean guardGone, int result, Component msg, int extra) {
        this.entityId = entityId;
        this.kind = kind;
        this.state = state;
        this.residents = residents;
        this.guardGone = guardGone;
        this.result = result;
        this.msg = msg;
        this.extra = extra;
    }

    public static void encode(NpcOpenPacket m, FriendlyByteBuf buf) {
        buf.writeVarInt(m.entityId);
        buf.writeUtf(m.kind, 64);
        buf.writeVarInt(m.state);
        buf.writeVarInt(m.residents);
        buf.writeBoolean(m.guardGone);
        buf.writeVarInt(m.result);
        buf.writeBoolean(m.msg != null);
        if (m.msg != null) {
            buf.writeComponent(m.msg);
        }
        buf.writeVarInt(m.extra);
    }

    public static NpcOpenPacket decode(FriendlyByteBuf buf) {
        int id = buf.readVarInt();
        String kind = buf.readUtf(64);
        int state = buf.readVarInt();
        int res = buf.readVarInt();
        boolean gg = buf.readBoolean();
        int result = buf.readVarInt();
        Component msg = buf.readBoolean() ? buf.readComponent() : null;
        int extra = buf.readVarInt();
        return new NpcOpenPacket(id, kind, state, res, gg, result, msg, extra);
    }

    public static void handle(NpcOpenPacket m, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context c = ctx.get();
        c.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientBridge.npc(m)));
        c.setPacketHandled(true);
    }
}
