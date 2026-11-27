package com.coleccionista.mapa;

import java.io.File;
import java.nio.file.Path;
import java.util.Map;

/**
 * Revisa los mapas del evento sin Minecraft ({@link Revisor}) y saca sus vistas ({@link Vista}) en una carpeta. Uso:
 * <pre>
 *   java -cp ... com.coleccionista.mapa.RevisarMapas &lt;carpeta de salida&gt; [mansion|...] [--estricto]
 * </pre>
 * Con {@code --estricto} termina con error si hay cuartos inaccesibles o escondites a los que no se llega (para la prueba de tests/run.sh).
 */
public final class RevisarMapas {
    private RevisarMapas() {
    }

    /** Dónde está cada sala en el mundo (x de su zona, como en Complejo) y cómo se construye. */
    private static Plano sala(String id) {
        return switch (id) {
            case "mansion" -> Mansion.crear(1234L);
            case "caja" -> CajaMusica.crear(1234L);
            case "sorpresa" -> SalaSorpresa.crear(1234L);
            case "te" -> SalaTe.crear(1234L);
            case "piano" -> SalaPiano.crear(1234L);
            case "veintiuno" -> SalaVeintiuno.crear(1234L);
            default -> throw new IllegalArgumentException(id);
        };
    }

    private static int dx(String id) {
        return switch (id) {
            case "mansion" -> 720;
            case "caja" -> 790;
            case "sorpresa" -> 870;
            case "te" -> 950;
            case "piano" -> 1030;
            case "veintiuno" -> 1110;
            default -> throw new IllegalArgumentException(id);
        };
    }

    /** El pasillo de servicio de cada sala (al que se llega por la puerta de atrás). */
    private static String pasillo(String id) {
        return switch (id) {
            case "caja" -> "pas_caj";
            case "sorpresa" -> "pas_sor";
            case "te" -> "pas_te";
            case "piano" -> "pas_piano";
            case "veintiuno" -> "pas_veintiuno";
            default -> "pas_man";
        };
    }

    /** Dónde están todos al terminar cada sala (de ahí tienen que poder caminar a la salida). */
    private static String[] alTerminar(String id) {
        return switch (id) {
            case "mansion" -> new String[] {"man.entrada"};
            case "caja" -> new String[] {"caja.centro"};
            case "sorpresa" -> new String[] {"sor.centro"};
            case "te" -> new String[] {"te.centro"};
            case "piano" -> new String[] {"piano.entrada", "piano.anillo."};
            default -> new String[] {};
        };
    }

    /**
     * Las marcas que no son un lugar para pararse (el centro de una mesa, la pista de un piano): basta con que se llegue a alguna casilla a
     * {@code radio} o menos (y de una altura parecida). Devuelve cuántas quedan lejos.
     */
    private static int cerca(Revisor r, Plano p, String prefijo, int radio) {
        int lejos = 0;
        for (Map.Entry<String, Plano.Marca> e : p.marcas.entrySet()) {
            if (!e.getKey().startsWith(prefijo)) {
                continue;
            }
            Plano.Marca m = e.getValue();
            boolean hay = false;
            for (int x = m.x() - radio; x <= m.x() + radio && !hay; x++) {
                for (int z = m.z() - radio; z <= m.z() + radio && !hay; z++) {
                    for (int y = m.y() - 1; y <= m.y() + 2 && !hay; y++) {
                        hay = r.alcanzado.contains(Plano.key(x, y, z));
                    }
                }
            }
            if (!hay) {
                lejos++;
                System.out.println("  lejos: no se llega cerca de " + e.getKey() + " (" + m.x() + ", " + m.y() + ", " + m.z() + ")");
            }
        }
        return lejos;
    }

    /** Copia un plano (bloques y marcas) corrido {@code ox} en x. */
    private static void copiar(Plano de, Plano a, int ox) {
        for (Map.Entry<Long, String> e : de.m.entrySet()) {
            long k = e.getKey();
            a.set(Plano.kx(k) + ox, Plano.ky(k), Plano.kz(k), e.getValue());
        }
        for (Map.Entry<String, Plano.Marca> e : de.marcas.entrySet()) {
            Plano.Marca m = e.getValue();
            a.marca(e.getKey(), m.x() + ox, m.y(), m.z(), m.yaw());
        }
    }

    /**
     * Los caminos hacia adelante: para cada capítulo, la sala que se deja, su pasillo y la que sigue, juntas como en el mundo; desde donde
     * terminan todos se tiene que llegar a la salida, recorrer el pasillo y entrar al de servicio de la siguiente.
     */
    private static int caminos(File out, Map<String, Revisor.Forma> deco, Path tex) throws Exception {
        int fallas = 0;
        for (String[] ps : Pasos.PASOS) {
            String hacia = ps[0], de = ps[1];
            int dxp = Integer.parseInt(ps[2]);
            Plano p = new Plano(1L);
            Plano pa = sala(de), pb = sala(hacia), pp = Pasos.crear(1L, hacia);
            // en el mundo: primero las salas y luego el pasillo (que se vuelve a construir después de ellas)
            copiar(pa, p, 0);
            copiar(pb, p, dx(hacia) - dx(de));
            copiar(pp, p, dxp - dx(de));
            // un vehículo (tren, elevador): se sube en una cabina y se baja de la otra; para caminar el mapa, el muro de en medio se abre
            for (Map.Entry<String, Plano.Marca> e : new java.util.ArrayList<>(p.marcas.entrySet())) {
                if (e.getKey().startsWith("trans.") && e.getKey().endsWith(".hueco")) {
                    Plano.Marca h = e.getValue();
                    for (int y = h.y(); y <= h.y() + 1; y++) {
                        p.set(h.x(), y, h.z(), "air");
                    }
                }
            }
            Plano.Marca ini = p.marcas.get("paso_" + hacia + ".inicio");
            Revisor r = new Revisor(p, deco).caminar(ini.x(), ini.y(), ini.z());
            r.marcas("paso_" + hacia + ".").marcas("salida." + de + ".").marcas(pasillo(hacia) + ".centro").marcas(pasillo(hacia) + ".entrada.");
            int lejos = 0;
            for (String m : alTerminar(de)) {
                lejos += cerca(r, p, m, 3);
            }
            int x0 = dxp - dx(de) - 12, x1 = dxp - dx(de) + pp.x1 + 12;
            // los escalones raros solo en el pasillo (en las salas hay mesitas y losas de adorno)
            r.pisos(dxp - dx(de) - 1, pp.z0 - 1, dxp - dx(de) + pp.x1 + 1, pp.z1 + 1, 0, 6);
            System.out.print(r.informe("Hacia " + hacia + " (desde " + de + ")", 8));
            int f = (int) (r.cuantos("no se llega") + r.cuantos("no cabe nadie")) + lejos;
            fallas += f;
            System.out.println("CAMINO " + de + " -> " + hacia + (f == 0 ? " OK" : " FALLA " + f));
            Vista v = new Vista(p, r, tex);
            v.iso(new File(out, "camino_" + hacia + ".png"), 4, 0, new int[] {x0, pp.z0 - 6, x1, pp.z1 + 6}, 14,
                    "Camino: " + de + " → " + hacia);
        }
        fallas += desenlace(out, deco, tex);
        return fallas;
    }

    /** El desenlace: de la mesa de juego, por la puerta del este, todo el pasillo de la salida hasta la luz. */
    private static int desenlace(File out, Map<String, Revisor.Forma> deco, Path tex) throws Exception {
        Plano p = new Plano(1L);
        copiar(sala("veintiuno"), p, 0);
        copiar(SalidaFinal.crear(1234L), p, 50);
        Plano.Marca ini = p.marcas.get("v21.entrada");
        Revisor r = new Revisor(p, deco).caminar(ini.x(), ini.y(), ini.z());
        r.marcas("salida.veintiuno.").marcas("sf.");
        r.pisos(50, 13, 50 + SalidaFinal.LARGO, 30, 0, 12);
        System.out.print(r.informe("El desenlace (de la mesa a la luz)", 8));
        int f = (int) (r.cuantos("no se llega") + r.cuantos("no cabe nadie"));
        System.out.println("CAMINO veintiuno -> luz" + (f == 0 ? " OK" : " FALLA " + f));
        Vista v = new Vista(p, r, tex);
        v.iso(new File(out, "camino_desenlace.png"), 6, 0, new int[] {36, 4, 50 + SalidaFinal.LARGO + 2, 38}, 9, "Desenlace: la mesa → la luz");
        v.iso(new File(out, "desenlace_vitrina.png"), 6, 0, new int[] {26, 2, 50, 14}, 18, "La vitrina vacía junto al trono");
        return f;
    }

    /** La cochera del teatro: de la puerta del teatro se llega al andén y adentro del autobús. */
    private static int cochera(File out, Map<String, Revisor.Forma> deco, Path tex) throws java.io.IOException {
        Plano p = SalaVitrina.crear(1234L);
        Revisor r = new Revisor(p, deco).caminar(SalaVitrina.SALIDA_X - 1, 1, 34);
        r.marcas("bus_tea.subir").marcas("bus_tea.centro").marcas("vit.antesala").mitades();
        System.out.print(r.informe("La cochera del teatro (del teatro al autobús)", 8));
        int f = (int) (r.cuantos("no se llega") + r.cuantos("no cabe nadie") + r.cuantos("mueble partido"));
        System.out.println("CAMINO teatro -> autobus" + (f == 0 ? " OK" : " FALLA " + f));
        Vista v = new Vista(p, r, tex);
        v.iso(new File(out, "cochera.png"), 9, 0, new int[] {50, 18, 80, 50}, 16, "La cochera y el autobús");
        v.iso(new File(out, "cochera_dentro.png"), 3, 0, new int[] {56, 36, 76, 47}, 22, "El autobús por dentro");
        return f;
    }

    /** Todos los mapas del evento (como en Complejo.zonas()), con su nombre. */
    private static Map<String, Plano> todos() {
        Map<String, Plano> t = new java.util.LinkedHashMap<>();
        t.put("celdas", AlaCeldas.crear(1234L));
        t.put("sala", SalaGrande.crear(1234L));
        for (int k = 1; k <= Circuito.CIRCUITOS; k++) {
            t.put("circ_" + k, Circuito.crear(1234L, k));
        }
        t.put("vitrina", SalaVitrina.crear(1234L));
        for (String id : new String[] {"mansion", "caja", "sorpresa", "te", "piano", "veintiuno"}) {
            t.put(id, sala(id));
        }
        t.put("salida_final", SalidaFinal.crear(1234L));
        t.put("cloacas", Cloacas.crear(1234L));
        t.put("salon", Salon.crear(1234L));
        t.put("puertas", Puertas.crear(1234L));
        t.put("final", Final.crear(1234L));
        t.put("capilla", Capilla.crear(1234L));
        t.put("archivo", Archivo.crear(1234L));
        t.put("laberintos", Laberintos.crear(1234L));
        t.put("galeria", Galeria.crear(1234L));
        for (String z : Antesala.ZONAS) {
            t.put("ante_" + z, Antesala.crear(1234L, z));
        }
        for (String[] ps : Pasos.PASOS) {
            t.put("paso_" + ps[0], Pasos.crear(1234L, ps[0]));
        }
        return t;
    }

    /** Las pantallas de todos los mapas: que su imagen no tape una puerta o un paso, ni se meta en un muro. */
    private static int pantallas(Map<String, Revisor.Forma> deco) {
        int fallas = 0, total = 0;
        for (Map.Entry<String, Plano> e : todos().entrySet()) {
            Revisor r = new Revisor(e.getValue(), deco).pantallas().corredizas().mitades();
            total += e.getValue().m.values().stream().filter(b -> b.startsWith("coleccionista:pantalla")).count();
            int f = r.problemas.size();
            if (f > 0) {
                System.out.print(r.informe("Pantallas y puertas de " + e.getKey(), 8));
            }
            fallas += f;
        }
        System.out.println("PANTALLAS, PUERTAS Y MUEBLES (" + total + " pantallas)" + (fallas == 0 ? " OK" : " FALLA " + fallas));
        return fallas;
    }

    public static void main(String[] a) throws Exception {
        File out = new File(a.length > 0 ? a[0] : "mapas_revision");
        out.mkdirs();
        String cual = a.length > 1 && !a[1].startsWith("--") ? a[1] : "mansion";
        boolean estricto = String.join(" ", a).contains("--estricto");
        Path base = Path.of(System.getProperty("coleccionista.raiz", "."));
        Map<String, Revisor.Forma> deco = Revisor.leerDeco(base.resolve("src/main/resources/data/coleccionista/deco.json"));
        Path tex = base.resolve("src/main/resources/assets/coleccionista/textures/block");
        int fallas = 0;
        if (cual.equals("mansion")) {
            Plano p = Mansion.crear(1234L);
            Plano.Marca e = p.marcas.get("man.entrada");
            Revisor r = new Revisor(p, deco).caminar(e.x(), e.y(), e.z());
            // la jaula de Cascabel está cerrada a propósito (la reja se abre en el juego); los techos, bardas y setos no son cuartos
            r.cerrado(Mansion.REJA_X0 - 1, Mansion.REJA_Z, Mansion.REJA_X0 + Mansion.REJA_Z1 + 1, Mansion.HZ1)
                    .ignorarIslas(c -> Mansion.enTecho(p, c[0], c[1], c[2])).islas(8, 0, 60).marcas("man.esc.").marcas("man.entrada")
                    .marcas("salida.mansion").marcas("bus_man.subir").marcas("bus_man.centro").pisos(p.x0, p.z0, p.x1, p.z1, 0, 40).puertas().mitades();
            System.out.print(r.informe("La mansión", 12));
            System.out.println("escondites: " + p.marcas.keySet().stream().filter(k -> k.startsWith("man.esc.")).count());
            Vista v = new Vista(p, r, tex);
            v.planta(new File(out, "mansion_sotano.png"), 0, 5, "man.esc.", "Mansión · sótano");
            v.planta(new File(out, "mansion_planta_baja.png"), 7, 13, "man.esc.", "Mansión · planta baja");
            v.planta(new File(out, "mansion_planta_alta.png"), 15, 20, "man.esc.", "Mansión · planta alta");
            v.planta(new File(out, "mansion_atico.png"), 22, 29, "man.esc.", "Mansión · ático");
            v.planta(new File(out, "mansion_jardin.png"), 6, 12, "man.esc.", "Mansión · jardín");
            for (int esq = 0; esq < 4; esq++) {
                v.iso(new File(out, "mansion_iso_" + esq + ".png"), 60, esq, null, 5, "Mansión y jardín · esquina " + esq);
            }
            // de cerca: la casa y cada piso
            int[] casa = {2, -7, 51, 54};
            v.iso(new File(out, "mansion_casa.png"), 60, 2, new int[] {-2, -8, 55, 56}, 9, "La casa");
            v.iso(new File(out, "mansion_corte_sotano.png"), 5, 0, casa, 9, "Sótano");
            v.iso(new File(out, "mansion_corte_baja.png"), 13, 0, casa, 9, "Planta baja");
            v.iso(new File(out, "mansion_corte_alta.png"), 20, 0, casa, 9, "Planta alta");
            v.iso(new File(out, "mansion_corte_atico.png"), 29, 0, casa, 9, "Ático");
            v.iso(new File(out, "mansion_vestibulo.png"), 20, 0, new int[] {15, -6, 38, 24}, 18, "Vestíbulo");
            v.iso(new File(out, "mansion_parada.png"), 14, 0, new int[] {-14, -36, 22, -1}, 16, "La parada del autobús y la reja");
            fallas += r.cuantos("cuarto inaccesible") + r.cuantos("no se llega") + r.cuantos("no cabe nadie") + r.cuantos("mueble partido");
        }
        if (cual.equals("caminos") || cual.equals("todo")) {
            fallas += caminos(out, deco, tex);
        }
        if (cual.equals("caminos") || cual.equals("todo")) {
            fallas += cochera(out, deco, tex);
        }
        if (cual.equals("pantallas") || cual.equals("caminos") || cual.equals("todo")) {
            fallas += pantallas(deco);
        }
        if (estricto && fallas > 0) {
            System.out.println("REVISION_MAPAS_FALLO " + fallas);
            System.exit(1);
        }
        System.out.println("REVISION_MAPAS_OK");
    }
}
