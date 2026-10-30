package com.pooltable.client;

import com.pooltable.block.PoolTableBE;
import com.pooltable.item.PoolCueItem;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;

/** Cuando hay varias mesas cerca, solo se dibuja la línea de puntería en la que está más cerca el jugador (la misma que usaría el servidor). */
final class ClientAim {
    private ClientAim() {
    }

    static boolean nearest(PoolTableBE be, LocalPlayer p) {
        if (p.level() == null) {
            return false;
        }
        Vec3 pos = p.position();
        return PoolTableBE.findNear(p.level(), pos, PoolCueItem.REACH + 0.4) == be;
    }
}
