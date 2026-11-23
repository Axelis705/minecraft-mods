package com.axeliscore;

/**
 * Los ejemplos del Core (Mara e Iker, la Persona de muestra, las criaturas, el Coloso, la galería y los comandos que los usan) son
 * solo para probar la librería: en los mods que usan el Core no aparecen. Se activan solos en el entorno de desarrollo (runClient,
 * runServer), en las pruebas automáticas ({@code -Daxcore.autotest=...}) o a mano con {@code -Daxcore.ejemplos=true} (en el
 * servidor y en el cliente a la vez: si solo uno los tiene, no se pueden conectar).
 */
public final class Ejemplos {
    private static final boolean ACTIVOS = calcular();

    private Ejemplos() {
    }

    private static boolean calcular() {
        if (Boolean.getBoolean("axcore.ejemplos")) {
            return true;
        }
        if (!System.getProperty("axcore.autotest", "").isEmpty()) {
            return true;
        }
        try {
            return !net.minecraftforge.fml.loading.FMLEnvironment.production;
        } catch (Throwable e) {
            return false;
        }
    }

    /** ¿Están los ejemplos? */
    public static boolean activos() {
        return ACTIVOS;
    }

    /** Mensaje para los comandos de ejemplo cuando no están. */
    public static int noEstan(net.minecraft.commands.CommandSourceStack s) {
        s.sendFailure(net.minecraft.network.chat.Component.literal(
                "Los ejemplos del Core solo están en pruebas (inicia con -Daxcore.ejemplos=true en el servidor y en el cliente)"));
        return 0;
    }
}
