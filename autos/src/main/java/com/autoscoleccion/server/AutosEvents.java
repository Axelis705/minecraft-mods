package com.autoscoleccion.server;

import com.autoscoleccion.AutosMod;
import com.autoscoleccion.registry.ModItems;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Al entrar: entrega el auto que se quedó a medio abrir. Al salir: cancela la prueba de la máquina. */
@Mod.EventBusSubscriber(modid = AutosMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class AutosEvents {
    private AutosEvents() {
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent e) {
        if (e.getEntity() instanceof ServerPlayer sp && AutosData.pendingCar(sp) >= 0) {
            Unboxing.claim(sp);
            sp.sendSystemMessage(Component.literal("§eTe entregamos el auto de la caja que dejaste abierta."));
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) {
        if (e.getEntity() instanceof ServerPlayer sp) {
            Machine.cancel(sp);
        }
    }

    /** Ayuda para dar un objeto: al inventario o, si no cabe, al suelo. */
    public static void give(ServerPlayer sp, ItemStack stack) {
        if (!sp.getInventory().add(stack)) {
            sp.drop(stack, false);
        }
    }

    static ItemStack carStack(int car) {
        return new ItemStack(ModItems.CARS.get(car).get());
    }
}
