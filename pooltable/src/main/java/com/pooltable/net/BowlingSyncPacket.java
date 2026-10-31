package com.pooltable.net;

import com.pooltable.client.PoolClient;
import com.pooltable.game.BowlingPhysics;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Servidor -> cliente: dónde están la bola y los pinos en este tick (mientras algo se mueve). */
public final class BowlingSyncPacket {
    public final BlockPos pos;
    public final float bu;
    public final float bv;
    public final boolean ballActive;
    public final float[] u;
    public final float[] v;
    public final byte[] state;
    public final boolean moving;

    public BowlingSyncPacket(BlockPos pos, float bu, float bv, boolean ballActive, float[] u, float[] v, byte[] state, boolean moving) {
        this.pos = pos;
        this.bu = bu;
        this.bv = bv;
        this.ballActive = ballActive;
        this.u = u;
        this.v = v;
        this.state = state;
        this.moving = moving;
    }

    public static void encode(BowlingSyncPacket m, FriendlyByteBuf b) {
        b.writeBlockPos(m.pos);
        b.writeFloat(m.bu);
        b.writeFloat(m.bv);
        b.writeBoolean(m.ballActive);
        b.writeBoolean(m.moving);
        for (int i = 0; i < BowlingPhysics.PINS; i++) {
            b.writeFloat(m.u[i]);
            b.writeFloat(m.v[i]);
            b.writeByte(m.state[i]);
        }
    }

    public static BowlingSyncPacket decode(FriendlyByteBuf b) {
        BlockPos pos = b.readBlockPos();
        float bu = b.readFloat();
        float bv = b.readFloat();
        boolean active = b.readBoolean();
        boolean moving = b.readBoolean();
        float[] u = new float[BowlingPhysics.PINS];
        float[] v = new float[BowlingPhysics.PINS];
        byte[] st = new byte[BowlingPhysics.PINS];
        for (int i = 0; i < BowlingPhysics.PINS; i++) {
            u[i] = b.readFloat();
            v[i] = b.readFloat();
            st[i] = b.readByte();
        }
        return new BowlingSyncPacket(pos, bu, bv, active, u, v, st, moving);
    }

    public static void handle(BowlingSyncPacket m, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> PoolClient.onBowlingSync(m)));
        ctx.get().setPacketHandled(true);
    }
}
