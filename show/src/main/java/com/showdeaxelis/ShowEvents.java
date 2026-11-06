package com.showdeaxelis;

import com.showdeaxelis.data.Banks;
import com.showdeaxelis.data.ShowManager;
import com.showdeaxelis.entity.SpectatorEntity;
import com.showdeaxelis.registry.ModEntities;
import net.minecraft.server.level.ServerPlayer;
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
