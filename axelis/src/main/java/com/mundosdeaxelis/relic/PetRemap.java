package com.mundosdeaxelis.relic;

import com.mundosdeaxelis.MundosDeAxelis;
import com.mundosdeaxelis.registry.ModItems;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.MissingMappingsEvent;
import net.minecraftforge.registries.RegistryObject;

import java.util.Map;

/**
 * Etapa 252: siete figuritas se quitaron del mod. Para que nadie pierda la suya, en los mundos viejos cada una se convierte
 * sola en la mascota nueva que ocupa su lugar (misma rareza y la misma habilidad base, rehecha).
 */
@Mod.EventBusSubscriber(modid = MundosDeAxelis.MODID)
public final class PetRemap {
    private PetRemap() {
    }

    private static final Map<String, RegistryObject<Item>> OLD_TO_NEW = Map.of(
            "comp_fox", ModItems.COMP_LAND_SHARK,
            "comp_jelly", ModItems.COMP_CLOUD,
            "comp_phoenix", ModItems.COMP_COMET_CAT,
            "comp_panda", ModItems.COMP_ASTRO_OCTOPUS,
            "comp_hedgehog", ModItems.COMP_TOASTER,
            "comp_raccoon", ModItems.COMP_VAMPIRE_BAT,
            "comp_capybara", ModItems.COMP_ZEN_CAPYBARA);

    @SubscribeEvent
    public static void onMissing(MissingMappingsEvent event) {
        for (MissingMappingsEvent.Mapping<Item> m : event.getMappings(ForgeRegistries.Keys.ITEMS, MundosDeAxelis.MODID)) {
            RegistryObject<Item> to = OLD_TO_NEW.get(m.getKey().getPath());
            if (to != null && to.isPresent()) {
                m.remap(to.get());
            }
        }
    }
}
