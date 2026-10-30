package com.kensei.net;

import com.kensei.client.Vfx;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Servidor -> clientes cercanos: pasó algo que merece un efecto visual (golpe, parry, esquive perfecto, bloqueo, guardia rota). */
public class FxPacket {
    public static final int HIT = 0;
    public static final int PARRY = 1;
    public static final int PERFECT_DODGE = 2;
    public static final int BLOCK = 3;
    public static final int GUARD_BREAK = 4;
    /** El jugador recibió daño (solo para mostrar el número). */
    public static final int DAMAGE_TAKEN = 5;
    /** Un peleador de duelo cayó (K.O.). */
    public static final int KO = 6;
    /** Se suma a {@code extra} de un HIT cuando el golpe mata al enemigo. */
    public static final int KILL_FLAG = 64;

    public final int type;
    public final float x;
    public final float y;
    public final float z;
    /** Daño final (HIT) o 0. */
    public final float value;
    /** Para HIT: el movimiento que pegó (1 a 4 tajo, 5 crítico, 7 estocada, 0 otro). */
    public final int extra;
    public final int attackerId;
    public final int victimId;
    /** Hacia dónde iba el golpe (dirección horizontal, en grados de yaw del atacante). */
    public final float yaw;

    public FxPacket(int type, float x, float y, float z, float value, int extra, int attackerId, int victimId, float yaw) {
        this.type = type;
        this.x = x;
        this.y = y;
        this.z = z;
        this.value = value;
        this.extra = extra;
        this.attackerId = attackerId;
        this.victimId = victimId;
        this.yaw = yaw;
    }

    public static void encode(FxPacket m, FriendlyByteBuf buf) {
        buf.writeByte(m.type);
        buf.writeFloat(m.x);
        buf.writeFloat(m.y);
        buf.writeFloat(m.z);
        buf.writeFloat(m.value);
        buf.writeByte(m.extra);
        buf.writeVarInt(m.attackerId + 1);
        buf.writeVarInt(m.victimId + 1);
        buf.writeFloat(m.yaw);
    }

    public static FxPacket decode(FriendlyByteBuf buf) {
        int type = buf.readByte();
        float x = buf.readFloat();
        float y = buf.readFloat();
        float z = buf.readFloat();
        float value = buf.readFloat();
        int extra = buf.readByte();
        int attacker = buf.readVarInt() - 1;
        int victim = buf.readVarInt() - 1;
        float yaw = buf.readFloat();
        return new FxPacket(type, x, y, z, value, extra, attacker, victim, yaw);
    }

    public static void handle(FxPacket m, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context c = ctx.get();
        c.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> Vfx.onServerFx(m)));
        c.setPacketHandled(true);
    }
}
