package com.mundosdeaxelis.network;

import com.mundosdeaxelis.client.ClientBridge;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Servidor -> jugadores en la pelea contra un jefe: cuánto daño ha hecho cada uno (en vivo) y, al terminar,
 * el reparto final con la probabilidad de extras que le tocó a cada quien.
 */
public class BossDamagePacket {
    /** pct: porcentaje del daño total (0-100). chance: probabilidad de extras en % (solo en el resultado final, -1 en vivo). */
    public record Entry(String name, int pct, int chance) {
    }

    public final String boss;
    public final boolean end;
    public final List<Entry> entries;

    public BossDamagePacket(String boss, boolean end, List<Entry> entries) {
        this.boss = boss;
        this.end = end;
        this.entries = entries;
    }

    public static void encode(BossDamagePacket m, FriendlyByteBuf buf) {
        buf.writeUtf(m.boss, 64);
        buf.writeBoolean(m.end);
        buf.writeVarInt(m.entries.size());
        for (Entry e : m.entries) {
            buf.writeUtf(e.name, 48);
            buf.writeVarInt(e.pct);
            buf.writeVarInt(e.chance + 1);
        }
    }

    public static BossDamagePacket decode(FriendlyByteBuf buf) {
        String boss = buf.readUtf(64);
        boolean end = buf.readBoolean();
        int n = buf.readVarInt();
        List<Entry> l = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            l.add(new Entry(buf.readUtf(48), buf.readVarInt(), buf.readVarInt() - 1));
        }
        return new BossDamagePacket(boss, end, l);
    }

    public static void handle(BossDamagePacket m, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context c = ctx.get();
        c.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientBridge.bossDamage(m)));
        c.setPacketHandled(true);
    }
}
