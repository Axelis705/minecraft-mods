package com.showdeaxelis.registry;

import com.showdeaxelis.ShowMod;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** Los sonidos de programa de television. Cada nombre es un archivo en assets/show_de_axelis/sounds/. */
public final class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, ShowMod.MOD_ID);
    private static final Map<String, RegistryObject<SoundEvent>> ALL = new LinkedHashMap<>();

    public static final String[] NAMES = {
        "intro", "applause", "applause_big", "cheer", "boo", "laugh", "correct", "reveal", "strike", "buzzer", "tick", "time_up",
        "drumroll", "fanfare", "trombone", "glitch", "blip", "whoosh", "pop", "whistle_up", "whistle_down", "rimshot",
        "suspense", "round_start", "vote", "ding", "crickets", "airhorn"
    };

    static {
        for (String n : NAMES) {
            ALL.put(n, SOUNDS.register(n, () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(ShowMod.MOD_ID, n))));
        }
    }

    private ModSounds() {
    }

    public static SoundEvent get(String name) {
        RegistryObject<SoundEvent> o = ALL.get(name);
        return o == null ? null : o.get();
    }
}
