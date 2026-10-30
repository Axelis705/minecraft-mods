package com.scardax.net;

import com.scardax.rules.Purge;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Cliente -> servidor: quitar los mobs ya cargados cuyo tipo está en 0 %. Solo lo aceptan los operadores. */
public class PurgePacket {
    public static void encode(PurgePacket m, FriendlyByteBuf buf) {
    }

    public static PurgePacket decode(FriendlyByteBuf buf) {
        return new PurgePacket();
    }

    public static void handle(PurgePacket m, Supplier<NetworkEvent.Context> ctx) {
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
            int n = Purge.run(sp.server);
            sp.sendSystemMessage(Component.translatable("scardax.msg.purged", n));
        });
        c.setPacketHandled(true);
    }
}
