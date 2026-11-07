package com.pooltable.client;

import com.pooltable.block.PoolTableBE;
import com.pooltable.net.PoolSyncPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.block.entity.BlockEntity;

/** Entrada del cliente para los paquetes de la mesa. */
public final class PoolClient {
    private PoolClient() {
    }

    public static void onSync(PoolSyncPacket m) {
        if (Minecraft.getInstance().level == null) {
            return;
        }
        BlockEntity be = Minecraft.getInstance().level.getBlockEntity(m.pos);
        if (be instanceof PoolTableBE t) {
            t.clientReceive(m.x, m.z, m.mask, m.moving);
        }
    }

    public static void onBowlingSync(com.pooltable.net.BowlingSyncPacket m) {
        if (Minecraft.getInstance().level == null) {
            return;
        }
        BlockEntity be = Minecraft.getInstance().level.getBlockEntity(m.pos);
        if (be instanceof com.pooltable.block.BowlingLaneBE t) {
            t.clientReceive(m.bu, m.bv, m.ballActive, m.u, m.v, m.state, m.moving);
        }
    }

    public static void openBowlingMenu(net.minecraft.core.BlockPos pos) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null && mc.level.getBlockEntity(pos) instanceof com.pooltable.block.BowlingLaneBE) {
            mc.setScreen(new BowlingMenuScreen(pos));
        }
    }

    public static void onHockeySync(com.pooltable.net.HockeySyncPacket m) {
        if (Minecraft.getInstance().level == null) {
            return;
        }
        BlockEntity be = Minecraft.getInstance().level.getBlockEntity(m.pos);
        if (be instanceof com.pooltable.block.AirHockeyBE t) {
            t.clientReceive(m.state, m.scoreA, m.scoreB, m.timer, m.px, m.pz, m.vx, m.vz, m.ax, m.az, m.bx, m.bz);
        }
    }

    public static void openHockey(net.minecraft.core.BlockPos pos) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null && mc.level.getBlockEntity(pos) instanceof com.pooltable.block.AirHockeyBE) {
            mc.setScreen(new HockeyScreen(pos));
        }
    }

    /** Abre el menú de la mesa (clic derecho con la mano libre). */
    public static void openMenu(net.minecraft.core.BlockPos pos) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null && mc.level.getBlockEntity(pos) instanceof PoolTableBE) {
            mc.setScreen(new PoolMenuScreen(pos));
        }
    }

    public static void onVolleySync(com.pooltable.net.VolleySyncPacket m) {
        if (Minecraft.getInstance().level == null) {
            return;
        }
        BlockEntity be = Minecraft.getInstance().level.getBlockEntity(m.pos);
        if (be instanceof com.pooltable.block.VolleyCourtBE t) {
            t.clientReceive(m);
            VolleyClient.onSync(t, m);
        }
    }

    public static void openVolley(net.minecraft.core.BlockPos pos) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null && mc.level.getBlockEntity(pos) instanceof com.pooltable.block.VolleyCourtBE) {
            mc.setScreen(new VolleyMenuScreen(pos));
        }
    }
}
