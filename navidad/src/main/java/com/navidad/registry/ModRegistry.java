package com.navidad.registry;

import com.navidad.Navidad;
import com.navidad.bloque.CajaRegalo;
import com.navidad.bloque.CajaRegaloBE;
import com.navidad.bloque.CajaRegaloItem;
import com.navidad.bloque.Calendario;
import com.navidad.entidad.HombreJengibre;
import com.navidad.entidad.Krampus;
import com.navidad.entidad.MunecoMalvado;
import com.navidad.entidad.Pinata;
import com.navidad.entidad.Reno;
import com.navidad.entidad.TrineoSanta;
import com.navidad.objeto.Bengala;
import com.navidad.objeto.Rosca;
import com.navidad.objeto.CampanaKrampus;
import net.minecraftforge.common.ForgeSpawnEggItem;
import com.navidad.objeto.Bebida;
import com.navidad.objeto.PinataItem;
import com.navidad.objeto.RegaloMisterioso;
import com.navidad.objeto.Ropa;
import com.navidad.objeto.Venda;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.RecordItem;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/** Registros del mod de Navidad (los bloques de decoración van aparte, en {@code bloque/deco/Deco}, desde {@code deco.json}). */
public final class ModRegistry {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, Navidad.MODID);
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, Navidad.MODID);
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, Navidad.MODID);
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, Navidad.MODID);
    public static final DeferredRegister<BlockEntityType<?>> BES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, Navidad.MODID);
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Navidad.MODID);

    /** En qué orden salen en la pestaña creativa. */
    private static final List<RegistryObject<? extends Item>> ORDEN = new ArrayList<>();

    private ModRegistry() {
    }

    private static <T extends Item> RegistryObject<T> item(String id, Supplier<T> s) {
        RegistryObject<T> r = ITEMS.register(id, s);
        ORDEN.add(r);
        return r;
    }

    private static Item.Properties props() {
        return new Item.Properties();
    }

    private static FoodProperties.Builder comida(int n, float sat) {
        return new FoodProperties.Builder().nutrition(n).saturationMod(sat);
    }

    // ------------------------------------------------------------------ sonidos

    private static RegistryObject<SoundEvent> sonido(String id) {
        return SOUNDS.register(id, () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(Navidad.MODID, id)));
    }

    public static final RegistryObject<SoundEvent> CANCION_PINATA = sonido("cancion_pinata");
    public static final RegistryObject<SoundEvent> CASCABELES = sonido("cascabeles");
    public static final RegistryObject<SoundEvent> JINGLE_BELLS = sonido("villancico_campanas");
    public static final RegistryObject<SoundEvent> WE_WISH = sonido("villancico_navidad");
    public static final RegistryObject<SoundEvent> NOCHE_PAZ = sonido("villancico_noche_paz");
    public static final RegistryObject<SoundEvent> ADORNEN = sonido("villancico_adornen");
    public static final RegistryObject<SoundEvent> RISA_SANTA = sonido("risa_santa");
    public static final RegistryObject<SoundEvent> KRAMPUS_RUGE = sonido("krampus_ruge");
    public static final RegistryObject<SoundEvent> KRAMPUS_MUSICA = sonido("krampus_musica");
    public static final RegistryObject<SoundEvent> CALENDARIO_ABRE = sonido("calendario_abre");
    public static final RegistryObject<SoundEvent> REGALO_ABRE = sonido("regalo_abre");

    // ------------------------------------------------------------------ comida

    public static final RegistryObject<Item> GALLETA_JENGIBRE = item("galleta_jengibre", () -> new Item(props().food(comida(3, 0.4F).fast().build())));
    public static final RegistryObject<Item> BASTON_CARAMELO = item("baston_caramelo", () -> new Item(props().food(comida(2, 0.2F).fast()
            .effect(() -> new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 200, 0), 1F).build())));
    public static final RegistryObject<Item> DULCE = item("dulce", () -> new Item(props().food(comida(1, 0.1F).fast().alwaysEat().build())));
    public static final RegistryObject<Item> TEJOCOTE = item("tejocote", () -> new Item(props().food(comida(2, 0.3F).build())));
    public static final RegistryObject<Item> MANDARINA = item("mandarina", () -> new Item(props().food(comida(3, 0.4F)
            .effect(() -> new MobEffectInstance(MobEffects.REGENERATION, 60, 0), 0.3F).build())));
    public static final RegistryObject<Item> CANA = item("cana", () -> new Item(props().food(comida(2, 0.2F).fast().build())));
    public static final RegistryObject<Item> TAMAL = item("tamal", () -> new Item(props().food(comida(6, 0.8F).build())));
    public static final RegistryObject<Item> BUNUELO = item("bunuelo", () -> new Item(props().food(comida(4, 0.5F)
            .effect(() -> new MobEffectInstance(MobEffects.DIG_SPEED, 400, 0), 1F).build())));
    public static final RegistryObject<Item> PAVO_NAVIDENO = item("pavo_navideno", () -> new Item(props().stacksTo(16).food(comida(12, 1.0F).meat()
            .effect(() -> new MobEffectInstance(MobEffects.REGENERATION, 200, 0), 1F)
            .effect(() -> new MobEffectInstance(MobEffects.ABSORPTION, 1200, 1), 1F).build())));
    public static final RegistryObject<Item> CHOCOLATE_CALIENTE = item("chocolate_caliente", () -> new Bebida(props().stacksTo(16)
            .food(comida(5, 0.6F).alwaysEat().effect(() -> new MobEffectInstance(MobEffects.REGENERATION, 100, 0), 1F).build())));
    public static final RegistryObject<Item> PONCHE = item("ponche", () -> new Bebida(props().stacksTo(16)
            .food(comida(4, 0.5F).alwaysEat().effect(() -> new MobEffectInstance(MobEffects.REGENERATION, 160, 0), 1F).build())));
    public static final RegistryObject<Item> ROSCA = item("rosca_reyes", () -> new Rosca(props().stacksTo(16).food(comida(8, 0.8F)
            .effect(() -> new MobEffectInstance(MobEffects.LUCK, 2400, 0), 1F).build())));
    public static final RegistryObject<Item> CHAMPURRADO = item("champurrado", () -> new Bebida(props().stacksTo(16)
            .food(comida(6, 0.7F).alwaysEat().effect(() -> new MobEffectInstance(MobEffects.ABSORPTION, 600, 0), 1F).build())));

    // ------------------------------------------------------------------ regalos y posadas

    public static final RegistryObject<Item> REGALO_MISTERIOSO = item("regalo_misterioso", () -> new RegaloMisterioso(props().stacksTo(16)
            .rarity(Rarity.UNCOMMON)));
    public static final RegistryObject<Item> PINATA = item("pinata", () -> new PinataItem(props().stacksTo(4)));
    public static final RegistryObject<Item> PALO_PINATA = item("palo_pinata", () -> new Item(props().stacksTo(1)));
    public static final RegistryObject<Item> VENDA = item("venda", () -> new Venda(props().stacksTo(1)));
    public static final RegistryObject<Item> BENGALA = item("bengala", () -> new Bengala(props().stacksTo(16)));
    /** En el cortapiedras se vuelve cualquier bloque de decoración. */
    public static final RegistryObject<Item> CAJA_ADORNOS = item("caja_adornos", () -> new Item(props()));

    public static final RegistryObject<Block> CAJA_REGALO = BLOCKS.register("caja_regalo", () -> new CajaRegalo(BlockBehaviour.Properties.of()
            .strength(0.4F).sound(SoundType.WOOL).noOcclusion()));
    public static final RegistryObject<Item> CAJA_REGALO_ITEM = item("caja_regalo", () -> new CajaRegaloItem(CAJA_REGALO.get(), props().stacksTo(1)));
    public static final RegistryObject<Block> CALENDARIO = BLOCKS.register("calendario_adviento", () -> new Calendario(BlockBehaviour.Properties.of()
            .strength(1.0F).sound(SoundType.WOOD).noOcclusion().lightLevel(s -> 6)));
    public static final RegistryObject<Item> CALENDARIO_ITEM = item("calendario_adviento", () -> new BlockItem(CALENDARIO.get(), props()));

    public static final RegistryObject<BlockEntityType<CajaRegaloBE>> CAJA_REGALO_BE = BES.register("caja_regalo",
            () -> BlockEntityType.Builder.of(CajaRegaloBE::new, CAJA_REGALO.get()).build(null));

    public static final RegistryObject<EntityType<Pinata>> PINATA_ENT = ENTITIES.register("pinata", () -> EntityType.Builder.<Pinata>of(Pinata::new,
            MobCategory.MISC).sized(1.2F, 1.7F).clientTrackingRange(10).build("navidad:pinata"));

    // ------------------------------------------------------------------ ropa (se pone encima con el vestuario del Core)

    public static final RegistryObject<Item> GORRO_SANTA = item("gorro_santa", () -> new Ropa(props().stacksTo(1), "gorro_santa"));
    public static final RegistryObject<Item> GORRO_DUENDE = item("gorro_duende", () -> new Ropa(props().stacksTo(1), "gorro_duende"));
    public static final RegistryObject<Item> CUERNOS_RENO = item("cuernos_reno", () -> new Ropa(props().stacksTo(1), "cuernos_reno"));
    public static final RegistryObject<Item> SUETER_RENO = item("sueter_reno", () -> new Ropa(props().stacksTo(1), "sueter_reno"));
    public static final RegistryObject<Item> SUETER_PINOS = item("sueter_pinos", () -> new Ropa(props().stacksTo(1), "sueter_pinos"));
    public static final RegistryObject<Item> SUETER_COPOS = item("sueter_copos", () -> new Ropa(props().stacksTo(1), "sueter_copos"));
    public static final RegistryObject<Item> TRAJE_SANTA = item("traje_santa", () -> new Ropa(props().stacksTo(1).rarity(Rarity.RARE), "traje_santa"));

    // ------------------------------------------------------------------ discos (villancicos de dominio público en caja de música)

    public static final RegistryObject<Item> DISCO_CAMPANAS = item("disco_campanas", () -> new RecordItem(1, JINGLE_BELLS, props().stacksTo(1)
            .rarity(Rarity.RARE), Musica.LARGO_CAMPANAS));
    public static final RegistryObject<Item> DISCO_NAVIDAD = item("disco_navidad", () -> new RecordItem(2, WE_WISH, props().stacksTo(1)
            .rarity(Rarity.RARE), Musica.LARGO_NAVIDAD));
    public static final RegistryObject<Item> DISCO_NOCHE_PAZ = item("disco_noche_paz", () -> new RecordItem(3, NOCHE_PAZ, props().stacksTo(1)
            .rarity(Rarity.RARE), Musica.LARGO_NOCHE_PAZ));
    public static final RegistryObject<Item> DISCO_ADORNEN = item("disco_adornen", () -> new RecordItem(4, ADORNEN, props().stacksTo(1)
            .rarity(Rarity.RARE), Musica.LARGO_ADORNEN));

    // ------------------------------------------------------------------ criaturas

    public static final RegistryObject<EntityType<MunecoMalvado>> MUNECO_MALVADO = ENTITIES.register("muneco_malvado",
            () -> EntityType.Builder.<MunecoMalvado>of(MunecoMalvado::new, MobCategory.MONSTER).sized(0.8F, 1.8F).clientTrackingRange(10)
                    .build("navidad:muneco_malvado"));
    public static final RegistryObject<EntityType<Reno>> RENO = ENTITIES.register("reno",
            () -> EntityType.Builder.<Reno>of(Reno::new, MobCategory.CREATURE).sized(1.0F, 1.6F).clientTrackingRange(10).build("navidad:reno"));
    public static final RegistryObject<EntityType<HombreJengibre>> HOMBRE_JENGIBRE = ENTITIES.register("hombre_jengibre",
            () -> EntityType.Builder.<HombreJengibre>of(HombreJengibre::new, MobCategory.CREATURE).sized(0.4F, 1.0F).clientTrackingRange(10)
                    .build("navidad:hombre_jengibre"));
    public static final RegistryObject<EntityType<Krampus>> KRAMPUS = ENTITIES.register("krampus",
            () -> EntityType.Builder.<Krampus>of(Krampus::new, MobCategory.MONSTER).sized(1.6F, 3.0F).fireImmune().clientTrackingRange(12)
                    .build("navidad:krampus"));

    public static final RegistryObject<EntityType<TrineoSanta>> TRINEO = ENTITIES.register("trineo_santa",
            () -> EntityType.Builder.<TrineoSanta>of(TrineoSanta::new, MobCategory.MISC).sized(3F, 2F).clientTrackingRange(20).updateInterval(2)
                    .build("navidad:trineo_santa"));

    public static final RegistryObject<Item> CAMPANA_KRAMPUS = item("campana_krampus", () -> new CampanaKrampus(props().stacksTo(1).rarity(Rarity.EPIC)));
    public static final RegistryObject<Item> CUERNO_KRAMPUS = item("cuerno_krampus", () -> new Item(props().stacksTo(16).rarity(Rarity.EPIC)));
    public static final RegistryObject<Item> HUEVO_MUNECO = item("huevo_muneco_malvado", () -> new ForgeSpawnEggItem(MUNECO_MALVADO, 0xF0F4FA, 0xC81E2A, props()));
    public static final RegistryObject<Item> HUEVO_RENO = item("huevo_reno", () -> new ForgeSpawnEggItem(RENO, 0x8C603A, 0xE4D6BA, props()));
    public static final RegistryObject<Item> HUEVO_JENGIBRE = item("huevo_hombre_jengibre", () -> new ForgeSpawnEggItem(HOMBRE_JENGIBRE, 0xB8743A, 0xF6F6F2, props()));
    public static final RegistryObject<Item> HUEVO_KRAMPUS = item("huevo_krampus", () -> new ForgeSpawnEggItem(KRAMPUS, 0x34261F, 0xFFBE28, props()));

    // ------------------------------------------------------------------ la pestaña

    public static final RegistryObject<CreativeModeTab> TAB = TABS.register("cosas", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.navidad.cosas"))
            .icon(() -> new ItemStack(REGALO_MISTERIOSO.get()))
            .displayItems((p, out) -> ORDEN.forEach(r -> out.accept(r.get()))).build());

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
        BLOCKS.register(bus);
        ENTITIES.register(bus);
        SOUNDS.register(bus);
        BES.register(bus);
        TABS.register(bus);
    }
}
