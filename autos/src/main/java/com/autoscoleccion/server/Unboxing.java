package com.autoscoleccion.server;

import com.autoscoleccion.game.Boxes;
import com.autoscoleccion.game.CarDef;
import com.autoscoleccion.game.Cars;
import com.autoscoleccion.game.Rarity;
import com.autoscoleccion.net.AutosNet;
import com.autoscoleccion.net.CollectionPacket;
import com.autoscoleccion.net.UnboxPacket;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

/**
 * Abrir una caja: el servidor elige el auto en cuanto se abre (y gasta la caja), lo deja "pendiente" y el cliente hace la animación.
 * Al terminar (o al cerrar la pantalla) el cliente pide el auto y se entrega. Si se cae el juego en medio, se entrega al volver.
 */
public final class Unboxing {
    private Unboxing() {
    }

    public static void open(ServerPlayer sp, InteractionHand hand, int box) {
        if (AutosData.pendingCar(sp) >= 0) {
            // hay uno a medio abrir: primero ese
            claim(sp);
        }
        ItemStack held = sp.getItemInHand(hand);
        if (!sp.getAbilities().instabuild) {
            held.shrink(1);
        }
        int car = Boxes.roll(box, sp.getRandom());
        boolean isNew = !AutosData.has(sp, car);
        AutosData.setPending(sp, car, box);
        AutosData.countOpened(sp);
        AutosNet.toPlayer(sp, new UnboxPacket(box, car, isNew));
    }

    /** Entrega el auto pendiente (si hay). */
    public static void claim(ServerPlayer sp) {
        int car = AutosData.pendingCar(sp);
        if (car < 0 || car >= Cars.COUNT) {
            AutosData.setPending(sp, -1, 0);
            return;
        }
        AutosData.setPending(sp, -1, 0);
        boolean first = AutosData.addCar(sp, car);
        AutosEvents.give(sp, AutosEvents.carStack(car));
        CarDef c = Cars.get(car);
        if (first && (c.rarity == Rarity.LEGENDARIO || c.rarity == Rarity.EPICO)) {
            sp.getServer().getPlayerList().broadcastSystemMessage(Component.literal((c.rarity == Rarity.LEGENDARIO ? "§6" : "§d") + "✦ "
                    + sp.getGameProfile().getName() + " consiguió el " + c.rarity.label.toLowerCase() + " §l" + c.name + "§r"
                    + (c.rarity == Rarity.LEGENDARIO ? "§6" : "§d") + " para su colección."), false);
        }
        AutosNet.toPlayer(sp, new CollectionPacket(AutosData.counts(sp), false));
    }
}
