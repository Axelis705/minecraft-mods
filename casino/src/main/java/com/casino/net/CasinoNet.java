package com.casino.net;

import com.casino.Casino;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public final class CasinoNet {
    private static final String PROTOCOL = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(Casino.MODID, "main"), () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);
    private static boolean registered = false;

    private CasinoNet() {
    }

    public static void register() {
        if (registered) {
            return;
        }
        registered = true;
        CHANNEL.registerMessage(0, CashierPacket.class, CashierPacket::encode, CashierPacket::decode, CashierPacket::handle);
        CHANNEL.registerMessage(1, WheelWinPacket.class, WheelWinPacket::encode, WheelWinPacket::decode, WheelWinPacket::handle);
        CHANNEL.registerMessage(2, PlinkoPacket.class, PlinkoPacket::encode, PlinkoPacket::decode, PlinkoPacket::handle);
        CHANNEL.registerMessage(3, SlotPacket.class, SlotPacket::encode, SlotPacket::decode, SlotPacket::handle);
        CHANNEL.registerMessage(4, BlackjackPacket.class, BlackjackPacket::encode, BlackjackPacket::decode, BlackjackPacket::handle);
        CHANNEL.registerMessage(5, OpenBlackjackPacket.class, OpenBlackjackPacket::encode, OpenBlackjackPacket::decode, OpenBlackjackPacket::handle);
        CHANNEL.registerMessage(6, RoulettePacket.class, RoulettePacket::encode, RoulettePacket::decode, RoulettePacket::handle);
        CHANNEL.registerMessage(8, PokerPacket.class, PokerPacket::encode, PokerPacket::decode, PokerPacket::handle);
        CHANNEL.registerMessage(9, OpenPokerPacket.class, OpenPokerPacket::encode, OpenPokerPacket::decode, OpenPokerPacket::handle);
        CHANNEL.registerMessage(7, OpenRoulettePacket.class, OpenRoulettePacket::encode, OpenRoulettePacket::decode, OpenRoulettePacket::handle);
    }

    public static void sendToServer(Object pkt) {
        CHANNEL.sendToServer(pkt);
    }

    public static void sendTo(ServerPlayer p, Object pkt) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> p), pkt);
    }
}
