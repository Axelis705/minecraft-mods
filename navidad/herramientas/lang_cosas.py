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
