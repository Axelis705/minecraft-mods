package com.mundosdeaxelis.client;

import com.mundosdeaxelis.registry.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvent;

/** Sonidos de interfaz del mod (apertura, pasar el cursor, clic, pestañas, éxito, error, forja). Solo cliente. */
public final class UiFx {
    private UiFx() {
    }

    private static void play(SoundEvent e, float pitch, float vol) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.getSoundManager() != null) {
            mc.getSoundManager().play(SimpleSoundInstance.forUI(e, pitch, vol));
        }
    }

    public static void open() {
        play(ModSounds.UI_OPEN.get(), 1.0F, 0.8F);
    }

    public static void hover() {
        play(ModSounds.UI_HOVER.get(), 0.95F + (float) Math.random() * 0.1F, 0.6F);
    }

    public static void click() {
        play(ModSounds.UI_CLICK.get(), 1.0F, 0.9F);
    }

    public static void tab() {
        play(ModSounds.UI_TAB.get(), 1.0F, 0.9F);
    }

    public static void success() {
        play(ModSounds.UI_SUCCESS.get(), 1.0F, 0.8F);
    }

    public static void error() {
        play(ModSounds.UI_ERROR.get(), 1.0F, 0.8F);
    }

    public static void forge() {
        play(ModSounds.UI_FORGE.get(), 1.0F, 0.9F);
    }

    /** Un sonido de la cerradura (clavija, fallo, giro, apertura). */
    public static void lock(net.minecraftforge.registries.RegistryObject<SoundEvent> e, float pitch, float vol) {
        play(e.get(), pitch, vol);
    }

    /** Un cuadro de tiempo en segundos, para las animaciones de las pantallas. */
    public static double time() {
        return net.minecraft.Util.getMillis() / 1000.0D;
    }
}
