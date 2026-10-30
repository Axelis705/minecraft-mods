package com.casino.net;

import com.casino.block.BlackjackBE;
import com.casino.block.BlackjackBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Cliente -> servidor: una jugada en una mesa de blackjack. El servidor comprueba todo (distancia, turno, fichas). */
public final class BlackjackPacket {
    public static final int DEAL = 0;
    public static final int HIT = 1;
    public static final int STAND = 2;
    public static final int DOUBLE = 3;
    /** Repartir ya, sin esperar a más jugadores. */
    public static final int START = 4;

    public final BlockPos pos;
    public final int action;
    public final int bet;

    public BlackjackPacket(BlockPos pos, int action, int bet) {
        this.pos = pos;
        this.action = action;
        this.bet = bet;
    }

    public static void encode(BlackjackPacket m, FriendlyByteBuf b) {
        b.writeBlockPos(m.pos);
        b.writeVarInt(m.action);
        b.writeVarInt(m.bet);
    }

    public static BlackjackPacket decode(FriendlyByteBuf b) {
        return new BlackjackPacket(b.readBlockPos(), b.readVarInt(), b.readVarInt());
    }

    public static void handle(BlackjackPacket m, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context c = ctx.get();
        c.enqueueWork(() -> {
            ServerPlayer p = c.getSender();
            if (p == null || !p.level().isLoaded(m.pos)) {
                return;
            }
            var st = p.level().getBlockState(m.pos);
            if (!(st.getBlock() instanceof BlackjackBlock)) {
                return;
            }
            BlockPos master = BlackjackBlock.masterOf(m.pos, st);
            if (p.distanceToSqr(master.getX() + 0.5, master.getY() + 1.0, master.getZ() + 0.5) <= 12 * 12
                    && p.level().getBlockEntity(master) instanceof BlackjackBE be) {
                be.act(p, m.action, m.bet);
            }
        });
        c.setPacketHandled(true);
    }
}
