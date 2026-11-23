package com.coleccionista.mapa;

/**
 * La antesala de una prueba: a donde llega el montacargas del Coleccionista. Una sala grande con el estilo de su zona: al oeste el
 * montacargas (una jaula de barrotes en su cubo oscuro, con su reja corrediza, su foco y una pantalla), en medio el salón con casilleros,
 * candelabros, letreros y una alfombra que lleva al portón de la prueba (al este), con el título de la prueba y otra pantalla encima. Las piezas despiertan en la jaula, el montacargas "viaja" y al abrirse la reja salen a la antesala; desde aquí el organizador
 * prepara la prueba.
 * <pre>
 *   x: 0 ... 30 (jaula: interior x 3..9, z 7..15; reja en x 10, z 10..11)   z: 0 ... 22   y: -1 ... 12 (piso en y 0)
 * </pre>
 * Marcas (con {@code id} la zona de la prueba): {@code ante.<id>.lugar.N} (dentro de la jaula, mirando a la reja), {@code ante.<id>.reja.a}
 * y {@code .reja.b}, {@code ante.<id>.foco.a} y {@code .foco.b}, {@code ante.<id>.centro} (en medio del salón), {@code ante.<id>.pantalla}.
 */
public final class Antesala {
    public static final int X0 = 0, X1 = 30, Z0 = 0, Z1 = 22, Y0 = -1, Y1 = 12;
    /** Lugares en la jaula (sobra para 24 piezas). */
    public static final int LUGARES = 32;
    private static final String C = "coleccionista:";

    /** Cómo se ve la antesala de cada prueba. */
    private record Tema(String numero, String titulo, String lema, String piso, String piso2, String muro, String muro2, String zocalo,
            String techo, String alfombra, String marco) {
    }

    private Antesala() {
    }

    /** Las zonas que tienen antesala (todas las de las pruebas; el Ala de celdas es donde empieza todo). */
    public static final String[] ZONAS = {"cloacas", "archivo", "salon", "laberintos", "capilla", "puertas", "final", "galeria"};

    private static Tema tema(String id) {
        return switch (id) {
            case "cloacas" -> new Tema("PRUEBA I", "ENCADENADOS", "Lo que se rompe, se encadena.", "piso_cloaca", "concreto_humedo",
                    "ladrillo_cloaca", "ladrillo_oscuro", "cobre_viejo", "concreto_humedo", null, "cobre_viejo");
            case "archivo" -> new Tema("PRUEBA II", "EL TRAIDOR", "Toda colección tiene una pieza falsa.", "parquet", "parquet",
                    "panel_madera", "papel_tapiz", "panel_madera", "panel_madera", "alfombra_cafe", "marco_dorado");
            case "salon" -> new Tema("PRUEBA III", "COMPARTIR O ROBAR", "Una moneda tiene dos caras. Tú también.", "marmol_crema",
                    "marmol_negro", "papel_tapiz", "papel_tapiz", "panel_madera", "panel_madera", "alfombra_roja", "marco_dorado");
            case "laberintos" -> new Tema("PRUEBA IV", "A CIEGAS", "Confía en una voz. Solo en una.", "linoleo_verde", "linoleo",
                    "acolchado", "acolchado", "azulejo_sucio", "acolchado", null, "metal_placa");
            case "capilla" -> new Tema("PRUEBA V", "EL SACRIFICIO", "Alguien tiene que quedarse.", "losa_piedra", "piedra_capilla",
                    "piedra_capilla", "piedra_capilla", "losa_piedra", "piedra_capilla", "alfombra_roja", "marco_dorado");
            case "puertas" -> new Tema("PRUEBA VI", "LAS PUERTAS", "Una de ellas no tiene piso.", "marmol_crema", "marmol_negro",
                    "ladrillo_viejo", "ladrillo_viejo", "marmol_negro", "ladrillo_viejo", "alfombra_negra", "marco_dorado");
            case "final" -> new Tema("LA FINAL", "LAS COPAS", "Una copa. Una pieza. Mía.", "marmol_negro", "marmol_oro", "marmol_negro",
                    "marmol_negro", "marmol_oro", "marmol_negro", "alfombra_roja", "marmol_oro");
            default -> new Tema("PRUEBA ★", "ENCADENADOS DE TRES", "Tres piezas, una cadena.", "concreto_piso", "concreto",
                    "azulejo", "yeso", "azulejo_sucio", "concreto", null, "metal_placa");
        };
    }

    public static Plano crear(long semilla, String id) {
        Tema t = tema(id);
        Plano p = new Plano(semilla ^ id.hashCode());
        String pre = "ante." + id + ".";
        // ---- el casco: piso, techo y muros con zócalo
        p.caja(X0, Y0, Z0, X1, Y0, Z1, C + "concreto");
        p.caja(X0, Y1, Z0, X1, Y1, Z1, C + t.techo);
        for (int x = X0; x <= X1; x++) {
            for (int z = Z0; z <= Z1; z++) {
                boolean borde = x == X0 || x == X1 || z == Z0 || z == Z1;
                p.set(x, 0, z, (x + z) % 2 == 0 ? C + t.piso : C + t.piso2);
                for (int y = 1; y < Y1; y++) {
                    if (borde) {
                        p.set(x, y, z, y == 1 ? C + t.zocalo : y >= Y1 - 2 ? C + t.muro2 : C + t.muro);
                    } else {
                        p.set(x, y, z, "air");
                    }
                }
            }
        }
        jaula(p, t, pre);
        salon(p, t, pre, id);
        Estilo.aplicar(p, java.util.Map.of());
        return p;
    }

    /** El montacargas: el cubo oscuro, la jaula de barrotes con su reja corrediza, el foco, las cadenas y la pantalla. */
    private static void jaula(Plano p, Tema t, String pre) {
        // el cubo (x 1..11, z 4..18): muro oscuro hasta el techo; por dentro un hueco de un bloque alrededor de la jaula
        for (int x = 1; x <= 11; x++) {
            for (int z = 4; z <= 18; z++) {
                boolean muro = x == 11 || z == 4 || z == 18;
                for (int y = 1; y < Y1; y++) {
                    if (x == 11) {
                        // la fachada del montacargas (lado del salón) con los muros de la zona
                        p.set(x, y, z, y == 1 ? C + t.zocalo : y >= Y1 - 2 ? C + t.muro2 : C + t.muro);
                    } else if (muro) {
                        p.set(x, y, z, C + "ladrillo_oscuro");
                    }
                }
                p.set(x, 0, z, C + "metal_negro");
            }
        }
        // la jaula: piso de placa, barrotes al oeste, norte y sur, techo de rejilla y la reja al este (x 10)
        for (int x = 2; x <= 10; x++) {
            for (int z = 6; z <= 16; z++) {
                p.set(x, 0, z, C + (x == 2 || x == 10 || z == 6 || z == 16 ? "metal_placa" : (x + z) % 3 == 0 ? "rejilla_piso[type=double]" : "metal_placa"));
                p.set(x, 6, z, C + (x == 2 || x == 10 || z == 6 || z == 16 ? "viga[axis=y]" : "rejilla_piso[type=bottom]"));
                boolean lado = x == 2 || x == 10 || z == 6 || z == 16;
                for (int y = 1; y <= 5; y++) {
                    if (!lado) {
                        p.set(x, y, z, "air");
                    } else if ((x == 2 || x == 10) && (z == 6 || z == 16)) {
                        p.set(x, y, z, C + "viga[axis=y]");
                    } else {
                        p.set(x, y, z, C + "barrotes");
                    }
                }
            }
        }
        // la reja corrediza (dos hojas: la de z 10 corre al norte, la de z 11 al sur)
        p.set(10, 1, 10, C + "puerta_celda[facing=west,half=lower,hinge=left,open=false]");
        p.set(10, 2, 10, C + "puerta_celda[facing=west,half=upper,hinge=left,open=false]");
        p.set(10, 1, 11, C + "puerta_celda[facing=west,half=lower,hinge=right,open=false]");
        p.set(10, 2, 11, C + "puerta_celda[facing=west,half=upper,hinge=right,open=false]");
        p.marca(pre + "reja.a", 10, 1, 10, 270);
        p.marca(pre + "reja.b", 10, 1, 11, 270);
        // la salida del cubo hacia el salón (x 11, frente a la reja), con su marco y la placa encima
        for (int z = 8; z <= 13; z++) {
            for (int y = 1; y <= 4; y++) {
                boolean marco = z == 8 || z == 13 || y == 4;
                p.set(11, y, z, marco ? C + t.marco : "air");
            }
        }
        p.texto(12, 5, 10, "placa", "east", 0xD9A93A, "MONTACARGAS", "Nº 7");
        p.texto(12, 5, 11, "placa", "east", 0xC0281F, "NO ABRIR", "EN MARCHA");
        p.set(12, 3, 7, C + "lampara_jaula[facing=east,encendida=true,falla=false]");
        p.set(12, 3, 14, C + "lampara_jaula[facing=east,encendida=true,falla=false]");
        // los focos de la jaula (en los barrotes de atrás) y las cadenas que suben del techo de la jaula
        p.set(3, 4, 9, C + "lampara_jaula[facing=east,encendida=true,falla=false]");
        p.set(3, 4, 13, C + "lampara_jaula[facing=east,encendida=true,falla=false]");
        p.marca(pre + "foco.a", 3, 4, 9, 0);
        p.marca(pre + "foco.b", 3, 4, 13, 0);
        for (int[] c : new int[][] {{4, 8}, {8, 8}, {4, 14}, {8, 14}, {6, 11}}) {
            for (int y = 7; y < Y1; y++) {
                p.set(c[0], y, c[1], "chain[axis=y]");
            }
        }
        // la pantallita sobre la reja (por dentro): el Coleccionista anuncia la parada
        p.set(9, 4, 11, "coleccionista:pantalla[facing=west]");
        p.marca(pre + "pantalla.jaula", 9, 4, 11, 0);
        // una mancha en el piso de la jaula (otros ya viajaron aquí)
        p.set(6, 1, 15, C + "mancha[facing=north]");
        // los lugares: una cuadrícula que mira a la reja (este)
        int n = 0;
        for (int z = 7; z <= 15 && n < LUGARES; z += 2) {
            for (int x = 3; x <= 9 && n < LUGARES; x += 2) {
                p.marca(pre + "lugar." + n++, x, 1, z, 270);
            }
        }
        for (int z = 8; z <= 14 && n < LUGARES; z += 2) {
            for (int x = 4; x <= 8 && n < LUGARES; x += 2) {
                p.marca(pre + "lugar." + n++, x, 1, z, 270);
            }
        }
    }

    /** El salón: alfombra, casilleros, candelabros, arañas, letreros y el portón de la prueba con su pantalla. */
    private static void salon(Plano p, Tema t, String pre, String id) {
        int cz = 11;
        // la alfombra de la reja al portón
        if (t.alfombra != null) {
            for (int x = 12; x <= 28; x++) {
                for (int z = cz - 2; z <= cz + 1; z++) {
                    p.set(x, 1, z, C + t.alfombra);
                }
            }
        }
        // columnas con zócalo cada 6 bloques en los muros largos
        for (int x = 14; x <= 26; x += 6) {
            for (int z : new int[] {1, Z1 - 1}) {
                for (int y = 1; y < Y1; y++) {
                    p.set(x, y, z, y == 1 || y == Y1 - 1 ? C + t.marco : C + t.muro2);
                }
            }
        }
        // casilleros (las cosas de las piezas que ya pasaron por aquí) y candelabros contra los muros largos
        for (int x = 16; x <= 25; x += 3) {
            boolean casillero = (x / 3) % 2 == 1;
            for (int[] lado : new int[][] {{2, 0}, {Z1 - 2, 1}}) {
                int z = lado[0];
                String hacia = lado[1] == 0 ? "south" : "north";
                if (casillero) {
                    p.set(x, 1, z, C + "archivero");
                    p.set(x, 2, z, C + "archivero");
                    p.set(x, 3, z, C + "cajon[facing=" + hacia + "]");
                } else {
                    p.set(x, 1, z, C + "candelabro[facing=" + hacia + ",half=lower]");
                    p.set(x, 2, z, C + "candelabro[facing=" + hacia + ",half=upper]");
                }
            }
        }
        // estandartes con el ojo en los muros largos, arriba
        for (int x = 17; x <= 25; x += 4) {
            p.set(x, 7, 1, C + "estandarte_rojo[facing=south,half=lower]");
            p.set(x, 8, 1, C + "estandarte_rojo[facing=south,half=upper]");
            p.set(x, 7, Z1 - 1, C + "estandarte_negro[facing=north,half=lower]");
            p.set(x, 8, Z1 - 1, C + "estandarte_negro[facing=north,half=upper]");
        }
        // arañas de velas colgando del techo
        for (int x = 15; x <= 27; x += 6) {
            p.set(x, Y1 - 1, cz, "chain[axis=y]");
            p.set(x, Y1 - 2, cz, C + "arana");
        }
        // luz pareja para que se vea todo (focos en el techo, cerca de los muros)
        for (int x = 13; x <= 28; x += 5) {
            p.set(x, Y1 - 1, 4, C + "lampara_tubo[facing=south,encendida=true,falla=false]");
            p.set(x, Y1 - 1, Z1 - 4, C + "lampara_tubo[facing=north,encendida=true,falla=false]");
        }
        // bancas para esperar (sillas) a los lados de la alfombra
        for (int x = 16; x <= 24; x += 4) {
            p.set(x, 1, cz - 5, C + "silla[facing=south]");
            p.set(x, 1, cz + 4, C + "silla[facing=north]");
        }
        // letreros con historia en el muro de la jaula (lado del salón)
        p.letrero(12, 3, 4, "", "east", "gold", "SALA DE ESPERA", "DE LAS PIEZAS");
        p.letrero(12, 3, Z1 - 4, "", "east", "white", "Sus cosas", "ya no son suyas.", "Ustedes tampoco.");
        p.letrero(18, 3, 1, "", "south", "gray", "Cada pieza que", "pasó por aquí", "sigue aquí.");
        p.letrero(24, 3, Z1 - 1, "", "north", "red", "Sonrían.", "Los estoy viendo.");
        p.set(13, Y1 - 2, 1, C + "camara[facing=south]");
        p.set(27, Y1 - 2, Z1 - 1, C + "camara[facing=north]");
        // detalles de la zona
        detalles(p, id, cz);
        // ---- el portón de la prueba (muro este): marco, rejas y oscuridad detrás; el título y la pantalla encima
        for (int z = cz - 3; z <= cz + 2; z++) {
            for (int y = 1; y <= 6; y++) {
                boolean marco = z == cz - 3 || z == cz + 2 || y == 6;
                p.set(X1 - 1, y, z, marco ? C + t.marco : C + "barrotes");
                p.set(X1, y, z, C + "ladrillo_oscuro");
            }
        }
        p.set(X1 - 1, 1, cz - 4, C + "candelabro[facing=west,half=lower]");
        p.set(X1 - 1, 2, cz - 4, C + "candelabro[facing=west,half=upper]");
        p.set(X1 - 1, 1, cz + 3, C + "candelabro[facing=west,half=lower]");
        p.set(X1 - 1, 2, cz + 3, C + "candelabro[facing=west,half=upper]");
        p.texto(X1 - 1, 7, cz - 1, "letrero", "west", 0xC0281F, t.numero);
        p.texto(X1 - 1, 7, cz, "letrero", "west", 0xE8E2D0, t.titulo);
        p.letrero(X1 - 1, 3, cz - 5, "", "west", "gray", t.lema);
        p.set(X1 - 1, 9, cz, "coleccionista:pantalla[facing=west]");
        p.marca(pre + "pantalla", X1 - 1, 9, cz, 0);
        p.marca(pre + "centro", 20, 1, cz, 90);
    }

    /** Lo que distingue a cada antesala. */
    private static void detalles(Plano p, String id, int cz) {
        switch (id) {
            case "cloacas" -> {
                for (int x = 14; x <= 27; x += 3) {
                    p.set(x, 1, cz + (x % 2 == 0 ? 3 : -4), C + "charco[facing=north]");
                }
                p.set(13, 1, 2, C + "barril[facing=south]");
                p.set(13, 1, Z1 - 2, C + "barril[facing=north]");
                for (int x = 15; x <= 27; x += 4) {
                    p.set(x, 4, 1, C + "argolla[facing=south]");
                    p.set(x, 5, 1, C + "musgo[facing=south]");
                    for (int y = 6; y < Y1; y++) {
                        p.set(x + 1, y, 3, "chain[axis=y]");
                    }
                }
            }
            case "archivo" -> {
                for (int x = 13; x <= 28; x++) {
                    if (x % 6 != 2) {
                        p.set(x, 4, 1, C + "estante_archivo");
                        p.set(x, 5, 1, C + "estante_archivo");
                        p.set(x, 4, Z1 - 1, C + "estante_archivo");
                        p.set(x, 5, Z1 - 1, C + "estante_archivo");
                    }
                }
                p.set(26, 1, cz - 4, C + "atril[facing=west]");
                p.set(14, 1, cz + 3, C + "archivero");
                p.set(14, 2, cz + 3, C + "papeles[facing=north]");
                for (int x = 15; x <= 27; x += 3) {
                    p.set(x, 1, cz + (x % 2 == 0 ? 2 : -3), C + "papeles[facing=" + (x % 2 == 0 ? "east" : "west") + "]");
                }
            }
            case "salon" -> {
                p.set(14, 1, cz - 4, C + "mesa_tapete[facing=north]");
                p.set(14, 2, cz - 4, C + "vela");
                p.set(26, 1, cz + 3, C + "mesa_tapete[facing=north]");
                p.set(26, 2, cz + 3, C + "vela");
                p.set(13, 1, cz + 3, C + "sillon[facing=east,half=lower]");
                p.set(13, 2, cz + 3, C + "sillon[facing=east,half=upper]");
                for (int x = 15; x <= 27; x += 6) {
                    p.set(x, Y1 - 2, cz - 6, C + "lampara_billar");
                    p.set(x, Y1 - 2, cz + 5, C + "lampara_billar");
                }
            }
            case "laberintos" -> {
                for (int x = 13; x <= 28; x += 2) {
                    for (int z = 3; z <= Z1 - 3; z += 4) {
                        p.set(x, Y1, z, C + "panel_luz");
                    }
                }
                p.set(14, 1, cz - 5, C + "camilla[facing=east]");
                p.set(26, 1, cz + 4, C + "silla_ruedas[facing=west]");
                p.set(20, 1, cz - 6, C + "mesa[facing=south]");
                p.set(20, 2, cz - 6, C + "objeto_venda[facing=south]");
                for (int x = 13; x <= 28; x++) {
                    p.set(x, 0, cz - 3, C + "marca_roja");
                    p.set(x, 0, cz + 2, C + "marca_roja");
                }
            }
            case "capilla" -> {
                for (int x = 15; x <= 27; x += 4) {
                    for (int y = 3; y <= 6; y++) {
                        p.set(x, y, 0, C + "vitral");
                        p.set(x, y, Z1, C + "vitral");
                    }
                }
                for (int x = 13; x <= 28; x += 3) {
                    p.set(x, 1, cz - 3, C + "vela");
                    p.set(x, 1, cz + 2, C + "vela");
                }
                p.set(26, 1, cz - 1, C + "placa_altar[facing=west]");
            }
            case "puertas" -> {
                // tres marcos de luz en el muro norte, uno de cada color: el anuncio de lo que viene
                String[] luz = {"marco_luz_roja", "marco_luz_azul", "marco_luz_verde"};
                for (int i = 0; i < 3; i++) {
                    int x = 15 + i * 6;
                    for (int y = 1; y <= 4; y++) {
                        p.set(x, y, 1, C + luz[i]);
                        p.set(x + 2, y, 1, C + luz[i]);
                    }
                    p.set(x + 1, 4, 1, C + luz[i]);
                    p.set(x + 1, 1, 1, C + "metal_negro");
                    p.set(x + 1, 2, 1, C + "metal_negro");
                    p.set(x + 1, 3, 1, C + "metal_negro");
                }
                for (int x = 14; x <= 28; x += 2) {
                    p.set(x, 0, cz + 4, C + "trampilla[facing=north,half=top,open=false]");
                }
            }
            case "final" -> {
                for (int x = 15; x <= 27; x += 4) {
                    p.set(x, 1, cz - 5, C + "ladrillo_oscuro");
                    p.set(x, 2, cz - 5, C + "vela");
                    p.set(x, 3, cz - 5, C + "vidrio_sucio");
                    p.set(x, 1, cz + 4, C + "ladrillo_oscuro");
                    p.set(x, 2, cz + 4, C + "vela");
                    p.set(x, 3, cz + 4, C + "vidrio_sucio");
                }
            }
            default -> {
                for (int x = 15; x <= 27; x += 4) {
                    for (int y = 1; y <= 3; y++) {
                        p.set(x, y, cz - 5, C + (y == 1 ? "poste" : "poste_liso") + "[axis=y]");
                        p.set(x + 2, y, cz + 4, C + (y == 1 ? "poste" : "poste_liso") + "[axis=y]");
                    }
                }
                p.set(14, 1, cz + 4, C + "mesa[facing=north]");
                p.set(14, 2, cz + 4, C + "objeto_esposas[facing=north]");
                for (int y = 6; y < Y1; y++) {
                    p.set(20, y, cz - 4, "chain[axis=y]");
                    p.set(20, y, cz + 3, "chain[axis=y]");
                }
            }
        }
    }
}
