package com.mundosdeaxelis.world;

/** GENERADO por herramientas/decor/build.py - no editar a mano. Nombres de la decoracion nueva de las estructuras y sus reglas de apoyo. */
public final class EstIds {
    private EstIds() {
    }

    public static final String CANDELABRO = "mundos_de_axelis:est_candelabro";
    public static final String ARANA = "mundos_de_axelis:est_arana";
    public static final String GLOBO = "mundos_de_axelis:est_globo";
    public static final String TELESCOPIO = "mundos_de_axelis:est_telescopio";
    public static final String MESA_ALQUIMIA = "mundos_de_axelis:est_mesa_alquimia";
    public static final String TAPIZ_ESTRELLAS = "mundos_de_axelis:est_tapiz_estrellas";
    public static final String TAPIZ_ARCANO = "mundos_de_axelis:est_tapiz_arcano";
    public static final String TAPIZ_REAL = "mundos_de_axelis:est_tapiz_real";
    public static final String TAPIZ_ARBOL = "mundos_de_axelis:est_tapiz_arbol";
    public static final String TAPIZ_MAPA = "mundos_de_axelis:est_tapiz_mapa";
    public static final String TRONO = "mundos_de_axelis:est_trono";
    public static final String SILLON = "mundos_de_axelis:est_sillon";
    public static final String PILA_LIBROS = "mundos_de_axelis:est_pila_libros";
    public static final String RELOJ = "mundos_de_axelis:est_reloj";
    public static final String GARGOLA = "mundos_de_axelis:est_gargola";
    public static final String CRISTAL_MANA = "mundos_de_axelis:est_cristal_mana";
    public static final String CANDELERO_PARED = "mundos_de_axelis:est_candelero_pared";
    public static final String MONTON_ORO = "mundos_de_axelis:est_monton_oro";
    public static final String JAULA = "mundos_de_axelis:est_jaula";
    public static final String GRILLETES = "mundos_de_axelis:est_grilletes";
    public static final String CALDERO_MAGICO = "mundos_de_axelis:est_caldero_magico";
    public static final String ESFERA_ARMILAR = "mundos_de_axelis:est_esfera_armilar";
    public static final String ESTANTERIA = "mundos_de_axelis:est_estanteria";
    public static final String ESTATUA_CABALLERO = "mundos_de_axelis:est_estatua_caballero";
    public static final String PEDESTAL_RUNICO = "mundos_de_axelis:est_pedestal_runico";

    /** Tipo de apoyo: "wall" (necesita muro detras), "soft" (tapiz: no necesita), "hang" (techo), "floor" (suelo), "" si no es de estas. */
    public static String support(String name) {
        if (name.startsWith("mundos_de_axelis:")) {
            name = name.substring(17);
        }
        return switch (name) {
            case "est_candelabro" -> "floor";
            case "est_arana" -> "hang";
            case "est_globo" -> "floor";
            case "est_telescopio" -> "floor";
            case "est_mesa_alquimia" -> "floor";
            case "est_tapiz_estrellas" -> "soft";
            case "est_tapiz_arcano" -> "soft";
            case "est_tapiz_real" -> "soft";
            case "est_tapiz_arbol" -> "soft";
            case "est_tapiz_mapa" -> "soft";
            case "est_trono" -> "floor";
            case "est_sillon" -> "floor";
            case "est_pila_libros" -> "floor";
            case "est_reloj" -> "floor";
            case "est_gargola" -> "floor";
            case "est_cristal_mana" -> "floor";
            case "est_candelero_pared" -> "wall";
            case "est_monton_oro" -> "floor";
            case "est_jaula" -> "hang";
            case "est_grilletes" -> "wall";
            case "est_caldero_magico" -> "floor";
            case "est_esfera_armilar" -> "floor";
            case "est_estanteria" -> "floor";
            case "est_estatua_caballero" -> "floor";
            case "est_pedestal_runico" -> "floor";
            default -> "";
        };
    }

    public static boolean isTall(String name) {
        if (name.startsWith("mundos_de_axelis:")) {
            name = name.substring(17);
        }
        return switch (name) {
            case "est_tapiz_estrellas", "est_tapiz_arcano", "est_tapiz_real", "est_tapiz_arbol", "est_tapiz_mapa", "est_trono", "est_reloj", "est_estanteria", "est_estatua_caballero" -> true;
            default -> false;
        };
    }
}
