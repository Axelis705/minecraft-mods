package com.mundosdeaxelis.network;

import com.mundosdeaxelis.client.ClientBridge;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Servidor -> cliente: crea, mueve o quita un efecto visual dentro del mundo (anillos, columnas, cúpulas...). */
public class VfxPacket {
    public final int action, id, type, rgb, flags, life, follow;
    public final float size, a, b;
    public final double x, y, z, dx, dy, dz;

    public VfxPacket(int action, int id, int type, int rgb, int flags, int life, int follow, float size, float a, float b,
                     double x, double y, double z, double dx, double dy, double dz) {
        this.action = action;
        this.id = id;
        this.type = type;
        this.rgb = rgb;
        this.flags = flags;
        this.life = life;
        this.follow = follow;
        this.size = size;
        this.a = a;
        this.b = b;
        this.x = x;
        this.y = y;
        this.z = z;
        this.dx = dx;
        this.dy = dy;
        this.dz = dz;
    }

    public static VfxPacket remove(int id) {
        return new VfxPacket(com.mundosdeaxelis.vfx.VfxType.ACT_REMOVE, id, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
    }

    public static void encode(VfxPacket m, FriendlyByteBuf buf) {
        buf.writeByte(m.action);
        buf.writeVarInt(m.id);
        if (m.action == com.mundosdeaxelis.vfx.VfxType.ACT_REMOVE) {
            return;
        }
        buf.writeByte(m.type);
        buf.writeInt(m.rgb);
        buf.writeVarInt(m.flags);
        buf.writeVarInt(m.life);
        buf.writeVarInt(m.follow);
        buf.writeFloat(m.size);
        buf.writeFloat(m.a);
        buf.writeFloat(m.b);
        buf.writeDouble(m.x);
        buf.writeDouble(m.y);
        buf.writeDouble(m.z);
        buf.writeFloat((float) m.dx);
        buf.writeFloat((float) m.dy);
        buf.writeFloat((float) m.dz);
    }

    public static VfxPacket decode(FriendlyByteBuf buf) {
        int action = buf.readByte();
        int id = buf.readVarInt();
        if (action == com.mundosdeaxelis.vfx.VfxType.ACT_REMOVE) {
            return remove(id);
        }
        int type = buf.readByte();
        int rgb = buf.readInt();
        int flags = buf.readVarInt();
        int life = buf.readVarInt();
        int follow = buf.readVarInt();
        float size = buf.readFloat(), a = buf.readFloat(), b = buf.readFloat();
        double x = buf.readDouble(), y = buf.readDouble(), z = buf.readDouble();
        double dx = buf.readFloat(), dy = buf.readFloat(), dz = buf.readFloat();
        return new VfxPacket(action, id, type, rgb, flags, life, follow, size, a, b, x, y, z, dx, dy, dz);
    }

    public static void handle(VfxPacket msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientBridge.vfx(msg)));
        context.setPacketHandled(true);
    }
}
