package com.pescacoleccion.registry;

import com.pescacoleccion.PescaColeccion;
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
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, PescaColeccion.MODID);
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, PescaColeccion.MODID);
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, PescaColeccion.MODID);
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, PescaColeccion.MODID);

    public static final net.minecraftforge.registries.RegistryObject<EntityType<com.pescacoleccion.pesca.Anzuelo>> ANZUELO = ENTITIES.register("anzuelo",
            () -> com.axeliscore.plat.Reg.entity(PescaColeccion.MODID, "anzuelo", EntityType.Builder.<com.pescacoleccion.pesca.Anzuelo>of(
                    com.pescacoleccion.pesca.Anzuelo::new, net.minecraft.world.entity.MobCategory.MISC).sized(0.25F, 0.25F)
                    .clientTrackingRange(6).updateInterval(1)));

    /** Las cañas (una por tipo de {@link com.pescacoleccion.pesca.Cana}). */
    public static final java.util.Map<com.pescacoleccion.pesca.Cana, net.minecraftforge.registries.RegistryObject<Item>> CANAS = new java.util.EnumMap<>(
            com.pescacoleccion.pesca.Cana.class);

    static {
        for (com.pescacoleccion.pesca.Cana c : com.pescacoleccion.pesca.Cana.values()) {
            CANAS.put(c, ITEMS.register(c.id, () -> new com.pescacoleccion.pesca.CanaItem(
                    com.axeliscore.plat.Reg.item(PescaColeccion.MODID, c.id).stacksTo(1), c)));
        }
    }

    public static final DeferredRegister<net.minecraft.world.item.CreativeModeTab> TABS = DeferredRegister.create(
            net.minecraft.core.registries.Registries.CREATIVE_MODE_TAB, PescaColeccion.MODID);
    public static final net.minecraftforge.registries.RegistryObject<net.minecraft.world.item.CreativeModeTab> TAB = TABS.register("main",
            () -> net.minecraft.world.item.CreativeModeTab.builder()
                    .title(net.minecraft.network.chat.Component.translatable("itemGroup." + PescaColeccion.MODID))
                    .icon(() -> new net.minecraft.world.item.ItemStack(CANAS.get(com.pescacoleccion.pesca.Cana.BAMBU).get()))
                    .displayItems((params, out) -> {
                        for (var c : CANAS.values()) {
                            out.accept(c.get());
                        }
                    }).build());

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
        TABS.register(bus);
    }
}
