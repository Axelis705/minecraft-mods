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

    /** Lobezno de escarcha: la manada que invoca el Lobo Ártico. */
    public static final RegistryObject<EntityType<com.mundosdeaxelis.entity.LoboEscarcha>> LOBO_ESCARCHA = ENTITIES.register("lobo_escarcha",
            () -> EntityType.Builder.<com.mundosdeaxelis.entity.LoboEscarcha>of(com.mundosdeaxelis.entity.LoboEscarcha::new, MobCategory.MONSTER)
                    .sized(0.9F, 1.1F)
                    .clientTrackingRange(10)
                    .build(new ResourceLocation(MundosDeAxelis.MODID, "lobo_escarcha").toString()));
}
