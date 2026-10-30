package com.kensei.net;

import com.kensei.Kensei;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

/** Canal de red de Kensei. Cliente -> servidor: pedir un esquive o un ataque. Servidor -> cliente: energía y combo para las barras, animaciones de los demás y sacudida de cámara. */
public final class KenseiNet {
    private static final String PROTOCOL = "5";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(Kensei.MODID, "main"),
            () -> PROTOCOL,
            PROTOCOL::equals,
            PROTOCOL::equals);

    private static boolean registered = false;

    private KenseiNet() {
    }

    public static void register() {
        if (registered) {
            return;
        }
        registered = true;
        int id = 0;
        CHANNEL.registerMessage(id++, DodgePacket.class, DodgePacket::encode, DodgePacket::decode, DodgePacket::handle);
        CHANNEL.registerMessage(id++, StatePacket.class, StatePacket::encode, StatePacket::decode, StatePacket::handle);
        CHANNEL.registerMessage(id++, MovePacket.class, MovePacket::encode, MovePacket::decode, MovePacket::handle);
        CHANNEL.registerMessage(id++, AnimPacket.class, AnimPacket::encode, AnimPacket::decode, AnimPacket::handle);
        CHANNEL.registerMessage(id++, ShakePacket.class, ShakePacket::encode, ShakePacket::decode, ShakePacket::handle);
        CHANNEL.registerMessage(id++, FxPacket.class, FxPacket::encode, FxPacket::decode, FxPacket::handle);
        CHANNEL.registerMessage(id++, DuelKeyPacket.class, DuelKeyPacket::encode, DuelKeyPacket::decode, DuelKeyPacket::handle);
        CHANNEL.registerMessage(id++, DuelStatePacket.class, DuelStatePacket::encode, DuelStatePacket::decode, DuelStatePacket::handle);
        CHANNEL.registerMessage(id++, OpenDuelMenuPacket.class, OpenDuelMenuPacket::encode, OpenDuelMenuPacket::decode, OpenDuelMenuPacket::handle);
        CHANNEL.registerMessage(id++, DuelRequestPacket.class, DuelRequestPacket::encode, DuelRequestPacket::decode, DuelRequestPacket::handle);
    }

    public static void sendTo(ServerPlayer p, StatePacket pkt) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> p), pkt);
    }

    public static void openDuelMenu(ServerPlayer p, int targetId) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> p), new OpenDuelMenuPacket(targetId));
    }

    public static void sendDuel(ServerPlayer p, DuelStatePacket pkt) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> p), pkt);
    }

    public static void shake(ServerPlayer p, float strength, int ticks) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> p), new ShakePacket(strength, ticks));
    }

    /** Manda un efecto visual a todos los jugadores que estén cerca del punto. */
    public static void fx(net.minecraft.server.level.ServerLevel level, int type, double x, double y, double z, float value, int extra,
                          int attackerId, int victimId, float yaw) {
        FxPacket pkt = new FxPacket(type, (float) x, (float) y, (float) z, value, extra, attackerId, victimId, yaw);
        CHANNEL.send(PacketDistributor.NEAR.with(() -> new PacketDistributor.TargetPoint(x, y, z, 48.0, level.dimension())), pkt);
    }

    /** Avisa a los que ven a esta entidad (y a ella misma si includeSelf) de que empezó un movimiento. */
    public static void broadcastAnim(Entity e, int move, float arg, boolean includeSelf) {
        AnimPacket pkt = new AnimPacket(e.getId(), move, arg);
        if (includeSelf) {
            CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> e), pkt);
        } else {
            CHANNEL.send(PacketDistributor.TRACKING_ENTITY.with(() -> e), pkt);
        }
    }
}
