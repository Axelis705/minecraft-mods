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
        {"corazon_magma", "Coraz\u00f3n de Magma", "jefes", "LEGENDARIA", "Late colgado de cadenas. Rompe sus anclas y golp\u00e9alo.", "#ff6a1a", "#2a1e24"},
        {"archimago", "Archimago", "jefes", "LEGENDARIA", "Se\u00f1or de la Torre. Cada hechizo avisa antes de caer.", "#4a3590", "#7ae8ff"},
        {"guardian_musgo", "Guardi\u00e1n de Musgo", "criaturas", "RARA", "Mini-jefe del bosque. Golpea el suelo y llama a los Brotes.", "#7a9a6a", "#5fbf4a"},
        {"capataz", "Capataz", "criaturas", "RARA", "Mini-jefe de la forja. Su martillo parte el suelo.", "#b0502a", "#d9d9d9"},
        {"brote", "Brote", "criaturas", "COMUN", "Reto\u00f1o del bosque. Corre hacia ti y no se rinde.", "#6fcf55", "#8a5a2a"},
        {"espinoso", "Espinoso", "criaturas", "COMUN", "Flor carn\u00edvora. Abre los p\u00e9talos y dispara una espina.", "#4a9a3a", "#d9c04a"},
        {"polinizador", "Polinizador", "criaturas", "COMUN", "Vuela en c\u00edrculos y lanza esporas que ralentizan.", "#e0c040", "#7fe08a"},
        {"raizal", "Raizal", "criaturas", "RARA", "G\u00f3lem de ra\u00edces. Te deja inm\u00f3vil si no lo esquivas.", "#6a8a4a", "#8a5a2a"},
        {"escoria", "Escoria", "criaturas", "COMUN", "Roca fundida con pu\u00f1os al rojo vivo.", "#c85a2a", "#4a3a3a"},
        {"chispero", "Chispero", "criaturas", "COMUN", "Horno con patas. Escupe flechas de fuego.", "#ff9a2a", "#ffe27a"},
        {"ascua", "Ascua", "criaturas", "COMUN", "Brasa viva que vuela y te deja d\u00e9bil.", "#ff7a1a", "#ffe27a"},
        {"fundidor", "Fundidor", "criaturas", "RARA", "G\u00f3lem de metal fundido. Hace erupci\u00f3n bajo tus pies.", "#9a9a9a", "#ff5a1a"},
        {"lobo_escarcha", "Lobo de Escarcha", "criaturas", "RARA", "Caza en manada. Cada mordisco enfr\u00eda.", "#9ad0f0", "#ffffff"},
        {"salamandra_lava", "Salamandra de Lava", "criaturas", "COMUN", "Anda sobre la lava y se cura en ella. S\u00e1cala a tierra.", "#3a3438", "#ff7a1a"},
        {"diablillo", "Diablillo", "criaturas", "COMUN", "Vuela en c\u00edrculos ri\u00e9ndose y lanza bolas de fuego.", "#c8322a", "#ffcc40"},
        {"larva_magma", "Larva de Magma", "criaturas", "COMUN", "Gusanito de lava: r\u00e1pido, fr\u00e1gil y muerde con fuego.", "#ff8a2a", "#3a2a2a"},
        {"cultista_volcan", "Cultista del Volc\u00e1n", "criaturas", "RARA", "Columnas de fuego y escudos de lava para los suyos.", "#4a1e18", "#ff8a2a"},
        {"esbirro", "Esbirro", "criaturas", "COMUN", "Guardi\u00e1n encapuchado. Si te alejas, aparece a tu lado.", "#2c2550", "#e0b84a"},
        {"libro_embrujado", "Libro Embrujado", "criaturas", "COMUN", "Vuela pasando hojas y dispara p\u00e1ginas m\u00e1gicas.", "#6a2a8a", "#e8d8b0"},
        {"armadura_encantada", "Armadura Encantada", "criaturas", "RARA", "Parece de adorno, hasta que te acercas demasiado.", "#b8bcc4", "#7a4cff"},
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
        {"axelis705", "Axelis705", "personajes", "EPICA", "Donde otros ven un mundo vac\u00edo, \u00e9l ve todo lo que falta por construir. Todo empez\u00f3 con una idea suya, y todav\u00eda le quedan mil m\u00e1s.", "#ffe27a", "#ffffff"},
        {"jadiel", "Jadiel", "personajes", "EPICA", "Para \u00e9l cada partida es una final y quiere llevarse el trofeo. Ahora, adem\u00e1s, entrena a su peque\u00f1o heredero.", "#c03030", "#ffffff"},
        {"marizux", "Marizux", "personajes", "EPICA", "Su risa suaviza hasta el d\u00eda m\u00e1s pesado. Peque\u00f1a, amable y siempre contenta: con ella el grupo se siente como en casa.", "#e88ab8", "#ffffff"},
        {"odette", "Odette", "personajes", "LEGENDARIA", "Es el mundo de Axelis705. Su luz ilumina cada rinc\u00f3n, y a su lado hasta la noche m\u00e1s larga se siente en calma.", "#8ac8ff", "#ffffff"},
        {"pavi223", "Pavi223", "personajes", "EPICA", "Entrena a su equipo sin descanso y no pierde una batalla por falta de ganas. Si algo es amarillo, ya es su favorito.", "#e0a020", "#ffffff"},
        {"rafa", "Rafa", "personajes", "EPICA", "Su chiste llega justo cuando nadie lo espera. Hasta un creeper lo pensar\u00eda dos veces antes de explotar en medio de la risa.", "#4a9a4a", "#ffffff"},
        {"scardax", "Scardax", "personajes", "EPICA", "Imponente: su presencia se nota apenas aparece. Juega, crea y siempre va a por m\u00e1s, pero ni le menciones Roblox, que lo odia con toda su alma.", "#d03030", "#ffffff"},
        {"chafet", "Chafet", "personajes", "EPICA", "Joven, tranquilo y de punter\u00eda certera, apunta con calma de veterano. Siempre aparece justo cuando el equipo lo necesita.", "#f0d890", "#ffffff"},
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
