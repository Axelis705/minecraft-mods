package com.kensei;

import com.kensei.net.KenseiNet;
import com.kensei.registry.ModCreativeTabs;
import com.kensei.registry.ModEntities;
import com.kensei.registry.ModItems;
import com.kensei.registry.ModParticles;
import com.kensei.registry.ModSounds;
import com.mojang.logging.LogUtils;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

/**
 * Kensei: combate con katanas. Esquive con barras de energía, parry perfecto con contraataque, combos y combate aéreo.
 * El valor de MODID debe coincidir con mod_id en gradle.properties.
 */
@Mod(Kensei.MODID)
public class Kensei {
    public static final String MODID = "kensei";
    public static final Logger LOGGER = LogUtils.getLogger();

    public Kensei(FMLJavaModLoadingContext context) {
        IEventBus modBus = context.getModEventBus();
        ModEntities.ENTITIES.register(modBus);
        ModItems.ITEMS.register(modBus);
        ModCreativeTabs.TABS.register(modBus);
        ModParticles.PARTICLES.register(modBus);
        ModSounds.SOUNDS.register(modBus);
        modBus.addListener(this::commonSetup);
        context.registerConfig(ModConfig.Type.COMMON, KenseiConfig.SPEC);
        // la postura de brazo de la katana se crea ya, antes de que el juego dibuje nada
        DistExecutor.safeRunWhenOn(Dist.CLIENT, () -> com.kensei.client.KatanaPoses::init);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(KenseiNet::register);
        LOGGER.info("Kensei: listo.");
    }
}
