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
    public static final DeferredRegister<net.minecraft.world.level.block.entity.BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(
            net.minecraft.core.registries.Registries.BLOCK_ENTITY_TYPE, PescaColeccion.MODID);

    /** Placa de trofeo (un pez montado en la pared) y acuario (peces que nadan). */
    public static final net.minecraftforge.registries.RegistryObject<Block> TROFEO = BLOCKS.register("trofeo",
            () -> new com.pescacoleccion.deco.TrofeoBlock(com.axeliscore.plat.Reg.block(PescaColeccion.MODID, "trofeo").strength(1F)
                    .sound(net.minecraft.world.level.block.SoundType.WOOD).noOcclusion()));
    public static final net.minecraftforge.registries.RegistryObject<Block> ACUARIO = BLOCKS.register("acuario",
            () -> new com.pescacoleccion.deco.AcuarioBlock(com.axeliscore.plat.Reg.block(PescaColeccion.MODID, "acuario").strength(0.6F)
                    .sound(net.minecraft.world.level.block.SoundType.GLASS).noOcclusion().lightLevel(st -> 4)));
    public static final net.minecraftforge.registries.RegistryObject<Item> TROFEO_ITEM = ITEMS.register("trofeo",
            () -> new net.minecraft.world.item.BlockItem(TROFEO.get(), com.axeliscore.plat.Reg.blockItem(PescaColeccion.MODID, "trofeo")));
    public static final net.minecraftforge.registries.RegistryObject<Item> ACUARIO_ITEM = ITEMS.register("acuario",
            () -> new net.minecraft.world.item.BlockItem(ACUARIO.get(), com.axeliscore.plat.Reg.blockItem(PescaColeccion.MODID, "acuario")));
    public static final net.minecraftforge.registries.RegistryObject<net.minecraft.world.level.block.entity.BlockEntityType<com.pescacoleccion.deco.TrofeoBE>> TROFEO_BE =
            BLOCK_ENTITIES.register("trofeo", () -> com.axeliscore.plat.Reg.blockEntity(com.pescacoleccion.deco.TrofeoBE::new, TROFEO.get()));
    public static final net.minecraftforge.registries.RegistryObject<net.minecraft.world.level.block.entity.BlockEntityType<com.pescacoleccion.deco.AcuarioBE>> ACUARIO_BE =
            BLOCK_ENTITIES.register("acuario", () -> com.axeliscore.plat.Reg.blockEntity(com.pescacoleccion.deco.AcuarioBE::new, ACUARIO.get()));

    public static final net.minecraftforge.registries.RegistryObject<EntityType<com.pescacoleccion.pesca.Anzuelo>> ANZUELO = ENTITIES.register("anzuelo",
            () -> com.axeliscore.plat.Reg.entity(PescaColeccion.MODID, "anzuelo", EntityType.Builder.<com.pescacoleccion.pesca.Anzuelo>of(
                    com.pescacoleccion.pesca.Anzuelo::new, net.minecraft.world.entity.MobCategory.MISC).sized(0.25F, 0.25F)
                    .clientTrackingRange(6).updateInterval(1)));

    /** Nilo, el pescador (llega solo la primera vez que alguien pesca). */
    public static final net.minecraftforge.registries.RegistryObject<EntityType<com.pescacoleccion.nilo.Nilo>> NILO = ENTITIES.register("nilo",
            () -> com.axeliscore.plat.Reg.entity(PescaColeccion.MODID, "nilo", EntityType.Builder.<com.pescacoleccion.nilo.Nilo>of(
                    com.pescacoleccion.nilo.Nilo::new, net.minecraft.world.entity.MobCategory.CREATURE).sized(0.6F, 1.8F)
                    .clientTrackingRange(10)));

    /** Las cañas (una por tipo de {@link com.pescacoleccion.pesca.Cana}). */
    public static final java.util.Map<com.pescacoleccion.pesca.Cana, net.minecraftforge.registries.RegistryObject<Item>> CANAS = new java.util.EnumMap<>(
            com.pescacoleccion.pesca.Cana.class);

    public static final net.minecraftforge.registries.RegistryObject<Item> LIBRO = ITEMS.register("libro_pesca",
            () -> new com.pescacoleccion.pesca.LibroItem(com.axeliscore.plat.Reg.item(PescaColeccion.MODID, "libro_pesca").stacksTo(1)));

    /** Las carnadas. */
    public static final java.util.Map<com.pescacoleccion.pesca.Cebo, net.minecraftforge.registries.RegistryObject<Item>> CEBOS = new java.util.EnumMap<>(
            com.pescacoleccion.pesca.Cebo.class);

    /** Un objeto por cada pez de la colección (por id de especie). */
    public static final java.util.Map<String, net.minecraftforge.registries.RegistryObject<Item>> PECES = new java.util.LinkedHashMap<>();

    static {
        for (com.pescacoleccion.pesca.Especie e : com.pescacoleccion.pesca.Especies.all()) {
            PECES.put(e.id(), ITEMS.register(e.itemId(), () -> new com.pescacoleccion.pesca.PezItem(
                    com.axeliscore.plat.Reg.item(PescaColeccion.MODID, e.itemId()).stacksTo(16), e.id())));
        }
        for (com.pescacoleccion.pesca.Cebo c : com.pescacoleccion.pesca.Cebo.values()) {
            CEBOS.put(c, ITEMS.register(c.id, () -> new com.pescacoleccion.pesca.CeboItem(com.axeliscore.plat.Reg.item(PescaColeccion.MODID, c.id), c)));
        }
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
                        out.accept(LIBRO.get());
                        out.accept(TROFEO_ITEM.get());
                        out.accept(ACUARIO_ITEM.get());
                        for (var c : CANAS.values()) {
                            out.accept(c.get());
                        }
                        for (var c : CEBOS.values()) {
                            out.accept(c.get());
                        }
                        for (var f : PECES.values()) {
                            out.accept(f.get());
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
        BLOCK_ENTITIES.register(bus);
        TABS.register(bus);
    }
}
