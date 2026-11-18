package com.mundosdeaxelis.network;

import com.mundosdeaxelis.client.ClientBridge;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/** Servidor -> jugador: sus lápidas activas (dimensión, posición y ticks que le quedan) para el contador y la flecha. */
public class GravePacket {
    public record Item(ResourceLocation dim, BlockPos pos, int ticksLeft) {
    }

    public final List<Item> graves;

    public GravePacket(List<Item> graves) {
        this.graves = graves;
    }

    public static void encode(GravePacket m, FriendlyByteBuf buf) {
        buf.writeVarInt(m.graves.size());
        for (Item i : m.graves) {
            buf.writeResourceLocation(i.dim);
            buf.writeBlockPos(i.pos);
            buf.writeVarInt(i.ticksLeft);
        }
    }

    public static GravePacket decode(FriendlyByteBuf buf) {
        int n = buf.readVarInt();
        List<Item> l = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            l.add(new Item(buf.readResourceLocation(), buf.readBlockPos(), buf.readVarInt()));
        }
        return new GravePacket(l);
    }

    public static void handle(GravePacket m, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context c = ctx.get();
        c.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientBridge.graves(m)));
        c.setPacketHandled(true);
    }
}
