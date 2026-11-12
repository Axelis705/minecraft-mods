package com.autoscoleccion.net;

import com.autoscoleccion.AutosMod;
import com.autoscoleccion.client.AutosClient;
import com.autoscoleccion.client.RcDriving;
import com.autoscoleccion.game.SkillGame;
import com.autoscoleccion.server.Driving;
import com.autoscoleccion.server.Machine;
import com.autoscoleccion.server.Unboxing;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** Comunicación cliente <-> servidor. El servidor decide todo (puntaje, caja, auto); el cliente juega, anima y muestra. */
public final class AutosNet {
    private static final String VERSION = "3";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(new ResourceLocation(AutosMod.MODID, "main"), () -> VERSION,
            VERSION::equals, VERSION::equals);
    /** Máximo de teclas que se aceptan de una partida (sobra para cualquier prueba). */
    public static final int MAX_INPUTS = 600;

    private AutosNet() {
    }

    public static void register() {
        int id = 0;
        CHANNEL.registerMessage(id++, StartGamePacket.class, (m, b) -> {
            b.writeVarInt(m.session());
            b.writeVarInt(m.type());
            b.writeLong(m.seed());
            b.writeBlockPos(m.pos());
        }, b -> new StartGamePacket(b.readVarInt(), b.readVarInt(), b.readLong(), b.readBlockPos()),
                (m, c) -> client(c, () -> () -> AutosClient.onStart(m)));
        CHANNEL.registerMessage(id++, GameResultPacket.class, (m, b) -> {
            b.writeVarInt(m.session());
            b.writeVarInt(m.endT());
            b.writeVarInt(m.inputs().size());
            for (SkillGame.Input in : m.inputs()) {
                b.writeVarInt(in.t());
                b.writeVarInt(in.key());
                b.writeBoolean(in.down());
            }
        }, b -> {
            int session = b.readVarInt();
            int endT = b.readVarInt();
            int n = b.readVarInt();
            if (n < 0 || n > MAX_INPUTS) {
                throw new IllegalArgumentException("demasiadas teclas: " + n);
            }
            List<SkillGame.Input> list = new ArrayList<>(n);
            for (int i = 0; i < n; i++) {
                list.add(new SkillGame.Input(b.readVarInt(), b.readVarInt(), b.readBoolean()));
            }
            return new GameResultPacket(session, endT, list);
        }, (m, c) -> server(c, sp -> Machine.finish(sp, m.session(), m.inputs(), m.endT())));
        CHANNEL.registerMessage(id++, GameOutcomePacket.class, (m, b) -> {
            b.writeVarInt(m.session());
            b.writeFloat(m.score());
            b.writeVarInt(m.box());
            b.writeFloat(m.o0());
            b.writeFloat(m.o1());
            b.writeFloat(m.o2());
            b.writeFloat(m.o3());
        }, b -> new GameOutcomePacket(b.readVarInt(), b.readFloat(), b.readVarInt(), b.readFloat(), b.readFloat(), b.readFloat(), b.readFloat()),
                (m, c) -> client(c, () -> () -> AutosClient.onOutcome(m)));
        CHANNEL.registerMessage(id++, UnboxPacket.class, (m, b) -> {
            b.writeVarInt(m.box());
            b.writeVarInt(m.car());
            b.writeBoolean(m.isNew());
        }, b -> new UnboxPacket(b.readVarInt(), b.readVarInt(), b.readBoolean()), (m, c) -> client(c, () -> () -> AutosClient.onUnbox(m)));
        CHANNEL.registerMessage(id++, ClaimPacket.class, (m, b) -> b.writeVarInt(m.what()), b -> new ClaimPacket(b.readVarInt()),
                (m, c) -> server(c, sp -> {
                    if (m.what() == ClaimPacket.CLAIM_CAR) {
                        Unboxing.claim(sp);
                    } else if (m.what() == ClaimPacket.CANCEL_GAME) {
                        Machine.cancel(sp);
                    }
                }));
        CHANNEL.registerMessage(id++, CollectionPacket.class, (m, b) -> {
            b.writeVarIntArray(m.counts());
            b.writeBoolean(m.open());
        }, b -> new CollectionPacket(b.readVarIntArray(64), b.readBoolean()), (m, c) -> client(c, () -> () -> AutosClient.onCollection(m)));
        registerDriving(id);
    }

    private static void registerDriving(int id) {
        CHANNEL.registerMessage(id++, DrivePacket.class, (m, b) -> b.writeVarInt(m.car()), b -> new DrivePacket(b.readVarInt()), (m, c) -> server(c, sp -> {
            if (m.car() >= 0) {
                Driving.start(sp, m.car());
            } else {
                Driving.stop(sp);
            }
        }));
        CHANNEL.registerMessage(id++, DriveStartPacket.class, (m, b) -> b.writeVarInt(m.entityId()), b -> new DriveStartPacket(b.readVarInt()),
                (m, c) -> client(c, () -> () -> RcDriving.onStart(m.entityId())));
        CHANNEL.registerMessage(id++, CarMovePacket.class, (m, b) -> {
            b.writeVarInt(m.entityId());
            b.writeDouble(m.x());
            b.writeDouble(m.y());
            b.writeDouble(m.z());
            b.writeFloat(m.yaw());
            b.writeFloat(m.steer());
        }, b -> new CarMovePacket(b.readVarInt(), b.readDouble(), b.readDouble(), b.readDouble(), b.readFloat(), b.readFloat()),
                (m, c) -> server(c, sp -> Driving.move(sp, m.entityId(), m.x(), m.y(), m.z(), m.yaw(), m.steer())));
    }

    private static void server(Supplier<NetworkEvent.Context> c, Consumer<ServerPlayer> run) {
        NetworkEvent.Context ctx = c.get();
        ServerPlayer sp = ctx.getSender();
        if (sp != null) {
            ctx.enqueueWork(() -> run.accept(sp));
        }
        ctx.setPacketHandled(true);
    }

    private static void client(Supplier<NetworkEvent.Context> c, Supplier<Runnable> run) {
        NetworkEvent.Context ctx = c.get();
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, run));
        ctx.setPacketHandled(true);
    }

    public static void toPlayer(ServerPlayer sp, Object msg) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> sp), msg);
    }

    public static void toServer(Object msg) {
        CHANNEL.sendToServer(msg);
    }
}
