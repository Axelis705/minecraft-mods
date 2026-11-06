package com.showdeaxelis;

import com.showdeaxelis.data.Banks;
import com.showdeaxelis.data.ShowManager;
import com.showdeaxelis.data.StageData;
import com.showdeaxelis.data.Studio;
import com.showdeaxelis.entity.SpectatorEntity;
import com.showdeaxelis.registry.ModEntities;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Los eventos del servidor que mueven el show: chat, reloj, entradas y salidas. */
public final class ShowEvents {
    private ShowEvents() {
    }

    @Mod.EventBusSubscriber(modid = ShowMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class ModBus {
        @SubscribeEvent
        public static void attributes(EntityAttributeCreationEvent e) {
            e.put(ModEntities.SPECTATOR.get(), SpectatorEntity.createAttributes().build());
        }
    }

    @Mod.EventBusSubscriber(modid = ShowMod.MOD_ID)
    public static final class ForgeBus {
        @SubscribeEvent
        public static void started(ServerStartedEvent e) {
            ShowManager.server = e.getServer();
            Banks.load();
        }

        @SubscribeEvent
        public static void tick(TickEvent.ServerTickEvent e) {
            if (e.phase == TickEvent.Phase.END) {
                ShowManager.tick(e.getServer());
            }
        }

        /** Sin murcielagos en el estudio ni alrededor (el estudio es cerrado y los murcielagos salen en lugares oscuros). */
        @SubscribeEvent
        public static void join(EntityJoinLevelEvent e) {
            if (e.getLevel().isClientSide() || !(e.getEntity() instanceof Bat) || ShowManager.server == null) {
                return;
            }
            StageData.Info st = StageData.get(ShowManager.server).info();
            if (st != null && Studio.studioArea(st).inflate(48).contains(e.getEntity().position())) {
                e.setCanceled(true);
            }
        }

        /** Tocar la campana del escenario: cuenta como timbre en el cara a cara de 100 mexicanos. */
        @SubscribeEvent
        public static void bell(PlayerInteractEvent.RightClickBlock e) {
            if (e.getLevel().isClientSide() || !(e.getEntity() instanceof ServerPlayer sp) || ShowManager.server == null
                    || !Studio.isStudioBell(ShowManager.server, e.getPos())) {
                return;
            }
            e.setCanceled(true);
            ShowManager.act(sp, "buzz", "", 0);
        }

        @SubscribeEvent
        public static void chat(ServerChatEvent e) {
            ServerPlayer sp = e.getPlayer();
            if (ShowManager.onChat(sp, e.getMessage().getString())) {
                e.setCanceled(true);
            }
        }

        @SubscribeEvent
        public static void login(PlayerEvent.PlayerLoggedInEvent e) {
            if (e.getEntity() instanceof ServerPlayer sp) {
                ShowManager.onLogin(sp);
            }
        }

        @SubscribeEvent
        public static void logout(PlayerEvent.PlayerLoggedOutEvent e) {
            if (e.getEntity() instanceof ServerPlayer sp) {
                ShowManager.onLogout(sp);
            }
        }
    }
}
