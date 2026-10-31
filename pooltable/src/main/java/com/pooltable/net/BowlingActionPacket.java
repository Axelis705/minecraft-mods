package com.pooltable.net;

import com.pooltable.block.BowlingLaneBE;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Cliente -> servidor: una acción del menú de la pista (apuntarse, salir, empezar, terminar). */
public final class BowlingActionPacket {
    public final BlockPos pos;
    public final int action;

    public BowlingActionPacket(BlockPos pos, int action) {
        this.pos = pos;
        this.action = action;
    }

    public static void encode(BowlingActionPacket m, FriendlyByteBuf b) {
        b.writeBlockPos(m.pos);
        b.writeVarInt(m.action);
    }

    public static BowlingActionPacket decode(FriendlyByteBuf b) {
        return new BowlingActionPacket(b.readBlockPos(), b.readVarInt());
    }

    public static void handle(BowlingActionPacket m, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context c = ctx.get();
        c.enqueueWork(() -> {
            ServerPlayer p = c.getSender();
            if (p == null || !p.level().isLoaded(m.pos)) {
                return;
            }
            BlockEntity be = p.level().getBlockEntity(m.pos);
            if (be instanceof BowlingLaneBE t) {
                Vec3 ctr = t.center();
                if (p.distanceToSqr(ctr.x, ctr.y, ctr.z) <= 16 * 16) {
                    t.menuAction(p, m.action);
                }
            }
        });
        c.setPacketHandled(true);
    }
}
