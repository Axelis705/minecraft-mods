package com.mundosdeaxelis.client;

import com.mundosdeaxelis.MundosDeAxelis;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Al salir de un mundo o de un servidor se borra lo que el mod tenía "a medias" en pantalla. Si no, al entrar en otro mundo
 * podía seguir la pantalla en negro de una entrada a dungeon (hasta 5 minutos), la cámara de una escena, el jugador congelado
 * sin poder moverse, la barra de frío, el mapa de la dungeon o los avisos de antes.
 */
@Mod.EventBusSubscriber(modid = MundosDeAxelis.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class ClientReset {
    private ClientReset() {
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut e) {
        Runnable[] all = {
                FadeOverlay::reset,
                CinematicClient::reset,
                FrozenClient::reset,
                BossTitleOverlay::reset,
                ScreenFx::reset,
                ColdHud::reset,
                GraveHud::reset,
                OreRadar::reset,
                BossDamageHud::reset,
                DungeonMapHud::reset,
                NoticeHud::reset,
                LootHud::reset,
                ClientCapes::clear,
        };
        for (Runnable r : all) {
            try {
                r.run();
            } catch (Exception ex) {
                MundosDeAxelis.LOGGER.error("Error al limpiar la pantalla del mod al salir", ex);
            }
        }
    }
}
