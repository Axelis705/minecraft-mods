package com.pooltable.net;

import com.pooltable.block.VolleyCourtBE;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Cliente -> servidor: una acción del menú de la cancha de voleibol (unirse a un equipo, tamaño, nivel, empezar, salir...). */
public final class VolleyMenuPacket {
    public final BlockPos pos;
    public final int action;
    public final int arg;

    public VolleyMenuPacket(BlockPos pos, int action, int arg) {
        this.pos = pos;
        this.action = action;
        this.arg = arg;
    }

    public static void encode(VolleyMenuPacket m, FriendlyByteBuf b) {
        b.writeBlockPos(m.pos);
        b.writeVarInt(m.action);
        b.writeVarInt(m.arg);
    }

    public static VolleyMenuPacket decode(FriendlyByteBuf b) {
        return new VolleyMenuPacket(b.readBlockPos(), b.readVarInt(), b.readVarInt());
    }

    public static void handle(VolleyMenuPacket m, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context c = ctx.get();
        c.enqueueWork(() -> {
            ServerPlayer p = c.getSender();
            if (p == null || !p.level().isLoaded(m.pos)) {
                return;
            }
            BlockEntity be = p.level().getBlockEntity(m.pos);
            if (be instanceof VolleyCourtBE t && p.distanceToSqr(m.pos.getX() + 0.5, m.pos.getY(), m.pos.getZ() + 0.5) <= 30 * 30) {
                t.menuAction(p, m.action, m.arg);
            }
        });
        c.setPacketHandled(true);
    }
}
