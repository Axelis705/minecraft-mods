package com.coleccionista.mapa;

/**
 * Los pasillos hacia adelante entre capítulos ({@link Salida}): cada uno sale de la puerta de salida de una sala y llega a la puerta de
 * atrás del pasillo de servicio de la siguiente. Están dibujados a mano en {@code data/coleccionista/planos/paso_<sala>.txt} (los arma
 * {@code herramientas/mapas/pasos_plano.py}), cada uno con su tema: el taller de cajas de música, el pasillo de los payasos, la merienda,
 * la galería de espejos y el pasillo de las cartas.
 */
public final class Pasos {
    /** Las salas a las que se llega caminando, en orden, con la que se deja y dónde empieza su pasillo (x relativa, como las zonas). */
    public static final String[][] PASOS = {
            {"caja", "mansion", "775"}, {"sorpresa", "caja", "852"}, {"te", "sorpresa", "934"}, {"piano", "te", "1014"},
            {"veintiuno", "piano", "1094"}};

    private Pasos() {
    }

    public static Plano crear(long semilla, String sala) {
        Plano p = new Plano(semilla);
        Casa.leer("/data/coleccionista/planos/paso_" + sala + ".txt").construir(p);
        return p;
    }

    /** La sala de la que se viene para llegar a {@code sala} (o null si a esa no se llega caminando). */
    public static String anterior(String sala) {
        for (String[] s : PASOS) {
            if (s[0].equals(sala)) {
                return s[1];
            }
        }
        return null;
    }
}
