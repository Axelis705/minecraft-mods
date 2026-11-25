#!/usr/bin/env python3
"""El guion del Coleccionista (los textos de todas sus frases), en un solo lugar.

Cada guion es una lista de frases: (clave, español, inglés, dirección para grabar). La clave es el nombre del audio
(<guion>_<clave>.ogg) y de la línea de idioma (coleccionista.voz.<guion>.<clave>). Las frases que terminan en "risa" son risas
(el subtítulo tiembla y el Coleccionista echa la cabeza atrás).

`python3 herramientas/guion_datos.py` escribe los textos en los tres idiomas y los guiones (con su duración) en Pantallas.java;
luego `herramientas/guion.py` escribe docs/guion-voces.md.
"""
import json
import os
import re

AQUI = os.path.dirname(os.path.abspath(__file__))
MOD = os.path.dirname(AQUI)
LANG = os.path.join(MOD, "src/main/resources/assets/coleccionista/lang")
PANTALLAS = os.path.join(MOD, "src/main/java/com/coleccionista/evento/Pantallas.java")

# Quién es: un coleccionista de porcelana. No odia a nadie: quiere a sus piezas como se quiere un objeto fino. Habla bajito, despacio,
# con cortesía de anticuario; nunca grita. Frases cortas y directas (así las prefiere Axelis). Nunca dice que mata:
# "se quedan conmigo", "en mi colección". Usa palabras de su oficio (vitrina, grieta, despostillar, réplica, original). Cierra cada
# explicación con su frase: "La vitrina está abierta."

GUIONES = {
    "bienvenida": [
        ("silencio", "…", "…", "Solo respira cerca del micrófono, lento, dos segundos."),
        ("hola", "Hola…", "Hello…", "Muy bajito, alargando la o."),
        ("que_tenemos", "Vaya, vaya… ¿Qué tenemos aquí?", "Well, well… What do we have here?", "Curioso, con una sonrisa que se oye."),
        ("durmieron", "Veo que ya despertaron. ¿Durmieron bien?", "I see you're awake. Did you sleep well?", "Cortés, como anfitrión."),
        ("acerquense", "No sean tímidos. Acérquense… (pulsen R para ver la pantalla de cerca)",
         "Don't be shy. Come closer… (press R to see the screen up close)",
         "Amable, invitando. Lo que va entre paréntesis no se dice: solo sale en el subtítulo."),
        ("presento", "Permítanme presentarme. Soy… el Coleccionista.", "Allow me to introduce myself. I am… the Collector.",
         "Pausa antes del nombre. Orgulloso."),
        ("coleccion", "Y ustedes son… mi nueva colección.", "And you are… my new collection.", "Despacio, saboreándolo."),
        ("pruebas", "Esta noche se enfrentarán a varias pruebas, una tras otra.", "Tonight you will face several trials, one after another.",
         "Claro, sin prisa."),
        ("solo_uno", "Y al final… solo uno podrá salir.", "And in the end… only one will be able to leave.", "Pausa larga antes de «solo uno»."),
        ("los_demas", "Los demás… se quedan conmigo para siempre.", "The rest… stay with me forever.", "Bajando la voz en «para siempre»."),
        ("risa", "Je, je, je… ¡Ja, ja, ja!", "Heh, heh, heh… Ha, ha, ha!", "Empieza contenida y se le escapa al final."),
        ("candado", "Primero necesitan salir de su celda. El candado tiene tres números… y los tres números están escondidos.",
         "First you need to get out of your cell. The lock has three numbers… and all three numbers are hidden.", "Ya compuesto. Explica."),
        ("escondidos", "Si no puedes pasar de aquí… je… te deseo suerte.", "If you can't get past this… heh… good luck.",
         "Burlón; el «je» es una risita."),
        ("final_risa", "¡Ja, ja, ja!", "Ha, ha, ha!", "Carcajada para cerrar."),
    ],
    "salida": [
        ("bravo", "Bravo… bravo.", "Bravo… bravo.", "Aplaudiendo despacio (si quieres, aplaude de verdad)."),
        ("todas", "Todos fuera de su celda. Ninguno se quedó atrás… qué lástima.",
         "All of you out of your cells. Nobody stayed behind… what a pity.", "Fingiendo que le da pena."),
        ("museo", "Esto que ven es la entrada de mi museo. Caminen con cuidado: el piso es viejo… como yo.",
         "What you see is the entrance to my museum. Walk carefully: the floor is old… like me.", "Guía de museo, con una risita al final."),
        ("salas", "Detrás de cada puerta hay una sala. Y en cada sala, una prueba.",
         "Behind every door there is a hall. And in every hall, a trial.", "Misterioso."),
        ("risa", "Je, je, je…", "Heh, heh, heh…", "Corta, hacia adentro."),
        ("porton", "Sigan la alfombra roja. Del otro lado del portón… los espero.",
         "Follow the red carpet. On the other side of the gate… I'll be waiting.", "Anfitrión amable; se oye que sonríe."),
    ],
    "equipos": [
        ("bienvenidos", "Bienvenidos… a mi gran vestíbulo.", "Welcome… to my grand lobby.", "Orgulloso, abriendo los brazos."),
        ("formen", "Ahora formen equipos. De dos… o de tres.", "Now form teams. Of two… or of three.", "Claro, marcando «dos» y «tres»."),
        ("ascensor", "Cada equipo, a un ascensor. Ni uno más… ni uno menos.", "Each team, into a lift. Not one more… not one less.",
         "Firme, como regla de la casa."),
        ("solos", "Los que se queden solos… se quedan conmigo.", "Those left alone… stay with me.", "Bajito, amenazante y amable a la vez."),
        ("risa", "Je, je…", "Heh, heh…", "Corta, hacia adentro."),
    ],
    "sentidos": [
        ("taller", "Bienvenidos a mi taller.", "Welcome to my workshop.", "Íntimo, como quien enseña su cuarto favorito."),
        ("mesa", "En la mesa hay tres regalos: una venda, una cinta… y unas esposas.",
         "On the table there are three gifts: a blindfold, a tape… and handcuffs.", "Presentándolos uno por uno."),
        ("venda", "Quien tome la venda no verá nada.", "Whoever takes the blindfold will see nothing.", "Despacio."),
        ("cinta", "Quien tome la cinta no podrá hablar.", "Whoever takes the tape will not be able to speak.", "Despacio."),
        ("esposas", "Y quien tome las esposas… no podrá tocar nada.", "And whoever takes the handcuffs… will not be able to touch anything.",
         "Con una pausa antes de «no podrá»."),
        ("parejas", "Si son dos, no hay cinta: uno no ve… y el otro no toca.", "If you are two, there is no tape: one can't see… and the other can't touch.",
         "Explicando, paciente."),
        ("cadena", "Y van a estar encadenados. Juntos hasta la salida.", "And you will be chained. Together until the exit.", "Con gusto."),
        ("ultimo", "El último equipo en salir… se queda en mi vitrina.", "The last team out… stays in my display case.", "Bajando la voz."),
        ("abierta", "La vitrina está abierta.", "The display case is open.", "Su cierre de siempre."),
    ],
    "vitrina": [
        ("ultimos", "Ah… el último equipo.", "Ah… the last team.", "Contento, como quien recibe un paquete."),
        ("quietos", "No se muevan. Las correas son viejas… pero aguantan.", "Don't move. The straps are old… but they hold.", "Tranquilo."),
        ("piezas", "Siempre quise unas piezas así: con miedo en la cara.", "I always wanted pieces like this: with fear on their faces.",
         "Admirándolos."),
        ("cera", "La cera está caliente. Va a doler… un poquito.", "The wax is hot. It will hurt… a little.", "Muy suave, casi tierno."),
        ("risa", "Je, je, je… ¡Ja, ja, ja!", "Heh, heh, heh… Ha, ha, ha!", "Se le escapa la carcajada."),
    ],
    "vitrina_fin": [
        ("hermosas", "Hermosas.", "Beautiful.", "Un suspiro, enamorado."),
        ("siguen", "Los demás… acompáñenme. La función continúa.", "The rest of you… follow me. The show goes on.", "Anfitrión, invitando a seguir."),
        ("abierta", "La vitrina está abierta.", "The display case is open.", "Su cierre de siempre."),
    ],
    "antesala": [
        ("polvo", "Las piezas lentas… solo juntan polvo.", "Slow pieces… only gather dust.", "Despectivo, sin levantar la voz."),
        ("servian", "Esas no me servían. Ustedes… todavía sí.", "Those were no use to me. You… still are.", "Pausa antes de «todavía sí»."),
        ("casa", "Sigan el pasillo. Mi casa los espera.", "Follow the hallway. My house awaits.", "Anfitrión; «mi casa» con cariño."),
        ("abierta", "La vitrina está abierta.", "The display case is open.", "Su cierre de siempre."),
    ],
    "mansion": [
        ("bienvenidos", "Bienvenidos a mi casa.", "Welcome to my home.", "Anfitrión orgulloso, cálido."),
        ("no_toquen", "Pasen, pasen… no toquen nada.", "Come in, come in… don't touch anything.", "Amable, con advertencia en «nada»."),
        ("presentar", "Quiero presentarles a alguien… ¿Cascabel?", "I'd like you to meet someone… Cascabel?",
         "Mirando hacia la jaula; llama con cariño."),
        ("se_escapo", "Uy… creo que se escapó mi mascota.", "Oops… I think my pet got out.", "Fingiendo sorpresa; «uy» juguetón."),
        ("ahi_esta", "Ah, no. Ahí está.", "Ah, no. There she is.", "Aliviado y divertido (se oye un rugido y los barrotes)."),
        ("no_ha_comido", "No ha comido en días. Se pone un poco… nerviosa.", "She hasn't eaten in days. She gets a little… restless.",
         "Como quien habla de un gato consentido."),
        ("minuto", "Les doy un minuto para esconderse. Después… abro la jaula.",
         "I'll give you one minute to hide. Then… I open the cage.", "Pausa larga antes de «abro la jaula»."),
        ("cascabel", "Si oyen un cascabel… no respiren.", "If you hear a bell… don't breathe.", "Susurrado, casi un secreto."),
        ("abierta", "La vitrina está abierta.", "The display case is open.", "Su cierre de siempre."),
    ],
    "mansion_suelta": [
        ("se_acabo", "Se acabó el minuto.", "Time's up.", "Seco, como quien cierra un reloj de bolsillo."),
        ("a_jugar", "¡Cascabel… a jugar!", "Cascabel… playtime!", "La única vez que sube la voz: alegre, de dueño orgulloso."),
        ("risa", "Je, je, je…", "Heh, heh, heh…", "Risa corta y contenta."),
    ],
    "mansion_fin": [
        ("a_tu_jaula", "¡Cascabel! A tu jaula.", "Cascabel! Back in your cage.", "Firme, como a una mascota; sin enojo."),
        ("buena", "Buena chica.", "Good girl.", "Tierno."),
        ("enteros", "Los que siguen enteros… síganme.", "Those of you still in one piece… follow me.", "Cortés, con humor negro."),
        ("abierta", "La vitrina está abierta.", "The display case is open.", "Su cierre de siempre."),
    ],
    "caja": [
        ('bienvenidos', "Bienvenidos a mi caja de música.", "Welcome to my music box.", "Orgulloso, como quien enseña su pieza favorita."),
        ('gondolas', "¿Ven mis góndolas? Busquen una y siéntense. De dos en dos.", "See my gondolas? Find one and sit down. Two by two.", "Amable, como anfitrión de feria."),
        ('espalda', "Cuando estén todos sentados, cada uno llevará una figura en la espalda: un círculo, un cuadrado, un triángulo o un rombo.", "Once you are all seated, each of you will wear a shape on your back: a circle, a square, a triangle or a diamond.", "Claro, como contando con los dedos."),
        ('no_se_ve', "El suyo no lo pueden ver. El de atrás… sí.", "You can't see your own. The one behind you… can.", "Pausa antes de «sí»."),
        ('diganse', "En la góndola, el de atrás le dice al de adelante qué símbolo lleva. Luego cambian de lugar.", "In the gondola, the one in back tells the one in front which symbol they wear. Then you switch seats.", "Despacio, que se entienda bien."),
        ('gira', "Cada vez que se apague la luz… les toca otro lugar. Y otra pareja.", "Every time the lights go out… you get another seat. And another partner.", "Misterioso en «otro lugar»."),
        ('voces', "Ah, y no se asusten de sus voces. Ahí adentro nadie suena como es.", "Oh, and don't be scared of your voices. In there, nobody sounds like themselves.", "Divertido, en confianza."),
        ('traidores', "Pero cuidado: entre ustedes hay dos traidores. Y ellos pueden mentir.", "But careful: there are two traitors among you. And they may lie.", "Bajando la voz."),
        ('al_final', "Al final, cada uno me dirá su símbolo. En voz alta.", "In the end, each of you will tell me your symbol. Out loud.", "Cortés."),
        ('si_fallan', "Si se equivocan… se quedan conmigo.", "If you're wrong… you stay with me.", "Suave, casi tierno."),
        ('traidor_bala', "Y el traidor que no engañe a nadie… también.", "And the traitor who fools no one… stays too.", "Con humor negro."),
        ('sientense', "Ahora… siéntense.", "Now… sit down.", "Lento: una orden suave."),
    ],
    "caja_revision": [
        ('circulo', "Qué bonitos se ven. Todos en círculo.", "How lovely you look. All in a circle.", "Encantado, como ante una vitrina."),
        ('juguete', "¿Oyen eso? Es mi juguete favorito.", "Do you hear that? It's my favorite toy.", "Encantado; casi susurrando «favorito»."),
        ('uno_por_uno', "Uno por uno. Cuando los mire… díganme su símbolo.", "One by one. When I look at you… tell me your symbol.", "Pausa larga antes de «díganme»."),
        ('fuerte', "En voz alta. Y márquenlo.", "Out loud. And mark it.", "Firme, sin subir la voz."),
    ],
    "caja_traidores": [
        ("ahora", "Y ahora… mis traidores.", "And now… my traitors.", "Susurro, con gusto."),
    ],
    "caja_fin": [
        ("se_acabo", "¿Oyen? Se acabó la música.", "Hear that? The music has stopped.", "Melancólico."),
        ("eran", "Mis traidores eran…", "My traitors were…", "Pausa larga al final: los nombres salen en pantalla."),
        ("enteros", "Los que siguen enteros… gracias por el paseo.", "Those still in one piece… thank you for the ride.",
         "Cortés, con humor negro."),
        ("abierta", "La vitrina está abierta.", "The display case is open.", "Su cierre de siempre."),
    ],
    "sorpresa": [
        ('bienvenidos', "Bienvenidos a mi teatrito.", "Welcome to my little theater.", "Orgulloso, como presentador de teatro viejo."),
        ('amigo', "Les presento a mi amigo más viejo: Don Resorte. Vive en esa caja.", "Meet my oldest friend: Mr. Spring. He lives in that box.", "Cariñoso, como si hablara de su mascota."),
        ('cuerda', "Le gusta que le den cuerda. Uno por uno van a subir y darle una, dos o tres vueltas a la manivela.", "He likes to be wound up. One by one, you'll go up and give the crank one, two or three turns.", "Juguetón, explicando un juego de niños."),
        ('una_tres', "Pero si alguien le da solo una… el que sigue tendrá que darle tres.", "But if someone gives only one… the next one will have to give three.", "Pausa en «una…»; luego firme."),
        ('hambre', "Don Resorte siempre sale con hambre. Y nadie sabe en qué vuelta.", "Mr. Spring always comes out hungry. And nobody knows on which turn.", "Bajando la voz; risita al final.", "WITCH_CELEBRATE"),
        ('zapatos', "No se preocupen… siempre deja los zapatos.", "Don't worry… he always leaves the shoes.", "Tranquilizador, con humor negro."),
        ('empecemos', "Empecemos.", "Let's begin.", "Corto, con gusto."),
    ],
    "sorpresa_fin": [
        ('lleno', "Don Resorte ya está lleno. Por hoy.", "Mr. Spring is full. For today.", "Satisfecho, como después de una gran cena."),
        ('zapatos', "Qué bonita colección de zapatos.", "What a lovely shoe collection.", "Admirando, como ante una vitrina."),
        ('enteros', "Los que siguen enteros… síganme.", "Those still in one piece… follow me.", "Cortés, con humor negro."),
    ],
    "montacargas": [
        ("viaje", "Respiren hondo… es solo un poco de gas.", "Breathe deep… it's just a little gas.",
         "Susurrado, como quien arrulla a alguien."),
        ("cloacas", "Próxima parada: las cloacas. Ahí abajo nada se seca nunca.",
         "Next stop: the sewers. Down there, nothing ever dries.", "Voz de operador de elevador antiguo."),
        ("archivo", "Próxima parada: mi archivo. Ahí guardo los secretos de todas mis piezas.",
         "Next stop: my archive. That's where I keep the secrets of all my pieces.", "Voz de operador; orgulloso en «mis piezas»."),
        ("salon", "Próxima parada: el salón de juego. Vayan pensando en quién confían.",
         "Next stop: the game parlor. Start thinking about who you trust.", "Voz de operador, con malicia."),
        ("laberintos", "Próxima parada: los laberintos. Vayan cerrando los ojos.",
         "Next stop: the mazes. You may start closing your eyes.", "Voz de operador, suave."),
        ("capilla", "Próxima parada: la capilla. Un buen lugar para rezar… o para ofrecerse.",
         "Next stop: the chapel. A fine place to pray… or to offer yourself.", "Voz de operador, solemne."),
        ("puertas", "Próxima parada: la sala de las puertas. Tres caminos… y uno no tiene piso.",
         "Next stop: the hall of doors. Three ways… and one has no floor.", "Voz de operador, juguetón."),
        ("final", "Última parada: mi cámara privada. Muy pocas piezas llegan hasta aquí.",
         "Last stop: my private chamber. Very few pieces make it this far.", "Lento, importante: es el final."),
        ("galeria", "Parada especial: la galería de los postes. Vayan en fila… y no se enreden.",
         "Special stop: the gallery of posts. Walk in line… and don't get tangled.", "Voz de operador, divertido."),
    ],
    "encadenados": [
        ("hola", "Buenas noches. Bienvenidos a las cloacas.", "Good evening, pieces. Welcome to the sewers.",
         "Elegante, como si el lugar fuera un salón de baile."),
        ("cadena", "Cada uno está encadenado a la pared… y a alguien más.", "Each of you is chained to the wall… and to another piece.",
         "Despacio; pausa en los puntos."),
        ("pareja", "Su pareja de esta noche. No la eligieron ustedes. La elegí yo.",
         "Your partner for tonight. You didn't choose her. I did.", "Orgulloso de su elección."),
        ("llaves", "Su llave no la tienen ustedes. Está en el barril de su pareja.",
         "You don't have your key. It's in your partner's barrel.", "Claro, explicando."),
        ("lanzar", "Pídanla con cortesía. Y cuando la tengan… láncenla con buena puntería.",
         "Ask for it politely. And when you have it… throw it with good aim.", "Burlón en «buena puntería»."),
        ("gas", "Cuando se abra la reja, el gas empezará a bajar por los túneles. Yo no lo respiraría mucho tiempo.",
         "When the gate opens, the gas will start coming down the tunnels. I wouldn't breathe it for long.",
         "Casual, como quien habla del clima."),
        ("refugio", "Al final hay un refugio. Es pequeño. No caben todos.", "At the end there is a shelter. It's small. Not all of you fit.",
         "Una pausa entre cada frase."),
        ("risa", "Je, je, je…", "Heh, heh, heh…", "Corta, hacia adentro."),
        ("vitrina", "Lo que los ata… también puede salvarlos. La vitrina está abierta.",
         "What binds you… can also save you. The display case is open.", "Su cierre de siempre, firme."),
    ],
    "traidor": [
        ("hola", "Bienvenidos a mi archivo. Aquí guardo todo lo que sé de ustedes… que es mucho.",
         "Welcome to my archive. Here I keep everything I know about you… which is a lot.", "Coqueto, sabiendo más de lo que dice."),
        ("boveda", "Cada sala tiene una bóveda cerrada con un código.", "Each room has a vault locked with a code.", "Claro, explicando."),
        ("notas", "Cada uno tiene una nota con dos números. Juntos, tienen el código completo.",
         "Each of you has a note with two numbers. Together, you have the whole code.", "Claro, explicando."),
        ("traidor", "Pero en cada sala hay un traidor. Una réplica barata entre piezas originales.",
         "But in every room there is a traitor. A cheap replica among original pieces.", "Con desprecio elegante en «réplica barata»."),
        ("miente", "Su nota miente… y el traidor lo sabe.", "Their note lies… and the traitor knows it.", "Susurrado, cómplice."),
        ("intentos", "Tienen tres intentos. Si abren la bóveda, el traidor es mío.",
         "You have three tries. If you open the vault, the traitor is mine.", "Firme."),
        ("si_no", "Si no la abren… el traidor elige a quién me llevo.", "If you don't… the traitor chooses who I take.",
         "Saboreándolo."),
        ("risa", "Je, je, je…", "Heh, heh, heh…", "Corta, hacia adentro."),
        ("vitrina", "Díganme… ¿en quién confían? La vitrina está abierta.", "Tell me… who do you trust? The display case is open.",
         "La pregunta despacio; el cierre firme."),
    ],
    "compartir": [
        ("hola", "Bienvenidos a mi salón de juego. Tomen asiento… frente a frente.",
         "Welcome to my game parlor. Take a seat… face to face.", "Anfitrión de casino elegante."),
        ("mesa", "La pieza que tienen enfrente es su pareja en esta mesa.", "The piece in front of you is your partner at this table.",
         "Claro."),
        ("hablen", "Tienen un minuto para hablar. Prometan lo que quieran: las promesas no cuestan nada.",
         "You have one minute to talk. Promise whatever you like: promises cost nothing.", "Burlón en «no cuestan nada»."),
        ("elijan", "Después, cada uno elegirá en secreto: compartir… o robar.",
         "Then each of you will choose in secret: share… or steal.", "Pausa larga antes de «o robar»."),
        ("comparten", "Si los dos comparten, siguen los dos.", "If you both share, you both go on.", "Contento."),
        ("roba", "Si uno roba, solo sigue el que robó.", "If one steals, only the thief goes on.", "Neutral, como juez."),
        ("roban", "Y si los dos roban… los dos se quedan conmigo.", "And if you both steal… you both stay with me.",
         "Encantado con la idea."),
        ("risa", "Je, je, je…", "Heh, heh, heh…", "Corta, hacia adentro."),
        ("vitrina", "La confianza es una pieza muy fina: se rompe con nada. La vitrina está abierta.",
         "Trust is a very fine piece: it breaks at nothing. The display case is open.", "Reflexivo; el cierre firme."),
    ],
    "ciegas": [
        ("hola", "Bienvenidos a mis laberintos. Paredes acolchadas… para que nadie se rompa.",
         "Welcome to my mazes. Padded walls… so nobody gets chipped.", "Cariñoso, como si los cuidara."),
        ("venda", "Casi todos caminarán con los ojos vendados. Solo verán un destello… de vez en cuando.",
         "Almost all of you will walk blindfolded. You'll only see a flash… every now and then.", "Misterioso."),
        ("guia", "En cada equipo, uno será el guía. Caminará arriba, sobre el vidrio, y lo verá todo.",
         "In each team, one piece will be the guide. She'll walk above, on the glass, and see everything.", "Claro, explicando."),
        ("voz", "Pero no podrá bajar. Solo tendrá su voz.", "But she can't come down. All she has is her voice.", "Lento."),
        ("trampas", "En el piso hay trampas. Quien pise una, regresa a la entrada.",
         "There are traps on the floor. Whoever steps on one goes back to the entrance.", "Claro."),
        ("mueven", "Y como me aburro fácil… de vez en cuando las cambio de lugar.",
         "And since I get bored easily… every now and then I move them.", "Caprichoso, como niño."),
        ("baldosas", "En el camino también hay baldosas que se encienden. Crúcenlas cuando estén apagadas.",
         "There are also tiles on the path that light up. Cross them when they're dark.", "Claro, explicando."),
        ("ultimo", "El último equipo en salir pierde a su guía. Por guiar mal.",
         "The last team out loses its guide. For guiding badly.", "Seco en «por guiar mal»."),
        ("risa", "Je, je, je…", "Heh, heh, heh…", "Corta, hacia adentro."),
        ("vitrina", "Escuchen bien. La vitrina está abierta.", "Listen carefully. The display case is open.", "Su cierre de siempre."),
    ],
    "sacrificio": [
        ("hola", "Bienvenidos a mi capilla. Aquí se viene a pedir… y a dar.",
         "Welcome to my chapel. Here you come to ask… and to give.", "Solemne, como sacerdote."),
        ("reja", "Cada cuarto tiene una reja. Solo se abre si alguien se queda adentro.",
         "Each room has a gate. It only opens if someone stays inside.", "Claro."),
        ("altar", "Quien se pare cinco segundos en el altar, se ofrece. Los demás salen libres.",
         "Whoever stands on the altar for five seconds offers herself. The others walk free.", "Despacio en «se ofrece»."),
        ("votar", "Si nadie se ofrece, votarán en secreto. La pieza más votada se queda.",
         "If nobody offers, you'll vote in secret. The piece with the most votes stays.", "Neutral, como juez."),
        ("risa", "Je, je, je…", "Heh, heh, heh…", "Corta, hacia adentro."),
        ("vitrina", "Las piezas valientes son las más bonitas de mi colección. La vitrina está abierta.",
         "Brave pieces are the most beautiful in my collection. The display case is open.", "Con admiración sincera."),
    ],
    "puertas": [
        ("hola", "Bienvenidos a la sala de las puertas.", "Welcome to the hall of doors.", "Presentador de concurso."),
        ("tres", "Tres puertas. Detrás de cada una, una cabina.", "Three doors. Behind each one, a booth.", "Presentador de concurso."),
        ("piso", "En una de ellas el piso se abre… y el agua de abajo es muy, muy honda.",
         "In one of them the floor opens… and the water below is very, very deep.", "Bajando la voz en «muy, muy honda»."),
        ("pista", "Antes de cada ronda les daré una pista. Casi siempre digo la verdad. Casi.",
         "Before each round I'll give you a clue. I almost always tell the truth. Almost.", "Pausa larga antes del último «casi»."),
        ("juntas", "Si todos eligen la misma puerta, esa vez el piso aguanta. Pónganse de acuerdo… si pueden.",
         "If you all choose the same door, that time the floor holds. Agree with each other… if you can.", "Retador."),
        ("tiempo", "Elijan rápido: el que no entre antes de que bajen las rejas, deja que yo elija por él.",
         "Choose quickly: whoever isn't in before the gates come down lets me choose for her.", "Un poco más rápido, con urgencia."),
        ("risa", "Je, je, je…", "Heh, heh, heh…", "Corta, hacia adentro."),
        ("vitrina", "Que la suerte los acompañe. La vitrina está abierta.", "May luck be with you. The display case is open.",
         "Su cierre de siempre."),
    ],
    "copas": [
        ("hola", "Bienvenidos a mi cámara privada. Muy pocas piezas llegan hasta aquí.",
         "Welcome to my private chamber. Very few pieces make it this far.", "Íntimo, casi conmovido."),
        ("llegaron", "Ustedes son lo mejor de mi colección. Los más fuertes. Los más valientes.",
         "You are the finest of my collection. The strongest. The most beautiful.", "Orgulloso, como padre."),
        ("copas", "Sobre la mesa hay copas. Todas huelen igual. Todas brillan igual.",
         "There are glasses on the table. They all smell the same. They all shine the same.", "Despacio, saboreando."),
        ("veneno", "Solo una tiene veneno.", "Only one is poisoned.", "Seco. Pausa después."),
        ("turnos", "Por turnos, cada uno beberá una copa. La última pieza en pie… sale libre.",
         "Taking turns, each of you will drink a glass. The last piece standing… walks free.", "Claro; pausa antes de «sale libre»."),
        ("promesa", "Y yo siempre cumplo mis promesas.", "And I always keep my promises.", "Sincero. O casi."),
        ("risa", "Je, je, je…", "Heh, heh, heh…", "Corta, hacia adentro."),
        ("salud", "¡Salud!", "Cheers!", "Brindando, con alegría de fiesta."),
    ],
    "tres": [
        ("hola", "Bienvenidos a mi galería de los postes.", "Welcome to my gallery of posts.", "Guía de museo."),
        ("objetos", "En su mesa hay tres regalos: una venda, una cinta y unas esposas.",
         "On your table there are three gifts: a blindfold, a tape and handcuffs.", "Como quien entrega regalos de cumpleaños."),
        ("venda", "Quien se ponga la venda no verá nada. Solo un destello, de vez en cuando.",
         "Whoever wears the blindfold won't see a thing. Only a flash, now and then.", "Claro."),
        ("cinta", "Quien se ponga la cinta no podrá decir ni una palabra.", "Whoever wears the tape won't say a single word.", "Claro."),
        ("esposas", "Y quien se ponga las esposas… no podrá tocar nada.", "And whoever wears the handcuffs… won't touch a thing.",
         "Pausa antes de «no podrá tocar nada»."),
        ("decidan", "Decidan entre ustedes quién se pone qué. Si no deciden, decido yo.",
         "Decide among yourselves who wears what. If you don't, I will.", "Amable, pero sin paciencia."),
        ("cadena", "Luego cruzarán la galería encadenados en fila. Cuidado con los postes: las cadenas se enredan.",
         "Then you'll cross the gallery chained in a line. Mind the posts: chains get tangled.", "Claro, explicando."),
        ("risa", "Je, je, je…", "Heh, heh, heh…", "Corta, hacia adentro."),
        ("vitrina", "El último trío en salir se queda conmigo. La vitrina está abierta.",
         "The last trio out stays with me. The display case is open.", "Su cierre de siempre."),
    ],
}

TITULOS = {
    "bienvenida": "El despertar: el discurso en la pantalla de cada celda (después del apagón)",
    "salida": "(Ya no se usa: cuando salen de las celdas no habla; en el patio ya no hay pantallas grandes)",
    "montacargas": "El montacargas (entre pruebas: el viaje y una parada por zona)",
    "encadenados": "Prueba 1 · Encadenados",
    "traidor": "Prueba 2 · El traidor",
    "compartir": "Prueba 3 · Compartir o robar",
    "ciegas": "Prueba 4 · A ciegas",
    "sacrificio": "Prueba 5 · El sacrificio",
    "puertas": "Prueba 6 · Las puertas",
    "copas": "Prueba 7 (final) · Las copas",
    "tres": "Prueba extra · Encadenados de tres",
    "equipos": "Capítulo 1 · La sala grande (formen equipos)",
    "sentidos": "Capítulo 1 · El taller de los sentidos (la mesa)",
    "vitrina": "Capítulo 1 · La vitrina (el último equipo)",
    "vitrina_fin": "Capítulo 1 · Después de la cera",
    "antesala": "Capítulo 1 · La antesala (después del teatro: la puerta se cierra detrás de todos)",
    "mansion": "Capítulo 2 · La mascota: en el vestíbulo de la casa (cuando ya entraron todos)",
    "mansion_suelta": "Capítulo 2 · La mascota: se acabó el minuto (se abre la jaula)",
    "mansion_fin": "Capítulo 2 · La mascota: se acabó el tiempo (Cascabel vuelve a su jaula)",
    "caja": "Capítulo 3 · La caja de música: en el salón de las góndolas (cuando ya entraron todos)",
    "caja_revision": "Capítulo 3 · La caja de música: todos sentados en círculo (antes de que baje el arma)",
    "caja_traidores": "Capítulo 3 · La caja de música: después de revisar a todos (le toca a los traidores)",
    "caja_fin": "Capítulo 3 · La caja de música: el final (sube el arma y se dicen los traidores)",
    "sorpresa": "Capítulo 4 · La caja sorpresa: en el teatrito (cuando ya entraron todos)",
    "sorpresa_fin": "Capítulo 4 · La caja sorpresa: el final (Don Resorte ya está lleno)",
}
ZONAS = ["cloacas", "archivo", "salon", "laberintos", "capilla", "puertas", "final", "galeria"]


def ticks(clave, texto):
    """Cuánto está la frase en pantalla (ticks): lo que tarda en leerse con calma; las risas y el silencio, fijos."""
    if clave == "silencio":
        return 50
    if clave.endswith("risa"):
        return 70 if "¡" in texto else 60
    seg = len(texto) / 13 + 1.3 + 0.35 * texto.count("…")
    return max(50, min(240, int(round(seg * 20 / 5.0)) * 5))


def con_audio(gid, clave, t):
    """Si ya está grabada, la frase dura al menos lo que dura su audio (+0.6 s)."""
    ogg = os.path.join(MOD, "src/main/resources/assets/coleccionista/sounds/voz", f"{gid}_{clave}.ogg")
    if not os.path.exists(ogg):
        return t
    import subprocess
    r = subprocess.run(["ffprobe", "-v", "error", "-show_entries", "format=duration", "-of", "csv=p=0", ogg], capture_output=True, text=True)
    seg = float(r.stdout.strip() or 0)
    return max(t, int((seg + 0.6) * 20 + 0.999))


def escribir():
    # 1) los textos en los tres idiomas: fuera las frases viejas, adentro las nuevas (donde estaban)
    for archivo, idx in (("es_mx.json", 1), ("es_es.json", 1), ("en_us.json", 2)):
        p = os.path.join(LANG, archivo)
        d = json.load(open(p, encoding="utf-8"))
        nuevo, puesto = {}, False
        for k, v in d.items():
            partes = k.split(".")
            if len(partes) == 4 and partes[1] == "voz" and partes[2] in GUIONES:
                if not puesto:
                    for gid, frases in GUIONES.items():
                        for f in frases:
                            nuevo[f"coleccionista.voz.{gid}.{f[0]}"] = f[idx]
                    puesto = True
                continue
            nuevo[k] = v
        json.dump(nuevo, open(p, "w", encoding="utf-8"), ensure_ascii=False, indent=2)
        open(p, "a", encoding="utf-8").write("\n")
    # 2) los guiones en Pantallas.java (cada método: sus frases y su duración)
    src = open(PANTALLAS, encoding="utf-8").read()
    for gid, frases in GUIONES.items():
        m = re.search(r"(public static List<Frase> " + gid + r"\(\) \{\n)(.*?)(\n        return l;)", src, re.S)
        if not m:
            raise SystemExit("no encontré el guion " + gid)
        cuerpo = ["        List<Frase> l = new ArrayList<>();"]
        if gid == "montacargas":
            f0 = frases[0]
            cuerpo.append(f'        l.add(new Frase("{f0[0]}", {ticks(f0[0], f0[1])}, null, 1F));')
            cuerpo.append("        // la parada: una frase por zona")
            cuerpo.append("        switch (parada) {")
            for f in frases[1:]:
                cuerpo.append(f'            case "{f[0]}" -> l.add(new Frase("{f[0]}", {ticks(f[0], f[1])}, null, 1F));')
            cuerpo.append("            default -> {")
            cuerpo.append("            }")
            cuerpo.append("        }")
        else:
            for f in frases:
                t = con_audio(gid, f[0], ticks(f[0], f[1]))
                if len(f) > 4:
                    cuerpo.append(f'        l.add(new Frase("{f[0]}", {t}, SoundEvents.{f[4]}, 0.5F));')
                elif f[0].endswith("risa") and not os.path.exists(os.path.join(MOD, "src/main/resources/assets/coleccionista/sounds/voz",
                                                                                 f"{gid}_{f[0]}.ogg")):
                    cuerpo.append(f'        l.add(new Frase("{f[0]}", {t}, SoundEvents.WITCH_CELEBRATE, 0.55F));')
                else:
                    cuerpo.append(f'        l.add(new Frase("{f[0]}", {t}, null, 1F));')
        src = src[:m.start(2)] + "\n".join(cuerpo) + src[m.end(2):]
    open(PANTALLAS, "w", encoding="utf-8").write(src)
    total = sum(1 for fs in GUIONES.values() for _ in fs)
    print(f"{total} frases escritas")
    for gid, frases in GUIONES.items():
        seg = sum(ticks(f[0], f[1]) for f in (frases[:1] + [max(frases[1:], key=lambda x: ticks(x[0], x[1]))] if gid == "montacargas"
                                              else frases)) / 20
        print(f"  {gid}: {len(frases)} frases, {seg:.0f} s")


if __name__ == "__main__":
    escribir()
