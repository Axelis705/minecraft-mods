package com.casino.net;

import com.casino.block.RouletteBE;
import com.casino.block.RouletteBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;


/**
 * Cliente -> servidor: una jugada en una mesa de ruleta. El servidor comprueba todo (distancia, fase, fichas, límites).
 * arg1 y arg2 según la acción: PLACE (casilla, fichas), REMOVE (casilla, fichas), CALL (cuál, valor de la ficha), el resto no los usa.
 */
public final class RoulettePacket {
    public static final int PLACE = 0;
    public static final int REMOVE = 1;
    public static final int UNDO = 2;
    public static final int CLEAR = 3;
    public static final int REBET = 4;
    public static final int DOUBLE = 5;
    public static final int READY = 6;
    public static final int CALL = 7;

    public final BlockPos pos;
    public final int action;
    public final int arg1;
    public final int arg2;

    public RoulettePacket(BlockPos pos, int action, int arg1, int arg2) {
        this.pos = pos;
        this.action = action;
        this.arg1 = arg1;
        this.arg2 = arg2;
    }

    public static void encode(RoulettePacket m, FriendlyByteBuf b) {
        b.writeBlockPos(m.pos);
        b.writeVarInt(m.action);
        b.writeVarInt(m.arg1);
        b.writeVarInt(m.arg2);
    }

    public static RoulettePacket decode(FriendlyByteBuf b) {
        return new RoulettePacket(b.readBlockPos(), b.readVarInt(), b.readVarInt(), b.readVarInt());
    }

    public static void handle(RoulettePacket m, ServerPlayer p) {
        if (p == null || !p.level().isLoaded(m.pos)) {
            return;
        }
        var st = p.level().getBlockState(m.pos);
        if (!(st.getBlock() instanceof RouletteBlock)) {
            return;
        }
        BlockPos master = RouletteBlock.masterOf(m.pos, st);
        if (p.distanceToSqr(master.getX() + 0.5, master.getY() + 1.0, master.getZ() + 0.5) <= 14 * 14
                && p.level().getBlockEntity(master) instanceof RouletteBE be) {
            be.act(p, m.action, m.arg1, m.arg2);
        }
    }
}
