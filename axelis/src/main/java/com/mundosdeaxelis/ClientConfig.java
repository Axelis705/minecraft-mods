package com.mundosdeaxelis;

import net.minecraftforge.common.ForgeConfigSpec;

/** Ajustes del jugador que se guardan en su propio equipo (config/mundos_de_axelis-client.toml). */
public final class ClientConfig {
    public static final ForgeConfigSpec SPEC;
    /** Volumen de los sonidos de las cajas (ruleta, cerradura y premio), de 0 a 100 %. */
    public static final ForgeConfigSpec.IntValue CRATE_VOLUME;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();
        b.push("sonidos");
        CRATE_VOLUME = b.comment("Volumen de los sonidos de las cajas (0-100 %)").defineInRange("crate_volume", 100, 0, 100);
        b.pop();
        SPEC = b.build();
    }

    private ClientConfig() {
    }
}
