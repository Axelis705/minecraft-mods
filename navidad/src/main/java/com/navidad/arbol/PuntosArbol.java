package com.navidad.arbol;

/**
 * Dónde se cuelgan los adornos del Pino de Navidad (lo escribe {@code herramientas/arbol.py} con las mismas cuentas del dibujo; no
 * se edita a mano). Cada lugar: x, y, z en bloques desde el centro del piso del árbol, y el piso de ramas (0 = el de abajo).
 */
public final class PuntosArbol {
    public static final float[][] LUGARES = {
            {0.0177F, 0.3044F, 0.5926F, 0},
            {-0.3751F, 0.2287F, 0.4842F, 0},
            {-0.5263F, 0.2652F, 0.1078F, 0},
            {-0.4970F, 0.2177F, -0.1715F, 0},
            {-0.2297F, 0.2724F, -0.4607F, 0},
            {0.1454F, 0.2572F, -0.3969F, 0},
            {0.4653F, 0.2360F, -0.1777F, 0},
            {0.5469F, 0.2405F, 0.1364F, 0},
            {0.3653F, 0.2452F, 0.4398F, 0},
            {-0.1852F, 0.6891F, 0.4493F, 1},
            {-0.4174F, 0.7248F, 0.2600F, 1},
            {-0.4675F, 0.6915F, -0.1337F, 1},
            {-0.1777F, 0.7318F, -0.3444F, 1},
            {0.1765F, 0.6987F, -0.3378F, 1},
            {0.3557F, 0.6968F, -0.1427F, 1},
            {0.4683F, 0.6964F, 0.2210F, 1},
            {0.1670F, 0.7015F, 0.4558F, 1},
            {-0.0674F, 1.1287F, 0.4369F, 2},
            {-0.3114F, 1.1475F, 0.2356F, 2},
            {-0.3836F, 1.1203F, -0.0770F, 2},
            {-0.1406F, 1.1145F, -0.2806F, 2},
            {0.2028F, 1.1298F, -0.2292F, 2},
            {0.3952F, 1.1056F, 0.0575F, 2},
            {0.2597F, 1.1246F, 0.3736F, 2},
            {-0.1196F, 1.4999F, 0.2866F, 3},
            {-0.2932F, 1.5061F, 0.0827F, 3},
            {-0.1407F, 1.5010F, -0.1609F, 3},
            {0.1597F, 1.4878F, -0.1837F, 3},
            {0.3005F, 1.4916F, 0.0745F, 3},
            {0.1482F, 1.4950F, 0.3202F, 3},
            {-0.0346F, 1.8254F, 0.2329F, 4},
            {-0.1730F, 1.8297F, 0.0742F, 4},
            {-0.0848F, 1.8235F, -0.0858F, 4},
            {0.1290F, 1.8232F, -0.0685F, 4},
            {0.1428F, 1.8219F, 0.1267F, 4}
    };
    /** La punta del árbol (donde va la estrella o el ángel). */
    public static final float[] PUNTA = {0.0000F, 2.4385F, 0.0000F};

    private PuntosArbol() {
    }
}
