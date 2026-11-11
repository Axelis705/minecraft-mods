package com.autoscoleccion.client;

import com.autoscoleccion.registry.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvent;

/** Sonidos de las pantallas (se oyen solo para el jugador). */
public final class Sfx {
    private Sfx() {
    }

    public static void play(String name, float pitch, float volume) {
        SoundEvent e = ModSounds.get(name);
        if (e != null) {
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(e, pitch, volume));
        }
    }

    public static void play(String name) {
        play(name, 1.0F, 0.8F);
    }
}
