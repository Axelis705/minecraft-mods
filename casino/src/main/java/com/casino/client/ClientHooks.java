package com.casino.client;

import com.casino.net.WheelWinPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

/** Entradas del cliente (se llaman solo desde el lado del cliente). */
public final class ClientHooks {
    private ClientHooks() {
    }

    public static void open(net.minecraft.client.gui.screens.Screen s) {
        //#if MC >= 26.2
        //$$ Minecraft.getInstance().gui.setScreen(s);
        //#else
        Minecraft.getInstance().setScreen(s);
        //#endif
    }

    public static void openCashier(BlockPos pos) {
        open(new CashierScreen(pos));
    }

    public static void openPlinko(BlockPos pos) {
        open(new PlinkoScreen(pos));
    }

    public static void openSlot(BlockPos pos) {
        open(new SlotScreen(pos));
    }

    public static void openScratchShop(BlockPos pos) {
        open(new ScratchShopScreen(pos));
    }

    public static void openScratchCard(boolean offHand) {
        open(new ScratchScreen(offHand));
    }

    public static void openPoker(BlockPos pos) {
        open(new PokerScreen(pos));
    }

    public static void openBlackjack(BlockPos pos) {
        open(new BlackjackScreen(pos));
    }

    public static void openRoulette(BlockPos pos) {
        open(new RouletteScreen(pos));
    }

    public static void showWin(WheelWinPacket p) {
        WinBanner.start(p.title, p.items, p.xp, p.big);
    }

    public static void openNeon(BlockPos pos) {
        open(new NeonScreen(pos));
    }

    public static void openGuide() {
        open(new GuideScreen());
    }
}
