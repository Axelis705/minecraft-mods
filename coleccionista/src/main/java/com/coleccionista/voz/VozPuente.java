package com.coleccionista.voz;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Lo único del chat de voz que comparten el mod y el complemento de Simple Voice Chat ({@link VozPlugin}). Así nada más del mod depende
 * de Simple Voice Chat: si no está instalado, el complemento nunca se carga y esto se queda sin usar.
 * <p>
 * El hilo del servidor ({@link Voz}) llena las listas cada medio segundo; el hilo de la voz solo las lee (son copias que no cambian).
 */
public final class VozPuente {
    /** ¿Simple Voice Chat cargó nuestro complemento? */
    public static volatile boolean disponible;
    public static volatile Set<UUID> vivos = Set.of(), fantasmas = Set.of(), organizadores = Set.of(), altavoz = Set.of();
    /** A quién se le cortó el micrófono (la cinta en la boca de Encadenados de tres). */
    public static volatile Set<UUID> mudos = Set.of();
    /** A quién se le puso la cinta para probar ({@code /evento probar cinta}). */
    public static volatile Set<UUID> mudosPrueba = Set.of();
    /** Las voces cambiadas (la caja de música): quién habla con otro tono (menos de 1, más grave; más de 1, más aguda). */
    public static volatile Map<UUID, Float> tonos = Map.of();
    /** Durante algunas pruebas, cada pieza viva en su grupo (sala, cuarto, laberinto): solo se oyen los del mismo grupo. */
    public static volatile Map<UUID, Integer> separados = Map.of();
    /** ¿Los vivos pueden usar los grupos de Simple Voice Chat? (no: así la voz es solo por cercanía, como debe ser en el evento) */
    public static volatile boolean gruposVivos;
    /** ¿Los organizadores oyen el canal de los fantasmas? */
    public static volatile boolean organizadorOyeFantasmas = true;
    /** ¿Separar la voz por salas en las pruebas por equipos? */
    public static volatile boolean separarSalas = true;
    /** (Cliente) Llegó voz por un canal directo (el de los fantasmas): su id. Lo pone el mod del lado del cliente. */
    public static volatile java.util.function.Consumer<UUID> alOirCanal;
    /** (Cliente) Estoy mandando voz (hablo). */
    public static volatile Runnable alHablarYo;
    /** Lo pone el complemento: saca de sus grupos a las piezas (lo llama el hilo del servidor). */
    public static volatile Runnable sincronizar;

    private VozPuente() {
    }

    /** ¿El receptor oye por cercanía al emisor? Los fantasmas nunca se oyen por cercanía; ellos sí oyen a los vivos. */
    public static boolean oye(UUID emisor, UUID receptor) {
        return oye(emisor, receptor, fantasmas, organizadores, separados);
    }

    public static boolean oye(UUID emisor, UUID receptor, Set<UUID> fant, Set<UUID> org, Map<UUID, Integer> sep) {
        if (fant.contains(emisor)) {
            return false;
        }
        if (fant.contains(receptor) || org.contains(receptor) || org.contains(emisor)) {
            return true;
        }
        Integer a = sep.get(emisor), b = sep.get(receptor);
        return a == null || b == null || a.equals(b);
    }

    /** ¿El receptor oye lo que dice un fantasma? (por el canal de los fantasmas, sin importar la distancia) */
    public static boolean oyeAlFantasma(UUID receptor) {
        return oyeAlFantasma(receptor, fantasmas, organizadores);
    }

    public static boolean oyeAlFantasma(UUID receptor, Set<UUID> fant, Set<UUID> org) {
        return fant.contains(receptor) || organizadorOyeFantasmas && org.contains(receptor);
    }

    /** ¿Esta pieza puede entrar o crear un grupo de Simple Voice Chat? */
    public static boolean puedeGrupo(UUID id) {
        return !fantasmas.contains(id) && (gruposVivos || !vivos.contains(id));
    }
}
