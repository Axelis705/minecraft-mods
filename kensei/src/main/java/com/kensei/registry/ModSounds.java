package com.kensei.registry;

import com.kensei.Kensei;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** Sonidos propios (sintetizados). Los archivos están en assets/kensei/sounds y se enlazan en sounds.json. */
public final class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, Kensei.MODID);

    public static final RegistryObject<SoundEvent> SLASH = reg("slash");
    public static final RegistryObject<SoundEvent> HIT = reg("hit");
    public static final RegistryObject<SoundEvent> PARRY = reg("parry");
    public static final RegistryObject<SoundEvent> DODGE = reg("dodge");
    public static final RegistryObject<SoundEvent> HEAVY_CHARGE = reg("heavy_charge");
    public static final RegistryObject<SoundEvent> HEAVY_HIT = reg("heavy_hit");
    public static final RegistryObject<SoundEvent> GUARD_BREAK = reg("guard_break");
    public static final RegistryObject<SoundEvent> DRAW = reg("draw");

    private ModSounds() {
    }

    private static RegistryObject<SoundEvent> reg(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(Kensei.MODID, name)));
    }
}
