package com.coleccionista.mapa;

/**
 * La sala grande: del otro lado del portón del patio. Un vestíbulo de hotel viejo del Coleccionista (mármol, terciopelo, arañas de velas)
 * con diez ascensores de equipo en las paredes, cinco al norte y cinco al sur, y una pantalla gigante al fondo. Aquí se juntan todos;
 * el Coleccionista pide equipos de 2 o de 3 y cada equipo se sube a un ascensor.
 * <pre>
 *   x: 0 ... 58 (la pared del oeste es la del patio, con el portón; la pantalla al este en x 57)
 *   z: -5 ... 39 (la sala en z 0..34; los ascensores del norte en z -4..-2 y los del sur en z 36..38)
 * </pre>
 * Marcas: {@code sala.centro}, {@code sala.entrada}, {@code sala.pantalla}, {@code sala.asc.N.luz} (el foco de arriba de cada puerta) y las
 * de cada ascensor ({@code cab.sala_N.*}, ver {@link Cabinas#ascensor}).
 */
public final class SalaGrande {
    public static final int ASCENSORES = 10;
    private static final String C = "coleccionista:";
    private static final int X1 = 56, Z1 = 34, TECHO = 13;

    private SalaGrande() {
    }

    /** El marco del ascensor N (1..5 al norte, 6..10 al sur, de oeste a este). */
    public static Marco marco(int n) {
        int j = (n - 1) % 5, a = 12 + 9 * j;
        return n <= 5 ? new Marco(a + Cabinas.EQ_ANCHO, -1, 180) : new Marco(a, Z1 + 2, 0);
    }

    public static Plano crear(long semilla) {
        Plano p = new Plano(semilla);
        // casco macizo (sin la pared del oeste, que es la del patio) y el hueco de la sala
        p.caja(0, 0, -6, X1 + 2, TECHO, Z1 + 1, C + "marmol_negro");
        // detrás de la pared sur solo donde están los ascensores (al oeste está la Capilla)
        p.caja(10, 0, Z1 + 2, X1 + 2, TECHO, Z1 + 5, C + "marmol_negro");
        p.caja(0, 1, 0, X1, TECHO - 1, Z1, "air");
        // piso de mármol en tablero, con el tapete rojo del portón a la pantalla
        for (int x = 0; x <= X1; x++) {
            for (int z = 0; z <= Z1; z++) {
                p.set(x, 0, z, C + ((x / 2 + z / 2) % 2 == 0 ? "marmol_negro" : "marmol_crema"));
            }
            for (int z = 16; z <= 20; z++) {
                p.set(x, 1, z, C + (z == 16 || z == 20 ? "tapete_orilla" : "tapete_rojo"));
            }
        }
        // las paredes: madera pulida abajo, papel tapiz arriba, con moldura dorada
        for (int y = 1; y < TECHO; y++) {
            String b = y <= 2 ? C + "madera_pulida" : y == 3 ? C + "marco_dorado" : C + "papel_tapiz";
            for (int x = 1; x <= X1; x++) {
                p.set(x, y, -1, b);
                p.set(x, y, Z1 + 1, b);
            }
            for (int z = 0; z <= Z1; z++) {
                p.set(X1 + 1, y, z, b);
            }
        }
        for (int n = 1; n <= ASCENSORES; n++) {
            ascensor(p, n);
        }
        pantalla(p);
        lamparas(p);
        muebles(p);
        p.marca("sala.centro", 30, 1, 18, 270);
        p.marca("sala.entrada", 2, 1, 18, 270);
        return p;
    }

    private static void ascensor(Plano p, int n) {
        Marco m = marco(n);
        Cabinas.ascensor(p, "sala_" + n, m);
        // por fuera: marco de latón alrededor de la puerta, el número y el foco que se prende cuando el equipo está listo
        for (int y = 1; y <= 3; y++) {
            m.set(p, 0, y, -1, C + "marco_dorado");
            m.set(p, 3, y, -1, C + "marco_dorado");
        }
        m.set(p, 1, 4, -1, C + "marco_dorado");
        m.set(p, 2, 4, -1, C + "marco_dorado");
        m.texto(p, 1, 3, -2, "placa", "north", 0xD9A93A, "ASCENSOR", String.valueOf(n));
        m.set(p, 2, 3, -2, C + "lampara_jaula[facing=north,encendida=false,falla=false]");
        m.marca(p, "sala.asc." + n + ".luz", 2, 3, -2, 0);
        m.texto(p, 1, 2, -2, "letrero", "north", 0xE8E2D0, "2 o 3", "personas");
    }

    /** La pantalla gigante del fondo, con cortinas de terciopelo y candelabros. */
    private static void pantalla(Plano p) {
        int x = X1;
        for (int z = 12; z <= 24; z++) {
            for (int y = 1; y <= 10; y++) {
                p.set(X1 + 1, y, z, C + "metal_negro");
            }
        }
        p.set(x, 6, 18, C + "pantalla[facing=west,grande=true]");
        p.marca("sala.pantalla", x, 6, 18, 90);
        for (int z : new int[] {11, 25}) {
            for (int y : new int[] {1, 3, 5, 7}) {
                p.set(x, y, z, C + "cortina[facing=west,half=lower]");
                p.set(x, y + 1, z, C + "cortina[facing=west,half=upper]");
            }
            p.set(x, 9, z == 11 ? 13 : 23, C + "bocina[facing=west]");
        }
        for (int z : new int[] {14, 22}) {
            p.set(x - 1, 1, z, C + "candelabro[facing=west,half=lower]");
            p.set(x - 1, 2, z, C + "candelabro[facing=west,half=upper]");
        }
        for (int z = 13; z <= 23; z++) {
            p.set(x, 1, z, C + "escalera_metal[facing=east]");
        }
    }

    /** Arañas de velas colgando y una luz pareja (invisible) para que no quede negro. */
    private static void lamparas(Plano p) {
        for (int x = 3; x <= X1 - 1; x += 6) {
            for (int z = 3; z <= Z1 - 1; z += 6) {
                if (p.vacio(x, 5, z)) {
                    p.set(x, 5, z, "light[level=14]");
                }
            }
        }
        for (int x = 8; x <= X1 - 4; x += 12) {
            for (int z : new int[] {9, 27}) {
                p.set(x, TECHO - 1, z, C + "cadena");
                p.set(x, TECHO - 2, z, C + "arana");
            }
        }
        for (int x = 3; x <= X1; x += 5) {
            for (int z = 3; z <= Z1; z += 5) {
                if (p.vacio(x, TECHO - 1, z)) {
                    p.set(x, TECHO - 1, z, "light[level=12]");
                }
            }
        }
        // vigas doradas en el techo
        for (int x = 6; x <= X1; x += 12) {
            for (int z = 0; z <= Z1; z++) {
                p.set(x, TECHO - 1, z, C + "marco_dorado");
            }
        }
    }

    /** Sillones contra las paredes entre los ascensores, estandartes y cámaras. */
    private static void muebles(Plano p) {
        for (int j = 0; j < 5; j++) {
            int x = 12 + 9 * j - 3;
            p.set(x, 1, 0, C + "sillon[facing=south,half=lower]");
            p.set(x, 2, 0, C + "sillon[facing=south,half=upper]");
            p.set(x, 1, Z1, C + "sillon[facing=north,half=lower]");
            p.set(x, 2, Z1, C + "sillon[facing=north,half=upper]");
            p.set(x, 5, 0, C + "estandarte_rojo[facing=south]");
            p.set(x, 5, Z1, C + "estandarte_rojo[facing=north]");
        }
        p.letrero(2, 4, 0, "", "south", "gold", "BIENVENIDOS", "AL GRAN", "VESTÍBULO");
        p.set(1, 9, 0, C + "camara[facing=south]");
        p.set(1, 9, Z1, C + "camara[facing=north]");
        p.set(X1, 10, 1, C + "camara[facing=west]");
    }
}
