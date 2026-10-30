package com.casino.client;

import com.casino.net.WheelWinPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

/** Entradas del cliente (se llaman solo desde el lado del cliente). */
public final class ClientHooks {
    private ClientHooks() {
    }

    public static void openCashier(BlockPos pos) {
        Minecraft.getInstance().setScreen(new CashierScreen(pos));
    }

    public static void openPlinko(BlockPos pos) {
        Minecraft.getInstance().setScreen(new PlinkoScreen(pos));
    }

    public static void openSlot(BlockPos pos) {
        Minecraft.getInstance().setScreen(new SlotScreen(pos));
    }

    public static void showWin(WheelWinPacket p) {
        WinBanner.start(p.title, p.items, p.xp, p.big);
    }
}
