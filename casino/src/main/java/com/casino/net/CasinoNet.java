package com.casino.net;

import com.casino.plat.Net;
import net.minecraft.server.level.ServerPlayer;

/** Todos los mensajes del casino (el canal y su versión están en plat/Net). */
public final class CasinoNet {
    private static boolean registered = false;

    private CasinoNet() {
    }

    public static void register() {
        if (registered) {
            return;
        }
        registered = true;
        Net.register(0, CashierPacket.class, CashierPacket::encode, CashierPacket::decode, CashierPacket::handle);
        Net.register(1, WheelWinPacket.class, WheelWinPacket::encode, WheelWinPacket::decode, WheelWinPacket::handle);
        Net.register(2, PlinkoPacket.class, PlinkoPacket::encode, PlinkoPacket::decode, PlinkoPacket::handle);
        Net.register(3, SlotPacket.class, SlotPacket::encode, SlotPacket::decode, SlotPacket::handle);
        Net.register(4, BlackjackPacket.class, BlackjackPacket::encode, BlackjackPacket::decode, BlackjackPacket::handle);
        Net.register(5, OpenBlackjackPacket.class, OpenBlackjackPacket::encode, OpenBlackjackPacket::decode, OpenBlackjackPacket::handle);
        Net.register(6, RoulettePacket.class, RoulettePacket::encode, RoulettePacket::decode, RoulettePacket::handle);
        Net.register(7, OpenRoulettePacket.class, OpenRoulettePacket::encode, OpenRoulettePacket::decode, OpenRoulettePacket::handle);
        Net.register(8, PokerPacket.class, PokerPacket::encode, PokerPacket::decode, PokerPacket::handle);
        Net.register(9, OpenPokerPacket.class, OpenPokerPacket::encode, OpenPokerPacket::decode, OpenPokerPacket::handle);
        Net.register(10, ScratchBuyPacket.class, ScratchBuyPacket::encode, ScratchBuyPacket::decode, ScratchBuyPacket::handle);
        Net.register(11, ScratchCardPacket.class, ScratchCardPacket::encode, ScratchCardPacket::decode, ScratchCardPacket::handle);
        Net.register(12, NeonPacket.class, NeonPacket::encode, NeonPacket::decode, NeonPacket::handle);
        Net.register(13, com.casino.diamond.OpenElevatorPacket.class, com.casino.diamond.OpenElevatorPacket::encode, com.casino.diamond.OpenElevatorPacket::decode, com.casino.diamond.OpenElevatorPacket::handle);
        Net.register(14, com.casino.diamond.ElevatorGoPacket.class, com.casino.diamond.ElevatorGoPacket::encode, com.casino.diamond.ElevatorGoPacket::decode, com.casino.diamond.ElevatorGoPacket::handle);
        Net.register(15, com.casino.diamond.ShopPacket.Open.class, com.casino.diamond.ShopPacket.Open::encode, com.casino.diamond.ShopPacket.Open::decode, com.casino.diamond.ShopPacket.Open::handle);
        Net.register(16, com.casino.diamond.ShopPacket.Buy.class, com.casino.diamond.ShopPacket.Buy::encode, com.casino.diamond.ShopPacket.Buy::decode, com.casino.diamond.ShopPacket.Buy::handle);
        Net.register(17, com.casino.diamond.ShopPacket.Edit.class, com.casino.diamond.ShopPacket.Edit::encode, com.casino.diamond.ShopPacket.Edit::decode, com.casino.diamond.ShopPacket.Edit::handle);
        Net.register(18, com.casino.diamond.RaceBetPacket.class, com.casino.diamond.RaceBetPacket::encode, com.casino.diamond.RaceBetPacket::decode, com.casino.diamond.RaceBetPacket::handle);
        Net.done();
    }

    public static void sendToServer(Object pkt) {
        Net.toServer(pkt);
    }

    public static void sendTo(ServerPlayer p, Object pkt) {
        Net.toPlayer(p, pkt);
    }
}
