package com.pooltable.net;

import com.pooltable.PoolTable;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public final class PoolNet {
    private static final String PROTOCOL = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(PoolTable.MODID, "main"), () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);
    private static boolean registered = false;

    private PoolNet() {
    }

    public static void register() {
        if (registered) {
            return;
        }
        registered = true;
        CHANNEL.registerMessage(0, PoolSyncPacket.class, PoolSyncPacket::encode, PoolSyncPacket::decode, PoolSyncPacket::handle);
        CHANNEL.registerMessage(1, PoolActionPacket.class, PoolActionPacket::encode, PoolActionPacket::decode, PoolActionPacket::handle);
        CHANNEL.registerMessage(2, BowlingActionPacket.class, BowlingActionPacket::encode, BowlingActionPacket::decode, BowlingActionPacket::handle);
        CHANNEL.registerMessage(3, BowlingSyncPacket.class, BowlingSyncPacket::encode, BowlingSyncPacket::decode, BowlingSyncPacket::handle);
    }

    public static void sendToServer(Object pkt) {
        CHANNEL.sendToServer(pkt);
    }

    public static void sendToTracking(ServerLevel level, BlockPos pos, Object pkt) {
        CHANNEL.send(PacketDistributor.TRACKING_CHUNK.with(() -> level.getChunkAt(pos)), pkt);
    }
}
