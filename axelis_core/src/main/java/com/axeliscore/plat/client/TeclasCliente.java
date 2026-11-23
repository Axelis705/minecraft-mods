package com.axeliscore.plat.client;

import com.axeliscore.AxelisCore;
import com.axeliscore.ui.Teclas;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.settings.KeyModifier;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;

/** (Cliente) Crea y registra las teclas de {@link Teclas} y hace su acción al pulsarlas. */
@Mod.EventBusSubscriber(modid = AxelisCore.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class TeclasCliente {
    //#if MC >= 26.1
    //$$ private static final Map<String, KeyMapping.Category> CATEGORIAS = new HashMap<>();
    //#endif

    /** Las letras A-Z con los números de tecla de esta versión (desde 26.3 Minecraft usa otros). */
    private static final int[] LETRAS = {InputConstants.KEY_A, InputConstants.KEY_B, InputConstants.KEY_C, InputConstants.KEY_D,
            InputConstants.KEY_E, InputConstants.KEY_F, InputConstants.KEY_G, InputConstants.KEY_H, InputConstants.KEY_I, InputConstants.KEY_J,
            InputConstants.KEY_K, InputConstants.KEY_L, InputConstants.KEY_M, InputConstants.KEY_N, InputConstants.KEY_O, InputConstants.KEY_P,
            InputConstants.KEY_Q, InputConstants.KEY_R, InputConstants.KEY_S, InputConstants.KEY_T, InputConstants.KEY_U, InputConstants.KEY_V,
            InputConstants.KEY_W, InputConstants.KEY_X, InputConstants.KEY_Y, InputConstants.KEY_Z};
    private static final int[] NUMEROS = {InputConstants.KEY_0, InputConstants.KEY_1, InputConstants.KEY_2, InputConstants.KEY_3,
            InputConstants.KEY_4, InputConstants.KEY_5, InputConstants.KEY_6, InputConstants.KEY_7, InputConstants.KEY_8, InputConstants.KEY_9};
    private static final int[] FUNCIONES = {InputConstants.KEY_F1, InputConstants.KEY_F2, InputConstants.KEY_F3, InputConstants.KEY_F4,
            InputConstants.KEY_F5, InputConstants.KEY_F6, InputConstants.KEY_F7, InputConstants.KEY_F8, InputConstants.KEY_F9,
            InputConstants.KEY_F10, InputConstants.KEY_F11, InputConstants.KEY_F12};

    private TeclasCliente() {
    }

    /** Número de GLFW → número de tecla de esta versión. */
    static int codigo(int glfw) {
        if (glfw >= 65 && glfw <= 90) {
            return LETRAS[glfw - 65];
        }
        if (glfw >= 48 && glfw <= 57) {
            return NUMEROS[glfw - 48];
        }
        if (glfw >= 290 && glfw <= 301) {
            return FUNCIONES[glfw - 290];
        }
        //#if MC >= 26.3
        //$$ return InputConstants.UNKNOWN.getValue();
        //#else
        return glfw < 0 ? InputConstants.UNKNOWN.getValue() : glfw;
        //#endif
    }

    @SubscribeEvent
    public static void onKeys(net.minecraftforge.client.event.RegisterKeyMappingsEvent e) {
        for (Teclas.Tecla t : Teclas.TODAS) {
            if (t.mapping != null) {
                continue;
            }
            String nombre = "key." + t.modid + "." + t.nombre;
            //#if MC >= 26.1
            //$$ KeyMapping.Category cat = CATEGORIAS.computeIfAbsent(t.modid, m -> KeyMapping.Category.register(com.axeliscore.plat.Ids.rl(m, "main")));
            //$$ KeyMapping k = new Segura(nombre, codigo(t.codigo), cat);
            //#else
            KeyMapping k = new Segura(nombre, codigo(t.codigo), "key.categories." + t.modid);
            //#endif
            t.mapping = k;
            e.register(k);
        }
    }

    /** (Cada cuadro) Hace la acción de las teclas pulsadas. */
    public static void revisar() {
        Minecraft mc = Minecraft.getInstance();
        for (Teclas.Tecla t : Teclas.TODAS) {
            if (!(t.mapping instanceof KeyMapping k)) {
                continue;
            }
            while (k.consumeClick()) {
                if (mc.player != null && mc.screen == null && t.alPulsar != null) {
                    try {
                        t.alPulsar.run();
                    } catch (RuntimeException ex) {
                        AxelisCore.LOGGER.error("Axelis Core: falló la tecla {}", t.nombre, ex);
                    }
                }
            }
        }
    }

    /** Tecla que no acepta códigos inválidos (en 26.3 una tecla guardada como -1 hacía caer el juego). */
    static final class Segura extends KeyMapping {
        //#if MC >= 26.1
        //$$ Segura(String name, int code, KeyMapping.Category cat) {
        //$$     super(name, code, cat);
        //$$ }
        //#else
        Segura(String name, int code, String cat) {
            super(name, code, cat);
        }
        //#endif

        static InputConstants.Key limpia(InputConstants.Key k) {
            return k == null || (k.getValue() < 0 && !k.equals(InputConstants.UNKNOWN)) ? InputConstants.UNKNOWN : k;
        }

        @Override
        public void setKey(InputConstants.Key k) {
            super.setKey(limpia(k));
        }

        @Override
        public void setKeyModifierAndCode(KeyModifier m, InputConstants.Key k) {
            super.setKeyModifierAndCode(m, limpia(k));
        }
    }
}
