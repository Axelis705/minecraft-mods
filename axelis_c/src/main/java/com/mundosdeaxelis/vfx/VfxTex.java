package com.mundosdeaxelis.vfx;

/** Texturas de efectos. El renderizador las busca dentro de assets/mundos_de_axelis/textures/vfx. */
public final class VfxTex {
    private VfxTex() {
    }

    public static final int RING = 0, RUNE_A = 1, RUNE_B = 2, COL = 3, LINE = 4, GLOW_LINE = 5, SLASH = 6, FLARE = 7, ORB = 8,
            DOME = 9, SWIRL = 10, TREE = 11, BANNER = 12, DRAGON = 13, FAIRY = 14, ZZZ = 15, COIN = 16, FANG = 17,
            RIB_FLAME = 18, RIB_VOID = 19, STARBURST = 20, HALO = 21, SOFT = 22, HEART = 23, DRAGON3D = 24, CREATURE3D = 25;

    public static final int COUNT = 26;

    /** Nombre del archivo (sin .png). */
    public static final String[] FILE = {
            "ring", "rune_a", "rune_b", "column", "line", "glow_line", "slash", "flare", "orb",
            "dome", "swirl", "tree", "banner", "dragon", "fairy", "zzz", "coin", "fang",
            "ribbon_flame", "ribbon_void", "starburst", "halo", "soft", "heart", "dragon3d", "creature3d"};

    /** Cuadros de animación puestos en tira vertical. */
    public static final int[] FRAMES = {1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 8, 4, 3, 2, 1, 1, 1, 1, 1, 1, 1, 1, 1};

    /** Ancho / alto de cada cuadro (para sprites). */
    public static final float[] ASPECT = {1, 1, 1, 1, 4, 4, 2, 1, 1, 1, 2, 0.667f, 0.5f, 1.25f, 1, 1, 1, 0.5f, 4, 4, 1, 1, 1, 1, 1, 1};

    /** Ticks que dura cada cuadro. */
    public static final int[] FRAME_TICKS = {1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2, 2, 8, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1};
}
