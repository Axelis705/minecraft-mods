package com.showdeaxelis.net;

import com.showdeaxelis.ShowMod;
import com.showdeaxelis.client.ShowClient;
import com.showdeaxelis.data.ShowManager;
import java.util.function.Supplier;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

/** Comunicacion cliente <-> servidor. El servidor decide todo; el cliente solo muestra y manda lo que el jugador pide. */
public final class ShowNet {
    private static final String VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(ShowMod.MOD_ID, "show"), () -> VERSION, VERSION::equals, VERSION::equals);

    private ShowNet() {
    }

    /** Cliente -> servidor: una accion del presentador o del jugador (el servidor revisa quien puede hacerla). */
    public record Action(String act, String s, int n) {
    }

    /** Servidor -> cliente: el estado completo del show para este jugador. */
    public record State(CompoundTag tag) {
    }

    /** Servidor -> cliente: algo que debe sonar o verse ahora (sonido, texto del locutor, efecto, camara). */
    public record Cue(String type, String text, int a, int b) {
    }

    /** Un trazo del dibujo (op 0) o borrar el lienzo (op 1). Va del dibujante al servidor y del servidor a todos los demas. */
    public record Draw(int op, int color, int size, int[] pts) {
    }

    public static final int MAX_PTS = 80;

    public static void init() {
        int id = 0;
        CHANNEL.registerMessage(id++, Action.class, (m, b) -> {
            b.writeUtf(m.act(), 64);
            b.writeUtf(m.s(), 256);
            b.writeVarInt(m.n());
        }, b -> new Action(b.readUtf(64), b.readUtf(256), b.readVarInt()), (m, c) -> server(c, sp -> ShowManager.act(sp, m.act(), m.s(), m.n())));
        CHANNEL.registerMessage(id++, State.class, (m, b) -> b.writeNbt(m.tag()), b -> new State(b.readNbt()),
                (m, c) -> client(c, () -> () -> ShowClient.onState(m.tag())));
        CHANNEL.registerMessage(id++, Cue.class, (m, b) -> {
            b.writeUtf(m.type(), 32);
            b.writeUtf(m.text(), 512);
            b.writeVarInt(m.a());
            b.writeVarInt(m.b());
        }, b -> new Cue(b.readUtf(32), b.readUtf(512), b.readVarInt(), b.readVarInt()), (m, c) -> client(c, () -> () -> ShowClient.onCue(m)));
        CHANNEL.registerMessage(id++, Draw.class, (m, b) -> {
            b.writeVarInt(m.op());
            b.writeVarInt(m.color());
            b.writeVarInt(m.size());
            b.writeVarInt(m.pts().length);
            for (int v : m.pts()) {
                b.writeVarInt(v);
            }
        }, b -> {
            int op = b.readVarInt();
            int color = b.readVarInt();
            int size = b.readVarInt();
            int n = Math.min(b.readVarInt(), MAX_PTS * 2);
            int[] pts = new int[n];
            for (int i = 0; i < n; i++) {
                pts[i] = b.readVarInt();
            }
            return new Draw(op, color, size, pts);
        }, (m, c) -> {
            NetworkEvent.Context ctx = c.get();
            if (ctx.getDirection().getReceptionSide().isServer()) {
                server(c, sp -> ShowManager.onDraw(sp, m.op(), m.color(), m.size(), m.pts()));
            } else {
                client(c, () -> () -> com.showdeaxelis.client.DrawCanvas.onPacket(m.op(), m.color(), m.size(), m.pts()));
            }
        });
    }

    public static void toPlayer(ServerPlayer sp, Object msg) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> sp), msg);
    }

    public static void toAll(Object msg) {
        CHANNEL.send(PacketDistributor.ALL.noArg(), msg);
    }

    private static void server(Supplier<NetworkEvent.Context> c, java.util.function.Consumer<ServerPlayer> r) {
        NetworkEvent.Context ctx = c.get();
        ctx.enqueueWork(() -> {
            ServerPlayer sp = ctx.getSender();
            if (sp != null) {
                r.accept(sp);
            }
        });
        ctx.setPacketHandled(true);
    }

    private static void client(Supplier<NetworkEvent.Context> c, Supplier<Runnable> r) {
        NetworkEvent.Context ctx = c.get();
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, r));
        ctx.setPacketHandled(true);
    }
}
