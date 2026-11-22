package com.mundosdeaxelis.world;

/**
 * Recorre cada estructura como lo haría un jugador desde la entrada (camina, salta 1 bloque, baja hasta 3, usa escaleras de mano)
 * y falla si queda alguna sala cerrada sin acceso o una zona a la que se llega y de la que no se puede volver.
 * Los topes de las tres estructuras antiguas son lo que queda de cornisas y azoteas que el unificador no puede (ni debe) abrir;
 * la torre nueva está planificada sala por sala: no se tolera ninguna sala sin acceso ni ninguna trampa. En el volcán se
 * tolera 1 (lo alto del corazón de piedra que cuelga en el santuario, adorno) y la caída por la lava falsa cuenta como camino.
 * En el Santuario del Bosque se tolera 1: la celda del NPC (cerrada con una puerta de hierro a propósito).
 */
public class ConectividadTest {
    public static void main(String[] args) {
        String[] kinds = {"bosque", "forja", "hielo", "torre", "volcan", "santuario", "fortin", "fragua", "torre_arcana"};
        int[] maxRooms = {3, 16, 2, 2, 2, 1, 1, 0, 0};
        int[] maxTraps = {2, 3, 2, 0, 0, 0, 0, 0, 0};
        // el volcán: 2 «salas» toleradas (lo alto del corazón colgado y, desde la etapa 262, la celda de Ignacio)
        // la fortaleza (torre): 2, las dos celdas de la cripta (la de Teo, etapa 265, y la del norte, cerrada con barrotes en la 287);
        // la Torre Arcana ya no tiene celda (es la despensa)
        // dónde empieza el recorrido (z): el santuario y el fortín entran por el camino, que va sobre el terreno real hasta el arco
        int[] startZ = {-3, -3, -3, -3, -3, 7, 7, 8, 6};
        int fails = 0;
        for (int i = 0; i < kinds.length; i++) {
            StructBp bp = StructDesigns.make(kinds[i]);
            if (!bp.unsupported.isEmpty()) {
                System.out.println(kinds[i] + ": bloques sin apoyo quitados del plano " + bp.unsupported);
            }
            int[] r = Linker.audit(bp, 0, 0, startZ[i], 10);
            // etapa 268: a todos los cofres del santuario se tiene que poder llegar desde la entrada
            java.util.List<String> lost = Linker.unreachableChests(bp, 0, 0, startZ[i]);
            boolean ok = r[2] == 0 && r[0] <= maxRooms[i] && r[1] <= maxTraps[i] && lost.isEmpty();
            System.out.println(kinds[i] + ": salas sin acceso " + r[0] + " (tope " + maxRooms[i] + "), trampas " + r[1] + " (tope " + maxTraps[i] + "), entrada "
                    + (r[2] == 0 ? "transitable" : "BLOQUEADA") + (lost.isEmpty() ? ", cofres al alcance" : ", COFRES SIN ACCESO " + lost) + (ok ? "  OK" : "  FALLA"));
            if (!ok) {
                fails++;
            }
        }
        if (fails > 0) {
            System.exit(1);
        }
    }
}
