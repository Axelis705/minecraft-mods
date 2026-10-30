package com.kensei.registry;

import com.kensei.Kensei;
import com.kensei.entity.RivalEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** Las criaturas de Kensei: por ahora solo el rival de práctica. */
public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, Kensei.MODID);

    public static final RegistryObject<EntityType<RivalEntity>> RIVAL = ENTITIES.register("rival",
            () -> EntityType.Builder.<RivalEntity>of(RivalEntity::new, MobCategory.MISC).sized(0.6F, 1.8F).clientTrackingRange(10).build("rival"));

    private ModEntities() {
    }
}
