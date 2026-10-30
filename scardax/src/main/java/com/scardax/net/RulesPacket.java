package com.scardax.net;

import com.scardax.client.ClientBridge;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.Map;
import java.util.function.Supplier;

/** Servidor -> cliente: las reglas de spawneo (solo las distintas de 100) y el modo estricto. Si {@code open} es true, además abre la pantalla. */
public class RulesPacket {
    public final boolean open;
    public final boolean strict;
    public final Map<ResourceLocation, Integer> rules;

    public RulesPacket(boolean open, boolean strict, Map<ResourceLocation, Integer> rules) {
        this.open = open;
        this.strict = strict;
        this.rules = rules;
    }

    public static void encode(RulesPacket m, FriendlyByteBuf buf) {
        buf.writeBoolean(m.open);
        buf.writeBoolean(m.strict);
        ScardaxNet.writeRules(buf, m.rules);
    }

    public static RulesPacket decode(FriendlyByteBuf buf) {
        boolean open = buf.readBoolean();
        boolean strict = buf.readBoolean();
        return new RulesPacket(open, strict, ScardaxNet.readRules(buf));
    }

    public static void handle(RulesPacket m, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context c = ctx.get();
        c.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientBridge.rules(m.open, m.strict, m.rules)));
        c.setPacketHandled(true);
    }
}
