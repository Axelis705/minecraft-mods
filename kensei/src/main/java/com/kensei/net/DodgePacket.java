package com.kensei.net;

import com.kensei.combat.Dodge;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Cliente -> servidor: quiero esquivar hacia esta dirección (adelante/atrás y izquierda/derecha). El servidor decide si se puede. */
public class DodgePacket {
    public final int fwd;
    public final int strafe;

    public DodgePacket(int fwd, int strafe) {
        this.fwd = fwd;
        this.strafe = strafe;
    }

    public static void encode(DodgePacket m, FriendlyByteBuf buf) {
        buf.writeByte(m.fwd);
        buf.writeByte(m.strafe);
    }

    public static DodgePacket decode(FriendlyByteBuf buf) {
        return new DodgePacket(buf.readByte(), buf.readByte());
    }

    public static void handle(DodgePacket m, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context c = ctx.get();
        ServerPlayer sp = c.getSender();
        c.enqueueWork(() -> {
            if (sp != null) {
                Dodge.tryDodge(sp, m.fwd, m.strafe);
            }
        });
        c.setPacketHandled(true);
    }
}
