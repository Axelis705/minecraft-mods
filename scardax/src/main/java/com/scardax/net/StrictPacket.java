package com.scardax.net;

import com.scardax.rules.SpawnRules;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Cliente -> servidor: activar o desactivar el modo estricto. Solo lo aceptan los operadores. */
public class StrictPacket {
    public final boolean on;

    public StrictPacket(boolean on) {
        this.on = on;
    }

    public static void encode(StrictPacket m, FriendlyByteBuf buf) {
        buf.writeBoolean(m.on);
    }

    public static StrictPacket decode(FriendlyByteBuf buf) {
        return new StrictPacket(buf.readBoolean());
    }

    public static void handle(StrictPacket m, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context c = ctx.get();
        ServerPlayer sp = c.getSender();
        c.enqueueWork(() -> {
            if (sp == null) {
                return;
            }
            if (!sp.hasPermissions(2)) {
                sp.sendSystemMessage(Component.translatable("scardax.msg.no_permission"));
                return;
            }
            SpawnRules rules = SpawnRules.of(sp.server);
            rules.setStrict(m.on);
            ScardaxNet.broadcast(sp.server, rules);
        });
        c.setPacketHandled(true);
    }
}
