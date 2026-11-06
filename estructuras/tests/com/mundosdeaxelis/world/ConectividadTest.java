package com.mundosdeaxelis.world;

/**
 * Recorre cada estructura como lo haría un jugador desde la entrada (camina, salta 1 bloque, baja hasta 3, usa escaleras de mano)
 * y falla si queda alguna sala cerrada sin acceso o una zona a la que se llega y de la que no se puede volver.
 * Los topes de las tres estructuras antiguas son lo que queda de cornisas y azoteas que el unificador no puede (ni debe) abrir;
 * la torre nueva está planificada sala por sala: solo se tolera el borde superior de una estantería de cuatro bloques (un adorno, no una sala).
 */
public class ConectividadTest {
    public static void main(String[] args) {
        String[] kinds = {"bosque", "forja", "hielo", "torre"};
        int[] maxRooms = {3, 16, 2, 1};
        int[] maxTraps = {2, 3, 2, 0};
        int fails = 0;
        for (int i = 0; i < kinds.length; i++) {
            StructBp bp = StructDesigns.make(kinds[i]);
            if (!bp.unsupported.isEmpty()) {
                System.out.println(kinds[i] + ": bloques sin apoyo quitados del plano " + bp.unsupported);
            }
            int[] r = Linker.audit(bp, 0, 0, -3, 10);
            boolean ok = r[2] == 0 && r[0] <= maxRooms[i] && r[1] <= maxTraps[i];
            System.out.println(kinds[i] + ": salas sin acceso " + r[0] + " (tope " + maxRooms[i] + "), trampas " + r[1] + " (tope " + maxTraps[i] + "), entrada "
                    + (r[2] == 0 ? "transitable" : "BLOQUEADA") + (ok ? "  OK" : "  FALLA"));
            if (!ok) {
                fails++;
            }
        }
        if (fails > 0) {
            System.exit(1);
        }
    }
}
