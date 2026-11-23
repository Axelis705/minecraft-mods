package com.axeliscore.registry;

import com.axeliscore.AxelisCore;
import com.axeliscore.character.Persona;
import com.axeliscore.plat.Reg;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** Los personajes listos del Core (se pueden invocar con /summon axelis_core:persona o con /axeliscore galeria). */
@Mod.EventBusSubscriber(modid = AxelisCore.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class CoreEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, AxelisCore.MODID);

    public static final RegistryObject<EntityType<Persona>> PERSONA = ENTITIES.register("persona",
            () -> Reg.entity(AxelisCore.MODID, "persona", EntityType.Builder.<Persona>of(Persona::new, MobCategory.CREATURE).sized(0.6F, 1.8F)
                    .clientTrackingRange(10)));

    private CoreEntities() {
    }

    @SubscribeEvent
    public static void onAttributes(EntityAttributeCreationEvent e) {
        e.put(PERSONA.get(), Persona.createAttributes().build());
    }
}
