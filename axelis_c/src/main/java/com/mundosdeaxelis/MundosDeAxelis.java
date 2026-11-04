package com.mundosdeaxelis;

import com.mojang.logging.LogUtils;
import com.mundosdeaxelis.network.ModNetwork;
import com.mundosdeaxelis.registry.ModBlockEntities;
import com.mundosdeaxelis.registry.ModBlocks;
import com.mundosdeaxelis.registry.ModCreativeTabs;
import com.mundosdeaxelis.registry.ModEntities;
import com.mundosdeaxelis.registry.ModItems;
import com.mundosdeaxelis.registry.ModSounds;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

/**
 * Clase principal del mod "Mundos de Axelis".
 * El valor de MODID debe coincidir con mod_id en gradle.properties.
 */
@Mod(MundosDeAxelis.MODID)
public class MundosDeAxelis {
    public static final String MODID = "mundos_de_axelis";
    public static final Logger LOGGER = LogUtils.getLogger();

    public MundosDeAxelis(FMLJavaModLoadingContext context) {
        IEventBus modBus = context.getModEventBus();

        ModBlocks.BLOCKS.register(modBus);
        ModItems.ITEMS.register(modBus);
        ModEntities.ENTITIES.register(modBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modBus);
        ModCreativeTabs.TABS.register(modBus);
        ModSounds.SOUNDS.register(modBus);
        com.mundosdeaxelis.registry.ModMenus.MENUS.register(modBus);
        com.mundosdeaxelis.particle.ModParticles.PARTICLES.register(modBus);

        context.registerConfig(net.minecraftforge.fml.config.ModConfig.Type.CLIENT, ClientConfig.SPEC);

        modBus.addListener(this::commonSetup);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(ModNetwork::register);
        event.enqueueWork(com.mundosdeaxelis.gear.ArmorCap::raise);
        LOGGER.info("Mundos de Axelis: listo.");
    }
}
