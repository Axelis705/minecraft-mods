#!/usr/bin/env python3
"""Recursos del Krampus renovado (todo lo que no es el modelo del Krampus ni del krampusillo):
- íconos: cadena, vara de abedul, saco, campanita de Santa (y el huevo del krampusillo);
- la máscara del Krampus (modelo de bloque con cuernos, para la cabeza y la mano) y su textura;
- el farol de Nochebuena (encendido y apagado) y el altar del Krampus: modelos, estados, botín;
- el saco en la espalda (prenda del Vestuario), la huella de pezuña y la vista desde adentro del costal;
- sonidos (sounds.json), efectos de partículas, botín de la guarida y del krampusillo, recetas y la generación de la guarida.
Uso: python3 herramientas/krampus_recursos.py"""
import json
import os
import random
import sys

from PIL import Image, ImageDraw, ImageFilter

AQUI = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, AQUI)
sys.path.insert(0, os.path.join(AQUI, '..', '..', 'axelis_core', 'herramientas'))
import iconos as I  # noqa: E402
import geo as G  # noqa: E402

RES = os.path.join(AQUI, '..', 'src', 'main', 'resources')
A = os.path.join(RES, 'assets', 'navidad')
D = os.path.join(RES, 'data', 'navidad')


def escribir(ruta, datos):
    os.makedirs(os.path.dirname(ruta), exist_ok=True)
    with open(ruta, 'w', encoding='utf-8') as f:
        json.dump(datos, f, indent=1, ensure_ascii=False)


# ------------------------------------------------------------------ íconos

def iconos():
    I.dibujo('cadena_krampus', [
        '..........ss....', '.........sSSs...', '.........s..s...', '........sSSs....', '.......s..s.....', '......sSSs......',
        '.....s..s.......', '....sSSs........', '...s..s.........', '..sSSs..........', '..s.k...........', '..kk.k..........',
        '.k...k..........', '.k..k...........', '..kk............', '................'],
        {'s': '#8a8a96', 'S': '#5a5a66', 'k': '#3a3a44'})
    I.dibujo('vara_abedul', [
        '............w.w.', '..........w.ww.w', '...........wwkw.', '.........wwkww..', '........wwkww...', '.......wkwww....',
        '......wwwkw.....', '.....rrwww......', '....rRRrw.......', '...wrRrr........', '..wkwr..........', '.wkw............',
        'wkw.............', 'ww..............', '................', '................'],
        {'w': '#eceae0', 'k': '#2a2826', 'r': '#c41c28', 'R': '#8a1018'})
    I.dibujo('saco_krampus', [
        '................', '.......yy.......', '......yYYy......', '.......yy.......', '......bBBb......', '.....bBbbBb.....',
        '....bbbbBbbb....', '...bbBbbbbbBb...', '...bbbbppppbb...', '..bbBbbpYYpbbb..', '..bbbbbppppbBb..', '..bBbbbbbbbbbb..',
        '..bbbbbBbbbbbb..', '...bbBbbbbbBb...', '....BBBBBBBB....', '................'],
        {'b': '#9a7848', 'B': '#6e5430', 'y': '#c4a468', 'Y': '#8a6a3a', 'p': '#6a4e38'})
    I.dibujo('campanita_santa', [
        '................', '.....r....r.....', '....rRr..rRr....', '.....rrrrrr.....', '.......yy.......', '......yYYy......',
        '.....yYYYYy.....', '.....yYwYYy.....', '....yYYwYYYy....', '....yYYYYYYy....', '...yYYYYYYYYy...', '...yyyyyyyyyy...',
        '...YYYYYYYYYY...', '.......kk.......', '................', '................'],
        {'y': '#f6d050', 'Y': '#c8961c', 'w': '#fff6c0', 'r': '#d42a2a', 'R': '#8a1018', 'k': '#6a4a10'})
    escribir(os.path.join(A, 'models', 'item', 'huevo_krampusillo.json'), {'parent': 'minecraft:item/template_spawn_egg'})


# ------------------------------------------------------------------ la máscara (modelo de bloque: se ve en la cabeza y en la mano)

def mascara():
    """Textura 32x32: 0-15 x 0-15 la cara (cuero con ceño, cuencas y boca), 16-31 x 0-7 pelo, 16-23 x 8-15 cuerno, 24-27 x 8-15 lengua,
    28-31 x 8-11 dientes."""
    rnd = random.Random(7)
    img = Image.new('RGBA', (32, 32), (0, 0, 0, 0))
    px = img.load()
    piel, piel_d, boca = (64, 40, 35), (38, 23, 21), (40, 10, 12)
    for y in range(16):
        for x in range(16):
            c = piel if (x + y) % 4 else (52, 33, 29)
            if y in (4, 5) and 2 <= x <= 13:
                c = piel_d                                    # ceño
            if y in (6, 7) and (2 <= x <= 5 or 10 <= x <= 13):
                c = boca                                      # cuencas
            if y == 7 and x in (3, 4, 11, 12):
                c = (230, 60, 30)                             # el brillo de los ojos
            if y in (11, 12) and 3 <= x <= 12:
                c = boca                                      # la boca
            if y == 11 and x in (3, 5, 10, 12):
                c = (232, 224, 198)
            if y >= 14 or y < 2:
                c = (40, 28, 24)
            k = rnd.randint(-6, 6)
            px[x, y] = (max(0, c[0] + k), max(0, c[1] + k), max(0, c[2] + k), 255)
    for y in range(8):
        for x in range(16, 32):
            c = (48, 34, 27) if (x * 3 + y) % 5 else (84, 60, 44)
            px[x, y] = c + (255,)
    for y in range(8, 16):
        for x in range(16, 24):
            t = (y - 8) / 7
            c = (int(226 - 140 * t), int(214 - 140 * t), int(182 - 122 * t))
            if (y + x) % 3 == 0:
                c = tuple(int(v * 0.8) for v in c)
            px[x, y] = c + (255,)
        for x in range(24, 28):
            px[x, y] = ((196, 42, 54) if (x + y) % 3 else (140, 22, 34)) + (255,)
    for y in range(8, 12):
        for x in range(28, 32):
            px[x, y] = (232, 224, 198, 255)
    os.makedirs(os.path.join(A, 'textures', 'item'), exist_ok=True)
    img.save(os.path.join(A, 'textures', 'item', 'mascara_krampus.png'))

    def cubo(desde, hasta, uv, rot=None, caras=None):
        e = {'from': desde, 'to': hasta, 'faces': {}}
        for f in caras or ('north', 'south', 'east', 'west', 'up', 'down'):
            e['faces'][f] = {'uv': uv, 'texture': '#m'}
        if rot:
            e['rotation'] = rot
        return e

    PELO = [8, 0, 12, 2]          # (las UV de los modelos van de 0 a 16 aunque la textura sea de 32)
    els = [
        cubo([1.5, 1, 0], [14.5, 15, 1.5], [0, 0, 8, 8], caras=['north']),                  # la cara
        cubo([1.5, 1, 0], [14.5, 15, 1.5], PELO, caras=['south', 'east', 'west', 'up', 'down']),
        cubo([1, 13, 1.5], [15, 16, 14], PELO),                                                 # la greña de arriba
        cubo([0.5, 3, 1.5], [1.5, 14, 13], PELO),                                               # los lados (cubren las orejas)
        cubo([14.5, 3, 1.5], [15.5, 14, 13], PELO),
        cubo([5.5, 4, -1.5], [10.5, 7, 0], [1.5, 5, 6.5, 6.5]),                                      # el hocico
        cubo([6.5, -6, -1.6], [9.5, 4, -0.6], [12, 4, 13.5, 7.5]),                                 # la lengua de fuera
        cubo([4, 2, -1.2], [5, 5, -0.2], [14, 4, 14.5, 5.5]),                                      # colmillos
        cubo([11, 2, -1.2], [12, 5, -0.2], [14, 4, 14.5, 5.5]),
    ]
    # cuernos: base gruesa que sale de la greña abriéndose hacia afuera y una punta más abierta (las dos piezas se tocan)
    import math
    for s in (-1, 1):
        x0 = 4 if s < 0 else 12
        # base: de y 13 a 20, girada 22.5 grados hacia afuera desde abajo
        els.append(cubo([x0 - 1.5, 13, 5], [x0 + 1.5, 20, 8], [8, 4, 12, 8],
                        rot={'angle': -22.5 * s, 'axis': 'z', 'origin': [x0, 13, 6.5]}))
        # dónde quedó la punta de la base
        xt = x0 + s * 7 * math.sin(math.radians(22.5))
        yt = 13 + 7 * math.cos(math.radians(22.5))
        els.append(cubo([xt - 1, yt - 0.5, 5.5], [xt + 1, yt + 5, 7.5], [8, 4, 12, 8],
                        rot={'angle': -45 * s, 'axis': 'z', 'origin': [xt, yt - 0.5, 6.5]}))
    modelo = {'textures': {'m': 'navidad:item/mascara_krampus', 'particle': 'navidad:item/mascara_krampus'}, 'elements': els,
              'gui_light': 'front',
              'display': {
                  'gui': {'rotation': [10, 160, 0], 'translation': [0, -2.5, 0], 'scale': [0.5, 0.5, 0.5]},
                  'ground': {'rotation': [0, 0, 0], 'translation': [0, 2, 0], 'scale': [0.4, 0.4, 0.4]},
                  'fixed': {'rotation': [0, 180, 0], 'translation': [0, -1, 0], 'scale': [0.6, 0.6, 0.6]},
                  'thirdperson_righthand': {'rotation': [75, 45, 0], 'translation': [0, 2.5, 0], 'scale': [0.4, 0.4, 0.4]},
                  'thirdperson_lefthand': {'rotation': [75, 45, 0], 'translation': [0, 2.5, 0], 'scale': [0.4, 0.4, 0.4]},
                  'firstperson_righthand': {'rotation': [0, 135, 0], 'translation': [0, 2, 0], 'scale': [0.4, 0.4, 0.4]},
                  'firstperson_lefthand': {'rotation': [0, 135, 0], 'translation': [0, 2, 0], 'scale': [0.4, 0.4, 0.4]},
                  'head': {'rotation': [0, 0, 0], 'translation': [0, -3.2, 0], 'scale': [1, 1, 1]}}}
    escribir(os.path.join(A, 'models', 'item', 'mascara_krampus.json'), modelo)


# ------------------------------------------------------------------ el farol de Nochebuena y el altar

def farol():
    """Textura 16x16: vidrio (0-5, 0-5), oro (6-7, 0-1), poste de madera oscura (10-11, 0-7), vela (12, 0-2) y su llama (13, 0-1)."""
    for nombre, lit in (('farol_nochebuena', True), ('farol_nochebuena_apagado', False)):
        img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
        px = img.load()
        for y in range(6):
            for x in range(6):
                borde = x in (0, 5) or y in (0, 5)
                if borde:
                    px[x, y] = (214, 164, 52, 255) if (x + y) % 2 else (170, 120, 30, 255)
                elif lit:
                    d = abs(x - 2.5) + abs(y - 3)
                    px[x, y] = (255, int(236 - d * 18), int(150 - d * 30), 255)
                else:
                    px[x, y] = (40, 44, 58, 255) if (x + y) % 3 else (60, 66, 84, 255)
        for y in range(2):
            for x in range(6, 8):
                px[x, y] = (220, 172, 60, 255)
        for y in range(8):
            for x in range(10, 12):
                px[x, y] = (58, 40, 24, 255) if (x + y) % 3 else (44, 30, 18, 255)
        for y in range(3):
            px[12, y] = (240, 236, 226, 255)
        px[13, 0] = (255, 220, 90, 255) if lit else (60, 60, 60, 255)
        px[13, 1] = (255, 150, 40, 255) if lit else (40, 40, 40, 255)
        img.save(os.path.join(A, 'textures', 'block', nombre + '.png'))

        def e(desde, hasta, uv, caras=('north', 'south', 'east', 'west', 'up', 'down')):
            return {'from': desde, 'to': hasta, 'faces': {f: {'uv': uv, 'texture': '#t'} for f in caras}}
        els = [e([7, 0, 7], [9, 8, 9], [10, 0, 12, 8]),                                    # poste
               e([4.5, 8, 4.5], [11.5, 9, 11.5], [6, 0, 8, 2]),                            # base dorada
               e([5, 9, 5], [11, 14.5, 11], [0, 0, 6, 6], ('north', 'south', 'east', 'west')),
               e([7.5, 9, 7.5], [8.5, 11.5, 8.5], [12, 0, 13, 3]),                         # la vela
               e([7.6, 11.5, 7.6], [8.4, 12.8, 8.4], [13, 0, 14, 2]),                      # la llama
               e([4.5, 14.5, 4.5], [11.5, 15.5, 11.5], [6, 0, 8, 2]),                      # tapa
               e([7, 15.5, 7], [9, 16, 9], [6, 0, 8, 2])]
        escribir(os.path.join(A, 'models', 'block', nombre + '.json'),
                 {'parent': 'minecraft:block/block', 'render_type': 'minecraft:cutout', 'ambientocclusion': False,
                  'textures': {'t': f'navidad:block/{nombre}', 'particle': f'navidad:block/{nombre}'}, 'elements': els})
    escribir(os.path.join(A, 'blockstates', 'farol_nochebuena.json'), {'variants': {
        f'lit={l},temporal={t}': {'model': 'navidad:block/farol_nochebuena' + ('' if l == 'true' else '_apagado')}
        for l in ('true', 'false') for t in ('true', 'false')}})
    escribir(os.path.join(A, 'models', 'item', 'farol_nochebuena.json'), {'parent': 'navidad:block/farol_nochebuena'})
    escribir(os.path.join(D, 'loot_tables', 'blocks', 'farol_nochebuena.json'), {'type': 'minecraft:block', 'pools': [{
        'rolls': 1, 'entries': [{'type': 'minecraft:item', 'name': 'navidad:farol_nochebuena'}],
        'conditions': [{'condition': 'minecraft:survives_explosion'}]}]})


def altar():
    def e(desde, hasta, tex, arriba=None, caras=('north', 'south', 'east', 'west', 'up', 'down')):
        el = {'from': desde, 'to': hasta, 'faces': {}}
        for f in caras:
            el['faces'][f] = {'texture': arriba if (f == 'up' and arriba) else tex}
        return el
    els = [e([2, 0, 2], [14, 3, 14], '#piedra'),
           e([4, 3, 4], [12, 11, 12], '#ladrillo'),
           e([1, 11, 1], [15, 15, 3], '#cincelada'), e([1, 11, 13], [15, 15, 15], '#cincelada'),
           e([1, 11, 3], [3, 15, 13], '#cincelada'), e([13, 11, 3], [15, 15, 13], '#cincelada'),
           e([3, 11, 3], [13, 13.5, 13], '#carbon'),
           e([6, 13.5, 6], [10, 14, 10], '#brasa', caras=('up', 'north', 'south', 'east', 'west'))]
    # dos cadenas colgando del tazón (planos cruzados con la textura de la cadena)
    for x, z in ((2, 2), (14, 14)):
        for ang in (45, -45):
            els.append({'from': [x - 1.5, 5, z], 'to': [x + 1.5, 11, z], 'rotation': {'angle': ang, 'axis': 'y', 'origin': [x, 8, z]},
                        'faces': {'north': {'uv': [0, 0, 3, 6], 'texture': '#cadena'}, 'south': {'uv': [0, 0, 3, 6], 'texture': '#cadena'}}})
    escribir(os.path.join(A, 'models', 'block', 'altar_krampus.json'), {
        'parent': 'minecraft:block/block', 'render_type': 'minecraft:cutout',
        'textures': {'piedra': 'minecraft:block/polished_blackstone', 'ladrillo': 'minecraft:block/polished_blackstone_bricks',
                     'cincelada': 'minecraft:block/chiseled_polished_blackstone', 'carbon': 'minecraft:block/coal_block',
                     'brasa': 'minecraft:block/magma', 'cadena': 'minecraft:block/chain', 'particle': 'minecraft:block/polished_blackstone'},
        'elements': els})
    escribir(os.path.join(A, 'blockstates', 'altar_krampus.json'), {'variants': {'': {'model': 'navidad:block/altar_krampus'}}})
    escribir(os.path.join(A, 'models', 'item', 'altar_krampus.json'), {'parent': 'navidad:block/altar_krampus'})
    escribir(os.path.join(D, 'loot_tables', 'blocks', 'altar_krampus.json'), {'type': 'minecraft:block', 'pools': [{
        'rolls': 1, 'entries': [{'type': 'minecraft:item', 'name': 'navidad:altar_krampus'}],
        'conditions': [{'condition': 'minecraft:survives_explosion'}]}]})


# ------------------------------------------------------------------ el saco en la espalda, la huella y el costal por dentro

def prenda_saco():
    costal, costal_d, costal_l, cuerda = (152, 120, 78), (116, 88, 54), (180, 150, 102), (196, 166, 112)

    def tela(face, x, y, w, h):
        if face == 'bottom':
            return costal_d
        if face in ('back',) and 2 <= x <= 4 and 3 <= y <= 5:
            return (110, 82, 62) if 2 < x < 4 and 3 < y < 5 else cuerda      # el parche
        return costal if (x + y) % 2 == 0 else costal_d if (x * 3 + y) % 5 == 0 else costal_l

    def soga(face, x, y, w, h):
        return cuerda if (x + y) % 3 else costal_d

    c = G.Creature('geometry.navidad.saco_krampus', 64, 64, seed=33)
    c.bone('cuerpo', [0, 24, 0])
    c.part('cuerpo', [-4, 12, 2.5], [8, 10, 6], tela)                     # el costal
    c.part('cuerpo', [-2, 22, 3.5], [4, 2, 4], tela)                      # el cuello del costal
    c.part('cuerpo', [-2.5, 22, 3], [5, 1, 5], soga)                      # amarrado
    c.part('cuerpo', [-1, 24, 4.5], [2, 2, 2], tela)                      # la puntita
    c.part('cuerpo', [-3.5, 23.5, -2.5], [1, 1, 6], soga)                 # las cuerdas sobre los hombros
    c.part('cuerpo', [2.5, 23.5, -2.5], [1, 1, 6], soga)
    c.part('cuerpo', [-3.5, 15, -2.6], [1, 9, 1], soga)                   # y por el pecho
    c.part('cuerpo', [2.5, 15, -2.6], [1, 9, 1], soga)
    c.build(os.path.join(A, 'geo', 'saco_krampus.geo.json'), os.path.join(A, 'textures', 'prenda', 'saco_krampus.png'))


def huella():
    img = Image.new('RGBA', (32, 32), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    # pezuña partida: dos dedos como frijoles, hundidos en la nieve (orilla clara, centro oscuro)
    for x0 in (7, 17):
        d.ellipse([x0, 4, x0 + 8, 27], fill=(170, 190, 210, 150))
        d.ellipse([x0 + 1, 6, x0 + 7, 25], fill=(48, 40, 46, 230))
        d.ellipse([x0 + 2, 9, x0 + 6, 21], fill=(24, 18, 22, 240))
    d.ellipse([13, 25, 19, 31], fill=(60, 54, 60, 120))
    img = img.filter(ImageFilter.SMOOTH)
    img.save(os.path.join(A, 'textures', 'entity', 'huella_krampus.png'))


def costal_dentro():
    """La vista desde adentro del costal: tejido café grueso, con la luz que se cuela entre los hilos y por unos agujeritos."""
    rnd = random.Random(12)
    w = 256
    img = Image.new('RGBA', (w, w), (0, 0, 0, 255))
    px = img.load()
    for y in range(w):
        for x in range(w):
            hy = (y // 4) % 2
            hx = (x // 4) % 2
            hilo = (x % 4 == 0) or (y % 4 == 0)
            base = 70 + 22 * (hx ^ hy)
            if hilo:
                base -= 30
            # luz que se cuela: más en el centro de arriba
            dx, dy = (x - w / 2) / (w / 2), (y - w * 0.35) / (w / 2)
            luz = max(0.0, 1 - (dx * dx + dy * dy)) ** 1.5
            k = rnd.randint(-6, 6)
            r = int(base * 0.75 + luz * 60) + k
            g = int(base * 0.52 + luz * 38) + k
            b = int(base * 0.30 + luz * 18) + k
            if x % 4 == 2 and y % 4 == 2 and luz > 0.2 and rnd.random() < 0.25:
                r, g, b = 255, 214, 140                   # entre los hilos se ve la luz
            px[x, y] = (max(0, min(255, r)), max(0, min(255, g)), max(0, min(255, b)), 255)
    d = ImageDraw.Draw(img)
    for _ in range(9):
        x, y = rnd.randint(30, 226), rnd.randint(20, 160)
        d.ellipse([x - 3, y - 2, x + 3, y + 2], fill=(255, 226, 160, 255))
    img.save(os.path.join(A, 'textures', 'gui', 'saco_dentro.png'))


# ------------------------------------------------------------------ sonidos, efectos, botín, recetas, generación

SONIDOS = {
    'krampus_cadenas': (['krampus_cadenas1', 'krampus_cadenas2', 'krampus_cadenas3'], 'navidad.sub.krampus_cadenas', False),
    'krampus_cascabeles': (['krampus_cascabeles1', 'krampus_cascabeles2'], 'navidad.sub.krampus_cascabeles', False),
    'krampus_latigo': (['krampus_latigo1', 'krampus_latigo2'], 'navidad.sub.krampus_latigo', False),
    'krampus_saco': (['krampus_saco'], 'navidad.sub.krampus_saco', False),
    'krampus_pezuna': (['krampus_pezuna1', 'krampus_pezuna2', 'krampus_pezuna3'], 'navidad.sub.krampus_pezuna', False),
    'krampus_soplido': (['krampus_soplido'], 'navidad.sub.krampus_soplido', False),
    'krampus_apagon': (['krampus_apagon'], 'navidad.sub.krampus_apagon', False),
    'krampus_apagon_musica': (['krampus_apagon_musica'], None, True),
    'krampus_risa': (['krampus_risa1', 'krampus_risa2'], 'navidad.sub.krampus_risa', False),
    'krampusillo_risa': (['krampusillo_risa1', 'krampusillo_risa2'], 'navidad.sub.krampusillo_risa', False),
    'krampusillo_dolor': (['krampusillo_dolor'], 'navidad.sub.krampusillo_dolor', False),
    'campanita': (['campanita'], 'navidad.sub.campanita', False),
    'gancho': (['gancho'], 'navidad.sub.gancho', False),
    'noche_krampus': (['noche_krampus'], 'navidad.sub.noche_krampus', False),
}


def sonidos():
    p = os.path.join(A, 'sounds.json')
    d = json.load(open(p, encoding='utf-8'))
    for k, (archivos, sub, stream) in SONIDOS.items():
        e = {'sounds': [{'name': 'navidad:' + a, 'stream': True} if stream else 'navidad:' + a for a in archivos]}
        if sub:
            e['subtitle'] = sub
        d[k] = e
    json.dump(d, open(p, 'w', encoding='utf-8'), indent=1, ensure_ascii=False)


def efectos():
    E = os.path.join(A, 'efectos')
    escribir(os.path.join(E, 'saco_roto.json'), {'sonido': 'minecraft:block.wool.break', 'volumen': 1.4, 'tono': 0.7, 'capas': [
        {'particula': 'polvo', 'color': '#9A7848', 'color2': '#6E5430', 'tam': 1.2, 'forma': 'esfera', 'radio': 0.8, 'cuantas': 40,
         'velocidad': 0.25, 'hacia': 'afuera'},
        {'particula': 'brillo', 'forma': 'esfera', 'radio': 1.0, 'cuantas': 20, 'velocidad': 0.15, 'hacia': 'afuera'},
        {'particula': 'humo', 'forma': 'esfera', 'radio': 0.6, 'cuantas': 14, 'velocidad': 0.05, 'hacia': 'arriba'}]})
    escribir(os.path.join(E, 'apagon.json'), {'sonido': 'minecraft:entity.warden.sonic_charge', 'volumen': 1.2, 'tono': 0.6, 'capas': [
        {'particula': 'humo_grande', 'forma': 'anillo', 'radio': 3.0, 'cuantas': 60, 'velocidad': 0.25, 'hacia': 'afuera'},
        {'particula': 'polvo', 'color': '#0A0610', 'color2': '#2A1030', 'tam': 2.4, 'forma': 'esfera', 'radio': 2.0, 'cuantas': 80,
         'velocidad': 0.35, 'hacia': 'afuera'},
        {'particula': 'sculk', 'forma': 'anillo', 'radio': 6.0, 'cuantas': 40, 'velocidad': 0.05, 'hacia': 'arriba'}]})
    escribir(os.path.join(E, 'farol_enciende.json'), {'capas': [
        {'particula': 'fuego', 'forma': 'esfera', 'radio': 0.25, 'cuantas': 10, 'velocidad': 0.04, 'hacia': 'arriba'},
        {'particula': 'polvo', 'color': '#FFE070', 'color2': '#FF9020', 'tam': 0.9, 'forma': 'anillo', 'radio': 0.6, 'cuantas': 16,
         'velocidad': 0.05, 'hacia': 'afuera'}]})
    escribir(os.path.join(E, 'campanita.json'), {'capas': [
        {'particula': 'polvo', 'color': '#FFE070', 'color2': '#FFFFFF', 'tam': 1.0, 'forma': 'anillo', 'radio': 2.0, 'cuantas': 40,
         'velocidad': 0.3, 'hacia': 'afuera'},
        {'particula': 'nota', 'forma': 'esfera', 'radio': 0.6, 'cuantas': 6, 'velocidad': 0.1, 'hacia': 'arriba'},
        {'particula': 'brillo', 'forma': 'esfera', 'radio': 1.5, 'cuantas': 24, 'velocidad': 0.12, 'hacia': 'afuera'}]})


def item(nombre, peso, mn=1, mx=1, extra=None):
    e = {'type': 'minecraft:item', 'name': nombre, 'weight': peso}
    if mx > 1 or mn > 1:
        e['functions'] = [{'function': 'minecraft:set_count', 'count': {'type': 'minecraft:uniform', 'min': mn, 'max': mx}}]
    if extra:
        e.setdefault('functions', []).extend(extra)
    return e


def botin():
    escribir(os.path.join(D, 'loot_tables', 'chests', 'guarida_krampus.json'), {'type': 'minecraft:chest', 'pools': [
        {'rolls': 1, 'entries': [item('navidad:campanita_santa', 1)]},
        {'rolls': 1, 'entries': [item('navidad:farol_nochebuena', 1, 2, 4)]},
        {'rolls': {'type': 'minecraft:uniform', 'min': 3, 'max': 6}, 'entries': [
            item('minecraft:coal', 30, 4, 14), item('navidad:dulce', 20, 3, 8), item('minecraft:iron_nugget', 12, 3, 9),
            item('minecraft:bone', 10, 1, 4), item('minecraft:chain', 10, 1, 3), item('navidad:adorno_cristal', 6, 1, 2),
            item('navidad:campana_krampus', 4), item('minecraft:gold_ingot', 6, 1, 3), item('minecraft:diamond', 2, 1, 2)]},
        {'rolls': 1, 'entries': [item('navidad:vara_abedul', 1), {'type': 'minecraft:empty', 'weight': 7}]}]})
    escribir(os.path.join(D, 'loot_tables', 'entities', 'krampusillo.json'), {'type': 'minecraft:entity', 'pools': [
        {'rolls': 1, 'entries': [item('minecraft:coal', 1, 0, 2, [{'function': 'minecraft:looting_enchant',
                                                                   'count': {'type': 'minecraft:uniform', 'min': 0, 'max': 1}}])]},
        {'rolls': 1, 'conditions': [{'condition': 'minecraft:killed_by_player'}, {'condition': 'minecraft:random_chance', 'chance': 0.35}],
         'entries': [item('navidad:dulce', 1, 1, 3)]}]})


def recetas():
    R = os.path.join(D, 'recipes')
    escribir(os.path.join(R, 'farol_nochebuena.json'), {
        'type': 'minecraft:crafting_shaped', 'category': 'misc', 'pattern': ['NGN', 'NTN', 'NNN'],
        'key': {'N': {'item': 'minecraft:iron_nugget'}, 'G': {'item': 'minecraft:gold_nugget'}, 'T': {'item': 'minecraft:torch'}},
        'result': {'item': 'navidad:farol_nochebuena', 'count': 1}})
    escribir(os.path.join(R, 'campanita_santa.json'), {
        'type': 'minecraft:crafting_shaped', 'category': 'misc', 'pattern': [' R ', ' G ', 'GNG'],
        'key': {'R': {'item': 'minecraft:red_dye'}, 'G': {'item': 'minecraft:gold_ingot'}, 'N': {'item': 'minecraft:gold_nugget'}},
        'result': {'item': 'navidad:campanita_santa', 'count': 1}})


def generacion():
    escribir(os.path.join(D, 'worldgen', 'configured_feature', 'guarida_krampus.json'), {'type': 'navidad:guarida_krampus', 'config': {}})
    escribir(os.path.join(D, 'worldgen', 'placed_feature', 'guarida_krampus.json'), {'feature': 'navidad:guarida_krampus', 'placement': [
        {'type': 'minecraft:rarity_filter', 'chance': 180}, {'type': 'minecraft:in_square'},
        {'type': 'minecraft:heightmap', 'heightmap': 'MOTION_BLOCKING_NO_LEAVES'}, {'type': 'minecraft:biome'}]})
    escribir(os.path.join(D, 'forge', 'biome_modifier', 'guarida_krampus.json'), {
        'type': 'forge:add_features', 'biomes': '#forge:is_snowy', 'features': 'navidad:guarida_krampus', 'step': 'surface_structures'})


if __name__ == '__main__':
    iconos()
    mascara()
    farol()
    altar()
    prenda_saco()
    huella()
    costal_dentro()
    sonidos()
    efectos()
    botin()
    recetas()
    generacion()
    print('recursos del Krampus listos')
