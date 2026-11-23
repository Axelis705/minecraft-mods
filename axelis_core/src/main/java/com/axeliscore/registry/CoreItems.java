package com.axeliscore.registry;

import com.axeliscore.AxelisCore;
import com.axeliscore.plat.Ids;
import com.axeliscore.plat.Reg;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** Objetos del Core: solo los de muestra (con los ejemplos activos; si no, sus campos son null). */
public final class CoreItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, AxelisCore.MODID);

    /** Muestra de objeto con modelo de Blockbench: un sombrero de paja; con clic derecho te lo pones (o te lo quitas). */
    public static final RegistryObject<Item> SOMBRERO = com.axeliscore.Ejemplos.activos() ? ITEMS.register("sombrero",
            () -> new com.axeliscore.item.SombreroDemo(Reg.item(AxelisCore.MODID, "sombrero").stacksTo(1))
                    .modelo3d(Ids.core("geo/sombrero.geo.json"), Ids.core("textures/prenda/sombrero.png"))) : null;

    private CoreItems() {
    }
}
