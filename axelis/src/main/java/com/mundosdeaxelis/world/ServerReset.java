package com.mundosdeaxelis.world;

import com.mundosdeaxelis.MundosDeAxelis;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Limpieza al cerrar el servidor. En un mundo de un jugador el juego sigue abierto al salir al menú, y todo lo que el mod guarda
 * "en memoria" (tareas programadas, candados de entrada, dungeons en curso, escenas, peleas...) pasaría al siguiente mundo que se
 * abra: tareas que tocan un mundo ya cerrado, un candado que deja sin poder entrar a la dungeon durante mucho rato, una dungeon
 * que parece estar "construyéndose" para siempre. Aquí se deja todo como recién arrancado.
 */
@Mod.EventBusSubscriber(modid = MundosDeAxelis.MODID)
public final class ServerReset {
    private ServerReset() {
    }

    /** Antes de guardar el mundo: devolver lo que una escena a medias dejó cambiado en los bloques. */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onStopping(ServerStoppingEvent e) {
        try {
            BossIntro.onServerStopping(e.getServer());
        } catch (Exception ex) {
            MundosDeAxelis.LOGGER.error("No se pudo devolver el suelo de la escena de la Forja", ex);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onStopped(ServerStoppedEvent e) {
        Runnable[] all = {
                Scheduler::clearRuntime,
                BossTravel::clearRuntime,
                ForestDungeon::clearRuntime,
                ForgeDungeon::clearRuntime,
                DungeonManager::clearRuntime,
                ForgeManager::clearRuntime,
                IceTravel::clearRuntime,
                TowerTravel::clearRuntime,
                FireTravel::clearRuntime,
                BossIntro::clearRuntime,
                IceIntro::clearRuntime,
                TowerIntro::clearRuntime,
                Props::clearRuntime,
                HeatZones::clearRuntime,
                Cold::clearRuntime,
                BossDamage::clearRuntime,
                BossRewards::clearRuntime,
                DungeonReset::clearRuntime,
                Estructuras::clearRuntime,
                Volcanes::clearRuntime,
                com.mundosdeaxelis.cinematic.CinematicManager::clearRuntime,
                com.mundosdeaxelis.block.BossPortalBlock::clearRuntime,
                com.mundosdeaxelis.block.VaultChestBlockEntity::clearRuntime,
                com.mundosdeaxelis.block.Seats::clearRuntime,
                com.mundosdeaxelis.relic.RedThreadItem::clearRuntime,
                com.mundosdeaxelis.relic.RewindAnchorItem::clearRuntime,
                com.mundosdeaxelis.gear.GearEvents::clearRuntime,
        };
        for (Runnable r : all) {
            try {
                r.run();
            } catch (Exception ex) {
                MundosDeAxelis.LOGGER.error("Error al limpiar el estado del mod al cerrar el servidor", ex);
            }
        }
    }
}
