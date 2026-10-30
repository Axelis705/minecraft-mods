package com.kensei.net;

import com.kensei.client.ClientState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Servidor -> cliente: la energía actual, los ticks que quedan de contraataque, el golpe del combo y la espera del crítico. Solo sirve para dibujar las barras. */
public class StatePacket {
    public final float energy;
    public final int counterTicks;
    public final int combo;
    public final int heavyCooldown;
    public final float stamina;
    public final float staminaMax;
    /** Ticks que le quedan de agotamiento (0 = no está agotado). */
    public final int exhaustTicks;

    public StatePacket(float energy, int counterTicks, int combo, int heavyCooldown, float stamina, float staminaMax, int exhaustTicks) {
        this.energy = energy;
        this.counterTicks = counterTicks;
        this.combo = combo;
        this.heavyCooldown = heavyCooldown;
        this.stamina = stamina;
        this.staminaMax = staminaMax;
        this.exhaustTicks = exhaustTicks;
    }

    public static void encode(StatePacket m, FriendlyByteBuf buf) {
        buf.writeFloat(m.energy);
        buf.writeVarInt(m.counterTicks);
        buf.writeVarInt(m.combo);
        buf.writeVarInt(m.heavyCooldown);
        buf.writeFloat(m.stamina);
        buf.writeFloat(m.staminaMax);
        buf.writeVarInt(m.exhaustTicks);
    }

    public static StatePacket decode(FriendlyByteBuf buf) {
        return new StatePacket(buf.readFloat(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readFloat(), buf.readFloat(), buf.readVarInt());
    }

    public static void handle(StatePacket m, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context c = ctx.get();
        c.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientState.update(m.energy, m.counterTicks, m.combo, m.heavyCooldown, m.stamina, m.staminaMax, m.exhaustTicks)));
        c.setPacketHandled(true);
    }
}
