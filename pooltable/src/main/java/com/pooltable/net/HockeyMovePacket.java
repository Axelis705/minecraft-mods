package com.pooltable.net;

import com.pooltable.block.AirHockeyBE;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Cliente -> servidor: dónde quiere el jugador su mazo (marco de la mesa). El servidor lo limita a la mitad del jugador y a su velocidad máxima. */
public final class HockeyMovePacket {
    public final BlockPos pos;
    public final float x;
    public final float z;

    public HockeyMovePacket(BlockPos pos, float x, float z) {
        this.pos = pos;
        this.x = x;
        this.z = z;
    }

    public static void encode(HockeyMovePacket m, FriendlyByteBuf b) {
        b.writeBlockPos(m.pos);
        b.writeFloat(m.x);
        b.writeFloat(m.z);
    }

    public static HockeyMovePacket decode(FriendlyByteBuf b) {
        return new HockeyMovePacket(b.readBlockPos(), b.readFloat(), b.readFloat());
    }

    public static void handle(HockeyMovePacket m, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context c = ctx.get();
        c.enqueueWork(() -> {
            ServerPlayer p = c.getSender();
            if (p == null || !p.level().isLoaded(m.pos)) {
                return;
            }
            BlockEntity be = p.level().getBlockEntity(m.pos);
            if (be instanceof AirHockeyBE t) {
                t.setTarget(p, m.x, m.z);
            }
        });
        c.setPacketHandled(true);
    }
}
