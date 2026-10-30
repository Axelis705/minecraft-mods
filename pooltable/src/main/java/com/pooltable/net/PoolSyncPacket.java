package com.pooltable.net;

import com.pooltable.client.PoolClient;
import com.pooltable.game.PoolPhysics;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Servidor -> cliente: dónde están las bolas en este tick (mientras algo se mueve). */
public final class PoolSyncPacket {
    public final BlockPos pos;
    public final float[] x;
    public final float[] z;
    public final int mask;
    public final boolean moving;

    public PoolSyncPacket(BlockPos pos, float[] x, float[] z, int mask, boolean moving) {
        this.pos = pos;
        this.x = x;
        this.z = z;
        this.mask = mask;
        this.moving = moving;
    }

    public static void encode(PoolSyncPacket m, FriendlyByteBuf b) {
        b.writeBlockPos(m.pos);
        for (int i = 0; i < PoolPhysics.N; i++) {
            b.writeFloat(m.x[i]);
            b.writeFloat(m.z[i]);
        }
        b.writeInt(m.mask);
        b.writeBoolean(m.moving);
    }

    public static PoolSyncPacket decode(FriendlyByteBuf b) {
        BlockPos pos = b.readBlockPos();
        float[] x = new float[PoolPhysics.N];
        float[] z = new float[PoolPhysics.N];
        for (int i = 0; i < PoolPhysics.N; i++) {
            x[i] = b.readFloat();
            z[i] = b.readFloat();
        }
        return new PoolSyncPacket(pos, x, z, b.readInt(), b.readBoolean());
    }

    public static void handle(PoolSyncPacket m, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> PoolClient.onSync(m)));
        ctx.get().setPacketHandled(true);
    }
}
