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
 * Servidor -> cliente: el plano de la dungeon para el minimapa (o {@code on = false} para quitarlo). Cada sala va como
 * {id, columna, fila, tipo, puertas (4 bits: norte, este, sur, oeste), 1 si es del camino principal, 1 si está despejada,
 * forma (0 cuadrada, 1 larga norte-sur, 2 larga este-oeste)}.
 */
public class DungeonMapPacket {
    public final boolean on;
    public final List<int[]> cells;

    public DungeonMapPacket(boolean on, List<int[]> cells) {
        this.on = on;
        this.cells = cells;
    }

    public static DungeonMapPacket off() {
        return new DungeonMapPacket(false, new ArrayList<>());
    }

    public static void encode(DungeonMapPacket m, FriendlyByteBuf buf) {
        buf.writeBoolean(m.on);
        buf.writeVarInt(m.cells.size());
        for (int[] c : m.cells) {
            buf.writeVarInt(c[0]);
            buf.writeVarInt(c[1] + 16);
            buf.writeVarInt(c[2] + 16);
            buf.writeVarInt(c[3]);
            buf.writeVarInt(c[4]);
            buf.writeVarInt(c[5]);
            buf.writeVarInt(c[6]);
            buf.writeVarInt(c[7]);
        }
    }

    public static DungeonMapPacket decode(FriendlyByteBuf buf) {
        boolean on = buf.readBoolean();
        int n = buf.readVarInt();
        List<int[]> cells = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            cells.add(new int[]{buf.readVarInt(), buf.readVarInt() - 16, buf.readVarInt() - 16, buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt()});
        }
        return new DungeonMapPacket(on, cells);
    }

    public static void handle(DungeonMapPacket m, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context c = ctx.get();
        c.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientBridge.dungeonMap(m)));
        c.setPacketHandled(true);
    }
}
