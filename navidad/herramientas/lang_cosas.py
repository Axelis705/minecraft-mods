#!/usr/bin/env python3
"""Nombres y textos de los objetos, mensajes y subtítulos de Navidad de Axelis (es_mx, es_es, en_us). Se puede correr varias veces.
Cada entrada: clave -> (es_mx, en_us[, es_es si cambia])."""
import json, os

AQUI = os.path.dirname(os.path.abspath(__file__))
L = os.path.join(AQUI, '..', 'src', 'main', 'resources', 'assets', 'navidad', 'lang')

T = {
    'itemGroup.navidad.cosas': ('Navidad: comida, regalos y ropa', 'Christmas: food, gifts and clothes'),
    'navidad.hola': ('Navidad de Axelis funciona con %s. %s', 'Navidad de Axelis runs on %s. %s'),
    'navidad.temporada.si': ('¡Es temporada navideña!', "It's Christmas season!"),
    'navidad.temporada.no': ('Todavía no es temporada navideña (del 1 de diciembre al 6 de enero).',
                             "It's not Christmas season yet (December 1 to January 6)."),
    'navidad.temporada.modo': ('Temporada navideña: %s', 'Christmas season: %s'),
    # comida
    'item.navidad.galleta_jengibre': ('Galleta de jengibre', 'Gingerbread cookie'),
    'item.navidad.baston_caramelo': ('Bastón de caramelo', 'Candy cane'),
    'item.navidad.dulce': ('Dulce', 'Candy'),
    'item.navidad.tejocote': ('Tejocote', 'Tejocote'),
    'item.navidad.mandarina': ('Mandarina', 'Mandarin', ),
    'item.navidad.cana': ('Caña', 'Sugarcane stick'),
    'item.navidad.tamal': ('Tamal', 'Tamale'),
    'item.navidad.bunuelo': ('Buñuelo', 'Buñuelo'),
    'item.navidad.pavo_navideno': ('Pavo navideño', 'Christmas turkey'),
    'item.navidad.chocolate_caliente': ('Chocolate caliente', 'Hot chocolate'),
    'item.navidad.ponche': ('Ponche', 'Christmas punch'),
    'item.navidad.champurrado': ('Champurrado', 'Champurrado'),
    # regalos y posadas
    'item.navidad.regalo_misterioso': ('Regalo misterioso', 'Mystery gift'),
    'navidad.regalo.ayuda': ('Clic derecho para abrirlo: ¿qué te tocará?', 'Right-click to open it: what will you get?'),
    'navidad.regalo.te_toco': ('🎁 ¡Te tocó %s!', '🎁 You got %s!'),
    'navidad.regalo.dado': ('Regalos misteriosos entregados: %s', 'Mystery gifts handed out: %s'),
    'item.navidad.pinata': ('Piñata', 'Piñata'),
    'navidad.pinata.ayuda': ('Clic derecho en el techo para colgarla (o en el piso). Pégale hasta romperla: ¡mejor con el palo y los ojos vendados!',
                             'Right-click a ceiling to hang it (or the floor). Hit it until it breaks: best with the stick and blindfolded!'),
    'navidad.pinata.casi': ('¡Dale, dale, dale! Ya casi...', 'Hit it, hit it! Almost there...'),
    'navidad.pinata.rota': ('🎉 ¡%s rompió la piñata!', '🎉 %s broke the piñata!'),
    'entity.navidad.pinata': ('Piñata', 'Piñata'),
    'item.navidad.palo_pinata': ('Palo de piñata', 'Piñata stick'),
    'item.navidad.venda': ('Venda', 'Blindfold'),
    'item.navidad.caja_adornos': ('Caja de adornos', 'Box of decorations'),
    'navidad.adornos.ayuda': ('En el cortapiedras se vuelve cualquier adorno navideño.', 'In a stonecutter it turns into any Christmas decoration.'),
    'navidad.venda.puesta': ('Te vendaron los ojos... ¡dale a la piñata!', "You're blindfolded... hit the piñata!"),
    'navidad.venda.ayuda': ('Clic derecho: te tapa los ojos y te marea un poco (como en las posadas).',
                            'Right-click: covers your eyes and makes you a bit dizzy (just like at a posada).'),
    'block.navidad.caja_regalo': ('Caja de regalo', 'Gift box'),
    'navidad.caja.ayuda': ('Ponla y dale clic derecho con lo que quieras regalar. Con una etiqueta con nombre: solo esa persona la abre.',
                           'Place it and right-click it with what you want to give. Use a name tag: only that person can open it.'),
    'navidad.caja.vacia': ('La caja está vacía: dale clic con lo que quieras regalar.', "The box is empty: click it with what you want to give."),
    'navidad.caja.envuelta': ('🎁 ¡Regalo envuelto! Ahora cualquiera lo puede abrir... o ponle "Para" con una etiqueta con nombre.',
                              '🎁 Gift wrapped! Anyone can open it now... or set "To" with a name tag.'),
    'navidad.caja.para': ('🏷 Este regalo ahora es para: %s', '🏷 This gift is now for: %s'),
    'navidad.caja.no_es_tuyo': ('Este regalo es para %s (de parte de %s). ¡No se vale abrirlo!', "This gift is for %s (from %s). No peeking!"),
    'navidad.caja.abierta': ('🎁 Regalo de %s: %s', '🎁 Gift from %s: %s'),
    'navidad.caja.item_envuelta': ('Envuelta', 'Wrapped'),
    'navidad.caja.item_de': ('De: %s', 'From: %s'),
    'navidad.caja.item_para': ('Para: %s', 'To: %s'),
    'block.navidad.calendario_adviento': ('Calendario de Adviento', 'Advent calendar'),
    'navidad.calendario.espera': ('Las puertitas se abren a partir del 1 de diciembre. Faltan %s días.',
                                  'The little doors open from December 1. %s days to go.'),
    'navidad.calendario.ya': ('Ya abriste la puerta del día %s. ¡Vuelve mañana!', 'You already opened door %s. Come back tomorrow!'),
    'navidad.calendario.puerta': ('🚪 Puerta de hoy: %s', "🚪 Today's door: %s"),
    'navidad.calendario.nochebuena': ('🌟 ¡Nochebuena! La puerta grande: %s', '🌟 Christmas Eve! The big door: %s'),
    'navidad.calendario.feliz': ('🎄 ¡Feliz Navidad! Abriste %s de 24 puertas.', '🎄 Merry Christmas! You opened %s of 24 doors.'),
    'navidad.calendario.fingido': ('Día de diciembre fingido: %s (0 = el de verdad)', 'Pretend December day: %s (0 = the real one)'),
    # ropa
    'item.navidad.gorro_santa': ('Gorro de Santa', 'Santa hat'),
    'item.navidad.gorro_duende': ('Gorro de duende', 'Elf hat'),
    'item.navidad.cuernos_reno': ('Cuernos de reno', 'Reindeer antlers'),
    'item.navidad.sueter_reno': ('Suéter de reno', 'Reindeer sweater', 'Jersey de reno'),
    'item.navidad.sueter_pinos': ('Suéter de pinos', 'Pine tree sweater', 'Jersey de pinos'),
    'item.navidad.sueter_copos': ('Suéter de copos', 'Snowflake sweater', 'Jersey de copos'),
    'item.navidad.traje_santa': ('Traje de Santa', 'Santa suit', 'Traje de Papá Noel'),
    'navidad.ropa.ayuda': ('Clic derecho para ponértelo o quitártelo (se ve encima de tu skin).',
                           'Right-click to put it on or take it off (shows over your skin).'),
    'navidad.ropa.puesta': ('Te pusiste: %s', 'You put on: %s'),
    'navidad.ropa.quitada': ('Te quitaste: %s', 'You took off: %s'),
    # discos
    'item.navidad.disco_campanas': ('Disco de música', 'Music disc'),
    'item.navidad.disco_campanas.desc': ('Cascabeles (Jingle Bells) - caja de música', 'Jingle Bells - music box'),
    'item.navidad.disco_navidad': ('Disco de música', 'Music disc'),
    'item.navidad.disco_navidad.desc': ('Te deseamos feliz Navidad - caja de música', 'We Wish You a Merry Christmas - music box'),
    'item.navidad.disco_noche_paz': ('Disco de música', 'Music disc'),
    'item.navidad.disco_noche_paz.desc': ('Noche de paz - caja de música', 'Silent Night - music box'),
    'item.navidad.disco_adornen': ('Disco de música', 'Music disc'),
    'item.navidad.disco_adornen.desc': ('Adornen los salones (Deck the Halls) - caja de música', 'Deck the Halls - music box'),
    # subtítulos
    'navidad.sub.pinata': ('Canción de la piñata', 'Piñata song'),
    'navidad.sub.cascabeles': ('Suenan cascabeles', 'Sleigh bells jingle'),
    'navidad.sub.risa': ('¡Jo, jo, jo!', 'Ho, ho, ho!'),
    'navidad.sub.krampus': ('El Krampus ruge', 'Krampus roars'),
    'navidad.sub.calendario': ('Se abre una puertita', 'A little door opens'),
    'navidad.sub.regalo': ('Se abre un regalo', 'A gift is opened'),
}


def aplicar(tabla):
    for code in ('es_mx', 'es_es', 'en_us'):
        p = os.path.join(L, code + '.json')
        d = json.load(open(p, encoding='utf-8'))
        for k, v in tabla.items():
            if code == 'en_us':
                d[k] = v[1]
            elif code == 'es_es' and len(v) > 2:
                d[k] = v[2]
            else:
                d[k] = v[0]
        json.dump(d, open(p, 'w', encoding='utf-8'), indent=2, ensure_ascii=False)


if __name__ == '__main__':
    aplicar(T)
    print(len(T), 'textos')

T2 = {
    # criaturas
    'entity.navidad.muneco_malvado': ('Muñeco de nieve malvado', 'Evil snowman'),
    'entity.navidad.reno': ('Reno', 'Reindeer'),
    'entity.navidad.hombre_jengibre': ('Hombre de jengibre', 'Gingerbread man'),
    'entity.navidad.krampus': ('Krampus', 'Krampus'),
    'item.navidad.huevo_muneco_malvado': ('Huevo de muñeco de nieve malvado', 'Evil snowman spawn egg'),
    'item.navidad.huevo_reno': ('Huevo de reno', 'Reindeer spawn egg'),
    'item.navidad.huevo_hombre_jengibre': ('Huevo de hombre de jengibre', 'Gingerbread man spawn egg'),
    'item.navidad.huevo_krampus': ('Huevo del Krampus', 'Krampus spawn egg'),
    'item.navidad.campana_krampus': ('Campana del Krampus', 'Krampus bell'),
    'item.navidad.cuerno_krampus': ('Cuerno del Krampus', 'Krampus horn'),
    'navidad.campana.ayuda': ('Tócala de noche... si te atreves. Llama al Krampus.', 'Ring it at night... if you dare. It summons the Krampus.'),
    'navidad.campana.dia': ('La campana suena, pero de día nadie contesta...', 'The bell rings, but nobody answers during the day...'),
    'navidad.krampus.llega': ('⛓ Se escuchan cadenas en la oscuridad... ¡el Krampus viene por los que se portaron mal!',
                              '⛓ Chains rattle in the dark... the Krampus is coming for the naughty ones!'),
    'navidad.krampus.fase2': ('¡El Krampus se enfureció!', 'The Krampus is furious!'),
    'navidad.krampus.fase2_sub': ('Cuidado con su saco...', 'Watch out for his sack...'),
    'navidad.krampus.vencido': ('🎄 ¡Vencieron al Krampus! ¡Salvaron la Navidad!', '🎄 You defeated the Krampus! Christmas is saved!'),
    'navidad.reno.amigo': ('🦌 ¡El reno ya es tu amigo! Clic derecho con la mano vacía para montarlo.',
                           '🦌 The reindeer is your friend now! Right-click with an empty hand to ride it.'),
    'navidad.reno.timido': ('El reno es tímido... dale zanahorias primero.', 'The reindeer is shy... give it some carrots first.'),
    'navidad.jengibre.burla1': ('🍪 "¡Corre, corre, que no me alcanzas!"', '🍪 "Run, run, you can\'t catch me!"'),
    'navidad.jengibre.burla2': ('🍪 "¡Soy de jengibre y soy más rápido que tú!"', '🍪 "I\'m made of ginger and I\'m faster than you!"'),
    'navidad.jengibre.burla3': ('🍪 "¡Ni la Señora Claus me pudo atrapar!"', '🍪 "Not even Mrs. Claus could catch me!"'),
    'navidad.jengibre.burla4': ('🍪 "¡Je, je! ¡Me escapé del horno!"', '🍪 "Hehe! I escaped from the oven!"'),
    # gente del Polo Norte
    'npc.navidad.santa': ('Santa Claus', 'Santa Claus', 'Papá Noel'),
    'npc.navidad.santa.hola1': ('¡Jo, jo, jo! ¿Te has portado bien este año?', 'Ho, ho, ho! Have you been good this year?'),
    'npc.navidad.santa.hola2': ('Los duendes trabajan día y noche. ¿Nos echas una mano?', 'The elves work day and night. Will you lend a hand?'),
    'npc.navidad.santa.hola3': ('Dicen que el Krampus anda suelto... ten cuidado de noche.', 'They say the Krampus is on the loose... be careful at night.'),
    'npc.navidad.senora_claus': ('Señora Claus', 'Mrs. Claus'),
    'npc.navidad.senora_claus.hola1': ('¡Pásale, que acaban de salir las galletas del horno!', 'Come in, the cookies just came out of the oven!'),
    'npc.navidad.senora_claus.hola2': ('Un chocolatito calientito para el frío, ¿no?', 'A nice hot chocolate for the cold, right?'),
    'npc.navidad.senora_claus.hola3': ('Si ves un hombre de jengibre corriendo... se me escapó otra vez.', 'If you see a gingerbread man running... he escaped again.'),
    'npc.navidad.duende_juguetero': ('Chispita, el duende juguetero', 'Sparky the toymaker elf'),
    'npc.navidad.duende_juguetero.hola1': ('¡Aquí se paga con dulces! Los mejores juguetes del Polo Norte.', 'We take candy here! The best toys at the North Pole.'),
    'npc.navidad.duende_juguetero.hola2': ('¿Una piñata? ¡Tengo de siete picos!', 'A piñata? I have seven-pointed ones!'),
    'npc.navidad.duende_juguetero.hola3': ('Tráeme bolas de nieve y te pago en dulces.', 'Bring me snowballs and I\'ll pay you in candy.'),
    'npc.navidad.duende_sastre': ('Botones, el duende sastre', 'Buttons the tailor elf'),
    'npc.navidad.duende_sastre.hola1': ('¡Un suéter feo de Navidad para cada ocasión!', 'An ugly Christmas sweater for every occasion!'),
    'npc.navidad.duende_sastre.hola2': ('El traje de Santa lo hago yo, ¿sabías?', 'I make Santa\'s suit, did you know?'),
    'npc.navidad.duende_sastre.hola3': ('Necesito lana roja y blanca. ¡Mucha!', 'I need red and white wool. Lots of it!'),
    'npc.navidad.duende_renos': ('Copito, el duende de los renos', 'Snowflake the reindeer elf'),
    'npc.navidad.duende_renos.hola1': ('Los renos aman las zanahorias. ¡Dales tres y serán tus amigos!', 'Reindeer love carrots. Give them three and they\'ll be your friends!'),
    'npc.navidad.duende_renos.hola2': ('De noche, en Navidad, los renos casi vuelan...', 'At night, at Christmas, reindeer almost fly...'),
    'npc.navidad.duende_renos.hola3': ('¿Viste a Relámpago? Se fue a pasear otra vez.', 'Have you seen Lightning? He wandered off again.'),
    # misiones
    'mision.navidad.galletas_leche': ('Galletas y leche', 'Cookies and milk'),
    'mision.navidad.galletas_leche.desc': ('Ay, el viaje es largo... ¿me traes galletas de jengibre y un vaso de leche?',
                                           'Oh, the journey is long... could you bring me gingerbread cookies and some milk?'),
    'mision.navidad.munecos': ('Muñecos rebeldes', 'Rebel snowmen'),
    'mision.navidad.munecos.desc': ('Unos muñecos de nieve se volvieron malos y avientan bolas de nieve a todos. ¡Detenlos!',
                                    'Some snowmen turned evil and throw snowballs at everyone. Stop them!'),
    'mision.navidad.jengibre': ('¡Atrápalos!', 'Catch them!'),
    'mision.navidad.jengibre.desc': ('Se escaparon tres hombres de jengibre de la cocina de la Señora Claus. ¡Corren muy rápido!',
                                     'Three gingerbread men escaped from Mrs. Claus\'s kitchen. They\'re really fast!'),
    'mision.navidad.pinata': ('¡Dale, dale!', 'Hit it, hit it!'),
    'mision.navidad.pinata.desc': ('En las posadas no puede faltar la piñata. Rompe una con tus amigos.',
                                   'A posada isn\'t complete without a piñata. Break one with your friends.'),
    'mision.navidad.regalos': ('El espíritu de dar', 'The spirit of giving'),
    'mision.navidad.regalos.desc': ('La Navidad se trata de compartir: envuelve regalos en cajas y que tus amigos los abran.',
                                    'Christmas is about sharing: wrap gifts in boxes and have your friends open them.'),
    'mision.navidad.krampus': ('Noche del Krampus', 'Krampus Night'),
    'mision.navidad.krampus.desc': ('El Krampus quiere arruinar la Navidad. Si lo enfrentas, ve con tus amigos... y bien preparado.',
                                    'The Krampus wants to ruin Christmas. If you face him, bring your friends... and come prepared.'),
    'mision.navidad.carta': ('Cartas para Santa', 'Letters for Santa'),
    'mision.navidad.carta.desc': ('Me llegan muchas cartas y se me acaba el papel. ¿Me traes papel y una pluma?',
                                  'I get so many letters that I run out of paper. Could you bring paper and a feather?'),
    'mision.navidad.ingredientes': ('Ingredientes', 'Ingredients'),
    'mision.navidad.ingredientes.desc': ('Para el chocolate me falta azúcar y cacao. ¿Me ayudas?', 'I need sugar and cocoa for the hot chocolate. Can you help?'),
    'mision.navidad.pavo': ('La cena de Nochebuena', 'Christmas Eve dinner'),
    'mision.navidad.pavo.desc': ('La cena de Nochebuena no se hace sola. Tráeme pollo cocido y te doy pavo.',
                                 'Christmas Eve dinner doesn\'t cook itself. Bring me cooked chicken and I\'ll give you turkey.'),
    'mision.navidad.horno': ('Horno encendido', 'Oven on'),
    'mision.navidad.horno.desc': ('Cada día horneo galletas. Si me traes trigo, te tocan unas.', 'I bake cookies every day. Bring me wheat and you get some.'),
    'mision.navidad.nieve': ('Guerra de nieve', 'Snowball fight'),
    'mision.navidad.nieve.desc': ('¡Los duendes vamos a tener guerra de nieve! Consíguenos bolas de nieve.',
                                  'We elves are having a snowball fight! Get us some snowballs.'),
    'mision.navidad.juguetes': ('Taller de juguetes', 'Toy workshop'),
    'mision.navidad.juguetes.desc': ('Se me acabó la madera y la pintura roja. ¡Sin eso no hay juguetes!', 'I ran out of wood and red paint. No toys without them!'),
    'mision.navidad.lana': ('Lana para Santa', 'Wool for Santa'),
    'mision.navidad.lana.desc': ('El traje de Santa se gasta cada año. Tráeme lana roja y blanca.', 'Santa\'s suit wears out every year. Bring me red and white wool.'),
    'mision.navidad.zanahorias': ('Comida para los renos', 'Reindeer food'),
    'mision.navidad.zanahorias.desc': ('Los renos tienen hambre después de practicar el vuelo.', 'The reindeer are hungry after flight practice.'),
    'mision.navidad.amigo_reno': ('Un amigo con cuernos', 'A friend with antlers'),
    'mision.navidad.amigo_reno.desc': ('Hazte amigo de un reno: dale zanahorias hasta que te quiera.', 'Make friends with a reindeer: give it carrots until it loves you.'),
    'navidad.aldea.lista': ('🎄 ¡La aldea de Santa está lista! (%s bloques en %s ms)', "🎄 Santa's village is ready! (%s blocks in %s ms)"),
    'item.navidad.rosca_reyes': ('Rosca de Reyes', 'Kings\' cake (rosca de reyes)'),
    'navidad.rosca.muneco': ('👶 ¡A %s le salió el muñequito! Le toca invitar los tamales el 2 de febrero.',
                             '👶 %s found the baby figurine! Now they have to buy the tamales on February 2nd.'),
    'navidad.rosca.nada': ('Mmm... esta vez no te salió el muñequito.', 'Mmm... no baby figurine this time.'),
    'item.navidad.bengala': ('Luz de bengala', 'Sparkler'),
    'navidad.bengala.ayuda': ('Clic derecho para encenderla. ¡Chispas en tu mano!', 'Right-click to light it. Sparks in your hand!'),
    'entity.navidad.trineo_santa': ('Trineo de Santa', "Santa's sleigh"),
    'navidad.fiesta.faltan': ('🎄 ¡Faltan %s días para Navidad!', '🎄 %s days until Christmas!'),
    'navidad.fiesta.nochebuena': ('🌟 ¡Hoy es Nochebuena! Esta noche el trineo de Santa pasa varias veces...',
                                  "🌟 It's Christmas Eve! Tonight Santa's sleigh will fly by several times..."),
    'navidad.fiesta.navidad': ('🎄 ¡Feliz Navidad! Santa te dejó un regalo en tu inventario.', '🎄 Merry Christmas! Santa left a gift in your inventory.'),
    'navidad.fiesta.reyes': ('👑 ¡Feliz Día de Reyes! Te tocó una rosca... ¿te saldrá el muñequito?',
                             "👑 Happy Three Kings' Day! You got a rosca... will you find the baby figurine?"),
    'navidad.fiesta.trineo': ('🛷 ¡Se escuchan cascabeles! ¡Mira al cielo: es el trineo de Santa!',
                              "🛷 Sleigh bells! Look at the sky: it's Santa's sleigh!"),
    'navidad.fiesta.posada': ('🪅 ¡%s armó una posada! A romper la piñata: dale, dale, dale...', '🪅 %s started a posada! Time to break the piñata!'),
    'navidad.fiesta.intercambio': ('🎁 Intercambio de regalos: a ti te toca regalarle a %s. ¡Es secreto, no le digas!',
                                   "🎁 Gift exchange: you're giving a gift to %s. It's a secret, don't tell!"),
    'navidad.fiesta.intercambio_pocos': ('Para el intercambio hacen falta al menos dos personas conectadas.',
                                         'The gift exchange needs at least two players online.'),
    'navidad.fiesta.intercambio_listo': ('Intercambio sorteado entre %s personas (a cada quien le llegó un mensaje secreto).',
                                         'Gift exchange drawn among %s players (everyone got a secret message).'),
    'axelis_core.obj.evento.navidad.pinata': ('Romper una piñata', 'Break a piñata'),
    'axelis_core.obj.evento.navidad.regalo_entregado': ('Regalos que abrieron tus amigos', 'Gifts opened by your friends'),
    'axelis_core.obj.evento.navidad.krampus': ('Vencer al Krampus', 'Defeat the Krampus'),
    'axelis_core.obj.evento.navidad.reno_amigo': ('Hacerte amigo de un reno', 'Befriend a reindeer'),
}

if __name__ == '__main__':
    aplicar(T2)
    print(len(T2), 'textos más')

T3 = {
    # invierno: trineo de madera, patines
    'item.navidad.trineo_madera': ('Trineo de madera', 'Wooden sled'),
    'entity.navidad.trineo_madera': ('Trineo de madera', 'Wooden sled'),
    'navidad.trineo_madera.ayuda': ('Ponlo en la nieve, súbete y déjate ir cuesta abajo. Caben dos.',
                                    'Put it on snow, hop on and slide downhill. Two can ride.'),
    'navidad.trineo_madera.ayuda2': ('A/D voltear · W empujar · S frenar · Shift bajarse. En el hielo resbala más.',
                                     'A/D steer · W push · S brake · Shift get off. Slides even more on ice.'),
    'item.navidad.patines': ('Patines de hielo', 'Ice skates'),
    'navidad.patines.ayuda': ('En el hielo vas mucho más rápido; fuera del hielo caminas torpe.',
                              'Much faster on ice; clumsy everywhere else.'),
    'navidad.sub.trineo': ('Se desliza un trineo', 'Sled slides'),
    'navidad.sub.cascabel': ('Suena un cascabel', 'Little bell jingles'),
    'navidad.sub.patin': ('Raspa un patín', 'Skate scrapes'),
    'navidad.sub.campanada': ('Campanada', 'Bell tolls'),
    'navidad.sub.carta': ('Sale volando una carta', 'A letter flies away'),
    # Año Nuevo
    'item.navidad.uvas': ('Uvas de Año Nuevo', "New Year's grapes"),
    'navidad.uvas.ayuda': ('En Año Nuevo, cómete una con cada campanada y pide un deseo.',
                           "On New Year's, eat one with each bell toll and make a wish."),
    'navidad.anio.prepara': ('🍇 ¡Ya casi es Año Nuevo! Ten listas tus 12 uvas: una por cada campanada.',
                             "🍇 It's almost New Year! Get your 12 grapes ready: one per bell toll."),
    'navidad.anio.falta': ('para el Año Nuevo', 'until New Year'),
    'navidad.anio.come': ('¡Cómete la primera uva!', 'Eat the first grape!'),
    'navidad.anio.campanada': ('Campanada %s de 12', 'Bell %s of 12'),
    'navidad.anio.uva': ('🍇 Uvas: %s de 12', '🍇 Grapes: %s of 12'),
    'navidad.anio.feliz': ('¡Feliz Año Nuevo!', 'Happy New Year!'),
    'navidad.anio.feliz2': ('¡Bienvenido %s!', 'Welcome %s!'),
    'navidad.anio.doce': ('🍇 ¡Te comiste las 12 uvas a tiempo! Se cumplirán tus 12 deseos (y tienes mucha suerte este rato).',
                          '🍇 You ate all 12 grapes in time! Your 12 wishes will come true (and you are very lucky for a while).'),
    'navidad.anio.algunas': ('🍇 Te comiste %s uvas. ¡Algo de suerte te toca!', '🍇 You ate %s grapes. Some luck for you!'),
    'navidad.anio.ninguna': ('No te comiste ninguna uva... ¡el próximo año!', "You didn't eat any grapes... next year!"),
    'navidad.anio.nochevieja': ('🍇 ¡Hoy es Nochevieja! Aquí están tus 12 uvas para la medianoche.',
                                "🍇 It's New Year's Eve! Here are your 12 grapes for midnight."),
    # carta a Santa
    'navidad.buzon.como': ('Escribe tu carta a Santa en un libro y pluma y dale clic al buzón con ella.',
                           'Write your letter to Santa in a book and quill and click the mailbox with it.'),
    'navidad.carta.vacaciones': ('Santa lee las cartas del 1 de diciembre al 6 de enero.', 'Santa reads letters from December 1 to January 6.'),
    'navidad.carta.pendiente': ('Santa todavía no contesta tu otra carta. ¡Paciencia!', "Santa hasn't answered your other letter yet. Be patient!"),
    'navidad.carta.hoy': ('Ya mandaste tu carta de hoy. Santa lee una al día por persona.', 'You already sent today\'s letter. Santa reads one a day per person.'),
    'navidad.carta.vacia': ('La carta está vacía... escribe qué te gustaría.', 'The letter is empty... write what you would like.'),
    'navidad.carta.enviada': ('✉ Tu carta salió volando rumbo al Polo Norte. Santa la leerá esta noche... ¡pórtate bien!',
                              '✉ Your letter flew off to the North Pole. Santa will read it tonight... be good!'),
    'navidad.carta.llego': ('🎅 ¡Llegó la respuesta de Santa! Revisa tu inventario.', "🎅 Santa's answer arrived! Check your inventory."),
    'navidad.carta.llego_malo': ('🎅 Llegó la respuesta de Santa... y pesa como carbón.', "🎅 Santa's answer arrived... it feels heavy, like coal."),
    'navidad.carta.resp.hola': ('Querido/a %s:', 'Dear %s,'),
    'navidad.carta.resp.leida': ('Leí tu carta junto a la chimenea, con un chocolate calientito. Mis duendes trabajaron toda la noche.',
                                 'I read your letter by the fireplace with a hot chocolate. My elves worked all night.'),
    'navidad.carta.resp.nada': ('Leí tu carta, pero no entendí bien qué querías... así que te mando una sorpresa.',
                                "I read your letter, but I couldn't quite tell what you wanted... so here's a surprise."),
    'navidad.carta.resp.malo': ('Me contaron que lastimaste a quien no debías. Este año te tocó carbón. ¡Todavía estás a tiempo de portarte bien!',
                                "I heard you hurt someone you shouldn't have. This year you get coal. There's still time to be good!"),
    'navidad.carta.resp.dificil': ('Lo que pediste es difícil hasta para mis duendes, pero te mando algo para compensar.',
                                   'What you asked for is hard even for my elves, but here is something to make up for it.'),
    'navidad.carta.resp.bonito': ('Hay deseos que no caben en una caja. Esos los cuidas tú, con los que quieres.',
                                  "Some wishes don't fit in a box. Those you take care of, with the people you love."),
    'navidad.carta.resp.trae': ('Te traje:', 'I brought you:'),
    'navidad.carta.resp.carbon': ('Te traje:', 'I brought you:'),
    'navidad.carta.resp.firma': ('¡Feliz Navidad!\n— Santa', 'Merry Christmas!\n— Santa'),
}

if __name__ == '__main__':
    aplicar(T3)
    print(len(T3), 'textos del invierno')

T4 = {
    'navidad.carrera.sale': ('🛷 ¡Arrancaste! El cronómetro corre...', '🛷 Go! The clock is running...'),
    'navidad.carrera.llega': ('🛷 ¡Llegaste abajo en %s segundos!', '🛷 You reached the bottom in %s seconds!'),
    'navidad.carrera.record': ('🏆 ¡Nuevo récord de la colina de trineos: %s, %s segundos!', '🏆 New sled hill record: %s, %s seconds!'),
    'navidad.carrera.record_es': ('El récord es de %s: %s segundos.', 'The record belongs to %s: %s seconds.'),
}

if __name__ == '__main__':
    aplicar(T4)
    print(len(T4), 'textos de la carrera')

T5 = {
    'navidad.muerdago': ('🌿 ¡%s y %s quedaron bajo el muérdago! Abrazo navideño 🤗', '🌿 %s and %s are under the mistletoe! Christmas hug 🤗'),
}

if __name__ == '__main__':
    aplicar(T5)
    print(len(T5), 'texto del muérdago')

T7 = {
    'navidad.calcetin.lleno': ('★ ¡Santa te dejó algo en el calcetín!', '★ Santa left something in the stocking!'),
    'navidad.calcetin.vacio': ('El calcetín está vacío... Santa lo vuelve a llenar por la noche.',
                               'The stocking is empty... Santa fills it again at night.'),
    'navidad.calcetin.carbon': ('Solo hay carbón... alguien lastimó a quien no debía.', 'Only coal... someone hurt who they shouldn\'t have.'),
    'navidad.calcetin.fuera': ('Fuera de temporada, los calcetines son solo de adorno.', 'Out of season, stockings are just decoration.'),
    'navidad.calcetin.ayuda': ('En temporada, Santa lo llena cada noche: clic derecho para sacar lo de hoy.',
                               'In season, Santa fills it every night: right-click to take today\'s treats.'),
}

if __name__ == '__main__':
    aplicar(T7)
    print(len(T7), 'textos de los calcetines')

T9 = {
    'navidad.galletas.puestas': ('★ Le dejaste galletas y leche a Santa. Esta noche pasará por ellas...',
                                 '★ You left cookies and milk for Santa. He\'ll come by tonight...'),
    'navidad.galletas.ya': ('Santa ya tiene tus galletas de esta noche.', 'Santa already has your cookies for tonight.'),
    'navidad.galletas.manana': ('Santa ya pasó por tus galletas hace poco. Déjale otras la próxima noche.',
                                'Santa already came for your cookies. Leave him more next night.'),
    'navidad.galletas.comidas': ('★ ¡Santa se comió tus galletas y te dejó un regalo junto al plato!',
                                 '★ Santa ate your cookies and left you a gift next to the plate!'),
}

if __name__ == '__main__':
    aplicar(T9)
    print(len(T9), 'textos de las galletas para Santa')

T10 = {
    'navidad.angel.haciendo': ('❄ Haciendo un ángel de nieve... (no te muevas)', '❄ Making a snow angel... (don\'t move)'),
    'navidad.angel.listo': ('❄ ¡Hiciste un ángel de nieve!', '❄ You made a snow angel!'),
}

if __name__ == '__main__':
    aplicar(T10)
    print(len(T10), 'textos de los ángeles de nieve')

T11 = {
    'navidad.deco.regalo_vacio': ('Está vacío... es de adorno.', 'It\'s empty... just a decoration.'),
    'navidad.deco.bebida': ('★ ¡Qué rico! Ya se te quitó el frío.', '★ Delicious! You\'re not cold anymore.'),
    'navidad.deco.bebida_espera': ('Espera un ratito para otro sorbo.', 'Wait a little for another sip.'),
}

if __name__ == '__main__':
    aplicar(T11)
    print(len(T11), 'textos de los adornos que responden')

T12 = {
    'item.navidad.bufanda': ('Bufanda navideña', 'Christmas scarf'),
    'item.navidad.orejeras': ('Orejeras', 'Earmuffs'),
}

if __name__ == '__main__':
    aplicar(T12)
    print(len(T12), 'textos de la bufanda y las orejeras')

T13 = {
    'navidad.santa.jojojo': ('¡Jo, jo, jo!', 'Ho, ho, ho!'),
    'navidad.santa.feliz': ('¡Feliz Navidad, %s! Te dejé un regalito...', 'Merry Christmas, %s! I left you a little something...'),
    'navidad.regalo_cielo.aterrizo': ('★ ¡Un regalo de Santa aterrizó cerca de ti!', '★ A gift from Santa landed near you!'),
    'navidad.regalo_cielo.atrapado': ('★ ¡Atrapaste un regalo en el aire!', '★ You caught a gift in mid-air!'),
    'entity.navidad.regalo_cayendo': ('Regalo de Santa', 'Gift from Santa'),
    'entity.navidad.trineo_santa': ('Trineo de Santa', "Santa's sleigh"),
}

if __name__ == '__main__':
    aplicar(T13)
    print(len(T13), 'textos de Santa en survival')
