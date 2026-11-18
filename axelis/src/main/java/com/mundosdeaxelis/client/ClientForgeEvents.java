package com.mundosdeaxelis.client;

import com.mundosdeaxelis.MundosDeAxelis;
import com.mundosdeaxelis.network.ModNetwork;
import com.mundosdeaxelis.network.SkipCinematicPacket;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.ComputeFovModifierEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Eventos del cliente en el bus de juego (ticks). */
@Mod.EventBusSubscriber(modid = MundosDeAxelis.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class ClientForgeEvents {
    private ClientForgeEvents() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            FadeOverlay.tick();
            ColdHud.tick();
            BossTitleOverlay.tick();
            CinematicClient.clientTick();
            while (ClientModEvents.SKIP.consumeClick()) {
                if (CinematicClient.isActive()) {
                    ModNetwork.CHANNEL.sendToServer(new SkipCinematicPacket());
                }
            }
        }
    }

    @SubscribeEvent
    public static void onCamera(ViewportEvent.ComputeCameraAngles event) {
        float s = ScreenFx.shake();
        if (s > 0.01F) {
            double t = net.minecraft.Util.getMillis() / 50.0;
            event.setRoll(event.getRoll() + (float) Math.sin(t * 2.3) * s * 1.4F);
            event.setPitch(event.getPitch() + (float) Math.sin(t * 3.1 + 1.0) * s);
            event.setYaw(event.getYaw() + (float) Math.cos(t * 2.7 + 2.0) * s);
        }
    }

    /** El Catalejo Astral acerca la vista mientras se mantiene el clic derecho. */
    @SubscribeEvent
    public static void onFov(ComputeFovModifierEvent event) {
        net.minecraft.world.entity.player.Player p = event.getPlayer();
        if (p.isUsingItem() && p.getUseItem().getItem() instanceof com.mundosdeaxelis.relic.AstralSpyglassItem) {
            float t = Math.min(1.0F, p.getTicksUsingItem() / 10.0F);
            event.setNewFovModifier(event.getNewFovModifier() * (1.0F - 0.78F * t * t * (3.0F - 2.0F * t)));
        }
    }

    private static long lastChatSound = -100;

    /** Cada mensaje del chat suena con un "ding" suave (más agudo si es alguien hablando: "<Nombre> ..."). */
    @SubscribeEvent
    public static void onChat(ClientChatReceivedEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }
        long now = mc.level.getGameTime();
        if (now - lastChatSound < 6) {
            return; // varios renglones seguidos suenan una sola vez
        }
        lastChatSound = now;
        boolean speech = event.getMessage().getString().startsWith("<");
        mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_CHIME.get(), speech ? 1.8F : 1.4F, 0.6F));
    }
}
