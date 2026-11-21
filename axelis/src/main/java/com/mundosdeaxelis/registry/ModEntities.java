package com.mundosdeaxelis.registry;

import com.mundosdeaxelis.MundosDeAxelis;
import com.mundosdeaxelis.entity.ReyBrote;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, MundosDeAxelis.MODID);

    /**
     * Etapa 254: las mascotas equipadas como criaturas de verdad (un solo tipo; la especie va en sus datos y cambia su caja,
     * su modelo y su forma de moverse). Nunca se guardan con el mundo: las crea {@code relic.PetKeeper}.
     */
    public static final RegistryObject<EntityType<com.mundosdeaxelis.entity.Mascota>> MASCOTA = ENTITIES.register("mascota",
            () -> EntityType.Builder.<com.mundosdeaxelis.entity.Mascota>of(com.mundosdeaxelis.entity.Mascota::new, MobCategory.CREATURE)
                    .sized(0.5F, 0.6F)
                    .clientTrackingRange(10)
                    .build(new ResourceLocation(MundosDeAxelis.MODID, "mascota").toString()));

    /** El Rey Brote: el jefe de la etapa 1 (Naturaleza). */
    public static final RegistryObject<EntityType<ReyBrote>> REY_BROTE = ENTITIES.register("rey_brote",
            () -> EntityType.Builder.<ReyBrote>of(ReyBrote::new, MobCategory.MONSTER)
                    .sized(3.0F, 3.0F)
                    .clientTrackingRange(16)
                    .build(new ResourceLocation(MundosDeAxelis.MODID, "rey_brote").toString()));

    /** Mobs de la dungeon del Rey Brote: Brote (común), Espinoso (flor que dispara) y Guardián de Musgo (mini-jefe). */
    public static final RegistryObject<EntityType<com.mundosdeaxelis.entity.Brote>> BROTE = ENTITIES.register("brote",
            () -> EntityType.Builder.<com.mundosdeaxelis.entity.Brote>of(com.mundosdeaxelis.entity.Brote::new, MobCategory.MONSTER)
                    .sized(0.7F, 1.5F)
                    .clientTrackingRange(10)
                    .build(new ResourceLocation(MundosDeAxelis.MODID, "brote").toString()));
    public static final RegistryObject<EntityType<com.mundosdeaxelis.entity.Espinoso>> ESPINOSO = ENTITIES.register("espinoso",
            () -> EntityType.Builder.<com.mundosdeaxelis.entity.Espinoso>of(com.mundosdeaxelis.entity.Espinoso::new, MobCategory.MONSTER)
                    .sized(0.8F, 1.6F)
                    .clientTrackingRange(10)
                    .build(new ResourceLocation(MundosDeAxelis.MODID, "espinoso").toString()));
    public static final RegistryObject<EntityType<com.mundosdeaxelis.entity.GuardianMusgo>> GUARDIAN_MUSGO = ENTITIES.register("guardian_musgo",
            () -> EntityType.Builder.<com.mundosdeaxelis.entity.GuardianMusgo>of(com.mundosdeaxelis.entity.GuardianMusgo::new, MobCategory.MONSTER)
                    .sized(1.9F, 3.0F)
                    .clientTrackingRange(16)
                    .build(new ResourceLocation(MundosDeAxelis.MODID, "guardian_musgo").toString()));

    public static final RegistryObject<EntityType<com.mundosdeaxelis.entity.Raizal>> RAIZAL = ENTITIES.register("raizal",
            () -> EntityType.Builder.<com.mundosdeaxelis.entity.Raizal>of(com.mundosdeaxelis.entity.Raizal::new, MobCategory.MONSTER)
                    .sized(1.0F, 2.4F)
                    .clientTrackingRange(12)
                    .build(new ResourceLocation(MundosDeAxelis.MODID, "raizal").toString()));
    public static final RegistryObject<EntityType<com.mundosdeaxelis.entity.Polinizador>> POLINIZADOR = ENTITIES.register("polinizador",
            () -> EntityType.Builder.<com.mundosdeaxelis.entity.Polinizador>of(com.mundosdeaxelis.entity.Polinizador::new, MobCategory.MONSTER)
                    .sized(0.7F, 0.8F)
                    .clientTrackingRange(10)
                    .build(new ResourceLocation(MundosDeAxelis.MODID, "polinizador").toString()));

    /** Mobs de la dungeon del Gran Herrero (etapa 2): Escoria (común), Chispero (horno que dispara), Ascua (brasa voladora), Fundidor (élite) y Capataz (mini-jefe). */
    public static final RegistryObject<EntityType<com.mundosdeaxelis.entity.Escoria>> ESCORIA = ENTITIES.register("escoria",
            () -> EntityType.Builder.<com.mundosdeaxelis.entity.Escoria>of(com.mundosdeaxelis.entity.Escoria::new, MobCategory.MONSTER)
                    .sized(0.7F, 1.5F)
                    .clientTrackingRange(10)
                    .fireImmune()
                    .build(new ResourceLocation(MundosDeAxelis.MODID, "escoria").toString()));
    public static final RegistryObject<EntityType<com.mundosdeaxelis.entity.Chispero>> CHISPERO = ENTITIES.register("chispero",
            () -> EntityType.Builder.<com.mundosdeaxelis.entity.Chispero>of(com.mundosdeaxelis.entity.Chispero::new, MobCategory.MONSTER)
                    .sized(0.8F, 1.6F)
                    .clientTrackingRange(10)
                    .fireImmune()
                    .build(new ResourceLocation(MundosDeAxelis.MODID, "chispero").toString()));
    public static final RegistryObject<EntityType<com.mundosdeaxelis.entity.Ascua>> ASCUA = ENTITIES.register("ascua",
            () -> EntityType.Builder.<com.mundosdeaxelis.entity.Ascua>of(com.mundosdeaxelis.entity.Ascua::new, MobCategory.MONSTER)
                    .sized(0.7F, 0.8F)
                    .clientTrackingRange(10)
                    .fireImmune()
                    .build(new ResourceLocation(MundosDeAxelis.MODID, "ascua").toString()));
    public static final RegistryObject<EntityType<com.mundosdeaxelis.entity.Fundidor>> FUNDIDOR = ENTITIES.register("fundidor",
            () -> EntityType.Builder.<com.mundosdeaxelis.entity.Fundidor>of(com.mundosdeaxelis.entity.Fundidor::new, MobCategory.MONSTER)
                    .sized(1.1F, 2.6F)
                    .clientTrackingRange(12)
                    .fireImmune()
                    .build(new ResourceLocation(MundosDeAxelis.MODID, "fundidor").toString()));
    /** Etapa 267: el Coloso de Hierro, minijefe de la Fragua Olvidada, y los lingotes al rojo que lanza. */
    public static final RegistryObject<EntityType<com.mundosdeaxelis.entity.ColosoHierro>> COLOSO_HIERRO = ENTITIES.register("coloso_hierro",
            () -> EntityType.Builder.<com.mundosdeaxelis.entity.ColosoHierro>of(com.mundosdeaxelis.entity.ColosoHierro::new, MobCategory.MONSTER)
                    .sized(2.0F, 3.5F)
                    .clientTrackingRange(16)
                    .fireImmune()
                    .build(new ResourceLocation(MundosDeAxelis.MODID, "coloso_hierro").toString()));
    public static final RegistryObject<EntityType<com.mundosdeaxelis.entity.LingoteRojo>> LINGOTE_ROJO = ENTITIES.register("lingote_rojo",
            () -> EntityType.Builder.<com.mundosdeaxelis.entity.LingoteRojo>of(com.mundosdeaxelis.entity.LingoteRojo::new, MobCategory.MISC)
                    .sized(0.35F, 0.35F)
                    .clientTrackingRange(8)
                    .updateInterval(5)
                    .fireImmune()
                    .build(new ResourceLocation(MundosDeAxelis.MODID, "lingote_rojo").toString()));
    public static final RegistryObject<EntityType<com.mundosdeaxelis.entity.Capataz>> CAPATAZ = ENTITIES.register("capataz",
            () -> EntityType.Builder.<com.mundosdeaxelis.entity.Capataz>of(com.mundosdeaxelis.entity.Capataz::new, MobCategory.MONSTER)
                    .sized(1.9F, 3.2F)
                    .clientTrackingRange(16)
                    .fireImmune()
                    .build(new ResourceLocation(MundosDeAxelis.MODID, "capataz").toString()));

    private ModEntities() {
    }

    /** El Gran Herrero de Brasas: el jefe de la etapa 2 (Forja). */
    public static final RegistryObject<EntityType<com.mundosdeaxelis.entity.GranHerrero>> GRAN_HERRERO = ENTITIES.register("gran_herrero",
            () -> EntityType.Builder.<com.mundosdeaxelis.entity.GranHerrero>of(com.mundosdeaxelis.entity.GranHerrero::new, MobCategory.MONSTER)
                    .sized(2.6F, 5.0F)
                    .clientTrackingRange(16)
                    .fireImmune()
                    .build(new ResourceLocation(MundosDeAxelis.MODID, "gran_herrero").toString()));

    /** El Lobo Ártico: el jefe de la etapa 3 (Hielo). */
    public static final RegistryObject<EntityType<com.mundosdeaxelis.entity.LoboArtico>> LOBO_ARTICO = ENTITIES.register("lobo_artico",
            () -> EntityType.Builder.<com.mundosdeaxelis.entity.LoboArtico>of(com.mundosdeaxelis.entity.LoboArtico::new, MobCategory.MONSTER)
                    .sized(2.2F, 4.8F)
                    .clientTrackingRange(16)
                    .build(new ResourceLocation(MundosDeAxelis.MODID, "lobo_artico").toString()));

    /** El Archimago: el jefe de la etapa 4 (la Torre). */
    public static final RegistryObject<EntityType<com.mundosdeaxelis.entity.Archimago>> ARCHIMAGO = ENTITIES.register("archimago",
            () -> EntityType.Builder.<com.mundosdeaxelis.entity.Archimago>of(com.mundosdeaxelis.entity.Archimago::new, MobCategory.MONSTER)
                    .sized(1.5F, 4.2F)
                    .clientTrackingRange(16)
                    .build(new ResourceLocation(MundosDeAxelis.MODID, "archimago").toString()));
    public static final RegistryObject<EntityType<com.mundosdeaxelis.entity.CorazonMagma>> CORAZON_MAGMA = ENTITIES.register("corazon_magma",
            () -> EntityType.Builder.<com.mundosdeaxelis.entity.CorazonMagma>of(com.mundosdeaxelis.entity.CorazonMagma::new, MobCategory.MONSTER)
                    .sized(3.6F, 4.6F)
                    .fireImmune()
                    .clientTrackingRange(16)
                    .build(new ResourceLocation(MundosDeAxelis.MODID, "corazon_magma").toString()));
    public static final RegistryObject<EntityType<com.mundosdeaxelis.entity.AnclaMagma>> ANCLA_MAGMA = ENTITIES.register("ancla_magma",
            () -> EntityType.Builder.<com.mundosdeaxelis.entity.AnclaMagma>of(com.mundosdeaxelis.entity.AnclaMagma::new, MobCategory.MISC)
                    .sized(1.0F, 1.6F)
                    .fireImmune()
                    .clientTrackingRange(16)
                    .build(new ResourceLocation(MundosDeAxelis.MODID, "ancla_magma").toString()));
    public static final RegistryObject<EntityType<com.mundosdeaxelis.entity.LarvaMagma>> LARVA_MAGMA = ENTITIES.register("larva_magma",
            () -> EntityType.Builder.<com.mundosdeaxelis.entity.LarvaMagma>of(com.mundosdeaxelis.entity.LarvaMagma::new, MobCategory.MONSTER)
                    .sized(0.7F, 0.45F)
                    .fireImmune()
                    .clientTrackingRange(10)
                    .build(new ResourceLocation(MundosDeAxelis.MODID, "larva_magma").toString()));
    public static final RegistryObject<EntityType<com.mundosdeaxelis.entity.Esbirro>> ESBIRRO = ENTITIES.register("esbirro",
            () -> EntityType.Builder.<com.mundosdeaxelis.entity.Esbirro>of(com.mundosdeaxelis.entity.Esbirro::new, MobCategory.MONSTER)
                    .sized(0.6F, 1.95F)
                    .clientTrackingRange(10)
                    .build(new ResourceLocation(MundosDeAxelis.MODID, "esbirro").toString()));
    /** Enemigos del volcán (etapa 4). */
    public static final RegistryObject<EntityType<com.mundosdeaxelis.entity.SalamandraLava>> SALAMANDRA_LAVA = ENTITIES.register("salamandra_lava",
            () -> EntityType.Builder.<com.mundosdeaxelis.entity.SalamandraLava>of(com.mundosdeaxelis.entity.SalamandraLava::new, MobCategory.MONSTER)
                    .sized(1.0F, 0.55F)
                    .fireImmune()
                    .clientTrackingRange(10)
                    .build(new ResourceLocation(MundosDeAxelis.MODID, "salamandra_lava").toString()));

    /** Enemigo del Fortín de Escarcha (etapa 258). */
    public static final RegistryObject<EntityType<com.mundosdeaxelis.entity.CentinelaEscarcha>> CENTINELA_ESCARCHA = ENTITIES.register("centinela_escarcha",
            () -> EntityType.Builder.<com.mundosdeaxelis.entity.CentinelaEscarcha>of(com.mundosdeaxelis.entity.CentinelaEscarcha::new, MobCategory.MONSTER)
                    .sized(0.65F, 2.05F)
                    .clientTrackingRange(10)
                    .build(new ResourceLocation(MundosDeAxelis.MODID, "centinela_escarcha").toString()));
    /** Habitantes (etapa 257): los NPC que se rescatan de las celdas de las estructuras. */
    public static final RegistryObject<EntityType<com.mundosdeaxelis.entity.Habitante>> HABITANTE = ENTITIES.register("habitante",
            () -> EntityType.Builder.<com.mundosdeaxelis.entity.Habitante>of(com.mundosdeaxelis.entity.Habitante::new, MobCategory.MISC)
                    .sized(0.6F, 1.8F)
                    .fireImmune()
                    .clientTrackingRange(10)
                    .build(new ResourceLocation(MundosDeAxelis.MODID, "habitante").toString()));

    /** Mini jefe del volcán (etapa 255): la Gran Salamandra, guardiana del paso al santuario. */
    public static final RegistryObject<EntityType<com.mundosdeaxelis.entity.GranSalamandra>> GRAN_SALAMANDRA = ENTITIES.register("gran_salamandra",
            () -> EntityType.Builder.<com.mundosdeaxelis.entity.GranSalamandra>of(com.mundosdeaxelis.entity.GranSalamandra::new, MobCategory.MONSTER)
                    .sized(1.9F, 1.05F)
                    .fireImmune()
                    .clientTrackingRange(10)
                    .build(new ResourceLocation(MundosDeAxelis.MODID, "gran_salamandra").toString()));

    public static final RegistryObject<EntityType<com.mundosdeaxelis.entity.Diablillo>> DIABLILLO = ENTITIES.register("diablillo",
            () -> EntityType.Builder.<com.mundosdeaxelis.entity.Diablillo>of(com.mundosdeaxelis.entity.Diablillo::new, MobCategory.MONSTER)
                    .sized(0.6F, 0.95F)
                    .fireImmune()
                    .clientTrackingRange(10)
                    .build(new ResourceLocation(MundosDeAxelis.MODID, "diablillo").toString()));

    public static final RegistryObject<EntityType<com.mundosdeaxelis.entity.CultistaVolcan>> CULTISTA_VOLCAN = ENTITIES.register("cultista_volcan",
            () -> EntityType.Builder.<com.mundosdeaxelis.entity.CultistaVolcan>of(com.mundosdeaxelis.entity.CultistaVolcan::new, MobCategory.MONSTER)
                    .sized(0.6F, 1.95F)
                    .fireImmune()
                    .clientTrackingRange(10)
                    .build(new ResourceLocation(MundosDeAxelis.MODID, "cultista_volcan").toString()));

    public static final RegistryObject<EntityType<com.mundosdeaxelis.entity.LibroEmbrujado>> LIBRO_EMBRUJADO = ENTITIES.register("libro_embrujado",
            () -> EntityType.Builder.<com.mundosdeaxelis.entity.LibroEmbrujado>of(com.mundosdeaxelis.entity.LibroEmbrujado::new, MobCategory.MONSTER)
                    .sized(0.6F, 0.6F)
                    .clientTrackingRange(10)
                    .build(new ResourceLocation(MundosDeAxelis.MODID, "libro_embrujado").toString()));
    public static final RegistryObject<EntityType<com.mundosdeaxelis.entity.ArmaduraEncantada>> ARMADURA_ENCANTADA = ENTITIES.register("armadura_encantada",
            () -> EntityType.Builder.<com.mundosdeaxelis.entity.ArmaduraEncantada>of(com.mundosdeaxelis.entity.ArmaduraEncantada::new, MobCategory.MONSTER)
                    .sized(0.6F, 1.95F)
                    .clientTrackingRange(10)
                    .build(new ResourceLocation(MundosDeAxelis.MODID, "armadura_encantada").toString()));

    /** Lobezno de escarcha: la manada que invoca el Lobo Ártico. */
    public static final RegistryObject<EntityType<com.mundosdeaxelis.entity.LoboEscarcha>> LOBO_ESCARCHA = ENTITIES.register("lobo_escarcha",
            () -> EntityType.Builder.<com.mundosdeaxelis.entity.LoboEscarcha>of(com.mundosdeaxelis.entity.LoboEscarcha::new, MobCategory.MONSTER)
                    .sized(0.9F, 1.1F)
                    .clientTrackingRange(10)
                    .build(new ResourceLocation(MundosDeAxelis.MODID, "lobo_escarcha").toString()));
}
