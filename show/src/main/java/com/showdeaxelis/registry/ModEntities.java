package com.showdeaxelis.registry;

import com.showdeaxelis.ShowMod;
import com.showdeaxelis.entity.SpectatorEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, ShowMod.MOD_ID);

    /** El publico del estudio: figuras que aplauden, abuchean y se rien. */
    public static final RegistryObject<EntityType<SpectatorEntity>> SPECTATOR = ENTITIES.register("spectator",
            () -> EntityType.Builder.<SpectatorEntity>of((t, l) -> new SpectatorEntity(t, l), MobCategory.MISC).sized(0.6f, 1.8f).clientTrackingRange(10).updateInterval(3).build("spectator"));

    private ModEntities() {
    }
}
