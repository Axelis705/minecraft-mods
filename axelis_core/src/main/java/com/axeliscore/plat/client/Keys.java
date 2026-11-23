package com.axeliscore.plat.client;

/**
 * Desde 26.3 Minecraft usa SDL para el teclado y las pantallas reciben códigos de tecla de SDL (scancodes). Todo el mod compara con los
 * códigos de siempre (GLFW: 256 = Esc, 257 = Enter, 65 = A...), así que aquí se traducen una sola vez al recibir la tecla.
 */
public final class Keys {
    private static final int[] SDL_TO_GLFW = new int[256];

    static {
        java.util.Arrays.fill(SDL_TO_GLFW, -1);
        for (int i = 0; i < 26; i++) {
            SDL_TO_GLFW[4 + i] = 65 + i; // A-Z
        }
        for (int i = 0; i < 9; i++) {
            SDL_TO_GLFW[30 + i] = 49 + i; // 1-9
            SDL_TO_GLFW[89 + i] = 321 + i; // teclado numérico 1-9
        }
        SDL_TO_GLFW[39] = 48; // 0
        SDL_TO_GLFW[98] = 320; // numérico 0
        int[][] pairs = {
                {40, 257}, {41, 256}, {42, 259}, {43, 258}, {44, 32}, {45, 45}, {46, 61}, {47, 91}, {48, 93}, {49, 92}, {51, 59},
                {52, 39}, {53, 96}, {54, 44}, {55, 46}, {56, 47}, {57, 280}, {70, 283}, {71, 281}, {72, 284}, {73, 260}, {74, 268},
                {75, 266}, {76, 261}, {77, 269}, {78, 267}, {79, 262}, {80, 263}, {81, 264}, {82, 265}, {83, 282}, {84, 331}, {85, 332},
                {86, 333}, {87, 334}, {88, 335}, {99, 330}, {103, 336}, {224, 341}, {225, 340}, {226, 342}, {227, 343}, {228, 345},
                {229, 344}, {230, 346}, {231, 347}};
        for (int[] p : pairs) {
            SDL_TO_GLFW[p[0]] = p[1];
        }
        for (int i = 0; i < 12; i++) {
            SDL_TO_GLFW[58 + i] = 290 + i; // F1-F12
            SDL_TO_GLFW[104 + i] = 302 + i; // F13-F24
        }
        SDL_TO_GLFW[220] = 330; // coma del teclado numérico
    }

    private Keys() {
    }

    /** Código de SDL (scancode) a código GLFW; si no se conoce, -1. */
    public static int glfw(int sdl) {
        return sdl >= 0 && sdl < SDL_TO_GLFW.length ? SDL_TO_GLFW[sdl] : -1;
    }
}
