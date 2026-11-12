package com.casino.plat;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
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
 * Mensajes entre cliente y servidor, igual en todas las versiones. Cada mensaje se maneja en el hilo principal; {@code sender} es el
 * jugador que lo mandó (en el servidor) o null (en el cliente).
 */
public final class Net {
    public interface Handler<M> {
        void handle(M msg, ServerPlayer sender);
    }

    //#if MC >= 1.21
    //$$ private static final SimpleChannel CHANNEL = ChannelBuilder.named(Ids.casino("main")).networkProtocolVersion(3).simpleChannel();
    //#else
    private static final String PROTOCOL = "3";
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(Ids.casino("main"), () -> PROTOCOL, PROTOCOL::equals,
            PROTOCOL::equals);
    //#endif

    private Net() {
    }

    public static <M> void register(int id, Class<M> cls, BiConsumer<M, FriendlyByteBuf> enc, Function<FriendlyByteBuf, M> dec, Handler<M> handler) {
        //#if MC >= 1.21
        //$$ CHANNEL.messageBuilder(cls, id).encoder(enc).decoder(dec).consumerMainThread((m, ctx) -> handler.handle(m, ctx.getSender())).add();
        //#else
        CHANNEL.registerMessage(id, cls, enc, dec, (m, sup) -> {
            var ctx = sup.get();
            ctx.enqueueWork(() -> handler.handle(m, ctx.getSender()));
            ctx.setPacketHandled(true);
        });
        //#endif
    }

    /** Al terminar de registrar (en 1.21+ el canal se cierra). */
    public static void done() {
        //#if MC >= 1.21
        //$$ CHANNEL.build();
        //#endif
    }

    public static void toServer(Object msg) {
        //#if MC >= 1.21
        //$$ CHANNEL.send(msg, PacketDistributor.SERVER.noArg());
        //#else
        CHANNEL.sendToServer(msg);
        //#endif
    }

    public static void toPlayer(ServerPlayer p, Object msg) {
        //#if MC >= 1.21
        //$$ CHANNEL.send(msg, PacketDistributor.PLAYER.with(p));
        //#else
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> p), msg);
        //#endif
    }

    /** Corre algo solo si este es el cliente del juego (las clases del cliente no se cargan en un servidor). */
    public static void onClient(Runnable r) {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            r.run();
        }
    }
}
