package com.coleccionista.mapa;

import java.util.ArrayList;
import java.util.List;

/**
 * El hotel abandonado del Coleccionista (el minijuego del principio del capítulo 1) y su sala de espera.
 * <p>
 * El hotel sale del plano de texto {@code planos/hotel.txt} (diez pisos de 51 x 51 dibujados a mano, ver
 * {@code herramientas/mapas/hotel_plano.py}); aquí se le pone lo que el texto no puede decir:
 * <ul>
 *   <li>los 32 ascensores de llegada ({@code hotel.cab.N} → {@code cab.hotel_hN.*}, idénticos por dentro a los de la torre de la sala);</li>
 *   <li>las 20 puertas de servicio ({@code hotel.serv.N}): un hueco con barrera (la puerta la dibuja una entidad), su placa "SERVICIO 4A"
 *   y las cuatro casillas de su clóset ({@code hotel.serv.N.k0..k3}, con el giro de la esquina del tobogán que les toca);</li>
 *   <li>los candelabros que se caen, los charcos, las tablas con clavos, los tubos sueltos, los nidos de ratas y los botiquines.</li>
 * </ul>
 * La sala de espera ({@link #espera}) es otra zona: un tubo de colores muy alto (por donde se cae al salir) que desemboca en un cuarto
 * de brincolín con colchones, bancas y la pared de monitores para ver a los que siguen adentro.
 */
public final class Hotel {
    private static final String C = "coleccionista:";
    public static final String PLANO = "/data/coleccionista/planos/hotel.txt";
    public static final int PISOS = 10, ALTO_PISO = 5;
    /** Cuántos ascensores de llegada y puertas de servicio tiene (los cuenta el plano). */
    public static int ascensores, puertas;
    private static final int[][] DIR = {{0, -1}, {0, 1}, {1, 0}, {-1, 0}};

    private Hotel() {
    }

    /** El piso (1..10) de una altura del plano. */
    public static int piso(int y) {
        return y / ALTO_PISO + 1;
    }

    /** "4A", "4B": el código de la puerta de servicio N (dos por piso, en el orden del plano). */
    public static String codigo(int n) {
        return (n / 2 + 1) + (n % 2 == 0 ? "A" : "B");
    }

    private static int[] dir(float yaw) {
        int y = Math.floorMod(Math.round(yaw), 360);
        return y == 0 ? new int[] {0, 1} : y == 90 ? new int[] {-1, 0} : y == 180 ? new int[] {0, -1} : new int[] {1, 0};
    }

    private static String haciaYaw(float yaw) {
        int y = Math.floorMod(Math.round(yaw), 360);
        return y == 0 ? "south" : y == 90 ? "west" : y == 180 ? "north" : "east";
    }

    public static Plano crear(long semilla) {
        Plano p = new Plano(semilla);
        Casa c = Casa.leer(PLANO);
        c.construir(p);
        List<String> nombres = new ArrayList<>(p.marcas.keySet());
        int cabs = 0, servs = 0;
        for (String n : nombres) {
            Plano.Marca m = p.marcas.get(n);
            Casa.Piso f = c.pisos.get(Math.min(c.pisos.size() - 1, piso(m.y() - 1) - 1));
            int y0 = m.y() - 1;
            if (n.startsWith("hotel.cab.")) {
                // la marca mira al pasillo; se entra al revés
                int k = Integer.parseInt(n.substring("hotel.cab.".length()));
                Cabinas.individual(p, "hotel_h" + k, Marco.deBloque(m.x(), m.z(), m.yaw() + 180F), y0, "PISO " + piso(y0));
                cabs++;
            } else if (n.startsWith("hotel.serv.")) {
                servicio(p, f, n, m, y0);
                servs++;
            } else if (n.startsWith("hotel.cand.")) {
                p.set(m.x(), y0 + 4, m.z(), C + "arana");
            } else if (n.startsWith("hotel.charco.")) {
                p.set(m.x(), y0 + 1, m.z(), C + "charco[facing=north]");
            } else if (n.startsWith("hotel.pua.")) {
                p.set(m.x(), y0 + 1, m.z(), C + "puas[facing=" + (m.x() % 2 == 0 ? "north" : "east") + "]");
            } else if (n.startsWith("hotel.tubo.")) {
                p.set(m.x(), y0 + 1, m.z(), C + "tubo_suelo[facing=" + haciaYaw(m.yaw()) + "]");
            } else if (n.startsWith("hotel.rata.")) {
                p.set(m.x(), y0 + 1, m.z(), C + "papeles[facing=" + haciaYaw(m.yaw() + 90F) + "]");
            } else if (n.startsWith("hotel.botiquin.")) {
                botiquin(p, f, n, m, y0);
            }
        }
        lamparas(p, c);
        ascensores = cabs;
        puertas = servs;
        return p;
    }

    /**
     * Las lámparas que se ven: una de cada tres luces de la cuadrícula del techo se vuelve lámpara de techo (faroles en el ático; los cuartos
     * quemados, los clósets y el pasillo de servicio se quedan casi a oscuras) y en los pasillos hay apliques en la pared cada tantos
     * pasos. Unas cuantas parpadean (las elige el bloque).
     */
    private static void lamparas(Plano p, Casa c) {
        String[] mira = {"south", "north", "west", "east"};
        for (Casa.Piso f : c.pisos) {
            int y0 = f.y();
            for (int z = 0; z < 51; z++) {
                for (int x = 0; x < 51; x++) {
                    char ch = f.en(x, z);
                    String arriba = p.get(x, y0 + 4, z);
                    // (una de cada tres: las demás se quedan como luz sin lámpara; así no parpadean cientos a la vez)
                    if (arriba != null && arriba.startsWith("light[") && ch != 'u' && ch != 'k' && ch != 'e'
                            && Math.floorMod(x / 5 + z / 5 * 2, 3) == 0) {
                        p.set(x, y0 + 4, z, ch == 't' ? C + "farol[facing=north,encendida=true,falla=false]"
                                : C + "plafon[facing=north,encendida=true,falla=false]");
                    }
                    if (ch == 'c' && Math.floorMod(x * 3 + z * 7, 5) == 0 && "air".equals(p.get(x, y0 + 2, z))) {
                        for (int i = 0; i < 4; i++) {
                            char w = f.en(x + DIR[i][0], z + DIR[i][1]);
                            if (w == '#' || w == 'X') {
                                p.set(x, y0 + 2, z, C + "aplique[facing=" + mira[i] + ",encendida=true,falla=false]");
                                break;
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Una puerta de servicio: el hueco se tapa con barrera (choca y se puede picar; la hoja la dibuja la entidad de la puerta), la placa
     * de latón arriba, por el lado del pasillo, y las cuatro casillas del clóset (para el hoyo del tobogán, la trampa o los trastes).
     */
    private static void servicio(Plano p, Casa.Piso f, String n, Plano.Marca m, int y0) {
        int k = Integer.parseInt(n.substring("hotel.serv.".length()));
        p.set(m.x(), y0 + 1, m.z(), "minecraft:barrier");
        p.set(m.x(), y0 + 2, m.z(), "minecraft:barrier");
        int[] d = dir(m.yaw());
        // la placa, del lado del pasillo (mirando hacia afuera del clóset)
        String afuera = haciaYaw(m.yaw() + 180F);
        p.texto(m.x() - d[0], y0 + 3, m.z() - d[1], "placa", afuera, 0xD9A93A, "SERVICIO", codigo(k));
        // el clóset: 2 x 2 detrás de la puerta (hacia un lado o hacia el otro: el que sea clóset en el dibujo)
        int px = -d[1], pz = d[0];
        if (f.en(m.x() + d[0] + px, m.z() + d[1] + pz) != 'k') {
            px = -px;
            pz = -pz;
        }
        List<int[]> cs = new ArrayList<>();
        for (int a = 1; a <= 2; a++) {
            for (int b = 0; b <= 1; b++) {
                cs.add(new int[] {m.x() + d[0] * a + px * b, m.z() + d[1] * a + pz * b});
            }
        }
        int minX = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
        for (int[] q : cs) {
            minX = Math.min(minX, q[0]);
            minZ = Math.min(minZ, q[1]);
        }
        for (int i = 0; i < cs.size(); i++) {
            int[] q = cs.get(i);
            boolean oeste = q[0] == minX, norte = q[1] == minZ;
            // la esquina del tobogán: el modelo tiene muro al norte y al oeste; girado cubre cada esquina
            float giro = norte ? (oeste ? 180F : 270F) : (oeste ? 90F : 0F);
            p.marca(n + ".k" + i, q[0], y0, q[1], giro);
        }
        // el fondo del clóset (para la trampa: apunta a la puerta)
        p.marca(n + ".fondo", m.x() + d[0] * 2, y0 + 1, m.z() + d[1] * 2, m.yaw() + 180F);
    }

    /** El botiquín va en la pared más cercana a su casilla (la marca queda en el bloque del botiquín). */
    private static void botiquin(Plano p, Casa.Piso f, String n, Plano.Marca m, int y0) {
        String[] mira = {"south", "north", "west", "east"};
        for (int i = 0; i < 4; i++) {
            char c = f.en(m.x() + DIR[i][0], m.z() + DIR[i][1]);
            if (c == '#' || c == 'X' || c == 'W') {
                p.set(m.x(), y0 + 2, m.z(), C + "botiquin[facing=" + mira[i] + "]");
                p.marca(n.replace("botiquin", "bot"), m.x(), y0 + 2, m.z(), 0);
                return;
            }
        }
        p.set(m.x(), y0 + 1, m.z(), C + "botiquin[facing=north]");
        p.marca(n.replace("botiquin", "bot"), m.x(), y0 + 1, m.z(), 0);
    }

    // ------------------------------------------------------------------ la sala de espera

    /** La sala de espera: el cuarto (de x 0 a 28, z 0 a 22) y, encima de su centro, el tubo del tobogán hasta y {@link #TUBO_ARRIBA}. */
    public static final int EX1 = 28, EZ1 = 22, ETECHO = 7, TUBO_X = 14, TUBO_Z = 11, TUBO_ARRIBA = 50;
    /** Cuántos monitores hay en la pared de los monitores (dos filas). */
    public static final int MONITORES = 12;
    private static final String[] ARCOIRIS = {"minecraft:red_concrete", "minecraft:yellow_concrete", "minecraft:lime_concrete",
            "minecraft:light_blue_concrete", "minecraft:orange_concrete", "minecraft:blue_concrete"};

    public static Plano espera(long semilla) {
        Plano p = new Plano(semilla);
        // el casco y el hueco
        p.caja(0, 0, 0, EX1, ETECHO, EZ1, "minecraft:yellow_terracotta");
        p.caja(1, 1, 1, EX1 - 1, ETECHO - 1, EZ1 - 1, "air");
        // piso de cuadros de hule espuma de colores (como de brincolín) y paredes amarillas de papel viejo con zoclo
        String[] hule = {"minecraft:red_wool", "minecraft:yellow_wool", "minecraft:lime_wool", "minecraft:light_blue_wool"};
        for (int x = 1; x < EX1; x++) {
            for (int z = 1; z < EZ1; z++) {
                p.set(x, 0, z, hule[(x / 2 + z / 2 * 3) % 4]);
                p.set(x, ETECHO, z, (x + z) % 2 == 0 ? "minecraft:smooth_sandstone" : "minecraft:sandstone");
            }
        }
        for (int y = 1; y < ETECHO; y++) {
            String b = y == 1 ? "minecraft:yellow_glazed_terracotta" : y == 4 ? "minecraft:orange_terracotta" : "minecraft:yellow_terracotta";
            for (int x = 0; x <= EX1; x++) {
                p.set(x, y, 0, b);
                p.set(x, y, EZ1, b);
            }
            for (int z = 0; z <= EZ1; z++) {
                p.set(0, y, z, b);
                p.set(EX1, y, z, b);
            }
        }
        // luces de tubo (zumban) en el techo, como de oficina vieja
        for (int x = 4; x < EX1; x += 6) {
            for (int z = 4; z < EZ1; z += 7) {
                if (Math.abs(x - TUBO_X) > 2 || Math.abs(z - TUBO_Z) > 2) {
                    p.set(x, ETECHO - 1, z, C + "lampara_tubo[facing=north,encendida=true,falla=false]");
                }
                p.set(x, ETECHO - 2, z, "light[level=14]");
            }
        }
        tubo(p);
        // abajo del tubo: colchones de brincolín (3 x 3 y una orilla)
        for (int x = TUBO_X - 2; x <= TUBO_X + 2; x++) {
            for (int z = TUBO_Z - 2; z <= TUBO_Z + 2; z++) {
                p.set(x, 1, z, C + "colchon_brinco[facing=north]");
            }
        }
        p.marca("hotel.espera.colchon", TUBO_X, 2, TUBO_Z, 0);
        // la pared de los monitores (al norte): dos filas de seis teles en repisas, con bancas enfrente
        for (int i = 0; i < MONITORES; i++) {
            int fila = i / 6, col = i % 6;
            int x = 4 + col * 4, y = 2 + fila * 2, z = 1;
            p.set(x, y - 1, z, "minecraft:dark_oak_slab[type=top]");
            p.set(x - 1, y - 1, z, "minecraft:dark_oak_slab[type=top]");
            p.set(x + 1, y - 1, z, "minecraft:dark_oak_slab[type=top]");
            p.set(x, y, z, C + "monitor[facing=south]");
            p.marca("hotel.monitor." + i, x, y, z, 0);
        }
        for (int x = 4; x <= 26; x += 3) {
            p.set(x, 1, 6, C + "banca[facing=north]");
        }
        // sillones de colores en las orillas y un letrero
        for (int z = 9; z <= 15; z += 3) {
            p.set(2, 1, z, C + "sillon[facing=east,half=lower]");
            p.set(2, 2, z, C + "sillon[facing=east,half=upper]");
            p.set(EX1 - 2, 1, z, C + "sillon[facing=west,half=lower]");
            p.set(EX1 - 2, 2, z, C + "sillon[facing=west,half=upper]");
        }
        p.letrero(TUBO_X, 4, EZ1 - 1, "", "north", "gold", "SALA DE ESPERA", "(por ahora)");
        p.letrero(TUBO_X, 5, 1, "", "south", "red", "EN VIVO");
        p.marca("hotel.espera.centro", TUBO_X, 1, 17, 180);
        return p;
    }

    /**
     * El tubo del tobogán: de 3 x 3 por dentro, del techo de la sala hasta muy arriba, con rayas de colores en espiral (al caer parece
     * que se gira) y luz adentro. Arriba, la marca de donde aparece quien sale del hotel.
     */
    private static void tubo(Plano p) {
        // el hoyo del techo (con borde de lámina)
        for (int x = TUBO_X - 1; x <= TUBO_X + 1; x++) {
            for (int z = TUBO_Z - 1; z <= TUBO_Z + 1; z++) {
                p.set(x, ETECHO, z, "air");
            }
        }
        // la pared del tubo: 12 casillas alrededor del hueco, en espiral
        int[][] anillo = new int[16][];
        int n = 0;
        for (int i = -2; i <= 2; i++) {
            anillo[n++] = new int[] {TUBO_X + i, TUBO_Z - 2};
        }
        for (int i = -1; i <= 2; i++) {
            anillo[n++] = new int[] {TUBO_X + 2, TUBO_Z + i};
        }
        for (int i = 1; i >= -2; i--) {
            anillo[n++] = new int[] {TUBO_X + i, TUBO_Z + 2};
        }
        for (int i = 1; i >= -1; i--) {
            anillo[n++] = new int[] {TUBO_X - 2, TUBO_Z + i};
        }
        for (int y = ETECHO; y <= TUBO_ARRIBA; y++) {
            for (int i = 0; i < n; i++) {
                int[] q = anillo[i];
                int c = Math.floorMod((y / 2) + i / 3, ARCOIRIS.length);
                p.set(q[0], y, q[1], ARCOIRIS[c]);
            }
            if (y > ETECHO) {
                for (int x = TUBO_X - 1; x <= TUBO_X + 1; x++) {
                    for (int z = TUBO_Z - 1; z <= TUBO_Z + 1; z++) {
                        p.set(x, y, z, y % 6 == 0 && x == TUBO_X && z == TUBO_Z ? "light[level=12]" : "air");
                    }
                }
            }
        }
        p.caja(TUBO_X - 2, TUBO_ARRIBA + 1, TUBO_Z - 2, TUBO_X + 2, TUBO_ARRIBA + 1, TUBO_Z + 2, "minecraft:black_concrete");
        p.marca("hotel.tobogan.arriba", TUBO_X, TUBO_ARRIBA - 2, TUBO_Z, 0);
    }
}
