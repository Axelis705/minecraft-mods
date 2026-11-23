package com.axeliscore.registry;

import com.axeliscore.AxelisCore;
import com.axeliscore.character.Alado;
import com.axeliscore.character.Bestia;
import com.axeliscore.character.Chispa;
import com.axeliscore.character.Coloso;
import com.axeliscore.character.Gelatina;
import com.axeliscore.character.Persona;
import com.axeliscore.plat.Reg;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Entidades del Core. Las de la librería (NPC, avisos de ataque y proyectiles) siempre están; las de muestra (persona, bestia, alado,
 * gelatina, coloso, chispa) solo con los ejemplos activos ({@link com.axeliscore.Ejemplos}): si no, sus campos son null.
 */
@Mod.EventBusSubscriber(modid = AxelisCore.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class CoreEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, AxelisCore.MODID);

    public static final RegistryObject<EntityType<Persona>> PERSONA = com.axeliscore.Ejemplos.activos() ? ENTITIES.register("persona",
            () -> Reg.entity(AxelisCore.MODID, "persona", EntityType.Builder.<Persona>of(Persona::new, MobCategory.CREATURE).sized(0.6F, 1.8F)
                    .clientTrackingRange(10))) : null;

    public static final RegistryObject<EntityType<Bestia>> BESTIA = com.axeliscore.Ejemplos.activos() ? ENTITIES.register("bestia",
            () -> Reg.entity(AxelisCore.MODID, "bestia", EntityType.Builder.<Bestia>of(Bestia::new, MobCategory.CREATURE).sized(0.9F, 1.25F)
                    .clientTrackingRange(10))) : null;
    public static final RegistryObject<EntityType<Alado>> ALADO = com.axeliscore.Ejemplos.activos() ? ENTITIES.register("alado",
            () -> Reg.entity(AxelisCore.MODID, "alado", EntityType.Builder.<Alado>of(Alado::new, MobCategory.CREATURE).sized(0.7F, 0.75F)
                    .clientTrackingRange(10))) : null;
    public static final RegistryObject<EntityType<Gelatina>> GELATINA = com.axeliscore.Ejemplos.activos() ? ENTITIES.register("gelatina",
            () -> Reg.entity(AxelisCore.MODID, "gelatina", EntityType.Builder.<Gelatina>of(Gelatina::new, MobCategory.CREATURE).sized(0.8F, 0.75F)
                    .clientTrackingRange(10))) : null;
    public static final RegistryObject<EntityType<Coloso>> COLOSO = com.axeliscore.Ejemplos.activos() ? ENTITIES.register("coloso",
            () -> Reg.entity(AxelisCore.MODID, "coloso", EntityType.Builder.<Coloso>of(Coloso::new, MobCategory.MONSTER).sized(1.9F, 2.9F)
                    .clientTrackingRange(12))) : null;
    public static final RegistryObject<EntityType<Chispa>> CHISPA = com.axeliscore.Ejemplos.activos() ? ENTITIES.register("chispa",
            () -> Reg.entity(AxelisCore.MODID, "chispa", EntityType.Builder.<Chispa>of(Chispa::new, MobCategory.CREATURE).sized(0.5F, 0.65F)
                    .clientTrackingRange(10))) : null;

    /** NPC con misiones, tienda, amistad y rutina (usa un {@link com.axeliscore.npc.NpcDef}). */
    public static final RegistryObject<EntityType<com.axeliscore.npc.Npc>> NPC = ENTITIES.register("npc",
            () -> Reg.entity(AxelisCore.MODID, "npc", EntityType.Builder.<com.axeliscore.npc.Npc>of(com.axeliscore.npc.Npc::new,
                    MobCategory.CREATURE).sized(0.6F, 1.8F).clientTrackingRange(10)));

    /** Aviso de ataque en el suelo (círculo, anillo, línea, cono, cuadro) que pega al llenarse. */
    public static final RegistryObject<EntityType<com.axeliscore.combat.Aviso>> AVISO = ENTITIES.register("aviso",
            () -> Reg.entity(AxelisCore.MODID, "aviso", EntityType.Builder.<com.axeliscore.combat.Aviso>of(com.axeliscore.combat.Aviso::new,
                    MobCategory.MISC).sized(0.5F, 0.25F).clientTrackingRange(8).updateInterval(1).fireImmune().noSave()));
    /** Proyectil del Core (orbe, objeto o modelo). */
    public static final RegistryObject<EntityType<com.axeliscore.combat.Proyectil>> PROYECTIL = ENTITIES.register("proyectil",
            () -> Reg.entity(AxelisCore.MODID, "proyectil", EntityType.Builder.<com.axeliscore.combat.Proyectil>of(
                    com.axeliscore.combat.Proyectil::new, MobCategory.MISC).sized(0.4F, 0.4F).clientTrackingRange(8).updateInterval(1).fireImmune()
                    .noSave()));

    private CoreEntities() {
    }

    @SubscribeEvent
    public static void onAttributes(EntityAttributeCreationEvent e) {
        if (com.axeliscore.Ejemplos.activos()) {
            e.put(PERSONA.get(), Persona.createAttributes().build());
            e.put(BESTIA.get(), Bestia.createAttributes().build());
            e.put(ALADO.get(), Alado.createAttributes().build());
            e.put(GELATINA.get(), Gelatina.createAttributes().build());
            e.put(COLOSO.get(), Coloso.createAttributes().build());
            e.put(CHISPA.get(), Chispa.createAttributes().build());
        }
        e.put(NPC.get(), Persona.createAttributes().build());
    }
}
