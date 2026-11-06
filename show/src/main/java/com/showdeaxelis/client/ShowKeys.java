package com.showdeaxelis.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.showdeaxelis.ShowMod;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

/** Teclas del show (se pueden cambiar en Controles) y registros del cliente. */
public final class ShowKeys {
    private static final String CAT = "key.categories.show_de_axelis";
    public static final KeyMapping PANEL = new KeyMapping("key.show_de_axelis.panel", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_Y, CAT);
    public static final KeyMapping BUZZ = new KeyMapping("key.show_de_axelis.buzz", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_G, CAT);
    public static final KeyMapping TV = new KeyMapping("key.show_de_axelis.tv", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_H, CAT);
    public static final KeyMapping DRAW = new KeyMapping("key.show_de_axelis.draw", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_J, CAT);
    public static final KeyMapping VOTE = new KeyMapping("key.show_de_axelis.vote", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_N, CAT);
    public static final KeyMapping HUD = new KeyMapping("key.show_de_axelis.hud", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_K, CAT);
    public static final KeyMapping[] ANS = {
        new KeyMapping("key.show_de_axelis.ans_a", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_Z, CAT),
        new KeyMapping("key.show_de_axelis.ans_b", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_X, CAT),
        new KeyMapping("key.show_de_axelis.ans_c", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_C, CAT),
        new KeyMapping("key.show_de_axelis.ans_d", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_V, CAT)
    };

    private ShowKeys() {
    }

    @Mod.EventBusSubscriber(modid = ShowMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class ModBus {
        @SubscribeEvent
        public static void keys(RegisterKeyMappingsEvent e) {
            e.register(PANEL);
            e.register(BUZZ);
            e.register(TV);
            e.register(DRAW);
            e.register(VOTE);
            e.register(HUD);
            for (KeyMapping k : ANS) {
                e.register(k);
            }
        }

        @SubscribeEvent
        public static void overlays(RegisterGuiOverlaysEvent e) {
            e.registerAboveAll("show", ShowHud::render);
        }

        @SubscribeEvent
        public static void renderers(EntityRenderersEvent.RegisterRenderers e) {
            e.registerEntityRenderer(com.showdeaxelis.registry.ModEntities.SPECTATOR.get(), SpectatorRenderer::new);
        }
    }

    @Mod.EventBusSubscriber(modid = ShowMod.MOD_ID, value = Dist.CLIENT)
    public static final class ForgeBus {
        @SubscribeEvent
        public static void tick(TickEvent.ClientTickEvent e) {
            if (e.phase != TickEvent.Phase.END) {
                return;
            }
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null || mc.screen != null) {
                return;
            }
            while (PANEL.consumeClick()) {
                HostScreen.open();
            }
            while (TV.consumeClick()) {
                TvCamera.toggle();
            }
            while (DRAW.consumeClick()) {
                if (ShowClient.active() && ShowClient.state.getString("phase").equals("DRAW")
                        && ShowClient.state.getCompound("dr").getBoolean("iDraw")) {
                    DrawScreen.open();
                }
            }
            while (VOTE.consumeClick()) {
                CompoundTag im = ShowClient.state.getCompound("imp");
                if (ShowClient.active() && ShowClient.state.getString("phase").equals("IMPOSTOR") && im.getString("stage").equals("VOTE")
                        && ShowClient.state.getCompound("you").getBoolean("player") && !im.getBoolean("iVoted")) {
                    VoteScreen.open();
                }
            }
            while (HUD.consumeClick()) {
                ShowClient.hudMode = (ShowClient.hudMode + 1) % 3;
                mc.player.displayClientMessage(Component.literal("Interfaz del show: " + new String[] {"automatica", "compacta", "completa"}[ShowClient.hudMode]), true);
            }
            while (BUZZ.consumeClick()) {
                ShowClient.send("buzz");
            }
            for (int i = 0; i < ANS.length; i++) {
                while (ANS[i].consumeClick()) {
                    ShowClient.send("ans", "", i);
                }
            }
        }
    }
}
