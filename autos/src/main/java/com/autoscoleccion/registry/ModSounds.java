package com.autoscoleccion.registry;

import com.autoscoleccion.AutosMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.LinkedHashMap;
import java.util.Map;

/** Sonidos propios (generados con herramientas/gen_sonidos.py). Cada nombre es un .ogg en assets/autos_coleccion/sounds/. */
public final class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, AutosMod.MODID);
    private static final Map<String, RegistryObject<SoundEvent>> ALL = new LinkedHashMap<>();

    public static final String[] NAMES = {
        "coin", "machine_start", "count", "go", "perfect", "good", "miss", "rev", "shift", "nitro", "note", "key", "error",
        "tally", "result", "box_drop", "box_shake", "tape", "flaps", "reveal_common", "reveal_rare", "reveal_epic", "reveal_legend",
        "sparkle", "place", "garage", "whoosh", "new_car", "engine", "bump", "skid"
    };

    static {
        for (String n : NAMES) {
            ALL.put(n, SOUNDS.register(n, () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(AutosMod.MODID, n))));
        }
    }

    private ModSounds() {
    }

    public static SoundEvent get(String name) {
        RegistryObject<SoundEvent> o = ALL.get(name);
        return o == null ? null : o.get();
    }
}
