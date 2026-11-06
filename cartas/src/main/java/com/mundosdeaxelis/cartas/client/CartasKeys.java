package com.mundosdeaxelis.cartas.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.mundosdeaxelis.cartas.Cartas;
import com.mundosdeaxelis.cartas.CartasNet;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

/** Teclas del album: R guarda las cartas nuevas, B abre el album (se pueden cambiar en Controles). */
public final class CartasKeys {
    public static final KeyMapping INSERT = new KeyMapping("key.mundos_de_axelis.cartas.insertar", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_R, "key.categories.mundos_de_axelis_cartas");
    public static final KeyMapping OPEN = new KeyMapping("key.mundos_de_axelis.cartas.abrir", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_B, "key.categories.mundos_de_axelis_cartas");

    private CartasKeys() {
    }

    @Mod.EventBusSubscriber(modid = Cartas.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class ModBus {
        @SubscribeEvent
        public static void keys(RegisterKeyMappingsEvent e) {
            e.register(INSERT);
            e.register(OPEN);
        }
    }

    @Mod.EventBusSubscriber(modid = Cartas.MOD_ID, value = Dist.CLIENT)
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
            while (INSERT.consumeClick()) {
                CartasNet.CHANNEL.sendToServer(new CartasNet.InsertRequest());
            }
            while (OPEN.consumeClick()) {
                CartasNet.CHANNEL.sendToServer(new CartasNet.OpenRequest());
            }
        }
    }
}
