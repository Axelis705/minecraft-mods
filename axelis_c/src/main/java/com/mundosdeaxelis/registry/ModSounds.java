package com.mundosdeaxelis.registry;

import com.mundosdeaxelis.MundosDeAxelis;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** Sonidos propios del mod (los archivos .ogg están en assets/mundos_de_axelis/sounds). */
public final class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, MundosDeAxelis.MODID);

    /** Sonidos de las interfaces (pantallas del mod). */
    public static final RegistryObject<SoundEvent> UI_OPEN = register("ui_open");
    public static final RegistryObject<SoundEvent> UI_HOVER = register("ui_hover");
    public static final RegistryObject<SoundEvent> UI_CLICK = register("ui_click");
    public static final RegistryObject<SoundEvent> UI_TAB = register("ui_tab");
    public static final RegistryObject<SoundEvent> UI_SUCCESS = register("ui_success");
    public static final RegistryObject<SoundEvent> UI_ERROR = register("ui_error");
    public static final RegistryObject<SoundEvent> UI_FORGE = register("ui_forge");
    public static final RegistryObject<SoundEvent> KATANA_SLASH = register("katana_slash");


    /** Rey Brote, sus mobs (Brote, Espinoso, Guardián de Musgo) y la dungeon del bosque (etapa 166). */
    public static final RegistryObject<SoundEvent> REY_BROTE_AMBIENT = register("rey_brote_ambient");
    public static final RegistryObject<SoundEvent> REY_BROTE_HURT = register("rey_brote_hurt");
    public static final RegistryObject<SoundEvent> REY_BROTE_DEATH = register("rey_brote_death");
    public static final RegistryObject<SoundEvent> REY_BROTE_CHARGE = register("rey_brote_charge");
    public static final RegistryObject<SoundEvent> REY_BROTE_SLAM = register("rey_brote_slam");
    public static final RegistryObject<SoundEvent> REY_BROTE_SPIT = register("rey_brote_spit");
    public static final RegistryObject<SoundEvent> REY_BROTE_ROAR = register("rey_brote_roar");
    public static final RegistryObject<SoundEvent> REY_BROTE_ABSORB = register("rey_brote_absorb");
    public static final RegistryObject<SoundEvent> BROTE_AMBIENT = register("brote_ambient");
    public static final RegistryObject<SoundEvent> BROTE_HURT = register("brote_hurt");
    public static final RegistryObject<SoundEvent> BROTE_DEATH = register("brote_death");
    public static final RegistryObject<SoundEvent> BROTE_ATTACK = register("brote_attack");
    public static final RegistryObject<SoundEvent> ESPINOSO_SHOOT = register("espinoso_shoot");
    public static final RegistryObject<SoundEvent> ESPINOSO_CHARGE = register("espinoso_charge");
    public static final RegistryObject<SoundEvent> ESPINOSO_HURT = register("espinoso_hurt");
    public static final RegistryObject<SoundEvent> ESPINOSO_DEATH = register("espinoso_death");
    public static final RegistryObject<SoundEvent> GUARDIAN_STEP = register("guardian_step");
    public static final RegistryObject<SoundEvent> GUARDIAN_HURT = register("guardian_hurt");
    public static final RegistryObject<SoundEvent> GUARDIAN_DEATH = register("guardian_death");
    public static final RegistryObject<SoundEvent> GUARDIAN_SLAM = register("guardian_slam");
    public static final RegistryObject<SoundEvent> GUARDIAN_ROAR = register("guardian_roar");
    public static final RegistryObject<SoundEvent> GUARDIAN_CHARGE = register("guardian_charge");
    public static final RegistryObject<SoundEvent> DUNGEON_DOOR_CLOSE = register("dungeon_door_close");
    public static final RegistryObject<SoundEvent> DUNGEON_DOOR_OPEN = register("dungeon_door_open");
    public static final RegistryObject<SoundEvent> DUNGEON_ROOM_START = register("dungeon_room_start");
    public static final RegistryObject<SoundEvent> DUNGEON_ROOM_CLEAR = register("dungeon_room_clear");
    public static final RegistryObject<SoundEvent> DUNGEON_BOSS_GATE = register("dungeon_boss_gate");
    public static final RegistryObject<SoundEvent> DUNGEON_WAVE = register("dungeon_wave");
    public static final RegistryObject<SoundEvent> RAIZAL_HURT = register("raizal_hurt");
    public static final RegistryObject<SoundEvent> RAIZAL_DEATH = register("raizal_death");
    public static final RegistryObject<SoundEvent> RAIZAL_ROOTS = register("raizal_roots");
    public static final RegistryObject<SoundEvent> RAIZAL_STEP = register("raizal_step");
    public static final RegistryObject<SoundEvent> POLINIZADOR_BUZZ = register("polinizador_buzz");
    public static final RegistryObject<SoundEvent> POLINIZADOR_HURT = register("polinizador_hurt");
    public static final RegistryObject<SoundEvent> POLINIZADOR_DEATH = register("polinizador_death");
    public static final RegistryObject<SoundEvent> POLINIZADOR_SPORE = register("polinizador_spore");
    public static final RegistryObject<SoundEvent> RUNE_0 = register("rune_0");
    public static final RegistryObject<SoundEvent> RUNE_1 = register("rune_1");
    public static final RegistryObject<SoundEvent> RUNE_2 = register("rune_2");
    public static final RegistryObject<SoundEvent> RUNE_3 = register("rune_3");
    public static final RegistryObject<SoundEvent> RUNE_FAIL = register("rune_fail");
    public static final RegistryObject<SoundEvent> RUNE_OK = register("rune_ok");
    public static final RegistryObject<SoundEvent> VAULT_OPEN = register("vault_open");
    public static final RegistryObject<SoundEvent> KEY_USE = register("key_use");
    public static final RegistryObject<SoundEvent> CRUMBLE = register("crumble");
    public static final RegistryObject<SoundEvent> KEY_GET = register("key_get");
    public static final RegistryObject<SoundEvent> CRATE_BONUS_KEY = register("crate_bonus_key");
    public static final RegistryObject<SoundEvent> LOCK_PIN = register("lock_pin");
    public static final RegistryObject<SoundEvent> LOCK_MISS = register("lock_miss");
    public static final RegistryObject<SoundEvent> LOCK_FAKE = register("lock_fake");
    public static final RegistryObject<SoundEvent> LOCK_TURN = register("lock_turn");
    public static final RegistryObject<SoundEvent> LOCK_OPEN = register("lock_open");
    public static final RegistryObject<SoundEvent> MUSIC_FOREST_DUNGEON = register("music_forest_dungeon");
    public static final RegistryObject<SoundEvent> MUSIC_FOREST_BOSS = register("music_forest_boss");
    // ---- Forja (etapa 2)
    public static final RegistryObject<SoundEvent> HERRERO_AMBIENT = register("herrero_ambient");
    public static final RegistryObject<SoundEvent> HERRERO_HURT = register("herrero_hurt");
    public static final RegistryObject<SoundEvent> HERRERO_DEATH = register("herrero_death");
    public static final RegistryObject<SoundEvent> HERRERO_CHARGE = register("herrero_charge");
    public static final RegistryObject<SoundEvent> HERRERO_SLAM = register("herrero_slam");
    public static final RegistryObject<SoundEvent> HERRERO_STEP = register("herrero_step");
    public static final RegistryObject<SoundEvent> HERRERO_SWING = register("herrero_swing");
    public static final RegistryObject<SoundEvent> HERRERO_LAND = register("herrero_land");
    public static final RegistryObject<SoundEvent> HERRERO_BURST = register("herrero_burst");
    public static final RegistryObject<SoundEvent> HERRERO_CLINK = register("herrero_clink");
    public static final RegistryObject<SoundEvent> HERRERO_BLAST = register("herrero_blast");
    public static final RegistryObject<SoundEvent> ESCORIA_STEP = register("escoria_step");
    public static final RegistryObject<SoundEvent> CHISPERO_AMBIENT = register("chispero_ambient");
    public static final RegistryObject<SoundEvent> FUNDIDOR_AMBIENT = register("fundidor_ambient");
    public static final RegistryObject<SoundEvent> CAPATAZ_AMBIENT = register("capataz_ambient");
    public static final RegistryObject<SoundEvent> CAPATAZ_BURST = register("capataz_burst");
    public static final RegistryObject<SoundEvent> FORGE_DOOR_OPEN = register("forge_door_open");
    public static final RegistryObject<SoundEvent> FORGE_DOOR_CLOSE = register("forge_door_close");
    public static final RegistryObject<SoundEvent> FORGE_ROOM_START = register("forge_room_start");
    public static final RegistryObject<SoundEvent> FORGE_ROOM_CLEAR = register("forge_room_clear");
    public static final RegistryObject<SoundEvent> FORGE_WAVE = register("forge_wave");
    public static final RegistryObject<SoundEvent> FORGE_BOSS_GATE = register("forge_boss_gate");
    public static final RegistryObject<SoundEvent> BOSS_RUMBLE = register("boss_rumble");
    public static final RegistryObject<SoundEvent> BOSS_RISER = register("boss_riser");
    public static final RegistryObject<SoundEvent> BOSS_IMPACT = register("boss_impact");
    public static final RegistryObject<SoundEvent> BOSS_TITLE_FORGE = register("boss_title_forge");
    public static final RegistryObject<SoundEvent> BOSS_TITLE_FOREST = register("boss_title_forest");
    public static final RegistryObject<SoundEvent> BOSS_ROAR_FOREST = register("boss_roar_forest");
    public static final RegistryObject<SoundEvent> BOSS_SHATTER = register("boss_shatter");
    public static final RegistryObject<SoundEvent> BOSS_PHASE = register("boss_phase");
    public static final RegistryObject<SoundEvent> RIVET_USE = register("rivet_use");
    public static final RegistryObject<SoundEvent> HERRERO_SPARKS = register("herrero_sparks");
    public static final RegistryObject<SoundEvent> HERRERO_ROAR = register("herrero_roar");
    public static final RegistryObject<SoundEvent> HERRERO_ANVIL = register("herrero_anvil");
    public static final RegistryObject<SoundEvent> HERRERO_TEMPER = register("herrero_temper");
    public static final RegistryObject<SoundEvent> ESCORIA_AMBIENT = register("escoria_ambient");
    public static final RegistryObject<SoundEvent> ESCORIA_HURT = register("escoria_hurt");
    public static final RegistryObject<SoundEvent> ESCORIA_DEATH = register("escoria_death");
    public static final RegistryObject<SoundEvent> ESCORIA_ATTACK = register("escoria_attack");
    public static final RegistryObject<SoundEvent> CHISPERO_CHARGE = register("chispero_charge");
    public static final RegistryObject<SoundEvent> CHISPERO_SHOOT = register("chispero_shoot");
    public static final RegistryObject<SoundEvent> CHISPERO_HURT = register("chispero_hurt");
    public static final RegistryObject<SoundEvent> CHISPERO_DEATH = register("chispero_death");
    public static final RegistryObject<SoundEvent> ASCUA_BUZZ = register("ascua_buzz");
    public static final RegistryObject<SoundEvent> ASCUA_HURT = register("ascua_hurt");
    public static final RegistryObject<SoundEvent> ASCUA_DEATH = register("ascua_death");
    public static final RegistryObject<SoundEvent> ASCUA_SHOOT = register("ascua_shoot");
    public static final RegistryObject<SoundEvent> FUNDIDOR_STEP = register("fundidor_step");
    public static final RegistryObject<SoundEvent> FUNDIDOR_HURT = register("fundidor_hurt");
    public static final RegistryObject<SoundEvent> FUNDIDOR_DEATH = register("fundidor_death");
    public static final RegistryObject<SoundEvent> FUNDIDOR_ERUPT = register("fundidor_erupt");
    public static final RegistryObject<SoundEvent> CAPATAZ_STEP = register("capataz_step");
    public static final RegistryObject<SoundEvent> CAPATAZ_HURT = register("capataz_hurt");
    public static final RegistryObject<SoundEvent> CAPATAZ_DEATH = register("capataz_death");
    public static final RegistryObject<SoundEvent> CAPATAZ_SLAM = register("capataz_slam");
    public static final RegistryObject<SoundEvent> CAPATAZ_ROAR = register("capataz_roar");
    public static final RegistryObject<SoundEvent> CAPATAZ_CHARGE = register("capataz_charge");
    public static final RegistryObject<SoundEvent> MUSIC_FORGE_DUNGEON = register("music_forge_dungeon");
    public static final RegistryObject<SoundEvent> MUSIC_FORGE_BOSS = register("music_forge_boss");

    /** Sonidos de las reliquias. */
    public static final RegistryObject<SoundEvent> RELIC_ACTIVATE = register("relic_activate");
    public static final RegistryObject<SoundEvent> RELIC_READY = register("relic_ready");
    public static final RegistryObject<SoundEvent> RELIC_BOOM = register("relic_boom");
    public static final RegistryObject<SoundEvent> RELIC_WHOOSH = register("relic_whoosh");
    public static final RegistryObject<SoundEvent> RELIC_HEAL = register("relic_heal");
    public static final RegistryObject<SoundEvent> RELIC_MAGIC = register("relic_magic");
    public static final RegistryObject<SoundEvent> RELIC_FIRE = register("relic_fire");
    public static final RegistryObject<SoundEvent> RELIC_ICE = register("relic_ice");
    public static final RegistryObject<SoundEvent> RELIC_DARK = register("relic_dark");
    public static final RegistryObject<SoundEvent> RELIC_REVIVE = register("relic_revive");

    /** Crates: tic de la ruleta, apertura de la cerradura, giro y un sonido de premio por cada rareza. */
    public static final RegistryObject<SoundEvent> CRATE_TICK = register("crate_tick");
    public static final RegistryObject<SoundEvent> CRATE_UNLOCK = register("crate_unlock");
    public static final RegistryObject<SoundEvent> CRATE_SPIN = register("crate_spin");
    public static final java.util.List<RegistryObject<SoundEvent>> CRATE_REVEAL = registerReveal();

    private static java.util.List<RegistryObject<SoundEvent>> registerReveal() {
        java.util.List<RegistryObject<SoundEvent>> l = new java.util.ArrayList<>();
        for (int i = 0; i < 8; i++) {
            l.add(register("crate_reveal_" + i));
        }
        return l;
    }

    /** Sonido al subir de nivel una reliquia. */
    public static final RegistryObject<SoundEvent> RELIC_LEVELUP = register("relic_levelup");

    /** Un sonido por cada "reliquia.tipo" (ej. "star_bow.charge"). */
    public static final java.util.Map<String, RegistryObject<SoundEvent>> RELIC_KEYED = registerKeyed();

    private static java.util.Map<String, RegistryObject<SoundEvent>> registerKeyed() {
        java.util.Map<String, RegistryObject<SoundEvent>> map = new java.util.HashMap<>();
        for (String key : RelicSoundKeys.KEYS) {
            map.put(key, register("relic_" + key.replace('.', '_')));
        }
        return map;
    }

    private ModSounds() {
    }

    private static RegistryObject<SoundEvent> register(String name) {
        return SOUNDS.register(name,
                () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(MundosDeAxelis.MODID, name)));
    }

    /** Lobo Ártico (jefe de la etapa 3) y sus lobeznos. */
    public static final RegistryObject<SoundEvent> LOBO_AMBIENT = register("lobo_ambient");
    public static final RegistryObject<SoundEvent> LOBO_HURT = register("lobo_hurt");
    public static final RegistryObject<SoundEvent> LOBO_DEATH = register("lobo_death");
    public static final RegistryObject<SoundEvent> LOBO_HOWL = register("lobo_howl");
    public static final RegistryObject<SoundEvent> LOBO_ROAR = register("lobo_roar");
    public static final RegistryObject<SoundEvent> LOBO_STEP = register("lobo_step");
    public static final RegistryObject<SoundEvent> LOBO_CHARGE = register("lobo_charge");
    public static final RegistryObject<SoundEvent> LOBO_DASH = register("lobo_dash");
    public static final RegistryObject<SoundEvent> LOBO_SLASH = register("lobo_slash");
    public static final RegistryObject<SoundEvent> LOBO_CROSS = register("lobo_cross");
    public static final RegistryObject<SoundEvent> LOBO_CRASH = register("lobo_crash");
    public static final RegistryObject<SoundEvent> LOBO_WIND = register("lobo_wind");
    public static final RegistryObject<SoundEvent> LOBO_MOON = register("lobo_moon");
    public static final RegistryObject<SoundEvent> LOBO_FREEZE = register("lobo_freeze");
    public static final RegistryObject<SoundEvent> LOBO_PUP_AMBIENT = register("lobo_pup_ambient");
    public static final RegistryObject<SoundEvent> LOBO_PUP_HURT = register("lobo_pup_hurt");
    public static final RegistryObject<SoundEvent> LOBO_PUP_DEATH = register("lobo_pup_death");
    public static final RegistryObject<SoundEvent> LOBO_PUP_BITE = register("lobo_pup_bite");

    /** Las cuatro notas de las runas de la Prueba de runas. */
    public static RegistryObject<SoundEvent> rune(int i) {
        return switch (Math.floorMod(i, 4)) {
            case 0 -> RUNE_0;
            case 1 -> RUNE_1;
            case 2 -> RUNE_2;
            default -> RUNE_3;
        };
    }
}
