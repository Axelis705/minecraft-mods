package com.pooltable.net;

import com.pooltable.client.PoolClient;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Servidor -> cliente: el estado de la partida en este tick (marcador, cuenta atrás, disco y los dos mazos). */
public final class HockeySyncPacket {
    public final BlockPos pos;
    public final byte state;
    public final byte scoreA;
    public final byte scoreB;
    public final float timer;
    public final float px;
    public final float pz;
    public final float vx;
    public final float vz;
    public final float ax;
    public final float az;
    public final float bx;
    public final float bz;

    public HockeySyncPacket(BlockPos pos, byte state, byte scoreA, byte scoreB, float timer, float px, float pz, float vx, float vz, float ax, float az, float bx, float bz) {
        this.pos = pos;
        this.state = state;
        this.scoreA = scoreA;
        this.scoreB = scoreB;
        this.timer = timer;
        this.px = px;
        this.pz = pz;
        this.vx = vx;
        this.vz = vz;
        this.ax = ax;
        this.az = az;
        this.bx = bx;
        this.bz = bz;
    }

    public static void encode(HockeySyncPacket m, FriendlyByteBuf b) {
        b.writeBlockPos(m.pos);
        b.writeByte(m.state);
        b.writeByte(m.scoreA);
        b.writeByte(m.scoreB);
        b.writeFloat(m.timer);
        b.writeFloat(m.px);
        b.writeFloat(m.pz);
        b.writeFloat(m.vx);
        b.writeFloat(m.vz);
        b.writeFloat(m.ax);
        b.writeFloat(m.az);
        b.writeFloat(m.bx);
        b.writeFloat(m.bz);
    }

    public static HockeySyncPacket decode(FriendlyByteBuf b) {
        return new HockeySyncPacket(b.readBlockPos(), b.readByte(), b.readByte(), b.readByte(), b.readFloat(), b.readFloat(), b.readFloat(), b.readFloat(),
                b.readFloat(), b.readFloat(), b.readFloat(), b.readFloat(), b.readFloat());
    }

    public static void handle(HockeySyncPacket m, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> PoolClient.onHockeySync(m)));
        ctx.get().setPacketHandled(true);
    }
}
