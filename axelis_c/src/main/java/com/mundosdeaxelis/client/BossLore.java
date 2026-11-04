package com.mundosdeaxelis.client;

import com.mundosdeaxelis.entity.GranHerrero;
import com.mundosdeaxelis.entity.ReyBrote;
import com.mundosdeaxelis.registry.ModEntities;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.LivingEntity;

import java.util.List;

/**
 * Textos de cada jefe y de su dungeon para las fichas ({@link BossBriefScreen}) y los resultados. Cada dungeon nueva solo
 * añade aquí su entrada (y su línea en {@code BossTravel.stageOf}); si falta, la ficha usa una genérica.
 */
public final class BossLore {
    private BossLore() {
    }

    /** Un ataque o fase del jefe. {@code danger}: se pinta en rojo (lo más importante de vigilar). */
    public record Attack(String name, String text, boolean danger) {
    }

    public record Lore(String name, String kind, String dungeon, String miniBoss, int accent, int bgTop, int bgBottom,
                       String desc, String strongHit, List<Attack> attacks, List<String> tips) {
    }

    private static final Lore FOREST = new Lore("Rey Brote", "Jefe del Bosque", "Dungeon del Bosque", "el Guardián de Musgo",
            0x7BD66B, 0xFF1D3A24, 0xFF0C1B10,
            "El rey ancestral del bosque: un enorme slime cubierto de hojas. Llama a sus gelatinitas y se las traga para curarse.",
            "el gran salto",
            List.of(new Attack("Gran salto", "Se agacha 1,5 s y deja un anillo en el suelo; luego salta y aplasta esa zona.", false),
                    new Attack("Baba", "Deja charcos que te ralentizan.", false),
                    new Attack("Gelatinitas", "Se las traga y SE CURA. Brillan justo antes de ser absorbidas.", true),
                    new Attack("Raíces", "Con poca vida: zarzas bajo tus pies, y se enfurece.", false)),
            List.of("Mata a las gelatinitas rápido: si brillan, el jefe está a punto de absorberlas.",
                    "Cuando se agacha, salta alto o aléjate del anillo del suelo.",
                    "Con poca vida no te quedes quieto: las raíces salen bajo tus pies.",
                    "Lleva comida y pociones; las mascotas con armadura te ayudan a aguantar."));

    private static final Lore FORGE = new Lore("Gran Herrero de Brasas", "Jefe de la Forja", "Dungeon de la Forja", "el Capataz",
            0xFF8A2A, 0xFF3A1A10, 0xFF140806,
            "Un coloso de hierro y lava con un martillo enorme. Forja soldados en su yunque y alterna entre «templado» y «al rojo».",
            "el martillazo",
            List.of(new Attack("Martillazo", "Levanta el martillo y marca un círculo en el suelo; después salta una onda de choque.", false),
                    new Attack("Chispas", "Círculos de brasas bajo tus pies.", false),
                    new Attack("Escorias", "Forja soldados en su yunque: acaba con ellas o te rodearán.", true),
                    new Attack("Mitad de vida", "Caen yunques y lanza anillos de brasas con un hueco.", false),
                    new Attack("Poca vida", "Erupciones de lava y doble onda en cada martillazo.", false)),
            List.of("Templado: casi no le haces daño; aprovecha para curarte y limpiar escorias.",
                    "Al rojo recibe más daño, pero también pega más fuerte: golpea y retrocede.",
                    "Sal del círculo del martillazo y salta la onda de choque.",
                    "No te quedes quieto: las chispas aparecen bajo tus pies."));

    private static final Lore ICE = new Lore("Lobo Ártico", "Jefe del Hielo", "Palacio de hielo", "el guardián del palacio",
            0x7FD6FF, 0xFF14304A, 0xFF060E18,
            "Un hombre lobo enorme, con capucha y capa negras y dos guadañas de escarcha. Es rápido, caza en manada y congela todo lo que toca.",
            "la embestida",
            List.of(new Attack("Embestida", "Se agacha y marca una línea en el hielo; después la recorre entera y te congela si te alcanza. Si se estrella contra un muro queda aturdido.", true),
                    new Attack("Tajos", "Guadañas cuerpo a cuerpo: el segundo tajo cruza en aspa y sube el frío.", false),
                    new Attack("Aullido", "Llama a una manada de lobeznos de escarcha. Repártete y acaba con ellos.", true),
                    new Attack("Ventisca", "Con poca vida: gira con las guadañas y la nieve tapa la vista. Corre a las zonas de calor del suelo (se mueven).", true),
                    new Attack("Luna de escarcha", "Con muy poca vida: el suelo se vuelve hielo liso y el lobo patina contra ti. Esquiva también los pinchos.", false)),
            List.of("Si te congelas, pulsa ESPACIO sin parar; un compañero puede ayudarte con clic derecho.",
                    "Cuando se estrella contra un muro queda aturdido y recibe más daño: es tu momento.",
                    "En la ventisca corre a las zonas de calor: no te protegen de las guadañas, solo del frío.",
                    "Lleva armadura completa: sus golpes sin protección pueden matarte de un solo golpe."));

    private static Lore generic(int stage) {
        return new Lore("Jefe de la etapa " + stage, "Jefe · Etapa " + stage, "Dungeon · Etapa " + stage, "el mini-jefe",
                0xB48CFF, 0xFF2A1D4A, 0xFF100A20, "Un jefe todavía sin descubrir.", "su golpe más fuerte",
                List.of(), List.of("Lleva buen equipo, comida y pociones."));
    }

    public static Lore of(int stage) {
        return switch (stage) {
            case 1 -> FOREST;
            case 2 -> FORGE;
            case 3 -> ICE;
            default -> generic(stage);
        };
    }

    /** Entidad del jefe para el retrato (null si esa etapa aún no tiene). */
    public static LivingEntity entity(int stage, ClientLevel level) {
        try {
            return switch (stage) {
                case 1 -> new ReyBrote(ModEntities.REY_BROTE.get(), level);
                case 2 -> new GranHerrero(ModEntities.GRAN_HERRERO.get(), level);
                case 3 -> new com.mundosdeaxelis.entity.LoboArtico(ModEntities.LOBO_ARTICO.get(), level);
                default -> null;
            };
        } catch (RuntimeException e) {
            return null;
        }
    }
}
