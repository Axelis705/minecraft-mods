package com.mundosdeaxelis.network;

import com.mundosdeaxelis.relic.EquipActions;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Jugador -> servidor: abrir el menú de equipo o usar una reliquia equipada. */
public class EquipActionPacket {
    public static final int OPEN = 0;
    public static final int USE_RELIC = 1;

    public final int action;
    public final int slot;

    public EquipActionPacket(int action, int slot) {
        this.action = action;
        this.slot = slot;
    }

    public static void encode(EquipActionPacket m, FriendlyByteBuf buf) {
        buf.writeVarInt(m.action);
        buf.writeVarInt(m.slot);
    }

    public static EquipActionPacket decode(FriendlyByteBuf buf) {
        return new EquipActionPacket(buf.readVarInt(), buf.readVarInt());
    }

    public static void handle(EquipActionPacket m, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context c = ctx.get();
        ServerPlayer sp = c.getSender();
        c.enqueueWork(() -> {
            if (sp == null || !sp.isAlive()) {
                return;
            }
            switch (m.action) {
                case OPEN -> EquipActions.open(sp);
                case USE_RELIC -> EquipActions.useRelic(sp, m.slot);
                default -> {
                }
            }
        });
        c.setPacketHandled(true);
    }
}
