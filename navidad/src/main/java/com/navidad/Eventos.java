package com.navidad;

import com.navidad.entidad.HombreJengibre;
import com.navidad.entidad.Krampus;
import com.navidad.entidad.MunecoMalvado;
import com.navidad.entidad.Reno;
import com.navidad.registry.ModRegistry;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.event.entity.SpawnPlacementRegisterEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Eventos del bus del mod: atributos de las criaturas y dónde pueden aparecer solas. */
@Mod.EventBusSubscriber(modid = Navidad.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class Eventos {
    private Eventos() {
    }

    @SubscribeEvent
    public static void atributos(EntityAttributeCreationEvent e) {
        e.put(ModRegistry.MUNECO_MALVADO.get(), MunecoMalvado.createAttributes().build());
        e.put(ModRegistry.RENO.get(), Reno.createAttributes().build());
        e.put(ModRegistry.HOMBRE_JENGIBRE.get(), HombreJengibre.createAttributes().build());
        e.put(ModRegistry.KRAMPUS.get(), Krampus.createAttributes().build());
        e.put(ModRegistry.PINGUINO.get(), com.navidad.entidad.Pinguino.createAttributes().build());
        e.put(ModRegistry.CASCANUECES.get(), com.navidad.entidad.Cascanueces.createAttributes().build());
        e.put(ModRegistry.RENITO.get(), com.navidad.entidad.Renito.createAttributes().build());
        e.put(ModRegistry.DUENDE_LADRON.get(), com.navidad.entidad.DuendeLadron.createAttributes().build());
        e.put(ModRegistry.KRAMPUSILLO.get(), com.navidad.entidad.Krampusillo.createAttributes().build());
    }

    @SubscribeEvent
    public static void apariciones(SpawnPlacementRegisterEvent e) {
        e.register(ModRegistry.MUNECO_MALVADO.get(), SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                MunecoMalvado::puedeSalir, SpawnPlacementRegisterEvent.Operation.REPLACE);
        e.register(ModRegistry.RENO.get(), SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Reno::puedeSalir, SpawnPlacementRegisterEvent.Operation.REPLACE);
        e.register(ModRegistry.HOMBRE_JENGIBRE.get(), SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                HombreJengibre::puedeSalir, SpawnPlacementRegisterEvent.Operation.REPLACE);
        e.register(ModRegistry.PINGUINO.get(), SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                com.navidad.entidad.Pinguino::puedeSalir, SpawnPlacementRegisterEvent.Operation.REPLACE);
        e.register(ModRegistry.RENITO.get(), SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                com.navidad.entidad.Renito::puedeSalir, SpawnPlacementRegisterEvent.Operation.REPLACE);
    }
}
