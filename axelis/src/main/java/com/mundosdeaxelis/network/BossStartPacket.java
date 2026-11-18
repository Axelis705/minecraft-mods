package com.mundosdeaxelis.network;

import com.mundosdeaxelis.block.BossPortalBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Cliente -> servidor: el jugador pulsó «Comenzar» en la ficha del jefe. El servidor lo revisa todo otra vez. */
public class BossStartPacket {
    private final BlockPos pos;

    public BossStartPacket(BlockPos pos) {
        this.pos = pos;
    }

    public static void encode(BossStartPacket m, FriendlyByteBuf buf) {
        buf.writeBlockPos(m.pos);
    }

    public static BossStartPacket decode(FriendlyByteBuf buf) {
        return new BossStartPacket(buf.readBlockPos());
    }

    public static void handle(BossStartPacket m, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context c = ctx.get();
        ServerPlayer sp = c.getSender();
        c.enqueueWork(() -> {
            if (sp != null) {
                BossPortalBlock.tryStart(sp, m.pos);
            }
        });
        c.setPacketHandled(true);
    }
}
