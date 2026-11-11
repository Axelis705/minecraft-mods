package com.autoscoleccion;

import com.autoscoleccion.net.AutosNet;
import com.autoscoleccion.registry.ModBlockEntities;
import com.autoscoleccion.registry.ModBlocks;
import com.autoscoleccion.registry.ModEntities;
import com.autoscoleccion.registry.ModItems;
import com.autoscoleccion.registry.ModSounds;
import com.autoscoleccion.registry.ModTabs;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

/**
 * Autos de Colección: una máquina expendedora que pide una moneda de oro y una prueba de habilidad, y da cajas de cochecitos de
 * colección (Ruta Urbana, Pista Nocturna, Leyendas del Circuito). Los autos se abren con animación, se guardan en el garaje y se
 * exhiben en vitrinas.
 */
@Mod(AutosMod.MODID)
public final class AutosMod {
    public static final String MODID = "autos_coleccion";

    public AutosMod() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        ModBlocks.BLOCKS.register(bus);
        ModItems.ITEMS.register(bus);
        ModBlockEntities.BLOCK_ENTITIES.register(bus);
        ModEntities.ENTITIES.register(bus);
        ModSounds.SOUNDS.register(bus);
        ModTabs.TABS.register(bus);
        AutosNet.register();
    }
}
