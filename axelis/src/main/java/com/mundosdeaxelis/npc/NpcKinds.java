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
    /** {@code slim}: brazos finos (como Alex) o anchos (como Steve). */
    public record Kind(String id, int accent, String house, int presoLines, int libreLines, int vecinoLines, boolean slim) {
        public String nameKey() {
            return "entity.mundos_de_axelis.habitante." + id;
        }

        public String line(String stage, int i) {
            return "npc.mundos_de_axelis." + id + "." + stage + "." + i;
        }
    }

    /** Ruda, la herbolaria: presa en la celda del Santuario del Bosque. */
    public static final Kind RUDA = new Kind("ruda", 0x7DC45A, "casa_ruda", 4, 3, 8, true);
    /** Bruno, el cartógrafo: preso en la celda del Fortín de Escarcha (etapa 258). */
    public static final Kind BRUNO = new Kind("bruno", 0x7FC8F0, "casa_bruno", 4, 3, 8, false);

    /** Hilda, la herrera: presa en la celda de la Fragua Olvidada (etapa 259). */
    public static final Kind HILDA = new Kind("hilda", 0xF0883A, "casa_hilda", 4, 3, 8, true);

    /** Teo, el bibliotecario: preso en la celda de la Torre Arcana (etapa 261). */
    public static final Kind TEO = new Kind("teo", 0x9B7BE0, "casa_teo", 4, 3, 8, false);

    public static final Map<String, Kind> ALL = new LinkedHashMap<>();

    static {
        ALL.put(RUDA.id(), RUDA);
        ALL.put(BRUNO.id(), BRUNO);
        ALL.put(HILDA.id(), HILDA);
        ALL.put(TEO.id(), TEO);
    }

    public static Kind get(String id) {
        return id == null ? null : ALL.get(id);
    }

    private NpcKinds() {
    }
}
