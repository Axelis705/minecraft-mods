package com.mundosdeaxelis;

import net.minecraftforge.common.ForgeConfigSpec;

/** Ajustes del jugador que se guardan en su propio equipo (config/mundos_de_axelis-client.toml). */
public final class ClientConfig {
    public static final ForgeConfigSpec SPEC;
    /** Volumen de los sonidos de las cajas (ruleta, cerradura y premio), de 0 a 100 %. */
    public static final ForgeConfigSpec.IntValue CRATE_VOLUME;
    /** Barra de almas a la derecha de la pantalla. */
    public static final ForgeConfigSpec.BooleanValue SHOW_SOULS;
    /** Mapa de la dungeon (arriba a la izquierda). */
    public static final ForgeConfigSpec.BooleanValue SHOW_MAP;
    /** Avisos en pantalla: false = todos, true = pocos (uno a la vez y más cortos). */
    public static final ForgeConfigSpec.BooleanValue FEW_NOTICES;
    /** Música propia de las dungeons y jefes. */
    public static final ForgeConfigSpec.BooleanValue DUNGEON_MUSIC;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();
        b.push("sonidos");
        CRATE_VOLUME = b.comment("Volumen de los sonidos de las cajas (0-100 %)").defineInRange("crate_volume", 100, 0, 100);
        b.pop();
        b.push("pantalla");
        SHOW_SOULS = b.comment("Mostrar la barra de almas a la derecha").define("show_souls", true);
        SHOW_MAP = b.comment("Mostrar el mapa de la dungeon").define("show_map", true);
        FEW_NOTICES = b.comment("Pocos avisos: solo uno a la vez y más cortos").define("few_notices", false);
        DUNGEON_MUSIC = b.comment("Música de las dungeons y jefes").define("dungeon_music", true);
        b.pop();
        SPEC = b.build();
    }

    private ClientConfig() {
    }
}
