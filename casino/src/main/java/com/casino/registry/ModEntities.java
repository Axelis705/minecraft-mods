package com.casino.registry;

import com.casino.plat.Reg;
import com.casino.Casino;
import com.casino.entity.DealerEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, Casino.MODID);

    public static final RegistryObject<EntityType<DealerEntity>> DEALER = ENTITIES.register("dealer",
            () -> Reg.entity("dealer", EntityType.Builder.of(DealerEntity::new, MobCategory.MISC).sized(0.6F, 1.8F).clientTrackingRange(8)));

    private ModEntities() {
    }
}
