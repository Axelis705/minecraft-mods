package com.axeliscore;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Eventos del juego que el Core escucha para todos los mods (por ahora: mandar los datos de cada jugador al entrar). */
//#if MC >= 26.1
//$$ @Mod.EventBusSubscriber(modid = AxelisCore.MODID)
//#endif
public final class CoreEvents {
    private CoreEvents() {
    }

    /** Lo que los mods quieren hacer cuando entra un jugador ({@link com.axeliscore.AxelisCore#onLogin}). */
    static final java.util.List<java.util.function.Consumer<ServerPlayer>> LOGIN = new java.util.concurrent.CopyOnWriteArrayList<>();

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) {
            com.axeliscore.data.PlayerStore.onLogin(p);
            for (var l : LOGIN) {
                try {
                    l.accept(p);
                } catch (RuntimeException ex) {
                    AxelisCore.LOGGER.error("Axelis Core: falló algo al entrar {}", p.getGameProfile().getName(), ex);
                }
            }
        }
    }
}
