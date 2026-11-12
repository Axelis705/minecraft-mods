package com.autoscoleccion.game;

import com.autoscoleccion.game.CarStyle.Shape;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.autoscoleccion.game.CarStyle.*;

/**
 * Los 40 cochecitos de la colección. Todas las marcas y modelos son inventados (ninguno copia un auto real): solo recuerdan
 * estilos (de ciudad, tuneados japoneses, muscle cars, superautos italianos, prototipos de resistencia...).
 */
public final class Cars {
    public static final int COUNT = 40;
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
        // (cada uno dibujado a mano: perfil de costado, planta y techo; medidas en píxeles, de atrás hacia adelante)
        add("kaze_r", "Kaze R", 1, Rarity.POCO_COMUN, "Coupé noventero de faros escondidos que nació en los puertos de montaña.",
                of(Shape.COUPE).dims(14.4F, 6.2F, 0.75F, 1.95F).wheels(1.25F, 1.2F, 4.5F, -4.1F, 0.12F)
                        .top(-7.2F, 2.3F, -6.7F, 2.6F, -4.8F, 2.68F, -2.0F, 2.72F, 1.8F, 2.62F, 4.0F, 2.38F, 6.0F, 2.05F, 6.9F, 1.78F, 7.2F, 1.45F)
                        .bottom(-7.2F, 1.0F, -6.5F, 0.75F, 6.5F, 0.75F, 7.2F, 1.0F)
                        .plan(-7.2F, 2.55F, -6.4F, 2.95F, -4.0F, 3.08F, 2.0F, 3.04F, 5.5F, 2.95F, 6.8F, 2.6F, 7.2F, 2.15F)
                        .roofLine(2, 3, -5.6F, 0, -3.4F, 3.45F, -1.5F, 4.08F, -0.1F, 4.12F, 1.8F, 0)
                        .roof(-1, 0.6F, 0).glassSlope(0, 0, 0.4F).humps(0.06F, 0.1F).dome(0.05F).form(0.9F, 0, 0, 0.12F)
                        .colors(0xF4F4F4, 0x1A1A1A).lights(HEAD_POPUP, TAIL_SLIM).rims(RIM_TWIN5, 0xC8CCD2).neon(0x30F0FF).with(SPOILER)
                        .details(D_SKIRTS | D_WIPERS));
        add("neon_sprint", "Neón Sprint", 1, Rarity.POCO_COMUN, "Hatch de carreras con alerón enorme, tres escapes y luces bajo el chasis.",
                of(Shape.HATCH).dims(14.4F, 6.6F, 0.7F, 2.25F).wheels(1.35F, 1.35F, 4.7F, -4.4F, 0.25F)
                        .top(-7.2F, 2.5F, -6.8F, 2.95F, -6.2F, 3.05F, 0F, 2.95F, 2.4F, 2.9F, 4.6F, 2.6F, 6.3F, 2.2F, 7.0F, 1.85F, 7.2F, 1.5F)
                        .bottom(-7.2F, 1.0F, -6.4F, 0.7F, 6.6F, 0.7F, 7.2F, 0.95F)
                        .plan(-7.2F, 2.6F, -6.5F, 3.05F, -4.4F, 3.25F, 0F, 3.1F, 4.7F, 3.25F, 6.6F, 2.9F, 7.2F, 2.3F)
                        .roofLine(1, 3, -6.4F, 0, -5.6F, 4.5F, -2.4F, 4.75F, 0.4F, 4.6F, 2.5F, 0)
                        .roof(-1, 0.55F, 0).glassSlope(0, 0, 0.45F).humps(0.12F, 0.15F).dome(0.06F).form(0.75F, 0, 0, 0)
                        .colors(0xFF3D7F, 0x1A1A24).lights(HEAD_ANGULAR, TAIL_C).rims(RIM_MESH, 0x1E1F24).neon(0xFF3DFF).with(BIG_WING | STRIPES)
                        .details(D_SPLITTER | D_DIFFUSER | D_HOOD_VENTS | D_CENTER_EXH | D_SKIRTS | D_WIPERS));
        add("drift_kitsune", "Drift Kitsune", 1, Rarity.RARO, "Coupé ochentero cuadrado y ligero, siempre de lado en las curvas.",
                of(Shape.COUPE).dims(13.8F, 6.0F, 0.9F, 2.1F).wheels(1.25F, 1.2F, 4.4F, -4.0F, 0.15F)
                        .top(-6.9F, 2.6F, -6.6F, 2.95F, -3.5F, 3.05F, 1.6F, 3.0F, 4.5F, 2.92F, 6.5F, 2.78F, 6.9F, 2.4F)
                        .bottom(-6.9F, 1.15F, -6.3F, 0.9F, 6.3F, 0.9F, 6.9F, 1.15F)
                        .plan(-6.9F, 2.75F, -6.5F, 2.95F, 6.4F, 2.95F, 6.9F, 2.7F)
                        .roofLine(2, 3, -5.0F, 0, -4.2F, 3.85F, -2.8F, 4.55F, 0.1F, 4.6F, 1.7F, 0)
                        .roof(0.03F, 0.45F, 0.04F).glassSlope(0, 0, 0.35F).form(0.4F, 0, 0, 0).flares(0.25F, 0.3F)
                        .colors(0xFF7A1A, 0x101010).lights(HEAD_POPUP, TAIL_BAR).rims(RIM_DISH, 0xC9A55A).neon(0xFFB13D).with(BIG_WING | SIDE_STRIPE)
                        .details(D_SPLITTER | D_WIPERS));
        add("tormenta_gt", "Tormenta GT", 1, Rarity.RARO, "Muscle car fastback de los 60 con franjas y un rugido que hace temblar los vidrios.",
                of(Shape.MUSCLE).dims(15.2F, 6.6F, 0.9F, 2.3F).wheels(1.4F, 1.35F, 4.6F, -4.4F, 0.15F)
                        .top(-7.6F, 2.85F, -7.2F, 3.15F, -5.0F, 3.1F, -3.6F, 3.15F, 0.2F, 3.15F, 3.0F, 3.1F, 6.0F, 2.95F, 7.2F, 2.75F, 7.6F, 2.3F)
                        .bottom(-7.6F, 1.3F, -7.0F, 0.95F, 7.0F, 0.95F, 7.6F, 1.4F)
                        .plan(-7.6F, 2.9F, -7.0F, 3.15F, -4.6F, 3.3F, -1.0F, 3.15F, 4.6F, 3.25F, 7.0F, 3.05F, 7.6F, 2.6F)
                        .roofLine(2, 3, -5.4F, 0, -3.6F, 3.85F, -1.6F, 4.6F, 0.0F, 4.62F, 1.9F, 0)
                        .roof(-1, 0.5F, 0.05F).glassSlope(0, 0, 0.45F).humps(0.1F, 0.18F).dome(0.06F).form(0.65F, 0, 0, 0)
                        .colors(0x1F4FD8, 0xF4F4F4).lights(HEAD_ROUND, TAIL_VERT).rims(RIM_DISH, 0xC8CCD2).with(STRIPES | HOOD_SCOOP | EXHAUST)
                        .details(D_SIDE_INTAKE | D_CHROME_TRIM | D_WIPERS));
        add("mamba_negra", "Mamba Negra", 1, Rarity.RARO, "Muscle car ancho y cuadrado, negro mate, con cuatro faros que parecen ojos.",
                of(Shape.MUSCLE).dims(16.4F, 7.2F, 1.0F, 2.5F).wheels(1.5F, 1.5F, 5.0F, -4.9F, 0.2F)
                        .top(-8.2F, 3.2F, -7.9F, 3.52F, -4.0F, 3.55F, 1.4F, 3.5F, 4.5F, 3.45F, 7.6F, 3.35F, 8.2F, 2.95F)
                        .bottom(-8.2F, 1.4F, -7.6F, 1.0F, 7.6F, 1.0F, 8.2F, 1.4F)
                        .plan(-8.2F, 3.3F, -7.6F, 3.55F, 7.6F, 3.55F, 8.2F, 3.25F)
                        .roofLine(2, 3, -4.4F, 0, -3.1F, 4.45F, -1.8F, 5.05F, 0.0F, 5.05F, 1.6F, 0)
                        .roof(0.02F, 0.5F, 0.1F).glassSlope(0, 0, 0.35F).humps(0, 0.08F).dome(0.08F).form(0.45F, 0, 0, 0)
                        .colors(0x1A1A1E, 0xC8102E).lights(HEAD_QUAD, TAIL_BAR).rims(RIM_MULTI, 0x2A2A2E).caliper(0xC8102E)
                        .with(HOOD_SCOOP | EXHAUST | SIDE_STRIPE).details(D_HOOD_LINES | D_SPLITTER | D_WIPERS));
        add("shogun_rs", "Shogun RS", 1, Rarity.RARO, "Deportivo japonés de los 90, curvo, con alerón de aro y calaveras redondas.",
                of(Shape.COUPE).dims(14.8F, 6.6F, 0.75F, 2.0F).wheels(1.35F, 1.35F, 4.6F, -4.3F, 0.2F)
                        .top(-7.4F, 2.6F, -6.9F, 2.88F, -4.6F, 2.95F, -2.6F, 2.95F, 1.2F, 2.85F, 3.5F, 2.6F, 5.8F, 2.15F, 7.0F, 1.75F, 7.4F, 1.35F)
                        .bottom(-7.4F, 1.1F, -6.8F, 0.75F, 6.8F, 0.75F, 7.4F, 1.0F)
                        .plan(-7.4F, 2.75F, -6.6F, 3.2F, -4.2F, 3.35F, -1.5F, 3.08F, 2.0F, 3.12F, 4.6F, 3.3F, 6.6F, 2.9F, 7.4F, 2.2F)
                        .roofLine(2, 3, -4.6F, 0, -3.2F, 3.75F, -1.6F, 4.25F, -0.2F, 4.25F, 1.4F, 0)
                        .roof(-1, 0.6F, 0).glassSlope(0, 0, 0.5F).humps(0.15F, 0.22F).dome(0.06F).form(1.4F, 0, 0, 0).teardrop(0.2F)
                        .colors(0xFFFFFF, 0xD7263D).lights(HEAD_SLIM, TAIL_ROUND2).rims(RIM_TWIN5, 0xD9D9D9).neon(0xD7263D).with(BIG_WING | NUMBER_DISC)
                        .details(D_RACE_DECALS | D_WIPERS));
        add("rally_lince", "Rally Lince", 1, Rarity.EPICO, "Sedán de rally con toma en el cofre, faros extra y rines dorados.",
                of(Shape.RALLY).dims(14.2F, 6.4F, 1.3F, 2.2F).wheels(1.45F, 1.3F, 4.4F, -4.2F, 0.22F)
                        .top(-7.1F, 2.95F, -6.6F, 3.45F, -4.2F, 3.6F, 1.6F, 3.55F, 4.2F, 3.4F, 6.4F, 3.1F, 7.1F, 2.75F)
                        .bottom(-7.1F, 1.75F, -6.4F, 1.3F, 6.4F, 1.3F, 7.1F, 1.7F)
                        .plan(-7.1F, 2.8F, -6.5F, 3.1F, -4.2F, 3.25F, 0F, 3.1F, 4.4F, 3.25F, 6.4F, 3.15F, 7.1F, 2.75F)
                        .roofLine(2, 3, -4.4F, 0, -3.4F, 4.4F, -2.2F, 5.15F, 0.2F, 5.15F, 1.8F, 0)
                        .roof(-1, 0.45F, 0).glassSlope(0, 0, 0.4F).humps(0.1F, 0.12F).dome(0.05F).form(0.8F, 0, 0, 0).flares(0.3F, 0.3F)
                        .colors(0x2E86DE, 0xF5F5F5).lights(HEAD_RECT, TAIL_RECT).rims(RIM_SIX, 0xD8B04A)
                        .with(ROOF_LIGHTS | MUDFLAPS | NUMBER_DISC | SPOILER | HOOD_SCOOP).details(D_FOG | D_RACE_DECALS | D_WIPERS));
        add("aurora_turbo", "Aurora Boreal Turbo", 1, Rarity.EPICO, "Coupé de curvas suaves y faros escondidos; la pintura cambia del verde al violeta.",
                of(Shape.COUPE).dims(14.0F, 6.6F, 0.7F, 1.8F).wheels(1.32F, 1.35F, 4.3F, -4.2F, 0.2F)
                        .top(-7.0F, 2.35F, -6.6F, 2.6F, -4.4F, 2.75F, -2.4F, 2.55F, 1.0F, 2.45F, 3.6F, 2.35F, 5.6F, 1.95F, 6.6F, 1.6F, 7.0F, 1.25F)
                        .bottom(-7.0F, 1.0F, -6.4F, 0.7F, 6.4F, 0.7F, 7.0F, 0.95F)
                        .plan(-7.0F, 2.6F, -6.4F, 3.1F, -4.3F, 3.3F, -1.2F, 2.9F, 1.8F, 2.95F, 4.3F, 3.25F, 6.4F, 2.8F, 7.0F, 2.0F)
                        .roofLine(2, 3, -4.8F, 0, -3.2F, 3.5F, -1.4F, 3.95F, -0.2F, 3.95F, 1.4F, 0)
                        .roof(-1, 0.65F, 0).glassSlope(0, 0, 0.55F).humps(0.3F, 0.35F).dome(0.02F).form(1.7F, 0, 0, 0.1F).teardrop(0.25F)
                        .colors(0x22D3A6, 0x7B3FE4).lights(HEAD_POPUP, TAIL_BAR).rims(RIM_TWIN5, 0xD9D9D9).neon(0x7B3FE4).with(SPOILER | TWO_TONE));
        add("fantasma_nocturno", "Fantasma Nocturno", 1, Rarity.EPICO, "Gran turismo japonés musculoso, casi invisible de noche… hasta que enciende el neón.",
                of(Shape.GT).dims(15.0F, 6.8F, 0.85F, 2.2F).wheels(1.42F, 1.4F, 4.6F, -4.3F, 0.22F)
                        .top(-7.5F, 2.85F, -7.1F, 3.15F, -4.6F, 3.2F, 1.6F, 3.15F, 4.2F, 3.0F, 6.6F, 2.7F, 7.5F, 2.35F)
                        .bottom(-7.5F, 1.2F, -6.9F, 0.85F, 6.9F, 0.85F, 7.5F, 1.2F)
                        .plan(-7.5F, 2.9F, -6.8F, 3.3F, -4.4F, 3.45F, 0F, 3.3F, 4.6F, 3.45F, 6.9F, 3.2F, 7.5F, 2.8F)
                        .roofLine(2, 3, -4.8F, 0, -3.6F, 4.1F, -2.0F, 4.65F, 0.2F, 4.65F, 1.8F, 0)
                        .roof(-1, 0.55F, 0).glassSlope(0, 0, 0.45F).humps(0.15F, 0.2F).dome(0.06F).form(0.55F, 0, 0, 0)
                        .colors(0x343A48, 0x6CF2FF).glass(0x101418).lights(HEAD_ANGULAR, TAIL_ROUND2).rims(RIM_MULTI, 0x15171C).neon(0x6CF2FF)
                        .with(SIDE_STRIPE | SPOILER).details(D_QUAD_EXH | D_HOOD_LINES | D_DIFFUSER | D_SIDE_INTAKE | D_WIPERS));
        add("dragon_turbo_x", "Dragón Turbo X", 1, Rarity.LEGENDARIO, "Cuña ochentera de puro ángulo: faros escondidos, tomas enormes y alas de dragón.",
                of(Shape.WEDGE).dims(14.4F, 7.0F, 0.65F, 1.6F).wheels(1.4F, 1.6F, 4.6F, -4.3F, 0.3F)
                        .top(-7.2F, 2.4F, -6.8F, 2.7F, -4.4F, 2.75F, -2.2F, 2.6F, 0F, 2.4F, 3.6F, 2.15F, 5.6F, 1.6F, 6.8F, 1.2F, 7.2F, 1.0F)
                        .bottom(-7.2F, 0.9F, -6.6F, 0.65F, 6.6F, 0.6F, 7.2F, 0.75F)
                        .plan(-7.2F, 3.0F, -6.4F, 3.5F, -4.2F, 3.5F, -1.6F, 3.25F, 2.0F, 3.2F, 4.6F, 3.25F, 6.6F, 2.9F, 7.2F, 2.6F)
                        .roofLine(1, 2, -2.8F, 0, -1.4F, 3.6F, 0.4F, 3.7F, 3.6F, 0)
                        .roof(-1, 0.75F, 0).glassSlope(0, 0, 0.7F).humps(0, 0.1F).dome(0).form(0.25F, 0, 0, 0)
                        .colors(0xC1121F, 0xFFC23D).lights(HEAD_POPUP, TAIL_RECT).rims(RIM_TURBINE, 0xFFC23D).neon(0xFF3D3D)
                        .with(BIG_WING | STRIPES | EXHAUST).details(D_SIDE_INTAKE | D_LOUVERS | D_ENGINE_GLASS));

        // ---------------------------------------------------------------- 3) Leyendas del Circuito: superautos y prototipos
        add("falcone_rosso", "Falcone Rosso", 2, Rarity.RARO, "Superauto italiano rojo pasión con motor central a la vista.",
                of(Shape.HYPER).dims(15.2F, 6.5F, 0.55F, 2.0F).wheels(1.35F, 1.45F, 4.4F, -4.4F, 0.2F)
                        .top(-7.6F, 2.35F, -7.2F, 2.65F, -4.8F, 2.75F, -2.8F, 2.85F, 0F, 2.8F, 2.6F, 2.6F, 4.5F, 2.25F, 6.2F, 1.75F, 7.2F, 1.35F, 7.6F, 1.05F)
                        .bottom(-7.6F, 1.0F, -6.8F, 0.55F, 6.8F, 0.5F, 7.6F, 0.75F)
                        .plan(-7.6F, 2.55F, -6.8F, 3.05F, -4.4F, 3.25F, -2.0F, 2.95F, 1.0F, 3.0F, 4.4F, 3.15F, 6.5F, 2.75F, 7.6F, 1.85F)
                        .roofLine(2, 3, -2.9F, 0, -1.8F, 3.6F, -0.5F, 4.0F, 0.5F, 3.98F, 2.6F, 0)
                        .roof(-1, 0.7F, 0).glassSlope(0, 0, 0.5F).humps(0.25F, 0.35F).dome(0.03F).form(1.3F, 0, 0, 0).teardrop(0.25F)
                        .colors(0xD3121A, 0x1A1A1A).lights(HEAD_SLIM, TAIL_ROUND2).rims(RIM_Y, 0xD9D9D9).caliper(0xFFD23F).with(SPOILER)
                        .details(D_SIDE_INTAKE | D_ENGINE_GLASS | D_DIFFUSER | D_CENTER_EXH));
        add("vortex_v12", "Vortex V12", 2, Rarity.RARO, "Doce cilindros y una cuña afilada como flecha, toda de ángulos.",
                of(Shape.WEDGE).dims(15.6F, 7.0F, 0.6F, 1.6F).wheels(1.45F, 1.6F, 4.7F, -4.6F, 0.25F)
                        .top(-7.8F, 2.45F, -7.4F, 2.75F, -5.0F, 2.8F, -2.4F, 2.7F, 0.4F, 2.5F, 2.8F, 2.2F, 5.2F, 1.7F, 7.0F, 1.2F, 7.8F, 0.95F)
                        .bottom(-7.8F, 0.9F, -7.0F, 0.55F, 7.0F, 0.5F, 7.8F, 0.65F)
                        .plan(-7.8F, 3.05F, -6.8F, 3.5F, -4.4F, 3.55F, -1.8F, 3.2F, 1.6F, 3.15F, 4.6F, 3.3F, 6.8F, 3.0F, 7.8F, 2.4F)
                        .roofLine(1, 2, -2.8F, 0, -1.4F, 3.55F, 0.2F, 3.62F, 2.8F, 0)
                        .roof(-1, 0.8F, 0).glassSlope(0, 0, 0.75F).humps(0.1F, 0.2F).dome(0).form(0.2F, 0, 0, 0)
                        .colors(0xC6E81A, 0x1A1A1A).lights(HEAD_ANGULAR, TAIL_C).rims(RIM_Y, 0x2B2B2B).with(EXHAUST)
                        .details(D_SIDE_INTAKE | D_ENGINE_GLASS | D_LOUVERS | D_DIFFUSER | D_SPLITTER | D_CENTER_EXH));
        add("imperial_gt", "Imperial GT", 2, Rarity.RARO, "Gran turismo de cofre larguísimo y parrilla de lujo para cruzar continentes.",
                of(Shape.GT).dims(16.2F, 6.8F, 0.85F, 2.1F).wheels(1.45F, 1.4F, 5.2F, -4.6F, 0.15F)
                        .top(-8.1F, 2.55F, -7.7F, 2.95F, -5.4F, 3.05F, -3.4F, 3.05F, 0.6F, 2.9F, 3.6F, 2.8F, 6.4F, 2.5F, 7.6F, 2.15F, 8.1F, 1.8F)
                        .bottom(-8.1F, 1.15F, -7.4F, 0.85F, 7.4F, 0.85F, 8.1F, 1.1F)
                        .plan(-8.1F, 2.7F, -7.4F, 3.2F, -5.0F, 3.35F, -2.4F, 3.15F, 1.0F, 3.15F, 5.0F, 3.3F, 7.4F, 3.0F, 8.1F, 2.4F)
                        .roofLine(2, 3, -5.6F, 0, -4.0F, 3.7F, -2.8F, 4.3F, -1.4F, 4.32F, 0.6F, 0)
                        .roof(-1, 0.6F, 0).glassSlope(0, 0, 0.5F).humps(0.15F, 0.25F).dome(0.08F).form(1.3F, 0, 0, 0.06F).teardrop(0.2F)
                        .colors(0x0E3B2E, 0xD9C38A).lights(HEAD_SLIM, TAIL_SLIM).rims(RIM_MULTI, 0xD9D9D9).with(CHROME)
                        .details(D_BIG_GRILLE | D_CHROME_TRIM | D_QUAD_EXH | D_HOOD_VENTS | D_WIPERS));
        add("cometa_plata", "Cometa Plata", 2, Rarity.EPICO, "Coupé plateado de los 50, de cejas marcadas y parrilla ancha: una joya de museo.",
                of(Shape.GT).dims(14.6F, 6.2F, 1.0F, 1.85F).wheels(1.4F, 1.2F, 4.4F, -4.2F, 0.05F)
                        .top(-7.3F, 2.35F, -6.6F, 2.75F, -4.6F, 2.85F, -2.8F, 2.9F, 0.4F, 2.85F, 3.6F, 2.8F, 5.8F, 2.55F, 6.9F, 2.15F, 7.3F, 1.7F)
                        .bottom(-7.3F, 1.4F, -6.6F, 1.0F, 6.6F, 1.0F, 7.3F, 1.3F)
                        .plan(-7.3F, 2.4F, -6.4F, 2.85F, -4.2F, 3.1F, -1.6F, 2.95F, 1.6F, 2.95F, 4.4F, 3.1F, 6.4F, 2.8F, 7.3F, 2.1F)
                        .roofLine(2, 3, -4.0F, 0, -3.0F, 3.75F, -1.9F, 4.15F, -0.3F, 4.15F, 0.6F, 0)
                        .roof(-1, 0.45F, 0).glassSlope(0, 0, 0.45F).humps(0.3F, 0.12F).dome(0.12F).form(1.8F, 0, 0, 0)
                        .colors(0xC9CED6, 0x2B2F36).lights(HEAD_ROUND, TAIL_VERT).rims(RIM_HUB, 0xE6E6E6).with(CHROME)
                        .details(D_OVAL_GRILLE | D_HOOD_LINES | D_CHROME_TRIM | D_WIPERS));
        add("hyperion_e", "Hyperion E", 2, Rarity.EPICO, "Hiperauto eléctrico: silencioso, brutal y con barras de luz de punta a punta.",
                of(Shape.HYPER).dims(15.2F, 6.8F, 0.6F, 1.6F).wheels(1.4F, 1.5F, 4.5F, -4.5F, 0.25F)
                        .top(-7.6F, 2.55F, -7.2F, 2.8F, -4.6F, 2.85F, -2.6F, 2.75F, 0F, 2.6F, 2.6F, 2.4F, 4.6F, 2.05F, 6.4F, 1.6F, 7.6F, 1.15F)
                        .bottom(-7.6F, 0.95F, -6.8F, 0.55F, 6.8F, 0.5F, 7.6F, 0.7F)
                        .plan(-7.6F, 2.8F, -6.6F, 3.3F, -4.4F, 3.4F, -2.0F, 3.1F, 1.4F, 3.1F, 4.4F, 3.3F, 6.6F, 2.9F, 7.6F, 2.2F)
                        .roofLine(2, 3, -3.6F, 0, -2.4F, 3.55F, -1.0F, 3.95F, 0.3F, 3.95F, 2.6F, 0)
                        .roof(-1, 0.7F, 0).glassSlope(0, 0, 0.55F).humps(0.2F, 0.3F).dome(0.03F).form(1.2F, 0, 0, 0).teardrop(0.2F)
                        .colors(0xF2F5FA, 0x0FA3FF).lights(HEAD_BAR, TAIL_BAR).rims(RIM_TURBINE, 0x1B1F26).neon(0x0FA3FF).with(SIDE_STRIPE)
                        .details(D_DIFFUSER | D_SPLITTER | D_SIDE_INTAKE | D_LIGHT_BAR));
        add("resistencia_24", "Prototipo Resistencia 24", 2, Rarity.EPICO, "Prototipo de carreras de 24 horas: aleta, alerón y número en la puerta.",
                of(Shape.PROTO).dims(16.6F, 7.0F, 0.5F, 1.4F).wheels(1.45F, 1.5F, 5.2F, -5.0F, 0.15F)
                        .top(-8.3F, 2.1F, -7.6F, 2.3F, -5.0F, 2.2F, -2.0F, 2.05F, 0.5F, 1.9F, 3.0F, 1.75F, 5.6F, 1.55F, 7.6F, 1.15F, 8.3F, 0.8F)
                        .bottom(-8.3F, 0.7F, -7.4F, 0.45F, 7.4F, 0.45F, 8.3F, 0.5F)
                        .plan(-8.3F, 3.1F, -6.6F, 3.5F, -4.4F, 3.5F, -2.0F, 2.7F, 2.0F, 2.7F, 5.2F, 3.45F, 7.4F, 3.3F, 8.3F, 2.6F)
                        .roofLine(1, 2, -2.4F, 0, -1.2F, 3.45F, 0.4F, 3.5F, 2.6F, 0)
                        .roof(-1, 1.3F, 0).glassSlope(0, 0, 0.35F).humps(0.5F, 0.55F).dome(0).form(1.1F, 0, 0, 0).teardrop(0.2F)
                        .colors(0x1D3557, 0xE63946).lights(HEAD_SLIM, TAIL_BAR).rims(RIM_MULTI, 0x1A1A1A)
                        .with(FIN | NUMBER_DISC | BIG_WING).details(D_SPLITTER | D_DIFFUSER | D_RACE_DECALS));
        add("monoplaza_astra", "Monoplaza Astra", 2, Rarity.EPICO, "Ruedas al aire, alerones enormes y 300 km/h en recta.",
                of(Shape.FORMULA).colors(0x0B6E4F, 0xFFD60A).rim(0x1A1A1A).with(BIG_WING | HELMET));
        add("solaris_concept", "Solaris Concept", 2, Rarity.LEGENDARIO, "Un auto que parece venir del futuro, con cúpula de cristal.",
                of(Shape.HYPER).dims(15.6F, 7.0F, 0.55F, 1.5F).wheels(1.45F, 1.5F, 4.6F, -4.6F, 0.2F)
                        .top(-7.8F, 2.1F, -7.2F, 2.4F, -4.6F, 2.5F, -2.0F, 2.35F, 1.0F, 2.15F, 3.6F, 1.9F, 6.0F, 1.45F, 7.8F, 0.95F)
                        .bottom(-7.8F, 0.8F, -6.8F, 0.5F, 6.8F, 0.5F, 7.8F, 0.6F)
                        .plan(-7.8F, 2.6F, -6.4F, 3.3F, -4.4F, 3.45F, -1.0F, 2.9F, 2.0F, 2.9F, 4.6F, 3.3F, 6.6F, 2.8F, 7.8F, 1.8F)
                        .roofLine(1, 2, -3.6F, 0, -1.6F, 3.5F, 0.6F, 3.55F, 3.6F, 0)
                        .roof(-1, 0.75F, 0).glassSlope(0, 0, 0.6F).humps(0.3F, 0.35F).dome(0).form(1.6F, 0, 0, 0).teardrop(0.3F)
                        .colors(0xFF9F1C, 0xFFFFFF).glass(0x3BD1FF).lights(HEAD_BAR, TAIL_BAR).rims(RIM_TURBINE, 0xF2F2F2).neon(0xFFD166)
                        .with(CANOPY | FIN).details(D_LIGHT_BAR | D_DIFFUSER));
        add("titan_hiper", "Titán Hiper", 2, Rarity.LEGENDARIO, "Dieciséis cilindros, curva lateral de otro color y barra de luz atrás: la cima de la colección.",
                of(Shape.HYPER).dims(15.8F, 7.0F, 0.6F, 1.8F).wheels(1.45F, 1.55F, 4.7F, -4.6F, 0.2F)
                        .top(-7.9F, 2.45F, -7.5F, 2.8F, -5.0F, 2.9F, -2.8F, 2.95F, 0F, 2.85F, 2.6F, 2.65F, 4.8F, 2.35F, 6.8F, 1.85F, 7.9F, 1.4F)
                        .bottom(-7.9F, 1.0F, -7.0F, 0.6F, 7.0F, 0.55F, 7.9F, 0.75F)
                        .plan(-7.9F, 2.8F, -7.0F, 3.35F, -4.6F, 3.5F, -2.0F, 3.2F, 1.0F, 3.2F, 4.6F, 3.4F, 6.8F, 3.0F, 7.9F, 2.3F)
                        .roofLine(2, 3, -3.6F, 0, -2.4F, 3.8F, -1.0F, 4.25F, 0.5F, 4.25F, 2.6F, 0)
                        .roof(-1, 0.75F, 0).glassSlope(0, 0, 0.55F).humps(0.2F, 0.3F).dome(0.04F).form(1.3F, 0, 0, 0).teardrop(0.25F)
                        .colors(0x13294B, 0x8EA8C3).lights(HEAD_SLIM, TAIL_BAR).rims(RIM_Y, 0xC0C0C0).with(TWO_TONE | SPOILER)
                        .details(D_C_LINE | D_OVAL_GRILLE | D_CENTER_EXH | D_DIFFUSER | D_ROOF_SCOOP | D_LIGHT_BAR));
        add("corona_oro", "Corona de Oro", 2, Rarity.LEGENDARIO, "Hiperauto hecho de oro de punta a punta. Brilla incluso en la oscuridad.",
                of(Shape.HYPER).dims(15.2F, 6.8F, 0.6F, 1.6F).wheels(1.4F, 1.5F, 4.5F, -4.5F, 0.2F)
                        .top(-7.6F, 2.6F, -7.2F, 2.85F, -4.6F, 2.85F, -2.4F, 2.7F, 0.2F, 2.55F, 2.8F, 2.35F, 5.0F, 1.95F, 6.8F, 1.45F, 7.6F, 1.1F)
                        .bottom(-7.6F, 1.0F, -6.8F, 0.55F, 6.8F, 0.5F, 7.6F, 0.65F)
                        .plan(-7.6F, 2.9F, -6.6F, 3.3F, -4.4F, 3.4F, -1.8F, 3.05F, 1.4F, 3.05F, 4.4F, 3.3F, 6.6F, 2.85F, 7.6F, 2.2F)
                        .roofLine(2, 3, -3.2F, 0, -2.0F, 3.6F, -0.6F, 3.95F, 0.6F, 3.95F, 2.8F, 0)
                        .roof(-1, 0.7F, 0).glassSlope(0, 0, 0.55F).humps(0.2F, 0.3F).dome(0.03F).form(1.0F, 0, 0, 0).teardrop(0.35F)
                        .colors(0xF4C430, 0xFFF1B0).lights(HEAD_SLIM, TAIL_ROUND2).rims(RIM_TURBINE, 0xFFF1B0).glass(0x6B4A12)
                        .with(GOLD_SPARKLE | CHROME | SPOILER).details(D_SIDE_INTAKE | D_DIFFUSER | D_SPLITTER | D_CENTER_EXH));

        // ---------------------------------------------------------------- 4) Exclusivos de Cine: autos únicos inventados
        add("centella_77", "Centella 77", 3, Rarity.EPICO, "Auto de óvalo con calcomanías de patrocinadores: nació para dar vueltas a 300 km/h.",
                of(Shape.COUPE).dims(16.0F, 6.8F, 0.7F, 2.2F).wheels(1.4F, 1.4F, 4.9F, -4.7F, 0.15F)
                        .top(-8.0F, 2.95F, -7.6F, 3.3F, -4.6F, 3.3F, 1.4F, 3.2F, 4.4F, 3.05F, 7.0F, 2.7F, 8.0F, 2.2F)
                        .bottom(-8.0F, 1.0F, -7.3F, 0.7F, 7.3F, 0.65F, 8.0F, 0.8F)
                        .plan(-8.0F, 2.95F, -7.2F, 3.35F, 7.2F, 3.35F, 8.0F, 2.9F)
                        .roofLine(2, 3, -4.8F, 0, -3.6F, 4.0F, -2.2F, 4.6F, 0F, 4.6F, 1.8F, 0)
                        .roof(-1, 0.55F, 0).glassSlope(0, 0, 0.45F).humps(0.05F, 0.08F).dome(0.06F).form(0.8F, 0, 0, 0)
                        .colors(0x1E5BFF, 0xFF8A1F).lights(HEAD_RECT, TAIL_BAR).rims(RIM_STEEL, 0x1A1A1A)
                        .with(NUMBER_DISC | SPOILER | SIDE_STRIPE).details(D_RACE_DECALS | D_SPLITTER | D_SIDE_EXH));
        add("buggy_duna", "Buggy Duna", 3, Rarity.EPICO, "Arenero de jaula y llantas gordas que vuela sobre las dunas.",
                of(Shape.BUGGY).dims(12.0F, 6.6F, 1.8F, 1.5F).wheels(1.9F, 1.6F, 3.8F, -3.9F, 0.9F).frontWheels(1.6F, 1.3F).open()
                        .top(-6.0F, 3.0F, -5.4F, 3.3F, -2.6F, 3.4F, 1.0F, 3.35F, 3.6F, 3.1F, 5.6F, 2.7F, 6.0F, 2.4F)
                        .bottom(-6.0F, 2.3F, -5.2F, 1.8F, 5.2F, 1.8F, 6.0F, 2.1F)
                        .plan(-6.0F, 2.0F, -5.0F, 2.4F, 2.0F, 2.4F, 5.2F, 1.9F, 6.0F, 1.5F)
                        .cab(1.2F, -2.6F, 1.6F).roof(-1, 0.3F, 0).form(0.8F, 0, 0, 0)
                        .colors(0xFFB000, 0x1A1A1A).lights(HEAD_ROUND, TAIL_ROUND1).rims(RIM_STEEL, 0x2A2A2A)
                        .with(ROOF_LIGHTS).details(D_CAGE | D_FLAMES));
        add("rover_selene", "Rover Selene", 3, Rarity.EPICO, "Vehículo lunar de seis ruedas, con antena de plato y bandera.",
                of(Shape.ROVER).dims(13.0F, 7.0F, 1.6F, 0.7F).wheels(1.25F, 1.1F, 4.6F, -4.6F, 1.2F).open()
                        .top(-6.5F, 2.3F, 6.5F, 2.3F).bottom(-6.5F, 1.6F, 6.5F, 1.6F).plan(-6.5F, 2.6F, 6.5F, 2.6F)
                        .cab(2.0F, -3.0F, 1.4F).form(0.3F, 0, 0, 0).colors(0xE8E8EC, 0xD8A63A).lights(HEAD_ROUND, TAIL_ROUND1).rims(RIM_MESH, 0xC0C4CA)
                        .details(D_CAGE | D_DISH));
        add("hotrod_llamarada", "Hot Rod Llamarada", 3, Rarity.LEGENDARIO, "Hot rod de los 30 con motor al aire, escapes de tubo y flamas pintadas a mano.",
                of(Shape.HOTROD).dims(13.6F, 5.0F, 1.2F, 2.0F).wheels(1.75F, 1.7F, 4.0F, -4.3F, 1.0F).frontWheels(1.35F, 1.0F).open()
                        .top(-6.8F, 2.8F, -6.3F, 3.2F, -3.0F, 3.25F, 0.6F, 3.2F, 1.1F, 2.3F, 4.6F, 2.3F, 5.2F, 3.25F, 6.4F, 3.3F, 6.8F, 3.0F)
                        .bottom(-6.8F, 1.6F, -6.0F, 1.2F, 6.0F, 1.2F, 6.8F, 1.4F)
                        .plan(-6.8F, 2.1F, -6.0F, 2.4F, 0.6F, 2.4F, 1.1F, 1.2F, 4.8F, 1.2F, 5.2F, 1.6F, 6.8F, 1.5F)
                        .cab(0.6F, -2.8F, 1.3F).glassSlope(0.3F, 0, 0).roof(-1, 0.3F, 0).form(0.9F, 0, 0, 0)
                        .colors(0x1A1A1E, 0xFF8A1F).lights(HEAD_ROUND, TAIL_ROUND1).rims(RIM_DISH, 0xE6E6E6).with(CHROME)
                        .details(D_ENGINE_OUT | D_FLAMES));
        add("monstruo_rugido", "Monstruo Rugido", 3, Rarity.LEGENDARIO, "Camioneta monstruo de llantas gigantes que aplasta todo a su paso.",
                of(Shape.MONSTER).dims(14.0F, 7.0F, 3.6F, 2.6F).wheels(2.9F, 2.4F, 4.6F, -4.6F, 1.8F).open()
                        .cab(2.2F, -1.4F, 2.4F).glassSlope(1.0F, 0.2F, 0.2F).slopes(0.6F, 0).form(0.5F, 0, 0, 0)
                        .colors(0x2BB24C, 0x111111).lights(HEAD_RECT, TAIL_VERT).rims(RIM_DISH, 0xD9DDE2)
                        .with(ROOF_LIGHTS | BULLBAR).details(D_FLAMES | D_BIG_GRILLE));
        add("dragster_infernal", "Dragster Infernal", 3, Rarity.LEGENDARIO, "Cuarto de milla en cinco segundos: llantas traseras enormes y un motor que escupe fuego.",
                of(Shape.DRAGSTER).dims(18.0F, 3.0F, 0.9F, 1.0F).wheels(2.6F, 2.4F, 7.6F, -6.6F, 1.4F).frontWheels(0.8F, 0.5F).open()
                        .top(-9.0F, 2.4F, -8.0F, 2.6F, -6.0F, 2.5F, -4.6F, 2.3F, -3.2F, 2.1F, 0F, 1.7F, 4.0F, 1.5F, 8.0F, 1.2F, 9.0F, 1.0F)
                        .bottom(-9.0F, 1.3F, -8.0F, 0.9F, 9.0F, 0.8F)
                        .plan(-9.0F, 1.0F, -7.4F, 1.4F, -4.4F, 1.4F, -2.6F, 1.0F, 9.0F, 0.4F)
                        .cab(-3.0F, -4.6F, 1.0F).roof(-1, 0.2F, 0).form(0.8F, 0, 0, 0)
                        .colors(0xD00000, 0xFFD23F).lights(HEAD_ROUND, TAIL_ROUND1).rims(RIM_DISH, 0xE6E6E6)
                        .with(BIG_WING).details(D_CAGE | D_ENGINE_OUT | D_FLAMES));
        add("tiburon_acero", "Tiburón de Acero", 3, Rarity.LEGENDARIO, "Auto de exhibición con forma de tiburón: aleta, branquias y una sonrisa llena de dientes.",
                of(Shape.SHARK).dims(16.0F, 6.6F, 0.7F, 1.8F).wheels(1.4F, 1.45F, 4.8F, -4.6F, 0.2F)
                        .top(-8.0F, 2.2F, -7.4F, 2.5F, -4.6F, 2.7F, -1.6F, 2.8F, 1.4F, 2.7F, 4.0F, 2.4F, 6.2F, 1.95F, 7.4F, 1.5F, 8.0F, 1.2F)
                        .bottom(-8.0F, 1.3F, -7.0F, 0.8F, 6.8F, 0.7F, 8.0F, 1.0F)
                        .plan(-8.0F, 1.0F, -7.0F, 2.2F, -4.6F, 3.2F, -1.0F, 3.3F, 2.0F, 3.2F, 4.6F, 3.0F, 6.6F, 2.2F, 8.0F, 0.9F)
                        .roofLine(1, 2, -4.2F, 0, -2.0F, 4.0F, 0.2F, 4.05F, 2.6F, 0)
                        .roof(-1, 0.75F, 0).glassSlope(0, 0, 0.6F).humps(0.15F, 0.2F).dome(0.05F).form(1.6F, 0, 0, 0).teardrop(0.3F)
                        .colors(0x6E7F8E, 0x2B3640).belly(0xF2F2EE).glass(0x14202A).lights(HEAD_SLIM, TAIL_SLIM).rims(RIM_TURBINE, 0x9AA6B2)
                        .details(D_SHARK | D_DIFFUSER));
        add("interceptor_sombra", "Interceptor Sombra", 3, Rarity.MITICO, "Interceptor blindado de facetas negras: entra a la ciudad sin hacer ruido.",
                of(Shape.WEDGE).dims(16.4F, 7.4F, 0.8F, 2.0F).wheels(1.7F, 1.8F, 5.0F, -4.8F, 0.25F)
                        .top(-8.2F, 2.9F, -7.6F, 3.2F, -5.0F, 3.2F, -2.4F, 3.1F, 1.0F, 2.9F, 3.8F, 2.6F, 6.2F, 2.1F, 8.2F, 1.4F)
                        .bottom(-8.2F, 1.4F, -7.2F, 0.8F, 7.2F, 0.75F, 8.2F, 1.0F)
                        .plan(-8.2F, 3.0F, -7.2F, 3.6F, -4.4F, 3.7F, 0F, 3.4F, 4.6F, 3.6F, 7.0F, 3.2F, 8.2F, 2.2F)
                        .roofLine(1, 2, -3.6F, 0, -1.6F, 4.1F, 0.4F, 4.1F, 3.4F, 0)
                        .roof(-1, 0.9F, 0).glassSlope(0, 0, 0.7F).humps(0.2F, 0.25F).dome(0).form(0.12F, 0, 0, 0)
                        .colors(0x2A2D35, 0xD7263D).glass(0x0E1014).lights(HEAD_SLIM, TAIL_BAR).rims(RIM_TURBINE, 0x0E0E10).caliper(0xD7263D)
                        .details(D_ARMOR | D_CENTER_EXH | D_DIFFUSER | D_SPLITTER));
        add("cohete_salar", "Cohete Salar", 3, Rarity.MITICO, "Bólido de récord de velocidad con motor de cohete: más rápido que el sonido.",
                of(Shape.ROCKET).dims(19.0F, 4.0F, 0.9F, 2.1F).wheels(1.0F, 0.8F, 6.4F, -6.8F, 0.5F).open()
                        .top(-9.5F, 2.6F, -8.0F, 3.0F, -2.0F, 3.0F, 4.0F, 2.6F, 8.0F, 1.8F, 9.5F, 1.3F)
                        .bottom(-9.5F, 1.4F, -8.0F, 1.0F, 8.0F, 1.0F, 9.5F, 1.3F)
                        .plan(-9.5F, 1.4F, -8.0F, 2.0F, -2.0F, 2.0F, 6.0F, 1.6F, 9.5F, 0.4F)
                        .roofLine(1, 2, 0.4F, 0, 1.4F, 3.75F, 2.4F, 3.75F, 3.8F, 0)
                        .roof(-1, 0.9F, 0).glassSlope(0, 0, 0.3F).form(1.6F, 0, 0, 0)
                        .colors(0xF4F4F4, 0xFF5A1F).glass(0x1B2A3A).lights(HEAD_SLIM, TAIL_SLIM).rims(RIM_HUB, 0xD0D4DA)
                        .with(FIN | STRIPES).details(D_JET));
        add("nebula_hover", "Nebula Hover", 3, Rarity.MITICO, "No tiene llantas: flota sobre cojines de energía. El auto más raro de todos.",
                of(Shape.HOVER).dims(15.0F, 7.0F, 1.3F, 1.6F).wheels(1.4F, 1.4F, 4.4F, -4.4F, 0)
                        .top(-7.5F, 2.6F, -7.0F, 2.9F, -3.0F, 3.1F, 1.0F, 3.0F, 4.0F, 2.7F, 6.4F, 2.2F, 7.5F, 1.7F)
                        .bottom(-7.5F, 1.7F, -6.5F, 1.3F, 6.5F, 1.3F, 7.5F, 1.5F)
                        .plan(-7.5F, 1.8F, -6.0F, 3.0F, -2.0F, 3.5F, 2.0F, 3.4F, 5.6F, 2.8F, 7.5F, 1.4F)
                        .roofLine(1, 2, -3.4F, 0, -1.6F, 4.3F, 0.6F, 4.3F, 3.0F, 0)
                        .roof(-1, 0.8F, 0).glassSlope(0, 0, 0.6F).form(1.8F, 0, 0, 0).teardrop(0.3F)
                        .colors(0x2A1B5C, 0x3FF2FF).glass(0x8A4DFF).lights(HEAD_BAR, TAIL_BAR).neon(0x3FF2FF)
                        .with(CANOPY | SIDE_STRIPE).details(D_HOVER | D_LIGHT_BAR));
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
