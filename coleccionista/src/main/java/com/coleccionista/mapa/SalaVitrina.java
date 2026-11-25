package com.coleccionista.mapa;

/**
 * La galería de la vitrina: adonde llegan los que salieron de los circuitos y desde donde ven, tras un vidrio, la sala de exhibición del
 * Coleccionista. Ahí llegan las sillas del último equipo, cada una dentro de su vitrina de vidrio; cae la cera y se quedan para siempre.
 * <pre>
 *   x: 0 ... 32   z: 0 ... 30   y: 0 ... 10
 *   galería (los vivos) z 1..10, el vidrio en z 11, la exhibición z 12..29
 * </pre>
 * Marcas: {@code vit.llega.K.i} (las sillas de llegada del equipo K, 1..10, i 0..2), {@code vit.caja.N} (el centro de la vitrina N,
 * 0..5, donde queda la silla), {@code vit.boquilla.N} (de donde cae la cera), {@code vit.luz.N} (el reflector de cada vitrina),
 * {@code vit.placa.N}, {@code vit.pantalla} y {@code vit.centro}.
 */
public final class SalaVitrina {
    public static final int VITRINAS = 6;
    private static final String C = "coleccionista:";

    private SalaVitrina() {
    }

    public static Plano crear(long semilla) {
        Plano p = new Plano(semilla);
        p.caja(0, 0, 0, 32, 10, 30, C + "marmol_negro");
        // la galería: alfombra negra, terciopelo, butacas en filas mirando al vidrio
        p.caja(1, 1, 1, 31, 8, 10, "air");
        for (int x = 1; x <= 31; x++) {
            for (int z = 1; z <= 10; z++) {
                p.set(x, 0, z, C + "parquet");
            }
            for (int y = 1; y <= 8; y++) {
                p.set(x, y, 0, C + (y <= 2 ? "madera_pulida" : "acolchado"));
            }
        }
        // las llegadas: 10 equipos de hasta 3, en dos filas de cinco grupos (mirando al vidrio, hacia el sur)
        for (int k = 1; k <= 10; k++) {
            int fila = (k - 1) / 5, g = (k - 1) % 5;
            int x0 = 2 + 6 * g, z = 7 - 3 * fila;
            for (int i = 0; i < 3; i++) {
                p.marca("vit.llega." + k + "." + i, x0 + 2 * i, 1, z, 0);
            }
            p.texto(x0 + 2, 1, z + 1, "placa", "north", 0x9A958A, "EQUIPO " + k);
        }
        // el vidrio grande
        for (int x = 1; x <= 31; x++) {
            for (int y = 1; y <= 8; y++) {
                p.set(x, y, 11, y == 1 || y == 8 ? C + "marco_dorado" : C + "vidrio_armado");
            }
        }
        // la sala de exhibición: piso de mármol crema, vitrinas en fila, la pantalla del Coleccionista al fondo
        p.caja(1, 1, 12, 31, 9, 29, "air");
        for (int x = 1; x <= 31; x++) {
            for (int z = 12; z <= 29; z++) {
                p.set(x, 0, z, C + ((x + z) % 2 == 0 ? "marmol_crema" : "marmol_oro"));
            }
        }
        for (int n = 0; n < VITRINAS; n++) {
            vitrina(p, n);
        }
        p.set(16, 6, 28, C + "pantalla[facing=north,grande=true]");
        p.marca("vit.pantalla", 16, 6, 28, 180);
        for (int x : new int[] {8, 24}) {
            p.set(x, 1, 28, C + "candelabro[facing=north,half=lower]");
            p.set(x, 2, 28, C + "candelabro[facing=north,half=upper]");
        }
        for (int x = 1; x <= 31; x++) {
            p.set(x, 1, 29, C + "cortina[facing=north,half=lower]");
            p.set(x, 2, 29, C + "cortina[facing=north,half=upper]");
        }
        // poca luz en la galería (los vivos están en penumbra); la exhibición se ilumina con los reflectores
        for (int x = 4; x <= 28; x += 8) {
            p.set(x, 8, 5, "light[level=6]");
        }
        p.letrero(16, 6, 1, "", "south", "gold", "GALERÍA", "DE LA COLECCIÓN");
        p.marca("vit.centro", 16, 1, 5, 0);
        return p;
    }

    /** Una vitrina: caja de vidrio de 3 x 3 sobre una tarima, la boquilla de la cera arriba, el reflector y la placa con el nombre. */
    private static void vitrina(Plano p, int n) {
        int x0 = 2 + 5 * n, z0 = 15;
        for (int x = x0; x <= x0 + 2; x++) {
            for (int z = z0; z <= z0 + 2; z++) {
                p.set(x, 0, z, C + "metal_negro");
                p.set(x, 5, z, C + "metal_negro");
                for (int y = 1; y <= 4; y++) {
                    boolean borde = x == x0 || x == x0 + 2 || z == z0 || z == z0 + 2;
                    boolean esquina = (x == x0 || x == x0 + 2) && (z == z0 || z == z0 + 2);
                    p.set(x, y, z, esquina ? C + "viga[axis=y]" : borde ? C + "vidrio_armado" : "air");
                }
            }
        }
        // el frente (hacia la galería) se queda abierto hasta que llega la silla: lo cierra el evento
        p.marca("vit.frente." + n, x0 + 1, 1, z0, 0);
        p.set(x0 + 1, 1, z0, "air");
        p.set(x0 + 1, 2, z0, "air");
        p.set(x0 + 1, 3, z0, "air");
        p.marca("vit.caja." + n, x0 + 1, 1, z0 + 1, 0);
        p.set(x0 + 1, 5, z0 + 1, C + "tubo[axis=y]");
        p.set(x0 + 1, 6, z0 + 1, C + "tubo[axis=y]");
        p.marca("vit.boquilla." + n, x0 + 1, 4, z0 + 1, 0);
        p.set(x0 + 1, 5, z0 - 1, C + "lampara_jaula[facing=north,encendida=false,falla=false]");
        p.marca("vit.luz." + n, x0 + 1, 5, z0 - 1, 0);
        p.texto(x0, 1, z0 - 1, "placa", "north", 0xD9A93A, "PIEZA", "—");
        p.marca("vit.placa." + n, x0, 1, z0 - 1, 0);
    }
}
