package com.autoscoleccion.registry;

import com.autoscoleccion.AutosMod;
import com.autoscoleccion.entity.RcCarEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, AutosMod.MODID);

    public static final RegistryObject<EntityType<RcCarEntity>> RC_CAR = ENTITIES.register("auto_rc",
            () -> EntityType.Builder.<RcCarEntity>of(RcCarEntity::new, MobCategory.MISC).sized(0.6F, 0.35F).clientTrackingRange(8).updateInterval(1)
                    .build("auto_rc"));

    private ModEntities() {
    }
}
