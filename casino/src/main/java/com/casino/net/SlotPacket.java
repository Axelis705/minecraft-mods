package com.casino.net;

import com.casino.block.SlotBE;
import com.casino.block.SlotBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Cliente -> servidor: tirar de la palanca de una tragamonedas con una apuesta. El servidor lo comprueba todo. */
public final class SlotPacket {
    public final BlockPos pos;
    public final int bet;

    public SlotPacket(BlockPos pos, int bet) {
        this.pos = pos;
        this.bet = bet;
    }

    public static void encode(SlotPacket m, FriendlyByteBuf b) {
        b.writeBlockPos(m.pos);
        b.writeVarInt(m.bet);
    }

    public static SlotPacket decode(FriendlyByteBuf b) {
        return new SlotPacket(b.readBlockPos(), b.readVarInt());
    }

    public static void handle(SlotPacket m, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context c = ctx.get();
        c.enqueueWork(() -> {
            ServerPlayer p = c.getSender();
            if (p == null || !p.level().isLoaded(m.pos)) {
                return;
            }
            var st = p.level().getBlockState(m.pos);
            if (!(st.getBlock() instanceof SlotBlock)) {
                return;
            }
            BlockPos master = SlotBlock.masterOf(m.pos, st);
            if (p.distanceToSqr(master.getX() + 0.5, master.getY() + 1.0, master.getZ() + 0.5) <= 12 * 12
                    && p.level().getBlockEntity(master) instanceof SlotBE be) {
                be.spin(p, m.bet);
            }
        });
        c.setPacketHandled(true);
    }
}
