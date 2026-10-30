package com.casino.net;

import com.casino.block.LuckyWheelBlock;
import com.casino.block.PlinkoBE;
import com.casino.block.PlinkoBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Cliente -> servidor: soltar una bola en una mesa de Plinko con una apuesta y un nivel de riesgo. El servidor lo comprueba todo. */
public final class PlinkoPacket {
    public final BlockPos pos;
    public final int bet;
    public final int risk;

    public PlinkoPacket(BlockPos pos, int bet, int risk) {
        this.pos = pos;
        this.bet = bet;
        this.risk = risk;
    }

    public static void encode(PlinkoPacket m, FriendlyByteBuf b) {
        b.writeBlockPos(m.pos);
        b.writeVarInt(m.bet);
        b.writeVarInt(m.risk);
    }

    public static PlinkoPacket decode(FriendlyByteBuf b) {
        return new PlinkoPacket(b.readBlockPos(), b.readVarInt(), b.readVarInt());
    }

    public static void handle(PlinkoPacket m, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context c = ctx.get();
        c.enqueueWork(() -> {
            ServerPlayer p = c.getSender();
            if (p == null || !p.level().isLoaded(m.pos)) {
                return;
            }
            var st = p.level().getBlockState(m.pos);
            if (!(st.getBlock() instanceof PlinkoBlock)) {
                return;
            }
            BlockPos master = LuckyWheelBlock.masterOf(m.pos, st);
            if (p.distanceToSqr(master.getX() + 0.5, master.getY() + 2.0, master.getZ() + 0.5) <= 12 * 12
                    && p.level().getBlockEntity(master) instanceof PlinkoBE be) {
                be.drop(p, m.bet, m.risk);
            }
        });
        c.setPacketHandled(true);
    }
}
