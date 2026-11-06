package com.mundosdeaxelis.cartas;

/** GENERADO por herramientas/gen.py a partir de cartas.json. No editar a mano. */
final class CardData {
    private CardData() {
    }

    /** id, nombre, color */
    static final String[][] CATEGORIES = {
        {"jefes", "Jefes", "#d94a3a"},
        {"criaturas", "Criaturas", "#7a8a3a"},
        {"mascotas", "Mascotas", "#e8a13a"},
        {"minecraft", "Minecraft", "#4aa05a"},
        {"espadas", "Espadas", "#6fa8dc"},
        {"personajes", "Personajes", "#b06ad9"},
    };

    /** id, nombre, categoria, rareza, descripcion, color1, color2 */
    static final String[][] CARDS = {
        {"rey_brote", "Rey Brote", "jefes", "RARA", "El soberano del bosque. Su corona nunca se marchita.", "#5fbf4a", "#ffd84a"},
        {"gran_herrero", "Gran Herrero", "jefes", "EPICA", "Forja con fuego que nunca se apaga.", "#e0733a", "#ffd84a"},
        {"lobo_artico", "Lobo \u00c1rtico", "jefes", "EPICA", "Su aullido congela el aire.", "#bfe6ff", "#4a90d9"},
        {"archimago", "Archimago", "jefes", "LEGENDARIA", "Guardi\u00e1n de la torre y de sus libros prohibidos.", "#b06ad9", "#ffe27a"},
        {"brote", "Brote", "criaturas", "COMUN", "Peque\u00f1o, verde y muy terco.", "#6fcf55", "#8a5a2a"},
        {"espinoso", "Espinoso", "criaturas", "COMUN", "Mejor no abrazarlo.", "#4a9a3a", "#d9c04a"},
        {"chispero", "Chispero", "criaturas", "COMUN", "Salta de la fragua con cada golpe.", "#ff9a2a", "#ffe27a"},
        {"fundidor", "Fundidor", "criaturas", "RARA", "Derrite todo lo que toca.", "#c85a2a", "#9aa0a6"},
        {"capataz", "Capataz", "criaturas", "RARA", "Manda en la forja y no admite quejas.", "#b0502a", "#d9d9d9"},
        {"lobo_escarcha", "Lobo de Escarcha", "criaturas", "RARA", "Caza en manada entre la nieve.", "#9ad0f0", "#ffffff"},
        {"esbirro", "Esbirro del Archimago", "criaturas", "COMUN", "Obedece sin hacer preguntas.", "#8a5ac0", "#e8e0f0"},
        {"libro_embrujado", "Libro Embrujado", "criaturas", "COMUN", "Vuela y no le gusta que lo lean.", "#a05ad0", "#ffe27a"},
        {"armadura_encantada", "Armadura Encantada", "criaturas", "RARA", "Vacia por dentro, llena de magia.", "#7a7fa0", "#b08aff"},
        {"espiritu_bosque", "Esp\u00edritu del Bosque", "mascotas", "RARA", "Te sigue entre las hojas.", "#7fe08a", "#ffffff"},
        {"golemcito", "Golemcito", "mascotas", "RARA", "Peque\u00f1o, terco y de hierro.", "#d98a4a", "#ffd84a"},
        {"fosu", "Fosu", "mascotas", "LEGENDARIA", "Una mariposa celestial.", "#ffc0e8", "#ffffff"},
        {"cookie", "Cookie", "mascotas", "LEGENDARIA", "Hur\u00f3n gris, ladr\u00f3n de corazones.", "#b4bac6", "#3a3f4a"},
        {"dragon_celestial", "Drag\u00f3n Celestial", "mascotas", "MITICA", "Naci\u00f3 de una estrella fugaz.", "#6ad0ff", "#ffe27a"},
        {"creeper", "Creeper", "minecraft", "COMUN", "Ssssss...", "#5fbf4a", "#1f3a1a"},
        {"zombi", "Zombi", "minecraft", "COMUN", "Camina hacia ti, siempre.", "#6a9a5a", "#2a3a4a"},
        {"esqueleto", "Esqueleto", "minecraft", "COMUN", "Tiene muy buena punter\u00eda.", "#d9d9d9", "#4a4a4a"},
        {"enderman", "Enderman", "minecraft", "RARA", "No lo mires a los ojos.", "#8a3ad0", "#1a0a2a"},
        {"wither", "Wither", "minecraft", "EPICA", "Tres cabezas, ninguna amable.", "#3a3a4a", "#d9d9ff"},
        {"ender_dragon", "Drag\u00f3n del End", "minecraft", "LEGENDARIA", "Reina del vac\u00edo.", "#2a1a3a", "#c06aff"},
        {"espada_hierro", "Espada de Hierro", "espadas", "COMUN", "Fiel y sin adornos.", "#d9d9d9", "#8a5a2a"},
        {"espada_diamante", "Espada de Diamante", "espadas", "RARA", "Brilla incluso en la oscuridad.", "#5ae0e0", "#8a5a2a"},
        {"espada_netherita", "Espada de Netherita", "espadas", "EPICA", "Forjada en las profundidades.", "#4a3f4a", "#c04a2a"},
        {"rey_slime", "Rey Slime", "personajes", "EPICA", "Gelatinoso, majestuoso e imposible de ignorar.", "#6ae05a", "#ffd84a"},
        {"cronista", "Cronista de la Torre", "personajes", "RARA", "Lo ha escrito todo, y sabe m\u00e1s de lo que cuenta.", "#c0a060", "#4a2a1a"},
        {"aldeano", "Aldeano", "personajes", "COMUN", "Hmm.", "#c08a6a", "#ffffff"},
        {"bruja", "Bruja", "personajes", "RARA", "Sus pociones nunca salen igual dos veces.", "#7a3aa0", "#6ae05a"},
    };

    /** id, nombre, color, cartas, rareza minima garantizada, prob. holografica */
    static final String[][] PACKS = {
        {"basico", "Sobre B\u00e1sico", "#9aa0a6", "4", "COMUN", "0.03"},
        {"plata", "Sobre de Plata", "#6fa8dc", "5", "RARA", "0.07"},
        {"oro", "Sobre de Oro", "#ffb21e", "6", "EPICA", "0.15"},
    };

    /** probabilidades (en %) por sobre, en el orden COMUN, RARA, EPICA, LEGENDARIA, MITICA */
    static final double[][] ODDS = {
        {70.0, 24.0, 5.0, 0.95, 0.05},
        {45.0, 38.0, 13.0, 3.5, 0.5},
        {20.0, 40.0, 28.0, 10.0, 2.0},
    };
}
