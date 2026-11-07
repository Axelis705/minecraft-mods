package com.pooltable.net;

import com.pooltable.block.VolleyCourtBE;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Cliente -> servidor: el jugador golpea la pelota (clic izquierdo = golpear, derecho = colocar). Lleva hacia dónde apuntaba con WASD en el momento
 * (adelante = profundo, atrás = corto, a los lados = ángulos) y si usaba la tecla de lanzarse.
 */
public final class VolleyHitPacket {
    public final BlockPos pos;
    public final int kind;
    public final float fwd;
    public final float right;
    public final boolean dive;
    /** Fuerza de 0 a 1 con la barra del saque (en los demás golpes no importa). */
    public final float power;

    public VolleyHitPacket(BlockPos pos, int kind, float fwd, float right, boolean dive) {
        this(pos, kind, fwd, right, dive, 0.7F);
    }

    public VolleyHitPacket(BlockPos pos, int kind, float fwd, float right, boolean dive, float power) {
        this.power = power;
        this.pos = pos;
        this.kind = kind;
        this.fwd = fwd;
        this.right = right;
        this.dive = dive;
    }

    public static void encode(VolleyHitPacket m, FriendlyByteBuf b) {
        b.writeBlockPos(m.pos);
        b.writeByte(m.kind);
        b.writeFloat(m.fwd);
        b.writeFloat(m.right);
        b.writeBoolean(m.dive);
        b.writeFloat(m.power);
    }

    public static VolleyHitPacket decode(FriendlyByteBuf b) {
        return new VolleyHitPacket(b.readBlockPos(), b.readByte(), b.readFloat(), b.readFloat(), b.readBoolean(), b.readFloat());
    }

    public static void handle(VolleyHitPacket m, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context c = ctx.get();
        c.enqueueWork(() -> {
            ServerPlayer p = c.getSender();
            if (p == null || !p.level().isLoaded(m.pos) || Float.isNaN(m.fwd) || Float.isNaN(m.right) || Float.isNaN(m.power)) {
                return;
            }
            BlockEntity be = p.level().getBlockEntity(m.pos);
            if (be instanceof VolleyCourtBE t) {
                t.humanHit(p, m.kind == 1 ? 1 : 0, m.fwd, m.right, m.dive, m.power);
            }
        });
        c.setPacketHandled(true);
    }
}
