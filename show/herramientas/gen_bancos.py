#!/usr/bin/env python3
"""Genera los bancos de fabrica del show (JSON). Los bancos que se editan de verdad viven en config/show_de_axelis/ dentro del juego."""
import json, os
OUT = os.path.join(os.path.dirname(__file__), "..", "src/main/resources/data/show_de_axelis/banco")
os.makedirs(OUT, exist_ok=True)

# ---------------------------------------------------------------- 100 mexicanos dijeron
# (pregunta, [(respuesta, puntos, [alias...]), ...])
Q = [
 ("Menciona una comida mexicana", [("Tacos", 38, ["taco", "tacos al pastor"]), ("Tamales", 18, ["tamal"]), ("Pozole", 14, []), ("Enchiladas", 12, ["enchilada"]), ("Chilaquiles", 10, []), ("Mole", 8, ["mole poblano"])]),
 ("Algo que lleva un taco", [("Tortilla", 25, []), ("Carne", 24, ["bistec", "pastor", "suadero"]), ("Salsa", 18, ["salsa verde", "salsa roja"]), ("Cebolla", 14, []), ("Cilantro", 11, []), ("Limón", 8, ["limon"])]),
 ("Una fruta que se come con chile", [("Mango", 34, []), ("Sandía", 22, ["sandia"]), ("Jícama", 16, ["jicama"]), ("Piña", 12, ["pina"]), ("Naranja", 9, []), ("Papaya", 7, [])]),
 ("Algo que se hace en un cumpleaños", [("Comer pastel", 30, ["pastel", "partir el pastel"]), ("Cantar las mañanitas", 26, ["mañanitas", "cantar", "cantar cumpleaños"]), ("Fiesta", 18, ["hacer fiesta"]), ("Romper la piñata", 12, ["piñata", "pinata"]), ("Recibir regalos", 8, ["regalos"]), ("Apagar las velitas", 6, ["velitas", "soplar las velas"])]),
 ("Una caricatura que veías de niño", [("Dragon Ball", 28, ["dragon ball z", "dbz", "goku"]), ("Los Simpson", 20, ["simpsons", "simpson"]), ("Bob Esponja", 18, ["bob esponja"]), ("Pokémon", 14, ["pokemon"]), ("El Chavo del 8", 12, ["el chavo", "chavo"]), ("Tom y Jerry", 8, [])]),
 ("Algo que se te olvida al salir de casa", [("Llaves", 34, ["las llaves"]), ("Celular", 26, ["telefono", "teléfono", "cel"]), ("Cartera", 20, ["billetera"]), ("Cargador", 10, []), ("Paraguas", 6, ["sombrilla"]), ("Cubrebocas", 4, ["tapabocas"])]),
 ("Un lugar donde te da miedo estar solo", [("Cementerio", 30, ["panteon", "panteón"]), ("Casa abandonada", 22, ["casa vieja", "casa embrujada"]), ("Bosque", 16, ["bosque oscuro"]), ("Sótano", 12, ["sotano"]), ("Elevador", 12, ["ascensor"]), ("Baño de noche", 8, ["baño", "bano"])]),
 ("Algo que se come en Navidad", [("Pavo", 28, ["guajolote"]), ("Buñuelos", 16, ["bunuelos"]), ("Ponche", 16, []), ("Romeritos", 14, []), ("Tamales", 14, ["tamal"]), ("Bacalao", 12, [])]),
 ("Una marca de refresco", [("Coca-Cola", 40, ["coca cola", "coca"]), ("Pepsi", 22, []), ("Sprite", 12, []), ("Fanta", 10, []), ("Jarritos", 9, []), ("Fresca", 7, [])]),
 ("Un videojuego famoso", [("Minecraft", 34, []), ("Mario", 18, ["super mario", "mario bros"]), ("Fortnite", 16, []), ("Zelda", 12, ["the legend of zelda"]), ("FIFA", 10, ["fifa 23", "ea fc"]), ("GTA", 10, ["gta v", "grand theft auto"])]),
 ("Algo que haces cuando no hay internet", [("Reiniciar el módem", 26, ["reiniciar el modem", "modem", "módem", "reiniciar router", "router"]), ("Dormir", 20, ["dormirme"]), ("Ver la tele", 18, ["television", "televisión", "tele"]), ("Quejarte", 14, ["quejarse", "enojarme", "enojarse"]), ("Usar datos", 12, ["datos del celular", "datos"]), ("Leer", 10, ["leer un libro"])]),
 ("Una mascota común", [("Perro", 45, ["perrito"]), ("Gato", 30, ["gatito"]), ("Pez", 10, ["pescado", "pecesito"]), ("Hámster", 6, ["hamster"]), ("Pájaro", 5, ["pajaro", "perico", "canario"]), ("Tortuga", 4, [])]),
 ("Algo que no puede faltar en una fiesta", [("Música", 28, ["musica"]), ("Pastel", 20, ["comida"]), ("Globos", 18, ["globo"]), ("Piñata", 14, ["pinata"]), ("Botana", 12, ["botanas"]), ("Bebidas", 8, ["alcohol", "chelas", "cerveza", "refresco"])]),
 ("Una excusa para llegar tarde", [("Tráfico", 42, ["trafico", "transito", "tránsito"]), ("Me quedé dormido", 24, ["me dormi", "me quede dormido", "se me hizo tarde"]), ("No sonó la alarma", 14, ["la alarma", "alarma"]), ("Se ponchó la llanta", 8, ["llanta ponchada", "ponchadura"]), ("Falló el transporte", 7, ["no pasaba el camion", "camion", "metro"]), ("Mi perro se comió mi tarea", 5, ["perro"])]),
 ("Qué querías ser de grande cuando eras niño", [("Futbolista", 28, ["jugador de futbol", "futbolista profesional"]), ("Doctor", 22, ["médico", "medico"]), ("Bombero", 18, []), ("Astronauta", 14, []), ("Youtuber", 10, ["streamer"]), ("Policía", 8, ["policia"])]),
 ("Algo que se rompe fácil", [("Vidrio", 30, ["cristal", "un vaso", "vaso"]), ("Corazón", 22, ["corazon"]), ("Pantalla del celular", 16, ["celular", "pantalla"]), ("Huevo", 14, ["huevos"]), ("Promesas", 10, ["una promesa"]), ("Plato", 8, ["platos"])]),
 ("Un sabor de helado", [("Chocolate", 34, []), ("Vainilla", 30, []), ("Fresa", 20, []), ("Limón", 8, ["limon"]), ("Mango", 5, []), ("Café", 3, ["cafe"])]),
 ("Algo que llevas a la playa", [("Bloqueador", 26, ["protector solar", "bronceador"]), ("Toalla", 24, []), ("Sombrilla", 16, ["parasol"]), ("Lentes de sol", 14, ["lentes", "gafas"]), ("Traje de baño", 12, ["bañador", "short"]), ("Hielera", 8, ["nevera"])]),
 ("Un país que quisieras visitar", [("Japón", 30, ["japon"]), ("Francia", 16, ["paris", "parís"]), ("España", 14, ["espana"]), ("Italia", 14, []), ("Estados Unidos", 14, ["eeuu", "usa"]), ("Corea", 12, ["corea del sur"])]),
 ("Algo que haces en la regadera", [("Cantar", 44, []), ("Pensar", 20, []), ("Lavarte el cabello", 12, ["lavarme el pelo", "shampoo", "champú", "champu"]), ("Hacer pipí", 12, ["orinar", "pipi", "pipí"]), ("Rascarte", 6, []), ("Llorar", 6, [])]),
 ("Un deporte", [("Fútbol", 46, ["futbol"]), ("Basquetbol", 18, ["baloncesto", "basket"]), ("Béisbol", 12, ["beisbol"]), ("Natación", 8, ["natacion"]), ("Box", 8, ["boxeo"]), ("Tenis", 8, [])]),
 ("Algo que da miedo", [("Arañas", 26, ["aranas", "araña"]), ("La oscuridad", 22, ["oscuridad", "la obscuridad"]), ("Payasos", 16, ["payaso"]), ("Fantasmas", 14, ["fantasma"]), ("Las alturas", 12, ["alturas"]), ("Cucarachas", 10, ["cucaracha"])]),
 ("Algo que hay en un altar de muertos", [("Cempasúchil", 30, ["cempasuchil", "flores de cempasuchil", "flores"]), ("Pan de muerto", 26, ["pan"]), ("Fotos", 14, ["fotografias", "fotografías", "foto"]), ("Veladoras", 14, ["velas", "veladora"]), ("Calaveritas", 10, ["calaveras", "calavera de azucar"]), ("Agua", 6, ["vaso de agua"])]),
 ("Una app de tu celular", [("WhatsApp", 36, ["whats", "wasap"]), ("TikTok", 18, ["tiktok", "tik tok"]), ("Instagram", 16, ["insta"]), ("YouTube", 12, ["you tube"]), ("Spotify", 10, []), ("Facebook", 8, ["face"])]),
 ("Un instrumento musical", [("Guitarra", 34, []), ("Piano", 20, []), ("Batería", 16, ["bateria"]), ("Violín", 12, ["violin"]), ("Flauta", 10, []), ("Trompeta", 8, [])]),
 ("Algo que hay en una escuela", [("Pizarrón", 24, ["pizarron", "pizarra"]), ("Maestros", 22, ["maestro", "profesores", "profesor"]), ("Pupitres", 16, ["pupitre", "bancas", "butacas"]), ("Patio", 14, ["cancha"]), ("Tienda", 12, ["cooperativa", "tiendita"]), ("Biblioteca", 12, [])]),
 ("Algo que haces cuando juega México en el Mundial", [("Gritar gol", 28, ["gritar", "gritar goool"]), ("Ver el partido con amigos", 24, ["ver el partido", "ver con amigos"]), ("Comer botana", 20, ["botana", "comer"]), ("Pintarte la cara", 12, ["pintarse la cara"]), ("Llorar", 10, ["sufrir"]), ("Ponerte la playera", 6, ["playera", "camiseta"])]),
 ("Un superhéroe", [("Spider-Man", 30, ["spiderman", "hombre araña", "hombre arana"]), ("Batman", 26, []), ("Superman", 20, []), ("Hulk", 10, []), ("Iron Man", 8, ["ironman"]), ("Thor", 6, [])]),
 ("Algo que dice un presentador de televisión", [("Bienvenidos", 30, ["bienvenidos al programa", "bienvenidos al show"]), ("Volvemos después de los comerciales", 26, ["volvemos", "comerciales", "ya regresamos"]), ("Un aplauso", 18, ["aplausos", "un fuerte aplauso"]), ("Gracias por ver", 14, ["gracias por vernos"]), ("Quédense con nosotros", 12, ["no se vayan", "quedense"])]),
 ("Algo que compras en el súper sin planearlo", [("Dulces", 24, ["dulce"]), ("Papas", 20, ["papitas", "botanas"]), ("Refresco", 18, ["soda"]), ("Helado", 14, []), ("Galletas", 14, ["galleta"]), ("Chicles", 10, ["chicle"])]),
 ("Un lugar para ir de vacaciones", [("La playa", 36, ["playa"]), ("Cancún", 18, ["cancun"]), ("La montaña", 14, ["montaña", "montana"]), ("Disneylandia", 14, ["disney"]), ("Un pueblo mágico", 10, ["pueblo magico", "pueblo"]), ("La casa de la abuela", 8, ["casa de la abuela", "con la abuela"])]),
 ("Algo que tiene ruedas", [("Carro", 30, ["coche", "auto", "automovil"]), ("Bicicleta", 26, ["bici"]), ("Patineta", 14, ["skate", "monopatin"]), ("Moto", 12, ["motocicleta"]), ("Carrito del súper", 10, ["carrito", "carrito del super"]), ("Patines", 8, [])]),
 ("Algo que dice una mamá mexicana", [("Ponte un suéter", 26, ["ponte un sueter", "abrigate", "suéter"]), ("Ya te dije que no", 22, ["que no", "no"]), ("Cómete todo", 20, ["comete todo", "terminate la comida"]), ("Apaga la luz", 14, []), ("¿Ya hiciste tu tarea?", 12, ["la tarea", "ya hiciste la tarea"]), ("Te voy a dar con la chancla", 6, ["chancla", "la chancla"])]),
 ("Una película de Disney", [("El Rey León", 28, ["rey leon", "el rey leon"]), ("Frozen", 20, []), ("Toy Story", 18, []), ("Coco", 16, []), ("Moana", 10, []), ("Aladdín", 8, ["aladdin"])]),
 ("Algo que haces cuando te da sueño", [("Bostezar", 34, []), ("Tomar café", 24, ["cafe", "tomar cafe"]), ("Dormir", 20, ["tomar una siesta", "siesta"]), ("Cerrar los ojos", 10, []), ("Lavarte la cara", 6, []), ("Ver videos", 6, ["tiktok"])]),
 ("Una parte de la casa", [("Cocina", 28, []), ("Sala", 24, []), ("Recámara", 22, ["recamara", "cuarto", "habitacion"]), ("Baño", 16, ["bano"]), ("Patio", 6, []), ("Cochera", 4, ["garage", "garaje"])]),
 ("Un color", [("Azul", 30, []), ("Rojo", 26, []), ("Verde", 16, []), ("Negro", 12, []), ("Amarillo", 10, []), ("Rosa", 6, ["rosado"])]),
 ("Algo que siempre se pierde", [("Calcetines", 28, ["calcetin", "calcetín", "un calcetin", "calcetas"]), ("Llaves", 22, []), ("Control remoto", 20, ["el control", "control"]), ("Paciencia", 12, ["la paciencia"]), ("Cargador", 10, []), ("La noción del tiempo", 8, ["el tiempo", "nocion del tiempo"])]),
 ("Algo que se hace en Minecraft", [("Minar", 32, ["picar", "excavar"]), ("Construir", 28, ["casas", "hacer casas"]), ("Pelear con creepers", 14, ["pelear", "matar mobs", "luchar"]), ("Sobrevivir", 12, []), ("Explorar", 8, []), ("Pescar", 6, [])]),
 ("Algo que hay en el Nether", [("Lava", 28, []), ("Ghasts", 18, ["ghast"]), ("Piglins", 16, ["piglin", "cerdos"]), ("Fortaleza", 14, ["fortaleza del nether"]), ("Netherrack", 14, ["roca del nether"]), ("Esqueletos wither", 10, ["esqueleto wither", "wither", "esqueletos"])]),
 ("Algo que da miedo en Minecraft", [("Creeper", 40, ["creepers"]), ("Wither", 18, []), ("Enderman", 14, ["endermans"]), ("Warden", 14, []), ("Las cuevas", 8, ["cuevas", "cueva"]), ("Esqueletos", 6, ["esqueleto"])]),
 ("Un animal de la granja", [("Vaca", 30, []), ("Gallina", 22, ["gallo", "pollo"]), ("Cerdo", 18, ["puerco", "marrano"]), ("Caballo", 12, []), ("Oveja", 10, []), ("Burro", 8, [])]),
 ("Una cosa que se hace en el cine", [("Comer palomitas", 40, ["palomitas", "comer"]), ("Ver una película", 20, ["ver peliculas", "ver pelicula"]), ("Apagar el celular", 12, []), ("Dormirse", 12, ["dormir"]), ("Pelear por el reposabrazos", 8, ["reposabrazos"]), ("Llorar", 8, [])]),
]

# ---------------------------------------------------------------- trivia (A/B/C/D, V/F, no cuadra, quien habla, mayor o menor)
T = []
def tri(cat, q, ops, ok, note=""):
    T.append({"cat": cat, "pregunta": q, "op": ops, "ok": ok, "nota": note})

TR = "Trivia"
tri(TR, "¿Cuántos estados tiene México, sin contar la Ciudad de México?", ["29", "30", "31", "32"], 2, "31 estados y la CDMX suman 32 entidades.")
tri(TR, "¿Qué animal aparece en el escudo nacional de México?", ["Un jaguar", "Un águila", "Un quetzal", "Un lobo"], 1, "Un águila parada sobre un nopal devorando una serpiente.")
tri(TR, "¿De qué color es la franja del centro de la bandera de México?", ["Verde", "Roja", "Blanca", "Azul"], 2)
tri(TR, "¿En qué estado nació el tequila?", ["Veracruz", "Jalisco", "Oaxaca", "Sonora"], 1, "En Jalisco, en el pueblo de Tequila.")
tri(TR, "¿En qué año empezó la Independencia de México (el Grito de Dolores)?", ["1521", "1810", "1857", "1910"], 1)
tri(TR, "¿En qué estado está Chichén Itzá?", ["Chiapas", "Yucatán", "Campeche", "Oaxaca"], 1)
tri(TR, "¿Qué ingrediente es la base del guacamole?", ["Tomate", "Pepino", "Aguacate", "Chile"], 2)
tri(TR, "¿Cuál es el volcán más alto de México?", ["Popocatépetl", "Pico de Orizaba", "Iztaccíhuatl", "Nevado de Toluca"], 1, "El Pico de Orizaba, con unos 5,600 metros.")
tri(TR, "¿De qué ciudad es el mole poblano?", ["Puebla", "Mérida", "Monterrey", "Tijuana"], 0)
tri(TR, "¿En cuántos Mundiales de fútbol ha sido sede México, contando el de 2026?", ["Uno", "Dos", "Tres", "Cuatro"], 2, "1970, 1986 y 2026 (este último compartido con Estados Unidos y Canadá).")
tri(TR, "¿Con qué se construye el portal al Nether?", ["Diamante", "Bedrock", "Hierro", "Obsidiana"], 3)
tri(TR, "¿Cuántos corazones de vida tiene el jugador de Minecraft?", ["5", "10", "20", "3"], 1)
tri(TR, "¿En qué dimensión vive el Ender Dragon?", ["El Nether", "El Overworld", "El End", "La Aurora"], 2)
tri(TR, "¿Con qué se doma a un lobo en Minecraft?", ["Huesos", "Manzanas", "Trigo", "Pescado"], 0)
tri(TR, "¿Cuántos marcos tiene un portal del End completo?", ["8", "10", "12", "16"], 2)
tri(TR, "¿Quién creó Minecraft?", ["Notch", "Shigeru Miyamoto", "Gabe Newell", "Steve Jobs"], 0, "Markus \"Notch\" Persson.")
tri(TR, "¿Qué mob suelta perlas de ender?", ["Creeper", "Enderman", "Ghast", "Bruja"], 1)
tri(TR, "¿Cuántos planetas tiene el Sistema Solar?", ["7", "8", "9", "10"], 1, "Plutón ya no cuenta, pobrecito.")
tri(TR, "¿Cuál es el océano más grande del mundo?", ["Atlántico", "Índico", "Pacífico", "Ártico"], 2)
tri(TR, "¿Cuántos huesos tiene un adulto?", ["106", "206", "306", "406"], 1)
tri(TR, "¿Qué gas es el más abundante en el aire que respiramos?", ["Oxígeno", "Nitrógeno", "Dióxido de carbono", "Helio"], 1, "El nitrógeno, casi el 78%. El oxígeno es solo el 21%.")
tri(TR, "¿Cuál es el animal terrestre más rápido?", ["Guepardo", "León", "Caballo", "Avestruz"], 0)
tri(TR, "¿Cuál es el país más grande del mundo?", ["China", "Canadá", "Estados Unidos", "Rusia"], 3)
tri(TR, "¿En qué año pisó el ser humano la Luna por primera vez?", ["1959", "1969", "1979", "1989"], 1)
tri(TR, "¿Qué metal es líquido a temperatura ambiente?", ["Plomo", "Mercurio", "Estaño", "Cobre"], 1)
tri(TR, "¿Cuál es la capital de Japón?", ["Kioto", "Osaka", "Tokio", "Nagoya"], 2)
tri(TR, "¿Cuál es el animal más grande del planeta?", ["Elefante", "Tiburón ballena", "Ballena azul", "Calamar gigante"], 2)
tri(TR, "¿Cuál es el símbolo químico del oro?", ["Or", "Au", "Ag", "Go"], 1)
tri(TR, "¿Quién escribió Don Quijote de la Mancha?", ["Cervantes", "Lope de Vega", "Neruda", "García Márquez"], 0)
tri(TR, "¿Cuál es el hueso más largo del cuerpo humano?", ["Húmero", "Fémur", "Tibia", "Columna"], 1)
tri(TR, "¿Qué planeta es conocido como el planeta rojo?", ["Venus", "Júpiter", "Marte", "Mercurio"], 2)
tri(TR, "¿De qué compañía es el personaje Mario?", ["Sega", "Nintendo", "Sony", "Atari"], 1)
tri(TR, "¿Qué número tiene Pikachu en la Pokédex?", ["1", "25", "100", "150"], 1)
tri(TR, "¿Cómo se llama la princesa que Mario siempre rescata?", ["Zelda", "Daisy", "Peach", "Rosalina"], 2)
tri(TR, "¿Cuántos meses tienen 28 días?", ["Uno", "Seis", "Los doce", "Ninguno"], 2, "Todos tienen al menos 28 días. Sí, es una trampa.")
tri(TR, "Un gallo pone un huevo en la punta de un techo. ¿Hacia dónde cae el huevo?", ["A la izquierda", "A la derecha", "Hacia el frente", "Los gallos no ponen huevos"], 3)
tri(TR, "¿Cuánto es 1 + 1 × 0?", ["0", "1", "2", "Depende de tu humor"], 1, "Primero se multiplica: 1 + 0 = 1.")
tri(TR, "¿Qué pesa más: un kilo de plumas o un kilo de plomo?", ["Las plumas", "El plomo", "Pesan lo mismo", "Depende de la gravedad"], 2)
tri(TR, "¿De qué color era el caballo blanco de Napoleón?", ["Negro", "Gris", "Café", "Blanco"], 3, "Lo dice la pregunta, pero nadie lee.")
tri(TR, "¿Cuántos animales de cada especie metió Moisés al arca?", ["Dos", "Siete", "Ninguno, fue Noé", "Cuarenta"], 2)
tri(TR, "Un avión se estrella justo en la frontera entre dos países. ¿Dónde entierran a los sobrevivientes?", ["En un país", "En el otro", "En el medio", "En ninguno: están vivos"], 3)
tri(TR, "¿Qué letra hay en medio del mar?", ["La A", "La R", "La M", "La Z"], 0, "M-A-R. La A. Gracias por jugar.")
tri(TR, "¿Cuál es el mejor programa de televisión de la historia?", ["Este", "Este también", "Obviamente este", "Todas las anteriores"], 3, "Respuesta correcta, aunque nadie lo discute.")
tri(TR, "¿Cuántos minutos faltan para que acabe el show?", ["Pocos", "Muchos", "Depende del público", "Sí"], 3, "Sí.")
tri(TR, "¿Qué fruta es la que más vitamina C tiene entre estas?", ["Plátano", "Manzana", "Guayaba", "Pera"], 2, "La guayaba supera incluso a la naranja.")
tri(TR, "¿Cuántas patas tiene una araña?", ["6", "8", "10", "12"], 1)
tri(TR, "¿Cuántos lados tiene un hexágono?", ["5", "6", "7", "8"], 1)
tri(TR, "¿Qué instrumento tiene teclas blancas y negras?", ["Guitarra", "Violín", "Piano", "Flauta"], 2)
tri(TR, "¿En qué continente está Egipto?", ["Asia", "Europa", "África", "Oceanía"], 2)
tri(TR, "¿Cuántos jugadores de cada equipo juegan en la cancha en un partido de fútbol?", ["9", "10", "11", "12"], 2)

VF = "True o Fake"
def vf(q, ok, note=""):
    tri(VF, q, ["Verdadero", "Falso"], 0 if ok else 1, note)
vf("Los pulpos tienen tres corazones.", True)
vf("El Sol es una estrella.", True)
vf("Los murciélagos son ciegos.", False, "Ven bastante bien; además usan eco.")
vf("El tomate es una fruta.", True, "Botánicamente sí, aunque nadie lo pone en la ensalada de frutas.")
vf("Usamos solo el 10% del cerebro.", False, "Es un mito: usamos todo el cerebro.")
vf("En Minecraft, un cerdo golpeado por un rayo se convierte en piglin zombificado.", True)
vf("Los avestruces esconden la cabeza bajo tierra cuando tienen miedo.", False)
vf("La Gran Muralla China se ve a simple vista desde la Luna.", False)
vf("La abeja melífera muere después de picar.", True)
vf("México reconoce más de 60 lenguas indígenas como lenguas nacionales.", True, "Son 68 en total.")
vf("Los gatos siempre caen de pie.", False, "Casi siempre, pero no siempre.")
vf("El chile pica por una sustancia llamada capsaicina.", True)
vf("Un rayo nunca cae dos veces en el mismo lugar.", False)
vf("Los delfines son peces.", False, "Son mamíferos.")
vf("El creeper nació de un error al intentar hacer un cerdo en Minecraft.", True)
vf("El diamante es uno de los materiales naturales más duros que existen.", True)
vf("El corazón de un camarón está en su cabeza.", True)
vf("Napoleón era extremadamente bajito.", False, "Medía lo normal para su época.")
vf("Comer zanahorias mejora la vista en la oscuridad.", False, "Es propaganda de la Segunda Guerra Mundial.")
vf("El tequila se hace con agave.", True)
vf("La Torre Eiffel crece unos centímetros en verano.", True, "El calor dilata el metal.")
vf("Los pingüinos viven en el Polo Norte.", False, "Viven en el sur; en el Polo Norte hay osos.")
vf("Un día en Venus dura más que un año en Venus.", True, "Gira tan lento que sí.")
vf("Los humanos tenemos exactamente cinco sentidos.", False)

NC = "Algo no cuadra"
def nc(ops, ok, note):
    tri(NC, "¿Cuál NO pertenece al grupo?", ops, ok, note)
nc(["Taco", "Enchilada", "Pozole", "Sushi"], 3, "El sushi no es mexicano.")
nc(["Creeper", "Zombi", "Esqueleto", "Vaca"], 3, "La vaca no es hostil.")
nc(["Rojo", "Azul", "Verde", "Lunes"], 3, "El lunes no es un color (aunque lo parezca).")
nc(["Guitarra", "Violín", "Arpa", "Martillo"], 3, "El martillo no es un instrumento musical.")
nc(["Mario", "Luigi", "Peach", "Sonic"], 3, "Sonic es de Sega, no del mundo de Mario.")
nc(["Perro", "Gato", "Hámster", "Tiburón"], 3, "Nadie tiene un tiburón de mascota.")
nc(["Manzana", "Plátano", "Zanahoria", "Uva"], 2, "La zanahoria es verdura.")
nc(["Oro", "Plata", "Cobre", "Madera"], 3, "La madera no es un metal.")
nc(["Lunes", "Martes", "Jueves", "Enero"], 3, "Enero es un mes, no un día.")
nc(["Tokio", "París", "Lima", "Brasil"], 3, "Brasil es un país, las demás son ciudades.")
nc(["Pikachu", "Charmander", "Squirtle", "Mickey"], 3, "Mickey no es un Pokémon.")
nc(["Rey", "Reina", "Alfil", "Dado"], 3, "El dado no se usa en el ajedrez.")
nc(["Spider-Man", "Batman", "Superman", "Gandalf"], 3, "Gandalf es mago, no superhéroe.")
nc(["Minecraft", "Terraria", "Fortnite", "Excel"], 3, "Excel no es un videojuego (aunque algunos lo juegan).")
nc(["Aguacate", "Tomate", "Cebolla", "Zapato"], 3, "El zapato no va en el guacamole.")

QH = "¿Quién habla?"
def qh(frase, ops, ok, note=""):
    tri(QH, "\"" + frase + "\"  ¿Quién lo dice?", ops, ok, note)
qh("Ssssssss...", ["Un creeper", "Un aldeano", "Un cerdo", "Steve"], 0)
qh("Hrmmmm.", ["Una oveja", "Un aldeano", "Un lobo", "Una gallina"], 1)
qh("Hasta la vista, baby.", ["Terminator", "Rambo", "Shrek", "Goku"], 0)
qh("Yo soy tu padre.", ["Darth Vader", "Gru", "Homero", "Gandalf"], 0)
qh("Hakuna Matata.", ["Timón y Pumba", "Woody y Buzz", "Shrek y Burro", "Tom y Jerry"], 0)
qh("¡Cowabunga!", ["Las Tortugas Ninja", "Los Simpson", "Pokémon", "Bob Esponja"], 0)
qh("¡Ay, caramba!", ["Bart Simpson", "Bob Esponja", "El Chavo", "Goku"], 0)
qh("Fue sin querer queriendo.", ["El Chavo del 8", "El Chapulín", "Mickey", "Don Gato"], 0)
qh("¡Bazinga!", ["Sheldon Cooper", "Tony Stark", "Walter White", "Harry Potter"], 0)
qh("Houston, tenemos un problema.", ["Un astronauta", "Un mesero", "Un futbolista", "Un aldeano"], 0)
qh("Yo soy Groot.", ["Groot", "Hulk", "Thor", "Un árbol cualquiera"], 0, "Dijo lo único que sabe decir.")
tri("Ejemplo (no se usa)", "\"[Escribe aquí una frase típica de tu amigo]\"  ¿Quién lo dice?", ["Amigo 1", "Amigo 2", "Amigo 3", "Amigo 4"], 0,
    "EDITA ESTO: copia este bloque, cambia la frase y los nombres, y pon cat: \"¿Quién habla?\" para que salga en el show.")

MM = "Mayor o menor"
def mm(q, a, b, ok, note=""):
    tri(MM, q, [a, b], ok, note)
mm("¿Qué es más alto?", "Torre Eiffel", "Estatua de la Libertad", 0, "330 m contra 93 m.")
mm("¿Qué país tiene más habitantes?", "México", "España", 0, "Unos 130 millones contra unos 48.")
mm("¿Qué planeta es más grande?", "Júpiter", "Saturno", 0)
mm("¿Qué es más antiguo?", "Chichén Itzá", "Torre Eiffel", 0, "Chichén Itzá tiene más de mil años.")
mm("¿Qué pesa más?", "Elefante africano", "Ballena azul", 1)
mm("¿Qué es más largo?", "Río Amazonas", "Río Támesis", 0)
mm("¿Quién tiene más estados?", "México", "Estados Unidos", 1, "México tiene 31 estados; Estados Unidos, 50.")
mm("¿Quién es más rápido?", "Guepardo", "Caballo de carreras", 0)
mm("¿Qué es más alto en Minecraft?", "El Overworld", "El Nether", 0, "El Overworld mide 384 bloques de alto; el Nether, 256.")
mm("¿Qué está más caliente?", "La superficie del Sol", "Un rayo", 1, "Un rayo llega a unos 30,000 °C; la superficie del Sol, a unos 5,500 °C.")
mm("¿Quién tiene más vida en Minecraft Java?", "Ender Dragon", "Wither", 1, "El Wither tiene 300 de vida y el Dragón 200.")
mm("¿Qué palabra tiene más letras?", "Hipopótamo", "Elefante", 0)
mm("¿Qué tiene más patas?", "Una araña", "Una hormiga", 0, "8 contra 6.")

for i, q in enumerate(T):
    q["id"] = "t%03d" % i

# ---------------------------------------------------------------- palabras del impostor
W = [
 ("Comida", "tacos pizza hamburguesa sushi helado pozole sopa pastel tamal ensalada palomitas chocolate".split()),
 ("Animales", "perro gato elefante tiburón jirafa pingüino caballo mariposa pulpo búho tortuga conejo".split()),
 ("Lugares", "playa escuela hospital cine aeropuerto biblioteca parque estadio panadería circo museo cueva".split()),
 ("Objetos de casa", "refrigerador sofá espejo lámpara cuchara almohada microondas escoba reloj cortina tijeras llave".split()),
 ("Minecraft", "creeper diamante antorcha pico aldeano portal cofre zombi cama obsidiana pepita elytra".split()),
 ("Profesiones", "doctor bombero maestro piloto cocinero policía payaso astronauta cantante mecánico dentista futbolista".split()),
 ("Cosas absurdas", "calcetín-suelto wifi-lento tarea-perdida lunes sueño chancla piñata alarma resaca dieta karaoke fantasma".split()),
 ("Deportes", "fútbol natación boxeo basquetbol tenis ciclismo ajedrez surf voleibol golf esgrima béisbol".split()),
]
words = [{"cat": c, "palabras": [p.replace("-", " ") for p in ps]} for c, ps in W]

# ---------------------------------------------------------------- adivina la palabra (palabra prohibida)
P = [
 ("Playa", "mar arena sol agua"), ("Pizza", "queso italiana masa redonda"), ("Perro", "ladrar mascota cola animal"), ("Cumpleaños", "pastel fiesta regalos velitas"),
 ("Escuela", "maestro tarea estudiar salón"), ("Celular", "llamar teléfono pantalla mensaje"), ("Hospital", "doctor enfermo médico cama"), ("Helado", "frío postre cono sabor"),
 ("Avión", "volar aeropuerto alas cielo"), ("Cine", "película palomitas pantalla sala"), ("Piñata", "romper palo dulces fiesta"), ("Tacos", "tortilla carne salsa mexicano"),
 ("Minecraft", "bloques cubos construir creeper"), ("Creeper", "explota verde minecraft sssss"), ("Mundial", "fútbol copa selección gol"), ("Navidad", "pino regalos santa diciembre"),
 ("Dormir", "cama sueño noche almohada"), ("Bicicleta", "ruedas pedal manubrio montar"), ("Pastel", "cumpleaños dulce horno velas"), ("Fantasma", "miedo muerto sábana espíritu"),
 ("Karaoke", "cantar micrófono canción voz"), ("Pirata", "barco tesoro parche loro"), ("Computadora", "teclado mouse pantalla internet"), ("Médico", "hospital bata enfermo salud"),
 ("Policía", "ladrón multa patrulla uniforme"), ("Payaso", "circo nariz risa globos"), ("Dentista", "dientes muela boca sillón"), ("Playera", "ropa camisa vestir tela"),
 ("Selfie", "foto celular cámara retrato"), ("Chancla", "pie sandalia mamá zapato"),
]
taboo = [{"palabra": a, "no": b.split()} for a, b in P]

# ---------------------------------------------------------------- frases del locutor
L = {
 "show_start": ["Damas y caballeros, y también las frutas del público, bienvenidos al Show de Axelis. Soy un locutor invisible y estoy contratado por horas.",
                "Buenas noches, buenos días o lo que sea que sea. Estás en el mejor programa de televisión de este grupo de amigos. No me cuestionen.",
                "¡Se enciende la luz roja! O verde. Alguien conectó mal las luces. Comencemos."],
 "round_start": ["Primera pregunta, primer error probable. Comencemos.", "Atención, concursantes: respiren, piensen y luego hagan lo contrario.", "Va una ronda más. El público no pidió esto pero aquí estamos.", "Se abre la pregunta. Los nervios son gratis."],
 "buzz": ["¡{player} tocó el timbre! Con firmeza. Con ganas. Con ruido.", "{player} se lanzó al timbre como si fuera lo último en el mundo.", "¡Timbre de {player}! Ojalá sepa lo que va a decir."],
 "dup": ["Eso ya estaba arriba, pero qué bonito entusiasmo.", "Ya lo dijeron, {player}. Pero gracias por confirmar que el tablero existe.", "Repetida. El público ya lo había visto."],
 "reveal": ["¡Está en el tablero! Ahora todos fingen que lo sabían.", "¡Respuesta correcta! Los jueces han dejado de dormir.", "¡Ahí está! Aplausos fingidos, por favor.", "Eso valía {text}. Bueno, valía puntos."],
 "wrong": ["Respuesta incorrecta. Estamos tristes y también un poquito felices.", "Eso no está en el tablero, ni cerca, ni en otro tablero.", "Mmm. No."],
 "strike": ["¡Una X! Qué atrevimiento equivocarse en televisión nacional.", "¡X para {side}! Duele más cuando hay público.", "Error. Gran error. Error de los que se recuerdan.", "¡Equis! Como la del trabajo cuando no llegas."],
 "steal": ["¡Tres errores! ¡El banco está libre y el rival respira de felicidad!", "¡Robo! Una palabra que da miedo y emoción al mismo tiempo.", "Se abre la oportunidad de robar. En televisión es legal."],
 "steal_ok": ["¡Robo exitoso! Nadie vio nada, todos aplauden.", "¡Se lo llevaron! El equipo contrario guarda un minuto de silencio.", "¡Qué atraco tan elegante, {player}!"],
 "steal_fail": ["Intento de robo fallido. La seguridad del programa está orgullosa.", "El robo no salió. Lo más cercano a un robo fue la pregunta.", "Fallaste, pero la intención es lo que cuenta. Y también los puntos."],
 "control": ["¡El control es de {side}! Úsenlo con sabiduría o al menos con ritmo.", "Tienen el control. Por favor, no lo pierdan, es el único que tenemos.", "{side} toma el mando. Ya era hora de que alguien lo hiciera."],
 "face_none": ["Nadie acertó. Cinco segundos de silencio incómodo y seguimos.", "Una pregunta tan difícil que hasta el público se rió de nervios.", "Los grillos del estudio piden más participación."],
 "round_win": ["¡{side} se lleva {pts} puntos! Un fuerte aplauso o por lo menos uno discreto.", "¡Ronda para {side}! Los demás tendrán que conformarse con la dignidad.", "¡{pts} puntos para {side}! Eso es mucho. Es bastante, la verdad."],
 "round_none": ["Nadie se llevó nada. Es histórico, pero por malas razones."],
 "trivia_start": ["Pregunta de trivia. Tienen un tiempo limitado, como los programas de televisión.", "A contestar rápido: el que acierta primero, gana más. Es la ley del show.", "Atención, es una pregunta con trampa. O sin trampa. Ya verán."],
 "trivia_reveal": ["Y la respuesta correcta es... ¡esa!", "Se acabó el tiempo. Los que acertaron, felicidades; los demás, mejor suerte en su otra vida.", "Veamos quién estudió y quién solo vino por la comida."],
 "trivia_none": ["Nadie acertó. Increíble. Se pasará a la siguiente pregunta con luto.", "Cero aciertos. Los jueces recomiendan leer más libros y ver menos videos."],
 "imp_start": ["¡Hay un impostor entre ustedes! Yo sé quién es. No les voy a decir. Soy muy profesional.", "Todos tienen una palabra secreta... menos uno. Que disimule, que disimule.", "Comienza El Impostor. Confíen en todos. O en nadie. Da igual."],
 "imp_discuss": ["Se acabaron las pistas. Ahora discutan, acusen y mientan con elegancia.", "Hora de la charla. Cualquier parecido con una reunión de trabajo es pura coincidencia."],
 "imp_vote": ["¡A votar! Escriban en el chat el número o el nombre del sospechoso.", "Redoble de tambores: voten a quien crean impostor. O a quien les caiga mal."],
 "imp_caught": ["¡{player} era el impostor! Pero todavía puede adivinar la palabra y salvarse.", "¡Lo atraparon! Aunque aún le queda una oportunidad para lucirse."],
 "imp_escaped": ["¡El impostor era {player} y se salió con la suya! Qué talento actoral.", "Nadie acusó al correcto. {player} era el impostor. Qué escalofrío."],
 "imp_end": ["Y así termina esta ronda de mentiras elegantes. La palabra era {text}.", "Fin del juego. La palabra era {text}. Algunos lo sospechaban, otros mentían muy bien."],
 "draw_start": ["{player} pasa al lienzo. Tiene {a} segundos para convertir una palabra en arte. O en algo parecido.", "Atención: {player} dibuja y los demás adivinan. Se aceptan garabatos, lágrimas y excusas.", "Se abre el museo. {player} es el artista de hoy y su pincel es un ratón."],
 "draw_solved": ["¡{player} lo adivinó! Se lleva {pts} puntos. Alguien entiende de arte moderno.", "¡Correcto, {player}! +{pts}. El dibujante respira aliviado.", "{player} acertó. Nadie sabe cómo, pero acertó."],
 "draw_bad": ["{player}, no se puede decir la palabra. Esto es un dibujo, no una conferencia.", "¡Trampa! {player} intentó soltar la palabra. El museo lo vio todo."],
 "draw_none": ["Se acabó el tiempo y nadie adivinó. La palabra era {text}. El dibujo era una declaración de intenciones.", "Nadie acertó. Era {text}. Se rumora que el dibujante dijo que estaba inspirado."],
 "draw_all": ["¡Todos adivinaron! Era {text}. Eso o el dibujo estaba muy claro o el público muy listo.", "Palabra descubierta por todos: {text}. Un aplauso para el pincel."],
 "taboo_start": ["{player} debe describir una palabra sin decir las prohibidas. Suerte y sin trampa.", "Atención, {side}: {player} describe y ustedes adivinan. Cualquier cosa menos la palabra, claro."],
 "taboo_bad": ["¡Dijiste una palabra prohibida, {player}! Los jueces se tapan los oídos.", "¡Prohibido! Esa palabra estaba en la lista. Te leímos."],
 "taboo_fail": ["Fin de la ronda. La palabra se quedó sin descubrir y los jueces sin paciencia.", "No se adivinó. La palabra era {text}. Cosa fácil, dicen algunos."],
 "taboo_solved": ["¡Adivinada! {player} se lleva {pts} puntos y la admiración de su equipo.", "¡Eso es! La palabra era {text}. Qué momento, qué elegancia."],
 "final": ["Y el ganador del Show de Axelis es... ¡{side}! Los demás recibirán un abrazo imaginario.", "¡Felicidades, {side}! Han ganado un premio increíble y totalmente distinto al anunciado."],
 "final_tie": ["¡Empate! Los jueces han decidido que todos ganaron, pero algunos ganaron un poco más.", "No hay ganador claro. Se resolverá con piedra, papel o tijera detrás de cámaras."],
 "techdiff": ["Estamos experimentando dificultades técnicas. Por favor, no se muevan, no respiren y no cambien de canal.", "Mil disculpas. Alguien tropezó con el cable de la imaginación. Volvemos enseguida.", "Se interrumpe la transmisión por causas ajenas a nuestra voluntad y también a la lógica."],
 "chiste": ["¿Qué hace una abeja en el gimnasio? ¡Zum-ba!", "¿Cuál es el colmo de un electricista? Que su esposa se llame Luz y sus hijos le sigan la corriente.", "Me dijeron que había un público muy exigente. Entonces vine con frutas.", "¿Por qué el libro de matemáticas estaba triste? Porque tenía muchos problemas.", "Dicen que la televisión te vuelve tonto. Yo no, yo ya vine así."],
 "premios": ["una caja vacía de zapatos", "un abrazo de una sandía", "un cupón para otro cupón", "el primer lugar en un concurso que nadie recuerda", "un cuarto de kilo de aplausos congelados", "una silla con aire acondicionado imaginario",
             "una excursión a la tienda de la esquina", "un diploma con tu nombre mal escrito", "un coche (de juguete, pero es tuyo)", "un año de suscripción gratis a nada", "un dragón... pero de dibujo", "la llave del cielo (no abre ninguna puerta)"],
}

RETOS = [
 "imita a una gallina convencida de que es un avión durante diez segundos.",
 "habla como presentador de noticias deportivas mientras describes lo que ves en la pantalla.",
 "di tu nombre completo al revés, con mucha seriedad.",
 "cuenta un chiste tan malo que el público tenga que abuchearte con cariño.",
 "haz tu mejor cara de sorpresa y mantenla hasta que el locutor diga basta.",
 "canta el principio de una canción inventada sobre la sandía.",
 "baila como si estuviera sonando música de ascensor.",
 "describe el sabor del color azul sin usar la palabra azul.",
 "presenta a tu compañero como si fuera la persona más famosa del universo.",
 "da un discurso de agradecimiento por ganar un premio que no existe.",
 "habla en cámara lenta durante quince segundos.",
 "haz un anuncio de televisión de una cuchara.",
 "imita a un creeper que se acaba de enterar de un secreto.",
 "di un trabalenguas con la boca llena de aire.",
 "actúa como si fueras un aldeano pidiendo un descuento en una tienda.",
 "narra un día normal de un calcetín perdido.",
 "inventa un eslogan para este programa en menos de diez segundos.",
 "haz sonidos de animal de granja hasta que alguien adivine cuál es.",
 "recita el abecedario con voz de villano de caricatura.",
 "defiende con total seriedad por qué los lunes deberían ser opcionales.",
]

DIBUJOS = [
 "casa", "perro", "gato", "sol", "luna", "árbol", "carro", "avión", "barco", "bicicleta", "pizza", "taco", "hamburguesa", "helado", "sandía", "banana",
 "manzana", "pulpo", "tiburón", "dinosaurio", "araña", "mariposa", "pingüino", "elefante", "jirafa", "serpiente", "robot", "cohete", "fantasma", "corona",
 "castillo", "guitarra", "pastel de cumpleaños", "paraguas", "reloj", "teléfono", "computadora", "televisión", "escalera", "montaña", "playa", "volcán",
 "arcoíris", "lluvia", "payaso", "pirata", "dragón", "creeper", "espada", "diamante", "cama", "zapato", "sombrero", "lentes", "llave", "bomba",
 "el lunes", "el wifi", "la tarea", "una resaca", "un mal chiste", "un ventilador triste", "una cuchara con miedo", "un pato con traje",
 "un tiburón en la regadera", "mi abuela en moto", "un taco volador", "un oso en el supermercado", "un calcetín perdido", "una nube enojada",
 "un pulpo cocinando", "un astronauta en la tienda", "un perro presidente", "el último pedazo de pizza", "un fantasma con pereza", "un árbol de Navidad en verano",
 "una sandía con lentes", "un gato millonario", "una jirafa en el elevador", "el examen sorpresa",
]

def w(name, data):
    with open(os.path.join(OUT, name), "w", encoding="utf-8") as f:
        json.dump(data, f, ensure_ascii=False, indent=1)

w("100mexicanos.json", [{"id": "m%03d" % i, "pregunta": q, "respuestas": [{"t": t, "p": p, "alias": a} for t, p, a in rs]} for i, (q, rs) in enumerate(Q)])
w("trivia.json", T)
w("palabras.json", words)
w("prohibidas.json", taboo)
w("anuncios.json", L)
w("retos.json", RETOS)
w("dibujos.json", DIBUJOS)
for i, (q, rs) in enumerate(Q):
    s = sum(p for _, p, _ in rs)
    assert 85 <= s <= 105, (q, s)
print(len(Q), "preguntas de 100 mexicanos;", len(T), "de trivia;", len(words), "categorias de impostor;", len(taboo), "prohibidas")
