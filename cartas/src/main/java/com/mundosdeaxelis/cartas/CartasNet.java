package com.mundosdeaxelis.cartas;

import com.mundosdeaxelis.cartas.client.CartasClient;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

/** Comunicacion cliente <-> servidor del sistema de cartas. El servidor decide todo; el cliente solo muestra. */
public final class CartasNet {
    private static final String VERSION = "2";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(Cartas.MOD_ID, "cartas"), () -> VERSION, VERSION::equals, VERSION::equals);

    private CartasNet() {
    }

    // ---------- paquetes ----------

    /** Cliente -> servidor: quiero abrir el album. */
    public record OpenRequest() {
    }

    /** Cliente -> servidor: pongo esta carta en su hueco del album. */
    public record PlaceRequest(String id, boolean holo) {
    }

    /** Servidor -> cliente: abre el album con este contenido. */
    public record AlbumOpen(Set<String> normal, Set<String> holo) {
    }

    /** Servidor -> cliente: estas cartas se acaban de guardar (para la animacion) y asi queda el album. */
    public record InsertResult(List<String> ids, List<Boolean> holo, Set<String> normal, Set<String> holoAll, float before, float after) {
    }

    /** Servidor -> cliente: se abrio un sobre; estas son las cartas (ya estan en el inventario). */
    public record PackOpen(String pack, List<String> ids, List<Boolean> holo, List<Boolean> isNew) {
    }

    // ---------- registro ----------

    public static void init() {
        int id = 0;
        CHANNEL.registerMessage(id++, OpenRequest.class, (m, b) -> {
        }, b -> new OpenRequest(), (m, c) -> server(c, sp -> openAlbum(sp)));
        CHANNEL.registerMessage(id++, PlaceRequest.class, (m, b) -> {
            b.writeUtf(m.id());
            b.writeBoolean(m.holo());
        }, b -> new PlaceRequest(b.readUtf(), b.readBoolean()), (m, c) -> server(c, sp -> AlbumInsert.place(sp, m.id(), m.holo())));
        CHANNEL.registerMessage(id++, AlbumOpen.class, (m, b) -> {
            writeSet(b, m.normal());
            writeSet(b, m.holo());
        }, b -> new AlbumOpen(readSet(b), readSet(b)), (m, c) -> client(c, () -> () -> CartasClient.onAlbumOpen(m)));
        CHANNEL.registerMessage(id++, InsertResult.class, (m, b) -> {
            writeList(b, m.ids());
            writeBools(b, m.holo());
            writeSet(b, m.normal());
            writeSet(b, m.holoAll());
            b.writeFloat(m.before());
            b.writeFloat(m.after());
        }, b -> new InsertResult(readList(b), readBools(b), readSet(b), readSet(b), b.readFloat(), b.readFloat()),
                (m, c) -> client(c, () -> () -> CartasClient.onInsertResult(m)));
        CHANNEL.registerMessage(id++, PackOpen.class, (m, b) -> {
            b.writeUtf(m.pack());
            writeList(b, m.ids());
            writeBools(b, m.holo());
            writeBools(b, m.isNew());
        }, b -> new PackOpen(b.readUtf(), readList(b), readBools(b), readBools(b)),
                (m, c) -> client(c, () -> () -> CartasClient.onPackOpen(m)));
    }

    // ---------- envio ----------

    public static void toPlayer(ServerPlayer sp, Object msg) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> sp), msg);
    }

    public static void openAlbum(ServerPlayer sp) {
        AlbumCap cap = AlbumCaps.of(sp);
        toPlayer(sp, new AlbumOpen(new LinkedHashSet<>(cap.normal), new LinkedHashSet<>(cap.holo)));
    }

    // ---------- ayudas ----------

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

    private static void writeSet(FriendlyByteBuf b, Set<String> s) {
        writeList(b, new ArrayList<>(s));
    }

    private static Set<String> readSet(FriendlyByteBuf b) {
        return new LinkedHashSet<>(readList(b));
    }

    private static void writeList(FriendlyByteBuf b, List<String> l) {
        b.writeVarInt(l.size());
        for (String s : l) {
            b.writeUtf(s);
        }
    }

    private static List<String> readList(FriendlyByteBuf b) {
        int n = b.readVarInt();
        List<String> l = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            l.add(b.readUtf());
        }
        return l;
    }

    private static void writeBools(FriendlyByteBuf b, List<Boolean> l) {
        b.writeVarInt(l.size());
        for (boolean x : l) {
            b.writeBoolean(x);
        }
    }

    private static List<Boolean> readBools(FriendlyByteBuf b) {
        int n = b.readVarInt();
        List<Boolean> l = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            l.add(b.readBoolean());
        }
        return l;
    }
}
