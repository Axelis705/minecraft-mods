package com.coleccionista.mapa;

/**
 * La antesala de una prueba: a donde llega el elevador del Coleccionista y de donde sale el siguiente. Dos salas que no se tocan:
 * <ul>
 * <li><b>Sala de llegada</b> (x 0..31): al oeste el elevador de llegada; en los muros largos, diez cubículos con puerta (las "puertas de
 * equipo": cada equipo se mete en uno y la puerta se cierra sola al llenarse); al este el portón de la prueba con su título y la pantalla
 * (ahí se paran las pruebas de uno por uno).</li>
 * <li><b>Sala de espera</b> (x 32..56): a donde llegan al terminar la prueba; aquí esperan a que terminen todos y al este sale el elevador a
 * la siguiente prueba.</li>
 * </ul>
 * <pre>
 *   x: 0 ... 56   z: 0 ... 22   y: -1 ... 12 (piso en y 0)
 *   elevador de llegada: interior x 2..5, z 7..14 (puerta en x 6)   cubículos: interior z 1..3 y 19..21, x 9+4b .. 11+4b (b = 0..4)
 *   elevador de salida: interior x 52..55, z 7..14 (puerta en x 51)
 * </pre>
 * Marcas (con {@code id} la zona de la prueba): {@code cab.ante_<id>.llega} y {@code cab.ante_<id>.sale} (los elevadores),
 * {@code ante.<id>.cub.N} (esquina del interior del cubículo N, 1..10) y {@code .cub.N.puerta} / {@code .cub.N.foco},
 * {@code ante.<id>.frente.a/.b} (el área frente al portón), {@code ante.<id>.espera.N} (lugares en la sala de espera), {@code ante.<id>.centro},
 * {@code ante.<id>.pantalla} y {@code ante.<id>.pantalla2}.
 */
public final class Antesala {
    public static final int X0 = 0, X1 = 56, Z0 = 0, Z1 = 22, Y0 = -1, Y1 = 12;
    /** El muro que separa la sala de llegada de la de espera. */
    public static final int XA = 31;
    /** Cubículos (puertas de equipo) y lugares en la sala de espera. */
    public static final int CUBICULOS = 10, LUGARES = 40;
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
        // el muro entre las dos salas
        for (int z = Z0; z <= Z1; z++) {
            for (int y = 1; y < Y1; y++) {
                p.set(XA, y, z, y == 1 ? C + t.zocalo : y >= Y1 - 2 ? C + t.muro2 : C + t.muro);
            }
        }
        salon(p, t, pre, id);
        Cabinas.elevador(p, "ante_" + id + ".llega", 6, 7, 90, null);
        cubiculos(p, t, pre);
        espera(p, t, pre, id);
        Estilo.aplicar(p, java.util.Map.of());
        return p;
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
        p.letrero(7, 3, 5, "", "east", "white", "Sus cosas", "ya no son suyas.", "Ustedes tampoco.");
        p.letrero(7, 3, 16, "", "east", "red", "Sonrían.", "Los estoy viendo.");
        p.set(13, Y1 - 2, 1, C + "camara[facing=south]");
        p.set(27, Y1 - 2, Z1 - 1, C + "camara[facing=north]");
        // detalles de la zona
        detalles(p, id, cz);
        // ---- el portón de la prueba (muro este): marco, rejas y oscuridad detrás; el título y la pantalla encima
        for (int z = cz - 3; z <= cz + 2; z++) {
            for (int y = 1; y <= 6; y++) {
                boolean marco = z == cz - 3 || z == cz + 2 || y == 6;
                p.set(XA - 1, y, z, marco ? C + t.marco : C + "barrotes");
            }
        }
        p.set(XA - 1, 1, cz - 4, C + "candelabro[facing=west,half=lower]");
        p.set(XA - 1, 2, cz - 4, C + "candelabro[facing=west,half=upper]");
        p.set(XA - 1, 1, cz + 3, C + "candelabro[facing=west,half=lower]");
        p.set(XA - 1, 2, cz + 3, C + "candelabro[facing=west,half=upper]");
        p.texto(XA - 1, 7, cz - 1, "letrero", "west", 0xC0281F, t.numero);
        p.texto(XA - 1, 7, cz, "letrero", "west", 0xE8E2D0, t.titulo);
        p.letrero(XA - 2, 3, cz - 5, "", "west", "gray", t.lema);
        p.set(XA - 1, 9, cz, "coleccionista:pantalla[facing=west]");
        p.marca(pre + "pantalla", XA - 1, 9, cz, 0);
        p.marca(pre + "centro", 20, 1, cz, 90);
    }

    /** Los cubículos de equipo: cinco en cada muro largo, con su puerta, su número y un foco que se prende al llenarse. */
    private static void cubiculos(Plano p, Tema t, String pre) {
        int n = 0;
        for (int lado = 0; lado < 2; lado++) {
            boolean norte = lado == 0;
            int zi = norte ? 1 : Z1 - 3, zf = norte ? 4 : Z1 - 4, zh = norte ? 5 : Z1 - 5;
            String adentro = norte ? "north" : "south", afuera = norte ? "south" : "north";
            for (int b = 0; b < 5; b++) {
                int x0 = 9 + 4 * b;
                n++;
                for (int x = x0 - 1; x <= x0 + 3; x++) {
                    for (int z = Math.min(zi, zf); z <= Math.max(zi + 2, zf); z++) {
                        boolean muro = x == x0 - 1 || x == x0 + 3 || z == zf;
                        p.set(x, 0, z, C + (muro ? "metal_placa" : t.piso2));
                        for (int y = 1; y <= 3; y++) {
                            p.set(x, y, z, muro ? C + "metal_negro" : "air");
                        }
                        p.set(x, 4, z, C + "metal_negro");
                    }
                }
                // los lugares marcados en el piso
                for (int i = 0; i < 3; i++) {
                    for (int k = 0; k < 3; k++) {
                        if ((i + k) % 2 == 0) {
                            p.set(x0 + i, 0, zi + k, C + "marca_roja");
                        }
                    }
                }
                p.set(x0 + 1, 1, zf, C + "puerta_celda[facing=" + adentro + ",half=lower,hinge=left,open=false]");
                p.set(x0 + 1, 2, zf, C + "puerta_celda[facing=" + adentro + ",half=upper,hinge=left,open=false]");
                p.set(x0, 3, zh, C + "lampara_jaula[facing=" + afuera + ",encendida=false,falla=false]");
                p.texto(x0 + 2, 3, zh, "placa", afuera, 0xD9A93A, "EQUIPO", String.valueOf(n));
                p.marca(pre + "cub." + n, x0, 1, zi, 0);
                p.marca(pre + "cub." + n + ".puerta", x0 + 1, 1, zf, 0);
                p.marca(pre + "cub." + n + ".foco", x0, 3, zh, 0);
            }
        }
        // el área frente al portón (las pruebas de uno por uno se juntan aquí)
        p.marca(pre + "frente.a", XA - 6, 1, 7, 0);
        p.marca(pre + "frente.b", XA - 1, 3, 15, 0);
        for (int x = XA - 6; x <= XA - 1; x++) {
            p.set(x, 0, 7, C + "marca_roja");
            p.set(x, 0, 15, C + "marca_roja");
        }
    }

    /** La sala de espera: a donde llegan al terminar la prueba; bancas, pantalla y el elevador a la siguiente prueba. */
    private static void espera(Plano p, Tema t, String pre, String id) {
        int cz = 11;
        if (t.alfombra != null) {
            for (int x = XA + 2; x <= 50; x++) {
                for (int z = cz - 1; z <= cz; z++) {
                    p.set(x, 1, z, C + t.alfombra);
                }
            }
        }
        for (int x = XA + 3; x <= 49; x += 4) {
            p.set(x, 1, 2, C + "silla[facing=south]");
            p.set(x, 1, Z1 - 2, C + "silla[facing=north]");
        }
        for (int x = XA + 3; x <= 50; x += 5) {
            p.set(x, Y1 - 1, 5, C + "lampara_tubo[facing=south,encendida=true,falla=false]");
            p.set(x, Y1 - 1, Z1 - 5, C + "lampara_tubo[facing=north,encendida=true,falla=false]");
            p.set(x, Y1 - 1, cz, C + "lampara_tubo[facing=north,encendida=true,falla=false]");
        }
        p.set(XA + 1, 6, cz, "coleccionista:pantalla[facing=east]");
        p.marca(pre + "pantalla2", XA + 1, 6, cz, 0);
        p.letrero(XA + 1, 3, cz - 4, "", "east", "gold", "SALA DE ESPERA", "", "Nadie sale", "hasta que terminen todos.");
        p.letrero(XA + 1, 3, cz + 4, "", "east", "white", "La colección", "sigue adelante.");
        p.set(XA + 1, Y1 - 2, 1, C + "camara[facing=south]");
        int n = 0;
        for (int z = 3; z <= 19 && n < LUGARES; z += 4) {
            for (int x = XA + 4; x <= 49 && n < LUGARES; x += 2) {
                p.marca(pre + "espera." + n++, x, 1, z, 270);
            }
        }
        p.marca(pre + "centro2", 42, 1, cz, 270);
        Cabinas.elevador(p, "ante_" + id + ".sale", 52, 15, 270, null);
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
