package com.pooltable.net;

import com.pooltable.block.PoolTableBE;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Cliente -> servidor: una acción del menú de la mesa (elegir modo, apuntarse, salir, empezar, reiniciar). */
public final class PoolActionPacket {
    public final BlockPos pos;
    public final int action;
    public final int arg;

    public PoolActionPacket(BlockPos pos, int action, int arg) {
        this.pos = pos;
        this.action = action;
        this.arg = arg;
    }

    public static void encode(PoolActionPacket m, FriendlyByteBuf b) {
        b.writeBlockPos(m.pos);
        b.writeVarInt(m.action);
        b.writeVarInt(m.arg);
    }

    public static PoolActionPacket decode(FriendlyByteBuf b) {
        return new PoolActionPacket(b.readBlockPos(), b.readVarInt(), b.readVarInt());
    }

    public static void handle(PoolActionPacket m, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context c = ctx.get();
        c.enqueueWork(() -> {
            ServerPlayer p = c.getSender();
            if (p == null || !p.level().isLoaded(m.pos)) {
                return;
            }
            BlockEntity be = p.level().getBlockEntity(m.pos);
            if (be instanceof PoolTableBE t) {
                Vec3 ctr = t.center();
                // el servidor comprueba que el jugador esté cerca de la mesa (no se fía del cliente)
                if (p.distanceToSqr(ctr.x, ctr.y, ctr.z) <= 12 * 12) {
                    t.menuAction(p, m.action, m.arg);
                }
            }
        });
        c.setPacketHandled(true);
    }
}
