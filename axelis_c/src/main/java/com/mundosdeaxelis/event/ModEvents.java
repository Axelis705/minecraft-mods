package com.mundosdeaxelis.event;

import com.mundosdeaxelis.MundosDeAxelis;
import com.mundosdeaxelis.registry.ModEntities;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.event.entity.SpawnPlacementRegisterEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Eventos comunes en el bus del mod. */
@Mod.EventBusSubscriber(modid = MundosDeAxelis.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ModEvents {
    private ModEvents() {
    }

    /** El Brote puede aparecer solo por el mundo (de noche y en la oscuridad, como los demás monstruos) cuando el mundo ya subió de nivel. */
    @SubscribeEvent
    public static void registerSpawns(SpawnPlacementRegisterEvent event) {
        event.register(ModEntities.BROTE.get(), net.minecraft.world.entity.SpawnPlacements.Type.ON_GROUND,
                net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                net.minecraft.world.entity.monster.Monster::checkMonsterSpawnRules, SpawnPlacementRegisterEvent.Operation.AND);
    }

    @SubscribeEvent
    public static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(ModEntities.BROTE.get(), com.mundosdeaxelis.entity.Brote.createAttributes().build());
        event.put(ModEntities.RAIZAL.get(), com.mundosdeaxelis.entity.Raizal.createAttributes().build());
        event.put(ModEntities.POLINIZADOR.get(), com.mundosdeaxelis.entity.Polinizador.createAttributes().build());
        event.put(ModEntities.ESPINOSO.get(), com.mundosdeaxelis.entity.Espinoso.createAttributes().build());
        event.put(ModEntities.GUARDIAN_MUSGO.get(), com.mundosdeaxelis.entity.GuardianMusgo.createAttributes().build());
        event.put(ModEntities.REY_BROTE.get(), com.mundosdeaxelis.entity.ReyBrote.createAttributes().build());
        event.put(ModEntities.ESCORIA.get(), com.mundosdeaxelis.entity.Escoria.createAttributes().build());
        event.put(ModEntities.CHISPERO.get(), com.mundosdeaxelis.entity.Chispero.createAttributes().build());
        event.put(ModEntities.ASCUA.get(), com.mundosdeaxelis.entity.Ascua.createAttributes().build());
        event.put(ModEntities.FUNDIDOR.get(), com.mundosdeaxelis.entity.Fundidor.createAttributes().build());
        event.put(ModEntities.CAPATAZ.get(), com.mundosdeaxelis.entity.Capataz.createAttributes().build());
        event.put(ModEntities.GRAN_HERRERO.get(), com.mundosdeaxelis.entity.GranHerrero.createAttributes().build());
        event.put(ModEntities.LOBO_ARTICO.get(), com.mundosdeaxelis.entity.LoboArtico.createAttributes().build());
        event.put(ModEntities.LOBO_ESCARCHA.get(), com.mundosdeaxelis.entity.LoboEscarcha.createAttributes().build());
    }
}
