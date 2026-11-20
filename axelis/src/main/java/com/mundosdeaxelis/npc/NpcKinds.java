package com.mundosdeaxelis.npc;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Los habitantes del mundo (etapa 257): uno por estructura, encerrado en su celda. Al rescatarlo te da una semilla para que crezca
 * su casa cerca de la tuya; desde ahí abre la tienda (la misma para todos: con más vecinos se desbloquean más cosas).
 * <p>
 * Cada uno tiene su id, su color (marco de la ventana, nombre), el diseño de su casa y cuántas frases de cada tipo tiene en los
 * archivos de idioma ({@code npc.mundos_de_axelis.<id>.preso.N}, {@code .libre.N}, {@code .vecino.N}).
 */
public final class NpcKinds {
    public record Kind(String id, int accent, String house, int presoLines, int libreLines, int vecinoLines) {
        public String nameKey() {
            return "entity.mundos_de_axelis.habitante." + id;
        }

        public String line(String stage, int i) {
            return "npc.mundos_de_axelis." + id + "." + stage + "." + i;
        }
    }

    /** Ruda, la herbolaria: presa en la celda del Santuario del Bosque. */
    public static final Kind RUDA = new Kind("ruda", 0x7DC45A, "casa_ruda", 4, 3, 8);

    public static final Map<String, Kind> ALL = new LinkedHashMap<>();

    static {
        ALL.put(RUDA.id(), RUDA);
    }

    public static Kind get(String id) {
        return id == null ? null : ALL.get(id);
    }

    private NpcKinds() {
    }
}
