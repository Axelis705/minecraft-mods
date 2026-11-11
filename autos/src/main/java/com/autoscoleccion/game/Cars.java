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
        // Cada auto tiene su propia silueta y detalles. Se inspiran en tipos de autos reales y épocas (compacto alemán, taxi
        // neoyorquino, muscle car americano, deportivo japonés de los 90, superauto italiano...), pero los nombres, marcas y
        // diseños exactos son inventados.

        // ---------------------------------------------------------------- 1) Ruta Urbana: autos de la calle
        add("brisa_compacto", "Brisa Compacto", 0, Rarity.COMUN, "El hatchback de todos los días: cabe en cualquier lugar.",
                of(Shape.HATCH).dims(12.8F, 6.4F, 1.1F, 2.5F).cab(1.9F, -5.6F, 2.4F).glassSlope(1.6F, 0.5F, 0.45F).slopes(1.3F, 0.15F)
                        .colors(0x6FB7E8, 0x2B3440).form(0.8F, 0, 0.15F, 0).lights(HEAD_RECT, TAIL_RECT).rims(RIM_SEVEN, 0xC8CCD2)
                        .details(D_WIPERS | D_HOOD_LINES));
        add("taxi_sol", "Taxi Ciudad Sol", 0, Rarity.COMUN, "Amarillo, cuadrado, con letrero y siempre con prisa.",
                of(Shape.SEDAN).dims(16.0F, 6.7F, 1.2F, 2.6F).cab(1.6F, -3.6F, 2.3F).glassSlope(1.2F, 1.0F, 0.3F).slopes(0.9F, 0.5F)
                        .colors(0xF5C531, 0x1E1E1E).form(0.5F, 0, 0, 0).roof(0.05F, 0.4F, 0.08F).lights(HEAD_RECT, TAIL_RECT).rims(RIM_HUB, 0xB8BCC2)
                        .with(TAXI_SIGN | SIDE_STRIPE).details(D_WIPERS | D_CHROME_TRIM));
        add("ranchera", "Pickup Ranchera", 0, Rarity.COMUN, "Caja abierta, defensa de tubo, parrilla cromada y lodo en las llantas.",
                of(Shape.PICKUP).dims(16.0F, 7.0F, 2.0F, 2.9F).cab(2.6F, -1.6F, 2.5F).glassSlope(0.9F, 0.15F, 0.2F).slopes(0.5F, 0)
                        .colors(0xB4472F, 0x3A3A3A).form(0.45F, 0, 0, 0).flares(0.3F, 0.3F).lights(HEAD_RECT, TAIL_VERT).rims(RIM_SIX, 0xA9AEB6)
                        .with(BULLBAR | MUDFLAPS).details(D_BIG_GRILLE | D_FOG | D_WIPERS));
        add("aurora_familiar", "Aurora Familiar", 0, Rarity.COMUN, "Camioneta familiar larga y cuadrada para los domingos.",
                of(Shape.WAGON).dims(15.4F, 6.6F, 1.15F, 2.5F).cab(2.3F, -6.9F, 2.25F).glassSlope(1.5F, 0.25F, 0.35F).slopes(1.2F, 0.1F)
                        .colors(0x2F4A6B, 0x6B6F78).form(0.6F, 0, 0, 0).lights(HEAD_RECT, TAIL_VERT).rims(RIM_MULTI, 0xA7ADB6)
                        .details(D_RAILS | D_WIPERS | D_CHROME_TRIM));
        add("mini_rayo", "Mini Rayo", 0, Rarity.POCO_COMUN, "Chiquito, redondito, techo blanco y más rápido de lo que parece.",
                of(Shape.MINI).dims(10.0F, 5.9F, 1.0F, 2.4F).cab(1.6F, -4.6F, 2.5F).glassSlope(1.0F, 0.15F, 0.15F).slopes(0.9F, 0)
                        .colors(0x2E8B57, 0xF2F2F2).form(1.3F, 0, 0, 0).flares(0.2F, 0.2F).roof(0.1F, 0.3F, 0).lights(HEAD_ROUND, TAIL_VERT)
                        .rims(RIM_MULTI, 0xE8E8E8).with(STRIPES | TWO_TONE).details(D_WIPERS));
        add("bolita_68", "Bolita '68", 0, Rarity.POCO_COMUN, "Un clásico redondo que sigue rodando por el barrio.",
                of(Shape.CLASSIC).dims(12.6F, 6.2F, 1.4F, 2.6F).cab(1.2F, -3.8F, 2.6F).glassSlope(1.0F, 2.2F, 0.8F).slopes(2.4F, 2.2F)
                        .colors(0x8FD3C8, 0xF0EAD6).form(1.5F, 0, 0, 0).flares(0.45F, 0.5F).roof(0.45F, 0.42F, 0).lights(HEAD_ROUND, TAIL_ROUND1)
                        .rims(RIM_HUB, 0xF0EAD6).with(CHROME));
        add("combi_viajera", "Combi Viajera", 0, Rarity.POCO_COMUN, "Dos colores, tabla de surf y ganas de playa.",
                of(Shape.VAN).dims(13.5F, 6.6F, 1.2F, 2.8F).cab(4.8F, -6.6F, 3.4F).glassSlope(1.1F, 0.1F, 0.3F).slopes(0.4F, 0)
                        .colors(0xE8833A, 0xF4EEDC).form(0.9F, 0, 0, 0).roof(0.25F, 0.42F, 0).lights(HEAD_ROUND, TAIL_ROUND1).rims(RIM_HUB, 0xF4EEDC)
                        .with(TWO_TONE | SURFBOARD | ROOF_RACK));
        add("patrulla", "Patrulla Urbana", 0, Rarity.RARO, "Sedán musculoso blanco y negro, con defensa de empuje y torreta.",
                of(Shape.SEDAN).dims(16.2F, 7.0F, 1.1F, 2.6F).cab(1.0F, -3.9F, 2.0F).glassSlope(2.0F, 1.7F, 0.5F).slopes(1.5F, 0.3F)
                        .colors(0xF4F6F8, 0x14161A).form(0.6F, 0, 0.1F, 0).roof(-1, 0.45F, 0.1F).lights(HEAD_SLIM, TAIL_BAR).rims(RIM_MULTI, 0x2A2C30)
                        .with(SIREN | TWO_TONE | BULLBAR).details(D_WIPERS | D_FOG));
        add("helados_paletin", "Helados Paletín", 0, Rarity.RARO, "La camioneta de reparto con el cono gigante que todos persiguen.",
                of(Shape.BOXVAN).dims(14.5F, 7.0F, 1.5F, 2.6F).cab(4.4F, -7.1F, 4.2F).glassSlope(0.8F, 0, 0).slopes(0.8F, 0)
                        .colors(0xF7B7D2, 0xFFFFFF).lights(HEAD_ROUND, TAIL_VERT).rims(RIM_STEEL, 0xEDEDED).with(ICE_CREAM | TWO_TONE)
                        .details(D_WIPERS));
        add("lowrider_dorado", "Lowrider Dorado", 0, Rarity.EPICO, "Largo, bajito, rines de rayos dorados y pintura que brilla.",
                of(Shape.LOWRIDER).dims(17.0F, 6.8F, 0.5F, 2.3F).cab(1.4F, -3.8F, 2.0F).glassSlope(1.5F, 1.8F, 0.4F).slopes(0.8F, 0.6F)
                        .colors(0x7A1FA8, 0xE9C46A).form(0.6F, 0, 0, 0).roof(0.05F, 0.45F, 0.05F).lights(HEAD_QUAD, TAIL_ROUND2).rims(RIM_WIRE, 0xFFD34D)
                        .with(CHROME | SIDE_STRIPE).details(D_CHROME_TRIM | D_BIG_GRILLE));

        // ---------------------------------------------------------------- 2) Pista Nocturna: tuneados japoneses, muscle y neón
        add("kaze_r", "Kaze R", 1, Rarity.POCO_COMUN, "Coupé noventero de faros escondidos que nació en los puertos de montaña.",
                of(Shape.COUPE).dims(14.0F, 6.6F, 0.9F, 2.2F).cab(1.2F, -4.6F, 1.9F).glassSlope(2.2F, 2.8F, 0.5F).slopes(2.0F, 0.3F)
                        .colors(0xF4F4F4, 0x1A1A1A).form(0.9F, 0, 0.2F, 0.18F).lights(HEAD_POPUP, TAIL_SLIM).rims(RIM_TWIN5, 0xC8CCD2)
                        .neon(0x30F0FF).with(SPOILER).details(D_SKIRTS));
        add("neon_sprint", "Neón Sprint", 1, Rarity.POCO_COMUN, "Hatch de carreras con alerón enorme, tres escapes y luces bajo el chasis.",
                of(Shape.HATCH).dims(13.4F, 6.8F, 0.85F, 2.4F).cab(1.6F, -5.3F, 2.2F).glassSlope(2.0F, 0.9F, 0.5F).slopes(1.9F, 0.3F)
                        .colors(0xFF3D7F, 0x1A1A24).form(0.7F, 0, 0.1F, 0).flares(0.35F, 0.35F).lights(HEAD_ANGULAR, TAIL_C).rims(RIM_MESH, 0x1E1F24)
                        .neon(0xFF3DFF).with(BIG_WING | STRIPES).details(D_SPLITTER | D_DIFFUSER | D_HOOD_VENTS | D_CENTER_EXH | D_SKIRTS));
        add("drift_kitsune", "Drift Kitsune", 1, Rarity.RARO, "Coupé ochentero cuadrado y ligero, siempre de lado en las curvas.",
                of(Shape.COUPE).dims(13.6F, 6.2F, 1.0F, 2.4F).cab(1.0F, -4.2F, 2.1F).glassSlope(1.6F, 1.3F, 0.4F).slopes(1.0F, 0.3F)
                        .colors(0xFF7A1A, 0x101010).form(0.4F, 0, 0, 0).flares(0.25F, 0.3F).roof(0.04F, 0.42F, 0.05F).lights(HEAD_POPUP, TAIL_BAR)
                        .rims(RIM_DISH, 0xC9A55A).neon(0xFFB13D).with(BIG_WING | SIDE_STRIPE).details(D_SPLITTER));
        add("tormenta_gt", "Tormenta GT", 1, Rarity.RARO, "Muscle car fastback de los 60 con franjas y un rugido que hace temblar los vidrios.",
                of(Shape.MUSCLE).dims(15.2F, 6.8F, 1.0F, 2.6F).cab(0.4F, -4.6F, 1.9F).glassSlope(1.8F, 3.2F, 0.5F).slopes(0.8F, 0.2F)
                        .colors(0x1F4FD8, 0xF4F4F4).form(0.65F, 0, 0, 0.2F).flares(0.25F, 0.35F).lights(HEAD_ROUND, TAIL_VERT).rims(RIM_DISH, 0xC8CCD2)
                        .with(STRIPES | HOOD_SCOOP | EXHAUST).details(D_SIDE_INTAKE | D_CHROME_TRIM));
        add("mamba_negra", "Mamba Negra", 1, Rarity.RARO, "Muscle car ancho y cuadrado, negro mate, con cuatro faros que parecen ojos.",
                of(Shape.MUSCLE).dims(16.2F, 7.3F, 1.0F, 2.8F).cab(0.2F, -3.9F, 1.85F).glassSlope(1.6F, 1.6F, 0.4F).slopes(0.7F, 0.25F)
                        .colors(0x1A1A1E, 0xC8102E).form(0.45F, 0, 0, 0).roof(0.03F, 0.45F, 0.12F).lights(HEAD_QUAD, TAIL_BAR).rims(RIM_MULTI, 0x2A2A2E)
                        .caliper(0xC8102E).with(HOOD_SCOOP | EXHAUST | SIDE_STRIPE).details(D_HOOD_LINES | D_SPLITTER));
        add("shogun_rs", "Shogun RS", 1, Rarity.RARO, "Deportivo japonés de los 90, curvo, con alerón de aro y calaveras redondas.",
                of(Shape.COUPE).dims(14.6F, 6.9F, 0.85F, 2.3F).cab(0.8F, -3.9F, 1.95F).glassSlope(2.4F, 2.4F, 0.6F).slopes(2.6F, 0.5F)
                        .colors(0xFFFFFF, 0xD7263D).form(1.35F, 0.15F, 0, 0).flares(0.3F, 0.4F).lights(HEAD_SLIM, TAIL_ROUND2).rims(RIM_TWIN5, 0xD9D9D9)
                        .neon(0xD7263D).with(BIG_WING | NUMBER_DISC).details(D_RACE_DECALS));
        add("rally_lince", "Rally Lince", 1, Rarity.EPICO, "Sedán de rally con toma en el cofre, faros extra y rines dorados.",
                of(Shape.RALLY).dims(13.6F, 6.6F, 1.6F, 2.5F).cab(1.6F, -4.0F, 2.3F).glassSlope(1.6F, 1.2F, 0.4F).slopes(1.4F, 0.4F)
                        .colors(0x2E86DE, 0xF5F5F5).form(0.8F, 0, 0, 0).flares(0.35F, 0.35F).lights(HEAD_RECT, TAIL_RECT).rims(RIM_SIX, 0xD8B04A)
                        .with(ROOF_LIGHTS | MUDFLAPS | NUMBER_DISC | SPOILER | HOOD_SCOOP).details(D_FOG | D_RACE_DECALS | D_WIPERS));
        add("aurora_turbo", "Aurora Boreal Turbo", 1, Rarity.EPICO, "Coupé de curvas suaves y faros escondidos; la pintura cambia del verde al violeta.",
                of(Shape.COUPE).dims(14.0F, 7.0F, 0.8F, 2.1F).cab(0.9F, -3.4F, 1.8F).glassSlope(2.4F, 2.4F, 0.7F).slopes(2.8F, 0.6F)
                        .colors(0x22D3A6, 0x7B3FE4).form(1.6F, 0.25F, 0, 0.12F).flares(0.4F, 0.45F).lights(HEAD_POPUP, TAIL_BAR).rims(RIM_TWIN5, 0xD9D9D9)
                        .neon(0x7B3FE4).with(SPOILER | TWO_TONE));
        add("fantasma_nocturno", "Fantasma Nocturno", 1, Rarity.EPICO, "Gran turismo japonés musculoso, casi invisible de noche… hasta que enciende el neón.",
                of(Shape.GT).dims(15.2F, 7.1F, 0.9F, 2.5F).cab(1.0F, -4.4F, 2.1F).glassSlope(2.2F, 2.6F, 0.6F).slopes(2.0F, 0.4F)
                        .colors(0x23262E, 0x6CF2FF).form(0.65F, 0, 0, 0).flares(0.35F, 0.4F).lights(HEAD_ANGULAR, TAIL_ROUND2).rims(RIM_MULTI, 0x15171C)
                        .glass(0x101418).neon(0x6CF2FF).with(SIDE_STRIPE | SPOILER).details(D_QUAD_EXH | D_HOOD_LINES | D_DIFFUSER | D_SIDE_INTAKE));
        add("dragon_turbo_x", "Dragón Turbo X", 1, Rarity.LEGENDARIO, "Cuña ochentera de puro ángulo: faros escondidos, tomas enormes y alas de dragón.",
                of(Shape.WEDGE).dims(14.6F, 7.5F, 0.7F, 2.0F).cab(1.6F, -3.0F, 1.9F).glassSlope(3.0F, 2.0F, 1.0F).slopes(4.0F, 0.4F)
                        .colors(0xC1121F, 0xFFC23D).form(0.3F, 0, 0.35F, 0).flares(0.45F, 0.65F).lights(HEAD_POPUP, TAIL_RECT).rims(RIM_TURBINE, 0xFFC23D)
                        .neon(0xFF3D3D).with(BIG_WING | STRIPES | EXHAUST).details(D_SIDE_INTAKE | D_ROOF_SCOOP | D_LOUVERS));

        // ---------------------------------------------------------------- 3) Leyendas del Circuito: superautos y prototipos
        add("falcone_rosso", "Falcone Rosso", 2, Rarity.RARO, "Superauto italiano rojo pasión con motor central a la vista.",
                of(Shape.HYPER).dims(14.8F, 7.2F, 0.7F, 2.1F).cab(1.4F, -2.6F, 1.75F).glassSlope(2.6F, 2.2F, 0.9F).slopes(3.4F, 0.5F)
                        .colors(0xD3121A, 0x1A1A1A).form(1.4F, 0.35F, 0.15F, 0).flares(0.4F, 0.55F).lights(HEAD_SLIM, TAIL_ROUND2).rims(RIM_Y, 0xD9D9D9)
                        .caliper(0xFFD23F).with(SPOILER).details(D_SIDE_INTAKE | D_ENGINE_GLASS | D_DIFFUSER | D_CENTER_EXH));
        add("vortex_v12", "Vortex V12", 2, Rarity.RARO, "Doce cilindros y una cuña afilada como flecha, toda de ángulos.",
                of(Shape.WEDGE).dims(15.2F, 7.6F, 0.65F, 1.95F).cab(1.4F, -3.0F, 1.75F).glassSlope(3.2F, 2.0F, 1.1F).slopes(4.0F, 0.3F)
                        .colors(0xC6E81A, 0x1A1A1A).form(0.25F, 0, 0.25F, 0).flares(0.5F, 0.7F).lights(HEAD_ANGULAR, TAIL_C).rims(RIM_Y, 0x2B2B2B)
                        .with(EXHAUST).details(D_SIDE_INTAKE | D_ENGINE_GLASS | D_LOUVERS | D_DIFFUSER | D_SPLITTER | D_CENTER_EXH));
        add("imperial_gt", "Imperial GT", 2, Rarity.RARO, "Gran turismo de cofre larguísimo y parrilla de lujo para cruzar continentes.",
                of(Shape.GT).dims(16.2F, 7.0F, 0.9F, 2.4F).cab(-0.8F, -5.0F, 2.0F).glassSlope(2.2F, 2.8F, 0.7F).slopes(2.2F, 0.6F)
                        .colors(0x0E3B2E, 0xD9C38A).form(1.2F, 0.15F, 0, 0.08F).flares(0.3F, 0.4F).lights(HEAD_SLIM, TAIL_SLIM).rims(RIM_MULTI, 0xD9D9D9)
                        .with(CHROME).details(D_BIG_GRILLE | D_CHROME_TRIM | D_QUAD_EXH | D_HOOD_VENTS));
        add("cometa_plata", "Cometa Plata", 2, Rarity.EPICO, "Coupé plateado de los 50, de cejas marcadas y parrilla ancha: una joya de museo.",
                of(Shape.GT).dims(14.4F, 6.6F, 1.1F, 2.4F).cab(0.4F, -3.4F, 2.0F).glassSlope(1.6F, 2.2F, 0.8F).slopes(2.0F, 1.5F)
                        .colors(0xC9CED6, 0x2B2F36).form(1.7F, 0, 0, 0).flares(0.45F, 0.4F).roof(0.3F, 0.45F, 0).lights(HEAD_ROUND, TAIL_VERT)
                        .rims(RIM_HUB, 0xE6E6E6).with(CHROME).details(D_OVAL_GRILLE | D_HOOD_LINES | D_CHROME_TRIM));
        add("hyperion_e", "Hyperion E", 2, Rarity.EPICO, "Hiperauto eléctrico: silencioso, brutal y con barras de luz de punta a punta.",
                of(Shape.HYPER).dims(15.0F, 7.2F, 0.65F, 2.0F).cab(1.6F, -2.6F, 1.75F).glassSlope(2.6F, 2.4F, 1.0F).slopes(3.6F, 0.4F)
                        .colors(0xF2F5FA, 0x0FA3FF).form(1.2F, 0.2F, 0.1F, 0).lights(HEAD_BAR, TAIL_BAR).rims(RIM_TURBINE, 0x1B1F26)
                        .neon(0x0FA3FF).with(SIDE_STRIPE | FIN).details(D_DIFFUSER | D_SPLITTER | D_SIDE_INTAKE | D_LIGHT_BAR));
        add("resistencia_24", "Prototipo Resistencia 24", 2, Rarity.EPICO, "Prototipo de carreras de 24 horas: aleta, alerón y número en la puerta.",
                of(Shape.PROTO).colors(0x1D3557, 0xE63946).lights(HEAD_SLIM, TAIL_BAR).rims(RIM_MULTI, 0x1A1A1A)
                        .with(FIN | NUMBER_DISC | BIG_WING).details(D_SPLITTER | D_DIFFUSER | D_RACE_DECALS | D_ROOF_SCOOP));
        add("monoplaza_astra", "Monoplaza Astra", 2, Rarity.EPICO, "Ruedas al aire, alerones enormes y 300 km/h en recta.",
                of(Shape.FORMULA).colors(0x0B6E4F, 0xFFD60A).rim(0x1A1A1A).with(BIG_WING | HELMET));
        add("solaris_concept", "Solaris Concept", 2, Rarity.LEGENDARIO, "Un auto que parece venir del futuro, con cúpula de cristal.",
                of(Shape.HYPER).colors(0xFF9F1C, 0xFFFFFF).glass(0x3BD1FF).form(1.6F, 0.3F, 0, 0).lights(HEAD_BAR, TAIL_BAR).rims(RIM_TURBINE, 0xF2F2F2)
                        .neon(0xFFD166).with(CANOPY | FIN).details(D_LIGHT_BAR | D_DIFFUSER));
        add("titan_hiper", "Titán Hiper", 2, Rarity.LEGENDARIO, "Dieciséis cilindros, curva lateral de otro color y barra de luz atrás: la cima de la colección.",
                of(Shape.HYPER).dims(15.6F, 7.4F, 0.7F, 2.2F).cab(1.2F, -2.8F, 1.8F).glassSlope(2.8F, 2.6F, 0.9F).slopes(3.2F, 0.3F)
                        .colors(0x13294B, 0x8EA8C3).form(1.3F, 0.2F, 0.1F, 0).flares(0.4F, 0.55F).lights(HEAD_SLIM, TAIL_BAR).rims(RIM_Y, 0xC0C0C0)
                        .with(TWO_TONE | BIG_WING).details(D_C_LINE | D_OVAL_GRILLE | D_CENTER_EXH | D_DIFFUSER | D_ROOF_SCOOP | D_LIGHT_BAR));
        add("corona_oro", "Corona de Oro", 2, Rarity.LEGENDARIO, "Hiperauto hecho de oro de punta a punta. Brilla incluso en la oscuridad.",
                of(Shape.HYPER).dims(15.0F, 7.2F, 0.65F, 2.0F).cab(1.6F, -2.4F, 1.7F).glassSlope(2.6F, 2.0F, 1.0F).slopes(3.4F, 0.4F)
                        .colors(0xF4C430, 0xFFF1B0).form(1.0F, 0.25F, 0.15F, 0).flares(0.4F, 0.6F).lights(HEAD_SLIM, TAIL_ROUND2).rims(RIM_TURBINE, 0xFFF1B0)
                        .glass(0x6B4A12).with(GOLD_SPARKLE | CHROME | SPOILER).details(D_SIDE_INTAKE | D_DIFFUSER | D_SPLITTER | D_CENTER_EXH));
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
