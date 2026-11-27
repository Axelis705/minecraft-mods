#!/usr/bin/env python3
"""La placa del nombre de la gente de Navidad (en lugar del letrerito negro de Minecraft): un listón rojo con orillas doradas, una
ramita de acebo con sus bolitas a la izquierda y la punta en cola de golondrina a la derecha. 32x16: izquierda (0-9), centro
(10-21, se estira) y derecha (22-31).  Uso: python3 herramientas/placa_nombre.py"""
import os
from PIL import Image

AQUI = os.path.dirname(os.path.abspath(__file__))
SALIDA = os.path.join(AQUI, '..', 'src', 'main', 'resources', 'assets', 'navidad', 'textures', 'gui', 'placa_nombre.png')

ROJO, ROJO2, ROJO3 = (176, 26, 38), (128, 14, 26), (214, 64, 70)
ORO, ORO2 = (240, 200, 84), (176, 128, 40)
VERDE, VERDE2, BAYA = (46, 128, 60), (26, 84, 40), (232, 40, 40)
T = (0, 0, 0, 0)

REJILLA = [
    "................................",
    "...gGG..GGg.....................",
    "..gGGGgGGGGg....................",
    "...gGBBbGGg.OOOOOOOOOOOOOOOOOOO.",
    "...OBBBBOOOOhhhhhhhhhhhhhhhhhhh.",
    "...hhBBhhhhhRRRRRRRRRRRRRRRRRRRr",
    "rrrRRRRRRRRRRRRRRRRRRRRRRRRRRRRr",
    "rrrRRRRRRRRRRRRRRRRRRRRRRRRRRRRr",
    ".rrRRRRRRRRRRRRRRRRRRRRRRRRRRRr.",
    ".rrRRRRRRRRRRRRRRRRRRRRRRRRRRRr.",
    "rrrRRRRRRRRRRRRRRRRRRRRRRRRRRRRr",
    "rrrRRRRRRRRRRRRRRRRRRRRRRRRRRRRr",
    "...ddddddddddddddddddddddddddd..",
    "...oooooooooooooooooooooooooooo.",
    "................................",
    "................................",
]
COLOR = {'R': ROJO, 'r': ROJO2, 'h': ROJO3, 'd': ROJO2, 'O': ORO, 'o': ORO2, 'G': VERDE, 'g': VERDE2, 'B': BAYA,
         'b': (255, 150, 150)}
im = Image.new('RGBA', (32, 16), T)
px = im.load()
for y, fila in enumerate(REJILLA):
    for x, ch in enumerate(fila.ljust(32, '.')[:32]):
        if ch != '.':
            px[x, y] = COLOR[ch] + (255,)
os.makedirs(os.path.dirname(SALIDA), exist_ok=True)
im.save(SALIDA)
im.resize((256, 128), Image.NEAREST).save('/tmp/claude-0/-home-claude/480fd80e-aa2e-51e0-b666-f35748868f51/scratchpad/nv2/placa.png')
print('placa lista')
