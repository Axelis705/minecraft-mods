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
