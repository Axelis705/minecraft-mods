package com.mundosdeaxelis.network;

import com.mundosdeaxelis.block.VaultChestBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Cliente -> servidor: cómo terminó el minijuego de la cerradura (0 = salió, 1 = abierta, 2 = fallada). El servidor lo revisa. */
public class LockpickResultPacket {
    public final BlockPos pos;
    public final int code;

    public LockpickResultPacket(BlockPos pos, int code) {
        this.pos = pos;
        this.code = code;
    }

    public static void encode(LockpickResultPacket m, FriendlyByteBuf buf) {
        buf.writeBlockPos(m.pos);
        buf.writeVarInt(m.code);
    }

    public static LockpickResultPacket decode(FriendlyByteBuf buf) {
        return new LockpickResultPacket(buf.readBlockPos(), buf.readVarInt());
    }

    public static void handle(LockpickResultPacket m, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context c = ctx.get();
        ServerPlayer sp = c.getSender();
        c.enqueueWork(() -> {
            if (sp != null) {
                VaultChestBlockEntity.onLockResult(sp, m.pos, m.code);
            }
        });
        c.setPacketHandled(true);
    }
}
