#!/usr/bin/env python3
"""La Caja de música navideña (modelo hecho a mano, medidas enteras):
cajita de madera laqueada en rojo con orillas y patitas doradas, cerradura al frente y un copo dorado en la tapa. La tapa se abre hacia
atrás (bisagra atrás arriba) y por dentro es de terciopelo verde con un espejito; adentro, sobre un escenario de espejo que gira, un
pinito con su estrella y dos regalitos; al lado, la manivela dorada que da vueltas.
Huesos: caja, tapa, escenario (gira en y), manivela (gira en x).  Uso: python3 herramientas/caja_musica.py"""
import os
import sys

AQUI = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.join(AQUI, '..', '..', 'axelis_core', 'herramientas'))
from geo import Anim, Creature, save_anims  # noqa: E402

A = os.path.join(AQUI, '..', 'src', 'main', 'resources', 'assets', 'navidad')

ROJO, ROJO2, ROJO3 = (168, 24, 36), (120, 14, 26), (204, 56, 64)
ORO, ORO2, ORO3 = (236, 190, 70), (176, 128, 40), (255, 226, 140)
VERDE, VERDE2 = (30, 96, 52), (20, 66, 38)
PINO, PINO2 = (36, 120, 58), (24, 86, 44)
ESPEJO, ESPEJO2 = (200, 214, 230), (150, 170, 196)


def laca(face, x, y, w, h):
    """Madera laqueada roja con una orilla dorada y vetas suaves."""
    if face == 'bottom':
        return ROJO2
    borde = x in (0, w - 1) or y in (0, h - 1)
    if face == 'top':
        return ORO if borde else (ROJO if (x + y) % 5 else ROJO3)
    if borde:
        return ORO if y != h - 1 else ORO2
    if face == 'front' and x == w // 2 and y in (1, 2):
        return (40, 30, 20) if y == 2 else ORO3          # la cerradura
    return ROJO if (x * 3 + y) % 7 else ROJO3


COPO = ["..........",
        "....o.....",
        "..o.o.o...",
        "...ooo....",
        ".ooooooo..",
        "...ooo....",
        "..o.o.o...",
        "....o....."]


def tapa(face, x, y, w, h):
    if face == 'bottom':
        # por dentro: terciopelo verde con un espejito al centro
        if 3 <= x <= w - 4 and 2 <= y <= h - 3:
            return ESPEJO if (x + y) % 3 else ESPEJO2
        return VERDE if (x + y) % 2 else VERDE2
    if face == 'top':
        if x in (0, w - 1) or y in (0, h - 1):
            return ORO
        fila = COPO[min(len(COPO) - 1, y)] if y < len(COPO) else ''
        if 0 <= x < len(fila) and fila[x] == 'o':
            return ORO3 if (x + y) % 2 else ORO
        return ROJO if (x + y) % 6 else ROJO3
    if y == h - 1 or x in (0, w - 1):
        return ORO
    return ROJO


def dentro(face, x, y, w, h):
    return VERDE if (x + y) % 2 else VERDE2


def espejo(face, x, y, w, h):
    if face == 'top':
        return ESPEJO if (x * 2 + y) % 5 else (236, 244, 255)
    return ORO


def pino(face, x, y, w, h):
    if face == 'bottom':
        return PINO2
    if (x + y * 2) % 5 == 0:
        return (230, 40, 40) if (x + y) % 2 else (240, 210, 70)   # bolitas
    return PINO if (x + y) % 3 else PINO2


def solido(c, c2=None):
    return lambda f, x, y, w, h: (c2 or c) if f == 'bottom' else c


c = Creature('geometry.navidad.caja_musica', 64, 64, seed=24)
c.bone('caja', [0, 0, 0])
for x, z in ((-5, -4), (4, -4), (-5, 3), (4, 3)):
    c.part('caja', [x, 0, z], [1, 1, 1], solido(ORO, ORO2))          # patitas
c.part('caja', [-5, 1, -4], [10, 5, 8], laca)                         # la caja
c.part('caja', [-4, 5.2, -3], [8, 1, 6], dentro)                      # el fondo de terciopelo (adentro)
c.bone('escenario', [0, 6, 0], 'caja')
c.part('escenario', [-2, 6, -2], [4, 1, 4], espejo)                    # escenario de espejo que gira
c.part('escenario', [-0.5, 7, -0.5], [1, 1, 1], solido((110, 70, 40)))  # tronquito
c.part('escenario', [-2, 8, -2], [4, 2, 4], pino)
c.part('escenario', [-1.5, 10, -1.5], [3, 2, 3], pino)
c.part('escenario', [-1, 12, -1], [2, 1, 2], pino)
c.bone('estrella', [0, 13, 0], 'escenario')
c.part('estrella', [-0.5, 13, -0.5], [1, 1, 1], solido(ORO3))
c.part('escenario', [1.2, 7, -2.6], [1, 1, 1], solido((40, 110, 200)))   # regalitos que giran con el pino
c.part('escenario', [-2.4, 7, 1.4], [1, 1, 1], solido((200, 40, 60)))
c.glow('estrella')
c.bone('tapa', [0, 6, 4], 'caja')
c.part('tapa', [-5, 6, -4], [10, 2, 8], tapa)
c.part('tapa', [-1, 8, -1], [2, 1, 2], solido(ORO3, ORO))                # botoncito dorado arriba
c.bone('manivela', [5.5, 3.5, 0], 'caja')
c.part('manivela', [5, 3, -0.5], [2, 1, 1], solido(ORO2))               # eje
c.part('manivela', [7, 3, -0.5], [1, 3, 1], solido(ORO))                # brazo
c.part('manivela', [7, 5, -1.5], [1, 1, 2], solido((110, 70, 40)))      # mango de madera
c.build(os.path.join(A, 'geo', 'caja_musica.geo.json'), os.path.join(A, 'textures', 'block', 'caja_musica.png'))

# animación de muestra (solo para verla en el visor; en el juego la mueve el código)
a = Anim('suena', 2.0, True)
a.rot('tapa', {0: [-100, 0, 0], 2.0: [-100, 0, 0]})
a.rot('escenario', {0: [0, 0, 0], 2.0: [0, 360, 0]})
a.rot('manivela', {0: [0, 0, 0], 2.0: [720, 0, 0]})
save_anims('caja_musica', [a], os.path.join(A, 'animations', 'caja_musica.animation.json'))
print('caja de música lista')

# ------------------------------------------------------------------ recursos del bloque (estado, modelos, botín, receta, sonidos)
import json  # noqa: E402

D = os.path.join(AQUI, '..', 'src', 'main', 'resources', 'data', 'navidad')


def escribir(ruta, datos):
    os.makedirs(os.path.dirname(ruta), exist_ok=True)
    with open(ruta, 'w', encoding='utf-8') as f:
        json.dump(datos, f, indent=1, ensure_ascii=False)


escribir(os.path.join(A, 'blockstates', 'caja_musica.json'), {'variants': {
    f'facing={d}': {'model': 'navidad:block/caja_musica'} for d in ('north', 'south', 'east', 'west')}})
escribir(os.path.join(A, 'models', 'block', 'caja_musica.json'), {'textures': {'particle': 'navidad:block/caja_musica'}})
escribir(os.path.join(A, 'models', 'item', 'caja_musica.json'), {
    'parent': 'minecraft:builtin/entity', 'gui_light': 'side',
    'textures': {'particle': 'navidad:block/caja_musica'},
    'display': {
        'gui': {'rotation': [30, 200, 0], 'translation': [0, 2.5, 0], 'scale': [1.1, 1.1, 1.1]},
        'ground': {'rotation': [0, 0, 0], 'translation': [0, 3, 0], 'scale': [0.6, 0.6, 0.6]},
        'fixed': {'rotation': [0, 180, 0], 'translation': [0, 2, 0], 'scale': [1, 1, 1]},
        'thirdperson_righthand': {'rotation': [75, 45, 0], 'translation': [0, 2.5, 0], 'scale': [0.5, 0.5, 0.5]},
        'thirdperson_lefthand': {'rotation': [75, 45, 0], 'translation': [0, 2.5, 0], 'scale': [0.5, 0.5, 0.5]},
        'firstperson_righthand': {'rotation': [0, 200, 0], 'translation': [0, 3, 0], 'scale': [0.7, 0.7, 0.7]},
        'firstperson_lefthand': {'rotation': [0, 200, 0], 'translation': [0, 3, 0], 'scale': [0.7, 0.7, 0.7]}}})
escribir(os.path.join(D, 'loot_tables', 'blocks', 'caja_musica.json'), {'type': 'minecraft:block', 'pools': [{
    'rolls': 1, 'entries': [{'type': 'minecraft:item', 'name': 'navidad:caja_musica'}],
    'conditions': [{'condition': 'minecraft:survives_explosion'}]}]})
escribir(os.path.join(D, 'recipes', 'caja_musica.json'), {
    'type': 'minecraft:crafting_shaped', 'category': 'misc',
    'pattern': [' O ', 'TNT', 'TTT'],
    'key': {'O': {'item': 'minecraft:gold_ingot'}, 'N': {'item': 'minecraft:note_block'}, 'T': {'tag': 'minecraft:planks'}},
    'result': {'item': 'navidad:caja_musica', 'count': 1}})
p = os.path.join(A, 'sounds.json')
d = json.load(open(p, encoding='utf-8'))
for n in ('campanas', 'navidad', 'noche_paz', 'adornen'):
    d['caja_' + n] = {'sounds': [{'name': 'navidad:caja_' + n, 'stream': True}], 'subtitle': 'navidad.sub.caja_musica'}
json.dump(d, open(p, 'w', encoding='utf-8'), indent=1, ensure_ascii=False)
print('recursos de la caja de música listos')
