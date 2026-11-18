package com.mundosdeaxelis.vfx;

/** Tipos de efecto visual y banderas. Los números se mandan por red: no cambiar el orden. */
public final class VfxType {
    private VfxType() {
    }

    public static final int RING = 0;      // anillo plano que se expande en el suelo
    public static final int RUNE = 1;      // círculo rúnico que gira
    public static final int COLUMN = 2;    // columna de luz
    public static final int BEAM = 3;      // rayo / hilo entre dos puntos
    public static final int BOLT = 4;      // relámpago irregular
    public static final int SLASH = 5;     // media luna de corte que avanza
    public static final int FLASH = 6;     // destello (billboard)
    public static final int ORB = 7;       // orbe de luz, con estela opcional
    public static final int DOME = 8;      // cúpula o esfera translúcida
    public static final int FUNNEL = 9;    // tornado
    public static final int SPRITE = 10;   // sprite animado que mira a la cámara
    public static final int CROSS = 11;    // sprite en planos cruzados (árbol, estandarte)
    public static final int MAW = 12;      // mandíbulas que salen del suelo
    public static final int RIBBON = 13;   // cinta ondulante (látigo, tentáculos)
    public static final int COIN = 14;     // moneda que gira y cae

    public static final int ACT_ADD = 0;
    public static final int ACT_UPDATE = 1;
    public static final int ACT_REMOVE = 2;

    public static final int F_TRAIL = 1;
    public static final int F_NORMAL = 2;     // usa dir como normal del plano
    public static final int F_HEMI = 4;       // media esfera
    public static final int F_CURVE = 8;      // el rayo se comba (hilo)
    public static final int F_CAPS = 16;      // destellos en los extremos
    public static final int F_FLICKER = 32;   // parpadeo
    public static final int F_ADD = 64;       // sprite con mezcla aditiva
    public static final int F_GROW = 128;     // la cinta crece desde el origen

    public static int style(int flags) {
        return (flags >> 8) & 15;
    }

    public static int withStyle(int flags, int style) {
        return (flags & ~(15 << 8)) | ((style & 15) << 8);
    }
}
