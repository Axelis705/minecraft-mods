package com.axeliscore.ui;

import net.minecraft.network.chat.Component;

/** Rarezas comunes a todos los mods del Core (color y nombre). Los índices se guardan en datos: solo agregar al final. */
public enum Rareza {
    COMUN(0xFFD6D9E0, 0xFF8C93A3),
    POCO_COMUN(0xFF7CE38B, 0xFF2F9E4A),
    RARO(0xFF6FB7FF, 0xFF2F6FD6),
    EPICO(0xFFC58CFF, 0xFF8240D6),
    LEGENDARIO(0xFFFFC14D, 0xFFE07A10),
    MITICO(0xFFFF7AC8, 0xFFE0307F);

    /** Color claro (texto, brillo) y oscuro (borde, degradado). */
    public final int light, dark;

    Rareza(int light, int dark) {
        this.light = light;
        this.dark = dark;
    }

    public Component nombre() {
        return Component.translatable("axelis_core.rareza." + name().toLowerCase(java.util.Locale.ROOT));
    }

    public static Rareza of(int i) {
        Rareza[] v = values();
        return v[Math.max(0, Math.min(v.length - 1, i))];
    }
}
