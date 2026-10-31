package com.pooltable.net;

import com.pooltable.block.AirHockeyBE;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Cliente -> servidor: una acción de la mesa de hockey (jugar contra la máquina con un nivel, esperar rival, unirse, salir). */
public final class HockeyActionPacket {
    public final BlockPos pos;
    public final int action;
    public final int arg;

    public HockeyActionPacket(BlockPos pos, int action, int arg) {
        this.pos = pos;
        this.action = action;
        this.arg = arg;
    }

    public static void encode(HockeyActionPacket m, FriendlyByteBuf b) {
        b.writeBlockPos(m.pos);
        b.writeVarInt(m.action);
        b.writeVarInt(m.arg);
    }

    public static HockeyActionPacket decode(FriendlyByteBuf b) {
        return new HockeyActionPacket(b.readBlockPos(), b.readVarInt(), b.readVarInt());
    }

    public static void handle(HockeyActionPacket m, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context c = ctx.get();
        c.enqueueWork(() -> {
            ServerPlayer p = c.getSender();
            if (p == null || !p.level().isLoaded(m.pos)) {
                return;
            }
            BlockEntity be = p.level().getBlockEntity(m.pos);
            if (be instanceof AirHockeyBE t) {
                Vec3 ctr = t.center();
                if (p.distanceToSqr(ctr.x, ctr.y, ctr.z) <= 10 * 10) {
                    t.menuAction(p, m.action, m.arg);
                }
            }
        });
        c.setPacketHandled(true);
    }
}
