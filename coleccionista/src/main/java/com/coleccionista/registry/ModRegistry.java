package com.coleccionista.registry;

import com.coleccionista.Coleccionista;
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
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, Coleccionista.MODID);
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, Coleccionista.MODID);
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, Coleccionista.MODID);
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, Coleccionista.MODID);

    /** El zumbido de las lámparas fluorescentes viejas. */
    public static final net.minecraftforge.registries.RegistryObject<SoundEvent> ZUMBIDO = SOUNDS.register("zumbido",
            () -> SoundEvent.createVariableRangeEvent(new net.minecraft.resources.ResourceLocation(Coleccionista.MODID, "zumbido")));
    /** El chispazo de una lámpara que falla (se apaga un instante y vuelve). */
    public static final net.minecraftforge.registries.RegistryObject<SoundEvent> CHISPA = SOUNDS.register("chispa",
            () -> SoundEvent.createVariableRangeEvent(new net.minecraft.resources.ResourceLocation(Coleccionista.MODID, "chispa")));

    // ---------------------------------------------------------------- bloques del mapa
    public static final net.minecraftforge.registries.RegistryObject<Block> PANTALLA = BLOCKS.register("pantalla",
            () -> new com.coleccionista.bloque.PantallaBlock(com.axeliscore.plat.Reg.block(Coleccionista.MODID, "pantalla")
                    .strength(-1F, 3600000F).noOcclusion().lightLevel(s -> 3).sound(net.minecraft.world.level.block.SoundType.GLASS)));
    public static final net.minecraftforge.registries.RegistryObject<Block> CANDADO = BLOCKS.register("candado",
            () -> new com.coleccionista.bloque.CandadoBlock(com.axeliscore.plat.Reg.block(Coleccionista.MODID, "candado")
                    .strength(-1F, 3600000F).noOcclusion().sound(net.minecraft.world.level.block.SoundType.METAL)));
    public static final net.minecraftforge.registries.RegistryObject<Block> FOCO = BLOCKS.register("foco",
            () -> new com.coleccionista.bloque.FocoBlock(com.axeliscore.plat.Reg.block(Coleccionista.MODID, "foco")
                    .strength(-1F, 3600000F).noOcclusion().sound(net.minecraft.world.level.block.SoundType.LANTERN)
                    .lightLevel(s -> com.coleccionista.bloque.FocoBlock.prendido(s) ? 15 : 0)));
    public static final net.minecraftforge.registries.RegistryObject<Item> PANTALLA_ITEM = ITEMS.register("pantalla",
            () -> new net.minecraft.world.item.BlockItem(PANTALLA.get(), com.axeliscore.plat.Reg.blockItem(Coleccionista.MODID, "pantalla")));
    public static final net.minecraftforge.registries.RegistryObject<Item> CANDADO_ITEM = ITEMS.register("candado",
            () -> new net.minecraft.world.item.BlockItem(CANDADO.get(), com.axeliscore.plat.Reg.blockItem(Coleccionista.MODID, "candado")));
    public static final net.minecraftforge.registries.RegistryObject<Item> FOCO_ITEM = ITEMS.register("foco",
            () -> new net.minecraft.world.item.BlockItem(FOCO.get(), com.axeliscore.plat.Reg.blockItem(Coleccionista.MODID, "foco")));
    /** El teclado de la bóveda (El traidor). */
    public static final net.minecraftforge.registries.RegistryObject<Block> TECLADO = BLOCKS.register("teclado",
            () -> new com.coleccionista.bloque.TecladoBlock(com.axeliscore.plat.Reg.block(Coleccionista.MODID, "teclado")
                    .strength(-1F, 3600000F).noOcclusion().sound(net.minecraft.world.level.block.SoundType.METAL)));
    public static final net.minecraftforge.registries.RegistryObject<Item> TECLADO_ITEM = ITEMS.register("teclado",
            () -> new net.minecraft.world.item.BlockItem(TECLADO.get(), com.axeliscore.plat.Reg.blockItem(Coleccionista.MODID, "teclado")));
    /** Una copa de la final (Las copas). */
    public static final net.minecraftforge.registries.RegistryObject<Block> COPA = BLOCKS.register("copa",
            () -> new com.coleccionista.bloque.CopaBlock(com.axeliscore.plat.Reg.block(Coleccionista.MODID, "copa")
                    .strength(-1F, 3600000F).noOcclusion().sound(net.minecraft.world.level.block.SoundType.METAL)));
    public static final net.minecraftforge.registries.RegistryObject<Item> COPA_ITEM = ITEMS.register("copa",
            () -> new net.minecraft.world.item.BlockItem(COPA.get(), com.axeliscore.plat.Reg.blockItem(Coleccionista.MODID, "copa")));
    /** La llave del grillete de una pieza (Encadenados). */
    public static final net.minecraftforge.registries.RegistryObject<Item> LLAVE = ITEMS.register("llave_grillete",
            () -> new com.coleccionista.prueba.LlaveGrillete(com.axeliscore.plat.Reg.item(Coleccionista.MODID, "llave_grillete").stacksTo(1)));
    public static final DeferredRegister<net.minecraft.world.level.block.entity.BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, Coleccionista.MODID);
    public static final net.minecraftforge.registries.RegistryObject<net.minecraft.world.level.block.entity.BlockEntityType<
            com.coleccionista.bloque.PantallaBE>> PANTALLA_BE = BLOCK_ENTITIES.register("pantalla",
            () -> com.axeliscore.plat.Reg.blockEntity(com.coleccionista.bloque.PantallaBE::new, PANTALLA.get()));
    /** El cuerpo de una pieza eliminada. */
    public static final net.minecraftforge.registries.RegistryObject<EntityType<com.coleccionista.evento.Cadaver>> CADAVER =
            ENTITIES.register("cadaver", () -> com.axeliscore.plat.Reg.entity(Coleccionista.MODID, "cadaver",
                    EntityType.Builder.<com.coleccionista.evento.Cadaver>of(com.coleccionista.evento.Cadaver::new,
                            net.minecraft.world.entity.MobCategory.MISC).sized(1.6F, 0.4F).clientTrackingRange(10)));
    /** La silla que se mueve sola (la salida de los circuitos de los sentidos). */
    public static final net.minecraftforge.registries.RegistryObject<EntityType<com.coleccionista.evento.Silla>> SILLA =
            ENTITIES.register("silla", () -> com.axeliscore.plat.Reg.entity(Coleccionista.MODID, "silla",
                    EntityType.Builder.<com.coleccionista.evento.Silla>of(com.coleccionista.evento.Silla::new,
                            net.minecraft.world.entity.MobCategory.MISC).sized(0.8F, 0.9F).clientTrackingRange(10).updateInterval(1)));
    /** La escopeta que sale del piso en la caja de música (capítulo 3). */
    public static final net.minecraftforge.registries.RegistryObject<EntityType<com.coleccionista.evento.Escopeta>> ESCOPETA =
            ENTITIES.register("escopeta", () -> com.axeliscore.plat.Reg.entity(Coleccionista.MODID, "escopeta",
                    EntityType.Builder.<com.coleccionista.evento.Escopeta>of(com.coleccionista.evento.Escopeta::new,
                            net.minecraft.world.entity.MobCategory.MISC).sized(0.6F, 0.6F).clientTrackingRange(10).updateInterval(1)));
    /** La caja sorpresa gigante con Don Resorte adentro (capítulo 4). */
    public static final net.minecraftforge.registries.RegistryObject<EntityType<com.coleccionista.evento.CajaSorpresa>> CAJA_SORPRESA =
            ENTITIES.register("caja_sorpresa", () -> com.axeliscore.plat.Reg.entity(Coleccionista.MODID, "caja_sorpresa",
                    EntityType.Builder.<com.coleccionista.evento.CajaSorpresa>of(com.coleccionista.evento.CajaSorpresa::new,
                            net.minecraft.world.entity.MobCategory.MISC).sized(3.5F, 3.6F).clientTrackingRange(10).updateInterval(1)));
    /** Las manos gigantes de porcelana del salón de té (capítulo 5): juegan piedra, papel o tijera. */
    public static final net.minecraftforge.registries.RegistryObject<EntityType<com.coleccionista.evento.ManoGigante>> MANO_GIGANTE =
            ENTITIES.register("mano_gigante", () -> com.axeliscore.plat.Reg.entity(Coleccionista.MODID, "mano_gigante",
                    EntityType.Builder.<com.coleccionista.evento.ManoGigante>of(com.coleccionista.evento.ManoGigante::new,
                            net.minecraft.world.entity.MobCategory.MISC).sized(0.8F, 0.8F).clientTrackingRange(10).updateInterval(2)));
    /** La bailarina de la caja de música gigante (capítulo 6, el piano). */
    public static final net.minecraftforge.registries.RegistryObject<EntityType<com.coleccionista.evento.Bailarina>> BAILARINA =
            ENTITIES.register("bailarina", () -> com.axeliscore.plat.Reg.entity(Coleccionista.MODID, "bailarina",
                    EntityType.Builder.<com.coleccionista.evento.Bailarina>of(com.coleccionista.evento.Bailarina::new,
                            net.minecraft.world.entity.MobCategory.MISC).sized(2.4F, 5F).clientTrackingRange(10).updateInterval(2)));
    /** Jugador de prueba (bot) para correr el evento sin amigos. */
    public static final net.minecraftforge.registries.RegistryObject<EntityType<com.coleccionista.bot.PiezaBot>> BOT = ENTITIES.register("bot",
            () -> com.axeliscore.plat.Reg.entity(Coleccionista.MODID, "bot", EntityType.Builder.<com.coleccionista.bot.PiezaBot>of(
                    com.coleccionista.bot.PiezaBot::new, net.minecraft.world.entity.MobCategory.MISC).sized(0.6F, 1.8F)
                    .clientTrackingRange(10)));

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
    }
}
