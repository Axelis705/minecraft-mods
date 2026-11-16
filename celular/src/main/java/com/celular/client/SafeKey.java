package com.celular.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.settings.KeyModifier;

/**
 * Tecla del celular que no acepta códigos inválidos. En 26.3 Minecraft pasó a otro sistema de teclado (SDL): "sin asignar" ya no es -1,
 * y una tecla guardada como "key.keyboard.-1" hacía caer el juego al entrar al mundo. Si llega un código negativo, queda sin asignar.
 */
final class SafeKey extends KeyMapping {
    //#if MC >= 26.1
    //$$ SafeKey(String name, int code, KeyMapping.Category cat) {
    //$$     super(name, code, cat);
    //$$ }
    //#else
    SafeKey(String name, int code, String cat) {
        super(name, code, cat);
    }
    //#endif

    static InputConstants.Key clean(InputConstants.Key k) {
        return k == null || (k.getValue() < 0 && !k.equals(InputConstants.UNKNOWN)) ? InputConstants.UNKNOWN : k;
    }

    @Override
    public void setKey(InputConstants.Key k) {
        super.setKey(clean(k));
    }

    @Override
    public void setKeyModifierAndCode(KeyModifier m, InputConstants.Key k) {
        super.setKeyModifierAndCode(m, clean(k));
    }
}
