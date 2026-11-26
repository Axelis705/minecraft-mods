package com.navidad.registry;

import com.navidad.Navidad;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
//#if MC >= 26.1
//$$ import net.minecraftforge.eventbus.api.bus.BusGroup;
//#else
import net.minecraftforge.eventbus.api.IEventBus;
//#endif
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Registros del mod. Para crear cosas usa las ayudas del Core, que ya ponen el id como pide cada versión:
 * <pre>
 *     public static final RegistryObject&lt;Item&gt; ANZUELO = ITEMS.register("anzuelo", () -> new Item(Reg.item(MODID, "anzuelo")));
 * </pre>
 */
public final class ModRegistry {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, Navidad.MODID);
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, Navidad.MODID);
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, Navidad.MODID);
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, Navidad.MODID);

    private ModRegistry() {
    }

    //#if MC >= 26.1
    //$$ public static void register(BusGroup bus) {
    //#else
    public static void register(IEventBus bus) {
    //#endif
        ITEMS.register(bus);
        BLOCKS.register(bus);
        ENTITIES.register(bus);
        SOUNDS.register(bus);
    }
}
