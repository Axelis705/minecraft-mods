package com.kensei.net;

import com.kensei.combat.Moves;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Cliente -> servidor: quiero hacer un ataque. kind 0 = golpe normal (clic izquierdo), 1 = ataque crítico (tecla R). El servidor decide si se puede. */
public class MovePacket {
    public static final int NORMAL = 0;
    public static final int HEAVY = 1;

    public final int kind;

    public MovePacket(int kind) {
        this.kind = kind;
    }

    public static void encode(MovePacket m, FriendlyByteBuf buf) {
        buf.writeByte(m.kind);
    }

    public static MovePacket decode(FriendlyByteBuf buf) {
        return new MovePacket(buf.readByte());
    }

    public static void handle(MovePacket m, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context c = ctx.get();
        ServerPlayer sp = c.getSender();
        c.enqueueWork(() -> {
            if (sp == null) {
                return;
            }
            if (m.kind == HEAVY) {
                Moves.tryHeavy(sp);
            } else {
                Moves.trySwing(sp);
            }
        });
        c.setPacketHandled(true);
    }
}
