#!/usr/bin/env python3
"""Los logros (advancements) de Navidad de Axelis: una pestaña propia con las cosas navideñas que puedes hacer.
Escribe data/navidad/advancements/navidad/*.json y sus textos (es_mx, es_es, en_us). Se puede correr varias veces.
Casi todos se dan desde el código (criterio 'hecho' imposible, ver Logros.java); unos pocos usan disparadores del juego."""
import json, os

AQUI = os.path.dirname(os.path.abspath(__file__))
RES = os.path.join(AQUI, '..', 'src', 'main', 'resources')
ADV = os.path.join(RES, 'data', 'navidad', 'advancements', 'navidad')
LANG = os.path.join(RES, 'assets', 'navidad', 'lang')

IMPOSIBLE = {'hecho': {'trigger': 'minecraft:impossible'}}

# id: (padre, icono, marco, título es, título en, descripción es, descripción en, criterios, oculto)
L = [
    ('raiz', None, 'navidad:gorro_santa', 'task', 'Navidad de Axelis', 'Navidad de Axelis',
     '¡Llegó la temporada más bonita del año!', 'The most wonderful time of the year is here!', {'hecho': {'trigger': 'minecraft:tick'}}, False),
    ('galleta', 'raiz', 'navidad:galleta_jengibre', 'task', 'Huele a jengibre', 'Smells like ginger',
     'Cómete una galleta de jengibre', 'Eat a gingerbread cookie',
     {'hecho': {'trigger': 'minecraft:consume_item', 'conditions': {'item': {'items': ['navidad:galleta_jengibre']}}}}, False),
    ('gorro', 'raiz', 'navidad:gorro_santa', 'task', 'Jo, jo, jo', 'Ho, ho, ho',
     'Consigue un gorro de Santa', "Get a Santa hat",
     {'hecho': {'trigger': 'minecraft:inventory_changed', 'conditions': {'items': [{'items': ['navidad:gorro_santa']}]}}}, False),
    ('carta', 'raiz', 'minecraft:writable_book', 'task', 'Querido Santa...', 'Dear Santa...',
     'Mete una carta en el buzón de Santa', "Put a letter in Santa's mailbox", IMPOSIBLE, False),
    ('respuesta', 'carta', 'minecraft:written_book', 'task', '¡Me contestó!', 'He wrote back!',
     'Recibe la respuesta de Santa', "Get Santa's answer", IMPOSIBLE, False),
    ('carbon', 'carta', 'minecraft:coal', 'task', 'Lista de los traviesos', 'Naughty list',
     'Recibe carbón de Santa', 'Get coal from Santa', IMPOSIBLE, True),
    ('regalo', 'raiz', 'navidad:caja_regalo', 'task', 'Es para mí', "It's for me",
     'Abre una caja de regalo', 'Open a gift box', IMPOSIBLE, False),
    ('calcetin', 'regalo', 'navidad:calcetin_rojo', 'task', 'Calcetín lleno', 'Stuffed stocking',
     'Saca lo que Santa dejó en un calcetín navideño', 'Take what Santa left in a Christmas stocking', IMPOSIBLE, False),
    ('galletas', 'regalo', 'navidad:galletas_leche', 'task', 'Santa pasó por aquí', 'Santa was here',
     'Déjale galletas y leche a Santa y que se las coma', 'Leave cookies and milk for Santa and let him eat them', IMPOSIBLE, False),
    ('pinata', 'raiz', 'navidad:palo_pinata', 'task', '¡Dale, dale, dale!', 'Hit it, hit it!',
     'Rompe una piñata en una posada', 'Break a piñata at a posada', IMPOSIBLE, False),
    ('rosca', 'pinata', 'navidad:rosca_reyes', 'task', 'Te tocan los tamales', "You're buying the tamales",
     'Encuentra el muñequito en la rosca de Reyes', 'Find the baby figurine in the rosca de Reyes', IMPOSIBLE, False),
    ('uvas', 'pinata', 'navidad:uvas', 'goal', 'Doce deseos', 'Twelve wishes',
     'Cómete las 12 uvas a tiempo con las campanadas de Año Nuevo', "Eat all 12 grapes in time with the New Year's bells", IMPOSIBLE, False),
    ('angel', 'raiz', 'navidad:angel_nieve', 'task', 'Ángel de nieve', 'Snow angel',
     'Agáchate en la nieve mirando al cielo hasta hacer un ángel de nieve', 'Crouch on the snow looking at the sky until you make a snow angel',
     IMPOSIBLE, False),
    ('muerdago', 'angel', 'navidad:muerdago', 'task', 'Bajo el muérdago', 'Under the mistletoe',
     'Recibe un abrazo navideño bajo el muérdago', 'Get a Christmas hug under the mistletoe', IMPOSIBLE, False),
    ('guerra', 'angel', 'minecraft:snowball', 'goal', 'Lluvia de bolazos', 'Snowball storm',
     'Gana una guerra de bolas de nieve (tú solo o con tu equipo)', 'Win a snowball fight (alone or with your team)', IMPOSIBLE, False),
    ('patines', 'angel', 'navidad:patines', 'task', 'Sobre hielo', 'On thin ice',
     'Patina sobre el hielo con patines', 'Skate on ice wearing ice skates', IMPOSIBLE, False),
    ('colina', 'patines', 'navidad:trineo_madera', 'task', '¡Bajada!', 'Downhill!',
     'Baja la colina de trineos de la aldea con el cronómetro corriendo', "Ride down the village sled hill with the clock running",
     IMPOSIBLE, False),
    ('record', 'colina', 'minecraft:clock', 'challenge', 'Rey de la colina', 'King of the hill',
     'Rompe el récord de la colina de trineos', 'Break the sled hill record', IMPOSIBLE, False),
    ('reno', 'raiz', 'navidad:huevo_reno', 'task', 'Amigo de los renos', 'Reindeer friend',
     'Hazte amigo de un reno dándole de comer', 'Befriend a reindeer by feeding it', IMPOSIBLE, False),
    ('krampus', 'reno', 'navidad:cuerno_krampus', 'challenge', '¡Navidad salvada!', 'Christmas saved!',
     'Vence al Krampus', 'Defeat the Krampus', IMPOSIBLE, False),
    ('nacimiento', 'raiz', 'navidad:pesebre', 'task', 'Noche de paz', 'Silent night',
     'Pon el pesebre con el Niño en tu nacimiento', 'Place the manger with the Child in your nativity scene', IMPOSIBLE, False),
    ('casita', 'nacimiento', 'navidad:ladrillo_jengibre', 'goal', 'Toc, toc, duende', 'Knock knock, elf',
     'Encuentra una casita de duende en la nieve y abre su cofre', "Find an elf cottage in the snow and open its chest",
     {'hecho': {'trigger': 'minecraft:player_generates_container_loot', 'conditions': {'loot_table': 'navidad:chests/casita_duende'}}}, False),
]


def main():
    os.makedirs(ADV, exist_ok=True)
    for f in os.listdir(ADV):
        os.remove(os.path.join(ADV, f))
    textos = {}
    for (aid, padre, icono, marco, tes, ten, des, den, crit, oculto) in L:
        d = {'display': {'icon': {'item': icono}, 'title': {'translate': f'advancement.navidad.{aid}'},
                         'description': {'translate': f'advancement.navidad.{aid}.desc'}, 'frame': marco,
                         'show_toast': aid != 'raiz', 'announce_to_chat': aid != 'raiz', 'hidden': oculto},
             'criteria': crit}
        if padre:
            d['parent'] = f'navidad:navidad/{padre}'
        else:
            d['display']['background'] = 'minecraft:textures/block/snow.png'
        json.dump(d, open(os.path.join(ADV, aid + '.json'), 'w', encoding='utf-8'), indent=1, ensure_ascii=False)
        textos[f'advancement.navidad.{aid}'] = (tes, ten)
        textos[f'advancement.navidad.{aid}.desc'] = (des, den)
    for code in ('es_mx', 'es_es', 'en_us'):
        p = os.path.join(LANG, code + '.json')
        j = json.load(open(p, encoding='utf-8'))
        for k, (es, en) in textos.items():
            j[k] = en if code == 'en_us' else es
        json.dump(j, open(p, 'w', encoding='utf-8'), indent=2, ensure_ascii=False)
    print(len(L), 'logros')


if __name__ == '__main__':
    main()
