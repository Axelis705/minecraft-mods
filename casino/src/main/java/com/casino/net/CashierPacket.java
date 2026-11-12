package com.casino.net;

import com.casino.block.CashierBlock;
import com.casino.block.Cashier;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;


/** Cliente -> servidor: una operación del cajero (comprar o canjear una cantidad de fichas con esmeraldas o diamantes). */
public final class CashierPacket {
    public final BlockPos pos;
    public final int kind;
    public final boolean buy;
    public final int amount;

    public CashierPacket(BlockPos pos, int kind, boolean buy, int amount) {
        this.pos = pos;
        this.kind = kind;
        this.buy = buy;
        this.amount = amount;
    }

    public static void encode(CashierPacket m, FriendlyByteBuf b) {
        b.writeBlockPos(m.pos);
        b.writeVarInt(m.kind);
        b.writeBoolean(m.buy);
        b.writeVarInt(m.amount);
    }

    public static CashierPacket decode(FriendlyByteBuf b) {
        return new CashierPacket(b.readBlockPos(), b.readVarInt(), b.readBoolean(), b.readVarInt());
    }

    public static void handle(CashierPacket m, ServerPlayer p) {
        if (p == null || !p.level().isLoaded(m.pos) || !(p.level().getBlockState(m.pos).getBlock() instanceof CashierBlock)) {
            return;
        }
        // el servidor comprueba que el jugador esté cerca del cajero
        if (p.distanceToSqr(m.pos.getX() + 0.5, m.pos.getY() + 0.5, m.pos.getZ() + 0.5) <= 10 * 10) {
            Cashier.exchange(p, m.kind, m.buy, m.amount);
        }
    }
}
