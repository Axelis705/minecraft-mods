#!/usr/bin/env python3
"""Textos del Libro de Navidad, el Pino para decorar y sus adornos (es_mx, es_es y en_us). Uso: python3 herramientas/lang_libro.py"""
import os
import sys

AQUI = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, AQUI)
from lang_cosas import aplicar  # noqa: E402
import adornos  # noqa: E402

T = {
    'block.navidad.pino_navidad': ('Pino de Navidad para decorar', 'Christmas pine to decorate'),
    'item.navidad.libro_navidad': ('Libro de Navidad', 'Christmas Book'),
    'navidad.pino.ayuda': ('Ponlo en tu casa y cuélgale adornos con clic derecho. Agachado con la mano vacía: descuelgas uno.',
                           'Place it at home and hang ornaments with right click. Sneak with an empty hand to take one off.'),
    'navidad.pino.trae': ('Trae %s adornos, %s tiras de luces y %s guirnaldas.', 'Has %s ornaments, %s light strings and %s garlands.'),
    'navidad.pino.sin_lugar': ('Ya no le caben más adornos colgados a este árbol.', 'No more room for hanging ornaments on this tree.'),
    'navidad.pino.luces_llenas': ('Tu árbol ya tiene las tres tiras de luces.', 'Your tree already has all three light strings.'),
    'navidad.pino.guirnaldas_llenas': ('Tu árbol ya tiene las tres guirnaldas.', 'Your tree already has all three garlands.'),
    'navidad.pino.sube': ('¡Tu árbol ahora es %s!', 'Your tree is now %s!'),
    'navidad.arbol.nivel.1': ('Arbolito', 'Little tree'),
    'navidad.arbol.nivel.2': ('Adornado', 'Decorated'),
    'navidad.arbol.nivel.3': ('Bonito', 'Lovely'),
    'navidad.arbol.nivel.4': ('Precioso', 'Beautiful'),
    'navidad.arbol.nivel.5': ('Espectacular', 'Spectacular'),
    'navidad.arbol.nivel.6': ('Legendario', 'Legendary'),
    'navidad.arbol.puntos': ('%s puntos (adornos %s, variedad %s, casa %s)', '%s points (ornaments %s, variety %s, home %s)'),
    'navidad.arbol.faltan': ('faltan %s para el siguiente nivel', '%s more for the next level'),
    'navidad.adorno.rareza.comun': ('Adorno común', 'Common ornament'),
    'navidad.adorno.rareza.bonito': ('Adorno bonito', 'Pretty ornament'),
    'navidad.adorno.rareza.raro': ('Adorno raro', 'Rare ornament'),
    'navidad.adorno.rareza.epico': ('Adorno épico', 'Epic ornament'),
    'navidad.adorno.puntos': ('+%s puntos para tu árbol', '+%s points for your tree'),
    'navidad.adorno.ayuda': ('Clic derecho en tu Pino de Navidad: se cuelga en la rama más cerca de donde le das.',
                             'Right click your Christmas pine: it hangs on the branch closest to where you click.'),
    'navidad.adorno.ayuda_luces': ('Clic derecho en tu Pino de Navidad para enredarle las luces (hasta 3 tiras).',
                                   'Right click your Christmas pine to wrap the lights around it (up to 3 strings).'),
    'navidad.adorno.ayuda_guirnalda': ('Clic derecho en tu Pino de Navidad para colgar la guirnalda (hasta 3).',
                                       'Right click your Christmas pine to drape the garland (up to 3).'),
    'navidad.adorno.ayuda_punta': ('Clic derecho en tu Pino de Navidad: va en la punta (brilla y gira).',
                                   'Right click your Christmas pine: it goes on the top (it shines and spins).'),
    # el libro
    'navidad.libro.ayuda': ('Clic derecho para abrirlo: tareas de Navidad que te dan adornos para tu árbol.',
                            'Right click to open it: Christmas tasks that give you ornaments for your tree.'),
    'navidad.libro.lista': ('¡Tarea del Libro de Navidad lista!', 'Christmas Book task done!'),
    'navidad.libro.capitulo': ('Capítulo %s', 'Chapter %s'),
    'navidad.libro.avance': ('Tareas reclamadas: %s de %s', 'Tasks claimed: %s of %s'),
    'navidad.libro.premio_capitulo': ('Premio del capítulo:', 'Chapter reward:'),
    'navidad.libro.reclamar': ('Reclamar premio', 'Claim reward'),
    'navidad.libro.reclamar_corto': ('Reclamar', 'Claim'),
    'navidad.libro.capitulo_listo': ('¡Capítulo completo!', 'Chapter complete!'),
    'navidad.libro.capitulo_titulo': ('★ ¡Capítulo terminado! ★', '★ Chapter finished! ★'),
    'navidad.libro.llevas': ('Llevas %s de %s', 'Progress: %s of %s'),
    'navidad.libro.premio': ('Premio:', 'Reward:'),
    'navidad.libro.mi_arbol': ('Mi árbol', 'My tree'),
    'navidad.libro.sin_arbol': ('Todavía no pones tu Pino de Navidad. Ponlo en tu casa y cuélgale adornos: entre más bonito esté, mejores '
                                'regalos te deja Santa cada mañana.',
                                "You haven't placed your Christmas pine yet. Put it in your house and hang ornaments on it: the prettier it is, "
                                'the better the gifts Santa leaves every morning.'),
    'navidad.libro.puntos_total': ('%s puntos', '%s points'),
    'navidad.libro.puntos_adornos': ('Adornos: %s', 'Ornaments: %s'),
    'navidad.libro.puntos_variedad': ('Distintos: +%s', 'Variety: +%s'),
    'navidad.libro.puntos_casa': ('Tu casa: +%s (de %s)', 'Your home: +%s (of %s)'),
    'navidad.libro.siguiente': ('Faltan %s para: %s', '%s more for: %s'),
    'navidad.libro.maximo': ('¡Tu árbol es legendario!', 'Your tree is legendary!'),
    'navidad.libro.regalos_nivel': ('Lo que deja Santa cada mañana', 'What Santa leaves every morning'),
    'navidad.libro.da.1': ('1 regalo sencillo', '1 simple gift'),
    'navidad.libro.da.2': ('1 o 2 regalos, a veces raros', '1 or 2 gifts, sometimes rare'),
    'navidad.libro.da.3': ('2 regalos y a veces un adorno', '2 gifts and sometimes an ornament'),
    'navidad.libro.da.4': ('2 o 3 regalos; adornos bonitos', '2 or 3 gifts; pretty ornaments'),
    'navidad.libro.da.5': ('3 regalos + 1 adorno siempre', '3 gifts + 1 ornament every day'),
    'navidad.libro.da.6': ('3 o 4 regalos + adorno raro', '3 or 4 gifts + a rare ornament'),
    'navidad.libro.consejo': ('Cada adorno distinto suma 1 más, y los adornos navideños de tu casa (cerca del árbol) suman hasta 25.',
                              'Each different ornament adds 1 more, and Christmas decorations in your home (near the tree) add up to 25.'),
    'navidad.libro.te_llega': ('★ Santa te mandó tu Libro de Navidad: ábrelo (clic derecho) para ver tus tareas y cómo va tu árbol.',
                               '★ Santa sent you your Christmas Book: open it (right click) to see your tasks and how your tree is doing.'),
    'navidad.bienvenida.1': ('• Pon tu Pino de Navidad y cuélgale adornos: entre más bonito, mejores regalos te deja Santa cada mañana. '
                             'Tu Libro de Navidad te dice qué hacer.',
                             '• Place your Christmas pine and hang ornaments on it: the prettier it is, the better the gifts Santa leaves '
                             'every morning. Your Christmas Book tells you what to do.'),
    'navidad.aviso.viene': ('¡Santa Claus se acerca!', 'Santa Claus is coming!'),
    'navidad.aviso.mira': ('¡Mira al cielo!', 'Look up at the sky!'),
    'navidad.aviso.lejos': ('viene a %s bloques', '%s blocks away'),
    'navidad.caja.toast.0': ('¡Abriste un regalo!', 'You opened a gift!'),
    'navidad.caja.toast.1': ('¡Un regalo raro!', 'A rare gift!'),
    'navidad.caja.toast.2': ('★ ¡Un regalo épico! ★', '★ An epic gift! ★'),
    'navidad.caja.toast_de': ('%s · de %s', '%s · from %s'),
    'item.navidad.lanzanieves': ('Lanzanieves', 'Snow Blaster'),
    'item.navidad.tanque_nieve': ('Tanque de nieve', 'Snow tank'),
    'entity.navidad.bola_gigante': ('Bola de nieve gigante', 'Giant snowball'),
    'navidad.lanzanieves.ayuda1': ('Mantén clic derecho: los cañones toman vuelo y disparan en ráfaga.',
                                   'Hold right click: the barrels spin up and fire in bursts.'),
    'navidad.lanzanieves.ayuda2': ('Con el tanque puesto, apunta a la nieve para aspirarla y rellenarlo.',
                                   'With the tank on, aim at snow to vacuum it and refill.'),
    'navidad.lanzanieves.ayuda3': ('Agachado: carga una bola gigante (8 tiros) y suelta para lanzarla.',
                                   'Sneaking: charge a giant snowball (8 shots) and release to launch it.'),
    'navidad.lanzanieves.sin_nieve': ('¡Sin nieve! Ponte el tanque de nieve o trae bolas de nieve.',
                                      'Out of snow! Wear the snow tank or carry snowballs.'),
    'navidad.lanzanieves.tanque_vacio': ('¡Tu tanque está vacío! Aspira nieve o llénalo (tecla K).',
                                         'Your tank is empty! Vacuum snow or fill it (K key).'),
    'navidad.lanzanieves.falta_gigante': ('Te faltan tiros: la bola gigante necesita %s.', 'Not enough snow: the giant snowball needs %s.'),
    'navidad.lanzanieves.hud_sin_tanque': ('sin tanque', 'no tank'),
    'navidad.lanzanieves.hud_lista': ('¡Suelta para lanzar!', 'Release to launch!'),
    'navidad.lanzanieves.hud_aspira': ('Aspirando nieve...', 'Vacuuming snow...'),
    'navidad.tanque.carga': ('Nieve: %s tiros', 'Snow: %s shots'),
    'navidad.tanque.tiros': ('%s tiros', '%s shots'),
    'navidad.tanque.ayuda': ('Clic derecho: te lo pones en la espalda. Agachado: abres sus ranuras (bolas o bloques de nieve).',
                             'Right click: wear it on your back. Sneaking: open its slots (snowballs or snow blocks).'),
    'navidad.tanque.no_traes': ('No traes puesto el tanque de nieve.', "You're not wearing the snow tank."),
    'key.navidad.tanque': ('Abrir el tanque de nieve', 'Open snow tank'),
    'key.categories.navidad': ('Navidad', 'Christmas'),
    'navidad.sub.lanzanieves': ('Lanzanieves dispara', 'Snow blaster fires'),
    'navidad.sub.lanzanieves_gira': ('Cañones girando', 'Barrels spin'),
    'navidad.sub.lanzanieves_carga': ('Bola gigante cargando', 'Giant snowball charging'),
    'navidad.sub.lanzanieves_bola': ('Bola gigante', 'Giant snowball'),
    'entity.navidad.cascanueces': ('Cascanueces guardián', 'Nutcracker guard'),
    'item.navidad.cascanueces_guardian': ('Cascanueces guardián', 'Nutcracker guard'),
    'navidad.cascanueces.ayuda1': ('Clic derecho en el piso: lo pones de guardia ahí (junto a tu árbol, por ejemplo).',
                                   'Right click the ground: he stands guard there (next to your tree, for example).'),
    'navidad.cascanueces.ayuda2': ('De noche hace su ronda, saca a los monstruos y atrapa al duende ladrón.',
                                   'At night he patrols, fights off monsters and catches the thief elf.'),
    'navidad.cascanueces.puesto': ('¡El cascanueces está de guardia!', 'The nutcracker is on guard!'),
    'navidad.cascanueces.ordenes': ('¡A sus órdenes! (agáchate y haz clic para levantarlo)', 'At your service! (sneak and click to pick him up)'),
    'navidad.cascanueces.de_otro': ('Este cascanueces cuida la casa de alguien más.', "This nutcracker guards someone else's home."),
    'navidad.cascanueces.cayo': ('¡Tumbaron a tu cascanueces! Recoge su figura y vuélvelo a poner.',
                                 'Your nutcracker was knocked down! Pick up his figure and place him again.'),
    'navidad.cascanueces.atrapo': ('★ ¡Tu cascanueces atrapó al duende ladrón y recuperó el regalo!',
                                   '★ Your nutcracker caught the thief elf and got the gift back!'),
    'navidad.sub.cascanueces': ('Cascanueces truena la quijada', 'Nutcracker clacks'),
    # capítulos
    'navidad.libro.cap.0': ('Preparativos', 'Getting ready'),
    'navidad.libro.cap.0.texto': ('Querido amigo: la Navidad empieza en casa. Pon tu pino, cuélgale adornos y llena tu casa de luces. Santa '
                                  'lo va a notar.',
                                  'Dear friend: Christmas begins at home. Place your pine, hang ornaments and fill your house with lights. '
                                  'Santa will notice.'),
    'navidad.libro.cap.1': ('Espíritu de invierno', 'Winter spirit'),
    'navidad.libro.cap.1.texto': ('Sal a la nieve: hay pingüinos y renitos que buscan amigos, peces bajo el hielo y nieve para jugar.',
                                  'Go out into the snow: penguins and little reindeer want friends, fish wait under the ice and the '
                                  'snow is for playing.'),
    'navidad.libro.cap.2': ('La magia de Santa', "Santa's magic"),
    'navidad.libro.cap.2.texto': ('Santa ya anda por el cielo. Atrapa sus regalos, escríbele y déjale galletas... y cuidado con los duendes '
                                  'traviesos.',
                                  "Santa is already in the sky. Catch his gifts, write to him, leave him cookies... and watch out for "
                                  'naughty elves.'),
    'navidad.libro.cap.3': ('Nochebuena', 'Christmas Eve'),
    'navidad.libro.cap.3.texto': ('Lo más difícil: un árbol espectacular, Krampus y los muñecos malvados. El premio: la estrella polar de '
                                  'cristal.',
                                  'The hardest part: a spectacular tree, Krampus and the evil snowmen. The reward: the crystal north '
                                  'star.'),
}

TAREAS = {
    'poner_arbol': ('Pon tu Pino de Navidad', 'Place your Christmas pine', 'Haz uno (abeto, hojas y tronco de abeto) o cómpralo a Don Abeto.',
                    'Craft one (spruce sapling, leaves and log) or buy it from Don Abeto.'),
    'colgar': ('Cuelga 5 adornos', 'Hang 5 ornaments', 'Clic derecho con un adorno en tu Pino de Navidad.', 'Right click your pine with an '
               'ornament.'),
    'troncos': ('Corta 16 troncos de abeto', 'Chop 16 spruce logs', 'Los abetos crecen en la taiga y en la nieve.',
                'Spruces grow in taigas and snowy places.'),
    'casa': ('Pon 10 adornos en tu casa', 'Place 10 decorations at home', 'Cualquier bloque de decoración navideña.',
             'Any Christmas decoration block.'),
    'bebida': ('Toma algo calientito', 'Drink something warm', 'Chocolate caliente, ponche o champurrado.', 'Hot chocolate, punch or '
               'champurrado.'),
    'abeto': ('Visita a Don Abeto', 'Visit Don Abeto', 'Su puesto de árboles sale en praderas, bosques y taigas.',
              'His tree lot appears in plains, forests and taigas.'),
    'pinguino': ('Haz tuyo a un pingüino', 'Befriend a penguin', 'Dale pescado crudo a uno en la nieve.', 'Feed raw fish to one in the snow.'),
    'golem': ('Arma un gólem de nieve', 'Build a snow golem', 'Dos bloques de nieve y una calabaza encima.', 'Two snow blocks with a pumpkin '
              'on top.'),
    'pescar': ('Pesca 8 peces', 'Catch 8 fish', 'Con caña, en cualquier agua.', 'With a fishing rod, in any water.'),
    'angel': ('Haz un ángel de nieve', 'Make a snow angel', 'Acuéstate en la nieve: agáchate quieto un ratito sobre ella.',
              'Lie in the snow: sneak still on it for a moment.'),
    'bolas': ('Lanza 50 bolas de nieve', 'Throw 50 snowballs', 'A mano o con el lanzanieves.', 'By hand or with the snow blaster.'),
    'renito': ('Haz tuyo a un renito', 'Befriend a baby reindeer', 'Con zanahorias (la dorada nunca falla).', 'With carrots (a golden one '
               'never fails).'),
    'regalo_aire': ('Atrapa un regalo en el aire', 'Catch a gift in mid-air', 'Pégale a un regalo de Santa antes de que toque el piso.',
                    "Hit one of Santa's gifts before it lands."),
    'abrir': ('Abre 10 regalos', 'Open 10 gifts', 'Cajas de regalo: de Santa, de tu árbol, de las cartas...', 'Gift boxes: from Santa, from your tree, from letters...'),
    'galletas': ('Galletas y leche para Santa', 'Cookies and milk for Santa', 'Déjalas cerca de tu árbol antes de dormir.',
                 'Leave them near your tree before sleeping.'),
    'carta': ('Escríbele a Santa', 'Write to Santa', 'Echa una carta en el buzón de Santa.', "Drop a letter in Santa's mailbox."),
    'calcetin': ('Que Santa llene tu calcetín', 'Get your stocking filled', 'Cuelga un calcetín en la pared y duerme.',
                 'Hang a stocking on a wall and sleep.'),
    'ladron': ('Atrapa al duende ladrón', 'Catch the thief elf', 'Algunas noches viene a robar regalos de tu árbol.',
               'Some nights he comes to steal gifts from your tree.'),
    'nivel_3': ('Árbol "Bonito"', '"Lovely" tree', 'Junta 25 puntos en tu árbol.', 'Get 25 points on your tree.'),
    'nivel_5': ('Árbol "Espectacular"', '"Spectacular" tree', 'Junta 70 puntos en tu árbol.', 'Get 70 points on your tree.'),
    'variedad': ('12 adornos distintos', '12 different ornaments', 'Cuélgale a tu árbol 12 adornos diferentes.',
                 'Hang 12 different ornaments on your tree.'),
    'munecos': ('Vence 10 muñecos malvados', 'Defeat 10 evil snowmen', 'Salen de noche en la nieve.', 'They come out at night in the '
                'snow.'),
    'viajero': ('Encuentra a Ramiro', 'Find Ramiro', 'El duende viajero llega cada dos o tres días con su reno.',
                'The traveling elf arrives every two or three days with his reindeer.'),
    'krampus': ('Vence a Krampus', 'Defeat Krampus', 'Llámalo con su campana... si te atreves.', 'Summon him with his bell... if you dare.'),
}

for k, (es, en, ces, cen) in TAREAS.items():
    T[f'navidad.libro.tarea.{k}'] = (es, en)
    T[f'navidad.libro.como.{k}'] = (ces, cen)

for id_, es, en, rareza, ancla, tipo, fn in adornos.ADORNOS:
    T[f'item.navidad.{id_}'] = (es, en)

if __name__ == '__main__':
    aplicar(T)
    print(len(T), 'textos del libro y el pino')
