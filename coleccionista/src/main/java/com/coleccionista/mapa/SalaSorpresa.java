package com.coleccionista.mapa;

/**
 * La sala de la caja sorpresa (capítulo 4): un teatrito de juguetes. Al centro, un piso redondo; alrededor, gradas en herradura (dos
 * escalones) con las sillas mirando al centro; la herradura se abre al este, hacia el escenario alto donde está la caja sorpresa gigante
 * (con su manivela al frente). Al escenario se sube por unos escalones de madera con barandal. Se llega por el pasillo de servicio
 * ({@link Pasaje}), por el muro del oeste.
 * <pre>
 *   x: 0 ... 63   z: -1 ... 43   y: -1 ... 15
 *   el pasillo x 2..15 (entra al salón por x 16), el salón x 17..61 / z 0..42 (alto 13)
 *   el centro de la herradura (35, 21): piso redondo hasta r 4.5; grada 1 (y 1) de r 5 a 7.4; grada 2 (y 2) de r 7.4 a 10
 *   el escenario x 46..58 / z 11..31 (se pisa en y 3); la caja en (52, 21); los escalones al centro del frente (x 44..45)
 * </pre>
 * Marcas: {@code sor.silla.N} (las sillas, con el yaw hacia el centro), {@code sor.centro}, {@code sor.entrada}, {@code sor.caja} (donde
 * va la caja, en el piso del escenario), {@code sor.manivela} (donde se para quien le da cuerda), {@code sor.subida} (al pie de los
 * escalones), {@code sor.zapatos.K} (donde caen los zapatos: el piso redondo del centro), {@code sor.luz.K} (las luces; el yaw es el nivel),
 * {@code sor.pantalla} y el pasillo {@code pas_sor}.
 */
public final class SalaSorpresa {
    private static final String C = "coleccionista:";
    public static final int HX0 = 17, HX1 = 61, HZ0 = 0, HZ1 = 42, ALTO = 13;
    /** El centro de la herradura. */
    public static final int CX = 35, CZ = 21;
    /** La caja (bloque del piso del escenario donde va su centro). */
    public static final int BX = 52, BZ = 21, PISO_ESCENARIO = 3;
    public static int sillas, zapatos, luces;

    private SalaSorpresa() {
    }

    public static Plano crear(long semilla) {
        Plano p = new Plano(semilla);
        sillas = 0;
        zapatos = 0;
        luces = 0;
        p.caja(0, -1, -1, 63, ALTO + 2, 43, C + "ladrillo_oscuro");
        Pasaje.pasillo(p, "pas_sor", new Marco(2, 26, 270));
        salon(p);
        herradura(p);
        escenario(p);
        return p;
    }

    private static void luz(Plano p, int x, int y, int z, int nivel) {
        p.set(x, y, z, "light[level=" + nivel + "]");
        p.marca("sor.luz." + luces++, x, y, z, nivel);
    }

    private static double r(int x, int z) {
        return Math.sqrt((x - CX) * (x - CX) + (z - CZ) * (z - CZ));
    }

    /** El ángulo (grados, -180..180) desde el centro: 0 es el este (el escenario), 90 el sur. */
    private static double ang(double x, double z) {
        return Math.toDegrees(Math.atan2(z - CZ, x - CX));
    }

    // ------------------------------------------------------------------ el salón

    private static void salon(Plano p) {
        for (int x = HX0; x <= HX1; x++) {
            for (int z = HZ0; z <= HZ1; z++) {
                p.set(x, 0, z, C + "parquet");
                for (int y = 1; y <= ALTO; y++) {
                    p.set(x, y, z, "air");
                }
                p.set(x, ALTO + 1, z, C + "madera_pulida");
            }
        }
        // las paredes: terciopelo oscuro entre molduras doradas, como un teatro viejo
        for (int y = 1; y <= ALTO; y++) {
            String b = y <= 2 ? C + "panel_madera" : y == 3 || y == ALTO ? C + "marco_dorado" : C + "acolchado";
            for (int x = HX0 - 1; x <= HX1 + 1; x++) {
                p.set(x, y, HZ0 - 1, b);
                p.set(x, y, HZ1 + 1, b);
            }
            for (int z = HZ0; z <= HZ1; z++) {
                String w = p.get(HX0 - 1, y, z);
                if (w == null || !w.contains("puerta")) {
                    p.set(HX0 - 1, y, z, b);
                }
                p.set(HX1 + 1, y, z, b);
            }
        }
        // el techo: una carpa de rayas que sale del centro (como un circo)
        for (int x = HX0; x <= HX1; x++) {
            for (int z = HZ0; z <= HZ1; z++) {
                int t = (int) Math.floor((ang(x, z) + 180) / 15.0);
                p.set(x, ALTO, z, t % 2 == 0 ? "minecraft:red_wool" : "minecraft:white_wool");
            }
        }
        p.set(CX, ALTO - 1, CZ, C + "cadena");
        p.set(CX, ALTO - 2, CZ, C + "arana");
        // estandartes y repisas con muñecas en las paredes del norte y del sur
        for (int x = HX0 + 3; x <= HX1 - 16; x += 5) {
            p.set(x, 7, HZ0, C + "estandarte_rojo[facing=south,half=lower]");
            p.set(x, 8, HZ0, C + "estandarte_rojo[facing=south,half=upper]");
            p.set(x, 7, HZ1, C + "estandarte_rojo[facing=north,half=lower]");
            p.set(x, 8, HZ1, C + "estandarte_rojo[facing=north,half=upper]");
        }
        for (int x = HX0 + 1; x <= HX1 - 15; x++) {
            p.set(x, 5, HZ0, C + "losa_madera[type=top]");
            p.set(x, 5, HZ1, C + "losa_madera[type=top]");
            if (x % 3 == 1) {
                p.set(x, 6, HZ0, C + "muneca[facing=south]");
                p.set(x, 6, HZ1, C + "muneca[facing=north]");
            }
        }
        // candelabros junto a la entrada
        for (int z : new int[] {CZ - 6, CZ + 6}) {
            p.set(HX0, 1, z, C + "candelabro[facing=east,half=lower]");
            p.set(HX0, 2, z, C + "candelabro[facing=east,half=upper]");
        }
        // la luz: tenue en el salón (se baja cuando sale el payaso)
        for (int x = HX0 + 3; x <= HX1 - 3; x += 7) {
            for (int z = HZ0 + 4; z <= HZ1 - 3; z += 7) {
                luz(p, x, ALTO - 2, z, 11);
            }
        }
        p.marca("sor.entrada", HX0 + 2, 1, CZ, 270);
    }

    // ------------------------------------------------------------------ la herradura (el piso del centro y las gradas)

    private static void herradura(Plano p) {
        for (int x = CX - 11; x <= CX + 11; x++) {
            for (int z = CZ - 11; z <= CZ + 11; z++) {
                double d = r(x, z), a = ang(x, z);
                if (d <= 4.5) {
                    // el piso redondo del centro: una alfombra de rombos
                    p.set(x, 0, z, C + ((x + z) % 2 == 0 ? "tapete_rojo" : "marmol_negro"));
                    continue;
                }
                if (d > 10.5 || Math.abs(a) < 50) {
                    continue;
                }
                boolean pasillo = cerca(a, 90) || cerca(a, 180) || cerca(a, -90);
                if (d <= 7.4) {
                    p.set(x, 1, z, pasillo ? C + "escalera_tablon[facing=" + hacia(a) + "]" : C + "parquet");
                } else if (d <= 10.0) {
                    p.set(x, 1, z, C + "madera_pulida");
                    p.set(x, 2, z, pasillo ? C + "escalera_tablon[facing=" + hacia(a) + "]" : C + "parquet");
                } else {
                    // atrás de la última grada: un barandal (menos en los pasillos)
                    p.set(x, 1, z, C + "madera_pulida");
                    p.set(x, 2, z, C + "madera_pulida");
                    if (!pasillo) {
                        p.set(x, 3, z, C + "barandal");
                    }
                }
            }
        }
        // las sillas: grada 1 (r 6.2) y grada 2 (r 8.7), de 60° a 300° (sin los pasillos), mirando al centro
        sillas(p, 6.2, 2);
        sillas(p, 8.7, 3);
        // la luz del centro (un foco que cuelga) y en las gradas
        luz(p, CX, 6, CZ, 12);
        for (int a = 70; a <= 290; a += 55) {
            double rad = Math.toRadians(a);
            luz(p, (int) Math.round(CX + Math.cos(rad) * 8), 6, (int) Math.round(CZ + Math.sin(rad) * 8), 10);
        }
        p.marca("sor.centro", CX, 1, CZ, 270);
    }

    private static boolean cerca(double a, double b) {
        double d = Math.abs(((a - b) % 360 + 540) % 360 - 180);
        return d < 7;
    }

    private static String hacia(double a) {
        if (cerca(a, 90)) {
            return "south";
        }
        if (cerca(a, -90)) {
            return "north";
        }
        return "west";
    }

    /** Sillas a lo largo de la grada (separadas unos 2.3 bloques), de 62° a 298°, sin tapar los pasillos; todas miran al centro. */
    private static void sillas(Plano p, double radio, int y) {
        double paso = Math.toDegrees(2.3 / radio);
        for (double a = 62; a <= 298; a += paso) {
            double aa = a > 180 ? a - 360 : a;
            if (lejos(aa, 90) < 11 || lejos(aa, 180) < 11 || lejos(aa, -90) < 11) {
                continue;
            }
            double rad = Math.toRadians(a);
            double x = CX + 0.5 + Math.cos(rad) * radio, z = CZ + 0.5 + Math.sin(rad) * radio;
            // mirando al centro (yaw de Minecraft: 0 es el sur, 90 el oeste)
            float yaw = (float) Math.toDegrees(Math.atan2(-(CX + 0.5 - x), CZ + 0.5 - z));
            p.marca("sor.silla." + sillas++, (int) Math.floor(x), y, (int) Math.floor(z), yaw);
        }
    }

    private static double lejos(double a, double b) {
        return Math.abs(((a - b) % 360 + 540) % 360 - 180);
    }

    // ------------------------------------------------------------------ el escenario

    private static void escenario(Plano p) {
        int x0 = 46, x1 = 58, z0 = 11, z1 = 31;
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                p.set(x, 1, z, C + "madera_pulida");
                p.set(x, 2, z, x == x0 ? C + "marco_dorado" : (x + z) % 2 == 0 ? C + "marmol_negro" : C + "marmol_crema");
            }
        }
        // el frente: un listón dorado y candilejas (focos al ras)
        for (int z = z0; z <= z1; z++) {
            p.set(x0 - 1, 1, z, C + "panel_madera");
            if (z % 3 == 0 && (z < 18 || z > 24)) {
                p.set(x0, 3, z, C + "foco[encendido=true,falla=false]");
            }
        }
        // los escalones (dos) con barandal, al centro del frente
        for (int z = CZ - 3; z <= CZ + 3; z++) {
            p.set(x0 - 2, 1, z, C + "escalera_tablon[facing=east]");
            p.set(x0 - 1, 1, z, C + "madera_pulida");
            p.set(x0 - 1, 2, z, C + "escalera_tablon[facing=east]");
            p.set(x0, 3, z, "air");
        }
        for (int x = x0 - 2; x <= x0 - 1; x++) {
            p.set(x, x == x0 - 2 ? 2 : 3, CZ - 4, C + "barandal");
            p.set(x, x == x0 - 2 ? 2 : 3, CZ + 4, C + "barandal");
            p.set(x, 1, CZ - 4, C + "madera_pulida");
            p.set(x, 1, CZ + 4, C + "madera_pulida");
            if (x == x0 - 1) {
                p.set(x, 2, CZ - 4, C + "madera_pulida");
                p.set(x, 2, CZ + 4, C + "madera_pulida");
            }
        }
        // el telón del fondo y los de los lados (rojo, con flecos)
        for (int z = z0; z <= z1; z++) {
            for (int y = 3; y <= ALTO - 1; y++) {
                p.set(x1 + 1, y, z, y == ALTO - 1 ? C + "telon_borde" : C + "telon");
            }
        }
        for (int x = x0 + 2; x <= x1; x++) {
            for (int y = 3; y <= ALTO - 1; y++) {
                p.set(x, y, z0 - 1, C + "telon");
                p.set(x, y, z1 + 1, C + "telon");
            }
        }
        // la caja sorpresa (la pone la prueba) y donde se para quien le da cuerda (al frente de la manivela)
        p.marca("sor.caja", BX, PISO_ESCENARIO, BZ, 90);
        p.marca("sor.manivela", BX - 3, PISO_ESCENARIO, BZ, 270);
        p.marca("sor.subida", x0 - 3, 1, CZ, 270);
        // los zapatos: los avienta al piso redondo del centro, en medio de todos (primero al centro y luego alrededor)
        p.marca("sor.zapatos." + zapatos++, CX, 1, CZ, 270);
        for (int k = 0; k < 8; k++) {
            double a = Math.toRadians(k * 45 + 22.5);
            p.marca("sor.zapatos." + zapatos++, (int) Math.round(CX + Math.cos(a) * 2.4), 1, (int) Math.round(CZ + Math.sin(a) * 2.4), k * 45);
        }
        for (int k = 0; k < 8; k++) {
            double a = Math.toRadians(k * 45);
            p.marca("sor.zapatos." + zapatos++, (int) Math.round(CX + Math.cos(a) * 3.6), 1, (int) Math.round(CZ + Math.sin(a) * 3.6), k * 45 + 90);
        }
        // candelabros en las esquinas del escenario y la luz de arriba (fuerte sobre la caja)
        for (int z : new int[] {z0 + 1, z1 - 1}) {
            p.set(x1 - 1, 3, z, C + "candelabro[facing=west,half=lower]");
            p.set(x1 - 1, 4, z, C + "candelabro[facing=west,half=upper]");
        }
        luz(p, BX, 9, BZ, 13);
        luz(p, BX - 4, 8, BZ - 6, 10);
        luz(p, BX - 4, 8, BZ + 6, 10);
        // la pantalla grande arriba del telón
        p.set(x1, 10, BZ, C + "pantalla[facing=west,grande=true]");
        p.marca("sor.pantalla", x1, 10, BZ, 90);
        p.texto(x0 - 1, 2, CZ - 6, "placa", "west", 0xD9A93A, "DON", "RESORTE");
        p.texto(x0 - 1, 2, CZ + 6, "placa", "west", 0xD9A93A, "DON", "RESORTE");
    }
}
