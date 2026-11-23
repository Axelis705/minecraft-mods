package com.axeliscore.plat;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.network.PacketDistributor;
//#if MC >= 1.21
//$$ import net.minecraftforge.network.ChannelBuilder;
//$$ import net.minecraftforge.network.SimpleChannel;
//#else
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
//#endif

import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * Un canal de mensajes entre cliente y servidor, igual en todas las versiones. Cada mod crea el suyo:
 *
 * <pre>
 *     public static final Net NET = new Net("pesca", "main", 1);
 *     // en el arranque común (FMLCommonSetupEvent, dentro de enqueueWork):
 *     NET.register(PescaMsg.class, PescaMsg::write, PescaMsg::read, (msg, sender) -> ...);
 *     NET.done();
 * </pre>
 * <p>
 * Cada mensaje se maneja en el hilo principal; {@code sender} es el jugador que lo mandó (en el servidor) o null (en el cliente). Si
 * cambias qué mensajes hay o cómo se escriben, sube el número de protocolo: así un cliente viejo no entra a un servidor nuevo.
 */
public final class Net {
    public interface Handler<M> {
        void handle(M msg, ServerPlayer sender);
    }

    private final SimpleChannel channel;
    private int next;

    public Net(String ns, String name, int protocol) {
        //#if MC >= 1.21
        //$$ this.channel = ChannelBuilder.named(Ids.rl(ns, name)).networkProtocolVersion(protocol).simpleChannel();
        //#else
        String p = String.valueOf(protocol);
        this.channel = NetworkRegistry.newSimpleChannel(Ids.rl(ns, name), () -> p, p::equals, p::equals);
        //#endif
    }

    /** Registra un tipo de mensaje (el número se asigna en orden: registra siempre en el mismo orden). */
    public <M> void register(Class<M> cls, BiConsumer<M, FriendlyByteBuf> enc, Function<FriendlyByteBuf, M> dec, Handler<M> handler) {
        int id = next++;
        //#if MC >= 1.21
        //$$ channel.messageBuilder(cls, id).encoder(enc).decoder(dec).consumerMainThread((m, ctx) -> handler.handle(m, ctx.getSender())).add();
        //#else
        channel.registerMessage(id, cls, enc, dec, (m, sup) -> {
            var ctx = sup.get();
            ctx.enqueueWork(() -> handler.handle(m, ctx.getSender()));
            ctx.setPacketHandled(true);
        });
        //#endif
    }

    /** Al terminar de registrar (en 1.21+ el canal se cierra). */
    public void done() {
        //#if MC >= 1.21
        //$$ channel.build();
        //#endif
    }

    public void toServer(Object msg) {
        //#if MC >= 1.21
        //$$ channel.send(msg, PacketDistributor.SERVER.noArg());
        //#else
        channel.sendToServer(msg);
        //#endif
    }

    public void toPlayer(ServerPlayer p, Object msg) {
        //#if MC >= 1.21
        //$$ channel.send(msg, PacketDistributor.PLAYER.with(p));
        //#else
        channel.send(PacketDistributor.PLAYER.with(() -> p), msg);
        //#endif
    }

    /** A todos los jugadores conectados. */
    public void toAll(MinecraftServer server, Object msg) {
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            toPlayer(p, msg);
        }
    }

    /** A los jugadores del mismo mundo que estén a menos de {@code range} bloques de la entidad (los que la ven). */
    public void toNear(Entity e, double range, Object msg) {
        for (net.minecraft.world.entity.player.Player p : e.level().players()) {
            if (p instanceof ServerPlayer sp && sp.distanceToSqr(e) <= range * range) {
                toPlayer(sp, msg);
            }
        }
    }

    /** Corre algo solo si este es el cliente del juego (las clases del cliente no se cargan en un servidor). */
    public static void onClient(Runnable r) {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            r.run();
        }
    }
}
