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

    /** Abre el menú de la mesa (clic derecho con la mano libre). */
    public static void openMenu(net.minecraft.core.BlockPos pos) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null && mc.level.getBlockEntity(pos) instanceof PoolTableBE) {
            mc.setScreen(new PoolMenuScreen(pos));
        }
    }
}
