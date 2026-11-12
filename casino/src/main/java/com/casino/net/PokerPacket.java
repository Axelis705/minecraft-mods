package com.casino.net;

import com.casino.block.PokerBE;
import com.casino.block.PokerBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;


/** Cliente -> servidor: una jugada en una mesa de póker. El servidor comprueba todo (distancia, turno, fichas). */
public final class PokerPacket {
    /** Poner la apuesta inicial (ante). */
    public static final int ANTE = 0;
    /** Igualar (paga el doble del ante). */
    public static final int CALL = 1;
    /** Retirarse. */
    public static final int FOLD = 2;
    /** Repartir ya, sin esperar a más jugadores. */
    public static final int START = 3;

    public final BlockPos pos;
    public final int action;
    public final int bet;

    public PokerPacket(BlockPos pos, int action, int bet) {
        this.pos = pos;
        this.action = action;
        this.bet = bet;
    }

    public static void encode(PokerPacket m, FriendlyByteBuf b) {
        b.writeBlockPos(m.pos);
        b.writeVarInt(m.action);
        b.writeVarInt(m.bet);
    }

    public static PokerPacket decode(FriendlyByteBuf b) {
        return new PokerPacket(b.readBlockPos(), b.readVarInt(), b.readVarInt());
    }

    public static void handle(PokerPacket m, ServerPlayer p) {
        if (p == null || !p.level().isLoaded(m.pos)) {
            return;
        }
        var st = p.level().getBlockState(m.pos);
        if (!(st.getBlock() instanceof PokerBlock)) {
            return;
        }
        BlockPos master = PokerBlock.masterOf(m.pos, st);
        if (p.distanceToSqr(master.getX() + 0.5, master.getY() + 1.0, master.getZ() + 0.5) <= 12 * 12
                && p.level().getBlockEntity(master) instanceof PokerBE be) {
            be.act(p, m.action, m.bet);
        }
    }
}
