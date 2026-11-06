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
        {"personajes", "Personajes", "#b06ad9"},
    };

    /** id, nombre, categoria, rareza, descripcion, color1, color2 */
    static final String[][] CARDS = {
        {"rey_brote", "Rey Brote", "jefes", "EPICA", "El soberano del bosque. Su corona nunca se marchita.", "#5fbf4a", "#ffd84a"},
        {"gran_herrero", "Gran Herrero", "jefes", "EPICA", "Forja con fuego que nunca se apaga.", "#e0733a", "#ffd84a"},
        {"lobo_artico", "Lobo \u00c1rtico", "jefes", "LEGENDARIA", "Su aullido congela el aire y llama a la manada.", "#bfe6ff", "#4a90d9"},
        {"guardian_musgo", "Guardi\u00e1n de Musgo", "criaturas", "RARA", "Mini-jefe del bosque. Golpea el suelo y llama a los Brotes.", "#7a9a6a", "#5fbf4a"},
        {"capataz", "Capataz", "criaturas", "RARA", "Mini-jefe de la forja. Su martillo parte el suelo.", "#b0502a", "#d9d9d9"},
        {"brote", "Brote", "criaturas", "COMUN", "Reto\u00f1o del bosque. Corre hacia ti y no se rinde.", "#6fcf55", "#8a5a2a"},
        {"espinoso", "Espinoso", "criaturas", "COMUN", "Flor carn\u00edvora. Abre los p\u00e9talos y dispara una espina.", "#4a9a3a", "#d9c04a"},
        {"polinizador", "Polinizador", "criaturas", "COMUN", "Vuela en c\u00edrculos y lanza esporas que ralentizan.", "#e0c040", "#7fe08a"},
        {"raizal", "Raizal", "criaturas", "RARA", "G\u00f3lem de ra\u00edces. Te deja inm\u00f3vil si no lo esquivas.", "#6a8a4a", "#8a5a2a"},
        {"raizal_anciano", "Raizal Anciano", "criaturas", "EPICA", "Guardi\u00e1n \u00e9lite de la C\u00e1mara de la Llave del bosque.", "#4a7a3a", "#c9a24a"},
        {"escoria", "Escoria", "criaturas", "COMUN", "Roca fundida con pu\u00f1os al rojo vivo.", "#c85a2a", "#4a3a3a"},
        {"chispero", "Chispero", "criaturas", "COMUN", "Horno con patas. Escupe flechas de fuego.", "#ff9a2a", "#ffe27a"},
        {"ascua", "Ascua", "criaturas", "COMUN", "Brasa viva que vuela y te deja d\u00e9bil.", "#ff7a1a", "#ffe27a"},
        {"fundidor", "Fundidor", "criaturas", "RARA", "G\u00f3lem de metal fundido. Hace erupci\u00f3n bajo tus pies.", "#9a9a9a", "#ff5a1a"},
        {"fundidor_mayor", "Fundidor Mayor", "criaturas", "EPICA", "\u00c9lite de la forja. Vigila la C\u00e1mara de la Llave.", "#7a6a6a", "#ff3a1a"},
        {"lobo_escarcha", "Lobo de Escarcha", "criaturas", "RARA", "Caza en manada. Cada mordisco enfr\u00eda.", "#9ad0f0", "#ffffff"},
        {"fosu", "Fosu", "mascotas", "LEGENDARIA", "Una mariposa celestial.", "#ffc0e8", "#ffffff"},
        {"cookie", "Cookie", "mascotas", "LEGENDARIA", "Hur\u00f3n gris, ladr\u00f3n de corazones.", "#b4bac6", "#3a3f4a"},
        {"creeper", "Creeper", "minecraft", "COMUN", "Ssssss...", "#5fbf4a", "#1f3a1a"},
        {"zombi", "Zombi", "minecraft", "COMUN", "Camina hacia ti, siempre.", "#6a9a5a", "#2a3a4a"},
        {"esqueleto", "Esqueleto", "minecraft", "COMUN", "Tiene muy buena punter\u00eda.", "#d9d9d9", "#4a4a4a"},
        {"enderman", "Enderman", "minecraft", "RARA", "No lo mires a los ojos.", "#241a34", "#e070ff"},
        {"wither", "Wither", "minecraft", "EPICA", "Tres cabezas, ninguna amable.", "#3a3a4a", "#d9d9ff"},
        {"ender_dragon", "Drag\u00f3n del End", "minecraft", "LEGENDARIA", "Reina del vac\u00edo.", "#2a1a3a", "#c06aff"},
        {"aldeano", "Aldeano", "minecraft", "COMUN", "Hmm.", "#c89a74", "#6b4a2a"},
        {"bruja", "Bruja", "minecraft", "RARA", "Sus pociones nunca salen igual dos veces.", "#7a3aa0", "#6ae05a"},
        {"axelis705", "Axelis705", "personajes", "EPICA", "El creador de todo. Le dicen dios: si \u00e9l se va, todos dejan de jugar. Es todo y m\u00e1s.", "#ffe27a", "#ffffff"},
        {"jadiel", "Jadiel", "personajes", "EPICA", "Tryhard en todo: competitivo, juega para ganar. Y adem\u00e1s, ya es padre de un hijo.", "#c03030", "#ffffff"},
        {"marizux", "Marizux", "personajes", "EPICA", "La ternura del grupo, la peque\u00f1a que todos queremos. Feliz, divertida, amable y cordial.", "#e88ab8", "#ffffff"},
        {"odette", "Odette", "personajes", "LEGENDARIA", "Es el mundo entero de Axelis705. Donde ella est\u00e1, todo tiene luz y sentido.", "#8ac8ff", "#ffffff"},
        {"pavi223", "Pavi223", "personajes", "EPICA", "Tryhard del Pok\u00e9mon hasta la \u00faltima batalla. Y si es amarillo, lo ama.", "#e0a020", "#ffffff"},
        {"rafa", "Rafa", "personajes", "EPICA", "El gracioso del grupo. Sus buenas bromas sacan risas siempre, al 100 %.", "#4a9a4a", "#ffffff"},
        {"scardax", "Scardax", "personajes", "EPICA", "Imponente. Juega, crea series y eventos, y siempre se cree mucho. Odia Roblox con toda el alma.", "#d03030", "#ffffff"},
        {"chafet", "Chafet", "personajes", "EPICA", "Mejor amigo de Axelis y ni\u00f1o prodigio del grupo. Gran habilidad en Valorant y siempre est\u00e1 ah\u00ed.", "#f0d890", "#ffffff"},
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
