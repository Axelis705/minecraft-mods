package com.autoscoleccion.game;

import com.autoscoleccion.game.CarStyle.Shape;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.autoscoleccion.game.CarStyle.*;

/**
 * Los 30 cochecitos de la colección. Todas las marcas y modelos son inventados (ninguno copia un auto real): solo recuerdan
 * estilos (de ciudad, tuneados japoneses, muscle cars, superautos italianos, prototipos de resistencia...).
 */
public final class Cars {
    public static final int COUNT = 30;
    private static final List<CarDef> ALL = new ArrayList<>();

    private Cars() {
    }

    private static void add(String id, String name, int line, Rarity r, String blurb, CarStyle st) {
        ALL.add(new CarDef(ALL.size() + 1, id, name, line, r, blurb, st));
    }

    static {
        // ---------------------------------------------------------------- 1) Ruta Urbana: autos de la calle
        add("brisa_compacto", "Brisa Compacto", 0, Rarity.COMUN, "El hatchback de todos los días: cabe en cualquier lugar.",
                of(Shape.HATCH).colors(0x6FB7E8, 0x2B3440));
        add("taxi_sol", "Taxi Ciudad Sol", 0, Rarity.COMUN, "Amarillo, con letrero y siempre con prisa.",
                of(Shape.SEDAN).colors(0xF5C531, 0x1E1E1E).with(TAXI_SIGN | SIDE_STRIPE));
        add("ranchera", "Pickup Ranchera", 0, Rarity.COMUN, "Caja abierta, defensa de tubo y lodo en las llantas.",
                of(Shape.PICKUP).colors(0xB4472F, 0x3A3A3A).with(BULLBAR));
        add("aurora_familiar", "Aurora Familiar", 0, Rarity.COMUN, "Sedán tranquilo para los domingos.",
                of(Shape.SEDAN).colors(0xE6E2D8, 0x6B6F78).rim(0xA7ADB6));
        add("mini_rayo", "Mini Rayo", 0, Rarity.POCO_COMUN, "Chiquito, redondito y más rápido de lo que parece.",
                of(Shape.MINI).colors(0x43B85C, 0xF2F2F2).with(STRIPES | TWO_TONE));
        add("bolita_68", "Bolita '68", 0, Rarity.POCO_COMUN, "Un clásico redondo que sigue rodando por el barrio.",
                of(Shape.CLASSIC).colors(0x8FD3C8, 0xF0EAD6).rim(0xF0EAD6).with(CHROME));
        add("combi_viajera", "Combi Viajera", 0, Rarity.POCO_COMUN, "Dos colores, tabla de surf y ganas de playa.",
                of(Shape.VAN).colors(0xE8833A, 0xF4EEDC).with(TWO_TONE | SURFBOARD | ROOF_RACK));
        add("patrulla", "Patrulla Urbana", 0, Rarity.RARO, "Blanca y negra, con torreta que destella.",
                of(Shape.SEDAN).colors(0xF4F6F8, 0x14161A).with(SIREN | TWO_TONE));
        add("helados_paletin", "Helados Paletín", 0, Rarity.RARO, "La camioneta con el cono gigante que todos persiguen.",
                of(Shape.VAN).colors(0xF7B7D2, 0xFFFFFF).with(ICE_CREAM | TWO_TONE));
        add("lowrider_dorado", "Lowrider Dorado", 0, Rarity.EPICO, "Largo, bajito, rines dorados y pintura que brilla.",
                of(Shape.LOWRIDER).colors(0x7A1FA8, 0xE9C46A).rim(0xFFD34D).with(CHROME | SIDE_STRIPE));

        // ---------------------------------------------------------------- 2) Pista Nocturna: tuneados y neón
        add("kaze_r", "Kaze R", 1, Rarity.POCO_COMUN, "Coupé ligero que nació en los puertos de montaña.",
                of(Shape.COUPE).colors(0xF4F4F4, 0x1A1A1A).neon(0x30F0FF).with(SPOILER));
        add("neon_sprint", "Neón Sprint", 1, Rarity.POCO_COMUN, "Hatch picante con luces bajo el chasis.",
                of(Shape.HATCH).colors(0xFF3D7F, 0x1A1A24).neon(0xFF3DFF).with(SPOILER | STRIPES));
        add("drift_kitsune", "Drift Kitsune", 1, Rarity.RARO, "Naranja como un zorro y siempre de lado en las curvas.",
                of(Shape.COUPE).colors(0xFF7A1A, 0x101010).neon(0xFFB13D).with(BIG_WING | SIDE_STRIPE));
        add("tormenta_gt", "Tormenta GT", 1, Rarity.RARO, "Muscle car con franjas y un rugido que hace temblar los vidrios.",
                of(Shape.MUSCLE).colors(0x1F4FD8, 0xF4F4F4).with(STRIPES | HOOD_SCOOP | EXHAUST));
        add("mamba_negra", "Mamba Negra", 1, Rarity.RARO, "Negro mate, escape doble y faros que parecen ojos.",
                of(Shape.MUSCLE).colors(0x1A1A1E, 0xC8102E).rim(0x2A2A2E).with(HOOD_SCOOP | EXHAUST | SIDE_STRIPE));
        add("shogun_rs", "Shogun RS", 1, Rarity.RARO, "Turbo, alerón gigante y una calcomanía de cerezo.",
                of(Shape.COUPE).colors(0xFFFFFF, 0xD7263D).neon(0xD7263D).with(BIG_WING | NUMBER_DISC));
        add("rally_lince", "Rally Lince", 1, Rarity.EPICO, "Lodo hasta el techo y faros extra para correr de noche.",
                of(Shape.RALLY).colors(0x2E86DE, 0xF5F5F5).with(ROOF_LIGHTS | MUDFLAPS | NUMBER_DISC | SPOILER | ROOF_RACK));
        add("aurora_turbo", "Aurora Boreal Turbo", 1, Rarity.EPICO, "Pintura que cambia del verde al violeta con la luz.",
                of(Shape.COUPE).colors(0x22D3A6, 0x7B3FE4).neon(0x7B3FE4).with(SPOILER | TWO_TONE));
        add("fantasma_nocturno", "Fantasma Nocturno", 1, Rarity.EPICO, "Casi invisible de noche… hasta que enciende el neón.",
                of(Shape.GT).colors(0x23262E, 0x6CF2FF).glass(0x101418).rim(0x15171C).neon(0x6CF2FF).with(SIDE_STRIPE | SPOILER));
        add("dragon_turbo_x", "Dragón Turbo X", 1, Rarity.LEGENDARIO, "El rey de la pista nocturna: rojo, dorado y con alas de dragón.",
                of(Shape.WEDGE).colors(0xC1121F, 0xFFC23D).rim(0xFFC23D).neon(0xFF3D3D).with(BIG_WING | STRIPES | EXHAUST));

        // ---------------------------------------------------------------- 3) Leyendas del Circuito: superautos
        add("falcone_rosso", "Falcone Rosso", 2, Rarity.RARO, "Superauto italiano rojo pasión con motor central.",
                of(Shape.WEDGE).colors(0xD3121A, 0x1A1A1A).rim(0xD9D9D9).with(SPOILER));
        add("vortex_v12", "Vortex V12", 2, Rarity.RARO, "Doce cilindros y una cuña afilada como flecha.",
                of(Shape.WEDGE).colors(0xC6E81A, 0x1A1A1A).rim(0x2B2B2B).with(EXHAUST));
        add("imperial_gt", "Imperial GT", 2, Rarity.RARO, "Gran turismo largo y elegante para cruzar continentes.",
                of(Shape.GT).colors(0x0E3B2E, 0xD9C38A).rim(0xD9D9D9).with(CHROME));
        add("cometa_plata", "Cometa Plata", 2, Rarity.EPICO, "Aluminio pulido y una línea de luz de punta a punta.",
                of(Shape.GT).colors(0xC9CED6, 0x2B2F36).rim(0xE6E6E6).with(CHROME | SIDE_STRIPE));
        add("hyperion_e", "Hyperion E", 2, Rarity.EPICO, "Hiperauto eléctrico: silencioso y brutal.",
                of(Shape.HYPER).colors(0xF2F5FA, 0x0FA3FF).rim(0x1B1F26).neon(0x0FA3FF).with(SIDE_STRIPE | FIN));
        add("resistencia_24", "Prototipo Resistencia 24", 2, Rarity.EPICO, "Aguanta veinticuatro horas a fondo sin pestañear.",
                of(Shape.PROTO).colors(0x1D3557, 0xE63946).with(FIN | NUMBER_DISC | BIG_WING));
        add("monoplaza_astra", "Monoplaza Astra", 2, Rarity.EPICO, "Ruedas al aire, alerones enormes y 300 km/h en recta.",
                of(Shape.FORMULA).colors(0x0B6E4F, 0xFFD60A).rim(0x1A1A1A).with(BIG_WING | HELMET));
        add("solaris_concept", "Solaris Concept", 2, Rarity.LEGENDARIO, "Un auto que parece venir del futuro, con cúpula de cristal.",
                of(Shape.HYPER).colors(0xFF9F1C, 0xFFFFFF).glass(0x3BD1FF).neon(0xFFD166).with(CANOPY | FIN));
        add("titan_hiper", "Titán Hiper", 2, Rarity.LEGENDARIO, "Dieciséis cilindros, dos tonos y la cima de la colección.",
                of(Shape.HYPER).colors(0x13294B, 0x8EA8C3).rim(0xC0C0C0).with(TWO_TONE | BIG_WING | EXHAUST));
        add("corona_oro", "Corona de Oro", 2, Rarity.LEGENDARIO, "Hecho de oro de punta a punta. Brilla incluso en la oscuridad.",
                of(Shape.WEDGE).colors(0xF4C430, 0xFFF1B0).rim(0xFFF1B0).glass(0x6B4A12).with(GOLD_SPARKLE | CHROME | SPOILER));
    }

    public static List<CarDef> all() {
        return Collections.unmodifiableList(ALL);
    }

    public static CarDef get(int index) {
        return ALL.get(index);
    }

    public static CarDef byId(String id) {
        for (CarDef c : ALL) {
            if (c.id.equals(id)) {
                return c;
            }
        }
        return null;
    }
}
