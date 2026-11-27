package com.navidad.arbol;

/**
 * Dónde se cuelgan los adornos del Pino de Navidad (lo escribe {@code herramientas/arbol.py} con las mismas cuentas del dibujo; no
 * se edita a mano). Cada lugar: x, y, z en bloques desde el centro del piso del árbol, y el piso de ramas (0 = el de abajo).
 */
public final class PuntosArbol {
    public static final float[][] LUGARES = {
            {-0.2487F, 0.6197F, 0.5818F, 1},
            {-0.5547F, 0.6680F, 0.3250F, 1},
            {-0.6064F, 0.6314F, -0.1920F, 1},
            {-0.2384F, 0.6749F, -0.4835F, 1},
            {0.2363F, 0.6326F, -0.4736F, 1},
            {0.4828F, 0.6270F, -0.2160F, 1},
            {0.6109F, 0.6361F, 0.2692F, 1},
            {0.2249F, 0.6349F, 0.5922F, 1},
            {-0.0945F, 1.0693F, 0.5876F, 2},
            {-0.4461F, 1.0913F, 0.3105F, 2},
            {-0.5277F, 1.0617F, -0.1294F, 2},
            {-0.1979F, 1.0495F, -0.4202F, 2},
            {0.2896F, 1.0675F, -0.3540F, 2},
            {0.5463F, 1.0411F, 0.0556F, 2},
            {0.3581F, 1.0669F, 0.4915F, 2},
            {-0.1916F, 1.4366F, 0.4216F, 3},
            {-0.4491F, 1.4521F, 0.0934F, 3},
            {-0.2226F, 1.4400F, -0.2911F, 3},
            {0.2433F, 1.4260F, -0.3126F, 3},
            {0.4549F, 1.4324F, 0.0806F, 3},
            {0.2253F, 1.4368F, 0.4544F, 3},
            {-0.0659F, 1.7719F, 0.3870F, 4},
            {-0.3308F, 1.7791F, 0.0849F, 4},
            {-0.1626F, 1.7679F, -0.2217F, 4},
            {0.2395F, 1.7703F, -0.1808F, 4},
            {0.2842F, 1.7616F, 0.1903F, 4}
    };
    /** La punta del árbol (donde va la estrella o el ángel). */
    public static final float[] PUNTA = {0.0000F, 2.4385F, 0.0000F};

    private PuntosArbol() {
    }
}
